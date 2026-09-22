package com.rskickbox.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.max

private const val RS_CHAT_MEDIA_BUCKET_V92="rs-chat-media"

data class RsChatAttachmentV92(
    val path:String,
    val kind:String,
    val name:String
)

@Serializable
data class RsCloudGroupMessageV92(
    val id:String,
    @SerialName("group_id") val groupId:String,
    @SerialName("sender_id") val senderId:String,
    @SerialName("sender_email") val senderEmail:String,
    @SerialName("sender_name") val senderName:String,
    val body:String,
    @SerialName("media_path") val mediaPath:String?=null,
    @SerialName("media_kind") val mediaKind:String?=null,
    @SerialName("media_name") val mediaName:String?=null,
    @SerialName("reply_to") val replyTo:String?=null,
    @SerialName("reply_body") val replyBody:String?=null,
    @SerialName("created_at") val createdAt:String
)

private fun rsChatFileNameV92(context:Context,uri:Uri):String{
    var result=uri.lastPathSegment?.substringAfterLast('/')?.takeIf{it.isNotBlank()} ?: "attachment"
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())result=cur.getString(0)?:result
        }
    }
    return result.take(120)
}

private fun rsChatMediaDurationMsV122(context:Context,uri:Uri):Long{
    val r=MediaMetadataRetriever()
    return try{
        r.setDataSource(context,uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L
    }finally{runCatching{r.release()}}
}

private fun rsChatSourceSizeV115(context:Context,uri:Uri):Long{
    if(uri.scheme=="file"){
        return runCatching{uri.path?.let(::File)?.length()?:-1L}.getOrDefault(-1L)
    }
    return runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.SIZE),null,null,null)?.use{cur->
            if(cur.moveToFirst())cur.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)
}

private fun rsChatImageBytesV92(context:Context,uri:Uri):ByteArray{
    // Photo Picker can return very large HEIC/JPEG/PNG images. Decode them to a
    // bounded software bitmap before compression so selecting/sending a photo
    // cannot exhaust the app process while video/audio continue using binary IO.
    val bitmap:Bitmap=if(Build.VERSION.SDK_INT>=28){
        val source=ImageDecoder.createSource(context.contentResolver,uri)
        ImageDecoder.decodeBitmap(source){decoder,info,_->
            val sourceW=info.size.width.coerceAtLeast(1)
            val sourceH=info.size.height.coerceAtLeast(1)
            val maxSide=max(sourceW,sourceH)
            if(maxSide>1440){
                val scale=1440f/maxSide.toFloat()
                decoder.setTargetSize(
                    (sourceW*scale).toInt().coerceAtLeast(1),
                    (sourceH*scale).toInt().coerceAtLeast(1)
                )
            }
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired=false
        }
    }else{
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
            ?:error("Could not read image.")
        require(bounds.outWidth>0&&bounds.outHeight>0){"Could not decode image."}
        var sample=1
        while(bounds.outWidth/sample>1440 || bounds.outHeight/sample>1440)sample*=2
        val opts=BitmapFactory.Options().apply{
            inSampleSize=sample
            inPreferredConfig=Bitmap.Config.RGB_565
        }
        context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}
            ?:error("Could not decode image.")
    }
    try{
        require(bitmap.width>0&&bitmap.height>0){"Could not decode image."}
        return ByteArrayOutputStream().use{out->
            require(bitmap.compress(Bitmap.CompressFormat.JPEG,82,out)){"Could not prepare image."}
            out.toByteArray()
        }
    }finally{
        runCatching{bitmap.recycle()}
    }
}

private fun rsChatBinaryBytesV125(context:Context,uri:Uri,maxBytes:Long=30L*1024L*1024L):ByteArray{
    val size=rsChatSourceSizeV115(context,uri)
    require(size in 1..maxBytes){"Chat attachment must be 30 MB or smaller."}
    require(size<=Int.MAX_VALUE){"Attachment is too large."}
    val result=ByteArray(size.toInt())
    context.contentResolver.openInputStream(uri)?.use{input->
        var offset=0
        while(offset<result.size){
            val read=input.read(result,offset,result.size-offset)
            if(read<0)break
            offset+=read
        }
        require(offset==result.size){"Could not read the complete attachment."}
    } ?: error("Could not read attachment.")
    return result
}

suspend fun rsUploadChatMediaV92(
    context:Context,
    uri:Uri,
    scopeType:String,
    scopeId:String
):Result<RsChatAttachmentV92> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(scopeType in setOf("coach","group","community","support")){"Invalid chat scope."}
    val mime=context.contentResolver.getType(uri)?.lowercase().orEmpty()
    val name=rsChatFileNameV92(context,uri)
    val lowerName=name.lowercase()
    val kind=when{
        mime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".webm") || lowerName.endsWith(".mov") || lowerName.endsWith(".3gp")->"VIDEO"
        mime.startsWith("audio/") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") || lowerName.endsWith(".mp3") || lowerName.endsWith(".ogg") || lowerName.endsWith(".wav")->"AUDIO"
        mime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif")->"IMAGE"
        else->"FILE"
    }

    if(kind=="VIDEO"){
        val duration=rsChatMediaDurationMsV122(context,uri)
        require(duration in 1..30_000){"Short videos can be up to 30 seconds."}
    }else if(kind=="AUDIO"){
        val duration=rsChatMediaDurationMsV122(context,uri)
        require(duration in 1..120_000){"Voice messages can be up to 2 minutes."}
    }

    val bytes=try{
        if(kind=="IMAGE")rsChatImageBytesV92(context,uri)
        else rsChatBinaryBytesV125(context,uri)
    }catch(t:OutOfMemoryError){
        throw IllegalStateException("This media file is too large for the device. Choose a smaller image or video.")
    }
    require(bytes.size<=30*1024*1024){"Chat attachment must be 30 MB or smaller."}

    val ext=when{
        kind=="IMAGE"->"jpg"
        kind=="AUDIO" && mime.contains("mpeg")->"mp3"
        kind=="AUDIO" && mime.contains("ogg")->"ogg"
        kind=="AUDIO" && mime.contains("wav")->"wav"
        kind=="AUDIO"->"m4a"
        kind=="FILE"->lowerName.substringAfterLast('.', "bin").take(8).replace(Regex("[^a-z0-9]"),"").ifBlank{"bin"}
        mime=="video/webm"->"webm"
        mime=="video/quicktime"->"mov"
        mime=="video/3gpp"->"3gp"
        else->"mp4"
    }
    val path=scopeType+"/"+scopeId+"/"+UUID.randomUUID().toString()+"."+ext
    val contentType=when{
        kind=="IMAGE"->ContentType.Image.JPEG
        kind=="AUDIO"->runCatching{ContentType.parse(if(mime.isBlank())"audio/mp4" else mime)}.getOrDefault(ContentType.parse("audio/mp4"))
        kind=="FILE"->runCatching{ContentType.parse(if(mime.isBlank())"application/octet-stream" else mime)}.getOrDefault(ContentType.Application.OctetStream)
        else->runCatching{ContentType.parse(if(mime.isBlank())"video/mp4" else mime)}.getOrDefault(ContentType.Video.MP4)
    }

    val currentUser=client.auth.currentUserOrNull()
    val sessionRole=rsCloudCurrentSessionV67().getOrNull()?.role
    // Personal storage quota belongs to the student who uploads the media.
    // Trainer/admin uploads are shared coaching content and must not consume
    // an individual student's allowance.
    val studentId=if(sessionRole==RsRole.TRAINER)"" else currentUser?.id.orEmpty()
    if(studentId.isNotBlank()){
        // Backward-compatible rollout: if the v0.114 capacity RPC is not installed yet,
        // continue with the existing upload/registration flow. Once migration 0048 is
        // live, this preflight blocks over-quota uploads before bytes are sent.
        rsStudentStorageCapacityV114(studentId,bytes.size.toLong()).getOrNull()?.let{capacity->
            require(capacity.allowed){
                capacity.warningMessage.ifBlank{
                    "Student storage limit reached. Free some space or increase the student's storage allowance."
                }
            }
        }
    }

    client.storage.from(RS_CHAT_MEDIA_BUCKET_V92).upload(path,bytes){
        upsert=false
        this.contentType=contentType
    }
    if(studentId.isNotBlank()){
        val registration=rsRegisterStudentMediaAssetV111(
            studentId=studentId,
            bucket=RS_CHAT_MEDIA_BUCKET_V92,
            path=path,
            kind=kind,
            sourceArea="CHAT",
            byteSize=bytes.size.toLong()
        )
        if(registration.isFailure){
            runCatching{client.storage.from(RS_CHAT_MEDIA_BUCKET_V92).delete(path)}
            throw registration.exceptionOrNull() ?: IllegalStateException("Could not register chat media storage.")
        }
    }
    RsChatAttachmentV92(path,kind,name)
}

suspend fun rsDeleteChatMediaV108(path:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.storage.from(RS_CHAT_MEDIA_BUCKET_V92).delete(path)
    // Migration 0049 keeps quota usage accurate after the file is removed.
    rsRemoveStudentMediaAssetByPathV115(RS_CHAT_MEDIA_BUCKET_V92,path).getOrNull()
    Unit
}

suspend fun rsChatMediaLocalUriV92(context:Context,path:String):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val ext=path.substringAfterLast('.',"bin")
    val dir=File(context.cacheDir,"rs_chat_media").apply{mkdirs()}
    val file=File(dir,path.hashCode().toString()+"."+ext)
    if(!file.exists()||file.length()==0L){
        val bytes=client.storage.from(RS_CHAT_MEDIA_BUCKET_V92).downloadAuthenticated(path)
        file.writeBytes(bytes)
    }
    Uri.fromFile(file).toString()
}

suspend fun rsCloudGroupMessagesV92(groupId:String):Result<List<RsCloudGroupMessageV92>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_group_message_feed_v2",
        buildJsonObject{put("p_group_id",groupId)}
    ).decodeList<RsCloudGroupMessageV92>()
}

suspend fun rsSendCloudGroupMessageV92(
    groupId:String,
    body:String,
    attachment:RsChatAttachmentV92?,
    replyTo:String?=null
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(body.trim().isNotBlank()||attachment!=null){"Write a message or add an attachment."}
    client.postgrest.rpc(
        "rs_send_group_message_v2",
        buildJsonObject{
            put("p_group_id",groupId)
            put("p_body",body.trim())
            if(attachment==null){
                put("p_media_path",kotlinx.serialization.json.JsonNull)
                put("p_media_kind",kotlinx.serialization.json.JsonNull)
                put("p_media_name",kotlinx.serialization.json.JsonNull)
            }else{
                put("p_media_path",attachment.path)
                put("p_media_kind",attachment.kind)
                put("p_media_name",attachment.name)
            }
            if(replyTo.isNullOrBlank())put("p_reply_to",kotlinx.serialization.json.JsonNull)
            else put("p_reply_to",replyTo)
        }
    )
    Unit
}

fun rsChatBackendFriendlyErrorV121(lang:RsLang,t:Throwable?):String{
    val raw=t?.message.orEmpty()
    return when{
        raw.contains("Bucket not found",ignoreCase=true) ||
        raw.contains("rs-chat-media",ignoreCase=true) && raw.contains("not found",ignoreCase=true) ||
        raw.contains("media_path",ignoreCase=true) && raw.contains("does not exist",ignoreCase=true) ||
        raw.contains("42703",ignoreCase=true) ||
        raw.contains("rs_send_coach_message_v2",ignoreCase=true) && raw.contains("not found",ignoreCase=true) ||
        raw.contains("rs_send_group_message",ignoreCase=true) && raw.contains("not found",ignoreCase=true) ->
            rsChatMediaT(lang,"backend_missing")
        raw.contains("row-level security",ignoreCase=true) ||
        raw.contains("permission",ignoreCase=true) ||
        raw.contains("unauthorized",ignoreCase=true) ->
            rsChatMediaT(lang,"permission_error")
        raw.contains("30 MB",ignoreCase=true) || raw.contains("30 seconds",ignoreCase=true) -> raw
        raw.contains("too large for the device",ignoreCase=true) -> raw
        else->rsChatMediaT(lang,"upload_error")
    }
}

fun rsChatMediaT(lang:RsLang,key:String):String{
    val en=mapOf(
        "image" to "Image","photo" to "Photo","video" to "Short video","voice" to "Voice message","file" to "File","remove" to "Remove attachment",
        "send" to "Send","sending" to "Sending…","uploading" to "Uploading attachment…",
        "attachment_ready" to "Attachment ready","open_video" to "Play short video","play_voice" to "Play voice message",
        "download_error" to "Could not load attachment.","upload_error" to "Could not upload attachment.","file_open_error" to "No compatible app could open this file.","message" to "Message…","message_or_media" to "Write a message or add an attachment.",
        "max_video" to "Short videos: maximum 30 seconds / 30 MB.","max_voice" to "Voice messages: maximum 2 minutes / 30 MB.","voice_unavailable" to "Voice recording could not start.","recording" to "Recording… tap stop when finished.","mic_permission" to "Microphone permission is required for voice messages.","voice_too_short" to "Voice message was too short. Try again.",
        "backend_missing" to "Chat media backend update is required. Ask the trainer/admin to run the latest RS KICKBOXING Supabase repair.",
        "permission_error" to "Chat media permission was not accepted by the server."
    )
    val nl=en+mapOf("image" to "Afbeelding","video" to "Korte video","remove" to "Bijlage verwijderen","send" to "Versturen","sending" to "Versturen…","uploading" to "Bijlage uploaden…","attachment_ready" to "Bijlage klaar","open_video" to "Korte video afspelen","download_error" to "Bijlage kon niet worden geladen.","upload_error" to "Bijlage kon niet worden geüpload.","message_or_media" to "Schrijf een bericht of voeg een afbeelding/video toe.","max_video" to "Korte video's: maximaal 30 seconden / 30 MB.")
    val pt=en+mapOf("image" to "Imagem","video" to "Vídeo curto","remove" to "Remover anexo","send" to "Enviar","sending" to "A enviar…","uploading" to "A carregar anexo…","attachment_ready" to "Anexo pronto","open_video" to "Reproduzir vídeo curto","download_error" to "Não foi possível carregar o anexo.","upload_error" to "Não foi possível enviar o anexo.","message_or_media" to "Escreve uma mensagem ou adiciona imagem/vídeo.","max_video" to "Vídeos curtos: máximo 30 segundos / 30 MB.")
    val es=en+mapOf("image" to "Imagen","video" to "Vídeo corto","remove" to "Quitar adjunto","send" to "Enviar","sending" to "Enviando…","uploading" to "Subiendo adjunto…","attachment_ready" to "Adjunto listo","open_video" to "Reproducir vídeo corto","download_error" to "No se pudo cargar el adjunto.","upload_error" to "No se pudo subir el adjunto.","message_or_media" to "Escribe un mensaje o añade imagen/vídeo.","max_video" to "Vídeos cortos: máximo 30 segundos / 30 MB.")
    val fr=en+mapOf("image" to "Image","video" to "Vidéo courte","remove" to "Supprimer la pièce jointe","send" to "Envoyer","sending" to "Envoi…","uploading" to "Import de la pièce jointe…","attachment_ready" to "Pièce jointe prête","open_video" to "Lire la vidéo courte","download_error" to "Impossible de charger la pièce jointe.","upload_error" to "Impossible d’envoyer la pièce jointe.","message_or_media" to "Écris un message ou ajoute une image/vidéo.","max_video" to "Vidéos courtes : maximum 30 secondes / 30 Mo.")
    val de=en+mapOf("image" to "Bild","video" to "Kurzvideo","remove" to "Anhang entfernen","send" to "Senden","sending" to "Senden…","uploading" to "Anhang wird hochgeladen…","attachment_ready" to "Anhang bereit","open_video" to "Kurzvideo abspielen","download_error" to "Anhang konnte nicht geladen werden.","upload_error" to "Anhang konnte nicht hochgeladen werden.","message_or_media" to "Nachricht schreiben oder Bild/Video hinzufügen.","max_video" to "Kurzvideos: maximal 30 Sekunden / 30 MB.")
    val it=en+mapOf("image" to "Immagine","video" to "Video breve","remove" to "Rimuovi allegato","send" to "Invia","sending" to "Invio…","uploading" to "Caricamento allegato…","attachment_ready" to "Allegato pronto","open_video" to "Riproduci video breve","download_error" to "Impossibile caricare l'allegato.","upload_error" to "Impossibile inviare l'allegato.","message_or_media" to "Scrivi un messaggio o aggiungi immagine/video.","max_video" to "Video brevi: massimo 30 secondi / 30 MB.")
    val pl=en+mapOf("image" to "Obraz","video" to "Krótki film","remove" to "Usuń załącznik","send" to "Wyślij","sending" to "Wysyłanie…","uploading" to "Przesyłanie załącznika…","attachment_ready" to "Załącznik gotowy","open_video" to "Odtwórz krótki film","download_error" to "Nie udało się wczytać załącznika.","upload_error" to "Nie udało się przesłać załącznika.","message_or_media" to "Napisz wiadomość lub dodaj obraz/film.","max_video" to "Krótkie filmy: maks. 30 sekund / 30 MB.")
    val tr=en+mapOf("image" to "Görsel","video" to "Kısa video","remove" to "Eki kaldır","send" to "Gönder","sending" to "Gönderiliyor…","uploading" to "Ek yükleniyor…","attachment_ready" to "Ek hazır","open_video" to "Kısa videoyu oynat","download_error" to "Ek yüklenemedi.","upload_error" to "Ek gönderilemedi.","message_or_media" to "Mesaj yaz veya görsel/video ekle.","max_video" to "Kısa videolar: en fazla 30 saniye / 30 MB.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsChatAttachmentPreviewV92(
    c:RsPalette,
    lang:RsLang,
    mediaPath:String?,
    mediaKind:String?,
    mediaName:String?
){
    if(mediaPath.isNullOrBlank()||mediaKind.isNullOrBlank())return
    val context=LocalContext.current
    val previewStore=remember{RsStore(context)}
    val autoPreview=previewStore.b("chat_auto_media_preview_v162",true)
    var requested by remember(mediaPath,autoPreview){mutableStateOf(autoPreview)}
    var localUri by remember(mediaPath){mutableStateOf("")}
    var error by remember(mediaPath){mutableStateOf("")}

    LaunchedEffect(mediaPath,requested){
        if(requested && localUri.isBlank()){
            rsChatMediaLocalUriV92(context,mediaPath)
                .onSuccess{localUri=it}
                .onFailure{error=rsChatMediaT(lang,"download_error")}
        }
    }

    if(!requested){
        OutlinedButton(
            onClick={requested=true},
            modifier=Modifier.fillMaxWidth()
        ){
            Text(
                "Load "+when(mediaKind){"IMAGE"->"image";"VIDEO"->"video";"AUDIO"->"audio";else->"file"},
                fontSize=9.sp
            )
        }
        return
    }

    if(error.isNotBlank()){
        Text(error,color=c.muted,fontSize=9.sp)
        return
    }
    if(localUri.isBlank()){
        LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
        return
    }

    when(mediaKind){
        "IMAGE"->{
            var fullImage by remember(localUri){mutableStateOf(false)}
            val bitmap=remember(localUri){
                runCatching{
                    val uri=Uri.parse(localUri)
                    BitmapFactory.decodeFile(uri.path)?.asImageBitmap()
                }.getOrNull()
            }
            if(bitmap!=null){
                Surface(
                    shape=RoundedCornerShape(18.dp),
                    color=c.panel.copy(alpha=.72f),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
                    modifier=Modifier.fillMaxWidth().clickable{fullImage=true}
                ){
                    Image(
                        bitmap=bitmap,
                        contentDescription=mediaName?:rsChatMediaT(lang,"image"),
                        contentScale=ContentScale.Crop,
                        modifier=Modifier.fillMaxWidth().heightIn(min=160.dp,max=340.dp)
                    )
                }
                if(fullImage){
                    androidx.compose.ui.window.Dialog(onDismissRequest={fullImage=false}){
                        Surface(
                            color=androidx.compose.ui.graphics.Color.Black,
                            shape=RoundedCornerShape(18.dp),
                            modifier=Modifier.fillMaxWidth().fillMaxHeight(.90f)
                        ){
                            Box(Modifier.fillMaxSize()){
                                Image(
                                    bitmap=bitmap,
                                    contentDescription=mediaName?:rsChatMediaT(lang,"image"),
                                    contentScale=ContentScale.Fit,
                                    modifier=Modifier.fillMaxSize()
                                )
                                TextButton(
                                    onClick={fullImage=false},
                                    modifier=Modifier.align(Alignment.TopEnd).padding(8.dp)
                                ){Text("×",fontSize=26.sp,color=androidx.compose.ui.graphics.Color.White)}
                            }
                        }
                    }
                }
            }else Text(rsChatMediaT(lang,"download_error"),color=c.muted,fontSize=9.sp)
        }
        "VIDEO","AUDIO"->{
            val player=remember(localUri){
                ExoPlayer.Builder(context).build().apply{
                    setMediaItem(MediaItem.fromUri(Uri.parse(localUri)))
                    repeatMode=Player.REPEAT_MODE_OFF
                    prepare()
                }
            }
            DisposableEffect(player){onDispose{player.release()}}
            Surface(
                shape=RoundedCornerShape(18.dp),
                color=androidx.compose.ui.graphics.Color.Black.copy(alpha=.72f),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
                modifier=Modifier.fillMaxWidth()
            ){
                AndroidView(
                    factory={ctx->
                        PlayerView(ctx).apply{
                            this.player=player
                            useController=true
                        }
                    },
                    update={it.player=player},
                    modifier=if(mediaKind=="VIDEO")
                        Modifier.fillMaxWidth().heightIn(min=190.dp,max=360.dp)
                    else Modifier.fillMaxWidth().height(86.dp)
                )
            }
            if(!mediaName.isNullOrBlank()){
                Text(mediaName,color=c.muted,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
        else->{
            OutlinedButton(
                onClick={
                    val file=File(Uri.parse(localUri).path?:return@OutlinedButton)
                    val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
                    val lower=mediaName.orEmpty().lowercase()
                    val mime=when{
                        lower.endsWith(".pdf")->"application/pdf"
                        lower.endsWith(".txt")->"text/plain"
                        lower.endsWith(".doc")->"application/msword"
                        lower.endsWith(".docx")->"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        else->"*/*"
                    }
                    val intent=Intent(Intent.ACTION_VIEW).apply{
                        setDataAndType(uri,mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching{context.startActivity(intent)}
                        .onFailure{error=rsChatMediaT(lang,"file_open_error")}
                },
                modifier=Modifier.fillMaxWidth()
            ){
                Text("⌑ "+(mediaName?:rsChatMediaT(lang,"file")),maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun RsChatComposerV92(
    c:RsPalette,
    lang:RsLang,
    scopeType:String,
    scopeId:String,
    enabled:Boolean=true,
    replyPreview:String?=null,
    onClearReply:()->Unit={},
    onSent:()->Unit,
    onStatus:(String)->Unit,
    onSend:suspend (String,RsChatAttachmentV92?)->Result<Unit>
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var draft by remember(scopeId){mutableStateOf("")}
    var picked by remember(scopeId){mutableStateOf<Uri?>(null)}
    var pickedKind by remember(scopeId){mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var attachmentMenu by remember{mutableStateOf(false)}
    var recording by remember{mutableStateOf(false)}
    var voiceFile by remember{mutableStateOf<File?>(null)}
    var recorder by remember{mutableStateOf<MediaRecorder?>(null)}

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            picked=uri
            pickedKind="IMAGE"
            onStatus("")
        }
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            picked=uri
            pickedKind="VIDEO"
            onStatus("")
        }
    }

    val startVoice:()->Unit = start@{
        if(recording||busy)return@start
        runCatching{
            val dir=File(context.cacheDir,"rs_voice_messages").apply{mkdirs()}
            val file=File(dir,"voice_"+UUID.randomUUID()+".m4a")
            val next=MediaRecorder().apply{
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            voiceFile=file
            recorder=next
            recording=true
            onStatus(rsChatMediaT(lang,"recording"))
        }.onFailure{
            recorder?.release()
            recorder=null
            recording=false
            onStatus(rsChatMediaT(lang,"voice_unavailable"))
        }
    }

    val micPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted)startVoice() else onStatus(rsChatMediaT(lang,"mic_permission"))
    }

    fun stopVoice(){
        if(!recording)return
        val file=voiceFile
        val ok=runCatching{recorder?.stop()}.isSuccess
        runCatching{recorder?.release()}
        recorder=null
        recording=false
        if(ok&&file!=null&&file.exists()&&file.length()>0){
            picked=Uri.fromFile(file)
            pickedKind="AUDIO"
            onStatus("")
        }else{
            runCatching{file?.delete()}
            onStatus(rsChatMediaT(lang,"voice_too_short"))
        }
    }

    DisposableEffect(Unit){
        onDispose{
            if(recording)runCatching{recorder?.stop()}
            runCatching{recorder?.release()}
            recorder=null
        }
    }

    fun sendMessage(){
        if(draft.trim().isBlank()&&picked==null){
            onStatus(rsChatMediaT(lang,"message_or_media"))
            return
        }
        if(busy||recording)return
        busy=true
        val body=draft.trim()
        val source=picked
        scope.launch{
            var uploaded:RsChatAttachmentV92?=null
            if(source!=null){
                onStatus(rsChatMediaT(lang,"uploading"))
                val result=rsUploadChatMediaV92(context,source,scopeType,scopeId)
                if(result.isFailure){
                    onStatus(rsChatBackendFriendlyErrorV121(lang,result.exceptionOrNull()))
                    busy=false
                    return@launch
                }
                uploaded=result.getOrNull()
            }
            onSend(body,uploaded)
                .onSuccess{
                    draft=""
                    picked=null
                    pickedKind=""
                    voiceFile=null
                    onStatus("")
                    onSent()
                }
                .onFailure{error->
                    if(uploaded!=null)rsDeleteChatMediaV108(uploaded.path)
                    onStatus(rsChatBackendFriendlyErrorV121(lang,error))
                }
            busy=false
        }
    }

    Surface(
        color=androidx.compose.ui.graphics.Color.Black.copy(alpha=.82f),
        contentColor=c.text,
        shape=RoundedCornerShape(30.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.52f)),
        tonalElevation=14.dp,
        modifier=Modifier.fillMaxWidth()
    ){
        Column(
            Modifier.fillMaxWidth().padding(9.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ){
            if(!replyPreview.isNullOrBlank()){
                Surface(
                    color=c.gold.copy(alpha=.08f),
                    shape=RoundedCornerShape(14.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.20f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Column(Modifier.weight(1f)){
                            Text("REPLYING TO",color=c.gold,fontSize=7.sp,fontWeight=FontWeight.Black)
                            Text(replyPreview.take(140),color=c.text,fontSize=9.sp,maxLines=2)
                        }
                        TextButton(onClick=onClearReply,contentPadding=PaddingValues(4.dp)){Text("×",fontSize=20.sp)}
                    }
                }
            }
            if(picked!=null){
                Surface(
                    color=c.gold.copy(alpha=.09f),
                    shape=RoundedCornerShape(17.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.20f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        Text(
                            when(pickedKind){"VIDEO"->"▶";"AUDIO"->"🎙";else->"▣"},
                            color=c.bright,fontSize=15.sp
                        )
                        Text(
                            rsChatMediaT(lang,"attachment_ready")+" · "+pickedKind,
                            color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp,
                            modifier=Modifier.weight(1f)
                        )
                        TextButton(
                            onClick={
                                if(pickedKind=="AUDIO")runCatching{voiceFile?.delete()}
                                picked=null;pickedKind="";voiceFile=null
                            },
                            enabled=!busy,
                            contentPadding=PaddingValues(horizontal=6.dp,vertical=0.dp)
                        ){Text("×",fontSize=20.sp,color=c.muted)}
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ){
                Box{
                    OutlinedButton(
                        onClick={attachmentMenu=true},
                        enabled=enabled&&!busy&&!recording,
                        modifier=Modifier.size(46.dp),
                        shape=CircleShape,
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.72f)),
                        contentPadding=PaddingValues(0.dp)
                    ){Text("+",fontSize=24.sp,color=c.bright,fontWeight=FontWeight.Black)}
                }

                OutlinedTextField(
                    value=draft,
                    onValueChange={draft=it.take(1200)},
                    placeholder={Text(if(recording)rsChatMediaT(lang,"recording") else rsChatMediaT(lang,"message"),fontSize=11.sp)},
                    modifier=Modifier.weight(1f),
                    minLines=1,maxLines=4,
                    enabled=enabled&&!busy&&!recording,
                    shape=RoundedCornerShape(22.dp),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedBorderColor=c.gold.copy(alpha=.55f),
                        unfocusedBorderColor=c.gold.copy(alpha=.18f),
                        focusedContainerColor=c.panel.copy(alpha=.52f),
                        unfocusedContainerColor=c.panel.copy(alpha=.42f)
                    )
                )

                OutlinedButton(
                    onClick={
                        if(recording)stopVoice()
                        else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startVoice()
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    enabled=enabled&&!busy,
                    modifier=Modifier.size(44.dp),
                    shape=CircleShape,
                    border=BorderStroke(1.dp,if(recording)c.bright else c.gold.copy(alpha=.54f)),
                    contentPadding=PaddingValues(0.dp)
                ){Text(if(recording)"■" else "🎙",fontSize=15.sp,color=c.bright)}

                Button(
                    onClick={sendMessage()},
                    enabled=enabled&&!busy&&!recording&&(draft.trim().isNotBlank()||picked!=null),
                    modifier=Modifier.size(44.dp),
                    shape=CircleShape,
                    contentPadding=PaddingValues(0.dp),
                    colors=ButtonDefaults.buttonColors(
                        containerColor=c.bright,
                        contentColor=androidx.compose.ui.graphics.Color.Black
                    )
                ){Text(if(busy)"…" else "➤",fontWeight=FontWeight.Black,fontSize=16.sp)}
            }
        }
    }

    if(attachmentMenu){
        Dialog(
            onDismissRequest={attachmentMenu=false},
            properties=DialogProperties(usePlatformDefaultWidth=false)
        ){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
                Surface(
                    color=androidx.compose.ui.graphics.Color.Black.copy(alpha=.96f),
                    shape=RoundedCornerShape(topStart=30.dp,topEnd=30.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.56f)),
                    tonalElevation=18.dp,
                    modifier=Modifier.padding(start=10.dp,end=10.dp,bottom=6.dp).fillMaxWidth().navigationBarsPadding()
                ){
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement=Arrangement.spacedBy(14.dp)
                    ){
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Column(Modifier.weight(1f)){
                                Text("RS MEDIA COMMAND",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp,letterSpacing=.8.sp)
                                Text("Choose what you want to send",color=c.muted,fontSize=9.sp)
                            }
                            TextButton(onClick={attachmentMenu=false}){Text("×",color=c.bright,fontSize=25.sp)}
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.spacedBy(12.dp)
                        ){
                            Surface(
                                color=c.gold.copy(alpha=.09f),
                                shape=RoundedCornerShape(24.dp),
                                border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                                modifier=Modifier.weight(1f).clickable{
                                    attachmentMenu=false
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ){
                                Column(
                                    Modifier.padding(vertical=22.dp,horizontal=12.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally,
                                    verticalArrangement=Arrangement.spacedBy(8.dp)
                                ){
                                    Surface(
                                        color=c.gold.copy(alpha=.12f),
                                        shape=CircleShape,
                                        border=BorderStroke(1.dp,c.gold.copy(alpha=.50f)),
                                        modifier=Modifier.size(54.dp)
                                    ){
                                        Box(contentAlignment=Alignment.Center){Text("▣",color=c.bright,fontSize=23.sp)}
                                    }
                                    Text(rsChatMediaT(lang,"photo").uppercase(),color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                                    Text("Gallery · camera image",color=c.muted,fontSize=8.sp)
                                }
                            }
                            Surface(
                                color=c.gold.copy(alpha=.09f),
                                shape=RoundedCornerShape(24.dp),
                                border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                                modifier=Modifier.weight(1f).clickable{
                                    attachmentMenu=false
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                            ){
                                Column(
                                    Modifier.padding(vertical=22.dp,horizontal=12.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally,
                                    verticalArrangement=Arrangement.spacedBy(8.dp)
                                ){
                                    Surface(
                                        color=c.gold.copy(alpha=.12f),
                                        shape=CircleShape,
                                        border=BorderStroke(1.dp,c.gold.copy(alpha=.50f)),
                                        modifier=Modifier.size(54.dp)
                                    ){
                                        Box(contentAlignment=Alignment.Center){Text("▶",color=c.bright,fontSize=21.sp)}
                                    }
                                    Text(rsChatMediaT(lang,"video").uppercase(),color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                                    Text("Training clip · technique",color=c.muted,fontSize=8.sp)
                                }
                            }
                        }
                        Surface(
                            color=androidx.compose.ui.graphics.Color(0xFF58C9FF).copy(alpha=.06f),
                            shape=RoundedCornerShape(16.dp),
                            border=BorderStroke(1.dp,androidx.compose.ui.graphics.Color(0xFF58C9FF).copy(alpha=.18f)),
                            modifier=Modifier.fillMaxWidth()
                        ){
                            Text(
                                "AI TRAINER READY · media can be used for technique coaching",
                                color=androidx.compose.ui.graphics.Color(0xFF58C9FF),
                                fontWeight=FontWeight.Bold,
                                fontSize=8.sp,
                                modifier=Modifier.padding(horizontal=12.dp,vertical=9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun rsGroupChatT(lang:RsLang,key:String):String{
    val en=mapOf(
        "manage_sub" to "Manage training groups and open their member chats.",
        "student_sub" to "Join training groups and chat with your group members.",
        "syncing" to "Syncing groups…","connected" to "Cloud groups connected",
        "created" to "Group created.","create_error" to "Could not create group.",
        "creating" to "Creating…","members" to "members","open_chat" to "Open group chat",
        "left" to "Left group.","joined" to "Joined group.","wait" to "Please wait…",
        "update_error" to "Could not update group.","deleted" to "Group deleted.",
        "delete_error" to "Could not delete group.","chat_sub" to "Private member-only group chat.",
        "loading_chat" to "Loading group chat…","chat_ready" to "Group chat connected",
        "no_messages" to "No messages yet. Start the group conversation.",
        "back" to "Back to groups"
    )
    val nl=en+mapOf("manage_sub" to "Beheer trainingsgroepen en open hun ledenchat.","student_sub" to "Word lid van trainingsgroepen en chat met groepsleden.","syncing" to "Groepen synchroniseren…","connected" to "Cloudgroepen verbonden","created" to "Groep aangemaakt.","create_error" to "Groep kon niet worden aangemaakt.","creating" to "Aanmaken…","members" to "leden","open_chat" to "Groepschat openen","left" to "Groep verlaten.","joined" to "Lid geworden van groep.","wait" to "Even wachten…","update_error" to "Groep kon niet worden bijgewerkt.","deleted" to "Groep verwijderd.","delete_error" to "Groep kon niet worden verwijderd.","chat_sub" to "Privé groepschat alleen voor leden.","loading_chat" to "Groepschat laden…","chat_ready" to "Groepschat verbonden","no_messages" to "Nog geen berichten. Start het gesprek.","back" to "Terug naar groepen")
    val pt=en+mapOf("manage_sub" to "Gere grupos de treino e abre os chats dos membros.","student_sub" to "Entra em grupos de treino e conversa com os membros.","syncing" to "A sincronizar grupos…","connected" to "Grupos cloud ligados","created" to "Grupo criado.","create_error" to "Não foi possível criar o grupo.","creating" to "A criar…","members" to "membros","open_chat" to "Abrir chat do grupo","left" to "Saíste do grupo.","joined" to "Entraste no grupo.","wait" to "Aguarda…","update_error" to "Não foi possível atualizar o grupo.","deleted" to "Grupo eliminado.","delete_error" to "Não foi possível eliminar o grupo.","chat_sub" to "Chat privado apenas para membros do grupo.","loading_chat" to "A carregar chat do grupo…","chat_ready" to "Chat do grupo ligado","no_messages" to "Ainda não há mensagens. Inicia a conversa.","back" to "Voltar aos grupos")
    val es=en+mapOf("manage_sub" to "Gestiona grupos de entrenamiento y abre sus chats.","student_sub" to "Únete a grupos de entrenamiento y chatea con sus miembros.","syncing" to "Sincronizando grupos…","connected" to "Grupos cloud conectados","created" to "Grupo creado.","create_error" to "No se pudo crear el grupo.","creating" to "Creando…","members" to "miembros","open_chat" to "Abrir chat del grupo","left" to "Has salido del grupo.","joined" to "Te has unido al grupo.","wait" to "Espera…","update_error" to "No se pudo actualizar el grupo.","deleted" to "Grupo eliminado.","delete_error" to "No se pudo eliminar el grupo.","chat_sub" to "Chat privado solo para miembros del grupo.","loading_chat" to "Cargando chat del grupo…","chat_ready" to "Chat del grupo conectado","no_messages" to "Aún no hay mensajes. Inicia la conversación.","back" to "Volver a grupos")
    val fr=en+mapOf("manage_sub" to "Gère les groupes d’entraînement et ouvre leurs chats.","student_sub" to "Rejoins des groupes d’entraînement et discute avec les membres.","syncing" to "Synchronisation des groupes…","connected" to "Groupes cloud connectés","created" to "Groupe créé.","create_error" to "Impossible de créer le groupe.","creating" to "Création…","members" to "membres","open_chat" to "Ouvrir le chat du groupe","left" to "Groupe quitté.","joined" to "Groupe rejoint.","wait" to "Patiente…","update_error" to "Impossible de mettre à jour le groupe.","deleted" to "Groupe supprimé.","delete_error" to "Impossible de supprimer le groupe.","chat_sub" to "Chat privé réservé aux membres du groupe.","loading_chat" to "Chargement du chat…","chat_ready" to "Chat de groupe connecté","no_messages" to "Aucun message. Lance la conversation.","back" to "Retour aux groupes")
    val de=en+mapOf("manage_sub" to "Trainingsgruppen verwalten und Mitgliederchats öffnen.","student_sub" to "Trainingsgruppen beitreten und mit Mitgliedern chatten.","syncing" to "Gruppen werden synchronisiert…","connected" to "Cloud-Gruppen verbunden","created" to "Gruppe erstellt.","create_error" to "Gruppe konnte nicht erstellt werden.","creating" to "Erstellen…","members" to "Mitglieder","open_chat" to "Gruppenchat öffnen","left" to "Gruppe verlassen.","joined" to "Gruppe beigetreten.","wait" to "Bitte warten…","update_error" to "Gruppe konnte nicht aktualisiert werden.","deleted" to "Gruppe gelöscht.","delete_error" to "Gruppe konnte nicht gelöscht werden.","chat_sub" to "Privater Chat nur für Gruppenmitglieder.","loading_chat" to "Gruppenchat wird geladen…","chat_ready" to "Gruppenchat verbunden","no_messages" to "Noch keine Nachrichten. Starte die Unterhaltung.","back" to "Zurück zu Gruppen")
    val it=en+mapOf("manage_sub" to "Gestisci gruppi di allenamento e apri le chat membri.","student_sub" to "Unisciti ai gruppi e chatta con i membri.","syncing" to "Sincronizzazione gruppi…","connected" to "Gruppi cloud collegati","created" to "Gruppo creato.","create_error" to "Impossibile creare il gruppo.","creating" to "Creazione…","members" to "membri","open_chat" to "Apri chat gruppo","left" to "Hai lasciato il gruppo.","joined" to "Sei entrato nel gruppo.","wait" to "Attendi…","update_error" to "Impossibile aggiornare il gruppo.","deleted" to "Gruppo eliminato.","delete_error" to "Impossibile eliminare il gruppo.","chat_sub" to "Chat privata solo per i membri del gruppo.","loading_chat" to "Caricamento chat gruppo…","chat_ready" to "Chat gruppo collegata","no_messages" to "Nessun messaggio. Inizia la conversazione.","back" to "Torna ai gruppi")
    val pl=en+mapOf("manage_sub" to "Zarządzaj grupami treningowymi i otwieraj czaty członków.","student_sub" to "Dołączaj do grup treningowych i rozmawiaj z członkami.","syncing" to "Synchronizacja grup…","connected" to "Grupy w chmurze połączone","created" to "Grupa utworzona.","create_error" to "Nie udało się utworzyć grupy.","creating" to "Tworzenie…","members" to "członków","open_chat" to "Otwórz czat grupy","left" to "Opuszczono grupę.","joined" to "Dołączono do grupy.","wait" to "Poczekaj…","update_error" to "Nie udało się zaktualizować grupy.","deleted" to "Grupa usunięta.","delete_error" to "Nie udało się usunąć grupy.","chat_sub" to "Prywatny czat tylko dla członków grupy.","loading_chat" to "Ładowanie czatu grupy…","chat_ready" to "Czat grupy połączony","no_messages" to "Brak wiadomości. Rozpocznij rozmowę.","back" to "Wróć do grup")
    val tr=en+mapOf("manage_sub" to "Antrenman gruplarını yönet ve üye sohbetlerini aç.","student_sub" to "Antrenman gruplarına katıl ve üyelerle sohbet et.","syncing" to "Gruplar eşitleniyor…","connected" to "Bulut grupları bağlı","created" to "Grup oluşturuldu.","create_error" to "Grup oluşturulamadı.","creating" to "Oluşturuluyor…","members" to "üye","open_chat" to "Grup sohbetini aç","left" to "Gruptan ayrıldın.","joined" to "Gruba katıldın.","wait" to "Lütfen bekle…","update_error" to "Grup güncellenemedi.","deleted" to "Grup silindi.","delete_error" to "Grup silinemedi.","chat_sub" to "Yalnızca grup üyelerine özel sohbet.","loading_chat" to "Grup sohbeti yükleniyor…","chat_ready" to "Grup sohbeti bağlı","no_messages" to "Henüz mesaj yok. Sohbeti başlat.","back" to "Gruplara dön")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}


suspend fun rsHideGroupMessageV162(messageId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_hide_group_message",
        buildJsonObject{put("p_message_id",messageId)}
    )
    Unit
}

suspend fun rsEditGroupMessageV162(messageId:String,body:String):Result<Unit> = runCatching{
    val clean=body.trim()
    require(clean.isNotBlank()){"Message cannot be empty."}
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_edit_group_message",
        buildJsonObject{
            put("p_message_id",messageId)
            put("p_body",clean)
        }
    )
    Unit
}

suspend fun rsDeleteGroupMessageForEveryoneV162(messageId:String,mediaPath:String?):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_delete_group_message_for_everyone",
        buildJsonObject{put("p_message_id",messageId)}
    )
    if(!mediaPath.isNullOrBlank())runCatching{rsDeleteChatMediaV108(mediaPath)}
    Unit
}


suspend fun rsHideGroupConversationForMeV162(messages:List<RsCloudGroupMessageV92>):Result<Unit> = runCatching{
    messages.forEach{message->
        rsHideGroupMessageV162(message.id).getOrThrow()
    }
    Unit
}

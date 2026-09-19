package com.rskickbox.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
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
    @SerialName("created_at") val createdAt:String
)

private fun rsChatFileNameV92(context:Context,uri:Uri):String{
    var result="attachment"
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())result=cur.getString(0)?:result
        }
    }
    return result.take(120)
}

private fun rsChatVideoDurationMsV92(context:Context,uri:Uri):Long{
    val r=MediaMetadataRetriever()
    return try{
        r.setDataSource(context,uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L
    }finally{runCatching{r.release()}}
}

private fun rsChatImageBytesV92(context:Context,uri:Uri):ByteArray{
    val original=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}
        ?:error("Could not read image.")
    val maxSide=max(original.width,original.height).coerceAtLeast(1)
    val scale=(1600f/maxSide).coerceAtMost(1f)
    val width=(original.width*scale).toInt().coerceAtLeast(1)
    val height=(original.height*scale).toInt().coerceAtLeast(1)
    val bitmap=if(width!=original.width||height!=original.height)
        Bitmap.createScaledBitmap(original,width,height,true)
    else original
    val out=ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG,86,out)
    if(bitmap!==original)bitmap.recycle()
    original.recycle()
    return out.toByteArray()
}

suspend fun rsUploadChatMediaV92(
    context:Context,
    uri:Uri,
    scopeType:String,
    scopeId:String
):Result<RsChatAttachmentV92> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(scopeType=="coach"||scopeType=="group"){"Invalid chat scope."}
    val mime=context.contentResolver.getType(uri)?.lowercase().orEmpty()
    val kind=if(mime.startsWith("video/"))"VIDEO" else "IMAGE"
    val name=rsChatFileNameV92(context,uri)

    if(kind=="VIDEO"){
        val duration=rsChatVideoDurationMsV92(context,uri)
        require(duration in 1..30_000){"Short videos can be up to 30 seconds."}
    }

    val bytes=if(kind=="IMAGE"){
        rsChatImageBytesV92(context,uri)
    }else{
        context.contentResolver.openInputStream(uri)?.use{it.readBytes()}
            ?:error("Could not read video.")
    }
    require(bytes.size<=30*1024*1024){"Chat attachment must be 30 MB or smaller."}

    val ext=when{
        kind=="IMAGE"->"jpg"
        mime=="video/webm"->"webm"
        mime=="video/quicktime"->"mov"
        mime=="video/3gpp"->"3gp"
        else->"mp4"
    }
    val path=scopeType+"/"+scopeId+"/"+UUID.randomUUID().toString()+"."+ext
    val contentType=if(kind=="IMAGE")ContentType.Image.JPEG
        else runCatching{ContentType.parse(if(mime.isBlank())"video/mp4" else mime)}.getOrDefault(ContentType.Video.MP4)

    client.storage.from(RS_CHAT_MEDIA_BUCKET_V92).upload(path,bytes){
        upsert=false
        this.contentType=contentType
    }
    RsChatAttachmentV92(path,kind,name)
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
        "rs_group_message_feed",
        buildJsonObject{put("p_group_id",groupId)}
    ).decodeList<RsCloudGroupMessageV92>()
}

suspend fun rsSendCloudGroupMessageV92(
    groupId:String,
    body:String,
    attachment:RsChatAttachmentV92?
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(body.trim().isNotBlank()||attachment!=null){"Write a message or add an attachment."}
    client.postgrest.rpc(
        "rs_send_group_message",
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
        }
    )
    Unit
}

fun rsChatMediaT(lang:RsLang,key:String):String{
    val en=mapOf(
        "image" to "Image","video" to "Short video","remove" to "Remove attachment",
        "send" to "Send","sending" to "Sending…","uploading" to "Uploading attachment…",
        "attachment_ready" to "Attachment ready","open_video" to "Play short video",
        "download_error" to "Could not load attachment.","message_or_media" to "Write a message or add an image/video.",
        "max_video" to "Short videos: maximum 30 seconds / 30 MB."
    )
    val nl=en+mapOf("image" to "Afbeelding","video" to "Korte video","remove" to "Bijlage verwijderen","send" to "Versturen","sending" to "Versturen…","uploading" to "Bijlage uploaden…","attachment_ready" to "Bijlage klaar","open_video" to "Korte video afspelen","download_error" to "Bijlage kon niet worden geladen.","message_or_media" to "Schrijf een bericht of voeg een afbeelding/video toe.","max_video" to "Korte video's: maximaal 30 seconden / 30 MB.")
    val pt=en+mapOf("image" to "Imagem","video" to "Vídeo curto","remove" to "Remover anexo","send" to "Enviar","sending" to "A enviar…","uploading" to "A carregar anexo…","attachment_ready" to "Anexo pronto","open_video" to "Reproduzir vídeo curto","download_error" to "Não foi possível carregar o anexo.","message_or_media" to "Escreve uma mensagem ou adiciona imagem/vídeo.","max_video" to "Vídeos curtos: máximo 30 segundos / 30 MB.")
    val es=en+mapOf("image" to "Imagen","video" to "Vídeo corto","remove" to "Quitar adjunto","send" to "Enviar","sending" to "Enviando…","uploading" to "Subiendo adjunto…","attachment_ready" to "Adjunto listo","open_video" to "Reproducir vídeo corto","download_error" to "No se pudo cargar el adjunto.","message_or_media" to "Escribe un mensaje o añade imagen/vídeo.","max_video" to "Vídeos cortos: máximo 30 segundos / 30 MB.")
    val fr=en+mapOf("image" to "Image","video" to "Vidéo courte","remove" to "Supprimer la pièce jointe","send" to "Envoyer","sending" to "Envoi…","uploading" to "Import de la pièce jointe…","attachment_ready" to "Pièce jointe prête","open_video" to "Lire la vidéo courte","download_error" to "Impossible de charger la pièce jointe.","message_or_media" to "Écris un message ou ajoute une image/vidéo.","max_video" to "Vidéos courtes : maximum 30 secondes / 30 Mo.")
    val de=en+mapOf("image" to "Bild","video" to "Kurzvideo","remove" to "Anhang entfernen","send" to "Senden","sending" to "Senden…","uploading" to "Anhang wird hochgeladen…","attachment_ready" to "Anhang bereit","open_video" to "Kurzvideo abspielen","download_error" to "Anhang konnte nicht geladen werden.","message_or_media" to "Nachricht schreiben oder Bild/Video hinzufügen.","max_video" to "Kurzvideos: maximal 30 Sekunden / 30 MB.")
    val it=en+mapOf("image" to "Immagine","video" to "Video breve","remove" to "Rimuovi allegato","send" to "Invia","sending" to "Invio…","uploading" to "Caricamento allegato…","attachment_ready" to "Allegato pronto","open_video" to "Riproduci video breve","download_error" to "Impossibile caricare l'allegato.","message_or_media" to "Scrivi un messaggio o aggiungi immagine/video.","max_video" to "Video brevi: massimo 30 secondi / 30 MB.")
    val pl=en+mapOf("image" to "Obraz","video" to "Krótki film","remove" to "Usuń załącznik","send" to "Wyślij","sending" to "Wysyłanie…","uploading" to "Przesyłanie załącznika…","attachment_ready" to "Załącznik gotowy","open_video" to "Odtwórz krótki film","download_error" to "Nie udało się wczytać załącznika.","message_or_media" to "Napisz wiadomość lub dodaj obraz/film.","max_video" to "Krótkie filmy: maks. 30 sekund / 30 MB.")
    val tr=en+mapOf("image" to "Görsel","video" to "Kısa video","remove" to "Eki kaldır","send" to "Gönder","sending" to "Gönderiliyor…","uploading" to "Ek yükleniyor…","attachment_ready" to "Ek hazır","open_video" to "Kısa videoyu oynat","download_error" to "Ek yüklenemedi.","message_or_media" to "Mesaj yaz veya görsel/video ekle.","max_video" to "Kısa videolar: en fazla 30 saniye / 30 MB.")
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
    var localUri by remember(mediaPath){mutableStateOf("")}
    var error by remember(mediaPath){mutableStateOf("")}

    LaunchedEffect(mediaPath){
        rsChatMediaLocalUriV92(context,mediaPath)
            .onSuccess{localUri=it}
            .onFailure{error=rsChatMediaT(lang,"download_error")}
    }

    if(error.isNotBlank()){
        Text(error,color=c.muted,fontSize=9.sp)
        return
    }
    if(localUri.isBlank()){
        LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
        return
    }

    if(mediaKind=="IMAGE"){
        val bitmap=remember(localUri){
            runCatching{
                val uri=Uri.parse(localUri)
                BitmapFactory.decodeFile(uri.path)?.asImageBitmap()
            }.getOrNull()
        }
        if(bitmap!=null){
            Image(
                bitmap=bitmap,
                contentDescription=mediaName?:rsChatMediaT(lang,"image"),
                contentScale=ContentScale.Crop,
                modifier=Modifier.fillMaxWidth().heightIn(min=120.dp,max=280.dp)
            )
        }
    }else{
        OutlinedButton(
            onClick={
                val file=File(Uri.parse(localUri).path?:return@OutlinedButton)
                val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
                val intent=Intent(Intent.ACTION_VIEW).apply{
                    setDataAndType(uri,"video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching{context.startActivity(intent)}
            },
            modifier=Modifier.fillMaxWidth()
        ){
            Text("▶ "+rsChatMediaT(lang,"open_video")+" · "+(mediaName?:"video"),maxLines=1,overflow=TextOverflow.Ellipsis)
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

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        picked=uri
        pickedKind=if(uri==null)"" else "IMAGE"
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        picked=uri
        pickedKind=if(uri==null)"" else "VIDEO"
    }

    RsPanel(c){
        OutlinedTextField(
            draft,
            {draft=it.take(1200)},
            label={Text(rsChatMediaT(lang,"message_or_media"))},
            modifier=Modifier.fillMaxWidth(),
            minLines=2,
            maxLines=6,
            enabled=enabled&&!busy
        )
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(
                onClick={imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                enabled=enabled&&!busy,
                modifier=Modifier.weight(1f)
            ){Text("▣ "+rsChatMediaT(lang,"image"),fontSize=10.sp)}
            OutlinedButton(
                onClick={videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))},
                enabled=enabled&&!busy,
                modifier=Modifier.weight(1f)
            ){Text("▶ "+rsChatMediaT(lang,"video"),fontSize=10.sp)}
        }
        if(picked!=null){
            Text(
                "✓ "+rsChatMediaT(lang,"attachment_ready")+" · "+pickedKind,
                color=c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(pickedKind=="VIDEO")Text(rsChatMediaT(lang,"max_video"),color=c.muted,fontSize=9.sp)
            TextButton(onClick={picked=null;pickedKind=""},enabled=!busy){
                Text(rsChatMediaT(lang,"remove"))
            }
        }
        Button(
            onClick={
                if(draft.trim().isBlank()&&picked==null){
                    onStatus(rsChatMediaT(lang,"message_or_media"))
                    return@Button
                }
                busy=true
                val body=draft.trim()
                val source=picked
                scope.launch{
                    var uploaded:RsChatAttachmentV92?=null
                    if(source!=null){
                        onStatus(rsChatMediaT(lang,"uploading"))
                        val result=rsUploadChatMediaV92(context,source,scopeType,scopeId)
                        if(result.isFailure){
                            onStatus(result.exceptionOrNull()?.message?:rsChatMediaT(lang,"download_error"))
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
                            onStatus("")
                            onSent()
                        }
                        .onFailure{onStatus(it.message?:"Could not send message.")}
                    busy=false
                }
            },
            enabled=enabled&&!busy&&(draft.trim().isNotBlank()||picked!=null),
            modifier=Modifier.fillMaxWidth()
        ){Text(if(busy)rsChatMediaT(lang,"sending") else rsChatMediaT(lang,"send"))}
    }
}

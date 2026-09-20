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

private fun rsChatSourceSizeV115(context:Context,uri:Uri):Long{
    return runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.SIZE),null,null,null)?.use{cur->
            if(cur.moveToFirst())cur.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)
}

private fun rsChatImageBytesV92(context:Context,uri:Uri):ByteArray{
    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
    context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
        ?:error("Could not read image.")
    require(bounds.outWidth>0&&bounds.outHeight>0){"Could not decode image."}

    var sample=1
    while(bounds.outWidth/sample>1800 || bounds.outHeight/sample>1800)sample*=2
    val opts=BitmapFactory.Options().apply{
        inSampleSize=sample
        inPreferredConfig=Bitmap.Config.RGB_565
    }
    val original=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}
        ?:error("Could not decode image.")
    try{
        val maxSide=max(original.width,original.height).coerceAtLeast(1)
        val scale=(1440f/maxSide).coerceAtMost(1f)
        val width=(original.width*scale).toInt().coerceAtLeast(1)
        val height=(original.height*scale).toInt().coerceAtLeast(1)
        val bitmap=if(width!=original.width||height!=original.height)
            Bitmap.createScaledBitmap(original,width,height,true)
        else original
        try{
            return ByteArrayOutputStream().use{out->
                bitmap.compress(Bitmap.CompressFormat.JPEG,82,out)
                out.toByteArray()
            }
        }finally{
            if(bitmap!==original)bitmap.recycle()
        }
    }finally{
        original.recycle()
    }
}

private fun rsChatVideoBytesV115(context:Context,uri:Uri):ByteArray{
    val size=rsChatSourceSizeV115(context,uri)
    require(size in 1..(30L*1024L*1024L)){"Chat video must be 30 MB or smaller."}
    require(size<=Int.MAX_VALUE){"Video is too large."}
    val result=ByteArray(size.toInt())
    context.contentResolver.openInputStream(uri)?.use{input->
        var offset=0
        while(offset<result.size){
            val read=input.read(result,offset,result.size-offset)
            if(read<0)break
            offset+=read
        }
        require(offset==result.size){"Could not read the complete video."}
    } ?: error("Could not read video.")
    return result
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

    val bytes=try{
        if(kind=="IMAGE")rsChatImageBytesV92(context,uri)
        else rsChatVideoBytesV115(context,uri)
    }catch(t:OutOfMemoryError){
        throw IllegalStateException("This media file is too large for the device. Choose a smaller image or video.")
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
        "download_error" to "Could not load attachment.","upload_error" to "Could not upload attachment.","message_or_media" to "Write a message or add an image/video.",
        "max_video" to "Short videos: maximum 30 seconds / 30 MB."
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

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            picked=uri
            pickedKind="IMAGE"
            onStatus("")
        }
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            picked=uri
            pickedKind="VIDEO"
            onStatus("")
        }
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
                onClick={imagePicker.launch(arrayOf("image/*"))},
                enabled=enabled&&!busy,
                modifier=Modifier.weight(1f)
            ){Text("▣ "+rsChatMediaT(lang,"image"),fontSize=10.sp)}
            OutlinedButton(
                onClick={videoPicker.launch(arrayOf("video/*"))},
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
                            val detail=result.exceptionOrNull()?.message.orEmpty().take(140)
                            onStatus(rsChatMediaT(lang,"upload_error")+(if(detail.isBlank())"" else " · "+detail))
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
                        .onFailure{
                            if(uploaded!=null)rsDeleteChatMediaV108(uploaded.path)
                            onStatus(rsReleaseT98(lang,"save_failed"))
                        }
                    busy=false
                }
            },
            enabled=enabled&&!busy&&(draft.trim().isNotBlank()||picked!=null),
            modifier=Modifier.fillMaxWidth()
        ){Text(if(busy)rsChatMediaT(lang,"sending") else rsChatMediaT(lang,"send"))}
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

package com.rskickbox.app

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

private const val RS_CHAT_GALLERY_BUCKET_V163="rs-chat-gallery"

@Serializable
data class RsChatGalleryItemV163(
    val id:String,
    @SerialName("gallery_path") val galleryPath:String,
    @SerialName("media_kind") val mediaKind:String,
    @SerialName("media_name") val mediaName:String,
    @SerialName("source_path") val sourcePath:String,
    @SerialName("saved_at") val savedAt:String
)

@Serializable
private data class RsExpiredChatMediaV163(
    @SerialName("media_path") val mediaPath:String
)

private fun rsGalleryContentTypeV163(kind:String,path:String):ContentType{
    val lower=path.lowercase()
    return when(kind){
        "IMAGE"->ContentType.Image.JPEG
        "VIDEO"->when{
            lower.endsWith(".webm")->ContentType.parse("video/webm")
            lower.endsWith(".mov")->ContentType.parse("video/quicktime")
            lower.endsWith(".3gp")->ContentType.parse("video/3gpp")
            else->ContentType.Video.MP4
        }
        "AUDIO"->when{
            lower.endsWith(".mp3")->ContentType.parse("audio/mpeg")
            lower.endsWith(".ogg")->ContentType.parse("audio/ogg")
            lower.endsWith(".wav")->ContentType.parse("audio/wav")
            else->ContentType.parse("audio/mp4")
        }
        else->ContentType.Application.OctetStream
    }
}

suspend fun rsSaveChatMediaToGalleryV163(
    sourcePath:String,
    mediaKind:String,
    mediaName:String?
):Result<Unit> = runCatching{
    require(sourcePath.isNotBlank()){"No media to save."}
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val uid=client.auth.currentUserOrNull()?.id ?: error("Sign in required.")
    val bytes=client.storage.from("rs-chat-media").downloadAuthenticated(sourcePath)
    require(bytes.isNotEmpty()){"Could not copy media."}

    val ext=sourcePath.substringAfterLast('.',when(mediaKind){"IMAGE"->"jpg";"VIDEO"->"mp4";"AUDIO"->"m4a";else->"bin"})
        .take(8)
        .replace(Regex("[^A-Za-z0-9]"),"")
        .ifBlank{"bin"}
    val galleryPath=uid+"/"+UUID.randomUUID().toString()+"."+ext

    client.storage.from(RS_CHAT_GALLERY_BUCKET_V163).upload(galleryPath,bytes){
        upsert=false
        contentType=rsGalleryContentTypeV163(mediaKind,galleryPath)
    }

    val registered=runCatching{
        client.postgrest.rpc(
            "rs_register_chat_gallery_item",
            buildJsonObject{
                put("p_gallery_path",galleryPath)
                put("p_media_kind",mediaKind)
                put("p_media_name",mediaName.orEmpty())
                put("p_source_path",sourcePath)
            }
        )
    }
    if(registered.isFailure){
        runCatching{client.storage.from(RS_CHAT_GALLERY_BUCKET_V163).delete(galleryPath)}
        throw registered.exceptionOrNull() ?: IllegalStateException("Could not register saved media.")
    }
    Unit
}

suspend fun rsChatGalleryFeedV163():Result<List<RsChatGalleryItemV163>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_chat_gallery_feed").decodeList<RsChatGalleryItemV163>()
}

suspend fun rsDeleteChatGalleryItemV163(item:RsChatGalleryItemV163):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.storage.from(RS_CHAT_GALLERY_BUCKET_V163).delete(item.galleryPath)
    client.postgrest.rpc(
        "rs_delete_chat_gallery_item",
        buildJsonObject{put("p_item_id",item.id)}
    )
    Unit
}

suspend fun rsChatGalleryLocalUriV163(context:Context,path:String):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val ext=path.substringAfterLast('.',"bin")
    val dir=File(context.cacheDir,"rs_chat_gallery").apply{mkdirs()}
    val file=File(dir,path.hashCode().toString()+"."+ext)
    if(!file.exists()||file.length()==0L){
        file.writeBytes(client.storage.from(RS_CHAT_GALLERY_BUCKET_V163).downloadAuthenticated(path))
    }
    Uri.fromFile(file).toString()
}

suspend fun rsCleanupExpiredChatMediaV163():Result<Int> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val rows=client.postgrest.rpc("rs_expired_chat_media_paths").decodeList<RsExpiredChatMediaV163>()
    var cleaned=0
    rows.distinctBy{it.mediaPath}.forEach{row->
        if(row.mediaPath.isBlank())return@forEach
        val deleted=runCatching{
            client.storage.from("rs-chat-media").delete(row.mediaPath)
        }.isSuccess
        if(deleted){
            runCatching{
                client.postgrest.rpc(
                    "rs_clear_expired_chat_media_path",
                    buildJsonObject{put("p_media_path",row.mediaPath)}
                )
            }.onSuccess{cleaned++}
        }
    }
    cleaned
}

@Composable
fun RsChatGalleryV163(c:RsPalette,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsChatGalleryItemV163>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    var busyId by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsChatGalleryFeedV163()
            .onSuccess{items=it}
            .onFailure{status=it.message?:"Could not load RS Chat Gallery."}
        loading=false
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Surface(
            color=Color.Black.copy(alpha=.76f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.28f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text("RS CHAT GALLERY",color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp)
                Text("Saved chat photos · videos · audio",color=c.gold,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Text(
                    "Temporary chat media expires after 7 days. Items saved here stay until you delete them.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
            }
        }

        if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(items.isEmpty()&&!loading){
                Surface(
                    color=c.panel.copy(alpha=.56f),
                    shape=RoundedCornerShape(20.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text("No saved chat media yet.",color=c.muted,modifier=Modifier.padding(14.dp))
                }
            }
            items.forEach{item->
                var local by remember(item.id){mutableStateOf("")}
                var loadError by remember(item.id){mutableStateOf("")}
                LaunchedEffect(item.id){
                    rsChatGalleryLocalUriV163(context,item.galleryPath)
                        .onSuccess{local=it}
                        .onFailure{loadError=it.message.orEmpty()}
                }

                Surface(
                    color=c.panel.copy(alpha=.66f),
                    shape=RoundedCornerShape(22.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                            Column(Modifier.weight(1f)){
                                Text(item.mediaName.ifBlank{item.mediaKind},color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp,maxLines=1)
                                Text(item.mediaKind,color=c.muted,fontSize=8.sp)
                            }
                            TextButton(
                                onClick={
                                    busyId=item.id
                                    scope.launch{
                                        rsDeleteChatGalleryItemV163(item)
                                            .onSuccess{revision++}
                                            .onFailure{status=it.message?:"Could not delete saved media."}
                                        busyId=null
                                    }
                                },
                                enabled=busyId==null
                            ){Text("Delete",fontSize=8.sp)}
                        }
                        if(loadError.isNotBlank())Text(loadError,color=c.muted,fontSize=8.sp)
                        else if(local.isBlank())LinearProgressIndicator(Modifier.fillMaxWidth())
                        else when(item.mediaKind){
                            "IMAGE"->RsUriPreviewV21(local,Modifier.fillMaxWidth().height(150.dp),"CENTER")
                            "VIDEO","AUDIO"->{
                                val player=remember(local){
                                    ExoPlayer.Builder(context).build().apply{
                                        setMediaItem(MediaItem.fromUri(Uri.parse(local)))
                                        prepare()
                                    }
                                }
                                DisposableEffect(player){onDispose{player.release()}}
                                AndroidView(
                                    factory={ctx->PlayerView(ctx).apply{this.player=player;useController=true}},
                                    update={it.player=player},
                                    modifier=if(item.mediaKind=="VIDEO")Modifier.fillMaxWidth().height(150.dp)
                                    else Modifier.fillMaxWidth().height(76.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

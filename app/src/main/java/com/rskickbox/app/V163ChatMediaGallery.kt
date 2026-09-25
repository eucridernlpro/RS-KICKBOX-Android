package com.rskickbox.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

data class RsSavedChatMediaV163(
    val id:String,
    val uri:String,
    val kind:String,
    val name:String,
    val savedAt:Long
)

data class RsRecentChatMediaV163(
    val uri:String,
    val kind:String,
    val name:String,
    val modifiedAt:Long
)

data class RsSavedChatMessageV196(
    val id:String,
    val sender:String,
    val body:String,
    val createdAt:Long,
    val savedAt:Long
)

private const val RS_CHAT_GALLERY_KEY_V163="rs_chat_gallery_v163"
private const val RS_CHAT_MESSAGE_GALLERY_KEY_V196="rs_chat_message_gallery_v196"

fun rsSaveChatMessageToGalleryV196(
    store:RsStore,
    message:RsCloudCoachMessageV72,
    sender:String
):Result<Unit> = runCatching{
    val raw=store.s(RS_CHAT_MESSAGE_GALLERY_KEY_V196,"")
    val a=if(raw.isBlank())JSONArray() else runCatching{JSONArray(raw)}.getOrDefault(JSONArray())
    val next=JSONArray()
    next.put(JSONObject().apply{
        put("id",message.id)
        put("sender",sender)
        put("body",message.body)
        put("createdAt",message.createdAtMillis())
        put("savedAt",System.currentTimeMillis())
    })
    for(i in 0 until minOf(a.length(),199))next.put(a.getJSONObject(i))
    store.ps(RS_CHAT_MESSAGE_GALLERY_KEY_V196,next.toString())
}

private fun rsLoadSavedChatMessagesV196(store:RsStore):List<RsSavedChatMessageV196>{
    val raw=store.s(RS_CHAT_MESSAGE_GALLERY_KEY_V196,"")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsSavedChatMessageV196(
                    id=o.optString("id"),
                    sender=o.optString("sender"),
                    body=o.optString("body"),
                    createdAt=o.optLong("createdAt"),
                    savedAt=o.optLong("savedAt")
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadChatGalleryV163(store:RsStore):List<RsSavedChatMediaV163>{
    val raw=store.s(RS_CHAT_GALLERY_KEY_V163,"")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(
                    RsSavedChatMediaV163(
                        id=o.optString("id"),
                        uri=o.optString("uri"),
                        kind=o.optString("kind"),
                        name=o.optString("name"),
                        savedAt=o.optLong("savedAt")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsSaveChatGalleryV163(store:RsStore,items:List<RsSavedChatMediaV163>){
    val a=JSONArray()
    items.forEach{x->
        a.put(JSONObject().apply{
            put("id",x.id)
            put("uri",x.uri)
            put("kind",x.kind)
            put("name",x.name)
            put("savedAt",x.savedAt)
        })
    }
    store.ps(RS_CHAT_GALLERY_KEY_V163,a.toString())
}

fun rsCacheTemporaryAiMediaV163(
    context:Context,
    source:Uri,
    kind:String
):Result<String> = runCatching{
    val dir=File(context.cacheDir,"rs_ai_chat_media").apply{mkdirs()}
    val mime=context.contentResolver.getType(source).orEmpty().lowercase()
    val ext=when{
        kind=="IMAGE" && mime.contains("png")->"png"
        kind=="IMAGE"->"jpg"
        mime.contains("webm")->"webm"
        mime.contains("quicktime")->"mov"
        else->"mp4"
    }
    val file=File(dir,"ai_"+UUID.randomUUID()+"."+ext)
    context.contentResolver.openInputStream(source)?.use{input->
        file.outputStream().use{output->input.copyTo(output)}
    } ?: error("Could not read selected media.")
    require(file.length()>0){"Selected media is empty."}
    file.setLastModified(System.currentTimeMillis())
    Uri.fromFile(file).toString()
}

private fun rsRecentChatMediaV163(context:Context):List<RsRecentChatMediaV163>{
    val dirs=listOf("rs_chat_media","rs_voice_messages","rs_ai_chat_media")
    return dirs.flatMap{dirName->
        File(context.cacheDir,dirName).listFiles()?.mapNotNull{file->
            if(!file.isFile||file.length()<=0L)return@mapNotNull null
            val ext=file.extension.lowercase()
            val kind=when(ext){
                "jpg","jpeg","png","webp","gif"->"IMAGE"
                "mp4","webm","mov","3gp"->"VIDEO"
                "m4a","aac","mp3","ogg","wav"->"AUDIO"
                else->return@mapNotNull null
            }
            RsRecentChatMediaV163(
                uri=Uri.fromFile(file).toString(),
                kind=kind,
                name=file.name,
                modifiedAt=file.lastModified()
            )
        }?:emptyList()
    }.sortedByDescending{it.modifiedAt}
}

fun rsCleanupChatMediaCacheV163(context:Context){
    val store=RsStore(context)
    val retentionDays=store.s(RsChatPermissionKeysV178.RETENTION_DAYS,"7").toIntOrNull()?.coerceIn(1,30)?:7
    val cutoff=System.currentTimeMillis()-retentionDays.toLong()*24L*60L*60L*1000L
    listOf("rs_chat_media","rs_voice_messages","rs_ai_chat_media").forEach{dirName->
        val dir=File(context.cacheDir,dirName)
        dir.listFiles()?.forEach{file->
            if(file.isFile && file.lastModified()<cutoff)runCatching{file.delete()}
        }
    }
}

fun rsSaveChatMediaToGalleryV163(
    context:Context,
    store:RsStore,
    sourceUri:String,
    kind:String,
    name:String?
):Result<Unit> = runCatching{
    val uri=Uri.parse(sourceUri)
    val ext=when{
        uri.scheme=="file"->File(uri.path.orEmpty()).extension.ifBlank{
            when(kind){"IMAGE"->"jpg";"VIDEO"->"mp4";"AUDIO"->"m4a";else->"bin"}
        }
        else->when(kind){"IMAGE"->"jpg";"VIDEO"->"mp4";"AUDIO"->"m4a";else->"bin"}
    }
    val dir=File(context.filesDir,"rs_chat_gallery").apply{mkdirs()}
    val target=File(dir,"saved_"+UUID.randomUUID()+"."+ext)
    if(uri.scheme=="file"){
        val source=File(uri.path?:error("Missing media file."))
        require(source.exists()&&source.length()>0){"Media file is unavailable."}
        source.copyTo(target,overwrite=false)
    }else{
        context.contentResolver.openInputStream(uri)?.use{input->
            target.outputStream().use{output->input.copyTo(output)}
        } ?: error("Could not read selected media.")
        require(target.length()>0){"Media file is unavailable."}
    }
    val items=rsLoadChatGalleryV163(store).toMutableList()
    items.add(
        0,
        RsSavedChatMediaV163(
            id=UUID.randomUUID().toString(),
            uri=Uri.fromFile(target).toString(),
            kind=kind,
            name=name.orEmpty().ifBlank{"Saved "+kind.lowercase()},
            savedAt=System.currentTimeMillis()
        )
    )
    rsSaveChatGalleryV163(store,items.take(200))
}

@Composable
fun RsChatMediaGalleryV163(c:RsPalette,store:RsStore,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    val galleryAllowed=rsChatPermissionV178(store,RsChatPermissionKeysV178.GALLERY,true)
    val retentionDays=store.s(RsChatPermissionKeysV178.RETENTION_DAYS,"7").toIntOrNull()?.coerceIn(1,30)?:7
    var revision by remember{mutableIntStateOf(0)}
    var status by remember{mutableStateOf("")}
    val items=remember(revision){rsLoadChatGalleryV163(store)}
    val savedMessages=remember(revision){rsLoadSavedChatMessagesV196(store)}
    val recent=remember(revision){
        rsCleanupChatMediaCacheV163(context)
        rsRecentChatMediaV163(context)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=8.dp,vertical=6.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        Text("RS CHAT GALLERY",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
        Text(
            "Saved photos, videos and audio stay here. Unsaved chat cache is automatically cleaned after "+retentionDays+" day"+if(retentionDays==1)"" else "s"+".",
            color=c.muted,fontSize=9.sp,lineHeight=13.sp
        )
        if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)

        Text("SAVED MESSAGES",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=.7.sp)
        if(savedMessages.isEmpty()){
            Text("No saved text messages yet. Ask RS AI to save a message after it reads it to you.",color=c.muted,fontSize=9.sp)
        }else{
            savedMessages.take(50).forEach{message->
                Surface(
                    color=Color.Black.copy(alpha=.66f),
                    shape=RoundedCornerShape(18.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text(message.sender,color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                        Text(
                            java.text.SimpleDateFormat("dd MMM · HH:mm",java.util.Locale.getDefault())
                                .format(java.util.Date(message.createdAt)),
                            color=c.muted,fontSize=8.sp
                        )
                        Text(message.body,color=c.text,fontSize=11.sp,lineHeight=15.sp)
                    }
                }
            }
        }

        Text("RECENT · AUTO-DELETE AFTER "+retentionDays+" DAY"+if(retentionDays==1)"" else "S",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=.7.sp)
        if(recent.isEmpty()){
            Text("No temporary chat media on this device.",color=c.muted,fontSize=9.sp)
        }else{
            recent.take(50).forEach{item->
                Surface(
                    color=Color.Black.copy(alpha=.62f),
                    shape=RoundedCornerShape(20.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text(item.name,color=c.text,fontSize=9.sp,maxLines=1)
                        RsRecentChatPreviewV163(c,item)
                        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                            val ageDays=((System.currentTimeMillis()-item.modifiedAt)/(24L*60L*60L*1000L)).coerceAtLeast(0L)
                            Text(
                                "Expires in "+(retentionDays.toLong()-ageDays).coerceAtLeast(0L)+" day(s)",
                                color=c.muted,fontSize=8.sp,modifier=Modifier.weight(1f)
                            )
                            if(galleryAllowed){
                                TextButton(onClick={
                                    rsSaveChatMediaToGalleryV163(context,store,item.uri,item.kind,item.name)
                                        .onSuccess{status="Saved to RS Chat Gallery.";revision++}
                                        .onFailure{status=it.message?:"Could not save media."}
                                }){Text("☆ Save",fontSize=9.sp)}
                            }
                        }
                    }
                }
            }
        }

        Text("SAVED GALLERY",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=.7.sp)

        if(items.isEmpty()){
            Surface(
                color=Color.Black.copy(alpha=.62f),
                shape=RoundedCornerShape(22.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Text(
                    "Nothing saved yet. Open media in a chat and choose Save to Gallery.",
                    color=c.muted,
                    fontSize=10.sp,
                    modifier=Modifier.padding(16.dp)
                )
            }
        }

        items.forEach{item->
            Surface(
                color=Color.Black.copy(alpha=.68f),
                shape=RoundedCornerShape(22.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Text(item.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    RsChatGalleryPreviewV163(c,item)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Text(
                            item.kind,
                            color=c.gold,
                            fontWeight=FontWeight.Black,
                            fontSize=8.sp,
                            modifier=Modifier.weight(1f)
                        )
                        TextButton(onClick={
                            val updated=rsLoadChatGalleryV163(store).filterNot{it.id==item.id}
                            runCatching{File(Uri.parse(item.uri).path.orEmpty()).delete()}
                            rsSaveChatGalleryV163(store,updated)
                            status="Saved media deleted."
                            revision++
                        }){Text("Delete",fontSize=9.sp)}
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RsChatGalleryPreviewV163(c:RsPalette,item:RsSavedChatMediaV163){
    when(item.kind){
        "IMAGE"->RsUriPreviewV21(item.uri,Modifier.fillMaxWidth().height(180.dp),"CENTER")
        "VIDEO"->RsMiniLocalVideoV163(item.uri,Modifier.fillMaxWidth().height(150.dp))
        "AUDIO"->Surface(
            color=c.gold.copy(alpha=.07f),
            shape=RoundedCornerShape(14.dp),
            modifier=Modifier.fillMaxWidth()
        ){
            Text("🎙  Saved voice / audio",color=c.text,modifier=Modifier.padding(14.dp))
        }
        else->Text(item.uri,color=c.muted,fontSize=8.sp)
    }
}


@Composable
fun RsMiniLocalVideoV163(uri:String,modifier:Modifier=Modifier){
    val context=LocalContext.current
    val player=remember(uri){
        ExoPlayer.Builder(context).build().apply{
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            playWhenReady=false
            prepare()
        }
    }
    DisposableEffect(player){onDispose{runCatching{player.release()}}}
    AndroidView(
        factory={ctx->
            PlayerView(ctx).apply{
                this.player=player
                useController=true
            }
        },
        update={it.player=player},
        modifier=modifier
    )
}


@Composable
private fun RsRecentChatPreviewV163(c:RsPalette,item:RsRecentChatMediaV163){
    when(item.kind){
        "IMAGE"->RsUriPreviewV21(item.uri,Modifier.fillMaxWidth().height(150.dp),"CENTER")
        "VIDEO"->RsMiniLocalVideoV163(item.uri,Modifier.fillMaxWidth().height(120.dp))
        "AUDIO"->Surface(
            color=c.gold.copy(alpha=.07f),
            shape=RoundedCornerShape(14.dp),
            modifier=Modifier.fillMaxWidth()
        ){
            Text("🎙  Temporary audio",color=c.text,modifier=Modifier.padding(12.dp))
        }
    }
}

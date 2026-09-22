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

private const val RS_CHAT_GALLERY_KEY_V163="rs_chat_gallery_v163"

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

fun rsCleanupChatMediaCacheV163(context:Context){
    val cutoff=System.currentTimeMillis()-7L*24L*60L*60L*1000L
    listOf("rs_chat_media","rs_voice_messages").forEach{dirName->
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
    val source=when(uri.scheme){
        "file"->File(uri.path?:error("Missing media file."))
        else->error("Media must be downloaded before it can be saved.")
    }
    require(source.exists()&&source.length()>0){"Media file is unavailable."}
    val ext=source.extension.ifBlank{
        when(kind){"IMAGE"->"jpg";"VIDEO"->"mp4";"AUDIO"->"m4a";else->"bin"}
    }
    val dir=File(context.filesDir,"rs_chat_gallery").apply{mkdirs()}
    val target=File(dir,"saved_"+UUID.randomUUID()+"."+ext)
    source.copyTo(target,overwrite=false)
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
    var revision by remember{mutableIntStateOf(0)}
    var status by remember{mutableStateOf("")}
    val items=remember(revision){rsLoadChatGalleryV163(store)}

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=8.dp,vertical=6.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        Text("RS CHAT GALLERY",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
        Text(
            "Saved photos, videos and audio stay here. Unsaved chat cache is automatically cleaned after 7 days.",
            color=c.muted,fontSize=9.sp,lineHeight=13.sp
        )
        if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)

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

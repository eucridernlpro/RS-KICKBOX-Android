package com.rskickbox.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken

class RsMusicPlaybackServiceV90 : MediaSessionService() {
    private var mediaSession:MediaSession?=null

    override fun onCreate(){
        super.onCreate()
        val player=ExoPlayer.Builder(this).build().apply{
            repeatMode=Player.REPEAT_MODE_ALL
        }
        mediaSession=MediaSession.Builder(this,player).build()
    }

    override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaSession?=mediaSession

    override fun onDestroy(){
        mediaSession?.run{
            player.release()
            release()
        }
        mediaSession=null
        super.onDestroy()
    }
}

data class RsPersistentTrackV90(val uri:String,val name:String)

@Composable
fun rememberRsMusicControllerV90():MediaController?{
    val context=LocalContext.current
    var controller by remember{mutableStateOf<MediaController?>(null)}

    DisposableEffect(Unit){
        val token=SessionToken(context,ComponentName(context,RsMusicPlaybackServiceV90::class.java))
        val future=MediaController.Builder(context,token).buildAsync()
        future.addListener({
            controller=runCatching{future.get()}.getOrNull()
        },ContextCompat.getMainExecutor(context))

        onDispose{
            controller?.release()
            controller=null
            if(!future.isDone)future.cancel(true)
        }
    }
    return controller
}

private fun rsPersistentTracksV90(store:RsStore):List<RsPersistentTrackV90> =
    store.s("local_music_tracks","")
        .split("§")
        .filter{it.contains("¤")}
        .mapNotNull{row->
            val p=row.split("¤",limit=2)
            if(p.size==2)RsPersistentTrackV90(p[0],p[1]) else null
        }

private fun rsSavePersistentTracksV90(store:RsStore,tracks:List<RsPersistentTrackV90>)=
    store.ps("local_music_tracks",tracks.joinToString("§"){"${it.uri}¤${it.name}"})

private fun rsTrackDisplayNameV90(context:Context,uri:Uri,fallback:String):String{
    var name=fallback
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())name=cur.getString(0)?:name
        }
    }
    return name
}

private fun rsMediaItemsV90(tracks:List<RsPersistentTrackV90>)=tracks.map{track->
    MediaItem.Builder()
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.name)
                .setArtist("RS KICKBOX")
                .build()
        )
        .build()
}

@Composable
fun RsPersistentMusicCenterV90(c:RsPalette,store:RsStore,role:RsRole,lang:RsLang){
    val context=LocalContext.current
    val controller=rememberRsMusicControllerV90()
    var tracks by remember{mutableStateOf(rsPersistentTracksV90(store))}
    var feedback by remember{mutableStateOf("")}
    var revision by remember{mutableIntStateOf(0)}

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()){
            val next=tracks.toMutableList()
            uris.forEachIndexed{i,uri->
                runCatching{
                    context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val track=RsPersistentTrackV90(
                    uri.toString(),
                    rsTrackDisplayNameV90(context,uri,"Training track "+(tracks.size+i+1))
                )
                if(next.none{it.uri==track.uri})next.add(track)
            }
            tracks=next
            rsSavePersistentTracksV90(store,next)
            feedback=uris.size.toString()+" audio file(s) added."
            revision++
        }
    }

    var playing by remember{mutableStateOf(controller?.isPlaying==true)}
    var index by remember{mutableIntStateOf(controller?.currentMediaItemIndex?.coerceAtLeast(0)?:0)}
    var position by remember{mutableLongStateOf(controller?.currentPosition?:0L)}
    var duration by remember{mutableLongStateOf(controller?.duration?.coerceAtLeast(0L)?:0L)}

    DisposableEffect(controller,revision){
        if(controller==null)return@DisposableEffect onDispose{}
        val listener=object:Player.Listener{
            override fun onIsPlayingChanged(isPlaying:Boolean){playing=isPlaying}
            override fun onMediaItemTransition(mediaItem:MediaItem?,reason:Int){
                index=controller.currentMediaItemIndex.coerceAtLeast(0)
            }
            override fun onPlaybackStateChanged(playbackState:Int){
                duration=controller.duration.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose{controller.removeListener(listener)}
    }

    LaunchedEffect(controller,playing,index){
        while(controller!=null){
            position=controller.currentPosition.coerceAtLeast(0L)
            duration=controller.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(500)
        }
    }

    fun playTrack(i:Int){
        val p=controller?:return
        if(tracks.isEmpty())return
        p.setMediaItems(rsMediaItemsV90(tracks),i.coerceIn(0,tracks.lastIndex),0L)
        p.prepare()
        p.play()
    }

    RsScroll(
        c,
        if(role==RsRole.TRAINER)"RS Music Manager" else "My RS Music",
        "Music continues across the app and outside it until you stop playback."
    ){
        RsPanel(c){
            Text("BACKGROUND PLAYER",color=c.bright,fontWeight=FontWeight.Black)
            Text(
                "Playback stays active while you open other RS KICKBOX pages or minimize the app. Android media controls appear in the notification area.",
                color=c.muted,
                fontSize=10.sp
            )
            Button(onClick={picker.launch(arrayOf("audio/*"))},modifier=Modifier.fillMaxWidth()){
                Text("Add music from device")
            }
            if(feedback.isNotBlank())Text(feedback,color=c.muted,fontSize=10.sp)
        }

        if(tracks.isEmpty()){
            RsPanel(c){
                Text("No music added yet.",color=c.bright,fontWeight=FontWeight.Bold)
                Text("Add audio from the device to create your RS training playlist.",color=c.muted)
            }
        }else{
            val safeIndex=index.coerceIn(0,tracks.lastIndex)
            val current=tracks[safeIndex]
            RsPanel(c){
                Text("NOW PLAYING",color=c.muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Text(current.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                Slider(
                    value=if(duration>0)position.toFloat()/duration else 0f,
                    onValueChange={fraction->
                        controller?.seekTo((duration*fraction).toLong())
                    },
                    modifier=Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                    FilledTonalButton(onClick={controller?.seekToPreviousMediaItem()}){Text("⏮")}
                    Button(onClick={
                        val p=controller?:return@Button
                        if(p.mediaItemCount==0)playTrack(safeIndex)
                        else if(p.isPlaying)p.pause() else p.play()
                    }){Text(if(playing)"⏸ Pause" else "▶ Play")}
                    FilledTonalButton(onClick={controller?.seekToNextMediaItem()}){Text("⏭")}
                }
                OutlinedButton(
                    onClick={
                        controller?.stop()
                        controller?.clearMediaItems()
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text("Stop & close player")}
            }

            Text("PLAYLIST",color=c.bright,fontWeight=FontWeight.Black)
            tracks.forEachIndexed{i,track->
                RsPanel(c){
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        TextButton(onClick={playTrack(i)},modifier=Modifier.weight(1f)){
                            Text(
                                if(i==safeIndex && controller?.mediaItemCount?:0>0)"▶ "+track.name else track.name,
                                color=if(i==safeIndex)c.bright else c.text,
                                maxLines=1,
                                overflow=TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick={
                            val next=tracks.toMutableList().also{it.removeAt(i)}
                            if(controller?.mediaItemCount?:0>0){
                                controller?.removeMediaItem(i)
                            }
                            tracks=next
                            rsSavePersistentTracksV90(store,next)
                            revision++
                        }){Text("Remove")}
                    }
                }
            }
        }
    }
}

@Composable
fun RsMiniMusicPlayerV90(
    c:RsPalette,
    onOpenMusic:()->Unit
){
    val controller=rememberRsMusicControllerV90()
    var title by remember{mutableStateOf("")}
    var playing by remember{mutableStateOf(false)}
    var hasMedia by remember{mutableStateOf(false)}

    fun refresh(){
        val p=controller
        if(p==null){
            title=""
            playing=false
            hasMedia=false
        }else{
            hasMedia=p.mediaItemCount>0
            title=p.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            playing=p.isPlaying
        }
    }

    DisposableEffect(controller){
        if(controller==null)return@DisposableEffect onDispose{}
        refresh()
        val listener=object:Player.Listener{
            override fun onEvents(player:Player,events:Player.Events){refresh()}
        }
        controller.addListener(listener)
        onDispose{controller.removeListener(listener)}
    }

    if(!hasMedia)return

    Surface(
        shape=RoundedCornerShape(16.dp),
        color=c.panel2,
        tonalElevation=3.dp,
        modifier=Modifier.fillMaxWidth()
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)
        ){
            Text("♫",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
            Text(
                title.ifBlank{"RS Music"},
                color=c.text,
                maxLines=1,
                overflow=TextOverflow.Ellipsis,
                modifier=Modifier.weight(1f)
            )
            TextButton(onClick={
                val p=controller?:return@TextButton
                if(p.isPlaying)p.pause() else p.play()
            }){Text(if(playing)"⏸" else "▶")}
            TextButton(onClick=onOpenMusic){Text("OPEN",fontSize=9.sp)}
            TextButton(onClick={
                controller?.stop()
                controller?.clearMediaItems()
            }){Text("✕")}
        }
    }
}

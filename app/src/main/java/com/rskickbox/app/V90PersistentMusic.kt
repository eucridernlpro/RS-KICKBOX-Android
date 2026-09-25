package com.rskickbox.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class RsMusicPlaybackServiceV90 : MediaSessionService() {
    private var mediaSession:MediaSession?=null

    override fun onCreate(){
        super.onCreate()
        val player=ExoPlayer.Builder(this).build().apply{
            repeatMode=Player.REPEAT_MODE_ALL
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val historyStore=RsStore(this)
        player.addListener(object:Player.Listener{
            override fun onMediaItemTransition(mediaItem:MediaItem?,reason:Int){
                val uri=mediaItem?.localConfiguration?.uri?.toString().orEmpty()
                if(uri.isBlank())return
                val current=historyStore.s("rs_music_recent_history_v165","")
                    .split("§")
                    .filter{it.isNotBlank()&&it!=uri}
                    .toMutableList()
                current.add(0,uri)
                historyStore.ps("rs_music_recent_history_v165",current.take(30).joinToString("§"))
            }
        })
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
data class RsNamedPlaylistV108(val name:String,val uris:Set<String>)

private fun rsLoadNamedPlaylistsV108(store:RsStore):List<RsNamedPlaylistV108>{
    val raw=store.s("music_named_playlists_v108","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                val uris=o.optJSONArray("uris")?:JSONArray()
                add(RsNamedPlaylistV108(
                    o.optString("name").take(60),
                    buildSet{for(j in 0 until uris.length())add(uris.optString(j))}
                ))
            }
        }.filter{it.name.isNotBlank()}
    }.getOrDefault(emptyList())
}

private fun rsSaveNamedPlaylistsV108(store:RsStore,items:List<RsNamedPlaylistV108>){
    val arr=JSONArray()
    items.forEach{p->
        arr.put(JSONObject().apply{
            put("name",p.name)
            put("uris",JSONArray().apply{p.uris.forEach{put(it)}})
        })
    }
    store.ps("music_named_playlists_v108",arr.toString())
}

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
                .setArtist("RS KICKBOXING")
                .build()
        )
        .build()
}

fun rsPrepareMusicTrackV169(
    context:Context,
    uri:Uri,
    fallbackName:String="RS Music"
):Result<RsPersistentTrackV90> = runCatching{
    runCatching{
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    val name=rsTrackDisplayNameV90(context,uri,fallbackName)
    RsPersistentTrackV90(uri.toString(),name)
}

fun rsSaveMusicTrackV169(
    store:RsStore,
    track:RsPersistentTrackV90
):Result<Unit> = runCatching{
    val current=rsPersistentTracksV90(store).toMutableList()
    if(current.none{it.uri==track.uri})current.add(track)
    rsSavePersistentTracksV90(store,current)
}

fun rsPlayMusicTrackV169(
    controller:MediaController?,
    track:RsPersistentTrackV90
):Result<Unit> = runCatching{
    val p=controller?:error("RS Music player is not ready yet.")
    val item=MediaItem.Builder()
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.name)
                .setArtist("RS KICKBOXING")
                .build()
        )
        .build()
    p.setMediaItem(item)
    p.prepare()
    p.play()
}

fun rsMusicPlaylistNamesV169(store:RsStore):List<String> =
    rsLoadNamedPlaylistsV108(store).map{it.name}

fun rsAllMusicTracksV171(store:RsStore):List<RsPersistentTrackV90> =
    rsPersistentTracksV90(store)

fun rsFindMusicTrackV171(store:RsStore,query:String):RsPersistentTrackV90?{
    val q=query.lowercase(java.util.Locale.ROOT)
    return rsPersistentTracksV90(store)
        .sortedByDescending{it.name.length}
        .firstOrNull{q.contains(it.name.lowercase(java.util.Locale.ROOT))}
}

fun rsAddMusicTrackToPlaylistV169(
    store:RsStore,
    playlistName:String,
    track:RsPersistentTrackV90
):Result<Unit> = runCatching{
    val clean=playlistName.trim().take(60)
    require(clean.isNotBlank()){"Playlist name is required."}
    rsSaveMusicTrackV169(store,track).getOrThrow()

    val current=rsLoadNamedPlaylistsV108(store).toMutableList()
    val index=current.indexOfFirst{it.name.equals(clean,true)}
    if(index>=0){
        val old=current[index]
        current[index]=old.copy(uris=old.uris+track.uri)
    }else{
        current.add(RsNamedPlaylistV108(clean,setOf(track.uri)))
    }
    rsSaveNamedPlaylistsV108(store,current)
    store.ps("music_active_playlist_v108",clean)
}


private fun rsEmbeddedArtworkV175(context:Context,uri:String):androidx.compose.ui.graphics.ImageBitmap?{
    if(uri.isBlank())return null
    return runCatching{
        val retriever=MediaMetadataRetriever()
        try{
            retriever.setDataSource(context,Uri.parse(uri))
            val bytes=retriever.embeddedPicture?:return@runCatching null
            BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap()
        }finally{
            runCatching{retriever.release()}
        }
    }.getOrNull()
}

@Composable
private fun RsMusicVisualizerV175(c:RsPalette,playing:Boolean){
    val transition=rememberInfiniteTransition(label="rs-music-viz")
    val phase by transition.animateFloat(
        initialValue=0f,
        targetValue=6.28318f,
        animationSpec=infiniteRepeatable(
            animation=tween(durationMillis=1100,easing=LinearEasing),
            repeatMode=RepeatMode.Restart
        ),
        label="music-phase"
    )
    Canvas(Modifier.fillMaxWidth().height(56.dp)){
        val bars=24
        val gap=size.width/(bars*2f)
        val barW=(size.width-gap*(bars+1))/bars
        repeat(bars){i->
            val base=if(playing){
                .22f+.72f*((sin(phase+i*.72f)+1f)/2f)
            }else .16f+.08f*((i%4)/3f)
            val h=size.height*base
            val left=gap+i*(barW+gap)
            drawRoundRect(
                color=if(i%3==0)c.bright.copy(alpha=.92f) else c.gold.copy(alpha=.72f),
                topLeft=androidx.compose.ui.geometry.Offset(left,(size.height-h)/2f),
                size=androidx.compose.ui.geometry.Size(barW.coerceAtLeast(2f),h),
                cornerRadius=androidx.compose.ui.geometry.CornerRadius(barW/2f,barW/2f)
            )
        }
    }
}

@Composable
private fun RsMusicSpatialVisualizerV201(
    c:RsPalette,
    playing:Boolean,
    preset:String
){
    val transition=rememberInfiniteTransition(label="rs-spatial-viz")
    val rotation by transition.animateFloat(
        initialValue=0f,
        targetValue=360f,
        animationSpec=infiniteRepeatable(
            animation=tween(if(playing)4200 else 9000,easing=LinearEasing),
            repeatMode=RepeatMode.Restart
        ),
        label="rs-spatial-rotation"
    )
    Canvas(
        Modifier.fillMaxWidth().height(120.dp)
            .graphicsLayer{rotationZ=if(playing)rotation else 0f}
    ){
        val center=androidx.compose.ui.geometry.Offset(size.width/2f,size.height/2f)
        val base=kotlin.math.min(size.width,size.height)
        val accent=when(preset){
            "BASS"->c.gold
            "ARENA"->Color(0xFF8A7CFF)
            "NEON"->Color(0xFF58C9FF)
            else->c.bright
        }
        repeat(5){i->
            val pulse=if(playing)(1f+0.05f*kotlin.math.sin((rotation/57.3f)+i)) else 1f
            drawCircle(
                color=accent.copy(alpha=.28f-(i*.035f)),
                radius=base*(.12f+i*.075f)*pulse,
                center=center,
                style=androidx.compose.ui.graphics.drawscope.Stroke(width=(2f+i))
            )
        }
        repeat(12){i->
            val a=(Math.PI*2*i/12.0)+(rotation*Math.PI/180.0)
            val r=base*.34f
            val x=center.x+(kotlin.math.cos(a)*r).toFloat()
            val y=center.y+(kotlin.math.sin(a)*r).toFloat()
            drawCircle(accent.copy(alpha=.55f),radius=3f+(i%3)*1.5f,center=androidx.compose.ui.geometry.Offset(x,y))
        }
    }
}

@Composable
fun RsPersistentMusicCenterV90(c:RsPalette,store:RsStore,role:RsRole,lang:RsLang){
    val context=LocalContext.current
    val controller=rememberRsMusicControllerV90()
    var tracks by remember{mutableStateOf(rsPersistentTracksV90(store))}
    var playlists by remember{mutableStateOf(rsLoadNamedPlaylistsV108(store))}
    var activePlaylist by remember{mutableStateOf(store.s("music_active_playlist_v108","ALL"))}
    var newPlaylistName by remember{mutableStateOf("")}
    var volume by remember{mutableFloatStateOf(store.s("music_volume_v108","1.0").toFloatOrNull()?.coerceIn(0f,1f)?:1f)}
    var feedback by remember{mutableStateOf("")}
    var revision by remember{mutableIntStateOf(0)}
    var searchQuery by remember{mutableStateOf("")}
    var shuffleOn by remember{mutableStateOf(controller?.shuffleModeEnabled==true)}
    var repeatMode by remember{mutableIntStateOf(controller?.repeatMode?:Player.REPEAT_MODE_ALL)}
    var libraryOpen by remember{mutableStateOf(false)}
    var musicGuideOpen by remember{mutableStateOf(false)}
    var libraryTab by remember{mutableStateOf("LIBRARY")}
    var spatialPreset by remember{mutableStateOf(store.s("music_visual_effect_v201","NEON"))}
    var speed by remember{mutableFloatStateOf(store.s("music_speed_v201","1.0").toFloatOrNull()?.coerceIn(.75f,1.5f)?:1f)}
    var favorites by remember{
        mutableStateOf(
            store.s("music_favorites_v201","").split("§").filter{it.isNotBlank()}.toSet()
        )
    }
    LaunchedEffect(Unit){
        when(store.s("music_open_library_v201","")){
            "IMPORT"->{libraryOpen=true;libraryTab="LIBRARY"}
            "PLAYLISTS"->{libraryOpen=true;libraryTab="PLAYLISTS"}
            "FAVORITES"->{libraryOpen=true;libraryTab="FAVORITES"}
        }
        store.ps("music_open_library_v201","")
    }

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()){
            val next=tracks.toMutableList()
            val imported=mutableSetOf<String>()
            uris.forEachIndexed{i,uri->
                runCatching{
                    context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val track=RsPersistentTrackV90(
                    uri.toString(),
                    rsTrackDisplayNameV90(context,uri,rsMusicBgT90(lang,"track")+" "+(tracks.size+i+1))
                )
                if(next.none{it.uri==track.uri})next.add(track)
                imported.add(track.uri)
            }
            tracks=next
            rsSavePersistentTracksV90(store,next)
            if(activePlaylist!="ALL" && imported.isNotEmpty()){
                playlists=playlists.map{p->if(p.name==activePlaylist)p.copy(uris=p.uris+imported) else p}
                rsSaveNamedPlaylistsV108(store,playlists)
            }
            feedback=uris.size.toString()+" "+rsMusicBgT90(lang,"added")
            revision++
        }
    }

    var playing by remember{mutableStateOf(controller?.isPlaying==true)}
    var index by remember{mutableIntStateOf(controller?.currentMediaItemIndex?.coerceAtLeast(0)?:0)}
    var position by remember{mutableLongStateOf(controller?.currentPosition?:0L)}
    var duration by remember{mutableLongStateOf(controller?.duration?.coerceAtLeast(0L)?:0L)}

    LaunchedEffect(controller,volume,speed){
        runCatching{controller?.volume=volume}
        runCatching{controller?.setPlaybackSpeed(speed)}
    }


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

    LaunchedEffect(controller){
        while(true){
            val p=controller?:break
            val ok=runCatching{
                position=p.currentPosition.coerceAtLeast(0L)
                duration=p.duration.coerceAtLeast(0L)
            }.isSuccess
            if(!ok)break
            kotlinx.coroutines.delay(500)
        }
    }

    val playlistTracks=if(activePlaylist=="ALL")tracks else {
        val allowed=playlists.firstOrNull{it.name==activePlaylist}?.uris.orEmpty()
        tracks.filter{it.uri in allowed}
    }
    val visibleTracks=playlistTracks.filter{
        searchQuery.isBlank() || it.name.contains(searchQuery.trim(),ignoreCase=true)
    }

    fun playTrack(i:Int){
        val p=controller?:return
        if(visibleTracks.isEmpty())return
        runCatching{
            p.setMediaItems(rsMediaItemsV90(visibleTracks),i.coerceIn(0,visibleTracks.lastIndex),0L)
            p.prepare()
            p.play()
        }.onFailure{feedback=rsMusicBgT90(lang,"player_error")}
    }

    val currentUri=controller?.currentMediaItem?.localConfiguration?.uri?.toString()
        ?: visibleTracks.getOrNull(index.coerceAtLeast(0))?.uri.orEmpty()
    var albumArtwork by remember{mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)}
    LaunchedEffect(currentUri){
        albumArtwork=if(currentUri.isBlank())null else withContext(Dispatchers.IO){
            rsEmbeddedArtworkV175(context,currentUri)
        }
    }

    Box(Modifier.fillMaxSize()){
    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsMusicT(lang,"trainer_title") else rsMusicT(lang,"student_title"),
        rsMusicBgT90(lang,"subtitle")
    ){
        Surface(
            color=Color.Black.copy(alpha=.88f),
            shape=RoundedCornerShape(30.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
            tonalElevation=18.dp,
            modifier=Modifier.fillMaxWidth()
        ){
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(c.gold.copy(alpha=.10f),Color.Black.copy(alpha=.96f))))
                    .padding(16.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                Surface(
                    shape=RoundedCornerShape(28.dp),
                    color=Color.Black,
                    border=androidx.compose.foundation.BorderStroke(2.dp,c.gold.copy(alpha=.72f)),
                    modifier=Modifier.fillMaxWidth().height(220.dp)
                ){
                    Box(Modifier.fillMaxSize()){
                        if(albumArtwork!=null){
                            Image(
                                bitmap=albumArtwork!!,
                                contentDescription="Album artwork",
                                contentScale=ContentScale.Crop,
                                modifier=Modifier.fillMaxSize()
                            )
                            Box(
                                Modifier.matchParentSize().background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent,Color.Black.copy(alpha=.18f),Color.Black.copy(alpha=.74f))
                                    )
                                )
                            )
                        }else{
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.radialGradient(
                                        listOf(c.gold.copy(alpha=.28f),Color(0xFF121212),Color.Black)
                                    )
                                ),
                                contentAlignment=Alignment.Center
                            ){
                                Text("RS ♫",color=c.bright,fontSize=54.sp,fontWeight=FontWeight.Black)
                            }
                        }
                        Surface(
                            shape=RoundedCornerShape(14.dp),
                            color=Color.Black.copy(alpha=.62f),
                            border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.38f)),
                            modifier=Modifier.align(Alignment.TopStart).padding(12.dp)
                        ){
                            Text("ROYAL SOUND",color=c.bright,fontSize=8.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))
                        }
                    }
                }
                RsMusicSpatialVisualizerV201(c,playing,spatialPreset)
                Text("RS MUSIC PRO",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp,letterSpacing=1.5.sp)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)
                ){
                    OutlinedButton(
                        onClick={picker.launch(arrayOf("audio/*"))},
                        modifier=Modifier.weight(1f)
                    ){Text("＋ IMPORT",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(
                        onClick={libraryOpen=true},
                        modifier=Modifier.weight(1f)
                    ){Text("☰ LIBRARY",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(
                        onClick={musicGuideOpen=true},
                        modifier=Modifier.weight(1f)
                    ){Text("✧ AI GUIDE",fontSize=9.sp,fontWeight=FontWeight.Black)}
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    listOf("NEON","BASS","ARENA","STUDIO").forEach{preset->
                        FilterChip(
                            selected=spatialPreset==preset,
                            onClick={
                                spatialPreset=preset
                                store.ps("music_visual_effect_v201",preset)
                            },
                            label={Text(preset,fontSize=8.sp)}
                        )
                    }
                }
                val liveTitle=controller?.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
                Text(
                    liveTitle.ifBlank{if(visibleTracks.isEmpty())"Choose or import a track" else visibleTracks.getOrNull(index.coerceIn(0,visibleTracks.lastIndex))?.name.orEmpty()},
                    color=Color.White,fontWeight=FontWeight.Bold,fontSize=15.sp,maxLines=2,overflow=TextOverflow.Ellipsis
                )
                Text(
                    if(playing)"PLAYING · AI CONTROL READY" else "READY · SOFIA / MARCUS VOICE CONTROL",
                    color=if(playing)Color(0xFF55D58A) else c.muted,
                    fontSize=9.sp,fontWeight=FontWeight.Black
                )
                Slider(
                    value=if(duration>0)(position.toFloat()/duration.toFloat()).coerceIn(0f,1f) else 0f,
                    onValueChange={fraction->runCatching{controller?.seekTo((duration*fraction).toLong())}},
                    modifier=Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceEvenly,
                    verticalAlignment=Alignment.CenterVertically
                ){
                    FilledTonalButton(onClick={runCatching{controller?.seekBack()}}){Text("↶10")}
                    FilledTonalButton(onClick={runCatching{controller?.seekToPreviousMediaItem();controller?.play()}}){Text("⏮")}
                    Button(
                        onClick={
                            val p=controller?:return@Button
                            if(p.mediaItemCount==0 && visibleTracks.isNotEmpty())playTrack(index.coerceIn(0,visibleTracks.lastIndex))
                            else if(p.isPlaying)p.pause() else p.play()
                        },
                        modifier=Modifier.size(66.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text(if(playing)"⏸" else "▶",fontSize=24.sp)}
                    FilledTonalButton(onClick={runCatching{controller?.seekToNextMediaItem();controller?.play()}}){Text("⏭")}
                    FilledTonalButton(onClick={runCatching{controller?.seekForward()}}){Text("10↷")}
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceEvenly,
                    verticalAlignment=Alignment.CenterVertically
                ){
                    FilterChip(
                        selected=shuffleOn,
                        onClick={
                            shuffleOn=!shuffleOn
                            runCatching{controller?.shuffleModeEnabled=shuffleOn}
                        },
                        label={Text("🔀 Shuffle",fontSize=9.sp)}
                    )
                    FilterChip(
                        selected=repeatMode!=Player.REPEAT_MODE_OFF,
                        onClick={
                            repeatMode=when(repeatMode){
                                Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE
                                else->Player.REPEAT_MODE_OFF
                            }
                            runCatching{controller?.repeatMode=repeatMode}
                        },
                        label={Text(if(repeatMode==Player.REPEAT_MODE_ONE)"🔂 One" else if(repeatMode==Player.REPEAT_MODE_ALL)"🔁 All" else "↪ Repeat",fontSize=9.sp)}
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(7.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    val liveUri=controller?.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
                    FilterChip(
                        selected=liveUri.isNotBlank() && liveUri in favorites,
                        onClick={
                            if(liveUri.isNotBlank()){
                                favorites=if(liveUri in favorites)favorites-liveUri else favorites+liveUri
                                store.ps("music_favorites_v201",favorites.joinToString("§"))
                            }
                        },
                        label={Text(if(liveUri in favorites)"★ FAVORITE" else "☆ FAVORITE",fontSize=8.sp)}
                    )
                    Text("SPEED",color=c.muted,fontSize=8.sp,fontWeight=FontWeight.Black)
                    Slider(
                        value=speed,
                        onValueChange={
                            speed=it
                            store.ps("music_speed_v201",it.toString())
                        },
                        valueRange=.75f..1.5f,
                        steps=2,
                        modifier=Modifier.weight(1f)
                    )
                    Text(String.format(java.util.Locale.US,"%.2fx",speed),color=c.bright,fontSize=8.sp)
                }
                OutlinedTextField(
                    value=searchQuery,
                    onValueChange={searchQuery=it.take(80)},
                    singleLine=true,
                    placeholder={Text("Search RS Music…")},
                    modifier=Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(18.dp)
                )
                Text(
                    "Voice: “Play music”, “Next song”, “Stop music”, “Open music player”, or “Play [track name]”.",
                    color=c.muted,fontSize=8.sp
                )
            }
        }

        if(feedback.isNotBlank()){
            RsPanel(c){Text(feedback,color=c.muted,fontSize=10.sp)}
        }

        RsPanel(c){
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Column(Modifier.weight(1f)){
                    Text("LIBRARY & PLAYLISTS",color=c.bright,fontWeight=FontWeight.Black)
                    Text("Import, search, favorites, recent music, queue and named playlists live in the sliding music library.",color=c.muted,fontSize=9.sp)
                }
                Button(onClick={libraryOpen=true}){Text("OPEN")}
            }
        }

        RsPanel(c){
            Text(rsMusicBgT90(lang,"volume"),color=c.bright,fontWeight=FontWeight.Black)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("🔈",fontSize=14.sp)
                Slider(
                    value=volume,
                    onValueChange={v->
                        volume=v
                        store.ps("music_volume_v108",v.toString())
                        runCatching{controller?.volume=v}
                    },
                    valueRange=0f..1f,
                    modifier=Modifier.weight(1f)
                )
                Text((volume*100).toInt().toString()+"%",color=c.muted,fontSize=9.sp,modifier=Modifier.width(40.dp))
            }
        }

        if(tracks.isEmpty()){
            RsPanel(c){
                Text(rsMusicBgT90(lang,"none"),color=c.bright,fontWeight=FontWeight.Bold)
                Text(rsMusicBgT90(lang,"none_desc"),color=c.muted)
            }
        }else{
            val safeList=visibleTracks.ifEmpty{tracks}
            val safeIndex=index.coerceIn(0,safeList.lastIndex)
            Text(rsMusicT(lang,"playlist"),color=c.bright,fontWeight=FontWeight.Black)
            visibleTracks.forEachIndexed{i,track->
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
                            if(activePlaylist=="ALL"){
                                val targetUri=track.uri
                                val next=tracks.filterNot{it.uri==targetUri}
                                tracks=next
                                rsSavePersistentTracksV90(store,next)
                                playlists=playlists.map{p->p.copy(uris=p.uris-targetUri)}
                                rsSaveNamedPlaylistsV108(store,playlists)
                                runCatching{if((controller?.mediaItemCount?:0)>i)controller?.removeMediaItem(i)}
                            }else{
                                playlists=playlists.map{p->if(p.name==activePlaylist)p.copy(uris=p.uris-track.uri) else p}
                                rsSaveNamedPlaylistsV108(store,playlists)
                                runCatching{if((controller?.mediaItemCount?:0)>i)controller?.removeMediaItem(i)}
                            }
                            revision++
                        }){Text(rsMusicT(lang,"remove"))}
                    }
                }
            }
        }
    }

    if(musicGuideOpen){
        AlertDialog(
            onDismissRequest={musicGuideOpen=false},
            confirmButton={
                Button(onClick={musicGuideOpen=false}){Text("GOT IT")}
            },
            title={
                Column{
                    Text("RS MUSIC PRO · AI GUIDE",fontWeight=FontWeight.Black)
                    Text(
                        "Sofia / Marcus music control",
                        color=c.muted,
                        fontSize=10.sp
                    )
                }
            },
            text={
                Column(
                    Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    Text("PLAYER",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    Text("Play / pause · previous / next · ±10 seconds · seek · volume · shuffle · repeat one/all · playback speed.",fontSize=10.sp)

                    Text("LIBRARY",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    Text("Tap LIBRARY to open the right-side sliding panel. Browse All Music, Playlists, Favorites and Recent. Swipe left → right to close it.",fontSize=10.sp)

                    Text("IMPORT & PLAYLISTS",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    Text("Import music directly from the player. Create named playlists inside the PLAYLISTS tab and open any playlist from the same panel.",fontSize=10.sp)

                    Text("VISUAL EFFECTS",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    Text("Choose NEON, BASS, ARENA or STUDIO for the spatial 3D-style visualizer.",fontSize=10.sp)

                    Text("VOICE COMMANDS",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    listOf(
                        "Play music",
                        "Pause music",
                        "Stop music",
                        "Next song",
                        "Previous song",
                        "What is playing?",
                        "Volume up / Volume down",
                        "Open music",
                        "Import music",
                        "Play [track name]",
                        "Play [playlist name]"
                    ).forEach{cmd->
                        Surface(
                            color=c.panel.copy(alpha=.65f),
                            shape=RoundedCornerShape(12.dp),
                            modifier=Modifier.fillMaxWidth()
                        ){
                            Text("“"+cmd+"”",modifier=Modifier.padding(9.dp),fontSize=10.sp)
                        }
                    }

                    Text(
                        "Music keeps playing while you navigate RS KICKBOXING or minimize the app. Android notification controls remain available.",
                        color=c.muted,
                        fontSize=9.sp
                    )
                }
            },
            containerColor=Color(0xFF0B0B0B),
            titleContentColor=Color.White,
            textContentColor=Color.White
        )
    }

    if(libraryOpen){
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha=.58f))
                .zIndex(20f)
                .clickable{libraryOpen=false}
        )
    }
    AnimatedVisibility(
        visible=libraryOpen,
        enter=slideInHorizontally(initialOffsetX={it})+fadeIn(),
        exit=slideOutHorizontally(targetOffsetX={it})+fadeOut(),
        modifier=Modifier.fillMaxHeight().fillMaxWidth(.92f).align(Alignment.CenterEnd).zIndex(21f)
    ){
        var dragX by remember{mutableFloatStateOf(0f)}
        Surface(
            color=Color.Black.copy(alpha=.98f),
            shape=RoundedCornerShape(topStart=28.dp,bottomStart=28.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.45f)),
            modifier=Modifier.fillMaxSize()
                .pointerInput(Unit){
                    detectHorizontalDragGestures(
                        onDragStart={dragX=0f},
                        onHorizontalDrag={change,amount->dragX+=amount;change.consume()},
                        onDragEnd={
                            if(dragX>90f)libraryOpen=false
                            dragX=0f
                        },
                        onDragCancel={dragX=0f}
                    )
                }
        ){
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    Text("RS MUSIC LIBRARY",color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp,modifier=Modifier.weight(1f))
                    TextButton(onClick={libraryOpen=false}){Text("×",fontSize=24.sp,color=c.bright)}
                }
                Text("Swipe left → right to close",color=c.muted,fontSize=8.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf("LIBRARY","PLAYLISTS","FAVORITES","RECENT").forEach{tab->
                        FilterChip(
                            selected=libraryTab==tab,
                            onClick={libraryTab=tab},
                            label={Text(tab,fontSize=7.sp)}
                        )
                    }
                }
                OutlinedTextField(
                    value=searchQuery,
                    onValueChange={searchQuery=it.take(80)},
                    placeholder={Text("Search music…")},
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Button(onClick={picker.launch(arrayOf("audio/*"))},modifier=Modifier.weight(1f)){
                        Text("＋ IMPORT MUSIC",fontSize=9.sp)
                    }
                    OutlinedButton(
                        onClick={libraryTab="PLAYLISTS"},
                        modifier=Modifier.weight(1f)
                    ){Text("＋ PLAYLIST",fontSize=9.sp)}
                }

                when(libraryTab){
                    "PLAYLISTS"->{
                        OutlinedTextField(
                            value=newPlaylistName,
                            onValueChange={newPlaylistName=it.take(60)},
                            label={Text("New playlist name")},
                            singleLine=true,
                            modifier=Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick={
                                val clean=newPlaylistName.trim()
                                if(clean.isNotBlank() && playlists.none{it.name.equals(clean,true)}){
                                    playlists=playlists+RsNamedPlaylistV108(clean,emptySet())
                                    rsSaveNamedPlaylistsV108(store,playlists)
                                    activePlaylist=clean
                                    store.ps("music_active_playlist_v108",clean)
                                    newPlaylistName=""
                                }
                            },
                            enabled=newPlaylistName.trim().isNotBlank(),
                            modifier=Modifier.fillMaxWidth()
                        ){Text("CREATE PLAYLIST")}
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier=Modifier.fillMaxSize(),
                            verticalArrangement=Arrangement.spacedBy(7.dp)
                        ){
                            item{
                                RsPanel(c){
                                    Text("ALL MUSIC",color=c.bright,fontWeight=FontWeight.Black)
                                    Text(tracks.size.toString()+" tracks",color=c.muted,fontSize=9.sp)
                                    TextButton(onClick={
                                        activePlaylist="ALL"
                                        store.ps("music_active_playlist_v108","ALL")
                                        libraryOpen=false
                                    }){Text("OPEN")}
                                }
                            }
                            items(playlists.size){i->
                                val p=playlists[i]
                                RsPanel(c){
                                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                        Column(Modifier.weight(1f)){
                                            Text(p.name,color=c.bright,fontWeight=FontWeight.Black)
                                            Text(p.uris.size.toString()+" tracks",color=c.muted,fontSize=9.sp)
                                        }
                                        TextButton(onClick={
                                            activePlaylist=p.name
                                            store.ps("music_active_playlist_v108",p.name)
                                            libraryOpen=false
                                        }){Text("OPEN")}
                                        TextButton(onClick={
                                            playlists=playlists.filterNot{it.name==p.name}
                                            rsSaveNamedPlaylistsV108(store,playlists)
                                            if(activePlaylist==p.name){
                                                activePlaylist="ALL"
                                                store.ps("music_active_playlist_v108","ALL")
                                            }
                                        }){Text("DELETE",fontSize=8.sp)}
                                    }
                                }
                            }
                        }
                    }
                    else->{
                        val recentUris=store.s("rs_music_recent_history_v165","").split("§").filter{it.isNotBlank()}
                        val drawerTracks=when(libraryTab){
                            "FAVORITES"->tracks.filter{it.uri in favorites}
                            "RECENT"->recentUris.mapNotNull{u->tracks.firstOrNull{it.uri==u}}
                            else->tracks
                        }.filter{searchQuery.isBlank()||it.name.contains(searchQuery,true)}
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier=Modifier.fillMaxSize(),
                            verticalArrangement=Arrangement.spacedBy(6.dp)
                        ){
                            items(drawerTracks.size){i->
                                val track=drawerTracks[i]
                                Surface(
                                    color=c.panel.copy(alpha=.65f),
                                    shape=RoundedCornerShape(16.dp),
                                    border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                                    modifier=Modifier.fillMaxWidth()
                                ){
                                    Row(
                                        Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment=Alignment.CenterVertically,
                                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                                    ){
                                        TextButton(onClick={
                                            val realIndex=visibleTracks.indexOfFirst{it.uri==track.uri}.coerceAtLeast(0)
                                            playTrack(realIndex)
                                            libraryOpen=false
                                        },modifier=Modifier.weight(1f)){
                                            Text(track.name,maxLines=1,overflow=TextOverflow.Ellipsis)
                                        }
                                        TextButton(onClick={
                                            favorites=if(track.uri in favorites)favorites-track.uri else favorites+track.uri
                                            store.ps("music_favorites_v201",favorites.joinToString("§"))
                                        }){Text(if(track.uri in favorites)"★" else "☆")}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun RsMiniMusicPlayerV90(
    c:RsPalette,
    lang:RsLang,
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
                runCatching{if(p.isPlaying)p.pause() else p.play()}
            }){Text(if(playing)"⏸" else "▶")}
            TextButton(onClick=onOpenMusic){Text(rsMusicBgT90(lang,"open"),fontSize=9.sp)}
            TextButton(onClick={
                runCatching{
                    controller?.stop()
                    controller?.clearMediaItems()
                }
            }){Text("✕")}
        }
    }
}


fun rsMusicBgT90(lang:RsLang,key:String):String{
    val en=mapOf(
        "track" to "Training track","added" to "audio file(s) added.",
        "subtitle" to "Music continues across the app and outside it until you stop playback.",
        "background_player" to "BACKGROUND PLAYER",
        "background_desc" to "Playback stays active while you open other RS KICKBOXING pages or minimize the app. Android media controls appear in the notification area.",
        "none" to "No music added yet.","none_desc" to "Add audio from the device to create your RS training playlist.",
        "now_playing" to "NOW PLAYING","stop_close" to "Stop & close player","open" to "OPEN",
        "playlists" to "PLAYLISTS","playlist_name" to "Playlist name","create" to "Create","all_music" to "All music","choose_playlist" to "Choose a playlist","delete_playlist" to "Delete","playlist_help_all" to "Choose a named playlist before adding audio to place new tracks inside it.","playlist_help_named" to "New audio files are added to the selected playlist.","volume" to "VOLUME",
        "player_error" to "The audio player was reset safely. Reopen the track and try again."
    )
    val nl=en+mapOf("track" to "Trainingstrack","added" to "audiobestand(en) toegevoegd.","subtitle" to "Muziek blijft spelen in de app en daarbuiten totdat je stopt.","background_player" to "ACHTERGRONDSPELER","background_desc" to "Muziek blijft spelen terwijl je andere RS KICKBOXING-pagina's opent of de app minimaliseert. Android-mediabediening verschijnt in de meldingsbalk.","none" to "Nog geen muziek toegevoegd.","none_desc" to "Voeg audio van je apparaat toe om je RS-trainingsplaylist te maken.","now_playing" to "NU AAN HET SPELEN","stop_close" to "Stoppen & speler sluiten","open" to "OPENEN","playlists" to "PLAYLISTS","playlist_name" to "Naam playlist","create" to "Maken","all_music" to "Alle muziek","choose_playlist" to "Kies playlist","delete_playlist" to "Verwijder","playlist_help_all" to "Kies eerst een playlist om nieuwe audio daarin toe te voegen.","playlist_help_named" to "Nieuwe audiobestanden worden aan de gekozen playlist toegevoegd.","volume" to "VOLUME")
    val pt=en+mapOf("track" to "Faixa de treino","added" to "ficheiro(s) de áudio adicionado(s).","subtitle" to "A música continua dentro e fora da app até parares.","background_player" to "LEITOR EM SEGUNDO PLANO","background_desc" to "A música continua enquanto abres outras páginas RS KICKBOXING ou minimizas a app. Os controlos aparecem na barra de notificações Android.","none" to "Ainda não adicionaste música.","none_desc" to "Adiciona áudio do dispositivo para criar a tua playlist RS.","now_playing" to "A TOCAR AGORA","stop_close" to "Parar & fechar leitor","open" to "ABRIR","playlists" to "PLAYLISTS","playlist_name" to "Nome da playlist","create" to "Criar","all_music" to "Toda a música","choose_playlist" to "Escolher playlist","delete_playlist" to "Eliminar","playlist_help_all" to "Escolhe uma playlist antes de adicionar áudio para colocar as novas faixas nela.","playlist_help_named" to "Novos ficheiros de áudio são adicionados à playlist selecionada.","volume" to "VOLUME")
    val es=en+mapOf("track" to "Pista de entrenamiento","added" to "archivo(s) de audio añadido(s).","subtitle" to "La música sigue sonando dentro y fuera de la app hasta que la detengas.","background_player" to "REPRODUCTOR EN SEGUNDO PLANO","background_desc" to "La música sigue mientras abres otras páginas de RS KICKBOXING o minimizas la app. Los controles aparecen en la barra de notificaciones de Android.","none" to "Aún no hay música añadida.","none_desc" to "Añade audio del dispositivo para crear tu playlist RS.","now_playing" to "REPRODUCIENDO","stop_close" to "Detener y cerrar reproductor","open" to "ABRIR","playlists" to "PLAYLISTS","playlist_name" to "Nombre de playlist","create" to "Crear","all_music" to "Toda la música","choose_playlist" to "Elegir playlist","delete_playlist" to "Eliminar","playlist_help_all" to "Elige una playlist antes de añadir audio para colocar allí las nuevas pistas.","playlist_help_named" to "Los nuevos archivos se añaden a la playlist seleccionada.","volume" to "VOLUMEN")
    val fr=en+mapOf("track" to "Piste d'entraînement","added" to "fichier(s) audio ajouté(s).","subtitle" to "La musique continue dans l'app et en arrière-plan jusqu'à l'arrêt.","background_player" to "LECTEUR EN ARRIÈRE-PLAN","background_desc" to "La musique continue quand tu ouvres d'autres pages RS KICKBOXING ou réduis l'app. Les contrôles Android apparaissent dans les notifications.","none" to "Aucune musique ajoutée.","none_desc" to "Ajoute de l'audio depuis l'appareil pour créer ta playlist RS.","now_playing" to "LECTURE EN COURS","stop_close" to "Arrêter & fermer le lecteur","open" to "OUVRIR","playlists" to "PLAYLISTS","playlist_name" to "Nom de playlist","create" to "Créer","all_music" to "Toute la musique","choose_playlist" to "Choisir playlist","delete_playlist" to "Supprimer","playlist_help_all" to "Choisis une playlist avant d’ajouter des fichiers audio.","playlist_help_named" to "Les nouveaux fichiers audio sont ajoutés à la playlist sélectionnée.","volume" to "VOLUME")
    val de=en+mapOf("track" to "Trainingstitel","added" to "Audiodatei(en) hinzugefügt.","subtitle" to "Musik läuft in der App und im Hintergrund weiter, bis du sie stoppst.","background_player" to "HINTERGRUND-PLAYER","background_desc" to "Musik läuft weiter, wenn du andere RS KICKBOXING-Seiten öffnest oder die App minimierst. Android-Mediensteuerung erscheint in den Benachrichtigungen.","none" to "Noch keine Musik hinzugefügt.","none_desc" to "Füge Audio vom Gerät hinzu, um deine RS-Playlist zu erstellen.","now_playing" to "JETZT LÄUFT","stop_close" to "Stoppen & Player schließen","open" to "ÖFFNEN","playlists" to "PLAYLISTS","playlist_name" to "Playlist-Name","create" to "Erstellen","all_music" to "Alle Musik","choose_playlist" to "Playlist wählen","delete_playlist" to "Löschen","playlist_help_all" to "Wähle eine Playlist, bevor du neue Audiodateien hinzufügst.","playlist_help_named" to "Neue Audiodateien werden der ausgewählten Playlist hinzugefügt.","volume" to "LAUTSTÄRKE")
    val it=en+mapOf("track" to "Brano allenamento","added" to "file audio aggiunto/i.","subtitle" to "La musica continua dentro e fuori dall'app finché non la fermi.","background_player" to "PLAYER IN BACKGROUND","background_desc" to "La musica continua mentre apri altre pagine RS KICKBOXING o riduci l'app. I controlli Android appaiono nelle notifiche.","none" to "Nessuna musica aggiunta.","none_desc" to "Aggiungi audio dal dispositivo per creare la playlist RS.","now_playing" to "IN RIPRODUZIONE","stop_close" to "Ferma & chiudi player","open" to "APRI","playlists" to "PLAYLIST","playlist_name" to "Nome playlist","create" to "Crea","all_music" to "Tutta la musica","choose_playlist" to "Scegli playlist","delete_playlist" to "Elimina","playlist_help_all" to "Scegli una playlist prima di aggiungere nuovi file audio.","playlist_help_named" to "I nuovi file audio vengono aggiunti alla playlist selezionata.","volume" to "VOLUME")
    val pl=en+mapOf("track" to "Utwór treningowy","added" to "plik(i) audio dodano.","subtitle" to "Muzyka gra w aplikacji i w tle, dopóki jej nie zatrzymasz.","background_player" to "ODTWARZACZ W TLE","background_desc" to "Muzyka gra dalej podczas otwierania innych stron RS KICKBOXING lub minimalizacji aplikacji. Sterowanie Android pojawia się w powiadomieniach.","none" to "Nie dodano jeszcze muzyki.","none_desc" to "Dodaj audio z urządzenia, aby utworzyć playlistę RS.","now_playing" to "TERAZ ODTWARZANE","stop_close" to "Zatrzymaj & zamknij odtwarzacz","open" to "OTWÓRZ","playlists" to "PLAYLISTY","playlist_name" to "Nazwa playlisty","create" to "Utwórz","all_music" to "Cała muzyka","choose_playlist" to "Wybierz playlistę","delete_playlist" to "Usuń","playlist_help_all" to "Wybierz playlistę przed dodaniem nowych plików audio.","playlist_help_named" to "Nowe pliki audio są dodawane do wybranej playlisty.","volume" to "GŁOŚNOŚĆ")
    val tr=en+mapOf("track" to "Antrenman parçası","added" to "ses dosyası eklendi.","subtitle" to "Müzik siz durdurana kadar uygulama içinde ve dışında çalmaya devam eder.","background_player" to "ARKA PLAN OYNATICI","background_desc" to "Diğer RS KICKBOXING sayfalarını açarken veya uygulamayı küçültürken müzik devam eder. Android medya kontrolleri bildirimlerde görünür.","none" to "Henüz müzik eklenmedi.","none_desc" to "RS antrenman çalma listesini oluşturmak için cihazdan ses ekle.","now_playing" to "ŞİMDİ ÇALIYOR","stop_close" to "Durdur & oynatıcıyı kapat","open" to "AÇ","playlists" to "ÇALMA LİSTELERİ","playlist_name" to "Liste adı","create" to "Oluştur","all_music" to "Tüm müzik","choose_playlist" to "Liste seç","delete_playlist" to "Sil","playlist_help_all" to "Yeni ses eklemeden önce bir çalma listesi seç.","playlist_help_named" to "Yeni ses dosyaları seçili çalma listesine eklenir.","volume" to "SES")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

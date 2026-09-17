package com.rskickbox.app

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class RsLocalTrackV20(val uri:String,val name:String)

@Composable
fun RsLocalMusicCenterV20(c:RsPalette, store:RsStore, role:RsRole){
    val context=LocalContext.current
    val tracks=remember{ mutableStateListOf<RsLocalTrackV20>().apply{
        val raw=store.s("local_music_tracks","")
        raw.split("§").filter{it.contains("¤")}.forEach{row->
            val p=row.split("¤",limit=2); if(p.size==2)add(RsLocalTrackV20(p[0],p[1]))
        }
    }}
    var current by remember{ mutableIntStateOf(0) }
    var playing by remember{ mutableStateOf(false) }
    var position by remember{ mutableIntStateOf(0) }
    var duration by remember{ mutableIntStateOf(0) }
    var volume by remember{ mutableFloatStateOf(store.s("music_volume","0.85").toFloatOrNull()?:.85f) }
    var player by remember{ mutableStateOf<MediaPlayer?>(null) }
    var feedback by remember{ mutableStateOf("") }
    var ytLink by remember{ mutableStateOf("") }
    var ytSaved by remember{ mutableStateOf(store.s("youtube_playlist_shortcut","")) }
    var scene by remember{ mutableIntStateOf(0) }

    fun persistTracks(){ store.ps("local_music_tracks",tracks.joinToString("§"){"${it.uri}¤${it.name}"}) }
    fun displayName(uri:Uri):String{
        var name="Training track ${tracks.size+1}"
        runCatching{
            context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
                if(cur.moveToFirst()) name=cur.getString(0)?:name
            }
        }
        return name
    }
    fun release(){
        runCatching{player?.stop()}
        runCatching{player?.release()}
        player=null;playing=false;position=0;duration=0
    }
    fun load(index:Int,auto:Boolean=true){
        if(tracks.isEmpty())return
        val safe=index.coerceIn(0,tracks.lastIndex)
        current=safe
        release()
        runCatching{
            val mp=MediaPlayer()
            mp.setDataSource(context,Uri.parse(tracks[safe].uri))
            mp.setOnPreparedListener{p->
                duration=p.duration
                p.setVolume(volume,volume)
                if(auto){p.start();playing=true}
            }
            mp.setOnCompletionListener{ if(tracks.isNotEmpty()) load((current+1)%tracks.size,true) }
            mp.prepareAsync()
            player=mp
        }.onFailure{feedback="Could not play this file on the device."}
    }
    fun openExternal(url:String){
        runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
            .onFailure{feedback="Could not open the link on this device."}
    }

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        uris.forEach{uri->
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            val track=RsLocalTrackV20(uri.toString(),displayName(uri))
            if(tracks.none{it.uri==track.uri}) tracks.add(track)
        }
        if(uris.isNotEmpty()){
            persistTracks()
            feedback="${uris.size} audio file(s) added to RS Music."
        }
    }

    DisposableEffect(Unit){ onDispose{release()} }
    LaunchedEffect(playing){ while(playing){ delay(500);position=player?.currentPosition?:0;duration=player?.duration?:duration } }
    LaunchedEffect(Unit){ while(true){delay(12000);scene=(scene+1)%4} }

    RsScroll(c,if(role==RsRole.TRAINER)"RS Music Manager" else "My RS Music","Device music, internal playlists and a cinematic training player — no Spotify subscription required."){
        RsPanel(c){
            Text("ADD YOUR OWN MUSIC",color=c.bright,fontWeight=FontWeight.Bold)
            Text("Choose MP3, M4A, AAC, WAV or other audio files supported by Android from your phone, tablet or connected document library.",color=c.muted)
            Button(onClick={picker.launch(arrayOf("audio/*"))},modifier=Modifier.fillMaxWidth()){Text("＋ Add music from device / library")}
            if(feedback.isNotBlank())Text(feedback,color=c.muted)
        }

        if(tracks.isNotEmpty()){
            Box(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(24.dp))){
                RsMusicWallpaperV20(c,store,scene)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.12f),Color.Black.copy(.42f),Color.Black.copy(.82f)))))
                Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.SpaceBetween){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Column{
                            Text("RS LIVE AUDIO",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                            Text("Cinematic training player",color=Color.White.copy(.68f),fontSize=11.sp)
                        }
                        Text("♛",color=c.bright,fontSize=28.sp)
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text(tracks[current.coerceIn(0,tracks.lastIndex)].name,color=Color.White,fontWeight=FontWeight.Black,fontSize=20.sp,maxLines=2)
                        Text("${current+1} / ${tracks.size}",color=c.bright,fontSize=11.sp)
                        Slider(
                            value=if(duration>0) position.toFloat()/duration else 0f,
                            onValueChange={f->
                                player?.seekTo((duration*f).toInt())
                                position=(duration*f).toInt()
                            },
                            modifier=Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                            FilledTonalButton(onClick={
                                if(tracks.size>1){
                                    load(if(current==0)tracks.lastIndex else current-1,true)
                                }
                            }){Text("⏮")}
                            Button(onClick={
                                if(player==null) load(current,true)
                                else if(playing){player?.pause();playing=false}
                                else{player?.start();playing=true}
                            }){Text(if(playing)"⏸ Pause" else "▶ Play")}
                            FilledTonalButton(onClick={
                                if(tracks.size>1){
                                    load((current+1)%tracks.size,true)
                                }
                            }){Text("⏭")}
                        }
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("VOL",color=Color.White.copy(.65f),fontSize=10.sp)
                            Slider(volume,{v->
                                volume=v
                                player?.setVolume(v,v)
                                store.ps("music_volume",v.toString())
                            },modifier=Modifier.weight(1f))
                        }
                    }
                }
            }
        }else{
            RsPanel(c){
                Text("PLAYER READY",color=c.bright,fontWeight=FontWeight.Bold)
                Text("The premium RS audio player appears automatically as soon as the first music file is added.",color=c.muted)
            }
        }

        if(tracks.isNotEmpty())RsPanel(c){
            Text("MY RS PLAYLIST",color=c.bright,fontWeight=FontWeight.Bold)
            tracks.forEachIndexed{i,t->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    TextButton(onClick={load(i,true)},modifier=Modifier.weight(1f)){
                        Column(Modifier.fillMaxWidth()){
                            Text(if(i==current)"▶ ${t.name}" else t.name,color=if(i==current)c.bright else c.text,maxLines=1)
                            Text("Local device audio",color=c.muted,fontSize=9.sp)
                        }
                    }
                    TextButton(onClick={
                        val wasCurrent=i==current
                        if(wasCurrent)release()
                        tracks.removeAt(i)
                        persistTracks()
                        if(current>tracks.lastIndex)current=(tracks.size-1).coerceAtLeast(0)
                    }){Text("Remove")}
                }
            }
        }

        RsPanel(c){
            Text("YOUTUBE PLAYLIST SHORTCUT",color=c.bright,fontWeight=FontWeight.Bold)
            Text("YouTube audio cannot be extracted into the RS background audio player. You can save a YouTube playlist shortcut and open it in YouTube instead.",color=c.muted)
            OutlinedTextField(ytLink,{ytLink=it},label={Text("YouTube playlist link")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(
                onClick={
                    ytSaved=ytLink.trim()
                    store.ps("youtube_playlist_shortcut",ytSaved)
                    feedback="YouTube playlist shortcut saved."
                },
                enabled=ytLink.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text("Save YouTube playlist shortcut")}
            if(ytSaved.isNotBlank())OutlinedButton(onClick={openExternal(ytSaved)},modifier=Modifier.fillMaxWidth()){Text("Open saved playlist in YouTube")}
        }

        if(role==RsRole.TRAINER)RsPanel(c){
            Text("TRAINER MUSIC CONTROL",color=c.bright,fontWeight=FontWeight.Bold)
            Text("The trainer can use the same local RS playlist for warm-up, pads, technical work, controlled sparring and cooldown. Future cloud sync can share approved playlist metadata without uploading copyrighted audio to RS servers.",color=c.muted)
            listOf("Warm-up","Pads / intensity","Technical work","Controlled sparring","Cooldown").forEach{block->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(block,color=c.text)
                    Text(if(tracks.isEmpty())"No music" else tracks[current.coerceIn(0,tracks.lastIndex)].name.take(18),color=c.bright,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RsMusicWallpaperV20(c:RsPalette,store:RsStore,scene:Int){
    val custom=store.s("visual_v21_music_wall_${scene+1}","")
    val infinite=rememberInfiniteTransition(label="musicwall")
    val move by infinite.animateFloat(0f,1f,infiniteRepeatable(tween(7000,easing=LinearEasing),RepeatMode.Reverse),label="move")
    Box(Modifier.fillMaxSize()){
        if(custom.isNotBlank()){
            RsUriPreviewV21(custom,Modifier.fillMaxSize(),store.s("visual_v21_pos_music_wall_${scene+1}","CENTER"))
        }
        Canvas(
            Modifier.fillMaxSize().then(
                if(custom.isBlank()) Modifier.background(Brush.linearGradient(listOf(Color(0xFF080706),Color(0xFF1A1208),Color.Black)))
                else Modifier
            )
        ){
            val w=size.width;val h=size.height
            drawCircle(c.bright.copy(alpha=.10f),w*(.34f+.06f*move),Offset(w*(.18f+.10f*move),h*.16f))
            drawCircle(Color(0xFFE25B3F).copy(alpha=.09f),w*.27f,Offset(w*(.82f-.08f*move),h*.20f))
            repeat(3){i->drawLine(c.bright.copy(alpha=.15f),Offset(0f,h*(.52f+i*.06f)),Offset(w,h*(.49f+i*.06f)),3f)}
            if(custom.isBlank()){
                when(scene){
                    0->{
                        drawCircle(Color.Black.copy(.86f),w*.045f,Offset(w*.68f,h*.26f))
                        drawLine(Color.Black.copy(.90f),Offset(w*.68f,h*.31f),Offset(w*.61f,h*.56f),18f)
                        drawLine(Color.Black.copy(.90f),Offset(w*.65f,h*.37f),Offset(w*.84f,h*.28f),14f)
                        drawLine(Color.Black.copy(.90f),Offset(w*.61f,h*.55f),Offset(w*.47f,h*.76f),17f)
                        drawLine(Color.Black.copy(.90f),Offset(w*.61f,h*.55f),Offset(w*.75f,h*.72f),17f)
                    }
                    1->{
                        drawRoundRect(Color.Black.copy(.72f),Offset(w*.58f,h*.18f),androidx.compose.ui.geometry.Size(w*.13f,h*.43f),androidx.compose.ui.geometry.CornerRadius(28f,28f))
                        drawLine(c.bright.copy(.30f),Offset(w*.645f,0f),Offset(w*.645f,h*.18f),6f)
                    }
                    2->{
                        drawCircle(Color.Black.copy(.80f),w*.04f,Offset(w*.60f,h*.28f))
                        drawCircle(Color.Black.copy(.80f),w*.04f,Offset(w*.76f,h*.30f))
                        drawLine(Color.Black.copy(.82f),Offset(w*.60f,h*.34f),Offset(w*.57f,h*.60f),16f)
                        drawLine(Color.Black.copy(.82f),Offset(w*.76f,h*.36f),Offset(w*.80f,h*.61f),16f)
                        drawLine(c.bright.copy(.25f),Offset(w*.62f,h*.42f),Offset(w*.74f,h*.40f),8f)
                    }
                    else->{
                        repeat(5){i->drawCircle(c.bright.copy(alpha=.08f+i*.015f),w*(.05f+i*.025f),Offset(w*(.45f+i*.09f),h*(.25f+i*.06f)),style=Stroke(5f))}
                    }
                }
            }
        }
        if(custom.isNotBlank()){
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.08f),Color.Black.copy(alpha=.26f),Color.Black.copy(alpha=.52f)))))
        }
    }
}

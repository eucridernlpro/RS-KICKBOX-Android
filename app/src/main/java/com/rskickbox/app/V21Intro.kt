package com.rskickbox.app

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private const val RS_INTRO_VIDEO_MAX_MS = 15_000L

private fun localSplashUriV31(context:android.content.Context,source:String,target:String):String?{
    if(source.isBlank())return null
    val uri=Uri.parse(source)
    if(uri.scheme=="file"){
        val existing=uri.path?.let(::File)
        return if(existing?.exists()==true)source else null
    }
    return runCatching{
        val mime=context.contentResolver.getType(uri).orEmpty()
        val ext=when{
            mime.contains("webm",true)->"webm"
            mime.contains("quicktime",true)->"mov"
            else->"mp4"
        }
        val dir=File(context.filesDir,"rs_splash").apply{mkdirs()}
        dir.listFiles()?.filter{it.name.startsWith(target+"_") }?.forEach{it.delete()}
        val out=File(dir,target+"_"+UUID.randomUUID()+"."+ext)
        context.contentResolver.openInputStream(uri)!!.use{input->
            FileOutputStream(out).use{output->input.copyTo(output)}
        }
        Uri.fromFile(out).toString()
    }.getOrNull()
}

private fun introVideoDurationMsV30(context:android.content.Context,uri:String):Long?{
    return runCatching{
        val mmr=MediaMetadataRetriever()
        mmr.setDataSource(context,Uri.parse(uri))
        val value=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        mmr.release()
        value
    }.getOrNull()
}

@Composable
fun RsCinematicIntroV21(c:RsPalette,store:RsStore,onFinished:()->Unit){
    val enabled=store.b("intro_enabled",true)
    if(!enabled){LaunchedEffect(Unit){onFinished()};return}

    val config=LocalConfiguration.current
    val isTablet=config.smallestScreenWidthDp>=600
    val legacy=store.s("intro_video_uri","")
    val phoneUri=store.s("intro_phone_video_uri",legacy)
    val tabletUri=store.s("intro_tablet_video_uri","")
    val selectedUri=if(isTablet && tabletUri.isNotBlank())tabletUri else phoneUri

    if(selectedUri.isBlank()){
        LaunchedEffect(Unit){onFinished()}
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black)){
        RsIntroVideoStageV30(
            uri=selectedUri,
            sound=store.b("intro_video_sound",true),
            onFinished=onFinished
        )
        if(store.b("intro_skip_enabled",true)){
            TextButton(
                onClick=onFinished,
                modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
            ){Text("Skip",color=Color.White.copy(alpha=.82f))}
        }
    }
}

@Composable
private fun RsIntroVideoStageV30(uri:String,sound:Boolean,onFinished:()->Unit){
    val context=LocalContext.current
    val finished=remember(uri){AtomicBoolean(false)}
    val player=remember(uri){ExoPlayer.Builder(context).build()}
    fun finishOnce(){
        if(finished.compareAndSet(false,true))onFinished()
    }

    DisposableEffect(player){
        val listener=object:Player.Listener{
            override fun onPlaybackStateChanged(state:Int){
                if(state==Player.STATE_ENDED)finishOnce()
            }
            override fun onPlayerError(error:androidx.media3.common.PlaybackException){
                finishOnce()
            }
        }
        player.addListener(listener)
        onDispose{
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(uri,sound){
        finished.set(false)
        player.volume=if(sound)1f else 0f
        player.repeatMode=Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        player.prepare()
        player.playWhenReady=true
    }

    LaunchedEffect(uri){
        kotlinx.coroutines.delay(RS_INTRO_VIDEO_MAX_MS)
        if(!finished.get()){
            runCatching{player.pause()}
            finishOnce()
        }
    }

    AndroidView(
        factory={ctx->
            PlayerView(ctx).apply{
                useController=false
                resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player=player
            }
        },
        update={view->
            view.player=player
            view.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        },
        modifier=Modifier.fillMaxSize().background(Color.Black)
    )
}

@Composable
private fun RsIntroVideoPreviewV30(uri:String,sound:Boolean,height:Int){
    val context=LocalContext.current
    val player=remember(uri){ExoPlayer.Builder(context).build()}
    DisposableEffect(player){onDispose{player.release()}}
    LaunchedEffect(uri,sound){
        player.volume=if(sound)1f else 0f
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        player.prepare()
        player.playWhenReady=false
        player.seekTo(1)
    }
    AndroidView(
        factory={ctx->
            PlayerView(ctx).apply{
                useController=true
                resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player=player
            }
        },
        update={view->view.player=player},
        modifier=Modifier.fillMaxWidth().height(height.dp).background(Color.Black)
    )
}

private data class PendingSplashV30(val uri:String="",val duration:Long=0L)

@Composable
fun RsIntroSettingsV21(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    var enabled by remember{mutableStateOf(store.b("intro_enabled",true))}
    var everyLaunch by remember{mutableStateOf(store.b("intro_every_launch",true))}
    var videoSound by remember{mutableStateOf(store.b("intro_video_sound",true))}
    var skipEnabled by remember{mutableStateOf(store.b("intro_skip_enabled",true))}

    val legacy=store.s("intro_video_uri","")
    var savedPhone by remember{mutableStateOf(store.s("intro_phone_video_uri",legacy))}
    var savedTablet by remember{mutableStateOf(store.s("intro_tablet_video_uri",""))}
    var pendingPhone by remember{mutableStateOf(PendingSplashV30())}
    var pendingTablet by remember{mutableStateOf(PendingSplashV30())}
    var activeTarget by remember{mutableStateOf("phone")}
    var savingTarget by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}

    fun validate(uri:Uri,target:String,persist:Boolean){
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        val candidate=uri.toString()
        val duration=introVideoDurationMsV30(context,candidate)
        when{
            duration==null || duration<=0L->message="Could not read this video's duration. Please choose another file."
            duration>RS_INTRO_VIDEO_MAX_MS->message="Video rejected: "+String.format("%.1f",duration/1000f)+" sec. Maximum splash length is 15.0 sec."
            else->{
                val item=PendingSplashV30(candidate,duration)
                if(target=="tablet")pendingTablet=item else pendingPhone=item
                val label=if(target=="tablet")"Tablet" else "Phone"
                message=label+" splash preview ready: "+String.format("%.2f",duration/1000f)+" sec. Not saved yet."
            }
        }
    }

    LaunchedEffect(Unit){
        if(savedPhone.isNotBlank() && !savedPhone.startsWith("file:")){
            localSplashUriV31(context,savedPhone,"phone")?.let{
                savedPhone=it
                store.ps("intro_phone_video_uri",it)
            }
        }
        if(savedTablet.isNotBlank() && !savedTablet.startsWith("file:")){
            localSplashUriV31(context,savedTablet,"tablet")?.let{
                savedTablet=it
                store.ps("intro_tablet_video_uri",it)
            }
        }
    }

    val galleryPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null)validate(uri,activeTarget,false)
    }
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)validate(uri,activeTarget,true)
    }

    RsScroll(c,"Splash Video Studio","Two dedicated splash videos: one for phones and one for tablets. The app selects the correct version automatically."){
        RsPanel(c){
            Text("SPLASH MASTER CONTROLS",color=c.bright,fontWeight=FontWeight.Black)
            IntroToggleV23("Enable splash video","Master on/off switch",enabled){enabled=it;store.pb("intro_enabled",it)}
            IntroToggleV23("Video sound","Play the splash video's own audio",videoSound){videoSound=it;store.pb("intro_video_sound",it)}
            IntroToggleV23("Show Skip button","Lets members bypass the splash",skipEnabled){skipEnabled=it;store.pb("intro_skip_enabled",it)}
            IntroToggleV23("Show every fresh app launch","Keep the current launch behavior configurable",everyLaunch){everyLaunch=it;store.pb("intro_every_launch",it)}
        }

        SplashEditorV30(
            c=c,
            title="PHONE SPLASH VIDEO",
            format="Recommended: portrait 9:16 · 1080×1920 or similar · MP4 H.264 · 15 sec max",
            saved=savedPhone,
            pending=pendingPhone,
            sound=videoSound,
            onGallery={activeTarget="phone";galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))},
            onFiles={activeTarget="phone";filePicker.launch(arrayOf("video/*"))},
            saving=savingTarget=="phone",
            onSave={
                savingTarget="phone"
                rsOptimizeSplashVideoV32(
                    context,
                    Uri.parse(pendingPhone.uri),
                    "phone",
                    onStatus={message=it},
                    onComplete={local->
                        savedPhone=local
                        store.ps("intro_phone_video_uri",local)
                        pendingPhone=PendingSplashV30()
                        savingTarget=""
                        message="Phone splash converted to H.264/AAC and saved."
                    },
                    onError={err->savingTarget="";message=err}
                )
            },
            onCancel={pendingPhone=PendingSplashV30();message="Pending phone splash discarded."},
            onDelete={
                store.ps("intro_phone_video_uri","")
                savedPhone=""
                pendingPhone=PendingSplashV30()
                message="Phone splash deleted."
            }
        )

        SplashEditorV30(
            c=c,
            title="TABLET SPLASH VIDEO",
            format="Recommended: tablet 16:10 or 4:3 master · minimum 1600px long edge · MP4 H.264 · 15 sec max. Auto-crops to the actual tablet screen.",
            saved=savedTablet,
            pending=pendingTablet,
            sound=videoSound,
            onGallery={activeTarget="tablet";galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))},
            onFiles={activeTarget="tablet";filePicker.launch(arrayOf("video/*"))},
            saving=savingTarget=="tablet",
            onSave={
                savingTarget="tablet"
                rsOptimizeSplashVideoV32(
                    context,
                    Uri.parse(pendingTablet.uri),
                    "tablet",
                    onStatus={message=it},
                    onComplete={local->
                        savedTablet=local
                        store.ps("intro_tablet_video_uri",local)
                        pendingTablet=PendingSplashV30()
                        savingTarget=""
                        message="Tablet splash converted to H.264/AAC and saved."
                    },
                    onError={err->savingTarget="";message=err}
                )
            },
            onCancel={pendingTablet=PendingSplashV30();message="Pending tablet splash discarded."},
            onDelete={
                store.ps("intro_tablet_video_uri","")
                savedTablet=""
                pendingTablet=PendingSplashV30()
                message="Tablet splash deleted."
            }
        )

        RsPanel(c){
            Text("AUTO DEVICE SELECTION",color=c.bright,fontWeight=FontWeight.Black)
            Text("Phones use the Phone Splash. Devices with a smallest screen width of 600dp or more use the Tablet Splash. If no tablet splash is saved, the phone splash is used as fallback.",color=c.text)
            Text("Both videos are center-cropped automatically to fill the real device screen without stretching.",color=c.muted,fontSize=10.sp)
            if(message.isNotBlank())Text(message,color=c.bright,fontSize=10.sp)
        }
    }
}

@Composable
private fun SplashEditorV30(
    c:RsPalette,
    title:String,
    format:String,
    saved:String,
    pending:PendingSplashV30,
    sound:Boolean,
    saving:Boolean,
    onGallery:()->Unit,
    onFiles:()->Unit,
    onSave:()->Unit,
    onCancel:()->Unit,
    onDelete:()->Unit
){
    RsPanel(c){
        Text(title,color=c.bright,fontWeight=FontWeight.Black)
        Text(format,color=Color.White.copy(alpha=.72f),fontSize=10.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Button(onClick=onGallery,enabled=!saving,modifier=Modifier.weight(1f)){Text(if(saved.isBlank()&&pending.uri.isBlank())"Gallery" else "Replace")}
            OutlinedButton(onClick=onFiles,enabled=!saving,modifier=Modifier.weight(1f)){Text("Files")}
        }
        val preview=if(pending.uri.isNotBlank())pending.uri else saved
        if(preview.isNotBlank()){
            Text(if(pending.uri.isNotBlank())"PENDING PREVIEW — NOT SAVED" else "SAVED SPLASH VIDEO",color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            val d=if(pending.uri.isNotBlank())pending.duration else 0L
            if(d>0L)Text("Duration: "+String.format("%.2f",d/1000f)+" sec / 15.00 sec max",color=c.muted,fontSize=10.sp)
            RsIntroVideoPreviewV30(preview,sound,190)
        }
        if(saving){
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Optimizing video for reliable playback…",color=c.bright,fontSize=10.sp)
        }
        if(pending.uri.isNotBlank()){
            Button(onClick=onSave,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text("✓ Save / Accept splash")}
            OutlinedButton(onClick=onCancel,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text("Cancel edit")}
        }
        if(saved.isNotBlank()){
            OutlinedButton(onClick=onDelete,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text("Delete saved splash")}
        }
    }
}

@Composable
private fun IntroToggleV23(title:String,subtitle:String,checked:Boolean,onChecked:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Switch(checked=checked,onCheckedChange=onChecked)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)){
            Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=15.sp)
            Text(subtitle,color=Color.White.copy(alpha=.68f),fontSize=11.sp)
        }
    }
}

package com.rskickbox.app

import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun RsCinematicIntroV21(c:RsPalette,store:RsStore,onFinished:()->Unit){
    val enabled=store.b("intro_enabled",true)
    if(!enabled){LaunchedEffect(Unit){onFinished()};return}

    val videoUri=store.s("intro_video_uri","")
    val stages=remember(
        videoUri,
        store.b("intro_video_enabled",true),
        store.b("intro_fighter_enabled",true),
        store.b("intro_gloves_enabled",true),
        store.b("intro_logo_enabled",true)
    ){
        buildList{
            if(videoUri.isNotBlank() && store.b("intro_video_enabled",true))add("video")
            if(store.b("intro_fighter_enabled",true))add("fighter")
            if(store.b("intro_gloves_enabled",true))add("gloves")
            if(store.b("intro_logo_enabled",true))add("logo")
        }
    }
    if(stages.isEmpty()){LaunchedEffect(Unit){onFinished()};return}

    var stageIndex by remember{mutableIntStateOf(0)}
    val current=stages.getOrNull(stageIndex)?:stages.last()
    fun advance(){
        if(stageIndex<stages.lastIndex)stageIndex++ else onFinished()
    }

    LaunchedEffect(current){
        when(current){
            "fighter"->{delay(1800);advance()}
            "gloves"->{delay(1100);advance()}
            "logo"->{delay(1500);advance()}
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)){
        AnimatedContent(
            targetState=current,
            transitionSpec={fadeIn(tween(420)) togetherWith fadeOut(tween(420))},
            label="introStage"
        ){stage->
            when(stage){
                "video"->RsIntroVideoStageV23(
                    uri=videoUri,
                    sound=store.b("intro_video_sound",true),
                    onFinished={advance()}
                )
                "fighter"->RsIntroFighterSceneV21(c,store)
                "gloves"->RsIntroGlovesSceneV21(c,store)
                else->RsIntroLogoSceneV21(c,store)
            }
        }
        if(store.b("intro_skip_enabled",true)){
            TextButton(
                onClick=onFinished,
                modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
            ){Text("Skip",color=Color.White.copy(alpha=.78f))}
        }
    }
}

private const val RS_INTRO_VIDEO_MAX_MS = 15_000L

private fun introVideoDurationMsV24(context:android.content.Context,uri:String):Long?{
    return runCatching{
        val mmr=MediaMetadataRetriever()
        mmr.setDataSource(context,Uri.parse(uri))
        val value=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        mmr.release()
        value
    }.getOrNull()
}

@Composable
private fun RsIntroVideoStageV23(uri:String,sound:Boolean,onFinished:()->Unit){
    AndroidView(
        factory={ctx->
            VideoView(ctx).apply{
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener{mp->
                    mp.isLooping=false
                    val v=if(sound)1f else 0f
                    mp.setVolume(v,v)
                    start()
                    postDelayed({
                        if(isPlaying && currentPosition>=RS_INTRO_VIDEO_MAX_MS.toInt()-250){
                            pause()
                            onFinished()
                        }
                    },RS_INTRO_VIDEO_MAX_MS)
                }
                setOnCompletionListener{onFinished()}
                setOnErrorListener{_,_,_->onFinished();true}
            }
        },
        modifier=Modifier.fillMaxSize()
    )
}

@Composable
private fun RsIntroFighterSceneV21(c:RsPalette,store:RsStore){
    val custom=store.s("visual_v21_intro_fighter","")
    val overlay=store.s("visual_v21_opacity_intro_fighter","0.34").toFloatOrNull()?:0.34f
    val inf=rememberInfiniteTransition(label="fighter")
    val punch by inf.animateFloat(0f,1f,infiniteRepeatable(tween(620,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="punch")
    val smoke by inf.animateFloat(0f,1f,infiniteRepeatable(tween(2600,easing=LinearEasing),RepeatMode.Reverse),label="smoke")
    Box(Modifier.fillMaxSize()){
        if(custom.isNotBlank())RsUriPreviewV21(custom,Modifier.fillMaxSize(),store.s("visual_v21_pos_intro_fighter","CENTER"))
        Canvas(Modifier.fillMaxSize().then(if(custom.isBlank())Modifier.background(Brush.radialGradient(listOf(Color(0xFF29180A),Color(0xFF090909),Color.Black),center=Offset.Unspecified,radius=1100f)) else Modifier)){
            val w=size.width;val h=size.height
            repeat(7){i->drawCircle(Color.White.copy(alpha=.018f+.015f*smoke),w*(.10f+i*.025f),Offset(w*((i*.19f+smoke*.08f)%1f),h*(.16f+i*.11f)))}
            drawLine(c.bright.copy(alpha=.22f),Offset(0f,h*.12f),Offset(w*.68f,h*.60f),w*.035f)
            drawLine(Color(0xFFE25B3F).copy(alpha=.14f),Offset(w,h*.16f),Offset(w*.38f,h*.61f),w*.03f)
            if(custom.isNotBlank())drawRect(Color.Black.copy(alpha=overlay.coerceIn(0f,.70f)))
            val head=Offset(w*.47f,h*.29f)
            drawCircle(Color.Black.copy(alpha=if(custom.isBlank()).97f else .36f),w*.055f,head)
            drawLine(Color.Black.copy(alpha=if(custom.isBlank())1f else .34f),Offset(w*.47f,h*.35f),Offset(w*.45f,h*.59f),w*.052f)
            drawLine(Color.Black.copy(alpha=if(custom.isBlank())1f else .34f),Offset(w*.45f,h*.43f),Offset(w*(.62f+.16f*punch),h*(.33f-.03f*punch)),w*.038f)
            drawLine(Color.Black.copy(alpha=if(custom.isBlank())1f else .34f),Offset(w*.45f,h*.43f),Offset(w*.31f,h*.50f),w*.034f)
            drawLine(Color.Black.copy(alpha=if(custom.isBlank())1f else .34f),Offset(w*.45f,h*.58f),Offset(w*.29f,h*.82f),w*.045f)
            drawLine(Color.Black.copy(alpha=if(custom.isBlank())1f else .34f),Offset(w*.45f,h*.58f),Offset(w*(.66f+.12f*punch),h*(.72f-.12f*punch)),w*.046f)
            drawCircle(c.bright.copy(alpha=.12f+.10f*punch),w*.20f,Offset(w*.55f,h*.45f),style=Stroke(w*.012f))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(28.dp).navigationBarsPadding()){
            Text("POWER · PRECISION · DISCIPLINE",color=Color.White.copy(alpha=.72f),fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text("RS KICKBOX",color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
private fun RsIntroGlovesSceneV21(c:RsPalette,store:RsStore){
    val custom=store.s("visual_v21_intro_gloves","")
    val overlay=store.s("visual_v21_opacity_intro_gloves","0.30").toFloatOrNull()?:0.30f
    val inf=rememberInfiniteTransition(label="gloves")
    val sway by inf.animateFloat(-1f,1f,infiniteRepeatable(tween(1200,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="sway")
    Box(Modifier.fillMaxSize()){
        if(custom.isNotBlank())RsUriPreviewV21(custom,Modifier.fillMaxSize(),store.s("visual_v21_pos_intro_gloves","CENTER"))
        Canvas(Modifier.fillMaxSize().then(if(custom.isBlank())Modifier.background(Brush.verticalGradient(listOf(Color(0xFF070707),Color.Black))) else Modifier)){
            val w=size.width;val h=size.height
            if(custom.isNotBlank())drawRect(Color.Black.copy(alpha=overlay.coerceIn(0f,.70f)))
            drawLine(c.bright.copy(alpha=if(custom.isBlank()).16f else .10f),Offset(w*.20f,0f),Offset(w*.55f,h),w*.018f)
            val rope1=w*(.43f+sway*.006f);val rope2=w*(.57f-sway*.006f)
            drawLine(Color(0xFF7B6038).copy(alpha=if(custom.isBlank())1f else .38f),Offset(rope1,0f),Offset(rope1,h*.39f),w*.010f)
            drawLine(Color(0xFF7B6038).copy(alpha=if(custom.isBlank())1f else .38f),Offset(rope2,0f),Offset(rope2,h*.42f),w*.010f)
            drawRoundRect(Color(0xFF17110C).copy(alpha=if(custom.isBlank())1f else .32f),Offset(rope1-w*.075f,h*.38f),androidx.compose.ui.geometry.Size(w*.14f,h*.20f),androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f))
            drawRoundRect(Color(0xFF17110C).copy(alpha=if(custom.isBlank())1f else .32f),Offset(rope2-w*.065f,h*.41f),androidx.compose.ui.geometry.Size(w*.14f,h*.20f),androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f))
            drawCircle(c.bright.copy(alpha=.20f),w*.12f,Offset(w*.50f,h*.48f),style=Stroke(w*.009f))
        }
        Text("THE WORK STARTS HERE",modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=54.dp),color=Color.White.copy(alpha=.66f),fontWeight=FontWeight.Bold,fontSize=12.sp)
    }
}

@Composable
private fun RsIntroLogoSceneV21(c:RsPalette,store:RsStore){
    val inf=rememberInfiniteTransition(label="logo")
    val glow by inf.animateFloat(.35f,1f,infiniteRepeatable(tween(750),RepeatMode.Reverse),label="glow")
    val customLogo=store.s("brand_asset_main_logo","")
    val crown=store.s("brand_asset_royal_crown","")
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.gold.copy(alpha=.18f),Color.Black),radius=900f)),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            if(crown.isNotBlank())RsUriPreviewV21(crown,Modifier.size(96.dp),"CENTER")
            else Text("♛",color=c.bright.copy(alpha=glow),fontSize=78.sp,fontWeight=FontWeight.Black)
            if(customLogo.isNotBlank())RsUriPreviewV21(customLogo,Modifier.size(180.dp),"CENTER")
            else Text("RS",color=c.bright,fontSize=92.sp,fontWeight=FontWeight.Black)
            Text(store.s("brand_header_name","RS KICKBOX"),color=Color.White.copy(alpha=.82f),fontSize=20.sp,fontWeight=FontWeight.Bold,letterSpacing=3.sp)
        }
    }
}

@Composable
private fun RsIntroVideoPreviewV23(uri:String,sound:Boolean){
    AndroidView(
        factory={ctx->
            VideoView(ctx).apply{
                val controls=MediaController(ctx)
                controls.setAnchorView(this)
                setMediaController(controls)
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener{mp->
                    val v=if(sound)1f else 0f
                    mp.setVolume(v,v)
                    seekTo(1)
                }
            }
        },
        update={view->
            if(!view.isPlaying && view.currentPosition==0)runCatching{view.seekTo(1)}
        },
        modifier=Modifier.fillMaxWidth().height(230.dp)
    )
}

@Composable
fun RsIntroSettingsV21(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    var enabled by remember{mutableStateOf(store.b("intro_enabled",true))}
    var everyLaunch by remember{mutableStateOf(store.b("intro_every_launch",true))}
    var videoEnabled by remember{mutableStateOf(store.b("intro_video_enabled",true))}
    var videoSound by remember{mutableStateOf(store.b("intro_video_sound",true))}
    var fighterEnabled by remember{mutableStateOf(store.b("intro_fighter_enabled",true))}
    var glovesEnabled by remember{mutableStateOf(store.b("intro_gloves_enabled",true))}
    var logoEnabled by remember{mutableStateOf(store.b("intro_logo_enabled",true))}
    var skipEnabled by remember{mutableStateOf(store.b("intro_skip_enabled",true))}
    var savedVideo by remember{mutableStateOf(store.s("intro_video_uri",""))}
    var pendingVideo by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            val candidate=uri.toString()
            val duration=introVideoDurationMsV24(context,candidate)
            when{
                duration==null || duration<=0L->{
                    pendingVideo=""
                    message="Could not read this video's duration. Please choose another file."
                }
                duration>RS_INTRO_VIDEO_MAX_MS->{
                    pendingVideo=""
                    message="Video rejected: "+String.format("%.1f",duration/1000f)+" sec. Maximum allowed intro video is 15.0 sec."
                }
                else->{
                    pendingVideo=candidate
                    message="Local preview ready: "+String.format("%.2f",duration/1000f)+" sec. It is not saved until you press Save / Accept."
                }
            }
        }
    }

    RsScroll(c,"Intro Director","Upload and preview the opening video locally before accepting it. All intro elements can be switched on or off from this same screen."){
        RsPanel(c){
            Text("INTRO MASTER CONTROLS",color=c.bright,fontWeight=FontWeight.Black)
            Text("All intro controls remain visible on this screen.",color=Color.White.copy(alpha=.68f),fontSize=10.sp)
            IntroToggleV23("Enable cinematic intro","Master on/off switch",enabled){enabled=it;store.pb("intro_enabled",it)}
            IntroToggleV23("Use uploaded intro video","Play the accepted local video first",videoEnabled){videoEnabled=it;store.pb("intro_video_enabled",it)}
            IntroToggleV23("Video sound","Play the uploaded video's own audio",videoSound){videoSound=it;store.pb("intro_video_sound",it)}
            IntroToggleV23("Fighter action scene","Animated or uploaded fighter scene",fighterEnabled){fighterEnabled=it;store.pb("intro_fighter_enabled",it)}
            IntroToggleV23("Hanging gloves scene","Animated or uploaded gloves reveal",glovesEnabled){glovesEnabled=it;store.pb("intro_gloves_enabled",it)}
            IntroToggleV23("RS crown + logo reveal","Final royal brand reveal",logoEnabled){logoEnabled=it;store.pb("intro_logo_enabled",it)}
            IntroToggleV23("Show Skip button","Lets members bypass the sequence",skipEnabled){skipEnabled=it;store.pb("intro_skip_enabled",it)}
            IntroToggleV23("Show every fresh app launch","Otherwise show once until intro state is reset",everyLaunch){everyLaunch=it;store.pb("intro_every_launch",it)}
        }

        RsPanel(c){
            Text("LOCAL INTRO VIDEO",color=c.bright,fontWeight=FontWeight.Black)
            Text("Choose a video from the phone, tablet or compatible document library. Maximum length is 15 seconds. The selected file is preview-only until explicitly accepted.",color=c.muted)
            Text("15 SEC MAX · local validation before save",color=c.bright,fontWeight=FontWeight.Bold,fontSize=11.sp)
            Button(
                onClick={picker.launch(arrayOf("video/*"))},
                modifier=Modifier.fillMaxWidth()
            ){Text(if(savedVideo.isBlank() && pendingVideo.isBlank())"＋ Upload intro video" else "✎ Edit / Replace video")}

            val previewUri=if(pendingVideo.isNotBlank())pendingVideo else savedVideo
            val previewDuration=remember(previewUri){if(previewUri.isBlank())null else introVideoDurationMsV24(context,previewUri)}
            if(previewUri.isNotBlank()){
                Text(if(pendingVideo.isNotBlank())"PENDING LOCAL PREVIEW — NOT SAVED" else "SAVED INTRO VIDEO",color=c.bright,fontWeight=FontWeight.Bold,fontSize=11.sp)
                previewDuration?.let{d->
                    Text("Duration: "+String.format("%.2f",d/1000f)+" sec / 15.00 sec max",color=if(d<=RS_INTRO_VIDEO_MAX_MS)c.muted else MaterialTheme.colorScheme.error,fontSize=10.sp)
                }
                RsIntroVideoPreviewV23(previewUri,videoSound)
            }

            if(pendingVideo.isNotBlank()){
                Button(
                    onClick={
                        val duration=introVideoDurationMsV24(context,pendingVideo)
                        if(duration!=null && duration in 1..RS_INTRO_VIDEO_MAX_MS){
                            savedVideo=pendingVideo
                            store.ps("intro_video_uri",pendingVideo)
                            pendingVideo=""
                            message="Intro video accepted and saved. 15-second safety limit active."
                        }else{
                            message="This video cannot be saved because it exceeds the 15-second intro limit or its duration cannot be verified."
                        }
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text("✓ Save / Accept video")}
                OutlinedButton(
                    onClick={pendingVideo="";message="Pending video discarded. Saved intro remains unchanged."},
                    modifier=Modifier.fillMaxWidth()
                ){Text("Cancel edit")}
            }

            if(savedVideo.isNotBlank()){
                OutlinedButton(
                    onClick={
                        store.ps("intro_video_uri","")
                        savedVideo=""
                        pendingVideo=""
                        message="Saved intro video deleted."
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text("Delete saved video")}
            }
            if(message.isNotBlank())Text(message,color=c.muted)
        }

        RsPanel(c){
            Text("CURRENT INTRO ORDER",color=c.bright,fontWeight=FontWeight.Black)
            val active=buildList{
                if(videoEnabled && savedVideo.isNotBlank())add("Uploaded video")
                if(fighterEnabled)add("Fighter action")
                if(glovesEnabled)add("Hanging gloves")
                if(logoEnabled)add("RS crown + logo")
            }
            Text(if(active.isEmpty())"No intro stages selected — login opens immediately." else active.joinToString("  →  "),color=c.text)
            Text("Uploaded video is limited to 15 seconds maximum. The optional fighter, gloves and logo stages can follow it as separate premium reveal steps.",color=c.muted,fontSize=10.sp)
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
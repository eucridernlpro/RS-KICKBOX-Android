package com.rskickbox.app

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.VideoView
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
import kotlinx.coroutines.launch

private const val RS_INTRO_VIDEO_MAX_MS = 17_000L

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
fun RsCinematicIntroV21(c:RsPalette,store:RsStore,lang:RsLang,onFinished:()->Unit){
    val enabled=store.b("intro_enabled",true)
    if(!enabled){LaunchedEffect(Unit){onFinished()};return}

    val config=LocalConfiguration.current
    val isTablet=config.smallestScreenWidthDp>=600
    val legacy=store.s("intro_video_uri","")
    val phoneUri=store.s("intro_phone_video_uri",legacy)
    val tabletUri=store.s("intro_tablet_video_uri","")
    val selectedUri=if(isTablet && tabletUri.isNotBlank())tabletUri else phoneUri
    val selectedPlayable=remember(selectedUri){
        if(selectedUri.isBlank())false
        else{
            val parsed=runCatching{Uri.parse(selectedUri)}.getOrNull()
            when(parsed?.scheme){
                "file"->parsed.path?.let(::File)?.let{it.exists()&&it.length()>0L}==true
                else->true
            }
        }
    }

    // Crash-loop guard: if the previous process died while the intro was marked
    // active, skip it once and let the already initialized app continue.
    val previousIntroInterrupted=remember{store.b("intro_playback_guard_v141",false)}
    if(previousIntroInterrupted){
        LaunchedEffect(Unit){
            store.pb("intro_playback_guard_v141",false)
            onFinished()
        }
        return
    }

    if(!selectedPlayable){
        LaunchedEffect(selectedUri){onFinished()}
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black)){
        RsSafeIntroVideoStageV141(
            uri=selectedUri,
            sound=store.b("intro_video_sound",true),
            onStarted={store.pb("intro_playback_guard_v141",true)},
            onFinished={
                store.pb("intro_playback_guard_v141",false)
                onFinished()
            }
        )
        if(store.b("intro_skip_enabled",true)){
            TextButton(
                onClick={
                    store.pb("intro_playback_guard_v141",false)
                    onFinished()
                },
                modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
            ){Text(rsIntroT102(lang,"skip"),color=Color.White.copy(alpha=.82f))}
        }
    }
}

@Composable
private fun RsSafeIntroVideoStageV141(
    uri:String,
    sound:Boolean,
    onStarted:()->Unit,
    onFinished:()->Unit
){
    RsSafeIntroVideoWithProgressV147(
        uri=uri,
        sound=sound,
        showProgress=false,
        onStarted=onStarted,
        onFinished=onFinished
    )
}

@Composable
fun RsSafeIntroVideoWithProgressV147(
    uri:String,
    sound:Boolean,
    showProgress:Boolean=true,
    onStarted:()->Unit,
    onFinished:()->Unit
){
    val context=LocalContext.current
    val finished=remember(uri){AtomicBoolean(false)}
    var videoView by remember(uri){mutableStateOf<VideoView?>(null)}
    var prepared by remember(uri){mutableStateOf(false)}
    var durationMs by remember(uri){mutableLongStateOf(0L)}
    var progress by remember(uri){mutableFloatStateOf(0f)}

    fun finishOnce(){
        if(finished.compareAndSet(false,true)){
            runCatching{videoView?.stopPlayback()}
            onFinished()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)){
        AndroidView(
            factory={ctx->
                VideoView(ctx).apply{
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setOnPreparedListener{mp->
                        mp.isLooping=false
                        mp.setVolume(if(sound)1f else 0f,if(sound)1f else 0f)
                        runCatching{
                            mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        }
                        durationMs=duration.toLong().coerceAtLeast(1L)
                        prepared=true
                        onStarted()
                        start()
                    }
                    setOnCompletionListener{
                        progress=1f
                        finishOnce()
                    }
                    setOnErrorListener{_,_,_->
                        finishOnce()
                        true
                    }
                    videoView=this
                    runCatching{setVideoURI(Uri.parse(uri))}
                        .onFailure{finishOnce()}
                }
            },
            update={view->videoView=view},
            modifier=Modifier.fillMaxSize().background(Color.Black)
        )

        if(showProgress){
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal=22.dp,vertical=18.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ){
                LinearProgressIndicator(
                    progress={progress.coerceIn(0f,1f)},
                    modifier=Modifier.fillMaxWidth().height(5.dp),
                    color=Color(0xFFD6B15E),
                    trackColor=Color.White.copy(alpha=.18f)
                )
                Text(
                    if(prepared)"RS KICKBOXING" else "Preparing intro…",
                    color=Color.White.copy(alpha=.74f),
                    fontSize=8.sp,
                    fontWeight=FontWeight.Bold,
                    modifier=Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    LaunchedEffect(uri,prepared,durationMs){
        if(prepared && durationMs>0L){
            while(!finished.get()){
                val pos=runCatching{videoView?.currentPosition?.toLong()?:0L}.getOrDefault(0L)
                progress=(pos.toFloat()/durationMs.toFloat()).coerceIn(0f,1f)
                kotlinx.coroutines.delay(50L)
            }
        }
    }

    LaunchedEffect(uri){
        kotlinx.coroutines.delay(RS_INTRO_VIDEO_MAX_MS+1500L)
        if(!finished.get())finishOnce()
    }

    DisposableEffect(uri){
        onDispose{
            runCatching{videoView?.stopPlayback()}
            videoView=null
        }
    }
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
fun RsIntroSettingsV21(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
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
    var previewUri by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}

    fun saveCloudBehavior(){
        if(RsSupabaseV60.configured){
            scope.launch{
                rsSaveCloudIntroSettingsV102(enabled,everyLaunch,videoSound,skipEnabled)
                    .onSuccess{message=rsIntroT102(lang,"cloud_saved")}
                    .onFailure{message=it.message?:rsIntroT102(lang,"cloud_failed")}
            }
        }
    }

    fun validate(uri:Uri,target:String,persist:Boolean){
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        val candidate=uri.toString()
        val duration=introVideoDurationMsV30(context,candidate)
        when{
            duration==null || duration<=0L->message=rsIntroT102(lang,"read_error")
            duration>RS_INTRO_VIDEO_MAX_MS->message=rsIntroT102(lang,"too_long")
            else->{
                val item=PendingSplashV30(candidate,duration)
                if(target=="tablet")pendingTablet=item else pendingPhone=item
                message=if(target=="tablet")rsIntroT102(lang,"tablet_ready") else rsIntroT102(lang,"phone_ready")
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

    RsScroll(c,rsIntroT102(lang,"studio"),rsIntroT102(lang,"studio_sub")){
        RsPanel(c){
            Text(rsIntroT102(lang,"master"),color=c.bright,fontWeight=FontWeight.Black)
            IntroToggleV23(rsIntroT102(lang,"enable"),rsIntroT102(lang,"enable_sub"),enabled){enabled=it;store.pb("intro_enabled",it);saveCloudBehavior()}
            IntroToggleV23(rsIntroT102(lang,"sound"),rsIntroT102(lang,"sound_sub"),videoSound){videoSound=it;store.pb("intro_video_sound",it)}
            IntroToggleV23(rsIntroT102(lang,"skip_toggle"),rsIntroT102(lang,"skip_sub"),skipEnabled){skipEnabled=it;store.pb("intro_skip_enabled",it);saveCloudBehavior()}
            IntroToggleV23(rsIntroT102(lang,"every"),rsIntroT102(lang,"every_sub"),everyLaunch){everyLaunch=it;store.pb("intro_every_launch",it);saveCloudBehavior()}
        }

        SplashEditorV30(
            c=c,
            lang=lang,
            title=rsIntroT102(lang,"phone"),
            format=rsIntroT102(lang,"phone_format"),
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
                        message=rsIntroT102(lang,"phone_synced")
                        if(RsSupabaseV60.configured){
                            scope.launch{
                                rsUploadCloudVisualAssetV101(context,"intro:phone",local,"VIDEO","CENTER",0f)
                                    .onSuccess{message=rsIntroT102(lang,"phone_synced")}
                                    .onFailure{message=it.message?:rsIntroT102(lang,"local_cloud_fail")}
                                savingTarget=""
                            }
                        }else savingTarget=""
                    },
                    onError={err->savingTarget="";message=err}
                )
            },
            onPreview={candidate->previewUri=candidate},
            onCancel={pendingPhone=PendingSplashV30();message=rsIntroT102(lang,"discarded")},
            onDelete={
                store.ps("intro_phone_video_uri","")
                savedPhone=""
                pendingPhone=PendingSplashV30()
                message=rsIntroT102(lang,"deleted")
                if(RsSupabaseV60.configured)scope.launch{rsDeleteCloudVisualAssetV101("intro:phone")}
            }
        )

        SplashEditorV30(
            c=c,
            lang=lang,
            title=rsIntroT102(lang,"tablet"),
            format=rsIntroT102(lang,"tablet_format"),
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
                        message=rsIntroT102(lang,"tablet_synced")
                        if(RsSupabaseV60.configured){
                            scope.launch{
                                rsUploadCloudVisualAssetV101(context,"intro:tablet",local,"VIDEO","CENTER",0f)
                                    .onSuccess{message=rsIntroT102(lang,"tablet_synced")}
                                    .onFailure{message=it.message?:rsIntroT102(lang,"local_cloud_fail")}
                                savingTarget=""
                            }
                        }else savingTarget=""
                    },
                    onError={err->savingTarget="";message=err}
                )
            },
            onPreview={candidate->previewUri=candidate},
            onCancel={pendingTablet=PendingSplashV30();message=rsIntroT102(lang,"discarded")},
            onDelete={
                store.ps("intro_tablet_video_uri","")
                savedTablet=""
                pendingTablet=PendingSplashV30()
                message=rsIntroT102(lang,"deleted")
                if(RsSupabaseV60.configured)scope.launch{rsDeleteCloudVisualAssetV101("intro:tablet")}
            }
        )

        RsPanel(c){
            Text(rsIntroT102(lang,"auto"),color=c.bright,fontWeight=FontWeight.Black)
            Text(rsIntroT102(lang,"auto_desc"),color=c.text)
            Text(rsIntroT102(lang,"crop_desc"),color=c.muted,fontSize=10.sp)
            if(message.isNotBlank())Text(message,color=c.bright,fontSize=10.sp)
        }
    }

    if(previewUri.isNotBlank()){
        androidx.compose.ui.window.Dialog(
            onDismissRequest={previewUri=""}
        ){
            Surface(
                color=Color.Black,
                modifier=Modifier.fillMaxWidth().fillMaxHeight(.94f)
            ){
                Box(Modifier.fillMaxSize().background(Color.Black)){
                    RsSafeIntroVideoStageV141(
                        uri=previewUri,
                        sound=videoSound,
                        onStarted={},
                        onFinished={previewUri=""}
                    )
                    TextButton(
                        onClick={previewUri=""},
                        modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                    ){Text(rsIntroT102(lang,"close_preview"),color=Color.White)}
                    Text(
                        rsIntroT102(lang,"full_test"),
                        color=Color.White.copy(alpha=.76f),
                        fontSize=9.sp,
                        fontWeight=FontWeight.Bold,
                        modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashEditorV30(
    c:RsPalette,
    lang:RsLang,
    title:String,
    format:String,
    saved:String,
    pending:PendingSplashV30,
    sound:Boolean,
    saving:Boolean,
    onGallery:()->Unit,
    onFiles:()->Unit,
    onSave:()->Unit,
    onPreview:(String)->Unit,
    onCancel:()->Unit,
    onDelete:()->Unit
){
    RsPanel(c){
        Text(title,color=c.bright,fontWeight=FontWeight.Black)
        Text(format,color=Color.White.copy(alpha=.72f),fontSize=10.sp)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Button(onClick=onGallery,enabled=!saving,modifier=Modifier.weight(1f)){Text(if(saved.isBlank()&&pending.uri.isBlank())rsIntroT102(lang,"gallery") else rsIntroT102(lang,"replace"))}
            OutlinedButton(onClick=onFiles,enabled=!saving,modifier=Modifier.weight(1f)){Text(rsIntroT102(lang,"files"))}
        }
        val preview=if(pending.uri.isNotBlank())pending.uri else saved
        if(preview.isNotBlank()){
            Text(if(pending.uri.isNotBlank())rsIntroT102(lang,"pending") else rsIntroT102(lang,"saved"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            val d=if(pending.uri.isNotBlank())pending.duration else 0L
            if(d>0L)Text(rsIntroT102(lang,"duration")+": "+String.format("%.2f",d/1000f)+" sec / "+rsIntroT102(lang,"max"),color=c.muted,fontSize=10.sp)
            RsIntroVideoPreviewV30(preview,sound,190)
            Button(
                onClick={onPreview(preview)},
                enabled=!saving,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsIntroT102(lang,"test"))}
        }
        if(saving){
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(rsIntroT102(lang,"optimizing"),color=c.bright,fontSize=10.sp)
        }
        if(pending.uri.isNotBlank()){
            Button(onClick=onSave,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text(rsIntroT102(lang,"save"))}
            OutlinedButton(onClick=onCancel,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text(rsIntroT102(lang,"cancel"))}
        }
        if(saved.isNotBlank()){
            OutlinedButton(onClick=onDelete,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text(rsIntroT102(lang,"delete"))}
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

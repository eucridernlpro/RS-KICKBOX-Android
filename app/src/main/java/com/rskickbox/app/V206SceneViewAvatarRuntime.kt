package com.rskickbox.app

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay

enum class RsAiSceneStageV206 {
    REFERENCE,
    LOADING_3D,
    LIVE_3D,
    CIRCUIT_BREAKER
}

data class RsAiGlbHealthV206(
    val exists:Boolean,
    val validHeader:Boolean,
    val byteSize:Long,
    val reason:String
){
    val usable:Boolean get()=exists && validHeader && byteSize>1024L
}

private fun rsAiGlbHealthV206(context:Context,path:String):RsAiGlbHealthV206{
    return runCatching{
        context.assets.openFd(path).use{fd->
            val size=fd.length
            context.assets.open(path).use{input->
                val header=ByteArray(4)
                val read=input.read(header)
                val valid=read==4 &&
                    header[0].toInt()==0x67 &&
                    header[1].toInt()==0x6C &&
                    header[2].toInt()==0x54 &&
                    header[3].toInt()==0x46
                RsAiGlbHealthV206(
                    exists=true,
                    validHeader=valid,
                    byteSize=size,
                    reason=if(valid)"READY" else "INVALID_GLB_HEADER"
                )
            }
        }
    }.getOrElse{
        RsAiGlbHealthV206(false,false,0L,"MISSING")
    }
}

private fun rsAiSceneCircuitOpenV206(store:RsStore,avatar:String):Boolean{
    val key="rs_scene_state_v206_"+avatar.lowercase()
    val state=store.s(key,"")
    val started=store.s(key+"_ms","0").toLongOrNull()?:0L
    val age=System.currentTimeMillis()-started
    // If the previous process died shortly after marking SceneView as STARTING,
    // assume a native/runtime failure and keep this avatar in safe fallback mode.
    return state=="STARTING_RENDER" && age in 0L..15L*60L*1000L
}

private fun rsAiSceneMarkV206(store:RsStore,avatar:String,state:String){
    val key="rs_scene_state_v206_"+avatar.lowercase()
    store.ps(key,state)
    store.ps(key+"_ms",System.currentTimeMillis().toString())
    store.ps("rs_ai_last_checkpoint_v205","SCENE_"+avatar+"_"+state)
    store.ps("rs_ai_last_checkpoint_ms_v205",System.currentTimeMillis().toString())
}

@Composable
private fun RsAiCinematicReferenceV206(
    avatar:String,
    modifier:Modifier
){
    val accent=if(avatar=="MALE")Color(0xFF58C9FF) else Color(0xFFD9A7C7)
    Box(
        modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black)
    ){
        Image(
            painter=painterResource(
                if(avatar=="MALE")R.drawable.rs_ai_marcus_default
                else R.drawable.rs_ai_sofia_default
            ),
            contentDescription=if(avatar=="MALE")"Marcus RS AI" else "Sofia RS AI",
            contentScale=ContentScale.Crop,
            modifier=Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha=.05f),
                        Color.Transparent,
                        Color.Black.copy(alpha=.42f)
                    )
                )
            )
        )
        androidx.compose.material3.Surface(
            color=Color.Black.copy(alpha=.62f),
            shape=RoundedCornerShape(14.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,accent.copy(alpha=.44f)),
            modifier=Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(8.dp)
        ){
            androidx.compose.foundation.layout.Column(
                Modifier.padding(horizontal=10.dp,vertical=6.dp),
                horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally
            ){
                androidx.compose.material3.Text(
                    if(avatar=="MALE")"MARCUS" else "SOFIA",
                    color=Color.White,
                    fontSize=12.sp,
                    fontWeight=androidx.compose.ui.text.font.FontWeight.Black,
                    letterSpacing=1.4.sp
                )
                androidx.compose.material3.Text(
                    "RS DIGITAL HUMAN · CINEMATIC REFERENCE",
                    color=accent,
                    fontSize=5.sp,
                    fontWeight=androidx.compose.ui.text.font.FontWeight.Black,
                    letterSpacing=.7.sp
                )
            }
        }
    }
}

@Composable
fun RsAiSceneAvatarV206(
    store:RsStore,
    avatar:String,
    speaking:Boolean,
    listening:Boolean,
    thinking:Boolean,
    motionEnabled:Boolean,
    modifier:Modifier=Modifier,
    onStage:(RsAiSceneStageV206)->Unit={}
){
    val context=LocalContext.current
    val identity=remember(avatar){rsAiIdentityV193(avatar)}
    val assetPath=identity.modelAsset
    val health=remember(assetPath){rsAiGlbHealthV206(context,assetPath)}
    val manuallyDisabled=store.b("rs_sceneview_disabled_v206",false)
    val circuitOpen=remember(avatar){rsAiSceneCircuitOpenV206(store,avatar)}
    val allow3d=health.usable && !manuallyDisabled && !circuitOpen

    if(!allow3d){
        val stage=if(circuitOpen)RsAiSceneStageV206.CIRCUIT_BREAKER else RsAiSceneStageV206.REFERENCE
        LaunchedEffect(stage,avatar){
            onStage(stage)
            if(circuitOpen)rsAiSceneMarkV206(store,avatar,"CIRCUIT_BREAKER")
            else{
                store.ps("rs_scene_health_v206_"+avatar.lowercase(),health.reason)
                store.ps("rs_scene_health_bytes_v206_"+avatar.lowercase(),health.byteSize.toString())
            }
        }
        RsAiCinematicReferenceV206(avatar,modifier)
        return
    }

    val engine=rememberEngine()
    val modelLoader=rememberModelLoader(engine)
    val cameraNode=rememberCameraNode(engine){
        position=Position(0f,.18f,2.45f)
        lookAt(Position(0f,.72f,0f))
    }
    val modelInstance=rememberModelInstance(modelLoader,assetPath)

    val state=rsAiAvatarStateV193(
        speaking=speaking,
        listening=listening,
        thinking=thinking
    )
    val requestedAnimation=rsAiAnimationNameV193(identity,state)

    LaunchedEffect(avatar,modelInstance){
        if(modelInstance==null){
            onStage(RsAiSceneStageV206.LOADING_3D)
            rsAiSceneMarkV206(store,avatar,"LOADING_MODEL")
            return@LaunchedEffect
        }
        onStage(RsAiSceneStageV206.LOADING_3D)
        rsAiSceneMarkV206(store,avatar,"STARTING_RENDER")
        delay(1400)
        rsAiSceneMarkV206(store,avatar,"READY")
        onStage(RsAiSceneStageV206.LIVE_3D)
    }

    DisposableEffect(avatar){
        onDispose{
            // A normal Compose disposal is not a crash. Mark it explicitly so
            // the next entry is allowed to try SceneView again.
            val state=store.s("rs_scene_state_v206_"+avatar.lowercase(),"")
            if(state in setOf("LOADING_MODEL","STARTING_RENDER","READY")){
                rsAiSceneMarkV206(store,avatar,"CLOSED_CLEAN")
            }
        }
    }

    SceneView(
        modifier=modifier.clip(RoundedCornerShape(22.dp)),
        engine=engine,
        modelLoader=modelLoader,
        cameraNode=cameraNode,
        cameraManipulator=null,
        isOpaque=false
    ){
        modelInstance?.let{instance->
            ModelNode(
                modelInstance=instance,
                // SceneView docs: autoAnimate=true ignores animationName.
                // Keep it false so our AI state machine owns animation changes.
                autoAnimate=false,
                animationName=if(motionEnabled)requestedAnimation else null,
                animationLoop=true,
                animationSpeed=1f,
                scaleToUnits=1.72f,
                centerOrigin=Position(0f,-1f,0f),
                position=Position(0f,-.82f,0f),
                isEditable=false,
                apply={
                    isShadowCaster=true
                    isShadowReceiver=true
                    onFrameError={error->
                        store.ps("rs_scene_last_error_v206",error.javaClass.simpleName+":"+error.message.orEmpty().take(180))
                        store.pb("rs_sceneview_disabled_v206",true)
                        rsAiSceneMarkV206(store,avatar,"FRAME_ERROR")
                    }
                }
            )
        }
    }
}

fun rsAiSceneDiagnosticsV206(context:Context,store:RsStore,avatar:String):String{
    val health=rsAiGlbHealthV206(context,rsAiIdentityV193(avatar).modelAsset)
    val state=store.s("rs_scene_state_v206_"+avatar.lowercase(),"NONE")
    val disabled=store.b("rs_sceneview_disabled_v206",false)
    return buildString{
        append(if(avatar=="MALE")"Marcus" else "Sofia")
        append(" · ")
        append(health.reason)
        append(" · ")
        append(state)
        if(disabled)append(" · DISABLED")
    }
}

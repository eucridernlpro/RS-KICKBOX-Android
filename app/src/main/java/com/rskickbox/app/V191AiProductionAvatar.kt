package com.rskickbox.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Production avatar bridge.
 *
 * If a rigged GLB is bundled at assets/models/rs_ai_sofia.glb or
 * assets/models/rs_ai_marcus.glb we render it with SceneView/Filament.
 * Until those fixed production assets are available we keep the local
 * procedural renderer as a crash-safe fallback.
 */
@Composable
fun RsAiProductionAvatarV191(
    avatar:String,
    speaking:Boolean,
    speechAmplitude:Float,
    motionEnabled:Boolean,
    listening:Boolean,
    thinking:Boolean,
    sensorX:Float,
    sensorY:Float,
    modifier:Modifier=Modifier
){
    val context=LocalContext.current
    val assetPath=if(avatar=="FEMALE"){
        "models/rs_ai_sofia.glb"
    }else{
        "models/rs_ai_marcus.glb"
    }
    val hasRiggedAsset=remember(assetPath){
        runCatching{
            context.assets.open(assetPath).use{}
            true
        }.getOrDefault(false)
    }

    if(!hasRiggedAsset){
        RsAiReal3DModelV183(
            avatar=avatar,
            speaking=speaking,
            speechAmplitude=speechAmplitude,
            motionEnabled=motionEnabled,
            listening=listening,
            thinking=thinking,
            sensorX=sensorX,
            sensorY=sensorY,
            modifier=modifier
        )
        return
    }

    val engine=rememberEngine()
    val modelLoader=rememberModelLoader(engine)
    val cameraNode=rememberCameraNode(engine){
        position=Position(
            x=(-sensorX*.12f).coerceIn(-.12f,.12f),
            y=(.08f+sensorY*.05f).coerceIn(.02f,.14f),
            z=2.35f
        )
        lookAt(Position(0f,.08f,0f))
    }

    SceneView(
        modifier=modifier,
        engine=engine,
        modelLoader=modelLoader,
        cameraNode=cameraNode,
        cameraManipulator=null,
        isOpaque=false
    ){
        rememberModelInstance(modelLoader,assetPath)?.let{instance->
            ModelNode(
                modelInstance=instance,
                scaleToUnits=1.72f,
                autoAnimate=motionEnabled
            )
        }
    }
}

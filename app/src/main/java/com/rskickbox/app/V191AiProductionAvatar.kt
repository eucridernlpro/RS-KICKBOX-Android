package com.rskickbox.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
    store:RsStore,
    avatar:String,
    speaking:Boolean,
    speechAmplitude:Float,
    speechText:String="",
    motionEnabled:Boolean,
    listening:Boolean,
    thinking:Boolean,
    sensorX:Float,
    sensorY:Float,
    modifier:Modifier=Modifier
){
    val context=LocalContext.current
    val identity=remember(avatar){rsAiIdentityV193(avatar)}
    val assetPath=identity.modelAsset
    val hasRiggedAsset=remember(assetPath){
        runCatching{
            context.assets.open(assetPath).use{}
            true
        }.getOrDefault(false)
    }

    if(!hasRiggedAsset){
        val visualSlot=if(avatar=="FEMALE")"ai_trainer_female" else "ai_trainer_male"
        val customVisual=remember(visualSlot){
            store.s("visual_v21_"+visualSlot,"").takeIf{it.isNotBlank()}
        }
        if(!customVisual.isNullOrBlank()){
            RsUriPreviewV21(
                customVisual,
                modifier
                    .clip(RoundedCornerShape(28.dp))
                    .graphicsLayer{
                        val depth=if(motionEnabled)1f else 0f
                        translationX=(-sensorX*20f*depth).coerceIn(-22f,22f)
                        translationY=(sensorY*10f*depth).coerceIn(-12f,12f)
                        scaleX=1.035f+(if(speaking) speechAmplitude*.012f else 0f)
                        scaleY=scaleX
                    },
                store.s("visual_v21_pos_"+visualSlot,"CENTER")
            )
        }else{
            val transition=rememberInfiniteTransition(label="rs-digital-human")
            val breath by transition.animateFloat(
                initialValue=0f,
                targetValue=1f,
                animationSpec=infiniteRepeatable(
                    animation=tween(if(speaking)260 else 1900,easing=FastOutSlowInEasing),
                    repeatMode=RepeatMode.Reverse
                ),
                label="rs-digital-human-breath"
            )
            val drawable=if(avatar=="FEMALE")R.drawable.rs_ai_sofia_default else R.drawable.rs_ai_marcus_default
            Image(
                painter=painterResource(drawable),
                contentDescription=identity.displayName,
                contentScale=ContentScale.Crop,
                modifier=modifier
                    .clip(RoundedCornerShape(28.dp))
                    .graphicsLayer{
                        val depth=if(motionEnabled)1f else 0f
                        translationX=(-sensorX*18f*depth).coerceIn(-20f,20f)
                        translationY=(sensorY*9f*depth).coerceIn(-10f,10f)
                        val speechDrive=if(speaking)(speechAmplitude.coerceIn(.08f,1f)*.010f + breath*.006f) else breath*.003f
                        scaleX=1.04f+speechDrive
                        scaleY=1.04f+speechDrive
                    }
            )
        }
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
            val state=rsAiAvatarStateV193(
                speaking=speaking,
                listening=listening,
                thinking=thinking
            )
            ModelNode(
                modelInstance=instance,
                scaleToUnits=1.72f,
                autoAnimate=motionEnabled,
                animationName=if(motionEnabled)rsAiAnimationNameV193(identity,state) else null,
                animationLoop=true
            )
        }
    }
}

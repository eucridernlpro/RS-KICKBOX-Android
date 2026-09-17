package com.rskickbox.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RsCinematicIntroV21(c:RsPalette,store:RsStore,onFinished:()->Unit){
    var stage by remember{ mutableIntStateOf(0) }
    val enabled=store.b("intro_enabled",true)
    if(!enabled){LaunchedEffect(Unit){onFinished()};return}
    LaunchedEffect(Unit){
        delay(2200);stage=1
        delay(1200);stage=2
        delay(1600);onFinished()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)){
        AnimatedContent(stage,transitionSpec={fadeIn(tween(450)) togetherWith fadeOut(tween(450))},label="introStage"){s->
            when(s){
                0->RsIntroFighterSceneV21(c,store)
                1->RsIntroGlovesSceneV21(c,store)
                else->RsIntroLogoSceneV21(c,store)
            }
        }
        TextButton(onClick=onFinished,modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)){Text("Skip",color=Color.White.copy(alpha=.72f))}
    }
}

@Composable
private fun RsIntroFighterSceneV21(c:RsPalette,store:RsStore){
    val custom=store.s("visual_v21_intro_fighter","")
    val inf=rememberInfiniteTransition(label="fighter")
    val punch by inf.animateFloat(0f,1f,infiniteRepeatable(tween(620,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="punch")
    val smoke by inf.animateFloat(0f,1f,infiniteRepeatable(tween(2600,easing=LinearEasing),RepeatMode.Reverse),label="smoke")
    Box(Modifier.fillMaxSize()){
        if(custom.isNotBlank())RsUriPreviewV21(custom,Modifier.fillMaxSize(),store.s("visual_v21_pos_intro_fighter","CENTER"))
        Canvas(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF29180A),Color(0xFF090909),Color.Black),center=Offset.Unspecified,radius=1100f))){
            val w=size.width;val h=size.height
            repeat(7){i->drawCircle(Color.White.copy(alpha=.018f+.015f*smoke),w*(.10f+i*.025f),Offset(w*((i*.19f+smoke*.08f)%1f),h*(.16f+i*.11f)))}
            drawLine(c.bright.copy(alpha=.22f),Offset(0f,h*.12f),Offset(w*.68f,h*.60f),w*.035f)
            drawLine(Color(0xFFE25B3F).copy(alpha=.14f),Offset(w,h*.16f),Offset(w*.38f,h*.61f),w*.03f)
            val head=Offset(w*.47f,h*.29f)
            drawCircle(Color.Black.copy(alpha=.97f),w*.055f,head)
            drawLine(Color.Black,Offset(w*.47f,h*.35f),Offset(w*.45f,h*.59f),w*.052f)
            drawLine(Color.Black,Offset(w*.45f,h*.43f),Offset(w*(.62f+.16f*punch),h*(.33f-.03f*punch)),w*.038f)
            drawLine(Color.Black,Offset(w*.45f,h*.43f),Offset(w*.31f,h*.50f),w*.034f)
            drawLine(Color.Black,Offset(w*.45f,h*.58f),Offset(w*.29f,h*.82f),w*.045f)
            drawLine(Color.Black,Offset(w*.45f,h*.58f),Offset(w*(.66f+.12f*punch),h*(.72f-.12f*punch)),w*.046f)
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
    val inf=rememberInfiniteTransition(label="gloves")
    val sway by inf.animateFloat(-1f,1f,infiniteRepeatable(tween(1200,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="sway")
    Box(Modifier.fillMaxSize()){
        if(custom.isNotBlank())RsUriPreviewV21(custom,Modifier.fillMaxSize(),store.s("visual_v21_pos_intro_gloves","CENTER"))
        Canvas(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070707),Color.Black)))){
            val w=size.width;val h=size.height
            drawLine(c.bright.copy(alpha=.16f),Offset(w*.20f,0f),Offset(w*.55f,h),w*.018f)
            val rope1=w*(.43f+sway*.006f);val rope2=w*(.57f-sway*.006f)
            drawLine(Color(0xFF7B6038),Offset(rope1,0f),Offset(rope1,h*.39f),w*.010f)
            drawLine(Color(0xFF7B6038),Offset(rope2,0f),Offset(rope2,h*.42f),w*.010f)
            drawRoundRect(Color(0xFF17110C),Offset(rope1-w*.075f,h*.38f),androidx.compose.ui.geometry.Size(w*.14f,h*.20f),androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f))
            drawRoundRect(Color(0xFF17110C),Offset(rope2-w*.065f,h*.41f),androidx.compose.ui.geometry.Size(w*.14f,h*.20f),androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f))
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
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.gold.copy(alpha=.18f),Color.Black),radius=900f)),contentAlignment=Alignment.Center){
        if(customLogo.isNotBlank()){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                RsUriPreviewV21(customLogo,Modifier.size(180.dp),"CENTER")
                Text(store.s("brand_header_name","RS KICKBOX"),color=c.bright,fontWeight=FontWeight.Black,fontSize=26.sp)
            }
        }else{
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text("♛",color=c.bright.copy(alpha=glow),fontSize=78.sp,fontWeight=FontWeight.Black)
                Text("RS",color=c.bright,fontSize=92.sp,fontWeight=FontWeight.Black)
                Text("KICKBOX",color=Color.White.copy(alpha=.80f),fontSize=20.sp,fontWeight=FontWeight.Bold,letterSpacing=4.sp)
            }
        }
    }
}

@Composable
fun RsIntroSettingsV21(c:RsPalette,store:RsStore){
    var enabled by remember{mutableStateOf(store.b("intro_enabled",true))}
    var everyLaunch by remember{mutableStateOf(store.b("intro_every_launch",true))}
    RsScroll(c,"Intro & Splash Settings","Control the cinematic sequence shown before the login page."){
        RsPanel(c){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Switch(enabled,{enabled=it;store.pb("intro_enabled",it)});Spacer(Modifier.width(10.dp));Column{Text("Cinematic intro",color=c.bright,fontWeight=FontWeight.Bold);Text("Fighter → gloves → RS crown logo",color=c.muted)}}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Checkbox(everyLaunch,{everyLaunch=it;store.pb("intro_every_launch",it)});Text("Show on every fresh app launch",color=c.text)}
            Text("Default duration: about 5 seconds. A Skip control is always available so the intro never blocks access.",color=c.muted)
        }
    }
}

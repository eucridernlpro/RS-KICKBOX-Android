package com.rskickbox.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun RsAiPremiumRoomV207(
    c:RsPalette,
    avatar:String,
    stage:RsAiSceneStageV206,
    listening:Boolean,
    thinking:Boolean,
    modifier:Modifier=Modifier,
    content:@Composable BoxScope.()->Unit
){
    val transition=rememberInfiniteTransition(label="rs-ai-room")
    val pulse by transition.animateFloat(
        initialValue=.38f,
        targetValue=.92f,
        animationSpec=infiniteRepeatable(
            animation=tween(1600,easing=FastOutSlowInEasing),
            repeatMode=RepeatMode.Reverse
        ),
        label="room-pulse"
    )
    val scan by transition.animateFloat(
        initialValue=0f,
        targetValue=1f,
        animationSpec=infiniteRepeatable(
            animation=tween(4200,easing=LinearEasing),
            repeatMode=RepeatMode.Restart
        ),
        label="room-scan"
    )
    val cyan=Color(0xFF58C9FF)
    val gold=c.gold

    Box(
        modifier.clip(RoundedCornerShape(26.dp)).background(
            Brush.verticalGradient(
                listOf(Color(0xFF020506),Color(0xFF071014),Color.Black)
            )
        )
    ){
        Canvas(Modifier.fillMaxSize()){
            // Vertical premium studio light bars.
            val barY=size.height*.08f
            val barH=size.height*.52f
            listOf(.06f,.17f,.82f,.93f).forEachIndexed{i,xn->
                val x=size.width*xn
                drawRoundRect(
                    color=(if(i%2==0)gold else cyan).copy(alpha=.18f+.18f*pulse),
                    topLeft=Offset(x,barY),
                    size=Size(4f,barH),
                    cornerRadius=androidx.compose.ui.geometry.CornerRadius(3f,3f)
                )
            }

            // Perspective floor grid.
            val horizon=size.height*.62f
            repeat(8){i->
                val y=horizon+(size.height-horizon)*(i/7f)
                drawLine(
                    color=gold.copy(alpha=.08f+(i/7f)*.10f),
                    start=Offset(0f,y),
                    end=Offset(size.width,y),
                    strokeWidth=1f
                )
            }
            repeat(9){i->
                val bottomX=size.width*(i/8f)
                val topX=size.width/2f+(bottomX-size.width/2f)*.22f
                drawLine(
                    color=cyan.copy(alpha=.07f),
                    start=Offset(topX,horizon),
                    end=Offset(bottomX,size.height),
                    strokeWidth=1f
                )
            }

            // Circular stage under the persona.
            val stageCenter=Offset(size.width*.40f,size.height*.78f)
            repeat(3){i->
                drawOval(
                    color=if(i==0)gold.copy(alpha=.22f) else cyan.copy(alpha=.10f),
                    topLeft=Offset(stageCenter.x-size.width*(.23f+i*.025f),stageCenter.y-size.height*(.035f+i*.008f)),
                    size=Size(size.width*(.46f+i*.05f),size.height*(.07f+i*.016f)),
                    style=Stroke(width=2f)
                )
            }

            // Holographic scan line.
            val sy=size.height*(.14f+scan*.62f)
            drawLine(
                color=cyan.copy(alpha=.10f+.10f*pulse),
                start=Offset(size.width*.48f,sy),
                end=Offset(size.width*.96f,sy),
                strokeWidth=1.5f
            )
        }

        // Ambient vignette.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha=.16f),
                        Color.Transparent,
                        Color.Black.copy(alpha=.32f)
                    )
                )
            )
        )

        Surface(
            color=Color.Black.copy(alpha=.55f),
            shape=RoundedCornerShape(12.dp),
            border=androidx.compose.foundation.BorderStroke(
                1.dp,
                when{
                    listening->Color(0xFF55D58A)
                    thinking->gold
                    stage==RsAiSceneStageV206.LIVE_3D->cyan
                    else->c.gold.copy(alpha=.35f)
                }
            ),
            modifier=Modifier.align(Alignment.TopStart).padding(9.dp)
        ){
            Row(
                Modifier.padding(horizontal=8.dp,vertical=5.dp),
                horizontalArrangement=Arrangement.spacedBy(5.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Text(
                    if(avatar=="MALE")"MARCUS" else "SOFIA",
                    color=Color.White,
                    fontSize=7.sp,
                    fontWeight=FontWeight.Black
                )
                Text(
                    when{
                        listening->"● LISTENING"
                        thinking->"● THINKING"
                        stage==RsAiSceneStageV206.LIVE_3D->"● LIVE 3D"
                        else->"● READY"
                    },
                    color=when{
                        listening->Color(0xFF55D58A)
                        thinking->gold
                        else->cyan
                    },
                    fontSize=6.sp,
                    fontWeight=FontWeight.Black
                )
            }
        }

        content()
    }
}

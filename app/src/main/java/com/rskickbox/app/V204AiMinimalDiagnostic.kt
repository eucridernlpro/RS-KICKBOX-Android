package com.rskickbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Absolute-minimum AI route used to isolate the persistent device crash.
 * Intentionally contains no image decode, TTS, SpeechRecognizer, sensors,
 * Media3 controller, cloud request, launchers, or long-lived effects.
 */
@Composable
fun RsAiMinimalDiagnosticV204(
    c:RsPalette,
    store:RsStore,
    role:RsRole,
    onNavigate:(String)->Unit={}
){
    // Do not execute old pending speech during crash isolation.
    store.ps("ai_pending_spoken_v171","")
    store.pb("ai_start_listening_v168",false)
    store.pb("ai_direct_listen_v195",false)

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Black,Color(0xFF071018),Color.Black)
            )
        )
    ){
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center
        ){
            Surface(
                color=Color(0xFF0E171D),
                shape=RoundedCornerShape(26.dp),
                border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.45f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    Text(
                        "RS AI",
                        color=c.bright,
                        fontSize=24.sp,
                        fontWeight=FontWeight.Black
                    )
                    Text(
                        "DIAGNOSTIC SAFE MODE",
                        color=Color(0xFF55D58A),
                        fontSize=10.sp,
                        fontWeight=FontWeight.Black
                    )
                    Text(
                        "AI route opened successfully. Advanced AI components are temporarily disabled while the crash source is isolated.",
                        color=Color.White,
                        fontSize=12.sp
                    )
                    Button(
                        onClick={onNavigate(if(role==RsRole.TRAINER)"trainer" else "home")},
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text("RETURN TO DASHBOARD")
                    }
                }
            }
        }
    }
}

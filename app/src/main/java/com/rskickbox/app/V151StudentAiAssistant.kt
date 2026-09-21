package com.rskickbox.app

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsStudentAiAssistantSafeV151(
    c:RsPalette,
    lang:RsLang,
    store:RsStore
){
    var openTechnique by remember{mutableStateOf(false)}
    var question by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf("")}
    val context=LocalContext.current
    var voiceStatus by remember{mutableStateOf("")}
    val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            val spoken=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if(spoken.isNotBlank()){
                question=spoken.take(600)
                voiceStatus="Voice question ready."
            }
        }
    }
    fun startVoiceQuestion(){
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT,"Ask RS AI Trainer")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,java.util.Locale.getDefault().toLanguageTag())
        }
        runCatching{voiceLauncher.launch(intent)}
            .onFailure{voiceStatus="Voice input is not available on this device."}
    }

    if(openTechnique){
        Column(Modifier.fillMaxSize()){
            Row(
                Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=6.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                OutlinedButton(
                    onClick={openTechnique=false},
                    contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)
                ){Text("‹  AI Assistant")}
                Text(
                    "TECHNIQUE ANALYSIS",
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=11.sp
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)){
                RsTechniqueCoachV27(c,lang,store,RsRole.STUDENT)
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal=8.dp,vertical=6.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        Surface(
            color=Color.Black.copy(alpha=.70f),
            shape=RoundedCornerShape(28.dp),
            border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.38f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(
                Modifier.padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Row(
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    Surface(
                        shape=CircleShape,
                        color=Color(0xFF58C9FF).copy(alpha=.10f),
                        border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.38f)),
                        modifier=Modifier.size(58.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            Text("✧",color=Color(0xFF58C9FF),fontSize=24.sp,fontWeight=FontWeight.Black)
                        }
                    }
                    Column(Modifier.weight(1f)){
                        Text("RS AI TRAINER",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                        Text("Sofia · Marcus · kickboxing coaching",color=Color(0xFF58C9FF),fontSize=9.sp,fontWeight=FontWeight.Bold)
                    }
                    Text("AI",color=Color(0xFF58C9FF),fontWeight=FontWeight.Black,fontSize=10.sp)
                }
                Text(
                    "Ask a coaching question or open Technique Analysis when you want to review a movement or video.",
                    color=c.muted,
                    fontSize=10.sp,
                    lineHeight=14.sp
                )
            }
        }

        Surface(
            color=Color.Black.copy(alpha=.62f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(
                Modifier.padding(14.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp)
            ){
                Text("ASK YOUR COACH",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=1.sp)
                OutlinedTextField(
                    value=question,
                    onValueChange={question=it.take(600)},
                    label={Text("Kickboxing question")},
                    minLines=3,
                    modifier=Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(
                        onClick={startVoiceQuestion()},
                        modifier=Modifier.weight(1f)
                    ){Text("🎙  Ask with voice")}
                    OutlinedButton(
                        onClick={openTechnique=true},
                        modifier=Modifier.weight(1f)
                    ){Text("🎥  Upload video")}
                }
                if(voiceStatus.isNotBlank())Text(voiceStatus,color=Color(0xFF58C9FF),fontSize=9.sp)
                Button(
                    onClick={
                        answer=when{
                            question.contains("kick",true)->
                                "Keep your base stable, rotate through the hip, keep the opposite hand high, and return immediately to stance after the kick."
                            question.contains("jab",true)||question.contains("cross",true)->
                                "Stay relaxed through the shoulders, rotate from the floor and hip, keep your guard compact, and recover the hand straight back to your face."
                            question.contains("guard",true)->
                                "Keep the chin protected, elbows controlled, hands returning to position after every strike, and avoid letting your stance become too narrow."
                            else->
                                "Focus first on balance, guard, controlled rotation, distance and a clean recovery to stance. Use Technique Analysis for a more specific movement review."
                        }
                    },
                    enabled=question.isNotBlank(),
                    modifier=Modifier.fillMaxWidth()
                ){Text("Ask RS AI Trainer")}
                if(answer.isNotBlank()){
                    Surface(
                        color=Color(0xFF58C9FF).copy(alpha=.07f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.22f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(answer,color=c.text,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(12.dp))
                    }
                }
            }
        }

        Surface(
            color=Color.Black.copy(alpha=.62f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.28f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Box(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(c.gold.copy(alpha=.08f),Color.Transparent,Color(0xFF58C9FF).copy(alpha=.06f))
                        )
                    )
                    .padding(14.dp)
            ){
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Text("TECHNIQUE ANALYSIS",color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp)
                    Text(
                        "Upload a short technique video, review correction points and use trainer-approved reference media.",
                        color=c.muted,
                        fontSize=10.sp,
                        lineHeight=14.sp
                    )
                    Button(
                        onClick={openTechnique=true},
                        modifier=Modifier.fillMaxWidth()
                    ){Text("Open Technique Analysis")}
                }
            }
        }
    }
}

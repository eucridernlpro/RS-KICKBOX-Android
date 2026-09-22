package com.rskickbox.app

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
    val languages=remember{
        listOf(
            "en" to ("English" to "en-US"),
            "nl" to ("Nederlands" to "nl-NL"),
            "pt" to ("Português" to "pt-PT"),
            "es" to ("Español" to "es-ES"),
            "fr" to ("Français" to "fr-FR"),
            "de" to ("Deutsch" to "de-DE"),
            "it" to ("Italiano" to "it-IT"),
            "pl" to ("Polski" to "pl-PL"),
            "tr" to ("Türkçe" to "tr-TR")
        )
    }
    var avatar by remember{
        mutableStateOf(store.s("ai_avatar_gender_v161","FEMALE").ifBlank{"FEMALE"})
    }
    var aiLanguage by remember{
        mutableStateOf(
            store.s("ai_voice_language_v161",lang.code)
                .takeIf{saved->languages.any{it.first==saved}}
                ?: "en"
        )
    }
    val selectedLanguage=languages.firstOrNull{it.first==aiLanguage}?:languages.first()
    var ttsReady by remember{mutableStateOf(false)}
    val tts=remember{
        TextToSpeech(context){status->
            ttsReady=status==TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(tts){
        onDispose{runCatching{tts.stop()};runCatching{tts.shutdown()}}
    }
    LaunchedEffect(aiLanguage,ttsReady){
        if(ttsReady){
            val locale=java.util.Locale.forLanguageTag(selectedLanguage.second.second)
            tts.language=locale
        }
    }
    fun speakAnswer(){
        if(answer.isBlank()||!ttsReady)return
        val locale=java.util.Locale.forLanguageTag(selectedLanguage.second.second)
        tts.language=locale
        tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"rs_ai_answer")
    }
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,selectedLanguage.second.second)
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
                Modifier.padding(14.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ){
                val avatarSlot=if(avatar=="FEMALE")"ai_trainer_female" else "ai_trainer_male"
                val avatarName=if(avatar=="FEMALE")"SOFIA" else "MARCUS"
                val avatarVisual=rsVisualUriWithBundledFallbackV113(context,store,avatarSlot)

                Box(
                    Modifier.fillMaxWidth().height(220.dp)
                        .background(Color.Black,RoundedCornerShape(22.dp))
                ){
                    if(avatarVisual.isNotBlank()){
                        RsUriPreviewV21(avatarVisual,Modifier.fillMaxSize(),store.s("visual_v21_pos_"+avatarSlot,"CENTER"))
                    }else{
                        Box(Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xFF091016),Color.Black))
                        ))
                    }
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=.18f),Color.Black.copy(alpha=.82f)))
                    ))
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(12.dp)
                    ){
                        Text(avatarName,color=Color.White,fontWeight=FontWeight.Black,fontSize=20.sp,letterSpacing=1.sp)
                        Text("RS AI TRAINER · "+selectedLanguage.second.first,color=Color(0xFF58C9FF),fontSize=9.sp,fontWeight=FontWeight.Bold)
                    }
                }

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(
                        onClick={
                            avatar="FEMALE"
                            store.ps("ai_avatar_gender_v161",avatar)
                        },
                        modifier=Modifier.weight(1f),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=if(avatar=="FEMALE")c.bright else c.panel,
                            contentColor=if(avatar=="FEMALE")Color.Black else c.text
                        )
                    ){Text("Sofia",fontWeight=FontWeight.Black,fontSize=9.sp)}
                    OutlinedButton(
                        onClick={
                            avatar="MALE"
                            store.ps("ai_avatar_gender_v161",avatar)
                        },
                        modifier=Modifier.weight(1f)
                    ){Text("Marcus",fontWeight=FontWeight.Black,fontSize=9.sp)}
                }

                Text("VOICE LANGUAGE",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    languages.forEach{item->
                        val code=item.first
                        val label=item.second.first
                        FilterChip(
                            selected=aiLanguage==code,
                            onClick={
                                aiLanguage=code
                                store.ps("ai_voice_language_v161",code)
                            },
                            label={Text(label,fontSize=8.sp)}
                        )
                    }
                }

                Text(
                    "Ask by text or voice, analyze technique video, and hear the coach answer in the selected language.",
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
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Text(answer,color=c.text,fontSize=12.sp,lineHeight=18.sp)
                            OutlinedButton(
                                onClick={speakAnswer()},
                                enabled=ttsReady,
                                modifier=Modifier.fillMaxWidth()
                            ){Text("🔊  Speak answer · "+selectedLanguage.second.first,fontSize=9.sp)}
                        }
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

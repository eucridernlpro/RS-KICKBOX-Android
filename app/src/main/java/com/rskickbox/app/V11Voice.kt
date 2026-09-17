package com.rskickbox.app

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsVoiceCoach(c:RsPalette,lang:RsLang,s:RsStore){
    val context=LocalContext.current
    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ready by remember{mutableStateOf(false)}
    var question by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf("")}
    var autoSpeak by remember{mutableStateOf(s.b("voice_auto",true))}

    DisposableEffect(Unit){
        val engine=TextToSpeech(context){status->ready=status==TextToSpeech.SUCCESS}
        tts=engine
        onDispose{engine.stop();engine.shutdown()}
    }
    LaunchedEffect(lang.code,ready){if(ready)tts?.language=lang.locale}

    fun coachReply():String=when(lang.code){
        "nl"->"Werk vanuit balans, houd je dekking hoog en voer de techniek eerst langzaam en correct uit voordat je versnelt."
        "pt"->"Trabalha com equilíbrio, mantém a guarda alta e executa primeiro a técnica devagar e corretamente antes de aumentar a velocidade."
        "es"->"Trabaja con equilibrio, mantén la guardia alta y ejecuta primero la técnica despacio y correctamente antes de aumentar la velocidad."
        "fr"->"Travaille en équilibre, garde les mains hautes et exécute d'abord la technique lentement et correctement avant d'accélérer."
        "de"->"Arbeite aus einer stabilen Balance, halte die Deckung hoch und führe die Technik zuerst langsam und sauber aus, bevor du schneller wirst."
        "it"->"Lavora in equilibrio, tieni alta la guardia ed esegui prima la tecnica lentamente e correttamente prima di aumentare la velocità."
        "pl"->"Pracuj w równowadze, trzymaj gardę wysoko i najpierw wykonuj technikę wolno i poprawnie, zanim zwiększysz tempo."
        "tr"->"Dengeni koru, gardını yukarıda tut ve hızlanmadan önce tekniği yavaş ve doğru şekilde uygula."
        else->"Work from balance, keep your guard high, and perform the technique slowly and correctly before increasing speed."
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("AI Voice Coach",color=c.bright,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
        Text("Ask about a move or training. The coach speaks the answer in the selected app language when that device voice is installed.",color=c.muted)
        RsPanel(c){
            Text("VOICE LANGUAGE: ${lang.name}",color=c.bright,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Speak answers automatically",color=c.text);Switch(autoSpeak,{autoSpeak=it;s.pb("voice_auto",it)})}
            Text(if(ready)"Voice engine ready" else "Loading device voice engine…",color=c.muted,fontSize=11.sp)
        }
        RsPanel(c){
            OutlinedTextField(question,{question=it},label={Text("Ask the coach")},modifier=Modifier.fillMaxWidth(),minLines=2)
            Button(onClick={answer=coachReply();if(autoSpeak&&ready){tts?.language=lang.locale;tts?.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"rs-coach")}},enabled=question.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Ask AI Coach")}
        }
        if(answer.isNotBlank())RsPanel(c){
            Text("COACH",color=c.bright,fontWeight=FontWeight.Bold);Text(answer,color=c.text)
            Button(onClick={if(ready){{tts?.language=lang.locale;tts?.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"rs-repeat")}}else{{}}},enabled=ready,modifier=Modifier.fillMaxWidth()){Text("🔊 Speak answer")}
        }
        Spacer(Modifier.height(20.dp))
    }
}

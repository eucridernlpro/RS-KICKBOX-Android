package com.rskickbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.launch

private data class RsSafeAiMessageV203(
    val mine:Boolean,
    val text:String
)

@Composable
fun RsAiSafeShellV203(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    role:RsRole,
    onNavigate:(String)->Unit={}
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    SideEffect{
        store.ps("rs_ai_last_checkpoint_v205","TEXT_SHELL_COMPOSED")
        store.ps("rs_ai_last_checkpoint_ms_v205",System.currentTimeMillis().toString())
    }
    var avatar by remember{mutableStateOf(store.s("ai_avatar_gender_v161","FEMALE"))}
    var aiLangCode by remember{mutableStateOf(store.s("ai_voice_language_v161",lang.code))}
    val aiLang=rsLangs.firstOrNull{it.code==aiLangCode}?:lang
    var draft by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var messages by remember{mutableStateOf<List<RsSafeAiMessageV203>>(emptyList())}
    val scroll=rememberScrollState()

    fun send(text:String){
        val body=text.trim()
        if(body.isBlank()||busy)return
        messages=messages+RsSafeAiMessageV203(true,body)
        draft=""
        busy=true
        status=""
        scope.launch{
            store.ps("rs_ai_last_checkpoint_v205","AI_REQUEST_START")
            store.ps("rs_ai_last_checkpoint_ms_v205",System.currentTimeMillis().toString())
            val route=store.s("session_last_route",if(role==RsRole.TRAINER)"trainer" else "home")
            val contextSummary=(
                "CURRENT RS ROLE: "+role.name+
                "\nCURRENT RS PAGE: "+route+
                "\n\nRS APP FEATURES:\n"+rsAiAppKnowledgeSummaryV175(aiLang,role).take(3000)
            )
            val local=rsAiLocalCoachAnswerV164(aiLang.code,body)
            val answer=runCatching{
                rsOnlineAiCoachV164(body,aiLang,contextSummary).getOrElse{local}
            }.getOrElse{local}
            messages=messages+RsSafeAiMessageV203(false,answer)
            store.ps("rs_ai_last_checkpoint_v205","AI_REQUEST_DONE")
            store.ps("rs_ai_last_checkpoint_ms_v205",System.currentTimeMillis().toString())
            busy=false
            status=""
        }
    }

    LaunchedEffect(Unit){
        val pending=store.s("ai_pending_spoken_v171","").trim()
        if(pending.isNotBlank()){
            store.ps("ai_pending_spoken_v171","")
            kotlinx.coroutines.delay(250)
            send(pending)
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Black,Color(0xFF071018),Color.Black)
            )
        )
    ){
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(6.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                FilterChip(
                    selected=avatar=="FEMALE",
                    onClick={
                        avatar="FEMALE"
                        store.ps("ai_avatar_gender_v161","FEMALE")
                    },
                    label={Text("SOFIA",fontSize=8.sp)}
                )
                FilterChip(
                    selected=avatar=="MALE",
                    onClick={
                        avatar="MALE"
                        store.ps("ai_avatar_gender_v161","MALE")
                    },
                    label={Text("MARCUS",fontSize=8.sp)}
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "AI SAFE MODE",
                    color=Color(0xFF55D58A),
                    fontSize=8.sp,
                    fontWeight=FontWeight.Black
                )
            }

            Surface(
                shape=RoundedCornerShape(22.dp),
                color=Color.Black,
                border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                modifier=Modifier.fillMaxWidth().height(150.dp)
            ){
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            listOf(
                                if(avatar=="MALE")Color(0xFF17314A) else Color(0xFF3A1D2E),
                                Color(0xFF0B1117),
                                Color.Black
                            )
                        )
                    ),
                    contentAlignment=Alignment.Center
                ){
                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                        Text(
                            if(avatar=="MALE")"MARCUS" else "SOFIA",
                            color=Color.White,
                            fontSize=26.sp,
                            fontWeight=FontWeight.Black
                        )
                        Text(
                            "RS AI · TEXT SAFE LAYER",
                            color=c.bright,
                            fontSize=9.sp,
                            fontWeight=FontWeight.Black
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(5.dp)
            ){
                rsLangs.forEach{l->
                    FilterChip(
                        selected=aiLangCode==l.code,
                        onClick={
                            aiLangCode=l.code
                            store.ps("ai_voice_language_v161",l.code)
                        },
                        label={Text(l.code.uppercase(),fontSize=7.sp)}
                    )
                }
            }

            Surface(
                color=Color.Black.copy(alpha=.72f),
                shape=RoundedCornerShape(18.dp),
                modifier=Modifier.weight(1f).fillMaxWidth()
            ){
                Column(
                    Modifier.fillMaxSize().verticalScroll(scroll).padding(10.dp),
                    verticalArrangement=Arrangement.spacedBy(7.dp)
                ){
                    if(messages.isEmpty()){
                        Text(
                            when(aiLang.code){
                                "nl"->"Praat met Sofia of Marcus. Deze stabiele modus houdt de geavanceerde 3D/TTS-laag tijdelijk uitgeschakeld terwijl we de crash isoleren."
                                "pt"->"Fala com a Sofia ou o Marcus. Este modo estável mantém temporariamente a camada avançada 3D/TTS desligada enquanto isolamos o crash."
                                "es"->"Habla con Sofia o Marcus. Este modo estable mantiene temporalmente desactivada la capa 3D/TTS avanzada mientras aislamos el fallo."
                                "fr"->"Parle avec Sofia ou Marcus. Ce mode stable désactive temporairement la couche 3D/TTS avancée pendant que nous isolons le crash."
                                else->"Talk with Sofia or Marcus by text. Voice, image rendering, sensors and 3D stay disabled in this isolation layer."
                            },
                            color=c.muted,
                            fontSize=10.sp
                        )
                    }
                    messages.forEach{m->
                        Surface(
                            color=if(m.mine)c.gold.copy(alpha=.16f) else Color(0xFF10202B),
                            shape=RoundedCornerShape(14.dp),
                            modifier=Modifier.fillMaxWidth(if(m.mine).88f else .94f)
                                .align(if(m.mine)Alignment.End else Alignment.Start)
                        ){
                            Text(
                                m.text,
                                color=Color.White,
                                fontSize=11.sp,
                                modifier=Modifier.padding(10.dp)
                            )
                        }
                    }
                    if(busy)CircularProgressIndicator(modifier=Modifier.size(22.dp))
                }
            }

            if(status.isNotBlank()){
                Text(status,color=c.muted,fontSize=9.sp)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(6.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                OutlinedTextField(
                    value=draft,
                    onValueChange={draft=it.take(1800)},
                    placeholder={Text("Message "+if(avatar=="MALE")"Marcus" else "Sofia")},
                    maxLines=3,
                    modifier=Modifier.weight(1f)
                )
                AssistChip(
                    onClick={},
                    enabled=false,
                    label={Text("VOICE OFF",fontSize=7.sp)}
                )
                FilledIconButton(
                    onClick={send(draft)},
                    enabled=draft.isNotBlank()&&!busy,
                    modifier=Modifier.size(44.dp)
                ){Text("➤")}
            }

            TextButton(
                onClick={onNavigate(if(role==RsRole.TRAINER)"trainer" else "home")},
                modifier=Modifier.align(Alignment.CenterHorizontally)
            ){Text("CLOSE AI",fontSize=8.sp)}
        }
    }
}

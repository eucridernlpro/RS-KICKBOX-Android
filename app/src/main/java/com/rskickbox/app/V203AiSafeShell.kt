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


private data class RsAiHoloCopyV206(
    val heading:String,
    val features:List<String>,
    val footer:String
)

private fun rsAiHoloCopyV206(code:String):RsAiHoloCopyV206=when(code){
    "nl"->RsAiHoloCopyV206(
        "WAT IK VOOR JE KAN DOEN",
        listOf("Natuurlijk praten","Training begeleiden","Muziek bedienen","Door de app navigeren","Helpen met instellingen"),
        "MEER DAN TECHNOLOGIE · JOUW EVOLUTIE"
    )
    "pt"->RsAiHoloCopyV206(
        "O QUE POSSO FAZER POR TI",
        listOf("Conversa natural","Orienta treinos","Controla música","Navega no app","Ajuda nas definições"),
        "MAIS QUE TECNOLOGIA · A TUA EVOLUÇÃO"
    )
    "es"->RsAiHoloCopyV206(
        "LO QUE PUEDO HACER POR TI",
        listOf("Conversación natural","Guía entrenamientos","Controla música","Navega por la app","Ayuda con ajustes"),
        "MÁS QUE TECNOLOGÍA · TU EVOLUCIÓN"
    )
    "fr"->RsAiHoloCopyV206(
        "CE QUE JE PEUX FAIRE",
        listOf("Conversation naturelle","Guide les entraînements","Contrôle la musique","Navigue dans l’app","Aide aux réglages"),
        "PLUS QUE LA TECHNOLOGIE · TON ÉVOLUTION"
    )
    "de"->RsAiHoloCopyV206(
        "WAS ICH FÜR DICH TUN KANN",
        listOf("Natürlich sprechen","Training begleiten","Musik steuern","In der App navigieren","Bei Einstellungen helfen"),
        "MEHR ALS TECHNOLOGIE · DEINE ENTWICKLUNG"
    )
    "it"->RsAiHoloCopyV206(
        "COSA POSSO FARE PER TE",
        listOf("Conversazione naturale","Guida gli allenamenti","Controlla la musica","Naviga nell’app","Aiuta nelle impostazioni"),
        "PIÙ DELLA TECNOLOGIA · LA TUA EVOLUZIONE"
    )
    "pl"->RsAiHoloCopyV206(
        "W CZYM MOGĘ POMÓC",
        listOf("Naturalna rozmowa","Prowadzi treningi","Steruje muzyką","Nawiguje po aplikacji","Pomaga w ustawieniach"),
        "WIĘCEJ NIŻ TECHNOLOGIA · TWÓJ ROZWÓJ"
    )
    "tr"->RsAiHoloCopyV206(
        "SENİN İÇİN NELER YAPABİLİRİM",
        listOf("Doğal konuşma","Antrenmanı yönlendirir","Müziği kontrol eder","Uygulamada gezinir","Ayarlara yardımcı olur"),
        "TEKNOLOJİDEN DAHA FAZLASI · GELİŞİMİN"
    )
    else->RsAiHoloCopyV206(
        "WHAT I CAN DO FOR YOU",
        listOf("Natural conversation","Guide training","Control music","Navigate the app","Help with settings"),
        "MORE THAN TECHNOLOGY · YOUR EVOLUTION"
    )
}

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
    val holo=remember(aiLangCode){rsAiHoloCopyV206(aiLangCode)}
    var draft by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var messages by remember{mutableStateOf<List<RsSafeAiMessageV203>>(emptyList())}
    var autoSpeak by remember{mutableStateOf(store.b("ai_safe_autospeak_v206",true))}
    var sceneStage by remember{mutableStateOf(RsAiSceneStageV206.REFERENCE)}
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
            if(autoSpeak && store.b("rs_ai_listening_enabled_v200",true)){
                store.ps("ai_avatar_gender_v161",avatar)
                store.ps("ai_voice_language_v161",aiLang.code)
                runCatching{RsVoiceWakeServiceV165.handoffSpeechAndListen(context,answer)}
            }
            store.ps("rs_ai_last_checkpoint_v205","AI_REQUEST_DONE")
            store.ps("rs_ai_last_checkpoint_ms_v205",System.currentTimeMillis().toString())
            busy=false
            status=""
        }
    }

    LaunchedEffect(Unit){
        // Pending speech may arrive after the AI route is already visible
        // (top-mic / Voice Wake handoff). Keep a lightweight watcher instead
        // of checking only once at composition time.
        while(true){
            val pending=store.s("ai_pending_spoken_v171","").trim()
            if(pending.isNotBlank() && !busy){
                store.ps("ai_pending_spoken_v171","")
                kotlinx.coroutines.delay(120)
                send(pending)
            }
            kotlinx.coroutines.delay(220)
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
                Surface(
                    color=Color(0xFF071B16),
                    shape=RoundedCornerShape(20.dp),
                    border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF55D58A).copy(alpha=.45f))
                ){
                    Text(
                        "STABLE AI CORE",
                        color=Color(0xFF55D58A),
                        fontSize=7.sp,
                        fontWeight=FontWeight.Black,
                        modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp)
                    )
                }
            }

            Surface(
                shape=RoundedCornerShape(24.dp),
                color=Color.Black,
                border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
                modifier=Modifier.fillMaxWidth().height(205.dp)
            ){
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(
                                if(avatar=="MALE")Color(0xFF122434) else Color(0xFF2B1724),
                                Color(0xFF081117),
                                Color.Black
                            )
                        )
                    )
                ){
                    Row(
                        Modifier.fillMaxSize().padding(14.dp),
                        horizontalArrangement=Arrangement.spacedBy(12.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Box(Modifier.weight(.42f).fillMaxHeight()){
                            RsAiSceneAvatarV206(
                                store=store,
                                avatar=avatar,
                                speaking=false,
                                listening=store.s("rs_voice_wake_status_v168","").contains("LISTEN"),
                                thinking=busy,
                                motionEnabled=true,
                                modifier=Modifier.fillMaxSize(),
                                onStage={sceneStage=it}
                            )
                            Surface(
                                color=Color.Black.copy(alpha=.58f),
                                shape=RoundedCornerShape(12.dp),
                                modifier=Modifier.align(Alignment.BottomCenter).padding(7.dp)
                            ){
                                Text(
                                    when(sceneStage){
                                        RsAiSceneStageV206.LIVE_3D->"LIVE 3D"
                                        RsAiSceneStageV206.LOADING_3D->"LOADING 3D"
                                        RsAiSceneStageV206.CIRCUIT_BREAKER->"3D SAFE FALLBACK"
                                        else->"CINEMATIC FALLBACK"
                                    },
                                    color=if(sceneStage==RsAiSceneStageV206.LIVE_3D)Color(0xFF55D58A) else c.bright,
                                    fontSize=6.sp,
                                    fontWeight=FontWeight.Black,
                                    modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp)
                                )
                            }
                        }

                        Surface(
                            color=Color(0xFF06151E).copy(alpha=.82f),
                            shape=RoundedCornerShape(18.dp),
                            border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.42f)),
                            modifier=Modifier.weight(.58f).fillMaxHeight()
                        ){
                            Column(
                                Modifier.fillMaxSize().padding(10.dp),
                                verticalArrangement=Arrangement.spacedBy(5.dp)
                            ){
                                Text(
                                    holo.heading,
                                    color=Color(0xFF8EDCFF),
                                    fontSize=8.sp,
                                    fontWeight=FontWeight.Black,
                                    letterSpacing=.8.sp
                                )
                                holo.features.forEachIndexed{i,item->
                                    Row(
                                        verticalAlignment=Alignment.CenterVertically,
                                        horizontalArrangement=Arrangement.spacedBy(6.dp)
                                    ){
                                        Text(
                                            when(i){
                                                0->"◉"
                                                1->"◇"
                                                2->"♫"
                                                3->"▦"
                                                else->"⚙"
                                            },
                                            color=if(i%2==0)Color(0xFF58C9FF) else c.gold,
                                            fontSize=9.sp
                                        )
                                        Text(
                                            item,
                                            color=Color.White.copy(alpha=.94f),
                                            fontSize=8.sp,
                                            lineHeight=10.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    holo.footer,
                                    color=c.gold,
                                    fontSize=6.sp,
                                    fontWeight=FontWeight.Black,
                                    letterSpacing=.7.sp
                                )
                            }
                        }
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
                                "nl"->"Praat met "+if(avatar=="MALE")"Marcus" else "Sofia"+". De stabiele AI-kern is actief terwijl de geavanceerde realtime lagen gecontroleerd terugkeren."
                                "pt"->"Fala com "+if(avatar=="MALE")"o Marcus" else "a Sofia"+". O núcleo estável de IA está ativo enquanto as camadas avançadas regressam de forma controlada."
                                "es"->"Habla con "+if(avatar=="MALE")"Marcus" else "Sofia"+". El núcleo estable de IA está activo mientras recuperamos las capas avanzadas de forma controlada."
                                "fr"->"Parle avec "+if(avatar=="MALE")"Marcus" else "Sofia"+". Le noyau IA stable est actif pendant que les couches avancées reviennent de manière contrôlée."
                                else->"Talk with "+if(avatar=="MALE")"Marcus" else "Sofia"+". The stable AI core is active while advanced realtime layers are restored in a controlled sequence."
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
                    onClick={
                        if(store.b("rs_ai_listening_enabled_v200",true)){
                            store.ps("ai_avatar_gender_v161",avatar)
                            store.ps("ai_voice_language_v161",aiLang.code)
                            runCatching{RsVoiceWakeServiceV165.directListenOnce(context)}
                            status=when(aiLang.code){
                                "nl"->"Ik luister."
                                "pt"->"Estou a ouvir."
                                "es"->"Estoy escuchando."
                                "fr"->"J’écoute."
                                "de"->"Ich höre zu."
                                else->"Listening."
                            }
                        }else{
                            status="AI listening is off. Enable MASTER AI LISTENING in settings."
                        }
                    },
                    label={Text("🎙",fontSize=10.sp)}
                )
                FilterChip(
                    selected=autoSpeak,
                    onClick={
                        autoSpeak=!autoSpeak
                        store.pb("ai_safe_autospeak_v206",autoSpeak)
                    },
                    label={Text(if(autoSpeak)"VOICE ON" else "VOICE OFF",fontSize=6.sp)}
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

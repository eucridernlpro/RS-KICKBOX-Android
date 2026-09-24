package com.rskickbox.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private data class RsAiChatMessageV163(
    val id:Long,
    val mine:Boolean,
    val text:String,
    val mediaUri:String?=null,
    val mediaKind:String?=null,
    val references:List<RsAiCoachReferenceV164> = emptyList()
)

private fun rsLoadAiChatHistoryV174(store:RsStore,key:String):List<RsAiChatMessageV163>{
    val raw=store.s(key,"")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.optJSONObject(i)?:continue
                add(
                    RsAiChatMessageV163(
                        id=o.optLong("id",System.currentTimeMillis()+i),
                        mine=o.optBoolean("mine",false),
                        text=o.optString("text"),
                        mediaUri=o.optString("mediaUri").takeIf{it.isNotBlank()},
                        mediaKind=o.optString("mediaKind").takeIf{it.isNotBlank()}
                    )
                )
            }
        }.takeLast(100)
    }.getOrDefault(emptyList())
}

private fun rsPruneAiChatV176(
    input:List<RsAiChatMessageV163>,
    now:Long=System.currentTimeMillis()
):List<RsAiChatMessageV163>{
    // Guide/media examples are temporary: keep them for at most 90 minutes.
    var items=input.sortedBy{it.id}.filterNot{message->
        val visual=message.mediaUri!=null || message.references.isNotEmpty()
        visual && now-message.id>90L*60L*1000L
    }.toMutableList()

    fun visualCount()=items.count{it.mediaUri!=null || it.references.isNotEmpty()}
    fun textWeight()=items.sumOf{it.text.length}

    // If the conversation becomes heavy, remove oldest content first.
    while(
        items.size>100 ||
        visualCount()>8 ||
        textWeight()>30_000
    ){
        if(items.isEmpty())break
        items.removeAt(0)
    }
    return items
}

private fun rsSaveAiChatHistoryV174(store:RsStore,key:String,messages:List<RsAiChatMessageV163>){
    val arr=JSONArray()
    messages.takeLast(100).forEach{message->
        arr.put(JSONObject().apply{
            put("id",message.id)
            put("mine",message.mine)
            put("text",message.text)
            put("mediaUri",message.mediaUri?:"")
            put("mediaKind",message.mediaKind?:"")
        })
    }
    store.ps(key,arr.toString())
}

@Composable
fun RsAiAssistantChatV163(
    c:RsPalette,
    lang:RsLang,
    store:RsStore,
    role:RsRole,
    onNavigate:(String)->Unit={}
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val languages=rsLangs
    var aiLang by remember{
        mutableStateOf(
            store.s("ai_voice_language_v161",lang.code)
                .takeIf{saved->languages.any{it.code==saved}}
                ?:lang.code
        )
    }
    val selectedLang=languages.firstOrNull{it.code==aiLang}?:lang
    var avatar by remember{mutableStateOf(store.s("ai_avatar_gender_v161","FEMALE"))}
    val aiHistoryKey=remember(role){"ai_chat_history_v174_"+role.name.lowercase()}
    var messages by remember(aiHistoryKey){mutableStateOf(rsLoadAiChatHistoryV174(store,aiHistoryKey))}
    val aiListState=rememberLazyListState()
    var confirmClearAiChat by remember{mutableStateOf(false)}
    var draft by remember{mutableStateOf("")}
    var pickedUri by remember{mutableStateOf<Uri?>(null)}
    var pickedKind by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var speaking by remember{mutableStateOf(false)}
    var attachMenu by remember{mutableStateOf(false)}
    var optionsMenu by remember{mutableStateOf(false)}
    var voiceProfileMenu by remember{mutableStateOf(false)}
    var trainerReferences by remember{mutableStateOf(false)}
    var autoSpeak by remember{mutableStateOf(store.b("ai_auto_speak_v163",true))}
    var avatarMotionEnabled by remember{mutableStateOf(store.b("ai_avatar_motion_v163",true))}
    var sensorParallaxEnabled by remember{mutableStateOf(store.b("ai_sensor_parallax_v180",true))}
    var avatarRenderMode by remember{mutableStateOf(store.s("ai_avatar_render_mode_v176","CINEMATIC_3D"))}
    var status by remember{mutableStateOf("")}
    var aiMessageMenuId by remember{mutableStateOf<Long?>(null)}
    val musicController=rememberRsMusicControllerV90()
    var pendingMusic by remember{mutableStateOf<RsPersistentTrackV90?>(null)}
    var musicDialog by remember{mutableStateOf(false)}
    var newPlaylistName by remember{mutableStateOf("")}
    var playlistMenu by remember{mutableStateOf(false)}
    var voiceConversationActive by remember{mutableStateOf(false)}
    var voiceResult by remember{mutableStateOf<String?>(null)}
    var resumeListeningSignal by remember{mutableIntStateOf(0)}
    var immersiveGreetingPending by remember{
        mutableStateOf(store.b("ai_start_listening_v168",false))
    }

    LaunchedEffect(messages,aiHistoryKey){
        val pruned=rsPruneAiChatV176(messages)
        if(pruned!=messages){
            messages=pruned
            return@LaunchedEffect
        }
        rsSaveAiChatHistoryV174(store,aiHistoryKey,pruned)
        if(pruned.isNotEmpty()){
            kotlinx.coroutines.delay(40)
            runCatching{aiListState.animateScrollToItem(pruned.lastIndex)}
        }
    }

    LaunchedEffect(aiHistoryKey){
        while(true){
            kotlinx.coroutines.delay(10*60*1000L)
            val pruned=rsPruneAiChatV176(messages)
            if(pruned!=messages)messages=pruned
        }
    }

    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ttsReady by remember{mutableStateOf(false)}
    var activeUtteranceId by remember{mutableStateOf("")}
    val voiceOverrideKey=remember(aiLang,avatar){
        "ai_voice_override_v181_"+aiLang+"_"+avatar.lowercase(Locale.ROOT)
    }
    val voiceOverrideName=store.s(voiceOverrideKey,"")
    val selectableVoices=remember(ttsReady,aiLang,avatar,tts){
        val target=selectedLang.locale
        val all=tts?.voices?.filter{it.locale.language.equals(target.language,true)}.orEmpty()
        val exact=all.filter{
            target.country.isNotBlank() && it.locale.country.equals(target.country,true)
        }
        (if(exact.isNotEmpty())exact else all)
            .filterNot{it.features.contains("notInstalled")}
            .sortedWith(
                compareBy<android.speech.tts.Voice>{it.isNetworkConnectionRequired}
                    .thenByDescending{it.quality}
                    .thenBy{it.name}
            )
            .take(16)
    }
    DisposableEffect(Unit){
        val engine=TextToSpeech(context){state->ttsReady=state==TextToSpeech.SUCCESS}
        engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(utteranceId:String?){
                if(utteranceId!=null && utteranceId==activeUtteranceId)scope.launch{speaking=true}
            }
            override fun onDone(utteranceId:String?){
                if(utteranceId!=null && utteranceId==activeUtteranceId){
                    scope.launch{
                        speaking=false
                        activeUtteranceId=""
                        if(voiceConversationActive)resumeListeningSignal++
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId:String?){
                if(utteranceId!=null && utteranceId==activeUtteranceId){
                    scope.launch{
                        speaking=false
                        activeUtteranceId=""
                        if(voiceConversationActive)resumeListeningSignal++
                    }
                }
            }
        })
        tts=engine
        onDispose{runCatching{engine.stop()};runCatching{engine.shutdown()}}
    }
    fun applySelectedVoice(){
        val engine=tts?:return
        val requested=selectedLang.locale
        val available=runCatching{engine.isLanguageAvailable(requested)}.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val effective=if(available>=TextToSpeech.LANG_AVAILABLE)requested else Locale.ENGLISH
        engine.language=effective
        val wantsMale=avatar=="MALE"
        engine.setSpeechRate(if(wantsMale).92f else .95f)
        engine.setPitch(if(wantsMale).88f else 1.06f)

        fun voiceScore(v:android.speech.tts.Voice):Int{
            val name=v.name.lowercase(Locale.ROOT)
            var score=v.quality*4-v.latency
            val maleHints=listOf(
                "male","man","mascul","masc","m1","m2","david","daniel","thomas","george",
                "hombre","homme","mann","uomo","homem","erkek"
            )
            val femaleHints=listOf(
                "female","woman","femin","fem","f1","f2","samantha","victoria","karen","anna","susan",
                "mujer","femme","frau","donna","mulher","kadın","kadin"
            )
            val maleMatch=maleHints.any{name.contains(it)}
            val femaleMatch=femaleHints.any{name.contains(it)}
            if(wantsMale){
                if(maleMatch)score+=700
                if(femaleMatch)score-=700
            }else{
                if(femaleMatch)score+=700
                if(maleMatch)score-=700
            }
            if(v.locale==effective)score+=90
            if(v.isNetworkConnectionRequired)score-=90 else score+=170
            if(v.features.contains("notInstalled"))score-=1000
            return score
        }

        val overrideName=store.s(voiceOverrideKey,"")
        val overrideVoice=engine.voices?.firstOrNull{
            it.name==overrideName &&
            it.locale.language.equals(effective.language,true) &&
            !it.features.contains("notInstalled")
        }
        if(overrideVoice!=null){
            engine.voice=overrideVoice
            return
        }

        val languageCandidates=engine.voices
            ?.filter{it.locale.language.equals(effective.language,true)}
            .orEmpty()
        val exactLocaleCandidates=languageCandidates.filter{
            it.locale.country.equals(effective.country,true) &&
            effective.country.isNotBlank()
        }
        val candidates=if(exactLocaleCandidates.isNotEmpty())exactLocaleCandidates else languageCandidates
        val hintedCandidates=candidates.filter{candidate->
            val n=candidate.name.lowercase(Locale.ROOT)
            if(wantsMale){
                listOf("male","mascul","masc","man","hombre","homme","mann","uomo","homem","erkek")
                    .any{n.contains(it)}
            }else{
                listOf("female","femin","fem","woman","mujer","femme","frau","donna","mulher","kadin","kadın")
                    .any{n.contains(it)}
            }
        }
        val pool=if(hintedCandidates.isNotEmpty())hintedCandidates else candidates
        val offlineBest=pool
            .filterNot{it.isNetworkConnectionRequired}
            .maxByOrNull{voiceScore(it)}
        val networkBest=pool
            .filter{it.isNetworkConnectionRequired}
            .maxByOrNull{voiceScore(it)}
        val matching=when{
            offlineBest==null->networkBest
            networkBest==null->offlineBest
            voiceScore(networkBest)>voiceScore(offlineBest)+260->networkBest
            else->offlineBest
        }
        if(matching!=null)engine.voice=matching
    }

    LaunchedEffect(aiLang,avatar,ttsReady){
        if(ttsReady){
            runCatching{tts?.stop()}
            speaking=false
            applySelectedVoice()
        }
    }

    fun speak(text:String){
        if(text.isBlank())return
        if(!ttsReady){
            if(voiceConversationActive)resumeListeningSignal++
            return
        }
        runCatching{tts?.stop()}
        speaking=false
        applySelectedVoice()
        val utteranceId="rs-ai-"+aiLang+"-"+avatar+"-"+System.nanoTime()
        activeUtteranceId=utteranceId
        val result=tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,utteranceId)
        if(result==TextToSpeech.ERROR && voiceConversationActive)resumeListeningSignal++
    }

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            attachMenu=false
            busy=true
            status=rsAiExtraV163(aiLang,"image_ready")
            scope.launch{
                val local=withContext(Dispatchers.IO){
                    rsCacheTemporaryAiMediaV163(context,uri,"IMAGE").getOrNull()
                }
                busy=false
                if(local!=null){
                    pickedUri=Uri.parse(local)
                    pickedKind="IMAGE"
                    status=""
                }else status="Could not prepare image."
            }
        }
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            attachMenu=false
            busy=true
            status=rsAiExtraV163(aiLang,"video_ready")
            scope.launch{
                val local=withContext(Dispatchers.IO){
                    rsCacheTemporaryAiMediaV163(context,uri,"VIDEO").getOrNull()
                }
                busy=false
                if(local!=null){
                    pickedUri=Uri.parse(local)
                    pickedKind="VIDEO"
                    status=""
                }else status="Could not prepare video."
            }
        }
    }

    val musicPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri->
        if(uri!=null){
            attachMenu=false
            rsPrepareMusicTrackV169(context,uri,"RS Music")
                .onSuccess{track->
                    rsSaveMusicTrackV169(store,track)
                        .onSuccess{
                            pendingMusic=track
                            musicDialog=true
                            status="Saved to RS Music · "+track.name
                        }
                        .onFailure{status=it.message?:"Could not save music."}
                }
                .onFailure{status=it.message?:"Could not open music file."}
        }
    }
    val quietVoice=remember(context,aiLang){
        RsQuietVoiceControllerV171(
            context=context,
            onResult={spoken->voiceResult=spoken.take(1800)},
            onStatus={value->status=value},
            onIdle={resumeListeningSignal++}
        )
    }
    DisposableEffect(quietVoice){
        onDispose{quietVoice.destroy()}
    }

    val microphonePermissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){granted->
        if(granted){
            voiceConversationActive=true
            quietVoice.start(selectedLang.locale)
        }else{
            voiceConversationActive=false
            status=rsAiExtraV163(aiLang,"mic_required")
        }
    }

    fun startVoice(){
        if(speaking)runCatching{tts?.stop()}
        speaking=false
        voiceConversationActive=true
        if(
            ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)
            ==PackageManager.PERMISSION_GRANTED
        ){
            quietVoice.start(selectedLang.locale)
        }else{
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun send(textOverride:String?=null){
        val body=(textOverride?:draft).trim()
        val media=pickedUri
        val kind=pickedKind
        if(body.isBlank()&&media==null)return
        if(busy)return
        busy=true
        status=""
        val userMessage=RsAiChatMessageV163(
            id=System.currentTimeMillis(),
            mine=true,
            text=body,
            mediaUri=media?.toString(),
            mediaKind=kind
        )
        messages=(messages+userMessage).takeLast(100)
        draft=""
        pickedUri=null
        pickedKind=null

        val currentRoute=store.s("ai_current_route_v177","")
        val guideRequest=if(kind==null){
            rsAiAppGuideMatchV175(body,selectedLang,role)
                ?:if(rsAiGenericCurrentPageHelpV177(body))rsAiAppGuideForRouteV177(currentRoute,selectedLang,role) else null
        }else null
        if(guideRequest!=null){
            val guideText=rsAiAppGuideTextV175(guideRequest,selectedLang)
            val guideVisual=rsVisualUriWithBundledFallbackV113(context,store,guideRequest.route)
            messages=rsPruneAiChatV176(
                messages+RsAiChatMessageV163(
                    id=System.currentTimeMillis()+1,
                    mine=false,
                    text=guideText,
                    mediaUri=guideVisual.takeIf{it.isNotBlank()},
                    mediaKind=guideVisual.takeIf{it.isNotBlank()}?.let{"GUIDE"}
                )
            )
            busy=false
            status=""
            if(autoSpeak)speak(guideText) else if(voiceConversationActive)resumeListeningSignal++
            return
        }

        val namedTrackRequest=if(
            kind==null &&
            listOf("play ","speel ","tocar ","reproducir ","joue ","abspielen ","riproduci ","odtwórz ","çal ")
                .any{body.lowercase().contains(it)}
        )rsFindMusicTrackV171(store,body) else null

        if(namedTrackRequest!=null){
            val reply=rsPlayMusicTrackV169(musicController,namedTrackRequest)
                .fold(
                    onSuccess={"Playing "+namedTrackRequest.name+"."},
                    onFailure={it.message?:"I couldn't start that track."}
                )
            messages=(messages+RsAiChatMessageV163(
                id=System.currentTimeMillis()+1,
                mine=false,
                text=reply
            )).takeLast(100)
            busy=false
            status=""
            if(autoSpeak)speak(reply) else if(voiceConversationActive)resumeListeningSignal++
            return
        }

        val platformIntent=if(kind==null)rsAiPlatformIntentV171(body) else RsAiPlatformIntentV171.None
        if(platformIntent !is RsAiPlatformIntentV171.None){
            val reply=when(platformIntent){
                is RsAiPlatformIntentV171.ChangeLanguage->{
                    val target=languages.firstOrNull{it.code==platformIntent.code}
                    if(target!=null){
                        aiLang=target.code
                        store.ps("ai_voice_language_v161",target.code)
                        when(target.code){
                            "nl"->"Natuurlijk. Ik spreek nu Nederlands."
                            "pt"->"Claro. Agora vou falar em português."
                            "es"->"Claro. Ahora hablaré en español."
                            "fr"->"Bien sûr. Je parle maintenant français."
                            "de"->"Natürlich. Ich spreche jetzt Deutsch."
                            "it"->"Certo. Ora parlerò in italiano."
                            "pl"->"Oczywiście. Teraz będę mówić po polsku."
                            "tr"->"Elbette. Artık Türkçe konuşacağım."
                            else->"Of course. I’ll speak English now."
                        }
                    }else "I couldn’t switch to that language."
                }
                is RsAiPlatformIntentV171.ChangeAvatar->{
                    avatar=platformIntent.avatar
                    store.ps("ai_avatar_gender_v161",platformIntent.avatar)
                    if(platformIntent.avatar=="MALE")"Marcus is active now. I’m ready."
                    else "Sofia is active now. I’m ready."
                }
                is RsAiPlatformIntentV171.UploadMusic->{
                    musicPicker.launch(arrayOf("audio/*"))
                    "I’m opening your music files. Choose the track you want and I’ll handle the rest."
                }
                is RsAiPlatformIntentV171.PlayMusic->{
                    val named=rsFindMusicTrackV171(store,body)
                    if(named!=null){
                        rsPlayMusicTrackV169(musicController,named)
                        "Playing "+named.name+"."
                    }else{
                        val first=rsAllMusicTracksV171(store).firstOrNull()
                        if(first!=null){
                            rsPlayMusicTrackV169(musicController,first)
                            "Playing "+first.name+"."
                        }else "Your RS Music library is empty. Say “upload music” and I’ll open your music files."
                    }
                }
                is RsAiPlatformIntentV171.PauseMusic->{
                    runCatching{musicController?.pause()}
                    "Music paused."
                }
                is RsAiPlatformIntentV171.StopMusic->{
                    runCatching{musicController?.stop()}
                    "Music stopped."
                }
                is RsAiPlatformIntentV171.NextMusic->{
                    runCatching{musicController?.seekToNextMediaItem();musicController?.play()}
                    "Playing the next track."
                }
                is RsAiPlatformIntentV171.PreviousMusic->{
                    runCatching{musicController?.seekToPreviousMediaItem();musicController?.play()}
                    "Going back to the previous track."
                }
                is RsAiPlatformIntentV171.OpenRoute->{
                    scope.launch{
                        kotlinx.coroutines.delay(250)
                        onNavigate(platformIntent.route)
                    }
                    "Opening "+platformIntent.label+"."
                }
                is RsAiPlatformIntentV171.Help->{
                    "I can navigate through RS KICKBOXING, open pages, control or upload music, switch Sofia or Marcus, change my spoken language, help with settings and app features, and coach your kickboxing training. Tell me naturally what you want me to do."
                }
                RsAiPlatformIntentV171.None->""
            }
            messages=(messages+RsAiChatMessageV163(
                id=System.currentTimeMillis()+1,
                mine=false,
                text=reply
            )).takeLast(100)
            busy=false
            status=""
            if(autoSpeak && reply.isNotBlank())speak(reply) else if(voiceConversationActive)resumeListeningSignal++
            return
        }

        scope.launch{
            val localFirst=when{
                kind=="VIDEO"&&media!=null->{
                    status=rsAiExtraV163(aiLang,"analyzing")
                    rsAnalyzeTechniqueVisionV106(
                        context,
                        media.toString(),
                        if(body.isBlank())"Uploaded kickboxing technique" else body,
                        selectedLang
                    ).getOrElse{rsAiLocalCoachAnswerV164(aiLang,body.ifBlank{"kickboxing technique"})}
                }
                kind=="IMAGE"&&media!=null->rsAiLocalCoachAnswerV164(
                    aiLang,
                    body.ifBlank{"kickboxing technique image"}
                )
                else->rsAiLocalCoachAnswerV164(aiLang,body)
            }

            val earlyRefs=if(kind=="VIDEO"||kind=="IMAGE"||body.length>2)
                rsAiReferencePreviewsV164(context,body,localFirst)
            else emptyList()

            val trainerReferenceSummary=earlyRefs.joinToString("\n"){ref->
                ref.item.title+" | tags: "+ref.item.techniqueTags.joinToString(", ")+" | trainer note: "+ref.item.description
            }
            val currentPage=store.s("ai_current_route_v177","")
            val currentPageTitle=if(currentPage.isBlank())"" else rsRouteTitle(
                selectedLang,currentPage,currentPage.replace('_',' ').replaceFirstChar{it.uppercase()}
            )
            val referenceSummary=(
                "CURRENT RS PAGE: "+currentPage+" | "+currentPageTitle+
                "\n\nRS APP FEATURES:\n"+rsAiAppKnowledgeSummaryV175(selectedLang,role).take(2300)+
                "\n\nTRAINER REFERENCES:\n"+trainerReferenceSummary.take(1100)
            )

            val reply=if(kind=="VIDEO"){
                localFirst
            }else{
                rsOnlineAiCoachV164(
                    question=body.ifBlank{"Kickboxing coaching"},
                    lang=selectedLang,
                    referenceSummary=referenceSummary
                ).getOrElse{localFirst}
            }

            val refs=if(earlyRefs.isNotEmpty())earlyRefs
            else rsAiReferencePreviewsV164(context,body,reply)

            messages=(messages+RsAiChatMessageV163(
                id=System.currentTimeMillis()+1,
                mine=false,
                text=reply,
                references=refs
            )).takeLast(100)
            status=""
            busy=false
            if(autoSpeak)speak(reply) else if(voiceConversationActive)resumeListeningSignal++
        }
    }

    LaunchedEffect(Unit){
        val pending=store.s("ai_pending_spoken_v171","").trim()
        if(pending.isNotBlank()){
            store.ps("ai_pending_spoken_v171","")
            kotlinx.coroutines.delay(650)
            voiceResult=pending
        }
    }

    LaunchedEffect(voiceResult){
        val spoken=voiceResult?.trim().orEmpty()
        if(spoken.isNotBlank()&&!busy){
            voiceResult=null
            send(spoken)
        }
    }

    LaunchedEffect(resumeListeningSignal,voiceConversationActive,aiLang){
        if(voiceConversationActive && !speaking && !busy){
            kotlinx.coroutines.delay(1200)
            startVoice()
        }
    }

    LaunchedEffect(ttsReady,immersiveGreetingPending){
        if(immersiveGreetingPending && ttsReady){
            store.pb("ai_start_listening_v168",false)
            immersiveGreetingPending=false
            voiceConversationActive=true
            val name=if(avatar=="FEMALE")"Sofia" else "Marcus"
            val greeting=when(aiLang){
                "nl"->"Hoi, ik ben "+name+". Wat kan ik vandaag voor je doen?"
                "pt"->"Olá, sou "+name+". O que posso fazer por ti hoje?"
                "es"->"Hola, soy "+name+". ¿Qué puedo hacer por ti hoy?"
                "fr"->"Salut, je suis "+name+". Que puis-je faire pour toi aujourd’hui ?"
                "de"->"Hallo, ich bin "+name+". Was kann ich heute für dich tun?"
                "it"->"Ciao, sono "+name+". Cosa posso fare per te oggi?"
                "pl"->"Cześć, jestem "+name+". Co mogę dziś dla ciebie zrobić?"
                "tr"->"Merhaba, ben "+name+". Bugün senin için ne yapabilirim?"
                else->"Hi, I’m "+name+". What can I do for you today?"
            }
            messages=(messages+RsAiChatMessageV163(
                id=System.currentTimeMillis(),
                mine=false,
                text=greeting
            )).takeLast(100)
            speak(greeting)
        }else if(immersiveGreetingPending && !ttsReady){
            kotlinx.coroutines.delay(700)
            if(!ttsReady){
                store.pb("ai_start_listening_v168",false)
                immersiveGreetingPending=false
                voiceConversationActive=true
                startVoice()
            }
        }
    }

    if(trainerReferences && role==RsRole.TRAINER){
        Column(Modifier.fillMaxSize()){
            Row(
                Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                OutlinedButton(onClick={trainerReferences=false}){Text("‹ AI Coach")}
                Spacer(Modifier.weight(1f))
                Text("TRAINER REFERENCES",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
            }
            Box(Modifier.fillMaxWidth().weight(1f)){
                RsTrainerAiReferenceChatV125(c,selectedLang)
            }
        }
        return
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF02060A),
                    c.panel.copy(alpha=.72f),
                    Color.Black
                )
            )
        )
    ){
    Column(
        Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=4.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ){
        Box(Modifier.fillMaxWidth().weight(1f)){
        RsAiAvatarStageV163(
            c=c,
            store=store,
            avatar=avatar,
            speaking=speaking,
            listening=voiceConversationActive && !speaking && !busy,
            thinking=busy && !speaking,
            avatarMotion=avatarMotionEnabled,
            renderMode=avatarRenderMode,
            language=selectedLang,
            onAvatarChange={next->
                runCatching{tts?.stop()}
                activeUtteranceId=""
                runCatching{quietVoice.stop()}
                speaking=false
                voiceConversationActive=false
                avatar=next
                store.ps("ai_avatar_gender_v161",next)
                status=when(aiLang){
                    "nl"->if(next=="MALE")"Marcus-stem geselecteerd." else "Sofia-stem geselecteerd."
                    "pt"->if(next=="MALE")"Voz do Marcus selecionada." else "Voz da Sofia selecionada."
                    "es"->if(next=="MALE")"Voz de Marcus seleccionada." else "Voz de Sofia seleccionada."
                    "fr"->if(next=="MALE")"Voix de Marcus sélectionnée." else "Voix de Sofia sélectionnée."
                    "de"->if(next=="MALE")"Marcus-Stimme ausgewählt." else "Sofia-Stimme ausgewählt."
                    "it"->if(next=="MALE")"Voce di Marcus selezionata." else "Voce di Sofia selezionata."
                    "pl"->if(next=="MALE")"Wybrano głos Marcusa." else "Wybrano głos Sofii."
                    "tr"->if(next=="MALE")"Marcus sesi seçildi." else "Sofia sesi seçildi."
                    else->if(next=="MALE")"Marcus voice selected." else "Sofia voice selected."
                }
            },
            onLanguageChange={nextLang->
                runCatching{tts?.stop()}
                activeUtteranceId=""
                runCatching{quietVoice.stop()}
                speaking=false
                voiceConversationActive=false
                aiLang=nextLang.code
                store.ps("ai_voice_language_v161",nextLang.code)
                val coachName=if(avatar=="FEMALE")"Sofia" else "Marcus"
                val languageMessage=when(nextLang.code){
                    "nl"->coachName+" spreekt en schrijft nu Nederlands."
                    "pt"->coachName+" agora fala e escreve em português."
                    "es"->coachName+" ahora habla y escribe en español."
                    "fr"->coachName+" parle et écrit maintenant en français."
                    "de"->coachName+" spricht und schreibt jetzt Deutsch."
                    "it"->coachName+" ora parla e scrive in italiano."
                    "pl"->coachName+" mówi i pisze teraz po polsku."
                    "tr"->coachName+" artık Türkçe konuşuyor ve yazıyor."
                    else->coachName+" now speaks and writes in English."
                }
                messages=rsPruneAiChatV176(
                    messages+RsAiChatMessageV163(
                        id=System.currentTimeMillis(),
                        mine=false,
                        text=languageMessage
                    )
                )
                status=languageMessage
            },
            languages=languages,
            onOptions={optionsMenu=true},
            modifier=Modifier.fillMaxSize(),
            immersive=true,
            sensorParallaxEnabled=sensorParallaxEnabled
        )

        Box(
            Modifier.fillMaxSize()
                .padding(start=8.dp,end=8.dp,top=82.dp,bottom=90.dp)
        ){
            LazyColumn(
                state=aiListState,
                modifier=Modifier.fillMaxSize(),
                verticalArrangement=Arrangement.spacedBy(6.dp),
                contentPadding=PaddingValues(top=6.dp,bottom=8.dp)
            ){
                if(messages.isEmpty()){
                    item{
                        val coachName=if(avatar=="FEMALE")"Sofia" else "Marcus"
                        Surface(
                            color=Color.Black.copy(alpha=.52f),
                            shape=RoundedCornerShape(22.dp),
                            border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                            modifier=Modifier.fillMaxWidth()
                        ){
                            Text(
                                when(aiLang){
                                    "nl"->"Praat met "+coachName+" alsof je met je echte coach praat."
                                    "pt"->"Fala com "+coachName+" como falarias com o teu treinador real."
                                    "es"->"Habla con "+coachName+" como con tu entrenador real."
                                    "fr"->"Parle à "+coachName+" comme à ton vrai coach."
                                    "de"->"Sprich mit "+coachName+" wie mit deinem echten Trainer."
                                    "it"->"Parla con "+coachName+" come faresti con il tuo vero allenatore."
                                    "pl"->"Rozmawiaj z "+coachName+" jak ze swoim prawdziwym trenerem."
                                    "tr"->coachName+" ile gerçek antrenörünle konuşur gibi konuş."
                                    else->"Talk to "+coachName+" like you would to a real coach."
                                },
                                color=c.muted,
                                fontSize=9.sp,
                                modifier=Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                items(messages,key={it.id}){message->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical=2.dp),
                        horizontalArrangement=if(message.mine)Arrangement.End else Arrangement.Start
                    ){
                        Surface(
                            color=if(message.mine)c.gold.copy(alpha=.13f) else Color(0xFF07131E).copy(alpha=.54f),
                            shape=RoundedCornerShape(18.dp),
                            border=BorderStroke(
                                1.dp,
                                if(message.mine)c.gold.copy(alpha=.48f) else Color(0xFF58C9FF).copy(alpha=.48f)
                            ),
                            modifier=if(message.mine)Modifier.fillMaxWidth(.86f) else Modifier.fillMaxWidth()
                        ){
                            Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                    Text(
                                        if(message.mine)"YOU" else if(avatar=="FEMALE")"SOFIA" else "MARCUS",
                                        color=if(message.mine)c.gold else Color(0xFF58C9FF),
                                        fontSize=7.sp,
                                        fontWeight=FontWeight.Black,
                                        modifier=Modifier.weight(1f)
                                    )
                                    Box{
                                        TextButton(
                                            onClick={aiMessageMenuId=message.id},
                                            contentPadding=PaddingValues(2.dp),
                                            modifier=Modifier.height(24.dp)
                                        ){Text("⋮",fontSize=16.sp,color=c.bright)}
                                        DropdownMenu(
                                            expanded=aiMessageMenuId==message.id,
                                            onDismissRequest={aiMessageMenuId=null}
                                        ){
                                            if(message.mediaUri!=null && message.mediaKind!=null){
                                                DropdownMenuItem(
                                                    text={Text(rsAiExtraV163(aiLang,"save_gallery"))},
                                                    onClick={
                                                        aiMessageMenuId=null
                                                        rsSaveChatMediaToGalleryV163(
                                                            context,
                                                            store,
                                                            message.mediaUri,
                                                            message.mediaKind,
                                                            if(message.mediaKind=="VIDEO")"AI video" else "AI image"
                                                        ).onSuccess{status=rsAiExtraV163(aiLang,"saved")}
                                                         .onFailure{status=it.message?:"Could not save media."}
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text={Text(rsAiExtraV163(aiLang,"delete_message"))},
                                                onClick={
                                                    aiMessageMenuId=null
                                                    messages=messages.filterNot{it.id==message.id}
                                                }
                                            )
                                        }
                                    }
                                }
                                if(message.mediaUri!=null){
                                    if(message.mediaKind=="IMAGE"||message.mediaKind=="GUIDE"){
                                        RsUriPreviewV21(
                                            message.mediaUri,
                                            Modifier.fillMaxWidth().heightIn(min=70.dp,max=140.dp),
                                            "CENTER"
                                        )
                                    }else if(message.mediaKind=="VIDEO"){
                                        RsMiniLocalVideoV163(
                                            message.mediaUri,
                                            Modifier.fillMaxWidth().height(110.dp)
                                        )
                                    }
                                }
                                if(message.text.isNotBlank()){
                                    Text(message.text,color=c.text,fontSize=11.sp,lineHeight=15.sp)
                                }
                                message.references.forEach{ref->
                                    Surface(
                                        color=Color.Black.copy(alpha=.58f),
                                        shape=RoundedCornerShape(14.dp),
                                        border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
                                        modifier=Modifier.fillMaxWidth()
                                    ){
                                        Column(Modifier.padding(7.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                            Text("TRAINER REFERENCE · "+ref.item.title,color=c.gold,fontWeight=FontWeight.Black,fontSize=8.sp)
                                            if(ref.item.kind=="VIDEO")RsMiniLocalVideoV163(ref.localUri,Modifier.fillMaxWidth().height(105.dp))
                                            else RsUriPreviewV21(ref.localUri,Modifier.fillMaxWidth().height(105.dp),"CENTER")
                                            if(ref.item.description.isNotBlank())Text(ref.item.description,color=c.text,fontSize=9.sp,lineHeight=13.sp)
                                            if(ref.item.techniqueTags.isNotEmpty())Text(ref.item.techniqueTags.take(8).joinToString(" · "),color=c.muted,fontSize=7.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start=8.dp,end=8.dp,bottom=4.dp),
            verticalArrangement=Arrangement.spacedBy(6.dp)
        ){
        if(status.isNotBlank()){
            Text(status,color=Color(0xFF58C9FF),fontSize=8.sp,modifier=Modifier.padding(horizontal=8.dp))
        }

        if(pickedUri!=null){
            Surface(
                color=Color.Black.copy(alpha=.88f),
                shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.25f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Row(
                    Modifier.fillMaxWidth().padding(7.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    Box(Modifier.size(width=92.dp,height=62.dp).clip(RoundedCornerShape(12.dp))){
                        if(pickedKind=="IMAGE"){
                            RsUriPreviewV21(pickedUri.toString(),Modifier.fillMaxSize(),"CENTER")
                        }else{
                            RsMiniLocalVideoV163(pickedUri.toString(),Modifier.fillMaxSize())
                        }
                    }
                    Column(Modifier.weight(1f)){
                        Text(
                            if(pickedKind=="VIDEO")rsAiExtraV163(aiLang,"video_ready") else rsAiExtraV163(aiLang,"image_ready"),
                            color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp
                        )
                        Text(
                            if(pickedKind=="VIDEO")rsAiExtraV163(aiLang,"video_preview") else rsAiExtraV163(aiLang,"image_preview"),
                            color=c.muted,fontSize=8.sp
                        )
                    }
                    TextButton(onClick={pickedUri=null;pickedKind=null}){Text("×",fontSize=20.sp)}
                }
            }
        }

        Surface(
            color=Color(0xFF02070B).copy(alpha=.78f),
            shape=RoundedCornerShape(30.dp),
            border=BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF58C9FF).copy(alpha=.72f),
                        c.bright.copy(alpha=.62f),
                        c.gold.copy(alpha=.70f),
                        Color(0xFF58C9FF).copy(alpha=.42f)
                    )
                )
            ),
            tonalElevation=24.dp,
            modifier=Modifier.fillMaxWidth().navigationBarsPadding()
        ){
            Row(
                Modifier.fillMaxWidth().padding(7.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ){
                OutlinedButton(
                    onClick={attachMenu=true},
                    enabled=!busy,
                    modifier=Modifier.size(46.dp),
                    shape=CircleShape,
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.72f)),
                    contentPadding=PaddingValues(0.dp)
                ){Text("+",fontSize=25.sp,color=c.bright,fontWeight=FontWeight.Black)}

                OutlinedTextField(
                    value=draft,
                    onValueChange={draft=it.take(1200)},
                    placeholder={Text(rsAiUiV162(aiLang,"question"),fontSize=10.sp)},
                    modifier=Modifier.weight(1f),
                    minLines=1,
                    maxLines=3,
                    enabled=!busy,
                    shape=RoundedCornerShape(22.dp),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedBorderColor=Color(0xFF58C9FF).copy(alpha=.78f),
                        unfocusedBorderColor=c.gold.copy(alpha=.30f),
                        focusedContainerColor=Color(0xFF06131A).copy(alpha=.62f),
                        unfocusedContainerColor=Color.Black.copy(alpha=.48f)
                    )
                )

                OutlinedButton(
                    onClick={
                        if(voiceConversationActive){
                            voiceConversationActive=false
                            quietVoice.stop()
                            status=""
                        }else startVoice()
                    },
                    enabled=!busy,
                    modifier=Modifier.size(44.dp),
                    shape=CircleShape,
                    border=BorderStroke(1.dp,if(voiceConversationActive)Color(0xFF58C9FF) else c.gold.copy(alpha=.55f)),
                    contentPadding=PaddingValues(0.dp)
                ){Text(if(voiceConversationActive)"◉" else "🎙",fontSize=15.sp)}

                Button(
                    onClick={send()},
                    enabled=!busy&&(draft.isNotBlank()||pickedUri!=null),
                    modifier=Modifier.size(44.dp),
                    shape=CircleShape,
                    contentPadding=PaddingValues(0.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=c.bright,contentColor=Color.Black)
                ){Text(if(busy)"…" else "➤",fontWeight=FontWeight.Black)}
            }
        }
        }
        }
    }

    if(optionsMenu){
        androidx.compose.ui.window.Dialog(onDismissRequest={optionsMenu=false}){
            Surface(
                color=Color.Black.copy(alpha=.98f),
                shape=RoundedCornerShape(24.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Text(rsAiExtraV163(aiLang,"settings"),color=c.bright,fontWeight=FontWeight.Black)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text(rsAiExtraV163(aiLang,"auto_speak"),color=c.text,fontSize=10.sp,modifier=Modifier.weight(1f))
                        Switch(autoSpeak,{
                            autoSpeak=it
                            store.pb("ai_auto_speak_v163",it)
                        })
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(rsAiExtraV163(aiLang,"avatar_motion"),color=c.text,fontSize=10.sp)
                            Text(rsAiExtraV163(aiLang,"avatar_motion_sub"),color=c.muted,fontSize=8.sp)
                        }
                        Switch(avatarMotionEnabled,{
                            avatarMotionEnabled=it
                            store.pb("ai_avatar_motion_v163",it)
                        })
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(rsAiExtraV163(aiLang,"motion_depth"),color=c.text,fontSize=10.sp)
                            Text(rsAiExtraV163(aiLang,"motion_depth_sub"),color=c.muted,fontSize=8.sp)
                        }
                        Switch(sensorParallaxEnabled,{
                            sensorParallaxEnabled=it
                            store.pb("ai_sensor_parallax_v180",it)
                        })
                    }
                    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text(rsAiExtraV163(aiLang,"voice_profile"),color=c.text,fontSize=10.sp)
                        Text(rsAiExtraV163(aiLang,"voice_profile_sub"),color=c.muted,fontSize=8.sp)
                        Box{
                            OutlinedButton(
                                onClick={voiceProfileMenu=true},
                                modifier=Modifier.fillMaxWidth()
                            ){
                                Text(
                                    if(voiceOverrideName.isBlank())rsAiExtraV163(aiLang,"voice_auto")
                                    else voiceOverrideName.take(42),
                                    maxLines=1,
                                    fontSize=8.sp
                                )
                            }
                            DropdownMenu(
                                expanded=voiceProfileMenu,
                                onDismissRequest={voiceProfileMenu=false}
                            ){
                                DropdownMenuItem(
                                    text={Text(rsAiExtraV163(aiLang,"voice_auto"),fontSize=9.sp)},
                                    onClick={
                                        voiceProfileMenu=false
                                        store.ps(voiceOverrideKey,"")
                                        runCatching{tts?.stop()}
                                        speaking=false
                                        applySelectedVoice()
                                        status=rsAiExtraV163(aiLang,"voice_selected")
                                    }
                                )
                                selectableVoices.forEach{voice->
                                    DropdownMenuItem(
                                        text={
                                            Column{
                                                Text(voice.name,fontSize=8.sp,maxLines=1)
                                                Text(
                                                    voice.locale.toLanguageTag()+
                                                        if(voice.isNetworkConnectionRequired)" · network" else " · local",
                                                    fontSize=7.sp,
                                                    color=c.muted
                                                )
                                            }
                                        },
                                        onClick={
                                            voiceProfileMenu=false
                                            store.ps(voiceOverrideKey,voice.name)
                                            runCatching{tts?.stop()}
                                            speaking=false
                                            applySelectedVoice()
                                            status=rsAiExtraV163(aiLang,"voice_selected")
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick={
                                val sample=when(aiLang){
                                    "nl"->"Dit is mijn geselecteerde Nederlandse stem."
                                    "pt"->"Esta é a minha voz portuguesa selecionada."
                                    "es"->"Esta es mi voz seleccionada."
                                    "fr"->"Voici ma voix sélectionnée."
                                    "de"->"Das ist meine ausgewählte Stimme."
                                    "it"->"Questa è la mia voce selezionata."
                                    "pl"->"To jest mój wybrany głos."
                                    "tr"->"Bu benim seçili sesim."
                                    else->"This is my selected voice."
                                }
                                speak(sample)
                            },
                            enabled=ttsReady,
                            modifier=Modifier.fillMaxWidth()
                        ){Text(rsAiExtraV163(aiLang,"test_voice"),fontSize=8.sp)}
                    }
                    Text(rsAiExtraV163(aiLang,"render"),color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        FilterChip(
                            selected=avatarRenderMode=="CINEMATIC_3D",
                            onClick={
                                avatarRenderMode="CINEMATIC_3D"
                                store.ps("ai_avatar_render_mode_v176","CINEMATIC_3D")
                            },
                            label={Text(rsAiExtraV163(aiLang,"realistic_3d"),fontSize=8.sp)},
                            modifier=Modifier.weight(1f)
                        )
                        FilterChip(
                            selected=avatarRenderMode=="CLEAN",
                            onClick={
                                avatarRenderMode="CLEAN"
                                store.ps("ai_avatar_render_mode_v176","CLEAN")
                            },
                            label={Text(rsAiExtraV163(aiLang,"clean"),fontSize=8.sp)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                    if(role==RsRole.TRAINER){
                        OutlinedButton(
                            onClick={optionsMenu=false;trainerReferences=true},
                            modifier=Modifier.fillMaxWidth()
                        ){Text(rsAiExtraV163(aiLang,"references"))}
                    }
                    OutlinedButton(
                        onClick={optionsMenu=false;confirmClearAiChat=true},
                        enabled=messages.isNotEmpty(),
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsAiExtraV163(aiLang,"delete_conversation"))}
                    OutlinedButton(onClick={optionsMenu=false},modifier=Modifier.fillMaxWidth()){Text(rsAiExtraV163(aiLang,"close"))}
                }
            }
        }
    }

    if(confirmClearAiChat){
        androidx.compose.ui.window.Dialog(onDismissRequest={confirmClearAiChat=false}){
            Surface(
                color=Color.Black.copy(alpha=.98f),
                shape=RoundedCornerShape(24.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text(rsAiExtraV163(aiLang,"delete_title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                    Text(
                        rsAiExtraV163(aiLang,"delete_desc"),
                        color=c.text,fontSize=10.sp
                    )
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(
                            onClick={confirmClearAiChat=false},
                            modifier=Modifier.weight(1f)
                        ){Text(rsAiExtraV163(aiLang,"cancel"))}
                        Button(
                            onClick={
                                messages=emptyList()
                                rsSaveAiChatHistoryV174(store,aiHistoryKey,emptyList())
                                confirmClearAiChat=false
                                status=rsAiExtraV163(aiLang,"conversation_deleted")
                            },
                            modifier=Modifier.weight(1f)
                        ){Text(rsAiExtraV163(aiLang,"delete_all"))}
                    }
                }
            }
        }
    }

    if(attachMenu){
        androidx.compose.ui.window.Dialog(onDismissRequest={attachMenu=false}){
            Surface(
                color=Color.Black.copy(alpha=.98f),
                shape=RoundedCornerShape(26.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                    Text(rsAiExtraV163(aiLang,"media"),color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp)
                    Text(rsAiExtraV163(aiLang,"media_sub"),color=c.muted,fontSize=9.sp)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(
                            onClick={
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("▣  "+rsAiExtraV163(aiLang,"photo"),fontSize=9.sp)}
                        OutlinedButton(
                            onClick={
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("▶  "+rsAiExtraV163(aiLang,"video"),fontSize=9.sp)}
                    }
                    OutlinedButton(
                        onClick={musicPicker.launch(arrayOf("audio/*"))},
                        modifier=Modifier.fillMaxWidth()
                    ){Text("♫  Music",fontSize=9.sp)}
                    OutlinedButton(
                        onClick={attachMenu=false;startVoice()},
                        modifier=Modifier.fillMaxWidth()
                    ){Text("🎙  "+rsAiExtraV163(aiLang,"voice"),fontSize=9.sp)}
                }
            }
        }
    }

    }

    if(musicDialog && pendingMusic!=null){
        val track=pendingMusic!!
        val playlists=rsMusicPlaylistNamesV169(store)

        androidx.compose.ui.window.Dialog(
            onDismissRequest={
                musicDialog=false
                pendingMusic=null
                playlistMenu=false
                newPlaylistName=""
            }
        ){
            Surface(
                color=Color.Black.copy(alpha=.98f),
                shape=RoundedCornerShape(26.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                    Text(rsAiExtraV163(aiLang,"music_title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp)
                    Text(track.name,color=c.text,fontSize=11.sp,maxLines=2)
                    Text(
                        rsAiExtraV163(aiLang,"music_choose"),
                        color=c.muted,fontSize=9.sp
                    )

                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(
                            onClick={
                                rsPlayMusicTrackV169(musicController,track)
                                    .onSuccess{status="Playing "+track.name}
                                    .onFailure{status=it.message?:"Could not play music."}
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("▶ "+rsAiExtraV163(aiLang,"play"),fontSize=9.sp)}

                        OutlinedButton(
                            onClick={
                                rsSaveMusicTrackV169(store,track)
                                    .onSuccess{
                                        status="Saved to RS Music."
                                        musicDialog=false
                                        pendingMusic=null
                                    }
                                    .onFailure{status=it.message?:"Could not save music."}
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("✓ "+rsAiExtraV163(aiLang,"done"),fontSize=9.sp)}
                    }

                    if(playlists.isNotEmpty()){
                        Box{
                            OutlinedButton(
                                onClick={playlistMenu=true},
                                modifier=Modifier.fillMaxWidth()
                            ){Text(rsAiExtraV163(aiLang,"add_playlist"),fontSize=9.sp)}
                            DropdownMenu(
                                expanded=playlistMenu,
                                onDismissRequest={playlistMenu=false}
                            ){
                                playlists.forEach{name->
                                    DropdownMenuItem(
                                        text={Text(name)},
                                        onClick={
                                            playlistMenu=false
                                            rsAddMusicTrackToPlaylistV169(store,name,track)
                                                .onSuccess{
                                                    status="Added to "+name+"."
                                                    musicDialog=false
                                                    pendingMusic=null
                                                }
                                                .onFailure{status=it.message?:"Could not update playlist."}
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value=newPlaylistName,
                        onValueChange={newPlaylistName=it.take(60)},
                        label={Text(rsAiExtraV163(aiLang,"new_playlist"))},
                        singleLine=true,
                        modifier=Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick={
                            rsAddMusicTrackToPlaylistV169(store,newPlaylistName,track)
                                .onSuccess{
                                    status="Playlist "+newPlaylistName.trim()+" created."
                                    musicDialog=false
                                    pendingMusic=null
                                    newPlaylistName=""
                                }
                                .onFailure{status=it.message?:"Could not create playlist."}
                        },
                        enabled=newPlaylistName.isNotBlank(),
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsAiExtraV163(aiLang,"create_playlist"))}

                    TextButton(
                        onClick={
                            musicDialog=false
                            pendingMusic=null
                            newPlaylistName=""
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsAiExtraV163(aiLang,"close"))}
                }
            }
        }
    }
}

@Composable
private fun RsAiAvatarStageV163(
    c:RsPalette,
    store:RsStore,
    avatar:String,
    speaking:Boolean,
    listening:Boolean,
    thinking:Boolean,
    avatarMotion:Boolean,
    renderMode:String,
    language:RsLang,
    onAvatarChange:(String)->Unit,
    onLanguageChange:(RsLang)->Unit,
    languages:List<RsLang>,
    onOptions:()->Unit,
    modifier:Modifier=Modifier.fillMaxWidth().height(330.dp),
    immersive:Boolean=false,
    sensorParallaxEnabled:Boolean=true
){
    val context=LocalContext.current
    var sensorX by remember{mutableFloatStateOf(0f)}
    var sensorY by remember{mutableFloatStateOf(0f)}
    DisposableEffect(sensorParallaxEnabled){
        if(!sensorParallaxEnabled){
            sensorX=0f
            sensorY=0f
            onDispose{}
        }else{
            val manager=context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
            val rotationSensor=manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val fallbackSensor=manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val sensor=rotationSensor?:fallbackSensor
            val rotationMatrix=FloatArray(9)
            val orientation=FloatArray(3)
            val smoothing=.16f
            val listener=object:SensorEventListener{
                override fun onSensorChanged(event:SensorEvent){
                    val rawX:Float
                    val rawY:Float
                    if(event.sensor.type==Sensor.TYPE_ROTATION_VECTOR){
                        SensorManager.getRotationMatrixFromVector(rotationMatrix,event.values)
                        SensorManager.getOrientation(rotationMatrix,orientation)
                        val pitch=orientation[1]
                        val roll=orientation[2]
                        rawX=(roll/(Math.PI.toFloat()/5f)).coerceIn(-1f,1f)
                        rawY=(pitch/(Math.PI.toFloat()/6f)).coerceIn(-1f,1f)
                    }else{
                        rawX=(event.values[0]/9.81f).coerceIn(-1f,1f)
                        rawY=(event.values[1]/9.81f).coerceIn(-1f,1f)
                    }
                    sensorX+=(rawX-sensorX)*smoothing
                    sensorY+=(rawY-sensorY)*smoothing
                }
                override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
            }
            if(sensor!=null)manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_GAME)
            onDispose{
                runCatching{manager?.unregisterListener(listener)}
                sensorX=0f
                sensorY=0f
            }
        }
    }
    val idleSlot=if(avatar=="FEMALE")"ai_trainer_female" else "ai_trainer_male"
    val speakingSlot=if(avatar=="FEMALE")"ai_trainer_female_speaking" else "ai_trainer_male_speaking"
    val speakingVisual=if(speaking)rsVisualUriWithBundledFallbackV113(context,store,speakingSlot) else ""
    val visual=if(speakingVisual.isNotBlank())speakingVisual
        else rsVisualUriWithBundledFallbackV113(context,store,idleSlot)
    val themeLayout=rsThemeLayoutV175(rsStoredThemeV175(store))
    val transition=rememberInfiniteTransition(label="ai-stage")
    val breath by transition.animateFloat(
        initialValue=.992f,
        targetValue=1.012f,
        animationSpec=infiniteRepeatable(tween(2100,easing=FastOutSlowInEasing),RepeatMode.Reverse),
        label="ai-breath"
    )
    val floatY by transition.animateFloat(
        initialValue=-2f,
        targetValue=3f,
        animationSpec=infiniteRepeatable(tween(2500,easing=FastOutSlowInEasing),RepeatMode.Reverse),
        label="ai-float"
    )
    val parallaxX by transition.animateFloat(
        initialValue=-5f,
        targetValue=5f,
        animationSpec=infiniteRepeatable(tween(4200,easing=FastOutSlowInEasing),RepeatMode.Reverse),
        label="ai-parallax"
    )
    val headTurn by transition.animateFloat(
        initialValue=-1.8f,
        targetValue=1.8f,
        animationSpec=infiniteRepeatable(tween(5200,easing=FastOutSlowInEasing),RepeatMode.Reverse),
        label="ai-head-turn"
    )
    val focusTilt by transition.animateFloat(
        initialValue=-.8f,
        targetValue=.8f,
        animationSpec=infiniteRepeatable(tween(3900,easing=FastOutSlowInEasing),RepeatMode.Reverse),
        label="ai-focus-tilt"
    )
    val lightSweep by transition.animateFloat(
        initialValue=.10f,
        targetValue=.32f,
        animationSpec=infiniteRepeatable(tween(1800,easing=LinearEasing),RepeatMode.Reverse),
        label="ai-light-sweep"
    )
    val aura by transition.animateFloat(
        initialValue=.22f,
        targetValue=if(speaking).62f else .38f,
        animationSpec=infiniteRepeatable(tween(if(speaking)620 else 1500,easing=FastOutSlowInEasing),RepeatMode.Reverse),
        label="ai-aura"
    )
    val stateScale by animateFloatAsState(
        targetValue=when{
            speaking->1.035f
            listening->1.020f
            thinking->1.012f
            else->1f
        },
        animationSpec=tween(420,easing=FastOutSlowInEasing),
        label="ai-state-scale"
    )
    val stateLift by animateFloatAsState(
        targetValue=when{
            speaking->-5f
            listening->-2f
            thinking->1f
            else->0f
        },
        animationSpec=tween(520,easing=FastOutSlowInEasing),
        label="ai-state-lift"
    )
    val stageGlow by animateFloatAsState(
        targetValue=when{
            speaking->.80f
            listening->.56f
            thinking->.48f
            else->.30f
        },
        animationSpec=tween(360,easing=FastOutSlowInEasing),
        label="ai-stage-glow"
    )
    var languageMenu by remember{mutableStateOf(false)}

    Surface(
        color=Color.Black,
        shape=RoundedCornerShape(themeLayout.panelRadius.dp),
        border=BorderStroke(
            if(themeLayout.strongLines)2.dp else 1.dp,
            if(speaking)Color(0xFF58C9FF).copy(alpha=.72f) else c.gold.copy(alpha=.46f)
        ),
        tonalElevation=18.dp,
        modifier=modifier
    ){
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(
                        c.bright.copy(alpha=aura*.42f),
                        c.gold.copy(alpha=.10f),
                        Color(0xFF07090C),
                        Color.Black
                    )
                )
            )
        ){
            // Rear depth layer: gives the assistant a cinematic 3D stage even
            // when the trainer uses a normal portrait asset.
            Box(
                Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=14.dp)
                    .clip(RoundedCornerShape((themeLayout.panelRadius+8).dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha=.035f),Color.Transparent,Color.Black.copy(alpha=.32f))
                        )
                    )
            )
            Box(
                Modifier.fillMaxSize().graphicsLayer{
                    scaleX=(if(avatarMotion)breath*1.035f else 1.035f)*stateScale
                    scaleY=(if(avatarMotion)breath*1.035f else 1.035f)*stateScale
                    translationY=(if(avatarMotion)floatY else 0f)+stateLift+
                        (if(immersive && sensorParallaxEnabled) sensorY*10f*density else 0f)
                    translationX=(if(avatarMotion && renderMode=="CINEMATIC_3D")parallaxX else 0f)+
                        (if(immersive && sensorParallaxEnabled) -sensorX*13f*density else 0f)
                    rotationY=(if(avatarMotion && renderMode=="CINEMATIC_3D")headTurn else 0f)+
                        (if(immersive && sensorParallaxEnabled) sensorX*3.5f else 0f)
                    rotationX=(if(avatarMotion && renderMode=="CINEMATIC_3D")focusTilt else 0f)+
                        (if(immersive && sensorParallaxEnabled) -sensorY*2.4f else 0f)
                    cameraDistance=if(renderMode=="CINEMATIC_3D")18f*density else 8f*density
                    shadowElevation=if(renderMode=="CINEMATIC_3D")24f else 0f
                }
            ){
                if(visual.isNotBlank()){
                    RsUriPreviewV21(visual,Modifier.fillMaxSize(),"CENTER")
                }else{
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xFF10131A),Color.Black))
                        )
                    )
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha=.04f),
                            Color.Transparent,
                            c.gold.copy(alpha=.05f),
                            Color.Black.copy(alpha=.78f)
                        )
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        listOf(
                            when{
                                speaking->Color(0xFF58C9FF).copy(alpha=stageGlow*.20f)
                                listening->Color(0xFF7EE8B5).copy(alpha=stageGlow*.16f)
                                thinking->c.gold.copy(alpha=stageGlow*.15f)
                                else->Color.Transparent
                            },
                            Color.Transparent
                        ),
                        radius=900f
                    )
                )
            )
            if(renderMode=="CINEMATIC_3D"){
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                c.bright.copy(alpha=lightSweep*.18f),
                                Color.Transparent,
                                c.gold.copy(alpha=lightSweep*.12f),
                                Color.Transparent
                            )
                        )
                    )
                )
                Box(
                    Modifier.fillMaxWidth(.46f).height(1.dp)
                        .align(Alignment.Center)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent,c.bright.copy(alpha=.26f),Color.Transparent)
                            )
                        )
                )
            }

            // Speaking pulse / holographic depth rings.
            if(renderMode=="CINEMATIC_3D"){
                Surface(
                    color=if(speaking)Color(0xFF58C9FF).copy(alpha=aura*.30f) else c.gold.copy(alpha=aura*.18f),
                    shape=CircleShape,
                    border=BorderStroke(1.dp,if(speaking)Color(0xFF58C9FF).copy(alpha=.46f) else c.bright.copy(alpha=.30f)),
                    modifier=Modifier.align(Alignment.BottomCenter)
                        .padding(bottom=44.dp)
                        .fillMaxWidth(.72f)
                        .height(42.dp)
                        .graphicsLayer{scaleX=1f+aura*.08f;scaleY=.44f}
                ){}
            }

            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().padding(9.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ){
                Box{
                    AssistChip(
                        onClick={languageMenu=true},
                        label={Text(language.name,fontSize=8.sp)},
                        leadingIcon={Text("◎",fontSize=11.sp)}
                    )
                    DropdownMenu(expanded=languageMenu,onDismissRequest={languageMenu=false}){
                        languages.forEach{item->
                            DropdownMenuItem(
                                text={Text(item.name)},
                                onClick={languageMenu=false;onLanguageChange(item)}
                            )
                        }
                    }
                }
                AssistChip(
                    onClick={},
                    enabled=false,
                    label={Text(
                        when(language.code){
                            "nl"->if(renderMode=="CINEMATIC_3D")"3D VIRTUELE ASSISTENT" else "AI-ASSISTENT"
                            "pt"->if(renderMode=="CINEMATIC_3D")"ASSISTENTE VIRTUAL 3D" else "ASSISTENTE IA"
                            "es"->if(renderMode=="CINEMATIC_3D")"ASISTENTE VIRTUAL 3D" else "ASISTENTE IA"
                            "fr"->if(renderMode=="CINEMATIC_3D")"ASSISTANT VIRTUEL 3D" else "ASSISTANT IA"
                            "de"->if(renderMode=="CINEMATIC_3D")"3D VIRTUELLER ASSISTENT" else "KI-ASSISTENT"
                            "it"->if(renderMode=="CINEMATIC_3D")"ASSISTENTE VIRTUALE 3D" else "ASSISTENTE IA"
                            "pl"->if(renderMode=="CINEMATIC_3D")"WIRTUALNY ASYSTENT 3D" else "ASYSTENT AI"
                            "tr"->if(renderMode=="CINEMATIC_3D")"3D SANAL ASİSTAN" else "YZ ASİSTANI"
                            else->if(renderMode=="CINEMATIC_3D")"3D VIRTUAL ASSISTANT" else "AI ASSISTANT"
                        },
                        fontSize=7.sp,
                        fontWeight=FontWeight.Black
                    )}
                )
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick=onOptions,
                    label={Text("⋮",fontSize=17.sp,fontWeight=FontWeight.Black)}
                )
            }

            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .padding(start=11.dp,end=11.dp,top=11.dp,bottom=if(immersive)82.dp else 11.dp),
                verticalAlignment=Alignment.Bottom
            ){
                Column(Modifier.weight(1f)){
                    Text(
                        if(avatar=="FEMALE")"SOFIA" else "MARCUS",
                        color=Color.White,fontWeight=FontWeight.Black,fontSize=21.sp,letterSpacing=1.sp
                    )
                    val assistantState=when(language.code){
                        "nl"->when{speaking->"SPREKEN";thinking->"DENKEN";listening->"LUISTEREN";else->"KLAAR"}
                        "pt"->when{speaking->"A FALAR";thinking->"A PENSAR";listening->"A OUVIR";else->"PRONTO"}
                        "es"->when{speaking->"HABLANDO";thinking->"PENSANDO";listening->"ESCUCHANDO";else->"LISTO"}
                        "fr"->when{speaking->"PARLE";thinking->"RÉFLÉCHIT";listening->"ÉCOUTE";else->"PRÊT"}
                        "de"->when{speaking->"SPRICHT";thinking->"DENKT";listening->"HÖRT ZU";else->"BEREIT"}
                        "it"->when{speaking->"PARLA";thinking->"PENSA";listening->"ASCOLTA";else->"PRONTO"}
                        "pl"->when{speaking->"MÓWI";thinking->"MYŚLI";listening->"SŁUCHA";else->"GOTOWY"}
                        "tr"->when{speaking->"KONUŞUYOR";thinking->"DÜŞÜNÜYOR";listening->"DİNLİYOR";else->"HAZIR"}
                        else->when{speaking->"SPEAKING";thinking->"THINKING";listening->"LISTENING";else->"READY"}
                    }
                    val stageMode=when(language.code){
                        "nl"->if(renderMode=="CINEMATIC_3D")"CINEMATISCHE 3D BETA" else "AI-ASSISTENT"
                        "pt"->if(renderMode=="CINEMATIC_3D")"3D CINEMÁTICO BETA" else "ASSISTENTE IA"
                        "es"->if(renderMode=="CINEMATIC_3D")"3D CINEMÁTICO BETA" else "ASISTENTE IA"
                        "fr"->if(renderMode=="CINEMATIC_3D")"3D CINÉMATIQUE BÊTA" else "ASSISTANT IA"
                        "de"->if(renderMode=="CINEMATIC_3D")"KINEMATISCHE 3D-BETA" else "KI-ASSISTENT"
                        "it"->if(renderMode=="CINEMATIC_3D")"3D CINEMATICO BETA" else "ASSISTENTE IA"
                        "pl"->if(renderMode=="CINEMATIC_3D")"KINOWE 3D BETA" else "ASYSTENT AI"
                        "tr"->if(renderMode=="CINEMATIC_3D")"SİNEMATİK 3D BETA" else "YZ ASİSTANI"
                        else->if(renderMode=="CINEMATIC_3D")"CINEMATIC 3D BETA" else "AI ASSISTANT"
                    }
                    Text(
                        assistantState+" · "+stageMode,
                        color=when{
                            speaking->Color(0xFF58C9FF)
                            thinking->c.gold
                            listening->Color(0xFF7EE8B5)
                            else->Color(0xFF36D27F)
                        },
                        fontSize=8.sp,fontWeight=FontWeight.Black
                    )
                }
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    FilterChip(
                        selected=avatar=="FEMALE",
                        onClick={onAvatarChange("FEMALE")},
                        label={Text("Sofia",fontSize=8.sp)}
                    )
                    FilterChip(
                        selected=avatar=="MALE",
                        onClick={onAvatarChange("MALE")},
                        label={Text("Marcus",fontSize=8.sp)}
                    )
                }
            }
        }
    }
}

package com.rskickbox.app

import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RsAiChatMessageV163(
    val id:Long,
    val mine:Boolean,
    val text:String,
    val mediaUri:String?=null,
    val mediaKind:String?=null,
    val references:List<RsAiCoachReferenceV164> = emptyList()
)

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
    var messages by remember{mutableStateOf<List<RsAiChatMessageV163>>(emptyList())}
    var draft by remember{mutableStateOf("")}
    var pickedUri by remember{mutableStateOf<Uri?>(null)}
    var pickedKind by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var speaking by remember{mutableStateOf(false)}
    var attachMenu by remember{mutableStateOf(false)}
    var optionsMenu by remember{mutableStateOf(false)}
    var trainerReferences by remember{mutableStateOf(false)}
    var autoSpeak by remember{mutableStateOf(store.b("ai_auto_speak_v163",true))}
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

    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ttsReady by remember{mutableStateOf(false)}
    DisposableEffect(Unit){
        val engine=TextToSpeech(context){state->ttsReady=state==TextToSpeech.SUCCESS}
        engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(utteranceId:String?){scope.launch{speaking=true}}
            override fun onDone(utteranceId:String?){
                scope.launch{
                    speaking=false
                    if(voiceConversationActive)resumeListeningSignal++
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId:String?){
                scope.launch{
                    speaking=false
                    if(voiceConversationActive)resumeListeningSignal++
                }
            }
        })
        tts=engine
        onDispose{runCatching{engine.stop()};runCatching{engine.shutdown()}}
    }
    fun applySelectedVoice(){
        val engine=tts?:return
        engine.language=selectedLang.locale
        val matching=engine.voices
            ?.filter{it.locale.language.equals(selectedLang.locale.language,true)}
            ?.sortedWith(
                compareByDescending<android.speech.tts.Voice>{!it.isNetworkConnectionRequired}
                    .thenByDescending{it.quality}
            )
            ?.firstOrNull()
        if(matching!=null)engine.voice=matching
    }

    LaunchedEffect(aiLang,ttsReady){
        if(ttsReady)applySelectedVoice()
    }

    fun speak(text:String){
        if(!ttsReady||text.isBlank())return
        applySelectedVoice()
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"rs-ai-chat")
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
                .onSuccess{
                    pendingMusic=it
                    musicDialog=true
                    status=it.name
                }
                .onFailure{status=it.message?:"Could not open music file."}
        }
    }
    val quietVoice=remember(context){
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

    fun startVoice(){
        if(speaking)runCatching{tts?.stop()}
        speaking=false
        voiceConversationActive=true
        quietVoice.start(selectedLang.locale)
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
        messages=(messages+userMessage).takeLast(30)
        draft=""
        pickedUri=null
        pickedKind=null

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
            )).takeLast(30)
            busy=false
            status=""
            if(autoSpeak)speak(reply)
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
            )).takeLast(30)
            busy=false
            status=""
            if(autoSpeak && reply.isNotBlank())speak(reply)
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

            val referenceSummary=earlyRefs.joinToString("\n"){ref->
                ref.item.title+" | tags: "+ref.item.techniqueTags.joinToString(", ")+" | trainer note: "+ref.item.description
            }

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
            )).takeLast(30)
            status=""
            busy=false
            if(autoSpeak)speak(reply)
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
            kotlinx.coroutines.delay(320)
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
            )).takeLast(30)
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
        Modifier.fillMaxSize().padding(horizontal=6.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ){
        RsAiAvatarStageV163(
            c=c,
            store=store,
            avatar=avatar,
            speaking=speaking,
            language=selectedLang,
            onAvatarChange={
                avatar=it
                store.ps("ai_avatar_gender_v161",it)
            },
            onLanguageChange={
                aiLang=it.code
                store.ps("ai_voice_language_v161",it.code)
            },
            languages=languages,
            onOptions={optionsMenu=true}
        )

        Box(Modifier.fillMaxWidth().weight(1f)){
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement=Arrangement.Bottom
            ){
                messages.takeLast(6).forEach{message->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical=2.dp),
                        horizontalArrangement=if(message.mine)Arrangement.End else Arrangement.Start
                    ){
                        Surface(
                            color=if(message.mine)c.gold.copy(alpha=.16f) else Color(0xFF58C9FF).copy(alpha=.08f),
                            shape=RoundedCornerShape(18.dp),
                            border=BorderStroke(
                                1.dp,
                                if(message.mine)c.gold.copy(alpha=.24f) else Color(0xFF58C9FF).copy(alpha=.22f)
                            ),
                            modifier=Modifier.fillMaxWidth(.86f)
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
                                                text={Text(rsAiExtraV163(aiLang,"delete_chat"))},
                                                onClick={
                                                    aiMessageMenuId=null
                                                    messages=messages.filterNot{it.id==message.id}
                                                }
                                            )
                                        }
                                    }
                                }
                                if(message.mediaUri!=null){
                                    if(message.mediaKind=="IMAGE"){
                                        RsUriPreviewV21(
                                            message.mediaUri,
                                            Modifier.fillMaxWidth().heightIn(min=70.dp,max=120.dp),
                                            "CENTER"
                                        )
                                    }else if(message.mediaKind=="VIDEO"){
                                        RsMiniLocalVideoV163(
                                            message.mediaUri,
                                            Modifier.fillMaxWidth().height(96.dp)
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
                                            Text(
                                                "TRAINER REFERENCE · "+ref.item.title,
                                                color=c.gold,fontWeight=FontWeight.Black,fontSize=8.sp
                                            )
                                            if(ref.item.kind=="VIDEO"){
                                                RsMiniLocalVideoV163(ref.localUri,Modifier.fillMaxWidth().height(105.dp))
                                            }else{
                                                RsUriPreviewV21(ref.localUri,Modifier.fillMaxWidth().height(105.dp),"CENTER")
                                            }
                                            if(ref.item.description.isNotBlank()){
                                                Text(ref.item.description,color=c.text,fontSize=9.sp,lineHeight=13.sp)
                                            }
                                            if(ref.item.techniqueTags.isNotEmpty()){
                                                Text(
                                                    ref.item.techniqueTags.take(8).joinToString(" · "),
                                                    color=c.muted,fontSize=7.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(messages.isEmpty()){
                    val coachName=if(avatar=="FEMALE")"Sofia" else "Marcus"
                    Text(
                        when(aiLang){
                            "nl"->"Praat met "+coachName+" alsof je met je echte coach praat."
                            "pt"->"Fala com "+coachName+" como falarias com o teu treinador real."
                            "es"->"Habla con "+coachName+" como con tu entrenador real."
                            "fr"->"Parle à "+coachName+" comme à ton vrai coach."
                            else->"Talk to "+coachName+" like you would to a real coach."
                        },
                        color=c.muted,
                        fontSize=9.sp,
                        modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp)
                    )
                }
            }
        }

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
            color=Color.Black.copy(alpha=.92f),
            shape=RoundedCornerShape(28.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.52f)),
            tonalElevation=18.dp,
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
                        focusedBorderColor=c.gold.copy(alpha=.48f),
                        unfocusedBorderColor=c.gold.copy(alpha=.16f),
                        focusedContainerColor=c.panel.copy(alpha=.46f),
                        unfocusedContainerColor=c.panel.copy(alpha=.36f)
                    )
                )

                OutlinedButton(
                    onClick={startVoice()},
                    enabled=!busy,
                    modifier=Modifier.size(44.dp),
                    shape=CircleShape,
                    contentPadding=PaddingValues(0.dp)
                ){Text("🎙",fontSize=15.sp)}

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
                    if(role==RsRole.TRAINER){
                        OutlinedButton(
                            onClick={optionsMenu=false;trainerReferences=true},
                            modifier=Modifier.fillMaxWidth()
                        ){Text(rsAiExtraV163(aiLang,"references"))}
                    }
                    OutlinedButton(onClick={optionsMenu=false},modifier=Modifier.fillMaxWidth()){Text(rsAiExtraV163(aiLang,"close"))}
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
                    Text("RS AI MUSIC",color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp)
                    Text(track.name,color=c.text,fontSize=11.sp,maxLines=2)
                    Text(
                        "Choose what RS should do with this music.",
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
                        ){Text("▶ Play",fontSize=9.sp)}

                        OutlinedButton(
                            onClick={
                                rsSaveMusicTrackV169(store,track)
                                    .onSuccess{status="Saved to RS Music."}
                                    .onFailure{status=it.message?:"Could not save music."}
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("☆ Save",fontSize=9.sp)}
                    }

                    if(playlists.isNotEmpty()){
                        Box{
                            OutlinedButton(
                                onClick={playlistMenu=true},
                                modifier=Modifier.fillMaxWidth()
                            ){Text("Add to existing playlist",fontSize=9.sp)}
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
                        label={Text("New playlist name")},
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
                    ){Text("Create playlist with this music")}

                    TextButton(
                        onClick={
                            musicDialog=false
                            pendingMusic=null
                            newPlaylistName=""
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text("Close")}
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
    language:RsLang,
    onAvatarChange:(String)->Unit,
    onLanguageChange:(RsLang)->Unit,
    languages:List<RsLang>,
    onOptions:()->Unit
){
    val context=LocalContext.current
    val idleSlot=if(avatar=="FEMALE")"ai_trainer_female" else "ai_trainer_male"
    val speakingSlot=if(avatar=="FEMALE")"ai_trainer_female_speaking" else "ai_trainer_male_speaking"
    val speakingVisual=if(speaking)rsVisualUriWithBundledFallbackV113(context,store,speakingSlot) else ""
    val visual=if(speakingVisual.isNotBlank())speakingVisual
        else rsVisualUriWithBundledFallbackV113(context,store,idleSlot)
    val avatarMotion=store.b("ai_avatar_motion_v163",true)
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
    var languageMenu by remember{mutableStateOf(false)}

    Surface(
        color=Color.Black,
        shape=RoundedCornerShape(26.dp),
        border=BorderStroke(1.dp,if(speaking)Color(0xFF58C9FF).copy(alpha=.58f) else c.gold.copy(alpha=.34f)),
        modifier=Modifier.fillMaxWidth().height(300.dp)
    ){
        Box(Modifier.fillMaxSize()){
            Box(
                Modifier.fillMaxSize().graphicsLayer{
                    scaleX=if(avatarMotion)breath else 1f
                    scaleY=if(avatarMotion)breath else 1f
                    translationY=if(avatarMotion)floatY else 0f
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
                        listOf(Color.Black.copy(alpha=.06f),Color.Transparent,Color.Black.copy(alpha=.74f))
                    )
                )
            )

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
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick=onOptions,
                    label={Text("⋮",fontSize=17.sp,fontWeight=FontWeight.Black)}
                )
            }

            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(11.dp),
                verticalAlignment=Alignment.Bottom
            ){
                Column(Modifier.weight(1f)){
                    Text(
                        if(avatar=="FEMALE")"SOFIA" else "MARCUS",
                        color=Color.White,fontWeight=FontWeight.Black,fontSize=21.sp,letterSpacing=1.sp
                    )
                    Text(
                        if(speaking)rsAiExtraV163(language.code,"live_speaking") else rsAiExtraV163(language.code,"live_ready"),
                        color=if(speaking)Color(0xFF58C9FF) else Color(0xFF36D27F),
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

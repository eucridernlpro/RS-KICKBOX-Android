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
import kotlinx.coroutines.launch

private data class RsAiChatMessageV163(
    val id:Long,
    val mine:Boolean,
    val text:String,
    val mediaUri:String?=null,
    val mediaKind:String?=null
)

@Composable
fun RsAiAssistantChatV163(
    c:RsPalette,
    lang:RsLang,
    store:RsStore,
    role:RsRole
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

    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ttsReady by remember{mutableStateOf(false)}
    DisposableEffect(Unit){
        val engine=TextToSpeech(context){state->ttsReady=state==TextToSpeech.SUCCESS}
        engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(utteranceId:String?){scope.launch{speaking=true}}
            override fun onDone(utteranceId:String?){scope.launch{speaking=false}}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId:String?){scope.launch{speaking=false}}
        })
        tts=engine
        onDispose{runCatching{engine.stop()};runCatching{engine.shutdown()}}
    }
    LaunchedEffect(aiLang,ttsReady){
        if(ttsReady)tts?.language=selectedLang.locale
    }

    fun speak(text:String){
        if(!ttsReady||text.isBlank())return
        tts?.language=selectedLang.locale
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"rs-ai-chat")
    }

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            pickedUri=uri
            pickedKind="IMAGE"
            attachMenu=false
        }
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            pickedUri=uri
            pickedKind="VIDEO"
            attachMenu=false
        }
    }
    val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            val spoken=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if(spoken.isNotBlank()){
                draft=spoken.take(1200)
                status=rsAiUiV162(aiLang,"voice_ready")
            }
        }
    }

    fun startVoice(){
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,selectedLang.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT,rsAiUiV162(aiLang,"ask_voice"))
        }
        runCatching{voiceLauncher.launch(intent)}
            .onFailure{status=rsAiUiV162(aiLang,"voice_unavailable")}
    }

    fun send(){
        val body=draft.trim()
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

        scope.launch{
            val reply=when{
                kind=="VIDEO"&&media!=null->{
                    status=if(aiLang=="nl")"Techniekvideo analyseren…" else "Analyzing technique video…"
                    rsAnalyzeTechniqueVisionV106(
                        context,
                        media.toString(),
                        if(body.isBlank())"Uploaded kickboxing technique" else body,
                        selectedLang
                    ).getOrElse{rsAiAnswerV162(aiLang,body.ifBlank{"kickboxing technique"})}
                }
                kind=="IMAGE"&&media!=null->when(aiLang){
                    "nl"->"Ik heb de afbeelding ontvangen. Beschrijf de techniek of het detail dat je wilt laten beoordelen."
                    "pt"->"Recebi a imagem. Diz qual técnica ou detalhe queres que eu analise."
                    "es"->"He recibido la imagen. Dime qué técnica o detalle quieres que revise."
                    "fr"->"J’ai reçu l’image. Dis-moi quelle technique ou quel détail tu veux analyser."
                    "de"->"Ich habe das Bild erhalten. Sag mir, welche Technik oder welches Detail ich prüfen soll."
                    "it"->"Ho ricevuto l’immagine. Dimmi quale tecnica o dettaglio vuoi analizzare."
                    "pl"->"Otrzymałem obraz. Powiedz, którą technikę lub szczegół mam przeanalizować."
                    "tr"->"Görseli aldım. Hangi teknik veya ayrıntıyı incelememi istediğini söyle."
                    else->"I received the image. Tell me which technique or detail you want me to review."
                }
                else->rsAiAnswerV162(aiLang,body)
            }
            messages=(messages+RsAiChatMessageV163(
                id=System.currentTimeMillis()+1,
                mine=false,
                text=reply
            )).takeLast(30)
            status=""
            busy=false
            if(autoSpeak)speak(reply)
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
                            if(pickedKind=="VIDEO")"VIDEO READY" else "IMAGE READY",
                            color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp
                        )
                        Text(
                            if(pickedKind=="VIDEO")"Compact preview · ready for AI analysis" else "Ready to send to AI coach",
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
                    Text("AI ASSISTANT SETTINGS",color=c.bright,fontWeight=FontWeight.Black)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("Speak AI replies automatically",color=c.text,fontSize=10.sp,modifier=Modifier.weight(1f))
                        Switch(autoSpeak,{
                            autoSpeak=it
                            store.pb("ai_auto_speak_v163",it)
                        })
                    }
                    if(role==RsRole.TRAINER){
                        OutlinedButton(
                            onClick={optionsMenu=false;trainerReferences=true},
                            modifier=Modifier.fillMaxWidth()
                        ){Text("Trainer Reference Gallery")}
                    }
                    OutlinedButton(onClick={optionsMenu=false},modifier=Modifier.fillMaxWidth()){Text("Close")}
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
                    Text("AI MEDIA",color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp)
                    Text("Choose what to send to your AI coach",color=c.muted,fontSize=9.sp)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(
                            onClick={
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("▣  Photo",fontSize=9.sp)}
                        OutlinedButton(
                            onClick={
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            modifier=Modifier.weight(1f)
                        ){Text("▶  Video",fontSize=9.sp)}
                    }
                    OutlinedButton(
                        onClick={attachMenu=false;startVoice()},
                        modifier=Modifier.fillMaxWidth()
                    ){Text("🎙  Voice",fontSize=9.sp)}
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
    val slot=if(avatar=="FEMALE")"ai_trainer_female" else "ai_trainer_male"
    val visual=rsVisualUriWithBundledFallbackV113(context,store,slot)
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
                    scaleX=breath
                    scaleY=breath
                    translationY=floatY
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
                        if(speaking)"●  LIVE · SPEAKING" else "●  LIVE · READY",
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

package com.rskickbox.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

private data class RsAiChatMessageV163(
    val fromAi:Boolean,
    val text:String,
    val mediaUri:String="",
    val mediaKind:String=""
)

@Composable
fun RsAiRoyalChatV163(
    c:RsPalette,
    store:RsStore,
    appLang:RsLang,
    role:RsRole
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var avatar by remember{mutableStateOf(store.s("ai_avatar_gender_v161","FEMALE").ifBlank{"FEMALE"})}
    var aiLanguage by remember{
        mutableStateOf(
            store.s("ai_voice_language_v161",appLang.code)
                .takeIf{saved->rsLangs.any{it.code==saved}}
                ?:appLang.code
        )
    }
    val selectedLang=rsLangs.firstOrNull{it.code==aiLanguage}?:appLang
    var messages by remember{
        mutableStateOf(
            listOf(
                RsAiChatMessageV163(
                    true,
                    when(selectedLang.code){
                        "nl"->"Ik ben klaar. Vraag iets over je training of stuur een korte techniekvideo."
                        "pt"->"Estou pronto. Pergunta sobre o teu treino ou envia um vídeo técnico curto."
                        "es"->"Estoy listo. Pregunta sobre tu entrenamiento o envía un vídeo técnico corto."
                        "fr"->"Je suis prêt. Pose une question sur ton entraînement ou envoie une courte vidéo technique."
                        "de"->"Ich bin bereit. Frag mich zu deinem Training oder sende ein kurzes Technikvideo."
                        "it"->"Sono pronto. Chiedi del tuo allenamento o invia un breve video tecnico."
                        "pl"->"Jestem gotowy. Zapytaj o trening lub wyślij krótki film techniczny."
                        "tr"->"Hazırım. Antrenmanın hakkında sor veya kısa bir teknik videosu gönder."
                        else->"I'm ready. Ask about your training or send a short technique video."
                    }
                )
            )
        )
    }
    var draft by remember{mutableStateOf("")}
    var pickedUri by remember{mutableStateOf("")}
    var pickedKind by remember{mutableStateOf("")}
    var attachmentMenu by remember{mutableStateOf(false)}
    var languageMenu by remember{mutableStateOf(false)}
    var pageMenu by remember{mutableStateOf(false)}
    var speaking by remember{mutableStateOf(false)}
    var analyzing by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var trainerReferences by remember{mutableStateOf(false)}

    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ttsReady by remember{mutableStateOf(false)}
    DisposableEffect(Unit){
        val engine=TextToSpeech(context){statusCode->
            ttsReady=statusCode==TextToSpeech.SUCCESS
        }
        tts=engine
        onDispose{
            runCatching{engine.stop()}
            runCatching{engine.shutdown()}
        }
    }
    LaunchedEffect(aiLanguage,ttsReady){
        if(ttsReady)tts?.language=selectedLang.locale
    }

    fun speak(text:String){
        if(!ttsReady||text.isBlank())return
        speaking=true
        tts?.language=selectedLang.locale
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"rs-ai-v163")
        scope.launch{
            kotlinx.coroutines.delay((text.length*42L).coerceIn(900L,7000L))
            speaking=false
        }
    }

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            pickedUri=uri.toString()
            pickedKind="IMAGE"
            status=""
        }
    }
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            pickedUri=uri.toString()
            pickedKind="VIDEO"
            status=""
        }
    }
    val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            val spoken=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if(spoken.isNotBlank())draft=spoken.take(1200)
        }
    }

    fun beginVoice(){
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,selectedLang.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT,rsAiUiV162(aiLanguage,"ask_voice"))
        }
        runCatching{voiceLauncher.launch(intent)}
            .onFailure{status=rsAiUiV162(aiLanguage,"voice_unavailable")}
    }

    if(trainerReferences && role==RsRole.TRAINER){
        Column(Modifier.fillMaxSize()){
            Row(
                Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=5.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                OutlinedButton(onClick={trainerReferences=false}){Text("‹  AI")}
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
        Modifier.fillMaxSize(),
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ){
        // Clean AI header.
        Surface(
            color=Color.Black.copy(alpha=.78f),
            shape=RoundedCornerShape(22.dp),
            border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.25f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Row(
                Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ){
                Column(Modifier.weight(1f)){
                    Text(
                        if(avatar=="FEMALE")"SOFIA" else "MARCUS",
                        color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp,letterSpacing=.8.sp
                    )
                    Text(
                        rsAiUiV162(aiLanguage,"title")+" · "+selectedLang.name,
                        color=Color(0xFF58C9FF),fontSize=8.sp,fontWeight=FontWeight.Bold
                    )
                }

                FilterChip(
                    selected=avatar=="FEMALE",
                    onClick={avatar="FEMALE";store.ps("ai_avatar_gender_v161",avatar)},
                    label={Text("S",fontSize=8.sp)}
                )
                FilterChip(
                    selected=avatar=="MALE",
                    onClick={avatar="MALE";store.ps("ai_avatar_gender_v161",avatar)},
                    label={Text("M",fontSize=8.sp)}
                )

                Box{
                    OutlinedButton(
                        onClick={languageMenu=true},
                        shape=CircleShape,
                        modifier=Modifier.size(38.dp),
                        contentPadding=PaddingValues(0.dp)
                    ){Text(selectedLang.code.uppercase(),fontSize=8.sp,fontWeight=FontWeight.Black)}
                    DropdownMenu(expanded=languageMenu,onDismissRequest={languageMenu=false}){
                        rsLangs.forEach{x->
                            DropdownMenuItem(
                                text={Text(x.name)},
                                onClick={
                                    aiLanguage=x.code
                                    store.ps("ai_voice_language_v161",x.code)
                                    languageMenu=false
                                }
                            )
                        }
                    }
                }

                Box{
                    OutlinedButton(
                        onClick={pageMenu=true},
                        shape=CircleShape,
                        modifier=Modifier.size(38.dp),
                        contentPadding=PaddingValues(0.dp)
                    ){Text("⋮",fontSize=19.sp,fontWeight=FontWeight.Black)}
                    DropdownMenu(expanded=pageMenu,onDismissRequest={pageMenu=false}){
                        DropdownMenuItem(
                            text={Text(rsAiUiV162(aiLanguage,"analysis"))},
                            onClick={
                                pageMenu=false
                                attachmentMenu=true
                            }
                        )
                        if(role==RsRole.TRAINER){
                            DropdownMenuItem(
                                text={Text("Trainer References")},
                                onClick={pageMenu=false;trainerReferences=true}
                            )
                        }
                        DropdownMenuItem(
                            text={Text("Clear AI conversation")},
                            onClick={
                                pageMenu=false
                                messages=emptyList()
                                pickedUri=""
                                pickedKind=""
                            }
                        )
                    }
                }
            }
        }

        // Persistent realistic assistant stage.
        val avatarSlot=if(avatar=="FEMALE")"ai_trainer_female" else "ai_trainer_male"
        val avatarVisual=rsVisualUriWithBundledFallbackV113(context,store,avatarSlot)
        val motion=rememberInfiniteTransition(label="aiAvatarMotion")
        val breathe by motion.animateFloat(
            initialValue=.992f,
            targetValue=1.012f,
            animationSpec=infiniteRepeatable(tween(if(speaking)650 else 1800,easing=FastOutSlowInEasing),RepeatMode.Reverse),
            label="breathe"
        )
        val lift by motion.animateFloat(
            initialValue=0f,
            targetValue=if(speaking)-4f else -2f,
            animationSpec=infiniteRepeatable(tween(if(speaking)520 else 1700,easing=FastOutSlowInEasing),RepeatMode.Reverse),
            label="lift"
        )

        Surface(
            color=Color.Black,
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=if(speaking).60f else .24f)),
            modifier=Modifier.fillMaxWidth().height(260.dp)
        ){
            Box(Modifier.fillMaxSize()){
                if(avatarVisual.isNotBlank()){
                    Box(
                        Modifier.fillMaxSize()
                            .graphicsLayer{
                                scaleX=breathe
                                scaleY=breathe
                                translationY=lift
                            }
                    ){
                        RsUriPreviewV21(avatarVisual,Modifier.fillMaxSize(),"CENTER")
                    }
                }else{
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xFF07131A),Color.Black))
                        ),
                        contentAlignment=Alignment.Center
                    ){
                        Text(if(avatar=="FEMALE")"SOFIA" else "MARCUS",color=Color(0xFF58C9FF),fontSize=30.sp,fontWeight=FontWeight.Black)
                    }
                }

                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.75f))
                        )
                    )
                )

                Surface(
                    color=Color.Black.copy(alpha=.70f),
                    shape=RoundedCornerShape(14.dp),
                    border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.32f)),
                    modifier=Modifier.align(Alignment.BottomStart).padding(10.dp)
                ){
                    Text(
                        if(speaking)"●  SPEAKING · "+selectedLang.name else "●  AI READY · "+selectedLang.name,
                        color=if(speaking)Color(0xFF58C9FF) else c.bright,
                        fontSize=8.sp,fontWeight=FontWeight.Black,
                        modifier=Modifier.padding(horizontal=9.dp,vertical=6.dp)
                    )
                }
            }
        }

        // Conversation area only scrolls; assistant + composer stay visible.
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal=4.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ){
            messages.forEach{msg->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=if(msg.fromAi)Arrangement.Start else Arrangement.End
                ){
                    Surface(
                        color=if(msg.fromAi)Color(0xFF58C9FF).copy(alpha=.08f) else c.gold.copy(alpha=.15f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(
                            1.dp,
                            if(msg.fromAi)Color(0xFF58C9FF).copy(alpha=.18f) else c.gold.copy(alpha=.20f)
                        ),
                        modifier=Modifier.fillMaxWidth(.86f)
                    ){
                        Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                            if(msg.mediaUri.isNotBlank()){
                                if(msg.mediaKind=="IMAGE"){
                                    RsUriPreviewV21(msg.mediaUri,Modifier.fillMaxWidth().height(105.dp),"CENTER")
                                }else if(msg.mediaKind=="VIDEO"){
                                    val player=remember(msg.mediaUri){
                                        ExoPlayer.Builder(context).build().apply{
                                            setMediaItem(MediaItem.fromUri(Uri.parse(msg.mediaUri)))
                                            prepare()
                                        }
                                    }
                                    DisposableEffect(player){onDispose{player.release()}}
                                    AndroidView(
                                        factory={ctx->PlayerView(ctx).apply{this.player=player;useController=true}},
                                        update={it.player=player},
                                        modifier=Modifier.fillMaxWidth().height(110.dp)
                                    )
                                }
                            }
                            if(msg.text.isNotBlank()){
                                Text(msg.text,color=c.text,fontSize=11.sp,lineHeight=15.sp)
                            }
                            if(msg.fromAi && msg.text.isNotBlank()){
                                TextButton(
                                    onClick={speak(msg.text)},
                                    contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)
                                ){Text("🔊  "+rsAiUiV162(aiLanguage,"speak"),fontSize=8.sp)}
                            }
                        }
                    }
                }
            }
            if(analyzing){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Start){
                    Surface(
                        color=Color(0xFF58C9FF).copy(alpha=.07f),
                        shape=RoundedCornerShape(16.dp),
                        border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.18f))
                    ){
                        Row(Modifier.padding(9.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
                            CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp)
                            Text("AI analyzing…",color=c.muted,fontSize=9.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Compact selected media preview directly above fixed composer.
        if(pickedUri.isNotBlank()){
            Surface(
                color=Color.Black.copy(alpha=.88f),
                shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Row(
                    Modifier.fillMaxWidth().padding(7.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    Box(Modifier.size(width=92.dp,height=62.dp).clip(RoundedCornerShape(12.dp))){
                        if(pickedKind=="IMAGE"){
                            RsUriPreviewV21(pickedUri,Modifier.fillMaxSize(),"CENTER")
                        }else if(pickedKind=="VIDEO"){
                            val player=remember(pickedUri){
                                ExoPlayer.Builder(context).build().apply{
                                    setMediaItem(MediaItem.fromUri(Uri.parse(pickedUri)))
                                    prepare()
                                }
                            }
                            DisposableEffect(player){onDispose{player.release()}}
                            AndroidView(
                                factory={ctx->PlayerView(ctx).apply{this.player=player;useController=false}},
                                update={it.player=player},
                                modifier=Modifier.fillMaxSize()
                            )
                        }
                    }
                    Column(Modifier.weight(1f)){
                        Text(
                            if(pickedKind=="VIDEO")rsAiUiV162(aiLanguage,"analysis") else "IMAGE",
                            color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp
                        )
                        Text("Ready to send",color=c.muted,fontSize=8.sp)
                    }
                    TextButton(onClick={pickedUri="";pickedKind=""}){Text("×",fontSize=20.sp)}
                }
            }
        }

        if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp,modifier=Modifier.padding(horizontal=6.dp))

        // Fixed royal AI composer.
        Surface(
            color=Color.Black.copy(alpha=.90f),
            shape=RoundedCornerShape(28.dp),
            border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.32f)),
            tonalElevation=16.dp,
            modifier=Modifier.fillMaxWidth()
        ){
            Row(
                Modifier.fillMaxWidth().padding(7.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(6.dp)
            ){
                Box{
                    OutlinedButton(
                        onClick={attachmentMenu=true},
                        modifier=Modifier.size(44.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.55f))
                    ){Text("+",fontSize=23.sp,color=c.bright,fontWeight=FontWeight.Black)}
                    DropdownMenu(expanded=attachmentMenu,onDismissRequest={attachmentMenu=false}){
                        DropdownMenuItem(
                            text={Text("Photo")},
                            onClick={
                                attachmentMenu=false
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        DropdownMenuItem(
                            text={Text("Video / technique")},
                            onClick={
                                attachmentMenu=false
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        )
                        DropdownMenuItem(
                            text={Text("Voice")},
                            onClick={attachmentMenu=false;beginVoice()}
                        )
                    }
                }

                OutlinedTextField(
                    value=draft,
                    onValueChange={draft=it.take(1200)},
                    placeholder={Text(rsAiUiV162(aiLanguage,"question"),fontSize=10.sp)},
                    modifier=Modifier.weight(1f),
                    minLines=1,
                    maxLines=3,
                    shape=RoundedCornerShape(22.dp),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedBorderColor=Color(0xFF58C9FF).copy(alpha=.48f),
                        unfocusedBorderColor=c.gold.copy(alpha=.15f),
                        focusedContainerColor=c.panel.copy(alpha=.45f),
                        unfocusedContainerColor=c.panel.copy(alpha=.36f)
                    )
                )

                OutlinedButton(
                    onClick={beginVoice()},
                    modifier=Modifier.size(42.dp),
                    shape=CircleShape,
                    contentPadding=PaddingValues(0.dp)
                ){Text("🎙",fontSize=14.sp)}

                Button(
                    onClick={
                        if(analyzing)return@Button
                        val userText=draft.trim()
                        val media=pickedUri
                        val kind=pickedKind
                        if(userText.isBlank()&&media.isBlank())return@Button

                        messages=messages+RsAiChatMessageV163(
                            fromAi=false,
                            text=userText,
                            mediaUri=media,
                            mediaKind=kind
                        )
                        draft=""
                        pickedUri=""
                        pickedKind=""

                        scope.launch{
                            if(kind=="VIDEO"&&media.isNotBlank()){
                                analyzing=true
                                status=""
                                val result=rsAnalyzeTechniqueVisionV106(
                                    context,
                                    media,
                                    "Kickboxing technique",
                                    selectedLang
                                )
                                val aiText=result.getOrElse{
                                    rsAiAnswerV162(aiLanguage,userText.ifBlank{"technique"})
                                }
                                messages=messages+RsAiChatMessageV163(true,aiText)
                                analyzing=false
                                if(store.b("voice_auto",true))speak(aiText)
                            }else{
                                val aiText=rsAiAnswerV162(aiLanguage,userText.ifBlank{
                                    if(kind=="IMAGE")"image technique" else "training"
                                })
                                messages=messages+RsAiChatMessageV163(true,aiText)
                                if(store.b("voice_auto",true))speak(aiText)
                            }
                        }
                    },
                    enabled=!analyzing&&(draft.isNotBlank()||pickedUri.isNotBlank()),
                    modifier=Modifier.size(42.dp),
                    shape=CircleShape,
                    contentPadding=PaddingValues(0.dp),
                    colors=ButtonDefaults.buttonColors(
                        containerColor=Color(0xFF58C9FF),
                        contentColor=Color.Black
                    )
                ){Text(if(analyzing)"…" else "➤",fontWeight=FontWeight.Black)}
            }
        }
    }
}

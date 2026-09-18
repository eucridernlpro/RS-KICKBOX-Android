package com.rskickbox.app

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RS_TECH_VIDEO_MAX_MS = 20_000L

private fun rsTechniqueVideoDirV36(context:android.content.Context):File =
    File(context.filesDir,"rs_technique_videos").apply{mkdirs()}

private fun rsCopyTechniqueVideoV36(context:android.content.Context,source:Uri):String{
    val mime=context.contentResolver.getType(source).orEmpty()
    val ext=when{
        mime.contains("webm",true)->"webm"
        mime.contains("quicktime",true)->"mov"
        mime.contains("3gpp",true)->"3gp"
        else->"mp4"
    }
    val out=File(rsTechniqueVideoDirV36(context),"tech_"+UUID.randomUUID()+"."+ext)
    context.contentResolver.openInputStream(source)!!.use{input->
        FileOutputStream(out).use{output->input.copyTo(output)}
    }
    return Uri.fromFile(out).toString()
}

private fun rsDeleteTechniqueVideoV36(context:android.content.Context,uriString:String){
    if(uriString.isBlank())return
    runCatching{
        val uri=Uri.parse(uriString)
        if(uri.scheme!="file")return
        val file=uri.path?.let(::File)?:return
        val root=rsTechniqueVideoDirV36(context).canonicalFile
        val target=file.canonicalFile
        if(target.parentFile==root && target.exists())target.delete()
    }
}

private data class TechniqueSubmissionV27(
    val id:String,val uri:String,val name:String,val technique:String,
    val created:String,val favorite:Boolean,val summary:String
)

private fun encodeTechniqueSubsV27(items:List<TechniqueSubmissionV27>)=items.joinToString("§"){
    listOf(it.id,it.uri,it.name,it.technique,it.created,it.favorite.toString(),it.summary.replace("¤"," ")).joinToString("¤")
}

private fun decodeTechniqueSubsV27(raw:String)=raw.split("§").mapNotNull{row->
    val p=row.split("¤",limit=7)
    if(p.size<7)null else TechniqueSubmissionV27(p[0],p[1],p[2],p[3],p[4],p[5].toBooleanStrictOrNull()?:false,p[6])
}

private fun videoDurationV27(context:android.content.Context,uri:Uri):Long=runCatching{
    val mmr=MediaMetadataRetriever()
    mmr.setDataSource(context,uri)
    val ms=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L
    mmr.release()
    ms
}.getOrDefault(0L)

private fun displayNameV27(context:android.content.Context,uri:Uri):String{
    var name="Technique video"
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())name=cur.getString(0)?:name
        }
    }
    return name
}

private fun coachSummaryV27(lang:RsLang):String=when(lang.code){
    "nl"->"Focus op balans, hoge dekking, gecontroleerde heuprotatie en een snelle terugkeer naar je basispositie. Werk eerst langzaam en technisch zuiver."
    "pt"->"Foca no equilíbrio, guarda alta, rotação controlada da anca e regresso rápido à posição base. Treina primeiro devagar e com boa técnica."
    "es"->"Concéntrate en el equilibrio, guardia alta, rotación controlada de cadera y regreso rápido a la posición base. Practica primero despacio y limpio."
    "fr"->"Travaille l'équilibre, une garde haute, une rotation contrôlée des hanches et un retour rapide en position. Commence lentement et proprement."
    "de"->"Achte auf Balance, hohe Deckung, kontrollierte Hüftrotation und eine schnelle Rückkehr in die Grundstellung. Übe zuerst langsam und sauber."
    "it"->"Concentrati su equilibrio, guardia alta, rotazione controllata dell'anca e rapido ritorno alla posizione base. Lavora prima lentamente e pulito."
    "pl"->"Skup się na równowadze, wysokiej gardzie, kontrolowanej rotacji bioder i szybkim powrocie do pozycji bazowej. Najpierw ćwicz wolno i czysto."
    "tr"->"Dengeye, yüksek garda, kontrollü kalça dönüşüne ve temel duruşa hızlı dönüşe odaklan. Önce yavaş ve temiz çalış."
    else->"Focus on balance, a high guard, controlled hip rotation, and a quick return to your base stance. Train slowly and cleanly before adding speed."
}

private fun techniquePointsV27(lang:RsLang):List<Pair<String,String>> = when(lang.code){
    "nl"->listOf("Basis & balans" to "Voeten stabiel, knieën zacht en gewicht gecentreerd.","Dekking" to "Handen keren direct terug naar het gezicht.","Rotatie" to "Draai heup en schouder samen zonder over te draaien.","Herstel" to "Kom na de techniek direct terug in een sterke positie.")
    "pt"->listOf("Base & equilíbrio" to "Pés estáveis, joelhos soltos e peso centrado.","Guarda" to "As mãos voltam imediatamente ao rosto.","Rotação" to "Anca e ombro rodam juntos sem exagerar.","Recuperação" to "Volta logo a uma posição forte depois da técnica.")
    "es"->listOf("Base & equilibrio" to "Pies estables, rodillas sueltas y peso centrado.","Guardia" to "Las manos vuelven inmediatamente al rostro.","Rotación" to "Cadera y hombro giran juntos sin exceso.","Recuperación" to "Vuelve rápido a una posición fuerte.")
    "fr"->listOf("Base & équilibre" to "Pieds stables, genoux souples et poids centré.","Garde" to "Les mains reviennent immédiatement au visage.","Rotation" to "Hanche et épaule tournent ensemble sans excès.","Retour" to "Reviens vite dans une position forte.")
    else->listOf("Base & balance" to "Stable feet, soft knees, and centered weight.","Guard" to "Hands return immediately to the face after each strike.","Rotation" to "Turn hip and shoulder together without over-rotating.","Recovery" to "Return quickly to a strong stance after the technique.")
}

@Composable
fun RsTechniqueCoachV27(c:RsPalette,lang:RsLang,store:RsStore,role:RsRole){
    if(role==RsRole.TRAINER)TrainerTechniqueHistoryV27(c,store)
    else StudentTechniqueCoachV27(c,lang,store)
}

@Composable
private fun StudentTechniqueCoachV27(c:RsPalette,lang:RsLang,store:RsStore){
    val context=LocalContext.current
    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ready by remember{mutableStateOf(false)}
    var selectedTechnique by remember{mutableStateOf("Roundhouse Kick")}
    var videoUri by remember{mutableStateOf("")}
    var videoName by remember{mutableStateOf("")}
    var durationMs by remember{mutableLongStateOf(0L)}
    var analysisReady by remember{mutableStateOf(false)}
    var feedback by remember{mutableStateOf("")}
    var question by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf("")}
    var autoSpeak by remember{mutableStateOf(store.b("voice_auto",true))}
    var techniqueMenu by remember{mutableStateOf(false)}
    var coachGender by remember{mutableStateOf(store.s("ai_coach_gender","male"))}
    var speaking by remember{mutableStateOf(false)}
    var importingVideo by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()

    DisposableEffect(Unit){
        val engine=TextToSpeech(context){status->ready=status==TextToSpeech.SUCCESS}
        tts=engine
        onDispose{engine.stop();engine.shutdown()}
    }
    LaunchedEffect(lang.code,ready){if(ready)tts?.language=lang.locale}

    fun acceptVideo(uri:Uri,persist:Boolean){
        val d=videoDurationV27(context,uri)
        if(d<=0L)feedback="Could not read this video file."
        else if(d>RS_TECH_VIDEO_MAX_MS)feedback="Video is too long. Technique uploads are limited to 20 seconds."
        else{
            if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            val pickedName=displayNameV27(context,uri)
            importingVideo=true
            feedback="Importing video into private RS KICKBOX storage…"
            scope.launch{
                val local=withContext(Dispatchers.IO){
                    runCatching{rsCopyTechniqueVideoV36(context,uri)}.getOrNull()
                }
                importingVideo=false
                if(local==null){
                    feedback="Could not import this video into private app storage."
                }else{
                    videoUri=local
                    videoName=pickedName
                    durationMs=d
                    analysisReady=false
                    feedback="Video ready for local preview."
                }
            }
        }
    }

    val galleryVideoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null)acceptVideo(uri,false)
    }
    val fileVideoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)acceptVideo(uri,true)
    }

    val speechLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val spoken=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if(!spoken.isNullOrBlank())question=spoken
    }

    val summary=coachSummaryV27(lang)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("AI TECHNIQUE COACH",color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
        Text("Upload a short kickboxing move or combination. Get text, visual and spoken coaching in ${lang.name}.",color=c.muted)

        RsPanel(c){
            Text("YOUR AI TRAINER",color=c.bright,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=coachGender=="male",onClick={coachGender="male";store.ps("ai_coach_gender","male")},label={Text("Male trainer")},modifier=Modifier.weight(1f))
                FilterChip(selected=coachGender=="female",onClick={coachGender="female";store.ps("ai_coach_gender","female")},label={Text("Female trainer")},modifier=Modifier.weight(1f))
            }
            CoachAvatarV28(c,store,coachGender,speaking,selectedTechnique)
            Text("The selected trainer demonstrates correction focus visually and speaks in the active app language. Production lip-synced dubbing can replace the preview voice layer later.",color=c.muted,fontSize=10.sp)
        }

        RsPanel(c){
            Text("1 · VIDEO & TECHNIQUE",color=c.bright,fontWeight=FontWeight.Bold)
            Box{
                OutlinedButton(onClick={techniqueMenu=true},modifier=Modifier.fillMaxWidth()){Text(selectedTechnique)}
                DropdownMenu(expanded=techniqueMenu,onDismissRequest={techniqueMenu=false}){
                    listOf("Jab","Cross","Jab · Cross","Roundhouse Kick","Low Kick","Front Kick","Knee","Defense & Counter","Custom Combination").forEach{t->
                        DropdownMenuItem(text={Text(t)},onClick={selectedTechnique=t;techniqueMenu=false;analysisReady=false})
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Button(
                    onClick={galleryVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))},
                    modifier=Modifier.weight(1f),
                    enabled=!importingVideo
                ){Text(if(importingVideo)"Importing…" else if(videoUri.isBlank())"Gallery" else "Replace")}
                OutlinedButton(
                    onClick={fileVideoPicker.launch(arrayOf("video/*"))},
                    modifier=Modifier.weight(1f),
                    enabled=!importingVideo
                ){Text("Files")}
            }
            Text("Gallery opens first for phone videos · Files is a fallback · maximum 20 seconds",color=c.muted,fontSize=10.sp)
            if(videoUri.isNotBlank()){
                RsTechniqueVideoPreviewV27(videoUri)
                Text("$videoName · ${"%.1f".format(durationMs/1000.0)} s",color=c.muted,fontSize=10.sp)
                Button(onClick={analysisReady=true;feedback="Structured coaching preview generated."},modifier=Modifier.fillMaxWidth()){Text("Analyze technique")}
                OutlinedButton(onClick={rsDeleteTechniqueVideoV36(context,videoUri);videoUri="";videoName="";durationMs=0L;analysisReady=false},modifier=Modifier.fillMaxWidth()){Text("Remove video")}
            }
            if(feedback.isNotBlank())Text(feedback,color=c.muted,fontSize=10.sp)
        }

        if(analysisReady)RsPanel(c){
            Text("2 · COACHING RESULT",color=c.bright,fontWeight=FontWeight.Black)
            Surface(shape=RoundedCornerShape(14.dp),color=c.gold.copy(alpha=.12f)){
                Text("PREVIEW MODE · Production frame-by-frame AI vision is not connected yet.",color=c.bright,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(10.dp))
            }
            Text(summary,color=c.text)
            techniquePointsV27(lang).forEachIndexed{i,(title,body)->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    VisualCueV27(c,i)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(title,color=c.bright,fontWeight=FontWeight.Bold)
                        Text(body,color=c.muted,fontSize=11.sp)
                    }
                }
            }
            Button(onClick={
                if(ready){
                    speaking=true
                    tts?.language=lang.locale
                    tts?.speak(summary,TextToSpeech.QUEUE_FLUSH,null,"technique-coach")
                }
            },enabled=ready,modifier=Modifier.fillMaxWidth()){Text("🔊 Speak coaching")}
            Button(onClick={
                val items=decodeTechniqueSubsV27(store.s("technique_submissions_v27","")).toMutableList()
                val id=System.currentTimeMillis().toString()
                val created=SimpleDateFormat("dd MMM yyyy · HH:mm",Locale.getDefault()).format(Date())
                items.add(0,TechniqueSubmissionV27(id,videoUri,videoName,selectedTechnique,created,false,summary))
                store.ps("technique_submissions_v27",encodeTechniqueSubsV27(items.take(40)))
                feedback="Analysis saved to technique history."
            },modifier=Modifier.fillMaxWidth()){Text("Save to history")}
        }

        RsPanel(c){
            Text("3 · ASK THE COACH",color=c.bright,fontWeight=FontWeight.Bold)
            Text("Voice is an extra input/output layer for the technique coach.",color=c.muted,fontSize=10.sp)
            OutlinedTextField(question,{question=it},label={Text("Ask about this movement")},modifier=Modifier.fillMaxWidth(),minLines=2)
            OutlinedButton(onClick={
                val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang.locale.toLanguageTag())
                }
                speechLauncher.launch(intent)
            },modifier=Modifier.fillMaxWidth()){Text("🎙 Ask by voice")}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Speak replies automatically",color=c.text,modifier=Modifier.weight(1f))
                Switch(autoSpeak,{v->autoSpeak=v;store.pb("voice_auto",v)})
            }
            Button(onClick={
                answer=summary
                if(autoSpeak&&ready){
                    speaking=true
                    tts?.language=lang.locale
                    tts?.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"technique-answer")
                }
            },enabled=question.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Ask AI Coach")}
            if(answer.isNotBlank())Text(answer,color=c.text)
        }
        Spacer(Modifier.height(18.dp))
    }
}


@Composable
private fun CoachAvatarV28(c:RsPalette,store:RsStore,gender:String,speaking:Boolean,technique:String){
    val custom=store.s(if(gender=="female")"visual_v21_ai_trainer_female" else "visual_v21_ai_trainer_male","")
    val pulse=rememberInfiniteTransition(label="coachPulse")
    val glow by pulse.animateFloat(.35f,1f,infiniteRepeatable(tween(850),RepeatMode.Reverse),label="coachGlow")
    Box(
        Modifier.fillMaxWidth().height(230.dp).background(Color.Black,RoundedCornerShape(18.dp)),
        contentAlignment=Alignment.Center
    ){
        if(custom.isNotBlank())RsUriPreviewV21(custom,Modifier.fillMaxSize(), "CENTER")
        Canvas(Modifier.fillMaxSize()){
            val w=size.width; val h=size.height
            drawCircle(c.bright.copy(alpha=.08f+.08f*glow),w*.34f,Offset(w*.5f,h*.44f))
            if(custom.isBlank()){
                val head=Offset(w*.50f,h*.28f)
                drawCircle(Color(0xFF111111),w*.055f,head)
                drawLine(Color(0xFF111111),Offset(w*.50f,h*.34f),Offset(w*.48f,h*.62f),w*.05f)
                drawLine(Color(0xFF111111),Offset(w*.48f,h*.43f),Offset(w*.30f,h*.51f),w*.035f)
                drawLine(Color(0xFF111111),Offset(w*.48f,h*.43f),Offset(w*.70f,h*.35f),w*.035f)
                drawLine(Color(0xFF111111),Offset(w*.48f,h*.61f),Offset(w*.31f,h*.82f),w*.042f)
                drawLine(Color(0xFF111111),Offset(w*.48f,h*.61f),Offset(w*.70f,h*.75f),w*.042f)
            }
            if(speaking)drawCircle(c.bright.copy(alpha=.35f*glow),w*.075f,Offset(w*.5f,h*.31f),style=Stroke(4f))
        }
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=if(custom.isNotBlank()).32f else .08f)))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)){
            Text(if(gender=="female")"RS AI FEMALE TRAINER" else "RS AI MALE TRAINER",color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp)
            Text("Demonstrating focus · $technique",color=Color.White.copy(alpha=.76f),fontSize=10.sp)
        }
    }
}

@Composable
private fun RsTechniqueVideoPreviewV27(uri:String){
    AndroidView(
        factory={ctx->
            VideoView(ctx).apply{
                val controller=MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener{seekTo(1)}
            }
        },
        modifier=Modifier.fillMaxWidth().height(230.dp).background(Color.Black)
    )
}

@Composable
private fun VisualCueV27(c:RsPalette,index:Int){
    Canvas(Modifier.size(58.dp)){
        val w=size.width;val h=size.height
        drawCircle(c.bright.copy(alpha=.16f),w*.46f,Offset(w*.5f,h*.5f),style=Stroke(3f))
        when(index%4){
            0->{drawLine(c.bright,Offset(w*.28f,h*.72f),Offset(w*.72f,h*.72f),5f);drawCircle(c.bright,w*.07f,Offset(w*.5f,h*.32f))}
            1->{drawLine(c.bright,Offset(w*.32f,h*.25f),Offset(w*.68f,h*.25f),5f);drawLine(c.bright,Offset(w*.5f,h*.25f),Offset(w*.5f,h*.72f),5f)}
            2->{drawArc(c.bright,200f,220f,false,topLeft=Offset(w*.18f,h*.18f),size=androidx.compose.ui.geometry.Size(w*.64f,h*.64f),style=Stroke(5f))}
            else->{drawLine(c.bright,Offset(w*.24f,h*.5f),Offset(w*.76f,h*.5f),5f)}
        }
    }
}

@Composable
private fun TrainerTechniqueHistoryV27(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    var revision by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    val items=remember(revision){decodeTechniqueSubsV27(store.s("technique_submissions_v27",""))}
    fun save(updated:List<TechniqueSubmissionV27>){store.ps("technique_submissions_v27",encodeTechniqueSubsV27(updated));revision++}

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("TECHNIQUE REVIEW HISTORY",color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
        Text("Student technique uploads, saved analyses and trainer favorites.",color=c.muted)
        if(message.isNotBlank())Text(message,color=c.bright,fontSize=10.sp)
        if(items.isEmpty())RsPanel(c){
            Text("No technique uploads yet",color=c.bright,fontWeight=FontWeight.Bold)
            Text("Saved student technique submissions will appear here.",color=c.muted)
        }
        items.forEach{item->
            RsPanel(c){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(item.technique,color=c.bright,fontWeight=FontWeight.Black)
                        Text(item.created,color=c.muted,fontSize=10.sp)
                        Text(item.name,color=c.text,fontSize=11.sp,maxLines=1)
                    }
                    Text(if(item.favorite)"★" else "☆",color=c.bright,fontSize=26.sp)
                }
                if(item.uri.isNotBlank())RsTechniqueVideoPreviewV27(item.uri)
                Text(item.summary,color=c.text,fontSize=11.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Button(onClick={
                        save(items.map{if(it.id==item.id)it.copy(favorite=!it.favorite) else it})
                        message=if(item.favorite)"Removed from favorites." else "Saved to trainer favorites."
                    },modifier=Modifier.weight(1f)){Text(if(item.favorite)"Unfavorite" else "Favorite")}
                    OutlinedButton(onClick={
                        save(items.filterNot{it.id==item.id})
                        rsDeleteTechniqueVideoV36(context,item.uri)
                        message="Technique submission deleted."
                    },modifier=Modifier.weight(1f)){Text("Delete")}
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

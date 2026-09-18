package com.rskickbox.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RsSessionBlockV52(
    val id:String,val title:String,val seconds:Int,val instructions:String
)

data class RsTrainingSessionV52(
    val id:String,val title:String,val blocks:List<RsSessionBlockV52>,val active:Boolean
)

private fun rsEncodeSessionsV52(items:List<RsTrainingSessionV52>):String{
    val a=JSONArray()
    items.forEach{s->
        a.put(JSONObject().apply{
            put("id",s.id);put("title",s.title);put("active",s.active)
            put("blocks",JSONArray().apply{
                s.blocks.forEach{b->put(JSONObject().apply{
                    put("id",b.id);put("title",b.title);put("seconds",b.seconds);put("instructions",b.instructions)
                })}
            })
        })
    }
    return a.toString()
}

private fun rsDecodeSessionsV52(raw:String):List<RsTrainingSessionV52>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                val ba=o.optJSONArray("blocks")?:JSONArray()
                val blocks=buildList{
                    for(j in 0 until ba.length()){
                        val b=ba.getJSONObject(j)
                        add(RsSessionBlockV52(
                            b.optString("id"),b.optString("title"),
                            b.optInt("seconds",60).coerceIn(10,3600),b.optString("instructions")
                        ))
                    }
                }
                add(RsTrainingSessionV52(o.optString("id"),o.optString("title"),blocks,o.optBoolean("active",true)))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsSeedSessionsV52()=listOf(
    RsTrainingSessionV52(
        "fundamentals_session","RS Fundamentals Session",
        listOf(
            RsSessionBlockV52("warmup","Warm-up",300,"Movement, guard, light footwork and mobility."),
            RsSessionBlockV52("jab","Jab rounds",180,"Sharp jab, immediate guard recovery and balanced stance."),
            RsSessionBlockV52("combo","Combination rounds",180,"Jab-cross-low kick with controlled recovery."),
            RsSessionBlockV52("cooldown","Cooldown",180,"Breathing, mobility and relaxed recovery.")
        ),true
    )
)

fun rsLoadSessionsV52(store:RsStore):List<RsTrainingSessionV52>{
    val raw=store.s("training_sessions_v52","")
    if(raw.isBlank()){
        rsSaveSessionsV52(store,rsSeedSessionsV52())
        return rsSeedSessionsV52()
    }
    return rsDecodeSessionsV52(raw)
}
private fun rsSaveSessionsV52(store:RsStore,items:List<RsTrainingSessionV52>)=
    store.ps("training_sessions_v52",rsEncodeSessionsV52(items))

private fun rsOpsUiV52(lang:RsLang,key:String):String{
    val en=mapOf(
        "builder" to "Session Builder","builder_sub" to "Create the structured training session shown to students.",
        "player" to "Session Player","player_sub" to "Follow the active trainer-built session block by block.",
        "new_session" to "+ New session","close" to "Close","title" to "Session title","block" to "Block title",
        "seconds" to "Seconds","instructions" to "Instructions","add_block" to "Add block","save" to "Save session",
        "active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm",
        "start" to "Start","pause" to "Pause","next" to "Next","restart" to "Restart","complete" to "Session complete",
        "qr" to "QR Attendance","qr_sub" to "Generate class check-in QR codes linked to the real attendance list.",
        "student_qr" to "Class Check-In","student_qr_sub" to "Scan the trainer QR to check in to a class.",
        "generate" to "Generate new check-in QR","scan" to "Scan class QR","checked" to "Checked in",
        "invalid" to "Invalid or expired attendance QR.","local_note" to "Local preview: cross-device attendance will sync after Supabase connection."
    )
    val nl=en+mapOf(
        "builder" to "Sessiebouwer","builder_sub" to "Maak de gestructureerde training die leerlingen zien.",
        "player" to "Sessiespeler","player_sub" to "Volg de actieve trainersessie blok voor blok.",
        "new_session" to "+ Nieuwe sessie","close" to "Sluiten","title" to "Sessietitel","block" to "Bloktitel",
        "seconds" to "Seconden","instructions" to "Instructies","add_block" to "Blok toevoegen","save" to "Sessie opslaan",
        "active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen",
        "start" to "Start","pause" to "Pauze","next" to "Volgende","restart" to "Herstart","complete" to "Sessie voltooid",
        "qr" to "QR Aanwezigheid","qr_sub" to "Genereer check-in QR-codes gekoppeld aan de echte aanwezigheidslijst.",
        "student_qr" to "Les Check-In","student_qr_sub" to "Scan de trainer-QR om in te checken.",
        "generate" to "Nieuwe check-in QR maken","scan" to "Les-QR scannen","checked" to "Ingecheckt",
        "invalid" to "Ongeldige of verlopen aanwezigheids-QR.","local_note" to "Lokale preview: cross-device aanwezigheid synchroniseert na Supabase-koppeling."
    )
    val pack=when(lang.code){"nl"->nl;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsSessionBuilderV52(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var sessionTitle by remember{mutableStateOf("")}
    var blockTitle by remember{mutableStateOf("")}
    var blockSeconds by remember{mutableStateOf("180")}
    var blockInstructions by remember{mutableStateOf("")}
    var draftBlocks by remember{mutableStateOf<List<RsSessionBlockV52>>(emptyList())}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val sessions=remember(revision){rsLoadSessionsV52(store)}
    fun save(items:List<RsTrainingSessionV52>){rsSaveSessionsV52(store,items);revision++}

    RsScroll(c,rsOpsUiV52(lang,"builder"),rsOpsUiV52(lang,"builder_sub")){
        Button(onClick={showCreate=!showCreate},modifier=Modifier.fillMaxWidth()){
            Text(if(showCreate)rsOpsUiV52(lang,"close") else rsOpsUiV52(lang,"new_session"))
        }
        if(showCreate)RsPanel(c){
            OutlinedTextField(sessionTitle,{sessionTitle=it.take(100)},label={Text(rsOpsUiV52(lang,"title"))},modifier=Modifier.fillMaxWidth())
            draftBlocks.forEachIndexed{i,b->
                Text((i+1).toString()+". "+b.title+" · "+b.seconds+"s",color=c.text)
            }
            OutlinedTextField(blockTitle,{blockTitle=it.take(80)},label={Text(rsOpsUiV52(lang,"block"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(blockSeconds,{blockSeconds=it.filter(Char::isDigit)},label={Text(rsOpsUiV52(lang,"seconds"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(blockInstructions,{blockInstructions=it.take(1000)},label={Text(rsOpsUiV52(lang,"instructions"))},modifier=Modifier.fillMaxWidth(),minLines=2)
            OutlinedButton(
                onClick={
                    draftBlocks=draftBlocks+RsSessionBlockV52(
                        UUID.randomUUID().toString(),blockTitle.trim(),
                        blockSeconds.toIntOrNull()?.coerceIn(10,3600)?:180,blockInstructions.trim()
                    )
                    blockTitle="";blockSeconds="180";blockInstructions=""
                },
                enabled=blockTitle.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"add_block"))}
            Button(
                onClick={
                    val next=sessions.map{it.copy(active=false)}+
                        RsTrainingSessionV52(UUID.randomUUID().toString(),sessionTitle.trim(),draftBlocks,true)
                    save(next)
                    sessionTitle="";draftBlocks=emptyList();showCreate=false
                },
                enabled=sessionTitle.isNotBlank()&&draftBlocks.isNotEmpty(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"save"))}
        }
        sessions.forEach{s->
            RsPanel(c){
                Text(s.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(s.blocks.size.toString()+" blocks · "+s.blocks.sumOf{it.seconds}/60+" min",color=c.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(s.active)rsOpsUiV52(lang,"active") else rsOpsUiV52(lang,"inactive"),color=c.muted)
                    Switch(s.active,{on->
                        save(sessions.map{
                            if(it.id==s.id)it.copy(active=on)
                            else if(on)it.copy(active=false) else it
                        })
                    })
                }
                s.blocks.forEachIndexed{i,b->Text((i+1).toString()+". "+b.title+" · "+b.seconds+"s",color=c.text,fontSize=11.sp)}
                OutlinedButton(
                    onClick={
                        if(pendingDelete==s.id){save(sessions.filterNot{it.id==s.id});pendingDelete=null}
                        else pendingDelete=s.id
                    },modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==s.id)rsOpsUiV52(lang,"confirm") else rsOpsUiV52(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsSessionPlayerV52(c:RsPalette,store:RsStore,lang:RsLang){
    val session=rsLoadSessionsV52(store).firstOrNull{it.active}
    var index by remember(session?.id){mutableIntStateOf(0)}
    var remaining by remember(session?.id,index){mutableIntStateOf(session?.blocks?.getOrNull(index)?.seconds?:0)}
    var running by remember(session?.id,index){mutableStateOf(false)}

    LaunchedEffect(running,remaining,index,session?.id){
        if(running && remaining>0){
            delay(1000)
            remaining--
        }else if(running && remaining<=0){
            running=false
        }
    }

    RsScroll(c,rsOpsUiV52(lang,"player"),rsOpsUiV52(lang,"player_sub")){
        if(session==null){
            RsPanel(c){Text("No active trainer session.",color=c.muted)}
        }else{
            val block=session.blocks.getOrNull(index)
            RsPanel(c){
                Text(session.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
                Text((index+1).coerceAtMost(session.blocks.size).toString()+" / "+session.blocks.size,color=c.muted)
            }
            if(block!=null){
                RsPanel(c){
                    Text(block.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=24.sp)
                    Text(block.instructions,color=c.text)
                    Text((remaining/60).toString().padStart(2,'0')+":"+(remaining%60).toString().padStart(2,'0'),color=c.bright,fontWeight=FontWeight.Black,fontSize=36.sp)
                    Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){
                        Text(if(running)rsOpsUiV52(lang,"pause") else rsOpsUiV52(lang,"start"))
                    }
                    OutlinedButton(
                        onClick={
                            if(index<session.blocks.lastIndex){index++;running=false}
                            else{index=session.blocks.size;running=false}
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsOpsUiV52(lang,"next"))}
                    OutlinedButton(
                        onClick={remaining=block.seconds;running=false},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsOpsUiV52(lang,"restart"))}
                }
            }else RsPanel(c){Text(rsOpsUiV52(lang,"complete"),color=c.bright,fontWeight=FontWeight.Black)}
        }
    }
}

private fun rsAttendancePayloadV52(classId:String,code:String)=
    "rskickbox://attendance?v=1&classId="+android.net.Uri.encode(classId)+"&code="+android.net.Uri.encode(code)

private fun rsParseAttendanceV52(raw:String):Pair<String,String>? = runCatching{
    val uri=android.net.Uri.parse(raw)
    if(uri.scheme!="rskickbox"||uri.host!="attendance")return null
    val classId=uri.getQueryParameter("classId").orEmpty()
    val code=uri.getQueryParameter("code").orEmpty()
    if(classId.isBlank()||code.isBlank())null else classId to code
}.getOrNull()

@Composable
fun RsQrAttendanceTrainerV52(c:RsPalette,store:RsStore,lang:RsLang){
    val classes=rsLoadClassesV38(store).filter{it.active}
    var selectedId by remember(classes){mutableStateOf(classes.firstOrNull()?.id.orEmpty())}
    var revision by remember{mutableIntStateOf(0)}
    val selected=classes.firstOrNull{it.id==selectedId}
    val code=if(selectedId.isBlank())"" else store.s("attendance_qr_v52_"+selectedId,"")
    val qr:Bitmap?=remember(selectedId,code,revision){
        if(selectedId.isBlank()||code.isBlank())null else rsQrBitmapV33(rsAttendancePayloadV52(selectedId,code),720)
    }

    RsScroll(c,rsOpsUiV52(lang,"qr"),rsOpsUiV52(lang,"qr_sub")){
        Text(rsOpsUiV52(lang,"local_note"),color=c.muted,fontSize=10.sp)
        classes.forEach{clazz->
            FilterChip(
                selected=selectedId==clazz.id,onClick={selectedId=clazz.id},
                label={Text(clazz.dayLabel+" "+clazz.timeLabel+" · "+clazz.title,maxLines=1)},
                modifier=Modifier.fillMaxWidth()
            )
        }
        if(selected!=null)RsPanel(c){
            Text(selected.title,color=c.bright,fontWeight=FontWeight.Black)
            Button(
                onClick={
                    store.ps("attendance_qr_v52_"+selected.id,UUID.randomUUID().toString().replace("-","").take(10).uppercase())
                    revision++
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"generate"))}
            if(qr!=null)Image(qr.asImageBitmap(),"Attendance QR",modifier=Modifier.fillMaxWidth().aspectRatio(1f))
        }
    }
}

@Composable
fun RsStudentCheckInV52(c:RsPalette,store:RsStore,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    var status by remember{mutableStateOf("")}
    val scanner=remember{GmsBarcodeScanning.getClient(context)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")

    RsScroll(c,rsOpsUiV52(lang,"student_qr"),rsOpsUiV52(lang,"student_qr_sub")){
        Text(rsOpsUiV52(lang,"local_note"),color=c.muted,fontSize=10.sp)
        RsPanel(c){
            Button(
                onClick={
                    scanner.startScan()
                        .addOnSuccessListener{barcode->
                            val pair=barcode.rawValue?.let(::rsParseAttendanceV52)
                            if(pair==null){
                                status=rsOpsUiV52(lang,"invalid")
                            }else{
                                val (classId,code)=pair
                                val expected=store.s("attendance_qr_v52_"+classId,"")
                                val validClass=rsLoadClassesV38(store).any{it.id==classId&&it.active}
                                if(expected.isNotBlank()&&expected==code&&validClass){
                                    store.pb(rsAttendanceKeyV38(classId,name),true)
                                    store.ps("attendance_email_v52_"+classId+"_"+email.lowercase(),System.currentTimeMillis().toString())
                                    status=rsOpsUiV52(lang,"checked")
                                }else status=rsOpsUiV52(lang,"invalid")
                            }
                        }
                        .addOnCanceledListener{}
                        .addOnFailureListener{status=rsOpsUiV52(lang,"invalid")}
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"scan"))}
            if(status.isNotBlank())Text(status,color=if(status==rsOpsUiV52(lang,"checked"))c.bright else c.muted,fontWeight=FontWeight.Bold)
        }
    }
}

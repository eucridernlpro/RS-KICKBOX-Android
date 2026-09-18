package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RsHomeworkV46(
    val id:String,val studentEmail:String,val title:String,val details:String,
    val dueLabel:String,val completed:Boolean,val createdAt:Long
)

data class RsCoachNoteV46(
    val id:String,val studentEmail:String,val note:String,val createdAt:Long
)

data class RsAssessmentV46(
    val id:String,val studentEmail:String,
    val punches:Int,val kicks:Int,val defense:Int,val footwork:Int,
    val combinations:Int,val conditioning:Int,
    val summary:String,val createdAt:Long
)

private fun rsArrayV46(store:RsStore,key:String)=runCatching{JSONArray(store.s(key,""))}.getOrElse{JSONArray()}

private fun rsLoadHomeworkV46(store:RsStore):List<RsHomeworkV46>{
    val a=rsArrayV46(store,"homework_v46")
    return buildList{
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            add(RsHomeworkV46(
                o.optString("id"),o.optString("email"),o.optString("title"),o.optString("details"),
                o.optString("due"),o.optBoolean("completed"),o.optLong("createdAt")
            ))
        }
    }
}
private fun rsSaveHomeworkV46(store:RsStore,items:List<RsHomeworkV46>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("title",x.title);put("details",x.details)
        put("due",x.dueLabel);put("completed",x.completed);put("createdAt",x.createdAt)
    })}
    store.ps("homework_v46",a.toString())
}

private fun rsLoadNotesV46(store:RsStore):List<RsCoachNoteV46>{
    val a=rsArrayV46(store,"coach_notes_v46")
    return buildList{
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            add(RsCoachNoteV46(o.optString("id"),o.optString("email"),o.optString("note"),o.optLong("createdAt")))
        }
    }
}
private fun rsSaveNotesV46(store:RsStore,items:List<RsCoachNoteV46>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("note",x.note);put("createdAt",x.createdAt)
    })}
    store.ps("coach_notes_v46",a.toString())
}

fun rsLoadAssessmentsV46(store:RsStore):List<RsAssessmentV46>{
    val a=rsArrayV46(store,"assessments_v46")
    return buildList{
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            add(RsAssessmentV46(
                o.optString("id"),o.optString("email"),
                o.optInt("punches",50),o.optInt("kicks",50),o.optInt("defense",50),
                o.optInt("footwork",50),o.optInt("combinations",50),o.optInt("conditioning",50),
                o.optString("summary"),o.optLong("createdAt")
            ))
        }
    }
}
private fun rsSaveAssessmentsV46(store:RsStore,items:List<RsAssessmentV46>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("punches",x.punches);put("kicks",x.kicks)
        put("defense",x.defense);put("footwork",x.footwork);put("combinations",x.combinations)
        put("conditioning",x.conditioning);put("summary",x.summary);put("createdAt",x.createdAt)
    })}
    store.ps("assessments_v46",a.toString())
}

fun rsHomeworkRawForStudentV46(store:RsStore,email:String):String{
    val a=JSONArray()
    rsLoadHomeworkV46(store).filter{it.studentEmail.equals(email,true)}.forEach{x->
        a.put(JSONObject().apply{
            put("id",x.id);put("title",x.title);put("details",x.details);put("due",x.dueLabel)
            put("completed",x.completed);put("createdAt",x.createdAt)
        })
    }
    return a.toString()
}

fun rsAssessmentsRawForStudentV46(store:RsStore,email:String):String{
    val a=JSONArray()
    rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}.forEach{x->
        a.put(JSONObject().apply{
            put("id",x.id);put("punches",x.punches);put("kicks",x.kicks);put("defense",x.defense)
            put("footwork",x.footwork);put("combinations",x.combinations);put("conditioning",x.conditioning)
            put("summary",x.summary);put("createdAt",x.createdAt)
        })
    }
    return a.toString()
}

fun rsCoachNotesRawForPrivacyV46(store:RsStore,email:String):String{
    val a=JSONArray()
    rsLoadNotesV46(store).filter{it.studentEmail.equals(email,true)}.forEach{x->
        a.put(JSONObject().apply{put("id",x.id);put("note",x.note);put("createdAt",x.createdAt)})
    }
    return a.toString()
}

fun rsRemoveDevelopmentDataForStudentV46(store:RsStore,email:String){
    rsSaveHomeworkV46(store,rsLoadHomeworkV46(store).filterNot{it.studentEmail.equals(email,true)})
    rsSaveNotesV46(store,rsLoadNotesV46(store).filterNot{it.studentEmail.equals(email,true)})
    rsSaveAssessmentsV46(store,rsLoadAssessmentsV46(store).filterNot{it.studentEmail.equals(email,true)})
}

private fun rsDateV46(ms:Long)=if(ms<=0L)"" else SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date(ms))

private fun rsDevUiV46(lang:RsLang,key:String):String{
    val en=mapOf(
        "homework" to "Homework","homework_sub" to "Your trainer assignments and completion status.",
        "manager" to "Homework Manager","manager_sub" to "Assign focused work to individual students.",
        "notes" to "Coach Notes","notes_sub" to "Private trainer notes linked to student development.",
        "assess" to "Assessments","assess_sub" to "Record skill scores and coaching feedback.",
        "progress" to "Progress","progress_sub" to "Latest assessed skill profile and development history.",
        "student" to "Student","title" to "Title","details" to "Details","due" to "Due","assign" to "Assign homework",
        "completed" to "Completed","open" to "Open","mark_done" to "Mark completed","reopen" to "Reopen",
        "delete" to "Delete","confirm" to "Confirm","new_note" to "New private coach note","save_note" to "Save note",
        "summary" to "Assessment summary","save_assessment" to "Save assessment","latest" to "LATEST ASSESSMENT",
        "history" to "ASSESSMENT HISTORY","none" to "No records yet."
    )
    val nl=en+mapOf(
        "homework" to "Huiswerk","homework_sub" to "Opdrachten van je trainer en voltooiingsstatus.",
        "manager" to "Huiswerkbeheer","manager_sub" to "Wijs gericht werk toe aan individuele leerlingen.",
        "notes" to "Coachnotities","notes_sub" to "Privénotities van de trainer gekoppeld aan ontwikkeling.",
        "assess" to "Beoordelingen","assess_sub" to "Leg vaardigheidsscores en coachfeedback vast.",
        "progress" to "Voortgang","progress_sub" to "Laatste vaardigheidsprofiel en ontwikkelingshistorie.",
        "student" to "Leerling","title" to "Titel","details" to "Details","due" to "Deadline","assign" to "Huiswerk toewijzen",
        "completed" to "Voltooid","open" to "Open","mark_done" to "Markeer voltooid","reopen" to "Heropenen",
        "delete" to "Verwijderen","confirm" to "Bevestigen","new_note" to "Nieuwe privé coachnotitie","save_note" to "Notitie opslaan",
        "summary" to "Samenvatting beoordeling","save_assessment" to "Beoordeling opslaan","latest" to "LAATSTE BEOORDELING",
        "history" to "BEOORDELINGSHISTORIE","none" to "Nog geen gegevens."
    )
    val pack=when(lang.code){"nl"->nl;else->en}
    return pack[key]?:en[key]?:key
}

fun rsStudentsV46(store:RsStore):List<Pair<String,String>> =
    (listOf("alex@rskickbox.nl" to "Alex de Vries")+
        rsLoadStudentsV33(store).map{it.email to it.name.ifBlank{it.email}})
        .distinctBy{it.first.lowercase()}

@Composable
fun RsStudentHomeworkV46(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val items=remember(revision){rsLoadHomeworkV46(store).filter{it.studentEmail.equals(email,true)}.sortedByDescending{it.createdAt}}
    RsScroll(c,rsDevUiV46(lang,"homework"),rsDevUiV46(lang,"homework_sub")){
        if(items.isEmpty())RsPanel(c){Text(rsDevUiV46(lang,"none"),color=c.muted)}
        items.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(x.details,color=c.text)
                if(x.dueLabel.isNotBlank())Text(rsDevUiV46(lang,"due")+" · "+x.dueLabel,color=c.muted)
                Text(if(x.completed)rsDevUiV46(lang,"completed") else rsDevUiV46(lang,"open"),color=c.bright,fontWeight=FontWeight.Bold)
                Button(
                    onClick={
                        rsSaveHomeworkV46(store,rsLoadHomeworkV46(store).map{if(it.id==x.id)it.copy(completed=!it.completed) else it})
                        revision++
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(x.completed)rsDevUiV46(lang,"reopen") else rsDevUiV46(lang,"mark_done"))}
            }
        }
    }
}

@Composable
fun RsHomeworkManagerV46(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var selectedEmail by remember{mutableStateOf(rsStudentsV46(store).firstOrNull()?.first.orEmpty())}
    var title by remember{mutableStateOf("")}
    var details by remember{mutableStateOf("")}
    var due by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val students=rsStudentsV46(store)
    val items=remember(revision){rsLoadHomeworkV46(store).sortedByDescending{it.createdAt}}

    RsScroll(c,rsDevUiV46(lang,"manager"),rsDevUiV46(lang,"manager_sub")){
        RsPanel(c){
            Text(rsDevUiV46(lang,"student"),color=c.muted)
            students.forEach{(email,name)->
                FilterChip(selected=selectedEmail==email,onClick={selectedEmail=email},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsDevUiV46(lang,"title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(details,{details=it.take(1000)},label={Text(rsDevUiV46(lang,"details"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            OutlinedTextField(due,{due=it.take(40)},label={Text(rsDevUiV46(lang,"due"))},modifier=Modifier.fillMaxWidth())
            Button(
                onClick={
                    rsSaveHomeworkV46(store,listOf(RsHomeworkV46(UUID.randomUUID().toString(),selectedEmail,title.trim(),details.trim(),due.trim(),false,System.currentTimeMillis()))+items)
                    title="";details="";due="";revision++
                },
                enabled=selectedEmail.isNotBlank()&&title.isNotBlank()&&details.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsDevUiV46(lang,"assign"))}
        }
        items.forEach{x->
            val name=students.firstOrNull{it.first.equals(x.studentEmail,true)}?.second?:x.studentEmail
            RsPanel(c){
                Text(name+" · "+x.title,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.details,color=c.text)
                Text((if(x.completed)rsDevUiV46(lang,"completed") else rsDevUiV46(lang,"open"))+" · "+rsDateV46(x.createdAt),color=c.muted)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){
                            rsSaveHomeworkV46(store,items.filterNot{it.id==x.id});pendingDelete=null;revision++
                        }else pendingDelete=x.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsDevUiV46(lang,"confirm") else rsDevUiV46(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsCoachNotesV46(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var selectedEmail by remember{mutableStateOf(rsStudentsV46(store).firstOrNull()?.first.orEmpty())}
    var note by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val students=rsStudentsV46(store)
    val notes=remember(revision){rsLoadNotesV46(store).sortedByDescending{it.createdAt}}

    RsScroll(c,rsDevUiV46(lang,"notes"),rsDevUiV46(lang,"notes_sub")){
        RsPanel(c){
            students.forEach{(email,name)->
                FilterChip(selected=selectedEmail==email,onClick={selectedEmail=email},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(note,{note=it.take(1500)},label={Text(rsDevUiV46(lang,"new_note"))},modifier=Modifier.fillMaxWidth(),minLines=4)
            Button(
                onClick={
                    rsSaveNotesV46(store,listOf(RsCoachNoteV46(UUID.randomUUID().toString(),selectedEmail,note.trim(),System.currentTimeMillis()))+notes)
                    note="";revision++
                },
                enabled=selectedEmail.isNotBlank()&&note.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsDevUiV46(lang,"save_note"))}
        }
        notes.filter{selectedEmail.isBlank()||it.studentEmail.equals(selectedEmail,true)}.forEach{x->
            RsPanel(c){
                Text(students.firstOrNull{it.first.equals(x.studentEmail,true)}?.second?:x.studentEmail,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.note,color=c.text)
                Text(rsDateV46(x.createdAt),color=c.muted,fontSize=10.sp)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){rsSaveNotesV46(store,notes.filterNot{it.id==x.id});pendingDelete=null;revision++}
                        else pendingDelete=x.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsDevUiV46(lang,"confirm") else rsDevUiV46(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsAssessmentsV46(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val students=rsStudentsV46(store)
    var selectedEmail by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    var punches by remember{mutableFloatStateOf(84f)}
    var kicks by remember{mutableFloatStateOf(71f)}
    var defense by remember{mutableFloatStateOf(67f)}
    var footwork by remember{mutableFloatStateOf(62f)}
    var combos by remember{mutableFloatStateOf(76f)}
    var conditioning by remember{mutableFloatStateOf(69f)}
    var summary by remember{mutableStateOf("")}
    val items=remember(revision){rsLoadAssessmentsV46(store).sortedByDescending{it.createdAt}}

    @Composable fun slider(label:String,value:Float,onChange:(Float)->Unit){
        Text(label+" · "+value.toInt(),color=c.text)
        Slider(value,onValueChange=onChange,valueRange=0f..100f)
    }

    RsScroll(c,rsDevUiV46(lang,"assess"),rsDevUiV46(lang,"assess_sub")){
        RsPanel(c){
            students.forEach{(email,name)->
                FilterChip(selected=selectedEmail==email,onClick={selectedEmail=email},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            slider("Punches",punches){punches=it};slider("Kicks",kicks){kicks=it};slider("Defense",defense){defense=it}
            slider("Footwork",footwork){footwork=it};slider("Combinations",combos){combos=it};slider("Conditioning",conditioning){conditioning=it}
            OutlinedTextField(summary,{summary=it.take(1200)},label={Text(rsDevUiV46(lang,"summary"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Button(
                onClick={
                    rsSaveAssessmentsV46(store,listOf(RsAssessmentV46(
                        UUID.randomUUID().toString(),selectedEmail,punches.toInt(),kicks.toInt(),defense.toInt(),
                        footwork.toInt(),combos.toInt(),conditioning.toInt(),summary.trim(),System.currentTimeMillis()
                    ))+items)
                    summary="";revision++
                },
                enabled=selectedEmail.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsDevUiV46(lang,"save_assessment"))}
        }
    }
}

@Composable
fun RsStudentProgressV46(c:RsPalette,store:RsStore,lang:RsLang){
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val assessments=rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}.sortedByDescending{it.createdAt}
    val latest=assessments.firstOrNull()?:RsAssessmentV46("seed",email,84,71,67,62,76,69,"",0L)
    val metrics=listOf(
        "Punches" to latest.punches,"Kicks" to latest.kicks,"Defense" to latest.defense,
        "Footwork" to latest.footwork,"Combinations" to latest.combinations,"Conditioning" to latest.conditioning
    )
    RsScroll(c,rsDevUiV46(lang,"progress"),rsDevUiV46(lang,"progress_sub")){
        RsPanel(c){
            Text(rsDevUiV46(lang,"latest"),color=c.bright,fontWeight=FontWeight.Black)
            metrics.forEach{(name,value)->
                Text(name+" · "+value,color=c.text)
                LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth())
            }
            if(latest.summary.isNotBlank())Text(latest.summary,color=c.muted)
            if(latest.createdAt>0L)Text(rsDateV46(latest.createdAt),color=c.muted,fontSize=10.sp)
        }
        Text(rsDevUiV46(lang,"history"),color=c.bright,fontWeight=FontWeight.Black)
        assessments.forEach{x->
            RsPanel(c){
                val avg=(x.punches+x.kicks+x.defense+x.footwork+x.combinations+x.conditioning)/6
                Text(rsDateV46(x.createdAt)+" · "+avg+"/100",color=c.bright,fontWeight=FontWeight.Bold)
                if(x.summary.isNotBlank())Text(x.summary,color=c.text)
            }
        }
    }
}

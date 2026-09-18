package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.UUID

data class RsChallengeV47(
    val id:String,val studentEmail:String,val title:String,val target:Int,
    val current:Int,val unit:String,val active:Boolean
)

data class RsFightCampV47(
    val studentEmail:String,val currentWeek:Int,val totalWeeks:Int,
    val focus:String,val active:Boolean
)

fun rsLoadChallengesV47(store:RsStore):List<RsChallengeV47>{
    val raw=store.s("challenges_v47","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsChallengeV47(
                    o.optString("id"),o.optString("email"),o.optString("title"),
                    o.optInt("target",10).coerceAtLeast(1),o.optInt("current",0).coerceAtLeast(0),
                    o.optString("unit","sessions"),o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveChallengesV47(store:RsStore,items:List<RsChallengeV47>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("title",x.title);put("target",x.target)
        put("current",x.current);put("unit",x.unit);put("active",x.active)
    })}
    store.ps("challenges_v47",a.toString())
}

fun rsLoadFightCampsV47(store:RsStore):List<RsFightCampV47>{
    val raw=store.s("fightcamps_v47","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsFightCampV47(
                    o.optString("email"),o.optInt("week",3).coerceIn(1,8),
                    o.optInt("total",8).coerceIn(1,12),o.optString("focus","Technique & conditioning"),
                    o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveFightCampsV47(store:RsStore,items:List<RsFightCampV47>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("email",x.studentEmail);put("week",x.currentWeek);put("total",x.totalWeeks)
        put("focus",x.focus);put("active",x.active)
    })}
    store.ps("fightcamps_v47",a.toString())
}

private fun rsPerformanceUiV47(lang:RsLang,key:String):String{
    val en=mapOf(
        "challenges" to "Challenges","challenge_sub" to "Track trainer-assigned performance goals.",
        "challenge_admin" to "Challenge Manager","challenge_admin_sub" to "Assign and manage student challenges.",
        "fightcamp" to "Fight Camp","fightcamp_sub" to "Your structured 8-week fight preparation plan.",
        "fightcamp_admin" to "Fight Camp Manager","fightcamp_admin_sub" to "Create and update student fight camps.",
        "badges" to "Badges","badges_sub" to "Achievements earned from real training progress.",
        "student" to "Student","title" to "Challenge title","target" to "Target","unit" to "Unit",
        "assign" to "Assign challenge","active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm",
        "progress" to "Progress","complete" to "Completed","increase" to "+1 progress",
        "week" to "Current week","focus" to "Camp focus","save" to "Save Fight Camp",
        "none" to "No active items yet.","earned" to "EARNED","locked" to "LOCKED"
    )
    val nl=en+mapOf(
        "challenges" to "Challenges","challenge_sub" to "Volg prestatiedoelen die je trainer heeft toegewezen.",
        "challenge_admin" to "Challengebeheer","challenge_admin_sub" to "Wijs challenges toe en beheer ze per leerling.",
        "fightcamp" to "Fight Camp","fightcamp_sub" to "Jouw gestructureerde 8-weekse voorbereiding.",
        "fightcamp_admin" to "Fight Camp-beheer","fightcamp_admin_sub" to "Maak en beheer fight camps per leerling.",
        "badges" to "Badges","badges_sub" to "Prestaties verdiend uit echte trainingsvoortgang.",
        "student" to "Leerling","title" to "Challenge titel","target" to "Doel","unit" to "Eenheid",
        "assign" to "Challenge toewijzen","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen",
        "progress" to "Voortgang","complete" to "Voltooid","increase" to "+1 voortgang",
        "week" to "Huidige week","focus" to "Camp-focus","save" to "Fight Camp opslaan",
        "none" to "Nog geen actieve items.","earned" to "VERDIEND","locked" to "VERGRENDELD"
    )
    val pack=when(lang.code){"nl"->nl;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsStudentChallengesV47(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val items=remember(revision){rsLoadChallengesV47(store).filter{it.studentEmail.equals(email,true)&&it.active}}
    RsScroll(c,rsPerformanceUiV47(lang,"challenges"),rsPerformanceUiV47(lang,"challenge_sub")){
        if(items.isEmpty())RsPanel(c){Text(rsPerformanceUiV47(lang,"none"),color=c.muted)}
        items.forEach{x->
            val current=x.current.coerceAtMost(x.target)
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(current.toString()+" / "+x.target+" "+x.unit,color=c.text)
                LinearProgressIndicator(progress={current.toFloat()/x.target},modifier=Modifier.fillMaxWidth())
                Text(if(current>=x.target)rsPerformanceUiV47(lang,"complete") else rsPerformanceUiV47(lang,"progress"),color=c.muted)
                if(current<x.target)Button(
                    onClick={
                        rsSaveChallengesV47(store,rsLoadChallengesV47(store).map{
                            if(it.id==x.id)it.copy(current=(it.current+1).coerceAtMost(it.target)) else it
                        })
                        revision++
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPerformanceUiV47(lang,"increase"))}
            }
        }
    }
}

@Composable
fun RsChallengeManagerV47(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val students=rsStudentsV46(store)
    var email by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    var title by remember{mutableStateOf("")}
    var target by remember{mutableStateOf("10")}
    var unit by remember{mutableStateOf("sessions")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val items=remember(revision){rsLoadChallengesV47(store)}
    fun save(list:List<RsChallengeV47>){rsSaveChallengesV47(store,list);revision++}

    RsScroll(c,rsPerformanceUiV47(lang,"challenge_admin"),rsPerformanceUiV47(lang,"challenge_admin_sub")){
        RsPanel(c){
            students.forEach{(e,name)->
                FilterChip(selected=email==e,onClick={email=e},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsPerformanceUiV47(lang,"title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(target,{target=it.filter(Char::isDigit)},label={Text(rsPerformanceUiV47(lang,"target"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(unit,{unit=it.take(30)},label={Text(rsPerformanceUiV47(lang,"unit"))},modifier=Modifier.fillMaxWidth())
            Button(
                onClick={
                    save(listOf(RsChallengeV47(UUID.randomUUID().toString(),email,title.trim(),target.toIntOrNull()?.coerceIn(1,10000)?:10,0,unit.trim().ifBlank{"sessions"},true))+items)
                    title="";target="10";unit="sessions"
                },
                enabled=email.isNotBlank()&&title.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPerformanceUiV47(lang,"assign"))}
        }
        items.forEach{x->
            RsPanel(c){
                Text(students.firstOrNull{it.first.equals(x.studentEmail,true)}?.second?:x.studentEmail,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.title+" · "+x.current+"/"+x.target+" "+x.unit,color=c.text)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(x.active)rsPerformanceUiV47(lang,"active") else rsPerformanceUiV47(lang,"inactive"),color=c.muted)
                    Switch(x.active,{v->save(items.map{if(it.id==x.id)it.copy(active=v) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){save(items.filterNot{it.id==x.id});pendingDelete=null}
                        else pendingDelete=x.id
                    },modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsPerformanceUiV47(lang,"confirm") else rsPerformanceUiV47(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsStudentFightCampV47(c:RsPalette,store:RsStore,lang:RsLang){
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val camp=rsLoadFightCampsV47(store).firstOrNull{it.studentEmail.equals(email,true)&&it.active}
        ?:RsFightCampV47(email,3,8,"Technique · Conditioning · Recovery",true)
    RsScroll(c,rsPerformanceUiV47(lang,"fightcamp"),rsPerformanceUiV47(lang,"fightcamp_sub")){
        RsPanel(c){
            Text("WEEK "+camp.currentWeek+" / "+camp.totalWeeks,color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black)
            LinearProgressIndicator(progress={camp.currentWeek.toFloat()/camp.totalWeeks},modifier=Modifier.fillMaxWidth())
            Text(camp.focus,color=c.text)
        }
        (1..camp.totalWeeks).forEach{week->
            RsPanel(c){
                Text("Week "+week,color=if(week<=camp.currentWeek)c.bright else c.muted,fontWeight=FontWeight.Bold)
                Text(
                    when{
                        week<camp.currentWeek->"Completed block"
                        week==camp.currentWeek->"Current training block"
                        else->"Upcoming block"
                    },
                    color=c.muted
                )
            }
        }
    }
}

@Composable
fun RsFightCampManagerV47(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val students=rsStudentsV46(store)
    var email by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    val camps=remember(revision){rsLoadFightCampsV47(store)}
    val selected=camps.firstOrNull{it.studentEmail.equals(email,true)}
    var week by remember(email,selected?.currentWeek){mutableFloatStateOf((selected?.currentWeek?:3).toFloat())}
    var focus by remember(email,selected?.focus){mutableStateOf(selected?.focus?:"Technique · Conditioning · Recovery")}
    var active by remember(email,selected?.active){mutableStateOf(selected?.active?:true)}

    RsScroll(c,rsPerformanceUiV47(lang,"fightcamp_admin"),rsPerformanceUiV47(lang,"fightcamp_admin_sub")){
        RsPanel(c){
            students.forEach{(e,name)->
                FilterChip(selected=email==e,onClick={email=e},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            Text(rsPerformanceUiV47(lang,"week")+" · "+week.toInt()+"/8",color=c.text)
            Slider(week,{week=it},valueRange=1f..8f,steps=6)
            OutlinedTextField(focus,{focus=it.take(500)},label={Text(rsPerformanceUiV47(lang,"focus"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(active)rsPerformanceUiV47(lang,"active") else rsPerformanceUiV47(lang,"inactive"),color=c.muted)
                Switch(active,{active=it})
            }
            Button(
                onClick={
                    val updated=camps.filterNot{it.studentEmail.equals(email,true)}+
                        RsFightCampV47(email,week.toInt().coerceIn(1,8),8,focus.trim(),active)
                    rsSaveFightCampsV47(store,updated)
                    revision++
                },
                enabled=email.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPerformanceUiV47(lang,"save"))}
        }
    }
}

@Composable
fun RsBadgesV47(c:RsPalette,store:RsStore,lang:RsLang){
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val challenges=rsLoadChallengesV47(store).filter{it.studentEmail.equals(email,true)}
    val completed=challenges.count{it.current>=it.target}
    val assessments=rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}
    val latest=assessments.maxByOrNull{it.createdAt}
    val avg=latest?.let{(it.punches+it.kicks+it.defense+it.footwork+it.combinations+it.conditioning)/6}?:0
    val camp=rsLoadFightCampsV47(store).firstOrNull{it.studentEmail.equals(email,true)}
    val badges=listOf(
        Triple("First Challenge","Complete 1 trainer challenge",completed>=1),
        Triple("Challenge Hunter","Complete 3 trainer challenges",completed>=3),
        Triple("Technical 70","Reach 70+ average assessment",avg>=70),
        Triple("Technical 80","Reach 80+ average assessment",avg>=80),
        Triple("Fight Camp","Reach week 4 of Fight Camp",(camp?.currentWeek?:0)>=4),
        Triple("Camp Finisher","Reach week 8 of Fight Camp",(camp?.currentWeek?:0)>=8)
    )
    RsScroll(c,rsPerformanceUiV47(lang,"badges"),rsPerformanceUiV47(lang,"badges_sub")){
        badges.forEach{(name,desc,earned)->
            RsPanel(c){
                Text((if(earned)"★ " else "☆ ")+name,color=if(earned)c.bright else c.muted,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(desc,color=c.text)
                Text(if(earned)rsPerformanceUiV47(lang,"earned") else rsPerformanceUiV47(lang,"locked"),color=c.muted)
            }
        }
    }
}

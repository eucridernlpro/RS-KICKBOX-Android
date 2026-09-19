package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.launch

data class RsTechniqueV54(
    val id:String,val name:String,val category:String,val difficulty:String,
    val power:Int,val speed:Int,val balance:Int,val defense:Int,val notes:String
)

private val rsTechniquesV54=listOf(
    RsTechniqueV54("jab","Jab","PUNCH","BEGINNER",55,95,90,85,"Fast straight lead-hand strike. Focus on recovery to guard."),
    RsTechniqueV54("cross","Cross","PUNCH","BEGINNER",82,82,80,72,"Rear straight with hip/shoulder rotation and stable base."),
    RsTechniqueV54("hook","Lead Hook","PUNCH","INTERMEDIATE",78,74,68,65,"Compact rotation, elbow line and immediate guard recovery."),
    RsTechniqueV54("low_kick","Low Kick","KICK","BEGINNER",86,70,72,58,"Pivot, hip turn, shin contact and balanced return."),
    RsTechniqueV54("roundhouse","Body Roundhouse","KICK","INTERMEDIATE",90,68,62,52,"Full hip rotation, pivot and protected head position."),
    RsTechniqueV54("teep","Teep","KICK","INTERMEDIATE",66,72,84,78,"Distance-control kick with strong chamber and recovery."),
    RsTechniqueV54("check","Low Kick Check","DEFENSE","BEGINNER",35,80,86,95,"Raise shin with stable posture while protecting head."),
    RsTechniqueV54("slip","Slip","DEFENSE","INTERMEDIATE",10,88,82,96,"Small head movement with eyes forward and balanced counter position."),
    RsTechniqueV54("clinch","Basic Clinch Frame","CLINCH","INTERMEDIATE",64,48,78,90,"Posture, inside control, framing and safe balance.")
)

private data class RsAcademyTrackV54(val id:String,val title:String,val target:Int)
private val rsAcademyTracksV54=listOf(
    RsAcademyTrackV54("jab","Jab Fundamentals",6),
    RsAcademyTrackV54("roundhouse","Roundhouse",6),
    RsAcademyTrackV54("defense","Defense",6),
    RsAcademyTrackV54("clinch","Clinching",6),
    RsAcademyTrackV54("combo","Combinations",6),
    RsAcademyTrackV54("footwork","Footwork",6)
)

private fun rsAcademyDoneV54(store:RsStore,id:String)=store.s("academy_v54_"+id,"0").toIntOrNull()?.coerceIn(0,6)?:0
private fun rsSetAcademyDoneV54(store:RsStore,id:String,value:Int)=store.ps("academy_v54_"+id,value.coerceIn(0,6).toString())

private fun rsTrainingUiV54(lang:RsLang,key:String):String{
    val en=mapOf(
        "academy" to "RS Academy","academy_sub" to "Structured skill tracks with saved lesson progress.",
        "techniques" to "Technique Library","techniques_sub" to "Search and study core RS KICKBOX techniques.",
        "home" to "Home Training","home_sub" to "Build a focused home session from your available time.",
        "workout" to "Workout Generator","workout_sub" to "Generate a structured kickboxing workout.",
        "compare" to "Technique Compare","compare_sub" to "Compare two techniques side by side.",
        "search" to "Search techniques","lesson" to "lesson","lessons" to "lessons",
        "continue" to "Complete next lesson","reset" to "Reset track","complete" to "COMPLETE",
        "duration" to "Duration","focus" to "Focus","generate" to "Generate plan","new" to "Generate another",
        "warmup" to "Warm-up","technique" to "Technique","conditioning" to "Conditioning","cooldown" to "Cooldown",
        "select_a" to "Technique A","select_b" to "Technique B","power" to "Power","speed" to "Speed",
        "balance" to "Balance","defense" to "Defense","difficulty" to "Difficulty"
    )
    val nl=en+mapOf(
        "academy" to "RS Academy","academy_sub" to "Gestructureerde vaardigheidstrajecten met opgeslagen lesvoortgang.",
        "techniques" to "Techniekbibliotheek","techniques_sub" to "Zoek en bestudeer belangrijke RS KICKBOX-technieken.",
        "home" to "Thuis Training","home_sub" to "Bouw een gerichte thuissessie met de tijd die je hebt.",
        "workout" to "Workout Generator","workout_sub" to "Genereer een gestructureerde kickbokstraining.",
        "compare" to "Techniek Vergelijken","compare_sub" to "Vergelijk twee technieken naast elkaar.",
        "search" to "Zoek technieken","lesson" to "les","lessons" to "lessen",
        "continue" to "Volgende les afronden","reset" to "Traject resetten","complete" to "VOLTOOID",
        "duration" to "Duur","focus" to "Focus","generate" to "Plan genereren","new" to "Nieuwe genereren",
        "warmup" to "Warming-up","technique" to "Techniek","conditioning" to "Conditie","cooldown" to "Cooling-down",
        "select_a" to "Techniek A","select_b" to "Techniek B","power" to "Kracht","speed" to "Snelheid",
        "balance" to "Balans","defense" to "Verdediging","difficulty" to "Moeilijkheid"
    )
    val pack=when(lang.code){"nl"->nl;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsAcademyV54(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){
        RsCloudAcademyV86(c,lang)
        return
    }
    var revision by remember{mutableIntStateOf(0)}
    RsScroll(c,rsTrainingUiV54(lang,"academy"),rsTrainingUiV54(lang,"academy_sub")){
        rsAcademyTracksV54.forEach{track->
            val done=remember(revision,track.id){rsAcademyDoneV54(store,track.id)}
            RsPanel(c){
                Text(track.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(done.toString()+" / "+track.target+" "+rsTrainingUiV54(lang,"lessons"),color=c.muted)
                LinearProgressIndicator(progress={done.toFloat()/track.target},modifier=Modifier.fillMaxWidth())
                if(done<track.target){
                    Button(
                        onClick={rsSetAcademyDoneV54(store,track.id,done+1);revision++},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsTrainingUiV54(lang,"continue"))}
                }else Text(rsTrainingUiV54(lang,"complete"),color=c.bright,fontWeight=FontWeight.Bold)
                if(done>0)OutlinedButton(
                    onClick={rsSetAcademyDoneV54(store,track.id,0);revision++},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsTrainingUiV54(lang,"reset"))}
            }
        }
    }
}

@Composable
fun RsTechniqueLibraryV54(c:RsPalette,lang:RsLang){
    var query by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("ALL")}
    val filtered=rsTechniquesV54.filter{
        (category=="ALL"||it.category==category)&&
            (query.isBlank()||it.name.contains(query,true)||it.notes.contains(query,true))
    }
    RsScroll(c,rsTrainingUiV54(lang,"techniques"),rsTrainingUiV54(lang,"techniques_sub")){
        OutlinedTextField(query,{query=it.take(100)},label={Text(rsTrainingUiV54(lang,"search"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            listOf("ALL","PUNCH","KICK","DEFENSE","CLINCH").forEach{x->
                FilterChip(selected=category==x,onClick={category=x},label={Text(x,fontSize=8.sp)},modifier=Modifier.weight(1f))
            }
        }
        filtered.forEach{t->
            RsPanel(c){
                Text(t.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=19.sp)
                Text(t.category+" · "+t.difficulty,color=c.muted,fontSize=10.sp)
                Text(t.notes,color=c.text)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("P "+t.power,color=c.muted,modifier=Modifier.weight(1f))
                    Text("S "+t.speed,color=c.muted,modifier=Modifier.weight(1f))
                    Text("B "+t.balance,color=c.muted,modifier=Modifier.weight(1f))
                    Text("D "+t.defense,color=c.muted,modifier=Modifier.weight(1f))
                }
            }
        }
    }
}

private fun rsGenerateWorkoutV54(minutes:Int,focus:String,seed:Int=Random.nextInt()):List<Pair<String,String>>{
    val rnd=Random(seed)
    val focusSet=when(focus){
        "PUNCH"->rsTechniquesV54.filter{it.category=="PUNCH"}
        "KICK"->rsTechniquesV54.filter{it.category=="KICK"}
        "DEFENSE"->rsTechniquesV54.filter{it.category=="DEFENSE"}
        else->rsTechniquesV54
    }
    val picks=focusSet.shuffled(rnd).take(minOf(4,focusSet.size))
    val techniqueMinutes=(minutes-8).coerceAtLeast(6)
    val per=(techniqueMinutes/maxOf(1,picks.size)).coerceAtLeast(2)
    return buildList{
        add("Warm-up" to "4 min · mobility, guard movement and footwork")
        picks.forEach{add(it.name to per.toString()+" min · "+it.notes)}
        add("Conditioning" to "2 min · controlled bodyweight finisher")
        add("Cooldown" to "2 min · breathing and mobility")
    }
}

@Composable
private fun RsWorkoutPlanV54(c:RsPalette,plan:List<Pair<String,String>>){
    plan.forEachIndexed{i,(title,detail)->
        RsPanel(c){
            Text((i+1).toString()+". "+title,color=c.bright,fontWeight=FontWeight.Black)
            Text(detail,color=c.text)
        }
    }
}

@Composable
fun RsHomeTrainingV54(c:RsPalette,store:RsStore,lang:RsLang){
    var minutes by remember{mutableFloatStateOf(store.s("home_minutes_v54","20").toFloatOrNull()?:20f)}
    var focus by remember{mutableStateOf(store.s("home_focus_v54","ALL"))}
    var plan by remember{mutableStateOf<List<Pair<String,String>>>(emptyList())}
    RsScroll(c,rsTrainingUiV54(lang,"home"),rsTrainingUiV54(lang,"home_sub")){
        RsPanel(c){
            Text(rsTrainingUiV54(lang,"duration")+" · "+minutes.toInt()+" min",color=c.text)
            Slider(minutes,{minutes=it},valueRange=10f..45f,steps=6)
            Text(rsTrainingUiV54(lang,"focus"),color=c.muted)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","PUNCH","KICK","DEFENSE").forEach{x->
                    FilterChip(selected=focus==x,onClick={focus=x},label={Text(x,fontSize=8.sp)},modifier=Modifier.weight(1f))
                }
            }
            Button(
                onClick={
                    store.ps("home_minutes_v54",minutes.toInt().toString());store.ps("home_focus_v54",focus)
                    plan=rsGenerateWorkoutV54(minutes.toInt(),focus)
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsTrainingUiV54(lang,"generate"))}
        }
        RsWorkoutPlanV54(c,plan)
    }
}

@Composable
fun RsWorkoutGeneratorV54(c:RsPalette,lang:RsLang){
    var minutes by remember{mutableFloatStateOf(30f)}
    var focus by remember{mutableStateOf("ALL")}
    var seed by remember{mutableIntStateOf(0)}
    var generated by remember{mutableStateOf(false)}
    val plan=remember(seed,generated,minutes,focus){if(generated)rsGenerateWorkoutV54(minutes.toInt(),focus,seed) else emptyList()}
    RsScroll(c,rsTrainingUiV54(lang,"workout"),rsTrainingUiV54(lang,"workout_sub")){
        RsPanel(c){
            Text(rsTrainingUiV54(lang,"duration")+" · "+minutes.toInt()+" min",color=c.text)
            Slider(minutes,{minutes=it},valueRange=15f..60f,steps=8)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","PUNCH","KICK","DEFENSE").forEach{x->
                    FilterChip(selected=focus==x,onClick={focus=x},label={Text(x,fontSize=8.sp)},modifier=Modifier.weight(1f))
                }
            }
            Button(onClick={seed=Random.nextInt();generated=true},modifier=Modifier.fillMaxWidth()){
                Text(if(generated)rsTrainingUiV54(lang,"new") else rsTrainingUiV54(lang,"generate"))
            }
        }
        RsWorkoutPlanV54(c,plan)
    }
}

@Composable
fun RsTechniqueCompareV54(c:RsPalette,lang:RsLang){
    var aId by remember{mutableStateOf("jab")}
    var bId by remember{mutableStateOf("roundhouse")}
    val a=rsTechniquesV54.first{it.id==aId}
    val b=rsTechniquesV54.first{it.id==bId}
    RsScroll(c,rsTrainingUiV54(lang,"compare"),rsTrainingUiV54(lang,"compare_sub")){
        Text(rsTrainingUiV54(lang,"select_a"),color=c.muted)
        rsTechniquesV54.chunked(3).forEach{row->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                row.forEach{t->FilterChip(selected=aId==t.id,onClick={aId=t.id},label={Text(t.name,fontSize=8.sp)},modifier=Modifier.weight(1f))}
                repeat(3-row.size){Spacer(Modifier.weight(1f))}
            }
        }
        Text(rsTrainingUiV54(lang,"select_b"),color=c.muted)
        rsTechniquesV54.chunked(3).forEach{row->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                row.forEach{t->FilterChip(selected=bId==t.id,onClick={bId=t.id},label={Text(t.name,fontSize=8.sp)},modifier=Modifier.weight(1f))}
                repeat(3-row.size){Spacer(Modifier.weight(1f))}
            }
        }
        RsPanel(c){
            Text(a.name+"  VS  "+b.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
            listOf(
                rsTrainingUiV54(lang,"power") to (a.power to b.power),
                rsTrainingUiV54(lang,"speed") to (a.speed to b.speed),
                rsTrainingUiV54(lang,"balance") to (a.balance to b.balance),
                rsTrainingUiV54(lang,"defense") to (a.defense to b.defense)
            ).forEach{(label,values)->
                Text(label+" · "+values.first+" / "+values.second,color=c.text)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    LinearProgressIndicator(progress={values.first/100f},modifier=Modifier.weight(1f))
                    LinearProgressIndicator(progress={values.second/100f},modifier=Modifier.weight(1f))
                }
            }
            Text(a.name+": "+a.notes,color=c.muted)
            Text(b.name+": "+b.notes,color=c.muted)
        }
    }
}


@Composable
private fun RsCloudAcademyV86(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var progress by remember{mutableStateOf<Map<String,Int>>(emptyMap())}
    var loading by remember{mutableStateOf(true)}
    var busyTrack by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsCloudAcademyProgressV86()
            .onSuccess{progress=it}
            .onFailure{status=it.message?:"Could not load Academy progress."}
        loading=false
    }

    RsScroll(c,rsTrainingUiV54(lang,"academy"),"Your Academy progress is synchronized with your RS KICKBOX account."){
        RsPanel(c){
            Text(
                if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        rsAcademyTracksV54.forEach{track->
            val done=(progress[track.id]?:0).coerceIn(0,track.target)
            RsPanel(c){
                Text(track.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(done.toString()+" / "+track.target+" "+rsTrainingUiV54(lang,"lessons"),color=c.muted)
                LinearProgressIndicator(
                    progress={if(track.target<=0)0f else done.toFloat()/track.target},
                    modifier=Modifier.fillMaxWidth()
                )
                if(done<track.target){
                    Button(
                        onClick={
                            busyTrack=track.id
                            scope.launch{
                                rsSetCloudAcademyProgressV86(track.id,done+1)
                                    .onSuccess{status=rsCloudT93(lang,"academy_saved");revision++}
                                    .onFailure{status=it.message?:"Could not save Academy progress."}
                                busyTrack=null
                            }
                        },
                        enabled=busyTrack==null,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(busyTrack==track.id)rsCloudT93(lang,"saving") else rsTrainingUiV54(lang,"continue"))}
                }else{
                    Text(rsTrainingUiV54(lang,"complete"),color=c.bright,fontWeight=FontWeight.Bold)
                }
                if(done>0){
                    OutlinedButton(
                        onClick={
                            busyTrack=track.id
                            scope.launch{
                                rsSetCloudAcademyProgressV86(track.id,0)
                                    .onSuccess{status=rsCloudT93(lang,"academy_reset");revision++}
                                    .onFailure{status=it.message?:"Could not reset Academy progress."}
                                busyTrack=null
                            }
                        },
                        enabled=busyTrack==null,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsTrainingUiV54(lang,"reset"))}
                }
            }
        }
    }
}

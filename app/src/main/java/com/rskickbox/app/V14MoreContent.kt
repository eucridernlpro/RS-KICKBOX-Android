package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun RsInfoModule(c:RsPalette,title:String,sub:String,items:List<Pair<String,String>>,action:String="Open"){
    RsScroll(c,title,sub){
        items.forEach{(name,body)->
            RsPanel(c){
                Text(name,color=c.bright,fontWeight=FontWeight.Bold)
                Text(body,color=c.muted)
                OutlinedButton(onClick={},modifier=Modifier.fillMaxWidth()){Text(action)}
            }
        }
    }
}

@Composable
fun RsHomeworkHub(c:RsPalette){
    RsScroll(c,"Homework","Coach-assigned practice for the week."){
        listOf(
            Triple("Jab recovery · 3 rounds","Due Friday","3 x 2:00 shadowboxing rounds. Reset guard after every jab."),
            Triple("Footwork square","Due Sunday","Forward, back, left and right without crossing the feet."),
            Triple("Defense journal","Optional","Write down three moments where you stayed composed under pressure.")
        ).forEachIndexed{i,(a,b,d)->
            var done by remember{mutableStateOf(i==0)}
            RsPanel(c){Text(a,color=c.bright,fontWeight=FontWeight.Bold);Text(b,color=c.text);Text(d,color=c.muted);Button(onClick={done=!done},modifier=Modifier.fillMaxWidth()){Text(if(done)"✓ Completed" else "Mark complete")}}
        }
    }
}

@Composable
fun RsFavoritesHub(c:RsPalette)=RsInfoModule(c,"Saved & Favorites","Your bookmarked techniques, lessons and sessions.",listOf(
    "Roundhouse Kick" to "Technique card · hip turn, pivot and balanced recovery",
    "Defense & Counters" to "Academy lesson · 61% complete",
    "20 min Fundamentals" to "Saved home session",
    "Coach Demo · Jab Recovery" to "Training media · 02:14"
),"Open saved item")

@Composable
fun RsHistoryHub(c:RsPalette)=RsInfoModule(c,"Training History","Recent sessions, attendance and completed learning.",listOf(
    "16 Sep · Fundamentals" to "60 min · attended · 420 XP",
    "14 Sep · Home Training" to "24 min · completed · 180 XP",
    "12 Sep · Advanced Pads" to "75 min · attended · 510 XP",
    "10 Sep · Academy" to "Roundhouse Kick lesson · 82% score"
),"View details")

@Composable
fun RsPrivateLessonsHub(c:RsPalette){
    var booked by remember{mutableStateOf(false)}
    RsScroll(c,"Private Lessons","Book focused one-to-one coaching sessions."){
        RsPanel(c){Text("Coach Ricardo",color=c.bright,fontWeight=FontWeight.Bold);Text("Technique correction · padwork · fight preparation · confidence building",color=c.muted)}
        listOf("Tue 22 Sep · 17:00","Thu 24 Sep · 16:00","Sat 26 Sep · 12:30").forEach{slot->RsPanel(c){Text(slot,color=c.text,fontWeight=FontWeight.Bold);Button(onClick={booked=!booked},modifier=Modifier.fillMaxWidth()){Text(if(booked)"Request sent" else "Request lesson")}}}
    }
}

@Composable
fun RsProfileHub(c:RsPalette){
    RsScroll(c,"My Profile","Member identity, goals and club information."){
        RsPanel(c){Text("ALEX MEMBER",color=c.bright,fontWeight=FontWeight.Black);Text("RS PRO · Level 3 · 12,480 XP",color=c.text);Text("Primary goal · stronger fundamentals and cleaner defense",color=c.muted)}
        RsPanel(c){Text("TRAINING GOALS",color=c.bright,fontWeight=FontWeight.Bold);Text("2–3 club sessions weekly\n1 home technique session\nImprove jab recovery and footwork",color=c.text);OutlinedButton(onClick={},modifier=Modifier.fillMaxWidth()){Text("Edit goals")}}
        RsPanel(c){Text("EMERGENCY & CONTACT",color=c.bright,fontWeight=FontWeight.Bold);Text("Emergency contact and medical notes are private and visible only where needed for club safety.",color=c.muted);OutlinedButton(onClick={},modifier=Modifier.fillMaxWidth()){Text("Manage details")}}
    }
}

@Composable
fun RsGroupsHub(c:RsPalette)=RsInfoModule(c,"Groups","Club spaces for focused communication.",listOf(
    "RS General" to "124 members · announcements and club conversation",
    "Beginners" to "42 members · fundamentals questions and homework",
    "Fight Team" to "18 members · camp planning and competition updates",
    "Parents & Youth" to "31 members · schedule and youth information"
),"Open group")

@Composable
fun RsSearchHub(c:RsPalette){
    var q by remember{mutableStateOf("")}
    RsScroll(c,"Search","Search the RS app across techniques, lessons, media and events."){
        RsPanel(c){OutlinedTextField(q,{q=it},label={Text("Search RS KICKBOX")},modifier=Modifier.fillMaxWidth());Button(onClick={},enabled=q.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Search")}}
        RsPanel(c){Text("POPULAR",color=c.bright,fontWeight=FontWeight.Bold);Text("Jab · Roundhouse · Defense · Footwork · Home training · Fight camp",color=c.muted)}
    }
}

@Composable
fun RsHomeworkManagerV14(c:RsPalette)=RsInfoModule(c,"Homework Manager","Assign, review and track student practice.",listOf(
    "Beginners · Jab recovery" to "18 assigned · 12 complete · due Friday",
    "Advanced · Exit angles" to "11 assigned · 7 complete · due Sunday",
    "Fight Team · Defense journal" to "8 assigned · 8 complete"
),"Manage assignment")

@Composable
fun RsMusicManagerV14(c:RsPalette)=RsInfoModule(c,"Music Manager","Manage playlists used during sessions. Only use music you are licensed or permitted to play.",listOf(
    "Warm-up playlist" to "28 min · 8 tracks",
    "Pads intensity" to "41 min · 12 tracks",
    "Cooldown" to "19 min · 6 tracks"
),"Manage playlist")

@Composable
fun RsLessonEditorV14(c:RsPalette)=RsInfoModule(c,"Lesson Editor","Create and organize premium Academy lessons.",listOf(
    "Jab Fundamentals" to "6 sections · video + drill + quiz",
    "Roundhouse Kick" to "7 sections · video + slow-motion points + drill",
    "Defense & Counters" to "5 sections · shell, parry, slip and return"
),"Edit lesson")

@Composable
fun RsMembershipPlansV14(c:RsPalette)=RsInfoModule(c,"Membership Plans","Configure what each subscription includes before individual overrides.",listOf(
    "RS BASIC" to "Classes · basic Academy · community · profile",
    "RS PRO" to "Basic + AI Coach · full Academy · home training · progress",
    "RS ELITE" to "Pro + Fight Camp · premium vault · advanced analysis · priority coaching"
),"Edit plan")

@Composable
fun RsProgressManagerV14(c:RsPalette)=RsInfoModule(c,"Progress Manager","Review student development and coaching priorities.",listOf(
    "Alex Member" to "Attendance 91% · Technique 72% · Streak 18 days",
    "Sofia R." to "Attendance 88% · Technique 79% · Streak 11 days",
    "Noah K." to "Attendance 76% · Technique 64% · Streak 6 days"
),"Open progress")

@Composable
fun RsAssessmentsV14(c:RsPalette)=RsInfoModule(c,"Coach Assessments","Structured coach notes and level reviews.",listOf(
    "Alex Member · Sep review" to "Guard recovery improved · next focus: relaxed cross and exit",
    "Sofia R. · Sep review" to "Strong balance · next focus: defensive counters",
    "Noah K. · Sep review" to "Good effort · next focus: stance consistency"
),"Open assessment")

@Composable
fun RsEventManagerV14(c:RsPalette)=RsInfoModule(c,"Event Manager","Create seminars, club days and competition events.",listOf(
    "Technique Seminar · 27 Sep" to "34 registered · capacity 40",
    "Open Mat · 04 Oct" to "21 registered · capacity 30",
    "RS Club Sparring Day · 18 Oct" to "18 registered · controlled rounds"
),"Manage event")

@Composable
fun RsQrAttendanceV14(c:RsPalette){
    RsScroll(c,"QR Attendance","Fast check-in workflow for club sessions."){
        RsPanel(c){Text("TODAY · FUNDAMENTALS 18:00",color=c.bright,fontWeight=FontWeight.Bold);Text("14 booked · 11 checked in",color=c.muted);Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Open QR scanner")};OutlinedButton(onClick={},modifier=Modifier.fillMaxWidth()){Text("Manual check-in")}}
        RsPanel(c){Text("LAST CHECK-INS",color=c.bright,fontWeight=FontWeight.Bold);Text("18:01 · Alex Member\n17:59 · Sofia R.\n17:58 · Noah K.",color=c.text)}
    }
}

@Composable
fun RsSessionBuilderV14(c:RsPalette){
    var rounds by remember{mutableFloatStateOf(5f)}
    RsScroll(c,"Session Builder","Build reusable round structures for class or home training."){
        RsPanel(c){Text("ROUNDS ${rounds.toInt()}",color=c.bright,fontWeight=FontWeight.Bold);Slider(rounds,{rounds=it},valueRange=3f..12f,steps=8);Text("Work 2:00 · Rest 0:45 · Cue: Jab · Cross · Low Kick",color=c.muted);Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Save session template")}}
    }
}

@Composable
fun RsChallengeManagerV14(c:RsPalette)=RsInfoModule(c,"Challenge Manager","Create safe club challenges and XP rewards.",listOf(
    "3 Session Week" to "250 XP · active · 46 participants",
    "Defense Focus" to "180 XP · active · 31 participants",
    "Footwork Flow" to "150 XP · scheduled next week"
),"Manage challenge")

@Composable
fun RsFightCampManagerV14(c:RsPalette)=RsInfoModule(c,"Fight Camp Manager","Plan technical, conditioning, controlled sparring and recovery blocks.",listOf(
    "Alex Member · Week 3/8" to "Technical quality under fatigue",
    "Sofia R. · Week 5/8" to "Distance control and composure",
    "Fight Team template" to "8-week reusable camp structure"
),"Open camp")

@Composable
fun RsLandingManagerV14(c:RsPalette)=RsInfoModule(c,"Promotion & Landing Manager","Control public-facing promotion cards and app callouts.",listOf(
    "Join RS KICKBOX" to "Primary membership promotion",
    "Van Stilte Naar Strijd" to "Trainer book promotion",
    "Technique Seminar" to "Event banner · 27 Sep"
),"Edit promotion")

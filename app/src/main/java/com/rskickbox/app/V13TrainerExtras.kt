package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RsNotificationsCenter(c:RsPalette){
    var feedback by remember{mutableStateOf("")}
    RsScroll(c,"Notification Center","Operational messaging to students and groups."){
        if(feedback.isNotBlank()) RsPanel(c){Text(feedback,color=c.bright,fontWeight=FontWeight.Bold)}
        listOf("Class reminder|Fundamentals starts at 18:00|Students booked","Payment reminder|Membership renewal due in 3 days|Selected members","Club news|New technique seminar announced|All members").forEach{r->val x=r.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.text);Text(x[2],color=c.muted);OutlinedButton(onClick={feedback="Notification editor opened · ${x[0]}"},modifier=Modifier.fillMaxWidth()){Text("Edit / send")}}}
        Button(onClick={feedback="New notification composer opened"},modifier=Modifier.fillMaxWidth()){Text("+ New notification")}
    }
}

@Composable
fun RsAnalytics(c:RsPalette){RsScroll(c,"Club Analytics","High-level performance preview for the trainer dashboard."){
    RsPanel(c){Text("124 MEMBERS",color=c.bright,fontWeight=FontWeight.Black);Text("+8 this month · 86% active",color=c.muted)}
    listOf("Class occupancy|78%","Monthly attendance|1,186 check-ins","Member retention|91%","Academy completion|64%","Outstanding payments|€392").forEach{r->val x=r.split('|');RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(x[0],color=c.text);Text(x[1],color=c.bright,fontWeight=FontWeight.Bold)}}}
}}

@Composable
fun RsDocuments(c:RsPalette){
    var opened by remember{mutableStateOf("")}
    RsScroll(c,"Documents & Waivers","Manage agreements and club documents."){
        if(opened.isNotBlank()) RsPanel(c){Text("Opened · $opened",color=c.bright,fontWeight=FontWeight.Bold)}
        listOf("Membership agreement|118/124 signed","Privacy acknowledgement|124/124 signed","Training rules|124/124 acknowledged","Event waiver|42/60 signed").forEach{r->val x=r.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.muted);OutlinedButton(onClick={opened=x[0]},modifier=Modifier.fillMaxWidth()){Text("Open manager")}}}
    }
}

@Composable
fun RsSupport(c:RsPalette){
    var supportCreated by remember{mutableStateOf(false)}
    var qcRun by remember{mutableStateOf(false)}
    RsScroll(c,"Support & Final QC","Club support queue plus the final trainer-side app control check."){
        listOf("#1042 · Login issue · Open","#1041 · Membership question · Waiting","#1038 · Booking problem · Resolved").forEach{RsPanel(c){Text(it,color=c.text)}}
        Button(onClick={supportCreated=true},modifier=Modifier.fillMaxWidth()){Text(if(supportCreated)"✓ Support item created" else "Create support item")}
        RsPanel(c){
            Text("FINAL QC CONTROL",color=c.bright,fontWeight=FontWeight.Black)
            Text("Checks the main preview areas before the next release candidate.",color=c.muted)
            listOf(
                "Login & language selector",
                "Student dashboard links",
                "Trainer dashboard links",
                "Theme & Background Studio",
                "Uploaded background library picker",
                "AI voice coach / microphone entry",
                "Session timer controls",
                "Class booking / homework / challenges",
                "Access & subscription controls",
                "Payment Center configuration",
                "Admin managers and content editors",
                "Safe scrolling and screen borders"
            ).forEach{Text((if(qcRun)"✓ " else "○ ")+it,color=if(qcRun)c.bright else c.text)}
            Button(onClick={qcRun=true},modifier=Modifier.fillMaxWidth()){Text(if(qcRun)"✓ QC preview completed" else "Run final QC preview")}
            if(qcRun) Text("UI/navigation checks passed in this local preview. Cloud auth, real payments, push delivery and production backend remain separate release tasks.",color=c.muted)
        }
    }
}

@Composable
fun RsReferrals(c:RsPalette){RsScroll(c,"Referrals","Member referral preview."){
    RsPanel(c){Text("28 INVITES",color=c.bright,fontWeight=FontWeight.Black);Text("9 joined · 32% conversion",color=c.muted)}
    listOf("Alex · 3 invites · 1 joined","Sofia · 5 invites · 2 joined","Mila · 2 invites · 1 joined").forEach{RsPanel(c){Text(it,color=c.text)}}
}}

@Composable
fun RsTrainerSchedule(c:RsPalette){
    var opened by remember{mutableStateOf("")}
    RsScroll(c,"Trainer Schedule","Daily coaching overview and planning preview."){
        if(opened.isNotBlank()) RsPanel(c){Text("Opened · $opened",color=c.bright,fontWeight=FontWeight.Bold)}
        listOf("17:00 · Private lesson · Alex","18:00 · Fundamentals · 18 capacity","19:15 · Advanced Pads · 12 capacity","20:30 · Sparring Lab · 10 capacity").forEach{item->RsPanel(c){Text(item,color=c.bright,fontWeight=FontWeight.Bold);OutlinedButton(onClick={opened=item},modifier=Modifier.fillMaxWidth()){Text("Open")}}}
    }
}

@Composable
fun RsContentManager(c:RsPalette){
    var feedback by remember{mutableStateOf("")}
    RsScroll(c,"Content Manager","Manage academy lessons, technique cards and training media."){
        if(feedback.isNotBlank()) RsPanel(c){Text(feedback,color=c.bright,fontWeight=FontWeight.Bold)}
        listOf("Academy lessons · 24 published","Technique cards · 48 published","Training videos · 37 published","Draft content · 6 items").forEach{item->RsPanel(c){Text(item,color=c.text);OutlinedButton(onClick={feedback="Manager opened · $item"},modifier=Modifier.fillMaxWidth()){Text("Manage")}}}
        Button(onClick={feedback="Create content editor opened"},modifier=Modifier.fillMaxWidth()){Text("+ Create content")}
    }
}

@Composable
fun RsCoachNotes(c:RsPalette){RsScroll(c,"Coach Notes","Private coaching observations and follow-up preview."){
    listOf("Alex|Guard recovery improving; revisit rear-hand return next session.","Sofia|Strong consistency. Add more controlled footwork transitions.","Noah|Keep intensity moderate while rebuilding attendance rhythm.").forEach{r->val x=r.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.text)}}
}}

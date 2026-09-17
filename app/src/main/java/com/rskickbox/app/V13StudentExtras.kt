package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RsTechniqueLibrary(c:RsPalette){RsScroll(c,"Smart Technique Library","Searchable kickboxing reference with coaching points and drill links."){
    listOf("Jab|Punch|Fast extension · shoulder protection · immediate recovery","Cross|Punch|Hip rotation · rear heel release · guard returns","Roundhouse Kick|Kick|Pivot · hip turn · shin contact · balanced recovery","Teep|Kick|Knee lift · hip drive · recoil · stance reset","Slip & Return|Defense|Small head movement · eyes forward · counter immediately").forEach{r->val x=r.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.muted);Text(x[2],color=c.text);Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Open technique")}}}
}}

@Composable
fun RsHomeTraining(c:RsPalette){RsScroll(c,"Home Training Mode","Structured sessions for training outside the gym."){
    listOf("20 min Fundamentals|Warm-up · shadowboxing · defense · cooldown","30 min Technique Flow|Footwork · combinations · controlled tempo","25 min Conditioning|Low-impact intervals · core · mobility").forEach{r->val x=r.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.muted);Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Start session")}}}
}}

@Composable
fun RsWorkoutGenerator(c:RsPalette){var duration by remember{mutableFloatStateOf(30f)};var focus by remember{mutableStateOf("Technique")};RsScroll(c,"Workout Generator","Create a session based on time and focus."){
    RsPanel(c){Text("DURATION ${duration.toInt()} MIN",color=c.bright,fontWeight=FontWeight.Bold);Slider(duration,{duration=it},valueRange=15f..60f);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Technique","Conditioning","Mixed").forEach{FilterChip(selected=focus==it,onClick={focus=it},label={Text(it)})}};Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("Generate workout")}}
    RsPanel(c){Text("PREVIEW",color=c.bright,fontWeight=FontWeight.Bold);Text("5 min warm-up · technique rounds · movement rounds · controlled conditioning · cooldown",color=c.text)}
}}

@Composable
fun RsBadges(c:RsPalette){RsScroll(c,"Badges & Achievements","Milestones for consistency, learning and positive club participation."){
    listOf("🥇 First 10 Sessions · Unlocked","🔥 14 Day Streak · Unlocked","🥊 Defense Student · Unlocked","⚡ Combo Builder · 72% progress","👑 RS Consistency · 48% progress").forEach{RsPanel(c){Text(it,color=c.bright,fontWeight=FontWeight.Bold)}}
}}

@Composable
fun RsKnowledgeVault(c:RsPalette){RsScroll(c,"RS Knowledge Vault","Education beyond individual techniques."){
    listOf("Training Principles|Balance, range, rhythm and recovery","Fight IQ|Reading patterns and managing distance","Recovery|Sleep, mobility, hydration and training load basics","Mindset|Discipline, composure and respectful competition","Equipment Guide|Gloves, wraps, shin guards and care").forEach{r->val x=r.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.muted);OutlinedButton(onClick={},modifier=Modifier.fillMaxWidth()){Text("Read")}}}
}}

@Composable
fun RsTechniqueCompare(c:RsPalette){RsScroll(c,"Technique Comparison","Compare coaching points side by side before practicing."){
    RsPanel(c){Text("JAB vs CROSS",color=c.bright,fontWeight=FontWeight.Black);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){Text("JAB",color=c.bright);Text("Lead hand\nSpeed\nRange finder\nFast recovery",color=c.text)};Column(Modifier.weight(1f)){Text("CROSS",color=c.bright);Text("Rear hand\nHip drive\nPower line\nRear rotation",color=c.text)}}}
}}

@Composable
fun RsPrivateCoachChat(c:RsPalette){var msg by remember{mutableStateOf("")};RsScroll(c,"Private Coach Chat","Direct student-to-coach communication preview."){
    RsPanel(c){Text("Coach Ricardo",color=c.bright,fontWeight=FontWeight.Bold);Text("Your jab recovery looks cleaner. Next session focus on staying relaxed before the cross.",color=c.text)}
    RsPanel(c){OutlinedTextField(msg,{msg=it},label={Text("Message coach")},modifier=Modifier.fillMaxWidth());Button(onClick={msg=""},enabled=msg.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Send privately")}}
}}

@Composable
fun RsEvents(c:RsPalette){RsScroll(c,"RS Events","Club events and seminars."){
    listOf("Technique Seminar · 27 Sep · 14:00","Open Mat · 04 Oct · 11:00","RS Club Sparring Day · 18 Oct · 13:00").forEachIndexed{i,t->var joined by remember{mutableStateOf(i==0)};RsPanel(c){Text(t,color=c.bright,fontWeight=FontWeight.Bold);Button(onClick={joined=!joined},modifier=Modifier.fillMaxWidth()){Text(if(joined)"Registered" else "Register")}}}
}}
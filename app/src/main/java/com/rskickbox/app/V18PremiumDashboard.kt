package com.rskickbox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RsDashItemV18(val route:String,val title:String,val hint:String,val kind:String)
private data class RsDashSectionV18(val title:String,val items:List<RsDashItemV18>)

@Composable
fun RsPremiumDashboardV18(c:RsPalette, role:RsRole, lang:RsLang, onRoute:(String)->Unit){
    val sections = if(role==RsRole.TRAINER) trainerSectionsV18() else studentSectionsV18()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ){
        RsPanel(c){
            Text(if(role==RsRole.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
            Text("${lang.name} · ${if(role==RsRole.TRAINER)"Trainer / Admin" else "Student"} · Premium Visual Dashboard",color=c.muted)
        }
        sections.forEach { section ->
            Text(section.title.uppercase(),color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp,letterSpacing=.7.sp,modifier=Modifier.padding(horizontal=4.dp))
            section.items.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    pair.forEach { item ->
                        Box(Modifier.weight(1f)){ RsPremiumTileV18(c,item,onRoute) }
                    }
                    if(pair.size==1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun RsPremiumTileV18(c:RsPalette,item:RsDashItemV18,onRoute:(String)->Unit){
    val accent = accentV18(item.kind,c)
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().height(188.dp).clip(shape)
            .background(Brush.linearGradient(listOf(c.panel2,accent.copy(alpha=.30f),c.panel)))
            .clickable{onRoute(item.route)}
    ){
        Canvas(Modifier.fillMaxSize()){
            drawCircle(accent.copy(alpha=.19f),size.minDimension*.82f,Offset(size.width*.88f,size.height*.12f))
            drawCircle(c.bright.copy(alpha=.055f),size.minDimension*.52f,Offset(size.width*.70f,size.height*.80f),style=Stroke(width=5f))
            drawFeatureArtworkV18(item.kind,accent)
            drawLine(c.bright.copy(alpha=.18f),Offset(0f,size.height*.79f),Offset(size.width,size.height*.69f),2f)
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.04f),Color.Black.copy(alpha=.18f),Color.Black.copy(alpha=.72f)))))
        Surface(
            modifier=Modifier.align(Alignment.TopStart).padding(13.dp),
            shape=RoundedCornerShape(12.dp),
            color=Color.Black.copy(alpha=.38f),
            border=androidx.compose.foundation.BorderStroke(1.dp,c.bright.copy(alpha=.40f))
        ){
            Text(featureGlyphV18(item.kind),color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text(item.title,color=c.bright,fontWeight=FontWeight.Bold,fontSize=17.sp,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=20.sp)
            Text(item.hint,color=Color.White.copy(alpha=.72f),fontSize=10.sp,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=13.sp)
        }
        Surface(
            modifier=Modifier.align(Alignment.TopEnd).padding(12.dp),
            shape=RoundedCornerShape(20.dp),
            color=accent.copy(alpha=.22f)
        ){
            Text("›",color=c.bright,fontSize=19.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(horizontal=9.dp,vertical=3.dp))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFeatureArtworkV18(kind:String,accent:Color){
    val w=size.width; val h=size.height
    when(kind){
        "ai" -> {
            repeat(5){i->
                val x=w*(.48f+i*.075f); val amp=h*(.04f+(i%3)*.018f)
                drawLine(accent.copy(alpha=.48f),Offset(x,h*.33f-amp),Offset(x,h*.33f+amp),8f)
            }
            drawCircle(accent.copy(alpha=.23f),w*.12f,Offset(w*.69f,h*.34f),style=Stroke(7f))
        }
        "timer" -> {
            drawCircle(accent.copy(alpha=.26f),w*.15f,Offset(w*.72f,h*.35f),style=Stroke(8f))
            drawLine(accent.copy(alpha=.56f),Offset(w*.72f,h*.35f),Offset(w*.72f,h*.26f),7f)
            drawLine(accent.copy(alpha=.56f),Offset(w*.72f,h*.35f),Offset(w*.79f,h*.38f),7f)
        }
        "academy","book" -> {
            drawLine(accent.copy(alpha=.5f),Offset(w*.55f,h*.22f),Offset(w*.55f,h*.54f),7f)
            drawLine(accent.copy(alpha=.36f),Offset(w*.55f,h*.23f),Offset(w*.83f,h*.18f),5f)
            drawLine(accent.copy(alpha=.36f),Offset(w*.55f,h*.23f),Offset(w*.31f,h*.18f),5f)
            repeat(3){i-> drawLine(accent.copy(alpha=.26f),Offset(w*.58f,h*(.29f+i*.07f)),Offset(w*.80f,h*(.27f+i*.07f)),3f)}
        }
        "fight","technique","training" -> {
            drawCircle(Color.Black.copy(alpha=.60f),w*.055f,Offset(w*.72f,h*.25f))
            drawLine(Color.Black.copy(alpha=.68f),Offset(w*.72f,h*.31f),Offset(w*.67f,h*.54f),15f)
            drawLine(Color.Black.copy(alpha=.68f),Offset(w*.70f,h*.36f),Offset(w*.86f,h*.29f),12f)
            drawLine(Color.Black.copy(alpha=.68f),Offset(w*.68f,h*.52f),Offset(w*.57f,h*.70f),14f)
            drawLine(Color.Black.copy(alpha=.68f),Offset(w*.68f,h*.52f),Offset(w*.80f,h*.68f),14f)
            drawCircle(accent.copy(alpha=.20f),w*.21f,Offset(w*.72f,h*.42f),style=Stroke(6f))
        }
        "progress","analytics" -> {
            val xs=listOf(.49f,.60f,.71f,.82f); val hs=listOf(.19f,.29f,.39f,.53f)
            xs.indices.forEach{i->drawRect(accent.copy(alpha=.28f+i*.07f),Offset(w*xs[i],h*(.66f-hs[i])),androidx.compose.ui.geometry.Size(w*.06f,h*hs[i]))}
            drawLine(accent.copy(alpha=.55f),Offset(w*.47f,h*.60f),Offset(w*.86f,h*.24f),5f)
        }
        "community","chat" -> {
            drawCircle(accent.copy(alpha=.36f),w*.075f,Offset(w*.60f,h*.31f))
            drawCircle(accent.copy(alpha=.28f),w*.075f,Offset(w*.78f,h*.31f))
            drawRoundRect(accent.copy(alpha=.19f),Offset(w*.48f,h*.44f),androidx.compose.ui.geometry.Size(w*.43f,h*.18f),androidx.compose.ui.geometry.CornerRadius(18f,18f))
        }
        "payment" -> {
            drawRoundRect(accent.copy(alpha=.22f),Offset(w*.48f,h*.23f),androidx.compose.ui.geometry.Size(w*.40f,h*.31f),androidx.compose.ui.geometry.CornerRadius(18f,18f))
            drawLine(accent.copy(alpha=.55f),Offset(w*.52f,h*.32f),Offset(w*.84f,h*.32f),8f)
            drawCircle(accent.copy(alpha=.5f),w*.035f,Offset(w*.80f,h*.47f))
        }
        "music" -> {
            drawCircle(accent.copy(alpha=.34f),w*.09f,Offset(w*.62f,h*.52f),style=Stroke(7f))
            drawCircle(accent.copy(alpha=.34f),w*.09f,Offset(w*.80f,h*.46f),style=Stroke(7f))
            drawLine(accent.copy(alpha=.60f),Offset(w*.70f,h*.22f),Offset(w*.70f,h*.49f),7f)
            drawLine(accent.copy(alpha=.60f),Offset(w*.88f,h*.17f),Offset(w*.88f,h*.43f),7f)
            drawLine(accent.copy(alpha=.60f),Offset(w*.70f,h*.22f),Offset(w*.88f,h*.17f),7f)
            repeat(4){i->drawLine(accent.copy(alpha=.24f),Offset(w*(.45f+i*.08f),h*.68f),Offset(w*(.45f+i*.08f),h*(.61f-(i%2)*.07f)),5f)}
        }
        "settings","admin" -> {
            drawCircle(accent.copy(alpha=.25f),w*.15f,Offset(w*.70f,h*.36f),style=Stroke(8f))
            drawCircle(accent.copy(alpha=.42f),w*.055f,Offset(w*.70f,h*.36f),style=Stroke(8f))
            repeat(6){i->
                val a=i*1.047f
                val x1=w*.70f+kotlin.math.cos(a)*w*.16f; val y1=h*.36f+kotlin.math.sin(a)*w*.16f
                val x2=w*.70f+kotlin.math.cos(a)*w*.21f; val y2=h*.36f+kotlin.math.sin(a)*w*.21f
                drawLine(accent.copy(alpha=.38f),Offset(x1,y1),Offset(x2,y2),7f)
            }
        }
        else -> {
            drawCircle(accent.copy(alpha=.18f),w*.18f,Offset(w*.73f,h*.36f),style=Stroke(6f))
            drawCircle(accent.copy(alpha=.24f),w*.09f,Offset(w*.73f,h*.36f),style=Stroke(5f))
        }
    }
}

private fun accentV18(kind:String,c:RsPalette):Color = when(kind){
    "fight" -> Color(0xFFE25B3F)
    "ai" -> Color(0xFF55B9FF)
    "timer" -> Color(0xFFF7B84B)
    "progress","analytics" -> Color(0xFF5ED08B)
    "community","chat" -> Color(0xFFAD7BFF)
    "payment" -> Color(0xFF55C7A8)
    "music" -> Color(0xFF1DB954)
    "academy","book" -> c.bright
    "training","technique" -> Color(0xFFE8A942)
    "settings","admin" -> Color(0xFFB7BDC9)
    else -> c.gold
}

private fun featureGlyphV18(kind:String)=when(kind){
    "ai"->"AI";"timer"->"◴";"academy"->"A";"book"->"B";"fight"->"FX";"training"->"TR";"technique"->"TK";"progress"->"↗";"analytics"->"▥";"community"->"●●";"chat"->"✦";"payment"->"€";"music"->"♫";"settings"->"⚙";"admin"->"RS";else->"♛"
}

private fun studentSectionsV18()=listOf(
    RsDashSectionV18("Core Training",listOf(
        RsDashItemV18("voice","AI Voice Coach","Ask · listen · improve","ai"),
        RsDashItemV18("session","Session Player","Rounds · timer · cues","timer"),
        RsDashItemV18("academy","RS Academy","Structured premium lessons","academy"),
        RsDashItemV18("techniques","Technique Library","Moves · details · drills","technique"),
        RsDashItemV18("home_training","Home Training","Train anywhere","training"),
        RsDashItemV18("workout","Workout Generator","Build your session","training")
    )),
    RsDashSectionV18("Club & Coaching",listOf(
        RsDashItemV18("classes","Classes & Events","Book club training","training"),
        RsDashItemV18("events","RS Events","Seminars · club days","community"),
        RsDashItemV18("coachchat","Private Coach Chat","Direct coach support","chat"),
        RsDashItemV18("community","Community","Club feed · connection","community"),
        RsDashItemV18("groups","Groups","Team spaces","community"),
        RsDashItemV18("private_lessons","Private Lessons","1-to-1 coaching","training")
    )),
    RsDashSectionV18("Performance",listOf(
        RsDashItemV18("progress","Progress","XP · technique · streak","progress"),
        RsDashItemV18("challenges","Challenges","Goals · XP · consistency","progress"),
        RsDashItemV18("badges","Badges","Achievements","progress"),
        RsDashItemV18("fightcamp","Fight Camp","Structured preparation","fight"),
        RsDashItemV18("compare","Technique Compare","Compare movement details","technique"),
        RsDashItemV18("history","Training History","Sessions · attendance","progress")
    )),
    RsDashSectionV18("Library, Music & Account",listOf(
        RsDashItemV18("vault","Knowledge Vault","Premium learning library","book"),
        RsDashItemV18("homework","Homework","Coach assignments","academy"),
        RsDashItemV18("favorites","Saved & Favorites","Your saved content","book"),
        RsDashItemV18("media","Training Media","Videos · uploads","training"),
        RsDashItemV18("music","My Music & Spotify","Playlists · training soundtrack","music"),
        RsDashItemV18("finance","Membership & Payments","Plan · payments · history","payment"),
        RsDashItemV18("book","Trainer Book","Van Stilte Naar Strijd","book"),
        RsDashItemV18("profile","My Profile","Goals · identity","settings"),
        RsDashItemV18("search","Search","Find anything in RS","admin"),
        RsDashItemV18("settings","Settings & Privacy","Preferences · account","settings")
    ))
)

private fun trainerSectionsV18()=listOf(
    RsDashSectionV18("Brand & Experience",listOf(
        RsDashItemV18("themes","Visual Theme Studio","Complete app identity","settings"),
        RsDashItemV18("backgrounds","Background Studio","Live visuals · uploads","training"),
        RsDashItemV18("landing_admin","Promotion Manager","Public app promotions","book"),
        RsDashItemV18("book","Book Manager","Trainer book experience","book")
    )),
    RsDashSectionV18("Coaching, Content & Music",listOf(
        RsDashItemV18("voice","AI Voice Coach","Coach assistant","ai"),
        RsDashItemV18("session","Trainer Session","Round control","timer"),
        RsDashItemV18("lesson_editor","Lesson Editor","Academy content","academy"),
        RsDashItemV18("content","Content Manager","Lessons · cards · media","academy"),
        RsDashItemV18("homework_admin","Homework Manager","Assign · review","academy"),
        RsDashItemV18("session_builder","Session Builder","Reusable training plans","training"),
        RsDashItemV18("music_admin","Music & Spotify Manager","Import · assign · save playlists","music"),
        RsDashItemV18("notes","Coach Notes","Private observations","chat")
    )),
    RsDashSectionV18("Members & Access",listOf(
        RsDashItemV18("members","Student Manager","Profiles · status","admin"),
        RsDashItemV18("access","Access & Subscriptions","Features · overrides","settings"),
        RsDashItemV18("plans_admin","Membership Plans","Basic · Pro · Elite","payment"),
        RsDashItemV18("progress_admin","Progress Manager","Student development","progress"),
        RsDashItemV18("assessments","Coach Assessments","Reviews · priorities","progress"),
        RsDashItemV18("challenge_admin","Challenge Manager","Safe club goals","progress"),
        RsDashItemV18("fightcamp_admin","Fight Camp Manager","Camp planning","fight")
    )),
    RsDashSectionV18("Club Operations",listOf(
        RsDashItemV18("classes","Class Manager","Schedule · capacity","training"),
        RsDashItemV18("attendance","Attendance","Check-in control","admin"),
        RsDashItemV18("qr_attendance","QR Attendance","Fast member check-in","admin"),
        RsDashItemV18("events_admin","Event Manager","Events · capacity","community"),
        RsDashItemV18("schedule","Trainer Schedule","Daily coaching plan","timer"),
        RsDashItemV18("notifications","Notifications","Messages · audiences","chat"),
        RsDashItemV18("documents","Documents & Waivers","Agreements · status","admin"),
        RsDashItemV18("referrals","Referrals","Invites · conversion","community")
    )),
    RsDashSectionV18("Business & Release",listOf(
        RsDashItemV18("payments","Payment Center","Methods · instructions","payment"),
        RsDashItemV18("invoices","Invoices","Revenue · status","payment"),
        RsDashItemV18("analytics","Analytics","Club performance","analytics"),
        RsDashItemV18("support","Support & Final QC","Operations · QA","admin"),
        RsDashItemV18("release","Release & Legal Center","Policies · Play Store","settings"),
        RsDashItemV18("settings","App Settings","Privacy · maintenance","settings")
    ))
)

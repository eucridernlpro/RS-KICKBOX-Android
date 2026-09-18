package com.rskickbox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DashV21(val route:String,val title:String,val hint:String,val kind:String)
private data class SectionV21(val title:String,val items:List<DashV21>)

@Composable
fun RsPremiumDashboardV21(c:RsPalette,store:RsStore,role:RsRole,lang:RsLang,onRoute:(String)->Unit){
    val sections=if(role==RsRole.TRAINER)trainerV21() else studentV21()
    val compactPhone=LocalConfiguration.current.screenWidthDp<380
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(15.dp)){
        RsPanel(c){
            Text(if(role==RsRole.TRAINER)rsT(lang,"trainer_dashboard") else rsT(lang,"student_dashboard"),color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
            Text("${lang.name} · ${if(role==RsRole.TRAINER)rsT(lang,"trainer_admin") else rsT(lang,"student")} · ${rsT(lang,"premium_experience")}",color=c.muted)
        }
        sections.forEach{section->
            Text(sectionTitleV25(lang,section.title).uppercase(),color=c.bright,fontWeight=FontWeight.Black,fontSize=sectionFontV25(sectionTitleV25(lang,section.title)),letterSpacing=.5.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=4.dp))
            section.items.chunked(if(compactPhone)1 else 2).forEach{pair->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    pair.forEach{item->Box(Modifier.weight(1f)){TileV21(c,store,role,lang,item,onRoute)}}
                    if(pair.size==1 && !compactPhone)Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun TileV21(c:RsPalette,store:RsStore,role:RsRole,lang:RsLang,item:DashV21,onRoute:(String)->Unit){
    val badgeCount=rsDashboardBadgeCountV55(store,role,item.route)
    val localizedTitle=rsRouteTitle(lang,item.route,item.title)
    val localizedHint=rsRouteHint(lang,item.route,item.hint)
    val titleSize=tileTitleFontV25(localizedTitle)
    val titleLine=tileTitleLineV25(localizedTitle)
    val hintSize=tileHintFontV25(localizedHint)
    val shape=RoundedCornerShape(26.dp)
    val custom=store.s("visual_v21_tile_${item.route}","")
    val overlay=store.s("visual_v21_opacity_tile_${item.route}","0.52").toFloatOrNull()?:.52f
    Box(Modifier.fillMaxWidth().height(220.dp).clip(shape).background(Brush.linearGradient(listOf(c.panel2,c.gold.copy(alpha=.20f),c.panel))).clickable{onRoute(item.route)}){
        if(custom.isNotBlank())RsUriPreviewV21(custom,Modifier.fillMaxSize(),store.s("visual_v21_pos_tile_${item.route}","CENTER"))
        else DefaultTileArtworkV21(c,item.kind)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.10f),Color.Black.copy(alpha=.28f),Color.Black.copy(alpha=overlay.coerceIn(.35f,.82f))))))
        Surface(modifier=Modifier.align(Alignment.TopStart).padding(14.dp),shape=RoundedCornerShape(13.dp),color=Color.Black.copy(alpha=.46f),border=androidx.compose.foundation.BorderStroke(1.dp,c.bright.copy(alpha=.45f))){
            Text(glyphV21(item.kind),color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))
        }
        Surface(modifier=Modifier.align(Alignment.TopEnd).padding(13.dp),shape=RoundedCornerShape(20.dp),color=c.gold.copy(alpha=.24f)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(horizontal=9.dp,vertical=3.dp)){
                if(badgeCount>0){
                    Surface(shape=RoundedCornerShape(12.dp),color=c.bright){
                        Text(
                            if(badgeCount>99)"99+" else badgeCount.toString(),
                            color=Color.Black,
                            fontSize=9.sp,
                            fontWeight=FontWeight.Black,
                            modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp)
                        )
                    }
                }
                Text("›",color=c.bright,fontSize=20.sp,fontWeight=FontWeight.Black)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text(localizedTitle,color=Color.White,fontWeight=FontWeight.Black,fontSize=titleSize,maxLines=3,overflow=TextOverflow.Ellipsis,lineHeight=titleLine)
            Text(localizedHint,color=Color.White.copy(alpha=.72f),fontSize=hintSize,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=(hintSize.value+2f).sp)
        }
    }
}

@Composable
private fun DefaultTileArtworkV21(c:RsPalette,kind:String){
    Canvas(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF080808),Color(0xFF1A1208),Color.Black)))){
        val w=size.width;val h=size.height
        drawCircle(c.bright.copy(alpha=.10f),w*.42f,Offset(w*.85f,h*.18f))
        drawCircle(c.gold.copy(alpha=.08f),w*.28f,Offset(w*.28f,h*.72f),style=Stroke(6f))
        when(kind){
            "fight","training","technique"->{
                drawCircle(Color.Black.copy(.90f),w*.055f,Offset(w*.70f,h*.28f));drawLine(Color.Black.copy(.94f),Offset(w*.70f,h*.34f),Offset(w*.64f,h*.59f),17f);drawLine(Color.Black.copy(.94f),Offset(w*.67f,h*.40f),Offset(w*.88f,h*.30f),13f);drawLine(Color.Black.copy(.94f),Offset(w*.64f,h*.58f),Offset(w*.49f,h*.80f),15f);drawLine(Color.Black.copy(.94f),Offset(w*.64f,h*.58f),Offset(w*.82f,h*.75f),15f)
            }
            "music"->{repeat(5){i->drawLine(c.bright.copy(alpha=.25f),Offset(w*(.44f+i*.08f),h*.64f),Offset(w*(.44f+i*.08f),h*(.35f-(i%2)*.08f)),6f)};drawCircle(c.bright.copy(.18f),w*.12f,Offset(w*.75f,h*.35f),style=Stroke(7f))}
            "ai"->{repeat(6){i->val x=w*(.43f+i*.07f);drawLine(Color(0xFF55B9FF).copy(alpha=.45f),Offset(x,h*.32f-(i%3)*12f),Offset(x,h*.32f+(i%3)*12f),7f)}}
            "payment"->{drawRoundRect(c.bright.copy(alpha=.16f),Offset(w*.45f,h*.22f),androidx.compose.ui.geometry.Size(w*.42f,h*.30f),androidx.compose.ui.geometry.CornerRadius(18f,18f));drawLine(c.bright.copy(.42f),Offset(w*.49f,h*.32f),Offset(w*.83f,h*.32f),7f)}
            "progress"->{listOf(.52f,.62f,.72f,.82f).forEachIndexed{i,x->drawRect(Color(0xFF5ED08B).copy(alpha=.20f+i*.08f),Offset(w*x,h*(.67f-i*.09f)),androidx.compose.ui.geometry.Size(w*.055f,h*(.12f+i*.09f)))}}
            else->{drawCircle(c.bright.copy(alpha=.14f),w*.18f,Offset(w*.70f,h*.38f),style=Stroke(6f));drawCircle(c.bright.copy(alpha=.18f),w*.08f,Offset(w*.70f,h*.38f),style=Stroke(5f))}
        }
    }
}

private fun sectionTitleV25(lang:RsLang,title:String)=when(title){
    "Core Training"->rsT(lang,"core_training")
    "Club & Coaching"->rsT(lang,"club_coaching")
    "Performance"->rsT(lang,"performance")
    "Library, Music & Account"->rsT(lang,"library_account")
    "Brand & Experience"->rsT(lang,"brand_experience")
    "Coaching, Content & Music"->rsT(lang,"coaching_content_music")
    "Members & Access"->rsT(lang,"members_access")
    "Club Operations"->rsT(lang,"club_operations")
    "Business & Release"->rsT(lang,"business_release")
    "Member Services"->when(lang.code){
        "nl"->"Ledenservices"
        "pt"->"Serviços para Membros"
        "es"->"Servicios para Miembros"
        "fr"->"Services Membres"
        "de"->"Mitgliederservice"
        "it"->"Servizi Membri"
        "pl"->"Usługi dla Członków"
        "tr"->"Üye Hizmetleri"
        else->"Member Services"
    }
    "Trainer Help"->when(lang.code){
        "nl"->"Trainer Hulp"
        "pt"->"Ajuda do Treinador"
        "es"->"Ayuda del Entrenador"
        "fr"->"Aide Entraîneur"
        "de"->"Trainer-Hilfe"
        "it"->"Aiuto Allenatore"
        "pl"->"Pomoc Trenera"
        "tr"->"Antrenör Yardımı"
        else->"Trainer Help"
    }
    else->title
}

private fun sectionFontV25(text:String)=when{
    text.length>=28->10.sp
    text.length>=22->11.sp
    text.length>=17->12.sp
    else->13.sp
}

private fun tileTitleFontV25(text:String)=when{
    text.length>=34->10.sp
    text.length>=28->11.sp
    text.length>=23->12.sp
    text.length>=18->13.sp
    else->15.sp
}

private fun tileTitleLineV25(text:String)=when{
    text.length>=34->12.sp
    text.length>=28->13.sp
    text.length>=23->14.sp
    text.length>=18->15.sp
    else->18.sp
}

private fun tileHintFontV25(text:String)=when{
    text.length>=34->8.sp
    text.length>=24->9.sp
    else->10.sp
}

private fun rsDashboardBadgeCountV55(store:RsStore,role:RsRole,route:String):Int{
    return if(role==RsRole.TRAINER){
        when(route){
            "coachchat"->rsTrainerUnreadCoachCountV55(store)
            "support"->rsLoadTicketsV51(store).count{it.status=="OPEN"}
            "homework_admin"->rsLoadHomeworkV46(store).count{!it.completed}
            "invoices"->rsLoadInvoicesV39(store).count{it.status!="PAID"}
            else->0
        }
    }else{
        val email=store.s("session_student_email","alex@rskickbox.nl")
        when(route){
            "notifications"->rsUnreadNotificationCountV55(store)
            "coachchat"->rsStudentUnreadCoachCountV55(store)
            "homework"->rsLoadHomeworkV46(store).count{it.studentEmail.equals(email,true)&&!it.completed}
            "support"->rsLoadTicketsV51(store).count{it.studentEmail.equals(email,true)&&it.status=="OPEN"}
            "finance"->rsLoadInvoicesV39(store).count{it.studentEmail.equals(email,true)&&it.status!="PAID"}
            else->0
        }
    }
}

private fun glyphV21(kind:String)=when(kind){"ai"->"AI";"music"->"♫";"payment"->"€";"progress"->"↗";"fight"->"FX";"training"->"TR";"technique"->"TK";"settings"->"⚙";"brand"->"♛";else->"RS"}

private fun studentV21()=listOf(
    SectionV21("Core Training",listOf(DashV21("voice","AI Technique Coach","Video · text · visuals · voice","ai"),DashV21("session","Session Player","Rounds · timer · cues","training"),DashV21("academy","RS Academy","Structured premium lessons","training"),DashV21("techniques","Technique Library","Moves · drills · details","technique"),DashV21("home_training","Home Training","Train anywhere","training"),DashV21("workout","Workout Generator","Build your session","training"))),
    SectionV21("Club & Coaching",listOf(DashV21("classes","Classes & Events","Book club training","training"),DashV21("events","RS Events","Seminars · club days","brand"),DashV21("coachchat","Private Coach Chat","Direct coach support","ai"),DashV21("community","Community","Club feed · connection","brand"),DashV21("groups","Groups","Team spaces","brand"),DashV21("private_lessons","Private Lessons","1-to-1 coaching","training"))),
    SectionV21("Member Services",listOf(
        DashV21("notifications","Notifications","Club updates · unread messages","ai"),
        DashV21("promotions","Promotions","Offers · book · external links","brand"),
        DashV21("checkin","Class Check-In","Scan trainer attendance QR","brand"),
        DashV21("documents","Club Documents","Rules · guides · information","brand"),
        DashV21("support","Support","Private member requests","settings"),
        DashV21("referrals","Referrals","Invite · share · grow","brand")
    )),
    SectionV21("Performance",listOf(DashV21("progress","Progress","XP · technique · streak","progress"),DashV21("challenges","Challenges","Goals · XP · consistency","progress"),DashV21("badges","Badges","Achievements","progress"),DashV21("fightcamp","Fight Camp","Structured preparation","fight"),DashV21("compare","Technique Compare","Compare movement","technique"),DashV21("history","Training History","Sessions · attendance","progress"))),
    SectionV21("Library, Music & Account",listOf(DashV21("vault","Knowledge Vault","Premium learning library","brand"),DashV21("homework","Homework","Coach assignments","training"),DashV21("favorites","Saved & Favorites","Your saved content","brand"),DashV21("media","Training Media","Videos · uploads","training"),DashV21("music","My RS Music","Local playlists · live player","music"),DashV21("finance","Membership & Payments","Plan · payments · history","payment"),DashV21("book","Trainer Book","Van Stilte Naar Strijd","brand"),DashV21("profile","My Profile","Goals · identity","settings"),DashV21("search","Search","Find anything in RS","brand"),DashV21("settings","Settings & Privacy","Preferences · account","settings")))
)

private fun trainerV21()=listOf(
    SectionV21("Trainer Help",listOf(
        DashV21("guide","App Guide","Examples · workflows · how every section works","brand")
    )),
    SectionV21("Brand & Experience",listOf(DashV21("themes","Visual Theme Studio","Complete app identity","brand"),DashV21("backgrounds","Visual Asset Studio","Pages · cards · banners","brand"),DashV21("branding","Branding & Site Settings","Logo · name · favicon","brand"),DashV21("intro_settings","Intro & Splash","Cinematic opening sequence","fight"),DashV21("landing_admin","Promotion Manager","Public app promotions","brand"),DashV21("book","Book Manager","Trainer book experience","brand"))),
    SectionV21("Coaching, Content & Music",listOf(DashV21("voice","AI Technique Coach","Video review · voice coach","ai"),DashV21("session","Trainer Session","Round control","training"),DashV21("lesson_editor","Lesson Editor","Academy content","training"),DashV21("content","Content Manager","Lessons · cards · media","brand"),DashV21("media","Training Media Manager","Videos · images · access tiers","training"),DashV21("homework_admin","Homework Manager","Assign · review","training"),DashV21("session_builder","Session Builder","Reusable training plans","training"),DashV21("music_admin","RS Music Manager","Device music · internal player","music"),DashV21("notes","Coach Notes","Private observations","brand"))),
    SectionV21("Members & Access",listOf(DashV21("members","Student Manager","Profiles · status","brand"),DashV21("access","Access & Subscriptions","Features · overrides","settings"),DashV21("plans_admin","Membership Plans","Basic · Pro · Elite","payment"),DashV21("progress_admin","Progress Manager","Student development","progress"),DashV21("assessments","Coach Assessments","Reviews · priorities","progress"),DashV21("challenge_admin","Challenge Manager","Safe club goals","progress"),DashV21("fightcamp_admin","Fight Camp Manager","Camp planning","fight"))),
    SectionV21("Club Operations",listOf(DashV21("classes","Class Manager","Schedule · capacity","training"),DashV21("attendance","Attendance","Check-in control","brand"),DashV21("qr_attendance","QR Attendance","Fast member check-in","brand"),DashV21("events_admin","Event Manager","Events · capacity","brand"),DashV21("schedule","Trainer Schedule","Daily coaching plan","training"),DashV21("notifications","Notifications","Messages · audiences","ai"),DashV21("documents","Documents & Waivers","Agreements · status","brand"),DashV21("referrals","Referrals","Invites · conversion","brand"))),
    SectionV21("Business & Release",listOf(DashV21("payments","Payment Center","Methods · instructions","payment"),DashV21("invoices","Invoices","Revenue · status","payment"),DashV21("analytics","Analytics","Club performance","progress"),DashV21("support","Support & Final QC","Operations · QA","settings"),DashV21("release","Release & Legal Center","Policies · Play Store","settings"),DashV21("settings","App Settings","Privacy · maintenance","settings")))
)

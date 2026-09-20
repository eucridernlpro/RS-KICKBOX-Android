package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class RsCommunicationTabV122{
    ALL,PRIVATE,GROUPS,AI,MEDIA,STUDENTS
}

private fun rsCommTabFromRouteV122(route:String,role:RsRole):RsCommunicationTabV122=when(route){
    "coachchat"->RsCommunicationTabV122.PRIVATE
    "groups"->RsCommunicationTabV122.GROUPS
    "voice"->RsCommunicationTabV122.AI
    "media"->RsCommunicationTabV122.MEDIA
    "members"->if(role==RsRole.TRAINER)RsCommunicationTabV122.STUDENTS else RsCommunicationTabV122.ALL
    else->RsCommunicationTabV122.ALL
}

private fun rsCommT122(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "RS COMMUNICATION HUB",
        "subtitle" to "One place for coaching, groups, media, voice and AI training support.",
        "all" to "All","private" to "Private","groups" to "Groups","ai" to "AI Coach",
        "media" to "Media","students" to "Students",
        "private_desc" to "Private trainer ↔ student coaching conversations.",
        "groups_desc" to "Team communication, training groups and announcements.",
        "ai_desc" to "Analyze technique videos, get corrections and matched trainer references.",
        "media_desc" to "Trainer videos, images and technique references in one library.",
        "students_desc" to "Student management, access and moderation tools.",
        "quick" to "QUICK ACCESS",
        "composer" to "CHAT TOOLS",
        "composer_desc" to "Photo · short video · voice message · AI coaching",
        "ai_flow" to "AI TRAINING FLOW",
        "ai_flow_desc" to "Upload a technique video → AI analyzes the move → explains corrections in the app language → matches trainer-uploaded images/videos/instructions by technique tags.",
        "moderation" to "TRAINER MODERATION",
        "moderation_desc" to "Manage students and groups from the same communication system instead of separate simple chat pages."
    )
    val nl=en+mapOf(
        "title" to "RS COMMUNICATIEHUB",
        "subtitle" to "Eén plek voor coaching, groepen, media, spraak en AI-trainingshulp.",
        "all" to "Alles","private" to "Privé","groups" to "Groepen","ai" to "AI Coach","media" to "Media","students" to "Studenten",
        "quick" to "SNELLE TOEGANG","composer" to "CHATTOOLS","moderation" to "TRAINERMODERATIE"
    )
    val pt=en+mapOf(
        "title" to "CENTRO DE COMUNICAÇÃO RS",
        "subtitle" to "Coaching, grupos, media, voz e apoio de treino por IA num só lugar.",
        "all" to "Tudo","private" to "Privado","groups" to "Grupos","ai" to "Coach IA","media" to "Media","students" to "Alunos",
        "quick" to "ACESSO RÁPIDO","composer" to "FERRAMENTAS DO CHAT","moderation" to "MODERAÇÃO DO TREINADOR"
    )
    val es=en+mapOf("all" to "Todo","private" to "Privado","groups" to "Grupos","ai" to "Coach IA","students" to "Alumnos")
    val fr=en+mapOf("all" to "Tout","private" to "Privé","groups" to "Groupes","ai" to "Coach IA","students" to "Élèves")
    val de=en+mapOf("all" to "Alle","private" to "Privat","groups" to "Gruppen","ai" to "KI-Coach","students" to "Schüler")
    val it=en+mapOf("all" to "Tutto","private" to "Privato","groups" to "Gruppi","ai" to "Coach IA","students" to "Studenti")
    val pl=en+mapOf("all" to "Wszystko","private" to "Prywatne","groups" to "Grupy","ai" to "Trener AI","students" to "Uczniowie")
    val tr=en+mapOf("all" to "Tümü","private" to "Özel","groups" to "Gruplar","ai" to "AI Koç","students" to "Öğrenciler")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsCommunicationQuickCardV122(
    c:RsPalette,
    title:String,
    subtitle:String,
    icon:String,
    onClick:()->Unit
){
    Card(
        onClick=onClick,
        colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.86f)),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.55f)),
        shape=RoundedCornerShape(22.dp),
        modifier=Modifier.fillMaxWidth()
    ){
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(14.dp)
        ){
            Surface(
                shape=RoundedCornerShape(18.dp),
                color=c.gold.copy(alpha=.16f),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.7f)),
                modifier=Modifier.size(48.dp)
            ){
                Box(contentAlignment=Alignment.Center){Text(icon,fontSize=21.sp)}
            }
            Column(Modifier.weight(1f)){
                Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                Text(subtitle,color=c.muted,fontSize=11.sp,lineHeight=15.sp)
            }
            Text("›",color=c.gold,fontSize=25.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
fun RsCommunicationHubV122(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    role:RsRole,
    initialRoute:String="communication"
){
    var selected by remember(initialRoute,role){mutableStateOf(rsCommTabFromRouteV122(initialRoute,role))}
    val tabs=buildList{
        add(RsCommunicationTabV122.ALL)
        add(RsCommunicationTabV122.PRIVATE)
        add(RsCommunicationTabV122.GROUPS)
        add(RsCommunicationTabV122.AI)
        add(RsCommunicationTabV122.MEDIA)
        if(role==RsRole.TRAINER)add(RsCommunicationTabV122.STUDENTS)
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        Text(rsCommT122(lang,"title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=27.sp)
        Text(rsCommT122(lang,"subtitle"),color=c.text.copy(alpha=.84f),fontSize=13.sp,lineHeight=19.sp)

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(7.dp)
        ){
            tabs.forEach{tab->
                val label=when(tab){
                    RsCommunicationTabV122.ALL->rsCommT122(lang,"all")
                    RsCommunicationTabV122.PRIVATE->rsCommT122(lang,"private")
                    RsCommunicationTabV122.GROUPS->rsCommT122(lang,"groups")
                    RsCommunicationTabV122.AI->rsCommT122(lang,"ai")
                    RsCommunicationTabV122.MEDIA->rsCommT122(lang,"media")
                    RsCommunicationTabV122.STUDENTS->rsCommT122(lang,"students")
                }
                FilterChip(
                    selected=selected==tab,
                    onClick={selected=tab},
                    label={Text(label,fontWeight=if(selected==tab)FontWeight.Black else FontWeight.SemiBold)},
                    leadingIcon=if(selected==tab){{Text("●",color=c.gold,fontSize=8.sp)}}else null
                )
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)){
        when(selected){
            RsCommunicationTabV122.ALL->Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Text(rsCommT122(lang,"quick"),color=c.gold,fontWeight=FontWeight.Black,fontSize=11.sp)
                RsCommunicationQuickCardV122(c,rsCommT122(lang,"private"),rsCommT122(lang,"private_desc"),"💬"){selected=RsCommunicationTabV122.PRIVATE}
                RsCommunicationQuickCardV122(c,rsCommT122(lang,"groups"),rsCommT122(lang,"groups_desc"),"👥"){selected=RsCommunicationTabV122.GROUPS}
                RsCommunicationQuickCardV122(c,rsCommT122(lang,"ai"),rsCommT122(lang,"ai_desc"),"✦"){selected=RsCommunicationTabV122.AI}
                RsCommunicationQuickCardV122(c,rsCommT122(lang,"media"),rsCommT122(lang,"media_desc"),"▣"){selected=RsCommunicationTabV122.MEDIA}
                if(role==RsRole.TRAINER){
                    RsCommunicationQuickCardV122(c,rsCommT122(lang,"students"),rsCommT122(lang,"students_desc"),"♛"){selected=RsCommunicationTabV122.STUDENTS}
                }
                RsPanel(c){
                    Text(rsCommT122(lang,"composer"),color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    Text(rsCommT122(lang,"composer_desc"),color=c.text,fontSize=12.sp)
                }
                RsPanel(c){
                    Text(rsCommT122(lang,"ai_flow"),color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    Text(rsCommT122(lang,"ai_flow_desc"),color=c.text,fontSize=12.sp,lineHeight=18.sp)
                }
                if(role==RsRole.TRAINER){
                    RsPanel(c){
                        Text(rsCommT122(lang,"moderation"),color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                        Text(rsCommT122(lang,"moderation_desc"),color=c.text,fontSize=12.sp,lineHeight=18.sp)
                    }
                }
            }
            RsCommunicationTabV122.PRIVATE->RsCoachChatV44(c,store,lang,role)
            RsCommunicationTabV122.GROUPS->RsGroupsV50(c,store,lang,role)
            RsCommunicationTabV122.AI->RsTechniqueCoachV27(c,lang,store,role)
            RsCommunicationTabV122.MEDIA->RsTrainingMediaV55(c,store,lang,role)
            RsCommunicationTabV122.STUDENTS->if(role==RsRole.TRAINER)RsMemberManager(c,store,lang)
        }
        }
    }
}

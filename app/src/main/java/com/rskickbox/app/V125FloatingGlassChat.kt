package com.rskickbox.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

private enum class RsChatHubTabV125{ALL,PRIVATE,GROUPS,AI}

private fun rsInitialChatTabV125(route:String)=when(route){
    "groups"->RsChatHubTabV125.GROUPS
    "voice"->RsChatHubTabV125.AI
    else->RsChatHubTabV125.ALL
}

@Composable
private fun RsFloatingGlassPanelV125(
    c:RsPalette,
    modifier:Modifier=Modifier,
    content:@Composable ColumnScope.()->Unit
){
    Surface(
        color=Color.Black.copy(alpha=.72f),
        shape=RoundedCornerShape(26.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.30f)),
        tonalElevation=12.dp,
        modifier=modifier
    ){
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp),
            content=content
        )
    }
}

@Composable
private fun RsContactPresenceV125(
    c:RsPalette,
    contact:RsChatContactV125,
    onClick:()->Unit
){
    Surface(
        color=if(contact.online)c.gold.copy(alpha=.10f) else Color.Black.copy(alpha=.58f),
        shape=RoundedCornerShape(22.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=if(contact.online).42f else .16f)),
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)
    ){
        Row(
            Modifier.fillMaxWidth().padding(11.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(11.dp)
        ){
            Box{
                RsMemberAvatarV68(c,contact.email,contact.displayName,size=48.dp)
                Surface(
                    color=if(contact.online)Color(0xFF36D27F) else Color(0xFFE65B63),
                    shape=CircleShape,
                    border=BorderStroke(2.dp,c.panel),
                    modifier=Modifier.size(13.dp).align(Alignment.BottomEnd)
                ){}
            }
            Column(Modifier.weight(1f)){
                Text(contact.displayName.ifBlank{contact.email},color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp)
                Text(if(contact.online)"Online" else "Offline",color=if(contact.online)Color(0xFF36D27F) else Color(0xFFE65B63),fontSize=10.sp,fontWeight=FontWeight.Bold)
            }
            Surface(
                shape=CircleShape,
                color=c.gold.copy(alpha=.10f),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.28f)),
                modifier=Modifier.size(34.dp)
            ){
                Box(contentAlignment=Alignment.Center){
                    Text("›",color=c.gold,fontSize=22.sp,fontWeight=FontWeight.Black)
                }
            }

        if(slideMenuOpen){
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha=.56f))
                    .zIndex(8f)
                    .clickable{slideMenuOpen=false}
            )
        }
        AnimatedVisibility(
            visible=slideMenuOpen,
            enter=slideInHorizontally(initialOffsetX={-it})+fadeIn(),
            exit=slideOutHorizontally(targetOffsetX={-it})+fadeOut(),
            modifier=Modifier.align(Alignment.CenterStart).fillMaxHeight().widthIn(max=310.dp).zIndex(9f)
        ){
            Surface(
                color=Color.Black.copy(alpha=.97f),
                shape=RoundedCornerShape(topEnd=30.dp,bottomEnd=30.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                tonalElevation=20.dp,
                modifier=Modifier.fillMaxHeight().fillMaxWidth()
            ){
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(14.dp),
                    verticalArrangement=Arrangement.spacedBy(9.dp)
                ){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("♛  RS CHAT",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp,letterSpacing=1.sp)
                            Text("ROYAL COMMUNICATION",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.3.sp)
                        }
                        TextButton(onClick={slideMenuOpen=false}){Text("×",color=c.bright,fontSize=24.sp)}
                    }
                    Surface(
                        color=c.gold.copy(alpha=.07f),
                        shape=RoundedCornerShape(16.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){
                            Text(if(role==RsRole.TRAINER)"TRAINER COMMAND" else "STUDENT HUB",color=c.muted,fontSize=8.sp,fontWeight=FontWeight.Black)
                            Text(contacts.count{it.online}.toString()+" ONLINE",color=Color(0xFF36D27F),fontSize=8.sp,fontWeight=FontWeight.Black)
                        }
                    }

                    Text("COMMUNICATION",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                    RsChatDrawerItemV144(c,"All Chats","Overview · live contacts · calls","◆",tab==RsChatHubTabV125.ALL){
                        tab=RsChatHubTabV125.ALL;slideMenuOpen=false
                    }
                    RsChatDrawerItemV144(c,"Private","Direct trainer/student conversation","✦",tab==RsChatHubTabV125.PRIVATE){
                        tab=RsChatHubTabV125.PRIVATE;slideMenuOpen=false
                    }

                    Text("TEAM SPACES",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                    RsChatDrawerItemV144(c,"Fight Groups","Team rooms · media · moderation","◈",tab==RsChatHubTabV125.GROUPS){
                        tab=RsChatHubTabV125.GROUPS;slideMenuOpen=false
                    }

                    Text("INTELLIGENCE",color=Color(0xFF58C9FF),fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                    RsChatDrawerItemV144(c,"RS AI Trainer","Sofia · Marcus · technique coaching","✧",tab==RsChatHubTabV125.AI,ai=true){
                        tab=RsChatHubTabV125.AI;slideMenuOpen=false
                    }

                    Spacer(Modifier.weight(1f))
                    Surface(
                        color=Color.Black.copy(alpha=.62f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text("RS SECURE COMMUNICATION",color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)
                            Text("Private · Groups · Voice · Video · AI",color=c.muted,fontSize=8.sp)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun RsRoyalChatTabV135(
    c:RsPalette,
    selected:Boolean,
    label:String,
    symbol:String,
    onClick:()->Unit
){
    Surface(
        color=if(selected)c.gold.copy(alpha=.18f) else Color.Black.copy(alpha=.48f),
        shape=RoundedCornerShape(18.dp),
        border=BorderStroke(1.dp,if(selected)c.gold.copy(alpha=.68f) else c.gold.copy(alpha=.18f)),
        tonalElevation=if(selected)8.dp else 2.dp,
        modifier=Modifier.clickable(onClick=onClick)
    ){
        Row(
            Modifier.padding(horizontal=12.dp,vertical=8.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)
        ){
            Text(symbol,color=if(selected)c.bright else c.gold,fontSize=11.sp,fontWeight=FontWeight.Black)
            Text(label,color=if(selected)c.bright else c.text,fontSize=10.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
private fun RsRoyalChatMenuCardV135(
    c:RsPalette,
    symbol:String,
    title:String,
    subtitle:String,
    badge:String,
    ai:Boolean=false,
    onClick:()->Unit
){
    val accent=if(ai)Color(0xFF58C9FF) else c.gold
    Surface(
        color=Color.Black.copy(alpha=.62f),
        shape=RoundedCornerShape(24.dp),
        border=BorderStroke(1.dp,accent.copy(alpha=.34f)),
        tonalElevation=10.dp,
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)
    ){
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)
        ){
            Surface(
                shape=RoundedCornerShape(18.dp),
                color=accent.copy(alpha=.10f),
                border=BorderStroke(1.dp,accent.copy(alpha=.34f)),
                modifier=Modifier.size(54.dp)
            ){
                Box(contentAlignment=Alignment.Center){
                    Text(symbol,color=accent,fontSize=21.sp,fontWeight=FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp,letterSpacing=.35.sp)
                Text(subtitle,color=c.muted,fontSize=9.sp,lineHeight=13.sp)
            }
            Surface(
                shape=RoundedCornerShape(12.dp),
                color=accent.copy(alpha=.10f),
                border=BorderStroke(1.dp,accent.copy(alpha=.22f))
            ){
                Text(badge,color=accent,fontSize=8.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(horizontal=7.dp,vertical=5.dp))
            }
        }
    }
}
@Composable
private fun RsChatDrawerItemV144(
    c:RsPalette,
    title:String,
    subtitle:String,
    symbol:String,
    selected:Boolean,
    ai:Boolean=false,
    onClick:()->Unit
){
    val accent=if(ai)Color(0xFF58C9FF) else c.gold
    Surface(
        color=if(selected)accent.copy(alpha=.14f) else Color.Black.copy(alpha=.38f),
        shape=RoundedCornerShape(20.dp),
        border=BorderStroke(1.dp,accent.copy(alpha=if(selected).56f else .18f)),
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)
    ){
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(11.dp)
        ){
            Surface(
                color=accent.copy(alpha=.10f),
                shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,accent.copy(alpha=.30f)),
                modifier=Modifier.size(46.dp)
            ){
                Box(contentAlignment=Alignment.Center){
                    Text(symbol,color=accent,fontSize=18.sp,fontWeight=FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f)){
                Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=12.sp)
                Text(subtitle,color=c.muted,fontSize=8.sp,maxLines=2)
            }
            Text("›",color=accent,fontSize=20.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
fun RsFloatingGlassChatHubV125(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    role:RsRole,
    initialRoute:String="coachchat",
    onBack:()->Unit
){
    BackHandler(onBack=onBack)
    var tab by remember(initialRoute){mutableStateOf(rsInitialChatTabV125(initialRoute))}
    var contacts by remember{mutableStateOf<List<RsChatContactV125>>(emptyList())}
    var presenceError by remember{mutableStateOf(false)}
    var privateStudentId by remember{mutableStateOf<String?>(null)}
    var presenceRevision by remember{mutableIntStateOf(0)}
    var searchQuery by remember{mutableStateOf("")}
    var slideMenuOpen by remember{mutableStateOf(false)}

    LaunchedEffect(role,presenceRevision){
        if(RsSupabaseV60.configured){
            rsTouchPresenceV125()
            rsChatContactsV125()
                .onSuccess{contacts=it;presenceError=false}
                .onFailure{presenceError=true}
        }
    }
    LaunchedEffect(role){
        if(RsSupabaseV60.configured){
            while(true){
                delay(45_000)
                rsTouchPresenceV125()
                rsChatContactsV125().onSuccess{contacts=it}
            }
        }
    }

    val filteredContacts=remember(contacts,searchQuery){
        val q=searchQuery.trim().lowercase()
        if(q.isBlank())contacts else contacts.filter{
            it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    val context=LocalContext.current
    val chatVisual=remember(store){rsVisualUriWithBundledFallbackV113(context,store,"coachchat")}
    Box(Modifier.fillMaxSize().background(Color.Black)){
        if(chatVisual.isNotBlank())RsUriPreviewV21(chatVisual,Modifier.fillMaxSize().alpha(.24f),"CENTER")
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha=.42f),
                        Color.Black.copy(alpha=.76f),
                        Color.Black.copy(alpha=.95f)
                    )
                )
            )
        )
        Column(Modifier.fillMaxSize().padding(6.dp)){
        Surface(
            color=Color.Black.copy(alpha=.74f),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.34f)),
            shape=RoundedCornerShape(24.dp),
            modifier=Modifier.fillMaxWidth()
        ){
            Row(
                Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=9.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)
            ){
                OutlinedButton(
                    onClick=onBack,
                    modifier=Modifier.size(42.dp),
                    shape=CircleShape,
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.34f)),
                    contentPadding=PaddingValues(0.dp)
                ){Text("‹",color=c.bright,fontSize=25.sp)}
                OutlinedButton(
                    onClick={slideMenuOpen=true},
                    modifier=Modifier.size(42.dp),
                    shape=CircleShape,
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.50f)),
                    contentPadding=PaddingValues(0.dp)
                ){Text("☰",color=c.bright,fontSize=17.sp,fontWeight=FontWeight.Black)}
                Column(Modifier.weight(1f)){
                    Text("RS COMMUNICATION HUB",color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp,letterSpacing=.9.sp)
                    Text("Private · Groups · Voice · Media · AI Coach",color=c.muted,fontSize=9.sp)
                }
                Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Surface(
                        color=c.gold.copy(alpha=.12f),
                        shape=RoundedCornerShape(14.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.30f))
                    ){
                        Text("♛  RS",color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp,modifier=Modifier.padding(horizontal=9.dp,vertical=6.dp))
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        Surface(color=Color(0xFF36D27F),shape=CircleShape,modifier=Modifier.size(6.dp)){}
                        Text("LIVE",color=Color(0xFF36D27F),fontSize=7.sp,fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                    }
                }
            }
        }

        Surface(
            color=c.panel.copy(alpha=.48f),
            shape=RoundedCornerShape(20.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.12f)),
            modifier=Modifier.fillMaxWidth().padding(top=7.dp)
        ){
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=9.dp,vertical=7.dp),
            horizontalArrangement=Arrangement.spacedBy(7.dp)
        ){
            listOf(
                Triple(RsChatHubTabV125.ALL,"All","◆"),
                Triple(RsChatHubTabV125.PRIVATE,"Private","✦"),
                Triple(RsChatHubTabV125.GROUPS,"Groups","◈"),
                Triple(RsChatHubTabV125.AI,"AI Coach","✧")
            ).forEach{(item,label,symbol)->
                RsRoyalChatTabV135(c,tab==item,label,symbol){tab=item}
            }
        }

        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(top=7.dp)){
            when(tab){
                RsChatHubTabV125.ALL->{
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement=Arrangement.spacedBy(10.dp)
                    ){
                        if(role==RsRole.TRAINER){
                            RsFloatingGlassPanelV125(c){
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                    Column(Modifier.weight(1f)){
                                        Text("PRIVATE LINES",color=c.gold,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.3.sp)
                                        Text("Trainer ↔ student secure communication",color=c.muted,fontSize=9.sp)
                                    }
                                    Text(contacts.count{it.online}.toString()+" ONLINE",color=Color(0xFF36D27F),fontSize=8.sp,fontWeight=FontWeight.Black)
                                }
                                OutlinedTextField(
                                    value=searchQuery,
                                    onValueChange={searchQuery=it.take(80)},
                                    placeholder={Text("Search students…",fontSize=11.sp)},
                                    singleLine=true,
                                    shape=RoundedCornerShape(18.dp),
                                    modifier=Modifier.fillMaxWidth(),
                                    colors=OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor=c.gold.copy(alpha=.55f),
                                        unfocusedBorderColor=c.gold.copy(alpha=.16f),
                                        focusedContainerColor=c.panel.copy(alpha=.40f),
                                        unfocusedContainerColor=c.panel.copy(alpha=.32f)
                                    )
                                )
                            }
                            RsTrainerCallControlV136(
                                c=c,
                                lang=lang,
                                onRoomCreated={}
                            )
                            if(presenceError){
                                RsFloatingGlassPanelV125(c){
                                    Text("Presence will become available after the latest Supabase chat migration is installed.",color=c.muted,fontSize=10.sp)
                                }
                            }
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier=Modifier.fillMaxSize(),
                                verticalArrangement=Arrangement.spacedBy(7.dp),
                                contentPadding=PaddingValues(bottom=12.dp)
                            ){
                                if(filteredContacts.isEmpty()){
                                    item{
                                        RsFloatingGlassPanelV125(c){
                                            Text(if(searchQuery.isBlank())"No chat contacts available." else "No students match this search.",color=c.muted,fontSize=10.sp)
                                        }
                                    }
                                }
                                items(filteredContacts.size){index->
                                    val contact=filteredContacts[index]
                                    RsContactPresenceV125(c,contact){
                                        privateStudentId=contact.userId
                                        tab=RsChatHubTabV125.PRIVATE
                                    }
                                }
                            }
                        }else{
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier=Modifier.fillMaxSize(),
                                verticalArrangement=Arrangement.spacedBy(9.dp),
                                contentPadding=PaddingValues(bottom=12.dp)
                            ){
                                item{
                                    RsRoyalChatMenuCardV135(
                                        c,"✦","PRIVATE COACH",
                                        "Direct trainer line · photo · video · voice · calls",
                                        "SECURE"
                                    ){tab=RsChatHubTabV125.PRIVATE}
                                }
                                item{
                                    RsRoyalChatMenuCardV135(
                                        c,"◈","FIGHT GROUPS",
                                        "Team communication · announcements · shared training media",
                                        "TEAM"
                                    ){tab=RsChatHubTabV125.GROUPS}
                                }
                                item{
                                    RsRoyalChatMenuCardV135(
                                        c,"✧","RS AI TRAINER",
                                        "Sofia / Marcus · technique analysis · spoken coaching · trainer references",
                                        "AI LIVE",ai=true
                                    ){tab=RsChatHubTabV125.AI}
                                }
                                item{
                                    RsStudentAllowedCallsV136(c,lang)
                                }
                                item{
                                    Surface(
                                        color=Color.Black.copy(alpha=.50f),
                                        shape=RoundedCornerShape(18.dp),
                                        border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                                        modifier=Modifier.fillMaxWidth()
                                    ){
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),
                                            horizontalArrangement=Arrangement.SpaceBetween
                                        ){
                                            Text("VOICE READY",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black)
                                            Text("VIDEO READY",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black)
                                            Text("AI READY",color=Color(0xFF58C9FF),fontSize=8.sp,fontWeight=FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                RsChatHubTabV125.PRIVATE->RsCoachChatV44(c,store,lang,role,privateStudentId)
                RsChatHubTabV125.GROUPS->RsGroupsV50(c,store,lang,role)
                RsChatHubTabV125.AI->if(role==RsRole.TRAINER) RsTrainerAiReferenceChatV125(c,lang) else RsTechniqueCoachV27(c,lang,store,role)
            }
        }
        }
    }
}

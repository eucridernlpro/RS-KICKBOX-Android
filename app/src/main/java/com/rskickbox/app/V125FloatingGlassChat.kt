package com.rskickbox.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        color=c.panel.copy(alpha=.72f),
        shape=RoundedCornerShape(26.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
        tonalElevation=8.dp,
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
        color=if(contact.online)c.gold.copy(alpha=.075f) else c.panel.copy(alpha=.50f),
        shape=RoundedCornerShape(20.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=if(contact.online).24f else .10f)),
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
            Text("›",color=c.gold,fontSize=23.sp,fontWeight=FontWeight.Black)
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

    Column(Modifier.fillMaxSize().padding(6.dp)){
        Surface(
            color=Color.Black.copy(alpha=.56f),
            border=BorderStroke(0.5.dp,c.gold.copy(alpha=.18f)),
            shape=RoundedCornerShape(22.dp),
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
                Column(Modifier.weight(1f)){
                    Text("RS CHAT",color=c.bright,fontWeight=FontWeight.Black,fontSize=19.sp,letterSpacing=.8.sp)
                    Text("Private · Groups · AI Coach · Voice",color=c.muted,fontSize=9.sp)
                }
                Surface(
                    color=c.gold.copy(alpha=.10f),
                    shape=RoundedCornerShape(14.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.18f))
                ){
                    Text("RS",color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))
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
                RsChatHubTabV125.ALL to "All",
                RsChatHubTabV125.PRIVATE to "Private",
                RsChatHubTabV125.GROUPS to "Groups",
                RsChatHubTabV125.AI to "AI Coach",
            ).forEach{(item,label)->
                FilterChip(
                    selected=tab==item,
                    onClick={tab=item},
                    label={Text(label,fontSize=10.sp,fontWeight=if(tab==item)FontWeight.Black else FontWeight.SemiBold)}
                )
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
                                Text("STUDENTS",color=c.gold,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.1.sp)
                                Text("Chat contacts only. Green = online · red = offline. Student management stays in the Trainer Dashboard.",color=c.muted,fontSize=9.sp)
                            }
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
                                items(contacts.size){index->
                                    val contact=contacts[index]
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
                                    RsFloatingGlassPanelV125(c,Modifier.clickable{tab=RsChatHubTabV125.PRIVATE}){
                                        Text("PRIVATE COACH",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                                        Text("Messages · photos · video · voice · files",color=c.muted,fontSize=10.sp)
                                    }
                                }
                                item{
                                    RsFloatingGlassPanelV125(c,Modifier.clickable{tab=RsChatHubTabV125.GROUPS}){
                                        Text("GROUPS",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                                        Text("Training groups and team conversations",color=c.muted,fontSize=10.sp)
                                    }
                                }
                                item{
                                    RsFloatingGlassPanelV125(c,Modifier.clickable{tab=RsChatHubTabV125.AI}){
                                        Text("RS AI TRAINER",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                                        Text("Upload technique video · spoken AI coaching · trainer reference examples",color=c.muted,fontSize=10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                RsChatHubTabV125.PRIVATE->RsCoachChatV44(c,store,lang,role,privateStudentId)
                RsChatHubTabV125.GROUPS->RsGroupsV50(c,store,lang,role)
                RsChatHubTabV125.AI->RsTechniqueCoachV27(c,lang,store,role)
            }
        }
    }
}

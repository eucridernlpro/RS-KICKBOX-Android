package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RsNotificationBellV156(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    onOpenCall:(String)->Unit,
    onOpenNotifications:()->Unit
){
    var open by remember{mutableStateOf(false)}
    var calls by remember{mutableStateOf<List<RsCallHistoryV156>>(emptyList())}
    var cloud by remember{mutableStateOf<List<RsCloudNotificationV74>>(emptyList())}
    var revision by remember{mutableIntStateOf(0)}
    val scope=rememberCoroutineScope()
    val dismissedCloud=remember(revision){
        store.s("rs_notification_hidden_v156","")
            .split(',').map{it.trim()}.filter{it.isNotBlank()}.toSet()
    }

    fun refresh(){
        scope.launch{
            rsCallHistoryV156().onSuccess{calls=it}
            rsCloudNotificationsV74().onSuccess{cloud=it.filterNot{n->n.id in dismissedCloud}}
        }
    }

    LaunchedEffect(revision){
        rsCallHistoryV156().onSuccess{calls=it}
        rsCloudNotificationsV74().onSuccess{cloud=it.filterNot{n->n.id in dismissedCloud}}
    }
    LaunchedEffect(Unit){
        while(true){
            delay(15_000)
            revision++
        }
    }

    val missedCount=calls.count{
        it.direction=="INCOMING" && it.status in setOf("RINGING","CANCELLED","MISSED")
    }
    val unreadCount=cloud.count{!it.read}+missedCount

    Box{
        Surface(
            shape=CircleShape,
            color=Color.Transparent,
            border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
            shadowElevation=8.dp,
            modifier=Modifier.size(39.dp).clickable{open=true;refresh()}
        ){
            Box(contentAlignment=Alignment.Center){
                Text("🔔",fontSize=15.sp)
                if(unreadCount>0){
                    Surface(
                        shape=CircleShape,
                        color=Color(0xFFE34F57),
                        modifier=Modifier.align(Alignment.TopEnd).size(16.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            Text(
                                if(unreadCount>9)"9+" else unreadCount.toString(),
                                color=Color.White,fontSize=7.sp,fontWeight=FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if(open){
        Dialog(
            onDismissRequest={open=false},
            properties=DialogProperties(usePlatformDefaultWidth=false)
        ){
            Box(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentAlignment=Alignment.TopCenter
            ){
                Surface(
                    color=Color(0xFF070707),
                    shape=RoundedCornerShape(bottomStart=28.dp,bottomEnd=28.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                    tonalElevation=20.dp,
                    modifier=Modifier.padding(horizontal=10.dp).fillMaxWidth().fillMaxHeight(.82f)
                ){
                    Column(
                        Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=10.dp),
                        verticalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Column(Modifier.weight(1f)){
                                Text("RS NOTIFICATIONS",color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp)
                                Text("Calls · messages · club updates",color=c.muted,fontSize=9.sp)
                            }
                            if(calls.isNotEmpty() || cloud.isNotEmpty()){
                                TextButton(
                                    onClick={
                                        scope.launch{
                                            calls.forEach{call->runCatching{rsHideCallHistoryV156(call.id)}}
                                            val allCloud=(dismissedCloud+cloud.map{it.id}).joinToString(",")
                                            store.ps("rs_notification_hidden_v156",allCloud)
                                            calls=emptyList()
                                            cloud=emptyList()
                                            revision++
                                        }
                                    }
                                ){Text("CLEAR ALL",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black)}
                            }
                            TextButton(onClick={open=false}){Text("×",color=c.bright,fontSize=24.sp)}
                        }

                        LazyColumn(
                            Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement=Arrangement.spacedBy(8.dp),
                            contentPadding=PaddingValues(bottom=12.dp)
                        ){
                            if(calls.isNotEmpty()){
                                item{
                                    Text("CALL HISTORY",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=.8.sp)
                                }
                                items(calls,key={it.id}){call->
                                    val missed=call.direction=="INCOMING" &&
                                        call.status in setOf("RINGING","CANCELLED","MISSED")
                                    Surface(
                                        color=if(missed)Color(0xFF240909) else c.panel.copy(alpha=.62f),
                                        shape=RoundedCornerShape(18.dp),
                                        border=BorderStroke(1.dp,if(missed)Color(0xFFE86161).copy(alpha=.45f) else c.gold.copy(alpha=.18f)),
                                        modifier=Modifier.fillMaxWidth()
                                    ){
                                        Row(
                                            Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment=Alignment.CenterVertically,
                                            horizontalArrangement=Arrangement.spacedBy(9.dp)
                                        ){
                                            Surface(
                                                shape=CircleShape,
                                                color=c.gold.copy(alpha=.09f),
                                                border=BorderStroke(1.dp,c.gold.copy(alpha=.30f)),
                                                modifier=Modifier.size(40.dp).clickable{
                                                    open=false
                                                    onOpenCall(call.peerId)
                                                }
                                            ){
                                                Box(contentAlignment=Alignment.Center){
                                                    Text(if(call.callType=="VIDEO")"📹" else "☎",color=c.bright,fontSize=16.sp)
                                                }
                                            }
                                            Column(
                                                Modifier.weight(1f).clickable{
                                                    open=false
                                                    onOpenCall(call.peerId)
                                                }
                                            ){
                                                Text(call.peerName.ifBlank{call.peerEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                                                Text(
                                                    when{
                                                        missed->"Missed "+call.callType.lowercase()+" call"
                                                        call.direction=="OUTGOING"->"Outgoing "+call.callType.lowercase()+" call"
                                                        else->"Incoming "+call.callType.lowercase()+" call"
                                                    },
                                                    color=if(missed)Color(0xFFFF8A80) else c.muted,
                                                    fontSize=9.sp
                                                )
                                                Text(rsCallHistoryTimeV156(call.createdAt),color=c.muted,fontSize=8.sp)
                                            }
                                            TextButton(
                                                onClick={
                                                    scope.launch{
                                                        rsHideCallHistoryV156(call.id).onSuccess{revision++}
                                                    }
                                                },
                                                contentPadding=PaddingValues(4.dp)
                                            ){Text("🗑",fontSize=14.sp)}
                                        }
                                    }
                                }
                            }

                            if(cloud.isNotEmpty()){
                                item{
                                    Text("APP NOTIFICATIONS",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=.8.sp)
                                }
                                items(cloud,key={it.id}){n->
                                    Surface(
                                        color=if(n.read)c.panel.copy(alpha=.52f) else c.gold.copy(alpha=.09f),
                                        shape=RoundedCornerShape(18.dp),
                                        border=BorderStroke(1.dp,c.gold.copy(alpha=if(n.read).14f else .36f)),
                                        modifier=Modifier.fillMaxWidth()
                                    ){
                                        Row(
                                            Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment=Alignment.CenterVertically,
                                            horizontalArrangement=Arrangement.spacedBy(8.dp)
                                        ){
                                            Column(
                                                Modifier.weight(1f).clickable{
                                                    scope.launch{rsMarkCloudNotificationReadV74(n.id)}
                                                    open=false
                                                    onOpenNotifications()
                                                }
                                            ){
                                                Text(n.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                                                Text(n.message,color=c.text,fontSize=9.sp,maxLines=3)
                                                Text(n.createdLabel(),color=c.muted,fontSize=8.sp)
                                            }
                                            TextButton(
                                                onClick={
                                                    val updated=(dismissedCloud+n.id).joinToString(",")
                                                    store.ps("rs_notification_hidden_v156",updated)
                                                    revision++
                                                },
                                                contentPadding=PaddingValues(4.dp)
                                            ){Text("🗑",fontSize=14.sp)}
                                        }
                                    }
                                }
                            }

                            if(calls.isEmpty()&&cloud.isEmpty()){
                                item{
                                    Text(
                                        "No notifications yet.",
                                        color=c.muted,
                                        modifier=Modifier.padding(vertical=20.dp).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

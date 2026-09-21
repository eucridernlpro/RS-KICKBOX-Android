package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun rsCallHistoryTimeV156(raw:String):String=runCatching{
    Instant.parse(raw).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM · HH:mm",Locale.getDefault()))
}.getOrDefault(raw)

@Composable
fun RsChatNotificationsV156(c:RsPalette,lang:RsLang){
    var calls by remember{mutableStateOf<List<RsCallHistoryV156>>(emptyList())}
    var cloud by remember{mutableStateOf<List<RsCloudNotificationV74>>(emptyList())}
    var status by remember{mutableStateOf("")}
    var revision by remember{mutableIntStateOf(0)}

    LaunchedEffect(revision){
        rsCallHistoryV156()
            .onSuccess{calls=it}
            .onFailure{status="Call history will be available after communications migration 0059."}
        rsCloudNotificationsV74().onSuccess{cloud=it}
    }
    LaunchedEffect(Unit){
        while(true){
            delay(15_000)
            revision++
        }
    }

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Surface(
            color=Color.Black.copy(alpha=.64f),
            shape=RoundedCornerShape(20.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text("RS NOTIFICATIONS",color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp)
                Text("Calls · missed calls · club notifications",color=c.muted,fontSize=9.sp)
                if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)
            }
        }
        LazyColumn(
            modifier=Modifier.fillMaxWidth().weight(1f),
            verticalArrangement=Arrangement.spacedBy(8.dp),
            contentPadding=PaddingValues(bottom=12.dp)
        ){
            if(calls.isNotEmpty()){
                item{Text("CALL HISTORY",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=1.sp)}
                items(calls,key={it.id}){call->
                    val missed=call.status=="MISSED" ||
                        (call.direction=="INCOMING" && call.status in setOf("CANCELLED","RINGING"))
                    Surface(
                        color=if(missed)Color(0xFF2A0909).copy(alpha=.72f) else Color.Black.copy(alpha=.58f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,if(missed)Color(0xFFFF5C5C).copy(alpha=.42f) else c.gold.copy(alpha=.18f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(11.dp),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(10.dp)
                        ){
                            Surface(
                                shape=CircleShape,
                                color=c.gold.copy(alpha=.10f),
                                border=BorderStroke(1.dp,c.gold.copy(alpha=.30f)),
                                modifier=Modifier.size(42.dp)
                            ){Box(contentAlignment=Alignment.Center){Text(if(call.callType=="VIDEO")"📹" else "☎",color=c.bright,fontSize=17.sp)}}
                            Column(Modifier.weight(1f)){
                                Text(call.peerName.ifBlank{call.peerEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=12.sp)
                                Text(
                                    when{
                                        missed->"MISSED "+call.callType.lowercase()+" call"
                                        call.direction=="OUTGOING"->"Outgoing "+call.callType.lowercase()+" call · "+call.status.lowercase()
                                        else->"Incoming "+call.callType.lowercase()+" call · "+call.status.lowercase()
                                    },
                                    color=if(missed)Color(0xFFFF8A80) else c.muted,
                                    fontSize=9.sp
                                )
                            }
                            Text(rsCallHistoryTimeV156(call.createdAt),color=c.muted,fontSize=8.sp)
                        }
                    }
                }
            }
            if(cloud.isNotEmpty()){
                item{Text("CLUB NOTIFICATIONS",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=1.sp)}
                items(cloud,key={it.id}){n->
                    Surface(
                        color=Color.Black.copy(alpha=.58f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=if(n.read).12f else .34f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
                            Text(n.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                            Text(n.message,color=c.text,fontSize=10.sp)
                            Text(n.createdLabel(),color=c.muted,fontSize=8.sp)
                        }
                    }
                }
            }
            if(calls.isEmpty()&&cloud.isEmpty()){
                item{
                    Surface(
                        color=Color.Black.copy(alpha=.52f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                        modifier=Modifier.fillMaxWidth()
                    ){Text("No notifications or call history yet.",color=c.muted,modifier=Modifier.padding(14.dp))}
                }
            }
        }
    }
}

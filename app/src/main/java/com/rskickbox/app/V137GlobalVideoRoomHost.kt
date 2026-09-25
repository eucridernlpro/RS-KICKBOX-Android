package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private fun rsVideoRoomFriendlyErrorV151(message:String):String{
    val m=message.lowercase()
    return when{
        "permission denied" in m || "42501" in m ->
            "Video rooms need the latest RS backend update. Ask the trainer/admin to run the video-room permission repair."
        "network" in m || "timeout" in m ->
            "Video room connection is temporarily unavailable. Check your internet connection and try again."
        else->"Video room is temporarily unavailable."
    }
}

@Composable
fun RsGlobalVideoRoomHostV137(
    c:RsPalette,
    lang:RsLang,
    role:RsRole,
    onRoomSessionFinished:(()->Unit)?=null
){
    val scope=rememberCoroutineScope()
    var rooms by remember{mutableStateOf<List<RsVideoRoomV136>>(emptyList())}
    var status by remember{mutableStateOf("")}
    var activeRoom by remember{mutableStateOf<RsVideoRoomV136?>(null)}
    var inviteRoom by remember{mutableStateOf<RsVideoRoomV136?>(null)}

    LaunchedEffect(role){
        while(isActive){
            rsMyVideoRoomsV136()
                .onSuccess{list->
                    rooms=list
                    val joined=list.firstOrNull{it.roomStatus=="OPEN"&&it.myStatus=="JOINED"}
                    activeRoom=joined
                    inviteRoom=if(joined==null)list.firstOrNull{
                        it.roomStatus=="OPEN"&&it.myRole!="HOST"&&it.myStatus=="INVITED"
                    }else null
                }
                .onFailure{status=rsVideoRoomFriendlyErrorV151(it.message.orEmpty())}
            delay(1200)
        }
    }

    inviteRoom?.let{room->
        Dialog(
            onDismissRequest={},
            properties=DialogProperties(usePlatformDefaultWidth=false)
        ){
            Box(
                Modifier.fillMaxSize().background(Color.Black),
                contentAlignment=Alignment.Center
            ){
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(16.dp)
                ){
                    Text("RS GROUP VIDEO",color=c.gold,fontWeight=FontWeight.Black,fontSize=15.sp,letterSpacing=1.sp)
                    Surface(
                        shape=CircleShape,
                        color=c.gold.copy(alpha=.10f),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.38f)),
                        modifier=Modifier.size(124.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            Text("♛\nRS",color=c.bright,fontWeight=FontWeight.Black,fontSize=28.sp)
                        }
                    }
                    Text(
                        room.hostName,
                        color=Color.White,fontWeight=FontWeight.Black,fontSize=24.sp
                    )
                    Text(
                        room.title,
                        color=c.muted,fontSize=12.sp
                    )
                    Text(
                        room.participantCount.toString()+" invited · "+room.onlineCount.toString()+" online",
                        color=Color(0xFF36D27F),fontWeight=FontWeight.Bold,fontSize=10.sp
                    )
                    if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(14.dp)
                    ){
                        OutlinedButton(
                            onClick={
                                scope.launch{
                                    rsSetVideoRoomStatusV136(room.roomId,"DECLINED")
                                        .onFailure{status=it.message.orEmpty()}
                                    inviteRoom=null
                                    onRoomSessionFinished?.invoke()
                                }
                            },
                            modifier=Modifier.weight(1f).height(56.dp)
                        ){Text("Decline")}
                        Button(
                            onClick={
                                scope.launch{
                                    rsSetVideoRoomStatusV136(room.roomId,"JOINED")
                                        .onSuccess{
                                            activeRoom=room.copy(myStatus="JOINED")
                                            inviteRoom=null
                                        }
                                        .onFailure{status=it.message.orEmpty()}
                                }
                            },
                            modifier=Modifier.weight(1f).height(56.dp)
                        ){Text("Join Video",fontWeight=FontWeight.Black)}
                    }
                }
            }
        }
    }

    activeRoom?.let{room->
        RsVideoRoomScreenV137(c,lang,room){
            activeRoom=null
            onRoomSessionFinished?.invoke()
        }
    }
}

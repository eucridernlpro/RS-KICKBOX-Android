package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

@Composable
fun RsTrainerCallControlV136(
    c:RsPalette,
    lang:RsLang,
    onRoomCreated:(String)->Unit
){
    val scope=rememberCoroutineScope()
    var students by remember{mutableStateOf<List<RsStudentCallPermissionV136>>(emptyList())}
    var expanded by remember{mutableStateOf(false)}
    var roomDialog by remember{mutableStateOf(false)}
    var selected by remember{mutableStateOf<Set<String>>(emptySet())}
    var status by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var revision by remember{mutableIntStateOf(0)}

    LaunchedEffect(revision){
        rsStudentCallPermissionListV136()
            .onSuccess{students=it;status=""}
            .onFailure{status=it.message.orEmpty()}
    }

    Surface(
        color=Color.Black.copy(alpha=.72f),
        shape=RoundedCornerShape(24.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.32f)),
        tonalElevation=12.dp,
        modifier=Modifier.fillMaxWidth()
    ){
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Surface(
                    shape=RoundedCornerShape(16.dp),
                    color=c.gold.copy(alpha=.10f),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.34f)),
                    modifier=Modifier.size(48.dp)
                ){
                    Box(contentAlignment=Alignment.Center){
                        Text("☎",color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Black)
                    }
                }
                Column(Modifier.weight(1f)){
                    Text("CALL CONTROL",color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp,letterSpacing=.7.sp)
                    Text("Student permissions · trainer group video",color=c.muted,fontSize=9.sp)
                }
                Surface(
                    shape=RoundedCornerShape(12.dp),
                    color=Color(0xFF36D27F).copy(alpha=.10f),
                    border=BorderStroke(1.dp,Color(0xFF36D27F).copy(alpha=.24f))
                ){
                    Text(
                        students.count{it.online}.toString()+" ONLINE",
                        color=Color(0xFF36D27F),fontWeight=FontWeight.Black,fontSize=8.sp,
                        modifier=Modifier.padding(horizontal=7.dp,vertical=5.dp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ){
                OutlinedButton(
                    onClick={expanded=!expanded},
                    modifier=Modifier.weight(1f),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.34f))
                ){
                    Text(if(expanded)"Hide Permissions" else "Student Call Permissions",fontSize=9.sp)
                }
                Button(
                    onClick={
                        selected=emptySet()
                        roomDialog=true
                    },
                    enabled=students.any{it.online},
                    modifier=Modifier.weight(1f)
                ){
                    Text("Group Video",fontSize=9.sp,fontWeight=FontWeight.Black)
                }
            }

            if(expanded){
                Text(
                    "Student↔student calls are OFF by default. Unlock Audio, Video, or both for each student.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
                students.forEach{student->
                    var audio by remember(student.studentId,student.audioEnabled){mutableStateOf(student.audioEnabled)}
                    var video by remember(student.studentId,student.videoEnabled){mutableStateOf(student.videoEnabled)}
                    Surface(
                        color=c.panel.copy(alpha=.45f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.14f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(9.dp)
                        ){
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                RsMemberAvatarV68(c,student.email,student.displayName,size=42.dp)
                                Text(
                                    if(student.online)"ONLINE" else "OFFLINE",
                                    color=if(student.online)Color(0xFF36D27F) else c.muted,
                                    fontSize=7.sp,fontWeight=FontWeight.Black
                                )
                            }
                            Column(Modifier.weight(1f)){
                                Text(
                                    student.displayName.ifBlank{student.email},
                                    color=c.bright,fontWeight=FontWeight.Bold,fontSize=11.sp
                                )
                                Text(student.email,color=c.muted,fontSize=8.sp)
                            }
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                Text("AUDIO",color=c.muted,fontSize=7.sp,fontWeight=FontWeight.Black)
                                Switch(
                                    checked=audio,
                                    onCheckedChange={next->
                                        audio=next
                                        scope.launch{
                                            rsSetStudentCallPermissionsV136(student.studentId,audio,video)
                                                .onSuccess{revision++}
                                                .onFailure{status=it.message.orEmpty()}
                                        }
                                    }
                                )
                            }
                            Column(horizontalAlignment=Alignment.CenterHorizontally){
                                Text("VIDEO",color=c.muted,fontSize=7.sp,fontWeight=FontWeight.Black)
                                Switch(
                                    checked=video,
                                    onCheckedChange={next->
                                        video=next
                                        scope.launch{
                                            rsSetStudentCallPermissionsV136(student.studentId,audio,video)
                                                .onSuccess{revision++}
                                                .onFailure{status=it.message.orEmpty()}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp)
        }
    }

    if(roomDialog){
        Dialog(
            onDismissRequest={if(!busy)roomDialog=false},
            properties=DialogProperties(usePlatformDefaultWidth=false)
        ){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
                Surface(
                    color=Color.Black.copy(alpha=.97f),
                    shape=RoundedCornerShape(topStart=30.dp,topEnd=30.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.52f)),
                    modifier=Modifier.fillMaxWidth().fillMaxHeight(.82f)
                ){
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement=Arrangement.spacedBy(10.dp)
                    ){
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Column(Modifier.weight(1f)){
                                Text("TRAINER GROUP VIDEO",color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp,letterSpacing=.7.sp)
                                Text("Only online students can be invited · max 8",color=c.muted,fontSize=9.sp)
                            }
                            TextButton(onClick={if(!busy){roomDialog=false}}){Text("×",color=c.bright,fontSize=24.sp)}
                        }

                        LazyColumn(
                            Modifier.weight(1f),
                            verticalArrangement=Arrangement.spacedBy(7.dp)
                        ){
                            val online=students.filter{it.online}
                            items(online.size){index->
                                val student=online[index]
                                val checked=student.studentId in selected
                                Surface(
                                    color=if(checked)c.gold.copy(alpha=.12f) else c.panel.copy(alpha=.44f),
                                    shape=RoundedCornerShape(18.dp),
                                    border=BorderStroke(1.dp,if(checked)c.gold.copy(alpha=.50f) else c.gold.copy(alpha=.14f)),
                                    modifier=Modifier.fillMaxWidth().clickable{
                                        selected=if(checked)selected-student.studentId
                                        else if(selected.size<8)selected+student.studentId else selected
                                    }
                                ){
                                    Row(
                                        Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment=Alignment.CenterVertically,
                                        horizontalArrangement=Arrangement.spacedBy(10.dp)
                                    ){
                                        RsMemberAvatarV68(c,student.email,student.displayName,size=44.dp)
                                        Column(Modifier.weight(1f)){
                                            Text(student.displayName.ifBlank{student.email},color=c.bright,fontWeight=FontWeight.Bold,fontSize=11.sp)
                                            Text("Online · ready for video",color=Color(0xFF36D27F),fontSize=8.sp)
                                        }
                                        Checkbox(
                                            checked=checked,
                                            onCheckedChange={
                                                selected=if(checked)selected-student.studentId
                                                else if(selected.size<8)selected+student.studentId else selected
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            selected.size.toString()+" selected",
                            color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp
                        )
                        Button(
                            onClick={
                                if(selected.isNotEmpty()&&!busy){
                                    busy=true
                                    scope.launch{
                                        rsCreateVideoRoomV136("RS Trainer Video Session",selected.toList())
                                            .onSuccess{roomId->
                                                roomDialog=false
                                                onRoomCreated(roomId)
                                            }
                                            .onFailure{status=it.message.orEmpty()}
                                        busy=false
                                    }
                                }
                            },
                            enabled=selected.isNotEmpty()&&!busy,
                            modifier=Modifier.fillMaxWidth().height(54.dp)
                        ){
                            Text(if(busy)"Starting…" else "Start Group Video",fontWeight=FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

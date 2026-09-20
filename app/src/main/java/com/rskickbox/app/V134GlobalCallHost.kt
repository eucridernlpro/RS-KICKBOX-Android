package com.rskickbox.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun rsCancelIncomingCallNotificationV134(context:Context,callId:String){
    runCatching{
        context.getSystemService(NotificationManager::class.java).cancel(callId.hashCode())
    }
}

@Composable
fun RsGlobalCallHostV134(
    c:RsPalette,
    lang:RsLang,
    role:RsRole
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val myId=remember{rsSupabaseClientV60()?.auth?.currentUserOrNull()?.id.orEmpty()}
    var incoming by remember{mutableStateOf<RsCallV131?>(null)}
    var accepted by remember{mutableStateOf<RsCallV131?>(null)}
    var status by remember{mutableStateOf("")}
    var pendingAccept by remember{mutableStateOf<RsCallV131?>(null)}

    val permissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){result->
        val call=pendingAccept
        pendingAccept=null
        if(call!=null){
            if(result.values.all{it}){
                scope.launch{
                    rsSetCallStatusV131(call.id,"ACCEPTED")
                        .onSuccess{
                            rsCancelIncomingCallNotificationV134(context,call.id)
                            incoming=null
                        }
                        .onFailure{status=it.message.orEmpty()}
                }
            }else status="Microphone/camera permission is required."
        }
    }

    LaunchedEffect(myId,role){
        while(isActive && myId.isNotBlank()){
            rsCallInboxV131().onSuccess{calls->
                val active=calls.firstOrNull{it.status=="ACCEPTED"}
                if(active!=null){
                    accepted=active
                    incoming=null
                    rsCancelIncomingCallNotificationV134(context,active.id)
                }else{
                    accepted=null
                    incoming=calls.firstOrNull{it.calleeId==myId && it.status=="RINGING"}
                }
            }
            delay(1000)
        }
    }

    incoming?.let{call->
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
                    Text(
                        if(call.callType=="VIDEO")"RS VIDEO CALL" else "RS AUDIO CALL",
                        color=c.gold,
                        fontWeight=FontWeight.Black,
                        fontSize=16.sp
                    )
                    Surface(
                        shape=CircleShape,
                        color=c.panel,
                        modifier=Modifier.size(138.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            RsMemberAvatarV68(
                                c,
                                call.peerEmail,
                                call.peerName.ifBlank{call.peerEmail},
                                size=138.dp
                            )
                        }
                    }
                    Text(
                        call.peerName.ifBlank{call.peerEmail.ifBlank{"RS Member"}},
                        color=Color.White,
                        fontWeight=FontWeight.Black,
                        fontSize=26.sp
                    )
                    Text(
                        if(call.callType=="VIDEO")"Incoming video call" else "Incoming audio call",
                        color=c.muted,
                        fontSize=14.sp
                    )
                    if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(16.dp)
                    ){
                        OutlinedButton(
                            onClick={
                                scope.launch{
                                    rsSetCallStatusV131(call.id,"DECLINED")
                                    rsCancelIncomingCallNotificationV134(context,call.id)
                                    incoming=null
                                }
                            },
                            modifier=Modifier.weight(1f).height(56.dp)
                        ){
                            Text("✕  Decline")
                        }
                        Button(
                            onClick={
                                val needs=buildList{
                                    if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
                                        add(Manifest.permission.RECORD_AUDIO)
                                    if(call.callType=="VIDEO" && ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
                                        add(Manifest.permission.CAMERA)
                                }
                                if(needs.isEmpty()){
                                    scope.launch{
                                        rsSetCallStatusV131(call.id,"ACCEPTED")
                                            .onSuccess{
                                                rsCancelIncomingCallNotificationV134(context,call.id)
                                                incoming=null
                                            }
                                            .onFailure{status=it.message.orEmpty()}
                                    }
                                }else{
                                    pendingAccept=call
                                    permissionLauncher.launch(needs.toTypedArray())
                                }
                            },
                            modifier=Modifier.weight(1f).height(56.dp)
                        ){
                            Text("☎  Accept")
                        }
                    }
                }
            }
        }
    }

    accepted?.let{call->
        RsActiveCallDialogV133(c,lang,call,myId){
            rsCancelIncomingCallNotificationV134(context,call.id)
            accepted=null
        }
    }
}

package com.rskickbox.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black,
                            c.gold.copy(alpha=.07f),
                            Color.Black
                        )
                    )
                ),
                contentAlignment=Alignment.Center
            ){
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(16.dp)
                ){
                    Surface(
                        color=Color.Black.copy(alpha=.72f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.48f))
                    ){
                        Text(
                            if(call.callType=="VIDEO")"♛  RS VIDEO CALL" else "♛  RS AUDIO CALL",
                            color=c.bright,
                            fontWeight=FontWeight.Black,
                            fontSize=13.sp,
                            letterSpacing=1.1.sp,
                            modifier=Modifier.padding(horizontal=16.dp,vertical=9.dp)
                        )
                    }
                    Surface(
                        shape=CircleShape,
                        color=Color.Black,
                        border=BorderStroke(3.dp,c.gold.copy(alpha=.82f)),
                        tonalElevation=18.dp,
                        modifier=Modifier.size(162.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            RsMemberAvatarV68(
                                c,
                                call.peerEmail,
                                call.peerName.ifBlank{call.peerEmail},
                                size=156.dp
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
                        if(call.callType=="VIDEO")"INCOMING VIDEO CALL" else "INCOMING AUDIO CALL",
                        color=c.gold,
                        fontSize=11.sp,
                        fontWeight=FontWeight.Black,
                        letterSpacing=1.2.sp
                    )
                    Text(
                        "RS KICKBOXING · SECURE CALL",
                        color=c.muted,
                        fontSize=9.sp
                    )
                    if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.SpaceEvenly,
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Surface(
                                shape=CircleShape,
                                color=Color(0xFF3A0909),
                                border=BorderStroke(2.dp,Color(0xFFFF5C5C).copy(alpha=.80f)),
                                tonalElevation=14.dp,
                                modifier=Modifier.size(70.dp).clickable{
                                    scope.launch{
                                        rsSetCallStatusV131(call.id,"DECLINED")
                                        rsCancelIncomingCallNotificationV134(context,call.id)
                                        incoming=null
                                    }
                                }
                            ){
                                Box(contentAlignment=Alignment.Center){Text("✕",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)}
                            }
                            Text("DECLINE",color=Color(0xFFFF8A80),fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=.9.sp)
                        }

                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Surface(
                                shape=CircleShape,
                                color=c.gold.copy(alpha=.18f),
                                border=BorderStroke(2.dp,c.bright.copy(alpha=.88f)),
                                tonalElevation=18.dp,
                                modifier=Modifier.size(76.dp).clickable{
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
                                }
                            ){
                                Box(contentAlignment=Alignment.Center){Text("☎",color=c.bright,fontSize=27.sp,fontWeight=FontWeight.Black)}
                            }
                            Text("ACCEPT",color=c.bright,fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=.9.sp)
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

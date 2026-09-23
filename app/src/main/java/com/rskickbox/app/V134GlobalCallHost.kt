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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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

fun rsHideIncomingCallNotificationV134(context:Context,callId:String){
    runCatching{
        context.getSystemService(NotificationManager::class.java).cancel(callId.hashCode())
    }
}

fun rsCancelIncomingCallNotificationV134(context:Context,callId:String){
    rsHideIncomingCallNotificationV134(context,callId)
    RsCallMonitorServiceV134.stopRing(context)
}

@Composable
fun RsGlobalCallHostV134(
    c:RsPalette,
    lang:RsLang,
    role:RsRole,
    initialIncomingCall:RsCallV131?=null,
    onCallSessionFinished:(()->Unit)?=null
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val myId=remember{rsSupabaseClientV60()?.auth?.currentUserOrNull()?.id.orEmpty()}
    var incoming by remember(initialIncomingCall?.id){mutableStateOf(initialIncomingCall)}
    var accepted by remember{mutableStateOf<RsCallV131?>(null)}
    var outgoing by remember{mutableStateOf<RsCallV131?>(null)}
    var status by remember{mutableStateOf("")}
    var pendingAccept by remember{mutableStateOf<RsCallV131?>(null)}
    var callActionBusy by remember{mutableStateOf(false)}

    LaunchedEffect(incoming?.id){
        incoming?.id?.takeIf{it.isNotBlank()}?.let{rsHideIncomingCallNotificationV134(context,it)}
    }

        val permissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){result->
        val call=pendingAccept
        pendingAccept=null
        if(call!=null){
            if(result.values.all{it}){
                callActionBusy=true
                scope.launch{
                    try{
                        rsSetCallStatusReliableV173(call.id,"ACCEPTED")
                            .onSuccess{
                                rsCancelIncomingCallNotificationV134(context,call.id)
                                incoming=null
                                outgoing=null
                                accepted=call.copy(status="ACCEPTED")
                                status=""
                            }
                            .onFailure{status=it.message.orEmpty().ifBlank{"Could not answer the call."}}
                    }finally{
                        callActionBusy=false
                    }
                }
            }else{
                callActionBusy=false
                status="Microphone/camera permission is required."
            }
        }else callActionBusy=false
    }

    LaunchedEffect(myId,role){
        while(isActive && myId.isNotBlank()){
            rsCallInboxV131().onSuccess{calls->
                val active=calls.firstOrNull{it.status=="ACCEPTED"}
                if(active!=null){
                    accepted=active
                    incoming=null
                    outgoing=null
                    rsCancelIncomingCallNotificationV134(context,active.id)
                }else{
                    accepted=null
                    incoming=calls.firstOrNull{it.calleeId==myId && it.status=="RINGING"}
                    outgoing=calls.firstOrNull{it.callerId==myId && it.status=="RINGING"}
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
                    Image(
                        painter=painterResource(R.drawable.rs_launcher_royal_v129),
                        contentDescription="RS KICKBOXING",
                        modifier=Modifier.size(88.dp)
                    )

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
                                modifier=Modifier.size(70.dp).clickable(enabled=!callActionBusy){
                                    if(!callActionBusy){
                                        callActionBusy=true
                                        scope.launch{
                                            try{
                                                rsSetCallStatusReliableV173(call.id,"DECLINED")
                                                    .onSuccess{
                                                        rsCancelIncomingCallNotificationV134(context,call.id)
                                                        incoming=null
                                                        status=""
                                                        onCallSessionFinished?.invoke()
                                                    }
                                                    .onFailure{status=it.message.orEmpty().ifBlank{"Could not decline the call."}}
                                            }finally{
                                                callActionBusy=false
                                            }
                                        }
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
                                modifier=Modifier.size(76.dp).clickable(enabled=!callActionBusy){
                                    if(callActionBusy)return@clickable
                                    val needs=buildList{
                                        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
                                            add(Manifest.permission.RECORD_AUDIO)
                                        if(call.callType=="VIDEO" && ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
                                            add(Manifest.permission.CAMERA)
                                    }
                                    if(needs.isEmpty()){
                                        callActionBusy=true
                                        scope.launch{
                                            try{
                                                rsSetCallStatusReliableV173(call.id,"ACCEPTED")
                                                    .onSuccess{
                                                        rsCancelIncomingCallNotificationV134(context,call.id)
                                                        incoming=null
                                                        outgoing=null
                                                        accepted=call.copy(status="ACCEPTED")
                                                        status=""
                                                    }
                                                    .onFailure{status=it.message.orEmpty().ifBlank{"Could not answer the call."}}
                                            }finally{
                                                callActionBusy=false
                                            }
                                        }
                                    }else{
                                        callActionBusy=true
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

    outgoing?.takeIf{accepted==null && incoming==null}?.let{call->
        Dialog(
            onDismissRequest={},
            properties=DialogProperties(usePlatformDefaultWidth=false)
        ){
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black,c.gold.copy(alpha=.08f),Color.Black))
                ),
                contentAlignment=Alignment.Center
            ){
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(18.dp)
                ){
                    Image(
                        painter=painterResource(R.drawable.rs_launcher_royal_v129),
                        contentDescription="RS KICKBOXING",
                        modifier=Modifier.size(82.dp)
                    )
                    Text(
                        if(call.callType=="VIDEO")"♛  RS VIDEO CALL" else "♛  RS AUDIO CALL",
                        color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp,letterSpacing=1.sp
                    )
                    Surface(
                        shape=CircleShape,
                        color=Color.Black,
                        border=BorderStroke(3.dp,c.gold.copy(alpha=.82f)),
                        modifier=Modifier.size(164.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            RsMemberAvatarV68(c,call.peerEmail,call.peerName.ifBlank{call.peerEmail},size=156.dp)
                        }
                    }
                    Text(
                        call.peerName.ifBlank{call.peerEmail.ifBlank{"RS Member"}},
                        color=Color.White,fontWeight=FontWeight.Black,fontSize=25.sp
                    )
                    Text("CALLING…",color=c.gold,fontSize=12.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)
                    Text("RS KICKBOXING · SECURE CALL",color=c.muted,fontSize=9.sp)
                    if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
                    Surface(
                        shape=CircleShape,
                        color=Color(0xFF4A0C0C),
                        border=BorderStroke(2.dp,Color(0xFFFF5C5C).copy(alpha=.78f)),
                        modifier=Modifier.size(76.dp).clickable(enabled=!callActionBusy){
                            if(!callActionBusy){
                                callActionBusy=true
                                scope.launch{
                                    try{
                                        rsSetCallStatusReliableV173(call.id,"CANCELLED")
                                            .onSuccess{
                                                outgoing=null
                                                status=""
                                                onCallSessionFinished?.invoke()
                                            }
                                            .onFailure{status=it.message.orEmpty().ifBlank{"Could not cancel the call."}}
                                    }finally{
                                        callActionBusy=false
                                    }
                                }
                            }
                        }
                    ){
                        Box(contentAlignment=Alignment.Center){
                            Text("✕",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Black)
                        }
                    }
                    Text("CANCEL",color=Color(0xFFFF8A80),fontSize=9.sp,fontWeight=FontWeight.Black)
                }
            }
        }
    }

    accepted?.let{call->
        RsActiveCallDialogV133(c,lang,call,myId){
            rsCancelIncomingCallNotificationV134(context,call.id)
            accepted=null
            onCallSessionFinished?.invoke()
        }
    }
}

package com.rskickbox.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

private fun rsCallT133(lang:RsLang,key:String):String{
    val en=mapOf(
        "audio" to "Audio call","video" to "Video call","calling" to "Calling…",
        "incoming_audio" to "Incoming audio call","incoming_video" to "Incoming video call",
        "accept" to "Accept","decline" to "Decline","cancel" to "Cancel",
        "connecting" to "Connecting…","connected" to "Connected","reconnecting" to "Reconnecting…",
        "failed" to "Connection failed","ended" to "Call ended","end" to "End call",
        "mic" to "Mic","camera" to "Camera","switch" to "Switch camera",
        "permission" to "Microphone/camera permission is required.",
        "online" to "Online","offline" to "Offline","offline_call" to "Both trainer and student must be online to call.",
        "unavailable" to "Call service is not available yet. Run the latest Supabase call update."
    )
    val nl=en+mapOf(
        "audio" to "Audiogesprek","video" to "Videogesprek","calling" to "Bellen…",
        "incoming_audio" to "Inkomend audiogesprek","incoming_video" to "Inkomend videogesprek",
        "accept" to "Opnemen","decline" to "Weigeren","cancel" to "Annuleren",
        "connecting" to "Verbinden…","connected" to "Verbonden","reconnecting" to "Opnieuw verbinden…",
        "failed" to "Verbinding mislukt","ended" to "Gesprek beëindigd","end" to "Gesprek stoppen",
        "mic" to "Microfoon","camera" to "Camera","switch" to "Camera wisselen",
        "permission" to "Toegang tot microfoon/camera is vereist.",
        "online" to "Online","offline" to "Offline","offline_call" to "Trainer en student moeten allebei online zijn om te bellen.",
        "unavailable" to "Belservice is nog niet beschikbaar. Voer de nieuwste Supabase-callupdate uit."
    )
    val pt=en+mapOf(
        "audio" to "Chamada de áudio","video" to "Videochamada","calling" to "A chamar…",
        "incoming_audio" to "Chamada de áudio recebida","incoming_video" to "Videochamada recebida",
        "accept" to "Atender","decline" to "Recusar","cancel" to "Cancelar",
        "connecting" to "A ligar…","connected" to "Ligado","reconnecting" to "A reconectar…",
        "failed" to "Ligação falhou","ended" to "Chamada terminada","end" to "Terminar chamada",
        "mic" to "Microfone","camera" to "Câmara","switch" to "Trocar câmara",
        "permission" to "É necessária permissão para microfone/câmara.",
        "online" to "Online","offline" to "Offline","offline_call" to "Treinador e aluno têm de estar online para ligar.",
        "unavailable" to "O serviço de chamadas ainda não está disponível. Executa a atualização Supabase mais recente."
    )
    val es=en+mapOf(
        "audio" to "Llamada de audio","video" to "Videollamada","calling" to "Llamando…",
        "incoming_audio" to "Llamada de audio entrante","incoming_video" to "Videollamada entrante",
        "accept" to "Aceptar","decline" to "Rechazar","cancel" to "Cancelar",
        "connecting" to "Conectando…","connected" to "Conectado","reconnecting" to "Reconectando…",
        "failed" to "Falló la conexión","ended" to "Llamada finalizada","end" to "Finalizar llamada",
        "mic" to "Micrófono","camera" to "Cámara","switch" to "Cambiar cámara",
        "permission" to "Se requiere permiso de micrófono/cámara.",
        "online" to "Online","offline" to "Offline","offline_call" to "Entrenador y alumno deben estar conectados para llamar.",
        "unavailable" to "El servicio de llamadas aún no está disponible. Ejecuta la última actualización de Supabase."
    )
    val fr=en+mapOf(
        "audio" to "Appel audio","video" to "Appel vidéo","calling" to "Appel…",
        "incoming_audio" to "Appel audio entrant","incoming_video" to "Appel vidéo entrant",
        "accept" to "Accepter","decline" to "Refuser","cancel" to "Annuler",
        "connecting" to "Connexion…","connected" to "Connecté","reconnecting" to "Reconnexion…",
        "failed" to "Échec de connexion","ended" to "Appel terminé","end" to "Terminer l’appel",
        "mic" to "Micro","camera" to "Caméra","switch" to "Changer caméra",
        "permission" to "L’autorisation micro/caméra est requise.",
        "online" to "En ligne","offline" to "Hors ligne","offline_call" to "Le trainer et l’élève doivent être en ligne pour appeler.",
        "unavailable" to "Le service d’appel n’est pas encore disponible. Exécute la dernière mise à jour Supabase."
    )
    val de=en+mapOf("audio" to "Audioanruf","video" to "Videoanruf","accept" to "Annehmen","decline" to "Ablehnen","cancel" to "Abbrechen","end" to "Anruf beenden")
    val it=en+mapOf("audio" to "Chiamata audio","video" to "Videochiamata","accept" to "Accetta","decline" to "Rifiuta","cancel" to "Annulla","end" to "Termina chiamata")
    val pl=en+mapOf("audio" to "Połączenie audio","video" to "Połączenie wideo","accept" to "Odbierz","decline" to "Odrzuć","cancel" to "Anuluj","end" to "Zakończ połączenie")
    val tr=en+mapOf("audio" to "Sesli arama","video" to "Görüntülü arama","accept" to "Kabul et","decline" to "Reddet","cancel" to "İptal","end" to "Aramayı bitir")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsDirectCallControlsV133(
    c:RsPalette,
    lang:RsLang,
    role:RsRole,
    peerId:String,
    peerName:String,
    peerEmail:String="",
    allowAudio:Boolean=true,
    allowVideo:Boolean=true,
    compact:Boolean=false
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val myId=remember{rsSupabaseClientV60()?.auth?.currentUserOrNull()?.id.orEmpty()}
    var resolvedPeerId by remember(peerId){mutableStateOf(peerId)}
    var resolvedPeerName by remember(peerName){mutableStateOf(peerName)}
    var resolvedPeerEmail by remember(peerEmail){mutableStateOf(peerEmail)}
    var peerOnline by remember{mutableStateOf(false)}
    var selfOnline by remember{mutableStateOf(false)}
    var active by remember{mutableStateOf<RsCallV131?>(null)}
    var status by remember{mutableStateOf("")}
    var pendingType by remember{mutableStateOf<String?>(null)}
    var pendingAcceptCallId by remember{mutableStateOf<String?>(null)}
    var refresh by remember{mutableIntStateOf(0)}

    LaunchedEffect(role,peerId){
        if(role==RsRole.STUDENT&&resolvedPeerId.isBlank()){
            rsStudentCallPeerV131()
                .onSuccess{peer->
                    if(peer!=null){
                        resolvedPeerId=peer.userId
                        resolvedPeerName=peer.displayName.ifBlank{peer.email}
                        resolvedPeerEmail=peer.email
                    }
                }
        }
    }

    LaunchedEffect(resolvedPeerId){
        while(isActive){
            if(resolvedPeerId.isNotBlank()){
                rsTouchPresenceV125()
                rsCallPeerStatusV132(resolvedPeerId)
                    .onSuccess{presence->
                        selfOnline=presence?.selfOnline==true
                        peerOnline=presence?.peerOnline==true
                        if(presence!=null){
                            if(presence.peerName.isNotBlank())resolvedPeerName=presence.peerName
                            if(presence.peerEmail.isNotBlank())resolvedPeerEmail=presence.peerEmail
                        }
                    }
                    .onFailure{
                        selfOnline=false
                        peerOnline=false
                    }
            }
            delay(5000)
        }
    }

    LaunchedEffect(resolvedPeerId,refresh){
        while(isActive){
            rsCallInboxV131()
                .onSuccess{list->
                    val now=list.firstOrNull{
                        it.peerId==resolvedPeerId && it.status in setOf("RINGING","ACCEPTED")
                    }
                    active=now
                }
                .onFailure{
                    if(status.isBlank())status=rsCallT133(lang,"unavailable")
                }
            delay(1500)
        }
    }

    fun beginCall(type:String){
        if(resolvedPeerId.isBlank())return
        // FCM can wake the peer even when they are not actively present in the app.
        // Presence is informative only; it must not block a real call.
        scope.launch{
            status=rsCallT133(lang,"calling")
            rsStartDirectCallV131(resolvedPeerId,type)
                .onSuccess{refresh++}
                .onFailure{status=it.message?:rsCallT133(lang,"unavailable")}
        }
    }

    val permissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){result->
        val type=pendingType
        val acceptCallId=pendingAcceptCallId
        pendingType=null
        pendingAcceptCallId=null
        if(result.values.all{it}){
            when{
                type!=null->beginCall(type)
                acceptCallId!=null->scope.launch{
                    rsSetCallStatusV131(acceptCallId,"ACCEPTED")
                        .onSuccess{refresh++}
                        .onFailure{status=it.message?:rsCallT133(lang,"unavailable")}
                }
            }
        }else if(type!=null||acceptCallId!=null){
            status=rsCallT133(lang,"permission")
        }
    }

    fun requestAndCall(type:String){
        val needs=buildList{
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)add(Manifest.permission.RECORD_AUDIO)
            if(type=="VIDEO"&&ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)add(Manifest.permission.CAMERA)
        }
        if(needs.isEmpty())beginCall(type)
        else{
            pendingType=type
            permissionLauncher.launch(needs.toTypedArray())
        }
    }

    val call=active
    if(compact){
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(
                color=Color.Transparent,
                shape=CircleShape,
                border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
                shadowElevation=10.dp,
                modifier=Modifier.size(39.dp).clickable(
                    enabled=allowAudio&&resolvedPeerId.isNotBlank()
                ){requestAndCall("AUDIO")}
            ){Box(contentAlignment=Alignment.Center){Text("☎",color=c.bright,fontSize=17.sp,fontWeight=FontWeight.Black)}}
            Surface(
                color=Color.Transparent,
                shape=CircleShape,
                border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
                shadowElevation=10.dp,
                modifier=Modifier.size(39.dp).clickable(
                    enabled=allowVideo&&resolvedPeerId.isNotBlank()
                ){requestAndCall("VIDEO")}
            ){Box(contentAlignment=Alignment.Center){Text("▣",color=c.bright,fontSize=16.sp,fontWeight=FontWeight.Black)}}
        }
        return
    }
    Surface(
        color=c.panel.copy(alpha=.55f),
        shape=RoundedCornerShape(20.dp),
        border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
        modifier=Modifier.fillMaxWidth()
    ){
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(12.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    RsMemberAvatarV68(
                        c,
                        resolvedPeerEmail,
                        resolvedPeerName.ifBlank{resolvedPeerEmail.ifBlank{"RS Member"}},
                        size=54.dp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if(peerOnline)rsCallT133(lang,"online") else rsCallT133(lang,"offline"),
                        color=if(peerOnline)Color(0xFF45D483) else c.muted,
                        fontSize=9.sp,
                        fontWeight=FontWeight.Black
                    )
                }
                Column(Modifier.weight(1f)){
                    Text(
                        resolvedPeerName.ifBlank{resolvedPeerEmail.ifBlank{"RS Member"}},
                        color=c.bright,
                        fontWeight=FontWeight.Black,
                        fontSize=14.sp
                    )
                    if(resolvedPeerEmail.isNotBlank())Text(resolvedPeerEmail,color=c.muted,fontSize=9.sp)
                }
            }
            if(call==null){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(
                        onClick={requestAndCall("AUDIO")},
                        enabled=allowAudio&&resolvedPeerId.isNotBlank(),
                        modifier=Modifier.weight(1f)
                    ){Text("☎  "+rsCallT133(lang,"audio"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={requestAndCall("VIDEO")},
                        enabled=allowVideo&&resolvedPeerId.isNotBlank(),
                        modifier=Modifier.weight(1f)
                    ){Text("▣  "+rsCallT133(lang,"video"),fontSize=10.sp)}
                }
                if(!peerOnline)Text("Push calling ready · recipient may be outside the app",color=c.muted,fontSize=9.sp)
            }else if(call.status=="RINGING"){
                val incoming=call.calleeId==myId
                Text(
                    if(incoming){
                        if(call.callType=="VIDEO")rsCallT133(lang,"incoming_video") else rsCallT133(lang,"incoming_audio")
                    }else rsCallT133(lang,"calling")+" "+resolvedPeerName,
                    color=c.bright,fontWeight=FontWeight.Black
                )
                if(incoming){
                    val type=call.callType
                    val acceptNeeds=buildList{
                        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)add(Manifest.permission.RECORD_AUDIO)
                        if(type=="VIDEO"&&ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)add(Manifest.permission.CAMERA)
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(
                            onClick={
                                if(acceptNeeds.isEmpty()){
                                    scope.launch{
                                        rsSetCallStatusV131(call.id,"ACCEPTED")
                                        refresh++
                                    }
                                }else{
                                    pendingType=null
                                    pendingAcceptCallId=call.id
                                    permissionLauncher.launch(acceptNeeds.toTypedArray())
                                }
                            },
                            modifier=Modifier.weight(1f)
                        ){Text(rsCallT133(lang,"accept"))}
                        OutlinedButton(
                            onClick={scope.launch{rsSetCallStatusV131(call.id,"DECLINED");refresh++}},
                            modifier=Modifier.weight(1f)
                        ){Text(rsCallT133(lang,"decline"))}
                    }
                }else{
                    OutlinedButton(
                        onClick={scope.launch{rsSetCallStatusV131(call.id,"CANCELLED");refresh++}},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsCallT133(lang,"cancel"))}
                }
            }
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)
        }
    }

}

@Composable
fun RsActiveCallDialogV133(
    c:RsPalette,
    lang:RsLang,
    call:RsCallV131,
    myId:String,
    onClosed:()->Unit
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val isCaller=call.callerId==myId
    val isVideo=call.callType=="VIDEO"
    var engineState by remember(call.id){mutableStateOf("CONNECTING")}
    var micOn by remember(call.id){mutableStateOf(true)}
    var cameraOn by remember(call.id){mutableStateOf(isVideo)}
    var speakerOn by remember(call.id){mutableStateOf(isVideo)}
    var closing by remember(call.id){mutableStateOf(false)}
    val audioManager=remember{context.getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    val engine=remember(call.id){
        RsWebRtcEngineV132(context,call.id,isCaller,isVideo,scope){engineState=it}
    }

    LaunchedEffect(engine){
        rsConfigureCallPipV154(context,true,if(isVideo)9 else 1,if(isVideo)16 else 1)
        audioManager.mode=AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn=speakerOn
        engine.start()
    }
    LaunchedEffect(call.id){
        while(isActive){
            rsCallInboxV131().onSuccess{list->
                val current=list.firstOrNull{it.id==call.id}
                if(!closing && (current==null||current.status in setOf("ENDED","DECLINED","MISSED","CANCELLED"))){
                    closing=true
                    onClosed()
                }
            }
            delay(1500)
        }
    }
    DisposableEffect(engine){
        onDispose{
            rsConfigureCallPipV154(context,false)
            engine.dispose()
            runCatching{
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn=false
                audioManager.mode=AudioManager.MODE_NORMAL
            }
        }
    }

    Dialog(
        onDismissRequest={},
        properties=DialogProperties(usePlatformDefaultWidth=false)
    ){
        Box(Modifier.fillMaxSize().background(Color.Black)){
            Image(
                painter=painterResource(R.drawable.rs_launcher_royal_v129),
                contentDescription="RS KICKBOXING",
                modifier=Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top=8.dp).size(64.dp)
            )

            if(isVideo){
                var remoteRenderer by remember{mutableStateOf<SurfaceViewRenderer?>(null)}
                var localRenderer by remember{mutableStateOf<SurfaceViewRenderer?>(null)}

                AndroidView(
                    factory={ctx->
                        SurfaceViewRenderer(ctx).also{view->
                            remoteRenderer=view
                            engine.attachRenderers(localRenderer,view)
                        }
                    },
                    modifier=Modifier.fillMaxSize()
                )
                AndroidView(
                    factory={ctx->
                        SurfaceViewRenderer(ctx).also{view->
                            localRenderer=view
                            engine.attachRenderers(view,remoteRenderer)
                        }
                    },
                    modifier=Modifier.align(Alignment.TopEnd).padding(14.dp).width(116.dp).height(164.dp)
                )

                DisposableEffect(call.id){
                    onDispose{
                        val local=localRenderer
                        val remote=remoteRenderer
                        engine.detachRenderers(local,remote)
                        runCatching{local?.release()}
                        runCatching{remote?.release()}
                        localRenderer=null
                        remoteRenderer=null
                    }
                }
            }else{
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    RsMemberAvatarV68(
                        c,
                        call.peerEmail,
                        call.peerName.ifBlank{call.peerEmail},
                        size=138.dp
                    )
                    Text(call.peerName.ifBlank{call.peerEmail},color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Black)
                    Text("RS AUDIO CALL",color=c.gold,fontSize=11.sp,fontWeight=FontWeight.Black)
                }
            }

            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha=.86f),
                                Color.Black.copy(alpha=.97f)
                            )
                        )
                    )
                    .padding(horizontal=16.dp,vertical=18.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(14.dp)
            ){
                Text(
                    when(engineState){
                        "CONNECTED"->rsCallT133(lang,"connected")
                        "RECONNECTING"->rsCallT133(lang,"reconnecting")
                        "FAILED"->rsCallT133(lang,"failed")
                        "ENDED"->rsCallT133(lang,"ended")
                        else->rsCallT133(lang,"connecting")
                    },
                    color=Color.White,fontWeight=FontWeight.Bold
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceEvenly,
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Surface(
                            shape=CircleShape,
                            color=Color.Black.copy(alpha=.72f),
                            border=BorderStroke(1.5.dp,c.gold.copy(alpha=.48f)),
                            tonalElevation=10.dp,
                            modifier=Modifier.size(58.dp).clickable{
                                rsEnterCallPipV154(context,if(isVideo)9 else 1,if(isVideo)16 else 1)
                            }
                        ){Box(contentAlignment=Alignment.Center){Text("↙",color=c.bright,fontSize=21.sp,fontWeight=FontWeight.Black)}}
                        Text("MINIMIZE",color=c.bright,fontSize=7.sp,fontWeight=FontWeight.Black)
                    }
                    fun premiumControl(
                        symbol:String,
                        label:String,
                        active:Boolean=true,
                        danger:Boolean=false,
                        action:()->Unit
                    ) = Unit

                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Surface(
                            shape=CircleShape,
                            color=if(micOn)c.gold.copy(alpha=.15f) else Color(0xFF2A1010),
                            border=BorderStroke(1.5.dp,if(micOn)c.bright.copy(alpha=.72f) else Color(0xFFFF7777).copy(alpha=.60f)),
                            tonalElevation=10.dp,
                            modifier=Modifier.size(58.dp).clickable{
                                micOn=!micOn
                                engine.setMicEnabled(micOn)
                            }
                        ){Box(contentAlignment=Alignment.Center){Text(if(micOn)"🎙" else "🔇",fontSize=20.sp)}}
                        Text("MIC",color=if(micOn)c.bright else Color(0xFFFF9999),fontSize=7.sp,fontWeight=FontWeight.Black)
                    }

                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Surface(
                            shape=CircleShape,
                            color=if(speakerOn)c.gold.copy(alpha=.15f) else Color.Black.copy(alpha=.72f),
                            border=BorderStroke(1.5.dp,c.gold.copy(alpha=.48f)),
                            tonalElevation=10.dp,
                            modifier=Modifier.size(58.dp).clickable{
                                speakerOn=!speakerOn
                                @Suppress("DEPRECATION")
                                audioManager.isSpeakerphoneOn=speakerOn
                            }
                        ){Box(contentAlignment=Alignment.Center){Text(if(speakerOn)"🔊" else "🔈",fontSize=20.sp)}}
                        Text("SPEAKER",color=c.bright,fontSize=7.sp,fontWeight=FontWeight.Black)
                    }

                    if(isVideo){
                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Surface(
                                shape=CircleShape,
                                color=if(cameraOn)c.gold.copy(alpha=.15f) else Color(0xFF2A1010),
                                border=BorderStroke(1.5.dp,if(cameraOn)c.bright.copy(alpha=.72f) else Color(0xFFFF7777).copy(alpha=.60f)),
                                tonalElevation=10.dp,
                                modifier=Modifier.size(58.dp).clickable{
                                    cameraOn=!cameraOn
                                    engine.setCameraEnabled(cameraOn)
                                }
                            ){Box(contentAlignment=Alignment.Center){Text(if(cameraOn)"▣" else "□",color=c.bright,fontSize=19.sp,fontWeight=FontWeight.Black)}}
                            Text("CAMERA",color=if(cameraOn)c.bright else Color(0xFFFF9999),fontSize=7.sp,fontWeight=FontWeight.Black)
                        }

                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Surface(
                                shape=CircleShape,
                                color=Color.Black.copy(alpha=.72f),
                                border=BorderStroke(1.5.dp,c.gold.copy(alpha=.48f)),
                                tonalElevation=10.dp,
                                modifier=Modifier.size(58.dp).clickable{engine.switchCamera()}
                            ){Box(contentAlignment=Alignment.Center){Text("↻",color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)}}
                            Text("SWITCH",color=c.bright,fontSize=7.sp,fontWeight=FontWeight.Black)
                        }
                    }

                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Surface(
                            shape=CircleShape,
                            color=Color(0xFF4A0C0C),
                            border=BorderStroke(2.dp,Color(0xFFFF5C5C).copy(alpha=.78f)),
                            tonalElevation=12.dp,
                            modifier=Modifier.size(64.dp).clickable(enabled=!closing){
                                if(!closing){
                                    closing=true
                                    scope.launch{
                                        runCatching{rsSetCallStatusV131(call.id,"ENDED")}
                                        onClosed()
                                    }
                                }
                            }
                        ){Box(contentAlignment=Alignment.Center){Text("✕",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Black)}}
                        Text("END",color=Color(0xFFFF8A80),fontSize=7.sp,fontWeight=FontWeight.Black)
                    }
                }
            }
        }
    }
}

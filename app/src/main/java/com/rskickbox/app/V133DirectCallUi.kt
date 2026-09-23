package com.rskickbox.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalConfiguration
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
    var actionBusy by remember{mutableStateOf(false)}

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
        if(resolvedPeerId.isBlank() || actionBusy || active!=null)return
        // FCM can wake the peer even when they are not actively present in the app.
        // Presence is informative only; it must not block a real call.
        actionBusy=true
        scope.launch{
            status=rsCallT133(lang,"calling")
            rsStartDirectCallV131(resolvedPeerId,type)
                .onSuccess{refresh++}
                .onFailure{status=it.message?:rsCallT133(lang,"unavailable")}
            actionBusy=false
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
                    rsSetCallStatusReliableV173(acceptCallId,"ACCEPTED")
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
                    enabled=allowAudio&&resolvedPeerId.isNotBlank()&&call==null&&!actionBusy
                ){requestAndCall("AUDIO")}
            ){Box(contentAlignment=Alignment.Center){Text("☎",color=c.bright,fontSize=17.sp,fontWeight=FontWeight.Black)}}
            Surface(
                color=Color.Transparent,
                shape=CircleShape,
                border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
                shadowElevation=10.dp,
                modifier=Modifier.size(39.dp).clickable(
                    enabled=allowVideo&&resolvedPeerId.isNotBlank()&&call==null&&!actionBusy
                ){requestAndCall("VIDEO")}
            ){Box(contentAlignment=Alignment.Center){Text("📹",color=c.bright,fontSize=16.sp,fontWeight=FontWeight.Black)}}
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
                    Text(
                        "Incoming call is handled in the full-screen RS call view.",
                        color=c.muted,fontSize=9.sp
                    )
                }else{
                    OutlinedButton(
                        onClick={
                            if(!actionBusy){
                                actionBusy=true
                                scope.launch{
                                    rsSetCallStatusReliableV173(call.id,"CANCELLED")
                                        .onSuccess{refresh++}
                                        .onFailure{status=it.message?:rsCallT133(lang,"unavailable")}
                                    actionBusy=false
                                }
                            }
                        },
                        enabled=!actionBusy,
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
    var moreOpen by remember(call.id){mutableStateOf(false)}
    var callSeconds by remember(call.id){mutableLongStateOf(0L)}
    var inPip by remember(call.id){mutableStateOf((context as? Activity)?.isInPictureInPictureMode==true)}
    var pipTransition by remember(call.id){mutableStateOf(false)}
    val audioManager=remember{context.getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    val engine=remember(call.id){
        RsWebRtcEngineV132(context,call.id,isCaller,isVideo,scope){engineState=it}
    }

    LaunchedEffect(engine){
        rsConfigureCallPipV154(context,true,if(isVideo)9 else 1,if(isVideo)16 else 1)
        (context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        audioManager.mode=AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(null,AudioManager.STREAM_VOICE_CALL,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn=speakerOn
        engine.start()
    }
    LaunchedEffect(call.id,engineState){
        if(engineState=="CONNECTED"){
            while(isActive){
                delay(1000)
                callSeconds++
            }
        }
    }

    LaunchedEffect(call.id){
        while(isActive){
            inPip=(context as? Activity)?.isInPictureInPictureMode==true
            if(inPip)pipTransition=true
            else if(pipTransition){
                delay(500)
                if((context as? Activity)?.isInPictureInPictureMode!=true)pipTransition=false
            }
            rsCallInboxV131().onSuccess{list->
                val current=list.firstOrNull{it.id==call.id}
                if(!closing && (current==null||current.status in setOf("ENDED","DECLINED","MISSED","CANCELLED"))){
                    closing=true
                    onClosed()
                }
            }
            delay(if(inPip)250 else 900)
        }
    }
    DisposableEffect(engine){
        onDispose{
            rsConfigureCallPipV154(context,false)
            engine.dispose()
            runCatching{
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn=false
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
                audioManager.mode=AudioManager.MODE_NORMAL
                (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Dialog(
        onDismissRequest={},
        properties=DialogProperties(usePlatformDefaultWidth=false)
    ){
        Box(Modifier.fillMaxSize().background(Color.Black)){
            if(!inPip && !pipTransition){
                Image(
                    painter=painterResource(R.drawable.rs_launcher_royal_v129),
                    contentDescription="RS KICKBOXING",
                    modifier=Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top=8.dp).size(64.dp)
                )
            }

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
                if(!inPip && !pipTransition){
                    AndroidView(
                        factory={ctx->
                            SurfaceViewRenderer(ctx).also{view->
                                localRenderer=view
                                engine.attachRenderers(view,remoteRenderer)
                            }
                        },
                        modifier=Modifier.align(Alignment.TopEnd).padding(10.dp).width(92.dp).height(128.dp)
                    )
                }

                LaunchedEffect(inPip,pipTransition){
                    if(inPip || pipTransition){
                        val local=localRenderer
                        if(local!=null){
                            engine.detachRenderers(local,null)
                            runCatching{local.release()}
                            localRenderer=null
                        }
                    }
                }

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
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF050505),c.gold.copy(alpha=.08f),Color.Black)
                        )
                    )
                )
                Column(
                    Modifier.align(Alignment.Center).padding(bottom=150.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(14.dp)
                ){
                    Surface(
                        shape=CircleShape,
                        color=Color.Black.copy(alpha=.82f),
                        border=BorderStroke(3.dp,c.gold.copy(alpha=.78f)),
                        tonalElevation=20.dp,
                        modifier=Modifier.size(214.dp)
                    ){
                        Box(contentAlignment=Alignment.Center){
                            RsMemberAvatarV68(
                                c,
                                call.peerEmail,
                                call.peerName.ifBlank{call.peerEmail},
                                size=204.dp
                            )
                        }
                    }
                    Text(
                        call.peerName.ifBlank{call.peerEmail.ifBlank{"RS Member"}},
                        color=Color.White,
                        fontSize=27.sp,
                        fontWeight=FontWeight.Black,
                        maxLines=1
                    )
                    Text("RS AUDIO CALL · SECURE",color=c.gold,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp)
                }
            }

            if(!inPip && !pipTransition){
                val screenWidth=LocalConfiguration.current.screenWidthDp
                val compactControls=screenWidth<380
                val controlSize=if(compactControls)62.dp else 70.dp
                val labelSize=if(compactControls)8.sp else 9.sp

                Column(
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha=.82f),
                                    Color.Black.copy(alpha=.98f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal=if(compactControls)10.dp else 18.dp,vertical=16.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    Surface(
                        color=Color.Black.copy(alpha=.74f),
                        shape=RoundedCornerShape(24.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.32f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=9.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Column(Modifier.weight(1f)){
                                Text(
                                    call.peerName.ifBlank{call.peerEmail.ifBlank{"RS Member"}},
                                    color=Color.White,fontWeight=FontWeight.Black,fontSize=14.sp,maxLines=1
                                )
                                Text(
                                    when(engineState){
                                        "CONNECTED"->rsCallT133(lang,"connected")+" · "+String.format("%02d:%02d",callSeconds/60,callSeconds%60)
                                        "RECONNECTING"->rsCallT133(lang,"reconnecting")
                                        "FAILED"->rsCallT133(lang,"failed")
                                        "ENDED"->rsCallT133(lang,"ended")
                                        else->rsCallT133(lang,"connecting")
                                    },
                                    color=if(engineState=="CONNECTED")Color(0xFF55D58A) else c.gold,
                                    fontSize=9.sp,fontWeight=FontWeight.Bold
                                )
                            }
                            Text(if(isVideo)"VIDEO" else "AUDIO",color=c.bright,fontSize=9.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp)
                        }
                    }

                    Surface(
                        color=Color(0xFF111416).copy(alpha=.96f),
                        shape=RoundedCornerShape(34.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.28f)),
                        tonalElevation=18.dp,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=14.dp),
                            verticalArrangement=Arrangement.spacedBy(12.dp)
                        ){
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement=Arrangement.SpaceEvenly,
                                verticalAlignment=Alignment.Top
                            ){
                                RsRoyalCallControlV174(
                                    c,if(speakerOn)"🔊" else "🔈","SPEAKER",
                                    controlSize,labelSize,speakerOn,false
                                ){
                                    speakerOn=!speakerOn
                                    @Suppress("DEPRECATION")
                                    audioManager.isSpeakerphoneOn=speakerOn
                                }
                                RsRoyalCallControlV174(
                                    c,if(micOn)"🎙" else "🔇","MUTE",
                                    controlSize,labelSize,micOn.not(),false
                                ){
                                    micOn=!micOn
                                    engine.setMicEnabled(micOn)
                                }
                                if(isVideo){
                                    RsRoyalCallControlV174(
                                        c,if(cameraOn)"📹" else "🚫","VIDEO",
                                        controlSize,labelSize,cameraOn.not(),false
                                    ){
                                        cameraOn=!cameraOn
                                        engine.setCameraEnabled(cameraOn)
                                    }
                                }else{
                                    RsRoyalCallControlV174(
                                        c,"↙","MINIMIZE",
                                        controlSize,labelSize,false,false
                                    ){
                                        pipTransition=true
                                        if(!rsEnterCallPipV154(context,1,1))pipTransition=false
                                    }
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement=Arrangement.SpaceEvenly,
                                verticalAlignment=Alignment.Top
                            ){
                                if(isVideo){
                                    RsRoyalCallControlV174(
                                        c,"↙","MINIMIZE",
                                        controlSize,labelSize,false,false
                                    ){
                                        pipTransition=true
                                        if(!rsEnterCallPipV154(context,9,16))pipTransition=false
                                    }
                                    RsRoyalCallControlV174(
                                        c,"↻","SWITCH",
                                        controlSize,labelSize,false,false
                                    ){engine.switchCamera()}
                                }else{
                                    RsRoyalCallControlV174(
                                        c,"•••","MORE",
                                        controlSize,labelSize,false,false
                                    ){moreOpen=true}
                                }
                                RsRoyalCallControlV174(
                                    c,"☎","END",
                                    controlSize,labelSize,false,true,
                                    enabled=!closing
                                ){
                                    if(!closing){
                                        closing=true
                                        scope.launch{
                                            runCatching{rsSetCallStatusReliableV173(call.id,"ENDED")}
                                            onClosed()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(moreOpen && !inPip && !pipTransition){
                Dialog(onDismissRequest={moreOpen=false}){
                    Surface(
                        color=Color(0xFF101315),
                        shape=RoundedCornerShape(26.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.44f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Text("RS CALL CONTROLS",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                            Text(
                                (if(isVideo)"Video" else "Audio")+" · "+
                                    (if(micOn)"Mic on" else "Mic muted")+" · "+
                                    (if(speakerOn)"Speaker on" else "Speaker off"),
                                color=c.text,fontSize=10.sp
                            )
                            Text(
                                "Connection: "+engineState.lowercase().replaceFirstChar{it.uppercase()}+
                                    if(callSeconds>0)" · "+String.format("%02d:%02d",callSeconds/60,callSeconds%60) else "",
                                color=c.muted,fontSize=9.sp
                            )
                            OutlinedButton(onClick={moreOpen=false},modifier=Modifier.fillMaxWidth()){Text("Close")}
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun RsRoyalCallControlV174(
    c:RsPalette,
    symbol:String,
    label:String,
    size:androidx.compose.ui.unit.Dp,
    labelSize:androidx.compose.ui.unit.TextUnit,
    active:Boolean=false,
    danger:Boolean=false,
    enabled:Boolean=true,
    onClick:()->Unit
){
    Column(
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(6.dp),
        modifier=Modifier.width(size+18.dp)
    ){
        Surface(
            shape=CircleShape,
            color=when{
                danger->Color(0xFFB11226)
                active->c.gold.copy(alpha=.26f)
                else->Color(0xFF20272A)
            },
            border=BorderStroke(
                if(danger)2.dp else 1.5.dp,
                when{
                    danger->Color(0xFFFF5168)
                    active->c.bright.copy(alpha=.88f)
                    else->c.gold.copy(alpha=.44f)
                }
            ),
            tonalElevation=14.dp,
            modifier=Modifier.size(size).clickable(enabled=enabled,onClick=onClick)
        ){
            Box(contentAlignment=Alignment.Center){
                Text(
                    symbol,
                    color=if(danger)Color.White else c.bright,
                    fontSize=if(symbol.length<=2)23.sp else 18.sp,
                    fontWeight=FontWeight.Black
                )
            }
        }
        Text(
            label,
            color=if(danger)Color(0xFFFF8A91) else c.bright,
            fontSize=labelSize,
            fontWeight=FontWeight.Black,
            maxLines=1
        )
    }
}

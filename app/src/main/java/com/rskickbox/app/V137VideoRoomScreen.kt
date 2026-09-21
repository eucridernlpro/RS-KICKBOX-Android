package com.rskickbox.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

@Composable
private fun RsRoomRemoteVideoV137(
    c:RsPalette,
    engine:RsVideoRoomEngineV137,
    member:RsVideoRoomMemberV136,
    state:String,
    modifier:Modifier=Modifier,
    onClick:(()->Unit)?=null
){
    var renderer by remember(member.userId){mutableStateOf<SurfaceViewRenderer?>(null)}
    Box(
        modifier
            .background(Color.Black,RoundedCornerShape(20.dp))
            .then(if(onClick!=null)Modifier.clickable(onClick=onClick) else Modifier)
    ){
        AndroidView(
            factory={ctx->
                SurfaceViewRenderer(ctx).also{view->
                    view.init(engine.eglContext,null)
                    view.setEnableHardwareScaler(true)
                    view.setMirror(false)
                    renderer=view
                    engine.attachRemoteRenderer(member.userId,view)
                }
            },
            modifier=Modifier.fillMaxSize()
        )
        Surface(
            color=Color.Black.copy(alpha=.58f),
            shape=RoundedCornerShape(12.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.25f)),
            modifier=Modifier.align(Alignment.BottomStart).padding(8.dp)
        ){
            Column(Modifier.padding(horizontal=8.dp,vertical=5.dp)){
                Text(
                    member.displayName.ifBlank{member.email},
                    color=Color.White,fontWeight=FontWeight.Black,fontSize=10.sp
                )
                Text(
                    when(state){
                        "CONNECTED"->"LIVE"
                        "RECONNECTING"->"RECONNECTING"
                        "FAILED"->"CONNECTION ISSUE"
                        else->if(member.memberStatus=="JOINED")"CONNECTING" else member.memberStatus
                    },
                    color=if(state=="CONNECTED")Color(0xFF36D27F) else c.gold,
                    fontSize=7.sp,fontWeight=FontWeight.Black
                )
            }
        }
    }
    DisposableEffect(member.userId){
        onDispose{
            renderer?.let{view->
                engine.detachRemoteRenderer(member.userId,view)
                runCatching{view.release()}
            }
            renderer=null
        }
    }
}

@Composable
private fun RsRoomLocalVideoV137(
    c:RsPalette,
    engine:RsVideoRoomEngineV137,
    modifier:Modifier=Modifier
){
    var renderer by remember{mutableStateOf<SurfaceViewRenderer?>(null)}
    Box(modifier.background(Color.Black,RoundedCornerShape(18.dp))){
        AndroidView(
            factory={ctx->
                SurfaceViewRenderer(ctx).also{view->
                    view.init(engine.eglContext,null)
                    view.setMirror(true)
                    view.setEnableHardwareScaler(true)
                    renderer=view
                    engine.attachLocalRenderer(view)
                }
            },
            modifier=Modifier.fillMaxSize()
        )
        Text(
            "YOU",
            color=c.bright,fontWeight=FontWeight.Black,fontSize=8.sp,
            modifier=Modifier.align(Alignment.BottomStart).padding(7.dp)
        )
    }
    DisposableEffect(Unit){
        onDispose{
            renderer?.let{view->
                engine.detachLocalRenderer(view)
                runCatching{view.release()}
            }
            renderer=null
        }
    }
}

@Composable
fun RsVideoRoomScreenV137(
    c:RsPalette,
    lang:RsLang,
    room:RsVideoRoomV136,
    onClosed:()->Unit
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val myId=remember{rsSupabaseClientV60()?.auth?.currentUserOrNull()?.id.orEmpty()}
    val isHost=room.myRole=="HOST"
    var members by remember(room.roomId){mutableStateOf<List<RsVideoRoomMemberV136>>(emptyList())}
    var peerStates by remember(room.roomId){mutableStateOf<Map<String,String>>(emptyMap())}
    var featuredId by remember(room.roomId){mutableStateOf<String?>(null)}
    var micOn by remember(room.roomId){mutableStateOf(true)}
    var cameraOn by remember(room.roomId){mutableStateOf(true)}
    var speakerOn by remember(room.roomId){mutableStateOf(true)}
    var gridMode by remember(room.roomId){mutableStateOf(true)}
    var permissionsReady by remember(room.roomId){mutableStateOf(false)}
    var status by remember(room.roomId){mutableStateOf("")}
    val audioManager=remember{context.getSystemService(Context.AUDIO_SERVICE) as AudioManager}

    val engine=remember(room.roomId){
        RsVideoRoomEngineV137(
            context=context,
            roomId=room.roomId,
            myId=myId,
            isHost=isHost,
            scope=scope
        ){peerId,state->
            scope.launch{
                peerStates=peerStates.toMutableMap().apply{put(peerId,state)}
            }
        }
    }

    val permissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){result->
        permissionsReady=result.values.all{it}
        if(!permissionsReady)status="Camera and microphone permission are required for video sessions."
    }

    LaunchedEffect(room.roomId){
        val needs=buildList{
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)add(Manifest.permission.RECORD_AUDIO)
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)add(Manifest.permission.CAMERA)
        }
        if(needs.isEmpty())permissionsReady=true
        else permissionLauncher.launch(needs.toTypedArray())
    }

    LaunchedEffect(permissionsReady){
        if(permissionsReady){
            audioManager.mode=AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn=speakerOn
            engine.start()
        }
    }

    LaunchedEffect(room.roomId,permissionsReady){
        while(isActive){
            rsVideoRoomMembersV136(room.roomId)
                .onSuccess{
                    members=it
                    if(permissionsReady)engine.syncMembers(it)
                }
                .onFailure{status=it.message.orEmpty()}

            rsMyVideoRoomsV136().onSuccess{rooms->
                if(rooms.none{it.roomId==room.roomId&&it.roomStatus=="OPEN"}){
                    onClosed()
                }
            }
            delay(1200)
        }
    }

    DisposableEffect(engine){
        onDispose{
            engine.dispose()
            runCatching{
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn=false
                audioManager.mode=AudioManager.MODE_NORMAL
            }
        }
    }

    val joinedMembers=members.filter{it.memberStatus=="JOINED"}
    val remoteMembers=joinedMembers.filter{it.userId!=myId}
    val featured=remoteMembers.firstOrNull{it.userId==featuredId}

    Dialog(
        onDismissRequest={},
        properties=DialogProperties(usePlatformDefaultWidth=false)
    ){
        Column(Modifier.fillMaxSize().background(Color.Black)){
            Row(
                Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Column(Modifier.weight(1f)){
                    Text(
                        room.title,
                        color=Color.White,fontWeight=FontWeight.Black,fontSize=16.sp
                    )
                    Text(
                        joinedMembers.size.toString()+" joined · "+members.count{it.online}.toString()+" online",
                        color=c.muted,fontSize=9.sp
                    )
                }
                Surface(
                    color=c.gold.copy(alpha=.10f),
                    shape=RoundedCornerShape(14.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.30f))
                ){
                    Text("♛  RS LIVE",color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp,modifier=Modifier.padding(horizontal=9.dp,vertical=6.dp))
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal=8.dp)){
                if(!permissionsReady){
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(10.dp)
                    ){
                        Text("CAMERA + MICROPHONE",color=c.gold,fontWeight=FontWeight.Black)
                        Text(status.ifBlank{"Waiting for permission…"},color=c.muted,fontSize=10.sp)
                    }
                }else{
                    if(featured!=null && !gridMode){
                        RsRoomRemoteVideoV137(
                            c,engine,featured,peerStates[featured.userId].orEmpty(),
                            modifier=Modifier.fillMaxSize(),
                            onClick={featuredId=null;gridMode=true}
                        )
                    }else if(remoteMembers.isEmpty()){
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(10.dp)
                        ){
                            Surface(
                                color=c.gold.copy(alpha=.08f),
                                shape=CircleShape,
                                border=BorderStroke(1.dp,c.gold.copy(alpha=.30f)),
                                modifier=Modifier.size(104.dp)
                            ){
                                Box(contentAlignment=Alignment.Center){
                                    Text("♛\nRS",color=c.bright,fontWeight=FontWeight.Black,fontSize=24.sp)
                                }
                            }
                            Text("WAITING FOR PARTICIPANTS",color=c.gold,fontWeight=FontWeight.Black,fontSize=13.sp)
                            Text(
                                "Joined members will appear here automatically.",
                                color=c.muted,fontSize=10.sp
                            )
                        }
                    }else{
                        val columns=when{
                            remoteMembers.size<=1->1
                            remoteMembers.size<=4->2
                            else->3
                        }
                        LazyVerticalGrid(
                            columns=GridCells.Fixed(columns),
                            modifier=Modifier.fillMaxSize(),
                            verticalArrangement=Arrangement.spacedBy(7.dp),
                            horizontalArrangement=Arrangement.spacedBy(7.dp)
                        ){
                            items(remoteMembers,key={it.userId}){member->
                                Surface(
                                    color=Color.Black,
                                    shape=RoundedCornerShape(22.dp),
                                    border=BorderStroke(
                                        1.dp,
                                        if(member.memberRole=="HOST")c.gold.copy(alpha=.65f)
                                        else c.gold.copy(alpha=.28f)
                                    ),
                                    tonalElevation=10.dp,
                                    modifier=Modifier.fillMaxWidth()
                                        .height(
                                            when{
                                                remoteMembers.size==1->420.dp
                                                remoteMembers.size<=4->245.dp
                                                else->180.dp
                                            }
                                        )
                                ){
                                    Box(Modifier.fillMaxSize()){
                                        RsRoomRemoteVideoV137(
                                            c,engine,member,peerStates[member.userId].orEmpty(),
                                            modifier=Modifier.fillMaxSize(),
                                            onClick={featuredId=member.userId;gridMode=false}
                                        )
                                        Surface(
                                            color=Color.Black.copy(alpha=.60f),
                                            shape=RoundedCornerShape(12.dp),
                                            border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
                                            modifier=Modifier.align(Alignment.TopStart).padding(8.dp)
                                        ){
                                            Text(
                                                if(member.memberRole=="HOST")"♛ TRAINER" else "RS MEMBER",
                                                color=if(member.memberRole=="HOST")c.bright else Color.White,
                                                fontSize=7.sp,
                                                fontWeight=FontWeight.Black,
                                                modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if(permissionsReady){
                    RsRoomLocalVideoV137(
                        c,engine,
                        modifier=Modifier.align(Alignment.TopEnd).padding(10.dp).width(105.dp).height(150.dp)
                    )
                }
            }

            if(members.isNotEmpty()){
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=7.dp),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)
                ){
                    items(members.size){index->
                        val member=members[index]
                        Surface(
                            color=if(member.userId==featuredId)c.gold.copy(alpha=.16f) else c.panel.copy(alpha=.50f),
                            shape=RoundedCornerShape(16.dp),
                            border=BorderStroke(1.dp,if(member.userId==featuredId)c.gold.copy(alpha=.55f) else c.gold.copy(alpha=.16f)),
                            modifier=Modifier.width(104.dp).clickable{
                                if(member.userId!=myId && member.memberStatus=="JOINED"){
                                    featuredId=if(featuredId==member.userId)null else member.userId
                                    gridMode=featuredId==null
                                }
                            }
                        ){
                            Row(
                                Modifier.padding(7.dp),
                                verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(6.dp)
                            ){
                                RsMemberAvatarV68(c,member.email,member.displayName,size=28.dp)
                                Column(Modifier.weight(1f)){
                                    Text(
                                        member.displayName.ifBlank{member.email}.take(12),
                                        color=c.bright,fontWeight=FontWeight.Bold,fontSize=8.sp
                                    )
                                    Text(
                                        if(member.online)"ONLINE" else member.memberStatus,
                                        color=if(member.online)Color(0xFF36D27F) else c.muted,
                                        fontSize=6.sp,fontWeight=FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha=.88f),Color.Black)
                        )
                    )
                    .padding(horizontal=10.dp,vertical=9.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceEvenly,
                    verticalAlignment=Alignment.CenterVertically
                ){
                    OutlinedButton(
                        onClick={micOn=!micOn;engine.setMicEnabled(micOn)},
                        modifier=Modifier.size(48.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text(if(micOn)"🎙" else "🔇")}
                    OutlinedButton(
                        onClick={cameraOn=!cameraOn;engine.setCameraEnabled(cameraOn)},
                        modifier=Modifier.size(48.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text(if(cameraOn)"▣" else "□")}
                    OutlinedButton(
                        onClick={
                            speakerOn=!speakerOn
                            @Suppress("DEPRECATION")
                            audioManager.isSpeakerphoneOn=speakerOn
                        },
                        modifier=Modifier.size(48.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text(if(speakerOn)"🔊" else "🔈")}
                    OutlinedButton(
                        onClick={engine.switchCamera()},
                        modifier=Modifier.size(48.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text("↻",fontSize=20.sp)}
                    OutlinedButton(
                        onClick={
                            gridMode=!gridMode
                            if(gridMode)featuredId=null
                            else if(featuredId==null)featuredId=remoteMembers.firstOrNull()?.userId
                        },
                        modifier=Modifier.size(48.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text(if(gridMode)"▦" else "▣",fontSize=18.sp)}
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Text(
                        "♛  RS GROUP VIDEO · "+joinedMembers.size+" LIVE",
                        color=c.gold,
                        fontSize=8.sp,
                        fontWeight=FontWeight.Black
                    )
                    Button(
                        onClick={
                            scope.launch{
                                rsSetVideoRoomStatusV136(room.roomId,if(isHost)"ENDED" else "LEFT")
                                onClosed()
                            }
                        },
                        modifier=Modifier.height(44.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF8D2020))
                    ){
                        Text(if(isHost)"End Session" else "Leave",fontWeight=FontWeight.Black)
                    }
                }
            }
        }
    }
}

package com.rskickbox.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsChatSettingsV162(c:RsPalette,store:RsStore,lang:RsLang){
    var autoMedia by remember{mutableStateOf(store.b("chat_auto_media_preview_v162",true))}
    var autoSpeak by remember{mutableStateOf(store.b("ai_auto_speak_v163",true))}
    var avatarMotion by remember{mutableStateOf(store.b("ai_avatar_motion_v163",true))}
    var swipeMenu by remember{mutableStateOf(store.b("chat_swipe_menu_v163",true))}
    val context=LocalContext.current
    var voiceWake by remember{mutableStateOf(store.b("rs_voice_wake_enabled_v165",false))}
    var voiceWakeStatus by remember{mutableStateOf("")}
    var listenerRevision by remember{mutableIntStateOf(0)}
    val listenerStatus=remember(listenerRevision){
        store.s("rs_voice_wake_status_v168","OFF").ifBlank{"OFF"}
    }
    LaunchedEffect(Unit){
        while(true){
            kotlinx.coroutines.delay(1500)
            listenerRevision++
        }
    }

    val micPermissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){granted->
        if(granted){
            voiceWake=true
            store.pb("rs_voice_wake_enabled_v165",true)
            runCatching{RsVoiceWakeServiceV165.start(context)}
                .onSuccess{voiceWakeStatus="RS Voice Wake is listening."}
                .onFailure{
                    voiceWake=false
                    store.pb("rs_voice_wake_enabled_v165",false)
                    voiceWakeStatus=it.message?:"Could not start RS Voice Wake."
                }
        }else{
            voiceWake=false
            store.pb("rs_voice_wake_enabled_v165",false)
            voiceWakeStatus="Microphone permission is required for RS Voice Wake."
        }
    }

    RsScroll(
        c,
        "RS CHAT SETTINGS",
        "Calls · media · privacy · conversation controls"
    ){
        Surface(
            color=c.panel.copy(alpha=.66f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("MEDIA & DATA",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text(
                    "Control when photos and videos inside chats are loaded on this device.",
                    color=c.muted,fontSize=9.sp
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Column(Modifier.weight(1f)){
                        Text("Automatic media previews",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        Text(
                            if(autoMedia)"Images and videos load automatically in conversations."
                            else "Media stays collapsed until you tap Load media.",
                            color=c.muted,fontSize=8.sp
                        )
                    }
                    Switch(
                        checked=autoMedia,
                        onCheckedChange={
                            autoMedia=it
                            store.pb("chat_auto_media_preview_v162",it)
                        }
                    )
                }
            }
        }

        Surface(
            color=Color.Black.copy(alpha=.66f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.22f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("AI ASSISTANT",color=Color(0xFF58C9FF),fontWeight=FontWeight.Black,fontSize=13.sp)
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Speak AI replies automatically",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        Text("Sofia / Marcus speak after a response.",color=c.muted,fontSize=8.sp)
                    }
                    Switch(autoSpeak,{autoSpeak=it;store.pb("ai_auto_speak_v163",it)})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Realistic avatar motion",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        Text("Subtle breathing / speaking motion for the AI stage.",color=c.muted,fontSize=8.sp)
                    }
                    Switch(avatarMotion,{avatarMotion=it;store.pb("ai_avatar_motion_v163",it)})
                }
                HorizontalDivider(color=Color(0xFF58C9FF).copy(alpha=.14f))
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("RS Voice Wake · beta",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        Text(
                            "Say “Wake up RS” / “Hey RS” or a supported localized wake phrase while the listening service is active.",
                            color=c.muted,fontSize=8.sp,lineHeight=12.sp
                        )
                    }
                    Switch(
                        checked=voiceWake,
                        onCheckedChange={enabled->
                            if(enabled){
                                val granted=ContextCompat.checkSelfPermission(
                                    context,Manifest.permission.RECORD_AUDIO
                                )==PackageManager.PERMISSION_GRANTED
                                if(granted){
                                    voiceWake=true
                                    store.pb("rs_voice_wake_enabled_v165",true)
                                    runCatching{RsVoiceWakeServiceV165.start(context)}
                                        .onSuccess{voiceWakeStatus="RS Voice Wake is listening."}
                                        .onFailure{
                                            voiceWake=false
                                            store.pb("rs_voice_wake_enabled_v165",false)
                                            voiceWakeStatus=it.message?:"Could not start RS Voice Wake."
                                        }
                                }else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }else{
                                voiceWake=false
                                store.pb("rs_voice_wake_enabled_v165",false)
                                RsVoiceWakeServiceV165.stop(context)
                                voiceWakeStatus="RS Voice Wake stopped."
                            }
                        }
                    )
                }
                Text(
                    "Voice Wake uses a foreground microphone service and shows an Android notification. It can continue while RS is minimized and may continue with the screen locked, but Android will stop it after Force Stop.",
                    color=c.muted,fontSize=8.sp,lineHeight=12.sp
                )
                if(voiceWakeStatus.isNotBlank()){
                    Text(voiceWakeStatus,color=Color(0xFF58C9FF),fontSize=8.sp)
                }
                Surface(
                    color=Color.Black.copy(alpha=.34f),
                    shape=RoundedCornerShape(14.dp),
                    border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.18f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(
                        Modifier.padding(10.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)
                    ){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                            Text("VOICE WAKE STATUS",color=c.muted,fontSize=8.sp,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                            Text(
                                listenerStatus,
                                color=if(listenerStatus=="LISTENING")Color(0xFF36D27F) else Color(0xFF58C9FF),
                                fontSize=8.sp,
                                fontWeight=FontWeight.Black
                            )
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedButton(
                                onClick={
                                    store.pb("ai_start_listening_v168",true)
                                    voiceWakeStatus="Opening RS AI microphone…"
                                },
                                modifier=Modifier.weight(1f)
                            ){Text("Test RS AI Mic",fontSize=8.sp)}
                            OutlinedButton(
                                onClick={
                                    runCatching{
                                        context.startActivity(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                },
                                modifier=Modifier.weight(1f)
                            ){Text("Battery Settings",fontSize=8.sp)}
                        }
                        Text(
                            "If status changes from LISTENING to an ERROR state after the screen locks, Android is blocking the background recognizer on this device.",
                            color=c.muted,
                            fontSize=8.sp,
                            lineHeight=11.sp
                        )
                    }
                }
            }
        }

        Surface(
            color=c.panel.copy(alpha=.60f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("CONVERSATION CONTROLS",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text("Every open private or group conversation has a top-right ⋮ menu.",color=c.text,fontSize=9.sp)
                Text(
                    "Message actions use the ⋮ on each post: Reply · Edit · Delete for me · Delete for everyone when permitted.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
                Text(
                    "Swipe a message to the right to reply to it.",
                    color=c.muted,fontSize=9.sp
                )
            }
        }

        Surface(
            color=c.panel.copy(alpha=.58f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("MEDIA RETENTION",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text("Temporary downloaded chat media is cleaned from this device after 7 days.",color=c.text,fontSize=9.sp)
                Text(
                    "Use Save media to Gallery from a message's ⋮ menu to keep a photo, video or audio item permanently.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
            }
        }

        Surface(
            color=c.panel.copy(alpha=.58f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("GESTURES",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("Swipe menu gesture",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        Text("Left → right opens the menu. Right → left closes it.",color=c.muted,fontSize=8.sp)
                    }
                    Switch(swipeMenu,{swipeMenu=it;store.pb("chat_swipe_menu_v163",it)})
                }
                Text("Swipe a message right to reply.",color=c.muted,fontSize=9.sp)
            }
        }

        RsCallRingtoneSettingsV138(c,store,lang)

        Surface(
            color=c.panel.copy(alpha=.54f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.14f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("PRIVACY",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text(
                    "Delete for me hides a message only from your account. Delete for everyone is limited to the sender or trainer/admin where allowed.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
            }
        }
    }
}

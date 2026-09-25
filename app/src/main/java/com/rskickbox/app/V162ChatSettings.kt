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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun RsChatSettingsV162(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole?=null,onOpenAi:()->Unit){
    var autoMedia by remember{mutableStateOf(store.b("chat_auto_media_preview_v162",true))}
    var autoSpeak by remember{mutableStateOf(store.b("ai_auto_speak_v163",true))}
    var avatarMotion by remember{mutableStateOf(store.b("ai_avatar_motion_v163",true))}
    var swipeMenu by remember{mutableStateOf(store.b("chat_swipe_menu_v163",true))}
    val context=LocalContext.current
    var voiceWake by remember{mutableStateOf(store.b("rs_voice_wake_enabled_v165",false))}
    var voiceWakeStatus by remember{mutableStateOf("")}
    var listenerRevision by remember{mutableIntStateOf(0)}
    val saveScope=rememberCoroutineScope()
    var permissionStatus by remember{mutableStateOf("")}
    var permImages by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.STUDENT_IMAGES,true))}
    var permVideos by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.STUDENT_VIDEOS,true))}
    var permAudio by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.STUDENT_AUDIO,true))}
    var permFiles by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.STUDENT_FILES,true))}
    var permCalls by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.PRIVATE_CALLS,true))}
    var permGroupMedia by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.GROUP_MEDIA,true))}
    var permCommunityMedia by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.COMMUNITY_MEDIA,true))}
    var permGallery by remember{mutableStateOf(store.b(RsChatPermissionKeysV178.GALLERY,true))}
    var retentionDays by remember{mutableIntStateOf(store.s(RsChatPermissionKeysV178.RETENTION_DAYS,"7").toIntOrNull()?.coerceIn(1,30)?:7)}
    val listenerStatus=remember(listenerRevision){
        store.s("rs_voice_wake_status_v168","OFF").ifBlank{"OFF"}
    }
    LaunchedEffect(role){
        if(role==RsRole.TRAINER && RsSupabaseV60.configured){
            rsSyncChatPermissionsV178(store).onSuccess{p->
                permImages=p.studentImagesEnabled
                permVideos=p.studentVideosEnabled
                permAudio=p.studentAudioEnabled
                permFiles=p.studentFilesEnabled
                permCalls=p.privateCallsEnabled
                permGroupMedia=p.groupMediaEnabled
                permCommunityMedia=p.communityMediaEnabled
                autoMedia=p.autoMediaPreviewEnabled
                permGallery=p.galleryEnabled
                retentionDays=p.mediaRetentionDays.coerceIn(1,30)
            }
        }
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
                .onSuccess{voiceWakeStatus=rsAiVoiceRuntimeTextV190(lang.code,"voice_wake_listening")}
                .onFailure{
                    voiceWake=false
                    store.pb("rs_voice_wake_enabled_v165",false)
                    voiceWakeStatus=it.message?:rsAiVoiceRuntimeTextV190(lang.code,"voice_wake_start_failed")
                }
        }else{
            voiceWake=false
            store.pb("rs_voice_wake_enabled_v165",false)
            voiceWakeStatus=rsAiVoiceRuntimeTextV190(lang.code,"voice_wake_mic_required")
        }
    }

    val settingsLayout=rsThemeLayoutV175(rsStoredThemeV175(store))
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal=1.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        Surface(
            color=Color.Black.copy(alpha=.78f),
            shape=RoundedCornerShape((settingsLayout.panelRadius+4).dp),
            border=BorderStroke(if(settingsLayout.strongLines)2.dp else 1.dp,c.gold.copy(alpha=.48f)),
            tonalElevation=18.dp,
            modifier=Modifier.fillMaxWidth()
        ){
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ){
                Text(
                    if(role==RsRole.TRAINER)"RS CHAT CONTROL CENTER" else "MY RS CHAT CONTROL",
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=16.sp,
                    letterSpacing=.8.sp
                )
                Text(
                    if(role==RsRole.TRAINER)
                        "Permissions · calls · media · AI · privacy · retention"
                    else
                        "Media · AI assistant · gestures · privacy",
                    color=c.gold,
                    fontSize=8.sp,
                    fontWeight=FontWeight.Black,
                    letterSpacing=1.05.sp
                )
                Box(
                    Modifier.fillMaxWidth().height(2.dp).background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Transparent,c.gold,c.bright,Color.Transparent)
                        )
                    )
                )
            }
        }
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

        if(role==RsRole.TRAINER){
            Surface(
                color=c.panel.copy(alpha=.68f),
                shape=RoundedCornerShape(24.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.30f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("TRAINER CHAT PERMISSIONS",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                    Text(
                        "Central controls are synchronized to student devices. Existing group-level students_can_post / students_can_media rules still apply in addition to these global permissions.",
                        color=c.muted,fontSize=8.sp,lineHeight=12.sp
                    )
                    RsChatPermissionToggleV178(c,"Student image upload","Allow students to send photos.",permImages){permImages=it}
                    RsChatPermissionToggleV178(c,"Student video upload","Allow short training videos.",permVideos){permVideos=it}
                    RsChatPermissionToggleV178(c,"Student voice / audio","Allow recorded voice messages and audio.",permAudio){permAudio=it}
                    RsChatPermissionToggleV178(c,"Student file upload","Allow general supported file attachments.",permFiles){permFiles=it}
                    RsChatPermissionToggleV178(c,"Private audio/video calls","Allow students to start/use private calling controls.",permCalls){permCalls=it}
                    RsChatPermissionToggleV178(c,"Group media","Global media gate for student group chats.",permGroupMedia){permGroupMedia=it}
                    RsChatPermissionToggleV178(c,"Community media","Allow student media posts in Community.",permCommunityMedia){permCommunityMedia=it}
                    RsChatPermissionToggleV178(c,"RS Chat Gallery","Allow the shared chat gallery workflow.",permGallery){permGallery=it}
                    Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("Media retention",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                            Text(retentionDays.toString()+" days",color=c.muted,fontSize=8.sp)
                        }
                        OutlinedButton(onClick={retentionDays=(retentionDays-1).coerceAtLeast(1)}){Text("−")}
                        Spacer(Modifier.width(5.dp))
                        OutlinedButton(onClick={retentionDays=(retentionDays+1).coerceAtMost(30)}){Text("+")}
                    }
                    Button(
                        onClick={
                            permissionStatus="Saving chat permissions…"
                            saveScope.launch{
                                rsSaveChatPermissionsV178(
                                    store,
                                    RsChatPermissionsV178(
                                        studentImagesEnabled=permImages,
                                        studentVideosEnabled=permVideos,
                                        studentAudioEnabled=permAudio,
                                        studentFilesEnabled=permFiles,
                                        privateCallsEnabled=permCalls,
                                        groupMediaEnabled=permGroupMedia,
                                        communityMediaEnabled=permCommunityMedia,
                                        autoMediaPreviewEnabled=autoMedia,
                                        galleryEnabled=permGallery,
                                        mediaRetentionDays=retentionDays
                                    )
                                ).onSuccess{
                                    permissionStatus="Trainer chat permissions saved."
                                }.onFailure{
                                    permissionStatus=it.message?:"Could not save trainer chat permissions."
                                }
                            }
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text("SAVE CHAT PERMISSIONS",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    if(permissionStatus.isNotBlank()){
                        Text(permissionStatus,color=if(permissionStatus.contains("saved",true))Color(0xFF36D27F) else c.muted,fontSize=8.sp)
                    }
                }
            }
        }

        if(role==RsRole.TRAINER){
            RsTrainerCallControlV136(
                c=c,
                lang=lang,
                onRoomCreated={}
            )
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
                val silentWakeEnabled=store.b("rs_voice_wake_silent_engine_v188",true)
                val silentWakeReady=RsOfflineWakeModelV188.installed(context)
                val silentWakeProgress=store.s("rs_silent_wake_download_progress_v188","0")
                    .toIntOrNull()?.coerceIn(0,100)?:0
                Surface(
                    color=Color.Black.copy(alpha=.52f),
                    shape=RoundedCornerShape(18.dp),
                    border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.22f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(
                        Modifier.fillMaxWidth().padding(11.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)
                    ){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("SILENT WAKE ENGINE",color=Color(0xFF58C9FF),fontWeight=FontWeight.Black,fontSize=10.sp)
                                Text(
                                    if(silentWakeReady)
                                        "Offline wake-word model ready · no per-user wake fee"
                                    else if(silentWakeProgress in 1..99)
                                        "Preparing offline model · "+silentWakeProgress+"%"
                                    else
                                        "One-time ~40 MB offline model prepares when Voice Wake is enabled.",
                                    color=c.muted,fontSize=8.sp,lineHeight=11.sp
                                )
                            }
                            Switch(
                                checked=silentWakeEnabled,
                                onCheckedChange={store.pb("rs_voice_wake_silent_engine_v188",it)}
                            )
                        }
                        Text(
                            if(silentWakeEnabled)
                                "Sleeping mode listens locally for Wake up RS / RS wake up / Hey RS. Android speech recognition starts only after wake."
                            else
                                "Silent offline wake is disabled; Android recognition fallback may produce system tones on some phones.",
                            color=c.text.copy(alpha=.78f),
                            fontSize=8.sp,
                            lineHeight=11.sp
                        )
                    }
                }

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
                                        .onSuccess{voiceWakeStatus=rsAiVoiceRuntimeTextV190(lang.code,"voice_wake_listening")}
                                        .onFailure{
                                            voiceWake=false
                                            store.pb("rs_voice_wake_enabled_v165",false)
                                            voiceWakeStatus=it.message?:rsAiVoiceRuntimeTextV190(lang.code,"voice_wake_start_failed")
                                        }
                                }else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }else{
                                voiceWake=false
                                store.pb("rs_voice_wake_enabled_v165",false)
                                RsVoiceWakeServiceV165.stop(context)
                                voiceWakeStatus=rsAiVoiceRuntimeTextV190(lang.code,"voice_wake_stopped")
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
                                    voiceWakeStatus=rsAiVoiceRuntimeTextV190(lang.code,"opening_ai_mic")
                                    onOpenAi()
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
                Text("Temporary chat media follows the trainer retention setting: "+retentionDays+" day"+if(retentionDays==1)"" else "s"+".",color=c.text,fontSize=9.sp)
                Text(
                    if(permGallery)"Use Save media to Gallery from a message's ⋮ menu to keep a photo, video or audio item permanently." else "RS Chat Gallery is disabled by the trainer.",
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

        if(role==RsRole.TRAINER){
            val diagLang=store.s("ai_voice_language_v161",lang.code)
            val diagAvatar=store.s("ai_avatar_gender_v161","FEMALE")
            val diagVoiceKey="ai_voice_override_v181_"+diagLang+"_"+diagAvatar.lowercase()
            val diagVoice=store.s(diagVoiceKey,"")
            val wakeStatus=store.s("rs_voice_wake_status_v168","UNKNOWN")
            val wakeMs=store.s("rs_voice_wake_status_ms_v168","0").toLongOrNull()?:0L
            val wakeAge=if(wakeMs>0L)((System.currentTimeMillis()-wakeMs)/1000L).coerceAtLeast(0L) else -1L
            val sofiaRigged=remember{
                runCatching{context.assets.open("models/rs_ai_sofia.glb").use{};true}.getOrDefault(false)
            }
            val marcusRigged=remember{
                runCatching{context.assets.open("models/rs_ai_marcus.glb").use{};true}.getOrDefault(false)
            }
            Surface(
                color=Color.Black.copy(alpha=.70f),
                shape=RoundedCornerShape(22.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.32f)),
                modifier=Modifier.fillMaxWidth()
            ){
                Column(
                    Modifier.fillMaxWidth().padding(13.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    Text("AI ENGINE DIAGNOSTICS",color=c.bright,fontWeight=FontWeight.Black,fontSize=12.sp)
                    Text(
                        "Device-local voice, wake and 3D status for trainer QC. No password or API key is displayed.",
                        color=c.muted,fontSize=8.sp,lineHeight=11.sp
                    )
                    HorizontalDivider(color=c.gold.copy(alpha=.14f))
                    RsAiDiagLineV191(c,"Assistant",if(diagAvatar=="MALE")"Marcus" else "Sofia")
                    RsAiDiagLineV191(c,"AI language",diagLang.uppercase())
                    RsAiDiagLineV191(c,"Voice profile",if(diagVoice.isBlank())"AUTO" else diagVoice.take(30))
                    val diagPlan=store.s("session_plan",if(role==RsRole.TRAINER)"ELITE" else "BASIC").uppercase()
                    RsAiDiagLineV191(c,"Membership AI tier",if(role==RsRole.TRAINER)"TRAINER · ELITE ENGINE" else diagPlan)
                    RsAiDiagLineV191(
                        c,
                        "Premium cloud voice",
                        when{
                            !RsSupabaseV60.configured->"NOT CONFIGURED · DEVICE TTS"
                            rsUsePremiumCloudVoiceV192(store,role)->"CONFIGURED · CLOUD ELIGIBLE"
                            else->"BASIC · DEVICE TTS"
                        }
                    )
                    RsAiDiagLineV191(
                        c,
                        "AI backend config",
                        if(RsSupabaseV60.configured)"SUPABASE CONFIGURED" else "SUPABASE NOT CONFIGURED"
                    )
                    RsAiDiagLineV191(
                        c,
                        "Silent wake",
                        when{
                            !store.b("rs_voice_wake_silent_engine_v188",true)->"OFF"
                            RsOfflineWakeModelV188.installed(context)->"READY · "+RsOfflineWakeModelV188.version(context)
                            else->"PREPARING / FALLBACK"
                        }
                    )
                    RsAiDiagLineV191(c,"Wake state",wakeStatus+(if(wakeAge>=0)" · "+wakeAge+"s" else ""))
                    RsAiDiagLineV191(c,"Wake security","BIOMETRIC / DEVICE CREDENTIAL ON DEMAND")
                    RsAiDiagLineV191(
                        c,
                        "Avatar render backend",
                        if(sofiaRigged || marcusRigged)"RIGGED GLB + PROCEDURAL FALLBACK" else "REAL-TIME PROCEDURAL 3D"
                    )
                    RsAiDiagLineV191(c,"Sofia rigged GLB",if(sofiaRigged)"READY" else "PROCEDURAL FALLBACK")
                    RsAiDiagLineV191(c,"Marcus rigged GLB",if(marcusRigged)"READY" else "PROCEDURAL FALLBACK")
                    OutlinedButton(
                        onClick={
                            listenerRevision++
                            voiceWakeStatus=rsAiVoiceRuntimeTextV190(lang.code,"diagnostics_refreshed")
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text("REFRESH AI DIAGNOSTICS",fontSize=8.sp)}
                }
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


@Composable
private fun RsChatPermissionToggleV178(
    c:RsPalette,
    title:String,
    subtitle:String,
    checked:Boolean,
    onChange:(Boolean)->Unit
){
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
    ){
        Column(Modifier.weight(1f)){
            Text(title,color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
            Text(subtitle,color=c.muted,fontSize=8.sp,lineHeight=11.sp)
        }
        Switch(checked=checked,onCheckedChange=onChange)
    }
}


@Composable
private fun RsAiDiagLineV191(c:RsPalette,label:String,value:String){
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement=Arrangement.SpaceBetween,
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
    ){
        Text(label,color=c.muted,fontSize=8.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            color=c.text,
            fontSize=8.sp,
            fontWeight=FontWeight.Bold,
            maxLines=1,
            modifier=Modifier.weight(1f,false)
        )
    }
}

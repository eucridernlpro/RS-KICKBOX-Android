package com.rskickbox.app

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

private enum class RsPreLoginStageV147{LOGO,VIDEO,LOGIN}

@Composable
fun RsKickboxV21App(
    initialAuthDeepLink:String?=null,
    skipIntroOnRestore:Boolean=false,
    initialIncomingAction:String?=null,
    initialIncomingCallId:String?=null,
    initialIncomingCallType:String?=null,
    initialIncomingCallerId:String?=null,
    initialIncomingCallerName:String?=null,
    initialIncomingCallerEmail:String?=null
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTabletStartup = config.smallestScreenWidthDp>=600
    val store = remember { RsStore(context) }
    val incomingCallLaunch=remember(initialIncomingAction){
        initialIncomingAction=="com.rskickbox.app.INCOMING_CALL" ||
        initialIncomingAction=="com.rskickbox.app.INCOMING_VIDEO_ROOM"
    }
    val voiceAssistantLaunch=remember(initialIncomingAction){
        initialIncomingAction=="com.rskickbox.app.OPEN_AI_VOICE"
    }
    val initialRequestedRoute=remember(initialIncomingAction){
        if(initialIncomingAction=="com.rskickbox.app.OPEN_RS_ROUTE")
            (context as? android.app.Activity)?.intent?.getStringExtra("route").orEmpty()
        else ""
    }
    val initialIncomingDirectCall=remember(
        initialIncomingAction,
        initialIncomingCallId,
        initialIncomingCallType,
        initialIncomingCallerId,
        initialIncomingCallerName,
        initialIncomingCallerEmail
    ){
        if(
            initialIncomingAction=="com.rskickbox.app.INCOMING_CALL" &&
            !initialIncomingCallId.isNullOrBlank()
        ){
            RsCallV131(
                id=initialIncomingCallId,
                callerId=initialIncomingCallerId.orEmpty(),
                calleeId="",
                studentId="",
                callType=initialIncomingCallType.orEmpty().ifBlank{"AUDIO"},
                status="RINGING",
                peerId=initialIncomingCallerId.orEmpty(),
                peerName=initialIncomingCallerName.orEmpty(),
                peerEmail=initialIncomingCallerEmail.orEmpty(),
                createdAt=""
            )
        }else null
    }
    val backgroundCallRole=remember(incomingCallLaunch){
        if(
            incomingCallLaunch &&
            store.b("background_calls_enabled",true) &&
            rsSupabaseClientV60()?.auth?.currentUserOrNull()!=null
        ){
            when(store.s("background_call_role","")){
                "trainer"->RsRole.TRAINER
                "student"->RsRole.STUDENT
                else->null
            }
        }else null
    }
    val currentVersionCode=BuildConfig.VERSION_CODE
    val previousVersionCode=remember{store.s("rs_last_started_version_code","0").toIntOrNull()?:0}
    val localSessionAuthMs=remember{
        store.s("session_password_auth_ms","0").toLongOrNull()?:0L
    }
    val updatedSinceLastLaunch=remember(currentVersionCode,previousVersionCode,localSessionAuthMs){
        (previousVersionCode>0 && previousVersionCode!=currentVersionCode) ||
        // First rollout of version-aware sessions: an old authenticated session
        // with no version marker must be treated as an APK-update migration.
        (previousVersionCode==0 && localSessionAuthMs>0L)
    }
    val appScope = rememberCoroutineScope()
    val callNotificationPermissionLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){}
    val trustedSessionMaxAgeMs=72L*60L*60L*1000L
    val inactivityLogoutMs=24L*60L*60L*1000L
    val localSessionActivityMs=remember(localSessionAuthMs){
        store.s("session_last_activity_ms",localSessionAuthMs.toString()).toLongOrNull()
            ?:localSessionAuthMs
    }
    val localSessionFresh=remember(localSessionAuthMs,localSessionActivityMs,currentVersionCode){
        val age=System.currentTimeMillis()-localSessionAuthMs
        val inactivityAge=System.currentTimeMillis()-localSessionActivityMs
        localSessionAuthMs>0L &&
            age in 0..trustedSessionMaxAgeMs &&
            localSessionActivityMs>0L &&
            inactivityAge in 0..inactivityLogoutMs &&
            rsSupabaseClientV60()?.auth?.currentUserOrNull()!=null
    }
    val localSessionRole=remember(localSessionFresh){
        if(!localSessionFresh)null
        else when(store.s("session_role","")){
            "trainer"->RsRole.TRAINER
            "student"->RsRole.STUDENT
            else->null
        }
    }
    // Normal trusted-session restore happens only after crown/splash reaches LOGIN.
    // This prevents heavy authenticated systems from initializing during startup presentation.
    val externalTrustedLaunch=remember(
        incomingCallLaunch,voiceAssistantLaunch,initialRequestedRoute,localSessionFresh,localSessionRole
    ){
        localSessionFresh && localSessionRole!=null &&
            (incomingCallLaunch || voiceAssistantLaunch || initialRequestedRoute.isNotBlank())
    }
    var role by remember {
        mutableStateOf<RsRole?>(
            backgroundCallRole ?: if((skipIntroOnRestore || externalTrustedLaunch) && localSessionFresh) localSessionRole else null
        )
    }
    var callOnlyMode by remember { mutableStateOf(incomingCallLaunch && backgroundCallRole!=null) }
    var lastActivityWriteMs by remember{
        mutableLongStateOf(
            store.s("session_last_activity_ms",localSessionAuthMs.toString()).toLongOrNull()
                ?:localSessionAuthMs
        )
    }
    fun markSessionActivityV166(){
        if(role==null)return
        val now=System.currentTimeMillis()
        if(now-lastActivityWriteMs>=15_000L){
            lastActivityWriteMs=now
            store.ps("session_last_activity_ms",now.toString())
        }
    }


    val restoredRoute=remember(localSessionRole,backgroundCallRole,voiceAssistantLaunch,initialRequestedRoute){
        when{
            backgroundCallRole!=null->"coachchat"
            voiceAssistantLaunch && localSessionFresh && localSessionRole!=null->"voice"
            initialRequestedRoute.isNotBlank() && localSessionFresh && localSessionRole!=null->initialRequestedRoute
            else->{
                val saved=store.s("session_last_route","")
                if(localSessionRole==RsRole.TRAINER) saved.ifBlank{"trainer"} else saved.ifBlank{"home"}
            }
        }
    }
    var route by remember { mutableStateOf(restoredRoute) }
    var authRestoreAttempted by remember { mutableStateOf(false) }
    var authRestoring by remember { mutableStateOf(false) }
    var cloudControlsRevision by remember { mutableIntStateOf(0) }
    var brandRevision by remember { mutableIntStateOf(0) }
    // Cloud brand/visual sync runs in the background. Never block cold start with
    // a generic loading screen before the cinematic RS splash.
    var brandAssetsRestoring by remember { mutableStateOf(false) }
    var introPreparing by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(rsInitialLanguageV111(store)) }
    var theme by remember { mutableStateOf(runCatching { RsTheme.valueOf(store.s("theme", "ELITE_GOLD")) }.getOrDefault(RsTheme.ELITE_GOLD)) }
    LaunchedEffect(role){
        if(
            role!=null &&
            store.b("rs_voice_wake_enabled_v165",false) &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,android.Manifest.permission.RECORD_AUDIO
            )==android.content.pm.PackageManager.PERMISSION_GRANTED
        ){
            runCatching{RsVoiceWakeServiceV165.start(context)}
        }
    }

    var introDone by remember { mutableStateOf(true) }
    var preLoginStage by remember {
        mutableStateOf(
            if(skipIntroOnRestore || incomingCallLaunch || externalTrustedLaunch)RsPreLoginStageV147.LOGIN
            else RsPreLoginStageV147.LOGO
        )
    }
    var passwordRecoveryLaunch by remember(initialAuthDeepLink){
        mutableStateOf(initialAuthDeepLink?.startsWith("rskickbox://auth-callback",ignoreCase=true)==true)
    }
    fun finishPreLoginV156(){
        if(localSessionFresh && localSessionRole!=null && !passwordRecoveryLaunch){
            authRestoreAttempted=true
            authRestoring=false
            role=localSessionRole
            route=when{
                voiceAssistantLaunch->"voice"
                initialRequestedRoute.isNotBlank()->initialRequestedRoute
                else->if(localSessionRole==RsRole.TRAINER)"trainer" else "home"
            }
            preLoginStage=RsPreLoginStageV147.LOGIN
        }else{
            preLoginStage=RsPreLoginStageV147.LOGIN
        }
    }

    val c = paletteFor(theme)

    val localSplashUri=remember(preLoginStage,isTabletStartup){
        if(!store.b("intro_enabled",true))""
        else{
            val legacy=store.s("intro_video_uri","")
            val phone=store.s("intro_phone_video_uri",legacy)
            val tablet=store.s("intro_tablet_video_uri","")
            val candidate=if(isTabletStartup && tablet.isNotBlank())tablet else phone
            val parsed=runCatching{android.net.Uri.parse(candidate)}.getOrNull()
            when(parsed?.scheme){
                "file"->parsed.path?.let{path->java.io.File(path)}?.takeIf{it.exists()&&it.length()>1024L}?.let{candidate}.orEmpty()
                "content"->candidate
                else->""
            }
        }
    }

    LaunchedEffect(preLoginStage){
        if(preLoginStage==RsPreLoginStageV147.LOGO){
            delay(850L)
            if(localSplashUri.isNotBlank()){
                preLoginStage=RsPreLoginStageV147.VIDEO
            }else{
                finishPreLoginV156()
            }
        }
    }

    LaunchedEffect(currentVersionCode){
        if(updatedSinceLastLaunch){
            // Preserve a still-valid 72-hour trusted login across APK updates,
            // but reset the deep route so a changed screen cannot crash startup.
            if(!localSessionFresh)store.ps("session_last_route","")
            if(!localSessionFresh)route="home"
            authRestoring=false
            if(!localSessionFresh){
                store.ps("session_password_auth_ms","0")
                store.ps("session_last_activity_ms","0")
                store.ps("session_role","")
                role=null
            }
        }
        store.ps("rs_last_started_version_code",currentVersionCode.toString())
    }

    LaunchedEffect(introPreparing,isTabletStartup){
        if(introPreparing){
            rsPrepareIntroAssetV140(context,store,isTabletStartup)
            introPreparing=false
        }
    }

    LaunchedEffect(introDone,role){
        if(introDone && role!=null && !callOnlyMode && RsSupabaseV60.configured){
            rsSyncCloudBrandV100(store)
                .onSuccess{settings->
                    val overrideMs=store.s("theme_local_override_ms_v170","0").toLongOrNull()?:0L
                    val keepLocal=overrideMs>0L
                    if(!keepLocal){
                        theme=runCatching{RsTheme.valueOf(settings.themeName)}.getOrDefault(theme)
                    }
                    brandRevision++
                }
            rsSyncCloudVisualAssetsV101(context,store)
                .onSuccess{brandRevision++}
            brandAssetsRestoring=false
        }else if(introDone){
            brandAssetsRestoring=false
        }
    }

    LaunchedEffect(authRestoreAttempted,preLoginStage,localSessionFresh,localSessionRole){
        if(!authRestoreAttempted && preLoginStage==RsPreLoginStageV147.LOGIN){
            authRestoreAttempted=true
            if(callOnlyMode && backgroundCallRole!=null){
                role=backgroundCallRole
                route="coachchat"
                authRestoring=false
            }else if(localSessionFresh && localSessionRole!=null){
                // Trusted login: no password prompt for up to 72 hours.
                role=localSessionRole
                route=if(localSessionRole==RsRole.TRAINER)"trainer" else "home"
                authRestoring=false
            }else{
                role=null
                route="home"
                authRestoring=false
                store.ps("session_password_auth_ms","0")
                store.ps("session_last_route","")
                store.ps("session_role","")
            }
            store.ps("rs_last_started_version_code",currentVersionCode.toString())
        }
    }

    LaunchedEffect(role,introDone){
        val backgroundCallIdentityAvailable=
            store.b("background_calls_enabled",true) &&
            !store.s("background_call_role","").isBlank() &&
            rsSupabaseClientV60()?.auth?.currentUserOrNull()!=null

        if(introDone && RsSupabaseV60.configured && (role!=null || backgroundCallIdentityAvailable)){
            RsCallMonitorServiceV134.start(context)
            if(
                android.os.Build.VERSION.SDK_INT>=33 &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )!=android.content.pm.PackageManager.PERMISSION_GRANTED
            ){
                callNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }else{
            runCatching{RsCallMonitorServiceV134.stop(context)}
        }
    }

    LaunchedEffect(role,introDone){
        if(introDone && role!=null && !callOnlyMode && RsSupabaseV60.configured){
            rsSyncCloudControlsV82(store)
                .onSuccess{
                    cloudControlsRevision++
                    if(role==RsRole.STUDENT && !rsStudentRouteEnabledV82(store,route))route="home"
                }
        }
    }

    LaunchedEffect(role,route){
        if(role!=null){
            store.ps("session_last_route",route)
            store.ps("ai_current_route_v177",route)
            store.ps("ai_current_role_v177",role!!.name)
            val homeRoute=if(role==RsRole.TRAINER)"trainer" else "home"
            if(route!=homeRoute && route !in setOf("voice","coachchat","groups","community","support","notifications","content")){
                store.ps("last_feature_route_v177",route)
            }
            markSessionActivityV166()
        }
    }

    LaunchedEffect(role,route,cloudControlsRevision){
        if(role==RsRole.STUDENT && !rsStudentRouteEnabledV82(store,route)){
            route="home"
        }
    }

    LaunchedEffect(introDone,role){
        if(introDone && role!=null && RsSupabaseV60.configured){
            while(true){
                rsSyncChatPermissionsV178(store)
                delay(90_000L)
            }
        }
    }

    LaunchedEffect(introDone,role){
        if(introDone && role!=null && RsSupabaseV60.configured){
            while(true){
                kotlinx.coroutines.delay(60_000)
                rsSyncCloudBrandV100(store).onSuccess{settings->
                    val overrideMs=store.s("theme_local_override_ms_v170","0").toLongOrNull()?:0L
                    val localTheme=store.s("theme",theme.name)
                    val keepLocal=overrideMs>0L
                    if(keepLocal && settings.themeName==localTheme){
                        store.ps("theme_local_override_ms_v170","0")
                        store.ps("theme_cloud_confirmed_v176",localTheme)
                    }else if(!keepLocal){
                        theme=runCatching{RsTheme.valueOf(settings.themeName)}.getOrDefault(theme)
                    }
                    brandRevision++
                }
                rsSyncCloudVisualAssetsV101(context,store).onSuccess{brandRevision++}
            }
        }
    }

    LaunchedEffect(role,introDone){
        if(introDone && role!=null && RsSupabaseV60.configured){
            while(true){
                rsTouchPresenceV125()
                delay(45_000)
            }
        }
    }

    LaunchedEffect(role){
        if(role!=null){
            while(true){
                delay(60_000L)
                val authMs=store.s("session_password_auth_ms","0").toLongOrNull()?:0L
                val activityMs=store.s("session_last_activity_ms",authMs.toString()).toLongOrNull()?:authMs
                val now=System.currentTimeMillis()
                val authExpired=authMs<=0L || now-authMs>trustedSessionMaxAgeMs
                val inactive=activityMs<=0L || now-activityMs>inactivityLogoutMs
                if(authExpired || inactive){
                    runCatching{RsVoiceWakeServiceV165.stop(context)}
                    runCatching{RsCallMonitorServiceV134.stop(context)}
                    store.ps("session_password_auth_ms","0")
                    store.ps("session_last_activity_ms","0")
                    store.ps("session_last_route","")
                    store.ps("session_role","")
                    store.pb("rs_voice_wake_enabled_v165",false)
                    rsCloudLogoutV63()
                    role=null
                    route="home"
                    preLoginStage=RsPreLoginStageV147.LOGIN
                    break
                }
            }
        }
    }

    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner,role,introDone){
        val observer=LifecycleEventObserver{_,event->
            if(event==Lifecycle.Event.ON_RESUME && introDone && role!=null){
                markSessionActivityV166()
                // Foreground hands-free is the default: when RS is visible and the
                // microphone permission exists, keep the single Voice Wake service
                // listening for natural commands without requiring the top mic.
                val micGranted=androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                )==android.content.pm.PackageManager.PERMISSION_GRANTED
                if(
                    store.b("rs_ai_listening_enabled_v200",true) &&
                    store.b("rs_voice_handsfree_foreground_v199",true) &&
                    micGranted
                ){
                    store.pb("rs_voice_wake_paused_for_ai_v197",false)
                    runCatching{RsVoiceWakeServiceV165.start(context)}
                }
                // Resume must be lightweight and must never rewrite visual files while
                // Compose/VideoView/Media3 surfaces are being restored.
                if(RsSupabaseV60.configured){
                    appScope.launch{runCatching{rsTouchPresenceV125()}}
                }
            }else if(event==Lifecycle.Event.ON_STOP && role!=null){
                // If background wake is not enabled, release the foreground-only
                // listener when RS leaves the screen. If enabled, keep it alive.
                if(!store.b("rs_voice_wake_enabled_v165",false)){
                    runCatching{RsVoiceWakeServiceV165.stop(context)}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}
    }

    val appThemeLayout=rsThemeLayoutV175(theme)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary=c.bright,
            secondary=c.gold,
            background=c.bg,
            surface=c.panel,
            onBackground=c.text,
            onSurface=c.text
        ),
        shapes=Shapes(
            extraSmall=RoundedCornerShape((appThemeLayout.buttonRadius/2).coerceAtLeast(4).dp),
            small=RoundedCornerShape(appThemeLayout.buttonRadius.dp),
            medium=RoundedCornerShape(appThemeLayout.panelRadius.dp),
            large=RoundedCornerShape(appThemeLayout.tileRadius.dp),
            extraLarge=RoundedCornerShape((appThemeLayout.tileRadius+6).dp)
        )
    ) {
        val currentBrandRevision=brandRevision
        Box(
            Modifier.fillMaxSize().pointerInput(role){
                if(role!=null){
                    awaitPointerEventScope{
                        while(true){
                            awaitPointerEvent()
                            markSessionActivityV166()
                        }
                    }
                }
            }
        ) {
            when {
                brandAssetsRestoring -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
                        CircularProgressIndicator()
                        Text("RS KICKBOXING",color=c.bright,fontWeight=FontWeight.Black)
                    }
                }
                authRestoring -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
                        CircularProgressIndicator()
                        Text(rsCommonT95(lang,"restoring_session"),color=c.text)
                    }
                }
                role == null -> when(preLoginStage){
                    RsPreLoginStageV147.LOGO -> Box(
                        Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment=Alignment.Center
                    ){
                        Column(
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(16.dp)
                        ){
                            androidx.compose.foundation.Image(
                                painter=androidx.compose.ui.res.painterResource(R.drawable.rs_launcher_royal_v129),
                                contentDescription="RS KICKBOXING",
                                modifier=Modifier.size(148.dp)
                            )
                            Text("RS KICKBOXING",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp,letterSpacing=1.5.sp)
                        }
                    }
                    RsPreLoginStageV147.VIDEO -> Box(Modifier.fillMaxSize().background(Color.Black)){
                        RsSafeIntroVideoWithProgressV147(
                            uri=localSplashUri,
                            sound=store.b("intro_video_sound",true),
                            showProgress=true,
                            onStarted={},
                            onFinished={finishPreLoginV156()}
                        )
                        if(store.b("intro_skip_enabled",true)){
                            TextButton(
                                onClick={finishPreLoginV156()},
                                modifier=Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                            ){Text("Skip",color=Color.White.copy(alpha=.82f))}
                        }
                    }
                    RsPreLoginStageV147.LOGIN -> RsLiveBackground(c, store, BgScope.LOGIN) {
                        RsPerPageBackgroundV21(store, "login") {
                            LoginV21(c, store, lang, passwordRecoveryLaunch, { selected -> store.pb("lang_manual_override_v111",true);lang=selected;store.ps("lang",selected.code) }) { selected ->
                                passwordRecoveryLaunch=false
                                callOnlyMode=false
                                role = selected
                                route = if(selected==RsRole.TRAINER) "trainer" else "home"
                            }
                        }
                    }
                }
                else -> {
                    val active = role!!
                    val scope = if(active==RsRole.TRAINER) BgScope.TRAINER_TRAINING else BgScope.STUDENT_TRAINING
                    RsLiveBackground(c, store, scope) {
                        RsPerPageBackgroundV21(store, route) {
                            val chatRoute=route in setOf(
                                "coachchat","groups","voice","media",
                                "community","support","notifications","content"
                            )
                            if(chatRoute){
                                RsFloatingGlassChatHubV125(
                                    c=c,
                                    store=store,
                                    lang=lang,
                                    role=active,
                                    initialRoute=route,
                                    onNavigate={target->
                                        store.pb("ai_immersive_v171",false)
                                        route=target
                                    },
                                    onBack={
                                        // Tear down the chat composition first, then return to
                                        // dashboard on the next frame. This avoids a race between
                                        // chat/call media cleanup and the dashboard background.
                                        appScope.launch{
                                            kotlinx.coroutines.delay(80)
                                            route=if(active==RsRole.TRAINER)"trainer" else "home"
                                        }
                                    }
                                )
                            }else ShellV21(c, store, active, lang, route, { selected -> store.pb("lang_manual_override_v111",true);lang=selected;store.ps("lang",selected.code) }, { route=it }, {
                                passwordRecoveryLaunch=false
                                role=null
                                route="home"
                                preLoginStage=RsPreLoginStageV147.LOGIN
                                store.ps("session_last_route","")
                                store.ps("session_password_auth_ms","0")
                                // Lock the application UI but retain the authenticated
                                // device identity so incoming calls can still be routed.
                                // Full account sign-out remains available from privacy/settings.
                                store.pb("background_calls_enabled",true)
                                RsCallMonitorServiceV134.start(context)
                            }) {
                                when(route) {
                                    "home", "trainer" -> RsPremiumDashboardV21(c, store, active, lang) { route=it }
                                    "owner_command" -> RsOwnerCommandCenterPageV179(c,store,lang){route=it}
                                    "themes" -> RsThemeStudio(c, theme) { selected ->
                                        val now=System.currentTimeMillis()
                                        theme=selected
                                        store.ps("theme",selected.name)
                                        store.ps("theme_local_override_ms_v170",now.toString())
                                        brandRevision++
                                        if(RsSupabaseV60.configured){
                                            appScope.launch{
                                                rsSaveCloudBrandV100(
                                                    store.s("brand_header_name","RS KICKBOXING"),
                                                    store.s("brand_login_title","Premium cinematic kickboxing"),
                                                    store.s("brand_login_subtitle","TRAIN · LEARN · CONNECT · GROW"),
                                                    store.s("brand_footer_text","RS KICKBOXING · TRAIN · LEARN · CONNECT · GROW"),
                                                    selected.name,
                                                    store.s("login_form_opacity","0.82").toFloatOrNull()?:.82f
                                                ).onSuccess{
                                                    // Keep the confirmed local theme protected.
                                                    // Cloud sync may confirm the same value later, but
                                                    // it must never undo a theme the user just applied.
                                                    store.ps("theme",selected.name)
                                                    store.ps("theme_cloud_confirmed_v176",selected.name)
                                                }
                                            }
                                        }
                                    }
                                    "backgrounds" -> RsVisualAssetStudioV21(c, store, lang)
                                    "branding" -> RsBrandSiteSettingsV21(c, store, lang)
                                    "intro_settings" -> RsIntroSettingsV21(c, store, lang)
                                    "voice" -> RsFloatingGlassChatHubV125(c,store,lang,active,"voice"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "session" -> if(active==RsRole.TRAINER) RsSessionBuilderV52(c,store,lang) else RsSessionPlayerV52(c,store,lang)
                                    "access" -> RsAccessControlV49(c,store,lang)
                                    "payments" -> RsTrainerPaymentCenterV39(c,store,lang)
                                    "members" -> RsMemberManager(c,store,lang)
                                    "classes" -> if(active==RsRole.TRAINER) RsClassManagerV38(c,store,lang) else RsStudentClassesV38(c,store,lang)
                                    "attendance" -> RsAttendanceCenterV89(c,store,lang)
                                    "checkin" -> RsStudentCheckInV52(c,store,lang)
                                    "invoices" -> RsTrainerInvoicesV39(c,store,lang)
                                    "book" -> if(active==RsRole.TRAINER) RsBookManagerV45(c,store,lang) else RsBookLibraryV45(c,store,lang)
                                    "settings" -> if(active==RsRole.TRAINER) RsAdminSettingsV56(c,store,lang){ selected -> lang=selected } else RsStudentPrivacyV40(c,store,lang,{ selected -> lang=selected }){
                                        role=null
                                        route="home"
                                    }
                                    "academy" -> RsAcademyV54(c,store,lang)
                                    "progress" -> RsStudentProgressV46(c,store,lang)
                                    "challenges" -> RsStudentChallengesV47(c,store,lang)
                                    "fightcamp" -> RsStudentFightCampV47(c,store,lang)
                                    "finance" -> RsStudentFinanceV39(c,store,lang)
                                    "community" -> RsFloatingGlassChatHubV125(c,store,lang,active,"community"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "media" -> RsFloatingGlassChatHubV125(c,store,lang,active,"gallery"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "music", "music_admin" -> RsPersistentMusicCenterV90(c, store, active, lang)
                                    "techniques" -> RsTechniqueLibraryV54(c,lang)
                                    "home_training" -> RsWorkoutHomeHubV89(c,store,lang)
                                    "workout" -> RsWorkoutHomeHubV89(c,store,lang)
                                    "badges" -> RsBadgesV47(c,store,lang)
                                    "vault" -> RsKnowledgeHubV89(c,store,lang)
                                    "compare" -> RsTechniqueCompareV54(c,lang)
                                    "coachchat" -> RsFloatingGlassChatHubV125(c,store,lang,active,"coachchat"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "events" -> RsStudentEventsV42(c,store,lang)
                                    "promotions" -> RsPromotionPageV45(c,store,lang){route="book"}
                                    "notifications" -> RsFloatingGlassChatHubV125(c,store,lang,active,"notifications"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "analytics" -> RsAnalyticsV53(c,store,lang)
                                    "guide" -> RsTrainerGuideV54(c,store,lang){route=it}
                                    "student_guide" -> RsStudentGuideV81(c,store,lang){route=it}
                                    "documents" -> RsDocumentsV51(c,store,lang,active)
                                    "support" -> RsFloatingGlassChatHubV125(c,store,lang,active,"support"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "referrals" -> RsReferralsV51(c,store,lang)
                                    "schedule" -> RsTrainerScheduleV43(c,store,lang)
                                    "content" -> RsFloatingGlassChatHubV125(c,store,lang,active,"content"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "notes" -> RsCoachNotesV46(c,store,lang)
                                    "homework" -> RsStudentHomeworkV46(c,store,lang)
                                    "favorites" -> RsFavoritesV48(c,store,lang)
                                    "history" -> RsHistoryV48(c,store,lang)
                                    "private_lessons" -> RsStudentPrivateLessonsV43(c,store,lang)
                                    "profile" -> RsProfileV50(c,store,lang)
                                    "groups" -> RsFloatingGlassChatHubV125(c,store,lang,active,"groups"){appScope.launch{kotlinx.coroutines.delay(80);route=if(active==RsRole.TRAINER)"trainer" else "home"}}
                                    "search" -> RsSearchV48(c,store,lang)
                                    "homework_admin" -> RsHomeworkManagerV46(c,store,lang)
                                    "lesson_editor" -> RsContentManagerV48(c,store,lang)
                                    "plans_admin" -> RsMembershipPlansV49(c,store,lang)
                                    "progress_admin" -> RsProgressManagerV53(c,store,lang)
                                    "assessments" -> RsAssessmentsV46(c,store,lang)
                                    "events_admin" -> RsEventManagerV42(c,store,lang)
                                    "qr_attendance" -> RsAttendanceCenterV89(c,store,lang)
                                    "session_builder" -> RsSessionBuilderV52(c,store,lang)
                                    "challenge_admin" -> RsChallengeManagerV47(c,store,lang)
                                    "fightcamp_admin" -> RsFightCampManagerV47(c,store,lang)
                                    "landing_admin" -> RsPromotionManagerV45(c,store,lang)
                                    "release" -> RsReleaseCenterV16(c, store, lang)
                                    "privacy_admin" -> RsPrivacyRequestsAdminV107(c,lang)
                                    else -> RsScroll(c, rsRouteTitle(lang,route,route.replaceFirstChar { it.uppercase() }), rsCommonT95(lang,"module_ready_sub")) {
                                        RsPanel(c) {
                                            Text(rsCommonT95(lang,"module_ready"),color=c.bright,fontWeight=FontWeight.Bold)
                                            Text(rsCommonT95(lang,"module_ready_sub"),color=c.muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(role!=null && introDone && !authRestoring){
                val finishCallOnlySession={
                    if(callOnlyMode){
                        callOnlyMode=false
                        role=null
                        route="home"
                        preLoginStage=RsPreLoginStageV147.LOGIN
                        store.ps("session_password_auth_ms","0")
                        store.ps("session_last_activity_ms","0")
                        store.ps("session_role","")
                        if(store.b("background_calls_enabled",true)){
                            RsCallMonitorServiceV134.start(context)
                        }
                    }
                }
                RsGlobalCallHostV134(
                    c,lang,role!!,
                    initialIncomingCall=initialIncomingDirectCall,
                    onCallSessionFinished=if(callOnlyMode)finishCallOnlySession else null
                )
                RsGlobalVideoRoomHostV137(
                    c,lang,role!!,
                    onRoomSessionFinished=if(callOnlyMode)finishCallOnlySession else null
                )
            }

        }
        @Suppress("UNUSED_VARIABLE") val keepBrandRevisionObserved=currentBrandRevision
    }
}

@Composable
private fun LoginV21(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    forcePasswordRecovery:Boolean=false,
    onLang:(RsLang)->Unit,
    onLogin:(RsRole)->Unit
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val rememberedLoginEmail=store.s("last_login_email","").trim()
    val biometricLoginAvailable=remember(rememberedLoginEmail){
        rememberedLoginEmail.isNotBlank() && MainActivity.biometricLoginAvailable()
    }
    var email by remember { mutableStateOf(rememberedLoginEmail) }
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var pendingInviteToken by remember { mutableStateOf("") }
    var forgotMode by remember { mutableStateOf(false) }
    var recoveryMode by remember(forcePasswordRecovery) { mutableStateOf(forcePasswordRecovery) }
    val formOpacity=store.s("login_form_opacity","0.82").toFloatOrNull()?.coerceIn(.20f,1f)?:.82f

    fun clearLoginFields(clearEmail:Boolean=true){
        if(clearEmail)email=""
        pass=""
        confirmPass=""
        showPassword=false
    }

    fun finishCloudLogin(session:RsCloudSessionV63){
        store.ps("session_student_email",session.email)
        store.ps("last_login_email",session.email)
        if(MainActivity.biometricLoginAvailable()){
            store.pb("biometric_login_enabled_v198",true)
        }
        store.ps("session_student_name",session.displayName)
        store.ps("session_plan",session.plan)
        val roleName=if(session.role==RsRole.TRAINER)"trainer" else "student"
        store.ps("session_role",roleName)
        store.ps("background_call_role",roleName)
        store.pb("background_calls_enabled",true)
        val loginNow=System.currentTimeMillis()
        store.ps("session_password_auth_ms",loginNow.toString())
        store.ps("session_last_activity_ms",loginNow.toString())
        rsRefreshAndRegisterFcmTokenV155()
        statusIsError=false
        status="✓ "+rsEnrollMsg(lang,"welcome",name=session.displayName)
        onLogin(session.role)
    }

    fun signInCloud(){
        if(busy)return
        if(email.isBlank()){
            statusIsError=true
            status=rsLoginT94(lang,"enter_email")
            return
        }
        if(pass.isBlank()){
            statusIsError=true
            status=rsLoginT94(lang,"enter_password")
            return
        }
        val attemptEmail=email
        val attemptPass=pass
        busy=true
        statusIsError=false
        status=rsLoginT94(lang,"checking")
        scope.launch{
            rsCloudLoginV63(attemptEmail,attemptPass)
                .onSuccess{finishCloudLogin(it)}
                .onFailure{
                    statusIsError=true
                    status=rsLoginT94(lang,"login_failed")
                    clearLoginFields(clearEmail=false)
                }
            busy=false
        }
    }

    fun signInBiometric(){
        if(busy || !biometricLoginAvailable)return
        busy=true
        statusIsError=false
        status=when(lang.code){
            "nl"->"Wachten op vingerafdruk…"
            "pt"->"A aguardar impressão digital…"
            "es"->"Esperando huella…"
            "fr"->"En attente de l’empreinte…"
            "de"->"Warte auf Fingerabdruck…"
            "it"->"In attesa dell’impronta…"
            "pl"->"Oczekiwanie na odcisk palca…"
            "tr"->"Parmak izi bekleniyor…"
            else->"Waiting for fingerprint…"
        }
        val launched=MainActivity.requestPersistentBiometricLogin(
            onSuccess={
                scope.launch{
                    rsCloudCurrentSessionV67()
                        .onSuccess{session->
                            if(session!=null){
                                finishCloudLogin(session)
                            }else{
                                busy=false
                                statusIsError=true
                                status=when(lang.code){
                                    "nl"->"Je beveiligde sessie is verlopen. Log één keer in met je wachtwoord; vingerafdruk blijft daarna beschikbaar."
                                    "pt"->"A sessão segura expirou. Entra uma vez com a palavra-passe; a impressão digital continuará disponível."
                                    "es"->"La sesión segura ha caducado. Inicia sesión una vez con tu contraseña; la huella seguirá disponible."
                                    "fr"->"La session sécurisée a expiré. Connecte-toi une fois avec ton mot de passe ; l’empreinte restera disponible."
                                    "de"->"Die sichere Sitzung ist abgelaufen. Melde dich einmal mit dem Passwort an; Fingerabdruck bleibt danach verfügbar."
                                    "it"->"La sessione sicura è scaduta. Accedi una volta con la password; l’impronta resterà disponibile."
                                    "pl"->"Bezpieczna sesja wygasła. Zaloguj się raz hasłem; odcisk palca pozostanie dostępny."
                                    "tr"->"Güvenli oturumun süresi doldu. Bir kez şifreyle giriş yap; parmak izi seçeneği kullanılmaya devam eder."
                                    else->"Your secure session expired. Sign in with your password once; fingerprint will remain available afterward."
                                }
                            }
                        }
                        .onFailure{
                            busy=false
                            statusIsError=true
                            status=when(lang.code){
                                "nl"->"Biometrisch inloggen kon je beveiligde RS-sessie niet herstellen. Gebruik je wachtwoord één keer."
                                "pt"->"O login biométrico não conseguiu restaurar a sessão RS. Usa a palavra-passe uma vez."
                                "es"->"El inicio biométrico no pudo restaurar la sesión RS. Usa tu contraseña una vez."
                                "fr"->"La connexion biométrique n’a pas pu restaurer la session RS. Utilise ton mot de passe une fois."
                                "de"->"Die biometrische Anmeldung konnte die RS-Sitzung nicht wiederherstellen. Verwende einmal dein Passwort."
                                else->"Biometric login could not restore your RS session. Use your password once."
                            }
                        }
                }
            },
            onError={message->
                busy=false
                statusIsError=true
                status=message
            }
        )
        if(!launched){
            busy=false
            statusIsError=true
            status=when(lang.code){
                "nl"->"Biometrisch inloggen is momenteel niet beschikbaar."
                "pt"->"O login biométrico não está disponível neste momento."
                "es"->"El inicio biométrico no está disponible ahora."
                "fr"->"La connexion biométrique n’est pas disponible pour le moment."
                "de"->"Biometrische Anmeldung ist derzeit nicht verfügbar."
                else->"Biometric login is not available right now."
            }
        }
    }

    fun requestPasswordReset(){
        if(busy)return
        if(email.isBlank()){
            statusIsError=true
            status=rsLoginT94(lang,"reset_email_first")
            return
        }
        val resetEmail=email.trim()
        busy=true
        statusIsError=false
        status=rsLoginT94(lang,"sending_reset")
        scope.launch{
            rsCloudRequestPasswordResetV87(resetEmail)
                .onSuccess{
                    status=rsLoginT94(lang,"reset_sent")
                    pass=""
                }
                .onFailure{
                    statusIsError=true
                    status=rsLoginT94(lang,"reset_failed")
                }
            busy=false
        }
    }

    fun updateRecoveredPassword(){
        if(busy)return
        if(pass.length<10){
            statusIsError=true
            status=rsLoginT94(lang,"new_password_min")
            return
        }
        if(pass!=confirmPass){
            statusIsError=true
            status=rsLoginT94(lang,"passwords_no_match")
            confirmPass=""
            return
        }
        val next=pass
        busy=true
        statusIsError=false
        status=rsLoginT94(lang,"updating_password")
        scope.launch{
            rsCloudUpdatePasswordV87(next)
                .onSuccess{
                    recoveryMode=false
                    forgotMode=false
                    clearLoginFields(clearEmail=true)
                    statusIsError=false
                    status=rsLoginT94(lang,"password_updated")
                }
                .onFailure{
                    statusIsError=true
                    status=rsLoginT94(lang,"password_update_failed")
                    pass=""
                    confirmPass=""
                }
            busy=false
        }
    }

    fun activateInvite(){
        if(busy)return
        if(pass.length<10){
            statusIsError=true
            status=rsLoginT94(lang,"new_password_min")
            return
        }
        busy=true
        statusIsError=false
        status=rsLoginT94(lang,"activating")
        scope.launch{
            rsRedeemStudentInviteV63(email,pendingInviteToken,pass)
                .onSuccess{
                    rsCloudLoginV63(email,pass)
                        .onSuccess{
                            pendingInviteToken=""
                            finishCloudLogin(it)
                        }
                        .onFailure{
                            statusIsError=true
                            status=rsLoginT94(lang,"account_signin_failed")
                            clearLoginFields(clearEmail=true)
                        }
                }
                .onFailure{
                    statusIsError=true
                    status=rsLoginT94(lang,"activate_failed")
                    pass=""
                }
            busy=false
        }
    }

    fun applyInvite(raw:String){
        val invite=rsParseInviteV33(raw)
        if(invite==null){
            statusIsError=true
            status=rsEnrollMsg(lang,"invalid_qr")
        }else{
            forgotMode=false
            recoveryMode=false
            email=invite.email
            pendingInviteToken=invite.activationCode
            pass=""
            statusIsError=false
            status=rsLoginT94(lang,"invite_loaded")
        }
    }

    val galleryQrPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            val raw=rsDecodeQrImageV33(context,uri)
            if(raw==null)status=rsEnrollMsg(lang,"qr_unreadable")
            else applyInvite(raw)
        }
    }

    val scannerOptions=remember{
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }
    val scanner=remember{GmsBarcodeScanning.getClient(context,scannerOptions)}

    val fieldColors=OutlinedTextFieldDefaults.colors(
        focusedTextColor=Color.White,
        unfocusedTextColor=Color.White,
        focusedLabelColor=Color.White,
        unfocusedLabelColor=Color.White.copy(alpha=.78f),
        cursorColor=c.bright,
        focusedBorderColor=c.bright,
        unfocusedBorderColor=Color.White.copy(alpha=.55f)
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)
        ){
            Image(
                painter=androidx.compose.ui.res.painterResource(R.drawable.rs_launcher_royal_v129),
                contentDescription="RS",
                modifier=Modifier.size(52.dp)
            )
            RsLettersLogoV111(c,store,Modifier.weight(1f))
        }
        Text(store.s("brand_login_title","Premium cinematic kickboxing"),color=Color.White,style=MaterialTheme.typography.headlineMedium)
        Text(store.s("brand_login_subtitle","TRAIN · LEARN · CONNECT · GROW"),color=Color.White.copy(alpha=.78f),fontSize=11.sp)

        Surface(
            shape=RoundedCornerShape(24.dp),
            color=c.panel.copy(alpha=formOpacity),
            border=androidx.compose.foundation.BorderStroke(1.dp,c.gold.copy(alpha=.55f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text(
                    when{
                        recoveryMode->rsLoginT94(lang,"recovery_title")
                        forgotMode->rsLoginT94(lang,"forgot_title")
                        else->rsT(lang,"member_access")
                    },
                    color=Color.White,
                    fontWeight=FontWeight.Bold
                )

                if(!forgotMode&&!recoveryMode){
                    Text(rsEnrollmentT(lang,"invite_title"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=12.sp)
                    Text(rsEnrollmentT(lang,"invite_desc"),color=Color.White.copy(alpha=.74f),fontSize=10.sp)

                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        Button(
                            onClick={
                                scanner.startScan()
                                    .addOnSuccessListener{barcode->
                                        val raw=barcode.rawValue
                                        if(raw.isNullOrBlank())status=rsEnrollMsg(lang,"scan_empty")
                                        else applyInvite(raw)
                                    }
                                    .addOnCanceledListener{status=rsEnrollMsg(lang,"scan_cancelled")}
                                    .addOnFailureListener{status=rsEnrollMsg(lang,"scanner_error",detail=it.message?:"unknown error")}
                            },
                            enabled=!busy,
                            modifier=Modifier.weight(1f)
                        ){Text(rsEnrollmentT(lang,"scan_qr"),fontSize=11.sp)}
                        OutlinedButton(
                            onClick={galleryQrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                            enabled=!busy,
                            modifier=Modifier.weight(1f)
                        ){Text(rsEnrollmentT(lang,"upload_qr"),fontSize=11.sp)}
                    }
                }else if(forgotMode){
                    Text(
                        rsLoginT94(lang,"forgot_desc"),
                        color=Color.White.copy(alpha=.78f),
                        fontSize=11.sp
                    )
                }else{
                    Text(
                        rsLoginT94(lang,"recovery_desc"),
                        color=Color.White.copy(alpha=.78f),
                        fontSize=11.sp
                    )
                }

                if(!recoveryMode){
                    OutlinedTextField(
                        email,
                        {
                            email=it
                            if(pendingInviteToken.isNotBlank())pendingInviteToken=""
                            if(statusIsError)status=""
                        },
                        label={Text(rsT(lang,"email"))},
                        colors=fieldColors,
                        singleLine=true,
                        enabled=!busy,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=ImeAction.Next),
                        modifier=Modifier.fillMaxWidth().semantics{contentType=ContentType.Username}
                    )
                }

                if(!forgotMode){
                    OutlinedTextField(
                        pass,
                        {
                            pass=it
                            if(statusIsError)status=""
                        },
                        label={Text(
                            when{
                                recoveryMode->rsLoginT94(lang,"new_password")
                                pendingInviteToken.isNotBlank()->rsLoginT94(lang,"create_password")
                                else->rsT(lang,"password")
                            }
                        )},
                        colors=fieldColors,
                        singleLine=true,
                        enabled=!busy,
                        visualTransformation=if(showPassword)VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),
                        trailingIcon={
                            TextButton(
                                onClick={showPassword=!showPassword},
                                enabled=!busy,
                                contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)
                            ){
                                Text(if(showPassword)rsCommonT95(lang,"hide") else "👁",color=Color.White,fontSize=13.sp)
                            }
                        },
                        modifier=Modifier.fillMaxWidth().semantics{contentType=ContentType.Password}
                    )
                }

                if(recoveryMode){
                    OutlinedTextField(
                        confirmPass,
                        {
                            confirmPass=it
                            if(statusIsError)status=""
                        },
                        label={Text(rsLoginT94(lang,"confirm_password"))},
                        colors=fieldColors,
                        singleLine=true,
                        enabled=!busy,
                        visualTransformation=if(showPassword)VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),
                        modifier=Modifier.fillMaxWidth().semantics{contentType=ContentType.NewPassword}
                    )
                }

                Button(
                    onClick={
                        when{
                            recoveryMode->updateRecoveredPassword()
                            forgotMode->requestPasswordReset()
                            pendingInviteToken.isNotBlank()->activateInvite()
                            else->signInCloud()
                        }
                    },
                    enabled=!busy && when{
                        recoveryMode->pass.isNotBlank()&&confirmPass.isNotBlank()
                        forgotMode->email.isNotBlank()
                        else->email.isNotBlank()&&pass.isNotBlank()
                    },
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(
                        when{
                            busy->rsLoginT94(lang,"wait")
                            recoveryMode->rsLoginT94(lang,"save_new_password")
                            forgotMode->rsLoginT94(lang,"send_reset")
                            pendingInviteToken.isNotBlank()->rsLoginT94(lang,"activate_account")
                            else->rsLoginT94(lang,"sign_in")
                        },
                        fontSize=12.sp,
                        maxLines=1
                    )
                }

                if(!forgotMode && !recoveryMode && pendingInviteToken.isBlank() && biometricLoginAvailable){
                    OutlinedButton(
                        onClick={signInBiometric()},
                        enabled=!busy,
                        modifier=Modifier.fillMaxWidth(),
                        border=androidx.compose.foundation.BorderStroke(1.dp,c.bright.copy(alpha=.70f))
                    ){
                        Text(
                            when(lang.code){
                                "nl"->"☝ INLOGGEN MET VINGERAFDRUK"
                                "pt"->"☝ ENTRAR COM IMPRESSÃO DIGITAL"
                                "es"->"☝ ENTRAR CON HUELLA"
                                "fr"->"☝ CONNEXION PAR EMPREINTE"
                                "de"->"☝ MIT FINGERABDRUCK ANMELDEN"
                                "it"->"☝ ACCEDI CON IMPRONTA"
                                "pl"->"☝ ZALOGUJ ODCISKIEM PALCA"
                                "tr"->"☝ PARMAK İZİYLE GİRİŞ"
                                else->"☝ SIGN IN WITH FINGERPRINT"
                            },
                            fontSize=11.sp,
                            fontWeight=FontWeight.Black
                        )
                    }
                    Text(
                        when(lang.code){
                            "nl"->"Beschikbaar voor het onthouden account: "+rememberedLoginEmail
                            "pt"->"Disponível para a conta guardada: "+rememberedLoginEmail
                            "es"->"Disponible para la cuenta guardada: "+rememberedLoginEmail
                            "fr"->"Disponible pour le compte mémorisé : "+rememberedLoginEmail
                            "de"->"Verfügbar für das gespeicherte Konto: "+rememberedLoginEmail
                            else->"Available for remembered account: "+rememberedLoginEmail
                        },
                        color=c.muted,
                        fontSize=9.sp
                    )
                }

                if(!recoveryMode){
                    OutlinedButton(
                        onClick={
                            forgotMode=!forgotMode
                            pendingInviteToken=""
                            pass=""
                            status=""
                            statusIsError=false
                        },
                        enabled=!busy,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(if(forgotMode)rsLoginT94(lang,"back_signin") else rsLoginT94(lang,"forgot_button"))
                    }
                }

                OutlinedButton(
                    onClick={
                        if(forgotMode||recoveryMode){
                            forgotMode=false
                            recoveryMode=false
                            clearLoginFields(clearEmail=true)
                            status=""
                            statusIsError=false
                        }else{
                            clearLoginFields(clearEmail=true)
                            pendingInviteToken=""
                            status=""
                            statusIsError=false
                        }
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                 ){Text(rsLoginT94(lang,"reset_screen"))}

                if(status.isNotBlank()){
                    Surface(
                        shape=RoundedCornerShape(12.dp),
                        color=if(statusIsError)Color(0xFF4A1414).copy(alpha=.92f) else c.panel2.copy(alpha=.96f),
                        border=androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if(statusIsError)Color(0xFFFF8A80) else c.bright.copy(alpha=.65f)
                        ),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement=Arrangement.spacedBy(8.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            if(busy)CircularProgressIndicator(modifier=Modifier.size(18.dp),strokeWidth=2.dp)
                            Text(
                                status,
                                color=if(statusIsError)Color(0xFFFFD7D2) else Color.White,
                                fontSize=12.sp,
                                fontWeight=FontWeight.Bold,
                                modifier=Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    if(RsSupabaseV60.configured)rsLoginT94(lang,"cloud_connected") else rsLoginT94(lang,"cloud_missing"),
                    color=if(RsSupabaseV60.configured)c.bright else Color(0xFFFF8A80),
                    fontSize=10.sp,
                    fontWeight=FontWeight.Bold
                )
                Text(
                    rsLoginT94(lang,"login_tip"),
                    color=c.muted,
                    fontSize=10.sp
                )
            }
        }

        Surface(
            shape=RoundedCornerShape(20.dp),
            color=c.panel.copy(alpha=(formOpacity*.92f).coerceIn(.18f,1f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp)){
                Text(rsEnrollmentT(lang,"build_label"),color=c.bright,fontWeight=FontWeight.Bold)
                Text(rsLoginT94(lang,"build_desc"),color=Color.White.copy(alpha=.72f))
            }
        }
    }
}

@Composable
private fun ShellV21(
    c:RsPalette,
    store:RsStore,
    role:RsRole,
    lang:RsLang,
    route:String,
    onLang:(RsLang)->Unit,
    onRoute:(String)->Unit,
    onLogout:()->Unit,
    content: @Composable () -> Unit
) {
    val home=if(role==RsRole.TRAINER)"trainer" else "home"
    val drawerState=rememberDrawerState(initialValue=DrawerValue.Closed)
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    val shellLayout=rsThemeLayoutV175(rsStoredThemeV175(store))
    val compactHeader=LocalConfiguration.current.screenWidthDp<380
    var pageSwipeX by remember{mutableFloatStateOf(0f)}
    var pageSwipeActive by remember{mutableStateOf(false)}
    var lastBackPressMs by remember{mutableLongStateOf(0L)}
    val chatFullScreen=route in setOf(
        "coachchat","groups","media","community","support","notifications","content"
    )

    BackHandler {
        when{
            drawerState.isOpen -> scope.launch{drawerState.close()}
            route!=home -> onRoute(home)
            else -> {
                val now=System.currentTimeMillis()
                if(now-lastBackPressMs<2200L){
                    (context as? Activity)?.moveTaskToBack(true)
                }else{
                    lastBackPressMs=now
                    Toast.makeText(context,"Press back again to leave RS KICKBOXING",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if(chatFullScreen){
        Box(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ){
            content()
        }
        return
    }

    val rawDrawerItems=if(role==RsRole.TRAINER) listOf(
        "members" to "Student Manager",
        "access" to "Access & Subscriptions",
        "plans_admin" to "Membership Plans",
        "session_builder" to "Session Builder",
        "content" to "Content Manager",
        "classes" to "Class Manager",
        "attendance" to "Attendance Center",
        "backgrounds" to "Visual Asset Studio",
        "branding" to "Branding & Site Settings",
        "payments" to "Payment Center",
        "notes" to "Coach Notes",
        "assessments" to "Assessments",
        "progress_admin" to "Progress Manager",
        "challenge_admin" to "Challenge Manager",
        "fightcamp_admin" to "Fight Camp Manager",
        "analytics" to "Analytics",
        "notifications" to "Notifications",
        "documents" to "Documents Manager",
        "community" to "Community Moderation",
        "landing_admin" to "Promotion Manager",
        "book" to "Book Manager",
        "schedule" to "Trainer Schedule",
        "release" to "Release & Legal Center",
        "privacy_admin" to "Privacy Requests",
        "settings" to "App Settings"
    ) else listOf(
        "home" to "RS Live Dashboard",
        "student_guide" to "App Guide",
        "coachchat" to "RS Chat",
        "music" to "My RS Music",
        "session" to "Session Player",
        "academy" to "RS Academy",
        "techniques" to "Technique Library",
        "home_training" to "Workout & Home Training",
        "compare" to "Technique Compare",
        "classes" to "Classes & Training",
        "checkin" to "Class Check-In",
        "homework" to "Homework",
        "progress" to "Progress",
        "challenges" to "Challenges",
        "fightcamp" to "Fight Camp",
        "badges" to "Badges",
        "community" to "Community",
        "vault" to "Knowledge Vault",

        "finance" to "Membership & Payments",
        "promotions" to "Promotions",
        "book" to "Trainer Book",
        "notifications" to "Notifications",
        "documents" to "Club Documents",
        "support" to "Support",
        "referrals" to "Referrals",
        "private_lessons" to "Private Lessons",
        "profile" to "My Profile",
        "settings" to "Settings & Privacy"
    )
    val drawerItems=if(role==RsRole.STUDENT)rawDrawerItems.filter{(target,_)->
        target in setOf("home","student_guide","coachchat","music") || rsStudentRouteEnabledV82(store,target)
    } else rawDrawerItems

    ModalNavigationDrawer(
        drawerState=drawerState,
        drawerContent={
            ModalDrawerSheet(
                drawerContainerColor=c.bg,
                drawerContentColor=c.text,
                modifier=Modifier.pointerInput(Unit){
                    var drawerSwipe=0f
                    detectHorizontalDragGestures(
                        onDragStart={drawerSwipe=0f},
                        onHorizontalDrag={change,amount->
                            drawerSwipe+=amount
                            change.consume()
                        },
                        onDragEnd={
                            if(drawerSwipe < -80f)scope.launch{drawerState.close()}
                            drawerSwipe=0f
                        },
                        onDragCancel={drawerSwipe=0f}
                    )
                }
            ){
                Column(
                    Modifier.fillMaxHeight().widthIn(
                        max=when(shellLayout.mode){
                            "TECH_COMPACT"->292.dp
                            "FIGHT_STRIP"->312.dp
                            else->330.dp
                        }
                    ).statusBarsPadding().navigationBarsPadding().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    Surface(
                        color=Color.Black.copy(alpha=.72f),
                        shape=RoundedCornerShape(shellLayout.panelRadius.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
                        tonalElevation=12.dp,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement=Arrangement.spacedBy(6.dp)
                        ){
                            RsPremiumBrandLockupV179(
                                c=c,
                                store=store,
                                modifier=Modifier.fillMaxWidth(),
                                compact=false,
                                subtitle=if(role==RsRole.TRAINER)"TRAINER / ADMIN" else "STUDENT"
                            )
                            Text(
                                if(role==RsRole.TRAINER)"ROYAL CONTROL NAVIGATION" else "MY RS NAVIGATION",
                                color=c.gold.copy(alpha=.86f),
                                fontSize=7.sp,
                                fontWeight=FontWeight.Black,
                                letterSpacing=1.2.sp
                            )
                        }
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(8.dp)
                    ){
                        if(role==RsRole.TRAINER){
                            RsDashboardCommandCenterV176(
                                c=c,
                                store=store,
                                role=role,
                                lang=lang,
                                onRoute={target->
                                    onRoute(target)
                                    scope.launch{drawerState.close()}
                                }
                            )
                            Text(
                                "MANAGEMENT",
                                color=c.gold,
                                fontSize=8.sp,
                                fontWeight=FontWeight.Black,
                                letterSpacing=1.3.sp,
                                modifier=Modifier.padding(start=4.dp,top=4.dp)
                            )
                        }
                        drawerItems.forEach{(target,fallback)->
                            val title=rsRouteTitle(lang,target,fallback)
                            Surface(
                                color=if(route==target)c.gold.copy(alpha=.12f) else Color.Black.copy(alpha=.30f),
                                shape=RoundedCornerShape(shellLayout.buttonRadius.dp),
                                border=BorderStroke(
                                    if(route==target && shellLayout.strongLines)2.dp else 1.dp,
                                    if(route==target)c.bright.copy(alpha=.58f) else c.gold.copy(alpha=.22f)
                                ),
                                modifier=Modifier.fillMaxWidth()
                            ){
                                Box{
                                    NavigationDrawerItem(
                                        label={
                                            Text(
                                                title,
                                                maxLines=1,
                                                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                fontWeight=if(route==target)FontWeight.Black else FontWeight.SemiBold,
                                                fontSize=12.sp
                                            )
                                        },
                                        icon={
                                            Surface(
                                                color=Color.Black.copy(alpha=.24f),
                                                shape=RoundedCornerShape(10.dp),
                                                border=BorderStroke(1.dp,c.gold.copy(alpha=.38f)),
                                                modifier=Modifier.size(34.dp)
                                            ){
                                                Box(contentAlignment=Alignment.Center){
                                                    Image(
                                                        painter=painterResource(R.drawable.rs_launcher_royal_v129),
                                                        contentDescription="RS",
                                                        modifier=Modifier.fillMaxSize().padding(2.dp)
                                                    )
                                                }
                                            }
                                        },
                                        badge={Text("›",color=if(route==target)c.bright else c.gold,fontSize=18.sp,fontWeight=FontWeight.Black)},
                                        selected=route==target,
                                        onClick={
                                            onRoute(target)
                                            scope.launch{drawerState.close()}
                                        },
                                        modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
                                        shape=RoundedCornerShape(shellLayout.buttonRadius.dp),
                                        colors=NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor=Color.Transparent,
                                            selectedTextColor=c.bright,
                                            unselectedContainerColor=Color.Transparent,
                                            unselectedTextColor=c.text
                                        )
                                    )
                                    Box(
                                        Modifier.fillMaxWidth(.42f).height(1.dp)
                                            .align(Alignment.BottomStart)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(c.gold.copy(alpha=.72f),c.bright.copy(alpha=.42f),Color.Transparent)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick={
                            scope.launch{drawerState.close()}
                            onLogout()
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsT(lang,"logout"))}
                }
            }
        }
    ){
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(route,drawerState.isOpen){
                    detectHorizontalDragGestures(
                        onDragStart={
                            pageSwipeX=0f
                            pageSwipeActive=true
                        },
                        onHorizontalDrag={change,amount->
                            if(pageSwipeActive){
                                pageSwipeX+=amount
                                change.consume()
                            }
                        },
                        onDragEnd={
                            if(pageSwipeActive){
                                when{
                                    drawerState.isOpen && pageSwipeX < -85f ->
                                        scope.launch{drawerState.close()}
                                    !drawerState.isOpen && pageSwipeX > 85f ->
                                        scope.launch{drawerState.open()}
                                    !drawerState.isOpen && pageSwipeX < -110f && route!=home ->
                                        onRoute(home)
                                }
                            }
                            pageSwipeX=0f
                            pageSwipeActive=false
                        },
                        onDragCancel={
                            pageSwipeX=0f
                            pageSwipeActive=false
                        }
                    )
                }
                .padding(if(route=="voice")4.dp else 9.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ) {
            if(route!="voice"){
            RsBrandedHeaderV21(c,store) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(if(compactHeader)4.dp else 7.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsPremiumBrandLockupV179(
                        c=c,
                        store=store,
                        modifier=Modifier.weight(1f),
                        compact=compactHeader,
                        subtitle=if(route==home)null else rsRouteTitle(
                            lang,route,route.replace('_',' ').replaceFirstChar{it.uppercase()}
                        )
                    )
                    OutlinedButton(
                        onClick={scope.launch{drawerState.open()}},
                        modifier=Modifier.size(if(compactHeader)32.dp else 36.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text("☰",fontSize=13.sp)}
                    RsNotificationBellV156(
                        c=c,
                        store=store,
                        lang=lang,
                        onOpenCall={peerId->
                            store.ps("chat_open_peer_id_v156",peerId)
                            onRoute("coachchat")
                        },
                        onOpenNotifications={onRoute("notifications")}
                    )
                    OutlinedButton(
                        onClick={
                            // The top mic always uses the single foreground Voice Wake
                            // recognizer. Do not create a second SpeechRecognizer here.
                            store.pb("ai_start_listening_v168",false)
                            store.pb("ai_direct_listen_v195",false)
                            store.pb("rs_voice_wake_resume_after_ai_v195",false)
                            store.pb("ai_immersive_v171",true)
                            if(store.b("rs_ai_listening_enabled_v200",true)){
                                runCatching{RsVoiceWakeServiceV165.directListenOnce(context)}
                                onRoute("voice")
                            }else{
                                android.widget.Toast.makeText(
                                    context,
                                    "AI listening is off. Turn it on manually in RS Chat → Settings.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier=Modifier.size(if(compactHeader)33.dp else 38.dp),
                        shape=CircleShape,
                        border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.58f)),
                        contentPadding=PaddingValues(0.dp)
                    ){
                        Text("🎙",fontSize=14.sp,color=Color(0xFF58C9FF))
                    }
                }
            }
            }
            if(route!="music" && route!="music_admin" && route!="voice"){
                RsMiniMusicPlayerV90(c,lang){onRoute(if(role==RsRole.TRAINER)"music_admin" else "music")}
            }
            if(route!=home && route !in setOf("voice","coachchat","groups","community","support","notifications","content")){
                RsPageAssistBarV177(
                    c=c,
                    store=store,
                    role=role,
                    lang=lang,
                    route=route,
                    onGuide={onRoute(if(role==RsRole.TRAINER)"guide" else "student_guide")},
                    onAiHelp={
                        val title=rsRouteTitle(lang,route,route.replace('_',' ').replaceFirstChar{it.uppercase()})
                        store.ps("ai_pending_spoken_v171","Explain what "+title+" is for and show me how to use this page step by step.")
                        store.pb("ai_start_listening_v168",false)
                        store.pb("ai_immersive_v171",true)
                        onRoute("voice")
                    }
                )
            }
            if(role==RsRole.STUDENT && rsOpsEnabledV56(store,RsOpsKeysV56.MAINTENANCE,false)){
                Surface(
                    color=c.gold.copy(alpha=.18f),
                    shape=RoundedCornerShape(14.dp),
                    border=androidx.compose.foundation.BorderStroke(1.dp,c.bright.copy(alpha=.40f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(
                        store.s(RsOpsKeysV56.MAINTENANCE_MESSAGE,rsCommonT95(lang,"maintenance_default")),
                        color=c.bright,
                        fontWeight=FontWeight.Bold,
                        fontSize=10.sp,
                        modifier=Modifier.padding(horizontal=12.dp,vertical=9.dp)
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .clip(if(route=="voice")RoundedCornerShape(0.dp) else RoundedCornerShape(shellLayout.panelRadius.dp))
            ){
                if(route!="voice")RsThemePageFrameV177(c,store)
                else Box(Modifier.fillMaxSize().background(Color.Black))
                Box(
                    Modifier.fillMaxSize().padding(
                        horizontal=if(route=="voice")0.dp else when(shellLayout.mode){
                            "TECH_COMPACT"->2.dp
                            "FIGHT_STRIP"->4.dp
                            else->3.dp
                        },
                        vertical=if(route=="voice")0.dp else 2.dp
                    )
                ){
                    content()
                    if(route=="voice"){
                        OutlinedButton(
                            onClick={scope.launch{drawerState.open()}},
                            modifier=Modifier.align(Alignment.CenterStart)
                                .padding(start=4.dp)
                                .size(width=30.dp,height=66.dp),
                            shape=RoundedCornerShape(16.dp),
                            border=BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF58C9FF).copy(alpha=.74f),
                                        c.gold.copy(alpha=.66f),
                                        Color(0xFF58C9FF).copy(alpha=.42f)
                                    )
                                )
                            ),
                            colors=ButtonDefaults.outlinedButtonColors(
                                containerColor=Color.Black.copy(alpha=.52f),
                                contentColor=c.bright
                            ),
                            contentPadding=PaddingValues(0.dp)
                        ){
                            Text("☰",fontSize=12.sp,fontWeight=FontWeight.Black)
                        }
                    }
                }
            }
            if(route!="voice")RsBrandedFooterV21(c,store)
        }
    }
}

@Composable
private fun RsThemePageFrameV177(c:RsPalette,store:RsStore){
    val layout=rsThemeLayoutV175(rsStoredThemeV175(store))
    val brush=when(layout.mode){
        "FIGHT_STRIP"->androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color.Black,c.gold.copy(alpha=.10f),Color.Black)
        )
        "TECH_COMPACT"->androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(c.bg,c.panel.copy(alpha=.72f),c.bg)
        )
        "HOLO_CARDS"->androidx.compose.ui.graphics.Brush.radialGradient(
            listOf(c.bright.copy(alpha=.08f),c.panel.copy(alpha=.62f),c.bg)
        )
        "PERFORMANCE_STACK"->androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(c.panel2.copy(alpha=.42f),c.bg,c.panel.copy(alpha=.32f))
        )
        else->androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(c.panel.copy(alpha=.40f),c.bg,c.panel2.copy(alpha=.34f))
        )
    }
    Box(Modifier.fillMaxSize().background(brush)){
        when(layout.mode){
            "FIGHT_STRIP"->{
                Box(Modifier.fillMaxHeight().width(4.dp).align(Alignment.CenterStart).background(c.bright.copy(alpha=.52f)))
                Box(Modifier.fillMaxHeight().width(1.dp).align(Alignment.CenterEnd).background(c.gold.copy(alpha=.30f)))
            }
            "TECH_COMPACT"->{
                Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).background(c.bright.copy(alpha=.32f)))
                Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(c.gold.copy(alpha=.20f)))
            }
            "HOLO_CARDS"->{
                Box(Modifier.size(150.dp).align(Alignment.TopEnd).background(
                    androidx.compose.ui.graphics.Brush.radialGradient(listOf(c.bright.copy(alpha=.10f),Color.Transparent))
                ))
            }
            "PERFORMANCE_STACK"->{
                Box(Modifier.fillMaxWidth(.42f).height(3.dp).align(Alignment.TopStart).background(c.gold.copy(alpha=.46f)))
            }
        }
    }
}

@Composable
private fun RsPageAssistBarV177(
    c:RsPalette,
    store:RsStore,
    role:RsRole,
    lang:RsLang,
    route:String,
    onGuide:()->Unit,
    onAiHelp:()->Unit
){
    val layout=rsThemeLayoutV175(rsStoredThemeV175(store))
    val title=rsRouteTitle(lang,route,route.replace('_',' ').replaceFirstChar{it.uppercase()})
    Surface(
        color=c.panel.copy(alpha=layout.glassAlpha.coerceIn(.62f,.96f)),
        shape=RoundedCornerShape(layout.buttonRadius.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=if(layout.strongLines).38f else .20f)),
        modifier=Modifier.fillMaxWidth()
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=5.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)
        ){
            Column(Modifier.weight(1f)){
                Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp,maxLines=1)
                Text("PAGE ASSIST",color=c.muted,fontSize=6.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)
            }
            TextButton(onClick=onGuide,contentPadding=PaddingValues(horizontal=7.dp,vertical=2.dp)){
                Text("GUIDE",fontSize=7.sp,fontWeight=FontWeight.Bold)
            }
            Button(
                onClick=onAiHelp,
                contentPadding=PaddingValues(horizontal=9.dp,vertical=4.dp),
                shape=RoundedCornerShape(layout.buttonRadius.dp)
            ){
                Text("✧ AI HELP",fontSize=7.sp,fontWeight=FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LanguageV21(current:RsLang,onSelect:(RsLang)->Unit,modifier:Modifier=Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.widthIn(min=66.dp,max=82.dp)) {
        OutlinedButton(
            onClick={open=true},
            modifier=Modifier.fillMaxWidth().heightIn(min=34.dp,max=36.dp),
            contentPadding=PaddingValues(horizontal=7.dp,vertical=2.dp)
        ){
            Text(
                "🌐 "+current.code.uppercase(),
                fontSize=9.5.sp,
                fontWeight=FontWeight.Bold,
                maxLines=1
            )
        }
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            rsLangs.forEach { language ->
                DropdownMenuItem(
                    text={Text(language.name)},
                    onClick={onSelect(language);open=false}
                )
            }
        }
    }
}

@Composable
private fun SessionV21(c:RsPalette,lang:RsLang) {
    var running by remember{mutableStateOf(false)}
    var phase by remember{mutableStateOf("WORK")}
    var sec by remember{mutableIntStateOf(120)}
    var round by remember{mutableIntStateOf(1)}
    LaunchedEffect(running,sec,phase,round) {
        if(running&&sec>0){delay(1000);sec--}
        else if(running&&sec==0){if(phase=="WORK"){phase="REST";sec=45}else if(round<5){round++;phase="WORK";sec=120}else{running=false;phase="COMPLETE"}}
    }
    val phaseText=when(phase){
        "REST"->rsT(lang,"rest")
        "COMPLETE"->rsT(lang,"complete")
        else->rsT(lang,"work")
    }
    RsScroll(c,rsT(lang,"session_title"),rsT(lang,"session_sub")) {
        RsPanel(c) {
            Text("${rsT(lang,"round")} $round / 5 · $phaseText",color=c.bright,fontWeight=FontWeight.Bold)
            Text("%d:%02d".format(sec/60,sec%60),color=c.bright,fontSize=50.sp,fontWeight=FontWeight.Black)
            Text(if(phase=="REST")rsRestInstruction(lang) else rsWorkInstruction(lang),color=c.text)
            val mainLabel=if(running)rsT(lang,"pause") else rsT(lang,"start")
            Button(onClick={if(phase=="COMPLETE"){round=1;phase="WORK";sec=120};running=!running},modifier=Modifier.fillMaxWidth()){
                Text(mainLabel,fontSize=adaptiveLabelSp(mainLabel,14f).sp,maxLines=1)
            }
            val next=rsT(lang,"next_round")
            OutlinedButton(onClick={if(round<5)round++;phase="WORK";sec=120;running=false},modifier=Modifier.fillMaxWidth()){
                Text(next,fontSize=adaptiveLabelSp(next,13f).sp,maxLines=1)
            }
            val reset=rsT(lang,"reset_session")
            OutlinedButton(onClick={round=1;phase="WORK";sec=120;running=false},modifier=Modifier.fillMaxWidth()){
                Text(reset,fontSize=adaptiveLabelSp(reset,13f).sp,maxLines=1)
            }
        }
    }
}

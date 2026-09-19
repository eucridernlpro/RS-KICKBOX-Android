package com.rskickbox.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun RsKickboxV21App() {
    val context = LocalContext.current
    val store = remember { RsStore(context) }
    val appScope = rememberCoroutineScope()
    var role by remember { mutableStateOf<RsRole?>(null) }
    var route by remember { mutableStateOf("home") }
    var authRestoreAttempted by remember { mutableStateOf(false) }
    var authRestoring by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(rsLangs.firstOrNull { it.code == store.s("lang", "en") } ?: rsLangs.first()) }
    var theme by remember { mutableStateOf(runCatching { RsTheme.valueOf(store.s("theme", "ELITE_GOLD")) }.getOrDefault(RsTheme.ELITE_GOLD)) }
    var introDone by remember { mutableStateOf(!store.b("intro_enabled", true) || (!store.b("intro_every_launch", true) && store.b("intro_seen", false))) }
    val c = paletteFor(theme)

    LaunchedEffect(introDone,authRestoreAttempted){
        if(introDone && !authRestoreAttempted){
            authRestoreAttempted=true
            if(RsSupabaseV60.configured){
                authRestoring=true
                rsCloudCurrentSessionV67()
                    .onSuccess{session->
                        if(session!=null){
                            store.ps("session_student_email",session.email)
                            store.ps("session_student_name",session.displayName)
                            store.ps("session_plan",session.plan)
                            store.ps("session_role",if(session.role==RsRole.TRAINER)"trainer" else "student")
                            role=session.role
                            route=if(session.role==RsRole.TRAINER)"trainer" else "home"
                        }
                    }
                authRestoring=false
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary=c.bright,secondary=c.gold,background=c.bg,surface=c.panel,onBackground=c.text,onSurface=c.text)) {
        Box(Modifier.fillMaxSize()) {
            when {
                !introDone -> RsCinematicIntroV21(c, store) {
                    store.pb("intro_seen", true)
                    introDone = true
                }
                authRestoring -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
                        CircularProgressIndicator()
                        Text("Restoring secure RS KICKBOX session…",color=c.text)
                    }
                }
                role == null -> RsLiveBackground(c, store, BgScope.LOGIN) {
                    RsPerPageBackgroundV21(store, "login") {
                        LoginV21(c, store, lang, { selected -> lang=selected;store.ps("lang",selected.code) }) { selected ->
                            role = selected
                            route = if(selected==RsRole.TRAINER) "trainer" else "home"
                        }
                    }
                }
                else -> {
                    val active = role!!
                    val scope = if(active==RsRole.TRAINER) BgScope.TRAINER_TRAINING else BgScope.STUDENT_TRAINING
                    RsLiveBackground(c, store, scope) {
                        RsPerPageBackgroundV21(store, route) {
                            ShellV21(c, store, active, lang, route, { selected -> lang=selected;store.ps("lang",selected.code) }, { route=it }, {
                                role=null
                                route="home"
                                appScope.launch { rsCloudLogoutV63() }
                            }) {
                                when(route) {
                                    "home", "trainer" -> RsPremiumDashboardV21(c, store, active, lang) { route=it }
                                    "themes" -> RsThemeStudio(c, theme) { selected -> theme=selected;store.ps("theme",selected.name) }
                                    "backgrounds" -> RsVisualAssetStudioV21(c, store)
                                    "branding" -> RsBrandSiteSettingsV21(c, store)
                                    "intro_settings" -> RsIntroSettingsV21(c, store)
                                    "voice" -> RsTechniqueCoachV27(c, lang, store, active)
                                    "session" -> if(active==RsRole.TRAINER) RsSessionBuilderV52(c,store,lang) else RsSessionPlayerV52(c,store,lang)
                                    "access" -> RsAccessControlV49(c,store,lang)
                                    "payments" -> RsTrainerPaymentCenterV39(c,store,lang)
                                    "members" -> RsMemberManager(c,store,lang)
                                    "classes" -> if(active==RsRole.TRAINER) RsClassManagerV38(c,store,lang) else RsStudentClassesV38(c,store,lang)
                                    "attendance" -> RsAttendanceV38(c,store,lang)
                                    "checkin" -> RsStudentCheckInV52(c,store,lang)
                                    "invoices" -> RsTrainerInvoicesV39(c,store,lang)
                                    "book" -> if(active==RsRole.TRAINER) RsBookManagerV45(c,store,lang) else RsBookLibraryV45(c,store,lang)
                                    "settings" -> if(active==RsRole.TRAINER) RsAdminSettingsV56(c,store,lang) else RsStudentPrivacyV40(c,store,lang){
                                        role=null
                                        route="home"
                                    }
                                    "academy" -> RsAcademyV54(c,store,lang)
                                    "progress" -> RsStudentProgressV46(c,store,lang)
                                    "challenges" -> RsStudentChallengesV47(c,store,lang)
                                    "fightcamp" -> RsStudentFightCampV47(c,store,lang)
                                    "finance" -> RsStudentFinanceV39(c,store,lang)
                                    "community" -> RsCommunityV50(c,store,lang,active)
                                    "media" -> RsTrainingMediaV55(c,store,lang,active)
                                    "music", "music_admin" -> RsLocalMusicCenterV20(c, store, active, lang)
                                    "techniques" -> RsTechniqueLibraryV54(c,lang)
                                    "home_training" -> RsHomeTrainingV54(c,store,lang)
                                    "workout" -> RsWorkoutGeneratorV54(c,lang)
                                    "badges" -> RsBadgesV47(c,store,lang)
                                    "vault" -> RsKnowledgeVaultV48(c,store,lang)
                                    "compare" -> RsTechniqueCompareV54(c,lang)
                                    "coachchat" -> RsCoachChatV44(c,store,lang,active)
                                    "events" -> RsStudentEventsV42(c,store,lang)
                                    "promotions" -> RsPromotionPageV45(c,store,lang){route="book"}
                                    "notifications" -> RsNotificationsV41(c,store,lang,active)
                                    "analytics" -> RsAnalyticsV53(c,store,lang)
                                    "guide" -> RsTrainerGuideV54(c,store,lang){route=it}
                                    "student_guide" -> RsStudentGuideV81(c,store,lang){route=it}
                                    "documents" -> RsDocumentsV51(c,store,lang,active)
                                    "support" -> RsSupportV51(c,store,lang,active)
                                    "referrals" -> RsReferralsV51(c,store,lang)
                                    "schedule" -> RsTrainerScheduleV43(c,store,lang)
                                    "content" -> RsContentManagerV48(c,store,lang)
                                    "notes" -> RsCoachNotesV46(c,store,lang)
                                    "homework" -> RsStudentHomeworkV46(c,store,lang)
                                    "favorites" -> RsFavoritesV48(c,store,lang)
                                    "history" -> RsHistoryV48(c,store,lang)
                                    "private_lessons" -> RsStudentPrivateLessonsV43(c,store,lang)
                                    "profile" -> RsProfileV50(c,store,lang)
                                    "groups" -> RsGroupsV50(c,store,lang,active)
                                    "search" -> RsSearchV48(c,store,lang)
                                    "homework_admin" -> RsHomeworkManagerV46(c,store,lang)
                                    "lesson_editor" -> RsContentManagerV48(c,store,lang)
                                    "plans_admin" -> RsMembershipPlansV49(c,store,lang)
                                    "progress_admin" -> RsProgressManagerV53(c,store,lang)
                                    "assessments" -> RsAssessmentsV46(c,store,lang)
                                    "events_admin" -> RsEventManagerV42(c,store,lang)
                                    "qr_attendance" -> RsQrAttendanceTrainerV52(c,store,lang)
                                    "session_builder" -> RsSessionBuilderV52(c,store,lang)
                                    "challenge_admin" -> RsChallengeManagerV47(c,store,lang)
                                    "fightcamp_admin" -> RsFightCampManagerV47(c,store,lang)
                                    "landing_admin" -> RsPromotionManagerV45(c,store,lang)
                                    "release" -> RsReleaseCenterV16(c, store)
                                    else -> RsScroll(c, route.replaceFirstChar { it.uppercase() }, "RS premium native module") {
                                        RsPanel(c) {
                                            Text("Module ready",color=c.bright,fontWeight=FontWeight.Bold)
                                            Text("This module uses the new v0.21 visual system.",color=c.muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginV21(c:RsPalette,store:RsStore,lang:RsLang,onLang:(RsLang)->Unit,onLogin:(RsRole)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var pendingInviteToken by remember { mutableStateOf("") }
    val formOpacity=store.s("login_form_opacity","0.82").toFloatOrNull()?.coerceIn(.20f,1f)?:.82f

    fun finishCloudLogin(session:RsCloudSessionV63){
        store.ps("session_student_email",session.email)
        store.ps("session_student_name",session.displayName)
        store.ps("session_plan",session.plan)
        store.ps("session_role",if(session.role==RsRole.TRAINER)"trainer" else "student")
        statusIsError=false
        status="✓ Login successful. Welcome "+session.displayName
        onLogin(session.role)
    }

    fun signInCloud(){
        if(busy)return
        if(email.isBlank()){
            statusIsError=true
            status="Enter your email address."
            return
        }
        if(pass.isBlank()){
            statusIsError=true
            status="Enter your password."
            return
        }
        busy=true
        statusIsError=false
        status="Checking your email and password…"
        scope.launch{
            rsCloudLoginV63(email,pass)
                .onSuccess{finishCloudLogin(it)}
                .onFailure{
                    statusIsError=true
                    status="Login failed: "+(it.message?.takeIf{msg->msg.isNotBlank()}?:"Email or password was not accepted.")
                }
            busy=false
        }
    }

    fun activateInvite(){
        if(busy)return
        if(pass.length<10){
            statusIsError=true
            status="Choose a password with at least 10 characters."
            return
        }
        busy=true
        statusIsError=false
        status="Activating your RS KICKBOX account…"
        scope.launch{
            rsRedeemStudentInviteV63(email,pendingInviteToken,pass)
                .onSuccess{
                    rsCloudLoginV63(email,pass)
                        .onSuccess{
                            pendingInviteToken=""
                            finishCloudLogin(it)
                        }
                        .onFailure{statusIsError=true;status=it.message?:"Account created, but sign-in failed."}
                }
                .onFailure{statusIsError=true;status=it.message?:"Could not activate invitation."}
            busy=false
        }
    }

    fun applyInvite(raw:String){
        val invite=rsParseInviteV33(raw)
        if(invite==null){
            statusIsError=true
            status=rsEnrollMsg(lang,"invalid_qr")
        }else{
            email=invite.email
            pendingInviteToken=invite.activationCode
            pass=""
            statusIsError=false
            status="Invitation loaded for "+invite.name.ifBlank{rsT(lang,"student")}+". Choose a password with at least 10 characters."
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
        val mainLogo=store.s("brand_asset_main_logo","")
        if(mainLogo.isNotBlank())RsUriPreviewV21(mainLogo,Modifier.fillMaxWidth().height(120.dp),"CENTER")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.spacedBy(10.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Text(
                "♛ "+store.s("brand_header_name","RS KICKBOX"),
                color=c.bright,
                fontSize=31.sp,
                fontWeight=FontWeight.Black,
                maxLines=1,
                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier=Modifier.weight(1f)
            )
            LanguageV21(lang,onLang)
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
                Text(rsT(lang,"member_access"),color=Color.White,fontWeight=FontWeight.Bold)
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

                OutlinedTextField(
                    email,
                    {email=it; if(pendingInviteToken.isNotBlank() && !it.equals(email,true))pendingInviteToken=""},
                    label={Text(rsT(lang,"email"))},
                    colors=fieldColors,
                    singleLine=true,
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    pass,
                    {pass=it},
                    label={Text(if(pendingInviteToken.isNotBlank())"Create password (10+ characters)" else "Password")},
                    colors=fieldColors,
                    singleLine=true,
                    enabled=!busy,
                    visualTransformation=if(showPassword)VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon={
                        TextButton(
                            onClick={showPassword=!showPassword},
                            enabled=!busy,
                            contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)
                        ){
                            Text(if(showPassword)"HIDE" else "👁",color=Color.White,fontSize=13.sp)
                        }
                    },
                    modifier=Modifier.fillMaxWidth()
                )
                Button(
                    onClick={if(pendingInviteToken.isNotBlank()) activateInvite() else signInCloud()},
                    enabled=!busy && email.isNotBlank() && pass.isNotBlank(),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(
                        when{
                            busy->"Please wait…"
                            pendingInviteToken.isNotBlank()->"Activate account"
                            else->"Sign in"
                        },
                        fontSize=12.sp,
                        maxLines=1
                    )
                }
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
                    if(RsSupabaseV60.configured)"● Cloud login connected" else "● Cloud backend is not configured in this build",
                    color=if(RsSupabaseV60.configured)c.bright else Color(0xFFFF8A80),
                    fontSize=10.sp,
                    fontWeight=FontWeight.Bold
                )
                Text("Your account role and access are verified securely from Supabase.",color=c.muted,fontSize=10.sp)
            }
        }

        Surface(
            shape=RoundedCornerShape(20.dp),
            color=c.panel.copy(alpha=(formOpacity*.92f).coerceIn(.18f,1f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp)){
                Text(rsEnrollmentT(lang,"build_label"),color=c.bright,fontWeight=FontWeight.Bold)
                Text("Trainer-created accounts · secure QR invitations · camera/gallery activation · authenticated cloud sessions.",color=Color.White.copy(alpha=.72f))
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
    val drawerItems=if(role==RsRole.TRAINER) listOf(
        "trainer" to "Trainer Dashboard",
        "guide" to "App Guide",
        "members" to "Student Manager",
        "access" to "Access & Subscriptions",
        "plans_admin" to "Membership Plans",
        "voice" to "AI Technique Coach",
        "session" to "Trainer Session",
        "session_builder" to "Session Builder",
        "qr_attendance" to "QR Attendance",
        "content" to "Content Manager",
        "media" to "Training Media Manager",
        "lesson_editor" to "Lesson Editor",
        "classes" to "Class Manager",
        "attendance" to "Attendance",
        "backgrounds" to "Visual Asset Studio",
        "branding" to "Branding & Site Settings",
        "payments" to "Payment Center",
        "homework_admin" to "Homework Manager",
        "notes" to "Coach Notes",
        "assessments" to "Assessments",
        "progress_admin" to "Progress Manager",
        "challenge_admin" to "Challenge Manager",
        "fightcamp_admin" to "Fight Camp Manager",
        "analytics" to "Analytics",
        "notifications" to "Notifications",
        "documents" to "Documents Manager",
        "support" to "Support Inbox",
        "community" to "Community Moderation",
        "groups" to "Groups Manager",
        "landing_admin" to "Promotion Manager",
        "book" to "Book Manager",
        "coachchat" to "Coach Inbox",
        "schedule" to "Trainer Schedule",
        "release" to "Release & Legal Center",
        "settings" to "App Settings"
    ) else listOf(
        "home" to "RS Live Dashboard",
        "voice" to "AI Technique Coach",
        "session" to "Session Player",
        "academy" to "RS Academy",
        "techniques" to "Technique Library",
        "home_training" to "Home Training",
        "workout" to "Workout Generator",
        "compare" to "Technique Compare",
        "classes" to "Classes & Events",
        "checkin" to "Class Check-In",
        "homework" to "Homework",
        "progress" to "Progress",
        "challenges" to "Challenges",
        "fightcamp" to "Fight Camp",
        "badges" to "Badges",
        "community" to "Community",
        "groups" to "Groups",
        "media" to "Training Media",
        "vault" to "Knowledge Vault",
        "favorites" to "Saved & Favorites",
        "history" to "Training History",
        "search" to "Search",
        "music" to "My RS Music",
        "finance" to "Membership & Payments",
        "promotions" to "Promotions",
        "book" to "Trainer Book",
        "notifications" to "Notifications",
        "documents" to "Club Documents",
        "support" to "Support",
        "referrals" to "Referrals",
        "coachchat" to "Private Coach Chat",
        "private_lessons" to "Private Lessons",
        "profile" to "My Profile",
        "settings" to "Settings & Privacy"
    )

    ModalNavigationDrawer(
        drawerState=drawerState,
        drawerContent={
            ModalDrawerSheet(
                drawerContainerColor=c.bg,
                drawerContentColor=c.text
            ){
                Column(
                    Modifier.fillMaxHeight().widthIn(max=330.dp).statusBarsPadding().navigationBarsPadding().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    RsPanel(c){
                        Text("♛ ${store.s("brand_header_name","RS KICKBOX")}",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
                        Text(
                            if(role==RsRole.TRAINER)rsT(lang,"trainer_admin") else rsT(lang,"student"),
                            color=c.muted,
                            fontSize=11.sp
                        )
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(3.dp)
                    ){
                        drawerItems.forEach{(target,fallback)->
                            val title=rsRouteTitle(lang,target,fallback)
                            NavigationDrawerItem(
                                label={Text(title,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},
                                selected=route==target,
                                onClick={
                                    onRoute(target)
                                    scope.launch{drawerState.close()}
                                },
                                colors=NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor=c.gold.copy(alpha=.22f),
                                    selectedTextColor=c.bright,
                                    unselectedContainerColor=Color.Transparent,
                                    unselectedTextColor=c.text
                                )
                            )
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
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(9.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ) {
            RsBrandedHeaderV21(c,store) {
                Text("♛ ${store.s("brand_header_name","RS KICKBOX")}",color=c.bright,fontSize=20.sp,fontWeight=FontWeight.Black)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(6.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick={scope.launch{drawerState.open()}},
                        modifier=Modifier.widthIn(min=42.dp,max=50.dp).heightIn(min=40.dp),
                        contentPadding=PaddingValues(horizontal=8.dp)
                    ){Text("☰")}
                    LanguageV21(lang,onLang)
                    if(route!=home) OutlinedButton(
                        onClick={onRoute(home)},
                        modifier=Modifier.widthIn(min=42.dp,max=50.dp).heightIn(min=40.dp),
                        contentPadding=PaddingValues(horizontal=8.dp)
                    ){Text("‹")}
                    OutlinedButton(
                        onClick=onLogout,
                        modifier=Modifier.widthIn(min=68.dp,max=106.dp).heightIn(min=40.dp),
                        contentPadding=PaddingValues(horizontal=6.dp)
                    ){
                        val logout=rsT(lang,"logout")
                        Text(logout,fontSize=adaptiveLabelSp(logout,9f).sp,maxLines=1)
                    }
                }
            }
            if(role==RsRole.STUDENT && rsOpsEnabledV56(store,RsOpsKeysV56.MAINTENANCE,false)){
                Surface(
                    color=c.gold.copy(alpha=.18f),
                    shape=RoundedCornerShape(14.dp),
                    border=androidx.compose.foundation.BorderStroke(1.dp,c.bright.copy(alpha=.40f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(
                        store.s(RsOpsKeysV56.MAINTENANCE_MESSAGE,"RS KICKBOX maintenance notice: some services may be temporarily limited."),
                        color=c.bright,
                        fontWeight=FontWeight.Bold,
                        fontSize=10.sp,
                        modifier=Modifier.padding(horizontal=12.dp,vertical=9.dp)
                    )
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)){content()}
            RsBrandedFooterV21(c,store)
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

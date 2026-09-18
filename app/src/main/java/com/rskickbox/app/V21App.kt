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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun RsKickboxV21App() {
    val context = LocalContext.current
    val store = remember { RsStore(context) }
    var role by remember { mutableStateOf<RsRole?>(null) }
    var route by remember { mutableStateOf("home") }
    var lang by remember { mutableStateOf(rsLangs.firstOrNull { it.code == store.s("lang", "en") } ?: rsLangs.first()) }
    var theme by remember { mutableStateOf(runCatching { RsTheme.valueOf(store.s("theme", "ELITE_GOLD")) }.getOrDefault(RsTheme.ELITE_GOLD)) }
    var introDone by remember { mutableStateOf(!store.b("intro_enabled", true) || (!store.b("intro_every_launch", true) && store.b("intro_seen", false))) }
    val c = paletteFor(theme)

    MaterialTheme(colorScheme = darkColorScheme(primary=c.bright,secondary=c.gold,background=c.bg,surface=c.panel,onBackground=c.text,onSurface=c.text)) {
        Box(Modifier.fillMaxSize()) {
            when {
                !introDone -> RsCinematicIntroV21(c, store) {
                    store.pb("intro_seen", true)
                    introDone = true
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
                            ShellV21(c, store, active, lang, route, { selected -> lang=selected;store.ps("lang",selected.code) }, { route=it }, { role=null }) {
                                when(route) {
                                    "home", "trainer" -> RsPremiumDashboardV21(c, store, active, lang) { route=it }
                                    "themes" -> RsThemeStudio(c, theme) { selected -> theme=selected;store.ps("theme",selected.name) }
                                    "backgrounds" -> RsVisualAssetStudioV21(c, store)
                                    "branding" -> RsBrandSiteSettingsV21(c, store)
                                    "intro_settings" -> RsIntroSettingsV21(c, store)
                                    "voice" -> RsTechniqueCoachV27(c, lang, store, active)
                                    "session" -> SessionV21(c,lang)
                                    "access" -> RsAccessControl(c, store)
                                    "payments" -> RsPaymentCenter(c, store)
                                    "members" -> RsMemberManager(c,store,lang)
                                    "classes" -> if(active==RsRole.TRAINER) RsClassManager(c) else RsStudentClasses(c)
                                    "attendance" -> RsAttendance(c)
                                    "invoices" -> RsInvoices(c)
                                    "book" -> if(active==RsRole.TRAINER) RsBookManager(c) else RsTrainerBook(c)
                                    "settings" -> if(active==RsRole.TRAINER) RsAdminSettingsV16(c,store) else RsStudentSettingsV16(c,store)
                                    "academy" -> RsAcademy(c)
                                    "progress" -> RsProgress(c)
                                    "challenges" -> RsChallenges(c)
                                    "fightcamp" -> RsFightCamp(c)
                                    "finance" -> RsFinance(c)
                                    "community" -> RsCommunity(c)
                                    "media" -> RsTrainingMediaV16(c, store)
                                    "music", "music_admin" -> RsLocalMusicCenterV20(c, store, active, lang)
                                    "techniques" -> RsTechniqueLibrary(c)
                                    "home_training" -> RsHomeTraining(c)
                                    "workout" -> RsWorkoutGenerator(c)
                                    "badges" -> RsBadges(c)
                                    "vault" -> RsKnowledgeVault(c)
                                    "compare" -> RsTechniqueCompare(c)
                                    "coachchat" -> RsPrivateCoachChat(c)
                                    "events" -> RsEvents(c)
                                    "notifications" -> RsNotificationsCenterV16(c, store)
                                    "analytics" -> RsAnalytics(c)
                                    "documents" -> RsDocuments(c)
                                    "support" -> RsSupport(c)
                                    "referrals" -> RsReferrals(c)
                                    "schedule" -> RsTrainerSchedule(c)
                                    "content" -> RsContentManager(c)
                                    "notes" -> RsCoachNotes(c)
                                    "homework" -> RsHomeworkHub(c)
                                    "favorites" -> RsFavoritesHub(c)
                                    "history" -> RsHistoryHub(c)
                                    "private_lessons" -> RsPrivateLessonsHub(c)
                                    "profile" -> RsProfileHub(c)
                                    "groups" -> RsGroupsHub(c)
                                    "search" -> RsSearchHub(c)
                                    "homework_admin" -> RsHomeworkManagerV14(c)
                                    "lesson_editor" -> RsLessonEditorV14(c)
                                    "plans_admin" -> RsMembershipPlansV14(c)
                                    "progress_admin" -> RsProgressManagerV14(c)
                                    "assessments" -> RsAssessmentsV14(c)
                                    "events_admin" -> RsEventManagerV14(c)
                                    "qr_attendance" -> RsQrAttendanceV14(c)
                                    "session_builder" -> RsSessionBuilderV14(c)
                                    "challenge_admin" -> RsChallengeManagerV14(c)
                                    "fightcamp_admin" -> RsFightCampManagerV14(c)
                                    "landing_admin" -> RsLandingManagerV14(c)
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
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var pass by remember { mutableStateOf("preview123") }
    var status by remember { mutableStateOf("") }
    val formOpacity=store.s("login_form_opacity","0.82").toFloatOrNull()?.coerceIn(.20f,1f)?:.82f

    fun applyInvite(raw:String){
        val invite=rsParseInviteV33(raw)
        if(invite==null){
            status="This QR is not a valid RS KICKBOX student invitation."
        }else{
            email=invite.email
            pass=invite.activationCode
            status="Invitation loaded for ${invite.name.ifBlank{"student"}} · ${invite.plan}. You can now log in."
        }
    }

    val galleryQrPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            val raw=rsDecodeQrImageV33(context,uri)
            if(raw==null)status="No readable RS KICKBOX QR code was found in this image."
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

    fun tryStudentLogin(){
        val accounts=rsLoadStudentsV33(store)
        val demo=email.equals("alex@rskickbox.nl",true) && pass=="preview123"
        val match=rsFindStudentV33(store,email,pass)
        when{
            demo->onLogin(RsRole.STUDENT)
            match!=null->{
                status="Welcome ${match.name}."
                onLogin(RsRole.STUDENT)
            }
            else->status="Student account not found, inactive, or activation code is incorrect."
        }
    }

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
        Text("♛ ${store.s("brand_header_name","RS KICKBOX")}",color=c.bright,fontSize=31.sp,fontWeight=FontWeight.Black)
        LanguageV21(lang,onLang)
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
                                    if(raw.isNullOrBlank())status="The scanned QR did not contain an invitation."
                                    else applyInvite(raw)
                                }
                                .addOnCanceledListener{status="QR scan cancelled."}
                                .addOnFailureListener{status="QR scanner could not open: ${it.message?:"unknown error"}"}
                        },
                        modifier=Modifier.weight(1f)
                    ){Text(rsEnrollmentT(lang,"scan_qr"),fontSize=11.sp)}
                    OutlinedButton(
                        onClick={galleryQrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                        modifier=Modifier.weight(1f)
                    ){Text(rsEnrollmentT(lang,"upload_qr"),fontSize=11.sp)}
                }

                OutlinedTextField(
                    email,
                    {email=it},
                    label={Text(rsT(lang,"email"))},
                    colors=fieldColors,
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    pass,
                    {pass=it},
                    label={Text(rsT(lang,"password")+" / activation code")},
                    colors=fieldColors,
                    singleLine=true,
                    visualTransformation=PasswordVisualTransformation(),
                    modifier=Modifier.fillMaxWidth()
                )
                Button(onClick={tryStudentLogin()},modifier=Modifier.fillMaxWidth()){
                    Text(rsT(lang,"student_preview"),fontSize=adaptiveLabelSp(rsT(lang,"student_preview"),12f).sp,maxLines=1)
                }
                OutlinedButton(onClick={onLogin(RsRole.TRAINER)},modifier=Modifier.fillMaxWidth()){
                    Text(rsT(lang,"trainer_preview"),fontSize=adaptiveLabelSp(rsT(lang,"trainer_preview"),12f).sp,maxLines=1)
                }
                if(status.isNotBlank())Text(status,color=Color.White,fontSize=10.sp)
            }
        }

        Surface(
            shape=RoundedCornerShape(20.dp),
            color=c.panel.copy(alpha=(formOpacity*.92f).coerceIn(.18f,1f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp)){
                Text(rsEnrollmentT(lang,"build_label"),color=c.bright,fontWeight=FontWeight.Bold)
                Text("Trainer-created accounts · QR invitations · camera/gallery login · adjustable transparent login form.",color=Color.White.copy(alpha=.72f))
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
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        RsBrandedHeaderV21(c,store) {
            Text("♛ ${store.s("brand_header_name","RS KICKBOX")}",color=c.bright,fontSize=20.sp,fontWeight=FontWeight.Black)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                LanguageV21(lang,onLang)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    if(route!=home) OutlinedButton(onClick={onRoute(home)},contentPadding=PaddingValues(horizontal=10.dp)){Text("‹")}
                    OutlinedButton(onClick=onLogout,contentPadding=PaddingValues(horizontal=8.dp)){
                        val logout=rsT(lang,"logout")
                        Text(logout,fontSize=adaptiveLabelSp(logout,10f).sp,maxLines=1)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)){content()}
        RsBrandedFooterV21(c,store)
    }
}

@Composable
private fun LanguageV21(current:RsLang,onSelect:(RsLang)->Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick={open=true},contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)){Text("🌐 ${current.name}",fontSize=11.sp,maxLines=1)}
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            rsLangs.forEach { language -> DropdownMenuItem(text={Text(language.name)},onClick={onSelect(language);open=false}) }
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

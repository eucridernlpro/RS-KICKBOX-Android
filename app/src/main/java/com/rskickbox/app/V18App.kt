package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun RsKickboxV18App() {
    val context = LocalContext.current
    val store = remember { RsStore(context) }
    var role by remember { mutableStateOf<RsRole?>(null) }
    var route by remember { mutableStateOf("home") }
    var lang by remember { mutableStateOf(rsLangs.firstOrNull { it.code == store.s("lang", "en") } ?: rsLangs.first()) }
    var theme by remember { mutableStateOf(runCatching { RsTheme.valueOf(store.s("theme", "ELITE_GOLD")) }.getOrDefault(RsTheme.ELITE_GOLD)) }
    var toolsOpen by remember { mutableStateOf(false) }
    var toolPage by remember { mutableStateOf("hub") }
    val c = paletteFor(theme)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = c.bright,
            secondary = c.gold,
            background = c.bg,
            surface = c.panel,
            onBackground = c.text,
            onSurface = c.text
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            if (role == null) {
                RsLiveBackground(c, store, BgScope.LOGIN) {
                    RsLoginV18(
                        c = c,
                        lang = lang,
                        onLang = { selected ->
                            lang = selected
                            store.ps("lang", selected.code)
                        },
                        onLogin = { selected ->
                            role = selected
                            route = if (selected == RsRole.TRAINER) "trainer" else "home"
                        }
                    )
                }
            } else {
                val active = role!!
                val scope = if (active == RsRole.TRAINER) BgScope.TRAINER_TRAINING else BgScope.STUDENT_TRAINING
                RsLiveBackground(c, store, scope) {
                    RsShellV18(
                        c = c,
                        role = active,
                        lang = lang,
                        route = route,
                        onLang = { selected ->
                            lang = selected
                            store.ps("lang", selected.code)
                        },
                        onRoute = { selectedRoute -> route = selectedRoute },
                        onLogout = { role = null },
                        content = {
                            when (route) {
                                "home", "trainer" -> RsPremiumDashboardV18(c, active, lang) { route = it }
                                "backgrounds" -> RsBackgroundStudio(c, store)
                                "themes" -> RsThemeStudio(c, theme) { selected ->
                                    theme = selected
                                    store.ps("theme", selected.name)
                                }
                                "voice" -> RsVoiceCoach(c, lang, store)
                                "session" -> RsSessionV18(c)
                                "access" -> RsAccessControl(c, store)
                                "payments" -> RsPaymentCenter(c, store)
                                "members" -> RsMemberManager(c)
                                "classes" -> if (active == RsRole.TRAINER) RsClassManager(c) else RsStudentClasses(c)
                                "attendance" -> RsAttendance(c)
                                "invoices" -> RsInvoices(c)
                                "book" -> if (active == RsRole.TRAINER) RsBookManager(c) else RsTrainerBook(c)
                                "settings" -> if (active == RsRole.TRAINER) RsAdminSettingsV16(c, store) else RsStudentSettingsV16(c, store)
                                "academy" -> RsAcademy(c)
                                "progress" -> RsProgress(c)
                                "challenges" -> RsChallenges(c)
                                "fightcamp" -> RsFightCamp(c)
                                "finance" -> RsFinance(c)
                                "community" -> RsCommunity(c)
                                "media" -> RsTrainingMediaV16(c, store)
                                "music", "music_admin" -> RsSpotifyMusicCenterV19(c, store, active)
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
                                else -> RsScroll(c, route.replaceFirstChar { it.uppercase() }, "RS native module · ${lang.name}") {
                                    RsPanel(c) {
                                        Text("Module ready", color = c.bright, fontWeight = FontWeight.Bold)
                                        Text("This screen keeps the selected premium cinematic visual system and safe mobile layout.", color = c.muted)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (role != null && !toolsOpen) {
                SmallFloatingActionButton(
                    onClick = {
                        toolsOpen = true
                        toolPage = "hub"
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                    containerColor = c.panel,
                    contentColor = c.bright
                ) { Text("⚙") }
            }

            if (toolsOpen) {
                Surface(modifier = Modifier.fillMaxSize(), color = c.bg) {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RsPanel(c) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("RS PRODUCTION TOOLS", color = c.bright, fontWeight = FontWeight.Black)
                                    Text("v0.19 premium music + dashboard preview", color = c.muted)
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (toolPage == "hub") toolsOpen = false
                                        else toolPage = "hub"
                                    }
                                ) { Text(if (toolPage == "hub") "Close" else "Back") }
                            }
                        }
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            when (toolPage) {
                                "security" -> RsAccountSecurityV17(c, store)
                                "onboarding" -> RsOnboardingV17(c, store)
                                "sync" -> RsSyncStatusV17(c, store)
                                "backend" -> RsBackendReadinessV17(c)
                                else -> RsProductionHubV18(c) { toolPage = it }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RsLoginV18(c: RsPalette, lang: RsLang, onLang: (RsLang) -> Unit, onLogin: (RsRole) -> Unit) {
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var pass by remember { mutableStateOf("preview123") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("♛ RS KICKBOX", color = c.bright, fontSize = 31.sp, fontWeight = FontWeight.Black)
        RsLanguageButtonV18(lang, onLang)
        Text("Premium cinematic kickboxing", color = c.text, style = MaterialTheme.typography.headlineMedium)
        Text("TRAIN · LEARN · CONNECT · GROW", color = c.muted, fontSize = 11.sp)
        RsPanel(c) {
            Text("MEMBER ACCESS", color = c.bright, fontWeight = FontWeight.Bold)
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = { onLogin(RsRole.STUDENT) }, modifier = Modifier.fillMaxWidth()) { Text("Student preview") }
            OutlinedButton(onClick = { onLogin(RsRole.TRAINER) }, modifier = Modifier.fillMaxWidth()) { Text("Trainer / Admin preview") }
        }
        RsPanel(c) {
            Text("v0.19 PREMIUM BUILD", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Larger premium dashboard cards plus shared Spotify music integration for students and trainers.", color = c.muted)
        }
    }
}

@Composable
private fun RsShellV18(
    c: RsPalette,
    role: RsRole,
    lang: RsLang,
    route: String,
    onLang: (RsLang) -> Unit,
    onRoute: (String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val home = if (role == RsRole.TRAINER) "trainer" else "home"
    Column(Modifier.fillMaxSize().padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        RsPanel(c) {
            Text("♛ RS KICKBOX", color = c.bright, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RsLanguageButtonV18(lang, onLang)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (route != home) {
                        OutlinedButton(onClick = { onRoute(home) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("‹") }
                    }
                    OutlinedButton(onClick = onLogout, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Log out", fontSize = 10.sp) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

@Composable
private fun RsSessionV18(c: RsPalette) {
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("WORK") }
    var sec by remember { mutableIntStateOf(120) }
    var round by remember { mutableIntStateOf(1) }
    LaunchedEffect(running, sec, phase, round) {
        if (running && sec > 0) {
            delay(1000)
            sec--
        } else if (running && sec == 0) {
            if (phase == "WORK") {
                phase = "REST"
                sec = 45
            } else if (round < 5) {
                round++
                phase = "WORK"
                sec = 120
            } else {
                running = false
                phase = "COMPLETE"
            }
        }
    }
    RsScroll(c, "Session Player", "Five-round training flow with 2:00 work and 0:45 rest phases.") {
        RsPanel(c) {
            Text("ROUND $round / 5 · $phase", color = c.bright, fontWeight = FontWeight.Bold)
            Text("%d:%02d".format(sec / 60, sec % 60), color = c.bright, fontSize = 50.sp, fontWeight = FontWeight.Black)
            Text(if (phase == "REST") "Breathe · reset stance · stay composed" else "Jab · Cross · Low Kick", color = c.text)
            Button(onClick = {
                if (phase == "COMPLETE") {
                    round = 1
                    phase = "WORK"
                    sec = 120
                }
                running = !running
            }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = {
                if (round < 5) round++
                phase = "WORK"
                sec = 120
                running = false
            }, modifier = Modifier.fillMaxWidth()) { Text("Next round") }
            OutlinedButton(onClick = {
                round = 1
                phase = "WORK"
                sec = 120
                running = false
            }, modifier = Modifier.fillMaxWidth()) { Text("Reset session") }
        }
    }
}

@Composable
private fun RsLanguageButtonV18(current: RsLang, onSelect: (RsLang) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Text("🌐 ${current.name}", fontSize = 11.sp, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            rsLangs.forEach { language ->
                DropdownMenuItem(text = { Text(language.name) }, onClick = {
                    onSelect(language)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun RsProductionHubV18(c: RsPalette, onOpen: (String) -> Unit) {
    RsScroll(c, "Production Hardening", "Preview-only controls used while final backend and release services are connected.") {
        val items = listOf(
            Triple("ACCOUNT & SECURITY", "App lock, biometric preference, password and session controls.", "security"),
            Triple("MEMBER ONBOARDING", "Test the polished first-launch member setup.", "onboarding"),
            Triple("DATA & SYNC", "See local preview data versus backend-required services.", "sync"),
            Triple("PRODUCTION INTEGRATION", "Auth, database, media, push, AI, payments and signing readiness.", "backend")
        )
        items.forEach { (title, body, route) ->
            RsPanel(c) {
                Text(title, color = c.bright, fontWeight = FontWeight.Bold)
                Text(body, color = c.muted)
                Button(onClick = { onOpen(route) }, modifier = Modifier.fillMaxWidth()) { Text("Open") }
            }
        }
    }
}

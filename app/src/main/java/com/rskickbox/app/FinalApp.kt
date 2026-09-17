package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val RsBlack = Color(0xFF050505)
private val RsPanel = Color(0xFF15120D)
private val RsPanel2 = Color(0xFF211A10)
private val RsGold = Color(0xFFC08A24)
private val RsGoldLight = Color(0xFFF0CF79)
private val RsCream = Color(0xFFF6F0E4)
private val RsMuted = Color(0xFFB8AD98)
private val RsGreen = Color(0xFF67C587)
private val RsRed = Color(0xFFE56F6F)

private enum class Role { STUDENT, TRAINER }
private enum class Lang { EN, NL }
private enum class Group { STUDENT, ADMIN }

private data class Feature(
    val id: String,
    val en: String,
    val nl: String,
    val group: Group,
    val icon: String = "♛"
)

private val features = listOf(
    Feature("dashboard", "Dashboard", "Dashboard", Group.STUDENT),
    Feature("academy", "RS Academy", "RS Academy", Group.STUDENT),
    Feature("ai", "AI Coach", "AI Coach", Group.STUDENT),
    Feature("train", "Train Anywhere", "Overal Trainen", Group.STUDENT),
    Feature("session", "Session Player", "Sessie Speler", Group.STUDENT),
    Feature("compare", "Technique Compare", "Techniek Vergelijken", Group.STUDENT),
    Feature("classes", "Classes & Events", "Lessen & Events", Group.STUDENT),
    Feature("community", "Community", "Community", Group.STUDENT),
    Feature("progress", "Progress & Profile", "Voortgang & Profiel", Group.STUDENT),
    Feature("homework", "Homework", "Huiswerk", Group.STUDENT),
    Feature("notifications", "Notifications", "Meldingen", Group.STUDENT),
    Feature("saved", "Saved & Favorites", "Opgeslagen & Favorieten", Group.STUDENT),
    Feature("history", "Training History", "Trainingsgeschiedenis", Group.STUDENT),
    Feature("challenges", "Challenges & Leaderboards", "Uitdagingen & Ranglijst", Group.STUDENT),
    Feature("fightcamp", "Fight Camp", "Fight Camp", Group.STUDENT),
    Feature("finance", "Membership & Finance", "Lidmaatschap & Financiën", Group.STUDENT),
    Feature("profile", "My Profile", "Mijn Profiel", Group.STUDENT),
    Feature("media", "Training Media", "Trainingsmedia", Group.STUDENT),
    Feature("settings", "Settings & Privacy", "Instellingen & Privacy", Group.STUDENT),
    Feature("book", "Trainer Book", "Boek van de Trainer", Group.STUDENT),
    Feature("vault", "Knowledge Vault", "Kennisbank", Group.STUDENT),
    Feature("support", "Support & Documents", "Support & Documenten", Group.STUDENT),
    Feature("private", "Private Lessons", "Privélessen", Group.STUDENT),

    Feature("admin", "Trainer Dashboard", "Trainer Dashboard", Group.ADMIN),
    Feature("attendance", "Attendance Manager", "Aanwezigheidsbeheer", Group.ADMIN),
    Feature("members", "Student Manager", "Ledenbeheer", Group.ADMIN),
    Feature("invoices", "Invoices & Payments", "Facturen & Betalingen", Group.ADMIN),
    Feature("homeworkadmin", "Homework Manager", "Huiswerkbeheer", Group.ADMIN),
    Feature("musicadmin", "Music Manager", "Muziekbeheer", Group.ADMIN),
    Feature("lessonadmin", "Lesson Editor", "Leseditor", Group.ADMIN),
    Feature("classadmin", "Class Manager", "Lesbeheer", Group.ADMIN),
    Feature("notifyadmin", "Notification Composer", "Meldingen Opstellen", Group.ADMIN),
    Feature("plansadmin", "Membership Plans", "Lidmaatschapsplannen", Group.ADMIN),
    Feature("progressadmin", "Progress Manager", "Voortgangsbeheer", Group.ADMIN),
    Feature("assessmentsadmin", "Coach Assessments", "Coachbeoordelingen", Group.ADMIN),
    Feature("eventsadmin", "Event Manager", "Eventbeheer", Group.ADMIN),
    Feature("scheduleadmin", "Weekly Schedule Manager", "Weekplanning Beheer", Group.ADMIN),
    Feature("attendanceflow", "QR Attendance", "QR Aanwezigheid", Group.ADMIN),
    Feature("bookadmin", "Book Manager", "Boekbeheer", Group.ADMIN),
    Feature("landingadmin", "Landing Page Manager", "Landingpage Beheer", Group.ADMIN),
    Feature("sessionadmin", "Session Builder", "Sessie Bouwer", Group.ADMIN),
    Feature("challengesadmin", "Challenge Manager", "Uitdagingenbeheer", Group.ADMIN),
    Feature("fightcampadmin", "Fight Camp Manager", "Fight Camp Beheer", Group.ADMIN)
)

@Composable
fun RsKickboxFinalApp() {
    var role by remember { mutableStateOf<Role?>(null) }
    var route by remember { mutableStateOf("dashboard") }
    var lang by remember { mutableStateOf(Lang.EN) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RsGoldLight,
            secondary = RsGold,
            background = RsBlack,
            surface = RsPanel,
            onPrimary = Color(0xFF1A1306),
            onBackground = RsCream,
            onSurface = RsCream
        )
    ) {
        if (role == null) {
            LoginScreen(lang, { lang = it }) {
                role = it
                route = if (it == Role.TRAINER) "admin" else "dashboard"
            }
        } else {
            AppShell(
                role = role!!,
                route = route,
                lang = lang,
                onRoute = { route = it },
                onLanguage = { lang = it },
                onLogout = { role = null }
            )
        }
    }
}

@Composable
private fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RsBlack, Color(0xFF090806), Color(0xFF130E07))))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        content = content
    )
}

@Composable
private fun LoginScreen(lang: Lang, onLanguage: (Lang) -> Unit, onLogin: (Role) -> Unit) {
    val nl = lang == Lang.NL
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var password by remember { mutableStateOf("preview123") }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderBrand(lang, onLanguage)
            Text(
                if (nl) "Premium kickboksplatform" else "Premium kickboxing platform",
                color = RsCream,
                style = MaterialTheme.typography.headlineMedium
            )
            Text("TRAIN · LEARN · CONNECT · GROW", color = RsMuted, fontSize = 11.sp, letterSpacing = 1.8.sp)

            GoldCard {
                Eyebrow(if (nl) "Leden toegang" else "Member access")
                Text(if (nl) "Welkom bij de RS-familie." else "Welcome to the RS family.", color = RsGoldLight, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text(if (nl) "Wachtwoord" else "Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onLogin(Role.STUDENT) }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Leerling login" else "Student login") }
                OutlinedButton(onClick = { onLogin(Role.TRAINER) }, modifier = Modifier.fillMaxWidth()) { Text("Trainer / Admin") }
            }

            GoldCard {
                Eyebrow(if (nl) "Boek van de trainer" else "Trainer book spotlight")
                Text("Van Stilte Naar Strijd", color = RsGoldLight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Kickboksen, karakter en de weg van basis naar beheersing", color = RsCream)
                Text(if (nl) "Techniek, discipline, respect, karakter en progressie in één RS-leerlijn." else "Technique, discipline, respect, character and progression in one RS learning path.", color = RsMuted)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeaderBrand(lang: Lang, onLanguage: (Lang) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("♛ RS KICKBOX", color = RsGoldLight, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { LanguageSwitch(lang, onLanguage) }
    }
}

@Composable
private fun AppShell(
    role: Role,
    route: String,
    lang: Lang,
    onRoute: (String) -> Unit,
    onLanguage: (Lang) -> Unit,
    onLogout: () -> Unit
) {
    val home = if (role == Role.TRAINER) "admin" else "dashboard"
    val current = features.firstOrNull { it.id == route }

    AppBackground {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .36f)),
                colors = CardDefaults.cardColors(containerColor = RsPanel.copy(alpha = .98f))
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("♛ RS KICKBOX", color = RsGoldLight, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (role == Role.TRAINER) "TRAINER / ADMIN" else "STUDENT", color = RsMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageSwitch(lang, onLanguage)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (route != home) {
                                OutlinedButton(
                                    onClick = { onRoute(home) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, RsGold.copy(alpha = .48f)),
                                    modifier = Modifier.heightIn(min = 38.dp)
                                ) { Text("‹", color = RsGoldLight, fontSize = 18.sp) }
                            }
                            OutlinedButton(
                                onClick = onLogout,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .58f)),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) { Text(if (lang == Lang.NL) "Uitloggen" else "Log out", color = RsGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    if (route != home && current != null) {
                        Text(if (lang == Lang.NL) current.nl else current.en, color = RsCream, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                ScreenRouter(role, route, lang, onRoute)
            }
        }
    }
}

@Composable
private fun ScreenRouter(role: Role, route: String, lang: Lang, onRoute: (String) -> Unit) {
    when (route) {
        "dashboard", "admin" -> DashboardScreen(role, lang, onRoute)
        "academy" -> AcademyScreen(lang)
        "ai" -> AiCoachScreen(lang)
        "session" -> SessionPlayerScreen(lang)
        "classes" -> ClassesScreen(lang)
        "community" -> CommunityScreen(lang)
        "progress" -> ProgressScreen(lang)
        "challenges" -> ChallengesScreen(lang)
        "fightcamp" -> FightCampScreen(lang)
        "finance" -> FinanceScreen(lang)
        "book", "bookadmin" -> BookScreen(lang, route == "bookadmin")
        else -> FunctionalModuleScreen(route, lang)
    }
}

@Composable
private fun DashboardScreen(role: Role, lang: Lang, onRoute: (String) -> Unit) {
    val nl = lang == Lang.NL
    val items = features.filter {
        if (role == Role.TRAINER) it.group == Group.ADMIN && it.id != "admin"
        else it.group == Group.STUDENT && it.id != "dashboard"
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldCard {
            Eyebrow(if (role == Role.TRAINER) "Trainer Control Center" else if (nl) "RS live dashboard" else "RS live dashboard")
            Text(
                if (role == Role.TRAINER) {
                    if (nl) "Beheer het complete RS-platform binnen één veilige mobiele layout." else "Run the complete RS platform inside one safe mobile layout."
                } else {
                    if (nl) "Train, leer, boek en volg je voortgang binnen RS KICKBOX." else "Train, learn, book and track your progress inside RS KICKBOX."
                },
                color = RsGoldLight,
                style = MaterialTheme.typography.titleLarge
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric(if (role == Role.TRAINER) if (nl) "Leden" else "Members" else "Level", if (role == Role.TRAINER) "124" else "3", Modifier.weight(1f))
                MiniMetric(if (role == Role.TRAINER) "Revenue" else "XP", if (role == Role.TRAINER) "€4,850" else "12,480", Modifier.weight(1f))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 132.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            items(items) { feature ->
                FeatureCard(feature, lang) { onRoute(feature.id) }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, lang: Lang, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, RsGold.copy(alpha = .34f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11100E))
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(feature.icon, color = RsGoldLight, fontSize = 21.sp)
            Text(if (lang == Lang.NL) feature.nl else feature.en, color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (lang == Lang.NL) "Open module" else "Open module", color = RsMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AcademyScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var completed by remember { mutableStateOf(setOf("Jab Fundamentals")) }
    ScreenColumn("RS Academy", if (nl) "Techniekbibliotheek van basis naar beheersing." else "Technique library from fundamentals to mastery.") {
        val lessons = listOf(
            Triple("Jab Fundamentals", "Beginner", if (nl) "Afstand, dekking en herstel" else "Distance, guard and recovery"),
            Triple("Roundhouse Kick", "Intermediate", if (nl) "Heuprotatie, balans en terugkeer" else "Hip rotation, balance and return"),
            Triple("Defense & Counters", "Intermediate", if (nl) "Blokken, ontwijken en reageren" else "Blocks, slips and responses"),
            Triple("Footwork Flow", "All Levels", if (nl) "Hoeken, exits en balans" else "Angles, exits and balance"),
            Triple("Combination Builder", "All Levels", if (nl) "Technieken vloeiend verbinden" else "Connect techniques into flowing combinations")
        )
        lessons.forEach { (name, level, desc) ->
            GoldCard {
                Eyebrow(level)
                Text(name, color = RsGoldLight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(desc, color = RsMuted)
                Button(onClick = { completed = completed + name }, enabled = name !in completed, modifier = Modifier.fillMaxWidth()) {
                    Text(if (name in completed) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Open les" else "Open lesson")
                }
            }
        }
    }
}

@Composable
private fun AiCoachScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var analyzed by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    ScreenColumn("AI Coach", if (nl) "Techniek analyseren, corrigeren en verbeteren." else "Analyze, correct and improve technique.") {
        GoldCard {
            Eyebrow("AI Technique Coach")
            Text(if (nl) "Upload een techniekclip en ontvang corrigerende feedback." else "Upload a technique clip and receive corrective feedback.", color = RsCream)
            Button(onClick = { analyzed = true }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Start preview-analyse" else "Start preview analysis") }
        }
        if (analyzed) GoldCard {
            Eyebrow(if (nl) "Analyse resultaat" else "Analysis result")
            Text("84%", color = RsGoldLight, fontSize = 38.sp, fontWeight = FontWeight.Black)
            LinearProgressIndicator(progress = { .84f }, modifier = Modifier.fillMaxWidth())
            Text(if (nl) "Achterste hand zakt na de low kick. Herstel eerst de dekking." else "Rear hand drops after the low kick. Restore guard first.", color = RsMuted)
        }
        GoldCard {
            OutlinedTextField(question, { question = it }, label = { Text(if (nl) "Stel een trainingsvraag" else "Ask a training question") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { question = "" }, enabled = question.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Vraag RS AI" else "Ask RS AI") }
        }
    }
}

@Composable
private fun SessionPlayerScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(120) }
    var round by remember { mutableIntStateOf(1) }
    LaunchedEffect(running, seconds) {
        if (running && seconds > 0) { delay(1000); seconds-- }
        if (seconds == 0) running = false
    }
    ScreenColumn(if (nl) "Sessie speler" else "Session Player", if (nl) "Rondetimer en combo-cues." else "Round timer and combo cues.") {
        GoldCard {
            Eyebrow("Round $round / 5")
            Text("%d:%02d".format(seconds / 60, seconds % 60), color = RsGoldLight, fontSize = 50.sp, fontWeight = FontWeight.Black)
            Text("Jab · Cross · Low Kick", color = RsMuted)
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) { Text(if (running) if (nl) "Pauze" else "Pause" else if (nl) "Start" else "Start") }
            OutlinedButton(onClick = { round = (round + 1).coerceAtMost(5); seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Volgende ronde" else "Next round") }
            TextButton(onClick = { round = 1; seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Reset" else "Reset") }
        }
    }
}

@Composable
private fun ClassesScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var booked by remember { mutableStateOf(setOf("Technique & Pads")) }
    ScreenColumn(if (nl) "Lessen & Events" else "Classes & Events", if (nl) "Boek trainingen en beheer je planning." else "Book training and manage your schedule.") {
        listOf(
            Triple("Technique & Pads", "Today · 19:00", "12/16"),
            Triple("Kickboxing Fundamentals", "Thu · 18:30", "9/16"),
            Triple("Advanced Sparring", "Fri · 20:00", "14/14")
        ).forEach { (name, time, capacity) ->
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text("$time · $capacity", color = RsMuted)
                if (capacity == "14/14") {
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Wachtlijst" else "Join waitlist") }
                } else {
                    Button(onClick = { booked = if (name in booked) booked - name else booked + name }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (name in booked) if (nl) "Geboekt ✓" else "Booked ✓" else if (nl) "Boek les" else "Book class")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var liked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    ScreenColumn("RS Community", if (nl) "Teamupdates, clubnieuws en groepschat." else "Team updates, club news and group chat.") {
        GoldCard {
            Eyebrow("Trainer update")
            Text(if (nl) "Sterke training vanavond. Blijf technisch scherp en help elkaar groeien." else "Strong session tonight. Stay technically sharp and help each other grow.", color = RsCream)
            TextButton(onClick = { liked = !liked }) { Text(if (liked) "♥ 25" else "♡ 24") }
        }
        GoldCard {
            Text("RS Fighters · 18 members", color = RsGoldLight, fontWeight = FontWeight.Bold)
            Text("Ricardo: Pads ready for tonight. Bring focus. 👊", color = RsMuted)
            OutlinedTextField(message, { message = it }, label = { Text(if (nl) "Bericht" else "Message") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { message = "" }, enabled = message.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Verstuur" else "Send") }
        }
    }
}

@Composable
private fun ProgressScreen(lang: Lang) {
    val nl = lang == Lang.NL
    ScreenColumn(if (nl) "Voortgang & Profiel" else "Progress & Profile", if (nl) "Jouw ontwikkeling van basis naar beheersing." else "Your development from fundamentals to mastery.") {
        GoldCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric("Level", "3", Modifier.weight(1f))
                MiniMetric("XP", "12,480", Modifier.weight(1f))
            }
            LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth())
            Text(if (nl) "520 XP tot level 4" else "520 XP to level 4", color = RsMuted)
        }
        listOf("Punches" to .82f, "Kicks" to .71f, "Defense" to .66f, "Footwork" to .74f, "Combinations" to .63f, "Conditioning" to .79f).forEach { (skill, score) ->
            GoldCard {
                Text(skill, color = RsGoldLight, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth())
                Text("${(score * 100).toInt()}%", color = RsMuted)
            }
        }
    }
}

@Composable
private fun ChallengesScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var done by remember { mutableStateOf(setOf<String>()) }
    ScreenColumn(if (nl) "Uitdagingen & Ranglijst" else "Challenges & Leaderboards", if (nl) "Verdien XP met technische en mentale doelen." else "Earn XP with technical and mindset goals.") {
        listOf("1,000 Kicks Challenge" to 500, "7-Day Discipline Streak" to 350, "Combo Master" to 400).forEach { (name, xp) ->
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text("+$xp XP", color = RsMuted)
                Button(onClick = { done = done + name }, enabled = name !in done, modifier = Modifier.fillMaxWidth()) { Text(if (name in done) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Markeer voltooid" else "Mark complete") }
            }
        }
        GoldCard {
            Eyebrow(if (nl) "Maandranglijst" else "Monthly leaderboard")
            Text("1. Mila · 18,420 XP", color = RsCream)
            Text("2. Alex · 12,480 XP", color = RsGoldLight, fontWeight = FontWeight.Bold)
            Text("3. Noah · 10,950 XP", color = RsCream)
        }
    }
}

@Composable
private fun FightCampScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var week by remember { mutableIntStateOf(3) }
    ScreenColumn("Fight Camp", if (nl) "Wedstrijdvoorbereiding, herstel en strategie." else "Fight preparation, recovery and strategy.") {
        GoldCard {
            Eyebrow(if (nl) "Camp voortgang" else "Camp progress")
            Text("TBD · Club Match", color = RsGoldLight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("24 Oct 2026 · -75 kg", color = RsMuted)
            LinearProgressIndicator(progress = { week / 6f }, modifier = Modifier.fillMaxWidth())
            Text("Week $week / 6 · Readiness 87%", color = RsCream)
            Button(onClick = { if (week < 6) week++ }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Volgende week" else "Advance week") }
        }
        listOf("Foundation & Analysis", "Intensity & Sparring", "Taper & Strategy", "Fight Week").forEachIndexed { index, phase ->
            GoldCard { Text("${index + 1}. $phase", color = if (index + 1 <= week) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FinanceScreen(lang: Lang) {
    val nl = lang == Lang.NL
    ScreenColumn(if (nl) "Lidmaatschap & Financiën" else "Membership & Finance", if (nl) "Plan, facturen en betaalhistorie." else "Plan, invoices and payment history.") {
        GoldCard {
            Eyebrow(if (nl) "Actief plan" else "Active plan")
            Text("RS PRO", color = RsGoldLight, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("€49 / month", color = RsCream)
            StatusPill(if (nl) "Actief" else "Active", true)
        }
        listOf("September 2026", "August 2026", "July 2026").forEach { month ->
            GoldCard { Text(month, color = RsCream); Text("€49 · ${if (nl) "Betaald" else "Paid"}", color = RsGreen) }
        }
    }
}

@Composable
private fun BookScreen(lang: Lang, admin: Boolean) {
    val nl = lang == Lang.NL
    val uri = LocalUriHandler.current
    var promo by remember { mutableStateOf(true) }
    ScreenColumn(if (admin) "Book Manager" else "Van Stilte Naar Strijd", "Kickboksen, karakter en de weg van basis naar beheersing") {
        GoldCard {
            Eyebrow(if (admin) "RS Book Manager" else if (nl) "Boek van de trainer" else "Trainer book")
            Text("Van Stilte Naar Strijd", color = RsGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(if (nl) "Van houding, stoten, trappen en knieën tot verdediging, combinaties, herstel, sparren en wedstrijdvoorbereiding." else "From stance, punches, kicks and knees to defense, combinations, recovery, sparring and fight preparation.", color = RsMuted)
            Button(onClick = { uri.openUri("https://www.amazon.nl/s?k=Van+Stilte+Naar+Strijd") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Zoek op Amazon" else "Search on Amazon") }
        }
        GoldCard {
            Eyebrow("Member extras")
            listOf("Sample Chapter", "Trainer Video", "Related Technique Lessons", "Mindset & Discipline Bonus", "Technique Drill PDF").forEach { Text("♛ $it", color = RsCream) }
        }
        if (admin) GoldCard {
            SettingToggle(if (nl) "Promotie actief" else "Promotion active", promo) { promo = it }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Boekgegevens bewerken" else "Edit book details") }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Promo media beheren" else "Manage promo media") }
        }
    }
}

@Composable
private fun FunctionalModuleScreen(route: String, lang: Lang) {
    val feature = features.firstOrNull { it.id == route }
    val nl = lang == Lang.NL
    val title = if (nl) feature?.nl else feature?.en
    var actionDone by remember(route) { mutableStateOf(false) }
    var toggle by remember(route) { mutableStateOf(true) }

    val details = moduleDetails(route, nl)
    ScreenColumn(title ?: route, details.first) {
        details.second.forEach { line ->
            GoldCard {
                Text(line.first, color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text(line.second, color = RsMuted)
            }
        }
        GoldCard {
            Eyebrow(if (nl) "Actieve module" else "Active module")
            SettingToggle(if (nl) "Ingeschakeld" else "Enabled", toggle) { toggle = it }
            Button(onClick = { actionDone = !actionDone }, modifier = Modifier.fillMaxWidth()) {
                Text(if (actionDone) if (nl) "Opgeslagen ✓" else "Saved ✓" else if (nl) "Test actie" else "Test action")
            }
            Text(if (nl) "Deze previewactie werkt lokaal. Clouddata en echte accounts worden in de productiebackend gekoppeld." else "This preview action works locally. Cloud data and real accounts are connected in the production backend.", color = RsMuted, fontSize = 11.sp)
        }
    }
}

private fun moduleDetails(route: String, nl: Boolean): Pair<String, List<Pair<String, String>>> {
    fun t(en: String, nlText: String) = if (nl) nlText else en
    return when (route) {
        "train" -> t("Home training mode with guided rounds and workout generation.", "Thuistraining met begeleide rondes en workoutgenerator.") to listOf(
            "20 min Technique" to t("Jab, cross, low kick, defense", "Jab, cross, low kick, verdediging"),
            "30 min Conditioning" to t("Intervals, core and recovery", "Intervallen, core en herstel")
        )
        "compare" -> t("Compare two techniques side by side.", "Vergelijk twee technieken naast elkaar.") to listOf("Jab vs Cross" to t("Guard, distance and rotation", "Dekking, afstand en rotatie"), "Low Kick vs Body Kick" to t("Target, hip angle and return", "Doel, heuphoek en terugkeer"))
        "homework" -> t("Assignments from your trainer with media submission.", "Opdrachten van je trainer met media-inzending.") to listOf("Defense Drill" to t("Due Friday · video required", "Vrijdag klaar · video vereist"), "Footwork Flow" to t("3 x 2 minutes", "3 x 2 minuten"))
        "notifications" -> t("RS alerts, class updates and coach messages.", "RS-meldingen, lesupdates en coachberichten.") to listOf("Class reminder" to t("Technique & Pads starts at 19:00", "Technique & Pads start om 19:00"), "Coach note" to t("New feedback available", "Nieuwe feedback beschikbaar"))
        "saved" -> t("Your saved lessons, drills and book extras.", "Jouw opgeslagen lessen, drills en boekextra's.") to listOf("Roundhouse Kick" to "RS Academy", "Mindset & Discipline" to "Van Stilte Naar Strijd")
        "history" -> t("Recent sessions, AI reviews and personal records.", "Recente sessies, AI-reviews en persoonlijke records.") to listOf("16 Sep" to t("Technique & Pads · 88%", "Technique & Pads · 88%"), "14 Sep" to t("Home session · 5 rounds", "Thuissessie · 5 rondes"))
        "profile" -> t("Member identity, goals and emergency contact.", "Ledenprofiel, doelen en noodcontact.") to listOf("Alex de Vries" to "Level 3 · 12,480 XP", t("Goal", "Doel") to t("Improve defense and conditioning", "Verdediging en conditie verbeteren"))
        "media" -> t("Training photos, videos and coach review clips.", "Trainingsfoto's, video's en coachreviewclips.") to listOf("Technique Review" to "00:24 · video", "Pad Session" to "8 photos")
        "settings" -> t("Language, notifications, privacy and data controls.", "Taal, meldingen, privacy en gegevensbeheer.") to listOf(t("Language", "Taal") to "EN / NL", t("Data controls", "Gegevensbeheer") to t("Export and account deletion flow", "Export en accountverwijdering"))
        "vault" -> t("RS knowledge, guides and premium learning resources.", "RS-kennis, gidsen en premium leermateriaal.") to listOf("Fight Preparation" to t("Checklist & strategy", "Checklist & strategie"), "Recovery" to t("Sleep, hydration and load", "Slaap, hydratatie en belasting"))
        "support" -> t("Help center, waivers, privacy and member documents.", "Helpcentrum, verklaringen, privacy en ledendocumenten.") to listOf(t("Help Center", "Helpcentrum") to t("FAQs and contact routes", "FAQ's en contactroutes"), t("Documents", "Documenten") to t("Waivers and membership files", "Verklaringen en lidmaatschapsbestanden"))
        "private" -> t("Book private coaching and view trainer availability.", "Boek privécoaching en bekijk trainerbeschikbaarheid.") to listOf("Ricardo" to "Tue 18:00 · Sat 10:00", t("Private lesson", "Privéles") to "60 min")
        "attendance" -> t("Attendance overview and manual check-in.", "Aanwezigheidsoverzicht en handmatige check-in.") to listOf("Technique & Pads" to "12 / 16", "Fundamentals" to "9 / 16")
        "members" -> t("Student roster, levels and attendance.", "Ledenlijst, levels en aanwezigheid.") to listOf("Mila Jansen" to "Level 4 · 96%", "Noah Bakker" to "Level 2 · 84%")
        "invoices" -> t("Invoices, payment status and membership finance.", "Facturen, betaalstatus en lidmaatschapsfinanciën.") to listOf("September" to "€49 · Paid", "Invoice #1048" to "€49 · Sent")
        "homeworkadmin" -> t("Create, assign and review homework.", "Maak, wijs toe en beoordeel huiswerk.") to listOf("Defense Drill" to "8 submitted · 3 review", "Footwork Flow" to "12 assigned")
        "musicadmin" -> t("Manage member-uploaded training music.", "Beheer door leden geüploade trainingsmuziek.") to listOf("Team Warmup" to "03:18", "Pad Round Mix" to "02:45")
        "lessonadmin" -> t("Edit Academy lessons and technique content.", "Bewerk Academy-lessen en techniekcontent.") to listOf("Jab Fundamentals" to "Published", "Defense & Counters" to "Draft")
        "classadmin" -> t("Create classes, capacities and waitlists.", "Maak lessen, capaciteit en wachtlijsten.") to listOf("Technique & Pads" to "12 / 16", "Advanced Sparring" to "14 / 14 · 3 waitlist")
        "notifyadmin" -> t("Compose targeted RS notifications.", "Stel gerichte RS-meldingen op.") to listOf("Audience" to t("All members", "Alle leden"), "Channel" to "Push + in-app")
        "plansadmin" -> t("Manage Basic, Pro and Elite memberships.", "Beheer Basic-, Pro- en Elite-lidmaatschappen.") to listOf("RS BASIC" to "€29", "RS PRO" to "€49", "RS ELITE" to "€79")
        "progressadmin" -> t("Review athlete progress and skill development.", "Bekijk voortgang en vaardigheidsontwikkeling.") to listOf("Alex" to "72% mastery", "Mila" to "84% mastery")
        "assessmentsadmin" -> t("Create coach assessments and scores.", "Maak coachbeoordelingen en scores.") to listOf("Technique" to "8.4 / 10", "Discipline" to "9.1 / 10")
        "eventsadmin" -> t("Create club events and manage attendance.", "Maak clubevents en beheer aanwezigheid.") to listOf("RS Sparring Night" to "24 Oct", "Technique Seminar" to "8 Nov")
        "scheduleadmin" -> t("Manage the trainer weekly schedule.", "Beheer de wekelijkse trainerplanning.") to listOf("Monday" to "18:30 Fundamentals · 20:00 Pads", "Friday" to "20:00 Sparring")
        "attendanceflow" -> t("QR check-in flow for class attendance.", "QR-check-in voor lesaanwezigheid.") to listOf("QR Session" to t("Ready to scan", "Klaar om te scannen"), t("Manual fallback", "Handmatige fallback") to t("Enabled", "Ingeschakeld"))
        "landingadmin" -> t("Manage public RS landing content.", "Beheer publieke RS-landingscontent.") to listOf("Hero" to "Elite Gold", "Book spotlight" to "Van Stilte Naar Strijd")
        "sessionadmin" -> t("Build timed training sessions and round cues.", "Bouw getimede trainingssessies en ronde-cues.") to listOf("5 x 2 min" to "45 sec rest", "Combo cue" to "Jab · Cross · Low Kick")
        "challengesadmin" -> t("Publish challenges, XP and leaderboard rules.", "Publiceer uitdagingen, XP en ranglijstregels.") to listOf("1,000 Kicks" to "+500 XP", "7-Day Discipline" to "+350 XP")
        "fightcampadmin" -> t("Build and monitor athlete fight camps.", "Bouw en volg fight camps van atleten.") to listOf("Alex" to "Week 3 / 6 · 87%", "Mila" to "Week 5 / 8 · 91%")
        else -> t("Native RS KICKBOX module.", "Native RS KICKBOX-module.") to listOf("RS Status" to t("Ready", "Gereed"), "Mobile layout" to t("Safe and scrollable", "Veilig en scrollbaar"))
    }
}

@Composable
private fun ScreenColumn(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(title, color = RsGoldLight, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        Text(subtitle, color = RsMuted, modifier = Modifier.fillMaxWidth())
        content()
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GoldCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .28f)),
        colors = CardDefaults.cardColors(containerColor = RsPanel.copy(alpha = .98f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = RsPanel2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, RsGold.copy(alpha = .26f))) {
        Column(Modifier.padding(10.dp)) {
            Text(label.uppercase(), color = RsMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = RsGoldLight, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text.uppercase(), color = RsGoldLight, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun StatusPill(text: String, good: Boolean) {
    Surface(color = (if (good) RsGreen else RsRed).copy(alpha = .14f), shape = RoundedCornerShape(20.dp)) {
        Text(text, color = if (good) RsGreen else RsRed, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun SettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = RsCream, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun LanguageSwitch(lang: Lang, onLanguage: (Lang) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = RsPanel2, border = BorderStroke(1.dp, RsGold.copy(alpha = .38f))) {
        Row(Modifier.padding(2.dp)) {
            Text("EN", modifier = Modifier.clickable { onLanguage(Lang.EN) }.padding(horizontal = 8.dp, vertical = 5.dp), color = if (lang == Lang.EN) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("NL", modifier = Modifier.clickable { onLanguage(Lang.NL) }.padding(horizontal = 8.dp, vertical = 5.dp), color = if (lang == Lang.NL) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

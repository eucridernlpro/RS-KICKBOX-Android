package com.rskickbox.app

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

private data class Feature(val id: String, val en: String, val nl: String, val group: Group, val icon: String = "♛")
private data class ModuleSpec(
    val titleEn: String,
    val titleNl: String,
    val subtitleEn: String,
    val subtitleNl: String,
    val cards: List<Pair<String, String>>,
    val actionEn: String = "Save",
    val actionNl: String = "Opslaan"
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

private class PreviewStore(context: Context) {
    private val prefs = context.getSharedPreferences("rs_kickbox_preview", Context.MODE_PRIVATE)
    fun getBool(key: String, fallback: Boolean = false) = prefs.getBoolean(key, fallback)
    fun setBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getInt(key: String, fallback: Int = 0) = prefs.getInt(key, fallback)
    fun setInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getString(key: String, fallback: String = "") = prefs.getString(key, fallback) ?: fallback
    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

@Composable
fun RsKickboxFinalApp() {
    val context = LocalContext.current
    val store = remember { PreviewStore(context) }
    var role by remember { mutableStateOf<Role?>(null) }
    var route by remember { mutableStateOf("dashboard") }
    var lang by remember { mutableStateOf(if (store.getString("lang", "EN") == "NL") Lang.NL else Lang.EN) }

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
            LoginScreen(lang, { lang = it; store.setString("lang", it.name) }) {
                role = it
                route = if (it == Role.TRAINER) "admin" else "dashboard"
            }
        } else {
            AppShell(
                role = role!!,
                route = route,
                lang = lang,
                store = store,
                onRoute = { route = it },
                onLanguage = { lang = it; store.setString("lang", it.name) },
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderBrand(lang, onLanguage)
            Text(if (nl) "Premium kickboksplatform" else "Premium kickboxing platform", color = RsCream, style = MaterialTheme.typography.headlineMedium)
            Text("TRAIN · LEARN · CONNECT · GROW", color = RsMuted, fontSize = 11.sp, letterSpacing = 1.8.sp)
            GoldCard {
                Eyebrow(if (nl) "Leden toegang" else "Member access")
                Text(if (nl) "Welkom bij de RS-familie." else "Welcome to the RS family.", color = RsGoldLight, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    password, { password = it }, label = { Text(if (nl) "Wachtwoord" else "Password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
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
        LanguageSwitch(lang, onLanguage)
    }
}

@Composable
private fun AppShell(
    role: Role,
    route: String,
    lang: Lang,
    store: PreviewStore,
    onRoute: (String) -> Unit,
    onLanguage: (Lang) -> Unit,
    onLogout: () -> Unit
) {
    val home = if (role == Role.TRAINER) "admin" else "dashboard"
    val current = features.firstOrNull { it.id == route }
    AppBackground {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .36f)),
                colors = CardDefaults.cardColors(containerColor = RsPanel.copy(alpha = .98f))
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("♛ RS KICKBOX", color = RsGoldLight, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (role == Role.TRAINER) "TRAINER / ADMIN" else "STUDENT", color = RsMuted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        LanguageSwitch(lang, onLanguage)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (route != home) {
                                OutlinedButton(
                                    onClick = { onRoute(home) },
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                    border = BorderStroke(1.dp, RsGold.copy(alpha = .48f)),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) { Text("‹", color = RsGoldLight, fontSize = 17.sp) }
                            }
                            OutlinedButton(
                                onClick = onLogout,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .58f)),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) { Text(if (lang == Lang.NL) "Uitloggen" else "Log out", color = RsGoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    if (route != home && current != null) {
                        Text(if (lang == Lang.NL) current.nl else current.en, color = RsCream, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) { ScreenRouter(role, route, lang, store, onRoute) }
        }
    }
}

@Composable
private fun ScreenRouter(role: Role, route: String, lang: Lang, store: PreviewStore, onRoute: (String) -> Unit) {
    when (route) {
        "dashboard", "admin" -> DashboardScreen(role, lang, onRoute)
        "academy" -> AcademyScreen(lang, store)
        "ai" -> AiCoachScreen(lang)
        "session" -> SessionPlayerScreen(lang)
        "classes" -> ClassesScreen(lang, store)
        "community" -> CommunityScreen(lang)
        "progress" -> ProgressScreen(lang)
        "challenges" -> ChallengesScreen(lang, store)
        "fightcamp" -> FightCampScreen(lang, store)
        "finance" -> FinanceScreen(lang)
        "book", "bookadmin" -> BookScreen(lang, route == "bookadmin", onRoute)
        "settings" -> SettingsScreen(lang, store)
        else -> FunctionalModuleScreen(route, lang, store, onRoute)
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
            Eyebrow(if (role == Role.TRAINER) "Trainer Control Center" else "RS live dashboard")
            Text(
                if (role == Role.TRAINER) {
                    if (nl) "Beheer het complete RS-platform binnen één veilige mobiele layout." else "Run the complete RS platform inside one safe mobile layout."
                } else {
                    if (nl) "Train, leer, boek en volg je voortgang binnen RS KICKBOX." else "Train, learn, book and track your progress inside RS KICKBOX."
                },
                color = RsGoldLight, style = MaterialTheme.typography.titleLarge
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric(if (role == Role.TRAINER) if (nl) "Leden" else "Members" else "Level", if (role == Role.TRAINER) "124" else "3", Modifier.weight(1f))
                MiniMetric(if (role == Role.TRAINER) "Revenue" else "XP", if (role == Role.TRAINER) "€4,850" else "12,480", Modifier.weight(1f))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            items(items) { feature -> FeatureCard(feature, lang) { onRoute(feature.id) } }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, lang: Lang, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RsGold.copy(alpha = .34f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11100E))
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(feature.icon, color = RsGoldLight, fontSize = 20.sp)
            Text(if (lang == Lang.NL) feature.nl else feature.en, color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(if (lang == Lang.NL) "Open module" else "Open module", color = RsMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AcademyScreen(lang: Lang, store: PreviewStore) {
    val nl = lang == Lang.NL
    val lessons = listOf(
        Triple("jab", "Jab Fundamentals", if (nl) "Afstand, dekking en herstel" else "Distance, guard and recovery"),
        Triple("roundhouse", "Roundhouse Kick", if (nl) "Heuprotatie, balans en terugkeer" else "Hip rotation, balance and return"),
        Triple("defense", "Defense & Counters", if (nl) "Blokken, ontwijken en reageren" else "Blocks, slips and responses"),
        Triple("footwork", "Footwork Flow", if (nl) "Hoeken, exits en balans" else "Angles, exits and balance"),
        Triple("combos", "Combination Builder", if (nl) "Vloeiende combinaties" else "Flowing combinations")
    )
    ScrollScreen("RS Academy", if (nl) "Techniekbibliotheek van basis naar beheersing." else "Technique library from fundamentals to mastery.") {
        lessons.forEach { (id, name, desc) ->
            var done by remember { mutableStateOf(store.getBool("lesson_$id")) }
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(desc, color = RsMuted)
                Button(
                    onClick = { done = true; store.setBool("lesson_$id", true) }, enabled = !done,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (done) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Markeer voltooid" else "Mark complete") }
            }
        }
    }
}

@Composable
private fun AiCoachScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var analyzed by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    ScrollScreen("AI Coach", if (nl) "Techniek analyseren, corrigeren en verbeteren." else "Analyze, correct and improve technique.") {
        GoldCard {
            Eyebrow("AI Technique Coach")
            Text(if (nl) "Gebruik de preview-analyse om de AI-flow te testen." else "Use the preview analysis to test the AI flow.", color = RsCream)
            Button(onClick = { analyzed = true }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Start preview-analyse" else "Start preview analysis") }
        }
        if (analyzed) GoldCard {
            Text("Technique score 84%", color = RsGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Black)
            LinearProgressIndicator(progress = { .84f }, modifier = Modifier.fillMaxWidth())
            Text(if (nl) "Achterste hand zakt na de low kick. Herstel eerst je dekking voordat je snelheid verhoogt." else "Rear hand drops after the low kick. Restore your guard before increasing speed.", color = RsMuted)
        }
        GoldCard {
            OutlinedTextField(question, { question = it }, label = { Text(if (nl) "Trainingsvraag" else "Training question") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { reply = if (nl) "Focus op rustige herhalingen, balans en dekking tussen elke techniek." else "Focus on controlled repetitions, balance and guard recovery between every technique." }, enabled = question.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Vraag RS AI" else "Ask RS AI") }
            if (reply.isNotBlank()) Text(reply, color = RsGoldLight)
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
        if (running && seconds == 0) running = false
    }
    ScrollScreen(if (nl) "Sessie speler" else "Session Player", if (nl) "Rondetimer, combo-cues en trainingsflow." else "Round timer, combo cues and training flow.") {
        GoldCard {
            Eyebrow("Round $round / 5")
            Text("%d:%02d".format(seconds / 60, seconds % 60), color = RsGoldLight, fontSize = 52.sp, fontWeight = FontWeight.Black)
            Text("Jab – Cross – Low Kick", color = RsMuted)
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) { Text(if (running) if (nl) "Pauze" else "Pause" else if (nl) "Start" else "Start") }
            OutlinedButton(onClick = { if (round < 5) round++; seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Volgende ronde" else "Next round") }
            TextButton(onClick = { round = 1; seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Reset sessie" else "Reset session") }
        }
    }
}

@Composable
private fun ClassesScreen(lang: Lang, store: PreviewStore) {
    val nl = lang == Lang.NL
    val classes = listOf(
        Triple("pads", "Technique & Pads", "Today · 19:00 · 12/16"),
        Triple("fundamentals", "Kickboxing Fundamentals", "Thu · 18:30 · 9/16"),
        Triple("sparring", "Advanced Sparring", "Fri · 20:00 · FULL")
    )
    ScrollScreen(if (nl) "Lessen & Events" else "Classes & Events", if (nl) "Boek je training en volg je planning." else "Book your training and follow your schedule.") {
        classes.forEach { (id, name, info) ->
            var booked by remember { mutableStateOf(store.getBool("class_$id")) }
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(info, color = RsMuted)
                if (id == "sparring") StatusPill(if (nl) "Wachtlijst" else "Waitlist")
                else Button(onClick = { booked = !booked; store.setBool("class_$id", booked) }, modifier = Modifier.fillMaxWidth()) { Text(if (booked) if (nl) "Annuleer boeking" else "Cancel booking" else if (nl) "Boek les" else "Book class") }
            }
        }
    }
}

@Composable
private fun CommunityScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var liked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    ScrollScreen("RS Community", if (nl) "Teamupdates, motivatie en clubnieuws." else "Team updates, motivation and club news.") {
        GoldCard {
            Eyebrow("Trainer update")
            Text(if (nl) "Sterke training vanavond. Blijf technisch scherp en help elkaar groeien." else "Strong session tonight. Stay technically sharp and help each other grow.", color = RsCream)
            TextButton(onClick = { liked = !liked }) { Text(if (liked) "♥ 25" else "♡ 24") }
        }
        GoldCard {
            Text("RS Fighters · 18 members", color = RsGoldLight, fontWeight = FontWeight.Bold)
            Text("Ricardo: Pads ready for tonight. Bring focus. 👊", color = RsMuted)
            OutlinedTextField(message, { message = it; sent = false }, label = { Text(if (nl) "Bericht" else "Message") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { sent = true; message = "" }, enabled = message.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Verstuur" else "Send") }
            if (sent) Text(if (nl) "Bericht verzonden in preview ✓" else "Message sent in preview ✓", color = RsGreen)
        }
    }
}

@Composable
private fun ProgressScreen(lang: Lang) {
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "Voortgang & Profiel" else "Progress & Profile", if (nl) "Jouw ontwikkeling van basis naar beheersing." else "Your development from fundamentals to mastery.") {
        GoldCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniMetric("Level", "3", Modifier.weight(1f)); MiniMetric("XP", "12,480", Modifier.weight(1f)); MiniMetric("Streak", "18d", Modifier.weight(1f))
            }
            LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth())
            Text(if (nl) "520 XP tot level 4" else "520 XP to level 4", color = RsMuted)
        }
        listOf("Punches" to .82f, "Kicks" to .71f, "Defense" to .66f, "Footwork" to .74f, "Combinations" to .63f, "Conditioning" to .79f).forEach { (skill, score) ->
            GoldCard { Text(skill, color = RsGoldLight, fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth()); Text("${(score * 100).toInt()}%", color = RsMuted) }
        }
    }
}

@Composable
private fun ChallengesScreen(lang: Lang, store: PreviewStore) {
    val nl = lang == Lang.NL
    val challenges = listOf("100 clean jabs" to 250, "3 training sessions" to 400, "Defense drill streak" to 300, "Recovery discipline" to 200)
    ScrollScreen(if (nl) "Uitdagingen & Ranglijst" else "Challenges & Leaderboards", if (nl) "Verdien XP met technische en mentale doelen." else "Earn XP with technical and mindset goals.") {
        challenges.forEachIndexed { index, (name, xp) ->
            var done by remember { mutableStateOf(store.getBool("challenge_$index")) }
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text("+$xp XP", color = RsMuted)
                Button(onClick = { done = true; store.setBool("challenge_$index", true) }, enabled = !done, modifier = Modifier.fillMaxWidth()) { Text(if (done) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Markeer voltooid" else "Mark complete") }
            }
        }
        GoldCard { Eyebrow(if (nl) "Maandranglijst" else "Monthly leaderboard"); Text("1. Mila · 4,820 XP\n2. Alex · 4,310 XP\n3. Sara · 4,080 XP", color = RsCream) }
    }
}

@Composable
private fun FightCampScreen(lang: Lang, store: PreviewStore) {
    val nl = lang == Lang.NL
    var week by remember { mutableIntStateOf(store.getInt("fight_week", 3).coerceIn(1, 6)) }
    ScrollScreen("Fight Camp", if (nl) "Gestructureerde wedstrijdvoorbereiding." else "Structured fight preparation.") {
        GoldCard {
            Text("Week $week / 6", color = RsGoldLight, fontSize = 25.sp, fontWeight = FontWeight.Black)
            LinearProgressIndicator(progress = { week / 6f }, modifier = Modifier.fillMaxWidth())
            Text(if (nl) "Doel: techniek, intensiteit, sparring en herstel in balans." else "Goal: balance technique, intensity, sparring and recovery.", color = RsMuted)
            Button(onClick = { if (week < 6) { week++; store.setInt("fight_week", week) } }, enabled = week < 6, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Week afronden" else "Complete week") }
        }
        listOf("Technical sharpness", "Conditioning", "Controlled sparring", "Recovery & mindset").forEach { GoldCard { Text("♛ $it", color = RsCream) } }
    }
}

@Composable
private fun FinanceScreen(lang: Lang) {
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "Lidmaatschap & Financiën" else "Membership & Finance", if (nl) "Beheer je abonnement en betalingen." else "Manage your plan and payments.") {
        GoldCard { Eyebrow(if (nl) "Actief plan" else "Active plan"); Text("RS PRO", color = RsGoldLight, fontSize = 28.sp, fontWeight = FontWeight.Black); Text("€49 / month", color = RsCream); StatusPill(if (nl) "Actief" else "Active") }
        listOf("September 2026", "August 2026", "July 2026").forEach { month -> GoldCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(month, color = RsCream); Text("€49 · Paid", color = RsGreen) } } }
    }
}

@Composable
private fun BookScreen(lang: Lang, admin: Boolean, onRoute: (String) -> Unit) {
    val nl = lang == Lang.NL
    val uri = LocalUriHandler.current
    ScrollScreen(if (admin) "Book Manager" else "Van Stilte Naar Strijd", "Kickboksen, karakter en de weg van basis naar beheersing") {
        GoldCard {
            Eyebrow(if (admin) "RS Book Manager" else if (nl) "Boek van de trainer" else "Trainer book")
            Text("Van Stilte Naar Strijd", color = RsGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(if (nl) "Van houding, stoten, trappen en knieën tot verdediging, combinaties, conditie, herstel, sparren en wedstrijdvoorbereiding." else "From stance, punches, kicks and knees to defense, combinations, conditioning, recovery, sparring and fight preparation.", color = RsMuted)
            Button(onClick = { uri.openUri("https://www.amazon.nl/s?k=Van+Stilte+Naar+Strijd") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Zoek op Amazon" else "Search on Amazon") }
            OutlinedButton(onClick = { onRoute("academy") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open gerelateerde lessen" else "Open related lessons") }
        }
        GoldCard { Eyebrow("App extras"); listOf("Sample Chapter", "Trainer Video", "Related Technique Lessons", "Mindset & Discipline Bonus", "Technique Drill PDF").forEach { Text("♛ $it", color = RsCream) } }
        if (admin) GoldCard { Text(if (nl) "Promotie- en boekbeheer is lokaal testbaar in deze preview." else "Promotion and book management are locally testable in this preview.", color = RsMuted); ActionStateButton("book_admin_saved", if (nl) "Wijzigingen opslaan" else "Save changes", store = rememberStore()) }
    }
}

@Composable
private fun rememberStore(): PreviewStore {
    val context = LocalContext.current
    return remember { PreviewStore(context) }
}

@Composable
private fun SettingsScreen(lang: Lang, store: PreviewStore) {
    val nl = lang == Lang.NL
    var notifications by remember { mutableStateOf(store.getBool("setting_notifications", true)) }
    var privateProfile by remember { mutableStateOf(store.getBool("setting_private", false)) }
    ScrollScreen(if (nl) "Instellingen & Privacy" else "Settings & Privacy", if (nl) "Jouw appvoorkeuren en privacy." else "Your app preferences and privacy.") {
        GoldCard {
            SettingToggle(if (nl) "Pushmeldingen" else "Push notifications", notifications) { notifications = it; store.setBool("setting_notifications", it) }
            SettingToggle(if (nl) "Privé profiel" else "Private profile", privateProfile) { privateProfile = it; store.setBool("setting_private", it) }
        }
        GoldCard { Eyebrow(if (nl) "Gegevens" else "Data"); Text(if (nl) "Previewgegevens worden alleen lokaal op dit toestel opgeslagen." else "Preview data is stored locally on this device only.", color = RsMuted) }
    }
}

private fun specFor(route: String): ModuleSpec = when (route) {
    "train" -> ModuleSpec("Train Anywhere", "Overal Trainen", "Home training mode and workout generator.", "Thuis trainen en workoutgenerator.", listOf("20-minute technique flow" to "5 rounds · no equipment", "Conditioning circuit" to "Bodyweight · 18 min", "Mobility & recovery" to "12 min"), "Start workout", "Start workout")
    "compare" -> ModuleSpec("Technique Compare", "Techniek Vergelijken", "Compare two techniques side by side.", "Vergelijk twee technieken naast elkaar.", listOf("Jab vs Cross" to "Range · hip rotation · recovery", "Low kick vs body kick" to "Target · angle · balance"), "Save comparison", "Vergelijking opslaan")
    "homework" -> ModuleSpec("Homework", "Huiswerk", "Coach-assigned work for your next session.", "Door de coach toegewezen werk voor je volgende training.", listOf("Guard recovery drill" to "3 × 2 minutes", "Footwork exits" to "4 directions · 40 reps", "Shadowboxing" to "5 rounds"), "Mark submitted", "Markeer ingediend")
    "notifications" -> ModuleSpec("Notifications", "Meldingen", "Club and coaching updates.", "Club- en coachupdates.", listOf("Class reminder" to "Technique & Pads · 19:00", "Coach feedback" to "New note on your homework", "Fight Camp" to "Week plan updated"), "Mark all read", "Alles gelezen")
    "saved" -> ModuleSpec("Saved & Favorites", "Opgeslagen & Favorieten", "Your saved RS content.", "Jouw opgeslagen RS-content.", listOf("Roundhouse Kick" to "Technique", "Defense & Counters" to "Academy lesson", "Fight Camp Week 3" to "Plan"), "Clear selection", "Selectie wissen")
    "history" -> ModuleSpec("Training History", "Trainingsgeschiedenis", "Recent sessions and personal records.", "Recente sessies en persoonlijke records.", listOf("Technique & Pads" to "16 Sep · 78 min", "Home Session" to "14 Sep · 32 min", "Sparring" to "12 Sep · 6 rounds"), "Export preview", "Preview exporteren")
    "profile" -> ModuleSpec("My Profile", "Mijn Profiel", "Your RS member identity.", "Jouw RS-ledenprofiel.", listOf("Level 3" to "12,480 XP", "Attendance" to "92%", "Member since" to "2024"), "Save profile", "Profiel opslaan")
    "media" -> ModuleSpec("Training Media", "Trainingsmedia", "Your technique clips and coach media.", "Jouw techniekclips en coachmedia.", listOf("Low kick review" to "Video · 0:24", "Pad session" to "Video · 1:12", "Guard correction" to "Image"), "Add preview media", "Previewmedia toevoegen")
    "vault" -> ModuleSpec("RS Knowledge Vault", "RS Kennisbank", "Training knowledge, mindset and club resources.", "Trainingskennis, mindset en clubbronnen.", listOf("Kickboxing Fundamentals" to "Guide", "Recovery Basics" to "Guide", "Competition Mindset" to "Article", "Gym Etiquette" to "Club resource"), "Save resource", "Bron opslaan")
    "support" -> ModuleSpec("Support & Documents", "Support & Documenten", "Help, waivers and member documents.", "Hulp, verklaringen en ledendocumenten.", listOf("Membership agreement" to "Document", "Privacy notice" to "Document", "Emergency contact" to "Profile item", "Help center" to "Support"), "Confirm reviewed", "Bevestig gelezen")
    "private" -> ModuleSpec("Private Lessons", "Privélessen", "Request one-to-one training availability.", "Vraag beschikbaarheid voor één-op-één training.", listOf("Technique review" to "60 min", "Fight preparation" to "75 min", "Personal fundamentals" to "60 min"), "Request preview slot", "Vraag previewtijd aan")
    "attendance" -> ModuleSpec("Attendance Manager", "Aanwezigheidsbeheer", "Manage attendance for today.", "Beheer aanwezigheid voor vandaag.", listOf("Technique & Pads" to "12 present · 4 open", "Fundamentals" to "9 present · 7 open"), "Save attendance", "Aanwezigheid opslaan")
    "members" -> ModuleSpec("Student Manager", "Ledenbeheer", "Member overview and progress.", "Ledenoverzicht en voortgang.", listOf("Alex de Vries" to "Level 3 · 92%", "Mila Jansen" to "Level 4 · 96%", "Noah Bakker" to "Level 2 · 84%", "Sara Visser" to "Level 3 · 89%"), "Save member note", "Ledennotitie opslaan")
    "invoices" -> ModuleSpec("Invoices & Payments", "Facturen & Betalingen", "Preview billing administration.", "Preview factuurbeheer.", listOf("INV-2609-001" to "€49 · Paid", "INV-2609-002" to "€49 · Paid", "INV-2609-003" to "€49 · Pending"), "Mark preview paid", "Markeer preview betaald")
    "homeworkadmin" -> ModuleSpec("Homework Manager", "Huiswerkbeheer", "Assign and review member homework.", "Wijs huiswerk toe en beoordeel het.", listOf("Alex" to "Guard recovery · submitted", "Mila" to "Footwork exits · pending", "Sara" to "Shadowboxing · submitted"), "Assign preview homework", "Previewhuiswerk toewijzen")
    "musicadmin" -> ModuleSpec("Music Manager", "Muziekbeheer", "Manage member-uploaded training tracks.", "Beheer door leden geüploade trainingstracks.", listOf("Warm-up Mix" to "03:22 · approved", "Heavy Bag Set" to "04:10 · review", "Sparring Flow" to "03:48 · approved"), "Approve selected", "Selectie goedkeuren")
    "lessonadmin" -> ModuleSpec("Lesson Editor", "Leseditor", "Create and update Academy content.", "Maak en bewerk Academy-content.", listOf("Jab Fundamentals" to "Published", "Roundhouse Kick" to "Published", "Clinch Basics" to "Draft"), "Save lesson draft", "Lesconcept opslaan")
    "classadmin" -> ModuleSpec("Class Manager", "Lesbeheer", "Manage schedule, capacity and waitlists.", "Beheer planning, capaciteit en wachtlijsten.", listOf("Technique & Pads" to "19:00 · 12/16", "Fundamentals" to "18:30 · 9/16", "Advanced Sparring" to "20:00 · 14/14"), "Save class changes", "Leswijzigingen opslaan")
    "notifyadmin" -> ModuleSpec("Notification Composer", "Meldingen Opstellen", "Create club and coach notifications.", "Maak club- en coachmeldingen.", listOf("Class reminder" to "Audience: booked students", "Club announcement" to "Audience: all members"), "Send preview notification", "Previewmelding versturen")
    "plansadmin" -> ModuleSpec("Membership Plans", "Lidmaatschapsplannen", "Manage Basic, Pro and Elite plans.", "Beheer Basic-, Pro- en Elite-plannen.", listOf("RS BASIC" to "€29 / month", "RS PRO" to "€49 / month", "RS ELITE" to "€79 / month"), "Save plan changes", "Planwijzigingen opslaan")
    "progressadmin" -> ModuleSpec("Progress Manager", "Voortgangsbeheer", "Update student level and skill progress.", "Werk niveau en vaardigheidsvoortgang bij.", listOf("Alex" to "Level 3 · 72%", "Mila" to "Level 4 · 81%", "Sara" to "Level 3 · 75%"), "Save progress", "Voortgang opslaan")
    "assessmentsadmin" -> ModuleSpec("Coach Assessments", "Coachbeoordelingen", "Record structured technique assessments.", "Leg gestructureerde techniekbeoordelingen vast.", listOf("Alex" to "Guard 8 · Balance 7", "Mila" to "Guard 9 · Balance 8", "Sara" to "Guard 8 · Balance 8"), "Save assessment", "Beoordeling opslaan")
    "eventsadmin" -> ModuleSpec("Event Manager", "Eventbeheer", "Manage seminars, club events and matches.", "Beheer seminars, clubevents en wedstrijden.", listOf("RS Technique Seminar" to "26 Sep · 24 seats", "Club Sparring Day" to "3 Oct · 18 athletes"), "Save event", "Event opslaan")
    "scheduleadmin" -> ModuleSpec("Weekly Schedule Manager", "Weekplanning Beheer", "Edit the weekly trainer schedule.", "Bewerk de wekelijkse trainerplanning.", listOf("Monday" to "18:30 Fundamentals · 19:45 Pads", "Wednesday" to "19:00 Technique", "Friday" to "20:00 Sparring"), "Save schedule", "Planning opslaan")
    "attendanceflow" -> ModuleSpec("QR Attendance", "QR Aanwezigheid", "Preview the QR check-in flow.", "Test de QR-incheckflow.", listOf("Technique & Pads" to "Scanner ready", "Last check-in" to "Alex · 18:52"), "Simulate QR check-in", "Simuleer QR-incheck")
    "landingadmin" -> ModuleSpec("Landing Page Manager", "Landingpage Beheer", "Manage public RS app promotion blocks.", "Beheer publieke RS-promotieblokken.", listOf("Hero message" to "Published", "Book spotlight" to "Published", "Membership CTA" to "Published"), "Save landing preview", "Landingpreview opslaan")
    "sessionadmin" -> ModuleSpec("Session Builder", "Sessie Bouwer", "Build round-based training sessions.", "Bouw trainingen op basis van rondes.", listOf("Warm-up" to "5 min", "Technique rounds" to "5 × 2 min", "Conditioning" to "3 × 1 min", "Cooldown" to "5 min"), "Save session", "Sessie opslaan")
    "challengesadmin" -> ModuleSpec("Challenge Manager", "Uitdagingenbeheer", "Create XP challenges and leaderboard goals.", "Maak XP-uitdagingen en ranglijstdoelen.", listOf("100 Clean Jabs" to "+250 XP", "3 Sessions" to "+400 XP", "Defense Streak" to "+300 XP"), "Publish preview challenge", "Previewuitdaging publiceren")
    "fightcampadmin" -> ModuleSpec("Fight Camp Manager", "Fight Camp Beheer", "Manage athlete fight-camp plans.", "Beheer fight-campplannen van atleten.", listOf("Alex" to "Week 3/6 · 87% readiness", "Mila" to "Week 5/6 · 91% readiness", "Sara" to "Week 2/6 · 82% readiness"), "Save camp plan", "Campplan opslaan")
    else -> ModuleSpec("RS Module", "RS Module", "Native module ready for preview.", "Native module klaar voor preview.", listOf("RS KICKBOX" to "Module connected"), "Save preview state", "Previewstatus opslaan")
}

@Composable
private fun FunctionalModuleScreen(route: String, lang: Lang, store: PreviewStore, onRoute: (String) -> Unit) {
    val spec = specFor(route)
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) spec.titleNl else spec.titleEn, if (nl) spec.subtitleNl else spec.subtitleEn) {
        spec.cards.forEach { (title, detail) ->
            GoldCard {
                Text(title, color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(detail, color = RsMuted)
            }
        }
        ActionStateButton("module_$route", if (nl) spec.actionNl else spec.actionEn, store)
        when (route) {
            "train" -> OutlinedButton(onClick = { onRoute("session") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open sessiespeler" else "Open Session Player") }
            "compare" -> OutlinedButton(onClick = { onRoute("academy") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open Academy" else "Open Academy") }
            "support" -> OutlinedButton(onClick = { onRoute("settings") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open privacy-instellingen" else "Open privacy settings") }
            "bookadmin" -> OutlinedButton(onClick = { onRoute("book") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open ledenboekpagina" else "Open member book page") }
        }
    }
}

@Composable
private fun ActionStateButton(key: String, label: String, store: PreviewStore) {
    var saved by remember { mutableStateOf(store.getBool(key)) }
    Button(onClick = { saved = true; store.setBool(key, true) }, enabled = !saved, modifier = Modifier.fillMaxWidth()) {
        Text(if (saved) "✓ Saved" else label)
    }
}

@Composable
private fun ScrollScreen(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 1.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = RsGoldLight, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(subtitle, color = RsMuted)
        content()
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun GoldCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .27f)),
        colors = CardDefaults.cardColors(containerColor = RsPanel.copy(alpha = .98f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 3.dp)) {
        Text(label.uppercase(), color = RsMuted, fontSize = 8.sp, letterSpacing = .8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = RsGoldLight, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun Eyebrow(text: String) { Text(text.uppercase(), color = RsGoldLight, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun StatusPill(text: String) {
    Surface(color = RsGreen.copy(alpha = .15f), shape = RoundedCornerShape(20.dp)) {
        Text(text, color = RsGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
    Surface(shape = RoundedCornerShape(16.dp), color = RsPanel2, border = BorderStroke(1.dp, RsGold.copy(alpha = .35f))) {
        Row(Modifier.padding(2.dp)) {
            Text("EN", modifier = Modifier.clickable { onLanguage(Lang.EN) }.padding(horizontal = 7.dp, vertical = 4.dp), color = if (lang == Lang.EN) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("NL", modifier = Modifier.clickable { onLanguage(Lang.NL) }.padding(horizontal = 7.dp, vertical = 4.dp), color = if (lang == Lang.NL) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

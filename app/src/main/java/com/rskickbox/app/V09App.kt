package com.rskickbox.app

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

private val VBlack = Color(0xFF050505)
private val VPanel = Color(0xE615120D)
private val VPanelStrong = Color(0xF511100E)
private val VPanel2 = Color(0xFF211A10)
private val VGold = Color(0xFFC08A24)
private val VGoldLight = Color(0xFFF0CF79)
private val VCream = Color(0xFFF6F0E4)
private val VMuted = Color(0xFFB8AD98)
private val VGreen = Color(0xFF67C587)
private val VRed = Color(0xFFE56F6F)

private enum class VRole { STUDENT, TRAINER }
private enum class VLang { EN, NL }
private enum class VGroup { STUDENT, ADMIN }

private data class VFeature(val id: String, val en: String, val nl: String, val group: VGroup, val icon: String = "♛")

private val vFeatures = listOf(
    VFeature("dashboard", "Dashboard", "Dashboard", VGroup.STUDENT),
    VFeature("academy", "RS Academy", "RS Academy", VGroup.STUDENT),
    VFeature("ai", "AI Coach", "AI Coach", VGroup.STUDENT),
    VFeature("train", "Train Anywhere", "Overal Trainen", VGroup.STUDENT),
    VFeature("session", "Session Player", "Sessie Speler", VGroup.STUDENT),
    VFeature("compare", "Technique Compare", "Techniek Vergelijken", VGroup.STUDENT),
    VFeature("classes", "Classes & Events", "Lessen & Events", VGroup.STUDENT),
    VFeature("community", "Community", "Community", VGroup.STUDENT),
    VFeature("progress", "Progress & Profile", "Voortgang & Profiel", VGroup.STUDENT),
    VFeature("homework", "Homework", "Huiswerk", VGroup.STUDENT),
    VFeature("notifications", "Notifications", "Meldingen", VGroup.STUDENT),
    VFeature("saved", "Saved & Favorites", "Opgeslagen & Favorieten", VGroup.STUDENT),
    VFeature("history", "Training History", "Trainingsgeschiedenis", VGroup.STUDENT),
    VFeature("challenges", "Challenges & Leaderboards", "Uitdagingen & Ranglijst", VGroup.STUDENT),
    VFeature("fightcamp", "Fight Camp", "Fight Camp", VGroup.STUDENT),
    VFeature("finance", "Membership & Payments", "Lidmaatschap & Betalen", VGroup.STUDENT),
    VFeature("profile", "My Profile", "Mijn Profiel", VGroup.STUDENT),
    VFeature("media", "Training Media", "Trainingsmedia", VGroup.STUDENT),
    VFeature("settings", "Settings & Privacy", "Instellingen & Privacy", VGroup.STUDENT),
    VFeature("book", "Trainer Book", "Boek van de Trainer", VGroup.STUDENT),
    VFeature("vault", "Knowledge Vault", "Kennisbank", VGroup.STUDENT),
    VFeature("support", "Support & Documents", "Support & Documenten", VGroup.STUDENT),
    VFeature("private", "Private Lessons", "Privélessen", VGroup.STUDENT),

    VFeature("admin", "Trainer Dashboard", "Trainer Dashboard", VGroup.ADMIN),
    VFeature("paymentsadmin", "Payment Center", "Betaalcentrum", VGroup.ADMIN, "€"),
    VFeature("invoices", "Invoices & Payments", "Facturen & Betalingen", VGroup.ADMIN),
    VFeature("members", "Student Manager", "Ledenbeheer", VGroup.ADMIN),
    VFeature("attendance", "Attendance Manager", "Aanwezigheidsbeheer", VGroup.ADMIN),
    VFeature("homeworkadmin", "Homework Manager", "Huiswerkbeheer", VGroup.ADMIN),
    VFeature("musicadmin", "Music Manager", "Muziekbeheer", VGroup.ADMIN),
    VFeature("lessonadmin", "Lesson Editor", "Leseditor", VGroup.ADMIN),
    VFeature("classadmin", "Class Manager", "Lesbeheer", VGroup.ADMIN),
    VFeature("notifyadmin", "Notification Composer", "Meldingen Opstellen", VGroup.ADMIN),
    VFeature("plansadmin", "Membership Plans", "Lidmaatschapsplannen", VGroup.ADMIN),
    VFeature("progressadmin", "Progress Manager", "Voortgangsbeheer", VGroup.ADMIN),
    VFeature("assessmentsadmin", "Coach Assessments", "Coachbeoordelingen", VGroup.ADMIN),
    VFeature("eventsadmin", "Event Manager", "Eventbeheer", VGroup.ADMIN),
    VFeature("scheduleadmin", "Weekly Schedule", "Weekplanning", VGroup.ADMIN),
    VFeature("attendanceflow", "QR Attendance", "QR Aanwezigheid", VGroup.ADMIN),
    VFeature("bookadmin", "Book Manager", "Boekbeheer", VGroup.ADMIN),
    VFeature("landingadmin", "Landing Page Manager", "Landingpage Beheer", VGroup.ADMIN),
    VFeature("promoadmin", "Visual & Promotion Manager", "Visuals & Promotiebeheer", VGroup.ADMIN, "✦"),
    VFeature("sessionadmin", "Session Builder", "Sessie Bouwer", VGroup.ADMIN),
    VFeature("challengesadmin", "Challenge Manager", "Uitdagingenbeheer", VGroup.ADMIN),
    VFeature("fightcampadmin", "Fight Camp Manager", "Fight Camp Beheer", VGroup.ADMIN)
)

private class VStore(context: Context) {
    private val prefs = context.getSharedPreferences("rs_kickbox_v09", Context.MODE_PRIVATE)
    fun bool(key: String, fallback: Boolean = false) = prefs.getBoolean(key, fallback)
    fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun int(key: String, fallback: Int = 0) = prefs.getInt(key, fallback)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun str(key: String, fallback: String = "") = prefs.getString(key, fallback) ?: fallback
    fun putStr(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

@Composable
fun RsKickboxV09App() {
    val context = LocalContext.current
    val store = remember { VStore(context) }
    var role by remember { mutableStateOf<VRole?>(null) }
    var route by remember { mutableStateOf("dashboard") }
    var lang by remember { mutableStateOf(if (store.str("lang", "EN") == "NL") VLang.NL else VLang.EN) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = VGoldLight,
            secondary = VGold,
            background = VBlack,
            surface = VPanelStrong,
            onPrimary = Color(0xFF1A1306),
            onBackground = VCream,
            onSurface = VCream
        )
    ) {
        if (role == null) {
            VLoginScreen(lang, store, onLanguage = { lang = it; store.putStr("lang", it.name) }) {
                role = it
                route = if (it == VRole.TRAINER) "admin" else "dashboard"
            }
        } else {
            VShell(role!!, route, lang, store, onRoute = { route = it }, onLanguage = { lang = it; store.putStr("lang", it.name) }) {
                role = null
            }
        }
    }
}

@Composable
private fun PremiumKickboxBackground(route: String, content: @Composable BoxScope.() -> Unit) {
    val motion = rememberInfiniteTransition(label = "rs-wallpaper")
    val drift by motion.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(14000), RepeatMode.Reverse),
        label = "drift"
    )
    val training = route in setOf("academy", "ai", "train", "session", "compare", "progress", "fightcamp", "challenges")
    val promotion = route in setOf("login", "dashboard", "admin", "book", "bookadmin", "landingadmin", "promoadmin")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050505), Color(0xFF0A0805), Color(0xFF100B04))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val gold = VGoldLight.copy(alpha = if (promotion) .11f else .07f)
            val bronze = VGold.copy(alpha = if (training) .10f else .06f)
            drawCircle(gold, radius = size.minDimension * .42f, center = Offset(size.width * drift, size.height * .19f))
            drawCircle(bronze, radius = size.minDimension * .32f, center = Offset(size.width * (1f - drift), size.height * .73f))
            val ropeAlpha = if (training) .14f else .08f
            repeat(3) { i ->
                val y = size.height * (.57f + i * .055f)
                drawLine(VGoldLight.copy(alpha = ropeAlpha), Offset(0f, y), Offset(size.width, y + size.height * .018f), strokeWidth = 2.2f)
            }
            drawLine(VGold.copy(alpha = .12f), Offset(size.width * .18f, 0f), Offset(size.width * .72f, size.height), strokeWidth = 1.6f)
            drawLine(VGoldLight.copy(alpha = .07f), Offset(size.width * .82f, 0f), Offset(size.width * .28f, size.height), strokeWidth = 1.2f)
        }
        if (promotion || training) {
            Text(
                if (training) "RS  TRAIN  FIGHT  GROW" else "RS KICKBOX  ELITE GOLD",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
                color = VGoldLight.copy(alpha = if (training) .055f else .075f),
                fontSize = if (training) 25.sp else 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (training) .30f else .24f)))
        content()
    }
}

@Composable
private fun VLoginScreen(lang: VLang, store: VStore, onLanguage: (VLang) -> Unit, onLogin: (VRole) -> Unit) {
    val nl = lang == VLang.NL
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var password by remember { mutableStateOf("preview123") }
    PremiumKickboxBackground("login") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("♛ RS KICKBOX", color = VGoldLight, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("TRAIN · LEARN · CONNECT · GROW", color = VMuted, fontSize = 10.sp, letterSpacing = 1.6.sp)
                }
                VLanguageSwitch(lang, onLanguage)
            }
            Text(if (nl) "Premium kickboksplatform" else "Premium kickboxing platform", color = VCream, style = MaterialTheme.typography.headlineMedium)
            VCard {
                VLabel(if (nl) "Leden toegang" else "Member access")
                Text(if (nl) "Welkom bij de RS-familie." else "Welcome to the RS family.", color = VGoldLight, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text(if (nl) "Wachtwoord" else "Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = { onLogin(VRole.STUDENT) }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Leerling login" else "Student login") }
                OutlinedButton(onClick = { onLogin(VRole.TRAINER) }, modifier = Modifier.fillMaxWidth()) { Text("Trainer / Admin") }
            }
            VCard {
                VLabel(if (nl) "Boek van de trainer" else "Trainer book spotlight")
                Text("Van Stilte Naar Strijd", color = VGoldLight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Kickboksen, karakter en de weg van basis naar beheersing", color = VCream)
                Text(if (nl) "Cinematische RS-promotie met een donkere overlay zodat tekst en knoppen altijd leesbaar blijven." else "Cinematic RS promotion with a dark overlay so text and controls remain readable.", color = VMuted)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VShell(role: VRole, route: String, lang: VLang, store: VStore, onRoute: (String) -> Unit, onLanguage: (VLang) -> Unit, onLogout: () -> Unit) {
    val home = if (role == VRole.TRAINER) "admin" else "dashboard"
    val current = vFeatures.firstOrNull { it.id == route }
    PremiumKickboxBackground(route) {
        Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, VGoldLight.copy(alpha = .42f)),
                colors = CardDefaults.cardColors(containerColor = VPanelStrong.copy(alpha = .95f))
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("♛ RS KICKBOX", color = VGoldLight, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (role == VRole.TRAINER) "TRAINER / ADMIN" else "STUDENT", color = VMuted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        VLanguageSwitch(lang, onLanguage)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (route != home) {
                                OutlinedButton(onClick = { onRoute(home) }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp), modifier = Modifier.heightIn(min = 36.dp)) { Text("‹", fontSize = 17.sp) }
                            }
                            OutlinedButton(onClick = onLogout, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp), modifier = Modifier.heightIn(min = 36.dp)) {
                                Text(if (lang == VLang.NL) "Uitloggen" else "Log out", color = VGoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (route != home && current != null) Text(if (lang == VLang.NL) current.nl else current.en, color = VCream, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) { VRouter(role, route, lang, store, onRoute) }
        }
    }
}

@Composable
private fun VRouter(role: VRole, route: String, lang: VLang, store: VStore, onRoute: (String) -> Unit) {
    when (route) {
        "dashboard", "admin" -> VDashboard(role, lang, onRoute)
        "academy" -> VAcademy(lang, store)
        "session" -> VSession(lang)
        "classes" -> VClasses(lang, store)
        "challenges" -> VChallenges(lang, store)
        "fightcamp" -> VFightCamp(lang, store)
        "finance" -> VMemberPayments(lang, store)
        "paymentsadmin", "invoices" -> VPaymentCenter(lang, store)
        "book", "bookadmin" -> VBook(lang)
        "promoadmin", "landingadmin" -> VPromotionManager(lang, store)
        else -> VGenericModule(route, lang, store, onRoute)
    }
}

@Composable
private fun VDashboard(role: VRole, lang: VLang, onRoute: (String) -> Unit) {
    val nl = lang == VLang.NL
    val list = vFeatures.filter { if (role == VRole.TRAINER) it.group == VGroup.ADMIN && it.id != "admin" else it.group == VGroup.STUDENT && it.id != "dashboard" }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VCard {
            VLabel(if (role == VRole.TRAINER) "Trainer Control Center" else "RS Live Dashboard")
            Text(if (role == VRole.TRAINER) if (nl) "Beheer leden, trainingen, betalingen, promotie en content vanuit één dashboard." else "Manage members, training, payments, promotion and content from one dashboard." else if (nl) "Train, leer, boek, betaal en volg je voortgang binnen RS KICKBOX." else "Train, learn, book, pay and track progress inside RS KICKBOX.", color = VGoldLight, style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                VMetric(if (role == VRole.TRAINER) if (nl) "Leden" else "Members" else "Level", if (role == VRole.TRAINER) "124" else "3", Modifier.weight(1f))
                VMetric(if (role == VRole.TRAINER) "Revenue" else "XP", if (role == VRole.TRAINER) "€4,850" else "12,480", Modifier.weight(1f))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            items(list) { f ->
                Card(Modifier.fillMaxWidth().clickable { onRoute(f.id) }, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, VGold.copy(alpha = .36f)), colors = CardDefaults.cardColors(containerColor = VPanelStrong.copy(alpha=.93f))) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(f.icon, color = VGoldLight, fontSize = 20.sp)
                        Text(if (nl) f.nl else f.en, color = VGoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (nl) "Open module" else "Open module", color = VMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VAcademy(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    val lessons = listOf("Jab Fundamentals", "Roundhouse Kick", "Defense & Counters", "Footwork Flow", "Combination Builder")
    VScreen("RS Academy", if (nl) "Techniekbibliotheek met cinematische trainingsachtergrond." else "Technique library with a cinematic training background.") {
        lessons.forEach { lesson ->
            var done by remember(lesson) { mutableStateOf(store.bool("lesson_$lesson", lesson == "Jab Fundamentals")) }
            VCard {
                Text(lesson, color = VGoldLight, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(if (nl) "Video, techniekpunten, fouten, drill en coach-cues." else "Video, key technique points, errors, drill and coach cues.", color = VMuted)
                Button(onClick = { done = true; store.putBool("lesson_$lesson", true) }, enabled = !done, modifier = Modifier.fillMaxWidth()) { Text(if (done) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Voltooi les" else "Complete lesson") }
            }
        }
    }
}

@Composable
private fun VSession(lang: VLang) {
    val nl = lang == VLang.NL
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(120) }
    var round by remember { mutableIntStateOf(1) }
    LaunchedEffect(running, seconds) { if (running && seconds > 0) { delay(1000); seconds-- } else if (seconds == 0) running = false }
    VScreen(if (nl) "Sessie speler" else "Session Player", if (nl) "Subtiele bewegende achtergrond; timer en tekst blijven dominant zichtbaar." else "Subtle moving background; timer and text remain dominant and readable.") {
        VCard {
            VLabel("Round $round / 5")
            Text("%d:%02d".format(seconds/60, seconds%60), color = VGoldLight, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("Jab · Cross · Low Kick", color = VMuted)
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) { Text(if (running) if (nl) "Pauze" else "Pause" else if (nl) "Start" else "Start") }
            OutlinedButton(onClick = { round = (round + 1).coerceAtMost(5); seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Volgende ronde" else "Next round") }
            TextButton(onClick = { round = 1; seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text("Reset") }
        }
    }
}

@Composable
private fun VClasses(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    val classes = listOf("Technique & Pads" to "Today · 19:00 · 12/16", "Kickboxing Fundamentals" to "Thu · 18:30 · 9/16", "Advanced Sparring" to "Fri · 20:00 · 14/14")
    VScreen(if (nl) "Lessen & Events" else "Classes & Events", if (nl) "Boek en beheer je trainingen." else "Book and manage your training.") {
        classes.forEach { (name, detail) ->
            var booked by remember(name) { mutableStateOf(store.bool("booked_$name", name == "Technique & Pads")) }
            VCard {
                Text(name, color = VGoldLight, fontWeight = FontWeight.Bold)
                Text(detail, color = VMuted)
                Button(onClick = { booked = !booked; store.putBool("booked_$name", booked) }, modifier = Modifier.fillMaxWidth()) { Text(if (booked) if (nl) "Geboekt ✓" else "Booked ✓" else if (nl) "Boek les" else "Book class") }
            }
        }
    }
}

@Composable
private fun VChallenges(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    VScreen(if (nl) "Uitdagingen & Ranglijst" else "Challenges & Leaderboards", if (nl) "Verdien XP met technische en mentale doelen." else "Earn XP with technical and mindset goals.") {
        listOf("1,000 Kicks Challenge" to 500, "7-Day Discipline Streak" to 350, "Combo Master" to 400).forEach { (name, xp) ->
            var done by remember(name) { mutableStateOf(store.bool("challenge_$name")) }
            VCard {
                Text(name, color = VGoldLight, fontWeight = FontWeight.Bold)
                Text("+$xp XP", color = VMuted)
                Button(onClick = { done = true; store.putBool("challenge_$name", true) }, enabled = !done, modifier = Modifier.fillMaxWidth()) { Text(if (done) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Markeer voltooid" else "Mark complete") }
            }
        }
    }
}

@Composable
private fun VFightCamp(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    var week by remember { mutableIntStateOf(store.int("fight_week", 3)) }
    VScreen("Fight Camp", if (nl) "Wedstrijdvoorbereiding met rustige visuele beweging achter de inhoud." else "Fight preparation with restrained visual motion behind the content.") {
        VCard {
            Text("TBD · Club Match", color = VGoldLight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("24 Oct 2026 · -75 kg", color = VMuted)
            LinearProgressIndicator(progress = { week / 6f }, modifier = Modifier.fillMaxWidth())
            Text("Week $week / 6 · Readiness 87%", color = VCream)
            Button(onClick = { if (week < 6) { week++; store.putInt("fight_week", week) } }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Volgende week" else "Advance week") }
        }
        VCard { Text(if (nl) "Veiligheidsregel: geen agressieve gewichtsdoelen of medische adviezen in de app zonder professionele begeleiding." else "Safety rule: no aggressive weight targets or medical advice in the app without professional supervision.", color = VMuted) }
    }
}

@Composable
private fun VMemberPayments(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    val uri = LocalUriHandler.current
    VScreen(if (nl) "Lidmaatschap & Betalen" else "Membership & Payments", if (nl) "Kies uit de door de trainer ingeschakelde betaalmethoden." else "Choose from payment methods enabled by the trainer.") {
        VCard {
            VLabel(if (nl) "Actief plan" else "Active plan")
            Text("RS PRO", color = VGoldLight, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("€49 / month", color = VCream)
            VStatus(if (nl) "Actief" else "Active", true)
        }
        if (store.bool("pay_bank", true)) VPaymentMethod("Bank transfer", if (nl) "Handmatig · geen app-verwerkingskosten" else "Manual · no app processing fee", store.str("iban", "NL00 BANK 0000 0000 00"), null)
        if (store.bool("pay_cash", true)) VPaymentMethod(if (nl) "Contant" else "Cash", if (nl) "Handmatig op locatie" else "Manual at the club", if (nl) "Laat trainer betaling bevestigen" else "Trainer confirms payment", null)
        if (store.bool("pay_tikkie", true)) VPaymentMethod("Tikkie", if (nl) "Providerkosten kunnen gelden" else "Provider fees may apply", if (nl) "Betaalverzoek" else "Payment request", store.str("tikkie_link"), uri)
        if (store.bool("pay_ideal", true)) VPaymentMethod("iDEAL / Wero", if (nl) "Via aangesloten betaalprovider" else "Via connected payment provider", if (nl) "Online betaling" else "Online payment", store.str("ideal_link"), uri)
        if (store.bool("pay_revolut", true)) VPaymentMethod("Revolut Pay", if (nl) "Merchant processing fee kan gelden" else "Merchant processing fee may apply", if (nl) "Revolut-betaallink" else "Revolut payment link", store.str("revolut_link"), uri)
        if (store.bool("pay_sepa", false)) VPaymentMethod("SEPA Direct Debit", if (nl) "Voor terugkerende contributie" else "For recurring membership", store.str("creditor_id", if (nl) "Creditor ID instellen" else "Set Creditor ID"), null)
        if (store.bool("pay_cards", false)) VPaymentMethod("Card / Apple Pay / Google Pay", if (nl) "Via PSP; transactiekosten gelden" else "Via PSP; transaction fees apply", store.str("card_link"), uri)
        if (store.bool("pay_paypal", false)) VPaymentMethod("PayPal", if (nl) "Providerkosten gelden doorgaans" else "Provider fees generally apply", if (nl) "Externe betaallink" else "External payment link", store.str("paypal_link"), uri)
        if (store.bool("pay_custom", false)) VPaymentMethod(if (nl) "Andere betaallink" else "Custom payment link", if (nl) "Door trainer ingesteld" else "Configured by trainer", store.str("custom_label", "Payment link"), store.str("custom_link"), uri)
    }
}

@Composable
private fun VPaymentMethod(title: String, fee: String, detail: String, link: String?, uri: androidx.compose.ui.platform.UriHandler? = null) {
    VCard {
        Text(title, color = VGoldLight, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(fee, color = VMuted, fontSize = 11.sp)
        Text(detail, color = VCream)
        if (!link.isNullOrBlank() && uri != null) Button(onClick = { uri.openUri(link) }, modifier = Modifier.fillMaxWidth()) { Text("Open payment") }
    }
}

@Composable
private fun VPaymentCenter(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    var saved by remember { mutableStateOf(false) }
    var company by remember { mutableStateOf(store.str("company", "RS KICKBOX")) }
    var iban by remember { mutableStateOf(store.str("iban", "")) }
    var bic by remember { mutableStateOf(store.str("bic", "")) }
    var kvk by remember { mutableStateOf(store.str("kvk", "")) }
    var vat by remember { mutableStateOf(store.str("vat", "")) }
    var email by remember { mutableStateOf(store.str("billing_email", "")) }
    var tikkie by remember { mutableStateOf(store.str("tikkie_link", "")) }
    var ideal by remember { mutableStateOf(store.str("ideal_link", "")) }
    var revolut by remember { mutableStateOf(store.str("revolut_link", "")) }
    var card by remember { mutableStateOf(store.str("card_link", "")) }
    var paypal by remember { mutableStateOf(store.str("paypal_link", "")) }
    var creditor by remember { mutableStateOf(store.str("creditor_id", "")) }
    var customLabel by remember { mutableStateOf(store.str("custom_label", "Other payment")) }
    var customLink by remember { mutableStateOf(store.str("custom_link", "")) }
    var bank by remember { mutableStateOf(store.bool("pay_bank", true)) }
    var cash by remember { mutableStateOf(store.bool("pay_cash", true)) }
    var tikkieOn by remember { mutableStateOf(store.bool("pay_tikkie", true)) }
    var idealOn by remember { mutableStateOf(store.bool("pay_ideal", true)) }
    var revolutOn by remember { mutableStateOf(store.bool("pay_revolut", true)) }
    var sepaOn by remember { mutableStateOf(store.bool("pay_sepa", false)) }
    var cardsOn by remember { mutableStateOf(store.bool("pay_cards", false)) }
    var paypalOn by remember { mutableStateOf(store.bool("pay_paypal", false)) }
    var customOn by remember { mutableStateOf(store.bool("pay_custom", false)) }

    VScreen(if (nl) "Betaalcentrum" else "Payment Center", if (nl) "Alle betaalinformatie, links en activeringen vanuit het Trainer/Admin dashboard." else "All payment information, links and activation controls from Trainer/Admin.") {
        VCard {
            VLabel(if (nl) "Bedrijfs- en factuurgegevens" else "Business & invoice details")
            VField(company, { company = it }, if (nl) "Bedrijfsnaam" else "Business name")
            VField(iban, { iban = it }, "IBAN")
            VField(bic, { bic = it }, "BIC")
            VField(kvk, { kvk = it }, "KvK")
            VField(vat, { vat = it }, if (nl) "BTW-nummer" else "VAT number")
            VField(email, { email = it }, if (nl) "Factuur e-mail" else "Billing email")
        }
        VCard {
            VLabel(if (nl) "Handmatig / zonder app-verwerkingsfee" else "Manual / no app processing fee")
            VToggle(if (nl) "Bankoverschrijving" else "Bank transfer", bank) { bank = it }
            VToggle(if (nl) "Contant" else "Cash", cash) { cash = it }
            Text(if (nl) "Bankkosten of administratieve kosten van je bank kunnen nog steeds gelden." else "Your bank may still charge account or transfer-related fees.", color = VMuted, fontSize = 11.sp)
        }
        VCard {
            VLabel("Tikkie")
            VToggle(if (nl) "Tikkie inschakelen" else "Enable Tikkie", tikkieOn) { tikkieOn = it }
            VField(tikkie, { tikkie = it }, "Tikkie payment link")
            VFeeNote(if (nl) "Tikkie Zakelijk is niet onbeperkt gratis; providerkosten kunnen gelden." else "Tikkie Business is not unlimited-free; provider fees may apply.")
        }
        VCard {
            VLabel("iDEAL / Wero")
            VToggle(if (nl) "iDEAL/Wero inschakelen" else "Enable iDEAL/Wero", idealOn) { idealOn = it }
            VField(ideal, { ideal = it }, if (nl) "PSP / betaallink" else "PSP / payment link")
            VFeeNote(if (nl) "iDEAL/Wero vereist normaal een aangesloten PSP/acquirer; tarieven verschillen per provider." else "iDEAL/Wero normally requires a contracted PSP/acquirer; pricing varies by provider.")
        }
        VCard {
            VLabel("Revolut")
            VToggle(if (nl) "Revolut Pay inschakelen" else "Enable Revolut Pay", revolutOn) { revolutOn = it }
            VField(revolut, { revolut = it }, "Revolut payment link")
            VFeeNote(if (nl) "Revolut merchant/payment-link verwerking kan transactiekosten hebben." else "Revolut merchant/payment-link processing can carry transaction fees.")
        }
        VCard {
            VLabel(if (nl) "Terugkerend & wallets" else "Recurring & wallets")
            VToggle("SEPA Direct Debit", sepaOn) { sepaOn = it }
            if (sepaOn) VField(creditor, { creditor = it }, "SEPA Creditor ID")
            VToggle("Card / Apple Pay / Google Pay", cardsOn) { cardsOn = it }
            if (cardsOn) VField(card, { card = it }, "Card/wallet payment link")
            VToggle("PayPal", paypalOn) { paypalOn = it }
            if (paypalOn) VField(paypal, { paypal = it }, "PayPal payment link")
        }
        VCard {
            VLabel(if (nl) "Andere betaalmethode" else "Custom payment method")
            VToggle(if (nl) "Eigen betaallink inschakelen" else "Enable custom payment link", customOn) { customOn = it }
            if (customOn) {
                VField(customLabel, { customLabel = it }, if (nl) "Naam methode" else "Method name")
                VField(customLink, { customLink = it }, if (nl) "Betaallink" else "Payment link")
            }
        }
        VCard {
            VLabel(if (nl) "Administratieve workflow" else "Administrative workflow")
            listOf(
                if (nl) "Factuurstatus: Concept → Verzonden → Betaald → Achterstallig" else "Invoice status: Draft → Sent → Paid → Overdue",
                if (nl) "Koppel betaalmethode aan lid en factuur" else "Attach payment method to member and invoice",
                if (nl) "Registreer handmatige betalingen met datum en referentie" else "Record manual payments with date and reference",
                if (nl) "Exporteer maandrapport voor boekhouding" else "Export monthly accounting report",
                if (nl) "Bewaar provider-ID's en links alleen in beveiligde productieopslag" else "Store provider IDs and links only in secure production storage"
            ).forEach { Text("♛ $it", color = VCream, fontSize = 12.sp) }
            Button(onClick = {
                store.putStr("company", company); store.putStr("iban", iban); store.putStr("bic", bic); store.putStr("kvk", kvk); store.putStr("vat", vat); store.putStr("billing_email", email)
                store.putStr("tikkie_link", tikkie); store.putStr("ideal_link", ideal); store.putStr("revolut_link", revolut); store.putStr("card_link", card); store.putStr("paypal_link", paypal); store.putStr("creditor_id", creditor); store.putStr("custom_label", customLabel); store.putStr("custom_link", customLink)
                store.putBool("pay_bank", bank); store.putBool("pay_cash", cash); store.putBool("pay_tikkie", tikkieOn); store.putBool("pay_ideal", idealOn); store.putBool("pay_revolut", revolutOn); store.putBool("pay_sepa", sepaOn); store.putBool("pay_cards", cardsOn); store.putBool("pay_paypal", paypalOn); store.putBool("pay_custom", customOn)
                saved = true
            }, modifier = Modifier.fillMaxWidth()) { Text(if (saved) if (nl) "Opgeslagen ✓" else "Saved ✓" else if (nl) "Betaalinstellingen opslaan" else "Save payment settings") }
        }
    }
}

@Composable
private fun VPromotionManager(lang: VLang, store: VStore) {
    val nl = lang == VLang.NL
    var motion by remember { mutableStateOf(store.bool("promo_motion", true)) }
    var loginHero by remember { mutableStateOf(store.bool("promo_login", true)) }
    var trainingBg by remember { mutableStateOf(store.bool("promo_training", true)) }
    var saved by remember { mutableStateOf(false) }
    VScreen(if (nl) "Visuals & Promotiebeheer" else "Visual & Promotion Manager", if (nl) "Beheer live RS-achtergronden zonder leesbaarheid te verliezen." else "Manage live RS backgrounds without sacrificing readability.") {
        VCard {
            VToggle(if (nl) "Live login achtergrond" else "Live login background", loginHero) { loginHero = it }
            VToggle(if (nl) "Subtiele trainingsachtergronden" else "Subtle training backgrounds", trainingBg) { trainingBg = it }
            VToggle(if (nl) "Beweging inschakelen" else "Enable motion", motion) { motion = it }
            Text(if (nl) "Regel: promotiepagina's mogen sterker bewegen; trainingspagina's gebruiken lage opacity en donkere scrim zodat tekst, timer en knoppen altijd dominant blijven." else "Rule: promotion pages may use stronger motion; training pages use low opacity plus a dark scrim so text, timers and controls always remain dominant.", color = VMuted)
            Button(onClick = { store.putBool("promo_motion", motion); store.putBool("promo_login", loginHero); store.putBool("promo_training", trainingBg); saved = true }, modifier = Modifier.fillMaxWidth()) { Text(if (saved) if (nl) "Opgeslagen ✓" else "Saved ✓" else if (nl) "Visual-instellingen opslaan" else "Save visual settings") }
        }
        VCard {
            VLabel(if (nl) "Promotieposities" else "Promotion placements")
            listOf("Login hero", "Dashboard hero", "Trainer Book spotlight", "Public landing page", "Events", "Fight Camp", "Academy", "Session Player").forEach { Text("♛ $it", color = VCream) }
        }
    }
}

@Composable
private fun VBook(lang: VLang) {
    val nl = lang == VLang.NL
    val uri = LocalUriHandler.current
    VScreen("Van Stilte Naar Strijd", "Kickboksen, karakter en de weg van basis naar beheersing") {
        VCard {
            Text("Van Stilte Naar Strijd", color = VGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(if (nl) "Trainerboek geïntegreerd met Academy, mindset en trainingscontent." else "Trainer book integrated with Academy, mindset and training content.", color = VMuted)
            Button(onClick = { uri.openUri("https://www.amazon.nl/s?k=Van+Stilte+Naar+Strijd") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Zoek op Amazon" else "Search on Amazon") }
        }
    }
}

@Composable
private fun VGenericModule(route: String, lang: VLang, store: VStore, onRoute: (String) -> Unit) {
    val nl = lang == VLang.NL
    val feature = vFeatures.firstOrNull { it.id == route }
    val title = if (nl) feature?.nl ?: route else feature?.en ?: route
    var done by remember(route) { mutableStateOf(store.bool("generic_$route")) }
    val cards = when (route) {
        "ai" -> listOf("Technique Analysis" to "84% score · guard recovery", "Corrective Workout" to "3 drills · 12 minutes")
        "train" -> listOf("20 min Technique" to "Jab · Cross · Low Kick", "30 min Conditioning" to "Intervals · Core · Recovery")
        "compare" -> listOf("Jab vs Cross" to "Guard · Distance · Rotation", "Low Kick vs Body Kick" to "Target · Hip angle · Return")
        "community" -> listOf("Trainer Update" to "Strong session tonight", "RS Fighters" to "18 members · group chat")
        "progress" -> listOf("Level 3" to "12,480 XP", "Mastery" to "72% overall")
        "homework" -> listOf("Defense Drill" to "Due Friday · video", "Footwork Flow" to "3 x 2 minutes")
        "notifications" -> listOf("Class Reminder" to "Technique & Pads · 19:00", "Coach Note" to "New feedback available")
        "saved" -> listOf("Roundhouse Kick" to "RS Academy", "Mindset & Discipline" to "Trainer Book")
        "history" -> listOf("16 Sep" to "Technique & Pads · 88%", "14 Sep" to "Home session · 5 rounds")
        "profile" -> listOf("Alex de Vries" to "Level 3 · 12,480 XP", "Goal" to "Defense & conditioning")
        "media" -> listOf("Technique Review" to "00:24 · video", "Pad Session" to "8 photos")
        "settings" -> listOf("Privacy" to "Export / delete account", "Language" to "EN / NL")
        "vault" -> listOf("Fight Preparation" to "Checklist & strategy", "Recovery" to "Sleep · hydration · load")
        "support" -> listOf("Help Center" to "FAQs · contact", "Documents" to "Waivers · membership files")
        "private" -> listOf("Ricardo" to "Tue 18:00 · Sat 10:00", "Private lesson" to "60 min")
        "members" -> listOf("Mila Jansen" to "Level 4 · 96%", "Noah Bakker" to "Level 2 · 84%")
        "attendance" -> listOf("Technique & Pads" to "12 / 16", "Fundamentals" to "9 / 16")
        "homeworkadmin" -> listOf("Defense Drill" to "8 submitted · 3 review", "Footwork Flow" to "12 assigned")
        "musicadmin" -> listOf("Team Warmup" to "03:18", "Pad Round Mix" to "02:45")
        "lessonadmin" -> listOf("Jab Fundamentals" to "Published", "Defense & Counters" to "Draft")
        "classadmin" -> listOf("Technique & Pads" to "12 / 16", "Advanced Sparring" to "14 / 14 · waitlist 3")
        "notifyadmin" -> listOf("Audience" to "All members", "Channel" to "Push + in-app")
        "plansadmin" -> listOf("RS BASIC" to "€29", "RS PRO" to "€49", "RS ELITE" to "€79")
        "progressadmin" -> listOf("Alex" to "72% mastery", "Mila" to "84% mastery")
        "assessmentsadmin" -> listOf("Technique" to "8.4 / 10", "Discipline" to "9.1 / 10")
        "eventsadmin" -> listOf("RS Sparring Night" to "24 Oct", "Technique Seminar" to "8 Nov")
        "scheduleadmin" -> listOf("Monday" to "18:30 Fundamentals · 20:00 Pads", "Friday" to "20:00 Sparring")
        "attendanceflow" -> listOf("QR Session" to "Ready to scan", "Manual fallback" to "Enabled")
        "sessionadmin" -> listOf("5 x 2 min" to "45 sec rest", "Combo cue" to "Jab · Cross · Low Kick")
        "challengesadmin" -> listOf("1,000 Kicks" to "+500 XP", "7-Day Discipline" to "+350 XP")
        "fightcampadmin" -> listOf("Alex" to "Week 3 / 6 · 87%", "Mila" to "Week 5 / 8 · 91%")
        else -> listOf("RS Status" to "Ready", "Mobile layout" to "Safe · scrollable · responsive")
    }
    VScreen(title, if (nl) "Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout.") {
        cards.forEach { (a,b) -> VCard { Text(a, color = VGoldLight, fontWeight = FontWeight.Bold); Text(b, color = VMuted) } }
        if (route == "train") Button(onClick = { onRoute("session") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open sessiespeler" else "Open Session Player") }
        if (route == "compare") Button(onClick = { onRoute("academy") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open RS Academy" else "Open RS Academy") }
        if (route == "support") Button(onClick = { onRoute("settings") }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open privacy" else "Open privacy") }
        VCard {
            Button(onClick = { done = !done; store.putBool("generic_$route", done) }, modifier = Modifier.fillMaxWidth()) { Text(if (done) if (nl) "Opgeslagen ✓" else "Saved ✓" else if (nl) "Test actie" else "Test action") }
        }
    }
}

@Composable
private fun VScreen(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 2.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, color = VGoldLight, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        Text(subtitle, color = VMuted, modifier = Modifier.fillMaxWidth())
        content()
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun VCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, VGoldLight.copy(alpha = .31f)), colors = CardDefaults.cardColors(containerColor = VPanelStrong.copy(alpha = .92f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun VMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = VPanel2.copy(alpha=.94f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, VGold.copy(alpha=.27f))) {
        Column(Modifier.padding(10.dp)) { Text(label.uppercase(), color = VMuted, fontSize = 9.sp, maxLines=1, overflow=TextOverflow.Ellipsis); Text(value, color=VGoldLight, fontSize=19.sp, fontWeight=FontWeight.Black, maxLines=1, overflow=TextOverflow.Ellipsis) }
    }
}

@Composable private fun VLabel(text: String) { Text(text.uppercase(), color=VGoldLight, fontSize=9.sp, letterSpacing=1.4.sp, fontWeight=FontWeight.Bold, modifier=Modifier.fillMaxWidth()) }
@Composable private fun VStatus(text: String, good: Boolean) { Surface(color=(if(good)VGreen else VRed).copy(alpha=.14f), shape=RoundedCornerShape(20.dp)) { Text(text, color=if(good)VGreen else VRed, modifier=Modifier.padding(horizontal=10.dp, vertical=5.dp), fontWeight=FontWeight.Bold, fontSize=11.sp) } }
@Composable private fun VToggle(label: String, value: Boolean, onChange: (Boolean)->Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) { Text(label, color=VCream, modifier=Modifier.weight(1f)); Switch(value,onChange) } }
@Composable private fun VField(value: String, onChange: (String)->Unit, label: String) { OutlinedTextField(value,onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true) }
@Composable private fun VFeeNote(text: String) { Surface(color=VGold.copy(alpha=.10f), shape=RoundedCornerShape(12.dp)) { Text(text, color=VMuted, fontSize=11.sp, modifier=Modifier.padding(10.dp)) } }

@Composable
private fun VLanguageSwitch(lang: VLang, onLanguage: (VLang)->Unit) {
    Surface(shape=RoundedCornerShape(16.dp),color=VPanel2,border=BorderStroke(1.dp,VGold.copy(alpha=.4f))) {
        Row(Modifier.padding(2.dp)) {
            Text("EN", Modifier.clickable{onLanguage(VLang.EN)}.padding(horizontal=8.dp,vertical=5.dp), color=if(lang==VLang.EN)VGoldLight else VMuted, fontWeight=FontWeight.Bold,fontSize=10.sp)
            Text("NL", Modifier.clickable{onLanguage(VLang.NL)}.padding(horizontal=8.dp,vertical=5.dp), color=if(lang==VLang.NL)VGoldLight else VMuted, fontWeight=FontWeight.Bold,fontSize=10.sp)
        }
    }
}

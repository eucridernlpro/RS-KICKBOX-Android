package com.rskickbox.app

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private enum class AppRole { STUDENT, TRAINER }
private enum class AppLang { EN, NL }
private enum class ThemeId { ELITE_GOLD, ROYAL_CRIMSON, MIDNIGHT_PLATINUM, EMERALD_COMBAT }
private enum class Plan { BASIC, PRO, ELITE }

private data class AppTheme(
    val id: ThemeId,
    val name: String,
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val accent: Color,
    val accent2: Color,
    val text: Color,
    val muted: Color
)

private data class Feature(val id: String, val en: String, val nl: String)

private val themes = listOf(
    AppTheme(ThemeId.ELITE_GOLD, "Elite Gold", Color(0xFF050505), Color(0xFF15120D), Color(0xFF211A10), Color(0xFFC08A24), Color(0xFFF0CF79), Color(0xFFF6F0E4), Color(0xFFB8AD98)),
    AppTheme(ThemeId.ROYAL_CRIMSON, "Royal Crimson", Color(0xFF080405), Color(0xFF1C0E12), Color(0xFF2B151A), Color(0xFFA12B3D), Color(0xFFF0A58E), Color(0xFFFFF3EF), Color(0xFFC6A2A0)),
    AppTheme(ThemeId.MIDNIGHT_PLATINUM, "Midnight Platinum", Color(0xFF04070B), Color(0xFF10161D), Color(0xFF19222C), Color(0xFF75889A), Color(0xFFE4EDF5), Color(0xFFF4F8FB), Color(0xFFA9B7C2)),
    AppTheme(ThemeId.EMERALD_COMBAT, "Emerald Combat", Color(0xFF030806), Color(0xFF0C1712), Color(0xFF11251B), Color(0xFF26835F), Color(0xFFB9E7C7), Color(0xFFF2FAF4), Color(0xFFA5B9AC))
)

private val studentFeatures = listOf(
    Feature("academy", "RS Academy", "RS Academy"),
    Feature("ai", "AI Coach", "AI Coach"),
    Feature("train", "Train Anywhere", "Overal Trainen"),
    Feature("session", "Session Player", "Sessie Speler"),
    Feature("compare", "Technique Compare", "Techniek Vergelijken"),
    Feature("classes", "Classes & Events", "Lessen & Events"),
    Feature("community", "Community", "Community"),
    Feature("progress", "Progress & Profile", "Voortgang & Profiel"),
    Feature("homework", "Homework", "Huiswerk"),
    Feature("notifications", "Notifications", "Meldingen"),
    Feature("history", "Training History", "Trainingsgeschiedenis"),
    Feature("challenges", "Challenges", "Uitdagingen"),
    Feature("fightcamp", "Fight Camp", "Fight Camp"),
    Feature("finance", "Membership & Payments", "Lidmaatschap & Betalen"),
    Feature("media", "Training Media", "Trainingsmedia"),
    Feature("book", "Trainer Book", "Boek van de Trainer"),
    Feature("vault", "Knowledge Vault", "Kennisbank"),
    Feature("private", "Private Lessons", "Privélessen"),
    Feature("support", "Support & Documents", "Support & Documenten"),
    Feature("settings", "Settings & Privacy", "Instellingen & Privacy")
)

private val adminFeatures = listOf(
    Feature("themes", "Visual Theme Studio", "Visueel Thema Studio"),
    Feature("access", "Access & Subscription Control", "Toegang & Abonnementen"),
    Feature("payments", "Payment Center", "Betaalcentrum"),
    Feature("members", "Student Manager", "Ledenbeheer"),
    Feature("classadmin", "Class Manager", "Lesbeheer"),
    Feature("attendance", "Attendance Manager", "Aanwezigheidsbeheer"),
    Feature("invoices", "Invoices", "Facturen"),
    Feature("homeworkadmin", "Homework Manager", "Huiswerkbeheer"),
    Feature("lessonadmin", "Lesson Editor", "Leseditor"),
    Feature("notifyadmin", "Notification Composer", "Meldingen Opstellen"),
    Feature("progressadmin", "Progress Manager", "Voortgangsbeheer"),
    Feature("bookadmin", "Book Manager", "Boekbeheer"),
    Feature("eventsadmin", "Event Manager", "Eventbeheer"),
    Feature("scheduleadmin", "Weekly Schedule", "Weekplanning"),
    Feature("sessionadmin", "Session Builder", "Sessie Bouwer"),
    Feature("fightcampadmin", "Fight Camp Manager", "Fight Camp Beheer")
)

private class Store(context: Context) {
    private val prefs = context.getSharedPreferences("rs_kickbox_v10", Context.MODE_PRIVATE)
    fun string(key: String, fallback: String = "") = prefs.getString(key, fallback) ?: fallback
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun bool(key: String, fallback: Boolean = false) = prefs.getBoolean(key, fallback)
    fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun int(key: String, fallback: Int = 0) = prefs.getInt(key, fallback)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
}

@Composable
fun RsKickboxV10App() {
    val context = LocalContext.current
    val store = remember { Store(context) }
    var role by remember { mutableStateOf<AppRole?>(null) }
    var route by remember { mutableStateOf("dashboard") }
    var lang by remember { mutableStateOf(if (store.string("lang", "EN") == "NL") AppLang.NL else AppLang.EN) }
    var themeId by remember {
        mutableStateOf(runCatching { ThemeId.valueOf(store.string("theme", ThemeId.ELITE_GOLD.name)) }.getOrDefault(ThemeId.ELITE_GOLD))
    }
    val theme = themes.first { it.id == themeId }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = theme.accent2,
            secondary = theme.accent,
            background = theme.bg,
            surface = theme.panel,
            onBackground = theme.text,
            onSurface = theme.text
        )
    ) {
        if (role == null) {
            LoginScreenV10(theme, lang, onLang = { lang = it; store.putString("lang", it.name) }) { selected ->
                role = selected
                route = if (selected == AppRole.TRAINER) "admin" else "dashboard"
            }
        } else {
            ShellV10(
                theme = theme,
                role = role!!,
                lang = lang,
                route = route,
                onLang = { lang = it; store.putString("lang", it.name) },
                onRoute = { route = it },
                onLogout = { role = null }
            ) {
                RouterV10(
                    theme = theme,
                    role = role!!,
                    lang = lang,
                    route = route,
                    store = store,
                    onRoute = { route = it },
                    onTheme = { themeId = it; store.putString("theme", it.name) }
                )
            }
        }
    }
}

@Composable
private fun LiveBackground(theme: AppTheme, strong: Boolean, content: @Composable BoxScope.() -> Unit) {
    val transition = rememberInfiniteTransition(label = "live")
    val motion by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (strong) 8000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "motion"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bg, theme.panel2.copy(alpha = .80f), theme.bg)))
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val opacity = if (strong) .22f else .07f
            drawCircle(
                color = theme.accent.copy(alpha = opacity),
                radius = size.minDimension * .30f,
                center = Offset(size.width * (.18f + .62f * motion), size.height * .24f)
            )
            drawCircle(
                color = theme.accent2.copy(alpha = opacity * .65f),
                radius = size.minDimension * .22f,
                center = Offset(size.width * (.80f - .48f * motion), size.height * .76f)
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (strong) .47f else .72f)))
        content()
    }
}

@Composable
private fun LoginScreenV10(theme: AppTheme, lang: AppLang, onLang: (AppLang) -> Unit, onLogin: (AppRole) -> Unit) {
    val nl = lang == AppLang.NL
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var password by remember { mutableStateOf("preview123") }
    LiveBackground(theme, strong = true) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("♛ RS KICKBOX", color = theme.accent2, fontSize = 31.sp, fontWeight = FontWeight.Black)
            LanguageV10(theme, lang, onLang)
            Text(if (nl) "Premium live kickboksplatform" else "Premium live kickboxing platform", color = theme.text, style = MaterialTheme.typography.headlineMedium)
            Text("TRAIN · LEARN · CONNECT · GROW", color = theme.muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
            PremiumCard(theme) {
                Text(if (nl) "LEDEN TOEGANG" else "MEMBER ACCESS", color = theme.accent2, fontWeight = FontWeight.Bold)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text(if (nl) "Wachtwoord" else "Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Button(onClick = { onLogin(AppRole.STUDENT) }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Leerling login" else "Student login") }
                OutlinedButton(onClick = { onLogin(AppRole.TRAINER) }, modifier = Modifier.fillMaxWidth()) { Text("Trainer / Admin") }
            }
            PremiumCard(theme) {
                Text("Van Stilte Naar Strijd", color = theme.accent2, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("Kickboksen, karakter en de weg van basis naar beheersing", color = theme.text)
                Text(if (nl) "Trainerboek · premium RS-leerlijn" else "Trainer book · premium RS learning path", color = theme.muted)
            }
        }
    }
}

@Composable
private fun ShellV10(
    theme: AppTheme,
    role: AppRole,
    lang: AppLang,
    route: String,
    onLang: (AppLang) -> Unit,
    onRoute: (String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val home = if (role == AppRole.TRAINER) "admin" else "dashboard"
    LiveBackground(theme, strong = false) {
        Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            PremiumCard(theme) {
                Text("♛ RS KICKBOX", color = theme.accent2, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(if (role == AppRole.TRAINER) "TRAINER / ADMIN" else "STUDENT", color = theme.muted, fontSize = 9.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    LanguageV10(theme, lang, onLang)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (route != home) {
                            OutlinedButton(onClick = { onRoute(home) }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp), modifier = Modifier.heightIn(min = 36.dp)) { Text("‹") }
                        }
                        OutlinedButton(onClick = onLogout, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp), modifier = Modifier.heightIn(min = 36.dp)) { Text(if (lang == AppLang.NL) "Uitloggen" else "Log out", fontSize = 10.sp) }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) { content() }
        }
    }
}

@Composable
private fun RouterV10(theme: AppTheme, role: AppRole, lang: AppLang, route: String, store: Store, onRoute: (String) -> Unit, onTheme: (ThemeId) -> Unit) {
    when (route) {
        "dashboard", "admin" -> DashboardV10(theme, role, lang, store, onRoute)
        "themes" -> ThemeStudioV10(theme, lang, store, onTheme)
        "access" -> AccessControlV10(theme, lang, store)
        "payments" -> PaymentCenterV10(theme, lang, store)
        "finance" -> MemberPaymentsV10(theme, lang, store)
        "academy" -> AcademyV10(theme, lang, store)
        "classes" -> ClassesV10(theme, lang, store)
        "session" -> SessionV10(theme, lang)
        else -> GenericV10(theme, lang, route, role, store)
    }
}

private fun planDefault(plan: Plan, id: String): Boolean = when (plan) {
    Plan.BASIC -> id in setOf("academy", "train", "classes", "progress", "history", "finance", "book", "support", "settings")
    Plan.PRO -> id !in setOf("private", "fightcamp")
    Plan.ELITE -> true
}

private fun allowedForStudent(store: Store, student: String, feature: String): Boolean {
    val plan = runCatching { Plan.valueOf(store.string("student_plan_$student", Plan.PRO.name)) }.getOrDefault(Plan.PRO)
    val planAllowed = store.bool("plan_${plan.name}_$feature", planDefault(plan, feature))
    val hasOverride = store.bool("has_override_${student}_$feature", false)
    return if (hasOverride) store.bool("override_${student}_$feature", planAllowed) else planAllowed
}

@Composable
private fun DashboardV10(theme: AppTheme, role: AppRole, lang: AppLang, store: Store, onRoute: (String) -> Unit) {
    val nl = lang == AppLang.NL
    val items = if (role == AppRole.TRAINER) adminFeatures else studentFeatures.filter { allowedForStudent(store, "Alex", it.id) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PremiumCard(theme) {
            Text(if (role == AppRole.TRAINER) "TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD", color = theme.accent2, fontWeight = FontWeight.Black)
            Text(
                if (role == AppRole.TRAINER) {
                    if (nl) "Volledige controle over thema, toegang, abonnementen en betalingen." else "Full control over theme, access, subscriptions and payments."
                } else {
                    if (nl) "Jouw persoonlijke RS-omgeving." else "Your personal RS environment."
                },
                color = theme.text,
                style = MaterialTheme.typography.titleLarge
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricV10(theme, if (role == AppRole.TRAINER) "Members" else "Level", if (role == AppRole.TRAINER) "124" else "3", Modifier.weight(1f))
                MetricV10(theme, if (role == AppRole.TRAINER) "Revenue" else "XP", if (role == AppRole.TRAINER) "€4,850" else "12,480", Modifier.weight(1f))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            items(items) { feature -> FeatureTileV10(theme, if (nl) feature.nl else feature.en) { onRoute(feature.id) } }
        }
    }
}

@Composable
private fun ThemeStudioV10(theme: AppTheme, lang: AppLang, store: Store, onTheme: (ThemeId) -> Unit) {
    val nl = lang == AppLang.NL
    ScrollV10(theme, if (nl) "Visueel Thema Studio" else "Visual Theme Studio", if (nl) "Kies de volledige premium look van de app." else "Choose the complete premium app look.") {
        themes.forEach { candidate ->
            PremiumCard(theme) {
                Text(candidate.name, color = theme.accent2, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(candidate.bg, candidate.panel, candidate.accent, candidate.accent2).forEach { color -> Surface(Modifier.size(24.dp), shape = CircleShape, color = color, border = BorderStroke(1.dp, Color.White.copy(alpha = .25f))) {} }
                }
                Text(
                    when (candidate.id) {
                        ThemeId.ELITE_GOLD -> "Black · gold · royal"
                        ThemeId.ROYAL_CRIMSON -> "Black · crimson · copper"
                        ThemeId.MIDNIGHT_PLATINUM -> "Midnight · platinum · ice"
                        ThemeId.EMERALD_COMBAT -> "Black · emerald · champagne"
                    },
                    color = theme.muted
                )
                Button(onClick = { onTheme(candidate.id); store.putString("theme", candidate.id.name) }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Activeer thema" else "Activate theme") }
            }
        }
        PremiumCard(theme) {
            Text(if (nl) "LIVE VISUAL REGELS" else "LIVE VISUAL RULES", color = theme.accent2, fontWeight = FontWeight.Bold)
            Text(if (nl) "Login/promotie krijgt sterkere beweging. Training gebruikt lage opacity en donkere scrim zodat tekst, timers en knoppen dominant blijven." else "Login/promotion gets stronger motion. Training uses low opacity and a dark scrim so text, timers and controls stay dominant.", color = theme.muted)
        }
    }
}

@Composable
private fun AccessControlV10(theme: AppTheme, lang: AppLang, store: Store) {
    val nl = lang == AppLang.NL
    var mode by remember { mutableStateOf("plans") }
    var student by remember { mutableStateOf("Alex") }
    ScrollV10(theme, if (nl) "Toegang & Abonnementen" else "Access & Subscription Control", if (nl) "Bepaal wat elke leerling kan zien en gebruiken." else "Control exactly what every student can see and use.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { mode = "plans" }, modifier = Modifier.weight(1f)) { Text(if (nl) "Plannen" else "Plans") }
            OutlinedButton(onClick = { mode = "student" }, modifier = Modifier.weight(1f)) { Text(if (nl) "Per leerling" else "Per student") }
        }
        if (mode == "plans") {
            Plan.entries.forEach { plan ->
                PremiumCard(theme) {
                    Text("RS ${plan.name}", color = theme.accent2, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    studentFeatures.forEach { feature ->
                        var enabled by remember(plan, feature.id) { mutableStateOf(store.bool("plan_${plan.name}_${feature.id}", planDefault(plan, feature.id))) }
                        ToggleV10(theme, if (nl) feature.nl else feature.en, enabled) { enabled = it; store.putBool("plan_${plan.name}_${feature.id}", it) }
                    }
                }
            }
        } else {
            PremiumCard(theme) {
                Text(if (nl) "Selecteer leerling" else "Select student", color = theme.accent2, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Alex", "Mila").forEach { name -> FilterChip(selected = student == name, onClick = { student = name }, label = { Text(name) }, modifier = Modifier.weight(1f)) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Noah", "Sara").forEach { name -> FilterChip(selected = student == name, onClick = { student = name }, label = { Text(name) }, modifier = Modifier.weight(1f)) }
                    }
                }
                var plan by remember(student) { mutableStateOf(runCatching { Plan.valueOf(store.string("student_plan_$student", Plan.PRO.name)) }.getOrDefault(Plan.PRO)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Plan.entries.forEach { item -> FilterChip(selected = plan == item, onClick = { plan = item; store.putString("student_plan_$student", item.name) }, label = { Text(item.name, fontSize = 10.sp) }, modifier = Modifier.weight(1f)) }
                }
            }
            PremiumCard(theme) {
                Text(if (nl) "Individuele overrides" else "Individual overrides", color = theme.accent2, fontWeight = FontWeight.Bold)
                Text(if (nl) "Deze schakelaars overschrijven het abonnement alleen voor deze leerling." else "These switches override the subscription only for this student.", color = theme.muted)
                studentFeatures.forEach { feature ->
                    var enabled by remember(student, feature.id) { mutableStateOf(allowedForStudent(store, student, feature.id)) }
                    ToggleV10(theme, if (nl) feature.nl else feature.en, enabled) {
                        enabled = it
                        store.putBool("has_override_${student}_${feature.id}", true)
                        store.putBool("override_${student}_${feature.id}", it)
                    }
                }
                TextButton(onClick = { studentFeatures.forEach { store.putBool("has_override_${student}_${it.id}", false) } }) { Text(if (nl) "Herstel abonnementstandaard" else "Reset to plan defaults") }
            }
        }
    }
}

@Composable
private fun PaymentCenterV10(theme: AppTheme, lang: AppLang, store: Store) {
    val nl = lang == AppLang.NL
    ScrollV10(theme, if (nl) "Betaalcentrum" else "Payment Center", if (nl) "Tijdelijke en toekomstige betaalmethoden beheren." else "Manage temporary and future payment methods.") {
        PremiumCard(theme) {
            Text(if (nl) "PRE-KVK MODUS" else "PRE-KVK MODE", color = theme.accent2, fontWeight = FontWeight.Black)
            Text(if (nl) "Gebruik alleen methoden die jouw bank/provider toestaat. Dit is geen manier om een wettelijke KVK-plicht te omzeilen." else "Use only methods allowed by your bank/provider. This is not a way to bypass a legal KVK registration duty.", color = theme.text)
            Text(if (nl) "Voor beperkte tijdelijke ontvangst zijn bankoverschrijving, contant en sommige particuliere betaalverzoeken praktisch. Voor reguliere betaalde abonnementen is zakelijke onboarding de juiste route zodra dat vereist is." else "For limited temporary receipts, bank transfer, cash and some personal payment requests can be practical. For regular paid subscriptions, business onboarding is the appropriate route once required.", color = theme.muted)
        }
        BillingFieldsV10(theme, lang, store)
        PaymentConfigV10(theme, store, "bank", "Bank transfer / SEPA", if (nl) "Geen app-processor; bankvoorwaarden kunnen gelden." else "No app processor; bank terms can still apply.", true, "IBAN / reference")
        PaymentConfigV10(theme, store, "cash", "Cash", if (nl) "Handmatige registratie en reconciliatie." else "Manual recording and reconciliation.", true, "Optional note")
        PaymentConfigV10(theme, store, "personal_request", if (nl) "Particulier betaalverzoek" else "Personal payment request", if (nl) "Alleen als dit past binnen particuliere provider-voorwaarden; niet als commerciële checkout." else "Only if allowed by personal provider terms; not as commercial checkout.", false, "Payment request URL")
        PaymentConfigV10(theme, store, "revolutme", "Revolut.me", if (nl) "Persoonlijke link met limieten; geen merchant checkout." else "Personal link with limits; not merchant checkout.", false, "https://revolut.me/...")
        PremiumCard(theme) {
            Text(if (nl) "NA KVK / MERCHANT ONBOARDING" else "AFTER KVK / MERCHANT ONBOARDING", color = theme.accent2, fontWeight = FontWeight.Black)
            Text(if (nl) "Later activeren: Tikkie Zakelijk, iDEAL/Wero, Revolut Business, kaarten/wallets en PayPal." else "Later activate: Tikkie Business, iDEAL/Wero, Revolut Business, cards/wallets and PayPal.", color = theme.muted)
        }
        PaymentConfigV10(theme, store, "tikkie", "Tikkie Business", if (nl) "Zakelijke onboarding en actuele tarieven." else "Business onboarding and current pricing.", false, "Business payment link")
        PaymentConfigV10(theme, store, "ideal", "iDEAL / Wero", if (nl) "Via PSP/acquirer; kosten verschillen per provider." else "Via PSP/acquirer; pricing varies by provider.", false, "PSP payment link")
        PaymentConfigV10(theme, store, "revolut_business", "Revolut Business", if (nl) "Merchant account; transactiekosten kunnen gelden." else "Merchant account; processing fees may apply.", false, "Merchant payment link")
        PaymentConfigV10(theme, store, "cards", "Cards / Apple Pay / Google Pay", if (nl) "Via PSP; verwerkingskosten." else "Via PSP; processing fees.", false, "Checkout link")
        PaymentConfigV10(theme, store, "paypal", "PayPal", if (nl) "Zakelijke betalingen hebben providerkosten." else "Commercial payments have provider fees.", false, "PayPal link")
    }
}

@Composable
private fun BillingFieldsV10(theme: AppTheme, lang: AppLang, store: Store) {
    val nl = lang == AppLang.NL
    PremiumCard(theme) {
        Text(if (nl) "Betaal- en factuurgegevens" else "Payment & billing details", color = theme.accent2, fontWeight = FontWeight.Bold)
        listOf(
            "display" to (if (nl) "Naam op betaling" else "Payment display name"),
            "iban" to "IBAN",
            "email" to (if (nl) "Factuur e-mail" else "Billing email"),
            "kvk" to "KvK (later)",
            "vat" to "VAT / BTW (later)"
        ).forEach { (key, label) ->
            var value by remember(key) { mutableStateOf(store.string("billing_$key")) }
            OutlinedTextField(value, { value = it; store.putString("billing_$key", it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
}

@Composable
private fun PaymentConfigV10(theme: AppTheme, store: Store, id: String, title: String, note: String, defaultOn: Boolean, placeholder: String) {
    var enabled by remember(id) { mutableStateOf(store.bool("pay_enabled_$id", defaultOn)) }
    var link by remember(id) { mutableStateOf(store.string("pay_link_$id")) }
    PremiumCard(theme) {
        ToggleV10(theme, title, enabled) { enabled = it; store.putBool("pay_enabled_$id", it) }
        Text(note, color = theme.muted, fontSize = 11.sp)
        OutlinedTextField(link, { link = it; store.putString("pay_link_$id", it) }, label = { Text(placeholder) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

@Composable
private fun MemberPaymentsV10(theme: AppTheme, lang: AppLang, store: Store) {
    val nl = lang == AppLang.NL
    val uri = LocalUriHandler.current
    ScrollV10(theme, if (nl) "Lidmaatschap & Betalen" else "Membership & Payments", if (nl) "Alleen door de trainer geactiveerde methoden zijn zichtbaar." else "Only trainer-enabled methods are visible.") {
        PremiumCard(theme) { Text("RS PRO", color = theme.accent2, fontSize = 28.sp, fontWeight = FontWeight.Black); Text("€49 / month", color = theme.text) }
        listOf(
            "bank" to "Bank transfer / SEPA",
            "cash" to "Cash",
            "personal_request" to (if (nl) "Particulier betaalverzoek" else "Personal payment request"),
            "revolutme" to "Revolut.me",
            "tikkie" to "Tikkie Business",
            "ideal" to "iDEAL / Wero",
            "revolut_business" to "Revolut Business",
            "cards" to "Cards / Wallets",
            "paypal" to "PayPal"
        ).forEach { (id, name) ->
            if (store.bool("pay_enabled_$id", id == "bank" || id == "cash")) {
                PremiumCard(theme) {
                    Text(name, color = theme.accent2, fontWeight = FontWeight.Bold)
                    val link = store.string("pay_link_$id")
                    if (id == "bank") {
                        Text(store.string("billing_iban", if (nl) "Stel IBAN in via Trainer Betaalcentrum" else "Configure IBAN in Trainer Payment Center"), color = theme.text)
                    } else if (link.startsWith("http")) {
                        Button(onClick = { runCatching { uri.openUri(link) } }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open betaling" else "Open payment") }
                    } else {
                        Text(if (nl) "Nog geen link ingesteld door trainer." else "No payment link configured by trainer yet.", color = theme.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademyV10(theme: AppTheme, lang: AppLang, store: Store) {
    val nl = lang == AppLang.NL
    ScrollV10(theme, "RS Academy", if (nl) "Techniekbibliotheek van basis naar beheersing." else "Technique library from fundamentals to mastery.") {
        listOf("Jab Fundamentals", "Roundhouse Kick", "Defense & Counters", "Footwork Flow", "Combination Builder").forEachIndexed { index, name ->
            var done by remember(index) { mutableStateOf(store.bool("lesson_$index")) }
            PremiumCard(theme) {
                Text(name, color = theme.accent2, fontWeight = FontWeight.Bold)
                Button(onClick = { done = true; store.putBool("lesson_$index", true) }, enabled = !done, modifier = Modifier.fillMaxWidth()) { Text(if (done) "Completed ✓" else if (nl) "Markeer voltooid" else "Mark complete") }
            }
        }
    }
}

@Composable
private fun ClassesV10(theme: AppTheme, lang: AppLang, store: Store) {
    val nl = lang == AppLang.NL
    ScrollV10(theme, if (nl) "Lessen & Events" else "Classes & Events", if (nl) "Boek je training." else "Book your training.") {
        listOf("Technique & Pads" to "Today · 19:00", "Fundamentals" to "Thu · 18:30", "Advanced Sparring" to "Fri · 20:00").forEachIndexed { index, item ->
            var booked by remember(index) { mutableStateOf(store.bool("class_$index")) }
            PremiumCard(theme) {
                Text(item.first, color = theme.accent2, fontWeight = FontWeight.Bold)
                Text(item.second, color = theme.muted)
                Button(onClick = { booked = !booked; store.putBool("class_$index", booked) }, modifier = Modifier.fillMaxWidth()) { Text(if (booked) if (nl) "Annuleer boeking" else "Cancel booking" else if (nl) "Boek les" else "Book class") }
            }
        }
    }
}

@Composable
private fun SessionV10(theme: AppTheme, lang: AppLang) {
    val nl = lang == AppLang.NL
    var running by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(120) }
    LaunchedEffect(running, seconds) {
        if (running && seconds > 0) { delay(1000); seconds-- }
        if (seconds == 0) running = false
    }
    ScrollV10(theme, if (nl) "Sessie Speler" else "Session Player", if (nl) "Subtiele live achtergrond, maximale leesbaarheid." else "Subtle live background, maximum readability.") {
        PremiumCard(theme) {
            Text("%d:%02d".format(seconds / 60, seconds % 60), color = theme.accent2, fontSize = 50.sp, fontWeight = FontWeight.Black)
            Text("Jab · Cross · Low Kick", color = theme.text)
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { seconds = 120; running = false }, modifier = Modifier.fillMaxWidth()) { Text("Reset") }
        }
    }
}

@Composable
private fun GenericV10(theme: AppTheme, lang: AppLang, route: String, role: AppRole, store: Store) {
    val feature = (if (role == AppRole.TRAINER) adminFeatures else studentFeatures).firstOrNull { it.id == route }
    val title = if (lang == AppLang.NL) feature?.nl else feature?.en
    ScrollV10(theme, title ?: route, if (lang == AppLang.NL) "Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout.") {
        repeat(3) { index ->
            PremiumCard(theme) {
                Text(listOf("Overview", "Actions", "Status")[index], color = theme.accent2, fontWeight = FontWeight.Bold)
                Text(if (lang == AppLang.NL) "Deze module is actief in de v0.10 preview en klaar voor backend-koppeling." else "This module is active in the v0.10 preview and ready for backend connection.", color = theme.muted)
            }
        }
        var saved by remember(route) { mutableStateOf(store.bool("generic_$route")) }
        Button(onClick = { saved = true; store.putBool("generic_$route", true) }, enabled = !saved, modifier = Modifier.fillMaxWidth()) { Text(if (saved) "Saved ✓" else if (lang == AppLang.NL) "Previewactie opslaan" else "Save preview action") }
    }
}

@Composable
private fun ScrollV10(theme: AppTheme, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = theme.accent2, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(subtitle, color = theme.muted)
        content()
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PremiumCard(theme: AppTheme, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, theme.accent2.copy(alpha = .28f)),
        colors = CardDefaults.cardColors(containerColor = theme.panel.copy(alpha = .96f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun FeatureTileV10(theme: AppTheme, title: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, theme.accent.copy(alpha = .38f)),
        colors = CardDefaults.cardColors(containerColor = theme.panel.copy(alpha = .94f))
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("♛", color = theme.accent2, fontSize = 20.sp)
            Text(title, color = theme.accent2, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Open module", color = theme.muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MetricV10(theme: AppTheme, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = theme.panel2, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, theme.accent.copy(alpha = .25f))) {
        Column(Modifier.padding(9.dp)) {
            Text(label.uppercase(), color = theme.muted, fontSize = 8.sp, maxLines = 1)
            Text(value, color = theme.accent2, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun ToggleV10(theme: AppTheme, label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = theme.text, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun LanguageV10(theme: AppTheme, lang: AppLang, onChange: (AppLang) -> Unit) {
    Surface(shape = RoundedCornerShape(15.dp), color = theme.panel2, border = BorderStroke(1.dp, theme.accent.copy(alpha = .38f))) {
        Row(Modifier.padding(2.dp)) {
            Text("EN", Modifier.clickable { onChange(AppLang.EN) }.padding(horizontal = 8.dp, vertical = 5.dp), color = if (lang == AppLang.EN) theme.accent2 else theme.muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("NL", Modifier.clickable { onChange(AppLang.NL) }.padding(horizontal = 8.dp, vertical = 5.dp), color = if (lang == AppLang.NL) theme.accent2 else theme.muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

package com.rskickbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RsBlack = Color(0xFF050505)
private val RsPanel = Color(0xFF15120D)
private val RsPanel2 = Color(0xFF211A10)
private val RsGold = Color(0xFFC08A24)
private val RsGoldLight = Color(0xFFF0CF79)
private val RsCream = Color(0xFFF6F0E4)
private val RsMuted = Color(0xFFB8AD98)
private val RsGreen = Color(0xFF67C587)

private enum class Role { STUDENT, TRAINER }
private enum class Lang { EN, NL }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
            ) { RsKickboxApp() }
        }
    }
}

@Composable
private fun RsKickboxApp() {
    var role by remember { mutableStateOf<Role?>(null) }
    var current by remember { mutableStateOf("Dashboard") }
    var lang by remember { mutableStateOf(Lang.EN) }
    if (role == null) {
        LoginScreen(lang, onLanguage = { lang = it }) { selected ->
            role = selected
            current = if (selected == Role.TRAINER) "Trainer Dashboard" else "Dashboard"
        }
    } else {
        AppShell(role!!, current, lang, onOpen = { current = it }, onBack = { current = if (role == Role.TRAINER) "Trainer Dashboard" else "Dashboard" }, onLanguage = { lang = it }, onLogout = { role = null })
    }
}

@Composable
private fun Background(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(RsBlack, Color(0xFF090806), Color(0xFF110D07)))
        ), content = content
    )
}

@Composable
private fun LoginScreen(lang: Lang, onLanguage: (Lang) -> Unit, onLogin: (Role) -> Unit) {
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var password by remember { mutableStateOf("preview123") }
    val nl = lang == Lang.NL
    Background {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("♛ RS KICKBOX", color = RsGoldLight, fontSize = 30.sp, fontWeight = FontWeight.Black)
                LanguageSwitch(lang, onLanguage)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (nl) "Premium kickboks platform" else "Premium kickboxing platform", color = RsCream, style = MaterialTheme.typography.headlineSmall)
                Text("TRAIN · LEARN · CONNECT · GROW", color = RsMuted, letterSpacing = 2.sp, fontSize = 11.sp)
            }
            GoldCard {
                Eyebrow(if (nl) "Leden toegang" else "Member Access")
                Text(if (nl) "Welkom bij de RS-familie." else "Welcome to the RS family.", color = RsGoldLight, style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text(if (nl) "Wachtwoord" else "Password") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onLogin(Role.STUDENT) }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Leerling login" else "Student Login") }
                OutlinedButton(onClick = { onLogin(Role.TRAINER) }, modifier = Modifier.fillMaxWidth()) { Text("Trainer / Admin") }
                HorizontalDivider(color = RsGold.copy(alpha = .3f))
                Eyebrow(if (nl) "Boek van de trainer" else "Trainer Book Spotlight")
                Text("Van Stilte Naar Strijd", color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text("Kickboksen, karakter en de weg van basis naar beheersing", color = RsMuted)
            }
        }
    }
}

@Composable
private fun AppShell(role: Role, current: String, lang: Lang, onOpen: (String) -> Unit, onBack: () -> Unit, onLanguage: (Lang) -> Unit, onLogout: () -> Unit) {
    Background {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("♛ RS KICKBOX", color = RsGoldLight, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    Text(if (role == Role.TRAINER) "TRAINER / ADMIN" else "STUDENT", color = RsMuted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    LanguageSwitch(lang, onLanguage)
                    if (current != "Dashboard" && current != "Trainer Dashboard") TextButton(onClick = onBack) { Text("‹") }
                    TextButton(onClick = onLogout) { Text(if (lang == Lang.NL) "Uit" else "Logout") }
                }
            }
            Box(Modifier.weight(1f)) {
                ScreenRouter(role, current, lang, onOpen)
            }
        }
    }
}

@Composable
private fun ScreenRouter(role: Role, current: String, lang: Lang, onOpen: (String) -> Unit) {
    when (current) {
        "Dashboard", "Trainer Dashboard" -> Dashboard(role, lang, onOpen)
        "Classes & Events" -> ClassesScreen(lang)
        "Community" -> CommunityScreen(lang)
        "Progress & Profile" -> ProgressScreen(lang)
        "Membership & Finance" -> FinanceScreen(lang)
        "Fight Camp" -> FightCampScreen(lang)
        "Challenges" -> ChallengesScreen(lang)
        "Settings & Privacy" -> SettingsScreen(lang)
        "Trainer Book", "Book Manager" -> BookScreen(lang, role == Role.TRAINER)
        "Student Manager" -> StudentManagerScreen(lang)
        else -> PlaceholderScreen(current, lang)
    }
}

@Composable
private fun Dashboard(role: Role, lang: Lang, onOpen: (String) -> Unit) {
    val nl = lang == Lang.NL
    val studentFeatures = listOf("RS Academy", "AI Coach", "Session Player", "Fight Camp", "Classes & Events", "Community", "Trainer Book", "Challenges", "Training History", "Membership & Finance", "Progress & Profile", "Settings & Privacy")
    val trainerFeatures = listOf("Student Manager", "Class Manager", "Attendance Manager", "Invoices & Payments", "Homework Manager", "Music Manager", "Book Manager", "Progress Manager", "Coach Assessments", "Challenge Manager", "Fight Camp Manager", "Landing Page Manager")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GoldCard {
            Eyebrow(if (role == Role.TRAINER) "Trainer Control Center" else if (nl) "RS live dashboard" else "RS Live Kickboxing Dashboard")
            Text(
                if (role == Role.TRAINER) if (nl) "Beheer het complete RS-platform." else "Run the complete RS platform."
                else if (nl) "Train in een premium RS kickbokswereld." else "Train inside a premium RS kickboxing world.",
                color = RsGoldLight, style = MaterialTheme.typography.headlineSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(if (role == Role.TRAINER) if (nl) "Leden" else "Members" else "Level", if (role == Role.TRAINER) "124" else "3", Modifier.weight(1f))
                Metric(if (role == Role.TRAINER) "Revenue" else "XP", if (role == Role.TRAINER) "€4,850" else "12,480", Modifier.weight(1f))
            }
        }
        LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
            items(if (role == Role.TRAINER) trainerFeatures else studentFeatures) { feature -> FeatureCard(feature) { onOpen(feature) } }
        }
    }
}

@Composable
private fun ClassesScreen(lang: Lang) {
    var booked by remember { mutableStateOf(setOf("Technique & Pads")) }
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "Lessen & Events" else "Classes & Events", if (nl) "Boek je training en volg je planning." else "Book your training and follow your schedule.") {
        listOf(
            Triple("Technique & Pads", "Today · 19:00", "12/16"),
            Triple("Kickboxing Fundamentals", "Thu · 18:30", "9/16"),
            Triple("Advanced Sparring", "Fri · 20:00", "14/14")
        ).forEach { (name,time,capacity) ->
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$time   •   $capacity", color = RsMuted)
                if (capacity == "14/14") AssistChip(onClick = {}, label = { Text(if (nl) "Wachtlijst" else "Waitlist") })
                else Button(onClick = { booked = if (name in booked) booked - name else booked + name }, modifier = Modifier.fillMaxWidth()) { Text(if (name in booked) if (nl) "Geboekt ✓" else "Booked ✓" else if (nl) "Boek les" else "Book class") }
            }
        }
    }
}

@Composable
private fun CommunityScreen(lang: Lang) {
    var liked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "RS Community" else "RS Community", if (nl) "Teamupdates, motivatie en clubnieuws." else "Team updates, motivation and club news.") {
        GoldCard {
            Eyebrow(if (nl) "Trainer update" else "Trainer update")
            Text(if (nl) "Sterke training vanavond. Blijf technisch scherp en help elkaar groeien." else "Strong session tonight. Stay technically sharp and help each other grow.", color = RsCream)
            TextButton(onClick = { liked = !liked }) { Text(if (liked) "♥ 25" else "♡ 24") }
        }
        GoldCard {
            Eyebrow(if (nl) "Groepschat" else "Group Chat")
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
    ScrollScreen(if (nl) "Voortgang & Profiel" else "Progress & Profile", if (nl) "Jouw ontwikkeling van basis naar beheersing." else "Your development from fundamentals to mastery.") {
        GoldCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Level", "3", Modifier.weight(1f)); Metric("XP", "12,480", Modifier.weight(1f)); Metric(if (nl) "Streak" else "Streak", "18d", Modifier.weight(1f))
            }
            LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth())
            Text(if (nl) "520 XP tot level 4" else "520 XP to level 4", color = RsMuted)
        }
        listOf("Punches" to .82f, "Kicks" to .71f, "Defense" to .66f, "Footwork" to .74f, "Combinations" to .63f, "Conditioning" to .79f).forEach { (skill,score) ->
            GoldCard { Text(skill, color = RsGoldLight, fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth()); Text("${(score*100).toInt()}%", color = RsMuted) }
        }
    }
}

@Composable
private fun FinanceScreen(lang: Lang) {
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "Lidmaatschap & Financiën" else "Membership & Finance", if (nl) "Beheer je abonnement en betalingen." else "Manage your plan and payments.") {
        GoldCard {
            Eyebrow(if (nl) "Actief plan" else "Active plan")
            Text("RS PRO", color = RsGoldLight, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("€49 / month", color = RsCream)
            StatusPill(if (nl) "Actief" else "Active")
        }
        listOf("September 2026" to "€49 · Paid", "August 2026" to "€49 · Paid", "July 2026" to "€49 · Paid").forEach { (month,status) ->
            GoldCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(month, color = RsCream); Text(status, color = RsGreen) } }
        }
    }
}

@Composable
private fun FightCampScreen(lang: Lang) {
    var completed by remember { mutableStateOf(2) }
    val nl = lang == Lang.NL
    ScrollScreen("Fight Camp", if (nl) "Een gestructureerde route naar wedstrijdvoorbereiding." else "A structured road to fight preparation.") {
        GoldCard {
            Eyebrow(if (nl) "Camp voortgang" else "Camp progress")
            Text("RS Fight Camp · 8 Weeks", color = RsGoldLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            LinearProgressIndicator(progress = { completed / 8f }, modifier = Modifier.fillMaxWidth())
            Text("Week $completed / 8", color = RsMuted)
            Button(onClick = { if (completed < 8) completed++ }, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Week afronden" else "Complete week") }
        }
        listOf("Technical sharpness", "Conditioning", "Controlled sparring", "Recovery & mindset").forEach { item -> GoldCard { Text("♛ $item", color = RsCream) } }
    }
}

@Composable
private fun ChallengesScreen(lang: Lang) {
    val nl = lang == Lang.NL
    var done by remember { mutableStateOf(setOf<String>()) }
    ScrollScreen(if (nl) "Uitdagingen" else "Challenges", if (nl) "Verdien XP met technische en mentale doelen." else "Earn XP with technical and mindset goals.") {
        listOf("100 clean jabs" to 250, "3 training sessions" to 400, "Defense drill streak" to 300, "Recovery discipline" to 200).forEach { (name,xp) ->
            GoldCard {
                Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text("+$xp XP", color = RsMuted)
                Button(onClick = { done = done + name }, enabled = name !in done, modifier = Modifier.fillMaxWidth()) { Text(if (name in done) if (nl) "Voltooid ✓" else "Completed ✓" else if (nl) "Markeer voltooid" else "Mark complete") }
            }
        }
    }
}

@Composable
private fun SettingsScreen(lang: Lang) {
    var notifications by remember { mutableStateOf(true) }
    var privateProfile by remember { mutableStateOf(false) }
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "Instellingen & Privacy" else "Settings & Privacy", if (nl) "Jouw appvoorkeuren en privacy." else "Your app preferences and privacy.") {
        GoldCard {
            SettingToggle(if (nl) "Pushmeldingen" else "Push notifications", notifications) { notifications = it }
            SettingToggle(if (nl) "Privé profiel" else "Private profile", privateProfile) { privateProfile = it }
        }
        GoldCard {
            Eyebrow(if (nl) "Gegevens" else "Data")
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Download mijn gegevens" else "Download my data") }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Privacycentrum" else "Privacy center") }
        }
    }
}

@Composable
private fun BookScreen(lang: Lang, admin: Boolean) {
    val nl = lang == Lang.NL
    var promo by remember { mutableStateOf(true) }
    ScrollScreen(if (admin) "Book Manager" else "Van Stilte Naar Strijd", if (nl) "Kickboksen, karakter en de weg van basis naar beheersing" else "Kickboxing, character and the road from fundamentals to mastery") {
        GoldCard {
            Eyebrow(if (admin) "RS Book Manager" else if (nl) "Boek van de trainer" else "Trainer Book")
            Text("Van Stilte Naar Strijd", color = RsGoldLight, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Kickboksen, karakter en de weg van basis naar beheersing", color = RsCream)
            Text(if (nl) "Van houding, stoten, trappen en knieën tot verdediging, combinaties, conditie, herstel, sparren en wedstrijdvoorbereiding — met discipline, respect en zelfbeheersing als rode draad." else "From stance, punches, kicks and knees to defense, combinations, conditioning, recovery, sparring and fight preparation — with discipline, respect and self-control throughout.", color = RsMuted)
        }
        GoldCard {
            Eyebrow(if (nl) "App extras" else "App extras")
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
private fun StudentManagerScreen(lang: Lang) {
    val nl = lang == Lang.NL
    ScrollScreen(if (nl) "Ledenbeheer" else "Student Manager", if (nl) "Teamoverzicht en voortgang." else "Team overview and progress.") {
        listOf(Triple("Alex de Vries", "Level 3", "92%"), Triple("Mila Jansen", "Level 4", "96%"), Triple("Noah Bakker", "Level 2", "84%"), Triple("Sara Visser", "Level 3", "89%")).forEach { (name,level,attendance) ->
            GoldCard { Text(name, color = RsGoldLight, fontWeight = FontWeight.Bold); Text("$level · ${if (nl) "Aanwezigheid" else "Attendance"} $attendance", color = RsMuted); OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(if (nl) "Open profiel" else "Open profile") } }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, lang: Lang) {
    ScrollScreen(title, if (lang == Lang.NL) "Deze module is verbonden met het native RS-platform en wordt verder uitgebreid." else "This module is connected to the native RS platform and will be expanded further.") {
        GoldCard {
            Text("♛", color = RsGoldLight, fontSize = 42.sp)
            Text(title, color = RsGoldLight, style = MaterialTheme.typography.headlineSmall)
            Text(if (lang == Lang.NL) "Native Android module actief." else "Native Android module active.", color = RsMuted)
        }
    }
}

@Composable
private fun ScrollScreen(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = RsGoldLight, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(subtitle, color = RsMuted)
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GoldCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .28f)), colors = CardDefaults.cardColors(containerColor = RsPanel.copy(alpha = .98f))) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
private fun FeatureCard(title: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, RsGold.copy(alpha = .30f)), colors = CardDefaults.cardColors(containerColor = Color(0xFF11100E))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("♛", color = RsGoldLight, fontSize = 22.sp)
            Text(title, color = RsGoldLight, fontWeight = FontWeight.Bold)
            Text("Open RS feature", color = RsMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 4.dp)) { Text(label.uppercase(), color = RsMuted, fontSize = 9.sp, letterSpacing = 1.sp); Text(value, color = RsGoldLight, fontSize = 22.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun Eyebrow(text: String) { Text(text.uppercase(), color = RsGoldLight, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun StatusPill(text: String) { Surface(color = RsGreen.copy(alpha = .15f), shape = RoundedCornerShape(20.dp)) { Text(text, color = RsGreen, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold) } }

@Composable
private fun SettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = RsCream); Switch(checked = value, onCheckedChange = onChange) }
}

@Composable
private fun LanguageSwitch(lang: Lang, onLanguage: (Lang) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = RsPanel2, border = BorderStroke(1.dp, RsGold.copy(alpha=.35f))) {
        Row(Modifier.padding(3.dp)) {
            Text("EN", modifier = Modifier.clickable { onLanguage(Lang.EN) }.padding(horizontal = 8.dp, vertical = 5.dp), color = if (lang == Lang.EN) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("NL", modifier = Modifier.clickable { onLanguage(Lang.NL) }.padding(horizontal = 8.dp, vertical = 5.dp), color = if (lang == Lang.NL) RsGoldLight else RsMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

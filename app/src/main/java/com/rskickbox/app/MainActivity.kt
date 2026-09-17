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
import androidx.compose.foundation.shape.RoundedCornerShape
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
private val RsGold = Color(0xFFC08A24)
private val RsGoldLight = Color(0xFFF0CF79)
private val RsCream = Color(0xFFF6F0E4)
private val RsMuted = Color(0xFFB8AD98)

private enum class Role { STUDENT, TRAINER }

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
            ) {
                RsKickboxApp()
            }
        }
    }
}

@Composable
private fun RsKickboxApp() {
    var role by remember { mutableStateOf<Role?>(null) }
    var current by remember { mutableStateOf("Dashboard") }

    if (role == null) {
        LoginScreen(onLogin = { selected -> role = selected; current = if (selected == Role.TRAINER) "Trainer Dashboard" else "Dashboard" })
    } else {
        DashboardShell(role = role!!, current = current, onOpen = { current = it }, onLogout = { role = null })
    }
}

@Composable
private fun Background(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(RsBlack, Color(0xFF090806), Color(0xFF110D07)))
        ),
        content = content
    )
}

@Composable
private fun LoginScreen(onLogin: (Role) -> Unit) {
    var email by remember { mutableStateOf("alex@rskickbox.nl") }
    var password by remember { mutableStateOf("preview123") }
    Background {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(RsGold.copy(alpha = .22f), Color.Transparent),
                    radius = 900f
                )
            )
        )
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("♛ RS KICKBOX", color = RsGoldLight, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text("Premium kickboxing platform", color = RsCream, style = MaterialTheme.typography.headlineSmall)
                Text("TRAIN · LEARN · CONNECT · GROW", color = RsMuted, letterSpacing = 2.sp, fontSize = 11.sp)
            }

            GoldCard {
                Eyebrow("Member Access")
                Text("Welcome to the RS family.", color = RsGoldLight, style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onLogin(Role.STUDENT) }, modifier = Modifier.fillMaxWidth()) { Text("Student Login") }
                OutlinedButton(onClick = { onLogin(Role.TRAINER) }, modifier = Modifier.fillMaxWidth()) { Text("Trainer / Admin") }
                HorizontalDivider(color = RsGold.copy(alpha = .3f))
                Eyebrow("Trainer Book Spotlight")
                Text("Van Stilte Naar Strijd", color = RsGoldLight, fontWeight = FontWeight.Bold)
                Text("Kickboksen, karakter en de weg van basis naar beheersing", color = RsMuted)
            }
        }
    }
}

@Composable
private fun DashboardShell(role: Role, current: String, onOpen: (String) -> Unit, onLogout: () -> Unit) {
    val studentFeatures = listOf(
        "RS Academy", "AI Coach", "Session Player", "Fight Camp",
        "Classes & Events", "Community", "Trainer Book", "Challenges",
        "Training History", "Membership & Finance", "Progress & Profile", "Settings & Privacy"
    )
    val trainerFeatures = listOf(
        "Student Manager", "Class Manager", "Attendance Manager", "Invoices & Payments",
        "Homework Manager", "Music Manager", "Book Manager", "Progress Manager",
        "Coach Assessments", "Challenge Manager", "Fight Camp Manager", "Landing Page Manager"
    )

    Background {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("♛ RS KICKBOX", color = RsGoldLight, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text(if (role == Role.TRAINER) "TRAINER / ADMIN" else "STUDENT", color = RsMuted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                OutlinedButton(onClick = onLogout) { Text("Logout") }
            }

            GoldCard {
                Eyebrow(if (role == Role.TRAINER) "Trainer Control Center" else "RS Live Kickboxing Dashboard")
                Text(
                    if (role == Role.TRAINER) "Run the complete RS platform." else "Train inside a premium RS kickboxing world.",
                    color = RsGoldLight,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("Current screen: $current", color = RsMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric(if (role == Role.TRAINER) "Members" else "Level", if (role == Role.TRAINER) "124" else "3", Modifier.weight(1f))
                    Metric(if (role == Role.TRAINER) "Revenue" else "XP", if (role == Role.TRAINER) "€4,850" else "12,480", Modifier.weight(1f))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(if (role == Role.TRAINER) trainerFeatures else studentFeatures) { feature ->
                    FeatureCard(feature) { onOpen(feature) }
                }
            }
        }
    }
}

@Composable
private fun GoldCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, RsGoldLight.copy(alpha = .28f)),
        colors = CardDefaults.cardColors(containerColor = RsPanel.copy(alpha = .98f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun FeatureCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, RsGold.copy(alpha = .30f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11100E))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("♛", color = RsGoldLight, fontSize = 22.sp)
            Text(title, color = RsGoldLight, fontWeight = FontWeight.Bold)
            Text("Open RS feature", color = RsMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = RsMuted, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = RsGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text.uppercase(), color = RsGoldLight, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

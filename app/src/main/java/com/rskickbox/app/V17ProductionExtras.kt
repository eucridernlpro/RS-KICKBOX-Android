package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RsAccountSecurityV17(c: RsPalette, s: RsStore) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(s.b("security_biometric", false)) }
    var appLock by remember { mutableStateOf(s.b("security_app_lock", true)) }
    var loginAlerts by remember { mutableStateOf(s.b("security_login_alerts", true)) }

    RsScroll(c, "Account & Security", "Security controls prepared for production authentication. Preview actions are local until the real auth backend is connected.") {
        RsPanel(c) {
            Text("SECURITY STATUS", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Preview account · alex@rskickbox.nl", color = c.text)
            Text("Production target: verified email, secure server session, password reset, device/session management and optional biometric unlock.", color = c.muted)
        }
        RsPanel(c) {
            Text("DEVICE SECURITY", color = c.bright, fontWeight = FontWeight.Bold)
            listOf(
                Triple("App lock", appLock) { v: Boolean -> appLock = v; s.pb("security_app_lock", v) },
                Triple("Biometric unlock", biometric) { v: Boolean -> biometric = v; s.pb("security_biometric", v) },
                Triple("New login alerts", loginAlerts) { v: Boolean -> loginAlerts = v; s.pb("security_login_alerts", v) }
            ).forEach { (label, enabled, change) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = c.text, modifier = Modifier.weight(1f))
                    Switch(enabled, change)
                }
            }
        }
        RsPanel(c) {
            Text("CHANGE PASSWORD", color = c.bright, fontWeight = FontWeight.Bold)
            OutlinedTextField(currentPassword, { currentPassword = it }, label = { Text("Current password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Confirm new password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                result = when {
                    currentPassword.isBlank() -> "Enter your current password."
                    newPassword.length < 8 -> "Use at least 8 characters for this preview."
                    newPassword != confirmPassword -> "The new passwords do not match."
                    else -> "Password-change flow validated locally. Production change will be handled securely by the authentication server."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Validate password change") }
            if (result.isNotBlank()) Text(result, color = c.muted)
        }
        RsPanel(c) {
            var resetSent by remember { mutableStateOf(false) }
            Text("RECOVERY & SESSIONS", color = c.bright, fontWeight = FontWeight.Bold)
            Button(onClick = { resetSent = true }, modifier = Modifier.fillMaxWidth()) { Text("Send password-reset preview") }
            OutlinedButton(onClick = { result = "Other-session sign-out queued locally. Real token revocation requires production authentication." }, modifier = Modifier.fillMaxWidth()) { Text("Sign out other devices") }
            if (resetSent) Text("Reset request prepared for alex@rskickbox.nl", color = c.muted)
        }
    }
}

@Composable
fun RsOnboardingV17(c: RsPalette, s: RsStore) {
    var step by remember { mutableIntStateOf(s.s("onboarding_step", "1").toIntOrNull()?.coerceIn(1, 5) ?: 1) }
    val titles = listOf("Welcome", "Training goals", "Experience", "Preferences", "Ready")
    RsScroll(c, "Member Onboarding", "Five-step setup preview for a polished first launch.") {
        RsPanel(c) {
            Text("STEP $step / 5 · ${titles[step - 1]}", color = c.bright, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { step / 5f }, modifier = Modifier.fillMaxWidth())
            Text(when (step) {
                1 -> "Choose language, confirm club invitation and review the member experience."
                2 -> "Select goals such as fundamentals, fitness, confidence, competition preparation or technique improvement."
                3 -> "Set beginner/intermediate/advanced experience so lessons and suggestions start at the right level."
                4 -> "Choose reminders, AI voice, privacy preferences and preferred training days."
                else -> "Review settings and enter the RS dashboard."
            }, color = c.text)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (step > 1) { step--; s.ps("onboarding_step", step.toString()) } }, enabled = step > 1, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(onClick = { if (step < 5) step++ else step = 1; s.ps("onboarding_step", step.toString()) }, modifier = Modifier.weight(1f)) { Text(if (step == 5) "Finish preview" else "Next") }
            }
        }
        RsPanel(c) {
            Text("PRODUCTION ONBOARDING", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Final onboarding will connect to the authenticated member profile and server-side subscription/access permissions.", color = c.muted)
        }
    }
}

@Composable
fun RsSyncStatusV17(c: RsPalette, s: RsStore) {
    var syncing by remember { mutableStateOf(false) }
    var lastSync by remember { mutableStateOf(s.s("last_sync_preview", "Not synced yet")) }
    RsScroll(c, "Data & Sync Center", "Preview of the production data-health and synchronization area.") {
        RsPanel(c) {
            Text("CURRENT MODE", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Local preview storage", color = c.text)
            Text("The APK currently keeps preview preferences on this device. Shared cloud data is intentionally not claimed as active yet.", color = c.muted)
        }
        RsPanel(c) {
            Text("SYNC HEALTH", color = c.bright, fontWeight = FontWeight.Bold)
            listOf(
                "Member profile" to "LOCAL READY",
                "Subscription permissions" to "BACKEND REQUIRED",
                "Class bookings" to "BACKEND REQUIRED",
                "Messages / notifications" to "BACKEND REQUIRED",
                "Training media" to "CLOUD STORAGE REQUIRED",
                "Payments" to "PROVIDER + WEBHOOK REQUIRED"
            ).forEach { (name, state) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = c.text, modifier = Modifier.weight(1f))
                    Text(state, color = c.bright)
                }
            }
        }
        RsPanel(c) {
            Button(onClick = {
                syncing = true
                lastSync = "Local preview check complete"
                s.ps("last_sync_preview", lastSync)
                syncing = false
            }, modifier = Modifier.fillMaxWidth()) { Text(if (syncing) "Checking…" else "Run local data check") }
            Text("Last check: $lastSync", color = c.muted)
        }
    }
}

@Composable
fun RsBackendReadinessV17(c: RsPalette) {
    RsScroll(c, "Production Integration Center", "Trainer view of what is connected and what still requires production services or credentials.") {
        listOf(
            Triple("Android client", "READY", "Native app, navigation, responsive layouts and preview interactions"),
            Triple("Authentication", "OPEN", "Connect secure account provider and token/session handling"),
            Triple("Database", "OPEN", "Connect shared member, class, access and progress data"),
            Triple("Media storage", "OPEN", "Authenticated upload/download with access rules"),
            Triple("Push notifications", "OPEN", "Connect delivery service and device tokens"),
            Triple("AI Coach backend", "OPEN", "Protected server API for real AI coaching responses"),
            Triple("Payments", "OPEN", "Activate provider account, checkout and webhook reconciliation"),
            Triple("Play Store signing", "OPEN", "Production key, signed AAB and release track")
        ).forEach { (name, state, body) ->
            RsPanel(c) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = c.bright, fontWeight = FontWeight.Bold)
                    Text(state, color = if (state == "READY") c.bright else c.muted, fontWeight = FontWeight.Bold)
                }
                Text(body, color = c.muted)
            }
        }
    }
}

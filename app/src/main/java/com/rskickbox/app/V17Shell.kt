package com.rskickbox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RsKickboxV17App() {
    val context = LocalContext.current
    val store = remember { RsStore(context) }
    var toolsOpen by remember { mutableStateOf(false) }
    var toolPage by remember { mutableStateOf("hub") }
    val theme = runCatching { RsTheme.valueOf(store.s("theme", "ELITE_GOLD")) }.getOrDefault(RsTheme.ELITE_GOLD)
    val c = paletteFor(theme)

    Box(Modifier.fillMaxSize()) {
        RsKickboxV16App()

        if (!toolsOpen) {
            SmallFloatingActionButton(
                onClick = { toolsOpen = true; toolPage = "hub" },
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                containerColor = c.panel,
                contentColor = c.bright
            ) { Text("⚙") }
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = c.bg
            ) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RsPanel(c) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("RS PRODUCTION TOOLS", color = c.bright, fontWeight = FontWeight.Black)
                                Text("v0.17 preview hardening", color = c.muted)
                            }
                            OutlinedButton(onClick = {
                                if (toolPage == "hub") toolsOpen = false else toolPage = "hub"
                            }) { Text(if (toolPage == "hub") "Close" else "Back") }
                        }
                    }
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        when (toolPage) {
                            "security" -> RsAccountSecurityV17(c, store)
                            "onboarding" -> RsOnboardingV17(c, store)
                            "sync" -> RsSyncStatusV17(c, store)
                            "backend" -> RsBackendReadinessV17(c)
                            else -> RsProductionHubV17(c) { toolPage = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RsProductionHubV17(c: RsPalette, onOpen: (String) -> Unit) {
    RsScroll(c, "Production Hardening", "Preview-only control center while the final backend/release layer is being connected.") {
        RsPanel(c) {
            Text("ACCOUNT & SECURITY", color = c.bright, fontWeight = FontWeight.Bold)
            Text("App lock, biometric preference, login alerts, password-change validation, reset and device-session controls.", color = c.muted)
            Button(onClick = { onOpen("security") }, modifier = Modifier.fillMaxWidth()) { Text("Open security center") }
        }
        RsPanel(c) {
            Text("MEMBER ONBOARDING", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Five-step first-launch flow for language, goals, experience and preferences.", color = c.muted)
            Button(onClick = { onOpen("onboarding") }, modifier = Modifier.fillMaxWidth()) { Text("Test onboarding") }
        }
        RsPanel(c) {
            Text("DATA & SYNC", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Clear separation between locally working preview data and services that still require the production backend.", color = c.muted)
            Button(onClick = { onOpen("sync") }, modifier = Modifier.fillMaxWidth()) { Text("Open sync center") }
        }
        RsPanel(c) {
            Text("PRODUCTION INTEGRATION", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Status for Android client, auth, database, media, push, AI, payments and Play Store signing.", color = c.muted)
            Button(onClick = { onOpen("backend") }, modifier = Modifier.fillMaxWidth()) { Text("Open integration center") }
        }
        RsPanel(c) {
            Text("WHY THIS BUTTON IS TEMPORARY", color = c.bright, fontWeight = FontWeight.Bold)
            Text("The floating gear exists only in acceptance previews so these release-hardening screens can be tested without replacing the existing student/trainer navigation. It will be removed or trainer-gated before the production release.", color = c.muted)
        }
    }
}

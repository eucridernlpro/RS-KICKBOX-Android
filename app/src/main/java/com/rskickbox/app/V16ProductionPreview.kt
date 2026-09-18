package com.rskickbox.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun RsStatusBannerV16(c: RsPalette, message: String) {
    if (message.isNotBlank()) {
        RsPanel(c) {
            Text("✓ $message", color = c.bright, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RsTrainingMediaV16(c: RsPalette, s: RsStore) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf(s.s("media_last_name", "")) }
    fun saveMedia(uri:Uri,persist:Boolean){
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        selectedName=uri.lastPathSegment?:"Selected media"
        s.ps("media_last_uri",uri.toString())
        s.ps("media_last_name",selectedName)
        status="Media selected."
    }
    val galleryPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri:Uri?->if(uri!=null)saveMedia(uri,false)}
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null)saveMedia(uri,true)}

    RsScroll(c, "Training Media", "Coach videos, drills and member uploads with device-library selection.") {
        RsPanel(c) {
            Text("UPLOAD / SELECT MEDIA", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Choose a training video or image from phone, tablet, PC-compatible document provider or connected cloud library.", color = c.muted)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Button(
                    onClick={galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))},
                    modifier=Modifier.weight(1f)
                ){Text("Gallery")}
                OutlinedButton(onClick={filePicker.launch(arrayOf("video/*","image/*"))},modifier=Modifier.weight(1f)){Text("Files")}
            }
            if (selectedName.isNotBlank()) {
                Text("Selected: $selectedName", color = c.text)
                OutlinedButton(onClick = {
                    s.ps("media_last_uri", "")
                    s.ps("media_last_name", "")
                    selectedName = ""
                    status = "Selected media removed from the preview."
                }, modifier = Modifier.fillMaxWidth()) { Text("Remove selected media") }
            }
        }
        listOf(
            "Coach Demo · Jab Recovery · 02:14",
            "Padwork Flow · 5-count combination · 03:46",
            "Defense Drill · Slip & return · 04:10",
            "Mobility · Post-training recovery · 06:20"
        ).forEach { item ->
            var opened by remember(item) { mutableStateOf(false) }
            RsPanel(c) {
                Text(item, color = c.bright, fontWeight = FontWeight.Bold)
                Text("HD training media preview", color = c.muted)
                Button(onClick = { opened = !opened }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (opened) "■ Close preview" else "▶ Play preview")
                }
                if (opened) Text("Preview player opened locally. Production streaming will use authenticated cloud media.", color = c.text)
            }
        }
        RsStatusBannerV16(c, status)
    }
}

@Composable
fun RsStudentSettingsV16(c: RsPalette, s: RsStore) {
    var status by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }
    var exportRequested by remember { mutableStateOf(s.b("data_export_requested", false)) }

    RsScroll(c, "Settings, Privacy & Account", "Personal preferences, consent, data access and account controls.") {
        listOf(
            "Push notifications",
            "Booking reminders",
            "Training reminders",
            "Private profile",
            "Voice coach auto-speak",
            "Personalized training recommendations"
        ).forEachIndexed { i, title ->
            var on by remember { mutableStateOf(s.b("student_setting_v16_$i", i < 3 || i == 4 || i == 5)) }
            RsPanel(c) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = c.text, modifier = Modifier.weight(1f))
                    Switch(on, { on = it; s.pb("student_setting_v16_$i", it) })
                }
            }
        }

        RsPanel(c) {
            Text("YOUR DATA", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Review the data controls that will connect to the production account backend.", color = c.muted)
            Button(onClick = {
                exportRequested = true
                s.pb("data_export_requested", true)
                status = "Data export request recorded in this preview."
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (exportRequested) "✓ Data export requested" else "Request my data export")
            }
            OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Request account deletion")
            }
        }

        RsPanel(c) {
            Text("PRIVACY SUMMARY", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Profile visibility · training history · coach notes · uploaded media · notification preferences · AI coaching interactions", color = c.text)
            Text("Production release will connect these controls to server-side deletion/export and the public privacy/deletion-request pages.", color = c.muted)
        }

        RsStatusBannerV16(c, status)
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false; deleteText = "" },
            title = { Text("Account deletion request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type DELETE to confirm the preview request. This preview does not delete a real cloud account yet.")
                    OutlinedTextField(deleteText, { deleteText = it }, label = { Text("Type DELETE") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    s.pb("account_delete_requested", true)
                    status = "Account deletion request recorded for testing."
                    showDelete = false
                    deleteText = ""
                }, enabled = deleteText == "DELETE") { Text("Confirm request") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false; deleteText = "" }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RsNotificationsCenterV16(c: RsPalette, s: RsStore) {
    var status by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("All members") }

    RsScroll(c, "Notification Center", "Compose operational messages, choose an audience and preview delivery settings.") {
        RsPanel(c) {
            Text("NEW NOTIFICATION", color = c.bright, fontWeight = FontWeight.Bold)
            OutlinedTextField(draft, { draft = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All members", "Beginners", "Fight Team").forEach { group ->
                    FilterChip(selected = audience == group, onClick = { audience = group }, label = { Text(group) })
                }
            }
            Button(onClick = {
                status = "Preview notification queued for $audience."
                draft = ""
            }, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Queue notification") }
        }

        listOf("Class reminders", "Payment reminders", "Club news", "Homework reminders", "Event updates").forEachIndexed { i, name ->
            var enabled by remember { mutableStateOf(s.b("notify_category_$i", true)) }
            RsPanel(c) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = c.text, modifier = Modifier.weight(1f))
                    Switch(enabled, { enabled = it; s.pb("notify_category_$i", it) })
                }
            }
        }
        RsStatusBannerV16(c, status)
    }
}

@Composable
fun RsReleaseCenterV16(c: RsPalette, s: RsStore) {
    val context = LocalContext.current
    var privacyUrl by remember { mutableStateOf(s.s("legal_privacy_url", "")) }
    var termsUrl by remember { mutableStateOf(s.s("legal_terms_url", "")) }
    var deleteUrl by remember { mutableStateOf(s.s("legal_delete_url", "")) }
    var supportEmail by remember { mutableStateOf(s.s("support_email", "support@rskickbox.nl")) }
    var status by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        if(!url.startsWith("https://")){
            status="Enter a valid public https:// URL first."
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { status = "Could not open link on this device." }
    }

    RsScroll(c, "Release & Legal Center", "Trainer-side control for Play Store legal links, support contact and production-readiness checks.") {
        RsPanel(c) {
            val backend=rsBackendStatusV36()
            Text("CURRENT ACCEPTANCE BUILD", color = c.bright, fontWeight = FontWeight.Bold)
            Text("RS KICKBOX v0.53.0 · versionCode 53", color = c.text)
            Text("Package: com.rskickbox.app · targetSdk 36", color = c.muted)
            Text("Backend mode: "+if(backend.readyForClientInitialization)"SUPABASE CONFIGURED" else "LOCAL ACCEPTANCE", color = c.bright)
            Text("Supabase URL: "+if(backend.projectUrlConfigured)"configured" else "not configured", color = c.muted)
            Text("Publishable key: "+if(backend.publishableKeyConfigured)"configured" else "not configured", color = c.muted)
            Text("Local-ready: premium navigation drawer · 9-language enrollment UI · QR creation/scanning/gallery import · compact 100-student manager · per-student classes/bookings/attendance · memberships/payments/invoice ledger · trainer/student notification center with read state · persistent RS Events/RSVPs · trainer availability/private lesson requests · private coach messaging with unread state · clickable promotion carousel · controlled preview/full in-app PDF book reader with search/zoom/page-swipe effect · shared homework/coach notes/assessments/progress · persistent challenges/8-week Fight Camp/earned badges · shared searchable content library/favorites/history · trainer-controlled membership plans/access · persistent profile/community/groups · club documents/support/referrals · trainer-built session player · QR attendance/check-in · live analytics/integrated trainer progress · private technique-video history storage · local data export/account deactivation · visual/splash media tools.", color = c.text)
            Text("Not production-connected yet: multi-device authentication, Supabase database, cloud media, push delivery, real AI vision, payment webhooks, signed Play Store AAB.", color = c.muted)
        }
        RsPanel(c) {
            Text("PUBLIC LEGAL LINKS", color = c.bright, fontWeight = FontWeight.Bold)
            OutlinedTextField(privacyUrl, { privacyUrl = it }, label = { Text("Privacy policy URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(termsUrl, { termsUrl = it }, label = { Text("Terms URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(deleteUrl, { deleteUrl = it }, label = { Text("Account deletion request URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(supportEmail, { supportEmail = it }, label = { Text("Support email") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                s.ps("legal_privacy_url", privacyUrl)
                s.ps("legal_terms_url", termsUrl)
                s.ps("legal_delete_url", deleteUrl)
                s.ps("support_email", supportEmail)
                status = "Legal/release settings saved locally."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save legal settings") }
            OutlinedButton(onClick = { openUrl(privacyUrl) }, enabled=privacyUrl.startsWith("https://"), modifier = Modifier.fillMaxWidth()) { Text("Test privacy link") }
            OutlinedButton(onClick = { openUrl(deleteUrl) }, enabled=deleteUrl.startsWith("https://"), modifier = Modifier.fillMaxWidth()) { Text("Test deletion-request link") }
        }

        RsPanel(c) {
            Text("PLAY STORE RELEASE CHECK", color = c.bright, fontWeight = FontWeight.Bold)
            val checks = listOf(
                "v0.53 Android acceptance source" to true,
                "Private QR enrollment workflow" to true,
                "Nine-language enrollment UI" to true,
                "Premium role navigation drawer" to true,
                "Privacy policy URL" to privacyUrl.startsWith("https://"),
                "Account deletion URL" to deleteUrl.startsWith("https://"),
                "Support email" to supportEmail.contains("@"),
                "Secure production backend" to false,
                "Signed release AAB" to false,
                "Real payment webhooks" to false,
                "Production AI backend" to false,
                "Physical-device matrix QC" to false
            )
            checks.forEach { (name, ok) ->
                Text("${if (ok) "✓" else "○"} $name", color = if (ok) c.bright else c.muted)
            }
        }
        RsStatusBannerV16(c, status)
    }
}

@Composable
fun RsAdminSettingsV16(c: RsPalette, s: RsStore) {
    var status by remember { mutableStateOf("") }
    RsScroll(c, "App Settings & Operations", "Trainer controls for notifications, privacy, maintenance and operational policy.") {
        listOf("Push notifications", "Booking reminders", "Payment reminders", "Community moderation", "Maintenance banner", "Require re-login after logout").forEachIndexed { i, title ->
            var on by remember { mutableStateOf(s.b("admin_setting_v16_$i", i < 4 || i == 5)) }
            RsPanel(c) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = c.text, modifier = Modifier.weight(1f))
                    Switch(on, { on = it; s.pb("admin_setting_v16_$i", it) })
                }
            }
        }
        RsPanel(c) {
            Text("DATA RETENTION PREVIEW", color = c.bright, fontWeight = FontWeight.Bold)
            var retention by remember { mutableStateOf(s.s("retention", "24 months")) }
            listOf("12 months", "24 months", "36 months").forEach { item ->
                FilterChip(selected = retention == item, onClick = { retention = item; s.ps("retention", item); status = "Retention preference set to $item." }, label = { Text(item) })
            }
            Text("Production retention periods must match the final privacy policy and legal/business requirements.", color = c.muted)
        }
        RsStatusBannerV16(c, status)
    }
}

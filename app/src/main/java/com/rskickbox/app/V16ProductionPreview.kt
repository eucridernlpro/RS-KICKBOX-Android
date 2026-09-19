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
import androidx.compose.ui.unit.sp

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

private fun rsReleaseCenterT105(lang:RsLang,key:String):String{
    val en=mapOf(
        "invalid_url" to "Enter a valid public https:// URL first.","open_failed" to "Could not open this link on this device.",
        "current_build" to "CURRENT BUILD","backend_mode" to "Backend mode","configured" to "configured","not_configured" to "not configured",
        "schema_ready" to "SCHEMA READY","schema_pending" to "CLIENT CONNECTED · SCHEMA PENDING","checking" to "CHECKING…","not_checked" to "NOT CHECKED",
        "test_backend" to "Test Supabase project & schema","readiness" to "RELEASE STATUS",
        "readiness_body" to "Cloud account/data services are connected where configured. Final store work still includes legal URLs, signed release bundle, production billing/webhook verification and physical-device QC.",
        "legal_links" to "PUBLIC LEGAL LINKS","privacy_url" to "Privacy policy URL","terms_url" to "Terms URL","deletion_url" to "Account deletion request URL","support_email" to "Support email",
        "saved" to "Legal/release settings saved.","save" to "Save legal settings","test_privacy" to "Test privacy link","test_delete" to "Test deletion-request link",
        "play_store" to "PLAY STORE RELEASE CHECK","source" to "Current Android source","enrollment" to "Private QR enrollment","languages" to "Nine-language UI",
        "privacy" to "Privacy policy URL","deletion" to "Account deletion URL","backend" to "Supabase schema ready","signed" to "Signed release AAB","billing" to "Production billing/webhooks","device" to "Physical-device matrix QC"
    )
    val nl=en+mapOf("invalid_url" to "Vul eerst een geldige openbare https://-URL in.","open_failed" to "Deze link kon op dit apparaat niet worden geopend.","current_build" to "HUIDIGE BUILD","backend_mode" to "Backendmodus","configured" to "ingesteld","not_configured" to "niet ingesteld","schema_ready" to "SCHEMA KLAAR","schema_pending" to "CLIENT VERBONDEN · SCHEMA NOG NIET KLAAR","checking" to "CONTROLEREN…","not_checked" to "NIET GECONTROLEERD","test_backend" to "Supabase-project & schema testen","readiness" to "RELEASESTATUS","readiness_body" to "Cloudaccount- en datadiensten zijn verbonden waar ingesteld. Voor de store blijven juridische URL’s, een gesigneerde releasebundel, productiecontrole van betalingen/webhooks en fysieke apparaattests over.","legal_links" to "OPENBARE JURIDISCHE LINKS","privacy_url" to "URL privacybeleid","terms_url" to "URL voorwaarden","deletion_url" to "URL accountverwijdering","support_email" to "Support-e-mail","saved" to "Juridische/release-instellingen opgeslagen.","save" to "Juridische instellingen opslaan","test_privacy" to "Privacylink testen","test_delete" to "Verwijderingslink testen","play_store" to "PLAY STORE RELEASECONTROLE","source" to "Huidige Android-bron","enrollment" to "Privé QR-inschrijving","languages" to "UI in negen talen","privacy" to "URL privacybeleid","deletion" to "URL accountverwijdering","backend" to "Supabase-schema gereed","signed" to "Gesigneerde release-AAB","billing" to "Productiebetalingen/webhooks","device" to "Fysieke apparaat-QC")
    val pt=en+mapOf("invalid_url" to "Introduz primeiro um URL público https:// válido.","open_failed" to "Não foi possível abrir este link neste dispositivo.","current_build" to "BUILD ATUAL","backend_mode" to "Modo backend","configured" to "configurado","not_configured" to "não configurado","schema_ready" to "SCHEMA PRONTO","schema_pending" to "CLIENTE LIGADO · SCHEMA PENDENTE","checking" to "A VERIFICAR…","not_checked" to "NÃO VERIFICADO","test_backend" to "Testar projeto e schema Supabase","readiness" to "ESTADO DA RELEASE","readiness_body" to "Os serviços cloud de conta/dados estão ligados quando configurados. Falta concluir URLs legais, bundle assinado, verificação de pagamentos/webhooks em produção e QC em dispositivos físicos.","legal_links" to "LINKS LEGAIS PÚBLICOS","privacy_url" to "URL da política de privacidade","terms_url" to "URL dos termos","deletion_url" to "URL para eliminar conta","support_email" to "E-mail de suporte","saved" to "Definições legais/release guardadas.","save" to "Guardar definições legais","test_privacy" to "Testar link de privacidade","test_delete" to "Testar link de eliminação","play_store" to "VERIFICAÇÃO PLAY STORE","source" to "Fonte Android atual","enrollment" to "Inscrição privada por QR","languages" to "UI em nove idiomas","privacy" to "URL de privacidade","deletion" to "URL de eliminação da conta","backend" to "Schema Supabase pronto","signed" to "AAB de release assinado","billing" to "Pagamentos/webhooks de produção","device" to "QC em dispositivos físicos")
    val es=en+mapOf("invalid_url" to "Introduce primero una URL pública https:// válida.","open_failed" to "No se pudo abrir este enlace en este dispositivo.","current_build" to "BUILD ACTUAL","backend_mode" to "Modo backend","configured" to "configurado","not_configured" to "no configurado","schema_ready" to "ESQUEMA LISTO","schema_pending" to "CLIENTE CONECTADO · ESQUEMA PENDIENTE","checking" to "COMPROBANDO…","not_checked" to "NO COMPROBADO","test_backend" to "Probar proyecto y esquema Supabase","readiness" to "ESTADO DE RELEASE","readiness_body" to "Los servicios cloud de cuenta/datos están conectados cuando están configurados. Falta completar URLs legales, bundle firmado, verificación de pagos/webhooks de producción y QC en dispositivos físicos.","legal_links" to "ENLACES LEGALES PÚBLICOS","privacy_url" to "URL de política de privacidad","terms_url" to "URL de términos","deletion_url" to "URL de eliminación de cuenta","support_email" to "Correo de soporte","saved" to "Ajustes legales/release guardados.","save" to "Guardar ajustes legales","test_privacy" to "Probar enlace de privacidad","test_delete" to "Probar enlace de eliminación","play_store" to "CONTROL DE RELEASE PLAY STORE","source" to "Fuente Android actual","enrollment" to "Registro privado por QR","languages" to "UI en nueve idiomas","privacy" to "URL de privacidad","deletion" to "URL de eliminación de cuenta","backend" to "Esquema Supabase listo","signed" to "AAB de release firmado","billing" to "Pagos/webhooks de producción","device" to "QC en dispositivos físicos")
    val fr=en+mapOf("invalid_url" to "Saisis d’abord une URL publique https:// valide.","open_failed" to "Impossible d’ouvrir ce lien sur cet appareil.","current_build" to "BUILD ACTUEL","backend_mode" to "Mode backend","configured" to "configuré","not_configured" to "non configuré","schema_ready" to "SCHÉMA PRÊT","schema_pending" to "CLIENT CONNECTÉ · SCHÉMA EN ATTENTE","checking" to "VÉRIFICATION…","not_checked" to "NON VÉRIFIÉ","test_backend" to "Tester projet & schéma Supabase","readiness" to "ÉTAT DE PUBLICATION","readiness_body" to "Les services cloud de compte/données sont connectés lorsqu’ils sont configurés. Restent les URL légales, le bundle signé, la vérification paiements/webhooks en production et le QC sur appareils physiques.","legal_links" to "LIENS JURIDIQUES PUBLICS","privacy_url" to "URL politique de confidentialité","terms_url" to "URL des conditions","deletion_url" to "URL suppression de compte","support_email" to "E-mail support","saved" to "Réglages juridiques/release enregistrés.","save" to "Enregistrer les réglages juridiques","test_privacy" to "Tester le lien confidentialité","test_delete" to "Tester le lien suppression","play_store" to "CONTRÔLE RELEASE PLAY STORE","source" to "Source Android actuelle","enrollment" to "Inscription privée par QR","languages" to "UI en neuf langues","privacy" to "URL confidentialité","deletion" to "URL suppression du compte","backend" to "Schéma Supabase prêt","signed" to "AAB de release signé","billing" to "Paiements/webhooks production","device" to "QC appareils physiques")
    val de=en+mapOf("invalid_url" to "Gib zuerst eine gültige öffentliche https://-URL ein.","open_failed" to "Dieser Link konnte auf diesem Gerät nicht geöffnet werden.","current_build" to "AKTUELLER BUILD","backend_mode" to "Backend-Modus","configured" to "konfiguriert","not_configured" to "nicht konfiguriert","schema_ready" to "SCHEMA BEREIT","schema_pending" to "CLIENT VERBUNDEN · SCHEMA AUSSTEHEND","checking" to "PRÜFUNG…","not_checked" to "NICHT GEPRÜFT","test_backend" to "Supabase-Projekt & Schema testen","readiness" to "RELEASE-STATUS","readiness_body" to "Cloud-Konto- und Datendienste sind verbunden, sofern konfiguriert. Offen sind Rechts-URLs, signiertes Release-Bundle, Produktionsprüfung von Zahlungen/Webhooks und Geräte-QC.","legal_links" to "ÖFFENTLICHE RECHTSLINKS","privacy_url" to "URL der Datenschutzrichtlinie","terms_url" to "URL der Bedingungen","deletion_url" to "URL zur Kontolöschung","support_email" to "Support-E-Mail","saved" to "Rechts-/Release-Einstellungen gespeichert.","save" to "Rechtseinstellungen speichern","test_privacy" to "Datenschutzlink testen","test_delete" to "Löschlink testen","play_store" to "PLAY STORE RELEASE-PRÜFUNG","source" to "Aktueller Android-Quellstand","enrollment" to "Private QR-Anmeldung","languages" to "UI in neun Sprachen","privacy" to "Datenschutz-URL","deletion" to "Kontolöschungs-URL","backend" to "Supabase-Schema bereit","signed" to "Signiertes Release-AAB","billing" to "Produktionszahlungen/Webhooks","device" to "Geräte-QC")
    val it=en+mapOf("invalid_url" to "Inserisci prima un URL pubblico https:// valido.","open_failed" to "Impossibile aprire questo link su questo dispositivo.","current_build" to "BUILD ATTUALE","backend_mode" to "Modalità backend","configured" to "configurato","not_configured" to "non configurato","schema_ready" to "SCHEMA PRONTO","schema_pending" to "CLIENT CONNESSO · SCHEMA IN ATTESA","checking" to "CONTROLLO…","not_checked" to "NON CONTROLLATO","test_backend" to "Testa progetto e schema Supabase","readiness" to "STATO RELEASE","readiness_body" to "I servizi cloud di account/dati sono collegati quando configurati. Restano URL legali, bundle firmato, verifica pagamenti/webhook di produzione e QC su dispositivi fisici.","legal_links" to "LINK LEGALI PUBBLICI","privacy_url" to "URL privacy policy","terms_url" to "URL termini","deletion_url" to "URL eliminazione account","support_email" to "E-mail supporto","saved" to "Impostazioni legali/release salvate.","save" to "Salva impostazioni legali","test_privacy" to "Testa link privacy","test_delete" to "Testa link eliminazione","play_store" to "CONTROLLO RELEASE PLAY STORE","source" to "Sorgente Android attuale","enrollment" to "Registrazione privata QR","languages" to "UI in nove lingue","privacy" to "URL privacy","deletion" to "URL eliminazione account","backend" to "Schema Supabase pronto","signed" to "AAB release firmato","billing" to "Pagamenti/webhook produzione","device" to "QC dispositivi fisici")
    val pl=en+mapOf("invalid_url" to "Najpierw wpisz prawidłowy publiczny adres https://.","open_failed" to "Nie udało się otworzyć tego linku na tym urządzeniu.","current_build" to "AKTUALNY BUILD","backend_mode" to "Tryb backendu","configured" to "skonfigurowano","not_configured" to "nie skonfigurowano","schema_ready" to "SCHEMAT GOTOWY","schema_pending" to "KLIENT POŁĄCZONY · SCHEMAT OCZEKUJE","checking" to "SPRAWDZANIE…","not_checked" to "NIE SPRAWDZONO","test_backend" to "Testuj projekt i schemat Supabase","readiness" to "STATUS WYDANIA","readiness_body" to "Usługi chmurowe konta/danych są połączone, jeśli zostały skonfigurowane. Pozostają adresy prawne, podpisany bundle, weryfikacja płatności/webhooków produkcyjnych i QC na fizycznych urządzeniach.","legal_links" to "PUBLICZNE LINKI PRAWNE","privacy_url" to "URL polityki prywatności","terms_url" to "URL warunków","deletion_url" to "URL usunięcia konta","support_email" to "E-mail wsparcia","saved" to "Ustawienia prawne/release zapisane.","save" to "Zapisz ustawienia prawne","test_privacy" to "Testuj link prywatności","test_delete" to "Testuj link usunięcia","play_store" to "KONTROLA RELEASE PLAY STORE","source" to "Aktualne źródło Android","enrollment" to "Prywatny zapis QR","languages" to "UI w dziewięciu językach","privacy" to "URL prywatności","deletion" to "URL usunięcia konta","backend" to "Schemat Supabase gotowy","signed" to "Podpisany release AAB","billing" to "Płatności/webhooki produkcyjne","device" to "QC na urządzeniach")
    val tr=en+mapOf("invalid_url" to "Önce geçerli bir herkese açık https:// adresi gir.","open_failed" to "Bu bağlantı bu cihazda açılamadı.","current_build" to "GÜNCEL BUILD","backend_mode" to "Backend modu","configured" to "yapılandırıldı","not_configured" to "yapılandırılmadı","schema_ready" to "ŞEMA HAZIR","schema_pending" to "İSTEMCİ BAĞLI · ŞEMA BEKLEMEDE","checking" to "KONTROL EDİLİYOR…","not_checked" to "KONTROL EDİLMEDİ","test_backend" to "Supabase proje ve şemasını test et","readiness" to "YAYIN DURUMU","readiness_body" to "Bulut hesap/veri hizmetleri yapılandırıldığında bağlıdır. Yasal URL’ler, imzalı release paketi, üretim ödeme/webhook doğrulaması ve fiziksel cihaz QC’si tamamlanmalıdır.","legal_links" to "HERKESE AÇIK YASAL BAĞLANTILAR","privacy_url" to "Gizlilik politikası URL’si","terms_url" to "Koşullar URL’si","deletion_url" to "Hesap silme URL’si","support_email" to "Destek e-postası","saved" to "Yasal/release ayarları kaydedildi.","save" to "Yasal ayarları kaydet","test_privacy" to "Gizlilik bağlantısını test et","test_delete" to "Silme bağlantısını test et","play_store" to "PLAY STORE YAYIN KONTROLÜ","source" to "Güncel Android kaynağı","enrollment" to "Özel QR kaydı","languages" to "Dokuz dilli arayüz","privacy" to "Gizlilik URL’si","deletion" to "Hesap silme URL’si","backend" to "Supabase şeması hazır","signed" to "İmzalı release AAB","billing" to "Üretim ödemeleri/webhooklar","device" to "Fiziksel cihaz QC")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsReleaseCenterV16(c: RsPalette, s: RsStore, lang:RsLang) {
    val context = LocalContext.current
    var privacyUrl by remember { mutableStateOf(s.s("legal_privacy_url", "https://eucridernlpro.github.io/RS-KICKBOX-Android/privacy-policy.html")) }
    var termsUrl by remember { mutableStateOf(s.s("legal_terms_url", "")) }
    var deleteUrl by remember { mutableStateOf(s.s("legal_delete_url", "https://eucridernlpro.github.io/RS-KICKBOX-Android/account-deletion.html")) }
    var supportEmail by remember { mutableStateOf(s.s("support_email", "support@rskickbox.nl")) }
    var status by remember { mutableStateOf("") }
    var backendProbe by remember { mutableStateOf<RsSupabaseProbeV62?>(null) }
    var backendProbeRevision by remember { mutableIntStateOf(0) }
    var backendProbeBusy by remember { mutableStateOf(false) }

    LaunchedEffect(backendProbeRevision){
        backendProbeBusy=true
        backendProbe=rsSupabaseProbeV62()
        backendProbeBusy=false
    }

    fun openUrl(url:String){
        if(!url.startsWith("https://")){
            status=rsReleaseCenterT105(lang,"invalid_url")
            return
        }
        runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
            .onFailure{status=rsReleaseCenterT105(lang,"open_failed")}
    }

    RsScroll(c,rsRouteTitle(lang,"release","Release & Legal Center"),rsRouteHint(lang,"release","Policies · backend · store readiness")){
        RsPanel(c){
            val backend=rsBackendStatusV36()
            val probe=backendProbe
            Text(rsReleaseCenterT105(lang,"current_build"),color=c.bright,fontWeight=FontWeight.Bold)
            Text("RS KICKBOX v"+BuildConfig.VERSION_NAME+" · versionCode "+BuildConfig.VERSION_CODE,color=c.text)
            Text("com.rskickbox.app · targetSdk 36",color=c.muted)
            Text(rsReleaseCenterT105(lang,"backend_mode")+": "+if(backend.readyForClientInitialization)"SUPABASE" else "LOCAL",color=c.bright)
            Text("Supabase URL: "+if(backend.projectUrlConfigured)rsReleaseCenterT105(lang,"configured") else rsReleaseCenterT105(lang,"not_configured"),color=c.muted)
            Text("Publishable key: "+if(backend.publishableKeyConfigured)rsReleaseCenterT105(lang,"configured") else rsReleaseCenterT105(lang,"not_configured"),color=c.muted)
            Text("Supabase: "+when(probe?.state){
                RsSupabaseSchemaStateV62.SCHEMA_READY->rsReleaseCenterT105(lang,"schema_ready")
                RsSupabaseSchemaStateV62.CLIENT_CONFIGURED_SCHEMA_PENDING->rsReleaseCenterT105(lang,"schema_pending")
                RsSupabaseSchemaStateV62.NOT_CONFIGURED->rsReleaseCenterT105(lang,"not_configured").uppercase()
                null->if(backendProbeBusy)rsReleaseCenterT105(lang,"checking") else rsReleaseCenterT105(lang,"not_checked")
            },color=if(probe?.state==RsSupabaseSchemaStateV62.SCHEMA_READY)c.bright else c.muted)
            OutlinedButton(onClick={backendProbeRevision++},enabled=!backendProbeBusy,modifier=Modifier.fillMaxWidth()){
                Text(if(backendProbeBusy)rsReleaseCenterT105(lang,"checking") else rsReleaseCenterT105(lang,"test_backend"))
            }
        }

        RsPanel(c){
            Text(rsReleaseCenterT105(lang,"readiness"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsReleaseCenterT105(lang,"readiness_body"),color=c.text)
        }

        RsPanel(c){
            Text(rsReleaseCenterT105(lang,"legal_links"),color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedTextField(privacyUrl,{privacyUrl=it},label={Text(rsReleaseCenterT105(lang,"privacy_url"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(termsUrl,{termsUrl=it},label={Text(rsReleaseCenterT105(lang,"terms_url"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(deleteUrl,{deleteUrl=it},label={Text(rsReleaseCenterT105(lang,"deletion_url"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(supportEmail,{supportEmail=it},label={Text(rsReleaseCenterT105(lang,"support_email"))},modifier=Modifier.fillMaxWidth())
            Button(onClick={
                s.ps("legal_privacy_url",privacyUrl);s.ps("legal_terms_url",termsUrl);s.ps("legal_delete_url",deleteUrl);s.ps("support_email",supportEmail)
                status=rsReleaseCenterT105(lang,"saved")
            },modifier=Modifier.fillMaxWidth()){Text(rsReleaseCenterT105(lang,"save"))}
            OutlinedButton(onClick={openUrl(privacyUrl)},enabled=privacyUrl.startsWith("https://"),modifier=Modifier.fillMaxWidth()){Text(rsReleaseCenterT105(lang,"test_privacy"))}
            OutlinedButton(onClick={openUrl(deleteUrl)},enabled=deleteUrl.startsWith("https://"),modifier=Modifier.fillMaxWidth()){Text(rsReleaseCenterT105(lang,"test_delete"))}
        }

        RsPanel(c){
            Text(rsReleaseCenterT105(lang,"play_store"),color=c.bright,fontWeight=FontWeight.Bold)
            val schemaReady=backendProbe?.state==RsSupabaseSchemaStateV62.SCHEMA_READY
            val checks=listOf(
                rsReleaseCenterT105(lang,"source") to true,
                rsReleaseCenterT105(lang,"enrollment") to true,
                rsReleaseCenterT105(lang,"languages") to true,
                rsReleaseCenterT105(lang,"privacy") to privacyUrl.startsWith("https://"),
                rsReleaseCenterT105(lang,"deletion") to deleteUrl.startsWith("https://"),
                rsReleaseCenterT105(lang,"support_email") to supportEmail.contains("@"),
                rsReleaseCenterT105(lang,"backend") to schemaReady,
                rsReleaseCenterT105(lang,"signed") to false,
                rsReleaseCenterT105(lang,"billing") to false,
                rsReleaseCenterT105(lang,"device") to false
            )
            checks.forEach{(name,ok)->Text((if(ok)"✓ " else "○ ")+name,color=if(ok)c.bright else c.muted)}
        }
        RsStatusBannerV16(c,status)
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

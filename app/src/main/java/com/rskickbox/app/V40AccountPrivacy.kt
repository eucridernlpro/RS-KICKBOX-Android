package com.rskickbox.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

private fun rsAccountUiV40(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "Settings & Privacy","sub" to "Preferences, privacy, local data export and account controls.",
        "prefs" to "PREFERENCES","push" to "Push notifications","booking" to "Booking reminders","training" to "Training reminders",
        "private_profile" to "Private profile","voice" to "Voice coach auto-speak","data" to "YOUR DATA",
        "export_desc" to "Create a JSON export of the local acceptance data linked to this student on this device.",
        "export" to "Export my local data","delete" to "Request account deletion","delete_title" to "Account deletion request",
        "delete_desc" to "Type DELETE to remove this offline test profile from this device. When cloud access is available, account deletion is requested securely from the RS KICKBOXING backend.",
        "type_delete" to "Type DELETE","confirm" to "Confirm request","cancel" to "Cancel","export_ready" to "Local data export ready to share.",
        "delete_done" to "Offline test profile removed from this device."
    )
    val nl=en+mapOf("title" to "Instellingen & Privacy","sub" to "Voorkeuren, privacy, lokale data-export en accountbeheer.","prefs" to "VOORKEUREN","push" to "Pushmeldingen","booking" to "Boekingsherinneringen","training" to "Trainingsherinneringen","private_profile" to "Privéprofiel","voice" to "Voice coach automatisch spreken","data" to "JOUW DATA","export_desc" to "Maak een JSON-export van lokale acceptatiedata voor deze leerling op dit apparaat.","export" to "Mijn lokale data exporteren","delete" to "Accountverwijdering aanvragen","delete_title" to "Verzoek accountverwijdering","delete_desc" to "Typ DELETE om dit offline testprofiel van dit apparaat te verwijderen. Met cloudtoegang wordt accountverwijdering veilig via de RS KICKBOXING-backend aangevraagd.","type_delete" to "Typ DELETE","confirm" to "Verzoek bevestigen","cancel" to "Annuleren","export_ready" to "Lokale data-export klaar om te delen.","delete_done" to "Offline testprofiel van dit apparaat verwijderd.")
    val pt=en+mapOf("title" to "Definições & Privacidade","sub" to "Preferências, privacidade, exportação local e controlo da conta.","prefs" to "PREFERÊNCIAS","push" to "Notificações push","booking" to "Lembretes de reservas","training" to "Lembretes de treino","private_profile" to "Perfil privado","voice" to "Coach de voz automático","data" to "OS TEUS DADOS","export_desc" to "Cria uma exportação JSON dos dados locais deste aluno neste dispositivo.","export" to "Exportar os meus dados locais","delete" to "Pedir eliminação da conta","delete_title" to "Pedido de eliminação","delete_desc" to "Escreve DELETE para remover este perfil offline de teste deste dispositivo. Com acesso cloud, a eliminação da conta é pedida com segurança ao backend RS KICKBOXING.","type_delete" to "Escreve DELETE","confirm" to "Confirmar pedido","cancel" to "Cancelar","export_ready" to "Exportação local pronta para partilhar.","delete_done" to "Perfil offline de teste removido deste dispositivo.")
    val es=en+mapOf("title" to "Ajustes & Privacidad","sub" to "Preferencias, privacidad, exportación local y controles de cuenta.","prefs" to "PREFERENCIAS","push" to "Notificaciones push","booking" to "Recordatorios de reservas","training" to "Recordatorios de entrenamiento","private_profile" to "Perfil privado","voice" to "Voz automática del coach","data" to "TUS DATOS","export_desc" to "Crea una exportación JSON de los datos locales de este alumno en este dispositivo.","export" to "Exportar mis datos locales","delete" to "Solicitar eliminación de cuenta","delete_title" to "Solicitud de eliminación","delete_desc" to "Escribe DELETE para eliminar este perfil offline de prueba de este dispositivo. Con acceso cloud, la eliminación de cuenta se solicita de forma segura al backend de RS KICKBOXING.","type_delete" to "Escribe DELETE","confirm" to "Confirmar solicitud","cancel" to "Cancelar","export_ready" to "Exportación local lista para compartir.","delete_done" to "Perfil offline de prueba eliminado de este dispositivo.")
    val fr=en+mapOf("title" to "Réglages & Confidentialité","sub" to "Préférences, confidentialité, export local et contrôle du compte.","prefs" to "PRÉFÉRENCES","push" to "Notifications push","booking" to "Rappels de réservation","training" to "Rappels d'entraînement","private_profile" to "Profil privé","voice" to "Réponses vocales automatiques","data" to "TES DONNÉES","export_desc" to "Crée un export JSON des données locales de cet élève sur cet appareil.","export" to "Exporter mes données locales","delete" to "Demander la suppression du compte","delete_title" to "Demande de suppression","delete_desc" to "Tape DELETE pour supprimer ce profil de test hors ligne de cet appareil. Avec l’accès cloud, la suppression du compte est demandée de façon sécurisée au backend RS KICKBOXING.","type_delete" to "Tape DELETE","confirm" to "Confirmer la demande","cancel" to "Annuler","export_ready" to "Export local prêt à partager.","delete_done" to "Profil de test hors ligne supprimé de cet appareil.")
    val de=en+mapOf("title" to "Einstellungen & Datenschutz","sub" to "Einstellungen, Datenschutz, lokaler Export und Kontosteuerung.","prefs" to "EINSTELLUNGEN","push" to "Push-Benachrichtigungen","booking" to "Buchungserinnerungen","training" to "Trainingserinnerungen","private_profile" to "Privates Profil","voice" to "Voice Coach automatisch","data" to "DEINE DATEN","export_desc" to "Erstellt einen JSON-Export der lokalen Daten dieses Schülers auf diesem Gerät.","export" to "Lokale Daten exportieren","delete" to "Kontolöschung anfordern","delete_title" to "Antrag auf Kontolöschung","delete_desc" to "Gib DELETE ein, um dieses Offline-Testprofil von diesem Gerät zu entfernen. Bei Cloud-Zugriff wird die Kontolöschung sicher über das RS KICKBOXING-Backend angefordert.","type_delete" to "DELETE eingeben","confirm" to "Antrag bestätigen","cancel" to "Abbrechen","export_ready" to "Lokaler Export kann geteilt werden.","delete_done" to "Offline-Testprofil von diesem Gerät entfernt.")
    val it=en+mapOf("title" to "Impostazioni & Privacy","sub" to "Preferenze, privacy, esportazione locale e controllo account.","prefs" to "PREFERENZE","push" to "Notifiche push","booking" to "Promemoria prenotazioni","training" to "Promemoria allenamenti","private_profile" to "Profilo privato","voice" to "Coach vocale automatico","data" to "I TUOI DATI","export_desc" to "Crea un export JSON dei dati locali di questo allievo sul dispositivo.","export" to "Esporta i miei dati locali","delete" to "Richiedi eliminazione account","delete_title" to "Richiesta eliminazione account","delete_desc" to "Digita DELETE per rimuovere questo profilo offline di test dal dispositivo. Con accesso cloud, l’eliminazione account viene richiesta in modo sicuro al backend RS KICKBOXING.","type_delete" to "Digita DELETE","confirm" to "Conferma richiesta","cancel" to "Annulla","export_ready" to "Export locale pronto da condividere.","delete_done" to "Profilo offline di test rimosso dal dispositivo.")
    val pl=en+mapOf("title" to "Ustawienia & Prywatność","sub" to "Preferencje, prywatność, eksport lokalny i kontrola konta.","prefs" to "PREFERENCJE","push" to "Powiadomienia push","booking" to "Przypomnienia o rezerwacji","training" to "Przypomnienia o treningu","private_profile" to "Profil prywatny","voice" to "Automatyczny głos trenera","data" to "TWOJE DANE","export_desc" to "Tworzy eksport JSON lokalnych danych tego ucznia na tym urządzeniu.","export" to "Eksportuj moje dane lokalne","delete" to "Poproś o usunięcie konta","delete_title" to "Żądanie usunięcia konta","delete_desc" to "Wpisz DELETE, aby usunąć ten lokalny profil testowy z urządzenia. Przy dostępie do chmury usunięcie konta jest bezpiecznie zlecane backendowi RS KICKBOXING.","type_delete" to "Wpisz DELETE","confirm" to "Potwierdź żądanie","cancel" to "Anuluj","export_ready" to "Eksport lokalny gotowy do udostępnienia.","delete_done" to "Lokalny profil testowy usunięty z urządzenia.")
    val tr=en+mapOf("title" to "Ayarlar & Gizlilik","sub" to "Tercihler, gizlilik, yerel dışa aktarma ve hesap kontrolleri.","prefs" to "TERCİHLER","push" to "Anlık bildirimler","booking" to "Rezervasyon hatırlatmaları","training" to "Antrenman hatırlatmaları","private_profile" to "Özel profil","voice" to "Otomatik sesli koç","data" to "VERİLERİN","export_desc" to "Bu cihazdaki öğrenciye ait yerel verilerin JSON dışa aktarımını oluşturur.","export" to "Yerel verilerimi dışa aktar","delete" to "Hesap silme isteği","delete_title" to "Hesap silme isteği","delete_desc" to "Bu çevrimdışı test profilini cihazdan kaldırmak için DELETE yaz. Bulut erişimi olduğunda hesap silme isteği güvenli biçimde RS KICKBOXING backendine gönderilir.","type_delete" to "DELETE yaz","confirm" to "İsteği onayla","cancel" to "İptal","export_ready" to "Yerel veri dışa aktarımı paylaşmaya hazır.","delete_done" to "Çevrimdışı test profili bu cihazdan kaldırıldı.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

private fun rsCloudPrivacyT105(lang:RsLang,key:String):String{
    val en=mapOf(
        "cloud_desc" to "You can share an immediate export of local app data or submit a secure request for account data held in the RS KICKBOXING backend.",
        "share_local" to "Share local app data export","request_export" to "Request cloud data export","export_submitted" to "Cloud data-export request submitted.",
        "requests" to "ACCOUNT REQUESTS","syncing_requests" to "Syncing request status…","requests_secure" to "Requests are tracked securely in Supabase.","none" to "No privacy/account requests yet.",
        "delete_desc" to "Type DELETE to submit a real account-deletion request. RS KICKBOXING will process the request securely and may retain only records that must legally be kept.",
        "delete_submitted" to "Account-deletion request submitted.","submitting" to "Submitting…",
        "links" to "PRIVACY & ACCOUNT LINKS","privacy" to "Privacy policy","deletion" to "Account deletion web page","link_note" to "These public links are used for privacy information and external account-deletion requests."
    )
    val nl=en+mapOf("cloud_desc" to "Je kunt direct lokale app-data delen of een beveiligd verzoek indienen voor accountgegevens in de RS KICKBOXING-backend.","share_local" to "Lokale app-data delen","request_export" to "Clouddata-export aanvragen","export_submitted" to "Clouddata-exportverzoek ingediend.","requests" to "ACCOUNTVERZOEKEN","syncing_requests" to "Verzoekstatus synchroniseren…","requests_secure" to "Verzoeken worden veilig in Supabase bijgehouden.","none" to "Nog geen privacy-/accountverzoeken.","delete_desc" to "Typ DELETE om een echt verzoek tot accountverwijdering in te dienen. RS KICKBOXING verwerkt dit veilig en bewaart alleen gegevens die wettelijk moeten worden behouden.","delete_submitted" to "Verzoek tot accountverwijdering ingediend.","submitting" to "Indienen…","links" to "PRIVACY- & ACCOUNTLINKS","privacy" to "Privacybeleid","deletion" to "Webpagina accountverwijdering","link_note" to "Deze openbare links geven privacy-informatie en bieden een externe route voor accountverwijdering.")
    val pt=en+mapOf("cloud_desc" to "Podes partilhar imediatamente os dados locais da app ou enviar um pedido seguro para os dados da conta guardados no backend RS KICKBOXING.","share_local" to "Partilhar dados locais da app","request_export" to "Pedir exportação de dados cloud","export_submitted" to "Pedido de exportação cloud enviado.","requests" to "PEDIDOS DA CONTA","syncing_requests" to "A sincronizar estado dos pedidos…","requests_secure" to "Os pedidos são registados com segurança no Supabase.","none" to "Ainda não existem pedidos de privacidade/conta.","delete_desc" to "Escreve DELETE para enviar um pedido real de eliminação da conta. O RS KICKBOXING processará o pedido com segurança e poderá manter apenas registos legalmente obrigatórios.","delete_submitted" to "Pedido de eliminação da conta enviado.","submitting" to "A enviar…","links" to "LINKS DE PRIVACIDADE & CONTA","privacy" to "Política de privacidade","deletion" to "Página web de eliminação da conta","link_note" to "Estes links públicos apresentam informação de privacidade e permitem pedidos externos de eliminação.")
    val es=en+mapOf("cloud_desc" to "Puedes compartir de inmediato los datos locales de la app o enviar una solicitud segura para los datos de cuenta guardados en el backend de RS KICKBOXING.","share_local" to "Compartir datos locales de la app","request_export" to "Solicitar exportación de datos cloud","export_submitted" to "Solicitud de exportación cloud enviada.","requests" to "SOLICITUDES DE CUENTA","syncing_requests" to "Sincronizando estado de solicitudes…","requests_secure" to "Las solicitudes se registran de forma segura en Supabase.","none" to "Aún no hay solicitudes de privacidad/cuenta.","delete_desc" to "Escribe DELETE para enviar una solicitud real de eliminación de cuenta. RS KICKBOXING la procesará de forma segura y solo podrá conservar registros exigidos legalmente.","delete_submitted" to "Solicitud de eliminación de cuenta enviada.","submitting" to "Enviando…","links" to "ENLACES DE PRIVACIDAD Y CUENTA","privacy" to "Política de privacidad","deletion" to "Página web de eliminación de cuenta","link_note" to "Estos enlaces públicos muestran información de privacidad y permiten solicitudes externas de eliminación.")
    val fr=en+mapOf("cloud_desc" to "Tu peux partager immédiatement les données locales de l’app ou envoyer une demande sécurisée pour les données de compte stockées dans le backend RS KICKBOXING.","share_local" to "Partager les données locales","request_export" to "Demander l’export cloud","export_submitted" to "Demande d’export cloud envoyée.","requests" to "DEMANDES DE COMPTE","syncing_requests" to "Synchronisation des demandes…","requests_secure" to "Les demandes sont suivies de façon sécurisée dans Supabase.","none" to "Aucune demande de confidentialité/compte.","delete_desc" to "Tape DELETE pour envoyer une vraie demande de suppression de compte. RS KICKBOXING la traitera de façon sécurisée et pourra conserver uniquement les données légalement requises.","delete_submitted" to "Demande de suppression de compte envoyée.","submitting" to "Envoi…","links" to "LIENS CONFIDENTIALITÉ & COMPTE","privacy" to "Politique de confidentialité","deletion" to "Page web de suppression du compte","link_note" to "Ces liens publics fournissent les informations de confidentialité et une voie externe pour demander la suppression.")
    val de=en+mapOf("cloud_desc" to "Du kannst lokale App-Daten sofort teilen oder eine sichere Anfrage für Kontodaten im RS KICKBOXING-Backend stellen.","share_local" to "Lokale App-Daten teilen","request_export" to "Cloud-Datenexport anfordern","export_submitted" to "Cloud-Datenexport angefordert.","requests" to "KONTOANFRAGEN","syncing_requests" to "Anfragestatus wird synchronisiert…","requests_secure" to "Anfragen werden sicher in Supabase verfolgt.","none" to "Noch keine Datenschutz-/Kontoanfragen.","delete_desc" to "Gib DELETE ein, um eine echte Kontolöschungsanfrage zu senden. RS KICKBOXING verarbeitet sie sicher und darf nur gesetzlich erforderliche Daten aufbewahren.","delete_submitted" to "Kontolöschungsanfrage gesendet.","submitting" to "Wird gesendet…","links" to "DATENSCHUTZ- & KONTOLINKS","privacy" to "Datenschutzrichtlinie","deletion" to "Webseite zur Kontolöschung","link_note" to "Diese öffentlichen Links enthalten Datenschutzinformationen und ermöglichen externe Löschanfragen.")
    val it=en+mapOf("cloud_desc" to "Puoi condividere subito i dati locali dell’app o inviare una richiesta sicura per i dati account nel backend RS KICKBOXING.","share_local" to "Condividi dati locali app","request_export" to "Richiedi esportazione cloud","export_submitted" to "Richiesta di esportazione cloud inviata.","requests" to "RICHIESTE ACCOUNT","syncing_requests" to "Sincronizzazione richieste…","requests_secure" to "Le richieste sono tracciate in modo sicuro in Supabase.","none" to "Nessuna richiesta privacy/account.","delete_desc" to "Digita DELETE per inviare una vera richiesta di eliminazione account. RS KICKBOXING la elaborerà in modo sicuro e potrà conservare solo dati obbligatori per legge.","delete_submitted" to "Richiesta eliminazione account inviata.","submitting" to "Invio…","links" to "LINK PRIVACY & ACCOUNT","privacy" to "Informativa privacy","deletion" to "Pagina web eliminazione account","link_note" to "Questi link pubblici mostrano informazioni sulla privacy e consentono richieste esterne di eliminazione.")
    val pl=en+mapOf("cloud_desc" to "Możesz natychmiast udostępnić lokalne dane aplikacji lub wysłać bezpieczny wniosek o dane konta przechowywane w backendzie RS KICKBOXING.","share_local" to "Udostępnij lokalne dane aplikacji","request_export" to "Poproś o eksport danych z chmury","export_submitted" to "Wysłano wniosek o eksport danych z chmury.","requests" to "WNIOSKI DOTYCZĄCE KONTA","syncing_requests" to "Synchronizacja statusu wniosków…","requests_secure" to "Wnioski są bezpiecznie śledzone w Supabase.","none" to "Brak wniosków prywatności/konta.","delete_desc" to "Wpisz DELETE, aby wysłać rzeczywisty wniosek o usunięcie konta. RS KICKBOXING przetworzy go bezpiecznie i może zachować wyłącznie dane wymagane prawem.","delete_submitted" to "Wysłano wniosek o usunięcie konta.","submitting" to "Wysyłanie…","links" to "LINKI PRYWATNOŚCI I KONTA","privacy" to "Polityka prywatności","deletion" to "Strona usunięcia konta","link_note" to "Te publiczne linki zawierają informacje o prywatności i zewnętrzną ścieżkę usunięcia konta.")
    val tr=en+mapOf("cloud_desc" to "Yerel uygulama verilerini hemen paylaşabilir veya RS KICKBOXING backendindeki hesap verileri için güvenli bir istek gönderebilirsin.","share_local" to "Yerel uygulama verilerini paylaş","request_export" to "Bulut veri dışa aktarımı iste","export_submitted" to "Bulut veri dışa aktarım isteği gönderildi.","requests" to "HESAP İSTEKLERİ","syncing_requests" to "İstek durumu eşitleniyor…","requests_secure" to "İstekler Supabase’de güvenli şekilde takip edilir.","none" to "Henüz gizlilik/hesap isteği yok.","delete_desc" to "Gerçek hesap silme isteği göndermek için DELETE yaz. RS KICKBOXING isteği güvenli şekilde işler ve yalnızca yasal olarak tutulması gereken kayıtları saklayabilir.","delete_submitted" to "Hesap silme isteği gönderildi.","submitting" to "Gönderiliyor…","links" to "GİZLİLİK & HESAP BAĞLANTILARI","privacy" to "Gizlilik politikası","deletion" to "Hesap silme web sayfası","link_note" to "Bu herkese açık bağlantılar gizlilik bilgilerini ve harici hesap silme yolunu sağlar.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsPrivacyLinksV105(c:RsPalette,store:RsStore,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    val privacy=store.s("legal_privacy_url","https://eucridernlpro.github.io/RS-KICKBOX-Android/privacy-policy.html")
    val deletion=store.s("legal_delete_url","https://eucridernlpro.github.io/RS-KICKBOX-Android/account-deletion.html")
    fun open(url:String){runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}}
    RsPanel(c){
        Text(rsCloudPrivacyT105(lang,"links"),color=c.bright,fontWeight=FontWeight.Bold)
        Text(rsCloudPrivacyT105(lang,"link_note"),color=c.muted,fontSize=10.sp)
        OutlinedButton(onClick={open(privacy)},modifier=Modifier.fillMaxWidth()){Text(rsCloudPrivacyT105(lang,"privacy"))}
        OutlinedButton(onClick={open(deletion)},modifier=Modifier.fillMaxWidth()){Text(rsCloudPrivacyT105(lang,"deletion"))}
    }
}

private fun rsBuildLocalExportV40(store:RsStore):String{
    val email=store.s("session_student_email","")
    val name=store.s("session_student_name","")
    val bookingKey="student_bookings_v38_"+email.lowercase()
    return JSONObject().apply{
        put("export_version",1)
        put("student",JSONObject().apply{
            put("name",name)
            put("email",email)
        })
        put("bookings",JSONArray(store.s(bookingKey,"").split(',').filter{it.isNotBlank()}))
        put("finance_invoices",JSONArray().apply{
            rsFinanceInvoicesForStudentV40(store,email,name).forEach{inv->
                put(JSONObject().apply{
                    put("id",inv.id)
                    put("period",inv.period)
                    put("amount_cents",inv.amountCents)
                    put("status",inv.status)
                })
            }
        })
        put("technique_history",rsTechniqueHistoryRawForStudentV40(store,email))
        put("coach_messages",rsCoachMessagesRawForStudentV44(store,email))
        put("homework",rsHomeworkRawForStudentV46(store,email))
        put("assessments",rsAssessmentsRawForStudentV46(store,email))
        put("trainer_private_notes",rsCoachNotesRawForPrivacyV46(store,email))
        put("social",rsSocialPrivacyRawV50(store,email))
        put("preferences",JSONObject().apply{
            put("language",store.s("lang","en"))
            put("voice_auto",store.b("voice_auto",true))
            put("private_profile",store.b("student_setting_v16_3",false))
        })
    }.toString(2)
}

private fun rsShareLocalExportV40(context:android.content.Context,store:RsStore){
    val dir=File(context.cacheDir,"shared").apply{mkdirs()}
    val file=File(dir,"RS_KICKBOX_local_data_export.json")
    FileOutputStream(file).use{it.write(rsBuildLocalExportV40(store).toByteArray(Charsets.UTF_8))}
    val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
    val intent=Intent(Intent.ACTION_SEND).apply{
        type="application/json"
        putExtra(Intent.EXTRA_STREAM,uri)
        clipData=ClipData.newRawUri("RS KICKBOXING data export",uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent,"Share RS KICKBOXING data export"))
}

@Composable
fun RsStudentPrivacyV40(c:RsPalette,store:RsStore,lang:RsLang,onLocalAccountDisabled:()->Unit){
    if(RsSupabaseV60.configured){
        RsCloudStudentPrivacyV78(c,store,lang)
        return
    }
    val context=androidx.compose.ui.platform.LocalContext.current
    var status by remember{mutableStateOf("")}
    var showDelete by remember{mutableStateOf(false)}
    var deleteText by remember{mutableStateOf("")}

    RsScroll(c,rsAccountUiV40(lang,"title"),rsAccountUiV40(lang,"sub")){
        RsPanel(c){
            Text(rsAccountUiV40(lang,"prefs"),color=c.bright,fontWeight=FontWeight.Bold)
            listOf(
                rsAccountUiV40(lang,"push"),
                rsAccountUiV40(lang,"booking"),
                rsAccountUiV40(lang,"training"),
                rsAccountUiV40(lang,"private_profile"),
                rsAccountUiV40(lang,"voice")
            ).forEachIndexed{i,label->
                var enabled by remember{mutableStateOf(store.b("student_setting_v16_"+i,i<3||i==4))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(label,color=c.text,modifier=Modifier.weight(1f))
                    Switch(enabled,{v->enabled=v;store.pb("student_setting_v16_"+i,v)})
                }
            }
        }
        RsPanel(c){
            Text(rsAccountUiV40(lang,"data"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsAccountUiV40(lang,"export_desc"),color=c.muted)
            Button(
                onClick={
                    rsShareLocalExportV40(context,store)
                    status=rsAccountUiV40(lang,"export_ready")
                },
                enabled=store.s("session_student_email","").isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsAccountUiV40(lang,"export"))}
            OutlinedButton(onClick={showDelete=true},modifier=Modifier.fillMaxWidth()){
                Text(rsAccountUiV40(lang,"delete"))
            }
            if(status.isNotBlank())Text(status,color=c.muted)
        }
        RsPrivacyLinksV105(c,store,lang)
    }

    if(showDelete){
        AlertDialog(
            onDismissRequest={showDelete=false;deleteText=""},
            title={Text(rsAccountUiV40(lang,"delete_title"))},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(rsAccountUiV40(lang,"delete_desc"))
                    OutlinedTextField(deleteText,{deleteText=it},label={Text(rsAccountUiV40(lang,"type_delete"))})
                }
            },
            confirmButton={
                Button(
                    onClick={
                        val email=store.s("session_student_email","")
                        if(!email.equals("alex@rskickbox.nl",true)){
                            val remaining=rsLoadStudentsV33(store).filterNot{it.email.equals(email,true)}
                            rsSaveStudentsV33(store,remaining)
                        }
                        store.pb("account_delete_requested",true)
                        store.ps("student_bookings_v38_"+email.lowercase(),"")
                        rsRemoveTechniqueHistoryForStudentV40(context,store,email)
                        rsRemoveCoachMessagesForStudentV44(store,email)
                        rsRemoveDevelopmentDataForStudentV46(store,email)
                        rsRemoveSocialDataForStudentV50(store,email)
                        store.ps("session_student_email","")
                        store.ps("session_student_name","")
                        status=rsAccountUiV40(lang,"delete_done")
                        showDelete=false
                        deleteText=""
                        onLocalAccountDisabled()
                    },
                    enabled=deleteText=="DELETE"
                ){Text(rsAccountUiV40(lang,"confirm"))}
            },
            dismissButton={
                TextButton(onClick={showDelete=false;deleteText=""}){Text(rsAccountUiV40(lang,"cancel"))}
            }
        )
    }
}


@Composable
private fun RsCloudStudentPrivacyV78(c:RsPalette,store:RsStore,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var requests by remember{mutableStateOf<List<RsAccountRequestV78>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var showDelete by remember{mutableStateOf(false)}
    var deleteText by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsMyAccountRequestsV78()
            .onSuccess{requests=it}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsAccountUiV40(lang,"title"),rsAccountUiV40(lang,"sub")){
        RsPanel(c){
            Text(rsAccountUiV40(lang,"prefs"),color=c.bright,fontWeight=FontWeight.Bold)
            listOf(
                rsAccountUiV40(lang,"push"),
                rsAccountUiV40(lang,"booking"),
                rsAccountUiV40(lang,"training"),
                rsAccountUiV40(lang,"private_profile"),
                rsAccountUiV40(lang,"voice")
            ).forEachIndexed{i,label->
                var enabled by remember{mutableStateOf(store.b("student_setting_v16_"+i,i<3||i==4))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(label,color=c.text,modifier=Modifier.weight(1f))
                    Switch(enabled,{v->enabled=v;store.pb("student_setting_v16_"+i,v)})
                }
            }
        }

        RsPanel(c){
            Text(rsAccountUiV40(lang,"data"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsCloudPrivacyT105(lang,"cloud_desc"),color=c.muted)
            Button(
                onClick={
                    rsShareLocalExportV40(context,store)
                    status=rsAccountUiV40(lang,"export_ready")
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsCloudPrivacyT105(lang,"share_local"))}

            OutlinedButton(
                onClick={
                    busy=true
                    status=""
                    scope.launch{
                        rsCreateAccountRequestV78("data_export")
                            .onSuccess{
                                status=rsCloudPrivacyT105(lang,"export_submitted")
                                revision++
                            }
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busy=false
                    }
                },
                enabled=!busy,
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)rsReleaseT98(lang,"please_wait") else rsCloudPrivacyT105(lang,"request_export"))}

            OutlinedButton(
                onClick={showDelete=true},
                enabled=!busy,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsAccountUiV40(lang,"delete"))}

            if(status.isNotBlank())Text(status,color=c.muted)
        }

        RsPanel(c){
            Text(rsCloudPrivacyT105(lang,"requests"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(
                if(loading)rsCloudPrivacyT105(lang,"syncing_requests") else rsCloudPrivacyT105(lang,"requests_secure"),
                color=c.muted,
                fontSize=10.sp
            )
            if(requests.isEmpty()&&!loading)Text(rsCloudPrivacyT105(lang,"none"),color=c.muted)
            requests.take(10).forEach{request->
                Text(
                    request.requestType.replace('_',' ').uppercase()+" · "+request.status.uppercase(),
                    color=c.bright,
                    fontWeight=FontWeight.Bold
                )
                Text(request.requestedLabel(),color=c.muted,fontSize=9.sp)
                if(request.staffNote.isNotBlank())Text(request.staffNote,color=c.text,fontSize=10.sp)
            }
        }
        RsPrivacyLinksV105(c,store,lang)
    }

    if(showDelete){
        AlertDialog(
            onDismissRequest={showDelete=false;deleteText=""},
            title={Text(rsAccountUiV40(lang,"delete_title"))},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(rsCloudPrivacyT105(lang,"delete_desc"))
                    OutlinedTextField(
                        deleteText,
                        {deleteText=it},
                        label={Text(rsAccountUiV40(lang,"type_delete"))}
                    )
                }
            },
            confirmButton={
                Button(
                    onClick={
                        busy=true
                        scope.launch{
                            rsCreateAccountRequestV78("account_deletion")
                                .onSuccess{
                                    status=rsCloudPrivacyT105(lang,"delete_submitted")
                                    revision++
                                    showDelete=false
                                    deleteText=""
                                }
                                .onFailure{status=rsReleaseT98(lang,"save_failed")}
                            busy=false
                        }
                    },
                    enabled=deleteText=="DELETE"&&!busy
                ){Text(if(busy)rsCloudPrivacyT105(lang,"submitting") else rsAccountUiV40(lang,"confirm"))}
            },
            dismissButton={
                TextButton(onClick={showDelete=false;deleteText=""}){Text(rsAccountUiV40(lang,"cancel"))}
            }
        )
    }
}

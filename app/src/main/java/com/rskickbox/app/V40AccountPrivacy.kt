package com.rskickbox.app

import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

private fun rsAccountUiV40(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "Settings & Privacy","sub" to "Preferences, privacy, local data export and account controls.",
        "prefs" to "PREFERENCES","push" to "Push notifications","booking" to "Booking reminders","training" to "Training reminders",
        "private_profile" to "Private profile","voice" to "Voice coach auto-speak","data" to "YOUR DATA",
        "export_desc" to "Create a JSON export of the local acceptance data linked to this student on this device.",
        "export" to "Export my local data","delete" to "Request account deletion","delete_title" to "Account deletion request",
        "delete_desc" to "Type DELETE to deactivate this local preview account. Production deletion will run server-side and may preserve records required by law.",
        "type_delete" to "Type DELETE","confirm" to "Confirm request","cancel" to "Cancel","export_ready" to "Local data export ready to share.",
        "delete_done" to "Local student access deactivated. Production backend deletion is not connected yet."
    )
    val nl=en+mapOf("title" to "Instellingen & Privacy","sub" to "Voorkeuren, privacy, lokale data-export en accountbeheer.","prefs" to "VOORKEUREN","push" to "Pushmeldingen","booking" to "Boekingsherinneringen","training" to "Trainingsherinneringen","private_profile" to "Privéprofiel","voice" to "Voice coach automatisch spreken","data" to "JOUW DATA","export_desc" to "Maak een JSON-export van lokale acceptatiedata voor deze leerling op dit apparaat.","export" to "Mijn lokale data exporteren","delete" to "Accountverwijdering aanvragen","delete_title" to "Verzoek accountverwijdering","delete_desc" to "Typ DELETE om dit lokale preview-account te deactiveren. Productieverwijdering gebeurt later server-side; wettelijk vereiste gegevens kunnen behouden blijven.","type_delete" to "Typ DELETE","confirm" to "Verzoek bevestigen","cancel" to "Annuleren","export_ready" to "Lokale data-export klaar om te delen.","delete_done" to "Lokale leerlingtoegang gedeactiveerd. Productieverwijdering is nog niet gekoppeld.")
    val pt=en+mapOf("title" to "Definições & Privacidade","sub" to "Preferências, privacidade, exportação local e controlo da conta.","prefs" to "PREFERÊNCIAS","push" to "Notificações push","booking" to "Lembretes de reservas","training" to "Lembretes de treino","private_profile" to "Perfil privado","voice" to "Coach de voz automático","data" to "OS TEUS DADOS","export_desc" to "Cria uma exportação JSON dos dados locais deste aluno neste dispositivo.","export" to "Exportar os meus dados locais","delete" to "Pedir eliminação da conta","delete_title" to "Pedido de eliminação","delete_desc" to "Escreve DELETE para desativar esta conta local de teste. A eliminação de produção será feita no servidor e poderá manter registos exigidos por lei.","type_delete" to "Escreve DELETE","confirm" to "Confirmar pedido","cancel" to "Cancelar","export_ready" to "Exportação local pronta para partilhar.","delete_done" to "Acesso local do aluno desativado. A eliminação no backend ainda não está ligada.")
    val es=en+mapOf("title" to "Ajustes & Privacidad","sub" to "Preferencias, privacidad, exportación local y controles de cuenta.","prefs" to "PREFERENCIAS","push" to "Notificaciones push","booking" to "Recordatorios de reservas","training" to "Recordatorios de entrenamiento","private_profile" to "Perfil privado","voice" to "Voz automática del coach","data" to "TUS DATOS","export_desc" to "Crea una exportación JSON de los datos locales de este alumno en este dispositivo.","export" to "Exportar mis datos locales","delete" to "Solicitar eliminación de cuenta","delete_title" to "Solicitud de eliminación","delete_desc" to "Escribe DELETE para desactivar esta cuenta local de prueba. La eliminación de producción será en servidor y puede conservar registros legales.","type_delete" to "Escribe DELETE","confirm" to "Confirmar solicitud","cancel" to "Cancelar","export_ready" to "Exportación local lista para compartir.","delete_done" to "Acceso local del alumno desactivado. La eliminación del backend aún no está conectada.")
    val fr=en+mapOf("title" to "Réglages & Confidentialité","sub" to "Préférences, confidentialité, export local et contrôle du compte.","prefs" to "PRÉFÉRENCES","push" to "Notifications push","booking" to "Rappels de réservation","training" to "Rappels d'entraînement","private_profile" to "Profil privé","voice" to "Réponses vocales automatiques","data" to "TES DONNÉES","export_desc" to "Crée un export JSON des données locales de cet élève sur cet appareil.","export" to "Exporter mes données locales","delete" to "Demander la suppression du compte","delete_title" to "Demande de suppression","delete_desc" to "Tape DELETE pour désactiver ce compte local de test. La suppression en production sera côté serveur et pourra conserver les données légalement requises.","type_delete" to "Tape DELETE","confirm" to "Confirmer la demande","cancel" to "Annuler","export_ready" to "Export local prêt à partager.","delete_done" to "Accès élève local désactivé. La suppression backend n'est pas encore connectée.")
    val de=en+mapOf("title" to "Einstellungen & Datenschutz","sub" to "Einstellungen, Datenschutz, lokaler Export und Kontosteuerung.","prefs" to "EINSTELLUNGEN","push" to "Push-Benachrichtigungen","booking" to "Buchungserinnerungen","training" to "Trainingserinnerungen","private_profile" to "Privates Profil","voice" to "Voice Coach automatisch","data" to "DEINE DATEN","export_desc" to "Erstellt einen JSON-Export der lokalen Daten dieses Schülers auf diesem Gerät.","export" to "Lokale Daten exportieren","delete" to "Kontolöschung anfordern","delete_title" to "Antrag auf Kontolöschung","delete_desc" to "Tippe DELETE, um dieses lokale Testkonto zu deaktivieren. Die Produktionslöschung erfolgt serverseitig; gesetzlich erforderliche Daten können erhalten bleiben.","type_delete" to "DELETE eingeben","confirm" to "Antrag bestätigen","cancel" to "Abbrechen","export_ready" to "Lokaler Export kann geteilt werden.","delete_done" to "Lokaler Schülerzugang deaktiviert. Backend-Löschung ist noch nicht verbunden.")
    val it=en+mapOf("title" to "Impostazioni & Privacy","sub" to "Preferenze, privacy, esportazione locale e controllo account.","prefs" to "PREFERENZE","push" to "Notifiche push","booking" to "Promemoria prenotazioni","training" to "Promemoria allenamenti","private_profile" to "Profilo privato","voice" to "Coach vocale automatico","data" to "I TUOI DATI","export_desc" to "Crea un export JSON dei dati locali di questo allievo sul dispositivo.","export" to "Esporta i miei dati locali","delete" to "Richiedi eliminazione account","delete_title" to "Richiesta eliminazione account","delete_desc" to "Digita DELETE per disattivare questo account locale di test. La cancellazione in produzione sarà server-side e potrà conservare record richiesti per legge.","type_delete" to "Digita DELETE","confirm" to "Conferma richiesta","cancel" to "Annulla","export_ready" to "Export locale pronto da condividere.","delete_done" to "Accesso locale allievo disattivato. La cancellazione backend non è ancora collegata.")
    val pl=en+mapOf("title" to "Ustawienia & Prywatność","sub" to "Preferencje, prywatność, eksport lokalny i kontrola konta.","prefs" to "PREFERENCJE","push" to "Powiadomienia push","booking" to "Przypomnienia o rezerwacji","training" to "Przypomnienia o treningu","private_profile" to "Profil prywatny","voice" to "Automatyczny głos trenera","data" to "TWOJE DANE","export_desc" to "Tworzy eksport JSON lokalnych danych tego ucznia na tym urządzeniu.","export" to "Eksportuj moje dane lokalne","delete" to "Poproś o usunięcie konta","delete_title" to "Żądanie usunięcia konta","delete_desc" to "Wpisz DELETE, aby dezaktywować lokalne konto testowe. Usuwanie produkcyjne będzie po stronie serwera; wymagane prawnie dane mogą zostać zachowane.","type_delete" to "Wpisz DELETE","confirm" to "Potwierdź żądanie","cancel" to "Anuluj","export_ready" to "Eksport lokalny gotowy do udostępnienia.","delete_done" to "Lokalny dostęp ucznia wyłączony. Usuwanie backendowe nie jest jeszcze podłączone.")
    val tr=en+mapOf("title" to "Ayarlar & Gizlilik","sub" to "Tercihler, gizlilik, yerel dışa aktarma ve hesap kontrolleri.","prefs" to "TERCİHLER","push" to "Anlık bildirimler","booking" to "Rezervasyon hatırlatmaları","training" to "Antrenman hatırlatmaları","private_profile" to "Özel profil","voice" to "Otomatik sesli koç","data" to "VERİLERİN","export_desc" to "Bu cihazdaki öğrenciye ait yerel verilerin JSON dışa aktarımını oluşturur.","export" to "Yerel verilerimi dışa aktar","delete" to "Hesap silme isteği","delete_title" to "Hesap silme isteği","delete_desc" to "Bu yerel test hesabını devre dışı bırakmak için DELETE yaz. Üretim silme işlemi sunucu tarafında yapılır ve yasal kayıtlar korunabilir.","type_delete" to "DELETE yaz","confirm" to "İsteği onayla","cancel" to "İptal","export_ready" to "Yerel veri dışa aktarımı paylaşmaya hazır.","delete_done" to "Yerel öğrenci erişimi devre dışı. Backend silme henüz bağlı değil.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
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
        clipData=ClipData.newRawUri("RS KICKBOX data export",uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent,"Share RS KICKBOX data export"))
}

@Composable
fun RsStudentPrivacyV40(c:RsPalette,store:RsStore,lang:RsLang,onLocalAccountDisabled:()->Unit){
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

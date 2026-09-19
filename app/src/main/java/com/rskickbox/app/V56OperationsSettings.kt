package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

object RsOpsKeysV56{
    const val MAINTENANCE="ops_maintenance_enabled_v56"
    const val MAINTENANCE_MESSAGE="ops_maintenance_message_v56"
    const val COMMUNITY_POSTS="ops_community_posts_v56"
    const val CLASS_BOOKING="ops_class_booking_v56"
    const val PRIVATE_LESSONS="ops_private_lessons_v56"
    const val REFERRALS="ops_referrals_v56"
    const val REMINDERS="ops_in_app_reminders_v56"
    const val RETENTION="ops_retention_v56"
}

fun rsOpsEnabledV56(store:RsStore,key:String,default:Boolean=true)=store.b(key,default)

fun rsOpsUiV56(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "App Settings & Operations",
        "sub" to "Live trainer controls that change what students can do inside RS KICKBOX.",
        "live" to "LIVE STUDENT CONTROLS",
        "maintenance" to "Maintenance banner",
        "maintenance_msg" to "Maintenance message",
        "maintenance_hint" to "Students keep access, but a prominent operational notice appears at the top of the app.",
        "community" to "Allow community posting",
        "community_hint" to "Students can still read active posts when posting is disabled.",
        "booking" to "Allow new class bookings",
        "booking_hint" to "Existing bookings can still be cancelled when new booking is disabled.",
        "private" to "Allow private-lesson requests",
        "private_hint" to "Existing lesson requests can still be cancelled.",
        "referrals" to "Allow referral sharing",
        "referrals_hint" to "Referral history remains visible when sharing is disabled.",
        "reminders" to "Enable in-app reminders",
        "reminders_hint" to "Controls local reminder surfaces. Production push delivery still requires the backend.",
        "data" to "DATA & PRIVACY POLICY SETTINGS",
        "retention" to "Local retention preference",
        "retention_hint" to "This preference must match the final privacy policy before production launch.",
        "status" to "OPERATIONAL STATUS",
        "enabled" to "ENABLED",
        "disabled" to "DISABLED",
        "saved" to "Settings save immediately.",
        "community_disabled" to "Community posting is temporarily disabled by the trainer.",
        "booking_disabled" to "New class bookings are temporarily disabled by the trainer.",
        "private_disabled" to "New private-lesson requests are temporarily disabled by the trainer.",
        "referrals_disabled" to "Referral sharing is temporarily disabled by the trainer."
    )
    val nl=en+mapOf(
        "title" to "App-instellingen & Operaties",
        "sub" to "Live trainerinstellingen die bepalen wat leerlingen in RS KICKBOX kunnen doen.",
        "live" to "LIVE LEERLINGINSTELLINGEN",
        "maintenance" to "Onderhoudsbanner",
        "maintenance_msg" to "Onderhoudsbericht",
        "maintenance_hint" to "Leerlingen houden toegang, maar zien bovenaan de app een duidelijk operationeel bericht.",
        "community" to "Community-posts toestaan",
        "community_hint" to "Leerlingen kunnen actieve posts blijven lezen wanneer plaatsen is uitgeschakeld.",
        "booking" to "Nieuwe lesboekingen toestaan",
        "booking_hint" to "Bestaande boekingen kunnen nog steeds worden geannuleerd.",
        "private" to "Privélesverzoeken toestaan",
        "private_hint" to "Bestaande privélesverzoeken kunnen nog steeds worden geannuleerd.",
        "referrals" to "Referrals delen toestaan",
        "referrals_hint" to "Referralhistorie blijft zichtbaar als delen uitstaat.",
        "reminders" to "In-app herinneringen inschakelen",
        "reminders_hint" to "Beheert lokale herinneringen. Productie-push vereist later de backend.",
        "data" to "DATA & PRIVACYBELEID",
        "retention" to "Lokale bewaartermijn",
        "retention_hint" to "Deze voorkeur moet bij release overeenkomen met het definitieve privacybeleid.",
        "status" to "OPERATIONELE STATUS",
        "enabled" to "INGESCHAKELD",
        "disabled" to "UITGESCHAKELD",
        "saved" to "Instellingen worden direct opgeslagen.",
        "community_disabled" to "Community-posts zijn tijdelijk uitgeschakeld door de trainer.",
        "booking_disabled" to "Nieuwe lesboekingen zijn tijdelijk uitgeschakeld door de trainer.",
        "private_disabled" to "Nieuwe privélesverzoeken zijn tijdelijk uitgeschakeld door de trainer.",
        "referrals_disabled" to "Referral delen is tijdelijk uitgeschakeld door de trainer."
    )
    val pt=en+mapOf(
        "title" to "Definições & Operações",
        "sub" to "Controlos do treinador que alteram o que os alunos podem fazer na app.",
        "live" to "CONTROLOS LIVE DOS ALUNOS",
        "maintenance" to "Banner de manutenção",
        "maintenance_msg" to "Mensagem de manutenção",
        "community" to "Permitir publicações na comunidade",
        "booking" to "Permitir novas reservas de aulas",
        "private" to "Permitir pedidos de aulas privadas",
        "referrals" to "Permitir partilha de referências",
        "reminders" to "Ativar lembretes na app",
        "data" to "DADOS & PRIVACIDADE",
        "retention" to "Preferência de retenção local",
        "status" to "ESTADO OPERACIONAL",
        "enabled" to "ATIVO",
        "disabled" to "DESATIVADO",
        "saved" to "As definições são guardadas imediatamente.",
        "community_disabled" to "As publicações na comunidade estão temporariamente desativadas pelo treinador.",
        "booking_disabled" to "Novas reservas de aulas estão temporariamente desativadas pelo treinador.",
        "private_disabled" to "Novos pedidos de aulas privadas estão temporariamente desativados pelo treinador.",
        "referrals_disabled" to "A partilha de referências está temporariamente desativada pelo treinador."
    )
    val es=en+mapOf(
        "title" to "Ajustes y Operaciones",
        "sub" to "Controles activos del entrenador que cambian lo que los alumnos pueden hacer dentro de RS KICKBOX.",
        "live" to "CONTROLES ACTIVOS DEL ALUMNO",
        "maintenance" to "Banner de mantenimiento",
        "maintenance_msg" to "Mensaje de mantenimiento",
        "maintenance_hint" to "Los alumnos mantienen el acceso, pero ven un aviso operativo destacado en la parte superior de la app.",
        "community" to "Permitir publicaciones en la comunidad",
        "community_hint" to "Los alumnos pueden seguir leyendo publicaciones activas cuando publicar está desactivado.",
        "booking" to "Permitir nuevas reservas de clases",
        "booking_hint" to "Las reservas existentes todavía pueden cancelarse.",
        "private" to "Permitir solicitudes de clases privadas",
        "private_hint" to "Las solicitudes existentes todavía pueden cancelarse.",
        "referrals" to "Permitir compartir referidos",
        "referrals_hint" to "El historial de referidos sigue visible cuando compartir está desactivado.",
        "reminders" to "Activar recordatorios en la app",
        "reminders_hint" to "Controla recordatorios locales. Las notificaciones push de producción requieren backend.",
        "data" to "DATOS Y PRIVACIDAD",
        "retention" to "Preferencia local de retención",
        "retention_hint" to "Debe coincidir con la política de privacidad final antes del lanzamiento.",
        "status" to "ESTADO OPERATIVO",
        "enabled" to "ACTIVADO",
        "disabled" to "DESACTIVADO",
        "saved" to "Los ajustes se guardan inmediatamente.",
        "community_disabled" to "Las publicaciones en la comunidad están temporalmente desactivadas por el entrenador.",
        "booking_disabled" to "Las nuevas reservas de clases están temporalmente desactivadas por el entrenador.",
        "private_disabled" to "Las nuevas solicitudes de clases privadas están temporalmente desactivadas por el entrenador.",
        "referrals_disabled" to "Compartir referidos está temporalmente desactivado por el entrenador."
    )
    val fr=en+mapOf(
        "title" to "Réglages & Opérations",
        "sub" to "Contrôles actifs du coach qui modifient ce que les élèves peuvent faire dans RS KICKBOX.",
        "live" to "CONTRÔLES ÉLÈVE ACTIFS",
        "maintenance" to "Bannière de maintenance",
        "maintenance_msg" to "Message de maintenance",
        "maintenance_hint" to "Les élèves gardent l’accès mais voient un avis opérationnel visible en haut de l’app.",
        "community" to "Autoriser les publications communautaires",
        "community_hint" to "Les élèves peuvent continuer à lire les publications actives si la publication est désactivée.",
        "booking" to "Autoriser les nouvelles réservations",
        "booking_hint" to "Les réservations existantes peuvent toujours être annulées.",
        "private" to "Autoriser les demandes de cours privés",
        "private_hint" to "Les demandes existantes peuvent toujours être annulées.",
        "referrals" to "Autoriser le partage de parrainage",
        "referrals_hint" to "L’historique reste visible si le partage est désactivé.",
        "reminders" to "Activer les rappels dans l’app",
        "reminders_hint" to "Contrôle les rappels locaux. Les push de production nécessitent le backend.",
        "data" to "DONNÉES & CONFIDENTIALITÉ",
        "retention" to "Préférence de conservation locale",
        "retention_hint" to "Doit correspondre à la politique de confidentialité finale avant lancement.",
        "status" to "ÉTAT OPÉRATIONNEL",
        "enabled" to "ACTIVÉ",
        "disabled" to "DÉSACTIVÉ",
        "saved" to "Les réglages sont enregistrés immédiatement.",
        "community_disabled" to "Les publications communautaires sont temporairement désactivées par l’entraîneur.",
        "booking_disabled" to "Les nouvelles réservations sont temporairement désactivées par l’entraîneur.",
        "private_disabled" to "Les nouvelles demandes de cours privés sont temporairement désactivées par l’entraîneur.",
        "referrals_disabled" to "Le partage de parrainage est temporairement désactivé par l’entraîneur."
    )
    val de=en+mapOf(
        "title" to "App-Einstellungen & Betrieb",
        "sub" to "Live-Trainersteuerungen, die beeinflussen, was Schüler in RS KICKBOX tun können.",
        "live" to "LIVE-SCHÜLERSTEUERUNGEN",
        "maintenance" to "Wartungsbanner",
        "maintenance_msg" to "Wartungsnachricht",
        "maintenance_hint" to "Schüler behalten Zugriff, sehen aber oben in der App einen deutlichen Betriebshinweis.",
        "community" to "Community-Beiträge erlauben",
        "community_hint" to "Aktive Beiträge bleiben lesbar, wenn das Posten deaktiviert ist.",
        "booking" to "Neue Kursbuchungen erlauben",
        "booking_hint" to "Bestehende Buchungen können weiterhin storniert werden.",
        "private" to "Anfragen für Privatstunden erlauben",
        "private_hint" to "Bestehende Anfragen können weiterhin storniert werden.",
        "referrals" to "Referral-Teilen erlauben",
        "referrals_hint" to "Der Referral-Verlauf bleibt sichtbar, wenn Teilen deaktiviert ist.",
        "reminders" to "In-App-Erinnerungen aktivieren",
        "reminders_hint" to "Steuert lokale Erinnerungen. Produktions-Push benötigt das Backend.",
        "data" to "DATEN & DATENSCHUTZ",
        "retention" to "Lokale Aufbewahrungspräferenz",
        "retention_hint" to "Muss vor Release zur finalen Datenschutzerklärung passen.",
        "status" to "BETRIEBSSTATUS",
        "enabled" to "AKTIV",
        "disabled" to "DEAKTIVIERT",
        "saved" to "Einstellungen werden sofort gespeichert.",
        "community_disabled" to "Community-Beiträge sind vorübergehend vom Trainer deaktiviert.",
        "booking_disabled" to "Neue Kursbuchungen sind vorübergehend vom Trainer deaktiviert.",
        "private_disabled" to "Neue Privatstunden-Anfragen sind vorübergehend vom Trainer deaktiviert.",
        "referrals_disabled" to "Referral-Teilen ist vorübergehend vom Trainer deaktiviert."
    )
    val it=en+mapOf(
        "title" to "Impostazioni & Operazioni",
        "sub" to "Controlli live del trainer che modificano ciò che gli allievi possono fare in RS KICKBOX.",
        "live" to "CONTROLLI LIVE ALLIEVI",
        "maintenance" to "Banner di manutenzione",
        "maintenance_msg" to "Messaggio di manutenzione",
        "maintenance_hint" to "Gli allievi mantengono l’accesso ma vedono un avviso operativo in alto nell’app.",
        "community" to "Consenti post nella community",
        "community_hint" to "Gli allievi possono continuare a leggere i post attivi se la pubblicazione è disattivata.",
        "booking" to "Consenti nuove prenotazioni lezioni",
        "booking_hint" to "Le prenotazioni esistenti possono ancora essere annullate.",
        "private" to "Consenti richieste di lezioni private",
        "private_hint" to "Le richieste esistenti possono ancora essere annullate.",
        "referrals" to "Consenti condivisione referral",
        "referrals_hint" to "La cronologia referral resta visibile se la condivisione è disattivata.",
        "reminders" to "Attiva promemoria nell’app",
        "reminders_hint" to "Controlla i promemoria locali. Le push di produzione richiedono il backend.",
        "data" to "DATI & PRIVACY",
        "retention" to "Preferenza di conservazione locale",
        "retention_hint" to "Deve corrispondere alla policy privacy finale prima del lancio.",
        "status" to "STATO OPERATIVO",
        "enabled" to "ATTIVO",
        "disabled" to "DISATTIVATO",
        "saved" to "Le impostazioni vengono salvate immediatamente.",
        "community_disabled" to "I post della community sono temporaneamente disattivati dal trainer.",
        "booking_disabled" to "Le nuove prenotazioni sono temporaneamente disattivate dal trainer.",
        "private_disabled" to "Le nuove richieste di lezioni private sono temporaneamente disattivate dal trainer.",
        "referrals_disabled" to "La condivisione referral è temporaneamente disattivata dal trainer."
    )
    val pl=en+mapOf(
        "title" to "Ustawienia i Operacje",
        "sub" to "Aktywne ustawienia trenera wpływające na to, co uczniowie mogą robić w RS KICKBOX.",
        "live" to "AKTYWNE KONTROLE UCZNIA",
        "maintenance" to "Baner konserwacji",
        "maintenance_msg" to "Komunikat konserwacyjny",
        "maintenance_hint" to "Uczniowie zachowują dostęp, ale widzą wyraźny komunikat operacyjny u góry aplikacji.",
        "community" to "Zezwalaj na posty społeczności",
        "community_hint" to "Uczniowie mogą nadal czytać aktywne posty, gdy publikowanie jest wyłączone.",
        "booking" to "Zezwalaj na nowe rezerwacje zajęć",
        "booking_hint" to "Istniejące rezerwacje można nadal anulować.",
        "private" to "Zezwalaj na prośby o lekcje prywatne",
        "private_hint" to "Istniejące prośby można nadal anulować.",
        "referrals" to "Zezwalaj na udostępnianie poleceń",
        "referrals_hint" to "Historia poleceń pozostaje widoczna, gdy udostępnianie jest wyłączone.",
        "reminders" to "Włącz przypomnienia w aplikacji",
        "reminders_hint" to "Steruje przypomnieniami lokalnymi. Push produkcyjny wymaga backendu.",
        "data" to "DANE I PRYWATNOŚĆ",
        "retention" to "Lokalna preferencja przechowywania",
        "retention_hint" to "Przed premierą musi odpowiadać finalnej polityce prywatności.",
        "status" to "STATUS OPERACYJNY",
        "enabled" to "WŁĄCZONE",
        "disabled" to "WYŁĄCZONE",
        "saved" to "Ustawienia zapisują się natychmiast.",
        "community_disabled" to "Publikowanie w społeczności zostało tymczasowo wyłączone przez trenera.",
        "booking_disabled" to "Nowe rezerwacje zajęć zostały tymczasowo wyłączone przez trenera.",
        "private_disabled" to "Nowe prośby o lekcje prywatne zostały tymczasowo wyłączone przez trenera.",
        "referrals_disabled" to "Udostępnianie poleceń zostało tymczasowo wyłączone przez trenera."
    )
    val tr=en+mapOf(
        "title" to "Uygulama Ayarları ve Operasyonlar",
        "sub" to "Öğrencilerin RS KICKBOX içinde neler yapabileceğini değiştiren canlı antrenör kontrolleri.",
        "live" to "CANLI ÖĞRENCİ KONTROLLERİ",
        "maintenance" to "Bakım bandı",
        "maintenance_msg" to "Bakım mesajı",
        "maintenance_hint" to "Öğrenciler erişimi korur ancak uygulamanın üstünde belirgin bir operasyon mesajı görür.",
        "community" to "Topluluk paylaşımına izin ver",
        "community_hint" to "Paylaşım kapalıyken öğrenciler aktif gönderileri okumaya devam edebilir.",
        "booking" to "Yeni ders rezervasyonlarına izin ver",
        "booking_hint" to "Mevcut rezervasyonlar yine iptal edilebilir.",
        "private" to "Özel ders isteklerine izin ver",
        "private_hint" to "Mevcut istekler yine iptal edilebilir.",
        "referrals" to "Referans paylaşımına izin ver",
        "referrals_hint" to "Paylaşım kapalıyken referans geçmişi görünür kalır.",
        "reminders" to "Uygulama içi hatırlatmaları aç",
        "reminders_hint" to "Yerel hatırlatmaları kontrol eder. Üretim push bildirimi backend gerektirir.",
        "data" to "VERİ & GİZLİLİK",
        "retention" to "Yerel saklama tercihi",
        "retention_hint" to "Yayın öncesinde nihai gizlilik politikasıyla eşleşmelidir.",
        "status" to "OPERASYON DURUMU",
        "enabled" to "AÇIK",
        "disabled" to "KAPALI",
        "saved" to "Ayarlar anında kaydedilir.",
        "community_disabled" to "Topluluk paylaşımı antrenör tarafından geçici olarak kapatıldı.",
        "booking_disabled" to "Yeni ders rezervasyonları antrenör tarafından geçici olarak kapatıldı.",
        "private_disabled" to "Yeni özel ders istekleri antrenör tarafından geçici olarak kapatıldı.",
        "referrals_disabled" to "Referans paylaşımı antrenör tarafından geçici olarak kapatıldı."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsOpsToggleV56(
    c:RsPalette,
    title:String,
    hint:String,
    value:Boolean,
    onChange:(Boolean)->Unit
){
    RsPanel(c){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text(title,color=c.text,fontWeight=FontWeight.Bold)
                Text(hint,color=c.muted,fontSize=10.sp)
            }
            Switch(value,onChange)
        }
    }
}

@Composable
fun RsAdminSettingsV56(c:RsPalette,store:RsStore,lang:RsLang){
    val scope=rememberCoroutineScope()
    val cloudMode=RsSupabaseV60.configured
    var maintenance by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.MAINTENANCE,false))}
    var maintenanceMessage by remember{
        mutableStateOf(store.s(RsOpsKeysV56.MAINTENANCE_MESSAGE,"RS KICKBOX maintenance notice: some services may be temporarily limited."))
    }
    var community by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.COMMUNITY_POSTS,true))}
    var booking by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.CLASS_BOOKING,true))}
    var privateLessons by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.PRIVATE_LESSONS,true))}
    var referrals by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.REFERRALS,true))}
    var reminders by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.REMINDERS,true))}
    var retention by remember{mutableStateOf(store.s(RsOpsKeysV56.RETENTION,"24 months"))}
    var disabledRoutes by remember{mutableStateOf(rsDisabledStudentRoutesV82(store))}
    var cloudLoading by remember{mutableStateOf(cloudMode)}
    var cloudSaving by remember{mutableStateOf(false)}
    var cloudStatus by remember{mutableStateOf("")}

    LaunchedEffect(cloudMode){
        if(cloudMode){
            cloudLoading=true
            rsSyncCloudControlsV82(store)
                .onSuccess{controls->
                    maintenance=controls.maintenanceEnabled
                    maintenanceMessage=controls.maintenanceMessage
                    community=controls.communityPostsEnabled
                    booking=controls.classBookingEnabled
                    privateLessons=controls.privateLessonsEnabled
                    referrals=controls.referralsEnabled
                    reminders=controls.inAppRemindersEnabled
                    retention=controls.retentionMonths.toString()+" months"
                    disabledRoutes=controls.disabledStudentRoutes.toSet()
                    cloudStatus="Live controls loaded from Supabase."
                }
                .onFailure{cloudStatus=it.message?:"Could not load cloud controls."}
            cloudLoading=false
        }
    }

    fun setBool(key:String,value:Boolean){store.pb(key,value)}

    fun featureLabel(route:String):String=when(route){
        "voice"->"AI Technique Coach"
        "session"->"Session Player"
        "academy"->"RS Academy"
        "techniques"->"Technique Library"
        "home_training"->"Home Training"
        "workout"->"Workout Generator"
        "classes"->"Classes & Bookings"
        "events"->"RS Events"
        "coachchat"->"Private Coach Chat"
        "community"->"Community"
        "groups"->"Groups"
        "private_lessons"->"Private Lessons"
        "notifications"->"Notifications"
        "promotions"->"Promotions"
        "checkin"->"Class Check-In"
        "documents"->"Club Documents"
        "referrals"->"Referrals"
        "progress"->"Progress"
        "challenges"->"Challenges"
        "badges"->"Badges"
        "fightcamp"->"Fight Camp"
        "compare"->"Technique Comparison"
        "history"->"Training History"
        "vault"->"Knowledge Vault"
        "homework"->"Homework"
        "favorites"->"Saved & Favorites"
        "media"->"Training Media"
        "music"->"My RS Music"
        "finance"->"Membership & Payments"
        "book"->"Trainer Book"
        "search"->"Search"
        else->route.replace('_',' ').replaceFirstChar{it.uppercase()}
    }

    RsScroll(c,rsOpsUiV56(lang,"title"),rsOpsUiV56(lang,"sub")){
        if(cloudMode)RsPanel(c){
            Text(
                if(cloudLoading)"Syncing live student controls…" else "Supabase student access controls connected",
                color=if(cloudLoading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(cloudStatus.isNotBlank())Text(cloudStatus,color=c.muted,fontSize=10.sp)
        }

        Text(rsOpsUiV56(lang,"live"),color=c.bright,fontWeight=FontWeight.Black)

        RsOpsToggleV56(
            c,rsOpsUiV56(lang,"maintenance"),rsOpsUiV56(lang,"maintenance_hint"),maintenance
        ){v->maintenance=v;setBool(RsOpsKeysV56.MAINTENANCE,v)}

        if(maintenance)RsPanel(c){
            OutlinedTextField(
                value=maintenanceMessage,
                onValueChange={
                    maintenanceMessage=it.take(240)
                    store.ps(RsOpsKeysV56.MAINTENANCE_MESSAGE,maintenanceMessage)
                },
                label={Text(rsOpsUiV56(lang,"maintenance_msg"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2
            )
        }

        RsOpsToggleV56(c,rsOpsUiV56(lang,"community"),rsOpsUiV56(lang,"community_hint"),community){
            community=it;setBool(RsOpsKeysV56.COMMUNITY_POSTS,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"booking"),rsOpsUiV56(lang,"booking_hint"),booking){
            booking=it;setBool(RsOpsKeysV56.CLASS_BOOKING,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"private"),rsOpsUiV56(lang,"private_hint"),privateLessons){
            privateLessons=it;setBool(RsOpsKeysV56.PRIVATE_LESSONS,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"referrals"),rsOpsUiV56(lang,"referrals_hint"),referrals){
            referrals=it;setBool(RsOpsKeysV56.REFERRALS,it)
        }
        RsOpsToggleV56(c,rsOpsUiV56(lang,"reminders"),rsOpsUiV56(lang,"reminders_hint"),reminders){
            reminders=it;setBool(RsOpsKeysV56.REMINDERS,it)
        }

        Text("STUDENT FEATURE ACCESS",color=c.bright,fontWeight=FontWeight.Black)
        RsPanel(c){
            Text(
                "Switch a student section OFF to remove it from the Student Dashboard, navigation and Student App Guide. Switch it ON to make it appear again.",
                color=c.muted,
                fontSize=10.sp
            )
        }

        RsStudentLockableRoutesV82.forEach{route->
            val enabled=route !in disabledRoutes
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween
                ){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                        Text(featureLabel(route),color=c.text,fontWeight=FontWeight.Bold)
                        Text(
                            if(enabled)"VISIBLE TO STUDENTS" else "HIDDEN / LOCKED",
                            color=if(enabled)c.bright else c.muted,
                            fontSize=9.sp,
                            fontWeight=FontWeight.Bold
                        )
                    }
                    Switch(
                        checked=enabled,
                        onCheckedChange={on->
                            disabledRoutes=if(on)disabledRoutes-route else disabledRoutes+route
                        },
                        enabled=!cloudLoading&&!cloudSaving
                    )
                }
            }
        }

        Text(rsOpsUiV56(lang,"data"),color=c.bright,fontWeight=FontWeight.Black)
        RsPanel(c){
            Text(rsOpsUiV56(lang,"retention"),color=c.text,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("12 months","24 months","36 months").forEach{x->
                    FilterChip(
                        selected=retention==x,
                        onClick={retention=x;store.ps(RsOpsKeysV56.RETENTION,x)},
                        label={Text(x,fontSize=9.sp)},
                        modifier=Modifier.weight(1f)
                    )
                }
            }
            Text(rsOpsUiV56(lang,"retention_hint"),color=c.muted,fontSize=10.sp)
        }

        if(cloudMode){
            Button(
                onClick={
                    cloudSaving=true
                    cloudStatus="Saving live controls…"
                    val retentionMonths=retention.substringBefore(' ').toIntOrNull()?:24
                    scope.launch{
                        rsSaveCloudControlsV82(
                            store,
                            maintenance,
                            maintenanceMessage,
                            community,
                            booking,
                            privateLessons,
                            referrals,
                            reminders,
                            retentionMonths,
                            disabledRoutes
                        )
                            .onSuccess{cloudStatus="✓ Student controls saved. They apply to the next student sync/login."}
                            .onFailure{cloudStatus=it.message?:"Could not save live controls."}
                        cloudSaving=false
                    }
                },
                enabled=!cloudLoading&&!cloudSaving,
                modifier=Modifier.fillMaxWidth()
            ){Text(if(cloudSaving)"Saving…" else "Save live student controls")}
        }

        Text(rsOpsUiV56(lang,"status"),color=c.bright,fontWeight=FontWeight.Black)
        RsPanel(c){
            listOf(
                rsOpsUiV56(lang,"community") to community,
                rsOpsUiV56(lang,"booking") to booking,
                rsOpsUiV56(lang,"private") to privateLessons,
                rsOpsUiV56(lang,"referrals") to referrals,
                rsOpsUiV56(lang,"reminders") to reminders
            ).forEach{(name,on)->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(name,color=c.text,fontSize=10.sp,modifier=Modifier.weight(1f))
                    Text(if(on)rsOpsUiV56(lang,"enabled") else rsOpsUiV56(lang,"disabled"),color=if(on)c.bright else c.muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                }
            }
            Text(
                (RsStudentLockableRoutesV82.size-disabledRoutes.size).toString()+" / "+RsStudentLockableRoutesV82.size+" student sections enabled",
                color=c.bright,
                fontSize=10.sp,
                fontWeight=FontWeight.Bold
            )
        }
    }
}

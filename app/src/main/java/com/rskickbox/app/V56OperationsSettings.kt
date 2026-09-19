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
        "sub" to "Live trainer controls that change what students can do inside RS KICKBOXING.",
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
        "sub" to "Live trainerinstellingen die bepalen wat leerlingen in RS KICKBOXING kunnen doen.",
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
        "sub" to "Controles activos del entrenador que cambian lo que los alumnos pueden hacer dentro de RS KICKBOXING.",
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
        "sub" to "Contrôles actifs du coach qui modifient ce que les élèves peuvent faire dans RS KICKBOXING.",
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
        "sub" to "Live-Trainersteuerungen, die beeinflussen, was Schüler in RS KICKBOXING tun können.",
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
        "sub" to "Controlli live del trainer che modificano ciò che gli allievi possono fare in RS KICKBOXING.",
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
        "sub" to "Aktywne ustawienia trenera wpływające na to, co uczniowie mogą robić w RS KICKBOXING.",
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
        "sub" to "Öğrencilerin RS KICKBOXING içinde neler yapabileceğini değiştiren canlı antrenör kontrolleri.",
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

private fun rsOpsCloudT105(lang:RsLang,key:String):String{
    val en=mapOf(
        "loaded" to "Live controls loaded from Supabase.","load_failed" to "Could not load cloud controls.",
        "syncing" to "Syncing live student controls…","connected" to "Supabase student access controls connected",
        "feature_help" to rsOpsCloudT105(lang,"feature_help"),
        "visible" to "VISIBLE TO STUDENTS","hidden" to "HIDDEN / LOCKED","saving" to "Saving live controls…",
        "saved" to "✓ Student controls saved. They apply to the next student sync/login.","save_failed" to "Could not save live controls.",
        "save_button" to "Save live student controls","sections_enabled" to "student sections enabled","months" to "months"
    )
    val nl=en+mapOf("loaded" to "Live instellingen geladen uit Supabase.","load_failed" to "Cloudinstellingen konden niet worden geladen.","syncing" to "Live leerlinginstellingen synchroniseren…","connected" to "Supabase leerlingtoegang verbonden","feature_help" to "Zet een leerlingsectie UIT om deze te verwijderen uit het Leerlingdashboard, de navigatie en de App Gids. Zet deze AAN om hem opnieuw zichtbaar te maken.","visible" to "ZICHTBAAR VOOR LEERLINGEN","hidden" to "VERBORGEN / GEBLOKKEERD","saving" to "Live instellingen opslaan…","saved" to "✓ Leerlinginstellingen opgeslagen. Ze gelden bij de volgende synchronisatie/login.","save_failed" to "Live instellingen konden niet worden opgeslagen.","save_button" to "Live leerlinginstellingen opslaan","sections_enabled" to "leerlingsecties ingeschakeld","months" to "maanden")
    val pt=en+mapOf("loaded" to "Controlos live carregados do Supabase.","load_failed" to "Não foi possível carregar os controlos cloud.","syncing" to "A sincronizar controlos live dos alunos…","connected" to "Controlos de acesso dos alunos ligados ao Supabase","feature_help" to "Desativa uma secção do aluno para a remover do Painel do Aluno, navegação e Guia da App. Ativa-a para voltar a aparecer.","visible" to "VISÍVEL PARA ALUNOS","hidden" to "OCULTO / BLOQUEADO","saving" to "A guardar controlos live…","saved" to "✓ Controlos dos alunos guardados. Aplicam-se na próxima sincronização/login.","save_failed" to "Não foi possível guardar os controlos live.","save_button" to "Guardar controlos live dos alunos","sections_enabled" to "secções de aluno ativas","months" to "meses")
    val es=en+mapOf("loaded" to "Controles activos cargados desde Supabase.","load_failed" to "No se pudieron cargar los controles cloud.","syncing" to "Sincronizando controles activos del alumno…","connected" to "Controles de acceso del alumno conectados a Supabase","feature_help" to "Desactiva una sección para quitarla del Panel del Alumno, navegación y Guía de la App. Actívala para que vuelva a aparecer.","visible" to "VISIBLE PARA ALUMNOS","hidden" to "OCULTO / BLOQUEADO","saving" to "Guardando controles activos…","saved" to "✓ Controles del alumno guardados. Se aplican en la próxima sincronización/inicio de sesión.","save_failed" to "No se pudieron guardar los controles activos.","save_button" to "Guardar controles activos del alumno","sections_enabled" to "secciones de alumno activadas","months" to "meses")
    val fr=en+mapOf("loaded" to "Contrôles actifs chargés depuis Supabase.","load_failed" to "Impossible de charger les contrôles cloud.","syncing" to "Synchronisation des contrôles élève…","connected" to "Contrôles d’accès élève connectés à Supabase","feature_help" to "Désactive une section élève pour la retirer du tableau de bord, de la navigation et du Guide App. Réactive-la pour la faire réapparaître.","visible" to "VISIBLE POUR LES ÉLÈVES","hidden" to "MASQUÉ / BLOQUÉ","saving" to "Enregistrement des contrôles…","saved" to "✓ Contrôles élève enregistrés. Ils s’appliquent à la prochaine synchronisation/connexion.","save_failed" to "Impossible d’enregistrer les contrôles.","save_button" to "Enregistrer les contrôles élève","sections_enabled" to "sections élève activées","months" to "mois")
    val de=en+mapOf("loaded" to "Live-Steuerungen aus Supabase geladen.","load_failed" to "Cloud-Steuerungen konnten nicht geladen werden.","syncing" to "Live-Schülersteuerungen werden synchronisiert…","connected" to "Supabase-Schülerzugriff verbunden","feature_help" to "Schalte einen Schülerbereich AUS, um ihn aus Schüler-Dashboard, Navigation und App-Anleitung zu entfernen. Schalte ihn EIN, damit er wieder erscheint.","visible" to "FÜR SCHÜLER SICHTBAR","hidden" to "AUSGEBLENDET / GESPERRT","saving" to "Live-Steuerungen werden gespeichert…","saved" to "✓ Schülersteuerungen gespeichert. Sie gelten bei der nächsten Synchronisierung/Anmeldung.","save_failed" to "Live-Steuerungen konnten nicht gespeichert werden.","save_button" to "Live-Schülersteuerungen speichern","sections_enabled" to "Schülerbereiche aktiviert","months" to "Monate")
    val it=en+mapOf("loaded" to "Controlli live caricati da Supabase.","load_failed" to "Impossibile caricare i controlli cloud.","syncing" to "Sincronizzazione controlli allievi…","connected" to "Controlli accesso allievi collegati a Supabase","feature_help" to "Disattiva una sezione allievo per rimuoverla da Dashboard, navigazione e Guida App. Riattivala per farla apparire di nuovo.","visible" to "VISIBILE AGLI ALLIEVI","hidden" to "NASCOSTO / BLOCCATO","saving" to "Salvataggio controlli live…","saved" to "✓ Controlli allievi salvati. Si applicano alla prossima sincronizzazione/accesso.","save_failed" to "Impossibile salvare i controlli live.","save_button" to "Salva controlli live allievi","sections_enabled" to "sezioni allievo attive","months" to "mesi")
    val pl=en+mapOf("loaded" to "Sterowanie na żywo wczytane z Supabase.","load_failed" to "Nie udało się wczytać sterowania z chmury.","syncing" to "Synchronizacja sterowania uczniów…","connected" to "Dostęp uczniów połączony z Supabase","feature_help" to "Wyłącz sekcję ucznia, aby usunąć ją z Panelu Ucznia, nawigacji i Przewodnika Aplikacji. Włącz ją, aby pojawiła się ponownie.","visible" to "WIDOCZNE DLA UCZNIÓW","hidden" to "UKRYTE / ZABLOKOWANE","saving" to "Zapisywanie sterowania…","saved" to "✓ Sterowanie uczniów zapisane. Zadziała przy następnej synchronizacji/logowaniu.","save_failed" to "Nie udało się zapisać sterowania.","save_button" to "Zapisz sterowanie uczniów","sections_enabled" to "sekcji ucznia aktywnych","months" to "miesiące")
    val tr=en+mapOf("loaded" to "Canlı kontroller Supabase’den yüklendi.","load_failed" to "Bulut kontrolleri yüklenemedi.","syncing" to "Canlı öğrenci kontrolleri eşitleniyor…","connected" to "Supabase öğrenci erişim kontrolleri bağlı","feature_help" to "Bir öğrenci bölümünü Öğrenci Paneli, gezinme ve Uygulama Rehberinden kaldırmak için KAPAT. Yeniden görünmesi için AÇ.","visible" to "ÖĞRENCİLERE GÖRÜNÜR","hidden" to "GİZLİ / KİLİTLİ","saving" to "Canlı kontroller kaydediliyor…","saved" to "✓ Öğrenci kontrolleri kaydedildi. Sonraki eşitleme/girişte uygulanır.","save_failed" to "Canlı kontroller kaydedilemedi.","save_button" to "Canlı öğrenci kontrollerini kaydet","sections_enabled" to "öğrenci bölümü etkin","months" to "ay")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}


@Composable
fun RsAdminSettingsV56(c:RsPalette,store:RsStore,lang:RsLang){
    val scope=rememberCoroutineScope()
    val cloudMode=RsSupabaseV60.configured
    var maintenance by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.MAINTENANCE,false))}
    var maintenanceMessage by remember{
        mutableStateOf(store.s(RsOpsKeysV56.MAINTENANCE_MESSAGE,rsCommonT95(lang,"maintenance_default")))
    }
    var community by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.COMMUNITY_POSTS,true))}
    var booking by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.CLASS_BOOKING,true))}
    var privateLessons by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.PRIVATE_LESSONS,true))}
    var referrals by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.REFERRALS,true))}
    var reminders by remember{mutableStateOf(rsOpsEnabledV56(store,RsOpsKeysV56.REMINDERS,true))}
    var retention by remember{mutableStateOf(store.s(RsOpsKeysV56.RETENTION,"24 months").substringBefore(' '))}
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
                    retention=controls.retentionMonths.toString()
                    disabledRoutes=controls.disabledStudentRoutes.toSet()
                    cloudStatus=rsOpsCloudT105(lang,"loaded")
                }
                .onFailure{cloudStatus=rsOpsCloudT105(lang,"load_failed")}
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
                if(cloudLoading)rsOpsCloudT105(lang,"syncing") else rsOpsCloudT105(lang,"connected"),
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

        Text(rsCloudT93(lang,"student_feature_access"),color=c.bright,fontWeight=FontWeight.Black)
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
                        Text(rsRouteTitle(lang,route,featureLabel(route)),color=c.text,fontWeight=FontWeight.Bold)
                        Text(
                            if(enabled)rsOpsCloudT105(lang,"visible") else rsOpsCloudT105(lang,"hidden"),
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
                listOf("12","24","36").forEach{x->
                    FilterChip(
                        selected=retention==x,
                        onClick={retention=x;store.ps(RsOpsKeysV56.RETENTION,x+" months")},
                        label={Text(x+" "+rsOpsCloudT105(lang,"months"),fontSize=9.sp)},
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
                    cloudStatus=rsOpsCloudT105(lang,"saving")
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
                            .onSuccess{cloudStatus=rsOpsCloudT105(lang,"saved")}
                            .onFailure{cloudStatus=rsOpsCloudT105(lang,"save_failed")}
                        cloudSaving=false
                    }
                },
                enabled=!cloudLoading&&!cloudSaving,
                modifier=Modifier.fillMaxWidth()
             ){Text(if(cloudSaving)rsReleaseT98(lang,"saving") else rsOpsCloudT105(lang,"save_button"))}
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
                (RsStudentLockableRoutesV82.size-disabledRoutes.size).toString()+" / "+RsStudentLockableRoutesV82.size+" "+rsOpsCloudT105(lang,"sections_enabled"),
                color=c.bright,
                fontSize=10.sp,
                fontWeight=FontWeight.Bold
            )
        }
    }
}

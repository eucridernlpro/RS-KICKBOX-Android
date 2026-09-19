package com.rskickbox.app

/**
 * Shared release-stage copy for older cloud screens that predate the centralized
 * localization pass. Keep every visible runtime status/error behind this helper.
 */
fun rsReleaseT98(lang:RsLang,key:String):String{
    val en=mapOf(
        "load_failed" to "Could not load the latest data.",
        "save_failed" to "Could not save the changes.",
        "update_failed" to "Could not update this item.",
        "delete_failed" to "Could not delete this item.",
        "syncing" to "Syncing…",
        "connected" to "Cloud connected",
        "saving" to "Saving…",
        "please_wait" to "Please wait…",
        "billing_sub" to "Your live membership and invoice history from RS KICKBOX.",
        "sync_billing" to "Syncing billing…",
        "billing_connected" to "Cloud billing connected",
        "per_month" to "per month",
        "invoice_sub" to "Live invoice ledger synchronized with RS KICKBOX cloud.",
        "sync_invoices" to "Syncing invoices…",
        "invoice_connected" to "Cloud invoice ledger connected",
        "invoice_paid" to "Invoice marked paid.",
        "invoice_pending" to "Invoice marked pending.",
        "notifications_admin_sub" to "Publish club updates to members across devices.",
        "notifications_student_sub" to "Club updates synchronized with your membership.",
        "notification_center_connected" to "Cloud notification center connected",
        "notifications_connected" to "Cloud notifications connected",
        "events_connected" to "Cloud events connected",
        "events_admin_sub" to "Create and manage shared club events with live RSVP capacity.",
        "event_manager_connected" to "Cloud event manager connected",
        "private_student_sub" to "Request private coaching from the shared trainer schedule.",
        "sync_private" to "Syncing private lessons…",
        "private_connected" to "Cloud private lessons connected",
        "private_admin_sub" to "Manage shared private lesson availability and student requests.",
        "trainer_schedule_connected" to "Cloud trainer schedule connected"
    )
    val nl=en+mapOf(
        "load_failed" to "De nieuwste gegevens konden niet worden geladen.","save_failed" to "De wijzigingen konden niet worden opgeslagen.","update_failed" to "Dit item kon niet worden bijgewerkt.","delete_failed" to "Dit item kon niet worden verwijderd.","syncing" to "Synchroniseren…","connected" to "Cloud verbonden","saving" to "Opslaan…","please_wait" to "Even wachten…",
        "billing_sub" to "Je actuele lidmaatschap en factuurgeschiedenis van RS KICKBOX.","sync_billing" to "Facturatie synchroniseren…","billing_connected" to "Cloudfacturatie verbonden","per_month" to "per maand",
        "invoice_sub" to "Live factuuroverzicht gesynchroniseerd met RS KICKBOX cloud.","sync_invoices" to "Facturen synchroniseren…","invoice_connected" to "Cloudfacturen verbonden","invoice_paid" to "Factuur als betaald gemarkeerd.","invoice_pending" to "Factuur als openstaand gemarkeerd.",
        "notifications_admin_sub" to "Publiceer clubupdates voor leden op al hun apparaten.","notifications_student_sub" to "Clubupdates gesynchroniseerd met jouw lidmaatschap.","notification_center_connected" to "Cloudmeldingenbeheer verbonden","notifications_connected" to "Cloudmeldingen verbonden",
        "events_connected" to "Cloudevents verbonden","events_admin_sub" to "Maak en beheer gedeelde clubevents met live RSVP-capaciteit.","event_manager_connected" to "Cloudeventbeheer verbonden",
        "private_student_sub" to "Vraag privécoaching aan via het gedeelde trainersschema.","sync_private" to "Privélessen synchroniseren…","private_connected" to "Cloudprivélessen verbonden","private_admin_sub" to "Beheer gedeelde privélesbeschikbaarheid en leerlingaanvragen.","trainer_schedule_connected" to "Cloud trainerschema verbonden"
    )
    val pt=en+mapOf(
        "load_failed" to "Não foi possível carregar os dados mais recentes.","save_failed" to "Não foi possível guardar as alterações.","update_failed" to "Não foi possível atualizar este item.","delete_failed" to "Não foi possível eliminar este item.","syncing" to "A sincronizar…","connected" to "Cloud ligada","saving" to "A guardar…","please_wait" to "Aguarda…",
        "billing_sub" to "O teu histórico atual de adesão e faturas RS KICKBOX.","sync_billing" to "A sincronizar faturação…","billing_connected" to "Faturação cloud ligada","per_month" to "por mês",
        "invoice_sub" to "Registo de faturas sincronizado com a cloud RS KICKBOX.","sync_invoices" to "A sincronizar faturas…","invoice_connected" to "Faturas cloud ligadas","invoice_paid" to "Fatura marcada como paga.","invoice_pending" to "Fatura marcada como pendente.",
        "notifications_admin_sub" to "Publica atualizações do clube para os membros em todos os dispositivos.","notifications_student_sub" to "Atualizações do clube sincronizadas com a tua adesão.","notification_center_connected" to "Centro de notificações cloud ligado","notifications_connected" to "Notificações cloud ligadas",
        "events_connected" to "Eventos cloud ligados","events_admin_sub" to "Cria e gere eventos partilhados com capacidade RSVP em tempo real.","event_manager_connected" to "Gestor de eventos cloud ligado",
        "private_student_sub" to "Pede coaching privado através do horário partilhado do treinador.","sync_private" to "A sincronizar aulas privadas…","private_connected" to "Aulas privadas cloud ligadas","private_admin_sub" to "Gere disponibilidade de aulas privadas e pedidos dos alunos.","trainer_schedule_connected" to "Horário cloud do treinador ligado"
    )
    val es=en+mapOf(
        "load_failed" to "No se pudieron cargar los datos más recientes.","save_failed" to "No se pudieron guardar los cambios.","update_failed" to "No se pudo actualizar este elemento.","delete_failed" to "No se pudo eliminar este elemento.","syncing" to "Sincronizando…","connected" to "Cloud conectada","saving" to "Guardando…","please_wait" to "Espera…",
        "billing_sub" to "Tu historial actual de membresía y facturas de RS KICKBOX.","sync_billing" to "Sincronizando facturación…","billing_connected" to "Facturación cloud conectada","per_month" to "al mes",
        "invoice_sub" to "Registro de facturas sincronizado con la cloud RS KICKBOX.","sync_invoices" to "Sincronizando facturas…","invoice_connected" to "Facturas cloud conectadas","invoice_paid" to "Factura marcada como pagada.","invoice_pending" to "Factura marcada como pendiente.",
        "notifications_admin_sub" to "Publica actualizaciones del club para los miembros en todos sus dispositivos.","notifications_student_sub" to "Actualizaciones del club sincronizadas con tu membresía.","notification_center_connected" to "Centro de notificaciones cloud conectado","notifications_connected" to "Notificaciones cloud conectadas",
        "events_connected" to "Eventos cloud conectados","events_admin_sub" to "Crea y gestiona eventos compartidos con capacidad RSVP en directo.","event_manager_connected" to "Gestor de eventos cloud conectado",
        "private_student_sub" to "Solicita coaching privado desde el horario compartido del entrenador.","sync_private" to "Sincronizando clases privadas…","private_connected" to "Clases privadas cloud conectadas","private_admin_sub" to "Gestiona disponibilidad de clases privadas y solicitudes de alumnos.","trainer_schedule_connected" to "Horario cloud del entrenador conectado"
    )
    val fr=en+mapOf(
        "load_failed" to "Impossible de charger les dernières données.","save_failed" to "Impossible d'enregistrer les modifications.","update_failed" to "Impossible de mettre cet élément à jour.","delete_failed" to "Impossible de supprimer cet élément.","syncing" to "Synchronisation…","connected" to "Cloud connecté","saving" to "Enregistrement…","please_wait" to "Patiente…",
        "billing_sub" to "Ton historique actuel d'adhésion et de factures RS KICKBOX.","sync_billing" to "Synchronisation de la facturation…","billing_connected" to "Facturation cloud connectée","per_month" to "par mois",
        "invoice_sub" to "Registre de factures synchronisé avec le cloud RS KICKBOX.","sync_invoices" to "Synchronisation des factures…","invoice_connected" to "Factures cloud connectées","invoice_paid" to "Facture marquée payée.","invoice_pending" to "Facture marquée en attente.",
        "notifications_admin_sub" to "Publie les mises à jour du club pour les membres sur tous leurs appareils.","notifications_student_sub" to "Mises à jour du club synchronisées avec ton adhésion.","notification_center_connected" to "Centre de notifications cloud connecté","notifications_connected" to "Notifications cloud connectées",
        "events_connected" to "Événements cloud connectés","events_admin_sub" to "Crée et gère des événements partagés avec capacité RSVP en direct.","event_manager_connected" to "Gestionnaire d'événements cloud connecté",
        "private_student_sub" to "Demande un coaching privé depuis l'agenda partagé de l'entraîneur.","sync_private" to "Synchronisation des cours privés…","private_connected" to "Cours privés cloud connectés","private_admin_sub" to "Gère les disponibilités privées et les demandes des élèves.","trainer_schedule_connected" to "Agenda cloud de l'entraîneur connecté"
    )
    val de=en+mapOf(
        "load_failed" to "Die neuesten Daten konnten nicht geladen werden.","save_failed" to "Die Änderungen konnten nicht gespeichert werden.","update_failed" to "Dieses Element konnte nicht aktualisiert werden.","delete_failed" to "Dieses Element konnte nicht gelöscht werden.","syncing" to "Synchronisieren…","connected" to "Cloud verbunden","saving" to "Speichern…","please_wait" to "Bitte warten…",
        "billing_sub" to "Deine aktuelle RS KICKBOX Mitgliedschafts- und Rechnungshistorie.","sync_billing" to "Abrechnung wird synchronisiert…","billing_connected" to "Cloud-Abrechnung verbunden","per_month" to "pro Monat",
        "invoice_sub" to "Live-Rechnungsübersicht mit der RS KICKBOX Cloud synchronisiert.","sync_invoices" to "Rechnungen werden synchronisiert…","invoice_connected" to "Cloud-Rechnungen verbunden","invoice_paid" to "Rechnung als bezahlt markiert.","invoice_pending" to "Rechnung als offen markiert.",
        "notifications_admin_sub" to "Veröffentliche Club-Updates für Mitglieder auf allen Geräten.","notifications_student_sub" to "Club-Updates mit deiner Mitgliedschaft synchronisiert.","notification_center_connected" to "Cloud-Benachrichtigungscenter verbunden","notifications_connected" to "Cloud-Benachrichtigungen verbunden",
        "events_connected" to "Cloud-Events verbunden","events_admin_sub" to "Erstelle und verwalte gemeinsame Club-Events mit Live-RSVP-Kapazität.","event_manager_connected" to "Cloud-Eventmanager verbunden",
        "private_student_sub" to "Fordere Privatcoaching über den gemeinsamen Trainerplan an.","sync_private" to "Privatstunden werden synchronisiert…","private_connected" to "Cloud-Privatstunden verbunden","private_admin_sub" to "Verwalte Privatstunden-Verfügbarkeit und Schüleranfragen.","trainer_schedule_connected" to "Cloud-Trainerplan verbunden"
    )
    val it=en+mapOf(
        "load_failed" to "Impossibile caricare i dati più recenti.","save_failed" to "Impossibile salvare le modifiche.","update_failed" to "Impossibile aggiornare questo elemento.","delete_failed" to "Impossibile eliminare questo elemento.","syncing" to "Sincronizzazione…","connected" to "Cloud collegato","saving" to "Salvataggio…","please_wait" to "Attendi…",
        "billing_sub" to "Il tuo storico attuale di abbonamento e fatture RS KICKBOX.","sync_billing" to "Sincronizzazione fatturazione…","billing_connected" to "Fatturazione cloud collegata","per_month" to "al mese",
        "invoice_sub" to "Registro fatture sincronizzato con la cloud RS KICKBOX.","sync_invoices" to "Sincronizzazione fatture…","invoice_connected" to "Fatture cloud collegate","invoice_paid" to "Fattura segnata come pagata.","invoice_pending" to "Fattura segnata come in attesa.",
        "notifications_admin_sub" to "Pubblica aggiornamenti del club per i membri su tutti i dispositivi.","notifications_student_sub" to "Aggiornamenti del club sincronizzati con il tuo abbonamento.","notification_center_connected" to "Centro notifiche cloud collegato","notifications_connected" to "Notifiche cloud collegate",
        "events_connected" to "Eventi cloud collegati","events_admin_sub" to "Crea e gestisci eventi condivisi con capienza RSVP in tempo reale.","event_manager_connected" to "Gestore eventi cloud collegato",
        "private_student_sub" to "Richiedi coaching privato dall'agenda condivisa del trainer.","sync_private" to "Sincronizzazione lezioni private…","private_connected" to "Lezioni private cloud collegate","private_admin_sub" to "Gestisci disponibilità privata e richieste degli allievi.","trainer_schedule_connected" to "Agenda cloud del trainer collegata"
    )
    val pl=en+mapOf(
        "load_failed" to "Nie udało się wczytać najnowszych danych.","save_failed" to "Nie udało się zapisać zmian.","update_failed" to "Nie udało się zaktualizować tego elementu.","delete_failed" to "Nie udało się usunąć tego elementu.","syncing" to "Synchronizacja…","connected" to "Chmura połączona","saving" to "Zapisywanie…","please_wait" to "Poczekaj…",
        "billing_sub" to "Twoja aktualna historia członkostwa i faktur RS KICKBOX.","sync_billing" to "Synchronizacja rozliczeń…","billing_connected" to "Rozliczenia w chmurze połączone","per_month" to "miesięcznie",
        "invoice_sub" to "Rejestr faktur zsynchronizowany z chmurą RS KICKBOX.","sync_invoices" to "Synchronizacja faktur…","invoice_connected" to "Faktury w chmurze połączone","invoice_paid" to "Faktura oznaczona jako opłacona.","invoice_pending" to "Faktura oznaczona jako oczekująca.",
        "notifications_admin_sub" to "Publikuj aktualizacje klubu dla członków na wszystkich urządzeniach.","notifications_student_sub" to "Aktualizacje klubu zsynchronizowane z Twoim członkostwem.","notification_center_connected" to "Centrum powiadomień w chmurze połączone","notifications_connected" to "Powiadomienia w chmurze połączone",
        "events_connected" to "Wydarzenia w chmurze połączone","events_admin_sub" to "Twórz i zarządzaj wydarzeniami z bieżącą liczbą RSVP.","event_manager_connected" to "Menedżer wydarzeń w chmurze połączony",
        "private_student_sub" to "Poproś o prywatny trening z udostępnionego grafiku trenera.","sync_private" to "Synchronizacja lekcji prywatnych…","private_connected" to "Lekcje prywatne w chmurze połączone","private_admin_sub" to "Zarządzaj dostępnością lekcji prywatnych i prośbami uczniów.","trainer_schedule_connected" to "Grafik trenera w chmurze połączony"
    )
    val tr=en+mapOf(
        "load_failed" to "En güncel veriler yüklenemedi.","save_failed" to "Değişiklikler kaydedilemedi.","update_failed" to "Bu öğe güncellenemedi.","delete_failed" to "Bu öğe silinemedi.","syncing" to "Eşitleniyor…","connected" to "Bulut bağlı","saving" to "Kaydediliyor…","please_wait" to "Lütfen bekle…",
        "billing_sub" to "Güncel RS KICKBOX üyelik ve fatura geçmişiniz.","sync_billing" to "Faturalama eşitleniyor…","billing_connected" to "Bulut faturalama bağlı","per_month" to "aylık",
        "invoice_sub" to "Canlı fatura kaydı RS KICKBOX bulutuyla eşitlenmiştir.","sync_invoices" to "Faturalar eşitleniyor…","invoice_connected" to "Bulut faturaları bağlı","invoice_paid" to "Fatura ödendi olarak işaretlendi.","invoice_pending" to "Fatura beklemede olarak işaretlendi.",
        "notifications_admin_sub" to "Kulüp güncellemelerini üyelerin tüm cihazlarında yayınlayın.","notifications_student_sub" to "Kulüp güncellemeleri üyeliğinizle eşitlenir.","notification_center_connected" to "Bulut bildirim merkezi bağlı","notifications_connected" to "Bulut bildirimleri bağlı",
        "events_connected" to "Bulut etkinlikleri bağlı","events_admin_sub" to "Canlı RSVP kapasitesiyle paylaşılan kulüp etkinlikleri oluşturun ve yönetin.","event_manager_connected" to "Bulut etkinlik yöneticisi bağlı",
        "private_student_sub" to "Paylaşılan antrenör takviminden özel koçluk isteyin.","sync_private" to "Özel dersler eşitleniyor…","private_connected" to "Bulut özel dersleri bağlı","private_admin_sub" to "Özel ders uygunluğunu ve öğrenci taleplerini yönetin.","trainer_schedule_connected" to "Bulut antrenör takvimi bağlı"
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

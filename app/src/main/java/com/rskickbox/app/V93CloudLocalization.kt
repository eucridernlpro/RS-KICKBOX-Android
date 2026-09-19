package com.rskickbox.app

fun rsCloudT93(lang:RsLang,key:String):String{
    val en=mapOf(
        "level" to "Level","date" to "Date YYYY-MM-DD","time" to "Time HH:mm","minutes" to "Minutes",
        "class_created" to "Class created and published.","class_deleted" to "Class deleted.","no_active_students" to "No active students found.",
        "email" to "Email","student_email" to "Student email","no_invoices" to "No invoices yet.","invoice_created" to "Invoice created.","invoice_deleted" to "Invoice deleted.",
        "notification_published" to "Notification published.","notification_deleted" to "Notification deleted.","event_created" to "Event created.",
        "confirmed" to "CONFIRMED","declined" to "DECLINED","requested" to "REQUESTED","no_private_slots" to "No private lesson availability yet.",
        "lesson_request_sent" to "Private lesson request sent.","lesson_request_cancelled" to "Private lesson request cancelled.",
        "lesson_slot_created" to "Private lesson slot created.","lesson_slot_deleted" to "Private lesson slot deleted.","lesson_confirmed" to "Private lesson confirmed.","lesson_declined" to "Private lesson declined.",
        "homework_assigned" to "Homework assigned.","coach_note_saved" to "Private coach note saved.","assessment_saved" to "Assessment saved.",
        "challenge_assigned" to "Challenge assigned.","challenge_deleted" to "Challenge deleted.","fightcamp_saved" to "Fight Camp saved.","syncing_badges" to "Syncing badges…",
        "content_saved" to "Training content saved.","content_deleted" to "Training content deleted.",
        "plans_saved" to "Membership plans saved.","plan_updated" to "Plan updated.",
        "uploading_photo" to "Uploading profile photo…","photo_updated" to "✓ Profile photo updated.","saving_profile" to "Saving profile…","profile_saved" to "✓ Profile saved to cloud.",
        "post_published" to "Post published.","post_deleted" to "Post deleted.",
        "publishing_session" to "Publishing session…","session_published" to "✓ Session published and set active.","session_deleted" to "Session deleted.",
        "creating_qr" to "Creating secure attendance QR…","qr_ready" to "✓ QR ready. It expires automatically.","qr_valid" to "Valid for about 15 minutes",
        "checkin_identity" to "Your check-in is tied to your signed-in student account.","checking_in" to "Checking in…",
        "academy_saved" to "Academy progress saved.","academy_reset" to "Academy track reset.",
        "downloading_media" to "Downloading protected training media…","uploading_media" to "Uploading protected training media…","media_published" to "Training media published.","media_deleted" to "Training media deleted.",
        "student_feature_access" to "STUDENT FEATURE ACCESS",
        "syncing" to "Syncing…","connected" to "Cloud connected","saving" to "Saving…","creating" to "Creating…","deleting" to "Deleting…",
        "please_wait" to "Please wait…","could_not_save" to "Could not save changes."
    )
    val nl=en+mapOf(
        "level" to "Niveau","date" to "Datum JJJJ-MM-DD","time" to "Tijd UU:mm","minutes" to "Minuten",
        "class_created" to "Les aangemaakt en gepubliceerd.","class_deleted" to "Les verwijderd.","no_active_students" to "Geen actieve leerlingen gevonden.",
        "email" to "E-mail","student_email" to "E-mail leerling","no_invoices" to "Nog geen facturen.","invoice_created" to "Factuur aangemaakt.","invoice_deleted" to "Factuur verwijderd.",
        "notification_published" to "Melding gepubliceerd.","notification_deleted" to "Melding verwijderd.","event_created" to "Evenement aangemaakt.",
        "confirmed" to "BEVESTIGD","declined" to "AFGEWEZEN","requested" to "AANGEVRAAGD","no_private_slots" to "Nog geen privéles-beschikbaarheid.",
        "lesson_request_sent" to "Privélesaanvraag verstuurd.","lesson_request_cancelled" to "Privélesaanvraag geannuleerd.","lesson_slot_created" to "Privélesmoment aangemaakt.","lesson_slot_deleted" to "Privélesmoment verwijderd.","lesson_confirmed" to "Privéles bevestigd.","lesson_declined" to "Privéles afgewezen.",
        "homework_assigned" to "Huiswerk toegewezen.","coach_note_saved" to "Privé coachnotitie opgeslagen.","assessment_saved" to "Beoordeling opgeslagen.",
        "challenge_assigned" to "Uitdaging toegewezen.","challenge_deleted" to "Uitdaging verwijderd.","fightcamp_saved" to "Fight Camp opgeslagen.","syncing_badges" to "Badges synchroniseren…",
        "content_saved" to "Trainingscontent opgeslagen.","content_deleted" to "Trainingscontent verwijderd.","plans_saved" to "Lidmaatschapsplannen opgeslagen.","plan_updated" to "Plan bijgewerkt.",
        "uploading_photo" to "Profielfoto uploaden…","photo_updated" to "✓ Profielfoto bijgewerkt.","saving_profile" to "Profiel opslaan…","profile_saved" to "✓ Profiel opgeslagen in de cloud.","post_published" to "Bericht gepubliceerd.","post_deleted" to "Bericht verwijderd.",
        "publishing_session" to "Sessie publiceren…","session_published" to "✓ Sessie gepubliceerd en actief gezet.","session_deleted" to "Sessie verwijderd.","creating_qr" to "Veilige aanwezigheids-QR maken…","qr_ready" to "✓ QR klaar. Deze verloopt automatisch.","qr_valid" to "Ongeveer 15 minuten geldig","checkin_identity" to "Je check-in is gekoppeld aan je ingelogde leerlingaccount.","checking_in" to "Inchecken…",
        "academy_saved" to "Academy-voortgang opgeslagen.","academy_reset" to "Academy-traject gereset.","downloading_media" to "Beveiligde trainingsmedia downloaden…","uploading_media" to "Beveiligde trainingsmedia uploaden…","media_published" to "Trainingsmedia gepubliceerd.","media_deleted" to "Trainingsmedia verwijderd.","student_feature_access" to "TOEGANG LEERLINGFUNCTIES",
        "syncing" to "Synchroniseren…","connected" to "Cloud verbonden","saving" to "Opslaan…","creating" to "Aanmaken…","deleting" to "Verwijderen…","please_wait" to "Even wachten…","could_not_save" to "Wijzigingen konden niet worden opgeslagen."
    )
    val pt=en+mapOf(
        "level" to "Nível","date" to "Data AAAA-MM-DD","time" to "Hora HH:mm","minutes" to "Minutos",
        "class_created" to "Aula criada e publicada.","class_deleted" to "Aula eliminada.","no_active_students" to "Nenhum aluno ativo encontrado.",
        "email" to "E-mail","student_email" to "E-mail do aluno","no_invoices" to "Ainda não há faturas.","invoice_created" to "Fatura criada.","invoice_deleted" to "Fatura eliminada.",
        "notification_published" to "Notificação publicada.","notification_deleted" to "Notificação eliminada.","event_created" to "Evento criado.",
        "confirmed" to "CONFIRMADA","declined" to "RECUSADA","requested" to "PEDIDA","no_private_slots" to "Ainda não há disponibilidade para aulas privadas.",
        "lesson_request_sent" to "Pedido de aula privada enviado.","lesson_request_cancelled" to "Pedido de aula privada cancelado.","lesson_slot_created" to "Horário de aula privada criado.","lesson_slot_deleted" to "Horário de aula privada eliminado.","lesson_confirmed" to "Aula privada confirmada.","lesson_declined" to "Aula privada recusada.",
        "homework_assigned" to "Tarefa atribuída.","coach_note_saved" to "Nota privada do treinador guardada.","assessment_saved" to "Avaliação guardada.",
        "challenge_assigned" to "Desafio atribuído.","challenge_deleted" to "Desafio eliminado.","fightcamp_saved" to "Fight Camp guardado.","syncing_badges" to "A sincronizar emblemas…",
        "content_saved" to "Conteúdo de treino guardado.","content_deleted" to "Conteúdo de treino eliminado.","plans_saved" to "Planos de adesão guardados.","plan_updated" to "Plano atualizado.",
        "uploading_photo" to "A carregar foto de perfil…","photo_updated" to "✓ Foto de perfil atualizada.","saving_profile" to "A guardar perfil…","profile_saved" to "✓ Perfil guardado na cloud.","post_published" to "Publicação publicada.","post_deleted" to "Publicação eliminada.",
        "publishing_session" to "A publicar sessão…","session_published" to "✓ Sessão publicada e ativada.","session_deleted" to "Sessão eliminada.","creating_qr" to "A criar QR seguro de presença…","qr_ready" to "✓ QR pronto. Expira automaticamente.","qr_valid" to "Válido durante cerca de 15 minutos","checkin_identity" to "O check-in fica ligado à tua conta de aluno autenticada.","checking_in" to "A fazer check-in…",
        "academy_saved" to "Progresso da Academia guardado.","academy_reset" to "Percurso da Academia reiniciado.","downloading_media" to "A descarregar media de treino protegida…","uploading_media" to "A carregar media de treino protegida…","media_published" to "Media de treino publicada.","media_deleted" to "Media de treino eliminada.","student_feature_access" to "ACESSO ÀS FUNÇÕES DO ALUNO",
        "syncing" to "A sincronizar…","connected" to "Cloud ligada","saving" to "A guardar…","creating" to "A criar…","deleting" to "A eliminar…","please_wait" to "Aguarda…","could_not_save" to "Não foi possível guardar as alterações."
    )
    val es=en+mapOf(
        "level" to "Nivel","date" to "Fecha AAAA-MM-DD","time" to "Hora HH:mm","minutes" to "Minutos",
        "class_created" to "Clase creada y publicada.","class_deleted" to "Clase eliminada.","no_active_students" to "No se encontraron alumnos activos.",
        "email" to "Correo","student_email" to "Correo del alumno","no_invoices" to "Aún no hay facturas.","invoice_created" to "Factura creada.","invoice_deleted" to "Factura eliminada.",
        "notification_published" to "Notificación publicada.","notification_deleted" to "Notificación eliminada.","event_created" to "Evento creado.",
        "confirmed" to "CONFIRMADA","declined" to "RECHAZADA","requested" to "SOLICITADA","no_private_slots" to "Aún no hay disponibilidad de clases privadas.",
        "lesson_request_sent" to "Solicitud de clase privada enviada.","lesson_request_cancelled" to "Solicitud de clase privada cancelada.","lesson_slot_created" to "Horario privado creado.","lesson_slot_deleted" to "Horario privado eliminado.","lesson_confirmed" to "Clase privada confirmada.","lesson_declined" to "Clase privada rechazada.",
        "homework_assigned" to "Tarea asignada.","coach_note_saved" to "Nota privada del entrenador guardada.","assessment_saved" to "Evaluación guardada.",
        "challenge_assigned" to "Desafío asignado.","challenge_deleted" to "Desafío eliminado.","fightcamp_saved" to "Fight Camp guardado.","syncing_badges" to "Sincronizando insignias…",
        "content_saved" to "Contenido de entrenamiento guardado.","content_deleted" to "Contenido de entrenamiento eliminado.","plans_saved" to "Planes de membresía guardados.","plan_updated" to "Plan actualizado.",
        "uploading_photo" to "Subiendo foto de perfil…","photo_updated" to "✓ Foto de perfil actualizada.","saving_profile" to "Guardando perfil…","profile_saved" to "✓ Perfil guardado en la nube.","post_published" to "Publicación creada.","post_deleted" to "Publicación eliminada.",
        "publishing_session" to "Publicando sesión…","session_published" to "✓ Sesión publicada y activada.","session_deleted" to "Sesión eliminada.","creating_qr" to "Creando QR seguro de asistencia…","qr_ready" to "✓ QR listo. Caduca automáticamente.","qr_valid" to "Válido durante unos 15 minutos","checkin_identity" to "Tu check-in está vinculado a tu cuenta de alumno iniciada.","checking_in" to "Registrando asistencia…",
        "academy_saved" to "Progreso de Academia guardado.","academy_reset" to "Ruta de Academia reiniciada.","downloading_media" to "Descargando media de entrenamiento protegida…","uploading_media" to "Subiendo media de entrenamiento protegida…","media_published" to "Media de entrenamiento publicada.","media_deleted" to "Media de entrenamiento eliminada.","student_feature_access" to "ACCESO A FUNCIONES DEL ALUMNO",
        "syncing" to "Sincronizando…","connected" to "Cloud conectada","saving" to "Guardando…","creating" to "Creando…","deleting" to "Eliminando…","please_wait" to "Espera…","could_not_save" to "No se pudieron guardar los cambios."
    )
    val fr=en+mapOf(
        "level" to "Niveau","date" to "Date AAAA-MM-JJ","time" to "Heure HH:mm","minutes" to "Minutes",
        "class_created" to "Cours créé et publié.","class_deleted" to "Cours supprimé.","no_active_students" to "Aucun élève actif trouvé.",
        "email" to "E-mail","student_email" to "E-mail élève","no_invoices" to "Aucune facture.","invoice_created" to "Facture créée.","invoice_deleted" to "Facture supprimée.",
        "notification_published" to "Notification publiée.","notification_deleted" to "Notification supprimée.","event_created" to "Événement créé.",
        "confirmed" to "CONFIRMÉ","declined" to "REFUSÉ","requested" to "DEMANDÉ","no_private_slots" to "Aucune disponibilité de cours privé.",
        "lesson_request_sent" to "Demande de cours privé envoyée.","lesson_request_cancelled" to "Demande de cours privé annulée.","lesson_slot_created" to "Créneau privé créé.","lesson_slot_deleted" to "Créneau privé supprimé.","lesson_confirmed" to "Cours privé confirmé.","lesson_declined" to "Cours privé refusé.",
        "homework_assigned" to "Devoir attribué.","coach_note_saved" to "Note privée du coach enregistrée.","assessment_saved" to "Évaluation enregistrée.",
        "challenge_assigned" to "Défi attribué.","challenge_deleted" to "Défi supprimé.","fightcamp_saved" to "Fight Camp enregistré.","syncing_badges" to "Synchronisation des badges…",
        "content_saved" to "Contenu d'entraînement enregistré.","content_deleted" to "Contenu d'entraînement supprimé.","plans_saved" to "Formules enregistrées.","plan_updated" to "Formule mise à jour.",
        "uploading_photo" to "Import de la photo de profil…","photo_updated" to "✓ Photo de profil mise à jour.","saving_profile" to "Enregistrement du profil…","profile_saved" to "✓ Profil enregistré dans le cloud.","post_published" to "Publication publiée.","post_deleted" to "Publication supprimée.",
        "publishing_session" to "Publication de la session…","session_published" to "✓ Session publiée et activée.","session_deleted" to "Session supprimée.","creating_qr" to "Création du QR de présence sécurisé…","qr_ready" to "✓ QR prêt. Il expire automatiquement.","qr_valid" to "Valable environ 15 minutes","checkin_identity" to "Ton check-in est lié à ton compte élève connecté.","checking_in" to "Check-in en cours…",
        "academy_saved" to "Progression Académie enregistrée.","academy_reset" to "Parcours Académie réinitialisé.","downloading_media" to "Téléchargement du média protégé…","uploading_media" to "Import du média protégé…","media_published" to "Média d'entraînement publié.","media_deleted" to "Média d'entraînement supprimé.","student_feature_access" to "ACCÈS AUX FONCTIONS ÉLÈVE",
        "syncing" to "Synchronisation…","connected" to "Cloud connecté","saving" to "Enregistrement…","creating" to "Création…","deleting" to "Suppression…","please_wait" to "Patiente…","could_not_save" to "Impossible d'enregistrer les modifications."
    )
    val de=en+mapOf(
        "level" to "Niveau","date" to "Datum JJJJ-MM-TT","time" to "Zeit HH:mm","minutes" to "Minuten",
        "class_created" to "Kurs erstellt und veröffentlicht.","class_deleted" to "Kurs gelöscht.","no_active_students" to "Keine aktiven Schüler gefunden.",
        "email" to "E-Mail","student_email" to "Schüler-E-Mail","no_invoices" to "Noch keine Rechnungen.","invoice_created" to "Rechnung erstellt.","invoice_deleted" to "Rechnung gelöscht.",
        "notification_published" to "Benachrichtigung veröffentlicht.","notification_deleted" to "Benachrichtigung gelöscht.","event_created" to "Event erstellt.",
        "confirmed" to "BESTÄTIGT","declined" to "ABGELEHNT","requested" to "ANGEFRAGT","no_private_slots" to "Noch keine Privatstunden verfügbar.",
        "lesson_request_sent" to "Privatstunden-Anfrage gesendet.","lesson_request_cancelled" to "Privatstunden-Anfrage storniert.","lesson_slot_created" to "Privatstunden-Termin erstellt.","lesson_slot_deleted" to "Privatstunden-Termin gelöscht.","lesson_confirmed" to "Privatstunde bestätigt.","lesson_declined" to "Privatstunde abgelehnt.",
        "homework_assigned" to "Hausaufgabe zugewiesen.","coach_note_saved" to "Private Trainernotiz gespeichert.","assessment_saved" to "Bewertung gespeichert.",
        "challenge_assigned" to "Challenge zugewiesen.","challenge_deleted" to "Challenge gelöscht.","fightcamp_saved" to "Fight Camp gespeichert.","syncing_badges" to "Abzeichen werden synchronisiert…",
        "content_saved" to "Trainingsinhalt gespeichert.","content_deleted" to "Trainingsinhalt gelöscht.","plans_saved" to "Mitgliedschaftspläne gespeichert.","plan_updated" to "Plan aktualisiert.",
        "uploading_photo" to "Profilfoto wird hochgeladen…","photo_updated" to "✓ Profilfoto aktualisiert.","saving_profile" to "Profil wird gespeichert…","profile_saved" to "✓ Profil in der Cloud gespeichert.","post_published" to "Beitrag veröffentlicht.","post_deleted" to "Beitrag gelöscht.",
        "publishing_session" to "Session wird veröffentlicht…","session_published" to "✓ Session veröffentlicht und aktiviert.","session_deleted" to "Session gelöscht.","creating_qr" to "Sicherer Anwesenheits-QR wird erstellt…","qr_ready" to "✓ QR bereit. Er läuft automatisch ab.","qr_valid" to "Etwa 15 Minuten gültig","checkin_identity" to "Dein Check-in ist mit deinem angemeldeten Schülerkonto verknüpft.","checking_in" to "Check-in läuft…",
        "academy_saved" to "Academy-Fortschritt gespeichert.","academy_reset" to "Academy-Track zurückgesetzt.","downloading_media" to "Geschützte Trainingsmedien werden geladen…","uploading_media" to "Geschützte Trainingsmedien werden hochgeladen…","media_published" to "Trainingsmedien veröffentlicht.","media_deleted" to "Trainingsmedien gelöscht.","student_feature_access" to "SCHÜLER-FUNKTIONSZUGRIFF",
        "syncing" to "Synchronisieren…","connected" to "Cloud verbunden","saving" to "Speichern…","creating" to "Erstellen…","deleting" to "Löschen…","please_wait" to "Bitte warten…","could_not_save" to "Änderungen konnten nicht gespeichert werden."
    )
    val it=en+mapOf(
        "level" to "Livello","date" to "Data AAAA-MM-GG","time" to "Ora HH:mm","minutes" to "Minuti",
        "class_created" to "Lezione creata e pubblicata.","class_deleted" to "Lezione eliminata.","no_active_students" to "Nessun allievo attivo trovato.",
        "email" to "E-mail","student_email" to "E-mail allievo","no_invoices" to "Nessuna fattura.","invoice_created" to "Fattura creata.","invoice_deleted" to "Fattura eliminata.",
        "notification_published" to "Notifica pubblicata.","notification_deleted" to "Notifica eliminata.","event_created" to "Evento creato.",
        "confirmed" to "CONFERMATA","declined" to "RIFIUTATA","requested" to "RICHIESTA","no_private_slots" to "Nessuna disponibilità per lezioni private.",
        "lesson_request_sent" to "Richiesta lezione privata inviata.","lesson_request_cancelled" to "Richiesta lezione privata annullata.","lesson_slot_created" to "Slot privato creato.","lesson_slot_deleted" to "Slot privato eliminato.","lesson_confirmed" to "Lezione privata confermata.","lesson_declined" to "Lezione privata rifiutata.",
        "homework_assigned" to "Compito assegnato.","coach_note_saved" to "Nota privata allenatore salvata.","assessment_saved" to "Valutazione salvata.",
        "challenge_assigned" to "Sfida assegnata.","challenge_deleted" to "Sfida eliminata.","fightcamp_saved" to "Fight Camp salvato.","syncing_badges" to "Sincronizzazione badge…",
        "content_saved" to "Contenuto allenamento salvato.","content_deleted" to "Contenuto allenamento eliminato.","plans_saved" to "Piani abbonamento salvati.","plan_updated" to "Piano aggiornato.",
        "uploading_photo" to "Caricamento foto profilo…","photo_updated" to "✓ Foto profilo aggiornata.","saving_profile" to "Salvataggio profilo…","profile_saved" to "✓ Profilo salvato nel cloud.","post_published" to "Post pubblicato.","post_deleted" to "Post eliminato.",
        "publishing_session" to "Pubblicazione sessione…","session_published" to "✓ Sessione pubblicata e attivata.","session_deleted" to "Sessione eliminata.","creating_qr" to "Creazione QR presenze sicuro…","qr_ready" to "✓ QR pronto. Scade automaticamente.","qr_valid" to "Valido circa 15 minuti","checkin_identity" to "Il check-in è collegato al tuo account allievo autenticato.","checking_in" to "Check-in in corso…",
        "academy_saved" to "Progresso Academy salvato.","academy_reset" to "Percorso Academy reimpostato.","downloading_media" to "Download media protetti…","uploading_media" to "Caricamento media protetti…","media_published" to "Media allenamento pubblicati.","media_deleted" to "Media allenamento eliminati.","student_feature_access" to "ACCESSO FUNZIONI ALLIEVO",
        "syncing" to "Sincronizzazione…","connected" to "Cloud collegato","saving" to "Salvataggio…","creating" to "Creazione…","deleting" to "Eliminazione…","please_wait" to "Attendi…","could_not_save" to "Impossibile salvare le modifiche."
    )
    val pl=en+mapOf(
        "level" to "Poziom","date" to "Data RRRR-MM-DD","time" to "Czas HH:mm","minutes" to "Minuty",
        "class_created" to "Zajęcia utworzone i opublikowane.","class_deleted" to "Zajęcia usunięte.","no_active_students" to "Nie znaleziono aktywnych uczniów.",
        "email" to "E-mail","student_email" to "E-mail ucznia","no_invoices" to "Brak faktur.","invoice_created" to "Faktura utworzona.","invoice_deleted" to "Faktura usunięta.",
        "notification_published" to "Powiadomienie opublikowane.","notification_deleted" to "Powiadomienie usunięte.","event_created" to "Wydarzenie utworzone.",
        "confirmed" to "POTWIERDZONO","declined" to "ODRZUCONO","requested" to "WYSŁANO","no_private_slots" to "Brak dostępnych lekcji prywatnych.",
        "lesson_request_sent" to "Prośba o lekcję prywatną wysłana.","lesson_request_cancelled" to "Prośba o lekcję prywatną anulowana.","lesson_slot_created" to "Termin prywatny utworzony.","lesson_slot_deleted" to "Termin prywatny usunięty.","lesson_confirmed" to "Lekcja prywatna potwierdzona.","lesson_declined" to "Lekcja prywatna odrzucona.",
        "homework_assigned" to "Zadanie przydzielone.","coach_note_saved" to "Prywatna notatka trenera zapisana.","assessment_saved" to "Ocena zapisana.",
        "challenge_assigned" to "Wyzwanie przydzielone.","challenge_deleted" to "Wyzwanie usunięte.","fightcamp_saved" to "Fight Camp zapisany.","syncing_badges" to "Synchronizacja odznak…",
        "content_saved" to "Treść treningowa zapisana.","content_deleted" to "Treść treningowa usunięta.","plans_saved" to "Plany członkostwa zapisane.","plan_updated" to "Plan zaktualizowany.",
        "uploading_photo" to "Przesyłanie zdjęcia profilu…","photo_updated" to "✓ Zdjęcie profilu zaktualizowane.","saving_profile" to "Zapisywanie profilu…","profile_saved" to "✓ Profil zapisany w chmurze.","post_published" to "Post opublikowany.","post_deleted" to "Post usunięty.",
        "publishing_session" to "Publikowanie sesji…","session_published" to "✓ Sesja opublikowana i aktywowana.","session_deleted" to "Sesja usunięta.","creating_qr" to "Tworzenie bezpiecznego QR obecności…","qr_ready" to "✓ QR gotowy. Wygasa automatycznie.","qr_valid" to "Ważny około 15 minut","checkin_identity" to "Check-in jest powiązany z zalogowanym kontem ucznia.","checking_in" to "Check-in…",
        "academy_saved" to "Postęp Akademii zapisany.","academy_reset" to "Ścieżka Akademii zresetowana.","downloading_media" to "Pobieranie chronionych mediów…","uploading_media" to "Przesyłanie chronionych mediów…","media_published" to "Media treningowe opublikowane.","media_deleted" to "Media treningowe usunięte.","student_feature_access" to "DOSTĘP DO FUNKCJI UCZNIA",
        "syncing" to "Synchronizacja…","connected" to "Chmura połączona","saving" to "Zapisywanie…","creating" to "Tworzenie…","deleting" to "Usuwanie…","please_wait" to "Poczekaj…","could_not_save" to "Nie udało się zapisać zmian."
    )
    val tr=en+mapOf(
        "level" to "Seviye","date" to "Tarih YYYY-AA-GG","time" to "Saat SS:dd","minutes" to "Dakika",
        "class_created" to "Ders oluşturuldu ve yayınlandı.","class_deleted" to "Ders silindi.","no_active_students" to "Aktif öğrenci bulunamadı.",
        "email" to "E-posta","student_email" to "Öğrenci e-postası","no_invoices" to "Henüz fatura yok.","invoice_created" to "Fatura oluşturuldu.","invoice_deleted" to "Fatura silindi.",
        "notification_published" to "Bildirim yayınlandı.","notification_deleted" to "Bildirim silindi.","event_created" to "Etkinlik oluşturuldu.",
        "confirmed" to "ONAYLANDI","declined" to "REDDEDİLDİ","requested" to "TALEP EDİLDİ","no_private_slots" to "Henüz özel ders müsaitliği yok.",
        "lesson_request_sent" to "Özel ders talebi gönderildi.","lesson_request_cancelled" to "Özel ders talebi iptal edildi.","lesson_slot_created" to "Özel ders saati oluşturuldu.","lesson_slot_deleted" to "Özel ders saati silindi.","lesson_confirmed" to "Özel ders onaylandı.","lesson_declined" to "Özel ders reddedildi.",
        "homework_assigned" to "Ödev atandı.","coach_note_saved" to "Özel antrenör notu kaydedildi.","assessment_saved" to "Değerlendirme kaydedildi.",
        "challenge_assigned" to "Görev atandı.","challenge_deleted" to "Görev silindi.","fightcamp_saved" to "Fight Camp kaydedildi.","syncing_badges" to "Rozetler eşitleniyor…",
        "content_saved" to "Antrenman içeriği kaydedildi.","content_deleted" to "Antrenman içeriği silindi.","plans_saved" to "Üyelik planları kaydedildi.","plan_updated" to "Plan güncellendi.",
        "uploading_photo" to "Profil fotoğrafı yükleniyor…","photo_updated" to "✓ Profil fotoğrafı güncellendi.","saving_profile" to "Profil kaydediliyor…","profile_saved" to "✓ Profil buluta kaydedildi.","post_published" to "Gönderi yayınlandı.","post_deleted" to "Gönderi silindi.",
        "publishing_session" to "Seans yayınlanıyor…","session_published" to "✓ Seans yayınlandı ve aktif edildi.","session_deleted" to "Seans silindi.","creating_qr" to "Güvenli katılım QR'ı oluşturuluyor…","qr_ready" to "✓ QR hazır. Otomatik olarak sona erer.","qr_valid" to "Yaklaşık 15 dakika geçerli","checkin_identity" to "Check-in, giriş yapmış öğrenci hesabına bağlıdır.","checking_in" to "Check-in yapılıyor…",
        "academy_saved" to "Akademi ilerlemesi kaydedildi.","academy_reset" to "Akademi yolu sıfırlandı.","downloading_media" to "Korumalı antrenman medyası indiriliyor…","uploading_media" to "Korumalı antrenman medyası yükleniyor…","media_published" to "Antrenman medyası yayınlandı.","media_deleted" to "Antrenman medyası silindi.","student_feature_access" to "ÖĞRENCİ ÖZELLİK ERİŞİMİ",
        "syncing" to "Eşitleniyor…","connected" to "Bulut bağlı","saving" to "Kaydediliyor…","creating" to "Oluşturuluyor…","deleting" to "Siliniyor…","please_wait" to "Lütfen bekle…","could_not_save" to "Değişiklikler kaydedilemedi."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

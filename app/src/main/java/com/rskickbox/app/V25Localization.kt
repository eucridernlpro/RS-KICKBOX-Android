package com.rskickbox.app

/**
 * v0.25 language pack.
 * Keeps UI copy centralized so layouts can be tested consistently in every supported language.
 */
fun rsT(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_dashboard" to "RS LIVE DASHBOARD",
        "trainer_dashboard" to "TRAINER CONTROL CENTER",
        "student" to "Student",
        "trainer_admin" to "Trainer / Admin",
        "premium_experience" to "Premium visual experience",
        "member_access" to "MEMBER ACCESS",
        "email" to "Email",
        "password" to "Password",
        "student_preview" to "Student preview",
        "trainer_preview" to "Trainer / Admin preview",
        "logout" to "Log out",
        "core_training" to "Core Training",
        "club_coaching" to "Club & Coaching",
        "performance" to "Performance",
        "library_account" to "Library, Music & Account",
        "brand_experience" to "Brand & Experience",
        "coaching_content_music" to "Coaching, Content & Music",
        "members_access" to "Members & Access",
        "club_operations" to "Club Operations",
        "business_release" to "Business & Release",
        "session_title" to "Session Player",
        "session_sub" to "Five-round training flow with 2:00 work and 0:45 rest phases.",
        "round" to "ROUND",
        "work" to "WORK",
        "rest" to "REST",
        "complete" to "COMPLETE",
        "pause" to "Pause",
        "start" to "Start",
        "next_round" to "Next round",
        "reset_session" to "Reset session"
    )
    val nl=mapOf(
        "student_dashboard" to "RS LIVE DASHBOARD",
        "trainer_dashboard" to "TRAINER CONTROLECENTRUM",
        "student" to "Leerling",
        "trainer_admin" to "Trainer / Beheerder",
        "premium_experience" to "Premium visuele ervaring",
        "member_access" to "LEDEN TOEGANG",
        "email" to "E-mail",
        "password" to "Wachtwoord",
        "student_preview" to "Leerling preview",
        "trainer_preview" to "Trainer / beheerder preview",
        "logout" to "Uitloggen",
        "core_training" to "Kerntraining",
        "club_coaching" to "Club & Coaching",
        "performance" to "Prestaties",
        "library_account" to "Bibliotheek, Muziek & Account",
        "brand_experience" to "Merk & Ervaring",
        "coaching_content_music" to "Coaching, Content & Muziek",
        "members_access" to "Leden & Toegang",
        "club_operations" to "Clubbeheer",
        "business_release" to "Zakelijk & Release",
        "session_title" to "Sessiespeler",
        "session_sub" to "Vijf rondes met 2:00 werk en 0:45 rust.",
        "round" to "RONDE","work" to "WERK","rest" to "RUST","complete" to "KLAAR",
        "pause" to "Pauze","start" to "Start","next_round" to "Volgende ronde","reset_session" to "Sessie resetten"
    )
    val pt=mapOf(
        "student_dashboard" to "PAINEL RS LIVE","trainer_dashboard" to "CENTRO DE CONTROLO DO TREINADOR",
        "student" to "Aluno","trainer_admin" to "Treinador / Admin","premium_experience" to "Experiência visual premium",
        "member_access" to "ACESSO DE MEMBRO","email" to "E-mail","password" to "Palavra-passe",
        "student_preview" to "Pré-visualização aluno","trainer_preview" to "Pré-visualização treinador / admin","logout" to "Sair",
        "core_training" to "Treino Principal","club_coaching" to "Clube & Coaching","performance" to "Desempenho",
        "library_account" to "Biblioteca, Música & Conta","brand_experience" to "Marca & Experiência",
        "coaching_content_music" to "Coaching, Conteúdo & Música","members_access" to "Membros & Acesso",
        "club_operations" to "Operações do Clube","business_release" to "Negócio & Lançamento",
        "session_title" to "Leitor de Sessão","session_sub" to "Cinco rondas com 2:00 de trabalho e 0:45 de descanso.",
        "round" to "RONDA","work" to "TRABALHO","rest" to "DESCANSO","complete" to "CONCLUÍDO",
        "pause" to "Pausa","start" to "Iniciar","next_round" to "Próxima ronda","reset_session" to "Repor sessão"
    )
    val es=mapOf(
        "student_dashboard" to "PANEL RS LIVE","trainer_dashboard" to "CENTRO DE CONTROL DEL ENTRENADOR",
        "student" to "Alumno","trainer_admin" to "Entrenador / Admin","premium_experience" to "Experiencia visual premium",
        "member_access" to "ACCESO DE MIEMBRO","email" to "Correo","password" to "Contraseña",
        "student_preview" to "Vista alumno","trainer_preview" to "Vista entrenador / admin","logout" to "Salir",
        "core_training" to "Entrenamiento Principal","club_coaching" to "Club & Coaching","performance" to "Rendimiento",
        "library_account" to "Biblioteca, Música & Cuenta","brand_experience" to "Marca & Experiencia",
        "coaching_content_music" to "Coaching, Contenido & Música","members_access" to "Miembros & Acceso",
        "club_operations" to "Operaciones del Club","business_release" to "Negocio & Lanzamiento",
        "session_title" to "Reproductor de Sesión","session_sub" to "Cinco rondas con 2:00 de trabajo y 0:45 de descanso.",
        "round" to "RONDA","work" to "TRABAJO","rest" to "DESCANSO","complete" to "COMPLETO",
        "pause" to "Pausa","start" to "Iniciar","next_round" to "Siguiente ronda","reset_session" to "Reiniciar sesión"
    )
    val fr=mapOf(
        "student_dashboard" to "TABLEAU RS LIVE","trainer_dashboard" to "CENTRE DE CONTRÔLE ENTRAÎNEUR",
        "student" to "Élève","trainer_admin" to "Entraîneur / Admin","premium_experience" to "Expérience visuelle premium",
        "member_access" to "ACCÈS MEMBRE","email" to "E-mail","password" to "Mot de passe",
        "student_preview" to "Aperçu élève","trainer_preview" to "Aperçu entraîneur / admin","logout" to "Déconnexion",
        "core_training" to "Entraînement Principal","club_coaching" to "Club & Coaching","performance" to "Performance",
        "library_account" to "Bibliothèque, Musique & Compte","brand_experience" to "Marque & Expérience",
        "coaching_content_music" to "Coaching, Contenu & Musique","members_access" to "Membres & Accès",
        "club_operations" to "Opérations du Club","business_release" to "Business & Publication",
        "session_title" to "Lecteur de Session","session_sub" to "Cinq rounds avec 2:00 d'effort et 0:45 de repos.",
        "round" to "ROUND","work" to "EFFORT","rest" to "REPOS","complete" to "TERMINÉ",
        "pause" to "Pause","start" to "Démarrer","next_round" to "Round suivant","reset_session" to "Réinitialiser"
    )
    val de=mapOf(
        "student_dashboard" to "RS LIVE DASHBOARD","trainer_dashboard" to "TRAINER-KONTROLLZENTRUM",
        "student" to "Schüler","trainer_admin" to "Trainer / Admin","premium_experience" to "Premium visuelles Erlebnis",
        "member_access" to "MITGLIEDERZUGANG","email" to "E-Mail","password" to "Passwort",
        "student_preview" to "Schüler-Vorschau","trainer_preview" to "Trainer-/Admin-Vorschau","logout" to "Abmelden",
        "core_training" to "Kerntraining","club_coaching" to "Club & Coaching","performance" to "Leistung",
        "library_account" to "Bibliothek, Musik & Konto","brand_experience" to "Marke & Erlebnis",
        "coaching_content_music" to "Coaching, Inhalte & Musik","members_access" to "Mitglieder & Zugriff",
        "club_operations" to "Club-Betrieb","business_release" to "Business & Veröffentlichung",
        "session_title" to "Session-Player","session_sub" to "Fünf Runden mit 2:00 Belastung und 0:45 Pause.",
        "round" to "RUNDE","work" to "ARBEIT","rest" to "PAUSE","complete" to "FERTIG",
        "pause" to "Pause","start" to "Start","next_round" to "Nächste Runde","reset_session" to "Session zurücksetzen"
    )
    val it=mapOf(
        "student_dashboard" to "DASHBOARD RS LIVE","trainer_dashboard" to "CENTRO CONTROLLO ALLENATORE",
        "student" to "Allievo","trainer_admin" to "Allenatore / Admin","premium_experience" to "Esperienza visiva premium",
        "member_access" to "ACCESSO MEMBRO","email" to "E-mail","password" to "Password",
        "student_preview" to "Anteprima allievo","trainer_preview" to "Anteprima allenatore / admin","logout" to "Esci",
        "core_training" to "Allenamento Base","club_coaching" to "Club & Coaching","performance" to "Prestazioni",
        "library_account" to "Libreria, Musica & Account","brand_experience" to "Brand & Esperienza",
        "coaching_content_music" to "Coaching, Contenuti & Musica","members_access" to "Membri & Accesso",
        "club_operations" to "Operazioni Club","business_release" to "Business & Pubblicazione",
        "session_title" to "Player Sessione","session_sub" to "Cinque round con 2:00 lavoro e 0:45 recupero.",
        "round" to "ROUND","work" to "LAVORO","rest" to "RECUPERO","complete" to "COMPLETO",
        "pause" to "Pausa","start" to "Avvia","next_round" to "Round successivo","reset_session" to "Reset sessione"
    )
    val pl=mapOf(
        "student_dashboard" to "PANEL RS LIVE","trainer_dashboard" to "CENTRUM STEROWANIA TRENERA",
        "student" to "Uczeń","trainer_admin" to "Trener / Admin","premium_experience" to "Ekskluzywne wrażenia wizualne",
        "member_access" to "DOSTĘP CZŁONKA","email" to "E-mail","password" to "Hasło",
        "student_preview" to "Podgląd ucznia","trainer_preview" to "Podgląd trenera / admina","logout" to "Wyloguj",
        "core_training" to "Trening Główny","club_coaching" to "Klub & Coaching","performance" to "Wyniki",
        "library_account" to "Biblioteka, Muzyka & Konto","brand_experience" to "Marka & Wrażenia",
        "coaching_content_music" to "Coaching, Treści & Muzyka","members_access" to "Członkowie & Dostęp",
        "club_operations" to "Operacje Klubu","business_release" to "Biznes & Publikacja",
        "session_title" to "Odtwarzacz Sesji","session_sub" to "Pięć rund: 2:00 pracy i 0:45 odpoczynku.",
        "round" to "RUNDA","work" to "PRACA","rest" to "ODPOCZYNEK","complete" to "GOTOWE",
        "pause" to "Pauza","start" to "Start","next_round" to "Następna runda","reset_session" to "Resetuj sesję"
    )
    val tr=mapOf(
        "student_dashboard" to "RS LIVE PANELİ","trainer_dashboard" to "ANTRENÖR KONTROL MERKEZİ",
        "student" to "Öğrenci","trainer_admin" to "Antrenör / Admin","premium_experience" to "Premium görsel deneyim",
        "member_access" to "ÜYE GİRİŞİ","email" to "E-posta","password" to "Şifre",
        "student_preview" to "Öğrenci önizleme","trainer_preview" to "Antrenör / admin önizleme","logout" to "Çıkış",
        "core_training" to "Ana Antrenman","club_coaching" to "Kulüp & Koçluk","performance" to "Performans",
        "library_account" to "Kütüphane, Müzik & Hesap","brand_experience" to "Marka & Deneyim",
        "coaching_content_music" to "Koçluk, İçerik & Müzik","members_access" to "Üyeler & Erişim",
        "club_operations" to "Kulüp Operasyonları","business_release" to "İşletme & Yayın",
        "session_title" to "Seans Oynatıcı","session_sub" to "2:00 çalışma ve 0:45 dinlenme ile beş raund.",
        "round" to "RAUND","work" to "ÇALIŞMA","rest" to "DİNLENME","complete" to "TAMAMLANDI",
        "pause" to "Duraklat","start" to "Başlat","next_round" to "Sonraki raund","reset_session" to "Seansı sıfırla"
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

private val routeTitlesV25=mapOf(
    "voice" to listOf("AI Voice Coach","AI Stemcoach","Coach de Voz IA","Coach de Voz IA","Coach Vocal IA","KI-Sprachcoach","Coach Vocale IA","Trener Głosowy AI","AI Sesli Koç"),
    "session" to listOf("Session Player","Sessiespeler","Leitor de Sessão","Reproductor de Sesión","Lecteur de Session","Session-Player","Player Sessione","Odtwarzacz Sesji","Seans Oynatıcı"),
    "academy" to listOf("RS Academy","RS Academy","Academia RS","Academia RS","Académie RS","RS Akademie","Accademia RS","Akademia RS","RS Akademi"),
    "techniques" to listOf("Technique Library","Techniekbibliotheek","Biblioteca de Técnicas","Biblioteca de Técnicas","Bibliothèque Technique","Technik-Bibliothek","Libreria Tecniche","Biblioteka Technik","Teknik Kütüphanesi"),
    "home_training" to listOf("Home Training","Thuis Training","Treino em Casa","Entrenamiento en Casa","Entraînement Maison","Heimtraining","Allenamento a Casa","Trening Domowy","Ev Antrenmanı"),
    "workout" to listOf("Workout Generator","Workout Generator","Gerador de Treino","Generador de Entreno","Générateur d'Entraînement","Workout-Generator","Generatore Allenamento","Generator Treningu","Antrenman Oluşturucu"),
    "classes" to listOf("Classes & Events","Lessen & Events","Aulas & Eventos","Clases & Eventos","Cours & Événements","Kurse & Events","Corsi & Eventi","Zajęcia & Wydarzenia","Dersler & Etkinlikler"),
    "events" to listOf("RS Events","RS Events","Eventos RS","Eventos RS","Événements RS","RS Events","Eventi RS","Wydarzenia RS","RS Etkinlikleri"),
    "coachchat" to listOf("Private Coach Chat","Privé Coach Chat","Chat Privado Coach","Chat Privado Coach","Chat Coach Privé","Privater Coach-Chat","Chat Coach Privato","Prywatny Chat Trenera","Özel Koç Sohbeti"),
    "community" to listOf("Community","Community","Comunidade","Comunidad","Communauté","Community","Community","Społeczność","Topluluk"),
    "groups" to listOf("Groups","Groepen","Grupos","Grupos","Groupes","Gruppen","Gruppi","Grupy","Gruplar"),
    "private_lessons" to listOf("Private Lessons","Privélessen","Aulas Privadas","Clases Privadas","Cours Privés","Privatstunden","Lezioni Private","Lekcje Prywatne","Özel Dersler"),
    "progress" to listOf("Progress","Voortgang","Progresso","Progreso","Progression","Fortschritt","Progressi","Postępy","İlerleme"),
    "challenges" to listOf("Challenges","Uitdagingen","Desafios","Desafíos","Défis","Challenges","Sfide","Wyzwania","Meydan Okumalar"),
    "badges" to listOf("Badges","Badges","Emblemas","Insignias","Badges","Abzeichen","Badge","Odznaki","Rozetler"),
    "fightcamp" to listOf("Fight Camp","Fight Camp","Fight Camp","Fight Camp","Fight Camp","Fight Camp","Fight Camp","Fight Camp","Fight Camp"),
    "compare" to listOf("Technique Compare","Techniek Vergelijken","Comparar Técnicas","Comparar Técnicas","Comparer Techniques","Techniken Vergleichen","Confronta Tecniche","Porównaj Techniki","Teknik Karşılaştır"),
    "history" to listOf("Training History","Trainingsgeschiedenis","Histórico de Treino","Historial de Entreno","Historique d'Entraînement","Trainingsverlauf","Storico Allenamenti","Historia Treningów","Antrenman Geçmişi"),
    "vault" to listOf("Knowledge Vault","Kennisbank","Cofre de Conhecimento","Biblioteca Premium","Coffre de Connaissances","Wissensarchiv","Archivio Conoscenza","Baza Wiedzy","Bilgi Kasası"),
    "homework" to listOf("Homework","Huiswerk","Tarefas","Tareas","Devoirs","Hausaufgaben","Compiti","Zadania","Ödev"),
    "favorites" to listOf("Saved & Favorites","Opgeslagen & Favorieten","Guardados & Favoritos","Guardados & Favoritos","Enregistrés & Favoris","Gespeichert & Favoriten","Salvati & Preferiti","Zapisane & Ulubione","Kaydedilenler & Favoriler"),
    "media" to listOf("Training Media","Trainingsmedia","Media de Treino","Media de Entreno","Médias d'Entraînement","Trainingsmedien","Media Allenamento","Media Treningowe","Antrenman Medyası"),
    "music" to listOf("My RS Music","Mijn RS Muziek","Minha Música RS","Mi Música RS","Ma Musique RS","Meine RS Musik","La Mia Musica RS","Moja Muzyka RS","RS Müziğim"),
    "finance" to listOf("Membership & Payments","Lidmaatschap & Betalingen","Adesão & Pagamentos","Membresía & Pagos","Adhésion & Paiements","Mitgliedschaft & Zahlungen","Abbonamento & Pagamenti","Członkostwo & Płatności","Üyelik & Ödemeler"),
    "book" to listOf("Trainer Book","Trainerboek","Livro do Treinador","Libro del Entrenador","Livre de l'Entraîneur","Trainerbuch","Libro Allenatore","Książka Trenera","Antrenör Kitabı"),
    "profile" to listOf("My Profile","Mijn Profiel","Meu Perfil","Mi Perfil","Mon Profil","Mein Profil","Il Mio Profilo","Mój Profil","Profilim"),
    "search" to listOf("Search","Zoeken","Pesquisar","Buscar","Rechercher","Suche","Cerca","Szukaj","Ara"),
    "settings" to listOf("Settings & Privacy","Instellingen & Privacy","Definições & Privacidade","Ajustes & Privacidad","Réglages & Confidentialité","Einstellungen & Datenschutz","Impostazioni & Privacy","Ustawienia & Prywatność","Ayarlar & Gizlilik"),
    "themes" to listOf("Visual Theme Studio","Visuele Thema Studio","Estúdio de Temas","Estudio de Temas","Studio de Thèmes","Design-Themenstudio","Studio Temi","Studio Motywów","Görsel Tema Stüdyosu"),
    "backgrounds" to listOf("Visual Asset Studio","Visuele Asset Studio","Estúdio Visual","Estudio Visual","Studio Visuel","Visuelles Asset-Studio","Studio Visuale","Studio Wizualne","Görsel Varlık Stüdyosu"),
    "branding" to listOf("Branding & Site Settings","Branding & Site-instellingen","Marca & Definições","Marca & Ajustes","Marque & Réglages","Branding & Seiteneinstellungen","Branding & Impostazioni","Marka & Ustawienia","Marka & Site Ayarları"),
    "intro_settings" to listOf("Intro & Splash","Intro & Splash","Intro & Splash","Intro & Splash","Intro & Splash","Intro & Splash","Intro & Splash","Intro & Splash","Intro & Splash"),
    "members" to listOf("Student Manager","Leerlingbeheer","Gestor de Alunos","Gestor de Alumnos","Gestion Élèves","Schülerverwaltung","Gestione Allievi","Menedżer Uczniów","Öğrenci Yönetimi"),
    "access" to listOf("Access & Subscriptions","Toegang & Abonnementen","Acesso & Subscrições","Acceso & Suscripciones","Accès & Abonnements","Zugriff & Abos","Accesso & Abbonamenti","Dostęp & Subskrypcje","Erişim & Abonelikler"),
    "payments" to listOf("Payment Center","Betalingscentrum","Centro de Pagamentos","Centro de Pagos","Centre de Paiement","Zahlungscenter","Centro Pagamenti","Centrum Płatności","Ödeme Merkezi"),
    "invoices" to listOf("Invoices","Facturen","Faturas","Facturas","Factures","Rechnungen","Fatture","Faktury","Faturalar"),
    "analytics" to listOf("Analytics","Analytics","Análises","Analíticas","Analytique","Analysen","Analisi","Analityka","Analiz"),
    "notifications" to listOf("Notifications","Meldingen","Notificações","Notificaciones","Notifications","Benachrichtigungen","Notifiche","Powiadomienia","Bildirimler"),
    "support" to listOf("Support & Final QC","Support & Eind-QC","Suporte & QC Final","Soporte & QC Final","Support & QC Final","Support & Final-QC","Supporto & QC Finale","Wsparcie & QC","Destek & Son QC"),
    "release" to listOf("Release & Legal Center","Release & Juridisch","Lançamento & Legal","Lanzamiento & Legal","Publication & Juridique","Release & Recht","Release & Legale","Publikacja & Prawo","Yayın & Hukuk"),
    "landing_admin" to listOf("Promotion Manager","Promotiebeheer","Gestor de Promoção","Gestor de Promoción","Gestion Promotion","Promo-Manager","Gestione Promozioni","Menedżer Promocji","Promosyon Yönetimi"),
    "lesson_editor" to listOf("Lesson Editor","Leseditor","Editor de Aulas","Editor de Clases","Éditeur de Cours","Lektionseditor","Editor Lezioni","Edytor Lekcji","Ders Editörü"),
    "content" to listOf("Content Manager","Contentbeheer","Gestor de Conteúdo","Gestor de Contenido","Gestion Contenu","Content-Manager","Gestione Contenuti","Menedżer Treści","İçerik Yönetimi"),
    "homework_admin" to listOf("Homework Manager","Huiswerkbeheer","Gestor de Tarefas","Gestor de Tareas","Gestion Devoirs","Hausaufgaben-Manager","Gestione Compiti","Menedżer Zadań","Ödev Yönetimi"),
    "session_builder" to listOf("Session Builder","Sessiebouwer","Criador de Sessões","Creador de Sesiones","Créateur de Sessions","Session-Builder","Crea Sessioni","Kreator Sesji","Seans Oluşturucu"),
    "music_admin" to listOf("RS Music Manager","RS Muziekbeheer","Gestor de Música RS","Gestor de Música RS","Gestion Musique RS","RS Musik-Manager","Gestione Musica RS","Menedżer Muzyki RS","RS Müzik Yönetimi"),
    "notes" to listOf("Coach Notes","Coachnotities","Notas do Coach","Notas del Coach","Notes Coach","Coach-Notizen","Note Coach","Notatki Trenera","Koç Notları"),
    "plans_admin" to listOf("Membership Plans","Lidmaatschapsplannen","Planos de Adesão","Planes de Membresía","Formules d'Adhésion","Mitgliedschaftspläne","Piani Abbonamento","Plany Członkostwa","Üyelik Planları"),
    "progress_admin" to listOf("Progress Manager","Voortgangsbeheer","Gestor de Progresso","Gestor de Progreso","Gestion Progression","Fortschrittsmanager","Gestione Progressi","Menedżer Postępów","İlerleme Yönetimi"),
    "assessments" to listOf("Coach Assessments","Coachbeoordelingen","Avaliações do Coach","Evaluaciones del Coach","Évaluations Coach","Coach-Bewertungen","Valutazioni Coach","Oceny Trenera","Koç Değerlendirmeleri"),
    "challenge_admin" to listOf("Challenge Manager","Uitdagingenbeheer","Gestor de Desafios","Gestor de Desafíos","Gestion Défis","Challenge-Manager","Gestione Sfide","Menedżer Wyzwań","Meydan Okuma Yönetimi"),
    "fightcamp_admin" to listOf("Fight Camp Manager","Fight Camp Beheer","Gestor Fight Camp","Gestor Fight Camp","Gestion Fight Camp","Fight-Camp-Manager","Gestione Fight Camp","Menedżer Fight Camp","Fight Camp Yönetimi"),
    "attendance" to listOf("Attendance","Aanwezigheid","Presenças","Asistencia","Présences","Anwesenheit","Presenze","Obecność","Yoklama"),
    "qr_attendance" to listOf("QR Attendance","QR Aanwezigheid","Presenças QR","Asistencia QR","Présence QR","QR-Anwesenheit","Presenze QR","Obecność QR","QR Yoklama"),
    "events_admin" to listOf("Event Manager","Eventbeheer","Gestor de Eventos","Gestor de Eventos","Gestion Événements","Event-Manager","Gestione Eventi","Menedżer Wydarzeń","Etkinlik Yönetimi"),
    "schedule" to listOf("Trainer Schedule","Trainerplanning","Agenda do Treinador","Agenda del Entrenador","Planning Entraîneur","Trainerplan","Agenda Allenatore","Plan Trenera","Antrenör Programı"),
    "documents" to listOf("Documents & Waivers","Documenten & Verklaringen","Documentos & Termos","Documentos & Renuncias","Documents & Décharges","Dokumente & Verzicht","Documenti & Liberatorie","Dokumenty & Zgody","Belgeler & Feragatler"),
    "referrals" to listOf("Referrals","Doorverwijzingen","Referências","Referidos","Parrainages","Empfehlungen","Referral","Polecenia","Referanslar")
)

private val routeHintsV25=mapOf(
    "voice" to listOf("Ask · listen · improve","Vraag · luister · verbeter","Pergunta · ouve · melhora","Pregunta · escucha · mejora","Demande · écoute · progresse","Fragen · hören · verbessern","Chiedi · ascolta · migliora","Pytaj · słuchaj · poprawiaj","Sor · dinle · geliş"),
    "session" to listOf("Rounds · timer · cues","Rondes · timer · cues","Rondas · tempo · sinais","Rounds · tiempo · señales","Rounds · chrono · repères","Runden · Timer · Signale","Round · timer · segnali","Rundy · czas · komendy","Raund · süre · komutlar"),
    "academy" to listOf("Structured premium lessons","Premium lessen","Lições premium estruturadas","Lecciones premium","Cours premium structurés","Strukturierte Premium-Lektionen","Lezioni premium strutturate","Lekcje premium","Yapılandırılmış premium dersler"),
    "music" to listOf("Local playlists · live player","Lokale playlists · live player","Playlists locais · leitor","Listas locales · reproductor","Playlists locales · lecteur","Lokale Playlists · Player","Playlist locali · player","Lokalne playlisty · odtwarzacz","Yerel listeler · oynatıcı"),
    "finance" to listOf("Plan · payments · history","Plan · betalingen · historie","Plano · pagamentos · histórico","Plan · pagos · historial","Forfait · paiements · historique","Plan · Zahlungen · Verlauf","Piano · pagamenti · storico","Plan · płatności · historia","Plan · ödemeler · geçmiş"),
    "settings" to listOf("Preferences · account","Voorkeuren · account","Preferências · conta","Preferencias · cuenta","Préférences · compte","Einstellungen · Konto","Preferenze · account","Preferencje · konto","Tercihler · hesap")
)

private fun langIndexV25(lang:RsLang)=when(lang.code){"nl"->1;"pt"->2;"es"->3;"fr"->4;"de"->5;"it"->6;"pl"->7;"tr"->8;else->0}
fun rsRouteTitle(lang:RsLang,route:String,fallback:String):String=routeTitlesV25[route]?.getOrNull(langIndexV25(lang))?:fallback
fun rsRouteHint(lang:RsLang,route:String,fallback:String):String=routeHintsV25[route]?.getOrNull(langIndexV25(lang))?:fallback


fun rsRestInstruction(lang:RsLang)=when(lang.code){
    "nl"->"Adem · herstel je houding · blijf rustig"
    "pt"->"Respira · recupera a guarda · mantém o controlo"
    "es"->"Respira · recupera la guardia · mantén el control"
    "fr"->"Respire · replace ta garde · reste calme"
    "de"->"Atmen · Stellung resetten · ruhig bleiben"
    "it"->"Respira · ripristina la guardia · resta composto"
    "pl"->"Oddychaj · ustaw gardę · zachowaj spokój"
    "tr"->"Nefes al · gardını yenile · sakin kal"
    else->"Breathe · reset stance · stay composed"
}
fun rsWorkInstruction(lang:RsLang)=when(lang.code){
    "nl"->"Jab · Cross · Low Kick"
    "pt"->"Jab · Direto · Low Kick"
    "es"->"Jab · Directo · Low Kick"
    "fr"->"Jab · Direct · Low Kick"
    "de"->"Jab · Cross · Low Kick"
    "it"->"Jab · Diretto · Low Kick"
    "pl"->"Jab · Cross · Low Kick"
    "tr"->"Jab · Cross · Low Kick"
    else->"Jab · Cross · Low Kick"
}

fun adaptiveLabelSp(text:String,base:Float=10f):Float=when{
    text.length>=24->(base-2f).coerceAtLeast(7f)
    text.length>=16->(base-1f).coerceAtLeast(8f)
    else->base
}


fun rsMusicT(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "My RS Music","trainer_title" to "RS Music Manager",
        "subtitle" to "Device music, internal playlists and a cinematic training player",
        "add_title" to "ADD YOUR OWN MUSIC","add_desc" to "Choose audio files supported by Android from your device or document library.",
        "add_button" to "＋ Add music from device / library","player_sub" to "Cinematic training player",
        "pause" to "Pause","play" to "Play","playlist" to "MY RS PLAYLIST","local_audio" to "Local device audio",
        "remove" to "Remove","youtube" to "YOUTUBE PLAYLIST SHORTCUT","youtube_link" to "YouTube playlist link",
        "save_youtube" to "Save YouTube playlist shortcut","open_youtube" to "Open saved playlist in YouTube",
        "trainer_control" to "TRAINER MUSIC CONTROL","player_ready" to "PLAYER READY",
        "player_ready_desc" to "The premium RS audio player appears automatically as soon as the first music file is added."
    )
    val packs=mapOf(
        "nl" to mapOf("student_title" to "Mijn RS Muziek","trainer_title" to "RS Muziekbeheer","subtitle" to "Muziek van je apparaat, interne playlists en een cinematografische trainingsspeler","add_title" to "VOEG JE EIGEN MUZIEK TOE","add_desc" to "Kies audiobestanden van je apparaat of documentenbibliotheek.","add_button" to "＋ Muziek toevoegen van apparaat","player_sub" to "Cinematografische trainingsspeler","pause" to "Pauze","play" to "Afspelen","playlist" to "MIJN RS PLAYLIST","local_audio" to "Lokale audio","remove" to "Verwijderen","youtube" to "YOUTUBE PLAYLIST SNELKOPPELING","youtube_link" to "YouTube playlist-link","save_youtube" to "YouTube playlist opslaan","open_youtube" to "Open playlist in YouTube","trainer_control" to "TRAINER MUZIEKBEHEER","player_ready" to "SPELER KLAAR","player_ready_desc" to "De premium RS-audiospeler verschijnt zodra het eerste muziekbestand is toegevoegd."),
        "pt" to mapOf("student_title" to "Minha Música RS","trainer_title" to "Gestor de Música RS","subtitle" to "Música do dispositivo, playlists internas e leitor de treino cinematográfico","add_title" to "ADICIONA A TUA MÚSICA","add_desc" to "Escolhe ficheiros de áudio do dispositivo ou biblioteca de documentos.","add_button" to "＋ Adicionar música do dispositivo","player_sub" to "Leitor de treino cinematográfico","pause" to "Pausa","play" to "Tocar","playlist" to "MINHA PLAYLIST RS","local_audio" to "Áudio local","remove" to "Remover","youtube" to "ATALHO PLAYLIST YOUTUBE","youtube_link" to "Link da playlist YouTube","save_youtube" to "Guardar playlist YouTube","open_youtube" to "Abrir playlist no YouTube","trainer_control" to "CONTROLO DE MÚSICA DO TREINADOR","player_ready" to "LEITOR PRONTO","player_ready_desc" to "O leitor premium RS aparece assim que adicionares o primeiro ficheiro de música."),
        "es" to mapOf("student_title" to "Mi Música RS","trainer_title" to "Gestor de Música RS","subtitle" to "Música del dispositivo, playlists internas y reproductor de entrenamiento","add_title" to "AÑADE TU PROPIA MÚSICA","add_desc" to "Elige archivos de audio del dispositivo o biblioteca de documentos.","add_button" to "＋ Añadir música del dispositivo","player_sub" to "Reproductor de entrenamiento cinematográfico","pause" to "Pausa","play" to "Reproducir","playlist" to "MI PLAYLIST RS","local_audio" to "Audio local","remove" to "Eliminar","youtube" to "ACCESO PLAYLIST YOUTUBE","youtube_link" to "Enlace de playlist YouTube","save_youtube" to "Guardar playlist YouTube","open_youtube" to "Abrir playlist en YouTube","trainer_control" to "CONTROL DE MÚSICA DEL ENTRENADOR","player_ready" to "REPRODUCTOR LISTO","player_ready_desc" to "El reproductor premium RS aparece al añadir el primer archivo de música."),
        "fr" to mapOf("student_title" to "Ma Musique RS","trainer_title" to "Gestion Musique RS","subtitle" to "Musique locale, playlists internes et lecteur d'entraînement cinématique","add_title" to "AJOUTE TA MUSIQUE","add_desc" to "Choisis des fichiers audio de l'appareil ou de la bibliothèque de documents.","add_button" to "＋ Ajouter musique de l'appareil","player_sub" to "Lecteur d'entraînement cinématique","pause" to "Pause","play" to "Lecture","playlist" to "MA PLAYLIST RS","local_audio" to "Audio local","remove" to "Supprimer","youtube" to "RACCOURCI PLAYLIST YOUTUBE","youtube_link" to "Lien playlist YouTube","save_youtube" to "Enregistrer playlist YouTube","open_youtube" to "Ouvrir playlist dans YouTube","trainer_control" to "CONTRÔLE MUSIQUE ENTRAÎNEUR","player_ready" to "LECTEUR PRÊT","player_ready_desc" to "Le lecteur audio premium RS apparaît dès l'ajout du premier fichier musical."),
        "de" to mapOf("student_title" to "Meine RS Musik","trainer_title" to "RS Musik-Manager","subtitle" to "Gerätemusik, interne Playlists und cinematischer Trainingsplayer","add_title" to "EIGENE MUSIK HINZUFÜGEN","add_desc" to "Wähle Audiodateien vom Gerät oder aus der Dokumentbibliothek.","add_button" to "＋ Musik vom Gerät hinzufügen","player_sub" to "Cinematischer Trainingsplayer","pause" to "Pause","play" to "Abspielen","playlist" to "MEINE RS PLAYLIST","local_audio" to "Lokales Audio","remove" to "Entfernen","youtube" to "YOUTUBE-PLAYLIST-LINK","youtube_link" to "YouTube Playlist-Link","save_youtube" to "YouTube Playlist speichern","open_youtube" to "Playlist in YouTube öffnen","trainer_control" to "TRAINER-MUSIKSTEUERUNG","player_ready" to "PLAYER BEREIT","player_ready_desc" to "Der Premium-RS-Audioplayer erscheint nach dem ersten hinzugefügten Musikstück."),
        "it" to mapOf("student_title" to "La Mia Musica RS","trainer_title" to "Gestione Musica RS","subtitle" to "Musica locale, playlist interne e player di allenamento cinematografico","add_title" to "AGGIUNGI LA TUA MUSICA","add_desc" to "Scegli file audio dal dispositivo o dalla libreria documenti.","add_button" to "＋ Aggiungi musica dal dispositivo","player_sub" to "Player di allenamento cinematografico","pause" to "Pausa","play" to "Riproduci","playlist" to "LA MIA PLAYLIST RS","local_audio" to "Audio locale","remove" to "Rimuovi","youtube" to "COLLEGAMENTO PLAYLIST YOUTUBE","youtube_link" to "Link playlist YouTube","save_youtube" to "Salva playlist YouTube","open_youtube" to "Apri playlist in YouTube","trainer_control" to "CONTROLLO MUSICA ALLENATORE","player_ready" to "PLAYER PRONTO","player_ready_desc" to "Il player audio premium RS appare dopo l'aggiunta del primo file musicale."),
        "pl" to mapOf("student_title" to "Moja Muzyka RS","trainer_title" to "Menedżer Muzyki RS","subtitle" to "Muzyka z urządzenia, playlisty i kinowy odtwarzacz treningowy","add_title" to "DODAJ WŁASNĄ MUZYKĘ","add_desc" to "Wybierz pliki audio z urządzenia lub biblioteki dokumentów.","add_button" to "＋ Dodaj muzykę z urządzenia","player_sub" to "Kinowy odtwarzacz treningowy","pause" to "Pauza","play" to "Odtwórz","playlist" to "MOJA PLAYLISTA RS","local_audio" to "Lokalne audio","remove" to "Usuń","youtube" to "SKRÓT PLAYLISTY YOUTUBE","youtube_link" to "Link playlisty YouTube","save_youtube" to "Zapisz playlistę YouTube","open_youtube" to "Otwórz playlistę w YouTube","trainer_control" to "STEROWANIE MUZYKĄ TRENERA","player_ready" to "ODTWARZACZ GOTOWY","player_ready_desc" to "Odtwarzacz premium RS pojawia się po dodaniu pierwszego pliku muzycznego."),
        "tr" to mapOf("student_title" to "RS Müziğim","trainer_title" to "RS Müzik Yönetimi","subtitle" to "Cihaz müziği, dahili listeler ve sinematik antrenman oynatıcısı","add_title" to "KENDİ MÜZİĞİNİ EKLE","add_desc" to "Cihazdan veya belge kitaplığından ses dosyaları seç.","add_button" to "＋ Cihazdan müzik ekle","player_sub" to "Sinematik antrenman oynatıcısı","pause" to "Duraklat","play" to "Oynat","playlist" to "RS ÇALMA LİSTEM","local_audio" to "Yerel ses","remove" to "Kaldır","youtube" to "YOUTUBE LİSTE KISAYOLU","youtube_link" to "YouTube liste bağlantısı","save_youtube" to "YouTube listesini kaydet","open_youtube" to "Listeyi YouTube'da aç","trainer_control" to "ANTRENÖR MÜZİK KONTROLÜ","player_ready" to "OYNATICI HAZIR","player_ready_desc" to "İlk müzik dosyası eklenince premium RS ses oynatıcısı otomatik görünür.")
    )
    return packs[lang.code]?.get(key)?:en[key]?:key
}

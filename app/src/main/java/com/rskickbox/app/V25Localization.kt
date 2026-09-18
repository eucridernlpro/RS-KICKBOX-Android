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

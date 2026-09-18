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
    "home" to listOf("RS Live Dashboard","RS Live Dashboard","Painel RS Live","Panel RS Live","Tableau RS Live","RS Live Dashboard","Dashboard RS Live","Panel RS Live","RS Live Paneli"),
    "trainer" to listOf("Trainer Dashboard","Trainer Dashboard","Painel do Treinador","Panel del Entrenador","Tableau Entraîneur","Trainer-Dashboard","Dashboard Allenatore","Panel Trenera","Antrenör Paneli"),
    "voice" to listOf("AI Technique Coach","AI Techniekcoach","Coach Técnico IA","Coach Técnico IA","Coach Technique IA","KI-Technikcoach","Coach Tecnico IA","Trener Techniki AI","AI Teknik Koçu"),
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
    "promotions" to listOf("Promotions","Promoties","Promoções","Promociones","Promotions","Aktionen","Promozioni","Promocje","Promosyonlar"),
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


/** v0.34 private-enrollment copy for all supported app languages. */
fun rsEnrollmentT(lang:RsLang,key:String):String{
    val en=mapOf(
        "invite_title" to "Student invitation QR",
        "invite_desc" to "Scan the QR from your trainer or upload the QR image from your gallery. Your email and activation code will be filled automatically.",
        "scan_qr" to "Scan QR",
        "upload_qr" to "Upload QR",
        "email" to "Email",
        "activation_code" to "Password / activation code",
        "student_login" to "Student login",
        "trainer_preview" to "Trainer / Admin preview",
        "build_label" to "v0.50 PRIVATE ENROLLMENT QC BUILD",
        "student_manager" to "Student Manager",
        "student_manager_sub" to "Create private RS KICKBOX student accounts, generate invitation QR codes and manage access.",
        "student_capacity" to "STUDENT CAPACITY",
        "active_accounts" to "active student accounts",
        "create_student" to "+ Create new student account",
        "close_new" to "Close new account",
        "new_student" to "NEW STUDENT ACCOUNT",
        "student_name" to "Student name",
        "plan" to "Plan",
        "create_qr" to "Create account + QR",
        "show_qr" to "Show invitation QR",
        "hide_qr" to "Hide invitation QR",
        "share_invite" to "Share QR + app link",
        "share_store" to "Share Play Store link only",
        "new_qr" to "New QR",
        "delete" to "Delete",
        "active" to "ACTIVE",
        "inactive" to "INACTIVE",
        "security_note" to "PRODUCTION SECURITY NOTE","search_students" to "Search students","shown" to "shown","total" to "total","show_more" to "Show more","confirm" to "Confirm"
    )
    val nl=mapOf(
        "invite_title" to "QR-uitnodiging leerling","invite_desc" to "Scan de QR van je trainer of upload de QR-afbeelding uit je galerij. Je e-mail en activatiecode worden automatisch ingevuld.","scan_qr" to "QR scannen","upload_qr" to "QR uploaden","email" to "E-mail","activation_code" to "Wachtwoord / activatiecode","student_login" to "Leerling inloggen","trainer_preview" to "Trainer / beheerder preview","build_label" to "v0.50 PRIVÉ INSCHRIJVING QC BUILD","student_manager" to "Leerlingbeheer","student_manager_sub" to "Maak privé RS KICKBOX-accounts, genereer QR-uitnodigingen en beheer toegang.","student_capacity" to "LEERLINGCAPACITEIT","active_accounts" to "actieve leerlingaccounts","create_student" to "+ Nieuw leerlingaccount","close_new" to "Nieuw account sluiten","new_student" to "NIEUW LEERLINGACCOUNT","student_name" to "Naam leerling","plan" to "Plan","create_qr" to "Account + QR maken","show_qr" to "QR-uitnodiging tonen","hide_qr" to "QR-uitnodiging verbergen","share_invite" to "QR + app-link delen","share_store" to "Alleen Play Store-link delen","new_qr" to "Nieuwe QR","delete" to "Verwijderen","active" to "ACTIEF","inactive" to "INACTIEF","security_note" to "PRODUCTIEBEVEILIGING","search_students" to "Leerlingen zoeken","shown" to "getoond","total" to "totaal","show_more" to "Meer tonen","confirm" to "Bevestigen"
    )
    val pt=mapOf(
        "invite_title" to "QR de convite do aluno","invite_desc" to "Lê o QR do treinador ou carrega a imagem QR da galeria. O e-mail e o código de ativação serão preenchidos automaticamente.","scan_qr" to "Ler QR","upload_qr" to "Carregar QR","email" to "E-mail","activation_code" to "Palavra-passe / código de ativação","student_login" to "Entrar como aluno","trainer_preview" to "Pré-visualização treinador / admin","build_label" to "v0.50 INSCRIÇÃO PRIVADA QC BUILD","student_manager" to "Gestor de Alunos","student_manager_sub" to "Cria contas privadas RS KICKBOX, gera convites QR e gere o acesso.","student_capacity" to "CAPACIDADE DE ALUNOS","active_accounts" to "contas de alunos ativas","create_student" to "+ Criar nova conta de aluno","close_new" to "Fechar nova conta","new_student" to "NOVA CONTA DE ALUNO","student_name" to "Nome do aluno","plan" to "Plano","create_qr" to "Criar conta + QR","show_qr" to "Mostrar QR do convite","hide_qr" to "Ocultar QR do convite","share_invite" to "Partilhar QR + link da app","share_store" to "Partilhar só link Play Store","new_qr" to "Novo QR","delete" to "Eliminar","active" to "ATIVO","inactive" to "INATIVO","security_note" to "NOTA DE SEGURANÇA DE PRODUÇÃO","search_students" to "Pesquisar alunos","shown" to "mostrados","total" to "total","show_more" to "Mostrar mais","confirm" to "Confirmar"
    )
    val es=mapOf(
        "invite_title" to "QR de invitación del alumno","invite_desc" to "Escanea el QR de tu entrenador o sube la imagen QR desde la galería. El correo y el código de activación se completarán automáticamente.","scan_qr" to "Escanear QR","upload_qr" to "Subir QR","email" to "Correo","activation_code" to "Contraseña / código de activación","student_login" to "Acceso alumno","trainer_preview" to "Vista entrenador / admin","build_label" to "v0.50 REGISTRO PRIVADO QC BUILD","student_manager" to "Gestor de Alumnos","student_manager_sub" to "Crea cuentas privadas RS KICKBOX, genera invitaciones QR y gestiona el acceso.","student_capacity" to "CAPACIDAD DE ALUMNOS","active_accounts" to "cuentas de alumnos activas","create_student" to "+ Crear nueva cuenta","close_new" to "Cerrar nueva cuenta","new_student" to "NUEVA CUENTA DE ALUMNO","student_name" to "Nombre del alumno","plan" to "Plan","create_qr" to "Crear cuenta + QR","show_qr" to "Mostrar QR de invitación","hide_qr" to "Ocultar QR de invitación","share_invite" to "Compartir QR + enlace app","share_store" to "Compartir solo Play Store","new_qr" to "Nuevo QR","delete" to "Eliminar","active" to "ACTIVO","inactive" to "INACTIVO","security_note" to "NOTA DE SEGURIDAD DE PRODUCCIÓN","search_students" to "Buscar alumnos","shown" to "mostrados","total" to "total","show_more" to "Mostrar más","confirm" to "Confirmar"
    )
    val fr=mapOf(
        "invite_title" to "QR d'invitation élève","invite_desc" to "Scanne le QR de ton entraîneur ou importe l'image QR depuis la galerie. L'e-mail et le code d'activation seront remplis automatiquement.","scan_qr" to "Scanner QR","upload_qr" to "Importer QR","email" to "E-mail","activation_code" to "Mot de passe / code d'activation","student_login" to "Connexion élève","trainer_preview" to "Aperçu entraîneur / admin","build_label" to "v0.50 INSCRIPTION PRIVÉE QC BUILD","student_manager" to "Gestion Élèves","student_manager_sub" to "Crée des comptes privés RS KICKBOX, génère des invitations QR et gère l'accès.","student_capacity" to "CAPACITÉ ÉLÈVES","active_accounts" to "comptes élèves actifs","create_student" to "+ Créer un compte élève","close_new" to "Fermer le nouveau compte","new_student" to "NOUVEAU COMPTE ÉLÈVE","student_name" to "Nom de l'élève","plan" to "Formule","create_qr" to "Créer compte + QR","show_qr" to "Afficher le QR","hide_qr" to "Masquer le QR","share_invite" to "Partager QR + lien app","share_store" to "Partager seulement Play Store","new_qr" to "Nouveau QR","delete" to "Supprimer","active" to "ACTIF","inactive" to "INACTIF","security_note" to "NOTE DE SÉCURITÉ PRODUCTION","search_students" to "Rechercher élèves","shown" to "affichés","total" to "total","show_more" to "Afficher plus","confirm" to "Confirmer"
    )
    val de=mapOf(
        "invite_title" to "Schüler-Einladungs-QR","invite_desc" to "Scanne den QR-Code deines Trainers oder lade das QR-Bild aus der Galerie hoch. E-Mail und Aktivierungscode werden automatisch ausgefüllt.","scan_qr" to "QR scannen","upload_qr" to "QR hochladen","email" to "E-Mail","activation_code" to "Passwort / Aktivierungscode","student_login" to "Schüler-Login","trainer_preview" to "Trainer-/Admin-Vorschau","build_label" to "v0.50 PRIVATE ANMELDUNG QC BUILD","student_manager" to "Schülerverwaltung","student_manager_sub" to "Private RS KICKBOX-Konten erstellen, QR-Einladungen erzeugen und Zugriff verwalten.","student_capacity" to "SCHÜLERKAPAZITÄT","active_accounts" to "aktive Schülerkonten","create_student" to "+ Neues Schülerkonto","close_new" to "Neues Konto schließen","new_student" to "NEUES SCHÜLERKONTO","student_name" to "Schülername","plan" to "Plan","create_qr" to "Konto + QR erstellen","show_qr" to "Einladungs-QR anzeigen","hide_qr" to "Einladungs-QR ausblenden","share_invite" to "QR + App-Link teilen","share_store" to "Nur Play-Store-Link teilen","new_qr" to "Neuer QR","delete" to "Löschen","active" to "AKTIV","inactive" to "INAKTIV","security_note" to "PRODUKTIONS-SICHERHEITSHINWEIS","search_students" to "Schüler suchen","shown" to "angezeigt","total" to "gesamt","show_more" to "Mehr anzeigen","confirm" to "Bestätigen"
    )
    val it=mapOf(
        "invite_title" to "QR invito allievo","invite_desc" to "Scansiona il QR dell'allenatore o carica l'immagine QR dalla galleria. E-mail e codice di attivazione verranno compilati automaticamente.","scan_qr" to "Scansiona QR","upload_qr" to "Carica QR","email" to "E-mail","activation_code" to "Password / codice attivazione","student_login" to "Accesso allievo","trainer_preview" to "Anteprima allenatore / admin","build_label" to "v0.50 ISCRIZIONE PRIVATA QC BUILD","student_manager" to "Gestione Allievi","student_manager_sub" to "Crea account RS KICKBOX privati, genera inviti QR e gestisci l'accesso.","student_capacity" to "CAPACITÀ ALLIEVI","active_accounts" to "account allievi attivi","create_student" to "+ Crea nuovo account","close_new" to "Chiudi nuovo account","new_student" to "NUOVO ACCOUNT ALLIEVO","student_name" to "Nome allievo","plan" to "Piano","create_qr" to "Crea account + QR","show_qr" to "Mostra QR invito","hide_qr" to "Nascondi QR invito","share_invite" to "Condividi QR + link app","share_store" to "Condividi solo Play Store","new_qr" to "Nuovo QR","delete" to "Elimina","active" to "ATTIVO","inactive" to "INATTIVO","security_note" to "NOTA SICUREZZA PRODUZIONE","search_students" to "Cerca allievi","shown" to "mostrati","total" to "totale","show_more" to "Mostra altro","confirm" to "Conferma"
    )
    val pl=mapOf(
        "invite_title" to "QR zaproszenia ucznia","invite_desc" to "Zeskanuj QR od trenera lub wczytaj obraz QR z galerii. E-mail i kod aktywacyjny zostaną uzupełnione automatycznie.","scan_qr" to "Skanuj QR","upload_qr" to "Wczytaj QR","email" to "E-mail","activation_code" to "Hasło / kod aktywacyjny","student_login" to "Logowanie ucznia","trainer_preview" to "Podgląd trener / admin","build_label" to "v0.50 PRYWATNY ZAPIS QC BUILD","student_manager" to "Menedżer Uczniów","student_manager_sub" to "Twórz prywatne konta RS KICKBOX, generuj zaproszenia QR i zarządzaj dostępem.","student_capacity" to "LIMIT UCZNIÓW","active_accounts" to "aktywnych kont uczniów","create_student" to "+ Utwórz konto ucznia","close_new" to "Zamknij nowe konto","new_student" to "NOWE KONTO UCZNIA","student_name" to "Imię ucznia","plan" to "Plan","create_qr" to "Utwórz konto + QR","show_qr" to "Pokaż QR zaproszenia","hide_qr" to "Ukryj QR zaproszenia","share_invite" to "Udostępnij QR + link app","share_store" to "Udostępnij tylko Play Store","new_qr" to "Nowy QR","delete" to "Usuń","active" to "AKTYWNY","inactive" to "NIEAKTYWNY","security_note" to "UWAGA O BEZPIECZEŃSTWIE PRODUKCJI","search_students" to "Szukaj uczniów","shown" to "pokazano","total" to "łącznie","show_more" to "Pokaż więcej","confirm" to "Potwierdź"
    )
    val tr=mapOf(
        "invite_title" to "Öğrenci davet QR'ı","invite_desc" to "Antrenörünün QR kodunu tara veya galeriden QR görselini yükle. E-posta ve aktivasyon kodu otomatik doldurulur.","scan_qr" to "QR tara","upload_qr" to "QR yükle","email" to "E-posta","activation_code" to "Şifre / aktivasyon kodu","student_login" to "Öğrenci girişi","trainer_preview" to "Antrenör / admin önizleme","build_label" to "v0.50 ÖZEL KAYIT QC BUILD","student_manager" to "Öğrenci Yönetimi","student_manager_sub" to "Özel RS KICKBOX hesapları oluştur, QR davetleri üret ve erişimi yönet.","student_capacity" to "ÖĞRENCİ KAPASİTESİ","active_accounts" to "aktif öğrenci hesabı","create_student" to "+ Yeni öğrenci hesabı","close_new" to "Yeni hesabı kapat","new_student" to "YENİ ÖĞRENCİ HESABI","student_name" to "Öğrenci adı","plan" to "Plan","create_qr" to "Hesap + QR oluştur","show_qr" to "Davet QR'ını göster","hide_qr" to "Davet QR'ını gizle","share_invite" to "QR + uygulama linkini paylaş","share_store" to "Sadece Play Store linkini paylaş","new_qr" to "Yeni QR","delete" to "Sil","active" to "AKTİF","inactive" to "PASİF","security_note" to "ÜRETİM GÜVENLİĞİ NOTU","search_students" to "Öğrenci ara","shown" to "gösteriliyor","total" to "toplam","show_more" to "Daha fazla göster","confirm" to "Onayla"
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}


fun rsEnrollMsg(lang:RsLang,key:String,name:String="",detail:String=""):String{
    val en=mapOf(
        "invalid_qr" to "This QR is not a valid RS KICKBOX student invitation.",
        "invite_loaded" to "Invitation loaded for {name} · {detail}. You can now log in.",
        "qr_unreadable" to "No readable RS KICKBOX QR code was found in this image.",
        "scan_empty" to "The scanned QR did not contain an invitation.",
        "scan_cancelled" to "QR scan cancelled.",
        "scanner_error" to "QR scanner could not open: {detail}",
        "welcome" to "Welcome {name}.",
        "login_failed" to "Student account not found, inactive, or activation code is incorrect.",
        "secure_hint" to "A secure invitation code is generated automatically. The QR contains the email + activation code, never a permanent password.",
        "enter_name" to "Enter the student's name.",
        "invalid_email" to "Enter a valid email address.",
        "email_exists" to "An account with this email already exists.",
        "limit_reached" to "The 100 active-student limit has been reached.",
        "created" to "Student account created. QR invitation is ready to share.",
        "no_accounts_title" to "NO CREATED STUDENT ACCOUNTS YET",
        "no_accounts_desc" to "Create the first student above. The account will immediately get an invitation QR.",
        "cannot_activate" to "Cannot activate more than 100 students.",
        "activated" to "{name} activated.",
        "deactivated" to "{name} deactivated.",
        "activation_label" to "Activation code",
        "store_ready" to "Play Store link ready to share",
        "new_qr_ready" to "A new invitation code was generated for {name}. The old QR is no longer valid in this preview.",
        "deleted" to "{name} deleted.",
        "delete_confirm" to "Tap Delete again to confirm removal of {name}.",
        "security_body" to "This acceptance build stores accounts locally. When the Supabase backend is connected, QR invitations will become one-time server tokens with expiry/revocation while keeping this same trainer/student workflow."
    )
    val nl=en+mapOf(
        "invalid_qr" to "Deze QR is geen geldige RS KICKBOX-leerlinguitnodiging.","invite_loaded" to "Uitnodiging geladen voor {name} · {detail}. Je kunt nu inloggen.","qr_unreadable" to "Geen leesbare RS KICKBOX QR-code gevonden in deze afbeelding.","scan_empty" to "De gescande QR bevatte geen uitnodiging.","scan_cancelled" to "QR-scan geannuleerd.","scanner_error" to "QR-scanner kon niet openen: {detail}","welcome" to "Welkom {name}.","login_failed" to "Leerlingaccount niet gevonden, inactief of activatiecode onjuist.","secure_hint" to "Er wordt automatisch een veilige activatiecode gemaakt. De QR bevat e-mail + activatiecode, nooit een permanent wachtwoord.","enter_name" to "Vul de naam van de leerling in.","invalid_email" to "Vul een geldig e-mailadres in.","email_exists" to "Er bestaat al een account met dit e-mailadres.","limit_reached" to "De limiet van 100 actieve leerlingen is bereikt.","created" to "Leerlingaccount aangemaakt. De QR-uitnodiging is klaar om te delen.","no_accounts_title" to "NOG GEEN LEERLINGACCOUNTS","no_accounts_desc" to "Maak hierboven de eerste leerling aan. Het account krijgt direct een QR-uitnodiging.","cannot_activate" to "Er kunnen niet meer dan 100 leerlingen actief zijn.","activated" to "{name} geactiveerd.","deactivated" to "{name} gedeactiveerd.","activation_label" to "Activatiecode","store_ready" to "Play Store-link klaar om te delen","new_qr_ready" to "Nieuwe uitnodigingscode gemaakt voor {name}. De oude QR is niet meer geldig in deze preview.","deleted" to "{name} verwijderd.","delete_confirm" to "Tik nogmaals op Verwijderen om {name} te verwijderen.","security_body" to "Deze acceptatiebuild bewaart accounts lokaal. Na koppeling met Supabase worden QR-uitnodigingen eenmalige server-tokens met verloop en intrekking, met dezelfde trainer/leerling-workflow."
    )
    val pt=en+mapOf("invalid_qr" to "Este QR não é um convite de aluno RS KICKBOX válido.","invite_loaded" to "Convite carregado para {name} · {detail}. Já podes entrar.","qr_unreadable" to "Não foi encontrado um QR RS KICKBOX legível nesta imagem.","scan_empty" to "O QR lido não continha um convite.","scan_cancelled" to "Leitura QR cancelada.","scanner_error" to "Não foi possível abrir o leitor QR: {detail}","welcome" to "Bem-vindo {name}.","login_failed" to "Conta de aluno não encontrada, inativa ou código de ativação incorreto.","secure_hint" to "É gerado automaticamente um código de convite seguro. O QR contém e-mail + código de ativação, nunca uma palavra-passe permanente.","enter_name" to "Introduz o nome do aluno.","invalid_email" to "Introduz um e-mail válido.","email_exists" to "Já existe uma conta com este e-mail.","limit_reached" to "Foi atingido o limite de 100 alunos ativos.","created" to "Conta de aluno criada. O convite QR está pronto para partilhar.","no_accounts_title" to "AINDA NÃO HÁ CONTAS DE ALUNOS","no_accounts_desc" to "Cria o primeiro aluno acima. A conta receberá imediatamente um convite QR.","cannot_activate" to "Não é possível ativar mais de 100 alunos.","activated" to "{name} ativado.","deactivated" to "{name} desativado.","activation_label" to "Código de ativação","store_ready" to "Link da Play Store pronto para partilhar","new_qr_ready" to "Foi gerado um novo código de convite para {name}. O QR antigo já não é válido nesta pré-visualização.","deleted" to "{name} eliminado.","delete_confirm" to "Toca novamente em Eliminar para confirmar a remoção de {name}.","security_body" to "Esta versão de aceitação guarda as contas localmente. Quando o Supabase for ligado, os convites QR passarão a tokens de servidor de utilização única, com expiração e revogação, mantendo o mesmo fluxo treinador/aluno.")
    val es=en+mapOf("invalid_qr" to "Este QR no es una invitación válida de alumno RS KICKBOX.","invite_loaded" to "Invitación cargada para {name} · {detail}. Ya puedes iniciar sesión.","qr_unreadable" to "No se encontró un QR RS KICKBOX legible en esta imagen.","scan_empty" to "El QR escaneado no contenía una invitación.","scan_cancelled" to "Escaneo QR cancelado.","scanner_error" to "No se pudo abrir el escáner QR: {detail}","welcome" to "Bienvenido {name}.","login_failed" to "Cuenta de alumno no encontrada, inactiva o código de activación incorrecto.","secure_hint" to "Se genera automáticamente un código de invitación seguro. El QR contiene correo + código de activación, nunca una contraseña permanente.","enter_name" to "Introduce el nombre del alumno.","invalid_email" to "Introduce un correo válido.","email_exists" to "Ya existe una cuenta con este correo.","limit_reached" to "Se alcanzó el límite de 100 alumnos activos.","created" to "Cuenta de alumno creada. La invitación QR está lista para compartir.","no_accounts_title" to "TODAVÍA NO HAY CUENTAS DE ALUMNOS","no_accounts_desc" to "Crea el primer alumno arriba. La cuenta recibirá inmediatamente una invitación QR.","cannot_activate" to "No se pueden activar más de 100 alumnos.","activated" to "{name} activado.","deactivated" to "{name} desactivado.","activation_label" to "Código de activación","store_ready" to "Enlace de Play Store listo para compartir","new_qr_ready" to "Se generó un nuevo código de invitación para {name}. El QR anterior ya no es válido en esta vista previa.","deleted" to "{name} eliminado.","delete_confirm" to "Pulsa Eliminar otra vez para confirmar la eliminación de {name}.","security_body" to "Esta versión de aceptación guarda las cuentas localmente. Al conectar Supabase, las invitaciones QR serán tokens de servidor de un solo uso con caducidad y revocación, manteniendo el mismo flujo entrenador/alumno.")
    val fr=en+mapOf("invalid_qr" to "Ce QR n'est pas une invitation élève RS KICKBOX valide.","invite_loaded" to "Invitation chargée pour {name} · {detail}. Tu peux maintenant te connecter.","qr_unreadable" to "Aucun QR RS KICKBOX lisible trouvé dans cette image.","scan_empty" to "Le QR scanné ne contenait pas d'invitation.","scan_cancelled" to "Scan QR annulé.","scanner_error" to "Impossible d'ouvrir le scanner QR : {detail}","welcome" to "Bienvenue {name}.","login_failed" to "Compte élève introuvable, inactif ou code d'activation incorrect.","secure_hint" to "Un code d'invitation sécurisé est généré automatiquement. Le QR contient l'e-mail + le code d'activation, jamais un mot de passe permanent.","enter_name" to "Saisis le nom de l'élève.","invalid_email" to "Saisis une adresse e-mail valide.","email_exists" to "Un compte existe déjà avec cet e-mail.","limit_reached" to "La limite de 100 élèves actifs est atteinte.","created" to "Compte élève créé. L'invitation QR est prête à être partagée.","no_accounts_title" to "AUCUN COMPTE ÉLÈVE CRÉÉ","no_accounts_desc" to "Crée le premier élève ci-dessus. Le compte recevra immédiatement une invitation QR.","cannot_activate" to "Impossible d'activer plus de 100 élèves.","activated" to "{name} activé.","deactivated" to "{name} désactivé.","activation_label" to "Code d'activation","store_ready" to "Lien Play Store prêt à partager","new_qr_ready" to "Un nouveau code d'invitation a été généré pour {name}. L'ancien QR n'est plus valide dans cet aperçu.","deleted" to "{name} supprimé.","delete_confirm" to "Appuie encore sur Supprimer pour confirmer la suppression de {name}.","security_body" to "Cette version d'acceptation stocke les comptes localement. Avec Supabase, les invitations QR deviendront des jetons serveur à usage unique avec expiration/révocation tout en conservant le même flux entraîneur/élève.")
    val de=en+mapOf("invalid_qr" to "Dieser QR-Code ist keine gültige RS KICKBOX-Schülereinladung.","invite_loaded" to "Einladung für {name} · {detail} geladen. Du kannst dich jetzt anmelden.","qr_unreadable" to "In diesem Bild wurde kein lesbarer RS KICKBOX-QR-Code gefunden.","scan_empty" to "Der gescannte QR-Code enthielt keine Einladung.","scan_cancelled" to "QR-Scan abgebrochen.","scanner_error" to "QR-Scanner konnte nicht geöffnet werden: {detail}","welcome" to "Willkommen {name}.","login_failed" to "Schülerkonto nicht gefunden, inaktiv oder Aktivierungscode falsch.","secure_hint" to "Ein sicherer Einladungscode wird automatisch erzeugt. Der QR enthält E-Mail + Aktivierungscode, niemals ein dauerhaftes Passwort.","enter_name" to "Schülernamen eingeben.","invalid_email" to "Gültige E-Mail-Adresse eingeben.","email_exists" to "Für diese E-Mail existiert bereits ein Konto.","limit_reached" to "Das Limit von 100 aktiven Schülern ist erreicht.","created" to "Schülerkonto erstellt. Die QR-Einladung kann geteilt werden.","no_accounts_title" to "NOCH KEINE SCHÜLERKONTEN","no_accounts_desc" to "Erstelle oben den ersten Schüler. Das Konto erhält sofort eine QR-Einladung.","cannot_activate" to "Es können nicht mehr als 100 Schüler aktiviert werden.","activated" to "{name} aktiviert.","deactivated" to "{name} deaktiviert.","activation_label" to "Aktivierungscode","store_ready" to "Play-Store-Link bereit zum Teilen","new_qr_ready" to "Für {name} wurde ein neuer Einladungscode erzeugt. Der alte QR-Code ist in dieser Vorschau nicht mehr gültig.","deleted" to "{name} gelöscht.","delete_confirm" to "Tippe erneut auf Löschen, um {name} zu entfernen.","security_body" to "Dieser Abnahme-Build speichert Konten lokal. Nach der Supabase-Anbindung werden QR-Einladungen einmalige Server-Tokens mit Ablauf/Widerruf, bei gleichem Trainer-/Schüler-Ablauf.")
    val it=en+mapOf("invalid_qr" to "Questo QR non è un invito allievo RS KICKBOX valido.","invite_loaded" to "Invito caricato per {name} · {detail}. Ora puoi accedere.","qr_unreadable" to "Nessun QR RS KICKBOX leggibile trovato in questa immagine.","scan_empty" to "Il QR scansionato non conteneva un invito.","scan_cancelled" to "Scansione QR annullata.","scanner_error" to "Impossibile aprire lo scanner QR: {detail}","welcome" to "Benvenuto {name}.","login_failed" to "Account allievo non trovato, inattivo o codice di attivazione errato.","secure_hint" to "Viene generato automaticamente un codice invito sicuro. Il QR contiene e-mail + codice di attivazione, mai una password permanente.","enter_name" to "Inserisci il nome dell'allievo.","invalid_email" to "Inserisci un indirizzo e-mail valido.","email_exists" to "Esiste già un account con questa e-mail.","limit_reached" to "È stato raggiunto il limite di 100 allievi attivi.","created" to "Account allievo creato. L'invito QR è pronto da condividere.","no_accounts_title" to "NESSUN ACCOUNT ALLIEVO CREATO","no_accounts_desc" to "Crea il primo allievo qui sopra. L'account riceverà subito un invito QR.","cannot_activate" to "Non è possibile attivare più di 100 allievi.","activated" to "{name} attivato.","deactivated" to "{name} disattivato.","activation_label" to "Codice di attivazione","store_ready" to "Link Play Store pronto da condividere","new_qr_ready" to "È stato generato un nuovo codice invito per {name}. Il vecchio QR non è più valido in questa anteprima.","deleted" to "{name} eliminato.","delete_confirm" to "Tocca di nuovo Elimina per confermare la rimozione di {name}.","security_body" to "Questa build di accettazione salva gli account localmente. Con Supabase, gli inviti QR diventeranno token server monouso con scadenza/revoca mantenendo lo stesso flusso allenatore/allievo.")
    val pl=en+mapOf("invalid_qr" to "Ten QR nie jest prawidłowym zaproszeniem ucznia RS KICKBOX.","invite_loaded" to "Zaproszenie wczytane dla {name} · {detail}. Możesz się teraz zalogować.","qr_unreadable" to "Nie znaleziono czytelnego kodu QR RS KICKBOX na tym obrazie.","scan_empty" to "Zeskanowany QR nie zawierał zaproszenia.","scan_cancelled" to "Skanowanie QR anulowane.","scanner_error" to "Nie można otworzyć skanera QR: {detail}","welcome" to "Witaj {name}.","login_failed" to "Nie znaleziono konta ucznia, konto jest nieaktywne lub kod aktywacyjny jest błędny.","secure_hint" to "Bezpieczny kod zaproszenia jest generowany automatycznie. QR zawiera e-mail + kod aktywacyjny, nigdy stałe hasło.","enter_name" to "Wpisz imię ucznia.","invalid_email" to "Wpisz prawidłowy adres e-mail.","email_exists" to "Konto z tym adresem e-mail już istnieje.","limit_reached" to "Osiągnięto limit 100 aktywnych uczniów.","created" to "Konto ucznia utworzone. Zaproszenie QR jest gotowe do udostępnienia.","no_accounts_title" to "BRAK UTWORZONYCH KONT UCZNIÓW","no_accounts_desc" to "Utwórz pierwszego ucznia powyżej. Konto natychmiast otrzyma zaproszenie QR.","cannot_activate" to "Nie można aktywować więcej niż 100 uczniów.","activated" to "{name} aktywowany.","deactivated" to "{name} dezaktywowany.","activation_label" to "Kod aktywacyjny","store_ready" to "Link Play Store gotowy do udostępnienia","new_qr_ready" to "Wygenerowano nowy kod zaproszenia dla {name}. Stary QR nie jest już ważny w tym podglądzie.","deleted" to "{name} usunięty.","delete_confirm" to "Naciśnij Usuń ponownie, aby potwierdzić usunięcie {name}.","security_body" to "Ta wersja testowa przechowuje konta lokalnie. Po podłączeniu Supabase zaproszenia QR staną się jednorazowymi tokenami serwera z wygaśnięciem i możliwością unieważnienia, przy zachowaniu tego samego przepływu trener/uczeń.")
    val tr=en+mapOf("invalid_qr" to "Bu QR geçerli bir RS KICKBOX öğrenci daveti değil.","invite_loaded" to "{name} · {detail} için davet yüklendi. Artık giriş yapabilirsin.","qr_unreadable" to "Bu görselde okunabilir bir RS KICKBOX QR kodu bulunamadı.","scan_empty" to "Taranan QR bir davet içermiyordu.","scan_cancelled" to "QR taraması iptal edildi.","scanner_error" to "QR tarayıcı açılamadı: {detail}","welcome" to "Hoş geldin {name}.","login_failed" to "Öğrenci hesabı bulunamadı, pasif veya aktivasyon kodu yanlış.","secure_hint" to "Güvenli bir davet kodu otomatik oluşturulur. QR e-posta + aktivasyon kodunu içerir, kalıcı şifre içermez.","enter_name" to "Öğrenci adını gir.","invalid_email" to "Geçerli bir e-posta adresi gir.","email_exists" to "Bu e-posta ile zaten bir hesap var.","limit_reached" to "100 aktif öğrenci sınırına ulaşıldı.","created" to "Öğrenci hesabı oluşturuldu. QR daveti paylaşmaya hazır.","no_accounts_title" to "HENÜZ ÖĞRENCİ HESABI YOK","no_accounts_desc" to "Yukarıdan ilk öğrenciyi oluştur. Hesap hemen bir QR daveti alır.","cannot_activate" to "100'den fazla öğrenci aktif edilemez.","activated" to "{name} aktifleştirildi.","deactivated" to "{name} pasifleştirildi.","activation_label" to "Aktivasyon kodu","store_ready" to "Play Store bağlantısı paylaşmaya hazır","new_qr_ready" to "{name} için yeni davet kodu oluşturuldu. Eski QR bu önizlemede artık geçerli değil.","deleted" to "{name} silindi.","delete_confirm" to "{name} hesabını kaldırmayı onaylamak için Sil'e tekrar dokun.","security_body" to "Bu kabul sürümü hesapları yerel olarak saklar. Supabase bağlandığında QR davetleri süreli/iptal edilebilir tek kullanımlık sunucu tokenlarına dönüşür ve aynı antrenör/öğrenci akışı korunur.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return (pack[key]?:en[key]?:key).replace("{name}",name).replace("{detail}",detail)
}

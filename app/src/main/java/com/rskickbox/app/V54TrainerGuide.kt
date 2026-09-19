package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RsGuideItemV54(
    val category:String,
    val title:String,
    val route:String,
    val purpose:String,
    val trainerDoes:String,
    val studentSees:String,
    val example:String,
    val steps:List<String>,
    val visual:String
)

private fun rsGuideItemsV54():List<RsGuideItemV54> = listOf(
    RsGuideItemV54(
        "START","Student Manager","members",
        "Create, activate, deactivate and manage private student accounts.",
        "Create a student, choose BASIC/PRO/ELITE, generate a QR invite and control whether the account is active.",
        "The student signs in with the private invitation flow and only sees their own account data.",
        "Example: create Maria as PRO, share her QR invitation, then deactivate the account temporarily if her membership pauses.",
        listOf("Open Student Manager","Enter name and email","Choose plan","Create account","Share QR invitation","Use Active switch when access changes"),
        "STUDENT_CARD"
    ),
    RsGuideItemV54(
        "START","Access & Membership Plans","access",
        "Control who has access and what each membership costs.",
        "Change a student's plan or active status. In Membership Plans you can edit BASIC, PRO and ELITE monthly prices and descriptions.",
        "Student Finance, book access and premium content use the student's assigned plan.",
        "Example: change a student from BASIC to PRO; their unlocked content and configured pricing update from the shared membership source.",
        listOf("Open Membership Plans","Set monthly prices","Save","Open Access & Subscriptions","Assign the correct plan to each student"),
        "PLAN"
    ),
    RsGuideItemV54(
        "COACHING","AI Technique Coach","voice",
        "Review short technique videos and keep a coaching history.",
        "Review submitted technique history and use the trainer AI persona/voice tools during coaching.",
        "Students can import a short technique video, select a technique, receive structured feedback, hear coaching and save it to history.",
        "Example: a student uploads a roundhouse kick video; the coach feedback highlights guard, pivot, balance and recovery.",
        listOf("Student opens AI Technique Coach","Upload up to 20 seconds","Choose technique","Analyze","Listen to coaching","Save result","Trainer reviews history"),
        "AI_COACH"
    ),
    RsGuideItemV54(
        "COACHING","Private Coach Chat","coachchat",
        "Keep private one-to-one communication in a separate thread for every student.",
        "Open Coach Inbox, choose a student, read unread messages and reply.",
        "Each student only sees their own private trainer conversation.",
        "Example: Bryan asks whether to attend sparring; trainer replies with a recommendation based on current progress.",
        listOf("Open Coach Inbox","Choose student","Read thread","Reply","Unread state updates automatically"),
        "CHAT"
    ),
    RsGuideItemV54(
        "COACHING","Homework & Training Plans","homework_admin",
        "Assign simple homework or reusable guided training plans to an individual student.",
        "Create a plan once, add ordered exercises with sets/reps/time, and attach trainer-approved Training Media images or videos without uploading duplicate copies. The same plan can then be assigned to different students.",
        "Student sees the assignment, follows each exercise step, studies the included visual/video examples and can mark steps plus the overall homework complete.",
        "Example: create a defense plan with footwork, guard recovery and a reference clip, then assign it to several students with separate due dates.",
        listOf("Open Homework Manager","Select student","Create or select a reusable training plan","Add ordered exercise steps","Attach existing Training Media where useful","Set due label","Assign plan","Review step and homework completion","Check trainer-only storage usage"),
        "HOMEWORK"
    ),
    RsGuideItemV54(
        "COACHING","Coach Notes","notes",
        "Keep private development notes that are not shown in the normal student UI.",
        "Select a student and save internal observations about technique, attitude, goals or follow-up.",
        "Students do not see these notes in their normal app screens.",
        "Example: note that a student should reduce sparring intensity for one week and focus on defense.",
        listOf("Open Coach Notes","Select student","Write private note","Save","Use notes when planning the next session"),
        "NOTES"
    ),
    RsGuideItemV54(
        "COACHING","Assessments & Progress","assessments",
        "Record measurable skill scores and turn them into a student progress profile.",
        "Score Punches, Kicks, Defense, Footwork, Combinations and Conditioning from 0–100 and add feedback.",
        "Student Progress shows the latest assessment and history. Trainer Progress combines assessments with homework, challenges and Fight Camp.",
        "Example: Defense improves from 67 to 75; the next assessment becomes the current progress profile.",
        listOf("Open Assessments","Select student","Set six skill scores","Write summary","Save","Review in Progress Manager"),
        "ASSESSMENT"
    ),
    RsGuideItemV54(
        "TRAINING","Classes & Attendance","classes_admin",
        "Run the class schedule, bookings and attendance from the same shared records.",
        "Create classes, set capacity, activate/deactivate classes and manage attendance.",
        "Students see active classes, available places and their bookings.",
        "Example: Advanced Pads has capacity 12; student bookings reduce available places and attendance is recorded against the same class.",
        listOf("Open Class Manager","Create class","Set capacity","Activate","Students book","Open Attendance after class"),
        "CLASS"
    ),
    RsGuideItemV54(
        "TRAINING","QR Attendance","qr_attendance",
        "Generate a class-specific check-in QR linked to the attendance system.",
        "Select an active class and generate a new check-in QR.",
        "Student uses Class Check-In to scan the trainer QR and register attendance.",
        "Example: put the QR on a tablet at reception so arriving students can scan before class.",
        listOf("Open QR Attendance","Select class","Generate QR","Show QR to students","Student scans from Class Check-In","Verify attendance"),
        "QR"
    ),
    RsGuideItemV54(
        "TRAINING","Session Builder","session_builder",
        "Build structured training sessions from timed blocks.",
        "Create a session title, add timed blocks with instructions and choose the active session.",
        "Student Session Player follows the active trainer-built workout with timer, next and restart controls.",
        "Example: 5-minute warm-up, 3-minute jab round, 3-minute combinations, 3-minute cooldown.",
        listOf("Open Session Builder","Create session","Add timed blocks","Save","Make active","Student opens Session Player"),
        "SESSION"
    ),
    RsGuideItemV54(
        "TRAINING","Private Lessons","schedule",
        "Offer one-to-one coaching availability and process requests.",
        "Create available private lesson slots, confirm one student and decline competing requests when necessary.",
        "Students request an available slot and see Requested, Confirmed or Declined status.",
        "Example: Saturday 12:00 is requested by two students; confirming one automatically keeps only one confirmed booking for that slot.",
        listOf("Open Trainer Schedule","Add availability","Wait for requests","Review student note","Confirm one request","Student sees confirmation"),
        "PRIVATE"
    ),
    RsGuideItemV54(
        "TRAINING","Challenges, Fight Camp & Badges","challenge_admin",
        "Create measurable goals and manage the fixed 8-week Fight Camp.",
        "Assign challenges, track completion and update each student's current Fight Camp week and focus.",
        "Students update challenge progress, follow the 8-week camp and earn badges from real completion/assessment data.",
        "Example: challenge = 20 bag rounds; when target is reached, the student's challenge completion and badge logic update.",
        listOf("Create challenge","Set target/unit","Student records progress","Update Fight Camp week","Review earned badges and analytics"),
        "CHALLENGE"
    ),
    RsGuideItemV54(
        "CONTENT","Content Manager & Knowledge Vault","content",
        "Publish searchable training lessons/articles by membership level.",
        "Create title, category, long-form content, access tier and published/draft state.",
        "Students see only unlocked published content, can search it, save favorites and reopen reading history.",
        "Example: publish Advanced Counter Fighting for PRO/ELITE while Jab Fundamentals remains available to ALL.",
        listOf("Open Content Manager","Write lesson","Choose access tier","Publish","Student finds it in Knowledge Vault/Search","Student can save favorite"),
        "CONTENT"
    ),
    RsGuideItemV54(
        "CONTENT","Training Media","media",
        "Publish real training videos and images while preserving original video audio.",
        "Upload a media file, add title/category/description, choose ALL/BASIC/PRO/ELITE access and publish or unpublish it.",
        "Students see only published media unlocked for their membership and can open videos with full player controls and audio.",
        "Example: publish an Advanced Padwork video to PRO/ELITE while a Jab Fundamentals clip remains available to ALL.",
        listOf("Open Training Media Manager","Choose image/video","Add title and category","Choose membership access","Publish","Student opens Training Media"),
        "CONTENT"
    ),
    RsGuideItemV54(
        "CONTENT","Promotion Manager","landing_admin",
        "Publish clickable promotional thumbnails and manage the featured book.",
        "Upload multiple thumbnails, give each an external URL, control active state, upload book cover/preview/full PDF and define access.",
        "Students see an auto-sliding promotion carousel. Clicking a promo opens its external link. The book can open Amazon or the in-app reader.",
        "Example: upload a seminar thumbnail linked to registration, a gloves thumbnail linked to a store, and the book cover linked to Amazon.",
        listOf("Upload thumbnail","Enter title","Paste external HTTPS link","Save","Repeat for more promos","Configure book cover/Amazon/PDF access below"),
        "PROMO"
    ),
    RsGuideItemV54(
        "CONTENT","Book Reader","book",
        "Offer a preview or full book inside the app with controlled access.",
        "Upload preview PDF and full PDF, select ALL/BASIC/PRO/ELITE/PRIVATE and optionally grant specific student emails free access.",
        "Students can swipe pages left/right, use page-turn perspective, pinch-zoom, pan, search text and jump between matches.",
        "Example: PRO and ELITE receive the full book, BASIC receives preview only, and one BASIC student is manually gifted full access.",
        listOf("Open Book Manager","Upload preview PDF","Upload full PDF","Set access tier","Add gifted emails if needed","Student opens Trainer Book"),
        "BOOK"
    ),
    RsGuideItemV54(
        "COMMUNITY","Notifications","notifications",
        "Send targeted announcements to all students or a specific membership tier.",
        "Create announcements for ALL, BASIC, PRO or ELITE and manage active notification records.",
        "Students see notifications matching their plan with individual read state.",
        "Example: send an ELITE-only message about advanced sparring or an ALL message about holiday opening hours.",
        listOf("Open Notifications","Write announcement","Choose audience","Publish","Student opens inbox","Read state is stored per student"),
        "NOTIFICATION"
    ),
    RsGuideItemV54(
        "COMMUNITY","Community & Groups","community",
        "Run member posts and focused training groups.",
        "Moderate posts, deactivate inappropriate posts, create groups and control which groups are active.",
        "Students can publish updates, delete their own posts and join/leave active groups.",
        "Example: create a Fight Camp Group and keep general Community posts separate from focused training membership.",
        listOf("Open Community Moderation","Review posts","Use active switch if needed","Open Groups Manager","Create group","Students join"),
        "COMMUNITY"
    ),
    RsGuideItemV54(
        "BUSINESS","Finance & Invoices","payments",
        "Manage membership money records and student invoices.",
        "Configure membership plans, create invoices, update payment status and review the ledger.",
        "Each student sees only their own membership/finance view and invoices.",
        "Example: RS PRO is €49/month; create a €49 invoice, mark it PAID when received, and Analytics counts collected revenue.",
        listOf("Set plan price","Open Payment Center","Create invoice for student","Set due/status","Mark PAID when received","Review Analytics"),
        "FINANCE"
    ),
    RsGuideItemV54(
        "BUSINESS","Events & Promotions","events_admin",
        "Manage club events separately from normal class bookings.",
        "Create events with capacity, activate/deactivate them and track RSVP totals.",
        "Students RSVP or cancel their own attendance from RS Events.",
        "Example: Technique Seminar capacity 40; RSVP progress updates as members join.",
        listOf("Open Event Manager","Create event","Set capacity","Activate","Students RSVP","Monitor attendance count"),
        "EVENT"
    ),
    RsGuideItemV54(
        "BUSINESS","Support & Documents","support",
        "Handle member questions and publish club information.",
        "Answer private support tickets and publish documents with membership access levels.",
        "Students can send private support requests and read documents unlocked for their plan.",
        "Example: publish gym rules to ALL and an advanced competition guide to ELITE, while answering a student's billing question privately.",
        listOf("Open Support Inbox","Reply/resolve tickets","Open Documents Manager","Publish document","Choose tier","Student sees unlocked documents"),
        "SUPPORT"
    ),
    RsGuideItemV54(
        "BUSINESS","Analytics","analytics",
        "See live operating metrics calculated from the same records used everywhere else.",
        "Review active students, collected/pending revenue, class occupancy, open support, homework, challenge completion and active Fight Camps.",
        "Students do not see the trainer analytics dashboard.",
        "Example: marking an invoice PAID increases Collected Revenue; class bookings change Class Occupancy automatically.",
        listOf("Open Analytics","Review KPIs","Open the related manager when a metric needs attention","Use Progress Manager for individual development"),
        "ANALYTICS"
    ),
    RsGuideItemV54(
        "SETTINGS","Branding & Visual Placement","branding",
        "Control the premium RS visual system without editing Android source.",
        "Select one visual placement slot, upload image/GIF/video, adjust login form opacity and configure supported visual assets.",
        "Students see the branded media in the corresponding app locations.",
        "Example: replace the login background while keeping the login form semi-transparent so the background remains visible.",
        listOf("Open Branding Studio","Choose placement","Upload media","Review fitted result","Adjust login opacity if needed","Save"),
        "BRANDING"
    ),
    RsGuideItemV54(
        "SETTINGS","App Settings & Operations","settings",
        "Control live student-facing operations without changing app code.",
        "Enable maintenance notices and switch community posting, class booking, private-lesson requests, referral sharing and in-app reminders on or off.",
        "Students immediately see the maintenance notice and affected actions become disabled while safe read/cancel/support actions remain available.",
        "Example: disable new class bookings during a schedule reset while still allowing students to cancel existing bookings.",
        listOf("Open App Settings","Set maintenance message if needed","Toggle operational controls","Changes save immediately","Review student-side result"),
        "RELEASE"
    ),
    RsGuideItemV54(
        "SETTINGS","Release, Privacy & Backend Readiness","release",
        "See what is local-ready versus what still requires production services.",
        "Review legal/support readiness, backend status, signing status and production limitations.",
        "Students get privacy/export/deactivation controls in Settings.",
        "Example: local enrollment can be tested now, while real multi-device authentication remains listed as requiring Supabase.",
        listOf("Open Release & Legal Center","Review green/local-ready items","Review production requirements","Complete legal/support information before store release"),
        "RELEASE"
    )
)

private fun rsGuideUiV54(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "Trainer App Guide","subtitle" to "How every major RS KICKBOXING section works, with examples and visual walkthroughs.",
        "search" to "Search app features…","all" to "ALL","purpose" to "WHAT THIS SECTION DOES","trainer" to "WHAT THE TRAINER DOES",
        "student" to "WHAT THE STUDENT SEES","example" to "EXAMPLE","workflow" to "HOW TO USE IT","visual" to "VISUAL EXAMPLE",
        "open" to "Open this section","quick" to "QUICK START","show" to "Show how it works","hide" to "Hide guide","sections" to "sections",
        "quick_body" to "Start with Student Manager → Membership Plans → Classes → Content → Notifications. Then add coaching, finance, promotions and advanced tools as you need them.",
        "tip" to "TIP","tip_body" to "You do not need to use every feature. Activate the parts that fit your gym workflow and expand later."
    )
    val nl=en+mapOf("title" to "Trainer App Gids","subtitle" to "Hoe elke belangrijke RS KICKBOXING-sectie werkt, met voorbeelden en visuele uitleg.","search" to "Zoek appfuncties…","all" to "ALLES","purpose" to "WAT DEZE SECTIE DOET","trainer" to "WAT DE TRAINER DOET","student" to "WAT DE LEERLING ZIET","example" to "VOORBEELD","workflow" to "HOE JE HET GEBRUIKT","visual" to "VISUEEL VOORBEELD","open" to "Open deze sectie","quick" to "SNEL STARTEN","show" to "Laat zien hoe het werkt","hide" to "Verberg uitleg","sections" to "secties","quick_body" to "Begin met Student Manager → Membership Plans → Classes → Content → Notifications. Voeg daarna coaching, finance, promoties en geavanceerde tools toe wanneer nodig.","tip" to "TIP","tip_body" to "Je hoeft niet elke functie te gebruiken. Activeer wat bij jouw sportschool past en breid later uit.")
    val pt=en+mapOf("title" to "Guia da App para Treinador","subtitle" to "Como funciona cada secção principal do RS KICKBOXING, com exemplos e explicações visuais.","search" to "Pesquisar funcionalidades…","all" to "TODAS","purpose" to "O QUE ESTA SECÇÃO FAZ","trainer" to "O QUE O TREINADOR FAZ","student" to "O QUE O ALUNO VÊ","example" to "EXEMPLO","workflow" to "COMO USAR","visual" to "EXEMPLO VISUAL","open" to "Abrir esta secção","quick" to "INÍCIO RÁPIDO","show" to "Ver como funciona","hide" to "Ocultar guia","sections" to "secções","quick_body" to "Começa por Student Manager → Membership Plans → Classes → Content → Notifications. Depois adiciona coaching, finanças, promoções e ferramentas avançadas quando precisares.","tip" to "DICA","tip_body" to "Não precisas de usar todas as funções. Ativa as que combinam com o teu ginásio e expande depois.")
    val es=en+mapOf("title" to "Guía de la App para Entrenador","subtitle" to "Cómo funciona cada sección principal de RS KICKBOXING, con ejemplos y recorridos visuales.","search" to "Buscar funciones…","all" to "TODAS","purpose" to "QUÉ HACE ESTA SECCIÓN","trainer" to "QUÉ HACE EL ENTRENADOR","student" to "QUÉ VE EL ALUMNO","example" to "EJEMPLO","workflow" to "CÓMO USARLO","visual" to "EJEMPLO VISUAL","open" to "Abrir esta sección","quick" to "INICIO RÁPIDO","show" to "Ver cómo funciona","hide" to "Ocultar guía","sections" to "secciones","quick_body" to "Empieza con Student Manager → Membership Plans → Classes → Content → Notifications. Después añade coaching, finanzas, promociones y herramientas avanzadas según necesites.","tip" to "CONSEJO","tip_body" to "No necesitas usar todas las funciones. Activa las que encajen con tu gimnasio y amplía después.")
    val fr=en+mapOf("title" to "Guide App Entraîneur","subtitle" to "Comment fonctionne chaque grande section de RS KICKBOXING, avec exemples et explications visuelles.","search" to "Rechercher des fonctions…","all" to "TOUT","purpose" to "RÔLE DE CETTE SECTION","trainer" to "CE QUE FAIT L’ENTRAÎNEUR","student" to "CE QUE VOIT L’ÉLÈVE","example" to "EXEMPLE","workflow" to "COMMENT L’UTILISER","visual" to "EXEMPLE VISUEL","open" to "Ouvrir cette section","quick" to "DÉMARRAGE RAPIDE","show" to "Voir comment ça marche","hide" to "Masquer le guide","sections" to "sections","quick_body" to "Commence par Student Manager → Membership Plans → Classes → Content → Notifications. Ajoute ensuite coaching, finance, promotions et outils avancés selon tes besoins.","tip" to "ASTUCE","tip_body" to "Tu n’as pas besoin d’utiliser toutes les fonctions. Active celles adaptées à ton club et développe ensuite.")
    val de=en+mapOf("title" to "Trainer-App-Anleitung","subtitle" to "So funktioniert jeder wichtige RS KICKBOXING-Bereich – mit Beispielen und visuellen Abläufen.","search" to "App-Funktionen suchen…","all" to "ALLE","purpose" to "WAS DIESER BEREICH MACHT","trainer" to "WAS DER TRAINER MACHT","student" to "WAS DER SCHÜLER SIEHT","example" to "BEISPIEL","workflow" to "SO VERWENDEST DU ES","visual" to "VISUELLES BEISPIEL","open" to "Diesen Bereich öffnen","quick" to "SCHNELLSTART","show" to "So funktioniert es","hide" to "Anleitung ausblenden","sections" to "Bereiche","quick_body" to "Beginne mit Student Manager → Membership Plans → Classes → Content → Notifications. Füge danach Coaching, Finanzen, Aktionen und erweiterte Tools hinzu.","tip" to "TIPP","tip_body" to "Du musst nicht jede Funktion nutzen. Aktiviere die Bereiche, die zu deinem Gym passen, und erweitere später.")
    val it=en+mapOf("title" to "Guida App Trainer","subtitle" to "Come funziona ogni sezione principale di RS KICKBOXING, con esempi e walkthrough visivi.","search" to "Cerca funzioni…","all" to "TUTTE","purpose" to "COSA FA QUESTA SEZIONE","trainer" to "COSA FA IL TRAINER","student" to "COSA VEDE L’ALLIEVO","example" to "ESEMPIO","workflow" to "COME SI USA","visual" to "ESEMPIO VISIVO","open" to "Apri questa sezione","quick" to "AVVIO RAPIDO","show" to "Mostra come funziona","hide" to "Nascondi guida","sections" to "sezioni","quick_body" to "Inizia con Student Manager → Membership Plans → Classes → Content → Notifications. Poi aggiungi coaching, finanza, promozioni e strumenti avanzati quando servono.","tip" to "SUGGERIMENTO","tip_body" to "Non devi usare ogni funzione. Attiva quelle adatte al tuo club ed espandi in seguito.")
    val pl=en+mapOf("title" to "Przewodnik Trenera","subtitle" to "Jak działa każda główna sekcja RS KICKBOXING, z przykładami i wizualnym wyjaśnieniem.","search" to "Szukaj funkcji…","all" to "WSZYSTKIE","purpose" to "CO ROBI TA SEKCJA","trainer" to "CO ROBI TRENER","student" to "CO WIDZI UCZEŃ","example" to "PRZYKŁAD","workflow" to "JAK TEGO UŻYWAĆ","visual" to "PRZYKŁAD WIZUALNY","open" to "Otwórz tę sekcję","quick" to "SZYBKI START","show" to "Pokaż jak to działa","hide" to "Ukryj przewodnik","sections" to "sekcji","quick_body" to "Zacznij od Student Manager → Membership Plans → Classes → Content → Notifications. Potem dodaj coaching, finanse, promocje i narzędzia zaawansowane.","tip" to "WSKAZÓWKA","tip_body" to "Nie musisz używać każdej funkcji. Włącz te, które pasują do Twojego klubu, i rozwijaj później.")
    val tr=en+mapOf("title" to "Antrenör Uygulama Rehberi","subtitle" to "RS KICKBOXING’taki her önemli bölümün nasıl çalıştığı; örnekler ve görsel anlatımlarla.","search" to "Uygulama özelliklerinde ara…","all" to "TÜMÜ","purpose" to "BU BÖLÜM NE YAPAR","trainer" to "ANTRENÖR NE YAPAR","student" to "ÖĞRENCİ NE GÖRÜR","example" to "ÖRNEK","workflow" to "NASIL KULLANILIR","visual" to "GÖRSEL ÖRNEK","open" to "Bu bölümü aç","quick" to "HIZLI BAŞLANGIÇ","show" to "Nasıl çalıştığını göster","hide" to "Rehberi gizle","sections" to "bölüm","quick_body" to "Student Manager → Membership Plans → Classes → Content → Notifications ile başla. Sonra ihtiyacına göre coaching, finans, promosyon ve gelişmiş araçları ekle.","tip" to "İPUCU","tip_body" to "Her özelliği kullanmak zorunda değilsin. Salonuna uygun bölümleri etkinleştir ve sonra genişlet.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

private fun rsGuideCategoryLabelV54(lang:RsLang,category:String):String{
    val values=when(category){
        "START"->listOf("START","START","INÍCIO","INICIO","DÉPART","START","INIZIO","START","BAŞLANGIÇ")
        "COACHING"->listOf("COACHING","COACHING","COACHING","COACHING","COACHING","COACHING","COACHING","COACHING","KOÇLUK")
        "TRAINING"->listOf("TRAINING","TRAINING","TREINO","ENTRENAMIENTO","ENTRAÎNEMENT","TRAINING","ALLENAMENTO","TRENING","ANTRENMAN")
        "CONTENT"->listOf("CONTENT","CONTENT","CONTEÚDO","CONTENIDO","CONTENU","INHALT","CONTENUTI","TREŚCI","İÇERİK")
        "COMMUNITY"->listOf("COMMUNITY","COMMUNITY","COMUNIDADE","COMUNIDAD","COMMUNAUTÉ","COMMUNITY","COMMUNITY","SPOŁECZNOŚĆ","TOPLULUK")
        "BUSINESS"->listOf("BUSINESS","BEDRIJF","NEGÓCIO","NEGOCIO","BUSINESS","BUSINESS","BUSINESS","BIZNES","İŞLETME")
        "SETTINGS"->listOf("SETTINGS","INSTELLINGEN","DEFINIÇÕES","AJUSTES","RÉGLAGES","EINSTELLUNGEN","IMPOSTAZIONI","USTAWIENIA","AYARLAR")
        else->listOf("ALL","ALLES","TODAS","TODAS","TOUT","ALLE","TUTTE","WSZYSTKIE","TÜMÜ")
    }
    val index=when(lang.code){"nl"->1;"pt"->2;"es"->3;"fr"->4;"de"->5;"it"->6;"pl"->7;"tr"->8;else->0}
    return values[index]
}

@Composable
private fun RsGuideVisualV54(c:RsPalette,type:String){
    Surface(color=c.panel.copy(alpha=.78f),shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            when(type){
                "STUDENT_CARD"->{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Column{
                            Text("Alex de Vries",color=c.bright,fontWeight=FontWeight.Black)
                            Text("alex@rskickbox.nl",color=c.muted,fontSize=9.sp)
                        }
                        AssistChip(onClick={},label={Text("PRO")})
                    }
                    LinearProgressIndicator(progress={.78f},modifier=Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Button({},Modifier.weight(1f)){Text("QR",fontSize=9.sp)}
                        OutlinedButton({},Modifier.weight(1f)){Text("ACTIVE",fontSize=9.sp)}
                    }
                }
                "PLAN"->{
                    listOf("BASIC · €29","PRO · €49","ELITE · €69").forEachIndexed{i,label->
                        Surface(color=if(i==1)c.gold.copy(alpha=.18f) else c.panel,shape=MaterialTheme.shapes.small,modifier=Modifier.fillMaxWidth()){
                            Row(Modifier.padding(9.dp),horizontalArrangement=Arrangement.SpaceBetween){
                                Text(label,color=c.bright,fontWeight=FontWeight.Bold)
                                Text(if(i==1)"SELECTED" else "AVAILABLE",color=c.muted,fontSize=9.sp)
                            }
                        }
                    }
                }
                "AI_COACH"->{
                    Text("ROUNDHOUSE KICK",color=c.bright,fontWeight=FontWeight.Black)
                    listOf("Guard recovery" to .78f,"Hip rotation" to .86f,"Balance" to .72f).forEach{(n,v)->
                        Text(n,color=c.text,fontSize=10.sp);LinearProgressIndicator(progress={v},modifier=Modifier.fillMaxWidth())
                    }
                    Text("Coach cue: pivot earlier and recover guard faster.",color=c.muted,fontSize=10.sp)
                }
                "CHAT"->{
                    Surface(color=c.gold.copy(alpha=.15f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth(.85f).align(Alignment.End)){
                        Text("Trainer: focus on defense tonight.",color=c.text,modifier=Modifier.padding(9.dp))
                    }
                    Surface(color=c.panel,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth(.82f)){
                        Text("Student: understood, thanks!",color=c.text,modifier=Modifier.padding(9.dp))
                    }
                }
                "HOMEWORK"->{
                    Text("Jab-only shadowboxing",color=c.bright,fontWeight=FontWeight.Black)
                    Text("5 rounds · due Friday",color=c.muted)
                    LinearProgressIndicator(progress={1f},modifier=Modifier.fillMaxWidth())
                    Text("✓ Completed",color=c.bright)
                }
                "NOTES"->{
                    Text("PRIVATE COACH NOTE",color=c.bright,fontWeight=FontWeight.Black)
                    Text("Work on distance management. Reduce sparring intensity this week.",color=c.text)
                    Text("Trainer only",color=c.muted,fontSize=9.sp)
                }
                "ASSESSMENT"->{
                    listOf("Punches" to .84f,"Kicks" to .71f,"Defense" to .75f,"Footwork" to .62f).forEach{(n,v)->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(n,color=c.text);Text((v*100).toInt().toString(),color=c.bright)}
                        LinearProgressIndicator(progress={v},modifier=Modifier.fillMaxWidth())
                    }
                }
                "CLASS"->{
                    Text("Advanced Pads · 19:15",color=c.bright,fontWeight=FontWeight.Black)
                    Text("10 / 12 booked",color=c.muted)
                    LinearProgressIndicator(progress={10f/12f},modifier=Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({},Modifier.weight(1f)){Text("BOOK",fontSize=9.sp)};OutlinedButton({},Modifier.weight(1f)){Text("ATTENDANCE",fontSize=9.sp)}}
                }
                "QR"->{
                    Box(Modifier.size(150.dp).align(Alignment.CenterHorizontally)){
                        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.SpaceEvenly){
                            repeat(7){r->
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                                    repeat(7){q->
                                        Surface(color=if((r*3+q*5)%4<2)c.bright else c.panel,modifier=Modifier.size(14.dp)){}
                                    }
                                }
                            }
                        }
                    }
                    Text("Fundamentals · TODAY",color=c.muted,modifier=Modifier.align(Alignment.CenterHorizontally))
                }
                "SESSION"->{
                    Text("RS Fundamentals Session",color=c.bright,fontWeight=FontWeight.Black)
                    listOf("Warm-up · 05:00","Jab rounds · 03:00","Combination rounds · 03:00","Cooldown · 03:00").forEachIndexed{i,t->
                        Text((i+1).toString()+". "+t,color=if(i==1)c.bright else c.text)
                    }
                }
                "PRIVATE"->{
                    Text("SAT · 12:00 · 45 min",color=c.bright,fontWeight=FontWeight.Black)
                    Text("Student request: Improve low-kick timing",color=c.text)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({},Modifier.weight(1f)){Text("CONFIRM",fontSize=9.sp)};OutlinedButton({},Modifier.weight(1f)){Text("DECLINE",fontSize=9.sp)}}
                }
                "CHALLENGE"->{
                    Text("20 BAG ROUNDS",color=c.bright,fontWeight=FontWeight.Black)
                    Text("14 / 20 rounds",color=c.text)
                    LinearProgressIndicator(progress={.70f},modifier=Modifier.fillMaxWidth())
                    Text("Fight Camp · Week 3 / 8",color=c.muted)
                }
                "CONTENT"->{
                    Text("Roundhouse Kick Mechanics",color=c.bright,fontWeight=FontWeight.Black)
                    Text("TECHNIQUE · PRO",color=c.muted)
                    Text("Hip rotation, pivot, shin line, guard discipline…",color=c.text,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({},Modifier.weight(1f)){Text("OPEN",fontSize=9.sp)};OutlinedButton({},Modifier.weight(1f)){Text("★ SAVE",fontSize=9.sp)}}
                }
                "PROMO"->{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        repeat(3){i->
                            Surface(color=c.gold.copy(alpha=.12f),shape=MaterialTheme.shapes.medium,modifier=Modifier.weight(1f).height(95.dp)){
                                Column(Modifier.padding(7.dp),verticalArrangement=Arrangement.SpaceBetween){
                                    Text(listOf("BOOK","SEMINAR","SHOP")[i],color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                                    Text("CLICK →",color=c.muted,fontSize=8.sp)
                                }
                            }
                        }
                    }
                    Text("Auto-slides horizontally · each card has its own link",color=c.muted,fontSize=9.sp)
                }
                "BOOK"->{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Surface(color=c.gold.copy(alpha=.12f),shape=MaterialTheme.shapes.medium,modifier=Modifier.width(90.dp).height(125.dp)){
                            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("BOOK\nCOVER",color=c.bright,fontWeight=FontWeight.Black)}
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Text("VAN STILTE NAAR STRIJD",color=c.bright,fontWeight=FontWeight.Black)
                            Button({},Modifier.fillMaxWidth()){Text("BUY ON AMAZON",fontSize=9.sp)}
                            OutlinedButton({},Modifier.fillMaxWidth()){Text("READ IN APP",fontSize=9.sp)}
                        }
                    }
                    Text("Reader: swipe pages · zoom · search · next match",color=c.muted,fontSize=9.sp)
                }
                "NOTIFICATION"->{
                    Text("HOLIDAY OPENING HOURS",color=c.bright,fontWeight=FontWeight.Black)
                    Text("Audience · ALL",color=c.muted)
                    Text("Gym closes at 18:00 on Friday.",color=c.text)
                    AssistChip(onClick={},label={Text("UNREAD")})
                }
                "COMMUNITY"->{
                    Text("Fight Camp Group",color=c.bright,fontWeight=FontWeight.Black)
                    Text("12 members",color=c.muted)
                    Surface(color=c.panel,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
                        Text("Hard session today. Week 3 complete!",color=c.text,modifier=Modifier.padding(9.dp))
                    }
                }
                "FINANCE"->{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("RS PRO",color=c.bright,fontWeight=FontWeight.Black);Text("€49.00",color=c.bright,fontWeight=FontWeight.Black)}
                    Text("Invoice #1042 · PAID",color=c.text)
                    LinearProgressIndicator(progress={1f},modifier=Modifier.fillMaxWidth())
                }
                "EVENT"->{
                    Text("Technique Seminar",color=c.bright,fontWeight=FontWeight.Black)
                    Text("24 / 40 RSVP",color=c.text)
                    LinearProgressIndicator(progress={.60f},modifier=Modifier.fillMaxWidth())
                    Button({},Modifier.fillMaxWidth()){Text("RSVP")}
                }
                "SUPPORT"->{
                    Text("Billing question",color=c.bright,fontWeight=FontWeight.Black)
                    Text("Student: My invoice still shows open.",color=c.text)
                    Text("Trainer reply: payment received, status updated.",color=c.muted)
                    AssistChip(onClick={},label={Text("RESOLVED")})
                }
                "ANALYTICS"->{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Box(Modifier.weight(1f)){RsMiniMetricV54(c,"STUDENTS","38")}
                        Box(Modifier.weight(1f)){RsMiniMetricV54(c,"REVENUE","€1,862")}
                    }
                    Text("Class occupancy · 78%",color=c.text)
                    LinearProgressIndicator(progress={.78f},modifier=Modifier.fillMaxWidth())
                }
                "BRANDING"->{
                    Text("LOGIN BACKGROUND",color=c.bright,fontWeight=FontWeight.Black)
                    Surface(color=c.gold.copy(alpha=.12f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().height(80.dp)){
                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("SELECTED MEDIA SLOT",color=c.muted)}
                    }
                    Text("Form opacity · 82%",color=c.text)
                    Slider(value=.82f,onValueChange={},enabled=false)
                }
                "RELEASE"->{
                    listOf("Android acceptance source" to true,"Legal/support info" to true,"Supabase production backend" to false,"Signed Play Store AAB" to false).forEach{(n,ok)->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(n,color=c.text,fontSize=10.sp);Text(if(ok)"✓" else "○",color=if(ok)c.bright else c.muted)}
                    }
                }
                else->Text("RS KICKBOXING",color=c.bright,fontWeight=FontWeight.Black)
            }
        }
    }
}

@Composable
private fun RsMiniMetricV54(c:RsPalette,label:String,value:String){
    Surface(color=c.gold.copy(alpha=.12f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(9.dp)){
            Text(label,color=c.muted,fontSize=8.sp)
            Text(value,color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
fun RsTrainerGuideV54(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    onNavigate:(String)->Unit
){
    var query by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("ALL")}
    var expanded by remember{mutableStateOf<String?>(null)}
    val items=remember{rsGuideItemsV54()}
    val categories=listOf("ALL","START","COACHING","TRAINING","CONTENT","COMMUNITY","BUSINESS","SETTINGS")
    val filtered=items.filter{
        (category=="ALL"||it.category==category) &&
        (query.isBlank() || listOf(it.title,it.purpose,it.trainerDoes,it.studentSees,it.example,it.category)
            .any{s->s.contains(query,true)})
    }

    RsScroll(c,rsGuideUiV54(lang,"title"),rsGuideUiV54(lang,"subtitle")){
        RsPanel(c){
            Text(rsGuideUiV54(lang,"quick"),color=c.bright,fontWeight=FontWeight.Black)
            Text(rsGuideUiV54(lang,"quick_body"),color=c.text)
            HorizontalDivider()
            Text(rsGuideUiV54(lang,"tip")+" · "+rsGuideUiV54(lang,"tip_body"),color=c.muted,fontSize=10.sp)
        }

        OutlinedTextField(
            value=query,
            onValueChange={query=it.take(100)},
            label={Text(rsGuideUiV54(lang,"search"))},
            modifier=Modifier.fillMaxWidth(),
            singleLine=true
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement=Arrangement.spacedBy(6.dp),
            contentPadding=PaddingValues(vertical=2.dp)
        ){
            items(categories.size){index->
                val cat=categories[index]
                FilterChip(
                    selected=category==cat,
                    onClick={category=cat},
                    label={Text(if(cat=="ALL")rsGuideUiV54(lang,"all") else rsGuideCategoryLabelV54(lang,cat),fontSize=9.sp)}
                )
            }
        }

        Text(filtered.size.toString()+" "+rsGuideUiV54(lang,"sections"),color=c.muted,fontSize=10.sp)

        filtered.forEach{item->
            val open=expanded==item.route
            RsPanel(c){
                Text(rsGuideCategoryLabelV54(lang,item.category),color=c.muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Text(item.title,color=c.bright,fontSize=19.sp,fontWeight=FontWeight.Black)
                Text(item.purpose,color=c.text,maxLines=if(open)Int.MAX_VALUE else 3,overflow=TextOverflow.Ellipsis)

                OutlinedButton(
                    onClick={expanded=if(open)null else item.route},
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(open)rsGuideUiV54(lang,"hide") else rsGuideUiV54(lang,"show"))}

                if(open){
                    HorizontalDivider()
                    Text(rsGuideUiV54(lang,"trainer"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    Text(item.trainerDoes,color=c.text)

                    Text(rsGuideUiV54(lang,"student"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    Text(item.studentSees,color=c.text)

                    Text(rsGuideUiV54(lang,"example"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    Text(item.example,color=c.text)

                    Text(rsGuideUiV54(lang,"workflow"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    item.steps.forEachIndexed{i,step->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            Surface(color=c.gold.copy(alpha=.18f),shape=MaterialTheme.shapes.small,modifier=Modifier.size(24.dp)){
                                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text((i+1).toString(),color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)}
                            }
                            Text(step,color=c.text,modifier=Modifier.weight(1f))
                        }
                    }

                    Text(rsGuideUiV54(lang,"visual"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    RsGuideVisualV54(c,item.visual)

                    Button(
                        onClick={onNavigate(item.route)},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsGuideUiV54(lang,"open"))}
                }
            }
        }
    }
}

package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RsStudentGuideItemV81(
    val category:String,
    val title:String,
    val route:String,
    val purpose:String,
    val example:String,
    val steps:List<String>,
    val tip:String,
    val minPlan:String="ALL"
)

private fun rsStudentGuideItemsV81()=listOf(
    RsStudentGuideItemV81("START","My Profile","profile",
        "Manage your RS identity, display name, goals and profile photo. Your photo can also appear next to your name in chats and member areas.",
        "Upload a profile photo, update your training goal and save the profile.",
        listOf("Open My Profile","Upload or change your profile photo","Check your display name","Write your current goal","Save changes"),
        "Use a clear face/profile image so your trainer can recognize you easily."
    ),
    RsStudentGuideItemV81("START","Notifications","notifications",
        "Read club announcements sent by the trainer. Notifications can be for everyone or for your membership level.",
        "The trainer sends a message about a changed class time and it appears in your notification inbox.",
        listOf("Open Notifications","Read unread announcements","Open the message details","The notification is marked as read"),
        "Check Notifications before training days for schedule or club updates."
    ),
    RsStudentGuideItemV81("TRAINING","Classes & Bookings","classes",
        "See available club classes, live capacity and your own booking status.",
        "Book an evening kickboxing class and later cancel it if your plans change.",
        listOf("Open Classes & Events","Choose a class","Check date, time and available places","Tap Book","Return later to cancel if needed"),
        "When a class is full or bookings are closed, the booking button is disabled."
    ),
    RsStudentGuideItemV81("TRAINING","Class Check-In","checkin",
        "Register your attendance by scanning the QR code shown by the trainer.",
        "At reception the trainer displays a class QR; scan it before training.",
        listOf("Open Class Check-In","Allow camera access if asked","Scan the trainer QR","Wait for confirmation","Your attendance is recorded"),
        "Only scan the QR for the class you are actually attending."
    ),
    RsStudentGuideItemV81("TRAINING","Session Player","session",
        "Follow the active trainer-built workout with timed rounds and instructions.",
        "The trainer publishes a session with warm-up, technique rounds and cooldown; you follow it from your phone.",
        listOf("Open Session Player","Read the current block","Start the timer","Follow the instruction","Move to the next block","Restart if needed"),
        "Keep the phone where the timer is visible but does not interfere with training."
    ),
    RsStudentGuideItemV81("TRAINING","AI Technique Coach","voice",
        "Upload a short technique video, receive structured coaching feedback and save the result to your history.",
        "Upload a roundhouse kick clip and review feedback about balance, guard, rotation and recovery.",
        listOf("Open AI Technique Coach","Choose trainer voice/persona","Upload a video up to 20 seconds","Choose the technique","Analyze","Listen/read feedback","Save to history"),
        "Use a clear full-body angle with enough space around you."
    ),
    RsStudentGuideItemV81("TRAINING","Technique Library","techniques",
        "Study kickboxing techniques, key coaching points and drills before practicing.",
        "Open the jab lesson before pad training and review stance, path and recovery.",
        listOf("Open Technique Library","Choose a technique","Read the key points","Review the drill","Practice slowly before increasing speed"),
        "Technique quality comes before speed or power."
    ),
    RsStudentGuideItemV81("TRAINING","Home Training","home_training",
        "Use guided training ideas when you cannot train at the gym.",
        "Choose a technique-focused home session on a day without club training.",
        listOf("Open Home Training","Choose the available workout","Prepare a safe training area","Follow the rounds","Stop if the area is not safe"),
        "Make sure you have enough clear space for kicks and movement."
    ),
    RsStudentGuideItemV81("TRAINING","Workout Generator","workout",
        "Generate a simple workout based on available time and training focus.",
        "Select 30 minutes and Mixed to create a warm-up, technique and conditioning session.",
        listOf("Open Workout Generator","Choose duration","Choose Technique, Conditioning or Mixed","Generate workout","Follow the generated structure"),
        "Generated sessions are a training aid; follow your trainer's instructions when they differ."
    ),
    RsStudentGuideItemV81("COACHING","Private Coach Chat","coachchat",
        "Send private one-to-one messages to your trainer and receive replies across devices.",
        "Ask whether you should join sparring this week and receive a private coach reply.",
        listOf("Open Private Coach Chat","Write your message","Tap Send","Return later to read the reply"),
        "Use Support for administrative problems; use Coach Chat for training/coaching questions."
    ),
    RsStudentGuideItemV81("COACHING","Homework","homework",
        "See training assignments created specifically for you and track completion.",
        "Your trainer assigns five rounds of jab-only shadowboxing due Friday.",
        listOf("Open Homework","Read the assignment","Check the due label","Complete the work","Mark completed","Reopen if you marked it too early"),
        "Your trainer can see the completion state."
    ),
    RsStudentGuideItemV81("COACHING","Private Lessons","private_lessons",
        "Request one-to-one coaching from available trainer time slots.",
        "Request Saturday at 12:00 and add a note that you want to work on defense.",
        listOf("Open Private Lessons","Choose an available slot","Add a short note","Send request","Wait for Confirmed or Declined status","Cancel if necessary"),
        "A request is not final until the trainer confirms it."
    ),
    RsStudentGuideItemV81("PERFORMANCE","Progress","progress",
        "See your latest trainer assessment and your development history.",
        "Your Defense score improves from 67 to 75 after several weeks of focused work.",
        listOf("Open Progress","Review the latest six skill scores","Read trainer feedback","Compare with earlier assessments","Use the weak areas as training focus"),
        "Progress is more useful when you compare several assessments over time."
    ),
    RsStudentGuideItemV81("PERFORMANCE","Challenges","challenges",
        "Work toward measurable goals assigned by the trainer.",
        "Complete a 20 bag-round challenge and update your progress as you train.",
        listOf("Open Challenges","Read the target and unit","Perform the training","Update your progress","Complete the target"),
        "Only record work you actually completed."
    ),
    RsStudentGuideItemV81("PERFORMANCE","Badges & Achievements","badges",
        "View milestones earned from your real training activity and progress.",
        "A consistency or challenge badge unlocks after the required activity is completed.",
        listOf("Open Badges","Review unlocked badges","Check progress on locked achievements","Keep training toward the next milestone"),
        "Badges are motivation tools; technique and consistent safe training remain the priority."
    ),
    RsStudentGuideItemV81("PERFORMANCE","Fight Camp","fightcamp",
        "Follow the structured Fight Camp plan assigned and managed by your trainer.",
        "Your trainer sets week 3 of an eight-week camp with defense as the current focus.",
        listOf("Open Fight Camp","Check current week","Read the current focus","Follow trainer instructions","Return as the trainer updates the plan"),
        "Fight Camp settings are managed by your trainer."
    ),
    RsStudentGuideItemV81("PERFORMANCE","Technique Comparison","compare",
        "Compare important differences between techniques side by side.",
        "Compare jab and cross mechanics before combination practice.",
        listOf("Open Technique Comparison","Choose/read the comparison","Study stance, rotation and recovery differences","Practice each technique separately","Combine them after"),
        "Use comparisons to understand differences, not to rush technique."
    ),
    RsStudentGuideItemV81("PERFORMANCE","Training History","history",
        "Reopen recently used training content and review your learning activity.",
        "Return to a lesson you opened earlier without searching for it again.",
        listOf("Open Training History","Browse recent items","Choose an item","Continue learning"),
        "Your cloud history follows your account across supported devices."
    ),
    RsStudentGuideItemV81("LIBRARY","RS Academy","academy",
        "Follow structured RS learning tracks and lessons.",
        "Open an Academy track and continue from your current lesson progress.",
        listOf("Open RS Academy","Choose a track","Read/watch the lesson","Complete the lesson","Continue to the next available part"),
        "Complete lessons in order when the track is designed progressively."
    ),
    RsStudentGuideItemV81("LIBRARY","Knowledge Vault","vault",
        "Read trainer-published training articles that are available to your membership.",
        "Open a Defense Essentials article and save it for later.",
        listOf("Open Knowledge Vault","Browse unlocked content","Open a lesson","Read it","Save to Favorites if useful"),
        "Content that is not included in your access level is not shown as available content."
    ),
    RsStudentGuideItemV81("LIBRARY","Saved & Favorites","favorites",
        "Keep useful training lessons in one quick-access list.",
        "Save a counter-fighting lesson so you can reopen it before training.",
        listOf("Open a Knowledge Vault lesson","Tap Save","Open Saved & Favorites later","Open the saved lesson","Remove it when no longer needed"),
        "Favorites are linked to your account in the cloud."
    ),
    RsStudentGuideItemV81("LIBRARY","Training Media","media",
        "Watch trainer-published videos and view training images permitted by your membership.",
        "Open a padwork demonstration published for your current plan.",
        listOf("Open Training Media","Choose a published item","Read its description","Open the video/image","Use player controls when available"),
        "Only media included in your membership access is available to you."
    ),
    RsStudentGuideItemV81("LIBRARY","Trainer Book","book",
        "Read the trainer book preview or full book when your account has access.",
        "Open the in-app book reader, swipe pages and search for a topic.",
        listOf("Open Trainer Book","Choose available preview/full version","Swipe between pages","Pinch to zoom","Use search when needed"),
        "Full-book access can depend on your plan or a trainer-granted private permission."
    ),
    RsStudentGuideItemV81("LIBRARY","Search","search",
        "Search your unlocked RS training library by technique, lesson or topic.",
        "Search for 'defense' to find matching unlocked content.",
        listOf("Open Search","Type a keyword","Review matching content","Open the result"),
        "Search only returns content your account is allowed to access."
    ),
    RsStudentGuideItemV81("COMMUNITY","RS Events","events",
        "See club events and manage your own RSVP.",
        "Register for a technique seminar and cancel later if you cannot attend.",
        listOf("Open RS Events","Choose an event","Check details and capacity","RSVP","Cancel your RSVP if needed"),
        "Normal classes and special events use separate booking areas."
    ),
    RsStudentGuideItemV81("COMMUNITY","Community","community",
        "Read club posts and, when enabled by the trainer, publish your own updates.",
        "Share a training achievement with the club community.",
        listOf("Open Community","Read active posts","Create a post when posting is enabled","Publish","Delete your own post if needed"),
        "Keep posts respectful and relevant to the club."
    ),
    RsStudentGuideItemV81("COMMUNITY","Groups","groups",
        "Join active focused member groups such as Fight Camp or training teams.",
        "Join a Fight Camp group to follow focused group activity.",
        listOf("Open Groups","Review active groups","Join a group","Open its member area","Leave when no longer relevant"),
        "Only join groups relevant to your training."
    ),
    RsStudentGuideItemV81("MEMBER","Club Documents","documents",
        "Read club documents, rules, waivers and guides available to your membership.",
        "Open gym rules or a competition information document.",
        listOf("Open Club Documents","Choose an available document","Read the information","Return to the document later if needed"),
        "Some documents may only be available to specific membership levels."
    ),
    RsStudentGuideItemV81("MEMBER","Support","support",
        "Send private administrative or app-related requests to the club.",
        "Open a ticket about a payment question or an app account problem.",
        listOf("Open Support","Create a request","Explain the issue clearly","Send","Return to read the response/status"),
        "For technique questions, use Private Coach Chat instead."
    ),
    RsStudentGuideItemV81("MEMBER","Referrals","referrals",
        "Share your referral option when referral sharing is enabled.",
        "Invite a friend using your available referral sharing flow.",
        listOf("Open Referrals","Review your referral information","Use Share when available","Return later to review referral history"),
        "Referral sharing can be temporarily switched off by the trainer."
    ),
    RsStudentGuideItemV81("MEMBER","Promotions","promotions",
        "See trainer/club promotional cards, offers and external links.",
        "Tap a seminar promotion to open the registration page.",
        listOf("Open Promotions","Browse the promotion cards","Tap an item that interests you","Follow the linked page if appropriate"),
        "External links leave the RS KICKBOX app."
    ),
    RsStudentGuideItemV81("ACCOUNT","Membership & Payments","finance",
        "See your active membership plan, billing state and your invoice/payment history.",
        "Check whether your current monthly invoice is paid or pending.",
        listOf("Open Membership & Payments","Check your plan","Review payment/billing status","Review invoice history","Use Support if something looks incorrect"),
        "Payment records are account-specific."
    ),
    RsStudentGuideItemV81("ACCOUNT","My RS Music","music",
        "Use your available training music/player features while working out.",
        "Open your saved training playlist before starting a home workout.",
        listOf("Open My RS Music","Choose available music/playlist","Start playback","Return to training"),
        "Keep volume low enough to hear coaching and your surroundings."
    ),
    RsStudentGuideItemV81("ACCOUNT","Settings & Privacy","settings",
        "Manage personal app/privacy options and account-related controls.",
        "Review privacy options or sign out of the app.",
        listOf("Open Settings & Privacy","Review account/privacy options","Change available preferences","Use sign-out/account controls when needed"),
        "Do not share your account password or invitation QR."
    )
)

private fun rsStudentGuideUiV81(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "Student App Guide",
        "subtitle" to "Learn what every available RS KICKBOX section does and how to use it.",
        "quick" to "QUICK START",
        "quick_body" to "Start with My Profile → Classes → Notifications → Homework → Progress. Then explore coaching, training and library tools.",
        "access" to "Your guide automatically follows your current app access. A section hidden or locked by the trainer will only appear here after it is unlocked.",
        "search" to "Search your app guide…",
        "all" to "ALL",
        "show" to "Show how it works",
        "hide" to "Hide guide",
        "purpose" to "WHAT THIS SECTION DOES",
        "example" to "EXAMPLE",
        "steps" to "HOW TO USE IT",
        "tip" to "GOOD TO KNOW",
        "open" to "Open this section",
        "sections" to "available sections"
    )
    val nl=en+mapOf(
        "title" to "App Gids voor Leerlingen",
        "subtitle" to "Leer wat elke beschikbare RS KICKBOX-sectie doet en hoe je die gebruikt.",
        "quick" to "SNEL STARTEN",
        "quick_body" to "Begin met Mijn Profiel → Lessen → Meldingen → Huiswerk → Voortgang. Ontdek daarna coaching, training en bibliotheekfuncties.",
        "access" to "Deze gids volgt automatisch jouw huidige app-toegang. Een door de trainer geblokkeerde sectie verschijnt hier pas nadat die weer is vrijgegeven.",
        "search" to "Zoek in de app-gids…","all" to "ALLES","show" to "Laat zien hoe het werkt","hide" to "Verberg uitleg",
        "purpose" to "WAT DEZE SECTIE DOET","example" to "VOORBEELD","steps" to "HOE JE HET GEBRUIKT","tip" to "GOED OM TE WETEN","open" to "Open deze sectie","sections" to "beschikbare secties"
    )
    val pt=en+mapOf(
        "title" to "Guia da App para Alunos",
        "subtitle" to "Aprende para que serve cada secção RS KICKBOX disponível e como a usar.",
        "quick" to "INÍCIO RÁPIDO",
        "quick_body" to "Começa por Perfil → Aulas → Notificações → Tarefas → Progresso. Depois explora coaching, treino e biblioteca.",
        "access" to "O guia acompanha automaticamente o teu acesso atual. Uma secção bloqueada pelo treinador só aparece aqui quando for desbloqueada.",
        "search" to "Pesquisar no guia…","all" to "TODAS","show" to "Ver como funciona","hide" to "Ocultar guia",
        "purpose" to "O QUE ESTA SECÇÃO FAZ","example" to "EXEMPLO","steps" to "COMO USAR","tip" to "BOM SABER","open" to "Abrir esta secção","sections" to "secções disponíveis"
    )
    val es=en+mapOf("title" to "Guía de la App para Alumnos","subtitle" to "Aprende qué hace cada sección disponible de RS KICKBOX y cómo usarla.","access" to "La guía sigue automáticamente tu acceso actual. Una sección bloqueada por el entrenador solo aparece cuando se desbloquea.","search" to "Buscar en la guía…","all" to "TODAS","show" to "Ver cómo funciona","hide" to "Ocultar guía","purpose" to "QUÉ HACE ESTA SECCIÓN","example" to "EJEMPLO","steps" to "CÓMO USARLA","tip" to "BUENO SABER","open" to "Abrir esta sección","sections" to "secciones disponibles")
    val fr=en+mapOf("title" to "Guide App Élève","subtitle" to "Découvre le rôle de chaque section RS KICKBOX disponible et comment l’utiliser.","access" to "Le guide suit automatiquement tes accès actuels. Une section bloquée par l’entraîneur apparaît seulement après son déblocage.","search" to "Rechercher dans le guide…","all" to "TOUT","show" to "Voir comment ça marche","hide" to "Masquer","purpose" to "RÔLE DE CETTE SECTION","example" to "EXEMPLE","steps" to "COMMENT L’UTILISER","tip" to "BON À SAVOIR","open" to "Ouvrir cette section","sections" to "sections disponibles")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;else->en}
    return pack[key]?:en[key]?:key
}

private fun rsStudentGuideCategoryV81(lang:RsLang,cat:String):String=when(cat){
    "START"->when(lang.code){"nl"->"START";"pt"->"INÍCIO";else->"START"}
    "TRAINING"->when(lang.code){"nl"->"TRAINING";"pt"->"TREINO";"es"->"ENTRENAMIENTO";"fr"->"ENTRAÎNEMENT";else->"TRAINING"}
    "COACHING"->"COACHING"
    "PERFORMANCE"->when(lang.code){"pt"->"DESEMPENHO";"es"->"RENDIMIENTO";"fr"->"PERFORMANCE";else->"PERFORMANCE"}
    "LIBRARY"->when(lang.code){"nl"->"BIBLIOTHEEK";"pt"->"BIBLIOTECA";"es"->"BIBLIOTECA";"fr"->"BIBLIOTHÈQUE";else->"LIBRARY"}
    "COMMUNITY"->when(lang.code){"nl"->"COMMUNITY";"pt"->"COMUNIDADE";"es"->"COMUNIDAD";"fr"->"COMMUNAUTÉ";else->"COMMUNITY"}
    "MEMBER"->when(lang.code){"nl"->"LEDENSERVICE";"pt"->"MEMBRO";"es"->"MIEMBRO";"fr"->"MEMBRE";else->"MEMBER"}
    "ACCOUNT"->when(lang.code){"nl"->"ACCOUNT";"pt"->"CONTA";"es"->"CUENTA";"fr"->"COMPTE";else->"ACCOUNT"}
    else->cat
}

fun rsStudentFeatureEnabledV81(store:RsStore,route:String):Boolean =
    rsStudentRouteEnabledV82(store,route)

private fun rsPlanRankV81(plan:String)=when(plan.uppercase()){
    "BASIC"->1
    "PRO"->2
    "ELITE"->3
    else->1
}

private fun rsStudentGuideVisibleV81(store:RsStore,item:RsStudentGuideItemV81):Boolean{
    if(!rsStudentFeatureEnabledV81(store,item.route))return false
    if(item.minPlan=="ALL")return true
    val plan=store.s("session_plan","BASIC")
    return rsPlanRankV81(plan)>=rsPlanRankV81(item.minPlan)
}

@Composable
fun RsStudentGuideV81(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    onNavigate:(String)->Unit
){
    var query by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("ALL")}
    var expanded by remember{mutableStateOf<String?>(null)}

    val visibleItems=rsStudentGuideItemsV81().filter{rsStudentGuideVisibleV81(store,it)}
    val categories=listOf("ALL","START","TRAINING","COACHING","PERFORMANCE","LIBRARY","COMMUNITY","MEMBER","ACCOUNT")
        .filter{it=="ALL"||visibleItems.any{item->item.category==it}}

    val filtered=visibleItems.filter{
        (category=="ALL"||it.category==category) &&
        (query.isBlank()||listOf(it.title,it.purpose,it.example,it.tip,it.category).any{s->s.contains(query,true)})
    }

    RsScroll(c,rsStudentGuideUiV81(lang,"title"),rsStudentGuideUiV81(lang,"subtitle")){
        RsPanel(c){
            Text(rsStudentGuideUiV81(lang,"quick"),color=c.bright,fontWeight=FontWeight.Black)
            Text(rsStudentGuideUiV81(lang,"quick_body"),color=c.text)
            HorizontalDivider()
            Text(rsStudentGuideUiV81(lang,"access"),color=c.muted,fontSize=10.sp)
        }

        OutlinedTextField(
            value=query,
            onValueChange={query=it.take(100)},
            label={Text(rsStudentGuideUiV81(lang,"search"))},
            modifier=Modifier.fillMaxWidth(),
            singleLine=true
        )

        LazyRow(
            horizontalArrangement=Arrangement.spacedBy(6.dp),
            contentPadding=PaddingValues(vertical=2.dp)
        ){
            items(categories.size){index->
                val cat=categories[index]
                FilterChip(
                    selected=category==cat,
                    onClick={category=cat},
                    label={Text(if(cat=="ALL")rsStudentGuideUiV81(lang,"all") else rsStudentGuideCategoryV81(lang,cat),fontSize=9.sp)}
                )
            }
        }

        Text(filtered.size.toString()+" "+rsStudentGuideUiV81(lang,"sections"),color=c.muted,fontSize=10.sp)

        filtered.forEach{item->
            val open=expanded==item.route
            RsPanel(c){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(rsStudentGuideCategoryV81(lang,item.category),color=c.muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                        Text(item.title,color=c.bright,fontSize=19.sp,fontWeight=FontWeight.Black)
                    }
                    Surface(shape=MaterialTheme.shapes.medium,color=c.gold.copy(alpha=.16f)){
                        Text("?",color=c.bright,fontSize=16.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(horizontal=11.dp,vertical=6.dp))
                    }
                }
                Text(item.purpose,color=c.text,maxLines=if(open)Int.MAX_VALUE else 3,overflow=TextOverflow.Ellipsis)

                OutlinedButton(
                    onClick={expanded=if(open)null else item.route},
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(open)rsStudentGuideUiV81(lang,"hide") else rsStudentGuideUiV81(lang,"show"))}

                if(open){
                    HorizontalDivider()
                    Text(rsStudentGuideUiV81(lang,"purpose"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    Text(item.purpose,color=c.text)

                    Text(rsStudentGuideUiV81(lang,"example"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    Text(item.example,color=c.text)

                    Text(rsStudentGuideUiV81(lang,"steps"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                    item.steps.forEachIndexed{i,step->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Top){
                            Surface(color=c.gold.copy(alpha=.18f),shape=MaterialTheme.shapes.small,modifier=Modifier.size(25.dp)){
                                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                                    Text((i+1).toString(),color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)
                                }
                            }
                            Text(step,color=c.text,modifier=Modifier.weight(1f))
                        }
                    }

                    RsPanel(c){
                        Text(rsStudentGuideUiV81(lang,"tip"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                        Text(item.tip,color=c.muted,fontSize=11.sp)
                    }

                    Button(
                        onClick={onNavigate(item.route)},
                        enabled=rsStudentFeatureEnabledV81(store,item.route),
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsStudentGuideUiV81(lang,"open"))}
                }
            }
        }
    }
}

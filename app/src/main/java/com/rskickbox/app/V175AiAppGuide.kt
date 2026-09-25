package com.rskickbox.app

data class RsAiAppGuideResultV175(
    val route:String,
    val title:String,
    val purpose:String,
    val steps:List<String>,
    val tip:String
)

private val rsAiAppRoutesV175=listOf(
    "student_guide","guide","voice","coachchat","music","music_admin","session","academy","techniques","home_training",
    "classes","events","checkin","homework","private_lessons","notifications","promotions","documents","support",
    "referrals","progress","challenges","badges","fightcamp","compare","vault","finance","book","profile","settings",
    "themes","backgrounds","branding","intro_settings","landing_admin","content","homework_admin","session_builder",
    "members","access","plans_admin","progress_admin","assessments","challenge_admin","fightcamp_admin","attendance",
    "events_admin","schedule","payments","invoices","analytics","release","privacy_admin","community","groups"
)

private fun rsAiGuideSpecialStepsV175(route:String,role:RsRole):List<String>?=when(route){
    "coachchat"->listOf(
        "Open RS Chat from the first dashboard section.",
        "Choose Private, Groups, Community, Support or AI Coach.",
        "Use + for supported media and use the call buttons for audio/video when available.",
        "Swipe left to right to open the RS Chat menu; swipe right to left to close the menu or leave chat when it is closed."
    )
    "music","music_admin"->listOf(
        "Open RS Music Pro from the top dashboard section.",
        "Use IMPORT directly from the main player to add audio from your device.",
        "Open LIBRARY for the sliding music panel with Library, Playlists, Favorites and Recent sections.",
        "Create and manage named playlists inside the sliding library instead of using a separate basic playlist form.",
        "Use search, favorites, shuffle, repeat-one/repeat-all, seek, ±10 second jumps, playback speed, volume and persistent background playback.",
        "Choose a visual effect preset such as Neon, Bass, Arena or Studio for the spatial 3D-style music visualizer.",
        "Swipe the music library from left to right to close it.",
        "You can also ask Sofia or Marcus to play, pause, stop, skip, open RS Music Pro or import music."
    )
    "student_guide","guide"->listOf(
        "Open App Guide from the first dashboard position.",
        "Choose the feature/category you want to learn.",
        "Open the RS AI Voice Commands section for spoken navigation examples.",
        "Read its purpose, example, steps and usage tip.",
        "You can also ask RS AI about any app feature and it will explain the feature and show a visual example when available."
    )
    "voice"->listOf(
        "Open RS AI Voice Commands in App Guide.",
        "Say commands naturally, for example: Open music, Open homework, Go to dashboard, Open settings, Change to Sofia, Change to Marcus, Another voice, Call Marcio, Video call Simone, Group video with Simone and Marcio, Minimize app, Close app, Close AI, or Show commands.",
        "RS AI turns supported commands directly into navigation or app actions instead of only replying with text.",
        "Swipe right to left on the AI page to leave AI and return to the correct dashboard.",
        "When Sofia is selected, voice-style changes stay female; when Marcus is selected, voice-style changes stay male. The selected language is preserved.",
        "If Android blocks automatic foreground launch while the app task is closed, use the silent RS heard you notification to hand off into the requested page."
    )
    "themes"->listOf(
        "Open Visual Theme Studio.",
        "Swipe horizontally through the available complete themes.",
        "Tap Select Style on the theme you want.",
        "Review the dashboard, chat, buttons and panels because the theme changes structure as well as colors."
    )
    "backgrounds"->listOf(
        "Open Visual Asset Studio.",
        "Choose the exact app page or dashboard tile.",
        "Upload/select the visual and adjust its placement/opacity when offered.",
        "Review the result on a real phone before keeping it."
    )
    "members"->listOf(
        "Open Student Manager.",
        "Create or select a student profile.",
        "Set membership/access and generate the private invite/QR when needed.",
        "Review status, storage and account access before sharing the invitation."
    )
    "homework","homework_admin"->listOf(
        if(role==RsRole.TRAINER)"Open Homework Manager and choose a student." else "Open Homework from your dashboard.",
        if(role==RsRole.TRAINER)"Create ordered exercises, instructions and trainer media references." else "Read each assigned exercise, media example, sets/reps/time and due information.",
        if(role==RsRole.TRAINER)"Publish/assign the plan and review completion." else "Mark exercise steps complete as you perform them.",
        "Use trainer-approved media examples rather than duplicating the same files."
    )
    "classes"->listOf(
        "Open Classes & Training.",
        if(role==RsRole.TRAINER)"Create/update class details, capacity and schedule." else "Choose a class and review date, time and live capacity.",
        if(role==RsRole.TRAINER)"Review attendance/bookings." else "Book or cancel when the controls are available."
    )
    "checkin","attendance"->listOf(
        if(role==RsRole.TRAINER)"Open Attendance Center and select/show the class check-in flow." else "Open Class Check-In.",
        if(role==RsRole.TRAINER)"Use the roster/QR tools for the correct class." else "Allow camera access and scan the trainer QR.",
        "Wait for confirmation before leaving the page."
    )
    "support"->listOf(
        "Open Support.",
        "Create or open the relevant private request.",
        "Describe the issue clearly and attach supported information when needed.",
        "Return to the ticket to follow the reply/status."
    )
    else->null
}

private fun rsAiLocalizedGenericStepsV179(title:String,lang:RsLang):List<String> = when(lang.code){
    "nl"->listOf(
        "Open $title via het dashboard of het zijmenu.",
        "Bekijk de beschikbare knoppen, instellingen en informatie op de pagina.",
        "Kies de actie die past bij wat je wilt doen.",
        "Controleer het resultaat of de status voordat je de pagina verlaat."
    )
    "pt"->listOf(
        "Abre $title no painel principal ou no menu lateral.",
        "Revê os controlos, definições e informações disponíveis nesta página.",
        "Escolhe a ação que corresponde ao que pretendes fazer.",
        "Confirma o resultado ou o estado antes de sair da página."
    )
    "es"->listOf(
        "Abre $title desde el panel principal o el menú lateral.",
        "Revisa los controles, ajustes y la información disponibles en esta página.",
        "Elige la acción que corresponda a lo que quieres hacer.",
        "Comprueba el resultado o el estado antes de salir de la página."
    )
    "fr"->listOf(
        "Ouvre $title depuis le tableau de bord ou le menu latéral.",
        "Consulte les commandes, réglages et informations disponibles sur cette page.",
        "Choisis l’action correspondant à ce que tu veux faire.",
        "Vérifie le résultat ou l’état avant de quitter la page."
    )
    "de"->listOf(
        "Öffne $title über das Dashboard oder das Seitenmenü.",
        "Prüfe die verfügbaren Steuerelemente, Einstellungen und Informationen auf dieser Seite.",
        "Wähle die Aktion aus, die zu deinem Ziel passt.",
        "Prüfe das Ergebnis oder den Status, bevor du die Seite verlässt."
    )
    "it"->listOf(
        "Apri $title dalla dashboard o dal menu laterale.",
        "Controlla i comandi, le impostazioni e le informazioni disponibili in questa pagina.",
        "Scegli l’azione che corrisponde a ciò che vuoi fare.",
        "Controlla il risultato o lo stato prima di lasciare la pagina."
    )
    "pl"->listOf(
        "Otwórz $title z pulpitu lub menu bocznego.",
        "Sprawdź dostępne elementy sterujące, ustawienia i informacje na tej stronie.",
        "Wybierz działanie odpowiadające temu, co chcesz zrobić.",
        "Sprawdź wynik lub stan przed opuszczeniem strony."
    )
    "tr"->listOf(
        "$title sayfasını ana panelden veya yan menüden aç.",
        "Bu sayfadaki kullanılabilir kontrolleri, ayarları ve bilgileri incele.",
        "Yapmak istediğin işleme uygun seçeneği seç.",
        "Sayfadan ayrılmadan önce sonucu veya durumu kontrol et."
    )
    else->listOf(
        "Open $title from the dashboard or side menu.",
        "Review the available controls, settings and information on this page.",
        "Choose the action that matches what you want to do.",
        "Check the result or status before leaving the page."
    )
}

private fun rsAiLocalizedGuideStepsV179(route:String,role:RsRole,lang:RsLang,title:String):List<String>{
    if(lang.code=="en"){
        return rsAiGuideSpecialStepsV175(route,role)?:rsAiLocalizedGenericStepsV179(title,lang)
    }
    return rsAiLocalizedGenericStepsV179(title,lang)
}

fun rsAiAppGuideMatchV175(question:String,lang:RsLang,role:RsRole):RsAiAppGuideResultV175?{
    val q=question.trim().lowercase()
    if(q.isBlank())return null
    val wantsGuide=listOf(
        "how","what is","what does","explain","help","guide","how do","how can","what can",
        "waarvoor","hoe","wat is","uitleg","ajuda","como","qué es","como funciona","comment","qu'est"
    ).any{q.contains(it)}
    if(!wantsGuide)return null

    val route=rsAiAppRoutesV175
        .map{r->
            val title=rsRouteTitle(lang,r,r.replace('_',' '))
            val tokens=(title.lowercase().split(" ","&","·","/")+r.replace('_',' ').split(" "))
                .filter{it.length>=3}
            r to tokens.count{q.contains(it)}
        }
        .filter{it.second>0}
        .maxByOrNull{it.second}
        ?.first
        ?:return null

    val title=rsRouteTitle(lang,route,route.replace('_',' ').replaceFirstChar{it.uppercase()})
    val hint=rsRouteHint(lang,route,title)
    val steps=rsAiLocalizedGuideStepsV179(route,role,lang,title)
    val purpose=when(lang.code){
        "nl"->"$title helpt je met: $hint."
        "pt"->"$title ajuda-te com: $hint."
        "es"->"$title te ayuda con: $hint."
        "fr"->"$title t'aide avec : $hint."
        "de"->"$title hilft dir bei: $hint."
        "it"->"$title ti aiuta con: $hint."
        "pl"->"$title pomaga w: $hint."
        "tr"->"$title şu konularda yardımcı olur: $hint."
        else->"$title is used for: $hint."
    }
    val tip=when(lang.code){
        "nl"->"Je kunt ook zeggen: ‘open $title’ en RS AI brengt je naar die pagina."
        "pt"->"Também podes dizer: ‘open $title’ e a IA RS abre essa página."
        "es"->"También puedes decir: ‘open $title’ y RS AI abrirá esa página."
        "fr"->"Tu peux aussi dire : ‘ouvre $title’ et RS AI ouvrira cette page."
        "de"->"Du kannst auch sagen: „Öffne $title“, und RS AI öffnet diese Seite."
        "it"->"Puoi anche dire: «apri $title» e RS AI aprirà questa pagina."
        "pl"->"Możesz też powiedzieć: „otwórz $title”, a RS AI otworzy tę stronę."
        "tr"->"“$title sayfasını aç” diyebilirsin; RS AI seni doğrudan bu sayfaya götürür."
        else->"You can also say “open $title” and RS AI can take you directly to that page."
    }
    return RsAiAppGuideResultV175(route,title,purpose,steps,tip)
}

fun rsAiAppGuideTextV175(result:RsAiAppGuideResultV175,lang:RsLang):String{
    val steps=result.steps.mapIndexed{i,s->(i+1).toString()+". "+s}.joinToString("\n")
    return when(lang.code){
        "nl"->result.title+"\n\nWaarvoor: "+result.purpose+"\n\nZo gebruik je het:\n"+steps+"\n\nTip: "+result.tip
        "pt"->result.title+"\n\nObjetivo: "+result.purpose+"\n\nComo usar:\n"+steps+"\n\nDica: "+result.tip
        "es"->result.title+"\n\nObjetivo: "+result.purpose+"\n\nCómo usarlo:\n"+steps+"\n\nConsejo: "+result.tip
        "fr"->result.title+"\n\nUtilité : "+result.purpose+"\n\nComment l'utiliser :\n"+steps+"\n\nConseil : "+result.tip
        "de"->result.title+"\n\nZweck: "+result.purpose+"\n\nSo verwendest du es:\n"+steps+"\n\nTipp: "+result.tip
        "it"->result.title+"\n\nObiettivo: "+result.purpose+"\n\nCome usarlo:\n"+steps+"\n\nSuggerimento: "+result.tip
        "pl"->result.title+"\n\nCel: "+result.purpose+"\n\nJak używać:\n"+steps+"\n\nWskazówka: "+result.tip
        "tr"->result.title+"\n\nAmaç: "+result.purpose+"\n\nNasıl kullanılır:\n"+steps+"\n\nİpucu: "+result.tip
        else->result.title+"\n\nPurpose: "+result.purpose+"\n\nHow to use it:\n"+steps+"\n\nTip: "+result.tip
    }
}

fun rsAiAppKnowledgeSummaryV175(lang:RsLang,role:RsRole):String =
    rsAiAppRoutesV175
        .filterNot{role==RsRole.STUDENT && it.endsWith("_admin")}
        .joinToString("\n"){route->
            val title=rsRouteTitle(lang,route,route.replace('_',' '))
            val hint=rsRouteHint(lang,route,title)
            route+" | "+title+" | "+hint
        }


fun rsAiAppGuideForRouteV177(route:String,lang:RsLang,role:RsRole):RsAiAppGuideResultV175?{
    if(route.isBlank() || route !in rsAiAppRoutesV175)return null
    val title=rsRouteTitle(lang,route,route.replace('_',' ').replaceFirstChar{it.uppercase()})
    val hint=rsRouteHint(lang,route,title)
    val steps=rsAiLocalizedGuideStepsV179(route,role,lang,title)
    val purpose=when(lang.code){
        "nl"->"$title helpt je met: $hint."
        "pt"->"$title ajuda-te com: $hint."
        "es"->"$title te ayuda con: $hint."
        "fr"->"$title t'aide avec : $hint."
        "de"->"$title hilft dir bei: $hint."
        else->"$title is used for: $hint."
    }
    val tip=when(lang.code){
        "nl"->"Vraag RS AI gerust wat je op deze pagina kunt doen of zeg welke actie je wilt uitvoeren."
        "pt"->"Podes perguntar à IA RS o que podes fazer nesta página ou dizer a ação que queres executar."
        "es"->"Puedes preguntar a RS AI qué puedes hacer en esta página o decir la acción que quieres realizar."
        "fr"->"Tu peux demander à RS AI ce que tu peux faire sur cette page ou lui dire l’action que tu veux effectuer."
        "de"->"Du kannst RS AI fragen, was du auf dieser Seite tun kannst, oder die gewünschte Aktion nennen."
        "it"->"Puoi chiedere a RS AI cosa puoi fare in questa pagina o indicare l’azione che vuoi eseguire."
        "pl"->"Możesz zapytać RS AI, co można zrobić na tej stronie, albo podać działanie, które chcesz wykonać."
        "tr"->"RS AI’a bu sayfada neler yapabileceğini sorabilir veya yapmak istediğin işlemi söyleyebilirsin."
        else->"Ask RS AI what you can do on this page, or tell it the action you want to perform."
    }
    return RsAiAppGuideResultV175(route,title,purpose,steps,tip)
}

fun rsAiGenericCurrentPageHelpV177(text:String):Boolean{
    val q=text.trim().lowercase()
    return listOf(
        "what can i do here","how do i use this page","help me with this page","explain this page",
        "what is this page","help here","what can i do on this page",
        "wat kan ik hier","hoe gebruik ik deze pagina","leg deze pagina uit",
        "o que posso fazer aqui","como uso esta página",
        "qué puedo hacer aquí","cómo uso esta página",
        "que puis-je faire ici","comment utiliser cette page"
    ).any{q.contains(it)}
}

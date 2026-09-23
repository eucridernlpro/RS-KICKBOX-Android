package com.rskickbox.app

data class RsAiAppGuideResultV175(
    val route:String,
    val title:String,
    val purpose:String,
    val steps:List<String>,
    val tip:String
)

private val rsAiAppRoutesV175=listOf(
    "student_guide","guide","coachchat","music","music_admin","session","academy","techniques","home_training",
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
        "Open RS Music from the top dashboard section.",
        "Import audio from your device and keep it in the RS library.",
        "Use search, playlists, shuffle, repeat, seek, volume and background playback.",
        "You can also ask Sofia or Marcus to play, pause, stop, skip or open RS Music."
    )
    "student_guide","guide"->listOf(
        "Open App Guide from the first dashboard position.",
        "Choose the feature/category you want to learn.",
        "Read its purpose, example, steps and usage tip.",
        "You can also ask RS AI about any app feature and it will explain the feature and show a visual example when available."
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
    val steps=rsAiGuideSpecialStepsV175(route,role)?:listOf(
        "Open $title from the dashboard or side menu.",
        "Review the available controls and information on the page.",
        "Choose the action that matches what you want to do.",
        "Check the result/status before leaving the page."
    )
    val purpose=when(lang.code){
        "nl"->"$title helpt je met: $hint."
        "pt"->"$title ajuda-te com: $hint."
        "es"->"$title te ayuda con: $hint."
        "fr"->"$title t'aide avec : $hint."
        "de"->"$title hilft dir bei: $hint."
        else->"$title is used for: $hint."
    }
    val tip=when(lang.code){
        "nl"->"Je kunt ook zeggen: ‘open $title’ en RS AI brengt je naar die pagina."
        "pt"->"Também podes dizer: ‘open $title’ e a IA RS abre essa página."
        "es"->"También puedes decir: ‘open $title’ y RS AI abrirá esa página."
        "fr"->"Tu peux aussi dire : ‘open $title’ et RS AI ouvrira cette page."
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
    val steps=rsAiGuideSpecialStepsV175(route,role)?:listOf(
        "Open $title from the dashboard or side menu.",
        "Review the available controls and information on the page.",
        "Choose the action that matches what you want to do.",
        "Check the result/status before leaving the page."
    )
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

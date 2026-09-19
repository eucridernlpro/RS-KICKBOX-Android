package com.rskickbox.app

fun rsCommonT95(lang:RsLang,key:String):String{
    val en=mapOf(
        "restoring_session" to "Restoring secure RS KICKBOXING session…",
        "module_ready" to "Module ready",
        "module_ready_sub" to "This module uses the current RS KICKBOXING visual system.",
        "maintenance_default" to "RS KICKBOXING maintenance notice: some services may be temporarily limited.",
        "hide" to "HIDE","none" to "Nothing here yet."
    )
    val nl=en+mapOf(
        "restoring_session" to "Beveiligde RS KICKBOXING-sessie herstellen…",
        "module_ready" to "Module gereed",
        "module_ready_sub" to "Deze module gebruikt het huidige RS KICKBOXING-ontwerpsysteem.",
        "maintenance_default" to "RS KICKBOXING onderhoudsmelding: sommige diensten kunnen tijdelijk beperkt zijn.",
        "hide" to "VERBERG","none" to "Nog niets hier."
    )
    val pt=en+mapOf(
        "restoring_session" to "A restaurar sessão segura RS KICKBOXING…",
        "module_ready" to "Módulo pronto",
        "module_ready_sub" to "Este módulo usa o sistema visual atual do RS KICKBOXING.",
        "maintenance_default" to "Aviso de manutenção RS KICKBOXING: alguns serviços podem estar temporariamente limitados.",
        "hide" to "OCULTAR","none" to "Ainda não há nada aqui."
    )
    val es=en+mapOf(
        "restoring_session" to "Restaurando sesión segura de RS KICKBOXING…",
        "module_ready" to "Módulo listo",
        "module_ready_sub" to "Este módulo usa el sistema visual actual de RS KICKBOXING.",
        "maintenance_default" to "Aviso de mantenimiento RS KICKBOXING: algunos servicios pueden estar temporalmente limitados.",
        "hide" to "OCULTAR","none" to "Todavía no hay nada aquí."
    )
    val fr=en+mapOf(
        "restoring_session" to "Restauration de la session sécurisée RS KICKBOXING…",
        "module_ready" to "Module prêt",
        "module_ready_sub" to "Ce module utilise le système visuel actuel de RS KICKBOXING.",
        "maintenance_default" to "Avis de maintenance RS KICKBOXING : certains services peuvent être temporairement limités.",
        "hide" to "MASQUER","none" to "Rien ici pour le moment."
    )
    val de=en+mapOf(
        "restoring_session" to "Sichere RS KICKBOXING-Sitzung wird wiederhergestellt…",
        "module_ready" to "Modul bereit",
        "module_ready_sub" to "Dieses Modul verwendet das aktuelle RS KICKBOXING-Designsystem.",
        "maintenance_default" to "RS KICKBOXING Wartungshinweis: Einige Dienste können vorübergehend eingeschränkt sein.",
        "hide" to "AUSBLENDEN","none" to "Noch nichts hier."
    )
    val it=en+mapOf(
        "restoring_session" to "Ripristino della sessione sicura RS KICKBOXING…",
        "module_ready" to "Modulo pronto",
        "module_ready_sub" to "Questo modulo usa il sistema visivo attuale di RS KICKBOXING.",
        "maintenance_default" to "Avviso manutenzione RS KICKBOXING: alcuni servizi potrebbero essere temporaneamente limitati.",
        "hide" to "NASCONDI","none" to "Ancora nulla qui."
    )
    val pl=en+mapOf(
        "restoring_session" to "Przywracanie bezpiecznej sesji RS KICKBOXING…",
        "module_ready" to "Moduł gotowy",
        "module_ready_sub" to "Ten moduł używa aktualnego systemu wizualnego RS KICKBOXING.",
        "maintenance_default" to "Komunikat serwisowy RS KICKBOXING: niektóre usługi mogą być tymczasowo ograniczone.",
        "hide" to "UKRYJ","none" to "Jeszcze nic tu nie ma."
    )
    val tr=en+mapOf(
        "restoring_session" to "Güvenli RS KICKBOXING oturumu geri yükleniyor…",
        "module_ready" to "Modül hazır",
        "module_ready_sub" to "Bu modül güncel RS KICKBOXING görsel sistemini kullanır.",
        "maintenance_default" to "RS KICKBOXING bakım bildirimi: bazı hizmetler geçici olarak sınırlı olabilir.",
        "hide" to "GİZLE","none" to "Henüz burada bir şey yok."
    )
    val pack=when(lang.code){
        "nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en
    }
    return pack[key]?:en[key]?:key
}

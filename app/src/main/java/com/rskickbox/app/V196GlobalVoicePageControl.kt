package com.rskickbox.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class RsVoicePageCommandV196 { SCROLL_DOWN, SCROLL_UP, TOP, BOTTOM }

object RsVoicePageBusV196 {
    private val _commands=MutableSharedFlow<RsVoicePageCommandV196>(
        replay=0,
        extraBufferCapacity=8,
        onBufferOverflow=BufferOverflow.DROP_OLDEST
    )
    val commands=_commands.asSharedFlow()

    fun send(command:RsVoicePageCommandV196){
        _commands.tryEmit(command)
    }
}

fun rsVoicePageCommandV196(text:String):RsVoicePageCommandV196?{
    val q=text.trim().lowercase()
    return when{
        listOf(
            "scroll down","go down","page down","move down","lower on page",
            "scroll naar beneden","ga naar beneden","naar beneden scrollen",
            "descer página","rolar para baixo","desce",
            "desplázate hacia abajo","baja la página","scroll abajo",
            "fais défiler vers le bas","descends la page",
            "nach unten scrollen","seite runter",
            "scorri verso il basso","vai giù",
            "przewiń w dół","niżej",
            "aşağı kaydır","sayfayı aşağı"
        ).any{q.contains(it)}->RsVoicePageCommandV196.SCROLL_DOWN
        listOf(
            "scroll up","go up","page up","move up","higher on page",
            "scroll naar boven","ga naar boven","naar boven scrollen",
            "subir página","rolar para cima",
            "desplázate hacia arriba","sube la página","scroll arriba",
            "fais défiler vers le haut","remonte la page",
            "nach oben scrollen","seite hoch",
            "scorri verso l'alto","vai su",
            "przewiń w górę","wyżej",
            "yukarı kaydır","sayfayı yukarı"
        ).any{q.contains(it)}->RsVoicePageCommandV196.SCROLL_UP
        listOf("go to top","scroll to top","top of page","naar bovenkant","início da página","principio de la página","haut de la page").any{q.contains(it)}
            ->RsVoicePageCommandV196.TOP
        listOf("go to bottom","scroll to bottom","bottom of page","onderkant pagina","fim da página","final de la página","bas de la page").any{q.contains(it)}
            ->RsVoicePageCommandV196.BOTTOM
        else->null
    }
}

fun rsVoiceReadPageRequestedV196(text:String):Boolean{
    val q=text.trim().lowercase()
    return listOf(
        "read this page","read page","read this page content","what is on this page","explain this page",
        "lees deze pagina","lees deze pagina voor","wat staat op deze pagina",
        "lê esta página","ler esta página","o que está nesta página",
        "lee esta página","qué hay en esta página",
        "lis cette page","lis le contenu de cette page",
        "lies diese seite","leggi questa pagina","przeczytaj tę stronę","bu sayfayı oku"
    ).any{q.contains(it)}
}

fun rsVoiceMessageReadRequestV196(text:String):String?{
    val raw=text.trim()
    val q=raw.lowercase()
    val triggers=listOf(
        "read the last messages from ","read last messages from ","read messages from ",
        "read me the last messages from ","latest messages from ",
        "lees de laatste berichten van ","lees berichten van ",
        "lê as últimas mensagens de ","ler mensagens de ",
        "lee los últimos mensajes de ","lee mensajes de ",
        "lis les derniers messages de ","lis les messages de "
    )
    val trigger=triggers.firstOrNull{q.contains(it)}?:return null
    val start=q.indexOf(trigger)+trigger.length
    return raw.substring(start).trim().trimEnd('.','?','!').take(80).ifBlank{null}
}

fun rsVoiceDeleteLastMessageRequestedV196(text:String):Boolean{
    val q=text.trim().lowercase()
    return listOf(
        "delete last message","delete that message","remove last message","delete it",
        "verwijder laatste bericht","verwijder dat bericht",
        "apaga a última mensagem","elimina esa mensagem",
        "elimina el último mensaje","borra ese mensaje",
        "supprime le dernier message","supprime ce message"
    ).any{q.contains(it)}
}

fun rsVoiceSaveLastMessageRequestedV196(text:String):Boolean{
    val q=text.trim().lowercase()
    return listOf(
        "save last message","save that message","save to gallery","save message to gallery",
        "bewaar laatste bericht","opslaan in galerij","bewaar in galerij",
        "guardar última mensagem","guardar na galeria",
        "guardar último mensaje","guardar en galería",
        "enregistrer le dernier message","enregistrer dans la galerie"
    ).any{q.contains(it)}
}

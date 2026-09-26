package com.rskickbox.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

class RsQuietVoiceControllerV171(
    private val context:Context,
    private val onResult:(String)->Unit,
    private val onStatus:(String)->Unit,
    private val onIdle:()->Unit={},
    private val onLanguageDetected:(String)->Unit={}
){
    /*
     * Compatibility adapter only.
     *
     * The production microphone owner is RsVoiceWakeServiceV165. Older AI
     * screens still compile against this controller, but this class must never
     * create its own SpeechRecognizer because two owners can race for AudioRecord
     * and reproduce the historical AI/chat/music mic crashes.
     */
    private var destroyed=false

    fun start(locale:Locale){
        if(destroyed)return
        val store=RsStore(context)
        val requestedCode=locale.language.lowercase(Locale.ROOT)
        if(requestedCode in setOf("en","nl","pt","es","fr","de","it","pl","tr")){
            store.ps("ai_voice_language_v161",requestedCode)
            store.ps("lang_last_spoken_v182",requestedCode)
            onLanguageDetected(requestedCode)
        }
        onStatus("")
        store.ps("rs_ai_last_checkpoint_v205","LEGACY_MIC_ROUTED_TO_SHARED_SERVICE")
        store.ps("rs_ai_last_checkpoint_ms_v205",System.currentTimeMillis().toString())
        runCatching{RsVoiceWakeServiceV165.directListenOnce(context)}
            .onFailure{
                onStatus("Voice input could not start.")
                onIdle()
            }
    }

    fun stop(){
        // Do not stop the shared wake service from a legacy screen lifecycle.
        // MASTER AI LISTENING and RsVoiceWakeServiceV165 own service shutdown.
    }

    fun destroy(){
        destroyed=true
    }
}

sealed class RsAiPlatformIntentV171{
    data class OpenRoute(val route:String,val label:String):RsAiPlatformIntentV171()
    data class ChangeLanguage(val code:String,val label:String):RsAiPlatformIntentV171()
    data class ChangeAvatar(val avatar:String,val label:String):RsAiPlatformIntentV171()
    data object UploadMusic:RsAiPlatformIntentV171()
    data object PlayMusic:RsAiPlatformIntentV171()
    data object PauseMusic:RsAiPlatformIntentV171()
    data object StopMusic:RsAiPlatformIntentV171()
    data object NextMusic:RsAiPlatformIntentV171()
    data object PreviousMusic:RsAiPlatformIntentV171()
    data object MinimizeApp:RsAiPlatformIntentV171()
    data object StopListening:RsAiPlatformIntentV171()
    data object CloseAi:RsAiPlatformIntentV171()
    data object CloseApp:RsAiPlatformIntentV171()
    data object Logout:RsAiPlatformIntentV171()
    data object ChangeVoiceStyle:RsAiPlatformIntentV171()
    data class DirectCall(val person:String,val type:String):RsAiPlatformIntentV171()
    data class GroupVideoCall(val people:List<String>):RsAiPlatformIntentV171()
    data object CommandGuide:RsAiPlatformIntentV171()
    data object Help:RsAiPlatformIntentV171()
    data object None:RsAiPlatformIntentV171()
}

private fun rsContainsAnyV171(s:String,vararg phrases:String)=phrases.any{s.contains(it)}

fun rsAiPlatformIntentV171(raw:String,lang:RsLang?=null):RsAiPlatformIntentV171{
    val s=raw.lowercase(Locale.ROOT).trim()

    val languageMap=listOf(
        Triple("en","English",arrayOf("english","inglês","ingles","anglais","englisch","inglese","angielski","ingilizce")),
        Triple("nl","Nederlands",arrayOf("dutch","nederlands","holandês","holandes","neerlandés","néerlandais","niederländisch")),
        Triple("pt","Português",arrayOf("portuguese","português","portugues","portugais","portugiesisch")),
        Triple("es","Español",arrayOf("spanish","español","espanol","espagnol","spanisch")),
        Triple("fr","Français",arrayOf("french","français","francais","französisch")),
        Triple("de","Deutsch",arrayOf("german","deutsch","alemão","aleman","allemand","tedesco")),
        Triple("it","Italiano",arrayOf("italian","italiano","italien","italienisch")),
        Triple("pl","Polski",arrayOf("polish","polski","polonês","polonais")),
        Triple("tr","Türkçe",arrayOf("turkish","türkçe","turkce","turco","turc"))
    )
    if(rsContainsAnyV171(s,"change language","change voice","speak ","switch to ","muda para","mudar para","cambiar a","parle ","sprich ","fala ")){
        languageMap.firstOrNull{(_,_,aliases)->aliases.any{s.contains(it)}}?.let{(code,label,_)->
            return RsAiPlatformIntentV171.ChangeLanguage(code,label)
        }
    }

    if(rsContainsAnyV171(
        s,
        "stop listening","stop listening to me","stop voice listening","turn listening off","disable listening",
        "stop met luisteren","luister niet meer","zet luisteren uit",
        "para de ouvir","parar de ouvir","desliga a escuta",
        "deja de escuchar","detén la escucha","desactiva la escucha",
        "arrête d'écouter","arrête de m'écouter","désactive l'écoute",
        "hör auf zuzuhören","zuhören ausschalten",
        "smetti di ascoltare","disattiva ascolto",
        "przestań słuchać","wyłącz nasłuchiwanie",
        "dinlemeyi durdur","dinlemeyi kapat"
    ))return RsAiPlatformIntentV171.StopListening

    if(rsContainsAnyV171(
        s,
        "minimize app","minimise app","put app in background","rs minimize","rs minimise",
        "minimaliseer app","zet app op achtergrond","minimiza a app","manda a app para segundo plano",
        "minimiza la app","réduis l'app","app minimieren","riduci app","zminimalizuj aplikację","uygulamayı küçült"
    ))return RsAiPlatformIntentV171.MinimizeApp
    if(rsContainsAnyV171(
        s,
        "close ai","exit ai","leave ai","go out of ai","back from ai","close assistant","exit assistant",
        "sluit ai","verlaat ai","sair da ia","fechar ia","salir de ia","cerrar ia","quitter l'ia","fermer l'ia",
        "ki verlassen","ki schließen","esci dall'ia","chiudi ia","wyjdź z ai","zamknij ai","ai'dan çık","ai kapat"
    ))return RsAiPlatformIntentV171.CloseAi
    if(rsContainsAnyV171(
        s,
        "close app","close rs","rs close app","sluit app","sluit rs","fecha a app","fecha rs",
        "cierra la app","cierra rs","ferme l'app","ferme rs","app schließen","rs schließen",
        "chiudi app","chiudi rs","zamknij aplikację","zamknij rs","uygulamayı kapat","rs kapat"
    ))return RsAiPlatformIntentV171.CloseApp
    if(rsContainsAnyV171(
        s,
        "log out","logout","sign out","rs log out","uitloggen","sair da conta","terminar sessão",
        "cerrar sesión","déconnexion","abmelden","esci dall'account","wyloguj","çıkış yap"
    ))return RsAiPlatformIntentV171.Logout

    if(rsContainsAnyV171(
        s,
        "another voice","different voice","change voice style","other voice style","next voice",
        "andere stem","andere stemstijl","outra voz","outra voz feminina","outra voz masculina",
        "otra voz","otra voz femenina","otra voz masculina","autre voix","autre style de voix",
        "andere stimme","altra voce","inny głos","başka ses"
    ))return RsAiPlatformIntentV171.ChangeVoiceStyle

    val groupVideoRegex=listOf(
        Regex("(?i)(?:group video|video group|group call)(?:\\s+with)?\\s+(?:me[, ]*(?:and|,)?\\s*)?(.+)"),
        Regex("(?i)(?:video call with)\\s+(?:me[, ]*(?:and|,)?\\s*)?(.+)"),
        Regex("(?i)(?:vídeo em grupo|video em grupo|chamada de grupo)(?:\\s+com)?\\s+(?:eu[, ]*(?:e|,)?\\s*)?(.+)"),
        Regex("(?i)(?:videochamada com)\\s+(?:eu[, ]*(?:e|,)?\\s*)?(.+)")
    )
    groupVideoRegex.firstNotNullOfOrNull{rx->rx.find(raw)?.groupValues?.getOrNull(1)}?.let{tail->
        val people=tail
            .replace(Regex("(?i)\\b(me|myself|eu|mim)\\b")," ")
            .split(Regex("(?i)\\s*(?:,|\\band\\b|\\be\\b|\\by\\b|\\bet\\b|\\bund\\b|\\be\\b)\\s*"))
            .map{it.trim().trim('.',',','!','?')}
            .filter{it.length>=2}
            .distinctBy{it.lowercase(Locale.ROOT)}
        if(people.size>=2)return RsAiPlatformIntentV171.GroupVideoCall(people)
    }

    val directVideo=Regex("(?i)(?:video call|videochat|make a video with|start video with|video with|videochamada com|video llamada con|appel vidéo avec|videoanruf mit)\\s+(.+)")
        .find(raw)?.groupValues?.getOrNull(1)?.trim()?.trim('.',',','!','?')
    if(!directVideo.isNullOrBlank())return RsAiPlatformIntentV171.DirectCall(directVideo,"VIDEO")

    val directAudio=Regex("(?i)(?:call|phone|ring|ligar para|liga para|chamar|llama a|appelle|ruf)\\s+(.+)")
        .find(raw)?.groupValues?.getOrNull(1)?.trim()?.trim('.',',','!','?')
    if(!directAudio.isNullOrBlank())return RsAiPlatformIntentV171.DirectCall(directAudio,"AUDIO")

    if(rsContainsAnyV171(
        s,
        "show commands","show me commands","voice commands","what commands can i use","command list",
        "show what you can do","visual preview of commands","show ai controls","how can i control rs",
        "toon commando's","spraakcommando's","mostrar comandos","comandos de voz","mostrar comandos de voz",
        "affiche les commandes","sprachbefehle","mostra comandi","pokaż komendy","sesli komutları göster"
    ))return RsAiPlatformIntentV171.CommandGuide

    if(rsContainsAnyV171(s,"marcus","male trainer","man trainer","male voice","homem","hombre","homme","mann")){
        return RsAiPlatformIntentV171.ChangeAvatar("MALE","Marcus")
    }
    if(rsContainsAnyV171(s,"sofia","female trainer","woman trainer","female voice","mulher","mujer","femme","frau")){
        return RsAiPlatformIntentV171.ChangeAvatar("FEMALE","Sofia")
    }

    if(rsContainsAnyV171(s,"upload music","add music","import music","new music","muziek upload","muziek toevoegen","carregar música","adicionar música","subir música","ajouter musique")){
        return RsAiPlatformIntentV171.UploadMusic
    }
    if(rsContainsAnyV171(s,"pause music","pause the music","pauzeer muziek","pausar música","pausa música","mets la musique en pause"))return RsAiPlatformIntentV171.PauseMusic
    if(rsContainsAnyV171(s,"stop music","stop the music","stop muziek","parar música","detener música","arrête la musique"))return RsAiPlatformIntentV171.StopMusic
    if(rsContainsAnyV171(s,"next song","next music","next track","volgend nummer","próxima música","siguiente canción","chanson suivante"))return RsAiPlatformIntentV171.NextMusic
    if(rsContainsAnyV171(s,"previous song","previous track","last song","vorig nummer","música anterior","canción anterior","chanson précédente"))return RsAiPlatformIntentV171.PreviousMusic
    if(rsContainsAnyV171(s,"play music","start music","resume music","speel muziek","tocar música","reproducir música","joue la musique"))return RsAiPlatformIntentV171.PlayMusic

    val routes=listOf(
        Triple("music","RS Music",arrayOf("music page","music player","rs music","muziek","música","musica","musique")),
        Triple("coachchat","Private Chats",arrayOf("private chat","private chats","private messages","direct chat","direct chats","coach chat","private coach","privé chat","prive chat","privé chats","prive chats")),
        Triple("support","Support",arrayOf("support","help desk","hulp","suporte","soporte")),
        Triple("groups","Groups",arrayOf("groups","groepen","grupos","groupes")),
        Triple("community","Community",arrayOf("community","comunidade","comunidad","communauté")),
        Triple("notifications","Notifications",arrayOf("notifications","meldingen","notificações","notificaciones")),
        Triple("book","Trainer Book",arrayOf("book","trainer book","boek","livro","libro","livre")),
        Triple("guide","App Guide",arrayOf("app guide","guide","handleiding","guia")),
        Triple("settings","Settings",arrayOf("settings","instellingen","definições","ajustes","réglages")),
        Triple("home","Dashboard",arrayOf("dashboard","home page","home screen","start page")),
        Triple("trainer","Trainer Dashboard",arrayOf("trainer dashboard","admin dashboard")),
        Triple("themes","Visual Theme Studio",arrayOf("theme studio","themes","visual themes","styles")),
        Triple("backgrounds","Visual Asset Studio",arrayOf("backgrounds","visual asset studio","visuals")),
        Triple("members","Student Manager",arrayOf("student manager","members","students")),
        Triple("classes","Classes",arrayOf("classes","training classes")),
        Triple("homework","Homework",arrayOf("homework","huiswerk")),
        Triple("progress","Progress",arrayOf("progress","voortgang")),
        Triple("academy","RS Academy",arrayOf("academy","rs academy")),
        Triple("profile","Profile",arrayOf("profile","my profile","profiel"))
    )

    if(rsContainsAnyV171(s,"open ","take me to","go to ","show me ","bring me to","navigate to","leva me","ir para","abre ","ouvrir ","öffne ","apri ","otwórz ","aç ","ga naar ")){
        routes.firstOrNull{(_,_,aliases)->aliases.any{s.contains(it)}}?.let{(route,label,_)->
            return RsAiPlatformIntentV171.OpenRoute(route,label)
        }

        if(lang!=null){
            val allRoutes=listOf(
                "home","trainer","guide","student_guide","voice","session","academy","techniques","home_training",
                "classes","events","coachchat","community","groups","private_lessons","progress","challenges","badges",
                "fightcamp","compare","vault","homework","music","finance","promotions","book","profile","settings",
                "themes","backgrounds","branding","intro_settings","members","access","payments","invoices","analytics",
                "notifications","checkin","support","release","privacy_admin","landing_admin","content","homework_admin",
                "session_builder","music_admin","notes","plans_admin","progress_admin","assessments","challenge_admin",
                "fightcamp_admin","attendance","events_admin","schedule","documents","referrals"
            )
            val match=allRoutes.mapNotNull{route->
                val title=rsRouteTitle(lang,route,route.replace('_',' '))
                val tokens=(title+" "+route.replace('_',' '))
                    .lowercase(Locale.ROOT)
                    .split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter{
                        it.length>=3 &&
                        it !in setOf("the","and","van","voor","del","des","der","die","das","app")
                    }
                val score=tokens.count{s.contains(it)}
                if(score>0)Triple(route,title,score) else null
            }.maxByOrNull{it.third}
            if(match!=null)return RsAiPlatformIntentV171.OpenRoute(match.first,match.second)
        }
    }

    if(rsContainsAnyV171(s,"what can you do","what can rs do","help me","help with the app","what are your features","o que podes fazer","wat kan je","qué puedes hacer")){
        return RsAiPlatformIntentV171.Help
    }

    return RsAiPlatformIntentV171.None
}

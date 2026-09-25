package com.rskickbox.app

import android.Manifest
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.io.File

private const val RS_VOICE_CHANNEL_V165="rs_voice_wake_v172"
private const val RS_VOICE_NOTIFICATION_ID_V165=1651
private const val RS_VOICE_LOGIN_NOTIFICATION_ID_V165=1652
private const val RS_VOICE_WAKE_ACTION_CHANNEL_V182="rs_voice_wake_action_v182"
private const val RS_VOICE_WAKE_ACTION_NOTIFICATION_ID_V182=1653
private const val RS_TRUSTED_LOGIN_MS_V165=72L*60L*60L*1000L
private const val RS_INACTIVITY_LOGOUT_MS_V165=24L*60L*60L*1000L

private fun rsWakePhrasesV165()=listOf(
    "wake up rs","rs wake up","hey rs","rs hey","ok rs","rs ok",
    "word wakker rs","rs word wakker",
    "acorda rs","rs acorda","olá rs","ola rs",
    "despierta rs","rs despierta","hola rs",
    "réveille toi rs","reveille toi rs","rs réveille toi","rs reveille toi","salut rs",
    "wach auf rs","rs wach auf","hallo rs",
    "svegliati rs","rs svegliati","ciao rs",
    "obudź się rs","obudz sie rs","rs obudź się","rs obudz sie","hej rs",
    "uyan rs","rs uyan","hey rs"
)

private fun rsContainsWakePhraseV165(text:String):Boolean{
    val s=text.lowercase(Locale.ROOT).replace(","," ").replace("."," ").trim()
    return rsWakePhrasesV165().any{s.contains(it)}
}

private fun rsStripWakePhraseV165(text:String):String{
    var out=text
    rsWakePhrasesV165()
        .sortedByDescending{it.length}
        .forEach{phrase->
            out=out.replace(Regex("(?i)\\b"+Regex.escape(phrase)+"\\b")," ")
        }
    return out.replace(Regex("[,;:.!?]+")," ").replace(Regex("\\s+")," ").trim()
}

private fun rsDetectSpokenLanguageV182(text:String,fallback:String):String{
    val s=text.lowercase(Locale.ROOT)
    val scored=listOf(
        "nl" to listOf("wat ","waar ","hoe ","muziek","instellingen","training","groep","leerling","sluit","minimaliseer","uitloggen","ga naar"),
        "pt" to listOf("o que","como ","abre ","abrir ","música","definições","treino","grupo","aluno","fecha","minimiza","sair","acorda"),
        "es" to listOf("qué ","como ","abre ","abrir ","música","ajustes","entrenamiento","grupo","alumno","cierra","minimiza","salir","despierta"),
        "fr" to listOf("quoi ","comment ","ouvre ","musique","réglages","entraînement","groupe","élève","ferme","réduis","déconnexion","réveille"),
        "de" to listOf("was ","wie ","öffne","musik","einstellungen","training","gruppe","schüler","schließe","minimiere","abmelden","wach auf"),
        "it" to listOf("cosa ","come ","apri ","musica","impostazioni","allenamento","gruppo","allievo","chiudi","riduci","esci","svegliati"),
        "pl" to listOf("co ","jak ","otwórz","muzyka","ustawienia","trening","grupa","uczeń","zamknij","zminimalizuj","wyloguj","obudź"),
        "tr" to listOf("ne ","nasıl ","aç","müzik","ayarlar","antrenman","grup","öğrenci","kapat","küçült","çıkış","uyan"),
        "en" to listOf("what ","how ","open ","music","settings","training","group","student","close","minimize","logout","wake up")
    ).map{(code,terms)->code to terms.count{sTerm->s.contains(sTerm)}}
    return scored.maxByOrNull{it.second}?.takeIf{it.second>0}?.first?:fallback
}

private fun rsVoiceSystemCommandV182(text:String):String?{
    val s=text.lowercase(Locale.ROOT)
    return when{
        listOf(
            "open app","open rs","bring rs back","show rs","open rs kickboxing",
            "open de app","open rs opnieuw","abrir app","abrir rs","abre la app","abre rs",
            "ouvre l'app","ouvre rs","app öffnen","rs öffnen","apri app","apri rs",
            "otwórz aplikację","otwórz rs","uygulamayı aç","rs aç"
        ).any{s.contains(it)}->"OPEN"
        listOf(
            "close app","rs close app","close rs","sluit app","sluit rs","fecha a app","fecha rs",
            "cierra la app","cierra rs","ferme l'app","ferme rs","app schließen","rs schließen",
            "chiudi app","chiudi rs","zamknij aplikację","zamknij rs","uygulamayı kapat","rs kapat"
        ).any{s.contains(it)}->"CLOSE"
        listOf(
            "minimize app","minimise app","rs minimize app","rs minimise app","put app in background",
            "minimaliseer app","zet app op achtergrond","minimiza a app","manda a app para segundo plano",
            "minimiza la app","réduis l'app","app minimieren","riduci app",
            "zminimalizuj aplikację","uygulamayı küçült"
        ).any{s.contains(it)}->"MINIMIZE"
        listOf(
            "log out","logout","sign out","rs log out","uitloggen","sair da conta","cerrar sesión",
            "déconnexion","abmelden","esci dall'account","wyloguj","çıkış yap"
        ).any{s.contains(it)}->"LOGOUT"
        else->null
    }
}

private fun rsVoiceGreetingV165(code:String):String=when(code){
    "nl"->"Hoi, ik ben wakker. Wat kan ik vandaag voor je doen?"
    "pt"->"Olá, estou aqui. O que posso fazer por ti hoje?"
    "es"->"Hola, estoy aquí. ¿Qué puedo hacer por ti hoy?"
    "fr"->"Salut, je suis là. Que puis-je faire pour toi aujourd’hui ?"
    "de"->"Hallo, ich bin da. Was kann ich heute für dich tun?"
    "it"->"Ciao, sono qui. Cosa posso fare per te oggi?"
    "pl"->"Cześć, jestem tutaj. Co mogę dziś dla ciebie zrobić?"
    "tr"->"Merhaba, buradayım. Bugün senin için ne yapabilirim?"
    else->"Hi, I’m here. What can I do for you today?"
}

private fun rsPlaylistNamePromptV165(code:String):String=when(code){
    "nl"->"Welke naam wil je voor deze playlist?"
    "pt"->"Que nome queres dar a esta playlist?"
    "es"->"¿Qué nombre quieres para esta playlist?"
    "fr"->"Quel nom veux-tu donner à cette playlist ?"
    "de"->"Wie soll diese Playlist heißen?"
    "it"->"Che nome vuoi dare a questa playlist?"
    "pl"->"Jak ma nazywać się ta playlista?"
    "tr"->"Bu çalma listesinin adı ne olsun?"
    else->"What name would you like for this playlist?"
}

private fun rsPlaylistSavedV165(code:String,name:String):String=when(code){
    "nl"->"Playlist $name is opgeslagen met je recente nummers."
    "pt"->"A playlist $name foi guardada com as tuas músicas recentes."
    "es"->"La playlist $name se guardó con tus canciones recientes."
    "fr"->"La playlist $name a été enregistrée avec tes morceaux récents."
    "de"->"Die Playlist $name wurde mit deinen letzten Titeln gespeichert."
    "it"->"La playlist $name è stata salvata con i tuoi brani recenti."
    "pl"->"Playlista $name została zapisana z ostatnimi utworami."
    "tr"->"$name çalma listesi son parçalarınla kaydedildi."
    else->"Playlist $name was saved with your recent songs."
}

private enum class RsVoiceMusicCommandV165{
    PLAY,PAUSE,STOP,NEXT,PREVIOUS,CREATE_LAST10,
    VOLUME_UP,VOLUME_DOWN,WHATS_PLAYING,OPEN_MUSIC,SLEEP,UNKNOWN
}

private fun rsVoiceMusicCommandV165(text:String):RsVoiceMusicCommandV165{
    val s=text.lowercase(Locale.ROOT)
    return when{
        listOf("create a playlist","maak een playlist","criar uma playlist","crea una playlist","crée une playlist","erstelle eine playlist","crea una playlist","utwórz playlist","çalma listesi oluştur").any{s.contains(it)}
            && listOf("last 10","laatste 10","últimas 10","ultimas 10","últimos 10","ultimos 10","dernières 10","letzten 10","ultimi 10","ostatnich 10","son 10").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.CREATE_LAST10
        listOf("next music","next song","volgende muziek","volgend nummer","próxima música","proxima musica","siguiente canción","chanson suivante","nächster titel","prossima canzone","następny utwór","sonraki şarkı").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.NEXT
        listOf("previous song","last song","vorige muziek","vorig nummer","música anterior","musica anterior","canción anterior","chanson précédente","vorheriger titel","canzone precedente","poprzedni utwór","önceki şarkı").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.PREVIOUS
        listOf("stop music","stop de muziek","parar música","detener música","arrête la musique","musik stoppen","ferma musica","zatrzymaj muzykę","müziği durdur").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.STOP
        listOf("pause music","pauzeer muziek","pausar música","pausa música","mets la musique en pause","musik pausieren","metti in pausa","wstrzymaj muzykę","müziği duraklat").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.PAUSE
        listOf("volume up","louder","harder","volume hoger","mais alto","sube el volumen","plus fort","lauter","più forte","glosniej","daha yüksek").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.VOLUME_UP
        listOf("volume down","quieter","softer","volume lager","mais baixo","baja el volumen","moins fort","leiser","più piano","ciszej","daha düşük").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.VOLUME_DOWN
        listOf("what is playing","what's playing","welk nummer speelt","wat speelt er","qual música está tocando","que canción suena","qu'est ce qui joue","was läuft","cosa sta suonando","co teraz gra","hangi şarkı çalıyor").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.WHATS_PLAYING
        listOf("open music","open music player","open rs music","open muziek","abrir música","abre la música","ouvre la musique","musik öffnen","apri musica","otwórz muzykę","müziği aç").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.OPEN_MUSIC
        listOf("go to sleep","sleep rs","ga slapen rs","vai dormir rs","duerme rs","dors rs","schlaf rs","dormi rs","śpij rs","uyu rs").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.SLEEP
        listOf("play music","resume music","speel muziek","ga door met muziek","tocar música","reproducir música","joue la musique","musik abspielen","riproduci musica","odtwórz muzykę","müzik çal").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.PLAY
        else->RsVoiceMusicCommandV165.UNKNOWN
    }
}

private fun rsVoiceRouteV167(text:String):String?{
    val s=text.lowercase(Locale.ROOT)
    val hasNavigationVerb=listOf(
        "open ","go to ","take me to ","show ","bring me to ","navigate ",
        "open ","ga naar ","toon ","breng me naar ",
        "abre ","abrir ","leva-me ","leva me ","vai para ",
        "abre ","ir a ","llévame ","llevame ","muestra ",
        "ouvre ","va à ","emmène-moi ","emmene moi ",
        "öffne ","gehe zu ","bring mich zu ",
        "apri ","vai a ","portami a ",
        "otwórz ","idź do ","idz do ","pokaż ",
        "aç ","git ","götür ","goster ","göster "
    ).any{s.contains(it)}
    if(!hasNavigationVerb)return null

    val routes=listOf(
        "voice" to listOf("rs ai","ai coach","ai trainer","assistant","assistent","assistente","asistente","coach ia","ki trainer","yz antrenör"),
        "coachchat" to listOf("rs chat","private chat","private chats","direct chat","coach chat","privé chat","chat privado","chat privé"),
        "groups" to listOf("groups","groepen","grupos","groupes","gruppen","gruppi","grupy","gruplar"),
        "community" to listOf("community","comunidade","comunidad","communauté","społeczność","topluluk"),
        "support" to listOf("support","hulp","suporte","soporte","aide","hilfe","supporto","pomoc","destek"),
        "notifications" to listOf("notifications","meldingen","notificações","notificaciones","benachrichtigungen","notifiche","powiadomienia","bildirimler"),
        "music" to listOf("rs music","music","muziek","música","musique","musik","musica","muzyka","müzik"),
        "guide" to listOf("app guide","guide","gids","guia","anleitung","przewodnik","rehber"),
        "themes" to listOf("themes","theme studio","styles","thema","temas","thèmes","design","motyw","tema"),
        "backgrounds" to listOf("visual asset","background","backgrounds","achtergrond","fundos","fondos","arrière-plan","hintergrund","sfondo","tło","arka plan"),
        "members" to listOf("student manager","students","studenten","alunos","alumnos","élèves","schüler","allievi","uczniowie","öğrenciler"),
        "access" to listOf("access","subscriptions","toegang","abonnement","subscrições","suscripciones","abonnements","zugriff","abos","abbonamenti","subskrypcje","abonelik"),
        "classes" to listOf("classes","class manager","lessen","aulas","clases","cours","kurse","lezioni","zajęcia","dersler"),
        "attendance" to listOf("attendance","aanwezigheid","presenças","asistencia","présences","anwesenheit","presenze","obecność","yoklama"),
        "homework_admin" to listOf("homework manager","huiswerkbeheer","gestor de tarefas","tareas","devoirs","hausaufgaben","compiti","zadania","ödev"),
        "homework" to listOf("homework","huiswerk","tarefas","tareas","devoirs","hausaufgaben","compiti","zadania","ödev"),
        "progress_admin" to listOf("progress manager","voortgangsbeheer","gestor de progresso","progreso","progression","fortschritt","progressi","postępy","ilerleme"),
        "progress" to listOf("progress","voortgang","progresso","progreso","progression","fortschritt","progressi","postępy","ilerleme"),
        "documents" to listOf("documents","documenten","documentos","documents","dokumente","documenti","dokumenty","belgeler"),
        "payments" to listOf("payment center","payments","betalingen","pagamentos","pagos","paiements","zahlungen","pagamenti","płatności","ödemeler"),
        "analytics" to listOf("analytics","analyses","análises","analíticas","analytique","analysen","analisi","analityka","analiz"),
        "settings" to listOf("settings","instellingen","definições","ajustes","réglages","einstellungen","impostazioni","ustawienia","ayarlar"),
        "trainer" to listOf("trainer dashboard","trainer panel","trainer dashboard","painel do treinador","panel del entrenador","tableau entraîneur"),
        "home" to listOf("dashboard","home","start page","startpagina","painel","panel","tableau","ana panel")
    )
    return routes.firstOrNull{(_,aliases)->aliases.any{s.contains(it)}}?.first
}

private fun rsVoiceDynamicRouteV184(text:String,lang:RsLang):String?{
    val s=text.lowercase(Locale.ROOT)
    val hasNavigationVerb=listOf(
        "open ","go to ","take me to ","show ","bring me to ","navigate ",
        "ga naar ","toon ","breng me naar ",
        "abre ","abrir ","leva-me ","leva me ","vai para ",
        "ir a ","llévame ","llevame ","muestra ",
        "ouvre ","va à ","emmène-moi ","emmene moi ",
        "öffne ","gehe zu ","bring mich zu ",
        "apri ","vai a ","portami a ",
        "otwórz ","idź do ","idz do ","pokaż ",
        "aç ","git ","götür ","goster ","göster "
    ).any{s.contains(it)}
    if(!hasNavigationVerb)return null

    val routes=listOf(
        "home","trainer","guide","student_guide","voice","session","academy","techniques","home_training",
        "classes","events","coachchat","community","groups","private_lessons","progress","challenges","badges",
        "fightcamp","compare","vault","homework","music","finance","promotions","book","profile","settings",
        "themes","backgrounds","branding","intro_settings","members","access","payments","invoices","analytics",
        "notifications","checkin","support","release","privacy_admin","landing_admin","content","homework_admin",
        "session_builder","music_admin","notes","plans_admin","progress_admin","assessments","challenge_admin",
        "fightcamp_admin","attendance","events_admin","schedule","documents","referrals"
    )
    return routes.mapNotNull{route->
        val title=rsRouteTitle(lang,route,route.replace('_',' '))
            .lowercase(Locale.ROOT)
            .replace("&"," ")
            .replace("·"," ")
        val tokens=title.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter{it.length>=3 && it !in setOf("the","and","van","voor","del","des","der","die","das")}
        val score=tokens.count{s.contains(it)}
        if(score>0)Triple(route,score,tokens.sumOf{it.length}) else null
    }.maxWithOrNull(compareBy<Triple<String,Int,Int>>{it.second}.thenBy{it.third})?.first
}

private fun rsTrackMapV165(store:RsStore):Map<String,String> =
    store.s("local_music_tracks","")
        .split("§")
        .filter{it.contains("¤")}
        .mapNotNull{row->
            val p=row.split("¤",limit=2)
            if(p.size==2)p[0] to p[1] else null
        }.toMap()

private fun rsSaveVoicePlaylistV165(context:Context,name:String,count:Int=10):Boolean{
    val store=RsStore(context)
    val recent=store.s("rs_music_recent_history_v165","")
        .split("§")
        .filter{it.isNotBlank()}
        .distinct()
        .take(count)
    if(recent.isEmpty())return false

    val raw=store.s("music_named_playlists_v108","")
    val arr=runCatching{if(raw.isBlank())JSONArray() else JSONArray(raw)}.getOrDefault(JSONArray())
    val next=JSONArray()
    for(i in 0 until arr.length()){
        val o=arr.optJSONObject(i)?:continue
        if(!o.optString("name").equals(name,true))next.put(o)
    }
    next.put(JSONObject().apply{
        put("name",name.take(60))
        put("uris",JSONArray().apply{recent.forEach{put(it)}})
    })
    store.ps("music_named_playlists_v108",next.toString())
    store.ps("music_active_playlist_v108",name.take(60))
    return true
}

private fun rsAllMusicItemsV165(context:Context):List<MediaItem>{
    val store=RsStore(context)
    return rsTrackMapV165(store).map{(uri,name)->
        MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist("RS KICKBOXING")
                    .build()
            )
            .build()
    }
}

class RsVoiceWakeServiceV165:Service(),TextToSpeech.OnInitListener{
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var recognizer:SpeechRecognizer?=null
    private var tts:TextToSpeech?=null
    private var wakeLock:PowerManager.WakeLock?=null
    private var awakeUntil=0L
    private var awaitingPlaylistName=false
    private var controllerFuture:ListenableFuture<MediaController>?=null
    private var controller:MediaController?=null
    private var wakeRecognitionFallback=false
    @Volatile private var serviceSpeaking=false
    @Volatile private var serviceUtteranceId=""
    private var serviceCloudVoicePlayer:MediaPlayer?=null
    private var serviceVoiceRequestId:Long=0L
    private var pendingHandoffSpeechV194:String=""
    private var pendingVoiceMessageV196:RsCloudCoachMessageV72?=null
    private var pendingVoiceMessageSenderV196:String=""
    private var toneRestoreJob:Job?=null
    private var systemToneMutedByRs=false
    private var offlineWakeEngineV188:RsOfflineWakeEngineV188?=null
    private var offlineWakePrepareJobV188:Job?=null
    private var silentWakePreparingV188=false
    private var silentWakeSessionFailedV188=false
    @Volatile private var pausedForForegroundAiV197=false
    private val store by lazy{RsStore(this)}

    private fun readCurrentPageV196(continueReading:Boolean=false){
        val route=store.s("ai_current_route_v177",store.s("session_last_route","")).ifBlank{
            if(store.s("session_role","")=="trainer")"trainer" else "home"
        }
        val role=if(store.s("session_role","")=="trainer")RsRole.TRAINER else RsRole.STUDENT
        val lang=language()
        val guide=rsAiAppGuideForRouteV177(route,lang,role)
        if(guide==null){
            val title=rsRouteTitle(lang,route,route.replace('_',' ').replaceFirstChar{it.uppercase()})
            speak(
                when(lang.code){
                    "nl"->"Je bent op "+title+". Ik kan deze pagina omhoog of omlaag scrollen. Zeg scroll omlaag of scroll omhoog."
                    "pt"->"Estás em "+title+". Posso deslocar esta página para baixo ou para cima."
                    "es"->"Estás en "+title+". Puedo desplazar esta página hacia abajo o hacia arriba."
                    "fr"->"Tu es sur "+title+". Je peux faire défiler cette page vers le bas ou vers le haut."
                    else->"You are on "+title+". I can scroll this page down or up."
                }
            )
            return
        }
        val key="voice_page_read_chunk_v196_"+route
        val previous=store.s(key,"0").toIntOrNull()?:0
        val chunk=if(continueReading)(previous+1).coerceAtMost(2) else 0
        store.ps(key,chunk.toString())
        val groups=guide.steps.chunked(kotlin.math.max(1,kotlin.math.ceil(guide.steps.size/3.0).toInt()))
        val selected=groups.getOrNull(chunk).orEmpty()
        val text=buildString{
            if(chunk==0){
                append(guide.title).append(". ")
                append(guide.purpose).append(" ")
            }
            selected.forEachIndexed{i,step->append((i+1)).append(". ").append(step).append(" ")}
            if(chunk<groups.lastIndex){
                append(
                    when(lang.code){
                        "nl"->"Zeg verder lezen voor het volgende deel."
                        "pt"->"Diz continuar a ler para a próxima parte."
                        "es"->"Di seguir leyendo para la siguiente parte."
                        "fr"->"Dis continuer la lecture pour la partie suivante."
                        else->"Say continue reading for the next part."
                    }
                )
            }else{
                append(guide.tip)
            }
        }
        speak(text)
    }

    private fun readMessagesFromV196(personName:String){
        scope.launch{
            val contacts=rsChatContactsV125().getOrElse{
                speak("I could not load your chat contacts.")
                return@launch
            }
            val resolved=rsAiBestContactV194(contacts,personName)
            if(resolved==null){
                speak(
                    when(language().code){
                        "nl"->"Ik kon "+personName+" niet eenduidig vinden in je chatcontacten."
                        "pt"->"Não consegui encontrar "+personName+" de forma inequívoca nos contactos."
                        "es"->"No pude encontrar a "+personName+" de forma inequívoca en tus contactos."
                        else->"I could not uniquely find "+personName+" in your chat contacts."
                    }
                )
                return@launch
            }
            val contact=contacts.firstOrNull{it.userId==resolved.userId}
            val studentId=if(contact?.role=="student")resolved.userId else rsCloudMyStudentIdV72().getOrElse{
                speak("I could not open that private message thread.")
                return@launch
            }
            val messages=rsCloudCoachMessagesV72(studentId).getOrElse{
                speak("I could not load the private messages.")
                return@launch
            }
            val incoming=messages.filter{message->
                when{
                    message.senderId.isNotBlank()->message.senderId==resolved.userId
                    contact?.role in setOf("trainer","admin")->message.senderRole in setOf("trainer","admin")
                    else->message.studentId==resolved.userId && message.senderRole=="student"
                }
            }.sortedByDescending{it.createdAtMillis()}.take(3)
            if(incoming.isEmpty()){
                speak(
                    when(language().code){
                        "nl"->"Ik heb geen recente ontvangen berichten van "+resolved.displayName+" gevonden."
                        "pt"->"Não encontrei mensagens recentes recebidas de "+resolved.displayName+"."
                        "es"->"No encontré mensajes recientes recibidos de "+resolved.displayName+"."
                        else->"I found no recent received messages from "+resolved.displayName+"."
                    }
                )
                return@launch
            }
            pendingVoiceMessageV196=incoming.first()
            pendingVoiceMessageSenderV196=resolved.displayName
            val formatter=java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault())
            val spoken=incoming.reversed().joinToString(" "){message->
                val time=formatter.format(java.util.Date(message.createdAtMillis()))
                val body=message.body.ifBlank{
                    when(message.mediaKind){
                        "IMAGE"->"image"
                        "VIDEO"->"video"
                        "AUDIO"->"voice message"
                        else->"media attachment"
                    }
                }
                time+". "+body.take(450)+"."
            }
            speak(
                when(language().code){
                    "nl"->"De laatste ontvangen berichten van "+resolved.displayName+" zijn: "+spoken+" Je kunt zeggen: verwijder dat bericht, of bewaar dat bericht in de galerij."
                    "pt"->"As últimas mensagens recebidas de "+resolved.displayName+" são: "+spoken+" Podes dizer apagar essa mensagem ou guardar essa mensagem na galeria."
                    "es"->"Los últimos mensajes recibidos de "+resolved.displayName+" son: "+spoken+" Puedes decir borrar ese mensaje o guardar ese mensaje en la galería."
                    "fr"->"Les derniers messages reçus de "+resolved.displayName+" sont : "+spoken+" Tu peux dire supprimer ce message ou enregistrer ce message dans la galerie."
                    else->"The latest received messages from "+resolved.displayName+" are: "+spoken+" You can say delete that message, or save that message to the gallery."
                }
            )
        }
    }

    private fun setWakeStatusV168(status:String){
        // Keep diagnostics in local state without constantly re-posting the
        // foreground notification. Re-notifying on every recognizer cycle made
        // Android repeatedly animate/chime the notification shade.
        store.ps("rs_voice_wake_status_v168",status)
        store.ps("rs_voice_wake_status_ms_v168",System.currentTimeMillis().toString())
    }

    override fun onCreate(){
        super.onCreate()
        pausedForForegroundAiV197=store.b("rs_voice_wake_paused_for_ai_v197",false)
        createChannel()
        startForeground(
            RS_VOICE_NOTIFICATION_ID_V165,
            notification(
                when(language().code){
                    "nl"->"Voice Wake klaar"
                    "pt"->"Voice Wake pronto"
                    "es"->"Voice Wake listo"
                    "fr"->"Voice Wake prêt"
                    "de"->"Voice Wake bereit"
                    "it"->"Voice Wake pronto"
                    "pl"->"Voice Wake gotowy"
                    "tr"->"Voice Wake hazır"
                    else->"Voice Wake ready"
                }
            )
        )
        tts=TextToSpeech(this,this)

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val pm=getSystemService(PowerManager::class.java)
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"rskickbox:voicewake").apply{
                setReferenceCounted(false)
                acquire()
            }
        }

        val token=SessionToken(this,ComponentName(this,RsMusicPlaybackServiceV90::class.java))
        controllerFuture=MediaController.Builder(this,token).buildAsync().also{future->
            future.addListener({
                controller=runCatching{future.get()}.getOrNull()
            },ContextCompat.getMainExecutor(this))
        }

        if(trustedSessionActive() || recoverableLockedIdentityV186()){
            if(pausedForForegroundAiV197){
                setWakeStatusV168("PAUSED_FOR_FOREGROUND_AI")
            }else{
                setWakeStatusV168(if(trustedSessionActive())"STARTING" else "LOCKED_LISTENING")
                startListening()
            }
        }else requireFreshLoginOrStop()
    }

    override fun onBind(intent:Intent?):IBinder?=null

    override fun onDestroy(){
        recognizer?.destroy()
        recognizer=null
        serviceVoiceRequestId++
        runCatching{serviceCloudVoicePlayer?.stop()}
        runCatching{serviceCloudVoicePlayer?.release()}
        serviceCloudVoicePlayer=null
        runCatching{tts?.stop()}
        runCatching{tts?.shutdown()}
        controller?.release()
        controller=null
        controllerFuture?.cancel(true)
        controllerFuture=null
        toneRestoreJob?.cancel()
        offlineWakePrepareJobV188?.cancel()
        offlineWakePrepareJobV188=null
        runCatching{offlineWakeEngineV188?.stop()}
        offlineWakeEngineV188=null
        if(systemToneMutedByRs){
            val audio=getSystemService(AudioManager::class.java)
            runCatching{audio.adjustStreamVolume(AudioManager.STREAM_SYSTEM,AudioManager.ADJUST_UNMUTE,0)}
            systemToneMutedByRs=false
        }
        if(wakeLock?.isHeld==true)wakeLock?.release()
        wakeLock=null
        scope.cancel()
        super.onDestroy()
    }

    override fun onInit(status:Int){
        if(status==TextToSpeech.SUCCESS){
            applyLanguage()
            val pending=pendingHandoffSpeechV194
            if(pending.isNotBlank()){
                pendingHandoffSpeechV194=""
                scope.launch{
                    delay(120)
                    speak(pending,thenListen=true)
                }
            }
        }
    }

    private fun language():RsLang{
        val code=store.s("ai_voice_language_v161",store.s("lang","en"))
        return rsLangs.firstOrNull{it.code==code}?:rsLangs.first()
    }

    private fun suppressRecognizerToneV185(){
        if(!store.b("rs_voice_wake_silent_tones_v185",true))return
        val audio=getSystemService(AudioManager::class.java)
        val alreadyMuted=runCatching{audio.isStreamMute(AudioManager.STREAM_SYSTEM)}.getOrDefault(false)
        if(!alreadyMuted && !systemToneMutedByRs){
            runCatching{audio.adjustStreamVolume(AudioManager.STREAM_SYSTEM,AudioManager.ADJUST_MUTE,0)}
            systemToneMutedByRs=true
        }
        toneRestoreJob?.cancel()
        toneRestoreJob=scope.launch{
            delay(850)
            if(systemToneMutedByRs){
                runCatching{audio.adjustStreamVolume(AudioManager.STREAM_SYSTEM,AudioManager.ADJUST_UNMUTE,0)}
                systemToneMutedByRs=false
            }
        }
    }

    private fun defaultDashboardRouteV182():String=
        if(store.s("session_role","")=="trainer")"trainer" else "home"

    private fun roleAwareRouteV182(route:String):String{
        val trainer=store.s("session_role","")=="trainer"
        return if(!trainer)route else when(route){
            "music"->"music_admin"
            "guide"->"guide"
            "homework"->"homework_admin"
            "progress"->"progress_admin"
            else->route
        }
    }

    private fun trustedSessionActive():Boolean{
        val authMs=store.s("session_password_auth_ms","0").toLongOrNull()?:0L
        val age=System.currentTimeMillis()-authMs
        val role=store.s("session_role","")
        val activityMs=store.s("session_last_activity_ms",authMs.toString()).toLongOrNull()?:authMs
        val inactivityAge=System.currentTimeMillis()-activityMs
        val localFresh=authMs>0L &&
            age in 0..RS_TRUSTED_LOGIN_MS_V165 &&
            activityMs>0L &&
            inactivityAge in 0..RS_INACTIVITY_LOGOUT_MS_V165 &&
            role in setOf("student","trainer")
        val cloudFresh=rsSupabaseClientV60()?.auth?.currentUserOrNull()!=null
        return localFresh && cloudFresh
    }

    private fun recoverableLockedIdentityV186():Boolean{
        val cloudFresh=rsSupabaseClientV60()?.auth?.currentUserOrNull()!=null
        val role=store.s("session_role","").ifBlank{store.s("background_call_role","")}
        return cloudFresh && role in setOf("student","trainer")
    }

    private fun loginRequiredText():String=when(language().code){
        "nl"->"Je login is verlopen. Open RS KICKBOXING en log opnieuw in om RS Voice Wake te gebruiken."
        "pt"->"O teu login expirou. Abre o RS KICKBOXING e inicia sessão novamente para usar o RS Voice Wake."
        "es"->"Tu sesión ha caducado. Abre RS KICKBOXING e inicia sesión de nuevo para usar RS Voice Wake."
        "fr"->"Ta session a expiré. Ouvre RS KICKBOXING et reconnecte-toi pour utiliser RS Voice Wake."
        "de"->"Deine Anmeldung ist abgelaufen. Öffne RS KICKBOXING und melde dich erneut an, um RS Voice Wake zu nutzen."
        "it"->"La sessione è scaduta. Apri RS KICKBOXING e accedi di nuovo per usare RS Voice Wake."
        "pl"->"Sesja logowania wygasła. Otwórz RS KICKBOXING i zaloguj się ponownie, aby używać RS Voice Wake."
        "tr"->"Oturum süren doldu. RS Voice Wake'i kullanmak için RS KICKBOXING'i açıp tekrar giriş yap."
        else->"Your login has expired. Open RS KICKBOXING and sign in again to use RS Voice Wake."
    }

    private fun requireFreshLoginOrStop():Boolean{
        if(trustedSessionActive())return true
        setWakeStatusV168("LOGIN_REQUIRED")
        recognizer?.cancel()
        awakeUntil=0L
        awaitingPlaylistName=false
        val nm=getSystemService(NotificationManager::class.java)
        nm.notify(
            RS_VOICE_LOGIN_NOTIFICATION_ID_V165,
            loginRequiredNotification()
        )
        speak(loginRequiredText(),thenListen=false)
        scope.launch{
            delay(1200)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return false
    }

    private fun applyLanguage(){
        val engine=tts?:return
        val requested=language().locale
        val available=runCatching{engine.isLanguageAvailable(requested)}.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val effective=if(available>=TextToSpeech.LANG_AVAILABLE)requested else Locale.ENGLISH
        engine.language=effective

        val avatar=store.s("ai_avatar_gender_v161","FEMALE")
        val wantsMale=avatar=="MALE"
        engine.setSpeechRate(if(wantsMale).92f else .96f)
        engine.setPitch(if(wantsMale).82f else 1.14f)

        val overrideKey="ai_voice_override_v181_"+language().code+"_"+avatar.lowercase(Locale.ROOT)
        val overrideName=store.s(overrideKey,"")
        val overrideVoice=engine.voices?.firstOrNull{
            val n=it.name.lowercase(Locale.ROOT)
            val opposite=if(wantsMale)
                listOf("female","woman","femin","fem","f1","f2","samantha","victoria","karen","anna","susan","mujer","femme","frau","donna","mulher","kadin","kadın").any{hint->n.contains(hint)}
            else
                listOf("male","man","mascul","masc","m1","m2","david","daniel","thomas","george","hombre","homme","mann","uomo","homem","erkek").any{hint->n.contains(hint)}
            it.name==overrideName &&
            it.locale.language.equals(effective.language,true) &&
            !it.features.contains("notInstalled") &&
            !opposite
        }
        if(overrideVoice!=null){
            engine.voice=overrideVoice
            return
        }

        val all=engine.voices
            ?.filter{
                it.locale.language.equals(effective.language,true) &&
                !it.features.contains("notInstalled")
            }
            .orEmpty()
        val exact=all.filter{
            effective.country.isNotBlank() &&
            it.locale.country.equals(effective.country,true)
        }
        val candidates=if(exact.isNotEmpty())exact else all

        val maleHints=listOf(
            "male","mascul","masc","man","m1","m2","david","daniel","thomas","george",
            "hombre","homme","mann","uomo","homem","erkek"
        )
        val femaleHints=listOf(
            "female","femin","fem","woman","f1","f2","samantha","victoria","karen","anna","susan",
            "mujer","femme","frau","donna","mulher","kadin","kadın"
        )
        fun score(v:android.speech.tts.Voice):Int{
            val n=v.name.lowercase(Locale.ROOT)
            val male=maleHints.any{n.contains(it)}
            val female=femaleHints.any{n.contains(it)}
            var result=v.quality*4-v.latency
            if(wantsMale){
                if(male)result+=800
                if(female)result-=900
            }else{
                if(female)result+=800
                if(male)result-=900
            }
            if(v.locale==effective)result+=120
            if(v.isNetworkConnectionRequired)result-=80 else result+=150
            return result
        }
        val hinted=candidates.filter{v->
            val n=v.name.lowercase(Locale.ROOT)
            if(wantsMale)maleHints.any{n.contains(it)} else femaleHints.any{n.contains(it)}
        }
        val safe=candidates.filter{v->
            val n=v.name.lowercase(Locale.ROOT)
            if(wantsMale)femaleHints.none{n.contains(it)} else maleHints.none{n.contains(it)}
        }
        val pool=when{
            hinted.isNotEmpty()->hinted
            safe.isNotEmpty()->safe
            else->emptyList()
        }
        pool.maxByOrNull{score(it)}?.let{engine.voice=it}
    }

    private fun speak(text:String,thenListen:Boolean=true){
        if(text.isBlank())return
        val speechAvatar=store.s("ai_avatar_gender_v161","FEMALE")
        val speechLanguage=language()
        serviceSpeaking=true
        setWakeStatusV168("SPEAKING")
        suppressRecognizerToneV185()
        recognizer?.cancel()
        runCatching{offlineWakeEngineV188?.stop()}
        offlineWakeEngineV188=null
        runCatching{tts?.stop()}
        serviceUtteranceId=""
        serviceVoiceRequestId++
        val requestId=serviceVoiceRequestId
        runCatching{serviceCloudVoicePlayer?.stop()}
        runCatching{serviceCloudVoicePlayer?.release()}
        serviceCloudVoicePlayer=null

        fun finishSpeech(){
            if(requestId!=serviceVoiceRequestId)return
            serviceSpeaking=false
            if(thenListen)scope.launch{
                delay(650)
                startListening()
            }
        }

        fun speakDeviceFallback(){
            if(requestId!=serviceVoiceRequestId)return
            if(
                store.s("ai_avatar_gender_v161","FEMALE")!=speechAvatar ||
                language().code!=speechLanguage.code
            ){
                serviceVoiceRequestId++
                serviceSpeaking=false
                if(thenListen)scope.launch{delay(250);startListening()}
                return
            }
            applyLanguage()
            val utteranceId="rs-voice-wake-"+speechLanguage.code+"-"+speechAvatar+"-"+System.nanoTime()
            serviceUtteranceId=utteranceId
            tts?.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){
                override fun onStart(id:String?){}
                override fun onDone(id:String?){
                    if(id!=serviceUtteranceId || requestId!=serviceVoiceRequestId)return
                    serviceUtteranceId=""
                    finishSpeech()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id:String?){
                    if(id!=serviceUtteranceId || requestId!=serviceVoiceRequestId)return
                    serviceUtteranceId=""
                    finishSpeech()
                }
            })
            val result=tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,utteranceId)
            if(result==TextToSpeech.ERROR){
                serviceUtteranceId=""
                finishSpeech()
            }
        }

        val role=if(store.s("session_role","")=="trainer")RsRole.TRAINER else RsRole.STUDENT
        if(rsUsePremiumCloudVoiceV192(store,role) && RsSupabaseV60.configured){
            scope.launch{
                val clip=rsCloudVoiceClipV192(
                    context=this@RsVoiceWakeServiceV165,
                    text=text,
                    lang=speechLanguage,
                    avatar=speechAvatar,
                    voiceStyle=store.s(
                        "ai_cloud_voice_style_v193_"+speechLanguage.code+"_"+speechAvatar.lowercase(Locale.ROOT),
                        "natural"
                    )
                )
                if(
                    requestId!=serviceVoiceRequestId ||
                    store.s("ai_avatar_gender_v161","FEMALE")!=speechAvatar ||
                    language().code!=speechLanguage.code
                )return@launch
                clip.onSuccess{voiceClip->
                    val player=rsCreateCloudVoicePlayerV192(
                        context=this@RsVoiceWakeServiceV165,
                        localUri=voiceClip.localUri,
                        onStarted={setWakeStatusV168("SPEAKING_PREMIUM")},
                        onCompleted={
                            if(requestId==serviceVoiceRequestId){
                                serviceCloudVoicePlayer=null
                                finishSpeech()
                            }
                        },
                        onError={
                            if(requestId==serviceVoiceRequestId){
                                serviceCloudVoicePlayer=null
                                speakDeviceFallback()
                            }
                        }
                    )
                    if(player!=null && requestId==serviceVoiceRequestId){
                        serviceCloudVoicePlayer=player
                    }else if(player==null && requestId==serviceVoiceRequestId){
                        speakDeviceFallback()
                    }
                }.onFailure{
                    if(requestId==serviceVoiceRequestId)speakDeviceFallback()
                }
            }
        }else{
            speakDeviceFallback()
        }
    }

    private fun createRecognizer(){
        if(recognizer!=null)return
        if(!SpeechRecognizer.isRecognitionAvailable(this))return
        recognizer=SpeechRecognizer.createSpeechRecognizer(this).apply{
            setRecognitionListener(object:RecognitionListener{
                override fun onReadyForSpeech(params:android.os.Bundle?){}
                override fun onBeginningOfSpeech(){}
                override fun onRmsChanged(rmsdB:Float){}
                override fun onBufferReceived(buffer:ByteArray?){}
                override fun onEndOfSpeech(){}
                override fun onLanguageDetection(results:android.os.Bundle){
                    if(Build.VERSION.SDK_INT<34 || serviceSpeaking)return
                    val confidence=results.getInt("language_detection_confidence_level",0)
                    if(confidence<2)return
                    val tag=results.getString("detected_language").orEmpty()
                    val code=Locale.forLanguageTag(tag).language.lowercase(Locale.ROOT)
                    if(
                        code in setOf("en","nl","pt","es","fr","de","it","pl","tr") &&
                        code!=language().code
                    ){
                        store.ps("ai_voice_language_v161",code)
                        store.ps("lang_last_spoken_v182",code)
                        applyLanguage()
                        setWakeStatusV168("LANGUAGE_"+code.uppercase(Locale.ROOT))
                    }
                }
                override fun onError(error:Int){
                    if(serviceSpeaking)return
                    val label=when(error){
                        SpeechRecognizer.ERROR_AUDIO->"ERROR_AUDIO"
                        SpeechRecognizer.ERROR_CLIENT->"ERROR_CLIENT"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"ERROR_PERMISSION"
                        SpeechRecognizer.ERROR_NETWORK->"ERROR_NETWORK"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"ERROR_NETWORK_TIMEOUT"
                        SpeechRecognizer.ERROR_NO_MATCH->"NO_MATCH"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY->"ERROR_BUSY"
                        SpeechRecognizer.ERROR_SERVER->"ERROR_SERVER"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT->"NO_SPEECH"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED->"LANGUAGE_FALLBACK"
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE->"LANGUAGE_FALLBACK"
                        else->"ERROR_"+error
                    }
                    setWakeStatusV168(label)
                    scope.launch{
                        if(
                            error==SpeechRecognizer.ERROR_CLIENT ||
                            error==SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error==SpeechRecognizer.ERROR_SERVER ||
                            error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                            error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                        ){
                            if(
                                error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                                error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                            )wakeRecognitionFallback=true
                            runCatching{recognizer?.destroy()}
                            recognizer=null
                        }
                        delay(
                            when(error){
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT->if(MainActivity.isForeground)650 else 2200
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY->900
                                else->1200
                            }
                        )
                        startListening()
                    }
                }
                override fun onResults(results:android.os.Bundle?){
                    if(serviceSpeaking)return
                    val list=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val text=list.firstOrNull().orEmpty()
                    handleTranscript(text)
                }
                override fun onPartialResults(partialResults:android.os.Bundle?){
                    if(serviceSpeaking)return
                    val list=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val partial=list.firstOrNull().orEmpty()
                    if(System.currentTimeMillis()>awakeUntil && rsContainsWakePhraseV165(partial)){
                        recognizer?.cancel()
                        awakeUntil=System.currentTimeMillis()+45_000L
                        setWakeStatusV168("HEARD_WAKE")
                        val command=rsStripWakePhraseV165(partial)
                        store.pb("ai_immersive_v171",true)
                        store.pb("ai_start_listening_v168",true)
                        if(command.isBlank()){
                            if(!MainActivity.isForeground)requestRouteOpenV182(defaultDashboardRouteV182())
                            speak(rsVoiceGreetingV165(language().code),thenListen=true)
                        }else{
                            scope.launch{
                                delay(80)
                                handleTranscript(command)
                            }
                        }
                    }
                }
                override fun onEvent(eventType:Int,params:android.os.Bundle?){}
            })
        }
    }

    private fun prepareSilentWakeV188(){
        if(silentWakePreparingV188 || silentWakeSessionFailedV188)return
        if(RsOfflineWakeModelV188.installed(this)){
            store.pb("rs_silent_wake_model_ready_v188",true)
            startSilentWakeV188()
            return
        }
        silentWakePreparingV188=true
        setWakeStatusV168("SILENT_WAKE_PREPARING")
        offlineWakePrepareJobV188?.cancel()
        offlineWakePrepareJobV188=scope.launch{
            val result=RsOfflineWakeModelV188.ensure(this@RsVoiceWakeServiceV165){progress->
                store.ps("rs_silent_wake_download_progress_v188",progress.toString())
                setWakeStatusV168("SILENT_WAKE_DOWNLOAD_"+progress)
            }
            silentWakePreparingV188=false
            result.onSuccess{
                store.pb("rs_silent_wake_model_ready_v188",true)
                store.ps("rs_silent_wake_download_progress_v188","100")
                silentWakeSessionFailedV188=false
                startSilentWakeV188()
            }.onFailure{
                store.pb("rs_silent_wake_model_ready_v188",false)
                silentWakeSessionFailedV188=true
                setWakeStatusV168("SILENT_WAKE_PREPARE_FAILED")
                // Fall back to Android recognition for this service session only.
                startListening()
            }
        }
    }

    private fun startSilentWakeV188(){
        if(serviceSpeaking || System.currentTimeMillis()<=awakeUntil)return
        if(!RsOfflineWakeModelV188.installed(this)){
            prepareSilentWakeV188()
            return
        }
        runCatching{recognizer?.cancel()}
        runCatching{offlineWakeEngineV188?.stop()}
        offlineWakeEngineV188=RsOfflineWakeEngineV188(
            context=this,
            onWake={heard->
                scope.launch{
                    if(serviceSpeaking)return@launch
                    awakeUntil=System.currentTimeMillis()+45_000L
                    setWakeStatusV168("HEARD_WAKE")
                    store.pb("ai_immersive_v171",true)
                    store.pb("ai_start_listening_v168",true)

                    val command=rsStripWakePhraseV165(heard)
                    val requested=if(command.isBlank())null
                    else rsVoiceRouteV167(command)?:rsVoiceDynamicRouteV184(command,language())

                    if(requested!=null){
                        val resolved=roleAwareRouteV182(requested)
                        requestRouteOpenV182(resolved)
                        val title=rsRouteTitle(
                            language(),
                            resolved,
                            resolved.replace('_',' ').replaceFirstChar{it.uppercase()}
                        )
                        speak(
                            when(language().code){
                                "nl"->"Ik open "+title+"."
                                "pt"->"Vou abrir "+title+"."
                                "es"->"Voy a abrir "+title+"."
                                "fr"->"J’ouvre "+title+"."
                                "de"->"Ich öffne "+title+"."
                                "it"->"Apro "+title+"."
                                "pl"->"Otwieram "+title+"."
                                "tr"->title+" açılıyor."
                                else->"Opening "+title+"."
                            },
                            thenListen=true
                        )
                    }else{
                        if(!MainActivity.isForeground)requestRouteOpenV182(defaultDashboardRouteV182())
                        speak(rsVoiceGreetingV165(language().code),thenListen=true)
                    }
                }
            },
            onState={state->setWakeStatusV168(state)}
        ).also{engine->
            engine.start().onFailure{
                silentWakeSessionFailedV188=true
                setWakeStatusV168("SILENT_WAKE_START_FAILED")
                runCatching{engine.stop()}
                offlineWakeEngineV188=null
                startListening()
            }
        }
    }

    private fun startListening(){
        if(pausedForForegroundAiV197)return
        if(serviceSpeaking)return
        if(!trustedSessionActive() && !recoverableLockedIdentityV186()){
            requireFreshLoginOrStop()
            return
        }
        if(
            ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
            !=PackageManager.PERMISSION_GRANTED
        )return

        if(
            !MainActivity.isForeground &&
            System.currentTimeMillis()>awakeUntil &&
            store.b("rs_voice_wake_silent_engine_v188",true) &&
            !silentWakeSessionFailedV188
        ){
            if(RsOfflineWakeModelV188.installed(this))startSilentWakeV188()
            else prepareSilentWakeV188()
            return
        }

        runCatching{offlineWakeEngineV188?.stop()}
        offlineWakeEngineV188=null
        createRecognizer()
        val lang=language()
        val recognitionLocale=if(wakeRecognitionFallback)Locale.getDefault() else lang.locale
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognitionLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
            if(Build.VERSION.SDK_INT>=34){
                val allowed=arrayListOf(
                    "en-US","nl-NL","pt-PT","es-ES","fr-FR","de-DE","it-IT","pl-PL","tr-TR"
                )
                putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION",true)
                putStringArrayListExtra(
                    "android.speech.extra.LANGUAGE_DETECTION_ALLOWED_LANGUAGES",
                    allowed
                )
                putExtra("android.speech.extra.ENABLE_LANGUAGE_SWITCH","balanced")
                putStringArrayListExtra(
                    "android.speech.extra.LANGUAGE_SWITCH_ALLOWED_LANGUAGES",
                    allowed
                )
            }
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,12000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,8000L)
        }
        setWakeStatusV168("READY")
        suppressRecognizerToneV185()
        runCatching{recognizer?.startListening(intent)}
            .onFailure{
                setWakeStatusV168("START_FAILED")
                scope.launch{
                    runCatching{recognizer?.destroy()}
                    recognizer=null
                    delay(1000)
                    startListening()
                }
            }
    }


    private fun openRouteV166(route:String){
        runCatching{
            startActivity(
                Intent(this,MainActivity::class.java).apply{
                    action="com.rskickbox.app.OPEN_RS_ROUTE"
                    putExtra("route",route)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            )
        }
    }

    private fun requestRouteOpenV182(route:String){
        val intent=Intent(this,MainActivity::class.java).apply{
            action="com.rskickbox.app.OPEN_RS_ROUTE"
            putExtra("route",route)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        val taskRestored=MainActivity.bringTaskToFrontFromVoice()
        var launchAttempted=taskRestored

        if(taskRestored){
            openRouteV166(route)
        }else{
            // A foreground microphone service is allowed to request a normal
            // app handoff on some Android/device combinations. Try this first.
            launchAttempted=runCatching{
                startActivity(intent)
                true
            }.getOrDefault(false)
            if(!launchAttempted && Build.VERSION.SDK_INT>=34){
                val pending=PendingIntent.getActivity(
                    this,
                    1880+route.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val options=ActivityOptions.makeBasic().apply{
                    @Suppress("DEPRECATION")
                    setPendingIntentBackgroundActivityStartMode(
                        if(Build.VERSION.SDK_INT>=36)
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                        else
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                launchAttempted=runCatching{
                    pending.send(this,0,intent,null,null,null,options.toBundle())
                    true
                }.getOrDefault(false)
            }
        }

        scope.launch{
            delay(if(taskRestored)450 else 900)
            if(!MainActivity.isForeground){
                val pending=PendingIntent.getActivity(
                    this@RsVoiceWakeServiceV165,
                    1820+route.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val title=rsRouteTitle(
                    language(),
                    route,
                    route.replace('_',' ').replaceFirstChar{it.uppercase()}
                )
                val text=when(language().code){
                    "nl"->"RS heeft je gehoord · tik om "+title+" te openen"
                    "pt"->"A RS ouviu-te · toca para abrir "+title
                    "es"->"RS te ha escuchado · toca para abrir "+title
                    "fr"->"RS t’a entendu · touche pour ouvrir "+title
                    "de"->"RS hat dich gehört · tippe, um "+title+" zu öffnen"
                    "it"->"RS ti ha sentito · tocca per aprire "+title
                    "pl"->"RS cię usłyszał · dotknij, aby otworzyć "+title
                    "tr"->"RS seni duydu · "+title+" açmak için dokun"
                    else->"RS heard you · tap to open "+title
                }
                setWakeStatusV168(if(launchAttempted)"WAKE_HANDOFF_BLOCKED" else "WAKE_HANDOFF_FAILED")
                getSystemService(NotificationManager::class.java).notify(
                    RS_VOICE_WAKE_ACTION_NOTIFICATION_ID_V182,
                    NotificationCompat.Builder(this@RsVoiceWakeServiceV165,RS_VOICE_WAKE_ACTION_CHANNEL_V182)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("RS KICKBOXING")
                        .setContentText(text)
                        .setContentIntent(pending)
                        .setAutoCancel(true)
                        .setSilent(true)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .build()
                )
            }else{
                setWakeStatusV168("WAKE_ROUTE_OPENED")
            }
        }
    }

    private fun openSystemActionV182(actionName:String){
        runCatching{
            startActivity(
                Intent(this,MainActivity::class.java).apply{
                    action=actionName
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }

    private fun adjustVolumeV166(direction:Int){
        val audio=getSystemService(AudioManager::class.java)
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if(direction>0)AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun playTrackByNameV166(text:String):Boolean{
        val lower=text.lowercase(Locale.ROOT)
        val tracks=rsTrackMapV165(store)
        val match=tracks.entries
            .sortedByDescending{it.value.length}
            .firstOrNull{lower.contains(it.value.lowercase(Locale.ROOT))}
            ?:return false
        val p=controller?:return false
        val item=MediaItem.Builder()
            .setUri(match.key)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(match.value)
                    .setArtist("RS KICKBOXING")
                    .build()
            )
            .build()
        p.setMediaItem(item)
        p.prepare()
        p.play()
        return true
    }

    private fun playPlaylistByNameV166(text:String):String?{
        val raw=store.s("music_named_playlists_v108","")
        val arr=runCatching{if(raw.isBlank())JSONArray() else JSONArray(raw)}.getOrDefault(JSONArray())
        val lower=text.lowercase(Locale.ROOT)
        for(i in 0 until arr.length()){
            val o=arr.optJSONObject(i)?:continue
            val name=o.optString("name")
            if(name.isBlank()||!lower.contains(name.lowercase(Locale.ROOT)))continue
            val uris=o.optJSONArray("uris")?:JSONArray()
            val titleMap=rsTrackMapV165(store)
            val items=buildList{
                for(j in 0 until uris.length()){
                    val uri=uris.optString(j)
                    if(uri.isBlank())continue
                    add(
                        MediaItem.Builder()
                            .setUri(uri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(titleMap[uri]?:File(Uri.parse(uri).path.orEmpty()).nameWithoutExtension.ifBlank{"RS Music"})
                                    .setArtist("RS KICKBOXING")
                                    .build()
                            )
                            .build()
                    )
                }
            }
            if(items.isEmpty())return null
            val p=controller?:return null
            p.setMediaItems(items)
            p.prepare()
            p.play()
            store.ps("music_active_playlist_v108",name)
            return name
        }
        return null
    }

    private fun handleTranscript(text:String){
        val trusted=trustedSessionActive()
        val recoverable=recoverableLockedIdentityV186()
        if(!trusted && !recoverable){
            if(!requireFreshLoginOrStop())return
        }
        if(text.isBlank()){
            startListening()
            return
        }

        if(!trusted && recoverable){
            if(!rsContainsWakePhraseV165(text)){
                setWakeStatusV168("LOCKED_LISTENING")
                scope.launch{
                    delay(900)
                    startListening()
                }
                return
            }
            val detectedLanguage=rsDetectSpokenLanguageV182(text,language().code)
            if(detectedLanguage!=language().code){
                store.ps("ai_voice_language_v161",detectedLanguage)
                store.ps("lang_last_spoken_v182",detectedLanguage)
                applyLanguage()
            }
            val command=rsStripWakePhraseV165(text)
            val requested=rsVoiceRouteV167(command)
                ?:rsVoiceDynamicRouteV184(command,language())
                ?:defaultDashboardRouteV182()
            val role=store.s("session_role","").ifBlank{store.s("background_call_role","")}
            if(role in setOf("student","trainer"))store.ps("session_role",role)
            requestRouteOpenV182(roleAwareRouteV182(requested))
            setWakeStatusV168("BIOMETRIC_REQUIRED")
            return
        }

        val now=System.currentTimeMillis()
        store.ps("session_last_activity_ms",now.toString())
        val detectedLanguage=rsDetectSpokenLanguageV182(text,language().code)
        if(detectedLanguage!=language().code){
            store.ps("ai_voice_language_v161",detectedLanguage)
            store.ps("lang_last_spoken_v182",detectedLanguage)
            applyLanguage()
        }

        if(awaitingPlaylistName){
            val name=text.trim().take(60)
            awaitingPlaylistName=false
            val ok=rsSaveVoicePlaylistV165(this,name,10)
            speak(
                if(ok)rsPlaylistSavedV165(language().code,name)
                else when(language().code){
                    "nl"->"Ik heb nog niet genoeg recente muziek gevonden om die playlist te maken."
                    "pt"->"Ainda não encontrei músicas recentes suficientes para criar essa playlist."
                    "es"->"Todavía no he encontrado suficientes canciones recientes para crear esa playlist."
                    "fr"->"Je n’ai pas encore trouvé assez de morceaux récents pour créer cette playlist."
                    "de"->"Ich habe noch nicht genug kürzlich gespielte Titel gefunden, um diese Playlist zu erstellen."
                    "it"->"Non ho ancora trovato abbastanza brani recenti per creare questa playlist."
                    "pl"->"Nie znalazłem jeszcze wystarczającej liczby ostatnich utworów, aby utworzyć tę playlistę."
                    "tr"->"Bu çalma listesini oluşturmak için henüz yeterli son parça bulamadım."
                    else->"I could not find enough recent music to create that playlist yet."
                }
            )
            return
        }

        var commandText=text
        if(now>awakeUntil){
            if(MainActivity.isForeground){
                // Hands-free foreground mode: while the app is visible, supported
                // commands do not require a wake phrase or top-mic tap.
                awakeUntil=now+10L*60L*1000L
                setWakeStatusV168("FOREGROUND_HANDSFREE")
            }else if(rsContainsWakePhraseV165(text)){
                awakeUntil=now+45_000L
                setWakeStatusV168("HEARD_WAKE")
                commandText=rsStripWakePhraseV165(text)
                store.pb("ai_immersive_v171",true)
                store.pb("ai_start_listening_v168",true)
                recognizer?.cancel()
                if(commandText.isBlank()){
                    requestRouteOpenV182(defaultDashboardRouteV182())
                    speak(rsVoiceGreetingV165(language().code),thenListen=true)
                    return
                }
            }else{
                startListening()
                return
            }
        }

        awakeUntil=if(MainActivity.isForeground)now+10L*60L*1000L else now+45_000L

        rsVoicePageCommandV196(commandText)?.let{pageCommand->
            RsVoicePageBusV196.send(pageCommand)
            speak(
                when(language().code){
                    "nl"->if(pageCommand==RsVoicePageCommandV196.SCROLL_UP)"Ik scroll omhoog." else "Ik scroll omlaag."
                    "pt"->if(pageCommand==RsVoicePageCommandV196.SCROLL_UP)"Vou subir a página." else "Vou descer a página."
                    "es"->if(pageCommand==RsVoicePageCommandV196.SCROLL_UP)"Desplazo la página hacia arriba." else "Desplazo la página hacia abajo."
                    "fr"->if(pageCommand==RsVoicePageCommandV196.SCROLL_UP)"Je fais défiler vers le haut." else "Je fais défiler vers le bas."
                    else->if(pageCommand==RsVoicePageCommandV196.SCROLL_UP)"Scrolling up." else "Scrolling down."
                }
            )
            return
        }

        val lowerCommand=commandText.lowercase(Locale.ROOT)
        if(rsVoiceReadPageRequestedV196(commandText)){
            readCurrentPageV196(false)
            return
        }
        if(listOf("continue reading","read next part","continue page","verder lezen","lees verder","continuar a ler","seguir leyendo","continue la lecture").any{lowerCommand.contains(it)}){
            readCurrentPageV196(true)
            return
        }

        rsVoiceMessageReadRequestV196(commandText)?.let{person->
            readMessagesFromV196(person)
            return
        }

        if(rsVoiceDeleteLastMessageRequestedV196(commandText)){
            val message=pendingVoiceMessageV196
            if(message==null){
                speak("There is no recently read message selected.")
            }else{
                scope.launch{
                    rsHideCoachMessageV156(message.id)
                        .onSuccess{
                            pendingVoiceMessageV196=null
                            speak(
                                when(language().code){
                                    "nl"->"Het bericht is voor jou verwijderd."
                                    "pt"->"A mensagem foi eliminada para ti."
                                    "es"->"El mensaje se eliminó para ti."
                                    "fr"->"Le message a été supprimé pour toi."
                                    else->"The message was deleted for you."
                                }
                            )
                        }
                        .onFailure{speak("I could not delete that message.")}
                }
            }
            return
        }

        if(rsVoiceSaveLastMessageRequestedV196(commandText)){
            val message=pendingVoiceMessageV196
            if(message==null){
                speak("There is no recently read message selected.")
            }else{
                val sender=pendingVoiceMessageSenderV196.ifBlank{"Chat"}
                scope.launch{
                    val savedText=rsSaveChatMessageToGalleryV196(store,message,sender)
                    if(!message.mediaPath.isNullOrBlank()&&!message.mediaKind.isNullOrBlank()){
                        rsChatMediaLocalUriV92(this@RsVoiceWakeServiceV165,message.mediaPath)
                            .onSuccess{local->
                                rsSaveChatMediaToGalleryV163(
                                    this@RsVoiceWakeServiceV165,store,local,message.mediaKind,message.mediaName
                                )
                            }
                    }
                    savedText
                        .onSuccess{
                            speak(
                                when(language().code){
                                    "nl"->"Het bericht is opgeslagen in de berichten-sectie van RS Chat Gallery."
                                    "pt"->"A mensagem foi guardada na secção de mensagens da RS Chat Gallery."
                                    "es"->"El mensaje se guardó en la sección de mensajes de RS Chat Gallery."
                                    "fr"->"Le message a été enregistrée dans la section messages de RS Chat Gallery."
                                    else->"The message was saved in the Messages section of RS Chat Gallery."
                                }
                            )
                        }
                        .onFailure{speak("I could not save that message.")}
                }
            }
            return
        }

        when(rsVoiceSystemCommandV182(commandText)){
            "OPEN"->{
                requestRouteOpenV182(defaultDashboardRouteV182())
                speak(
                    when(language().code){
                        "nl"->"Ik open RS KICKBOXING."
                        "pt"->"Vou abrir o RS KICKBOXING."
                        "es"->"Voy a abrir RS KICKBOXING."
                        "fr"->"J’ouvre RS KICKBOXING."
                        "de"->"Ich öffne RS KICKBOXING."
                        "it"->"Apro RS KICKBOXING."
                        "pl"->"Otwieram RS KICKBOXING."
                        "tr"->"RS KICKBOXING açılıyor."
                        else->"Opening RS KICKBOXING."
                    }
                )
                return
            }
            "MINIMIZE"->{
                MainActivity.moveToBackgroundFromVoice()
                speak(
                    when(language().code){
                        "nl"->"Oké. Ik zet RS op de achtergrond en blijf luisteren."
                        "pt"->"Está bem. Vou colocar a RS em segundo plano e continuo a ouvir."
                        "es"->"De acuerdo. Pondré RS en segundo plano y seguiré escuchando."
                        "fr"->"D’accord. Je mets RS en arrière-plan et je continue d’écouter."
                        "de"->"Okay. Ich lege RS in den Hintergrund und höre weiter zu."
                        else->"Okay. I’ll move RS to the background and keep listening."
                    }
                )
                return
            }
            "CLOSE"->{
                awakeUntil=System.currentTimeMillis()+45_000L
                store.pb("ai_immersive_v171",false)
                store.pb("ai_start_listening_v168",false)
                speak(
                    when(language().code){
                        "nl"->"Ik sluit het RS-venster en blijf nog even actief luisteren op de achtergrond."
                        "pt"->"Vou fechar a janela RS e continuar a ouvir ativamente em segundo plano por alguns instantes."
                        "es"->"Cerraré la ventana de RS y seguiré escuchando activamente en segundo plano durante un momento."
                        "fr"->"Je ferme la fenêtre RS et je continue à écouter activement en arrière-plan pendant un moment."
                        "de"->"Ich schließe das RS-Fenster und höre im Hintergrund noch eine Weile aktiv weiter."
                        "it"->"Chiudo la finestra RS e continuo ad ascoltare attivamente in background per un po’."
                        "pl"->"Zamknę okno RS i przez chwilę będę aktywnie słuchać w tle."
                        "tr"->"RS penceresini kapatıyorum ve bir süre arka planda aktif olarak dinlemeye devam edeceğim."
                        else->"I’ll close the RS window and keep actively listening in the background for a while."
                    },
                    thenListen=true
                )
                scope.launch{
                    delay(900)
                    MainActivity.closeTaskFromVoice()
                }
                return
            }
            "LOGOUT"->{
                awakeUntil=0L
                store.ps("session_password_auth_ms","0")
                store.ps("session_last_activity_ms","0")
                store.ps("session_last_route","")
                store.ps("session_role","")
                store.ps("background_call_role","")
                store.pb("rs_voice_wake_enabled_v165",false)
                scope.launch{runCatching{rsCloudLogoutV63()}}
                speak(
                    when(language().code){
                        "nl"->"Je wordt uitgelogd."
                        "pt"->"Vou terminar a tua sessão."
                        "es"->"Voy a cerrar tu sesión."
                        "fr"->"Je vais te déconnecter."
                        "de"->"Du wirst abgemeldet."
                        else->"I’m signing you out."
                    },
                    thenListen=false
                )
                scope.launch{
                    delay(500)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    if(MainActivity.isForeground){
                        openRouteV166("home")
                    }
                }
                return
            }
        }

        when(val assistantIntent=rsAiPlatformIntentV171(commandText,language())){
            is RsAiPlatformIntentV171.ChangeAvatar->{
                serviceVoiceRequestId++
                runCatching{serviceCloudVoicePlayer?.stop()}
                runCatching{serviceCloudVoicePlayer?.release()}
                serviceCloudVoicePlayer=null
                runCatching{tts?.stop()}
                serviceSpeaking=false
                store.ps("ai_avatar_gender_v161",assistantIntent.avatar)
                applyLanguage()
                speak(
                    if(assistantIntent.avatar=="MALE")rsAiActionV184(language().code,"marcus_active")
                    else rsAiActionV184(language().code,"sofia_active")
                )
                return
            }
            is RsAiPlatformIntentV171.ChangeVoiceStyle->{
                val avatar=store.s("ai_avatar_gender_v161","FEMALE")
                val key="ai_cloud_voice_style_v193_"+language().code+"_"+avatar.lowercase(Locale.ROOT)
                val styles=listOf("natural","calm","energetic","soft","direct","deep")
                val current=store.s(key,"natural")
                val index=styles.indexOf(current).takeIf{it>=0}?:0
                val next=styles[(index+1)%styles.size]
                store.ps(key,next)
                speak(
                    when(language().code){
                        "nl"->"Stemstijl gewijzigd naar "+next+". "+if(avatar=="MALE")"Marcus blijft mannelijk." else "Sofia blijft vrouwelijk."
                        "pt"->"Estilo de voz alterado para "+next+". "+if(avatar=="MALE")"O Marcus continua com voz masculina." else "A Sofia continua com voz feminina."
                        "es"->"Estilo de voz cambiado a "+next+". "+if(avatar=="MALE")"Marcus sigue con voz masculina." else "Sofia sigue con voz femenina."
                        "fr"->"Style de voix changé vers "+next+". "+if(avatar=="MALE")"Marcus reste masculin." else "Sofia reste féminine."
                        "de"->"Stimmstil auf "+next+" geändert. "+if(avatar=="MALE")"Marcus bleibt männlich." else "Sofia bleibt weiblich."
                        else->"Voice style changed to "+next+". "+if(avatar=="MALE")"Marcus stays male." else "Sofia stays female."
                    }
                )
                return
            }
            is RsAiPlatformIntentV171.ChangeLanguage->{
                store.ps("ai_voice_language_v161",assistantIntent.code)
                store.ps("lang_last_spoken_v182",assistantIntent.code)
                applyLanguage()
                speak(
                    when(assistantIntent.code){
                        "nl"->"Natuurlijk. Ik spreek nu Nederlands."
                        "pt"->"Claro. Agora vou falar em português."
                        "es"->"Claro. Ahora hablaré en español."
                        "fr"->"Bien sûr. Je parle maintenant français."
                        "de"->"Natürlich. Ich spreche jetzt Deutsch."
                        "it"->"Certo. Ora parlerò in italiano."
                        "pl"->"Oczywiście. Teraz będę mówić po polsku."
                        "tr"->"Elbette. Artık Türkçe konuşacağım."
                        else->"Of course. I’ll speak English now."
                    }
                )
                return
            }
            is RsAiPlatformIntentV171.CloseAi->{
                requestRouteOpenV182(defaultDashboardRouteV182())
                speak(
                    when(language().code){
                        "nl"->"Ik sluit AI en ga terug naar het dashboard."
                        "pt"->"Vou fechar a IA e voltar ao painel."
                        "es"->"Cerraré la IA y volveré al panel."
                        "fr"->"Je ferme l’IA et je retourne au tableau de bord."
                        "de"->"Ich schließe die KI und gehe zum Dashboard zurück."
                        else->"Closing AI and returning to the dashboard."
                    }
                )
                return
            }
            is RsAiPlatformIntentV171.DirectCall->{
                scope.launch{
                    val contacts=rsChatContactsV125().getOrElse{emptyList()}
                    val person=rsAiBestContactV194(contacts,assistantIntent.person)
                    if(person==null){
                        speak(
                            when(language().code){
                                "nl"->"Ik kon "+assistantIntent.person+" niet vinden in je RS-contacten."
                                "pt"->"Não encontrei "+assistantIntent.person+" nos teus contactos RS."
                                "es"->"No encontré a "+assistantIntent.person+" en tus contactos RS."
                                "fr"->"Je n’ai pas trouvé "+assistantIntent.person+" dans tes contacts RS."
                                "de"->"Ich konnte "+assistantIntent.person+" in deinen RS-Kontakten nicht finden."
                                else->"I couldn’t find "+assistantIntent.person+" in your RS contacts."
                            }
                        )
                        return@launch
                    }

                    val needsMic=ActivityCompat.checkSelfPermission(this@RsVoiceWakeServiceV165,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED
                    val needsCamera=assistantIntent.type=="VIDEO" &&
                        ActivityCompat.checkSelfPermission(this@RsVoiceWakeServiceV165,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED
                    if(needsMic||needsCamera){
                        store.ps("chat_open_peer_id_v156",person.userId)
                        requestRouteOpenV182("coachchat")
                        speak(
                            when(language().code){
                                "nl"->"Ik heb "+person.displayName+" gevonden. Open RS Chat om de benodigde toestemming één keer te geven."
                                "pt"->"Encontrei "+person.displayName+". Abre o RS Chat para dar a permissão necessária uma vez."
                                "es"->"Encontré a "+person.displayName+". Abre RS Chat para dar el permiso necesario una vez."
                                "fr"->"J’ai trouvé "+person.displayName+". Ouvre RS Chat pour accorder l’autorisation nécessaire une fois."
                                else->"I found "+person.displayName+". Open RS Chat once to grant the required permission."
                            }
                        )
                        return@launch
                    }

                    rsStartDirectCallV131(person.userId,assistantIntent.type)
                        .onSuccess{
                            store.ps("chat_open_peer_id_v156",person.userId)
                            requestRouteOpenV182("coachchat")
                            speak(
                                when(language().code){
                                    "nl"->if(assistantIntent.type=="VIDEO")"Ik start een videogesprek met "+person.displayName+"." else "Ik bel "+person.displayName+"."
                                    "pt"->if(assistantIntent.type=="VIDEO")"Vou iniciar uma videochamada com "+person.displayName+"." else "Vou ligar para "+person.displayName+"."
                                    "es"->if(assistantIntent.type=="VIDEO")"Voy a iniciar una videollamada con "+person.displayName+"." else "Voy a llamar a "+person.displayName+"."
                                    "fr"->if(assistantIntent.type=="VIDEO")"Je lance un appel vidéo avec "+person.displayName+"." else "J’appelle "+person.displayName+"."
                                    "de"->if(assistantIntent.type=="VIDEO")"Ich starte einen Videoanruf mit "+person.displayName+"." else "Ich rufe "+person.displayName+" an."
                                    else->if(assistantIntent.type=="VIDEO")"Starting a video call with "+person.displayName+"." else "Calling "+person.displayName+"."
                                }
                            )
                        }
                        .onFailure{
                            speak(it.message?:"The call could not be started.")
                        }
                }
                return
            }
            is RsAiPlatformIntentV171.GroupVideoCall->{
                scope.launch{
                    if(store.s("session_role","")!="trainer"){
                        speak(
                            when(language().code){
                                "nl"->"Groepsvideo wordt momenteel door de trainer gehost."
                                "pt"->"A videochamada de grupo é atualmente alojada pelo treinador."
                                "es"->"La videollamada grupal actualmente la organiza el entrenador."
                                "fr"->"La vidéo de groupe est actuellement hébergée par l’entraîneur."
                                else->"Group video rooms are currently hosted by the trainer."
                            }
                        )
                        return@launch
                    }

                    val contacts=rsChatContactsV125().getOrElse{emptyList()}
                    val (people,missing)=rsAiResolveContactsV194(contacts,assistantIntent.people)
                    if(missing.isNotEmpty()){
                        speak(
                            when(language().code){
                                "nl"->"Ik kon deze RS-contacten niet vinden: "+missing.joinToString(", ")+"."
                                "pt"->"Não encontrei estes contactos RS: "+missing.joinToString(", ")+"."
                                "es"->"No encontré estos contactos RS: "+missing.joinToString(", ")+"."
                                "fr"->"Je n’ai pas trouvé ces contacts RS : "+missing.joinToString(", ")+"."
                                else->"I couldn’t find these RS contacts: "+missing.joinToString(", ")+"."
                            }
                        )
                        return@launch
                    }
                    if(people.size<2){
                        speak("I need at least two other RS contacts for a group video call.")
                        return@launch
                    }

                    val needsMic=ActivityCompat.checkSelfPermission(this@RsVoiceWakeServiceV165,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED
                    val needsCamera=ActivityCompat.checkSelfPermission(this@RsVoiceWakeServiceV165,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED
                    if(needsMic||needsCamera){
                        requestRouteOpenV182("coachchat")
                        speak(
                            when(language().code){
                                "nl"->"Ik heb de deelnemers gevonden. Open RS Chat om microfoon- en cameratoegang één keer toe te staan."
                                "pt"->"Encontrei os participantes. Abre o RS Chat para autorizar o microfone e a câmara uma vez."
                                "es"->"Encontré a los participantes. Abre RS Chat para autorizar micrófono y cámara una vez."
                                else->"I found the participants. Open RS Chat once to grant microphone and camera permission."
                            }
                        )
                        return@launch
                    }

                    val title="RS Group Video · "+people.joinToString(" · "){it.displayName}
                    rsCreateVideoRoomV136(title,people.map{it.userId})
                        .onSuccess{
                            requestRouteOpenV182("coachchat")
                            speak(
                                when(language().code){
                                    "nl"->"Ik start de groepsvideo met "+people.joinToString(", "){it.displayName}+"."
                                    "pt"->"Vou iniciar a videochamada de grupo com "+people.joinToString(", "){it.displayName}+"."
                                    "es"->"Voy a iniciar la videollamada grupal con "+people.joinToString(", "){it.displayName}+"."
                                    "fr"->"Je lance la vidéo de groupe avec "+people.joinToString(", "){it.displayName}+"."
                                    else->"Starting the group video with "+people.joinToString(", "){it.displayName}+"."
                                }
                            )
                        }
                        .onFailure{
                            speak(it.message?:"The group video could not be started.")
                        }
                }
                return
            }
            is RsAiPlatformIntentV171.CommandGuide->{
                requestRouteOpenV182(if(store.s("session_role","")=="trainer")"guide" else "student_guide")
                speak(
                    when(language().code){
                        "nl"->"Ik open de AI-spraakcommando’s in de App Gids."
                        "pt"->"Vou abrir os comandos de voz da IA no Guia da App."
                        "es"->"Abriré los comandos de voz de IA en la Guía de la App."
                        "fr"->"J’ouvre les commandes vocales IA dans le Guide de l’App."
                        "de"->"Ich öffne die KI-Sprachbefehle in der App-Anleitung."
                        else->"Opening AI voice commands in the App Guide."
                    }
                )
                return
            }
            else->{}
        }

        val requestedRoute=rsVoiceRouteV167(commandText)
            ?:rsVoiceDynamicRouteV184(commandText,language())
        if(requestedRoute!=null){
            val resolvedRoute=roleAwareRouteV182(requestedRoute)
            requestRouteOpenV182(resolvedRoute)
            val title=rsRouteTitle(
                language(),
                resolvedRoute,
                resolvedRoute.replace('_',' ').replaceFirstChar{it.uppercase()}
            )
            speak(
                when(language().code){
                    "nl"->"Ik open "+title+"."
                    "pt"->"Vou abrir "+title+"."
                    "es"->"Voy a abrir "+title+"."
                    "fr"->"J’ouvre "+title+"."
                    "de"->"Ich öffne "+title+"."
                    "it"->"Apro "+title+"."
                    "pl"->"Otwieram "+title+"."
                    "tr"->title+" açılıyor."
                    else->"Opening "+title+"."
                }
            )
            return
        }

        val playlistName=playPlaylistByNameV166(commandText)
        if(playlistName!=null){
            speak(
                when(language().code){
                    "nl"->"Playlist "+playlistName+" gestart."
                    "pt"->"Playlist "+playlistName+" iniciada."
                    "es"->"Playlist "+playlistName+" iniciada."
                    "fr"->"Playlist "+playlistName+" lancée."
                    else->"Playlist "+playlistName+" started."
                }
            )
            return
        }

        if(
            (commandText.contains("play",true)||commandText.contains("speel",true)||
             commandText.contains("tocar",true)||commandText.contains("reproducir",true)||
             commandText.contains("joue",true)||commandText.contains("abspielen",true)||
             commandText.contains("riproduci",true)||commandText.contains("odtwórz",true)||
             commandText.contains("çal",true)) &&
            playTrackByNameV166(commandText)
        ){
            speak(
                when(language().code){
                    "nl"->"Nummer gestart."
                    "pt"->"Música iniciada."
                    "es"->"Canción iniciada."
                    "fr"->"Morceau lancé."
                    "de"->"Titel gestartet."
                    "it"->"Brano avviato."
                    "pl"->"Utwór uruchomiony."
                    "tr"->"Parça başlatıldı."
                    else->"Track started."
                }
            )
            return
        }

        when(rsVoiceMusicCommandV165(commandText)){
            RsVoiceMusicCommandV165.PLAY->{
                ensureMusicLoadedAndPlay()
                speak(
                    when(language().code){
                        "nl"->"Muziek gestart."
                        "pt"->"Música iniciada."
                        "es"->"Música iniciada."
                        "fr"->"Musique lancée."
                        "de"->"Musik gestartet."
                        "it"->"Musica avviata."
                        "pl"->"Muzyka uruchomiona."
                        "tr"->"Müzik başlatıldı."
                        else->"Music started."
                    }
                )
            }
            RsVoiceMusicCommandV165.PAUSE->{
                controller?.pause()
                speak(
                    when(language().code){
                        "nl"->"Muziek gepauzeerd."
                        "pt"->"Música em pausa."
                        "es"->"Música en pausa."
                        "fr"->"Musique en pause."
                        "de"->"Musik pausiert."
                        "it"->"Musica in pausa."
                        "pl"->"Muzyka wstrzymana."
                        "tr"->"Müzik duraklatıldı."
                        else->"Music paused."
                    }
                )
            }
            RsVoiceMusicCommandV165.STOP->{
                controller?.stop()
                speak(
                    when(language().code){
                        "nl"->"Muziek gestopt."
                        "pt"->"Música parada."
                        "es"->"Música detenida."
                        "fr"->"Musique arrêtée."
                        "de"->"Musik gestoppt."
                        "it"->"Musica fermata."
                        "pl"->"Muzyka zatrzymana."
                        "tr"->"Müzik durduruldu."
                        else->"Music stopped."
                    }
                )
            }
            RsVoiceMusicCommandV165.NEXT->{
                controller?.seekToNextMediaItem()
                controller?.play()
                speak(
                    when(language().code){
                        "nl"->"Volgend nummer."
                        "pt"->"Próxima música."
                        "es"->"Siguiente canción."
                        "fr"->"Morceau suivant."
                        "de"->"Nächster Titel."
                        "it"->"Brano successivo."
                        "pl"->"Następny utwór."
                        "tr"->"Sonraki parça."
                        else->"Next track."
                    }
                )
            }
            RsVoiceMusicCommandV165.PREVIOUS->{
                controller?.seekToPreviousMediaItem()
                controller?.play()
                speak(
                    when(language().code){
                        "nl"->"Vorig nummer."
                        "pt"->"Música anterior."
                        "es"->"Canción anterior."
                        "fr"->"Morceau précédent."
                        "de"->"Vorheriger Titel."
                        "it"->"Brano precedente."
                        "pl"->"Poprzedni utwór."
                        "tr"->"Önceki parça."
                        else->"Previous track."
                    }
                )
            }
            RsVoiceMusicCommandV165.CREATE_LAST10->{
                awaitingPlaylistName=true
                speak(rsPlaylistNamePromptV165(language().code))
            }
            RsVoiceMusicCommandV165.VOLUME_UP->{
                adjustVolumeV166(1)
                speak(
                    when(language().code){
                        "nl"->"Volume hoger."
                        "pt"->"Volume aumentado."
                        "es"->"Volumen aumentado."
                        "fr"->"Volume augmenté."
                        "de"->"Lautstärke erhöht."
                        "it"->"Volume aumentato."
                        "pl"->"Głośność zwiększona."
                        "tr"->"Ses yükseltildi."
                        else->"Volume up."
                    }
                )
            }
            RsVoiceMusicCommandV165.VOLUME_DOWN->{
                adjustVolumeV166(-1)
                speak(
                    when(language().code){
                        "nl"->"Volume lager."
                        "pt"->"Volume reduzido."
                        "es"->"Volumen reducido."
                        "fr"->"Volume réduit."
                        "de"->"Lautstärke verringert."
                        "it"->"Volume ridotto."
                        "pl"->"Głośność zmniejszona."
                        "tr"->"Ses azaltıldı."
                        else->"Volume down."
                    }
                )
            }
            RsVoiceMusicCommandV165.WHATS_PLAYING->{
                val title=controller?.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
                speak(
                    if(title.isBlank()){
                        when(language().code){
                            "nl"->"Er speelt nu geen nummer."
                            "pt"->"Nenhuma música está a tocar agora."
                            "es"->"No hay ninguna canción reproduciéndose ahora."
                            "fr"->"Aucun morceau n’est en cours de lecture."
                            "de"->"Im Moment wird kein Titel abgespielt."
                            "it"->"Al momento non è in riproduzione alcun brano."
                            "pl"->"W tej chwili nic nie jest odtwarzane."
                            "tr"->"Şu anda hiçbir parça çalmıyor."
                            else->"Nothing is playing right now."
                        }
                    }else{
                        when(language().code){
                            "nl"->"Nu speelt "+title+"."
                            "pt"->"Está a tocar "+title+"."
                            "es"->"Está sonando "+title+"."
                            "fr"->"Lecture en cours : "+title+"."
                            "de"->"Jetzt läuft "+title+"."
                            "it"->"In riproduzione: "+title+"."
                            "pl"->"Teraz odtwarzany jest "+title+"."
                            "tr"->"Şimdi çalıyor: "+title+"."
                            else->"Now playing "+title+"."
                        }
                    }
                )
            }
            RsVoiceMusicCommandV165.OPEN_MUSIC->{
                openRouteV166("music")
                speak(
                    when(language().code){
                        "nl"->"RS Music geopend."
                        "pt"->"RS Music aberto."
                        "es"->"RS Music abierto."
                        "fr"->"Ouverture de RS Music."
                        "de"->"RS Music wird geöffnet."
                        "it"->"Apro RS Music."
                        "pl"->"Otwieram RS Music."
                        "tr"->"RS Music açılıyor."
                        else->"Opening RS Music."
                    }
                )
            }
            RsVoiceMusicCommandV165.SLEEP->{
                awakeUntil=0L
                speak(
                    when(language().code){
                        "nl"->"Oké. Ik luister weer alleen naar de wekzin."
                        "pt"->"Está bem. Vou voltar a ouvir apenas a frase de ativação."
                        "es"->"De acuerdo. Volveré a escuchar solo la frase de activación."
                        "fr"->"D’accord. Je reviens à l’écoute de la phrase d’activation uniquement."
                        "de"->"Okay. Ich höre jetzt wieder nur auf das Aktivierungswort."
                        "it"->"Va bene. Torno ad ascoltare solo la frase di attivazione."
                        "pl"->"Dobrze. Wracam do nasłuchiwania tylko hasła aktywacyjnego."
                        "tr"->"Tamam. Yalnızca uyandırma ifadesini dinlemeye dönüyorum."
                        else->"Okay. I’ll go back to listening only for the wake phrase."
                    }
                )
            }
            RsVoiceMusicCommandV165.UNKNOWN->{
                scope.launch{
                    val lang=language()
                    val role=if(store.s("session_role","")=="trainer")RsRole.TRAINER else RsRole.STUDENT
                    val currentRoute=store.s("session_last_route",defaultDashboardRouteV182())
                    val currentTitle=rsRouteTitle(
                        lang,currentRoute,currentRoute.replace('_',' ').replaceFirstChar{it.uppercase()}
                    )
                    val contextSummary=(
                        "CURRENT RS ROLE: "+role.name+
                        "\nCURRENT RS PAGE: "+currentRoute+" | "+currentTitle+
                        "\n\nRS APP FEATURES:\n"+rsAiAppKnowledgeSummaryV175(lang,role).take(3000)
                    )
                    val local=rsAiLocalCoachAnswerV164(lang.code,commandText)
                    val answer=rsOnlineAiCoachV164(commandText,lang,contextSummary)
                        .getOrElse{local}
                    speak(answer)
                }
            }
        }
    }

    private fun ensureMusicLoadedAndPlay(){
        val p=controller?:return
        if(p.mediaItemCount==0){
            val items=rsAllMusicItemsV165(this)
            if(items.isNotEmpty()){
                p.setMediaItems(items)
                p.prepare()
            }
        }
        p.play()
    }

    private fun createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            val nm=getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    RS_VOICE_CHANNEL_V165,
                    "RS Voice Wake",
                    NotificationManager.IMPORTANCE_MIN
                ).apply{
                    description="Keeps RS Voice Wake listening while enabled."
                    setSound(null,null)
                    enableVibration(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    RS_VOICE_WAKE_ACTION_CHANNEL_V182,
                    "RS Voice Wake actions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply{
                    description="Silent visual handoff when Android blocks automatic background app opening."
                    setSound(null,null)
                    enableVibration(false)
                }
            )
        }
    }

    private fun loginRequiredNotification():Notification{
        val openIntent=Intent(this,MainActivity::class.java).apply{
            action="com.rskickbox.app.OPEN_AI_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending=PendingIntent.getActivity(
            this,167,openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this,RS_VOICE_CHANNEL_V165)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RS KICKBOXING")
            .setContentText(
                when(language().code){
                    "nl"->"Login vereist · tik om RS KICKBOXING te openen"
                    "pt"->"Login necessário · toca para abrir RS KICKBOXING"
                    "es"->"Inicio de sesión requerido · toca para abrir RS KICKBOXING"
                    "fr"->"Connexion requise · touche pour ouvrir RS KICKBOXING"
                    "de"->"Anmeldung erforderlich · tippe, um RS KICKBOXING zu öffnen"
                    "it"->"Accesso richiesto · tocca per aprire RS KICKBOXING"
                    "pl"->"Wymagane logowanie · dotknij, aby otworzyć RS KICKBOXING"
                    "tr"->"Giriş gerekli · RS KICKBOXING'i açmak için dokun"
                    else->"Login required · tap to open RS KICKBOXING"
                }
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOngoing(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun notification(text:String):Notification{
        val openIntent=Intent(this,MainActivity::class.java).apply{
            action="com.rskickbox.app.OPEN_AI_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending=PendingIntent.getActivity(
            this,165,openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent=Intent(this,RsVoiceWakeServiceV165::class.java).apply{action="STOP"}
        val stopPending=PendingIntent.getService(
            this,166,stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this,RS_VOICE_CHANNEL_V165)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RS Voice Wake")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0,"Stop",stopPending)
            .build()
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action=="DIRECT_AI_ONCE"){
            pausedForForegroundAiV197=false
            store.pb("rs_voice_wake_paused_for_ai_v197",false)
            awakeUntil=System.currentTimeMillis()+45_000L
            serviceVoiceRequestId++
            runCatching{serviceCloudVoicePlayer?.stop()}
            runCatching{serviceCloudVoicePlayer?.release()}
            serviceCloudVoicePlayer=null
            runCatching{tts?.stop()}
            serviceSpeaking=false
            serviceUtteranceId=""
            runCatching{offlineWakeEngineV188?.stop()}
            offlineWakeEngineV188=null
            runCatching{recognizer?.cancel()}
            setWakeStatusV168("DIRECT_AI_PREPARING")
            scope.launch{
                delay(260)
                if(!serviceSpeaking){
                    setWakeStatusV168("DIRECT_AI_LISTENING")
                    startListening()
                }
            }
            return START_STICKY
        }
        if(intent?.action=="PAUSE_FOR_FOREGROUND_AI"){
            pausedForForegroundAiV197=true
            store.pb("rs_voice_wake_paused_for_ai_v197",true)
            serviceVoiceRequestId++
            runCatching{serviceCloudVoicePlayer?.stop()}
            runCatching{serviceCloudVoicePlayer?.release()}
            serviceCloudVoicePlayer=null
            runCatching{tts?.stop()}
            serviceSpeaking=false
            serviceUtteranceId=""
            runCatching{recognizer?.cancel()}
            runCatching{recognizer?.destroy()}
            recognizer=null
            runCatching{offlineWakeEngineV188?.stop()}
            offlineWakeEngineV188=null
            setWakeStatusV168("PAUSED_FOR_FOREGROUND_AI")
            return START_STICKY
        }
        if(intent?.action=="RESUME_AFTER_FOREGROUND_AI"){
            pausedForForegroundAiV197=false
            store.pb("rs_voice_wake_paused_for_ai_v197",false)
            setWakeStatusV168("RESUMING_AFTER_FOREGROUND_AI")
            scope.launch{
                delay(350)
                if(!pausedForForegroundAiV197 && !serviceSpeaking)startListening()
            }
            return START_STICKY
        }
        if(intent?.action=="STOP"){
            store.pb("rs_voice_wake_enabled_v165",false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if(intent?.action=="SPEAK_HANDOFF"){
            pausedForForegroundAiV197=false
            store.pb("rs_voice_wake_paused_for_ai_v197",false)
            val message=intent.getStringExtra("message").orEmpty().trim()
            if(message.isNotBlank()){
                pendingHandoffSpeechV194=message
                awakeUntil=System.currentTimeMillis()+45_000L
                store.pb("rs_voice_wake_enabled_v165",true)
                scope.launch{
                    delay(180)
                    if(pendingHandoffSpeechV194==message && tts!=null){
                        pendingHandoffSpeechV194=""
                        speak(message,thenListen=true)
                    }
                }
            }
        }
        return START_STICKY
    }

    companion object{
        fun start(context:Context){
            val store=RsStore(context)
            val authMs=store.s("session_password_auth_ms","0").toLongOrNull()?:0L
            val age=System.currentTimeMillis()-authMs
            val role=store.s("session_role","")
            val activityMs=store.s("session_last_activity_ms",authMs.toString()).toLongOrNull()?:authMs
            val inactivityAge=System.currentTimeMillis()-activityMs
            val fresh=authMs>0L && age in 0..RS_TRUSTED_LOGIN_MS_V165 &&
                activityMs>0L && inactivityAge in 0..RS_INACTIVITY_LOGOUT_MS_V165 &&
                role in setOf("student","trainer") &&
                rsSupabaseClientV60()?.auth?.currentUserOrNull()!=null
            if(!fresh){
                context.startActivity(
                    Intent(context,MainActivity::class.java)
                        .setAction("com.rskickbox.app.OPEN_AI_VOICE")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                return
            }
            val i=Intent(context,RsVoiceWakeServiceV165::class.java)
            ContextCompat.startForegroundService(context,i)
        }
        fun directListenOnce(context:Context){
            val intent=Intent(context,RsVoiceWakeServiceV165::class.java).apply{
                action="DIRECT_AI_ONCE"
            }
            ContextCompat.startForegroundService(context,intent)
        }
        fun handoffSpeechAndListen(context:Context,message:String){
            val store=RsStore(context)
            store.pb("rs_voice_wake_enabled_v165",true)
            val intent=Intent(context,RsVoiceWakeServiceV165::class.java).apply{
                action="SPEAK_HANDOFF"
                putExtra("message",message.take(1200))
            }
            ContextCompat.startForegroundService(context,intent)
        }
        fun pauseForForegroundAi(context:Context){
            RsStore(context).pb("rs_voice_wake_paused_for_ai_v197",true)
            val intent=Intent(context,RsVoiceWakeServiceV165::class.java).apply{
                action="PAUSE_FOR_FOREGROUND_AI"
            }
            ContextCompat.startForegroundService(context,intent)
        }
        fun resumeAfterForegroundAi(context:Context){
            RsStore(context).pb("rs_voice_wake_paused_for_ai_v197",false)
            val intent=Intent(context,RsVoiceWakeServiceV165::class.java).apply{
                action="RESUME_AFTER_FOREGROUND_AI"
            }
            ContextCompat.startForegroundService(context,intent)
        }
        fun stop(context:Context){
            context.stopService(Intent(context,RsVoiceWakeServiceV165::class.java))
        }
    }
}

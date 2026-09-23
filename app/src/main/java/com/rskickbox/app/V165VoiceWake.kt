package com.rskickbox.app

import android.Manifest
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
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

private const val RS_VOICE_CHANNEL_V165="rs_voice_wake_v165"
private const val RS_VOICE_NOTIFICATION_ID_V165=1651
private const val RS_VOICE_LOGIN_NOTIFICATION_ID_V165=1652
private const val RS_TRUSTED_LOGIN_MS_V165=72L*60L*60L*1000L
private const val RS_INACTIVITY_LOGOUT_MS_V165=24L*60L*60L*1000L

private fun rsWakePhrasesV165()=listOf(
    "wake up rs","hey rs","ok rs",
    "word wakker rs","hey rs",
    "acorda rs","olá rs","ola rs",
    "despierta rs","hola rs",
    "réveille toi rs","reveille toi rs","salut rs",
    "wach auf rs","hallo rs",
    "svegliati rs","ciao rs",
    "obudź się rs","obudz sie rs","hej rs",
    "uyan rs","hey rs"
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
    return when{
        listOf("open ai","open ai coach","open rs ai","open coach","open assistant","open de ai","open ai coach","abrir ai","abrir treinador ai","abre la ia","abre el entrenador ia","ouvre l'ia","ouvre le coach ia","ki öffnen","apri ai","otwórz ai","yapay zekayı aç").any{s.contains(it)}->"voice"
        listOf("open support","open hulp","open ondersteuning","abrir suporte","abre soporte","ouvre support","support öffnen","apri supporto","otwórz pomoc","desteği aç").any{s.contains(it)}->"support"
        listOf("open groups","open groepen","abrir grupos","abre grupos","ouvre groupes","gruppen öffnen","apri gruppi","otwórz grupy","grupları aç").any{s.contains(it)}->"groups"
        listOf("open community","open community","abrir comunidade","abre comunidad","ouvre communauté","community öffnen","apri community","otwórz społeczność","topluluğu aç").any{s.contains(it)}->"community"
        listOf("open notifications","open meldingen","abrir notificações","abre notificaciones","ouvre notifications","benachrichtigungen öffnen","apri notifiche","otwórz powiadomienia","bildirimleri aç").any{s.contains(it)}->"notifications"
        else->null
    }
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
    private val store by lazy{RsStore(this)}

    private fun setWakeStatusV168(status:String){
        // Keep diagnostics in local state without constantly re-posting the
        // foreground notification. Re-notifying on every recognizer cycle made
        // Android repeatedly animate/chime the notification shade.
        store.ps("rs_voice_wake_status_v168",status)
        store.ps("rs_voice_wake_status_ms_v168",System.currentTimeMillis().toString())
    }

    override fun onCreate(){
        super.onCreate()
        createChannel()
        startForeground(RS_VOICE_NOTIFICATION_ID_V165,notification("Voice wake ready"))
        tts=TextToSpeech(this,this)

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            val pm=getSystemService(PowerManager::class.java)
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"rskickbox:voicewake").apply{
                setReferenceCounted(false)
                acquire(60*60*1000L)
            }
        }

        val token=SessionToken(this,ComponentName(this,RsMusicPlaybackServiceV90::class.java))
        controllerFuture=MediaController.Builder(this,token).buildAsync().also{future->
            future.addListener({
                controller=runCatching{future.get()}.getOrNull()
            },ContextCompat.getMainExecutor(this))
        }

        if(trustedSessionActive()){
            setWakeStatusV168("STARTING")
            startListening()
        }else requireFreshLoginOrStop()
    }

    override fun onBind(intent:Intent?):IBinder?=null

    override fun onDestroy(){
        recognizer?.destroy()
        recognizer=null
        runCatching{tts?.stop()}
        runCatching{tts?.shutdown()}
        controller?.release()
        controller=null
        controllerFuture?.cancel(true)
        controllerFuture=null
        if(wakeLock?.isHeld==true)wakeLock?.release()
        wakeLock=null
        scope.cancel()
        super.onDestroy()
    }

    override fun onInit(status:Int){
        if(status==TextToSpeech.SUCCESS)applyLanguage()
    }

    private fun language():RsLang{
        val code=store.s("lang","en")
        return rsLangs.firstOrNull{it.code==code}?:rsLangs.first()
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
        val voice=engine.voices
            ?.filter{it.locale.language.equals(effective.language,true)}
            ?.sortedWith(compareByDescending<android.speech.tts.Voice>{!it.isNetworkConnectionRequired}.thenByDescending{it.quality})
            ?.firstOrNull()
        if(voice!=null)engine.voice=voice
    }

    private fun speak(text:String,thenListen:Boolean=true){
        setWakeStatusV168("SPEAKING")
        recognizer?.cancel()
        applyLanguage()
        tts?.setOnUtteranceProgressListener(object:android.speech.tts.UtteranceProgressListener(){
            override fun onStart(utteranceId:String?){}
            override fun onDone(utteranceId:String?){
                if(thenListen)scope.launch{
                    delay(250)
                    startListening()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId:String?){
                if(thenListen)scope.launch{
                    delay(250)
                    startListening()
                }
            }
        })
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"rs-voice-wake")
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
                override fun onError(error:Int){
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
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT->2200
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY->900
                                else->1200
                            }
                        )
                        startListening()
                    }
                }
                override fun onResults(results:android.os.Bundle?){
                    val list=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val text=list.firstOrNull().orEmpty()
                    handleTranscript(text)
                }
                override fun onPartialResults(partialResults:android.os.Bundle?){
                    val list=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val partial=list.firstOrNull().orEmpty()
                    if(System.currentTimeMillis()>awakeUntil && rsContainsWakePhraseV165(partial)){
                        recognizer?.cancel()
                        awakeUntil=System.currentTimeMillis()+45_000L
                        setWakeStatusV168("HEARD_WAKE")
                        val command=rsStripWakePhraseV165(partial)
                        store.pb("ai_immersive_v171",true)
                        store.pb("ai_start_listening_v168",true)
                        if(command.isNotBlank())store.ps("ai_pending_spoken_v171",command)
                        openRouteV166("voice")
                        if(command.isBlank())speak(rsVoiceGreetingV165(language().code),thenListen=false)
                    }
                }
                override fun onEvent(eventType:Int,params:android.os.Bundle?){}
            })
        }
    }

    private fun startListening(){
        if(!trustedSessionActive()){
            requireFreshLoginOrStop()
            return
        }
        if(
            ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
            !=PackageManager.PERMISSION_GRANTED
        )return
        createRecognizer()
        val lang=language()
        val recognitionLocale=if(wakeRecognitionFallback)Locale.getDefault() else lang.locale
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,recognitionLocale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,4200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,2800L)
        }
        setWakeStatusV168("READY")
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
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        if(!requireFreshLoginOrStop())return
        if(text.isBlank()){
            startListening()
            return
        }

        val now=System.currentTimeMillis()
        store.ps("session_last_activity_ms",now.toString())

        if(awaitingPlaylistName){
            val name=text.trim().take(60)
            awaitingPlaylistName=false
            val ok=rsSaveVoicePlaylistV165(this,name,10)
            speak(
                if(ok)rsPlaylistSavedV165(language().code,name)
                else when(language().code){
                    "nl"->"Ik heb nog niet genoeg recente muziek gevonden om die playlist te maken."
                    "pt"->"Ainda não encontrei músicas recentes suficientes para criar essa playlist."
                    else->"I could not find enough recent music to create that playlist yet."
                }
            )
            return
        }

        var commandText=text
        if(now>awakeUntil){
            if(rsContainsWakePhraseV165(text)){
                awakeUntil=now+45_000L
                setWakeStatusV168("HEARD_WAKE")
                commandText=rsStripWakePhraseV165(text)
                store.pb("ai_immersive_v171",true)
                store.pb("ai_start_listening_v168",true)
                if(commandText.isNotBlank())store.ps("ai_pending_spoken_v171",commandText)
                openRouteV166("voice")
                recognizer?.cancel()
                return
            }else{
                startListening()
                return
            }
        }

        awakeUntil=now+45_000L

        val requestedRoute=rsVoiceRouteV167(commandText)
        if(requestedRoute!=null){
            openRouteV166(requestedRoute)
            speak(
                when(language().code){
                    "nl"->"Ik open "+when(requestedRoute){"voice"->"de AI Coach";"support"->"Support";"groups"->"Groepen";"community"->"Community";else->"Meldingen"}+"."
                    "pt"->"Vou abrir "+when(requestedRoute){"voice"->"o Treinador IA";"support"->"o Suporte";"groups"->"os Grupos";"community"->"a Comunidade";else->"as Notificações"}+"."
                    "es"->"Voy a abrir "+when(requestedRoute){"voice"->"el Entrenador IA";"support"->"Soporte";"groups"->"Grupos";"community"->"Comunidad";else->"Notificaciones"}+"."
                    else->"Opening "+when(requestedRoute){"voice"->"AI Coach";"support"->"Support";"groups"->"Groups";"community"->"Community";else->"Notifications"}+"."
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
                            else->"Nothing is playing right now."
                        }
                    }else{
                        when(language().code){
                            "nl"->"Nu speelt "+title+"."
                            "pt"->"Está a tocar "+title+"."
                            "es"->"Está sonando "+title+"."
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
                        else->"Okay. I’ll go back to listening only for the wake phrase."
                    }
                )
            }
            RsVoiceMusicCommandV165.UNKNOWN->{
                scope.launch{
                    val lang=language()
                    val local=rsAiLocalCoachAnswerV164(lang.code,commandText)
                    val answer=rsOnlineAiCoachV164(commandText,lang,"")
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
                    NotificationManager.IMPORTANCE_LOW
                ).apply{
                    description="Keeps RS Voice Wake listening while enabled."
                    setSound(null,null)
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
            .setContentText("Login required · tap to open RS KICKBOXING")
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
        if(intent?.action=="STOP"){
            store.pb("rs_voice_wake_enabled_v165",false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
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
        fun stop(context:Context){
            context.stopService(Intent(context,RsVoiceWakeServiceV165::class.java))
        }
    }
}

package com.rskickbox.app

import android.Manifest
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val RS_VOICE_CHANNEL_V165="rs_voice_wake_v165"
private const val RS_VOICE_NOTIFICATION_ID_V165=1651

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
    PLAY,PAUSE,STOP,NEXT,PREVIOUS,CREATE_LAST10,UNKNOWN
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
        listOf("play music","resume music","speel muziek","ga door met muziek","tocar música","reproducir música","joue la musique","musik abspielen","riproduci musica","odtwórz muzykę","müzik çal").any{s.contains(it)}
            ->RsVoiceMusicCommandV165.PLAY
        else->RsVoiceMusicCommandV165.UNKNOWN
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
    private val store by lazy{RsStore(this)}

    override fun onCreate(){
        super.onCreate()
        createChannel()
        startForeground(RS_VOICE_NOTIFICATION_ID_V165,notification("RS Voice Wake is listening"))
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

        startListening()
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

    private fun applyLanguage(){
        val engine=tts?:return
        val lang=language()
        engine.language=lang.locale
        val voice=engine.voices
            ?.filter{it.locale.language.equals(lang.locale.language,true)}
            ?.sortedWith(compareByDescending<android.speech.tts.Voice>{!it.isNetworkConnectionRequired}.thenByDescending{it.quality})
            ?.firstOrNull()
        if(voice!=null)engine.voice=voice
    }

    private fun speak(text:String,thenListen:Boolean=true){
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
                    scope.launch{
                        delay(if(error==SpeechRecognizer.ERROR_NO_MATCH)600 else 1200)
                        startListening()
                    }
                }
                override fun onResults(results:android.os.Bundle?){
                    val list=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val text=list.firstOrNull().orEmpty()
                    handleTranscript(text)
                }
                override fun onPartialResults(partialResults:android.os.Bundle?){}
                override fun onEvent(eventType:Int,params:android.os.Bundle?){}
            })
        }
    }

    private fun startListening(){
        if(
            ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
            !=PackageManager.PERMISSION_GRANTED
        )return
        createRecognizer()
        val lang=language()
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
        }
        runCatching{recognizer?.startListening(intent)}
            .onFailure{
                scope.launch{delay(1000);startListening()}
            }
    }

    private fun handleTranscript(text:String){
        if(text.isBlank()){
            startListening()
            return
        }

        val now=System.currentTimeMillis()

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

        if(now>awakeUntil){
            if(rsContainsWakePhraseV165(text)){
                awakeUntil=now+45_000L
                speak(rsVoiceGreetingV165(language().code))
            }else{
                startListening()
            }
            return
        }

        awakeUntil=now+45_000L
        when(rsVoiceMusicCommandV165(text)){
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
            RsVoiceMusicCommandV165.UNKNOWN->{
                scope.launch{
                    val lang=language()
                    val local=rsAiLocalCoachAnswerV164(lang.code,text)
                    val answer=rsOnlineAiCoachV164(text,lang,"")
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
            val i=Intent(context,RsVoiceWakeServiceV165::class.java)
            ContextCompat.startForegroundService(context,i)
        }
        fun stop(context:Context){
            context.stopService(Intent(context,RsVoiceWakeServiceV165::class.java))
        }
    }
}

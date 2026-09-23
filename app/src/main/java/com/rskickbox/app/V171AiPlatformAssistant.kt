package com.rskickbox.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
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
    private val onIdle:()->Unit={}
){
    private var recognizer:SpeechRecognizer?=null
    private var running=false
    private var preferOfflineRecognition=true
    private var lastLocale=Locale.getDefault()
    private var closed=false

    private fun ensureRecognizer(){
        if(recognizer!=null)return
        if(!SpeechRecognizer.isRecognitionAvailable(context)){
            onStatus("Speech recognition is not available on this device.")
            return
        }
        recognizer=SpeechRecognizer.createSpeechRecognizer(context).apply{
            setRecognitionListener(object:RecognitionListener{
                override fun onReadyForSpeech(params:Bundle?){
                    running=true
                    onStatus("LISTENING")
                }
                override fun onBeginningOfSpeech(){onStatus("HEARING YOU")}
                override fun onRmsChanged(rmsdB:Float){}
                override fun onBufferReceived(buffer:ByteArray?){}
                override fun onEndOfSpeech(){onStatus("PROCESSING")}
                override fun onError(error:Int){
                    running=false
                    when(error){
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT->{
                            onStatus("")
                            onIdle()
                        }
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->onStatus("Microphone permission required.")
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY->{
                            destroyRecognizer()
                            onStatus("Microphone was busy. Try again.")
                        }
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED->{
                            if(preferOfflineRecognition){
                                preferOfflineRecognition=false
                                destroyRecognizer()
                                onStatus("Trying online voice recognition…")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if(!closed)start(lastLocale)
                                },800)
                            }else onStatus("Voice recognition for this language is unavailable. Try another language or install its speech pack.")
                        }
                        else->onStatus("Voice input unavailable. Error "+error)
                    }
                }
                override fun onResults(results:Bundle?){
                    running=false
                    val text=results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                        .trim()
                    onStatus("")
                    if(text.isNotBlank())onResult(text)
                }
                override fun onPartialResults(partialResults:Bundle?){}
                override fun onEvent(eventType:Int,params:Bundle?){}
            })
        }
    }

    fun start(locale:Locale){
        closed=false
        lastLocale=locale
        if(
            ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)
            !=PackageManager.PERMISSION_GRANTED
        ){
            onStatus("Microphone permission required.")
            return
        }
        if(running)return
        ensureRecognizer()
        if(recognizer==null)return
        val intent=android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,preferOfflineRecognition)
        }
        running=true
        runCatching{recognizer?.startListening(intent)}
            .onFailure{
                destroyRecognizer()
                onStatus("Voice input could not start.")
            }
    }

    fun stop(){
        running=false
        runCatching{recognizer?.cancel()}
    }

    fun destroy(){
        closed=true
        destroyRecognizer()
    }

    private fun destroyRecognizer(){
        running=false
        runCatching{recognizer?.destroy()}
        recognizer=null
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
    data object Help:RsAiPlatformIntentV171()
    data object None:RsAiPlatformIntentV171()
}

private fun rsContainsAnyV171(s:String,vararg phrases:String)=phrases.any{s.contains(it)}

fun rsAiPlatformIntentV171(raw:String):RsAiPlatformIntentV171{
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

    if(rsContainsAnyV171(s,"open ","take me to","go to ","show me ","bring me to","navigate to","leva me","ir para","abre ","ouvrir ","öffne ")){
        routes.firstOrNull{(_,_,aliases)->aliases.any{s.contains(it)}}?.let{(route,label,_)->
            return RsAiPlatformIntentV171.OpenRoute(route,label)
        }
    }

    if(rsContainsAnyV171(s,"what can you do","what can rs do","help me","help with the app","what are your features","o que podes fazer","wat kan je","qué puedes hacer")){
        return RsAiPlatformIntentV171.Help
    }

    return RsAiPlatformIntentV171.None
}

package com.rskickbox.app

import java.util.Locale
import kotlin.math.max

enum class RsAiAvatarStateV193 {
    IDLE, LISTENING, THINKING, SPEAKING
}

enum class RsAiVisemeV193 {
    REST, A, E, I, O, U, FV, L, MBP, WQ
}

data class RsAiAvatarIdentityV193(
    val id:String,
    val displayName:String,
    val gender:String,
    val modelAsset:String,
    val bundledDrawableName:String,
    val idleAnimation:String,
    val listeningAnimation:String,
    val thinkingAnimation:String,
    val speakingAnimation:String
)

val RsSofiaIdentityV193=RsAiAvatarIdentityV193(
    id="SOFIA",
    displayName="Sofia",
    gender="FEMALE",
    modelAsset="models/rs_ai_sofia.glb",
    bundledDrawableName="rs_ai_sofia_default",
    idleAnimation="idle",
    listeningAnimation="listening",
    thinkingAnimation="thinking",
    speakingAnimation="speaking"
)

val RsMarcusIdentityV193=RsAiAvatarIdentityV193(
    id="MARCUS",
    displayName="Marcus",
    gender="MALE",
    modelAsset="models/rs_ai_marcus.glb",
    bundledDrawableName="rs_ai_marcus_default",
    idleAnimation="idle",
    listeningAnimation="listening",
    thinkingAnimation="thinking",
    speakingAnimation="speaking"
)

fun rsAiIdentityV193(avatar:String):RsAiAvatarIdentityV193 =
    if(avatar=="MALE")RsMarcusIdentityV193 else RsSofiaIdentityV193

fun rsAiAvatarStateV193(
    speaking:Boolean,
    listening:Boolean,
    thinking:Boolean
):RsAiAvatarStateV193=when{
    speaking->RsAiAvatarStateV193.SPEAKING
    thinking->RsAiAvatarStateV193.THINKING
    listening->RsAiAvatarStateV193.LISTENING
    else->RsAiAvatarStateV193.IDLE
}

fun rsAiAnimationNameV193(identity:RsAiAvatarIdentityV193,state:RsAiAvatarStateV193):String=when(state){
    RsAiAvatarStateV193.IDLE->identity.idleAnimation
    RsAiAvatarStateV193.LISTENING->identity.listeningAnimation
    RsAiAvatarStateV193.THINKING->identity.thinkingAnimation
    RsAiAvatarStateV193.SPEAKING->identity.speakingAnimation
}

/**
 * Lightweight multilingual viseme estimator.
 *
 * This is intentionally deterministic and dependency-free. It gives the avatar
 * renderer a stable viseme timeline before/without a server-side phoneme timing
 * service. Once the production GLBs expose facial morph targets, the same enum
 * is mapped to those targets. Cloud speech can later override this estimator
 * with exact provider timings without changing the avatar API.
 */
fun rsAiVisemeAtV193(
    text:String,
    elapsedMs:Long,
    estimatedDurationMs:Long
):RsAiVisemeV193{
    if(text.isBlank())return RsAiVisemeV193.REST
    val clean=text.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N} ]")," ")
        .replace(Regex("\\s+")," ")
        .trim()
    if(clean.isBlank())return RsAiVisemeV193.REST

    val duration=max(estimatedDurationMs,700L)
    val progress=(elapsedMs.coerceIn(0L,duration).toDouble()/duration.toDouble())
    val index=(progress*(clean.length-1)).toInt().coerceIn(0,clean.lastIndex)
    val window=clean.substring(index,(index+3).coerceAtMost(clean.length))

    return when{
        window.startsWith("m") || window.startsWith("b") || window.startsWith("p")->RsAiVisemeV193.MBP
        window.startsWith("f") || window.startsWith("v")->RsAiVisemeV193.FV
        window.startsWith("l")->RsAiVisemeV193.L
        window.startsWith("w") || window.startsWith("q")->RsAiVisemeV193.WQ
        window.startsWith("a") || window.startsWith("á") || window.startsWith("à") || window.startsWith("ã")->RsAiVisemeV193.A
        window.startsWith("e") || window.startsWith("é") || window.startsWith("ê")->RsAiVisemeV193.E
        window.startsWith("i") || window.startsWith("í")->RsAiVisemeV193.I
        window.startsWith("o") || window.startsWith("ó") || window.startsWith("ô") || window.startsWith("õ")->RsAiVisemeV193.O
        window.startsWith("u") || window.startsWith("ú") || window.startsWith("ü")->RsAiVisemeV193.U
        clean[index].isWhitespace()->RsAiVisemeV193.REST
        else->RsAiVisemeV193.REST
    }
}

fun rsAiEstimatedSpeechDurationV193(text:String):Long{
    val words=text.trim().split(Regex("\\s+")).count{it.isNotBlank()}
    // Conversational TTS is usually around 130-180 wpm. 155 keeps mouth motion
    // natural and avoids frantic switching on short prompts.
    return ((words.coerceAtLeast(1)/155.0)*60_000.0).toLong().coerceAtLeast(900L)
}

fun rsAiGenderSafeVoiceNameV193(
    voiceName:String,
    avatar:String
):Boolean{
    val n=voiceName.lowercase(Locale.ROOT)
    val male=listOf("male","mascul","masc","man","m1","m2","david","daniel","thomas","george","hombre","homme","mann","uomo","homem","erkek")
    val female=listOf("female","femin","fem","woman","f1","f2","samantha","victoria","karen","anna","susan","mujer","femme","frau","donna","mulher","kadin","kadın")
    return if(avatar=="MALE"){
        female.none{n.contains(it)}
    }else{
        male.none{n.contains(it)}
    }
}

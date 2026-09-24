package com.rskickbox.app

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.io.File

data class RsCloudVoiceClipV192(
    val localUri:String,
    val model:String,
    val voice:String
)

fun rsUsePremiumCloudVoiceV192(store:RsStore,role:RsRole):Boolean{
    if(role==RsRole.TRAINER)return true
    return when(store.s("session_plan","BASIC").uppercase()){
        "PRO","ELITE"->true
        else->false
    }
}

suspend fun rsCloudVoiceClipV192(
    context:Context,
    text:String,
    lang:RsLang,
    avatar:String
):Result<RsCloudVoiceClipV192> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS cloud backend is not configured.")
    val response=client.functions.invoke(
        function="ai-voice-speech",
        body=buildJsonObject{
            put("text",text.take(2200))
            put("language_code",lang.code)
            put("avatar",if(avatar=="MALE")"MALE" else "FEMALE")
        }
    )
    val raw=response.bodyAsText()
    if(!response.status.isSuccess()){
        val message=runCatching{JSONObject(raw).optString("error")}.getOrDefault("")
        error(message.ifBlank{"Premium RS voice is unavailable."})
    }
    val payload=JSONObject(raw)
    val encoded=payload.optString("audio_base64")
    require(encoded.isNotBlank()){"Premium RS voice returned empty audio."}
    val bytes=Base64.decode(encoded,Base64.DEFAULT)
    require(bytes.size>128){"Premium RS voice returned invalid audio."}

    val dir=File(context.cacheDir,"rs_ai_voice_v192").apply{mkdirs()}
    dir.listFiles()?.filter{it.isFile && System.currentTimeMillis()-it.lastModified()>60L*60L*1000L}
        ?.forEach{runCatching{it.delete()}}
    val file=File(
        dir,
        "rs_voice_"+lang.code+"_"+avatar.lowercase()+"_"+System.nanoTime()+".mp3"
    )
    file.writeBytes(bytes)
    RsCloudVoiceClipV192(
        localUri=Uri.fromFile(file).toString(),
        model=payload.optString("model"),
        voice=payload.optString("voice")
    )
}

fun rsCreateCloudVoicePlayerV192(
    context:Context,
    localUri:String,
    onStarted:()->Unit={},
    onCompleted:()->Unit={},
    onError:(String)->Unit={}
):MediaPlayer? = runCatching{
    MediaPlayer().apply{
        setDataSource(context,Uri.parse(localUri))
        setOnPreparedListener{
            onStarted()
            it.start()
        }
        setOnCompletionListener{
            runCatching{it.release()}
            onCompleted()
        }
        setOnErrorListener{player,_,_->
            runCatching{player.release()}
            onError("Cloud voice playback failed.")
            true
        }
        prepareAsync()
    }
}.onFailure{
    onError(it.message?:"Cloud voice playback failed.")
}.getOrNull()

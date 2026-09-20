package com.rskickbox.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.io.ByteArrayOutputStream

private const val RS_AI_FRAME_MAX_WIDTH_V106=640
private const val RS_AI_FRAME_QUALITY_V106=72

private fun rsTechniqueFrameBase64V106(bitmap:Bitmap):String{
    val scaled=if(bitmap.width>RS_AI_FRAME_MAX_WIDTH_V106){
        val h=(bitmap.height*(RS_AI_FRAME_MAX_WIDTH_V106.toFloat()/bitmap.width)).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap,RS_AI_FRAME_MAX_WIDTH_V106,h,true)
    }else bitmap

    val bytes=ByteArrayOutputStream().use{out->
        scaled.compress(Bitmap.CompressFormat.JPEG,RS_AI_FRAME_QUALITY_V106,out)
        out.toByteArray()
    }
    if(scaled!==bitmap)scaled.recycle()
    bitmap.recycle()
    return Base64.encodeToString(bytes,Base64.NO_WRAP)
}

private fun rsSampleTechniqueFramesV106(context:Context,uriString:String):List<String>{
    val uri=Uri.parse(uriString)
    val retriever=MediaMetadataRetriever()
    try{
        retriever.setDataSource(context,uri)
        val durationMs=retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.coerceAtLeast(1L)
            ?:1L

        val fractions=listOf(.15,.40,.65,.90)
        return fractions.mapNotNull{fraction->
            val timeUs=(durationMs*1000.0*fraction).toLong()
            retriever.getFrameAtTime(timeUs,MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let(::rsTechniqueFrameBase64V106)
        }
    }finally{
        runCatching{retriever.release()}
    }
}

suspend fun rsAnalyzeTechniqueVisionV106(
    context:Context,
    localUri:String,
    technique:String,
    lang:RsLang
):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val frames=withContext(Dispatchers.IO){rsSampleTechniqueFramesV106(context,localUri)}
    require(frames.size>=2){"Not enough readable video frames."}

    val response=client.functions.invoke(
        function="analyze-technique",
        body=buildJsonObject{
            put("technique",technique)
            put("language",lang.name)
            put("frames",buildJsonArray{frames.forEach{add(JsonPrimitive(it))}})
        }
    )
    val raw=response.bodyAsText()
    if(!response.status.isSuccess()){
        val message=runCatching{JSONObject(raw).optString("error")}.getOrDefault("")
        error(message.ifBlank{"Technique AI is temporarily unavailable."})
    }
    val analysis=JSONObject(raw).optString("analysis").trim()
    require(analysis.isNotBlank()){"Technique AI returned an empty review."}
    analysis
}


data class RsAiReferenceClassificationV125(
    val move:String,
    val tags:List<String>,
    val coachNote:String
)

private fun rsSingleImageFrameV125(context:Context,uri:Uri):String{
    val bitmap=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}
        ?:error("Could not read trainer reference image.")
    return rsTechniqueFrameBase64V106(bitmap)
}

suspend fun rsClassifyTrainerReferenceV125(
    context:Context,
    source:Uri,
    lang:RsLang
):Result<RsAiReferenceClassificationV125> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val mime=context.contentResolver.getType(source).orEmpty().lowercase()
    val frames=withContext(Dispatchers.IO){
        if(mime.startsWith("image/")) listOf(rsSingleImageFrameV125(context,source))
        else rsSampleTechniqueFramesV106(context,source.toString())
    }
    require(frames.isNotEmpty()){"No readable trainer reference frames."}

    val response=client.functions.invoke(
        function="analyze-technique",
        body=buildJsonObject{
            put("mode","reference")
            put("technique","Auto-detect trainer reference")
            put("language",lang.name)
            put("frames",buildJsonArray{frames.forEach{add(JsonPrimitive(it))}})
        }
    )
    val raw=response.bodyAsText()
    if(!response.status.isSuccess()){
        val message=runCatching{JSONObject(raw).optString("error")}.getOrDefault("")
        error(message.ifBlank{"AI reference classification is temporarily unavailable."})
    }
    val payload=JSONObject(raw)
    val move=payload.optString("move").trim()
    require(move.isNotBlank()){"AI could not confidently identify a move. Enter the label manually."}
    val tagsJson=payload.optJSONArray("tags")
    val tags=buildList{
        if(tagsJson!=null){
            for(i in 0 until tagsJson.length()){
                tagsJson.optString(i).trim().takeIf{it.isNotBlank()}?.let(::add)
            }
        }
    }
    RsAiReferenceClassificationV125(
        move=move,
        tags=tags.distinct().take(20),
        coachNote=payload.optString("coach_note").trim()
    )
}

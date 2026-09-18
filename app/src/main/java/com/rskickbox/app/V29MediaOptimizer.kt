package com.rskickbox.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.Effects
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class RsVisualMediaInfoV29(
    val uri:String,
    val kind:String,
    val optimized:Boolean,
    val note:String
)

private fun rsVisualDirV29(context:Context):File =
    File(context.filesDir,"rs_visual_media").apply{mkdirs()}

fun rsVisualMimeV29(context:Context,uri:Uri):String {
    val direct=context.contentResolver.getType(uri)
    if(!direct.isNullOrBlank())return direct
    val p=uri.toString().lowercase()
    return when{
        p.endsWith(".gif")->"image/gif"
        p.endsWith(".mp4")||p.endsWith(".m4v")||p.endsWith(".mov")||p.endsWith(".webm")->"video/*"
        p.endsWith(".png")->"image/png"
        p.endsWith(".webp")->"image/webp"
        else->"image/*"
    }
}

fun rsVisualKindV29(context:Context,uri:Uri):String{
    val mime=rsVisualMimeV29(context,uri)
    return when{
        mime.startsWith("video/")->"VIDEO"
        mime=="image/gif"->"GIF"
        else->"IMAGE"
    }
}

private fun rsTargetSizeV29(slotSize:String):Pair<Int,Int> = when(slotSize){
    "SMALL"->720 to 720
    "MEDIUM"->1280 to 480
    else->720 to 1280
}

private fun rsCopyRawV29(context:Context,uri:Uri,extension:String):String{
    val out=File(rsVisualDirV29(context),"rs_${UUID.randomUUID()}.$extension")
    context.contentResolver.openInputStream(uri)!!.use{input->
        FileOutputStream(out).use{output->input.copyTo(output)}
    }
    return Uri.fromFile(out).toString()
}

private fun rsCenterCropBitmapV29(source:Bitmap,targetW:Int,targetH:Int):Bitmap{
    val scale=max(targetW.toFloat()/source.width,targetH.toFloat()/source.height)
    val scaledW=max(1,(source.width*scale).toInt())
    val scaledH=max(1,(source.height*scale).toInt())
    val scaled=Bitmap.createScaledBitmap(source,scaledW,scaledH,true)
    val x=max(0,(scaledW-targetW)/2)
    val y=max(0,(scaledH-targetH)/2)
    val cropW=min(targetW,scaled.width-x)
    val cropH=min(targetH,scaled.height-y)
    val cropped=Bitmap.createBitmap(scaled,x,y,cropW,cropH)
    if(scaled!==source && scaled!==cropped)scaled.recycle()
    return cropped
}

private fun rsNormalizeStaticImageV29(context:Context,uri:Uri,slotSize:String):String{
    val (targetW,targetH)=rsTargetSizeV29(slotSize)
    val bitmap=context.contentResolver.openInputStream(uri)!!.use{BitmapFactory.decodeStream(it)}
        ?:return rsCopyRawV29(context,uri,"jpg")
    val cropped=rsCenterCropBitmapV29(bitmap,targetW,targetH)
    val out=File(rsVisualDirV29(context),"rs_${UUID.randomUUID()}.jpg")
    FileOutputStream(out).use{stream->cropped.compress(Bitmap.CompressFormat.JPEG,88,stream)}
    if(cropped!==bitmap)cropped.recycle()
    bitmap.recycle()
    return Uri.fromFile(out).toString()
}

@OptIn(UnstableApi::class)
fun rsImportVisualMediaV29(
    context:Context,
    source:Uri,
    slotSize:String,
    onStatus:(String)->Unit,
    onComplete:(RsVisualMediaInfoV29)->Unit,
    onError:(String)->Unit
){
    when(rsVisualKindV29(context,source)){
        "GIF"->{
            runCatching{
                onStatus("Preserving GIF animation and preparing automatic crop playback…")
                val saved=rsCopyRawV29(context,source,"gif")
                onComplete(RsVisualMediaInfoV29(saved,"GIF",true,"Animated GIF preserved; auto-cropped to the selected visual slot at playback."))
            }.onFailure{onError(it.message?:"Could not import GIF.")}
        }
        "IMAGE"->{
            runCatching{
                onStatus("Formatting image to the selected visual slot…")
                val saved=rsNormalizeStaticImageV29(context,source,slotSize)
                onComplete(RsVisualMediaInfoV29(saved,"IMAGE",true,"Image auto-cropped and optimized for this visual slot."))
            }.onFailure{onError(it.message?:"Could not optimize image.")}
        }
        else->rsOptimizeBackgroundVideoV29(context,source,slotSize,onStatus,onComplete,onError)
    }
}

@OptIn(UnstableApi::class)
private fun rsOptimizeBackgroundVideoV29(
    context:Context,
    source:Uri,
    slotSize:String,
    onStatus:(String)->Unit,
    onComplete:(RsVisualMediaInfoV29)->Unit,
    onError:(String)->Unit
){
    val (targetW,targetH)=rsTargetSizeV29(slotSize)
    val output=File(rsVisualDirV29(context),"rs_${UUID.randomUUID()}.mp4")
    if(output.exists())output.delete()

    onStatus("Optimizing video for smooth background playback…")

    val effect:Effect=Presentation.createForWidthAndHeight(
        targetW,
        targetH,
        Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
    )
    val edited=EditedMediaItem.Builder(MediaItem.fromUri(source))
        .setRemoveAudio(true)
        .setEffects(Effects(emptyList(),listOf(effect)))
        .build()

    val transformer=Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .addListener(object:Transformer.Listener{
            override fun onCompleted(composition:Composition,exportResult:ExportResult){
                onComplete(
                    RsVisualMediaInfoV29(
                        Uri.fromFile(output).toString(),
                        "VIDEO",
                        true,
                        "Video converted to muted H.264 MP4 and cropped to ${targetW}×${targetH} for smoother app playback."
                    )
                )
            }
            override fun onError(composition:Composition,exportResult:ExportResult,exportException:ExportException){
                output.delete()
                onError("Video optimization failed: ${exportException.message?:"unknown error"}")
            }
        })
        .build()

    runCatching{transformer.start(edited,output.absolutePath)}
        .onFailure{output.delete();onError("Video optimization could not start: ${it.message}")}
}

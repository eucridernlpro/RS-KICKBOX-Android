package com.rskickbox.app

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.URL
import java.util.UUID

private const val RS_BRAND_BUCKET_V101="rs-brand-assets"

@Serializable
data class RsCloudVisualAssetV101(
    @SerialName("slot_key") val slotKey:String,
    @SerialName("object_path") val objectPath:String,
    @SerialName("media_kind") val mediaKind:String,
    val position:String,
    @SerialName("overlay_opacity") val overlayOpacity:Double,
    @SerialName("updated_at") val updatedAt:String
)

private fun rsBrandAssetExtV101(context:Context,uri:Uri):String{
    val mime=context.contentResolver.getType(uri).orEmpty().lowercase()
    return when{
        mime.contains("png")->"png"
        mime.contains("webp")->"webp"
        mime.contains("gif")->"gif"
        mime.contains("webm")->"webm"
        mime.contains("quicktime")->"mov"
        mime.contains("jpeg")||mime.contains("jpg")->"jpg"
        uri.toString().lowercase().endsWith(".png")->"png"
        uri.toString().lowercase().endsWith(".webp")->"webp"
        uri.toString().lowercase().endsWith(".gif")->"gif"
        uri.toString().lowercase().endsWith(".webm")->"webm"
        uri.toString().lowercase().endsWith(".mov")->"mov"
        uri.toString().lowercase().endsWith(".mp4")->"mp4"
        else->"jpg"
    }
}

private fun rsBrandMimeV101(context:Context,uri:Uri,kind:String):String{
    val detected=context.contentResolver.getType(uri)
    if(!detected.isNullOrBlank())return detected
    return when(kind){
        "VIDEO"->"video/mp4"
        "GIF"->"image/gif"
        else->"image/jpeg"
    }
}

private fun rsBrandBytesV101(context:Context,uri:Uri):ByteArray{
    return if(uri.scheme=="file"){
        File(uri.path?:error("Brand asset path is missing.")).readBytes()
    }else{
        context.contentResolver.openInputStream(uri)?.use{it.readBytes()}
            ?:error("Could not read brand asset.")
    }
}

suspend fun rsCloudVisualAssetsV101():Result<List<RsCloudVisualAssetV101>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.from("rs_visual_assets").select().decodeList<RsCloudVisualAssetV101>()
}

private fun rsPublicBrandUrlV101(path:String):String =
    BuildConfig.SUPABASE_URL.trimEnd('/')+"/storage/v1/object/public/"+RS_BRAND_BUCKET_V101+"/"+path

private fun rsLocalBrandKeyV101(slot:String):String=when{
    slot.startsWith("visual:")->"visual_v21_"+slot.removePrefix("visual:")
    slot.startsWith("brand:")->"brand_asset_"+slot.removePrefix("brand:")
    slot=="intro:phone"->"intro_phone_video_uri"
    slot=="intro:tablet"->"intro_tablet_video_uri"
    else->""
}

private fun rsLocalVisualSlotV101(slot:String):String?=
    slot.takeIf{it.startsWith("visual:")}?.removePrefix("visual:")

private suspend fun rsDownloadBrandAssetV101(
    context:Context,
    asset:RsCloudVisualAssetV101
):String=withContext(Dispatchers.IO){
    val ext=asset.objectPath.substringAfterLast('.',if(asset.mediaKind=="VIDEO")"mp4" else "jpg")
    val safe=asset.slotKey.replace(Regex("[^a-zA-Z0-9_-]"),"_")
    val dir=File(context.filesDir,"rs_cloud_brand").apply{mkdirs()}
    val file=File(dir,safe+"_"+asset.updatedAt.hashCode().toUInt().toString(16)+"."+ext)
    if(!file.exists()||file.length()==0L){
        URL(rsPublicBrandUrlV101(asset.objectPath)).openStream().use{input->
            file.outputStream().use{output->input.copyTo(output)}
        }
    }
    Uri.fromFile(file).toString()
}

suspend fun rsSyncCloudVisualAssetsV101(
    context:Context,
    store:RsStore
):Result<List<RsCloudVisualAssetV101>> = runCatching{
    val assets=rsCloudVisualAssetsV101().getOrThrow()
    for(asset in assets){
        val localKey=rsLocalBrandKeyV101(asset.slotKey)
        if(localKey.isBlank())continue
        val marker="cloud_brand_path_v101_"+asset.slotKey
        val cachedPath=store.s(marker,"")
        val current=store.s(localKey,"")
        val usable=current.startsWith("file:") && runCatching{File(Uri.parse(current).path!!).exists()}.getOrDefault(false)
        val localUri=if(cachedPath==asset.objectPath && usable){
            current
        }else{
            rsDownloadBrandAssetV101(context,asset)
        }
        store.ps(localKey,localUri)
        store.ps(marker,asset.objectPath)

        rsLocalVisualSlotV101(asset.slotKey)?.let{slot->
            store.ps("visual_v21_kind_"+slot,asset.mediaKind)
            store.ps("visual_v21_pos_"+slot,asset.position)
            store.ps("visual_v21_opacity_"+slot,asset.overlayOpacity.toFloat().coerceIn(0f,.88f).toString())
        }
    }
    assets
}

suspend fun rsUploadCloudVisualAssetV101(
    context:Context,
    slotKey:String,
    localUri:String,
    mediaKind:String,
    position:String="CENTER",
    overlayOpacity:Float=.55f
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val uri=Uri.parse(localUri)
    val ext=rsBrandAssetExtV101(context,uri)
    val mime=rsBrandMimeV101(context,uri,mediaKind)
    val bytes=rsBrandBytesV101(context,uri)
    require(bytes.size<=30*1024*1024){"Brand asset is larger than 30 MB."}

    val previous=client.from("rs_visual_assets")
        .select{filter{eq("slot_key",slotKey)}}
        .decodeList<RsCloudVisualAssetV101>()
        .firstOrNull()

    val safe=slotKey.replace(Regex("[^a-zA-Z0-9_-]"),"_")
    val path="assets/"+safe+"/"+UUID.randomUUID().toString()+"."+ext

    client.storage.from(RS_BRAND_BUCKET_V101).upload(path,bytes){
        upsert=false
        contentType=runCatching{ContentType.parse(mime)}.getOrDefault(ContentType.Application.OctetStream)
    }

    try{
        client.postgrest.rpc(
            "rs_staff_upsert_visual_asset",
            buildJsonObject{
                put("p_slot_key",slotKey)
                put("p_object_path",path)
                put("p_media_kind",mediaKind)
                put("p_position",position)
                put("p_overlay_opacity",overlayOpacity.coerceIn(0f,.88f))
            }
        )
    }catch(t:Throwable){
        runCatching{client.storage.from(RS_BRAND_BUCKET_V101).delete(path)}
        throw t
    }

    if(previous!=null && previous.objectPath!=path){
        runCatching{client.storage.from(RS_BRAND_BUCKET_V101).delete(previous.objectPath)}
    }
}

suspend fun rsUpdateCloudVisualMetadataV101(
    slotKey:String,
    position:String,
    overlayOpacity:Float
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_update_visual_asset_metadata",
        buildJsonObject{
            put("p_slot_key",slotKey)
            put("p_position",position)
            put("p_overlay_opacity",overlayOpacity.coerceIn(0f,.88f))
        }
    )
    Unit
}

suspend fun rsDeleteCloudVisualAssetV101(slotKey:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val old=client.from("rs_visual_assets")
        .select{filter{eq("slot_key",slotKey)}}
        .decodeList<RsCloudVisualAssetV101>()
        .firstOrNull()

    client.postgrest.rpc(
        "rs_staff_delete_visual_asset",
        buildJsonObject{put("p_slot_key",slotKey)}
    )

    if(old!=null){
        runCatching{client.storage.from(RS_BRAND_BUCKET_V101).delete(old.objectPath)}
    }
}


suspend fun rsPrepareIntroAssetV140(
    context:Context,
    store:RsStore,
    isTablet:Boolean
):Result<Boolean> = runCatching{
    val preferredKey=if(isTablet)"intro_tablet_video_uri" else "intro_phone_video_uri"
    val fallbackKey="intro_phone_video_uri"

    fun usable(value:String):Boolean{
        if(value.isBlank())return false
        val uri=runCatching{Uri.parse(value)}.getOrNull()?:return false
        return when(uri.scheme){
            "file"->uri.path?.let(::File)?.let{it.exists()&&it.length()>0L}==true
            "content"->runCatching{
                context.contentResolver.openFileDescriptor(uri,"r")?.use{it.statSize!=0L}==true
            }.getOrDefault(false)
            else->false
        }
    }

    val preferredLocal=store.s(preferredKey,"")
    if(usable(preferredLocal))return@runCatching true
    if(isTablet){
        val phoneLocal=store.s(fallbackKey,store.s("intro_video_uri",""))
        if(usable(phoneLocal))return@runCatching true
    }

    val assets=rsCloudVisualAssetsV101().getOrThrow()
    val preferredSlot=if(isTablet)"intro:tablet" else "intro:phone"
    val asset=assets.firstOrNull{it.slotKey==preferredSlot}
        ?:assets.firstOrNull{it.slotKey=="intro:phone"}
        ?:return@runCatching false

    val local=rsDownloadBrandAssetV101(context,asset)
    val localKey=rsLocalBrandKeyV101(asset.slotKey)
    if(localKey.isNotBlank()){
        store.ps(localKey,local)
        store.ps("cloud_brand_path_v101_"+asset.slotKey,asset.objectPath)
    }
    usable(local)
}

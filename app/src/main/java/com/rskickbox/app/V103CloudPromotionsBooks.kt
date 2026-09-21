package com.rskickbox.app

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.URL
import java.util.UUID

private const val RS_PROMO_BUCKET_V103="rs-promotions"
private const val RS_BOOK_BUCKET_V103="rs-books"

@Serializable
data class RsCloudPromotionV103(
    val id:String,
    val title:String,
    @SerialName("image_path") val imagePath:String,
    @SerialName("external_url") val externalUrl:String,
    val active:Boolean,
    @SerialName("sort_order") val sortOrder:Int
)

@Serializable
data class RsCloudBookV103(
    val id:String,
    val title:String,
    @SerialName("cover_path") val coverPath:String?=null,
    @SerialName("amazon_url") val amazonUrl:String?=null,
    @SerialName("preview_path") val previewPath:String?=null,
    @SerialName("full_path") val fullPath:String?=null,
    @SerialName("access_tier") val accessTier:String,
    @SerialName("can_read_full") val canReadFull:Boolean,
    @SerialName("gifted_emails") val giftedEmails:List<String> = emptyList()
)

private fun rsReadLocalBytesV103(context:Context,raw:String):ByteArray{
    val uri=Uri.parse(raw)
    return if(uri.scheme=="file"){
        File(uri.path?:error("File path is missing.")).readBytes()
    }else{
        context.contentResolver.openInputStream(uri)?.use{it.readBytes()}
            ?:error("Could not read selected file.")
    }
}

private fun rsMimeV103(context:Context,raw:String,fallback:String):String{
    val uri=Uri.parse(raw)
    return context.contentResolver.getType(uri)
        ?:when{
            raw.lowercase().endsWith(".png")->"image/png"
            raw.lowercase().endsWith(".webp")->"image/webp"
            raw.lowercase().endsWith(".pdf")->"application/pdf"
            else->fallback
        }
}

private fun rsExtV103(raw:String,mime:String):String=when{
    mime.contains("pdf",true)->"pdf"
    mime.contains("png",true)->"png"
    mime.contains("webp",true)->"webp"
    raw.lowercase().endsWith(".pdf")->"pdf"
    raw.lowercase().endsWith(".png")->"png"
    raw.lowercase().endsWith(".webp")->"webp"
    else->"jpg"
}

private fun rsPublicPromoUrlV103(path:String):String =
    BuildConfig.SUPABASE_URL.trimEnd('/')+"/storage/v1/object/public/"+RS_PROMO_BUCKET_V103+"/"+path

suspend fun rsCloudPromotionsV103():Result<List<RsCloudPromotionV103>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_promotion_feed").decodeList<RsCloudPromotionV103>()
}

suspend fun rsUploadPromotionImageV103(context:Context,localUri:String):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val mime=rsMimeV103(context,localUri,"image/jpeg")
    require(mime.startsWith("image/")){"Promotion artwork must be an image."}
    val bytes=rsReadLocalBytesV103(context,localUri)
    require(bytes.size<=10*1024*1024){"Promotion artwork must be 10 MB or smaller."}
    val path="promo/"+UUID.randomUUID()+"."+rsExtV103(localUri,mime)
    client.storage.from(RS_PROMO_BUCKET_V103).upload(path,bytes){
        upsert=false
        contentType=runCatching{ContentType.parse(mime)}.getOrDefault(ContentType.Image.JPEG)
    }
    path
}

suspend fun rsCreateCloudPromotionV103(title:String,imagePath:String,url:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_promotion",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_image_path",imagePath)
            put("p_external_url",url.trim())
        }
    )
    Unit
}

suspend fun rsSetCloudPromotionActiveV103(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_promotion_active",
        buildJsonObject{
            put("p_promotion_id",id)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsDeleteCloudPromotionV103(id:String,path:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_promotion",buildJsonObject{put("p_promotion_id",id)})
    runCatching{client.storage.from(RS_PROMO_BUCKET_V103).delete(path)}
    Unit
}

suspend fun rsCloudBookV103():Result<RsCloudBookV103?> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_book_feed").decodeList<RsCloudBookV103>().firstOrNull()
}

suspend fun rsUploadBookAssetV103(
    context:Context,
    localUri:String,
    kind:String
):Result<String> = runCatching{
    require(kind in setOf("cover","preview","full")){"Invalid book media kind."}
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val mime=rsMimeV103(context,localUri,if(kind=="cover")"image/jpeg" else "application/pdf")
    if(kind=="cover")require(mime.startsWith("image/")){"Book cover must be an image."}
    else require(mime=="application/pdf"||localUri.lowercase().endsWith(".pdf")){"Book file must be a PDF."}
    val bytes=rsReadLocalBytesV103(context,localUri)
    require(bytes.size<=50*1024*1024){"Book file must be 50 MB or smaller."}
    val path=kind+"/"+UUID.randomUUID()+"."+rsExtV103(localUri,mime)
    client.storage.from(RS_BOOK_BUCKET_V103).upload(path,bytes){
        upsert=false
        contentType=runCatching{ContentType.parse(mime)}.getOrDefault(ContentType.Application.OctetStream)
    }
    path
}

suspend fun rsSaveCloudBookV103(
    id:String?,
    title:String,
    coverPath:String?,
    amazonUrl:String,
    previewPath:String?,
    fullPath:String?,
    accessTier:String,
    giftedEmails:Set<String>
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_save_book",
        buildJsonObject{
            if(id==null)put("p_book_id",kotlinx.serialization.json.JsonNull) else put("p_book_id",id)
            put("p_title",title.trim())
            put("p_cover_path",coverPath.orEmpty())
            put("p_amazon_url",amazonUrl.trim())
            put("p_preview_path",previewPath.orEmpty())
            put("p_full_path",fullPath.orEmpty())
            put("p_access_tier",accessTier)
            put("p_gifted_emails",JsonArray(giftedEmails.sorted().map(::JsonPrimitive)))
        }
    )
    Unit
}

private suspend fun rsBookLocalUriV103(context:Context,path:String,kind:String):String{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val ext=path.substringAfterLast('.',if(kind=="cover")"jpg" else "pdf")
    val dir=File(context.cacheDir,"rs_book_cloud").apply{mkdirs()}
    val file=File(dir,kind+"_"+path.hashCode()+"."+ext)
    if(!file.exists()||file.length()==0L){
        val bytes=client.storage.from(RS_BOOK_BUCKET_V103).downloadAuthenticated(path)
        file.writeBytes(bytes)
    }
    return Uri.fromFile(file).toString()
}

suspend fun rsCloudBookConfigLocalV103(context:Context):Result<Pair<RsBookConfigV45?,String?>> = runCatching{
    val row=rsCloudBookV103().getOrThrow() ?: return@runCatching null to null
    val cover=row.coverPath?.takeIf{it.isNotBlank()}?.let{rsBookLocalUriV103(context,it,"cover")}.orEmpty()
    val preview=row.previewPath?.takeIf{it.isNotBlank()}?.let{rsBookLocalUriV103(context,it,"preview")}.orEmpty()
    val full=row.fullPath?.takeIf{it.isNotBlank()&&row.canReadFull}?.let{rsBookLocalUriV103(context,it,"full")}.orEmpty()
    RsBookConfigV45(
        title=row.title,
        coverUri=cover,
        amazonUrl=row.amazonUrl.orEmpty(),
        previewPdfUri=preview,
        fullPdfUri=full,
        accessTier=row.accessTier,
        giftedEmails=row.giftedEmails.map{it.lowercase()}.toSet()
    ) to row.id
}

fun rsCloudPromotionAsLocalV103(item:RsCloudPromotionV103)=RsPromoItemV45(
    id=item.id,
    title=item.title,
    imageUri=rsPublicPromoUrlV103(item.imagePath),
    externalUrl=item.externalUrl,
    active=item.active
)

suspend fun rsCloudPromotionLocalV156(context:Context,item:RsCloudPromotionV103):RsPromoItemV45{
    val client=rsSupabaseClientV60() ?: return rsCloudPromotionAsLocalV103(item)
    val ext=item.imagePath.substringAfterLast('.', "jpg")
    val dir=File(context.cacheDir,"rs_promo_cloud").apply{mkdirs()}
    val file=File(dir,"promo_"+item.imagePath.hashCode()+"."+ext)
    if(!file.exists()||file.length()==0L){
        val bytes=client.storage.from(RS_PROMO_BUCKET_V103).downloadAuthenticated(item.imagePath)
        file.writeBytes(bytes)
    }
    return RsPromoItemV45(
        id=item.id,
        title=item.title,
        imageUri=Uri.fromFile(file).toString(),
        externalUrl=item.externalUrl,
        active=item.active
    )
}

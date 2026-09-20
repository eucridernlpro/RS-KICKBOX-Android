package com.rskickbox.app

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

private const val RS_TRAINING_MEDIA_BUCKET_V73="rs-training-media"

@Serializable
data class RsCloudTrainingMediaRowV73(
    val id:String,
    val title:String,
    val category:String,
    val description:String,
    @SerialName("media_path") val mediaPath:String,
    @SerialName("media_kind") val mediaKind:String,
    @SerialName("access_tier") val accessTier:String,
    val published:Boolean,
    @SerialName("technique_tags") val techniqueTags:List<String> = emptyList(),
    @SerialName("ai_reference") val aiReference:Boolean = false
)

private fun rsCloudMediaMimeV73(context:Context,uri:Uri):String =
    context.contentResolver.getType(uri)?.lowercase()
        ?: when(uri.toString().lowercase()){
            in listOf("") -> "application/octet-stream"
            else -> "application/octet-stream"
        }

private fun rsCloudMediaKindV73(mime:String):String = when{
    mime=="image/gif" -> "GIF"
    mime.startsWith("image/") -> "IMAGE"
    else -> "VIDEO"
}

private fun rsCloudMediaExtensionV73(mime:String):String=when(mime){
    "video/webm"->"webm"
    "video/quicktime"->"mov"
    "image/png"->"png"
    "image/webp"->"webp"
    "image/gif"->"gif"
    "image/jpeg"->"jpg"
    else->"mp4"
}

private fun rsCloudMediaContentTypeV73(mime:String):ContentType =
    runCatching{ContentType.parse(mime)}.getOrElse{ContentType.Application.OctetStream}

suspend fun rsCloudTrainingMediaV73():Result<List<RsTrainingMediaItemV55>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.from("rs_training_media")
        .select()
        .decodeList<RsCloudTrainingMediaRowV73>()
        .map{
            RsTrainingMediaItemV55(
                id=it.id,
                title=it.title,
                category=it.category,
                description=it.description,
                uri=it.mediaPath,
                kind=it.mediaKind,
                accessTier=it.accessTier,
                published=it.published,
                techniqueTags=it.techniqueTags,
                aiReference=it.aiReference
            )
        }
}

suspend fun rsUploadCloudTrainingMediaV73(
    context:Context,
    source:Uri,
    title:String,
    category:String,
    description:String,
    accessTier:String,
    published:Boolean=true
):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val mime=rsCloudMediaMimeV73(context,source)
    val kind=rsCloudMediaKindV73(mime)
    val extension=rsCloudMediaExtensionV73(mime)
    val path="catalog/"+UUID.randomUUID().toString()+"."+extension
    val bytes=context.contentResolver.openInputStream(source)?.use{it.readBytes()}
        ?:error("Could not read the selected media file.")
    require(bytes.size<=100*1024*1024){"Media file is larger than 100 MB."}

    client.storage.from(RS_TRAINING_MEDIA_BUCKET_V73).upload(path,bytes){
        upsert=false
        contentType=rsCloudMediaContentTypeV73(mime)
    }

    try{
        client.postgrest.rpc(
            "rs_staff_create_training_media",
            buildJsonObject{
                put("p_title",title.trim())
                put("p_category",category.trim().ifBlank{"TECHNIQUE"})
                put("p_description",description.trim())
                put("p_media_path",path)
                put("p_media_kind",kind)
                put("p_access_tier",accessTier)
                put("p_published",published)
            }
        )
    }catch(t:Throwable){
        runCatching{client.storage.from(RS_TRAINING_MEDIA_BUCKET_V73).delete(path)}
        throw t
    }

    path
}

suspend fun rsCloudTrainingMediaLocalUriV73(
    context:Context,
    item:RsTrainingMediaItemV55
):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val dir=File(context.cacheDir,"rs_cloud_training_media").apply{mkdirs()}
    val extension=item.uri.substringAfterLast('.',if(item.kind=="VIDEO")"mp4" else "jpg")
    val file=File(dir,item.id+"."+extension)
    if(!file.exists() || file.length()==0L){
        val bytes=client.storage.from(RS_TRAINING_MEDIA_BUCKET_V73).downloadAuthenticated(item.uri)
        file.writeBytes(bytes)
    }
    Uri.fromFile(file).toString()
}

suspend fun rsSetCloudTrainingMediaPublishedV73(
    id:String,
    published:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_training_media_published",
        buildJsonObject{
            put("p_media_id",id)
            put("p_published",published)
        }
    )
    Unit
}

suspend fun rsDeleteCloudTrainingMediaV73(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val row=client.from("rs_training_media")
        .select{
            filter{eq("id",id)}
        }
        .decodeSingle<RsCloudTrainingMediaRowV73>()

    client.postgrest.rpc(
        "rs_staff_delete_training_media",
        buildJsonObject{put("p_media_id",id)}
    )
    runCatching{client.storage.from(RS_TRAINING_MEDIA_BUCKET_V73).delete(row.mediaPath)}
    Unit
}


suspend fun rsSetCloudTrainingMediaAiReferenceV110(
    id:String,
    aiReference:Boolean,
    techniqueTags:List<String>
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_mark_training_media_ai_reference",
        buildJsonObject{
            put("p_media_id",id)
            put("p_ai_reference",aiReference)
            put("p_technique_tags",kotlinx.serialization.json.buildJsonArray{
                techniqueTags
                    .map{it.trim().lowercase()}
                    .filter{it.isNotBlank()}
                    .distinct()
                    .take(20)
                    .forEach{add(kotlinx.serialization.json.JsonPrimitive(it))}
            })
        }
    )
    Unit
}


private fun rsAiMatchTextV114(value:String):String =
    value.lowercase()
        .replace(Regex("[^a-z0-9à-ÿ]+")," ")
        .trim()

suspend fun rsAiReferenceMatchesV114(
    technique:String,
    analysis:String
):Result<List<RsTrainingMediaItemV55>> = runCatching{
    val haystack=rsAiMatchTextV114(technique+" "+analysis)
    val words=haystack.split(" ").filter{it.length>=3}.toSet()

    rsCloudTrainingMediaV73().getOrThrow()
        .asSequence()
        .filter{it.published && it.aiReference && it.techniqueTags.isNotEmpty()}
        .map{item->
            val tagScore=item.techniqueTags.sumOf{rawTag->
                val tag=rsAiMatchTextV114(rawTag)
                when{
                    tag.isBlank()->0
                    haystack.contains(tag)->8
                    tag.split(" ").filter{it.length>=3}.any{it in words}->3
                    else->0
                }
            }
            val title=rsAiMatchTextV114(item.title+" "+item.category)
            val titleScore=title.split(" ").filter{it.length>=3}.count{it in words}
            item to (tagScore+titleScore)
        }
        .filter{it.second>0}
        .sortedWith(compareByDescending<Pair<RsTrainingMediaItemV55,Int>>{it.second}.thenBy{it.first.title.lowercase()})
        .take(3)
        .map{it.first}
        .toList()
}

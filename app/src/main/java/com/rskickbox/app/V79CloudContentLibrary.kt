package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudContentRowV79(
    val id:String,
    val title:String,
    val category:String,
    val body:String,
    @SerialName("access_tier") val accessTier:String,
    val published:Boolean,
    val favorite:Boolean,
    @SerialName("last_opened_at") val lastOpenedAt:String?=null
){
    fun asLocal():RsContentItemV48 =
        RsContentItemV48(id,title,category,body,accessTier,published)
}

suspend fun rsCloudContentV79():Result<List<RsCloudContentRowV79>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_content_catalog").decodeList<RsCloudContentRowV79>()
}

suspend fun rsToggleCloudContentFavoriteV79(id:String,favorite:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_toggle_content_favorite",
        buildJsonObject{
            put("p_content_id",id)
            put("p_favorite",favorite)
        }
    )
    Unit
}

suspend fun rsRecordCloudContentOpenV79(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_record_content_open",
        buildJsonObject{put("p_content_id",id)}
    )
    Unit
}

suspend fun rsCreateCloudContentV79(
    title:String,
    category:String,
    body:String,
    accessTier:String,
    published:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_content",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_category",category.trim())
            put("p_body",body)
            put("p_access_tier",accessTier)
            put("p_published",published)
        }
    )
    Unit
}

suspend fun rsSetCloudContentPublishedV79(id:String,published:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_content_published",
        buildJsonObject{
            put("p_content_id",id)
            put("p_published",published)
        }
    )
    Unit
}

suspend fun rsDeleteCloudContentV79(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_content",
        buildJsonObject{put("p_content_id",id)}
    )
    Unit
}

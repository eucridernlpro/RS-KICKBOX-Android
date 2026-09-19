package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class RsCloudNotificationV74(
    val id:String,
    val title:String,
    val message:String,
    val audience:String,
    @SerialName("created_at") val createdAt:String,
    val read:Boolean
){
    fun createdLabel():String = runCatching{
        Instant.parse(createdAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm",Locale.getDefault()))
    }.getOrDefault(createdAt)
}

@Serializable
private data class RsUnreadNotificationCountRowV74(
    val rs_unread_notification_count:Int
)

suspend fun rsCloudNotificationsV74():Result<List<RsCloudNotificationV74>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_notification_feed")
        .decodeList<RsCloudNotificationV74>()
}

suspend fun rsCloudUnreadNotificationCountV74():Int{
    val client=rsSupabaseClientV60() ?: return 0
    return runCatching{
        client.postgrest.rpc("rs_unread_notification_count")
            .decodeSingle<RsUnreadNotificationCountRowV74>()
            .rs_unread_notification_count
    }.getOrDefault(0)
}

suspend fun rsMarkCloudNotificationReadV74(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_mark_notification_read",
        buildJsonObject{put("p_notification_id",id)}
    )
    Unit
}

suspend fun rsCreateCloudNotificationV74(
    title:String,
    message:String,
    audience:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_notification",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_message",message.trim())
            put("p_audience",audience)
        }
    )
    Unit
}

suspend fun rsDeleteCloudNotificationV74(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_notification",
        buildJsonObject{put("p_notification_id",id)}
    )
    Unit
}

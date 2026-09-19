package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudEventV75(
    val id:String,
    val title:String,
    @SerialName("when_label") val whenLabel:String,
    val location:String,
    val capacity:Int,
    val active:Boolean,
    @SerialName("going_count") val goingCount:Int,
    @SerialName("my_status") val myStatus:String?=null
)

suspend fun rsCloudEventsV75():Result<List<RsCloudEventV75>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_event_catalog").decodeList<RsCloudEventV75>()
}

suspend fun rsCloudEventRsvpV75(eventId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_event_rsvp",buildJsonObject{put("p_event_id",eventId)})
    Unit
}

suspend fun rsCloudCancelEventRsvpV75(eventId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_cancel_event_rsvp",buildJsonObject{put("p_event_id",eventId)})
    Unit
}

suspend fun rsCloudCreateEventV75(
    title:String,
    whenLabel:String,
    location:String,
    capacity:Int
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_event",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_when_label",whenLabel.trim())
            put("p_location",location.trim())
            put("p_capacity",capacity)
        }
    )
    Unit
}

suspend fun rsCloudSetEventActiveV75(eventId:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_event_active",
        buildJsonObject{
            put("p_event_id",eventId)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsCloudDeleteEventV75(eventId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_event",buildJsonObject{put("p_event_id",eventId)})
    Unit
}

package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudPrivateSlotV76(
    @SerialName("slot_id") val slotId:String,
    @SerialName("day_label") val dayLabel:String,
    @SerialName("time_label") val timeLabel:String,
    @SerialName("duration_minutes") val durationMinutes:Int,
    val active:Boolean,
    @SerialName("my_booking_id") val myBookingId:String?=null,
    @SerialName("my_status") val myStatus:String?=null,
    @SerialName("my_note") val myNote:String?=null
)

@Serializable
data class RsCloudPrivateRequestV76(
    @SerialName("booking_id") val bookingId:String,
    @SerialName("slot_id") val slotId:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val note:String,
    val status:String,
    @SerialName("day_label") val dayLabel:String,
    @SerialName("time_label") val timeLabel:String,
    @SerialName("duration_minutes") val durationMinutes:Int
)

suspend fun rsCloudPrivateSlotsV76():Result<List<RsCloudPrivateSlotV76>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_private_lesson_catalog")
        .decodeList<RsCloudPrivateSlotV76>()
}

suspend fun rsRequestCloudPrivateLessonV76(slotId:String,note:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_request_private_lesson",
        buildJsonObject{
            put("p_slot_id",slotId)
            put("p_note",note.take(1000))
        }
    )
    Unit
}

suspend fun rsCancelCloudPrivateLessonV76(bookingId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_cancel_private_lesson_request",
        buildJsonObject{put("p_booking_id",bookingId)}
    )
    Unit
}

suspend fun rsCloudPrivateRequestsV76():Result<List<RsCloudPrivateRequestV76>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_private_lesson_requests")
        .decodeList<RsCloudPrivateRequestV76>()
}

suspend fun rsCreateCloudPrivateSlotV76(
    dayLabel:String,
    timeLabel:String,
    durationMinutes:Int
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_private_slot",
        buildJsonObject{
            put("p_day_label",dayLabel.trim())
            put("p_time_label",timeLabel.trim())
            put("p_duration_minutes",durationMinutes)
        }
    )
    Unit
}

suspend fun rsSetCloudPrivateSlotActiveV76(slotId:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_private_slot_active",
        buildJsonObject{
            put("p_slot_id",slotId)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsDeleteCloudPrivateSlotV76(slotId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_private_slot",
        buildJsonObject{put("p_slot_id",slotId)}
    )
    Unit
}

suspend fun rsSetCloudPrivateRequestStatusV76(
    bookingId:String,
    status:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_private_booking_status",
        buildJsonObject{
            put("p_booking_id",bookingId)
            put("p_status",status)
        }
    )
    Unit
}

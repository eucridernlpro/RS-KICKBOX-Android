package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class RsCloudClassRowV69(
    val id:String,
    val title:String,
    val level:String,
    @SerialName("starts_at") val startsAt:String,
    @SerialName("duration_minutes") val durationMinutes:Int,
    val capacity:Int,
    @SerialName("booking_open") val bookingOpen:Boolean,
    val active:Boolean,
    @SerialName("booked_count") val bookedCount:Int=0,
    @SerialName("my_booking_status") val myBookingStatus:String?=null
)

data class RsCloudClassStateV69(
    val clazz:RsClubClassV38,
    val bookedByMe:Boolean,
    val startsAt:String
)

private fun rsClassDisplayV69(startsAt:String):Pair<String,String>{
    return runCatching{
        val zone=ZoneId.systemDefault()
        val dt=Instant.parse(startsAt).atZone(zone)
        val day=dt.format(DateTimeFormatter.ofPattern("EEE dd MMM",Locale.getDefault()))
        val time=dt.format(DateTimeFormatter.ofPattern("HH:mm",Locale.getDefault()))
        day to time
    }.getOrElse{startsAt to ""}
}

suspend fun rsCloudClassesV69():Result<List<RsCloudClassStateV69>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_class_catalog")
        .decodeList<RsCloudClassRowV69>()
        .map{row->
            val (day,time)=rsClassDisplayV69(row.startsAt)
            RsCloudClassStateV69(
                clazz=RsClubClassV38(
                    id=row.id,
                    title=row.title,
                    dayLabel=day,
                    timeLabel=time,
                    level=row.level,
                    capacity=row.capacity,
                    booked=row.bookedCount,
                    bookingOpen=row.bookingOpen,
                    active=row.active
                ),
                bookedByMe=row.myBookingStatus=="booked",
                startsAt=row.startsAt
            )
        }
}

suspend fun rsBookClassV69(classId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_book_class",
        buildJsonObject{put("p_class_id",classId)}
    )
    Unit
}

suspend fun rsCancelClassV69(classId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_cancel_class_booking",
        buildJsonObject{put("p_class_id",classId)}
    )
    Unit
}

fun rsClassStartIsoV69(date:String,time:String):Result<String> = runCatching{
    val day=LocalDate.parse(date.trim(),DateTimeFormatter.ISO_LOCAL_DATE)
    val local=LocalDateTime.parse(
        day.toString()+"T"+time.trim(),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    )
    local.atZone(ZoneId.systemDefault()).toInstant().toString()
}

suspend fun rsCreateCloudClassV69(
    title:String,
    level:String,
    date:String,
    time:String,
    durationMinutes:Int,
    capacity:Int
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val startsAt=rsClassStartIsoV69(date,time).getOrThrow()
    client.postgrest.rpc(
        "rs_staff_create_class",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_level",level.trim().ifBlank{"ALL LEVELS"})
            put("p_starts_at",startsAt)
            put("p_duration_minutes",durationMinutes)
            put("p_capacity",capacity)
        }
    )
    Unit
}

suspend fun rsSetCloudClassStateV69(
    classId:String,
    active:Boolean,
    bookingOpen:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_class_state",
        buildJsonObject{
            put("p_class_id",classId)
            put("p_active",active)
            put("p_booking_open",bookingOpen)
        }
    )
    Unit
}

suspend fun rsDeleteCloudClassV69(classId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_class",
        buildJsonObject{put("p_class_id",classId)}
    )
    Unit
}

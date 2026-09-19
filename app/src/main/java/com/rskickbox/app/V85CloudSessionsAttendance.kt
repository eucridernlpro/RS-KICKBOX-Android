package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudSessionBlockV85(
    val id:String,
    val title:String,
    val seconds:Int,
    val instructions:String=""
)

@Serializable
data class RsCloudTrainingSessionV85(
    val id:String,
    val title:String,
    val active:Boolean,
    val blocks:List<RsCloudSessionBlockV85> = emptyList()
){
    fun asLocal():RsTrainingSessionV52=RsTrainingSessionV52(
        id,
        title,
        blocks.map{RsSessionBlockV52(it.id,it.title,it.seconds,it.instructions)},
        active
    )
}

@Serializable
data class RsAttendanceQrV85(
    @SerialName("class_id") val classId:String,
    val token:String,
    @SerialName("expires_at") val expiresAt:String
)

suspend fun rsCloudTrainingSessionsV85():Result<List<RsTrainingSessionV52>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_training_session_catalog")
        .decodeList<RsCloudTrainingSessionV85>()
        .map{it.asLocal()}
}

suspend fun rsCreateCloudTrainingSessionV85(
    title:String,
    blocks:List<RsSessionBlockV52>
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val blockJson=JsonArray(blocks.map{b->
        buildJsonObject{
            put("title",b.title)
            put("seconds",b.seconds)
            put("instructions",b.instructions)
        }
    })
    client.postgrest.rpc(
        "rs_staff_create_training_session",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_blocks",blockJson)
        }
    )
    Unit
}

suspend fun rsSetCloudTrainingSessionActiveV85(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_training_session_active",
        buildJsonObject{
            put("p_session_id",id)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsDeleteCloudTrainingSessionV85(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_training_session",
        buildJsonObject{put("p_session_id",id)}
    )
    Unit
}

suspend fun rsCreateAttendanceQrV85(classId:String,validMinutes:Int=15):Result<RsAttendanceQrV85> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_attendance_qr",
        buildJsonObject{
            put("p_class_id",classId)
            put("p_valid_minutes",validMinutes.coerceIn(2,60))
        }
    ).decodeList<RsAttendanceQrV85>().firstOrNull()
        ?:error("Attendance QR could not be created.")
}

suspend fun rsCloudAttendanceCheckInV85(classId:String,token:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_student_attendance_checkin",
        buildJsonObject{
            put("p_class_id",classId)
            put("p_token",token)
        }
    )
    Unit
}

package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsAttendanceRosterRowV70(
    @SerialName("student_id") val studentId:String,
    @SerialName("display_name") val displayName:String,
    val email:String,
    val booked:Boolean,
    val present:Boolean,
    @SerialName("checked_in_at") val checkedInAt:String?=null
)

suspend fun rsCloudAttendanceRosterV70(classId:String):Result<List<RsAttendanceRosterRowV70>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_attendance_roster",
        buildJsonObject{put("p_class_id",classId)}
    ).decodeList<RsAttendanceRosterRowV70>()
}

suspend fun rsCloudSetAttendanceV70(
    classId:String,
    studentId:String,
    present:Boolean,
    note:String=""
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_attendance",
        buildJsonObject{
            put("p_class_id",classId)
            put("p_student_id",studentId)
            put("p_present",present)
            put("p_note",note)
        }
    )
    Unit
}

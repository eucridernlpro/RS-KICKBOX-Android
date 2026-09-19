package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Serializable
data class RsDevStudentV80(
    val id:String,
    val email:String,
    @SerialName("display_name") val displayName:String,
    val plan:String,
    val active:Boolean
)

@Serializable
data class RsCloudHomeworkV80(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val title:String,
    val details:String,
    @SerialName("due_label") val dueLabel:String,
    val completed:Boolean,
    @SerialName("created_at") val createdAt:String
)

@Serializable
data class RsCloudCoachNoteV80(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val note:String,
    @SerialName("created_at") val createdAt:String
)

@Serializable
data class RsCloudAssessmentV80(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val punches:Int,
    val kicks:Int,
    val defense:Int,
    val footwork:Int,
    val combinations:Int,
    val conditioning:Int,
    val summary:String,
    @SerialName("created_at") val createdAt:String
){
    fun createdMillis():Long=runCatching{Instant.parse(createdAt).toEpochMilli()}.getOrDefault(0L)
}

suspend fun rsDevelopmentStudentsV80():Result<List<RsDevStudentV80>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_development_students").decodeList<RsDevStudentV80>()
}

suspend fun rsHomeworkFeedV80():Result<List<RsCloudHomeworkV80>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_homework_feed").decodeList<RsCloudHomeworkV80>()
}

suspend fun rsAssignHomeworkV80(studentId:String,title:String,details:String,due:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_assign_homework",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_title",title)
            put("p_details",details)
            put("p_due_label",due)
        }
    )
    Unit
}

suspend fun rsSetHomeworkCompletedV80(id:String,completed:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_homework_completed",
        buildJsonObject{
            put("p_homework_id",id)
            put("p_completed",completed)
        }
    )
    Unit
}

suspend fun rsDeleteHomeworkV80(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_homework",buildJsonObject{put("p_homework_id",id)})
    Unit
}

suspend fun rsCoachNotesFeedV80():Result<List<RsCloudCoachNoteV80>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_coach_notes_feed").decodeList<RsCloudCoachNoteV80>()
}

suspend fun rsAddCoachNoteV80(studentId:String,note:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_add_coach_note",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_note",note)
        }
    )
    Unit
}

suspend fun rsDeleteCoachNoteV80(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_coach_note",buildJsonObject{put("p_note_id",id)})
    Unit
}

suspend fun rsAssessmentFeedV80():Result<List<RsCloudAssessmentV80>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_assessment_feed").decodeList<RsCloudAssessmentV80>()
}

suspend fun rsAddAssessmentV80(
    studentId:String,
    punches:Int,
    kicks:Int,
    defense:Int,
    footwork:Int,
    combinations:Int,
    conditioning:Int,
    summary:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_add_assessment",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_punches",punches)
            put("p_kicks",kicks)
            put("p_defense",defense)
            put("p_footwork",footwork)
            put("p_combinations",combinations)
            put("p_conditioning",conditioning)
            put("p_summary",summary)
        }
    )
    Unit
}

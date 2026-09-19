package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudChallengeV83(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val title:String,
    val target:Int,
    val current:Int,
    val unit:String,
    val active:Boolean
)

@Serializable
data class RsCloudFightCampV83(
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    @SerialName("current_week") val currentWeek:Int,
    @SerialName("total_weeks") val totalWeeks:Int,
    val focus:String,
    val active:Boolean
)

suspend fun rsCloudChallengesV83():Result<List<RsCloudChallengeV83>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_challenge_feed").decodeList<RsCloudChallengeV83>()
}

suspend fun rsAssignCloudChallengeV83(
    studentId:String,
    title:String,
    target:Int,
    unit:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_assign_challenge",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_title",title)
            put("p_target",target)
            put("p_unit",unit)
        }
    )
    Unit
}

suspend fun rsIncreaseCloudChallengeV83(id:String,delta:Int=1):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_update_my_challenge_progress",
        buildJsonObject{
            put("p_challenge_id",id)
            put("p_delta",delta)
        }
    )
    Unit
}

suspend fun rsSetCloudChallengeActiveV83(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_challenge_active",
        buildJsonObject{
            put("p_challenge_id",id)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsDeleteCloudChallengeV83(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_challenge",buildJsonObject{put("p_challenge_id",id)})
    Unit
}

suspend fun rsCloudFightCampsV83():Result<List<RsCloudFightCampV83>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_fight_camp_feed").decodeList<RsCloudFightCampV83>()
}

suspend fun rsSetCloudFightCampV83(
    studentId:String,
    currentWeek:Int,
    focus:String,
    active:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_fight_camp",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_current_week",currentWeek)
            put("p_focus",focus)
            put("p_active",active)
        }
    )
    Unit
}

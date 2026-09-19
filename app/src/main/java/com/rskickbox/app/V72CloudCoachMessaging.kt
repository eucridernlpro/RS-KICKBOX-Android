package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Serializable
data class RsCloudCoachThreadV72(
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    @SerialName("last_message") val lastMessage:String?=null,
    @SerialName("last_message_at") val lastMessageAt:String?=null,
    @SerialName("unread_count") val unreadCount:Int=0
)

@Serializable
data class RsCloudCoachMessageV72(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    @SerialName("sender_role") val senderRole:String,
    val body:String,
    @SerialName("created_at") val createdAt:String
){
    fun createdAtMillis():Long=runCatching{Instant.parse(createdAt).toEpochMilli()}.getOrDefault(0L)
}

suspend fun rsCloudCoachThreadsV72():Result<List<RsCloudCoachThreadV72>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_coach_threads")
        .decodeList<RsCloudCoachThreadV72>()
}

suspend fun rsCloudCoachMessagesV72(studentId:String):Result<List<RsCloudCoachMessageV72>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_coach_thread_messages",
        buildJsonObject{put("p_student_id",studentId)}
    ).decodeList<RsCloudCoachMessageV72>()
}

suspend fun rsCloudMyStudentIdV72():Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val raw=client.postgrest.rpc("rs_my_student_id").body
    raw.toString().trim().trim('"').takeIf{it.isNotBlank()&&it!="null"}
        ?:error("Active student account not found.")
}

suspend fun rsCloudSendCoachMessageV72(studentId:String,body:String):Result<Unit> = runCatching{
    val clean=body.trim()
    require(clean.isNotBlank()){"Write a message first."}
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_send_coach_message",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_body",clean)
        }
    )
    Unit
}

suspend fun rsCloudMarkCoachReadV72(studentId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_mark_coach_thread_read",
        buildJsonObject{put("p_student_id",studentId)}
    )
    Unit
}

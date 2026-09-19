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
    @SerialName("media_path") val mediaPath:String?=null,
    @SerialName("media_kind") val mediaKind:String?=null,
    @SerialName("media_name") val mediaName:String?=null,
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

@Serializable
private data class RsMyStudentIdRowV72(
    @SerialName("student_id") val studentId:String
)

suspend fun rsCloudMyStudentIdV72():Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_student_id")
        .decodeList<RsMyStudentIdRowV72>()
        .firstOrNull()?.studentId
        ?:error("Active student account not found.")
}

suspend fun rsCloudSendCoachMessageV72(
    studentId:String,
    body:String,
    attachment:RsChatAttachmentV92?=null
):Result<Unit> = runCatching{
    val clean=body.trim()
    require(clean.isNotBlank()||attachment!=null){"Write a message or add an attachment."}
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_send_coach_message_v2",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_body",clean)
            if(attachment==null){
                put("p_media_path",kotlinx.serialization.json.JsonNull)
                put("p_media_kind",kotlinx.serialization.json.JsonNull)
                put("p_media_name",kotlinx.serialization.json.JsonNull)
            }else{
                put("p_media_path",attachment.path)
                put("p_media_kind",attachment.kind)
                put("p_media_name",attachment.name)
            }
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

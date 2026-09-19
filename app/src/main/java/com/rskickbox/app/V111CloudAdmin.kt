package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsStudentMediaAssetV111(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("storage_bucket") val storageBucket:String,
    @SerialName("storage_path") val storagePath:String,
    @SerialName("media_kind") val mediaKind:String,
    @SerialName("source_area") val sourceArea:String,
    @SerialName("byte_size") val byteSize:Long,
    @SerialName("created_at") val createdAt:String
)

@Serializable
data class RsPrivateLessonTrainingV111(
    @SerialName("booking_id") val bookingId:String,
    @SerialName("template_id") val templateId:String,
    @SerialName("template_title") val templateTitle:String,
    @SerialName("homework_id") val homeworkId:String?=null,
    @SerialName("assigned_at") val assignedAt:String
)

suspend fun rsRegisterStudentMediaAssetV111(
    studentId:String,
    bucket:String,
    path:String,
    kind:String,
    sourceArea:String,
    byteSize:Long
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_register_student_media_asset",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_storage_bucket",bucket)
            put("p_storage_path",path)
            put("p_media_kind",kind)
            put("p_source_area",sourceArea)
            put("p_byte_size",byteSize)
        }
    )
    Unit
}

suspend fun rsStudentMediaAssetsV111(studentId:String):Result<List<RsStudentMediaAssetV111>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_student_media_assets",
        buildJsonObject{put("p_student_id",studentId)}
    ).decodeList<RsStudentMediaAssetV111>()
}

suspend fun rsDeleteStudentMediaAssetV111(asset:RsStudentMediaAssetV111):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.storage.from(asset.storageBucket).delete(asset.storagePath)
    client.postgrest.rpc(
        "rs_staff_remove_student_media_asset",
        buildJsonObject{put("p_asset_id",asset.id)}
    )
    Unit
}

suspend fun rsAssignPrivateLessonTrainingV111(
    bookingId:String,
    templateId:String,
    dueLabel:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_assign_private_lesson_training",
        buildJsonObject{
            put("p_booking_id",bookingId)
            put("p_template_id",templateId)
            put("p_due_label",dueLabel.trim())
        }
    )
    Unit
}

suspend fun rsPrivateLessonTrainingFeedV111():Result<List<RsPrivateLessonTrainingV111>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc("rs_private_lesson_training_feed")
        .decodeList<RsPrivateLessonTrainingV111>()
}

suspend fun rsStaffEditCoachMessageV111(messageId:String,body:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_edit_coach_message",
        buildJsonObject{put("p_message_id",messageId);put("p_body",body.trim())}
    )
    Unit
}

suspend fun rsStaffDeleteCoachMessageV111(messageId:String,mediaPath:String?):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_coach_message",
        buildJsonObject{put("p_message_id",messageId)}
    )
    if(!mediaPath.isNullOrBlank())runCatching{client.storage.from("rs-chat-media").delete(mediaPath)}
    Unit
}

suspend fun rsStaffClearCoachThreadV111(studentId:String,mediaPaths:List<String>):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_clear_coach_thread",
        buildJsonObject{put("p_student_id",studentId)}
    )
    mediaPaths.filter{it.isNotBlank()}.distinct().forEach{path->
        runCatching{client.storage.from("rs-chat-media").delete(path)}
    }
    Unit
}

suspend fun rsStaffEditGroupMessageV111(messageId:String,body:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_edit_group_message",
        buildJsonObject{put("p_message_id",messageId);put("p_body",body.trim())}
    )
    Unit
}

suspend fun rsStaffDeleteGroupMessageV111(messageId:String,mediaPath:String?):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_delete_group_message",
        buildJsonObject{put("p_message_id",messageId)}
    )
    if(!mediaPath.isNullOrBlank())runCatching{client.storage.from("rs-chat-media").delete(mediaPath)}
    Unit
}

suspend fun rsStaffClearGroupChatV111(groupId:String,mediaPaths:List<String>):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_clear_group_chat",
        buildJsonObject{put("p_group_id",groupId)}
    )
    mediaPaths.filter{it.isNotBlank()}.distinct().forEach{path->
        runCatching{client.storage.from("rs-chat-media").delete(path)}
    }
    Unit
}

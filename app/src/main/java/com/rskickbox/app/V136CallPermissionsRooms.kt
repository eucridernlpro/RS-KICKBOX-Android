package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsStudentCallPermissionV136(
    @SerialName("student_id") val studentId:String,
    val email:String,
    @SerialName("display_name") val displayName:String,
    @SerialName("avatar_path") val avatarPath:String?=null,
    val online:Boolean=false,
    @SerialName("audio_enabled") val audioEnabled:Boolean=false,
    @SerialName("video_enabled") val videoEnabled:Boolean=false
)

@Serializable
data class RsMyCallPermissionsV136(
    @SerialName("audio_enabled") val audioEnabled:Boolean=false,
    @SerialName("video_enabled") val videoEnabled:Boolean=false
)

@Serializable
data class RsVideoRoomV136(
    @SerialName("room_id") val roomId:String,
    val title:String,
    @SerialName("room_status") val roomStatus:String,
    @SerialName("host_id") val hostId:String,
    @SerialName("host_name") val hostName:String,
    @SerialName("my_role") val myRole:String,
    @SerialName("my_status") val myStatus:String,
    @SerialName("participant_count") val participantCount:Long=0,
    @SerialName("online_count") val onlineCount:Long=0,
    @SerialName("created_at") val createdAt:String
)

@Serializable
data class RsVideoRoomMemberV136(
    @SerialName("user_id") val userId:String,
    @SerialName("display_name") val displayName:String,
    val email:String,
    @SerialName("avatar_path") val avatarPath:String?=null,
    @SerialName("member_role") val memberRole:String,
    @SerialName("member_status") val memberStatus:String,
    val online:Boolean=false
)

@Serializable
data class RsVideoRoomSignalV136(
    val id:Long,
    @SerialName("sender_id") val senderId:String,
    @SerialName("target_id") val targetId:String,
    @SerialName("signal_kind") val signalKind:String,
    val payload:JsonElement,
    @SerialName("created_at") val createdAt:String
)

suspend fun rsStudentCallPermissionListV136():Result<List<RsStudentCallPermissionV136>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_student_call_permission_list").decodeList<RsStudentCallPermissionV136>()
}

suspend fun rsSetStudentCallPermissionsV136(
    studentId:String,
    audioEnabled:Boolean,
    videoEnabled:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_student_call_permissions",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_audio_enabled",audioEnabled)
            put("p_video_enabled",videoEnabled)
        }
    )
    Unit
}

suspend fun rsMyStudentCallPermissionsV136():Result<RsMyCallPermissionsV136?> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_student_call_permissions")
        .decodeList<RsMyCallPermissionsV136>()
        .firstOrNull()
}

suspend fun rsCreateVideoRoomV136(
    title:String,
    studentIds:List<String>
):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val response=client.postgrest.rpc(
        "rs_create_video_room",
        buildJsonObject{
            put("p_title",title)
            put("p_student_ids",JsonArray(studentIds.distinct().map(::JsonPrimitive)))
        }
    )
    response.data.trim().trim('"').ifBlank{error("Video room could not be created.")}
}

suspend fun rsMyVideoRoomsV136():Result<List<RsVideoRoomV136>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_video_rooms").decodeList<RsVideoRoomV136>()
}

suspend fun rsVideoRoomMembersV136(roomId:String):Result<List<RsVideoRoomMemberV136>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_video_room_members",
        buildJsonObject{put("p_room_id",roomId)}
    ).decodeList<RsVideoRoomMemberV136>()
}

suspend fun rsSetVideoRoomStatusV136(roomId:String,status:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_video_room_status",
        buildJsonObject{
            put("p_room_id",roomId)
            put("p_status",status.uppercase())
        }
    )
    Unit
}

suspend fun rsAddVideoRoomSignalV136(
    roomId:String,
    targetId:String,
    kind:String,
    payload:JsonElement
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_add_video_room_signal",
        buildJsonObject{
            put("p_room_id",roomId)
            put("p_target_id",targetId)
            put("p_signal_kind",kind.uppercase())
            put("p_payload",payload)
        }
    )
    Unit
}

suspend fun rsVideoRoomSignalsSinceV136(
    roomId:String,
    afterId:Long
):Result<List<RsVideoRoomSignalV136>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_video_room_signals_since",
        buildJsonObject{
            put("p_room_id",roomId)
            put("p_after_id",afterId)
        }
    ).decodeList<RsVideoRoomSignalV136>()
}

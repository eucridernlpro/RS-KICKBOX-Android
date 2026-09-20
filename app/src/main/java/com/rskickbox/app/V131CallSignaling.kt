package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCallV131(
    val id:String,
    @SerialName("caller_id") val callerId:String,
    @SerialName("callee_id") val calleeId:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("call_type") val callType:String,
    val status:String,
    @SerialName("peer_id") val peerId:String,
    @SerialName("peer_name") val peerName:String,
    @SerialName("peer_email") val peerEmail:String,
    @SerialName("created_at") val createdAt:String,
    @SerialName("answered_at") val answeredAt:String?=null,
    @SerialName("ended_at") val endedAt:String?=null
)

@Serializable
private data class RsCallIdV131(
    @SerialName("call_id") val callId:String
)

@Serializable
data class RsCallPeerV131(
    @SerialName("user_id") val userId:String,
    @SerialName("display_name") val displayName:String,
    val email:String
)

@Serializable
data class RsCallSignalV131(
    val id:Long,
    @SerialName("sender_id") val senderId:String,
    @SerialName("signal_kind") val signalKind:String,
    val payload:JsonElement,
    @SerialName("created_at") val createdAt:String
)

suspend fun rsStartDirectCallV131(peerId:String,type:String):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_start_direct_call",
        buildJsonObject{
            put("p_peer_id",peerId)
            put("p_call_type",type.uppercase())
        }
    ).decodeList<RsCallIdV131>().firstOrNull()?.callId
        ?:error("Call could not be started.")
}

suspend fun rsCallInboxV131():Result<List<RsCallV131>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_call_inbox").decodeList<RsCallV131>()
}

suspend fun rsStudentCallPeerV131():Result<RsCallPeerV131?> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_student_call_peer").decodeList<RsCallPeerV131>().firstOrNull()
}

suspend fun rsSetCallStatusV131(callId:String,status:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_call_status",
        buildJsonObject{
            put("p_call_id",callId)
            put("p_status",status.uppercase())
        }
    )
    Unit
}

suspend fun rsAddCallSignalV131(
    callId:String,
    kind:String,
    payload:JsonElement
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_add_call_signal",
        buildJsonObject{
            put("p_call_id",callId)
            put("p_signal_kind",kind.uppercase())
            put("p_payload",payload)
        }
    )
    Unit
}

suspend fun rsCallSignalsSinceV131(callId:String,afterId:Long):Result<List<RsCallSignalV131>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_call_signals_since",
        buildJsonObject{
            put("p_call_id",callId)
            put("p_after_id",afterId)
        }
    ).decodeList<RsCallSignalV131>()
}

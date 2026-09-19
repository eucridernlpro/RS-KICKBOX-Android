package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class RsAccountRequestV78(
    val id:String,
    @SerialName("request_type") val requestType:String,
    val status:String,
    @SerialName("user_note") val userNote:String="",
    @SerialName("staff_note") val staffNote:String="",
    @SerialName("requested_at") val requestedAt:String,
    @SerialName("completed_at") val completedAt:String?=null
){
    fun requestedLabel():String=runCatching{
        Instant.parse(requestedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm",Locale.getDefault()))
    }.getOrDefault(requestedAt)
}

suspend fun rsMyAccountRequestsV78():Result<List<RsAccountRequestV78>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_account_requests")
        .decodeList<RsAccountRequestV78>()
}

suspend fun rsCreateAccountRequestV78(
    requestType:String,
    note:String=""
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_request_account_action",
        buildJsonObject{
            put("p_request_type",requestType)
            put("p_user_note",note.take(1000))
        }
    )
    Unit
}

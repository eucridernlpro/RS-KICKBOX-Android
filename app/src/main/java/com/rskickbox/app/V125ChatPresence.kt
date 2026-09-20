package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RsChatContactV125(
    @SerialName("user_id") val userId:String,
    val email:String,
    @SerialName("display_name") val displayName:String,
    @SerialName("avatar_path") val avatarPath:String?=null,
    val role:String,
    val online:Boolean=false,
    @SerialName("last_seen_at") val lastSeenAt:String?=null
)

suspend fun rsTouchPresenceV125():Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_touch_presence")
    Unit
}

suspend fun rsChatContactsV125():Result<List<RsChatContactV125>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_chat_contacts").decodeList<RsChatContactV125>()
}

package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Serializable
data class RsCloudSocialProfileV84(
    @SerialName("user_id") val userId:String,
    val email:String,
    @SerialName("display_name") val displayName:String,
    val bio:String,
    @SerialName("training_goal") val trainingGoal:String,
    @SerialName("public_profile") val publicProfile:Boolean
)

@Serializable
data class RsCloudCommunityPostV84(
    val id:String,
    @SerialName("author_id") val authorId:String,
    @SerialName("author_email") val authorEmail:String,
    @SerialName("author_name") val authorName:String,
    val body:String,
    val active:Boolean,
    @SerialName("created_at") val createdAt:String
){
    fun createdMillis():Long=runCatching{Instant.parse(createdAt).toEpochMilli()}.getOrDefault(0L)
}

@Serializable
data class RsCloudGroupV84(
    val id:String,
    val name:String,
    val description:String,
    val active:Boolean,
    val joined:Boolean,
    @SerialName("member_count") val memberCount:Int
)

suspend fun rsCloudMySocialProfileV84():Result<RsCloudSocialProfileV84> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_my_social_profile")
        .decodeList<RsCloudSocialProfileV84>()
        .firstOrNull()
        ?:error("Profile not found.")
}

suspend fun rsSaveCloudSocialProfileV84(
    displayName:String,
    bio:String,
    trainingGoal:String,
    publicProfile:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_save_my_social_profile",
        buildJsonObject{
            put("p_display_name",displayName)
            put("p_bio",bio)
            put("p_training_goal",trainingGoal)
            put("p_public_profile",publicProfile)
        }
    )
    Unit
}

suspend fun rsCloudCommunityV84():Result<List<RsCloudCommunityPostV84>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_community_feed").decodeList<RsCloudCommunityPostV84>()
}

suspend fun rsCreateCloudCommunityPostV84(body:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_create_community_post",
        buildJsonObject{put("p_body",body.trim())}
    )
    Unit
}

suspend fun rsDeleteCloudCommunityPostV84(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_delete_community_post",buildJsonObject{put("p_post_id",id)})
    Unit
}

suspend fun rsSetCloudCommunityPostActiveV84(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_community_post_active",
        buildJsonObject{
            put("p_post_id",id)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsCloudGroupsV84():Result<List<RsCloudGroupV84>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_group_catalog").decodeList<RsCloudGroupV84>()
}

suspend fun rsSetMyCloudGroupMembershipV84(id:String,join:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_my_group_membership",
        buildJsonObject{
            put("p_group_id",id)
            put("p_join",join)
        }
    )
    Unit
}

suspend fun rsCreateCloudGroupV84(name:String,description:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_group",
        buildJsonObject{
            put("p_name",name.trim())
            put("p_description",description.trim())
        }
    )
    Unit
}

suspend fun rsSetCloudGroupActiveV84(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_group_active",
        buildJsonObject{
            put("p_group_id",id)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsDeleteCloudGroupV84(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_group",buildJsonObject{put("p_group_id",id)})
    Unit
}

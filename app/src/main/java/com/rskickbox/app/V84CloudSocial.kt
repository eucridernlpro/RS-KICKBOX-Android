package com.rskickbox.app

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

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
    @SerialName("media_path") val mediaPath:String?=null,
    @SerialName("media_kind") val mediaKind:String?=null,
    @SerialName("media_name") val mediaName:String?=null,
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
    @SerialName("member_count") val memberCount:Int,
    @SerialName("online_count") val onlineCount:Int=0,
    @SerialName("offline_count") val offlineCount:Int=0,
    @SerialName("thumbnail_path") val thumbnailPath:String?=null,
    @SerialName("students_can_post") val studentsCanPost:Boolean=true,
    @SerialName("students_can_media") val studentsCanMedia:Boolean=true,
    @SerialName("open_join") val openJoin:Boolean=true
)

@Serializable
data class RsCloudGroupMemberV144(
    @SerialName("user_id") val userId:String,
    @SerialName("display_name") val displayName:String,
    val email:String,
    @SerialName("avatar_path") val avatarPath:String?=null,
    val online:Boolean=false,
    @SerialName("joined_at") val joinedAt:String=""
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
    runCatching{
        client.postgrest.rpc("rs_community_feed_v2").decodeList<RsCloudCommunityPostV84>()
    }.getOrElse{
        client.postgrest.rpc("rs_community_feed").decodeList<RsCloudCommunityPostV84>()
    }
}

suspend fun rsCreateCloudCommunityPostV84(
    body:String,
    attachment:RsChatAttachmentV92?=null
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val v2=runCatching{
        client.postgrest.rpc(
            "rs_create_community_post_v2",
            buildJsonObject{
                put("p_body",body.trim())
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
    }
    if(v2.isFailure){
        require(attachment==null){"Community media requires the latest RS CHAT backend update."}
        client.postgrest.rpc(
            "rs_create_community_post",
            buildJsonObject{put("p_body",body.trim())}
        )
    }
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

suspend fun rsCreateCloudGroupV84(
    name:String,
    description:String,
    thumbnailPath:String?=null,
    studentsCanPost:Boolean=true,
    studentsCanMedia:Boolean=true,
    openJoin:Boolean=true
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_group_v2",
        buildJsonObject{
            put("p_name",name.trim())
            put("p_description",description.trim())
            put("p_thumbnail_path",thumbnailPath.orEmpty())
            put("p_students_can_post",studentsCanPost)
            put("p_students_can_media",studentsCanMedia)
            put("p_open_join",openJoin)
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


private const val RS_GROUP_ART_BUCKET_V144="rs-group-artwork"

fun rsGroupThumbnailUrlV144(path:String?):String{
    val clean=path.orEmpty().trim()
    if(clean.isBlank())return ""
    return BuildConfig.SUPABASE_URL.trimEnd('/')+"/storage/v1/object/public/"+RS_GROUP_ART_BUCKET_V144+"/"+clean
}

suspend fun rsUploadGroupThumbnailV144(context:Context,raw:String):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val uri=Uri.parse(raw)
    val mime=context.contentResolver.getType(uri)
        ?:when{
            raw.endsWith(".png",true)->"image/png"
            raw.endsWith(".webp",true)->"image/webp"
            else->"image/jpeg"
        }
    require(mime.startsWith("image/")){"Group thumbnail must be an image."}
    val bytes=if(uri.scheme=="file"){
        java.io.File(uri.path?:error("Missing image path")).readBytes()
    }else{
        context.contentResolver.openInputStream(uri)?.use{it.readBytes()}
            ?:error("Could not read group image.")
    }
    require(bytes.size<=6*1024*1024){"Group thumbnail must be 6 MB or smaller."}
    val ext=when{
        mime.contains("png",true)->"png"
        mime.contains("webp",true)->"webp"
        else->"jpg"
    }
    val path="groups/"+UUID.randomUUID()+"."+ext
    client.storage.from(RS_GROUP_ART_BUCKET_V144).upload(path,bytes){
        upsert=false
        contentType=runCatching{ContentType.parse(mime)}.getOrDefault(ContentType.Image.JPEG)
    }
    path
}

suspend fun rsUpdateCloudGroupV144(
    id:String,
    name:String,
    description:String,
    thumbnailPath:String?,
    studentsCanPost:Boolean,
    studentsCanMedia:Boolean,
    openJoin:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_update_group_v2",
        buildJsonObject{
            put("p_group_id",id)
            put("p_name",name.trim())
            put("p_description",description.trim())
            put("p_thumbnail_path",thumbnailPath.orEmpty())
            put("p_students_can_post",studentsCanPost)
            put("p_students_can_media",studentsCanMedia)
            put("p_open_join",openJoin)
        }
    )
    Unit
}

suspend fun rsCloudGroupMembersV144(groupId:String):Result<List<RsCloudGroupMemberV144>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_group_members",
        buildJsonObject{put("p_group_id",groupId)}
    ).decodeList<RsCloudGroupMemberV144>()
}

suspend fun rsRemoveCloudGroupMemberV144(groupId:String,studentId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_remove_group_member",
        buildJsonObject{
            put("p_group_id",groupId)
            put("p_student_id",studentId)
        }
    )
    Unit
}


suspend fun rsGroupThumbnailLocalV144(context:Context,path:String?):Result<String> = runCatching{
    val clean=path.orEmpty().trim()
    if(clean.isBlank())return@runCatching ""
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val ext=clean.substringAfterLast('.', "jpg")
    val dir=java.io.File(context.cacheDir,"rs_group_artwork").apply{mkdirs()}
    val file=java.io.File(dir,"group_"+clean.hashCode()+"."+ext)
    if(!file.exists()||file.length()==0L){
        val bytes=client.storage.from(RS_GROUP_ART_BUCKET_V144).downloadAuthenticated(clean)
        file.writeBytes(bytes)
    }
    Uri.fromFile(file).toString()
}

package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object RsChatPermissionKeysV178{
    const val STUDENT_IMAGES="chat_perm_student_images_v178"
    const val STUDENT_VIDEOS="chat_perm_student_videos_v178"
    const val STUDENT_AUDIO="chat_perm_student_audio_v178"
    const val STUDENT_FILES="chat_perm_student_files_v178"
    const val PRIVATE_CALLS="chat_perm_private_calls_v178"
    const val GROUP_MEDIA="chat_perm_group_media_v178"
    const val COMMUNITY_MEDIA="chat_perm_community_media_v178"
    const val AUTO_PREVIEW="chat_auto_media_preview_v162"
    const val GALLERY="chat_perm_gallery_v178"
    const val RETENTION_DAYS="chat_perm_retention_days_v178"
}

@Serializable
data class RsChatPermissionsV178(
    @SerialName("student_images_enabled") val studentImagesEnabled:Boolean=true,
    @SerialName("student_videos_enabled") val studentVideosEnabled:Boolean=true,
    @SerialName("student_audio_enabled") val studentAudioEnabled:Boolean=true,
    @SerialName("student_files_enabled") val studentFilesEnabled:Boolean=true,
    @SerialName("private_calls_enabled") val privateCallsEnabled:Boolean=true,
    @SerialName("group_media_enabled") val groupMediaEnabled:Boolean=true,
    @SerialName("community_media_enabled") val communityMediaEnabled:Boolean=true,
    @SerialName("auto_media_preview_enabled") val autoMediaPreviewEnabled:Boolean=true,
    @SerialName("gallery_enabled") val galleryEnabled:Boolean=true,
    @SerialName("media_retention_days") val mediaRetentionDays:Int=7
)

fun rsCacheChatPermissionsV178(store:RsStore,p:RsChatPermissionsV178){
    store.pb(RsChatPermissionKeysV178.STUDENT_IMAGES,p.studentImagesEnabled)
    store.pb(RsChatPermissionKeysV178.STUDENT_VIDEOS,p.studentVideosEnabled)
    store.pb(RsChatPermissionKeysV178.STUDENT_AUDIO,p.studentAudioEnabled)
    store.pb(RsChatPermissionKeysV178.STUDENT_FILES,p.studentFilesEnabled)
    store.pb(RsChatPermissionKeysV178.PRIVATE_CALLS,p.privateCallsEnabled)
    store.pb(RsChatPermissionKeysV178.GROUP_MEDIA,p.groupMediaEnabled)
    store.pb(RsChatPermissionKeysV178.COMMUNITY_MEDIA,p.communityMediaEnabled)
    store.pb(RsChatPermissionKeysV178.AUTO_PREVIEW,p.autoMediaPreviewEnabled)
    store.pb(RsChatPermissionKeysV178.GALLERY,p.galleryEnabled)
    store.ps(RsChatPermissionKeysV178.RETENTION_DAYS,p.mediaRetentionDays.coerceIn(1,30).toString())
}

fun rsChatPermissionV178(store:RsStore,key:String,default:Boolean=true)=store.b(key,default)

suspend fun rsCloudChatPermissionsV178():Result<RsChatPermissionsV178> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_chat_permissions")
        .decodeList<RsChatPermissionsV178>()
        .firstOrNull()
        ?:error("RS Chat permissions are not available.")
}

suspend fun rsSyncChatPermissionsV178(store:RsStore):Result<RsChatPermissionsV178> =
    rsCloudChatPermissionsV178().onSuccess{rsCacheChatPermissionsV178(store,it)}

suspend fun rsSaveChatPermissionsV178(
    store:RsStore,
    permissions:RsChatPermissionsV178
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_update_chat_permissions",
        buildJsonObject{
            put("p_student_images_enabled",permissions.studentImagesEnabled)
            put("p_student_videos_enabled",permissions.studentVideosEnabled)
            put("p_student_audio_enabled",permissions.studentAudioEnabled)
            put("p_student_files_enabled",permissions.studentFilesEnabled)
            put("p_private_calls_enabled",permissions.privateCallsEnabled)
            put("p_group_media_enabled",permissions.groupMediaEnabled)
            put("p_community_media_enabled",permissions.communityMediaEnabled)
            put("p_auto_media_preview_enabled",permissions.autoMediaPreviewEnabled)
            put("p_gallery_enabled",permissions.galleryEnabled)
            put("p_media_retention_days",permissions.mediaRetentionDays.coerceIn(1,30))
        }
    )
    rsCacheChatPermissionsV178(store,permissions)
}

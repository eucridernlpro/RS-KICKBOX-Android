package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

@Serializable
data class RsCloudDocumentV99(
    val id:String,
    val title:String,
    val body:String,
    @SerialName("access_tier") val accessTier:String,
    val active:Boolean,
    @SerialName("created_at") val createdAt:String
)

@Serializable
data class RsCloudSupportTicketV99(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val subject:String,
    val message:String,
    @SerialName("trainer_reply") val trainerReply:String,
    @SerialName("media_path") val mediaPath:String?=null,
    @SerialName("media_kind") val mediaKind:String?=null,
    @SerialName("media_name") val mediaName:String?=null,
    val status:String,
    @SerialName("created_at") val createdAt:String
){
    fun createdAtMillis():Long=runCatching{Instant.parse(createdAt).toEpochMilli()}.getOrDefault(0L)
}

@Serializable
data class RsCloudReferralV99(
    val id:String,
    @SerialName("owner_id") val ownerId:String,
    @SerialName("owner_email") val ownerEmail:String,
    @SerialName("owner_name") val ownerName:String,
    val code:String,
    val uses:Int,
    val active:Boolean
)

suspend fun rsCloudDocumentsV99():Result<List<RsCloudDocumentV99>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_member_documents_feed").decodeList<RsCloudDocumentV99>()
}

suspend fun rsCreateCloudDocumentV99(title:String,body:String,tier:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_document",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_body",body)
            put("p_access_tier",tier)
        }
    )
    Unit
}

suspend fun rsSetCloudDocumentActiveV99(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_document_active",
        buildJsonObject{
            put("p_document_id",id)
            put("p_active",active)
        }
    )
    Unit
}

suspend fun rsDeleteCloudDocumentV99(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_document",buildJsonObject{put("p_document_id",id)})
    Unit
}

suspend fun rsCloudSupportFeedV99():Result<List<RsCloudSupportTicketV99>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    runCatching{
        client.postgrest.rpc("rs_support_feed_v2").decodeList<RsCloudSupportTicketV99>()
    }.getOrElse{
        client.postgrest.rpc("rs_support_feed").decodeList<RsCloudSupportTicketV99>()
    }
}

suspend fun rsCreateCloudSupportTicketV99(
    subject:String,
    message:String,
    attachment:RsChatAttachmentV92?=null
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val v2=runCatching{
        client.postgrest.rpc(
            "rs_create_support_ticket_v2",
            buildJsonObject{
                put("p_subject",subject.trim())
                put("p_message",message.trim())
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
        require(attachment==null){"Support media requires the latest RS CHAT backend update."}
        client.postgrest.rpc(
            "rs_create_support_ticket",
            buildJsonObject{
                put("p_subject",subject.trim())
                put("p_message",message.trim())
            }
        )
    }
    Unit
}

suspend fun rsUpdateCloudSupportTicketV99(
    id:String,
    reply:String,
    status:String,
    attachment:RsChatAttachmentV92?=null
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val v2=runCatching{
        client.postgrest.rpc(
            "rs_staff_update_support_ticket_v2",
            buildJsonObject{
                put("p_ticket_id",id)
                put("p_trainer_reply",reply)
                put("p_status",status)
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
        require(attachment==null){"Support media requires the latest RS CHAT backend update."}
        client.postgrest.rpc(
            "rs_staff_update_support_ticket",
            buildJsonObject{
                put("p_ticket_id",id)
                put("p_trainer_reply",reply)
                put("p_status",status)
            }
        )
    }
    Unit
}

suspend fun rsDeleteCloudSupportTicketV99(id:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_delete_support_ticket",buildJsonObject{put("p_ticket_id",id)})
    Unit
}

suspend fun rsCloudReferralsV99():Result<List<RsCloudReferralV99>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_referral_feed").decodeList<RsCloudReferralV99>()
}

suspend fun rsSetCloudReferralActiveV99(id:String,active:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_referral_active",
        buildJsonObject{
            put("p_referral_id",id)
            put("p_active",active)
        }
    )
    Unit
}

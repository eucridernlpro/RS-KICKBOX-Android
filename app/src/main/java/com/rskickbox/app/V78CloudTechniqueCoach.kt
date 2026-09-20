package com.rskickbox.app

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

private const val RS_TECHNIQUE_BUCKET_V78="rs-technique-submissions"

@Serializable
data class RsCloudTechniqueSubmissionV78(
    val id:String,
    @SerialName("student_id") val studentId:String,
    @SerialName("student_email") val studentEmail:String,
    @SerialName("student_name") val studentName:String,
    val technique:String,
    @SerialName("media_path") val mediaPath:String,
    @SerialName("media_name") val mediaName:String,
    @SerialName("student_summary") val studentSummary:String,
    @SerialName("trainer_note") val trainerNote:String,
    @SerialName("trainer_favorite") val trainerFavorite:Boolean,
    @SerialName("created_at") val createdAt:String
)

private fun rsTechniqueMimeV78(context:Context,uri:Uri):String =
    context.contentResolver.getType(uri)?.lowercase()
        ?: when(uri.toString().substringAfterLast('.', "").lowercase()){
            "webm"->"video/webm"
            "mov"->"video/quicktime"
            "3gp"->"video/3gpp"
            else->"video/mp4"
        }

private fun rsTechniqueExtV78(mime:String):String=when(mime){
    "video/webm"->"webm"
    "video/quicktime"->"mov"
    "video/3gpp"->"3gp"
    else->"mp4"
}

private fun rsTechniqueBytesV78(context:Context,uri:Uri):ByteArray{
    return if(uri.scheme=="file"){
        val path=uri.path ?: error("Technique video path is missing.")
        File(path).readBytes()
    }else{
        context.contentResolver.openInputStream(uri)?.use{it.readBytes()}
            ?:error("Could not read technique video.")
    }
}

suspend fun rsUploadTechniqueSubmissionV78(
    context:Context,
    localUri:String,
    mediaName:String,
    technique:String,
    summary:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val user=client.auth.currentUserOrNull() ?: error("Please sign in again.")
    val uri=Uri.parse(localUri)
    val mime=rsTechniqueMimeV78(context,uri)
    val ext=rsTechniqueExtV78(mime)
    val path=user.id+"/"+UUID.randomUUID().toString()+"."+ext
    val bytes=rsTechniqueBytesV78(context,uri)
    require(bytes.size<=50*1024*1024){"Technique video is larger than 50 MB."}

    val capacity=rsStudentStorageCapacityV114(user.id,bytes.size.toLong()).getOrThrow()
    require(capacity.allowed){
        capacity.warningMessage.ifBlank{
            "Student storage limit reached. Free some space or increase the student's storage allowance."
        }
    }

    client.storage.from(RS_TECHNIQUE_BUCKET_V78).upload(path,bytes){
        upsert=false
        contentType=runCatching{ContentType.parse(mime)}.getOrDefault(ContentType.Video.MP4)
    }

    try{
        client.from("rs_technique_submissions").insert(
            buildJsonObject{
                put("student_id",user.id)
                put("technique",technique)
                put("media_path",path)
                put("media_name",mediaName)
                put("student_summary",summary)
            }
        )
        rsRegisterStudentMediaAssetV111(
            studentId=user.id,
            bucket=RS_TECHNIQUE_BUCKET_V78,
            path=path,
            kind="VIDEO",
            sourceArea="TECHNIQUE",
            byteSize=bytes.size.toLong()
        ).getOrThrow()
    }catch(t:Throwable){
        runCatching{client.storage.from(RS_TECHNIQUE_BUCKET_V78).delete(path)}
        throw t
    }
}

suspend fun rsCloudTechniqueSubmissionsV78():Result<List<RsCloudTechniqueSubmissionV78>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_technique_submission_catalog")
        .decodeList<RsCloudTechniqueSubmissionV78>()
}

suspend fun rsSetTechniqueReviewV78(
    submissionId:String,
    favorite:Boolean,
    note:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_technique_review",
        buildJsonObject{
            put("p_submission_id",submissionId)
            put("p_favorite",favorite)
            put("p_trainer_note",note.take(2000))
        }
    )
    Unit
}

suspend fun rsTechniqueLocalUriV78(
    context:Context,
    submission:RsCloudTechniqueSubmissionV78
):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val ext=submission.mediaPath.substringAfterLast('.', "mp4")
    val dir=File(context.cacheDir,"rs_cloud_technique").apply{mkdirs()}
    val file=File(dir,submission.id+"."+ext)
    if(!file.exists() || file.length()==0L){
        val bytes=client.storage.from(RS_TECHNIQUE_BUCKET_V78)
            .downloadAuthenticated(submission.mediaPath)
        file.writeBytes(bytes)
    }
    Uri.fromFile(file).toString()
}

suspend fun rsDeleteTechniqueSubmissionV78(
    submission:RsCloudTechniqueSubmissionV78
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.from("rs_technique_submissions").delete{
        filter{eq("id",submission.id)}
    }
    runCatching{
        client.storage.from(RS_TECHNIQUE_BUCKET_V78).delete(submission.mediaPath)
    }
    Unit
}

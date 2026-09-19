package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsAcademyProgressRowV86(
    @SerialName("track_code") val trackCode:String,
    @SerialName("completed_lessons") val completedLessons:Int
)

suspend fun rsCloudAcademyProgressV86():Result<Map<String,Int>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_academy_progress_feed")
        .decodeList<RsAcademyProgressRowV86>()
        .associate{it.trackCode to it.completedLessons}
}

suspend fun rsSetCloudAcademyProgressV86(trackCode:String,completed:Int):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_academy_progress",
        buildJsonObject{
            put("p_track_code",trackCode)
            put("p_completed_lessons",completed.coerceIn(0,100))
        }
    )
    Unit
}

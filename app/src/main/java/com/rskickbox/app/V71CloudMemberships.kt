package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudPlanV71(
    val code:String,
    val name:String,
    @SerialName("monthly_cents") val monthlyCents:Int,
    val currency:String="EUR",
    val description:String="",
    val active:Boolean=true
)

@Serializable
data class RsCloudStudentAccessV71(
    val id:String,
    val email:String,
    @SerialName("display_name") val displayName:String,
    val plan:String,
    val active:Boolean,
    @SerialName("membership_status") val membershipStatus:String,
    @SerialName("amount_cents") val amountCents:Int,
    @SerialName("current_period_end") val currentPeriodEnd:String?=null
)

@Serializable
data class RsMyMembershipV71(
    val plan:String,
    val active:Boolean,
    @SerialName("membership_status") val membershipStatus:String,
    @SerialName("amount_cents") val amountCents:Int,
    val currency:String="EUR",
    @SerialName("current_period_end") val currentPeriodEnd:String?=null
)

suspend fun rsCloudPlansV71():Result<List<RsPlanV49>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_membership_plan_catalog")
        .decodeList<RsCloudPlanV71>()
        .map{RsPlanV49(it.code,it.name,it.monthlyCents,it.active,it.description)}
}

suspend fun rsUpdateCloudPlanV71(plan:RsPlanV49):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_update_membership_plan",
        buildJsonObject{
            put("p_code",plan.code)
            put("p_monthly_cents",plan.monthlyCents)
            put("p_description",plan.description)
            put("p_active",plan.active)
        }
    )
    Unit
}

suspend fun rsCloudStudentAccessV71():Result<List<RsCloudStudentAccessV71>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_student_access_catalog")
        .decodeList<RsCloudStudentAccessV71>()
}

suspend fun rsSetCloudStudentAccessV71(
    studentId:String,
    plan:String,
    active:Boolean,
    membershipStatus:String="active"
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_student_access",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_plan",plan)
            put("p_active",active)
            put("p_membership_status",membershipStatus)
        }
    )
    Unit
}

suspend fun rsMyMembershipV71():RsMyMembershipV71?{
    val client=rsSupabaseClientV60() ?: return null
    return runCatching{
        client.postgrest.rpc("rs_my_membership")
            .decodeList<RsMyMembershipV71>()
            .firstOrNull()
    }.getOrNull()
}

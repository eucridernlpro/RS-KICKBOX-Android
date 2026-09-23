package com.rskickbox.app

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudAppControlsV82(
    @SerialName("maintenance_enabled") val maintenanceEnabled:Boolean=false,
    @SerialName("maintenance_message") val maintenanceMessage:String="",
    @SerialName("community_posts_enabled") val communityPostsEnabled:Boolean=true,
    @SerialName("class_booking_enabled") val classBookingEnabled:Boolean=true,
    @SerialName("private_lessons_enabled") val privateLessonsEnabled:Boolean=true,
    @SerialName("referrals_enabled") val referralsEnabled:Boolean=true,
    @SerialName("in_app_reminders_enabled") val inAppRemindersEnabled:Boolean=true,
    @SerialName("retention_months") val retentionMonths:Int=24,
    @SerialName("disabled_student_routes") val disabledStudentRoutes:List<String> = emptyList()
)

val RsStudentLockableRoutesV82=listOf(
    "voice","session","academy","techniques","home_training","workout",
    "classes","events","coachchat","community","groups","private_lessons",
    "notifications","promotions","checkin","documents","referrals",
    "progress","challenges","badges","fightcamp","compare","history",
    "vault","homework","favorites","media","music","finance","book","search"
)

private const val RS_DISABLED_ROUTES_CACHE_V82="cloud_disabled_student_routes_v82"

fun rsDisabledStudentRoutesV82(store:RsStore):Set<String> =
    store.s(RS_DISABLED_ROUTES_CACHE_V82,"")
        .split('|')
        .map{it.trim()}
        .filter{it.isNotBlank()}
        .toSet()

fun rsCacheCloudControlsV82(store:RsStore,controls:RsCloudAppControlsV82){
    store.pb(RsOpsKeysV56.MAINTENANCE,controls.maintenanceEnabled)
    store.ps(RsOpsKeysV56.MAINTENANCE_MESSAGE,controls.maintenanceMessage)
    store.pb(RsOpsKeysV56.COMMUNITY_POSTS,controls.communityPostsEnabled)
    store.pb(RsOpsKeysV56.CLASS_BOOKING,controls.classBookingEnabled)
    store.pb(RsOpsKeysV56.PRIVATE_LESSONS,controls.privateLessonsEnabled)
    store.pb(RsOpsKeysV56.REFERRALS,controls.referralsEnabled)
    store.pb(RsOpsKeysV56.REMINDERS,controls.inAppRemindersEnabled)
    store.ps(RsOpsKeysV56.RETENTION,controls.retentionMonths.toString()+" months")
    store.ps(RS_DISABLED_ROUTES_CACHE_V82,controls.disabledStudentRoutes.distinct().joinToString("|"))
}

suspend fun rsCloudAppControlsV82():Result<RsCloudAppControlsV82> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_app_controls")
        .decodeList<RsCloudAppControlsV82>()
        .firstOrNull()
        ?:error("App controls are not available.")
}

suspend fun rsSyncCloudControlsV82(store:RsStore):Result<RsCloudAppControlsV82> =
    rsCloudAppControlsV82().onSuccess{rsCacheCloudControlsV82(store,it)}

suspend fun rsSaveCloudControlsV82(
    store:RsStore,
    maintenanceEnabled:Boolean,
    maintenanceMessage:String,
    communityPostsEnabled:Boolean,
    classBookingEnabled:Boolean,
    privateLessonsEnabled:Boolean,
    referralsEnabled:Boolean,
    remindersEnabled:Boolean,
    retentionMonths:Int,
    disabledRoutes:Set<String>
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_update_app_controls",
        buildJsonObject{
            put("p_maintenance_enabled",maintenanceEnabled)
            put("p_maintenance_message",maintenanceMessage.take(240))
            put("p_community_posts_enabled",communityPostsEnabled)
            put("p_class_booking_enabled",classBookingEnabled)
            put("p_private_lessons_enabled",privateLessonsEnabled)
            put("p_referrals_enabled",referralsEnabled)
            put("p_in_app_reminders_enabled",remindersEnabled)
            put("p_retention_months",retentionMonths)
            put(
                "p_disabled_student_routes",
                JsonArray(disabledRoutes.filter{it in RsStudentLockableRoutesV82}.sorted().map{JsonPrimitive(it)})
            )
        }
    )
    val controls=RsCloudAppControlsV82(
        maintenanceEnabled,
        maintenanceMessage.take(240),
        communityPostsEnabled,
        classBookingEnabled,
        privateLessonsEnabled,
        referralsEnabled,
        remindersEnabled,
        retentionMonths,
        disabledRoutes.filter{it in RsStudentLockableRoutesV82}.sorted()
    )
    rsCacheCloudControlsV82(store,controls)
}

fun rsStudentRouteEnabledV82(store:RsStore,route:String):Boolean{
    // Temporary full-access test mode: keep every student feature visible by
    // default so the complete app can be physically tested. This can later be
    // switched off without changing the route architecture.
    if(store.b("student_full_access_test_v142",true))return true

    if(route in setOf("home","student_guide","profile","settings","support","music"))return true
    if(route in rsDisabledStudentRoutesV82(store))return false
    return when(route){
        "private_lessons"->rsOpsEnabledV56(store,RsOpsKeysV56.PRIVATE_LESSONS,true)
        "referrals"->rsOpsEnabledV56(store,RsOpsKeysV56.REFERRALS,true)
        else->true
    }
}

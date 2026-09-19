package com.rskickbox.app

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsCloudBrandSettingsV100(
    @SerialName("header_name") val headerName:String,
    @SerialName("login_title") val loginTitle:String,
    @SerialName("login_subtitle") val loginSubtitle:String,
    @SerialName("footer_text") val footerText:String,
    @SerialName("theme_name") val themeName:String,
    @SerialName("login_form_opacity") val loginFormOpacity:Double
)

suspend fun rsCloudBrandSettingsV100():Result<RsCloudBrandSettingsV100> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.from("rs_brand_settings")
        .select()
        .decodeSingle<RsCloudBrandSettingsV100>()
}

suspend fun rsSyncCloudBrandV100(store:RsStore):Result<RsCloudBrandSettingsV100> = runCatching{
    val settings=rsCloudBrandSettingsV100().getOrThrow()
    store.ps("brand_header_name",settings.headerName)
    store.ps("brand_login_title",settings.loginTitle)
    store.ps("brand_login_subtitle",settings.loginSubtitle)
    store.ps("brand_footer_text",settings.footerText)
    store.ps("theme",settings.themeName)
    store.ps("login_form_opacity",settings.loginFormOpacity.coerceIn(.20,1.0).toString())
    settings
}

suspend fun rsSaveCloudBrandV100(
    headerName:String,
    loginTitle:String,
    loginSubtitle:String,
    footerText:String,
    themeName:String,
    loginFormOpacity:Float
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_save_brand_settings",
        buildJsonObject{
            put("p_header_name",headerName.trim())
            put("p_login_title",loginTitle.trim())
            put("p_login_subtitle",loginSubtitle.trim())
            put("p_footer_text",footerText.trim())
            put("p_theme_name",themeName)
            put("p_login_form_opacity",loginFormOpacity.coerceIn(.20f,1f))
        }
    )
    Unit
}

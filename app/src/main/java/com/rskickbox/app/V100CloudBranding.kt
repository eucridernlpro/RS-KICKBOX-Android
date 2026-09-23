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
    @SerialName("login_form_opacity") val loginFormOpacity:Double,
    @SerialName("intro_enabled") val introEnabled:Boolean=true,
    @SerialName("intro_every_launch") val introEveryLaunch:Boolean=true,
    @SerialName("intro_video_sound") val introVideoSound:Boolean=true,
    @SerialName("intro_skip_enabled") val introSkipEnabled:Boolean=true
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
    val localThemeOverrideMs=store.s("theme_local_override_ms_v170","0").toLongOrNull()?:0L
    val localTheme=store.s("theme","ELITE_GOLD")
    val preserveLocalTheme=localThemeOverrideMs>0L
    if(preserveLocalTheme){
        // A theme explicitly confirmed on this device remains authoritative
        // until cloud returns the same value. This prevents delayed cloud
        // refreshes from undoing a newly selected style.
        if(settings.themeName==localTheme){
            store.ps("theme_cloud_confirmed_v176",localTheme)
            store.ps("theme_local_override_ms_v170","0")
        }
    }else{
        store.ps("theme",settings.themeName)
    }
    store.ps("login_form_opacity",settings.loginFormOpacity.coerceIn(.20,1.0).toString())
    store.pb("intro_enabled",settings.introEnabled)
    store.pb("intro_every_launch",settings.introEveryLaunch)
    store.pb("intro_video_sound",settings.introVideoSound)
    store.pb("intro_skip_enabled",settings.introSkipEnabled)
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


fun rsBrandUiV100(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "Branding & Site Settings","sub" to "Global RS identity shared across login and member devices.",
        "identity" to "APP IDENTITY","header" to "Header / app name","login_title" to "Login title","login_subtitle" to "Login subtitle",
        "footer" to "Footer text","opacity" to "Login panel opacity","save" to "Save global brand settings",
        "saving" to "Saving…","saved" to "✓ Global branding saved to Supabase.","save_failed" to "Could not save global branding.",
        "assets" to "BRAND ASSETS","asset_note" to "Logo and visual uploads are still device-local in this stage. They will move to protected cloud storage in the next media pass.",
        "upload" to "Upload","replace" to "Replace","reset" to "Reset","release" to "RELEASE NOTE",
        "release_note" to "Launcher icons remain build-time resources and will be packaged for the signed Play Store release."
    )
    val nl=en+mapOf(
        "title" to "Branding & Site-instellingen","sub" to "Globale RS-identiteit gedeeld op login en apparaten van leden.","identity" to "APP-IDENTITEIT",
        "header" to "Header / appnaam","login_title" to "Logintitel","login_subtitle" to "Loginondertitel","footer" to "Voettekst","opacity" to "Doorzichtigheid loginpaneel",
        "save" to "Globale branding opslaan","saving" to "Opslaan…","saved" to "✓ Globale branding opgeslagen in Supabase.","save_failed" to "Globale branding kon niet worden opgeslagen.",
        "assets" to "MERKASSETS","asset_note" to "Logo- en visualuploads zijn in deze fase nog lokaal op het apparaat. Ze gaan in de volgende mediastap naar beveiligde cloudopslag.",
        "upload" to "Uploaden","replace" to "Vervangen","reset" to "Resetten","release" to "RELEASE-OPMERKING","release_note" to "Launcher-iconen blijven build-resources en worden verpakt voor de ondertekende Play Store-release."
    )
    val pt=en+mapOf(
        "title" to "Branding & Definições","sub" to "Identidade global RS partilhada no login e nos dispositivos dos membros.","identity" to "IDENTIDADE DA APP",
        "header" to "Cabeçalho / nome da app","login_title" to "Título do login","login_subtitle" to "Subtítulo do login","footer" to "Texto do rodapé","opacity" to "Opacidade do painel de login",
        "save" to "Guardar branding global","saving" to "A guardar…","saved" to "✓ Branding global guardado no Supabase.","save_failed" to "Não foi possível guardar o branding global.",
        "assets" to "RECURSOS DA MARCA","asset_note" to "Logos e visuais ainda são locais nesta fase. Serão movidos para armazenamento cloud protegido no próximo passo.",
        "upload" to "Carregar","replace" to "Substituir","reset" to "Repor","release" to "NOTA DE RELEASE","release_note" to "Os ícones launcher continuam recursos de build e serão incluídos na release assinada da Play Store."
    )
    val es=en+mapOf(
        "title" to "Branding y Ajustes","sub" to "Identidad global RS compartida en el login y los dispositivos de miembros.","identity" to "IDENTIDAD DE LA APP",
        "header" to "Cabecera / nombre de app","login_title" to "Título del login","login_subtitle" to "Subtítulo del login","footer" to "Texto del pie","opacity" to "Opacidad del panel de login",
        "save" to "Guardar branding global","saving" to "Guardando…","saved" to "✓ Branding global guardado en Supabase.","save_failed" to "No se pudo guardar el branding global.",
        "assets" to "RECURSOS DE MARCA","asset_note" to "Los logos y visuales siguen siendo locales en esta fase. Pasarán a almacenamiento cloud protegido en el siguiente paso.",
        "upload" to "Subir","replace" to "Reemplazar","reset" to "Restablecer","release" to "NOTA DE RELEASE","release_note" to "Los iconos launcher siguen siendo recursos de compilación y se incluirán en la versión firmada de Play Store."
    )
    val fr=en+mapOf(
        "title" to "Branding & Réglages","sub" to "Identité RS globale partagée sur la connexion et les appareils des membres.","identity" to "IDENTITÉ DE L'APP",
        "header" to "En-tête / nom de l'app","login_title" to "Titre de connexion","login_subtitle" to "Sous-titre de connexion","footer" to "Texte du pied","opacity" to "Opacité du panneau de connexion",
        "save" to "Enregistrer le branding global","saving" to "Enregistrement…","saved" to "✓ Branding global enregistré dans Supabase.","save_failed" to "Impossible d'enregistrer le branding global.",
        "assets" to "RESSOURCES DE MARQUE","asset_note" to "Les logos et visuels restent locaux à ce stade. Ils passeront au stockage cloud protégé lors de la prochaine étape média.",
        "upload" to "Importer","replace" to "Remplacer","reset" to "Réinitialiser","release" to "NOTE DE RELEASE","release_note" to "Les icônes launcher restent des ressources de build et seront incluses dans la version Play Store signée."
    )
    val de=en+mapOf(
        "title" to "Branding & Einstellungen","sub" to "Globale RS-Identität für Login und Mitgliedergeräte.","identity" to "APP-IDENTITÄT",
        "header" to "Header / App-Name","login_title" to "Login-Titel","login_subtitle" to "Login-Untertitel","footer" to "Footer-Text","opacity" to "Deckkraft des Login-Panels",
        "save" to "Globales Branding speichern","saving" to "Speichern…","saved" to "✓ Globales Branding in Supabase gespeichert.","save_failed" to "Globales Branding konnte nicht gespeichert werden.",
        "assets" to "MARKEN-ASSETS","asset_note" to "Logo- und Visual-Uploads sind in dieser Phase noch gerätelokal. Im nächsten Medien-Schritt wechseln sie in geschützten Cloud-Speicher.",
        "upload" to "Hochladen","replace" to "Ersetzen","reset" to "Zurücksetzen","release" to "RELEASE-HINWEIS","release_note" to "Launcher-Icons bleiben Build-Ressourcen und werden für die signierte Play-Store-Version verpackt."
    )
    val it=en+mapOf(
        "title" to "Branding & Impostazioni","sub" to "Identità RS globale condivisa nel login e sui dispositivi dei membri.","identity" to "IDENTITÀ APP",
        "header" to "Header / nome app","login_title" to "Titolo login","login_subtitle" to "Sottotitolo login","footer" to "Testo footer","opacity" to "Opacità pannello login",
        "save" to "Salva branding globale","saving" to "Salvataggio…","saved" to "✓ Branding globale salvato in Supabase.","save_failed" to "Impossibile salvare il branding globale.",
        "assets" to "ASSET DEL BRAND","asset_note" to "Logo e visual restano locali in questa fase. Passeranno allo storage cloud protetto nel prossimo passaggio media.",
        "upload" to "Carica","replace" to "Sostituisci","reset" to "Reimposta","release" to "NOTA RELEASE","release_note" to "Le icone launcher restano risorse di build e saranno incluse nella release firmata Play Store."
    )
    val pl=en+mapOf(
        "title" to "Branding i Ustawienia","sub" to "Globalna identyfikacja RS wspólna dla logowania i urządzeń członków.","identity" to "IDENTYFIKACJA APLIKACJI",
        "header" to "Nagłówek / nazwa aplikacji","login_title" to "Tytuł logowania","login_subtitle" to "Podtytuł logowania","footer" to "Tekst stopki","opacity" to "Przezroczystość panelu logowania",
        "save" to "Zapisz globalny branding","saving" to "Zapisywanie…","saved" to "✓ Globalny branding zapisany w Supabase.","save_failed" to "Nie udało się zapisać globalnego brandingu.",
        "assets" to "ZASOBY MARKI","asset_note" to "Logo i grafiki są jeszcze lokalne na tym etapie. W następnym kroku zostaną przeniesione do chronionego storage w chmurze.",
        "upload" to "Prześlij","replace" to "Zastąp","reset" to "Resetuj","release" to "NOTA WYDANIA","release_note" to "Ikony launchera pozostają zasobami build i zostaną spakowane do podpisanej wersji Play Store."
    )
    val tr=en+mapOf(
        "title" to "Marka & Ayarlar","sub" to "Girişte ve üye cihazlarında paylaşılan global RS kimliği.","identity" to "UYGULAMA KİMLİĞİ",
        "header" to "Başlık / uygulama adı","login_title" to "Giriş başlığı","login_subtitle" to "Giriş alt başlığı","footer" to "Altbilgi metni","opacity" to "Giriş paneli opaklığı",
        "save" to "Global markayı kaydet","saving" to "Kaydediliyor…","saved" to "✓ Global marka Supabase'e kaydedildi.","save_failed" to "Global marka kaydedilemedi.",
        "assets" to "MARKA DOSYALARI","asset_note" to "Logo ve görseller bu aşamada hâlâ cihazda yereldir. Sonraki medya adımında korumalı bulut depolamaya taşınacak.",
        "upload" to "Yükle","replace" to "Değiştir","reset" to "Sıfırla","release" to "YAYIN NOTU","release_note" to "Launcher simgeleri build kaynakları olarak kalır ve imzalı Play Store sürümüne paketlenir."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}


suspend fun rsSaveCloudIntroSettingsV102(
    enabled:Boolean,
    everyLaunch:Boolean,
    sound:Boolean,
    skipEnabled:Boolean
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_save_intro_settings",
        buildJsonObject{
            put("p_intro_enabled",enabled)
            put("p_intro_every_launch",everyLaunch)
            put("p_intro_video_sound",sound)
            put("p_intro_skip_enabled",skipEnabled)
        }
    )
    Unit
}

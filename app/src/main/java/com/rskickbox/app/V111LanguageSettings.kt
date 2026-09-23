package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val RS_SUPPORTED_LANGUAGE_CODES_V111=setOf("en","nl","pt","es","fr","de","it","pl","tr")

fun rsDeviceLanguageV111():RsLang{
    val code=Locale.getDefault().language.lowercase(Locale.ROOT)
    val safe=if(code in RS_SUPPORTED_LANGUAGE_CODES_V111)code else "en"
    return rsLangs.firstOrNull{it.code==safe}?:rsLangs.first()
}

fun rsInitialLanguageV111(store:RsStore):RsLang{
    val manual=store.b("lang_manual_override_v111",false)
    if(manual){
        val saved=store.s("lang","en")
        return rsLangs.firstOrNull{it.code==saved}?:rsDeviceLanguageV111()
    }
    val detected=rsDeviceLanguageV111()
    store.ps("lang",detected.code)
    return detected
}

private fun rsLanguageSettingsT111(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "App language",
        "sub" to "By default RS KICKBOXING follows your phone language when that language is supported.",
        "phone" to "Use phone language",
        "manual" to "Choose another supported language",
        "active" to "Current language"
    )
    val nl=en+mapOf("title" to "App-taal","sub" to "RS KICKBOXING volgt standaard de taal van je telefoon wanneer die taal wordt ondersteund.","phone" to "Telefoontaal gebruiken","manual" to "Kies een andere ondersteunde taal","active" to "Huidige taal")
    val pt=en+mapOf("title" to "Idioma da app","sub" to "Por padrão, RS KICKBOXING segue o idioma do telefone quando esse idioma é suportado.","phone" to "Usar idioma do telefone","manual" to "Escolher outro idioma suportado","active" to "Idioma atual")
    val es=en+mapOf("title" to "Idioma de la app","sub" to "Por defecto, RS KICKBOXING usa el idioma del teléfono cuando está disponible.","phone" to "Usar idioma del teléfono","manual" to "Elegir otro idioma compatible","active" to "Idioma actual")
    val fr=en+mapOf("title" to "Langue de l’app","sub" to "Par défaut, RS KICKBOXING suit la langue du téléphone lorsqu’elle est prise en charge.","phone" to "Utiliser la langue du téléphone","manual" to "Choisir une autre langue disponible","active" to "Langue actuelle")
    val de=en+mapOf("title" to "App-Sprache","sub" to "Standardmäßig folgt RS KICKBOXING der Telefonsprache, wenn diese unterstützt wird.","phone" to "Telefonsprache verwenden","manual" to "Andere unterstützte Sprache wählen","active" to "Aktuelle Sprache")
    val it=en+mapOf("title" to "Lingua dell’app","sub" to "Per impostazione predefinita RS KICKBOXING segue la lingua del telefono quando supportata.","phone" to "Usa lingua del telefono","manual" to "Scegli un’altra lingua supportata","active" to "Lingua attuale")
    val pl=en+mapOf("title" to "Język aplikacji","sub" to "Domyślnie RS KICKBOXING używa języka telefonu, jeśli jest obsługiwany.","phone" to "Użyj języka telefonu","manual" to "Wybierz inny obsługiwany język","active" to "Aktualny język")
    val tr=en+mapOf("title" to "Uygulama dili","sub" to "Varsayılan olarak RS KICKBOXING, destekleniyorsa telefon dilini kullanır.","phone" to "Telefon dilini kullan","manual" to "Başka desteklenen dil seç","active" to "Geçerli dil")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsLanguageSettingsPanelV111(
    c:RsPalette,
    store:RsStore,
    current:RsLang,
    onSelect:(RsLang)->Unit
){
    var open by remember{mutableStateOf(false)}
    val manual=store.b("lang_manual_override_v111",false)
    RsPanel(c){
        Text(rsLanguageSettingsT111(current,"title"),color=c.bright,fontWeight=FontWeight.Black)
        Text(rsLanguageSettingsT111(current,"sub"),color=c.muted)
        Text(rsLanguageSettingsT111(current,"active")+": "+current.name,color=c.text,fontWeight=FontWeight.Bold)
        Button(
            onClick={
                store.pb("lang_manual_override_v111",false)
                val detected=rsDeviceLanguageV111()
                store.ps("lang",detected.code)
                open=false
                onSelect(detected)
            },
            modifier=Modifier.fillMaxWidth()
        ){Text(rsLanguageSettingsT111(current,"phone"))}
        Text(rsLanguageSettingsT111(current,"manual"),color=c.muted,fontSize=10.sp)
        Box(Modifier.fillMaxWidth()){
            OutlinedButton(
                onClick={open=true},
                modifier=Modifier.fillMaxWidth()
            ){
                Text(if(manual)current.name else rsLanguageSettingsT111(current,"phone"))
            }
            DropdownMenu(
                expanded=open,
                onDismissRequest={open=false}
            ){
                rsLangs.forEach{language->
                    DropdownMenuItem(
                        text={Text(language.name)},
                        onClick={
                            store.pb("lang_manual_override_v111",true)
                            store.ps("lang",language.code)
                            open=false
                            onSelect(language)
                        }
                    )
                }
            }
        }
    }
}


fun rsVisibleBrandNameV111(store:RsStore):String{
    val raw=store.s("brand_header_name","RS KICKBOXING").trim()
    return when{
        raw.equals("RS KICKBOX",true)->"RS KICKBOXING"
        raw.equals("RS KICKBOXING",true)->"RS KICKBOXING"
        raw.isBlank()->"RS KICKBOXING"
        else->raw
    }
}

@Composable
fun RsThemeHeaderMarkV176(
    c:RsPalette,
    store:RsStore,
    modifier:Modifier=Modifier
){
    val theme=rsStoredThemeV175(store)
    val layout=rsThemeLayoutV175(theme)
    val radius=when(layout.mode){
        "FIGHT_STRIP"->10.dp
        "TECH_COMPACT"->8.dp
        "HOLO_CARDS"->18.dp
        "PERFORMANCE_STACK"->14.dp
        else->16.dp
    }
    Surface(
        color=c.panel2,
        shape=RoundedCornerShape(radius),
        border=androidx.compose.foundation.BorderStroke(
            if(layout.strongLines)2.dp else 1.dp,
            c.bright.copy(alpha=.78f)
        ),
        tonalElevation=10.dp,
        modifier=modifier
    ){
        Box(
            Modifier.fillMaxSize(),
            contentAlignment=Alignment.Center
        ){
            Text(
                "RS",
                color=c.bright,
                fontWeight=FontWeight.Black,
                fontSize=17.sp,
                letterSpacing=.6.sp
            )
        }
    }
}

@Composable
fun RsLettersLogoV111(c:RsPalette,store:RsStore,modifier:Modifier=Modifier){
    val raw=rsVisibleBrandNameV111(store).uppercase(Locale.ROOT)
    val standard=raw=="RS KICKBOXING"
    if(standard){
        Column(modifier,verticalArrangement=Arrangement.spacedBy(0.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                Text(
                    "RS",
                    color=c.gold,
                    fontWeight=FontWeight.Black,
                    fontSize=11.sp,
                    letterSpacing=1.2.sp
                )
                Text(
                    "KICKBOXING",
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=16.sp,
                    letterSpacing=1.1.sp,
                    maxLines=1
                )
            }
        }
    }else{
        Text(
            text=raw.ifBlank{"RS KICKBOXING"},
            color=c.bright,
            fontWeight=FontWeight.Black,
            fontSize=17.sp,
            letterSpacing=1.0.sp,
            maxLines=1,
            overflow=TextOverflow.Ellipsis,
            modifier=modifier
        )
    }
}

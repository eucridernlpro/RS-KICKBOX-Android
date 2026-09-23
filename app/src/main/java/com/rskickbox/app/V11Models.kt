package com.rskickbox.app

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.util.Locale

data class RsLang(val code:String,val name:String,val locale:Locale)
val rsLangs=listOf(
    RsLang("en","English",Locale.ENGLISH),
    RsLang("nl","Nederlands",Locale("nl","NL")),
    RsLang("pt","Português",Locale("pt","PT")),
    RsLang("es","Español",Locale("es","ES")),
    RsLang("fr","Français",Locale.FRENCH),
    RsLang("de","Deutsch",Locale.GERMAN),
    RsLang("it","Italiano",Locale.ITALIAN),
    RsLang("pl","Polski",Locale("pl","PL")),
    RsLang("tr","Türkçe",Locale("tr","TR"))
)

enum class RsRole{STUDENT,TRAINER}
enum class BgScope{LOGIN,STUDENT_TRAINING,TRAINER_TRAINING,PROMO}
enum class BgStyle{CINEMATIC_RING,GOLD_SMOKE,ARENA_LIGHTS,RED_CORNER,MINIMAL_DARK}
enum class BgPos{LEFT,CENTER,RIGHT,TOP,BOTTOM}
enum class RsTheme{
    ELITE_GOLD,
    CRIMSON_FIGHT_NIGHT,
    PLATINUM_PRO,
    EMERALD_PERFORMANCE,
    ROYAL_SAPPHIRE,
    PURPLE_LEGACY,
    ICE_TITANIUM,
    INFERNO_NEON
}

data class RsPalette(val bg:Color,val panel:Color,val panel2:Color,val gold:Color,val bright:Color,val text:Color,val muted:Color)
val elitePalette=RsPalette(Color(0xFF050505),Color(0xFF15120D),Color(0xFF211A10),Color(0xFFC08A24),Color(0xFFF0CF79),Color(0xFFF6F0E4),Color(0xFFB8AD98))
val crimsonPalette=RsPalette(Color(0xFF080405),Color(0xFF190C10),Color(0xFF2A1117),Color(0xFF8F2238),Color(0xFFFFB39B),Color(0xFFFFF3EF),Color(0xFFC9A2A0))
val platinumPalette=RsPalette(Color(0xFF04070A),Color(0xFF10161D),Color(0xFF17232E),Color(0xFF6E8295),Color(0xFFE4EDF5),Color(0xFFF5F8FB),Color(0xFFA8B6C2))
val emeraldPalette=RsPalette(Color(0xFF020806),Color(0xFF0A1711),Color(0xFF11231A),Color(0xFF267A59),Color(0xFFB8E5C8),Color(0xFFF1FAF4),Color(0xFFA5B9AC))
val sapphirePalette=RsPalette(Color(0xFF02040A),Color(0xFF07101E),Color(0xFF0A1930),Color(0xFF1256A3),Color(0xFF6CC8FF),Color(0xFFF3F8FF),Color(0xFF9CB6CF))
val purplePalette=RsPalette(Color(0xFF08030C),Color(0xFF160A21),Color(0xFF231035),Color(0xFF7F38B5),Color(0xFFE0A5FF),Color(0xFFFFF4FF),Color(0xFFC3A6CA))
val icePalette=RsPalette(Color(0xFF020709),Color(0xFF091317),Color(0xFF10232A),Color(0xFF4B9FB3),Color(0xFFC6F5FF),Color(0xFFF4FEFF),Color(0xFFA9C4CB))
val infernoPalette=RsPalette(Color(0xFF0A0301),Color(0xFF1A0B04),Color(0xFF2E1206),Color(0xFFB54213),Color(0xFFFFB05C),Color(0xFFFFF4E9),Color(0xFFC8A38B))

fun paletteFor(theme:RsTheme)=when(theme){
    RsTheme.ELITE_GOLD->elitePalette
    RsTheme.CRIMSON_FIGHT_NIGHT->crimsonPalette
    RsTheme.PLATINUM_PRO->platinumPalette
    RsTheme.EMERALD_PERFORMANCE->emeraldPalette
    RsTheme.ROYAL_SAPPHIRE->sapphirePalette
    RsTheme.PURPLE_LEGACY->purplePalette
    RsTheme.ICE_TITANIUM->icePalette
    RsTheme.INFERNO_NEON->infernoPalette
}

class RsStore(context:Context){
    private val p=context.getSharedPreferences("rs_v12",Context.MODE_PRIVATE)
    fun s(k:String,d:String="")=p.getString(k,d)?:d
    fun ps(k:String,v:String)=p.edit().putString(k,v).apply()
    fun b(k:String,d:Boolean=false)=p.getBoolean(k,d)
    fun pb(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
    fun f(k:String,d:Float=.4f)=p.getFloat(k,d)
    fun pf(k:String,v:Float)=p.edit().putFloat(k,v).apply()
}
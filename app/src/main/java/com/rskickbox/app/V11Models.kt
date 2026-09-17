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

data class RsPalette(val bg:Color,val panel:Color,val panel2:Color,val gold:Color,val bright:Color,val text:Color,val muted:Color)
val elitePalette=RsPalette(Color(0xFF050505),Color(0xFF15120D),Color(0xFF211A10),Color(0xFFC08A24),Color(0xFFF0CF79),Color(0xFFF6F0E4),Color(0xFFB8AD98))

class RsStore(context:Context){
    private val p=context.getSharedPreferences("rs_v11",Context.MODE_PRIVATE)
    fun s(k:String,d:String="")=p.getString(k,d)?:d
    fun ps(k:String,v:String)=p.edit().putString(k,v).apply()
    fun b(k:String,d:Boolean=false)=p.getBoolean(k,d)
    fun pb(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
    fun f(k:String,d:Float=.4f)=p.getFloat(k,d)
    fun pf(k:String,v:Float)=p.edit().putFloat(k,v).apply()
}

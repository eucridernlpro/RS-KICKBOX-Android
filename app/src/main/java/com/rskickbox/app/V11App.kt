package com.rskickbox.app

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private enum class V11Role { STUDENT, TRAINER }
private enum class V11Scope { LOGIN, STUDENT_TRAINING, TRAINER_TRAINING, PROMO }
private enum class V11Bg { CINEMATIC_RING, GOLD_SMOKE, ARENA_LIGHTS, RED_CORNER, MINIMAL_DARK }
private enum class V11Pos { LEFT, CENTER, RIGHT, TOP, BOTTOM }
private enum class V11Theme { GOLD, CRIMSON, PLATINUM, EMERALD }

private data class AppLang(val code:String,val label:String,val locale:Locale)
private val appLangs=listOf(
    AppLang("en","English",Locale.ENGLISH),
    AppLang("nl","Nederlands",Locale("nl","NL")),
    AppLang("pt","Português",Locale("pt","PT")),
    AppLang("es","Español",Locale("es","ES")),
    AppLang("fr","Français",Locale.FRENCH),
    AppLang("de","Deutsch",Locale.GERMAN),
    AppLang("it","Italiano",Locale.ITALIAN),
    AppLang("pl","Polski",Locale("pl","PL")),
    AppLang("tr","Türkçe",Locale("tr","TR"))
)

private data class V11Colors(val bg:Color,val panel:Color,val panel2:Color,val accent:Color,val bright:Color,val text:Color,val muted:Color)
private val themes=mapOf(
    V11Theme.GOLD to V11Colors(Color(0xFF050505),Color(0xFF15120D),Color(0xFF211A10),Color(0xFFC08A24),Color(0xFFF0CF79),Color(0xFFF6F0E4),Color(0xFFB8AD98)),
    V11Theme.CRIMSON to V11Colors(Color(0xFF080405),Color(0xFF1C0E12),Color(0xFF2B151A),Color(0xFFA12B3D),Color(0xFFF0A58E),Color(0xFFFFF3EF),Color(0xFFC6A2A0)),
    V11Theme.PLATINUM to V11Colors(Color(0xFF04070B),Color(0xFF10161D),Color(0xFF19222C),Color(0xFF75889A),Color(0xFFE4EDF5),Color(0xFFF4F8FB),Color(0xFFA9B7C2)),
    V11Theme.EMERALD to V11Colors(Color(0xFF030806),Color(0xFF0C1712),Color(0xFF11251B),Color(0xFF26835F),Color(0xFFB9E7C7),Color(0xFFF2FAF4),Color(0xFFA5B9AC))
)

private class V11Prefs(context:Context){
    private val p=context.getSharedPreferences("rs_v11",Context.MODE_PRIVATE)
    fun s(k:String,d:String="")=p.getString(k,d)?:d
    fun ps(k:String,v:String)=p.edit().putString(k,v).apply()
    fun b(k:String,d:Boolean=false)=p.getBoolean(k,d)
    fun pb(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
    fun f(k:String,d:Float=0.5f)=p.getFloat(k,d)
    fun pf(k:String,v:Float)=p.edit().putFloat(k,v).apply()
}

@Composable
fun RsKickboxV11App(){
    val context=LocalContext.current
    val prefs=remember{V11Prefs(context)}
    var role by remember{mutableStateOf<V11Role?>(null)}
    var route by remember{mutableStateOf("dashboard")}
    var lang by remember{mutableStateOf(appLangs.firstOrNull{it.code==prefs.s("lang","en") }?:appLangs.first())}
    var theme by remember{mutableStateOf(runCatching{V11Theme.valueOf(prefs.s("theme","GOLD"))}.getOrDefault(V11Theme.GOLD))}
    val colors=themes.getValue(theme)

    MaterialTheme(colorScheme=darkColorScheme(primary=colors.bright,secondary=colors.accent,background=colors.bg,surface=colors.panel,onBackground=colors.text,onSurface=colors.text)){
        if(role==null){
            LiveBackground(colors,prefs,V11Scope.LOGIN){ LoginPage(colors,lang,{lang=it;prefs.ps("lang",it.code)}){r->role=r;route=if(r==V11Role.TRAINER)"trainer" else "dashboard"} }
        }else{
            val scope=if(role==V11Role.TRAINER) V11Scope.TRAINER_TRAINING else V11Scope.STUDENT_TRAINING
            LiveBackground(colors,prefs,scope){
                AppShell(colors,role!!,lang,route,{lang=it;prefs.ps("lang",it.code)},{route=it},{role=null}){
                    when(route){
                        "dashboard","trainer"->Dashboard(colors,role!!,lang,{route=it})
                        "backgrounds"->BackgroundStudio(colors,lang,prefs)
                        "themes"->ThemeStudio(colors,lang,theme){theme=it;prefs.ps("theme",it.name)}
                        "assistant"->VoiceCoach(colors,lang,prefs)
                        "session"->SessionPlayer(colors,lang,prefs)
                        else->GenericPage(colors,lang,route)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBackground(c:V11Colors,p:V11Prefs,scope:V11Scope,content:@Composable BoxScope.()->Unit){
    val bg=runCatching{V11Bg.valueOf(p.s("bg_${scope.name}", if(scope==V11Scope.LOGIN)"CINEMATIC_RING" else "ARENA_LIGHTS"))}.getOrDefault(V11Bg.CINEMATIC_RING)
    val pos=runCatching{V11Pos.valueOf(p.s("pos_${scope.name}","CENTER"))}.getOrDefault(V11Pos.CENTER)
    val intensity=p.f("int_${scope.name}",if(scope==V11Scope.LOGIN).85f else .35f)
    val tr=rememberInfiniteTransition(label="live-bg")
    val move by tr.animateFloat(0f,1f,infiniteRepeatable(tween(if(scope==V11Scope.LOGIN)7000 else 12000,easing=LinearEasing),RepeatMode.Reverse),label="move")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bg,c.panel2,c.bg))).statusBarsPadding().navigationBarsPadding()){
        Canvas(Modifier.fillMaxSize()){
            val anchor=when(pos){
                V11Pos.LEFT->Offset(size.width*.25f,size.height*.55f); V11Pos.RIGHT->Offset(size.width*.75f,size.height*.55f)
                V11Pos.TOP->Offset(size.width*.5f,size.height*.28f); V11Pos.BOTTOM->Offset(size.width*.5f,size.height*.72f); else->Offset(size.width*.5f,size.height*.53f)
            }
            val a=(.06f+.18f*intensity)
            val light=when(bg){
                V11Bg.RED_CORNER->Color(0xFFC33A35); V11Bg.PLATINUM_COMPAT()->Color.White; else->c.accent
            }
            drawCircle(light.copy(alpha=a),size.minDimension*(.28f+.08f*move),Offset(anchor.x+size.width*.09f*(move-.5f),anchor.y-size.height*.2f))
            if(bg!=V11Bg.MINIMAL_DARK){
                repeat(3){i->drawLine(c.bright.copy(alpha=.08f*intensity),Offset(0f,size.height*(.42f+i*.045f)),Offset(size.width,size.height*(.44f+i*.045f)),4f)}
                // stylized fighter silhouettes
                val left=Offset(anchor.x-size.width*.13f,anchor.y)
                val right=Offset(anchor.x+size.width*.12f,anchor.y)
                fun fighter(o:Offset,flip:Float){
                    drawCircle(Color.Black.copy(alpha=.82f),size.minDimension*.035f,Offset(o.x,o.y-size.height*.12f))
                    drawLine(Color.Black.copy(alpha=.9f),Offset(o.x,o.y-size.height*.08f),Offset(o.x+flip*size.width*.015f,o.y+size.height*.06f),22f)
                    drawLine(Color.Black.copy(alpha=.9f),Offset(o.x,o.y-size.height*.03f),Offset(o.x+flip*size.width*.10f,o.y-size.height*.08f),18f)
                    drawLine(Color.Black.copy(alpha=.9f),Offset(o.x+flip*size.width*.01f,o.y+size.height*.04f),Offset(o.x-flip*size.width*.05f,o.y+size.height*.15f),20f)
                    drawLine(Color.Black.copy(alpha=.9f),Offset(o.x+flip*size.width*.01f,o.y+size.height*.04f),Offset(o.x+flip*size.width*.07f,o.y+size.height*.14f),20f)
                }
                fighter(left,1f); fighter(right,-1f)
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(scope==V11Scope.LOGIN) .42f else .72f)))
        content()
    }
}

private fun V11Bg.PLATINUM_COMPAT():V11Bg = V11Bg.ARENA_LIGHTS

@Composable
private fun LoginPage(c:V11Colors,lang:AppLang,onLang:(AppLang)->Unit,onLogin:(V11Role)->Unit){
    var email by remember{mutableStateOf("alex@rskickbox.nl")}; var pass by remember{mutableStateOf("preview123")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("♛ RS KICKBOX",color=c.bright,fontSize=31.sp,fontWeight=FontWeight.Black)
        LanguageButton(c,lang,onLang)
        Text("Premium cinematic kickboxing",color=c.text,style=MaterialTheme.typography.headlineMedium)
        Text("TRAIN · LEARN · CONNECT · GROW",color=c.muted,fontSize=11.sp)
        CardBlock(c){
            Text("MEMBER ACCESS",color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(pass,{pass=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            Button(onClick={onLogin(V11Role.STUDENT)},modifier=Modifier.fillMaxWidth()){Text("Student")}
            OutlinedButton(onClick={onLogin(V11Role.TRAINER)},modifier=Modifier.fillMaxWidth()){Text("Trainer / Admin")}
        }
    }
}

@Composable
private fun AppShell(c:V11Colors,role:V11Role,lang:AppLang,route:String,onLang:(AppLang)->Unit,onRoute:(String)->Unit,onLogout:()->Unit,content:@Composable()->Unit){
    val home=if(role==V11Role.TRAINER)"trainer" else "dashboard"
    Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        CardBlock(c){
            Text("♛ RS KICKBOX",color=c.bright,fontSize=20.sp,fontWeight=FontWeight.Black)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                LanguageButton(c,lang,onLang)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    if(route!=home) OutlinedButton(onClick={onRoute(home)},contentPadding=PaddingValues(horizontal=10.dp)){Text("‹")}
                    OutlinedButton(onClick=onLogout,contentPadding=PaddingValues(horizontal=10.dp)){Text("Log out",fontSize=10.sp)}
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)){content()}
    }
}

@Composable
private fun Dashboard(c:V11Colors,role:V11Role,lang:AppLang,onRoute:(String)->Unit){
    val tiles=if(role==V11Role.TRAINER) listOf(
        "backgrounds" to "Background Studio","themes" to "Visual Theme Studio","assistant" to "AI Voice Coach","session" to "Session Player",
        "access" to "Access & Subscriptions","payments" to "Payment Center","members" to "Student Manager","classes" to "Class Manager",
        "attendance" to "Attendance","invoices" to "Invoices","book" to "Book Manager","settings" to "Settings"
    ) else listOf(
        "assistant" to "AI Voice Coach","session" to "Session Player","academy" to "RS Academy","classes" to "Classes & Events",
        "progress" to "Progress","challenges" to "Challenges","fightcamp" to "Fight Camp","finance" to "Membership & Payments",
        "book" to "Trainer Book","community" to "Community","media" to "Training Media","settings" to "Settings"
    )
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        CardBlock(c){Text(if(role==V11Role.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp);Text("${lang.label} · ${if(role==V11Role.TRAINER)"Trainer" else "Student"}",color=c.muted)}
        LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=18.dp)){
            items(tiles){(id,label)->FeatureCard(c,label){onRoute(id)}}
        }
    }
}

@Composable
private fun BackgroundStudio(c:V11Colors,lang:AppLang,p:V11Prefs){
    var scope by remember{mutableStateOf(V11Scope.LOGIN)}
    ScrollPage(c,"Background Studio","Control the background style, strength and position on every part of the app."){
        CardBlock(c){
            Text("PAGE GROUP",color=c.bright,fontWeight=FontWeight.Bold)
            V11Scope.entries.forEach{s->FilterChip(selected=scope==s,onClick={scope=s},label={Text(s.name.replace('_',' '),fontSize=10.sp)})}
        }
        CardBlock(c){
            Text("BACKGROUND STYLE",color=c.bright,fontWeight=FontWeight.Bold)
            V11Bg.entries.forEach{bg->
                val active=runCatching{V11Bg.valueOf(p.s("bg_${scope.name}","CINEMATIC_RING"))}.getOrDefault(V11Bg.CINEMATIC_RING)==bg
                FilterChip(selected=active,onClick={p.ps("bg_${scope.name}",bg.name)},label={Text(bg.name.replace('_',' '))})
            }
        }
        CardBlock(c){
            Text("POSITION",color=c.bright,fontWeight=FontWeight.Bold)
            V11Pos.entries.forEach{pos->
                val active=runCatching{V11Pos.valueOf(p.s("pos_${scope.name}","CENTER"))}.getOrDefault(V11Pos.CENTER)==pos
                FilterChip(selected=active,onClick={p.ps("pos_${scope.name}",pos.name)},label={Text(pos.name)})
            }
        }
        CardBlock(c){
            var intensity by remember(scope){mutableFloatStateOf(p.f("int_${scope.name}",if(scope==V11Scope.LOGIN).85f else .35f))}
            Text("MOTION / VISUAL INTENSITY ${(intensity*100).toInt()}%",color=c.bright,fontWeight=FontWeight.Bold)
            Slider(value=intensity,onValueChange={intensity=it;p.pf("int_${scope.name}",it)},valueRange=0.05f..1f)
            Text("Training pages should normally stay around 25–45% so content remains easy to read.",color=c.muted,fontSize=11.sp)
        }
    }
}

@Composable
private fun ThemeStudio(c:V11Colors,lang:AppLang,current:V11Theme,onTheme:(V11Theme)->Unit){
    ScrollPage(c,"Visual Theme Studio","Switch the complete premium visual theme of the app."){
        V11Theme.entries.forEach{t->CardBlock(c){Text(t.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp);Button(onClick={onTheme(t)},enabled=t!=current,modifier=Modifier.fillMaxWidth()){Text(if(t==current)"Active" else "Activate")}}}
    }
}

@Composable
private fun VoiceCoach(c:V11Colors,lang:AppLang,p:V11Prefs){
    val context=LocalContext.current
    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ready by remember{mutableStateOf(false)}
    var question by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf("")}
    var autoSpeak by remember{mutableStateOf(p.b("voice_auto",true))}

    DisposableEffect(Unit){
        val engine=TextToSpeech(context){status->ready=status==TextToSpeech.SUCCESS}
        tts=engine
        onDispose{engine.stop();engine.shutdown()}
    }
    LaunchedEffect(lang.code,ready){if(ready)tts?.language=lang.locale}

    fun reply(q:String):String{
        val low=q.lowercase()
        return when{
            low.contains("jab")||low.contains("stoot")->"Keep your chin protected, shoulder relaxed, extend straight and return the hand quickly to guard."
            low.contains("kick")||low.contains("trap")->"Turn the supporting foot, rotate the hip, keep your guard high and recover your stance before the next action."
            low.contains("defen")||low.contains("guard")->"Keep your eyes on the chest, elbows close and return immediately to a balanced stance after every defense."
            else->"Focus on balance, clean technique and controlled breathing. Start slowly, then increase speed only while the movement stays correct."
        }
    }

    ScrollPage(c,"AI Voice Coach","Ask a training or technique question. The coach can answer aloud in the selected app language when the device has that voice installed."){
        CardBlock(c){
            Text("VOICE LANGUAGE",color=c.bright,fontWeight=FontWeight.Bold); LanguageButton(c,lang){ }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Speak answers automatically",color=c.text);Switch(autoSpeak,{autoSpeak=it;p.pb("voice_auto",it)})}
            Text(if(ready)"Voice engine ready" else "Loading device voice engine…",color=if(ready)Color(0xFF67C587) else c.muted,fontSize=11.sp)
        }
        CardBlock(c){
            OutlinedTextField(question,{question=it},label={Text("Ask the coach")},modifier=Modifier.fillMaxWidth(),minLines=2)
            Button(onClick={
                answer=reply(question)
                if(autoSpeak&&ready){tts?.language=lang.locale;tts?.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"rscoach")}
            },enabled=question.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Ask AI Coach")}
        }
        if(answer.isNotBlank()) CardBlock(c){
            Text("COACH",color=c.bright,fontWeight=FontWeight.Bold);Text(answer,color=c.text)
            Button(onClick={if(ready){{tts?.language=lang.locale;tts?.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"rscoach-repeat")}}else{{}}},modifier=Modifier.fillMaxWidth(),enabled=ready){Text("🔊 Speak answer")}
        }
    }
}

@Composable
private fun SessionPlayer(c:V11Colors,lang:AppLang,p:V11Prefs){
    var running by remember{mutableStateOf(false)}; var sec by remember{mutableIntStateOf(120)}
    LaunchedEffect(running,sec){if(running&&sec>0){delay(1000);sec--}else if(sec==0)running=false}
    ScrollPage(c,"Session Player","Premium live training background with reduced intensity for readability."){
        CardBlock(c){Text("%d:%02d".format(sec/60,sec%60),color=c.bright,fontSize=50.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=c.text);Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){Text(if(running)"Pause" else "Start")};OutlinedButton(onClick={sec=120;running=false},modifier=Modifier.fillMaxWidth()){Text("Reset")}}
    }
}

@Composable
private fun GenericPage(c:V11Colors,lang:AppLang,route:String){
    ScrollPage(c,route.replaceFirstChar{it.uppercase()},"Native RS module · ${lang.label}"){
        repeat(3){i->CardBlock(c){Text(listOf("Overview","Actions","Status")[i],color=c.bright,fontWeight=FontWeight.Bold);Text("This section remains inside the safe mobile layout and follows the selected premium theme/background.",color=c.muted)}}
    }
}

@Composable
private fun LanguageButton(c:V11Colors,current:AppLang,onSelect:(AppLang)->Unit){
    var open by remember{mutableStateOf(false)}
    Box{
        OutlinedButton(onClick={open=true},contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)){Text("🌐 ${current.label}",fontSize=11.sp)}
        DropdownMenu(expanded=open,onDismissRequest={open=false}){
            appLangs.forEach{lang->DropdownMenuItem(text={Text(lang.label)},onClick={onSelect(lang);open=false})}
        }
    }
}

@Composable
private fun ScrollPage(c:V11Colors,title:String,sub:String,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=1.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(title,color=c.bright,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(sub,color=c.muted);content();Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CardBlock(c:V11Colors,content:@Composable ColumnScope.()->Unit){
    Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.bright.copy(alpha=.28f)),colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.95f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}
}

@Composable
private fun FeatureCard(c:V11Colors,title:String,onClick:()->Unit){
    Card(modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.accent.copy(alpha=.4f)),colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.93f))){Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("♛",color=c.bright,fontSize=20.sp);Text(title,color=c.bright,fontWeight=FontWeight.Bold,maxLines=2)}}
}

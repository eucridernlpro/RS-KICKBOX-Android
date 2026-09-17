package com.rskickbox.app

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class XRole { STUDENT, TRAINER }
private enum class XLang { EN, NL }
private enum class XTheme { GOLD, CRIMSON, PLATINUM, EMERALD }
private enum class XPlan { BASIC, PRO, ELITE }
private data class XColors(val name:String,val bg:Color,val panel:Color,val panel2:Color,val accent:Color,val bright:Color,val text:Color,val muted:Color)
private data class XFeature(val id:String,val en:String,val nl:String)

private val xThemes=mapOf(
 XTheme.GOLD to XColors("Elite Gold",Color(0xFF050505),Color(0xFF15120D),Color(0xFF211A10),Color(0xFFC08A24),Color(0xFFF0CF79),Color(0xFFF6F0E4),Color(0xFFB8AD98)),
 XTheme.CRIMSON to XColors("Royal Crimson",Color(0xFF080405),Color(0xFF1C0E12),Color(0xFF2B151A),Color(0xFFA12B3D),Color(0xFFF0A58E),Color(0xFFFFF3EF),Color(0xFFC6A2A0)),
 XTheme.PLATINUM to XColors("Midnight Platinum",Color(0xFF04070B),Color(0xFF10161D),Color(0xFF19222C),Color(0xFF75889A),Color(0xFFE4EDF5),Color(0xFFF4F8FB),Color(0xFFA9B7C2)),
 XTheme.EMERALD to XColors("Emerald Combat",Color(0xFF030806),Color(0xFF0C1712),Color(0xFF11251B),Color(0xFF26835F),Color(0xFFB9E7C7),Color(0xFFF2FAF4),Color(0xFFA5B9AC))
)

private val xStudent=listOf(
 XFeature("academy","RS Academy","RS Academy"),XFeature("ai","AI Coach","AI Coach"),XFeature("train","Train Anywhere","Overal Trainen"),XFeature("session","Session Player","Sessie Speler"),XFeature("classes","Classes & Events","Lessen & Events"),XFeature("community","Community","Community"),XFeature("progress","Progress & Profile","Voortgang & Profiel"),XFeature("history","Training History","Trainingsgeschiedenis"),XFeature("challenges","Challenges","Uitdagingen"),XFeature("fightcamp","Fight Camp","Fight Camp"),XFeature("finance","Membership & Payments","Lidmaatschap & Betalen"),XFeature("media","Training Media","Trainingsmedia"),XFeature("book","Trainer Book","Boek van de Trainer"),XFeature("private","Private Lessons","Privélessen"),XFeature("support","Support","Support"),XFeature("settings","Settings & Privacy","Instellingen & Privacy")
)
private val xAdmin=listOf(
 XFeature("themes","Visual Theme Studio","Visueel Thema Studio"),XFeature("access","Access & Subscription Control","Toegang & Abonnementen"),XFeature("payments","Payment Center","Betaalcentrum"),XFeature("members","Student Manager","Ledenbeheer"),XFeature("classadmin","Class Manager","Lesbeheer"),XFeature("attendance","Attendance Manager","Aanwezigheidsbeheer"),XFeature("invoices","Invoices","Facturen"),XFeature("homeworkadmin","Homework Manager","Huiswerkbeheer"),XFeature("lessonadmin","Lesson Editor","Leseditor"),XFeature("notifyadmin","Notification Composer","Meldingen Opstellen"),XFeature("progressadmin","Progress Manager","Voortgangsbeheer"),XFeature("bookadmin","Book Manager","Boekbeheer"),XFeature("eventsadmin","Event Manager","Eventbeheer"),XFeature("scheduleadmin","Weekly Schedule","Weekplanning"),XFeature("sessionadmin","Session Builder","Sessie Bouwer"),XFeature("fightcampadmin","Fight Camp Manager","Fight Camp Beheer")
)

private class XP(context:Context){private val p=context.getSharedPreferences("rsv10",Context.MODE_PRIVATE);fun s(k:String,d:String="")=p.getString(k,d)?:d;fun ps(k:String,v:String)=p.edit().putString(k,v).apply();fun b(k:String,d:Boolean=false)=p.getBoolean(k,d);fun pb(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()}

@Composable
fun RsKickboxV10App(){
 val context=LocalContext.current
 val prefs=remember(context){XP(context)}
 var role by remember{mutableStateOf<XRole?>(null)}
 var route by remember{mutableStateOf("dashboard")}
 var lang by remember{mutableStateOf(if(prefs.s("lang","EN")=="NL")XLang.NL else XLang.EN)}
 var theme by remember{mutableStateOf(runCatching{XTheme.valueOf(prefs.s("theme",XTheme.GOLD.name))}.getOrDefault(XTheme.GOLD))}
 val c=xThemes.getValue(theme)
 MaterialTheme(colorScheme=darkColorScheme(primary=c.bright,secondary=c.accent,background=c.bg,surface=c.panel,onBackground=c.text,onSurface=c.text)){
  if(role==null){XLogin(c,lang,{lang=it;prefs.ps("lang",it.name)}){r->role=r;route=if(r==XRole.TRAINER)"admin" else "dashboard"}}
  else{XShell(c,role!!,lang,route,{lang=it;prefs.ps("lang",it.name)},{route=it},{role=null}){
    XRouter(c,role!!,lang,route,prefs,{route=it}){newTheme->theme=newTheme;prefs.ps("theme",newTheme.name)}
  }}
 }
}

@Composable
private fun XBackground(c:XColors,strong:Boolean,content:@Composable BoxScope.() -> Unit){
 val tr=rememberInfiniteTransition(label="bg")
 val m by tr.animateFloat(0f,1f,infiniteRepeatable(tween(if(strong)8000 else 14000,easing=LinearEasing),RepeatMode.Reverse),label="motion")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bg,c.panel2,c.bg))).statusBarsPadding().navigationBarsPadding()){
  Canvas(Modifier.fillMaxSize()){val a=if(strong).20f else .06f;drawCircle(c.accent.copy(alpha=a),size.minDimension*.31f,Offset(size.width*(.18f+.60f*m),size.height*.22f));drawCircle(c.bright.copy(alpha=a*.6f),size.minDimension*.23f,Offset(size.width*(.80f-.45f*m),size.height*.76f))}
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(strong).48f else .73f)))
  content()
 }
}

@Composable
private fun XLogin(c:XColors,l:XLang,onLang:(XLang)->Unit,onLogin:(XRole)->Unit){
 val nl=l==XLang.NL;var email by remember{mutableStateOf("alex@rskickbox.nl")};var pass by remember{mutableStateOf("preview123")}
 XBackground(c,true){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("♛ RS KICKBOX",color=c.bright,fontSize=31.sp,fontWeight=FontWeight.Black);XLanguage(c,l,onLang);Text(if(nl)"Premium live kickboksplatform" else "Premium live kickboxing platform",color=c.text,style=MaterialTheme.typography.headlineMedium);Text("TRAIN · LEARN · CONNECT · GROW",color=c.muted,fontSize=11.sp)
  XCard(c){Text(if(nl)"LEDEN TOEGANG" else "MEMBER ACCESS",color=c.bright,fontWeight=FontWeight.Bold);OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(pass,{pass=it},label={Text(if(nl)"Wachtwoord" else "Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());Button(onClick={onLogin(XRole.STUDENT)},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Leerling login" else "Student login")};OutlinedButton(onClick={onLogin(XRole.TRAINER)},modifier=Modifier.fillMaxWidth()){Text("Trainer / Admin")}}
  XCard(c){Text("Van Stilte Naar Strijd",color=c.bright,fontSize=23.sp,fontWeight=FontWeight.Black);Text("Kickboksen, karakter en de weg van basis naar beheersing",color=c.text)}
 }}
}

@Composable
private fun XShell(c:XColors,r:XRole,l:XLang,route:String,onLang:(XLang)->Unit,onRoute:(String)->Unit,onLogout:()->Unit,content:@Composable () -> Unit){
 val home=if(r==XRole.TRAINER)"admin" else "dashboard"
 XBackground(c,false){Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  XCard(c){Text("♛ RS KICKBOX",color=c.bright,fontSize=20.sp,fontWeight=FontWeight.Black);Text(if(r==XRole.TRAINER)"TRAINER / ADMIN" else "STUDENT",color=c.muted,fontSize=9.sp);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){XLanguage(c,l,onLang);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){if(route!=home){OutlinedButton(onClick={onRoute(home)},contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text("‹")}};OutlinedButton(onClick=onLogout,contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text(if(l==XLang.NL)"Uitloggen" else "Log out",fontSize=10.sp)}}}}
  Box(Modifier.fillMaxWidth().weight(1f)){content()}
 }}
}

@Composable
private fun XRouter(c:XColors,r:XRole,l:XLang,route:String,p:XP,onRoute:(String)->Unit,onTheme:(XTheme)->Unit){
 when(route){
  "dashboard","admin"->XDashboard(c,r,l,p,onRoute)
  "themes"->XThemeStudio(c,l,p,onTheme)
  "access"->XAccess(c,l,p)
  "payments"->XPayments(c,l,p)
  "finance"->XMemberPayments(c,l,p)
  "session"->XSession(c,l)
  "academy"->XAcademy(c,l,p)
  "classes"->XClasses(c,l,p)
  else->XGeneric(c,l,route,r,p)
 }
}

private fun xDefault(plan:XPlan,id:String)=when(plan){XPlan.BASIC->id in setOf("academy","train","classes","progress","history","finance","book","support","settings");XPlan.PRO->id !in setOf("private","fightcamp");XPlan.ELITE->true}
private fun xAllowed(p:XP,student:String,id:String):Boolean{val plan=runCatching{XPlan.valueOf(p.s("student_plan_$student",XPlan.PRO.name))}.getOrDefault(XPlan.PRO);val base=p.b("plan_${plan.name}_$id",xDefault(plan,id));return if(p.b("has_${student}_$id"))p.b("override_${student}_$id",base) else base}

@Composable
private fun XDashboard(c:XColors,r:XRole,l:XLang,p:XP,onRoute:(String)->Unit){
 val nl=l==XLang.NL;val tiles=if(r==XRole.TRAINER)xAdmin else xStudent.filter{xAllowed(p,"Alex",it.id)}
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
  XCard(c){Text(if(r==XRole.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=c.bright,fontWeight=FontWeight.Black);Text(if(r==XRole.TRAINER){if(nl)"Volledige controle over thema, toegang, abonnementen en betalingen." else "Full control over theme, access, subscriptions and payments."}else if(nl)"Jouw persoonlijke RS-omgeving." else "Your personal RS environment.",color=c.text,style=MaterialTheme.typography.titleLarge)}
  LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=18.dp)){items(tiles){f->XTileCard(c,if(nl)f.nl else f.en){onRoute(f.id)}}}
 }
}

@Composable
private fun XThemeStudio(c:XColors,l:XLang,p:XP,onTheme:(XTheme)->Unit){val nl=l==XLang.NL;XScroll(c,if(nl)"Visueel Thema Studio" else "Visual Theme Studio",if(nl)"Kies één premium look voor de hele app." else "Choose one premium look for the whole app."){xThemes.forEach{(id,x)->XCard(c){Text(x.name,color=c.bright,fontSize=19.sp,fontWeight=FontWeight.Black);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(x.bg,x.panel,x.accent,x.bright).forEach{col->Surface(modifier=Modifier.size(24.dp),shape=CircleShape,color=col,border=BorderStroke(1.dp,Color.White.copy(alpha=.25f))){} }};Button(onClick={onTheme(id);p.ps("theme",id.name)},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Activeer thema" else "Activate theme")}}};XCard(c){Text(if(nl)"Live achtergrond" else "Live background",color=c.bright,fontWeight=FontWeight.Bold);Text(if(nl)"Sterker op login/promotie, subtieler op training zodat tekst en timers duidelijk blijven." else "Stronger on login/promotion, subtler on training so text and timers remain clear.",color=c.muted)}}}

@Composable
private fun XAccess(c:XColors,l:XLang,p:XP){
 val nl=l==XLang.NL;var mode by remember{mutableStateOf("plans")};var student by remember{mutableStateOf("Alex")}
 XScroll(c,if(nl)"Toegang & Abonnementen" else "Access & Subscription Control",if(nl)"Bepaal exact wat elke leerling kan zien en gebruiken." else "Control exactly what every student can see and use."){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={mode="plans"},modifier=Modifier.weight(1f)){Text(if(nl)"Plannen" else "Plans")};OutlinedButton(onClick={mode="student"},modifier=Modifier.weight(1f)){Text(if(nl)"Per leerling" else "Per student")}}
  if(mode=="plans"){
   XPlan.entries.forEach{plan->XCard(c){Text("RS ${plan.name}",color=c.bright,fontWeight=FontWeight.Black);xStudent.forEach{f->var enabled by remember(plan,f.id){mutableStateOf(p.b("plan_${plan.name}_${f.id}",xDefault(plan,f.id)))};XToggle(c,if(nl)f.nl else f.en,enabled){enabled=it;p.pb("plan_${plan.name}_${f.id}",it)}}}}
  }else{
   XCard(c){Text(if(nl)"Selecteer leerling" else "Select student",color=c.bright,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Alex","Mila","Noah","Sara").forEach{name->FilterChip(selected=student==name,onClick={student=name},label={Text(name,fontSize=9.sp)},modifier=Modifier.weight(1f))}};var plan by remember(student){mutableStateOf(runCatching{XPlan.valueOf(p.s("student_plan_$student",XPlan.PRO.name))}.getOrDefault(XPlan.PRO))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){XPlan.entries.forEach{x->FilterChip(selected=plan==x,onClick={plan=x;p.ps("student_plan_$student",x.name)},label={Text(x.name,fontSize=9.sp)},modifier=Modifier.weight(1f))}}}
   XCard(c){Text(if(nl)"Individuele overrides" else "Individual overrides",color=c.bright,fontWeight=FontWeight.Bold);xStudent.forEach{f->var enabled by remember(student,f.id){mutableStateOf(xAllowed(p,student,f.id))};XToggle(c,if(nl)f.nl else f.en,enabled){enabled=it;p.pb("has_${student}_${f.id}",true);p.pb("override_${student}_${f.id}",it)}}}
  }
 }
}

@Composable
private fun XPayments(c:XColors,l:XLang,p:XP){val nl=l==XLang.NL;XScroll(c,if(nl)"Betaalcentrum" else "Payment Center",if(nl)"Tijdelijke en toekomstige betaalmethoden beheren." else "Manage temporary and future payment methods."){
 XCard(c){Text(if(nl)"Tijdelijke betaalmodus" else "Temporary payment mode",color=c.bright,fontWeight=FontWeight.Black);Text(if(nl)"Gebruik alleen methoden die jouw bank/provider toestaat. Voor regelmatige commerciële abonnementen kunnen registratie- en zakelijke provider-eisen gelden." else "Use only methods allowed by your bank/provider. Regular commercial memberships may trigger registration and business-provider requirements.",color=c.text)}
 XBilling(c,l,p)
 XPayCfg(c,p,"bank","Bank transfer / SEPA",if(nl)"Directe overschrijving; bankvoorwaarden gelden." else "Direct transfer; bank terms apply.",true)
 XPayCfg(c,p,"cash","Cash",if(nl)"Handmatig registreren." else "Record manually.",true)
 XPayCfg(c,p,"personal",if(nl)"Particulier betaalverzoek" else "Personal payment request",if(nl)"Alleen binnen particuliere provider-voorwaarden; niet bedoeld als commerciële checkout." else "Only within personal-provider terms; not intended as commercial checkout.",false)
 XPayCfg(c,p,"revolutme","Revolut.me",if(nl)"Persoonlijke link met providerlimieten." else "Personal link subject to provider limits.",false)
 XCard(c){Text(if(nl)"Zakelijke opties voor later" else "Business options for later",color=c.bright,fontWeight=FontWeight.Black);Text("Tikkie Business · iDEAL/Wero · Revolut Business · Cards/Wallets · PayPal",color=c.muted)}
 listOf("tikkie" to "Tikkie Business","ideal" to "iDEAL / Wero","revolut" to "Revolut Business","cards" to "Cards / Apple Pay / Google Pay","paypal" to "PayPal").forEach{x->XPayCfg(c,p,x.first,x.second,if(nl)"Zakelijke onboarding/providerkosten kunnen gelden." else "Business onboarding/provider fees may apply.",false)}
}}

@Composable private fun XBilling(c:XColors,l:XLang,p:XP){val nl=l==XLang.NL;XCard(c){Text(if(nl)"Betaal- en factuurgegevens" else "Payment & billing details",color=c.bright,fontWeight=FontWeight.Bold);listOf("name" to (if(nl)"Naam op betaling" else "Payment display name"),"iban" to "IBAN","email" to (if(nl)"Factuur e-mail" else "Billing email"),"kvk" to "KvK (later)","vat" to "VAT/BTW (later)").forEach{x->var v by remember(x.first){mutableStateOf(p.s("bill_${x.first}"))};OutlinedTextField(v,{v=it;p.ps("bill_${x.first}",it)},label={Text(x.second)},singleLine=true,modifier=Modifier.fillMaxWidth())}}}
@Composable private fun XPayCfg(c:XColors,p:XP,id:String,name:String,note:String,on:Boolean){var enabled by remember(id){mutableStateOf(p.b("pay_$id",on))};var link by remember(id){mutableStateOf(p.s("link_$id"))};XCard(c){XToggle(c,name,enabled){enabled=it;p.pb("pay_$id",it)};Text(note,color=c.muted,fontSize=11.sp);OutlinedTextField(link,{link=it;p.ps("link_$id",it)},label={Text("Payment link / reference")},singleLine=true,modifier=Modifier.fillMaxWidth())}}

@Composable private fun XMemberPayments(c:XColors,l:XLang,p:XP){val nl=l==XLang.NL;val uri=LocalUriHandler.current;XScroll(c,if(nl)"Lidmaatschap & Betalen" else "Membership & Payments",if(nl)"Alleen geactiveerde methoden zijn zichtbaar." else "Only enabled methods are visible."){XCard(c){Text("RS PRO",color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black);Text("€49 / month",color=c.text)};listOf("bank" to "Bank transfer / SEPA","cash" to "Cash","personal" to "Personal payment request","revolutme" to "Revolut.me","tikkie" to "Tikkie Business","ideal" to "iDEAL / Wero","revolut" to "Revolut Business","cards" to "Cards / Wallets","paypal" to "PayPal").forEach{x->if(p.b("pay_${x.first}",x.first=="bank"||x.first=="cash")){XCard(c){Text(x.second,color=c.bright,fontWeight=FontWeight.Bold);val link=p.s("link_${x.first}");if(x.first=="bank")Text(p.s("bill_iban",if(nl)"IBAN nog niet ingesteld" else "IBAN not configured"),color=c.text)else if(link.startsWith("http"))Button(onClick={runCatching{uri.openUri(link)}},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Open betaling" else "Open payment")}else Text(if(nl)"Nog geen link ingesteld." else "No payment link configured.",color=c.muted)}}}}}

@Composable private fun XAcademy(c:XColors,l:XLang,p:XP){val nl=l==XLang.NL;XScroll(c,"RS Academy",if(nl)"Techniekbibliotheek." else "Technique library."){listOf("Jab Fundamentals","Roundhouse Kick","Defense & Counters","Footwork Flow").forEachIndexed{i,n->var done by remember(i){mutableStateOf(p.b("lesson$i"))};XCard(c){Text(n,color=c.bright,fontWeight=FontWeight.Bold);Button(onClick={done=true;p.pb("lesson$i",true)},modifier=Modifier.fillMaxWidth(),enabled=!done){Text(if(done)"Completed ✓" else if(nl)"Voltooi les" else "Complete lesson")}}}}}
@Composable private fun XClasses(c:XColors,l:XLang,p:XP){val nl=l==XLang.NL;XScroll(c,if(nl)"Lessen & Events" else "Classes & Events",if(nl)"Boek trainingen." else "Book training."){listOf("Technique & Pads" to "Today · 19:00","Fundamentals" to "Thu · 18:30").forEachIndexed{i,x->var booked by remember(i){mutableStateOf(p.b("class$i"))};XCard(c){Text(x.first,color=c.bright,fontWeight=FontWeight.Bold);Text(x.second,color=c.muted);Button(onClick={booked=!booked;p.pb("class$i",booked)},modifier=Modifier.fillMaxWidth()){Text(if(booked){if(nl)"Annuleer" else "Cancel"}else if(nl)"Boek les" else "Book class")}}}}}
@Composable private fun XSession(c:XColors,l:XLang){val nl=l==XLang.NL;var running by remember{mutableStateOf(false)};var sec by remember{mutableIntStateOf(120)};LaunchedEffect(running,sec){if(running&&sec>0){delay(1000);sec--}else if(sec==0)running=false};XScroll(c,if(nl)"Sessie Speler" else "Session Player",if(nl)"Subtiele live achtergrond." else "Subtle live background."){XCard(c){Text("%d:%02d".format(sec/60,sec%60),color=c.bright,fontSize=50.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=c.text);Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){Text(if(running)"Pause" else "Start")};OutlinedButton(onClick={sec=120;running=false},modifier=Modifier.fillMaxWidth()){Text("Reset")}}}}

@Composable private fun XGeneric(c:XColors,l:XLang,route:String,r:XRole,p:XP){val f=(if(r==XRole.TRAINER)xAdmin else xStudent).firstOrNull{it.id==route};XScroll(c,if(l==XLang.NL)f?.nl?:route else f?.en?:route,if(l==XLang.NL)"Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout."){repeat(3){i->XCard(c){Text(listOf("Overview","Actions","Status")[i],color=c.bright,fontWeight=FontWeight.Bold);Text(if(l==XLang.NL)"Actief in de v0.10 preview." else "Active in the v0.10 preview.",color=c.muted)}};var saved by remember(route){mutableStateOf(p.b("generic$route"))};Button(onClick={saved=true;p.pb("generic$route",true)},modifier=Modifier.fillMaxWidth(),enabled=!saved){Text(if(saved)"Saved ✓" else "Save preview action")}}}

@Composable private fun XScroll(c:XColors,title:String,sub:String,content:@Composable ColumnScope.() -> Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=1.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=c.bright,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(sub,color=c.muted);content();Spacer(Modifier.height(18.dp))}}
@Composable private fun XCard(c:XColors,content:@Composable ColumnScope.() -> Unit){Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.bright.copy(alpha=.28f)),colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.96f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
@Composable private fun XTileCard(c:XColors,title:String,onClick:()->Unit){Card(modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.accent.copy(alpha=.38f)),colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.94f))){Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("♛",color=c.bright,fontSize=20.sp);Text(title,color=c.bright,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=2)}}}
@Composable private fun XToggle(c:XColors,label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=c.text,modifier=Modifier.weight(1f));Switch(checked=value,onCheckedChange=onChange)}}
@Composable private fun XLanguage(c:XColors,l:XLang,onChange:(XLang)->Unit){Surface(shape=RoundedCornerShape(15.dp),color=c.panel2,border=BorderStroke(1.dp,c.accent.copy(alpha=.38f))){Row(Modifier.padding(2.dp)){Text("EN",Modifier.clickable{onChange(XLang.EN)}.padding(horizontal=8.dp,vertical=5.dp),color=if(l==XLang.EN)c.bright else c.muted,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("NL",Modifier.clickable{onChange(XLang.NL)}.padding(horizontal=8.dp,vertical=5.dp),color=if(l==XLang.NL)c.bright else c.muted,fontSize=10.sp,fontWeight=FontWeight.Bold)}}}
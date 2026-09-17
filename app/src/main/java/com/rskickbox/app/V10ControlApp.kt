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
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class CRole { STUDENT, TRAINER }
private enum class CLang { EN, NL }
private enum class CThemeId { ELITE_GOLD, ROYAL_CRIMSON, MIDNIGHT_PLATINUM, EMERALD_COMBAT }
private enum class CPlan { BASIC, PRO, ELITE }

private data class CTheme(val id:CThemeId,val name:String,val bg:Color,val panel:Color,val panel2:Color,val accent:Color,val bright:Color,val text:Color,val muted:Color)
private data class CTile(val id:String,val en:String,val nl:String)

private val cThemes=listOf(
 CTheme(CThemeId.ELITE_GOLD,"Elite Gold",Color(0xFF050505),Color(0xFF15120D),Color(0xFF211A10),Color(0xFFC08A24),Color(0xFFF0CF79),Color(0xFFF6F0E4),Color(0xFFB8AD98)),
 CTheme(CThemeId.ROYAL_CRIMSON,"Royal Crimson",Color(0xFF080405),Color(0xFF1C0E12),Color(0xFF2B151A),Color(0xFFA12B3D),Color(0xFFF0A58E),Color(0xFFFFF3EF),Color(0xFFC6A2A0)),
 CTheme(CThemeId.MIDNIGHT_PLATINUM,"Midnight Platinum",Color(0xFF04070B),Color(0xFF10161D),Color(0xFF19222C),Color(0xFF75889A),Color(0xFFE4EDF5),Color(0xFFF4F8FB),Color(0xFFA9B7C2)),
 CTheme(CThemeId.EMERALD_COMBAT,"Emerald Combat",Color(0xFF030806),Color(0xFF0C1712),Color(0xFF11251B),Color(0xFF26835F),Color(0xFFB9E7C7),Color(0xFFF2FAF4),Color(0xFFA5B9AC))
)

private val cStudent=listOf(
 CTile("academy","RS Academy","RS Academy"),CTile("ai","AI Coach","AI Coach"),CTile("train","Train Anywhere","Overal Trainen"),CTile("session","Session Player","Sessie Speler"),CTile("compare","Technique Compare","Techniek Vergelijken"),CTile("classes","Classes & Events","Lessen & Events"),CTile("community","Community","Community"),CTile("progress","Progress & Profile","Voortgang & Profiel"),CTile("homework","Homework","Huiswerk"),CTile("history","Training History","Trainingsgeschiedenis"),CTile("challenges","Challenges","Uitdagingen"),CTile("fightcamp","Fight Camp","Fight Camp"),CTile("finance","Membership & Payments","Lidmaatschap & Betalen"),CTile("media","Training Media","Trainingsmedia"),CTile("book","Trainer Book","Boek van de Trainer"),CTile("private","Private Lessons","Privélessen"),CTile("support","Support","Support"),CTile("settings","Settings & Privacy","Instellingen & Privacy")
)
private val cAdmin=listOf(
 CTile("themes","Visual Theme Studio","Visueel Thema Studio"),CTile("access","Access & Subscription Control","Toegang & Abonnementen"),CTile("payments","Payment Center","Betaalcentrum"),CTile("members","Student Manager","Ledenbeheer"),CTile("classadmin","Class Manager","Lesbeheer"),CTile("attendance","Attendance Manager","Aanwezigheidsbeheer"),CTile("invoices","Invoices","Facturen"),CTile("homeworkadmin","Homework Manager","Huiswerkbeheer"),CTile("lessonadmin","Lesson Editor","Leseditor"),CTile("notifyadmin","Notification Composer","Meldingen Opstellen"),CTile("progressadmin","Progress Manager","Voortgangsbeheer"),CTile("bookadmin","Book Manager","Boekbeheer"),CTile("eventsadmin","Event Manager","Eventbeheer"),CTile("scheduleadmin","Weekly Schedule","Weekplanning"),CTile("sessionadmin","Session Builder","Sessie Bouwer"),CTile("fightcampadmin","Fight Camp Manager","Fight Camp Beheer")
)

private class CPrefs(context:Context){
 private val p=context.getSharedPreferences("rs_v10",Context.MODE_PRIVATE)
 fun s(k:String,d:String="")=p.getString(k,d)?:d
 fun ps(k:String,v:String)=p.edit().putString(k,v).apply()
 fun b(k:String,d:Boolean=false)=p.getBoolean(k,d)
 fun pb(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
}

@Composable fun RsKickboxV10App(){
 val ctx=LocalContext.current; val prefs=remember{CPrefs(ctx)}
 var role by remember{mutableStateOf<CRole?>(null)}; var route by remember{mutableStateOf("dashboard")}
 var lang by remember{mutableStateOf(if(prefs.s("lang","EN")=="NL")CLang.NL else CLang.EN)}
 var themeId by remember{mutableStateOf(runCatching{CThemeId.valueOf(prefs.s("theme",CThemeId.ELITE_GOLD.name))}.getOrDefault(CThemeId.ELITE_GOLD))}
 val theme=cThemes.first{it.id==themeId}
 MaterialTheme(colorScheme=darkColorScheme(primary=theme.bright,secondary=theme.accent,background=theme.bg,surface=theme.panel,onBackground=theme.text,onSurface=theme.text)){
  if(role==null) CLogin(theme,lang,{lang=it;prefs.ps("lang",it.name)}){role=it;route=if(it==CRole.TRAINER)"admin" else "dashboard"}
  else CShell(theme,role!!,lang,route,{lang=it;prefs.ps("lang",it.name)},{route=it},{role=null}){
   CRouter(theme,role!!,lang,route,prefs,{route=it}){themeId=it;prefs.ps("theme",it.name)}
  }
 }
}

@Composable private fun CLive(theme:CTheme,strong:Boolean,content:@Composable BoxScope.()->Unit){
 val tr=rememberInfiniteTransition(label="live"); val m by tr.animateFloat(0f,1f,infiniteRepeatable(tween(if(strong)8000 else 14000,easing=LinearEasing),RepeatMode.Reverse),label="m")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.bg,theme.panel2,theme.bg))).statusBarsPadding().navigationBarsPadding()){
  Canvas(Modifier.fillMaxSize()){val a=if(strong).22f else .07f;drawCircle(theme.accent.copy(alpha=a),size.minDimension*.30f,Offset(size.width*(.18f+.62f*m),size.height*.24f));drawCircle(theme.bright.copy(alpha=a*.6f),size.minDimension*.22f,Offset(size.width*(.8f-.48f*m),size.height*.76f))}
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(strong).47f else .72f)));content()
 }
}

@Composable private fun CLogin(t:CTheme,l:CLang,onLang:(CLang)->Unit,onLogin:(CRole)->Unit){
 val nl=l==CLang.NL;var email by remember{mutableStateOf("alex@rskickbox.nl")};var pass by remember{mutableStateOf("preview123")}
 CLive(t,true){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("♛ RS KICKBOX",color=t.bright,fontSize=31.sp,fontWeight=FontWeight.Black);CLanguage(t,l,onLang);Text(if(nl)"Premium live kickboksplatform" else "Premium live kickboxing platform",color=t.text,style=MaterialTheme.typography.headlineMedium);Text("TRAIN · LEARN · CONNECT · GROW",color=t.muted,fontSize=11.sp)
  CCard(t){Text(if(nl)"LEDEN TOEGANG" else "MEMBER ACCESS",color=t.bright,fontWeight=FontWeight.Bold);OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(pass,{pass=it},label={Text(if(nl)"Wachtwoord" else "Password")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth());Button({onLogin(CRole.STUDENT)},Modifier.fillMaxWidth()){Text(if(nl)"Leerling login" else "Student login")};OutlinedButton({onLogin(CRole.TRAINER)},Modifier.fillMaxWidth()){Text("Trainer / Admin")}}
  CCard(t){Text("Van Stilte Naar Strijd",color=t.bright,fontSize=23.sp,fontWeight=FontWeight.Black);Text("Kickboksen, karakter en de weg van basis naar beheersing",color=t.text)}
 }}
}

@Composable private fun CShell(t:CTheme,r:CRole,l:CLang,route:String,onLang:(CLang)->Unit,onRoute:(String)->Unit,onLogout:()->Unit,content:@Composable()->Unit){
 val home=if(r==CRole.TRAINER)"admin" else "dashboard"
 CLive(t,false){Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  CCard(t){Text("♛ RS KICKBOX",color=t.bright,fontSize=20.sp,fontWeight=FontWeight.Black);Text(if(r==CRole.TRAINER)"TRAINER / ADMIN" else "STUDENT",color=t.muted,fontSize=9.sp);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){CLanguage(t,l,onLang);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){if(route!=home)OutlinedButton({onRoute(home)},contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text("‹")};OutlinedButton(onLogout,contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text(if(l==CLang.NL)"Uitloggen" else "Log out",fontSize=10.sp)}}}}
  Box(Modifier.fillMaxWidth().weight(1f)){content()}
 }}
}

@Composable private fun CRouter(t:CTheme,r:CRole,l:CLang,route:String,p:CPrefs,onRoute:(String)->Unit,onTheme:(CThemeId)->Unit){when(route){"dashboard","admin"->CDashboard(t,r,l,p,onRoute);"themes"->CThemes(t,l,p,onTheme);"access"->CAccess(t,l,p);"payments"->CPayments(t,l,p);"finance"->CMemberPay(t,l,p);"academy"->CAcademy(t,l,p);"classes"->CClasses(t,l,p);"session"->CSession(t,l);else->CGeneric(t,l,route,r,p)}}

private fun planDefault(plan:CPlan,id:String)=when(plan){CPlan.BASIC->id in setOf("academy","train","classes","progress","history","finance","book","support","settings");CPlan.PRO->id !in setOf("private","fightcamp");CPlan.ELITE->true}
private fun allowed(p:CPrefs,student:String,id:String):Boolean{val plan=runCatching{CPlan.valueOf(p.s("student_plan_$student",CPlan.PRO.name))}.getOrDefault(CPlan.PRO);val base=p.b("plan_${plan.name}_$id",planDefault(plan,id));return if(p.b("has_${student}_$id"))p.b("override_${student}_$id",base) else base}

@Composable private fun CDashboard(t:CTheme,r:CRole,l:CLang,p:CPrefs,onRoute:(String)->Unit){val nl=l==CLang.NL;val list=if(r==CRole.TRAINER)cAdmin else cStudent.filter{allowed(p,"Alex",it.id)};Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){CCard(t){Text(if(r==CRole.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=t.bright,fontWeight=FontWeight.Black);Text(if(r==CRole.TRAINER)(if(nl)"Volledige controle over thema, toegang, abonnementen en betalingen." else "Full control over theme, access, subscriptions and payments.") else if(nl)"Jouw persoonlijke RS-omgeving." else "Your personal RS environment.",color=t.text,style=MaterialTheme.typography.titleLarge)};LazyVerticalGrid(GridCells.Adaptive(128.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=18.dp)){items(list){f->CTileCard(t,if(nl)f.nl else f.en){onRoute(f.id)}}}}}

@Composable private fun CThemes(t:CTheme,l:CLang,p:CPrefs,onTheme:(CThemeId)->Unit){val nl=l==CLang.NL;CScroll(t,if(nl)"Visueel Thema Studio" else "Visual Theme Studio",if(nl)"Kies één premium look voor de hele app." else "Choose one premium look for the whole app."){cThemes.forEach{c->CCard(t){Text(c.name,color=t.bright,fontSize=19.sp,fontWeight=FontWeight.Black);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(c.bg,c.panel,c.accent,c.bright).forEach{col->Surface(Modifier.size(24.dp),CircleShape,col,border=BorderStroke(1.dp,Color.White.copy(alpha=.25f))){} }};Button({onTheme(c.id);p.ps("theme",c.id.name)},Modifier.fillMaxWidth()){Text(if(nl)"Activeer thema" else "Activate theme")}}};CCard(t){Text(if(nl)"Live achtergrond" else "Live background",color=t.bright,fontWeight=FontWeight.Bold);Text(if(nl)"Sterker op login/promotie, subtieler op training zodat inhoud leesbaar blijft." else "Stronger on login/promotion, subtler on training so content stays readable.",color=t.muted)}}}

@Composable private fun CAccess(t:CTheme,l:CLang,p:CPrefs){val nl=l==CLang.NL;var mode by remember{mutableStateOf("plans")};var student by remember{mutableStateOf("Alex")};CScroll(t,if(nl)"Toegang & Abonnementen" else "Access & Subscription Control",if(nl)"Bepaal exact wat elke leerling kan zien en gebruiken." else "Control exactly what every student can see and use."){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button({mode="plans"},Modifier.weight(1f)){Text(if(nl)"Plannen" else "Plans")};OutlinedButton({mode="student"},Modifier.weight(1f)){Text(if(nl)"Per leerling" else "Per student")}}
 if(mode=="plans")CPlan.entries.forEach{plan->CCard(t){Text("RS ${plan.name}",color=t.bright,fontWeight=FontWeight.Black);cStudent.forEach{f->var en by remember(plan,f.id){mutableStateOf(p.b("plan_${plan.name}_${f.id}",planDefault(plan,f.id)))};CToggle(t,if(nl)f.nl else f.en,en){en=it;p.pb("plan_${plan.name}_${f.id}",it)}}}}
 else{CCard(t){Text(if(nl)"Selecteer leerling" else "Select student",color=t.bright,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Alex","Mila").forEach{name->FilterChip(student==name,{student=name},{Text(name)},Modifier.weight(1f))}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Noah","Sara").forEach{name->FilterChip(student==name,{student=name},{Text(name)},Modifier.weight(1f))}};var plan by remember(student){mutableStateOf(runCatching{CPlan.valueOf(p.s("student_plan_$student",CPlan.PRO.name))}.getOrDefault(CPlan.PRO))};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){CPlan.entries.forEach{x->FilterChip(plan==x,{plan=x;p.ps("student_plan_$student",x.name)},{Text(x.name,fontSize=10.sp)},Modifier.weight(1f))}}};CCard(t){Text(if(nl)"Individuele overrides" else "Individual overrides",color=t.bright,fontWeight=FontWeight.Bold);cStudent.forEach{f->var en by remember(student,f.id){mutableStateOf(allowed(p,student,f.id))};CToggle(t,if(nl)f.nl else f.en,en){en=it;p.pb("has_${student}_${f.id}",true);p.pb("override_${student}_${f.id}",it)}};TextButton({cStudent.forEach{p.pb("has_${student}_${it.id}",false)}}){Text(if(nl)"Herstel abonnementstandaard" else "Reset plan defaults")}}}}
}

@Composable private fun CPayments(t:CTheme,l:CLang,p:CPrefs){val nl=l==CLang.NL;CScroll(t,if(nl)"Betaalcentrum" else "Payment Center",if(nl)"Tijdelijke en toekomstige betaalmethoden beheren." else "Manage temporary and future payment methods."){CCard(t){Text(if(nl)"Tijdelijke betaalmodus" else "Temporary payment mode",color=t.bright,fontWeight=FontWeight.Black);Text(if(nl)"Gebruik alleen methoden die jouw bank of provider toestaat. Voor regelmatige commerciële abonnementen kunnen registratie- en zakelijke provider-eisen gelden." else "Use only methods allowed by your bank or provider. Regular commercial memberships may trigger registration and business-provider requirements.",color=t.text)};CBilling(t,l,p);CPayCfg(t,p,"bank","Bank transfer / SEPA",if(nl)"Directe overschrijving; normale bankvoorwaarden gelden." else "Direct transfer; normal bank terms apply.",true);CPayCfg(t,p,"cash","Cash",if(nl)"Handmatig registreren." else "Record manually.",true);CPayCfg(t,p,"personal","Personal payment request",if(nl)"Alleen binnen particuliere provider-voorwaarden; niet bedoeld als commerciële checkout." else "Only within personal-provider terms; not intended as commercial checkout.",false);CPayCfg(t,p,"revolutme","Revolut.me",if(nl)"Persoonlijke link met providerlimieten." else "Personal link subject to provider limits.",false);CCard(t){Text(if(nl)"Zakelijke opties voor later" else "Business options for later",color=t.bright,fontWeight=FontWeight.Black);Text("Tikkie Business · iDEAL/Wero · Revolut Business · Cards/Wallets · PayPal",color=t.muted)};listOf("tikkie" to "Tikkie Business","ideal" to "iDEAL / Wero","revolut" to "Revolut Business","cards" to "Cards / Apple Pay / Google Pay","paypal" to "PayPal").forEach{x->CPayCfg(t,p,x.first,x.second,if(nl)"Zakelijke onboarding/providerkosten kunnen gelden." else "Business onboarding/provider fees may apply.",false)}}}

@Composable private fun CBilling(t:CTheme,l:CLang,p:CPrefs){val nl=l==CLang.NL;CCard(t){Text(if(nl)"Betaal- en factuurgegevens" else "Payment & billing details",color=t.bright,fontWeight=FontWeight.Bold);listOf("name" to (if(nl)"Naam op betaling" else "Payment display name"),"iban" to "IBAN","email" to (if(nl)"Factuur e-mail" else "Billing email"),"kvk" to "KvK (later)","vat" to "VAT/BTW (later)").forEach{x->var v by remember(x.first){mutableStateOf(p.s("bill_${x.first}"))};OutlinedTextField(v,{v=it;p.ps("bill_${x.first}",it)},label={Text(x.second)},singleLine=true,modifier=Modifier.fillMaxWidth())}}}
@Composable private fun CPayCfg(t:CTheme,p:CPrefs,id:String,name:String,note:String,on:Boolean){var en by remember(id){mutableStateOf(p.b("pay_$id",on))};var link by remember(id){mutableStateOf(p.s("link_$id"))};CCard(t){CToggle(t,name,en){en=it;p.pb("pay_$id",it)};Text(note,color=t.muted,fontSize=11.sp);OutlinedTextField(link,{link=it;p.ps("link_$id",it)},label={Text("Payment link / reference")},singleLine=true,modifier=Modifier.fillMaxWidth())}}

@Composable private fun CMemberPay(t:CTheme,l:CLang,p:CPrefs){val nl=l==CLang.NL;val uri=LocalUriHandler.current;CScroll(t,if(nl)"Lidmaatschap & Betalen" else "Membership & Payments",if(nl)"Alleen geactiveerde methoden zijn zichtbaar." else "Only enabled methods are visible."){CCard(t){Text("RS PRO",color=t.bright,fontSize=28.sp,fontWeight=FontWeight.Black);Text("€49 / month",color=t.text)};listOf("bank" to "Bank transfer / SEPA","cash" to "Cash","personal" to "Personal payment request","revolutme" to "Revolut.me","tikkie" to "Tikkie Business","ideal" to "iDEAL / Wero","revolut" to "Revolut Business","cards" to "Cards / Wallets","paypal" to "PayPal").forEach{x->if(p.b("pay_${x.first}",x.first=="bank"||x.first=="cash"))CCard(t){Text(x.second,color=t.bright,fontWeight=FontWeight.Bold);val link=p.s("link_${x.first}");if(x.first=="bank")Text(p.s("bill_iban",if(nl)"IBAN nog niet ingesteld" else "IBAN not configured"),color=t.text) else if(link.startsWith("http"))Button({runCatching{uri.openUri(link)}},Modifier.fillMaxWidth()){Text(if(nl)"Open betaling" else "Open payment")} else Text(if(nl)"Nog geen link ingesteld." else "No payment link configured.",color=t.muted)}}}}

@Composable private fun CAcademy(t:CTheme,l:CLang,p:CPrefs){val nl=l==CLang.NL;CScroll(t,"RS Academy",if(nl)"Techniekbibliotheek." else "Technique library."){listOf("Jab Fundamentals","Roundhouse Kick","Defense & Counters","Footwork Flow").forEachIndexed{i,n->var done by remember(i){mutableStateOf(p.b("lesson$i"))};CCard(t){Text(n,color=t.bright,fontWeight=FontWeight.Bold);Button({done=true;p.pb("lesson$i",true)},Modifier.fillMaxWidth(),enabled=!done){Text(if(done)"Completed ✓" else if(nl)"Voltooi les" else "Complete lesson")}}}}}
@Composable private fun CClasses(t:CTheme,l:CLang,p:CPrefs){val nl=l==CLang.NL;CScroll(t,if(nl)"Lessen & Events" else "Classes & Events",if(nl)"Boek trainingen." else "Book training."){listOf("Technique & Pads" to "Today · 19:00","Fundamentals" to "Thu · 18:30").forEachIndexed{i,x->var b by remember(i){mutableStateOf(p.b("class$i"))};CCard(t){Text(x.first,color=t.bright,fontWeight=FontWeight.Bold);Text(x.second,color=t.muted);Button({b=!b;p.pb("class$i",b)},Modifier.fillMaxWidth()){Text(if(b)(if(nl)"Annuleer" else "Cancel") else if(nl)"Boek les" else "Book class")}}}}}
@Composable private fun CSession(t:CTheme,l:CLang){val nl=l==CLang.NL;var run by remember{mutableStateOf(false)};var sec by remember{mutableIntStateOf(120)};LaunchedEffect(run,sec){if(run&&sec>0){delay(1000);sec--}else if(sec==0)run=false};CScroll(t,if(nl)"Sessie Speler" else "Session Player",if(nl)"Subtiele live achtergrond." else "Subtle live background."){CCard(t){Text("%d:%02d".format(sec/60,sec%60),color=t.bright,fontSize=50.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=t.text);Button({run=!run},Modifier.fillMaxWidth()){Text(if(run)"Pause" else "Start")};OutlinedButton({sec=120;run=false},Modifier.fillMaxWidth()){Text("Reset")}}}}

@Composable private fun CGeneric(t:CTheme,l:CLang,route:String,r:CRole,p:CPrefs){val f=(if(r==CRole.TRAINER)cAdmin else cStudent).firstOrNull{it.id==route};CScroll(t,if(l==CLang.NL)f?.nl?:route else f?.en?:route,if(l==CLang.NL)"Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout."){repeat(3){i->CCard(t){Text(listOf("Overview","Actions","Status")[i],color=t.bright,fontWeight=FontWeight.Bold);Text(if(l==CLang.NL)"Actief in de v0.10 preview." else "Active in the v0.10 preview.",color=t.muted)}};var s by remember(route){mutableStateOf(p.b("generic$route"))};Button({s=true;p.pb("generic$route",true)},Modifier.fillMaxWidth(),enabled=!s){Text(if(s)"Saved ✓" else "Save preview action")}}}

@Composable private fun CScroll(t:CTheme,title:String,sub:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=1.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=t.bright,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(sub,color=t.muted);content();Spacer(Modifier.height(18.dp))}}
@Composable private fun CCard(t:CTheme,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,t.bright.copy(alpha=.28f)),colors=CardDefaults.cardColors(containerColor=t.panel.copy(alpha=.96f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
@Composable private fun CTileCard(t:CTheme,title:String,onClick:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,t.accent.copy(alpha=.38f)),colors=CardDefaults.cardColors(containerColor=t.panel.copy(alpha=.94f))){Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("♛",color=t.bright,fontSize=20.sp);Text(title,color=t.bright,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}}}
@Composable private fun CToggle(t:CTheme,label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=t.text,modifier=Modifier.weight(1f));Switch(value,onChange)}}
@Composable private fun CLanguage(t:CTheme,l:CLang,onChange:(CLang)->Unit){Surface(shape=RoundedCornerShape(15.dp),color=t.panel2,border=BorderStroke(1.dp,t.accent.copy(alpha=.38f))){Row(Modifier.padding(2.dp)){Text("EN",Modifier.clickable{onChange(CLang.EN)}.padding(8.dp,5.dp),color=if(l==CLang.EN)t.bright else t.muted,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("NL",Modifier.clickable{onChange(CLang.NL)}.padding(8.dp,5.dp),color=if(l==CLang.NL)t.bright else t.muted,fontSize=10.sp,fontWeight=FontWeight.Bold)}}}

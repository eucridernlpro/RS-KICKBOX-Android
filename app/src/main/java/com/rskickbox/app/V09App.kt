package com.rskickbox.app

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val G = Color(0xFFC08A24)
private val GL = Color(0xFFF0CF79)
private val C = Color(0xFFF6F0E4)
private val M = Color(0xFFB8AD98)
private val P = Color(0xF511100E)
private val P2 = Color(0xFF211A10)
private enum class R { STUDENT, TRAINER }
private enum class L { EN, NL }
private data class F(val id:String,val en:String,val nl:String,val admin:Boolean=false,val icon:String="♛")

private val fs = listOf(
 F("academy","RS Academy","RS Academy"),F("ai","AI Coach","AI Coach"),F("train","Train Anywhere","Overal Trainen"),F("session","Session Player","Sessie Speler"),F("compare","Technique Compare","Techniek Vergelijken"),F("classes","Classes & Events","Lessen & Events"),F("community","Community","Community"),F("progress","Progress & Profile","Voortgang & Profiel"),F("homework","Homework","Huiswerk"),F("notifications","Notifications","Meldingen"),F("saved","Saved & Favorites","Opgeslagen & Favorieten"),F("history","Training History","Trainingsgeschiedenis"),F("challenges","Challenges","Uitdagingen"),F("fightcamp","Fight Camp","Fight Camp"),F("finance","Membership & Payments","Lidmaatschap & Betalen"),F("profile","My Profile","Mijn Profiel"),F("media","Training Media","Trainingsmedia"),F("settings","Settings & Privacy","Instellingen & Privacy"),F("book","Trainer Book","Boek van de Trainer"),F("vault","Knowledge Vault","Kennisbank"),F("support","Support & Documents","Support & Documenten"),F("private","Private Lessons","Privélessen"),
 F("payments","Payment Center","Betaalcentrum",true,"€"),F("invoices","Invoices & Payments","Facturen & Betalingen",true),F("members","Student Manager","Ledenbeheer",true),F("attendance","Attendance Manager","Aanwezigheidsbeheer",true),F("homeworkadmin","Homework Manager","Huiswerkbeheer",true),F("musicadmin","Music Manager","Muziekbeheer",true),F("lessonadmin","Lesson Editor","Leseditor",true),F("classadmin","Class Manager","Lesbeheer",true),F("notifyadmin","Notification Composer","Meldingen Opstellen",true),F("plansadmin","Membership Plans","Lidmaatschapsplannen",true),F("progressadmin","Progress Manager","Voortgangsbeheer",true),F("assessmentsadmin","Coach Assessments","Coachbeoordelingen",true),F("eventsadmin","Event Manager","Eventbeheer",true),F("scheduleadmin","Weekly Schedule","Weekplanning",true),F("attendanceflow","QR Attendance","QR Aanwezigheid",true),F("bookadmin","Book Manager","Boekbeheer",true),F("landingadmin","Landing Page Manager","Landingpage Beheer",true),F("promo","Visual & Promotion Manager","Visuals & Promotiebeheer",true,"✦"),F("sessionadmin","Session Builder","Sessie Bouwer",true),F("challengesadmin","Challenge Manager","Uitdagingenbeheer",true),F("fightcampadmin","Fight Camp Manager","Fight Camp Beheer",true)
)

private class S(ctx:Context){
 private val p=ctx.getSharedPreferences("rs_v09",Context.MODE_PRIVATE)
 fun b(k:String,d:Boolean=false)=p.getBoolean(k,d); fun pb(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
 fun i(k:String,d:Int=0)=p.getInt(k,d); fun pi(k:String,v:Int)=p.edit().putInt(k,v).apply()
 fun s(k:String,d:String="")=p.getString(k,d)?:d; fun ps(k:String,v:String)=p.edit().putString(k,v).apply()
}

@Composable fun RsKickboxV09App(){
 val store=remember{S(LocalContext.current)}; var role by remember{mutableStateOf<R?>(null)}; var route by remember{mutableStateOf("home")}; var lang by remember{mutableStateOf(if(store.s("lang","EN")=="NL")L.NL else L.EN)}
 MaterialTheme(colorScheme=darkColorScheme(primary=GL,secondary=G,background=Color.Black,surface=P,onBackground=C,onSurface=C)){
  if(role==null) Login(lang,{lang=it;store.ps("lang",it.name)}){role=it;route=if(it==R.TRAINER)"admin" else "home"}
  else Shell(role!!,route,lang,store,{route=it},{lang=it;store.ps("lang",it.name)}){role=null}
 }
}

@Composable private fun LiveBg(route:String,content:@Composable BoxScope.()->Unit){
 val t=rememberInfiniteTransition(label="live"); val x by t.animateFloat(-.2f,1.2f,infiniteRepeatable(tween(14000),RepeatMode.Reverse),label="x")
 val training=route in setOf("academy","ai","train","session","compare","progress","fightcamp","challenges")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF050505),Color(0xFF0D0904),Color(0xFF050505)))).windowInsetsPadding(WindowInsets.safeDrawing)){
  Canvas(Modifier.fillMaxSize()){
   drawCircle(GL.copy(alpha=if(training).05f else .11f),size.minDimension*.43f,Offset(size.width*x,size.height*.2f)); drawCircle(G.copy(alpha=.07f),size.minDimension*.32f,Offset(size.width*(1-x),size.height*.76f))
   repeat(3){n->val y=size.height*(.56f+n*.055f);drawLine(GL.copy(alpha=if(training).08f else .12f),Offset(0f,y),Offset(size.width,y+18f),2f)}
  }
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(training).42f else .28f)))
  content()
 }
}

@Composable private fun Login(lang:L,onLang:(L)->Unit,onLogin:(R)->Unit){val nl=lang==L.NL;var email by remember{mutableStateOf("alex@rskickbox.nl")};var pw by remember{mutableStateOf("preview123")}
 LiveBg("login"){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("♛ RS KICKBOX",color=GL,fontSize=28.sp,fontWeight=FontWeight.Black);Text("TRAIN · LEARN · CONNECT · GROW",color=M,fontSize=10.sp)};Lang(lang,onLang)}
  Text(if(nl)"Premium kickboksplatform" else "Premium kickboxing platform",color=C,style=MaterialTheme.typography.headlineMedium)
  CardX{Text(if(nl)"LEDEN TOEGANG" else "MEMBER ACCESS",color=GL,fontWeight=FontWeight.Bold);OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(pw,{pw=it},label={Text(if(nl)"Wachtwoord" else "Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());Button({onLogin(R.STUDENT)},Modifier.fillMaxWidth()){Text(if(nl)"Leerling login" else "Student login")};OutlinedButton({onLogin(R.TRAINER)},Modifier.fillMaxWidth()){Text("Trainer / Admin")}}
  CardX{Text("Van Stilte Naar Strijd",color=GL,fontSize=22.sp,fontWeight=FontWeight.Black);Text("Kickboksen, karakter en de weg van basis naar beheersing",color=C);Text(if(nl)"Live RS-achtergrond met donkere leesbaarheidslaag." else "Live RS background with a dark readability layer.",color=M)}
 }}
}

@Composable private fun Shell(role:R,route:String,lang:L,store:S,onRoute:(String)->Unit,onLang:(L)->Unit,onLogout:()->Unit){val home=if(role==R.TRAINER)"admin" else "home";val cur=fs.firstOrNull{it.id==route}
 LiveBg(route){Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,GL.copy(alpha=.4f)),colors=CardDefaults.cardColors(containerColor=P)){
   Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("♛ RS KICKBOX",color=GL,fontSize=20.sp,fontWeight=FontWeight.Black);Text(if(role==R.TRAINER)"TRAINER / ADMIN" else "STUDENT",color=M,fontSize=9.sp);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Lang(lang,onLang);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){if(route!=home)OutlinedButton({onRoute(home)},contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text("‹")};OutlinedButton(onLogout,contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text(if(lang==L.NL)"Uitloggen" else "Log out",fontSize=10.sp)}}};if(cur!=null)Text(if(lang==L.NL)cur.nl else cur.en,color=C,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
  }
  Box(Modifier.weight(1f).fillMaxWidth()){Router(role,route,lang,store,onRoute)}
 }}
}

@Composable private fun Router(role:R,route:String,lang:L,store:S,onRoute:(String)->Unit){when(route){"home","admin"->Dash(role,lang,onRoute);"payments","invoices"->PaymentCenter(lang,store);"finance"->MemberPay(lang,store);"session"->Session(lang);"classes"->Classes(lang,store);"fightcamp"->Fight(lang,store);"book","bookadmin"->Book(lang);"promo","landingadmin"->Promo(lang,store);else->Module(route,lang,store,onRoute)}}

@Composable private fun Dash(role:R,lang:L,onRoute:(String)->Unit){val nl=lang==L.NL;val list=fs.filter{if(role==R.TRAINER)it.admin else !it.admin}
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){CardX{Text(if(role==R.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=GL,fontWeight=FontWeight.Bold);Text(if(role==R.TRAINER)(if(nl)"Beheer leden, trainingen, betalingen en promotie." else "Manage members, training, payments and promotion.") else (if(nl)"Train, leer, boek en betaal binnen RS KICKBOX." else "Train, learn, book and pay inside RS KICKBOX."),color=C)};LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=18.dp)){items(list){f->Card(modifier=Modifier.fillMaxWidth().clickable{onRoute(f.id)},shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,G.copy(alpha=.35f)),colors=CardDefaults.cardColors(containerColor=P)){Column(Modifier.padding(13.dp)){Text(f.icon,color=GL,fontSize=19.sp);Text(if(nl)f.nl else f.en,color=GL,fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}}}}}
}

@Composable private fun PaymentCenter(lang:L,store:S){val nl=lang==L.NL;var saved by remember{mutableStateOf(false)};var company by remember{mutableStateOf(store.s("company","RS KICKBOX"))};var iban by remember{mutableStateOf(store.s("iban"))};var bic by remember{mutableStateOf(store.s("bic"))};var kvk by remember{mutableStateOf(store.s("kvk"))};var vat by remember{mutableStateOf(store.s("vat"))};var mail by remember{mutableStateOf(store.s("billing"))};var tikkie by remember{mutableStateOf(store.s("tikkie"))};var ideal by remember{mutableStateOf(store.s("ideal"))};var revolut by remember{mutableStateOf(store.s("revolut"))};var card by remember{mutableStateOf(store.s("card"))};var paypal by remember{mutableStateOf(store.s("paypal"))};var custom by remember{mutableStateOf(store.s("custom"))};var bankOn by remember{mutableStateOf(store.b("bankOn",true))};var cashOn by remember{mutableStateOf(store.b("cashOn",true))};var tOn by remember{mutableStateOf(store.b("tOn",true))};var iOn by remember{mutableStateOf(store.b("iOn",true))};var rOn by remember{mutableStateOf(store.b("rOn",true))};var cardOn by remember{mutableStateOf(store.b("cardOn"))};var ppOn by remember{mutableStateOf(store.b("ppOn"))};var customOn by remember{mutableStateOf(store.b("customOn"))}
 Screen(if(nl)"Betaalcentrum" else "Payment Center",if(nl)"Beheer alle betaalinformatie vanuit Trainer/Admin." else "Manage all payment information from Trainer/Admin."){
  CardX{Text(if(nl)"BEDRIJFS- EN FACTUURGEGEVENS" else "BUSINESS & INVOICE DETAILS",color=GL,fontWeight=FontWeight.Bold);Field(company,{company=it},if(nl)"Bedrijfsnaam" else "Business name");Field(iban,{iban=it},"IBAN");Field(bic,{bic=it},"BIC");Field(kvk,{kvk=it},"KvK");Field(vat,{vat=it},if(nl)"BTW-nummer" else "VAT number");Field(mail,{mail=it},if(nl)"Factuur e-mail" else "Billing email")}
  CardX{Toggle(if(nl)"Bankoverschrijving" else "Bank transfer",bankOn){bankOn=it};Toggle(if(nl)"Contant" else "Cash",cashOn){cashOn=it};Text(if(nl)"Handmatig: geen app-processorfee; bankkosten kunnen wel gelden." else "Manual: no app processor fee; bank fees can still apply.",color=M,fontSize=11.sp)}
  Provider("Tikkie",tOn,{tOn=it},tikkie,{tikkie=it},if(nl)"Tikkie Zakelijk is niet onbeperkt gratis; providerkosten kunnen gelden." else "Tikkie Business is not unlimited-free; provider fees may apply.")
  Provider("iDEAL / Wero",iOn,{iOn=it},ideal,{ideal=it},if(nl)"Via PSP/acquirer; tarieven verschillen per provider." else "Via PSP/acquirer; pricing varies by provider.")
  Provider("Revolut Pay",rOn,{rOn=it},revolut,{revolut=it},if(nl)"Merchant/payment-link transactiekosten kunnen gelden." else "Merchant/payment-link transaction fees can apply.")
  Provider("Card / Apple Pay / Google Pay",cardOn,{cardOn=it},card,{card=it},if(nl)"Via PSP; transactiekosten gelden." else "Via PSP; transaction fees apply.")
  Provider("PayPal",ppOn,{ppOn=it},paypal,{paypal=it},if(nl)"Providerkosten gelden doorgaans." else "Provider fees generally apply.")
  Provider(if(nl)"Andere betaalmethode" else "Custom payment method",customOn,{customOn=it},custom,{custom=it},if(nl)"Voor elke extra betaallink." else "For any additional payment link.")
  CardX{Text(if(nl)"ADMINISTRATIEVE WORKFLOW" else "ADMINISTRATIVE WORKFLOW",color=GL,fontWeight=FontWeight.Bold);listOf("Draft → Sent → Paid → Overdue",if(nl)"Koppel betaalmethode aan lid/factuur" else "Attach payment method to member/invoice",if(nl)"Registreer handmatige betaling + referentie" else "Record manual payment + reference",if(nl)"Maandrapport voor boekhouding" else "Monthly accounting report",if(nl)"Providercredentials alleen server-side" else "Provider credentials server-side only").forEach{Text("♛ $it",color=C,fontSize=12.sp)};Button({store.ps("company",company);store.ps("iban",iban);store.ps("bic",bic);store.ps("kvk",kvk);store.ps("vat",vat);store.ps("billing",mail);store.ps("tikkie",tikkie);store.ps("ideal",ideal);store.ps("revolut",revolut);store.ps("card",card);store.ps("paypal",paypal);store.ps("custom",custom);store.pb("bankOn",bankOn);store.pb("cashOn",cashOn);store.pb("tOn",tOn);store.pb("iOn",iOn);store.pb("rOn",rOn);store.pb("cardOn",cardOn);store.pb("ppOn",ppOn);store.pb("customOn",customOn);saved=true},Modifier.fillMaxWidth()){Text(if(saved)(if(nl)"Opgeslagen ✓" else "Saved ✓") else (if(nl)"Betaalinstellingen opslaan" else "Save payment settings"))}}
 }
}

@Composable private fun Provider(name:String,on:Boolean,setOn:(Boolean)->Unit,link:String,setLink:(String)->Unit,note:String){CardX{Toggle("Enable $name",on,setOn);if(on)Field(link,setLink,"$name payment link");Text(note,color=M,fontSize=11.sp)}}

@Composable private fun MemberPay(lang:L,store:S){val nl=lang==L.NL;Screen(if(nl)"Lidmaatschap & Betalen" else "Membership & Payments",if(nl)"Alleen methoden die Trainer/Admin activeert." else "Only methods enabled by Trainer/Admin are shown."){CardX{Text("RS PRO",color=GL,fontSize=26.sp,fontWeight=FontWeight.Black);Text("€49 / month",color=C)};if(store.b("bankOn",true))Pay("Bank transfer",if(nl)"Handmatig · geen app-processorfee" else "Manual · no app processor fee",store.s("iban","Set IBAN"),null);if(store.b("cashOn",true))Pay(if(nl)"Contant" else "Cash",if(nl)"Handmatig op locatie" else "Manual at club",if(nl)"Trainer bevestigt betaling" else "Trainer confirms payment",null);if(store.b("tOn",true))Pay("Tikkie",if(nl)"Providerkosten kunnen gelden" else "Provider fees may apply","Payment request",store.s("tikkie"));if(store.b("iOn",true))Pay("iDEAL / Wero","PSP / acquirer","Online payment",store.s("ideal"));if(store.b("rOn",true))Pay("Revolut Pay",if(nl)"Transactiekosten kunnen gelden" else "Transaction fees may apply","Payment link",store.s("revolut"));if(store.b("cardOn"))Pay("Card / Apple Pay / Google Pay","PSP","Wallet/card payment",store.s("card"));if(store.b("ppOn"))Pay("PayPal","Provider","External payment",store.s("paypal"));if(store.b("customOn"))Pay(if(nl)"Andere betaalmethode" else "Custom payment","Custom","External link",store.s("custom"))}}
@Composable private fun Pay(name:String,fee:String,detail:String,link:String?){val uri=LocalUriHandler.current;CardX{Text(name,color=GL,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(fee,color=M,fontSize=11.sp);Text(detail,color=C);if(!link.isNullOrBlank())Button({uri.openUri(link)},Modifier.fillMaxWidth()){Text("Open payment")}}}

@Composable private fun Promo(lang:L,store:S){val nl=lang==L.NL;var motion by remember{mutableStateOf(store.b("motion",true))};var login by remember{mutableStateOf(store.b("loginBg",true))};var training by remember{mutableStateOf(store.b("trainingBg",true))};Screen(if(nl)"Visuals & Promotiebeheer" else "Visual & Promotion Manager",if(nl)"Live achtergronden met leesbaarheidsregels." else "Live backgrounds with readability rules."){CardX{Toggle(if(nl)"Live login achtergrond" else "Live login background",login){login=it};Toggle(if(nl)"Subtiele trainingsachtergrond" else "Subtle training background",training){training=it};Toggle(if(nl)"Beweging" else "Motion",motion){motion=it};Text(if(nl)"Promotie mag sterker bewegen; training blijft donker en subtiel." else "Promotion may use stronger motion; training stays dark and subtle.",color=M);Button({store.pb("motion",motion);store.pb("loginBg",login);store.pb("trainingBg",training)},Modifier.fillMaxWidth()){Text(if(nl)"Opslaan" else "Save")}};CardX{listOf("Login hero","Dashboard hero","Trainer Book","Landing page","Events","Fight Camp","Academy","Session Player").forEach{Text("♛ $it",color=C)}}}}

@Composable private fun Session(lang:L){val nl=lang==L.NL;var running by remember{mutableStateOf(false)};var sec by remember{mutableIntStateOf(120)};var round by remember{mutableIntStateOf(1)};LaunchedEffect(running,sec){if(running&&sec>0){delay(1000);sec--}else if(sec==0)running=false};Screen(if(nl)"Sessie speler" else "Session Player",if(nl)"Subtiele bewegende achtergrond; timer blijft dominant." else "Subtle moving background; timer remains dominant."){CardX{Text("ROUND $round / 5",color=GL,fontWeight=FontWeight.Bold);Text("%d:%02d".format(sec/60,sec%60),color=GL,fontSize=48.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=M);Button({running=!running},Modifier.fillMaxWidth()){Text(if(running)(if(nl)"Pauze" else "Pause") else "Start")};OutlinedButton({round=(round+1).coerceAtMost(5);sec=120;running=false},Modifier.fillMaxWidth()){Text(if(nl)"Volgende ronde" else "Next round")}}}}

@Composable private fun Classes(lang:L,store:S){val nl=lang==L.NL;Screen(if(nl)"Lessen & Events" else "Classes & Events","RS schedule"){listOf("Technique & Pads" to "Today · 19:00 · 12/16","Fundamentals" to "Thu · 18:30 · 9/16","Advanced Sparring" to "Fri · 20:00 · 14/14").forEach{(n,d)->var booked by remember(n){mutableStateOf(store.b("book_$n",n=="Technique & Pads"))};CardX{Text(n,color=GL,fontWeight=FontWeight.Bold);Text(d,color=M);Button({booked=!booked;store.pb("book_$n",booked)},Modifier.fillMaxWidth()){Text(if(booked)(if(nl)"Geboekt ✓" else "Booked ✓") else (if(nl)"Boek les" else "Book class"))}}}}}

@Composable private fun Fight(lang:L,store:S){val nl=lang==L.NL;var w by remember{mutableIntStateOf(store.i("fightweek",3))};Screen("Fight Camp",if(nl)"Wedstrijdvoorbereiding met rustige beweging." else "Fight preparation with restrained motion."){CardX{Text("TBD · Club Match",color=GL,fontSize=21.sp,fontWeight=FontWeight.Black);Text("24 Oct 2026 · -75 kg",color=M);LinearProgressIndicator(progress={w/6f},modifier=Modifier.fillMaxWidth());Text("Week $w / 6 · Readiness 87%",color=C);Button({if(w<6){w++;store.pi("fightweek",w)}},Modifier.fillMaxWidth()){Text(if(nl)"Volgende week" else "Advance week")}};CardX{Text(if(nl)"Geen agressieve gewichtsdoelen of medisch advies zonder professionele begeleiding." else "No aggressive weight targets or medical advice without professional supervision.",color=M)}}}

@Composable private fun Book(lang:L){val nl=lang==L.NL;val uri=LocalUriHandler.current;Screen("Van Stilte Naar Strijd","Kickboksen, karakter en de weg van basis naar beheersing"){CardX{Text("Van Stilte Naar Strijd",color=GL,fontSize=24.sp,fontWeight=FontWeight.Black);Text(if(nl)"Geïntegreerd met Academy, mindset en training." else "Integrated with Academy, mindset and training.",color=M);Button({uri.openUri("https://www.amazon.nl/s?k=Van+Stilte+Naar+Strijd")},Modifier.fillMaxWidth()){Text(if(nl)"Zoek op Amazon" else "Search on Amazon")}}}}

@Composable private fun Module(route:String,lang:L,store:S,onRoute:(String)->Unit){val nl=lang==L.NL;val f=fs.firstOrNull{it.id==route};val title=if(nl)f?.nl?:route else f?.en?:route;var done by remember(route){mutableStateOf(store.b("x_$route"))};val rows=when(route){"ai"->listOf("Technique Analysis" to "84% · guard recovery","Corrective Workout" to "3 drills · 12 min");"train"->listOf("20 min Technique" to "Jab · Cross · Low Kick","30 min Conditioning" to "Intervals · Core · Recovery");"progress"->listOf("Level 3" to "12,480 XP","Mastery" to "72% overall");"members"->listOf("Mila Jansen" to "Level 4 · 96%","Noah Bakker" to "Level 2 · 84%");"plansadmin"->listOf("RS BASIC" to "€29","RS PRO" to "€49","RS ELITE" to "€79");else->listOf("RS Status" to "Ready","Mobile layout" to "Safe · scrollable · responsive")};Screen(title,if(nl)"Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout."){rows.forEach{(a,b)->CardX{Text(a,color=GL,fontWeight=FontWeight.Bold);Text(b,color=M)}};if(route=="train")Button({onRoute("session")},Modifier.fillMaxWidth()){Text(if(nl)"Open sessiespeler" else "Open Session Player")};if(route=="compare")Button({onRoute("academy")},Modifier.fillMaxWidth()){Text("Open RS Academy")};CardX{Button({done=!done;store.pb("x_$route",done)},Modifier.fillMaxWidth()){Text(if(done)(if(nl)"Opgeslagen ✓" else "Saved ✓") else (if(nl)"Test actie" else "Test action"))}}}}

@Composable private fun Screen(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=2.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title,color=GL,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,modifier=Modifier.fillMaxWidth());Text(subtitle,color=M,modifier=Modifier.fillMaxWidth());content();Spacer(Modifier.height(22.dp))}}
@Composable private fun CardX(content:@Composable ColumnScope.()->Unit){Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,GL.copy(alpha=.31f)),colors=CardDefaults.cardColors(containerColor=P)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
@Composable private fun Field(v:String,on:(String)->Unit,label:String){OutlinedTextField(v,on,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}
@Composable private fun Toggle(label:String,v:Boolean,on:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=C,modifier=Modifier.weight(1f));Switch(v,on)}}
@Composable private fun Lang(lang:L,on:(L)->Unit){Surface(shape=RoundedCornerShape(16.dp),color=P2,border=BorderStroke(1.dp,G.copy(alpha=.4f))){Row(Modifier.padding(2.dp)){Text("EN",Modifier.clickable{on(L.EN)}.padding(horizontal=8.dp,vertical=5.dp),color=if(lang==L.EN)GL else M,fontWeight=FontWeight.Bold,fontSize=10.sp);Text("NL",Modifier.clickable{on(L.NL)}.padding(horizontal=8.dp,vertical=5.dp),color=if(lang==L.NL)GL else M,fontWeight=FontWeight.Bold,fontSize=10.sp)}}}

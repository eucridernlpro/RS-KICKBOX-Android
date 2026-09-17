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

private val V9Gold = Color(0xFFC08A24)
private val V9GoldLight = Color(0xFFF0CF79)
private val V9Cream = Color(0xFFF6F0E4)
private val V9Muted = Color(0xFFB8AD98)
private val V9Panel = Color(0xF511100E)
private val V9Panel2 = Color(0xFF211A10)
private enum class V9Role { STUDENT, TRAINER }
private enum class V9Lang { EN, NL }
private data class V9Feature(val id:String,val en:String,val nl:String,val admin:Boolean=false,val icon:String="♛")

private val v9Features = listOf(
 V9Feature("academy","RS Academy","RS Academy"),V9Feature("ai","AI Coach","AI Coach"),V9Feature("train","Train Anywhere","Overal Trainen"),V9Feature("session","Session Player","Sessie Speler"),V9Feature("compare","Technique Compare","Techniek Vergelijken"),V9Feature("classes","Classes & Events","Lessen & Events"),V9Feature("community","Community","Community"),V9Feature("progress","Progress & Profile","Voortgang & Profiel"),V9Feature("homework","Homework","Huiswerk"),V9Feature("notifications","Notifications","Meldingen"),V9Feature("saved","Saved & Favorites","Opgeslagen & Favorieten"),V9Feature("history","Training History","Trainingsgeschiedenis"),V9Feature("challenges","Challenges","Uitdagingen"),V9Feature("fightcamp","Fight Camp","Fight Camp"),V9Feature("finance","Membership & Payments","Lidmaatschap & Betalen"),V9Feature("profile","My Profile","Mijn Profiel"),V9Feature("media","Training Media","Trainingsmedia"),V9Feature("settings","Settings & Privacy","Instellingen & Privacy"),V9Feature("book","Trainer Book","Boek van de Trainer"),V9Feature("vault","Knowledge Vault","Kennisbank"),V9Feature("support","Support & Documents","Support & Documenten"),V9Feature("private","Private Lessons","Privélessen"),
 V9Feature("payments","Payment Center","Betaalcentrum",true,"€"),V9Feature("invoices","Invoices & Payments","Facturen & Betalingen",true),V9Feature("members","Student Manager","Ledenbeheer",true),V9Feature("attendance","Attendance Manager","Aanwezigheidsbeheer",true),V9Feature("homeworkadmin","Homework Manager","Huiswerkbeheer",true),V9Feature("musicadmin","Music Manager","Muziekbeheer",true),V9Feature("lessonadmin","Lesson Editor","Leseditor",true),V9Feature("classadmin","Class Manager","Lesbeheer",true),V9Feature("notifyadmin","Notification Composer","Meldingen Opstellen",true),V9Feature("plansadmin","Membership Plans","Lidmaatschapsplannen",true),V9Feature("progressadmin","Progress Manager","Voortgangsbeheer",true),V9Feature("assessmentsadmin","Coach Assessments","Coachbeoordelingen",true),V9Feature("eventsadmin","Event Manager","Eventbeheer",true),V9Feature("scheduleadmin","Weekly Schedule","Weekplanning",true),V9Feature("attendanceflow","QR Attendance","QR Aanwezigheid",true),V9Feature("bookadmin","Book Manager","Boekbeheer",true),V9Feature("landingadmin","Landing Page Manager","Landingpage Beheer",true),V9Feature("promo","Visual & Promotion Manager","Visuals & Promotiebeheer",true,"✦"),V9Feature("sessionadmin","Session Builder","Sessie Bouwer",true),V9Feature("challengesadmin","Challenge Manager","Uitdagingenbeheer",true),V9Feature("fightcampadmin","Fight Camp Manager","Fight Camp Beheer",true)
)

private class V9Store(ctx:Context){
 private val p=ctx.getSharedPreferences("rs_v09",Context.MODE_PRIVATE)
 fun bool(k:String,d:Boolean=false)=p.getBoolean(k,d)
 fun putBool(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
 fun int(k:String,d:Int=0)=p.getInt(k,d)
 fun putInt(k:String,v:Int)=p.edit().putInt(k,v).apply()
 fun str(k:String,d:String="")=p.getString(k,d)?:d
 fun putStr(k:String,v:String)=p.edit().putString(k,v).apply()
}

@Composable
fun RsKickboxV09App(){
 val context=LocalContext.current
 val store=remember(context){V9Store(context)}
 var role by remember{mutableStateOf<V9Role?>(null)}
 var route by remember{mutableStateOf("home")}
 var lang by remember{mutableStateOf(if(store.str("lang","EN")=="NL")V9Lang.NL else V9Lang.EN)}
 MaterialTheme(colorScheme=darkColorScheme(primary=V9GoldLight,secondary=V9Gold,background=Color.Black,surface=V9Panel,onBackground=V9Cream,onSurface=V9Cream)){
  if(role==null) V9Login(lang,{lang=it;store.putStr("lang",it.name)}){role=it;route=if(it==V9Role.TRAINER)"admin" else "home"}
  else V9Shell(role!!,route,lang,store,{route=it},{lang=it;store.putStr("lang",it.name)}){role=null}
 }
}

@Composable
private fun V9LiveBackground(route:String,content:@Composable BoxScope.()->Unit){
 val transition=rememberInfiniteTransition(label="rs-live")
 val drift by transition.animateFloat(initialValue=-.2f,targetValue=1.2f,animationSpec=infiniteRepeatable(tween(14000),RepeatMode.Reverse),label="drift")
 val training=route in setOf("academy","ai","train","session","compare","progress","fightcamp","challenges")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF050505),Color(0xFF0D0904),Color(0xFF050505)))).windowInsetsPadding(WindowInsets.safeDrawing)){
  Canvas(Modifier.fillMaxSize()){
   drawCircle(V9GoldLight.copy(alpha=if(training).05f else .11f),size.minDimension*.43f,Offset(size.width*drift,size.height*.2f))
   drawCircle(V9Gold.copy(alpha=.07f),size.minDimension*.32f,Offset(size.width*(1f-drift),size.height*.76f))
   repeat(3){n->val y=size.height*(.56f+n*.055f);drawLine(V9GoldLight.copy(alpha=if(training).08f else .12f),Offset(0f,y),Offset(size.width,y+18f),strokeWidth=2f)}
  }
  Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(training).42f else .28f)))
  content()
 }
}

@Composable
private fun V9Login(lang:V9Lang,onLang:(V9Lang)->Unit,onLogin:(V9Role)->Unit){
 val nl=lang==V9Lang.NL
 var email by remember{mutableStateOf("alex@rskickbox.nl")};var password by remember{mutableStateOf("preview123")}
 V9LiveBackground("login"){
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("♛ RS KICKBOX",color=V9GoldLight,fontSize=28.sp,fontWeight=FontWeight.Black);Text("TRAIN · LEARN · CONNECT · GROW",color=V9Muted,fontSize=10.sp)};V9Language(lang,onLang)}
   Text(if(nl)"Premium kickboksplatform" else "Premium kickboxing platform",color=V9Cream,style=MaterialTheme.typography.headlineMedium)
   V9Card{Text(if(nl)"LEDEN TOEGANG" else "MEMBER ACCESS",color=V9GoldLight,fontWeight=FontWeight.Bold);OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(password,{password=it},label={Text(if(nl)"Wachtwoord" else "Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());Button(onClick={onLogin(V9Role.STUDENT)},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Leerling login" else "Student login")};OutlinedButton(onClick={onLogin(V9Role.TRAINER)},modifier=Modifier.fillMaxWidth()){Text("Trainer / Admin")}}
   V9Card{Text("Van Stilte Naar Strijd",color=V9GoldLight,fontSize=22.sp,fontWeight=FontWeight.Black);Text("Kickboksen, karakter en de weg van basis naar beheersing",color=V9Cream);Text(if(nl)"Premium live RS-achtergrond met donkere leesbaarheidslaag." else "Premium live RS background with a dark readability layer.",color=V9Muted)}
  }
 }
}

@Composable
private fun V9Shell(role:V9Role,route:String,lang:V9Lang,store:V9Store,onRoute:(String)->Unit,onLang:(V9Lang)->Unit,onLogout:()->Unit){
 val home=if(role==V9Role.TRAINER)"admin" else "home";val current=v9Features.firstOrNull{it.id==route}
 V9LiveBackground(route){Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,V9GoldLight.copy(alpha=.4f)),colors=CardDefaults.cardColors(containerColor=V9Panel)){
   Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("♛ RS KICKBOX",color=V9GoldLight,fontSize=20.sp,fontWeight=FontWeight.Black);Text(if(role==V9Role.TRAINER)"TRAINER / ADMIN" else "STUDENT",color=V9Muted,fontSize=9.sp);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){V9Language(lang,onLang);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){if(route!=home)OutlinedButton(onClick={onRoute(home)},contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text("‹")};OutlinedButton(onClick=onLogout,contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp)){Text(if(lang==V9Lang.NL)"Uitloggen" else "Log out",fontSize=10.sp)}}};if(current!=null)Text(if(lang==V9Lang.NL)current.nl else current.en,color=V9Cream,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
  }
  Box(Modifier.weight(1f).fillMaxWidth()){V9Router(role,route,lang,store,onRoute)}
 }}
}

@Composable private fun V9Router(role:V9Role,route:String,lang:V9Lang,store:V9Store,onRoute:(String)->Unit){when(route){"home","admin"->V9Dashboard(role,lang,onRoute);"payments","invoices"->V9PaymentCenter(lang,store);"finance"->V9MemberPayments(lang,store);"session"->V9Session(lang);"classes"->V9Classes(lang,store);"fightcamp"->V9FightCamp(lang,store);"book","bookadmin"->V9Book(lang);"promo","landingadmin"->V9Promotion(lang,store);else->V9Module(route,lang,store,onRoute)}}

@Composable
private fun V9Dashboard(role:V9Role,lang:V9Lang,onRoute:(String)->Unit){
 val nl=lang==V9Lang.NL;val list=v9Features.filter{if(role==V9Role.TRAINER)it.admin else !it.admin}
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){V9Card{Text(if(role==V9Role.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=V9GoldLight,fontWeight=FontWeight.Bold);Text(if(role==V9Role.TRAINER)(if(nl)"Beheer leden, trainingen, betalingen en promotie." else "Manage members, training, payments and promotion.") else (if(nl)"Train, leer, boek en betaal binnen RS KICKBOX." else "Train, learn, book and pay inside RS KICKBOX."),color=V9Cream)};LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=18.dp)){items(list){f->Card(modifier=Modifier.fillMaxWidth().clickable{onRoute(f.id)},shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,V9Gold.copy(alpha=.35f)),colors=CardDefaults.cardColors(containerColor=V9Panel)){Column(Modifier.padding(13.dp)){Text(f.icon,color=V9GoldLight,fontSize=19.sp);Text(if(nl)f.nl else f.en,color=V9GoldLight,fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)}}}}}
}

@Composable
private fun V9PaymentCenter(lang:V9Lang,store:V9Store){
 val nl=lang==V9Lang.NL;var saved by remember{mutableStateOf(false)}
 var company by remember{mutableStateOf(store.str("company","RS KICKBOX"))};var iban by remember{mutableStateOf(store.str("iban"))};var bic by remember{mutableStateOf(store.str("bic"))};var kvk by remember{mutableStateOf(store.str("kvk"))};var vat by remember{mutableStateOf(store.str("vat"))};var mail by remember{mutableStateOf(store.str("billing"))}
 var tikkie by remember{mutableStateOf(store.str("tikkie"))};var ideal by remember{mutableStateOf(store.str("ideal"))};var revolut by remember{mutableStateOf(store.str("revolut"))};var card by remember{mutableStateOf(store.str("card"))};var paypal by remember{mutableStateOf(store.str("paypal"))};var custom by remember{mutableStateOf(store.str("custom"))}
 var bankOn by remember{mutableStateOf(store.bool("bankOn",true))};var cashOn by remember{mutableStateOf(store.bool("cashOn",true))};var tOn by remember{mutableStateOf(store.bool("tOn",true))};var iOn by remember{mutableStateOf(store.bool("iOn",true))};var rOn by remember{mutableStateOf(store.bool("rOn",true))};var cardOn by remember{mutableStateOf(store.bool("cardOn"))};var ppOn by remember{mutableStateOf(store.bool("ppOn"))};var customOn by remember{mutableStateOf(store.bool("customOn"))}
 V9Screen(if(nl)"Betaalcentrum" else "Payment Center",if(nl)"Beheer alle betaalinformatie vanuit Trainer/Admin." else "Manage all payment information from Trainer/Admin."){
  V9Card{Text(if(nl)"BEDRIJFS- EN FACTUURGEGEVENS" else "BUSINESS & INVOICE DETAILS",color=V9GoldLight,fontWeight=FontWeight.Bold);V9Field(company,{company=it},if(nl)"Bedrijfsnaam" else "Business name");V9Field(iban,{iban=it},"IBAN");V9Field(bic,{bic=it},"BIC");V9Field(kvk,{kvk=it},"KvK");V9Field(vat,{vat=it},if(nl)"BTW-nummer" else "VAT number");V9Field(mail,{mail=it},if(nl)"Factuur e-mail" else "Billing email")}
  V9Card{V9Toggle(if(nl)"Bankoverschrijving" else "Bank transfer",bankOn){bankOn=it};V9Toggle(if(nl)"Contant" else "Cash",cashOn){cashOn=it};Text(if(nl)"Handmatig: geen app-processorfee; bankkosten kunnen wel gelden." else "Manual: no app processor fee; bank fees can still apply.",color=V9Muted,fontSize=11.sp)}
  V9Provider("Tikkie",tOn,{tOn=it},tikkie,{tikkie=it},if(nl)"Tikkie Zakelijk is niet onbeperkt gratis; providerkosten kunnen gelden." else "Tikkie Business is not unlimited-free; provider fees may apply.")
  V9Provider("iDEAL / Wero",iOn,{iOn=it},ideal,{ideal=it},if(nl)"Via PSP/acquirer; tarieven verschillen per provider." else "Via PSP/acquirer; pricing varies by provider.")
  V9Provider("Revolut Pay",rOn,{rOn=it},revolut,{revolut=it},if(nl)"Merchant/payment-link transactiekosten kunnen gelden." else "Merchant/payment-link transaction fees can apply.")
  V9Provider("Card / Apple Pay / Google Pay",cardOn,{cardOn=it},card,{card=it},if(nl)"Via PSP; transactiekosten gelden." else "Via PSP; transaction fees apply.")
  V9Provider("PayPal",ppOn,{ppOn=it},paypal,{paypal=it},if(nl)"Providerkosten gelden doorgaans." else "Provider fees generally apply.")
  V9Provider(if(nl)"Andere betaalmethode" else "Custom payment method",customOn,{customOn=it},custom,{custom=it},if(nl)"Voor elke extra betaallink." else "For any additional payment link.")
  V9Card{Text(if(nl)"ADMINISTRATIEVE WORKFLOW" else "ADMINISTRATIVE WORKFLOW",color=V9GoldLight,fontWeight=FontWeight.Bold);listOf("Draft → Sent → Paid → Overdue",if(nl)"Koppel betaalmethode aan lid/factuur" else "Attach payment method to member/invoice",if(nl)"Registreer handmatige betaling + referentie" else "Record manual payment + reference",if(nl)"Maandrapport voor boekhouding" else "Monthly accounting report",if(nl)"Providercredentials alleen server-side" else "Provider credentials server-side only").forEach{Text("♛ $it",color=V9Cream,fontSize=12.sp)};Button(onClick={store.putStr("company",company);store.putStr("iban",iban);store.putStr("bic",bic);store.putStr("kvk",kvk);store.putStr("vat",vat);store.putStr("billing",mail);store.putStr("tikkie",tikkie);store.putStr("ideal",ideal);store.putStr("revolut",revolut);store.putStr("card",card);store.putStr("paypal",paypal);store.putStr("custom",custom);store.putBool("bankOn",bankOn);store.putBool("cashOn",cashOn);store.putBool("tOn",tOn);store.putBool("iOn",iOn);store.putBool("rOn",rOn);store.putBool("cardOn",cardOn);store.putBool("ppOn",ppOn);store.putBool("customOn",customOn);saved=true},modifier=Modifier.fillMaxWidth()){Text(if(saved)(if(nl)"Opgeslagen ✓" else "Saved ✓") else (if(nl)"Betaalinstellingen opslaan" else "Save payment settings"))}}
 }
}

@Composable private fun V9Provider(name:String,on:Boolean,setOn:(Boolean)->Unit,link:String,setLink:(String)->Unit,note:String){V9Card{V9Toggle("Enable $name",on,setOn);if(on)V9Field(link,setLink,"$name payment link");Text(note,color=V9Muted,fontSize=11.sp)}}

@Composable
private fun V9MemberPayments(lang:V9Lang,store:V9Store){val nl=lang==V9Lang.NL;V9Screen(if(nl)"Lidmaatschap & Betalen" else "Membership & Payments",if(nl)"Alleen methoden die Trainer/Admin activeert." else "Only methods enabled by Trainer/Admin are shown."){V9Card{Text("RS PRO",color=V9GoldLight,fontSize=26.sp,fontWeight=FontWeight.Black);Text("€49 / month",color=V9Cream)};if(store.bool("bankOn",true))V9Pay("Bank transfer",if(nl)"Handmatig · geen app-processorfee" else "Manual · no app processor fee",store.str("iban","Set IBAN"),null);if(store.bool("cashOn",true))V9Pay(if(nl)"Contant" else "Cash",if(nl)"Handmatig op locatie" else "Manual at club",if(nl)"Trainer bevestigt betaling" else "Trainer confirms payment",null);if(store.bool("tOn",true))V9Pay("Tikkie",if(nl)"Providerkosten kunnen gelden" else "Provider fees may apply","Payment request",store.str("tikkie"));if(store.bool("iOn",true))V9Pay("iDEAL / Wero","PSP / acquirer","Online payment",store.str("ideal"));if(store.bool("rOn",true))V9Pay("Revolut Pay",if(nl)"Transactiekosten kunnen gelden" else "Transaction fees may apply","Payment link",store.str("revolut"));if(store.bool("cardOn"))V9Pay("Card / Apple Pay / Google Pay","PSP","Wallet/card payment",store.str("card"));if(store.bool("ppOn"))V9Pay("PayPal","Provider","External payment",store.str("paypal"));if(store.bool("customOn"))V9Pay(if(nl)"Andere betaalmethode" else "Custom payment","Custom","External link",store.str("custom"))}}
@Composable private fun V9Pay(name:String,fee:String,detail:String,link:String?){val uri=LocalUriHandler.current;V9Card{Text(name,color=V9GoldLight,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(fee,color=V9Muted,fontSize=11.sp);Text(detail,color=V9Cream);if(!link.isNullOrBlank())Button(onClick={uri.openUri(link)},modifier=Modifier.fillMaxWidth()){Text("Open payment")}}}

@Composable private fun V9Promotion(lang:V9Lang,store:V9Store){val nl=lang==V9Lang.NL;var motion by remember{mutableStateOf(store.bool("motion",true))};var login by remember{mutableStateOf(store.bool("loginBg",true))};var training by remember{mutableStateOf(store.bool("trainingBg",true))};V9Screen(if(nl)"Visuals & Promotiebeheer" else "Visual & Promotion Manager",if(nl)"Live achtergronden met leesbaarheidsregels." else "Live backgrounds with readability rules."){V9Card{V9Toggle(if(nl)"Live login achtergrond" else "Live login background",login){login=it};V9Toggle(if(nl)"Subtiele trainingsachtergrond" else "Subtle training background",training){training=it};V9Toggle(if(nl)"Beweging" else "Motion",motion){motion=it};Text(if(nl)"Promotie mag sterker bewegen; training blijft donker en subtiel." else "Promotion may use stronger motion; training stays dark and subtle.",color=V9Muted);Button(onClick={store.putBool("motion",motion);store.putBool("loginBg",login);store.putBool("trainingBg",training)},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Opslaan" else "Save")}};V9Card{listOf("Login hero","Dashboard hero","Trainer Book","Landing page","Events","Fight Camp","Academy","Session Player").forEach{Text("♛ $it",color=V9Cream)}}}}

@Composable private fun V9Session(lang:V9Lang){val nl=lang==V9Lang.NL;var running by remember{mutableStateOf(false)};var sec by remember{mutableIntStateOf(120)};var round by remember{mutableIntStateOf(1)};LaunchedEffect(running,sec){if(running&&sec>0){delay(1000);sec--}else if(sec==0)running=false};V9Screen(if(nl)"Sessie speler" else "Session Player",if(nl)"Subtiele bewegende achtergrond; timer blijft dominant." else "Subtle moving background; timer remains dominant."){V9Card{Text("ROUND $round / 5",color=V9GoldLight,fontWeight=FontWeight.Bold);Text("%d:%02d".format(sec/60,sec%60),color=V9GoldLight,fontSize=48.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=V9Muted);Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){Text(if(running)(if(nl)"Pauze" else "Pause") else "Start")};OutlinedButton(onClick={round=(round+1).coerceAtMost(5);sec=120;running=false},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Volgende ronde" else "Next round")}}}}

@Composable private fun V9Classes(lang:V9Lang,store:V9Store){val nl=lang==V9Lang.NL;V9Screen(if(nl)"Lessen & Events" else "Classes & Events","RS schedule"){listOf("Technique & Pads" to "Today · 19:00 · 12/16","Fundamentals" to "Thu · 18:30 · 9/16","Advanced Sparring" to "Fri · 20:00 · 14/14").forEach{(n,d)->var booked by remember(n){mutableStateOf(store.bool("book_$n",n=="Technique & Pads"))};V9Card{Text(n,color=V9GoldLight,fontWeight=FontWeight.Bold);Text(d,color=V9Muted);Button(onClick={booked=!booked;store.putBool("book_$n",booked)},modifier=Modifier.fillMaxWidth()){Text(if(booked)(if(nl)"Geboekt ✓" else "Booked ✓") else (if(nl)"Boek les" else "Book class"))}}}}}

@Composable private fun V9FightCamp(lang:V9Lang,store:V9Store){val nl=lang==V9Lang.NL;var week by remember{mutableIntStateOf(store.int("fightweek",3))};V9Screen("Fight Camp",if(nl)"Wedstrijdvoorbereiding met rustige beweging." else "Fight preparation with restrained motion."){V9Card{Text("TBD · Club Match",color=V9GoldLight,fontSize=21.sp,fontWeight=FontWeight.Black);Text("24 Oct 2026 · -75 kg",color=V9Muted);LinearProgressIndicator(progress={week/6f},modifier=Modifier.fillMaxWidth());Text("Week $week / 6 · Readiness 87%",color=V9Cream);Button(onClick={if(week<6){week++;store.putInt("fightweek",week)}},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Volgende week" else "Advance week")}};V9Card{Text(if(nl)"Geen agressieve gewichtsdoelen of medisch advies zonder professionele begeleiding." else "No aggressive weight targets or medical advice without professional supervision.",color=V9Muted)}}}

@Composable private fun V9Book(lang:V9Lang){val nl=lang==V9Lang.NL;val uri=LocalUriHandler.current;V9Screen("Van Stilte Naar Strijd","Kickboksen, karakter en de weg van basis naar beheersing"){V9Card{Text("Van Stilte Naar Strijd",color=V9GoldLight,fontSize=24.sp,fontWeight=FontWeight.Black);Text(if(nl)"Geïntegreerd met Academy, mindset en training." else "Integrated with Academy, mindset and training.",color=V9Muted);Button(onClick={uri.openUri("https://www.amazon.nl/s?k=Van+Stilte+Naar+Strijd")},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Zoek op Amazon" else "Search on Amazon")}}}}

@Composable private fun V9Module(route:String,lang:V9Lang,store:V9Store,onRoute:(String)->Unit){val nl=lang==V9Lang.NL;val f=v9Features.firstOrNull{it.id==route};val title=if(nl)f?.nl?:route else f?.en?:route;var done by remember(route){mutableStateOf(store.bool("x_$route"))};val rows=when(route){"ai"->listOf("Technique Analysis" to "84% · guard recovery","Corrective Workout" to "3 drills · 12 min");"train"->listOf("20 min Technique" to "Jab · Cross · Low Kick","30 min Conditioning" to "Intervals · Core · Recovery");"progress"->listOf("Level 3" to "12,480 XP","Mastery" to "72% overall");"members"->listOf("Mila Jansen" to "Level 4 · 96%","Noah Bakker" to "Level 2 · 84%");"plansadmin"->listOf("RS BASIC" to "€29","RS PRO" to "€49","RS ELITE" to "€79");else->listOf("RS Status" to "Ready","Mobile layout" to "Safe · scrollable · responsive")};V9Screen(title,if(nl)"Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout."){rows.forEach{(a,b)->V9Card{Text(a,color=V9GoldLight,fontWeight=FontWeight.Bold);Text(b,color=V9Muted)}};if(route=="train")Button(onClick={onRoute("session")},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Open sessiespeler" else "Open Session Player")};if(route=="compare")Button(onClick={onRoute("academy")},modifier=Modifier.fillMaxWidth()){Text("Open RS Academy")};V9Card{Button(onClick={done=!done;store.putBool("x_$route",done)},modifier=Modifier.fillMaxWidth()){Text(if(done)(if(nl)"Opgeslagen ✓" else "Saved ✓") else (if(nl)"Test actie" else "Test action"))}}}}

@Composable private fun V9Screen(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=2.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title,color=V9GoldLight,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,modifier=Modifier.fillMaxWidth());Text(subtitle,color=V9Muted,modifier=Modifier.fillMaxWidth());content();Spacer(Modifier.height(22.dp))}}
@Composable private fun V9Card(content:@Composable ColumnScope.()->Unit){Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,V9GoldLight.copy(alpha=.31f)),colors=CardDefaults.cardColors(containerColor=V9Panel)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
@Composable private fun V9Field(value:String,onChange:(String)->Unit,label:String){OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}
@Composable private fun V9Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=V9Cream,modifier=Modifier.weight(1f));Switch(value,onChange)}}
@Composable private fun V9Language(lang:V9Lang,onChange:(V9Lang)->Unit){Surface(shape=RoundedCornerShape(16.dp),color=V9Panel2,border=BorderStroke(1.dp,V9Gold.copy(alpha=.4f))){Row(Modifier.padding(2.dp)){Text("EN",Modifier.clickable{onChange(V9Lang.EN)}.padding(horizontal=8.dp,vertical=5.dp),color=if(lang==V9Lang.EN)V9GoldLight else V9Muted,fontWeight=FontWeight.Bold,fontSize=10.sp);Text("NL",Modifier.clickable{onChange(V9Lang.NL)}.padding(horizontal=8.dp,vertical=5.dp),color=if(lang==V9Lang.NL)V9GoldLight else V9Muted,fontWeight=FontWeight.Bold,fontSize=10.sp)}}}

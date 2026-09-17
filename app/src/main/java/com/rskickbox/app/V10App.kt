package com.rskickbox.app

import android.content.Context
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class V10Role { STUDENT, TRAINER }
private enum class V10Lang { EN, NL }
private enum class V10ThemeId { ELITE_GOLD, ROYAL_CRIMSON, MIDNIGHT_PLATINUM, EMERALD_COMBAT }
private enum class V10Plan { BASIC, PRO, ELITE }

private data class V10Theme(
    val id: V10ThemeId,
    val name: String,
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val accent: Color,
    val accent2: Color,
    val text: Color,
    val muted: Color
)

private val v10Themes = listOf(
    V10Theme(V10ThemeId.ELITE_GOLD, "Elite Gold", Color(0xFF050505), Color(0xFF15120D), Color(0xFF211A10), Color(0xFFC08A24), Color(0xFFF0CF79), Color(0xFFF6F0E4), Color(0xFFB8AD98)),
    V10Theme(V10ThemeId.ROYAL_CRIMSON, "Royal Crimson", Color(0xFF070405), Color(0xFF1A0D10), Color(0xFF281217), Color(0xFF9E2235), Color(0xFFF0A58E), Color(0xFFFFF3EF), Color(0xFFC9A6A2)),
    V10Theme(V10ThemeId.MIDNIGHT_PLATINUM, "Midnight Platinum", Color(0xFF04070B), Color(0xFF10161D), Color(0xFF18212B), Color(0xFF7B8A99), Color(0xFFE5EDF4), Color(0xFFF4F8FB), Color(0xFFAAB6C0)),
    V10Theme(V10ThemeId.EMERALD_COMBAT, "Emerald Combat", Color(0xFF030806), Color(0xFF0C1712), Color(0xFF11251B), Color(0xFF24855E), Color(0xFFB9E7C7), Color(0xFFF2FAF4), Color(0xFFA2B9AA))
)

private data class V10Feature(val id: String, val en: String, val nl: String)
private val studentFeaturesV10 = listOf(
    V10Feature("academy", "RS Academy", "RS Academy"),
    V10Feature("ai", "AI Coach", "AI Coach"),
    V10Feature("train", "Train Anywhere", "Overal Trainen"),
    V10Feature("session", "Session Player", "Sessie Speler"),
    V10Feature("compare", "Technique Compare", "Techniek Vergelijken"),
    V10Feature("classes", "Classes & Events", "Lessen & Events"),
    V10Feature("community", "Community", "Community"),
    V10Feature("progress", "Progress & Profile", "Voortgang & Profiel"),
    V10Feature("homework", "Homework", "Huiswerk"),
    V10Feature("notifications", "Notifications", "Meldingen"),
    V10Feature("history", "Training History", "Trainingsgeschiedenis"),
    V10Feature("challenges", "Challenges", "Uitdagingen"),
    V10Feature("fightcamp", "Fight Camp", "Fight Camp"),
    V10Feature("finance", "Membership & Payments", "Lidmaatschap & Betalen"),
    V10Feature("media", "Training Media", "Trainingsmedia"),
    V10Feature("book", "Trainer Book", "Boek van de Trainer"),
    V10Feature("vault", "Knowledge Vault", "Kennisbank"),
    V10Feature("private", "Private Lessons", "Privélessen"),
    V10Feature("support", "Support & Documents", "Support & Documenten"),
    V10Feature("settings", "Settings & Privacy", "Instellingen & Privacy")
)

private val adminFeaturesV10 = listOf(
    V10Feature("themes", "Visual Theme Studio", "Visueel Thema Studio"),
    V10Feature("access", "Access & Subscription Control", "Toegang & Abonnementen"),
    V10Feature("payments", "Payment Center", "Betaalcentrum"),
    V10Feature("members", "Student Manager", "Ledenbeheer"),
    V10Feature("classesadmin", "Class Manager", "Lesbeheer"),
    V10Feature("attendance", "Attendance Manager", "Aanwezigheidsbeheer"),
    V10Feature("invoices", "Invoices", "Facturen"),
    V10Feature("homeworkadmin", "Homework Manager", "Huiswerkbeheer"),
    V10Feature("lessonsadmin", "Lesson Editor", "Leseditor"),
    V10Feature("notificationsadmin", "Notification Composer", "Meldingen Opstellen"),
    V10Feature("progressadmin", "Progress Manager", "Voortgangsbeheer"),
    V10Feature("bookadmin", "Book Manager", "Boekbeheer"),
    V10Feature("eventsadmin", "Event Manager", "Eventbeheer"),
    V10Feature("scheduleadmin", "Weekly Schedule", "Weekplanning"),
    V10Feature("sessionadmin", "Session Builder", "Sessie Bouwer"),
    V10Feature("fightcampadmin", "Fight Camp Manager", "Fight Camp Beheer")
)

private class V10Store(context: Context) {
    private val p = context.getSharedPreferences("rs_kickbox_v10", Context.MODE_PRIVATE)
    fun s(k:String,d:String="")=p.getString(k,d)?:d
    fun putS(k:String,v:String)=p.edit().putString(k,v).apply()
    fun b(k:String,d:Boolean=false)=p.getBoolean(k,d)
    fun putB(k:String,v:Boolean)=p.edit().putBoolean(k,v).apply()
    fun i(k:String,d:Int=0)=p.getInt(k,d)
    fun putI(k:String,v:Int)=p.edit().putInt(k,v).apply()
}

@Composable
fun RsKickboxV10App() {
    val context = LocalContext.current
    val store = remember { V10Store(context) }
    var role by remember { mutableStateOf<V10Role?>(null) }
    var lang by remember { mutableStateOf(if (store.s("lang","EN")=="NL") V10Lang.NL else V10Lang.EN) }
    var route by remember { mutableStateOf("dashboard") }
    var themeId by remember { mutableStateOf(runCatching { V10ThemeId.valueOf(store.s("theme", V10ThemeId.ELITE_GOLD.name)) }.getOrDefault(V10ThemeId.ELITE_GOLD)) }
    val theme = v10Themes.first { it.id == themeId }

    MaterialTheme(colorScheme = darkColorScheme(primary=theme.accent2, secondary=theme.accent, background=theme.bg, surface=theme.panel, onBackground=theme.text, onSurface=theme.text)) {
        if (role == null) {
            V10Login(theme, lang, onLang={lang=it;store.putS("lang",it.name)}) { selected -> role=selected; route=if(selected==V10Role.TRAINER) "admin" else "dashboard" }
        } else {
            V10Shell(theme, role!!, lang, route,
                onLang={lang=it;store.putS("lang",it.name)},
                onRoute={route=it},
                onLogout={role=null},
                content={ V10Router(theme, role!!, lang, route, store, onRoute={route=it}, onTheme={themeId=it;store.putS("theme",it.name)}) }
            )
        }
    }
}

@Composable
private fun LiveWallpaper(theme: V10Theme, strong: Boolean, content:@Composable BoxScope.()->Unit) {
    val infinite=rememberInfiniteTransition(label="rs-live")
    val t by infinite.animateFloat(0f,1f,infiniteRepeatable(tween(if(strong) 8500 else 14000, easing=LinearEasing),RepeatMode.Reverse),label="motion")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.bg, theme.panel2.copy(alpha=.75f), theme.bg))).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Canvas(Modifier.fillMaxSize()) {
            val a=if(strong).22f else .075f
            drawCircle(theme.accent.copy(alpha=a), radius=size.minDimension*(if(strong).32f else .25f), center=Offset(size.width*(.20f+.58f*t),size.height*.22f))
            drawCircle(theme.accent2.copy(alpha=a*.65f), radius=size.minDimension*.22f, center=Offset(size.width*(.78f-.48f*t),size.height*.75f))
            val ringAlpha=if(strong).20f else .055f
            repeat(3){ idx ->
                val r=size.minDimension*(.18f+idx*.085f)
                drawOval(theme.accent2.copy(alpha=ringAlpha), topLeft=Offset(size.width*.5f-r,size.height*.47f-r*.55f), size=Size(r*2,r*1.1f), style=Stroke(width=1.5f+idx))
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=if(strong).48f else .72f)))
        content()
    }
}

@Composable
private fun V10Login(theme:V10Theme, lang:V10Lang, onLang:(V10Lang)->Unit, onLogin:(V10Role)->Unit){
    val nl=lang==V10Lang.NL
    var email by remember{mutableStateOf("alex@rskickbox.nl")}; var pass by remember{mutableStateOf("preview123")}
    LiveWallpaper(theme,true){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)){
            Text("♛ RS KICKBOX",color=theme.accent2,fontSize=31.sp,fontWeight=FontWeight.Black)
            LangSwitchV10(theme,lang,onLang)
            Text(if(nl)"Premium live kickboksplatform" else "Premium live kickboxing platform",color=theme.text,style=MaterialTheme.typography.headlineMedium)
            Text("TRAIN · LEARN · CONNECT · GROW",color=theme.muted,fontSize=11.sp,letterSpacing=1.5.sp)
            PremiumCard(theme){
                Text(if(nl)"LEDEN TOEGANG" else "MEMBER ACCESS",color=theme.accent2,fontWeight=FontWeight.Bold,fontSize=11.sp)
                OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(pass,{pass=it},label={Text(if(nl)"Wachtwoord" else "Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
                Button(onClick={onLogin(V10Role.STUDENT)},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Leerling login" else "Student login")}
                OutlinedButton(onClick={onLogin(V10Role.TRAINER)},modifier=Modifier.fillMaxWidth()){Text("Trainer / Admin")}
            }
            PremiumCard(theme){ Text("Van Stilte Naar Strijd",color=theme.accent2,fontSize=23.sp,fontWeight=FontWeight.Black); Text("Kickboksen, karakter en de weg van basis naar beheersing",color=theme.text); Text(if(nl)"Trainerboek · premium RS-leerlijn" else "Trainer book · premium RS learning path",color=theme.muted) }
        }
    }
}

@Composable
private fun V10Shell(theme:V10Theme, role:V10Role, lang:V10Lang, route:String, onLang:(V10Lang)->Unit, onRoute:(String)->Unit, onLogout:()->Unit, content:@Composable()->Unit){
    val home=if(role==V10Role.TRAINER)"admin" else "dashboard"
    LiveWallpaper(theme,false){
        Column(Modifier.fillMaxSize().padding(horizontal=9.dp,vertical=7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            PremiumCard(theme){
                Text("♛ RS KICKBOX",color=theme.accent2,fontSize=20.sp,fontWeight=FontWeight.Black,maxLines=1)
                Text(if(role==V10Role.TRAINER)"TRAINER / ADMIN" else "STUDENT",color=theme.muted,fontSize=9.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    LangSwitchV10(theme,lang,onLang)
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        if(route!=home) OutlinedButton(onClick={onRoute(home)},contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp),modifier=Modifier.heightIn(min=36.dp)){Text("‹")}
                        OutlinedButton(onClick=onLogout,contentPadding=PaddingValues(horizontal=9.dp,vertical=3.dp),modifier=Modifier.heightIn(min=36.dp)){Text(if(lang==V10Lang.NL)"Uitloggen" else "Log out",fontSize=10.sp)}
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()){content()}
        }
    }
}

@Composable
private fun V10Router(theme:V10Theme, role:V10Role, lang:V10Lang, route:String, store:V10Store, onRoute:(String)->Unit, onTheme:(V10ThemeId)->Unit){
    when(route){
        "dashboard","admin"->V10Dashboard(theme,role,lang,store,onRoute)
        "themes"->ThemeStudio(theme,lang,store,onTheme)
        "access"->AccessControl(theme,lang,store)
        "payments"->PaymentCenter(theme,lang,store)
        "finance"->MemberPayments(theme,lang,store)
        "session"->SessionV10(theme,lang)
        "academy"->AcademyV10(theme,lang,store)
        "classes"->ClassesV10(theme,lang,store)
        else->GenericV10(theme,lang,route,role,store,onRoute)
    }
}

private fun planDefault(plan:V10Plan,id:String):Boolean = when(plan){
    V10Plan.BASIC -> id in setOf("academy","train","classes","progress","history","finance","book","support","settings")
    V10Plan.PRO -> id !in setOf("private","fightcamp")
    V10Plan.ELITE -> true
}

private fun allowedForStudent(store:V10Store, student:String, feature:String):Boolean {
    val plan=runCatching{V10Plan.valueOf(store.s("student_plan_$student",V10Plan.PRO.name))}.getOrDefault(V10Plan.PRO)
    val planKey="plan_${plan.name}_$feature"
    val planAllowed=store.b(planKey,planDefault(plan,feature))
    val overrideKey="override_${student}_$feature"
    return if(store.b("has_$overrideKey",false)) store.b(overrideKey,planAllowed) else planAllowed
}

@Composable
private fun V10Dashboard(theme:V10Theme, role:V10Role, lang:V10Lang, store:V10Store, onRoute:(String)->Unit){
    val nl=lang==V10Lang.NL
    val items=if(role==V10Role.TRAINER) adminFeaturesV10 else studentFeaturesV10.filter{allowedForStudent(store,"Alex",it.id)}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        PremiumCard(theme){
            Text(if(role==V10Role.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=theme.accent2,fontWeight=FontWeight.Black)
            Text(if(role==V10Role.TRAINER)(if(nl)"Volledige controle over thema, toegang, abonnementen en betalingen." else "Full control over theme, access, subscriptions and payments.") else (if(nl)"Jouw persoonlijke RS-omgeving." else "Your personal RS environment."),color=theme.text,style=MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){MiniV10(theme,if(role==V10Role.TRAINER)"Members" else "Level",if(role==V10Role.TRAINER)"124" else "3",Modifier.weight(1f));MiniV10(theme,if(role==V10Role.TRAINER)"Revenue" else "XP",if(role==V10Role.TRAINER)"€4,850" else "12,480",Modifier.weight(1f))}
        }
        LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=18.dp)){
            items(items){f->FeatureTile(theme,if(nl)f.nl else f.en){onRoute(f.id)}}
        }
    }
}

@Composable
private fun ThemeStudio(theme:V10Theme,lang:V10Lang,store:V10Store,onTheme:(V10ThemeId)->Unit){
    val nl=lang==V10Lang.NL
    ScrollV10(theme,if(nl)"Visueel Thema Studio" else "Visual Theme Studio",if(nl)"Kies de volledige look van de app voor alle leden." else "Choose the complete app look for all members."){
        v10Themes.forEach{candidate->
            PremiumCard(theme){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Column(Modifier.weight(1f)){Text(candidate.name,color=theme.accent2,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(when(candidate.id){V10ThemeId.ELITE_GOLD->"Black · gold · royal";V10ThemeId.ROYAL_CRIMSON->"Black · crimson · copper";V10ThemeId.MIDNIGHT_PLATINUM->"Midnight · platinum · ice";V10ThemeId.EMERALD_COMBAT->"Black · emerald · champagne"},color=theme.muted)}
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){ listOf(candidate.bg,candidate.panel,candidate.accent,candidate.accent2).forEach{col->Surface(Modifier.size(22.dp),shape=CircleShape,color=col,border=BorderStroke(1.dp,Color.White.copy(alpha=.25f))){} } }
                }
                Button(onClick={onTheme(candidate.id);store.putS("theme",candidate.id.name)},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Activeer thema" else "Activate theme")}
            }
        }
        PremiumCard(theme){Text(if(nl)"LIVE ACHTERGRONDREGELS" else "LIVE BACKGROUND RULES",color=theme.accent2,fontWeight=FontWeight.Bold);Text(if(nl)"Promotie/login: sterkere beweging. Training: lage opacity + donkere scrim. Tekst en timers blijven dominant." else "Promotion/login: stronger motion. Training: low opacity + dark scrim. Text and timers stay dominant.",color=theme.muted)}
    }
}

@Composable
private fun AccessControl(theme:V10Theme,lang:V10Lang,store:V10Store){
    val nl=lang==V10Lang.NL
    var tab by remember{mutableStateOf("plans")}
    var student by remember{mutableStateOf("Alex")}
    ScrollV10(theme,if(nl)"Toegang & Abonnementen" else "Access & Subscription Control",if(nl)"Bepaal exact wat elke leerling kan zien en gebruiken." else "Control exactly what every student can see and use."){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={tab="plans"},modifier=Modifier.weight(1f)){Text(if(nl)"Plannen" else "Plans")};OutlinedButton(onClick={tab="student"},modifier=Modifier.weight(1f)){Text(if(nl)"Per leerling" else "Per student")}}
        if(tab=="plans"){
            V10Plan.entries.forEach{plan->
                PremiumCard(theme){Text("RS ${plan.name}",color=theme.accent2,fontSize=19.sp,fontWeight=FontWeight.Black);Text(if(nl)"Standaard functies voor dit abonnement" else "Default features for this subscription",color=theme.muted)
                    studentFeaturesV10.forEach{f->
                        var enabled by remember(plan,f.id){mutableStateOf(store.b("plan_${plan.name}_${f.id}",planDefault(plan,f.id)))}
                        ToggleV10(theme,if(nl)f.nl else f.en,enabled){enabled=it;store.putB("plan_${plan.name}_${f.id}",it)}
                    }
                }
            }
        } else {
            PremiumCard(theme){
                Text(if(nl)"Selecteer leerling" else "Select student",color=theme.accent2,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Alex","Mila","Noah","Sara").forEach{s->FilterChip(selected=student==s,onClick={student=s},label={Text(s)})}}
                var plan by remember(student){mutableStateOf(runCatching{V10Plan.valueOf(store.s("student_plan_$student",V10Plan.PRO.name))}.getOrDefault(V10Plan.PRO))}
                Text(if(nl)"Abonnement" else "Subscription",color=theme.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){V10Plan.entries.forEach{p->FilterChip(selected=plan==p,onClick={plan=p;store.putS("student_plan_$student",p.name)},label={Text(p.name)})}}
            }
            PremiumCard(theme){
                Text(if(nl)"Individuele overrides" else "Individual overrides",color=theme.accent2,fontWeight=FontWeight.Bold)
                Text(if(nl)"Elke schakelaar overschrijft het abonnement voor alleen deze leerling." else "Each switch overrides the subscription for this student only.",color=theme.muted)
                studentFeaturesV10.forEach{f->
                    val current=allowedForStudent(store,student,f.id)
                    var enabled by remember(student,f.id){mutableStateOf(current)}
                    ToggleV10(theme,if(nl)f.nl else f.en,enabled){enabled=it;store.putB("has_override_${student}_${f.id}",true);store.putB("override_${student}_${f.id}",it)}
                }
                TextButton(onClick={studentFeaturesV10.forEach{f->store.putB("has_override_${student}_${f.id}",false)}}){Text(if(nl)"Herstel abonnementstandaard" else "Reset to plan defaults")}
            }
        }
    }
}

@Composable
private fun PaymentCenter(theme:V10Theme,lang:V10Lang,store:V10Store){
    val nl=lang==V10Lang.NL
    ScrollV10(theme,if(nl)"Betaalcentrum" else "Payment Center",if(nl)"Configureer tijdelijke en toekomstige betaalmethoden." else "Configure temporary and future payment methods."){
        PremiumCard(theme){
            Text(if(nl)"PRE-KVK MODUS" else "PRE-KVK MODE",color=theme.accent2,fontWeight=FontWeight.Black)
            Text(if(nl)"Gebruik alleen methoden die jouw bank/provider toestaat. Deze modus is geen manier om een wettelijke KVK-plicht te omzeilen." else "Only use methods allowed by your bank/provider. This mode is not a way to bypass a legal KVK registration duty.",color=theme.text)
            Text(if(nl)"Voor terugbetalingen of zeer beperkte niet-zakelijke ontvangst: gewone bankoverschrijving of een particulier betaalverzoek kan praktisch zijn. Voor reguliere betaalde app-abonnementen is zakelijke registratie/provider-onboarding meestal de juiste route." else "For reimbursements or very limited non-business receipts: ordinary bank transfer or a personal payment request can be practical. For regular paid app memberships, business registration/provider onboarding is usually the appropriate route.",color=theme.muted)
        }
        BusinessFields(theme,lang,store)
        PaymentMethodConfig(theme,lang,store,"bank","Bank transfer / SEPA",if(nl)"Geen app-processor; bankvoorwaarden kunnen gelden." else "No app processor; bank terms can still apply.",true,"IBAN / payment reference")
        PaymentMethodConfig(theme,lang,store,"cash","Cash",if(nl)"Handmatig registreren en reconciliëren." else "Manual recording and reconciliation.",true,"Optional note")
        PaymentMethodConfig(theme,lang,store,"personal_request",if(nl)"Particulier betaalverzoek" else "Personal payment request",if(nl)"Alleen als gebruik past binnen particuliere voorwaarden; niet bedoeld als commerciële checkout." else "Only when use fits personal-account terms; not intended as commercial checkout.",false,"Paste payment request URL")
        PaymentMethodConfig(theme,lang,store,"revolutme","Revolut.me",if(nl)"Persoonlijke link heeft limieten; geen vervanging voor merchant checkout." else "Personal link has limits; not a substitute for merchant checkout.",false,"https://revolut.me/...")
        PremiumCard(theme){Text(if(nl)"NA KVK / MERCHANT ONBOARDING" else "AFTER KVK / MERCHANT ONBOARDING",color=theme.accent2,fontWeight=FontWeight.Black);Text(if(nl)"Activeer hier later Tikkie Zakelijk, iDEAL/Wero via PSP, Revolut Business, kaarten/wallets en PayPal." else "Later activate Tikkie Business, iDEAL/Wero via PSP, Revolut Business, cards/wallets and PayPal here.",color=theme.muted)}
        PaymentMethodConfig(theme,lang,store,"tikkie_business","Tikkie Business",if(nl)"Zakelijke onboarding + actuele tarieven." else "Business onboarding + current pricing.",false,"Business payment link/API")
        PaymentMethodConfig(theme,lang,store,"ideal","iDEAL / Wero",if(nl)"Via gecontracteerde PSP/acquirer; kosten verschillen per provider." else "Via contracted PSP/acquirer; pricing varies by provider.",false,"PSP payment link")
        PaymentMethodConfig(theme,lang,store,"revolut_business","Revolut Business",if(nl)"Merchant account/payment links; transactiekosten kunnen gelden." else "Merchant account/payment links; processing fees may apply.",false,"Merchant payment link")
        PaymentMethodConfig(theme,lang,store,"cards","Cards / Apple Pay / Google Pay",if(nl)"Via PSP; verwerkingskosten." else "Via PSP; processing fees.",false,"Checkout/payment link")
        PaymentMethodConfig(theme,lang,store,"paypal","PayPal",if(nl)"Zakelijke betalingen hebben providerkosten." else "Commercial payments have provider fees.",false,"PayPal payment link")
    }
}

@Composable
private fun BusinessFields(theme:V10Theme,lang:V10Lang,store:V10Store){
    val nl=lang==V10Lang.NL
    PremiumCard(theme){
        Text(if(nl)"Betaal- en factuurgegevens" else "Payment & billing details",color=theme.accent2,fontWeight=FontWeight.Bold)
        listOf("display_name" to (if(nl)"Naam op betaling" else "Payment display name"),"iban" to "IBAN","billing_email" to (if(nl)"Factuur e-mail" else "Billing email"),"kvk" to "KvK (later)","vat" to "VAT / BTW (later)").forEach{(key,label)->
            var value by remember{mutableStateOf(store.s("pay_$key"))}
            OutlinedTextField(value,{value=it;store.putS("pay_$key",it)},label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true)
        }
    }
}

@Composable
private fun PaymentMethodConfig(theme:V10Theme,lang:V10Lang,store:V10Store,id:String,title:String,note:String,defaultOn:Boolean,placeholder:String){
    var enabled by remember{id;mutableStateOf(store.b("pay_enabled_$id",defaultOn))}
    var link by remember{id;mutableStateOf(store.s("pay_link_$id"))}
    PremiumCard(theme){
        ToggleV10(theme,title,enabled){enabled=it;store.putB("pay_enabled_$id",it)}
        Text(note,color=theme.muted,fontSize=11.sp)
        OutlinedTextField(link,{link=it;store.putS("pay_link_$id",it)},label={Text(placeholder)},modifier=Modifier.fillMaxWidth(),singleLine=true)
    }
}

@Composable
private fun MemberPayments(theme:V10Theme,lang:V10Lang,store:V10Store){
    val nl=lang==V10Lang.NL; val uri=LocalUriHandler.current
    ScrollV10(theme,if(nl)"Lidmaatschap & Betalen" else "Membership & Payments",if(nl)"Betaal met een door de trainer ingeschakelde methode." else "Pay using a method enabled by the trainer."){
        PremiumCard(theme){Text("RS PRO",color=theme.accent2,fontSize=28.sp,fontWeight=FontWeight.Black);Text("€49 / month",color=theme.text)}
        listOf("bank" to "Bank transfer / SEPA","cash" to "Cash","personal_request" to (if(nl)"Particulier betaalverzoek" else "Personal payment request"),"revolutme" to "Revolut.me","tikkie_business" to "Tikkie Business","ideal" to "iDEAL / Wero","revolut_business" to "Revolut Business","cards" to "Cards / Wallets","paypal" to "PayPal").forEach{(id,name)->
            if(store.b("pay_enabled_$id",id in setOf("bank","cash"))) PremiumCard(theme){Text(name,color=theme.accent2,fontWeight=FontWeight.Bold);val link=store.s("pay_link_$id");if(id=="bank") Text(store.s("pay_iban","Configure IBAN in Trainer Payment Center"),color=theme.text) else if(link.startsWith("http")) Button(onClick={runCatching{uri.openUri(link)}},modifier=Modifier.fillMaxWidth()){Text(if(nl)"Open betaling" else "Open payment")} else Text(if(nl)"Nog geen link ingesteld door trainer." else "No payment link configured by trainer yet.",color=theme.muted)}
        }
    }
}

@Composable
private fun AcademyV10(theme:V10Theme,lang:V10Lang,store:V10Store){val nl=lang==V10Lang.NL;ScrollV10(theme,"RS Academy",if(nl)"Techniekbibliotheek van basis naar beheersing." else "Technique library from fundamentals to mastery."){listOf("Jab Fundamentals","Roundhouse Kick","Defense & Counters","Footwork Flow","Combination Builder").forEachIndexed{i,n->var d by remember{mutableStateOf(store.b("v10lesson$i"))};PremiumCard(theme){Text(n,color=theme.accent2,fontWeight=FontWeight.Bold);Button(onClick={d=true;store.putB("v10lesson$i",true)},enabled=!d,modifier=Modifier.fillMaxWidth()){Text(if(d)"Completed ✓" else if(nl)"Markeer voltooid" else "Mark complete")}}}}}

@Composable
private fun ClassesV10(theme:V10Theme,lang:V10Lang,store:V10Store){val nl=lang==V10Lang.NL;ScrollV10(theme,if(nl)"Lessen & Events" else "Classes & Events",if(nl)"Boek je training." else "Book your training."){listOf("Technique & Pads" to "Today · 19:00","Fundamentals" to "Thu · 18:30","Advanced Sparring" to "Fri · 20:00").forEachIndexed{i,(n,t)->var booked by remember{mutableStateOf(store.b("v10class$i"))};PremiumCard(theme){Text(n,color=theme.accent2,fontWeight=FontWeight.Bold);Text(t,color=theme.muted);Button(onClick={booked=!booked;store.putB("v10class$i",booked)},modifier=Modifier.fillMaxWidth()){Text(if(booked)(if(nl)"Annuleer boeking" else "Cancel booking") else (if(nl)"Boek les" else "Book class"))}}}}}

@Composable
private fun SessionV10(theme:V10Theme,lang:V10Lang){val nl=lang==V10Lang.NL;var running by remember{mutableStateOf(false)};var sec by remember{mutableIntStateOf(120)};LaunchedEffect(running,sec){if(running&&sec>0){delay(1000);sec--}else if(sec==0)running=false};ScrollV10(theme,if(nl)"Sessie Speler" else "Session Player",if(nl)"Subtiele live achtergrond, maximale leesbaarheid." else "Subtle live background, maximum readability."){PremiumCard(theme){Text("%d:%02d".format(sec/60,sec%60),color=theme.accent2,fontSize=50.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=theme.text);Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){Text(if(running)"Pause" else "Start")};OutlinedButton(onClick={sec=120;running=false},modifier=Modifier.fillMaxWidth()){Text("Reset")}}}}

@Composable
private fun GenericV10(theme:V10Theme,lang:V10Lang,route:String,role:V10Role,store:V10Store,onRoute:(String)->Unit){
    val f=(if(role==V10Role.TRAINER)adminFeaturesV10 else studentFeaturesV10).firstOrNull{it.id==route};val title=if(lang==V10Lang.NL)f?.nl else f?.en
    ScrollV10(theme,title?:route,if(lang==V10Lang.NL)"Native RS-module met veilige mobiele layout." else "Native RS module with safe mobile layout."){
        repeat(3){i->PremiumCard(theme){Text(when(i){0->if(lang==V10Lang.NL)"Overzicht" else "Overview";1->if(lang==V10Lang.NL)"Acties" else "Actions";else->if(lang==V10Lang.NL)"Status" else "Status"},color=theme.accent2,fontWeight=FontWeight.Bold);Text(if(lang==V10Lang.NL)"Deze module is actief in de v0.10 preview en klaar voor backend-koppeling." else "This module is active in the v0.10 preview and ready for backend connection.",color=theme.muted)}}
        var saved by remember(route){mutableStateOf(store.b("generic_$route"))};Button(onClick={saved=true;store.putB("generic_$route",true)},enabled=!saved,modifier=Modifier.fillMaxWidth()){Text(if(saved)"Saved ✓" else if(lang==V10Lang.NL)"Previewactie opslaan" else "Save preview action")}
    }
}

@Composable
private fun ScrollV10(theme:V10Theme,title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=1.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=theme.accent2,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(subtitle,color=theme.muted);content();Spacer(Modifier.height(18.dp))}}

@Composable
private fun PremiumCard(theme:V10Theme,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,theme.accent2.copy(alpha=.28f)),colors=CardDefaults.cardColors(containerColor=theme.panel.copy(alpha=.96f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}

@Composable
private fun FeatureTile(theme:V10Theme,title:String,onClick:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,theme.accent.copy(alpha=.38f)),colors=CardDefaults.cardColors(containerColor=theme.panel.copy(alpha=.94f))){Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("♛",color=theme.accent2,fontSize=20.sp);Text(title,color=theme.accent2,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis);Text("Open module",color=theme.muted,fontSize=10.sp)}}}

@Composable
private fun MiniV10(theme:V10Theme,label:String,value:String,modifier:Modifier=Modifier){Surface(modifier=modifier,color=theme.panel2,shape=RoundedCornerShape(13.dp),border=BorderStroke(1.dp,theme.accent.copy(alpha=.25f))){Column(Modifier.padding(9.dp)){Text(label.uppercase(),color=theme.muted,fontSize=8.sp,maxLines=1);Text(value,color=theme.accent2,fontSize=18.sp,fontWeight=FontWeight.Black,maxLines=1)}}}

@Composable
private fun ToggleV10(theme:V10Theme,label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=theme.text,modifier=Modifier.weight(1f));Switch(value,onChange)}}

@Composable
private fun LangSwitchV10(theme:V10Theme,lang:V10Lang,onChange:(V10Lang)->Unit){Surface(shape=RoundedCornerShape(15.dp),color=theme.panel2,border=BorderStroke(1.dp,theme.accent.copy(alpha=.38f))){Row(Modifier.padding(2.dp)){Text("EN",Modifier.clickable{onChange(V10Lang.EN)}.padding(horizontal=8.dp,vertical=5.dp),color=if(lang==V10Lang.EN)theme.accent2 else theme.muted,fontWeight=FontWeight.Bold,fontSize=10.sp);Text("NL",Modifier.clickable{onChange(V10Lang.NL)}.padding(horizontal=8.dp,vertical=5.dp),color=if(lang==V10Lang.NL)theme.accent2 else theme.muted,fontWeight=FontWeight.Bold,fontSize=10.sp)}}}

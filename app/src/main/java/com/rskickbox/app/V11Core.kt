package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun RsKickboxV11App() {
    val context = LocalContext.current
    val store = remember { RsStore(context) }
    var role by remember { mutableStateOf<RsRole?>(null) }
    var route by remember { mutableStateOf("home") }
    var lang by remember { mutableStateOf(rsLangs.firstOrNull { it.code == store.s("lang", "en") } ?: rsLangs.first()) }
    var theme by remember { mutableStateOf(runCatching { RsTheme.valueOf(store.s("theme", "ELITE_GOLD")) }.getOrDefault(RsTheme.ELITE_GOLD)) }
    val c = paletteFor(theme)

    MaterialTheme(colorScheme = darkColorScheme(primary=c.bright,secondary=c.gold,background=c.bg,surface=c.panel,onBackground=c.text,onSurface=c.text)) {
        if (role == null) {
            RsLiveBackground(c, store, BgScope.LOGIN) {
                RsLogin(c, lang, { selected -> lang = selected; store.ps("lang", selected.code) }) { selectedRole ->
                    role = selectedRole
                    route = if (selectedRole == RsRole.TRAINER) "trainer" else "home"
                }
            }
        } else {
            val scope = if (role == RsRole.TRAINER) BgScope.TRAINER_TRAINING else BgScope.STUDENT_TRAINING
            RsLiveBackground(c, store, scope) {
                RsShell(c,role!!,lang,route,{selected->lang=selected;store.ps("lang",selected.code)},{route=it},{role=null}) {
                    when (route) {
                        "home", "trainer" -> RsDashboard(c, role!!, lang) { route = it }
                        "backgrounds" -> RsBackgroundStudio(c, store)
                        "themes" -> RsThemeStudio(c, theme) { selected -> theme=selected;store.ps("theme",selected.name) }
                        "voice" -> RsVoiceCoach(c, lang, store)
                        "session" -> RsSession(c)
                        "access" -> RsAccessControl(c, store)
                        "payments" -> RsPaymentCenter(c, store)
                        "members" -> RsMemberManager(c)
                        "classes" -> if (role==RsRole.TRAINER) RsClassManager(c) else RsStudentClasses(c)
                        "attendance" -> RsAttendance(c)
                        "invoices" -> RsInvoices(c)
                        "book" -> if (role==RsRole.TRAINER) RsBookManager(c) else RsTrainerBook(c)
                        "settings" -> if (role==RsRole.TRAINER) RsAdminSettings(c,store) else RsStudentSettings(c,store)
                        "academy" -> RsAcademy(c)
                        "progress" -> RsProgress(c)
                        "challenges" -> RsChallenges(c)
                        "fightcamp" -> RsFightCamp(c)
                        "finance" -> RsFinance(c)
                        "community" -> RsCommunity(c)
                        "media" -> RsTrainingMedia(c)
                        "techniques" -> RsTechniqueLibrary(c)
                        "home_training" -> RsHomeTraining(c)
                        "workout" -> RsWorkoutGenerator(c)
                        "badges" -> RsBadges(c)
                        "vault" -> RsKnowledgeVault(c)
                        "compare" -> RsTechniqueCompare(c)
                        "coachchat" -> RsPrivateCoachChat(c)
                        "events" -> RsEvents(c)
                        "notifications" -> RsNotificationsCenter(c)
                        "analytics" -> RsAnalytics(c)
                        "documents" -> RsDocuments(c)
                        "support" -> RsSupport(c)
                        "referrals" -> RsReferrals(c)
                        "schedule" -> RsTrainerSchedule(c)
                        "content" -> RsContentManager(c)
                        "notes" -> RsCoachNotes(c)
                        else -> RsGeneric(c, route, lang)
                    }
                }
            }
        }
    }
}

@Composable
private fun RsLogin(c:RsPalette,lang:RsLang,onLang:(RsLang)->Unit,onLogin:(RsRole)->Unit){
    var email by remember{mutableStateOf("alex@rskickbox.nl")};var pass by remember{mutableStateOf("preview123")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("♛ RS KICKBOX",color=c.bright,fontSize=31.sp,fontWeight=FontWeight.Black)
        RsLanguageButton(lang,onLang)
        Text("Premium cinematic kickboxing",color=c.text,style=MaterialTheme.typography.headlineMedium)
        Text("TRAIN · LEARN · CONNECT · GROW",color=c.muted,fontSize=11.sp)
        RsPanel(c){
            Text("MEMBER ACCESS",color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(pass,{pass=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            Button(onClick={onLogin(RsRole.STUDENT)},modifier=Modifier.fillMaxWidth()){Text("Student")}
            OutlinedButton(onClick={onLogin(RsRole.TRAINER)},modifier=Modifier.fillMaxWidth()){Text("Trainer / Admin")}
        }
    }
}

@Composable
private fun RsShell(c:RsPalette,role:RsRole,lang:RsLang,route:String,onLang:(RsLang)->Unit,onRoute:(String)->Unit,onLogout:()->Unit,content:@Composable()->Unit){
    val home=if(role==RsRole.TRAINER)"trainer" else "home"
    Column(Modifier.fillMaxSize().padding(9.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        RsPanel(c){
            Text("♛ RS KICKBOX",color=c.bright,fontSize=20.sp,fontWeight=FontWeight.Black)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                RsLanguageButton(lang,onLang)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    if(route!=home)OutlinedButton(onClick={onRoute(home)},contentPadding=PaddingValues(horizontal=10.dp)){Text("‹")}
                    OutlinedButton(onClick=onLogout,contentPadding=PaddingValues(horizontal=10.dp)){Text("Log out",fontSize=10.sp)}
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)){content()}
    }
}

@Composable
private fun RsDashboard(c:RsPalette,role:RsRole,lang:RsLang,onRoute:(String)->Unit){
    val tiles=if(role==RsRole.TRAINER) listOf(
        "themes" to "Visual Theme Studio","backgrounds" to "Background Studio","voice" to "AI Voice Coach","session" to "Trainer Session",
        "access" to "Access & Subscriptions","payments" to "Payment Center","members" to "Student Manager","classes" to "Class Manager",
        "attendance" to "Attendance","invoices" to "Invoices","notifications" to "Notifications","analytics" to "Analytics",
        "schedule" to "Trainer Schedule","content" to "Content Manager","notes" to "Coach Notes","documents" to "Documents & Waivers",
        "referrals" to "Referrals","support" to "Support","book" to "Book Manager","settings" to "Settings"
    ) else listOf(
        "voice" to "AI Voice Coach","session" to "Session Player","academy" to "RS Academy","techniques" to "Technique Library",
        "home_training" to "Home Training","workout" to "Workout Generator","classes" to "Classes & Events","events" to "RS Events",
        "progress" to "Progress","challenges" to "Challenges","badges" to "Badges","fightcamp" to "Fight Camp",
        "compare" to "Technique Compare","vault" to "Knowledge Vault","coachchat" to "Private Coach Chat","finance" to "Membership & Payments",
        "book" to "Trainer Book","community" to "Community","media" to "Training Media","settings" to "Settings"
    )
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        RsPanel(c){Text(if(role==RsRole.TRAINER)"TRAINER CONTROL CENTER" else "RS LIVE DASHBOARD",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp);Text("${lang.name} · ${if(role==RsRole.TRAINER)"Trainer" else "Student"}",color=c.muted)}
        LazyVerticalGrid(columns=GridCells.Adaptive(128.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=18.dp)){items(tiles){(id,label)->RsTile(c,label){onRoute(id)}}}
    }
}

@Composable
private fun RsSession(c:RsPalette){
    var running by remember{mutableStateOf(false)};var sec by remember{mutableIntStateOf(120)}
    LaunchedEffect(running,sec){if(running&&sec>0){delay(1000);sec--}else if(sec==0)running=false}
    RsScroll(c,"Session Player","Live cinematic training background stays darker so timer and technique cues remain fully readable."){
        RsPanel(c){Text("%d:%02d".format(sec/60,sec%60),color=c.bright,fontSize=50.sp,fontWeight=FontWeight.Black);Text("Jab · Cross · Low Kick",color=c.text);Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){Text(if(running)"Pause" else "Start")};OutlinedButton(onClick={sec=120;running=false},modifier=Modifier.fillMaxWidth()){Text("Reset")}}
    }
}

@Composable
private fun RsGeneric(c:RsPalette,route:String,lang:RsLang){RsScroll(c,route.replaceFirstChar{it.uppercase()},"Native RS module · ${lang.name}"){repeat(3){i->RsPanel(c){Text(listOf("Overview","Actions","Status")[i],color=c.bright,fontWeight=FontWeight.Bold);Text("Safe mobile layout with the selected cinematic background system.",color=c.muted)}}}}

@Composable
private fun RsLanguageButton(current:RsLang,onSelect:(RsLang)->Unit){
    var open by remember{mutableStateOf(false)}
    Box{OutlinedButton(onClick={open=true},contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)){Text("🌐 ${current.name}",fontSize=11.sp)};DropdownMenu(expanded=open,onDismissRequest={open=false}){rsLangs.forEach{lang->DropdownMenuItem(text={Text(lang.name)},onClick={onSelect(lang);open=false})}}}
}

@Composable
fun RsScroll(c:RsPalette,title:String,sub:String,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=1.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=c.bright,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black);Text(sub,color=c.muted);content();Spacer(Modifier.height(20.dp))}}

@Composable
fun RsPanel(c:RsPalette,content:@Composable ColumnScope.()->Unit){Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.bright.copy(alpha=.28f)),colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.95f))){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}

@Composable
private fun RsTile(c:RsPalette,title:String,onClick:()->Unit){Card(modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.4f)),colors=CardDefaults.cardColors(containerColor=c.panel.copy(alpha=.93f))){Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("♛",color=c.bright,fontSize=20.sp);Text(title,color=c.bright,fontWeight=FontWeight.Bold,maxLines=2)}}}
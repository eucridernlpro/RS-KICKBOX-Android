package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsThemeStudio(c:RsPalette,theme:RsTheme,onTheme:(RsTheme)->Unit){
    RsScroll(c,"Visual Theme Studio","Trainer control for the complete app visual identity. Changes apply instantly to student and trainer areas."){
        RsTheme.entries.forEach{t->
            val p=paletteFor(t)
            RsPanel(c){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column{Text(t.name.replace('_',' '),color=c.bright,fontWeight=FontWeight.Bold);Text(if(theme==t)"ACTIVE" else "Premium preset",color=c.muted,fontSize=11.sp)}
                    Button(onClick={onTheme(t)},enabled=theme!=t){Text(if(theme==t)"Selected" else "Apply")}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(p.bg,p.panel,p.gold,p.bright).forEach{x->Surface(modifier=Modifier.size(34.dp),shape=MaterialTheme.shapes.small,color=x){}}}
            }
        }
    }
}

@Composable
fun RsAccessControl(c:RsPalette,s:RsStore){
    val features=listOf("AI Voice Coach","RS Academy","Session Player","Technique Library","Community","Fight Camp","Challenges","Trainer Book","Training Media","Private Coach Chat")
    var plan by remember{mutableStateOf("PRO")}
    RsScroll(c,"Access & Subscriptions","Control what every subscription and individual student can see and use."){
        RsPanel(c){
            Text("SUBSCRIPTION MATRIX",color=c.bright,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("BASIC","PRO","ELITE").forEach{p->FilterChip(selected=plan==p,onClick={plan=p},label={Text(p)})}}
            features.forEachIndexed{i,f->
                val key="access_${plan}_$i";var on by remember(plan){mutableStateOf(s.b(key,plan!="BASIC"||i<4))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(f,color=c.text,modifier=Modifier.weight(1f));Switch(on,{on=it;s.pb(key,it)})}
            }
        }
        RsPanel(c){
            Text("INDIVIDUAL STUDENT OVERRIDES",color=c.bright,fontWeight=FontWeight.Bold)
            listOf("Alex de Vries · PRO","Sofia Martins · ELITE","Noah Jansen · BASIC").forEachIndexed{i,n->
                var custom by remember{mutableStateOf(s.b("student_override_$i",false))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(n,color=c.text);Text(if(custom)"Custom permissions active" else "Uses subscription defaults",color=c.muted,fontSize=11.sp)};Switch(custom,{custom=it;s.pb("student_override_$i",it)})}
            }
        }
    }
}

@Composable
fun RsPaymentCenter(c:RsPalette,s:RsStore){
    val methods=listOf("Bank transfer","Manual payment request","Tikkie / payment link","iDEAL / Wero merchant","Revolut Pay","Cards / Apple Pay / Google Pay","PayPal","Cash at club")
    RsScroll(c,"Payment Center","Configure accepted methods, payment instructions and operational status. Provider onboarding remains separate from this preview."){
        RsPanel(c){Text("BUSINESS PAYMENT SETTINGS",color=c.bright,fontWeight=FontWeight.Bold);Text("Temporary/manual methods can be enabled before merchant onboarding. Commercial use still needs to follow Dutch registration, tax and provider rules.",color=c.muted)}
        methods.forEachIndexed{i,m->
            var on by remember{mutableStateOf(s.b("pay_$i",i<2||i==7))}
            RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(m,color=c.text,fontWeight=FontWeight.Bold);Text(if(i in listOf(0,1,7))"Manual / direct method" else "Provider integration / onboarding required",color=c.muted,fontSize=11.sp)};Switch(on,{on=it;s.pb("pay_$i",it)})}}
        }
        RsPanel(c){
            var account by remember{mutableStateOf(s.s("pay_account","NL00 BANK 0000 0000 00"))};var note by remember{mutableStateOf(s.s("pay_note","RS KICKBOX membership"))}
            OutlinedTextField(account,{account=it;s.ps("pay_account",it)},label={Text("Bank / account details")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(note,{note=it;s.ps("pay_note",it)},label={Text("Payment reference / instructions")},modifier=Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun RsMemberManager(c:RsPalette){
    RsScroll(c,"Student Manager","Preview member administration with status, subscription, attendance and quick actions."){
        listOf("Alex de Vries|PRO|86%|Active","Sofia Martins|ELITE|94%|Active","Noah Jansen|BASIC|68%|Review","Mila Costa|PRO|91%|Active").forEach{row->val x=row.split('|');RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text("${x[1]} · Attendance ${x[2]} · ${x[3]}",color=c.muted);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={}){Text("Profile")};OutlinedButton(onClick={}){Text("Message")}}}}
    }
}

@Composable
fun RsClassManager(c:RsPalette){
    RsScroll(c,"Class Manager","Create and manage training sessions, capacity and booking state."){
        listOf("Kickboxing Fundamentals · 18:00 · 14/18","Advanced Pads · 19:15 · 10/12","Sparring Lab · 20:30 · 8/10").forEach{item->RsPanel(c){Text(item,color=c.bright,fontWeight=FontWeight.Bold);LinearProgressIndicator(progress={.72f},modifier=Modifier.fillMaxWidth());Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={}){Text("Edit")};OutlinedButton(onClick={}){Text("Attendance")}}}}
        Button(onClick={},modifier=Modifier.fillMaxWidth()){Text("+ Create new class")}
    }
}

@Composable
fun RsAttendance(c:RsPalette){
    RsScroll(c,"Attendance","Live preview of check-in management and class attendance."){
        RsPanel(c){Text("Tonight · Fundamentals",color=c.bright,fontWeight=FontWeight.Bold);Text("12 checked in · 2 absent · 4 spots open",color=c.muted)}
        listOf("Alex de Vries","Sofia Martins","Noah Jansen","Mila Costa").forEachIndexed{i,n->var present by remember{mutableStateOf(i!=2)};RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(n,color=c.text);Checkbox(present,{present=it})}}}
    }
}

@Composable
fun RsInvoices(c:RsPalette){
    RsScroll(c,"Invoices & Revenue","Administrative preview for payment status and monthly club income."){
        RsPanel(c){Text("SEPTEMBER",color=c.bright,fontWeight=FontWeight.Bold);Text("€4,850 collected · €392 pending",color=c.text,fontSize=22.sp);LinearProgressIndicator(progress={.91f},modifier=Modifier.fillMaxWidth())}
        listOf("INV-26091 · Alex · €49 · Paid","INV-26092 · Sofia · €69 · Paid","INV-26093 · Noah · €29 · Pending").forEach{RsPanel(c){Text(it,color=c.text)}}
    }
}

@Composable
fun RsBookManager(c:RsPalette){
    RsScroll(c,"Trainer Book Manager","Manage how Van Stilte Naar Strijd is presented inside RS KICKBOX."){
        RsPanel(c){Text("VAN STILTE NAAR STRIJD",color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black);Text("Kickboksen, karakter en de weg van basis naar beheersing",color=c.text);Text("Digital companion enabled · premium member extras active",color=c.muted)}
        listOf("Featured on student dashboard","Show chapter companion content","Enable book-only exercises","Promote Amazon / store link").forEachIndexed{i,t->var on by remember{mutableStateOf(i<3)};RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(t,color=c.text,modifier=Modifier.weight(1f));Switch(on,{on=it})}}}
    }
}

@Composable
fun RsAdminSettings(c:RsPalette,s:RsStore){
    RsScroll(c,"App Settings & Operations","Core trainer controls for notifications, privacy, maintenance and release readiness."){
        listOf("Push notifications","Booking reminders","Payment reminders","Community moderation","Maintenance banner").forEachIndexed{i,t->var on by remember{mutableStateOf(s.b("admin_setting_$i",i<4))};RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(t,color=c.text);Switch(on,{on=it;s.pb("admin_setting_$i",it)})}}}
        RsPanel(c){Text("PRODUCTION CHECKLIST",color=c.bright,fontWeight=FontWeight.Bold);Text("Secure auth · server permissions · cloud media · push delivery · account deletion · privacy policy · signed AAB · device testing",color=c.muted)}
    }
}
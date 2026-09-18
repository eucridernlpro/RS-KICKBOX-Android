package com.rskickbox.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
    val features=listOf("AI Technique Coach","RS Academy","Session Player","Technique Library","Community","Fight Camp","Challenges","Trainer Book","Training Media","Private Coach Chat")
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
fun RsMemberManager(c:RsPalette,s:RsStore?=null,lang:RsLang=rsLangs.first()){
    val context=LocalContext.current
    val localStore=remember{RsStore(context)}
    val store=s?:localStore
    var revision by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var name by remember{mutableStateOf("")}
    var email by remember{mutableStateOf("")}
    var plan by remember{mutableStateOf("PRO")}
    var status by remember{mutableStateOf("")}
    var expandedQrStudentId by remember{mutableStateOf<String?>(null)}
    var pendingDeleteStudentId by remember{mutableStateOf<String?>(null)}
    var studentSearch by remember{mutableStateOf("")}
    var visibleStudentCount by remember{mutableIntStateOf(20)}
    val students=remember(revision){rsLoadStudentsV33(store)}
    val activeCount=students.count{it.active}
    val filteredStudents=remember(students,studentSearch){
        val q=studentSearch.trim()
        if(q.isBlank())students else students.filter{
            it.name.contains(q,ignoreCase=true) ||
            it.email.contains(q,ignoreCase=true) ||
            it.plan.contains(q,ignoreCase=true)
        }
    }
    LaunchedEffect(studentSearch){visibleStudentCount=20}
    val visibleStudents=filteredStudents.take(visibleStudentCount)

    fun save(items:List<RsStudentAccountV33>){
        rsSaveStudentsV33(store,items)
        revision++
    }

    RsScroll(c,rsEnrollmentT(lang,"student_manager"),rsEnrollmentT(lang,"student_manager_sub")){
        RsPanel(c){
            Text(rsEnrollmentT(lang,"student_capacity"),color=c.bright,fontWeight=FontWeight.Black)
            Text("$activeCount / $RS_STUDENT_LIMIT_V33 ${rsEnrollmentT(lang,"active_accounts")}",color=c.text,fontSize=20.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={activeCount.toFloat()/RS_STUDENT_LIMIT_V33},modifier=Modifier.fillMaxWidth())
            Button(
                onClick={showCreate=!showCreate},
                enabled=activeCount<RS_STUDENT_LIMIT_V33,
                modifier=Modifier.fillMaxWidth()
            ){Text(if(showCreate)rsEnrollmentT(lang,"close_new") else rsEnrollmentT(lang,"create_student"))}
        }

        if(showCreate)RsPanel(c){
            Text(rsEnrollmentT(lang,"new_student"),color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(name,{name=it},label={Text(rsEnrollmentT(lang,"student_name"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(email,{email=it},label={Text(rsEnrollmentT(lang,"email"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("BASIC","PRO","ELITE").forEach{p->
                    FilterChip(selected=plan==p,onClick={plan=p},label={Text(p)},modifier=Modifier.weight(1f))
                }
            }
            Text(rsEnrollMsg(lang,"secure_hint"),color=c.muted,fontSize=10.sp)
            Button(
                onClick={
                    val cleanEmail=email.trim()
                    when{
                        name.trim().isBlank()->status=rsEnrollMsg(lang,"enter_name")
                        !cleanEmail.contains("@")->status=rsEnrollMsg(lang,"invalid_email")
                        students.any{it.email.equals(cleanEmail,true)}->status=rsEnrollMsg(lang,"email_exists")
                        activeCount>=RS_STUDENT_LIMIT_V33->status=rsEnrollMsg(lang,"limit_reached")
                        else->{
                            val account=RsStudentAccountV33(
                                id=java.util.UUID.randomUUID().toString(),
                                name=name.trim(),
                                email=cleanEmail,
                                plan=plan,
                                activationCode=rsNewActivationCodeV33(),
                                active=true,
                                createdAt=System.currentTimeMillis()
                            )
                            save(listOf(account)+students)
                            name=""
                            email=""
                            plan="PRO"
                            showCreate=false
                            status=rsEnrollMsg(lang,"created")
                        }
                    }
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsEnrollmentT(lang,"create_qr"))}
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        if(students.isEmpty())RsPanel(c){
            Text(rsEnrollMsg(lang,"no_accounts_title"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsEnrollMsg(lang,"no_accounts_desc"),color=c.muted)
        } else {
            RsPanel(c){
                OutlinedTextField(
                    value=studentSearch,
                    onValueChange={studentSearch=it},
                    label={Text(rsEnrollmentT(lang,"search_students"))},
                    supportingText={Text("${filteredStudents.size} ${rsEnrollmentT(lang,"shown")} · ${students.size} ${rsEnrollmentT(lang,"total")}")},
                    singleLine=true,
                    modifier=Modifier.fillMaxWidth()
                )
            }
        }

        visibleStudents.forEach{student->
            RsPanel(c){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(student.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                        Text(student.email,color=c.text,fontSize=11.sp)
                        Text("${student.plan} · ${if(student.active)rsEnrollmentT(lang,"active") else rsEnrollmentT(lang,"inactive")}",color=c.muted,fontSize=10.sp)
                    }
                    Switch(
                        checked=student.active,
                        onCheckedChange={on->
                            if(on && activeCount>=RS_STUDENT_LIMIT_V33){
                                status=rsEnrollMsg(lang,"cannot_activate")
                            }else{
                                save(students.map{if(it.id==student.id)it.copy(active=on) else it})
                                status=if(on)rsEnrollMsg(lang,"activated",student.name) else rsEnrollMsg(lang,"deactivated",student.name)
                            }
                        }
                    )
                }

                val qrExpanded=expandedQrStudentId==student.id
                OutlinedButton(
                    onClick={expandedQrStudentId=if(qrExpanded)null else student.id},
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(qrExpanded)rsEnrollmentT(lang,"hide_qr") else rsEnrollmentT(lang,"show_qr"))}
                if(qrExpanded){
                    val qr=remember(student.id,student.activationCode){
                        rsQrBitmapV33(rsInvitePayloadV33(student),720)
                    }
                    Surface(
                        shape=MaterialTheme.shapes.large,
                        color=androidx.compose.ui.graphics.Color.White,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Image(
                            bitmap=qr.asImageBitmap(),
                            contentDescription="RS KICKBOX student invitation QR",
                            modifier=Modifier.fillMaxWidth().aspectRatio(1f).padding(14.dp)
                        )
                    }
                }
                Text("${rsEnrollMsg(lang,"activation_label")}: ${student.activationCode}",color=c.bright,fontWeight=FontWeight.Bold)
                Text(rsEnrollMsg(lang,"store_ready"),color=c.muted,fontSize=10.sp)

                Button(
                    onClick={rsShareStudentInviteV33(context,student)},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsEnrollmentT(lang,"share_invite"))}
                OutlinedButton(
                    onClick={rsSharePlayStoreLinkV33(context)},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsEnrollmentT(lang,"share_store"))}

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    OutlinedButton(
                        onClick={
                            val newCode=rsNewActivationCodeV33()
                            save(students.map{if(it.id==student.id)it.copy(activationCode=newCode) else it})
                            status=rsEnrollMsg(lang,"new_qr_ready",student.name)
                        },
                        modifier=Modifier.weight(1f)
                    ){Text(rsEnrollmentT(lang,"new_qr"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={
                            if(pendingDeleteStudentId==student.id){
                                save(students.filterNot{it.id==student.id})
                                if(expandedQrStudentId==student.id)expandedQrStudentId=null
                                pendingDeleteStudentId=null
                                status=rsEnrollMsg(lang,"deleted",student.name)
                            }else{
                                pendingDeleteStudentId=student.id
                                status=rsEnrollMsg(lang,"delete_confirm",student.name)
                            }
                        },
                        modifier=Modifier.weight(1f)
                    ){
                        Text(
                            if(pendingDeleteStudentId==student.id)rsEnrollmentT(lang,"confirm") else rsEnrollmentT(lang,"delete"),
                            fontSize=10.sp
                        )
                    }
                }
            }
        }

        if(visibleStudents.size<filteredStudents.size){
            OutlinedButton(
                onClick={visibleStudentCount=(visibleStudentCount+20).coerceAtMost(filteredStudents.size)},
                modifier=Modifier.fillMaxWidth()
            ){
                Text("${rsEnrollmentT(lang,"show_more")} · ${visibleStudents.size} / ${filteredStudents.size}",fontSize=11.sp)
            }
        }

        RsPanel(c){
            Text(rsEnrollmentT(lang,"security_note"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsEnrollMsg(lang,"security_body"),color=c.muted,fontSize=10.sp)
        }
        if(status.isNotBlank())Text(status,color=c.bright,fontSize=10.sp)
    }
}

@Composable
fun RsClassManager(c:RsPalette){
    var status by remember{mutableStateOf("")}
    var created by remember{mutableStateOf(false)}
    RsScroll(c,"Class Manager","Create and manage training sessions, capacity and booking state."){
        listOf("Kickboxing Fundamentals · 18:00 · 14/18","Advanced Pads · 19:15 · 10/12","Sparring Lab · 20:30 · 8/10").forEach{item->
            RsPanel(c){
                Text(item,color=c.bright,fontWeight=FontWeight.Bold)
                LinearProgressIndicator(progress={.72f},modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(onClick={status="Edit preview opened for $item"}){Text("Edit")}
                    OutlinedButton(onClick={status="Attendance preview opened for $item"}){Text("Attendance")}
                }
            }
        }
        Button(onClick={created=!created;status=if(created)"New-class form preview opened." else ""},modifier=Modifier.fillMaxWidth()){Text(if(created)"Close new-class form" else "+ Create new class")}
        if(created)RsPanel(c){
            Text("NEW CLASS PREVIEW",color=c.bright,fontWeight=FontWeight.Bold)
            Text("Title · time · capacity · level · booking state",color=c.muted)
        }
        if(status.isNotBlank())Text(status,color=c.muted)
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
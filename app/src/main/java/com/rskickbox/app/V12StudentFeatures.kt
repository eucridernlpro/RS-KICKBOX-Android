package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@Composable
fun RsAcademy(c:RsPalette){
    RsScroll(c,"RS Academy","Structured technique learning with premium lesson previews and progress tracking."){
        listOf("Jab Fundamentals|92%|Guard, extension, recovery and timing","Roundhouse Kick|74%|Hip rotation, shin line and balance","Defense & Counters|61%|Shell, parry, slip and return","Clinching Basics|48%|Posture, frames and safe knee entries","Combo Builder|35%|Flow, rhythm and exit angles","Footwork Flow|28%|Range, pivots and stance recovery").forEach{row->
            val x=row.split('|')
            var open by remember(row){mutableStateOf(false)}
            RsPanel(c){
                Text(x[0],color=c.bright,fontWeight=FontWeight.Bold)
                Text(x[2],color=c.muted)
                LinearProgressIndicator(progress={x[1].removeSuffix("%").toFloat()/100f},modifier=Modifier.fillMaxWidth())
                Text("Progress ${x[1]}",color=c.text)
                Button(onClick={open=!open},modifier=Modifier.fillMaxWidth()){Text(if(open)"Close lesson" else "Open lesson")}
                if(open)Text("Lesson preview · stance check · controlled repetitions · coach cues · recovery drill",color=c.text)
            }
        }
    }
}

@Composable
fun RsStudentClasses(c:RsPalette){
    RsScroll(c,"Classes & Events","Book club sessions and view live capacity."){
        listOf("Today · 18:00 · Fundamentals · 14/18","Today · 19:15 · Advanced Pads · 10/12","Fri · 20:30 · Sparring Lab · 8/10","Sat · 10:00 · Conditioning · 16/20").forEachIndexed{i,t->var booked by remember{mutableStateOf(i==0)};RsPanel(c){Text(t,color=c.bright,fontWeight=FontWeight.Bold);Text(if(booked)"You are booked" else "Places available",color=c.muted);Button(onClick={booked=!booked},modifier=Modifier.fillMaxWidth()){Text(if(booked)"Cancel booking" else "Book class")}}}
    }
}

@Composable
fun RsProgress(c:RsPalette){
    RsScroll(c,"Progress & Profile","A complete view of training consistency and technique development."){
        RsPanel(c){Text("LEVEL 3 · 12,480 XP",color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black);Text("18 day training streak",color=c.text);LinearProgressIndicator(progress={.72f},modifier=Modifier.fillMaxWidth())}
        listOf("Punches" to .84f,"Kicks" to .71f,"Defense" to .67f,"Footwork" to .62f,"Combinations" to .76f,"Conditioning" to .69f).forEach{(n,v)->RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(n,color=c.text);Text("${(v*100).toInt()}%",color=c.bright)};LinearProgressIndicator(progress={v},modifier=Modifier.fillMaxWidth())}}
    }
}

@Composable
fun RsChallenges(c:RsPalette){
    RsScroll(c,"Challenges","Motivation and achievements designed around safe, consistent training."){
        listOf("3 Session Week|Complete three training sessions|250 XP","Defense Focus|Finish two defense lessons|180 XP","Footwork Flow|Complete the footwork session|150 XP","Consistency|Train on five different days|400 XP").forEachIndexed{i,row->val x=row.split('|');var done by remember{mutableStateOf(i==1)};RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.muted);Text(x[2],color=c.text);Button(onClick={done=!done},modifier=Modifier.fillMaxWidth()){Text(if(done)"✓ Completed" else "Mark complete")}}}
    }
}

@Composable
fun RsFightCamp(c:RsPalette){
    RsScroll(c,"Fight Camp","Eight-week structured camp focused on technical sharpness, conditioning, controlled sparring and recovery."){
        RsPanel(c){Text("WEEK 3 OF 8",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp);LinearProgressIndicator(progress={3f/8f},modifier=Modifier.fillMaxWidth());Text("Current focus · technical quality under fatigue",color=c.muted)}
        listOf("Technical sharpness|4 sessions planned","Conditioning|3 sessions planned","Controlled sparring|2 sessions planned","Recovery & mindset|Daily check-in").forEach{row->
            val x=row.split('|')
            var open by remember(row){mutableStateOf(false)}
            RsPanel(c){
                Text(x[0],color=c.bright,fontWeight=FontWeight.Bold)
                Text(x[1],color=c.muted)
                OutlinedButton(onClick={open=!open},modifier=Modifier.fillMaxWidth()){Text(if(open)"Close plan" else "Open plan")}
                if(open)Text("Preview plan · technical rounds · recovery target · coach check-in",color=c.text)
            }
        }
    }
}

@Composable
fun RsFinance(c:RsPalette){
    RsScroll(c,"Membership & Payments","Member view of subscription, payment status and enabled payment methods."){
        RsPanel(c){Text("RS PRO",color=c.bright,fontSize=26.sp,fontWeight=FontWeight.Black);Text("€49 / month · Active",color=c.text);Text("Next renewal · 01 Oct 2026",color=c.muted)}
        var selectedMethod by remember{mutableStateOf("")}
        listOf("Bank transfer","Manual payment request","Cash at club").forEach{method->
            RsPanel(c){
                Text(method,color=c.text,fontWeight=FontWeight.Bold)
                OutlinedButton(onClick={selectedMethod=method},modifier=Modifier.fillMaxWidth()){Text(if(selectedMethod==method)"✓ Selected" else "Use this method")}
                if(selectedMethod==method)Text("Local preview selection. Production payment processing is not connected yet.",color=c.muted,fontSize=10.sp)
            }
        }
        RsPanel(c){Text("PAYMENT HISTORY",color=c.bright,fontWeight=FontWeight.Bold);Text("Sep 2026 · €49 · Paid\nAug 2026 · €49 · Paid\nJul 2026 · €49 · Paid",color=c.muted)}
    }
}

@Composable
fun RsTrainerBook(c:RsPalette){
    val context=LocalContext.current
    var status by remember{mutableStateOf("")}
    RsScroll(c,"Trainer Book","The digital companion to Van Stilte Naar Strijd."){
        RsPanel(c){
            Text("VAN STILTE NAAR STRIJD",color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)
            Text("Kickboksen, karakter en de weg van basis naar beheersing",color=c.text)
            Text("Coach philosophy · fundamentals · technique · conditioning · recovery · fight preparation · discipline",color=c.muted)
            Button(onClick={status=if(status.isBlank())"Companion reading mode opened." else ""},modifier=Modifier.fillMaxWidth()){Text(if(status.isBlank())"Continue reading" else "Close reading")}
            OutlinedButton(onClick={
                runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.amazon.nl/s?k=Van+Stilte+Naar+Strijd")))}
                    .onFailure{status="Could not open Amazon on this device."}
            },modifier=Modifier.fillMaxWidth()){Text("Find on Amazon.nl")}
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        RsPanel(c){Text("APP EXTRAS",color=c.bright,fontWeight=FontWeight.Bold);Text("Technique companion cards · training prompts · member-only exercises · progress connections",color=c.muted)}
    }
}

@Composable
fun RsCommunity(c:RsPalette){
    RsScroll(c,"Community","Club feed and training communication preview."){
        listOf("Coach Ricardo|Great fundamentals session tonight. Focus this week: clean recovery after every combination.|14","Sofia|Finished the Roundhouse Kick lesson. The balance drill really helped.|9").forEach{row->val x=row.split('|');var liked by remember{mutableStateOf(false)};RsPanel(c){Text(x[0],color=c.bright,fontWeight=FontWeight.Bold);Text(x[1],color=c.text);OutlinedButton(onClick={liked=!liked}){Text(if(liked)"♥ ${x[2].toInt()+1}" else "♡ ${x[2]}")}}}
        RsPanel(c){var msg by remember{mutableStateOf("")};OutlinedTextField(msg,{msg=it},label={Text("Write to the club")},modifier=Modifier.fillMaxWidth());Button(onClick={msg=""},enabled=msg.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Send")}}
    }
}

@Composable
fun RsTrainingMedia(c:RsPalette){
    RsScroll(c,"Training Media","Preview library for coach videos, drills and member uploads."){
        listOf("Coach Demo · Jab Recovery · 02:14","Padwork Flow · 5-count combination · 03:46","Defense Drill · Slip & return · 04:10","Mobility · Post-training recovery · 06:20").forEach{item->
            var playing by remember(item){mutableStateOf(false)}
            RsPanel(c){
                Text(item,color=c.bright,fontWeight=FontWeight.Bold)
                Text("HD training media preview",color=c.muted)
                Button(onClick={playing=!playing},modifier=Modifier.fillMaxWidth()){Text(if(playing)"■ Close preview" else "▶ Play")}
                if(playing)Text("Local media preview opened.",color=c.text)
            }
        }
    }
}

@Composable
fun RsStudentSettings(c:RsPalette,s:RsStore){
    RsScroll(c,"Settings & Privacy","Personal app preferences and privacy controls."){
        listOf("Push notifications","Booking reminders","Training reminders","Private profile","Voice coach auto-speak").forEachIndexed{i,t->var on by remember{mutableStateOf(s.b("student_setting_$i",i<3||i==4))};RsPanel(c){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(t,color=c.text,modifier=Modifier.weight(1f));Switch(on,{on=it;s.pb("student_setting_$i",it)})}}}
        var dataStatus by remember{mutableStateOf("")}
        RsPanel(c){
            Text("YOUR DATA",color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedButton(onClick={dataStatus="Data export request recorded locally."},modifier=Modifier.fillMaxWidth()){Text("Download my data")}
            OutlinedButton(onClick={dataStatus="Account deletion request recorded locally."},modifier=Modifier.fillMaxWidth()){Text("Request account deletion")}
            if(dataStatus.isNotBlank())Text(dataStatus,color=c.muted,fontSize=10.sp)
        }
    }
}
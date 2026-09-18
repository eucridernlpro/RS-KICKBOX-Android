package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

data class RsClubClassV38(
    val id:String,
    val title:String,
    val dayLabel:String,
    val timeLabel:String,
    val level:String,
    val capacity:Int,
    val booked:Int,
    val bookingOpen:Boolean,
    val active:Boolean
)

private val rsSeedClassesV38=listOf(
    RsClubClassV38("fundamentals","Kickboxing Fundamentals","Today","18:00","ALL LEVELS",18,14,true,true),
    RsClubClassV38("advanced_pads","Advanced Pads","Today","19:15","ADVANCED",12,10,true,true),
    RsClubClassV38("sparring","Sparring Lab","Fri","20:30","ADVANCED",10,8,true,true),
    RsClubClassV38("conditioning","Conditioning","Sat","10:00","ALL LEVELS",20,16,true,true)
)

private fun rsEncodeClassesV38(items:List<RsClubClassV38>):String{
    val arr=JSONArray()
    items.forEach{c->
        arr.put(JSONObject().apply{
            put("id",c.id)
            put("title",c.title)
            put("day",c.dayLabel)
            put("time",c.timeLabel)
            put("level",c.level)
            put("capacity",c.capacity)
            put("booked",c.booked)
            put("bookingOpen",c.bookingOpen)
            put("active",c.active)
        })
    }
    return arr.toString()
}

private fun rsDecodeClassesV38(raw:String):List<RsClubClassV38>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsClubClassV38(
                    o.optString("id"),
                    o.optString("title"),
                    o.optString("day"),
                    o.optString("time"),
                    o.optString("level","ALL LEVELS"),
                    o.optInt("capacity",16).coerceAtLeast(1),
                    o.optInt("booked",0).coerceAtLeast(0),
                    o.optBoolean("bookingOpen",true),
                    o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

fun rsLoadClassesV38(store:RsStore):List<RsClubClassV38>{
    val raw=store.s("club_classes_v38","")
    if(raw.isBlank()){
        store.ps("club_classes_v38",rsEncodeClassesV38(rsSeedClassesV38))
        return rsSeedClassesV38
    }
    return rsDecodeClassesV38(raw).ifEmpty{rsSeedClassesV38}
}

fun rsSaveClassesV38(store:RsStore,items:List<RsClubClassV38>)=
    store.ps("club_classes_v38",rsEncodeClassesV38(items))

private fun rsBookedIdsV38(store:RsStore):Set<String> =
    store.s("student_bookings_v38","").split(',').filter{it.isNotBlank()}.toSet()

private fun rsSaveBookedIdsV38(store:RsStore,ids:Set<String>)=
    store.ps("student_bookings_v38",ids.joinToString(","))

private fun rsAttendanceKeyV38(classId:String,student:String)=
    "attendance_v38_"+classId+"_"+student.lowercase().replace(Regex("[^a-z0-9]"),"_")

@Composable
fun RsStudentClassesV38(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val classes=remember(revision){rsLoadClassesV38(store).filter{it.active}}
    val bookedIds=remember(revision){rsBookedIdsV38(store)}
    RsScroll(c,rsRouteTitle(lang,"classes","Classes & Events"),"Book club sessions and view live local capacity."){
        if(classes.isEmpty())RsPanel(c){Text("No active classes available.",color=c.muted)}
        classes.forEach{clazz->
            val isBooked=bookedIds.contains(clazz.id)
            val effectiveBooked=(clazz.booked + if(isBooked)1 else 0).coerceAtMost(clazz.capacity)
            val full=effectiveBooked>=clazz.capacity
            RsPanel(c){
                Text(clazz.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(clazz.dayLabel+" · "+clazz.timeLabel+" · "+clazz.level,color=c.text,fontSize=11.sp)
                Text(effectiveBooked.toString()+" / "+clazz.capacity,color=c.muted)
                LinearProgressIndicator(progress={effectiveBooked.toFloat()/clazz.capacity},modifier=Modifier.fillMaxWidth())
                Text(
                    when{
                        isBooked->"You are booked"
                        !clazz.bookingOpen->"Booking closed"
                        full->"Class full"
                        else->"Places available"
                    },
                    color=if(isBooked)c.bright else c.muted
                )
                Button(
                    onClick={
                        val next=bookedIds.toMutableSet()
                        if(isBooked)next.remove(clazz.id) else next.add(clazz.id)
                        rsSaveBookedIdsV38(store,next)
                        revision++
                    },
                    enabled=isBooked || (clazz.bookingOpen&&!full),
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(isBooked)"Cancel booking" else "Book class")}
            }
        }
    }
}

@Composable
fun RsClassManagerV38(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf("")}
    var day by remember{mutableStateOf("")}
    var time by remember{mutableStateOf("")}
    var capacity by remember{mutableStateOf("16")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val classes=remember(revision){rsLoadClassesV38(store)}

    fun save(items:List<RsClubClassV38>){
        rsSaveClassesV38(store,items)
        revision++
    }

    RsScroll(c,"Class Manager","Create and manage sessions, capacity and booking state."){
        Button(onClick={showCreate=!showCreate},modifier=Modifier.fillMaxWidth()){
            Text(if(showCreate)"Close new class" else "+ Create new class")
        }
        if(showCreate)RsPanel(c){
            Text("NEW CLASS",color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(title,{title=it},label={Text("Class title")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                OutlinedTextField(day,{day=it},label={Text("Day")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(time,{time=it},label={Text("Time")},modifier=Modifier.weight(1f),singleLine=true)
            }
            OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit)},label={Text("Capacity")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(
                onClick={
                    val cap=capacity.toIntOrNull()?.coerceIn(1,100)?:16
                    save(listOf(RsClubClassV38(UUID.randomUUID().toString(),title.trim(),day.trim(),time.trim(),"ALL LEVELS",cap,0,true,true))+classes)
                    title=""
                    day=""
                    time=""
                    capacity="16"
                    showCreate=false
                },
                enabled=title.isNotBlank()&&day.isNotBlank()&&time.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text("Save class")}
        }

        classes.forEach{clazz->
            RsPanel(c){
                Text(clazz.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(clazz.dayLabel+" · "+clazz.timeLabel+" · "+clazz.level,color=c.text,fontSize=11.sp)
                Text(clazz.booked.toString()+" / "+clazz.capacity,color=c.muted)
                LinearProgressIndicator(progress={(clazz.booked.toFloat()/clazz.capacity).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(clazz.active)"ACTIVE" else "INACTIVE",color=c.muted)
                    Switch(clazz.active,{on->save(classes.map{if(it.id==clazz.id)it.copy(active=on) else it})})
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(clazz.bookingOpen)"BOOKING OPEN" else "BOOKING CLOSED",color=c.muted)
                    Switch(clazz.bookingOpen,{on->save(classes.map{if(it.id==clazz.id)it.copy(bookingOpen=on) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==clazz.id){
                            save(classes.filterNot{it.id==clazz.id})
                            val next=rsBookedIdsV38(store).toMutableSet()
                            next.remove(clazz.id)
                            rsSaveBookedIdsV38(store,next)
                            pendingDelete=null
                        }else pendingDelete=clazz.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==clazz.id)"Confirm delete" else "Delete")}
            }
        }
    }
}

@Composable
fun RsAttendanceV38(c:RsPalette,store:RsStore,lang:RsLang){
    val classes=rsLoadClassesV38(store).filter{it.active}
    var selectedClassId by remember(classes){mutableStateOf(classes.firstOrNull()?.id.orEmpty())}
    val students=listOf("Alex de Vries","Sofia Martins","Noah Jansen","Mila Costa")
    RsScroll(c,rsRouteTitle(lang,"attendance","Attendance"),"Check students in against the same local class schedule."){
        classes.forEach{clazz->
            FilterChip(
                selected=selectedClassId==clazz.id,
                onClick={selectedClassId=clazz.id},
                label={Text(clazz.dayLabel+" "+clazz.timeLabel+" · "+clazz.title,maxLines=1)},
                modifier=Modifier.fillMaxWidth()
            )
        }
        val selected=classes.firstOrNull{it.id==selectedClassId}
        if(selected!=null){
            RsPanel(c){
                Text(selected.title,color=c.bright,fontWeight=FontWeight.Black)
                Text(selected.dayLabel+" · "+selected.timeLabel+" · "+selected.booked+"/"+selected.capacity,color=c.muted)
            }
            students.forEachIndexed{i,name->
                var present by remember(selected.id,name){
                    mutableStateOf(store.b(rsAttendanceKeyV38(selected.id,name),i!=2))
                }
                RsPanel(c){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        Text(name,color=c.text,modifier=Modifier.weight(1f))
                        Checkbox(present,{v->present=v;store.pb(rsAttendanceKeyV38(selected.id,name),v)})
                    }
                }
            }
        }
    }
}

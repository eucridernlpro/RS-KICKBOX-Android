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
import java.time.LocalDate
import kotlinx.coroutines.launch

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
    return rsDecodeClassesV38(raw)
}

fun rsSaveClassesV38(store:RsStore,items:List<RsClubClassV38>)=
    store.ps("club_classes_v38",rsEncodeClassesV38(items))

private fun rsBookingOwnerV38(store:RsStore)=store.s("session_student_email","alex@rskickbox.nl").lowercase()

private fun rsBookedIdsV38(store:RsStore):Set<String> =
    store.s("student_bookings_v38_"+rsBookingOwnerV38(store),"").split(',').filter{it.isNotBlank()}.toSet()

private fun rsSaveBookedIdsV38(store:RsStore,ids:Set<String>)=
    store.ps("student_bookings_v38_"+rsBookingOwnerV38(store),ids.joinToString(","))

fun rsAttendanceKeyV38(classId:String,student:String)=
    "attendance_v38_"+classId+"_"+student.lowercase().replace(Regex("[^a-z0-9]"),"_")

private fun rsClassUiV38(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_sub" to "Book club sessions and view live local capacity.","none" to "No active classes available.",
        "booked" to "You are booked","closed" to "Booking closed","full" to "Class full","available" to "Places available",
        "cancel" to "Cancel booking","book" to "Book class","trainer_title" to "Class Manager",
        "trainer_sub" to "Create and manage sessions, capacity and booking state.","close_new" to "Close new class",
        "create_new" to "+ Create new class","new_class" to "NEW CLASS","class_title" to "Class title","day" to "Day",
        "time" to "Time","capacity" to "Capacity","save_class" to "Save class","active" to "ACTIVE","inactive" to "INACTIVE",
        "booking_open" to "BOOKING OPEN","booking_closed" to "BOOKING CLOSED","delete" to "Delete","confirm_delete" to "Confirm delete",
        "attendance_sub" to "Check students in against the same local class schedule."
    )
    val nl=en+mapOf("student_sub" to "Boek clubtrainingen en bekijk de actuele lokale capaciteit.","none" to "Geen actieve lessen beschikbaar.","booked" to "Je bent geboekt","closed" to "Boeking gesloten","full" to "Les vol","available" to "Plaatsen beschikbaar","cancel" to "Boeking annuleren","book" to "Les boeken","trainer_title" to "Lesbeheer","trainer_sub" to "Maak trainingen en beheer capaciteit en boekingen.","close_new" to "Nieuwe les sluiten","create_new" to "+ Nieuwe les maken","new_class" to "NIEUWE LES","class_title" to "Naam les","day" to "Dag","time" to "Tijd","capacity" to "Capaciteit","save_class" to "Les opslaan","active" to "ACTIEF","inactive" to "INACTIEF","booking_open" to "BOEKING OPEN","booking_closed" to "BOEKING GESLOTEN","delete" to "Verwijderen","confirm_delete" to "Verwijderen bevestigen","attendance_sub" to "Check leerlingen in op hetzelfde lokale lesrooster.")
    val pt=en+mapOf("student_sub" to "Reserva treinos do clube e vê a capacidade local.","none" to "Não há aulas ativas.","booked" to "Estás inscrito","closed" to "Reservas fechadas","full" to "Aula cheia","available" to "Lugares disponíveis","cancel" to "Cancelar reserva","book" to "Reservar aula","trainer_title" to "Gestor de Aulas","trainer_sub" to "Cria sessões e gere capacidade e reservas.","close_new" to "Fechar nova aula","create_new" to "+ Criar nova aula","new_class" to "NOVA AULA","class_title" to "Nome da aula","day" to "Dia","time" to "Hora","capacity" to "Capacidade","save_class" to "Guardar aula","active" to "ATIVA","inactive" to "INATIVA","booking_open" to "RESERVAS ABERTAS","booking_closed" to "RESERVAS FECHADAS","delete" to "Eliminar","confirm_delete" to "Confirmar eliminação","attendance_sub" to "Faz o check-in dos alunos no mesmo horário local.")
    val es=en+mapOf("student_sub" to "Reserva sesiones del club y consulta la capacidad local.","none" to "No hay clases activas.","booked" to "Estás inscrito","closed" to "Reservas cerradas","full" to "Clase completa","available" to "Plazas disponibles","cancel" to "Cancelar reserva","book" to "Reservar clase","trainer_title" to "Gestor de Clases","trainer_sub" to "Crea sesiones y gestiona capacidad y reservas.","close_new" to "Cerrar nueva clase","create_new" to "+ Crear nueva clase","new_class" to "NUEVA CLASE","class_title" to "Nombre de clase","day" to "Día","time" to "Hora","capacity" to "Capacidad","save_class" to "Guardar clase","active" to "ACTIVA","inactive" to "INACTIVA","booking_open" to "RESERVAS ABIERTAS","booking_closed" to "RESERVAS CERRADAS","delete" to "Eliminar","confirm_delete" to "Confirmar eliminación","attendance_sub" to "Registra alumnos usando el mismo horario local.")
    val fr=en+mapOf("student_sub" to "Réserve les séances du club et consulte la capacité locale.","none" to "Aucun cours actif.","booked" to "Tu es inscrit","closed" to "Réservations fermées","full" to "Cours complet","available" to "Places disponibles","cancel" to "Annuler","book" to "Réserver","trainer_title" to "Gestion des Cours","trainer_sub" to "Crée des séances et gère capacité et réservations.","close_new" to "Fermer","create_new" to "+ Créer un cours","new_class" to "NOUVEAU COURS","class_title" to "Nom du cours","day" to "Jour","time" to "Heure","capacity" to "Capacité","save_class" to "Enregistrer","active" to "ACTIF","inactive" to "INACTIF","booking_open" to "RÉSERVATIONS OUVERTES","booking_closed" to "RÉSERVATIONS FERMÉES","delete" to "Supprimer","confirm_delete" to "Confirmer suppression","attendance_sub" to "Enregistre les élèves avec le même planning local.")
    val de=en+mapOf("student_sub" to "Buche Club-Sessions und sieh die lokale Kapazität.","none" to "Keine aktiven Kurse.","booked" to "Du bist gebucht","closed" to "Buchung geschlossen","full" to "Kurs voll","available" to "Plätze verfügbar","cancel" to "Buchung stornieren","book" to "Kurs buchen","trainer_title" to "Kursverwaltung","trainer_sub" to "Erstelle Sessions und verwalte Kapazität und Buchungen.","close_new" to "Neuen Kurs schließen","create_new" to "+ Neuen Kurs erstellen","new_class" to "NEUER KURS","class_title" to "Kursname","day" to "Tag","time" to "Zeit","capacity" to "Kapazität","save_class" to "Kurs speichern","active" to "AKTIV","inactive" to "INAKTIV","booking_open" to "BUCHUNG OFFEN","booking_closed" to "BUCHUNG GESCHLOSSEN","delete" to "Löschen","confirm_delete" to "Löschen bestätigen","attendance_sub" to "Checke Schüler im selben lokalen Kursplan ein.")
    val it=en+mapOf("student_sub" to "Prenota sessioni del club e controlla la capacità locale.","none" to "Nessun corso attivo.","booked" to "Sei prenotato","closed" to "Prenotazioni chiuse","full" to "Corso pieno","available" to "Posti disponibili","cancel" to "Annulla prenotazione","book" to "Prenota corso","trainer_title" to "Gestione Corsi","trainer_sub" to "Crea sessioni e gestisci capacità e prenotazioni.","close_new" to "Chiudi nuovo corso","create_new" to "+ Crea nuovo corso","new_class" to "NUOVO CORSO","class_title" to "Nome corso","day" to "Giorno","time" to "Ora","capacity" to "Capacità","save_class" to "Salva corso","active" to "ATTIVO","inactive" to "INATTIVO","booking_open" to "PRENOTAZIONI APERTE","booking_closed" to "PRENOTAZIONI CHIUSE","delete" to "Elimina","confirm_delete" to "Conferma eliminazione","attendance_sub" to "Registra gli allievi sullo stesso calendario locale.")
    val pl=en+mapOf("student_sub" to "Rezerwuj treningi klubu i sprawdzaj lokalną pojemność.","none" to "Brak aktywnych zajęć.","booked" to "Masz rezerwację","closed" to "Rezerwacje zamknięte","full" to "Brak miejsc","available" to "Dostępne miejsca","cancel" to "Anuluj rezerwację","book" to "Zarezerwuj","trainer_title" to "Menedżer Zajęć","trainer_sub" to "Twórz zajęcia i zarządzaj pojemnością oraz rezerwacjami.","close_new" to "Zamknij formularz","create_new" to "+ Utwórz zajęcia","new_class" to "NOWE ZAJĘCIA","class_title" to "Nazwa zajęć","day" to "Dzień","time" to "Godzina","capacity" to "Limit","save_class" to "Zapisz zajęcia","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","booking_open" to "REZERWACJE OTWARTE","booking_closed" to "REZERWACJE ZAMKNIĘTE","delete" to "Usuń","confirm_delete" to "Potwierdź usunięcie","attendance_sub" to "Oznaczaj obecność na tym samym lokalnym planie.")
    val tr=en+mapOf("student_sub" to "Kulüp derslerini rezerve et ve yerel kapasiteyi gör.","none" to "Aktif ders yok.","booked" to "Rezervasyonun var","closed" to "Rezervasyon kapalı","full" to "Ders dolu","available" to "Yer mevcut","cancel" to "Rezervasyonu iptal et","book" to "Ders rezervasyonu","trainer_title" to "Ders Yönetimi","trainer_sub" to "Ders oluştur, kapasiteyi ve rezervasyonları yönet.","close_new" to "Yeni dersi kapat","create_new" to "+ Yeni ders oluştur","new_class" to "YENİ DERS","class_title" to "Ders adı","day" to "Gün","time" to "Saat","capacity" to "Kapasite","save_class" to "Dersi kaydet","active" to "AKTİF","inactive" to "PASİF","booking_open" to "REZERVASYON AÇIK","booking_closed" to "REZERVASYON KAPALI","delete" to "Sil","confirm_delete" to "Silmeyi onayla","attendance_sub" to "Aynı yerel ders planında öğrenci girişlerini işaretle.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsStudentClassesV38(c:RsPalette,store:RsStore,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var cloudItems by remember{mutableStateOf<List<RsCloudClassStateV69>>(emptyList())}
    var cloudLoading by remember{mutableStateOf(RsSupabaseV60.configured)}
    var cloudBusyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}
    val cloudMode=RsSupabaseV60.configured

    LaunchedEffect(cloudMode,revision){
        if(cloudMode){
            cloudLoading=true
            rsCloudClassesV69()
                .onSuccess{cloudItems=it}
                .onFailure{status=it.message?:"Could not load club classes."}
            cloudLoading=false
        }
    }

    val classes=if(cloudMode)cloudItems.map{it.clazz}.filter{it.active}
        else remember(revision){rsLoadClassesV38(store).filter{it.active}}
    val bookedIds=if(cloudMode)cloudItems.filter{it.bookedByMe}.map{it.clazz.id}.toSet()
        else remember(revision){rsBookedIdsV38(store)}
    val bookingAllowed=rsOpsEnabledV56(store,RsOpsKeysV56.CLASS_BOOKING,true)

    RsScroll(c,rsRouteTitle(lang,"classes","Classes & Events"),rsClassUiV38(lang,"student_sub")){
        if(cloudMode)RsPanel(c){
            Text(
                if(cloudLoading)"Syncing live class schedule…" else "Live club schedule connected",
                color=if(cloudLoading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        if(classes.isEmpty()&&!cloudLoading)RsPanel(c){Text(rsClassUiV38(lang,"none"),color=c.muted)}
        classes.forEach{clazz->
            val isBooked=bookedIds.contains(clazz.id)
            val effectiveBooked=clazz.booked.coerceAtMost(clazz.capacity)
            val full=effectiveBooked>=clazz.capacity
            RsPanel(c){
                Text(clazz.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(clazz.dayLabel+" · "+clazz.timeLabel+" · "+clazz.level,color=c.text,fontSize=11.sp)
                Text(effectiveBooked.toString()+" / "+clazz.capacity,color=c.muted)
                LinearProgressIndicator(
                    progress={if(clazz.capacity<=0)0f else effectiveBooked.toFloat()/clazz.capacity},
                    modifier=Modifier.fillMaxWidth()
                )
                Text(
                    when{
                        isBooked->rsClassUiV38(lang,"booked")
                        !clazz.bookingOpen->rsClassUiV38(lang,"closed")
                        full->rsClassUiV38(lang,"full")
                        else->rsClassUiV38(lang,"available")
                    },
                    color=if(isBooked)c.bright else c.muted
                )
                if(!bookingAllowed && !isBooked)Text(rsOpsUiV56(lang,"booking_disabled"),color=c.muted,fontSize=10.sp)
                Button(
                    onClick={
                        if(cloudMode){
                            cloudBusyId=clazz.id
                            status=""
                            scope.launch{
                                val result=if(isBooked)rsCancelClassV69(clazz.id) else rsBookClassV69(clazz.id)
                                result
                                    .onSuccess{
                                        status=if(isBooked)"Booking cancelled." else "Class booked successfully."
                                        revision++
                                    }
                                    .onFailure{status=it.message?:"Could not update booking."}
                                cloudBusyId=null
                            }
                        }else{
                            val next=bookedIds.toMutableSet()
                            val allClasses=rsLoadClassesV38(store)
                            if(isBooked){
                                next.remove(clazz.id)
                                rsSaveClassesV38(store,allClasses.map{
                                    if(it.id==clazz.id)it.copy(booked=(it.booked-1).coerceAtLeast(0)) else it
                                })
                            }else{
                                next.add(clazz.id)
                                rsSaveClassesV38(store,allClasses.map{
                                    if(it.id==clazz.id)it.copy(booked=(it.booked+1).coerceAtMost(it.capacity)) else it
                                })
                            }
                            rsSaveBookedIdsV38(store,next)
                            revision++
                        }
                    },
                    enabled=cloudBusyId==null && (isBooked || (bookingAllowed&&clazz.bookingOpen&&!full)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(
                        if(cloudBusyId==clazz.id)"Please wait…"
                        else if(isBooked)rsClassUiV38(lang,"cancel")
                        else rsClassUiV38(lang,"book")
                    )
                }
            }
        }
    }
}

@Composable
fun RsClassManagerV38(c:RsPalette,store:RsStore,lang:RsLang){
    val scope=rememberCoroutineScope()
    val cloudMode=RsSupabaseV60.configured
    var revision by remember{mutableIntStateOf(0)}
    var cloudItems by remember{mutableStateOf<List<RsCloudClassStateV69>>(emptyList())}
    var cloudLoading by remember{mutableStateOf(cloudMode)}
    var cloudBusy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var showCreate by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf("")}
    var level by remember{mutableStateOf("ALL LEVELS")}
    var day by remember{mutableStateOf(LocalDate.now().plusDays(1).toString())}
    var time by remember{mutableStateOf("18:00")}
    var duration by remember{mutableStateOf("60")}
    var capacity by remember{mutableStateOf("16")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(cloudMode,revision){
        if(cloudMode){
            cloudLoading=true
            rsCloudClassesV69()
                .onSuccess{cloudItems=it}
                .onFailure{status=it.message?:"Could not load cloud classes."}
            cloudLoading=false
        }
    }

    val classes=if(cloudMode)cloudItems.map{it.clazz} else remember(revision){rsLoadClassesV38(store)}

    fun saveLocal(items:List<RsClubClassV38>){
        rsSaveClassesV38(store,items)
        revision++
    }

    RsScroll(c,rsClassUiV38(lang,"trainer_title"),rsClassUiV38(lang,"trainer_sub")){
        if(cloudMode)RsPanel(c){
            Text(
                if(cloudLoading)"Syncing live class manager…" else "Supabase class manager connected",
                color=if(cloudLoading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        Button(
            onClick={showCreate=!showCreate},
            enabled=!cloudBusy,
            modifier=Modifier.fillMaxWidth()
        ){
            Text(if(showCreate)rsClassUiV38(lang,"close_new") else rsClassUiV38(lang,"create_new"))
        }

        if(showCreate)RsPanel(c){
            Text(rsClassUiV38(lang,"new_class"),color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsClassUiV38(lang,"class_title"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(level,{level=it.take(40)},label={Text("Level")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                OutlinedTextField(day,{day=it.take(10)},label={Text("Date YYYY-MM-DD")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(time,{time=it.take(5)},label={Text("Time HH:mm")},modifier=Modifier.weight(1f),singleLine=true)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                OutlinedTextField(duration,{duration=it.filter(Char::isDigit).take(3)},label={Text("Minutes")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit).take(3)},label={Text(rsClassUiV38(lang,"capacity"))},modifier=Modifier.weight(1f),singleLine=true)
            }
            Button(
                onClick={
                    val cap=capacity.toIntOrNull()?.coerceIn(1,100)?:16
                    val mins=duration.toIntOrNull()?.coerceIn(15,300)?:60
                    if(cloudMode){
                        cloudBusy=true
                        status=""
                        scope.launch{
                            rsCreateCloudClassV69(title,level,day,time,mins,cap)
                                .onSuccess{
                                    title=""
                                    level="ALL LEVELS"
                                    day=LocalDate.now().plusDays(1).toString()
                                    time="18:00"
                                    duration="60"
                                    capacity="16"
                                    showCreate=false
                                    status="Class created and published."
                                    revision++
                                }
                                .onFailure{status=it.message?:"Could not create class. Use date YYYY-MM-DD and time HH:mm."}
                            cloudBusy=false
                        }
                    }else{
                        saveLocal(listOf(RsClubClassV38(UUID.randomUUID().toString(),title.trim(),day.trim(),time.trim(),level.trim().ifBlank{"ALL LEVELS"},cap,0,true,true))+classes)
                        title=""
                        day=LocalDate.now().plusDays(1).toString()
                        time="18:00"
                        capacity="16"
                        showCreate=false
                    }
                },
                enabled=!cloudBusy&&title.isNotBlank()&&day.isNotBlank()&&time.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(cloudBusy)"Publishing…" else rsClassUiV38(lang,"save_class"))}
        }

        classes.forEach{clazz->
            RsPanel(c){
                Text(clazz.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(clazz.dayLabel+" · "+clazz.timeLabel+" · "+clazz.level,color=c.text,fontSize=11.sp)
                Text(clazz.booked.toString()+" / "+clazz.capacity,color=c.muted)
                LinearProgressIndicator(progress={(clazz.booked.toFloat()/clazz.capacity).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())

                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(clazz.active)rsClassUiV38(lang,"active") else rsClassUiV38(lang,"inactive"),color=c.muted)
                    Switch(
                        clazz.active,
                        {on->
                            if(cloudMode){
                                cloudBusy=true
                                scope.launch{
                                    rsSetCloudClassStateV69(clazz.id,on,clazz.bookingOpen)
                                        .onSuccess{revision++}
                                        .onFailure{status=it.message?:"Could not update class."}
                                    cloudBusy=false
                                }
                            }else saveLocal(classes.map{if(it.id==clazz.id)it.copy(active=on) else it})
                        },
                        enabled=!cloudBusy
                    )
                }

                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(clazz.bookingOpen)rsClassUiV38(lang,"booking_open") else rsClassUiV38(lang,"booking_closed"),color=c.muted)
                    Switch(
                        clazz.bookingOpen,
                        {on->
                            if(cloudMode){
                                cloudBusy=true
                                scope.launch{
                                    rsSetCloudClassStateV69(clazz.id,clazz.active,on)
                                        .onSuccess{revision++}
                                        .onFailure{status=it.message?:"Could not update booking state."}
                                    cloudBusy=false
                                }
                            }else saveLocal(classes.map{if(it.id==clazz.id)it.copy(bookingOpen=on) else it})
                        },
                        enabled=!cloudBusy
                    )
                }

                OutlinedButton(
                    onClick={
                        if(pendingDelete==clazz.id){
                            if(cloudMode){
                                cloudBusy=true
                                scope.launch{
                                    rsDeleteCloudClassV69(clazz.id)
                                        .onSuccess{
                                            pendingDelete=null
                                            status="Class deleted."
                                            revision++
                                        }
                                        .onFailure{status=it.message?:"Could not delete class."}
                                    cloudBusy=false
                                }
                            }else{
                                saveLocal(classes.filterNot{it.id==clazz.id})
                                val next=rsBookedIdsV38(store).toMutableSet()
                                next.remove(clazz.id)
                                rsSaveBookedIdsV38(store,next)
                                pendingDelete=null
                            }
                        }else pendingDelete=clazz.id
                    },
                    enabled=!cloudBusy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==clazz.id)rsClassUiV38(lang,"confirm_delete") else rsClassUiV38(lang,"delete"))}
            }
        }
    }
}
@Composable
fun RsAttendanceV38(c:RsPalette,store:RsStore,lang:RsLang){
    val classes=rsLoadClassesV38(store).filter{it.active}
    var selectedClassId by remember(classes){mutableStateOf(classes.firstOrNull()?.id.orEmpty())}
    val createdStudents=rsLoadStudentsV33(store).filter{it.active}.map{it.name.ifBlank{it.email}}
    val students=(listOf("Alex de Vries")+createdStudents).distinct()
    RsScroll(c,rsRouteTitle(lang,"attendance","Attendance"),rsClassUiV38(lang,"attendance_sub")){
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

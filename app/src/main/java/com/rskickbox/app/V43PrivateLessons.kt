package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

data class RsPrivateSlotV43(
    val id:String,
    val dayLabel:String,
    val timeLabel:String,
    val durationMinutes:Int,
    val active:Boolean
)

data class RsPrivateBookingV43(
    val id:String,
    val slotId:String,
    val studentEmail:String,
    val studentName:String,
    val note:String,
    val status:String
)

private val rsSeedSlotsV43=listOf(
    RsPrivateSlotV43("slot_tue_1830","Tue","18:30",60,true),
    RsPrivateSlotV43("slot_thu_1730","Thu","17:30",60,true),
    RsPrivateSlotV43("slot_sat_1200","Sat","12:00",45,true)
)

private fun rsEncodeSlotsV43(items:List<RsPrivateSlotV43>):String{
    val arr=JSONArray()
    items.forEach{s->
        arr.put(JSONObject().apply{
            put("id",s.id)
            put("day",s.dayLabel)
            put("time",s.timeLabel)
            put("duration",s.durationMinutes)
            put("active",s.active)
        })
    }
    return arr.toString()
}

private fun rsDecodeSlotsV43(raw:String):List<RsPrivateSlotV43>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsPrivateSlotV43(
                    o.optString("id"),
                    o.optString("day"),
                    o.optString("time"),
                    o.optInt("duration",60).coerceIn(15,180),
                    o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadSlotsV43(store:RsStore):List<RsPrivateSlotV43>{
    val raw=store.s("private_slots_v43","")
    if(raw.isBlank()){
        store.ps("private_slots_v43",rsEncodeSlotsV43(rsSeedSlotsV43))
        return rsSeedSlotsV43
    }
    return rsDecodeSlotsV43(raw)
}

private fun rsSaveSlotsV43(store:RsStore,items:List<RsPrivateSlotV43>)=
    store.ps("private_slots_v43",rsEncodeSlotsV43(items))

private fun rsEncodeBookingsV43(items:List<RsPrivateBookingV43>):String{
    val arr=JSONArray()
    items.forEach{b->
        arr.put(JSONObject().apply{
            put("id",b.id)
            put("slotId",b.slotId)
            put("studentEmail",b.studentEmail)
            put("studentName",b.studentName)
            put("note",b.note)
            put("status",b.status)
        })
    }
    return arr.toString()
}

private fun rsDecodeBookingsV43(raw:String):List<RsPrivateBookingV43>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsPrivateBookingV43(
                    o.optString("id"),
                    o.optString("slotId"),
                    o.optString("studentEmail"),
                    o.optString("studentName"),
                    o.optString("note"),
                    o.optString("status","REQUESTED")
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadBookingsV43(store:RsStore)=rsDecodeBookingsV43(store.s("private_bookings_v43",""))
private fun rsSaveBookingsV43(store:RsStore,items:List<RsPrivateBookingV43>)=
    store.ps("private_bookings_v43",rsEncodeBookingsV43(items))

private fun rsPrivateUiV43(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "Private Lessons","student_sub" to "Request a one-to-one coaching slot with your trainer.",
        "trainer_title" to "Trainer Schedule","trainer_sub" to "Manage private coaching availability and student requests.",
        "available" to "Available","requested" to "Requested","confirmed" to "Confirmed","declined" to "Declined",
        "request" to "Request lesson","cancel" to "Cancel request","note" to "Note for trainer",
        "new_slot" to "+ Add availability","close" to "Close","day" to "Day","time" to "Time",
        "duration" to "Duration min","save" to "Save slot","active" to "ACTIVE","inactive" to "INACTIVE",
        "delete" to "Delete","confirm" to "Confirm","requests" to "STUDENT REQUESTS","none" to "No requests yet.",
        "approve" to "Confirm lesson","decline" to "Decline","reset" to "Set requested"
    )
    val nl=en+mapOf("student_title" to "Privélessen","student_sub" to "Vraag een één-op-één coachingsmoment aan.","trainer_title" to "Trainerplanning","trainer_sub" to "Beheer privécoach-beschikbaarheid en leerlingverzoeken.","available" to "Beschikbaar","requested" to "Aangevraagd","confirmed" to "Bevestigd","declined" to "Afgewezen","request" to "Privéles aanvragen","cancel" to "Aanvraag annuleren","note" to "Notitie voor trainer","new_slot" to "+ Beschikbaarheid toevoegen","close" to "Sluiten","day" to "Dag","time" to "Tijd","duration" to "Duur min","save" to "Moment opslaan","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen","requests" to "LEERLINGVERZOEKEN","none" to "Nog geen verzoeken.","approve" to "Les bevestigen","decline" to "Afwijzen","reset" to "Terug naar aangevraagd")
    val pt=en+mapOf("student_title" to "Aulas Privadas","student_sub" to "Pede uma sessão individual com o treinador.","trainer_title" to "Agenda do Treinador","trainer_sub" to "Gere disponibilidade privada e pedidos dos alunos.","available" to "Disponível","requested" to "Pedido","confirmed" to "Confirmado","declined" to "Recusado","request" to "Pedir aula","cancel" to "Cancelar pedido","note" to "Nota para o treinador","new_slot" to "+ Adicionar disponibilidade","close" to "Fechar","day" to "Dia","time" to "Hora","duration" to "Duração min","save" to "Guardar horário","active" to "ATIVO","inactive" to "INATIVO","delete" to "Eliminar","confirm" to "Confirmar","requests" to "PEDIDOS DOS ALUNOS","none" to "Ainda não há pedidos.","approve" to "Confirmar aula","decline" to "Recusar","reset" to "Voltar a pedido")
    val es=en+mapOf("student_title" to "Clases Privadas","student_sub" to "Solicita una sesión individual con tu entrenador.","trainer_title" to "Agenda del Entrenador","trainer_sub" to "Gestiona disponibilidad privada y solicitudes.","available" to "Disponible","requested" to "Solicitada","confirmed" to "Confirmada","declined" to "Rechazada","request" to "Solicitar clase","cancel" to "Cancelar solicitud","note" to "Nota para entrenador","new_slot" to "+ Añadir disponibilidad","close" to "Cerrar","day" to "Día","time" to "Hora","duration" to "Duración min","save" to "Guardar horario","active" to "ACTIVO","inactive" to "INACTIVO","delete" to "Eliminar","confirm" to "Confirmar","requests" to "SOLICITUDES DE ALUMNOS","none" to "Aún no hay solicitudes.","approve" to "Confirmar clase","decline" to "Rechazar","reset" to "Volver a solicitada")
    val fr=en+mapOf("student_title" to "Cours Privés","student_sub" to "Demande un créneau de coaching individuel.","trainer_title" to "Planning Entraîneur","trainer_sub" to "Gère les disponibilités privées et demandes élèves.","available" to "Disponible","requested" to "Demandé","confirmed" to "Confirmé","declined" to "Refusé","request" to "Demander un cours","cancel" to "Annuler la demande","note" to "Note pour l'entraîneur","new_slot" to "+ Ajouter disponibilité","close" to "Fermer","day" to "Jour","time" to "Heure","duration" to "Durée min","save" to "Enregistrer","active" to "ACTIF","inactive" to "INACTIF","delete" to "Supprimer","confirm" to "Confirmer","requests" to "DEMANDES ÉLÈVES","none" to "Aucune demande.","approve" to "Confirmer le cours","decline" to "Refuser","reset" to "Remettre en demande")
    val de=en+mapOf("student_title" to "Privatstunden","student_sub" to "Fordere einen persönlichen Coaching-Termin an.","trainer_title" to "Trainer-Zeitplan","trainer_sub" to "Verwalte private Verfügbarkeit und Schüleranfragen.","available" to "Verfügbar","requested" to "Angefragt","confirmed" to "Bestätigt","declined" to "Abgelehnt","request" to "Stunde anfragen","cancel" to "Anfrage stornieren","note" to "Notiz für Trainer","new_slot" to "+ Verfügbarkeit hinzufügen","close" to "Schließen","day" to "Tag","time" to "Zeit","duration" to "Dauer min","save" to "Termin speichern","active" to "AKTIV","inactive" to "INAKTIV","delete" to "Löschen","confirm" to "Bestätigen","requests" to "SCHÜLERANFRAGEN","none" to "Noch keine Anfragen.","approve" to "Stunde bestätigen","decline" to "Ablehnen","reset" to "Auf angefragt setzen")
    val it=en+mapOf("student_title" to "Lezioni Private","student_sub" to "Richiedi una sessione individuale con il trainer.","trainer_title" to "Agenda Allenatore","trainer_sub" to "Gestisci disponibilità privata e richieste.","available" to "Disponibile","requested" to "Richiesta","confirmed" to "Confermata","declined" to "Rifiutata","request" to "Richiedi lezione","cancel" to "Annulla richiesta","note" to "Nota per allenatore","new_slot" to "+ Aggiungi disponibilità","close" to "Chiudi","day" to "Giorno","time" to "Ora","duration" to "Durata min","save" to "Salva orario","active" to "ATTIVO","inactive" to "INATTIVO","delete" to "Elimina","confirm" to "Conferma","requests" to "RICHIESTE ALLIEVI","none" to "Nessuna richiesta.","approve" to "Conferma lezione","decline" to "Rifiuta","reset" to "Imposta richiesta")
    val pl=en+mapOf("student_title" to "Lekcje Prywatne","student_sub" to "Poproś o indywidualny termin z trenerem.","trainer_title" to "Grafik Trenera","trainer_sub" to "Zarządzaj dostępnością i prośbami uczniów.","available" to "Dostępny","requested" to "Poproszono","confirmed" to "Potwierdzono","declined" to "Odrzucono","request" to "Poproś o lekcję","cancel" to "Anuluj prośbę","note" to "Notatka dla trenera","new_slot" to "+ Dodaj dostępność","close" to "Zamknij","day" to "Dzień","time" to "Godzina","duration" to "Czas min","save" to "Zapisz termin","active" to "AKTYWNY","inactive" to "NIEAKTYWNY","delete" to "Usuń","confirm" to "Potwierdź","requests" to "PROŚBY UCZNIÓW","none" to "Brak próśb.","approve" to "Potwierdź lekcję","decline" to "Odrzuć","reset" to "Ustaw jako poproszono")
    val tr=en+mapOf("student_title" to "Özel Dersler","student_sub" to "Antrenörünle bire bir ders talep et.","trainer_title" to "Antrenör Programı","trainer_sub" to "Özel ders uygunluğunu ve öğrenci taleplerini yönet.","available" to "Uygun","requested" to "Talep edildi","confirmed" to "Onaylandı","declined" to "Reddedildi","request" to "Ders talep et","cancel" to "Talebi iptal et","note" to "Antrenöre not","new_slot" to "+ Uygunluk ekle","close" to "Kapat","day" to "Gün","time" to "Saat","duration" to "Süre dk","save" to "Saati kaydet","active" to "AKTİF","inactive" to "PASİF","delete" to "Sil","confirm" to "Onayla","requests" to "ÖĞRENCİ TALEPLERİ","none" to "Henüz talep yok.","approve" to "Dersi onayla","decline" to "Reddet","reset" to "Tekrar talep yap")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsStudentPrivateLessonsV43(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var noteBySlot by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")
    val slots=remember(revision){rsLoadSlotsV43(store).filter{it.active}}
    val allBookings=remember(revision){rsLoadBookingsV43(store)}
    val mine=allBookings.filter{it.studentEmail.equals(email,true)}
    val requestsAllowed=rsOpsEnabledV56(store,RsOpsKeysV56.PRIVATE_LESSONS,true)

    RsScroll(c,rsPrivateUiV43(lang,"student_title"),rsPrivateUiV43(lang,"student_sub")){
        slots.forEach{slot->
            val booking=mine.firstOrNull{it.slotId==slot.id && it.status!="CANCELLED"}
            RsPanel(c){
                Text(slot.dayLabel+" · "+slot.timeLabel,color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Black)
                Text(slot.durationMinutes.toString()+" min",color=c.muted)
                val statusText=when(booking?.status){
                    "CONFIRMED"->rsPrivateUiV43(lang,"confirmed")
                    "DECLINED"->rsPrivateUiV43(lang,"declined")
                    "REQUESTED"->rsPrivateUiV43(lang,"requested")
                    else->rsPrivateUiV43(lang,"available")
                }
                Text(statusText,color=c.text,fontWeight=FontWeight.Bold)
                if(booking==null || booking.status=="DECLINED"){
                    if(!requestsAllowed)Text("New private-lesson requests are temporarily disabled by the trainer.",color=c.muted,fontSize=10.sp)
                    OutlinedTextField(
                        value=noteBySlot[slot.id].orEmpty(),
                        onValueChange={v->noteBySlot=noteBySlot+(slot.id to v)},
                        label={Text(rsPrivateUiV43(lang,"note"))},
                        modifier=Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick={
                            val updated=rsLoadBookingsV43(store).filterNot{
                                it.slotId==slot.id && it.studentEmail.equals(email,true)
                            }+RsPrivateBookingV43(
                                UUID.randomUUID().toString(),slot.id,email,name,noteBySlot[slot.id].orEmpty().trim(),"REQUESTED"
                            )
                            rsSaveBookingsV43(store,updated)
                            revision++
                        },
                        enabled=requestsAllowed,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsPrivateUiV43(lang,"request"))}
                }else{
                    OutlinedButton(
                        onClick={
                            rsSaveBookingsV43(store,rsLoadBookingsV43(store).filterNot{it.id==booking.id})
                            revision++
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsPrivateUiV43(lang,"cancel"))}
                }
            }
        }
    }
}

@Composable
fun RsTrainerScheduleV43(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var day by remember{mutableStateOf("")}
    var time by remember{mutableStateOf("")}
    var duration by remember{mutableStateOf("60")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val slots=remember(revision){rsLoadSlotsV43(store)}
    val bookings=remember(revision){rsLoadBookingsV43(store)}
    fun saveSlots(items:List<RsPrivateSlotV43>){rsSaveSlotsV43(store,items);revision++}
    fun saveBookings(items:List<RsPrivateBookingV43>){rsSaveBookingsV43(store,items);revision++}

    RsScroll(c,rsPrivateUiV43(lang,"trainer_title"),rsPrivateUiV43(lang,"trainer_sub")){
        Button(onClick={showCreate=!showCreate},modifier=Modifier.fillMaxWidth()){
            Text(if(showCreate)rsPrivateUiV43(lang,"close") else rsPrivateUiV43(lang,"new_slot"))
        }
        if(showCreate)RsPanel(c){
            OutlinedTextField(day,{day=it},label={Text(rsPrivateUiV43(lang,"day"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(time,{time=it},label={Text(rsPrivateUiV43(lang,"time"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(duration,{duration=it.filter(Char::isDigit)},label={Text(rsPrivateUiV43(lang,"duration"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(
                onClick={
                    val mins=duration.toIntOrNull()?.coerceIn(15,180)?:60
                    saveSlots(listOf(RsPrivateSlotV43(UUID.randomUUID().toString(),day.trim(),time.trim(),mins,true))+slots)
                    day="";time="";duration="60";showCreate=false
                },
                enabled=day.isNotBlank()&&time.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPrivateUiV43(lang,"save"))}
        }

        slots.forEach{slot->
            val count=bookings.count{it.slotId==slot.id && it.status!="CANCELLED"}
            RsPanel(c){
                Text(slot.dayLabel+" · "+slot.timeLabel,color=c.bright,fontWeight=FontWeight.Black)
                Text(slot.durationMinutes.toString()+" min · "+count+" request(s)",color=c.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(slot.active)rsPrivateUiV43(lang,"active") else rsPrivateUiV43(lang,"inactive"),color=c.muted)
                    Switch(slot.active,{v->saveSlots(slots.map{if(it.id==slot.id)it.copy(active=v) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==slot.id){
                            saveSlots(slots.filterNot{it.id==slot.id})
                            rsSaveBookingsV43(store,bookings.filterNot{it.slotId==slot.id})
                            pendingDelete=null
                            revision++
                        }else pendingDelete=slot.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==slot.id)rsPrivateUiV43(lang,"confirm") else rsPrivateUiV43(lang,"delete"))}
            }
        }

        Text(rsPrivateUiV43(lang,"requests"),color=c.bright,fontWeight=FontWeight.Black)
        if(bookings.isEmpty())RsPanel(c){Text(rsPrivateUiV43(lang,"none"),color=c.muted)}
        bookings.forEach{booking->
            val slot=slots.firstOrNull{it.id==booking.slotId}
            RsPanel(c){
                Text(booking.studentName,color=c.bright,fontWeight=FontWeight.Black)
                Text(booking.studentEmail,color=c.muted,fontSize=10.sp)
                Text((slot?.dayLabel?:"")+" · "+(slot?.timeLabel?:""),color=c.text)
                if(booking.note.isNotBlank())Text(booking.note,color=c.muted)
                val statusText=when(booking.status){
                    "CONFIRMED"->rsPrivateUiV43(lang,"confirmed")
                    "DECLINED"->rsPrivateUiV43(lang,"declined")
                    else->rsPrivateUiV43(lang,"requested")
                }
                Text(statusText,color=c.bright,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(
                        onClick={
                            saveBookings(bookings.map{
                                when{
                                    it.id==booking.id->it.copy(status="CONFIRMED")
                                    it.slotId==booking.slotId && it.status!="CANCELLED"->it.copy(status="DECLINED")
                                    else->it
                                }
                            })
                        },
                        modifier=Modifier.weight(1f)
                    ){Text(rsPrivateUiV43(lang,"approve"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={saveBookings(bookings.map{if(it.id==booking.id)it.copy(status="DECLINED") else it})},
                        modifier=Modifier.weight(1f)
                    ){Text(rsPrivateUiV43(lang,"decline"),fontSize=10.sp)}
                }
                if(booking.status!="REQUESTED")OutlinedButton(
                    onClick={saveBookings(bookings.map{if(it.id==booking.id)it.copy(status="REQUESTED") else it})},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPrivateUiV43(lang,"reset"),fontSize=10.sp)}
            }
        }
    }
}

package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlinx.coroutines.launch

data class RsEventV42(
    val id:String,
    val title:String,
    val whenLabel:String,
    val location:String,
    val capacity:Int,
    val going:Int,
    val active:Boolean
)

private val rsSeedEventsV42=listOf(
    RsEventV42("open_mat","Open Mat","Sun · 11:00","RS KICKBOX",30,18,true),
    RsEventV42("sparring_night","Sparring Night","Fri · 20:30","RS KICKBOX",20,11,true),
    RsEventV42("seminar","Technique Seminar","Sat · 13:00","RS KICKBOX",40,24,true)
)

private fun rsEncodeEventsV42(items:List<RsEventV42>):String{
    val arr=JSONArray()
    items.forEach{e->
        arr.put(JSONObject().apply{
            put("id",e.id);put("title",e.title);put("when",e.whenLabel);put("location",e.location)
            put("capacity",e.capacity);put("going",e.going);put("active",e.active)
        })
    }
    return arr.toString()
}

private fun rsDecodeEventsV42(raw:String):List<RsEventV42>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsEventV42(
                    o.optString("id"),o.optString("title"),o.optString("when"),o.optString("location"),
                    o.optInt("capacity",20).coerceAtLeast(1),o.optInt("going",0).coerceAtLeast(0),
                    o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadEventsV42(store:RsStore):List<RsEventV42>{
    val raw=store.s("events_v42","")
    if(raw.isBlank()){
        store.ps("events_v42",rsEncodeEventsV42(rsSeedEventsV42))
        return rsSeedEventsV42
    }
    return rsDecodeEventsV42(raw)
}

private fun rsSaveEventsV42(store:RsStore,items:List<RsEventV42>)=
    store.ps("events_v42",rsEncodeEventsV42(items))

private fun rsEventOwnerV42(store:RsStore)=store.s("session_student_email","alex@rskickbox.nl").lowercase()
private fun rsEventRsvpsV42(store:RsStore):Set<String> =
    store.s("event_rsvps_v42_"+rsEventOwnerV42(store),"").split(',').filter{it.isNotBlank()}.toSet()
private fun rsSaveEventRsvpsV42(store:RsStore,ids:Set<String>)=
    store.ps("event_rsvps_v42_"+rsEventOwnerV42(store),ids.joinToString(","))

private fun rsEventUiV42(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "RS Events","student_sub" to "Club events, seminars and special training moments.",
        "trainer_title" to "Event Manager","trainer_sub" to "Create and manage club events and RSVP capacity.",
        "going" to "You are going","available" to "Places available","full" to "Event full",
        "join" to "RSVP","cancel" to "Cancel RSVP","new" to "+ Create event","close" to "Close",
        "title" to "Event title","when" to "Date / time","location" to "Location","capacity" to "Capacity",
        "save" to "Save event","active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm"
    )
    val nl=en+mapOf("student_title" to "RS Events","student_sub" to "Clubevents, seminars en speciale trainingsmomenten.","trainer_title" to "Eventbeheer","trainer_sub" to "Maak en beheer clubevents en RSVP-capaciteit.","going" to "Je gaat","available" to "Plaatsen beschikbaar","full" to "Event vol","join" to "Aanmelden","cancel" to "Aanmelding annuleren","new" to "+ Event maken","close" to "Sluiten","title" to "Eventnaam","when" to "Datum / tijd","location" to "Locatie","capacity" to "Capaciteit","save" to "Event opslaan","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen")
    val pt=en+mapOf("student_title" to "Eventos RS","student_sub" to "Eventos, seminários e momentos especiais do clube.","trainer_title" to "Gestor de Eventos","trainer_sub" to "Cria e gere eventos e capacidade de inscrições.","going" to "Vais participar","available" to "Lugares disponíveis","full" to "Evento cheio","join" to "Participar","cancel" to "Cancelar participação","new" to "+ Criar evento","close" to "Fechar","title" to "Nome do evento","when" to "Data / hora","location" to "Local","capacity" to "Capacidade","save" to "Guardar evento","active" to "ATIVO","inactive" to "INATIVO","delete" to "Eliminar","confirm" to "Confirmar")
    val es=en+mapOf("student_title" to "Eventos RS","student_sub" to "Eventos, seminarios y momentos especiales del club.","trainer_title" to "Gestor de Eventos","trainer_sub" to "Crea y gestiona eventos y capacidad.","going" to "Vas a asistir","available" to "Plazas disponibles","full" to "Evento completo","join" to "Confirmar asistencia","cancel" to "Cancelar asistencia","new" to "+ Crear evento","close" to "Cerrar","title" to "Nombre del evento","when" to "Fecha / hora","location" to "Lugar","capacity" to "Capacidad","save" to "Guardar evento","active" to "ACTIVO","inactive" to "INACTIVO","delete" to "Eliminar","confirm" to "Confirmar")
    val fr=en+mapOf("student_title" to "Événements RS","student_sub" to "Événements, séminaires et moments spéciaux du club.","trainer_title" to "Gestion Événements","trainer_sub" to "Crée et gère les événements et la capacité.","going" to "Tu participes","available" to "Places disponibles","full" to "Événement complet","join" to "Participer","cancel" to "Annuler","new" to "+ Créer événement","close" to "Fermer","title" to "Nom","when" to "Date / heure","location" to "Lieu","capacity" to "Capacité","save" to "Enregistrer","active" to "ACTIF","inactive" to "INACTIF","delete" to "Supprimer","confirm" to "Confirmer")
    val de=en+mapOf("student_title" to "RS Events","student_sub" to "Club-Events, Seminare und besondere Trainingsmomente.","trainer_title" to "Event-Manager","trainer_sub" to "Erstelle und verwalte Events und Kapazität.","going" to "Du nimmst teil","available" to "Plätze verfügbar","full" to "Event voll","join" to "Teilnehmen","cancel" to "Teilnahme stornieren","new" to "+ Event erstellen","close" to "Schließen","title" to "Eventname","when" to "Datum / Zeit","location" to "Ort","capacity" to "Kapazität","save" to "Event speichern","active" to "AKTIV","inactive" to "INAKTIV","delete" to "Löschen","confirm" to "Bestätigen")
    val it=en+mapOf("student_title" to "Eventi RS","student_sub" to "Eventi, seminari e momenti speciali del club.","trainer_title" to "Gestione Eventi","trainer_sub" to "Crea e gestisci eventi e capacità.","going" to "Parteciperai","available" to "Posti disponibili","full" to "Evento pieno","join" to "Partecipa","cancel" to "Annulla partecipazione","new" to "+ Crea evento","close" to "Chiudi","title" to "Nome evento","when" to "Data / ora","location" to "Luogo","capacity" to "Capacità","save" to "Salva evento","active" to "ATTIVO","inactive" to "INATTIVO","delete" to "Elimina","confirm" to "Conferma")
    val pl=en+mapOf("student_title" to "Wydarzenia RS","student_sub" to "Wydarzenia, seminaria i specjalne treningi.","trainer_title" to "Menedżer Wydarzeń","trainer_sub" to "Twórz wydarzenia i zarządzaj limitem miejsc.","going" to "Bierzesz udział","available" to "Dostępne miejsca","full" to "Brak miejsc","join" to "Dołącz","cancel" to "Anuluj udział","new" to "+ Utwórz wydarzenie","close" to "Zamknij","title" to "Nazwa wydarzenia","when" to "Data / czas","location" to "Miejsce","capacity" to "Limit","save" to "Zapisz wydarzenie","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","delete" to "Usuń","confirm" to "Potwierdź")
    val tr=en+mapOf("student_title" to "RS Etkinlikleri","student_sub" to "Kulüp etkinlikleri, seminerler ve özel antrenmanlar.","trainer_title" to "Etkinlik Yönetimi","trainer_sub" to "Etkinlik oluştur ve kapasiteyi yönet.","going" to "Katılıyorsun","available" to "Yer mevcut","full" to "Etkinlik dolu","join" to "Katıl","cancel" to "Katılımı iptal et","new" to "+ Etkinlik oluştur","close" to "Kapat","title" to "Etkinlik adı","when" to "Tarih / saat","location" to "Konum","capacity" to "Kapasite","save" to "Etkinliği kaydet","active" to "AKTİF","inactive" to "PASİF","delete" to "Sil","confirm" to "Onayla")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsStudentEventsV42(c:RsPalette,store:RsStore,lang:RsLang){
    if(!RsSupabaseV60.configured){
        var revision by remember{mutableIntStateOf(0)}
        val items=remember(revision){rsLoadEventsV42(store).filter{it.active}}
        val rsvps=remember(revision){rsEventRsvpsV42(store)}
        RsScroll(c,rsEventUiV42(lang,"student_title"),rsEventUiV42(lang,"student_sub")){
            items.forEach{event->
                val joined=event.id in rsvps
                val going=event.going.coerceAtMost(event.capacity)
                val full=going>=event.capacity
                RsPanel(c){
                    Text(event.title,color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Black)
                    Text(event.whenLabel+" · "+event.location,color=c.text)
                    Text(going.toString()+" / "+event.capacity,color=c.muted)
                    LinearProgressIndicator(progress={going.toFloat()/event.capacity},modifier=Modifier.fillMaxWidth())
                    Text(if(joined)rsEventUiV42(lang,"going") else if(full)rsEventUiV42(lang,"full") else rsEventUiV42(lang,"available"),color=c.muted)
                    Button(onClick={
                        val next=rsvps.toMutableSet()
                        val all=rsLoadEventsV42(store)
                        if(joined){
                            next.remove(event.id)
                            rsSaveEventsV42(store,all.map{if(it.id==event.id)it.copy(going=(it.going-1).coerceAtLeast(0)) else it})
                        }else{
                            next.add(event.id)
                            rsSaveEventsV42(store,all.map{if(it.id==event.id)it.copy(going=(it.going+1).coerceAtMost(it.capacity)) else it})
                        }
                        rsSaveEventRsvpsV42(store,next)
                        revision++
                    },enabled=joined||!full,modifier=Modifier.fillMaxWidth()){
                        Text(if(joined)rsEventUiV42(lang,"cancel") else rsEventUiV42(lang,"join"))
                    }
                }
            }
        }
        return
    }

    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudEventV75>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsCloudEventsV75()
            .onSuccess{items=it.filter{e->e.active}}
            .onFailure{status=it.message?:"Could not load events."}
        loading=false
    }

    RsScroll(c,rsEventUiV42(lang,"student_title"),"Live club events synchronized across devices."){
        RsPanel(c){
            Text(if(loading)"Syncing events…" else "Cloud events connected",color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        items.forEach{event->
            val joined=event.myStatus=="going"
            val full=event.goingCount>=event.capacity
            RsPanel(c){
                Text(event.title,color=c.bright,fontSize=18.sp,fontWeight=FontWeight.Black)
                Text(event.whenLabel+" · "+event.location,color=c.text)
                Text(event.goingCount.toString()+" / "+event.capacity,color=c.muted)
                LinearProgressIndicator(progress={(event.goingCount.toFloat()/event.capacity).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                Text(if(joined)rsEventUiV42(lang,"going") else if(full)rsEventUiV42(lang,"full") else rsEventUiV42(lang,"available"),color=c.muted)
                Button(
                    onClick={
                        busyId=event.id
                        scope.launch{
                            val result=if(joined)rsCloudCancelEventRsvpV75(event.id) else rsCloudEventRsvpV75(event.id)
                            result.onSuccess{revision++}.onFailure{status=it.message?:"Could not update RSVP."}
                            busyId=null
                        }
                    },
                    enabled=busyId==null&&(joined||!full),
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(busyId==event.id)"Please wait…" else if(joined)rsEventUiV42(lang,"cancel") else rsEventUiV42(lang,"join"))}
            }
        }
    }
}

@Composable
fun RsEventManagerV42(c:RsPalette,store:RsStore,lang:RsLang){
    if(!RsSupabaseV60.configured){
        var revision by remember{mutableIntStateOf(0)}
        var showCreate by remember{mutableStateOf(false)}
        var title by remember{mutableStateOf("")}
        var whenLabel by remember{mutableStateOf("")}
        var location by remember{mutableStateOf("")}
        var capacity by remember{mutableStateOf("20")}
        var pendingDelete by remember{mutableStateOf<String?>(null)}
        val items=remember(revision){rsLoadEventsV42(store)}
        fun save(updated:List<RsEventV42>){rsSaveEventsV42(store,updated);revision++}
        RsScroll(c,rsEventUiV42(lang,"trainer_title"),rsEventUiV42(lang,"trainer_sub")){
            Button(onClick={showCreate=!showCreate},modifier=Modifier.fillMaxWidth()){Text(if(showCreate)rsEventUiV42(lang,"close") else rsEventUiV42(lang,"new"))}
            if(showCreate)RsPanel(c){
                OutlinedTextField(title,{title=it},label={Text(rsEventUiV42(lang,"title"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(whenLabel,{whenLabel=it},label={Text(rsEventUiV42(lang,"when"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(location,{location=it},label={Text(rsEventUiV42(lang,"location"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit)},label={Text(rsEventUiV42(lang,"capacity"))},modifier=Modifier.fillMaxWidth())
                Button(onClick={
                    val cap=capacity.toIntOrNull()?.coerceIn(1,200)?:20
                    save(listOf(RsEventV42(UUID.randomUUID().toString(),title.trim(),whenLabel.trim(),location.trim(),cap,0,true))+items)
                    title="";whenLabel="";location="";capacity="20";showCreate=false
                },enabled=title.isNotBlank()&&whenLabel.isNotBlank()&&location.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text(rsEventUiV42(lang,"save"))}
            }
            items.forEach{event->
                RsPanel(c){
                    Text(event.title,color=c.bright,fontWeight=FontWeight.Black)
                    Text(event.whenLabel+" · "+event.location,color=c.text)
                    Text(event.going.toString()+" / "+event.capacity,color=c.muted)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(event.active)rsEventUiV42(lang,"active") else rsEventUiV42(lang,"inactive"),color=c.muted)
                        Switch(event.active,{v->save(items.map{if(it.id==event.id)it.copy(active=v) else it})})
                    }
                    OutlinedButton(onClick={
                        if(pendingDelete==event.id){save(items.filterNot{it.id==event.id});pendingDelete=null}else pendingDelete=event.id
                    },modifier=Modifier.fillMaxWidth()){Text(if(pendingDelete==event.id)rsEventUiV42(lang,"confirm") else rsEventUiV42(lang,"delete"))}
                }
            }
        }
        return
    }

    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudEventV75>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var showCreate by remember{mutableStateOf(false)}
    var title by remember{mutableStateOf("")}
    var whenLabel by remember{mutableStateOf("")}
    var location by remember{mutableStateOf("")}
    var capacity by remember{mutableStateOf("20")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsCloudEventsV75().onSuccess{items=it}.onFailure{status=it.message?:"Could not load events."}
        loading=false
    }

    RsScroll(c,rsEventUiV42(lang,"trainer_title"),"Create and manage shared club events with live RSVP capacity."){
        RsPanel(c){
            Text(if(loading)"Syncing events…" else "Cloud event manager connected",color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        Button(onClick={showCreate=!showCreate},enabled=!busy,modifier=Modifier.fillMaxWidth()){
            Text(if(showCreate)rsEventUiV42(lang,"close") else rsEventUiV42(lang,"new"))
        }
        if(showCreate)RsPanel(c){
            OutlinedTextField(title,{title=it.take(120)},label={Text(rsEventUiV42(lang,"title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(whenLabel,{whenLabel=it.take(120)},label={Text(rsEventUiV42(lang,"when"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(location,{location=it.take(120)},label={Text(rsEventUiV42(lang,"location"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit).take(3)},label={Text(rsEventUiV42(lang,"capacity"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            Button(onClick={
                busy=true
                scope.launch{
                    rsCloudCreateEventV75(title,whenLabel,location,capacity.toIntOrNull()?.coerceIn(1,500)?:20)
                        .onSuccess{
                            title="";whenLabel="";location="";capacity="20";showCreate=false;status="Event created.";revision++
                        }
                        .onFailure{status=it.message?:"Could not create event."}
                    busy=false
                }
            },enabled=!busy&&title.isNotBlank()&&whenLabel.isNotBlank()&&location.isNotBlank(),modifier=Modifier.fillMaxWidth()){
                Text(if(busy)"Saving…" else rsEventUiV42(lang,"save"))
            }
        }
        items.forEach{event->
            RsPanel(c){
                Text(event.title,color=c.bright,fontWeight=FontWeight.Black)
                Text(event.whenLabel+" · "+event.location,color=c.text)
                Text(event.goingCount.toString()+" / "+event.capacity,color=c.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(event.active)rsEventUiV42(lang,"active") else rsEventUiV42(lang,"inactive"),color=c.muted)
                    Switch(event.active,{v->
                        busy=true
                        scope.launch{
                            rsCloudSetEventActiveV75(event.id,v).onSuccess{revision++}.onFailure{status=it.message?:"Could not update event."}
                            busy=false
                        }
                    },enabled=!busy)
                }
                OutlinedButton(onClick={
                    if(pendingDelete==event.id){
                        busy=true
                        scope.launch{
                            rsCloudDeleteEventV75(event.id).onSuccess{pendingDelete=null;revision++}.onFailure{status=it.message?:"Could not delete event."}
                            busy=false
                        }
                    }else pendingDelete=event.id
                },enabled=!busy,modifier=Modifier.fillMaxWidth()){
                    Text(if(pendingDelete==event.id)rsEventUiV42(lang,"confirm") else rsEventUiV42(lang,"delete"))
                }
            }
        }
    }
}
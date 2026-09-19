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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

data class RsNotificationV41(
    val id:String,
    val title:String,
    val message:String,
    val audience:String,
    val created:String
)

private fun rsEncodeNotificationsV41(items:List<RsNotificationV41>):String{
    val arr=JSONArray()
    items.forEach{n->
        arr.put(JSONObject().apply{
            put("id",n.id)
            put("title",n.title)
            put("message",n.message)
            put("audience",n.audience)
            put("created",n.created)
        })
    }
    return arr.toString()
}

private fun rsDecodeNotificationsV41(raw:String):List<RsNotificationV41>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsNotificationV41(
                    o.optString("id"),
                    o.optString("title"),
                    o.optString("message"),
                    o.optString("audience","ALL"),
                    o.optString("created")
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadNotificationsV41(store:RsStore)=
    rsDecodeNotificationsV41(store.s("notifications_v41",""))

private fun rsSaveNotificationsV41(store:RsStore,items:List<RsNotificationV41>)=
    store.ps("notifications_v41",rsEncodeNotificationsV41(items))

private fun rsReadNotificationIdsV41(store:RsStore):Set<String>{
    val email=store.s("session_student_email","alex@rskickbox.nl").lowercase()
    return store.s("notification_read_v41_"+email,"").split(',').filter{it.isNotBlank()}.toSet()
}

private fun rsSaveReadNotificationIdsV41(store:RsStore,ids:Set<String>){
    val email=store.s("session_student_email","alex@rskickbox.nl").lowercase()
    store.ps("notification_read_v41_"+email,ids.joinToString(","))
}

fun rsUnreadNotificationCountV55(store:RsStore):Int{
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val account=rsLoadStudentsV33(store).firstOrNull{it.email.equals(email,true)}
    val plan=account?.plan?.uppercase()?:"PRO"
    val read=rsReadNotificationIdsV41(store)
    return rsLoadNotificationsV41(store).count{
        (it.audience=="ALL"||it.audience==plan) && it.id !in read
    }
}

private fun rsNotificationUiV41(lang:RsLang,key:String):String{
    val en=mapOf(
        "trainer_title" to "Notification Center","trainer_sub" to "Create local club notifications for all members or selected plans.",
        "student_title" to "Notifications","student_sub" to "Club updates and reminders for your membership.",
        "new" to "NEW NOTIFICATION","title" to "Title","message" to "Message","send" to "Publish notification",
        "all" to "ALL","delete" to "Delete","confirm" to "Confirm","empty" to "No notifications yet.",
        "mark_read" to "Mark read","read" to "Read","unread" to "Unread","audience" to "Audience"
    )
    val nl=en+mapOf("trainer_title" to "Meldingencentrum","trainer_sub" to "Maak lokale clubmeldingen voor alle leden of geselecteerde plannen.","student_title" to "Meldingen","student_sub" to "Clubupdates en herinneringen voor jouw lidmaatschap.","new" to "NIEUWE MELDING","title" to "Titel","message" to "Bericht","send" to "Melding publiceren","delete" to "Verwijderen","confirm" to "Bevestigen","empty" to "Nog geen meldingen.","mark_read" to "Markeer gelezen","read" to "Gelezen","unread" to "Ongelezen","audience" to "Doelgroep")
    val pt=en+mapOf("trainer_title" to "Centro de Notificações","trainer_sub" to "Cria notificações locais para todos ou planos selecionados.","student_title" to "Notificações","student_sub" to "Atualizações e lembretes do clube para a tua adesão.","new" to "NOVA NOTIFICAÇÃO","title" to "Título","message" to "Mensagem","send" to "Publicar notificação","delete" to "Eliminar","confirm" to "Confirmar","empty" to "Ainda não há notificações.","mark_read" to "Marcar lida","read" to "Lida","unread" to "Não lida","audience" to "Público")
    val es=en+mapOf("trainer_title" to "Centro de Notificaciones","trainer_sub" to "Crea notificaciones locales para todos o planes seleccionados.","student_title" to "Notificaciones","student_sub" to "Actualizaciones y recordatorios del club.","new" to "NUEVA NOTIFICACIÓN","title" to "Título","message" to "Mensaje","send" to "Publicar notificación","delete" to "Eliminar","confirm" to "Confirmar","empty" to "Aún no hay notificaciones.","mark_read" to "Marcar leída","read" to "Leída","unread" to "No leída","audience" to "Público")
    val fr=en+mapOf("trainer_title" to "Centre de Notifications","trainer_sub" to "Crée des notifications locales pour tous ou certaines formules.","student_title" to "Notifications","student_sub" to "Actualités et rappels du club pour ton adhésion.","new" to "NOUVELLE NOTIFICATION","title" to "Titre","message" to "Message","send" to "Publier","delete" to "Supprimer","confirm" to "Confirmer","empty" to "Aucune notification.","mark_read" to "Marquer lue","read" to "Lue","unread" to "Non lue","audience" to "Audience")
    val de=en+mapOf("trainer_title" to "Benachrichtigungscenter","trainer_sub" to "Erstelle lokale Clubmeldungen für alle oder ausgewählte Pläne.","student_title" to "Benachrichtigungen","student_sub" to "Club-Updates und Erinnerungen für deine Mitgliedschaft.","new" to "NEUE BENACHRICHTIGUNG","title" to "Titel","message" to "Nachricht","send" to "Veröffentlichen","delete" to "Löschen","confirm" to "Bestätigen","empty" to "Noch keine Benachrichtigungen.","mark_read" to "Als gelesen","read" to "Gelesen","unread" to "Ungelesen","audience" to "Zielgruppe")
    val it=en+mapOf("trainer_title" to "Centro Notifiche","trainer_sub" to "Crea notifiche locali per tutti o piani selezionati.","student_title" to "Notifiche","student_sub" to "Aggiornamenti e promemoria del club.","new" to "NUOVA NOTIFICA","title" to "Titolo","message" to "Messaggio","send" to "Pubblica notifica","delete" to "Elimina","confirm" to "Conferma","empty" to "Nessuna notifica.","mark_read" to "Segna letta","read" to "Letta","unread" to "Non letta","audience" to "Pubblico")
    val pl=en+mapOf("trainer_title" to "Centrum Powiadomień","trainer_sub" to "Twórz lokalne powiadomienia dla wszystkich lub wybranych planów.","student_title" to "Powiadomienia","student_sub" to "Aktualności i przypomnienia klubu.","new" to "NOWE POWIADOMIENIE","title" to "Tytuł","message" to "Wiadomość","send" to "Opublikuj","delete" to "Usuń","confirm" to "Potwierdź","empty" to "Brak powiadomień.","mark_read" to "Oznacz przeczytane","read" to "Przeczytane","unread" to "Nieprzeczytane","audience" to "Odbiorcy")
    val tr=en+mapOf("trainer_title" to "Bildirim Merkezi","trainer_sub" to "Tüm üyeler veya seçili planlar için yerel bildirim oluştur.","student_title" to "Bildirimler","student_sub" to "Üyeliğin için kulüp güncellemeleri ve hatırlatmalar.","new" to "YENİ BİLDİRİM","title" to "Başlık","message" to "Mesaj","send" to "Bildirimi yayınla","delete" to "Sil","confirm" to "Onayla","empty" to "Henüz bildirim yok.","mark_read" to "Okundu işaretle","read" to "Okundu","unread" to "Okunmadı","audience" to "Hedef")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsNotificationsV41(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    if(RsSupabaseV60.configured){
        if(role==RsRole.TRAINER)RsCloudTrainerNotificationsV74(c,lang)
        else RsCloudStudentNotificationsV74(c,lang)
    }else{
        if(role==RsRole.TRAINER)RsTrainerNotificationsV41(c,store,lang)
        else RsStudentNotificationsV41(c,store,lang)
    }
}

@Composable
private fun RsTrainerNotificationsV41(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var title by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}
    var audience by remember{mutableStateOf("ALL")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val items=remember(revision){rsLoadNotificationsV41(store)}
    fun save(updated:List<RsNotificationV41>){rsSaveNotificationsV41(store,updated);revision++}

    RsScroll(c,rsNotificationUiV41(lang,"trainer_title"),rsNotificationUiV41(lang,"trainer_sub")){
        RsPanel(c){
            Text(rsNotificationUiV41(lang,"new"),color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedTextField(title,{title=it},label={Text(rsNotificationUiV41(lang,"title"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(message,{message=it},label={Text(rsNotificationUiV41(lang,"message"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","BASIC","PRO","ELITE").forEach{plan->
                    FilterChip(selected=audience==plan,onClick={audience=plan},label={Text(plan)},modifier=Modifier.weight(1f))
                }
            }
            Button(
                onClick={
                    val created=SimpleDateFormat("dd MMM yyyy · HH:mm",Locale.getDefault()).format(Date())
                    save(listOf(RsNotificationV41(UUID.randomUUID().toString(),title.trim(),message.trim(),audience,created))+items)
                    title=""
                    message=""
                    audience="ALL"
                },
                enabled=title.isNotBlank()&&message.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsNotificationUiV41(lang,"send"))}
        }
        if(items.isEmpty())RsPanel(c){Text(rsNotificationUiV41(lang,"empty"),color=c.muted)}
        items.forEach{item->
            RsPanel(c){
                Text(item.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(item.message,color=c.text)
                Text(item.created+" · "+rsNotificationUiV41(lang,"audience")+" "+item.audience,color=c.muted,fontSize=10.sp)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==item.id){
                            save(items.filterNot{it.id==item.id})
                            pendingDelete=null
                        }else pendingDelete=item.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==item.id)rsNotificationUiV41(lang,"confirm") else rsNotificationUiV41(lang,"delete"))}
            }
        }
    }
}

@Composable
private fun RsStudentNotificationsV41(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val account=rsLoadStudentsV33(store).firstOrNull{it.email.equals(email,true)}
    val plan=account?.plan?.uppercase()?:"PRO"
    val items=remember(revision){rsLoadNotificationsV41(store).filter{it.audience=="ALL"||it.audience==plan}}
    val readIds=remember(revision){rsReadNotificationIdsV41(store)}

    RsScroll(c,rsNotificationUiV41(lang,"student_title"),rsNotificationUiV41(lang,"student_sub")){
        if(items.isEmpty())RsPanel(c){Text(rsNotificationUiV41(lang,"empty"),color=c.muted)}
        items.forEach{item->
            val read=item.id in readIds
            RsPanel(c){
                Text(item.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(item.message,color=c.text)
                Text(item.created+" · "+if(read)rsNotificationUiV41(lang,"read") else rsNotificationUiV41(lang,"unread"),color=c.muted,fontSize=10.sp)
                if(!read)Button(
                    onClick={
                        val next=readIds.toMutableSet()
                        next.add(item.id)
                        rsSaveReadNotificationIdsV41(store,next)
                        revision++
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsNotificationUiV41(lang,"mark_read"))}
            }
        }
    }
}


@Composable
private fun RsCloudTrainerNotificationsV74(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudNotificationV74>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}
    var audience by remember{mutableStateOf("ALL")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsCloudNotificationsV74()
            .onSuccess{items=it}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsNotificationUiV41(lang,"trainer_title"),rsReleaseT98(lang,"notifications_admin_sub")){
        RsPanel(c){
            Text(
                if(loading)"Syncing notifications…" else rsReleaseT98(lang,"notification_center_connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        RsPanel(c){
            Text(rsNotificationUiV41(lang,"new"),color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedTextField(
                title,
                {title=it.take(120)},
                label={Text(rsNotificationUiV41(lang,"title"))},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                enabled=!busy
            )
            OutlinedTextField(
                message,
                {message=it.take(2000)},
                label={Text(rsNotificationUiV41(lang,"message"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=3,
                enabled=!busy
            )
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","BASIC","PRO","ELITE").forEach{plan->
                    FilterChip(
                        selected=audience==plan,
                        onClick={audience=plan},
                        enabled=!busy,
                        label={Text(plan,fontSize=9.sp)},
                        modifier=Modifier.weight(1f)
                    )
                }
            }
            Button(
                onClick={
                    busy=true
                    status=""
                    scope.launch{
                        rsCreateCloudNotificationV74(title,message,audience)
                            .onSuccess{
                                title=""
                                message=""
                                audience="ALL"
                                status=rsCloudT93(lang,"notification_published")
                                revision++
                            }
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busy=false
                    }
                },
                enabled=!busy&&title.isNotBlank()&&message.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Publishing…" else rsNotificationUiV41(lang,"send"))}
        }

        if(items.isEmpty()&&!loading)RsPanel(c){Text(rsNotificationUiV41(lang,"empty"),color=c.muted)}

        items.forEach{item->
            RsPanel(c){
                Text(item.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(item.message,color=c.text)
                Text(
                    item.createdLabel()+" · "+rsNotificationUiV41(lang,"audience")+" "+item.audience,
                    color=c.muted,
                    fontSize=10.sp
                )
                OutlinedButton(
                    onClick={
                        if(pendingDelete==item.id){
                            busy=true
                            scope.launch{
                                rsDeleteCloudNotificationV74(item.id)
                                    .onSuccess{
                                        pendingDelete=null
                                        status=rsCloudT93(lang,"notification_deleted")
                                        revision++
                                    }
                                    .onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                busy=false
                            }
                        }else pendingDelete=item.id
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==item.id)rsNotificationUiV41(lang,"confirm") else rsNotificationUiV41(lang,"delete"))}
            }
        }
    }
}

@Composable
private fun RsCloudStudentNotificationsV74(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudNotificationV74>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsCloudNotificationsV74()
            .onSuccess{items=it}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsNotificationUiV41(lang,"student_title"),rsReleaseT98(lang,"notifications_student_sub")){
        RsPanel(c){
            Text(
                if(loading)"Syncing notifications…" else rsReleaseT98(lang,"notifications_connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        if(items.isEmpty()&&!loading)RsPanel(c){Text(rsNotificationUiV41(lang,"empty"),color=c.muted)}

        items.forEach{item->
            RsPanel(c){
                Text(item.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(item.message,color=c.text)
                Text(
                    item.createdLabel()+" · "+if(item.read)rsNotificationUiV41(lang,"read") else rsNotificationUiV41(lang,"unread"),
                    color=c.muted,
                    fontSize=10.sp
                )
                if(!item.read){
                    Button(
                        onClick={
                            busyId=item.id
                            scope.launch{
                                rsMarkCloudNotificationReadV74(item.id)
                                    .onSuccess{revision++}
                                    .onFailure{status=rsReleaseT98(lang,"update_failed")}
                                busyId=null
                            }
                        },
                        enabled=busyId==null,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(busyId==item.id)rsReleaseT98(lang,"please_wait") else rsNotificationUiV41(lang,"mark_read"))}
                }
            }
        }
    }
}

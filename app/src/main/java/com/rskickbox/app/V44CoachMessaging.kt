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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

data class RsCoachMessageV44(
    val id:String,
    val studentEmail:String,
    val studentName:String,
    val sender:String,
    val body:String,
    val createdAt:Long
)

private fun rsEncodeCoachMessagesV44(items:List<RsCoachMessageV44>):String{
    val arr=JSONArray()
    items.forEach{m->
        arr.put(JSONObject().apply{
            put("id",m.id)
            put("studentEmail",m.studentEmail)
            put("studentName",m.studentName)
            put("sender",m.sender)
            put("body",m.body)
            put("createdAt",m.createdAt)
        })
    }
    return arr.toString()
}

private fun rsDecodeCoachMessagesV44(raw:String):List<RsCoachMessageV44>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsCoachMessageV44(
                    id=o.optString("id"),
                    studentEmail=o.optString("studentEmail"),
                    studentName=o.optString("studentName"),
                    sender=o.optString("sender","student"),
                    body=o.optString("body"),
                    createdAt=o.optLong("createdAt",0L)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadCoachMessagesV44(store:RsStore)=
    rsDecodeCoachMessagesV44(store.s("coach_messages_v44",""))

private fun rsSaveCoachMessagesV44(store:RsStore,items:List<RsCoachMessageV44>)=
    store.ps("coach_messages_v44",rsEncodeCoachMessagesV44(items))

fun rsCoachMessagesRawForStudentV44(store:RsStore,email:String):String =
    rsEncodeCoachMessagesV44(
        rsLoadCoachMessagesV44(store).filter{it.studentEmail.equals(email,true)}
    )

fun rsRemoveCoachMessagesForStudentV44(store:RsStore,email:String){
    rsSaveCoachMessagesV44(
        store,
        rsLoadCoachMessagesV44(store).filterNot{it.studentEmail.equals(email,true)}
    )
    store.ps(rsTrainerReadKeyV44(email),"0")
    store.ps(rsStudentReadKeyV44(email),"0")
}

private fun rsThreadKeyV44(email:String)=email.trim().lowercase().replace(Regex("[^a-z0-9]"),"_")
private fun rsTrainerReadKeyV44(email:String)="coach_read_trainer_v44_"+rsThreadKeyV44(email)
private fun rsStudentReadKeyV44(email:String)="coach_read_student_v44_"+rsThreadKeyV44(email)

fun rsTrainerUnreadCoachCountV55(store:RsStore):Int{
    val messages=rsLoadCoachMessagesV44(store)
    return messages.filter{it.sender=="student"}.count{message->
        val lastRead=store.s(rsTrainerReadKeyV44(message.studentEmail),"0").toLongOrNull()?:0L
        message.createdAt>lastRead
    }
}

fun rsStudentUnreadCoachCountV55(store:RsStore):Int{
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val lastRead=store.s(rsStudentReadKeyV44(email),"0").toLongOrNull()?:0L
    return rsLoadCoachMessagesV44(store).count{
        it.studentEmail.equals(email,true) && it.sender=="trainer" && it.createdAt>lastRead
    }
}

private fun rsCoachTimeV44(value:Long):String =
    if(value<=0L)"" else SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(Date(value))

private fun rsCoachUiV44(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "Private Coach Chat","student_sub" to "Private messages between you and your trainer.",
        "trainer_title" to "Coach Inbox","trainer_sub" to "Private student conversations and unread coaching messages.",
        "message" to "Message","send" to "Send","no_messages" to "No messages yet.",
        "start" to "Send the first private message to your trainer.","students" to "STUDENT THREADS",
        "no_students" to "No student conversations yet.","unread" to "unread","back" to "Back to inbox",
        "student" to "STUDENT","trainer" to "TRAINER","delete" to "Delete thread","confirm" to "Confirm",
        "empty_body" to "Write a message first."
    )
    val nl=en+mapOf("student_title" to "Privé Coach Chat","student_sub" to "Privéberichten tussen jou en je trainer.","trainer_title" to "Coach Inbox","trainer_sub" to "Privégesprekken met leerlingen en ongelezen coachberichten.","message" to "Bericht","send" to "Versturen","no_messages" to "Nog geen berichten.","start" to "Stuur het eerste privébericht naar je trainer.","students" to "LEERLINGGESPREKKEN","no_students" to "Nog geen leerlinggesprekken.","unread" to "ongelezen","back" to "Terug naar inbox","student" to "LEERLING","trainer" to "TRAINER","delete" to "Gesprek verwijderen","confirm" to "Bevestigen","empty_body" to "Schrijf eerst een bericht.")
    val pt=en+mapOf("student_title" to "Chat Privado Coach","student_sub" to "Mensagens privadas entre ti e o treinador.","trainer_title" to "Caixa do Treinador","trainer_sub" to "Conversas privadas e mensagens não lidas dos alunos.","message" to "Mensagem","send" to "Enviar","no_messages" to "Ainda não há mensagens.","start" to "Envia a primeira mensagem privada ao treinador.","students" to "CONVERSAS DOS ALUNOS","no_students" to "Ainda não há conversas.","unread" to "não lidas","back" to "Voltar à caixa","student" to "ALUNO","trainer" to "TREINADOR","delete" to "Eliminar conversa","confirm" to "Confirmar","empty_body" to "Escreve uma mensagem primeiro.")
    val es=en+mapOf("student_title" to "Chat Privado Coach","student_sub" to "Mensajes privados entre tú y tu entrenador.","trainer_title" to "Bandeja del Entrenador","trainer_sub" to "Conversaciones privadas y mensajes no leídos.","message" to "Mensaje","send" to "Enviar","no_messages" to "Aún no hay mensajes.","start" to "Envía el primer mensaje privado a tu entrenador.","students" to "CONVERSACIONES DE ALUMNOS","no_students" to "Aún no hay conversaciones.","unread" to "sin leer","back" to "Volver a bandeja","student" to "ALUMNO","trainer" to "ENTRENADOR","delete" to "Eliminar conversación","confirm" to "Confirmar","empty_body" to "Escribe un mensaje primero.")
    val fr=en+mapOf("student_title" to "Chat Coach Privé","student_sub" to "Messages privés entre toi et ton entraîneur.","trainer_title" to "Boîte Coach","trainer_sub" to "Conversations privées et messages élèves non lus.","message" to "Message","send" to "Envoyer","no_messages" to "Aucun message.","start" to "Envoie le premier message privé à ton entraîneur.","students" to "CONVERSATIONS ÉLÈVES","no_students" to "Aucune conversation.","unread" to "non lus","back" to "Retour à la boîte","student" to "ÉLÈVE","trainer" to "ENTRAÎNEUR","delete" to "Supprimer la conversation","confirm" to "Confirmer","empty_body" to "Écris d'abord un message.")
    val de=en+mapOf("student_title" to "Privater Coach-Chat","student_sub" to "Private Nachrichten zwischen dir und deinem Trainer.","trainer_title" to "Coach-Postfach","trainer_sub" to "Private Schülergespräche und ungelesene Nachrichten.","message" to "Nachricht","send" to "Senden","no_messages" to "Noch keine Nachrichten.","start" to "Sende die erste private Nachricht an deinen Trainer.","students" to "SCHÜLERGESPRÄCHE","no_students" to "Noch keine Gespräche.","unread" to "ungelesen","back" to "Zurück zum Postfach","student" to "SCHÜLER","trainer" to "TRAINER","delete" to "Gespräch löschen","confirm" to "Bestätigen","empty_body" to "Schreibe zuerst eine Nachricht.")
    val it=en+mapOf("student_title" to "Chat Coach Privato","student_sub" to "Messaggi privati tra te e il trainer.","trainer_title" to "Posta Coach","trainer_sub" to "Conversazioni private e messaggi allievi non letti.","message" to "Messaggio","send" to "Invia","no_messages" to "Nessun messaggio.","start" to "Invia il primo messaggio privato al trainer.","students" to "CONVERSAZIONI ALLIEVI","no_students" to "Nessuna conversazione.","unread" to "non letti","back" to "Torna alla posta","student" to "ALLIEVO","trainer" to "TRAINER","delete" to "Elimina conversazione","confirm" to "Conferma","empty_body" to "Scrivi prima un messaggio.")
    val pl=en+mapOf("student_title" to "Prywatny Chat Trenera","student_sub" to "Prywatne wiadomości między tobą a trenerem.","trainer_title" to "Skrzynka Trenera","trainer_sub" to "Prywatne rozmowy i nieprzeczytane wiadomości uczniów.","message" to "Wiadomość","send" to "Wyślij","no_messages" to "Brak wiadomości.","start" to "Wyślij pierwszą prywatną wiadomość do trenera.","students" to "ROZMOWY UCZNIÓW","no_students" to "Brak rozmów.","unread" to "nieprzeczytane","back" to "Powrót do skrzynki","student" to "UCZEŃ","trainer" to "TRENER","delete" to "Usuń rozmowę","confirm" to "Potwierdź","empty_body" to "Najpierw napisz wiadomość.")
    val tr=en+mapOf("student_title" to "Özel Koç Sohbeti","student_sub" to "Sen ve antrenörün arasındaki özel mesajlar.","trainer_title" to "Koç Gelen Kutusu","trainer_sub" to "Özel öğrenci konuşmaları ve okunmamış mesajlar.","message" to "Mesaj","send" to "Gönder","no_messages" to "Henüz mesaj yok.","start" to "Antrenörüne ilk özel mesajı gönder.","students" to "ÖĞRENCİ KONUŞMALARI","no_students" to "Henüz konuşma yok.","unread" to "okunmamış","back" to "Gelen kutusuna dön","student" to "ÖĞRENCİ","trainer" to "ANTRENÖR","delete" to "Konuşmayı sil","confirm" to "Onayla","empty_body" to "Önce bir mesaj yaz.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsCoachChatV44(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    if(RsSupabaseV60.configured){
        if(role==RsRole.TRAINER)RsCloudTrainerCoachInboxV72(c,lang)
        else RsCloudStudentCoachThreadV72(c,lang)
    }else{
        if(role==RsRole.TRAINER)RsTrainerCoachInboxV44(c,store,lang)
        else RsStudentCoachThreadV44(c,store,lang)
    }
}

@Composable
private fun RsMessageBubbleV44(c:RsPalette,lang:RsLang,message:RsCoachMessageV44){
    val trainer=message.sender=="trainer"
    Surface(
        color=if(trainer)c.gold.copy(alpha=.18f) else c.panel,
        shape=MaterialTheme.shapes.large,
        modifier=Modifier.fillMaxWidth()
    ){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            if(trainer){
                Text(
                    rsCoachUiV44(lang,"trainer"),
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=10.sp
                )
            }else{
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,message.studentEmail,message.studentName,size=32.dp)
                    Column{
                        Text(
                            message.studentName.ifBlank{rsCoachUiV44(lang,"student")},
                            color=c.bright,
                            fontWeight=FontWeight.Black,
                            fontSize=11.sp
                        )
                        Text(rsCoachUiV44(lang,"student"),color=c.muted,fontSize=8.sp)
                    }
                }
            }
            Text(message.body,color=c.text)
            Text(rsCoachTimeV44(message.createdAt),color=c.muted,fontSize=9.sp)
        }
    }
}

@Composable
private fun RsStudentCoachThreadV44(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var draft by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")
    val all=remember(revision){rsLoadCoachMessagesV44(store)}
    val thread=all.filter{it.studentEmail.equals(email,true)}.sortedBy{it.createdAt}
    val latestTrainer=thread.filter{it.sender=="trainer"}.maxOfOrNull{it.createdAt}?:0L
    LaunchedEffect(latestTrainer){
        if(latestTrainer>0L)store.ps(rsStudentReadKeyV44(email),latestTrainer.toString())
    }

    RsScroll(c,rsCoachUiV44(lang,"student_title"),rsCoachUiV44(lang,"student_sub")){
        if(thread.isEmpty())RsPanel(c){
            Text(rsCoachUiV44(lang,"no_messages"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsCoachUiV44(lang,"start"),color=c.muted)
        }
        thread.forEach{RsMessageBubbleV44(c,lang,it)}
        RsPanel(c){
            OutlinedTextField(
                value=draft,
                onValueChange={draft=it.take(1200)},
                label={Text(rsCoachUiV44(lang,"message"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=3,
                maxLines=8
            )
            Button(
                onClick={
                    val clean=draft.trim()
                    if(clean.isBlank()){
                        status=rsCoachUiV44(lang,"empty_body")
                    }else{
                        val updated=rsLoadCoachMessagesV44(store)+RsCoachMessageV44(
                            UUID.randomUUID().toString(),email,name,"student",clean,System.currentTimeMillis()
                        )
                        rsSaveCoachMessagesV44(store,updated)
                        draft=""
                        status=""
                        revision++
                    }
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsCoachUiV44(lang,"send"))}
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
    }
}

@Composable
private fun RsTrainerCoachInboxV44(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var selectedEmail by remember{mutableStateOf<String?>(null)}
    var draft by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val all=remember(revision){rsLoadCoachMessagesV44(store)}
    val localStudents=rsLoadStudentsV33(store).map{it.email to it.name.ifBlank{it.email}}
    val messageStudents=all.map{it.studentEmail to it.studentName.ifBlank{it.studentEmail}}
    val students=(localStudents+messageStudents).distinctBy{it.first.lowercase()}.sortedBy{it.second.lowercase()}

    fun save(items:List<RsCoachMessageV44>){
        rsSaveCoachMessagesV44(store,items)
        revision++
    }

    if(selectedEmail==null){
        RsScroll(c,rsCoachUiV44(lang,"trainer_title"),rsCoachUiV44(lang,"trainer_sub")){
            Text(rsCoachUiV44(lang,"students"),color=c.bright,fontWeight=FontWeight.Black)
            if(students.isEmpty())RsPanel(c){Text(rsCoachUiV44(lang,"no_students"),color=c.muted)}
            students.forEach{(email,name)->
                val thread=all.filter{it.studentEmail.equals(email,true)}
                val latestStudent=thread.filter{it.sender=="student"}.maxOfOrNull{it.createdAt}?:0L
                val trainerRead=store.s(rsTrainerReadKeyV44(email),"0").toLongOrNull()?:0L
                val unread=thread.count{it.sender=="student" && it.createdAt>trainerRead}
                RsPanel(c){
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(10.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        RsMemberAvatarV68(c,email,name,size=46.dp)
                        Column(Modifier.weight(1f)){
                            Text(name,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                            Text(email,color=c.muted,fontSize=10.sp)
                        }
                    }
                    Text(
                        if(unread>0)unread.toString()+" "+rsCoachUiV44(lang,"unread")
                        else thread.lastOrNull()?.body?.take(80)?:rsCoachUiV44(lang,"no_messages"),
                        color=if(unread>0)c.bright else c.text,
                        fontWeight=if(unread>0)FontWeight.Bold else FontWeight.Normal
                    )
                    Button(onClick={selectedEmail=email;pendingDelete=null},modifier=Modifier.fillMaxWidth()){
                        Text(rsRouteTitle(lang,"coachchat","Private Coach Chat"))
                    }
                }
            }
        }
    }else{
        val email=selectedEmail!!
        val pair=students.firstOrNull{it.first.equals(email,true)}
        val name=pair?.second?:email
        val thread=all.filter{it.studentEmail.equals(email,true)}.sortedBy{it.createdAt}
        val latestStudent=thread.filter{it.sender=="student"}.maxOfOrNull{it.createdAt}?:0L
        LaunchedEffect(email,latestStudent){
            if(latestStudent>0L)store.ps(rsTrainerReadKeyV44(email),latestStudent.toString())
        }
        RsScroll(c,name,email){
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,email,name,size=54.dp)
                    Column(Modifier.weight(1f)){
                        Text(name,color=c.bright,fontWeight=FontWeight.Black,fontSize=19.sp)
                        Text(email,color=c.muted,fontSize=10.sp)
                    }
                }
            }
            OutlinedButton(onClick={selectedEmail=null;draft="";pendingDelete=null},modifier=Modifier.fillMaxWidth()){
                Text(rsCoachUiV44(lang,"back"))
            }
            if(thread.isEmpty())RsPanel(c){Text(rsCoachUiV44(lang,"no_messages"),color=c.muted)}
            thread.forEach{RsMessageBubbleV44(c,lang,it)}
            RsPanel(c){
                OutlinedTextField(
                    value=draft,
                    onValueChange={draft=it.take(1200)},
                    label={Text(rsCoachUiV44(lang,"message"))},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=3,
                    maxLines=8
                )
                Button(
                    onClick={
                        val clean=draft.trim()
                        if(clean.isNotBlank()){
                            save(rsLoadCoachMessagesV44(store)+RsCoachMessageV44(
                                UUID.randomUUID().toString(),email,name,"trainer",clean,System.currentTimeMillis()
                            ))
                            draft=""
                        }
                    },
                    enabled=draft.trim().isNotBlank(),
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsCoachUiV44(lang,"send"))}
                OutlinedButton(
                    onClick={
                        if(pendingDelete==email){
                            save(rsLoadCoachMessagesV44(store).filterNot{it.studentEmail.equals(email,true)})
                            store.ps(rsTrainerReadKeyV44(email),"0")
                            store.ps(rsStudentReadKeyV44(email),"0")
                            pendingDelete=null
                            selectedEmail=null
                        }else pendingDelete=email
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==email)rsCoachUiV44(lang,"confirm") else rsCoachUiV44(lang,"delete"))}
            }
        }
    }
}


@Composable
private fun RsCloudCoachBubbleV72(
    c:RsPalette,
    lang:RsLang,
    message:RsCloudCoachMessageV72
){
    val trainer=message.senderRole=="trainer" || message.senderRole=="admin"
    Surface(
        color=if(trainer)c.gold.copy(alpha=.18f) else c.panel,
        shape=MaterialTheme.shapes.large,
        modifier=Modifier.fillMaxWidth()
    ){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            if(trainer){
                Text(
                    rsCoachUiV44(lang,"trainer"),
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=10.sp
                )
            }else{
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,message.studentEmail,message.studentName,size=32.dp)
                    Text(
                        message.studentName.ifBlank{rsCoachUiV44(lang,"student")},
                        color=c.bright,
                        fontWeight=FontWeight.Black,
                        fontSize=11.sp
                    )
                }
            }
            if(message.body.isNotBlank())Text(message.body,color=c.text)
            RsChatAttachmentPreviewV92(c,lang,message.mediaPath,message.mediaKind,message.mediaName)
            Text(rsCoachTimeV44(message.createdAtMillis()),color=c.muted,fontSize=9.sp)
        }
    }
}

@Composable
private fun RsCloudStudentCoachThreadV72(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var studentId by remember{mutableStateOf("")}
    var messages by remember{mutableStateOf<List<RsCloudCoachMessageV72>>(emptyList())}
    var draft by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var sending by remember{mutableStateOf(false)}
    var revision by remember{mutableIntStateOf(0)}

    LaunchedEffect(revision){
        loading=true
        rsCloudMyStudentIdV72()
            .onSuccess{id->
                studentId=id
                rsCloudCoachMessagesV72(id)
                    .onSuccess{messages=it}
                    .onFailure{status=it.message?:"Could not load coach messages."}
                rsCloudMarkCoachReadV72(id)
            }
            .onFailure{status=it.message?:"Could not resolve student account."}
        loading=false
    }

    RsScroll(c,rsCoachUiV44(lang,"student_title"),rsCoachUiV44(lang,"student_sub")){
        RsPanel(c){
            Text(
                if(loading)"Syncing private coach chat…" else "Private cloud chat connected",
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        if(messages.isEmpty()&&!loading)RsPanel(c){
            Text(rsCoachUiV44(lang,"no_messages"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsCoachUiV44(lang,"start"),color=c.muted)
        }

        messages.forEach{RsCloudCoachBubbleV72(c,lang,it)}

        if(studentId.isNotBlank()){
            RsChatComposerV92(
                c=c,
                lang=lang,
                scopeType="coach",
                scopeId=studentId,
                enabled=!loading,
                onSent={revision++},
                onStatus={status=it},
                onSend={body,attachment->rsCloudSendCoachMessageV72(studentId,body,attachment)}
            )
        }
    }
}

@Composable
private fun RsCloudTrainerCoachInboxV72(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var threads by remember{mutableStateOf<List<RsCloudCoachThreadV72>>(emptyList())}
    var selected by remember{mutableStateOf<RsCloudCoachThreadV72?>(null)}
    var messages by remember{mutableStateOf<List<RsCloudCoachMessageV72>>(emptyList())}
    var draft by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var sending by remember{mutableStateOf(false)}
    var revision by remember{mutableIntStateOf(0)}

    LaunchedEffect(revision,selected?.studentId){
        loading=true
        if(selected==null){
            rsCloudCoachThreadsV72()
                .onSuccess{threads=it}
                .onFailure{status=it.message?:"Could not load coach inbox."}
        }else{
            val id=selected!!.studentId
            rsCloudCoachMessagesV72(id)
                .onSuccess{messages=it}
                .onFailure{status=it.message?:"Could not load conversation."}
            rsCloudMarkCoachReadV72(id)
        }
        loading=false
    }

    if(selected==null){
        RsScroll(c,rsCoachUiV44(lang,"trainer_title"),rsCoachUiV44(lang,"trainer_sub")){
            RsPanel(c){
                Text(
                    if(loading)"Syncing coach inbox…" else "Cloud coach inbox connected",
                    color=if(loading)c.muted else c.bright,
                    fontWeight=FontWeight.Bold,
                    fontSize=10.sp
                )
                if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
            }
            Text(rsCoachUiV44(lang,"students"),color=c.bright,fontWeight=FontWeight.Black)
            if(threads.isEmpty()&&!loading)RsPanel(c){Text(rsCoachUiV44(lang,"no_students"),color=c.muted)}
            threads.forEach{thread->
                RsPanel(c){
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(10.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        RsMemberAvatarV68(c,thread.studentEmail,thread.studentName,size=48.dp)
                        Column(Modifier.weight(1f)){
                            Text(thread.studentName.ifBlank{thread.studentEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                            Text(thread.studentEmail,color=c.muted,fontSize=9.sp)
                            Text(
                                if(thread.unreadCount>0)thread.unreadCount.toString()+" "+rsCoachUiV44(lang,"unread")
                                else thread.lastMessage?.take(90)?:rsCoachUiV44(lang,"no_messages"),
                                color=if(thread.unreadCount>0)c.bright else c.text,
                                fontWeight=if(thread.unreadCount>0)FontWeight.Bold else FontWeight.Normal,
                                fontSize=10.sp
                            )
                        }
                    }
                    Button(
                        onClick={selected=thread;status="";revision++},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsRouteTitle(lang,"coachchat","Private Coach Chat"))}
                }
            }
        }
    }else{
        val thread=selected!!
        RsScroll(c,thread.studentName.ifBlank{thread.studentEmail},thread.studentEmail){
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,thread.studentEmail,thread.studentName,size=54.dp)
                    Column(Modifier.weight(1f)){
                        Text(thread.studentName.ifBlank{thread.studentEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=19.sp)
                        Text(thread.studentEmail,color=c.muted,fontSize=10.sp)
                    }
                }
            }
            OutlinedButton(
                onClick={selected=null;draft="";status="";messages=emptyList();revision++},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsCoachUiV44(lang,"back"))}

            if(status.isNotBlank())RsPanel(c){Text(status,color=c.muted,fontSize=10.sp)}
            if(messages.isEmpty()&&!loading)RsPanel(c){Text(rsCoachUiV44(lang,"no_messages"),color=c.muted)}
            messages.forEach{RsCloudCoachBubbleV72(c,lang,it)}

            RsChatComposerV92(
                c=c,
                lang=lang,
                scopeType="coach",
                scopeId=thread.studentId,
                enabled=!loading,
                onSent={revision++},
                onStatus={status=it},
                onSend={body,attachment->rsCloudSendCoachMessageV72(thread.studentId,body,attachment)}
            )
        }
    }
}

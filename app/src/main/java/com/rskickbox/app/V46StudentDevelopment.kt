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

data class RsHomeworkV46(
    val id:String,val studentEmail:String,val title:String,val details:String,
    val dueLabel:String,val completed:Boolean,val createdAt:Long
)

data class RsCoachNoteV46(
    val id:String,val studentEmail:String,val note:String,val createdAt:Long
)

data class RsAssessmentV46(
    val id:String,val studentEmail:String,
    val punches:Int,val kicks:Int,val defense:Int,val footwork:Int,
    val combinations:Int,val conditioning:Int,
    val summary:String,val createdAt:Long
)

private fun rsArrayV46(store:RsStore,key:String)=runCatching{JSONArray(store.s(key,""))}.getOrElse{JSONArray()}

fun rsLoadHomeworkV46(store:RsStore):List<RsHomeworkV46>{
    val a=rsArrayV46(store,"homework_v46")
    return buildList{
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            add(RsHomeworkV46(
                o.optString("id"),o.optString("email"),o.optString("title"),o.optString("details"),
                o.optString("due"),o.optBoolean("completed"),o.optLong("createdAt")
            ))
        }
    }
}
private fun rsSaveHomeworkV46(store:RsStore,items:List<RsHomeworkV46>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("title",x.title);put("details",x.details)
        put("due",x.dueLabel);put("completed",x.completed);put("createdAt",x.createdAt)
    })}
    store.ps("homework_v46",a.toString())
}

private fun rsLoadNotesV46(store:RsStore):List<RsCoachNoteV46>{
    val a=rsArrayV46(store,"coach_notes_v46")
    return buildList{
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            add(RsCoachNoteV46(o.optString("id"),o.optString("email"),o.optString("note"),o.optLong("createdAt")))
        }
    }
}
private fun rsSaveNotesV46(store:RsStore,items:List<RsCoachNoteV46>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("note",x.note);put("createdAt",x.createdAt)
    })}
    store.ps("coach_notes_v46",a.toString())
}

fun rsLoadAssessmentsV46(store:RsStore):List<RsAssessmentV46>{
    val a=rsArrayV46(store,"assessments_v46")
    return buildList{
        for(i in 0 until a.length()){
            val o=a.getJSONObject(i)
            add(RsAssessmentV46(
                o.optString("id"),o.optString("email"),
                o.optInt("punches",50),o.optInt("kicks",50),o.optInt("defense",50),
                o.optInt("footwork",50),o.optInt("combinations",50),o.optInt("conditioning",50),
                o.optString("summary"),o.optLong("createdAt")
            ))
        }
    }
}
private fun rsSaveAssessmentsV46(store:RsStore,items:List<RsAssessmentV46>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("punches",x.punches);put("kicks",x.kicks)
        put("defense",x.defense);put("footwork",x.footwork);put("combinations",x.combinations)
        put("conditioning",x.conditioning);put("summary",x.summary);put("createdAt",x.createdAt)
    })}
    store.ps("assessments_v46",a.toString())
}

fun rsHomeworkRawForStudentV46(store:RsStore,email:String):String{
    val a=JSONArray()
    rsLoadHomeworkV46(store).filter{it.studentEmail.equals(email,true)}.forEach{x->
        a.put(JSONObject().apply{
            put("id",x.id);put("title",x.title);put("details",x.details);put("due",x.dueLabel)
            put("completed",x.completed);put("createdAt",x.createdAt)
        })
    }
    return a.toString()
}

fun rsAssessmentsRawForStudentV46(store:RsStore,email:String):String{
    val a=JSONArray()
    rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}.forEach{x->
        a.put(JSONObject().apply{
            put("id",x.id);put("punches",x.punches);put("kicks",x.kicks);put("defense",x.defense)
            put("footwork",x.footwork);put("combinations",x.combinations);put("conditioning",x.conditioning)
            put("summary",x.summary);put("createdAt",x.createdAt)
        })
    }
    return a.toString()
}

fun rsCoachNotesRawForPrivacyV46(store:RsStore,email:String):String{
    val a=JSONArray()
    rsLoadNotesV46(store).filter{it.studentEmail.equals(email,true)}.forEach{x->
        a.put(JSONObject().apply{put("id",x.id);put("note",x.note);put("createdAt",x.createdAt)})
    }
    return a.toString()
}

fun rsRemoveDevelopmentDataForStudentV46(store:RsStore,email:String){
    rsSaveHomeworkV46(store,rsLoadHomeworkV46(store).filterNot{it.studentEmail.equals(email,true)})
    rsSaveNotesV46(store,rsLoadNotesV46(store).filterNot{it.studentEmail.equals(email,true)})
    rsSaveAssessmentsV46(store,rsLoadAssessmentsV46(store).filterNot{it.studentEmail.equals(email,true)})
}

private fun rsDateV46(ms:Long)=if(ms<=0L)"" else SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date(ms))

private fun rsDevUiV46(lang:RsLang,key:String):String{
    val en=mapOf(
        "homework" to "Homework","homework_sub" to "Your trainer assignments and completion status.",
        "manager" to "Homework Manager","manager_sub" to "Assign focused work to individual students.",
        "notes" to "Coach Notes","notes_sub" to "Private trainer notes linked to student development.",
        "assess" to "Assessments","assess_sub" to "Record skill scores and coaching feedback.",
        "progress" to "Progress","progress_sub" to "Latest assessed skill profile and development history.",
        "student" to "Student","title" to "Title","details" to "Details","due" to "Due","assign" to "Assign homework",
        "completed" to "Completed","open" to "Open","mark_done" to "Mark completed","reopen" to "Reopen",
        "delete" to "Delete","confirm" to "Confirm","new_note" to "New private coach note","save_note" to "Save note",
        "summary" to "Assessment summary","save_assessment" to "Save assessment","latest" to "LATEST ASSESSMENT",
        "history" to "ASSESSMENT HISTORY","none" to "No records yet."
    )
    val nl=en+mapOf(
        "homework" to "Huiswerk","homework_sub" to "Opdrachten van je trainer en voltooiingsstatus.","manager" to "Huiswerkbeheer","manager_sub" to "Wijs gericht werk toe aan individuele leerlingen.",
        "notes" to "Coachnotities","notes_sub" to "Privénotities van de trainer gekoppeld aan ontwikkeling.","assess" to "Beoordelingen","assess_sub" to "Leg vaardigheidsscores en coachfeedback vast.",
        "progress" to "Voortgang","progress_sub" to "Laatste vaardigheidsprofiel en ontwikkelingshistorie.","student" to "Leerling","title" to "Titel","details" to "Details","due" to "Deadline","assign" to "Huiswerk toewijzen",
        "completed" to "Voltooid","open" to "Open","mark_done" to "Markeer voltooid","reopen" to "Heropenen","delete" to "Verwijderen","confirm" to "Bevestigen",
        "new_note" to "Nieuwe privé coachnotitie","save_note" to "Notitie opslaan","summary" to "Samenvatting beoordeling","save_assessment" to "Beoordeling opslaan",
        "latest" to "LAATSTE BEOORDELING","history" to "BEOORDELINGSHISTORIE","none" to "Nog geen gegevens."
    )
    val pt=en+mapOf(
        "homework" to "Trabalho de Casa","homework_sub" to "Tarefas do teu treinador e estado de conclusão.","manager" to "Gestor de Tarefas","manager_sub" to "Atribui trabalho focado a alunos individuais.",
        "notes" to "Notas do Coach","notes_sub" to "Notas privadas do treinador ligadas ao desenvolvimento do aluno.","assess" to "Avaliações","assess_sub" to "Regista pontuações técnicas e feedback do treinador.",
        "progress" to "Progresso","progress_sub" to "Último perfil técnico avaliado e histórico de desenvolvimento.","student" to "Aluno","title" to "Título","details" to "Detalhes","due" to "Prazo","assign" to "Atribuir tarefa",
        "completed" to "Concluído","open" to "Aberto","mark_done" to "Marcar concluído","reopen" to "Reabrir","delete" to "Eliminar","confirm" to "Confirmar",
        "new_note" to "Nova nota privada do coach","save_note" to "Guardar nota","summary" to "Resumo da avaliação","save_assessment" to "Guardar avaliação",
        "latest" to "ÚLTIMA AVALIAÇÃO","history" to "HISTÓRICO DE AVALIAÇÕES","none" to "Ainda não existem registos."
    )
    val es=en+mapOf(
        "homework" to "Tareas","homework_sub" to "Tareas de tu entrenador y estado de finalización.","manager" to "Gestor de Tareas","manager_sub" to "Asigna trabajo específico a cada alumno.",
        "notes" to "Notas del Coach","notes_sub" to "Notas privadas del entrenador vinculadas al desarrollo.","assess" to "Evaluaciones","assess_sub" to "Registra puntuaciones y feedback técnico.",
        "progress" to "Progreso","progress_sub" to "Último perfil evaluado e historial de desarrollo.","student" to "Alumno","title" to "Título","details" to "Detalles","due" to "Fecha límite","assign" to "Asignar tarea",
        "completed" to "Completado","open" to "Abierto","mark_done" to "Marcar completado","reopen" to "Reabrir","delete" to "Eliminar","confirm" to "Confirmar",
        "new_note" to "Nueva nota privada del coach","save_note" to "Guardar nota","summary" to "Resumen de evaluación","save_assessment" to "Guardar evaluación",
        "latest" to "ÚLTIMA EVALUACIÓN","history" to "HISTORIAL DE EVALUACIONES","none" to "Aún no hay registros."
    )
    val fr=en+mapOf(
        "homework" to "Devoirs","homework_sub" to "Tes tâches entraîneur et leur état.","manager" to "Gestion des Devoirs","manager_sub" to "Attribue un travail ciblé à chaque élève.",
        "notes" to "Notes Coach","notes_sub" to "Notes privées de l’entraîneur liées au développement.","assess" to "Évaluations","assess_sub" to "Enregistre les scores techniques et le feedback.",
        "progress" to "Progression","progress_sub" to "Dernier profil évalué et historique de progression.","student" to "Élève","title" to "Titre","details" to "Détails","due" to "Échéance","assign" to "Attribuer le devoir",
        "completed" to "Terminé","open" to "Ouvert","mark_done" to "Marquer terminé","reopen" to "Rouvrir","delete" to "Supprimer","confirm" to "Confirmer",
        "new_note" to "Nouvelle note privée coach","save_note" to "Enregistrer la note","summary" to "Résumé de l’évaluation","save_assessment" to "Enregistrer l’évaluation",
        "latest" to "DERNIÈRE ÉVALUATION","history" to "HISTORIQUE DES ÉVALUATIONS","none" to "Aucun enregistrement."
    )
    val de=en+mapOf(
        "homework" to "Hausaufgaben","homework_sub" to "Traineraufgaben und Abschlussstatus.","manager" to "Hausaufgaben-Manager","manager_sub" to "Weise einzelnen Schülern gezielte Aufgaben zu.",
        "notes" to "Coach-Notizen","notes_sub" to "Private Trainernotizen zur Entwicklung.","assess" to "Bewertungen","assess_sub" to "Technikwerte und Coaching-Feedback erfassen.",
        "progress" to "Fortschritt","progress_sub" to "Letztes Bewertungsprofil und Entwicklungsverlauf.","student" to "Schüler","title" to "Titel","details" to "Details","due" to "Fällig","assign" to "Aufgabe zuweisen",
        "completed" to "Erledigt","open" to "Offen","mark_done" to "Als erledigt markieren","reopen" to "Wieder öffnen","delete" to "Löschen","confirm" to "Bestätigen",
        "new_note" to "Neue private Coach-Notiz","save_note" to "Notiz speichern","summary" to "Bewertungszusammenfassung","save_assessment" to "Bewertung speichern",
        "latest" to "LETZTE BEWERTUNG","history" to "BEWERTUNGSVERLAUF","none" to "Noch keine Einträge."
    )
    val it=en+mapOf(
        "homework" to "Compiti","homework_sub" to "Compiti assegnati dal trainer e stato di completamento.","manager" to "Gestione Compiti","manager_sub" to "Assegna lavoro mirato a singoli allievi.",
        "notes" to "Note Coach","notes_sub" to "Note private del trainer legate allo sviluppo.","assess" to "Valutazioni","assess_sub" to "Registra punteggi tecnici e feedback.",
        "progress" to "Progresso","progress_sub" to "Ultimo profilo valutato e storico dello sviluppo.","student" to "Allievo","title" to "Titolo","details" to "Dettagli","due" to "Scadenza","assign" to "Assegna compito",
        "completed" to "Completato","open" to "Aperto","mark_done" to "Segna completato","reopen" to "Riapri","delete" to "Elimina","confirm" to "Conferma",
        "new_note" to "Nuova nota privata coach","save_note" to "Salva nota","summary" to "Riepilogo valutazione","save_assessment" to "Salva valutazione",
        "latest" to "ULTIMA VALUTAZIONE","history" to "STORICO VALUTAZIONI","none" to "Nessun dato ancora."
    )
    val pl=en+mapOf(
        "homework" to "Zadania","homework_sub" to "Zadania od trenera i status wykonania.","manager" to "Menedżer Zadań","manager_sub" to "Przydzielaj konkretne zadania poszczególnym uczniom.",
        "notes" to "Notatki Trenera","notes_sub" to "Prywatne notatki trenera dotyczące rozwoju.","assess" to "Oceny","assess_sub" to "Zapisuj wyniki umiejętności i uwagi trenera.",
        "progress" to "Postęp","progress_sub" to "Najnowszy profil oceny i historia rozwoju.","student" to "Uczeń","title" to "Tytuł","details" to "Szczegóły","due" to "Termin","assign" to "Przydziel zadanie",
        "completed" to "Ukończone","open" to "Otwarte","mark_done" to "Oznacz jako ukończone","reopen" to "Otwórz ponownie","delete" to "Usuń","confirm" to "Potwierdź",
        "new_note" to "Nowa prywatna notatka trenera","save_note" to "Zapisz notatkę","summary" to "Podsumowanie oceny","save_assessment" to "Zapisz ocenę",
        "latest" to "NAJNOWSZA OCENA","history" to "HISTORIA OCEN","none" to "Brak danych."
    )
    val tr=en+mapOf(
        "homework" to "Ödevler","homework_sub" to "Antrenör görevlerin ve tamamlanma durumu.","manager" to "Ödev Yönetimi","manager_sub" to "Öğrencilere odaklı çalışmalar ata.",
        "notes" to "Koç Notları","notes_sub" to "Öğrenci gelişimine bağlı özel antrenör notları.","assess" to "Değerlendirmeler","assess_sub" to "Teknik puanları ve koç geri bildirimini kaydet.",
        "progress" to "İlerleme","progress_sub" to "Son değerlendirme profili ve gelişim geçmişi.","student" to "Öğrenci","title" to "Başlık","details" to "Detaylar","due" to "Son tarih","assign" to "Ödev ata",
        "completed" to "Tamamlandı","open" to "Açık","mark_done" to "Tamamlandı işaretle","reopen" to "Yeniden aç","delete" to "Sil","confirm" to "Onayla",
        "new_note" to "Yeni özel koç notu","save_note" to "Notu kaydet","summary" to "Değerlendirme özeti","save_assessment" to "Değerlendirmeyi kaydet",
        "latest" to "SON DEĞERLENDİRME","history" to "DEĞERLENDİRME GEÇMİŞİ","none" to "Henüz kayıt yok."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

fun rsStudentsV46(store:RsStore):List<Pair<String,String>> =
    (listOf("alex@rskickbox.nl" to "Alex de Vries")+
        rsLoadStudentsV33(store).map{it.email to it.name.ifBlank{it.email}})
        .distinctBy{it.first.lowercase()}

@Composable
fun RsStudentHomeworkV46(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudStudentHomeworkV80(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val items=remember(revision){rsLoadHomeworkV46(store).filter{it.studentEmail.equals(email,true)}.sortedByDescending{it.createdAt}}
    RsScroll(c,rsDevUiV46(lang,"homework"),rsDevUiV46(lang,"homework_sub")){
        if(items.isEmpty())RsPanel(c){Text(rsDevUiV46(lang,"none"),color=c.muted)}
        items.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(x.details,color=c.text)
                if(x.dueLabel.isNotBlank())Text(rsDevUiV46(lang,"due")+" · "+x.dueLabel,color=c.muted)
                Text(if(x.completed)rsDevUiV46(lang,"completed") else rsDevUiV46(lang,"open"),color=c.bright,fontWeight=FontWeight.Bold)
                Button(
                    onClick={
                        rsSaveHomeworkV46(store,rsLoadHomeworkV46(store).map{if(it.id==x.id)it.copy(completed=!it.completed) else it})
                        revision++
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(x.completed)rsDevUiV46(lang,"reopen") else rsDevUiV46(lang,"mark_done"))}
            }
        }
    }
}

@Composable
fun RsHomeworkManagerV46(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudHomeworkManagerV80(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    var selectedEmail by remember{mutableStateOf(rsStudentsV46(store).firstOrNull()?.first.orEmpty())}
    var title by remember{mutableStateOf("")}
    var details by remember{mutableStateOf("")}
    var due by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val students=rsStudentsV46(store)
    val items=remember(revision){rsLoadHomeworkV46(store).sortedByDescending{it.createdAt}}

    RsScroll(c,rsDevUiV46(lang,"manager"),rsDevUiV46(lang,"manager_sub")){
        RsPanel(c){
            Text(rsDevUiV46(lang,"student"),color=c.muted)
            students.forEach{(email,name)->
                FilterChip(selected=selectedEmail==email,onClick={selectedEmail=email},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsDevUiV46(lang,"title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(details,{details=it.take(1000)},label={Text(rsDevUiV46(lang,"details"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            OutlinedTextField(due,{due=it.take(40)},label={Text(rsDevUiV46(lang,"due"))},modifier=Modifier.fillMaxWidth())
            Button(
                onClick={
                    rsSaveHomeworkV46(store,listOf(RsHomeworkV46(UUID.randomUUID().toString(),selectedEmail,title.trim(),details.trim(),due.trim(),false,System.currentTimeMillis()))+items)
                    title="";details="";due="";revision++
                },
                enabled=selectedEmail.isNotBlank()&&title.isNotBlank()&&details.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsDevUiV46(lang,"assign"))}
        }
        items.forEach{x->
            val name=students.firstOrNull{it.first.equals(x.studentEmail,true)}?.second?:x.studentEmail
            RsPanel(c){
                Text(name+" · "+x.title,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.details,color=c.text)
                Text((if(x.completed)rsDevUiV46(lang,"completed") else rsDevUiV46(lang,"open"))+" · "+rsDateV46(x.createdAt),color=c.muted)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){
                            rsSaveHomeworkV46(store,items.filterNot{it.id==x.id});pendingDelete=null;revision++
                        }else pendingDelete=x.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsDevUiV46(lang,"confirm") else rsDevUiV46(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsCoachNotesV46(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudCoachNotesV80(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    var selectedEmail by remember{mutableStateOf(rsStudentsV46(store).firstOrNull()?.first.orEmpty())}
    var note by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val students=rsStudentsV46(store)
    val notes=remember(revision){rsLoadNotesV46(store).sortedByDescending{it.createdAt}}

    RsScroll(c,rsDevUiV46(lang,"notes"),rsDevUiV46(lang,"notes_sub")){
        RsPanel(c){
            students.forEach{(email,name)->
                FilterChip(selected=selectedEmail==email,onClick={selectedEmail=email},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(note,{note=it.take(1500)},label={Text(rsDevUiV46(lang,"new_note"))},modifier=Modifier.fillMaxWidth(),minLines=4)
            Button(
                onClick={
                    rsSaveNotesV46(store,listOf(RsCoachNoteV46(UUID.randomUUID().toString(),selectedEmail,note.trim(),System.currentTimeMillis()))+notes)
                    note="";revision++
                },
                enabled=selectedEmail.isNotBlank()&&note.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsDevUiV46(lang,"save_note"))}
        }
        notes.filter{selectedEmail.isBlank()||it.studentEmail.equals(selectedEmail,true)}.forEach{x->
            RsPanel(c){
                Text(students.firstOrNull{it.first.equals(x.studentEmail,true)}?.second?:x.studentEmail,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.note,color=c.text)
                Text(rsDateV46(x.createdAt),color=c.muted,fontSize=10.sp)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){rsSaveNotesV46(store,notes.filterNot{it.id==x.id});pendingDelete=null;revision++}
                        else pendingDelete=x.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsDevUiV46(lang,"confirm") else rsDevUiV46(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsAssessmentsV46(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudAssessmentsV80(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    val students=rsStudentsV46(store)
    var selectedEmail by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    var punches by remember{mutableFloatStateOf(84f)}
    var kicks by remember{mutableFloatStateOf(71f)}
    var defense by remember{mutableFloatStateOf(67f)}
    var footwork by remember{mutableFloatStateOf(62f)}
    var combos by remember{mutableFloatStateOf(76f)}
    var conditioning by remember{mutableFloatStateOf(69f)}
    var summary by remember{mutableStateOf("")}
    val items=remember(revision){rsLoadAssessmentsV46(store).sortedByDescending{it.createdAt}}

    @Composable fun slider(label:String,value:Float,onChange:(Float)->Unit){
        Text(label+" · "+value.toInt(),color=c.text)
        Slider(value,onValueChange=onChange,valueRange=0f..100f)
    }

    RsScroll(c,rsDevUiV46(lang,"assess"),rsDevUiV46(lang,"assess_sub")){
        RsPanel(c){
            students.forEach{(email,name)->
                FilterChip(selected=selectedEmail==email,onClick={selectedEmail=email},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            slider("Punches",punches){punches=it};slider("Kicks",kicks){kicks=it};slider("Defense",defense){defense=it}
            slider("Footwork",footwork){footwork=it};slider("Combinations",combos){combos=it};slider("Conditioning",conditioning){conditioning=it}
            OutlinedTextField(summary,{summary=it.take(1200)},label={Text(rsDevUiV46(lang,"summary"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Button(
                onClick={
                    rsSaveAssessmentsV46(store,listOf(RsAssessmentV46(
                        UUID.randomUUID().toString(),selectedEmail,punches.toInt(),kicks.toInt(),defense.toInt(),
                        footwork.toInt(),combos.toInt(),conditioning.toInt(),summary.trim(),System.currentTimeMillis()
                    ))+items)
                    summary="";revision++
                },
                enabled=selectedEmail.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsDevUiV46(lang,"save_assessment"))}
        }
    }
}

@Composable
fun RsStudentProgressV46(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudStudentProgressV80(c,lang);return}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val assessments=rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}.sortedByDescending{it.createdAt}
    val latest=assessments.firstOrNull()?:RsAssessmentV46("seed",email,84,71,67,62,76,69,"",0L)
    val metrics=listOf(
        "Punches" to latest.punches,"Kicks" to latest.kicks,"Defense" to latest.defense,
        "Footwork" to latest.footwork,"Combinations" to latest.combinations,"Conditioning" to latest.conditioning
    )
    RsScroll(c,rsDevUiV46(lang,"progress"),rsDevUiV46(lang,"progress_sub")){
        RsPanel(c){
            Text(rsDevUiV46(lang,"latest"),color=c.bright,fontWeight=FontWeight.Black)
            metrics.forEach{(name,value)->
                Text(name+" · "+value,color=c.text)
                LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth())
            }
            if(latest.summary.isNotBlank())Text(latest.summary,color=c.muted)
            if(latest.createdAt>0L)Text(rsDateV46(latest.createdAt),color=c.muted,fontSize=10.sp)
        }
        Text(rsDevUiV46(lang,"history"),color=c.bright,fontWeight=FontWeight.Black)
        assessments.forEach{x->
            RsPanel(c){
                val avg=(x.punches+x.kicks+x.defense+x.footwork+x.combinations+x.conditioning)/6
                Text(rsDateV46(x.createdAt)+" · "+avg+"/100",color=c.bright,fontWeight=FontWeight.Bold)
                if(x.summary.isNotBlank())Text(x.summary,color=c.text)
            }
        }
    }
}


@Composable
private fun RsCloudStudentHomeworkV80(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudHomeworkV80>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsHomeworkFeedV80()
            .onSuccess{items=it}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsDevUiV46(lang,"homework"),rsDevUiV46(lang,"homework_sub")){
        RsPanel(c){
            Text(if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        if(items.isEmpty()&&!loading)RsPanel(c){Text(rsDevUiV46(lang,"none"),color=c.muted)}
        items.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(x.details,color=c.text)
                if(x.dueLabel.isNotBlank())Text(rsDevUiV46(lang,"due")+" · "+x.dueLabel,color=c.muted)
                Text(if(x.completed)rsDevUiV46(lang,"completed") else rsDevUiV46(lang,"open"),color=c.bright,fontWeight=FontWeight.Bold)
                RsHomeworkStepsPanelV110(c,lang,x.id)
                Button(
                    onClick={
                        busyId=x.id
                        scope.launch{
                            rsSetHomeworkCompletedV80(x.id,!x.completed)
                                .onSuccess{revision++}
                                .onFailure{status=rsReleaseT98(lang,"update_failed")}
                            busyId=null
                        }
                    },
                    enabled=busyId==null,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(busyId==x.id)"Please wait…" else if(x.completed)rsDevUiV46(lang,"reopen") else rsDevUiV46(lang,"mark_done"))}
            }
        }
    }
}

@Composable
private fun RsCloudHomeworkManagerV80(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var students by remember{mutableStateOf<List<RsDevStudentV80>>(emptyList())}
    var items by remember{mutableStateOf<List<RsCloudHomeworkV80>>(emptyList())}
    var selectedId by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var details by remember{mutableStateOf("")}
    var due by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsDevelopmentStudentsV80().onSuccess{
            students=it
            if(selectedId.isBlank())selectedId=it.firstOrNull()?.id.orEmpty()
        }.onFailure{status=rsReleaseT98(lang,"load_failed")}
        rsHomeworkFeedV80().onSuccess{items=it}.onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsDevUiV46(lang,"manager"),rsDevUiV46(lang,"manager_sub")){
        RsPanel(c){
            Text(if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        RsPanel(c){
            students.forEach{s->
                FilterChip(selected=selectedId==s.id,onClick={selectedId=s.id},label={Text(s.displayName.ifBlank{s.email})},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsDevUiV46(lang,"title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(details,{details=it.take(1000)},label={Text(rsDevUiV46(lang,"details"))},modifier=Modifier.fillMaxWidth(),minLines=3,enabled=!busy)
            OutlinedTextField(due,{due=it.take(40)},label={Text(rsDevUiV46(lang,"due"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            Button(
                onClick={
                    busy=true
                    scope.launch{
                        rsAssignHomeworkV80(selectedId,title.trim(),details.trim(),due.trim())
                            .onSuccess{title="";details="";due="";status=rsCloudT93(lang,"homework_assigned");revision++}
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busy=false
                    }
                },
                enabled=!busy&&selectedId.isNotBlank()&&title.isNotBlank()&&details.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Saving…" else rsDevUiV46(lang,"assign"))}
        }

        RsTrainingPlanManagerPanelV110(c,lang,selectedId){revision++}

        items.forEach{x->
            RsPanel(c){
                Text(x.studentName.ifBlank{x.studentEmail}+" · "+x.title,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.details,color=c.text)
                Text((if(x.completed)rsDevUiV46(lang,"completed") else rsDevUiV46(lang,"open"))+" · "+x.dueLabel,color=c.muted)
                RsHomeworkStepsPanelV110(c,lang,x.id)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){
                            busy=true
                            scope.launch{
                                rsDeleteHomeworkV80(x.id).onSuccess{pendingDelete=null;revision++}.onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                busy=false
                            }
                        }else pendingDelete=x.id
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsDevUiV46(lang,"confirm") else rsDevUiV46(lang,"delete"))}
            }
        }
        RsStudentStoragePanelV110(c,lang)
    }
}

@Composable
private fun RsCloudCoachNotesV80(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var students by remember{mutableStateOf<List<RsDevStudentV80>>(emptyList())}
    var notes by remember{mutableStateOf<List<RsCloudCoachNoteV80>>(emptyList())}
    var selectedId by remember{mutableStateOf("")}
    var note by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsDevelopmentStudentsV80().onSuccess{students=it;if(selectedId.isBlank())selectedId=it.firstOrNull()?.id.orEmpty()}
        rsCoachNotesFeedV80().onSuccess{notes=it}.onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsDevUiV46(lang,"notes"),rsDevUiV46(lang,"notes_sub")){
        RsPanel(c){
            Text(if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            students.forEach{s->FilterChip(selected=selectedId==s.id,onClick={selectedId=s.id},label={Text(s.displayName.ifBlank{s.email})},modifier=Modifier.fillMaxWidth())}
            OutlinedTextField(note,{note=it.take(1500)},label={Text(rsDevUiV46(lang,"new_note"))},modifier=Modifier.fillMaxWidth(),minLines=4,enabled=!busy)
            Button(
                onClick={
                    busy=true
                    scope.launch{
                        rsAddCoachNoteV80(selectedId,note.trim())
                            .onSuccess{note="";status=rsCloudT93(lang,"coach_note_saved");revision++}
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busy=false
                    }
                },
                enabled=!busy&&selectedId.isNotBlank()&&note.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Saving…" else rsDevUiV46(lang,"save_note"))}
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        notes.filter{selectedId.isBlank()||it.studentId==selectedId}.forEach{x->
            RsPanel(c){
                Text(x.studentName.ifBlank{x.studentEmail},color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.note,color=c.text)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){
                            busy=true
                            scope.launch{
                                rsDeleteCoachNoteV80(x.id).onSuccess{pendingDelete=null;revision++}.onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                busy=false
                            }
                        }else pendingDelete=x.id
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsDevUiV46(lang,"confirm") else rsDevUiV46(lang,"delete"))}
            }
        }
    }
}

@Composable
private fun RsCloudAssessmentsV80(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var students by remember{mutableStateOf<List<RsDevStudentV80>>(emptyList())}
    var selectedId by remember{mutableStateOf("")}
    var punches by remember{mutableFloatStateOf(75f)}
    var kicks by remember{mutableFloatStateOf(75f)}
    var defense by remember{mutableFloatStateOf(75f)}
    var footwork by remember{mutableFloatStateOf(75f)}
    var combos by remember{mutableFloatStateOf(75f)}
    var conditioning by remember{mutableFloatStateOf(75f)}
    var summary by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsDevelopmentStudentsV80().onSuccess{students=it;if(selectedId.isBlank())selectedId=it.firstOrNull()?.id.orEmpty()}
        loading=false
    }

    @Composable fun metric(label:String,value:Float,onChange:(Float)->Unit){
        Text(label+" · "+value.toInt(),color=c.text)
        Slider(value,onValueChange=onChange,valueRange=0f..100f,enabled=!busy)
    }

    RsScroll(c,rsDevUiV46(lang,"assess"),rsDevUiV46(lang,"assess_sub")){
        RsPanel(c){
            Text(if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            students.forEach{s->FilterChip(selected=selectedId==s.id,onClick={selectedId=s.id},label={Text(s.displayName.ifBlank{s.email})},modifier=Modifier.fillMaxWidth())}
            metric("Punches",punches){punches=it};metric("Kicks",kicks){kicks=it};metric("Defense",defense){defense=it}
            metric("Footwork",footwork){footwork=it};metric("Combinations",combos){combos=it};metric("Conditioning",conditioning){conditioning=it}
            OutlinedTextField(summary,{summary=it.take(1200)},label={Text(rsDevUiV46(lang,"summary"))},modifier=Modifier.fillMaxWidth(),minLines=3,enabled=!busy)
            Button(
                onClick={
                    busy=true
                    scope.launch{
                        rsAddAssessmentV80(selectedId,punches.toInt(),kicks.toInt(),defense.toInt(),footwork.toInt(),combos.toInt(),conditioning.toInt(),summary.trim())
                            .onSuccess{summary="";status=rsCloudT93(lang,"assessment_saved");revision++}
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busy=false
                    }
                },
                enabled=!busy&&selectedId.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Saving…" else rsDevUiV46(lang,"save_assessment"))}
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
    }
}

@Composable
private fun RsCloudStudentProgressV80(c:RsPalette,lang:RsLang){
    var assessments by remember{mutableStateOf<List<RsCloudAssessmentV80>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(Unit){
        loading=true
        rsAssessmentFeedV80().onSuccess{assessments=it}.onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    val latest=assessments.firstOrNull()
    RsScroll(c,rsDevUiV46(lang,"progress"),rsDevUiV46(lang,"progress_sub")){
        RsPanel(c){
            Text(if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        if(latest==null&&!loading)RsPanel(c){Text(rsDevUiV46(lang,"none"),color=c.muted)}
        if(latest!=null){
            RsPanel(c){
                Text(rsDevUiV46(lang,"latest"),color=c.bright,fontWeight=FontWeight.Black)
                listOf(
                    "Punches" to latest.punches,"Kicks" to latest.kicks,"Defense" to latest.defense,
                    "Footwork" to latest.footwork,"Combinations" to latest.combinations,"Conditioning" to latest.conditioning
                ).forEach{(name,value)->
                    Text(name+" · "+value,color=c.text)
                    LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth())
                }
                if(latest.summary.isNotBlank())Text(latest.summary,color=c.muted)
            }
            Text(rsDevUiV46(lang,"history"),color=c.bright,fontWeight=FontWeight.Black)
            assessments.forEach{x->
                RsPanel(c){
                    val avg=(x.punches+x.kicks+x.defense+x.footwork+x.combinations+x.conditioning)/6
                    Text(avg.toString()+"/100",color=c.bright,fontWeight=FontWeight.Bold)
                    if(x.summary.isNotBlank())Text(x.summary,color=c.text)
                }
            }
        }
    }
}

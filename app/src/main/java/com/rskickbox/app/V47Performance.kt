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
import kotlinx.coroutines.launch

data class RsChallengeV47(
    val id:String,val studentEmail:String,val title:String,val target:Int,
    val current:Int,val unit:String,val active:Boolean
)

data class RsFightCampV47(
    val studentEmail:String,val currentWeek:Int,val totalWeeks:Int,
    val focus:String,val active:Boolean
)

fun rsLoadChallengesV47(store:RsStore):List<RsChallengeV47>{
    val raw=store.s("challenges_v47","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsChallengeV47(
                    o.optString("id"),o.optString("email"),o.optString("title"),
                    o.optInt("target",10).coerceAtLeast(1),o.optInt("current",0).coerceAtLeast(0),
                    o.optString("unit","sessions"),o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveChallengesV47(store:RsStore,items:List<RsChallengeV47>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("title",x.title);put("target",x.target)
        put("current",x.current);put("unit",x.unit);put("active",x.active)
    })}
    store.ps("challenges_v47",a.toString())
}

fun rsLoadFightCampsV47(store:RsStore):List<RsFightCampV47>{
    val raw=store.s("fightcamps_v47","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsFightCampV47(
                    o.optString("email"),o.optInt("week",3).coerceIn(1,8),
                    o.optInt("total",8).coerceIn(1,12),o.optString("focus","Technique & conditioning"),
                    o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveFightCampsV47(store:RsStore,items:List<RsFightCampV47>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("email",x.studentEmail);put("week",x.currentWeek);put("total",x.totalWeeks)
        put("focus",x.focus);put("active",x.active)
    })}
    store.ps("fightcamps_v47",a.toString())
}

private fun rsPerformanceUiV47(lang:RsLang,key:String):String{
    val en=mapOf(
        "challenges" to "Challenges","challenge_sub" to "Track trainer-assigned performance goals.",
        "challenge_admin" to "Challenge Manager","challenge_admin_sub" to "Assign and manage student challenges.",
        "fightcamp" to "Fight Camp","fightcamp_sub" to "Your structured 8-week fight preparation plan.",
        "fightcamp_admin" to "Fight Camp Manager","fightcamp_admin_sub" to "Create and update student fight camps.",
        "badges" to "Badges","badges_sub" to "Achievements earned from real training progress.",
        "student" to "Student","title" to "Challenge title","target" to "Target","unit" to "Unit",
        "assign" to "Assign challenge","active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm",
        "progress" to "Progress","complete" to "Completed","increase" to "+1 progress",
        "week" to "Current week","focus" to "Camp focus","save" to "Save Fight Camp",
        "none" to "No active items yet.","earned" to "EARNED","locked" to "LOCKED","week_label" to "Week","completed_block" to "Completed block","current_block" to "Current training block","upcoming_block" to "Upcoming block"
    )
    val nl=en+mapOf("challenges" to "Challenges","challenge_sub" to "Volg prestatiedoelen die je trainer heeft toegewezen.","challenge_admin" to "Challengebeheer","challenge_admin_sub" to "Wijs challenges toe en beheer ze per leerling.","fightcamp" to "Fight Camp","fightcamp_sub" to "Jouw gestructureerde 8-weekse voorbereiding.","fightcamp_admin" to "Fight Camp-beheer","fightcamp_admin_sub" to "Maak en beheer fight camps per leerling.","badges" to "Badges","badges_sub" to "Prestaties verdiend uit echte trainingsvoortgang.","student" to "Leerling","title" to "Challenge titel","target" to "Doel","unit" to "Eenheid","assign" to "Challenge toewijzen","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen","progress" to "Voortgang","complete" to "Voltooid","increase" to "+1 voortgang","week" to "Huidige week","focus" to "Camp-focus","save" to "Fight Camp opslaan","none" to "Nog geen actieve items.","earned" to "VERDIEND","locked" to "VERGRENDELD","week_label" to "Week","completed_block" to "Voltooid blok","current_block" to "Huidig trainingsblok","upcoming_block" to "Komend blok")
    val pt=en+mapOf("challenges" to "Desafios","challenge_sub" to "Acompanha objetivos de desempenho atribuídos pelo treinador.","challenge_admin" to "Gestor de Desafios","challenge_admin_sub" to "Atribui e gere desafios dos alunos.","fightcamp" to "Fight Camp","fightcamp_sub" to "O teu plano estruturado de preparação de 8 semanas.","fightcamp_admin" to "Gestor Fight Camp","fightcamp_admin_sub" to "Cria e atualiza fight camps dos alunos.","badges" to "Distintivos","badges_sub" to "Conquistas ganhas através do progresso real de treino.","student" to "Aluno","title" to "Título do desafio","target" to "Objetivo","unit" to "Unidade","assign" to "Atribuir desafio","active" to "ATIVO","inactive" to "INATIVO","delete" to "Eliminar","confirm" to "Confirmar","progress" to "Progresso","complete" to "Concluído","increase" to "+1 progresso","week" to "Semana atual","focus" to "Foco do camp","save" to "Guardar Fight Camp","none" to "Ainda não existem itens ativos.","earned" to "CONQUISTADO","locked" to "BLOQUEADO","week_label" to "Semana","completed_block" to "Bloco concluído","current_block" to "Bloco atual","upcoming_block" to "Próximo bloco")
    val es=en+mapOf("challenges" to "Desafíos","challenge_sub" to "Sigue objetivos de rendimiento asignados por tu entrenador.","challenge_admin" to "Gestor de Desafíos","challenge_admin_sub" to "Asigna y gestiona desafíos de alumnos.","fightcamp" to "Fight Camp","fightcamp_sub" to "Tu plan estructurado de preparación de 8 semanas.","fightcamp_admin" to "Gestor Fight Camp","fightcamp_admin_sub" to "Crea y actualiza fight camps de alumnos.","badges" to "Insignias","badges_sub" to "Logros obtenidos con progreso real de entrenamiento.","student" to "Alumno","title" to "Título del desafío","target" to "Objetivo","unit" to "Unidad","assign" to "Asignar desafío","active" to "ACTIVO","inactive" to "INACTIVO","delete" to "Eliminar","confirm" to "Confirmar","progress" to "Progreso","complete" to "Completado","increase" to "+1 progreso","week" to "Semana actual","focus" to "Enfoque del camp","save" to "Guardar Fight Camp","none" to "Aún no hay elementos activos.","earned" to "CONSEGUIDO","locked" to "BLOQUEADO","week_label" to "Semana","completed_block" to "Bloque completado","current_block" to "Bloque actual","upcoming_block" to "Próximo bloque")
    val fr=en+mapOf("challenges" to "Défis","challenge_sub" to "Suis les objectifs de performance attribués par ton entraîneur.","challenge_admin" to "Gestion des Défis","challenge_admin_sub" to "Attribue et gère les défis des élèves.","fightcamp" to "Fight Camp","fightcamp_sub" to "Ton plan structuré de préparation sur 8 semaines.","fightcamp_admin" to "Gestion Fight Camp","fightcamp_admin_sub" to "Crée et mets à jour les fight camps des élèves.","badges" to "Badges","badges_sub" to "Récompenses gagnées grâce aux vrais progrès d’entraînement.","student" to "Élève","title" to "Titre du défi","target" to "Objectif","unit" to "Unité","assign" to "Attribuer le défi","active" to "ACTIF","inactive" to "INACTIF","delete" to "Supprimer","confirm" to "Confirmer","progress" to "Progression","complete" to "Terminé","increase" to "+1 progression","week" to "Semaine actuelle","focus" to "Objectif du camp","save" to "Enregistrer Fight Camp","none" to "Aucun élément actif.","earned" to "OBTENU","locked" to "VERROUILLÉ","week_label" to "Semaine","completed_block" to "Bloc terminé","current_block" to "Bloc actuel","upcoming_block" to "Bloc à venir")
    val de=en+mapOf("challenges" to "Challenges","challenge_sub" to "Verfolge vom Trainer zugewiesene Leistungsziele.","challenge_admin" to "Challenge-Manager","challenge_admin_sub" to "Weise Schülern Challenges zu und verwalte sie.","fightcamp" to "Fight Camp","fightcamp_sub" to "Dein strukturierter 8-Wochen-Vorbereitungsplan.","fightcamp_admin" to "Fight-Camp-Manager","fightcamp_admin_sub" to "Erstelle und aktualisiere Fight Camps der Schüler.","badges" to "Abzeichen","badges_sub" to "Erfolge aus echtem Trainingsfortschritt.","student" to "Schüler","title" to "Challenge-Titel","target" to "Ziel","unit" to "Einheit","assign" to "Challenge zuweisen","active" to "AKTIV","inactive" to "INAKTIV","delete" to "Löschen","confirm" to "Bestätigen","progress" to "Fortschritt","complete" to "Abgeschlossen","increase" to "+1 Fortschritt","week" to "Aktuelle Woche","focus" to "Camp-Fokus","save" to "Fight Camp speichern","none" to "Noch keine aktiven Einträge.","earned" to "VERDIENT","locked" to "GESPERRT","week_label" to "Woche","completed_block" to "Abgeschlossener Block","current_block" to "Aktueller Trainingsblock","upcoming_block" to "Kommender Block")
    val it=en+mapOf("challenges" to "Sfide","challenge_sub" to "Segui gli obiettivi di performance assegnati dal trainer.","challenge_admin" to "Gestione Sfide","challenge_admin_sub" to "Assegna e gestisci le sfide degli allievi.","fightcamp" to "Fight Camp","fightcamp_sub" to "Il tuo piano strutturato di preparazione di 8 settimane.","fightcamp_admin" to "Gestione Fight Camp","fightcamp_admin_sub" to "Crea e aggiorna i fight camp degli allievi.","badges" to "Badge","badges_sub" to "Traguardi ottenuti dal vero progresso in allenamento.","student" to "Allievo","title" to "Titolo sfida","target" to "Obiettivo","unit" to "Unità","assign" to "Assegna sfida","active" to "ATTIVO","inactive" to "INATTIVO","delete" to "Elimina","confirm" to "Conferma","progress" to "Progresso","complete" to "Completato","increase" to "+1 progresso","week" to "Settimana attuale","focus" to "Focus del camp","save" to "Salva Fight Camp","none" to "Nessun elemento attivo.","earned" to "OTTENUTO","locked" to "BLOCCATO","week_label" to "Settimana","completed_block" to "Blocco completato","current_block" to "Blocco attuale","upcoming_block" to "Blocco successivo")
    val pl=en+mapOf("challenges" to "Wyzwania","challenge_sub" to "Śledź cele wydajności przydzielone przez trenera.","challenge_admin" to "Menedżer Wyzwań","challenge_admin_sub" to "Przydzielaj i zarządzaj wyzwaniami uczniów.","fightcamp" to "Fight Camp","fightcamp_sub" to "Twój 8-tygodniowy plan przygotowania.","fightcamp_admin" to "Menedżer Fight Camp","fightcamp_admin_sub" to "Twórz i aktualizuj fight campy uczniów.","badges" to "Odznaki","badges_sub" to "Osiągnięcia zdobyte dzięki realnemu postępowi.","student" to "Uczeń","title" to "Tytuł wyzwania","target" to "Cel","unit" to "Jednostka","assign" to "Przydziel wyzwanie","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","delete" to "Usuń","confirm" to "Potwierdź","progress" to "Postęp","complete" to "Ukończone","increase" to "+1 postęp","week" to "Aktualny tydzień","focus" to "Fokus campu","save" to "Zapisz Fight Camp","none" to "Brak aktywnych elementów.","earned" to "ZDOBYTE","locked" to "ZABLOKOWANE","week_label" to "Tydzień","completed_block" to "Ukończony blok","current_block" to "Bieżący blok treningowy","upcoming_block" to "Nadchodzący blok")
    val tr=en+mapOf("challenges" to "Görevler","challenge_sub" to "Antrenör tarafından atanan performans hedeflerini takip et.","challenge_admin" to "Görev Yönetimi","challenge_admin_sub" to "Öğrenci görevlerini ata ve yönet.","fightcamp" to "Fight Camp","fightcamp_sub" to "Yapılandırılmış 8 haftalık dövüş hazırlık planın.","fightcamp_admin" to "Fight Camp Yönetimi","fightcamp_admin_sub" to "Öğrenci fight camplerini oluştur ve güncelle.","badges" to "Rozetler","badges_sub" to "Gerçek antrenman ilerlemesinden kazanılan başarılar.","student" to "Öğrenci","title" to "Görev başlığı","target" to "Hedef","unit" to "Birim","assign" to "Görev ata","active" to "AKTİF","inactive" to "PASİF","delete" to "Sil","confirm" to "Onayla","progress" to "İlerleme","complete" to "Tamamlandı","increase" to "+1 ilerleme","week" to "Mevcut hafta","focus" to "Camp odağı","save" to "Fight Camp kaydet","none" to "Henüz aktif öğe yok.","earned" to "KAZANILDI","locked" to "KİLİTLİ","week_label" to "Hafta","completed_block" to "Tamamlanan blok","current_block" to "Mevcut antrenman bloğu","upcoming_block" to "Yaklaşan blok")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsStudentChallengesV47(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudStudentChallengesV83(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val items=remember(revision){rsLoadChallengesV47(store).filter{it.studentEmail.equals(email,true)&&it.active}}
    RsScroll(c,rsPerformanceUiV47(lang,"challenges"),rsPerformanceUiV47(lang,"challenge_sub")){
        if(items.isEmpty())RsPanel(c){Text(rsPerformanceUiV47(lang,"none"),color=c.muted)}
        items.forEach{x->
            val current=x.current.coerceAtMost(x.target)
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(current.toString()+" / "+x.target+" "+x.unit,color=c.text)
                LinearProgressIndicator(progress={current.toFloat()/x.target},modifier=Modifier.fillMaxWidth())
                Text(if(current>=x.target)rsPerformanceUiV47(lang,"complete") else rsPerformanceUiV47(lang,"progress"),color=c.muted)
                if(current<x.target)Button(
                    onClick={
                        rsSaveChallengesV47(store,rsLoadChallengesV47(store).map{
                            if(it.id==x.id)it.copy(current=(it.current+1).coerceAtMost(it.target)) else it
                        })
                        revision++
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPerformanceUiV47(lang,"increase"))}
            }
        }
    }
}

@Composable
fun RsChallengeManagerV47(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudChallengeManagerV83(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    val students=rsStudentsV46(store)
    var email by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    var title by remember{mutableStateOf("")}
    var target by remember{mutableStateOf("10")}
    var unit by remember{mutableStateOf("sessions")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val items=remember(revision){rsLoadChallengesV47(store)}
    fun save(list:List<RsChallengeV47>){rsSaveChallengesV47(store,list);revision++}

    RsScroll(c,rsPerformanceUiV47(lang,"challenge_admin"),rsPerformanceUiV47(lang,"challenge_admin_sub")){
        RsPanel(c){
            students.forEach{(e,name)->
                FilterChip(selected=email==e,onClick={email=e},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsPerformanceUiV47(lang,"title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(target,{target=it.filter(Char::isDigit)},label={Text(rsPerformanceUiV47(lang,"target"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(unit,{unit=it.take(30)},label={Text(rsPerformanceUiV47(lang,"unit"))},modifier=Modifier.fillMaxWidth())
            Button(
                onClick={
                    save(listOf(RsChallengeV47(UUID.randomUUID().toString(),email,title.trim(),target.toIntOrNull()?.coerceIn(1,10000)?:10,0,unit.trim().ifBlank{"sessions"},true))+items)
                    title="";target="10";unit="sessions"
                },
                enabled=email.isNotBlank()&&title.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPerformanceUiV47(lang,"assign"))}
        }
        items.forEach{x->
            RsPanel(c){
                Text(students.firstOrNull{it.first.equals(x.studentEmail,true)}?.second?:x.studentEmail,color=c.bright,fontWeight=FontWeight.Bold)
                Text(x.title+" · "+x.current+"/"+x.target+" "+x.unit,color=c.text)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(x.active)rsPerformanceUiV47(lang,"active") else rsPerformanceUiV47(lang,"inactive"),color=c.muted)
                    Switch(x.active,{v->save(items.map{if(it.id==x.id)it.copy(active=v) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){save(items.filterNot{it.id==x.id});pendingDelete=null}
                        else pendingDelete=x.id
                    },modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsPerformanceUiV47(lang,"confirm") else rsPerformanceUiV47(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsStudentFightCampV47(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudStudentFightCampV83(c,lang);return}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val camp=rsLoadFightCampsV47(store).firstOrNull{it.studentEmail.equals(email,true)&&it.active}
        ?:RsFightCampV47(email,3,8,"Technique · Conditioning · Recovery",true)
    RsScroll(c,rsPerformanceUiV47(lang,"fightcamp"),rsPerformanceUiV47(lang,"fightcamp_sub")){
        RsPanel(c){
            Text(rsPerformanceUiV47(lang,"week_label").uppercase()+" "+camp.currentWeek+" / "+camp.totalWeeks,color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black)
            LinearProgressIndicator(progress={camp.currentWeek.toFloat()/camp.totalWeeks},modifier=Modifier.fillMaxWidth())
            Text(camp.focus,color=c.text)
        }
        (1..camp.totalWeeks).forEach{week->
            RsPanel(c){
                Text(rsPerformanceUiV47(lang,"week_label")+" "+week,color=if(week<=camp.currentWeek)c.bright else c.muted,fontWeight=FontWeight.Bold)
                Text(
                    when{
                        week<camp.currentWeek->rsPerformanceUiV47(lang,"completed_block")
                        week==camp.currentWeek->rsPerformanceUiV47(lang,"current_block")
                        else->rsPerformanceUiV47(lang,"upcoming_block")
                    },
                    color=c.muted
                )
            }
        }
    }
}

@Composable
fun RsFightCampManagerV47(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudFightCampManagerV83(c,lang);return}
    var revision by remember{mutableIntStateOf(0)}
    val students=rsStudentsV46(store)
    var email by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    val camps=remember(revision){rsLoadFightCampsV47(store)}
    val selected=camps.firstOrNull{it.studentEmail.equals(email,true)}
    var week by remember(email,selected?.currentWeek){mutableFloatStateOf((selected?.currentWeek?:3).toFloat())}
    var focus by remember(email,selected?.focus){mutableStateOf(selected?.focus?:"Technique · Conditioning · Recovery")}
    var active by remember(email,selected?.active){mutableStateOf(selected?.active?:true)}

    RsScroll(c,rsPerformanceUiV47(lang,"fightcamp_admin"),rsPerformanceUiV47(lang,"fightcamp_admin_sub")){
        RsPanel(c){
            students.forEach{(e,name)->
                FilterChip(selected=email==e,onClick={email=e},label={Text(name)},modifier=Modifier.fillMaxWidth())
            }
            Text(rsPerformanceUiV47(lang,"week")+" · "+week.toInt()+"/8",color=c.text)
            Slider(week,{week=it},valueRange=1f..8f,steps=6)
            OutlinedTextField(focus,{focus=it.take(500)},label={Text(rsPerformanceUiV47(lang,"focus"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(active)rsPerformanceUiV47(lang,"active") else rsPerformanceUiV47(lang,"inactive"),color=c.muted)
                Switch(active,{active=it})
            }
            Button(
                onClick={
                    val updated=camps.filterNot{it.studentEmail.equals(email,true)}+
                        RsFightCampV47(email,week.toInt().coerceIn(1,8),8,focus.trim(),active)
                    rsSaveFightCampsV47(store,updated)
                    revision++
                },
                enabled=email.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPerformanceUiV47(lang,"save"))}
        }
    }
}

@Composable
fun RsBadgesV47(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudBadgesV83(c,lang);return}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val challenges=rsLoadChallengesV47(store).filter{it.studentEmail.equals(email,true)}
    val completed=challenges.count{it.current>=it.target}
    val assessments=rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}
    val latest=assessments.maxByOrNull{it.createdAt}
    val avg=latest?.let{(it.punches+it.kicks+it.defense+it.footwork+it.combinations+it.conditioning)/6}?:0
    val camp=rsLoadFightCampsV47(store).firstOrNull{it.studentEmail.equals(email,true)}
    val badges=listOf(
        Triple("First Challenge","Complete 1 trainer challenge",completed>=1),
        Triple("Challenge Hunter","Complete 3 trainer challenges",completed>=3),
        Triple("Technical 70","Reach 70+ average assessment",avg>=70),
        Triple("Technical 80","Reach 80+ average assessment",avg>=80),
        Triple("Fight Camp","Reach week 4 of Fight Camp",(camp?.currentWeek?:0)>=4),
        Triple("Camp Finisher","Reach week 8 of Fight Camp",(camp?.currentWeek?:0)>=8)
    )
    RsScroll(c,rsPerformanceUiV47(lang,"badges"),rsPerformanceUiV47(lang,"badges_sub")){
        badges.forEach{(name,desc,earned)->
            RsPanel(c){
                Text((if(earned)"★ " else "☆ ")+name,color=if(earned)c.bright else c.muted,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(desc,color=c.text)
                Text(if(earned)rsPerformanceUiV47(lang,"earned") else rsPerformanceUiV47(lang,"locked"),color=c.muted)
            }
        }
    }
}


@Composable
private fun RsCloudStudentChallengesV83(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudChallengeV83>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsCloudChallengesV83()
            .onSuccess{items=it.filter{row->row.active}}
            .onFailure{status=it.message?:"Could not load challenges."}
        loading=false
    }

    RsScroll(c,rsPerformanceUiV47(lang,"challenges"),"Live trainer-assigned goals synchronized with your account."){
        RsPanel(c){
            Text(if(loading)"Syncing challenges…" else "Cloud challenges connected",color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        if(items.isEmpty()&&!loading)RsPanel(c){Text(rsPerformanceUiV47(lang,"none"),color=c.muted)}
        items.forEach{x->
            val current=x.current.coerceAtMost(x.target)
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(current.toString()+" / "+x.target+" "+x.unit,color=c.text)
                LinearProgressIndicator(progress={current.toFloat()/x.target},modifier=Modifier.fillMaxWidth())
                Text(if(current>=x.target)rsPerformanceUiV47(lang,"complete") else rsPerformanceUiV47(lang,"progress"),color=c.muted)
                if(current<x.target)Button(
                    onClick={
                        busyId=x.id
                        scope.launch{
                            rsIncreaseCloudChallengeV83(x.id)
                                .onSuccess{revision++}
                                .onFailure{status=it.message?:"Could not update challenge."}
                            busyId=null
                        }
                    },
                    enabled=busyId==null,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(busyId==x.id)"Saving…" else rsPerformanceUiV47(lang,"increase"))}
            }
        }
    }
}

@Composable
private fun RsCloudChallengeManagerV83(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var students by remember{mutableStateOf<List<RsDevStudentV80>>(emptyList())}
    var items by remember{mutableStateOf<List<RsCloudChallengeV83>>(emptyList())}
    var selectedId by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var target by remember{mutableStateOf("10")}
    var unit by remember{mutableStateOf("sessions")}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsDevelopmentStudentsV80()
            .onSuccess{students=it;if(selectedId.isBlank())selectedId=it.firstOrNull()?.id.orEmpty()}
            .onFailure{status=it.message?:"Could not load students."}
        rsCloudChallengesV83()
            .onSuccess{items=it}
            .onFailure{status=it.message?:"Could not load challenges."}
        loading=false
    }

    RsScroll(c,rsPerformanceUiV47(lang,"challenge_admin"),"Assign and manage cloud challenges for every student."){
        RsPanel(c){
            Text(if(loading)"Syncing Challenge Manager…" else "Cloud Challenge Manager connected",color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        RsPanel(c){
            students.forEach{s->
                FilterChip(
                    selected=selectedId==s.id,
                    onClick={selectedId=s.id},
                    label={Text(s.displayName.ifBlank{s.email})},
                    modifier=Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(title,{title=it.take(100)},label={Text(rsPerformanceUiV47(lang,"title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(target,{target=it.filter(Char::isDigit).take(5)},label={Text(rsPerformanceUiV47(lang,"target"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(unit,{unit=it.take(30)},label={Text(rsPerformanceUiV47(lang,"unit"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            Button(
                onClick={
                    busy=true
                    scope.launch{
                        rsAssignCloudChallengeV83(selectedId,title.trim(),target.toIntOrNull()?.coerceIn(1,10000)?:10,unit.trim().ifBlank{"sessions"})
                            .onSuccess{title="";target="10";unit="sessions";status=rsCloudT93(lang,"challenge_assigned");revision++}
                            .onFailure{status=it.message?:"Could not assign challenge."}
                        busy=false
                    }
                },
                enabled=!busy&&selectedId.isNotBlank()&&title.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Saving…" else rsPerformanceUiV47(lang,"assign"))}
        }

        items.forEach{x->
            RsPanel(c){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    RsMemberAvatarV68(c,x.studentEmail,x.studentName,size=42.dp)
                    Column(Modifier.weight(1f)){
                        Text(x.studentName.ifBlank{x.studentEmail},color=c.bright,fontWeight=FontWeight.Bold)
                        Text(x.title+" · "+x.current+"/"+x.target+" "+x.unit,color=c.text)
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(x.active)rsPerformanceUiV47(lang,"active") else rsPerformanceUiV47(lang,"inactive"),color=c.muted)
                    Switch(
                        x.active,
                        {value->
                            busy=true
                            scope.launch{
                                rsSetCloudChallengeActiveV83(x.id,value)
                                    .onSuccess{revision++}
                                    .onFailure{status=it.message?:"Could not update challenge."}
                                busy=false
                            }
                        },
                        enabled=!busy
                    )
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){
                            busy=true
                            scope.launch{
                                rsDeleteCloudChallengeV83(x.id)
                                    .onSuccess{pendingDelete=null;status=rsCloudT93(lang,"challenge_deleted");revision++}
                                    .onFailure{status=it.message?:"Could not delete challenge."}
                                busy=false
                            }
                        }else pendingDelete=x.id
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsPerformanceUiV47(lang,"confirm") else rsPerformanceUiV47(lang,"delete"))}
            }
        }
    }
}

@Composable
private fun RsCloudStudentFightCampV83(c:RsPalette,lang:RsLang){
    var camp by remember{mutableStateOf<RsCloudFightCampV83?>(null)}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(Unit){
        loading=true
        rsCloudFightCampsV83()
            .onSuccess{camp=it.firstOrNull{row->row.active}}
            .onFailure{status=it.message?:"Could not load Fight Camp."}
        loading=false
    }

    RsScroll(c,rsPerformanceUiV47(lang,"fightcamp"),"Your trainer-managed Fight Camp synced from the club backend."){
        RsPanel(c){
            Text(if(loading)"Syncing Fight Camp…" else "Cloud Fight Camp connected",color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        val active=camp
        if(active==null&&!loading)RsPanel(c){Text(rsPerformanceUiV47(lang,"none"),color=c.muted)}
        if(active!=null){
            RsPanel(c){
                Text(rsPerformanceUiV47(lang,"week_label").uppercase()+" "+active.currentWeek+" / "+active.totalWeeks,color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black)
                LinearProgressIndicator(progress={active.currentWeek.toFloat()/active.totalWeeks},modifier=Modifier.fillMaxWidth())
                Text(active.focus,color=c.text)
            }
            (1..active.totalWeeks).forEach{week->
                RsPanel(c){
                    Text(rsPerformanceUiV47(lang,"week_label")+" "+week,color=if(week<=active.currentWeek)c.bright else c.muted,fontWeight=FontWeight.Bold)
                    Text(
                        when{
                            week<active.currentWeek->rsPerformanceUiV47(lang,"completed_block")
                            week==active.currentWeek->rsPerformanceUiV47(lang,"current_block")
                            else->rsPerformanceUiV47(lang,"upcoming_block")
                        },
                        color=c.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun RsCloudFightCampManagerV83(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var students by remember{mutableStateOf<List<RsDevStudentV80>>(emptyList())}
    var camps by remember{mutableStateOf<List<RsCloudFightCampV83>>(emptyList())}
    var selectedId by remember{mutableStateOf("")}
    var week by remember{mutableFloatStateOf(1f)}
    var focus by remember{mutableStateOf("Technique · Conditioning · Recovery")}
    var active by remember{mutableStateOf(true)}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsDevelopmentStudentsV80().onSuccess{
            students=it
            if(selectedId.isBlank())selectedId=it.firstOrNull()?.id.orEmpty()
        }.onFailure{status=it.message?:"Could not load students."}
        rsCloudFightCampsV83().onSuccess{camps=it}.onFailure{status=it.message?:"Could not load Fight Camps."}
        loading=false
    }

    val selectedCamp=camps.firstOrNull{it.studentId==selectedId}
    LaunchedEffect(selectedId,selectedCamp?.currentWeek,selectedCamp?.focus,selectedCamp?.active){
        week=(selectedCamp?.currentWeek?:1).toFloat()
        focus=selectedCamp?.focus?:"Technique · Conditioning · Recovery"
        active=selectedCamp?.active?:true
    }

    RsScroll(c,rsPerformanceUiV47(lang,"fightcamp_admin"),"Create and update the same Fight Camp students see on their devices."){
        RsPanel(c){
            Text(if(loading)"Syncing Fight Camp Manager…" else "Cloud Fight Camp Manager connected",color=if(loading)c.muted else c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        RsPanel(c){
            students.forEach{s->
                FilterChip(selected=selectedId==s.id,onClick={selectedId=s.id},label={Text(s.displayName.ifBlank{s.email})},modifier=Modifier.fillMaxWidth())
            }
            Text(rsPerformanceUiV47(lang,"week")+" · "+week.toInt()+"/8",color=c.text)
            Slider(week,{week=it},valueRange=1f..8f,steps=6,enabled=!busy)
            OutlinedTextField(focus,{focus=it.take(500)},label={Text(rsPerformanceUiV47(lang,"focus"))},modifier=Modifier.fillMaxWidth(),minLines=3,enabled=!busy)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(active)rsPerformanceUiV47(lang,"active") else rsPerformanceUiV47(lang,"inactive"),color=c.muted)
                Switch(active,{active=it},enabled=!busy)
            }
            Button(
                onClick={
                    busy=true
                    scope.launch{
                        rsSetCloudFightCampV83(selectedId,week.toInt(),focus.trim(),active)
                            .onSuccess{status=rsCloudT93(lang,"fightcamp_saved");revision++}
                            .onFailure{status=it.message?:"Could not save Fight Camp."}
                        busy=false
                    }
                },
                enabled=!busy&&selectedId.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Saving…" else rsPerformanceUiV47(lang,"save"))}
        }
    }
}

@Composable
private fun RsCloudBadgesV83(c:RsPalette,lang:RsLang){
    var challenges by remember{mutableStateOf<List<RsCloudChallengeV83>>(emptyList())}
    var assessments by remember{mutableStateOf<List<RsCloudAssessmentV80>>(emptyList())}
    var camps by remember{mutableStateOf<List<RsCloudFightCampV83>>(emptyList())}
    var loading by remember{mutableStateOf(true)}

    LaunchedEffect(Unit){
        loading=true
        rsCloudChallengesV83().onSuccess{challenges=it}
        rsAssessmentFeedV80().onSuccess{assessments=it}
        rsCloudFightCampsV83().onSuccess{camps=it}
        loading=false
    }

    val completed=challenges.count{it.current>=it.target}
    val latest=assessments.firstOrNull()
    val avg=latest?.let{(it.punches+it.kicks+it.defense+it.footwork+it.combinations+it.conditioning)/6}?:0
    val camp=camps.firstOrNull()
    val badges=listOf(
        Triple("First Challenge","Complete 1 trainer challenge",completed>=1),
        Triple("Challenge Hunter","Complete 3 trainer challenges",completed>=3),
        Triple("Technical 70","Reach 70+ average assessment",avg>=70),
        Triple("Technical 80","Reach 80+ average assessment",avg>=80),
        Triple("Fight Camp","Reach week 4 of Fight Camp",(camp?.currentWeek?:0)>=4),
        Triple("Camp Finisher","Reach week 8 of Fight Camp",(camp?.currentWeek?:0)>=8)
    )

    RsScroll(c,rsPerformanceUiV47(lang,"badges"),"Achievements calculated from your synchronized RS training records."){
        if(loading)RsPanel(c){Text(rsCloudT93(lang,"syncing_badges"),color=c.muted)}
        badges.forEach{(name,desc,earned)->
            RsPanel(c){
                Text((if(earned)"★ " else "☆ ")+name,color=if(earned)c.bright else c.muted,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(desc,color=c.text)
                Text(if(earned)rsPerformanceUiV47(lang,"earned") else rsPerformanceUiV47(lang,"locked"),color=c.muted)
            }
        }
    }
}

package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun rsMoneyV53(cents:Int)="€"+String.format("%.2f",cents/100.0)

private fun rsAnalyticsUiV53(lang:RsLang,key:String):String{
    val en=mapOf(
        "analytics" to "Analytics","analytics_sub" to "Live local operating metrics from the connected RS KICKBOX modules.",
        "students" to "Active students","revenue" to "Collected revenue","pending" to "Pending invoices",
        "occupancy" to "Class occupancy","support" to "Open support","homework" to "Open homework",
        "challenges" to "Challenge completion","fightcamp" to "Active Fight Camps",
        "progress" to "Progress Manager","progress_sub" to "One student view across assessments, homework, challenges and Fight Camp.",
        "latest" to "Latest assessment","none" to "No records yet.","plan" to "Membership"
    )
    val nl=en+mapOf("analytics" to "Analytics","analytics_sub" to "Live lokale bedrijfsmetingen uit de gekoppelde RS KICKBOX-modules.","students" to "Actieve leerlingen","revenue" to "Ontvangen omzet","pending" to "Openstaande facturen","occupancy" to "Lesbezetting","support" to "Open support","homework" to "Open huiswerk","challenges" to "Challenge-voltooiing","fightcamp" to "Actieve Fight Camps","progress" to "Voortgangsbeheer","progress_sub" to "Eén leerlingoverzicht voor beoordelingen, huiswerk, challenges en Fight Camp.","latest" to "Laatste beoordeling","none" to "Nog geen gegevens.","plan" to "Lidmaatschap")
    val pt=en+mapOf("analytics" to "Analítica","analytics_sub" to "Métricas locais ao vivo dos módulos RS KICKBOX ligados.","students" to "Alunos ativos","revenue" to "Receita recebida","pending" to "Faturas pendentes","occupancy" to "Ocupação das aulas","support" to "Suporte aberto","homework" to "Tarefas abertas","challenges" to "Conclusão de desafios","fightcamp" to "Fight Camps ativos","progress" to "Gestor de Progresso","progress_sub" to "Uma vista por aluno com avaliações, tarefas, desafios e Fight Camp.","latest" to "Última avaliação","none" to "Ainda não há registos.","plan" to "Adesão")
    val es=en+mapOf("analytics" to "Analítica","analytics_sub" to "Métricas locales en vivo de los módulos RS KICKBOX conectados.","students" to "Alumnos activos","revenue" to "Ingresos cobrados","pending" to "Facturas pendientes","occupancy" to "Ocupación de clases","support" to "Soporte abierto","homework" to "Tareas abiertas","challenges" to "Finalización de desafíos","fightcamp" to "Fight Camps activos","progress" to "Gestor de Progreso","progress_sub" to "Vista por alumno de evaluaciones, tareas, desafíos y Fight Camp.","latest" to "Última evaluación","none" to "Aún no hay registros.","plan" to "Membresía")
    val fr=en+mapOf("analytics" to "Analytique","analytics_sub" to "Indicateurs locaux en direct des modules RS KICKBOX connectés.","students" to "Élèves actifs","revenue" to "Revenus encaissés","pending" to "Factures en attente","occupancy" to "Occupation des cours","support" to "Support ouvert","homework" to "Devoirs ouverts","challenges" to "Défis terminés","fightcamp" to "Fight Camps actifs","progress" to "Gestion Progression","progress_sub" to "Vue élève regroupant évaluations, devoirs, défis et Fight Camp.","latest" to "Dernière évaluation","none" to "Aucun enregistrement.","plan" to "Adhésion")
    val de=en+mapOf("analytics" to "Analytics","analytics_sub" to "Live-Betriebskennzahlen aus den verbundenen RS KICKBOX-Modulen.","students" to "Aktive Schüler","revenue" to "Eingenommener Umsatz","pending" to "Offene Rechnungen","occupancy" to "Kursauslastung","support" to "Offener Support","homework" to "Offene Hausaufgaben","challenges" to "Challenge-Abschluss","fightcamp" to "Aktive Fight Camps","progress" to "Fortschritts-Manager","progress_sub" to "Eine Schüleransicht über Bewertungen, Hausaufgaben, Challenges und Fight Camp.","latest" to "Letzte Bewertung","none" to "Noch keine Einträge.","plan" to "Mitgliedschaft")
    val it=en+mapOf("analytics" to "Analisi","analytics_sub" to "Metriche operative locali in tempo reale dai moduli RS KICKBOX collegati.","students" to "Allievi attivi","revenue" to "Ricavi incassati","pending" to "Fatture in sospeso","occupancy" to "Occupazione classi","support" to "Supporto aperto","homework" to "Compiti aperti","challenges" to "Sfide completate","fightcamp" to "Fight Camp attivi","progress" to "Gestione Progresso","progress_sub" to "Una vista per allievo su valutazioni, compiti, sfide e Fight Camp.","latest" to "Ultima valutazione","none" to "Nessun dato ancora.","plan" to "Abbonamento")
    val pl=en+mapOf("analytics" to "Analityka","analytics_sub" to "Bieżące lokalne metryki z połączonych modułów RS KICKBOX.","students" to "Aktywni uczniowie","revenue" to "Zebrany przychód","pending" to "Oczekujące faktury","occupancy" to "Obłożenie zajęć","support" to "Otwarte wsparcie","homework" to "Otwarte zadania","challenges" to "Ukończenie wyzwań","fightcamp" to "Aktywne Fight Campy","progress" to "Menedżer Postępu","progress_sub" to "Jedna karta ucznia obejmująca oceny, zadania, wyzwania i Fight Camp.","latest" to "Najnowsza ocena","none" to "Brak danych.","plan" to "Członkostwo")
    val tr=en+mapOf("analytics" to "Analitik","analytics_sub" to "Bağlı RS KICKBOX modüllerinden canlı yerel işletme metrikleri.","students" to "Aktif öğrenciler","revenue" to "Tahsil edilen gelir","pending" to "Bekleyen faturalar","occupancy" to "Ders doluluk oranı","support" to "Açık destek","homework" to "Açık ödevler","challenges" to "Görev tamamlama","fightcamp" to "Aktif Fight Campler","progress" to "İlerleme Yönetimi","progress_sub" to "Değerlendirme, ödev, görev ve Fight Camp için tek öğrenci görünümü.","latest" to "Son değerlendirme","none" to "Henüz kayıt yok.","plan" to "Üyelik")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsMetricCardV53(c:RsPalette,label:String,value:String,sub:String=""){
    RsPanel(c){
        Text(label,color=c.muted,fontSize=10.sp)
        Text(value,color=c.bright,fontSize=27.sp,fontWeight=FontWeight.Black)
        if(sub.isNotBlank())Text(sub,color=c.text,fontSize=10.sp)
    }
}

@Composable
fun RsAnalyticsV53(c:RsPalette,store:RsStore,lang:RsLang){
    val students=rsLoadStudentsV33(store)
    val activeStudents=students.count{it.active}
    val invoices=rsLoadInvoicesV39(store)
    val paid=invoices.filter{it.status=="PAID"}.sumOf{it.amountCents}
    val pending=invoices.filter{it.status!="PAID"}.sumOf{it.amountCents}
    val classes=rsLoadClassesV38(store).filter{it.active}
    val totalCapacity=classes.sumOf{it.capacity}
    val totalBooked=classes.sumOf{it.booked.coerceAtMost(it.capacity)}
    val occupancy=if(totalCapacity>0)totalBooked.toFloat()/totalCapacity else 0f
    val supportOpen=rsLoadTicketsV51(store).count{it.status=="OPEN"}
    val homeworkOpen=rsLoadHomeworkV46(store).count{!it.completed}
    val challenges=rsLoadChallengesV47(store)
    val completedChallenges=challenges.count{it.current>=it.target}
    val challengeRate=if(challenges.isNotEmpty())completedChallenges.toFloat()/challenges.size else 0f
    val camps=rsLoadFightCampsV47(store).count{it.active}

    RsScroll(c,rsAnalyticsUiV53(lang,"analytics"),rsAnalyticsUiV53(lang,"analytics_sub")){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Box(Modifier.weight(1f)){RsMetricCardV53(c,rsAnalyticsUiV53(lang,"students"),activeStudents.toString())}
            Box(Modifier.weight(1f)){RsMetricCardV53(c,rsAnalyticsUiV53(lang,"fightcamp"),camps.toString())}
        }
        RsMetricCardV53(c,rsAnalyticsUiV53(lang,"revenue"),rsMoneyV53(paid))
        RsMetricCardV53(c,rsAnalyticsUiV53(lang,"pending"),rsMoneyV53(pending))
        RsPanel(c){
            Text(rsAnalyticsUiV53(lang,"occupancy"),color=c.muted)
            Text((occupancy*100).toInt().toString()+"%",color=c.bright,fontSize=27.sp,fontWeight=FontWeight.Black)
            LinearProgressIndicator(progress={occupancy.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
            Text(totalBooked.toString()+" / "+totalCapacity+" booked places",color=c.text,fontSize=10.sp)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Box(Modifier.weight(1f)){RsMetricCardV53(c,rsAnalyticsUiV53(lang,"support"),supportOpen.toString())}
            Box(Modifier.weight(1f)){RsMetricCardV53(c,rsAnalyticsUiV53(lang,"homework"),homeworkOpen.toString())}
        }
        RsPanel(c){
            Text(rsAnalyticsUiV53(lang,"challenges"),color=c.muted)
            Text((challengeRate*100).toInt().toString()+"%",color=c.bright,fontSize=27.sp,fontWeight=FontWeight.Black)
            LinearProgressIndicator(progress={challengeRate.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
            Text(completedChallenges.toString()+" / "+challenges.size+" completed",color=c.text,fontSize=10.sp)
        }
    }
}

@Composable
fun RsProgressManagerV53(c:RsPalette,store:RsStore,lang:RsLang){
    val students=rsStudentsV46(store)
    var email by remember{mutableStateOf(students.firstOrNull()?.first.orEmpty())}
    val account=rsLoadStudentsV33(store).firstOrNull{it.email.equals(email,true)}
    val assessments=rsLoadAssessmentsV46(store).filter{it.studentEmail.equals(email,true)}.sortedByDescending{it.createdAt}
    val latest=assessments.firstOrNull()
    val homework=rsLoadHomeworkV46(store).filter{it.studentEmail.equals(email,true)}
    val challenges=rsLoadChallengesV47(store).filter{it.studentEmail.equals(email,true)}
    val camp=rsLoadFightCampsV47(store).firstOrNull{it.studentEmail.equals(email,true)&&it.active}

    RsScroll(c,rsAnalyticsUiV53(lang,"progress"),rsAnalyticsUiV53(lang,"progress_sub")){
        students.forEach{(e,name)->
            FilterChip(selected=email==e,onClick={email=e},label={Text(name)},modifier=Modifier.fillMaxWidth())
        }
        RsPanel(c){
            Text(students.firstOrNull{it.first.equals(email,true)}?.second?:email,color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
            Text(rsAnalyticsUiV53(lang,"plan")+" · "+(account?.plan?:"PRO"),color=c.muted)
            Text("Homework · "+homework.count{!it.completed}+" open / "+homework.size+" total",color=c.text)
            Text("Challenges · "+challenges.count{it.current>=it.target}+" completed / "+challenges.size+" total",color=c.text)
            if(camp!=null)Text("Fight Camp · week "+camp.currentWeek+"/"+camp.totalWeeks+" · "+camp.focus,color=c.text)
        }
        if(latest==null){
            RsPanel(c){Text(rsAnalyticsUiV53(lang,"none"),color=c.muted)}
        }else{
            RsPanel(c){
                Text(rsAnalyticsUiV53(lang,"latest"),color=c.bright,fontWeight=FontWeight.Black)
                listOf(
                    "Punches" to latest.punches,"Kicks" to latest.kicks,"Defense" to latest.defense,
                    "Footwork" to latest.footwork,"Combinations" to latest.combinations,"Conditioning" to latest.conditioning
                ).forEach{(name,value)->
                    Text(name+" · "+value,color=c.text)
                    LinearProgressIndicator(progress={value/100f},modifier=Modifier.fillMaxWidth())
                }
                if(latest.summary.isNotBlank())Text(latest.summary,color=c.muted)
            }
        }
    }
}

package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RsAttendanceCenterV89(c:RsPalette,store:RsStore,lang:RsLang){
    var tab by remember{mutableIntStateOf(0)}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        RsHubTabsV89(
            c,
            listOf(rsHubT89(lang,"attendance_roster"),rsHubT89(lang,"qr_checkin")),
            tab
        ){tab=it}
        Box(Modifier.fillMaxWidth().weight(1f)){
            if(tab==0)RsAttendanceV38(c,store,lang)
            else RsQrAttendanceTrainerV52(c,store,lang)
        }
    }
}

@Composable
fun RsKnowledgeHubV89(c:RsPalette,store:RsStore,lang:RsLang){
    var tab by remember{mutableIntStateOf(0)}
    val labels=listOf(rsHubT89(lang,"browse"),rsHubT89(lang,"search"),rsHubT89(lang,"favorites"),rsHubT89(lang,"history"))
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        RsHubTabsV89(c,labels,tab){tab=it}
        Box(Modifier.fillMaxWidth().weight(1f)){
            when(tab){
                1->RsSearchV48(c,store,lang)
                2->RsFavoritesV48(c,store,lang)
                3->RsHistoryV48(c,store,lang)
                else->RsKnowledgeVaultV48(c,store,lang)
            }
        }
    }
}

@Composable
fun RsWorkoutHomeHubV89(c:RsPalette,store:RsStore,lang:RsLang){
    var tab by remember{mutableIntStateOf(0)}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        RsHubTabsV89(c,listOf(rsHubT89(lang,"home_training"),rsHubT89(lang,"workout_generator")),tab){tab=it}
        Box(Modifier.fillMaxWidth().weight(1f)){
            if(tab==0)RsHomeTrainingV54(c,store,lang)
            else RsWorkoutGeneratorV54(c,lang)
        }
    }
}

@Composable
private fun RsHubTabsV89(
    c:RsPalette,
    labels:List<String>,
    selected:Int,
    onSelected:(Int)->Unit
){
    ScrollableTabRow(
        selectedTabIndex=selected.coerceIn(0,labels.lastIndex),
        edgePadding=8.dp,
        containerColor=c.panel,
        contentColor=c.bright
    ){
        labels.forEachIndexed{i,label->
            Tab(
                selected=selected==i,
                onClick={onSelected(i)},
                text={Text(label,fontWeight=if(selected==i)FontWeight.Black else FontWeight.Medium)}
            )
        }
    }
}


fun rsHubT89(lang:RsLang,key:String):String{
    val en=mapOf(
        "attendance_roster" to "Attendance Roster","qr_checkin" to "QR Check-In",
        "browse" to "Browse","search" to "Search","favorites" to "Favorites","history" to "History",
        "home_training" to "Home Training","workout_generator" to "Workout Generator"
    )
    val nl=en+mapOf("attendance_roster" to "Aanwezigheidslijst","qr_checkin" to "QR Check-In","browse" to "Bladeren","search" to "Zoeken","favorites" to "Favorieten","history" to "Geschiedenis","home_training" to "Thuis Training","workout_generator" to "Workout Generator")
    val pt=en+mapOf("attendance_roster" to "Lista de Presenças","qr_checkin" to "Check-In QR","browse" to "Explorar","search" to "Pesquisar","favorites" to "Favoritos","history" to "Histórico","home_training" to "Treino em Casa","workout_generator" to "Gerador de Treino")
    val es=en+mapOf("attendance_roster" to "Lista de Asistencia","qr_checkin" to "Check-In QR","browse" to "Explorar","search" to "Buscar","favorites" to "Favoritos","history" to "Historial","home_training" to "Entrenamiento en Casa","workout_generator" to "Generador de Entreno")
    val fr=en+mapOf("attendance_roster" to "Liste de Présence","qr_checkin" to "Check-In QR","browse" to "Parcourir","search" to "Rechercher","favorites" to "Favoris","history" to "Historique","home_training" to "Entraînement Maison","workout_generator" to "Générateur d'Entraînement")
    val de=en+mapOf("attendance_roster" to "Anwesenheitsliste","qr_checkin" to "QR Check-In","browse" to "Durchsuchen","search" to "Suchen","favorites" to "Favoriten","history" to "Verlauf","home_training" to "Heimtraining","workout_generator" to "Workout-Generator")
    val it=en+mapOf("attendance_roster" to "Registro Presenze","qr_checkin" to "Check-In QR","browse" to "Sfoglia","search" to "Cerca","favorites" to "Preferiti","history" to "Cronologia","home_training" to "Allenamento a Casa","workout_generator" to "Generatore Allenamento")
    val pl=en+mapOf("attendance_roster" to "Lista Obecności","qr_checkin" to "Check-In QR","browse" to "Przeglądaj","search" to "Szukaj","favorites" to "Ulubione","history" to "Historia","home_training" to "Trening Domowy","workout_generator" to "Generator Treningu")
    val tr=en+mapOf("attendance_roster" to "Katılım Listesi","qr_checkin" to "QR Check-In","browse" to "Göz At","search" to "Ara","favorites" to "Favoriler","history" to "Geçmiş","home_training" to "Ev Antrenmanı","workout_generator" to "Antrenman Oluşturucu")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

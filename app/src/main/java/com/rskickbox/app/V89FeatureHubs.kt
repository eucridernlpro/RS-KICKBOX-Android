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
            listOf(rsUiV91(lang,"attendance_roster"),rsUiV91(lang,"qr_checkin")),
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
    val labels=listOf(rsUiV91(lang,"browse"),rsUiV91(lang,"search"),rsUiV91(lang,"favorites"),rsUiV91(lang,"history"))
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
        RsHubTabsV89(c,listOf(rsUiV91(lang,"home_training"),rsUiV91(lang,"workout_generator")),tab){tab=it}
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


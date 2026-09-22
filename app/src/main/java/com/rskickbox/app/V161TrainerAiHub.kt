package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsTrainerAiHubV161(c:RsPalette,lang:RsLang,store:RsStore){
    var mode by remember{mutableStateOf("COACH")}

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Surface(
            color=Color.Black.copy(alpha=.74f),
            shape=RoundedCornerShape(22.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(
                    "TRAINER AI COMMAND",
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=14.sp
                )
                Text(
                    "Use the same AI coach as students, plus trainer-only trusted technique references.",
                    color=c.muted,
                    fontSize=9.sp
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    Button(
                        onClick={mode="COACH"},
                        modifier=Modifier.weight(1f),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=if(mode=="COACH")c.bright else c.panel,
                            contentColor=if(mode=="COACH")Color.Black else c.text
                        )
                    ){Text("AI Coach",fontSize=9.sp,fontWeight=FontWeight.Black)}
                    OutlinedButton(
                        onClick={mode="REFERENCES"},
                        modifier=Modifier.weight(1f),
                        border=BorderStroke(1.dp,if(mode=="REFERENCES")c.gold else c.gold.copy(alpha=.28f))
                    ){Text("Trainer References",fontSize=9.sp,fontWeight=FontWeight.Black)}
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)){
            if(mode=="COACH")RsStudentAiAssistantSafeV151(c,lang,store)
            else RsTrainerAiReferenceChatV125(c,lang)
        }
    }
}

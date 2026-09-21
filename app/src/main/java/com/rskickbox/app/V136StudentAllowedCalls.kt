package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun RsStudentAllowedCallsV136(
    c:RsPalette,
    lang:RsLang
){
    var contacts by remember{mutableStateOf<List<RsStudentCallContactV136>>(emptyList())}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(Unit){
        while(isActive){
            rsTouchPresenceV125()
            rsStudentCallContactsV136()
                .onSuccess{contacts=it;status=""}
                .onFailure{status=it.message.orEmpty()}
            delay(5000)
        }
    }

    if(contacts.isEmpty()&&status.isBlank())return

    Surface(
        color=Color.Black.copy(alpha=.62f),
        shape=RoundedCornerShape(24.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.24f)),
        modifier=Modifier.fillMaxWidth()
    ){
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            Text(
                "ALLOWED STUDENT CALLS",
                color=c.gold,
                fontWeight=FontWeight.Black,
                fontSize=10.sp,
                letterSpacing=1.sp
            )
            Text(
                "Only call types unlocked by the trainer appear here.",
                color=c.muted,
                fontSize=9.sp
            )
            contacts.forEach{contact->
                RsDirectCallControlsV133(
                    c=c,
                    lang=lang,
                    role=RsRole.STUDENT,
                    peerId=contact.userId,
                    peerName=contact.displayName.ifBlank{contact.email},
                    peerEmail=contact.email,
                    allowAudio=contact.audioAllowed,
                    allowVideo=contact.videoAllowed
                )
            }
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp)
        }
    }
}

package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RsChatSettingsV162(c:RsPalette,store:RsStore,lang:RsLang){
    var autoMedia by remember{mutableStateOf(store.b("chat_auto_media_preview_v162",true))}

    RsScroll(
        c,
        "RS CHAT SETTINGS",
        "Calls · media · privacy · conversation controls"
    ){
        Surface(
            color=c.panel.copy(alpha=.66f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("MEDIA & DATA",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text(
                    "Control when photos and videos inside chats are loaded on this device.",
                    color=c.muted,fontSize=9.sp
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Column(Modifier.weight(1f)){
                        Text("Automatic media previews",color=c.text,fontWeight=FontWeight.Bold,fontSize=10.sp)
                        Text(
                            if(autoMedia)"Images and videos load automatically in conversations."
                            else "Media stays collapsed until you tap Load media.",
                            color=c.muted,fontSize=8.sp
                        )
                    }
                    Switch(
                        checked=autoMedia,
                        onCheckedChange={
                            autoMedia=it
                            store.pb("chat_auto_media_preview_v162",it)
                        }
                    )
                }
            }
        }

        Surface(
            color=c.panel.copy(alpha=.60f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("CONVERSATION CONTROLS",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text("Every open private or group conversation has a top-right ⋮ menu.",color=c.text,fontSize=9.sp)
                Text(
                    "Message actions use the ⋮ on each post: Reply · Edit · Delete for me · Delete for everyone when permitted.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
                Text(
                    "Swipe a message to the right to reply to it.",
                    color=c.muted,fontSize=9.sp
                )
            }
        }

        RsCallRingtoneSettingsV138(c,store,lang)

        Surface(
            color=c.panel.copy(alpha=.54f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.14f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("PRIVACY",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text(
                    "Delete for me hides a message only from your account. Delete for everyone is limited to the sender or trainer/admin where allowed.",
                    color=c.muted,fontSize=9.sp,lineHeight=13.sp
                )
            }
        }
    }
}

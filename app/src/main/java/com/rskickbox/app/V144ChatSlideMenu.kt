package com.rskickbox.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun RsChatSlideMenuV144(
    c:RsPalette,
    role:RsRole,
    onlineCount:Int,
    visible:Boolean,
    selected:String,
    onClose:()->Unit,
    onAll:()->Unit,
    onPrivate:()->Unit,
    onGroups:()->Unit,
    onCommunity:()->Unit,
    onSupport:()->Unit,
    onGallery:()->Unit,
    onNotifications:()->Unit,
    onAi:()->Unit,
    onSettings:()->Unit,
    onCreateGroup:()->Unit,
    onClearChat:()->Unit,
    canClearChat:Boolean
){
    if(visible){
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha=.56f))
                .zIndex(8f)
                .clickable(onClick=onClose)
        )
    }
    AnimatedVisibility(
        visible=visible,
        enter=slideInHorizontally(initialOffsetX={-it})+fadeIn(),
        exit=slideOutHorizontally(targetOffsetX={-it})+fadeOut(),
        modifier=Modifier.fillMaxHeight().widthIn(max=310.dp).zIndex(9f)
    ){
        Surface(
            color=Color.Black.copy(alpha=.97f),
            shape=RoundedCornerShape(topEnd=30.dp,bottomEnd=30.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.42f)),
            tonalElevation=20.dp,
            modifier=Modifier.fillMaxHeight().fillMaxWidth()
        ){
            var drawerDrag by androidx.compose.runtime.remember{androidx.compose.runtime.mutableFloatStateOf(0f)}
            Column(
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit){
                        detectHorizontalDragGestures(
                            onDragStart={drawerDrag=0f},
                            onHorizontalDrag={change,amount->
                                drawerDrag+=amount
                                change.consume()
                            },
                            onDragEnd={
                                if(drawerDrag < -75f)onClose()
                                drawerDrag=0f
                            },
                            onDragCancel={drawerDrag=0f}
                        )
                    },
                verticalArrangement=Arrangement.spacedBy(9.dp)
            ){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("♛  RS CHAT",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp,letterSpacing=1.sp)
                        Text("ROYAL COMMUNICATION",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.3.sp)
                    }
                    TextButton(onClick=onClose){Text("×",color=c.bright,fontSize=24.sp)}
                }

                Surface(
                    color=c.gold.copy(alpha=.07f),
                    shape=RoundedCornerShape(16.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(role==RsRole.TRAINER)"TRAINER COMMAND" else "STUDENT HUB",color=c.muted,fontSize=8.sp,fontWeight=FontWeight.Black)
                        Text(onlineCount.toString()+" ONLINE",color=Color(0xFF36D27F),fontSize=8.sp,fontWeight=FontWeight.Black)
                    }
                }

                Text("COMMUNICATION",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                RsChatSlideItemV144(c,"All Chats","Overview · live contacts · calls","◆",selected=="ALL",false,onAll)
                RsChatSlideItemV144(c,"Private","Direct trainer/student conversation","✦",selected=="PRIVATE",false,onPrivate)
                RsChatSlideItemV144(c,"Support","Private RS support conversation","?",selected=="SUPPORT",false,onSupport)

                Text("TEAM SPACES",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                RsChatSlideItemV144(c,"Fight Groups","Team rooms · media · moderation","◈",selected=="GROUPS",false,onGroups)
                RsChatSlideItemV144(c,"Community","Club feed · members · connection","◎",selected=="COMMUNITY",false,onCommunity)
                if(role==RsRole.TRAINER){
                    RsChatSlideItemV144(c,"Create / Manage Group","Build groups · members · permissions","＋",false,false,onCreateGroup)
                }

                Text("INTELLIGENCE",color=Color(0xFF58C9FF),fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                RsChatSlideItemV144(c,"RS AI Trainer","Sofia · Marcus · technique coaching","✧",selected=="AI",true,onAi)

                Text("MEDIA & CONTROL",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp,modifier=Modifier.padding(top=5.dp))
                RsChatSlideItemV144(c,"RS Chat Gallery","Saved photos · videos · audio","▣",selected=="GALLERY",false,onGallery)
                RsChatSlideItemV144(c,"Notifications","Calls · messages · club alerts","●",selected=="NOTIFICATIONS",false,onNotifications)
                RsChatSlideItemV144(c,"Chat Settings","Calls · media · privacy · appearance","⚙",selected=="SETTINGS",false,onSettings)
                if(canClearChat){
                    RsChatSlideItemV144(c,"Clear private chat","Hide all messages from your view","⌫",false,false,onClearChat)
                }

                Spacer(Modifier.height(8.dp))
                Surface(
                    color=Color.Black.copy(alpha=.62f),
                    shape=RoundedCornerShape(18.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text("RS SECURE COMMUNICATION",color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)
                        Text("Private · Groups · Voice · Video · AI",color=c.muted,fontSize=8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RsChatSlideItemV144(
    c:RsPalette,
    title:String,
    subtitle:String,
    symbol:String,
    selected:Boolean,
    ai:Boolean,
    onClick:()->Unit
){
    val accent=if(ai)Color(0xFF58C9FF) else c.gold
    Surface(
        color=if(selected)accent.copy(alpha=.14f) else Color.Black.copy(alpha=.38f),
        shape=RoundedCornerShape(20.dp),
        border=BorderStroke(1.dp,accent.copy(alpha=if(selected).56f else .18f)),
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)
    ){
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(11.dp)
        ){
            Surface(
                color=accent.copy(alpha=.10f),
                shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,accent.copy(alpha=.30f)),
                modifier=Modifier.size(46.dp)
            ){
                Box(contentAlignment=Alignment.Center){Text(symbol,color=accent,fontSize=18.sp,fontWeight=FontWeight.Black)}
            }
            Column(Modifier.weight(1f)){
                Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=12.sp)
                Text(subtitle,color=c.muted,fontSize=8.sp,maxLines=2)
            }
            Text("›",color=accent,fontSize=20.sp,fontWeight=FontWeight.Black)
        }
    }
}

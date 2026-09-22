package com.rskickbox.app

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RsChatGalleryV163(c:RsPalette,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    val store=remember{RsStore(context)}
    RsChatMediaGalleryV163(c,store,lang)
}

@Composable
fun RsAiRoyalChatV163(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    RsAiAssistantChatV163(c,lang,store,role)
}

@Composable
private fun RsRoyalTextComposerV163(
    c:RsPalette,
    lang:RsLang,
    enabled:Boolean=true,
    placeholder:String,
    onPlus:(()->Unit)?=null,
    onSend:(String)->Unit
){
    var draft by remember{mutableStateOf("")}
    val voiceLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            val spoken=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if(spoken.isNotBlank())draft=spoken.take(1800)
        }
    }
    fun voice(){
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang.locale.toLanguageTag())
        }
        runCatching{voiceLauncher.launch(intent)}
    }

    Surface(
        color=Color.Black.copy(alpha=.92f),
        shape=RoundedCornerShape(28.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.48f)),
        tonalElevation=16.dp,
        modifier=Modifier.fillMaxWidth().navigationBarsPadding()
    ){
        Row(
            Modifier.fillMaxWidth().padding(7.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(7.dp)
        ){
            OutlinedButton(
                onClick={onPlus?:{}},
                enabled=enabled&&onPlus!=null,
                modifier=Modifier.size(44.dp),
                shape=CircleShape,
                contentPadding=PaddingValues(0.dp)
            ){Text("+",fontSize=23.sp,fontWeight=FontWeight.Black)}
            OutlinedTextField(
                value=draft,
                onValueChange={draft=it.take(1800)},
                placeholder={Text(placeholder,fontSize=10.sp)},
                modifier=Modifier.weight(1f),
                minLines=1,
                maxLines=3,
                enabled=enabled,
                shape=RoundedCornerShape(22.dp)
            )
            OutlinedButton(
                onClick={voice()},
                enabled=enabled,
                modifier=Modifier.size(42.dp),
                shape=CircleShape,
                contentPadding=PaddingValues(0.dp)
            ){Text("🎙",fontSize=14.sp)}
            Button(
                onClick={
                    val text=draft.trim()
                    if(text.isNotBlank()){
                        draft=""
                        onSend(text)
                    }
                },
                enabled=enabled&&draft.isNotBlank(),
                modifier=Modifier.size(42.dp),
                shape=CircleShape,
                contentPadding=PaddingValues(0.dp),
                colors=ButtonDefaults.buttonColors(containerColor=c.bright,contentColor=Color.Black)
            ){Text("➤",fontWeight=FontWeight.Black)}
        }
    }
}

@Composable
fun RsCommunityChatV163(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    if(!RsSupabaseV60.configured){
        RsCommunityV50(c,store,lang,role)
        return
    }
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var posts by remember{mutableStateOf<List<RsCloudCommunityPostV84>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var menuId by remember{mutableStateOf<String?>(null)}
    val ownEmail=store.s("session_student_email","")

    LaunchedEffect(revision){
        loading=true
        rsCloudCommunityV84()
            .onSuccess{posts=it}
            .onFailure{status=rsChatFriendlyCloudErrorV164(it.message.orEmpty(),"Could not load Community.")}
        loading=false
    }

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=7.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Column(Modifier.weight(1f)){
                Text("RS COMMUNITY",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text("Club conversation · updates · members",color=c.muted,fontSize=8.sp)
            }
            Text(if(loading)"SYNCING" else "LIVE",color=if(loading)c.muted else Color(0xFF36D27F),fontSize=8.sp,fontWeight=FontWeight.Black)
        }
        if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp,modifier=Modifier.padding(horizontal=8.dp))

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=4.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ){
            if(posts.isEmpty()&&!loading){
                Text("No community messages yet.",color=c.muted,fontSize=9.sp)
            }
            posts.filter{it.active||role==RsRole.TRAINER}.forEach{post->
                Surface(
                    color=c.panel.copy(alpha=.66f),
                    shape=RoundedCornerShape(20.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.14f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            RsMemberAvatarV68(c,post.authorEmail,post.authorName,size=34.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){
                                Text(post.authorName.ifBlank{post.authorEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
                                Text(
                                    SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(Date(post.createdMillis())),
                                    color=c.muted,fontSize=7.sp
                                )
                            }
                            Box{
                                TextButton(
                                    onClick={menuId=post.id},
                                    contentPadding=PaddingValues(3.dp)
                                ){Text("⋮",fontSize=18.sp,color=c.bright)}
                                DropdownMenu(
                                    expanded=menuId==post.id,
                                    onDismissRequest={menuId=null}
                                ){
                                    if(role==RsRole.TRAINER){
                                        DropdownMenuItem(
                                            text={Text(if(post.active)"Hide post" else "Show post")},
                                            onClick={
                                                menuId=null
                                                scope.launch{
                                                    rsSetCloudCommunityPostActiveV84(post.id,!post.active)
                                                        .onSuccess{revision++}
                                                        .onFailure{status=rsChatFriendlyCloudErrorV164(it.message.orEmpty(),"Could not update Community.")}
                                                }
                                            }
                                        )
                                    }
                                    if(role==RsRole.TRAINER||post.authorEmail.equals(ownEmail,true)){
                                        DropdownMenuItem(
                                            text={Text("Delete")},
                                            onClick={
                                                menuId=null
                                                scope.launch{
                                                    rsDeleteCloudCommunityPostV84(post.id)
                                                        .onSuccess{revision++}
                                                        .onFailure{status=it.message.orEmpty()}
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Text(post.body,color=c.text,fontSize=12.sp,lineHeight=17.sp)
                    }
                }
            }
        }

        RsRoyalTextComposerV163(
            c=c,
            lang=lang,
            enabled=!busy,
            placeholder=when(lang.code){
                "nl"->"Bericht aan de community…"
                "pt"->"Mensagem para a comunidade…"
                "es"->"Mensaje para la comunidad…"
                "fr"->"Message à la communauté…"
                else->"Message the community…"
            },
            onPlus=null,
            onSend={text->
                busy=true
                scope.launch{
                    rsCreateCloudCommunityPostV84(text)
                        .onSuccess{revision++}
                        .onFailure{status=rsChatFriendlyCloudErrorV164(it.message.orEmpty(),"Could not send Community message.")}
                    busy=false
                }
            }
        )
    }
}

@Composable
fun RsSupportChatV163(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    if(!RsSupabaseV60.configured){
        RsSupportV51(c,store,lang,role)
        return
    }
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var tickets by remember{mutableStateOf<List<RsCloudSupportTicketV99>>(emptyList())}
    var selected by remember{mutableStateOf<RsCloudSupportTicketV99?>(null)}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var menu by remember{mutableStateOf(false)}

    LaunchedEffect(revision){
        loading=true
        rsCloudSupportFeedV99()
            .onSuccess{
                tickets=it.sortedBy{row->row.createdAtMillis()}
                selected?.let{old->selected=it.firstOrNull{row->row.id==old.id}}
            }
            .onFailure{status=rsChatFriendlyCloudErrorV164(it.message.orEmpty(),"Could not load Support.")}
        loading=false
    }

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=7.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Column(Modifier.weight(1f)){
                Text("RS SUPPORT CHAT",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text(
                    if(role==RsRole.TRAINER)"Member support inbox" else "Private conversation with the RS team",
                    color=c.muted,fontSize=8.sp
                )
            }
            if(role==RsRole.TRAINER&&selected!=null){
                Box{
                    OutlinedButton(
                        onClick={menu=true},
                        modifier=Modifier.size(38.dp),
                        shape=CircleShape,
                        contentPadding=PaddingValues(0.dp)
                    ){Text("⋮",fontSize=18.sp)}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                        DropdownMenuItem(
                            text={Text(if(selected?.status=="RESOLVED")"Reopen" else "Resolve")},
                            onClick={
                                val ticket=selected?:return@DropdownMenuItem
                                menu=false
                                scope.launch{
                                    rsUpdateCloudSupportTicketV99(
                                        ticket.id,
                                        ticket.trainerReply,
                                        if(ticket.status=="RESOLVED")"OPEN" else "RESOLVED"
                                    ).onSuccess{revision++}
                                }
                            }
                        )
                        DropdownMenuItem(
                            text={Text("Delete ticket")},
                            onClick={
                                val ticket=selected?:return@DropdownMenuItem
                                menu=false
                                scope.launch{
                                    rsDeleteCloudSupportTicketV99(ticket.id)
                                        .onSuccess{selected=null;revision++}
                                }
                            }
                        )
                    }
                }
            }
        }

        if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp,modifier=Modifier.padding(horizontal=8.dp))

        if(role==RsRole.TRAINER&&selected==null){
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=4.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ){
                tickets.asReversed().forEach{ticket->
                    Surface(
                        color=c.panel.copy(alpha=.66f),
                        shape=RoundedCornerShape(20.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                        modifier=Modifier.fillMaxWidth().clickable{selected=ticket}
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(11.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            RsMemberAvatarV68(c,ticket.studentEmail,ticket.studentName,size=38.dp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)){
                                Text(ticket.studentName.ifBlank{ticket.studentEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                                Text(ticket.subject,color=c.text,fontSize=9.sp,maxLines=1)
                                Text(ticket.message,color=c.muted,fontSize=8.sp,maxLines=2)
                            }
                            Text(ticket.status,color=if(ticket.status=="OPEN")Color(0xFF36D27F) else c.muted,fontSize=7.sp,fontWeight=FontWeight.Black)
                        }
                    }
                }
            }
        }else{
            val visible=if(role==RsRole.TRAINER)listOfNotNull(selected) else tickets
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=4.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ){
                if(role==RsRole.TRAINER&&selected!=null){
                    TextButton(onClick={selected=null}){Text("‹ Support inbox")}
                }
                visible.forEach{ticket->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                        Surface(
                            color=c.gold.copy(alpha=.15f),
                            shape=RoundedCornerShape(18.dp),
                            border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
                            modifier=Modifier.fillMaxWidth(.86f)
                        ){
                            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                Text(ticket.subject,color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)
                                Text(ticket.message,color=c.text,fontSize=11.sp)
                            }
                        }
                    }
                    if(ticket.trainerReply.isNotBlank()){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Start){
                            Surface(
                                color=c.panel.copy(alpha=.72f),
                                shape=RoundedCornerShape(18.dp),
                                border=BorderStroke(1.dp,c.gold.copy(alpha=.12f)),
                                modifier=Modifier.fillMaxWidth(.86f)
                            ){
                                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                    Text("RS SUPPORT",color=c.gold,fontWeight=FontWeight.Black,fontSize=8.sp)
                                    Text(ticket.trainerReply,color=c.text,fontSize=11.sp)
                                }
                            }
                        }
                    }
                }
            }

            RsRoyalTextComposerV163(
                c=c,
                lang=lang,
                enabled=!busy&&(role==RsRole.STUDENT||selected!=null),
                placeholder=if(role==RsRole.TRAINER)"Reply to this member…" else "Message RS Support…",
                onPlus=null,
                onSend={text->
                    busy=true
                    scope.launch{
                        if(role==RsRole.TRAINER){
                            val ticket=selected
                            if(ticket!=null){
                                rsUpdateCloudSupportTicketV99(ticket.id,text,ticket.status)
                                    .onSuccess{revision++}
                                    .onFailure{status=rsChatFriendlyCloudErrorV164(it.message.orEmpty(),"Could not send Support reply.")}
                            }
                        }else{
                            rsCreateCloudSupportTicketV99("RS Chat Support",text)
                                .onSuccess{revision++}
                                .onFailure{status=rsChatFriendlyCloudErrorV164(it.message.orEmpty(),"Could not send Support message.")}
                        }
                        busy=false
                    }
                }
            )
        }
    }
}

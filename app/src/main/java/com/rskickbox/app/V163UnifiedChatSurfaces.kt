package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
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

@Composable
private fun RsChatSurfaceHeaderV163(
    c:RsPalette,
    title:String,
    subtitle:String,
    onMenu:(()->Unit)?=null
){
    Surface(
        color=Color.Black.copy(alpha=.72f),
        shape=RoundedCornerShape(20.dp),
        border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
        modifier=Modifier.fillMaxWidth()
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Column(Modifier.weight(1f)){
                Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp)
                Text(subtitle,color=c.muted,fontSize=8.sp)
            }
            if(onMenu!=null){
                OutlinedButton(
                    onClick=onMenu,
                    modifier=Modifier.size(38.dp),
                    shape=CircleShape,
                    contentPadding=PaddingValues(0.dp)
                ){Text("⋮",fontSize=18.sp,fontWeight=FontWeight.Black)}
            }
        }
    }
}

@Composable
fun RsCommunityChatV163(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    role:RsRole
){
    val scope=rememberCoroutineScope()
    val userId=rsCurrentCloudUserIdV111()
    var revision by remember{mutableIntStateOf(0)}
    var posts by remember{mutableStateOf<List<RsCloudCommunityPostV84>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    var menu by remember{mutableStateOf(false)}
    val postingAllowed=rsOpsEnabledV56(store,RsOpsKeysV56.COMMUNITY_POSTS,true)

    LaunchedEffect(revision){
        loading=true
        rsCloudCommunityV84()
            .onSuccess{posts=it}
            .onFailure{status=it.message?:"Could not load Community."}
        loading=false
    }

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Box{
            RsChatSurfaceHeaderV163(
                c,
                "RS COMMUNITY",
                "Club conversation · media · members",
                onMenu={menu=true}
            )
            DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                DropdownMenuItem(
                    text={Text("Refresh")},
                    onClick={menu=false;revision++}
                )
                DropdownMenuItem(
                    text={Text("RS Chat Gallery")},
                    onClick={
                        menu=false
                        status="Open Gallery from the RS CHAT menu."
                    }
                )
            }
        }

        if(status.isNotBlank()){
            Text(status,color=c.muted,fontSize=8.sp,modifier=Modifier.padding(horizontal=6.dp))
        }

        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal=3.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ){
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(posts.isEmpty()&&!loading){
                Surface(
                    color=c.panel.copy(alpha=.56f),
                    shape=RoundedCornerShape(18.dp),
                    border=BorderStroke(1.dp,c.gold.copy(alpha=.15f)),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text("No community messages yet.",color=c.muted,modifier=Modifier.padding(12.dp))
                }
            }

            posts.forEach{post->
                val mine=post.authorId==userId
                var postMenu by remember(post.id){mutableStateOf(false)}
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start,
                    verticalAlignment=Alignment.Bottom
                ){
                    if(!mine){
                        RsMemberAvatarV68(c,post.authorEmail,post.authorName,size=30.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Surface(
                        color=if(mine)c.gold.copy(alpha=.15f) else c.panel.copy(alpha=.68f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.16f)),
                        modifier=Modifier.fillMaxWidth(.87f)
                    ){
                        Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                Text(
                                    post.authorName.ifBlank{post.authorEmail},
                                    color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp,
                                    modifier=Modifier.weight(1f)
                                )
                                Box{
                                    TextButton(
                                        onClick={postMenu=true},
                                        contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp),
                                        modifier=Modifier.height(26.dp)
                                    ){Text("⋮",fontSize=18.sp)}
                                    DropdownMenu(expanded=postMenu,onDismissRequest={postMenu=false}){
                                        if(!post.mediaPath.isNullOrBlank()&&!post.mediaKind.isNullOrBlank()){
                                            DropdownMenuItem(
                                                text={Text("Save media to Gallery")},
                                                onClick={
                                                    postMenu=false
                                                    scope.launch{
                                                        rsSaveChatMediaToGalleryV163(
                                                            post.mediaPath,
                                                            post.mediaKind,
                                                            post.mediaName
                                                        )
                                                            .onSuccess{status="Saved to RS Chat Gallery."}
                                                            .onFailure{status=it.message?:"Could not save media."}
                                                    }
                                                }
                                            )
                                        }
                                        if(mine||role==RsRole.TRAINER){
                                            DropdownMenuItem(
                                                text={Text("Delete post")},
                                                onClick={
                                                    postMenu=false
                                                    scope.launch{
                                                        rsDeleteCloudCommunityPostV84(post.id)
                                                            .onSuccess{
                                                                if(!post.mediaPath.isNullOrBlank())runCatching{rsDeleteChatMediaV108(post.mediaPath)}
                                                                revision++
                                                            }
                                                            .onFailure{status=it.message?:"Could not delete post."}
                                                    }
                                                }
                                            )
                                        }
                                        if(role==RsRole.TRAINER){
                                            DropdownMenuItem(
                                                text={Text(if(post.active)"Hide post" else "Show post")},
                                                onClick={
                                                    postMenu=false
                                                    scope.launch{
                                                        rsSetCloudCommunityPostActiveV84(post.id,!post.active)
                                                            .onSuccess{revision++}
                                                            .onFailure{status=it.message?:"Could not update post."}
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if(post.body.isNotBlank())Text(post.body,color=c.text,fontSize=12.sp,lineHeight=16.sp)
                            RsChatAttachmentPreviewV92(c,lang,post.mediaPath,post.mediaKind,post.mediaName)
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }

        RsChatComposerV92(
            c=c,
            lang=lang,
            scopeType="community",
            scopeId=userId,
            enabled=!loading&&postingAllowed&&userId.isNotBlank(),
            onSent={revision++},
            onStatus={status=it},
            onSend={body,attachment->rsCreateCloudCommunityPostV84(body,attachment)}
        )
    }
}

@Composable
fun RsSupportChatV163(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    role:RsRole
){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudSupportTicketV99>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    var selectedStudent by remember{mutableStateOf<String?>(null)}
    var headerMenu by remember{mutableStateOf(false)}

    LaunchedEffect(revision){
        loading=true
        rsCloudSupportFeedV99()
            .onSuccess{rows->
                items=rows
                if(role==RsRole.STUDENT){
                    selectedStudent=rsCurrentCloudUserIdV111()
                }
            }
            .onFailure{status=it.message?:"Could not load Support."}
        loading=false
    }

    if(role==RsRole.TRAINER && selectedStudent==null){
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(7.dp)){
            RsChatSurfaceHeaderV163(c,"RS SUPPORT","Member support conversations")
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp)
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ){
                val groups=items.groupBy{it.studentId}
                if(groups.isEmpty()&&!loading){
                    Text("No support conversations.",color=c.muted,modifier=Modifier.padding(12.dp))
                }
                groups.forEach{(studentId,rows)->
                    val latest=rows.maxByOrNull{it.createdAtMillis()} ?: return@forEach
                    Surface(
                        color=c.panel.copy(alpha=.64f),
                        shape=RoundedCornerShape(20.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.20f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(9.dp)
                        ){
                            RsMemberAvatarV68(c,latest.studentEmail,latest.studentName,size=42.dp)
                            Column(Modifier.weight(1f)){
                                Text(latest.studentName.ifBlank{latest.studentEmail},color=c.bright,fontWeight=FontWeight.Black)
                                Text(latest.message.take(80),color=c.muted,fontSize=9.sp,maxLines=2)
                            }
                            TextButton(onClick={selectedStudent=studentId}){Text("Open")}
                        }
                    }
                }
            }
        }
        return
    }

    val studentId=selectedStudent ?: rsCurrentCloudUserIdV111()
    val thread=items.filter{it.studentId==studentId}.sortedBy{it.createdAtMillis()}
    val latest=thread.lastOrNull()
    val displayName=latest?.studentName?.ifBlank{latest.studentEmail}.orEmpty()

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Box{
            RsChatSurfaceHeaderV163(
                c,
                if(role==RsRole.TRAINER)displayName.ifBlank{"RS SUPPORT"} else "RS SUPPORT",
                if(role==RsRole.TRAINER)"Private member support" else "Private support conversation",
                onMenu={headerMenu=true}
            )
            DropdownMenu(expanded=headerMenu,onDismissRequest={headerMenu=false}){
                if(role==RsRole.TRAINER && latest!=null){
                    DropdownMenuItem(
                        text={Text(if(latest.status=="RESOLVED")"Reopen latest request" else "Resolve latest request")},
                        onClick={
                            headerMenu=false
                            scope.launch{
                                rsUpdateCloudSupportTicketV99(
                                    latest.id,
                                    latest.trainerReply,
                                    if(latest.status=="RESOLVED")"OPEN" else "RESOLVED",
                                    null
                                ).onSuccess{revision++}.onFailure{status=it.message.orEmpty()}
                            }
                        }
                    )
                    DropdownMenuItem(
                        text={Text("Delete latest request")},
                        onClick={
                            headerMenu=false
                            scope.launch{
                                rsDeleteCloudSupportTicketV99(latest.id)
                                    .onSuccess{revision++}
                                    .onFailure{status=it.message.orEmpty()}
                            }
                        }
                    )
                }
                if(role==RsRole.TRAINER){
                    DropdownMenuItem(text={Text("Back to Support Inbox")},onClick={headerMenu=false;selectedStudent=null})
                }
            }
        }

        if(status.isNotBlank())Text(status,color=c.muted,fontSize=8.sp,modifier=Modifier.padding(horizontal=6.dp))

        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal=3.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ){
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            thread.forEach{ticket->
                // Member message.
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                    Surface(
                        color=c.gold.copy(alpha=.15f),
                        shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                        modifier=Modifier.fillMaxWidth(.86f)
                    ){
                        Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                            if(ticket.message.isNotBlank())Text(ticket.message,color=c.text,fontSize=12.sp,lineHeight=16.sp)
                            RsChatAttachmentPreviewV92(c,lang,ticket.mediaPath,ticket.mediaKind,ticket.mediaName)
                            if(!ticket.mediaPath.isNullOrBlank()&&!ticket.mediaKind.isNullOrBlank()){
                                TextButton(
                                    onClick={
                                        scope.launch{
                                            rsSaveChatMediaToGalleryV163(ticket.mediaPath,ticket.mediaKind,ticket.mediaName)
                                                .onSuccess{status="Saved to RS Chat Gallery."}
                                                .onFailure{status=it.message.orEmpty()}
                                        }
                                    },
                                    contentPadding=PaddingValues(2.dp)
                                ){Text("Save media",fontSize=8.sp)}
                            }
                        }
                    }
                }

                if(ticket.trainerReply.isNotBlank()||!ticket.trainerMediaPath.isNullOrBlank()){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Start){
                        Surface(
                            color=Color(0xFF58C9FF).copy(alpha=.07f),
                            shape=RoundedCornerShape(18.dp),
                            border=BorderStroke(1.dp,Color(0xFF58C9FF).copy(alpha=.18f)),
                            modifier=Modifier.fillMaxWidth(.86f)
                        ){
                            Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Text("RS SUPPORT",color=Color(0xFF58C9FF),fontWeight=FontWeight.Black,fontSize=8.sp)
                                if(ticket.trainerReply.isNotBlank())Text(ticket.trainerReply,color=c.text,fontSize=12.sp,lineHeight=16.sp)
                                RsChatAttachmentPreviewV92(c,lang,ticket.trainerMediaPath,ticket.trainerMediaKind,ticket.trainerMediaName)
                                if(!ticket.trainerMediaPath.isNullOrBlank()&&!ticket.trainerMediaKind.isNullOrBlank()){
                                    TextButton(
                                        onClick={
                                            scope.launch{
                                                rsSaveChatMediaToGalleryV163(ticket.trainerMediaPath,ticket.trainerMediaKind,ticket.trainerMediaName)
                                                    .onSuccess{status="Saved to RS Chat Gallery."}
                                                    .onFailure{status=it.message.orEmpty()}
                                            }
                                        },
                                        contentPadding=PaddingValues(2.dp)
                                    ){Text("Save media",fontSize=8.sp)}
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }

        if(role==RsRole.STUDENT){
            RsChatComposerV92(
                c=c,
                lang=lang,
                scopeType="support",
                scopeId=studentId,
                enabled=!loading&&studentId.isNotBlank(),
                onSent={revision++},
                onStatus={status=it},
                onSend={body,attachment->
                    rsCreateCloudSupportTicketV99("RS Support",body,attachment)
                }
            )
        }else{
            val target=thread.lastOrNull{it.status!="RESOLVED"} ?: latest
            RsChatComposerV92(
                c=c,
                lang=lang,
                scopeType="support",
                scopeId=studentId,
                enabled=!loading&&studentId.isNotBlank()&&target!=null,
                onSent={revision++},
                onStatus={status=it},
                onSend={body,attachment->
                    if(target==null)Result.failure(IllegalStateException("No support request selected."))
                    else rsUpdateCloudSupportTicketV99(target.id,body,"OPEN",attachment)
                }
            )
        }
    }
}

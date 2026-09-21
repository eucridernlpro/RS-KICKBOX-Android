package com.rskickbox.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun RsProfessionalGroupsV144(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var groups by remember{mutableStateOf<List<RsCloudGroupV84>>(emptyList())}
    var thumbs by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    var selectedGroup by remember{mutableStateOf<RsCloudGroupV84?>(null)}
    var messages by remember{mutableStateOf<List<RsCloudGroupMessageV92>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var createOpen by remember{mutableStateOf(false)}
    var editGroup by remember{mutableStateOf<RsCloudGroupV84?>(null)}
    var membersGroup by remember{mutableStateOf<RsCloudGroupV84?>(null)}
    var members by remember{mutableStateOf<List<RsCloudGroupMemberV144>>(emptyList())}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision,selectedGroup?.id){
        loading=true
        if(selectedGroup==null){
            rsCloudGroupsV84().onSuccess{list->
                groups=list
                val next=thumbs.toMutableMap()
                list.forEach{g->
                    if(!g.thumbnailPath.isNullOrBlank()&&next[g.id].isNullOrBlank()){
                        rsGroupThumbnailLocalV144(context,g.thumbnailPath).onSuccess{local->
                            if(local.isNotBlank())next[g.id]=local
                        }
                    }
                }
                thumbs=next
                status=""
            }.onFailure{status=it.message.orEmpty()}
        }else{
            rsCloudGroupMessagesV92(selectedGroup!!.id)
                .onSuccess{messages=it}
                .onFailure{status=rsChatBackendFriendlyErrorV121(lang,it)}
        }
        loading=false
    }

    selectedGroup?.let{g->
        RsProfessionalGroupChatV144(
            c,lang,role,g,thumbs[g.id].orEmpty(),messages,loading,status,
            onBack={selectedGroup=null;messages=emptyList();status="";revision++},
            onRefresh={revision++},
            onStatus={status=it},
            onManage=if(role==RsRole.TRAINER){{editGroup=g}} else null
        )
        return
    }

    RsScroll(c,"FIGHT GROUPS",if(role==RsRole.TRAINER)"Professional team spaces · moderation · live member presence" else "Your RS KICKBOXING team communication"){
        Surface(
            color=Color.Black.copy(alpha=.70f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.34f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                Column(Modifier.weight(1f)){
                    Text("GROUP COMMAND",color=c.bright,fontWeight=FontWeight.Black,fontSize=14.sp,letterSpacing=.9.sp)
                    Text(groups.size.toString()+" groups · "+groups.sumOf{it.onlineCount}.toString()+" members online",color=Color(0xFF36D27F),fontSize=9.sp,fontWeight=FontWeight.Bold)
                }
                if(role==RsRole.TRAINER)Button(onClick={createOpen=true},enabled=!busy){Text("+ NEW GROUP",fontSize=9.sp,fontWeight=FontWeight.Black)}
            }
        }

        if(status.isNotBlank())Surface(color=c.panel.copy(alpha=.60f),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),modifier=Modifier.fillMaxWidth()){
            Text(status,color=c.muted,fontSize=9.sp,modifier=Modifier.padding(10.dp))
        }
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())

        groups.forEach{g->
            RsGroupVisualCardV144(
                c,g,thumbs[g.id].orEmpty(),role,busy,
                onOpen={selectedGroup=g;status="";revision++},
                onJoinLeave={
                    busy=true
                    scope.launch{
                        rsSetMyCloudGroupMembershipV84(g.id,!g.joined)
                            .onSuccess{revision++}
                            .onFailure{status=it.message.orEmpty()}
                        busy=false
                    }
                },
                onManage={editGroup=g},
                onMembers={
                    membersGroup=g
                    members=emptyList()
                    scope.launch{
                        rsCloudGroupMembersV144(g.id)
                            .onSuccess{members=it}
                            .onFailure{status=it.message.orEmpty()}
                    }
                }
            )
        }
        if(groups.isEmpty()&&!loading)RsPanel(c){Text("No groups available yet.",color=c.muted)}
    }

    if(createOpen){
        RsGroupEditorV144(c,"CREATE GROUP",null,"",busy,
            onDismiss={if(!busy)createOpen=false},
            onSave={name,description,imageRaw,canPost,canMedia,openJoin->
                busy=true
                scope.launch{
                    val thumbPath=if(imageRaw.isNotBlank()){
                        rsUploadGroupThumbnailV144(context,imageRaw).getOrElse{
                            status=it.message.orEmpty();busy=false;return@launch
                        }
                    }else null
                    rsCreateCloudGroupV84(name,description,thumbPath,canPost,canMedia,openJoin)
                        .onSuccess{status="Group created.";createOpen=false;revision++}
                        .onFailure{status=it.message.orEmpty()}
                    busy=false
                }
            }
        )
    }

    editGroup?.let{g->
        RsGroupEditorV144(c,"EDIT GROUP",g,thumbs[g.id].orEmpty(),busy,
            onDismiss={if(!busy)editGroup=null},
            onSave={name,description,imageRaw,canPost,canMedia,openJoin->
                busy=true
                scope.launch{
                    var path=g.thumbnailPath
                    if(imageRaw.isNotBlank()&&imageRaw!=thumbs[g.id]){
                        path=rsUploadGroupThumbnailV144(context,imageRaw).getOrElse{
                            status=it.message.orEmpty();busy=false;return@launch
                        }
                    }
                    rsUpdateCloudGroupV144(g.id,name,description,path,canPost,canMedia,openJoin)
                        .onSuccess{status="Group settings updated.";editGroup=null;revision++}
                        .onFailure{status=it.message.orEmpty()}
                    busy=false
                }
            },
            onToggleActive={value->
                busy=true
                scope.launch{
                    rsSetCloudGroupActiveV84(g.id,value)
                        .onSuccess{editGroup=null;revision++}
                        .onFailure{status=it.message.orEmpty()}
                    busy=false
                }
            },
            onDelete={
                if(pendingDelete==g.id){
                    busy=true
                    scope.launch{
                        rsDeleteCloudGroupV84(g.id)
                            .onSuccess{pendingDelete=null;editGroup=null;status="Group deleted.";revision++}
                            .onFailure{status=it.message.orEmpty()}
                        busy=false
                    }
                }else pendingDelete=g.id
            },
            deleteArmed=pendingDelete==g.id
        )
    }

    membersGroup?.let{g->
        Dialog(onDismissRequest={if(!busy)membersGroup=null},properties=DialogProperties(usePlatformDefaultWidth=false)){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
                Surface(color=Color.Black.copy(alpha=.97f),shape=RoundedCornerShape(topStart=30.dp,topEnd=30.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.48f)),modifier=Modifier.fillMaxWidth().fillMaxHeight(.80f)){
                    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("GROUP MEMBERS",color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
                                Text(g.memberCount.toString()+" total · "+g.onlineCount+" online · "+g.offlineCount+" offline",color=c.muted,fontSize=9.sp)
                            }
                            TextButton(onClick={membersGroup=null}){Text("×",color=c.bright,fontSize=24.sp)}
                        }
                        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
                            items(members,key={it.userId}){m->
                                Surface(color=c.panel.copy(alpha=.48f),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.15f)),modifier=Modifier.fillMaxWidth()){
                                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
                                        RsMemberAvatarV68(c,m.email,m.displayName,size=42.dp)
                                        Column(Modifier.weight(1f)){
                                            Text(m.displayName.ifBlank{m.email},color=c.bright,fontWeight=FontWeight.Bold,fontSize=11.sp)
                                            Text(if(m.online)"ONLINE" else "OFFLINE",color=if(m.online)Color(0xFF36D27F) else c.muted,fontSize=8.sp,fontWeight=FontWeight.Black)
                                        }
                                        OutlinedButton(onClick={
                                            busy=true
                                            scope.launch{
                                                rsRemoveCloudGroupMemberV144(g.id,m.userId)
                                                    .onSuccess{members=members.filterNot{it.userId==m.userId};revision++}
                                                    .onFailure{status=it.message.orEmpty()}
                                                busy=false
                                            }
                                        },enabled=!busy){Text("Remove",fontSize=8.sp)}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RsGroupVisualCardV144(c:RsPalette,group:RsCloudGroupV84,thumbnailUri:String,role:RsRole,busy:Boolean,onOpen:()->Unit,onJoinLeave:()->Unit,onManage:()->Unit,onMembers:()->Unit){
    val shape=RoundedCornerShape(28.dp)
    Box(Modifier.fillMaxWidth().height(245.dp).background(Color.Black,shape).clickable{if(role==RsRole.TRAINER||group.joined)onOpen()}){
        if(thumbnailUri.isNotBlank())RsUriPreviewV21(thumbnailUri,Modifier.fillMaxSize(),"CENTER")
        else Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF070707),Color(0xFF24170A),Color.Black))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.08f),Color.Black.copy(alpha=.24f),Color.Black.copy(alpha=.88f)))))
        Surface(color=Color.Black.copy(alpha=.58f),shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.40f)),modifier=Modifier.align(Alignment.TopStart).padding(12.dp)){
            Text("♛  RS GROUP",color=c.bright,fontWeight=FontWeight.Black,fontSize=8.sp,modifier=Modifier.padding(horizontal=9.dp,vertical=6.dp))
        }
        Surface(color=if(group.active)Color(0xFF163323).copy(alpha=.82f) else Color(0xFF421010).copy(alpha=.82f),shape=RoundedCornerShape(14.dp),modifier=Modifier.align(Alignment.TopEnd).padding(12.dp)){
            Text(if(group.active)"ACTIVE" else "PAUSED",color=if(group.active)Color(0xFF7BF0AA) else Color(0xFFFF8A80),fontWeight=FontWeight.Black,fontSize=7.sp,modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(group.name,color=Color.White,fontWeight=FontWeight.Black,fontSize=20.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(group.description,color=Color.White.copy(alpha=.78f),fontSize=10.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                RsGroupStatPillV144(c,group.memberCount.toString()+" MEMBERS",c.bright)
                RsGroupStatPillV144(c,group.onlineCount.toString()+" ONLINE",Color(0xFF65E79C))
                RsGroupStatPillV144(c,group.offlineCount.toString()+" OFFLINE",Color.White.copy(alpha=.62f))
            }
            if(role==RsRole.TRAINER){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Button(onClick=onOpen,enabled=!busy,modifier=Modifier.weight(1f)){Text("Open Chat",fontSize=8.sp)}
                    OutlinedButton(onClick=onMembers,enabled=!busy,modifier=Modifier.weight(1f)){Text("Members",fontSize=8.sp)}
                    OutlinedButton(onClick=onManage,enabled=!busy,modifier=Modifier.weight(1f)){Text("Manage",fontSize=8.sp)}
                }
            }else{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    if(group.joined)Button(onClick=onOpen,modifier=Modifier.weight(1f)){Text("Open Group",fontSize=9.sp)}
                    OutlinedButton(onClick=onJoinLeave,enabled=!busy&&(group.joined||group.openJoin),modifier=Modifier.weight(1f)){
                        Text(if(group.joined)"Leave" else if(group.openJoin)"Join" else "Trainer Locked",fontSize=9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RsGroupStatPillV144(c:RsPalette,label:String,color:Color){
    Surface(color=Color.Black.copy(alpha=.56f),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,color.copy(alpha=.30f))){
        Text(label,color=color,fontWeight=FontWeight.Black,fontSize=7.sp,modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp))
    }
}

@Composable
private fun RsGroupEditorV144(
    c:RsPalette,title:String,initial:RsCloudGroupV84?,thumbnailUri:String,busy:Boolean,
    onDismiss:()->Unit,
    onSave:(String,String,String,Boolean,Boolean,Boolean)->Unit,
    onToggleActive:((Boolean)->Unit)?=null,
    onDelete:(()->Unit)?=null,
    deleteArmed:Boolean=false
){
    var name by remember(initial?.id){mutableStateOf(initial?.name.orEmpty())}
    var description by remember(initial?.id){mutableStateOf(initial?.description.orEmpty())}
    var imageRaw by remember(initial?.id){mutableStateOf(thumbnailUri)}
    var canPost by remember(initial?.id){mutableStateOf(initial?.studentsCanPost?:true)}
    var canMedia by remember(initial?.id){mutableStateOf(initial?.studentsCanMedia?:true)}
    var openJoin by remember(initial?.id){mutableStateOf(initial?.openJoin?:true)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null)imageRaw=uri.toString()}

    Dialog(onDismissRequest={if(!busy)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)){
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.BottomCenter){
            Surface(color=Color.Black.copy(alpha=.97f),shape=RoundedCornerShape(topStart=30.dp,topEnd=30.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.52f)),modifier=Modifier.fillMaxWidth().fillMaxHeight(.90f)){
                Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(title,color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp,letterSpacing=.8.sp)
                            Text("Identity · thumbnail · moderation",color=c.muted,fontSize=9.sp)
                        }
                        TextButton(onClick=onDismiss,enabled=!busy){Text("×",color=c.bright,fontSize=24.sp)}
                    }
                    if(imageRaw.isNotBlank()){
                        Box(Modifier.fillMaxWidth().height(170.dp)){RsUriPreviewV21(imageRaw,Modifier.fillMaxSize(),"CENTER");Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.18f)))}
                    }else{
                        Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(listOf(Color.Black,Color(0xFF251807),Color.Black)),RoundedCornerShape(22.dp)),contentAlignment=Alignment.Center){
                            Text("♛ RS GROUP",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
                        }
                    }
                    OutlinedButton(onClick={picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Choose Group Thumbnail")}
                    OutlinedTextField(name,{name=it.take(80)},label={Text("Group name")},enabled=!busy,modifier=Modifier.fillMaxWidth(),singleLine=true)
                    OutlinedTextField(description,{description=it.take(500)},label={Text("Description")},enabled=!busy,modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4)
                    Text("TRAINER MODERATION",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=1.sp)
                    RsGroupModerationSwitchV144(c,"Students can post messages","Allow members to send text messages",canPost,!busy){canPost=it}
                    RsGroupModerationSwitchV144(c,"Students can upload media","Allow photos, videos and voice in this group",canMedia,!busy){canMedia=it}
                    RsGroupModerationSwitchV144(c,"Students can join themselves","Turn off for trainer-managed membership only",openJoin,!busy){openJoin=it}
                    Spacer(Modifier.weight(1f))
                    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                    Button(onClick={onSave(name.trim(),description.trim(),imageRaw,canPost,canMedia,openJoin)},enabled=!busy&&name.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp)){
                        Text(if(busy)"Saving…" else "Save Group",fontWeight=FontWeight.Black)
                    }
                    if(initial!=null&&onToggleActive!=null)OutlinedButton(onClick={onToggleActive(!initial.active)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(initial.active)"Pause Group" else "Activate Group")}
                    if(initial!=null&&onDelete!=null)OutlinedButton(onClick=onDelete,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(deleteArmed)"CONFIRM DELETE GROUP" else "Delete Group",color=Color(0xFFFF8A80))}
                }
            }
        }
    }
}

@Composable
private fun RsGroupModerationSwitchV144(c:RsPalette,title:String,sub:String,checked:Boolean,enabled:Boolean,onChecked:(Boolean)->Unit){
    Surface(color=c.panel.copy(alpha=.44f),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.14f)),modifier=Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text(title,color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp);Text(sub,color=c.muted,fontSize=8.sp)}
            Switch(checked=checked,onCheckedChange=onChecked,enabled=enabled)
        }
    }
}

@Composable
private fun RsProfessionalGroupChatV144(
    c:RsPalette,lang:RsLang,role:RsRole,group:RsCloudGroupV84,thumbnailUri:String,
    messages:List<RsCloudGroupMessageV92>,loading:Boolean,status:String,
    onBack:()->Unit,onRefresh:()->Unit,onStatus:(String)->Unit,onManage:(()->Unit)?
){
    RsScroll(c,group.name,group.description){
        Box(Modifier.fillMaxWidth().height(150.dp).background(Color.Black,RoundedCornerShape(24.dp))){
            if(thumbnailUri.isNotBlank())RsUriPreviewV21(thumbnailUri,Modifier.fillMaxSize(),"CENTER")
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.10f),Color.Black.copy(.82f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)){
                Text(group.name,color=Color.White,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(group.memberCount.toString()+" members · "+group.onlineCount+" online · "+group.offlineCount+" offline",color=Color.White.copy(alpha=.78f),fontSize=9.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick=onBack,modifier=Modifier.weight(1f)){Text("Back")}
            if(onManage!=null)OutlinedButton(onClick=onManage,modifier=Modifier.weight(1f)){Text("Group Settings")}
        }
        if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())

        val currentUserId=rsCurrentCloudUserIdV111()
        messages.forEach{m->
            val mine=m.senderId==currentUserId
            Row(Modifier.fillMaxWidth(),horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start,verticalAlignment=Alignment.Bottom){
                if(!mine){RsMemberAvatarV68(c,m.senderEmail,m.senderName,size=30.dp);Spacer(Modifier.width(7.dp))}
                Surface(
                    color=if(mine)c.gold.copy(alpha=.16f) else c.panel.copy(alpha=.70f),
                    shape=RoundedCornerShape(topStart=20.dp,topEnd=20.dp,bottomStart=if(mine)20.dp else 6.dp,bottomEnd=if(mine)6.dp else 20.dp),
                    border=BorderStroke(1.dp,if(mine)c.gold.copy(alpha=.25f) else c.gold.copy(alpha=.10f)),
                    modifier=Modifier.fillMaxWidth(.84f)
                ){
                    Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text(m.senderName.ifBlank{m.senderEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=9.sp)
                        if(m.body.isNotBlank())Text(m.body,color=c.text,fontSize=13.sp,lineHeight=18.sp)
                        RsChatAttachmentPreviewV92(c,lang,m.mediaPath,m.mediaKind,m.mediaName)
                        if(role==RsRole.TRAINER){
                            RsTrainerMessageAdminV111(c,lang,m.body,mine,loading,
                                onEdit={body->rsStaffEditGroupMessageV111(m.id,body)},
                                onDelete={rsStaffDeleteGroupMessageV111(m.id,m.mediaPath)},
                                onChanged=onRefresh,onStatus=onStatus
                            )
                        }
                    }
                }
            }
        }

        if(role==RsRole.TRAINER&&messages.isNotEmpty()){
            RsTrainerClearConversationV111(c,lang,!loading,
                onClear={rsStaffClearGroupChatRobustV116(group.id,messages)},
                onChanged=onRefresh,onStatus=onStatus
            )
        }

        if(role==RsRole.TRAINER||group.studentsCanPost){
            RsChatComposerV92(
                c=c,lang=lang,scopeType="group",scopeId=group.id,enabled=!loading,
                onSent=onRefresh,onStatus=onStatus,
                onSend={body,attachment->
                    if(role!=RsRole.TRAINER&&attachment!=null&&!group.studentsCanMedia)
                        Result.failure(IllegalStateException("Trainer has disabled student media in this group."))
                    else rsSendCloudGroupMessageV92(group.id,body,attachment)
                }
            )
        }else{
            Surface(color=Color.Black.copy(alpha=.66f),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),modifier=Modifier.fillMaxWidth()){
                Text("Trainer has set this group to read-only for students.",color=c.muted,fontSize=10.sp,modifier=Modifier.padding(12.dp))
            }
        }
    }
}

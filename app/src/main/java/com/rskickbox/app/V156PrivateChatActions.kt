package com.rskickbox.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun RsCloudCoachBubbleV156(
    c:RsPalette,
    lang:RsLang,
    message:RsCloudCoachMessageV72,
    viewerRole:RsRole,
    onReply:(RsCloudCoachMessageV72)->Unit,
    onChanged:()->Unit,
    onStatus:(String)->Unit
){
    val scope=rememberCoroutineScope()
    val context=androidx.compose.ui.platform.LocalContext.current
    val currentId=rsCurrentCloudUserIdV111()
    val trainer=message.senderRole=="trainer"||message.senderRole=="admin"
    val mine=message.senderId==currentId || (message.senderId.isBlank() && if(viewerRole==RsRole.TRAINER)trainer else !trainer)
    var menu by remember{mutableStateOf(false)}
    var editing by remember{mutableStateOf(false)}
    var draft by remember(message.id,message.body){mutableStateOf(message.body)}
    var dragX by remember(message.id){mutableFloatStateOf(0f)}

    Row(
        Modifier.fillMaxWidth()
            .pointerInput(message.id){
                detectHorizontalDragGestures(
                    onDragStart={dragX=0f},
                    onHorizontalDrag={change,amount->
                        if(amount>0f){
                            dragX+=amount
                            change.consume()
                        }
                    },
                    onDragEnd={
                        if(dragX>85f)onReply(message)
                        dragX=0f
                    },
                    onDragCancel={dragX=0f}
                )
            },
        horizontalArrangement=if(mine)Arrangement.End else Arrangement.Start,
        verticalAlignment=Alignment.Bottom
    ){
        Surface(
            color=if(mine)c.gold.copy(alpha=.17f) else c.panel.copy(alpha=.72f),
            shape=RoundedCornerShape(
                topStart=20.dp,topEnd=20.dp,
                bottomStart=if(mine)20.dp else 6.dp,
                bottomEnd=if(mine)6.dp else 20.dp
            ),
            border=BorderStroke(1.dp,if(mine)c.gold.copy(alpha=.28f) else c.gold.copy(alpha=.12f)),
            tonalElevation=4.dp,
            modifier=Modifier.fillMaxWidth(.86f)
        ){
            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(
                        if(trainer)"TRAINER" else message.studentName.ifBlank{"STUDENT"},
                        color=if(mine)c.bright else c.text,
                        fontWeight=FontWeight.Black,
                        fontSize=9.sp,
                        modifier=Modifier.weight(1f)
                    )
                    Box{
                        TextButton(
                            onClick={menu=true},
                            contentPadding=PaddingValues(horizontal=7.dp,vertical=0.dp),
                            modifier=Modifier.height(28.dp)
                        ){Text("⋮",color=c.bright,fontSize=19.sp,fontWeight=FontWeight.Black)}
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                            DropdownMenuItem(text={Text("Reply")},onClick={menu=false;onReply(message)})
                            if(mine || viewerRole==RsRole.TRAINER){
                                DropdownMenuItem(text={Text("Edit")},onClick={menu=false;editing=true;draft=message.body})
                            }
                            if(!message.mediaPath.isNullOrBlank()&&!message.mediaKind.isNullOrBlank()){
                                DropdownMenuItem(
                                    text={Text("Save media to Gallery")},
                                    onClick={
                                        menu=false
                                        scope.launch{
                                            rsSaveChatMediaToGalleryV163(
                                                message.mediaPath,
                                                message.mediaKind,
                                                message.mediaName
                                            )
                                                .onSuccess{onStatus("Saved to RS Chat Gallery.")}
                                                .onFailure{onStatus(it.message?:"Could not save media.")}
                                        }
                                    }
                                )
                            }
                            if(!message.mediaPath.isNullOrBlank() && !message.mediaKind.isNullOrBlank()){
                                DropdownMenuItem(
                                    text={Text("Save to Gallery")},
                                    onClick={
                                        menu=false
                                        scope.launch{
                                            rsChatMediaLocalUriV92(context,message.mediaPath)
                                                .onSuccess{local->
                                                    rsSaveChatMediaToGalleryV163(
                                                        context,galleryStore,local,message.mediaKind,message.mediaName
                                                    ).onSuccess{onStatus("Saved to RS Chat Gallery.")}
                                                     .onFailure{onStatus(it.message.orEmpty())}
                                                }
                                                .onFailure{onStatus(it.message.orEmpty())}
                                        }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text={Text("Delete for me")},
                                onClick={
                                    menu=false
                                    scope.launch{
                                        rsHideCoachMessageV156(message.id)
                                            .onSuccess{onChanged()}
                                            .onFailure{onStatus(it.message.orEmpty())}
                                    }
                                }
                            )
                            if(mine || viewerRole==RsRole.TRAINER){
                                DropdownMenuItem(
                                    text={Text("Delete for everyone")},
                                    onClick={
                                        menu=false
                                        scope.launch{
                                            rsDeleteCoachMessageForEveryoneV156(message.id,message.mediaPath)
                                                .onSuccess{onChanged()}
                                                .onFailure{onStatus(it.message.orEmpty())}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                if(!message.replyBody.isNullOrBlank()){
                    Surface(
                        color=c.gold.copy(alpha=.08f),
                        shape=RoundedCornerShape(10.dp),
                        border=BorderStroke(1.dp,c.gold.copy(alpha=.18f)),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(message.replyBody.take(180),color=c.muted,fontSize=9.sp,maxLines=3,modifier=Modifier.padding(7.dp))
                    }
                }
                if(message.body.isNotBlank())Text(message.body,color=c.text,fontSize=13.sp,lineHeight=18.sp)
                RsChatAttachmentPreviewV92(c,lang,message.mediaPath,message.mediaKind,message.mediaName)
                Text(
                    java.text.SimpleDateFormat("dd MMM · HH:mm",java.util.Locale.getDefault())
                        .format(java.util.Date(message.createdAtMillis())),
                    color=c.muted,fontSize=8.sp,modifier=Modifier.align(Alignment.End)
                )
            }
        }
    }

    if(editing){
        AlertDialog(
            onDismissRequest={editing=false},
            title={Text("Edit message")},
            text={
                OutlinedTextField(
                    value=draft,
                    onValueChange={draft=it.take(1200)},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2,maxLines=6
                )
            },
            confirmButton={
                TextButton(onClick={
                    scope.launch{
                        rsEditCoachMessageV156(message.id,draft)
                            .onSuccess{editing=false;onChanged()}
                            .onFailure{onStatus(it.message.orEmpty())}
                    }
                },enabled=draft.trim().isNotBlank()){Text("Save")}
            },
            dismissButton={TextButton(onClick={editing=false}){Text("Cancel")}}
        )
    }
}

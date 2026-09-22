package com.rskickbox.app

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private fun rsAiReferenceNameV125(context:android.content.Context,uri:Uri):String{
    var name="Trainer reference"
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())name=cur.getString(0)?:name
        }
    }
    return name
}

private fun rsAiReferenceKindV125(context:android.content.Context,uri:Uri):String{
    val mime=context.contentResolver.getType(uri).orEmpty().lowercase()
    return if(mime.startsWith("video/"))"VIDEO" else "IMAGE"
}

@Composable
fun RsTrainerAiReferenceChatV125(c:RsPalette,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var source by remember{mutableStateOf<Uri?>(null)}
    var sourceKind by remember{mutableStateOf("IMAGE")}
    var sourceName by remember{mutableStateOf("")}
    var move by remember{mutableStateOf("")}
    var tags by remember{mutableStateOf("")}
    var note by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    var classifying by remember{mutableStateOf(false)}
    var saving by remember{mutableStateOf(false)}
    var confirmed by remember{mutableStateOf(false)}
    var lastSaved by remember{mutableStateOf<RsTrainingMediaItemV55?>(null)}
    var attachmentMenu by remember{mutableStateOf(false)}

    fun accept(uri:Uri?){
        if(uri==null)return
        source=uri
        sourceKind=rsAiReferenceKindV125(context,uri)
        sourceName=rsAiReferenceNameV125(context,uri)
        if(move.isBlank()){
            move=sourceName.substringBeforeLast('.').replace('_',' ').replace('-',' ').trim().take(120)
        }
        confirmed=false
        status=""
    }

    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){accept(it)}
    val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){accept(it)}
    val files=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            accept(uri)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=8.dp,vertical=4.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        Surface(
            color=c.panel.copy(alpha=.70f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.18f))
        ){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text("TRAINER AI REFERENCE CHAT",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(
                    "Upload a trainer image or video. AI suggests the move, tags and a short coaching note. You confirm it before the media enters the trusted AI teaching gallery.",
                    color=c.muted,fontSize=10.sp,lineHeight=15.sp
                )
            }
        }

        Surface(
            color=c.panel.copy(alpha=.66f),
            shape=RoundedCornerShape(24.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.16f))
        ){
            Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Box{
                    OutlinedButton(
                        onClick={attachmentMenu=true},
                        enabled=!saving&&!classifying,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text("＋  Add trainer reference",fontSize=10.sp,fontWeight=FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded=attachmentMenu,
                        onDismissRequest={attachmentMenu=false},
                        containerColor=c.panel
                    ){
                        DropdownMenuItem(
                            text={Text("▣  Photo")},
                            onClick={
                                attachmentMenu=false
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        DropdownMenuItem(
                            text={Text("▶  Video")},
                            onClick={
                                attachmentMenu=false
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        )
                        DropdownMenuItem(
                            text={Text("⌑  File")},
                            onClick={
                                attachmentMenu=false
                                files.launch(arrayOf("image/*","video/*"))
                            }
                        )
                    }
                }

                val selected=source
                if(selected!=null){
                    RsTrainingMediaPreviewV55(
                        c,
                        RsTrainingMediaItemV55(
                            id="preview",
                            title=sourceName,
                            category="TECHNIQUE",
                            description="",
                            uri=selected.toString(),
                            kind=sourceKind,
                            accessTier="ALL",
                            published=false
                        ),
                        modifier=Modifier.fillMaxWidth().heightIn(min=190.dp,max=360.dp)
                    )
                    Text(sourceName,color=c.muted,fontSize=9.sp,maxLines=1)

                    Button(
                        onClick={
                            if(classifying)return@Button
                            classifying=true
                            confirmed=false
                            status="AI is reading the trainer reference…"
                            scope.launch{
                                rsClassifyTrainerReferenceV125(context,selected,lang)
                                    .onSuccess{result->
                                        move=result.move
                                        tags=result.tags.joinToString(", ")
                                        if(result.coachNote.isNotBlank())note=result.coachNote
                                        status="AI suggestion ready. Check the label and press Confirm."
                                    }
                                    .onFailure{
                                        status="AI classification is unavailable or uncertain. Enter the move and tags manually, then confirm."
                                    }
                                classifying=false
                            }
                        },
                        enabled=!classifying&&!saving,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(classifying)"Analyzing…" else "✦ AI identify move & tags")}
                }

                OutlinedTextField(
                    value=move,
                    onValueChange={move=it.take(120);confirmed=false},
                    label={Text("Move / combination label")},
                    placeholder={Text("Example: Rear Roundhouse Kick")},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=!saving
                )
                OutlinedTextField(
                    value=tags,
                    onValueChange={tags=it.take(500);confirmed=false},
                    label={Text("Technique tags")},
                    placeholder={Text("roundhouse, rear kick, pivot, hip rotation, guard")},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2,maxLines=4,
                    enabled=!saving
                )
                OutlinedTextField(
                    value=note,
                    onValueChange={note=it.take(500);confirmed=false},
                    label={Text("Trainer instruction")},
                    placeholder={Text("Short instruction the student should see with this example")},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2,maxLines=5,
                    enabled=!saving
                )

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    OutlinedButton(
                        onClick={confirmed=true;status="✓ Trainer confirmed the reference label and coaching note."},
                        enabled=source!=null&&move.isNotBlank()&&!saving&&!classifying,
                        modifier=Modifier.weight(1f)
                    ){Text(if(confirmed)"✓ Confirmed" else "Confirm",fontSize=10.sp)}
                    Button(
                        onClick={
                            val uri=source?:return@Button
                            if(!confirmed){
                                status="Confirm the move label before saving this as an AI reference."
                                return@Button
                            }
                            saving=true
                            status="Saving trusted trainer reference…"
                            scope.launch{
                                val normalizedTags=(tags.split(",")+move)
                                    .map{it.trim().lowercase()}
                                    .filter{it.isNotBlank()}
                                    .distinct()
                                    .take(20)
                                rsUploadCloudTrainingMediaV73(
                                    context=context,
                                    source=uri,
                                    title=move.trim(),
                                    category="TECHNIQUE",
                                    description=note.trim(),
                                    accessTier="ALL",
                                    published=true
                                ).onSuccess{path->
                                    val item=rsCloudTrainingMediaV73().getOrNull()?.firstOrNull{it.uri==path}
                                    if(item==null){
                                        status="Media uploaded, but its AI reference record could not be verified yet."
                                    }else{
                                        rsSetCloudTrainingMediaAiReferenceV110(item.id,true,normalizedTags)
                                            .onSuccess{
                                                lastSaved=item.copy(
                                                    title=move.trim(),
                                                    description=note.trim(),
                                                    techniqueTags=normalizedTags,
                                                    aiReference=true
                                                )
                                                status="✓ Saved to the trusted AI reference gallery."
                                                source=null
                                                sourceName=""
                                                move=""
                                                tags=""
                                                note=""
                                                confirmed=false
                                            }
                                            .onFailure{status="Media uploaded, but AI reference tags could not be saved."}
                                    }
                                }.onFailure{status=it.message?:"Could not save trainer reference."}
                                saving=false
                            }
                        },
                        enabled=source!=null&&move.isNotBlank()&&confirmed&&!saving&&!classifying,
                        modifier=Modifier.weight(1f)
                    ){Text(if(saving)"Saving…" else "Save to AI gallery",fontSize=10.sp)}
                }

                if(status.isNotBlank()){
                    Text(status,color=if(status.startsWith("✓"))c.bright else c.muted,fontSize=10.sp,lineHeight=14.sp)
                }
            }
        }

        lastSaved?.let{saved->
            Surface(
                color=c.gold.copy(alpha=.08f),
                shape=RoundedCornerShape(20.dp),
                border=BorderStroke(1.dp,c.gold.copy(alpha=.22f))
            ){
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Text("LAST TRUSTED REFERENCE",color=c.gold,fontWeight=FontWeight.Black,fontSize=9.sp,letterSpacing=.8.sp)
                    Text(saved.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp)
                    Text(saved.techniqueTags.joinToString(" · "),color=c.muted,fontSize=9.sp)
                    if(saved.description.isNotBlank())Text(saved.description,color=c.text,fontSize=10.sp)
                }
            }
        }
    }
}

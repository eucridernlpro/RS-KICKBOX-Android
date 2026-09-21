package com.rskickbox.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RsCloudPromotionManagerV104(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var promos by remember{mutableStateOf<List<RsCloudPromotionV103>>(emptyList())}
    var promoLocal by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    var bookRow by remember{mutableStateOf<RsCloudBookV103?>(null)}
    var bookLocal by remember{mutableStateOf<RsBookConfigV45?>(null)}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    var promoTitle by remember{mutableStateOf("")}
    var promoLink by remember{mutableStateOf("")}
    var promoImage by remember{mutableStateOf("")}

    var bookTitle by remember{mutableStateOf("VAN STILTE NAAR STRIJD")}
    var amazonUrl by remember{mutableStateOf("")}
    var accessTier by remember{mutableStateOf("PRO")}
    var giftedText by remember{mutableStateOf("")}
    var pendingCover by remember{mutableStateOf("")}
    var pendingPreview by remember{mutableStateOf("")}
    var pendingFull by remember{mutableStateOf("")}
    var readerUri by remember{mutableStateOf("")}
    var readerTitle by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        loading=true
        rsCloudPromotionsV103()
            .onSuccess{loaded->
                promos=loaded
                promoLocal=loaded.mapNotNull{item->
                    runCatching{rsCloudPromotionLocalV156(context,item)}.getOrNull()
                        ?.let{local->item.id to local.imageUri}
                }.toMap()
            }
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        rsCloudBookV103()
            .onSuccess{row->
                bookRow=row
                if(row!=null){
                    bookTitle=row.title
                    amazonUrl=row.amazonUrl.orEmpty()
                    accessTier=row.accessTier
                    giftedText=row.giftedEmails.joinToString(",")
                }
            }
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        rsCloudBookConfigLocalV103(context)
            .onSuccess{bookLocal=it.first}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    val promoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            promoImage=uri.toString()
            status=rsPromoUiV45(lang,"thumb_ready")
        }
    }
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            pendingCover=uri.toString()
            status=rsPromoUiV45(lang,"cover_ready")
        }
    }
    val previewPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            pendingPreview=uri.toString()
            status=rsPromoUiV45(lang,"preview_ready")
        }
    }
    val fullPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            pendingFull=uri.toString()
            status=rsPromoUiV45(lang,"full_ready")
        }
    }

    if(readerUri.isNotBlank()){
        RsPdfBookReaderV45(c,store,lang,readerTitle,readerUri){readerUri=""}
        return
    }

    RsScroll(c,rsPromoUiV45(lang,"manager"),rsPromoUiV45(lang,"manager_sub")){
        RsPanel(c){
            Text(
                if(loading)rsReleaseT98(lang,"syncing") else rsReleaseT98(lang,"connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        RsPanel(c){
            Text(rsPromoUiV45(lang,"new_thumb"),color=c.bright,fontWeight=FontWeight.Black)
            if(promoImage.isNotBlank())RsUriPreviewV21(promoImage,Modifier.fillMaxWidth().height(150.dp),"CENTER")
            OutlinedButton(
                onClick={promoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                enabled=!busy,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"image"))}
            OutlinedTextField(promoTitle,{promoTitle=it.take(120)},label={Text(rsPromoUiV45(lang,"title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(promoLink,{promoLink=it.trim()},label={Text(rsPromoUiV45(lang,"link"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            if(busy){
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(status,color=c.bright,fontSize=10.sp,fontWeight=FontWeight.Bold)
            }else if(status.isNotBlank()){
                Text(status,color=c.muted,fontSize=10.sp)
            }

            Button(
                onClick={
                    busy=true
                    status="Preparing book upload…"
                    scope.launch{
                        rsUploadPromotionImageV103(context,promoImage)
                            .fold(
                                onSuccess={path->
                                    rsCreateCloudPromotionV103(promoTitle,path,promoLink)
                                        .onSuccess{
                                            promoTitle=""
                                            promoLink=""
                                            promoImage=""
                                            status=rsPromoUiV45(lang,"thumb_ready")
                                            revision++
                                        }
                                        .onFailure{status=rsReleaseT98(lang,"save_failed")}
                                },
                                onFailure={status=rsReleaseT98(lang,"save_failed")}
                            )
                        busy=false
                    }
                },
                enabled=!busy&&promoTitle.isNotBlank()&&promoImage.isNotBlank()&&rsValidExternalV45(promoLink),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)rsReleaseT98(lang,"saving") else rsPromoUiV45(lang,"save"))}
        }

        promos.forEach{item->
            RsPanel(c){
                val preview=promoLocal[item.id].orEmpty()
                if(preview.isNotBlank())RsUriPreviewV21(
                    preview,
                    Modifier.fillMaxWidth().height(130.dp),
                    "CENTER"
                )
                Text(item.title,color=c.bright,fontWeight=FontWeight.Bold)
                Text(item.externalUrl,color=c.muted,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(item.active)rsPromoUiV45(lang,"active") else rsPromoUiV45(lang,"inactive"),color=c.muted)
                    Switch(
                        checked=item.active,
                        onCheckedChange={on->
                            busy=true
                            scope.launch{
                                rsSetCloudPromotionActiveV103(item.id,on)
                                    .onSuccess{revision++}
                                    .onFailure{status=rsReleaseT98(lang,"update_failed")}
                                busy=false
                            }
                        },
                        enabled=!busy
                    )
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==item.id){
                            busy=true
                            scope.launch{
                                rsDeleteCloudPromotionV103(item.id,item.imagePath)
                                    .onSuccess{pendingDelete=null;revision++}
                                    .onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                busy=false
                            }
                        }else pendingDelete=item.id
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==item.id)rsPromoUiV45(lang,"confirm") else rsPromoUiV45(lang,"delete"))}
            }
        }

        RsPanel(c){
            Text(rsPromoUiV45(lang,"book_manager"),color=c.bright,fontWeight=FontWeight.Black)
            val coverPreview=pendingCover.ifBlank{bookLocal?.coverUri.orEmpty()}
            if(coverPreview.isNotBlank())RsUriPreviewV21(coverPreview,Modifier.fillMaxWidth().height(190.dp),"CENTER")
            OutlinedButton(
                onClick={coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                enabled=!busy,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"cover"))}
            OutlinedTextField(bookTitle,{bookTitle=it.take(160)},label={Text(rsPromoUiV45(lang,"book_title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedTextField(amazonUrl,{amazonUrl=it.trim()},label={Text(rsPromoUiV45(lang,"amazon"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
            OutlinedButton(onClick={previewPicker.launch(arrayOf("application/pdf"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){
                Text(rsPromoUiV45(lang,"preview_pdf")+(if(pendingPreview.isNotBlank()||bookRow?.previewPath?.isNotBlank()==true)" ✓" else ""))
            }
            OutlinedButton(onClick={fullPicker.launch(arrayOf("application/pdf"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){
                Text(rsPromoUiV45(lang,"full_pdf")+(if(pendingFull.isNotBlank()||bookRow?.fullPath?.isNotBlank()==true)" ✓" else ""))
            }
            bookLocal?.previewPdfUri?.takeIf{it.isNotBlank()}?.let{uri->
                OutlinedButton(
                    onClick={readerTitle=bookTitle+" · "+rsPromoUiV45(lang,"preview");readerUri=uri},
                    modifier=Modifier.fillMaxWidth()
                ){Text("Trainer · "+rsPromoUiV45(lang,"preview"))}
            }
            bookLocal?.fullPdfUri?.takeIf{it.isNotBlank()}?.let{uri->
                Button(
                    onClick={readerTitle=bookTitle;readerUri=uri},
                    modifier=Modifier.fillMaxWidth()
                ){Text("Trainer · "+rsPromoUiV45(lang,"read"))}
            }

            Text(rsPromoUiV45(lang,"access"),color=c.muted,fontSize=10.sp)
            listOf(listOf("ALL","BASIC","PRO"),listOf("ELITE","PRIVATE")).forEach{row->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    row.forEach{tier->
                        FilterChip(
                            selected=accessTier==tier,
                            onClick={accessTier=tier},
                            label={Text(tier,fontSize=9.sp)},
                            enabled=!busy,
                            modifier=Modifier.weight(1f)
                        )
                    }
                    if(row.size==2)Spacer(Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                giftedText,
                {giftedText=it},
                label={Text(rsPromoUiV45(lang,"gift"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2,
                enabled=!busy
            )

            Button(
                onClick={
                    busy=true
                    status=rsReleaseT98(lang,"saving")
                    scope.launch{
                        var coverPath=bookRow?.coverPath
                        var previewPath=bookRow?.previewPath
                        var fullPath=bookRow?.fullPath

                        if(pendingCover.isNotBlank())status="Uploading book cover…"
                        val coverResult=if(pendingCover.isNotBlank())rsUploadBookAssetV103(context,pendingCover,"cover") else Result.success(coverPath.orEmpty())
                        if(coverResult.isFailure){
                            status=rsReleaseT98(lang,"save_failed")
                            busy=false
                            return@launch
                        }
                        if(pendingCover.isNotBlank())coverPath=coverResult.getOrNull()

                        if(pendingPreview.isNotBlank())status="Uploading preview PDF…"
                        val previewResult=if(pendingPreview.isNotBlank())rsUploadBookAssetV103(context,pendingPreview,"preview") else Result.success(previewPath.orEmpty())
                        if(previewResult.isFailure){
                            status=rsReleaseT98(lang,"save_failed")
                            busy=false
                            return@launch
                        }
                        if(pendingPreview.isNotBlank())previewPath=previewResult.getOrNull()

                        if(pendingFull.isNotBlank())status="Uploading full book PDF…"
                        val fullResult=if(pendingFull.isNotBlank())rsUploadBookAssetV103(context,pendingFull,"full") else Result.success(fullPath.orEmpty())
                        if(fullResult.isFailure){
                            status=rsReleaseT98(lang,"save_failed")
                            busy=false
                            return@launch
                        }
                        if(pendingFull.isNotBlank())fullPath=fullResult.getOrNull()

                        val emails=giftedText.split(',',';','\n')
                            .map{it.trim().lowercase()}
                            .filter{it.contains("@")}
                            .toSet()

                        status="Saving book settings…"
                        rsSaveCloudBookV103(
                            bookRow?.id,
                            bookTitle,
                            coverPath,
                            amazonUrl,
                            previewPath,
                            fullPath,
                            accessTier,
                            emails
                        )
                            .onSuccess{
                                pendingCover=""
                                pendingPreview=""
                                pendingFull=""
                                status=rsPromoUiV45(lang,"book_saved")
                                revision++
                            }
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busy=false
                    }
                },
                enabled=!busy&&bookTitle.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)rsReleaseT98(lang,"saving") else rsPromoUiV45(lang,"save_book"))}
        }
    }
}

@Composable
fun RsCloudPromotionPageV104(
    c:RsPalette,
    store:RsStore,
    lang:RsLang,
    onOpenBook:()->Unit
){
    val context=LocalContext.current
    var promos by remember{mutableStateOf<List<RsPromoItemV45>>(emptyList())}
    var book by remember{mutableStateOf<RsBookConfigV45?>(null)}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    val listState=rememberLazyListState()

    LaunchedEffect(Unit){
        loading=true
        rsCloudPromotionsV103()
            .onSuccess{loaded->
                promos=loaded.filter{x->x.active}.mapNotNull{item->
                    runCatching{rsCloudPromotionLocalV156(context,item)}.getOrNull()
                }
            }
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        rsCloudBookConfigLocalV103(context)
            .onSuccess{book=it.first}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    LaunchedEffect(promos.size){
        if(promos.size>1){
            var index=0
            while(true){
                delay(3200)
                index=(index+1)%promos.size
                listState.animateScrollToItem(index)
            }
        }
    }

    RsScroll(c,rsPromoUiV45(lang,"promo_page"),rsPromoUiV45(lang,"promo_sub")){
        if(loading)RsPanel(c){Text(rsReleaseT98(lang,"syncing"),color=c.muted)}
        if(status.isNotBlank())RsPanel(c){Text(status,color=c.muted)}
        if(promos.isNotEmpty()){
            Text(rsPromoUiV45(lang,"featured"),color=c.bright,fontWeight=FontWeight.Black)
            LazyRow(
                state=listState,
                horizontalArrangement=Arrangement.spacedBy(12.dp),
                contentPadding=PaddingValues(horizontal=2.dp)
            ){
                items(promos,key={it.id}){item->
                    Surface(
                        color=c.panel,
                        shape=MaterialTheme.shapes.large,
                        modifier=Modifier.width(280.dp).clickable{rsOpenExternalV45(context,item.externalUrl)}
                    ){
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            RsUriPreviewV21(item.imageUri,Modifier.fillMaxWidth().height(145.dp),"CENTER")
                            Text(item.title,color=c.bright,fontWeight=FontWeight.Bold,maxLines=2)
                        }
                    }
                }
            }
        }
        val b=book
        if(b!=null && (b.coverUri.isNotBlank()||b.amazonUrl.isNotBlank()||b.previewPdfUri.isNotBlank())){
            Text(rsPromoUiV45(lang,"book"),color=c.bright,fontWeight=FontWeight.Black)
            RsPanel(c){
                if(b.coverUri.isNotBlank())RsUriPreviewV21(
                    b.coverUri,
                    Modifier.fillMaxWidth().height(230.dp).clickable{rsOpenExternalV45(context,b.amazonUrl)},
                    "CENTER"
                )
                Text(b.title,color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
                if(rsValidExternalV45(b.amazonUrl))Button(
                    onClick={rsOpenExternalV45(context,b.amazonUrl)},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPromoUiV45(lang,"buy"))}
                if(b.previewPdfUri.isNotBlank())OutlinedButton(onClick=onOpenBook,modifier=Modifier.fillMaxWidth()){
                    Text(rsPromoUiV45(lang,"open_reader"))
                }
            }
        }
    }
}

@Composable
fun RsCloudBookLibraryV104(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    var book by remember{mutableStateOf<RsBookConfigV45?>(null)}
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    var readerUri by remember{mutableStateOf("")}
    var readerTitle by remember{mutableStateOf("")}

    LaunchedEffect(Unit){
        loading=true
        rsCloudBookConfigLocalV103(context)
            .onSuccess{book=it.first}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    if(readerUri.isNotBlank()){
        RsPdfBookReaderV45(c,store,lang,readerTitle,readerUri){readerUri=""}
        return
    }

    val b=book
    RsScroll(c,rsRouteTitle(lang,"book","Trainer Book"),b?.title.orEmpty()){
        if(loading)RsPanel(c){Text(rsReleaseT98(lang,"syncing"),color=c.muted)}
        if(status.isNotBlank())RsPanel(c){Text(status,color=c.muted)}
        if(b==null&&!loading)RsPanel(c){Text(rsCommonT95(lang,"none"),color=c.muted)}
        if(b!=null)RsPanel(c){
            if(b.coverUri.isNotBlank())RsUriPreviewV21(
                b.coverUri,
                Modifier.fillMaxWidth().height(270.dp).clickable{rsOpenExternalV45(context,b.amazonUrl)},
                "CENTER"
            )
            Text(b.title,color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)
            if(rsValidExternalV45(b.amazonUrl))Button(
                onClick={rsOpenExternalV45(context,b.amazonUrl)},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"buy"))}

            if(b.previewPdfUri.isNotBlank())OutlinedButton(
                onClick={
                    readerTitle=b.title+" · "+rsPromoUiV45(lang,"preview")
                    readerUri=b.previewPdfUri
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"preview"))}

            if(b.fullPdfUri.isNotBlank()){
                Button(
                    onClick={
                        readerTitle=b.title
                        readerUri=b.fullPdfUri
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPromoUiV45(lang,"read"))}
            }else{
                Text(rsPromoUiV45(lang,"locked"),color=c.muted,fontSize=10.sp)
            }
        }
    }
}

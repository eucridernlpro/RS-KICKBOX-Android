package com.rskickbox.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

data class RsPromoItemV45(
    val id:String,
    val title:String,
    val imageUri:String,
    val externalUrl:String,
    val active:Boolean
)

data class RsBookConfigV45(
    val title:String="VAN STILTE NAAR STRIJD",
    val coverUri:String="",
    val amazonUrl:String="",
    val previewPdfUri:String="",
    val fullPdfUri:String="",
    val accessTier:String="PRO",
    val giftedEmails:Set<String> = emptySet()
)

private fun rsPromoDirV45(context:android.content.Context)=
    File(context.filesDir,"rs_books").apply{mkdirs()}

private fun rsCopyBookPdfV45(context:android.content.Context,source:Uri,label:String):String{
    val dir=rsPromoDirV45(context)
    dir.listFiles()?.filter{it.name.startsWith(label+"_") }?.forEach{it.delete()}
    val out=File(dir,label+"_"+UUID.randomUUID()+".pdf")
    context.contentResolver.openInputStream(source)!!.use{input->
        FileOutputStream(out).use{output->input.copyTo(output)}
    }
    return Uri.fromFile(out).toString()
}

private fun rsDeleteBookPdfV45(context:android.content.Context,uriString:String){
    if(uriString.isBlank())return
    runCatching{
        val uri=Uri.parse(uriString)
        if(uri.scheme!="file")return
        val file=uri.path?.let(::File)?:return
        val root=rsPromoDirV45(context).canonicalFile
        val target=file.canonicalFile
        if(target.parentFile==root && target.exists())target.delete()
    }
}

private fun rsEncodePromosV45(items:List<RsPromoItemV45>):String{
    val arr=JSONArray()
    items.forEach{p->
        arr.put(JSONObject().apply{
            put("id",p.id);put("title",p.title);put("imageUri",p.imageUri)
            put("externalUrl",p.externalUrl);put("active",p.active)
        })
    }
    return arr.toString()
}

private fun rsDecodePromosV45(raw:String):List<RsPromoItemV45>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsPromoItemV45(
                    o.optString("id"),o.optString("title"),o.optString("imageUri"),
                    o.optString("externalUrl"),o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsLoadPromosV45(store:RsStore)=rsDecodePromosV45(store.s("promo_items_v45",""))
private fun rsSavePromosV45(store:RsStore,items:List<RsPromoItemV45>)=
    store.ps("promo_items_v45",rsEncodePromosV45(items))

private fun rsEncodeBookV45(book:RsBookConfigV45)=JSONObject().apply{
    put("title",book.title)
    put("coverUri",book.coverUri)
    put("amazonUrl",book.amazonUrl)
    put("previewPdfUri",book.previewPdfUri)
    put("fullPdfUri",book.fullPdfUri)
    put("accessTier",book.accessTier)
    put("giftedEmails",JSONArray(book.giftedEmails.toList()))
}.toString()

private fun rsLoadBookV45(store:RsStore):RsBookConfigV45{
    val raw=store.s("book_config_v45","")
    if(raw.isBlank())return RsBookConfigV45()
    return runCatching{
        val o=JSONObject(raw)
        val gifted=o.optJSONArray("giftedEmails")
        val emails=buildSet{
            if(gifted!=null)for(i in 0 until gifted.length())add(gifted.optString(i).lowercase())
        }
        RsBookConfigV45(
            title=o.optString("title","VAN STILTE NAAR STRIJD"),
            coverUri=o.optString("coverUri"),
            amazonUrl=o.optString("amazonUrl"),
            previewPdfUri=o.optString("previewPdfUri"),
            fullPdfUri=o.optString("fullPdfUri"),
            accessTier=o.optString("accessTier","PRO"),
            giftedEmails=emails
        )
    }.getOrDefault(RsBookConfigV45())
}

private fun rsSaveBookV45(store:RsStore,book:RsBookConfigV45)=store.ps("book_config_v45",rsEncodeBookV45(book))

private fun rsValidExternalV45(url:String)=url.startsWith("https://")||url.startsWith("http://")

private fun rsOpenExternalV45(context:android.content.Context,url:String){
    if(!rsValidExternalV45(url))return
    runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
}

private fun rsTierRankV45(tier:String)=when(tier.uppercase()){
    "ALL"->0
    "BASIC"->1
    "PRO"->2
    "ELITE"->3
    else->99
}

private fun rsStudentTierV45(store:RsStore):String{
    val email=store.s("session_student_email","alex@rskickbox.nl")
    return rsLoadStudentsV33(store).firstOrNull{it.email.equals(email,true)}?.plan?.uppercase()?:"PRO"
}

private fun rsCanReadFullV45(store:RsStore,book:RsBookConfigV45):Boolean{
    val email=store.s("session_student_email","alex@rskickbox.nl").lowercase()
    if(email in book.giftedEmails)return true
    val required=rsTierRankV45(book.accessTier)
    if(required==99)return false
    return rsTierRankV45(rsStudentTierV45(store))>=required
}

private fun rsPromoUiV45(lang:RsLang,key:String):String{
    val en=mapOf(
        "promo_page" to "Promotions","promo_sub" to "Offers, products and featured RS KICKBOX content.",
        "featured" to "FEATURED PROMOTIONS","book" to "FEATURED BOOK","buy" to "Buy on Amazon",
        "preview" to "Read Preview","read" to "Read Full Book","locked" to "Full book access is not included in your current access level.",
        "manager" to "Promotion Manager","manager_sub" to "Upload clickable thumbnails, external links and control the in-app book.",
        "new_thumb" to "NEW PROMOTION THUMBNAIL","title" to "Title","link" to "External link","image" to "Choose thumbnail",
        "save" to "Save thumbnail","active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm",
        "book_manager" to "BOOK PROMOTION & READER","book_title" to "Book title","amazon" to "Amazon link",
        "cover" to "Choose book thumbnail / cover","preview_pdf" to "Upload preview PDF","full_pdf" to "Upload full book PDF",
        "access" to "Full book access","gift" to "Free access emails (comma separated)","save_book" to "Save book settings",
        "open_reader" to "Open Reader","search" to "Search book","next_match" to "Next match","no_match" to "No matching page found.",
        "page" to "Page","zoom" to "Zoom","back" to "Back to library"
    )
    val nl=en+mapOf("promo_page" to "Promoties","promo_sub" to "Aanbiedingen, producten en uitgelichte RS KICKBOX-content.","featured" to "UITGELICHTE PROMOTIES","book" to "UITGELICHT BOEK","buy" to "Kopen op Amazon","preview" to "Preview lezen","read" to "Volledig boek lezen","locked" to "Volledige boektoegang zit niet in je huidige toegangsniveau.","manager" to "Promotiebeheer","manager_sub" to "Upload klikbare thumbnails, externe links en beheer het boek in de app.","new_thumb" to "NIEUWE PROMOTIETHUMBNAIL","title" to "Titel","link" to "Externe link","image" to "Thumbnail kiezen","save" to "Thumbnail opslaan","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen","book_manager" to "BOEKPROMOTIE & READER","book_title" to "Boektitel","amazon" to "Amazon-link","cover" to "Boekthumbnail / cover kiezen","preview_pdf" to "Preview-PDF uploaden","full_pdf" to "Volledig boek-PDF uploaden","access" to "Toegang volledig boek","gift" to "Gratis toegang e-mails (komma gescheiden)","save_book" to "Boekinstellingen opslaan","open_reader" to "Reader openen","search" to "Zoek in boek","next_match" to "Volgende match","no_match" to "Geen overeenkomende pagina gevonden.","page" to "Pagina","zoom" to "Zoom","back" to "Terug naar bibliotheek")
    val pack=when(lang.code){"nl"->nl;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsPromotionManagerV45(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    var revision by remember{mutableIntStateOf(0)}
    var promoTitle by remember{mutableStateOf("")}
    var promoLink by remember{mutableStateOf("")}
    var promoImage by remember{mutableStateOf("")}
    var promoStatus by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val promos=remember(revision){rsLoadPromosV45(store)}
    var book by remember(revision){mutableStateOf(rsLoadBookV45(store))}
    var giftedText by remember(book.giftedEmails){mutableStateOf(book.giftedEmails.joinToString(","))}

    val promoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            rsImportVisualMediaV29(
                context,uri,"MEDIUM",
                onStatus={promoStatus=it},
                onComplete={info->
                    if(promoImage.isNotBlank()&&promoImage!=info.uri)rsDeleteOwnedVisualV29(context,promoImage)
                    promoImage=info.uri
                    promoStatus="Thumbnail ready."
                },
                onError={promoStatus=it}
            )
        }
    }
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null){
            rsImportVisualMediaV29(
                context,uri,"MEDIUM",
                onStatus={promoStatus=it},
                onComplete={info->
                    val old=book.coverUri
                    book=book.copy(coverUri=info.uri)
                    if(old.isNotBlank()&&old!=info.uri)rsDeleteOwnedVisualV29(context,old)
                    promoStatus="Book cover ready."
                },
                onError={promoStatus=it}
            )
        }
    }
    val previewPdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{
            val old=book.previewPdfUri
            val saved=rsCopyBookPdfV45(context,uri,"preview")
            book=book.copy(previewPdfUri=saved)
            if(old.isNotBlank()&&old!=saved)rsDeleteBookPdfV45(context,old)
            promoStatus="Preview PDF imported."
        }.onFailure{promoStatus="Could not import preview PDF."}
    }
    val fullPdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{
            val old=book.fullPdfUri
            val saved=rsCopyBookPdfV45(context,uri,"full")
            book=book.copy(fullPdfUri=saved)
            if(old.isNotBlank()&&old!=saved)rsDeleteBookPdfV45(context,old)
            promoStatus="Full book PDF imported."
        }.onFailure{promoStatus="Could not import full book PDF."}
    }

    fun savePromos(items:List<RsPromoItemV45>){rsSavePromosV45(store,items);revision++}

    RsScroll(c,rsPromoUiV45(lang,"manager"),rsPromoUiV45(lang,"manager_sub")){
        RsPanel(c){
            Text(rsPromoUiV45(lang,"new_thumb"),color=c.bright,fontWeight=FontWeight.Black)
            if(promoImage.isNotBlank())RsUriPreviewV21(promoImage,Modifier.fillMaxWidth().height(150.dp),"CENTER")
            OutlinedButton(
                onClick={promoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"image"))}
            OutlinedTextField(promoTitle,{promoTitle=it.take(80)},label={Text(rsPromoUiV45(lang,"title"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(promoLink,{promoLink=it.trim()},label={Text(rsPromoUiV45(lang,"link"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(
                onClick={
                    savePromos(listOf(RsPromoItemV45(UUID.randomUUID().toString(),promoTitle.trim(),promoImage,promoLink.trim(),true))+promos)
                    promoTitle="";promoLink="";promoImage="";promoStatus=""
                },
                enabled=promoTitle.isNotBlank()&&promoImage.isNotBlank()&&rsValidExternalV45(promoLink),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"save"))}
            if(promoStatus.isNotBlank())Text(promoStatus,color=c.muted,fontSize=10.sp)
        }

        promos.forEach{item->
            RsPanel(c){
                RsUriPreviewV21(item.imageUri,Modifier.fillMaxWidth().height(130.dp),"CENTER")
                Text(item.title,color=c.bright,fontWeight=FontWeight.Bold)
                Text(item.externalUrl,color=c.muted,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(item.active)rsPromoUiV45(lang,"active") else rsPromoUiV45(lang,"inactive"),color=c.muted)
                    Switch(item.active,{on->savePromos(promos.map{if(it.id==item.id)it.copy(active=on) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==item.id){
                            rsDeleteOwnedVisualV29(context,item.imageUri)
                            savePromos(promos.filterNot{it.id==item.id})
                            pendingDelete=null
                        }else pendingDelete=item.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==item.id)rsPromoUiV45(lang,"confirm") else rsPromoUiV45(lang,"delete"))}
            }
        }

        RsPanel(c){
            Text(rsPromoUiV45(lang,"book_manager"),color=c.bright,fontWeight=FontWeight.Black)
            if(book.coverUri.isNotBlank())RsUriPreviewV21(book.coverUri,Modifier.fillMaxWidth().height(190.dp),"CENTER")
            OutlinedButton(
                onClick={coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"cover"))}
            OutlinedTextField(book.title,{book=book.copy(title=it.take(120))},label={Text(rsPromoUiV45(lang,"book_title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(book.amazonUrl,{book=book.copy(amazonUrl=it.trim())},label={Text(rsPromoUiV45(lang,"amazon"))},modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={previewPdfPicker.launch(arrayOf("application/pdf"))},modifier=Modifier.fillMaxWidth()){
                Text(rsPromoUiV45(lang,"preview_pdf")+(if(book.previewPdfUri.isNotBlank())" ✓" else ""))
            }
            OutlinedButton(onClick={fullPdfPicker.launch(arrayOf("application/pdf"))},modifier=Modifier.fillMaxWidth()){
                Text(rsPromoUiV45(lang,"full_pdf")+(if(book.fullPdfUri.isNotBlank())" ✓" else ""))
            }
            Text(rsPromoUiV45(lang,"access"),color=c.muted,fontSize=10.sp)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","BASIC","PRO","ELITE","PRIVATE").forEach{tier->
                    FilterChip(
                        selected=book.accessTier==tier,
                        onClick={book=book.copy(accessTier=tier)},
                        label={Text(tier,fontSize=8.sp)},
                        modifier=Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                giftedText,
                {giftedText=it},
                label={Text(rsPromoUiV45(lang,"gift"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2
            )
            Button(
                onClick={
                    val emails=giftedText.split(',',';','\n').map{it.trim().lowercase()}.filter{it.contains("@")}.toSet()
                    book=book.copy(giftedEmails=emails)
                    rsSaveBookV45(store,book)
                    promoStatus="Book settings saved."
                    revision++
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"save_book"))}
        }
    }
}

@Composable
fun RsPromotionPageV45(c:RsPalette,store:RsStore,lang:RsLang,onOpenBook:()->Unit){
    val context=LocalContext.current
    val promos=rsLoadPromosV45(store).filter{it.active&&it.imageUri.isNotBlank()}
    val book=rsLoadBookV45(store)
    val listState=rememberLazyListState()
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
                            Text(item.externalUrl,color=c.muted,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if(book.coverUri.isNotBlank()||book.amazonUrl.isNotBlank()||book.previewPdfUri.isNotBlank()){
            Text(rsPromoUiV45(lang,"book"),color=c.bright,fontWeight=FontWeight.Black)
            RsPanel(c){
                if(book.coverUri.isNotBlank())RsUriPreviewV21(
                    book.coverUri,
                    Modifier.fillMaxWidth().height(230.dp).clickable{rsOpenExternalV45(context,book.amazonUrl)},
                    "CENTER"
                )
                Text(book.title,color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
                if(rsValidExternalV45(book.amazonUrl))Button(
                    onClick={rsOpenExternalV45(context,book.amazonUrl)},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPromoUiV45(lang,"buy"))}
                if(book.previewPdfUri.isNotBlank())OutlinedButton(onClick=onOpenBook,modifier=Modifier.fillMaxWidth()){
                    Text(rsPromoUiV45(lang,"open_reader"))
                }
            }
        }
    }
}

@Composable
fun RsBookLibraryV45(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    val book=rsLoadBookV45(store)
    var readerUri by remember{mutableStateOf("")}
    var readerTitle by remember{mutableStateOf(book.title)}
    val fullAllowed=rsCanReadFullV45(store,book)

    if(readerUri.isNotBlank()){
        RsPdfBookReaderV45(c,lang,readerTitle,readerUri){readerUri=""}
        return
    }

    RsScroll(c,rsRouteTitle(lang,"book","Trainer Book"),book.title){
        RsPanel(c){
            if(book.coverUri.isNotBlank())RsUriPreviewV21(
                book.coverUri,
                Modifier.fillMaxWidth().height(270.dp).clickable{rsOpenExternalV45(context,book.amazonUrl)},
                "CENTER"
            )
            Text(book.title,color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)
            if(rsValidExternalV45(book.amazonUrl))Button(
                onClick={rsOpenExternalV45(context,book.amazonUrl)},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"buy"))}
            if(book.previewPdfUri.isNotBlank())OutlinedButton(
                onClick={readerTitle=book.title+" · Preview";readerUri=book.previewPdfUri},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsPromoUiV45(lang,"preview"))}
            if(book.fullPdfUri.isNotBlank()){
                Button(
                    onClick={readerTitle=book.title;readerUri=book.fullPdfUri},
                    enabled=fullAllowed,
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsPromoUiV45(lang,"read"))}
                if(!fullAllowed)Text(rsPromoUiV45(lang,"locked"),color=c.muted,fontSize=10.sp)
            }
        }
    }
}

@Composable
fun RsBookManagerV45(c:RsPalette,store:RsStore,lang:RsLang){
    RsPromotionManagerV45(c,store,lang)
}

@Composable
private fun RsPdfBookReaderV45(c:RsPalette,lang:RsLang,title:String,uriString:String,onBack:()->Unit){
    val context=LocalContext.current
    val file=remember(uriString){Uri.parse(uriString).path?.let(::File)}
    var renderer by remember(uriString){mutableStateOf<PdfRenderer?>(null)}
    var descriptor by remember(uriString){mutableStateOf<ParcelFileDescriptor?>(null)}
    var pageCount by remember(uriString){mutableIntStateOf(0)}
    var search by remember{mutableStateOf("")}
    var searching by remember{mutableStateOf(false)}
    var matches by remember{mutableStateOf<List<Int>>(emptyList())}
    var matchIndex by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()

    DisposableEffect(uriString){
        val opened=runCatching{
            val f=file?:error("Missing PDF file")
            val pfd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY)
            val pdf=PdfRenderer(pfd)
            descriptor=pfd
            renderer=pdf
            pageCount=pdf.pageCount
        }
        if(opened.isFailure)message="Could not open this PDF."
        onDispose{
            runCatching{renderer?.close()}
            runCatching{descriptor?.close()}
            renderer=null
            descriptor=null
        }
    }

    val pagerState=rememberPagerState(pageCount={max(1,pageCount)})

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick=onBack,modifier=Modifier.weight(.35f)){Text(rsPromoUiV45(lang,"back"),fontSize=10.sp)}
            Text(title,color=c.bright,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(.65f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedTextField(
                search,{search=it.take(80)},
                label={Text(rsPromoUiV45(lang,"search"))},
                modifier=Modifier.weight(1f),
                singleLine=true
            )
            Button(
                onClick={
                    if(search.isBlank()||file==null)return@Button
                    searching=true
                    message=""
                    scope.launch{
                        val result=withContext(Dispatchers.IO){
                            runCatching{
                                PDFBoxResourceLoader.init(context)
                                PDDocument.load(file).use{doc->
                                    val stripper=PDFTextStripper()
                                    buildList{
                                        for(p in 1..doc.numberOfPages){
                                            stripper.startPage=p
                                            stripper.endPage=p
                                            val text=stripper.getText(doc)
                                            if(text.contains(search,ignoreCase=true))add(p-1)
                                        }
                                    }
                                }
                            }.getOrDefault(emptyList())
                        }
                        matches=result
                        matchIndex=0
                        searching=false
                        if(result.isNotEmpty())pagerState.animateScrollToPage(result.first())
                        else message=rsPromoUiV45(lang,"no_match")
                    }
                },
                enabled=!searching&&search.isNotBlank(),
                modifier=Modifier.widthIn(min=84.dp)
            ){Text(if(searching)"…" else "Search")}
        }
        if(matches.isNotEmpty())Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text((matchIndex+1).toString()+" / "+matches.size,color=c.muted,fontSize=10.sp)
            TextButton(onClick={
                if(matches.isNotEmpty()){
                    matchIndex=(matchIndex+1)%matches.size
                    scope.launch{pagerState.animateScrollToPage(matches[matchIndex])}
                }
            }){Text(rsPromoUiV45(lang,"next_match"))}
        }
        if(message.isNotBlank())Text(message,color=c.muted,fontSize=10.sp)
        if(pageCount>0){
            HorizontalPager(
                state=pagerState,
                modifier=Modifier.fillMaxWidth().weight(1f)
            ){page->
                val pageOffset=((pagerState.currentPage-page)+pagerState.currentPageOffsetFraction).coerceIn(-1f,1f)
                RsPdfPageV45(
                    c=c,
                    renderer=renderer,
                    page=page,
                    modifier=Modifier.fillMaxSize().graphicsLayer{
                        rotationY=-pageOffset*18f
                        scaleX=1f-(pageOffset.absoluteValue*.04f)
                        scaleY=1f-(pageOffset.absoluteValue*.04f)
                        alpha=1f-(pageOffset.absoluteValue*.18f)
                    }
                )
            }
            Text(
                rsPromoUiV45(lang,"page")+" "+(pagerState.currentPage+1)+" / "+pageCount,
                color=c.muted,
                modifier=Modifier.fillMaxWidth()
            )
        }else Box(Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun RsPdfPageV45(c:RsPalette,renderer:PdfRenderer?,page:Int,modifier:Modifier=Modifier){
    var scale by remember(page){mutableFloatStateOf(1f)}
    var offsetX by remember(page){mutableFloatStateOf(0f)}
    var offsetY by remember(page){mutableFloatStateOf(0f)}
    val bitmap by produceState<Bitmap?>(null,renderer,page){
        value=withContext(Dispatchers.IO){
            val r=renderer?:return@withContext null
            synchronized(r){
                runCatching{
                    r.openPage(page).use{p->
                        val targetW=1000
                        val targetH=max(1,(targetW*(p.height.toFloat()/p.width)).toInt())
                        Bitmap.createBitmap(targetW,targetH,Bitmap.Config.ARGB_8888).also{bmp->
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            p.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }.getOrNull()
            }
        }
    }
    Surface(color=c.panel,shape=MaterialTheme.shapes.medium,modifier=modifier){
        if(bitmap!=null)Image(
            bitmap=bitmap!!.asImageBitmap(),
            contentDescription="Book page "+(page+1),
            modifier=Modifier.fillMaxSize()
                .pointerInput(page){
                    detectTransformGestures{_,pan,zoom,_->
                        scale=(scale*zoom).coerceIn(1f,4f)
                        if(scale>1f){
                            offsetX+=pan.x
                            offsetY+=pan.y
                        }else{
                            offsetX=0f;offsetY=0f
                        }
                    }
                }
                .graphicsLayer{
                    scaleX=scale;scaleY=scale
                    translationX=offsetX;translationY=offsetY
                }
        )
    }
}

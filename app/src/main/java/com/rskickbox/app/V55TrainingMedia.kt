package com.rskickbox.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class RsTrainingMediaItemV55(
    val id:String,
    val title:String,
    val category:String,
    val description:String,
    val uri:String,
    val kind:String,
    val accessTier:String,
    val published:Boolean
)

private fun rsTrainingMediaDirV55(context:android.content.Context)=
    File(context.filesDir,"rs_training_media").apply{mkdirs()}

private fun rsTrainingMediaExtensionV55(context:android.content.Context,source:Uri):String{
    val type=context.contentResolver.getType(source).orEmpty().lowercase()
    return when{
        type.contains("webm")->"webm"
        type.contains("quicktime")->"mov"
        type.contains("3gpp")->"3gp"
        type.contains("png")->"png"
        type.contains("webp")->"webp"
        type.contains("gif")->"gif"
        type.contains("jpeg")||type.contains("jpg")->"jpg"
        type.startsWith("video/")->"mp4"
        else->"bin"
    }
}

private fun rsTrainingMediaKindV55(context:android.content.Context,source:Uri):String{
    val type=context.contentResolver.getType(source).orEmpty().lowercase()
    return when{
        type.startsWith("video/")->"VIDEO"
        type=="image/gif"->"GIF"
        else->"IMAGE"
    }
}

private fun rsCopyTrainingMediaV55(context:android.content.Context,source:Uri):Pair<String,String>{
    val kind=rsTrainingMediaKindV55(context,source)
    val ext=rsTrainingMediaExtensionV55(context,source)
    val out=File(rsTrainingMediaDirV55(context),"media_"+UUID.randomUUID()+"."+ext)
    context.contentResolver.openInputStream(source)!!.use{input->
        FileOutputStream(out).use{output->input.copyTo(output)}
    }
    return Uri.fromFile(out).toString() to kind
}

private fun rsDeleteTrainingMediaV55(context:android.content.Context,uriString:String){
    if(uriString.isBlank())return
    runCatching{
        val uri=Uri.parse(uriString)
        if(uri.scheme!="file")return
        val file=uri.path?.let(::File)?:return
        val root=rsTrainingMediaDirV55(context).canonicalFile
        val target=file.canonicalFile
        if(target.parentFile==root && target.exists())target.delete()
    }
}

private fun rsLoadTrainingMediaV55(store:RsStore):List<RsTrainingMediaItemV55>{
    val raw=store.s("training_media_v55","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsTrainingMediaItemV55(
                    o.optString("id"),o.optString("title"),o.optString("category"),
                    o.optString("description"),o.optString("uri"),o.optString("kind","VIDEO"),
                    o.optString("tier","ALL"),o.optBoolean("published",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsSaveTrainingMediaV55(store:RsStore,items:List<RsTrainingMediaItemV55>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("title",x.title);put("category",x.category);put("description",x.description)
        put("uri",x.uri);put("kind",x.kind);put("tier",x.accessTier);put("published",x.published)
    })}
    store.ps("training_media_v55",a.toString())
}

private fun rsMediaStorageNoteV109(lang:RsLang):String=when(lang.code){
    "nl"->"Bestanden worden geüpload naar beveiligde Supabase Storage. Leerlingen krijgen toegang volgens hun actieve abonnement."
    "pt"->"Os ficheiros são enviados para Supabase Storage protegido. Os alunos recebem acesso conforme o plano ativo."
    "es"->"Los archivos se suben a Supabase Storage protegido. Los alumnos reciben acceso según su plan activo."
    "fr"->"Les fichiers sont envoyés vers un stockage Supabase protégé. Les élèves ont accès selon leur abonnement actif."
    "de"->"Dateien werden in geschützten Supabase Storage hochgeladen. Schüler erhalten Zugriff gemäß ihrem aktiven Plan."
    "it"->"I file vengono caricati su Supabase Storage protetto. Gli allievi ricevono accesso in base al piano attivo."
    "pl"->"Pliki są przesyłane do chronionego Supabase Storage. Uczniowie otrzymują dostęp zgodnie z aktywnym planem."
    "tr"->"Dosyalar korumalı Supabase Storage alanına yüklenir. Öğrenciler aktif planlarına göre erişim alır."
    else->rsMediaStorageNoteV109(lang)
}

private fun rsMediaUiV55(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "Training Media","student_sub" to "Coach videos, drills and visual training resources unlocked for your membership.",
        "trainer_title" to "Training Media Manager","trainer_sub" to "Upload real training videos/images, organize them and control student access.",
        "new" to "NEW TRAINING MEDIA","title" to "Title","category" to "Category","description" to "Description",
        "tier" to "Access","gallery" to "Gallery","files" to "Files","file" to "Choose image / video","save" to "Publish media","published" to "PUBLISHED",
        "draft" to "DRAFT","delete" to "Delete","confirm" to "Confirm","open" to "Open media","close" to "Close media",
        "none" to "No training media available yet.","importing" to "Importing media…","ready" to "Media ready.",
        "preserved" to "Original media is stored privately in the app with video audio preserved.","show_more" to "Show more","import_error" to "Could not import media."
    )
    val nl=en+mapOf(
        "student_title" to "Trainingsmedia","student_sub" to "Coachvideo's, drills en visuele trainingsbronnen voor jouw lidmaatschap.",
        "trainer_title" to "Trainingsmedia-beheer","trainer_sub" to "Upload echte trainingsvideo's/afbeeldingen, organiseer ze en beheer leerlingtoegang.",
        "new" to "NIEUWE TRAININGSMEDIA","title" to "Titel","category" to "Categorie","description" to "Beschrijving",
        "tier" to "Toegang","gallery" to "Galerij","files" to "Bestanden","file" to "Kies afbeelding / video","save" to "Media publiceren","published" to "GEPUBLICEERD",
        "draft" to "CONCEPT","delete" to "Verwijderen","confirm" to "Bevestigen","open" to "Media openen","close" to "Media sluiten",
        "none" to "Nog geen trainingsmedia beschikbaar.","importing" to "Media importeren…","ready" to "Media gereed.",
        "preserved" to "Originele media wordt privé in de app opgeslagen met video-audio behouden.","show_more" to "Meer tonen","import_error" to "Media importeren is mislukt."
    )
    val pt=en+mapOf(
        "student_title" to "Media de Treino","student_sub" to "Vídeos do treinador, exercícios e recursos visuais desbloqueados para o teu plano.",
        "trainer_title" to "Gestor de Media de Treino","trainer_sub" to "Carrega vídeos/imagens reais e controla o acesso dos alunos.",
        "new" to "NOVA MEDIA DE TREINO","title" to "Título","category" to "Categoria","description" to "Descrição",
        "tier" to "Acesso","gallery" to "Galeria","files" to "Ficheiros","file" to "Escolher imagem / vídeo","save" to "Publicar media","published" to "PUBLICADO",
        "draft" to "RASCUNHO","delete" to "Eliminar","confirm" to "Confirmar","open" to "Abrir media","close" to "Fechar media",
        "none" to "Ainda não há media de treino.","importing" to "A importar media…","ready" to "Media pronta.",
        "preserved" to "A media original fica guardada de forma privada com o áudio do vídeo preservado.","show_more" to "Mostrar mais","import_error" to "Não foi possível importar a media."
    )
    val es=en+mapOf(
        "student_title" to "Media de Entrenamiento","student_sub" to "Vídeos del entrenador, ejercicios y recursos visuales desbloqueados para tu membresía.",
        "trainer_title" to "Gestor de Media de Entrenamiento","trainer_sub" to "Sube vídeos/imágenes reales y controla el acceso de los alumnos.",
        "new" to "NUEVA MEDIA DE ENTRENAMIENTO","title" to "Título","category" to "Categoría","description" to "Descripción",
        "tier" to "Acceso","gallery" to "Galería","files" to "Archivos","file" to "Elegir imagen / vídeo","save" to "Publicar media","published" to "PUBLICADO",
        "draft" to "BORRADOR","delete" to "Eliminar","confirm" to "Confirmar","open" to "Abrir media","close" to "Cerrar media",
        "none" to "Aún no hay media de entrenamiento.","importing" to "Importando media…","ready" to "Media lista.",
        "preserved" to "La media original se guarda de forma privada con el audio del vídeo preservado.","show_more" to "Mostrar más","import_error" to "No se pudo importar la media."
    )
    val fr=en+mapOf(
        "student_title" to "Médias d’Entraînement","student_sub" to "Vidéos du coach, exercices et ressources visuelles débloqués pour ton abonnement.",
        "trainer_title" to "Gestion des Médias d’Entraînement","trainer_sub" to "Téléverse de vraies vidéos/images et contrôle l’accès des élèves.",
        "new" to "NOUVEAU MÉDIA D’ENTRAÎNEMENT","title" to "Titre","category" to "Catégorie","description" to "Description",
        "tier" to "Accès","gallery" to "Galerie","files" to "Fichiers","file" to "Choisir image / vidéo","save" to "Publier le média","published" to "PUBLIÉ",
        "draft" to "BROUILLON","delete" to "Supprimer","confirm" to "Confirmer","open" to "Ouvrir le média","close" to "Fermer le média",
        "none" to "Aucun média d’entraînement disponible.","importing" to "Import du média…","ready" to "Média prêt.",
        "preserved" to "Le média original est stocké en privé avec l’audio vidéo conservé.","show_more" to "Afficher plus","import_error" to "Impossible d’importer le média."
    )
    val de=en+mapOf(
        "student_title" to "Trainingsmedien","student_sub" to "Coach-Videos, Drills und visuelle Trainingsressourcen passend zu deiner Mitgliedschaft.",
        "trainer_title" to "Trainingsmedien verwalten","trainer_sub" to "Lade echte Trainingsvideos/-bilder hoch und steuere den Schülerzugang.",
        "new" to "NEUES TRAININGSMEDIUM","title" to "Titel","category" to "Kategorie","description" to "Beschreibung",
        "tier" to "Zugang","gallery" to "Galerie","files" to "Dateien","file" to "Bild / Video wählen","save" to "Medium veröffentlichen","published" to "VERÖFFENTLICHT",
        "draft" to "ENTWURF","delete" to "Löschen","confirm" to "Bestätigen","open" to "Medium öffnen","close" to "Medium schließen",
        "none" to "Noch keine Trainingsmedien verfügbar.","importing" to "Medium wird importiert…","ready" to "Medium bereit.",
        "preserved" to "Das Originalmedium wird privat gespeichert; Video-Audio bleibt erhalten.","show_more" to "Mehr anzeigen","import_error" to "Medium konnte nicht importiert werden."
    )
    val it=en+mapOf(
        "student_title" to "Media di Allenamento","student_sub" to "Video del trainer, esercizi e risorse visive disponibili per il tuo abbonamento.",
        "trainer_title" to "Gestione Media di Allenamento","trainer_sub" to "Carica video/immagini reali e controlla l’accesso degli allievi.",
        "new" to "NUOVO MEDIA DI ALLENAMENTO","title" to "Titolo","category" to "Categoria","description" to "Descrizione",
        "tier" to "Accesso","gallery" to "Galleria","files" to "File","file" to "Scegli immagine / video","save" to "Pubblica media","published" to "PUBBLICATO",
        "draft" to "BOZZA","delete" to "Elimina","confirm" to "Conferma","open" to "Apri media","close" to "Chiudi media",
        "none" to "Nessun media di allenamento disponibile.","importing" to "Importazione media…","ready" to "Media pronto.",
        "preserved" to "Il media originale viene salvato privatamente mantenendo l’audio del video.","show_more" to "Mostra altro","import_error" to "Impossibile importare il media."
    )
    val pl=en+mapOf(
        "student_title" to "Media Treningowe","student_sub" to "Filmy trenera, ćwiczenia i zasoby wizualne dostępne dla twojego planu.",
        "trainer_title" to "Menedżer Mediów Treningowych","trainer_sub" to "Dodawaj prawdziwe filmy/obrazy i kontroluj dostęp uczniów.",
        "new" to "NOWE MEDIA TRENINGOWE","title" to "Tytuł","category" to "Kategoria","description" to "Opis",
        "tier" to "Dostęp","gallery" to "Galeria","files" to "Pliki","file" to "Wybierz obraz / wideo","save" to "Opublikuj media","published" to "OPUBLIKOWANE",
        "draft" to "SZKIC","delete" to "Usuń","confirm" to "Potwierdź","open" to "Otwórz media","close" to "Zamknij media",
        "none" to "Brak dostępnych mediów treningowych.","importing" to "Importowanie mediów…","ready" to "Media gotowe.",
        "preserved" to "Oryginalny plik jest przechowywany prywatnie z zachowaniem dźwięku wideo.","show_more" to "Pokaż więcej","import_error" to "Nie udało się zaimportować mediów."
    )
    val tr=en+mapOf(
        "student_title" to "Antrenman Medyası","student_sub" to "Üyeliğin için açılmış antrenör videoları, çalışmalar ve görsel kaynaklar.",
        "trainer_title" to "Antrenman Medyası Yöneticisi","trainer_sub" to "Gerçek video/görseller yükle ve öğrenci erişimini yönet.",
        "new" to "YENİ ANTRENMAN MEDYASI","title" to "Başlık","category" to "Kategori","description" to "Açıklama",
        "tier" to "Erişim","gallery" to "Galeri","files" to "Dosyalar","file" to "Görsel / video seç","save" to "Medyayı yayınla","published" to "YAYINDA",
        "draft" to "TASLAK","delete" to "Sil","confirm" to "Onayla","open" to "Medyayı aç","close" to "Medyayı kapat",
        "none" to "Henüz antrenman medyası yok.","importing" to "Medya içe aktarılıyor…","ready" to "Medya hazır.",
        "preserved" to "Orijinal medya özel olarak saklanır ve video sesi korunur.","show_more" to "Daha fazla göster","import_error" to "Medya içe aktarılamadı."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsTrainingMediaPlayerV55(c:RsPalette,lang:RsLang,item:RsTrainingMediaItemV55,onClose:()->Unit){
    val context=LocalContext.current
    val player=if(item.kind=="VIDEO")remember(item.uri){
        ExoPlayer.Builder(context).build().apply{
            setMediaItem(MediaItem.fromUri(Uri.parse(item.uri)))
            repeatMode=Player.REPEAT_MODE_OFF
            prepare()
        }
    }else null
    DisposableEffect(player){onDispose{player?.release()}}
    RsScroll(c,item.title,item.category+" · "+item.accessTier){
        OutlinedButton(onClick=onClose,modifier=Modifier.fillMaxWidth()){Text(rsMediaUiV55(lang,"close"))}
        RsPanel(c){
            Text(item.description,color=c.text)
        }
        Surface(shape=MaterialTheme.shapes.large,color=c.panel,modifier=Modifier.fillMaxWidth().heightIn(min=240.dp,max=520.dp)){
            if(item.kind=="VIDEO"){
                AndroidView(
                    factory={ctx->PlayerView(ctx).apply{useController=true;resizeMode=androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;this.player=player}},
                    update={it.player=player},
                    modifier=Modifier.fillMaxWidth().height(360.dp)
                )
            }else{
                RsUriPreviewV21(item.uri,Modifier.fillMaxWidth().heightIn(min=260.dp,max=520.dp),"CENTER")
            }
        }
    }
}

@Composable
fun RsTrainingMediaV55(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    if(RsSupabaseV60.configured){
        RsCloudTrainingMediaScreenV73(c,lang,role)
        return
    }
    val context=LocalContext.current
    var revision by remember{mutableIntStateOf(0)}
    var selected by remember{mutableStateOf<RsTrainingMediaItemV55?>(null)}
    var title by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("TECHNIQUE")}
    var description by remember{mutableStateOf("")}
    var tier by remember{mutableStateOf("ALL")}
    var pickedUri by remember{mutableStateOf("")}
    var pickedKind by remember{mutableStateOf("VIDEO")}
    var importing by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    var visibleCount by remember{mutableIntStateOf(20)}
    val scope=rememberCoroutineScope()
    val pendingUriState=rememberUpdatedState(pickedUri)
    DisposableEffect(Unit){
        onDispose{
            val pending=pendingUriState.value
            if(pending.isNotBlank())rsDeleteTrainingMediaV55(context,pending)
        }
    }

    val all=remember(revision){rsLoadTrainingMediaV55(store)}
    val studentRank=rsContentRankV48(rsContentStudentTierV48(store))
    val visible=if(role==RsRole.TRAINER)all else all.filter{
        it.published && rsContentRankV48(it.accessTier)<=studentRank
    }
    val shown=visible.take(visibleCount)

    fun importPicked(uri:Uri?){
        if(uri==null)return
        importing=true
        status=rsMediaUiV55(lang,"importing")
        scope.launch{
            val result=withContext(Dispatchers.IO){
                runCatching{rsCopyTrainingMediaV55(context,uri)}
            }
            result.onSuccess{(saved,kind)->
                val previous=pickedUri
                pickedUri=saved
                pickedKind=kind
                if(previous.isNotBlank()&&previous!=saved){
                    withContext(Dispatchers.IO){rsDeleteTrainingMediaV55(context,previous)}
                }
                status=rsMediaUiV55(lang,"ready")
            }.onFailure{
                status=rsReleaseT98(lang,"save_failed")
            }
            importing=false
        }
    }

    val galleryPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        importPicked(uri)
    }
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        importPicked(uri)
    }

    fun save(items:List<RsTrainingMediaItemV55>){
        rsSaveTrainingMediaV55(store,items)
        revision++
    }

    if(selected!=null){
        RsTrainingMediaPlayerV55(c,lang,selected!!){selected=null}
        return
    }

    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsMediaUiV55(lang,"trainer_title") else rsMediaUiV55(lang,"student_title"),
        if(role==RsRole.TRAINER)rsMediaUiV55(lang,"trainer_sub") else rsMediaUiV55(lang,"student_sub")
    ){
        if(role==RsRole.TRAINER){
            RsPanel(c){
                Text(rsMediaUiV55(lang,"new"),color=c.bright,fontWeight=FontWeight.Black)
                Text(rsMediaUiV55(lang,"preserved"),color=c.muted,fontSize=10.sp)
                if(pickedUri.isNotBlank()){
                    RsUriPreviewV21(pickedUri,Modifier.fillMaxWidth().height(170.dp),"CENTER")
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(
                        onClick={galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))},
                        enabled=!importing,
                        modifier=Modifier.weight(1f)
                    ){Text(if(importing)"…" else rsMediaUiV55(lang,"gallery"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={filePicker.launch(arrayOf("video/*","image/*"))},
                        enabled=!importing,
                        modifier=Modifier.weight(1f)
                    ){Text(rsMediaUiV55(lang,"files"),fontSize=10.sp)}
                }
                OutlinedTextField(title,{title=it.take(120)},label={Text(rsMediaUiV55(lang,"title"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(category,{category=it.take(50)},label={Text(rsMediaUiV55(lang,"category"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(description,{description=it.take(1200)},label={Text(rsMediaUiV55(lang,"description"))},modifier=Modifier.fillMaxWidth(),minLines=3)
                Text(rsMediaUiV55(lang,"tier"),color=c.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    listOf("ALL","BASIC","PRO","ELITE").forEach{x->
                        FilterChip(selected=tier==x,onClick={tier=x},label={Text(x,fontSize=9.sp)},modifier=Modifier.weight(1f))
                    }
                }
                Button(
                    onClick={
                        save(listOf(RsTrainingMediaItemV55(
                            UUID.randomUUID().toString(),title.trim(),category.trim(),description.trim(),
                            pickedUri,pickedKind,tier,true
                        ))+all)
                        title="";category="TECHNIQUE";description="";tier="ALL";pickedUri="";pickedKind="VIDEO";status=""
                    },
                    enabled=title.isNotBlank()&&pickedUri.isNotBlank()&&!importing,
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsMediaUiV55(lang,"save"))}
                if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
            }
        }

        if(visible.isEmpty())RsPanel(c){Text(rsMediaUiV55(lang,"none"),color=c.muted)}

        shown.forEach{item->
            RsPanel(c){
                if(item.kind=="VIDEO"){
                    Surface(
                        color=c.gold.copy(alpha=.10f),
                        shape=MaterialTheme.shapes.large,
                        modifier=Modifier.fillMaxWidth().height(150.dp)
                    ){
                        Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
                                Text("▶",color=c.bright,fontSize=34.sp,fontWeight=FontWeight.Black)
                                Text("VIDEO",color=c.muted,fontSize=9.sp)
                            }
                        }
                    }
                }else{
                    RsUriPreviewV21(item.uri,Modifier.fillMaxWidth().height(170.dp),"CENTER")
                }
                Text(item.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(item.category+" · "+item.accessTier+" · "+item.kind,color=c.muted,fontSize=10.sp)
                if(item.description.isNotBlank())Text(item.description,color=c.text,maxLines=4)
                Button(onClick={selected=item},modifier=Modifier.fillMaxWidth()){Text(rsMediaUiV55(lang,"open"))}

                if(role==RsRole.TRAINER){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(item.published)rsMediaUiV55(lang,"published") else rsMediaUiV55(lang,"draft"),color=c.muted)
                        Switch(item.published,{v->save(all.map{if(it.id==item.id)it.copy(published=v) else it})})
                    }
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==item.id){
                                rsDeleteTrainingMediaV55(context,item.uri)
                                save(all.filterNot{it.id==item.id})
                                pendingDelete=null
                            }else pendingDelete=item.id
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==item.id)rsMediaUiV55(lang,"confirm") else rsMediaUiV55(lang,"delete"))}
                }
            }
        }
        if(visibleCount<visible.size){
            OutlinedButton(
                onClick={visibleCount=(visibleCount+20).coerceAtMost(visible.size)},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsMediaUiV55(lang,"show_more")+" · "+visibleCount.coerceAtMost(visible.size)+" / "+visible.size)}
        }
    }
}


@Composable
private fun RsCloudTrainingMediaScreenV73(c:RsPalette,lang:RsLang,role:RsRole){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var all by remember{mutableStateOf<List<RsTrainingMediaItemV55>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var selected by remember{mutableStateOf<RsTrainingMediaItemV55?>(null)}
    var selectedLocalUri by remember{mutableStateOf("")}
    var title by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("TECHNIQUE")}
    var description by remember{mutableStateOf("")}
    var tier by remember{mutableStateOf("ALL")}
    var pickedSource by remember{mutableStateOf<Uri?>(null)}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsCloudTrainingMediaV73()
            .onSuccess{all=it}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    fun pick(uri:Uri?){
        pickedSource=uri
        status=if(uri==null)"" else "Media selected and ready to upload."
    }

    val galleryPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->pick(uri)}
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->pick(uri)}

    val activeSelected=selected
    if(activeSelected!=null){
        LaunchedEffect(activeSelected.id){
            selectedLocalUri=""
            status=rsCloudT93(lang,"downloading_media")
            rsCloudTrainingMediaLocalUriV73(context,activeSelected)
                .onSuccess{selectedLocalUri=it;status=""}
                .onFailure{status=rsReleaseT98(lang,"load_failed")}
        }
        if(selectedLocalUri.isBlank()){
            RsScroll(c,activeSelected.title,activeSelected.category+" · "+activeSelected.accessTier){
                OutlinedButton(
                    onClick={selected=null;selectedLocalUri="";status=""},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsMediaUiV55(lang,"close"))}
                RsPanel(c){
                    CircularProgressIndicator()
                    Text(status.ifBlank{"Loading protected media…"},color=c.muted)
                }
            }
        }else{
            RsTrainingMediaPlayerV55(
                c,
                lang,
                activeSelected.copy(uri=selectedLocalUri)
            ){
                selected=null
                selectedLocalUri=""
                status=""
            }
        }
        return
    }

    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsMediaUiV55(lang,"trainer_title") else rsMediaUiV55(lang,"student_title"),
        if(role==RsRole.TRAINER)rsMediaUiV55(lang,"trainer_sub") else rsMediaUiV55(lang,"student_sub")
    ){
        RsPanel(c){
            Text(
                if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        if(role==RsRole.TRAINER){
            RsPanel(c){
                Text(rsMediaUiV55(lang,"new"),color=c.bright,fontWeight=FontWeight.Black)
                Text(
                    "Files are uploaded to protected Supabase Storage. Students receive access according to their active plan.",
                    color=c.muted,
                    fontSize=10.sp
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(
                        onClick={galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))},
                        enabled=!busy,
                        modifier=Modifier.weight(1f)
                    ){Text(rsMediaUiV55(lang,"gallery"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={filePicker.launch(arrayOf("video/*","image/*"))},
                        enabled=!busy,
                        modifier=Modifier.weight(1f)
                    ){Text(rsMediaUiV55(lang,"files"),fontSize=10.sp)}
                }
                Text(
                    if(pickedSource!=null)"✓ Media selected" else "Choose an image or video up to 100 MB.",
                    color=if(pickedSource!=null)c.bright else c.muted,
                    fontSize=10.sp
                )
                OutlinedTextField(
                    title,
                    {title=it.take(120)},
                    label={Text(rsMediaUiV55(lang,"title"))},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=!busy
                )
                OutlinedTextField(
                    category,
                    {category=it.take(50)},
                    label={Text(rsMediaUiV55(lang,"category"))},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=!busy
                )
                OutlinedTextField(
                    description,
                    {description=it.take(1200)},
                    label={Text(rsMediaUiV55(lang,"description"))},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=3,
                    enabled=!busy
                )
                Text(rsMediaUiV55(lang,"tier"),color=c.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    listOf("ALL","BASIC","PRO","ELITE").forEach{x->
                        FilterChip(
                            selected=tier==x,
                            onClick={tier=x},
                            enabled=!busy,
                            label={Text(x,fontSize=9.sp)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                }
                Button(
                    onClick={
                        val source=pickedSource?:return@Button
                        busy=true
                        status=rsCloudT93(lang,"uploading_media")
                        scope.launch{
                            rsUploadCloudTrainingMediaV73(
                                context,
                                source,
                                title,
                                category,
                                description,
                                tier,
                                true
                            )
                                .onSuccess{
                                    title=""
                                    category="TECHNIQUE"
                                    description=""
                                    tier="ALL"
                                    pickedSource=null
                                    status=rsCloudT93(lang,"media_published")
                                    revision++
                                }
                                .onFailure{status=rsReleaseT98(lang,"save_failed")}
                            busy=false
                        }
                    },
                    enabled=!busy&&title.isNotBlank()&&pickedSource!=null,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(busy)"Uploading…" else rsMediaUiV55(lang,"save"))}
            }
        }

        if(all.isEmpty()&&!loading)RsPanel(c){Text(rsMediaUiV55(lang,"none"),color=c.muted)}

        all.forEach{item->
            RsPanel(c){
                Surface(
                    color=c.gold.copy(alpha=.10f),
                    shape=MaterialTheme.shapes.large,
                    modifier=Modifier.fillMaxWidth().height(140.dp)
                ){
                    Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){
                        Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
                            Text(if(item.kind=="VIDEO")"▶" else "▣",color=c.bright,fontSize=34.sp,fontWeight=FontWeight.Black)
                            Text(item.kind,color=c.muted,fontSize=9.sp)
                        }
                    }
                }
                Text(item.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(item.category+" · "+item.accessTier+" · "+item.kind,color=c.muted,fontSize=10.sp)
                if(item.description.isNotBlank())Text(item.description,color=c.text,maxLines=4)
                Button(
                    onClick={selected=item;status=""},
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsMediaUiV55(lang,"open"))}

                if(role==RsRole.TRAINER){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(item.published)rsMediaUiV55(lang,"published") else rsMediaUiV55(lang,"draft"),color=c.muted)
                        Switch(
                            item.published,
                            {value->
                                busy=true
                                scope.launch{
                                    rsSetCloudTrainingMediaPublishedV73(item.id,value)
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
                                    rsDeleteCloudTrainingMediaV73(item.id)
                                        .onSuccess{
                                            pendingDelete=null
                                            status=rsCloudT93(lang,"media_deleted")
                                            revision++
                                        }
                                        .onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                    busy=false
                                }
                            }else pendingDelete=item.id
                        },
                        enabled=!busy,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==item.id)rsMediaUiV55(lang,"confirm") else rsMediaUiV55(lang,"delete"))}
                }
            }
        }
    }
}

package com.rskickbox.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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

private fun rsMediaUiV55(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "Training Media","student_sub" to "Coach videos, drills and visual training resources unlocked for your membership.",
        "trainer_title" to "Training Media Manager","trainer_sub" to "Upload real training videos/images, organize them and control student access.",
        "new" to "NEW TRAINING MEDIA","title" to "Title","category" to "Category","description" to "Description",
        "tier" to "Access","file" to "Choose image / video","save" to "Publish media","published" to "PUBLISHED",
        "draft" to "DRAFT","delete" to "Delete","confirm" to "Confirm","open" to "Open media","close" to "Close media",
        "none" to "No training media available yet.","importing" to "Importing media…","ready" to "Media ready.",
        "preserved" to "Original media is stored privately in the app with video audio preserved."
    )
    val nl=en+mapOf(
        "student_title" to "Trainingsmedia","student_sub" to "Coachvideo's, drills en visuele trainingsbronnen voor jouw lidmaatschap.",
        "trainer_title" to "Trainingsmedia-beheer","trainer_sub" to "Upload echte trainingsvideo's/afbeeldingen, organiseer ze en beheer leerlingtoegang.",
        "new" to "NIEUWE TRAININGSMEDIA","title" to "Titel","category" to "Categorie","description" to "Beschrijving",
        "tier" to "Toegang","file" to "Kies afbeelding / video","save" to "Media publiceren","published" to "GEPUBLICEERD",
        "draft" to "CONCEPT","delete" to "Verwijderen","confirm" to "Bevestigen","open" to "Media openen","close" to "Media sluiten",
        "none" to "Nog geen trainingsmedia beschikbaar.","importing" to "Media importeren…","ready" to "Media gereed.",
        "preserved" to "Originele media wordt privé in de app opgeslagen met video-audio behouden."
    )
    val pt=en+mapOf(
        "student_title" to "Media de Treino","student_sub" to "Vídeos do treinador, exercícios e recursos visuais desbloqueados para o teu plano.",
        "trainer_title" to "Gestor de Media de Treino","trainer_sub" to "Carrega vídeos/imagens reais e controla o acesso dos alunos.",
        "new" to "NOVA MEDIA DE TREINO","title" to "Título","category" to "Categoria","description" to "Descrição",
        "tier" to "Acesso","file" to "Escolher imagem / vídeo","save" to "Publicar media","published" to "PUBLICADO",
        "draft" to "RASCUNHO","delete" to "Eliminar","confirm" to "Confirmar","open" to "Abrir media","close" to "Fechar media",
        "none" to "Ainda não há media de treino.","importing" to "A importar media…","ready" to "Media pronta.",
        "preserved" to "A media original fica guardada de forma privada com o áudio do vídeo preservado."
    )
    val es=en+mapOf(
        "student_title" to "Media de Entrenamiento","student_sub" to "Vídeos del entrenador, ejercicios y recursos visuales desbloqueados para tu membresía.",
        "trainer_title" to "Gestor de Media de Entrenamiento","trainer_sub" to "Sube vídeos/imágenes reales y controla el acceso de los alumnos.",
        "new" to "NUEVA MEDIA DE ENTRENAMIENTO","title" to "Título","category" to "Categoría","description" to "Descripción",
        "tier" to "Acceso","file" to "Elegir imagen / vídeo","save" to "Publicar media","published" to "PUBLICADO",
        "draft" to "BORRADOR","delete" to "Eliminar","confirm" to "Confirmar","open" to "Abrir media","close" to "Cerrar media",
        "none" to "Aún no hay media de entrenamiento.","importing" to "Importando media…","ready" to "Media lista.",
        "preserved" to "La media original se guarda de forma privada con el audio del vídeo preservado."
    )
    val fr=en+mapOf(
        "student_title" to "Médias d’Entraînement","student_sub" to "Vidéos du coach, exercices et ressources visuelles débloqués pour ton abonnement.",
        "trainer_title" to "Gestion des Médias d’Entraînement","trainer_sub" to "Téléverse de vraies vidéos/images et contrôle l’accès des élèves.",
        "new" to "NOUVEAU MÉDIA D’ENTRAÎNEMENT","title" to "Titre","category" to "Catégorie","description" to "Description",
        "tier" to "Accès","file" to "Choisir image / vidéo","save" to "Publier le média","published" to "PUBLIÉ",
        "draft" to "BROUILLON","delete" to "Supprimer","confirm" to "Confirmer","open" to "Ouvrir le média","close" to "Fermer le média",
        "none" to "Aucun média d’entraînement disponible.","importing" to "Import du média…","ready" to "Média prêt.",
        "preserved" to "Le média original est stocké en privé avec l’audio vidéo conservé."
    )
    val de=en+mapOf(
        "student_title" to "Trainingsmedien","student_sub" to "Coach-Videos, Drills und visuelle Trainingsressourcen passend zu deiner Mitgliedschaft.",
        "trainer_title" to "Trainingsmedien verwalten","trainer_sub" to "Lade echte Trainingsvideos/-bilder hoch und steuere den Schülerzugang.",
        "new" to "NEUES TRAININGSMEDIUM","title" to "Titel","category" to "Kategorie","description" to "Beschreibung",
        "tier" to "Zugang","file" to "Bild / Video wählen","save" to "Medium veröffentlichen","published" to "VERÖFFENTLICHT",
        "draft" to "ENTWURF","delete" to "Löschen","confirm" to "Bestätigen","open" to "Medium öffnen","close" to "Medium schließen",
        "none" to "Noch keine Trainingsmedien verfügbar.","importing" to "Medium wird importiert…","ready" to "Medium bereit.",
        "preserved" to "Das Originalmedium wird privat gespeichert; Video-Audio bleibt erhalten."
    )
    val it=en+mapOf(
        "student_title" to "Media di Allenamento","student_sub" to "Video del trainer, esercizi e risorse visive disponibili per il tuo abbonamento.",
        "trainer_title" to "Gestione Media di Allenamento","trainer_sub" to "Carica video/immagini reali e controlla l’accesso degli allievi.",
        "new" to "NUOVO MEDIA DI ALLENAMENTO","title" to "Titolo","category" to "Categoria","description" to "Descrizione",
        "tier" to "Accesso","file" to "Scegli immagine / video","save" to "Pubblica media","published" to "PUBBLICATO",
        "draft" to "BOZZA","delete" to "Elimina","confirm" to "Conferma","open" to "Apri media","close" to "Chiudi media",
        "none" to "Nessun media di allenamento disponibile.","importing" to "Importazione media…","ready" to "Media pronto.",
        "preserved" to "Il media originale viene salvato privatamente mantenendo l’audio del video."
    )
    val pl=en+mapOf(
        "student_title" to "Media Treningowe","student_sub" to "Filmy trenera, ćwiczenia i zasoby wizualne dostępne dla twojego planu.",
        "trainer_title" to "Menedżer Mediów Treningowych","trainer_sub" to "Dodawaj prawdziwe filmy/obrazy i kontroluj dostęp uczniów.",
        "new" to "NOWE MEDIA TRENINGOWE","title" to "Tytuł","category" to "Kategoria","description" to "Opis",
        "tier" to "Dostęp","file" to "Wybierz obraz / wideo","save" to "Opublikuj media","published" to "OPUBLIKOWANE",
        "draft" to "SZKIC","delete" to "Usuń","confirm" to "Potwierdź","open" to "Otwórz media","close" to "Zamknij media",
        "none" to "Brak dostępnych mediów treningowych.","importing" to "Importowanie mediów…","ready" to "Media gotowe.",
        "preserved" to "Oryginalny plik jest przechowywany prywatnie z zachowaniem dźwięku wideo."
    )
    val tr=en+mapOf(
        "student_title" to "Antrenman Medyası","student_sub" to "Üyeliğin için açılmış antrenör videoları, çalışmalar ve görsel kaynaklar.",
        "trainer_title" to "Antrenman Medyası Yöneticisi","trainer_sub" to "Gerçek video/görseller yükle ve öğrenci erişimini yönet.",
        "new" to "YENİ ANTRENMAN MEDYASI","title" to "Başlık","category" to "Kategori","description" to "Açıklama",
        "tier" to "Erişim","file" to "Görsel / video seç","save" to "Medyayı yayınla","published" to "YAYINDA",
        "draft" to "TASLAK","delete" to "Sil","confirm" to "Onayla","open" to "Medyayı aç","close" to "Medyayı kapat",
        "none" to "Henüz antrenman medyası yok.","importing" to "Medya içe aktarılıyor…","ready" to "Medya hazır.",
        "preserved" to "Orijinal medya özel olarak saklanır ve video sesi korunur."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
private fun RsTrainingMediaPlayerV55(c:RsPalette,item:RsTrainingMediaItemV55,onClose:()->Unit){
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
        OutlinedButton(onClick=onClose,modifier=Modifier.fillMaxWidth()){Text("Close")}
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
    val scope=rememberCoroutineScope()

    val all=remember(revision){rsLoadTrainingMediaV55(store)}
    val studentRank=rsContentRankV48(rsContentStudentTierV48(store))
    val visible=if(role==RsRole.TRAINER)all else all.filter{
        it.published && rsContentRankV48(it.accessTier)<=studentRank
    }

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
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
                    status=it.message?:"Could not import media."
                }
                importing=false
            }
        }
    }

    fun save(items:List<RsTrainingMediaItemV55>){
        rsSaveTrainingMediaV55(store,items)
        revision++
    }

    if(selected!=null){
        RsTrainingMediaPlayerV55(c,selected!!){selected=null}
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
                OutlinedButton(
                    onClick={picker.launch(arrayOf("video/*","image/*"))},
                    enabled=!importing,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(importing)rsMediaUiV55(lang,"importing") else rsMediaUiV55(lang,"file"))}
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

        visible.forEach{item->
            RsPanel(c){
                RsUriPreviewV21(item.uri,Modifier.fillMaxWidth().height(170.dp),"CENTER")
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
    }
}

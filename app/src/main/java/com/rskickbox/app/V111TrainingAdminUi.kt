package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private fun rsV111Ui(lang:RsLang,key:String):String{
    val en=mapOf(
        "files" to "Manage stored files",
        "hide_files" to "Hide stored files",
        "no_files" to "No tracked student media yet.",
        "delete" to "Delete file",
        "confirm" to "Confirm delete",
        "deleted" to "Cloud file deleted.",
        "delete_failed" to "Could not delete this cloud file.",
        "plan" to "Private lesson training plan",
        "plan_help" to "Attach one reusable training plan to this confirmed private lesson. It also appears in the student's Homework.",
        "choose_plan" to "Choose training plan",
        "due" to "Due / follow-up label",
        "assign" to "Assign plan to private lesson",
        "assigned" to "Training plan assigned.",
        "current_plan" to "Assigned plan",
        "backend" to "This feature will activate after the v0.111 Supabase update is installed."
    )
    val nl=en+mapOf(
        "files" to "Opgeslagen bestanden beheren","hide_files" to "Bestanden verbergen","no_files" to "Nog geen bijgehouden leerlingmedia.",
        "delete" to "Bestand verwijderen","confirm" to "Verwijderen bevestigen","deleted" to "Cloudbestand verwijderd.","delete_failed" to "Cloudbestand kon niet worden verwijderd.",
        "plan" to "Trainingsplan privéles","plan_help" to "Koppel één herbruikbaar trainingsplan aan deze bevestigde privéles. Het verschijnt ook in Huiswerk.",
        "choose_plan" to "Kies trainingsplan","due" to "Deadline / follow-up","assign" to "Plan aan privéles koppelen","assigned" to "Trainingsplan gekoppeld.",
        "current_plan" to "Gekoppeld plan","backend" to "Deze functie wordt actief nadat de v0.111 Supabase-update is geïnstalleerd."
    )
    val pt=en+mapOf(
        "files" to "Gerir ficheiros guardados","hide_files" to "Ocultar ficheiros","no_files" to "Ainda não há media do aluno registada.",
        "delete" to "Eliminar ficheiro","confirm" to "Confirmar eliminação","deleted" to "Ficheiro cloud eliminado.","delete_failed" to "Não foi possível eliminar o ficheiro.",
        "plan" to "Plano de treino da aula privada","plan_help" to "Liga um plano reutilizável a esta aula privada confirmada. Também aparece nas Tarefas do aluno.",
        "choose_plan" to "Escolher plano","due" to "Prazo / acompanhamento","assign" to "Atribuir plano à aula privada","assigned" to "Plano de treino atribuído.",
        "current_plan" to "Plano atribuído","backend" to "Esta função fica ativa depois de instalar a atualização Supabase v0.111."
    )
    val es=en+mapOf("files" to "Gestionar archivos guardados","hide_files" to "Ocultar archivos","no_files" to "Todavía no hay media registrada.","delete" to "Eliminar archivo","confirm" to "Confirmar eliminación","deleted" to "Archivo cloud eliminado.","delete_failed" to "No se pudo eliminar el archivo.","plan" to "Plan de entrenamiento de clase privada","choose_plan" to "Elegir plan","due" to "Fecha / seguimiento","assign" to "Asignar plan","assigned" to "Plan asignado.","current_plan" to "Plan asignado")
    val fr=en+mapOf("files" to "Gérer les fichiers stockés","hide_files" to "Masquer les fichiers","no_files" to "Aucun média élève suivi.","delete" to "Supprimer le fichier","confirm" to "Confirmer","deleted" to "Fichier cloud supprimé.","delete_failed" to "Impossible de supprimer le fichier.","plan" to "Plan d’entraînement du cours privé","choose_plan" to "Choisir le plan","due" to "Échéance / suivi","assign" to "Attribuer le plan","assigned" to "Plan attribué.","current_plan" to "Plan attribué")
    val de=en+mapOf("files" to "Gespeicherte Dateien verwalten","hide_files" to "Dateien ausblenden","no_files" to "Noch keine erfassten Schülermedien.","delete" to "Datei löschen","confirm" to "Löschen bestätigen","deleted" to "Cloud-Datei gelöscht.","delete_failed" to "Cloud-Datei konnte nicht gelöscht werden.","plan" to "Trainingsplan Privatstunde","choose_plan" to "Trainingsplan wählen","due" to "Fällig / Nachbereitung","assign" to "Plan zuweisen","assigned" to "Plan zugewiesen.","current_plan" to "Zugewiesener Plan")
    val it=en+mapOf("files" to "Gestisci file salvati","hide_files" to "Nascondi file","no_files" to "Nessun media allievo tracciato.","delete" to "Elimina file","confirm" to "Conferma eliminazione","deleted" to "File cloud eliminato.","delete_failed" to "Impossibile eliminare il file.","plan" to "Piano lezione privata","choose_plan" to "Scegli piano","due" to "Scadenza / follow-up","assign" to "Assegna piano","assigned" to "Piano assegnato.","current_plan" to "Piano assegnato")
    val pl=en+mapOf("files" to "Zarządzaj zapisanymi plikami","hide_files" to "Ukryj pliki","no_files" to "Brak śledzonych mediów ucznia.","delete" to "Usuń plik","confirm" to "Potwierdź usunięcie","deleted" to "Plik w chmurze usunięty.","delete_failed" to "Nie udało się usunąć pliku.","plan" to "Plan treningowy lekcji prywatnej","choose_plan" to "Wybierz plan","due" to "Termin / follow-up","assign" to "Przypisz plan","assigned" to "Plan przypisany.","current_plan" to "Przypisany plan")
    val tr=en+mapOf("files" to "Saklanan dosyaları yönet","hide_files" to "Dosyaları gizle","no_files" to "Henüz takip edilen öğrenci medyası yok.","delete" to "Dosyayı sil","confirm" to "Silmeyi onayla","deleted" to "Bulut dosyası silindi.","delete_failed" to "Dosya silinemedi.","plan" to "Özel ders antrenman planı","choose_plan" to "Plan seç","due" to "Son tarih / takip","assign" to "Plan ata","assigned" to "Plan atandı.","current_plan" to "Atanan plan")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

private fun rsFileSizeV111(bytes:Long):String{
    val mb=bytes/1048576.0
    return if(mb>=1.0)String.format(java.util.Locale.US,"%.1f MB",mb)
    else String.format(java.util.Locale.US,"%.0f KB",bytes/1024.0)
}

@Composable
fun RsStudentStorageCleanupV111(c:RsPalette,lang:RsLang,studentId:String){
    val scope=rememberCoroutineScope()
    var expanded by remember(studentId){mutableStateOf(false)}
    var assets by remember(studentId){mutableStateOf<List<RsStudentMediaAssetV111>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    var supported by remember{mutableStateOf(true)}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}

    fun reload(){
        loading=true
        scope.launch{
            rsStudentMediaAssetsV111(studentId)
                .onSuccess{assets=it;supported=true}
                .onFailure{supported=false}
            loading=false
        }
    }

    OutlinedButton(
        onClick={
            expanded=!expanded
            if(expanded)reload()
        },
        modifier=Modifier.fillMaxWidth()
    ){Text(rsV111Ui(lang,if(expanded)"hide_files" else "files"))}

    if(!expanded)return
    if(!supported){
        Text(rsV111Ui(lang,"backend"),color=c.muted,fontSize=9.sp)
        return
    }
    if(loading)LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
    if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)
    if(assets.isEmpty()&&!loading)Text(rsV111Ui(lang,"no_files"),color=c.muted,fontSize=9.sp)

    assets.forEach{asset->
        Surface(color=c.gold.copy(alpha=.07f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(asset.sourceArea+" · "+asset.mediaKind,color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
                Text(rsFileSizeV111(asset.byteSize)+" · "+asset.storagePath.substringAfterLast('/'),color=c.muted,fontSize=9.sp)
                OutlinedButton(
                    onClick={
                        if(pendingDelete==asset.id){
                            scope.launch{
                                rsDeleteStudentMediaAssetV111(asset)
                                    .onSuccess{status=rsV111Ui(lang,"deleted");pendingDelete=null;reload()}
                                    .onFailure{status=rsV111Ui(lang,"delete_failed")}
                            }
                        }else pendingDelete=asset.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsV111Ui(lang,if(pendingDelete==asset.id)"confirm" else "delete"),fontSize=9.sp)}
            }
        }
    }
}

@Composable
fun RsPrivateLessonPlanAssignmentV111(
    c:RsPalette,
    lang:RsLang,
    bookingId:String,
    onAssigned:()->Unit
){
    val scope=rememberCoroutineScope()
    var templates by remember{mutableStateOf<List<RsTrainingTemplateV110>>(emptyList())}
    var links by remember{mutableStateOf<List<RsPrivateLessonTrainingV111>>(emptyList())}
    var selectedTemplate by remember{mutableStateOf("")}
    var due by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var supported by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(bookingId){
        rsTrainingTemplatesV110().onSuccess{
            templates=it
            if(selectedTemplate.isBlank())selectedTemplate=it.firstOrNull()?.id.orEmpty()
        }
        rsPrivateLessonTrainingFeedV111()
            .onSuccess{links=it;supported=true}
            .onFailure{supported=false}
    }

    RsPanel(c){
        Text(rsV111Ui(lang,"plan"),color=c.bright,fontWeight=FontWeight.Black,fontSize=16.sp)
        Text(rsV111Ui(lang,"plan_help"),color=c.muted,fontSize=9.sp)
        val current=links.firstOrNull{it.bookingId==bookingId}
        if(current!=null){
            Text(rsV111Ui(lang,"current_plan")+" · "+current.templateTitle,color=c.bright,fontWeight=FontWeight.Bold)
        }
        if(!supported){
            Text(rsV111Ui(lang,"backend"),color=c.muted,fontSize=9.sp)
            return@RsPanel
        }
        templates.forEach{template->
            FilterChip(
                selected=selectedTemplate==template.id,
                onClick={selectedTemplate=template.id},
                label={Text(template.title)},
                modifier=Modifier.fillMaxWidth()
            )
        }
        OutlinedTextField(
            due,{due=it.take(40)},
            label={Text(rsV111Ui(lang,"due"))},
            modifier=Modifier.fillMaxWidth(),
            enabled=!busy
        )
        Button(
            onClick={
                busy=true
                scope.launch{
                    rsAssignPrivateLessonTrainingV111(bookingId,selectedTemplate,due)
                        .onSuccess{
                            status=rsV111Ui(lang,"assigned")
                            rsPrivateLessonTrainingFeedV111().onSuccess{links=it}
                            onAssigned()
                        }
                        .onFailure{status=it.message.orEmpty()}
                    busy=false
                }
            },
            enabled=!busy&&selectedTemplate.isNotBlank(),
            modifier=Modifier.fillMaxWidth()
        ){Text(rsV111Ui(lang,"assign"))}
        if(status.isNotBlank())Text(status,color=c.muted,fontSize=9.sp)
    }
}

@Composable
fun RsPrivateLessonPlanBadgeV111(c:RsPalette,lang:RsLang,bookingId:String){
    var link by remember(bookingId){mutableStateOf<RsPrivateLessonTrainingV111?>(null)}
    LaunchedEffect(bookingId){
        rsPrivateLessonTrainingFeedV111().onSuccess{rows->link=rows.firstOrNull{it.bookingId==bookingId}}
    }
    val current=link?:return
    Surface(color=c.gold.copy(alpha=.10f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(10.dp)){
            Text(rsV111Ui(lang,"current_plan"),color=c.muted,fontSize=9.sp)
            Text(current.templateTitle,color=c.bright,fontWeight=FontWeight.Bold)
        }
    }
}

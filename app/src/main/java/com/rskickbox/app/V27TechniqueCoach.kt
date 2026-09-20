package com.rskickbox.app

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RS_TECH_VIDEO_MAX_MS = 20_000L

private fun rsTechniqueVideoDirV36(context:android.content.Context):File =
    File(context.filesDir,"rs_technique_videos").apply{mkdirs()}

private fun rsCopyTechniqueVideoV36(context:android.content.Context,source:Uri):String{
    val mime=context.contentResolver.getType(source).orEmpty()
    val ext=when{
        mime.contains("webm",true)->"webm"
        mime.contains("quicktime",true)->"mov"
        mime.contains("3gpp",true)->"3gp"
        else->"mp4"
    }
    val out=File(rsTechniqueVideoDirV36(context),"tech_"+UUID.randomUUID()+"."+ext)
    context.contentResolver.openInputStream(source)!!.use{input->
        FileOutputStream(out).use{output->input.copyTo(output)}
    }
    return Uri.fromFile(out).toString()
}

private fun rsDeleteTechniqueVideoV36(context:android.content.Context,uriString:String){
    if(uriString.isBlank())return
    runCatching{
        val uri=Uri.parse(uriString)
        if(uri.scheme!="file")return
        val file=uri.path?.let(::File)?:return
        val root=rsTechniqueVideoDirV36(context).canonicalFile
        val target=file.canonicalFile
        if(target.parentFile==root && target.exists())target.delete()
    }
}

private data class TechniqueSubmissionV27(
    val id:String,val uri:String,val name:String,val technique:String,
    val created:String,val favorite:Boolean,val summary:String,val ownerEmail:String
)

private fun encodeTechniqueSubsV27(items:List<TechniqueSubmissionV27>)=items.joinToString("§"){
    listOf(it.id,it.uri,it.name,it.technique,it.created,it.favorite.toString(),it.summary.replace("¤"," "),it.ownerEmail).joinToString("¤")
}

private fun decodeTechniqueSubsV27(raw:String)=raw.split("§").mapNotNull{row->
    val p=row.split("¤",limit=8)
    if(p.size<7)null else TechniqueSubmissionV27(
        p[0],p[1],p[2],p[3],p[4],p[5].toBooleanStrictOrNull()?:false,p[6],p.getOrElse(7){""}
    )
}

private fun rsDeleteTechniqueVideoIfUnusedV36(context:android.content.Context,store:RsStore,uriString:String){
    if(uriString.isBlank())return
    val referenced=decodeTechniqueSubsV27(store.s("technique_submissions_v27","")).any{it.uri==uriString}
    if(!referenced)rsDeleteTechniqueVideoV36(context,uriString)
}

fun rsTechniqueHistoryRawForStudentV40(store:RsStore,email:String):String =
    encodeTechniqueSubsV27(
        decodeTechniqueSubsV27(store.s("technique_submissions_v27",""))
            .filter{it.ownerEmail.equals(email,true)}
    )

fun rsRemoveTechniqueHistoryForStudentV40(context:android.content.Context,store:RsStore,email:String){
    val all=decodeTechniqueSubsV27(store.s("technique_submissions_v27",""))
    val remove=all.filter{it.ownerEmail.equals(email,true)}
    val keep=all.filterNot{it.ownerEmail.equals(email,true)}
    store.ps("technique_submissions_v27",encodeTechniqueSubsV27(keep))
    remove.map{it.uri}.distinct().forEach{uri->
        if(keep.none{it.uri==uri})rsDeleteTechniqueVideoV36(context,uri)
    }
}

private fun videoDurationV27(context:android.content.Context,uri:Uri):Long=runCatching{
    val mmr=MediaMetadataRetriever()
    mmr.setDataSource(context,uri)
    val ms=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L
    mmr.release()
    ms
}.getOrDefault(0L)

private fun displayNameV27(context:android.content.Context,uri:Uri):String{
    var name="Technique video"
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())name=cur.getString(0)?:name
        }
    }
    return name
}

private fun coachSummaryV27(lang:RsLang):String=when(lang.code){
    "nl"->"Focus op balans, hoge dekking, gecontroleerde heuprotatie en een snelle terugkeer naar je basispositie. Werk eerst langzaam en technisch zuiver."
    "pt"->"Foca no equilíbrio, guarda alta, rotação controlada da anca e regresso rápido à posição base. Treina primeiro devagar e com boa técnica."
    "es"->"Concéntrate en el equilibrio, guardia alta, rotación controlada de cadera y regreso rápido a la posición base. Practica primero despacio y limpio."
    "fr"->"Travaille l'équilibre, une garde haute, une rotation contrôlée des hanches et un retour rapide en position. Commence lentement et proprement."
    "de"->"Achte auf Balance, hohe Deckung, kontrollierte Hüftrotation und eine schnelle Rückkehr in die Grundstellung. Übe zuerst langsam und sauber."
    "it"->"Concentrati su equilibrio, guardia alta, rotazione controllata dell'anca e rapido ritorno alla posizione base. Lavora prima lentamente e pulito."
    "pl"->"Skup się na równowadze, wysokiej gardzie, kontrolowanej rotacji bioder i szybkim powrocie do pozycji bazowej. Najpierw ćwicz wolno i czysto."
    "tr"->"Dengeye, yüksek garda, kontrollü kalça dönüşüne ve temel duruşa hızlı dönüşe odaklan. Önce yavaş ve temiz çalış."
    else->"Focus on balance, a high guard, controlled hip rotation, and a quick return to your base stance. Train slowly and cleanly before adding speed."
}

private fun techniquePointsV27(lang:RsLang):List<Pair<String,String>> = when(lang.code){
    "nl"->listOf("Basis & balans" to "Voeten stabiel, knieën zacht en gewicht gecentreerd.","Dekking" to "Handen keren direct terug naar het gezicht.","Rotatie" to "Draai heup en schouder samen zonder over te draaien.","Herstel" to "Kom na de techniek direct terug in een sterke positie.")
    "pt"->listOf("Base & equilíbrio" to "Pés estáveis, joelhos soltos e peso centrado.","Guarda" to "As mãos voltam imediatamente ao rosto.","Rotação" to "Anca e ombro rodam juntos sem exagerar.","Recuperação" to "Volta logo a uma posição forte depois da técnica.")
    "es"->listOf("Base & equilibrio" to "Pies estables, rodillas sueltas y peso centrado.","Guardia" to "Las manos vuelven inmediatamente al rostro.","Rotación" to "Cadera y hombro giran juntos sin exceso.","Recuperación" to "Vuelve rápido a una posición fuerte.")
    "fr"->listOf("Base & équilibre" to "Pieds stables, genoux souples et poids centré.","Garde" to "Les mains reviennent immédiatement au visage.","Rotation" to "Hanche et épaule tournent ensemble sans excès.","Retour" to "Reviens vite dans une position forte.")
    "de"->listOf("Basis & Balance" to "Stabile Füße, weiche Knie und zentriertes Gewicht.","Deckung" to "Die Hände kehren nach jedem Schlag sofort zum Gesicht zurück.","Rotation" to "Hüfte und Schulter drehen gemeinsam ohne Überrotation.","Rückkehr" to "Nach der Technik schnell in eine starke Grundstellung zurückkehren.")
    "it"->listOf("Base & equilibrio" to "Piedi stabili, ginocchia morbide e peso centrato.","Guardia" to "Le mani tornano subito al viso dopo ogni colpo.","Rotazione" to "Anca e spalla ruotano insieme senza esagerare.","Recupero" to "Ritorna rapidamente in una posizione forte dopo la tecnica.")
    "pl"->listOf("Baza i równowaga" to "Stabilne stopy, miękkie kolana i wycentrowany ciężar.","Garda" to "Dłonie natychmiast wracają do twarzy po każdym uderzeniu.","Rotacja" to "Biodro i bark obracają się razem bez nadmiernego skrętu.","Powrót" to "Po technice szybko wróć do mocnej pozycji.")
    "tr"->listOf("Temel & denge" to "Ayaklar sağlam, dizler rahat ve ağırlık merkezde.","Gard" to "Her vuruştan sonra eller hemen yüze geri döner.","Dönüş" to "Kalça ve omuz aşırı dönmeden birlikte döner.","Toparlanma" to "Teknikten sonra hızlıca güçlü temel duruşa dön.")
    else->listOf("Base & balance" to "Stable feet, soft knees, and centered weight.","Guard" to "Hands return immediately to the face after each strike.","Rotation" to "Turn hip and shoulder together without over-rotating.","Recovery" to "Return quickly to a strong stance after the technique.")
}

private fun coachUiV36(lang:RsLang,key:String):String{
    val en=mapOf("history_title" to "TECHNIQUE REVIEW HISTORY","history_sub" to "Student technique uploads, saved analyses and trainer favorites.","none" to "No technique uploads yet","none_sub" to "Saved student technique submissions will appear here.","favorite" to "Favorite","unfavorite" to "Unfavorite","delete" to "Delete","confirm" to "Confirm","deleted" to "Technique submission deleted.","confirm_delete" to "Tap Delete again to confirm this technique submission.","fav_saved" to "Saved to trainer favorites.","fav_removed" to "Removed from favorites.")
    val nl=en+mapOf("history_title" to "TECHNIEK REVIEWGESCHIEDENIS","history_sub" to "Techniekvideo's van leerlingen, opgeslagen analyses en trainerfavorieten.","none" to "Nog geen techniekuploads","none_sub" to "Opgeslagen inzendingen verschijnen hier.","favorite" to "Favoriet","unfavorite" to "Uit favorieten","delete" to "Verwijderen","confirm" to "Bevestigen","deleted" to "Techniekinzending verwijderd.","confirm_delete" to "Tik nogmaals op Verwijderen om deze inzending te bevestigen.","fav_saved" to "Opgeslagen bij trainerfavorieten.","fav_removed" to "Uit favorieten verwijderd.")
    val pt=en+mapOf("history_title" to "HISTÓRICO DE REVISÃO TÉCNICA","history_sub" to "Vídeos técnicos dos alunos, análises guardadas e favoritos do treinador.","none" to "Ainda não há vídeos técnicos","none_sub" to "As submissões guardadas aparecerão aqui.","favorite" to "Favorito","unfavorite" to "Retirar favorito","delete" to "Eliminar","confirm" to "Confirmar","deleted" to "Submissão técnica eliminada.","confirm_delete" to "Toca novamente em Eliminar para confirmar esta submissão.","fav_saved" to "Guardado nos favoritos do treinador.","fav_removed" to "Removido dos favoritos.")
    val es=en+mapOf("history_title" to "HISTORIAL DE REVISIÓN TÉCNICA","history_sub" to "Vídeos técnicos de alumnos, análisis guardados y favoritos del entrenador.","none" to "Aún no hay vídeos técnicos","none_sub" to "Las entregas guardadas aparecerán aquí.","favorite" to "Favorito","unfavorite" to "Quitar favorito","delete" to "Eliminar","confirm" to "Confirmar","deleted" to "Entrega técnica eliminada.","confirm_delete" to "Pulsa Eliminar otra vez para confirmar esta entrega.","fav_saved" to "Guardado en favoritos del entrenador.","fav_removed" to "Eliminado de favoritos.")
    val fr=en+mapOf("history_title" to "HISTORIQUE DE REVUE TECHNIQUE","history_sub" to "Vidéos techniques élèves, analyses enregistrées et favoris entraîneur.","none" to "Aucune vidéo technique","none_sub" to "Les soumissions enregistrées apparaîtront ici.","favorite" to "Favori","unfavorite" to "Retirer favori","delete" to "Supprimer","confirm" to "Confirmer","deleted" to "Soumission technique supprimée.","confirm_delete" to "Appuie encore sur Supprimer pour confirmer cette soumission.","fav_saved" to "Ajouté aux favoris entraîneur.","fav_removed" to "Retiré des favoris.")
    val de=en+mapOf("history_title" to "TECHNIK-REVIEW-VERLAUF","history_sub" to "Technikvideos der Schüler, gespeicherte Analysen und Trainer-Favoriten.","none" to "Noch keine Technikvideos","none_sub" to "Gespeicherte Einsendungen erscheinen hier.","favorite" to "Favorit","unfavorite" to "Favorit entfernen","delete" to "Löschen","confirm" to "Bestätigen","deleted" to "Technik-Einsendung gelöscht.","confirm_delete" to "Tippe erneut auf Löschen, um diese Einsendung zu bestätigen.","fav_saved" to "In Trainer-Favoriten gespeichert.","fav_removed" to "Aus Favoriten entfernt.")
    val it=en+mapOf("history_title" to "STORICO REVISIONE TECNICA","history_sub" to "Video tecnici degli allievi, analisi salvate e preferiti dell'allenatore.","none" to "Nessun video tecnico","none_sub" to "Le submission salvate appariranno qui.","favorite" to "Preferito","unfavorite" to "Rimuovi preferito","delete" to "Elimina","confirm" to "Conferma","deleted" to "Submission tecnica eliminata.","confirm_delete" to "Tocca di nuovo Elimina per confermare questa submission.","fav_saved" to "Salvato nei preferiti allenatore.","fav_removed" to "Rimosso dai preferiti.")
    val pl=en+mapOf("history_title" to "HISTORIA OCENY TECHNIKI","history_sub" to "Filmy techniczne uczniów, zapisane analizy i ulubione trenera.","none" to "Brak filmów technicznych","none_sub" to "Zapisane zgłoszenia pojawią się tutaj.","favorite" to "Ulubione","unfavorite" to "Usuń z ulubionych","delete" to "Usuń","confirm" to "Potwierdź","deleted" to "Zgłoszenie techniczne usunięte.","confirm_delete" to "Naciśnij Usuń ponownie, aby potwierdzić to zgłoszenie.","fav_saved" to "Zapisano w ulubionych trenera.","fav_removed" to "Usunięto z ulubionych.")
    val tr=en+mapOf("history_title" to "TEKNİK İNCELEME GEÇMİŞİ","history_sub" to "Öğrenci teknik videoları, kaydedilen analizler ve antrenör favorileri.","none" to "Henüz teknik videosu yok","none_sub" to "Kaydedilen gönderimler burada görünecek.","favorite" to "Favori","unfavorite" to "Favoriden çıkar","delete" to "Sil","confirm" to "Onayla","deleted" to "Teknik gönderim silindi.","confirm_delete" to "Bu gönderimi onaylamak için Sil'e tekrar dokun.","fav_saved" to "Antrenör favorilerine kaydedildi.","fav_removed" to "Favorilerden çıkarıldı.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}


private fun coachStudentUiV105(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "AI TECHNIQUE COACH",
        "sub" to "Upload a short kickboxing move or combination. Get text, visual and spoken coaching in your app language.",
        "trainer" to "YOUR AI TRAINER","male" to "Male trainer","female" to "Female trainer",
        "trainer_desc" to "The selected trainer demonstrates correction focus visually and speaks in the active app language.",
        "step1" to "1 · VIDEO & TECHNIQUE","files" to "Files","gallery" to "Gallery","replace" to "Replace","importing" to "Importing…",
        "picker_note" to "Gallery opens first for phone videos · Files is a fallback · maximum 20 seconds",
        "read_fail" to "Could not read this video file.","too_long" to "Video is too long. Technique uploads are limited to 20 seconds.",
        "import_private" to "Importing video into private RS KICKBOXING storage…","import_fail" to "Could not import this video into private app storage.",
        "ready" to "Video ready for local preview.","preview_generated" to "Structured coaching preview generated.",
        "analyze" to "Analyze technique","remove" to "Remove video","step2" to "2 · COACHING RESULT",
        "preview_mode" to "PREVIEW MODE · Frame-by-frame AI vision is not connected yet.",
        "speak" to "🔊 Speak coaching","saving_cloud" to "Saving technique review to secure cloud history…",
        "saved_cloud" to "✓ Analysis saved to cloud technique history.","saved_local" to "Analysis saved to technique history.",
        "save_history" to "Save to history","step3" to "3 · ASK THE COACH",
        "voice_desc" to "Voice is an extra input/output layer for the technique coach.",
        "ask_label" to "Ask about this movement","ask_voice" to "🎙 Ask by voice","auto_speak" to "Speak replies automatically","ask_ai" to "Ask AI Coach",
        "focus" to "Demonstrating focus",
        "history_cloud_sub" to "Cloud student technique uploads, trainer notes and favorites.",
        "syncing" to "Syncing technique submissions…","history_connected" to "Secure cloud technique history connected",
        "back_history" to "Back to technique history","loading_video" to "Loading video…","student_summary" to "Student coaching summary",
        "open_video" to "Open technique video","trainer_note" to "Trainer note"
    )
    val nl=en+mapOf(
        "title" to "AI TECHNIEKCOACH","sub" to "Upload een korte kickboksbeweging of combinatie. Ontvang tekstuele, visuele en gesproken coaching in je app-taal.",
        "trainer" to "JOUW AI-TRAINER","male" to "Mannelijke trainer","female" to "Vrouwelijke trainer","trainer_desc" to "De gekozen trainer laat visueel zien waar de correctiefocus ligt en spreekt in de actieve app-taal.",
        "step1" to "1 · VIDEO & TECHNIEK","files" to "Bestanden","gallery" to "Galerij","replace" to "Vervangen","importing" to "Importeren…",
        "picker_note" to "Galerij opent eerst voor telefoonvideo’s · Bestanden is de reserveoptie · maximaal 20 seconden",
        "read_fail" to "Dit videobestand kon niet worden gelezen.","too_long" to "De video is te lang. Techniekuploads zijn beperkt tot 20 seconden.",
        "import_private" to "Video importeren naar privéopslag van RS KICKBOXING…","import_fail" to "De video kon niet naar de privéopslag van de app worden geïmporteerd.",
        "ready" to "Video klaar voor lokale preview.","preview_generated" to "Gestructureerde coachingpreview aangemaakt.","analyze" to "Techniek analyseren","remove" to "Video verwijderen",
        "step2" to "2 · COACHINGRESULTAAT","preview_mode" to "PREVIEWMODUS · Frame-voor-frame AI-visie is nog niet verbonden.","speak" to "🔊 Coaching uitspreken",
        "saving_cloud" to "Techniekreview opslaan in beveiligde cloudgeschiedenis…","saved_cloud" to "✓ Analyse opgeslagen in cloudgeschiedenis.","saved_local" to "Analyse opgeslagen in techniekgeschiedenis.","save_history" to "Opslaan in geschiedenis",
        "step3" to "3 · VRAAG DE COACH","voice_desc" to "Spraak is een extra invoer- en uitvoerlaag voor de techniekcoach.","ask_label" to "Vraag iets over deze beweging","ask_voice" to "🎙 Vraag met stem","auto_speak" to "Antwoorden automatisch uitspreken","ask_ai" to "Vraag AI Coach",
        "focus" to "Correctiefocus","history_cloud_sub" to "Clouduploads van leerlingtechniek, trainernotities en favorieten.","syncing" to "Techniekinzendingen synchroniseren…","history_connected" to "Beveiligde cloudgeschiedenis verbonden","back_history" to "Terug naar techniekgeschiedenis","loading_video" to "Video laden…","student_summary" to "Coachingoverzicht leerling","open_video" to "Techniekvideo openen","trainer_note" to "Trainernotitie"
    )
    val pt=en+mapOf(
        "title" to "COACH TÉCNICO IA","sub" to "Envia um movimento ou combinação curta de kickboxing. Recebe coaching em texto, visual e voz no idioma da app.",
        "trainer" to "O TEU TREINADOR IA","male" to "Treinador masculino","female" to "Treinadora feminina","trainer_desc" to "O treinador selecionado mostra visualmente o foco da correção e fala no idioma ativo da app.",
        "step1" to "1 · VÍDEO & TÉCNICA","files" to "Ficheiros","gallery" to "Galeria","replace" to "Substituir","importing" to "A importar…","picker_note" to "A Galeria abre primeiro · Ficheiros é alternativa · máximo 20 segundos",
        "read_fail" to "Não foi possível ler este vídeo.","too_long" to "O vídeo é demasiado longo. O limite é 20 segundos.","import_private" to "A importar vídeo para armazenamento privado RS KICKBOXING…","import_fail" to "Não foi possível importar o vídeo para o armazenamento privado.","ready" to "Vídeo pronto para pré-visualização.","preview_generated" to "Pré-visualização de coaching criada.","analyze" to "Analisar técnica","remove" to "Remover vídeo",
        "step2" to "2 · RESULTADO DO COACHING","preview_mode" to "MODO PREVIEW · A visão IA frame a frame ainda não está ligada.","speak" to "🔊 Ouvir coaching","saving_cloud" to "A guardar revisão técnica no histórico cloud seguro…","saved_cloud" to "✓ Análise guardada no histórico cloud.","saved_local" to "Análise guardada no histórico técnico.","save_history" to "Guardar no histórico",
        "step3" to "3 · PERGUNTAR AO COACH","voice_desc" to "A voz é uma camada extra de entrada e saída do coach técnico.","ask_label" to "Pergunta sobre este movimento","ask_voice" to "🎙 Perguntar por voz","auto_speak" to "Ler respostas automaticamente","ask_ai" to "Perguntar ao Coach IA",
        "focus" to "Foco da correção","history_cloud_sub" to "Uploads técnicos dos alunos, notas do treinador e favoritos na cloud.","syncing" to "A sincronizar submissões técnicas…","history_connected" to "Histórico técnico cloud seguro ligado","back_history" to "Voltar ao histórico técnico","loading_video" to "A carregar vídeo…","student_summary" to "Resumo de coaching do aluno","open_video" to "Abrir vídeo técnico","trainer_note" to "Nota do treinador"
    )
    val es=en+mapOf(
        "title" to "COACH TÉCNICO IA","sub" to "Sube un movimiento o combinación corta de kickboxing. Recibe coaching de texto, visual y voz en el idioma de la app.",
        "trainer" to "TU ENTRENADOR IA","male" to "Entrenador masculino","female" to "Entrenadora femenina","trainer_desc" to "El entrenador seleccionado muestra visualmente el foco de corrección y habla en el idioma activo de la app.",
        "step1" to "1 · VÍDEO Y TÉCNICA","files" to "Archivos","gallery" to "Galería","replace" to "Reemplazar","importing" to "Importando…","picker_note" to "La Galería se abre primero · Archivos es alternativa · máximo 20 segundos",
        "read_fail" to "No se pudo leer este vídeo.","too_long" to "El vídeo es demasiado largo. El límite es 20 segundos.","import_private" to "Importando vídeo al almacenamiento privado de RS KICKBOXING…","import_fail" to "No se pudo importar el vídeo al almacenamiento privado.","ready" to "Vídeo listo para vista previa.","preview_generated" to "Vista previa de coaching generada.","analyze" to "Analizar técnica","remove" to "Eliminar vídeo",
        "step2" to "2 · RESULTADO DEL COACHING","preview_mode" to "MODO PREVIEW · La visión IA fotograma a fotograma aún no está conectada.","speak" to "🔊 Escuchar coaching","saving_cloud" to "Guardando revisión técnica en historial cloud seguro…","saved_cloud" to "✓ Análisis guardado en historial cloud.","saved_local" to "Análisis guardado en historial técnico.","save_history" to "Guardar en historial",
        "step3" to "3 · PREGUNTAR AL COACH","voice_desc" to "La voz es una capa adicional de entrada y salida para el coach técnico.","ask_label" to "Pregunta sobre este movimiento","ask_voice" to "🎙 Preguntar por voz","auto_speak" to "Leer respuestas automáticamente","ask_ai" to "Preguntar al Coach IA",
        "focus" to "Foco de corrección","history_cloud_sub" to "Vídeos técnicos de alumnos, notas del entrenador y favoritos en cloud.","syncing" to "Sincronizando entregas técnicas…","history_connected" to "Historial técnico cloud seguro conectado","back_history" to "Volver al historial técnico","loading_video" to "Cargando vídeo…","student_summary" to "Resumen de coaching del alumno","open_video" to "Abrir vídeo técnico","trainer_note" to "Nota del entrenador"
    )
    val fr=en+mapOf(
        "title" to "COACH TECHNIQUE IA","sub" to "Envoie un mouvement ou une combinaison courte de kickboxing. Reçois un coaching texte, visuel et vocal dans la langue de l’app.",
        "trainer" to "TON ENTRAÎNEUR IA","male" to "Entraîneur homme","female" to "Entraîneuse","trainer_desc" to "L’entraîneur choisi montre visuellement le point de correction et parle dans la langue active de l’app.",
        "step1" to "1 · VIDÉO & TECHNIQUE","files" to "Fichiers","gallery" to "Galerie","replace" to "Remplacer","importing" to "Importation…","picker_note" to "La Galerie s’ouvre d’abord · Fichiers en alternative · maximum 20 secondes",
        "read_fail" to "Impossible de lire cette vidéo.","too_long" to "La vidéo est trop longue. Limite : 20 secondes.","import_private" to "Importation vers le stockage privé RS KICKBOXING…","import_fail" to "Impossible d’importer la vidéo dans le stockage privé.","ready" to "Vidéo prête pour l’aperçu.","preview_generated" to "Aperçu de coaching généré.","analyze" to "Analyser la technique","remove" to "Supprimer la vidéo",
        "step2" to "2 · RÉSULTAT DU COACHING","preview_mode" to "MODE APERÇU · La vision IA image par image n’est pas encore connectée.","speak" to "🔊 Écouter le coaching","saving_cloud" to "Enregistrement de la revue technique dans l’historique cloud sécurisé…","saved_cloud" to "✓ Analyse enregistrée dans l’historique cloud.","saved_local" to "Analyse enregistrée dans l’historique technique.","save_history" to "Enregistrer dans l’historique",
        "step3" to "3 · DEMANDER AU COACH","voice_desc" to "La voix est une couche d’entrée/sortie supplémentaire du coach technique.","ask_label" to "Pose une question sur ce mouvement","ask_voice" to "🎙 Demander par la voix","auto_speak" to "Lire les réponses automatiquement","ask_ai" to "Demander au Coach IA",
        "focus" to "Point de correction","history_cloud_sub" to "Vidéos techniques élèves, notes entraîneur et favoris cloud.","syncing" to "Synchronisation des vidéos techniques…","history_connected" to "Historique technique cloud sécurisé connecté","back_history" to "Retour à l’historique technique","loading_video" to "Chargement vidéo…","student_summary" to "Résumé coaching élève","open_video" to "Ouvrir la vidéo technique","trainer_note" to "Note entraîneur"
    )
    val de=en+mapOf(
        "title" to "KI-TECHNIKCOACH","sub" to "Lade eine kurze Kickbox-Bewegung oder Kombination hoch. Erhalte Text-, visuelles und gesprochenes Coaching in deiner App-Sprache.",
        "trainer" to "DEIN KI-TRAINER","male" to "Männlicher Trainer","female" to "Weibliche Trainerin","trainer_desc" to "Der ausgewählte Trainer zeigt den Korrekturfokus visuell und spricht in der aktiven App-Sprache.",
        "step1" to "1 · VIDEO & TECHNIK","files" to "Dateien","gallery" to "Galerie","replace" to "Ersetzen","importing" to "Importieren…","picker_note" to "Galerie öffnet zuerst · Dateien als Alternative · maximal 20 Sekunden",
        "read_fail" to "Diese Videodatei konnte nicht gelesen werden.","too_long" to "Das Video ist zu lang. Maximal 20 Sekunden.","import_private" to "Video wird in privaten RS KICKBOXING-Speicher importiert…","import_fail" to "Video konnte nicht in den privaten App-Speicher importiert werden.","ready" to "Video bereit zur Vorschau.","preview_generated" to "Strukturierte Coaching-Vorschau erstellt.","analyze" to "Technik analysieren","remove" to "Video entfernen",
        "step2" to "2 · COACHING-ERGEBNIS","preview_mode" to "VORSCHAUMODUS · Frame-für-Frame-KI-Vision ist noch nicht verbunden.","speak" to "🔊 Coaching vorlesen","saving_cloud" to "Technikreview wird sicher in der Cloud gespeichert…","saved_cloud" to "✓ Analyse in Cloud-Verlauf gespeichert.","saved_local" to "Analyse im Technikverlauf gespeichert.","save_history" to "Im Verlauf speichern",
        "step3" to "3 · COACH FRAGEN","voice_desc" to "Sprache ist eine zusätzliche Ein-/Ausgabeschicht für den Technikcoach.","ask_label" to "Frage zu dieser Bewegung","ask_voice" to "🎙 Per Sprache fragen","auto_speak" to "Antworten automatisch vorlesen","ask_ai" to "KI-Coach fragen",
        "focus" to "Korrekturfokus","history_cloud_sub" to "Cloud-Technikvideos der Schüler, Trainernotizen und Favoriten.","syncing" to "Technikeinsendungen werden synchronisiert…","history_connected" to "Sicherer Cloud-Technikverlauf verbunden","back_history" to "Zurück zum Technikverlauf","loading_video" to "Video wird geladen…","student_summary" to "Coaching-Zusammenfassung","open_video" to "Technikvideo öffnen","trainer_note" to "Trainernotiz"
    )
    val it=en+mapOf(
        "title" to "COACH TECNICO IA","sub" to "Carica un breve movimento o combinazione di kickboxing. Ricevi coaching testuale, visivo e vocale nella lingua dell’app.",
        "trainer" to "IL TUO TRAINER IA","male" to "Trainer uomo","female" to "Trainer donna","trainer_desc" to "Il trainer selezionato mostra visivamente il focus della correzione e parla nella lingua attiva dell’app.",
        "step1" to "1 · VIDEO & TECNICA","files" to "File","gallery" to "Galleria","replace" to "Sostituisci","importing" to "Importazione…","picker_note" to "Si apre prima la Galleria · File come alternativa · massimo 20 secondi",
        "read_fail" to "Impossibile leggere questo video.","too_long" to "Il video è troppo lungo. Limite: 20 secondi.","import_private" to "Importazione nel deposito privato RS KICKBOXING…","import_fail" to "Impossibile importare il video nel deposito privato.","ready" to "Video pronto per l’anteprima.","preview_generated" to "Anteprima coaching generata.","analyze" to "Analizza tecnica","remove" to "Rimuovi video",
        "step2" to "2 · RISULTATO COACHING","preview_mode" to "MODALITÀ PREVIEW · La visione IA frame per frame non è ancora collegata.","speak" to "🔊 Ascolta coaching","saving_cloud" to "Salvataggio revisione tecnica nello storico cloud sicuro…","saved_cloud" to "✓ Analisi salvata nello storico cloud.","saved_local" to "Analisi salvata nello storico tecnico.","save_history" to "Salva nello storico",
        "step3" to "3 · CHIEDI AL COACH","voice_desc" to "La voce è un livello aggiuntivo di input/output del coach tecnico.","ask_label" to "Chiedi di questo movimento","ask_voice" to "🎙 Chiedi a voce","auto_speak" to "Leggi automaticamente le risposte","ask_ai" to "Chiedi al Coach IA",
        "focus" to "Focus correzione","history_cloud_sub" to "Video tecnici allievi, note trainer e preferiti in cloud.","syncing" to "Sincronizzazione invii tecnici…","history_connected" to "Storico tecnico cloud sicuro collegato","back_history" to "Torna allo storico tecnico","loading_video" to "Caricamento video…","student_summary" to "Riepilogo coaching allievo","open_video" to "Apri video tecnico","trainer_note" to "Nota trainer"
    )
    val pl=en+mapOf(
        "title" to "TRENER TECHNIKI AI","sub" to "Prześlij krótki ruch lub kombinację kickboxingu. Otrzymaj coaching tekstowy, wizualny i głosowy w języku aplikacji.",
        "trainer" to "TWÓJ TRENER AI","male" to "Trener","female" to "Trenerka","trainer_desc" to "Wybrany trener pokazuje wizualnie obszar korekty i mówi w aktywnym języku aplikacji.",
        "step1" to "1 · WIDEO I TECHNIKA","files" to "Pliki","gallery" to "Galeria","replace" to "Zastąp","importing" to "Importowanie…","picker_note" to "Najpierw otwiera się Galeria · Pliki jako alternatywa · maksymalnie 20 sekund",
        "read_fail" to "Nie udało się odczytać filmu.","too_long" to "Film jest za długi. Maksymalnie 20 sekund.","import_private" to "Importowanie filmu do prywatnej pamięci RS KICKBOXING…","import_fail" to "Nie udało się zaimportować filmu do prywatnej pamięci.","ready" to "Film gotowy do podglądu.","preview_generated" to "Utworzono podgląd coachingu.","analyze" to "Analizuj technikę","remove" to "Usuń film",
        "step2" to "2 · WYNIK COACHINGU","preview_mode" to "TRYB PODGLĄDU · Analiza AI klatka po klatce nie jest jeszcze połączona.","speak" to "🔊 Odtwórz coaching","saving_cloud" to "Zapisywanie oceny techniki w bezpiecznej chmurze…","saved_cloud" to "✓ Analiza zapisana w chmurze.","saved_local" to "Analiza zapisana w historii techniki.","save_history" to "Zapisz w historii",
        "step3" to "3 · ZAPYTAJ TRENERA","voice_desc" to "Głos jest dodatkową warstwą wejścia/wyjścia trenera techniki.","ask_label" to "Zapytaj o ten ruch","ask_voice" to "🎙 Zapytaj głosowo","auto_speak" to "Czytaj odpowiedzi automatycznie","ask_ai" to "Zapytaj Trenera AI",
        "focus" to "Obszar korekty","history_cloud_sub" to "Filmy techniczne uczniów, notatki trenera i ulubione w chmurze.","syncing" to "Synchronizacja zgłoszeń technicznych…","history_connected" to "Bezpieczna historia techniki w chmurze połączona","back_history" to "Wróć do historii techniki","loading_video" to "Ładowanie filmu…","student_summary" to "Podsumowanie coachingu ucznia","open_video" to "Otwórz film techniczny","trainer_note" to "Notatka trenera"
    )
    val tr=en+mapOf(
        "title" to "YAPAY ZEKA TEKNİK KOÇU","sub" to "Kısa bir kickboks hareketi veya kombinasyonu yükle. Uygulama dilinde metin, görsel ve sesli koçluk al.",
        "trainer" to "YAPAY ZEKA ANTRENÖRÜN","male" to "Erkek antrenör","female" to "Kadın antrenör","trainer_desc" to "Seçilen antrenör düzeltme odağını görsel olarak gösterir ve aktif uygulama dilinde konuşur.",
        "step1" to "1 · VİDEO & TEKNİK","files" to "Dosyalar","gallery" to "Galeri","replace" to "Değiştir","importing" to "İçe aktarılıyor…","picker_note" to "Önce Galeri açılır · Dosyalar alternatif · en fazla 20 saniye",
        "read_fail" to "Bu video okunamadı.","too_long" to "Video çok uzun. En fazla 20 saniye.","import_private" to "Video özel RS KICKBOXING depolamasına aktarılıyor…","import_fail" to "Video özel uygulama depolamasına aktarılamadı.","ready" to "Video önizlemeye hazır.","preview_generated" to "Yapılandırılmış koçluk önizlemesi oluşturuldu.","analyze" to "Tekniği analiz et","remove" to "Videoyu kaldır",
        "step2" to "2 · KOÇLUK SONUCU","preview_mode" to "ÖNİZLEME MODU · Kare kare yapay zeka görüntü analizi henüz bağlı değil.","speak" to "🔊 Koçluğu seslendir","saving_cloud" to "Teknik inceleme güvenli bulut geçmişine kaydediliyor…","saved_cloud" to "✓ Analiz bulut geçmişine kaydedildi.","saved_local" to "Analiz teknik geçmişine kaydedildi.","save_history" to "Geçmişe kaydet",
        "step3" to "3 · KOÇA SOR","voice_desc" to "Ses, teknik koçu için ek bir giriş/çıkış katmanıdır.","ask_label" to "Bu hareket hakkında sor","ask_voice" to "🎙 Sesle sor","auto_speak" to "Yanıtları otomatik seslendir","ask_ai" to "Yapay Zeka Koçuna Sor",
        "focus" to "Düzeltme odağı","history_cloud_sub" to "Öğrenci teknik videoları, antrenör notları ve bulut favorileri.","syncing" to "Teknik gönderimler eşitleniyor…","history_connected" to "Güvenli bulut teknik geçmişi bağlı","back_history" to "Teknik geçmişine dön","loading_video" to "Video yükleniyor…","student_summary" to "Öğrenci koçluk özeti","open_video" to "Teknik videoyu aç","trainer_note" to "Antrenör notu"
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

private fun coachAiStatusV106(lang:RsLang,key:String):String{
    val packs=mapOf(
        "en" to mapOf("analyzing" to "Analyzing sampled video frames with AI…","active" to "AI VISION ACTIVE · Review generated from sampled video frames.","fallback" to "AI vision is unavailable right now. Using the safe structured coaching preview."),
        "nl" to mapOf("analyzing" to "Geselecteerde videoframes analyseren met AI…","active" to "AI-VISIE ACTIEF · Review gemaakt uit geselecteerde videoframes.","fallback" to "AI-visie is nu niet beschikbaar. De veilige gestructureerde coachingpreview wordt gebruikt."),
        "pt" to mapOf("analyzing" to "A analisar frames do vídeo com IA…","active" to "VISÃO IA ATIVA · Revisão criada a partir de frames do vídeo.","fallback" to "A visão IA não está disponível agora. Será usada a pré-visualização segura de coaching."),
        "es" to mapOf("analyzing" to "Analizando fotogramas del vídeo con IA…","active" to "VISIÓN IA ACTIVA · Revisión generada a partir de fotogramas del vídeo.","fallback" to "La visión IA no está disponible ahora. Se usará la vista previa segura de coaching."),
        "fr" to mapOf("analyzing" to "Analyse d’images vidéo avec l’IA…","active" to "VISION IA ACTIVE · Revue générée à partir d’images vidéo échantillonnées.","fallback" to "La vision IA est indisponible pour le moment. L’aperçu de coaching sécurisé sera utilisé."),
        "de" to mapOf("analyzing" to "Video-Frames werden mit KI analysiert…","active" to "KI-VISION AKTIV · Review aus ausgewählten Video-Frames erstellt.","fallback" to "KI-Vision ist momentan nicht verfügbar. Die sichere strukturierte Coaching-Vorschau wird verwendet."),
        "it" to mapOf("analyzing" to "Analisi dei frame video con IA…","active" to "VISIONE IA ATTIVA · Revisione generata da frame video campionati.","fallback" to "La visione IA non è disponibile ora. Verrà usata l’anteprima sicura di coaching."),
        "pl" to mapOf("analyzing" to "Analiza klatek wideo przez AI…","active" to "WIZJA AI AKTYWNA · Ocena utworzona z próbkowanych klatek wideo.","fallback" to "Wizja AI jest teraz niedostępna. Zostanie użyty bezpieczny podgląd coachingu."),
        "tr" to mapOf("analyzing" to "Video kareleri yapay zeka ile analiz ediliyor…","active" to "YAPAY ZEKA GÖRÜŞÜ AKTİF · İnceleme örnek video karelerinden oluşturuldu.","fallback" to "Yapay zeka görüntü analizi şu anda kullanılamıyor. Güvenli yapılandırılmış koçluk önizlemesi kullanılacak.")
    )
    val pack=packs[lang.code]?:packs["en"]!!
    return pack[key]?:packs["en"]!![key]?:key
}


private fun coachAiReferenceUiV114(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "TRAINER-APPROVED CORRECTION EXAMPLES",
        "sub" to "Matched from Training Media using the technique and the AI review.",
        "open" to "Open correction example",
        "loading" to "Loading protected trainer example…"
    )
    val packs=mapOf(
        "nl" to mapOf("title" to "DOOR TRAINER GOEDGEKEURDE CORRECTIEVOORBEELDEN","sub" to "Gekoppeld vanuit Trainingsmedia op basis van techniek en AI-review.","open" to "Correctievoorbeeld openen","loading" to "Beveiligd trainervoorbeeld laden…"),
        "pt" to mapOf("title" to "EXEMPLOS DE CORREÇÃO APROVADOS PELO TREINADOR","sub" to "Correspondência feita a partir da Media de Treino usando a técnica e a análise IA.","open" to "Abrir exemplo de correção","loading" to "A carregar exemplo protegido…"),
        "es" to mapOf("title" to "EJEMPLOS DE CORRECCIÓN APROBADOS POR EL ENTRENADOR","sub" to "Coincidencias de Media de Entrenamiento según la técnica y el análisis IA.","open" to "Abrir ejemplo de corrección","loading" to "Cargando ejemplo protegido…"),
        "fr" to mapOf("title" to "EXEMPLES DE CORRECTION APPROUVÉS PAR L’ENTRAÎNEUR","sub" to "Correspondances depuis les Médias d’Entraînement selon la technique et l’analyse IA.","open" to "Ouvrir l’exemple de correction","loading" to "Chargement de l’exemple protégé…"),
        "de" to mapOf("title" to "VOM TRAINER FREIGEGEBENE KORREKTURBEISPIELE","sub" to "Aus Trainingsmedien anhand von Technik und KI-Auswertung abgeglichen.","open" to "Korrekturbeispiel öffnen","loading" to "Geschütztes Trainerbeispiel wird geladen…"),
        "it" to mapOf("title" to "ESEMPI DI CORREZIONE APPROVATI DAL TRAINER","sub" to "Abbinati dai Media di Allenamento usando tecnica e analisi IA.","open" to "Apri esempio di correzione","loading" to "Caricamento esempio protetto…"),
        "pl" to mapOf("title" to "PRZYKŁADY KOREKTY ZATWIERDZONE PRZEZ TRENERA","sub" to "Dopasowane z Mediów Treningowych na podstawie techniki i analizy AI.","open" to "Otwórz przykład korekty","loading" to "Ładowanie chronionego przykładu…"),
        "tr" to mapOf("title" to "ANTRENÖR ONAYLI DÜZELTME ÖRNEKLERİ","sub" to "Teknik ve yapay zekâ incelemesine göre Antrenman Medyasından eşleştirildi.","open" to "Düzeltme örneğini aç","loading" to "Korumalı antrenör örneği yükleniyor…")
    )
    return packs[lang.code]?.get(key)?:en[key]?:key
}

@Composable
fun RsTechniqueCoachV27(c:RsPalette,lang:RsLang,store:RsStore,role:RsRole){
    if(role==RsRole.TRAINER){
        if(RsSupabaseV60.configured)RsTrainerAiHubV125(c,lang)
        else TrainerTechniqueHistoryV27(c,store,lang)
    }else StudentTechniqueCoachV27(c,lang,store)
}

@Composable
private fun RsTrainerAiHubV125(c:RsPalette,lang:RsLang){
    var section by remember{mutableStateOf("references")}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
            FilterChip(
                selected=section=="references",
                onClick={section="references"},
                label={Text("AI Reference Library")},
                modifier=Modifier.weight(1f)
            )
            FilterChip(
                selected=section=="reviews",
                onClick={section="reviews"},
                label={Text("Student Reviews")},
                modifier=Modifier.weight(1f)
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)){
            if(section=="references")RsTrainerAiReferenceChatV125(c,lang)
            else RsCloudTrainerTechniqueHistoryV78(c,lang)
        }
    }
}

@Composable
private fun StudentTechniqueCoachV27(c:RsPalette,lang:RsLang,store:RsStore){
    val context=LocalContext.current
    var tts by remember{mutableStateOf<TextToSpeech?>(null)}
    var ready by remember{mutableStateOf(false)}
    var selectedTechnique by remember{mutableStateOf("Roundhouse Kick")}
    var videoUri by remember{mutableStateOf("")}
    var videoName by remember{mutableStateOf("")}
    var durationMs by remember{mutableLongStateOf(0L)}
    var analysisReady by remember{mutableStateOf(false)}
    var feedback by remember{mutableStateOf("")}
    var question by remember{mutableStateOf("")}
    var answer by remember{mutableStateOf("")}
    var autoSpeak by remember{mutableStateOf(store.b("voice_auto",true))}
    var techniqueMenu by remember{mutableStateOf(false)}
    var coachGender by remember{mutableStateOf(store.s("ai_coach_gender","male"))}
    var speaking by remember{mutableStateOf(false)}
    var importingVideo by remember{mutableStateOf(false)}
    var cloudSaving by remember{mutableStateOf(false)}
    var analyzingAi by remember{mutableStateOf(false)}
    var aiSummary by remember{mutableStateOf("")}
    var aiReferences by remember{mutableStateOf<List<RsTrainingMediaItemV55>>(emptyList())}
    var referencePreviewUri by remember{mutableStateOf("")}
    var referenceLoading by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()

    DisposableEffect(Unit){
        val engine=TextToSpeech(context){status->ready=status==TextToSpeech.SUCCESS}
        engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(utteranceId:String?){scope.launch{speaking=true}}
            override fun onDone(utteranceId:String?){scope.launch{speaking=false}}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId:String?){scope.launch{speaking=false}}
        })
        tts=engine
        onDispose{engine.stop();engine.shutdown()}
    }
    LaunchedEffect(lang.code,ready,coachGender){
        if(ready){
            tts?.language=lang.locale
            tts?.setSpeechRate(if(coachGender=="female")1.01f else .96f)
            tts?.setPitch(if(coachGender=="female")1.08f else .90f)
        }
    }

    fun acceptVideo(uri:Uri,persist:Boolean){
        val d=videoDurationV27(context,uri)
        if(d<=0L)feedback=coachStudentUiV105(lang,"read_fail")
        else if(d>RS_TECH_VIDEO_MAX_MS)feedback=coachStudentUiV105(lang,"too_long")
        else{
            if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            val pickedName=displayNameV27(context,uri)
            importingVideo=true
            feedback=coachStudentUiV105(lang,"import_private")
            scope.launch{
                val local=withContext(Dispatchers.IO){
                    runCatching{rsCopyTechniqueVideoV36(context,uri)}.getOrNull()
                }
                importingVideo=false
                if(local==null){
                    feedback=coachStudentUiV105(lang,"import_fail")
                }else{
                    val previous=videoUri
                    videoUri=local
                    videoName=pickedName
                    durationMs=d
                    analysisReady=false
                    if(previous.isNotBlank() && previous!=local){
                        rsDeleteTechniqueVideoIfUnusedV36(context,store,previous)
                    }
                    feedback=coachStudentUiV105(lang,"ready")
                }
            }
        }
    }

    val galleryVideoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null)acceptVideo(uri,false)
    }
    val fileVideoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)acceptVideo(uri,true)
    }

    val speechLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val spoken=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if(!spoken.isNullOrBlank())question=spoken
    }

    val baseSummary=coachSummaryV27(lang)
    val summary=if(aiSummary.isNotBlank())aiSummary else baseSummary
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text(coachStudentUiV105(lang,"title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
        Text(coachStudentUiV105(lang,"sub"),color=c.muted)

        RsPanel(c){
            Text(coachStudentUiV105(lang,"trainer"),color=c.bright,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=coachGender=="male",onClick={coachGender="male";store.ps("ai_coach_gender","male")},label={Text("Marcus · "+coachStudentUiV105(lang,"male"))},modifier=Modifier.weight(1f))
                FilterChip(selected=coachGender=="female",onClick={coachGender="female";store.ps("ai_coach_gender","female")},label={Text("Sofia · "+coachStudentUiV105(lang,"female"))},modifier=Modifier.weight(1f))
            }
            CoachAvatarV28(c,store,coachGender,speaking,selectedTechnique,lang)
            Text(coachStudentUiV105(lang,"trainer_desc"),color=c.muted,fontSize=10.sp)
        }

        RsPanel(c){
            Text(coachStudentUiV105(lang,"step1"),color=c.bright,fontWeight=FontWeight.Bold)
            Box{
                OutlinedButton(onClick={techniqueMenu=true},modifier=Modifier.fillMaxWidth()){Text(selectedTechnique)}
                DropdownMenu(expanded=techniqueMenu,onDismissRequest={techniqueMenu=false}){
                    listOf("Jab","Cross","Jab · Cross","Roundhouse Kick","Low Kick","Front Kick","Knee","Defense & Counter","Custom Combination").forEach{t->
                        DropdownMenuItem(text={Text(t)},onClick={selectedTechnique=t;techniqueMenu=false;analysisReady=false;aiSummary="";aiReferences=emptyList();referencePreviewUri=""})
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Button(
                    onClick={galleryVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))},
                    modifier=Modifier.weight(1f),
                    enabled=!importingVideo
                 ){Text(if(importingVideo)coachStudentUiV105(lang,"importing") else if(videoUri.isBlank())coachStudentUiV105(lang,"gallery") else coachStudentUiV105(lang,"replace"))}
                OutlinedButton(
                    onClick={fileVideoPicker.launch(arrayOf("video/*"))},
                    modifier=Modifier.weight(1f),
                    enabled=!importingVideo
                 ){Text(coachStudentUiV105(lang,"files"))}
            }
            Text(coachStudentUiV105(lang,"picker_note"),color=c.muted,fontSize=10.sp)
            if(videoUri.isNotBlank()){
                RsTechniqueVideoPreviewV27(videoUri)
                Text("$videoName · ${"%.1f".format(durationMs/1000.0)} s",color=c.muted,fontSize=10.sp)
                Button(
                    onClick={
                        if(analyzingAi)return@Button
                        if(RsSupabaseV60.configured){
                            analyzingAi=true
                            feedback=coachAiStatusV106(lang,"analyzing")
                            scope.launch{
                                val result=rsAnalyzeTechniqueVisionV106(context,videoUri,selectedTechnique,lang)
                                if(result.isSuccess){
                                    val analysis=result.getOrThrow()
                                    aiSummary=analysis
                                    aiReferences=rsAiReferenceMatchesV114(selectedTechnique,analysis).getOrDefault(emptyList())
                                    referencePreviewUri=""
                                    analysisReady=true
                                    feedback=coachAiStatusV106(lang,"active")
                                }else{
                                    aiSummary=""
                                    aiReferences=emptyList()
                                    referencePreviewUri=""
                                    analysisReady=true
                                    feedback=coachAiStatusV106(lang,"fallback")
                                }
                                analyzingAi=false
                            }
                        }else{
                            aiSummary=""
                            analysisReady=true
                            feedback=coachStudentUiV105(lang,"preview_generated")
                        }
                    },
                    enabled=!analyzingAi,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(analyzingAi)rsReleaseT98(lang,"please_wait") else coachStudentUiV105(lang,"analyze"))}
                OutlinedButton(onClick={val previous=videoUri;videoUri="";videoName="";durationMs=0L;analysisReady=false;aiSummary="";aiReferences=emptyList();referencePreviewUri="";rsDeleteTechniqueVideoIfUnusedV36(context,store,previous)},modifier=Modifier.fillMaxWidth()){Text(coachStudentUiV105(lang,"remove"))}
            }
            if(feedback.isNotBlank())Text(feedback,color=c.muted,fontSize=10.sp)
        }

        if(analysisReady)RsPanel(c){
            Text(coachStudentUiV105(lang,"step2"),color=c.bright,fontWeight=FontWeight.Black)
            Surface(shape=RoundedCornerShape(14.dp),color=c.gold.copy(alpha=.12f)){
                Text(if(aiSummary.isNotBlank())coachAiStatusV106(lang,"active") else coachStudentUiV105(lang,"preview_mode"),color=c.bright,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(10.dp))
            }
            Text(summary,color=c.text)
            techniquePointsV27(lang).forEachIndexed{i,(title,body)->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    VisualCueV27(c,i)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(title,color=c.bright,fontWeight=FontWeight.Bold)
                        Text(body,color=c.muted,fontSize=11.sp)
                    }
                }
            }
            if(aiReferences.isNotEmpty()){
                HorizontalDivider(color=c.gold.copy(alpha=.25f))
                Text(coachAiReferenceUiV114(lang,"title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
                Text(coachAiReferenceUiV114(lang,"sub"),color=c.muted,fontSize=9.sp)
                aiReferences.forEach{item->
                    Surface(color=c.gold.copy(alpha=.07f),shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(item.title,color=c.bright,fontWeight=FontWeight.Bold)
                            Text(item.category+" · "+item.techniqueTags.joinToString(", "),color=c.muted,fontSize=9.sp,maxLines=2)
                            if(item.description.isNotBlank()){
                                Text(item.description,color=c.text,fontSize=11.sp,lineHeight=15.sp,maxLines=4)
                            }
                            OutlinedButton(
                                onClick={
                                    referenceLoading=true
                                    scope.launch{
                                        rsCloudTrainingMediaLocalUriV73(context,item)
                                            .onSuccess{referencePreviewUri=it}
                                            .onFailure{feedback=rsReleaseT98(lang,"load_failed")}
                                        referenceLoading=false
                                    }
                                },
                                enabled=!referenceLoading,
                                modifier=Modifier.fillMaxWidth()
                            ){Text(if(referenceLoading)coachAiReferenceUiV114(lang,"loading") else coachAiReferenceUiV114(lang,"open"),fontSize=9.sp)}
                        }
                    }
                }
                if(referencePreviewUri.isNotBlank()){
                    RsUriPreviewV21(referencePreviewUri,Modifier.fillMaxWidth().height(220.dp),"CENTER")
                }
            }
            Button(onClick={
                if(ready){
                    speaking=true
                    tts?.language=lang.locale
                    tts?.speak(summary,TextToSpeech.QUEUE_FLUSH,null,"technique-coach")
                }
            },enabled=ready,modifier=Modifier.fillMaxWidth()){Text(coachStudentUiV105(lang,"speak"))}
            Button(
                onClick={
                    if(RsSupabaseV60.configured){
                        cloudSaving=true
                        feedback=coachStudentUiV105(lang,"saving_cloud")
                        scope.launch{
                            rsUploadTechniqueSubmissionV78(
                                context,
                                videoUri,
                                videoName,
                                selectedTechnique,
                                summary
                            )
                                .onSuccess{feedback=coachStudentUiV105(lang,"saved_cloud")}
                                .onFailure{feedback=rsReleaseT98(lang,"save_failed")}
                            cloudSaving=false
                        }
                    }else{
                        val items=decodeTechniqueSubsV27(store.s("technique_submissions_v27","")).toMutableList()
                        val id=System.currentTimeMillis().toString()
                        val created=SimpleDateFormat("dd MMM yyyy · HH:mm",Locale.getDefault()).format(Date())
                        items.add(0,TechniqueSubmissionV27(id,videoUri,videoName,selectedTechnique,created,false,summary,store.s("session_student_email","alex@rskickbox.nl")))
                        store.ps("technique_submissions_v27",encodeTechniqueSubsV27(items.take(40)))
                        feedback=coachStudentUiV105(lang,"saved_local")
                    }
                },
                enabled=!cloudSaving,
                modifier=Modifier.fillMaxWidth()
             ){Text(if(cloudSaving)rsReleaseT98(lang,"saving") else coachStudentUiV105(lang,"save_history"))}
        }

        RsPanel(c){
            Text(coachStudentUiV105(lang,"step3"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(coachStudentUiV105(lang,"voice_desc"),color=c.muted,fontSize=10.sp)
            OutlinedTextField(question,{question=it},label={Text(coachStudentUiV105(lang,"ask_label"))},modifier=Modifier.fillMaxWidth(),minLines=2)
            OutlinedButton(onClick={
                val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang.locale.toLanguageTag())
                }
                speechLauncher.launch(intent)
            },modifier=Modifier.fillMaxWidth()){Text(coachStudentUiV105(lang,"ask_voice"))}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(coachStudentUiV105(lang,"auto_speak"),color=c.text,modifier=Modifier.weight(1f))
                Switch(autoSpeak,{v->autoSpeak=v;store.pb("voice_auto",v)})
            }
            Button(onClick={
                answer=summary
                if(autoSpeak&&ready){
                    speaking=true
                    tts?.language=lang.locale
                    tts?.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"technique-answer")
                }
            },enabled=question.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text(coachStudentUiV105(lang,"ask_ai"))}
            if(answer.isNotBlank())Text(answer,color=c.text)
        }
        Spacer(Modifier.height(18.dp))
    }
}


@Composable
private fun CoachAvatarV28(c:RsPalette,store:RsStore,gender:String,speaking:Boolean,technique:String,lang:RsLang){
    val custom=store.s(if(gender=="female")"visual_v21_ai_trainer_female" else "visual_v21_ai_trainer_male","")
    val pulse=rememberInfiniteTransition(label="coachPulse")
    val glow by pulse.animateFloat(.35f,1f,infiniteRepeatable(tween(850),RepeatMode.Reverse),label="coachGlow")
    val bundledPainter=painterResource(id=R.drawable.rs_training_session_v127)
    val trainerName=if(gender=="female")"AI Coach Sofia" else "AI Coach Marcus"
    val trainerRole=if(gender=="female")coachStudentUiV105(lang,"female") else coachStudentUiV105(lang,"male")
    Box(
        Modifier.fillMaxWidth().height(260.dp).background(Color.Black,RoundedCornerShape(18.dp)),
        contentAlignment=Alignment.Center
    ){
        if(custom.isNotBlank()){
            RsUriPreviewV21(custom,Modifier.fillMaxSize(),"CENTER")
        }else{
            Image(
                painter=bundledPainter,
                contentDescription=trainerName,
                contentScale=ContentScale.Crop,
                alignment=if(gender=="female")Alignment.CenterEnd else Alignment.CenterStart,
                modifier=Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier.matchParentSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha=.06f),
                        Color.Transparent,
                        Color.Black.copy(alpha=.64f)
                    )
                )
            )
        )
        if(speaking){
            Canvas(Modifier.fillMaxSize()){
                drawCircle(
                    c.bright.copy(alpha=.22f+.24f*glow),
                    size.minDimension*.08f,
                    Offset(size.width*.88f,size.height*.14f),
                    style=Stroke(5f)
                )
            }
            Surface(
                modifier=Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape=RoundedCornerShape(20.dp),
                color=Color.Black.copy(alpha=.72f),
                border=androidx.compose.foundation.BorderStroke(1.dp,c.bright.copy(alpha=.55f))
            ){
                Text(
                    "●  SPEAKING",
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=9.sp,
                    modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp)
                )
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)){
            Text(trainerName,color=Color.White,fontWeight=FontWeight.Black,fontSize=18.sp)
            Text("RS KICKBOXING · "+trainerRole,color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            Text(coachStudentUiV105(lang,"focus")+" · "+technique,color=Color.White.copy(alpha=.80f),fontSize=10.sp)
        }
    }
}

@Composable
private fun RsTechniqueVideoPreviewV27(uri:String){
    AndroidView(
        factory={ctx->
            VideoView(ctx).apply{
                val controller=MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener{seekTo(1)}
            }
        },
        modifier=Modifier.fillMaxWidth().height(230.dp).background(Color.Black)
    )
}

@Composable
private fun VisualCueV27(c:RsPalette,index:Int){
    Canvas(Modifier.size(58.dp)){
        val w=size.width;val h=size.height
        drawCircle(c.bright.copy(alpha=.16f),w*.46f,Offset(w*.5f,h*.5f),style=Stroke(3f))
        when(index%4){
            0->{drawLine(c.bright,Offset(w*.28f,h*.72f),Offset(w*.72f,h*.72f),5f);drawCircle(c.bright,w*.07f,Offset(w*.5f,h*.32f))}
            1->{drawLine(c.bright,Offset(w*.32f,h*.25f),Offset(w*.68f,h*.25f),5f);drawLine(c.bright,Offset(w*.5f,h*.25f),Offset(w*.5f,h*.72f),5f)}
            2->{drawArc(c.bright,200f,220f,false,topLeft=Offset(w*.18f,h*.18f),size=androidx.compose.ui.geometry.Size(w*.64f,h*.64f),style=Stroke(5f))}
            else->{drawLine(c.bright,Offset(w*.24f,h*.5f),Offset(w*.76f,h*.5f),5f)}
        }
    }
}

@Composable
private fun TrainerTechniqueHistoryV27(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    var revision by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    var pendingDeleteId by remember{mutableStateOf<String?>(null)}
    val items=remember(revision){decodeTechniqueSubsV27(store.s("technique_submissions_v27",""))}
    fun save(updated:List<TechniqueSubmissionV27>){store.ps("technique_submissions_v27",encodeTechniqueSubsV27(updated));revision++}

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text(coachUiV36(lang,"history_title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
        Text(coachUiV36(lang,"history_sub"),color=c.muted)
        if(message.isNotBlank())Text(message,color=c.bright,fontSize=10.sp)
        if(items.isEmpty())RsPanel(c){
            Text(coachUiV36(lang,"none"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(coachUiV36(lang,"none_sub"),color=c.muted)
        }
        items.forEach{item->
            RsPanel(c){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(item.technique,color=c.bright,fontWeight=FontWeight.Black)
                        Text(item.created,color=c.muted,fontSize=10.sp)
                        Text(item.name,color=c.text,fontSize=11.sp,maxLines=1)
                        if(item.ownerEmail.isNotBlank())Text(item.ownerEmail,color=c.muted,fontSize=9.sp,maxLines=1)
                    }
                    Text(if(item.favorite)"★" else "☆",color=c.bright,fontSize=26.sp)
                }
                if(item.uri.isNotBlank())RsTechniqueVideoPreviewV27(item.uri)
                Text(item.summary,color=c.text,fontSize=11.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Button(onClick={
                        save(items.map{if(it.id==item.id)it.copy(favorite=!it.favorite) else it})
                        message=if(item.favorite)coachUiV36(lang,"fav_removed") else coachUiV36(lang,"fav_saved")
                    },modifier=Modifier.weight(1f)){Text(if(item.favorite)coachUiV36(lang,"unfavorite") else coachUiV36(lang,"favorite"))}
                    OutlinedButton(onClick={
                        if(pendingDeleteId==item.id){
                            val updated=items.filterNot{it.id==item.id}
                            save(updated)
                            if(updated.none{it.uri==item.uri})rsDeleteTechniqueVideoV36(context,item.uri)
                            pendingDeleteId=null
                            message=coachUiV36(lang,"deleted")
                        }else{
                            pendingDeleteId=item.id
                            message=coachUiV36(lang,"confirm_delete")
                        }
                    },modifier=Modifier.weight(1f)){
                        Text(if(pendingDeleteId==item.id)coachUiV36(lang,"confirm") else coachUiV36(lang,"delete"))
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}


@Composable
private fun RsCloudTrainerTechniqueHistoryV78(c:RsPalette,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var items by remember{mutableStateOf<List<RsCloudTechniqueSubmissionV78>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}
    var pendingDeleteId by remember{mutableStateOf<String?>(null)}
    var playing by remember{mutableStateOf<RsCloudTechniqueSubmissionV78?>(null)}
    var playingUri by remember{mutableStateOf("")}
    var notes by remember{mutableStateOf<Map<String,String>>(emptyMap())}

    LaunchedEffect(revision){
        loading=true
        rsCloudTechniqueSubmissionsV78()
            .onSuccess{rows->
                items=rows
                notes=rows.associate{it.id to it.trainerNote}
            }
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    val active=playing
    if(active!=null){
        LaunchedEffect(active.id){
            playingUri=""
            status="Loading protected technique video…"
            rsTechniqueLocalUriV78(context,active)
                .onSuccess{playingUri=it;status=""}
                .onFailure{status=rsReleaseT98(lang,"load_failed")}
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            Text(active.studentName.ifBlank{active.studentEmail},color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
            Text(active.technique,color=c.text)
            OutlinedButton(
                onClick={playing=null;playingUri="";status=""},
                modifier=Modifier.fillMaxWidth()
             ){Text(coachStudentUiV105(lang,"back_history"))}
            if(playingUri.isBlank()){
                RsPanel(c){
                    CircularProgressIndicator()
                    Text(status.ifBlank{coachStudentUiV105(lang,"loading_video")},color=c.muted)
                }
            }else{
                RsTechniqueVideoPreviewV27(playingUri)
            }
            RsPanel(c){
                Text(coachStudentUiV105(lang,"student_summary"),color=c.bright,fontWeight=FontWeight.Bold)
                Text(active.studentSummary,color=c.text)
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        Text(coachUiV36(lang,"history_title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
        Text(coachStudentUiV105(lang,"history_cloud_sub"),color=c.muted)
        RsPanel(c){
            Text(
                if(loading)coachStudentUiV105(lang,"syncing") else coachStudentUiV105(lang,"history_connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        if(items.isEmpty()&&!loading)RsPanel(c){
            Text(coachUiV36(lang,"none"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(coachUiV36(lang,"none_sub"),color=c.muted)
        }
        items.forEach{item->
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,item.studentEmail,item.studentName,size=46.dp)
                    Column(Modifier.weight(1f)){
                        Text(item.studentName.ifBlank{item.studentEmail},color=c.bright,fontWeight=FontWeight.Black)
                        Text(item.studentEmail,color=c.muted,fontSize=9.sp)
                        Text(item.technique,color=c.text,fontSize=11.sp)
                    }
                    if(item.trainerFavorite)Text("★",color=c.bright,fontSize=22.sp)
                }
                if(item.studentSummary.isNotBlank())Text(item.studentSummary.take(220),color=c.text,maxLines=4)
                Button(
                    onClick={playing=item;status=""},
                    modifier=Modifier.fillMaxWidth()
                 ){Text(coachStudentUiV105(lang,"open_video"))}
                OutlinedTextField(
                    value=notes[item.id]?:item.trainerNote,
                    onValueChange={v->notes=notes+(item.id to v.take(2000))},
                    label={Text(coachStudentUiV105(lang,"trainer_note"))},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=busyId==null
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Button(
                        onClick={
                            busyId=item.id
                            scope.launch{
                                rsSetTechniqueReviewV78(
                                    item.id,
                                    !item.trainerFavorite,
                                    notes[item.id].orEmpty()
                                )
                                    .onSuccess{
                                        status=if(item.trainerFavorite)coachUiV36(lang,"fav_removed") else coachUiV36(lang,"fav_saved")
                                        revision++
                                    }
                                    .onFailure{status=rsReleaseT98(lang,"save_failed")}
                                busyId=null
                            }
                        },
                        enabled=busyId==null,
                        modifier=Modifier.weight(1f)
                    ){Text(if(item.trainerFavorite)coachUiV36(lang,"unfavorite") else coachUiV36(lang,"favorite"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={
                            if(pendingDeleteId==item.id){
                                busyId=item.id
                                scope.launch{
                                    rsDeleteTechniqueSubmissionV78(item)
                                        .onSuccess{
                                            pendingDeleteId=null
                                            status=coachUiV36(lang,"deleted")
                                            revision++
                                        }
                                        .onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                    busyId=null
                                }
                            }else pendingDeleteId=item.id
                        },
                        enabled=busyId==null,
                        modifier=Modifier.weight(1f)
                    ){Text(if(pendingDeleteId==item.id)coachUiV36(lang,"confirm") else coachUiV36(lang,"delete"),fontSize=10.sp)}
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

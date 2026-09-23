package com.rskickbox.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import java.io.File

private data class VisualSlotV21(val key:String,val title:String,val group:String,val size:String,val hint:String)
private fun visualKeyV21(slot:String)="visual_v21_$slot"
private fun posKeyV21(slot:String)="visual_v21_pos_$slot"
private fun opacityKeyV21(slot:String)="visual_v21_opacity_$slot"


fun rsBundledVisualUriV113(context:android.content.Context,slot:String):String{
    // v0.129: use several packaged RS artworks across dashboard tiles and pages
    // instead of collapsing most surfaces to one student/trainer image.
    val route=slot
        .removePrefix("tile_student_")
        .removePrefix("tile_trainer_")
        .removePrefix("tile_")
    val drawableName=when(route){
        "coachchat","media","voice","notifications","groups"->"rs_chat_experience_v127"
        "session","session_builder","home_training","workout","classes","academy","homework","homework_admin"->"rs_training_session_v127"
        "fightcamp","fightcamp_admin","challenges","challenge_admin"->"rs_fight_camp_v127"
        "techniques","compare","progress","progress_admin","assessments","badges"->"rs_bg_v117_student"
        "community","events","events_admin","members","attendance","checkin","private_lessons","schedule"->"rs_bg_v117_trainer"
        "student_home"->"rs_training_session_v127"
        "trainer_home"->"rs_bg_v117_trainer"
        "login","profile","settings","finance","book","payments","invoices","support","release",
        "privacy_admin","promotions","referrals","documents","themes","backgrounds","branding",
        "intro_settings","landing_admin","access","plans_admin","notes","analytics","guide",
        "student_guide","content","music","music_admin","vault","favorites","history"->"rs_bg_v117_login"
        "header","footer"->"rs_bg_v117_trainer"
        else->{
            // Deterministic spread so unknown/new routes also do not all look identical.
            val pack=listOf(
                "rs_training_session_v127",
                "rs_fight_camp_v127",
                "rs_chat_experience_v127",
                "rs_bg_v117_student",
                "rs_bg_v117_trainer",
                "rs_bg_v117_login"
            )
            pack[(route.hashCode() and Int.MAX_VALUE)%pack.size]
        }
    }
    val id=context.resources.getIdentifier(drawableName,"drawable",context.packageName)
    return if(id==0)"" else "android.resource://"+context.packageName+"/"+id
}

fun rsVisualUriUsableV116(context:android.content.Context,value:String):Boolean{
    if(value.isBlank())return false
    return runCatching{
        val uri=Uri.parse(value)
        when(uri.scheme?.lowercase()){
            "file"->{
                val path=uri.path?:return@runCatching false
                val file=File(path)
                if(!file.exists() || file.length()<=0L)return@runCatching false
                // Old v0.115 visual-pack extracts could exist on disk but contain
                // truncated/corrupt image bytes. Never let them override the APK fallback.
                if(path.contains("rs_visual_pack_v115") || path.contains("rs_visual_pack_v116")){
                    return@runCatching false
                }
                val ext=file.extension.lowercase()
                if(ext in setOf("mp4","webm","mov","3gp"))true
                else{
                    val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    BitmapFactory.decodeFile(path,opts)
                    opts.outWidth>0 && opts.outHeight>0
                }
            }
            "content"->context.contentResolver.openInputStream(uri)?.use{input->
                input.read()!=-1
            }?:false
            "android.resource"->true
            // Cloud visual sync stores a verified local file URI. A raw remote URL here
            // is treated as stale so bundled visuals still work offline and after updates.
            "http","https"->false
            else->false
        }
    }.getOrDefault(false)
}

fun rsVisualUriWithBundledFallbackV113(context:android.content.Context,store:RsStore,slot:String):String{
    val key=visualKeyV21(slot)
    val saved=store.s(key,"")
    if(rsVisualUriUsableV116(context,saved))return saved
    if(saved.isNotBlank())store.ps(key,"")
    return rsBundledVisualUriV113(context,slot)
}


private fun rsVisualStudioT109(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "VISUAL PLACEMENT EDITOR","sub" to "Choose one place. Only that visual is loaded and edited.",
        "category" to "Category","place" to "Place","preview" to "VISUAL PREVIEW",
        "preview_selected" to "Preview selected background","gallery" to "Gallery","files" to "Files","reset" to "Reset",
        "optimizing" to "Automatic formatting / optimization in progress…","overlay" to "Overlay","login_transparency" to "LOGIN FORM TRANSPARENCY",
        "login_transparency_sub" to "Controls how much of the login background remains visible through the login boxes.",
        "position" to "Position","close_preview" to "Close preview","full_test" to "FULL-SCREEN BACKGROUND TEST",
        "default_visual" to "DEFAULT RS VISUAL","custom_saved" to "CUSTOM VISUAL SAVED","cloud_synced" to "Cloud synced",
        "local_cloud_fail" to "Visual saved locally but cloud sync failed.","preparing" to "Preparing visual…",
        "size_small" to "Best for dashboard cards. Use a clear subject with safe space for title text.",
        "size_medium" to "Best for header/footer banners. Use a wide image with the main subject away from text controls.",
        "size_big" to "Best for full-page backgrounds. Use portrait or adaptable artwork with important details away from screen edges."
    )
    val packs=mapOf(
        "nl" to mapOf("title" to "VISUELE PLAATSING","sub" to "Kies één plek. Alleen die visual wordt geladen en bewerkt.","category" to "Categorie","place" to "Plaats","preview" to "VISUELE PREVIEW","preview_selected" to "Geselecteerde achtergrond bekijken","gallery" to "Galerij","files" to "Bestanden","reset" to "Herstellen","optimizing" to "Automatische opmaak / optimalisatie bezig…","overlay" to "Overlay","login_transparency" to "TRANSPARANTIE LOGINFORMULIER","login_transparency_sub" to "Bepaalt hoeveel van de loginachtergrond zichtbaar blijft door de loginvakken.","position" to "Positie","close_preview" to "Preview sluiten","full_test" to "VOLLEDIGE ACHTERGRONDTEST","default_visual" to "STANDAARD RS-VISUAL","custom_saved" to "AANGEPASTE VISUAL OPGESLAGEN","cloud_synced" to "Cloud gesynchroniseerd","local_cloud_fail" to "Visual lokaal opgeslagen maar cloudsynchronisatie is mislukt.","preparing" to "Visual voorbereiden…","size_small" to "Voor dashboardkaarten. Houd ruimte vrij voor titels.","size_medium" to "Voor header/footer-banners. Houd het onderwerp weg van tekstbediening.","size_big" to "Voor volledige pagina-achtergronden. Houd belangrijke details weg van schermranden."),
        "pt" to mapOf("title" to "EDITOR DE POSICIONAMENTO VISUAL","sub" to "Escolhe um local. Apenas esse visual é carregado e editado.","category" to "Categoria","place" to "Local","preview" to "PRÉ-VISUALIZAÇÃO","preview_selected" to "Pré-visualizar fundo selecionado","gallery" to "Galeria","files" to "Ficheiros","reset" to "Repor","optimizing" to "Formatação / otimização automática em curso…","overlay" to "Sobreposição","login_transparency" to "TRANSPARÊNCIA DO LOGIN","login_transparency_sub" to "Controla quanto do fundo fica visível através das caixas de login.","position" to "Posição","close_preview" to "Fechar pré-visualização","full_test" to "TESTE DE FUNDO EM ECRÃ COMPLETO","default_visual" to "VISUAL RS PADRÃO","custom_saved" to "VISUAL PERSONALIZADO GUARDADO","cloud_synced" to "Cloud sincronizada","local_cloud_fail" to "Visual guardado localmente mas a sincronização cloud falhou.","preparing" to "A preparar visual…","size_small" to "Ideal para cartões do painel. Deixa espaço seguro para texto.","size_medium" to "Ideal para banners. Mantém o assunto afastado dos controlos.","size_big" to "Ideal para fundos de página completa. Mantém detalhes importantes longe das margens."),
        "es" to mapOf("title" to "EDITOR DE COLOCACIÓN VISUAL","sub" to "Elige un lugar. Solo ese visual se carga y edita.","category" to "Categoría","place" to "Lugar","preview" to "VISTA PREVIA","preview_selected" to "Previsualizar fondo seleccionado","gallery" to "Galería","files" to "Archivos","reset" to "Restablecer","optimizing" to "Formato / optimización automática en curso…","overlay" to "Superposición","login_transparency" to "TRANSPARENCIA DEL LOGIN","login_transparency_sub" to "Controla cuánto fondo queda visible a través de los campos de login.","position" to "Posición","close_preview" to "Cerrar vista previa","full_test" to "PRUEBA DE FONDO A PANTALLA COMPLETA","default_visual" to "VISUAL RS PREDETERMINADO","custom_saved" to "VISUAL PERSONALIZADO GUARDADO","cloud_synced" to "Cloud sincronizada","local_cloud_fail" to "Visual guardado localmente pero falló la sincronización cloud.","preparing" to "Preparando visual…"),
        "fr" to mapOf("title" to "ÉDITEUR DE PLACEMENT VISUEL","sub" to "Choisis un emplacement. Seul ce visuel est chargé et modifié.","category" to "Catégorie","place" to "Emplacement","preview" to "APERÇU VISUEL","preview_selected" to "Aperçu du fond sélectionné","gallery" to "Galerie","files" to "Fichiers","reset" to "Réinitialiser","optimizing" to "Mise en forme / optimisation automatique…","overlay" to "Superposition","login_transparency" to "TRANSPARENCE DU LOGIN","login_transparency_sub" to "Contrôle la visibilité du fond à travers les champs de connexion.","position" to "Position","close_preview" to "Fermer l’aperçu","full_test" to "TEST DU FOND PLEIN ÉCRAN","default_visual" to "VISUEL RS PAR DÉFAUT","custom_saved" to "VISUEL PERSONNALISÉ ENREGISTRÉ","cloud_synced" to "Cloud synchronisé","local_cloud_fail" to "Visuel enregistré localement mais synchronisation cloud échouée.","preparing" to "Préparation du visuel…"),
        "de" to mapOf("title" to "VISUELLE PLATZIERUNG","sub" to "Wähle einen Bereich. Nur dieses Visual wird geladen und bearbeitet.","category" to "Kategorie","place" to "Bereich","preview" to "VORSCHAU","preview_selected" to "Ausgewählten Hintergrund ansehen","gallery" to "Galerie","files" to "Dateien","reset" to "Zurücksetzen","optimizing" to "Automatische Formatierung / Optimierung läuft…","overlay" to "Overlay","login_transparency" to "LOGIN-TRANSPARENZ","login_transparency_sub" to "Steuert, wie viel Hintergrund durch die Login-Felder sichtbar bleibt.","position" to "Position","close_preview" to "Vorschau schließen","full_test" to "VOLLBILD-HINTERGRUNDTEST","default_visual" to "STANDARD-RS-VISUAL","custom_saved" to "EIGENES VISUAL GESPEICHERT","cloud_synced" to "Cloud synchronisiert","local_cloud_fail" to "Visual lokal gespeichert, Cloud-Sync fehlgeschlagen.","preparing" to "Visual wird vorbereitet…"),
        "it" to mapOf("title" to "EDITOR POSIZIONAMENTO VISIVO","sub" to "Scegli una posizione. Viene caricato e modificato solo quel visual.","category" to "Categoria","place" to "Posizione","preview" to "ANTEPRIMA","preview_selected" to "Anteprima sfondo selezionato","gallery" to "Galleria","files" to "File","reset" to "Ripristina","optimizing" to "Formattazione / ottimizzazione automatica…","overlay" to "Overlay","login_transparency" to "TRASPARENZA LOGIN","login_transparency_sub" to "Controlla quanto sfondo resta visibile attraverso i campi login.","position" to "Posizione","close_preview" to "Chiudi anteprima","full_test" to "TEST SFONDO A SCHERMO INTERO","default_visual" to "VISUAL RS PREDEFINITO","custom_saved" to "VISUAL PERSONALIZZATO SALVATO","cloud_synced" to "Cloud sincronizzato","local_cloud_fail" to "Visual salvato localmente ma sincronizzazione cloud fallita.","preparing" to "Preparazione visual…"),
        "pl" to mapOf("title" to "EDYTOR POŁOŻENIA WIZUALNEGO","sub" to "Wybierz miejsce. Tylko ten element jest ładowany i edytowany.","category" to "Kategoria","place" to "Miejsce","preview" to "PODGLĄD","preview_selected" to "Podgląd wybranego tła","gallery" to "Galeria","files" to "Pliki","reset" to "Resetuj","optimizing" to "Automatyczne formatowanie / optymalizacja…","overlay" to "Nakładka","login_transparency" to "PRZEZROCZYSTOŚĆ LOGOWANIA","login_transparency_sub" to "Steruje widocznością tła przez pola logowania.","position" to "Pozycja","close_preview" to "Zamknij podgląd","full_test" to "TEST TŁA PEŁNOEKRANOWEGO","default_visual" to "DOMYŚLNY WIZUAL RS","custom_saved" to "WŁASNY WIZUAL ZAPISANY","cloud_synced" to "Cloud zsynchronizowany","local_cloud_fail" to "Wizual zapisany lokalnie, ale synchronizacja cloud nie powiodła się.","preparing" to "Przygotowywanie wizualu…"),
        "tr" to mapOf("title" to "GÖRSEL YERLEŞİM EDİTÖRÜ","sub" to "Bir alan seç. Yalnızca o görsel yüklenir ve düzenlenir.","category" to "Kategori","place" to "Alan","preview" to "GÖRSEL ÖNİZLEME","preview_selected" to "Seçili arka planı önizle","gallery" to "Galeri","files" to "Dosyalar","reset" to "Sıfırla","optimizing" to "Otomatik biçimlendirme / optimizasyon sürüyor…","overlay" to "Kaplama","login_transparency" to "GİRİŞ ŞEFFAFLIĞI","login_transparency_sub" to "Giriş kutularından arka planın ne kadar görüneceğini belirler.","position" to "Konum","close_preview" to "Önizlemeyi kapat","full_test" to "TAM EKRAN ARKA PLAN TESTİ","default_visual" to "VARSAYILAN RS GÖRSELİ","custom_saved" to "ÖZEL GÖRSEL KAYDEDİLDİ","cloud_synced" to "Cloud senkronize","local_cloud_fail" to "Görsel yerel kaydedildi fakat cloud senkronizasyonu başarısız.","preparing" to "Görsel hazırlanıyor…")
    )
    return packs[lang.code]?.get(key)?:en[key]?:key
}

private fun rsVisualGroupV109(lang:RsLang,group:String):String{
    val maps=mapOf(
        "nl" to mapOf("Splash / Login" to "Splash / Login","Dashboards" to "Dashboards","Banners" to "Banners","Music Player" to "Muziekspeler","Student / Training" to "Leerling / Training","Student / Club" to "Leerling / Club","Student / Performance" to "Leerling / Prestatie","Student / Library" to "Leerling / Bibliotheek","Student / Account" to "Leerling / Account","Trainer / Brand" to "Trainer / Merk","Trainer / Coaching" to "Trainer / Coaching","Trainer / Members" to "Trainer / Leden","Trainer / Operations" to "Trainer / Operatie","Trainer / Business" to "Trainer / Zakelijk","Dashboard tiles" to "Dashboardkaarten"),
        "pt" to mapOf("Splash / Login" to "Splash / Login","Dashboards" to "Painéis","Banners" to "Banners","Music Player" to "Leitor de Música","Student / Training" to "Aluno / Treino","Student / Club" to "Aluno / Clube","Student / Performance" to "Aluno / Desempenho","Student / Library" to "Aluno / Biblioteca","Student / Account" to "Aluno / Conta","Trainer / Brand" to "Treinador / Marca","Trainer / Coaching" to "Treinador / Coaching","Trainer / Members" to "Treinador / Membros","Trainer / Operations" to "Treinador / Operações","Trainer / Business" to "Treinador / Negócio","Dashboard tiles" to "Cartões do painel"),
        "es" to mapOf("Splash / Login" to "Splash / Login","Dashboards" to "Paneles","Banners" to "Banners","Music Player" to "Reproductor de Música","Student / Training" to "Alumno / Entrenamiento","Student / Club" to "Alumno / Club","Student / Performance" to "Alumno / Rendimiento","Student / Library" to "Alumno / Biblioteca","Student / Account" to "Alumno / Cuenta","Trainer / Brand" to "Entrenador / Marca","Trainer / Coaching" to "Entrenador / Coaching","Trainer / Members" to "Entrenador / Miembros","Trainer / Operations" to "Entrenador / Operaciones","Trainer / Business" to "Entrenador / Negocio","Dashboard tiles" to "Tarjetas del panel"),
        "fr" to mapOf("Splash / Login" to "Splash / Connexion","Dashboards" to "Tableaux de bord","Banners" to "Bannières","Music Player" to "Lecteur Musique","Student / Training" to "Élève / Entraînement","Student / Club" to "Élève / Club","Student / Performance" to "Élève / Performance","Student / Library" to "Élève / Bibliothèque","Student / Account" to "Élève / Compte","Trainer / Brand" to "Entraîneur / Marque","Trainer / Coaching" to "Entraîneur / Coaching","Trainer / Members" to "Entraîneur / Membres","Trainer / Operations" to "Entraîneur / Opérations","Trainer / Business" to "Entraîneur / Activité","Dashboard tiles" to "Cartes du tableau"),
        "de" to mapOf("Splash / Login" to "Splash / Login","Dashboards" to "Dashboards","Banners" to "Banner","Music Player" to "Musik-Player","Student / Training" to "Schüler / Training","Student / Club" to "Schüler / Club","Student / Performance" to "Schüler / Leistung","Student / Library" to "Schüler / Bibliothek","Student / Account" to "Schüler / Konto","Trainer / Brand" to "Trainer / Marke","Trainer / Coaching" to "Trainer / Coaching","Trainer / Members" to "Trainer / Mitglieder","Trainer / Operations" to "Trainer / Betrieb","Trainer / Business" to "Trainer / Geschäft","Dashboard tiles" to "Dashboard-Karten"),
        "it" to mapOf("Splash / Login" to "Splash / Login","Dashboards" to "Dashboard","Banners" to "Banner","Music Player" to "Player Musica","Student / Training" to "Allievo / Allenamento","Student / Club" to "Allievo / Club","Student / Performance" to "Allievo / Prestazione","Student / Library" to "Allievo / Libreria","Student / Account" to "Allievo / Account","Trainer / Brand" to "Allenatore / Brand","Trainer / Coaching" to "Allenatore / Coaching","Trainer / Members" to "Allenatore / Membri","Trainer / Operations" to "Allenatore / Operazioni","Trainer / Business" to "Allenatore / Business","Dashboard tiles" to "Schede dashboard"),
        "pl" to mapOf("Splash / Login" to "Splash / Logowanie","Dashboards" to "Panele","Banners" to "Banery","Music Player" to "Odtwarzacz Muzyki","Student / Training" to "Uczeń / Trening","Student / Club" to "Uczeń / Klub","Student / Performance" to "Uczeń / Wyniki","Student / Library" to "Uczeń / Biblioteka","Student / Account" to "Uczeń / Konto","Trainer / Brand" to "Trener / Marka","Trainer / Coaching" to "Trener / Coaching","Trainer / Members" to "Trener / Członkowie","Trainer / Operations" to "Trener / Operacje","Trainer / Business" to "Trener / Biznes","Dashboard tiles" to "Karty panelu"),
        "tr" to mapOf("Splash / Login" to "Splash / Giriş","Dashboards" to "Paneller","Banners" to "Bannerlar","Music Player" to "Müzik Oynatıcı","Student / Training" to "Öğrenci / Antrenman","Student / Club" to "Öğrenci / Kulüp","Student / Performance" to "Öğrenci / Performans","Student / Library" to "Öğrenci / Kütüphane","Student / Account" to "Öğrenci / Hesap","Trainer / Brand" to "Antrenör / Marka","Trainer / Coaching" to "Antrenör / Koçluk","Trainer / Members" to "Antrenör / Üyeler","Trainer / Operations" to "Antrenör / Operasyon","Trainer / Business" to "Antrenör / İş","Dashboard tiles" to "Panel kartları")
    )
    return maps[lang.code]?.get(group)?:group
}

private fun rsVisualSlotTitleV109(lang:RsLang,slot:VisualSlotV21):String{
    val route=slot.key.removePrefix("tile_")
    return when(slot.key){
        "login"->when(lang.code){"nl"->"Loginpagina";"pt"->"Página de login";"es"->"Página de login";"fr"->"Page de connexion";"de"->"Login-Seite";"it"->"Pagina login";"pl"->"Strona logowania";"tr"->"Giriş sayfası";else->slot.title}
        "student_home"->rsRouteTitle(lang,"home",slot.title)
        "trainer_home"->rsRouteTitle(lang,"trainer",slot.title)
        else->rsRouteTitle(lang,route,slot.title)
    }
}

private val visualSlotsV21=listOf(
    VisualSlotV21("login","Login page","Splash / Login","BIG","Behind member access"),
    VisualSlotV21("student_home","Student dashboard","Dashboards","BIG","Student dashboard background"),
    VisualSlotV21("trainer_home","Trainer dashboard","Dashboards","BIG","Trainer dashboard background"),
    VisualSlotV21("header","App header banner","Banners","MEDIUM","Global header banner"),
    VisualSlotV21("footer","App footer banner","Banners","MEDIUM","Global footer banner"),
    VisualSlotV21("music_wall_1","Music wallpaper 1","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("music_wall_2","Music wallpaper 2","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("music_wall_3","Music wallpaper 3","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("music_wall_4","Music wallpaper 4","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("voice","AI Technique Coach","Student / Training","BIG","AI coach background"),
    VisualSlotV21("ai_trainer_male","AI male trainer idle visual","AI Trainer Personas","BIG","Photorealistic full-body Marcus idle image/video"),
    VisualSlotV21("ai_trainer_male_speaking","AI male trainer speaking visual","AI Trainer Personas","BIG","Photorealistic full-body Marcus speaking animation/video"),
    VisualSlotV21("ai_trainer_female","AI female trainer idle visual","AI Trainer Personas","BIG","Photorealistic full-body Sofia idle image/video"),
    VisualSlotV21("ai_trainer_female_speaking","AI female trainer speaking visual","AI Trainer Personas","BIG","Photorealistic full-body Sofia speaking animation/video"),
    VisualSlotV21("session","Session Player","Student / Training","BIG","Session timer background"),
    VisualSlotV21("academy","RS Academy","Student / Training","BIG","Academy background"),
    VisualSlotV21("techniques","Technique Library","Student / Training","BIG","Technique library background"),
    VisualSlotV21("home_training","Home Training","Student / Training","BIG","Home training background"),
    VisualSlotV21("workout","Workout Generator","Student / Training","BIG","Workout generator background"),
    VisualSlotV21("classes","Classes & Events","Student / Club","BIG","Class booking / manager background"),
    VisualSlotV21("events","RS Events","Student / Club","BIG","Events background"),
    VisualSlotV21("coachchat","Private Coach Chat","Student / Club","BIG","Coach chat background"),
    VisualSlotV21("community","Community","Student / Club","BIG","Community background"),
    VisualSlotV21("groups","Groups","Student / Club","BIG","Groups background"),
    VisualSlotV21("private_lessons","Private Lessons","Student / Club","BIG","Private lessons background"),
    VisualSlotV21("progress","Progress","Student / Performance","BIG","Progress background"),
    VisualSlotV21("challenges","Challenges","Student / Performance","BIG","Challenges background"),
    VisualSlotV21("badges","Badges","Student / Performance","BIG","Badges background"),
    VisualSlotV21("fightcamp","Fight Camp","Student / Performance","BIG","Fight camp background"),
    VisualSlotV21("compare","Technique Compare","Student / Performance","BIG","Technique comparison background"),
    VisualSlotV21("history","Training History","Student / Performance","BIG","History background"),
    VisualSlotV21("vault","Knowledge Vault","Student / Library","BIG","Knowledge background"),
    VisualSlotV21("homework","Homework","Student / Library","BIG","Homework background"),
    VisualSlotV21("favorites","Saved & Favorites","Student / Library","BIG","Favorites background"),
    VisualSlotV21("media","Training Media","Student / Library","BIG","Media background"),
    VisualSlotV21("music","RS Music","Student / Library","BIG","Internal audio page background"),
    VisualSlotV21("finance","Membership & Payments","Student / Account","BIG","Membership background"),
    VisualSlotV21("book","Trainer Book","Student / Account","BIG","Book background"),
    VisualSlotV21("profile","My Profile","Student / Account","BIG","Profile background"),
    VisualSlotV21("search","Search","Student / Account","BIG","Search background"),
    VisualSlotV21("settings","Settings & Privacy","Student / Account","BIG","Settings background"),
    VisualSlotV21("student_guide","Student App Guide","Student / Account","BIG","Student app guide background"),
    VisualSlotV21("guide","Trainer App Guide","Trainer / Brand","BIG","Trainer app guide background"),
    VisualSlotV21("themes","Visual Theme Studio","Trainer / Brand","BIG","Theme studio background"),
    VisualSlotV21("backgrounds","Visual Asset Studio","Trainer / Brand","BIG","Visual studio background"),
    VisualSlotV21("branding","Branding & Site Settings","Trainer / Brand","BIG","Branding settings background"),
    VisualSlotV21("intro_settings","Intro & Splash","Trainer / Brand","BIG","Intro settings background"),
    VisualSlotV21("landing_admin","Promotion Manager","Trainer / Brand","BIG","Promotion background"),
    VisualSlotV21("lesson_editor","Lesson Editor","Trainer / Coaching","BIG","Lesson editor background"),
    VisualSlotV21("content","Content Manager","Trainer / Coaching","BIG","Content manager background"),
    VisualSlotV21("homework_admin","Homework Manager","Trainer / Coaching","BIG","Homework manager background"),
    VisualSlotV21("session_builder","Session Builder","Trainer / Coaching","BIG","Session builder background"),
    VisualSlotV21("music_admin","RS Music Manager","Trainer / Coaching","BIG","Music manager background"),
    VisualSlotV21("notes","Coach Notes","Trainer / Coaching","BIG","Coach notes background"),
    VisualSlotV21("members","Student Manager","Trainer / Members","BIG","Student manager background"),
    VisualSlotV21("access","Access & Subscriptions","Trainer / Members","BIG","Access background"),
    VisualSlotV21("plans_admin","Membership Plans","Trainer / Members","BIG","Plans background"),
    VisualSlotV21("progress_admin","Progress Manager","Trainer / Members","BIG","Progress manager background"),
    VisualSlotV21("assessments","Coach Assessments","Trainer / Members","BIG","Assessments background"),
    VisualSlotV21("challenge_admin","Challenge Manager","Trainer / Members","BIG","Challenge manager background"),
    VisualSlotV21("fightcamp_admin","Fight Camp Manager","Trainer / Members","BIG","Fight camp manager background"),
    VisualSlotV21("attendance","Attendance","Trainer / Operations","BIG","Attendance background"),
    VisualSlotV21("qr_attendance","QR Attendance","Trainer / Operations","BIG","QR attendance background"),
    VisualSlotV21("events_admin","Event Manager","Trainer / Operations","BIG","Event manager background"),
    VisualSlotV21("schedule","Trainer Schedule","Trainer / Operations","BIG","Schedule background"),
    VisualSlotV21("notifications","Notifications","Trainer / Operations","BIG","Notifications background"),
    VisualSlotV21("documents","Documents & Waivers","Trainer / Operations","BIG","Documents background"),
    VisualSlotV21("referrals","Referrals","Trainer / Operations","BIG","Referrals background"),
    VisualSlotV21("payments","Payment Center","Trainer / Business","BIG","Payments background"),
    VisualSlotV21("invoices","Invoices","Trainer / Business","BIG","Invoices background"),
    VisualSlotV21("analytics","Analytics","Trainer / Business","BIG","Analytics background"),
    VisualSlotV21("support","Support & Final QC","Trainer / Business","BIG","Support background"),
    VisualSlotV21("release","Release & Legal Center","Trainer / Business","BIG","Release background")
)

private val dashboardTileSlotsV26=listOf(
    "voice" to "AI Technique Coach","session" to "Session Player","academy" to "RS Academy","techniques" to "Technique Library",
    "home_training" to "Home Training","workout" to "Workout Generator","classes" to "Classes & Events","events" to "RS Events",
    "coachchat" to "Private Coach Chat","community" to "Community","groups" to "Groups","private_lessons" to "Private Lessons",
    "progress" to "Progress","challenges" to "Challenges","badges" to "Badges","fightcamp" to "Fight Camp",
    "compare" to "Technique Compare","history" to "Training History","vault" to "Knowledge Vault","homework" to "Homework",
    "favorites" to "Saved & Favorites","media" to "Training Media","music" to "RS Music","finance" to "Membership & Payments",
    "book" to "Trainer Book","profile" to "My Profile","search" to "Search","settings" to "Settings & Privacy",
    "student_guide" to "Student App Guide","guide" to "Trainer App Guide",
    "themes" to "Visual Theme Studio","backgrounds" to "Visual Asset Studio","branding" to "Branding & Site Settings","intro_settings" to "Intro & Splash",
    "landing_admin" to "Promotion Manager","lesson_editor" to "Lesson Editor","content" to "Content Manager","homework_admin" to "Homework Manager",
    "session_builder" to "Session Builder","music_admin" to "RS Music Manager","notes" to "Coach Notes","members" to "Student Manager",
    "access" to "Access & Subscriptions","plans_admin" to "Membership Plans","progress_admin" to "Progress Manager","assessments" to "Coach Assessments",
    "challenge_admin" to "Challenge Manager","fightcamp_admin" to "Fight Camp Manager","attendance" to "Attendance","qr_attendance" to "QR Attendance",
    "events_admin" to "Event Manager","schedule" to "Trainer Schedule","notifications" to "Notifications","documents" to "Documents & Waivers",
    "referrals" to "Referrals","payments" to "Payment Center","invoices" to "Invoices","analytics" to "Analytics",
    "support" to "Support & Final QC","release" to "Release & Legal Center"
).map{(route,title)->VisualSlotV21("tile_$route",title,"Dashboard tiles","SMALL","Dashboard card visual")}

private val allVisualSlotsV26=visualSlotsV21+dashboardTileSlotsV26

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsVisualAssetStudioV21(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var selectedGroup by remember{mutableStateOf(allVisualSlotsV26.first().group)}
    var selectedSlotKey by remember{mutableStateOf(allVisualSlotsV26.first().key)}
    var groupMenuOpen by remember{mutableStateOf(false)}
    var slotMenuOpen by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var optimizing by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    var fullPreview by remember{mutableStateOf(false)}

    val groups=remember{allVisualSlotsV26.map{it.group}.distinct()}
    val visibleSlots=remember(selectedGroup){allVisualSlotsV26.filter{it.group==selectedGroup}}
    val selected=visibleSlots.firstOrNull{it.key==selectedSlotKey}?:visibleSlots.first()

    LaunchedEffect(selectedGroup){
        if(visibleSlots.none{it.key==selectedSlotKey})selectedSlotKey=visibleSlots.first().key
    }

    var pos by remember(selected.key,refresh){mutableStateOf(store.s(posKeyV21(selected.key),"CENTER"))}
    var opacity by remember(selected.key,refresh){mutableFloatStateOf(store.s(opacityKeyV21(selected.key),"0.55").toFloatOrNull()?:.55f)}
    val customUri=store.s(visualKeyV21(selected.key),"")
    val uri=customUri.ifBlank{rsBundledVisualUriV113(context,selected.key)}

    fun saveVisual(picked:Uri,persist:Boolean){
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(picked,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        optimizing=true
        message=rsVisualStudioT109(lang,"preparing")
        rsImportVisualMediaV29(
            context=context,
            source=picked,
            slotSize=selected.size,
            onStatus={message=it},
            onComplete={info->
                val previous=store.s(visualKeyV21(selected.key),"")
                store.ps(visualKeyV21(selected.key),info.uri)
                store.ps("visual_v21_kind_${selected.key}",info.kind)
                if(previous.isNotBlank() && previous!=info.uri)rsDeleteOwnedVisualV29(context,previous)
                message=info.note
                if(RsSupabaseV60.configured){
                    optimizing=true
                    scope.launch{
                        rsUploadCloudVisualAssetV101(
                            context,
                            "visual:"+selected.key,
                            info.uri,
                            info.kind,
                            pos,
                            opacity
                        )
                            .onSuccess{message=info.note+" · "+rsVisualStudioT109(lang,"cloud_synced")}
                            .onFailure{message=it.message?:rsVisualStudioT109(lang,"local_cloud_fail")}
                        optimizing=false
                        refresh++
                    }
                }else{
                    optimizing=false
                    refresh++
                }
            },
            onError={err->message=err;optimizing=false}
        )
    }
    val galleryPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){picked->
        if(picked!=null)saveVisual(picked,false)
    }
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){picked->
        if(picked!=null)saveVisual(picked,true)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal=6.dp,vertical=4.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Text(rsVisualStudioT109(lang,"title"),color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
        Text(rsVisualStudioT109(lang,"sub"),color=c.muted,fontSize=11.sp,maxLines=2)

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            ExposedDropdownMenuBox(
                expanded=groupMenuOpen,
                onExpandedChange={groupMenuOpen=!groupMenuOpen},
                modifier=Modifier.weight(1f)
            ){
                OutlinedTextField(
                    value=rsVisualGroupV109(lang,selectedGroup),
                    onValueChange={},
                    readOnly=true,
                    label={Text(rsVisualStudioT109(lang,"category"),fontSize=10.sp)},
                    trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(groupMenuOpen)},
                    modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    textStyle=LocalTextStyle.current.copy(fontSize=11.sp),
                    singleLine=true
                )
                ExposedDropdownMenu(expanded=groupMenuOpen,onDismissRequest={groupMenuOpen=false}){
                    groups.forEach{g->
                        DropdownMenuItem(text={Text(rsVisualGroupV109(lang,g),fontSize=12.sp)},onClick={
                            selectedGroup=g
                            selectedSlotKey=allVisualSlotsV26.first{it.group==g}.key
                            groupMenuOpen=false
                        })
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded=slotMenuOpen,
                onExpandedChange={slotMenuOpen=!slotMenuOpen},
                modifier=Modifier.weight(1.25f)
            ){
                OutlinedTextField(
                    value=rsVisualSlotTitleV109(lang,selected),
                    onValueChange={},
                    readOnly=true,
                    label={Text(rsVisualStudioT109(lang,"place"),fontSize=10.sp)},
                    trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(slotMenuOpen)},
                    modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    textStyle=LocalTextStyle.current.copy(fontSize=10.sp),
                    singleLine=true
                )
                ExposedDropdownMenu(expanded=slotMenuOpen,onDismissRequest={slotMenuOpen=false}){
                    visibleSlots.forEach{slot->
                        DropdownMenuItem(text={Text(rsVisualSlotTitleV109(lang,slot),fontSize=12.sp)},onClick={selectedSlotKey=slot.key;slotMenuOpen=false})
                    }
                }
            }
        }

        Surface(
            modifier=Modifier.fillMaxWidth().weight(1f),
            shape=RoundedCornerShape(22.dp),
            color=c.panel,
            border=BorderStroke(1.dp,c.gold.copy(alpha=.55f))
        ){
            Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(rsVisualSlotTitleV109(lang,selected),color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp,maxLines=2)
                        Text(selected.hint,color=c.muted,fontSize=10.sp,maxLines=2)
                    }
                    Surface(shape=RoundedCornerShape(12.dp),color=c.gold.copy(alpha=.18f),border=BorderStroke(1.dp,c.bright.copy(alpha=.35f))){
                        Text(selected.size,color=c.bright,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))
                    }
                }

                Text(
                    when(selected.size){
                        "SMALL"->rsVisualStudioT109(lang,"size_small")
                        "MEDIUM"->rsVisualStudioT109(lang,"size_medium")
                        else->rsVisualStudioT109(lang,"size_big")
                    },
                    color=c.text.copy(alpha=.84f),fontSize=9.sp,maxLines=3
                )

                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(c.panel2)
                ){
                    if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos)
                    else Box(Modifier.fillMaxSize().background(c.gold.copy(alpha=.10f)),contentAlignment=Alignment.Center){
                        Column(horizontalAlignment=Alignment.CenterHorizontally){
                            Text("♛ RS",color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black)
                            Text(rsVisualStudioT109(lang,"preview"),color=c.muted,fontSize=10.sp)
                        }
                    }
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity.coerceIn(0f,.85f))))
                    Text(if(customUri.isBlank())rsVisualStudioT109(lang,"default_visual") else rsVisualStudioT109(lang,"custom_saved"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=9.sp,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
                }

                if(uri.isNotBlank()){
                    Button(
                        onClick={fullPreview=true},
                        enabled=!optimizing,
                        modifier=Modifier.fillMaxWidth()
                    ){Text("▶ "+rsVisualStudioT109(lang,"preview_selected"),fontSize=10.sp)}
                }

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(
                        onClick={galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))},
                        enabled=!optimizing,
                        modifier=Modifier.weight(1f)
                    ){Text(rsVisualStudioT109(lang,"gallery"),fontSize=10.sp)}
                    OutlinedButton(onClick={filePicker.launch(arrayOf("image/*","video/*"))},enabled=!optimizing,modifier=Modifier.weight(1f)){Text(rsVisualStudioT109(lang,"files"),fontSize=10.sp)}
                    OutlinedButton(
                        onClick={
                            val previous=store.s(visualKeyV21(selected.key),"")
                            rsDeleteOwnedVisualV29(context,previous)
                            store.ps(visualKeyV21(selected.key),"")
                            store.ps("visual_v21_kind_${selected.key}","")
                            message="${selected.title} reset."
                            if(RsSupabaseV60.configured){
                                scope.launch{
                                    rsDeleteCloudVisualAssetV101("visual:"+selected.key)
                                    refresh++
                                }
                            }else refresh++
                        },
                        enabled=uri.isNotBlank(),
                        modifier=Modifier.weight(1f)
                    ){Text(rsVisualStudioT109(lang,"reset"),fontSize=10.sp)}
                }

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    listOf("LEFT","CENTER","RIGHT","TOP","BOTTOM").forEach{p->
                        FilterChip(
                            selected=pos==p,
                            onClick={
                                pos=p
                                store.ps(posKeyV21(selected.key),p)
                                if(RsSupabaseV60.configured){
                                    scope.launch{rsUpdateCloudVisualMetadataV101("visual:"+selected.key,p,opacity)}
                                }
                            },
                            label={Text(p.take(1),fontSize=9.sp)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                }

                if(optimizing){
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(rsVisualStudioT109(lang,"optimizing"),color=c.bright,fontSize=9.sp)
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(rsVisualStudioT109(lang,"overlay")+" ${(opacity*100).toInt()}%",color=c.muted,fontSize=9.sp,modifier=Modifier.width(75.dp))
                    Slider(
                        value=opacity,
                        onValueChange={v->opacity=v;store.ps(opacityKeyV21(selected.key),v.toString())},
                        onValueChangeFinished={
                            if(RsSupabaseV60.configured){
                                scope.launch{rsUpdateCloudVisualMetadataV101("visual:"+selected.key,pos,opacity)}
                            }
                        },
                        valueRange=0f..0.85f,
                        modifier=Modifier.weight(1f)
                    )
                }
                if(selected.key=="login"){
                    var loginFormOpacity by remember(refresh){mutableFloatStateOf(store.s("login_form_opacity","0.82").toFloatOrNull()?:.82f)}
                    Text(rsVisualStudioT109(lang,"login_transparency"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
                    Text(rsVisualStudioT109(lang,"login_transparency_sub"),color=c.muted,fontSize=9.sp)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("${(loginFormOpacity*100).toInt()}%",color=c.text,fontSize=10.sp,modifier=Modifier.width(45.dp))
                        Slider(
                            value=loginFormOpacity,
                            onValueChange={v->
                                loginFormOpacity=v
                                store.ps("login_form_opacity",v.toString())
                            },
                            onValueChangeFinished={
                                if(RsSupabaseV60.configured){
                                    scope.launch{
                                        rsSaveCloudBrandV100(
                                            store.s("brand_header_name","RS KICKBOXING"),
                                            store.s("brand_login_title","Premium cinematic kickboxing"),
                                            store.s("brand_login_subtitle","TRAIN · LEARN · CONNECT · GROW"),
                                            store.s("brand_footer_text","RS KICKBOXING · TRAIN · LEARN · CONNECT · GROW"),
                                            store.s("theme","ELITE_GOLD"),
                                            loginFormOpacity
                                        )
                                    }
                                }
                            },
                            valueRange=.20f..1f,
                            modifier=Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if(message.isNotBlank())Text(message,color=c.bright,fontSize=10.sp,maxLines=1)
    }

    if(fullPreview && uri.isNotBlank()){
        androidx.compose.ui.window.Dialog(onDismissRequest={fullPreview=false}){
            Surface(
                color=Color.Black,
                modifier=Modifier.fillMaxWidth().fillMaxHeight(.94f)
            ){
                Box(Modifier.fillMaxSize().background(Color.Black)){
                    RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos)
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity.coerceIn(0f,.85f))))
                    Column(
                        Modifier.align(Alignment.TopStart).fillMaxWidth().padding(14.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)
                    ){
                        Text(rsVisualSlotTitleV109(lang,selected),color=Color.White,fontWeight=FontWeight.Black)
                        Text(rsVisualStudioT109(lang,"position")+" $pos · "+rsVisualStudioT109(lang,"overlay")+" ${(opacity*100).toInt()}%",color=Color.White.copy(alpha=.74f),fontSize=10.sp)
                    }
                    TextButton(
                        onClick={fullPreview=false},
                        modifier=Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ){Text(rsVisualStudioT109(lang,"close_preview"),color=Color.White)}
                    Text(
                        rsVisualStudioT109(lang,"full_test"),
                        color=Color.White.copy(alpha=.72f),
                        fontSize=9.sp,
                        fontWeight=FontWeight.Bold,
                        modifier=Modifier.align(Alignment.BottomCenter).padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RsBrandSiteSettingsV21(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var header by remember{mutableStateOf(store.s("brand_header_name","RS KICKBOXING"))}
    var title by remember{mutableStateOf(store.s("brand_login_title","Premium cinematic kickboxing"))}
    var subtitle by remember{mutableStateOf(store.s("brand_login_subtitle","TRAIN · LEARN · CONNECT · GROW"))}
    var footer by remember{mutableStateOf(store.s("brand_footer_text","RS KICKBOXING · TRAIN · LEARN · CONNECT · GROW"))}
    var loginOpacity by remember{mutableFloatStateOf(store.s("login_form_opacity","0.82").toFloatOrNull()?.coerceIn(.20f,1f)?:.82f)}
    var active by remember{mutableStateOf("")}
    var refresh by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    var saving by remember{mutableStateOf(false)}

    fun saveBrandAsset(uri:Uri,persist:Boolean){
        if(active.isBlank())return
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        store.ps("brand_asset_$active",uri.toString())
        val activeKey=active
        refresh++
        message=active+" updated."
        if(RsSupabaseV60.configured){
            scope.launch{
                val kind=rsVisualKindV29(context,uri)
                rsUploadCloudVisualAssetV101(context,"brand:"+activeKey,uri.toString(),kind,"CENTER",0f)
                    .onSuccess{message=activeKey+" updated · cloud synced"}
                    .onFailure{message=it.message?:"Brand asset saved locally but cloud sync failed."}
            }
        }
    }

    val galleryPickerBrand=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null)saveBrandAsset(uri,false)
    }
    val filePickerBrand=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)saveBrandAsset(uri,true)
    }

    RsScroll(c,rsBrandUiV100(lang,"title"),rsBrandUiV100(lang,"sub")){
        RsPanel(c){
            Text(rsBrandUiV100(lang,"identity"),color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(header,{header=it.take(80)},label={Text(rsBrandUiV100(lang,"header"))},modifier=Modifier.fillMaxWidth(),enabled=!saving)
            OutlinedTextField(title,{title=it.take(140)},label={Text(rsBrandUiV100(lang,"login_title"))},modifier=Modifier.fillMaxWidth(),enabled=!saving)
            OutlinedTextField(subtitle,{subtitle=it.take(180)},label={Text(rsBrandUiV100(lang,"login_subtitle"))},modifier=Modifier.fillMaxWidth(),enabled=!saving)
            OutlinedTextField(footer,{footer=it.take(180)},label={Text(rsBrandUiV100(lang,"footer"))},modifier=Modifier.fillMaxWidth(),enabled=!saving)
            Text(rsBrandUiV100(lang,"opacity")+" · "+(loginOpacity*100).toInt()+"%",color=c.muted,fontSize=10.sp)
            Slider(
                value=loginOpacity,
                onValueChange={loginOpacity=it},
                valueRange=.20f..1f,
                enabled=!saving,
                modifier=Modifier.fillMaxWidth()
            )
            Button(
                onClick={
                    store.ps("brand_header_name",header.trim())
                    store.ps("brand_login_title",title.trim())
                    store.ps("brand_login_subtitle",subtitle.trim())
                    store.ps("brand_footer_text",footer.trim())
                    store.ps("login_form_opacity",loginOpacity.toString())
                    if(RsSupabaseV60.configured){
                        saving=true
                        message=""
                        scope.launch{
                            rsSaveCloudBrandV100(
                                header,
                                title,
                                subtitle,
                                footer,
                                store.s("theme","ELITE_GOLD"),
                                loginOpacity
                            )
                                .onSuccess{message=rsBrandUiV100(lang,"saved")}
                                .onFailure{message=it.message?:rsBrandUiV100(lang,"save_failed")}
                            saving=false
                        }
                    }else{
                        message=rsBrandUiV100(lang,"saved")
                    }
                },
                enabled=!saving&&header.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(saving)rsBrandUiV100(lang,"saving") else rsBrandUiV100(lang,"save"))}
            if(message.isNotBlank())Text(message,color=c.muted,fontSize=10.sp)
        }

        Text(rsBrandUiV100(lang,"assets"),color=c.bright,fontWeight=FontWeight.Black)
        Text(rsBrandUiV100(lang,"asset_note"),color=c.muted,fontSize=10.sp)

        listOf(
            "main_logo" to "Main RS logo",
            "header_letters_logo" to "RS KICKBOXING header letters logo",
            "compact_logo" to "Legacy compact logo",
            "royal_crown" to "Royal crown artwork",
            "favicon" to "Favicon / release icon preview"
        ).forEach{(keyName,label)->
            val uri=store.s("brand_asset_$keyName","")
            RsPanel(c){
                Text(label,color=c.bright,fontWeight=FontWeight.Bold)
                if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxWidth().height(110.dp),"CENTER")
                else Box(Modifier.fillMaxWidth().height(70.dp).background(c.panel2),contentAlignment=Alignment.Center){
                    Text("♛ RS",color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(
                        onClick={
                            active=keyName
                            galleryPickerBrand.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier=Modifier.weight(1f)
                    ){Text(if(uri.isBlank())rsBrandUiV100(lang,"upload") else rsBrandUiV100(lang,"replace"))}
                    OutlinedButton(
                        onClick={
                            store.ps("brand_asset_$keyName","")
                            if(RsSupabaseV60.configured){
                                scope.launch{rsDeleteCloudVisualAssetV101("brand:"+keyName);refresh++}
                            }else refresh++
                        },
                        enabled=uri.isNotBlank(),
                        modifier=Modifier.weight(1f)
                    ){Text(rsBrandUiV100(lang,"reset"))}
                }
            }
        }

        RsPanel(c){
            Text(rsBrandUiV100(lang,"release"),color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsBrandUiV100(lang,"release_note"),color=c.muted)
        }
        key(refresh){Spacer(Modifier.height(1.dp))}
    }
}

private val rsPreviewBitmapCacheV129=object:LruCache<String,Bitmap>(20*1024*1024){
    override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
}

private fun rsCachedPreviewBitmapV129(context:android.content.Context,uri:Uri):Bitmap?{
    val key=uri.toString()
    rsPreviewBitmapCacheV129.get(key)?.let{return it}
    return runCatching{
        val resolver=context.contentResolver
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
        if(bounds.outWidth<=0||bounds.outHeight<=0)return@runCatching null
        var sample=1
        // Keep enough detail for book covers and premium dashboard tiles while
        // still sampling very large camera/gallery images to avoid OOM crashes.
        while(bounds.outWidth/sample>1080 || bounds.outHeight/sample>1080)sample*=2
        val opts=BitmapFactory.Options().apply{
            inSampleSize=sample
            inPreferredConfig=Bitmap.Config.ARGB_8888
        }
        val bitmap=resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}
        if(bitmap!=null)rsPreviewBitmapCacheV129.put(key,bitmap)
        bitmap
    }.getOrNull()
}

@Composable
fun RsUriPreviewV21(uri:String,modifier:Modifier=Modifier,position:String="CENTER"){
    val context=LocalContext.current
    val parsed=remember(uri){Uri.parse(uri)}
    val kind=remember(uri){rsVisualKindV29(context,parsed)}
    when(kind){
        "VIDEO"->{
            val player=remember(uri){
                ExoPlayer.Builder(context).build().apply{
                    volume=0f
                    repeatMode=Player.REPEAT_MODE_ONE
                    setMediaItem(MediaItem.fromUri(parsed))
                    prepare()
                    playWhenReady=true
                }
            }
            DisposableEffect(player){onDispose{player.release()}}
            AndroidView(
                factory={ctx->
                    PlayerView(ctx).apply{
                        useController=false
                        resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player=player
                    }
                },
                modifier=modifier,
                update={view->
                    view.player=player
                    view.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    if(!player.isPlaying)player.playWhenReady=true
                }
            )
        }
        "GIF"->AndroidView(
            factory={ctx->ImageView(ctx).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_CROP}},
            modifier=modifier,
            update={view->
                if(Build.VERSION.SDK_INT>=28)runCatching{
                    val src=ImageDecoder.createSource(context.contentResolver,parsed)
                    val drawable=ImageDecoder.decodeDrawable(src)
                    view.setImageDrawable(drawable)
                    (drawable as? AnimatedImageDrawable)?.apply{
                        repeatCount=AnimatedImageDrawable.REPEAT_INFINITE
                        start()
                    }
                }
            }
        )
        else->{
            val bitmap=remember(uri){rsCachedPreviewBitmapV129(context,parsed)}
            AndroidView(
                factory={ctx->ImageView(ctx).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_CROP}},
                modifier=modifier,
                update={view->
                    view.scaleType=when(position){
                        "LEFT","RIGHT","TOP","BOTTOM"->ImageView.ScaleType.CENTER_CROP
                        else->ImageView.ScaleType.CENTER_CROP
                    }
                    view.setImageBitmap(bitmap)
                }
            )
        }
    }
}

@Composable
fun RsPerPageBackgroundV21(store:RsStore,route:String,content:@Composable ()->Unit){
    val context=LocalContext.current
    val key=when(route){"home"->"student_home";"trainer"->"trainer_home";else->route}
    val saved=store.s(visualKeyV21(key),"")
    val customVisual=rsVisualUriUsableV116(context,saved)
    val uri=if(customVisual)saved else rsBundledVisualUriV113(context,key)
    val configuredOpacity=store.s(opacityKeyV21(key),"0.34").toFloatOrNull()?:.34f
    // Keep bundled art clearly visible. Custom Visual Studio backgrounds still use
    // the trainer-selected opacity without being overridden here.
    val opacity=if(customVisual)configuredOpacity.coerceIn(0f,.78f)
        else configuredOpacity.coerceIn(0f,.34f)
    val pos=store.s(posKeyV21(key),"CENTER")
    Box(Modifier.fillMaxSize()){
        if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos)
        if(uri.isNotBlank())Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity)))
        content()
    }
}

@Composable
fun RsBrandedHeaderV21(c:RsPalette,store:RsStore,content:@Composable ColumnScope.()->Unit){
    val context=LocalContext.current
    val uri=rsVisualUriWithBundledFallbackV113(context,store,"header")
    val layout=rsThemeLayoutV175(rsStoredThemeV175(store))
    val headerBrush=when(layout.mode){
        "FIGHT_STRIP"->androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(c.bg,c.panel2,c.gold.copy(alpha=.20f),c.bg))
        "TECH_COMPACT"->androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.panel,c.panel2,c.bg))
        "HOLO_CARDS"->androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(c.panel2,c.bright.copy(alpha=.10f),c.panel))
        "PERFORMANCE_STACK"->androidx.compose.ui.graphics.Brush.verticalGradient(listOf(c.panel2,c.panel,c.bg))
        else->androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(c.panel,c.gold.copy(alpha=.08f),c.panel2))
    }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(layout.panelRadius.dp))
            .background(headerBrush)
    ){
        if(uri.isNotBlank())RsUriPreviewV21(
            uri,
            Modifier.matchParentSize(),
            store.s(posKeyV21("header"),"CENTER")
        )
        Box(
            Modifier.matchParentSize().background(
                Color.Black.copy(
                    alpha=store.s(opacityKeyV21("header"),"0.46").toFloatOrNull()?:.46f
                )
            )
        )
        if(layout.strongLines){
            Box(
                Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Transparent,c.bright,c.gold,Color.Transparent)
                        )
                    )
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(
                horizontal=when(layout.mode){"TECH_COMPACT"->7.dp;"FIGHT_STRIP"->11.dp;else->9.dp},
                vertical=when(layout.mode){"TECH_COMPACT"->5.dp;"HOLO_CARDS"->9.dp;else->7.dp}
            ),
            verticalArrangement=Arrangement.spacedBy(4.dp)
        ){
            content()
        }
    }
}

@Composable
fun RsBrandedFooterV21(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    val uri=rsVisualUriWithBundledFallbackV113(context,store,"footer")
    Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(16.dp)).background(c.panel),contentAlignment=Alignment.Center){if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.matchParentSize(),store.s(posKeyV21("footer"),"CENTER"));Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.55f)));Text(store.s("brand_footer_text","RS KICKBOXING · TRAIN · LEARN · CONNECT · GROW"),color=c.muted,fontSize=9.sp,maxLines=1)}
}

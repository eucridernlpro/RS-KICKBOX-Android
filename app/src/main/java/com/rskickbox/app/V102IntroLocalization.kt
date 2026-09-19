package com.rskickbox.app

fun rsIntroT102(lang:RsLang,key:String):String{
    val en=mapOf(
        "skip" to "Skip","studio" to "Splash Video Studio",
        "studio_sub" to "Dedicated phone and tablet splash videos with automatic device selection.",
        "master" to "SPLASH MASTER CONTROLS","enable" to "Enable splash video","enable_sub" to "Master on/off switch",
        "sound" to "Video sound","sound_sub" to "Play the splash video's own audio",
        "skip_toggle" to "Show Skip button","skip_sub" to "Lets members bypass the splash",
        "every" to "Show every fresh app launch","every_sub" to "Show the splash again whenever the app starts fresh",
        "phone" to "PHONE SPLASH VIDEO","tablet" to "TABLET SPLASH VIDEO",
        "gallery" to "Gallery","replace" to "Replace","files" to "Files",
        "pending" to "PENDING PREVIEW — NOT SAVED","saved" to "SAVED SPLASH VIDEO",
        "duration" to "Duration","max" to "15.00 sec max",
        "test" to "▶ Test full-screen splash","optimizing" to "Optimizing video for reliable playback…",
        "save" to "✓ Save / Accept splash","cancel" to "Cancel edit","delete" to "Delete saved splash",
        "auto" to "AUTO DEVICE SELECTION",
        "auto_desc" to "Phones use the Phone Splash. Tablets use the Tablet Splash. If no tablet splash is saved, the phone version is used as fallback.",
        "crop_desc" to "Both videos are center-cropped automatically to fill the screen without stretching.",
        "close_preview" to "Close preview","full_test" to "FULL-SCREEN SPLASH TEST",
        "cloud_saved" to "Splash settings saved to Supabase.","cloud_failed" to "Splash settings could not be saved.",
        "phone_ready" to "Phone splash preview ready.","tablet_ready" to "Tablet splash preview ready.",
        "read_error" to "Could not read this video's duration. Choose another file.",
        "too_long" to "Video rejected. Maximum splash length is 15 seconds.",
        "phone_synced" to "Phone splash saved and cloud synced.","tablet_synced" to "Tablet splash saved and cloud synced.",
        "local_cloud_fail" to "Splash saved locally but cloud sync failed.",
        "deleted" to "Splash deleted.","discarded" to "Pending splash edit discarded."
    )
    val nl=en+mapOf(
        "skip" to "Overslaan","studio" to "Splashvideo Studio","studio_sub" to "Aparte splashvideo's voor telefoon en tablet met automatische apparaatkeuze.",
        "master" to "SPLASH HOOFDINSTELLINGEN","enable" to "Splashvideo inschakelen","enable_sub" to "Hoofdschakelaar aan/uit",
        "sound" to "Videogeluid","sound_sub" to "Speel het eigen geluid van de splashvideo af","skip_toggle" to "Knop Overslaan tonen","skip_sub" to "Laat leden de splash overslaan",
        "every" to "Bij elke nieuwe appstart tonen","every_sub" to "Toon de splash opnieuw bij iedere verse appstart",
        "phone" to "TELEFOON SPLASHVIDEO","tablet" to "TABLET SPLASHVIDEO","gallery" to "Galerij","replace" to "Vervangen","files" to "Bestanden",
        "pending" to "PREVIEW — NOG NIET OPGESLAGEN","saved" to "OPGESLAGEN SPLASHVIDEO","duration" to "Duur","max" to "max. 15,00 sec",
        "test" to "▶ Splash op volledig scherm testen","optimizing" to "Video optimaliseren voor betrouwbare weergave…","save" to "✓ Splash opslaan / accepteren","cancel" to "Bewerking annuleren","delete" to "Opgeslagen splash verwijderen",
        "auto" to "AUTOMATISCHE APPARAATKEUZE","auto_desc" to "Telefoons gebruiken de telefoonsplash. Tablets gebruiken de tabletsplash. Zonder tabletsplash wordt de telefoonversie gebruikt.",
        "crop_desc" to "Beide video's worden automatisch gecentreerd bijgesneden zonder uitrekken.","close_preview" to "Preview sluiten","full_test" to "VOLLEDIG SCHERM SPLASH TEST",
        "cloud_saved" to "Splashinstellingen opgeslagen in Supabase.","cloud_failed" to "Splashinstellingen konden niet worden opgeslagen.","read_error" to "De videoduur kon niet worden gelezen. Kies een ander bestand.","too_long" to "Video geweigerd. Maximale splashduur is 15 seconden.",
        "phone_synced" to "Telefoonsplash opgeslagen en met cloud gesynchroniseerd.","tablet_synced" to "Tabletsplash opgeslagen en met cloud gesynchroniseerd.","local_cloud_fail" to "Splash lokaal opgeslagen maar cloudsynchronisatie mislukt.","deleted" to "Splash verwijderd.","discarded" to "Openstaande splashbewerking verwijderd."
    )
    val pt=en+mapOf(
        "skip" to "Saltar","studio" to "Estúdio de Vídeo Splash","studio_sub" to "Vídeos splash dedicados para telemóvel e tablet com seleção automática.",
        "master" to "CONTROLOS PRINCIPAIS","enable" to "Ativar vídeo splash","enable_sub" to "Interruptor principal","sound" to "Som do vídeo","sound_sub" to "Reproduzir o áudio do vídeo splash",
        "skip_toggle" to "Mostrar botão Saltar","skip_sub" to "Permite saltar o splash","every" to "Mostrar em cada novo arranque","every_sub" to "Mostrar novamente sempre que a app iniciar de novo",
        "phone" to "SPLASH TELEMÓVEL","tablet" to "SPLASH TABLET","gallery" to "Galeria","replace" to "Substituir","files" to "Ficheiros","pending" to "PRÉ-VISUALIZAÇÃO — NÃO GUARDADO","saved" to "SPLASH GUARDADO",
        "duration" to "Duração","max" to "máx. 15,00 s","test" to "▶ Testar splash em ecrã completo","optimizing" to "A otimizar vídeo…","save" to "✓ Guardar / Aceitar splash","cancel" to "Cancelar edição","delete" to "Eliminar splash guardado",
        "auto" to "SELEÇÃO AUTOMÁTICA","auto_desc" to "Telemóveis usam o splash de telemóvel. Tablets usam o splash de tablet. Sem versão tablet, é usada a versão de telemóvel.","crop_desc" to "Os vídeos são recortados ao centro automaticamente sem deformação.","close_preview" to "Fechar pré-visualização","full_test" to "TESTE SPLASH ECRÃ COMPLETO",
        "cloud_saved" to "Definições splash guardadas no Supabase.","cloud_failed" to "Não foi possível guardar as definições splash.","read_error" to "Não foi possível ler a duração do vídeo. Escolhe outro ficheiro.","too_long" to "Vídeo rejeitado. O máximo é 15 segundos.","phone_synced" to "Splash de telemóvel guardado e sincronizado.","tablet_synced" to "Splash de tablet guardado e sincronizado.","local_cloud_fail" to "Splash guardado localmente mas falhou a sincronização cloud.","deleted" to "Splash eliminado.","discarded" to "Edição pendente descartada."
    )
    val es=en+mapOf(
        "skip" to "Saltar","studio" to "Estudio de Vídeo Splash","studio_sub" to "Vídeos splash para móvil y tablet con selección automática.",
        "master" to "CONTROLES PRINCIPALES","enable" to "Activar vídeo splash","enable_sub" to "Interruptor principal","sound" to "Sonido del vídeo","sound_sub" to "Reproducir el audio del splash",
        "skip_toggle" to "Mostrar botón Saltar","skip_sub" to "Permite omitir el splash","every" to "Mostrar en cada inicio nuevo","every_sub" to "Mostrar de nuevo al iniciar la app",
        "phone" to "SPLASH MÓVIL","tablet" to "SPLASH TABLET","gallery" to "Galería","replace" to "Reemplazar","files" to "Archivos","pending" to "VISTA PREVIA — NO GUARDADO","saved" to "SPLASH GUARDADO",
        "duration" to "Duración","max" to "máx. 15,00 s","test" to "▶ Probar splash a pantalla completa","optimizing" to "Optimizando vídeo…","save" to "✓ Guardar / Aceptar splash","cancel" to "Cancelar edición","delete" to "Eliminar splash guardado",
        "auto" to "SELECCIÓN AUTOMÁTICA","auto_desc" to "Los móviles usan el splash móvil. Las tablets usan el splash tablet. Sin versión tablet se usa la móvil.","crop_desc" to "Los vídeos se recortan al centro automáticamente sin deformarse.","close_preview" to "Cerrar vista previa","full_test" to "PRUEBA SPLASH PANTALLA COMPLETA",
        "cloud_saved" to "Ajustes splash guardados en Supabase.","cloud_failed" to "No se pudieron guardar los ajustes splash.","read_error" to "No se pudo leer la duración del vídeo. Elige otro archivo.","too_long" to "Vídeo rechazado. Máximo 15 segundos.","phone_synced" to "Splash móvil guardado y sincronizado.","tablet_synced" to "Splash tablet guardado y sincronizado.","local_cloud_fail" to "Splash guardado localmente pero falló la sincronización cloud.","deleted" to "Splash eliminado.","discarded" to "Edición pendiente descartada."
    )
    val fr=en+mapOf(
        "skip" to "Passer","studio" to "Studio Vidéo Splash","studio_sub" to "Vidéos splash téléphone et tablette avec sélection automatique.",
        "master" to "RÉGLAGES PRINCIPAUX","enable" to "Activer la vidéo splash","enable_sub" to "Interrupteur principal","sound" to "Son de la vidéo","sound_sub" to "Lire le son de la vidéo splash",
        "skip_toggle" to "Afficher le bouton Passer","skip_sub" to "Permet de passer le splash","every" to "Afficher à chaque nouveau lancement","every_sub" to "Réafficher au démarrage de l'app",
        "phone" to "SPLASH TÉLÉPHONE","tablet" to "SPLASH TABLETTE","gallery" to "Galerie","replace" to "Remplacer","files" to "Fichiers","pending" to "APERÇU — NON ENREGISTRÉ","saved" to "SPLASH ENREGISTRÉ",
        "duration" to "Durée","max" to "15,00 s max","test" to "▶ Tester en plein écran","optimizing" to "Optimisation de la vidéo…","save" to "✓ Enregistrer / Accepter","cancel" to "Annuler","delete" to "Supprimer le splash",
        "auto" to "SÉLECTION AUTOMATIQUE","auto_desc" to "Les téléphones utilisent le splash téléphone. Les tablettes utilisent le splash tablette. Sans version tablette, la version téléphone est utilisée.","crop_desc" to "Les vidéos sont recadrées automatiquement sans déformation.","close_preview" to "Fermer l'aperçu","full_test" to "TEST SPLASH PLEIN ÉCRAN",
        "cloud_saved" to "Réglages splash enregistrés dans Supabase.","cloud_failed" to "Impossible d'enregistrer les réglages splash.","read_error" to "Impossible de lire la durée de cette vidéo. Choisis un autre fichier.","too_long" to "Vidéo refusée. Maximum 15 secondes.","phone_synced" to "Splash téléphone enregistré et synchronisé.","tablet_synced" to "Splash tablette enregistré et synchronisé.","local_cloud_fail" to "Splash enregistré localement mais synchronisation cloud échouée.","deleted" to "Splash supprimé.","discarded" to "Modification en attente annulée."
    )
    val de=en+mapOf("skip" to "Überspringen","studio" to "Splash-Video-Studio","master" to "SPLASH-HAUPTSTEUERUNG","enable" to "Splash-Video aktivieren","sound" to "Videoton","skip_toggle" to "Überspringen anzeigen","every" to "Bei jedem Neustart zeigen","phone" to "TELEFON-SPLASH","tablet" to "TABLET-SPLASH","gallery" to "Galerie","replace" to "Ersetzen","files" to "Dateien","save" to "✓ Splash speichern","cancel" to "Bearbeitung abbrechen","delete" to "Gespeicherten Splash löschen","close_preview" to "Vorschau schließen")
    val it=en+mapOf("skip" to "Salta","studio" to "Studio Video Splash","master" to "CONTROLLI SPLASH","enable" to "Attiva video splash","sound" to "Audio video","skip_toggle" to "Mostra Salta","every" to "Mostra a ogni nuovo avvio","phone" to "SPLASH TELEFONO","tablet" to "SPLASH TABLET","gallery" to "Galleria","replace" to "Sostituisci","files" to "File","save" to "✓ Salva splash","cancel" to "Annulla modifica","delete" to "Elimina splash","close_preview" to "Chiudi anteprima")
    val pl=en+mapOf("skip" to "Pomiń","studio" to "Studio Wideo Splash","master" to "GŁÓWNE USTAWIENIA","enable" to "Włącz wideo splash","sound" to "Dźwięk wideo","skip_toggle" to "Pokaż Pomiń","every" to "Pokazuj przy każdym nowym uruchomieniu","phone" to "SPLASH TELEFON","tablet" to "SPLASH TABLET","gallery" to "Galeria","replace" to "Zastąp","files" to "Pliki","save" to "✓ Zapisz splash","cancel" to "Anuluj edycję","delete" to "Usuń splash","close_preview" to "Zamknij podgląd")
    val tr=en+mapOf("skip" to "Atla","studio" to "Splash Video Stüdyosu","master" to "ANA SPLASH KONTROLLERİ","enable" to "Splash videoyu aç","sound" to "Video sesi","skip_toggle" to "Atla düğmesini göster","every" to "Her yeni açılışta göster","phone" to "TELEFON SPLASH","tablet" to "TABLET SPLASH","gallery" to "Galeri","replace" to "Değiştir","files" to "Dosyalar","save" to "✓ Splash kaydet","cancel" to "Düzenlemeyi iptal et","delete" to "Kayıtlı splashı sil","close_preview" to "Önizlemeyi kapat")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

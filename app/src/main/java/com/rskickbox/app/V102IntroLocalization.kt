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
    val de=en+mapOf(
        "skip" to "Überspringen","studio" to "Splash-Video-Studio","studio_sub" to "Separate Splash-Videos für Telefon und Tablet mit automatischer Geräteauswahl.",
        "master" to "SPLASH-HAUPTSTEUERUNG","enable" to "Splash-Video aktivieren","enable_sub" to "Hauptschalter ein/aus",
        "sound" to "Videoton","sound_sub" to "Originalton des Splash-Videos abspielen","skip_toggle" to "Überspringen anzeigen","skip_sub" to "Mitglieder können den Splash überspringen",
        "every" to "Bei jedem neuen App-Start zeigen","every_sub" to "Splash bei jedem frischen Start erneut anzeigen",
        "phone" to "TELEFON-SPLASHVIDEO","tablet" to "TABLET-SPLASHVIDEO","gallery" to "Galerie","replace" to "Ersetzen","files" to "Dateien",
        "pending" to "VORSCHAU — NOCH NICHT GESPEICHERT","saved" to "GESPEICHERTES SPLASHVIDEO","duration" to "Dauer","max" to "max. 15,00 Sek.",
        "test" to "▶ Splash im Vollbild testen","optimizing" to "Video wird für zuverlässige Wiedergabe optimiert…","save" to "✓ Splash speichern / übernehmen","cancel" to "Bearbeitung abbrechen","delete" to "Gespeicherten Splash löschen",
        "auto" to "AUTOMATISCHE GERÄTEAUSWAHL","auto_desc" to "Telefone verwenden den Telefon-Splash. Tablets verwenden den Tablet-Splash. Ohne Tablet-Version wird die Telefon-Version verwendet.",
        "crop_desc" to "Beide Videos werden automatisch mittig zugeschnitten, ohne verzerrt zu werden.","close_preview" to "Vorschau schließen","full_test" to "VOLLBILD-SPLASH-TEST",
        "cloud_saved" to "Splash-Einstellungen in Supabase gespeichert.","cloud_failed" to "Splash-Einstellungen konnten nicht gespeichert werden.",
        "phone_ready" to "Telefon-Splash-Vorschau bereit.","tablet_ready" to "Tablet-Splash-Vorschau bereit.","read_error" to "Videodauer konnte nicht gelesen werden. Wähle eine andere Datei.",
        "too_long" to "Video abgelehnt. Maximale Splash-Dauer: 15 Sekunden.","phone_synced" to "Telefon-Splash gespeichert und mit der Cloud synchronisiert.","tablet_synced" to "Tablet-Splash gespeichert und mit der Cloud synchronisiert.",
        "local_cloud_fail" to "Splash lokal gespeichert, aber Cloud-Synchronisierung fehlgeschlagen.","deleted" to "Splash gelöscht.","discarded" to "Ausstehende Splash-Bearbeitung verworfen."
    )
    val it=en+mapOf(
        "skip" to "Salta","studio" to "Studio Video Splash","studio_sub" to "Video splash separati per telefono e tablet con selezione automatica del dispositivo.",
        "master" to "CONTROLLI PRINCIPALI SPLASH","enable" to "Attiva video splash","enable_sub" to "Interruttore principale",
        "sound" to "Audio video","sound_sub" to "Riproduci l'audio originale del video splash","skip_toggle" to "Mostra Salta","skip_sub" to "Permette ai membri di saltare lo splash",
        "every" to "Mostra a ogni nuovo avvio","every_sub" to "Mostra di nuovo lo splash a ogni nuovo avvio dell'app",
        "phone" to "SPLASH TELEFONO","tablet" to "SPLASH TABLET","gallery" to "Galleria","replace" to "Sostituisci","files" to "File",
        "pending" to "ANTEPRIMA — NON ANCORA SALVATA","saved" to "VIDEO SPLASH SALVATO","duration" to "Durata","max" to "max 15,00 sec",
        "test" to "▶ Prova splash a schermo intero","optimizing" to "Ottimizzazione video per una riproduzione affidabile…","save" to "✓ Salva / Accetta splash","cancel" to "Annulla modifica","delete" to "Elimina splash salvato",
        "auto" to "SELEZIONE AUTOMATICA DISPOSITIVO","auto_desc" to "I telefoni usano lo splash telefono. I tablet usano lo splash tablet. Se manca la versione tablet, viene usata quella telefono.",
        "crop_desc" to "Entrambi i video vengono ritagliati al centro automaticamente senza deformazioni.","close_preview" to "Chiudi anteprima","full_test" to "TEST SPLASH A SCHERMO INTERO",
        "cloud_saved" to "Impostazioni splash salvate in Supabase.","cloud_failed" to "Impossibile salvare le impostazioni splash.",
        "phone_ready" to "Anteprima splash telefono pronta.","tablet_ready" to "Anteprima splash tablet pronta.","read_error" to "Impossibile leggere la durata del video. Scegli un altro file.",
        "too_long" to "Video rifiutato. Durata massima splash: 15 secondi.","phone_synced" to "Splash telefono salvato e sincronizzato nel cloud.","tablet_synced" to "Splash tablet salvato e sincronizzato nel cloud.",
        "local_cloud_fail" to "Splash salvato localmente ma sincronizzazione cloud non riuscita.","deleted" to "Splash eliminato.","discarded" to "Modifica splash in sospeso annullata."
    )
    val pl=en+mapOf(
        "skip" to "Pomiń","studio" to "Studio Wideo Splash","studio_sub" to "Osobne filmy splash dla telefonu i tabletu z automatycznym wyborem urządzenia.",
        "master" to "GŁÓWNE USTAWIENIA SPLASH","enable" to "Włącz wideo splash","enable_sub" to "Główny przełącznik",
        "sound" to "Dźwięk wideo","sound_sub" to "Odtwarzaj oryginalny dźwięk filmu splash","skip_toggle" to "Pokaż Pomiń","skip_sub" to "Pozwala użytkownikom pominąć splash",
        "every" to "Pokazuj przy każdym nowym uruchomieniu","every_sub" to "Pokazuj splash ponownie przy każdym świeżym starcie aplikacji",
        "phone" to "SPLASH TELEFON","tablet" to "SPLASH TABLET","gallery" to "Galeria","replace" to "Zastąp","files" to "Pliki",
        "pending" to "PODGLĄD — JESZCZE NIE ZAPISANO","saved" to "ZAPISANE WIDEO SPLASH","duration" to "Czas trwania","max" to "maks. 15,00 s",
        "test" to "▶ Testuj splash na pełnym ekranie","optimizing" to "Optymalizacja wideo dla niezawodnego odtwarzania…","save" to "✓ Zapisz / Akceptuj splash","cancel" to "Anuluj edycję","delete" to "Usuń zapisany splash",
        "auto" to "AUTOMATYCZNY WYBÓR URZĄDZENIA","auto_desc" to "Telefony używają wersji telefonicznej, a tablety wersji tabletowej. Jeśli nie ma wersji tabletowej, używana jest telefoniczna.",
        "crop_desc" to "Oba filmy są automatycznie kadrowane centralnie bez rozciągania.","close_preview" to "Zamknij podgląd","full_test" to "TEST SPLASH NA PEŁNYM EKRANIE",
        "cloud_saved" to "Ustawienia splash zapisane w Supabase.","cloud_failed" to "Nie udało się zapisać ustawień splash.",
        "phone_ready" to "Podgląd splash telefonu gotowy.","tablet_ready" to "Podgląd splash tabletu gotowy.","read_error" to "Nie udało się odczytać długości filmu. Wybierz inny plik.",
        "too_long" to "Film odrzucony. Maksymalna długość splash to 15 sekund.","phone_synced" to "Splash telefonu zapisany i zsynchronizowany z chmurą.","tablet_synced" to "Splash tabletu zapisany i zsynchronizowany z chmurą.",
        "local_cloud_fail" to "Splash zapisano lokalnie, ale synchronizacja z chmurą nie powiodła się.","deleted" to "Splash usunięty.","discarded" to "Odrzucono oczekującą edycję splash."
    )
    val tr=en+mapOf(
        "skip" to "Atla","studio" to "Splash Video Stüdyosu","studio_sub" to "Telefon ve tablet için otomatik cihaz seçimine sahip ayrı splash videoları.",
        "master" to "ANA SPLASH KONTROLLERİ","enable" to "Splash videoyu aç","enable_sub" to "Ana aç/kapat düğmesi",
        "sound" to "Video sesi","sound_sub" to "Splash videosunun kendi sesini oynat","skip_toggle" to "Atla düğmesini göster","skip_sub" to "Üyelerin splash ekranını atlamasına izin verir",
        "every" to "Her yeni açılışta göster","every_sub" to "Uygulama her yeni başladığında splash ekranını yeniden göster",
        "phone" to "TELEFON SPLASH","tablet" to "TABLET SPLASH","gallery" to "Galeri","replace" to "Değiştir","files" to "Dosyalar",
        "pending" to "ÖNİZLEME — HENÜZ KAYDEDİLMEDİ","saved" to "KAYDEDİLMİŞ SPLASH VİDEOSU","duration" to "Süre","max" to "en fazla 15,00 sn",
        "test" to "▶ Tam ekran splash testi","optimizing" to "Güvenilir oynatma için video optimize ediliyor…","save" to "✓ Splash kaydet / kabul et","cancel" to "Düzenlemeyi iptal et","delete" to "Kayıtlı splashı sil",
        "auto" to "OTOMATİK CİHAZ SEÇİMİ","auto_desc" to "Telefonlar telefon splashını, tabletler tablet splashını kullanır. Tablet sürümü yoksa telefon sürümü kullanılır.",
        "crop_desc" to "Her iki video da bozulmadan ekranı dolduracak şekilde otomatik ortadan kırpılır.","close_preview" to "Önizlemeyi kapat","full_test" to "TAM EKRAN SPLASH TESTİ",
        "cloud_saved" to "Splash ayarları Supabase'e kaydedildi.","cloud_failed" to "Splash ayarları kaydedilemedi.",
        "phone_ready" to "Telefon splash önizlemesi hazır.","tablet_ready" to "Tablet splash önizlemesi hazır.","read_error" to "Video süresi okunamadı. Başka bir dosya seç.",
        "too_long" to "Video reddedildi. Maksimum splash süresi 15 saniyedir.","phone_synced" to "Telefon splashı kaydedildi ve bulutla eşitlendi.","tablet_synced" to "Tablet splashı kaydedildi ve bulutla eşitlendi.",
        "local_cloud_fail" to "Splash yerel olarak kaydedildi ancak bulut eşitlemesi başarısız oldu.","deleted" to "Splash silindi.","discarded" to "Bekleyen splash düzenlemesi iptal edildi."
    )
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

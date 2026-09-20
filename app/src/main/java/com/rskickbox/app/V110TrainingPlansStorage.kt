package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RsTrainingTemplateV110(
    val id:String,
    val title:String,
    val description:String,
    val category:String,
    val active:Boolean,
    @SerialName("created_at") val createdAt:String
)

@Serializable
data class RsTrainingTemplateStepV110(
    val id:String,
    @SerialName("template_id") val templateId:String,
    @SerialName("step_order") val stepOrder:Int,
    val title:String,
    val instructions:String,
    @SerialName("sets_reps_time") val setsRepsTime:String,
    @SerialName("media_id") val mediaId:String?=null,
    @SerialName("media_title") val mediaTitle:String?=null,
    @SerialName("media_kind") val mediaKind:String?=null
)

@Serializable
data class RsHomeworkStepV110(
    val id:String,
    @SerialName("homework_id") val homeworkId:String,
    @SerialName("step_order") val stepOrder:Int,
    val title:String,
    val instructions:String,
    @SerialName("sets_reps_time") val setsRepsTime:String,
    @SerialName("media_id") val mediaId:String?=null,
    @SerialName("media_title") val mediaTitle:String?=null,
    @SerialName("media_kind") val mediaKind:String?=null,
    val completed:Boolean
)

@Serializable
data class RsStudentStorageUsageV110(
    @SerialName("student_id") val studentId:String,
    val email:String,
    @SerialName("display_name") val displayName:String,
    @SerialName("membership_plan") val membershipPlan:String="PRO",
    @SerialName("used_bytes") val usedBytes:Long,
    @SerialName("limit_bytes") val limitBytes:Long,
    @SerialName("asset_count") val assetCount:Long,
    @SerialName("warning_message") val warningMessage:String,
    @SerialName("custom_limit") val customLimit:Boolean=false
)

@Serializable
data class RsStorageQuotaSettingsV112(
    @SerialName("backend_tier") val backendTier:String,
    @SerialName("free_basic") val freeBasic:Long,
    @SerialName("free_pro") val freePro:Long,
    @SerialName("free_elite") val freeElite:Long,
    @SerialName("pro_basic") val proBasic:Long,
    @SerialName("pro_pro") val proPro:Long,
    @SerialName("pro_elite") val proElite:Long
)

suspend fun rsTrainingTemplatesV110():Result<List<RsTrainingTemplateV110>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc("rs_training_templates_feed").decodeList<RsTrainingTemplateV110>()
}

suspend fun rsTrainingTemplateStepsV110(templateId:String):Result<List<RsTrainingTemplateStepV110>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_training_template_steps_feed",
        buildJsonObject{ put("p_template_id",templateId) }
    ).decodeList<RsTrainingTemplateStepV110>()
}

suspend fun rsCreateTrainingTemplateV110(title:String,description:String,category:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_create_training_template",
        buildJsonObject{
            put("p_title",title.trim())
            put("p_description",description.trim())
            put("p_category",category.trim().ifBlank{"GENERAL"})
        }
    )
    Unit
}

suspend fun rsAddTrainingTemplateStepV110(
    templateId:String,
    order:Int,
    title:String,
    instructions:String,
    setsRepsTime:String,
    mediaId:String?
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_add_training_template_step",
        buildJsonObject{
            put("p_template_id",templateId)
            put("p_order",order)
            put("p_title",title.trim())
            put("p_instructions",instructions.trim())
            put("p_sets_reps_time",setsRepsTime.trim())
            if(mediaId.isNullOrBlank()) put("p_media_id",null as String?) else put("p_media_id",mediaId)
        }
    )
    Unit
}

suspend fun rsAssignTrainingTemplateV110(studentId:String,templateId:String,due:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_assign_training_template",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_template_id",templateId)
            put("p_due_label",due.trim())
        }
    )
    Unit
}

suspend fun rsHomeworkStepsV110(homeworkId:String):Result<List<RsHomeworkStepV110>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_homework_steps_feed",
        buildJsonObject{ put("p_homework_id",homeworkId) }
    ).decodeList<RsHomeworkStepV110>()
}

suspend fun rsSetHomeworkStepCompletedV110(stepId:String,completed:Boolean):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_set_homework_step_completed",
        buildJsonObject{
            put("p_step_id",stepId)
            put("p_completed",completed)
        }
    )
    Unit
}

suspend fun rsStudentStorageUsageV110():Result<List<RsStudentStorageUsageV110>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_student_storage_usage").decodeList<RsStudentStorageUsageV110>()
}

suspend fun rsSetStudentStorageLimitV110(studentId:String,limitBytes:Long,warning:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_student_storage_limit",
        buildJsonObject{
            put("p_student_id",studentId)
            put("p_limit_bytes",limitBytes)
            put("p_warning_message",warning.trim())
        }
    )
    Unit
}

suspend fun rsStorageQuotaSettingsV112():Result<RsStorageQuotaSettingsV112> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_storage_quota_settings").decodeList<RsStorageQuotaSettingsV112>().first()
}

suspend fun rsSetStorageBackendTierV112(tier:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("Cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_set_storage_backend_tier",
        buildJsonObject{ put("p_backend_tier",tier) }
    )
    Unit
}

private fun rsTrainingPlanT110(lang:RsLang,key:String):String{
    val en=mapOf(
        "templates" to "Reusable Training Plans",
        "templates_sub" to "Build once, then assign the same guided plan to different students without uploading media again.",
        "new_plan" to "NEW TRAINING PLAN","plan_title" to "Plan title","description" to "Description","category" to "Category",
        "create" to "Create plan","select_plan" to "SELECT PLAN","step" to "ADD EXERCISE STEP","step_title" to "Exercise title",
        "instructions" to "Instructions","sets" to "Sets / reps / time","media" to "Instruction media","no_media" to "No media",
        "add_step" to "Add step","assign" to "Assign selected plan","due" to "Due","no_plans" to "No training plans yet.",
        "storage" to "Student Storage","storage_sub" to "Trainer-only soft limits. Shared trainer instruction media is stored once and is not charged to every student.",
        "used" to "used","files" to "files","limit" to "soft limit","save_limit" to "Save limit",
        "backend_tier" to "Supabase storage profile","free_tier" to "Free","pro_tier" to "Pro",
        "quota_note" to "Default space follows both the Supabase tier and the student's BASIC / PRO / ELITE membership. Individual overrides remain possible.",
        "inherited" to "plan default","custom" to "custom override",
        "media_reuse" to "Media is referenced from Training Media, not duplicated.",
        "steps" to "TRAINING STEPS","done" to "Done","mark_done" to "Mark done","reopen" to "Reopen",
        "open_example" to "Open example","close_example" to "Close example","loading_example" to "Loading protected example…",
        "backend_unavailable" to "Cloud training data is unavailable. Check the connection or trainer permissions."
    )
    val nl=en+mapOf(
        "templates" to "Herbruikbare Trainingsplannen","templates_sub" to "Bouw één keer en wijs hetzelfde begeleide plan toe zonder media opnieuw te uploaden.",
        "new_plan" to "NIEUW TRAININGSPLAN","plan_title" to "Plantitel","description" to "Beschrijving","category" to "Categorie","create" to "Plan maken",
        "select_plan" to "KIES PLAN","step" to "OEFENING TOEVOEGEN","step_title" to "Titel oefening","instructions" to "Instructies","sets" to "Sets / herhalingen / tijd",
        "media" to "Instructiemedia","no_media" to "Geen media","add_step" to "Stap toevoegen","assign" to "Geselecteerd plan toewijzen","due" to "Deadline",
        "no_plans" to "Nog geen trainingsplannen.","storage" to "Opslag per leerling","storage_sub" to "Alleen voor trainer. Gedeelde instructiemedia wordt één keer opgeslagen.",
        "used" to "gebruikt","files" to "bestanden","limit" to "zachte limiet","save_limit" to "Limiet opslaan",
        "backend_tier" to "Supabase-opslagprofiel","free_tier" to "Gratis","pro_tier" to "Pro",
        "quota_note" to "Standaardruimte volgt zowel het Supabase-pakket als BASIC / PRO / ELITE. Individuele limieten blijven mogelijk.",
        "inherited" to "standaard van pakket","custom" to "aangepaste limiet","media_reuse" to "Media wordt gekoppeld vanuit Trainingsmedia, niet gedupliceerd.",
        "steps" to "TRAININGSSTAPPEN","done" to "Klaar","mark_done" to "Markeer klaar","reopen" to "Heropenen",
        "open_example" to "Voorbeeld openen","close_example" to "Voorbeeld sluiten","loading_example" to "Beveiligd voorbeeld laden…"
    )
    val pt=en+mapOf(
        "templates" to "Planos de Treino Reutilizáveis","templates_sub" to "Cria uma vez e atribui o mesmo plano guiado a vários alunos sem voltar a carregar a media.",
        "new_plan" to "NOVO PLANO","plan_title" to "Título do plano","description" to "Descrição","category" to "Categoria","create" to "Criar plano",
        "select_plan" to "ESCOLHER PLANO","step" to "ADICIONAR EXERCÍCIO","step_title" to "Título do exercício","instructions" to "Instruções","sets" to "Séries / repetições / tempo",
        "media" to "Media de instrução","no_media" to "Sem media","add_step" to "Adicionar passo","assign" to "Atribuir plano selecionado","due" to "Prazo",
        "no_plans" to "Ainda não existem planos.","storage" to "Armazenamento por Aluno","storage_sub" to "Limites internos apenas para o treinador. Media partilhada é guardada só uma vez.",
        "used" to "usado","files" to "ficheiros","limit" to "limite flexível","save_limit" to "Guardar limite",
        "backend_tier" to "Perfil de armazenamento Supabase","free_tier" to "Grátis","pro_tier" to "Pro",
        "quota_note" to "O espaço padrão depende do plano Supabase e do nível BASIC / PRO / ELITE. Limites individuais continuam possíveis.",
        "inherited" to "padrão do plano","custom" to "limite personalizado","media_reuse" to "A media é referenciada da Media de Treino, sem duplicação.",
        "steps" to "PASSOS DO TREINO","done" to "Concluído","mark_done" to "Marcar concluído","reopen" to "Reabrir",
        "open_example" to "Abrir exemplo","close_example" to "Fechar exemplo","loading_example" to "A carregar exemplo protegido…"
    )
    val es=en+mapOf("templates" to "Planes de Entrenamiento Reutilizables","new_plan" to "NUEVO PLAN","plan_title" to "Título del plan","description" to "Descripción","category" to "Categoría","create" to "Crear plan","step" to "AÑADIR EJERCICIO","step_title" to "Título del ejercicio","instructions" to "Instrucciones","sets" to "Series / repeticiones / tiempo","media" to "Media de instrucción","no_media" to "Sin media","add_step" to "Añadir paso","assign" to "Asignar plan seleccionado","due" to "Fecha límite","storage" to "Almacenamiento del Alumno","used" to "usado","files" to "archivos","limit" to "límite flexible","save_limit" to "Guardar límite","steps" to "PASOS DE ENTRENAMIENTO","done" to "Hecho","mark_done" to "Marcar hecho","reopen" to "Reabrir")
    val fr=en+mapOf("templates" to "Plans d’Entraînement Réutilisables","new_plan" to "NOUVEAU PLAN","plan_title" to "Titre du plan","description" to "Description","category" to "Catégorie","create" to "Créer le plan","step" to "AJOUTER UN EXERCICE","step_title" to "Titre de l’exercice","instructions" to "Instructions","sets" to "Séries / répétitions / temps","media" to "Média d’instruction","no_media" to "Aucun média","add_step" to "Ajouter l’étape","assign" to "Attribuer le plan","due" to "Échéance","storage" to "Stockage Élève","used" to "utilisé","files" to "fichiers","limit" to "limite souple","save_limit" to "Enregistrer la limite","steps" to "ÉTAPES D’ENTRAÎNEMENT","done" to "Terminé","mark_done" to "Marquer terminé","reopen" to "Rouvrir")
    val de=en+mapOf("templates" to "Wiederverwendbare Trainingspläne","new_plan" to "NEUER PLAN","plan_title" to "Plantitel","description" to "Beschreibung","category" to "Kategorie","create" to "Plan erstellen","step" to "ÜBUNG HINZUFÜGEN","step_title" to "Übungstitel","instructions" to "Anweisungen","sets" to "Sätze / Wiederholungen / Zeit","media" to "Anleitungsmedium","no_media" to "Kein Medium","add_step" to "Schritt hinzufügen","assign" to "Plan zuweisen","due" to "Fällig","storage" to "Schülerspeicher","used" to "verwendet","files" to "Dateien","limit" to "Soft-Limit","save_limit" to "Limit speichern","steps" to "TRAININGSSCHRITTE","done" to "Erledigt","mark_done" to "Erledigt markieren","reopen" to "Wieder öffnen")
    val it=en+mapOf("templates" to "Piani di Allenamento Riutilizzabili","new_plan" to "NUOVO PIANO","plan_title" to "Titolo piano","description" to "Descrizione","category" to "Categoria","create" to "Crea piano","step" to "AGGIUNGI ESERCIZIO","step_title" to "Titolo esercizio","instructions" to "Istruzioni","sets" to "Serie / ripetizioni / tempo","media" to "Media istruttivo","no_media" to "Nessun media","add_step" to "Aggiungi passo","assign" to "Assegna piano","due" to "Scadenza","storage" to "Spazio Allievo","used" to "usato","files" to "file","limit" to "limite flessibile","save_limit" to "Salva limite","steps" to "PASSI ALLENAMENTO","done" to "Fatto","mark_done" to "Segna fatto","reopen" to "Riapri")
    val pl=en+mapOf("templates" to "Wielorazowe Plany Treningowe","new_plan" to "NOWY PLAN","plan_title" to "Tytuł planu","description" to "Opis","category" to "Kategoria","create" to "Utwórz plan","step" to "DODAJ ĆWICZENIE","step_title" to "Tytuł ćwiczenia","instructions" to "Instrukcje","sets" to "Serie / powtórzenia / czas","media" to "Media instruktażowe","no_media" to "Brak mediów","add_step" to "Dodaj krok","assign" to "Przypisz plan","due" to "Termin","storage" to "Pamięć Ucznia","used" to "użyto","files" to "plików","limit" to "miękki limit","save_limit" to "Zapisz limit","steps" to "KROKI TRENINGU","done" to "Gotowe","mark_done" to "Oznacz gotowe","reopen" to "Otwórz ponownie")
    val tr=en+mapOf("templates" to "Yeniden Kullanılabilir Antrenman Planları","new_plan" to "YENİ PLAN","plan_title" to "Plan başlığı","description" to "Açıklama","category" to "Kategori","create" to "Plan oluştur","step" to "EGZERSİZ EKLE","step_title" to "Egzersiz başlığı","instructions" to "Talimatlar","sets" to "Set / tekrar / süre","media" to "Eğitim medyası","no_media" to "Medya yok","add_step" to "Adım ekle","assign" to "Planı ata","due" to "Son tarih","storage" to "Öğrenci Depolaması","used" to "kullanıldı","files" to "dosya","limit" to "esnek limit","save_limit" to "Limiti kaydet","steps" to "ANTRENMAN ADIMLARI","done" to "Tamam","mark_done" to "Tamamlandı işaretle","reopen" to "Yeniden aç")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    val extra=mapOf(
        "nl" to mapOf("backend_unavailable" to "Cloud-trainingsgegevens zijn niet beschikbaar. Controleer de verbinding of trainerrechten."),
        "pt" to mapOf("backend_unavailable" to "Os dados de treino na cloud não estão disponíveis. Verifica a ligação ou as permissões do treinador."),
        "es" to mapOf(
            "templates_sub" to "Créalo una vez y asigna el mismo plan guiado a distintos alumnos sin volver a subir media.","select_plan" to "ELEGIR PLAN","no_plans" to "Todavía no hay planes de entrenamiento.",
            "storage_sub" to "Límites internos del entrenador. La media de instrucción compartida se guarda una sola vez y no se cobra a cada alumno.","backend_tier" to "Perfil de almacenamiento Supabase","free_tier" to "Gratis","pro_tier" to "Pro",
            "quota_note" to "El espacio predeterminado depende del plan Supabase y de la membresía BASIC / PRO / ELITE. Los límites individuales siguen disponibles.","inherited" to "valor del plan","custom" to "límite personalizado",
            "media_reuse" to "La media se referencia desde Media de Entrenamiento, sin duplicarla.","open_example" to "Abrir ejemplo","close_example" to "Cerrar ejemplo","loading_example" to "Cargando ejemplo protegido…",
            "backend_unavailable" to "Los datos de entrenamiento en la nube no están disponibles. Comprueba la conexión o los permisos del entrenador."
        ),
        "fr" to mapOf(
            "templates_sub" to "Crée-le une fois puis attribue le même plan guidé à plusieurs élèves sans réimporter les médias.","select_plan" to "CHOISIR UN PLAN","no_plans" to "Aucun plan d’entraînement pour le moment.",
            "storage_sub" to "Limites internes du coach. Les médias d’instruction partagés sont stockés une seule fois et ne sont pas facturés à chaque élève.","backend_tier" to "Profil de stockage Supabase","free_tier" to "Gratuit","pro_tier" to "Pro",
            "quota_note" to "L’espace par défaut dépend du forfait Supabase et de l’adhésion BASIC / PRO / ELITE. Les limites individuelles restent possibles.","inherited" to "valeur du forfait","custom" to "limite personnalisée",
            "media_reuse" to "Les médias sont référencés depuis les Médias d’Entraînement, sans duplication.","open_example" to "Ouvrir l’exemple","close_example" to "Fermer l’exemple","loading_example" to "Chargement de l’exemple protégé…",
            "backend_unavailable" to "Les données d’entraînement cloud sont indisponibles. Vérifie la connexion ou les autorisations de l’entraîneur."
        ),
        "de" to mapOf(
            "templates_sub" to "Einmal erstellen und denselben geführten Plan mehreren Schülern zuweisen, ohne Medien erneut hochzuladen.","select_plan" to "PLAN AUSWÄHLEN","no_plans" to "Noch keine Trainingspläne vorhanden.",
            "storage_sub" to "Interne Trainerlimits. Geteilte Anleitungsmedien werden einmal gespeichert und nicht jedem Schüler berechnet.","backend_tier" to "Supabase-Speicherprofil","free_tier" to "Kostenlos","pro_tier" to "Pro",
            "quota_note" to "Der Standardspeicher richtet sich nach Supabase-Tarif und BASIC / PRO / ELITE. Individuelle Limits bleiben möglich.","inherited" to "Planstandard","custom" to "eigenes Limit",
            "media_reuse" to "Medien werden aus Trainingsmedien referenziert und nicht dupliziert.","open_example" to "Beispiel öffnen","close_example" to "Beispiel schließen","loading_example" to "Geschütztes Beispiel wird geladen…",
            "backend_unavailable" to "Cloud-Trainingsdaten sind nicht verfügbar. Prüfe Verbindung oder Trainerberechtigungen."
        ),
        "it" to mapOf(
            "templates_sub" to "Crealo una volta e assegna lo stesso piano guidato a più allievi senza ricaricare i media.","select_plan" to "SCEGLI PIANO","no_plans" to "Nessun piano di allenamento disponibile.",
            "storage_sub" to "Limiti interni del trainer. I media didattici condivisi vengono salvati una sola volta e non conteggiati per ogni allievo.","backend_tier" to "Profilo di archiviazione Supabase","free_tier" to "Gratis","pro_tier" to "Pro",
            "quota_note" to "Lo spazio predefinito dipende dal piano Supabase e dall’abbonamento BASIC / PRO / ELITE. Restano possibili limiti individuali.","inherited" to "valore del piano","custom" to "limite personalizzato",
            "media_reuse" to "I media vengono collegati dalla Media di Allenamento, senza duplicazione.","open_example" to "Apri esempio","close_example" to "Chiudi esempio","loading_example" to "Caricamento esempio protetto…",
            "backend_unavailable" to "I dati di allenamento cloud non sono disponibili. Controlla la connessione o i permessi dell’allenatore."
        ),
        "pl" to mapOf(
            "templates_sub" to "Utwórz raz, a potem przypisuj ten sam plan różnym uczniom bez ponownego przesyłania mediów.","select_plan" to "WYBIERZ PLAN","no_plans" to "Brak planów treningowych.",
            "storage_sub" to "Wewnętrzne limity trenera. Wspólne media instruktażowe są przechowywane raz i nie są naliczane każdemu uczniowi.","backend_tier" to "Profil pamięci Supabase","free_tier" to "Darmowy","pro_tier" to "Pro",
            "quota_note" to "Domyślna przestrzeń zależy od planu Supabase i członkostwa BASIC / PRO / ELITE. Indywidualne limity nadal są możliwe.","inherited" to "domyślny plan","custom" to "własny limit",
            "media_reuse" to "Media są odwoływane z Mediów Treningowych, bez duplikowania.","open_example" to "Otwórz przykład","close_example" to "Zamknij przykład","loading_example" to "Ładowanie chronionego przykładu…",
            "backend_unavailable" to "Dane treningowe w chmurze są niedostępne. Sprawdź połączenie lub uprawnienia trenera."
        ),
        "tr" to mapOf(
            "templates_sub" to "Bir kez oluştur, medyayı tekrar yüklemeden aynı rehberli planı farklı öğrencilere ata.","select_plan" to "PLAN SEÇ","no_plans" to "Henüz antrenman planı yok.",
            "storage_sub" to "Antrenöre özel dahili limitler. Paylaşılan eğitim medyası bir kez saklanır ve her öğrenciye ayrı yazılmaz.","backend_tier" to "Supabase depolama profili","free_tier" to "Ücretsiz","pro_tier" to "Pro",
            "quota_note" to "Varsayılan alan Supabase paketi ile BASIC / PRO / ELITE üyeliğine göre belirlenir. Bireysel limitler kullanılabilir.","inherited" to "plan varsayılanı","custom" to "özel limit",
            "media_reuse" to "Medya, Antrenman Medyasından referanslanır; kopyalanmaz.","open_example" to "Örneği aç","close_example" to "Örneği kapat","loading_example" to "Korumalı örnek yükleniyor…",
            "backend_unavailable" to "Bulut antrenman verileri kullanılamıyor. Bağlantıyı veya antrenör izinlerini kontrol et."
        )
    )[lang.code]
    return extra?.get(key)?:pack[key]?:en[key]?:key
}

private fun rsMbV110(bytes:Long):String = String.format(java.util.Locale.US,"%.1f MB",bytes/1048576.0)

@Composable
fun RsTrainingPlanManagerPanelV110(
    c:RsPalette,
    lang:RsLang,
    studentId:String,
    onAssigned:()->Unit
){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var templates by remember{mutableStateOf<List<RsTrainingTemplateV110>>(emptyList())}
    var media by remember{mutableStateOf<List<RsTrainingMediaItemV55>>(emptyList())}
    var selectedTemplateId by remember{mutableStateOf("")}
    var selectedSteps by remember{mutableStateOf<List<RsTrainingTemplateStepV110>>(emptyList())}
    var title by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("GENERAL")}
    var stepTitle by remember{mutableStateOf("")}
    var instructions by remember{mutableStateOf("")}
    var sets by remember{mutableStateOf("")}
    var selectedMediaId by remember{mutableStateOf("")}
    var due by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}

    LaunchedEffect(revision){
        rsTrainingTemplatesV110().onSuccess{
            templates=it
            if(selectedTemplateId.isBlank())selectedTemplateId=it.firstOrNull()?.id.orEmpty()
        }.onFailure{
            status=rsTrainingPlanT110(lang,"backend_unavailable")
        }
        rsCloudTrainingMediaV73().onSuccess{media=it.filter{m->m.published}}
    }
    LaunchedEffect(selectedTemplateId,revision){
        if(selectedTemplateId.isBlank())selectedSteps=emptyList()
        else rsTrainingTemplateStepsV110(selectedTemplateId).onSuccess{selectedSteps=it}
    }

    RsPanel(c){
        Text(rsTrainingPlanT110(lang,"templates"),color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
        Text(rsTrainingPlanT110(lang,"templates_sub"),color=c.muted,fontSize=10.sp)
        Text(rsTrainingPlanT110(lang,"media_reuse"),color=c.muted,fontSize=10.sp)
        if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)

        Text(rsTrainingPlanT110(lang,"new_plan"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
        OutlinedTextField(title,{title=it.take(120)},label={Text(rsTrainingPlanT110(lang,"plan_title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
        OutlinedTextField(description,{description=it.take(1200)},label={Text(rsTrainingPlanT110(lang,"description"))},modifier=Modifier.fillMaxWidth(),minLines=2,enabled=!busy)
        OutlinedTextField(category,{category=it.take(50)},label={Text(rsTrainingPlanT110(lang,"category"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
        Button(
            onClick={
                busy=true
                scope.launch{
                    rsCreateTrainingTemplateV110(title,description,category)
                        .onSuccess{title="";description="";category="GENERAL";revision++;status=""}
                        .onFailure{status=it.message.orEmpty()}
                    busy=false
                }
            },
            enabled=!busy&&title.isNotBlank(),
            modifier=Modifier.fillMaxWidth()
        ){Text(rsTrainingPlanT110(lang,"create"))}

        if(templates.isEmpty()){
            Text(rsTrainingPlanT110(lang,"no_plans"),color=c.muted)
        }else{
            Text(rsTrainingPlanT110(lang,"select_plan"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
            templates.forEach{t->
                FilterChip(
                    selected=selectedTemplateId==t.id,
                    onClick={selectedTemplateId=t.id},
                    label={Text(t.title)},
                    modifier=Modifier.fillMaxWidth()
                )
            }

            if(selectedTemplateId.isNotBlank()){
                Text(rsTrainingPlanT110(lang,"step"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
                OutlinedTextField(stepTitle,{stepTitle=it.take(120)},label={Text(rsTrainingPlanT110(lang,"step_title"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
                OutlinedTextField(instructions,{instructions=it.take(1200)},label={Text(rsTrainingPlanT110(lang,"instructions"))},modifier=Modifier.fillMaxWidth(),minLines=2,enabled=!busy)
                OutlinedTextField(sets,{sets=it.take(120)},label={Text(rsTrainingPlanT110(lang,"sets"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
                Text(rsTrainingPlanT110(lang,"media"),color=c.muted,fontSize=10.sp)
                FilterChip(selected=selectedMediaId.isBlank(),onClick={selectedMediaId=""},label={Text(rsTrainingPlanT110(lang,"no_media"))},modifier=Modifier.fillMaxWidth())
                media.take(30).forEach{m->
                    FilterChip(
                        selected=selectedMediaId==m.id,
                        onClick={selectedMediaId=m.id},
                        label={Text(m.title+" · "+m.kind)},
                        modifier=Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick={
                        busy=true
                        scope.launch{
                            rsAddTrainingTemplateStepV110(selectedTemplateId,selectedSteps.size+1,stepTitle,instructions,sets,selectedMediaId.ifBlank{null})
                                .onSuccess{stepTitle="";instructions="";sets="";selectedMediaId="";revision++;status=""}
                                .onFailure{status=it.message.orEmpty()}
                            busy=false
                        }
                    },
                    enabled=!busy&&stepTitle.isNotBlank(),
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsTrainingPlanT110(lang,"add_step"))}

                selectedSteps.forEach{s->
                    Surface(color=c.gold.copy(alpha=.10f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(s.stepOrder.toString()+". "+s.title,color=c.bright,fontWeight=FontWeight.Bold)
                            if(s.setsRepsTime.isNotBlank())Text(s.setsRepsTime,color=c.muted,fontSize=10.sp)
                            if(s.instructions.isNotBlank())Text(s.instructions,color=c.text,fontSize=11.sp)
                            if(!s.mediaTitle.isNullOrBlank())Text("▶ "+s.mediaTitle,color=c.bright,fontSize=10.sp)
                        }
                    }
                }

                OutlinedTextField(due,{due=it.take(40)},label={Text(rsTrainingPlanT110(lang,"due"))},modifier=Modifier.fillMaxWidth(),enabled=!busy)
                Button(
                    onClick={
                        busy=true
                        scope.launch{
                            rsAssignTrainingTemplateV110(studentId,selectedTemplateId,due)
                                .onSuccess{due="";status="✓ "+rsTrainingPlanT110(lang,"assign");onAssigned()}
                                .onFailure{status=it.message.orEmpty()}
                            busy=false
                        }
                    },
                    enabled=!busy&&studentId.isNotBlank()&&selectedSteps.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsTrainingPlanT110(lang,"assign"))}
            }
        }
    }
}

@Composable
fun RsHomeworkStepsPanelV110(c:RsPalette,lang:RsLang,homeworkId:String){
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    var revision by remember{mutableIntStateOf(0)}
    var steps by remember{mutableStateOf<List<RsHomeworkStepV110>>(emptyList())}
    var mediaCatalog by remember{mutableStateOf<List<RsTrainingMediaItemV55>>(emptyList())}
    var supported by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var openMediaId by remember{mutableStateOf<String?>(null)}
    var openMediaLocalUri by remember{mutableStateOf("")}
    var mediaLoading by remember{mutableStateOf(false)}

    LaunchedEffect(homeworkId,revision){
        rsHomeworkStepsV110(homeworkId)
            .onSuccess{steps=it;supported=true}
            .onFailure{supported=false}
        if(mediaCatalog.isEmpty()){
            rsCloudTrainingMediaV73().onSuccess{mediaCatalog=it}
        }
    }
    if(!supported || steps.isEmpty())return

    Text(rsTrainingPlanT110(lang,"steps"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
    steps.forEach{s->
        Surface(color=c.gold.copy(alpha=.08f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(s.stepOrder.toString()+". "+s.title,color=c.bright,fontWeight=FontWeight.Bold)
                if(s.setsRepsTime.isNotBlank())Text(s.setsRepsTime,color=c.muted,fontSize=10.sp)
                if(s.instructions.isNotBlank())Text(s.instructions,color=c.text)
                if(!s.mediaTitle.isNullOrBlank()){
                    Text((if(s.mediaKind=="VIDEO")"▶ " else "▣ ")+s.mediaTitle,color=c.bright,fontSize=10.sp)
                    val source=mediaCatalog.firstOrNull{it.id==s.mediaId}
                    if(openMediaId==s.mediaId){
                        if(mediaLoading){
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                CircularProgressIndicator(modifier=Modifier.size(20.dp),strokeWidth=2.dp)
                                Text(rsTrainingPlanT110(lang,"loading_example"),color=c.muted,fontSize=10.sp)
                            }
                        }else if(source!=null&&openMediaLocalUri.isNotBlank()){
                            RsTrainingMediaPreviewV55(
                                c,
                                source.copy(uri=openMediaLocalUri),
                                Modifier.fillMaxWidth().heightIn(min=220.dp,max=420.dp)
                            )
                        }
                        OutlinedButton(
                            onClick={openMediaId=null;openMediaLocalUri="";mediaLoading=false},
                            modifier=Modifier.fillMaxWidth()
                        ){Text(rsTrainingPlanT110(lang,"close_example"))}
                    }else if(source!=null){
                        OutlinedButton(
                            onClick={
                                openMediaId=s.mediaId
                                openMediaLocalUri=""
                                mediaLoading=true
                                scope.launch{
                                    rsCloudTrainingMediaLocalUriV73(context,source)
                                        .onSuccess{openMediaLocalUri=it}
                                        .onFailure{openMediaId=null}
                                    mediaLoading=false
                                }
                            },
                            enabled=!mediaLoading,
                            modifier=Modifier.fillMaxWidth()
                        ){Text(rsTrainingPlanT110(lang,"open_example"))}
                    }
                }
                OutlinedButton(
                    onClick={
                        busyId=s.id
                        scope.launch{
                            rsSetHomeworkStepCompletedV110(s.id,!s.completed).onSuccess{revision++}
                            busyId=null
                        }
                    },
                    enabled=busyId==null,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(s.completed)rsTrainingPlanT110(lang,"reopen") else rsTrainingPlanT110(lang,"mark_done"))}
            }
        }
    }
}

@Composable
fun RsStudentStoragePanelV110(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var rows by remember{mutableStateOf<List<RsStudentStorageUsageV110>>(emptyList())}
    var quota by remember{mutableStateOf<RsStorageQuotaSettingsV112?>(null)}
    var supported by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        rsStudentStorageUsageV110()
            .onSuccess{rows=it;supported=true}
            .onFailure{supported=false}
        rsStorageQuotaSettingsV112().onSuccess{quota=it}
    }
    if(!supported)return

    RsPanel(c){
        Text(rsTrainingPlanT110(lang,"storage"),color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
        Text(rsTrainingPlanT110(lang,"storage_sub"),color=c.muted,fontSize=10.sp)
        quota?.let{q->
            Text(rsTrainingPlanT110(lang,"backend_tier"),color=c.bright,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf("FREE","PRO").forEach{tier->
                    FilterChip(
                        selected=q.backendTier==tier,
                        onClick={
                            busyId="tier"
                            scope.launch{
                                rsSetStorageBackendTierV112(tier).onSuccess{revision++}
                                busyId=null
                            }
                        },
                        label={Text(if(tier=="FREE")rsTrainingPlanT110(lang,"free_tier") else rsTrainingPlanT110(lang,"pro_tier"))},
                        enabled=busyId==null,
                        modifier=Modifier.weight(1f)
                    )
                }
            }
            val b=if(q.backendTier=="PRO")q.proBasic else q.freeBasic
            val p=if(q.backendTier=="PRO")q.proPro else q.freePro
            val e=if(q.backendTier=="PRO")q.proElite else q.freeElite
            Text("BASIC "+rsMbV110(b)+" · PRO "+rsMbV110(p)+" · ELITE "+rsMbV110(e),color=c.muted,fontSize=10.sp)
            Text(rsTrainingPlanT110(lang,"quota_note"),color=c.muted,fontSize=10.sp)
            HorizontalDivider()
        }
        rows.forEach{row->
            val fraction=if(row.limitBytes<=0L)0f else (row.usedBytes.toDouble()/row.limitBytes.toDouble()).coerceIn(0.0,1.0).toFloat()
            var limitMb by remember(row.studentId,row.limitBytes){mutableStateOf((row.limitBytes/1048576L).toString())}
            Text(row.displayName.ifBlank{row.email}+" · "+row.membershipPlan,color=c.bright,fontWeight=FontWeight.Bold)
            Text(rsMbV110(row.usedBytes)+" "+rsTrainingPlanT110(lang,"used")+" · "+row.assetCount+" "+rsTrainingPlanT110(lang,"files")+" · "+rsMbV110(row.limitBytes)+" "+rsTrainingPlanT110(lang,"limit")+" · "+rsTrainingPlanT110(lang,if(row.customLimit)"custom" else "inherited"),color=c.muted,fontSize=10.sp)
            LinearProgressIndicator(progress={fraction},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(
                limitMb,
                {limitMb=it.filter(Char::isDigit).take(5)},
                label={Text(rsTrainingPlanT110(lang,"limit")+" MB")},
                modifier=Modifier.fillMaxWidth(),
                enabled=busyId==null
            )
            OutlinedButton(
                onClick={
                    val mb=limitMb.toLongOrNull()?.coerceAtLeast(1L)?:50L
                    busyId=row.studentId
                    scope.launch{
                        rsSetStudentStorageLimitV110(row.studentId,mb*1048576L,row.warningMessage).onSuccess{revision++}
                        busyId=null
                    }
                },
                enabled=busyId==null,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsTrainingPlanT110(lang,"save_limit"))}
            RsStudentStorageCleanupV111(c,lang,row.studentId)
            HorizontalDivider()
        }
    }
}

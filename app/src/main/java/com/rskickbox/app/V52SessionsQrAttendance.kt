package com.rskickbox.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RsSessionBlockV52(
    val id:String,val title:String,val seconds:Int,val instructions:String
)

data class RsTrainingSessionV52(
    val id:String,val title:String,val blocks:List<RsSessionBlockV52>,val active:Boolean
)

private fun rsEncodeSessionsV52(items:List<RsTrainingSessionV52>):String{
    val a=JSONArray()
    items.forEach{s->
        a.put(JSONObject().apply{
            put("id",s.id);put("title",s.title);put("active",s.active)
            put("blocks",JSONArray().apply{
                s.blocks.forEach{b->put(JSONObject().apply{
                    put("id",b.id);put("title",b.title);put("seconds",b.seconds);put("instructions",b.instructions)
                })}
            })
        })
    }
    return a.toString()
}

private fun rsDecodeSessionsV52(raw:String):List<RsTrainingSessionV52>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                val ba=o.optJSONArray("blocks")?:JSONArray()
                val blocks=buildList{
                    for(j in 0 until ba.length()){
                        val b=ba.getJSONObject(j)
                        add(RsSessionBlockV52(
                            b.optString("id"),b.optString("title"),
                            b.optInt("seconds",60).coerceIn(10,3600),b.optString("instructions")
                        ))
                    }
                }
                add(RsTrainingSessionV52(o.optString("id"),o.optString("title"),blocks,o.optBoolean("active",true)))
            }
        }
    }.getOrDefault(emptyList())
}

private fun rsSeedSessionsV52()=listOf(
    RsTrainingSessionV52(
        "fundamentals_session","RS Fundamentals Session",
        listOf(
            RsSessionBlockV52("warmup","Warm-up",300,"Movement, guard, light footwork and mobility."),
            RsSessionBlockV52("jab","Jab rounds",180,"Sharp jab, immediate guard recovery and balanced stance."),
            RsSessionBlockV52("combo","Combination rounds",180,"Jab-cross-low kick with controlled recovery."),
            RsSessionBlockV52("cooldown","Cooldown",180,"Breathing, mobility and relaxed recovery.")
        ),true
    )
)

fun rsLoadSessionsV52(store:RsStore):List<RsTrainingSessionV52>{
    val raw=store.s("training_sessions_v52","")
    if(raw.isBlank()){
        rsSaveSessionsV52(store,rsSeedSessionsV52())
        return rsSeedSessionsV52()
    }
    return rsDecodeSessionsV52(raw)
}
private fun rsSaveSessionsV52(store:RsStore,items:List<RsTrainingSessionV52>)=
    store.ps("training_sessions_v52",rsEncodeSessionsV52(items))

private fun rsOpsUiV52(lang:RsLang,key:String):String{
    val en=mapOf(
        "builder" to "Session Builder","builder_sub" to "Create the structured training session shown to students.",
        "player" to "Session Player","player_sub" to "Follow the active trainer-built session block by block.",
        "new_session" to "+ New session","close" to "Close","title" to "Session title","block" to "Block title",
        "seconds" to "Seconds","instructions" to "Instructions","add_block" to "Add block","save" to "Save session",
        "active" to "ACTIVE","inactive" to "INACTIVE","delete" to "Delete","confirm" to "Confirm",
        "start" to "Start","pause" to "Pause","next" to "Next","restart" to "Restart","complete" to "Session complete",
        "qr" to "QR Attendance","qr_sub" to "Generate class check-in QR codes linked to the real attendance list.",
        "student_qr" to "Class Check-In","student_qr_sub" to "Scan the trainer QR to check in to a class.",
        "generate" to "Generate new check-in QR","scan" to "Scan class QR","checked" to "Checked in",
        "invalid" to "Invalid or expired attendance QR.","local_note" to "Local preview: cross-device attendance will sync after Supabase connection.","no_active_session" to "No active trainer session."
    )
    val nl=en+mapOf("builder" to "Sessiebouwer","builder_sub" to "Maak de gestructureerde training die leerlingen zien.","player" to "Sessiespeler","player_sub" to "Volg de actieve trainersessie blok voor blok.","new_session" to "+ Nieuwe sessie","close" to "Sluiten","title" to "Sessietitel","block" to "Bloktitel","seconds" to "Seconden","instructions" to "Instructies","add_block" to "Blok toevoegen","save" to "Sessie opslaan","active" to "ACTIEF","inactive" to "INACTIEF","delete" to "Verwijderen","confirm" to "Bevestigen","start" to "Start","pause" to "Pauze","next" to "Volgende","restart" to "Herstart","complete" to "Sessie voltooid","qr" to "QR Aanwezigheid","qr_sub" to "Genereer check-in QR-codes gekoppeld aan de echte aanwezigheidslijst.","student_qr" to "Les Check-In","student_qr_sub" to "Scan de trainer-QR om in te checken.","generate" to "Nieuwe check-in QR maken","scan" to "Les-QR scannen","checked" to "Ingecheckt","invalid" to "Ongeldige of verlopen aanwezigheids-QR.","local_note" to "Lokale preview: cross-device aanwezigheid synchroniseert na Supabase-koppeling.","no_active_session" to "Geen actieve trainersessie.")
    val pt=en+mapOf("builder" to "Construtor de Sessões","builder_sub" to "Cria a sessão estruturada mostrada aos alunos.","player" to "Leitor de Sessão","player_sub" to "Segue a sessão ativa criada pelo treinador, bloco a bloco.","new_session" to "+ Nova sessão","close" to "Fechar","title" to "Título da sessão","block" to "Título do bloco","seconds" to "Segundos","instructions" to "Instruções","add_block" to "Adicionar bloco","save" to "Guardar sessão","active" to "ATIVO","inactive" to "INATIVO","delete" to "Eliminar","confirm" to "Confirmar","start" to "Iniciar","pause" to "Pausa","next" to "Seguinte","restart" to "Reiniciar","complete" to "Sessão concluída","qr" to "Presença QR","qr_sub" to "Gera QR de check-in ligados à lista real de presenças.","student_qr" to "Check-In da Aula","student_qr_sub" to "Lê o QR do treinador para entrar na aula.","generate" to "Gerar novo QR de check-in","scan" to "Ler QR da aula","checked" to "Check-in efetuado","invalid" to "QR de presença inválido ou expirado.","local_note" to "Prévia local: a presença entre dispositivos sincroniza após ligação ao Supabase.","no_active_session" to "Não existe sessão ativa do treinador.")
    val es=en+mapOf("builder" to "Constructor de Sesiones","builder_sub" to "Crea la sesión estructurada que ven los alumnos.","player" to "Reproductor de Sesión","player_sub" to "Sigue la sesión activa creada por el entrenador bloque a bloque.","new_session" to "+ Nueva sesión","close" to "Cerrar","title" to "Título de sesión","block" to "Título del bloque","seconds" to "Segundos","instructions" to "Instrucciones","add_block" to "Añadir bloque","save" to "Guardar sesión","active" to "ACTIVO","inactive" to "INACTIVO","delete" to "Eliminar","confirm" to "Confirmar","start" to "Iniciar","pause" to "Pausa","next" to "Siguiente","restart" to "Reiniciar","complete" to "Sesión completada","qr" to "Asistencia QR","qr_sub" to "Genera QR de check-in conectados a la lista real de asistencia.","student_qr" to "Check-In de Clase","student_qr_sub" to "Escanea el QR del entrenador para entrar en clase.","generate" to "Generar nuevo QR de check-in","scan" to "Escanear QR de clase","checked" to "Check-in realizado","invalid" to "QR de asistencia inválido o caducado.","local_note" to "Vista previa local: la asistencia entre dispositivos se sincroniza tras conectar Supabase.","no_active_session" to "No hay una sesión activa del entrenador.")
    val fr=en+mapOf("builder" to "Créateur de Séances","builder_sub" to "Crée la séance structurée affichée aux élèves.","player" to "Lecteur de Séance","player_sub" to "Suis la séance active créée par l’entraîneur bloc par bloc.","new_session" to "+ Nouvelle séance","close" to "Fermer","title" to "Titre de la séance","block" to "Titre du bloc","seconds" to "Secondes","instructions" to "Instructions","add_block" to "Ajouter un bloc","save" to "Enregistrer la séance","active" to "ACTIF","inactive" to "INACTIF","delete" to "Supprimer","confirm" to "Confirmer","start" to "Démarrer","pause" to "Pause","next" to "Suivant","restart" to "Recommencer","complete" to "Séance terminée","qr" to "Présence QR","qr_sub" to "Génère des QR de check-in liés à la vraie liste de présence.","student_qr" to "Check-In du Cours","student_qr_sub" to "Scanne le QR entraîneur pour t’enregistrer au cours.","generate" to "Générer un nouveau QR","scan" to "Scanner le QR du cours","checked" to "Enregistré","invalid" to "QR de présence invalide ou expiré.","local_note" to "Prévisualisation locale : la présence multi-appareils sera synchronisée après connexion Supabase.","no_active_session" to "Aucune séance entraîneur active.")
    val de=en+mapOf("builder" to "Session-Builder","builder_sub" to "Erstelle die strukturierte Trainingseinheit für Schüler.","player" to "Session-Player","player_sub" to "Folge der aktiven Trainer-Session Block für Block.","new_session" to "+ Neue Session","close" to "Schließen","title" to "Session-Titel","block" to "Blocktitel","seconds" to "Sekunden","instructions" to "Anweisungen","add_block" to "Block hinzufügen","save" to "Session speichern","active" to "AKTIV","inactive" to "INAKTIV","delete" to "Löschen","confirm" to "Bestätigen","start" to "Start","pause" to "Pause","next" to "Weiter","restart" to "Neu starten","complete" to "Session abgeschlossen","qr" to "QR-Anwesenheit","qr_sub" to "Erzeuge Check-in-QRs für die echte Anwesenheitsliste.","student_qr" to "Kurs-Check-In","student_qr_sub" to "Scanne den Trainer-QR zum Einchecken.","generate" to "Neuen Check-in-QR erzeugen","scan" to "Kurs-QR scannen","checked" to "Eingecheckt","invalid" to "Ungültiger oder abgelaufener Anwesenheits-QR.","local_note" to "Lokale Vorschau: Geräteübergreifende Anwesenheit synchronisiert nach Supabase-Verbindung.","no_active_session" to "Keine aktive Trainer-Session.")
    val it=en+mapOf("builder" to "Creatore Sessioni","builder_sub" to "Crea la sessione strutturata mostrata agli allievi.","player" to "Player Sessione","player_sub" to "Segui la sessione attiva creata dal trainer, blocco per blocco.","new_session" to "+ Nuova sessione","close" to "Chiudi","title" to "Titolo sessione","block" to "Titolo blocco","seconds" to "Secondi","instructions" to "Istruzioni","add_block" to "Aggiungi blocco","save" to "Salva sessione","active" to "ATTIVO","inactive" to "INATTIVO","delete" to "Elimina","confirm" to "Conferma","start" to "Avvia","pause" to "Pausa","next" to "Avanti","restart" to "Riavvia","complete" to "Sessione completata","qr" to "Presenza QR","qr_sub" to "Genera QR di check-in collegati alla vera lista presenze.","student_qr" to "Check-In Lezione","student_qr_sub" to "Scansiona il QR del trainer per il check-in.","generate" to "Genera nuovo QR","scan" to "Scansiona QR lezione","checked" to "Check-in effettuato","invalid" to "QR presenza non valido o scaduto.","local_note" to "Anteprima locale: la presenza tra dispositivi si sincronizza dopo il collegamento Supabase.","no_active_session" to "Nessuna sessione trainer attiva.")
    val pl=en+mapOf("builder" to "Kreator Sesji","builder_sub" to "Twórz ustrukturyzowaną sesję pokazywaną uczniom.","player" to "Odtwarzacz Sesji","player_sub" to "Realizuj aktywną sesję trenera blok po bloku.","new_session" to "+ Nowa sesja","close" to "Zamknij","title" to "Tytuł sesji","block" to "Tytuł bloku","seconds" to "Sekundy","instructions" to "Instrukcje","add_block" to "Dodaj blok","save" to "Zapisz sesję","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","delete" to "Usuń","confirm" to "Potwierdź","start" to "Start","pause" to "Pauza","next" to "Dalej","restart" to "Uruchom ponownie","complete" to "Sesja zakończona","qr" to "Obecność QR","qr_sub" to "Generuj kody QR check-in połączone z prawdziwą listą obecności.","student_qr" to "Check-In na Zajęcia","student_qr_sub" to "Zeskanuj QR trenera, aby się zameldować.","generate" to "Generuj nowy QR","scan" to "Skanuj QR zajęć","checked" to "Zameldowano","invalid" to "Nieprawidłowy lub wygasły QR obecności.","local_note" to "Podgląd lokalny: obecność między urządzeniami zsynchronizuje się po połączeniu Supabase.","no_active_session" to "Brak aktywnej sesji trenera.")
    val tr=en+mapOf("builder" to "Seans Oluşturucu","builder_sub" to "Öğrencilere gösterilen yapılandırılmış antrenman seansını oluştur.","player" to "Seans Oynatıcı","player_sub" to "Antrenörün oluşturduğu aktif seansı blok blok takip et.","new_session" to "+ Yeni seans","close" to "Kapat","title" to "Seans başlığı","block" to "Blok başlığı","seconds" to "Saniye","instructions" to "Talimatlar","add_block" to "Blok ekle","save" to "Seansı kaydet","active" to "AKTİF","inactive" to "PASİF","delete" to "Sil","confirm" to "Onayla","start" to "Başlat","pause" to "Duraklat","next" to "Sonraki","restart" to "Yeniden başlat","complete" to "Seans tamamlandı","qr" to "QR Yoklama","qr_sub" to "Gerçek yoklama listesine bağlı sınıf check-in QR kodları oluştur.","student_qr" to "Ders Check-In","student_qr_sub" to "Derse giriş için antrenör QR kodunu tara.","generate" to "Yeni check-in QR oluştur","scan" to "Ders QR tara","checked" to "Giriş yapıldı","invalid" to "Geçersiz veya süresi dolmuş yoklama QR.","local_note" to "Yerel önizleme: cihazlar arası yoklama Supabase bağlantısından sonra senkronize olur.","no_active_session" to "Aktif antrenör seansı yok.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsSessionBuilderV52(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var sessionTitle by remember{mutableStateOf("")}
    var blockTitle by remember{mutableStateOf("")}
    var blockSeconds by remember{mutableStateOf("180")}
    var blockInstructions by remember{mutableStateOf("")}
    var draftBlocks by remember{mutableStateOf<List<RsSessionBlockV52>>(emptyList())}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val sessions=remember(revision){rsLoadSessionsV52(store)}
    fun save(items:List<RsTrainingSessionV52>){rsSaveSessionsV52(store,items);revision++}

    RsScroll(c,rsOpsUiV52(lang,"builder"),rsOpsUiV52(lang,"builder_sub")){
        Button(onClick={showCreate=!showCreate},modifier=Modifier.fillMaxWidth()){
            Text(if(showCreate)rsOpsUiV52(lang,"close") else rsOpsUiV52(lang,"new_session"))
        }
        if(showCreate)RsPanel(c){
            OutlinedTextField(sessionTitle,{sessionTitle=it.take(100)},label={Text(rsOpsUiV52(lang,"title"))},modifier=Modifier.fillMaxWidth())
            draftBlocks.forEachIndexed{i,b->
                Text((i+1).toString()+". "+b.title+" · "+b.seconds+"s",color=c.text)
            }
            OutlinedTextField(blockTitle,{blockTitle=it.take(80)},label={Text(rsOpsUiV52(lang,"block"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(blockSeconds,{blockSeconds=it.filter(Char::isDigit)},label={Text(rsOpsUiV52(lang,"seconds"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(blockInstructions,{blockInstructions=it.take(1000)},label={Text(rsOpsUiV52(lang,"instructions"))},modifier=Modifier.fillMaxWidth(),minLines=2)
            OutlinedButton(
                onClick={
                    draftBlocks=draftBlocks+RsSessionBlockV52(
                        UUID.randomUUID().toString(),blockTitle.trim(),
                        blockSeconds.toIntOrNull()?.coerceIn(10,3600)?:180,blockInstructions.trim()
                    )
                    blockTitle="";blockSeconds="180";blockInstructions=""
                },
                enabled=blockTitle.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"add_block"))}
            Button(
                onClick={
                    val next=sessions.map{it.copy(active=false)}+
                        RsTrainingSessionV52(UUID.randomUUID().toString(),sessionTitle.trim(),draftBlocks,true)
                    save(next)
                    sessionTitle="";draftBlocks=emptyList();showCreate=false
                },
                enabled=sessionTitle.isNotBlank()&&draftBlocks.isNotEmpty(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"save"))}
        }
        sessions.forEach{s->
            RsPanel(c){
                Text(s.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(s.blocks.size.toString()+" blocks · "+s.blocks.sumOf{it.seconds}/60+" min",color=c.muted)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(s.active)rsOpsUiV52(lang,"active") else rsOpsUiV52(lang,"inactive"),color=c.muted)
                    Switch(s.active,{on->
                        save(sessions.map{
                            if(it.id==s.id)it.copy(active=on)
                            else if(on)it.copy(active=false) else it
                        })
                    })
                }
                s.blocks.forEachIndexed{i,b->Text((i+1).toString()+". "+b.title+" · "+b.seconds+"s",color=c.text,fontSize=11.sp)}
                OutlinedButton(
                    onClick={
                        if(pendingDelete==s.id){save(sessions.filterNot{it.id==s.id});pendingDelete=null}
                        else pendingDelete=s.id
                    },modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==s.id)rsOpsUiV52(lang,"confirm") else rsOpsUiV52(lang,"delete"))}
            }
        }
    }
}

@Composable
fun RsSessionPlayerV52(c:RsPalette,store:RsStore,lang:RsLang){
    val session=rsLoadSessionsV52(store).firstOrNull{it.active}
    var index by remember(session?.id){mutableIntStateOf(0)}
    var remaining by remember(session?.id,index){mutableIntStateOf(session?.blocks?.getOrNull(index)?.seconds?:0)}
    var running by remember(session?.id,index){mutableStateOf(false)}

    LaunchedEffect(running,remaining,index,session?.id){
        if(running && remaining>0){
            delay(1000)
            remaining--
        }else if(running && remaining<=0){
            running=false
        }
    }

    RsScroll(c,rsOpsUiV52(lang,"player"),rsOpsUiV52(lang,"player_sub")){
        if(session==null){
            RsPanel(c){Text(rsOpsUiV52(lang,"no_active_session"),color=c.muted)}
        }else{
            val block=session.blocks.getOrNull(index)
            RsPanel(c){
                Text(session.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=22.sp)
                Text((index+1).coerceAtMost(session.blocks.size).toString()+" / "+session.blocks.size,color=c.muted)
            }
            if(block!=null){
                RsPanel(c){
                    Text(block.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=24.sp)
                    Text(block.instructions,color=c.text)
                    Text((remaining/60).toString().padStart(2,'0')+":"+(remaining%60).toString().padStart(2,'0'),color=c.bright,fontWeight=FontWeight.Black,fontSize=36.sp)
                    Button(onClick={running=!running},modifier=Modifier.fillMaxWidth()){
                        Text(if(running)rsOpsUiV52(lang,"pause") else rsOpsUiV52(lang,"start"))
                    }
                    OutlinedButton(
                        onClick={
                            if(index<session.blocks.lastIndex){index++;running=false}
                            else{index=session.blocks.size;running=false}
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsOpsUiV52(lang,"next"))}
                    OutlinedButton(
                        onClick={remaining=block.seconds;running=false},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsOpsUiV52(lang,"restart"))}
                }
            }else RsPanel(c){Text(rsOpsUiV52(lang,"complete"),color=c.bright,fontWeight=FontWeight.Black)}
        }
    }
}

private fun rsAttendancePayloadV52(classId:String,code:String)=
    "rskickbox://attendance?v=1&classId="+android.net.Uri.encode(classId)+"&code="+android.net.Uri.encode(code)

private fun rsParseAttendanceV52(raw:String):Pair<String,String>? = runCatching{
    val uri=android.net.Uri.parse(raw)
    if(uri.scheme!="rskickbox"||uri.host!="attendance")return null
    val classId=uri.getQueryParameter("classId").orEmpty()
    val code=uri.getQueryParameter("code").orEmpty()
    if(classId.isBlank()||code.isBlank())null else classId to code
}.getOrNull()

@Composable
fun RsQrAttendanceTrainerV52(c:RsPalette,store:RsStore,lang:RsLang){
    val classes=rsLoadClassesV38(store).filter{it.active}
    var selectedId by remember(classes){mutableStateOf(classes.firstOrNull()?.id.orEmpty())}
    var revision by remember{mutableIntStateOf(0)}
    val selected=classes.firstOrNull{it.id==selectedId}
    val code=if(selectedId.isBlank())"" else store.s("attendance_qr_v52_"+selectedId,"")
    val qr:Bitmap?=remember(selectedId,code,revision){
        if(selectedId.isBlank()||code.isBlank())null else rsQrBitmapV33(rsAttendancePayloadV52(selectedId,code),720)
    }

    RsScroll(c,rsOpsUiV52(lang,"qr"),rsOpsUiV52(lang,"qr_sub")){
        Text(rsOpsUiV52(lang,"local_note"),color=c.muted,fontSize=10.sp)
        classes.forEach{clazz->
            FilterChip(
                selected=selectedId==clazz.id,onClick={selectedId=clazz.id},
                label={Text(clazz.dayLabel+" "+clazz.timeLabel+" · "+clazz.title,maxLines=1)},
                modifier=Modifier.fillMaxWidth()
            )
        }
        if(selected!=null)RsPanel(c){
            Text(selected.title,color=c.bright,fontWeight=FontWeight.Black)
            Button(
                onClick={
                    store.ps("attendance_qr_v52_"+selected.id,UUID.randomUUID().toString().replace("-","").take(10).uppercase())
                    revision++
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"generate"))}
            if(qr!=null)Image(qr.asImageBitmap(),"Attendance QR",modifier=Modifier.fillMaxWidth().aspectRatio(1f))
        }
    }
}

@Composable
fun RsStudentCheckInV52(c:RsPalette,store:RsStore,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    var status by remember{mutableStateOf("")}
    val scanner=remember{GmsBarcodeScanning.getClient(context)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")

    RsScroll(c,rsOpsUiV52(lang,"student_qr"),rsOpsUiV52(lang,"student_qr_sub")){
        Text(rsOpsUiV52(lang,"local_note"),color=c.muted,fontSize=10.sp)
        RsPanel(c){
            Button(
                onClick={
                    scanner.startScan()
                        .addOnSuccessListener{barcode->
                            val pair=barcode.rawValue?.let(::rsParseAttendanceV52)
                            if(pair==null){
                                status=rsOpsUiV52(lang,"invalid")
                            }else{
                                val (classId,code)=pair
                                val expected=store.s("attendance_qr_v52_"+classId,"")
                                val validClass=rsLoadClassesV38(store).any{it.id==classId&&it.active}
                                if(expected.isNotBlank()&&expected==code&&validClass){
                                    store.pb(rsAttendanceKeyV38(classId,name),true)
                                    store.ps("attendance_email_v52_"+classId+"_"+email.lowercase(),System.currentTimeMillis().toString())
                                    status=rsOpsUiV52(lang,"checked")
                                }else status=rsOpsUiV52(lang,"invalid")
                            }
                        }
                        .addOnCanceledListener{}
                        .addOnFailureListener{status=rsOpsUiV52(lang,"invalid")}
                },
                modifier=Modifier.fillMaxWidth()
            ){Text(rsOpsUiV52(lang,"scan"))}
            if(status.isNotBlank())Text(status,color=if(status==rsOpsUiV52(lang,"checked"))c.bright else c.muted,fontWeight=FontWeight.Bold)
        }
    }
}

package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RsClubDocumentV51(
    val id:String,val title:String,val body:String,val accessTier:String,val active:Boolean
)

data class RsSupportTicketV51(
    val id:String,val studentEmail:String,val studentName:String,val subject:String,
    val message:String,val trainerReply:String,val status:String,val createdAt:Long
)

data class RsReferralV51(
    val id:String,val ownerEmail:String,val code:String,val uses:Int,val active:Boolean
)

private fun rsLoadDocumentsV51(store:RsStore):List<RsClubDocumentV51>{
    val raw=store.s("documents_v51","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsClubDocumentV51(
                    o.optString("id"),o.optString("title"),o.optString("body"),
                    o.optString("tier","ALL"),o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveDocumentsV51(store:RsStore,items:List<RsClubDocumentV51>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("title",x.title);put("body",x.body);put("tier",x.accessTier);put("active",x.active)
    })}
    store.ps("documents_v51",a.toString())
}

fun rsLoadTicketsV51(store:RsStore):List<RsSupportTicketV51>{
    val raw=store.s("support_tickets_v51","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsSupportTicketV51(
                    o.optString("id"),o.optString("email"),o.optString("name"),
                    o.optString("subject"),o.optString("message"),o.optString("reply"),
                    o.optString("status","OPEN"),o.optLong("createdAt")
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveTicketsV51(store:RsStore,items:List<RsSupportTicketV51>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.studentEmail);put("name",x.studentName);put("subject",x.subject)
        put("message",x.message);put("reply",x.trainerReply);put("status",x.status);put("createdAt",x.createdAt)
    })}
    store.ps("support_tickets_v51",a.toString())
}

private fun rsLoadReferralsV51(store:RsStore):List<RsReferralV51>{
    val raw=store.s("referrals_v51","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsReferralV51(o.optString("id"),o.optString("email"),o.optString("code"),o.optInt("uses"),o.optBoolean("active",true)))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveReferralsV51(store:RsStore,items:List<RsReferralV51>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("email",x.ownerEmail);put("code",x.code);put("uses",x.uses);put("active",x.active)
    })}
    store.ps("referrals_v51",a.toString())
}

private fun rsServiceUiV51(lang:RsLang,key:String):String{
    val en=mapOf(
        "documents" to "Club Documents","documents_sub" to "Rules, guides and club information available to your membership.",
        "documents_admin" to "Documents Manager","documents_admin_sub" to "Publish club documents by membership level.",
        "support" to "Support","support_sub" to "Send a private support request to the RS KICKBOX team.",
        "support_admin" to "Support Inbox","support_admin_sub" to "Review and answer member support requests.",
        "referrals" to "Referrals","referrals_sub" to "Share RS KICKBOX with someone you know.",
        "title" to "Title","body" to "Document","tier" to "Access","publish" to "Publish document",
        "subject" to "Subject","message" to "Message","send" to "Send request","reply" to "Trainer reply",
        "save_reply" to "Save reply","open" to "OPEN","resolved" to "RESOLVED","resolve" to "Resolve","reopen" to "Reopen",
        "delete" to "Delete","confirm" to "Confirm","none" to "Nothing here yet.",
        "your_code" to "Your referral code","share" to "Share referral","uses" to "uses"
    )
    val nl=en+mapOf("documents" to "Clubdocumenten","documents_sub" to "Regels, gidsen en clubinformatie voor jouw lidmaatschap.","documents_admin" to "Documentbeheer","documents_admin_sub" to "Publiceer clubdocumenten per lidmaatschapsniveau.","support" to "Support","support_sub" to "Stuur een privé supportverzoek naar het RS KICKBOX-team.","support_admin" to "Support Inbox","support_admin_sub" to "Bekijk en beantwoord supportverzoeken van leden.","referrals" to "Referrals","referrals_sub" to "Deel RS KICKBOX met iemand die je kent.","title" to "Titel","body" to "Document","tier" to "Toegang","publish" to "Document publiceren","subject" to "Onderwerp","message" to "Bericht","send" to "Verzoek versturen","reply" to "Trainerantwoord","save_reply" to "Antwoord opslaan","open" to "OPEN","resolved" to "OPGELOST","resolve" to "Oplossen","reopen" to "Heropenen","delete" to "Verwijderen","confirm" to "Bevestigen","none" to "Nog niets hier.","your_code" to "Jouw referralcode","share" to "Referral delen","uses" to "keer gebruikt")
    val pt=en+mapOf("documents" to "Documentos do Clube","documents_sub" to "Regras, guias e informação do clube disponíveis para a tua adesão.","documents_admin" to "Gestor de Documentos","documents_admin_sub" to "Publica documentos por nível de adesão.","support" to "Suporte","support_sub" to "Envia um pedido privado à equipa RS KICKBOX.","support_admin" to "Caixa de Suporte","support_admin_sub" to "Revê e responde a pedidos de suporte dos membros.","referrals" to "Referências","referrals_sub" to "Partilha RS KICKBOX com alguém que conheces.","title" to "Título","body" to "Documento","tier" to "Acesso","publish" to "Publicar documento","subject" to "Assunto","message" to "Mensagem","send" to "Enviar pedido","reply" to "Resposta do treinador","save_reply" to "Guardar resposta","open" to "ABERTO","resolved" to "RESOLVIDO","resolve" to "Resolver","reopen" to "Reabrir","delete" to "Eliminar","confirm" to "Confirmar","none" to "Ainda não há nada aqui.","your_code" to "O teu código de referência","share" to "Partilhar referência","uses" to "utilizações")
    val es=en+mapOf("documents" to "Documentos del Club","documents_sub" to "Reglas, guías e información del club disponibles para tu membresía.","documents_admin" to "Gestor de Documentos","documents_admin_sub" to "Publica documentos por nivel de membresía.","support" to "Soporte","support_sub" to "Envía una solicitud privada al equipo RS KICKBOX.","support_admin" to "Bandeja de Soporte","support_admin_sub" to "Revisa y responde solicitudes de los miembros.","referrals" to "Referidos","referrals_sub" to "Comparte RS KICKBOX con alguien que conozcas.","title" to "Título","body" to "Documento","tier" to "Acceso","publish" to "Publicar documento","subject" to "Asunto","message" to "Mensaje","send" to "Enviar solicitud","reply" to "Respuesta del entrenador","save_reply" to "Guardar respuesta","open" to "ABIERTO","resolved" to "RESUELTO","resolve" to "Resolver","reopen" to "Reabrir","delete" to "Eliminar","confirm" to "Confirmar","none" to "Todavía no hay nada aquí.","your_code" to "Tu código de referido","share" to "Compartir referido","uses" to "usos")
    val fr=en+mapOf("documents" to "Documents du Club","documents_sub" to "Règles, guides et informations du club disponibles pour ton adhésion.","documents_admin" to "Gestion Documents","documents_admin_sub" to "Publie des documents par niveau d’adhésion.","support" to "Support","support_sub" to "Envoie une demande privée à l’équipe RS KICKBOX.","support_admin" to "Boîte Support","support_admin_sub" to "Examine et répond aux demandes des membres.","referrals" to "Parrainages","referrals_sub" to "Partage RS KICKBOX avec quelqu’un que tu connais.","title" to "Titre","body" to "Document","tier" to "Accès","publish" to "Publier le document","subject" to "Sujet","message" to "Message","send" to "Envoyer la demande","reply" to "Réponse entraîneur","save_reply" to "Enregistrer la réponse","open" to "OUVERT","resolved" to "RÉSOLU","resolve" to "Résoudre","reopen" to "Rouvrir","delete" to "Supprimer","confirm" to "Confirmer","none" to "Rien ici pour le moment.","your_code" to "Ton code de parrainage","share" to "Partager le parrainage","uses" to "utilisations")
    val de=en+mapOf("documents" to "Club-Dokumente","documents_sub" to "Regeln, Leitfäden und Clubinfos für deine Mitgliedschaft.","documents_admin" to "Dokumenten-Manager","documents_admin_sub" to "Veröffentliche Club-Dokumente nach Mitgliedsstufe.","support" to "Support","support_sub" to "Sende eine private Supportanfrage an das RS KICKBOX-Team.","support_admin" to "Support-Postfach","support_admin_sub" to "Prüfe und beantworte Mitgliederanfragen.","referrals" to "Empfehlungen","referrals_sub" to "Teile RS KICKBOX mit jemandem, den du kennst.","title" to "Titel","body" to "Dokument","tier" to "Zugriff","publish" to "Dokument veröffentlichen","subject" to "Betreff","message" to "Nachricht","send" to "Anfrage senden","reply" to "Trainer-Antwort","save_reply" to "Antwort speichern","open" to "OFFEN","resolved" to "ERLEDIGT","resolve" to "Erledigen","reopen" to "Wieder öffnen","delete" to "Löschen","confirm" to "Bestätigen","none" to "Noch nichts hier.","your_code" to "Dein Empfehlungscode","share" to "Empfehlung teilen","uses" to "Nutzungen")
    val it=en+mapOf("documents" to "Documenti del Club","documents_sub" to "Regole, guide e informazioni del club disponibili per il tuo abbonamento.","documents_admin" to "Gestione Documenti","documents_admin_sub" to "Pubblica documenti per livello di abbonamento.","support" to "Supporto","support_sub" to "Invia una richiesta privata al team RS KICKBOX.","support_admin" to "Posta Supporto","support_admin_sub" to "Controlla e rispondi alle richieste dei membri.","referrals" to "Referral","referrals_sub" to "Condividi RS KICKBOX con qualcuno che conosci.","title" to "Titolo","body" to "Documento","tier" to "Accesso","publish" to "Pubblica documento","subject" to "Oggetto","message" to "Messaggio","send" to "Invia richiesta","reply" to "Risposta trainer","save_reply" to "Salva risposta","open" to "APERTO","resolved" to "RISOLTO","resolve" to "Risolvi","reopen" to "Riapri","delete" to "Elimina","confirm" to "Conferma","none" to "Ancora nulla qui.","your_code" to "Il tuo codice referral","share" to "Condividi referral","uses" to "utilizzi")
    val pl=en+mapOf("documents" to "Dokumenty Klubu","documents_sub" to "Zasady, poradniki i informacje klubowe dostępne dla Twojego członkostwa.","documents_admin" to "Menedżer Dokumentów","documents_admin_sub" to "Publikuj dokumenty według poziomu członkostwa.","support" to "Wsparcie","support_sub" to "Wyślij prywatne zgłoszenie do zespołu RS KICKBOX.","support_admin" to "Skrzynka Wsparcia","support_admin_sub" to "Przeglądaj i odpowiadaj na zgłoszenia członków.","referrals" to "Polecenia","referrals_sub" to "Udostępnij RS KICKBOX komuś, kogo znasz.","title" to "Tytuł","body" to "Dokument","tier" to "Dostęp","publish" to "Opublikuj dokument","subject" to "Temat","message" to "Wiadomość","send" to "Wyślij zgłoszenie","reply" to "Odpowiedź trenera","save_reply" to "Zapisz odpowiedź","open" to "OTWARTE","resolved" to "ROZWIĄZANE","resolve" to "Rozwiąż","reopen" to "Otwórz ponownie","delete" to "Usuń","confirm" to "Potwierdź","none" to "Jeszcze nic tu nie ma.","your_code" to "Twój kod polecający","share" to "Udostępnij polecenie","uses" to "użycia")
    val tr=en+mapOf("documents" to "Kulüp Belgeleri","documents_sub" to "Üyeliğine açık kurallar, rehberler ve kulüp bilgileri.","documents_admin" to "Belge Yönetimi","documents_admin_sub" to "Üyelik seviyesine göre kulüp belgeleri yayınla.","support" to "Destek","support_sub" to "RS KICKBOX ekibine özel destek talebi gönder.","support_admin" to "Destek Gelen Kutusu","support_admin_sub" to "Üye destek taleplerini incele ve yanıtla.","referrals" to "Referanslar","referrals_sub" to "RS KICKBOX’u tanıdığın biriyle paylaş.","title" to "Başlık","body" to "Belge","tier" to "Erişim","publish" to "Belgeyi yayınla","subject" to "Konu","message" to "Mesaj","send" to "Talep gönder","reply" to "Antrenör yanıtı","save_reply" to "Yanıtı kaydet","open" to "AÇIK","resolved" to "ÇÖZÜLDÜ","resolve" to "Çöz","reopen" to "Yeniden aç","delete" to "Sil","confirm" to "Onayla","none" to "Henüz burada bir şey yok.","your_code" to "Referans kodun","share" to "Referansı paylaş","uses" to "kullanım")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsDocumentsV51(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    var revision by remember{mutableIntStateOf(0)}
    var title by remember{mutableStateOf("")}
    var body by remember{mutableStateOf("")}
    var tier by remember{mutableStateOf("ALL")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val all=remember(revision){rsLoadDocumentsV51(store)}
    val studentRank=rsContentRankV48(rsContentStudentTierV48(store))
    val visible=if(role==RsRole.TRAINER)all else all.filter{it.active&&rsContentRankV48(it.accessTier)<=studentRank}
    fun save(items:List<RsClubDocumentV51>){rsSaveDocumentsV51(store,items);revision++}

    RsScroll(c,
        if(role==RsRole.TRAINER)rsServiceUiV51(lang,"documents_admin") else rsServiceUiV51(lang,"documents"),
        if(role==RsRole.TRAINER)rsServiceUiV51(lang,"documents_admin_sub") else rsServiceUiV51(lang,"documents_sub")
    ){
        if(role==RsRole.TRAINER)RsPanel(c){
            OutlinedTextField(title,{title=it.take(120)},label={Text(rsServiceUiV51(lang,"title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(body,{body=it.take(10000)},label={Text(rsServiceUiV51(lang,"body"))},modifier=Modifier.fillMaxWidth(),minLines=5)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","BASIC","PRO","ELITE").forEach{x->
                    FilterChip(selected=tier==x,onClick={tier=x},label={Text(x,fontSize=9.sp)},modifier=Modifier.weight(1f))
                }
            }
            Button(
                onClick={
                    save(listOf(RsClubDocumentV51(UUID.randomUUID().toString(),title.trim(),body.trim(),tier,true))+all)
                    title="";body="";tier="ALL"
                },
                enabled=title.isNotBlank()&&body.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsServiceUiV51(lang,"publish"))}
        }
        if(visible.isEmpty())RsPanel(c){Text(rsServiceUiV51(lang,"none"),color=c.muted)}
        visible.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                if(role==RsRole.TRAINER)Text(x.accessTier,color=c.muted)
                Text(x.body,color=c.text)
                if(role==RsRole.TRAINER){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(x.active)"ACTIVE" else "INACTIVE",color=c.muted)
                        Switch(x.active,{v->save(all.map{if(it.id==x.id)it.copy(active=v) else it})})
                    }
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==x.id){save(all.filterNot{it.id==x.id});pendingDelete=null}
                            else pendingDelete=x.id
                        },modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==x.id)rsServiceUiV51(lang,"confirm") else rsServiceUiV51(lang,"delete"))}
                }
            }
        }
    }
}

@Composable
fun RsSupportV51(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    var revision by remember{mutableIntStateOf(0)}
    var subject by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}
    var replyDrafts by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")
    val all=remember(revision){rsLoadTicketsV51(store).sortedByDescending{it.createdAt}}
    val visible=if(role==RsRole.TRAINER)all else all.filter{it.studentEmail.equals(email,true)}
    fun save(items:List<RsSupportTicketV51>){rsSaveTicketsV51(store,items);revision++}

    RsScroll(c,
        if(role==RsRole.TRAINER)rsServiceUiV51(lang,"support_admin") else rsServiceUiV51(lang,"support"),
        if(role==RsRole.TRAINER)rsServiceUiV51(lang,"support_admin_sub") else rsServiceUiV51(lang,"support_sub")
    ){
        if(role==RsRole.STUDENT)RsPanel(c){
            OutlinedTextField(subject,{subject=it.take(120)},label={Text(rsServiceUiV51(lang,"subject"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(message,{message=it.take(2000)},label={Text(rsServiceUiV51(lang,"message"))},modifier=Modifier.fillMaxWidth(),minLines=4)
            Button(
                onClick={
                    save(listOf(RsSupportTicketV51(UUID.randomUUID().toString(),email,name,subject.trim(),message.trim(),"","OPEN",System.currentTimeMillis()))+all)
                    subject="";message=""
                },
                enabled=subject.isNotBlank()&&message.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsServiceUiV51(lang,"send"))}
        }
        if(visible.isEmpty())RsPanel(c){Text(rsServiceUiV51(lang,"none"),color=c.muted)}
        visible.forEach{x->
            RsPanel(c){
                Text(x.subject,color=c.bright,fontWeight=FontWeight.Black)
                if(role==RsRole.TRAINER)Text(x.studentName+" · "+x.studentEmail,color=c.muted,fontSize=10.sp)
                Text(x.message,color=c.text)
                Text(SimpleDateFormat("dd MMM yyyy · HH:mm",Locale.getDefault()).format(Date(x.createdAt)),color=c.muted,fontSize=9.sp)
                if(x.trainerReply.isNotBlank()){
                    Text(rsServiceUiV51(lang,"reply"),color=c.bright,fontWeight=FontWeight.Bold)
                    Text(x.trainerReply,color=c.text)
                }
                Text(if(x.status=="RESOLVED")rsServiceUiV51(lang,"resolved") else rsServiceUiV51(lang,"open"),color=c.muted)
                if(role==RsRole.TRAINER){
                    OutlinedTextField(
                        replyDrafts[x.id]?:x.trainerReply,
                        {v->replyDrafts=replyDrafts+(x.id to v.take(2000))},
                        label={Text(rsServiceUiV51(lang,"reply"))},
                        modifier=Modifier.fillMaxWidth(),minLines=2
                    )
                    Button(
                        onClick={
                            val reply=replyDrafts[x.id]?:x.trainerReply
                            save(all.map{if(it.id==x.id)it.copy(trainerReply=reply) else it})
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsServiceUiV51(lang,"save_reply"))}
                    OutlinedButton(
                        onClick={save(all.map{if(it.id==x.id)it.copy(status=if(it.status=="RESOLVED")"OPEN" else "RESOLVED") else it})},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(x.status=="RESOLVED")rsServiceUiV51(lang,"reopen") else rsServiceUiV51(lang,"resolve"))}
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==x.id){save(all.filterNot{it.id==x.id});pendingDelete=null}
                            else pendingDelete=x.id
                        },modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==x.id)rsServiceUiV51(lang,"confirm") else rsServiceUiV51(lang,"delete"))}
                }
            }
        }
    }
}

@Composable
fun RsReferralsV51(c:RsPalette,store:RsStore,lang:RsLang){
    val context=androidx.compose.ui.platform.LocalContext.current
    val email=store.s("session_student_email","alex@rskickbox.nl")
    var revision by remember{mutableIntStateOf(0)}
    val all=remember(revision){rsLoadReferralsV51(store)}
    var mine=all.firstOrNull{it.ownerEmail.equals(email,true)}
    if(mine==null){
        mine=RsReferralV51(UUID.randomUUID().toString(),email,"RS"+UUID.randomUUID().toString().replace("-","").take(7).uppercase(),0,true)
        rsSaveReferralsV51(store,all+mine)
    }
    val referral=mine!!
    val sharingAllowed=rsOpsEnabledV56(store,RsOpsKeysV56.REFERRALS,true)
    RsScroll(c,rsServiceUiV51(lang,"referrals"),rsServiceUiV51(lang,"referrals_sub")){
        RsPanel(c){
            Text(rsServiceUiV51(lang,"your_code"),color=c.muted)
            Text(referral.code,color=c.bright,fontWeight=FontWeight.Black,fontSize=30.sp)
            Text(referral.uses.toString()+" "+rsServiceUiV51(lang,"uses"),color=c.text)
            Button(
                onClick={
                    val intent=android.content.Intent(android.content.Intent.ACTION_SEND).apply{
                        type="text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT,"Join RS KICKBOX with referral code "+referral.code+"\n"+RS_PLAY_STORE_URL_V33)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent,"Share RS KICKBOX referral"))
                },
                enabled=sharingAllowed,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsServiceUiV51(lang,"share"))}
            if(!sharingAllowed)Text("Referral sharing is temporarily disabled by the trainer.",color=c.muted,fontSize=10.sp)
        }
    }
}

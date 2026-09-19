package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlinx.coroutines.launch

data class RsInvoiceV39(
    val id:String,
    val studentName:String,
    val studentEmail:String,
    val period:String,
    val amountCents:Int,
    val status:String
)

private val rsSeedInvoicesV39=listOf(
    RsInvoiceV39("INV-26091","Alex de Vries","alex@rskickbox.nl","Sep 2026",4900,"PAID"),
    RsInvoiceV39("INV-26090","Alex de Vries","alex@rskickbox.nl","Aug 2026",4900,"PAID"),
    RsInvoiceV39("INV-26089","Alex de Vries","alex@rskickbox.nl","Jul 2026",4900,"PAID")
)

private fun rsEncodeInvoicesV39(items:List<RsInvoiceV39>):String{
    val arr=JSONArray()
    items.forEach{i->
        arr.put(JSONObject().apply{
            put("id",i.id)
            put("student",i.studentName)
            put("email",i.studentEmail)
            put("period",i.period)
            put("amount",i.amountCents)
            put("status",i.status)
        })
    }
    return arr.toString()
}

private fun rsDecodeInvoicesV39(raw:String):List<RsInvoiceV39>{
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(RsInvoiceV39(
                    o.optString("id"),
                    o.optString("student"),
                    o.optString("email"),
                    o.optString("period"),
                    o.optInt("amount",0),
                    o.optString("status","PENDING")
                ))
            }
        }
    }.getOrDefault(emptyList())
}

fun rsLoadInvoicesV39(store:RsStore):List<RsInvoiceV39>{
    val raw=store.s("finance_invoices_v39","")
    if(raw.isBlank()){
        store.ps("finance_invoices_v39",rsEncodeInvoicesV39(rsSeedInvoicesV39))
        return rsSeedInvoicesV39
    }
    return rsDecodeInvoicesV39(raw)
}

private fun rsSaveInvoicesV39(store:RsStore,items:List<RsInvoiceV39>)=
    store.ps("finance_invoices_v39",rsEncodeInvoicesV39(items))

fun rsFinanceInvoicesForStudentV40(store:RsStore,studentEmail:String,studentName:String):List<RsInvoiceV39> =
    rsLoadInvoicesV39(store).filter{
        if(it.studentEmail.isNotBlank())it.studentEmail.equals(studentEmail,true)
        else it.studentName.equals(studentName,true)
    }

private fun rsMoneyV39(cents:Int)="€"+String.format("%.2f",cents/100.0)

private val rsPaymentMethodsV39=listOf(
    "Bank transfer","Manual payment request","Tikkie / payment link","iDEAL / Wero merchant",
    "Revolut Pay","Cards / Apple Pay / Google Pay","PayPal","Cash at club"
)

private fun rsFinanceUiV39(lang:RsLang,key:String):String{
    val en=mapOf(
        "student_title" to "Membership & Payments","student_sub" to "Your plan, renewal status, payment methods and invoice history.",
        "active" to "Active","next_renewal" to "Next renewal","history" to "PAYMENT HISTORY","paid" to "Paid","pending" to "Pending",
        "trainer_title" to "Payment Center","trainer_sub" to "Configure accepted methods and local payment instructions.",
        "methods" to "PAYMENT METHODS","details" to "BUSINESS PAYMENT DETAILS","account" to "Bank / account details",
        "reference" to "Payment reference / instructions","invoices" to "Invoices & Revenue","invoice_sub" to "Local invoice ledger prepared for backend migration.",
        "create_invoice" to "+ Create invoice","close" to "Close","student" to "Student","period" to "Period","amount" to "Amount €",
        "save" to "Save invoice","mark_paid" to "Mark paid","mark_pending" to "Mark pending","delete" to "Delete","confirm" to "Confirm",
        "collected" to "collected","outstanding" to "outstanding"
    )
    val nl=en+mapOf("student_title" to "Lidmaatschap & Betalingen","student_sub" to "Je plan, verlenging, betaalmethoden en factuurhistorie.","active" to "Actief","next_renewal" to "Volgende verlenging","history" to "BETALINGSHISTORIE","paid" to "Betaald","pending" to "Openstaand","trainer_title" to "Betalingscentrum","trainer_sub" to "Beheer betaalmethoden en lokale betaalinstructies.","methods" to "BETAALMETHODEN","details" to "ZAKELIJKE BETAALGEGEVENS","account" to "Bank / rekeninggegevens","reference" to "Betalingskenmerk / instructies","invoices" to "Facturen & Omzet","invoice_sub" to "Lokale factuuradministratie voorbereid voor backendmigratie.","create_invoice" to "+ Factuur maken","close" to "Sluiten","student" to "Leerling","period" to "Periode","amount" to "Bedrag €","save" to "Factuur opslaan","mark_paid" to "Markeer betaald","mark_pending" to "Markeer openstaand","delete" to "Verwijderen","confirm" to "Bevestigen","collected" to "ontvangen","outstanding" to "openstaand")
    val pt=en+mapOf("student_title" to "Adesão & Pagamentos","student_sub" to "Plano, renovação, métodos de pagamento e histórico.","active" to "Ativo","next_renewal" to "Próxima renovação","history" to "HISTÓRICO DE PAGAMENTOS","paid" to "Pago","pending" to "Pendente","trainer_title" to "Centro de Pagamentos","trainer_sub" to "Configura métodos aceites e instruções locais.","methods" to "MÉTODOS DE PAGAMENTO","details" to "DADOS DE PAGAMENTO","account" to "Banco / conta","reference" to "Referência / instruções","invoices" to "Faturas & Receita","invoice_sub" to "Registo local de faturas preparado para o backend.","create_invoice" to "+ Criar fatura","close" to "Fechar","student" to "Aluno","period" to "Período","amount" to "Valor €","save" to "Guardar fatura","mark_paid" to "Marcar pago","mark_pending" to "Marcar pendente","delete" to "Eliminar","confirm" to "Confirmar","collected" to "recebido","outstanding" to "pendente")
    val es=en+mapOf("student_title" to "Membresía & Pagos","student_sub" to "Plan, renovación, métodos de pago e historial.","active" to "Activo","next_renewal" to "Próxima renovación","history" to "HISTORIAL DE PAGOS","paid" to "Pagado","pending" to "Pendiente","trainer_title" to "Centro de Pagos","trainer_sub" to "Configura métodos aceptados e instrucciones locales.","methods" to "MÉTODOS DE PAGO","details" to "DATOS DE PAGO","account" to "Banco / cuenta","reference" to "Referencia / instrucciones","invoices" to "Facturas & Ingresos","invoice_sub" to "Registro local de facturas preparado para backend.","create_invoice" to "+ Crear factura","close" to "Cerrar","student" to "Alumno","period" to "Periodo","amount" to "Importe €","save" to "Guardar factura","mark_paid" to "Marcar pagada","mark_pending" to "Marcar pendiente","delete" to "Eliminar","confirm" to "Confirmar","collected" to "cobrado","outstanding" to "pendiente")
    val fr=en+mapOf("student_title" to "Adhésion & Paiements","student_sub" to "Forfait, renouvellement, moyens de paiement et historique.","active" to "Actif","next_renewal" to "Prochain renouvellement","history" to "HISTORIQUE DES PAIEMENTS","paid" to "Payé","pending" to "En attente","trainer_title" to "Centre de Paiement","trainer_sub" to "Configure les moyens acceptés et les instructions locales.","methods" to "MOYENS DE PAIEMENT","details" to "INFORMATIONS DE PAIEMENT","account" to "Banque / compte","reference" to "Référence / instructions","invoices" to "Factures & Revenus","invoice_sub" to "Registre local des factures préparé pour le backend.","create_invoice" to "+ Créer facture","close" to "Fermer","student" to "Élève","period" to "Période","amount" to "Montant €","save" to "Enregistrer","mark_paid" to "Marquer payée","mark_pending" to "Marquer en attente","delete" to "Supprimer","confirm" to "Confirmer","collected" to "encaissé","outstanding" to "à recevoir")
    val de=en+mapOf("student_title" to "Mitgliedschaft & Zahlungen","student_sub" to "Plan, Verlängerung, Zahlungsmethoden und Verlauf.","active" to "Aktiv","next_renewal" to "Nächste Verlängerung","history" to "ZAHLUNGSVERLAUF","paid" to "Bezahlt","pending" to "Offen","trainer_title" to "Zahlungscenter","trainer_sub" to "Akzeptierte Methoden und lokale Zahlungsanweisungen verwalten.","methods" to "ZAHLUNGSMETHODEN","details" to "ZAHLUNGSDETAILS","account" to "Bank / Konto","reference" to "Referenz / Hinweise","invoices" to "Rechnungen & Umsatz","invoice_sub" to "Lokales Rechnungsbuch für Backendmigration.","create_invoice" to "+ Rechnung erstellen","close" to "Schließen","student" to "Schüler","period" to "Zeitraum","amount" to "Betrag €","save" to "Rechnung speichern","mark_paid" to "Als bezahlt","mark_pending" to "Als offen","delete" to "Löschen","confirm" to "Bestätigen","collected" to "eingenommen","outstanding" to "offen")
    val it=en+mapOf("student_title" to "Abbonamento & Pagamenti","student_sub" to "Piano, rinnovo, metodi di pagamento e storico.","active" to "Attivo","next_renewal" to "Prossimo rinnovo","history" to "STORICO PAGAMENTI","paid" to "Pagato","pending" to "In attesa","trainer_title" to "Centro Pagamenti","trainer_sub" to "Configura metodi accettati e istruzioni locali.","methods" to "METODI DI PAGAMENTO","details" to "DATI DI PAGAMENTO","account" to "Banca / conto","reference" to "Riferimento / istruzioni","invoices" to "Fatture & Ricavi","invoice_sub" to "Registro locale fatture pronto per backend.","create_invoice" to "+ Crea fattura","close" to "Chiudi","student" to "Allievo","period" to "Periodo","amount" to "Importo €","save" to "Salva fattura","mark_paid" to "Segna pagata","mark_pending" to "Segna in attesa","delete" to "Elimina","confirm" to "Conferma","collected" to "incassato","outstanding" to "da incassare")
    val pl=en+mapOf("student_title" to "Członkostwo & Płatności","student_sub" to "Plan, odnowienie, metody płatności i historia.","active" to "Aktywne","next_renewal" to "Następne odnowienie","history" to "HISTORIA PŁATNOŚCI","paid" to "Zapłacono","pending" to "Oczekuje","trainer_title" to "Centrum Płatności","trainer_sub" to "Konfiguruj metody i lokalne instrukcje płatności.","methods" to "METODY PŁATNOŚCI","details" to "DANE PŁATNOŚCI","account" to "Bank / konto","reference" to "Tytuł / instrukcje","invoices" to "Faktury & Przychody","invoice_sub" to "Lokalny rejestr faktur gotowy do migracji.","create_invoice" to "+ Utwórz fakturę","close" to "Zamknij","student" to "Uczeń","period" to "Okres","amount" to "Kwota €","save" to "Zapisz fakturę","mark_paid" to "Oznacz zapłaconą","mark_pending" to "Oznacz oczekującą","delete" to "Usuń","confirm" to "Potwierdź","collected" to "otrzymano","outstanding" to "oczekuje")
    val tr=en+mapOf("student_title" to "Üyelik & Ödemeler","student_sub" to "Planın, yenileme, ödeme yöntemleri ve geçmiş.","active" to "Aktif","next_renewal" to "Sonraki yenileme","history" to "ÖDEME GEÇMİŞİ","paid" to "Ödendi","pending" to "Bekliyor","trainer_title" to "Ödeme Merkezi","trainer_sub" to "Kabul edilen yöntemleri ve yerel talimatları ayarla.","methods" to "ÖDEME YÖNTEMLERİ","details" to "ÖDEME BİLGİLERİ","account" to "Banka / hesap","reference" to "Referans / talimatlar","invoices" to "Faturalar & Gelir","invoice_sub" to "Backend için hazırlanmış yerel fatura kaydı.","create_invoice" to "+ Fatura oluştur","close" to "Kapat","student" to "Öğrenci","period" to "Dönem","amount" to "Tutar €","save" to "Faturayı kaydet","mark_paid" to "Ödendi işaretle","mark_pending" to "Bekliyor işaretle","delete" to "Sil","confirm" to "Onayla","collected" to "tahsil edildi","outstanding" to "bekliyor")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsStudentFinanceV39(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){
        RsCloudStudentFinanceV77(c,lang)
        return
    }
    val sessionEmail=store.s("session_student_email","alex@rskickbox.nl")
    val sessionName=store.s("session_student_name","Alex de Vries")
    val account=rsLoadStudentsV33(store).firstOrNull{it.email.equals(sessionEmail,true)}
    val plan=account?.plan?.uppercase()?:"PRO"
    val amountCents=rsPlanMonthlyCentsV49(store,plan)
    val invoices=rsLoadInvoicesV39(store).filter{
        if(it.studentEmail.isNotBlank())it.studentEmail.equals(sessionEmail,true)
        else it.studentName.equals(sessionName,true)
    }
    val enabledMethods=rsPaymentMethodsV39.filterIndexed{i,_->store.b("pay_v39_"+i,i<2||i==7)}
    RsScroll(c,rsFinanceUiV39(lang,"student_title"),rsFinanceUiV39(lang,"student_sub")){
        RsPanel(c){
            Text("RS "+plan,color=c.bright,fontSize=26.sp,fontWeight=FontWeight.Black)
            Text(rsMoneyV39(amountCents)+" / month · "+rsFinanceUiV39(lang,"active"),color=c.text)
            Text(rsFinanceUiV39(lang,"next_renewal")+" · 01 Oct 2026",color=c.muted)
        }
        if(enabledMethods.isNotEmpty())RsPanel(c){
            Text(rsFinanceUiV39(lang,"methods"),color=c.bright,fontWeight=FontWeight.Bold)
            enabledMethods.forEach{Text("• "+it,color=c.text)}
        }
        RsPanel(c){
            Text(rsFinanceUiV39(lang,"history"),color=c.bright,fontWeight=FontWeight.Bold)
            invoices.forEach{i->
                Text(i.period+" · "+rsMoneyV39(i.amountCents)+" · "+if(i.status=="PAID")rsFinanceUiV39(lang,"paid") else rsFinanceUiV39(lang,"pending"),color=c.muted)
            }
        }
    }
}

@Composable
fun RsTrainerPaymentCenterV39(c:RsPalette,store:RsStore,lang:RsLang){
    var account by remember{mutableStateOf(store.s("pay_v39_account","NL00 BANK 0000 0000 00"))}
    var note by remember{mutableStateOf(store.s("pay_v39_note","RS KICKBOX membership"))}
    RsScroll(c,rsFinanceUiV39(lang,"trainer_title"),rsFinanceUiV39(lang,"trainer_sub")){
        RsPanel(c){
            Text(rsFinanceUiV39(lang,"methods"),color=c.bright,fontWeight=FontWeight.Bold)
            rsPaymentMethodsV39.forEachIndexed{i,m->
                var enabled by remember{mutableStateOf(store.b("pay_v39_"+i,i<2||i==7))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text(m,color=c.text,modifier=Modifier.weight(1f))
                    Switch(enabled,{v->enabled=v;store.pb("pay_v39_"+i,v)})
                }
            }
        }
        RsPanel(c){
            Text(rsFinanceUiV39(lang,"details"),color=c.bright,fontWeight=FontWeight.Bold)
            OutlinedTextField(account,{account=it;store.ps("pay_v39_account",it)},label={Text(rsFinanceUiV39(lang,"account"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(note,{note=it;store.ps("pay_v39_note",it)},label={Text(rsFinanceUiV39(lang,"reference"))},modifier=Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun RsTrainerInvoicesV39(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){
        RsCloudTrainerInvoicesV77(c,lang)
        return
    }
    var revision by remember{mutableIntStateOf(0)}
    var showCreate by remember{mutableStateOf(false)}
    var student by remember{mutableStateOf("")}
    var studentEmail by remember{mutableStateOf("")}
    var period by remember{mutableStateOf("")}
    var amount by remember{mutableStateOf("49.00")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val invoices=remember(revision){rsLoadInvoicesV39(store)}
    fun save(items:List<RsInvoiceV39>){rsSaveInvoicesV39(store,items);revision++}

    val collected=invoices.filter{it.status=="PAID"}.sumOf{it.amountCents}
    val outstanding=invoices.filter{it.status!="PAID"}.sumOf{it.amountCents}

    RsScroll(c,rsFinanceUiV39(lang,"invoices"),rsFinanceUiV39(lang,"invoice_sub")){
        RsPanel(c){
            Text(rsMoneyV39(collected)+" "+rsFinanceUiV39(lang,"collected"),color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
            Text(rsMoneyV39(outstanding)+" "+rsFinanceUiV39(lang,"outstanding"),color=c.muted)
        }
        Button(onClick={showCreate=!showCreate},modifier=Modifier.fillMaxWidth()){
            Text(if(showCreate)rsFinanceUiV39(lang,"close") else rsFinanceUiV39(lang,"create_invoice"))
        }
        if(showCreate)RsPanel(c){
            OutlinedTextField(student,{student=it},label={Text(rsFinanceUiV39(lang,"student"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(studentEmail,{studentEmail=it},label={Text(rsCloudT93(lang,"email"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(period,{period=it},label={Text(rsFinanceUiV39(lang,"period"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(amount,{amount=it.filter{ch->ch.isDigit()||ch=='.'||ch==','}},label={Text(rsFinanceUiV39(lang,"amount"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(
                onClick={
                    val cents=((amount.replace(',','.').toDoubleOrNull()?:0.0)*100).toInt().coerceAtLeast(0)
                    save(listOf(RsInvoiceV39("INV-"+UUID.randomUUID().toString().take(8).uppercase(),student.trim(),studentEmail.trim(),period.trim(),cents,"PENDING"))+invoices)
                    student=""
                    studentEmail=""
                    period=""
                    amount="49.00"
                    showCreate=false
                },
                enabled=student.isNotBlank()&&studentEmail.contains("@")&&period.isNotBlank()&&(amount.replace(',','.').toDoubleOrNull()?:0.0)>0.0,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsFinanceUiV39(lang,"save"))}
        }

        invoices.forEach{inv->
            RsPanel(c){
                Text(inv.id+" · "+inv.studentName,color=c.bright,fontWeight=FontWeight.Bold)
                if(inv.studentEmail.isNotBlank())Text(inv.studentEmail,color=c.muted)
                Text(inv.period+" · "+rsMoneyV39(inv.amountCents),color=c.text)
                Text(if(inv.status=="PAID")rsFinanceUiV39(lang,"paid") else rsFinanceUiV39(lang,"pending"),color=c.muted)
                Button(
                    onClick={
                        val next=if(inv.status=="PAID")"PENDING" else "PAID"
                        save(invoices.map{if(it.id==inv.id)it.copy(status=next) else it})
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(inv.status=="PAID")rsFinanceUiV39(lang,"mark_pending") else rsFinanceUiV39(lang,"mark_paid"))}
                OutlinedButton(
                    onClick={
                        if(pendingDelete==inv.id){
                            save(invoices.filterNot{it.id==inv.id})
                            pendingDelete=null
                        }else pendingDelete=inv.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==inv.id)rsFinanceUiV39(lang,"confirm") else rsFinanceUiV39(lang,"delete"))}
            }
        }
    }
}


@Composable
private fun RsCloudStudentFinanceV77(c:RsPalette,lang:RsLang){
    var loading by remember{mutableStateOf(true)}
    var status by remember{mutableStateOf("")}
    var summary by remember{mutableStateOf<RsCloudBillingSummaryV77?>(null)}
    var invoices by remember{mutableStateOf<List<RsCloudInvoiceV77>>(emptyList())}

    LaunchedEffect(Unit){
        loading=true
        rsMyBillingSummaryV77()
            .onSuccess{summary=it}
            .onFailure{status=it.message?:"Could not load membership billing."}
        rsMyInvoicesV77()
            .onSuccess{invoices=it}
            .onFailure{status=it.message?:"Could not load invoice history."}
        loading=false
    }

    RsScroll(c,rsFinanceUiV39(lang,"student_title"),"Your real membership and invoice history from RS KICKBOX."){
        RsPanel(c){
            Text(
                if(loading)"Syncing billing…" else "Cloud billing connected",
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        val billing=summary
        if(billing!=null){
            RsPanel(c){
                Text("RS "+billing.plan,color=c.bright,fontSize=26.sp,fontWeight=FontWeight.Black)
                Text(
                    rsMoneyV39(billing.amountCents)+" / month · "+
                        if(billing.active&&billing.membershipStatus=="active")rsFinanceUiV39(lang,"active")
                        else billing.membershipStatus.uppercase(),
                    color=c.text
                )
                Text(
                    rsFinanceUiV39(lang,"next_renewal")+" · "+rsCloudDateLabelV77(billing.currentPeriodEnd),
                    color=c.muted
                )
            }
        }

        RsPanel(c){
            Text(rsFinanceUiV39(lang,"history"),color=c.bright,fontWeight=FontWeight.Bold)
            if(invoices.isEmpty()&&!loading)Text(rsCloudT93(lang,"no_invoices"),color=c.muted)
            invoices.forEach{i->
                Text(
                    i.periodLabel+" · "+rsMoneyV39(i.amountCents)+" · "+
                        when(i.status){
                            "paid"->rsFinanceUiV39(lang,"paid")
                            else->rsFinanceUiV39(lang,"pending")
                        },
                    color=c.muted
                )
                Text(i.invoiceNumber,color=c.text,fontSize=9.sp)
            }
        }
    }
}

@Composable
private fun RsCloudTrainerInvoicesV77(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var invoices by remember{mutableStateOf<List<RsCloudInvoiceV77>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var showCreate by remember{mutableStateOf(false)}
    var studentEmail by remember{mutableStateOf("")}
    var period by remember{mutableStateOf("")}
    var amount by remember{mutableStateOf("49.00")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision){
        loading=true
        rsStaffInvoicesV77()
            .onSuccess{invoices=it}
            .onFailure{status=it.message?:"Could not load invoices."}
        loading=false
    }

    val collected=invoices.filter{it.status=="paid"}.sumOf{it.amountCents}
    val outstanding=invoices.filter{it.status in listOf("pending","overdue")}.sumOf{it.amountCents}

    RsScroll(c,rsFinanceUiV39(lang,"invoices"),"Live invoice ledger synchronized with Supabase."){
        RsPanel(c){
            Text(rsMoneyV39(collected)+" "+rsFinanceUiV39(lang,"collected"),color=c.bright,fontSize=22.sp,fontWeight=FontWeight.Black)
            Text(rsMoneyV39(outstanding)+" "+rsFinanceUiV39(lang,"outstanding"),color=c.muted)
            Text(
                if(loading)"Syncing invoices…" else "Cloud invoice ledger connected",
                color=if(loading)c.muted else c.bright,
                fontSize=10.sp,
                fontWeight=FontWeight.Bold
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        Button(
            onClick={showCreate=!showCreate},
            enabled=!busy,
            modifier=Modifier.fillMaxWidth()
        ){Text(if(showCreate)rsFinanceUiV39(lang,"close") else rsFinanceUiV39(lang,"create_invoice"))}

        if(showCreate)RsPanel(c){
            OutlinedTextField(
                studentEmail,
                {studentEmail=it.take(180)},
                label={Text(rsCloudT93(lang,"student_email"))},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                enabled=!busy
            )
            OutlinedTextField(
                period,
                {period=it.take(80)},
                label={Text(rsFinanceUiV39(lang,"period"))},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                enabled=!busy
            )
            OutlinedTextField(
                amount,
                {amount=it.filter{ch->ch.isDigit()||ch=='.'||ch==','}.take(12)},
                label={Text(rsFinanceUiV39(lang,"amount"))},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                enabled=!busy
            )
            Button(
                onClick={
                    val cents=((amount.replace(',','.').toDoubleOrNull()?:0.0)*100).toInt().coerceAtLeast(0)
                    busy=true
                    status=""
                    scope.launch{
                        rsCreateCloudInvoiceV77(studentEmail,period,cents)
                            .onSuccess{
                                studentEmail=""
                                period=""
                                amount="49.00"
                                showCreate=false
                                status=rsCloudT93(lang,"invoice_created")
                                revision++
                            }
                            .onFailure{status=it.message?:"Could not create invoice."}
                        busy=false
                    }
                },
                enabled=!busy&&studentEmail.contains("@")&&period.isNotBlank()&&(amount.replace(',','.').toDoubleOrNull()?:0.0)>0.0,
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busy)"Saving…" else rsFinanceUiV39(lang,"save"))}
        }

        invoices.forEach{inv->
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,inv.studentEmail.orEmpty(),inv.studentName.orEmpty(),size=44.dp)
                    Column(Modifier.weight(1f)){
                        Text(inv.invoiceNumber+" · "+inv.studentName.orEmpty(),color=c.bright,fontWeight=FontWeight.Bold)
                        Text(inv.studentEmail.orEmpty(),color=c.muted,fontSize=9.sp)
                    }
                }
                Text(inv.periodLabel+" · "+rsMoneyV39(inv.amountCents),color=c.text)
                Text(inv.status.uppercase(),color=c.muted)
                Button(
                    onClick={
                        busy=true
                        val next=if(inv.status=="paid")"pending" else "paid"
                        scope.launch{
                            rsSetCloudInvoiceStatusV77(inv.id,next)
                                .onSuccess{
                                    status=if(next=="paid")"Invoice marked paid." else "Invoice marked pending."
                                    revision++
                                }
                                .onFailure{status=it.message?:"Could not update invoice."}
                            busy=false
                        }
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){
                    Text(if(inv.status=="paid")rsFinanceUiV39(lang,"mark_pending") else rsFinanceUiV39(lang,"mark_paid"))
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==inv.id){
                            busy=true
                            scope.launch{
                                rsDeleteCloudInvoiceV77(inv.id)
                                    .onSuccess{
                                        pendingDelete=null
                                        status=rsCloudT93(lang,"invoice_deleted")
                                        revision++
                                    }
                                    .onFailure{status=it.message?:"Could not delete invoice."}
                                busy=false
                            }
                        }else pendingDelete=inv.id
                    },
                    enabled=!busy,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==inv.id)rsFinanceUiV39(lang,"confirm") else rsFinanceUiV39(lang,"delete"))}
            }
        }
    }
}

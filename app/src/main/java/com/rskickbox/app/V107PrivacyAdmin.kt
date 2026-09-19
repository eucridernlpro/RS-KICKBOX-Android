package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
data class RsStaffAccountRequestV107(
    val id:String,
    @SerialName("user_id") val userId:String,
    val email:String?=null,
    @SerialName("display_name") val displayName:String?=null,
    @SerialName("request_type") val requestType:String,
    val status:String,
    @SerialName("user_note") val userNote:String="",
    @SerialName("staff_note") val staffNote:String="",
    @SerialName("requested_at") val requestedAt:String,
    @SerialName("completed_at") val completedAt:String?=null
)

suspend fun rsStaffAccountRequestsV107():Result<List<RsStaffAccountRequestV107>> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc("rs_staff_account_requests").decodeList<RsStaffAccountRequestV107>()
}

suspend fun rsStaffUpdateAccountRequestV107(
    requestId:String,
    status:String,
    staffNote:String
):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.postgrest.rpc(
        "rs_staff_update_account_request",
        buildJsonObject{
            put("p_request_id",requestId)
            put("p_status",status)
            put("p_staff_note",staffNote.take(2000))
        }
    )
    Unit
}

private fun rsPrivacyAdminT107(lang:RsLang,key:String):String{
    val en=mapOf(
        "title" to "Privacy Requests",
        "sub" to "Review data-export and account-deletion requests from members.",
        "refresh" to "Refresh",
        "empty" to "No account/privacy requests.",
        "member" to "Member",
        "type" to "Request",
        "status" to "Status",
        "requested" to "Requested",
        "note" to "Staff note",
        "save" to "Save status",
        "processing" to "Processing",
        "completed" to "Completed",
        "rejected" to "Rejected",
        "cancelled" to "Cancelled",
        "load_failed" to "Could not load privacy requests.",
        "save_failed" to "Could not update this request.",
        "saved" to "Request updated.",
        "important" to "Important",
        "important_body" to "Mark account deletion as completed only after the authorized server-side deletion/retention procedure has actually been performed."
    )
    val packs=mapOf(
        "nl" to mapOf("title" to "Privacyverzoeken","sub" to "Beheer data-export- en accountverwijderingsverzoeken van leden.","refresh" to "Vernieuwen","empty" to "Geen account-/privacyverzoeken.","member" to "Lid","type" to "Verzoek","status" to "Status","requested" to "Aangevraagd","note" to "Notitie medewerker","save" to "Status opslaan","processing" to "In behandeling","completed" to "Voltooid","rejected" to "Afgewezen","cancelled" to "Geannuleerd","load_failed" to "Privacyverzoeken konden niet worden geladen.","save_failed" to "Verzoek kon niet worden bijgewerkt.","saved" to "Verzoek bijgewerkt.","important" to "Belangrijk","important_body" to "Markeer accountverwijdering pas als voltooid nadat de bevoegde server-side verwijderings-/bewaarprocedure echt is uitgevoerd."),
        "pt" to mapOf("title" to "Pedidos de Privacidade","sub" to "Gerir pedidos de exportação de dados e eliminação de conta.","refresh" to "Atualizar","empty" to "Sem pedidos de conta/privacidade.","member" to "Membro","type" to "Pedido","status" to "Estado","requested" to "Pedido em","note" to "Nota da equipa","save" to "Guardar estado","processing" to "Em processamento","completed" to "Concluído","rejected" to "Rejeitado","cancelled" to "Cancelado","load_failed" to "Não foi possível carregar os pedidos.","save_failed" to "Não foi possível atualizar o pedido.","saved" to "Pedido atualizado.","important" to "Importante","important_body" to "Marca a eliminação como concluída apenas depois do procedimento autorizado de eliminação/retenção no servidor ter sido realmente executado."),
        "es" to mapOf("title" to "Solicitudes de Privacidad","sub" to "Gestiona exportaciones de datos y solicitudes de eliminación de cuenta.","refresh" to "Actualizar","empty" to "No hay solicitudes de cuenta/privacidad.","member" to "Miembro","type" to "Solicitud","status" to "Estado","requested" to "Solicitada","note" to "Nota del equipo","save" to "Guardar estado","processing" to "Procesando","completed" to "Completada","rejected" to "Rechazada","cancelled" to "Cancelada","load_failed" to "No se pudieron cargar las solicitudes.","save_failed" to "No se pudo actualizar la solicitud.","saved" to "Solicitud actualizada.","important" to "Importante","important_body" to "Marca la eliminación como completada solo después de ejecutar realmente el procedimiento autorizado de eliminación/retención en el servidor."),
        "fr" to mapOf("title" to "Demandes de Confidentialité","sub" to "Gérer les exports de données et suppressions de compte.","refresh" to "Actualiser","empty" to "Aucune demande de compte/confidentialité.","member" to "Membre","type" to "Demande","status" to "Statut","requested" to "Demandée","note" to "Note équipe","save" to "Enregistrer le statut","processing" to "En traitement","completed" to "Terminée","rejected" to "Refusée","cancelled" to "Annulée","load_failed" to "Impossible de charger les demandes.","save_failed" to "Impossible de mettre à jour la demande.","saved" to "Demande mise à jour.","important" to "Important","important_body" to "Ne marque la suppression comme terminée qu’après exécution réelle de la procédure serveur autorisée de suppression/conservation."),
        "de" to mapOf("title" to "Datenschutzanfragen","sub" to "Datenexport- und Kontolöschanfragen von Mitgliedern verwalten.","refresh" to "Aktualisieren","empty" to "Keine Konto-/Datenschutzanfragen.","member" to "Mitglied","type" to "Anfrage","status" to "Status","requested" to "Angefragt","note" to "Teamnotiz","save" to "Status speichern","processing" to "In Bearbeitung","completed" to "Abgeschlossen","rejected" to "Abgelehnt","cancelled" to "Abgebrochen","load_failed" to "Anfragen konnten nicht geladen werden.","save_failed" to "Anfrage konnte nicht aktualisiert werden.","saved" to "Anfrage aktualisiert.","important" to "Wichtig","important_body" to "Kontolöschung erst als abgeschlossen markieren, nachdem das autorisierte serverseitige Lösch-/Aufbewahrungsverfahren tatsächlich durchgeführt wurde."),
        "it" to mapOf("title" to "Richieste Privacy","sub" to "Gestisci esportazioni dati e richieste di eliminazione account.","refresh" to "Aggiorna","empty" to "Nessuna richiesta account/privacy.","member" to "Membro","type" to "Richiesta","status" to "Stato","requested" to "Richiesta","note" to "Nota staff","save" to "Salva stato","processing" to "In elaborazione","completed" to "Completata","rejected" to "Rifiutata","cancelled" to "Annullata","load_failed" to "Impossibile caricare le richieste.","save_failed" to "Impossibile aggiornare la richiesta.","saved" to "Richiesta aggiornata.","important" to "Importante","important_body" to "Segna l’eliminazione come completata solo dopo che la procedura server autorizzata di eliminazione/conservazione è stata realmente eseguita."),
        "pl" to mapOf("title" to "Wnioski Prywatności","sub" to "Obsługuj eksport danych i wnioski o usunięcie konta.","refresh" to "Odśwież","empty" to "Brak wniosków konta/prywatności.","member" to "Członek","type" to "Wniosek","status" to "Status","requested" to "Zgłoszono","note" to "Notatka personelu","save" to "Zapisz status","processing" to "W toku","completed" to "Zakończone","rejected" to "Odrzucone","cancelled" to "Anulowane","load_failed" to "Nie udało się wczytać wniosków.","save_failed" to "Nie udało się zaktualizować wniosku.","saved" to "Wniosek zaktualizowany.","important" to "Ważne","important_body" to "Oznacz usunięcie konta jako zakończone dopiero po faktycznym wykonaniu autoryzowanej procedury usunięcia/retencji po stronie serwera."),
        "tr" to mapOf("title" to "Gizlilik İstekleri","sub" to "Veri dışa aktarma ve hesap silme isteklerini yönet.","refresh" to "Yenile","empty" to "Hesap/gizlilik isteği yok.","member" to "Üye","type" to "İstek","status" to "Durum","requested" to "İstek tarihi","note" to "Ekip notu","save" to "Durumu kaydet","processing" to "İşleniyor","completed" to "Tamamlandı","rejected" to "Reddedildi","cancelled" to "İptal edildi","load_failed" to "İstekler yüklenemedi.","save_failed" to "İstek güncellenemedi.","saved" to "İstek güncellendi.","important" to "Önemli","important_body" to "Hesap silmeyi yalnızca yetkili sunucu tarafı silme/saklama prosedürü gerçekten tamamlandıktan sonra tamamlandı olarak işaretle.")
    )
    return packs[lang.code]?.get(key)?:en[key]?:key
}

private fun rsRequestTypeLabelV107(lang:RsLang,type:String):String = when(type){
    "account_deletion" -> when(lang.code){
        "nl"->"Account verwijderen";"pt"->"Eliminar conta";"es"->"Eliminar cuenta";"fr"->"Supprimer le compte";"de"->"Konto löschen";"it"->"Elimina account";"pl"->"Usuń konto";"tr"->"Hesabı sil";else->"Account deletion"
    }
    "data_export" -> when(lang.code){
        "nl"->"Data-export";"pt"->"Exportar dados";"es"->"Exportar datos";"fr"->"Export de données";"de"->"Datenexport";"it"->"Esporta dati";"pl"->"Eksport danych";"tr"->"Veri dışa aktarma";else->"Data export"
    }
    else -> type.replace('_',' ')
}

@Composable
fun RsPrivacyRequestsAdminV107(c:RsPalette,lang:RsLang){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var loading by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var requests by remember{mutableStateOf<List<RsStaffAccountRequestV107>>(emptyList())}

    LaunchedEffect(revision){
        if(!RsSupabaseV60.configured)return@LaunchedEffect
        loading=true
        rsStaffAccountRequestsV107()
            .onSuccess{requests=it;message=""}
            .onFailure{message=rsPrivacyAdminT107(lang,"load_failed")}
        loading=false
    }

    RsScroll(c,rsPrivacyAdminT107(lang,"title"),rsPrivacyAdminT107(lang,"sub")){
        RsPanel(c){
            Text(rsPrivacyAdminT107(lang,"important"),color=c.bright,fontWeight=FontWeight.Black)
            Text(rsPrivacyAdminT107(lang,"important_body"),color=c.muted,fontSize=10.sp)
        }
        OutlinedButton(onClick={revision++},enabled=!loading,modifier=Modifier.fillMaxWidth()){
            Text(rsPrivacyAdminT107(lang,"refresh"))
        }
        if(message.isNotBlank())Text(message,color=c.muted)
        if(!RsSupabaseV60.configured){
            Text(rsReleaseT98(lang,"backend_not_connected"),color=c.muted)
            return@RsScroll
        }
        if(requests.isEmpty()&&!loading)Text(rsPrivacyAdminT107(lang,"empty"),color=c.muted)

        requests.forEach{request->
            var selectedStatus by remember(request.id,request.status){mutableStateOf(request.status)}
            var staffNote by remember(request.id,request.staffNote){mutableStateOf(request.staffNote)}
            var saving by remember(request.id){mutableStateOf(false)}
            RsPanel(c){
                Text(request.displayName?.ifBlank{null} ?: request.email ?: request.userId,color=c.bright,fontWeight=FontWeight.Black,fontSize=17.sp)
                if(!request.email.isNullOrBlank())Text(request.email,color=c.muted,fontSize=10.sp)
                Text(rsPrivacyAdminT107(lang,"type")+": "+rsRequestTypeLabelV107(lang,request.requestType),color=c.text)
                Text(rsPrivacyAdminT107(lang,"requested")+": "+request.requestedAt,color=c.muted,fontSize=10.sp)
                if(request.userNote.isNotBlank())Text(request.userNote,color=c.text,fontSize=10.sp)

                Text(rsPrivacyAdminT107(lang,"status"),color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
                val states=listOf("requested","processing","completed","rejected","cancelled")
                states.chunked(2).forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        row.forEach{state->
                            FilterChip(
                                selected=selectedStatus==state,
                                onClick={selectedStatus=state},
                                label={Text(if(state=="requested")rsPrivacyAdminT107(lang,"requested") else rsPrivacyAdminT107(lang,state),fontSize=9.sp)},
                                modifier=Modifier.weight(1f)
                            )
                        }
                        if(row.size==1)Spacer(Modifier.weight(1f))
                    }
                }

                OutlinedTextField(
                    value=staffNote,
                    onValueChange={staffNote=it.take(2000)},
                    label={Text(rsPrivacyAdminT107(lang,"note"))},
                    modifier=Modifier.fillMaxWidth(),
                    minLines=2
                )
                Button(
                    onClick={
                        saving=true
                        scope.launch{
                            rsStaffUpdateAccountRequestV107(request.id,selectedStatus,staffNote)
                                .onSuccess{message=rsPrivacyAdminT107(lang,"saved");revision++}
                                .onFailure{message=rsPrivacyAdminT107(lang,"save_failed")}
                            saving=false
                        }
                    },
                    enabled=!saving,
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(saving)rsReleaseT98(lang,"please_wait") else rsPrivacyAdminT107(lang,"save"))}
            }
        }
    }
}

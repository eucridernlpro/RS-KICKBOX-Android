package com.rskickbox.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private fun rsChatAdminT111(lang:RsLang,key:String):String{
    val en=mapOf(
        "edit" to "Edit message","delete" to "Delete message","confirm_delete" to "Confirm delete",
        "save" to "Save edit","cancel" to "Cancel","clear" to "Clear conversation",
        "confirm_clear" to "Confirm clear conversation","cleared" to "Conversation cleared.",
        "updated" to "Message updated.","deleted" to "Message deleted.",
        "edit_hint" to "Edit trainer/admin message text","manage" to "TRAINER CHAT MANAGEMENT"
    )
    val nl=en+mapOf("edit" to "Bericht bewerken","delete" to "Bericht verwijderen","confirm_delete" to "Verwijderen bevestigen","save" to "Wijziging opslaan","cancel" to "Annuleren","clear" to "Gesprek wissen","confirm_clear" to "Gesprek wissen bevestigen","cleared" to "Gesprek gewist.","updated" to "Bericht bijgewerkt.","deleted" to "Bericht verwijderd.","edit_hint" to "Tekst van trainer/adminbericht bewerken","manage" to "TRAINER CHATBEHEER")
    val pt=en+mapOf("edit" to "Editar mensagem","delete" to "Eliminar mensagem","confirm_delete" to "Confirmar eliminação","save" to "Guardar edição","cancel" to "Cancelar","clear" to "Limpar conversa","confirm_clear" to "Confirmar limpeza","cleared" to "Conversa limpa.","updated" to "Mensagem atualizada.","deleted" to "Mensagem eliminada.","edit_hint" to "Editar texto da mensagem do treinador/admin","manage" to "GESTÃO DE CHAT DO TREINADOR")
    val es=en+mapOf("edit" to "Editar mensaje","delete" to "Eliminar mensaje","confirm_delete" to "Confirmar eliminación","save" to "Guardar edición","cancel" to "Cancelar","clear" to "Vaciar conversación","confirm_clear" to "Confirmar vaciado","cleared" to "Conversación vaciada.","updated" to "Mensaje actualizado.","deleted" to "Mensaje eliminado.","edit_hint" to "Editar texto del mensaje","manage" to "GESTIÓN DE CHAT")
    val fr=en+mapOf("edit" to "Modifier le message","delete" to "Supprimer le message","confirm_delete" to "Confirmer la suppression","save" to "Enregistrer","cancel" to "Annuler","clear" to "Effacer la conversation","confirm_clear" to "Confirmer l’effacement","cleared" to "Conversation effacée.","updated" to "Message modifié.","deleted" to "Message supprimé.","edit_hint" to "Modifier le texte du message","manage" to "GESTION DU CHAT")
    val de=en+mapOf("edit" to "Nachricht bearbeiten","delete" to "Nachricht löschen","confirm_delete" to "Löschen bestätigen","save" to "Änderung speichern","cancel" to "Abbrechen","clear" to "Unterhaltung leeren","confirm_clear" to "Leeren bestätigen","cleared" to "Unterhaltung geleert.","updated" to "Nachricht aktualisiert.","deleted" to "Nachricht gelöscht.","edit_hint" to "Nachrichtentext bearbeiten","manage" to "TRAINER-CHATVERWALTUNG")
    val it=en+mapOf("edit" to "Modifica messaggio","delete" to "Elimina messaggio","confirm_delete" to "Conferma eliminazione","save" to "Salva modifica","cancel" to "Annulla","clear" to "Svuota conversazione","confirm_clear" to "Conferma svuotamento","cleared" to "Conversazione svuotata.","updated" to "Messaggio aggiornato.","deleted" to "Messaggio eliminato.","edit_hint" to "Modifica testo messaggio","manage" to "GESTIONE CHAT TRAINER")
    val pl=en+mapOf("edit" to "Edytuj wiadomość","delete" to "Usuń wiadomość","confirm_delete" to "Potwierdź usunięcie","save" to "Zapisz zmianę","cancel" to "Anuluj","clear" to "Wyczyść rozmowę","confirm_clear" to "Potwierdź wyczyszczenie","cleared" to "Rozmowa wyczyszczona.","updated" to "Wiadomość zaktualizowana.","deleted" to "Wiadomość usunięta.","edit_hint" to "Edytuj tekst wiadomości","manage" to "ZARZĄDZANIE CZATEM")
    val tr=en+mapOf("edit" to "Mesajı düzenle","delete" to "Mesajı sil","confirm_delete" to "Silmeyi onayla","save" to "Düzenlemeyi kaydet","cancel" to "İptal","clear" to "Konuşmayı temizle","confirm_clear" to "Temizlemeyi onayla","cleared" to "Konuşma temizlendi.","updated" to "Mesaj güncellendi.","deleted" to "Mesaj silindi.","edit_hint" to "Mesaj metnini düzenle","manage" to "ANTRENÖR SOHBET YÖNETİMİ")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsTrainerMessageAdminV111(
    c:RsPalette,
    lang:RsLang,
    body:String,
    canEdit:Boolean,
    busy:Boolean,
    onEdit:suspend (String)->Result<Unit>,
    onDelete:suspend ()->Result<Unit>,
    onChanged:()->Unit,
    onStatus:(String)->Unit
){
    val scope=rememberCoroutineScope()
    var editing by remember{mutableStateOf(false)}
    var draft by remember(body){mutableStateOf(body)}
    var pendingDelete by remember{mutableStateOf(false)}

    if(editing){
        OutlinedTextField(
            draft,{draft=it.take(1200)},
            label={Text(rsChatAdminT111(lang,"edit_hint"))},
            modifier=Modifier.fillMaxWidth(),
            minLines=2,
            enabled=!busy
        )
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Button(
                onClick={
                    scope.launch{
                        onEdit(draft)
                            .onSuccess{editing=false;onStatus(rsChatAdminT111(lang,"updated"));onChanged()}
                            .onFailure{onStatus(it.message.orEmpty())}
                    }
                },
                enabled=!busy&&(draft.isNotBlank()),
                modifier=Modifier.weight(1f)
            ){Text(rsChatAdminT111(lang,"save"),fontSize=9.sp)}
            OutlinedButton(
                onClick={editing=false;draft=body},
                enabled=!busy,
                modifier=Modifier.weight(1f)
            ){Text(rsChatAdminT111(lang,"cancel"),fontSize=9.sp)}
        }
    }else{
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            if(canEdit){
                OutlinedButton(
                    onClick={editing=true;pendingDelete=false},
                    enabled=!busy,
                    modifier=Modifier.weight(1f)
                ){Text(rsChatAdminT111(lang,"edit"),fontSize=9.sp)}
            }
            OutlinedButton(
                onClick={
                    if(pendingDelete){
                        scope.launch{
                            onDelete()
                                .onSuccess{pendingDelete=false;onStatus(rsChatAdminT111(lang,"deleted"));onChanged()}
                                .onFailure{onStatus(it.message.orEmpty())}
                        }
                    }else pendingDelete=true
                },
                enabled=!busy,
                modifier=Modifier.weight(1f)
            ){Text(rsChatAdminT111(lang,if(pendingDelete)"confirm_delete" else "delete"),fontSize=9.sp)}
        }
    }
}

@Composable
fun RsTrainerClearConversationV111(
    c:RsPalette,
    lang:RsLang,
    enabled:Boolean,
    onClear:suspend ()->Result<Unit>,
    onChanged:()->Unit,
    onStatus:(String)->Unit
){
    val scope=rememberCoroutineScope()
    var confirm by remember{mutableStateOf(false)}
    RsPanel(c){
        Text(rsChatAdminT111(lang,"manage"),color=c.bright,fontWeight=FontWeight.Black,fontSize=10.sp)
        OutlinedButton(
            onClick={
                if(confirm){
                    scope.launch{
                        onClear()
                            .onSuccess{confirm=false;onStatus(rsChatAdminT111(lang,"cleared"));onChanged()}
                            .onFailure{onStatus(it.message.orEmpty())}
                    }
                }else confirm=true
            },
            enabled=enabled,
            modifier=Modifier.fillMaxWidth()
        ){Text(rsChatAdminT111(lang,if(confirm)"confirm_clear" else "clear"))}
    }
}

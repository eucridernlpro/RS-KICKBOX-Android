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
import java.util.UUID

data class RsContentItemV48(
    val id:String,val title:String,val category:String,val body:String,
    val accessTier:String,val published:Boolean
)

private fun rsSeedContentV48()=listOf(
    RsContentItemV48("jab_basics","Jab Fundamentals","TECHNIQUE","Stance, shoulder protection, straight path, quick recovery and balance.","ALL",true),
    RsContentItemV48("roundhouse","Roundhouse Kick Mechanics","TECHNIQUE","Hip rotation, pivot, shin line, guard discipline and controlled recovery.","BASIC",true),
    RsContentItemV48("defense","Defense Essentials","DEFENSE","High guard, parry, slip, checking and distance management.","PRO",true),
    RsContentItemV48("fight_prep","Fight Preparation","FIGHT CAMP","Planning intensity, recovery, sparring load and tactical focus.","ELITE",true)
)

private fun rsLoadContentV48(store:RsStore):List<RsContentItemV48>{
    val raw=store.s("content_v48","")
    if(raw.isBlank()){
        rsSaveContentV48(store,rsSeedContentV48())
        return rsSeedContentV48()
    }
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsContentItemV48(
                    o.optString("id"),o.optString("title"),o.optString("category"),
                    o.optString("body"),o.optString("tier","ALL"),o.optBoolean("published",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveContentV48(store:RsStore,items:List<RsContentItemV48>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("title",x.title);put("category",x.category);put("body",x.body)
        put("tier",x.accessTier);put("published",x.published)
    })}
    store.ps("content_v48",a.toString())
}

fun rsContentRankV48(tier:String)=when(tier.uppercase()){
    "ALL"->0;"BASIC"->1;"PRO"->2;"ELITE"->3;else->99
}
fun rsContentStudentTierV48(store:RsStore):String{
    val email=store.s("session_student_email","alex@rskickbox.nl")
    return rsLoadStudentsV33(store).firstOrNull{it.email.equals(email,true)}?.plan?.uppercase()?:"PRO"
}
private fun rsVisibleContentV48(store:RsStore):List<RsContentItemV48>{
    val rank=rsContentRankV48(rsContentStudentTierV48(store))
    return rsLoadContentV48(store).filter{it.published&&rsContentRankV48(it.accessTier)<=rank}
}
private fun rsContentOwnerV48(store:RsStore)=store.s("session_student_email","alex@rskickbox.nl").lowercase()
private fun rsFavoriteIdsV48(store:RsStore):Set<String> =
    store.s("content_favorites_v48_"+rsContentOwnerV48(store),"").split(',').filter{it.isNotBlank()}.toSet()
private fun rsSaveFavoriteIdsV48(store:RsStore,ids:Set<String>)=
    store.ps("content_favorites_v48_"+rsContentOwnerV48(store),ids.joinToString(","))
private fun rsHistoryIdsV48(store:RsStore):List<String> =
    store.s("content_history_v48_"+rsContentOwnerV48(store),"").split(',').filter{it.isNotBlank()}
private fun rsPushHistoryV48(store:RsStore,id:String){
    val next=(listOf(id)+rsHistoryIdsV48(store).filterNot{it==id}).take(30)
    store.ps("content_history_v48_"+rsContentOwnerV48(store),next.joinToString(","))
}

private fun rsContentUiV48(lang:RsLang,key:String):String{
    val en=mapOf(
        "manager" to "Content Manager","manager_sub" to "Create and publish searchable training content by subscription level.",
        "vault" to "Knowledge Vault","vault_sub" to "Your unlocked RS KICKBOX training library.",
        "favorites" to "Saved & Favorites","favorites_sub" to "Training content you saved for quick access.",
        "search" to "Search","search_sub" to "Search your unlocked training library.",
        "new" to "NEW CONTENT","title" to "Title","category" to "Category","body" to "Content",
        "tier" to "Access","publish" to "PUBLISHED","draft" to "DRAFT","save" to "Save content",
        "delete" to "Delete","confirm" to "Confirm","favorite" to "Save","unfavorite" to "Saved",
        "read" to "Open","back" to "Back","query" to "Search techniques, lessons, topics…","none" to "No matching content."
    )
    val nl=en+mapOf("manager" to "Contentbeheer","manager_sub" to "Maak en publiceer doorzoekbare trainingscontent per abonnementsniveau.","vault" to "Kennisbibliotheek","vault_sub" to "Jouw vrijgegeven RS KICKBOX trainingsbibliotheek.","favorites" to "Opgeslagen & Favorieten","favorites_sub" to "Trainingscontent die je hebt opgeslagen.","search" to "Zoeken","search_sub" to "Zoek in jouw vrijgegeven trainingsbibliotheek.","new" to "NIEUWE CONTENT","title" to "Titel","category" to "Categorie","body" to "Inhoud","tier" to "Toegang","publish" to "GEPUBLICEERD","draft" to "CONCEPT","save" to "Content opslaan","delete" to "Verwijderen","confirm" to "Bevestigen","favorite" to "Opslaan","unfavorite" to "Opgeslagen","read" to "Openen","back" to "Terug","query" to "Zoek technieken, lessen, onderwerpen…","none" to "Geen overeenkomende content.")
    val pt=en+mapOf("manager" to "Gestor de Conteúdo","manager_sub" to "Cria e publica conteúdo de treino pesquisável por nível de adesão.","vault" to "Biblioteca de Conhecimento","vault_sub" to "A tua biblioteca RS KICKBOX desbloqueada.","favorites" to "Guardados & Favoritos","favorites_sub" to "Conteúdo de treino guardado para acesso rápido.","search" to "Pesquisar","search_sub" to "Pesquisa na tua biblioteca desbloqueada.","new" to "NOVO CONTEÚDO","title" to "Título","category" to "Categoria","body" to "Conteúdo","tier" to "Acesso","publish" to "PUBLICADO","draft" to "RASCUNHO","save" to "Guardar conteúdo","delete" to "Eliminar","confirm" to "Confirmar","favorite" to "Guardar","unfavorite" to "Guardado","read" to "Abrir","back" to "Voltar","query" to "Pesquisar técnicas, aulas, tópicos…","none" to "Nenhum conteúdo encontrado.")
    val es=en+mapOf("manager" to "Gestor de Contenido","manager_sub" to "Crea y publica contenido de entrenamiento buscable por nivel de membresía.","vault" to "Biblioteca de Conocimiento","vault_sub" to "Tu biblioteca RS KICKBOX desbloqueada.","favorites" to "Guardados & Favoritos","favorites_sub" to "Contenido guardado para acceso rápido.","search" to "Buscar","search_sub" to "Busca en tu biblioteca desbloqueada.","new" to "NUEVO CONTENIDO","title" to "Título","category" to "Categoría","body" to "Contenido","tier" to "Acceso","publish" to "PUBLICADO","draft" to "BORRADOR","save" to "Guardar contenido","delete" to "Eliminar","confirm" to "Confirmar","favorite" to "Guardar","unfavorite" to "Guardado","read" to "Abrir","back" to "Volver","query" to "Buscar técnicas, lecciones, temas…","none" to "No hay contenido coincidente.")
    val fr=en+mapOf("manager" to "Gestion Contenu","manager_sub" to "Crée et publie du contenu d’entraînement recherchable par niveau d’adhésion.","vault" to "Bibliothèque de Connaissances","vault_sub" to "Ta bibliothèque RS KICKBOX débloquée.","favorites" to "Enregistrés & Favoris","favorites_sub" to "Contenu enregistré pour un accès rapide.","search" to "Rechercher","search_sub" to "Recherche dans ta bibliothèque débloquée.","new" to "NOUVEAU CONTENU","title" to "Titre","category" to "Catégorie","body" to "Contenu","tier" to "Accès","publish" to "PUBLIÉ","draft" to "BROUILLON","save" to "Enregistrer le contenu","delete" to "Supprimer","confirm" to "Confirmer","favorite" to "Enregistrer","unfavorite" to "Enregistré","read" to "Ouvrir","back" to "Retour","query" to "Rechercher techniques, leçons, sujets…","none" to "Aucun contenu correspondant.")
    val de=en+mapOf("manager" to "Content-Manager","manager_sub" to "Erstelle und veröffentliche durchsuchbare Trainingsinhalte nach Mitgliedsstufe.","vault" to "Wissensbibliothek","vault_sub" to "Deine freigeschaltete RS KICKBOX-Trainingsbibliothek.","favorites" to "Gespeichert & Favoriten","favorites_sub" to "Gespeicherte Trainingsinhalte für schnellen Zugriff.","search" to "Suche","search_sub" to "Durchsuche deine freigeschaltete Bibliothek.","new" to "NEUER INHALT","title" to "Titel","category" to "Kategorie","body" to "Inhalt","tier" to "Zugriff","publish" to "VERÖFFENTLICHT","draft" to "ENTWURF","save" to "Inhalt speichern","delete" to "Löschen","confirm" to "Bestätigen","favorite" to "Speichern","unfavorite" to "Gespeichert","read" to "Öffnen","back" to "Zurück","query" to "Techniken, Lektionen, Themen suchen…","none" to "Keine passenden Inhalte.")
    val it=en+mapOf("manager" to "Gestione Contenuti","manager_sub" to "Crea e pubblica contenuti di allenamento ricercabili per livello di abbonamento.","vault" to "Biblioteca Conoscenza","vault_sub" to "La tua libreria RS KICKBOX sbloccata.","favorites" to "Salvati & Preferiti","favorites_sub" to "Contenuti salvati per accesso rapido.","search" to "Cerca","search_sub" to "Cerca nella tua libreria sbloccata.","new" to "NUOVO CONTENUTO","title" to "Titolo","category" to "Categoria","body" to "Contenuto","tier" to "Accesso","publish" to "PUBBLICATO","draft" to "BOZZA","save" to "Salva contenuto","delete" to "Elimina","confirm" to "Conferma","favorite" to "Salva","unfavorite" to "Salvato","read" to "Apri","back" to "Indietro","query" to "Cerca tecniche, lezioni, argomenti…","none" to "Nessun contenuto corrispondente.")
    val pl=en+mapOf("manager" to "Menedżer Treści","manager_sub" to "Twórz i publikuj przeszukiwalne treści treningowe według poziomu członkostwa.","vault" to "Biblioteka Wiedzy","vault_sub" to "Twoja odblokowana biblioteka RS KICKBOX.","favorites" to "Zapisane & Ulubione","favorites_sub" to "Zapisane treści treningowe do szybkiego dostępu.","search" to "Szukaj","search_sub" to "Przeszukuj odblokowaną bibliotekę.","new" to "NOWA TREŚĆ","title" to "Tytuł","category" to "Kategoria","body" to "Treść","tier" to "Dostęp","publish" to "OPUBLIKOWANE","draft" to "SZKIC","save" to "Zapisz treść","delete" to "Usuń","confirm" to "Potwierdź","favorite" to "Zapisz","unfavorite" to "Zapisane","read" to "Otwórz","back" to "Wróć","query" to "Szukaj technik, lekcji, tematów…","none" to "Brak pasujących treści.")
    val tr=en+mapOf("manager" to "İçerik Yönetimi","manager_sub" to "Üyelik seviyesine göre aranabilir antrenman içeriği oluştur ve yayınla.","vault" to "Bilgi Kütüphanesi","vault_sub" to "Açık RS KICKBOX antrenman kütüphanen.","favorites" to "Kaydedilenler & Favoriler","favorites_sub" to "Hızlı erişim için kaydettiğin içerikler.","search" to "Ara","search_sub" to "Açık antrenman kütüphanende ara.","new" to "YENİ İÇERİK","title" to "Başlık","category" to "Kategori","body" to "İçerik","tier" to "Erişim","publish" to "YAYINLANDI","draft" to "TASLAK","save" to "İçeriği kaydet","delete" to "Sil","confirm" to "Onayla","favorite" to "Kaydet","unfavorite" to "Kaydedildi","read" to "Aç","back" to "Geri","query" to "Teknik, ders, konu ara…","none" to "Eşleşen içerik yok.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsContentManagerV48(c:RsPalette,store:RsStore,lang:RsLang){
    var revision by remember{mutableIntStateOf(0)}
    var title by remember{mutableStateOf("")}
    var category by remember{mutableStateOf("TECHNIQUE")}
    var body by remember{mutableStateOf("")}
    var tier by remember{mutableStateOf("ALL")}
    var published by remember{mutableStateOf(true)}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val items=remember(revision){rsLoadContentV48(store)}
    fun save(list:List<RsContentItemV48>){rsSaveContentV48(store,list);revision++}

    RsScroll(c,rsContentUiV48(lang,"manager"),rsContentUiV48(lang,"manager_sub")){
        RsPanel(c){
            Text(rsContentUiV48(lang,"new"),color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(title,{title=it.take(120)},label={Text(rsContentUiV48(lang,"title"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(category,{category=it.take(40)},label={Text(rsContentUiV48(lang,"category"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(body,{body=it.take(8000)},label={Text(rsContentUiV48(lang,"body"))},modifier=Modifier.fillMaxWidth(),minLines=6)
            Text(rsContentUiV48(lang,"tier"),color=c.muted)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                listOf("ALL","BASIC","PRO","ELITE").forEach{x->
                    FilterChip(selected=tier==x,onClick={tier=x},label={Text(x,fontSize=9.sp)},modifier=Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(published)rsContentUiV48(lang,"publish") else rsContentUiV48(lang,"draft"),color=c.muted)
                Switch(published,{published=it})
            }
            Button(
                onClick={
                    save(listOf(RsContentItemV48(UUID.randomUUID().toString(),title.trim(),category.trim(),body.trim(),tier,published))+items)
                    title="";category="TECHNIQUE";body="";tier="ALL";published=true
                },
                enabled=title.isNotBlank()&&body.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsContentUiV48(lang,"save"))}
        }
        items.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black)
                Text(x.category+" · "+x.accessTier+" · "+if(x.published)rsContentUiV48(lang,"publish") else rsContentUiV48(lang,"draft"),color=c.muted)
                Text(x.body.take(220)+(if(x.body.length>220)"…" else ""),color=c.text)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text(if(x.published)rsContentUiV48(lang,"publish") else rsContentUiV48(lang,"draft"),color=c.muted)
                    Switch(x.published,{v->save(items.map{if(it.id==x.id)it.copy(published=v) else it})})
                }
                OutlinedButton(
                    onClick={
                        if(pendingDelete==x.id){save(items.filterNot{it.id==x.id});pendingDelete=null}
                        else pendingDelete=x.id
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(if(pendingDelete==x.id)rsContentUiV48(lang,"confirm") else rsContentUiV48(lang,"delete"))}
            }
        }
    }
}

@Composable
private fun RsContentReaderV48(c:RsPalette,store:RsStore,lang:RsLang,item:RsContentItemV48,onBack:()->Unit){
    val favorites=rsFavoriteIdsV48(store)
    val saved=item.id in favorites
    LaunchedEffect(item.id){rsPushHistoryV48(store,item.id)}
    RsScroll(c,item.title,item.category+" · "+item.accessTier){
        OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text(rsContentUiV48(lang,"back"))}
        RsPanel(c){
            Text(item.body,color=c.text,fontSize=16.sp,lineHeight=24.sp)
        }
        Button(
            onClick={
                val next=favorites.toMutableSet()
                if(saved)next.remove(item.id) else next.add(item.id)
                rsSaveFavoriteIdsV48(store,next)
                onBack()
            },
            modifier=Modifier.fillMaxWidth()
        ){Text(if(saved)rsContentUiV48(lang,"unfavorite") else rsContentUiV48(lang,"favorite"))}
    }
}

@Composable
private fun RsContentListV48(c:RsPalette,store:RsStore,lang:RsLang,title:String,subtitle:String,items:List<RsContentItemV48>){
    var selected by remember{mutableStateOf<RsContentItemV48?>(null)}
    if(selected!=null){
        RsContentReaderV48(c,store,lang,selected!!){selected=null}
        return
    }
    val favorites=rsFavoriteIdsV48(store)
    RsScroll(c,title,subtitle){
        if(items.isEmpty())RsPanel(c){Text(rsContentUiV48(lang,"none"),color=c.muted)}
        items.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(x.category+" · "+x.accessTier,color=c.muted)
                Text(x.body.take(180)+(if(x.body.length>180)"…" else ""),color=c.text)
                Button(onClick={selected=x},modifier=Modifier.fillMaxWidth()){Text(rsContentUiV48(lang,"read"))}
                Text(if(x.id in favorites)"★ "+rsContentUiV48(lang,"unfavorite") else "",color=c.bright,fontSize=10.sp)
            }
        }
    }
}

@Composable
fun RsKnowledgeVaultV48(c:RsPalette,store:RsStore,lang:RsLang)=
    RsContentListV48(c,store,lang,rsContentUiV48(lang,"vault"),rsContentUiV48(lang,"vault_sub"),rsVisibleContentV48(store))

@Composable
fun RsFavoritesV48(c:RsPalette,store:RsStore,lang:RsLang){
    val ids=rsFavoriteIdsV48(store)
    RsContentListV48(c,store,lang,rsContentUiV48(lang,"favorites"),rsContentUiV48(lang,"favorites_sub"),rsVisibleContentV48(store).filter{it.id in ids})
}

@Composable
fun RsSearchV48(c:RsPalette,store:RsStore,lang:RsLang){
    var query by remember{mutableStateOf("")}
    var selected by remember{mutableStateOf<RsContentItemV48?>(null)}
    if(selected!=null){
        RsContentReaderV48(c,store,lang,selected!!){selected=null}
        return
    }
    val all=rsVisibleContentV48(store)
    val results=if(query.isBlank())emptyList() else all.filter{
        it.title.contains(query,true)||it.category.contains(query,true)||it.body.contains(query,true)
    }
    RsScroll(c,rsContentUiV48(lang,"search"),rsContentUiV48(lang,"search_sub")){
        OutlinedTextField(query,{query=it.take(120)},label={Text(rsContentUiV48(lang,"query"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
        if(query.isNotBlank()&&results.isEmpty())RsPanel(c){Text(rsContentUiV48(lang,"none"),color=c.muted)}
        results.forEach{x->
            RsPanel(c){
                Text(x.title,color=c.bright,fontWeight=FontWeight.Black)
                Text(x.category,color=c.muted)
                Button(onClick={selected=x},modifier=Modifier.fillMaxWidth()){Text(rsContentUiV48(lang,"read"))}
            }
        }
    }
}

@Composable
fun RsHistoryV48(c:RsPalette,store:RsStore,lang:RsLang){
    val ids=rsHistoryIdsV48(store)
    val byId=rsVisibleContentV48(store).associateBy{it.id}
    RsContentListV48(c,store,lang,"Training History","Recently opened training content.",ids.mapNotNull{byId[it]})
}

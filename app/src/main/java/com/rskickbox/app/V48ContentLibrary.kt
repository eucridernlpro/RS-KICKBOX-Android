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

private fun rsContentRankV48(tier:String)=when(tier.uppercase()){
    "ALL"->0;"BASIC"->1;"PRO"->2;"ELITE"->3;else->99
}
private fun rsContentStudentTierV48(store:RsStore):String{
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
    val nl=en+mapOf(
        "manager" to "Contentbeheer","manager_sub" to "Maak en publiceer doorzoekbare trainingscontent per abonnementsniveau.",
        "vault" to "Kennisbibliotheek","vault_sub" to "Jouw vrijgegeven RS KICKBOX trainingsbibliotheek.",
        "favorites" to "Opgeslagen & Favorieten","favorites_sub" to "Trainingscontent die je hebt opgeslagen.",
        "search" to "Zoeken","search_sub" to "Zoek in jouw vrijgegeven trainingsbibliotheek.",
        "new" to "NIEUWE CONTENT","title" to "Titel","category" to "Categorie","body" to "Inhoud",
        "tier" to "Toegang","publish" to "GEPUBLICEERD","draft" to "CONCEPT","save" to "Content opslaan",
        "delete" to "Verwijderen","confirm" to "Bevestigen","favorite" to "Opslaan","unfavorite" to "Opgeslagen",
        "read" to "Openen","back" to "Terug","query" to "Zoek technieken, lessen, onderwerpen…","none" to "Geen overeenkomende content."
    )
    val pack=when(lang.code){"nl"->nl;else->en}
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

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

data class RsProfileV50(
    val email:String,val displayName:String,val bio:String,val goal:String,val publicProfile:Boolean
)

data class RsCommunityPostV50(
    val id:String,val authorEmail:String,val authorName:String,val body:String,val createdAt:Long,val active:Boolean
)

data class RsGroupV50(
    val id:String,val name:String,val description:String,val active:Boolean
)

private fun rsProfileKeyV50(email:String)="profile_v50_"+email.lowercase().replace(Regex("[^a-z0-9]"),"_")
private fun rsLoadProfileV50(store:RsStore,email:String,name:String):RsProfileV50{
    val raw=store.s(rsProfileKeyV50(email),"")
    if(raw.isBlank())return RsProfileV50(email,name,"","Improve technique, fitness and consistency.",false)
    return runCatching{
        val o=JSONObject(raw)
        RsProfileV50(
            email,
            o.optString("displayName",name),
            o.optString("bio"),
            o.optString("goal"),
            o.optBoolean("publicProfile",false)
        )
    }.getOrElse{RsProfileV50(email,name,"","Improve technique, fitness and consistency.",false)}
}
private fun rsSaveProfileV50(store:RsStore,p:RsProfileV50)=store.ps(
    rsProfileKeyV50(p.email),
    JSONObject().apply{
        put("displayName",p.displayName);put("bio",p.bio);put("goal",p.goal);put("publicProfile",p.publicProfile)
    }.toString()
)

private fun rsLoadPostsV50(store:RsStore):List<RsCommunityPostV50>{
    val raw=store.s("community_posts_v50","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsCommunityPostV50(
                    o.optString("id"),o.optString("authorEmail"),o.optString("authorName"),
                    o.optString("body"),o.optLong("createdAt"),o.optBoolean("active",true)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSavePostsV50(store:RsStore,items:List<RsCommunityPostV50>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("authorEmail",x.authorEmail);put("authorName",x.authorName)
        put("body",x.body);put("createdAt",x.createdAt);put("active",x.active)
    })}
    store.ps("community_posts_v50",a.toString())
}

private fun rsSeedGroupsV50()=listOf(
    RsGroupV50("fundamentals","Fundamentals Crew","Technique, balance and basics.",true),
    RsGroupV50("sparring","Sparring Team","Controlled sparring development and preparation.",true),
    RsGroupV50("fightcamp","Fight Camp Group","Training focus for active fight-camp members.",true)
)
private fun rsLoadGroupsV50(store:RsStore):List<RsGroupV50>{
    val raw=store.s("groups_v50","")
    if(raw.isBlank()){
        rsSaveGroupsV50(store,rsSeedGroupsV50())
        return rsSeedGroupsV50()
    }
    return runCatching{
        val a=JSONArray(raw)
        buildList{
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                add(RsGroupV50(o.optString("id"),o.optString("name"),o.optString("description"),o.optBoolean("active",true)))
            }
        }
    }.getOrDefault(emptyList())
}
private fun rsSaveGroupsV50(store:RsStore,items:List<RsGroupV50>){
    val a=JSONArray()
    items.forEach{x->a.put(JSONObject().apply{
        put("id",x.id);put("name",x.name);put("description",x.description);put("active",x.active)
    })}
    store.ps("groups_v50",a.toString())
}
private fun rsJoinedGroupsV50(store:RsStore,email:String):Set<String> =
    store.s("joined_groups_v50_"+email.lowercase().replace(Regex("[^a-z0-9]"),"_"),"")
        .split(',').filter{it.isNotBlank()}.toSet()
private fun rsSaveJoinedGroupsV50(store:RsStore,email:String,ids:Set<String>)=
    store.ps("joined_groups_v50_"+email.lowercase().replace(Regex("[^a-z0-9]"),"_"),ids.joinToString(","))

fun rsSocialPrivacyRawV50(store:RsStore,email:String):String{
    val name=store.s("session_student_name","")
    val profile=rsLoadProfileV50(store,email,name)
    val posts=rsLoadPostsV50(store).filter{it.authorEmail.equals(email,true)}
    val joined=rsJoinedGroupsV50(store,email)
    return JSONObject().apply{
        put("profile",JSONObject().apply{
            put("display_name",profile.displayName);put("bio",profile.bio);put("training_goal",profile.goal);put("public_profile",profile.publicProfile)
        })
        put("posts",JSONArray().apply{
            posts.forEach{x->put(JSONObject().apply{put("id",x.id);put("body",x.body);put("created_at",x.createdAt);put("active",x.active)})}
        })
        put("joined_group_ids",JSONArray(joined.toList()))
    }.toString()
}

fun rsRemoveSocialDataForStudentV50(store:RsStore,email:String){
    store.ps(rsProfileKeyV50(email),"")
    rsSavePostsV50(store,rsLoadPostsV50(store).filterNot{it.authorEmail.equals(email,true)})
    rsSaveJoinedGroupsV50(store,email,emptySet())
}

private fun rsSocialUiV50(lang:RsLang,key:String):String{
    val en=mapOf(
        "profile" to "My Profile","profile_sub" to "Your RS KICKBOX identity and privacy controls.",
        "display" to "Display name","bio" to "Bio","goal" to "Training goal","public" to "Public profile","save" to "Save profile",
        "community" to "Community","community_sub" to "Member updates inside the RS KICKBOX community.",
        "write" to "Share an update…","post" to "Post","delete" to "Delete","confirm" to "Confirm",
        "groups" to "Groups","groups_sub" to "Join training groups that match your goals.","join" to "Join","leave" to "Leave",
        "manager" to "Community & Groups","manager_sub" to "Moderate posts and manage available training groups.",
        "new_group" to "New group","name" to "Group name","description" to "Description","create" to "Create group",
        "active" to "ACTIVE","inactive" to "INACTIVE","none" to "Nothing here yet."
    )
    val nl=en+mapOf(
        "profile" to "Mijn Profiel","profile_sub" to "Jouw RS KICKBOX-identiteit en privacy-instellingen.",
        "display" to "Weergavenaam","bio" to "Bio","goal" to "Trainingsdoel","public" to "Openbaar profiel","save" to "Profiel opslaan",
        "community" to "Community","community_sub" to "Updates van leden binnen de RS KICKBOX-community.",
        "write" to "Deel een update…","post" to "Plaatsen","delete" to "Verwijderen","confirm" to "Bevestigen",
        "groups" to "Groepen","groups_sub" to "Sluit aan bij trainingsgroepen die bij je doelen passen.","join" to "Deelnemen","leave" to "Verlaten",
        "manager" to "Community & Groepen","manager_sub" to "Modereer berichten en beheer beschikbare trainingsgroepen.",
        "new_group" to "Nieuwe groep","name" to "Groepsnaam","description" to "Beschrijving","create" to "Groep maken",
        "active" to "ACTIEF","inactive" to "INACTIEF","none" to "Nog niets hier."
    )
    val pack=when(lang.code){"nl"->nl;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsProfileV50(c:RsPalette,store:RsStore,lang:RsLang){
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val sessionName=store.s("session_student_name","Alex de Vries")
    var profile by remember(email){mutableStateOf(rsLoadProfileV50(store,email,sessionName))}
    var saved by remember{mutableStateOf(false)}

    RsScroll(c,rsSocialUiV50(lang,"profile"),rsSocialUiV50(lang,"profile_sub")){
        RsPanel(c){
            Text(email,color=c.muted,fontSize=10.sp)
            OutlinedTextField(profile.displayName,{profile=profile.copy(displayName=it.take(80));saved=false},label={Text(rsSocialUiV50(lang,"display"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(profile.bio,{profile=profile.copy(bio=it.take(500));saved=false},label={Text(rsSocialUiV50(lang,"bio"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            OutlinedTextField(profile.goal,{profile=profile.copy(goal=it.take(300));saved=false},label={Text(rsSocialUiV50(lang,"goal"))},modifier=Modifier.fillMaxWidth(),minLines=2)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(rsSocialUiV50(lang,"public"),color=c.text)
                Switch(profile.publicProfile,{profile=profile.copy(publicProfile=it);saved=false})
            }
            Button(
                onClick={rsSaveProfileV50(store,profile);store.ps("session_student_name",profile.displayName);saved=true},
                enabled=profile.displayName.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsSocialUiV50(lang,"save"))}
            if(saved)Text("✓",color=c.bright)
        }
    }
}

@Composable
fun RsCommunityV50(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    var revision by remember{mutableIntStateOf(0)}
    var draft by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")
    val posts=remember(revision){rsLoadPostsV50(store).sortedByDescending{it.createdAt}}
    fun save(list:List<RsCommunityPostV50>){rsSavePostsV50(store,list);revision++}

    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsSocialUiV50(lang,"manager") else rsSocialUiV50(lang,"community"),
        if(role==RsRole.TRAINER)rsSocialUiV50(lang,"manager_sub") else rsSocialUiV50(lang,"community_sub")
    ){
        if(role==RsRole.STUDENT)RsPanel(c){
            OutlinedTextField(draft,{draft=it.take(1000)},label={Text(rsSocialUiV50(lang,"write"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Button(
                onClick={
                    save(listOf(RsCommunityPostV50(UUID.randomUUID().toString(),email,name,draft.trim(),System.currentTimeMillis(),true))+posts)
                    draft=""
                },
                enabled=draft.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsSocialUiV50(lang,"post"))}
        }
        val visible=posts.filter{it.active||role==RsRole.TRAINER}
        if(visible.isEmpty())RsPanel(c){Text(rsSocialUiV50(lang,"none"),color=c.muted)}
        visible.forEach{x->
            RsPanel(c){
                Text(x.authorName,color=c.bright,fontWeight=FontWeight.Black)
                Text(x.body,color=c.text)
                Text(SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(Date(x.createdAt)),color=c.muted,fontSize=9.sp)
                if(role==RsRole.TRAINER){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(x.active)rsSocialUiV50(lang,"active") else rsSocialUiV50(lang,"inactive"),color=c.muted)
                        Switch(x.active,{v->save(posts.map{if(it.id==x.id)it.copy(active=v) else it})})
                    }
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==x.id){save(posts.filterNot{it.id==x.id});pendingDelete=null}
                            else pendingDelete=x.id
                        },modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==x.id)rsSocialUiV50(lang,"confirm") else rsSocialUiV50(lang,"delete"))}
                }else if(x.authorEmail.equals(email,true)){
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==x.id){save(posts.filterNot{it.id==x.id});pendingDelete=null}
                            else pendingDelete=x.id
                        },modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==x.id)rsSocialUiV50(lang,"confirm") else rsSocialUiV50(lang,"delete"))}
                }
            }
        }
    }
}

@Composable
fun RsGroupsV50(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    var revision by remember{mutableIntStateOf(0)}
    var name by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val groups=remember(revision){rsLoadGroupsV50(store)}
    val joined=remember(revision){rsJoinedGroupsV50(store,email)}
    fun save(list:List<RsGroupV50>){rsSaveGroupsV50(store,list);revision++}

    RsScroll(c,rsSocialUiV50(lang,"groups"),rsSocialUiV50(lang,"groups_sub")){
        if(role==RsRole.TRAINER)RsPanel(c){
            Text(rsSocialUiV50(lang,"new_group"),color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(name,{name=it.take(80)},label={Text(rsSocialUiV50(lang,"name"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(description,{description=it.take(500)},label={Text(rsSocialUiV50(lang,"description"))},modifier=Modifier.fillMaxWidth(),minLines=2)
            Button(
                onClick={
                    save(listOf(RsGroupV50(UUID.randomUUID().toString(),name.trim(),description.trim(),true))+groups)
                    name="";description=""
                },
                enabled=name.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsSocialUiV50(lang,"create"))}
        }
        groups.filter{it.active||role==RsRole.TRAINER}.forEach{g->
            val isJoined=g.id in joined
            RsPanel(c){
                Text(g.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(g.description,color=c.text)
                if(role==RsRole.STUDENT){
                    Button(
                        onClick={
                            val next=joined.toMutableSet()
                            if(isJoined)next.remove(g.id) else next.add(g.id)
                            rsSaveJoinedGroupsV50(store,email,next)
                            revision++
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(isJoined)rsSocialUiV50(lang,"leave") else rsSocialUiV50(lang,"join"))}
                }else{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(g.active)rsSocialUiV50(lang,"active") else rsSocialUiV50(lang,"inactive"),color=c.muted)
                        Switch(g.active,{v->save(groups.map{if(it.id==g.id)it.copy(active=v) else it})})
                    }
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==g.id){save(groups.filterNot{it.id==g.id});pendingDelete=null}
                            else pendingDelete=g.id
                        },modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==g.id)rsSocialUiV50(lang,"confirm") else rsSocialUiV50(lang,"delete"))}
                }
            }
        }
    }
}

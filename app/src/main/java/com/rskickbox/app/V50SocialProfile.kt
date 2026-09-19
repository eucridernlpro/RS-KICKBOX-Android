package com.rskickbox.app

import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

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

private fun rsProfilePrivacyNoteV109(lang:RsLang,cloud:Boolean):String=when(lang.code){
    "nl"->if(cloud)"Je trainer kan je profielidentiteit zien. Andere leden zien profielgegevens alleen wanneer Openbaar profiel is ingeschakeld." else "Je profielfoto is zichtbaar voor je trainer en in ledenonderdelen waar je profiel mag worden getoond."
    "pt"->if(cloud)"O treinador pode ver a tua identidade de perfil. Outros membros só veem detalhes quando o Perfil público está ativo." else "A tua foto de perfil é visível ao treinador e nas áreas de membros onde o teu perfil pode ser mostrado."
    "es"->if(cloud)"Tu entrenador puede ver tu identidad de perfil. Otros miembros solo ven detalles cuando Perfil público está activado." else "Tu foto de perfil es visible para tu entrenador y en las áreas de miembros donde tu perfil puede mostrarse."
    "fr"->if(cloud)"Ton entraîneur peut voir ton identité de profil. Les autres membres voient les détails uniquement si Profil public est activé." else "Ta photo de profil est visible par ton entraîneur et dans les espaces membres où ton profil peut être affiché."
    "de"->if(cloud)"Dein Trainer kann deine Profilidentität sehen. Andere Mitglieder sehen Profildetails nur bei aktiviertem öffentlichen Profil." else "Dein Profilfoto ist für deinen Trainer und in freigegebenen Mitgliederbereichen sichtbar."
    "it"->if(cloud)"Il tuo allenatore può vedere l’identità del profilo. Gli altri membri vedono i dettagli solo se Profilo pubblico è attivo." else "La foto profilo è visibile al tuo allenatore e nelle aree membri in cui il profilo può essere mostrato."
    "pl"->if(cloud)"Trener widzi tożsamość profilu. Inni członkowie widzą szczegóły tylko po włączeniu profilu publicznego." else "Zdjęcie profilowe jest widoczne dla trenera i w obszarach członkowskich, w których profil może być wyświetlany."
    "tr"->if(cloud)"Antrenörün profil kimliğini görebilir. Diğer üyeler ayrıntıları yalnızca Herkese açık profil açıkken görür." else "Profil fotoğrafın antrenörün ve profilinin gösterilmesine izin verilen üye alanlarında görünür."
    else->if(cloud)rsProfilePrivacyNoteV109(lang,true) else rsProfilePrivacyNoteV109(lang,false)
}

private fun rsSocialUiV50(lang:RsLang,key:String):String{
    val en=mapOf(
        "profile" to "My Profile","profile_sub" to "Your RS KICKBOXING identity and privacy controls.",
        "display" to "Display name","bio" to "Bio","goal" to "Training goal","public" to "Public profile","save" to "Save profile",
        "community" to "Community","community_sub" to "Member updates inside the RS KICKBOXING community.",
        "write" to "Share an update…","post" to "Post","delete" to "Delete","confirm" to "Confirm",
        "groups" to "Groups","groups_sub" to "Join training groups that match your goals.","join" to "Join","leave" to "Leave",
        "manager" to "Community & Groups","manager_sub" to "Moderate posts and manage available training groups.",
        "new_group" to "New group","name" to "Group name","description" to "Description","create" to "Create group",
        "active" to "ACTIVE","inactive" to "INACTIVE","none" to "Nothing here yet."
    )
    val nl=en+mapOf("profile" to "Mijn Profiel","profile_sub" to "Jouw RS KICKBOXING-identiteit en privacy-instellingen.","display" to "Weergavenaam","bio" to "Bio","goal" to "Trainingsdoel","public" to "Openbaar profiel","save" to "Profiel opslaan","community" to "Community","community_sub" to "Updates van leden binnen de RS KICKBOXING-community.","write" to "Deel een update…","post" to "Plaatsen","delete" to "Verwijderen","confirm" to "Bevestigen","groups" to "Groepen","groups_sub" to "Sluit aan bij trainingsgroepen die bij je doelen passen.","join" to "Deelnemen","leave" to "Verlaten","manager" to "Community & Groepen","manager_sub" to "Modereer berichten en beheer beschikbare trainingsgroepen.","new_group" to "Nieuwe groep","name" to "Groepsnaam","description" to "Beschrijving","create" to "Groep maken","active" to "ACTIEF","inactive" to "INACTIEF","none" to "Nog niets hier.")
    val pt=en+mapOf("profile" to "Meu Perfil","profile_sub" to "A tua identidade RS KICKBOXING e controlos de privacidade.","display" to "Nome visível","bio" to "Bio","goal" to "Objetivo de treino","public" to "Perfil público","save" to "Guardar perfil","community" to "Comunidade","community_sub" to "Atualizações dos membros na comunidade RS KICKBOXING.","write" to "Partilha uma atualização…","post" to "Publicar","delete" to "Eliminar","confirm" to "Confirmar","groups" to "Grupos","groups_sub" to "Entra em grupos de treino que combinam com os teus objetivos.","join" to "Entrar","leave" to "Sair","manager" to "Comunidade & Grupos","manager_sub" to "Modera publicações e gere grupos de treino disponíveis.","new_group" to "Novo grupo","name" to "Nome do grupo","description" to "Descrição","create" to "Criar grupo","active" to "ATIVO","inactive" to "INATIVO","none" to "Ainda não há nada aqui.")
    val es=en+mapOf("profile" to "Mi Perfil","profile_sub" to "Tu identidad RS KICKBOXING y controles de privacidad.","display" to "Nombre visible","bio" to "Bio","goal" to "Objetivo de entrenamiento","public" to "Perfil público","save" to "Guardar perfil","community" to "Comunidad","community_sub" to "Actualizaciones de miembros dentro de la comunidad RS KICKBOXING.","write" to "Comparte una actualización…","post" to "Publicar","delete" to "Eliminar","confirm" to "Confirmar","groups" to "Grupos","groups_sub" to "Únete a grupos de entrenamiento que encajen con tus objetivos.","join" to "Unirse","leave" to "Salir","manager" to "Comunidad & Grupos","manager_sub" to "Modera publicaciones y gestiona grupos disponibles.","new_group" to "Nuevo grupo","name" to "Nombre del grupo","description" to "Descripción","create" to "Crear grupo","active" to "ACTIVO","inactive" to "INACTIVO","none" to "Todavía no hay nada aquí.")
    val fr=en+mapOf("profile" to "Mon Profil","profile_sub" to "Ton identité RS KICKBOXING et tes réglages de confidentialité.","display" to "Nom affiché","bio" to "Bio","goal" to "Objectif d’entraînement","public" to "Profil public","save" to "Enregistrer le profil","community" to "Communauté","community_sub" to "Actualités des membres dans la communauté RS KICKBOXING.","write" to "Partager une mise à jour…","post" to "Publier","delete" to "Supprimer","confirm" to "Confirmer","groups" to "Groupes","groups_sub" to "Rejoins les groupes d’entraînement adaptés à tes objectifs.","join" to "Rejoindre","leave" to "Quitter","manager" to "Communauté & Groupes","manager_sub" to "Modère les publications et gère les groupes disponibles.","new_group" to "Nouveau groupe","name" to "Nom du groupe","description" to "Description","create" to "Créer le groupe","active" to "ACTIF","inactive" to "INACTIF","none" to "Rien ici pour le moment.")
    val de=en+mapOf("profile" to "Mein Profil","profile_sub" to "Deine RS KICKBOXING-Identität und Datenschutzeinstellungen.","display" to "Anzeigename","bio" to "Bio","goal" to "Trainingsziel","public" to "Öffentliches Profil","save" to "Profil speichern","community" to "Community","community_sub" to "Mitglieder-Updates in der RS KICKBOXING-Community.","write" to "Update teilen…","post" to "Posten","delete" to "Löschen","confirm" to "Bestätigen","groups" to "Gruppen","groups_sub" to "Tritt Trainingsgruppen bei, die zu deinen Zielen passen.","join" to "Beitreten","leave" to "Verlassen","manager" to "Community & Gruppen","manager_sub" to "Moderiere Beiträge und verwalte verfügbare Trainingsgruppen.","new_group" to "Neue Gruppe","name" to "Gruppenname","description" to "Beschreibung","create" to "Gruppe erstellen","active" to "AKTIV","inactive" to "INAKTIV","none" to "Noch nichts hier.")
    val it=en+mapOf("profile" to "Il Mio Profilo","profile_sub" to "La tua identità RS KICKBOXING e i controlli privacy.","display" to "Nome visualizzato","bio" to "Bio","goal" to "Obiettivo di allenamento","public" to "Profilo pubblico","save" to "Salva profilo","community" to "Community","community_sub" to "Aggiornamenti dei membri nella community RS KICKBOXING.","write" to "Condividi un aggiornamento…","post" to "Pubblica","delete" to "Elimina","confirm" to "Conferma","groups" to "Gruppi","groups_sub" to "Entra nei gruppi di allenamento adatti ai tuoi obiettivi.","join" to "Entra","leave" to "Esci","manager" to "Community & Gruppi","manager_sub" to "Modera i post e gestisci i gruppi disponibili.","new_group" to "Nuovo gruppo","name" to "Nome gruppo","description" to "Descrizione","create" to "Crea gruppo","active" to "ATTIVO","inactive" to "INATTIVO","none" to "Ancora nulla qui.")
    val pl=en+mapOf("profile" to "Mój Profil","profile_sub" to "Twoja tożsamość RS KICKBOXING i ustawienia prywatności.","display" to "Nazwa wyświetlana","bio" to "Bio","goal" to "Cel treningowy","public" to "Profil publiczny","save" to "Zapisz profil","community" to "Społeczność","community_sub" to "Aktualności członków społeczności RS KICKBOXING.","write" to "Udostępnij aktualizację…","post" to "Opublikuj","delete" to "Usuń","confirm" to "Potwierdź","groups" to "Grupy","groups_sub" to "Dołącz do grup treningowych pasujących do Twoich celów.","join" to "Dołącz","leave" to "Opuść","manager" to "Społeczność & Grupy","manager_sub" to "Moderuj posty i zarządzaj dostępnymi grupami.","new_group" to "Nowa grupa","name" to "Nazwa grupy","description" to "Opis","create" to "Utwórz grupę","active" to "AKTYWNE","inactive" to "NIEAKTYWNE","none" to "Jeszcze nic tu nie ma.")
    val tr=en+mapOf("profile" to "Profilim","profile_sub" to "RS KICKBOXING kimliğin ve gizlilik kontrollerin.","display" to "Görünen ad","bio" to "Bio","goal" to "Antrenman hedefi","public" to "Herkese açık profil","save" to "Profili kaydet","community" to "Topluluk","community_sub" to "RS KICKBOXING topluluğundaki üye güncellemeleri.","write" to "Bir güncelleme paylaş…","post" to "Paylaş","delete" to "Sil","confirm" to "Onayla","groups" to "Gruplar","groups_sub" to "Hedeflerine uygun antrenman gruplarına katıl.","join" to "Katıl","leave" to "Ayrıl","manager" to "Topluluk & Gruplar","manager_sub" to "Gönderileri yönet ve kullanılabilir antrenman gruplarını düzenle.","new_group" to "Yeni grup","name" to "Grup adı","description" to "Açıklama","create" to "Grup oluştur","active" to "AKTİF","inactive" to "PASİF","none" to "Henüz burada bir şey yok.")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

@Composable
fun RsProfileV50(c:RsPalette,store:RsStore,lang:RsLang){
    if(RsSupabaseV60.configured){RsCloudProfileV84(c,store,lang);return}
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val sessionName=store.s("session_student_name","Alex de Vries")
    var profile by remember(email){mutableStateOf(rsLoadProfileV50(store,email,sessionName))}
    var saved by remember{mutableStateOf(false)}
    var avatarBusy by remember{mutableStateOf(false)}
    var avatarStatus by remember{mutableStateOf("")}
    var avatarRefresh by remember{mutableIntStateOf(0)}

    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null && !avatarBusy){
            avatarBusy=true
            avatarStatus=rsCloudT93(lang,"uploading_photo")
            scope.launch{
                rsUploadMyAvatarV68(context,uri)
                    .onSuccess{
                        avatarRefresh++
                        avatarStatus=rsCloudT93(lang,"photo_updated")
                    }
                    .onFailure{
                        avatarStatus=rsReleaseT98(lang,"save_failed")
                    }
                avatarBusy=false
            }
        }
    }

    RsScroll(c,rsSocialUiV50(lang,"profile"),rsSocialUiV50(lang,"profile_sub")){
        RsPanel(c){
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(14.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                RsMemberAvatarV68(
                    c=c,
                    email=email,
                    name=profile.displayName.ifBlank{sessionName},
                    size=88.dp,
                    refreshKey=avatarRefresh
                )
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(
                        profile.displayName.ifBlank{sessionName},
                        color=c.bright,
                        fontWeight=FontWeight.Black,
                        fontSize=20.sp
                    )
                    Text(email,color=c.muted,fontSize=10.sp)
                    OutlinedButton(
                        onClick={
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled=!avatarBusy,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(if(avatarBusy)"Uploading…" else "Upload / change profile photo",fontSize=10.sp)
                    }
                }
            }
            if(avatarStatus.isNotBlank()){
                Text(
                    avatarStatus,
                    color=if(avatarStatus.startsWith("✓"))c.bright else c.muted,
                    fontSize=10.sp
                )
            }

            OutlinedTextField(
                profile.displayName,
                {profile=profile.copy(displayName=it.take(80));saved=false},
                label={Text(rsSocialUiV50(lang,"display"))},
                modifier=Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                profile.bio,
                {profile=profile.copy(bio=it.take(500));saved=false},
                label={Text(rsSocialUiV50(lang,"bio"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=3
            )
            OutlinedTextField(
                profile.goal,
                {profile=profile.copy(goal=it.take(300));saved=false},
                label={Text(rsSocialUiV50(lang,"goal"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically
            ){
                Text(rsSocialUiV50(lang,"public"),color=c.text)
                Switch(profile.publicProfile,{profile=profile.copy(publicProfile=it);saved=false})
            }
            Text(
                "Your profile photo is visible to your trainer and in member areas where your profile is allowed to be shown.",
                color=c.muted,
                fontSize=9.sp
            )
            Button(
                onClick={
                    rsSaveProfileV50(store,profile)
                    store.ps("session_student_name",profile.displayName)
                    saved=true
                },
                enabled=profile.displayName.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(rsSocialUiV50(lang,"save"))}
            if(saved)Text("✓",color=c.bright)
        }
    }
}
@Composable
fun RsCommunityV50(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    if(RsSupabaseV60.configured){RsCloudCommunityV84(c,store,lang,role);return}
    var revision by remember{mutableIntStateOf(0)}
    var draft by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val email=store.s("session_student_email","alex@rskickbox.nl")
    val name=store.s("session_student_name","Alex de Vries")
    val posts=remember(revision){rsLoadPostsV50(store).sortedByDescending{it.createdAt}}
    val postingAllowed=rsOpsEnabledV56(store,RsOpsKeysV56.COMMUNITY_POSTS,true)
    fun save(list:List<RsCommunityPostV50>){rsSavePostsV50(store,list);revision++}

    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsSocialUiV50(lang,"manager") else rsSocialUiV50(lang,"community"),
        if(role==RsRole.TRAINER)rsSocialUiV50(lang,"manager_sub") else rsSocialUiV50(lang,"community_sub")
    ){
        if(role==RsRole.STUDENT)RsPanel(c){
            if(!postingAllowed)Text(rsOpsUiV56(lang,"community_disabled"),color=c.muted,fontSize=10.sp)
            OutlinedTextField(draft,{draft=it.take(1000)},label={Text(rsSocialUiV50(lang,"write"))},modifier=Modifier.fillMaxWidth(),minLines=3)
            Button(
                onClick={
                    save(listOf(RsCommunityPostV50(UUID.randomUUID().toString(),email,name,draft.trim(),System.currentTimeMillis(),true))+posts)
                    draft=""
                },
                enabled=draft.isNotBlank()&&postingAllowed,
                modifier=Modifier.fillMaxWidth()
            ){Text(rsSocialUiV50(lang,"post"))}
        }
        val visible=posts.filter{it.active||role==RsRole.TRAINER}
        if(visible.isEmpty())RsPanel(c){Text(rsSocialUiV50(lang,"none"),color=c.muted)}
        visible.forEach{x->
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,x.authorEmail,x.authorName,size=38.dp)
                    Text(x.authorName,color=c.bright,fontWeight=FontWeight.Black)
                }
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
    if(RsSupabaseV60.configured){RsCloudGroupsScreenV84(c,lang,role);return}
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


@Composable
private fun RsCloudProfileV84(c:RsPalette,store:RsStore,lang:RsLang){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val email=store.s("session_student_email","")
    var displayName by remember{mutableStateOf(store.s("session_student_name",""))}
    var bio by remember{mutableStateOf("")}
    var goal by remember{mutableStateOf("")}
    var publicProfile by remember{mutableStateOf(false)}
    var loading by remember{mutableStateOf(true)}
    var saving by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    var avatarBusy by remember{mutableStateOf(false)}
    var avatarRefresh by remember{mutableIntStateOf(0)}

    LaunchedEffect(Unit){
        loading=true
        rsCloudMySocialProfileV84()
            .onSuccess{
                displayName=it.displayName
                bio=it.bio
                goal=it.trainingGoal
                publicProfile=it.publicProfile
                store.ps("session_student_name",it.displayName)
            }
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->
        if(uri!=null && !avatarBusy){
            avatarBusy=true
            status="Uploading profile photo…"
            scope.launch{
                rsUploadMyAvatarV68(context,uri)
                    .onSuccess{
                        avatarRefresh++
                        status="✓ Profile photo updated."
                    }
                    .onFailure{status=rsReleaseT98(lang,"save_failed")}
                avatarBusy=false
            }
        }
    }

    RsScroll(c,rsSocialUiV50(lang,"profile"),rsSocialUiV50(lang,"profile_sub")){
        RsPanel(c){
            Text(
                if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }
        RsPanel(c){
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(14.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                RsMemberAvatarV68(c,email,displayName,size=88.dp,refreshKey=avatarRefresh)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(displayName.ifBlank{email},color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
                    Text(email,color=c.muted,fontSize=10.sp)
                    OutlinedButton(
                        onClick={
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled=!avatarBusy&&!loading,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(if(avatarBusy)"Uploading…" else "Upload / change profile photo",fontSize=10.sp)
                    }
                }
            }
            OutlinedTextField(
                displayName,
                {displayName=it.take(80)},
                label={Text(rsSocialUiV50(lang,"display"))},
                modifier=Modifier.fillMaxWidth(),
                enabled=!saving&&!loading
            )
            OutlinedTextField(
                bio,
                {bio=it.take(500)},
                label={Text(rsSocialUiV50(lang,"bio"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=3,
                enabled=!saving&&!loading
            )
            OutlinedTextField(
                goal,
                {goal=it.take(300)},
                label={Text(rsSocialUiV50(lang,"goal"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=2,
                enabled=!saving&&!loading
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically
            ){
                Text(rsSocialUiV50(lang,"public"),color=c.text)
                Switch(publicProfile,{publicProfile=it},enabled=!saving&&!loading)
            }
            Text(
                "Your trainer can see your profile identity. Other members only see profile details when Public profile is enabled.",
                color=c.muted,
                fontSize=9.sp
            )
            Button(
                onClick={
                    saving=true
                    status=rsCloudT93(lang,"saving_profile")
                    scope.launch{
                        rsSaveCloudSocialProfileV84(displayName,bio,goal,publicProfile)
                            .onSuccess{
                                store.ps("session_student_name",displayName)
                                status=rsCloudT93(lang,"profile_saved")
                            }
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        saving=false
                    }
                },
                enabled=!saving&&!loading&&displayName.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text(if(saving)rsCloudT93(lang,"saving") else rsSocialUiV50(lang,"save"))}
        }
    }
}

@Composable
private fun RsCloudCommunityV84(c:RsPalette,store:RsStore,lang:RsLang,role:RsRole){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var posts by remember{mutableStateOf<List<RsCloudCommunityPostV84>>(emptyList())}
    var draft by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}
    val ownEmail=store.s("session_student_email","")
    val postingAllowed=rsOpsEnabledV56(store,RsOpsKeysV56.COMMUNITY_POSTS,true)

    LaunchedEffect(revision){
        loading=true
        rsCloudCommunityV84()
            .onSuccess{posts=it}
            .onFailure{status=rsReleaseT98(lang,"load_failed")}
        loading=false
    }

    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsSocialUiV50(lang,"manager") else rsSocialUiV50(lang,"community"),
        if(role==RsRole.TRAINER)rsSocialUiV50(lang,"manager_sub") else rsSocialUiV50(lang,"community_sub")
    ){
        RsPanel(c){
            Text(
                if(loading)rsCloudT93(lang,"syncing") else rsCloudT93(lang,"connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        if(role==RsRole.STUDENT)RsPanel(c){
            if(!postingAllowed)Text(rsOpsUiV56(lang,"community_disabled"),color=c.muted,fontSize=10.sp)
            OutlinedTextField(
                draft,
                {draft=it.take(1000)},
                label={Text(rsSocialUiV50(lang,"write"))},
                modifier=Modifier.fillMaxWidth(),
                minLines=3,
                enabled=busyId==null&&postingAllowed
            )
            Button(
                onClick={
                    busyId="new"
                    scope.launch{
                        rsCreateCloudCommunityPostV84(draft.trim())
                            .onSuccess{draft="";status=rsCloudT93(lang,"post_published");revision++}
                            .onFailure{status=rsReleaseT98(lang,"save_failed")}
                        busyId=null
                    }
                },
                enabled=draft.isNotBlank()&&postingAllowed&&busyId==null,
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busyId=="new")rsCloudT93(lang,"saving") else rsSocialUiV50(lang,"post"))}
        }

        if(posts.isEmpty()&&!loading)RsPanel(c){Text(rsSocialUiV50(lang,"none"),color=c.muted)}

        posts.forEach{x->
            RsPanel(c){
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    RsMemberAvatarV68(c,x.authorEmail,x.authorName,size=40.dp)
                    Column(Modifier.weight(1f)){
                        Text(x.authorName.ifBlank{x.authorEmail},color=c.bright,fontWeight=FontWeight.Black)
                        Text(
                            SimpleDateFormat("dd MMM · HH:mm",Locale.getDefault()).format(Date(x.createdMillis())),
                            color=c.muted,
                            fontSize=9.sp
                        )
                    }
                }
                Text(x.body,color=c.text)

                if(role==RsRole.TRAINER){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(x.active)rsSocialUiV50(lang,"active") else rsSocialUiV50(lang,"inactive"),color=c.muted)
                        Switch(
                            x.active,
                            {value->
                                busyId=x.id
                                scope.launch{
                                    rsSetCloudCommunityPostActiveV84(x.id,value)
                                        .onSuccess{revision++}
                                        .onFailure{status=rsReleaseT98(lang,"update_failed")}
                                    busyId=null
                                }
                            },
                            enabled=busyId==null
                        )
                    }
                }

                if(role==RsRole.TRAINER || x.authorEmail.equals(ownEmail,true)){
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==x.id){
                                busyId=x.id
                                scope.launch{
                                    rsDeleteCloudCommunityPostV84(x.id)
                                        .onSuccess{pendingDelete=null;status=rsCloudT93(lang,"post_deleted");revision++}
                                        .onFailure{status=rsReleaseT98(lang,"delete_failed")}
                                    busyId=null
                                }
                            }else pendingDelete=x.id
                        },
                        enabled=busyId==null,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==x.id)rsSocialUiV50(lang,"confirm") else rsSocialUiV50(lang,"delete"))}
                }
            }
        }
    }
}

@Composable
private fun RsCloudGroupsScreenV84(c:RsPalette,lang:RsLang,role:RsRole){
    val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)}
    var groups by remember{mutableStateOf<List<RsCloudGroupV84>>(emptyList())}
    var selectedGroup by remember{mutableStateOf<RsCloudGroupV84?>(null)}
    var messages by remember{mutableStateOf<List<RsCloudGroupMessageV92>>(emptyList())}
    var name by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var busyId by remember{mutableStateOf<String?>(null)}
    var status by remember{mutableStateOf("")}
    var pendingDelete by remember{mutableStateOf<String?>(null)}

    LaunchedEffect(revision,selectedGroup?.id){
        loading=true
        if(selectedGroup==null){
            rsCloudGroupsV84()
                .onSuccess{groups=it}
                .onFailure{status=rsGroupChatT(lang,"update_error")}
        }else{
            rsCloudGroupMessagesV92(selectedGroup!!.id)
                .onSuccess{messages=it}
                .onFailure{status=rsGroupChatT(lang,"update_error")}
        }
        loading=false
    }

    val activeGroup=selectedGroup
    if(activeGroup!=null){
        RsScroll(c,activeGroup.name,rsGroupChatT(lang,"chat_sub")){
            OutlinedButton(
                onClick={selectedGroup=null;messages=emptyList();status="";revision++},
                modifier=Modifier.fillMaxWidth()
            ){Text(rsGroupChatT(lang,"back"))}

            RsPanel(c){
                Text(
                    if(loading)rsGroupChatT(lang,"loading_chat") else rsGroupChatT(lang,"chat_ready"),
                    color=if(loading)c.muted else c.bright,
                    fontWeight=FontWeight.Bold,
                    fontSize=10.sp
                )
                if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
            }

            if(messages.isEmpty()&&!loading){
                RsPanel(c){Text(rsGroupChatT(lang,"no_messages"),color=c.muted)}
            }

            messages.forEach{m->
                RsPanel(c){
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        RsMemberAvatarV68(c,m.senderEmail,m.senderName,size=34.dp)
                        Text(m.senderName.ifBlank{m.senderEmail},color=c.bright,fontWeight=FontWeight.Black,fontSize=11.sp)
                    }
                    if(m.body.isNotBlank())Text(m.body,color=c.text)
                    RsChatAttachmentPreviewV92(c,lang,m.mediaPath,m.mediaKind,m.mediaName)
                }
            }

            RsChatComposerV92(
                c=c,
                lang=lang,
                scopeType="group",
                scopeId=activeGroup.id,
                enabled=!loading,
                onSent={revision++},
                onStatus={status=it},
                onSend={body,attachment->rsSendCloudGroupMessageV92(activeGroup.id,body,attachment)}
            )
        }
        return
    }

    RsScroll(
        c,
        rsSocialUiV50(lang,"groups"),
        if(role==RsRole.TRAINER)rsGroupChatT(lang,"manage_sub") else rsGroupChatT(lang,"student_sub")
    ){
        RsPanel(c){
            Text(
                if(loading)rsGroupChatT(lang,"syncing") else rsGroupChatT(lang,"connected"),
                color=if(loading)c.muted else c.bright,
                fontWeight=FontWeight.Bold,
                fontSize=10.sp
            )
            if(status.isNotBlank())Text(status,color=c.muted,fontSize=10.sp)
        }

        if(role==RsRole.TRAINER)RsPanel(c){
            Text(rsSocialUiV50(lang,"new_group"),color=c.bright,fontWeight=FontWeight.Black)
            OutlinedTextField(name,{name=it.take(80)},label={Text(rsSocialUiV50(lang,"name"))},modifier=Modifier.fillMaxWidth(),enabled=busyId==null)
            OutlinedTextField(description,{description=it.take(500)},label={Text(rsSocialUiV50(lang,"description"))},modifier=Modifier.fillMaxWidth(),minLines=2,enabled=busyId==null)
            Button(
                onClick={
                    busyId="new"
                    scope.launch{
                        rsCreateCloudGroupV84(name,description)
                            .onSuccess{name="";description="";status=rsGroupChatT(lang,"created");revision++}
                            .onFailure{status=it.message?:rsGroupChatT(lang,"create_error")}
                        busyId=null
                    }
                },
                enabled=name.isNotBlank()&&busyId==null,
                modifier=Modifier.fillMaxWidth()
            ){Text(if(busyId=="new")rsGroupChatT(lang,"creating") else rsSocialUiV50(lang,"create"))}
        }

        groups.forEach{g->
            RsPanel(c){
                Text(g.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
                Text(g.description,color=c.text)
                Text(g.memberCount.toString()+" "+rsGroupChatT(lang,"members"),color=c.muted,fontSize=10.sp)

                if(role==RsRole.STUDENT){
                    Button(
                        onClick={
                            busyId=g.id
                            scope.launch{
                                rsSetMyCloudGroupMembershipV84(g.id,!g.joined)
                                    .onSuccess{status=if(g.joined)rsGroupChatT(lang,"left") else rsGroupChatT(lang,"joined");revision++}
                                    .onFailure{status=rsGroupChatT(lang,"update_error")}
                                busyId=null
                            }
                        },
                        enabled=busyId==null,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(busyId==g.id)rsGroupChatT(lang,"wait") else if(g.joined)rsSocialUiV50(lang,"leave") else rsSocialUiV50(lang,"join"))}

                    if(g.joined){
                        OutlinedButton(
                            onClick={selectedGroup=g;status="";revision++},
                            modifier=Modifier.fillMaxWidth()
                        ){Text(rsGroupChatT(lang,"open_chat"))}
                    }
                }else{
                    Button(
                        onClick={selectedGroup=g;status="";revision++},
                        modifier=Modifier.fillMaxWidth()
                    ){Text(rsGroupChatT(lang,"open_chat"))}

                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(if(g.active)rsSocialUiV50(lang,"active") else rsSocialUiV50(lang,"inactive"),color=c.muted)
                        Switch(
                            g.active,
                            {value->
                                busyId=g.id
                                scope.launch{
                                    rsSetCloudGroupActiveV84(g.id,value)
                                        .onSuccess{revision++}
                                        .onFailure{status=rsGroupChatT(lang,"update_error")}
                                    busyId=null
                                }
                            },
                            enabled=busyId==null
                        )
                    }
                    OutlinedButton(
                        onClick={
                            if(pendingDelete==g.id){
                                busyId=g.id
                                scope.launch{
                                    rsDeleteCloudGroupV84(g.id)
                                        .onSuccess{pendingDelete=null;status=rsGroupChatT(lang,"deleted");revision++}
                                        .onFailure{status=it.message?:rsGroupChatT(lang,"delete_error")}
                                    busyId=null
                                }
                            }else pendingDelete=g.id
                        },
                        enabled=busyId==null,
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(pendingDelete==g.id)rsSocialUiV50(lang,"confirm") else rsSocialUiV50(lang,"delete"))}
                }
            }
        }
    }
}

package com.rskickbox.app

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private data class VisualSlotV21(val key:String,val title:String,val group:String,val size:String,val hint:String)
private fun visualKeyV21(slot:String)="visual_v21_$slot"
private fun posKeyV21(slot:String)="visual_v21_pos_$slot"
private fun opacityKeyV21(slot:String)="visual_v21_opacity_$slot"

private val visualSlotsV21=listOf(
    VisualSlotV21("login","Login page","Full page","BIG","Behind member access"),
    VisualSlotV21("intro_fighter","Intro fighter scene","Splash / Intro","BIG","Opening fighter visual"),
    VisualSlotV21("intro_gloves","Intro gloves scene","Splash / Intro","BIG","Hanging gloves reveal"),
    VisualSlotV21("student_home","Student dashboard","Full page","BIG","Student dashboard background"),
    VisualSlotV21("trainer_home","Trainer dashboard","Full page","BIG","Trainer dashboard background"),
    VisualSlotV21("header","App header banner","Banners","MEDIUM","Global header banner"),
    VisualSlotV21("footer","App footer banner","Banners","MEDIUM","Global footer banner"),
    VisualSlotV21("music","Music player","Full page","BIG","RS Live Audio background"),
    VisualSlotV21("academy","RS Academy page","Full page","BIG","Academy background"),
    VisualSlotV21("session","Session Player page","Full page","BIG","Session timer background"),
    VisualSlotV21("voice","AI Voice Coach page","Full page","BIG","AI coach background"),
    VisualSlotV21("payments","Payment Center page","Full page","BIG","Payments background"),
    VisualSlotV21("community","Community page","Full page","BIG","Community background"),
    VisualSlotV21("progress","Progress page","Full page","BIG","Progress background"),
    VisualSlotV21("fightcamp","Fight Camp page","Full page","BIG","Fight camp background"),
    VisualSlotV21("book","Trainer Book page","Full page","BIG","Book background"),
    VisualSlotV21("media","Training Media page","Full page","BIG","Media background"),
    VisualSlotV21("settings","Settings page","Full page","BIG","Settings background")
)

@Composable
fun RsVisualAssetStudioV21(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    var active by remember{mutableStateOf<VisualSlotV21?>(null)}
    var message by remember{mutableStateOf("")}
    var refresh by remember{mutableIntStateOf(0)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        val slot=active
        if(uri!=null&&slot!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            store.ps(visualKeyV21(slot.key),uri.toString());message="${slot.title} saved.";refresh++
        }
    }
    RsScroll(c,"Visual Asset Studio","Control small, medium and full-page visuals across RS KICKBOX."){
        RsPanel(c){Text("GLOBAL VISUAL LIBRARY",color=c.bright,fontWeight=FontWeight.Black);Text("Every page, card and banner can use its own uploaded visual, position and readability overlay.",color=c.muted);if(message.isNotBlank())Text(message,color=c.bright,fontSize=11.sp)}
        visualSlotsV21.groupBy{it.group}.forEach{(group,slots)->
            Text(group.uppercase(),color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
            slots.forEach{slot->key(refresh,slot.key){VisualSlotCardV21(c,store,slot,{active=slot;picker.launch(arrayOf("image/*"))}){refresh++}}}
        }
        Text("DASHBOARD CARD BACKGROUNDS",color=c.bright,fontWeight=FontWeight.Black,fontSize=13.sp)
        val tiles=listOf("voice" to "AI Voice Coach","session" to "Session Player","academy" to "RS Academy","techniques" to "Technique Library","home_training" to "Home Training","workout" to "Workout Generator","classes" to "Classes & Events","progress" to "Progress","challenges" to "Challenges","fightcamp" to "Fight Camp","community" to "Community","coachchat" to "Private Coach Chat","media" to "Training Media","music" to "RS Music","finance" to "Membership & Payments","book" to "Trainer Book","themes" to "Theme Studio","backgrounds" to "Visual Asset Studio","branding" to "Branding & Site Settings","members" to "Student Manager","payments" to "Payment Center","analytics" to "Analytics","notifications" to "Notifications","support" to "Support & QC")
        tiles.forEach{(route,title)->
            val slot=VisualSlotV21("tile_$route",title,"Dashboard tiles","SMALL","Dashboard card visual")
            key(refresh,slot.key){VisualSlotCardV21(c,store,slot,{active=slot;picker.launch(arrayOf("image/*"))}){refresh++}}
        }
    }
}

@Composable
private fun VisualSlotCardV21(c:RsPalette,store:RsStore,slot:VisualSlotV21,onUpload:()->Unit,onChanged:()->Unit){
    var pos by remember{mutableStateOf(store.s(posKeyV21(slot.key),"CENTER"))}
    var opacity by remember{mutableFloatStateOf(store.s(opacityKeyV21(slot.key),"0.55").toFloatOrNull()?:.55f)}
    val uri=store.s(visualKeyV21(slot.key),"")
    RsPanel(c){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(slot.title,color=c.bright,fontWeight=FontWeight.Bold);Text("${slot.size} · ${slot.hint}",color=c.muted,fontSize=10.sp)};Surface(shape=RoundedCornerShape(12.dp),color=c.gold.copy(alpha=.18f),border=BorderStroke(1.dp,c.bright.copy(alpha=.35f))){Text(slot.size,color=c.bright,fontSize=9.sp,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))}}
        Box(Modifier.fillMaxWidth().height(if(slot.size=="BIG")140.dp else if(slot.size=="MEDIUM")95.dp else 80.dp).clip(RoundedCornerShape(16.dp)).background(c.panel2)){
            if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos) else Box(Modifier.fillMaxSize().background(c.gold.copy(alpha=.10f)),contentAlignment=Alignment.Center){Text("Default RS visual",color=c.muted)}
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity.coerceIn(0f,.85f))))
            Text(if(uri.isBlank())"DEFAULT" else "CUSTOM SAVED",color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick=onUpload,modifier=Modifier.weight(1f)){Text(if(uri.isBlank())"Upload" else "Replace")};OutlinedButton(onClick={store.ps(visualKeyV21(slot.key),"");onChanged()},enabled=uri.isNotBlank(),modifier=Modifier.weight(1f)){Text("Reset")}}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("LEFT","CENTER","RIGHT","TOP","BOTTOM").forEach{p->FilterChip(selected=pos==p,onClick={pos=p;store.ps(posKeyV21(slot.key),p)},label={Text(p.take(1),fontSize=9.sp)})}}
        Text("Dark overlay ${(opacity*100).toInt()}%",color=c.muted,fontSize=10.sp)
        Slider(value=opacity,onValueChange={v->opacity=v;store.ps(opacityKeyV21(slot.key),v.toString())},valueRange=0f..0.85f)
    }
}

@Composable
fun RsBrandSiteSettingsV21(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    var header by remember{mutableStateOf(store.s("brand_header_name","RS KICKBOX"))}
    var title by remember{mutableStateOf(store.s("brand_login_title","Premium cinematic kickboxing"))}
    var subtitle by remember{mutableStateOf(store.s("brand_login_subtitle","TRAIN · LEARN · CONNECT · GROW"))}
    var footer by remember{mutableStateOf(store.s("brand_footer_text","RS KICKBOX · TRAIN · LEARN · CONNECT · GROW"))}
    var active by remember{mutableStateOf("")};var refresh by remember{mutableIntStateOf(0)};var message by remember{mutableStateOf("")}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null&&active.isNotBlank()){runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};store.ps("brand_asset_$active",uri.toString());refresh++;message="$active updated."}}
    RsScroll(c,"Branding & Site Settings","Trainer-controlled RS identity, logos and app presentation."){
        RsPanel(c){Text("APP IDENTITY",color=c.bright,fontWeight=FontWeight.Black);OutlinedTextField(header,{header=it},label={Text("Header / app name")},modifier=Modifier.fillMaxWidth());OutlinedTextField(title,{title=it},label={Text("Login title")},modifier=Modifier.fillMaxWidth());OutlinedTextField(subtitle,{subtitle=it},label={Text("Login subtitle")},modifier=Modifier.fillMaxWidth());OutlinedTextField(footer,{footer=it},label={Text("Footer text")},modifier=Modifier.fillMaxWidth());Button(onClick={store.ps("brand_header_name",header.trim());store.ps("brand_login_title",title.trim());store.ps("brand_login_subtitle",subtitle.trim());store.ps("brand_footer_text",footer.trim());message="Brand text saved."},modifier=Modifier.fillMaxWidth()){Text("Save brand text")};if(message.isNotBlank())Text(message,color=c.muted)}
        listOf("main_logo" to "Main RS logo","compact_logo" to "Compact header logo","royal_crown" to "Royal crown artwork","favicon" to "Favicon / release icon preview").forEach{(keyName,label)->
            val uri=store.s("brand_asset_$keyName","")
            RsPanel(c){Text(label,color=c.bright,fontWeight=FontWeight.Bold);if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxWidth().height(110.dp),"CENTER") else Box(Modifier.fillMaxWidth().height(70.dp).background(c.panel2),contentAlignment=Alignment.Center){Text("♛ RS",color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={active=keyName;picker.launch(arrayOf("image/*"))},modifier=Modifier.weight(1f)){Text(if(uri.isBlank())"Upload" else "Replace")};OutlinedButton(onClick={store.ps("brand_asset_$keyName","");refresh++},enabled=uri.isNotBlank(),modifier=Modifier.weight(1f)){Text("Reset")}}}
        }
        RsPanel(c){Text("RELEASE NOTE",color=c.bright,fontWeight=FontWeight.Bold);Text("The favicon / icon upload is a release artwork reference. Android launcher icons remain build-time resources and will be packaged for the signed Play Store release.",color=c.muted)}
        key(refresh){Spacer(Modifier.height(1.dp))}
    }
}

@Composable
fun RsUriPreviewV21(uri:String,modifier:Modifier=Modifier,position:String="CENTER"){
    AndroidView(factory={ctx->ImageView(ctx).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_CROP}},modifier=modifier,update={view->runCatching{view.setImageURI(Uri.parse(uri))}})
}

@Composable
fun RsPerPageBackgroundV21(store:RsStore,route:String,content:@Composable ()->Unit){
    val key=when(route){"home"->"student_home";"trainer"->"trainer_home";else->route};val uri=store.s(visualKeyV21(key),"");val opacity=store.s(opacityKeyV21(key),"0.60").toFloatOrNull()?:.60f;val pos=store.s(posKeyV21(key),"CENTER")
    Box(Modifier.fillMaxSize()){if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos);if(uri.isNotBlank())Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity.coerceIn(0f,.88f))));content()}
}

@Composable
fun RsBrandedHeaderV21(c:RsPalette,store:RsStore,content:@Composable ColumnScope.()->Unit){
    val uri=store.s(visualKeyV21("header"),"")
    val compactLogo=store.s("brand_asset_compact_logo","")
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.panel)){
        if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.matchParentSize(),store.s(posKeyV21("header"),"CENTER"))
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=store.s(opacityKeyV21("header"),"0.55").toFloatOrNull()?:.55f)))
        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(compactLogo.isNotBlank())RsUriPreviewV21(compactLogo,Modifier.height(42.dp).width(82.dp),"CENTER")
            content()
        }
    }
}

@Composable
fun RsBrandedFooterV21(c:RsPalette,store:RsStore){
    val uri=store.s(visualKeyV21("footer"),"")
    Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(16.dp)).background(c.panel),contentAlignment=Alignment.Center){if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.matchParentSize(),store.s(posKeyV21("footer"),"CENTER"));Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=.55f)));Text(store.s("brand_footer_text","RS KICKBOX · TRAIN · LEARN · CONNECT · GROW"),color=c.muted,fontSize=9.sp,maxLines=1)}
}

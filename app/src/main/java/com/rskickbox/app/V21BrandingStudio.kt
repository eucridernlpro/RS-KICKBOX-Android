package com.rskickbox.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private data class VisualSlotV21(val key:String,val title:String,val group:String,val size:String,val hint:String)
private fun visualKeyV21(slot:String)="visual_v21_$slot"
private fun posKeyV21(slot:String)="visual_v21_pos_$slot"
private fun opacityKeyV21(slot:String)="visual_v21_opacity_$slot"

private val visualSlotsV21=listOf(
    VisualSlotV21("login","Login page","Splash / Login","BIG","Behind member access"),
    VisualSlotV21("student_home","Student dashboard","Dashboards","BIG","Student dashboard background"),
    VisualSlotV21("trainer_home","Trainer dashboard","Dashboards","BIG","Trainer dashboard background"),
    VisualSlotV21("header","App header banner","Banners","MEDIUM","Global header banner"),
    VisualSlotV21("footer","App footer banner","Banners","MEDIUM","Global footer banner"),
    VisualSlotV21("music_wall_1","Music wallpaper 1","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("music_wall_2","Music wallpaper 2","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("music_wall_3","Music wallpaper 3","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("music_wall_4","Music wallpaper 4","Music Player","BIG","Rotating live-audio wallpaper"),
    VisualSlotV21("voice","AI Technique Coach","Student / Training","BIG","AI coach background"),
    VisualSlotV21("ai_trainer_male","AI male trainer visual","AI Trainer Personas","BIG","Photorealistic cinematic male trainer artwork"),
    VisualSlotV21("ai_trainer_female","AI female trainer visual","AI Trainer Personas","BIG","Photorealistic cinematic female trainer artwork"),
    VisualSlotV21("session","Session Player","Student / Training","BIG","Session timer background"),
    VisualSlotV21("academy","RS Academy","Student / Training","BIG","Academy background"),
    VisualSlotV21("techniques","Technique Library","Student / Training","BIG","Technique library background"),
    VisualSlotV21("home_training","Home Training","Student / Training","BIG","Home training background"),
    VisualSlotV21("workout","Workout Generator","Student / Training","BIG","Workout generator background"),
    VisualSlotV21("classes","Classes & Events","Student / Club","BIG","Class booking / manager background"),
    VisualSlotV21("events","RS Events","Student / Club","BIG","Events background"),
    VisualSlotV21("coachchat","Private Coach Chat","Student / Club","BIG","Coach chat background"),
    VisualSlotV21("community","Community","Student / Club","BIG","Community background"),
    VisualSlotV21("groups","Groups","Student / Club","BIG","Groups background"),
    VisualSlotV21("private_lessons","Private Lessons","Student / Club","BIG","Private lessons background"),
    VisualSlotV21("progress","Progress","Student / Performance","BIG","Progress background"),
    VisualSlotV21("challenges","Challenges","Student / Performance","BIG","Challenges background"),
    VisualSlotV21("badges","Badges","Student / Performance","BIG","Badges background"),
    VisualSlotV21("fightcamp","Fight Camp","Student / Performance","BIG","Fight camp background"),
    VisualSlotV21("compare","Technique Compare","Student / Performance","BIG","Technique comparison background"),
    VisualSlotV21("history","Training History","Student / Performance","BIG","History background"),
    VisualSlotV21("vault","Knowledge Vault","Student / Library","BIG","Knowledge background"),
    VisualSlotV21("homework","Homework","Student / Library","BIG","Homework background"),
    VisualSlotV21("favorites","Saved & Favorites","Student / Library","BIG","Favorites background"),
    VisualSlotV21("media","Training Media","Student / Library","BIG","Media background"),
    VisualSlotV21("music","RS Music","Student / Library","BIG","Internal audio page background"),
    VisualSlotV21("finance","Membership & Payments","Student / Account","BIG","Membership background"),
    VisualSlotV21("book","Trainer Book","Student / Account","BIG","Book background"),
    VisualSlotV21("profile","My Profile","Student / Account","BIG","Profile background"),
    VisualSlotV21("search","Search","Student / Account","BIG","Search background"),
    VisualSlotV21("settings","Settings & Privacy","Student / Account","BIG","Settings background"),
    VisualSlotV21("themes","Visual Theme Studio","Trainer / Brand","BIG","Theme studio background"),
    VisualSlotV21("backgrounds","Visual Asset Studio","Trainer / Brand","BIG","Visual studio background"),
    VisualSlotV21("branding","Branding & Site Settings","Trainer / Brand","BIG","Branding settings background"),
    VisualSlotV21("intro_settings","Intro & Splash","Trainer / Brand","BIG","Intro settings background"),
    VisualSlotV21("landing_admin","Promotion Manager","Trainer / Brand","BIG","Promotion background"),
    VisualSlotV21("lesson_editor","Lesson Editor","Trainer / Coaching","BIG","Lesson editor background"),
    VisualSlotV21("content","Content Manager","Trainer / Coaching","BIG","Content manager background"),
    VisualSlotV21("homework_admin","Homework Manager","Trainer / Coaching","BIG","Homework manager background"),
    VisualSlotV21("session_builder","Session Builder","Trainer / Coaching","BIG","Session builder background"),
    VisualSlotV21("music_admin","RS Music Manager","Trainer / Coaching","BIG","Music manager background"),
    VisualSlotV21("notes","Coach Notes","Trainer / Coaching","BIG","Coach notes background"),
    VisualSlotV21("members","Student Manager","Trainer / Members","BIG","Student manager background"),
    VisualSlotV21("access","Access & Subscriptions","Trainer / Members","BIG","Access background"),
    VisualSlotV21("plans_admin","Membership Plans","Trainer / Members","BIG","Plans background"),
    VisualSlotV21("progress_admin","Progress Manager","Trainer / Members","BIG","Progress manager background"),
    VisualSlotV21("assessments","Coach Assessments","Trainer / Members","BIG","Assessments background"),
    VisualSlotV21("challenge_admin","Challenge Manager","Trainer / Members","BIG","Challenge manager background"),
    VisualSlotV21("fightcamp_admin","Fight Camp Manager","Trainer / Members","BIG","Fight camp manager background"),
    VisualSlotV21("attendance","Attendance","Trainer / Operations","BIG","Attendance background"),
    VisualSlotV21("qr_attendance","QR Attendance","Trainer / Operations","BIG","QR attendance background"),
    VisualSlotV21("events_admin","Event Manager","Trainer / Operations","BIG","Event manager background"),
    VisualSlotV21("schedule","Trainer Schedule","Trainer / Operations","BIG","Schedule background"),
    VisualSlotV21("notifications","Notifications","Trainer / Operations","BIG","Notifications background"),
    VisualSlotV21("documents","Documents & Waivers","Trainer / Operations","BIG","Documents background"),
    VisualSlotV21("referrals","Referrals","Trainer / Operations","BIG","Referrals background"),
    VisualSlotV21("payments","Payment Center","Trainer / Business","BIG","Payments background"),
    VisualSlotV21("invoices","Invoices","Trainer / Business","BIG","Invoices background"),
    VisualSlotV21("analytics","Analytics","Trainer / Business","BIG","Analytics background"),
    VisualSlotV21("support","Support & Final QC","Trainer / Business","BIG","Support background"),
    VisualSlotV21("release","Release & Legal Center","Trainer / Business","BIG","Release background")
)

private val dashboardTileSlotsV26=listOf(
    "voice" to "AI Technique Coach","session" to "Session Player","academy" to "RS Academy","techniques" to "Technique Library",
    "home_training" to "Home Training","workout" to "Workout Generator","classes" to "Classes & Events","events" to "RS Events",
    "coachchat" to "Private Coach Chat","community" to "Community","groups" to "Groups","private_lessons" to "Private Lessons",
    "progress" to "Progress","challenges" to "Challenges","badges" to "Badges","fightcamp" to "Fight Camp",
    "compare" to "Technique Compare","history" to "Training History","vault" to "Knowledge Vault","homework" to "Homework",
    "favorites" to "Saved & Favorites","media" to "Training Media","music" to "RS Music","finance" to "Membership & Payments",
    "book" to "Trainer Book","profile" to "My Profile","search" to "Search","settings" to "Settings & Privacy",
    "themes" to "Visual Theme Studio","backgrounds" to "Visual Asset Studio","branding" to "Branding & Site Settings","intro_settings" to "Intro & Splash",
    "landing_admin" to "Promotion Manager","lesson_editor" to "Lesson Editor","content" to "Content Manager","homework_admin" to "Homework Manager",
    "session_builder" to "Session Builder","music_admin" to "RS Music Manager","notes" to "Coach Notes","members" to "Student Manager",
    "access" to "Access & Subscriptions","plans_admin" to "Membership Plans","progress_admin" to "Progress Manager","assessments" to "Coach Assessments",
    "challenge_admin" to "Challenge Manager","fightcamp_admin" to "Fight Camp Manager","attendance" to "Attendance","qr_attendance" to "QR Attendance",
    "events_admin" to "Event Manager","schedule" to "Trainer Schedule","notifications" to "Notifications","documents" to "Documents & Waivers",
    "referrals" to "Referrals","payments" to "Payment Center","invoices" to "Invoices","analytics" to "Analytics",
    "support" to "Support & Final QC","release" to "Release & Legal Center"
).map{(route,title)->VisualSlotV21("tile_$route",title,"Dashboard tiles","SMALL","Dashboard card visual")}

private val allVisualSlotsV26=visualSlotsV21+dashboardTileSlotsV26

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsVisualAssetStudioV21(c:RsPalette,store:RsStore){
    val context=LocalContext.current
    var selectedGroup by remember{mutableStateOf(allVisualSlotsV26.first().group)}
    var selectedSlotKey by remember{mutableStateOf(allVisualSlotsV26.first().key)}
    var groupMenuOpen by remember{mutableStateOf(false)}
    var slotMenuOpen by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var optimizing by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    var fullPreview by remember{mutableStateOf(false)}

    val groups=remember{allVisualSlotsV26.map{it.group}.distinct()}
    val visibleSlots=remember(selectedGroup){allVisualSlotsV26.filter{it.group==selectedGroup}}
    val selected=visibleSlots.firstOrNull{it.key==selectedSlotKey}?:visibleSlots.first()

    LaunchedEffect(selectedGroup){
        if(visibleSlots.none{it.key==selectedSlotKey})selectedSlotKey=visibleSlots.first().key
    }

    var pos by remember(selected.key,refresh){mutableStateOf(store.s(posKeyV21(selected.key),"CENTER"))}
    var opacity by remember(selected.key,refresh){mutableFloatStateOf(store.s(opacityKeyV21(selected.key),"0.55").toFloatOrNull()?:.55f)}
    val uri=store.s(visualKeyV21(selected.key),"")

    fun saveVisual(picked:Uri,persist:Boolean){
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(picked,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        optimizing=true
        message="Preparing ${selected.title}…"
        rsImportVisualMediaV29(
            context=context,
            source=picked,
            slotSize=selected.size,
            onStatus={message=it},
            onComplete={info->
                val previous=store.s(visualKeyV21(selected.key),"")
                store.ps(visualKeyV21(selected.key),info.uri)
                store.ps("visual_v21_kind_${selected.key}",info.kind)
                if(previous.isNotBlank() && previous!=info.uri)rsDeleteOwnedVisualV29(context,previous)
                message=info.note
                optimizing=false
                refresh++
            },
            onError={err->message=err;optimizing=false}
        )
    }
    val galleryPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){picked->
        if(picked!=null)saveVisual(picked,false)
    }
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){picked->
        if(picked!=null)saveVisual(picked,true)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal=6.dp,vertical=4.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Text("VISUAL PLACEMENT EDITOR",color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp)
        Text("Choose one place. Only that visual is loaded and edited.",color=c.muted,fontSize=11.sp,maxLines=2)

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            ExposedDropdownMenuBox(
                expanded=groupMenuOpen,
                onExpandedChange={groupMenuOpen=!groupMenuOpen},
                modifier=Modifier.weight(1f)
            ){
                OutlinedTextField(
                    value=selectedGroup,
                    onValueChange={},
                    readOnly=true,
                    label={Text("Category",fontSize=10.sp)},
                    trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(groupMenuOpen)},
                    modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    textStyle=LocalTextStyle.current.copy(fontSize=11.sp),
                    singleLine=true
                )
                ExposedDropdownMenu(expanded=groupMenuOpen,onDismissRequest={groupMenuOpen=false}){
                    groups.forEach{g->
                        DropdownMenuItem(text={Text(g,fontSize=12.sp)},onClick={
                            selectedGroup=g
                            selectedSlotKey=allVisualSlotsV26.first{it.group==g}.key
                            groupMenuOpen=false
                        })
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded=slotMenuOpen,
                onExpandedChange={slotMenuOpen=!slotMenuOpen},
                modifier=Modifier.weight(1.25f)
            ){
                OutlinedTextField(
                    value=selected.title,
                    onValueChange={},
                    readOnly=true,
                    label={Text("Place",fontSize=10.sp)},
                    trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(slotMenuOpen)},
                    modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    textStyle=LocalTextStyle.current.copy(fontSize=10.sp),
                    singleLine=true
                )
                ExposedDropdownMenu(expanded=slotMenuOpen,onDismissRequest={slotMenuOpen=false}){
                    visibleSlots.forEach{slot->
                        DropdownMenuItem(text={Text(slot.title,fontSize=12.sp)},onClick={selectedSlotKey=slot.key;slotMenuOpen=false})
                    }
                }
            }
        }

        Surface(
            modifier=Modifier.fillMaxWidth().weight(1f),
            shape=RoundedCornerShape(22.dp),
            color=c.panel,
            border=BorderStroke(1.dp,c.gold.copy(alpha=.55f))
        ){
            Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(selected.title,color=c.bright,fontWeight=FontWeight.Black,fontSize=15.sp,maxLines=2)
                        Text(selected.hint,color=c.muted,fontSize=10.sp,maxLines=2)
                    }
                    Surface(shape=RoundedCornerShape(12.dp),color=c.gold.copy(alpha=.18f),border=BorderStroke(1.dp,c.bright.copy(alpha=.35f))){
                        Text(selected.size,color=c.bright,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp))
                    }
                }

                Text(
                    when(selected.size){
                        "SMALL"->"Best for dashboard cards. Use a clear subject with safe space for title text."
                        "MEDIUM"->"Best for header/footer banners. Use a wide image with the main subject away from text controls."
                        else->"Best for full-page backgrounds. Use portrait or adaptable artwork with important details away from screen edges."
                    },
                    color=c.text.copy(alpha=.84f),fontSize=9.sp,maxLines=3
                )

                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(c.panel2)
                ){
                    if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos)
                    else Box(Modifier.fillMaxSize().background(c.gold.copy(alpha=.10f)),contentAlignment=Alignment.Center){
                        Column(horizontalAlignment=Alignment.CenterHorizontally){
                            Text("♛ RS",color=c.bright,fontSize=28.sp,fontWeight=FontWeight.Black)
                            Text("VISUAL PREVIEW",color=c.muted,fontSize=10.sp)
                        }
                    }
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity.coerceIn(0f,.85f))))
                    Text(if(uri.isBlank())"DEFAULT RS VISUAL" else "CUSTOM VISUAL SAVED",color=c.bright,fontWeight=FontWeight.Bold,fontSize=9.sp,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
                }

                if(uri.isNotBlank()){
                    Button(
                        onClick={fullPreview=true},
                        enabled=!optimizing,
                        modifier=Modifier.fillMaxWidth()
                    ){Text("▶ Preview selected background",fontSize=10.sp)}
                }

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(
                        onClick={galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))},
                        enabled=!optimizing,
                        modifier=Modifier.weight(1f)
                    ){Text("Gallery",fontSize=10.sp)}
                    OutlinedButton(onClick={filePicker.launch(arrayOf("image/*","video/*"))},enabled=!optimizing,modifier=Modifier.weight(1f)){Text("Files",fontSize=10.sp)}
                    OutlinedButton(
                        onClick={
                            val previous=store.s(visualKeyV21(selected.key),"")
                            rsDeleteOwnedVisualV29(context,previous)
                            store.ps(visualKeyV21(selected.key),"")
                            store.ps("visual_v21_kind_${selected.key}","")
                            message="${selected.title} reset."
                            refresh++
                        },
                        enabled=uri.isNotBlank(),
                        modifier=Modifier.weight(1f)
                    ){Text("Reset",fontSize=10.sp)}
                }

                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    listOf("LEFT","CENTER","RIGHT","TOP","BOTTOM").forEach{p->
                        FilterChip(
                            selected=pos==p,
                            onClick={pos=p;store.ps(posKeyV21(selected.key),p)},
                            label={Text(p.take(1),fontSize=9.sp)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                }

                if(optimizing){
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Automatic formatting / optimization in progress…",color=c.bright,fontSize=9.sp)
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("Overlay ${(opacity*100).toInt()}%",color=c.muted,fontSize=9.sp,modifier=Modifier.width(75.dp))
                    Slider(
                        value=opacity,
                        onValueChange={v->opacity=v;store.ps(opacityKeyV21(selected.key),v.toString())},
                        valueRange=0f..0.85f,
                        modifier=Modifier.weight(1f)
                    )
                }
                if(selected.key=="login"){
                    var loginFormOpacity by remember(refresh){mutableFloatStateOf(store.s("login_form_opacity","0.82").toFloatOrNull()?:.82f)}
                    Text("LOGIN FORM TRANSPARENCY",color=c.bright,fontWeight=FontWeight.Bold,fontSize=10.sp)
                    Text("Controls how much of the login background remains visible through the login boxes.",color=c.muted,fontSize=9.sp)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("${(loginFormOpacity*100).toInt()}%",color=c.text,fontSize=10.sp,modifier=Modifier.width(45.dp))
                        Slider(
                            value=loginFormOpacity,
                            onValueChange={v->
                                loginFormOpacity=v
                                store.ps("login_form_opacity",v.toString())
                            },
                            valueRange=.20f..1f,
                            modifier=Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if(message.isNotBlank())Text(message,color=c.bright,fontSize=10.sp,maxLines=1)
    }

    if(fullPreview && uri.isNotBlank()){
        androidx.compose.ui.window.Dialog(onDismissRequest={fullPreview=false}){
            Surface(
                color=Color.Black,
                modifier=Modifier.fillMaxWidth().fillMaxHeight(.94f)
            ){
                Box(Modifier.fillMaxSize().background(Color.Black)){
                    RsUriPreviewV21(uri,Modifier.fillMaxSize(),pos)
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha=opacity.coerceIn(0f,.85f))))
                    Column(
                        Modifier.align(Alignment.TopStart).fillMaxWidth().padding(14.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)
                    ){
                        Text(selected.title,color=Color.White,fontWeight=FontWeight.Black)
                        Text("Position $pos · Overlay ${(opacity*100).toInt()}%",color=Color.White.copy(alpha=.74f),fontSize=10.sp)
                    }
                    TextButton(
                        onClick={fullPreview=false},
                        modifier=Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ){Text("Close preview",color=Color.White)}
                    Text(
                        "FULL-SCREEN BACKGROUND TEST",
                        color=Color.White.copy(alpha=.72f),
                        fontSize=9.sp,
                        fontWeight=FontWeight.Bold,
                        modifier=Modifier.align(Alignment.BottomCenter).padding(12.dp)
                    )
                }
            }
        }
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
    fun saveBrandAsset(uri:Uri,persist:Boolean){
        if(active.isBlank())return
        if(persist)runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        store.ps("brand_asset_$active",uri.toString());refresh++;message="$active updated."
    }
    val galleryPickerBrand=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null)saveBrandAsset(uri,false)}
    val filePickerBrand=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)saveBrandAsset(uri,true)}
    RsScroll(c,"Branding & Site Settings","Trainer-controlled RS identity, logos and app presentation."){
        RsPanel(c){Text("APP IDENTITY",color=c.bright,fontWeight=FontWeight.Black);OutlinedTextField(header,{header=it},label={Text("Header / app name")},modifier=Modifier.fillMaxWidth());OutlinedTextField(title,{title=it},label={Text("Login title")},modifier=Modifier.fillMaxWidth());OutlinedTextField(subtitle,{subtitle=it},label={Text("Login subtitle")},modifier=Modifier.fillMaxWidth());OutlinedTextField(footer,{footer=it},label={Text("Footer text")},modifier=Modifier.fillMaxWidth());Button(onClick={store.ps("brand_header_name",header.trim());store.ps("brand_login_title",title.trim());store.ps("brand_login_subtitle",subtitle.trim());store.ps("brand_footer_text",footer.trim());message="Brand text saved."},modifier=Modifier.fillMaxWidth()){Text("Save brand text")};if(message.isNotBlank())Text(message,color=c.muted)}
        listOf("main_logo" to "Main RS logo","compact_logo" to "Compact header logo","royal_crown" to "Royal crown artwork","favicon" to "Favicon / release icon preview").forEach{(keyName,label)->
            val uri=store.s("brand_asset_$keyName","")
            RsPanel(c){Text(label,color=c.bright,fontWeight=FontWeight.Bold);if(uri.isNotBlank())RsUriPreviewV21(uri,Modifier.fillMaxWidth().height(110.dp),"CENTER") else Box(Modifier.fillMaxWidth().height(70.dp).background(c.panel2),contentAlignment=Alignment.Center){Text("♛ RS",color=c.bright,fontSize=24.sp,fontWeight=FontWeight.Black)};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={active=keyName;galleryPickerBrand.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},modifier=Modifier.weight(1f)){Text(if(uri.isBlank())"Upload" else "Replace")};OutlinedButton(onClick={store.ps("brand_asset_$keyName","");refresh++},enabled=uri.isNotBlank(),modifier=Modifier.weight(1f)){Text("Reset")}}}
        }
        RsPanel(c){Text("RELEASE NOTE",color=c.bright,fontWeight=FontWeight.Bold);Text("The favicon / icon upload is a release artwork reference. Android launcher icons remain build-time resources and will be packaged for the signed Play Store release.",color=c.muted)}
        key(refresh){Spacer(Modifier.height(1.dp))}
    }
}

@Composable
fun RsUriPreviewV21(uri:String,modifier:Modifier=Modifier,position:String="CENTER"){
    val context=LocalContext.current
    val parsed=remember(uri){Uri.parse(uri)}
    val kind=remember(uri){rsVisualKindV29(context,parsed)}
    when(kind){
        "VIDEO"->{
            val player=remember(uri){
                ExoPlayer.Builder(context).build().apply{
                    volume=0f
                    repeatMode=Player.REPEAT_MODE_ONE
                    setMediaItem(MediaItem.fromUri(parsed))
                    prepare()
                    playWhenReady=true
                }
            }
            DisposableEffect(player){onDispose{player.release()}}
            AndroidView(
                factory={ctx->
                    PlayerView(ctx).apply{
                        useController=false
                        resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player=player
                    }
                },
                modifier=modifier,
                update={view->
                    view.player=player
                    view.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    if(!player.isPlaying)player.playWhenReady=true
                }
            )
        }
        else->AndroidView(
            factory={ctx->ImageView(ctx).apply{adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_CROP}},
            modifier=modifier,
            update={view->
                runCatching{
                    if(kind=="GIF" && Build.VERSION.SDK_INT>=28){
                        val src=ImageDecoder.createSource(context.contentResolver,parsed)
                        val drawable=ImageDecoder.decodeDrawable(src)
                        view.setImageDrawable(drawable)
                        (drawable as? AnimatedImageDrawable)?.apply{
                            repeatCount=AnimatedImageDrawable.REPEAT_INFINITE
                            start()
                        }
                    }else view.setImageURI(parsed)
                }
            }
        )
    }
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

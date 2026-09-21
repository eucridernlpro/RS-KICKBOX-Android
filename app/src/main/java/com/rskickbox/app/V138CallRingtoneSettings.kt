package com.rskickbox.app

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun rsCallRingtoneKeyV138(store:RsStore):String{
    val role=store.s("session_role","user")
    val email=store.s("session_student_email","").lowercase()
    return "rs_call_ringtone_uri_v138_"+role+"_"+email.hashCode()
}

fun rsSavedCallRingtoneUriV138(context:Context):Uri{
    val store=RsStore(context)
    val saved=store.s(rsCallRingtoneKeyV138(store),"")
    if(saved.isNotBlank()){
        val uri=runCatching{Uri.parse(saved)}.getOrNull()
        if(uri!=null)return uri
    }
    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
}

private fun rsRingtoneLabelV138(context:Context,uri:String):String{
    if(uri.isBlank())return "RS default / phone ringtone"
    return runCatching{
        context.contentResolver.query(
            Uri.parse(uri),
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,null,null
        )?.use{cursor->
            if(cursor.moveToFirst())cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf{it.isNotBlank()} ?: "Custom RS ringtone"
}

@Composable
fun RsCallRingtoneSettingsV138(
    c:RsPalette,
    store:RsStore,
    lang:RsLang
){
    val context=LocalContext.current
    val key=remember{rsCallRingtoneKeyV138(store)}
    var saved by remember{mutableStateOf(store.s(key,""))}
    var preview:Ringtone? by remember{mutableStateOf(null)}
    var message by remember{mutableStateOf("")}

    fun stopPreview(){
        runCatching{preview?.stop()}
        preview=null
    }

    fun play(uri:Uri){
        stopPreview()
        preview=runCatching{RingtoneManager.getRingtone(context,uri)}.getOrNull()
        runCatching{preview?.play()}
    }

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saved=uri.toString()
            store.ps(key,saved)
            message="RS call ringtone saved."
            play(uri)
        }
    }

    DisposableEffect(Unit){onDispose{stopPreview()}}

    RsPanel(c){
        Text("RS CALL RINGTONE",color=c.bright,fontWeight=FontWeight.Black)
        Text(
            "Choose a ringtone from this phone. It is used only for incoming RS KICKBOXING calls.",
            color=c.muted,fontSize=10.sp
        )

        Surface(
            color=Color.Black.copy(alpha=.52f),
            shape=RoundedCornerShape(16.dp),
            border=BorderStroke(1.dp,c.gold.copy(alpha=.22f)),
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(11.dp)){
                Text("CURRENT",color=c.gold,fontSize=8.sp,fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                Text(rsRingtoneLabelV138(context,saved),color=c.text,fontWeight=FontWeight.Bold,fontSize=11.sp)
            }
        }

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(
                onClick={picker.launch(arrayOf("audio/*"))},
                modifier=Modifier.weight(1f)
            ){Text("Choose from phone",fontSize=9.sp)}

            OutlinedButton(
                onClick={play(if(saved.isBlank())RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) else Uri.parse(saved))},
                modifier=Modifier.weight(1f)
            ){Text("Preview",fontSize=9.sp)}
        }

        OutlinedButton(
            onClick={
                stopPreview()
                store.ps(key,"")
                saved=""
                message="RS ringtone reset to default."
            },
            modifier=Modifier.fillMaxWidth()
        ){Text("Use default ringtone",fontSize=9.sp)}

        if(message.isNotBlank())Text(message,color=c.muted,fontSize=9.sp)
    }
}

package com.rskickbox.app

import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val rsFcmScopeV155=CoroutineScope(SupervisorJob()+Dispatchers.IO)

suspend fun rsRegisterFcmTokenV155(token:String):Result<Unit> = runCatching{
    val clean=token.trim()
    if(clean.length<20)return@runCatching
    val client=rsSupabaseClientV60() ?: return@runCatching
    val label=(Build.MANUFACTURER+" "+Build.MODEL).trim().ifBlank{"Android"}
    client.postgrest.rpc(
        "rs_register_fcm_token",
        buildJsonObject{
            put("p_token",clean)
            put("p_device_label",label)
        }
    )
    Unit
}

fun rsRefreshAndRegisterFcmTokenV155(){
    if(!RsSupabaseV60.configured)return
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener{token->
            rsFcmScopeV155.launch{rsRegisterFcmTokenV155(token)}
        }
}

suspend fun rsSendCallPushV155(callId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: return@runCatching
    client.functions.invoke(
        "send-call-push",
        body=buildJsonObject{
            put("kind","direct_call")
            put("call_id",callId)
        }
    )
    Unit
}

suspend fun rsSendVideoRoomPushV155(roomId:String):Result<Unit> = runCatching{
    val client=rsSupabaseClientV60() ?: return@runCatching
    client.functions.invoke(
        "send-call-push",
        body=buildJsonObject{
            put("kind","video_room")
            put("room_id",roomId)
        }
    )
    Unit
}

class RsFirebaseMessagingServiceV155:FirebaseMessagingService(){
    override fun onNewToken(token:String){
        super.onNewToken(token)
        rsFcmScopeV155.launch{rsRegisterFcmTokenV155(token)}
    }

    override fun onMessageReceived(message:RemoteMessage){
        super.onMessageReceived(message)
        val data=message.data
        val kind=data["kind"].orEmpty()
        val action=when(kind){
            "direct_call"->RsCallMonitorServiceV134.ACTION_PUSH_CALL
            "video_room"->RsCallMonitorServiceV134.ACTION_PUSH_ROOM
            else->return
        }
        val serviceIntent=Intent(this,RsCallMonitorServiceV134::class.java).apply{
            this.action=action
            data.forEach{(k,v)->putExtra(k,v)}
        }
        runCatching{
            ContextCompat.startForegroundService(this,serviceIntent)
        }
    }
}

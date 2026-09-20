package com.rskickbox.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*

class RsCallMonitorServiceV134:Service(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var lastNotifiedCallId:String?=null

    companion object{
        const val ACTION_START="com.rskickbox.app.CALL_MONITOR_START"
        const val ACTION_STOP="com.rskickbox.app.CALL_MONITOR_STOP"
        private const val CHANNEL_MONITOR="rs_call_monitor"
        private const val CHANNEL_CALLS="rs_incoming_calls"
        private const val FOREGROUND_ID=9134

        fun start(context:Context){
            val i=Intent(context,RsCallMonitorServiceV134::class.java).setAction(ACTION_START)
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context:Context){
            context.startService(Intent(context,RsCallMonitorServiceV134::class.java).setAction(ACTION_STOP))
        }
    }

    override fun onCreate(){
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action==ACTION_STOP){
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(FOREGROUND_ID,monitorNotification())
        scope.coroutineContext.cancelChildren()
        scope.launch{monitorLoop()}
        return START_STICKY
    }

    private suspend fun monitorLoop(){
        while(isActive){
            runCatching{
                val client=rsSupabaseClientV60()
                val uid=client?.auth?.currentUserOrNull()?.id.orEmpty()
                if(uid.isBlank()){
                    delay(5000)
                    continue
                }
                rsTouchPresenceV125()
                rsCallInboxV131().onSuccess{calls->
                    val incoming=calls.firstOrNull{
                        it.calleeId==uid && it.status=="RINGING"
                    }
                    if(incoming!=null && incoming.id!=lastNotifiedCallId){
                        lastNotifiedCallId=incoming.id
                        showIncomingCall(incoming)
                    }
                    if(incoming==null)lastNotifiedCallId=null
                }
            }
            delay(5000)
        }
    }

    private fun createChannels(){
        if(Build.VERSION.SDK_INT>=26){
            val nm=getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR,
                    "RS Chat connection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply{
                    description="Keeps trainer/student call availability active while the app is minimized."
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CALLS,
                    "RS incoming calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply{
                    description="Incoming RS KICKBOXING trainer/student calls."
                    lockscreenVisibility=Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    enableVibration(true)
                }
            )
        }
    }

    private fun monitorNotification():Notification{
        val openIntent=PendingIntent.getActivity(
            this,1,
            Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this,CHANNEL_MONITOR)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RS KICKBOXING")
            .setContentText("RS Chat online · calls available")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun showIncomingCall(call:RsCallV131){
        val fullIntent=Intent(this,MainActivity::class.java).apply{
            action="com.rskickbox.app.INCOMING_CALL"
            putExtra("rs_incoming_call_id",call.id)
            flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending=PendingIntent.getActivity(
            this,
            call.id.hashCode(),
            fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val isVideo=call.callType=="VIDEO"
        val notification=NotificationCompat.Builder(this,CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(call.peerName.ifBlank{"RS Member"})
            .setContentText(if(isVideo)"Incoming RS video call" else "Incoming RS audio call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pending)
            .setFullScreenIntent(pending,true)
            .build()
        getSystemService(NotificationManager::class.java).notify(call.id.hashCode(),notification)
    }

    override fun onDestroy(){
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent:Intent?):IBinder?=null
}

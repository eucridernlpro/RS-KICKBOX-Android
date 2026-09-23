package com.rskickbox.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*

class RsCallMonitorServiceV134:Service(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var lastNotifiedCallId:String?=null
    private var lastNotifiedRoomId:String?=null
    private var ringtone:Ringtone?=null

    companion object{
        const val ACTION_START="com.rskickbox.app.CALL_MONITOR_START"
        const val ACTION_STOP="com.rskickbox.app.CALL_MONITOR_STOP"
        const val ACTION_STOP_RING="com.rskickbox.app.CALL_RING_STOP"
        const val ACTION_ACCEPT_CALL="com.rskickbox.app.CALL_ACCEPT"
        const val ACTION_DECLINE_CALL="com.rskickbox.app.CALL_DECLINE"
        const val ACTION_JOIN_ROOM="com.rskickbox.app.ROOM_JOIN"
        const val ACTION_DECLINE_ROOM="com.rskickbox.app.ROOM_DECLINE"
        const val ACTION_PUSH_CALL="com.rskickbox.app.PUSH_CALL"
        const val ACTION_PUSH_ROOM="com.rskickbox.app.PUSH_ROOM"
        private const val CHANNEL_MONITOR="rs_call_monitor"
        private const val CHANNEL_CALLS="rs_incoming_calls_v4"
        private const val FOREGROUND_ID=9134

        fun start(context:Context){
            val i=Intent(context,RsCallMonitorServiceV134::class.java).setAction(ACTION_START)
            runCatching{
                if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i) else context.startService(i)
            }
        }
        fun stop(context:Context){
            runCatching{context.stopService(Intent(context,RsCallMonitorServiceV134::class.java))}
        }
        fun stopRing(context:Context){
            val i=Intent(context,RsCallMonitorServiceV134::class.java).setAction(ACTION_STOP_RING)
            runCatching{
                if(Build.VERSION.SDK_INT>=26)context.startForegroundService(i) else context.startService(i)
            }
        }
    }

    override fun onCreate(){
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        when(intent?.action){
            ACTION_PUSH_CALL->{
                startForeground(FOREGROUND_ID,monitorNotification())
                val id=intent.getStringExtra("call_id").orEmpty()
                if(id.isNotBlank()){
                    lastNotifiedCallId=id
                    startRinging()
                    showIncomingCall(
                        RsCallV131(
                            id=id,
                            callerId=intent.getStringExtra("caller_id").orEmpty(),
                            calleeId="",
                            studentId="",
                            callType=intent.getStringExtra("call_type").orEmpty().ifBlank{"AUDIO"},
                            status="RINGING",
                            peerId=intent.getStringExtra("caller_id").orEmpty(),
                            peerName=intent.getStringExtra("caller_name").orEmpty(),
                            peerEmail=intent.getStringExtra("caller_email").orEmpty(),
                            createdAt=intent.getStringExtra("created_at").orEmpty()
                        )
                    )
                }
                scope.coroutineContext.cancelChildren()
                scope.launch{monitorLoop()}
                return START_STICKY
            }
            ACTION_PUSH_ROOM->{
                startForeground(FOREGROUND_ID,monitorNotification())
                val id=intent.getStringExtra("room_id").orEmpty()
                if(id.isNotBlank()){
                    lastNotifiedRoomId=id
                    startRinging()
                    showIncomingVideoRoom(
                        RsVideoRoomV136(
                            roomId=id,
                            title=intent.getStringExtra("title").orEmpty().ifBlank{"RS Video Session"},
                            roomStatus="OPEN",
                            hostId=intent.getStringExtra("host_id").orEmpty(),
                            hostName=intent.getStringExtra("host_name").orEmpty().ifBlank{"RS Trainer"},
                            myRole="STUDENT",
                            myStatus="INVITED",
                            participantCount=intent.getStringExtra("participant_count")?.toLongOrNull()?:0L,
                            onlineCount=0L,
                            createdAt=intent.getStringExtra("created_at").orEmpty()
                        )
                    )
                }
                scope.coroutineContext.cancelChildren()
                scope.launch{monitorLoop()}
                return START_STICKY
            }
            ACTION_ACCEPT_CALL->{
                val id=intent.getStringExtra("call_id").orEmpty()
                if(id.isNotBlank())scope.launch{
                    rsSetCallStatusV131(id,"ACCEPTED")
                    stopRinging()
                    val open=Intent(this@RsCallMonitorServiceV134,MainActivity::class.java).apply{
                        action="com.rskickbox.app.INCOMING_CALL"
                        putExtra("rs_incoming_call_id",id)
                        this.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    runCatching{startActivity(open)}
                }
                return START_STICKY
            }
            ACTION_DECLINE_CALL->{
                val id=intent.getStringExtra("call_id").orEmpty()
                if(id.isNotBlank())scope.launch{
                    rsSetCallStatusV131(id,"DECLINED")
                    stopRinging()
                    getSystemService(NotificationManager::class.java).cancel(id.hashCode())
                }
                return START_STICKY
            }
            ACTION_JOIN_ROOM->{
                val id=intent.getStringExtra("room_id").orEmpty()
                if(id.isNotBlank())scope.launch{
                    rsSetVideoRoomStatusV136(id,"JOINED")
                    stopRinging()
                    val open=Intent(this@RsCallMonitorServiceV134,MainActivity::class.java).apply{
                        action="com.rskickbox.app.INCOMING_VIDEO_ROOM"
                        putExtra("rs_video_room_id",id)
                        this.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    runCatching{startActivity(open)}
                }
                return START_STICKY
            }
            ACTION_DECLINE_ROOM->{
                val id=intent.getStringExtra("room_id").orEmpty()
                if(id.isNotBlank())scope.launch{
                    rsSetVideoRoomStatusV136(id,"DECLINED")
                    stopRinging()
                    getSystemService(NotificationManager::class.java).cancel(id.hashCode())
                }
                return START_STICKY
            }
        }
        if(intent?.action==ACTION_STOP_RING){
            stopRinging()
            return START_STICKY
        }
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
        while(scope.isActive){
            val client=rsSupabaseClientV60()
            val uid=client?.auth?.currentUserOrNull()?.id.orEmpty()
            if(uid.isNotBlank()){
                runCatching{
                    rsTouchPresenceV125()
                    rsCallInboxV131().onSuccess{calls->
                        val incoming=calls.firstOrNull{
                            it.calleeId==uid && it.status=="RINGING"
                        }
                        if(incoming!=null && incoming.id!=lastNotifiedCallId){
                            lastNotifiedCallId=incoming.id
                            startRinging()
                            showIncomingCall(incoming)
                        }
                        if(incoming==null){
                            lastNotifiedCallId=null
                            stopRinging()
                        }
                    }
                    rsMyVideoRoomsV136().onSuccess{rooms->
                        val invite=rooms.firstOrNull{
                            it.myRole=="STUDENT" && it.myStatus=="INVITED" && it.roomStatus=="OPEN"
                        }
                        if(invite!=null && invite.roomId!=lastNotifiedRoomId){
                            lastNotifiedRoomId=invite.roomId
                            startRinging()
                            showIncomingVideoRoom(invite)
                        }
                        if(invite==null){
                            lastNotifiedRoomId=null
                            if(lastNotifiedCallId==null)stopRinging()
                        }
                    }
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
                    vibrationPattern=longArrayOf(0,700,350,700,350,900)
                    val ringUri=rsSavedCallRingtoneUriV138(this@RsCallMonitorServiceV134)
                    setSound(
                        ringUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
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

    private fun startRinging(){
        if(ringtone?.isPlaying==true)return
        val uri:Uri=rsSavedCallRingtoneUriV138(this)
        ringtone=RingtoneManager.getRingtone(this,uri)?.apply{
            if(Build.VERSION.SDK_INT>=28)isLooping=true
            play()
        }
    }

    private fun stopRinging(){
        runCatching{ringtone?.stop()}
        ringtone=null
    }

    private fun showIncomingCall(call:RsCallV131){
        val fullIntent=Intent(this,MainActivity::class.java).apply{
            action="com.rskickbox.app.INCOMING_CALL"
            putExtra("rs_incoming_call_id",call.id)
            putExtra("rs_incoming_call_type",call.callType)
            putExtra("rs_incoming_caller_id",call.peerId.ifBlank{call.callerId})
            putExtra("rs_incoming_caller_name",call.peerName)
            putExtra("rs_incoming_caller_email",call.peerEmail)
            flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullPending=PendingIntent.getActivity(
            this,call.id.hashCode(),fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val answerIntent=Intent(this,RsCallMonitorServiceV134::class.java).apply{
            action=ACTION_ACCEPT_CALL
            putExtra("call_id",call.id)
        }
        val declineIntent=Intent(this,RsCallMonitorServiceV134::class.java).apply{
            action=ACTION_DECLINE_CALL
            putExtra("call_id",call.id)
        }
        val answerPending=PendingIntent.getService(
            this,call.id.hashCode()+101,answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePending=PendingIntent.getService(
            this,call.id.hashCode()+102,declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val person=Person.Builder()
            .setName(call.peerName.ifBlank{call.peerEmail.ifBlank{"RS Member"}})
            .setImportant(true)
            .build()
        val isVideo=call.callType=="VIDEO"
        val notification=NotificationCompat.Builder(this,CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(person.name)
            .setContentText(if(isVideo)"Incoming RS video call" else "Incoming RS audio call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setVibrate(longArrayOf(0,700,350,700,350,900))
            .setContentIntent(fullPending)
            .setFullScreenIntent(fullPending,true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    declinePending,
                    answerPending
                )
            )
            .addPerson(person)
            .build()
        getSystemService(NotificationManager::class.java).notify(call.id.hashCode(),notification)
    }

    private fun showIncomingVideoRoom(room:RsVideoRoomV136){
        val fullIntent=Intent(this,MainActivity::class.java).apply{
            action="com.rskickbox.app.INCOMING_VIDEO_ROOM"
            putExtra("rs_video_room_id",room.roomId)
            flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullPending=PendingIntent.getActivity(
            this,room.roomId.hashCode(),fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val joinIntent=Intent(this,RsCallMonitorServiceV134::class.java).apply{
            action=ACTION_JOIN_ROOM
            putExtra("room_id",room.roomId)
        }
        val declineIntent=Intent(this,RsCallMonitorServiceV134::class.java).apply{
            action=ACTION_DECLINE_ROOM
            putExtra("room_id",room.roomId)
        }
        val joinPending=PendingIntent.getService(
            this,room.roomId.hashCode()+201,joinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePending=PendingIntent.getService(
            this,room.roomId.hashCode()+202,declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val person=Person.Builder()
            .setName(room.hostName.ifBlank{"RS Trainer"})
            .setImportant(true)
            .build()
        val notification=NotificationCompat.Builder(this,CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(room.hostName.ifBlank{"RS Trainer"})
            .setContentText("RS group video · "+room.participantCount+" participants")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVibrate(longArrayOf(0,700,350,700,350,900))
            .setContentIntent(fullPending)
            .setFullScreenIntent(fullPending,true)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    declinePending,
                    joinPending
                )
            )
            .addPerson(person)
            .build()
        getSystemService(NotificationManager::class.java).notify(room.roomId.hashCode(),notification)
    }

    override fun onDestroy(){
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent:Intent?):IBinder?=null
}

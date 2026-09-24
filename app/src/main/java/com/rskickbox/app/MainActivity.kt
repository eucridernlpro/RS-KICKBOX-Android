package com.rskickbox.app

import android.content.Intent
import android.os.Bundle
import android.app.NotificationManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.jan.supabase.auth.handleDeeplinks
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    companion object{
        @Volatile var isForeground:Boolean=false
            private set
        @Volatile var isAlive:Boolean=false
            private set
        private var activeActivity:WeakReference<MainActivity>?=null

        fun moveToBackgroundFromVoice():Boolean{
            val activity=activeActivity?.get()?:return false
            activity.runOnUiThread{activity.moveTaskToBack(true)}
            return true
        }
    }

    override fun onStart(){
        super.onStart()
        isForeground=true
    }

    override fun onStop(){
        isForeground=false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAlive=true
        activeActivity=WeakReference(this)
        applyIncomingCallWindow(intent)
        dismissIncomingCallNotification(intent)
        RsSupabaseV60.client?.handleDeeplinks(intent)
        val restoredFromAndroidState=savedInstanceState!=null
        setContent {
            RsKickboxV21App(
                initialAuthDeepLink=intent?.dataString,
                skipIntroOnRestore=restoredFromAndroidState,
                initialIncomingAction=intent?.action,
                initialIncomingCallId=intent?.getStringExtra("rs_incoming_call_id"),
                initialIncomingCallType=intent?.getStringExtra("rs_incoming_call_type"),
                initialIncomingCallerId=intent?.getStringExtra("rs_incoming_caller_id"),
                initialIncomingCallerName=intent?.getStringExtra("rs_incoming_caller_name"),
                initialIncomingCallerEmail=intent?.getStringExtra("rs_incoming_caller_email")
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIncomingCallWindow(intent)
        dismissIncomingCallNotification(intent)
        RsSupabaseV60.client?.handleDeeplinks(intent)

        // MediaSession/launcher intents can arrive while music keeps playing.
        // Do not recreate the whole activity for those: recreation used to
        // tear down Compose state and could bounce a valid user back to Login.
        val isAuthCallback=intent.data?.scheme.equals("rskickbox",ignoreCase=true) &&
            intent.data?.host.equals("auth-callback",ignoreCase=true)
        // Direct calls are already observed by the global call host. Recreating
        // a live Activity here can tear down Compose/WebRTC during the exact
        // transition when the user is trying to answer.
        val isIncomingVideoRoomAction=intent.action=="com.rskickbox.app.INCOMING_VIDEO_ROOM"
        val isVoiceAssistantAction=intent.action=="com.rskickbox.app.OPEN_AI_VOICE"
        val isRsRouteAction=intent.action=="com.rskickbox.app.OPEN_RS_ROUTE"
        if(isAuthCallback || isIncomingVideoRoomAction || isVoiceAssistantAction || isRsRouteAction)recreate()
    }

    override fun onDestroy(){
        // A configuration/recreate handoff briefly destroys the old Activity.
        // Do not mark the whole app as closed during that transition or the
        // call monitor may incorrectly switch to notification-only mode.
        if(!isChangingConfigurations){
            isAlive=false
            if(activeActivity?.get()===this)activeActivity=null
        }
        super.onDestroy()
    }

    private fun dismissIncomingCallNotification(intent:Intent?){
        val id=intent?.getStringExtra("rs_incoming_call_id").orEmpty()
        if(id.isNotBlank()){
            runCatching{getSystemService(NotificationManager::class.java).cancel(id.hashCode())}
            runCatching{RsCallMonitorServiceV134.stopRing(this)}
        }
        val roomId=intent?.getStringExtra("rs_video_room_id").orEmpty()
        if(roomId.isNotBlank()){
            runCatching{getSystemService(NotificationManager::class.java).cancel(roomId.hashCode())}
            runCatching{RsCallMonitorServiceV134.stopRing(this)}
        }
    }

    private fun applyIncomingCallWindow(intent:Intent?){
        if(
            intent?.action=="com.rskickbox.app.INCOMING_CALL" ||
            intent?.action=="com.rskickbox.app.INCOMING_VIDEO_ROOM" ||
            intent?.action=="com.rskickbox.app.BRING_RS_CALL"
        ){
            if(android.os.Build.VERSION.SDK_INT>=27){
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }else{
                @Suppress("DEPRECATION")
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }
}

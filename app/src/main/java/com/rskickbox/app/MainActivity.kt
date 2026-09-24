package com.rskickbox.app

import android.content.Intent
import android.os.Bundle
import android.app.NotificationManager
import android.app.ActivityManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.auth
import java.lang.ref.WeakReference

class MainActivity : FragmentActivity() {
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

        fun bringTaskToFrontFromVoice():Boolean{
            val activity=activeActivity?.get()?:return false
            return runCatching{
                val manager=activity.getSystemService(ActivityManager::class.java)
                manager.moveTaskToFront(activity.taskId,ActivityManager.MOVE_TASK_WITH_HOME)
                true
            }.getOrDefault(false)
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

    private var appRendered=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAlive=true
        activeActivity=WeakReference(this)
        applyIncomingCallWindow(intent)
        dismissIncomingCallNotification(intent)
        RsSupabaseV60.client?.handleDeeplinks(intent)

        if(shouldRequireWakeBiometric(intent)){
            requestWakeBiometric(intent,savedInstanceState)
        }else{
            renderApp(intent,savedInstanceState)
        }
    }

    private fun renderApp(sourceIntent:Intent?,savedInstanceState:Bundle?){
        if(appRendered)return
        appRendered=true
        val restoredFromAndroidState=savedInstanceState!=null
        setContent {
            RsKickboxV21App(
                initialAuthDeepLink=sourceIntent?.dataString,
                skipIntroOnRestore=restoredFromAndroidState,
                initialIncomingAction=sourceIntent?.action,
                initialIncomingCallId=sourceIntent?.getStringExtra("rs_incoming_call_id"),
                initialIncomingCallType=sourceIntent?.getStringExtra("rs_incoming_call_type"),
                initialIncomingCallerId=sourceIntent?.getStringExtra("rs_incoming_caller_id"),
                initialIncomingCallerName=sourceIntent?.getStringExtra("rs_incoming_caller_name"),
                initialIncomingCallerEmail=sourceIntent?.getStringExtra("rs_incoming_caller_email")
            )
        }
    }

    private fun shouldRequireWakeBiometric(sourceIntent:Intent?):Boolean{
        val wakeAction=sourceIntent?.action=="com.rskickbox.app.OPEN_AI_VOICE" ||
            sourceIntent?.action=="com.rskickbox.app.OPEN_RS_ROUTE"
        if(!wakeAction)return false

        val store=RsStore(this)
        val cloudUser=rsSupabaseClientV60()?.auth?.currentUserOrNull()
        if(cloudUser==null)return false

        val role=store.s("session_role","").ifBlank{store.s("background_call_role","")}
        if(role !in setOf("trainer","student"))return false

        val authMs=store.s("session_password_auth_ms","0").toLongOrNull()?:0L
        val activityMs=store.s("session_last_activity_ms",authMs.toString()).toLongOrNull()?:0L
        val now=System.currentTimeMillis()
        val trusted=authMs>0L &&
            now-authMs in 0..(72L*60L*60L*1000L) &&
            activityMs>0L &&
            now-activityMs in 0..(24L*60L*60L*1000L)
        if(trusted)return false

        val authenticators=
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(this).canAuthenticate(authenticators)==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun requestWakeBiometric(sourceIntent:Intent?,savedInstanceState:Bundle?){
        val executor=ContextCompat.getMainExecutor(this)
        val prompt=BiometricPrompt(
            this,
            executor,
            object:BiometricPrompt.AuthenticationCallback(){
                override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){
                    super.onAuthenticationSucceeded(result)
                    val store=RsStore(this@MainActivity)
                    val role=store.s("session_role","").ifBlank{store.s("background_call_role","")}
                    val now=System.currentTimeMillis()
                    if(role in setOf("trainer","student")){
                        store.ps("session_role",role)
                        store.ps("session_password_auth_ms",now.toString())
                        store.ps("session_last_activity_ms",now.toString())
                    }
                    renderApp(sourceIntent,savedInstanceState)
                }

                override fun onAuthenticationError(errorCode:Int,errString:CharSequence){
                    super.onAuthenticationError(errorCode,errString)
                    renderApp(sourceIntent,savedInstanceState)
                }
            }
        )
        val info=BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock RS KICKBOXING")
            .setSubtitle("Confirm your identity to continue the RS voice request")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
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
        if(isVoiceAssistantAction || isRsRouteAction){
            if(shouldRequireWakeBiometric(intent)){
                requestWakeBiometric(intent,null)
                return
            }
        }
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

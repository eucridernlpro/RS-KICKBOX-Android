package com.rskickbox.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIncomingCallWindow(intent)
        RsSupabaseV60.client?.handleDeeplinks(intent)
        val restoredFromAndroidState=savedInstanceState!=null
        setContent {
            RsKickboxV21App(
                initialAuthDeepLink=intent?.dataString,
                skipIntroOnRestore=restoredFromAndroidState,
                initialIncomingAction=intent?.action
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIncomingCallWindow(intent)
        RsSupabaseV60.client?.handleDeeplinks(intent)

        // MediaSession/launcher intents can arrive while music keeps playing.
        // Do not recreate the whole activity for those: recreation used to
        // tear down Compose state and could bounce a valid user back to Login.
        val isAuthCallback=intent.data?.scheme.equals("rskickbox",ignoreCase=true) &&
            intent.data?.host.equals("auth-callback",ignoreCase=true)
        val isIncomingCallAction=
            intent.action=="com.rskickbox.app.INCOMING_CALL" ||
            intent.action=="com.rskickbox.app.INCOMING_VIDEO_ROOM"
        if(isAuthCallback || isIncomingCallAction)recreate()
    }

    private fun applyIncomingCallWindow(intent:Intent?){
        if(intent?.action=="com.rskickbox.app.INCOMING_CALL" || intent?.action=="com.rskickbox.app.INCOMING_VIDEO_ROOM"){
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

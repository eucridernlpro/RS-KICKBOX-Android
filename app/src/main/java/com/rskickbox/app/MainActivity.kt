package com.rskickbox.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RsSupabaseV60.client?.handleDeeplinks(intent)
        setContent { RsKickboxV21App(intent?.dataString) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        RsSupabaseV60.client?.handleDeeplinks(intent)
        recreate()
    }
}

package com.rskickbox.app

enum class RsBackendModeV36 { LOCAL_ACCEPTANCE, SUPABASE_CONFIGURED }

data class RsBackendStatusV36(
    val mode:RsBackendModeV36,
    val projectUrlConfigured:Boolean,
    val publishableKeyConfigured:Boolean
){
    val readyForClientInitialization:Boolean
        get()=projectUrlConfigured && publishableKeyConfigured
}

fun rsBackendStatusV36():RsBackendStatusV36{
    val hasUrl=BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_URL.contains(".supabase.co")
    val hasKey=BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()
    return RsBackendStatusV36(
        mode=if(hasUrl&&hasKey)RsBackendModeV36.SUPABASE_CONFIGURED else RsBackendModeV36.LOCAL_ACCEPTANCE,
        projectUrlConfigured=hasUrl,
        publishableKeyConfigured=hasKey
    )
}

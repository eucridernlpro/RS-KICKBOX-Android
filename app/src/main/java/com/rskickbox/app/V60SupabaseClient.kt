package com.rskickbox.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Production backend boundary.
 *
 * The app remains fully usable in LOCAL_ACCEPTANCE mode when build-time
 * Supabase configuration is absent. No secret/service-role key belongs here.
 */
object RsSupabaseV60 {
    val client:SupabaseClient? by lazy {
        val status=rsBackendStatusV36()
        if(!status.readyForClientInitialization){
            null
        }else{
            createSupabaseClient(
                supabaseUrl=BuildConfig.SUPABASE_URL,
                supabaseKey=BuildConfig.SUPABASE_PUBLISHABLE_KEY
            ){
                install(Auth)
                install(Postgrest)
                install(Functions)
            }
        }
    }

    val configured:Boolean
        get()=client!=null
}

fun rsSupabaseClientV60():SupabaseClient?=RsSupabaseV60.client

package com.rskickbox.app

import io.github.jan.supabase.postgrest.from

enum class RsSupabaseSchemaStateV62 {
    NOT_CONFIGURED,
    CLIENT_CONFIGURED_SCHEMA_PENDING,
    SCHEMA_READY
}

data class RsSupabaseProbeV62(
    val state:RsSupabaseSchemaStateV62,
    val message:String
)

suspend fun rsSupabaseProbeV62():RsSupabaseProbeV62{
    val client=rsSupabaseClientV60()
        ?:return RsSupabaseProbeV62(
            RsSupabaseSchemaStateV62.NOT_CONFIGURED,
            "Supabase client is not configured in this build."
        )

    return try{
        client.from("rs_app_settings").select()
        RsSupabaseProbeV62(
            RsSupabaseSchemaStateV62.SCHEMA_READY,
            "Supabase project and RS KICKBOX schema are reachable."
        )
    }catch(t:Throwable){
        RsSupabaseProbeV62(
            RsSupabaseSchemaStateV62.CLIENT_CONFIGURED_SCHEMA_PENDING,
            "Supabase client is configured, but the RS KICKBOX schema is not reachable yet."
        )
    }
}

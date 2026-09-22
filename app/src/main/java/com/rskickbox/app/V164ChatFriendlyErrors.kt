package com.rskickbox.app

fun rsChatFriendlyCloudErrorV164(raw:String,fallback:String):String{
    val s=raw.lowercase()
    return when{
        "pgrst202" in s || "schema cache" in s || "could not find the function" in s ->
            fallback+" The app is using compatibility mode until the latest RS backend update is available."
        "network" in s || "timeout" in s || "unable to resolve host" in s ->
            "Connection problem. Check your internet connection and try again."
        "jwt" in s || "authentication" in s || "not authenticated" in s ->
            "Your session needs to reconnect. Please reopen RS KICKBOXING and try again."
        else->raw.takeIf{it.isNotBlank() && it.length<180}?:fallback
    }
}

package com.rskickbox.app

import java.text.Normalizer
import java.util.Locale

data class RsAiResolvedContactV194(
    val userId:String,
    val displayName:String,
    val email:String
)

private fun rsAiNormalizePersonV194(value:String):String =
    Normalizer.normalize(value.trim().lowercase(Locale.ROOT),Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"")
        .replace(Regex("[^\\p{L}\\p{N}@._ -]")," ")
        .replace(Regex("\\s+")," ")
        .trim()

fun rsAiBestContactV194(
    contacts:List<RsChatContactV125>,
    spokenName:String
):RsAiResolvedContactV194?{
    val needle=rsAiNormalizePersonV194(spokenName)
    if(needle.isBlank())return null

    val ranked=contacts.mapNotNull{contact->
        val display=rsAiNormalizePersonV194(contact.displayName)
        val email=rsAiNormalizePersonV194(contact.email)
        val emailName=email.substringBefore("@")
        val score=when{
            display==needle -> 1000
            emailName==needle -> 950
            display.startsWith(needle) || needle.startsWith(display) -> 800
            emailName.startsWith(needle) || needle.startsWith(emailName) -> 750
            display.contains(needle) || needle.contains(display) -> 650
            emailName.contains(needle) || needle.contains(emailName) -> 600
            else -> {
                val wanted=needle.split(" ").filter{it.length>=2}
                val available=(display+" "+emailName).split(" ").filter{it.length>=2}
                wanted.count{w->available.any{a->a==w || a.startsWith(w) || w.startsWith(a)}}*100
            }
        }
        if(score<=0)null else contact to score
    }.sortedByDescending{it.second}

    val best=ranked.firstOrNull()?.first?:return null
    return RsAiResolvedContactV194(
        userId=best.userId,
        displayName=best.displayName.ifBlank{best.email.substringBefore("@")},
        email=best.email
    )
}

fun rsAiResolveContactsV194(
    contacts:List<RsChatContactV125>,
    names:List<String>
):Pair<List<RsAiResolvedContactV194>,List<String>>{
    val resolved=mutableListOf<RsAiResolvedContactV194>()
    val missing=mutableListOf<String>()
    names.forEach{name->
        val match=rsAiBestContactV194(contacts,name)
        if(match==null)missing+=name
        else if(resolved.none{it.userId==match.userId})resolved+=match
    }
    return resolved to missing
}

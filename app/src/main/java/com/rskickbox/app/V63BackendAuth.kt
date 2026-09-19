package com.rskickbox.app

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject

@Serializable
data class RsCloudProfileV63(
    val id:String,
    val email:String,
    @SerialName("display_name") val displayName:String="",
    val role:String="student",
    val plan:String="PRO",
    val active:Boolean=true,
    @SerialName("avatar_path") val avatarPath:String?=null
)

data class RsCloudSessionV63(
    val role:RsRole,
    val email:String,
    val displayName:String,
    val plan:String
)

data class RsCloudInviteV63(
    val id:String,
    val email:String,
    val displayName:String,
    val plan:String,
    val expiresAt:String,
    val inviteUri:String
)

suspend fun rsCloudLoginV63(email:String,password:String):Result<RsCloudSessionV63> = runCatching {
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(email.trim().contains("@")){"Enter a valid email address."}
    require(password.isNotBlank()){"Enter your password."}

    try{
        client.auth.signInWith(Email){
            this.email=email.trim()
            this.password=password
        }
    }catch(t:Throwable){
        val raw=t.message.orEmpty()
        val friendly=when{
            raw.contains("Invalid login credentials",ignoreCase=true)->"Email or password is incorrect."
            raw.contains("Email not confirmed",ignoreCase=true)->"This email address is not confirmed yet."
            raw.contains("network",ignoreCase=true) || raw.contains("timeout",ignoreCase=true)->"Could not reach the RS KICKBOX server. Check your internet connection and try again."
            raw.isBlank()->"The login request was not accepted."
            else->raw
        }
        error(friendly)
    }

    val user=client.auth.currentUserOrNull() ?: error("Sign-in completed without an active user session.")
    val profile=client.from("rs_profiles").select{
        filter{eq("id",user.id)}
    }.decodeSingle<RsCloudProfileV63>()

    require(profile.active){"This RS KICKBOX account is inactive."}
    val appRole=if(profile.role=="trainer" || profile.role=="admin") RsRole.TRAINER else RsRole.STUDENT
    RsCloudSessionV63(
        role=appRole,
        email=profile.email,
        displayName=profile.displayName.ifBlank{profile.email},
        plan=profile.plan
    )
}


suspend fun rsCloudCurrentSessionV67():Result<RsCloudSessionV63?> = runCatching {
    val client=rsSupabaseClientV60() ?: return@runCatching null
    val user=client.auth.currentUserOrNull() ?: return@runCatching null
    val profile=client.from("rs_profiles").select{
        filter{eq("id",user.id)}
    }.decodeSingle<RsCloudProfileV63>()
    if(!profile.active){
        client.auth.signOut()
        return@runCatching null
    }
    val appRole=if(profile.role=="trainer" || profile.role=="admin") RsRole.TRAINER else RsRole.STUDENT
    RsCloudSessionV63(
        role=appRole,
        email=profile.email,
        displayName=profile.displayName.ifBlank{profile.email},
        plan=profile.plan
    )
}

suspend fun rsCloudStudentsV67():Result<List<RsStudentAccountV33>> = runCatching {
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    client.from("rs_profiles").select{
        filter{eq("role","student")}
    }.decodeList<RsCloudProfileV63>()
        .sortedBy{it.displayName.lowercase()}
        .map{profile->
            RsStudentAccountV33(
                id=profile.id,
                name=profile.displayName,
                email=profile.email,
                plan=profile.plan,
                activationCode="",
                active=profile.active,
                createdAt=0L
            )
        }
}

suspend fun rsCloudLogoutV63():Result<Unit> = runCatching {
    rsSupabaseClientV60()?.auth?.signOut()
}

suspend fun rsCloudRequestPasswordResetV87(email:String):Result<Unit> = runCatching {
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val clean=email.trim()
    require(clean.contains("@")){"Enter a valid email address."}
    client.auth.resetPasswordForEmail(
        email=clean,
        redirectUrl="rskickbox://auth-callback"
    )
}

suspend fun rsCloudUpdatePasswordV87(newPassword:String):Result<Unit> = runCatching {
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(newPassword.length>=10){"Choose a password with at least 10 characters."}
    require(client.auth.currentUserOrNull()!=null){"Open the password reset link from your email first."}
    client.auth.updateUser {
        password=newPassword
    }
    client.auth.signOut()
}


suspend fun rsCreateStudentInviteV63(
    name:String,
    email:String,
    plan:String
):Result<RsCloudInviteV63> = runCatching {
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val response=client.functions.invoke(
        function="create-student-invite",
        body=buildJsonObject{
            put("display_name",name.trim())
            put("email",email.trim())
            put("plan",plan)
        }
    )
    val raw=response.bodyAsText()
    if(!response.status.isSuccess()){
        val message=runCatching{JSONObject(raw).optString("error")}.getOrNull().orEmpty()
        val safeMessage=when{
            message.contains("capacity",ignoreCase=true)->"Could not verify student capacity. Backend connection needs attention."
            message.contains("access",ignoreCase=true)->"Trainer/admin authorization was not accepted."
            message.isNotBlank()->message
            else->"Could not create student invitation ("+response.status.value+")."
        }
        error(safeMessage)
    }
    val json=JSONObject(raw)
    RsCloudInviteV63(
        id=json.getString("invite_id"),
        email=json.getString("email"),
        displayName=json.getString("display_name"),
        plan=json.getString("plan"),
        expiresAt=json.getString("expires_at"),
        inviteUri=json.getString("invite_uri")
    )
}

suspend fun rsRedeemStudentInviteV63(
    email:String,
    token:String,
    password:String
):Result<Unit> = runCatching {
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    require(password.length>=10){"Choose a password with at least 10 characters."}
    val response=client.functions.invoke(
        function="redeem-student-invite",
        body=buildJsonObject{
            put("email",email.trim())
            put("token",token)
            put("password",password)
        }
    )
    val raw=response.bodyAsText()
    if(!response.status.isSuccess()){
        val message=runCatching{JSONObject(raw).optString("error")}.getOrNull().orEmpty()
        error(message.ifBlank{"Could not activate invitation ("+response.status.value+")."})
    }
}

fun rsCloudInviteAsLocalV63(invite:RsCloudInviteV63):RsStudentAccountV33 =
    RsStudentAccountV33(
        id=invite.id,
        name=invite.displayName,
        email=invite.email,
        plan=invite.plan,
        activationCode=invite.inviteUri,
        active=true,
        createdAt=System.currentTimeMillis()
    )

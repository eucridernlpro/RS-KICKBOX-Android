package com.rskickbox.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import kotlin.math.min

private const val RS_PROFILE_BUCKET_V68="rs-profile-images"

@Serializable
data class RsMemberIdentityV68(
    val id:String,
    @SerialName("display_name") val displayName:String="",
    @SerialName("avatar_path") val avatarPath:String?=null
)

private fun rsAvatarBytesV68(context:Context,uri:Uri):ByteArray{
    val bitmap=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}
        ?:error("Could not read the selected profile image.")

    val side=min(bitmap.width,bitmap.height)
    val left=(bitmap.width-side)/2
    val top=(bitmap.height-side)/2
    val square=Bitmap.createBitmap(bitmap,left,top,side,side)
    val scaled=Bitmap.createScaledBitmap(square,512,512,true)
    val out=ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG,88,out)

    if(square!==bitmap)square.recycle()
    if(scaled!==square)scaled.recycle()
    bitmap.recycle()

    val bytes=out.toByteArray()
    require(bytes.size<=2*1024*1024){"Profile image is too large after optimization."}
    return bytes
}

suspend fun rsUploadMyAvatarV68(context:Context,uri:Uri):Result<String> = runCatching{
    val client=rsSupabaseClientV60() ?: error("RS KICKBOX cloud backend is not configured.")
    val user=client.auth.currentUserOrNull() ?: error("Please sign in again.")
    val path=user.id+"/avatar.jpg"
    val bytes=rsAvatarBytesV68(context,uri)

    client.storage.from(RS_PROFILE_BUCKET_V68).upload(path,bytes){
        upsert=true
        contentType=ContentType.Image.JPEG
    }

    client.postgrest.rpc(
        "rs_set_my_avatar",
        buildJsonObject{put("p_avatar_path",path)}
    )

    path
}

suspend fun rsMemberIdentityV68(email:String):RsMemberIdentityV68?{
    if(email.isBlank())return null
    val client=rsSupabaseClientV60() ?: return null
    return runCatching{
        client.postgrest.rpc(
            "rs_member_identity",
            buildJsonObject{put("p_email",email.trim())}
        ).decodeList<RsMemberIdentityV68>().firstOrNull()
    }.getOrNull()
}

suspend fun rsAvatarBitmapV68(email:String):ImageBitmap?{
    val client=rsSupabaseClientV60() ?: return null
    val identity=rsMemberIdentityV68(email) ?: return null
    val path=identity.avatarPath?.takeIf{it.isNotBlank()} ?: return null
    return runCatching{
        val bytes=client.storage.from(RS_PROFILE_BUCKET_V68).downloadAuthenticated(path)
        BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun RsMemberAvatarV68(
    c:RsPalette,
    email:String,
    name:String,
    modifier:Modifier=Modifier,
    size:Dp=42.dp,
    refreshKey:Int=0
){
    var bitmap by remember(email,refreshKey){mutableStateOf<ImageBitmap?>(null)}
    LaunchedEffect(email,refreshKey){
        bitmap=rsAvatarBitmapV68(email)
    }

    Surface(
        shape=CircleShape,
        color=c.gold.copy(alpha=.24f),
        modifier=modifier.size(size)
    ){
        val image=bitmap
        if(image!=null){
            Image(
                bitmap=image,
                contentDescription="Profile photo for "+name,
                contentScale=ContentScale.Crop,
                modifier=Modifier.clip(CircleShape)
            )
        }else{
            Box(contentAlignment=Alignment.Center){
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "♛",
                    color=c.bright,
                    fontWeight=FontWeight.Black,
                    fontSize=(size.value*.38f).sp
                )
            }
        }
    }
}

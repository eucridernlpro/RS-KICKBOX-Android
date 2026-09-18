package com.rskickbox.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

const val RS_PLAY_STORE_URL_V33 = "https://play.google.com/store/apps/details?id=com.rskickbox.app"
const val RS_STUDENT_LIMIT_V33 = 100

data class RsStudentAccountV33(
    val id:String,
    val name:String,
    val email:String,
    val plan:String,
    val activationCode:String,
    val active:Boolean,
    val createdAt:Long
)

fun rsLoadStudentsV33(store:RsStore):List<RsStudentAccountV33>{
    val raw=store.s("students_v33","")
    if(raw.isBlank())return emptyList()
    return runCatching{
        val arr=JSONArray(raw)
        buildList{
            for(i in 0 until arr.length()){
                val o=arr.getJSONObject(i)
                add(
                    RsStudentAccountV33(
                        id=o.optString("id"),
                        name=o.optString("name"),
                        email=o.optString("email"),
                        plan=o.optString("plan","PRO"),
                        activationCode=o.optString("code"),
                        active=o.optBoolean("active",true),
                        createdAt=o.optLong("createdAt",0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

fun rsSaveStudentsV33(store:RsStore,items:List<RsStudentAccountV33>){
    val arr=JSONArray()
    items.forEach{item->
        arr.put(JSONObject().apply{
            put("id",item.id)
            put("name",item.name)
            put("email",item.email)
            put("plan",item.plan)
            put("code",item.activationCode)
            put("active",item.active)
            put("createdAt",item.createdAt)
        })
    }
    store.ps("students_v33",arr.toString())
}

fun rsNewActivationCodeV33():String =
    UUID.randomUUID().toString().replace("-","").take(8).uppercase()

fun rsInvitePayloadV33(account:RsStudentAccountV33):String =
    Uri.Builder()
        .scheme("rskickbox")
        .authority("invite")
        .appendQueryParameter("v","1")
        .appendQueryParameter("id",account.id)
        .appendQueryParameter("name",account.name)
        .appendQueryParameter("email",account.email)
        .appendQueryParameter("plan",account.plan)
        .appendQueryParameter("code",account.activationCode)
        .build()
        .toString()

fun rsParseInviteV33(payload:String):RsStudentAccountV33?{
    return runCatching{
        val uri=Uri.parse(payload)
        if(uri.scheme!="rskickbox" || uri.host!="invite")return null
        val email=uri.getQueryParameter("email").orEmpty().trim()
        val code=uri.getQueryParameter("code").orEmpty().trim()
        if(email.isBlank()||code.isBlank())return null
        RsStudentAccountV33(
            id=uri.getQueryParameter("id").orEmpty(),
            name=uri.getQueryParameter("name").orEmpty(),
            email=email,
            plan=uri.getQueryParameter("plan").orEmpty().ifBlank{"PRO"},
            activationCode=code,
            active=true,
            createdAt=0L
        )
    }.getOrNull()
}

fun rsFindStudentV33(store:RsStore,email:String,activationCode:String):RsStudentAccountV33? =
    rsLoadStudentsV33(store).firstOrNull{
        it.active &&
        it.email.equals(email.trim(),ignoreCase=true) &&
        it.activationCode.equals(activationCode.trim(),ignoreCase=true)
    }

fun rsQrBitmapV33(payload:String,size:Int=900):Bitmap{
    val matrix=QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,size,size)
    val pixels=IntArray(size*size)
    for(y in 0 until size){
        for(x in 0 until size){
            pixels[y*size+x]=if(matrix[x,y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888)
}

fun rsDecodeQrImageV33(context:Context,uri:Uri):String?{
    return runCatching{
        val bitmap=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?:return null
        val width=bitmap.width
        val height=bitmap.height
        val pixels=IntArray(width*height)
        bitmap.getPixels(pixels,0,width,0,0,width,height)
        val source=RGBLuminanceSource(width,height,pixels)
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }.getOrNull()
}

fun rsShareStudentInviteV33(context:Context,account:RsStudentAccountV33){
    val payload=rsInvitePayloadV33(account)
    val bitmap=rsQrBitmapV33(payload)
    val dir=File(context.cacheDir,"shared").apply{mkdirs()}
    val file=File(dir,"RS_KICKBOX_${account.id.ifBlank{"invite"}}.png")
    FileOutputStream(file).use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
    val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
    val text=buildString{
        append("RS KICKBOX invitation for ")
        append(account.name)
        append("\\n\\nInstall the app: ")
        append(RS_PLAY_STORE_URL_V33)
        append("\\n\\nScan the attached QR on the RS KICKBOX login page. Your activation code is inside the QR invitation.")
    }
    val intent=Intent(Intent.ACTION_SEND).apply{
        type="image/png"
        putExtra(Intent.EXTRA_STREAM,uri)
        putExtra(Intent.EXTRA_TEXT,text)
        clipData=ClipData.newRawUri("RS KICKBOX invitation QR",uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent,"Share RS KICKBOX invitation"))
}

fun rsSharePlayStoreLinkV33(context:Context){
    val intent=Intent(Intent.ACTION_SEND).apply{
        type="text/plain"
        putExtra(Intent.EXTRA_TEXT,"Download RS KICKBOX: $RS_PLAY_STORE_URL_V33")
    }
    context.startActivity(Intent.createChooser(intent,"Share RS KICKBOX download link"))
}

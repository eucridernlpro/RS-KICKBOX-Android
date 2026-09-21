package com.rskickbox.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational

private fun rsFindActivityV154(context:Context):Activity?{
    var current:Context?=context
    while(current is ContextWrapper){
        if(current is Activity)return current
        current=current.baseContext
    }
    return current as? Activity
}

fun rsConfigureCallPipV154(
    context:Context,
    enabled:Boolean,
    aspectWidth:Int=9,
    aspectHeight:Int=16
){
    if(Build.VERSION.SDK_INT<26)return
    val activity=rsFindActivityV154(context)?:return
    val builder=PictureInPictureParams.Builder()
        .setAspectRatio(Rational(aspectWidth,aspectHeight))
    if(Build.VERSION.SDK_INT>=31){
        builder.setAutoEnterEnabled(enabled)
        builder.setSeamlessResizeEnabled(true)
    }
    runCatching{activity.setPictureInPictureParams(builder.build())}
}

fun rsEnterCallPipV154(
    context:Context,
    aspectWidth:Int=9,
    aspectHeight:Int=16
):Boolean{
    if(Build.VERSION.SDK_INT<26)return false
    val activity=rsFindActivityV154(context)?:return false
    val builder=PictureInPictureParams.Builder()
        .setAspectRatio(Rational(aspectWidth,aspectHeight))
    if(Build.VERSION.SDK_INT>=31){
        builder.setAutoEnterEnabled(true)
        builder.setSeamlessResizeEnabled(true)
    }
    val params=builder.build()
    return runCatching{
        activity.setPictureInPictureParams(params)
        activity.enterPictureInPictureMode(params)
    }.getOrDefault(false)
}

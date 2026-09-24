package com.rskickbox.app

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RsAiReal3DModelV183(
    avatar:String,
    speaking:Boolean,
    listening:Boolean,
    thinking:Boolean,
    sensorX:Float,
    sensorY:Float,
    modifier:Modifier=Modifier
){
    val context=LocalContext.current
    val view=remember{
        RsAi3DViewV183(context).also{it.onResume()}
    }
    DisposableEffect(view){
        onDispose{runCatching{view.onPause()}}
    }
    AndroidView(
        factory={view},
        update={
            it.updateState(
                avatar=avatar,
                speaking=speaking,
                listening=listening,
                thinking=thinking,
                sensorX=sensorX,
                sensorY=sensorY
            )
        },
        modifier=modifier
    )
}

private class RsAi3DViewV183(context:Context):GLSurfaceView(context){
    private val rsRenderer=RsAi3DRendererV183()
    init{
        setEGLContextClientVersion(2)
        setRenderer(rsRenderer)
        renderMode=RENDERMODE_CONTINUOUSLY
        setZOrderOnTop(false)
        preserveEGLContextOnPause=true
    }

    fun updateState(
        avatar:String,
        speaking:Boolean,
        listening:Boolean,
        thinking:Boolean,
        sensorX:Float,
        sensorY:Float
    ){
        rsRenderer.avatar=avatar
        rsRenderer.speaking=speaking
        rsRenderer.listening=listening
        rsRenderer.thinking=thinking
        rsRenderer.sensorX=sensorX
        rsRenderer.sensorY=sensorY
    }
}

private data class RsGlMeshV183(
    val vertices:FloatBuffer,
    val indices:ShortBuffer,
    val indexCount:Int
)

private class RsAi3DRendererV183:GLSurfaceView.Renderer{
    @Volatile var avatar:String="FEMALE"
    @Volatile var speaking:Boolean=false
    @Volatile var listening:Boolean=false
    @Volatile var thinking:Boolean=false
    @Volatile var sensorX:Float=0f
    @Volatile var sensorY:Float=0f

    private var program=0
    private var aPosition=0
    private var aNormal=0
    private var uMvp=0
    private var uModel=0
    private var uColor=0
    private var uLight=0

    private lateinit var sphere:RsGlMeshV183
    private lateinit var cylinder:RsGlMeshV183
    private lateinit var cube:RsGlMeshV183

    private val projection=FloatArray(16)
    private val view=FloatArray(16)
    private val vp=FloatArray(16)
    private val model=FloatArray(16)
    private val mvp=FloatArray(16)

    override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,config:javax.microedition.khronos.egl.EGLConfig?){
        GLES20.glClearColor(0.004f,0.009f,0.014f,1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program=buildProgram()
        aPosition=GLES20.glGetAttribLocation(program,"aPosition")
        aNormal=GLES20.glGetAttribLocation(program,"aNormal")
        uMvp=GLES20.glGetUniformLocation(program,"uMvp")
        uModel=GLES20.glGetUniformLocation(program,"uModel")
        uColor=GLES20.glGetUniformLocation(program,"uColor")
        uLight=GLES20.glGetUniformLocation(program,"uLight")

        sphere=makeSphere(18,24)
        cylinder=makeCylinder(24)
        cube=makeCube()
    }

    override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,width:Int,height:Int){
        GLES20.glViewport(0,0,width,height)
        val ratio=width.toFloat()/height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projection,0,38f,ratio,.1f,30f)
    }

    override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val t=SystemClock.uptimeMillis()/1000f
        val breath=sin(t*1.55f)*.018f
        val talk=if(speaking)(sin(t*12f)*.5f+.5f) else 0f
        val idleYaw=sin(t*.42f)*1.8f
        val eyeFocus=sin(t*.67f)*.016f
        val roomYaw=(-sensorX*10f).coerceIn(-9f,9f)
        val roomPitch=(sensorY*6f).coerceIn(-5f,5f)

        Matrix.setLookAtM(
            view,0,
            sensorX*.23f,.18f+sensorY*.12f,4.35f,
            0f,.28f,0f,
            0f,1f,0f
        )
        Matrix.multiplyMM(vp,0,projection,0,view,0)

        drawRoom(t)

        val root=FloatArray(16)
        Matrix.setIdentityM(root,0)
        Matrix.rotateM(root,0,idleYaw+roomYaw,0f,1f,0f)
        Matrix.rotateM(root,0,roomPitch,1f,0f,0f)
        Matrix.translateM(root,0,0f,-.12f,0f)

        val female=avatar=="FEMALE"
        val skin=if(female) floatArrayOf(.76f,.52f,.40f,1f) else floatArrayOf(.58f,.38f,.28f,1f)
        val suit=if(female) floatArrayOf(.025f,.032f,.045f,1f) else floatArrayOf(.018f,.024f,.031f,1f)
        val gold=floatArrayOf(.86f,.61f,.13f,1f)
        val neon=when{
            speaking->floatArrayOf(.18f,.72f,1f,1f)
            listening->floatArrayOf(.18f,.92f,.62f,1f)
            thinking->floatArrayOf(.95f,.68f,.18f,1f)
            else->floatArrayOf(.70f,.48f,.10f,1f)
        }

        // Torso / core.
        part(cube,root,0f,.34f,0f,if(female).43f else .50f,.66f+breath,if(female).22f else .26f,suit)
        part(cube,root,0f,-.23f,0f,if(female).39f else .44f,.30f,.23f,suit)
        part(cylinder,root,0f,.75f,0f,.105f,.16f,.105f,skin)

        // Head and face.
        val head=FloatArray(16)
        copy(root,head)
        Matrix.translateM(head,0,0f,1.04f,0f)
        Matrix.rotateM(head,0,sin(t*.58f)*1.8f,0f,1f,0f)
        Matrix.rotateM(head,0,sin(t*.37f)*.9f,1f,0f,0f)
        drawMesh(sphere,head,if(female).275f else .29f,if(female).34f else .35f,.275f,skin)

        // Hair shell, eyes and a speech-reactive jaw/mouth.
        part(sphere,head,0f,.105f,-.045f,if(female).283f else .297f,.23f,.285f,floatArrayOf(.025f,.018f,.015f,1f))
        part(sphere,head,-.09f,.035f,.245f+eyeFocus,.026f,.018f,.015f,neon)
        part(sphere,head,.09f,.035f,.245f-eyeFocus,.026f,.018f,.015f,neon)
        part(cube,head,0f,-.105f,.263f,.09f,.018f+talk*.022f,.012f,floatArrayOf(.08f,.015f,.018f,1f))

        // Shoulders and arms.
        val shoulderY=.58f
        arm(root,-1f,shoulderY,female,suit,gold,t)
        arm(root,1f,shoulderY,female,suit,gold,t)

        // Legs.
        leg(root,-1f,female,suit,gold,t)
        leg(root,1f,female,suit,gold,t)

        // Chest mark / live core.
        part(sphere,root,0f,.43f,.235f,.07f,.07f,.035f,neon)
        part(sphere,root,0f,.43f,.244f,.034f,.034f,.018f,gold)
    }

    private fun drawRoom(t:Float){
        val gold=floatArrayOf(.42f,.27f,.06f,.35f)
        val blue=floatArrayOf(.04f,.36f,.56f,.28f)
        val dark=floatArrayOf(.008f,.014f,.021f,1f)

        Matrix.setIdentityM(model,0)
        part(cube,model,0f,-1.02f,-.20f,3.8f,.025f,3.8f,dark)
        part(cube,model,0f,.35f,-1.55f,3.6f,2.65f,.03f,dark)

        // Perspective rails make phone parallax visibly read as a room.
        for(i in -3..3){
            part(cube,model,i*.52f,-.68f,-.55f,.008f,.012f,1.65f,if(i%2==0)gold else blue)
        }
        for(i in -2..3){
            part(cube,model,-1.72f,i*.40f,-.82f,.012f,.008f,1.35f,gold)
            part(cube,model,1.72f,i*.40f,-.82f,.012f,.008f,1.35f,blue)
        }

        val pulse=(sin(t*1.8f)*.5f+.5f)
        part(sphere,model,0f,.10f,-1.44f,.56f+pulse*.04f,.56f+pulse*.04f,.025f,floatArrayOf(.04f,.22f,.35f,.11f))
    }

    private fun arm(root:FloatArray,side:Float,shoulderY:Float,female:Boolean,suit:FloatArray,gold:FloatArray,t:Float){
        val upper=FloatArray(16)
        copy(root,upper)
        Matrix.translateM(upper,0,side*(if(female).48f else .56f),shoulderY,0f)
        Matrix.rotateM(upper,0,side*(13f+sin(t*.55f)*1.7f),0f,0f,1f)
        Matrix.rotateM(upper,0,-7f,1f,0f,0f)
        drawMesh(cylinder,upper,.095f,.42f,.095f,suit)

        val fore=FloatArray(16)
        copy(root,fore)
        Matrix.translateM(fore,0,side*(if(female).60f else .69f),.22f,.055f)
        Matrix.rotateM(fore,0,side*(9f+sin(t*.55f+.7f)*2f),0f,0f,1f)
        Matrix.rotateM(fore,0,-4f,1f,0f,0f)
        drawMesh(cylinder,fore,.082f,.37f,.082f,suit)

        part(sphere,root,side*(if(female).66f else .75f),-.03f,.10f,.14f,.13f,.17f,gold)
    }

    private fun leg(root:FloatArray,side:Float,female:Boolean,suit:FloatArray,gold:FloatArray,t:Float){
        val hip=side*(if(female).19f else .22f)
        val thigh=FloatArray(16)
        copy(root,thigh)
        Matrix.translateM(thigh,0,hip,-.60f,0f)
        Matrix.rotateM(thigh,0,side*sin(t*.33f)*.8f,0f,0f,1f)
        drawMesh(cylinder,thigh,if(female).115f else .13f,.50f,if(female).115f else .13f,suit)

        val shin=FloatArray(16)
        copy(root,shin)
        Matrix.translateM(shin,0,hip,-1.08f,.015f)
        drawMesh(cylinder,shin,if(female).09f else .105f,.47f,if(female).09f else .105f,suit)
        part(cube,root,hip,-1.34f,.10f,.13f,.08f,.27f,floatArrayOf(.018f,.022f,.028f,1f))
        part(cube,root,hip,-.96f,.105f,.105f,.035f,.12f,gold)
    }

    private fun part(mesh:RsGlMeshV183,parent:FloatArray,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,color:FloatArray){
        val local=FloatArray(16)
        copy(parent,local)
        Matrix.translateM(local,0,x,y,z)
        drawMesh(mesh,local,sx,sy,sz,color)
    }

    private fun drawMesh(mesh:RsGlMeshV183,parent:FloatArray,sx:Float,sy:Float,sz:Float,color:FloatArray){
        copy(parent,model)
        Matrix.scaleM(model,0,sx,sy,sz)
        Matrix.multiplyMM(mvp,0,vp,0,model,0)

        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0)
        GLES20.glUniformMatrix4fv(uModel,1,false,model,0)
        GLES20.glUniform4fv(uColor,1,color,0)
        GLES20.glUniform3f(uLight,-.35f,.72f,.62f)

        mesh.vertices.position(0)
        GLES20.glVertexAttribPointer(aPosition,3,GLES20.GL_FLOAT,false,24,mesh.vertices)
        GLES20.glEnableVertexAttribArray(aPosition)
        mesh.vertices.position(3)
        GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,24,mesh.vertices)
        GLES20.glEnableVertexAttribArray(aNormal)

        mesh.indices.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,mesh.indexCount,GLES20.GL_UNSIGNED_SHORT,mesh.indices)
    }

    private fun copy(src:FloatArray,dst:FloatArray){
        System.arraycopy(src,0,dst,0,16)
    }

    private fun buildProgram():Int{
        val vertex=compileShader(
            GLES20.GL_VERTEX_SHADER,
            """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vWorld;
            void main(){
                vec4 world=uModel*vec4(aPosition,1.0);
                vWorld=world.xyz;
                vNormal=normalize(mat3(uModel)*aNormal);
                gl_Position=uMvp*vec4(aPosition,1.0);
            }
            """.trimIndent()
        )
        val fragment=compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLight;
            varying vec3 vNormal;
            varying vec3 vWorld;
            void main(){
                vec3 n=normalize(vNormal);
                vec3 l=normalize(uLight);
                float diff=max(dot(n,l),0.0);
                float rim=pow(1.0-max(abs(n.z),0.0),2.0);
                vec3 c=uColor.rgb*(0.30+diff*0.72)+vec3(rim*0.10);
                gl_FragColor=vec4(c,uColor.a);
            }
            """.trimIndent()
        )
        return GLES20.glCreateProgram().also{
            GLES20.glAttachShader(it,vertex)
            GLES20.glAttachShader(it,fragment)
            GLES20.glBindAttribLocation(it,0,"aPosition")
            GLES20.glBindAttribLocation(it,1,"aNormal")
            GLES20.glLinkProgram(it)
            GLES20.glDeleteShader(vertex)
            GLES20.glDeleteShader(fragment)
        }
    }

    private fun compileShader(type:Int,source:String):Int=
        GLES20.glCreateShader(type).also{
            GLES20.glShaderSource(it,source)
            GLES20.glCompileShader(it)
        }

    private fun makeCube():RsGlMeshV183{
        val data=floatArrayOf(
            // front
            -1f,-1f,1f, 0f,0f,1f, 1f,-1f,1f, 0f,0f,1f, 1f,1f,1f, 0f,0f,1f, -1f,1f,1f, 0f,0f,1f,
            // back
            1f,-1f,-1f, 0f,0f,-1f, -1f,-1f,-1f, 0f,0f,-1f, -1f,1f,-1f, 0f,0f,-1f, 1f,1f,-1f, 0f,0f,-1f,
            // left
            -1f,-1f,-1f, -1f,0f,0f, -1f,-1f,1f, -1f,0f,0f, -1f,1f,1f, -1f,0f,0f, -1f,1f,-1f, -1f,0f,0f,
            // right
            1f,-1f,1f, 1f,0f,0f, 1f,-1f,-1f, 1f,0f,0f, 1f,1f,-1f, 1f,0f,0f, 1f,1f,1f, 1f,0f,0f,
            // top
            -1f,1f,1f, 0f,1f,0f, 1f,1f,1f, 0f,1f,0f, 1f,1f,-1f, 0f,1f,0f, -1f,1f,-1f, 0f,1f,0f,
            // bottom
            -1f,-1f,-1f, 0f,-1f,0f, 1f,-1f,-1f, 0f,-1f,0f, 1f,-1f,1f, 0f,-1f,0f, -1f,-1f,1f, 0f,-1f,0f
        )
        val idx=shortArrayOf(
            0,1,2,0,2,3, 4,5,6,4,6,7, 8,9,10,8,10,11,
            12,13,14,12,14,15, 16,17,18,16,18,19, 20,21,22,20,22,23
        )
        return mesh(data,idx)
    }

    private fun makeSphere(stacks:Int,slices:Int):RsGlMeshV183{
        val v=ArrayList<Float>()
        val idx=ArrayList<Short>()
        for(i in 0..stacks){
            val phi=PI*i/stacks
            val y=cos(phi).toFloat()
            val r=sin(phi).toFloat()
            for(j in 0..slices){
                val th=2.0*PI*j/slices
                val x=(r*cos(th)).toFloat()
                val z=(r*sin(th)).toFloat()
                v.add(x);v.add(y);v.add(z)
                v.add(x);v.add(y);v.add(z)
            }
        }
        val row=slices+1
        for(i in 0 until stacks){
            for(j in 0 until slices){
                val a=(i*row+j).toShort()
                val b=((i+1)*row+j).toShort()
                val c=(i*row+j+1).toShort()
                val d=((i+1)*row+j+1).toShort()
                idx.add(a);idx.add(b);idx.add(c)
                idx.add(c);idx.add(b);idx.add(d)
            }
        }
        return mesh(v.toFloatArray(),idx.toShortArray())
    }

    private fun makeCylinder(slices:Int):RsGlMeshV183{
        val v=ArrayList<Float>()
        val idx=ArrayList<Short>()
        for(level in 0..1){
            val y=if(level==0)-1f else 1f
            for(j in 0..slices){
                val th=2.0*PI*j/slices
                val x=cos(th).toFloat()
                val z=sin(th).toFloat()
                v.add(x);v.add(y);v.add(z)
                v.add(x);v.add(0f);v.add(z)
            }
        }
        val row=slices+1
        for(j in 0 until slices){
            val a=j.toShort()
            val b=(row+j).toShort()
            val c=(j+1).toShort()
            val d=(row+j+1).toShort()
            idx.add(a);idx.add(b);idx.add(c)
            idx.add(c);idx.add(b);idx.add(d)
        }
        return mesh(v.toFloatArray(),idx.toShortArray())
    }

    private fun mesh(v:FloatArray,i:ShortArray):RsGlMeshV183{
        val vb=ByteBuffer.allocateDirect(v.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(v).position(0)
        val ib=ByteBuffer.allocateDirect(i.size*2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(i).position(0)
        return RsGlMeshV183(vb,ib,i.size)
    }
}

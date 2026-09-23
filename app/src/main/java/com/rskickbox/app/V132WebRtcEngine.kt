package com.rskickbox.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class RsWebRtcEngineV132(
    context:Context,
    private val callId:String,
    private val isCaller:Boolean,
    private val videoEnabled:Boolean,
    private val scope:CoroutineScope,
    private val onState:(String)->Unit
){
    private val appContext=context.applicationContext
    private val eglBase:EglBase=EglBase.create()
    private val factory:PeerConnectionFactory
    private var peerConnection:PeerConnection?=null
    private var audioSource:AudioSource?=null
    private var audioTrack:AudioTrack?=null
    private var audioSender:org.webrtc.RtpSender?=null
    private var videoSource:VideoSource?=null
    private var videoTrack:VideoTrack?=null
    private var videoSender:org.webrtc.RtpSender?=null
    private var videoCapturer:VideoCapturer?=null
    private var surfaceTextureHelper:SurfaceTextureHelper?=null
    private var remoteVideoTrack:VideoTrack?=null
    private var localRenderer:SurfaceViewRenderer?=null
    private var remoteRenderer:SurfaceViewRenderer?=null
    private var signalJob:Job?=null
    private var lastSignalId:Long=0
    private var started=false
    private var disposed=false

    val eglContext:EglBase.Context get()=eglBase.eglBaseContext

    init{
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        val encoder=DefaultVideoEncoderFactory(eglContext,true,true)
        val decoder=DefaultVideoDecoderFactory(eglContext)
        factory=PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoder)
            .setVideoDecoderFactory(decoder)
            .createPeerConnectionFactory()
    }

    fun attachRenderers(local:SurfaceViewRenderer?,remote:SurfaceViewRenderer?){
        val previousLocal=localRenderer
        val previousRemote=remoteRenderer

        if(local!==previousLocal){
            previousLocal?.let{old->runCatching{videoTrack?.removeSink(old)}}
            localRenderer=local
            local?.let{renderer->
                runCatching{
                    renderer.init(eglContext,null)
                    renderer.setMirror(true)
                    renderer.setEnableHardwareScaler(true)
                    renderer.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    videoTrack?.addSink(renderer)
                }
            }
        }

        if(remote!==previousRemote){
            previousRemote?.let{old->runCatching{remoteVideoTrack?.removeSink(old)}}
            remoteRenderer=remote
            remote?.let{renderer->
                runCatching{
                    renderer.init(eglContext,null)
                    renderer.setMirror(false)
                    renderer.setEnableHardwareScaler(true)
                    renderer.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    remoteVideoTrack?.addSink(renderer)
                }
            }
        }
    }

    fun start(){
        if(started||disposed)return
        started=true
        onState("CONNECTING")

        // Prefer direct P2P through STUN. If NAT/firewall traversal fails,
        // WebRTC can fall back to TURN relay. The OpenRelay entries are for
        // preview/device testing; production can later swap to dedicated RS
        // credentials without changing the call architecture.
        val iceServers=listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )
        val rtcConfig=PeerConnection.RTCConfiguration(iceServers).apply{
            sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy=PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize=4
        }

        peerConnection=factory.createPeerConnection(rtcConfig,object:PeerConnection.Observer{
            override fun onSignalingChange(newState:PeerConnection.SignalingState?){}
            override fun onIceConnectionChange(newState:PeerConnection.IceConnectionState?){
                when(newState){
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED->onState("CONNECTED")
                    PeerConnection.IceConnectionState.DISCONNECTED->onState("RECONNECTING")
                    PeerConnection.IceConnectionState.FAILED->onState("FAILED")
                    PeerConnection.IceConnectionState.CLOSED->onState("ENDED")
                    else->{}
                }
            }
            override fun onIceConnectionReceivingChange(receiving:Boolean){}
            override fun onIceGatheringChange(newState:PeerConnection.IceGatheringState?){}
            override fun onIceCandidate(candidate:IceCandidate?){
                candidate?:return
                scope.launch{
                    rsAddCallSignalV131(
                        callId,
                        "ICE",
                        buildJsonObject{
                            put("sdpMid",candidate.sdpMid.orEmpty())
                            put("sdpMLineIndex",candidate.sdpMLineIndex)
                            put("candidate",candidate.sdp)
                        }
                    )
                }
            }
            override fun onIceCandidatesRemoved(candidates:Array<out IceCandidate>?){}
            override fun onAddStream(stream:MediaStream?){}
            override fun onRemoveStream(stream:MediaStream?){}
            override fun onDataChannel(channel:DataChannel?){}
            override fun onRenegotiationNeeded(){}
            override fun onAddTrack(receiver:RtpReceiver?,mediaStreams:Array<out MediaStream>?){
                if(disposed)return
                val track=receiver?.track()
                if(track is VideoTrack){
                    remoteVideoTrack=track
                    remoteRenderer?.let{renderer->runCatching{track.addSink(renderer)}}
                }
            }
            override fun onTrack(transceiver:RtpTransceiver?){
                if(disposed)return
                val track=transceiver?.receiver?.track()
                if(track is VideoTrack){
                    remoteVideoTrack=track
                    remoteRenderer?.let{renderer->runCatching{track.addSink(renderer)}}
                }
            }
        })

        val pc=peerConnection ?: run{
            onState("FAILED")
            return
        }

        val audioConstraints=MediaConstraints().apply{
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation","true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression","true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter","true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl","true"))
            optional.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection","true"))
        }
        audioSource=factory.createAudioSource(audioConstraints)
        audioTrack=factory.createAudioTrack("RS_AUDIO",audioSource).also{
            it.setEnabled(true)
            audioSender=pc.addTrack(it)
        }
        tuneAudioSender(audioSender)

        if(videoEnabled){
            val capturer=createCameraCapturer()
            if(capturer!=null){
                videoCapturer=capturer
                surfaceTextureHelper=SurfaceTextureHelper.create("RSVideoCapture",eglContext)
                videoSource=factory.createVideoSource(false)
                capturer.initialize(surfaceTextureHelper,appContext,videoSource!!.capturerObserver)
                runCatching{capturer.startCapture(720,1280,30)}
                runCatching{videoSource?.adaptOutputFormat(720,1280,30)}
                videoTrack=factory.createVideoTrack("RS_VIDEO",videoSource).also{
                    it.setEnabled(true)
                    localRenderer?.let(it::addSink)
                    videoSender=pc.addTrack(it)
                }
                tuneVideoSender(videoSender)
            }
        }

        signalJob=scope.launch{
            while(isActive&&!disposed){
                rsCallSignalsSinceV131(callId,lastSignalId)
                    .onSuccess{signals->
                        for(signal in signals){
                            lastSignalId=maxOf(lastSignalId,signal.id)
                            handleSignal(signal)
                        }
                    }
                delay(250)
            }
        }

        if(isCaller)createOffer()
    }

    private fun tuneAudioSender(sender:org.webrtc.RtpSender?){
        sender?:return
        runCatching{
            val p=sender.parameters
            p.encodings.forEach{encoding->
                encoding.maxBitrateBps=64_000
            }
            sender.parameters=p
        }
    }

    private fun tuneVideoSender(sender:org.webrtc.RtpSender?){
        sender?:return
        runCatching{
            val p=sender.parameters
            p.degradationPreference=org.webrtc.RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
            p.encodings.forEach{encoding->
                encoding.maxBitrateBps=1_800_000
                encoding.maxFramerate=30
            }
            sender.parameters=p
        }
    }

    private fun createCameraCapturer():VideoCapturer?{
        val enumerator=Camera2Enumerator(appContext)
        val names=enumerator.deviceNames
        names.firstOrNull{enumerator.isFrontFacing(it)}?.let{name->
            enumerator.createCapturer(name,null)?.let{return it}
        }
        names.firstOrNull()?.let{name->
            return enumerator.createCapturer(name,null)
        }
        return null
    }

    private fun createOffer(){
        val pc=peerConnection?:return
        pc.createOffer(object:SdpObserver{
            override fun onCreateSuccess(desc:SessionDescription?){
                desc?:return
                pc.setLocalDescription(simpleSdpObserver(),desc)
                scope.launch{
                    rsAddCallSignalV131(
                        callId,"OFFER",
                        buildJsonObject{put("sdp",desc.description)}
                    )
                }
            }
            override fun onSetSuccess(){}
            override fun onCreateFailure(error:String?){onState("FAILED")}
            override fun onSetFailure(error:String?){onState("FAILED")}
        },MediaConstraints())
    }

    private fun createAnswer(){
        val pc=peerConnection?:return
        pc.createAnswer(object:SdpObserver{
            override fun onCreateSuccess(desc:SessionDescription?){
                desc?:return
                pc.setLocalDescription(simpleSdpObserver(),desc)
                scope.launch{
                    rsAddCallSignalV131(
                        callId,"ANSWER",
                        buildJsonObject{put("sdp",desc.description)}
                    )
                }
            }
            override fun onSetSuccess(){}
            override fun onCreateFailure(error:String?){onState("FAILED")}
            override fun onSetFailure(error:String?){onState("FAILED")}
        },MediaConstraints())
    }

    private fun handleSignal(signal:RsCallSignalV131){
        val pc=peerConnection?:return
        when(signal.signalKind.uppercase()){
            "OFFER"->{
                if(isCaller)return
                val sdp=signal.payload.jsonObject["sdp"]?.jsonPrimitive?.content?:return
                pc.setRemoteDescription(object:SdpObserver{
                    override fun onCreateSuccess(desc:SessionDescription?){}
                    override fun onSetSuccess(){createAnswer()}
                    override fun onCreateFailure(error:String?){onState("FAILED")}
                    override fun onSetFailure(error:String?){onState("FAILED")}
                },SessionDescription(SessionDescription.Type.OFFER,sdp))
            }
            "ANSWER"->{
                if(!isCaller)return
                val sdp=signal.payload.jsonObject["sdp"]?.jsonPrimitive?.content?:return
                pc.setRemoteDescription(simpleSdpObserver(),SessionDescription(SessionDescription.Type.ANSWER,sdp))
            }
            "ICE"->{
                val obj=signal.payload.jsonObject
                val mid=obj["sdpMid"]?.jsonPrimitive?.content.orEmpty()
                val line=obj["sdpMLineIndex"]?.jsonPrimitive?.content?.toIntOrNull()?:0
                val candidate=obj["candidate"]?.jsonPrimitive?.content?:return
                pc.addIceCandidate(IceCandidate(mid,line,candidate))
            }
        }
    }

    private fun simpleSdpObserver()=object:SdpObserver{
        override fun onCreateSuccess(desc:SessionDescription?){}
        override fun onSetSuccess(){}
        override fun onCreateFailure(error:String?){onState("FAILED")}
        override fun onSetFailure(error:String?){onState("FAILED")}
    }

    fun detachRenderers(local:SurfaceViewRenderer?,remote:SurfaceViewRenderer?){
        local?.let{renderer->
            runCatching{videoTrack?.removeSink(renderer)}
            if(localRenderer===renderer)localRenderer=null
        }
        remote?.let{renderer->
            runCatching{remoteVideoTrack?.removeSink(renderer)}
            if(remoteRenderer===renderer)remoteRenderer=null
        }
    }

    fun setMicEnabled(enabled:Boolean){
        if(disposed)return
        runCatching{audioTrack?.setEnabled(enabled)}
    }

    fun setCameraEnabled(enabled:Boolean){
        if(disposed)return
        runCatching{videoTrack?.setEnabled(enabled)}
    }

    fun switchCamera(){
        if(disposed)return
        runCatching{(videoCapturer as? CameraVideoCapturer)?.switchCamera(null)}
    }

    fun dispose(){
        if(disposed)return
        disposed=true
        signalJob?.cancel()
        signalJob=null

        val local=localRenderer
        val remote=remoteRenderer
        val localTrack=videoTrack
        val remoteTrack=remoteVideoTrack
        localRenderer=null
        remoteRenderer=null
        runCatching{local?.let{localTrack?.removeSink(it)}}
        runCatching{remote?.let{remoteTrack?.removeSink(it)}}

        runCatching{videoCapturer?.stopCapture()}
        runCatching{videoCapturer?.dispose()}
        videoCapturer=null
        runCatching{surfaceTextureHelper?.dispose()}
        surfaceTextureHelper=null
        remoteVideoTrack=null
        videoSender=null
        runCatching{videoTrack?.dispose()}
        videoTrack=null
        runCatching{videoSource?.dispose()}
        videoSource=null
        audioSender=null
        runCatching{audioTrack?.dispose()}
        audioTrack=null
        runCatching{audioSource?.dispose()}
        audioSource=null
        runCatching{peerConnection?.close()}
        runCatching{peerConnection?.dispose()}
        peerConnection=null

        // Compose owns SurfaceViewRenderer.release(). Give AndroidView teardown
        // a short head start before releasing the shared native EGL/factory.
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching{factory.dispose()}
            runCatching{eglBase.release()}
        },500L)
    }
}

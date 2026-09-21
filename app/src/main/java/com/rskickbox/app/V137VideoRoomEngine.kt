package com.rskickbox.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.webrtc.*

class RsVideoRoomEngineV137(
    context:Context,
    private val roomId:String,
    private val myId:String,
    private val isHost:Boolean,
    private val scope:CoroutineScope,
    private val onPeerState:(String,String)->Unit
){
    private val appContext=context.applicationContext
    private val eglBase=EglBase.create()
    private val factory:PeerConnectionFactory
    private var audioSource:AudioSource?=null
    private var audioTrack:AudioTrack?=null
    private val audioSenders=mutableMapOf<String,RtpSender>()
    private var videoSource:VideoSource?=null
    private var videoTrack:VideoTrack?=null
    private val videoSenders=mutableMapOf<String,RtpSender>()
    private var videoCapturer:VideoCapturer?=null
    private var surfaceTextureHelper:SurfaceTextureHelper?=null
    private val peers=mutableMapOf<String,PeerConnection>()
    private val remoteTracks=mutableMapOf<String,VideoTrack>()
    private val remoteSinks=mutableMapOf<String,MutableSet<VideoSink>>()
    private var localSink:VideoSink?=null
    private var signalJob:Job?=null
    private var lastSignalId=0L
    private var started=false
    private var disposed=false

    val eglContext:EglBase.Context get()=eglBase.eglBaseContext

    init{
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory=PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext,true,true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .createPeerConnectionFactory()
    }

    fun start(){
        if(started||disposed)return
        started=true

        val audioConstraints=MediaConstraints().apply{
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation","true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression","true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter","true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl","true"))
            optional.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection","true"))
        }
        audioSource=factory.createAudioSource(audioConstraints)
        audioTrack=factory.createAudioTrack("RS_ROOM_AUDIO",audioSource).apply{setEnabled(true)}

        val capturer=createCameraCapturer()
        if(capturer!=null){
            videoCapturer=capturer
            surfaceTextureHelper=SurfaceTextureHelper.create("RSRoomCapture",eglContext)
            videoSource=factory.createVideoSource(false)
            capturer.initialize(surfaceTextureHelper,appContext,videoSource!!.capturerObserver)
            runCatching{capturer.startCapture(720,1280,30)}
            runCatching{videoSource?.adaptOutputFormat(720,1280,30)}
            videoTrack=factory.createVideoTrack("RS_ROOM_VIDEO",videoSource).apply{setEnabled(true)}
            localSink?.let{videoTrack?.addSink(it)}
        }

        signalJob=scope.launch{
            while(isActive&&!disposed){
                rsVideoRoomSignalsSinceV136(roomId,lastSignalId)
                    .onSuccess{signals->
                        for(signal in signals){
                            lastSignalId=maxOf(lastSignalId,signal.id)
                            if(signal.senderId!=myId)handleSignal(signal)
                        }
                    }
                delay(250)
            }
        }
    }

    fun syncMembers(members:List<RsVideoRoomMemberV136>){
        if(!started||disposed)return
        if(isHost){
            members.filter{
                it.memberRole=="STUDENT" &&
                it.memberStatus=="JOINED" &&
                it.userId!=myId
            }.forEach{member->
                ensurePeer(member.userId,true)
            }
        }else{
            members.firstOrNull{
                it.memberRole=="HOST" &&
                it.memberStatus=="JOINED" &&
                it.userId!=myId
            }?.let{host->
                ensurePeer(host.userId,false)
            }
        }

        val valid=members.filter{it.memberStatus=="JOINED"}.map{it.userId}.toSet()
        peers.keys.filter{it !in valid}.toList().forEach(::removePeer)
    }

    fun attachLocalRenderer(renderer:SurfaceViewRenderer){
        localSink?.let{old->videoTrack?.removeSink(old)}
        localSink=renderer
        videoTrack?.addSink(renderer)
    }

    fun detachLocalRenderer(renderer:SurfaceViewRenderer){
        videoTrack?.removeSink(renderer)
        if(localSink===renderer)localSink=null
    }

    fun attachRemoteRenderer(peerId:String,renderer:SurfaceViewRenderer){
        remoteSinks.getOrPut(peerId){linkedSetOf()}.add(renderer)
        remoteTracks[peerId]?.addSink(renderer)
    }

    fun detachRemoteRenderer(peerId:String,renderer:SurfaceViewRenderer){
        remoteTracks[peerId]?.removeSink(renderer)
        remoteSinks[peerId]?.remove(renderer)
    }

    fun setMicEnabled(enabled:Boolean){audioTrack?.setEnabled(enabled)}
    fun setCameraEnabled(enabled:Boolean){videoTrack?.setEnabled(enabled)}
    fun switchCamera(){(videoCapturer as? CameraVideoCapturer)?.switchCamera(null)}

    private fun tuneAudioSender(sender:RtpSender?){
        sender?:return
        runCatching{
            val p=sender.parameters
            p.encodings.forEach{it.maxBitrateBps=64_000}
            sender.parameters=p
        }
    }

    private fun tuneVideoSender(sender:RtpSender?){
        sender?:return
        runCatching{
            val p=sender.parameters
            p.degradationPreference=RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
            p.encodings.forEach{encoding->
                encoding.maxBitrateBps=1_500_000
                encoding.maxFramerate=30
            }
            sender.parameters=p
        }
    }

    private fun createCameraCapturer():VideoCapturer?{
        val e=Camera2Enumerator(appContext)
        val names=e.deviceNames
        names.firstOrNull{e.isFrontFacing(it)}?.let{name->
            e.createCapturer(name,null)?.let{return it}
        }
        names.firstOrNull()?.let{name->return e.createCapturer(name,null)}
        return null
    }

    private fun iceServers()=listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
            .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
    )

    private fun ensurePeer(peerId:String,initiator:Boolean):PeerConnection?{
        peers[peerId]?.let{return it}
        val config=PeerConnection.RTCConfiguration(iceServers()).apply{
            sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy=PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize=4
        }
        val pc=factory.createPeerConnection(config,object:PeerConnection.Observer{
            override fun onSignalingChange(newState:PeerConnection.SignalingState?){}
            override fun onIceConnectionChange(newState:PeerConnection.IceConnectionState?){
                val state=when(newState){
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED->"CONNECTED"
                    PeerConnection.IceConnectionState.DISCONNECTED->"RECONNECTING"
                    PeerConnection.IceConnectionState.FAILED->"FAILED"
                    PeerConnection.IceConnectionState.CLOSED->"ENDED"
                    else->"CONNECTING"
                }
                onPeerState(peerId,state)
            }
            override fun onIceConnectionReceivingChange(receiving:Boolean){}
            override fun onIceGatheringChange(newState:PeerConnection.IceGatheringState?){}
            override fun onIceCandidate(candidate:IceCandidate?){
                candidate?:return
                scope.launch{
                    rsAddVideoRoomSignalV136(
                        roomId,peerId,"ICE",
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
                handleRemoteTrack(peerId,receiver?.track())
            }
            override fun onTrack(transceiver:RtpTransceiver?){
                handleRemoteTrack(peerId,transceiver?.receiver?.track())
            }
        })?:return null

        peers[peerId]=pc
        audioTrack?.let{track->
            pc.addTrack(track)?.let{sender->
                audioSenders[peerId]=sender
                tuneAudioSender(sender)
            }
        }
        videoTrack?.let{track->
            pc.addTrack(track)?.let{sender->
                videoSenders[peerId]=sender
                tuneVideoSender(sender)
            }
        }

        if(initiator)createOffer(peerId,pc)
        return pc
    }

    private fun handleRemoteTrack(peerId:String,track:MediaStreamTrack?){
        if(track is VideoTrack){
            remoteTracks[peerId]?.let{old->
                remoteSinks[peerId]?.forEach{old.removeSink(it)}
            }
            remoteTracks[peerId]=track
            remoteSinks[peerId]?.forEach{track.addSink(it)}
        }
    }

    private fun createOffer(peerId:String,pc:PeerConnection){
        pc.createOffer(object:SdpObserver{
            override fun onCreateSuccess(desc:SessionDescription?){
                desc?:return
                pc.setLocalDescription(simpleObserver(peerId),desc)
                scope.launch{
                    rsAddVideoRoomSignalV136(
                        roomId,peerId,"OFFER",
                        buildJsonObject{put("sdp",desc.description)}
                    )
                }
            }
            override fun onSetSuccess(){}
            override fun onCreateFailure(error:String?){onPeerState(peerId,"FAILED")}
            override fun onSetFailure(error:String?){onPeerState(peerId,"FAILED")}
        },MediaConstraints())
    }

    private fun createAnswer(peerId:String,pc:PeerConnection){
        pc.createAnswer(object:SdpObserver{
            override fun onCreateSuccess(desc:SessionDescription?){
                desc?:return
                pc.setLocalDescription(simpleObserver(peerId),desc)
                scope.launch{
                    rsAddVideoRoomSignalV136(
                        roomId,peerId,"ANSWER",
                        buildJsonObject{put("sdp",desc.description)}
                    )
                }
            }
            override fun onSetSuccess(){}
            override fun onCreateFailure(error:String?){onPeerState(peerId,"FAILED")}
            override fun onSetFailure(error:String?){onPeerState(peerId,"FAILED")}
        },MediaConstraints())
    }

    private fun handleSignal(signal:RsVideoRoomSignalV136){
        val peerId=signal.senderId
        when(signal.signalKind.uppercase()){
            "OFFER"->{
                val pc=ensurePeer(peerId,false)?:return
                val sdp=signal.payload.jsonObject["sdp"]?.jsonPrimitive?.content?:return
                pc.setRemoteDescription(object:SdpObserver{
                    override fun onCreateSuccess(desc:SessionDescription?){}
                    override fun onSetSuccess(){createAnswer(peerId,pc)}
                    override fun onCreateFailure(error:String?){onPeerState(peerId,"FAILED")}
                    override fun onSetFailure(error:String?){onPeerState(peerId,"FAILED")}
                },SessionDescription(SessionDescription.Type.OFFER,sdp))
            }
            "ANSWER"->{
                val pc=peers[peerId]?:return
                val sdp=signal.payload.jsonObject["sdp"]?.jsonPrimitive?.content?:return
                pc.setRemoteDescription(simpleObserver(peerId),SessionDescription(SessionDescription.Type.ANSWER,sdp))
            }
            "ICE"->{
                val pc=ensurePeer(peerId,false)?:return
                val obj=signal.payload.jsonObject
                val mid=obj["sdpMid"]?.jsonPrimitive?.content.orEmpty()
                val line=obj["sdpMLineIndex"]?.jsonPrimitive?.content?.toIntOrNull()?:0
                val candidate=obj["candidate"]?.jsonPrimitive?.content?:return
                pc.addIceCandidate(IceCandidate(mid,line,candidate))
            }
        }
    }

    private fun simpleObserver(peerId:String)=object:SdpObserver{
        override fun onCreateSuccess(desc:SessionDescription?){}
        override fun onSetSuccess(){}
        override fun onCreateFailure(error:String?){onPeerState(peerId,"FAILED")}
        override fun onSetFailure(error:String?){onPeerState(peerId,"FAILED")}
    }

    private fun removePeer(peerId:String){
        remoteTracks.remove(peerId)?.let{track->
            remoteSinks[peerId]?.forEach{track.removeSink(it)}
        }
        peers.remove(peerId)?.let{pc->
            pc.close()
            pc.dispose()
        }
        audioSenders.remove(peerId)
        videoSenders.remove(peerId)
        remoteSinks.remove(peerId)
        onPeerState(peerId,"ENDED")
    }

    fun dispose(){
        if(disposed)return
        disposed=true
        signalJob?.cancel()
        signalJob=null
        peers.keys.toList().forEach(::removePeer)
        runCatching{videoCapturer?.stopCapture()}
        runCatching{videoCapturer?.dispose()}
        videoCapturer=null
        runCatching{surfaceTextureHelper?.dispose()}
        surfaceTextureHelper=null
        localSink?.let{videoTrack?.removeSink(it)}
        localSink=null
        videoTrack?.dispose()
        videoTrack=null
        videoSource?.dispose()
        videoSource=null
        audioTrack?.dispose()
        audioTrack=null
        audioSource?.dispose()
        audioSource=null
        factory.dispose()
        eglBase.release()
    }
}

package com.rskickbox.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken

class RsMusicPlaybackServiceV90 : MediaSessionService() {
    private var mediaSession:MediaSession?=null

    override fun onCreate(){
        super.onCreate()
        val player=ExoPlayer.Builder(this).build().apply{
            repeatMode=Player.REPEAT_MODE_ALL
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        mediaSession=MediaSession.Builder(this,player).build()
    }

    override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaSession?=mediaSession

    override fun onDestroy(){
        mediaSession?.run{
            player.release()
            release()
        }
        mediaSession=null
        super.onDestroy()
    }
}

data class RsPersistentTrackV90(val uri:String,val name:String)

@Composable
fun rememberRsMusicControllerV90():MediaController?{
    val context=LocalContext.current
    var controller by remember{mutableStateOf<MediaController?>(null)}

    DisposableEffect(Unit){
        val token=SessionToken(context,ComponentName(context,RsMusicPlaybackServiceV90::class.java))
        val future=MediaController.Builder(context,token).buildAsync()
        future.addListener({
            controller=runCatching{future.get()}.getOrNull()
        },ContextCompat.getMainExecutor(context))

        onDispose{
            controller?.release()
            controller=null
            if(!future.isDone)future.cancel(true)
        }
    }
    return controller
}

private fun rsPersistentTracksV90(store:RsStore):List<RsPersistentTrackV90> =
    store.s("local_music_tracks","")
        .split("§")
        .filter{it.contains("¤")}
        .mapNotNull{row->
            val p=row.split("¤",limit=2)
            if(p.size==2)RsPersistentTrackV90(p[0],p[1]) else null
        }

private fun rsSavePersistentTracksV90(store:RsStore,tracks:List<RsPersistentTrackV90>)=
    store.ps("local_music_tracks",tracks.joinToString("§"){"${it.uri}¤${it.name}"})

private fun rsTrackDisplayNameV90(context:Context,uri:Uri,fallback:String):String{
    var name=fallback
    runCatching{
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cur->
            if(cur.moveToFirst())name=cur.getString(0)?:name
        }
    }
    return name
}

private fun rsMediaItemsV90(tracks:List<RsPersistentTrackV90>)=tracks.map{track->
    MediaItem.Builder()
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.name)
                .setArtist("RS KICKBOX")
                .build()
        )
        .build()
}

@Composable
fun RsPersistentMusicCenterV90(c:RsPalette,store:RsStore,role:RsRole,lang:RsLang){
    val context=LocalContext.current
    val controller=rememberRsMusicControllerV90()
    var tracks by remember{mutableStateOf(rsPersistentTracksV90(store))}
    var feedback by remember{mutableStateOf("")}
    var revision by remember{mutableIntStateOf(0)}

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()){
            val next=tracks.toMutableList()
            uris.forEachIndexed{i,uri->
                runCatching{
                    context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val track=RsPersistentTrackV90(
                    uri.toString(),
                    rsTrackDisplayNameV90(context,uri,rsMusicBgT90(lang,"track")+" "+(tracks.size+i+1))
                )
                if(next.none{it.uri==track.uri})next.add(track)
            }
            tracks=next
            rsSavePersistentTracksV90(store,next)
            feedback=uris.size.toString()+" "+rsMusicBgT90(lang,"added")
            revision++
        }
    }

    var playing by remember{mutableStateOf(controller?.isPlaying==true)}
    var index by remember{mutableIntStateOf(controller?.currentMediaItemIndex?.coerceAtLeast(0)?:0)}
    var position by remember{mutableLongStateOf(controller?.currentPosition?:0L)}
    var duration by remember{mutableLongStateOf(controller?.duration?.coerceAtLeast(0L)?:0L)}

    DisposableEffect(controller,revision){
        if(controller==null)return@DisposableEffect onDispose{}
        val listener=object:Player.Listener{
            override fun onIsPlayingChanged(isPlaying:Boolean){playing=isPlaying}
            override fun onMediaItemTransition(mediaItem:MediaItem?,reason:Int){
                index=controller.currentMediaItemIndex.coerceAtLeast(0)
            }
            override fun onPlaybackStateChanged(playbackState:Int){
                duration=controller.duration.coerceAtLeast(0L)
            }
        }
        controller.addListener(listener)
        onDispose{controller.removeListener(listener)}
    }

    LaunchedEffect(controller,playing,index){
        while(controller!=null){
            position=controller.currentPosition.coerceAtLeast(0L)
            duration=controller.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(500)
        }
    }

    fun playTrack(i:Int){
        val p=controller?:return
        if(tracks.isEmpty())return
        p.setMediaItems(rsMediaItemsV90(tracks),i.coerceIn(0,tracks.lastIndex),0L)
        p.prepare()
        p.play()
    }

    RsScroll(
        c,
        if(role==RsRole.TRAINER)rsMusicT(lang,"trainer_title") else rsMusicT(lang,"student_title"),
        rsMusicBgT90(lang,"subtitle")
    ){
        RsPanel(c){
            Text(rsMusicBgT90(lang,"background_player"),color=c.bright,fontWeight=FontWeight.Black)
            Text(
                rsMusicBgT90(lang,"background_desc"),
                color=c.muted,
                fontSize=10.sp
            )
            Button(onClick={picker.launch(arrayOf("audio/*"))},modifier=Modifier.fillMaxWidth()){
                Text(rsMusicT(lang,"add_button"))
            }
            if(feedback.isNotBlank())Text(feedback,color=c.muted,fontSize=10.sp)
        }

        if(tracks.isEmpty()){
            RsPanel(c){
                Text(rsMusicBgT90(lang,"none"),color=c.bright,fontWeight=FontWeight.Bold)
                Text(rsMusicBgT90(lang,"none_desc"),color=c.muted)
            }
        }else{
            val safeIndex=index.coerceIn(0,tracks.lastIndex)
            val current=tracks[safeIndex]
            RsPanel(c){
                Text(rsMusicBgT90(lang,"now_playing"),color=c.muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Text(current.name,color=c.bright,fontWeight=FontWeight.Black,fontSize=20.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                Slider(
                    value=if(duration>0)position.toFloat()/duration else 0f,
                    onValueChange={fraction->
                        controller?.seekTo((duration*fraction).toLong())
                    },
                    modifier=Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                    FilledTonalButton(onClick={controller?.seekToPreviousMediaItem()}){Text("⏮")}
                    Button(onClick={
                        val p=controller?:return@Button
                        if(p.mediaItemCount==0)playTrack(safeIndex)
                        else if(p.isPlaying)p.pause() else p.play()
                    }){Text(if(playing)"⏸ "+rsMusicT(lang,"pause") else "▶ "+rsMusicT(lang,"play"))}
                    FilledTonalButton(onClick={controller?.seekToNextMediaItem()}){Text("⏭")}
                }
                OutlinedButton(
                    onClick={
                        controller?.stop()
                        controller?.clearMediaItems()
                    },
                    modifier=Modifier.fillMaxWidth()
                ){Text(rsMusicBgT90(lang,"stop_close"))}
            }

            Text(rsMusicT(lang,"playlist"),color=c.bright,fontWeight=FontWeight.Black)
            tracks.forEachIndexed{i,track->
                RsPanel(c){
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        TextButton(onClick={playTrack(i)},modifier=Modifier.weight(1f)){
                            Text(
                                if(i==safeIndex && controller?.mediaItemCount?:0>0)"▶ "+track.name else track.name,
                                color=if(i==safeIndex)c.bright else c.text,
                                maxLines=1,
                                overflow=TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick={
                            val next=tracks.toMutableList().also{it.removeAt(i)}
                            if(controller?.mediaItemCount?:0>0){
                                controller?.removeMediaItem(i)
                            }
                            tracks=next
                            rsSavePersistentTracksV90(store,next)
                            revision++
                        }){Text(rsMusicT(lang,"remove"))}
                    }
                }
            }
        }
    }
}

@Composable
fun RsMiniMusicPlayerV90(
    c:RsPalette,
    lang:RsLang,
    onOpenMusic:()->Unit
){
    val controller=rememberRsMusicControllerV90()
    var title by remember{mutableStateOf("")}
    var playing by remember{mutableStateOf(false)}
    var hasMedia by remember{mutableStateOf(false)}

    fun refresh(){
        val p=controller
        if(p==null){
            title=""
            playing=false
            hasMedia=false
        }else{
            hasMedia=p.mediaItemCount>0
            title=p.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            playing=p.isPlaying
        }
    }

    DisposableEffect(controller){
        if(controller==null)return@DisposableEffect onDispose{}
        refresh()
        val listener=object:Player.Listener{
            override fun onEvents(player:Player,events:Player.Events){refresh()}
        }
        controller.addListener(listener)
        onDispose{controller.removeListener(listener)}
    }

    if(!hasMedia)return

    Surface(
        shape=RoundedCornerShape(16.dp),
        color=c.panel2,
        tonalElevation=3.dp,
        modifier=Modifier.fillMaxWidth()
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)
        ){
            Text("♫",color=c.bright,fontWeight=FontWeight.Black,fontSize=18.sp)
            Text(
                title.ifBlank{"RS Music"},
                color=c.text,
                maxLines=1,
                overflow=TextOverflow.Ellipsis,
                modifier=Modifier.weight(1f)
            )
            TextButton(onClick={
                val p=controller?:return@TextButton
                if(p.isPlaying)p.pause() else p.play()
            }){Text(if(playing)"⏸" else "▶")}
            TextButton(onClick=onOpenMusic){Text(rsMusicBgT90(lang,"open"),fontSize=9.sp)}
            TextButton(onClick={
                controller?.stop()
                controller?.clearMediaItems()
            }){Text("✕")}
        }
    }
}


fun rsMusicBgT90(lang:RsLang,key:String):String{
    val en=mapOf(
        "track" to "Training track","added" to "audio file(s) added.",
        "subtitle" to "Music continues across the app and outside it until you stop playback.",
        "background_player" to "BACKGROUND PLAYER",
        "background_desc" to "Playback stays active while you open other RS KICKBOX pages or minimize the app. Android media controls appear in the notification area.",
        "none" to "No music added yet.","none_desc" to "Add audio from the device to create your RS training playlist.",
        "now_playing" to "NOW PLAYING","stop_close" to "Stop & close player","open" to "OPEN"
    )
    val nl=en+mapOf("track" to "Trainingstrack","added" to "audiobestand(en) toegevoegd.","subtitle" to "Muziek blijft spelen in de app en daarbuiten totdat je stopt.","background_player" to "ACHTERGRONDSPELER","background_desc" to "Muziek blijft spelen terwijl je andere RS KICKBOX-pagina's opent of de app minimaliseert. Android-mediabediening verschijnt in de meldingsbalk.","none" to "Nog geen muziek toegevoegd.","none_desc" to "Voeg audio van je apparaat toe om je RS-trainingsplaylist te maken.","now_playing" to "NU AAN HET SPELEN","stop_close" to "Stoppen & speler sluiten","open" to "OPENEN")
    val pt=en+mapOf("track" to "Faixa de treino","added" to "ficheiro(s) de áudio adicionado(s).","subtitle" to "A música continua dentro e fora da app até parares.","background_player" to "LEITOR EM SEGUNDO PLANO","background_desc" to "A música continua enquanto abres outras páginas RS KICKBOX ou minimizas a app. Os controlos aparecem na barra de notificações Android.","none" to "Ainda não adicionaste música.","none_desc" to "Adiciona áudio do dispositivo para criar a tua playlist RS.","now_playing" to "A TOCAR AGORA","stop_close" to "Parar & fechar leitor","open" to "ABRIR")
    val es=en+mapOf("track" to "Pista de entrenamiento","added" to "archivo(s) de audio añadido(s).","subtitle" to "La música sigue sonando dentro y fuera de la app hasta que la detengas.","background_player" to "REPRODUCTOR EN SEGUNDO PLANO","background_desc" to "La música sigue mientras abres otras páginas de RS KICKBOX o minimizas la app. Los controles aparecen en la barra de notificaciones de Android.","none" to "Aún no hay música añadida.","none_desc" to "Añade audio del dispositivo para crear tu playlist RS.","now_playing" to "REPRODUCIENDO","stop_close" to "Detener y cerrar reproductor","open" to "ABRIR")
    val fr=en+mapOf("track" to "Piste d'entraînement","added" to "fichier(s) audio ajouté(s).","subtitle" to "La musique continue dans l'app et en arrière-plan jusqu'à l'arrêt.","background_player" to "LECTEUR EN ARRIÈRE-PLAN","background_desc" to "La musique continue quand tu ouvres d'autres pages RS KICKBOX ou réduis l'app. Les contrôles Android apparaissent dans les notifications.","none" to "Aucune musique ajoutée.","none_desc" to "Ajoute de l'audio depuis l'appareil pour créer ta playlist RS.","now_playing" to "LECTURE EN COURS","stop_close" to "Arrêter & fermer le lecteur","open" to "OUVRIR")
    val de=en+mapOf("track" to "Trainingstitel","added" to "Audiodatei(en) hinzugefügt.","subtitle" to "Musik läuft in der App und im Hintergrund weiter, bis du sie stoppst.","background_player" to "HINTERGRUND-PLAYER","background_desc" to "Musik läuft weiter, wenn du andere RS KICKBOX-Seiten öffnest oder die App minimierst. Android-Mediensteuerung erscheint in den Benachrichtigungen.","none" to "Noch keine Musik hinzugefügt.","none_desc" to "Füge Audio vom Gerät hinzu, um deine RS-Playlist zu erstellen.","now_playing" to "JETZT LÄUFT","stop_close" to "Stoppen & Player schließen","open" to "ÖFFNEN")
    val it=en+mapOf("track" to "Brano allenamento","added" to "file audio aggiunto/i.","subtitle" to "La musica continua dentro e fuori dall'app finché non la fermi.","background_player" to "PLAYER IN BACKGROUND","background_desc" to "La musica continua mentre apri altre pagine RS KICKBOX o riduci l'app. I controlli Android appaiono nelle notifiche.","none" to "Nessuna musica aggiunta.","none_desc" to "Aggiungi audio dal dispositivo per creare la playlist RS.","now_playing" to "IN RIPRODUZIONE","stop_close" to "Ferma & chiudi player","open" to "APRI")
    val pl=en+mapOf("track" to "Utwór treningowy","added" to "plik(i) audio dodano.","subtitle" to "Muzyka gra w aplikacji i w tle, dopóki jej nie zatrzymasz.","background_player" to "ODTWARZACZ W TLE","background_desc" to "Muzyka gra dalej podczas otwierania innych stron RS KICKBOX lub minimalizacji aplikacji. Sterowanie Android pojawia się w powiadomieniach.","none" to "Nie dodano jeszcze muzyki.","none_desc" to "Dodaj audio z urządzenia, aby utworzyć playlistę RS.","now_playing" to "TERAZ ODTWARZANE","stop_close" to "Zatrzymaj & zamknij odtwarzacz","open" to "OTWÓRZ")
    val tr=en+mapOf("track" to "Antrenman parçası","added" to "ses dosyası eklendi.","subtitle" to "Müzik siz durdurana kadar uygulama içinde ve dışında çalmaya devam eder.","background_player" to "ARKA PLAN OYNATICI","background_desc" to "Diğer RS KICKBOX sayfalarını açarken veya uygulamayı küçültürken müzik devam eder. Android medya kontrolleri bildirimlerde görünür.","none" to "Henüz müzik eklenmedi.","none_desc" to "RS antrenman çalma listesini oluşturmak için cihazdan ses ekle.","now_playing" to "ŞİMDİ ÇALIYOR","stop_close" to "Durdur & oynatıcıyı kapat","open" to "AÇ")
    val pack=when(lang.code){"nl"->nl;"pt"->pt;"es"->es;"fr"->fr;"de"->de;"it"->it;"pl"->pl;"tr"->tr;else->en}
    return pack[key]?:en[key]?:key
}

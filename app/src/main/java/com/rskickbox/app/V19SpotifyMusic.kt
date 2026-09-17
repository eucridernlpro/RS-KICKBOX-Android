package com.rskickbox.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RsSpotifyMusicCenterV19(c: RsPalette, store: RsStore, role: RsRole) {
    val context = LocalContext.current
    var clientId by remember { mutableStateOf(store.s("spotify_client_id", "")) }
    var redirectUri by remember { mutableStateOf(store.s("spotify_redirect_uri", "rskickbox://spotify-callback")) }
    var connected by remember { mutableStateOf(store.b("spotify_connected_preview", false)) }
    var playlistLink by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var defaultPlaylist by remember { mutableStateOf(store.s("spotify_default_playlist", "Warm-up Energy")) }
    val saved = remember { mutableStateListOf<String>().apply {
        val raw = store.s("spotify_saved_playlists", "Warm-up Energy|Pads Intensity|Cooldown Flow")
        addAll(raw.split("|").filter { it.isNotBlank() })
    } }

    fun persistSaved() = store.ps("spotify_saved_playlists", saved.joinToString("|"))
    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { feedback = "Could not open Spotify on this device." }
    }

    RsScroll(
        c,
        if (role == RsRole.TRAINER) "Music & Spotify Manager" else "My Music & Spotify",
        if (role == RsRole.TRAINER)
            "Connect Spotify, prepare class playlists and choose music for warm-up, pads, sparring and cooldown."
        else
            "Connect Spotify and keep your favorite training playlists available inside RS KICKBOX."
    ) {
        RsPanel(c) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SPOTIFY CONNECTION", color = c.bright, fontWeight = FontWeight.Bold)
                    Text(if (connected) "● Connected preview" else "○ Not connected", color = if (connected) c.bright else c.muted)
                }
                Text("Spotify", color = c.bright, fontWeight = FontWeight.Black)
            }
            Text("Production connection uses Spotify OAuth Authorization Code + PKCE. No Spotify client secret will be stored in the Android app.", color = c.muted)
            Button(
                onClick = {
                    if (clientId.isBlank()) {
                        feedback = "Spotify Client ID is not configured yet. Trainer/Admin can add it below after creating the Spotify developer app."
                    } else {
                        val scopes = "playlist-read-private playlist-read-collaborative playlist-modify-private playlist-modify-public user-read-private"
                        val url = "https://accounts.spotify.com/authorize?client_id=${Uri.encode(clientId)}&response_type=code&redirect_uri=${Uri.encode(redirectUri)}&scope=${Uri.encode(scopes)}&show_dialog=true"
                        open(url)
                        feedback = "Spotify authorization opened. PKCE token exchange + callback will be activated with the production Spotify app credentials."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (connected) "Reconnect Spotify" else "Connect Spotify account") }
            OutlinedButton(onClick = { open("https://open.spotify.com/") }, modifier = Modifier.fillMaxWidth()) { Text("Open Spotify") }
            if (feedback.isNotBlank()) Text(feedback, color = c.muted)
        }

        RsPanel(c) {
            Text("IMPORT FROM SPOTIFY", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Paste a Spotify playlist link now, or after OAuth is connected choose directly from your Spotify playlists.", color = c.muted)
            OutlinedTextField(
                value = playlistLink,
                onValueChange = { playlistLink = it },
                label = { Text("Spotify playlist link") },
                placeholder = { Text("https://open.spotify.com/playlist/…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    val clean = playlistLink.trim()
                    if (!clean.contains("spotify.com/playlist/")) {
                        feedback = "Enter a valid Spotify playlist link."
                    } else {
                        val name = "Spotify Playlist ${saved.size + 1}"
                        if (name !in saved) saved.add(name)
                        store.ps("spotify_link_${name.hashCode()}", clean)
                        persistSaved()
                        playlistLink = ""
                        feedback = "Playlist saved inside RS KICKBOX."
                    }
                },
                enabled = playlistLink.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Spotify playlist in RS") }
        }

        RsPanel(c) {
            Text("RS TRAINING PLAYLISTS", color = c.bright, fontWeight = FontWeight.Bold)
            Text("Choose a default playlist. Spotify audio continues to play through Spotify; RS KICKBOX keeps the training shortcut and playlist assignment.", color = c.muted)
            saved.forEach { name ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = defaultPlaylist == name,
                        onClick = {
                            defaultPlaylist = name
                            store.ps("spotify_default_playlist", name)
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(name, color = c.text, fontWeight = FontWeight.Bold)
                        Text(if (defaultPlaylist == name) "Default training playlist" else "Saved playlist", color = c.muted)
                    }
                    val link = store.s("spotify_link_${name.hashCode()}", "")
                    if (link.isNotBlank()) TextButton(onClick = { open(link) }) { Text("Open") }
                }
            }
            OutlinedButton(
                onClick = {
                    val newName = when {
                        "Fight Night" !in saved -> "Fight Night"
                        "Technical Flow" !in saved -> "Technical Flow"
                        else -> "RS Playlist ${saved.size + 1}"
                    }
                    saved.add(newName)
                    persistSaved()
                    feedback = "$newName added to your RS playlist collection."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Create internal RS playlist") }
        }

        if (role == RsRole.TRAINER) {
            RsPanel(c) {
                Text("CLASS PLAYLIST ASSIGNMENTS", color = c.bright, fontWeight = FontWeight.Bold)
                listOf(
                    "Warm-up" to "Warm-up Energy",
                    "Pads / intensity" to "Pads Intensity",
                    "Technical work" to "Technical Flow",
                    "Sparring" to "Fight Night",
                    "Cooldown" to "Cooldown Flow"
                ).forEach { (block, playlist) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(block, color = c.text)
                        Text(playlist, color = c.bright, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Later the trainer can assign any connected Spotify playlist to a session template or class.", color = c.muted)
            }

            RsPanel(c) {
                Text("SPOTIFY ADMIN SETUP", color = c.bright, fontWeight = FontWeight.Bold)
                Text("Needed once for the production connection. Client ID is public configuration; the app will use PKCE instead of embedding a client secret.", color = c.muted)
                OutlinedTextField(clientId, { clientId = it }, label = { Text("Spotify Client ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(redirectUri, { redirectUri = it }, label = { Text("Redirect URI") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = {
                    store.ps("spotify_client_id", clientId.trim())
                    store.ps("spotify_redirect_uri", redirectUri.trim())
                    feedback = "Spotify app configuration saved locally for the preview."
                }, modifier = Modifier.fillMaxWidth()) { Text("Save Spotify configuration") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(connected, onCheckedChange = {
                        connected = it
                        store.pb("spotify_connected_preview", it)
                    })
                    Text("Simulate connected account for preview testing", color = c.text)
                }
            }
        }

        RsPanel(c) {
            Text("SPOTIFY PLATFORM RULES", color = c.bright, fontWeight = FontWeight.Bold)
            Text("RS KICKBOX will store playlist references and permissions, not copy Spotify audio into the app. Playback and Spotify content remain under Spotify's platform and account rules.", color = c.muted)
        }
    }
}

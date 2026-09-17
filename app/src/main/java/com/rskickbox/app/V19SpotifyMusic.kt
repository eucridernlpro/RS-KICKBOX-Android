package com.rskickbox.app

import androidx.compose.runtime.Composable

/**
 * Compatibility route retained from v0.19. The Spotify-first implementation has
 * been replaced by the RS local music player so existing dashboard routes keep
 * working while the app moves to device-library audio as the primary source.
 */
@Composable
fun RsSpotifyMusicCenterV19(c:RsPalette, store:RsStore, role:RsRole){
    RsLocalMusicCenterV20(c,store,role)
}

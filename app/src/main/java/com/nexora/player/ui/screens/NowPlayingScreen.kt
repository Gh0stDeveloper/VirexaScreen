
package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.nexora.player.data.model.MediaKind
import com.nexora.player.playback.PlayerEngine

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val snapshot by PlayerEngine.snapshot.collectAsState()
    val current = snapshot.currentItem

    when (current?.kind) {
        MediaKind.VIDEO -> VideoPlayerScreen(modifier.fillMaxSize(), current, onClose)
        MediaKind.AUDIO, null -> AudioPlayerScreen(modifier.fillMaxSize(), current, onClose)
    }
}

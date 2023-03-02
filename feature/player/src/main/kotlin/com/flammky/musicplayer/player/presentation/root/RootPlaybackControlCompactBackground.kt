package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flammky.musicplayer.player.presentation.root.CompactControlBackgroundState.Applier.Companion.PrepareCompositionInline

@Composable
fun RootPlaybackControlCompactBackground(
    state: CompactControlBackgroundState
) {
    state.applier
        .apply { PrepareCompositionInline() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .run { with(state) { backgroundModifier() } }
    ) {}
}
package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun RootPlaybackControlCompact(
    state: RootPlaybackControlCompactState
) {
    val applier = remember(state) {
       RootPlaybackControlCompactApplier(state)
           .apply {
               prepareState()
           }
    }.apply {
        PrepareCompose()
    }
}
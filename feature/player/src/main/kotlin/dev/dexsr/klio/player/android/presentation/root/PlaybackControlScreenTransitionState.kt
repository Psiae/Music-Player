package dev.dexsr.klio.player.android.presentation.root

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlaybackControlScreenTransitionState(
    private val screenState: PlaybackControlScreenState
) {

    var targetHeightPx by mutableStateOf(0)

    var stagedOffsetPx by mutableStateOf(0)

    var renderedOffsetPx by mutableStateOf(0)

    val freeze by derivedStateOf { screenState.freeze }

    var consumeShowSnap = true

    fun shouldShowScreen(): Boolean {
        return stagedOffsetPx < targetHeightPx
    }
}
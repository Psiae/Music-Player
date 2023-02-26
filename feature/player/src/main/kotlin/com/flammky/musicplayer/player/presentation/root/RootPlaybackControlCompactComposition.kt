package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class RootPlaybackControlCompactComposition {
    // lateinit, safe to assert
    var currentTransition by mutableStateOf<RootPlaybackControlCompactTransitionState?>(null)
    // lateinit, safe to assert
    var currentPager by mutableStateOf<RootPlaybackControlCompactPagerState?>(null)
    // lateinit, safe to assert
    var currentControls by mutableStateOf<RootPlaybackControlCompactControlsState?>(null)
    // lateinit, safe to assert
    var currentTimeBar by mutableStateOf<RootPlaybackControlCompactTimeBarState?>(null)
    // lateinit, safe to assert
    var currentBackground by mutableStateOf<RootPlaybackControlCompactBackgroundState?>(null)
}

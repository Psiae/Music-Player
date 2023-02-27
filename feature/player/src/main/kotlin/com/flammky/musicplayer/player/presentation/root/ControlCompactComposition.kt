package com.flammky.musicplayer.player.presentation.root

import androidx.compose.ui.unit.Dp
import com.flammky.musicplayer.base.compose.SnapshotRead

class ControlCompactComposition internal constructor(
    private val getLayoutHeight: @SnapshotRead () -> Dp,
    private val getLayoutWidth: @SnapshotRead () -> Dp
) {

    val transitionState = CompactControlTransitionState(
        getLayoutHeight = getLayoutHeight,
        getLayoutWidth = getLayoutWidth
    )
    val pagerState = CompactControlPagerState()
    val controlsState = CompactButtonControlsState()
    val timeBarState = CompactTimeBarState()
    val backgroundState = CompactBackgroundState()
}

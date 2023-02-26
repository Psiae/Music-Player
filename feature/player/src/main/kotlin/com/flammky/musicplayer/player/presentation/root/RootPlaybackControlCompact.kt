package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun RootPlaybackControlCompact(
    state: RootPlaybackControlCompactState
) {
    val coordinator = remember(state) {
       RootPlaybackControlCompactCoordinator(state)
           .apply {
               prepareState()
           }
    }.apply {
        PrepareCompose()
    }
    ContentTransition(coordinator = coordinator)
}

@Composable
private fun ContentTransition(
    coordinator: RootPlaybackControlCompactCoordinator
) {
    val state = remember(coordinator) {
        RootPlaybackControlCompactTransitionState()
    }.apply {

    }
}

@Composable
private fun ContentLayout(
    composition: RootPlaybackControlCompactComposition
) {
    Box {
        RootPlaybackControlCompactBackground(composition.currentBackground!!)
        Column {
            Row {
                PagerControl(state = composition.currentPager!!)
                ButtonControls(controlState = composition.currentControls!!)
            }
            TimeBar(timeBarData = composition.currentTimeBar!!)
        }
    }

}

@Composable
private fun PagerControl(
    state: RootPlaybackControlCompactPagerState
) {

}

@Composable
private fun ArtworkDisplay(
    pagerData: RootPlaybackControlCompactPagerState
) {

}

@Composable
private fun ButtonControls(
    controlState: RootPlaybackControlCompactControlsState
) {

}

@Composable
private fun TimeBar(
    timeBarData: RootPlaybackControlCompactTimeBarState
) {

}
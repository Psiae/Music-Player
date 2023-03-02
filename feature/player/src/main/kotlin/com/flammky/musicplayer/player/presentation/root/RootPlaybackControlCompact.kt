package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.flammky.musicplayer.player.presentation.root.CompactControlTransitionState.Applier.Companion.PrepareCompositionInline
import com.flammky.musicplayer.player.presentation.root.ControlCompactCoordinator.Companion.PrepareCompositionInline
import com.google.accompanist.pager.ExperimentalPagerApi

@Composable
internal fun RootPlaybackControlCompact(
    state: RootPlaybackControlCompactState
) {
    val coordinator = state.coordinator
        .apply {
            PrepareCompositionInline()
        }
    TransitioningContentLayout(coordinator.layoutComposition)
}

@Composable
private fun TransitioningContentLayout(
    composition: ControlCompactComposition
) {
    val state = composition.transitionState
        .apply {
            applier.PrepareCompositionInline()
        }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(state.layoutHeight)
                .width(state.layoutWidth)
                .offset(state.animatedLayoutOffset.x, state.animatedLayoutOffset.y)
        ) {
            ContentLayout(composition = composition)
        }
    }
}

@Composable
private fun ContentLayout(
    composition: ControlCompactComposition
) {
    Box {
        RootPlaybackControlCompactBackground(composition.backgroundState)
        Column {
            Row {
                PagerControl(state = composition.pagerState)
                ButtonControls(state = composition.controlsState)
            }
            TimeBar(state = composition.timeBarState)
        }
    }

}

@OptIn(ExperimentalPagerApi::class)
@Composable
private inline fun RowScope.PagerControl(
    state: CompactControlPagerState
) = Box(modifier = Modifier.weight(1f)) { CompactControlPager(state = state) }

@Composable
private inline fun ArtworkDisplay(
    state: CompactControlArtworkState
) = Box { CompactControlArtwork(state = state) }

@Composable
private inline fun ButtonControls(
    state: CompactButtonControlsState
) = Box { CompactControlButtons(state = state) }

@Composable
private inline fun TimeBar(
    state: CompactControlTimeBarState
) = Box { CompactControlTimeBar(state = state) }
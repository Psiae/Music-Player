package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerContentInViewModifier
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerScrollableState
import dev.dexsr.klio.player.android.presentation.root.main.pager.gesture.bringContentInView
import dev.dexsr.klio.player.android.presentation.root.main.pager.gesture.pointerScrollable
import dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll.PlaybackPagerOverscrollEffect

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.playbackPagerScrollable(
    state: PlaybackPagerScrollableState,
    orientation: Orientation,
    overscrollEffect: PlaybackPagerOverscrollEffect
): Modifier = composed(
    factory = {
        val reverseDirection = false
        val coroutineScope = rememberCoroutineScope()
        val bringContentInView =
            remember(coroutineScope, orientation, state, reverseDirection) {
                PlaybackPagerContentInViewModifier(
                    coroutineScope,
                    orientation,
                    state,
                    reverseDirection
                )
            }

        Modifier
            .run {
                @OptIn(ExperimentalFoundationApi::class)
                focusGroup()
            }
            .bringContentInView(bringContentInView)
            .pointerScrollable(
                state = state,
                orientation = orientation,
                overscrollEffect = overscrollEffect
            )
    }
)
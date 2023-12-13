package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.Velocity
import dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll.PlaybackPagerOverscrollEffect

interface PlaybackPagerGestureScrollableState {

    suspend fun bringChildFocusScroll(
        scroll: suspend PlaybackPagerScrollScope.() -> Unit
    )

    // call to this function meant the beginning of a new scroll
    // maybe we can return something instead of performFling in this interface
    suspend fun userDragScroll(
        overscrollEffect: PlaybackPagerOverscrollEffect,
        scroll: suspend PlaybackPagerScrollScope.() -> Unit
    ): Any

    // performFling of previous successful userDragScroll
    // maybe: coroutine job
    @OptIn(ExperimentalFoundationApi::class)
    suspend fun performFling(
        key: Any,
        velocity: Velocity,
        overscrollEffect: PlaybackPagerOverscrollEffect
    )
}
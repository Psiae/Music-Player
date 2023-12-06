package dev.dexsr.klio.player.android.presentation.root.main.pager

interface PlaybackPagerGestureScrollableState {

    suspend fun bringChildFocusScroll(
        scroll: suspend PlaybackPagerScrollScope.() -> Unit
    )
}
package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.runtime.Composable

class PlaybackPagerContentFactory(
    private val itemProvider: PlaybackPagerItemLayoutProvider
) {

    fun getContent(index: Int): @Composable () -> Unit {
        return itemProvider.getContent(index)
    }
}
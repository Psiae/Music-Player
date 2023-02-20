package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun rememberRootPlaybackControlPagerState(
    composition: RootPlaybackControlMainScope
): RootPlaybackControlPagerState {
    return remember(composition) {
        RootPlaybackControlPagerState(
            composition = composition,
            pagerLayoutState = PagerState(),
            observeMetadata = composition.observeTrackMetadata,
            observeArtwork = composition.observeArtwork
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
internal class RootPlaybackControlPagerState constructor(
    private val composition: RootPlaybackControlMainScope,
    val pagerLayoutState: PagerState,
    observeMetadata: (String) -> Flow<MediaMetadata?>,
    observeArtwork: (String) -> Flow<Any?>,
) {

    private val _observeMetadata = observeMetadata
    private val _observeArtwork = observeArtwork

    val compositionLatestQueue
        get() = composition.currentQueue

    var latestDisplayQueue by mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)

    var overrideDisplayQueue by mutableStateOf<OldPlaybackQueue?>(null)
        private set

    val maskedQueue by derivedStateOf { overrideDisplayQueue ?: latestDisplayQueue }

    fun incrementQueueReader() {
        composition.currentQueueReaderCount++
    }

    fun decrementQueueReader() {
        composition.currentQueueReaderCount--
    }

    fun observeMetadata(id: String) = _observeMetadata(id)
    fun observeArtwork(id: String) = _observeArtwork(id)

    fun overrideMoveNextIndex(
        withBase: OldPlaybackQueue,
        newIndex: Int
    ): Boolean {
        if (
            (maskedQueue.currentIndex + 1) != newIndex ||
            maskedQueue.currentIndex != withBase.currentIndex ||
            maskedQueue.list !== withBase.list
        ) {
            return false
        }
        overrideDisplayQueue = withBase.copy(currentIndex = newIndex)
        return true
    }

    fun overrideMovePreviousIndex(
        withBase: OldPlaybackQueue,
        newIndex: Int
    ): Boolean {
        if (
            (maskedQueue.currentIndex - 1) != newIndex ||
            maskedQueue.currentIndex != withBase.currentIndex ||
            maskedQueue.list !== withBase.list
        ) {
            return false
        }
        overrideDisplayQueue = withBase.copy(currentIndex = newIndex)
        return true
    }

    fun clearOverride() {
        overrideDisplayQueue = null
    }

    fun onCompositionQueueUpdated(
        new: OldPlaybackQueue
    ) {
        clearOverride()
        latestDisplayQueue = new
    }
}


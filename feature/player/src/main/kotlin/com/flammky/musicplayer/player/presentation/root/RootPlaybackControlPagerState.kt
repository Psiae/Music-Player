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

    suspend fun requestPlaybackMoveNext(
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectNextIndex: Int,
        expectNextId: String
    ): Boolean {
        val success = runCatching {
            if (!overrideMoveNextIndex(expectCurrentQueue, expectNextIndex)) {
                return@runCatching false
            }
            composition.playbackController.requestSeekAsync(
                expectFromIndex,
                expectFromId,
                expectNextIndex,
                expectNextId
            ).await().run {
                eventDispatch?.join()
                success
            }
        }.getOrElse { false }
        if (--overrideCount == 0) {
            overrideDisplayQueue = null
        }
        return success
    }

    suspend fun requestPlaybackMovePrevious(
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousIndex: Int,
        expectPreviousId: String
    ): Boolean {
        val success = runCatching {
            if (
                !overrideMoveNextIndex(expectCurrentQueue, expectPreviousIndex)
            ) {
                return@runCatching false
            }
            composition.playbackController.requestSeekAsync(
                expectFromIndex,
                expectFromId,
                expectPreviousIndex,
                expectPreviousId
            ).await().run {
                eventDispatch?.join()
                success
            }
        }.getOrElse { false }
        if (--overrideCount == 0) {
            overrideDisplayQueue = null
        }
        return success
    }



    private val _observeMetadata = observeMetadata
    private val _observeArtwork = observeArtwork

    val compositionLatestQueue
        get() = composition.currentQueue

    var latestDisplayQueue by mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)

    var overrideDisplayQueue by mutableStateOf<OldPlaybackQueue?>(null)
        private set

    val maskedDisplayQueue by derivedStateOf { overrideDisplayQueue ?: latestDisplayQueue }

    var overrideCount = 0

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
        overrideCount++
        overrideDisplayQueue = (overrideDisplayQueue ?: withBase).copy(currentIndex = newIndex)
        return true
    }

    fun overrideMovePreviousIndex(
        withBase: OldPlaybackQueue,
        newIndex: Int
    ): Boolean {
        overrideCount++
        overrideDisplayQueue = (overrideDisplayQueue ?: withBase).copy(currentIndex = newIndex)
        return true
    }

    fun clearOverride() {
        overrideDisplayQueue = null
    }

    fun onCompositionQueueUpdated(
        new: OldPlaybackQueue
    ) {
    }
}


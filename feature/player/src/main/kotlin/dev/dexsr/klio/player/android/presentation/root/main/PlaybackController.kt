package dev.dexsr.klio.player.android.presentation.root.main

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface PlaybackController {

    fun playbackProgressAsFlow(
        uiWidthDp: Float
    ): Flow<PlaybackProgress>

    fun playbackProgressAsFlow(
        getUiWidthDp: () -> Float
    ): Flow<PlaybackProgress>

    fun playbackProgressionStateAsFlow(

    ): Flow<PlaybackProgressionState>

    fun invokeOnTimelineChanged(
        range: Int,
        block: (PlaybackTimeline, Int) -> Unit
    ): DisposableHandle

    fun playAsync()

    fun pauseAsync()

    fun seekToNextMediaItemAsync(): Deferred<Boolean>

    fun seekToPreviousMediaItemAsync(): Deferred<Boolean>

    fun seekToPreviousAsync()

    fun seekToNextAsync()

    fun toggleRepeatAsync()

    fun toggleShuffleAsync()

    fun requestSeekAsync(
        percent: Float,
    ): Deferred<Boolean>

    fun getPlaybackProgressAsync(): Deferred<PlaybackProgress>

    fun getTimelineAsync(
        range: Int
    ): Deferred<PlaybackTimeline>

    fun seekToIndexAsync(
        fromIndex: Int,
        fromId: String,
        index: Int,
        id: String
    ): Deferred<Result<Boolean>>

    fun moveQueueItemAsync(
        fromIndex: Int,
        fromId: String,
        index: Int,
        id: String
    ): Deferred<Result<Boolean>>
}

object NoOpPlaybackController : PlaybackController {


    override fun playbackProgressAsFlow(uiWidthDp: Float): Flow<PlaybackProgress> {
        return flowOf()
    }

    override fun playbackProgressAsFlow(getUiWidthDp: () -> Float): Flow<PlaybackProgress> {
        return flowOf()
    }

    override fun playbackProgressionStateAsFlow(): Flow<PlaybackProgressionState> {
        return flowOf()
    }

    override fun requestSeekAsync(
        percent: Float
    ): Deferred<Boolean> {
        return CompletableDeferred<Boolean>().apply { cancel() }
    }

    override fun invokeOnTimelineChanged(
        range: Int,
        block: (PlaybackTimeline, Int) -> Unit
    ): DisposableHandle {
        return DisposableHandle {  }
    }

    override fun playAsync() {
    }

    override fun pauseAsync() {
    }

    override fun seekToNextMediaItemAsync(): Deferred<Boolean> {
        return CompletableDeferred<Boolean>().apply { cancel() }
    }

    override fun seekToPreviousMediaItemAsync(): Deferred<Boolean> {
        return CompletableDeferred<Boolean>().apply { cancel() }
    }

    override fun toggleRepeatAsync() {
    }

    override fun toggleShuffleAsync() {
    }

    override fun seekToPreviousAsync() {
    }

    override fun seekToNextAsync() {
    }

    override fun getPlaybackProgressAsync(): Deferred<PlaybackProgress> {
        return CompletableDeferred<PlaybackProgress>().apply { cancel() }
    }

    override fun getTimelineAsync(
        range: Int
    ): Deferred<PlaybackTimeline> {
        return CompletableDeferred<PlaybackTimeline>().apply { cancel() }
    }

    override fun seekToIndexAsync(
        fromIndex: Int,
        fromId: String,
        index: Int,
        id: String
    ): Deferred<Result<Boolean>> {
        return CompletableDeferred<Result<Boolean>>().apply { cancel() }
    }

    override fun moveQueueItemAsync(
        fromIndex: Int,
        fromId: String,
        index: Int,
        id: String
    ): Deferred<Result<Boolean>> {
        return CompletableDeferred<Result<Boolean>>().apply { cancel() }
    }
}
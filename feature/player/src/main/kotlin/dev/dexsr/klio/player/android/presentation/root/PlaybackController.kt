package dev.dexsr.klio.player.android.presentation.root

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

    fun seekToNextMediaItemAsync()

    fun seekToPreviousMediaItemAsync()

    fun seekToPreviousAsync()

    fun seekToNextAsync()

    fun toggleRepeatAsync()

    fun toggleShuffleAsync()

    fun requestSeekAsync(
        percent: Float,
    ): Deferred<Boolean>

    fun getPlaybackProgressAsync(): Deferred<PlaybackProgress>
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

    override fun seekToNextMediaItemAsync() {
    }

    override fun seekToPreviousMediaItemAsync() {
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
}
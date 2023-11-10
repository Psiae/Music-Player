package dev.dexsr.klio.player.android.presentation.root

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface RootCompactPlaybackController {

    fun currentlyPlayingMediaIdAsFlow(): Flow<String?>

    fun playbackProgressAsFlow(
        uiWidthDp: Float
    ): Flow<PlaybackProgress>

    fun playbackProgressionStateAsFlow(

    ): Flow<PlaybackProgressionState>

    fun playbackTimelineAsFlow(
        range: Int
    ): Flow<PlaybackTimeline>

    fun getPlaybackTimelineAsync(
        range: Int
    ): Deferred<PlaybackTimeline>

    fun play()

    fun pause()

    fun seekToNextMediaItemAsync()

    fun seekToPreviousMediaItemAsync()

    fun invokeOnMoveToNextMediaItem(
        block: (Int) -> Unit
    ): DisposableHandle

    fun invokeOnMoveToPreviousMediaItem(
        block: (Int) -> Unit
    ): DisposableHandle

    fun invokeOnTimelineChanged(
        range: Int,
        block: (PlaybackTimeline, Int) -> Unit
    ): DisposableHandle
}

object NoOpRootCompactPlaybackController : RootCompactPlaybackController {

    override fun currentlyPlayingMediaIdAsFlow(): Flow<String?> = flowOf()

    override fun playbackProgressAsFlow(uiWidthDp: Float): Flow<PlaybackProgress> = flowOf()

    override fun playbackProgressionStateAsFlow(): Flow<PlaybackProgressionState> {
        return flowOf()
    }

    override fun playbackTimelineAsFlow(range: Int): Flow<PlaybackTimeline> {
        return flowOf()
    }

    override fun getPlaybackTimelineAsync(range: Int): Deferred<PlaybackTimeline> {
        return CompletableDeferred<PlaybackTimeline>().apply { cancel() }
    }

    override fun play() {
    }

    override fun pause() {
    }

    override fun seekToNextMediaItemAsync() {
    }

    override fun seekToPreviousMediaItemAsync() {
    }

    override fun invokeOnMoveToNextMediaItem(block: (Int) -> Unit): DisposableHandle {
        return DisposableHandle {  }
    }

    override fun invokeOnMoveToPreviousMediaItem(block: (Int) -> Unit): DisposableHandle {
        return DisposableHandle {  }
    }

    override fun invokeOnTimelineChanged(
        range: Int,
        block: (PlaybackTimeline, Int) -> Unit
    ): DisposableHandle {
        return DisposableHandle {}
    }
}
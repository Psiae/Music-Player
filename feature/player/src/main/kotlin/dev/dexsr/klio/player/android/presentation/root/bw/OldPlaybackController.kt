package dev.dexsr.klio.player.android.presentation.root.bw

import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.base.ktx.coroutines.initAsParentCompleter
import dev.dexsr.klio.player.android.presentation.root.PlaybackController
import dev.dexsr.klio.player.android.presentation.root.PlaybackProgress
import dev.dexsr.klio.player.android.presentation.root.PlaybackProgressionState
import dev.dexsr.klio.player.android.presentation.root.PlaybackTimeline
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class OldPlaybackController(
    private val user: User,
    private val viewModel: PlaybackControlViewModel
) : PlaybackController {

    private val pc = viewModel.createUserPlaybackController(
        user
    )

    private val coroutineScope = CoroutineScope(SupervisorJob())

    // same impl, going to be removed anyway
    private val compact = OldRootCompactPlaybackController(user, viewModel)

    override fun playbackProgressAsFlow(
        uiWidthDp: Float
    ): Flow<PlaybackProgress> {
        return playbackProgressAsFlow(
            getUiWidthDp = { uiWidthDp }
        )
    }

    override fun playbackProgressAsFlow(
        getUiWidthDp: () -> Float
    ): Flow<PlaybackProgress> {

        return flow {
            val po = pc.createPlaybackObserver()
            val channel = Channel<PlaybackProgress>(Channel.UNLIMITED)
            val workers = mutableListOf<Job>()
            try {
                po.createDurationCollector()
                    .apply {
                        startCollect().join()
                        var progressCollector: Job? = null
                        coroutineScope.launch(Dispatchers.Main) {
                            durationStateFlow.collect() { duration ->
                                progressCollector?.cancel()
                                progressCollector = launch {
                                    val pc = po.createProgressionCollector()
                                    try {
                                        pc.setCollectEvent(true)
                                        pc.setIntervalHandler { isEvent, progress, bufferedProgress, duration, speed ->
                                            if (progress == Duration.ZERO || duration == Duration.ZERO || speed == 0f) {
                                                PlaybackConstants.DURATION_UNSET
                                            } else {
                                                (duration.inWholeMilliseconds / getUiWidthDp() / speed).toLong()
                                                    .takeIf { it > 100 }?.milliseconds
                                                    ?.coerceAtMost(1000.milliseconds)
                                                    ?: PlaybackConstants.DURATION_UNSET
                                            }
                                        }
                                        pc.run {
                                            startCollectPosition().join()
                                            launch {
                                                positionStateFlow.collect {
                                                    channel.send(
                                                        PlaybackProgress(
                                                            duration = duration,
                                                            position = positionStateFlow.value,
                                                            bufferedPosition = bufferedPositionStateFlow.value
                                                        )
                                                    )
                                                }
                                            }
                                            launch {
                                                bufferedPositionStateFlow.collect {
                                                    channel.send(
                                                        PlaybackProgress(
                                                            duration = duration,
                                                            position = positionStateFlow.value,
                                                            bufferedPosition = bufferedPositionStateFlow.value
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        awaitCancellation()
                                    } finally {
                                        pc.dispose()
                                    }
                                }
                            }
                        }.also { workers.add(it) }
                    }
                for (element in channel) {
                    emit(element)
                }
            } finally {
                po.dispose()
                workers.forEach { it.cancel() }
            }
        }
    }

    override fun playbackProgressionStateAsFlow(): Flow<PlaybackProgressionState> {
        return compact.playbackProgressionStateAsFlow()
    }

    override fun invokeOnTimelineChanged(
        range: Int,
        block: (PlaybackTimeline, Int) -> Unit
    ): DisposableHandle {
        return compact.invokeOnTimelineChanged(range, block)
    }

    override fun playAsync() {
        return compact.play()
    }

    override fun pauseAsync() {
        return compact.pause()
    }

    override fun seekToNextMediaItemAsync() {
        return compact.seekToNextMediaItemAsync()
    }

    override fun seekToPreviousMediaItemAsync() {
        return compact.seekToPreviousMediaItemAsync()
    }

    override fun toggleRepeatAsync() {
        return compact.toggleRepeatAsync()
    }

    override fun toggleShuffleAsync() {
    }

    override fun requestSeekAsync(
        percent: Float
    ): Deferred<Boolean> {
        val def = CompletableDeferred<Boolean>()
        coroutineScope.launch {
            def.completeWith(
                runCatching {
                    pc.requestSeekPositionAsync(percent).await().run {
                        eventDispatch?.join()
                        success
                    }
                }
            )
        }.initAsParentCompleter(def)
        return def
    }

    override fun seekToPreviousAsync() {
        pc.requestSeekPreviousAsync(Duration.ZERO)
    }

    override fun seekToNextAsync() {
        pc.requestSeekNextAsync(Duration.ZERO)
    }

    override fun getPlaybackProgressAsync(): Deferred<PlaybackProgress> {
        val def = CompletableDeferred<PlaybackProgress>()
        coroutineScope.launch {
            def.completeWith(
                runCatching { pc.getPlaybackProgressAsync().await().getOrThrow() }
            )
        }.initAsParentCompleter(def)
        return def
    }

    fun dispose() {
        compact.dispose()
        pc.dispose()
    }
}
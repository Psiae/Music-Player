package dev.dexsr.klio.player.presentation.root.bw

import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.player.presentation.root.PlaybackProgress
import dev.dexsr.klio.player.presentation.root.PlaybackProgressionState
import dev.dexsr.klio.player.presentation.root.RootCompactPlaybackController
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class OldRootCompactPlaybackController(
    private val user: User,
    private val viewModel: PlaybackControlViewModel
) : RootCompactPlaybackController {

    private val playbackController = viewModel.createUserPlaybackController(user = user)
    private val coroutineScope = CoroutineScope(SupervisorJob())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun currentlyPlayingMediaIdAsFlow(): Flow<String?> {
        return flow {
            val po = playbackController.createPlaybackObserver()
            try {
                po.createQueueCollector()
                    .apply {
                        startCollect().join()
                        queueStateFlow
                            .mapLatest { q ->
                                if (q.currentIndex == -1) return@mapLatest null
                                q.list[q.currentIndex]
                            }
                            .collect(this@flow)
                    }
            } finally {
                po.dispose()
            }
        }
    }

    override fun playbackProgressAsFlow(
        uiWidthDp: Float
    ): Flow<PlaybackProgress> {

        return flow {
            val po = playbackController.createPlaybackObserver()
            val channel = Channel<PlaybackProgress>(Channel.UNLIMITED)
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
                                                (duration.inWholeMilliseconds / uiWidthDp / speed).toLong()
                                                    .takeIf { it > 100 }?.milliseconds
                                                    ?: PlaybackConstants.DURATION_UNSET
                                            }
                                        }
                                        pc.run {
                                            startCollectPosition().join()
                                            launch {
                                                positionStateFlow.collect {
                                                    channel.send(PlaybackProgress(
                                                        duration = duration,
                                                        position = positionStateFlow.value,
                                                        bufferedPosition = bufferedPositionStateFlow.value
                                                    ))
                                                }
                                            }
                                            launch {
                                                bufferedPositionStateFlow.collect {
                                                    channel.send(PlaybackProgress(
                                                        duration = duration,
                                                        position = positionStateFlow.value,
                                                        bufferedPosition = bufferedPositionStateFlow.value
                                                    ))
                                                }
                                            }
                                        }
                                        awaitCancellation()
                                    } finally {
                                        pc.dispose()
                                    }
                                }
                            }
                        }
                    }
                for (element in channel) {
                    emit(element)
                }
            } finally {
                po.dispose()
            }

        }
    }

    override fun playbackProgressionStateAsFlow(): Flow<PlaybackProgressionState> {
        return flow {
            val channel = Channel<PlaybackProgressionState>(Channel.UNLIMITED)
            val obs = playbackController.createPlaybackObserver()
            try {
                coroutineScope.launch {
                    val pc = obs.createPropertiesCollector()
                    try {
                        pc.apply {
                            startCollect().join()
                        }
                        pc.run {
                            propertiesStateFlow
                                .collect {
                                    val progression = PlaybackProgressionState(
                                        isPlaying = it.playing,
                                        canPlay = it.canPlay,
                                        playWhenReady = it.playWhenReady,
                                        canPlayWhenReady = it.canPlayWhenReady
                                    )
                                    channel.send(progression)
                                }
                        }
                    } finally {
                        pc.dispose()
                    }
                }

                for (element in channel) {
                    emit(element)
                }
            } finally {
                obs.dispose()
            }
        }
    }

    override fun play() {
        playbackController.requestPlayAsync()
    }

    override fun pause() {
        playbackController.requestSetPlayWhenReadyAsync(playWhenReady = false)
    }

    fun dispose() {
        playbackController.dispose()
        coroutineScope.cancel()
    }
}
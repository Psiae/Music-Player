package dev.dexsr.klio.player.android.presentation.root.bw

import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.base.ktx.coroutines.initAsParentCompleter
import dev.dexsr.klio.player.android.presentation.root.PlaybackProgress
import dev.dexsr.klio.player.android.presentation.root.PlaybackProgressionState
import dev.dexsr.klio.player.android.presentation.root.PlaybackTimeline
import dev.dexsr.klio.player.android.presentation.root.RootCompactPlaybackController
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
                                                (duration.inWholeMilliseconds / uiWidthDp / speed).toLong()
                                                    .takeIf { it > 100 }?.milliseconds
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
        return flow {
            val channel = Channel<PlaybackProgressionState>(Channel.UNLIMITED)
            val obs = playbackController.createPlaybackObserver()
            val workers = mutableListOf<Job>()
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
                }.also { workers.add(it) }

                for (element in channel) {
                    emit(element)
                }
            } finally {
                obs.dispose()
                workers.forEach { it.cancel() }
            }
        }
    }

    override fun playbackTimelineAsFlow(range: Int): Flow<PlaybackTimeline> {
        return flow {
            val po = playbackController.createPlaybackObserver()
            try {
                po.createQueueCollector()
                    .apply {
                        startCollect().join()
                        queueStateFlow
                            .mapLatest { q ->
                                if (q.currentIndex < 0) {
                                    return@mapLatest PlaybackTimeline(
                                        currentIndex = -1,
                                        items = persistentListOf()
                                    )
                                }
                                if (range < 0 ){
                                    return@mapLatest PlaybackTimeline(
                                        currentIndex = -1,
                                        items = persistentListOf()
                                    )
                                }
                                if (range > q.list.size) {
                                    return@mapLatest PlaybackTimeline(
                                        currentIndex = q.currentIndex,
                                        items = q.list
                                    )
                                }
                                PlaybackTimeline(
                                    currentIndex = range.coerceAtMost(minOf(q.list.size - 1, q.currentIndex)),
                                    items = persistentListOf<String>().builder()
                                        .apply {
                                            // take left
                                            val leftRange = range
                                                .coerceAtMost(q.currentIndex)
                                            repeat(leftRange) { i ->
                                                add(q.list[q.currentIndex - (leftRange - i)])
                                            }
                                            // take center
                                            add(q.list[q.currentIndex])
                                            // take right
                                            val rightRange = range
                                                .coerceAtMost(q.list.lastIndex - q.currentIndex)
                                            repeat(rightRange) { i ->
                                                add(q.list[q.currentIndex + (i + 1)])
                                            }
                                        }
                                        .build()
                                )
                            }
                            .collect(this@flow)
                    }
            } finally {
                po.dispose()
            }
        }
    }

    override fun getPlaybackTimelineAsync(
        range: Int
    ): Deferred<PlaybackTimeline> {
        val def = CompletableDeferred<PlaybackTimeline>()

        coroutineScope.launch(Dispatchers.Main) {

            val po = playbackController.createPlaybackObserver()

            try {
                val q = po.createQueueCollector()
                    .apply { startCollect().join() }
                    .queueStateFlow.first()
                val tl = run get@ {
                    if (q.currentIndex < 0) {
                        return@get PlaybackTimeline(
                            currentIndex = -1,
                            items = persistentListOf()
                        )
                    }
                    if (range < 0 ){
                        return@get PlaybackTimeline(
                            currentIndex = -1,
                            items = persistentListOf()
                        )
                    }
                    if (range > q.list.size) {
                        return@get PlaybackTimeline(
                            currentIndex = q.currentIndex,
                            items = q.list
                        )
                    }
                    PlaybackTimeline(
                        currentIndex = range.coerceAtMost(minOf(q.list.size - 1, q.currentIndex)),
                        items = persistentListOf<String>().builder()
                            .apply {
                                // take left
                                val leftRange = range
                                    .coerceAtMost(q.currentIndex)
                                repeat(leftRange) { i ->
                                    add(q.list[q.currentIndex - (leftRange - i)])
                                }
                                // take center
                                add(q.list[q.currentIndex])
                                // take right
                                val rightRange = range
                                    .coerceAtMost(q.list.lastIndex - q.currentIndex)
                                repeat(rightRange) { i ->
                                    add(q.list[q.currentIndex + (i + 1)])
                                }
                            }
                            .build()
                    )
                }
                def.complete(PlaybackTimeline(tl.currentIndex, tl.items))
            } finally {
                po.dispose()
            }

        }.initAsParentCompleter(parent = def)

        return def
    }

    override fun invokeOnMoveToNextMediaItem(
        block: (step: Int) -> Unit
    ): DisposableHandle {
        val task = coroutineScope.launch(Dispatchers.Main) {
            // temporary, we are going to completely remake the media controller anyway
            var last: PlaybackTimeline? = null
            playbackTimelineAsFlow(Int.MAX_VALUE)
                .collect { timeline ->
                    if (last == null) {
                        last = timeline
                        return@collect
                    }
                    if (last!!.currentIndex < 0) {
                        last = timeline
                        block(0)
                        return@collect
                    }
                    if (last!!.currentIndex < timeline.currentIndex) {
                        block(timeline.currentIndex - last!!.currentIndex)
                    }
                    last = timeline
                }
        }
        return DisposableHandle { task.cancel() }
    }

    override fun invokeOnMoveToPreviousMediaItem(
        block: (step: Int) -> Unit
    ): DisposableHandle {
        val task = coroutineScope.launch(Dispatchers.Main) {
            // temporary, we are going to completely remake the media controller anyway
            var last: PlaybackTimeline? = null
            playbackTimelineAsFlow(Int.MAX_VALUE)
                .collect { timeline ->
                    if (last == null) {
                        last = timeline
                        return@collect
                    }
                    if (last!!.currentIndex < 0) {
                        last = timeline
                        return@collect
                    }
                    if (timeline.currentIndex < 0) {
                        last = timeline
                        return@collect
                    }
                    if (last!!.currentIndex > timeline.currentIndex) {
                        block(last!!.currentIndex - timeline.currentIndex)
                    }
                    last = timeline
                }
        }
        return DisposableHandle { task.cancel() }
    }

    override fun invokeOnTimelineChanged(
        range: Int,
        block: (PlaybackTimeline) -> Unit
    ): DisposableHandle {
        val task = coroutineScope.launch(Dispatchers.Main) {
            var last: PlaybackTimeline? = null
            playbackTimelineAsFlow(range)
                .collect { timeline ->
                    if (last == null) {
                        last = timeline
                        block(timeline)
                    }
                    if (last != timeline) {
                        last = timeline
                        block(timeline)
                    }
                }
        }

        return DisposableHandle { task.cancel() }
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
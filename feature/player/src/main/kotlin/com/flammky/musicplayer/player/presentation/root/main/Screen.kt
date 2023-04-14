package com.flammky.musicplayer.player.presentation.root.main

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.palette.graphics.Palette
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.root.main.PlaybackControlScreenLayoutCoordinator.Companion.restore
import com.flammky.musicplayer.player.presentation.root.main.PlaybackControlScreenLayoutCoordinator.Companion.saveAsBundle
import com.flammky.musicplayer.player.presentation.root.main.queue.PlaybackQueueScreen
import com.flammky.musicplayer.player.presentation.root.main.queue.QueueContainerTransitionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import kotlin.time.Duration

data class PlaybackControlScreenIntents(
    val dismiss: () -> Unit,
    val requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectToIndex: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPositionAsync: (
        expectFromId: String,
        expectFromDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    val requestMoveQueueItemAsync: (
        from: Int,
        expectFromId: String,
        to: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    val requestPlayAsync: () -> Deferred<Result<Boolean>>,
    val requestPauseAsync: () -> Deferred<Result<Boolean>>,
    val requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>,
    val requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>,
)

data class PlaybackControlScreenDataSource(
    val user: User,
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observePlaybackProperties: () -> Flow<PlaybackProperties>,
    val observeDuration: () -> Flow<Duration>,
    val observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>,
    val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
)

data class MainScreenDataSource(
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observePlaybackProperties: () -> Flow<PlaybackProperties>,
    val observeDuration: () -> Flow<Duration>,
    val observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>,
    val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
    val observePalette: () -> Flow<Palette?>,
)

data class MainScreenIntents(
    val dismiss: () -> Unit,
    val showQueue: () -> Unit,
    val requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectToIndex: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPositionAsync: (
        expectFromId: String,
        expectFromDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    val requestPlayAsync: () -> Deferred<Result<Boolean>>,
    val requestPauseAsync: () -> Deferred<Result<Boolean>>,
    val requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>,
    val requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>,
)

data class QueueScreenDataSource(
    val user: User,
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
    val observePlaybackProperties: () -> Flow<PlaybackProperties>
)

data class QueueScreenIntents(
    val requestMoveQueueItemAsync: (
        from: Int,
        expectFromId: String,
        to: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekIndexAsync: (
        from: Int,
        expectFromId: String,
        to: Int,
        expectToId: String,
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
    val requestPlayAsync: () -> Deferred<Result<Boolean>>,
    val requestPauseAsync: () -> Deferred<Result<Boolean>>
)

@Composable
fun rememberPlaybackControlScreenState(
    intents: PlaybackControlScreenIntents,
    source: PlaybackControlScreenDataSource,
    backPressRegistry: BackPressRegistry
): PlaybackControlScreenState {
    return remember(intents, source, backPressRegistry) {
        PlaybackControlScreenState(intents, source, backPressRegistry)
    }
}

@Composable
fun PlaybackControlScreen(
    state: PlaybackControlScreenState
) = rememberSaveable(
    state,
    saver = remember(state) { PlaybackControlScreenCoordinator.Saver(state) }
) { PlaybackControlScreenCoordinator(state) }.ComposeLayout(
    main = {
        PlaybackControlMainScreen()
    },
    queue = {
        // should queue be inside main ?
        PlaybackQueueScreen()
    }
)

class PlaybackControlScreenState(
    internal val intents: PlaybackControlScreenIntents,
    internal val source: PlaybackControlScreenDataSource,
    internal val backPressRegistry: BackPressRegistry
) {

    private var currentCoordinator by mutableStateOf<PlaybackControlScreenCoordinator?>(null)

    private var show by mutableStateOf(false)

    @SnapshotRead
    fun visibleHeightPx(rendered: Boolean): Int {
        return currentCoordinator
            ?.let { cord ->
                with(cord.layoutCoordinator) {
                    if (rendered) currentRenderedHeightPx() else currentStagedHeightPx()
                }
            }
            ?: 0
    }

    @SnapshotRead
    fun visibleWidthPx(rendered: Boolean): Int {
        return currentCoordinator
            ?.let { cord ->
                with(cord.layoutCoordinator) {
                    if (rendered) currentRenderedWidthPx() else currentStagedWidthPx()
                }
            }
            ?: 0
    }

    @SnapshotRead
    fun targetHeightPx(): Int {
        return currentCoordinator?.layoutCoordinator?.currentTargetHeightPx() ?: 0
    }

    @SnapshotRead
    fun targetWidthPx(): Int {
        return currentCoordinator?.layoutCoordinator?.currentTargetWidthPx() ?: 0
    }

    fun requestShow(): Boolean {
        Timber.d(
            "player.root.main.Screen.kt: PlaybackControlScreenState@${System.identityHashCode(this)}_requestShow()[$currentCoordinator]"
        )
        this.show = true
        currentCoordinator?.show()
        return true
    }

    fun requestHide(): Boolean {
        Timber.d(
            "player.root.main.Screen.kt: PlaybackControlScreenState@${System.identityHashCode(this)}_requestHide()[$currentCoordinator]"
        )
        this.show = false
        currentCoordinator?.hide()
        return true
    }

    internal fun updateCoordinator(coordinator: PlaybackControlScreenCoordinator) {
        if (currentCoordinator == coordinator) {
            return
        }
        check(currentCoordinator?.disposed != false) {
            "dupe"
        }
        currentCoordinator = coordinator
        if (show) requestShow() else requestHide()
    }
}

class PlaybackControlScreenCoordinator(
    private val state: PlaybackControlScreenState,
) {

    var disposed by mutableStateOf(false)
        private set

    val layoutCoordinator = PlaybackControlScreenLayoutCoordinator(state)

    fun show(): Boolean = layoutCoordinator.show()

    fun hide(): Boolean = layoutCoordinator.hide()


    interface MainRenderScope {
        val modifier: Modifier
        val transitionState: MainContainerTransitionState
        val dataSource: MainScreenDataSource
        val intents: MainScreenIntents
    }


    interface QueueScreenRenderScope {
        val transitionState: QueueContainerTransitionState
        val dataSource: QueueScreenDataSource
        val intents: QueueScreenIntents
    }

    class MainRenderScopeImpl(
        override val modifier: Modifier,
        override val transitionState: MainContainerTransitionState,
        override val intents: MainScreenIntents,
        override val dataSource: MainScreenDataSource
    ) : MainRenderScope {

    }

    class QueueScreenRenderScopeImpl(
        override val transitionState: QueueContainerTransitionState,
        override val intents: QueueScreenIntents,
        override val dataSource: QueueScreenDataSource
    ) : QueueScreenRenderScope {

    }


    @Composable
    fun ComposeLayout(
        main: @Composable MainRenderScope.() -> Unit,
        queue: @Composable QueueScreenRenderScope.() -> Unit
    ) {
        state.updateCoordinator(this)
        with(layoutCoordinator) {
            val uiCoroutineScope = rememberCoroutineScope()
            val mainTransition = rememberSaveable(
                this@PlaybackControlScreenCoordinator,
                saver = MainContainerTransitionState.Saver(uiCoroutineScope)
            ) init@ {
                MainContainerTransitionState(uiCoroutineScope)
            }
            val queueTransition = rememberSaveable(
                this@PlaybackControlScreenCoordinator,
                saver = QueueContainerTransitionState.Saver(mainTransition, uiCoroutineScope)
            ) init@ {
                QueueContainerTransitionState(mainTransition, uiCoroutineScope)
            }
            PlaceLayout(
                main = run {
                    val render = observeMainRenderScope(
                        mainTransition,
                        queueTransition
                    )
                    remember(render) {
                        PlaybackControlScreenLayoutCoordinator.MainPlacement(
                            composable = @Composable { render.main() },
                            transitionState = render.transitionState
                        )
                    }
                },
                queue = run {
                    val render = observeQueueRenderScope(
                        queueTransition
                    )
                    remember(render) {
                        PlaybackControlScreenLayoutCoordinator.QueuePlacement(
                            composable = @Composable { render.queue() },
                            transitionState = render.transitionState
                        )
                    }
                }
            )
            DisposableEffect(
                key1 = mainTransition.show, queueTransition.show,
                effect = {
                    if (!mainTransition.show && !mainTransition.show) {
                        return@DisposableEffect onDispose {  }
                    }
                    val backPressConsumer = BackPressRegistry.BackPressConsumer {
                        if (queueTransition.show) {
                            queueTransition.hide()
                            return@BackPressConsumer
                        }
                        if (mainTransition.show) {
                            state.intents.dismiss()
                        }
                    }
                    state.backPressRegistry.registerBackPressConsumer(backPressConsumer)
                    onDispose { state.backPressRegistry.unregisterBackPressConsumer(backPressConsumer) }
                }
            )
        }
    }

    @Composable
    private fun observeMainRenderScope(
        transitionState: MainContainerTransitionState,
        queueTransitionState: QueueContainerTransitionState
    ): MainRenderScope {
        return MainRenderScopeImpl(
            modifier = Modifier,
            transitionState = transitionState,
            intents = remember(state) {
                MainScreenIntents(
                    dismiss = state.intents.dismiss,
                    showQueue = queueTransitionState::show,
                    requestSeekNextAsync = state.intents.requestSeekNextAsync,
                    requestSeekPreviousAsync = state.intents.requestSeekPreviousAsync,
                    requestSeekNextWithExpectAsync = state.intents.requestSeekNextWithExpectAsync,
                    requestSeekPreviousWithExpectAsync = state.intents.requestSeekPreviousWithExpectAsync,
                    requestSeekAsync = state.intents.requestSeekAsync,
                    requestSeekPositionAsync = state.intents.requestSeekPositionAsync,
                    requestPlayAsync = state.intents.requestPlayAsync,
                    requestPauseAsync = state.intents.requestPauseAsync,
                    requestToggleRepeatAsync = state.intents.requestToggleRepeatAsync,
                    requestToggleShuffleAsync = state.intents.requestToggleShuffleAsync,
                )
            },
            dataSource = run {
                val bitmapFlowBuilder = rememberCurrentBitmapFlowBuilder(
                    observeQueue = state.source.observeQueue,
                    observeArtwork = state.source.observeArtwork
                )
                val paletteFlowBuilder = rememberCurrentPaletteFlowBuilder(
                    observeBitmap = bitmapFlowBuilder
                )
                remember(state, paletteFlowBuilder) {
                    MainScreenDataSource(
                        observeQueue = state.source.observeQueue,
                        observePlaybackProperties = state.source.observePlaybackProperties,
                        observeDuration = state.source.observeDuration,
                        observePositionWithIntervalHandle = state.source.observePositionWithIntervalHandle,
                        observeTrackMetadata = state.source.observeTrackMetadata,
                        observeArtwork = state.source.observeArtwork,
                        observePalette = paletteFlowBuilder
                    )
                }
            }
        )
    }

    @Composable
    private fun observeQueueRenderScope(
        transitionState: QueueContainerTransitionState,
    ): QueueScreenRenderScope {
        return QueueScreenRenderScopeImpl(
            transitionState,
            intents = QueueScreenIntents(
                requestMoveQueueItemAsync = state.intents.requestMoveQueueItemAsync,
                requestSeekIndexAsync = state.intents.requestSeekAsync,
                requestSeekPreviousAsync = state.intents.requestSeekPreviousAsync,
                requestSeekNextAsync = state.intents.requestSeekNextAsync,
                requestPlayAsync = state.intents.requestPlayAsync,
                requestPauseAsync = state.intents.requestPauseAsync
            ),
            dataSource = QueueScreenDataSource(
                user = state.source.user,
                observeQueue = state.source.observeQueue,
                observeTrackMetadata = state.source.observeTrackMetadata,
                observeArtwork = state.source.observeArtwork,
                observePlaybackProperties = state.source.observePlaybackProperties
            )
        )
    }

    @Composable
    private fun rememberCurrentBitmapFlowBuilder(
        observeQueue: () -> Flow<OldPlaybackQueue>,
        observeArtwork: (String) -> Flow<Any?>,
    ): () -> Flow<Bitmap?> {
        val uiCoroutineScope = rememberCoroutineScope()
        val upObserveQueue = rememberUpdatedState(observeQueue)
        val upObserveArtwork = rememberUpdatedState(observeArtwork)
        val readerCountState = remember { mutableStateOf(0) }
        val returns = remember { mutableStateOf<Bitmap?>(null) }


        DisposableEffect(
            key1 = this,
            effect = {
                val supervisor = SupervisorJob()

                // currentBitmap observer
                uiCoroutineScope.launch(supervisor) {
                    var job: Job? = null
                    snapshotFlow { readerCountState.value }
                        .collect { count ->
                            if (count == 0) {
                                job?.cancel()
                            } else if (count >= 1) {
                                // snapshotFlow is conflated by default
                                if (job?.isActive == true) return@collect
                                job = launch {
                                    var latestQueueFlow: Job? = null
                                    var latestArtworkFlow: Job? = null
                                    var latestTransformer: Job? = null

                                    snapshotFlow { upObserveQueue.value() }
                                        .collect { queueFlow ->
                                            latestQueueFlow?.cancel()
                                            latestQueueFlow = launch {
                                                queueFlow.collect {
                                                    latestTransformer?.cancel()
                                                    latestTransformer = launch {
                                                        it.list.getOrNull(it.currentIndex)
                                                            ?.let {
                                                                latestArtworkFlow?.cancel()
                                                                latestArtworkFlow = launch {
                                                                    snapshotFlow { upObserveArtwork.value(it) }
                                                                        .collect { artFlow ->
                                                                            artFlow.collect collectArtwork@ { art ->
                                                                                latestTransformer?.cancel()
                                                                                if (art is Bitmap) {
                                                                                    returns.value = art
                                                                                    return@collectArtwork
                                                                                }
                                                                                // else we need to collect the biggest layout viewport
                                                                                // within the composition and load the bitmap according to
                                                                                // the source kind, which is somewhat preferable
                                                                                returns.value = null
                                                                            }
                                                                        }
                                                                }
                                                            }
                                                            ?: run { returns.value = null }
                                                    }
                                                }

                                            }
                                        }
                                }
                            }
                        }
                }

                onDispose {
                    supervisor.cancel()
                }
            }
        )

        return remember(this) {
            {
                flow {
                    readerCountState.value++
                    try {
                        snapshotFlow { returns.value }.collect(this)
                    } finally {
                        readerCountState.value--
                    }
                }
            }
        }
    }

    @Composable
    private fun rememberCurrentPaletteFlowBuilder(
        observeBitmap: () -> Flow<Bitmap?>
    ): () -> Flow<Palette?> {
        val uiCoroutineScope = rememberCoroutineScope()
        val upObserveBitmap = rememberUpdatedState(observeBitmap)
        val readerCountState = remember { mutableStateOf(0) }
        val returns = remember { mutableStateOf<Palette?>(null) }

        DisposableEffect(
            this,
            effect = {
                val supervisor = SupervisorJob()

                uiCoroutineScope.launch(supervisor) {
                    var job: Job? = null
                    snapshotFlow { readerCountState.value }
                        .collect { count ->
                            if (count == 0) {
                                job?.cancel()
                            } else if (count >= 1) {
                                // snapshotFlow is conflated by default
                                if (job?.isActive == true) return@collect
                                job = launch {
                                    var latestFlow: Job? = null
                                    var latestTransformer: Job? = null
                                    snapshotFlow { upObserveBitmap.value() }
                                        .collect flow@ { flow ->
                                            latestFlow?.cancel()
                                            latestFlow = launch {
                                                flow.collect {
                                                    latestTransformer?.cancel()
                                                    if (it == null) {
                                                        returns.value = null
                                                        return@collect
                                                    }
                                                    latestTransformer = launch(Dispatchers.Default) {
                                                        val gen = Palette
                                                            .from(it).maximumColorCount(16).generate()
                                                        ensureActive()
                                                        returns.value = gen
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }

                onDispose { supervisor.cancel() }
            }
        )

        return remember(this) {
            {
                flow {
                    readerCountState.value++
                    try {
                        snapshotFlow { returns.value }.collect(this)
                    } finally {
                        readerCountState.value--
                    }
                }
            }
        }
    }

    companion object {
        fun Saver(
            state: PlaybackControlScreenState
        ): Saver<PlaybackControlScreenCoordinator, Bundle> {
            return Saver(
                save = { self ->
                    Bundle()
                        .apply {
                            val prop = self::layoutCoordinator
                            putBundle(
                                prop.name, prop.get().saveAsBundle()
                            )
                        }
                },
                restore = { bundle ->
                    PlaybackControlScreenCoordinator(
                        state
                    ).apply {
                        layoutCoordinator.restore(bundle.getBundle(::layoutCoordinator.name)!!)
                    }
                }
            )
        }
    }
}

class PlaybackControlScreenLayoutCoordinator(
    private val state: PlaybackControlScreenState,
) {

    private var mainPlacement by mutableStateOf<MainPlacement?>(null)
    private var queuePlacement by mutableStateOf<QueuePlacement?>(null)

    @SnapshotRead
    fun currentRenderedHeightPx(): Int {
        return mainPlacement?.transitionState?.renderedHeightPx ?: 0
    }

    @SnapshotRead
    fun currentRenderedWidthPx(): Int {
        return mainPlacement?.transitionState?.renderedWidthPx ?: 0
    }

    @SnapshotRead
    fun currentStagedHeightPx(): Int {
        return mainPlacement?.transitionState?.stagedHeightPx ?: 0
    }

    @SnapshotRead
    fun currentStagedWidthPx(): Int {
        return mainPlacement?.transitionState?.stagedWidthPx ?: 0
    }

    @SnapshotRead
    fun currentTargetHeightPx(): Int {
        return mainPlacement?.transitionState?.targetHeightPx ?: 0
    }

    @SnapshotRead
    fun currentTargetWidthPx(): Int {
        return mainPlacement?.transitionState?.targetWidthPx ?: 0
    }

    fun show(): Boolean {
        Timber.d(
            "player.root.main.Screen.kt: PlaybackControlScreenLayoutCoordinator@${System.identityHashCode(this)}_hide()"
        )
        return mainPlacement?.transitionState?.apply { show() } != null
    }

    fun hide(): Boolean {
        Timber.d(
            "player.root.main.Screen.kt: PlaybackControlScreenLayoutCoordinator@${System.identityHashCode(this)}_hide()"
        )
        return mainPlacement?.transitionState?.apply { hide() } != null
    }

    data class MainPlacement(
        val composable: @Composable () -> Unit,
        val transitionState: MainContainerTransitionState
    )

    data class QueuePlacement(
        val composable: @Composable () -> Unit,
        val transitionState: QueueContainerTransitionState
    )

    @Composable
    fun PlaceLayout(
        main: MainPlacement,
        queue: QueuePlacement,
    ) {
        this.mainPlacement = main
        this.queuePlacement = queue
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            main.composable()
            queue.composable()
        }
    }

    companion object {
        fun PlaybackControlScreenLayoutCoordinator.saveAsBundle(): Bundle {
            return Bundle()
                .apply {

                }
        }

        fun PlaybackControlScreenLayoutCoordinator.restore(bundle: Bundle) {

        }
    }
}
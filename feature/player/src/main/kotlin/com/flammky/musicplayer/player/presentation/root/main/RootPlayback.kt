package com.flammky.musicplayer.player.presentation.root.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlCompact
import com.flammky.musicplayer.player.presentation.root.rememberRootPlaybackControlCompactState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import kotlin.time.Duration

@Composable
fun rememberRootPlaybackControlState(
    user: User,
    viewModelStoreOwner: ViewModelStoreOwner
): RootPlaybackControlState {
    val viewModel = hiltViewModel<PlaybackControlViewModel>(viewModelStoreOwner)
    return rememberSaveable(
        user, viewModel,
        saver = object : Saver<RootPlaybackControlState, Bundle> {

            override fun restore(value: Bundle): RootPlaybackControlState {
                return RootPlaybackControlState(user, viewModel).apply { restore(value) }
            }

            override fun SaverScope.save(value: RootPlaybackControlState): Bundle {
                return value.saveAsBundle()
            }
        }
    ) {
        RootPlaybackControlState(user, viewModel)
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun RootPlaybackControl(
    state: RootPlaybackControlState
) {
    val coordinator = remember(state) { PlaybackControlCoordinator(state) }

    coordinator.ComposeLayout(
        screen = @Composable {
            PlaybackControlScreen(
                state = rememberPlaybackControlScreenState(
                    intents = remember(intentsKey) {
                        PlaybackControlScreenIntents(
                            requestSeekNextAsync = requestSeekNextAsync,
                            requestSeekPreviousAsync = requestSeekPreviousAsync,
                            requestSeekNextWithExpectAsync = requestSeekNextWithExpectAsync,
                            requestSeekPreviousWithExpectAsync = requestSeekPreviousWithExpectAsync,
                            requestSeekAsync = requestSeekAsync,
                            requestSeekPositionAsync = requestSeekPositionAsync,
                            requestMoveQueueItemAsync = requestMoveQueueItemAsync,
                            requestPlayAsync = requestPlayAsync,
                            requestPauseAsync = requestPauseAsync,
                            requestToggleRepeatAsync = requestToggleRepeatAsync,
                            requestToggleShuffleAsync = requestToggleShuffleAsync,
                            dismiss = dismiss
                        )
                    },
                    source = remember(sourceKey) {
                        PlaybackControlScreenDataSource(
                            user = user,
                            observeQueue = observeQueue,
                            observePlaybackProperties = observePlaybackProperties,
                            observeDuration = observeDuration,
                            observePositionWithIntervalHandle = observePositionWithIntervalHandle,
                            observeTrackMetadata = observeTrackMetadata,
                            observeArtwork = observeArtwork
                        )
                    },
                    backPressRegistry = backPressRegistry
                ).apply {
                    remember(showSelf) {
                        if (showSelf) requestShow() else requestHide()
                    }
                }
            )
        },
        compact = @Composable {
            RootPlaybackControlCompact(
                rememberRootPlaybackControlCompactState(
                    user = user,
                    onBaseClicked = onBaseClicked,
                    onArtworkClicked = onArtworkClicked,
                ).apply {
                    bottomSpacing = with(LocalDensity.current) { bottomSpacingState.value.toDp() }
                    coordinator.updateCompactTopPositionRelativeToAnchor(
                        with(LocalDensity.current) { topPositionRelativeToAnchor.roundToPx() }
                    )
                }
            )
        }
    )
}


class RootPlaybackControlState internal constructor(
    internal val user: User,
    internal val viewModel: PlaybackControlViewModel
) {

    private var bottomBarHeightPx = 0
    private var currentCoordinator by mutableStateOf<PlaybackControlCoordinator?>(null)

    val hasBackPressConsumer by derivedStateOf { currentCoordinator?.hasBackPressConsumer == true }

    val compactTopPositionFromAnchor
        get() = currentCoordinator?.compactTopPositionRelativeToAnchor ?: 0

    fun consumeBackPress() {
        currentCoordinator?.consumeBackPress()
    }

    fun restore(
        bundle: Bundle
    ) {
    }

    fun saveAsBundle(): Bundle {
        return Bundle()
            .apply {

            }
    }

    fun updateBottomBarHeight(height: Int) {
        this.bottomBarHeightPx = height
        currentCoordinator?.updateBottomBarHeight(bottomBarHeightPx)
    }

    internal fun updateCoordinator(
        cord: PlaybackControlCoordinator
    ) {
        this.currentCoordinator = cord.apply {
            updateBottomBarHeight(bottomBarHeightPx)
        }
    }
}

internal class PlaybackControlCoordinator(
    private val state: RootPlaybackControlState
) {

    private val user: User = state.user

    private val playbackController: PlaybackController =
        state.viewModel.createUserPlaybackController(state.user)

    private val observeArtwork: (String) -> Flow<Any?> =
        state.viewModel::observeMediaArtwork

    private val observeTrackMetadata: (String) -> Flow<MediaMetadata?> =
        state.viewModel::observeMediaMetadata

    private val coroutineScope: CoroutineScope =
        CoroutineScope(AndroidUiDispatcher.Main + SupervisorJob())

    private val coroutineDispatchScope: CoroutineScope =
        CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val layoutCoordinator = PlaybackControlLayoutCoordinator()

    private var currentScreenScope by mutableStateOf<ScreenScopeImpl?>(null)

    var bottomBarSpacing by mutableStateOf(0)
        private set

    var compactTopPositionRelativeToAnchor by mutableStateOf(0)
        private set

    val hasBackPressConsumer by derivedStateOf {
        val screen = currentScreenScope?.state?.hasBackPressConsumer() == true
        screen
    }

    fun updateBottomBarHeight(height: Int) {
        Timber.d("player.presentation.root.main.RootPlayback.kt PlaybackControlCoordinator@${System.identityHashCode(this)}.updateBottomBarHeight($height)")
        this.bottomBarSpacing = height
    }

    fun updateCompactTopPositionRelativeToAnchor(pos: Int) {
        this.compactTopPositionRelativeToAnchor = pos
    }

    fun consumeBackPress() {
        currentScreenScope?.state?.backPress()
    }

    @Composable
    fun ComposeLayout(
        screen: @Composable ScreenRenderScope.() -> Unit,
        compact: @Composable CompactRenderScope.() -> Unit
    ) {
        state.updateCoordinator(this)
        val upScreen = rememberUpdatedState(screen)
        val upCompact = rememberUpdatedState(compact)
        val screenScope = presentScreen()
        val compactScope = presentCompact(screenScope)
        with(layoutCoordinator) {
            PlaceLayout(
                screen = remember(screenScope) {
                    PlaybackControlLayoutCoordinator.ScreenPlacement {
                        with(upScreen) {
                            screenScope.observeRenderScope().value()
                        }
                    }
                },
                compact = remember(compactScope) {
                    PlaybackControlLayoutCoordinator.CompactPlacement {
                        with(upCompact) {
                            compactScope.value()
                        }
                    }
                }
            )
        }
    }

    private fun updateScreenScope(
        scope: ScreenScopeImpl
    ) {
        if (currentScreenScope == scope) return
        currentScreenScope = scope
    }

    interface ScreenRenderScope {
        val showSelf: Boolean
        val sourceKey: Any
        val intentsKey: Any
        val user: User
        val backPressRegistry: BackPressRegistry
        val dismiss: () -> Unit
        val requestSeekNextAsync: () -> Deferred<Result<Boolean>>
        val requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>
        val requestSeekNextWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>
        val requestSeekPreviousWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>
        val requestSeekAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectToIndex: Int,
            expectToId: String
        ) -> Deferred<Result<Boolean>>
        val requestSeekPositionAsync: (
            expectFromId: String,
            expectFromDuration: Duration,
            percent: Float
        ) -> Deferred<Result<Boolean>>
        val requestMoveQueueItemAsync: (
            from: Int,
            expectFromId: String,
            to: Int,
            expectToId: String
        ) -> Deferred<Result<Boolean>>
        val requestPlayAsync: () -> Deferred<Result<Boolean>>
        val requestPauseAsync: () -> Deferred<Result<Boolean>>
        val requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>
        val requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>
        val observeQueue: () -> Flow<OldPlaybackQueue>
        val observePlaybackProperties: () -> Flow<PlaybackProperties>
        val observeDuration: () -> Flow<Duration>
        val observePositionWithIntervalHandle: (
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ) -> Flow<Duration>
        val observeTrackMetadata: (String) -> Flow<MediaMetadata?>
        val observeArtwork: (String) -> Flow<Any?>
    }

    interface CompactRenderScope {
        val user: User
        val onBaseClicked: (() -> Unit)
        val onArtworkClicked: (() -> Unit)
        val bottomSpacingState: State<Int>
    }

    private class ScreenState() {

        val backPressRegistry = BackPressRegistry()

        var showSelf by mutableStateOf(false)
            private set

        fun showSelf() {
            Timber.d(
                "player.root.main.RootPlayback.kt: PlaybackControlCoordinator_ScreenState@${System.identityHashCode(this)}_showSelf()"
            )
            showSelf = true
        }

        fun dismiss() {
            Timber.d(
                "player.root.main.RootPlayback.kt: PlaybackControlCoordinator_ScreenState@${System.identityHashCode(this)}_dismiss()"
            )
            showSelf = false
        }

        fun hasBackPressConsumer(): Boolean {
            return backPressRegistry.hasBackPressConsumer()
        }

        fun backPress() {
            backPressRegistry.consumeBackPress()
        }
    }


    private class ScreenScopeImpl(
        private val user: User,
        private val playbackController: PlaybackController,
        private val observeArtwork: (String) -> Flow<Any?>,
        private val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
        private val coroutineScope: CoroutineScope,
        private val coroutineDispatchScope: CoroutineScope,
    ) {

        val state = ScreenState()

        @Composable
        fun observeRenderScope(): ScreenRenderScopeImpl {
            return ScreenRenderScopeImpl(
                showSelf = state.showSelf,
                sourceKey = this,
                intentsKey = this,
                backPressRegistry = state.backPressRegistry,
                user = user,
                dismiss = state::dismiss,
                requestSeekNextAsync = ::requestSeekNextAsync,
                requestSeekPreviousAsync = ::requestSeekPreviousAsync,
                requestSeekNextWithExpectAsync = ::requestSeekNextWithExpectAsync,
                requestSeekPreviousWithExpectAsync = ::requestSeekPreviousWithExpectAsync,
                requestSeekAsync = ::requestSeekAsync,
                requestSeekPositionAsync = ::requestSeekPositionAsync,
                requestMoveQueueItemAsync = ::requestMoveQueueItemAsync,
                requestPlayAsync = ::requestPlayAsync,
                requestPauseAsync = ::requestPauseAsync,
                requestToggleRepeatAsync = ::requestToggleRepeatAsync,
                requestToggleShuffleAsync = ::requestToggleShuffleAsync,
                observeQueue = ::observeQueue,
                observePlaybackProperties = ::observePlaybackProperties,
                observeDuration = ::observeDuration,
                observePositionWithIntervalHandle = ::observePositionWithIntervalHandle,
                observeTrackMetadata = observeTrackMetadata,
                observeArtwork = observeArtwork,
            )
        }

        private fun requestSeekNextAsync(): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekNextAsync(Duration.ZERO).await()
                        .run {
                            eventDispatch?.join()
                            success
                        }
                }
            }
        }

        private fun requestSeekNextWithExpectAsync(
            expectCurrentIndex: Int,
            expectCurrentId: String,
            expectNextId: String
        ): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekAsync(
                        expectCurrentIndex,
                        expectCurrentId,
                        expectCurrentIndex + 1,
                        expectNextId
                    ).await().run {
                        eventDispatch?.join()
                        success
                    }
                }
            }
        }

        private fun requestSeekPreviousAsync(): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekPreviousAsync(Duration.ZERO).await()
                        .run {
                            eventDispatch?.join()
                            success
                        }
                }
            }
        }

        private fun requestSeekPreviousWithExpectAsync(
            expectCurrentIndex: Int,
            expectCurrentId: String,
            expectPreviousId: String
        ): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekAsync(
                        expectCurrentIndex,
                        expectCurrentId,
                        expectCurrentIndex - 1,
                        expectPreviousId
                    ).await().run {
                        eventDispatch?.join()
                        success
                    }
                }
            }
        }

        private fun requestSeekPositionAsync(
            expectFromId: String,
            expectFromDuration: Duration,
            percent: Float
        ): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekPositionAsync(
                        expectFromId,
                        expectFromDuration,
                        percent
                    ).await().run {
                        eventDispatch?.join()
                        success
                    }
                }
            }
        }

        private fun requestMoveQueueItemAsync(
            from: Int,
            expectFromId: String,
            to: Int,
            expectToId: String
        ): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekAsync(
                        from,
                        expectFromId,
                        to,
                        expectToId
                    ).await().run {
                        eventDispatch?.join()
                        success
                    }
                }
            }
        }

        private fun requestSeekAsync(
            expectFromIndex: Int,
            expectFromId: String,
            expectToIndex: Int,
            expectToId: String
        ): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSeekAsync(
                        expectFromIndex,
                        expectFromId,
                        expectToIndex,
                        expectToId
                    ).await().run {
                        eventDispatch?.join()
                        success
                    }
                }
            }
        }

        private fun requestPlayAsync(): Deferred<Result<Boolean>> {
            Timber.d("player.root.main.RootPlayback.kt: ScreenScopeImpl_requestPlayAsync()")
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestPlayAsync()
                        .await()
                        .run {
                            eventDispatch?.join()
                            success
                        }
                }
            }
        }

        private fun requestPauseAsync(): Deferred<Result<Boolean>> {
            Timber.d("player.root.main.RootPlayback.kt: ScreenScopeImpl_requestPauseAsync()")
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestSetPlayWhenReadyAsync(false)
                        .await()
                        .run {
                            eventDispatch?.join()
                            success
                        }
                }
            }
        }

        private fun requestToggleRepeatAsync(): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestToggleRepeatModeAsync()
                        .await()
                        .run {
                            eventDispatch?.join()
                            success
                        }
                }
            }
        }

        private fun requestToggleShuffleAsync(): Deferred<Result<Boolean>> {
            return coroutineDispatchScope.async {
                runCatching {
                    playbackController.requestToggleShuffleModeAsync()
                        .await()
                        .run {
                            eventDispatch?.join()
                            success
                        }
                }
            }
        }

        private fun observeQueue(): Flow<OldPlaybackQueue> {
            return flow {
                val observer = playbackController.createPlaybackObserver()
                try {
                    observer.createQueueCollector().apply {
                        startCollect().join()
                        queueStateFlow.collect(this@flow)
                    }
                } finally {
                    observer.dispose()
                }
            }
        }

        private fun observePlaybackProperties(): Flow<PlaybackProperties> {
            return flow {
                val observer = playbackController.createPlaybackObserver()
                try {
                    observer.createPropertiesCollector().apply {
                        startCollect().join()
                        propertiesStateFlow.collect(this@flow)
                    }
                } finally {
                    observer.dispose()
                }
            }
        }

        private fun observeDuration(): Flow<Duration> {
            return flow {
                val observer = playbackController.createPlaybackObserver()
                try {
                    observer.createDurationCollector()
                        .apply {
                            startCollect().join()
                        }
                        .run {
                            durationStateFlow.collect(this@flow)
                        }
                } finally {
                    observer.dispose()
                }
            }
        }

        private fun observePositionWithIntervalHandle(
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ): Flow<Duration> {
            return flow {
                val observer = playbackController.createPlaybackObserver()
                try {
                    observer.createProgressionCollector().apply {
                        setIntervalHandler(getInterval)
                        startCollectPosition().join()
                        positionStateFlow.collect(this@flow)
                    }
                } finally {
                    observer.dispose()
                }
            }
        }
    }

    private class ScreenRenderScopeImpl(
        override val sourceKey: Any,
        override val intentsKey: Any,
        override val user: User,
        override val showSelf: Boolean,
        override val backPressRegistry: BackPressRegistry,
        override val dismiss: () -> Unit,
        override val observeArtwork: (String) -> Flow<Any?>,
        override val observePlaybackProperties: () -> Flow<PlaybackProperties>,
        override val observeDuration: () -> Flow<Duration>,
        override val observePositionWithIntervalHandle: (
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ) -> Flow<Duration>,
        override val observeQueue: () -> Flow<OldPlaybackQueue>,
        override val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
        override val requestMoveQueueItemAsync: (
            from: Int,
            expectFromId: String,
            to: Int,
            expectToId: String
        ) -> Deferred<Result<Boolean>>,
        override val requestPauseAsync: () -> Deferred<Result<Boolean>>,
        override val requestPlayAsync: () -> Deferred<Result<Boolean>>,
        override val requestSeekAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectToIndex: Int,
            expectToId: String
        ) -> Deferred<Result<Boolean>>,
        override val requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
        override val requestSeekNextWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>,
        override val requestSeekPositionAsync: (
            expectFromId: String,
            expectFromDuration: Duration,
            percent: Float
        ) -> Deferred<Result<Boolean>>,
        override val requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
        override val requestSeekPreviousWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>,
        override val requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>,
        override val requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>
    ) : ScreenRenderScope

    private class CompactRenderScopeImpl(
        override val user: User,
        override val onBaseClicked: (() -> Unit),
        override val onArtworkClicked: (() -> Unit),
        override val bottomSpacingState: State<Int>
    ) : CompactRenderScope

    @Composable
    private fun presentScreen(): ScreenScopeImpl {
        val screenImpl = rememberSaveable(
            this,
            saver = remember {
                Saver(
                    save = { self ->
                        Bundle()
                            .apply { putBoolean("show", self.state.showSelf) }
                    },
                    restore = { bundle ->
                        ScreenScopeImpl(
                            user,
                            playbackController,
                            observeArtwork,
                            observeTrackMetadata,
                            coroutineScope,
                            coroutineDispatchScope
                        ).apply {
                            if (bundle.getBoolean("show")) state.showSelf() else state.dismiss()
                        }
                    }
                )
            }
        ) {
            ScreenScopeImpl(
                user,
                playbackController,
                observeArtwork,
                observeTrackMetadata,
                coroutineScope,
                coroutineDispatchScope
            )
        }
        currentScreenScope = screenImpl
        return screenImpl
    }

    @Composable
    private fun presentCompact(
        screen: ScreenScopeImpl
    ): CompactRenderScopeImpl {
        return CompactRenderScopeImpl(
            user,
            screen.state::showSelf,
            screen.state::showSelf,
            remember(this) { derivedStateOf { bottomBarSpacing } }
        )
    }
}

class PlaybackControlLayoutCoordinator() {

    data class ScreenPlacement(
        val composable: @Composable () -> Unit
    )

    data class CompactPlacement(
        val composable: @Composable () -> Unit
    )

    @Composable
    fun PlaceLayout(
        screen: ScreenPlacement,
        compact: CompactPlacement
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            compact.composable()
            screen.composable()
        }
    }
}
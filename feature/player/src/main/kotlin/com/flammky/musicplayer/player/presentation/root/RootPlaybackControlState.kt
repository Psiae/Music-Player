package com.flammky.musicplayer.player.presentation.root

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KMutableProperty
import kotlin.time.Duration

class RootPlaybackControlState internal constructor(
    internal val user: User,
    internal val viewModel: PlaybackControlViewModel
) {

    internal var currentComposition: RootPlaybackControlComposition? = null

    private var restorationBundle: Bundle? = null
    private var compositionRestorationBundle: Bundle? = null
    private var pendingRestore = false
    private var restored = false

    internal val freezeState = mutableStateOf(false)
    internal val showSelfState = mutableStateOf(false)

    internal var dismissHandle: (() -> Boolean)? = null

    internal val coordinator = RootPlaybackControlCoordinator(
        state = this,
        playbackController = viewModel.createUserPlaybackController(user),
        observeArtwork = viewModel::observeMediaArtwork,
        observeTrackMetadata = viewModel::observeMediaMetadata,
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
        coroutineDispatchScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    )

    /**
     * state on whether the Layout is `visually` visible, false means that it's fully hidden
     */
    val isVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition
            ?.let { currentScope ->
                currentScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { _ -> currentScope.visibleHeight > 0 }
            } == true
    } @SnapshotRead get

    /**
     * state on whether the Layout is `visually` fully visible
     */
    val isFullyVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition
            ?.let { currentScope ->
                currentScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { target -> currentScope.visibleHeight >= target }
            } == true
    } @SnapshotRead get

    /**
     * state on whether the Layout is `visually` visible, false means that it's fully hidden
     */
    val queueScreenVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition?.queueComposition
            ?.let { queueScope ->
                queueScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { _ -> queueScope.visibleHeight > 0 }
            } == true
    } @SnapshotRead get

    /**
     * state on whether the Layout is `visually` fully visible
     */
    val queueScreenFullyVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition?.queueComposition
            ?.let { queueScope ->
                queueScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { target -> queueScope.visibleHeight >= target }
            } == true
    } @SnapshotRead get

    val consumeBackPress by derivedStateOf(policy = structuralEqualityPolicy()) {
        showSelfState.value || currentComposition?.consumeBackPress == true
    }

    @Composable
    fun Compose(
        modifier: Modifier = Modifier
    ) {
        check(restored)
        RootPlaybackControl(modifier = modifier, state = this)
    }

    internal fun restoreComposition(
        composition: RootPlaybackControlComposition
    ) {
        compositionRestorationBundle?.let {
            compositionRestorationBundle = null
            composition.performRestore(it)
        }
    }

    fun showSelf() {
        showSelfState.value = true
    }

    fun dismiss() {
        if (dismissHandle?.invoke() != false) {
            showSelfState.value = false
        }
    }

    fun backPress() {
        // TODO: Define back press consumer
        if (currentComposition?.consumeBackPress() == true) {
            return
        }
        dismiss()
    }

    internal fun performRestore(bundle: Bundle) {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = bundle
        compositionRestorationBundle = bundle.getBundle(RootPlaybackControlComposition::class.simpleName)
        pendingRestore = true
    }

    internal fun performPendingRestore(
        initialFreezeState: Boolean,
        initialShowSelfState: Boolean
    ) {
        if (restored) {
            check(!pendingRestore)
            return
        }
        check(pendingRestore)
        showSelfState.value = initialShowSelfState
        freezeState.value = initialFreezeState
        restored = true
        pendingRestore = false
    }

    internal fun skipRestore() {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = null
        pendingRestore = true
    }

    companion object {
        internal fun saver(
            user: User,
            viewModel: PlaybackControlViewModel
        ): Saver<RootPlaybackControlState, Bundle> = Saver(
            save = { state ->
                Bundle()
                    .apply {
                        state.currentComposition?.let {
                            putBundle(it::class.simpleName, it.performSave())
                        }
                    }
            },
            restore = { bundle ->
                RootPlaybackControlState(user, viewModel)
                    .apply {
                        performRestore(bundle)
                    }
            }
        )
    }
}

internal class RootPlaybackControlCoordinator(
    private val state: RootPlaybackControlState,
    private val playbackController: PlaybackController,
    private val observeArtwork: (String) -> Flow<Any?>,
    private val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatchScope: CoroutineScope,
) {

    val layoutComposition: RootPlaybackControlComposition

    init {
        @Suppress("SENSELESS_COMPARISON")
        check(state.coordinator == null) {
            "State Layout Coordinator should only be created by the state"
        }
        layoutComposition = RootPlaybackControlComposition(
            showSelf = state.showSelfState::value,
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
            dismiss = state::dismiss
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

@Composable
fun rememberRootPlaybackControlState(
    user: User,
    initialFreezeState: Boolean,
    initialShowSelfState: Boolean,
    dismissHandle: (() -> Boolean)? = null
): RootPlaybackControlState {
    val viewModel = hiltViewModel<PlaybackControlViewModel>()
    return rememberSaveable(
        saver = RootPlaybackControlState.saver(user, viewModel)
    ) init@ {
        RootPlaybackControlState(user, viewModel)
            .apply {
                this.freezeState.value = initialFreezeState
                this.showSelfState.value = initialShowSelfState
                this.dismissHandle = dismissHandle
                skipRestore()
            }
    }.apply {
        this.dismissHandle = dismissHandle
        performPendingRestore(
            initialFreezeState,
            initialShowSelfState
        )
    }
}

internal class RootPlaybackControlComposition(
    val showSelf: @SnapshotRead () -> Boolean,
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
    val dismiss: () -> Unit
) {

    private var queueCompositionRestorationBundle: Bundle? = null

    internal var rememberFullyTransitioned by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentQueue by mutableStateOf(OldPlaybackQueue.UNSET)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentQueueReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPlaybackMetadata by mutableStateOf<MediaMetadata?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentMetadataReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPlaybackBitmap by mutableStateOf<Bitmap?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentBitmapReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentBitmapReaderBiggestViewport by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPlaybackPalette by mutableStateOf<Palette?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPaletteReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var showPlaybackQueue by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite private set

    // should this be nullable tho ?
    var queueComposition by mutableStateOf<RootPlaybackControlQueueScope?>(null)
        @SnapshotRead get
        @SnapshotWrite internal set

    var fullyVisibleHeightTarget by mutableStateOf(-1)
        @SnapshotRead get
        @SnapshotWrite internal set

    var visibleHeight by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite internal set

    val consumeBackPress by derivedStateOf {
        showPlaybackQueue
    }

    internal fun performSave(): Bundle {
        return Bundle()
            .apply {
                val showPlaybackQueueRef: KMutableProperty<Boolean> = ::showPlaybackQueue
                putBoolean(::rememberFullyTransitioned.name, rememberFullyTransitioned)
                putBoolean(showPlaybackQueueRef.name, showPlaybackQueue)
                queueComposition?.let {
                    putBundle(it::class.simpleName, it.performSave())
                }
            }
    }

    internal fun performRestore(bundle: Bundle) {
        bundle
            .run {
                val showPlaybackQueueRef: KMutableProperty<Boolean> = ::showPlaybackQueue
                rememberFullyTransitioned = getBoolean(::rememberFullyTransitioned.name)
                showPlaybackQueue = getBoolean(showPlaybackQueueRef.name, showPlaybackQueue)
                queueCompositionRestorationBundle =
                    getBundle(RootPlaybackControlQueueScope::class.simpleName)
            }
    }

    internal fun showPlaybackQueue() {
        showPlaybackQueue = true
    }

    internal fun dismissPlaybackQueue() {
        showPlaybackQueue = false
    }

    internal fun consumeBackPress(): Boolean {
        if (showPlaybackQueue) {
            showPlaybackQueue = false
            return true
        }
        return false
    }

    internal fun restoreQueueComposition(
        composition: RootPlaybackControlQueueScope
    ) {
        queueCompositionRestorationBundle?.let {
            queueCompositionRestorationBundle = null
            composition.performRestore(it)
        }
    }

    companion object {
        // Saver ?
    }
}

internal class RootPlaybackControlQueueScope(
    private val mainComposition: RootPlaybackControlComposition,
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
    ) -> Deferred<Result<Boolean>>
) {
    val currentQueue
        get() = mainComposition.currentQueue

    var fullyVisibleHeightTarget by mutableStateOf(-1)
        @SnapshotRead get
        @SnapshotWrite internal set

    var visibleHeight by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite internal set

    var playbackControlContentPadding by mutableStateOf(PaddingValues(0.dp))
        @SnapshotRead get
        @SnapshotWrite internal set

    var statusBarInset by mutableStateOf(0.dp)
        @SnapshotRead get
        @SnapshotWrite internal set

    val columnVisibilityContentPadding by derivedStateOf {
        val pc = playbackControlContentPadding
        PaddingValues(
            top = maxOf(pc.calculateTopPadding(), statusBarInset),
            bottom = maxOf(pc.calculateBottomPadding())
        )
    }

    val showSelf
        get() = mainComposition.showPlaybackQueue

    internal var rememberFullyTransitionedState by mutableStateOf(false)

    fun performSave(): Bundle {
        return Bundle()
            .apply {
                putBoolean(::rememberFullyTransitionedState.name, rememberFullyTransitionedState)
            }
    }

    fun performRestore(bundle: Bundle) {
        rememberFullyTransitionedState = bundle.getBoolean(::rememberFullyTransitionedState.name)
    }

    fun incrementQueueReaderCount() {
        mainComposition.currentQueueReaderCount++
    }

    fun decrementQueueReaderCount() {
        mainComposition.currentQueueReaderCount--
    }

    fun observeTrackMetadata(id: String) = mainComposition.observeTrackMetadata(id)
    fun observeTrackArtwork(id: String) = mainComposition.observeArtwork(id)

    companion object {
        // Saver ?
    }
}
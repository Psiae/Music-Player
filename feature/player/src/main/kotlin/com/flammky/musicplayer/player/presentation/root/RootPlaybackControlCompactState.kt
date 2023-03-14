package com.flammky.musicplayer.player.presentation.root

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.isDarkAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantContentColorAsState
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class RootPlaybackControlCompactState internal constructor(
    internal val playbackController: PlaybackController,
    val onObserveMetadata: (String) -> Flow<MediaMetadata?>,
    val onObserveArtwork: (String) -> Flow<Any?>,
    val coroutineScope: CoroutineScope,
    val coroutineDispatchScope: CoroutineScope,
) {

    @Suppress("JoinDeclarationAndAssignment")
    internal val coordinator: ControlCompactCoordinator

    var height by mutableStateOf<Dp>(55.dp)
        @SnapshotRead get
        @SnapshotWrite set

    var width by mutableStateOf<Dp>(Dp.Unspecified)
        @SnapshotRead get
        @SnapshotWrite set

    /**
     * the bottom offset for the coordinator to apply
     */
    var bottomSpacing by mutableStateOf<Dp>(0.dp)
        @SnapshotRead get
        @SnapshotWrite set

    var artworkClickedHandle by mutableStateOf<(() -> Unit)?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    var baseClickedHandle by mutableStateOf<(() -> Unit)?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    /**
     * mutable freeze state for the coordinator to apply, when set to true the layout will no longer
     * collect remote updates nor dispatch user request (dropped)
     */
    var freeze by mutableStateOf<Boolean>(false)
        @SnapshotRead get
        @SnapshotWrite set

    val topPositionRelativeToParent
        @SnapshotRead get() = coordinator.topPositionRelativeToParent

    val topPositionRelativeToAnchor
        @SnapshotRead get() = coordinator.topPositionRelativeToAnchor

    init {
        // I guess we should provide lambdas as opposed to `this`
        coordinator = ControlCompactCoordinator(this, coroutineScope, coroutineDispatchScope)
    }

    internal fun dispose() {
        playbackController.dispose()
        coroutineScope.cancel()
        coroutineDispatchScope.cancel()
    }
}

internal class ControlCompactCoordinator(
    private val state: RootPlaybackControlCompactState,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatchScope: CoroutineScope,
) {

    val coordinatorSupervisorJob = SupervisorJob()

    val layoutComposition = ControlCompactComposition(
        getLayoutHeight = @SnapshotRead state::height::get,
        getLayoutWidth = @SnapshotRead state::width::get,
        getLayoutBottomOffset = @SnapshotRead state::bottomSpacing::get,
        getArtworkClickedHandle = @SnapshotRead state::artworkClickedHandle::get,
        getBaseClickedHandle = @SnapshotRead state::baseClickedHandle::get,
        observeMetadata = state.onObserveMetadata,
        observeArtwork = state.onObserveArtwork,
        observePlaybackQueue = ::observeControllerPlaybackQueue,
        observePlaybackProperties = ::observeControllerPlaybackProperties,
        setPlayWhenReady = ::setPlayWhenReady,
        observeProgressWithIntervalHandle = ::observeProgressWithIntervalHandle,
        observeBufferedProgressWithIntervalHandle = ::observeBufferedProgressWithIntervalHandle,
        observeDuration = ::observeDuration,
        requestPlaybackMoveNext = ::requestPlaybackMoveNext,
        requestPlaybackMovePrevious = ::requestPlaybackMovePrevious,
        coroutineScope = coroutineScope
    )

    val topPositionRelativeToParent
        get() = layoutComposition.topPosition

    val topPositionRelativeToAnchor
        get() = layoutComposition.topPositionFromAnchor

    private var queueReaderCount by mutableStateOf(0)
    private var propertiesReaderCount by mutableStateOf(0)

    var remoteQueueSnapshot by mutableStateOf(OldPlaybackQueue.UNSET)
        private set

    var remotePlaybackPropertiesSnapshot by mutableStateOf(PlaybackProperties.UNSET)
        private set

    val freeze by derivedStateOf { state.freeze }

    private fun observeControllerPlaybackProperties(): Flow<PlaybackProperties> {
        return flow {
            propertiesReaderCount++
            try {
                snapshotFlow { remotePlaybackPropertiesSnapshot }
                    .collect(this)
            }  finally {
                propertiesReaderCount--
            }
        }
    }

    private fun observeControllerPlaybackQueue(): Flow<OldPlaybackQueue> {
        return flow {
            queueReaderCount++
            try {
                snapshotFlow { remoteQueueSnapshot }
                    .collect(this)
            } finally {
                queueReaderCount--
            }
        }
    }

    private fun setPlayWhenReady(
        play: Boolean,
        joinCollectorDispatch: Boolean
    ): Deferred<Result<Boolean>> {
        return coroutineDispatchScope.async {
            runCatching {
                val result = state.playbackController.requestSetPlayWhenReadyAsync(play).await()
                if (joinCollectorDispatch) {
                    result.eventDispatch?.join()
                }
                result.success
            }
        }
    }

    private fun observeProgressWithIntervalHandle(
        getNextInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ): Flow<Duration> {
        return flow {
            val observer = state.playbackController.createPlaybackObserver()
            val collector = observer.createProgressionCollector()
            try {
                collector
                    .apply {
                        setIntervalHandler { _, p, b, d, s ->
                            getNextInterval(p, d, s)
                        }
                        startCollectPosition().join()
                    }
                    .run {
                        positionStateFlow.collect(this@flow)
                    }
            } finally {
                observer.dispose()
            }
        }
    }

    private fun observeBufferedProgressWithIntervalHandle(
        getNextInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ): Flow<Duration> {
        return flow {
            val observer = state.playbackController.createPlaybackObserver()
            val collector = observer.createProgressionCollector()
            try {
                collector
                    .apply {
                        setIntervalHandler { _, p, b, d, s ->
                            getNextInterval(p, d, s)
                        }
                        startCollectPosition().join()
                    }
                    .run {
                        bufferedPositionStateFlow.collect(this@flow)
                    }
            } finally {
                observer.dispose()
            }
        }
    }

    private fun observeDuration(): Flow<Duration> {
        return flow {
            val observer = state.playbackController.createPlaybackObserver()
            val collector = observer.createDurationCollector()
            try {
                collector
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

    private fun requestPlaybackMoveNext(
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectNextIndex: Int,
        expectNextId: String
    ): Deferred<Result<Boolean>> {
        return coroutineDispatchScope.async {
            runCatching {
                if (expectFromIndex + 1 != expectNextIndex) {
                    return@runCatching false
                }
                state.playbackController.requestSeekAsync(
                    expectFromIndex,
                    expectFromId,
                    expectNextIndex,
                    expectNextId
                ).await().run {
                    eventDispatch?.join()
                    success
                }
            }
        }
    }

    private fun requestPlaybackMovePrevious(
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectNextIndex: Int,
        expectNextId: String
    ): Deferred<Result<Boolean>> {
        return coroutineDispatchScope.async {
            runCatching {
                if (expectFromIndex - 1 != expectNextIndex) {
                    return@runCatching false
                }
                state.playbackController.requestSeekAsync(
                    expectFromIndex,
                    expectFromId,
                    expectNextIndex,
                    expectNextId
                ).await().run {
                    eventDispatch?.join()
                    success
                }
            }
        }
    }

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @Composable
        inline fun ControlCompactCoordinator.PrepareCompositionInline() {
            // I don't think these should be composable, but that's for later
            ComposeRemoteQueueReaderObserver()
            ComposeRemotePropertiesReaderObserver()
            DisposableEffect(
                key1 = this,
                effect = {
                    // TODO: assert that this state can only be used within a single composition
                    //  tree at a time (acquire slot)
                    onDispose {
                        // TODO: release slot
                    }
                }
            )
        }

        @Composable
        private inline fun ControlCompactCoordinator.ComposeRemoteQueueReaderObserver() {
            LaunchedEffect(
                key1 = this,
                block = {
                    launch(coordinatorSupervisorJob) {
                        var latestCollectorJob: Job? = null
                        snapshotFlow { queueReaderCount }
                            .map {
                                check(it >= 0)
                                it > 0
                            }
                            .distinctUntilChanged()
                            .collect { hasActiveReader ->
                                if (!hasActiveReader) {
                                    remoteQueueSnapshot = OldPlaybackQueue.UNSET
                                    latestCollectorJob?.cancel()
                                    return@collect
                                }
                                check(latestCollectorJob?.isActive != true)
                                latestCollectorJob = launch {
                                    var collectWithFreezeHandle: Job? = null
                                    snapshotFlow { state.freeze }
                                        .collect { freeze ->
                                            if (freeze) {
                                                collectWithFreezeHandle?.cancel()
                                                return@collect
                                            }
                                            check(collectWithFreezeHandle?.isActive != true)
                                            collectWithFreezeHandle = launch {
                                                val observer = state.playbackController.createPlaybackObserver()
                                                try {
                                                    val collector = observer.createQueueCollector()
                                                        .apply { startCollect().join() }
                                                    collector.queueStateFlow
                                                        .collect {
                                                            remoteQueueSnapshot = it
                                                        }
                                                } finally {
                                                    observer.dispose()
                                                }
                                            }
                                        }
                                }
                            }
                    }.join()
                }
            )
        }

        @Composable
        private inline fun ControlCompactCoordinator.ComposeRemotePropertiesReaderObserver() {
            LaunchedEffect(
                key1 = this,
                block = {
                    launch(coordinatorSupervisorJob) {
                        var latestCollectorJob: Job? = null
                        snapshotFlow { propertiesReaderCount }
                            .map {
                                check(it >= 0)
                                it > 0
                            }
                            .distinctUntilChanged()
                            .collect { hasActiveReader ->
                                if (!hasActiveReader) {
                                    remotePlaybackPropertiesSnapshot = PlaybackProperties.UNSET
                                    latestCollectorJob?.cancel()
                                    return@collect
                                }
                                check(latestCollectorJob?.isActive != true)
                                latestCollectorJob = launch {
                                    var collectWithFreezeHandle: Job? = null
                                    snapshotFlow { state.freeze }
                                        .collect { freeze ->
                                            if (freeze) {
                                                collectWithFreezeHandle?.cancel()
                                                return@collect
                                            }
                                            check(collectWithFreezeHandle?.isActive != true)
                                            collectWithFreezeHandle = launch {
                                                val observer = state.playbackController.createPlaybackObserver()
                                                try {
                                                    val collector = observer.createPropertiesCollector()
                                                        .apply { startCollect().join() }
                                                    collector.propertiesStateFlow
                                                        .collect {
                                                            remotePlaybackPropertiesSnapshot = it
                                                        }
                                                } finally {
                                                    observer.dispose()
                                                }
                                            }
                                        }
                                }
                            }
                    }.join()
                }
            )
        }
    }
}

class CompactControlTransitionState(
    private val getLayoutHeight: @SnapshotRead () -> Dp,
    private val getLayoutWidth: @SnapshotRead () -> Dp,
    private val getLayoutBottomSpacing: @SnapshotRead () -> Dp,
    private val observeQueue: () -> Flow<OldPlaybackQueue>
) {

    val applier: Applier = Applier(this)

    private var showSelf by mutableStateOf(false)

    companion object {

        @Composable
        internal fun CompactControlTransitionState.getLayoutOffset(
            constraints: Constraints,
            density: Density
        ): DpOffset {
            return remember {
                val override = getLayoutHeight()
                Animatable(
                    initialValue = with(density) {
                        if (override == Dp.Unspecified) {
                            (constraints.maxHeight).toDp()
                        } else {
                            override.coerceIn(0.dp, (constraints.maxHeight).toDp())
                        }
                    },
                    typeConverter = Dp.VectorConverter,
                )
            }.apply {
                LaunchedEffect(
                    key1 = this,
                    block = {
                        var latestAnimateJob: Job? = null
                        snapshotFlow { showSelf }
                            .collect { show ->
                                latestAnimateJob?.cancel()
                                latestAnimateJob = launch {
                                    if (show) {
                                        snapshotFlow { getLayoutBottomSpacing().unaryMinus() }
                                            .collect {
                                                Timber.d("RootPlaybackControlCompact_Transition: animateTo: $it")
                                                animateTo(it, tween(200))
                                            }
                                    } else {
                                        snapshotFlow { getLayoutHeight() }
                                            .collect {
                                                Timber.d("RootPlaybackControlCompact_Transition: animateTo: $it")
                                                animateTo(it, tween(200))
                                            }
                                    }
                                }
                            }
                    }
                )
            }.run {
                DpOffset(0.dp, value)
            }
        }

        @Composable
        internal fun CompactControlTransitionState.getLayoutWidth(
            constraints: Constraints
        ): Dp {
            val override = getLayoutWidth()
            return with(LocalDensity.current) {
                if (override == Dp.Unspecified) {
                    (constraints.maxWidth).toDp() - 15.dp
                } else {
                    override.coerceIn(0.dp, (constraints.maxWidth).toDp())
                }
            }
        }

        @Composable
        internal fun CompactControlTransitionState.getLayoutHeight(
            constraints: Constraints
        ): Dp {
            val override = getLayoutHeight()
            return with(LocalDensity.current) {
                if (override == Dp.Unspecified) {
                    55.dp
                } else {
                    override.coerceIn(0.dp, (constraints.maxHeight).toDp())
                }
            }
        }
    }

    class Applier(
        private val state: CompactControlTransitionState
    ) {
        companion object {
            @Composable
            fun Applier.PrepareComposition() {
                val coroutineScope = rememberCoroutineScope()
                DisposableEffect(
                    key1 = this,
                    effect = {
                        val queueListener = coroutineScope.launch {
                            state.observeQueue()
                                .collect { queue ->
                                    state.showSelf = queue.list.getOrNull(queue.currentIndex) != null
                                }
                        }
                        onDispose {
                            queueListener.cancel()
                        }
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalPagerApi::class)
class CompactControlPagerState(
    val layoutState: PagerState,
    val observeMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val requestPlaybackMovePrevious: (
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousIndex: Int,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestPlaybackMoveNext: (
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousIndex: Int,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val isSurfaceDark: @SnapshotRead () -> Boolean
) {

    val applier = Applier(this)

    class Applier(private val state: CompactControlPagerState) {
        companion object {

            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                observeForScope()
                    .apply {
                        content()
                        // assume that invoking the content means the pager layout is recomposed
                        DoLayoutComposedWork(this@ComposeLayout)
                    }
            }

            @Composable
            private fun Applier.observeForScope(): CompositionScope {
                val composableCoroutineScope = rememberCoroutineScope()
                val state = remember {
                    mutableStateOf(
                        CompositionScope(
                            state.layoutState,
                            OldPlaybackQueue.UNSET,
                            CoroutineScope(
                                composableCoroutineScope.coroutineContext +
                                        SupervisorJob(composableCoroutineScope.coroutineContext.job)
                            ),
                        )
                    )
                }.apply {
                    DisposableEffect(
                        key1 = this,
                        effect = {
                            val supervisor = SupervisorJob(composableCoroutineScope.coroutineContext.job)
                            composableCoroutineScope.launch(supervisor) {
                                try {
                                    state.observeQueue()
                                        .collect { queue ->
                                            value.lifetimeCoroutineScope.cancel()
                                            val lifetimeCoroutineScope = CoroutineScope(
                                                currentCoroutineContext() + SupervisorJob(supervisor)
                                            )
                                            value = CompositionScope(state.layoutState, queue, lifetimeCoroutineScope)
                                        }
                                } finally {
                                    value.lifetimeCoroutineScope.cancel()
                                }
                            }
                            onDispose { supervisor.cancel() }
                        }
                    )
                }

                return state.value
            }

            @Composable
            private fun CompositionScope.DoLayoutComposedWork(self: Applier) {
                DisposableEffect(
                    key1 = this,
                    effect = {
                        val supervisor = SupervisorJob()
                        val pageCorrectionJob = lifetimeCoroutineScope.launch(supervisor) {
                            val targetIndex = queueData.currentIndex.coerceAtLeast(0)
                            val spread =
                                (queueData.currentIndex - 2) ..(queueData.currentIndex + 2)
                            val expectScrollFromPage = layoutState.currentPage
                            val isDirectionToLeft = expectScrollFromPage > targetIndex
                            launch {
                                try {
                                    if (layoutState.currentPage in spread) {
                                        layoutState.animateScrollToPage(targetIndex)
                                    } else {
                                        layoutState.scrollToPage(targetIndex)
                                    }
                                    awaitPageCorrected()
                                    onPageFullyCorrected()
                                } catch (cce: CancellationException) {
                                    onPageFullCorrectionCancelled()
                                }
                            }
                            var latestCollectedPage = -1
                            snapshotFlow { layoutState.currentPage }
                                .first { page ->
                                    if (latestCollectedPage == -1) {
                                        check(page == expectScrollFromPage)
                                    } else {
                                        if (isDirectionToLeft) {
                                            check(page < latestCollectedPage)
                                        } else {
                                            check(page > latestCollectedPage)
                                        }
                                    }
                                    latestCollectedPage = page
                                    latestCollectedPage == targetIndex
                                }
                            onPageCorrected()
                        }
                        val pageSupervisorJob = lifetimeCoroutineScope.launch(supervisor) {
                            awaitPageCorrected()
                            val targetPage = queueData.currentIndex.coerceAtLeast(0)
                            snapshotFlow { layoutState.currentPage }
                                .onStart {
                                    check(layoutState.currentPage == targetPage)
                                    currentPageInCompositionSpan = targetPage
                                    onPageSupervisorInstalled()
                                }
                                .collect { page ->
                                    currentPageInCompositionSpan = page
                                }
                        }
                        // should we install listener on every page change instead ?
                        val userDragListenerJob = lifetimeCoroutineScope.launch(supervisor) {
                            val stack = mutableListOf<UserDragInstance>()
                            try {
                                layoutState.interactionSource.interactions
                                    .onStart { onDragSupervisorInstalled() }
                                    .collect { interaction ->
                                        Log.d("RootCompactPager", "drag: collected=$interaction")
                                        if (interaction !is DragInteraction) return@collect
                                        when (interaction) {
                                            is DragInteraction.Start -> {
                                                val dragInstance = UserDragInstance(
                                                    interaction,
                                                    currentPageInCompositionSpan
                                                )
                                                val scrollInstance = UserScrollInstance(dragInstance)
                                                stack.add(dragInstance)
                                                userDraggingLayout = true
                                                userScrollInstance = scrollInstance
                                                lifetimeCoroutineScope.launch {
                                                    scrollInstance.inLifetime {
                                                        snapshotFlow { currentPageInCompositionSpan }
                                                            .collect { page ->
                                                                scrollInstance.onScrollOverPage(page)
                                                            }
                                                    }
                                                }
                                                lifetimeCoroutineScope.launch {
                                                    try {
                                                        scrollInstance.inLifetime {
                                                            snapshotFlow { layoutState.isScrollInProgress }
                                                                .first { !it }
                                                        }
                                                        scrollInstance.onScrollStopped()
                                                    } catch (ce: CancellationException) {
                                                        scrollInstance.onScrollInterrupted()
                                                    }
                                                }
                                            }
                                            is DragInteraction.Stop -> {
                                                var indexToRemove: Int? = null
                                                run {
                                                    stack.forEachIndexed { index, userDragInstance ->
                                                        if (userDragInstance.start === interaction.start) {
                                                            indexToRemove = index
                                                            return@run
                                                        }
                                                    }
                                                }
                                                indexToRemove?.let { i ->
                                                    stack.removeAt(i).apply {
                                                        check(start === interaction.start)
                                                        onStopped()
                                                    }
                                                    if (stack.isEmpty()) userDraggingLayout = false
                                                }
                                            }
                                            is DragInteraction.Cancel -> {
                                                var indexToRemove: Int? = null
                                                run {
                                                    stack.forEachIndexed { index, userDragInstance ->
                                                        if (userDragInstance.start === interaction.start) {
                                                            indexToRemove = index
                                                            return@run
                                                        }
                                                    }
                                                }
                                                indexToRemove?.let { i ->
                                                    stack.removeAt(i).apply {
                                                        check(start === interaction.start)
                                                        onCancelled()
                                                    }
                                                    if (stack.isEmpty()) userDraggingLayout = false
                                                }
                                            }
                                        }
                                    }
                            } finally {
                                stack.forEach { it.onCancelled() }
                            }
                        }
                        val userSwipeListenerJob = lifetimeCoroutineScope.launch(supervisor) {
                            snapshotFlow { userScrollInstance }
                                .filterNotNull()
                                .collect { scroll ->
                                    Log.d("RootCompactPager", "swipe: collectedScrollInstance=$scroll")
                                    val next = runCatching {
                                        scroll.inLifetime {
                                            runCatching {
                                                scroll.drag.inLifetime { awaitCancellation() }
                                            }
                                            snapshotFlow { scroll.currentPage }
                                                .first { page ->
                                                    page != scroll.drag.startPage
                                                }
                                        }
                                    }.getOrElse { -1 }
                                    Log.d("RootCompactPager", "swipe: next=$next")
                                    launch {
                                        val success = when (next) {
                                            scroll.drag.startPage + 1 -> {
                                                self.state.requestPlaybackMoveNext(
                                                    queueData,
                                                    scroll.drag.startPage,
                                                    queueData.list[scroll.drag.startPage],
                                                    next,
                                                    queueData.list[next]
                                                ).await().getOrElse { false }
                                            }
                                            scroll.drag.startPage - 1 -> {
                                                self.state.requestPlaybackMovePrevious(
                                                    queueData,
                                                    scroll.drag.startPage,
                                                    queueData.list[scroll.drag.startPage],
                                                    next,
                                                    queueData.list[next]
                                                ).await().getOrElse { false }
                                            }
                                            else -> false
                                        }
                                        Log.d("RootCompactPager", "swipe: success=$success")
                                        if (!success) {
                                            layoutState.stopScroll(MutatePriority.PreventUserInput)
                                            snapshotFlow { userDraggingLayout }.first { !it }
                                            layoutState.scrollToPage(queueData.currentIndex)
                                        }
                                    }
                                }
                        }
                        lifetimeCoroutineScope.launch(supervisor) {
                            awaitPageCorrected()
                            awaitPageSupervisor()
                            awaitDragSupervisor()
                            onUserInteractionListenerInstalled()
                            userScrollEnabled = true
                        }
                        onDispose { supervisor.cancel() }
                    }
                )
            }
        }
    }

    class UserDragInstance(
        val start: DragInteraction.Start,
        val startPage: Int
    ) {

        val lifetime = SupervisorJob()

        fun onStopped()  {
            check(lifetime.isActive)
            lifetime.cancel()
        }

        fun onCancelled() {
            check(lifetime.isActive)
            lifetime.cancel()
        }

        suspend fun inLifetime(
            block: suspend () -> Unit
        ) {
            withContext(lifetime) {
                block()
            }
        }
    }

    class UserScrollInstance(
        val drag: UserDragInstance
    ) {
        private val nextInstance = CompletableDeferred<UserScrollInstance>()
        private val lifetime = SupervisorJob()
        private val completion = SupervisorJob()

        var currentPage by mutableStateOf(drag.startPage)
            private set

        fun onScrollStopped() {
            check(lifetime.isActive)
            check(completion.isActive)
            completion.complete()
            lifetime.cancel()
        }

        fun onScrollInterrupted() {
            check(lifetime.isActive)
            check(completion.isActive)
            completion.cancel()
            lifetime.cancel()
        }

        fun onNextScroll(next: UserScrollInstance) {
            check(next !== this)
            check(!completion.isActive)
            check(lifetime.isCompleted)
            check(nextInstance.isActive)
            nextInstance.complete(next)
        }

        fun onScrollOverPage(page: Int) {
            check(lifetime.isActive)
            currentPage = page
        }

        suspend fun awaitNextScroll(): UserScrollInstance = nextInstance.await()

        suspend fun <R> inLifetime(
            block: suspend () -> R
        ): R {
            return withContext(lifetime) {
                block()
            }
        }
    }

    class CompositionScope(
        val layoutState: PagerState,
        val queueData: OldPlaybackQueue,
        val lifetimeCoroutineScope: CoroutineScope,
    ) {
        private val userInteractionListenerInstallationJob = Job()
        private val pageSupervisorInstallationJob = Job()
        private val dragSupervisorInstallationJob = Job()


        var userDraggingLayout by mutableStateOf(false)
        var userScrollEnabled by mutableStateOf(false)
        var currentPageInCompositionSpan by mutableStateOf(-1)
        var userScrollInstance by mutableStateOf<UserScrollInstance?>(null)

        private val pageCorrectionJob = Job()
        private val pageFullCorrectionJob = Job()

        fun onPageCorrected() {
            check(pageCorrectionJob.isActive)
            pageCorrectionJob.complete()
        }

        fun onPageFullyCorrected() {
            check(pageFullCorrectionJob.isActive)
            pageFullCorrectionJob.complete()
        }

        fun onPageFullCorrectionCancelled() {
            check(pageFullCorrectionJob.isActive)
            pageFullCorrectionJob.cancel()
        }

        fun onPageSupervisorInstalled() {
            check(pageSupervisorInstallationJob.isActive)
            pageSupervisorInstallationJob.complete()
        }

        fun onDragSupervisorInstalled() {
            check(dragSupervisorInstallationJob.isActive)
            dragSupervisorInstallationJob.complete()
        }

        fun onUserInteractionListenerInstalled() {
            check(pageCorrectionJob.isCompleted)
            check(pageSupervisorInstallationJob.isCompleted)
            check(dragSupervisorInstallationJob.isCompleted)
            check(userInteractionListenerInstallationJob.isActive)
            userInteractionListenerInstallationJob.complete()
        }

        suspend fun awaitPageCorrected() {
            pageCorrectionJob.join()
        }
        suspend fun awaitUserInteractionListener() {
            userInteractionListenerInstallationJob.join()
        }
        suspend fun awaitPageSupervisor() {
            pageSupervisorInstallationJob.join()
        }
        suspend fun awaitDragSupervisor() {
            dragSupervisorInstallationJob.join()
        }
    }
}

class CompactButtonControlsState(
    private val observePlaybackProperties: () -> Flow<PlaybackProperties>,
    private val setPlayWhenReady: (play: Boolean, joinCollectorDispatch: Boolean) -> Deferred<Result<Boolean>>,
    private val isSurfaceDark: @SnapshotRead () -> Boolean
) {

    val applier = Applier(this)

    class Applier(private val state: CompactButtonControlsState) {
        companion object {

            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                observeForScope()
                    .run {
                        content()
                    }
            }

            @Composable
            private fun Applier.observeForScope(): CompositionScope {
                val composableCoroutineScope = rememberCoroutineScope()
                val latestScopeState = remember {
                    mutableStateOf(
                        CompositionScope(
                            PlaybackProperties.UNSET,
                            CoroutineScope(SupervisorJob()),
                            state.setPlayWhenReady,
                            state.isSurfaceDark
                        )
                    )
                }
                DisposableEffect(
                    key1 = this,
                    effect = {
                        check(state.applier === this@observeForScope)
                        val supervisor = SupervisorJob()
                        composableCoroutineScope.launch(supervisor) {
                            state.observePlaybackProperties()
                                .collect {
                                    Timber.d("CompactControlState, collectedProperties: $it")
                                    latestScopeState.value.onReplaced()
                                    latestScopeState.value =
                                        CompositionScope(
                                            it,
                                            CoroutineScope(
                                                composableCoroutineScope.coroutineContext + SupervisorJob(supervisor)
                                            ),
                                            state.setPlayWhenReady,
                                            state.isSurfaceDark
                                        )
                                }
                        }
                        onDispose {
                            latestScopeState.value.onDisposedByComposer()
                            supervisor.cancel()
                        }
                    }
                )
                return latestScopeState.value
            }
        }
    }


    class CompositionScope(
        private val properties: PlaybackProperties,
        private val lifetimeCoroutineScope: CoroutineScope,
        private val setPlayWhenReady: (
            play: Boolean,
            joinCollectorDispatch: Boolean
        ) -> Deferred<Result<Boolean>>,
        val isSurfaceDark: @SnapshotRead () -> Boolean
    ) {

        var allowPlayPauseInteraction by mutableStateOf(true)

        fun Modifier.playButtonInteractionModifier(
            interactionSource: MutableInteractionSource? = null,
            indication: Indication? = null,
        ): Modifier {
            return composed {
                clickable(
                    interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                    indication = indication,
                    enabled = allowPlayPauseInteraction,
                    onClickLabel = null,
                    role = Role.Button
                ) {
                    if (!allowPlayPauseInteraction) return@clickable
                    allowPlayPauseInteraction = false
                    lifetimeCoroutineScope.launch {
                        setPlayWhenReady(!properties.playWhenReady, true).await()
                        allowPlayPauseInteraction = true
                    }
                }
            }
        }

        fun onDisposedByComposer() {
            lifetimeCoroutineScope.cancel()
        }

        fun onReplaced() {
            lifetimeCoroutineScope.cancel()
        }

        companion object {

            @Composable
            fun CompositionScope.showPlayButton(): Boolean {
                return !properties.playWhenReady
            }
        }
    }
}

class CompactControlTimeBarState(
    private val observeProgressWithIntervalHandle: (
        getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ) -> Flow<Duration>,
    private val observeBufferedProgressWithIntervalHandle: (
        getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ) -> Flow<Duration>,
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val observeDuration: () -> Flow<Duration>
) {

    val applier = Applier(this)

    class Applier(private val state: CompactControlTimeBarState) {

        companion object {

            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                check(this === state.applier)
                observeForScope()
                    .apply {
                        content()
                        DoLayoutComposedWork(this)
                    }
            }


            @Composable
            private fun Applier.observeForScope(): CompositionScope {
                val composableCoroutineScope = rememberCoroutineScope()
                val latestScopeState = remember {
                    mutableStateOf(
                        CompositionScope(
                            CoroutineScope(
                                composableCoroutineScope.coroutineContext + SupervisorJob()
                            ),
                            OldPlaybackQueue.UNSET,
                            state.observeProgressWithIntervalHandle,
                            state.observeBufferedProgressWithIntervalHandle,
                            state.observeDuration
                        )
                    )
                }
                DisposableEffect(
                    key1 = this,
                    effect = {
                        composableCoroutineScope.launch {
                            state.observeQueue()
                                .collect { queue ->
                                    latestScopeState.value.onReplaced()
                                    latestScopeState.value = CompositionScope(
                                        CoroutineScope(
                                            composableCoroutineScope.coroutineContext + SupervisorJob()
                                        ),
                                        queue,
                                        state.observeProgressWithIntervalHandle,
                                        state.observeBufferedProgressWithIntervalHandle,
                                        state.observeDuration
                                    )
                                }
                        }
                        onDispose { latestScopeState.value.onDisposedByComposer() }
                    }
                )
                return latestScopeState.value
            }

            @Composable
            private fun Applier.DoLayoutComposedWork(
                composition: CompositionScope
            ) {

            }
        }
    }

    class CompositionScope(
        val lifetimeCoroutineScope: CoroutineScope,
        val queueData: OldPlaybackQueue,
        private val observeProgressWithIntervalHandle: (
            getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
        ) -> Flow<Duration>,
        private val observeBufferedProgressWithIntervalHandle: (
            getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
        ) -> Flow<Duration>,
        private val observeDuration: () -> Flow<Duration>
    ) {

        fun onDisposedByComposer() {
            lifetimeCoroutineScope.cancel()
        }

        fun onReplaced() {
            lifetimeCoroutineScope.cancel()
        }

        companion object {

            @Composable
            fun CompositionScope.positionTimeBarValue(width: Dp): Float {
                val animatable = remember(this) {
                    Animatable(0f)
                }
                val latestWidth = rememberUpdatedState(newValue = width)

                LaunchedEffect(
                    key1 = this,
                    block = {
                        var latestCollector: Job? = null
                        snapshotFlow { latestWidth.value }
                            .collect {
                                latestCollector?.cancel()
                                latestCollector = lifetimeCoroutineScope.launch {
                                    observeDuration().collect { duration ->
                                        if (duration <= Duration.ZERO) {
                                            animatable.animateTo(0f, tween(100))
                                            return@collect
                                        }
                                        observeProgressWithIntervalHandle { progress, duration, speed ->
                                            if (duration == Duration.ZERO ||
                                                speed == 0f
                                            ) {
                                                PlaybackConstants.DURATION_UNSET
                                            } else {
                                                (duration.inWholeMilliseconds / it.value / speed)
                                                    .toLong()
                                                    .takeIf { it > 100 }?.milliseconds
                                                    ?: PlaybackConstants.DURATION_UNSET
                                            }
                                        }.collect { position ->
                                            if (position <= Duration.ZERO) {
                                                animatable.animateTo(0f, tween(100))
                                            }
                                            animatable.animateTo(
                                                (position / duration)
                                                    .toFloat()
                                                    .coerceAtLeast(0f),
                                                tween(100)
                                            )
                                        }
                                    }
                                }
                            }
                    }
                )

                return animatable.value
            }

            @Composable
            fun CompositionScope.bufferedPositionTimeBarValue(width: Dp): Float {
                val animatable = remember(this) {
                    Animatable(0f)
                }
                val latestWidth = rememberUpdatedState(newValue = width)
                LaunchedEffect(
                    key1 = this,
                    block = {
                        var latestCollector: Job? = null
                        snapshotFlow { latestWidth.value }
                            .collect {
                                latestCollector?.cancel()
                                latestCollector = lifetimeCoroutineScope.launch {
                                    observeDuration().collect { duration ->
                                        if (duration <= Duration.ZERO) {
                                            animatable.animateTo(0f, tween(100))
                                            return@collect
                                        }
                                        observeBufferedProgressWithIntervalHandle { progress, duration, speed ->
                                            if (duration == Duration.ZERO ||
                                                speed == 0f
                                            ) {
                                                PlaybackConstants.DURATION_UNSET
                                            } else {
                                                (duration.inWholeMilliseconds / it.value / speed)
                                                    .toLong()
                                                    .takeIf { it > 100 }?.milliseconds
                                                    ?: PlaybackConstants.DURATION_UNSET
                                            }
                                        }.collect { position ->
                                            if (position <= Duration.ZERO) {
                                                animatable.animateTo(0f, tween(100))
                                            }
                                            animatable.animateTo(
                                                (position / duration)
                                                    .toFloat()
                                                    .coerceAtLeast(0f),
                                                tween(100)
                                            )
                                        }
                                    }
                                }
                            }
                    }
                )

                return animatable.value
            }
        }
    }
}

class CompactControlBackgroundState(
    private val observeArtwork: (String) -> Flow<Any?>,
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val coroutineScope: CoroutineScope,
    private val onComposingBackgroundColor: (Color) -> Unit,
    private val getBackgroundClickedHandle: () -> (() -> Unit)?
) {

    val applier = Applier(this)

    private var palette by mutableStateOf<Palette?>(null)

    fun Modifier.backgroundModifier(): Modifier {
        return composed {
            val backgroundColor = @Composable {
                val darkTheme = Theme.isDarkAsState().value
                val compositeOver = Theme.surfaceVariantColorAsState().value
                val factor = 0.7f
                val paletteGen = palette
                    .run {
                        remember(this, darkTheme) {
                            if (this is Palette) {
                                if (darkTheme) {
                                    getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
                                } else {
                                    getVibrantColor(getLightMutedColor(getDominantColor(-1)))
                                }
                            } else {
                                -1
                            }
                        }
                    }
                remember(compositeOver, paletteGen) {
                    if (paletteGen == -1) {
                        compositeOver
                    } else {
                        Color(paletteGen).copy(factor).compositeOver(compositeOver)
                    }
                }
            }
            val animatedBackgroundTransitionColor by
                animateColorAsState(targetValue = backgroundColor())
            onComposingBackgroundColor(animatedBackgroundTransitionColor)
            background(animatedBackgroundTransitionColor)
        }
    }

    fun Modifier.interactionModifier(): Modifier {
        return composed {
            getBackgroundClickedHandle()
                ?.let {
                    clickable(onClick = it)
                }
                ?: this
        }
    }

    companion object {

    }

    class Applier(private val state: CompactControlBackgroundState) {

        private var compositionCount by mutableStateOf(0)

        fun launchQueueObserver() {
            check(compositionCount == 1)
            var latestJob: Job? = null
            state.coroutineScope.launch {
                snapshotFlow { compositionCount }
                    .map { it > 0 }
                    .distinctUntilChanged()
                    .collect { active ->
                        latestJob?.cancel()
                        if (active) {
                            latestJob = launch {
                                var latestBitmapObserver: Job? = null
                                state.observeQueue().collect { queue ->
                                    latestBitmapObserver?.cancel()
                                    val id = queue.list.getOrNull(queue.currentIndex)
                                        ?: run {
                                            state.palette = null
                                            return@collect
                                        }
                                    latestBitmapObserver = launch {
                                        state.observeArtwork(id)
                                            .collect { art ->
                                                if (art !is Bitmap) {
                                                    state.palette = null
                                                    return@collect
                                                }
                                                val palette = withContext(Dispatchers.IO) {
                                                    Palette.from(art).maximumColorCount(16).generate()
                                                }
                                                ensureActive()
                                                state.palette = palette
                                            }
                                    }
                                }
                            }
                        } else {
                            state.palette = null
                        }
                    }
            }
        }

        companion object {

            @Composable
            fun Applier.PrepareCompositionInline() {
                DisposableEffect(
                    key1 = this,
                    effect = {
                        check(compositionCount == 0)
                        compositionCount++
                        launchQueueObserver()
                        onDispose {
                            check(compositionCount == 1)
                            compositionCount--
                        }
                    }
                )
            }
        }
    }

    class LayoutData()
}

class CompactControlArtworkState(
    private val observeArtwork: (String) -> Flow<Any?>,
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val getArtworkClickedHandle: () -> (() -> Unit)?
) {

    val applier = Applier(this)

    var latestQueue by mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)

    companion object {

        fun CompactControlArtworkState.getInteractionModifier(): Modifier {
            return Modifier.composed {
                getArtworkClickedHandle()
                    ?.let {
                        clickable(onClick = it)
                    }
                    ?: this
            }
        }

        fun CompactControlArtworkState.getLayoutModifier(imageModel: Any?): Modifier {
            return Modifier.composed {
                this
                    .fillMaxHeight()
                    .aspectRatio(1f, true)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Theme.surfaceVariantContentColorAsState().value)
                    .then(getInteractionModifier())
            }
        }

        @Composable
        fun CompactControlArtworkState.getImageModel(): Any {
            val data = latestQueue.let { queue ->
                val id = queue.list.getOrNull(queue.currentIndex)
                    ?: return@let null
                val flow = remember(id) {
                    observeArtwork(id)
                }
                flow.collectAsState(initial = Unit).value
            }
            val context = LocalContext.current
            return remember(data) {
                ImageRequest.Builder(context)
                    .data(data)
                    .crossfade(true)
                    .build()
            }
        }

        @Composable
        fun CompactControlArtworkState.getContentScale(imageModel: Any?): ContentScale {
            // TODO: The content scale will depends on the image data,
            //  Spotify for example doesn't allow cropping so we must `fit`
            return ContentScale.Crop
        }
    }

    class Applier(private val state: CompactControlArtworkState) {

        companion object {

            @Composable
            fun Applier.PrepareComposition() {
                val coroutineScope = rememberCoroutineScope()
                DisposableEffect(
                    key1 = this,
                    effect = {
                        val job = coroutineScope.launch {
                            state.observeQueue()
                                .collect { queue ->
                                    state.latestQueue = queue
                                }
                        }
                        onDispose { job.cancel() }
                    }
                )
            }
        }
    }

    class LayoutData()

    class CompositionScope {

    }
}
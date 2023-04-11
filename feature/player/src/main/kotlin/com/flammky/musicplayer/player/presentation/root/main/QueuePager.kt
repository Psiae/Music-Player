package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotReader
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.root.runRemember
import com.google.accompanist.pager.*
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext
import coil.request.ImageRequest as CoilImageRequest

@Composable
fun rememberQueuePagerState(
    key: Any,
    observeQueue: () -> Flow<OldPlaybackQueue>,
    observeArtwork: (String) -> Flow<Any?>,
    requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
): QueuePagerState {
    return remember(key) {
        QueuePagerState(
            observeQueue = observeQueue,
            observeArtwork = observeArtwork,
            requestSeekNextWithExpectAsync = requestSeekNextWithExpectAsync,
            requestSeekPreviousWithExpectAsync = requestSeekPreviousWithExpectAsync
        )
    }
}

@Composable
fun QueuePager(
    state: QueuePagerState
) = state.coordinator.ComposeContent(
    pager = @SnapshotRead {
        providePagerStateRenderer { PagerLayout() }
    }
)

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun QueuePagerCoordinator.QueuePagerRenderScope.PagerLayout() {
    val scrollEnabled = userScrollEnabled()
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        count = pageCount(),
        state = pagerState(),
        flingBehavior = flingBehavior(),
        key = { pageKey(page = it) },
        userScrollEnabled = scrollEnabled
    ) { index ->
        PageItem(
            index = index,
            content = @SnapshotRead {
                provideImageStateRenderer {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = coilImageRequest(),
                        contentDescription = "art",
                        contentScale = contentScale()
                    )
                }
            }
        )
    }
}

class QueuePagerState(
    observeQueue: () -> Flow<OldPlaybackQueue>,
    observeArtwork: (String) -> Flow<Any?>,
    requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
) {
    val coordinator = QueuePagerCoordinator(
        observeQueue,
        observeArtwork,
        requestSeekNextWithExpectAsync,
        requestSeekPreviousWithExpectAsync
    )
}

@OptIn(ExperimentalPagerApi::class)
class QueuePagerCoordinator(
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val observeArtwork: (String) -> Flow<Any?>,
    private val requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    private val requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
) {

    private val layoutCoordinator = QueuePagerLayoutCoordinator()

    interface QueuePagerScope {

        fun providePagerStateRenderer(
            content: @Composable QueuePagerRenderScope.() -> Unit
        )
    }

    interface QueuePagerRenderScope {

        @Composable
        fun pagerState(): PagerState

        @Composable
        fun pageCount(): Int

        fun contentAlpha(): Float

        @Composable
        fun flingBehavior(): FlingBehavior

        @SnapshotRead
        fun pageKey(page: Int): Any

        @Composable
        fun userScrollEnabled(): Boolean

        @Composable
        fun PagerScope.PageItem(
            index: Int,
            content: PagerItemScope.() -> Unit
        )
    }

    interface PagerItemScope {

        fun provideImageStateRenderer(image: @Composable () -> Unit)

        @Composable
        fun coilImageRequest(): CoilImageRequest

        @Composable
        fun contentScale(): ContentScale
    }

    private class PagerItemScopeImpl(
        val id: String,
        private val observeArtwork: (String) -> Flow<Any?>,
    ): PagerItemScope {

        var image by mutableStateOf<@Composable () -> Unit>({})
            private set

        override fun provideImageStateRenderer(image: @Composable () -> Unit) {
            this.image = image
        }

        @Composable
        override fun coilImageRequest(): coil.request.ImageRequest {
            val ctx = LocalContext.current
            val data = remember(id) { observeArtwork(id) }.collectAsState(initial = null).value
            return remember(this, data, ctx) {
                coil.request.ImageRequest.Builder(ctx)
                    .data(data)
                    .build()
            }
        }

        @Composable
        override fun contentScale(): ContentScale {
            // TODO: Spotify artworks should be [ContentScale.None]
            return ContentScale.Crop
        }
    }

    private class QueuePagerRenderScopeImpl(
        val layoutCoordinator: QueuePagerLayoutCoordinator,
        val pagerState: PagerState,
        val pageCount: Int,
        val pageKeys: ImmutableList<Any>,
        val mediaIds: ImmutableList<String>,
        val userScrollEnabled: Boolean,
        private val observeArtwork: (String) -> Flow<Any?>,
    ) : QueuePagerRenderScope {

        @Composable
        override fun pagerState(): PagerState = pagerState

        @Composable
        override fun pageCount(): Int = pageCount

        override fun contentAlpha(): Float = 1f

        @OptIn(ExperimentalSnapperApi::class)
        @Composable
        override fun flingBehavior(): FlingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapIndex = PagerDefaults.singlePageSnapIndex
        )

        override fun pageKey(page: Int): Any = pageKeys[page]

        @Composable
        override fun userScrollEnabled(): Boolean = userScrollEnabled

        @Composable
        override fun PagerScope.PageItem(
            index: Int,
            content: @SnapshotReader PagerItemScope.() -> Unit
        ) {
            val upContent = rememberUpdatedState(content)
            val scopeState = remember(
                this@QueuePagerRenderScopeImpl,
                this@PageItem, index
            ) {
                val impl = PagerItemScopeImpl(
                    mediaIds[index],
                    observeArtwork
                )
                derivedStateOf { impl.apply(upContent.value) }
            }
            with(layoutCoordinator) {
                PlacePagerItem @SnapshotRead {
                    val scope = scopeState.value
                    provideLayoutData(image = scope.image)
                }
            }
        }
    }

    class UserDragInstance(
        val startPage: Int,
        val interactionStart: DragInteraction.Start
    ) {
        var currentPage by mutableStateOf(startPage)
        private val completionJob = Job()

        fun onCancelled() {
            check(completionJob.isActive)
            completionJob.cancel()
        }

        fun onStopped() {
            check(completionJob.isActive)
            completionJob.complete()
        }

        fun isStopped(): Boolean {
            return completionJob.isCompleted
        }

        fun isCancelled(): Boolean {
            return completionJob.isCancelled
        }

        suspend fun <R> inLifetime(
            block: suspend () -> R
        ): R {
            val lifetime = Job()
            completionJob.invokeOnCompletion { lifetime.cancel() }
            return withContext(lifetime) { block() }
        }
    }

    class UserSwipeInstance(
        val scroll: UserScrollInstance,
        val requestSeekNextWithExpectAsync: () -> Deferred<Result<Boolean>>,
        val requestSeekPreviousWithExpectAsync: () -> Deferred<Result<Boolean>>,
        val dispatchSnapPageCorrection: () -> Job
    )

    class UserScrollInstance(
        val drag: UserDragInstance
    ) {
        val startPage = drag.startPage
        var currentPage by mutableStateOf(drag.startPage)
        private val completionJob = Job()

        fun isActive(): Boolean {
            return completionJob.isActive
        }

        fun isCancelled(): Boolean {
            return completionJob.isCancelled
        }

        fun isCompleted(): Boolean {
            Timber.d("UserScrollInstance@${System.identityHashCode(this)}, isCompleted ${completionJob.isCompleted}")
            return completionJob.isCompleted
        }

        fun onCancelled() {
            Timber.d("UserScrollInstance@${System.identityHashCode(this)}, onCancelled")
            check(completionJob.isActive)
            completionJob.cancel()
        }

        fun onStopped() {
            Timber.d("UserScrollInstance@${System.identityHashCode(this)}, onStopped")
            check(completionJob.isActive)
            completionJob.complete()
        }

        suspend fun <R> inLifetime(
            block: suspend () -> R
        ): R {
            val lifetime = Job()
            completionJob.invokeOnCompletion { lifetime.cancel() }
            return withContext(lifetime) { block() }
        }
    }

    private class PagerContinuation(
        val ancestor: PagerContinuation?,
        val pagerState: PagerState,
        val pageCount: Int,
        val initialTargetPage: Int,
        val uiCoroutineScope: CoroutineScope,
        val requestSeekNextWithExpectAsync: (expectFromIndex: Int) -> Deferred<Result<Boolean>>,
        val requestSeekPreviousWithExpectAsync: (expectFromIndex: Int) -> Deferred<Result<Boolean>>,
        val userDraggingState: State<Boolean>
    ) {

        private val _renderJob = Job()
        private val lifetime = SupervisorJob()

        val isUserDragging by derivedStateOf { userDraggingState.value }

        var readyForUserScroll by mutableStateOf(false)
            private set

        fun initialPageCorrected() {
            readyForUserScroll = true
        }

        fun disableScroll() {
            readyForUserScroll = false
        }

        fun invokeAfterFirstRender(
            block: () -> Unit
        ) {
            _renderJob.invokeOnCompletion { if (it == null) block() }
        }

        val activeUserScrollInstance = mutableStateListOf<UserScrollInstance>()

        fun onDisposed() {
            lifetime.cancel()
        }

        fun onSucceeded() {
            lifetime.cancel()
        }

        suspend fun <R> withLifetime(
            block: suspend CoroutineScope.() -> R
        ): R = withContext(lifetime) { block() }

        suspend fun onRendered() {
            // TODO: I don't think we should install per continuation
            _renderJob.complete()
        }
    }

    private class PagerContinuationSupervisor(
        private val uiCoroutineScope: CoroutineScope,
    ) {
        private var supervised by mutableStateOf<PagerContinuation?>(null)
        private val lifetime = SupervisorJob()

        private var prepared = false
        private var isUserDragging by mutableStateOf(false)
        private val activeUserScrollInstance = mutableListOf<UserScrollInstance>()

        private var swipeListenerInstalled by mutableStateOf(false)

        fun next(
            pageCount: Int,
            initialTargetPage: Int,
            swipeNext: (Int) -> Deferred<Result<Boolean>>,
            swipePrevious: (Int) -> Deferred<Result<Boolean>>,
        ) {
            check(lifetime.isActive)
            val anc = supervised?.apply { onSucceeded() }
            supervised = PagerContinuation(
                anc,
                anc?.pagerState ?: PagerState(initialTargetPage),
                pageCount,
                initialTargetPage,
                uiCoroutineScope,
                { i -> swipeNext(i) },
                { i -> swipePrevious(i) },
                derivedStateOf { isUserDragging }
            )
            if (!prepared) {
                prepareInitialContinuation()
            }
            prepareNewContinuation()
        }

        fun current(): PagerContinuation {
            return supervised ?:
                PagerContinuation(
                    null,
                    PagerState(0),
                    0,
                    0,
                    CoroutineScope(EmptyCoroutineContext).apply { cancel() },
                    requestSeekNextWithExpectAsync = { CompletableDeferred<Result<Boolean>>().apply { cancel() } },
                    requestSeekPreviousWithExpectAsync = { CompletableDeferred<Result<Boolean>>().apply { cancel() } },
                    derivedStateOf { false },
                )
        }

        fun dispose() {
            lifetime.cancel()
            supervised?.onDisposed()
        }


        private fun prepareInitialContinuation() {
            check(!prepared)
            val pCont = requireNotNull(supervised)
            uiCoroutineScope.launch {
                val dragListenerInstallJob = Job()
                val swipeListenerInstallJob = Job()
                val swipeToSeekIntentListenerInstallJob = Job()
                val swipeChannel = Channel<UserSwipeInstance>(Channel.UNLIMITED)
                launch {
                    installUserDraggingListener(
                        pCont.pagerState,
                        onInstalled = { dragListenerInstallJob.complete() }
                    )
                }
                launch {
                    installUserSwipeListener(
                        swipeChannel,
                        pCont.pagerState,
                        onInstalled = { swipeListenerInstallJob.complete() },
                        onFailure = { error("") }
                    )
                }
                launch {
                    installUserSwipeToSeekIntentListener(
                        swipeChannel,
                        onInstalled = { swipeToSeekIntentListenerInstallJob.complete() },
                        onFailure = { error("") }
                    )
                }
                launch {
                    dragListenerInstallJob.join()
                    swipeListenerInstallJob.join()
                    swipeToSeekIntentListenerInstallJob.join()
                    swipeListenerInstalled = true
                }
            }
            prepared = true
        }

        private fun prepareNewContinuation() {
            val pCont = supervised!!
            val initialPageCorrectionAtPageJob = Job()
            val initialPageCorrectionFullyAtPageJob = Job()
            if (pCont.initialTargetPage == pCont.pagerState.currentPage) {
                pCont.initialPageCorrected()
                return
            }
            pCont.invokeAfterFirstRender {
                uiCoroutineScope.launch { pCont.withLifetime {
                    val targetPage = pCont.initialTargetPage
                    val spread = (pCont.initialTargetPage - 2) ..(pCont.initialTargetPage + 2)
                    supervisorScope {
                        val correctorDispatchJob = Job()
                        val correctorJob = launch(lifetime) {
                            if (isUserDragging) {
                                pCont.pagerState.stopScroll(MutatePriority.PreventUserInput)
                                if (isUserDragging) snapshotFlow { isUserDragging }.first { !it }
                            }
                            check(activeUserScrollInstance.isEmpty())
                            correctorDispatchJob.complete()
                            if (pCont.ancestor != null && pCont.pagerState.currentPage in spread) {
                                pCont.pagerState.animateScrollToPage(targetPage)
                            } else {
                                pCont.pagerState.scrollToPage(targetPage)
                            }
                        }
                        val atPage = launch(lifetime) {
                            correctorDispatchJob.join()
                            snapshotFlow { pCont.pagerState.currentPage }.first { it == targetPage }
                            check(!isUserDragging)
                            pCont.initialPageCorrected()
                        }
                        runCatching {
                            atPage.join()
                        }.onFailure {
                            initialPageCorrectionAtPageJob.cancel()
                        }.onSuccess {
                            initialPageCorrectionAtPageJob.complete()
                            runCatching {
                                correctorJob.join()
                            }.onSuccess {
                                initialPageCorrectionFullyAtPageJob.complete()
                            }.onFailure {
                                initialPageCorrectionFullyAtPageJob.cancel()
                            }
                        }
                    }
                } }
            }
        }


        private suspend fun installUserDraggingListener(
            pagerState: PagerState,
            onInstalled: () -> Unit
        ) {
            val dragStack = mutableListOf<DragInteraction.Start>()
            pagerState.interactionSource.interactions
                .onStart { onInstalled() }
                .collect { drag ->
                    when (drag) {
                        is DragInteraction.Start -> dragStack.add(drag)
                        is DragInteraction.Stop -> dragStack.remove(drag.start)
                        is DragInteraction.Cancel -> dragStack.remove(drag.start)
                        else -> error("received non-drag interaction")
                    }
                    check(dragStack.size <= 1)
                    isUserDragging = dragStack.isNotEmpty()
                }
        }

        private suspend fun installUserSwipeListener(
            swipeChannel: Channel<UserSwipeInstance>,
            pagerState: PagerState,
            onInstalled: () -> Unit,
            onFailure: () -> Unit
        ) {
            var installed = false
            val scrollInstanceStack = mutableListOf<UserScrollInstance>()
            runCatching {
                pagerState.interactionSource.interactions
                    .onStart {
                        installed = true
                        onInstalled()
                    }
                    .collect { interaction ->
                        // pretty sure the pager only emits DragInteraction, maybe assert ?
                        if (interaction !is DragInteraction) {
                            return@collect
                        }
                        val pCont = supervised!!
                        when (interaction) {
                            is DragInteraction.Start -> {
                                Timber.d(
                                    "PagerCont@${System.identityHashCode(this)}, " +
                                            "DragInteraction.Start=$interaction"
                                )
                                check(scrollInstanceStack.size <= 1)
                                check(scrollInstanceStack.none { it.drag.interactionStart == interaction })
                                val drag =
                                    UserDragInstance(
                                        pagerState.currentPage,
                                        interaction
                                    )
                                val scroll =
                                    UserScrollInstance(
                                        drag
                                    )
                                scrollInstanceStack.add(scroll)
                                activeUserScrollInstance.add(scroll)
                                uiCoroutineScope.launch { pCont.withLifetime {
                                    launch {
                                        runCatching {
                                            scroll.drag.inLifetime {
                                                snapshotFlow { pagerState.currentPage }
                                                    .collect {
                                                        scroll.drag.currentPage = it
                                                    }
                                            }
                                        }.onFailure {
                                            if (it !is CancellationException) throw it
                                            Timber.d(
                                                "PagerCont@${System.identityHashCode(this)}, " +
                                                        "DragInteraction.Start=$interaction fail observe drag current page"
                                            )
                                        }
                                    }
                                    launch {
                                        runCatching {
                                            scroll.inLifetime {
                                                snapshotFlow { pagerState.currentPage }
                                                    .collect {
                                                        scroll.currentPage = it
                                                    }
                                            }
                                        }.onFailure {
                                            if (it !is CancellationException) throw it
                                            Timber.d(
                                                "PagerCont@${System.identityHashCode(this)}, " +
                                                        "DragInteraction.Start=$interaction fail observe scroll current page"
                                            )
                                        }
                                    }
                                    launch {
                                        runCatching {
                                            scroll.inLifetime {
                                                snapshotFlow { pagerState.isScrollInProgress }
                                                    .first { !it }
                                                scroll.onStopped()
                                            }
                                        }.onFailure {
                                            if (it !is CancellationException) throw it
                                            Timber.d(
                                                "PagerCont@${System.identityHashCode(this)}, " +
                                                        "DragInteraction.Start=$interaction fail observe scroll progress"
                                            )
                                        }
                                    }
                                    launch {
                                        runCatching {
                                            scroll.drag.inLifetime { awaitCancellation() }
                                        }
                                        runCatching {
                                            scroll.inLifetime {
                                                snapshotFlow { scroll.currentPage }
                                                    .first { it != scroll.startPage }
                                            }
                                        }
                                        ensureActive()
                                        if (!scroll.isCancelled()) {
                                            val swipe = UserSwipeInstance(
                                                scroll,
                                                requestSeekPreviousWithExpectAsync = {
                                                    pCont.requestSeekPreviousWithExpectAsync(scroll.startPage)
                                                },
                                                requestSeekNextWithExpectAsync = {
                                                    pCont.requestSeekNextWithExpectAsync(scroll.startPage)
                                                },
                                                dispatchSnapPageCorrection = {
                                                    uiCoroutineScope.launch {
                                                        pCont.withLifetime {
                                                            pagerState.stopScroll(MutatePriority.PreventUserInput)
                                                            if (isUserDragging) snapshotFlow { isUserDragging }.first { !it }
                                                            check(activeUserScrollInstance.isEmpty())
                                                            pagerState.scrollToPage(pCont.initialTargetPage)
                                                        }
                                                    }
                                                }
                                            )
                                            swipeChannel.send(swipe)
                                        }
                                    }
                                } }
                            }
                            is DragInteraction.Cancel -> {
                                Timber.d(
                                    "PagerCont@${System.identityHashCode(this)}, " +
                                            "DragInteraction.Cancel"
                                )
                                var indexToRemove = -1
                                run {
                                    scrollInstanceStack.forEachIndexed { i, instance ->
                                        if (instance.drag.interactionStart == interaction.start) {
                                            instance.drag.onCancelled()
                                            indexToRemove = i
                                            return@run
                                        }
                                    }
                                }
                                if (indexToRemove >= 0) {
                                    val get = scrollInstanceStack.removeAt(indexToRemove)
                                    activeUserScrollInstance.remove(get)
                                }
                            }
                            is DragInteraction.Stop -> {
                                Timber.d(
                                    "PagerCont@${System.identityHashCode(this)}, " +
                                            "DragInteraction.Stop"
                                )
                                var indexToRemove = -1
                                run {
                                    scrollInstanceStack.forEachIndexed { i, instance ->
                                        if (instance.drag.interactionStart == interaction.start) {
                                            instance.drag.onStopped()
                                            indexToRemove = i
                                            return@run
                                        }
                                    }
                                }
                                if (indexToRemove >= 0) {
                                    val get = scrollInstanceStack.removeAt(indexToRemove)
                                    activeUserScrollInstance.remove(get)
                                }
                            }
                        }
                    }
            }.onFailure {
                if (!installed) onFailure()
                if (it !is CancellationException) throw it
                scrollInstanceStack.forEach { instance ->
                    instance.onCancelled()
                    activeUserScrollInstance.remove(instance)
                }
                scrollInstanceStack.clear()
            }
        }

        private suspend fun installUserSwipeToSeekIntentListener(
            swipeChannel: Channel<UserSwipeInstance>,
            onInstalled: () -> Unit,
            onFailure: () -> Unit
        ) {
            var installed = false
            runCatching {
                swipeChannel
                    .receiveAsFlow()
                    .onStart {
                        installed = true
                        onInstalled()
                    }
                    .collect { swipe ->
                        val scroll = swipe.scroll
                        Timber.d("MainQueuePager: swipe={${scroll.startPage}, ${scroll.currentPage}")
                        when (scroll.currentPage) {
                            scroll.startPage -1 -> {
                                val accepted = swipe.requestSeekPreviousWithExpectAsync()
                                    .await()
                                    .getOrElse { false }
                                if (!accepted) {
                                    swipe.dispatchSnapPageCorrection().join()
                                }
                            }
                            scroll.startPage +1 -> {
                                val accepted = swipe.requestSeekNextWithExpectAsync()
                                    .await()
                                    .getOrElse { false }
                                if (!accepted) {
                                    swipe.dispatchSnapPageCorrection().join()
                                }
                            }
                            else -> swipe.dispatchSnapPageCorrection().join()
                        }
                    }
            }.onFailure {
                if (!installed) onFailure()
                throw it
            }
        }
    }

    private class PagerCompositionSupervisor {
        private var supervised by mutableStateOf<PagerComposition?>(null)
        private val lifetime = SupervisorJob()

        fun next(
            layoutCoordinator: QueuePagerLayoutCoordinator,
            pCont: PagerContinuation,
            queueData: OldPlaybackQueue,
            observeArtwork: (String) -> Flow<Any?>,
        ) {
            supervised = PagerComposition(
                layoutCoordinator,
                pCont,
                queueData,
                observeArtwork,
            )
        }

        fun current(): PagerComposition {
            return supervised
                ?: PagerComposition(
                    QueuePagerLayoutCoordinator(),
                    pCont = PagerContinuation(
                        null,
                        PagerState(0),
                        0,
                        0,
                        CoroutineScope(EmptyCoroutineContext),
                        { CompletableDeferred<Result<Boolean>>().apply { cancel() } },
                        { CompletableDeferred<Result<Boolean>>().apply { cancel() } },
                        derivedStateOf { false },
                    ),
                    queueData = OldPlaybackQueue.UNSET,
                    observeArtwork = { emptyFlow() },
                )
        }

        fun dispose() {
            lifetime.cancel()
            supervised?.onDisposed()
        }
    }

    private class PagerComposition(
        val layoutCoordinator: QueuePagerLayoutCoordinator,
        val pCont: PagerContinuation,
        private val queueData: OldPlaybackQueue,
        private val observeArtwork: (String) -> Flow<Any?>,
    ) {

        private val lifetime = SupervisorJob()

        fun onDisposed() {
            lifetime.cancel()
        }

        fun onSucceeded() {
            lifetime.cancel()
        }

        @Composable
        fun observeRenderScope(): QueuePagerRenderScopeImpl {
            return QueuePagerRenderScopeImpl(
                layoutCoordinator = layoutCoordinator,
                pagerState = pCont.pagerState,
                pageCount = queueData.list.size,
                pageKeys = queueData.list,
                mediaIds = queueData.list,
                userScrollEnabled = pCont.readyForUserScroll,
                observeArtwork,
            )
        }

        @Composable
        fun OnRendered(scope: QueuePagerRenderScopeImpl) {
            Timber.d(
                """
                    RootMainPlaybackControl@${System.identityHashCode(this)}, QueuePager onRendered.
                    proposed count = ${scope.pageCount},
                    rendered = ${scope.pagerState.pageCount}
                    userScrollEnabled = ${scope.userScrollEnabled}
                """.trimIndent()
            )

            val coroutineScope = rememberCoroutineScope()
            DisposableEffect(
                key1 = this,
                effect = {

                    coroutineScope.launch(lifetime) {
                        pCont.onRendered()
                    }

                    onDispose {  }
                }
            )
        }
    }

    class QueuePagerScopeImpl(
        private val layoutCoordinator: QueuePagerLayoutCoordinator,
        private val observeQueue: () -> Flow<OldPlaybackQueue>,
        private val observeArtwork: (String) -> Flow<Any?>,
        private val requestSeekNextWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectNextId: String
        ) -> Deferred<Result<Boolean>>,
        private val requestSeekPreviousWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>,
    ): QueuePagerScope {

        var pagerRenderer by mutableStateOf<@Composable () -> Unit>({})
            private set

        override fun providePagerStateRenderer(
            content: @Composable QueuePagerRenderScope.() -> Unit
        ) {
            pagerRenderer = @Composable {
                observePagerComposition().run {
                    val render = observeRenderScope()
                    render.content()
                    OnRendered(render)
                }
            }
        }

        @Composable
        private fun observePagerComposition(): PagerComposition {

            val uiCoroutineScope = rememberCoroutineScope()

            val pContSupervisor = remember(this) {
                PagerContinuationSupervisor(
                    uiCoroutineScope,
                )
            }

            val pCompSupervisor = remember(this) {
                PagerCompositionSupervisor()
            }

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    uiCoroutineScope.launch(supervisor) {
                        observeQueue().collect { queue ->
                            Timber.d(
                                """
                                    RootMainPlaybackControl, Pager, newQueue=$queue
                                """.trimIndent()
                            )
                            val pCont = pContSupervisor
                                .apply {
                                    next(
                                        pageCount = queue.list.size,
                                        initialTargetPage = queue.currentIndex.coerceAtLeast(0),
                                        swipeNext = { index ->
                                            requestSeekNextWithExpectAsync(
                                                queue.currentIndex,
                                                queue.list[index],
                                                queue.list[index + 1]
                                            )
                                        },
                                        swipePrevious = { index ->
                                            requestSeekPreviousWithExpectAsync(
                                                queue.currentIndex,
                                                queue.list[index],
                                                queue.list[index - 1]
                                            )
                                        }
                                    )
                                }
                                .current()
                            pCompSupervisor.next(
                                layoutCoordinator,
                                pCont,
                                queue,
                                observeArtwork
                            )
                        }
                    }
                    onDispose {
                        supervisor.cancel()
                        pContSupervisor.dispose()
                        pCompSupervisor.dispose()
                    }
                }
            )

            return pCompSupervisor.current()
        }
    }

    @Composable
    fun ComposeContent(
        pager: @SnapshotReader QueuePagerScope.() -> Unit
    ) {
        val upPagerApplier = rememberUpdatedState(pager)
        with(layoutCoordinator) {
            val scopeState = rememberPagerScope()
                .runRemember {
                    derivedStateOf { apply(upPagerApplier.value) }
                }
            PlacePager(
                pager = @SnapshotRead {
                    val scope = scopeState.value
                    provideLayoutData { scope.pagerRenderer() }
                }
            )
        }
    }

    @Composable
    fun rememberPagerScope(): QueuePagerScopeImpl {
        return remember(this) {
            QueuePagerScopeImpl(
                layoutCoordinator,
                observeQueue,
                observeArtwork,
                requestSeekNextWithExpectAsync,
                requestSeekPreviousWithExpectAsync
            )
        }
    }


}

class QueuePagerLayoutCoordinator {

    interface PagerLayoutScope {

        fun provideLayoutData(content: @Composable () -> Unit)
    }

    interface PagerLayoutItemScope {

        fun provideLayoutData(image: @Composable () -> Unit)
    }

    private class PagerLayoutScopeImpl() : PagerLayoutScope {

        var pagerContent by mutableStateOf<@Composable () -> Unit>({})
            private set

        override fun provideLayoutData(content: @Composable () -> Unit) {
            this.pagerContent = content
        }
    }

    private class PagerLayoutItemScopeImpl() : PagerLayoutItemScope {

        var imageContent by mutableStateOf<@Composable () -> Unit>({})
            private set

        override fun provideLayoutData(
            image: @Composable () -> Unit
        ) {
            this.imageContent = image
        }
    }

    @Composable
    fun PlacePager(
        pager: @SnapshotReader PagerLayoutScope.() -> Unit
    ) = BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val upPagerApplier = rememberUpdatedState(pager)
        remember(this@QueuePagerLayoutCoordinator) {
            PagerLayoutScopeImpl()
        }.runRemember {
            derivedStateOf { apply(upPagerApplier.value) }
        }.value.pagerContent()
    }

    @Composable
    fun PlacePagerItem(
        item: @SnapshotReader PagerLayoutItemScope.() -> Unit
    ) = BoxWithConstraints(
        modifier = Modifier
            .height(280.dp)
            .fillMaxWidth(0.8f),
        contentAlignment = Alignment.Center
    ) {
        val scope = remember(this@QueuePagerLayoutCoordinator) {
            PagerLayoutItemScopeImpl()
        }.runRemember(item) {
            derivedStateOf { apply(item) }
        }.value.imageContent()
    }
}
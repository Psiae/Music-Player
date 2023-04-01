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
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.root.runRemember
import com.google.accompanist.pager.*
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext
import coil.request.ImageRequest as CoilImageRequest

@Composable
fun QueuePager(
    state: QueuePagerState
) = state.coordinator.ComposeContent(
    pager = {
        providePagerStateRenderer { PagerLayout() }
    }
)

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun QueuePagerCoordinator.QueuePagerRenderScope.PagerLayout() {
    HorizontalPager(
        count = pageCount(),
        state = pagerState(),
        flingBehavior = flingBehavior(),
        key = { pageKey(page = it) },
        userScrollEnabled = userScrollEnabled()
    ) { index ->
        PageItem(
            index = index,
            content = {
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
            content: PagerItemScope.() -> Unit
        ) {
            val scope = remember(this@QueuePagerRenderScopeImpl, this@PageItem, index) {
                PagerItemScopeImpl(
                    mediaIds[index],
                    observeArtwork
                )
            }.apply(content)
            with(layoutCoordinator) {
                PlacePagerItem {
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
        val pageCount: Int,
        val initialTargetPage: Int,
        private val uiCoroutineScope: CoroutineScope,
        private val requestSeekNextWithExpectAsync: (expectFromIndex: Int) -> Deferred<Result<Boolean>>,
        private val requestSeekPreviousWithExpectAsync: (expectFromIndex: Int) -> Deferred<Result<Boolean>>,
    ) {

        private val lifetime = SupervisorJob()

        private val dragListenerInstallJob = Job()
        private val initialPageCorrectionAtPageJob = Job()
        private val initialPageCorrectionFullyAtPageJob = Job()
        private val swipeListenerInstallJob = Job()
        private val swipeToSeekIntentListenerInstallJob = Job()

        val pagerState: PagerState =
            ancestor?.pagerState ?: PagerState(initialTargetPage)

        var initialPageCorrected by mutableStateOf(false)
            private set

        var prepared by mutableStateOf(false)
            private set

        var isUserDragging by mutableStateOf(false)
            private set

        var readyForUserScroll by mutableStateOf(false)
            private set

        val userSwipeChannel = Channel<UserScrollInstance>(
            capacity = Channel.UNLIMITED,
            onUndeliveredElement = {
                error("Undelivered Element on Unlimited Buffer")
            }
        )

        val activeUserScrollInstance = mutableStateListOf<UserScrollInstance>()

        fun onDisposed() {
            lifetime.cancel()
        }

        fun onSucceeded() {
            lifetime.cancel()
        }

        suspend fun prepare() {
            supervisorScope {
                launch(lifetime) { installIsUserDraggingListener() }
                launch(lifetime) { doInitialLayoutPageCorrection() }
                launch(lifetime) { installUserSwipeListener() }
                launch(lifetime) { installUserSwipeToSeekIntentListener() }
                launch(lifetime) {
                    swipeToSeekIntentListenerInstallJob.join()
                    readyForUserScroll = true
                }
            }
        }

        private suspend fun installIsUserDraggingListener() {
            check(!dragListenerInstallJob.isCompleted)
            val stack = mutableListOf<DragInteraction>()
            pagerState.interactionSource.interactions
                .onStart {
                    check(!dragListenerInstallJob.isCompleted)
                    dragListenerInstallJob.complete()
                }
                .collect { interaction ->
                    // pretty sure the pager only emits DragInteraction, maybe assert ?
                    if (interaction !is DragInteraction) return@collect
                    when (interaction) {
                        is DragInteraction.Start -> {
                            check(readyForUserScroll)
                            stack.add(interaction)
                        }
                        is DragInteraction.Cancel -> {
                            stack.remove(interaction.start)
                        }
                        is DragInteraction.Stop -> {
                            stack.remove(interaction.start)
                        }
                    }
                    isUserDragging = stack.isNotEmpty()
                }
        }

        private suspend fun doInitialLayoutPageCorrection() {
            check(!initialPageCorrectionAtPageJob.isCompleted)
            check(!initialPageCorrectionFullyAtPageJob.isCompleted)
            val targetPage = initialTargetPage
            val spread = (initialTargetPage - 2) ..(initialTargetPage + 2)
            supervisorScope {
                val correctorDispatchJob = Job()
                val correctorJob = uiCoroutineScope.launch {
                    dragListenerInstallJob.join()
                    if (ancestor?.isUserDragging == true) {
                        pagerState.stopScroll(MutatePriority.PreventUserInput)
                    }
                    correctorDispatchJob.complete()
                    if (ancestor != null && targetPage in spread) {
                        pagerState.animateScrollToPage(targetPage)
                    } else {
                        pagerState.scrollToPage(targetPage)
                    }
                }
                val atPage = launch {
                    correctorDispatchJob.join()
                    snapshotFlow { pagerState.currentPage }.first { it == targetPage }
                    initialPageCorrectionAtPageJob.complete()
                }
                atPage.join()
                correctorJob.join()
                initialPageCorrectionFullyAtPageJob.complete()
            }
        }

        private suspend fun installUserSwipeListener() {
            check(!swipeListenerInstallJob.isCompleted)
            // TODO: should not be a stack
            val scrollInstanceStack = mutableListOf<UserScrollInstance>()
            runCatching {
                pagerState.interactionSource.interactions
                    .onStart { swipeListenerInstallJob.complete() }
                    .collect { interaction ->
                        // pretty sure the pager only emits DragInteraction, maybe assert ?
                        if (interaction !is DragInteraction) return@collect
                        when (interaction) {
                            is DragInteraction.Start -> {
                                Timber.d(
                                    "PagerCont@${System.identityHashCode(this)}, " +
                                            "DragInteraction.Start"
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
                                uiCoroutineScope.launch {
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
                                        if (!scroll.isCancelled()) {
                                            userSwipeChannel.send(scroll)
                                        }
                                    }
                                }
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
                if (!swipeListenerInstallJob.isCompleted) swipeListenerInstallJob.cancel()
                if (it !is CancellationException) throw it
                scrollInstanceStack.forEach { instance ->
                    instance.onCancelled()
                    activeUserScrollInstance.remove(instance)
                }
                scrollInstanceStack.clear()
            }
        }

        private suspend fun installUserSwipeToSeekIntentListener() {
            check(!swipeToSeekIntentListenerInstallJob.isCompleted)
            runCatching {
                swipeListenerInstallJob.join()
                userSwipeChannel
                    .receiveAsFlow()
                    .onStart { swipeToSeekIntentListenerInstallJob.complete() }
                    .collect { scroll ->
                        Timber.d("MainQueuePager: swipe={${scroll.startPage}, ${scroll.currentPage}, $initialTargetPage}")
                        if (scroll.startPage != initialTargetPage) {
                            dispatchSnapPageCorrection().join()
                            return@collect
                        }
                        when (scroll.currentPage) {
                            initialTargetPage -1 -> {
                                val accepted = requestSeekPreviousWithExpectAsync(
                                    scroll.startPage,
                                ).await().getOrElse { false }
                                if (!accepted) {
                                    dispatchSnapPageCorrection().join()
                                }
                            }
                            initialTargetPage +1 -> {
                                val accepted = requestSeekNextWithExpectAsync(
                                    scroll.startPage,
                                ).await().getOrElse { false }
                                if (!accepted) {
                                    dispatchSnapPageCorrection().join()
                                }
                            }
                            else -> dispatchSnapPageCorrection().join()
                        }
                    }
            }.onFailure {
                if (!swipeListenerInstallJob.isCompleted) swipeListenerInstallJob.cancel()
                throw it
            }
        }

        private suspend fun dispatchSnapPageCorrection(): Job {
            return uiCoroutineScope.launch(lifetime) {
                dragListenerInstallJob.join()
                pagerState.stopScroll(MutatePriority.PreventUserInput)
                if (isUserDragging) snapshotFlow { isUserDragging }.first { !it }
                check(activeUserScrollInstance.isEmpty())
                pagerState.scrollToPage(initialTargetPage)
            }
        }
    }

    private class PagerContinuationSnapshot()

    private class PagerComposition(
        val layoutCoordinator: QueuePagerLayoutCoordinator,
        val pCont: PagerContinuation,
        private val queueData: OldPlaybackQueue,
        private val observeArtwork: (String) -> Flow<Any?>,
    ) {

        private val lifetime = SupervisorJob()

        fun onDisposed() {
            pCont.onDisposed()
            lifetime.cancel()
        }

        fun onSucceeded() {
            pCont.onSucceeded()
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
            val coroutineScope = rememberCoroutineScope()
            DisposableEffect(
                key1 = this,
                effect = {

                    coroutineScope.launch(lifetime + Dispatchers.Main.immediate) {
                        pCont.prepare()
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

            val returns = remember(this) {
                mutableStateOf(
                    PagerComposition(
                        QueuePagerLayoutCoordinator(),
                        pCont = PagerContinuation(
                            null,
                            0,
                            0,
                            CoroutineScope(EmptyCoroutineContext),
                            { CompletableDeferred<Result<Boolean>>().apply { cancel() } },
                            { CompletableDeferred<Result<Boolean>>().apply { cancel() } }
                        ),
                        queueData = OldPlaybackQueue.UNSET,
                        observeArtwork = { emptyFlow() },
                    )
                )
            }

            val uiCoroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    uiCoroutineScope.launch(supervisor) {
                        var initial = true
                        observeQueue().collect { queue ->
                            val current = returns.value
                            val next = PagerComposition(
                                layoutCoordinator = layoutCoordinator,
                                pCont = PagerContinuation(
                                    ancestor = if (initial) null else current.pCont,
                                    pageCount = queue.list.size,
                                    initialTargetPage = queue.currentIndex.coerceAtLeast(0),
                                    uiCoroutineScope = CoroutineScope(
                                        uiCoroutineScope.coroutineContext +
                                                SupervisorJob(supervisor)
                                    ),
                                    requestSeekNextWithExpectAsync = { i ->
                                        requestSeekNextWithExpectAsync(
                                            i,
                                            queue.list[i],
                                            queue.list[i + 1]
                                        )
                                    },
                                    requestSeekPreviousWithExpectAsync = { i ->
                                        requestSeekPreviousWithExpectAsync(
                                            i,
                                            queue.list[i],
                                            queue.list[i - 1]
                                        )
                                    }
                                ),
                                queueData = queue,
                                observeArtwork = observeArtwork
                            )
                            if (!initial) {
                                current.apply { onSucceeded() }
                            }
                            initial = false
                            returns.value = next
                        }
                    }

                    onDispose {
                        supervisor.cancel()
                        returns.value.apply { onDisposed() }
                    }
                }
            )

            return returns.value
        }
    }

    @Composable
    fun ComposeContent(
        pager: QueuePagerScope.() -> Unit
    ) {
        with(layoutCoordinator) {
            val scope = rememberPagerScope()
                .runRemember(pager) {
                    derivedStateOf { apply(pager) }
                }.value
            PlacePager(
                pager = {
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
        pager: PagerLayoutScope.() -> Unit
    ) = BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val scope = remember(this@QueuePagerLayoutCoordinator) {
            PagerLayoutScopeImpl()
        }.runRemember(pager) {
            derivedStateOf { apply(pager) }
        }.value
        scope.pagerContent()
    }

    @Composable
    fun PlacePagerItem(
        item: PagerLayoutItemScope.() -> Unit
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
        }.value
        scope.imageContent()
    }
}
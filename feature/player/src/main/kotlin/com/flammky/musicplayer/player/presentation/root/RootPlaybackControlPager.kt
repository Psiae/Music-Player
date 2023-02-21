package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.math.abs

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun BoxScope.RootPlaybackControlPager(
    state: RootPlaybackControlPagerState
) {
    val coroutineScope = rememberCoroutineScope()
    val upStateApplier = remember(state) {
        PagerStateApplier(state, coroutineScope)
    }.apply {
        ComposePreLayout()
    }
    BoxWithConstraints {
        val baseQueue = state.latestDisplayQueue
        val maskedQueue = state.maskedDisplayQueue
        if (maskedQueue.list.isEmpty()) return@BoxWithConstraints
        HorizontalPager(
            state = upStateApplier.pagerLayoutState,
            count = maskedQueue.list.size,
            flingBehavior = upStateApplier.rememberFlingBehavior(constraints = constraints),
            key = { i -> maskedQueue.list[i] }
        ) { i ->
            val id = maskedQueue.list[i]
            remember(state, id) {
                PagerItemScope(observeCurrentArtwork = { state.observeArtwork(id) })
            }.run {
                PagerItem()
            }
        }
        upStateApplier.ComposePostLayout(baseQueue = baseQueue, displayQueue = maskedQueue)
    }
}

@OptIn(ExperimentalPagerApi::class)
private class PagerStateApplier(
    private val state: RootPlaybackControlPagerState,
    private val coroutineScope: CoroutineScope
) {

    private var userDragging by mutableStateOf(false)

    val pagerLayoutState = state.pagerLayoutState

    private var pageInternalTouchWaiter = mutableListOf<() -> Unit>()

    private suspend fun scrollToPage(
        internal: Boolean,
        index: Int
    ) {
        Timber.d("RootPlaybackControlPager scrollToPage($internal, $index)")
        if (internal) {
            pageInternalTouchWaiter.forEach { it() }
            pageInternalTouchWaiter.clear()
            pagerLayoutState.stopScroll(MutatePriority.UserInput)
        }
        pagerLayoutState.scrollToPage(index)
    }

    private suspend fun animateScrollToPage(
        internal: Boolean,
        index: Int
    ) {
        Timber.d("RootPlaybackControlPager animateScrollToPage($internal, $index)")
        if (userDragging) {
            return scrollToPage(internal, index)
        }
        if (internal) {
            pageInternalTouchWaiter.forEach { it() }
            pageInternalTouchWaiter.clear()
            pagerLayoutState.stopScroll(MutatePriority.PreventUserInput)
        }
        pagerLayoutState.animateScrollToPage(index)
    }

    private suspend fun scrollToLatestBasePage(
        internal: Boolean,
    ) {
        if (internal) {
            pageInternalTouchWaiter.forEach { it() }
            pageInternalTouchWaiter.clear()
        }
        pagerLayoutState.scrollToPage(
            state.compositionLatestQueue.currentIndex.coerceAtLeast(0)
        )
    }

    private suspend fun scrollToLatestOverrideOrBasePage(
        internal: Boolean,
    ) {
        if (internal) {
            pageInternalTouchWaiter.forEach { it() }
            pageInternalTouchWaiter.clear()
        }
    }

    @Composable
    fun ComposePreLayout() {
        ListenLatestParentQueue()
        LaunchedEffect(key1 = Unit, block = {
            val dragInteractions = mutableListOf<DragInteraction.Start>()
            pagerLayoutState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> dragInteractions.add(interaction)
                    is DragInteraction.Stop -> dragInteractions.remove(interaction.start)
                    is DragInteraction.Cancel -> dragInteractions.remove(interaction.start)
                }
                userDragging = dragInteractions.isNotEmpty()
            }
        })
    }

    @OptIn(ExperimentalSnapperApi::class)
    @Composable
    fun rememberFlingBehavior(constraints: Constraints): FlingBehavior {
        val baseFling = PagerDefaults.flingBehavior(pagerLayoutState)
        return remember(baseFling, constraints) {
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    val constraint = constraints.maxWidth * 0.8F / 0.5f
                    val coerced =
                        (initialVelocity).coerceIn(-abs(constraint)..abs(constraint))
                    return with(baseFling) { performFling(coerced) }
                }
            }
        }
    }

    @Composable
    fun ComposePostLayout(
        baseQueue: OldPlaybackQueue,
        displayQueue: OldPlaybackQueue
    ) {
        ListenUserPageSwipe(
            baseQueue = baseQueue,
            displayQueue = displayQueue
        )
        LaunchedEffect(key1 = displayQueue, block = {
            if (displayQueue.currentIndex == -1) {
                return@LaunchedEffect pagerLayoutState.scrollToPage(0)
            }
            val spread = displayQueue.currentIndex - 2 .. displayQueue.currentIndex + 2
            if (pagerLayoutState.currentPage in spread) {
                animateScrollToPage(true, displayQueue.currentIndex)
            } else {
                scrollToPage(true, displayQueue.currentIndex)
            }
        })
    }

    @Composable
    private fun ListenLatestParentQueue() {
        LaunchedEffect(
            this,
            block = {
                snapshotFlow { state.compositionLatestQueue }
                    .first { it !== OldPlaybackQueue.UNSET }
                    .let { first ->
                        state
                            .apply {
                                latestDisplayQueue = first
                                scrollToPage(true, if (first.currentIndex == -1) 0 else first.currentIndex)
                            }
                    }
                snapshotFlow { state.compositionLatestQueue }
                    .collect { current ->
                        state
                            .apply {
                                Timber.d("state.compositionLatestQueue updated")
                                latestDisplayQueue = current
                            }
                    }
            }
        )
    }

    @Composable
    private fun ListenUserPageSwipe(
        baseQueue: OldPlaybackQueue,
        displayQueue: OldPlaybackQueue,
    ) {
        val upBaseQueue = rememberUpdatedState(newValue = baseQueue)
        val upDisplayQueue = rememberUpdatedState(newValue = displayQueue)
        LaunchedEffect(
            this,
            block = {
                forEachUserPageScroll { instance ->
                    val baseQueue = upBaseQueue.value
                    val displayQueue = upDisplayQueue.value
                    var latestPage = instance.startingPage
                    try {
                        runCatching { instance.whileDragging { awaitCancellation() } }
                        instance.whileScrollingOrInterrupted {
                            snapshotFlow { pagerLayoutState.currentPage }
                                .collect {
                                    if (it != latestPage) {
                                        launch {
                                            onUserSwipe(baseQueue, displayQueue, instance.startingPage, latestPage)
                                        }
                                    }
                                    latestPage = it
                                }
                        }
                    } finally {
                    }
                }
            }
        )
    }

    /**
     * Callback on which the user fully swipe the page and released the pointer
     */
    private suspend fun onUserSwipe(
        baseQueue: OldPlaybackQueue,
        displayQueue: OldPlaybackQueue,
        from: Int,
        to: Int
    ) {
        Timber.d("RootPlaybackControlPager onUserSwipe($from, $to)")
        check(from != to)
        if (to !in baseQueue.list.indices || to !in displayQueue.list.indices) {
            return
        }
        when (to) {
            from + 1 -> {
                val success = state.requestPlaybackMoveNext(
                    displayQueue,
                    from,
                    displayQueue.list[from],
                    to,
                    displayQueue.list[to]
                )
                Timber.d("RootPlaybackControlPager onUserSwipe($from, $to), success=$success")
                if (!success) {
                    scrollToPage(true, from)
                }
            }
            from - 1 -> {
                val success = state.requestPlaybackMovePrevious(
                    displayQueue,
                    from,
                    displayQueue.list[from],
                    to,
                    displayQueue.list[to]
                )
                Timber.d("RootPlaybackControlPager onUserSwipe($from, $to), success=$success")
                if (!success) {
                    scrollToPage(true, from)
                }
            }
            else -> scrollToPage(true, from)
        }
    }

    private suspend fun forEachUserPageScroll(
        block: suspend (instance: UserPageScrollInstance) -> Unit
    ) {
        val dragging = mutableStateOf(false)
        val coroutineScope = CoroutineScope(currentCoroutineContext())
        var latestDragStartInteractionAtPage = -1
        coroutineScope.launch {
            val dragInteractions = mutableListOf<DragInteraction.Start>()
            pagerLayoutState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> {
                        latestDragStartInteractionAtPage = pagerLayoutState.currentPage
                        dragInteractions.add(interaction)
                    }
                    is DragInteraction.Stop -> dragInteractions.remove(interaction.start)
                    is DragInteraction.Cancel -> dragInteractions.remove(interaction.start)
                }
                dragging.value = dragInteractions.isNotEmpty()
            }
        }
        coroutineScope.launch {
            var latestInstance: UserPageScrollInstance? = null
            try {
                snapshotFlow { dragging.value }
                    .collect { start ->
                        if (start) {
                            latestInstance
                                ?.apply {
                                    check(pointerUp)
                                    markScrollInterrupted()
                                }
                            latestInstance =
                                UserPageScrollInstance(latestDragStartInteractionAtPage)
                                    .apply {
                                        launch {
                                            whileScrollingOrInterrupted {
                                                awaitPageCorrection {
                                                    markScrollInterrupted()
                                                }
                                            }
                                        }
                                        launch {
                                            run { block(this@apply) }
                                        }
                                    }
                        } else {
                            latestInstance
                                ?.apply {
                                    markPointerUp()
                                    launch {
                                        whileScrollingOrInterrupted {
                                            awaitLayoutScrollingEnd {
                                                markScrollEnd()
                                            }
                                        }
                                    }
                                }
                        }
                    }
            } catch (ce: CancellationException) {
                check(!currentCoroutineContext().isActive)
                latestInstance?.markScrollInterrupted()
            }
        }
    }
    private class UserPageScrollInstance(
        val startingPage: Int
    ) {
        private val dragSupervisor = SupervisorJob()
        private val scrollJob = SupervisorJob()
        private val dragJob = SupervisorJob(dragSupervisor)

        var pointerUp: Boolean = false
            private set

        var interrupted: Boolean = false
            private set

        fun markPointerUp() {
            check(!pointerUp) {
                "$pointerUp"
            }
            pointerUp = true
            dragJob.cancel()
            dragSupervisor.cancel()
        }

        fun markScrollEnd() {
            check(pointerUp && !interrupted) {
                "$pointerUp $interrupted"
            }
            scrollJob.cancel()
        }

        fun markScrollInterrupted() {
            Timber.d("RootPlaybackControlPager, markScrollInterrupted")
            interrupted = true
            dragJob.cancel()
            scrollJob.cancel()
        }

        suspend fun whileScrollingOrInterrupted(
            block: suspend CoroutineScope.() -> Unit
        ) = withContext(scrollJob) { block() }

        suspend fun whileDraggingOrScrollInterrupted(
            block: suspend CoroutineScope.() -> Unit
        ) = withContext(dragJob) { block() }

        suspend fun whileDragging(
            block: suspend CoroutineScope.() -> Unit
        ) = withContext(dragSupervisor) { block() }
    }

    private suspend fun awaitPageCorrection(
        block: suspend () -> Unit
    ) {
        suspendCancellableCoroutine<Unit> { uCont ->
            val await = { uCont.resume(Unit) }
            pageInternalTouchWaiter.add(await)
            uCont.invokeOnCancellation { pageInternalTouchWaiter.remove(await) }
        }
        block()
    }

    private suspend fun awaitLayoutScrollingEnd(
        block: suspend () -> Unit
    ) {
        snapshotFlow { pagerLayoutState.isScrollInProgress }.first { !it }
        block()
    }
}

private class PagerItemScope(
    val observeCurrentArtwork: () -> Flow<Any?>
) {

}

@Composable
private fun PagerItemScope.PagerItem() {
    val artwork by observeCurrentArtwork().collectAsState(initial = null)
    val context = LocalContext.current
    val req = remember(artwork) {
        ImageRequest.Builder(context)
            .data(artwork)
            .build()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(280.dp)
                .align(Alignment.Center),
            model = req,
            contentDescription = "art",
            contentScale = ContentScale.Crop
        )
    }
}


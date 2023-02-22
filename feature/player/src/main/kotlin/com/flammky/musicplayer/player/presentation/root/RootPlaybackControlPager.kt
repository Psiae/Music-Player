package com.flammky.musicplayer.player.presentation.root

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

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun BoxScope.RootPlaybackControlPager(
    state: RootPlaybackControlPagerState
) {
    val coroutineScope = rememberCoroutineScope()
    val upStateApplier = remember(state) {
        PagerStateApplier(state, coroutineScope)
            .apply {
                prepareState()
            }
    }.apply {
        PrepareComposition()
    }
    BoxWithConstraints {
        // there is definitely a way to get optimization on the the PagerScope item provider
        val data = state.currentCompositionLayoutData
            ?: return@BoxWithConstraints
        upStateApplier.PagerCompositionStart(data)
        val displayQueue = data.queueData
        HorizontalPager(
            state = upStateApplier.pagerLayoutState,
            userScrollEnabled = true,
            count = displayQueue.list.size,
            flingBehavior = upStateApplier.rememberFlingBehavior(constraints = constraints),
            key = { i -> displayQueue.list[i] },
        ) { i ->
            val id = displayQueue.list[i]
            remember(state, id) {
                PagerItemScope(observeCurrentArtwork = { state.observeArtwork(id) })
            }.run {
                PagerItem()
            }
        }
        upStateApplier.PagerCompositionEnd(data)
    }
}

@OptIn(ExperimentalPagerApi::class)
private class PagerStateApplier(
    private val state: RootPlaybackControlPagerState,
    private val coroutineScope: CoroutineScope
) {

    fun prepareState() {
        state.currentCompositionLayoutData = null
    }

    private var userDraggingLayout by mutableStateOf(false)

    val pagerLayoutState = state.pagerLayoutState

    private var pageInternalTouchWaiter = mutableListOf<() -> Unit>()

    private suspend fun scrollToPage(
        correction: Boolean,
        index: Int
    ) {
        Timber.d("RootPlaybackControlPager scrollToPage($correction, $index)")
        if (correction) {
            pageInternalTouchWaiter.forEach { it() }
            pageInternalTouchWaiter.clear()
            pagerLayoutState.stopScroll(MutatePriority.UserInput)
        }
        pagerLayoutState.scrollToPage(index)
    }

    private suspend fun animateScrollToPage(
        correction: Boolean,
        index: Int
    ) {
        Timber.d("RootPlaybackControlPager animateScrollToPage($correction, $index)")
        if (userDraggingLayout) {
            return scrollToPage(correction, index)
        }
        if (correction) {
            pageInternalTouchWaiter.forEach { it() }
            pageInternalTouchWaiter.clear()
            pagerLayoutState.stopScroll(MutatePriority.PreventUserInput)
        }
        pagerLayoutState.animateScrollToPage(index)
    }

    @Composable
    fun PrepareComposition() {
        ListenLatestParentQueue()
        LaunchedEffect(key1 = this, block = {
            val dragInteractions = mutableListOf<DragInteraction.Start>()
            pagerLayoutState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> dragInteractions.add(interaction)
                    is DragInteraction.Stop -> dragInteractions.remove(interaction.start)
                    is DragInteraction.Cancel -> dragInteractions.remove(interaction.start)
                }
                this@PagerStateApplier.userDraggingLayout = dragInteractions.isNotEmpty()
            }
        })
    }

    @Composable
    fun PagerCompositionStart(
        composition: PagerLayoutComposition
    ) {
        Timber.d("RootPlaybackControlPager: PagerCompositionStart $composition")
        DisposableEffect(
            key1 = composition,
            effect = {
                onDispose { composition.onForgotten() }
            }
        )
    }

    @Composable
    fun PagerCompositionEnd(
        composition: PagerLayoutComposition
    ) {
        Timber.d("RootPlaybackControlPager: PagerCompositionEnd $composition")
        remember(composition) {
            composition.launchInRememberScope {
                launch {
                    if (
                        composition.isFirstComposition ||
                        run {
                            val spread =
                                (composition.queueData.currentIndex - 2) ..
                                        (composition.queueData.currentIndex + 2)
                            pagerLayoutState.currentPage !in spread
                        }
                    ) {
                        scrollToPage(true, composition.queueData.currentIndex)
                    } else {
                        animateScrollToPage(true, composition.queueData.currentIndex)
                    }
                    composition.awaitScrollToPageCorrection()
                    composition.onPageCorrectionFullyAtPage()
                }
                composition.onPageCorrectionDispatched()
                snapshotFlow { pagerLayoutState.currentPage }
                    .first { it == composition.queueData.currentIndex }
                composition.onPageCorrectionAtPage()
            }
            composition
                .apply {
                    launchInRememberScope {
                        awaitScrollToPageCorrection()
                        awaitUserInteractionListener()
                        readyForUserScroll = true
                    }
                }
        }
        InstallInteractionListenerForComposition(composition = composition)
        ListenUserPageSwipeOnComposition(composition = composition)
    }

    @OptIn(ExperimentalSnapperApi::class)
    @Composable
    fun rememberFlingBehavior(constraints: Constraints): FlingBehavior {
        return PagerDefaults.flingBehavior(
            state = pagerLayoutState,
            snapIndex = PagerDefaults.singlePageSnapIndex
        )
        /*val baseFling = PagerDefaults.flingBehavior(pagerLayoutState)
        return remember(baseFling, constraints) {
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    val constraint = constraints.maxWidth * 0.8F / 0.5f
                    val coerced =
                        (initialVelocity).coerceIn(-abs(constraint)..abs(constraint))
                    return with(baseFling) { performFling(coerced) }
                }
            }
        }*/
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
                                currentCompositionLayoutData?.onForgotten()
                                currentCompositionLayoutData = PagerLayoutComposition(
                                    true,
                                    first,
                                    CoroutineScope(
                                        currentCoroutineContext() +
                                                SupervisorJob(currentCoroutineContext().job)
                                    )
                                )
                            }
                    }
                snapshotFlow { state.compositionLatestQueue }
                    .collect { current ->
                        state
                            .apply {
                                currentCompositionLayoutData?.onForgotten()
                                currentCompositionLayoutData = PagerLayoutComposition(
                                    false,
                                    current,
                                    CoroutineScope(
                                        currentCoroutineContext() +
                                                SupervisorJob(currentCoroutineContext().job)
                                    )
                                )
                            }
                    }
            }
        )
    }

    @Composable
    private fun InstallInteractionListenerForComposition(
        composition: PagerLayoutComposition
    ) {
        remember(composition) {
            val layoutPageCollectorStart = Job()
            val layoutDragCollectorStart = Job()
            val dragCollectorStart = Job()
            composition
                .apply {
                    launchInRememberScope {
                        composition.awaitScrollToPageCorrection()
                        snapshotFlow { pagerLayoutState.currentPage }
                            .collect { page ->
                                composition.currentPageInCompositionSpan = page
                                layoutPageCollectorStart.complete()
                            }
                    }
                    launchInRememberScope {
                        composition.awaitScrollToPageCorrection()
                        layoutPageCollectorStart.join()
                        snapshotFlow { userDraggingLayout }
                            .collect { dragging ->
                                composition.userDraggingToStartIndex =
                                    if (dragging) true to currentPageInCompositionSpan else null
                                layoutDragCollectorStart.complete()
                            }
                    }
                    launchInRememberScope {
                        composition.awaitScrollToPageCorrection()
                        layoutDragCollectorStart.join()
                        var latestDragInstance: PagerLayoutComposition.UserDragInstance? = null
                        var latestScrollInstance: PagerLayoutComposition.UserScrollInstance? = null
                        try {
                            snapshotFlow { composition.userDraggingToStartIndex }
                                .collect {
                                    if (it != null) {
                                        latestScrollInstance?.onInterruptedByAnotherScroll()
                                        val (dragging: Boolean, page: Int) = it
                                        check(dragging)
                                        val newDrag = PagerLayoutComposition.UserDragInstance(page)
                                        val newScroll = PagerLayoutComposition.UserScrollInstance(newDrag)
                                        composition.currentUserDragInstance = newDrag
                                        composition.currentUserScrollInstance = newScroll
                                        latestDragInstance = newDrag
                                        latestScrollInstance = newScroll
                                    } else {
                                        latestDragInstance?.onPointerUp()
                                    }
                                    dragCollectorStart.complete()
                                }
                        } catch (ce: CancellationException) {
                            latestDragInstance?.onCancelled()
                            latestScrollInstance?.onCancelled()
                        }
                    }
                    launchInRememberScope {
                        layoutPageCollectorStart.join()
                        layoutDragCollectorStart.join()
                        dragCollectorStart.join()
                        composition.onUserInteractionListenerInstalled()
                    }
                }
        }
    }

    @Composable
    private fun ListenUserPageSwipeOnComposition(
        composition: PagerLayoutComposition,
    ) {
        LaunchedEffect(
            composition,
            block = {
                composition.forEachUserPageScroll { instance ->
                    Timber.d("RootPlaybackControlPager, forEachUserPageScroll $instance")
                    try {
                        composition.launchInRememberScope {
                            var latestPage = instance.startPageIndex
                            runCatching { instance.whileDraggingOrInterrupted { awaitCancellation() } }
                            instance.whileScrollingOrInterrupted {
                                // I think we should update via instance instead
                                snapshotFlow { composition.currentPageInCompositionSpan }
                                    .collect {
                                        val currentLatestPage = latestPage
                                        latestPage = it
                                        if (it != currentLatestPage) {
                                            coroutineScope.launch {
                                                onUserSwipe(composition.queueData, currentLatestPage, it)
                                            }
                                        }
                                    }
                            }
                        }
                    } catch (ce: CancellationException) {}
                }
            }
        )
    }

    /**
     * Callback on which the user fully swipe the page and released the pointer
     */
    private suspend fun onUserSwipe(
        displayQueue: OldPlaybackQueue,
        from: Int,
        to: Int
    ) {
        Timber.d("RootPlaybackControlPager onUserSwipe($from, $to)")
        check(from != to)
        if (to !in displayQueue.list.indices) {
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


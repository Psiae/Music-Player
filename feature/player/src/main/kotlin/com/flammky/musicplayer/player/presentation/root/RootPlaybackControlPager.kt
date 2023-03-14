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
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.player.BuildConfig
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun BoxScope.RootPlaybackControlPager(
    state: PlaybackControlPagerState
) {
    Timber.d("RootPlaybackControlPager, Entry Recomposed $state")
    val coroutineScope = rememberCoroutineScope()
    val upStateApplier = remember(state) {
        PlaybackControlPagerStateApplier(state, coroutineScope)
            .apply {
                Timber.d("RootPlaybackControlPager, Entry new Applier $this for $state")
                prepareState()
            }
    }.apply {
        PrepareComposition()
    }

    BoxWithConstraints {
        // there is definitely a way to get optimization on the the PagerScope item provider
        val data = state.currentCompositionLayoutData
            ?: run {
                upStateApplier.pagerCompositionSkip()
                return@BoxWithConstraints
            }
        upStateApplier.PagerCompositionStart(data)
        val displayQueue = data.queueData
        // TODO: we should dispatch drag delta by ourselves,
        //  every scroll event should only be composed of single Pointer Input
        HorizontalPager(
            state = upStateApplier.pagerLayoutState,
            userScrollEnabled = data.readyForUserScroll,
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
internal class PlaybackControlPagerStateApplier(
    private val state: PlaybackControlPagerState,
    private val coroutineScope: CoroutineScope
) {

    companion object {

        @Composable
        fun PlaybackControlPagerStateApplier.ComposeLayout(
            content: @Composable PagerLayoutCompositionScope.() -> Unit
        ) {

        }
/*
        @Composable
        private fun PlaybackControlPagerStateApplier.observeForCompositionScope(): PagerLayoutCompositionScope {
            val composableCoroutineScope = rememberCoroutineScope()
            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    composableCoroutineScope.launch {

                    }

                    onDispose {
                        supervisor.cancel()
                    }
                }
            )
        }*/
    }

    private var currentPreAppliedLayout by mutableStateOf<PagerLayoutCompositionScope?>(null)
    private var currentAppliedLayout by mutableStateOf<PagerLayoutCompositionScope?>(null)

    private var firstAppliedLayout = true

    fun prepareState() {
        Timber.d("RootPlaybackControlPager, PagerStateApplier: prepareState")
        state.currentCompositionLayoutData = null
    }

    private var userDraggingLayout by mutableStateOf(false)

    val pagerLayoutState = state.pagerLayoutState

    private suspend fun scrollToAppliedCompositionPage(
        animate: Boolean,
        debugReason: String = ""
    ) {
        if (BuildConfig.DEBUG) {
            Timber.d("scrollToAppliedCompositionPage($animate, $debugReason)")
        }
        coroutineContext.ensureActive()
        currentAppliedLayout?.let {
            it.withRememberSupervisor {
                pagerLayoutState.stopScroll(MutatePriority.PreventUserInput)
                snapshotFlow { userDraggingLayout }.first { dragging -> !dragging }
                if (animate) {
                    pagerLayoutState.animateScrollToPage(it.queueData.currentIndex)
                } else {
                    pagerLayoutState.scrollToPage(it.queueData.currentIndex)
                }
            }
        }
    }

    @Composable
    fun PrepareComposition() {
        Timber.d("RootPlaybackControlPager, PagerStateApplier, prepareComposition")
        ListenLatestParentQueue()
        LaunchedEffect(key1 = this, block = {
            val dragInteractions = mutableListOf<DragInteraction.Start>()
            pagerLayoutState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> dragInteractions.add(interaction)
                    is DragInteraction.Stop -> dragInteractions.remove(interaction.start)
                    is DragInteraction.Cancel -> dragInteractions.remove(interaction.start)
                }
                this@PlaybackControlPagerStateApplier.userDraggingLayout = dragInteractions.isNotEmpty()
                Timber.d("RootPlaybackControlPager, diagnostic: new Interaction=$interaction, dragging=$userDraggingLayout")
            }
        })
        LaunchedEffect(
            key1 = this, block = {
                snapshotFlow { pagerLayoutState.currentPage }
                    .collect {
                        Timber.d("RootPlaybackControlPager, diagnostic: new CurrentPage=$it, dragging=$userDraggingLayout")
                    }
            }
        )
        DisposableEffect(
            key1 = this,
            effect = {
                state.incrementQueueReader()
                onDispose { state.decrementQueueReader() }
            }
        )
    }

    fun pagerCompositionSkip() {
        currentPreAppliedLayout = null
        currentAppliedLayout = null
        firstAppliedLayout = true
    }

    @Composable
    fun PagerCompositionStart(
        composition: PagerLayoutCompositionScope
    ) {
        currentPreAppliedLayout = composition
        DisposableEffect(
            key1 = composition,
            effect = {
                onDispose { composition.onForgotten() }
            }
        )
    }

    /**
     * callback on which the data is expected to be reflected to the layout
     */
    @Composable
    fun PagerCompositionEnd(
        composition: PagerLayoutCompositionScope
    ) {
        Timber.d("RootPlaybackControlPager: PagerCompositionEnd $composition")
        check(currentPreAppliedLayout == composition)
        currentAppliedLayout = composition
        LaunchedEffect(
            composition,
            block = {
                composition.withRememberSupervisor {
                    val targetIndex = composition.queueData.currentIndex.coerceAtLeast(0)
                    val spread =
                        (composition.queueData.currentIndex - 2) ..(composition.queueData.currentIndex + 2)
                    val expectScrollFromPage = state.pagerLayoutState.currentPage
                    val isDirectionToLeft = expectScrollFromPage > targetIndex
                    launch {
                        when {
                            firstAppliedLayout -> {
                                firstAppliedLayout = false
                                scrollToAppliedCompositionPage(
                                    false,
                                    "compositionInitialPageCorrection, firstLayout"
                                )
                            }
                            run {
                                pagerLayoutState.currentPage !in spread
                            } -> {
                                scrollToAppliedCompositionPage(
                                    false,
                                    "compositionInitialPageCorrection, out spread"
                                )
                            }
                            else -> {
                                scrollToAppliedCompositionPage(
                                    true,
                                    "compositionInitialPageCorrection, in spread"
                                )
                            }
                        }
                        composition
                            .apply {
                                awaitScrollToPageCorrection()
                                onPageCorrectionFullyAtPage()
                            }
                    }
                    composition
                        .apply {
                            onPageCorrectionDispatched()
                            var latestCollectedPage = -1
                            snapshotFlow { state.pagerLayoutState.currentPage }
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
                            onPageCorrectionAtPage()
                            awaitScrollToPageCorrection()
                            awaitUserInteractionListener()
                            readyForUserScroll = true
                            Timber.d("RootPlaybackControlPager $composition readyForUserScroll true")
                        }
                }
            }
        )
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
    }

    @Composable
    private fun ListenLatestParentQueue() {
        Timber.d("RootPlaybackControlPager, PagerStateApplier, ListenLatestParentQueue")
        LaunchedEffect(
            this,
            block = {
                snapshotFlow { state.compositionLatestQueue }
                    .collect { current ->
                        state
                            .apply {
                                currentCompositionLayoutData?.onForgotten()
                                if (current === PlaybackConstants.QUEUE_UNSET) {
                                    currentCompositionLayoutData = null
                                    return@collect
                                }
                                currentCompositionLayoutData = PagerLayoutCompositionScope(
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
        composition: PagerLayoutCompositionScope
    ) {
        remember(composition) {
            val targetInitialPage = composition.queueData.currentIndex.coerceAtLeast(0)
            val layoutPageCollectorStart = Job()
            val layoutDragCollectorStart = Job()
            val dragCollectorStart = Job()
            composition
                .apply {
                    launchInRememberScope {
                        composition.awaitScrollToPageCorrection()
                        snapshotFlow { pagerLayoutState.currentPage }
                            .onStart {
                                composition.currentPageInCompositionSpan = targetInitialPage
                                layoutPageCollectorStart.complete()
                            }
                            .collect { page ->
                                composition.currentPageInCompositionSpan = page
                            }
                    }
                    launchInRememberScope {
                        composition.awaitScrollToPageCorrection()
                        layoutPageCollectorStart.join()
                        snapshotFlow { userDraggingLayout }
                            .onStart {
                                composition.userDraggingToStartIndex = null
                                layoutDragCollectorStart.complete()
                            }
                            .collect { dragging ->
                                composition.userDraggingToStartIndex =
                                    if (dragging) true to currentPageInCompositionSpan else null
                            }
                    }
                    launchInRememberScope {
                        composition.awaitScrollToPageCorrection()
                        layoutDragCollectorStart.join()
                        check(pagerLayoutState.currentPage == targetInitialPage)
                        var latestDragInstance: PagerLayoutCompositionScope.UserDragInstance? = null
                        var latestScrollInstance: PagerLayoutCompositionScope.UserScrollInstance? = null
                        try {
                            snapshotFlow { composition.userDraggingToStartIndex }
                                .collect {
                                    if (it != null) {
                                        latestScrollInstance?.onInterruptedByAnotherScroll()
                                        val (dragging: Boolean, page: Int) = it
                                        check(dragging)
                                        val newDrag = PagerLayoutCompositionScope.UserDragInstance(page)
                                        val newScroll = PagerLayoutCompositionScope.UserScrollInstance(newDrag)
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
                        check(pagerLayoutState.currentPage == targetInitialPage)
                        composition.onUserInteractionListenerInstalled()
                    }
                }
        }
    }

    @Composable
    private fun ListenUserPageSwipeOnComposition(
        composition: PagerLayoutCompositionScope,
    ) {
        LaunchedEffect(
            composition,
            block = {
                composition.run {
                    launchInRememberScope {
                        forEachUserPageScroll { instance ->
                            Timber.d("RootPlaybackControlPager, forEachUserPageScroll $instance")
                            var latestPage = instance.startPageIndex
                            runCatching {
                                // suspend until drag pointer is up
                                instance.whileDraggingOrInterrupted { awaitCancellation() }
                            }
                            runCatching {
                                // while the page is scrolling,
                                // collect until page is changed or the scroll is ended / interrupted
                                instance.whileScrollingOrInterrupted {
                                    // I think we should update via instance instead
                                    latestPage = snapshotFlow { composition.currentPageInCompositionSpan }
                                        .first { it != instance.startPageIndex }
                                }
                            }
                            // if changed successfully make a request to the playback controller
                            if (latestPage != instance.startPageIndex) {
                                composition.launchInRememberScope {
                                    onUserSwipe(composition, instance.startPageIndex, latestPage)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Callback on which the user fully swipe the page and released the pointer
     */
    private suspend fun onUserSwipe(
        composition: PagerLayoutCompositionScope,
        from: Int,
        to: Int
    ) {
        Timber.d("RootPlaybackControlPager onUserSwipe($from, $to, $composition)")
        check(from != to) {
            "Possible Inconsistency: onUserSwipe should only be called with different page"
        }
        check(to in composition.queueData.list.indices) {
            "Possible Inconsistency: onUserSwipe request should always be in queue range"
        }
        if (composition.isForgotten) {
            return
        }
        if (from != composition.queueData.currentIndex || to !in from -1..from +1) {
            return scrollToAppliedCompositionPage(
                false,
                "onUserSwipe($composition, $from, $to), " +
                        "invalid request data $from ${composition.queueData.currentIndex}"
            )
        }
        when (to) {
            from + 1 -> {
                val result = withContext(coroutineScope.coroutineContext) {
                    state.requestPlaybackMoveNext(
                        composition.queueData,
                        from,
                        composition.queueData.list[from],
                        composition.queueData.list[to]
                    )
                }
                Timber.d("RootPlaybackControlPager onUserSwipe($from, $to), success=$result")
                if (!result.isSuccess) {
                    scrollToAppliedCompositionPage(
                        false,
                        "onUserSwipe($composition, $from, $to), " +
                                "requestPlaybackMoveNextFail(${result.exceptionOrNull()})"
                    )
                }
            }
            from - 1 -> {
                val result = withContext(coroutineScope.coroutineContext) {
                    state.requestPlaybackMovePrevious(
                        composition.queueData,
                        from,
                        composition.queueData.list[from],
                        composition.queueData.list[to]
                    )
                }
                Timber.d("RootPlaybackControlPager onUserSwipe($from, $to), success=$result")
                if (!result.isSuccess) {
                    scrollToAppliedCompositionPage(
                        false,
                        "onUserSwipe($composition, $from, $to), " +
                                "requestPlaybackMovePreviousFail(${result.exceptionOrNull()})"
                    )
                }
            }
            else -> error("should not reach here")
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


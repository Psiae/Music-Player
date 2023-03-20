package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.runtime.*
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.CompositionScope.Companion.OnLayoutComposed
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerDefaults
import com.google.accompanist.pager.PagerState
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalPagerApi::class)
internal class RootPlaybackControlPagerState(
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observeMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
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
) {

    val applier = Applier(this)


    class Applier(
        private val state: RootPlaybackControlPagerState
    ) {

        init {
            @Suppress("SENSELESS_COMPARISON")
            check(state.applier == null)
        }

        companion object {
            
            @Composable
            fun Applier.ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {
                observeForScope()
                    .run {
                        content()
                        if (this === CompositionScope.UNSET) return@run
                        OnLayoutComposed()
                    }
            }

            @Composable
            fun Applier.observeForScope(): CompositionScope {
                val latestScope = remember {
                    mutableStateOf(CompositionScope.UNSET)
                }
                val coroutineScope = rememberCoroutineScope()
                DisposableEffect(
                    this,
                    effect = {
                        val supervisor = SupervisorJob()

                        coroutineScope.launch(supervisor) {
                            state.observeQueue()
                                .collect { queue ->
                                    val current = latestScope.value
                                    val next = CompositionScope(
                                        CoroutineScope(coroutineContext + SupervisorJob()),
                                        if (current === CompositionScope.UNSET) {
                                            PagerState(0)
                                        } else {
                                            current.pagerLayoutState
                                        },
                                        queueData = queue,
                                        observeMetadata = state.observeMetadata,
                                        observeArtwork = state.observeArtwork,
                                        requestSeekNextWithExpectAsync = state.requestSeekNextWithExpectAsync,
                                        requestSeekPreviousWithExpectAsync = state.requestSeekPreviousWithExpectAsync
                                    )
                                    if (current !== CompositionScope.UNSET) {
                                        current.apply {
                                            onSucceeded(next)
                                        }
                                        next.apply {
                                            onSucceeding(current)
                                        }
                                    }
                                    latestScope.value = next
                                }
                        }
                        onDispose {
                            latestScope.value.onDetachedFromComposition()
                            supervisor.cancel()
                        }
                    }
                )
                return latestScope.value
            }
        }
    }

    class CompositionScope(
        val lifetimeCoroutineScope: CoroutineScope,
        val pagerLayoutState: PagerState,
        val queueData: OldPlaybackQueue,
        val observeMetadata: (String) -> Flow<MediaMetadata?>,
        val observeArtwork: (String) -> Flow<Any?>,
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
    ) {

        private val globalDragListenerInstallJob = Job()
        private val initialPageCorrectionAtPageJob = Job()
        private val initialPageCorrectionFullyAtPageJob = Job()
        private val swipeListenerToSeekIntentInstallJob = Job()

        private var ancestor: CompositionScope? = null
        private var successor: CompositionScope? = null

        private var disposeSlot = true

        private var prepareSwipeState by mutableStateOf(false)

        private var isUserDragging by mutableStateOf(false)

        private var registeredUserScrollInstance = mutableListOf<UserScrollInstance>()

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

        fun onSucceeded(next: CompositionScope) {
            check(disposeSlot)
            check(successor == null)
            successor = next
            lifetimeCoroutineScope.cancel()
        }

        fun onSucceeding(previous: CompositionScope) {
            check(ancestor == null)
            check(previous !== Companion.UNSET)
            check(previous.successor == null || previous.successor === this)
            check(previous.disposeSlot)
            ancestor = previous
        }

        fun onDetachedFromComposition() {
            check(disposeSlot)
            disposeSlot = false
            lifetimeCoroutineScope.cancel()
        }

        companion object {
            val UNSET = CompositionScope(
                CoroutineScope(EmptyCoroutineContext),
                PagerState(0),
                OldPlaybackQueue.UNSET,
                { emptyFlow() },
                { emptyFlow() },
                { _, _, _ -> CompletableDeferred<Result<Boolean>>().apply { cancel() } },
                { _, _, _ -> CompletableDeferred<Result<Boolean>>().apply { cancel() } },
            )

            fun CompositionScope.identityHashCode(): Int = System.identityHashCode(this)

            @Composable
            fun CompositionScope.pageCount(): Int {
                return queueData.list.size
            }

            @Composable
            fun CompositionScope.contentAlpha(): Float {
                ancestor?.let {
                    if (it.initialPageCorrectionAtPageJob.isCompleted) {
                        return 1f
                    }
                }
                val state = remember(this) {
                    mutableStateOf(0f)
                }
                LaunchedEffect(
                    key1 = state,
                    block = {
                        initialPageCorrectionAtPageJob.join()
                        state.value = 1f
                    }
                )
                return state.value
            }

            @OptIn(ExperimentalSnapperApi::class)
            @Composable
            fun CompositionScope.flingBehavior(): FlingBehavior {
                return PagerDefaults.flingBehavior(
                    state = pagerLayoutState,
                    snapIndex = PagerDefaults.singlePageSnapIndex
                )
            }

            fun CompositionScope.layoutKey(page: Int): Any {
                return queueData.list[page]
            }

            @SnapshotRead
            @Composable
            fun CompositionScope.userScrollEnabled(): Boolean {
                return prepareSwipeState &&
                        (pagerLayoutState.currentPage == queueData.currentIndex || isUserDragging)
            }

            @Composable
            fun CompositionScope.OnLayoutComposed() {
                LaunchedEffect(
                    key1 = this,
                    block = {
                        lifetimeCoroutineScope.launch {
                            doInstallGlobalIsDraggingListener()
                        }
                        lifetimeCoroutineScope.launch {
                            doInitialLayoutCorrection()
                        }
                        lifetimeCoroutineScope.launch {
                            doInstallSwipeToSeekIntentListener()
                        }
                        lifetimeCoroutineScope.launch {
                            globalDragListenerInstallJob.join()
                            initialPageCorrectionAtPageJob.join()
                            swipeListenerToSeekIntentInstallJob.join()
                            prepareSwipeState = true
                        }
                    }
                )
            }

            private suspend fun CompositionScope.doInstallGlobalIsDraggingListener() {
                val stack = mutableListOf<DragInteraction>()
                pagerLayoutState.interactionSource.interactions
                    .onStart {
                        globalDragListenerInstallJob.complete()
                    }
                    .collect { interaction ->
                        // pretty sure the pager only emits DragInteraction, maybe assert ?
                        if (interaction !is DragInteraction) return@collect
                        when (interaction) {
                            is DragInteraction.Start -> stack.add(interaction)
                            is DragInteraction.Cancel -> stack.remove(interaction.start)
                            is DragInteraction.Stop -> stack.remove(interaction.start)
                        }
                        isUserDragging = stack.isNotEmpty()
                    }
            }

            private suspend fun CompositionScope.doInitialLayoutCorrection() {
                val targetPage = queueData.currentIndex.coerceAtLeast(0)
                val spread = (queueData.currentIndex - 2) ..(queueData.currentIndex + 2)
                withContext(SupervisorJob()) {
                    val correctorDispatchJob = Job()
                    val correctorJob = launch {
                        globalDragListenerInstallJob.join()
                        if (ancestor?.isUserDragging == true || isUserDragging) {
                            pagerLayoutState.stopScroll(MutatePriority.PreventUserInput)
                            snapshotFlow { isUserDragging }.first { !it }
                        }
                        correctorDispatchJob.complete()
                        if (ancestor != null && targetPage in spread) {
                            pagerLayoutState.animateScrollToPage(targetPage)
                        } else {
                            pagerLayoutState.scrollToPage(targetPage)
                        }
                    }
                    val atPage = launch {
                        correctorDispatchJob.join()
                        snapshotFlow { pagerLayoutState.currentPage }
                            .first { it == targetPage }
                        initialPageCorrectionAtPageJob.complete()
                    }
                    atPage.join()
                    correctorJob.join()
                    initialPageCorrectionFullyAtPageJob.complete()
                }
            }

            private suspend fun CompositionScope.doInstallSwipeToSeekIntentListener() {
                forEachUserSwipeToOtherPage(
                    onStart = {
                        swipeListenerToSeekIntentInstallJob.complete()
                    }
                ) { scroll ->
                    Timber.d("RootPlaybackControlPagerState_CompositionScope: swipe=$scroll")
                    if (scroll.startPage != queueData.currentIndex) {
                        dispatchSnapPageCorrection().join()
                    }
                    when (scroll.currentPage) {
                        queueData.currentIndex -1 -> {
                            val accepted = requestSeekPreviousWithExpectAsync(
                                scroll.startPage,
                                queueData.list[scroll.startPage],
                                queueData.list[scroll.currentPage]
                            ).await().getOrElse { false }
                            if (!accepted) {
                                dispatchSnapPageCorrection().join()
                            }
                        }
                        queueData.currentIndex +1 -> {
                            val accepted = requestSeekNextWithExpectAsync(
                                scroll.startPage,
                                queueData.list[scroll.startPage],
                                queueData.list[scroll.currentPage]
                            ).await().getOrElse { false }
                            if (!accepted) {
                                dispatchSnapPageCorrection().join()
                            }
                        }
                        else -> dispatchSnapPageCorrection().join()
                    }
                }
            }

            // the term `swipe` here means that the user dragged the page until the target page
            // visibility is more than half and then user release the drag without any interference
            // in between
            private suspend fun CompositionScope.forEachUserSwipeToOtherPage(
                onStart: () -> Unit = {},
                block: suspend (UserScrollInstance) -> Unit
            ): Unit = withContext(currentCoroutineContext()) {
                // TODO: should not be a stack
                // TODO: should assert that Scroll Direction should never change once the drag is up
                val scrollInstanceStack = mutableListOf<UserScrollInstance>()
                val swipeChannel = Channel<UserScrollInstance>(Channel.UNLIMITED)
                launch {
                    for (swipe in swipeChannel) {
                        block(swipe)
                    }
                }
                launch {
                    runCatching {
                        pagerLayoutState.interactionSource.interactions
                            .onStart { onStart() }
                            .collect { interaction ->
                                // pretty sure the pager only emits DragInteraction, maybe assert ?
                                if (interaction !is DragInteraction) return@collect
                                when (interaction) {
                                    is DragInteraction.Start -> {
                                        Timber.d("CompositionScope@${identityHashCode()}, DragInteraction.Start")
                                        check(scrollInstanceStack.size <= 1)
                                        check(scrollInstanceStack.none { it.drag.interactionStart == interaction })
                                        val drag = UserDragInstance(
                                            pagerLayoutState.currentPage,
                                            interaction
                                        )
                                        val scroll = UserScrollInstance(drag)
                                        scrollInstanceStack.add(scroll)
                                        registeredUserScrollInstance.add(scroll)
                                        CoroutineScope(currentCoroutineContext()).launch {
                                            launch {
                                                runCatching {
                                                    scroll.drag.inLifetime {
                                                        snapshotFlow { pagerLayoutState.currentPage }
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
                                                        snapshotFlow { pagerLayoutState.currentPage }
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
                                                        snapshotFlow { pagerLayoutState.isScrollInProgress }
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
                                                    swipeChannel.send(scroll)
                                                }
                                            }
                                        }
                                    }
                                    is DragInteraction.Cancel -> {
                                        Timber.d("CompositionScope@${identityHashCode()}, DragInteraction.Cancel")
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
                                            registeredUserScrollInstance.remove(get)
                                        }
                                    }
                                    is DragInteraction.Stop -> {
                                        Timber.d("CompositionScope@${identityHashCode()}, DragInteraction.Stop")
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
                                            registeredUserScrollInstance.remove(get)
                                        }
                                    }
                                }
                            }
                    }.onFailure {
                        if (it !is CancellationException) throw it
                        scrollInstanceStack.forEach { instance ->
                            instance.onCancelled()
                            registeredUserScrollInstance.remove(instance)
                        }
                        scrollInstanceStack.clear()
                    }
                }
            }

            private fun CompositionScope.dispatchSnapPageCorrection(): Job {
                return lifetimeCoroutineScope.launch {
                    globalDragListenerInstallJob.join()
                    pagerLayoutState.stopScroll(MutatePriority.PreventUserInput)
                    snapshotFlow { isUserDragging }.first { !it }
                    assertNoActiveUserScrollListener()
                    pagerLayoutState.scrollToPage(queueData.currentIndex)
                }
            }

            @kotlin.jvm.Throws(IllegalStateException::class)
            private fun CompositionScope.assertNoActiveUserScrollListener() {
                registeredUserScrollInstance.forEach { check(!it.isActive()) }
            }
        }
    }
}
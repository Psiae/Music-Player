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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalPagerApi::class)
internal class RootPlaybackControlPagerState(
    val coroutineScope: CoroutineScope,
    val pagerLayoutState: PagerState,
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
                observeForScope().run {
                    content()
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

        private val dragListenerInstallJob = Job()
        private val initialPageCorrectionAtPageJob = Job()
        private val initialPageCorrectionFullyAtPageJob = Job()

        private var ancestor: CompositionScope? = null
        private var successor: CompositionScope? = null

        private var disposeSlot = true

        private var readyForSwipe by mutableStateOf(false)

        private var isUserDragging by mutableStateOf(false)

        class UserDragInstance(

        ) {

        }

        class UserScrollInstance(
            val drag: UserDragInstance
        ) {

        }

        fun onSucceeded(next: CompositionScope) {
            check(disposeSlot)
            check(successor == null)
            successor = next
            disposeSlot = false
            lifetimeCoroutineScope.cancel()
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
                return readyForSwipe
            }

            @Composable
            @NonRestartableComposable
            fun CompositionScope.OnLayoutComposed() {
                LaunchedEffect(
                    key1 = this,
                    block = {
                        lifetimeCoroutineScope.launch {
                            doInstallIsDraggingListener()
                        }
                        lifetimeCoroutineScope.launch {
                            doInitialLayoutCorrection()
                        }
                        lifetimeCoroutineScope.launch {

                        }
                    }
                )
            }

            private suspend fun CompositionScope.doInstallIsDraggingListener() {
                val stack = mutableListOf<DragInteraction>()
                pagerLayoutState.interactionSource.interactions
                    .onStart {
                        dragListenerInstallJob.complete()
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
                coroutineScope {
                    val correctorDispatchJob = Job()
                    val correctorJob = launch {
                        dragListenerInstallJob.join()
                        pagerLayoutState.stopScroll(MutatePriority.PreventUserInput)
                        snapshotFlow { isUserDragging }.first { !it }
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
                    check(pagerLayoutState.currentPage == targetPage)
                    snapshotFlow { pagerLayoutState.isScrollInProgress }
                        .first { !it }
                    check(pagerLayoutState.currentPage == targetPage)
                    initialPageCorrectionFullyAtPageJob.complete()
                }
            }

            private suspend fun CompositionScope.doInstallInteractionListener() {

            }

            private suspend fun CompositionScope.doInstallSwipeToSeekIntentListener() {

            }
        }
    }
}
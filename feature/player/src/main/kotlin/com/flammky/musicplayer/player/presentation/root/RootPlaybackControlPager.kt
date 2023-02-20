package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.abs

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class)
@Composable
internal fun BoxScope.RootPlaybackControlPager(
    state: RootPlaybackControlPagerState
) {
    val upState = rememberUpdatedState(newValue = state)
    val upStateApplier = remember(upState.value) {
        PagerStateApplier(upState.value)
    }.apply {
        ComposePreLayout()
    }
    BoxWithConstraints {
        val pagerState = upState.value
        val baseQueue = upState.value.latestDisplayQueue
        val maskedQueue = upState.value.maskedQueue
        if (maskedQueue.list.isEmpty()) return@BoxWithConstraints
        HorizontalPager(
            state = upStateApplier.pagerLayoutState,
            count = maskedQueue.list.size,
            flingBehavior = run {
                val baseFling = PagerDefaults.flingBehavior(state = upStateApplier.pagerLayoutState)
                remember(baseFling, constraints) {
                    object : FlingBehavior {
                        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                            val constraint = constraints.maxWidth * 0.8F / 0.5f
                            val coerced =
                                (initialVelocity).coerceIn(-abs(constraint)..abs(constraint))
                            return with(baseFling) { performFling(coerced) }
                        }
                    }
                }
            },
            key = { i -> maskedQueue.list[i] }
        ) { i ->
            val id = maskedQueue.list[i]
            PagerItemScope(observeCurrentArtwork = { pagerState.observeArtwork(id) })
                .run {
                    PagerItem()
                }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
private class PagerStateApplier(
    private val state: RootPlaybackControlPagerState,
) {

    val pagerLayoutState = state.pagerLayoutState

    @Composable
    fun ComposePreLayout() {
        ListenLatestParentQueue()
    }

    @Composable
    fun ComposeAfterLayout(
        baseQueue: OldPlaybackQueue,
        displayQueue: OldPlaybackQueue
    ) {
    }

    @Composable
    private fun ListenLatestParentQueue() {
        LaunchedEffect(
            this,
            block = {
                var first = true
                snapshotFlow { state.compositionLatestQueue }
                    .collect {
                        state.latestDisplayQueue = it
                        if (it === OldPlaybackQueue.UNSET) {
                            return@collect
                        }
                        if (first) {
                            first = false
                            pagerLayoutState.scrollToPage(it.currentIndex)
                        } else {
                            val spread = (it.currentIndex - 2)..(it.currentIndex + 2)
                            if (pagerLayoutState.currentPage in spread) {
                                pagerLayoutState.animateScrollToPage(it.currentIndex)
                            } else {
                                pagerLayoutState.scrollToPage(it.currentIndex)
                            }
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
        LaunchedEffect(
            this,
            baseQueue,
            displayQueue,
            block = {
                forEachUserPageDrag { instance ->
                    check(instance.startingPage >= 0)
                    var latestPage = instance.startingPage
                    try {
                        instance.whilePointerDown {
                            snapshotFlow { pagerLayoutState.currentPage }
                                .collect { currentPage ->
                                    latestPage = currentPage
                                }
                        }
                    } finally {
                        if (instance.pointerUp && instance.startingPage != latestPage) {
                            onUserSwipe(baseQueue, displayQueue, instance.startingPage, latestPage)
                        }
                    }
                }
            }
        )
    }

    /**
     * Callback on which the user fully swipe the page and released the pointer
     */
    private fun onUserSwipe(
        baseQueue: OldPlaybackQueue,
        displayQueue: OldPlaybackQueue,
        from: Int,
        to: Int
    ) {
        check(from != to)
        if (state.latestDisplayQueue !== baseQueue || to !in baseQueue.list.indices) {
            return
        }
        state.overrideMoveNextIndex(displayQueue, to)
    }


    private suspend fun forEachUserPageDrag(
        block: suspend (instance: UserPageDragInstance) -> Unit
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
            var latestInstance: UserPageDragInstance? = null
            try {
                snapshotFlow { dragging.value }
                    .collect { start ->
                        if (start) {
                            check(latestInstance?.pointerUp != false)
                            latestInstance = UserPageDragInstance(latestDragStartInteractionAtPage)
                            block(latestInstance!!)
                        } else {
                            latestInstance?.markPointerUp()
                        }
                    }
            } catch (ce: CancellationException) {
                check(!currentCoroutineContext().isActive)
                latestInstance?.cancel()
            }
        }
    }

    private class UserPageDragInstance(
        val startingPage: Int
    ) {
        private val supervisor = SupervisorJob()

        var pointerUp: Boolean = false
            private set

        fun markPointerUp() {
            check(!pointerUp)
            pointerUp = true
            supervisor.cancel()
        }

        fun cancel() {
            supervisor.cancel()
        }

        suspend fun whilePointerDown(
            block: suspend () -> Unit
        ) = withContext(supervisor) { block() }

        suspend fun awaitPointerUpOrCancellation(
            onPointerUp: suspend () -> Unit
        ) = whilePointerDown {
            try {
                awaitCancellation()
            } finally {
                if (pointerUp) onPointerUp()
            }
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


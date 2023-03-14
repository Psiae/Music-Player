package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun rememberRootPlaybackControlPagerState(
    composition: RootPlaybackControlComposition
): PlaybackControlPagerState {
    val coroutineScope = rememberCoroutineScope()
    return remember(composition) {
        PlaybackControlPagerState(
            composition = composition,
            coroutineScope = coroutineScope,
            pagerLayoutState = PagerState(),
            observeMetadata = composition.observeTrackMetadata,
            observeArtwork = composition.observeArtwork
        )
    }
}

internal class PagerLayoutCompositionScope(
    val queueData: OldPlaybackQueue,
    private val rememberCoroutineScope: CoroutineScope,
) {
    private val initialPageCorrectionDispatchJob = Job()
    private val initialPageCorrectionScrolledAtPageJob = Job()
    private val initialPageCorrectionFullyScrolledAtPageJob = Job()
    private val userInteractionListenerInstallingJob = Job()
    private val rememberSupervisor = SupervisorJob()

    var isForgotten = false
        private set

    var currentUserDragInstance by mutableStateOf<UserDragInstance?>(null)

    var currentUserScrollInstance by mutableStateOf<UserScrollInstance?>(null)

    var userDraggingToStartIndex by mutableStateOf<Pair<Boolean, Int>?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    var currentPageInCompositionSpan by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite set

    var readyForUserScroll by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite set

    suspend inline fun awaitFullyScrollToPageCorrection() = initialPageCorrectionFullyScrolledAtPageJob.join()
    suspend inline fun awaitScrollToPageCorrection() = initialPageCorrectionScrolledAtPageJob.join()
    suspend inline fun awaitUserInteractionListener() = userInteractionListenerInstallingJob.join()

    fun onPageCorrectionDispatched() {
        initialPageCorrectionDispatchJob.complete()
    }

    fun onPageCorrectionAtPage() {
        initialPageCorrectionScrolledAtPageJob.complete()
    }

    fun onPageCorrectionFullyAtPage() {
        initialPageCorrectionFullyScrolledAtPageJob.complete()
    }

    fun onUserInteractionListenerInstalled() {
        userInteractionListenerInstallingJob.complete()
    }

    fun onForgotten() {
        Timber.d("PagerLayoutComposition=$this onForgotten()")
        isForgotten = true
        rememberCoroutineScope.cancel()
        rememberSupervisor.cancel()
        initialPageCorrectionDispatchJob.cancel()
        initialPageCorrectionScrolledAtPageJob.cancel()
        initialPageCorrectionFullyScrolledAtPageJob.cancel()
        userInteractionListenerInstallingJob.cancel()
    }

    suspend fun awaitDispose(
        block: suspend () -> Unit
    ) = withContext(rememberSupervisor) { block() }

    fun launchInRememberScope(
        block: suspend CoroutineScope.() -> Unit
    ): Job  {
        return rememberCoroutineScope.launch(rememberSupervisor, block = block)
    }

    suspend fun withRememberSupervisor(block: suspend CoroutineScope.() -> Unit) = withContext(rememberSupervisor, block)

    class UserDragInstance(
        val startPageIndex: Int
    ) {
        private val dragSupervisor = SupervisorJob()

        var pointerUp = false
            private set

        suspend fun whileDragging(
            block: suspend () -> Unit
        ) = withContext(dragSupervisor) { block() }

        fun onPointerUp() {
            pointerUp = true
            dragSupervisor.cancel()
        }

        fun onCancelled() = dragSupervisor.cancel()
    }

    class UserScrollInstance(
        private val dragInstance: UserDragInstance
    ) {
        private val completionSupervisor = SupervisorJob()

        val startPageIndex = dragInstance.startPageIndex

        val pointerUp
            get() = dragInstance.pointerUp

        suspend fun awaitScrollCompletion() {
            withContext(completionSupervisor) {
                suspendCancellableCoroutine<Unit> { /* never resume */ }
            }
        }

        suspend fun whileDraggingOrInterrupted(
            block: suspend () -> Unit
        ) = dragInstance.whileDragging { block() }

        suspend fun whileScrollingOrInterrupted(
            block: suspend () -> Unit
        ) = withContext(completionSupervisor) { block() }

        fun onInterruptedByAnotherScroll() = completionSupervisor.cancel()
        fun onCancelled() = completionSupervisor.cancel()
    }

    /**
     * run the given [block] for every User scrolling instance in this composition until forgotten
     */
    suspend fun forEachUserPageScroll(
        block: suspend (instance: UserScrollInstance) -> Unit
    ) {
        while (true) {
            try {
                val instance = awaitScrollInstance()
                block(instance)
                awaitNextScrollInstance(instance)
            } catch (ce: CancellationException) {
                if (!currentCoroutineContext().isActive) {
                    // cancelled externally
                } else if (!rememberSupervisor.isActive) {
                    // the composition is forgotten
                }
                break
            }
        }
    }

    private suspend fun awaitScrollInstance(): UserScrollInstance {
        // conflated by default, which probably is not the thing we want
        return snapshotFlow { currentUserScrollInstance }
            .first { it != null } as UserScrollInstance
    }

    private suspend fun awaitNextScrollInstance(current: UserScrollInstance): UserScrollInstance? {
        // conflated by default, which probably is not the thing we want
        return snapshotFlow { currentUserScrollInstance }
            .first { it != current }
    }
}


@OptIn(ExperimentalPagerApi::class)
internal class PlaybackControlPagerState constructor(
    val composition: RootPlaybackControlComposition,
    val coroutineScope: CoroutineScope,
    val pagerLayoutState: PagerState,
    val observeMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
) {

    val applier = PlaybackControlPagerStateApplier(this, coroutineScope)

    var currentCompositionLayoutData by mutableStateOf<PagerLayoutCompositionScope?>(null)

    suspend fun requestPlaybackMoveNext(
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectNextId: String
    ): Result<Boolean> = composition.requestSeekNextWithExpectAsync(
        expectFromIndex,
        expectFromId,
        expectNextId
    ).await()

    suspend fun requestPlaybackMovePrevious(
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ): Result<Boolean> = composition.requestSeekPreviousWithExpectAsync(
        expectFromIndex,
        expectFromId,
        expectPreviousId
    ).await()



    private val _observeMetadata = observeMetadata
    private val _observeArtwork = observeArtwork

    val compositionLatestQueue
        get() = composition.currentQueue

    fun incrementQueueReader() {
        composition.currentQueueReaderCount++
    }

    fun decrementQueueReader() {
        composition.currentQueueReaderCount--
    }

    fun observeMetadata(id: String) = _observeMetadata(id)
    fun observeArtwork(id: String) = _observeArtwork(id)

    fun overrideMoveNextIndex(
        withBase: OldPlaybackQueue,
        newIndex: Int
    ): Boolean {
        return true
    }

    fun overrideMovePreviousIndex(
        withBase: OldPlaybackQueue,
        newIndex: Int
    ): Boolean {
        return true
    }

    fun onCompositionQueueUpdated(
        new: OldPlaybackQueue
    ) {
    }
}


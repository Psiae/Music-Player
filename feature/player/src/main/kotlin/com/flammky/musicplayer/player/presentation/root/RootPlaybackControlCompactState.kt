package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


internal class RootPlaybackControlCompactState(
    val playbackController: PlaybackController,
    val onBackgroundClicked: () -> Unit,
    val onArtworkClicked: () -> Unit,
) {

    var currentLayoutComposition by mutableStateOf<RootPlaybackControlCompactComposition?>(null)

    var height by mutableStateOf<Dp>(55.dp)
        @SnapshotRead get
        @SnapshotWrite set

    /**
     * the bottom offset for the coordinator to apply
     */
    var bottomOffset by mutableStateOf<Dp>(0.dp)
        @SnapshotRead get
        @SnapshotWrite set

    /**
     * mutable freeze state for the coordinator to apply, when set to true the layout will no longer
     * collect remote updates nor dispatch user request (dropped)
     */
    var freeze by mutableStateOf<Boolean>(false)
        @SnapshotRead get
        @SnapshotWrite set
}

internal class RootPlaybackControlCompactCoordinator(
    private val state: RootPlaybackControlCompactState
) {

    private val layoutComposition = RootPlaybackControlCompactComposition()

    private var queueReaderCount = 0

    var remoteQueueSnapshot by mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)
        private set

    val freeze by derivedStateOf { state.freeze }

    fun prepareState() {
        state.currentLayoutComposition = layoutComposition
    }

    fun incrementQueueReader() {
        queueReaderCount++
    }

    fun decrementQueueReader() {
        check(queueReaderCount > 0)
        queueReaderCount--
    }

    @Suppress("NOTHING_TO_INLINE")
    @Composable
    inline fun PrepareCompose() {
        val composableCoroutineScope = rememberCoroutineScope()
        DisposableEffect(
            key1 = this,
            effect = {
                val supervisor = SupervisorJob(composableCoroutineScope.coroutineContext.job)
                val coroutineScope = CoroutineScope(composableCoroutineScope.coroutineContext + supervisor)
                val playbackObserver = state.playbackController.createPlaybackObserver()

                launchQueueCollectorForReader(coroutineScope)

                onDispose {
                    supervisor.cancel()
                    playbackObserver.dispose()
                }
            }
        )
    }

    fun onComposingTransition(state: RootPlaybackControlCompactTransitionState) {
        layoutComposition.currentTransition = state
    }

    private fun launchQueueCollectorForReader(
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            var latestCollectorJob: Job? = null
            snapshotFlow { queueReaderCount }
                .map {
                    check(it >= 0)
                    it > 0
                }
                .distinctUntilChanged()
                .collect { hasActiveReader ->
                    if (!hasActiveReader) {
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
                                    } catch (ce: CancellationException) {

                                    } finally {
                                        observer.dispose()
                                    }
                                }
                            }
                    }
                }
        }
    }
}

class RootPlaybackControlCompactTransitionState() {

}

class RootPlaybackControlCompactPagerState() {


    class LayoutComposition()
}

class RootPlaybackControlCompactControlsState() {


    class LayoutData()
}

class RootPlaybackControlCompactTimeBarState() {

    class LayoutData()
}

class RootPlaybackControlCompactBackgroundState() {

    class LayoutData()
}
package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


internal class RootPlaybackControlCompactState(
    val playbackController: PlaybackController,
    val onBackgroundClicked: () -> Unit,
    val onArtworkClicked: () -> Unit,
) {

    val coordinator = ControlCompactCoordinator(this)

    var height by mutableStateOf<Dp>(55.dp)
        @SnapshotRead get
        @SnapshotWrite set

    var width by mutableStateOf<Dp>(Dp.Infinity)
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

internal class ControlCompactCoordinator(
    private val state: RootPlaybackControlCompactState
) {

    val coordinatorSupervisorJob = SupervisorJob()

    val layoutComposition = ControlCompactComposition(
        getLayoutHeight = @SnapshotRead { state.height },
        getLayoutWidth = @SnapshotRead { state.width }
    )

    private var queueReaderCount = 0

    var remoteQueueSnapshot by mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)
        private set

    val freeze by derivedStateOf { state.freeze }

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @Composable
        inline fun ControlCompactCoordinator.PrepareCompositionInline() {
            ComposeRemoteQueueReader()
        }

        @Composable
        private fun ControlCompactCoordinator.ComposeRemoteQueueReader() {
            LaunchedEffect(
                key1 = this,
                block = {
                    launch(coordinatorSupervisorJob) {
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
                    }.join()
                }
            )
        }
    }
}

class CompactControlTransitionState(
    private val getLayoutHeight: @SnapshotRead () -> Dp,
    private val getLayoutWidth: @SnapshotRead () -> Dp
) {

    val applier: Applier = Applier(this)

    val layoutHeight: Dp
        @SnapshotRead get() = getLayoutHeight()

    val layoutWidth: Dp
        @SnapshotRead get() = getLayoutWidth()

    val animatedLayoutOffset: DpOffset by mutableStateOf(DpOffset.Zero)

    class Applier(
        private val state: CompactControlTransitionState
    ) {
        companion object {
            @Suppress("NOTHING_TO_INLINE")
            @Composable
            inline fun Applier.PrepareCompositionInline() {

            }
        }
    }
}
@OptIn(ExperimentalPagerApi::class)
class CompactControlPagerState(
    val layoutState: PagerState,
    val observeMetadata: (String) -> Flow<MediaMetadata?>
) {

    val applier = Applier()

    var currentLayoutComposition by mutableStateOf<LayoutComposition?>(null)

    class Applier {
        companion object {
            @Suppress("NOTHING_TO_INLINE")
            @Composable
            inline fun Applier.PrepareCompositionInline() {

            }
        }
    }

    class LayoutComposition(
        val queueData: OldPlaybackQueue
    ) {

        var userScrollReady by mutableStateOf(false)

        companion object {

            @Composable
            fun LayoutComposition.OnLayoutComposed() {
                val coroutineScope = rememberCoroutineScope()
                remember(this) {
                    coroutineScope.launch {  }
                }
            }
        }
    }
}

class CompactButtonControlsState() {

    val applier = Applier()

    class Applier {
        companion object {
            @Suppress("NOTHING_TO_INLINE")
            @Composable
            inline fun Applier.PrepareCompositionInline() {

            }
        }
    }


    class LayoutData()
}

class CompactTimeBarState() {

    val applier = Applier()

    class Applier {

    }

    class LayoutData()
}

class CompactBackgroundState() {

    val applier = Applier()

    class Applier {

    }

    class LayoutData()
}
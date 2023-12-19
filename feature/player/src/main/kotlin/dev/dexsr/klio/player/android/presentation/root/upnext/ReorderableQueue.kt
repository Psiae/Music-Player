package dev.dexsr.klio.player.android.presentation.root.upnext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.root.main.queue.QueueContainerTransitionState
import com.flammky.musicplayer.player.presentation.root.main.queue.ReorderableQueueLazyColumn
import com.flammky.musicplayer.player.presentation.root.main.queue.ReorderableQueueLazyColumnDataSource
import com.flammky.musicplayer.player.presentation.root.main.queue.ReorderableQueueLazyColumnIntents
import com.flammky.musicplayer.player.presentation.root.main.queue.rememberReorderableQueueLazyColumnState
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackControlScreenState
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Composable
fun ReorderableQueue(
    modifier: Modifier,
    container: PlaybackControlScreenState,
    visible: Boolean,
    bottomSpacing: Dp,
    backgroundColor: Color
) {
    OldReorderableQueue(modifier = modifier, container, visible, bottomSpacing, backgroundColor)
}

@Composable
private fun OldReorderableQueue(
    modifier: Modifier,
    container: PlaybackControlScreenState,
    visible: Boolean,
    bottomSpacing: Dp,
    backgroundColor: Color
) {
    val queueState = rememberReorderableQueueLazyColumnState(
        intents = remember(container.playbackController) {
            ReorderableQueueLazyColumnIntents(
                requestSeekIndexAsync = container.playbackController::seekToIndexAsync,
                requestMoveQueueItemAsync = container.playbackController::moveQueueItemAsync
            )
        },
        dataSource = remember(container.playbackController) {
            ReorderableQueueLazyColumnDataSource(
                observeQueue = {
                    flow {
                        val disposables = mutableListOf<DisposableHandle>()
                        val qChannel = Channel<OldPlaybackQueue>(Channel.CONFLATED)
                        try {
                            container.playbackController.invokeOnTimelineChanged(Int.MAX_VALUE) { tl, step ->
                                qChannel.trySend(OldPlaybackQueue(tl.items, tl.currentIndex))
                            }.also { disposables.add(it) }
                            for (q in qChannel) {
                                emit(q)
                            }
                        } finally {
                            disposables.forEach { it.dispose() }
                        }
                    }
                },
                observeTrackMetadata = container.mediaMetadataProvider::oldDescriptionAsFlow,
                observeTrackArtwork = { id ->
                    container.mediaMetadataProvider.artworkAsFlow(id)
                        .map { art ->
                            art?.image?.value
                        }
                }
            )
        }
    )
    val state = rememberOldQueueContainerTransitionState()
    val upBackgroundColor = rememberUpdatedState(newValue = backgroundColor)
    val upBottomContentPadding = rememberUpdatedState(newValue = bottomSpacing)
    BoxWithConstraints(modifier.fillMaxSize()) {

        val upState = rememberUpdatedState(
            state.apply {
                updateConstraints(constraints, immediate = true)
            }
        )
        val upQstate = rememberUpdatedState(
            queueState
                .apply {
                    topContentPadding = 0.dp
                    bottomContentPadding = upBottomContentPadding.value
                }
        )
        val upStagedHeight = rememberUpdatedState(newValue = upState.value.stagedHeightPx)
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { upState.value.onRender(upStagedHeight.value) }
        ) {
            if (upStagedHeight.value > 0) {
                ReorderableQueueLazyColumn(
                    transitionState = upState.value,
                    state = upQstate.value,
                    backgroundColor = upBackgroundColor.value
                )
            }
        }
    }
    DisposableEffect(
        key1 = state,
        effect = {

            onDispose { state.onRender(0) }
        }
    )
    LaunchedEffect(
        visible,
        block = {
            // render lazily
            if (visible) state.show(immediate = true)
            // hide here will detach the composition completely,
            // we would rather to just "freeze" it from update instead
            /*else state.hide(immediate = false)*/
        }
    )
}

@Composable
private fun rememberOldQueueContainerTransitionState(): QueueContainerTransitionState {
    val cs = rememberCoroutineScope()

    return rememberSaveable(
        cs,
        saver = QueueContainerTransitionState.Saver(parent = null, uiCoroutineScope = cs)
    ) {
        QueueContainerTransitionState(
            parent = null,
            uiCoroutineScope = cs,
            initialRememberFullTransitionRendered = false,
            isRestoredInstance = false
        )
    }
}
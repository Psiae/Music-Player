package com.flammky.musicplayer.player.presentation.root.main.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.isDarkAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.player.presentation.root.main.PlaybackControlScreenCoordinator

@Composable
fun PlaybackControlScreenCoordinator.QueueScreenRenderScope.PlaybackQueueScreen() {
    val dataSourceKey = remember(dataSource) { Any() }
    val intentsKey = remember(intents) { Any() }
    val compactState = rememberCompactControlState(
        intents = remember(intentsKey) {
            CompactControlIntents(
                seekNext = { intents.requestSeekNextAsync() },
                seekPrevious = { intents.requestSeekPreviousAsync() },
                play = { intents.requestPlayAsync() },
                pause = { intents.requestPauseAsync() }
            )
        },
        dataSource = remember(dataSourceKey) {
            CompactControlDataSource(
                dataSource.observeQueue,
                dataSource.observePlaybackProperties,
                dataSource.observeArtwork,
                dataSource.observeTrackMetadata
            )
        },
    )
    val queueState = rememberReorderableQueueLazyColumnState(
        intents = remember(intentsKey) {
            ReorderableQueueLazyColumnIntents(
                requestSeekIndexAsync = intents.requestSeekIndexAsync,
                requestMoveQueueItemAsync = intents.requestMoveQueueItemAsync
            )
        },
        dataSource = remember(dataSourceKey) {
            ReorderableQueueLazyColumnDataSource(
                observeQueue = dataSource.observeQueue,
                observeTrackMetadata = dataSource.observeTrackMetadata,
                observeTrackArtwork = dataSource.observeArtwork
            )
        }
    )
    PlaceContents(
        container = @Composable { state, content ->
            QueueTransition(
                state = state,
                content = content
            )
        },
        background = @Composable {
            QueueScreenBackground()
        },
        compact = @Composable {
            CompactControl(state = compactState, transitionState = transitionState)
        },
        queue = @Composable { transitionState ->
            val statusBarInset = with(LocalDensity.current) {
                WindowInsets.statusBars.getTop(this).toDp()
            }
            val navigationBarInset = with(LocalDensity.current) {
                WindowInsets.navigationBars.getBottom(this).toDp()
            }
            ReorderableQueueLazyColumn(
                transitionState = transitionState,
                state = queueState.apply {
                    topContentPadding = maxOf(statusBarInset, compactState.stagedLayoutHeight)
                    bottomContentPadding = navigationBarInset
                }
            )
        }
    )
}

@Composable
private fun PlaybackControlScreenCoordinator.QueueScreenRenderScope.PlaceContents(
    container: @Composable (QueueContainerTransitionState, content: @Composable () -> Unit) -> Unit,
    background: @Composable () -> Unit,
    compact: @Composable () -> Unit,
    queue: @Composable (QueueContainerTransitionState) -> Unit
) = Box(modifier = Modifier.fillMaxSize()) {
    container(transitionState) {
        background()
        queue(transitionState)
        Column {
            compact()
            if (transitionState.rememberFullTransitionRendered && !Theme.isDarkAsState().value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme.surfaceVariantColorAsState().value)
                )
            }
        }
    }
}
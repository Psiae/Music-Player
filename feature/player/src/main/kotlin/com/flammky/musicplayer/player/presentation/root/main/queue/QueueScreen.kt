package com.flammky.musicplayer.player.presentation.root.main.queue

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlCompact
import com.flammky.musicplayer.player.presentation.root.main.PlaybackControlScreenCoordinator
import com.flammky.musicplayer.player.presentation.root.rememberRootPlaybackControlCompactState

@Composable
fun PlaybackControlScreenCoordinator.QueueScreenRenderScope.PlaybackQueueScreen() {
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
            RootPlaybackControlCompact(
                state = rememberRootPlaybackControlCompactState(
                    user = dataSource.user,
                    onBaseClicked = null,
                    onArtworkClicked = null
                ).apply {
                    bottomSpacing = with(LocalDensity.current) {
                        WindowInsets.navigationBars.getBottom(this).toDp()
                    }
                }
            )
        },
        queue = @Composable { transitionState ->
            val statusBarInset = with(LocalDensity.current) {
                WindowInsets.statusBars.getTop(this).toDp()
            }
            ReorderableQueueLazyColumn(
                // TODO:
                getPlaybackControlContentPadding = { PaddingValues(bottom = 55.dp) },
                getStatusBarInset = { statusBarInset },
                transitionState = transitionState
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
        compact()
    }
}
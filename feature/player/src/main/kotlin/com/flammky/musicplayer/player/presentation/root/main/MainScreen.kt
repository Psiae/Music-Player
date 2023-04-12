package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.isDarkAsState


@Composable
fun PlaybackControlScreenCoordinator.MainRenderScope.PlaybackControlMainScreen() {
    PlaceContents(
        modifier = modifier,
        transition = @Composable { state, content ->
            MainTransition(
                state = state,
                content = content
            )
        },
        background = @Composable {
            MainScreenBackground(
                modifier = Modifier.fillMaxSize(),
                dataSource = remember(dataSource) {
                    MainScreenBackgroundDataSource(dataSource.observePalette)
                },
                themeInfo = run {
                    val dark = Theme.isDarkAsState().value
                    val back = Theme.backgroundColorAsState().value
                    val absBack = Theme.absoluteBackgroundColorAsState().value
                    MainScreenThemeInfo(dark, back, absBack)
                }
            )
        },
        toolbar = @Composable {
            MainScreenToolBar(dismiss = intents.dismiss)
        },
        content = @Composable {
            MainScreenContent()
        }
    )
}

@Composable
private fun PlaybackControlScreenCoordinator.MainRenderScope.PlaceContents(
    modifier: Modifier,
    transition: @Composable (state: MainContainerTransitionState, content: @Composable () -> Unit) -> Unit,
    background: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
    content: @Composable () -> Unit
) = Box(modifier = modifier) {
    transition(
        state = transitionState,
        content = {
            background()
            Column(modifier = Modifier.fillMaxSize()) {
                toolbar()
                content()
            }
        }
    )
}
package com.kylentt.mediaplayer.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kylentt.mediaplayer.core.util.getDisplayTitle
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor
import timber.log.Timber

@Composable
@Preview
fun HomeScreen(
    controller: ControllerViewModel = hiltViewModel(),
    vm: HomeViewModel = hiltViewModel(),
) {
    Timber.d("ComposeDebug HomeScreen")
    val currentlyPlaying by remember { controller.playerCurrentMediaItem }
    val currentPlayState by remember { controller.playerCurrentPlaystate }
    val textColor = DefaultColor.getDNTextColor()

    HomeScreenLayout(
        textColor = textColor,
        currentlyPlaying = currentlyPlaying.getDisplayTitle,
        currentlyPlayingState = currentPlayState
    )
}

@Composable
fun HomeScreenLayout(
    textColor: Color,
    currentlyPlaying: CharSequence?,
    currentlyPlayingState: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.padding(top = 15.dp))
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Home Screen", color = textColor)
            Text(text = "Currently Playing: $currentlyPlaying", color = textColor)
            Text(text = "Currently Playing State: $currentlyPlayingState", color = textColor)
        }
    }
}
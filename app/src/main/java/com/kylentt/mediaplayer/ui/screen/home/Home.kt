package com.kylentt.mediaplayer.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kylentt.mediaplayer.core.exoplayer.getDisplayTitle
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor

@Composable
fun HomeScreen(
    controller: ControllerViewModel = hiltViewModel(),
    vm: HomeViewModel = hiltViewModel(),
) {
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
    currentlyPlayingState: String,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(state = scroll),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
package com.kylentt.mediaplayer.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor
import timber.log.Timber

@Composable
fun HomeScreen(
    controller: ControllerViewModel = hiltViewModel(),
    vm: HomeViewModel = hiltViewModel(),
) {
    Timber.d("ComposeDebug HomeScreen")

    val currentlyPlaying by remember { controller.playerCurrentMediaItem }
    val currentPlayState by remember { controller.playerCurrentPlaystate }
    val textColor = DefaultColor.getDNTextColor()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home Screen", color = textColor)
        Text(text = "Currently Playing: ${currentlyPlaying.getDisplayTitle}", color = textColor)
        Text(text = "Currently Playing State: $currentPlayState", color = textColor)
    }
}
package com.kylentt.mediaplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel

@Composable
fun HomeScreen(vm: ControllerViewModel) {
    val songList = vm.songList.collectAsState()
    val currentlyPlaying = vm.playerCurrentMediaItem
    val currentPlayState = vm.playerCurrentPlaystate
    val textColor = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Home Screen", color = textColor)
            Text(text = "Song Found: ${songList.value.size}", color = textColor)
            Text(text = "Currently Playing: ${currentlyPlaying.value.getDisplayTitle}", color = textColor)
            Text(text = "Currently Playing State: ${currentPlayState.value}", color = textColor)
        }
    }
}

@Composable
@Preview
fun HomeScreenPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Home Screen", color = Color.LightGray)
            Text(text = "Song Found: ", color = Color.LightGray)
            Text(text = "Currently Playing: ", color = Color.LightGray)
        }
    }
}
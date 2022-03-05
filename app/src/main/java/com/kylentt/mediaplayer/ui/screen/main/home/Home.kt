package com.kylentt.mediaplayer.ui.screen.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    controller: ControllerViewModel = hiltViewModel(),
    vm: HomeViewModel = hiltViewModel(),
) {
    val currentlyPlaying = controller.playerCurrentMediaItem
    val currentPlayState = controller.playerCurrentPlaystate
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
            Text(text = "Currently Playing: ${currentlyPlaying.value.getDisplayTitle}", color = textColor)
            Text(text = "Currently Playing State: ${currentPlayState.value}", color = textColor)
        }
    }
}
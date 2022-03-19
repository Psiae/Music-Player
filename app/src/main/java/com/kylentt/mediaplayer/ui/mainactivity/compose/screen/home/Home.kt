package com.kylentt.mediaplayer.ui.mainactivity.compose.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kylentt.mediaplayer.core.util.handler.getDisplayTitle
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.HomeAppBar
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.HomeCard
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.StatusBarSpacer
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.DefaultColor

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
            .verticalScroll(state = scroll),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusBarSpacer()
        HomeAppBar()
        HomeBarCardSpacer()
        HomeCard()


        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Home Screen", color = textColor)
            Text(text = "Currently Playing: $currentlyPlaying", color = textColor)
            Text(text = "Currently Playing State: $currentlyPlayingState", color = textColor)
        }
        Spacer(modifier = Modifier.height(1000.dp))
        Text(text = "END OF COLUMN")
    }
}

@Composable
fun HomeBarCardSpacer() {
    Spacer(modifier = Modifier.height(15.dp))
}
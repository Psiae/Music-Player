package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components.HomeAppBar
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components.util.StatusBarSpacer
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.ColorHelper
import timber.log.Timber

@Composable
internal fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
) {
    val currentlyPlaying by remember { vm.playerItemState }
    val currentPlayState by remember { vm.playerPlaybackState }
    val textColor = ColorHelper.getDNTextColor()

    Timber.d("ComposeDebug HomeScreen")

    HomeScreenLayout(
        textColor = textColor,
        currentlyPlaying = currentlyPlaying.getDisplayTitle,
        currentlyPlayingState = currentPlayState.toStrState()
    )
}

@Composable
fun HomeScreenLayout(
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    currentlyPlaying: CharSequence? = "My Song",
    currentlyPlayingState: String = "State Unit",
) {

    Timber.d("ComposeDebug HomeScreenLayout")

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
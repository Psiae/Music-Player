package com.flammky.musicplayer.player.presentation.root.main.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState

@Composable
fun QueueScreenBackground() = Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Theme.backgroundColorAsState().value
    )
)
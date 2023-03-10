package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.player.presentation.root.CompactControlTimeBarState.Applier.Companion.ComposeLayout
import com.flammky.musicplayer.player.presentation.root.CompactControlTimeBarState.CompositionScope.Companion.bufferedPositionTimeBarValue
import com.flammky.musicplayer.player.presentation.root.CompactControlTimeBarState.CompositionScope.Companion.positionTimeBarValue

@Composable
fun BoxScope.CompactControlTimeBar(
    state: CompactControlTimeBarState
) {
    state.applier.ComposeLayout {

        BoxWithConstraints(
            modifier = Modifier
                .background(
                    Theme.surfaceVariantColorAsState().value
                        .copy(alpha = 0.7f)
                        .compositeOver(Theme.absoluteBackgroundColorAsState().value)
                )
                .height((2.5).dp)
        ) {

            LinearProgressIndicator(
                modifier = remember { Modifier.fillMaxSize() },
                color = Theme.backgroundContentColorAsState().value.copy(alpha = 0.25f),
                backgroundColor = Color.Transparent,
                progress = bufferedPositionTimeBarValue(width = maxWidth)
            )

            LinearProgressIndicator(
                modifier = remember { Modifier.fillMaxSize() },
                color = Theme.backgroundContentColorAsState().value,
                backgroundColor = Color.Transparent,
                progress = positionTimeBarValue(width = maxWidth)
            )
        }
    }
}
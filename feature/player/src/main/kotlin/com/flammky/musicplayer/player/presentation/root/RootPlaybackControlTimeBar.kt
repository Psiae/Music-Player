package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import com.flammky.musicplayer.player.presentation.main.compose.Slider
import com.flammky.musicplayer.player.presentation.main.compose.SliderDefaults
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlTimeBarState.Applier.Companion.ComposeLayout
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlTimeBarState.CompositionScope.Companion.animatedSliderValue
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlTimeBarState.CompositionScope.Companion.timeBarTextDurationDisplayValue
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlTimeBarState.CompositionScope.Companion.timeBarTextPositionDisplayValue
import kotlin.time.Duration

@Composable
fun RootPlaybackControlTimeBar(
    state: RootPlaybackControlTimeBarState,
    trackWidth: Dp
) {
    state.applier.ComposeLayout {

        Column(
            modifier = remember(trackWidth) {
                Modifier.fillMaxWidth()
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                modifier = remember {
                    Modifier
                        .width(trackWidth)
                        .height(14.dp)
                },
                value = animatedSliderValue(sliderWidth = trackWidth),
                onValueChange = ::onUserScrubToPosition,
                onValueChangeFinished = ::onScrubFinished,
                trackHeight = 6.dp,
                thumbSize = 12.dp,
                // TODO: find a way to put buffer position
                colors = SliderDefaults.colors(
                    activeTrackColor = Theme.surfaceContentColorAsState().value,
                    thumbColor = Theme.backgroundContentColorAsState().value
                )
            )
            TimeBarText(
                sliderWidth = trackWidth,
                scrubThumbRadius = 6.dp,
                position = timeBarTextPositionDisplayValue(),
                duration = timeBarTextDurationDisplayValue()
            )
        }
    }
}

@Composable
private fun TimeBarText(
    sliderWidth: Dp,
    scrubThumbRadius: Dp,
    position: Duration,
    duration: Duration
) {

    Row(
        modifier = Modifier
            .width(sliderWidth - scrubThumbRadius)
            .padding(top = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val formattedProgress: String = remember(position, duration) {
            val seconds =
                if (position.isNegative() || duration.isNegative()) 0
                else position.inWholeSeconds
            if (seconds > 3600) {
                String.format(
                    "%02d:%02d:%02d",
                    seconds / 3600,
                    seconds % 3600 / 60,
                    seconds % 60
                )
            } else {
                String.format(
                    "%02d:%02d",
                    seconds / 60,
                    seconds % 60
                )
            }
        }

        Text(
            text = formattedProgress,
            color = Theme.backgroundContentColorAsState().value,
            style = MaterialTheme.typography.bodySmall
        )

        val formattedDuration: String = remember(duration) {
            val seconds =
                if (duration.isNegative() || duration.isInfinite()) 0
                else duration.inWholeSeconds
            if (seconds > 3600) {
                String.format(
                    "%02d:%02d:%02d",
                    seconds / 3600,
                    seconds % 3600 / 60,
                    seconds % 60
                )
            } else {
                String.format(
                    "%02d:%02d",
                    seconds / 60,
                    seconds % 60
                )
            }
        }
        Text(
            text = formattedDuration,
            color = Theme.backgroundContentColorAsState().value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
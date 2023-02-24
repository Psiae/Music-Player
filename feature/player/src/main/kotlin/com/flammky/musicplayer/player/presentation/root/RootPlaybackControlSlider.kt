package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import com.flammky.musicplayer.player.presentation.main.compose.Slider
import com.flammky.musicplayer.player.presentation.main.compose.SliderDefaults
import kotlin.time.Duration

@Composable
internal fun RootPlaybackControlSlider(
    state: RootPlaybackControlSliderState
) {
    val coroutineScope = rememberCoroutineScope()
    val applier = remember(state) {
        RootPlaybackControlSliderApplier(state, coroutineScope)
            .apply {
                prepareState()
            }
    }.apply {
        PrepareCompose()
    }

    BoxWithConstraints {
        val composition = state.layoutComposition
            ?: run {
                applier.skipLayoutComposition()
                return@BoxWithConstraints
            }
        applier.StartLayoutComposition(composition)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val sliderWidth = remember(this@BoxWithConstraints.maxWidth) {
                this@BoxWithConstraints.maxWidth * 0.85f
            }
            Slider(
                modifier = remember {
                    Modifier.height(14.dp)
                }.runRemember(sliderWidth) {
                    width(sliderWidth)
                },
                enabled = composition.readyForScrub,
                value = composition.sliderDisplayValue(),
                onValueChange = remember(composition) {
                    { value ->  composition.onValueChange(value) }
                },
                onValueChangeFinished = remember(composition) {
                    { composition.onValueChangeFinished() }
                },
                trackHeight = 6.dp,
                thumbSize = 12.dp,
                colors = SliderDefaults.colors(
                    activeTrackColor = Theme.surfaceContentColorAsState().value,
                    thumbColor = Theme.backgroundContentColorAsState().value
                )
            )
            PlaybackPositionSliderText(
                sliderWidth = sliderWidth,
                scrubThumbRadius = 14.dp,
                position = composition.sliderTextPositionValue(),
                duration = composition.sliderTextDurationDisplayValue()
            )
        }
        remember(composition, constraints.maxWidth) {
            composition
                .apply {
                    layoutWidthDp = maxWidth
                }
        }
        applier.EndLayoutComposition(composition)
    }
}

@Composable
private fun PlaybackPositionSliderText(
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
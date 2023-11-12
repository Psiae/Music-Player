package dev.dexsr.klio.player.android.presentation.root

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.player.R
import dev.dexsr.klio.base.compose.NoOpPainter
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState

@Composable
fun PlaybackControlMainScreenControls(
    modifier: Modifier,
    state: PlaybackControlMainScreenState
) {
    val progressionState = remember(state) {
        state.playbackController.playbackProgressionStateAsFlow()
    }.collectAsState(initial = PlaybackProgressionState.UNSET)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            val canDoAction = progressionState.value.canToggleShuffleMode
            Box(modifier = Modifier.size(40.dp)) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (interactionSource.collectIsPressedAsState().value) 30.dp else 32.dp)
                        .then(
                            Modifier
                                .clickable(
                                    enabled = canDoAction,
                                    indication = null,
                                    interactionSource = interactionSource,
                                    onClick = { state.playbackController.toggleShuffleAsync() }
                                )
                        ),
                    painter = painterResource(
                        id = R.drawable.ios_glyph_shuffle_100
                    ),
                    contentDescription = "prev",
                    tint = MD3Theme.backgroundContentColorAsState().value.copy(
                        alpha = if (!canDoAction) {
                            0.38f
                        }  else if (progressionState.value.shuffleMode == 0) {
                            0.68f
                        } else {
                            1f
                        }
                    )
                )
            }
        }

        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            val canDoAction = progressionState.value.canSeekPrevious
            Box(modifier = Modifier.size(40.dp)) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (interactionSource.collectIsPressedAsState().value) 30.dp else 32.dp)
                        .then(
                            Modifier
                                .clickable(
                                    enabled = canDoAction,
                                    indication = null,
                                    interactionSource = interactionSource,
                                    onClick = { state.playbackController.seekToPreviousAsync() }
                                )
                        ),
                    painter = painterResource(
                        id = R.drawable.ios_glyph_seek_previous_100
                    ),
                    contentDescription = "prev",
                    tint = MD3Theme.backgroundContentColorAsState().value.copy(
                        alpha = if (!canDoAction) {
                            0.38f
                        }  else {
                            1f
                        }
                    )
                )
            }
        }

        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            val showPlay = !progressionState.value.playWhenReady
            val canDoAction = if (showPlay)
                progressionState.value.canPlay
            else
                progressionState.value.playWhenReady
            Box(modifier = Modifier.size(40.dp)) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (interactionSource.collectIsPressedAsState().value) 36.dp else 38.dp)
                        .then(
                            Modifier
                                .clickable(
                                    enabled = canDoAction,
                                    indication = null,
                                    interactionSource = interactionSource,
                                    onClick = {
                                        if (showPlay) {
                                            state.playbackController.playAsync()
                                        } else {
                                            state.playbackController.pauseAsync()
                                        }
                                    }
                                )
                        ),
                    painter = painterResource(
                        id = if (showPlay) {
                            R.drawable.ios_glyph_play_100
                        } else {
                            R.drawable.ios_glyph_pause_100
                        }
                    ),
                    contentDescription = if (showPlay) "play" else "pause",
                    tint = MD3Theme.backgroundContentColorAsState().value.copy(
                        alpha = if (!canDoAction) {
                            0.38f
                        }  else {
                            1f
                        }
                    )
                )
            }
        }

        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            val canSeekNext = progressionState.value.canSeekNext
            Box(modifier = Modifier.size(40.dp)) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (interactionSource.collectIsPressedAsState().value) 30.dp else 32.dp)
                        .then(
                            Modifier
                                .clickable(
                                    enabled = canSeekNext,
                                    indication = null,
                                    interactionSource = interactionSource,
                                    onClick = { state.playbackController.seekToNextAsync() }
                                )
                        ),
                    painter = painterResource(
                        id = R.drawable.ios_glyph_seek_next_100
                    ),
                    contentDescription = "next",
                    tint = MD3Theme.backgroundContentColorAsState().value.copy(
                        alpha = if (!canSeekNext) {
                            0.38f
                        }  else {
                            1f
                        }
                    )
                )
            }
        }
        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            Box(modifier = Modifier.size(40.dp)) {
                val repeatMode = progressionState.value.repeatMode
                val canToggleRepeat = progressionState.value.canToggleRepeat

                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(if (interactionSource.collectIsPressedAsState().value) 30.dp else 32.dp)
                        .then(when (repeatMode) {
                            0, 1, 2 -> {
                                Modifier
                                    .clickable(
                                        enabled = canToggleRepeat,
                                        indication = null,
                                        interactionSource = interactionSource,
                                        onClick = { state.playbackController.toggleRepeatAsync() }
                                    )
                            }
                            else -> Modifier
                        }),
                    painter = when(repeatMode) {
                        0, 2 -> painterResource(id = R.drawable.ios_glyph_repeat_100)
                        1 -> painterResource(R.drawable.ios_glyph_repeat_one_100)
                        else -> NoOpPainter
                    },
                    contentDescription = "toggle repeat mode",
                    tint = MD3Theme.backgroundContentColorAsState().value.copy(
                        alpha = if (!canToggleRepeat) {
                            0.38f
                        } else if (repeatMode == 0) {
                            0.68f
                        } else {
                            1f
                        }
                    )
                )
            }
        }
    }
}
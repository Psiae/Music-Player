package com.flammky.musicplayer.player.presentation.root

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.darkSurfaceContentColorAsState
import com.flammky.musicplayer.base.theme.compose.lightSurfaceContentColorAsState
import com.flammky.musicplayer.player.R
import com.flammky.musicplayer.player.presentation.root.CompactButtonControlsState.Applier.Companion.ComposeLayout
import com.flammky.musicplayer.player.presentation.root.CompactButtonControlsState.CompositionScope.Companion.showPlayButton

@Composable
fun CompactControlButtons(
    state: CompactButtonControlsState
) {
    state.applier.ComposeLayout {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(30.dp),
                contentAlignment = Alignment.Center
            ) {}
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(30.dp),
                contentAlignment = Alignment.Center
            ) {}
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(30.dp),
                contentAlignment = Alignment.Center
            ) {
                // IsPlaying callback from mediaController is somewhat not accurate
                val icon =
                    if (showPlayButton()) {
                        R.drawable.play_filled_round_corner_32
                    } else {
                        R.drawable.pause_filled_narrow_rounded_corner_32
                    }

                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val size by animateDpAsState(targetValue =  if (pressed) 18.dp else 21.dp)
                Icon(
                    modifier = Modifier
                        .size(size)
                        .playButtonInteractionModifier(interactionSource),
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isSurfaceDark()) {
                        Theme.darkSurfaceContentColorAsState().value
                    } else {
                        Theme.lightSurfaceContentColorAsState().value
                    },
                )
            }
        }
    }
}
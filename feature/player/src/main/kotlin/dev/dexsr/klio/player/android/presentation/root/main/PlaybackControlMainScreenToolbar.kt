package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.surfaceContentColorAsState

@Composable
fun PlaybackControlMainScreenToolbar(
    modifier: Modifier,
    state: PlaybackControlMainScreenState
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {

        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = state.userDismissible,
                        onClick = state::onDismissButtonClicked
                    )
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .composed {
                            Modifier
                                .size(
                                    if (interactionSource.collectIsPressedAsState().value) 26.dp
                                    else 28.dp
                                )
                        },
                    painter = painterResource(id = com.flammky.musicplayer.player.R.drawable.ios_glyph_expand_arrow_down_100),
                    contentDescription = "dismiss",
                    tint = MD3Theme.surfaceContentColorAsState().value
                )
            }
        }

        // decide what to put in the middle
        Spacer(modifier = Modifier.weight(2f))

        run {
            val interactionSource = remember {
                MutableInteractionSource()
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = false,
                        onClick = {  }
                    )
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .composed {
                            Modifier
                                .size(
                                    if (interactionSource.collectIsPressedAsState().value) 26.dp
                                    else 28.dp
                                )
                        },
                    painter = painterResource(id = com.flammky.musicplayer.player.R.drawable.more_vert_48px),
                    contentDescription = "more",
                    tint = MD3Theme.surfaceContentColorAsState().value
                )
            }
        }

    }
}
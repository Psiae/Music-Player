package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.player.R

@Composable
fun MainScreenToolBar(
    dismiss: () -> Unit
) {
    PlaceContents(
        dismiss = MainScreenToolbarDismissContents(
            icon = { modifier, color ->
                Icon(
                    modifier = modifier
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ios_glyph_expand_arrow_down_100),
                    contentDescription = "close",
                    tint = color
                )
            },
            onClick = dismiss
        ),
        more = MainScreenToolbarMoreContents(
            icon = { modifier, color ->
                Icon(
                    modifier = modifier
                        .align(Alignment.Center),
                    painter = painterResource(id = com.flammky.musicplayer.base.R.drawable.more_vert_48px),
                    contentDescription = "more",
                    tint = color
                )
            }
        )
    )
}

private data class MainScreenToolbarDismissContents(
    val icon: @Composable BoxScope.(modifier: Modifier, tint: Color) -> Unit,
    val onClick: () -> Unit
)

private data class MainScreenToolbarMoreContents(
    val icon: @Composable BoxScope.(modifier: Modifier, tint: Color) -> Unit
)

@Composable
private fun PlaceContents(
    dismiss: MainScreenToolbarDismissContents,
    more: MainScreenToolbarMoreContents
) = BoxWithConstraints(modifier = Modifier.statusBarsPadding()) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .align(Alignment.Center)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        run dismiss@ {
            val interactionSource = MutableInteractionSource()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = true,
                        onClick = dismiss.onClick
                    ),
                content = {
                    with(dismiss) {
                        icon(
                            modifier = Modifier.composed {
                                this
                                    .size(if (interactionSource.collectIsPressedAsState().value) 26.dp else 28.dp)
                                    .align(Alignment.Center)
                            },
                            tint = Theme.backgroundContentColorAsState().value
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.weight(2f))
        run more@ {
            val interactionSource = MutableInteractionSource()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = true,
                        onClick = dismiss.onClick
                    ),
                content = {
                    with(more) {
                        icon(
                            modifier = Modifier.composed {
                                size(if (interactionSource.collectIsPressedAsState().value) 26.dp else 28.dp)
                            },
                            tint = Theme.backgroundContentColorAsState().value
                        )
                    }
                }
            )
        }
    }
}
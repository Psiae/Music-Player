package com.flammky.musicplayer.player.presentation.root.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.player.presentation.root.runRemember

@Composable
fun CompactControl(
    state: CompactControlState
) = state.coordinator.SetContent(
    description = {
        placeLayout(
            artwork = { art -> ArtworkLayout(art) },
            text = { title, subtitle -> TextLayout(title, subtitle) }
        )
    },
    buttons = {
        placeLayout(
            previousButton = { clickable, onClick, iconSize ->
                SeekPreviousButton(clickable, onClick, iconSize)
            },
            nextButton = { clickable, onClick, iconSize ->
                SeekNextButton(clickable, onClick, iconSize)
            }
        )
    }
)

@Composable
private fun ArtworkLayout(
    data: Any?
) {
    val context = LocalContext.current
    val model = remember(context, data) {
        ImageRequest.Builder(context)
            .data(data)
    }
    AsyncImage(
        model = model,
        contentDescription = "artwork"
    )
}

@Composable
private fun TextLayout(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = title)
        Text(text = subtitle)
    }
}

@Composable
private fun SeekPreviousButton(
    clickable: Boolean,
    onClick: () -> Unit,
    iconSize: Dp
) {
    Icon(
        modifier = Modifier.runRemember(clickable, onClick) {
            clickable(enabled = clickable, onClick = onClick)
        }.runRemember(iconSize) {
            size(iconSize)
        },
        painter = painterResource(id = com.flammky.musicplayer.player.R.drawable.ios_glyph_seek_previous_100),
        contentDescription = "previous",
        tint = Theme.backgroundContentColorAsState().value
    )
}

@Composable
private fun SeekNextButton(
    clickable: Boolean,
    onClick: () -> Unit,
    iconSize: Dp
) {
    Icon(
        modifier = Modifier.runRemember(clickable, onClick) {
            Modifier.clickable(enabled = clickable, onClick = onClick)
        }.runRemember(iconSize) {
            size(iconSize)
        },
        painter = painterResource(id = com.flammky.musicplayer.player.R.drawable.ios_glyph_seek_next_100),
        contentDescription = "next",
        tint = Theme.backgroundContentColorAsState().value
    )
}

class CompactControlState() {
    val coordinator = CompactControlCoordinator.of(this)
    val layoutCoordinator = CompactControlLayoutCoordinator.of(this)
}

class CompactControlCoordinator private constructor(
    private val state: CompactControlState
) {

    interface DescriptionLayoutScope {
        fun placeLayout(
            artwork: @Composable (data: Any?) -> Unit,
            text: @Composable (title: String, subtitle: String) -> Unit
        )
    }

    interface ButtonsLayoutScope {
        fun placeLayout(
            previousButton: @Composable (
                clickable: Boolean,
                onClick: () -> Unit,
                iconSize: Dp
            ) -> Unit,
            nextButton: @Composable (
                clickable: Boolean,
                onClick: () -> Unit,
                iconSize: Dp
            ) -> Unit
        )
    }

    class DescriptionLayoutState(
        val artwork: @Composable (data: Any?) -> Unit,
        val text: @Composable (title: String, subtitle: String) -> Unit,
    )

    class ButtonsLayoutState(
        val previousButton: @Composable (
            clickable: Boolean,
            onClick: () -> Unit,
            iconSize: Dp
        ) -> Unit,
        val nextButton: @Composable (
            clickable: Boolean,
            onClick: () -> Unit,
            iconSize: Dp
        ) -> Unit,
    )



    @Composable
    fun SetContent(
        description: @Composable DescriptionLayoutScope.() -> Unit,
        buttons: @Composable ButtonsLayoutScope.() -> Unit
    ) {
        val width = state.layoutCoordinator.layoutWidth()
        val height = state.layoutCoordinator.layoutHeight()
        val padding = state.layoutCoordinator.contentPadding()
        Row(
            modifier = remember(width, height) {
                Modifier
                    .width(width)
                    .height(height)
            }.runRemember(padding) {
                padding(padding)
            }
        ) {
            var descriptionLayoutState by remember {
                mutableStateOf<DescriptionLayoutState?>(null)
            }
            var buttonsLayoutState by remember {
                mutableStateOf<ButtonsLayoutState?>(null)
            }
            val descScope = remember {
                object : DescriptionLayoutScope {
                    override fun placeLayout(
                        artwork: @Composable (data: Any?) -> Unit,
                        text: @Composable (title: String, subtitle: String) -> Unit
                    ) {
                        descriptionLayoutState = DescriptionLayoutState(artwork, text)
                    }
                }
            }
            val buttonScope = remember {
                object : ButtonsLayoutScope {
                    override fun placeLayout(
                        previousButton: @Composable (
                            clickable: Boolean,
                            onClick: () -> Unit,
                            iconSize: Dp
                        ) -> Unit,
                        nextButton: @Composable (
                            clickable: Boolean,
                            onClick: () -> Unit,
                            iconSize: Dp
                        ) -> Unit
                    ) {
                        buttonsLayoutState = ButtonsLayoutState(previousButton, nextButton)
                    }
                }
            }
            descScope.description()
            buttonScope.buttons()
            
        }
    }

    companion object {
        fun of(state: CompactControlState): CompactControlCoordinator {
            @Suppress("SENSELESS_COMPARISON")
            check(state.coordinator == null)
            return CompactControlCoordinator(state)
        }
    }
}

class CompactControlLayoutCoordinator private constructor(
    private val state: CompactControlState
) {

    fun layoutWidth(): Dp {
       TODO()
    }

    fun layoutHeight(): Dp {
        TODO()
    }

    fun contentPadding(): Dp {
        TODO()
    }


    companion object {
        fun of(state: CompactControlState): CompactControlLayoutCoordinator {
            @Suppress("SENSELESS_COMPARISON")
            check(state.layoutCoordinator == null)
            return CompactControlLayoutCoordinator(state)
        }
    }
}
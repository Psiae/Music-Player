package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.player.presentation.main.compose.TransitioningCompactPlaybackControl

/**
 * State class of our CompactPlaybackControl of our `Root`
 *
 * @param bottomOffsetState the bottom offset relative to the parent layout viewport on which [Layout]
 * will be called, e.g. Bottom Navigation height of a scaffold
 *
 * @param freezeState flag to `freeze` or `pause` the layout, useful to reduce load
 * @param onArtworkClicked the callback when the Artwork layout is clicked
 * @param onBackgroundClicked the callback when the Background layout is clicked
 */
class RootCompactPlaybackControlState internal constructor(
    // I do think that passing down the User would be more ideal than observing via ViewModel
    /* TODO: private val userState: State<User> */
    private val bottomOffsetState: State<Dp>,
    private val freezeState: State<Boolean>,
    private val onArtworkClicked: () -> Unit = {},
    private val onBackgroundClicked: () -> Unit = {},
) {

    var layoutVisibleHeight: Dp by mutableStateOf(0.dp)
        @SnapshotRead get
        @SnapshotWrite private set

    @Composable
    fun BoxScope.Layout() {
        TransitioningCompactPlaybackControl(
            bottomVisibilityVerticalPadding = bottomOffsetState.value,
            freezeState = freezeState,
            onArtworkClicked = onArtworkClicked,
            onBaseClicked = onBackgroundClicked,
            onLayoutVisibleHeightChanged = { layoutVisibleHeight = it }
        )
    }
}

@Composable
fun rememberRootCompactPlaybackControlState(
    bottomOffsetState: State<Dp>,
    freezeState: State<Boolean>,
    onArtworkClicked: () -> Unit,
    onBackgroundClicked: () -> Unit
): RootCompactPlaybackControlState {
    return remember(bottomOffsetState, freezeState) {
        RootCompactPlaybackControlState(
            bottomOffsetState,
            freezeState,
            onArtworkClicked,
            onBackgroundClicked
        )
    }
}


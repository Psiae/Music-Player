package dev.dexsr.klio.player.presentation.root

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.base.compose.SnapshotWrite

abstract class RootCompactPlaybackControlPanelState() {

    abstract val onSurfaceClicked: (() -> Unit)?
        @SnapshotRead get

    abstract val onArtClicked: (() -> Unit)?
        @SnapshotRead get

    abstract var heightFromAnchor: Dp
        @SnapshotRead get
        @SnapshotWrite set
}

@Composable
fun rememberRootCompactPlaybackControlPanelState(
    onSurfaceClicked: (() -> Unit)?,
    onArtClicked: (() -> Unit)?
): RootCompactPlaybackControlPanelState {
    return remember {
        RootCompactPlaybackControlPanelStateImpl()
    }.apply {
        onSurfaceClickedState.value = onSurfaceClicked
        onArtClickedState.value = onArtClicked
    }
}


private class RootCompactPlaybackControlPanelStateImpl(

) : RootCompactPlaybackControlPanelState() {

    val onSurfaceClickedState = mutableStateOf<(() -> Unit)?>(null)
    val onArtClickedState = mutableStateOf<(() -> Unit)?>(null)

    override val onSurfaceClicked: (() -> Unit)?
        @SnapshotRead get() = onSurfaceClickedState.value

    override val onArtClicked: (() -> Unit)?
        @SnapshotRead get() = onArtClickedState.value

    override var heightFromAnchor: Dp by mutableStateOf(0.dp)
        @SnapshotRead get
}
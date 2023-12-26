package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.runtime.*

// mark compose stable for skippable

@Stable
class PlaybackControlMainScreenState(
    val playbackController: PlaybackController,
    val mediaMetadataProvider: MediaMetadataProvider
) {

    var userDismissible by mutableStateOf(false)
    var onUserDismiss by mutableStateOf<(() -> Unit)?>(null)

    var canOpenMore by mutableStateOf(false)
    var onOpenMore by mutableStateOf<(() -> Unit)?>(null)

    var freeze by mutableStateOf(false)

    fun onDismissButtonClicked() {
        onUserDismiss?.invoke()
    }

    fun onOpenMoreButtonClicked() {
        onOpenMore?.invoke()
    }

}

@Composable
fun rememberPlaybackControlMainScreenState(
    container: PlaybackControlScreenState,
    transitionState: PlaybackControlScreenTransitionState
): PlaybackControlMainScreenState {

    return remember(container) {
        PlaybackControlMainScreenState(
            container.playbackController,
            container.mediaMetadataProvider
        )
    }.apply {
        userDismissible = true
        onUserDismiss = container::hide
    }
}
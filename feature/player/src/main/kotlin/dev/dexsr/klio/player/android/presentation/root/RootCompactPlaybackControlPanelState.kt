package dev.dexsr.klio.player.android.presentation.root

import androidx.annotation.MainThread
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.base.compose.SnapshotWrite
import dev.dexsr.klio.base.compose.rememberWithCompositionObserver
import dev.dexsr.klio.player.android.presentation.root.bw.OldRootCompactMediaMetadataProvider
import dev.dexsr.klio.player.android.presentation.root.bw.OldRootCompactPlaybackController

class RootCompactPlaybackControlPanelState internal constructor(
    val playbackController: RootCompactPlaybackController,
    val mediaMetadataProvider: RootCompactMediaMetadataProvider
) {

    private val onSurfaceClickedState = mutableStateOf<(() -> Unit)?>(null)
    private val onArtClickedState = mutableStateOf<(() -> Unit)?>(null)

    val onSurfaceClicked: (() -> Unit)?
        @SnapshotRead get() = onSurfaceClickedState.value

    val onArtClicked: (() -> Unit)?
        @SnapshotRead get() = onArtClickedState.value

    var heightFromAnchor: Dp by mutableStateOf(0.dp)
        @SnapshotRead get
        @SnapshotWrite set

    var freeze: Boolean by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite set

    var isSurfaceDark: Boolean by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite set

    @MainThread
    fun params(
        onArtClicked: (() -> Unit)?,
        onSurfaceClicked: (() -> Unit)?
    ) {
        checkInMainLooper()
        onArtClickedState.value = onArtClicked
        onSurfaceClickedState.value = onSurfaceClicked
    }
}

@Composable
fun rememberRootCompactPlaybackControlPanelState(
    onSurfaceClicked: (() -> Unit)?,
    onArtClicked: (() -> Unit)?
): RootCompactPlaybackControlPanelState {
    return oldRememberRootCompactPlaybackControlPanelState(onSurfaceClicked, onArtClicked)
}

@Composable
private fun oldRememberRootCompactPlaybackControlPanelState(
    onArtClicked: (() -> Unit)?,
    onSurfaceClicked: (() -> Unit)?
): RootCompactPlaybackControlPanelState {
    val userState = remember {
        mutableStateOf<User?>(null)
    }
    val vm = viewModel<PlaybackControlViewModel>()
    LaunchedEffect(key1 = Unit, block = {
        AuthService.get().observeCurrentUser().collect { userState.value = it }
    })
    val state = userState.value
        ?.let { user ->
            val pc = rememberWithCompositionObserver(
                user, vm,
                onRemembered = {},
                onAbandoned = { it.dispose() },
                onForgotten = { it.dispose() }
            ) {
                OldRootCompactPlaybackController(user, vm)
            }
            val mmp = rememberWithCompositionObserver(
                user, vm,
                onRemembered = {},
                onForgotten = { it.dispose() },
                onAbandoned = { it.dispose() }
            ) {
                OldRootCompactMediaMetadataProvider(user, vm)
            }
            remember(pc, mmp) { RootCompactPlaybackControlPanelState(pc, mmp) }
        }
        ?: remember {
            RootCompactPlaybackControlPanelState(
                NoOpRootCompactPlaybackController,
                NoOpRootCompactMediaMetadataProvider
            )
        }
    SideEffect {
        state.params(onArtClicked, onSurfaceClicked)
    }
    return state
}
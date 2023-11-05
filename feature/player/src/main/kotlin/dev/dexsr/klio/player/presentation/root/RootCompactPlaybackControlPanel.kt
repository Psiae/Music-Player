package dev.dexsr.klio.player.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlCompact
import com.flammky.musicplayer.player.presentation.root.rememberRootPlaybackControlCompactState

@Composable
fun RootCompactPlaybackControlPanel(
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
) {
    // TODO: Impl
    if (true) {
        Box(modifier = Modifier.height(56.dp)) {
            OldRootPlaybackControlPanel(state = state, bottomSpacing)
        }
    }
}

@Composable
@Deprecated("")
private fun OldRootPlaybackControlPanel(
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
) {
    val userState = remember {
        mutableStateOf<User?>(null)
    }
    LaunchedEffect(key1 = Unit, block = {
        AuthService.get().observeCurrentUser().collect { userState.value = it }
    })
    userState.value?.let {  user ->
        RootPlaybackControlCompact(
            state = rememberRootPlaybackControlCompactState(
                user = user,
                onBaseClicked = state.onSurfaceClicked,
                onArtworkClicked = state.onArtClicked
            ).apply {
                this.bottomSpacing = bottomSpacing
            }.run {
                state.apply { this.heightFromAnchor = topPositionRelativeToAnchor }
                this
            }
        )
    }
}
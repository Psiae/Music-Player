package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import kotlinx.coroutines.SupervisorJob


internal class RootPlaybackControlCompactState(
    private val playbackController: PlaybackController
) {
    var bottomVisibilityOffset by mutableStateOf<Dp>(0.dp)
        @SnapshotRead get
        @SnapshotWrite set

    var freeze by mutableStateOf<Dp>(0.dp)
        @SnapshotRead
        @SnapshotWrite set
}

internal class RootPlaybackControlCompactApplier(
    private val state: RootPlaybackControlCompactState
) {

    fun prepareState() {

    }

    @Composable
    fun PrepareCompose() {
        val coroutineScope = rememberCoroutineScope()
        DisposableEffect(
            key1 = this,
            effect = {
                val supervisor = SupervisorJob()
                onDispose { supervisor.cancel() }
            }
        )
    }
}

class RootPlaybackControlCompactPager(

)
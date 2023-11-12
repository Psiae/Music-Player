package dev.dexsr.klio.player.android.presentation.root

import androidx.compose.runtime.*
import com.flammky.musicplayer.player.presentation.root.main.ComposeBackPressRegistry
import dev.dexsr.klio.base.compose.SnapshotRead

abstract class RootPlaybackControlScreenState {

    abstract val backPressRegistry: ComposeBackPressRegistry

    abstract var showSelf: Boolean
        @SnapshotRead get
}

@Composable
fun rememberRootPlaybackControlScreenState(
): RootPlaybackControlScreenState {
    return remember {
        RootPlaybackControlScreenStateImpl()
    }.apply {

    }
}


private class RootPlaybackControlScreenStateImpl(

) : RootPlaybackControlScreenState() {

    override val backPressRegistry = ComposeBackPressRegistry()

    override var showSelf: Boolean by mutableStateOf(false)
        @SnapshotRead get
}

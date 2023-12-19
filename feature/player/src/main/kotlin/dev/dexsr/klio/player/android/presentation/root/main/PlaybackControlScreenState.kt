package dev.dexsr.klio.player.android.presentation.root.main

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.root.main.ComposeBackPressRegistry
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.base.compose.SnapshotWrite
import dev.dexsr.klio.base.compose.rememberWithCompositionObserver
import dev.dexsr.klio.player.android.presentation.root.bw.OldMediaMetadataProvider
import dev.dexsr.klio.player.android.presentation.root.bw.OldPlaybackController

// mark as stable for skippable
@Stable
class PlaybackControlScreenState(
    val playbackController: PlaybackController,
    val mediaMetadataProvider: MediaMetadataProvider,
    val backPressRegistry: ComposeBackPressRegistry
) {

    private val backPressConsumer = ComposeBackPressRegistry.BackPressConsumer {
        if (showSelf) {
            hide()
            return@BackPressConsumer true
        }
        false
    }

    var freeze: Boolean by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite set


    var showSelf: Boolean by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite private set

    fun show() {
        checkInMainLooper()
        if (showSelf) return
        showSelf = true
        backPressRegistry.registerBackPressConsumer(backPressConsumer)
    }

    fun hide() {
        checkInMainLooper()
        if (!showSelf) return
        showSelf = false
        backPressRegistry.unregisterBackPressConsumer(backPressConsumer)
    }

    fun saveState(): Bundle {
        return Bundle().apply {
            putBoolean("showSelf", this@PlaybackControlScreenState.showSelf)
        }
    }

    fun restoreState(bundle: Bundle) {
        if (bundle.containsKey("showSelf")) {
            showSelf = bundle.getBoolean("showSelf")
        }
    }

    companion object {

        fun Saver(
            playbackController: PlaybackController,
            mediaMetadataProvider: MediaMetadataProvider,
            registry: ComposeBackPressRegistry
        ): Saver<PlaybackControlScreenState, android.os.Bundle> {
            return androidx.compose.runtime.saveable.Saver(
                save = { it.saveState() },
                restore = {
                    PlaybackControlScreenState(
                        playbackController,
                        mediaMetadataProvider,
                        registry
                    ).apply { restoreState(it) }
                }
            )
        }
    }
}


@Composable
fun rememberPlaybackControlScreenState(

): PlaybackControlScreenState {
    return oldRememberPlaybackControlScreenState()
}

@Composable
private fun oldRememberPlaybackControlScreenState(
): PlaybackControlScreenState {
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
                OldPlaybackController(user, vm)
            }
            val mmp = rememberWithCompositionObserver(
                user, vm,
                onRemembered = {},
                onForgotten = { it.dispose() },
                onAbandoned = { it.dispose() }
            ) {
                OldMediaMetadataProvider(user, vm)
            }
            val backPressRegistry = remember(
                pc,
                mmp,
            ) {
                ComposeBackPressRegistry()
            }
            rememberSaveable(
                pc,
                mmp,
                saver = remember(pc, mmp, backPressRegistry) {
                    PlaybackControlScreenState.Saver(pc, mmp, backPressRegistry)
                }
            ) { PlaybackControlScreenState(
                playbackController = pc,
                mediaMetadataProvider = mmp,
                backPressRegistry = backPressRegistry
            ) }
        }
        ?: remember {
            PlaybackControlScreenState(
                playbackController = NoOpPlaybackController,
                mediaMetadataProvider = NoOpMediaMetadataProvider,
                backPressRegistry = ComposeBackPressRegistry()
            )
        }
    return state
}
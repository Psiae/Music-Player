package com.flammky.musicplayer.player.presentation.root

import android.os.Bundle
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.flammky.musicplayer.player.presentation.main.compose.TransitioningPlaybackControl

class RootPlaybackControlState internal constructor() {

    private var restorationBundle: Bundle? = null

    private var pendingRestore = false
    private var restored = false
    private var wasTransitionedState by mutableStateOf(false)
    internal var freezeState = mutableStateOf(false)
    internal var showSelfState = mutableStateOf(false)

    @Composable
    fun BoxScope.Layout(
        onDismissRequest: () -> Unit
    ) {
        check(restored)
        TransitioningPlaybackControl(
            showSelfState = showSelfState,
            dismiss = onDismissRequest
        )
    }

    internal fun performRestore(bundle: Bundle) {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = bundle
        pendingRestore = true
    }

    internal fun performPendingRestore() {
        check(pendingRestore)
        restorationBundle?.let { bundle ->
            wasTransitionedState =
                bundle.getBoolean(::wasTransitionedState.name) && showSelfState.value
        }
        restored = true
    }

    internal fun skipRestore() {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = null
        pendingRestore = true
    }

    companion object {
        fun saver(): Saver<RootPlaybackControlState, Bundle> = Saver(
            save = { state ->
                Bundle()
                    .apply {
                        putBoolean(state::wasTransitionedState.name, state.wasTransitionedState)
                    }
            },
            restore = { bundle ->
                RootPlaybackControlState()
                    .apply {
                        performRestore(bundle)
                    }
            }
        )
    }
}

@Composable
fun rememberRootPlaybackControlState(
    freezeState: State<Boolean>,
    showSelfState: State<Boolean>,
): RootPlaybackControlState {
    return rememberSaveable(saver = RootPlaybackControlState.saver()) {
        RootPlaybackControlState()
            .apply {
                this.freezeState.value = freezeState.value
                this.showSelfState.value = showSelfState.value
                skipRestore()
            }
    }.apply {
        this.freezeState.value = freezeState.value
        this.showSelfState.value = showSelfState.value
        performPendingRestore()
    }
}
package com.flammky.musicplayer.player.presentation.root

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.main.PlaybackControlTrackMetadata
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.main.compose.TransitioningPlaybackControl
import kotlinx.coroutines.flow.Flow

class RootPlaybackControlState internal constructor(
    internal val user: User,
    internal val viewModel: PlaybackControlViewModel
) {

    internal var currentComposition: RootPlaybackControlMainScope? = null

    private var restorationBundle: Bundle? = null
    private var pendingRestore = false
    private var restored = false

    // TODO: move these
    internal val rememberMainFullyTransitionedState = mutableStateOf(false)
    internal val rememberQueueFullyTransitionedState = mutableStateOf(false)
    internal val freezeState = mutableStateOf(false)
    internal val showMainState = mutableStateOf(false)

    internal var dismissHandle: (() -> Boolean)? = null

    /**
     * state on whether the Layout is `visually` visible, false means that it's fully hidden
     */
    val mainScreenVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition
            ?.let { currentScope ->
                currentScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { _ -> currentScope.visibleHeight > 0 }
            } == true
    } @SnapshotRead get

    /**
     * state on whether the Layout is `visually` fully visible
     */
    val mainScreenFullyVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition
            ?.let { currentScope ->
                currentScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { target -> currentScope.visibleHeight >= target }
            } == true
    } @SnapshotRead get

    /**
     * state on whether the Layout is `visually` visible, false means that it's fully hidden
     */
    val queueScreenVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition?.queueComposition
            ?.let { queueScope ->
                queueScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { _ -> queueScope.visibleHeight > 0 }
            } == true
    } @SnapshotRead get

    /**
     * state on whether the Layout is `visually` fully visible
     */
    val queueScreenFullyVisible by derivedStateOf(policy = structuralEqualityPolicy()) {
        currentComposition?.queueComposition
            ?.let { queueScope ->
                queueScope.fullyVisibleHeightTarget
                    // TODO: assert that this should not happen
                    .takeIf { it >= 0 }
                    ?.let { target -> queueScope.visibleHeight >= target }
            } == true
    } @SnapshotRead get

    @Composable
    fun Compose(
        modifier: Modifier = Modifier
    ) {
        check(restored)
        /* TODO: RootPlaybackControl(state = this) */
        TransitioningPlaybackControl(
            showSelfState = showMainState,
            dismiss = { dismiss() }
        )
    }

    fun showSelf() {
        showMainState.value = true
    }

    fun dismiss() {
        if (dismissHandle?.invoke() != false) {
            showMainState.value = false
        }
    }

    fun backPress() {
        // TODO
        dismiss()
    }

    internal fun performRestore(bundle: Bundle) {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = bundle
        pendingRestore = true
    }

    internal fun performPendingRestore(
        initialFreezeState: Boolean,
        initialShowSelfState: Boolean
    ) {
        if (restored) {
            check(!pendingRestore)
            return
        }
        check(pendingRestore)
        showMainState.value = initialShowSelfState
        freezeState.value = initialFreezeState
        restorationBundle
            ?.let {
                rememberMainFullyTransitionedState.value =
                    it.getBoolean(::rememberMainFullyTransitionedState.name) && showMainState.value
            }
            ?: run {
                rememberMainFullyTransitionedState.value =
                    showMainState.value
            }
        restored = true
        pendingRestore = false
    }

    internal fun skipRestore() {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = null
        pendingRestore = true
    }

    companion object {
        internal fun saver(
            user: User,
            viewModel: PlaybackControlViewModel
        ): Saver<RootPlaybackControlState, Bundle> = Saver(
            save = { state ->
                Bundle()
                    .apply {
                        putBoolean(
                            state::rememberMainFullyTransitionedState.name,
                            state.mainScreenFullyVisible
                        )
                    }
            },
            restore = { bundle ->
                RootPlaybackControlState(user, viewModel)
                    .apply {
                        performRestore(bundle)
                    }
            }
        )
    }
}

@Composable
fun rememberRootPlaybackControlState(
    user: User,
    initialFreezeState: Boolean,
    initialShowSelfState: Boolean,
    dismissHandle: (() -> Boolean)? = null
): RootPlaybackControlState {
    val viewModel = hiltViewModel<PlaybackControlViewModel>()
    return rememberSaveable(
        saver = RootPlaybackControlState.saver(user, viewModel)
    ) {
        RootPlaybackControlState(user, viewModel)
            .apply {
                this.freezeState.value = initialFreezeState
                this.showMainState.value = initialShowSelfState
                this.dismissHandle = dismissHandle
                skipRestore()
            }
    }.apply {
        this.dismissHandle = dismissHandle
        performPendingRestore(
            initialFreezeState,
            initialShowSelfState
        )
    }
}

internal class RootPlaybackControlMainScope(
    val state: RootPlaybackControlState,
    val playbackController: PlaybackController,
    val observeMetadata: (String) -> Flow<PlaybackControlTrackMetadata>,
    val dismiss: () -> Unit
) {

    internal var currentQueue by mutableStateOf(OldPlaybackQueue.UNSET)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentQueueReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPlaybackMetadata by mutableStateOf<PlaybackControlTrackMetadata?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentMetadataReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPlaybackBitmap by mutableStateOf<Bitmap?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentBitmapReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentBitmapReaderBiggestViewport by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPlaybackPalette by mutableStateOf<Palette?>(null)
        @SnapshotRead get
        @SnapshotWrite set

    internal var currentPaletteReaderCount by mutableStateOf<Int>(0)
        @SnapshotRead get
        @SnapshotWrite set

    // should this be nullable tho ?
    var queueComposition by mutableStateOf<RootPlaybackControlQueueScope?>(null)
        @SnapshotRead get
        @SnapshotWrite internal set

    var fullyVisibleHeightTarget by mutableStateOf(-1)
        @SnapshotRead get
        @SnapshotWrite internal set

    var visibleHeight by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite internal set

    internal fun showPlaybackQueue() {
        // TODO
    }

    companion object {
        // Saver ?
        fun saver(
            state: RootPlaybackControlState,
            controller: PlaybackController,
            viewModel: PlaybackControlViewModel,
            dismiss: () -> Unit
        ): Saver<RootPlaybackControlMainScope, Bundle> {
            return Saver(
                save = {
                    Bundle()
                        .apply {
                            // TODO
                        }
                },
                restore = {
                    RootPlaybackControlMainScope(
                        state,
                        controller,
                        viewModel::observeMetadata,
                        dismiss
                    ).apply {
                        // TODO
                    }
                }
            )
        }
    }
}

internal class RootPlaybackControlQueueScope(
    val mainComposition: RootPlaybackControlMainScope
) {

    var fullyVisibleHeightTarget by mutableStateOf(-1)
        @SnapshotRead get
        @SnapshotWrite internal set

    var visibleHeight by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite internal set

    companion object {
        // Saver ?
        fun saver(
            main: RootPlaybackControlMainScope
        ): Saver<RootPlaybackControlQueueScope, Bundle> {
            return Saver(
                save = {
                    Bundle()
                        .apply {
                            // TODO
                        }
                },
                restore = {
                    RootPlaybackControlQueueScope(main)
                        .apply {
                            // TODO
                        }
                }
            )
        }
    }
}
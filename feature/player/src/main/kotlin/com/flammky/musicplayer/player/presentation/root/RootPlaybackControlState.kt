package com.flammky.musicplayer.player.presentation.root

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.main.PlaybackControlTrackMetadata
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.main.compose.TransitioningPlaybackControl
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KMutableProperty

class RootPlaybackControlState internal constructor(
    internal val user: User,
    internal val viewModel: PlaybackControlViewModel
) {

    internal var currentComposition: RootPlaybackControlMainScope? = null

    private var restorationBundle: Bundle? = null
    private var compositionRestorationBundle: Bundle? = null
    private var pendingRestore = false
    private var restored = false

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

    val consumeBackPress by derivedStateOf(policy = structuralEqualityPolicy()) {
        showMainState.value || currentComposition?.consumeBackPress == true
    }

    @Composable
    fun Compose(
        modifier: Modifier = Modifier
    ) {
        check(restored)
        // unstable
        /*RootPlaybackControl(modifier = modifier, state = this)*/
        TransitioningPlaybackControl(
            showSelfState = showMainState,
            dismiss = ::dismiss
        )
    }

    internal fun restoreComposition(
        composition: RootPlaybackControlMainScope
    ) {
        compositionRestorationBundle?.let {
            compositionRestorationBundle = null
            composition.performRestore(it)
        }
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
        // TODO: Define back press consumer
        if (currentComposition?.consumeBackPress() == true) {
            return
        }
        dismiss()
    }

    internal fun performRestore(bundle: Bundle) {
        check(!pendingRestore)
        check(!restored)
        restorationBundle = bundle
        compositionRestorationBundle = bundle.getBundle(RootPlaybackControlMainScope::class.simpleName)
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
                        state.currentComposition?.let {
                            putBundle(it::class.simpleName, it.performSave())
                        }
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
    ) init@ {
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
    val observeTrackSimpleMetadata: (String) -> Flow<PlaybackControlTrackMetadata>,
    val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
    val dismiss: () -> Unit
) {

    private var queueCompositionRestorationBundle: Bundle? = null

    internal var rememberFullyTransitioned by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite set

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

    internal var showPlaybackQueue by mutableStateOf(false)
        @SnapshotRead get
        @SnapshotWrite private set

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

    val consumeBackPress by derivedStateOf {
        showPlaybackQueue
    }

    internal fun performSave(): Bundle {
        return Bundle()
            .apply {
                val showPlaybackQueueRef: KMutableProperty<Boolean> = ::showPlaybackQueue
                putBoolean(::rememberFullyTransitioned.name, rememberFullyTransitioned)
                putBoolean(showPlaybackQueueRef.name, showPlaybackQueue)
                queueComposition?.let {
                    putBundle(it::class.simpleName, it.performSave())
                }
            }
    }

    internal fun performRestore(bundle: Bundle) {
        bundle
            .run {
                val showPlaybackQueueRef: KMutableProperty<Boolean> = ::showPlaybackQueue
                rememberFullyTransitioned = getBoolean(::rememberFullyTransitioned.name)
                showPlaybackQueue = getBoolean(showPlaybackQueueRef.name, showPlaybackQueue)
                queueCompositionRestorationBundle =
                    getBundle(RootPlaybackControlQueueScope::class.simpleName)
            }
    }

    internal fun showPlaybackQueue() {
        showPlaybackQueue = true
    }

    internal fun dismissPlaybackQueue() {
        showPlaybackQueue = false
    }

    internal fun consumeBackPress(): Boolean {
        if (showPlaybackQueue) {
            showPlaybackQueue = false
            return true
        }
        return false
    }

    internal fun restoreQueueComposition(
        composition: RootPlaybackControlQueueScope
    ) {
        queueCompositionRestorationBundle?.let {
            queueCompositionRestorationBundle = null
            composition.performRestore(it)
        }
    }

    companion object {
        // Saver ?
        fun saver(
            state: RootPlaybackControlState,
            controller: PlaybackController,
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
                        state.viewModel::observeSimpleMetadata,
                        state.viewModel::observeMediaMetadata,
                        state.viewModel::observeMediaArtwork,
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
    private val mainComposition: RootPlaybackControlMainScope
) {
    val playbackController
        get() = mainComposition.playbackController

    val currentQueue
        get() = mainComposition.currentQueue

    var fullyVisibleHeightTarget by mutableStateOf(-1)
        @SnapshotRead get
        @SnapshotWrite internal set

    var visibleHeight by mutableStateOf(0)
        @SnapshotRead get
        @SnapshotWrite internal set

    var playbackControlContentPadding by mutableStateOf(PaddingValues(0.dp))
        @SnapshotRead get
        @SnapshotWrite internal set

    var statusBarInset by mutableStateOf(0.dp)
        @SnapshotRead get
        @SnapshotWrite internal set

    val columnVisibilityContentPadding by derivedStateOf {
        val pc = playbackControlContentPadding
        PaddingValues(
            top = maxOf(pc.calculateTopPadding(), statusBarInset),
            bottom = maxOf(pc.calculateBottomPadding())
        )
    }

    val showSelf
        get() = mainComposition.showPlaybackQueue

    internal var rememberFullyTransitionedState by mutableStateOf(false)

    fun performSave(): Bundle {
        return Bundle()
            .apply {
                putBoolean(::rememberFullyTransitionedState.name, rememberFullyTransitionedState)
            }
    }

    fun performRestore(bundle: Bundle) {
        rememberFullyTransitionedState = bundle.getBoolean(::rememberFullyTransitionedState.name)
    }

    fun incrementQueueReaderCount() {
        mainComposition.currentQueueReaderCount++
    }

    fun decrementQueueReaderCount() {
        mainComposition.currentQueueReaderCount--
    }

    fun observeTrackMetadata(id: String) = mainComposition.observeTrackMetadata(id)
    fun observeTrackArtwork(id: String) = mainComposition.observeArtwork(id)

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
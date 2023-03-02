package com.flammky.musicplayer.player.presentation.root

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotWrite
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.isDarkAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration


internal class RootPlaybackControlCompactState(
    val playbackController: PlaybackController,
    val onBackgroundClicked: () -> Unit,
    val onArtworkClicked: () -> Unit,
    val onObserveMetadata: (String) -> Flow<MediaMetadata?>,
    val onObserveArtwork: (String) -> Flow<Any?>,
    val coroutineScope: CoroutineScope,
    val coroutineDispatchScope: CoroutineScope,
) {

    val coordinator = ControlCompactCoordinator(this, coroutineScope, coroutineDispatchScope)

    var height by mutableStateOf<Dp>(55.dp)
        @SnapshotRead get
        @SnapshotWrite set

    var width by mutableStateOf<Dp>(Dp.Infinity)
        @SnapshotRead get
        @SnapshotWrite set

    /**
     * the bottom offset for the coordinator to apply
     */
    var bottomOffset by mutableStateOf<Dp>(0.dp)
        @SnapshotRead get
        @SnapshotWrite set

    /**
     * mutable freeze state for the coordinator to apply, when set to true the layout will no longer
     * collect remote updates nor dispatch user request (dropped)
     */
    var freeze by mutableStateOf<Boolean>(false)
        @SnapshotRead get
        @SnapshotWrite set
}

internal class ControlCompactCoordinator(
    private val state: RootPlaybackControlCompactState,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatchScope: CoroutineScope,
) {

    val coordinatorSupervisorJob = SupervisorJob()

    val layoutComposition = ControlCompactComposition(
        getLayoutHeight = @SnapshotRead state::height::get,
        getLayoutWidth = @SnapshotRead state::width::get,
        observeMetadata = state.onObserveMetadata,
        observeArtwork = state.onObserveArtwork,
        observePlaybackQueue = ::observeControllerPlaybackQueue,
        observePlaybackProperties = ::observeControllerPlaybackProperties,
        setPlayWhenReady = ::setPlayWhenReady,
        observeProgressWithIntervalHandle = ::observeProgressWithIntervalHandle,
        coroutineScope = coroutineScope
    )

    private var queueReaderCount by mutableStateOf(0)
    private var propertiesReaderCount by mutableStateOf(0)

    var remoteQueueSnapshot by mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)
        private set

    var remotePlaybackPropertiesSnapshot by mutableStateOf(PlaybackProperties.UNSET)
        private set

    val freeze by derivedStateOf { state.freeze }

    private fun observeControllerPlaybackProperties(): Flow<PlaybackProperties> {
        return flow {
            propertiesReaderCount++
            try {
                snapshotFlow { remotePlaybackPropertiesSnapshot }
                    .collect(this)
            }  finally {
                propertiesReaderCount--
            }
        }
    }

    private fun observeControllerPlaybackQueue(): Flow<OldPlaybackQueue> {
        return flow {
            queueReaderCount++
            try {
                snapshotFlow { remoteQueueSnapshot }
                    .collect(this)
            } finally {
                queueReaderCount--
            }
        }
    }

    private fun setPlayWhenReady(
        play: Boolean,
        joinCollectorDispatch: Boolean
    ): Deferred<Result<Boolean>> {
        return coroutineDispatchScope.async {
            runCatching {
                val result = state.playbackController.requestSetPlayWhenReadyAsync(play).await()
                if (joinCollectorDispatch) {
                    result.eventDispatch?.join()
                }
                result.success
            }
        }
    }

    private fun observeProgressWithIntervalHandle (
        getNextInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ): Flow<Duration> {
        return flow {
            val observer = state.playbackController.createPlaybackObserver()
            val collector = observer.createProgressionCollector()
            try {
                collector
                    .apply {
                        setIntervalHandler { _, p, d, s ->
                            getNextInterval(p, d, s)
                        }
                        startCollectPosition().join()
                    }
                collector.positionStateFlow.collect(this@flow)
            } finally {
                observer.dispose()
            }
        }
    }

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @Composable
        inline fun ControlCompactCoordinator.PrepareCompositionInline() {
            // I don't think these should be composable, but that's for later
            ComposeRemoteQueueReaderObserver()
            DisposableEffect(
                key1 = this,
                effect = {
                    // TODO: assert that this state can only be used within a single composition
                    //  tree at a time (acquire slot)
                    onDispose {
                        // TODO: release slot
                    }
                }
            )
        }

        @Composable
        private inline fun ControlCompactCoordinator.ComposeRemoteQueueReaderObserver() {
            LaunchedEffect(
                key1 = this,
                block = {
                    launch(coordinatorSupervisorJob) {
                        var latestCollectorJob: Job? = null
                        snapshotFlow { queueReaderCount }
                            .map {
                                check(it >= 0)
                                it > 0
                            }
                            .distinctUntilChanged()
                            .collect { hasActiveReader ->
                                if (!hasActiveReader) {
                                    remoteQueueSnapshot = OldPlaybackQueue.UNSET
                                    latestCollectorJob?.cancel()
                                    return@collect
                                }
                                check(latestCollectorJob?.isActive != true)
                                latestCollectorJob = launch {
                                    var collectWithFreezeHandle: Job? = null
                                    snapshotFlow { state.freeze }
                                        .collect { freeze ->
                                            if (freeze) {
                                                collectWithFreezeHandle?.cancel()
                                                return@collect
                                            }
                                            check(collectWithFreezeHandle?.isActive != true)
                                            collectWithFreezeHandle = launch {
                                                val observer = state.playbackController.createPlaybackObserver()
                                                try {
                                                    val collector = observer.createQueueCollector()
                                                        .apply { startCollect().join() }
                                                    collector.queueStateFlow
                                                        .collect {
                                                            remoteQueueSnapshot = it
                                                        }
                                                } finally {
                                                    observer.dispose()
                                                }
                                            }
                                        }
                                }
                            }
                    }.join()
                }
            )
        }

        @Composable
        private inline fun ControlCompactCoordinator.ComposeRemotePropertiesReaderObserver() {
            LaunchedEffect(
                key1 = this,
                block = {
                    launch(coordinatorSupervisorJob) {
                        var latestCollectorJob: Job? = null
                        snapshotFlow { propertiesReaderCount }
                            .map {
                                check(it >= 0)
                                it > 0
                            }
                            .distinctUntilChanged()
                            .collect { hasActiveReader ->
                                if (!hasActiveReader) {
                                    remotePlaybackPropertiesSnapshot = PlaybackProperties.UNSET
                                    latestCollectorJob?.cancel()
                                    return@collect
                                }
                                check(latestCollectorJob?.isActive != true)
                                latestCollectorJob = launch {
                                    var collectWithFreezeHandle: Job? = null
                                    snapshotFlow { state.freeze }
                                        .collect { freeze ->
                                            if (freeze) {
                                                collectWithFreezeHandle?.cancel()
                                                return@collect
                                            }
                                            check(collectWithFreezeHandle?.isActive != true)
                                            collectWithFreezeHandle = launch {
                                                val observer = state.playbackController.createPlaybackObserver()
                                                try {
                                                    val collector = observer.createPropertiesCollector()
                                                        .apply { startCollect().join() }
                                                    collector.propertiesStateFlow
                                                        .collect {
                                                            remotePlaybackPropertiesSnapshot = it
                                                        }
                                                } finally {
                                                    observer.dispose()
                                                }
                                            }
                                        }
                                }
                            }
                    }.join()
                }
            )
        }
    }
}

class CompactControlTransitionState(
    private val getLayoutHeight: @SnapshotRead () -> Dp,
    private val getLayoutWidth: @SnapshotRead () -> Dp
) {

    val applier: Applier = Applier(this)

    val layoutHeight: Dp
        @SnapshotRead get() = getLayoutHeight()

    val layoutWidth: Dp
        @SnapshotRead get() = getLayoutWidth()

    val animatedLayoutOffset: DpOffset by mutableStateOf(DpOffset.Zero)

    class Applier(
        private val state: CompactControlTransitionState
    ) {
        companion object {
            @Suppress("NOTHING_TO_INLINE")
            @Composable
            inline fun Applier.PrepareCompositionInline() {

            }
        }
    }
}
@OptIn(ExperimentalPagerApi::class)
class CompactControlPagerState(
    val layoutState: PagerState,
    val observeMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val isSurfaceDark: @SnapshotRead () -> Boolean
) {

    val applier = Applier()

    var currentLayoutComposition by mutableStateOf<LayoutComposition?>(null)

    class Applier {
        companion object {
            @Suppress("NOTHING_TO_INLINE")
            @Composable
            inline fun Applier.PrepareCompositionInline() {

            }
        }
    }

    class LayoutComposition(
        val queueData: OldPlaybackQueue
    ) {

        var userScrollReady by mutableStateOf(false)

        companion object {

            @Composable
            inline fun LayoutComposition.OnComposingLayout() {

            }

            @Composable
            inline fun LayoutComposition.OnLayoutComposed() {
                val coroutineScope = rememberCoroutineScope()
                remember(this) {
                    coroutineScope.launch {  }
                }
            }
        }
    }
}

class CompactButtonControlsState(
    observePlaybackProperties: () -> Flow<PlaybackProperties>,
    setPlayWhenReady: (play: Boolean, joinCollectorDispatch: Boolean) -> Deferred<Result<Boolean>>
) {

    val applier = Applier()

    class Applier {
        companion object {
            @Suppress("NOTHING_TO_INLINE")
            @Composable
            inline fun Applier.PrepareCompositionInline() {

            }
        }
    }


    class LayoutData()
}

class CompactControlTimeBarState(
    private val observeProgressWithIntervalHandle: (
        getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ) -> Flow<Duration>
) {

    val applier = Applier()

    class Applier {

    }

    class LayoutData()
}

class CompactControlBackgroundState(
    private val observeArtwork: (String) -> Flow<Any?>,
    private val observeQueue: () -> Flow<OldPlaybackQueue>,
    private val coroutineScope: CoroutineScope,
    private val onComposingBackgroundColor: (Color) -> Unit
) {

    val applier = Applier(this)

    private var palette by mutableStateOf<Palette?>(null)

    fun Modifier.backgroundModifier(): Modifier {
        return composed {
            val darkTheme = Theme.isDarkAsState().value
            val color = palette
                .run {
                    val argb =
                        if (this is Palette) {
                            if (darkTheme) {
                                getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
                            } else {
                                getVibrantColor(getLightMutedColor(getDominantColor(-1)))
                            }
                        } else {
                            -1
                        }
                    if (argb != -1) {
                        Color(argb)
                    } else {
                        Theme.surfaceVariantColorAsState().value
                    }
                }
            onComposingBackgroundColor(color)
            background(color)
        }
    }

    companion object {

    }

    class Applier(private val state: CompactControlBackgroundState) {

        private var compositionCount by mutableStateOf(0)

        fun launchQueueObserver() {
            check(compositionCount == 1)
            var latestJob: Job? = null
            state.coroutineScope.launch {
                snapshotFlow { compositionCount }
                    .map { it > 0 }
                    .distinctUntilChanged()
                    .collect { active ->
                        latestJob?.cancel()
                        if (active) {
                            latestJob = launch {
                                var latestBitmapObserver: Job? = null
                                state.observeQueue().collect { queue ->
                                    latestBitmapObserver?.cancel()
                                    val id = queue.list.getOrNull(queue.currentIndex)
                                        ?: run {
                                            state.palette = null
                                            return@collect
                                        }
                                    latestBitmapObserver = launch {
                                        state.observeArtwork(id)
                                            .collect { art ->
                                                if (art !is Bitmap) {
                                                    state.palette = null
                                                    return@collect
                                                }
                                                val palette = withContext(Dispatchers.IO) {
                                                    Palette.from(art).maximumColorCount(16).generate()
                                                }
                                                ensureActive()
                                                state.palette = palette
                                            }
                                    }
                                }
                            }
                        } else {
                            state.palette = null
                        }
                    }
            }
        }

        companion object {

            @Composable
            fun Applier.PrepareCompositionInline() {
                DisposableEffect(
                    key1 = this,
                    effect = {
                        check(compositionCount == 0)
                        compositionCount++
                        launchQueueObserver()
                        onDispose {
                            check(compositionCount == 1)
                            compositionCount--
                        }
                    }
                )
            }
        }
    }

    class LayoutData()
}

class CompactControlArtworkState() {

    val applier = Applier()

    class Applier {

    }

    class LayoutData()
}
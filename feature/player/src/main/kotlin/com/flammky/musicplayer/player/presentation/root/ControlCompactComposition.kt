package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

class ControlCompactComposition internal constructor(
    private val getLayoutHeight: @SnapshotRead () -> Dp,
    private val getLayoutWidth: @SnapshotRead () -> Dp,
    private val getLayoutBottomOffset: @SnapshotRead () -> Dp,
    private val getBaseClickedHandle: @SnapshotRead () -> (() -> Unit)?,
    private val getArtworkClickedHandle: @SnapshotRead () -> (() -> Unit)?,
    private val observeMetadata: (String) -> Flow<MediaMetadata?>,
    private val observeArtwork: (String) -> Flow<Any?>,
    private val observePlaybackQueue: () -> Flow<OldPlaybackQueue>,
    private val observePlaybackProperties: () -> Flow<PlaybackProperties>,
    private val setPlayWhenReady: (play: Boolean, joinCollectorDispatch: Boolean) -> Deferred<Result<Boolean>>,
    private val observeProgressWithIntervalHandle: (
        getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ) -> Flow<Duration>,
    private val observeBufferedProgressWithIntervalHandle: (
        getInterval: (progress: Duration, duration: Duration, speed: Float) -> Duration
    ) -> Flow<Duration>,
    private val observeDuration: () -> Flow<Duration>,
    private val requestPlaybackMovePrevious: (
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousIndex: Int,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    private val requestPlaybackMoveNext: (
        expectCurrentQueue: OldPlaybackQueue,
        expectFromIndex: Int,
        expectFromId: String,
        expectNextIndex: Int,
        expectNextId: String
    ) -> Deferred<Result<Boolean>>,
    private val coroutineScope: CoroutineScope
) {

    private var isPagerSurfaceDark by mutableStateOf(false)
    private var isButtonControlSurfaceDark by mutableStateOf(false)

    var topPosition by mutableStateOf(0.dp)
    var topPositionFromAnchor by mutableStateOf(0.dp)

    val transitionState = CompactControlTransitionState(
        getLayoutHeight = getLayoutHeight,
        getLayoutWidth = getLayoutWidth,
        getLayoutBottomSpacing = getLayoutBottomOffset,
        observeQueue = observePlaybackQueue
    )

    val backgroundState = CompactControlBackgroundState(
        observeArtwork = observeArtwork,
        observeQueue = observePlaybackQueue,
        onComposingBackgroundColor = {
            val dark = it.luminance() <= 0.4
            isPagerSurfaceDark = dark
            isButtonControlSurfaceDark = dark
        },
        coroutineScope = coroutineScope,
        getBackgroundClickedHandle = getBaseClickedHandle
    )

    val artworkDisplayState = CompactControlArtworkState(
        observeArtwork = observeArtwork,
        observeQueue = observePlaybackQueue,
        getArtworkClickedHandle = getArtworkClickedHandle
    )

    @OptIn(ExperimentalPagerApi::class)
    val pagerState = CompactControlPagerState(
        layoutState = PagerState(0),
        observeArtwork = observeArtwork,
        observeMetadata = observeMetadata,
        observeQueue = observePlaybackQueue,
        isSurfaceDark = ::isPagerSurfaceDark,
        requestPlaybackMovePrevious = requestPlaybackMovePrevious,
        requestPlaybackMoveNext = requestPlaybackMoveNext,
    )

    val controlsState = CompactButtonControlsState(
        observePlaybackProperties = observePlaybackProperties,
        setPlayWhenReady = setPlayWhenReady,
        isSurfaceDark = ::isButtonControlSurfaceDark
    )

    val timeBarState = CompactControlTimeBarState(
        observeProgressWithIntervalHandle = observeProgressWithIntervalHandle,
        observeBufferedProgressWithIntervalHandle = observeBufferedProgressWithIntervalHandle,
        observeQueue = observePlaybackQueue,
        observeDuration = observeDuration,
    )
}

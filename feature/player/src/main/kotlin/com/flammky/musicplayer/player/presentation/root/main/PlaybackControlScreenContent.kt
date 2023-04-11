package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

@Composable
fun rememberPlaybackControlScreenContentState(
    key: Any,
    requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
    requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
    requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectToIndex: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekPositionAsync: (
        expectFromId: String,
        expectFromDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    requestMoveQueueItemAsync: (
        from: Int,
        expectFromId: String,
        to: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    requestPlayAsync: () -> Deferred<Result<Boolean>>,
    requestPauseAsync: () -> Deferred<Result<Boolean>>,
    requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>,
    requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>,
    observeQueue: () -> Flow<OldPlaybackQueue>,
    observePlaybackProperties: () -> Flow<PlaybackProperties>,
    observeDuration: () -> Flow<Duration>,
    observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>,
    observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    observeArtwork: (String) -> Flow<Any?>,
): PlaybackControlScreenContentState {
    return remember(key) {
        PlaybackControlScreenContentState(
            requestSeekNextAsync,
            requestSeekPreviousAsync,
            requestSeekNextWithExpectAsync,
            requestSeekPreviousWithExpectAsync,
            requestSeekAsync,
            requestSeekPositionAsync,
            requestMoveQueueItemAsync,
            requestPlayAsync,
            requestPauseAsync,
            requestToggleRepeatAsync,
            requestToggleShuffleAsync,
            observeQueue,
            observePlaybackProperties,
            observeDuration,
            observePositionWithIntervalHandle,
            observeTrackMetadata,
            observeArtwork
        )
    }
}

@Composable
fun PlaybackControlScreenContent(
    state: PlaybackControlScreenContentState
) = state.coordinator.ComposeLayout(
    pager = {
        QueuePager(
            state = rememberQueuePagerState(
                key = this,
                observeQueue = observePlaybackQueue,
                observeArtwork = observeArtwork,
                requestSeekNextWithExpectAsync = requestSeekNextWithExpectAsync,
                requestSeekPreviousWithExpectAsync = requestSeekPreviousWithExpectAsync
            )
        )
    },
    description = {
        PlaybackTrackDescription(
            state = rememberPlaybackTrackDescriptionState(
                key = this,
                observeCurrentMetadata = observeCurrentMetadata
            )
        )
    },
    timeBar = {
        PlaybackTimeBar(
            state = rememberPlaybackControlTimeBarState(
                key = this,
                observeQueue = observeQueue,
                onRequestSeek = onRequestSeek,
                observeDuration = observeDuration,
                observeProgressWithIntervalHandle = observeProgressWithIntervalHandle
            )
        )
    },
    property = {
        PlaybackPropertyControl(
            state = rememberPlaybackPropertyControlState(
                key = this,
                propertiesFlow = propertiesFlow,
                play = play,
                pause = pause,
                toggleShuffleMode = toggleShuffleMode,
                toggleRepeatMode = toggleRepeatMode,
                seekNext = seekNext,
                seekPrevious = seekPrevious
            )
        )
    },
    extra = {  }
)

class PlaybackControlScreenContentState(
    requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
    requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
    requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectToIndex: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    requestSeekPositionAsync: (
        expectFromId: String,
        expectFromDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    requestMoveQueueItemAsync: (
        from: Int,
        expectFromId: String,
        to: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    requestPlayAsync: () -> Deferred<Result<Boolean>>,
    requestPauseAsync: () -> Deferred<Result<Boolean>>,
    requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>,
    requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>,
    observeQueue: () -> Flow<OldPlaybackQueue>,
    observePlaybackProperties: () -> Flow<PlaybackProperties>,
    observeDuration: () -> Flow<Duration>,
    observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>,
    observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    observeArtwork: (String) -> Flow<Any?>,
) {
    val coordinator = PlaybackControlScreenContentCoordinator(
        requestSeekNextAsync,
        requestSeekPreviousAsync,
        requestSeekNextWithExpectAsync,
        requestSeekPreviousWithExpectAsync,
        requestSeekAsync,
        requestSeekPositionAsync,
        requestMoveQueueItemAsync,
        requestPlayAsync,
        requestPauseAsync,
        requestToggleRepeatAsync,
        requestToggleShuffleAsync,
        observeQueue,
        observePlaybackProperties,
        observeDuration,
        observePositionWithIntervalHandle,
        observeTrackMetadata,
        observeArtwork
    )
}

class PlaybackControlScreenContentCoordinator(
    val requestSeekNextAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekPreviousAsync: () -> Deferred<Result<Boolean>>,
    val requestSeekNextWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPreviousWithExpectAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectPreviousId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekAsync: (
        expectFromIndex: Int,
        expectFromId: String,
        expectToIndex: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekPositionAsync: (
        expectFromId: String,
        expectFromDuration: Duration,
        percent: Float
    ) -> Deferred<Result<Boolean>>,
    val requestMoveQueueItemAsync: (
        from: Int,
        expectFromId: String,
        to: Int,
        expectToId: String
    ) -> Deferred<Result<Boolean>>,
    val requestPlayAsync: () -> Deferred<Result<Boolean>>,
    val requestPauseAsync: () -> Deferred<Result<Boolean>>,
    val requestToggleRepeatAsync: () -> Deferred<Result<Boolean>>,
    val requestToggleShuffleAsync: () -> Deferred<Result<Boolean>>,
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observePlaybackProperties: () -> Flow<PlaybackProperties>,
    val observeDuration: () -> Flow<Duration>,
    val observePositionWithIntervalHandle: (
        getInterval: (
            event: Boolean,
            position: Duration,
            bufferedPosition: Duration,
            duration: Duration,
            speed: Float
        ) -> Duration
    ) -> Flow<Duration>,
    val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    val observeArtwork: (String) -> Flow<Any?>,
) {

    val layoutCoordinator = PlaybackControlScreenContentLayoutCoordinator()

    interface PagerScope {
        val observePlaybackQueue: () -> Flow<OldPlaybackQueue>
        val observeArtwork: (String) -> Flow<Any?>
        val requestSeekNextWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>
        val requestSeekPreviousWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>
    }

    interface DescriptionScope {
        val observeCurrentMetadata: () -> Flow<MediaMetadata?>
    }

    interface TimeBarScope {
        val observeQueue: () -> Flow<OldPlaybackQueue>
        val onRequestSeek: (
            expectFromId: String,
            expectDuration: Duration,
            percent: Float
        ) -> Deferred<Result<Boolean>>
        val observeDuration: () -> Flow<Duration>
        val observeProgressWithIntervalHandle: (
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ) -> Flow<Duration>
    }

    interface PropertyScope {
        val propertiesFlow: () -> Flow<PlaybackProperties>
        val play: () -> Unit
        val pause: () -> Unit
        val toggleShuffleMode: () -> Unit
        val toggleRepeatMode: () -> Unit
        val seekNext: () -> Unit
        val seekPrevious: () -> Unit
    }

    interface ExtraScope {

    }

    private class PagerScopeImpl(
        override val observePlaybackQueue: () -> Flow<OldPlaybackQueue>,
        override val observeArtwork: (String) -> Flow<Any?>,
        override val requestSeekNextWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>,
        override val requestSeekPreviousWithExpectAsync: (
            expectFromIndex: Int,
            expectFromId: String,
            expectPreviousId: String
        ) -> Deferred<Result<Boolean>>
    ) : PagerScope

    private class DescriptionScopeImpl(
        override val observeCurrentMetadata: () -> Flow<MediaMetadata?>
    ) : DescriptionScope

    private class TimeBarScopeImpl(
        override val observeQueue: () -> Flow<OldPlaybackQueue>,
        override val onRequestSeek: (
            expectFromId: String,
            expectDuration: Duration,
            percent: Float
        ) -> Deferred<Result<Boolean>>,
        override val observeDuration: () -> Flow<Duration>,
        override val observeProgressWithIntervalHandle: (
            getInterval: (
                event: Boolean,
                position: Duration,
                bufferedPosition: Duration,
                duration: Duration,
                speed: Float
            ) -> Duration
        ) -> Flow<Duration>
    ) : TimeBarScope

    private class PropertyScopeImpl(
        override val propertiesFlow: () -> Flow<PlaybackProperties>,
        override val play: () -> Unit,
        override val pause: () -> Unit,
        override val toggleShuffleMode: () -> Unit,
        override val toggleRepeatMode: () -> Unit,
        override val seekNext: () -> Unit,
        override val seekPrevious: () -> Unit
    ) : PropertyScope

    private class ExtraScopeImpl(

    ) : ExtraScope {

    }

    @Composable
    fun ComposeLayout(
        pager: @Composable PagerScope.() -> Unit,
        description: @Composable DescriptionScope.() -> Unit,
        timeBar: @Composable TimeBarScope.() -> Unit,
        property: @Composable PropertyScope.() -> Unit,
        extra: @Composable ExtraScope.() -> Unit
    ) {
        val upPager = rememberUpdatedState(pager)
        val upDescription = rememberUpdatedState(description)
        val upTimeBar = rememberUpdatedState(timeBar)
        val upProperty = rememberUpdatedState(property)
        val upExtra = rememberUpdatedState(extra)
        with(layoutCoordinator) {
            PlaceLayout(
                pager = { /*TODO*/ },
                description = { /*TODO*/ },
                timeBar = { /*TODO*/ },
                property = { /*TODO*/ },
                extra = { /*TODO*/ }
            )
        }
    }
}

class PlaybackControlScreenContentLayoutCoordinator(

) {

    @Composable
    fun PlaceLayout(
        pager: @Composable () -> Unit,
        description: @Composable () -> Unit,
        timeBar: @Composable () -> Unit,
        property: @Composable () -> Unit,
        extra: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                pager()
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                description()
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                timeBar()
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                property()
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                extra()
            }
        }
    }
}
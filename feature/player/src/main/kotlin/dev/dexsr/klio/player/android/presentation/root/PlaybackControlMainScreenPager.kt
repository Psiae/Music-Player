package dev.dexsr.klio.player.android.presentation.root

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.player.android.presentation.root.main.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Composable
fun PlaybackControlMainScreenPager(
    state: PlaybackControlMainScreenState,
    contentPadding: PaddingValues
) {
    // TODO: impl
    BoxWithConstraints(
        modifier = Modifier
            .height(300.dp)
            .fillMaxWidth()
    ) {
        if (false) {
            FoundationDescriptionPager(
                modifier = Modifier,
                state = remember(state) {
                    FoundationDescriptionPagerState(state)
                },
                contentPadding = contentPadding
            )
        } else {
            PlaybackPagerDef(
                modifier = Modifier,
                state = rememberPlaybackPagerState(pc = state.playbackController),
                mediaMetadataProvider = state.mediaMetadataProvider,
                itemPadding = contentPadding
            )
        }
    }
}

@Composable
fun rememberPlaybackPagerState(
    pc: PlaybackController
): LazyPlaybackPagerState {

    return rememberPlaybackPagerState(player = rememberPlaybackPagerPlayer(pc = pc))
}

@Composable
fun rememberPlaybackPagerState(
    pc: RootCompactPlaybackController
): LazyPlaybackPagerState {

    return rememberPlaybackPagerState(player = rememberPlaybackPagerPlayer(pc = pc))
}

@Composable
fun rememberPlaybackPagerState(
    player: PlaybackPagerPlayer
): LazyPlaybackPagerState {
    return remember(player) {
        LazyPlaybackPagerState(player)
    }
}


@Composable
fun rememberPlaybackPagerPlayer(
    pc: PlaybackController
): PlaybackPagerPlayer {

    val coroutineScope = rememberCoroutineScope()

    return remember(pc) {
        object : PlaybackPagerPlayer() {

            override fun timelineAndStepAsFlow(range: Int): Flow<Pair<PlaybackTimeline, Int>> {
                return flow {
                    val channel = Channel<Pair<PlaybackTimeline, Int>>(Channel.CONFLATED)
                    val disposable = pc.invokeOnTimelineChanged(range) { timeline, step ->
                        channel.trySend(timeline to step)
                    }
                    try {
                        for (element in channel) {
                            emit(element)
                        }
                    } finally {
                        disposable.dispose()
                    }
                }
            }

            override fun seekToNextMediaItemAsync(resultRange: Int): Deferred<Result<PlaybackTimeline>> {
                return coroutineScope.async {
                    runCatching {
                        pc.seekToNextMediaItemAsync().await()
                        pc.getTimelineAsync(resultRange).await()
                    }
                }
            }

            override fun seekToPreviousMediaItemAsync(resultRange: Int): Deferred<Result<PlaybackTimeline>> {
                return coroutineScope.async {
                    runCatching {
                        pc.seekToPreviousMediaItemAsync().await()
                        pc.getTimelineAsync(resultRange).await()
                    }
                }
            }
        }
    }
}

@Composable
fun rememberPlaybackPagerPlayer(
    pc: RootCompactPlaybackController
): PlaybackPagerPlayer {

    val coroutineScope = rememberCoroutineScope()

    return remember(pc) {
        object : PlaybackPagerPlayer() {

            override fun timelineAndStepAsFlow(range: Int): Flow<Pair<PlaybackTimeline, Int>> {
                return flow {
                    val channel = Channel<Pair<PlaybackTimeline, Int>>(Channel.CONFLATED)
                    val disposable = pc.invokeOnTimelineChanged(range) { timeline, step ->
                        channel.trySend(timeline to step)
                    }
                    try {
                        for (element in channel) {
                            emit(element)
                        }
                    } finally {
                        disposable.dispose()
                    }
                }
            }

            override fun seekToNextMediaItemAsync(resultRange: Int): Deferred<Result<PlaybackTimeline>> {
                return coroutineScope.async {
                    runCatching {
                        pc.seekToNextMediaItemAsync().await()
                        pc.getTimelineAsync(resultRange).await()
                    }
                }
            }

            override fun seekToPreviousMediaItemAsync(resultRange: Int): Deferred<Result<PlaybackTimeline>> {
                return coroutineScope.async {
                    runCatching {
                        pc.seekToPreviousMediaItemAsync().await()
                        pc.getTimelineAsync(resultRange).await()
                    }
                }
            }
        }
    }
}
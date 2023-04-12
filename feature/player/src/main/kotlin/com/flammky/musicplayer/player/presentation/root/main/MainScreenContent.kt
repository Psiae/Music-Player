package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.isDarkAsState
import com.flammky.musicplayer.player.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun PlaybackControlScreenCoordinator.MainRenderScope.MainScreenContent(
    modifier: Modifier = Modifier
) {
    val observeCurrentMetadataFlowBuilder = rememberObserveCurrentMetadataFlowBuilder()

    PlaceContents(
        modifier = modifier,
        transitionState = transitionState,
        pager = {
            QueuePager(
                state = rememberQueuePagerState(
                    key = dataSource to intents,
                    observeQueue = dataSource.observeQueue,
                    observeArtwork = dataSource.observeArtwork,
                    requestSeekNextWithExpectAsync = intents.requestSeekNextWithExpectAsync,
                    requestSeekPreviousWithExpectAsync = intents.requestSeekPreviousWithExpectAsync
                )
            )
        },
        description = {
            PlaybackTrackDescription(
                state = rememberPlaybackTrackDescriptionState(
                    key = dataSource,
                    observeCurrentMetadata = observeCurrentMetadataFlowBuilder
                )
            )
        },
        timeBar = {
            PlaybackTimeBar(
                state = rememberPlaybackControlTimeBarState(
                    key = dataSource,
                    observeQueue = dataSource.observeQueue,
                    onRequestSeek = intents.requestSeekPositionAsync,
                    observeDuration = dataSource.observeDuration,
                    observeProgressWithIntervalHandle = dataSource.observePositionWithIntervalHandle
                )
            )
        },
        properties = {
            PlaybackPropertyControl(
                state = rememberPlaybackPropertyControlState(
                    key = dataSource to intents,
                    propertiesFlow = dataSource.observePlaybackProperties,
                    play = { intents.requestPlayAsync() },
                    pause = { intents.requestPauseAsync() },
                    toggleShuffleMode = { intents.requestToggleShuffleAsync() },
                    toggleRepeatMode = { intents.requestToggleRepeatAsync() },
                    seekNext = { intents.requestSeekNextAsync() },
                    seekPrevious = { intents.requestSeekPreviousAsync() }
                )
            )
        },
        extra = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                intents.showQueue()
                            }
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                            painter = painterResource(id = R.drawable.glyph_playlist_100px),
                            contentDescription = "queue",
                            tint = Theme.backgroundContentColorAsState().value
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PlaybackControlScreenCoordinator.MainRenderScope.rememberObserveCurrentMetadataFlowBuilder(): () -> Flow<MediaMetadata?> {

    val readerCountState = remember {
        mutableStateOf(0)
    }

    val currentMetadataState = remember {
        mutableStateOf<MediaMetadata?>(null)
    }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(
        key1 = dataSource,
        effect = {
            val supervisor = SupervisorJob()

            // currentMetadata observer
            coroutineScope.launch(supervisor) {
                var job: Job? = null
                snapshotFlow { readerCountState.value }
                    .collect { count ->
                        if (count == 0) {
                            job?.cancel()
                        } else if (count >= 1) {
                            // snapshotFlow is conflated by default
                            if (job?.isActive == true) return@collect
                            job = launch {
                                var latestTransformer: Job? = null
                                dataSource.observeQueue()
                                    .map {
                                        it.list.getOrNull(it.currentIndex)
                                    }
                                    .distinctUntilChanged()
                                    .collect { id ->
                                        latestTransformer?.cancel()
                                        if (id == null) {
                                            currentMetadataState.value = null
                                            return@collect
                                        }
                                        latestTransformer = launch {
                                            dataSource.observeTrackMetadata(id)
                                                .collect { currentMetadataState.value = it }
                                        }
                                    }
                            }
                        }
                    }
            }

            onDispose {
                supervisor.cancel()
            }
        }
    )



    return remember(dataSource) {
        {
            flow {
                readerCountState.value++

                try {
                    snapshotFlow { currentMetadataState.value }.collect(this)
                } finally {
                    readerCountState.value--
                }
            }
        }
    }
}

@Composable
private fun PlaceContents(
    modifier: Modifier,
    transitionState: MainContainerTransitionState,
    pager: @Composable () -> Unit,
    description: @Composable () -> Unit,
    timeBar: @Composable () -> Unit,
    properties: @Composable () -> Unit,
    extra: @Composable () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (transitionState.rememberFullTransitionRendered) 1f else 0f,
        animationSpec = tween(
            if (transitionState.rememberFullTransitionRendered && Theme.isDarkAsState().value) 150 else 0
        )
    )
    Column(modifier = modifier
        .fillMaxSize()
        .alpha(animatedAlpha)) {

        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .alpha(animatedAlpha)
        ) {
            pager()
        }
        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .alpha(animatedAlpha)
        ) {
            description()
        }
        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .alpha(animatedAlpha)
        ) {
            timeBar()
        }
        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .alpha(animatedAlpha)
        ) {
            properties()
        }
        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .alpha(animatedAlpha)
        ) {
            extra()
        }
    }
}
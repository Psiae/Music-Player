package com.flammky.musicplayer.player.presentation.root

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.androidx.content.context.findActivity
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.compose.NoInlineBox
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.player.R
import com.flammky.musicplayer.player.presentation.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.root.main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.flammky.musicplayer.base.R as BaseResource

@Composable
internal fun RootPlaybackControl(
    modifier: Modifier = Modifier,
    state: RootPlaybackControlState
) {
    RootPlaybackControl(
        modifier = modifier,
        state = state,
        content = {
            TransitioningContentLayout()
        },
    )
}

@Composable
private fun RootPlaybackControl(
    modifier: Modifier,
    state: RootPlaybackControlState,
    content: @Composable RootPlaybackControlComposition.() -> Unit,
) {
    if (state.showSelfState.value) {
        LockScreenOrientation(landscape = false)
    }

    val viewModel: PlaybackControlViewModel = hiltViewModel()

    val controllerState = remember {
        mutableStateOf<PlaybackController?>(null)
    }.apply {
        DisposableEffect(
            this,
            viewModel,
            effect = {
                val controller = viewModel.createUserPlaybackController(state.user)
                value = controller
                onDispose { controller.dispose() }
            }
        )
    }


    NoInline {
        val controller = controllerState.value
            ?: return@NoInline
        BoxWithConstraints(
            modifier = modifier
        ) {
            val mainScope = state.coordinator.layoutComposition
            // Main Content
            mainScope
                .apply {
                    fullyVisibleHeightTarget = constraints.maxHeight
                }
                .run {
                    state.currentComposition = this
                    content()
                }
        }
    }
}

@Composable
private fun RootPlaybackControlComposition.TransitioningContentLayout() {
    // Think if there is somewhat better way to do this
    val transitionHeightState = updateTransition(targetState = showSelf(), label = "")
        .animateInt(
            label = "Playback Control Transition",
            targetValueByState = { targetShowSelf ->
                if (targetShowSelf) fullyVisibleHeightTarget else 0
            },
            transitionSpec = {
                remember(targetState) {
                    tween(
                        durationMillis = if (targetState) {
                            if (rememberFullyTransitioned) {
                                0
                            } else {
                                350
                            }
                        } else {
                            250
                        },
                        easing = if (targetState) FastOutSlowInEasing else LinearOutSlowInEasing
                    )
                }
            }
        ).apply {
            LaunchedEffect(
                this@TransitioningContentLayout, this.value, fullyVisibleHeightTarget,
                block = {
                    visibleHeight = value
                    if (visibleHeight == fullyVisibleHeightTarget) {
                        rememberFullyTransitioned = true
                    } else if (visibleHeight == 0) {
                        rememberFullyTransitioned = false
                    }
                }
            )
        }

    val blockerFraction =
        if (showSelf() || visibleHeight > 0) 1f else 0f

    // Inline Box to immediately consume input during transition
    Box(
        modifier = Modifier
            .fillMaxSize(blockerFraction)
            .clickable(
                enabled = true,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        if (visibleHeight > 0) {
            val localDensity = LocalDensity.current
            val absBackgroundColor = Theme.absoluteBackgroundColorAsState().value
            Layout(
                modifier = Modifier
                    .runRemember {
                        Modifier.fillMaxWidth()
                    }
                    .runRemember(
                        localDensity,
                        fullyVisibleHeightTarget,
                        transitionHeightState.value
                    ) {
                        this
                            .height(
                                height = with(localDensity) {
                                    fullyVisibleHeightTarget.toDp()
                                }
                            )
                            .offset(
                                y = with(localDensity) {
                                    (fullyVisibleHeightTarget - transitionHeightState.value).toDp()
                                }
                            )
                    }
                    .runRemember(absBackgroundColor) {
                        this
                            .background(
                                color = absBackgroundColor.copy(alpha = 0.97f)
                            )
                    },
                queue = {
                    Queue(parent = it)
                },
                background = {
                    RadialPlaybackBackground(composition = it)
                },
                toolbar = {
                    PlaybackControlToolBar(
                        state = remember(it) {
                            PlaybackControlToolBarState(
                                onDismissClick = it.dismiss
                            )
                        }
                    )
                },
                pager = {
                    QueuePager(
                        state = remember(it) {
                            QueuePagerState(
                                observeQueue = it.observeQueue,
                                observeArtwork = it.observeArtwork,
                                requestSeekNextWithExpectAsync = it.requestSeekNextWithExpectAsync,
                                requestSeekPreviousWithExpectAsync = it.requestSeekPreviousWithExpectAsync
                            )
                        }
                    )
                },
                description = {
                    PlaybackTrackDescription(
                        state = remember(it) {
                            PlaybackDescriptionState(
                                observeCurrentMetadata = {
                                    flow {
                                        it.currentMetadataReaderCount++
                                        runCatching {
                                            snapshotFlow { it.currentPlaybackMetadata }
                                                .collect(this)
                                        }
                                        it.currentMetadataReaderCount--
                                    }
                                }
                            )
                        }
                    )
                },
                seekbar = {
                    PlaybackTimeBar(
                        state = remember(it) {
                            PlaybackControlTimeBarState(
                                observeQueue = it.observeQueue,
                                onRequestSeek = it.requestSeekPositionAsync,
                                observeDuration = it.observeDuration,
                                observeProgressWithIntervalHandle = it.observePositionWithIntervalHandle,
                            )
                        }
                    )
                },
                primaryControlRow = {
                    PlaybackPropertyControl(
                        state = remember(it) {
                            PlaybackPropertyControlState(
                                propertiesFlow = it.observePlaybackProperties,
                                play = { it.requestPlayAsync() },
                                pause = { it.requestPauseAsync() },
                                toggleShuffleMode = { it.requestToggleShuffleAsync() },
                                toggleRepeatMode = { it.requestToggleRepeatAsync() },
                                seekNext = { it.requestSeekNextAsync() },
                                seekPrevious = { it.requestSeekPreviousAsync() }
                            )
                        }
                    )
                },
                secondaryControlRow = {
                    SecondaryControl(composition = it)
                }
            )
        }
    }
}

@Composable
private fun RootPlaybackControlComposition.Layout(
    modifier: Modifier,
    queue: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    background: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    toolbar: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    pager: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    description: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    seekbar: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    primaryControlRow: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    secondaryControlRow: @Composable BoxScope.(composition: RootPlaybackControlComposition) -> Unit,
    // TODO: Lyrics ?
) {
    val composition = this
    val coroutineScope = rememberCoroutineScope()
    val animatedAlpha by animateFloatAsState(
        targetValue = if (composition.rememberFullyTransitioned) 1f else 0f,
        animationSpec = tween(
            if (composition.rememberFullyTransitioned && Theme.isDarkAsState().value) 150 else 0
        )
    )
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(animatedAlpha)
        ) {
            background(composition)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                toolbar(composition)
            }
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .alpha(animatedAlpha)
            ) {
                pager(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .alpha(animatedAlpha)
            ) {
                description(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .alpha(animatedAlpha)
            ) {
                seekbar(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .alpha(animatedAlpha)
            ) {
                primaryControlRow(composition)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .alpha(animatedAlpha)
            ) {
                secondaryControlRow(composition)
            }
        }
        queue(composition)
    }
    DisposableEffect(key1 = composition, effect = {
        val supervisor = SupervisorJob()

        // queue observer
        coroutineScope.launch(supervisor) {
            var job: Job? = null
            snapshotFlow { composition.currentQueueReaderCount }
                .collect { count ->
                    if (count == 0) {
                        job?.cancel()
                    } else if (count >= 1) {
                        // snapshotFlow is conflated by default
                        if (job?.isActive == true) return@collect
                        job = launch {
                            composition.observeQueue()
                                .collect {
                                    composition.currentQueue = it
                                }
                        }
                    }
                }
        }

        // currentMetadata observer
        coroutineScope.launch(supervisor) {
            var job: Job? = null
            snapshotFlow { composition.currentMetadataReaderCount }
                .collect { count ->
                    if (count == 0) {
                        job?.cancel()
                    } else if (count >= 1) {
                        // snapshotFlow is conflated by default
                        if (job?.isActive == true) return@collect
                        job = launch {
                            var latestTransformer: Job? = null
                            composition.currentQueueReaderCount++
                            try {
                                snapshotFlow { composition.currentQueue }
                                    .map {
                                        it.list.getOrNull(it.currentIndex)
                                    }
                                    .distinctUntilChanged()
                                    .collect {
                                        latestTransformer?.cancel()
                                        if (it == null) {
                                            composition.currentPlaybackMetadata = null
                                            return@collect
                                        }
                                        latestTransformer = launch {
                                            composition
                                                .observeTrackMetadata(it)
                                                .collect(composition::currentPlaybackMetadata::set)
                                        }
                                    }
                            } finally {
                                composition.currentQueueReaderCount--
                            }
                        }
                    }
                }
        }

        // currentBitmap observer
        coroutineScope.launch(supervisor) {
            var job: Job? = null
            snapshotFlow { composition.currentBitmapReaderCount }
                .collect { count ->
                    if (count == 0) {
                        job?.cancel()
                    } else if (count >= 1) {
                        // snapshotFlow is conflated by default
                        if (job?.isActive == true) return@collect
                        job = launch {
                            var latestTransformer: Job? = null
                            composition.currentQueueReaderCount++
                            try {
                                composition.observeQueue()
                                    .collect {
                                        latestTransformer?.cancel()
                                        latestTransformer = launch {
                                            it.list.getOrNull(it.currentIndex)
                                                ?.let {
                                                    composition.observeArtwork(it)
                                                        .collect collectArtwork@ { art ->
                                                            latestTransformer?.cancel()
                                                            if (art is Bitmap) {
                                                                composition.currentPlaybackBitmap = art
                                                                return@collectArtwork
                                                            }
                                                            // else we need to collect the biggest layout viewport
                                                            // within the composition and load the bitmap according to
                                                            // the source kind
                                                            composition.currentPlaybackBitmap = null
                                                        }
                                                }
                                                ?: run {
                                                    composition.currentPlaybackBitmap = null
                                                }
                                        }
                                    }
                            } finally {
                                composition.currentQueueReaderCount--
                            }
                        }
                    }
                }
        }

        // currentPalette observer
        coroutineScope.launch(supervisor) {
            var job: Job? = null
            snapshotFlow { composition.currentPaletteReaderCount }
                .collect { count ->
                    if (count == 0) {
                        job?.cancel()
                    } else if (count >= 1) {
                        // snapshotFlow is conflated by default
                        if (job?.isActive == true) return@collect
                        job = launch {
                            var latestTransformer: Job? = null
                            composition.currentBitmapReaderCount++
                            try {
                                snapshotFlow { composition.currentPlaybackBitmap }
                                    .collect collectMetadata@ {
                                        latestTransformer?.cancel()
                                        if (it == null) {
                                            composition.currentPlaybackPalette = null
                                            return@collectMetadata
                                        }
                                        latestTransformer = launch(Dispatchers.Default) {
                                            val gen = Palette
                                                .from(it).maximumColorCount(16).generate()
                                            ensureActive()
                                            composition.currentPlaybackPalette = gen
                                        }
                                    }
                            } finally {
                                composition.currentBitmapReaderCount--
                            }
                        }
                    }
                }
        }

        onDispose { supervisor.cancel() }
    })
}

@Composable
private fun BoxScope.RadialPlaybackBackground(
    composition: RootPlaybackControlComposition
) {
    val darkTheme by Theme.isDarkAsState()
    val absoluteBackgroundColor by Theme.absoluteBackgroundColorAsState()
    val backgroundColor by Theme.backgroundColorAsState()
    val compositeBase =
        if (isSystemInDarkTheme()) {
            backgroundColor
        } else {
            absoluteBackgroundColor
        }
    val currentPaletteColor by remember(composition, darkTheme) {
        derivedStateOf {
            composition.currentPlaybackPalette.run {
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
                    backgroundColor
                }
            }
        }
    }
    val radialColorBase by animateColorAsState(
        targetValue = currentPaletteColor,
        animationSpec = tween(500)
    )
    BoxWithConstraints {
        Box(
            modifier = remember {
                Modifier.fillMaxSize()
            }.runRemember(radialColorBase, compositeBase, constraints) {
                this.background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            radialColorBase.copy(alpha = 0.55f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.45f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.35f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.2f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.15f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.1f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.05f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.0f).compositeOver(compositeBase)
                        ),
                        center = Offset(
                            constraints.maxWidth.toFloat() / 2,
                            constraints.maxHeight.toFloat() / 3.5f
                        ),
                        radius = constraints.maxWidth.toFloat() * 0.9f
                    )
                )
            }
        )
    }
    DisposableEffect(key1 = composition, effect = {
        composition.currentPaletteReaderCount++
        onDispose { composition.currentPaletteReaderCount-- }
    })
}

@Composable
private fun BoxScope.Toolbar(
    composition: RootPlaybackControlComposition
) {
    Row(modifier = Modifier
        .height(40.dp)
        .fillMaxWidth()
        .align(Alignment.Center)
        .padding(horizontal = 15.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dismissInteractionSource = remember { MutableInteractionSource() }
        val size = if (dismissInteractionSource.collectIsPressedAsState().value) 24.dp else 26.dp
        Box(
            modifier = Modifier
                .size(35.dp)
                .align(Alignment.CenterVertically)
                .clickable(
                    interactionSource = dismissInteractionSource,
                    indication = null,
                    enabled = true,
                    onClickLabel = null,
                    role = null,
                    onClick = { composition.dismiss() }
                )
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(size),
                painter = painterResource(id = R.drawable.ios_glyph_expand_arrow_down_100),
                contentDescription = "close",
                tint = Theme.backgroundContentColorAsState().value
            )
        }
        Spacer(modifier = Modifier.weight(2f))
        Icon(
            modifier = Modifier.size(26.dp),
            painter = painterResource(id = BaseResource.drawable.more_vert_48px),
            contentDescription = "more",
            tint = Theme.backgroundContentColorAsState().value
        )
    }
}

@Composable
private fun BoxScope.Description(
    composition: RootPlaybackControlComposition
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val metadata by remember(composition) {
                derivedStateOf { composition.currentPlaybackMetadata }
            }.apply {
                DisposableEffect(key1 = composition, effect = {
                    composition.currentMetadataReaderCount++
                    onDispose { composition.currentMetadataReaderCount-- }
                })
            }
            PlaybackDescriptionTitle(
                text = remember(metadata) {
                    if (metadata === MediaMetadata.UNSET) {
                        ""
                    } else {
                        metadata?.title
                            ?: metadata.run {
                                (this as? AudioFileMetadata)?.file
                                    ?.let { fileMetadata ->
                                        fileMetadata.fileName
                                            ?.ifBlank {
                                                (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
                                            }
                                            ?: ""
                                    }
                                    ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
                            }
                            ?: "TITLE_NONE"
                    }
                }
            )
            PlaybackDescriptionSubtitle(
                text = remember(metadata) {
                    if (metadata === MediaMetadata.UNSET) {
                        ""
                    } else {
                        (metadata as? AudioMetadata)
                            ?.let {
                                it.albumArtistName ?: it.artistName
                            }
                            ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
                            ?: "SUBTITLE_NONE"
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaybackDescriptionTitle(text: String) {
    Text(
        text = text,
        color = Theme.surfaceContentColorAsState().value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PlaybackDescriptionSubtitle(text: String) {
    Text(
        text = text,
        color = Theme.surfaceContentColorAsState().value,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}


@Composable
private fun BoxScope.Queue(
    parent: RootPlaybackControlComposition
) {
    BoxWithConstraints {
        if (!parent.showPlaybackQueue && parent.queueComposition == null) {
            return@BoxWithConstraints
        }
        parent.queueComposition = remember(parent) {
            RootPlaybackControlQueueScope(
                parent,
                parent.requestMoveQueueItemAsync,
                parent.requestSeekAsync
            )
        }.apply {
            parent.restoreQueueComposition(this)
            fullyVisibleHeightTarget = constraints.maxHeight
        }
        RootPlaybackControlQueueScreen(composition = parent.queueComposition!!)
    }
}

@Composable
private fun BoxScope.PrimaryControlRow(
    composition: RootPlaybackControlComposition
) {
    // TODO: Tech Debt
    val coroutineScope = rememberCoroutineScope()

    val propertiesState = remember {
        mutableStateOf(PlaybackConstants.PROPERTIES_UNSET)
    }

    DisposableEffect(key1 = composition, effect = {
        val supervisor = SupervisorJob()
        coroutineScope.launch(supervisor) {
            composition.observePlaybackProperties()
                .collect {
                    propertiesState.value = it
                }
        }
        onDispose {
            supervisor.cancel()
            propertiesState.value = PlaybackConstants.PROPERTIES_UNSET
        }
    })


    Box(modifier = Modifier
        .height(40.dp)
        .fillMaxWidth(0.8f)
        .align(Alignment.Center)
    ) {
        @Suppress("DeferredResultUnused")
        (PlaybackControlButtons(
        playbackPropertiesState = propertiesState,
        play = {
            composition.requestPlayAsync()
        },
        pause = {
            composition.requestPauseAsync()
        },
        next = {
            composition.requestSeekNextAsync()
        },
        previous = {
            composition.requestSeekPreviousAsync()
        },
        enableRepeat = {
            composition.requestToggleRepeatAsync()
        },
        enableRepeatAll = {
            composition.requestToggleRepeatAsync()
        },
        disableRepeat = {
            composition.requestToggleRepeatAsync()
        },
        enableShuffle = {
            composition.requestToggleShuffleAsync()
        },
        disableShuffle = {
            composition.requestToggleShuffleAsync()
        }
    ))
    }
}

@Composable
private fun PlaybackControlButtons(
    playbackPropertiesState: State<PlaybackProperties>,
    play: () -> Unit,
    pause: () -> Unit,
    next: () -> Unit,
    previous: () -> Unit,
    enableRepeat: () -> Unit,
    enableRepeatAll: () -> Unit,
    disableRepeat: () -> Unit,
    enableShuffle: () -> Unit,
    disableShuffle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        NoInlineBox(
            modifier = Modifier.size(40.dp)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val size by animateDpAsState(
                targetValue = if (interactionSource.collectIsPressedAsState().value) 27.dp else 30.dp
            )
            val shuffleOn by remember(playbackPropertiesState) {
                derivedStateOf {
                    playbackPropertiesState.value.shuffleMode == ShuffleMode.ON
                }
            }
            Icon(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { if (shuffleOn) disableShuffle() else enableShuffle() }
                    ),
                painter = painterResource(id = R.drawable.ios_glyph_shuffle_100),
                contentDescription = "shuffle",
                tint = if (shuffleOn)
                    Theme.backgroundContentColorAsState().value
                else
                    Color(0xFF787878)
            )
        }

        // Previous
        NoInlineBox(
            modifier = Modifier.size(
                40.dp
            )
        ) {
            // later check for command availability
            val interactionSource = remember { MutableInteractionSource() }
            val size by animateDpAsState(
                targetValue = if (interactionSource.collectIsPressedAsState().value) 32.dp else 35.dp
            )
            Icon(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { previous() }
                    ),
                painter = painterResource(id = R.drawable.ios_glyph_seek_previous_100),
                contentDescription = "previous",
                tint = Theme.backgroundContentColorAsState().value
            )
        }

        // Play / Pause
        NoInlineBox(
            modifier = Modifier.size(
                40.dp
            )
        ) {
            // later check for command availability
            val rememberPlayPainter = painterResource(id = R.drawable.ios_glyph_play_100)
            val rememberPausePainter = painterResource(id = R.drawable.ios_glyph_pause_100)
            val interactionSource = remember { MutableInteractionSource() }
            val size by animateDpAsState(
                targetValue = if (interactionSource.collectIsPressedAsState().value) 37.dp else 40.dp
            )
            val playWhenReady by remember(playbackPropertiesState) {
                derivedStateOf {
                    playbackPropertiesState.value.playWhenReady
                }
            }
            Icon(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { if (playWhenReady) pause() else play() }
                    ),
                painter = if (playWhenReady)
                    rememberPausePainter
                else
                    rememberPlayPainter,
                contentDescription = "play",
                tint = Theme.backgroundContentColorAsState().value
            )
        }

        // Next
        NoInlineBox(
            modifier = Modifier.size(
                40.dp
            )
        ) {
            // later check for command availability

            val interactionSource = remember { MutableInteractionSource() }
            val size by animateDpAsState(
                targetValue = if (interactionSource.collectIsPressedAsState().value) 32.dp else 35.dp
            )
            val hasNext by remember(playbackPropertiesState) {
                derivedStateOf {
                    playbackPropertiesState.value.canSeekNext
                }
            }
            Icon(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { if (hasNext) next() }
                    ),
                painter = painterResource(id = R.drawable.ios_glyph_seek_next_100),
                contentDescription = "next",
                tint = if (hasNext)
                    Theme.backgroundContentColorAsState().value
                else
                    Color(0xFF787878)
            )
        }

        // Repeat
        NoInlineBox(
            modifier = Modifier.size(
                40.dp
            )
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val size by animateDpAsState(
                targetValue = if (interactionSource.collectIsPressedAsState().value) 27.dp else 30.dp
            )
            val repeatMode by remember(playbackPropertiesState) {
                derivedStateOf {
                    playbackPropertiesState.value.repeatMode
                }
            }
            Icon(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            when (repeatMode) {
                                RepeatMode.OFF -> enableRepeat()
                                RepeatMode.ONE -> enableRepeatAll()
                                RepeatMode.ALL -> disableRepeat()
                            }
                        }
                    ),
                painter = when (repeatMode) {
                    RepeatMode.OFF -> painterResource(id = R.drawable.ios_glyph_repeat_100)
                    RepeatMode.ONE -> painterResource(id = R.drawable.ios_glyph_repeat_one_100)
                    RepeatMode.ALL -> painterResource(id = R.drawable.ios_glyph_repeat_100)
                },
                contentDescription = "repeat",
                tint = if (repeatMode == RepeatMode.OFF)
                    Color(0xFF787878)
                else
                    Theme.backgroundContentColorAsState().value
            )
        }
    }
}

@Composable
private fun BoxScope.SecondaryControl(
    composition: RootPlaybackControlComposition
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(30.dp)
            .align(Alignment.Center),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clickable {
                    composition.showPlaybackQueue()
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


@Composable
private fun LockScreenOrientation(landscape: Boolean) {
    val activity = LocalContext.current.findActivity()
        ?: error("cannot Lock Screen Orientation, LocalContext is not an Activity")
    DisposableEffect(key1 = Unit) {
        val original = activity.requestedOrientation
        activity.requestedOrientation =
            if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        onDispose { activity.requestedOrientation = original }
    }
}

@Composable
inline fun <T, R> T.runRemember(
    vararg keys: Any?,
    crossinline block: @DisallowComposableCalls T.() -> R
): R {
    return remember(this, *keys) {
        block()
    }
}
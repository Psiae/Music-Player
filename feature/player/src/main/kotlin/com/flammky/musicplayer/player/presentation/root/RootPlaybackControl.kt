package com.flammky.musicplayer.player.presentation.root

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.androidx.content.context.findActivity
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.common.kotlin.comparable.clampPositive
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.compose.NoInlineBox
import com.flammky.musicplayer.base.media.playback.*
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.player.R
import com.flammky.musicplayer.player.presentation.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.main.PlaybackControlTrackMetadata
import com.flammky.musicplayer.player.presentation.main.compose.Slider
import com.flammky.musicplayer.player.presentation.main.compose.SliderDefaults
import com.google.accompanist.pager.*
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
            ContentTransition()
        },
    )
}

@Composable
private fun RootPlaybackControl(
    modifier: Modifier,
    state: RootPlaybackControlState,
    content: @Composable RootPlaybackControlMainScope.() -> Unit,
) {
    if (state.showMainState.value) {
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
            val mainScope = remember(state, controller) {
                RootPlaybackControlMainScope(
                    state,
                    controller,
                    observeTrackSimpleMetadata = state.viewModel::observeSimpleMetadata,
                    observeTrackMetadata = state.viewModel::observeMediaMetadata,
                    observeArtwork = state.viewModel::observeMediaArtwork,
                    dismiss = state::dismiss,
                ).apply {
                    state.restoreComposition(this)
                }
            }
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
private fun RootPlaybackControlMainScope.ContentTransition() {
    // Think if there is somewhat better way to do this
    val transitionHeightState = updateTransition(targetState = state.showMainState.value, label = "")
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
                this@ContentTransition, this.value, fullyVisibleHeightTarget,
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
        if (state.showMainState.value || visibleHeight > 0) 1f else 0f

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
                    Toolbar(composition = it)
                },
                pager = {
                    RootPlaybackControlPager(
                        state = rememberRootPlaybackControlPagerState(composition = it)
                    )
                },
                description = {
                    Description(composition = it)
                },
                seekbar = {
                    RootPlaybackControlSlider(
                        state = rememberRootPlaybackControlSliderState(parentComposition = it)
                    )
                },
                primaryControlRow = {
                    PrimaryControlRow(composition = it)
                },
                secondaryControlRow = {
                    SecondaryControl(composition = it)
                }
            )
        }
    }
}

@Composable
private fun RootPlaybackControlMainScope.Layout(
    modifier: Modifier,
    queue: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    background: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    toolbar: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    pager: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    description: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    seekbar: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    primaryControlRow: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
    secondaryControlRow: @Composable BoxScope.(composition: RootPlaybackControlMainScope) -> Unit,
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
            Spacer(modifier = Modifier.height(15.dp))
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
                            val observer = composition.playbackController.createPlaybackObserver()
                            val queueCollector = observer.createQueueCollector()
                            try {
                                queueCollector
                                    .apply {
                                        startCollect().join()
                                    }
                                    .run {
                                        queueStateFlow.collect {
                                            composition.currentQueue = it
                                        }
                                    }
                            } finally {
                                queueCollector.dispose()
                                observer.dispose()
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
                                                .observeTrackSimpleMetadata(it)
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
                            composition.currentMetadataReaderCount++
                            try {
                                snapshotFlow { composition.currentPlaybackMetadata }
                                    .collect collectMetadata@ {
                                        latestTransformer?.cancel()
                                        if (it?.artwork == null) {
                                            composition.currentPlaybackBitmap = null
                                            return@collectMetadata
                                        }
                                        if (it.artwork is Bitmap) {
                                            composition.currentPlaybackBitmap = it.artwork
                                            return@collectMetadata
                                        }
                                        composition.currentPlaybackBitmap = null
                                        // else we need to collect the biggest layout viewport
                                        // within the composition and load the bitmap according to
                                        // the source kind
                                    }
                            } finally {
                                composition.currentMetadataReaderCount--
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
    composition: RootPlaybackControlMainScope
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
    composition: RootPlaybackControlMainScope
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

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class)
@Composable
private fun BoxScope.Pager(
    composition: RootPlaybackControlMainScope
) {
    // TODO: Tech Debt, should define the state
    val upComposition = rememberUpdatedState(newValue = composition)
    upComposition.value.apply {
        DisposableEffect(
            key1 = this,
            effect = {
                currentQueueReaderCount++
                onDispose { currentQueueReaderCount-- }
            }
        )
    }
    val queueOverrideAmountState = remember(composition) { mutableStateOf(0) }
    val queueOverrideState = remember(composition) { mutableStateOf<OldPlaybackQueue?>(null) }
    val maskedQueueState = remember(composition) {
        derivedStateOf { queueOverrideState.value ?: composition.currentQueue }
    }
    val queue = maskedQueueState.value
    val queueIndex = queue.currentIndex
    val queueList = queue.list

    remember(queueList.size, queueIndex) {
        if (queueList.isEmpty()) {
            require( queueIndex == PlaybackConstants.INDEX_UNSET) {
                "empty QueueList must be followed by an invalid Index, queue=$queueList index=$queueIndex"
            }
            null
        } else {
            requireNotNull(queueList.getOrNull(queueIndex)) {
                "non-empty QueueList must be followed by a valid index"
            }
        }
    } ?: return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        val pagerState = rememberPagerState(maskedQueueState.value.currentIndex.clampPositive())
        val basePagerFling = PagerDefaults.flingBehavior(state = pagerState)
        val rememberUpdatedConstraintState = rememberUpdatedState(newValue = constraints)

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            count = queueList.size,
            flingBehavior = remember {
                object : FlingBehavior {
                    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                        val constraint = rememberUpdatedConstraintState.value.maxWidth.toFloat() * 0.8F / 0.5f
                        val coerced = (initialVelocity).coerceIn(-abs(constraint)..abs(constraint))
                        return with(basePagerFling) { performFling(coerced) }.also {
                            Timber.d("FullPlaybackControl, fling vel=$initialVelocity, coerced=$coerced, left=$it")
                        }
                    }
                }
            },
            key = { queueList[it] }
        ) {
            val id = maskedQueueState.value.list.getOrNull(it)
            val state =
                if (id == null) {
                    remember {
                        mutableStateOf(PlaybackControlTrackMetadata())
                    }
                } else {
                    composition.observeTrackSimpleMetadata(id).collectAsState(PlaybackControlTrackMetadata())
                }
            TracksPagerItem(metadataState = state)
        }

        val touchedState = remember { mutableStateOf(false) }

        PagerListenMediaIndexChange(
            indexState = remember(composition) {
                derivedStateOf { composition.currentQueue.currentIndex }
            },
            pagerState = pagerState,
            overrideState = remember(composition) {
                derivedStateOf { queueOverrideState.value != null }
            },
            onScroll = { touchedState.value = false }
        )

        // Should consider to Just be be either `seekPrevious / seekNext`

        PagerListenUserDrag(
            pagerState = pagerState,
            onStartDrag = { touchedState.value = true },
            onEndDrag = { touchedState.value = true }
        )

        PagerListenPageState(
            composition = composition,
            pagerState = pagerState,
            queueState = maskedQueueState,
            touchedState = touchedState,
            shouldSeekIndex = { c, index ->
                if (c != upComposition.value) return@PagerListenPageState false
                val currentQ = maskedQueueState.value
                queueOverrideState.value = currentQ.copy(currentIndex = index)
                true
            },
            seekIndex = { c, index ->
                if (c != upComposition.value) {
                    return@PagerListenPageState false
                }
                queueOverrideAmountState.value++
                val seek = runCatching {
                    val q = composition.currentQueue
                    val result: PlaybackController.RequestResult = when (index) {
                        q.currentIndex + 1 -> {
                            c.playbackController.requestSeekNextAsync(Duration.ZERO).await()
                        }
                        q.currentIndex - 1 -> {
                            c.playbackController.requestSeekPreviousItemAsync(Duration.ZERO).await()
                        }
                        else -> return@runCatching false
                    }
                    result.eventDispatch?.join()
                    result.success
                }.onFailure {
                    if (it !is CancellationException) throw it
                }
                if (--queueOverrideAmountState.value == 0) {
                    queueOverrideState.value = null
                }
                (seek.getOrElse { false })
                    .also {
                        if (!it && queueOverrideAmountState.value == 0) {
                            touchedState.value = false
                            pagerState.scrollToPage(maskedQueueState.value.currentIndex)
                        }
                        Timber.d("PagerListenPageChange seek to $index done, " +
                                "$it ${queueOverrideAmountState.value}, ${queueOverrideState.value}")
                    }
            }
        )
    }
}

@Composable
private fun TracksPagerItem(
    metadataState: State<PlaybackControlTrackMetadata>
) {
    val art = metadataState.value.artwork
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val context = LocalContext.current
        val req = remember(art) {
            ImageRequest.Builder(context)
                .data(art)
                .build()
        }
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(280.dp)
                .align(Alignment.Center),
            model = req,
            contentDescription = "art",
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerListenMediaIndexChange(
    indexState: State<Int>,
    pagerState: PagerState,
    overrideState: State<Boolean>,
    onScroll: suspend () -> Unit
) {
    val currentIndex = indexState.value
    val currentOverride = overrideState.value
    LaunchedEffect(
        key1 = currentIndex,
        key2 = currentOverride
    ) {
        if (currentOverride) return@LaunchedEffect
        // TODO: Velocity check, if the user is dragging the pager but aren't moving
        // or if the user drag velocity will ends in another page
        if (currentIndex >= 0 &&
            pagerState.currentPage != currentIndex &&
            !pagerState.isScrollInProgress
        ) {
            onScroll()
            if (pagerState.currentPage in currentIndex - 2 .. currentIndex + 2) {
                pagerState.animateScrollToPage(currentIndex)
            } else {
                pagerState.scrollToPage(currentIndex)
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerListenUserDrag(
    pagerState: PagerState,
    onStartDrag: () -> Unit,
    onEndDrag: () -> Unit
) {
    LaunchedEffect(
        null
    ) {
        var wasDragging = false
        val stack = mutableListOf<DragInteraction.Start>()
        pagerState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> stack.add(interaction)
                is DragInteraction.Stop -> stack.remove(interaction.start)
                is DragInteraction.Cancel -> stack.remove(interaction.start)
            }
            if (stack.isNotEmpty()) {
                if (!wasDragging) {
                    wasDragging = true
                    onStartDrag()
                }
            } else {
                if (wasDragging) {
                    wasDragging = false
                    onEndDrag()
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerListenPageState(
    composition: RootPlaybackControlMainScope,
    pagerState: PagerState,
    queueState: State<OldPlaybackQueue>,
    touchedState: State<Boolean>,
    shouldSeekIndex: (RootPlaybackControlMainScope, Int) -> Boolean,
    seekIndex: suspend (RootPlaybackControlMainScope, Int) -> Boolean
) {
    val upComposition = rememberUpdatedState(newValue = composition)
    val dragging by pagerState.interactionSource.collectIsDraggedAsState()
    val touched by touchedState
    val page = pagerState.currentPage
    val rememberedPageState = remember {
        mutableStateOf(page)
    }
    LaunchedEffect(
        page,
        dragging,
        touched,
        block = {
            if (touched && !dragging &&
                (page != rememberedPageState.value
                        || rememberedPageState.value != queueState.value.currentIndex)
            ) {
                if (shouldSeekIndex(upComposition.value, page)) {
                    rememberedPageState.value = page
                    seekIndex(upComposition.value, page)
                }
            }
        }
    )
}

@Composable
private fun BoxScope.Description(
    composition: RootPlaybackControlMainScope
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val currentMetadata by remember(composition) {
                derivedStateOf { composition.currentPlaybackMetadata }
            }.apply {
                DisposableEffect(key1 = composition, effect = {
                    composition.currentMetadataReaderCount++
                    onDispose { composition.currentMetadataReaderCount-- }
                })
            }
            PlaybackDescriptionTitle(currentMetadata?.title ?: "")
            PlaybackDescriptionSubtitle(currentMetadata?.subtitle ?: "")
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
private fun BoxScope.Seekbar(
    composition: RootPlaybackControlMainScope
) {
    // TODO: Tech Debt, should define the state
    val upComposition = rememberUpdatedState(newValue = composition)

    BoxWithConstraints {
        val coroutineScope = rememberCoroutineScope()

        val sliderPositionCollectorReady = remember {
            mutableStateOf(false)
        }
        val sliderTextPositionCollectorReady = remember {
            mutableStateOf(false)
        }
        val sliderDurationCollectorReady = remember {
            mutableStateOf(false)
        }
        val sliderReady = remember {
            mutableStateOf(false)
        }
        val sliderTextReady = remember {
            mutableStateOf(false)
        }
        val sliderWidth = remember(maxWidth) {
            maxWidth * 0.85f
        }

        val playbackObserver = remember(composition) {
            composition.playbackController.createPlaybackObserver()
        }.apply {
            DisposableEffect(key1 = this, effect = {
                onDispose { dispose() }
            })
        }

        val sliderPositionCollector = remember(playbackObserver) {
            playbackObserver.createProgressionCollector(
                collectorContext = coroutineScope.coroutineContext,
                includeEvent = true,
            ).apply {
                coroutineScope.launch {
                    startCollectPosition().join()
                    sliderPositionCollectorReady.value = true
                }
                setIntervalHandler { _, progress, duration, speed ->
                    if (progress == Duration.ZERO || duration == Duration.ZERO || speed == 0f) {
                        null
                    } else {
                        (duration.inWholeMilliseconds / sliderWidth.value / speed).toLong()
                            .takeIf { it > 100 }?.milliseconds
                            ?: PlaybackConstants.DURATION_UNSET
                    }.also {
                        Timber.d("Playback_Slider_Debug: intervalHandler($it) param: $progress $duration $speed")
                    }
                }
            }
        }.apply {
            DisposableEffect(key1 = this, effect = {
                onDispose { dispose() }
            })
        }

        val sliderTextPositionCollector = remember(playbackObserver) {
            playbackObserver.createProgressionCollector(
                collectorContext = coroutineScope.coroutineContext,
                includeEvent = true,
            ).apply {
                coroutineScope.launch {
                    startCollectPosition().join()
                    sliderTextPositionCollectorReady.value = true
                }
            }
        }.apply {
            DisposableEffect(key1 = this, effect = {
                onDispose { dispose() }
            })
        }

        val durationCollector = remember(playbackObserver) {
            playbackObserver.createDurationCollector(
                collectorContext = coroutineScope.coroutineContext
            ).apply {
                coroutineScope.launch {
                    startCollect().join()
                    sliderDurationCollectorReady.value = true
                }
            }
        }.apply {
            DisposableEffect(key1 = this, effect = {
                onDispose { dispose() }
            })
        }

        val ready = remember {
            derivedStateOf { sliderReady.value && sliderTextReady.value }
        }

        val alpha = if (ready.value) 1f else 0f

        if (sliderPositionCollectorReady.value && sliderDurationCollectorReady.value) {
            sliderReady.value = true
        }

        if (sliderTextPositionCollectorReady.value && sliderDurationCollectorReady.value) {
            sliderTextReady.value = true
        }

        if (!sliderReady.value || !sliderTextReady.value) {
            return@BoxWithConstraints
        }

        val sliderTextPositionState = sliderTextPositionCollector.positionStateFlow.collectAsState()
        val sliderPositionState = sliderPositionCollector.positionStateFlow.collectAsState()
        val durationState = durationCollector.durationStateFlow.collectAsState()
        val interactionSource = remember { MutableInteractionSource() }
        val draggedState = interactionSource.collectIsDraggedAsState()
        val pressedState = interactionSource.collectIsPressedAsState()

        val changedValueState = remember {
            // the Slider should constraint it to 0, consider change it to nullable Float
            mutableStateOf(-1f)
        }
        val seekRequestCountState = remember {
            mutableStateOf(0)
        }

        // consume the `animate` composable, it will recreate a new state so it will not animate
        // should the first visible progress be animated ?
        val consumeAnimationState = remember { mutableStateOf(1) }

        NoInlineBox(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val seekRequestCount = seekRequestCountState.value
                val consumeAnimationCount = consumeAnimationState.value

                require(seekRequestCount == 0 || consumeAnimationCount > 0) {
                    "seekRequest should be followed by consumeAnimation"
                }

                val position by sliderPositionState
                val duration by durationState

                val sliderValue = when {
                    draggedState.value or pressedState.value or (changedValueState.value >= 0) -> {
                        changedValueState.value
                    }
                    consumeAnimationState.value > 0 -> {
                        consumeAnimationState.value = 0
                        (position / duration).toFloat().clamp(0f, 1f)
                    }
                    else -> {
                        val clampedProgress = (position / duration).toFloat().clamp(0f, 1f)
                        // should snap
                        val tweenDuration = if (clampedProgress == 1f || clampedProgress == 0f) 0 else 100
                        animateFloatAsState(
                            targetValue = clampedProgress,
                            animationSpec = tween(tweenDuration)
                        ).value
                    }
                }

                Slider(
                    modifier = Modifier
                        .width(sliderWidth)
                        .height(16.dp),
                    value = sliderValue,
                    // unfortunately There's no guarantee these 2 will be called in order
                    onValueChange = { value ->
                        Timber.d("Slider_DEBUG: onValueChange: $value")
                        changedValueState.value = value
                    },
                    onValueChangeFinished = {
                        Timber.d("Slider_DEBUG: onValueChangeFinished: ${changedValueState.value}")
                        changedValueState.value.takeIf { it >= 0 }?.let {
                            seekRequestCountState.value++
                            consumeAnimationState.value++
                            coroutineScope.launch {
                                val seekTo = (duration.inWholeMilliseconds * it).toLong().milliseconds
                                composition.playbackController
                                    .requestSeekAsync(seekTo, coroutineContext)
                                    .await().eventDispatch?.join()
                                if (--seekRequestCountState.value == 0) {
                                    changedValueState.value = -1f
                                }
                            }
                            return@Slider
                        }
                        if (seekRequestCountState.value == 0) changedValueState.value = -1f
                    },
                    interactionSource = interactionSource,
                    trackHeight = 6.dp,
                    thumbSize = 12.dp,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Theme.surfaceContentColorAsState().value,
                        thumbColor = Theme.backgroundContentColorAsState().value
                    )
                )
                Row(
                    modifier = Modifier
                        .width(this@BoxWithConstraints.maxWidth * 0.85f - 14.dp)
                        .padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PlaybackPositionSliderText(
                        progressState = remember {
                            derivedStateOf {
                                val raw = sliderTextPositionState.value
                                if (changedValueState.value >= 0) {
                                    changedValueState.value
                                } else {
                                    raw.inWholeMilliseconds / durationState.value.inWholeMilliseconds.toFloat()
                                }
                            }
                        },
                        durationState = durationState
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.PlaybackPositionSliderText(
    progressState: State<Float>,
    durationState: State<Duration>
) {

    NoInline {
        val duration = durationState.value
        val progress = progressState.value
        val formattedProgress: String = remember(progress, duration) {
            val seconds =
                if (duration.isNegative()) 0
                else (duration.inWholeSeconds * progress).toLong()
            if (seconds > 3600) {
                String.format(
                    "%02d:%02d:%02d",
                    seconds / 3600,
                    seconds % 3600 / 60,
                    seconds % 60
                )
            } else {
                String.format(
                    "%02d:%02d",
                    seconds / 60,
                    seconds % 60
                )
            }
        }

        Text(
            text = formattedProgress,
            color = Theme.backgroundContentColorAsState().value,
            style = MaterialTheme.typography.bodySmall
        )
    }

    NoInline {
        val duration = durationState.value
        val formattedDuration: String = remember(duration) {
            val seconds =
                if (duration.isNegative() || duration.isInfinite()) 0
                else duration.inWholeSeconds
            if (seconds > 3600) {
                String.format(
                    "%02d:%02d:%02d",
                    seconds / 3600,
                    seconds % 3600 / 60,
                    seconds % 60
                )
            } else {
                String.format(
                    "%02d:%02d",
                    seconds / 60,
                    seconds % 60
                )
            }
        }
        Text(
            text = formattedDuration,
            color = Theme.backgroundContentColorAsState().value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BoxScope.Queue(
    parent: RootPlaybackControlMainScope
) {
    BoxWithConstraints {
        if (!parent.showPlaybackQueue && parent.queueComposition == null) {
            return@BoxWithConstraints
        }
        parent.queueComposition = remember(parent) {
            RootPlaybackControlQueueScope(parent)
        }.apply {
            parent.restoreQueueComposition(this)
            fullyVisibleHeightTarget = constraints.maxHeight
        }
        RootPlaybackControlQueueScreen(composition = parent.queueComposition!!)
    }
}

@Composable
private fun BoxScope.PrimaryControlRow(
    composition: RootPlaybackControlMainScope
) {
    // TODO: Tech Debt
    val controller = composition.playbackController
    val coroutineScope = rememberCoroutineScope()

    val rememberUpdatedController = rememberUpdatedState(newValue = controller)

    val propertiesState = remember {
        mutableStateOf(PlaybackConstants.PROPERTIES_UNSET)
    }

    DisposableEffect(key1 = controller, effect = {
        val supervisor = SupervisorJob()
        val observer = controller.createPlaybackObserver().createPropertiesCollector()
            .apply {
                coroutineScope.launch(supervisor) {
                    startCollect().join()
                    propertiesStateFlow.collect { propertiesState.value = it }
                }
            }
        onDispose {
            supervisor.cancel()
            observer.dispose()
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
            rememberUpdatedController.value.requestPlayAsync()
        },
        pause = {
            rememberUpdatedController.value.requestSetPlayWhenReadyAsync(false)
        },
        next = {
            rememberUpdatedController.value.requestSeekNextAsync(Duration.ZERO)
        },
        previous = {
            rememberUpdatedController.value.requestSeekPreviousAsync(Duration.ZERO)
        },
        enableRepeat = {
            rememberUpdatedController.value.requestSetRepeatModeAsync(RepeatMode.ONE)
        },
        enableRepeatAll = {
            rememberUpdatedController.value.requestSetRepeatModeAsync(RepeatMode.ALL)
        },
        disableRepeat = {
            rememberUpdatedController.value.requestSetRepeatModeAsync(RepeatMode.OFF)
        },
        enableShuffle = {
            rememberUpdatedController.value.requestSetShuffleModeAsync(ShuffleMode.ON)
        },
        disableShuffle = {
            rememberUpdatedController.value.requestSetShuffleModeAsync(ShuffleMode.OFF)
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
                painter = painterResource(id = R.drawable.ios_glyph_seek_previos_100),
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
    composition: RootPlaybackControlMainScope
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
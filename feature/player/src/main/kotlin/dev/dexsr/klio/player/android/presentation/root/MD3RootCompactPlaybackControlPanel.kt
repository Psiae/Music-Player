package dev.dexsr.klio.player.android.presentation.root

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.palette.graphics.Palette
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.R
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlCompact
import com.flammky.musicplayer.player.presentation.root.rememberRootPlaybackControlCompactState
import dev.dexsr.klio.base.compose.Stack
import dev.dexsr.klio.base.compose.consumeDownGesture
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.*
import dev.dexsr.klio.player.android.presentation.root.compact.FoundationDescriptionPagerState
import dev.dexsr.klio.player.android.presentation.root.compact.FoundationDescriptionPager
import dev.dexsr.klio.player.shared.LocalMediaArtwork
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.time.Duration

@Composable
fun MD3RootCompactPlaybackControlPanel(
    modifier: Modifier = Modifier,
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
) {
    // TODO: Impl
    if (false) {
        Box(modifier = Modifier.height(56.dp)) {
            OldRootPlaybackControlPanel(state = state, bottomSpacing)
        }
    } else {
        BoxWithConstraints(modifier.fillMaxWidth()) {
            TransitioningRootCompactPlaybackControlPanel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(maxWidth - (7.dp + 7.dp)),
                state = state,
                bottomSpacing =  bottomSpacing
            )
        }
    }
}

@Composable
fun TransitioningRootCompactPlaybackControlPanel(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
) {
    val offset = transitioningRootCompactPlaybackControlOffsetAsState(
        state = state,
        height = 65.dp,
        bottomSpacing = bottomSpacing
    ).value
    Stack(
        modifier
            .offset { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
            .consumeDownGesture()
    ) {
        SubcomposeLayout() { constraints ->
            val layoutHeight = 65.dp
                .roundToPx()
                .coerceIn(0, constraints.maxHeight)
            val layoutWidth = constraints.maxWidth
            val pcPadding = PaddingValues(7.dp)
            val ltr = LayoutDirection.Ltr

            val contentConstraints = constraints.copy(
                minHeight = 0,
                maxHeight = layoutHeight,
                minWidth = 0,
                maxWidth = layoutWidth
            )

            val surface = subcompose("Surface") {
                // temp
               RootCompactPlaybackControlSurface(
                   modifier = Modifier,
                   state = state
               )
            }.fastMap { it.measure(contentConstraints) }

            var progressBarHeight = 0
            val progressBar = subcompose("ProgressBar") {
                RootCompactPlaybackControlProgressBar(
                    modifier = Modifier
                        .sizeIn(maxHeight = 3.dp)
                        .fillMaxWidth(),
                    state = state
                )
            }.fastMap { measurable ->
                measurable
                    .measure(contentConstraints)
                    .also { if (it.height > progressBarHeight) progressBarHeight = it.height }
            }

            var artworkWidth = 0
            val artwork = subcompose("Artwork") {
                val layoutHeight = layoutHeight - progressBarHeight
                val size =
                    if (layoutWidth < layoutHeight) {
                        layoutWidth.toDp()
                            .minus(pcPadding.calculateLeftPadding(ltr))
                            .minus(pcPadding.calculateRightPadding(ltr))
                    } else {
                        layoutHeight.toDp()
                            .minus(pcPadding.calculateTopPadding())
                            .minus(pcPadding.calculateBottomPadding())
                    }.coerceAtLeast(0.dp)
                RootCompactPlaybackControlArtworkDisplay(
                    modifier = Modifier.size(size = size),
                    state = state
                )
            }.fastMap { measurable ->
                measurable
                    .measure(contentConstraints)
                    .also { if (it.width > artworkWidth) artworkWidth = it.width }
            }

            var controlWidth = 0
            var controlHeight = 0
            val control = subcompose("Control") {
                val layoutHeight = layoutHeight - progressBarHeight
                val maxHeight = layoutHeight.toDp()
                    .minus(pcPadding.calculateTopPadding())
                    .minus(pcPadding.calculateBottomPadding())
                val maxWidth = layoutWidth.toDp()
                    .minus(pcPadding.calculateLeftPadding(ltr))
                    .minus(artworkWidth.toDp())
                    .minus(7.dp).minus(7.dp)
                    .minus(pcPadding.calculateRightPadding(ltr))
                RootCompactPlaybackControlButtons(
                    modifier = Modifier
                        .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight),
                    state = state
                )
            }.fastMap { measurable ->
                measurable
                    .measure(contentConstraints)
                    .also { if (it.width > controlWidth) controlWidth = it.width }
                    .also { if (it.height > controlHeight) controlHeight = it.height }
            }

            var pagerWidth = 0
            val pager = subcompose("Pager") {
                val layoutHeight = layoutHeight - progressBarHeight
                val maxHeight = layoutHeight.toDp()
                    .minus(pcPadding.calculateTopPadding())
                    .minus(pcPadding.calculateBottomPadding())
                val maxWidth = layoutWidth.toDp()
                    .minus(pcPadding.calculateLeftPadding(ltr))
                    .minus(artworkWidth.toDp())
                    .minus(7.dp)
                    .minus(7.dp)
                    .minus(controlWidth.toDp())
                    .minus(pcPadding.calculateRightPadding(ltr))
                DescriptionPager(
                    modifier = Modifier
                        .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight),
                    state = state
                )
            }.fastMap { measurable ->
                measurable
                    .measure(contentConstraints)
                    .also { if (it.width > pagerWidth) pagerWidth = it.width }
            }

            layout(
                width = layoutWidth,
                height = layoutHeight
            ) {
                surface.fastForEach { it.place(0, 0) }
                run {
                    val height = progressBar.fastMaxBy { it.height }?.height ?: 0
                    val topOffset = layoutHeight - height
                    progressBar.fastForEach { it.place(0, topOffset) }
                }
                run {
                    val topOffset = pcPadding.calculateTopPadding().roundToPx()
                    val leftOffset = pcPadding.calculateLeftPadding(ltr).roundToPx()
                    artwork.fastForEach { it.place(x = leftOffset, y = topOffset) }
                }
                run {
                    val topOffset = pcPadding.calculateTopPadding().roundToPx()
                    val leftOffset = pcPadding.calculateLeftPadding(ltr).roundToPx() + artworkWidth + 7.dp.roundToPx()
                    pager.fastForEach { it.place(x = leftOffset, y = topOffset) }
                }
                run {
                    val topOffset = run {
                        layoutHeight
                            .minus(progressBarHeight)
                            .div(2f)
                            .minus(controlHeight.toFloat().div(2))
                            .roundToInt()
                    }
                    val leftOffset = layoutWidth - controlWidth - pcPadding.calculateRightPadding(ltr).roundToPx()
                    control.fastForEach { it.place(x = leftOffset, y = topOffset) }
                }
            }
        }
    }
}

@Composable
@Deprecated("")
private fun OldRootPlaybackControlPanel(
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
) {
    val userState = remember {
        mutableStateOf<User?>(null)
    }
    LaunchedEffect(key1 = Unit, block = {
        AuthService.get().observeCurrentUser().collect { userState.value = it }
    })
    userState.value?.let {  user ->
        RootPlaybackControlCompact(
            state = rememberRootPlaybackControlCompactState(
                user = user,
                onBaseClicked = state.onSurfaceClicked,
                onArtworkClicked = state.onArtClicked
            ).apply {
                this.bottomSpacing = bottomSpacing
            }.run {
                state.apply { this.heightFromAnchor = topPositionRelativeToAnchor }
                this
            }
        )
    }
}

@Composable
private fun transitioningRootCompactPlaybackControlOffsetAsState(
    state: RootCompactPlaybackControlPanelState,
    height: Dp,
    bottomSpacing: Dp
): State<DpOffset> {
    val bottomSpacingState = rememberUpdatedState(newValue = bottomSpacing)
    val heightState = rememberUpdatedState(newValue = height)
    val animatable = remember(state) {
        Animatable(
            initialValue = heightState.value,
            typeConverter = Dp.VectorConverter,
        )
    }
    DisposableEffect(
        key1 = state,
        key2 = animatable,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())

            coroutineScope.launch(Dispatchers.Main) {
                var task: Job? = null
                snapshotFlow { state.freeze }
                    .distinctUntilChanged()
                    .collect { freeze ->
                        task?.cancel()
                        if (freeze) return@collect
                        task = launch {
                            var animator: Job? = null
                            state.playbackController
                                .currentlyPlayingMediaIdAsFlow()
                                .distinctUntilChanged()
                                .collect { mediaID ->
                                    animator?.cancel()
                                    animator = launch {
                                        var animateToTarget: Job? = null
                                        if (mediaID == null) {
                                            snapshotFlow { heightState.value }
                                                .distinctUntilChanged()
                                                .collect { target ->
                                                    animateToTarget?.cancel()
                                                    animateToTarget = launch(AndroidUiDispatcher.Main) {
                                                        animatable.animateTo(target, animationSpec = tween(200))
                                                    }
                                                }
                                        } else {
                                            snapshotFlow { bottomSpacingState.value.unaryMinus() }
                                                .distinctUntilChanged()
                                                .collect { target ->
                                                    animateToTarget?.cancel()
                                                    animateToTarget = launch(AndroidUiDispatcher.Main) {
                                                        animatable.animateTo(target, animationSpec = tween(200))
                                                    }
                                                }
                                        }
                                    }
                                }
                        }
                    }
            }


            onDispose { coroutineScope.cancel() }
        }
    )
    return remember(state) {
        derivedStateOf { DpOffset(x = 0.dp, y = animatable.value) }
    }
}

@Composable
private fun RootCompactPlaybackControlArtworkDisplay(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState
) {
    val ctx = LocalContext.current
    val artState = currentlyPlayingArtworkAsState(state = state, resetOnChange = false)
    val model = remember(artState.value) {
        ImageRequest.Builder(ctx)
            .data(artState.value?.image?.value)
            .crossfade(true)
            .build()
    }
    val clickableModifier = state.onArtClicked
        ?.let { onArtClicked ->
            Modifier.clickable(enabled = !state.freeze) { if (!state.freeze) onArtClicked() }
        }
        ?: Modifier
    Image(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(5.dp))
            .background(Theme.surfaceVariantContentColorAsState().value)
            .then(clickableModifier),
        painter = rememberAsyncImagePainter(model = model),
        contentDescription = "Currently Playing Artwork",
        contentScale = if (artState.value?.allowTransform != true) {
            ContentScale.Fit
        } else {
            ContentScale.Crop
        }
    )
}

@Composable
private fun currentlyPlayingArtworkAsState(
    state: RootCompactPlaybackControlPanelState,
    resetOnChange: Boolean
): State<LocalMediaArtwork?> {
    val upResetOnChange = rememberUpdatedState(newValue = resetOnChange)
    val returns = remember(state) {
        mutableStateOf<LocalMediaArtwork?>(null)
    }
    DisposableEffect(
        key1 = state,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())

            coroutineScope.launch(Dispatchers.Main) {
                var task: Job? = null

                snapshotFlow { state.freeze }
                    .distinctUntilChanged()
                    .collect { freeze ->
                        task?.cancel()
                        if (freeze) return@collect
                        task = launch {
                            var artworkCollector: Job? = null
                            state.playbackController.currentlyPlayingMediaIdAsFlow()
                                .distinctUntilChanged()
                                .collect { mediaID ->
                                    artworkCollector?.cancel()
                                    if (upResetOnChange.value) {
                                        returns.value = null
                                    }
                                    if (mediaID == null) {
                                        returns.value = null
                                        return@collect
                                    }
                                    artworkCollector = launch {
                                        state.mediaMetadataProvider.artworkAsFlow(mediaID)
                                            .collect { image -> returns.value = image }
                                    }
                                }
                        }
                    }

            }

            onDispose { coroutineScope.cancel() ; returns.value = null  }
        }
    )
    return returns
}

@Composable
private fun RootCompactPlaybackControlProgressBar(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .composed {
                background(
                    MD3Theme.surfaceVariantColorAsState().value
                        .copy(alpha = 0.7f)
                        .compositeOver(MD3Theme.blackOrWhite())
                )
            }
    ) {

        val progress = observePlaybackProgressAsState(
            state = state,
            progressIndicatorLengthDp = maxWidth.value,
            bufferedProgressIndicatorLengthDp = maxWidth.value
        ).value

        LinearProgressIndicator(
            modifier = remember { Modifier.fillMaxSize() },
            color = MD3Theme.backgroundContentColorAsState().value.copy(alpha = 0.25f),
            backgroundColor = Color.Transparent,
            progress = animatePlaybackProgressAsState(
                key = state,
                position = progress.bufferedPosition,
                duration = progress.duration,
                animationSpec = tween(150)
            ).value
        )

        LinearProgressIndicator(
            modifier = remember { Modifier.fillMaxSize() },
            color = MD3Theme.backgroundContentColorAsState().value,
            backgroundColor = Color.Transparent,
            progress = animatePlaybackProgressAsState(
                key = state,
                position = progress.position,
                duration = progress.duration,
                animationSpec = tween(150)
            ).value
        )
    }
}

@Composable
private fun observePlaybackProgressAsState(
    state: RootCompactPlaybackControlPanelState,
    progressIndicatorLengthDp: Float,
    bufferedProgressIndicatorLengthDp: Float
): State<PlaybackProgress> {
    val returns = remember(state) {
        mutableStateOf<PlaybackProgress>(PlaybackProgress.UNSET)
    }

    val upWidth = rememberUpdatedState(
        newValue = minOf(
            progressIndicatorLengthDp,
            bufferedProgressIndicatorLengthDp
        )
    )

    DisposableEffect(
        key1 = state,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())

            var latestCollector: Job? = null
            coroutineScope.launch(Dispatchers.Main) {
                snapshotFlow { upWidth.value }
                    .collect { width ->
                        latestCollector?.cancel()
                        latestCollector = launch {
                            state.playbackController.playbackProgressAsFlow(width)
                                .collect { progress ->
                                   returns.value = progress
                                }
                        }
                    }
            }

            onDispose { coroutineScope.cancel() }
        }
    )

    return returns
}

@Composable
private fun animatePlaybackProgressAsState(
    key: Any,
    position: Duration,
    duration: Duration,
    animationSpec: AnimationSpec<Float>
): State<Float> {
    val animatable = remember(key) {
        Animatable(
            initialValue = 0f,
            visibilityThreshold = 0.01f,
        )
    }
    val animSpec: AnimationSpec<Float> by rememberUpdatedState(
        animationSpec.run {
            if (this is SpringSpec &&
                this.visibilityThreshold != 0.01f
            ) {
                spring(dampingRatio, stiffness, visibilityThreshold)
            } else {
                this
            }
        }
    )
    val channel = remember(key) { Channel<Float>(Channel.CONFLATED) }
    SideEffect {
        val positionMS = position.inWholeMilliseconds
        val durationMS = duration.inWholeMilliseconds
        val targetValue = when {
            positionMS <= 0 -> 0f
            durationMS <= 0 -> 0f
            positionMS > durationMS -> 1f
            else -> positionMS.toFloat() / durationMS
        }
        channel.trySend(targetValue)
    }
    LaunchedEffect(channel) {
        for (target in channel) {
            // This additional poll is needed because when the channel suspends on receive and
            // two values are produced before consumers' dispatcher resumes, only the first value
            // will be received.
            // It may not be an issue elsewhere, but in animation we want to avoid being one
            // frame late.
            val newTarget = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    animatable.animateTo(newTarget, animSpec)
                }
            }
        }
    }

    return remember(animatable) {
        derivedStateOf { animatable.value }
    }
}


@Composable
private fun RootCompactPlaybackControlButtons(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState
) {
    Stack(modifier) {
        SubcomposeLayout { constraints ->

            val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val spacerPx = 3.dp.roundToPx()

            var playPauseButtonWidth = 0
            var playPauseButtonHeight = 0
            val playPauseButton = subcompose("PlayPauseButton") {
                RootCompactPlaybackControlPlayPauseButton(modifier = Modifier, state = state)
            }.fastMap { measurable ->
                measurable.measure(contentConstraints)
                    .also {
                        if (it.width > playPauseButtonWidth) playPauseButtonWidth = it.width
                        if (it.height > playPauseButtonHeight) playPauseButtonHeight = it.height
                    }
            }

            var futureSlot2Width = 0
            var futureSlot2Height = 0
            val futureSlot2 = subcompose("FutureSlot2") {
                Box(modifier = Modifier.size(35.dp))
            }.fastMap { measurable ->
                measurable.measure(contentConstraints)
                    .also {
                        if (it.width > futureSlot2Width) futureSlot2Width = it.width
                        if (it.height > futureSlot2Height) futureSlot2Height = it.height
                    }
            }

            var futureSlot3Width = 0
            var futureSlot3Height = 0
            val futureSlot3 = subcompose("FutureSlot3") {
                Box(modifier = Modifier.size(35.dp))
            }.fastMap { measurable ->
                measurable.measure(contentConstraints)
                    .also {
                        if (it.width > futureSlot3Width) futureSlot3Width = it.width
                        if (it.height > futureSlot3Height) futureSlot3Height = it.height
                    }
            }

            layout(
                height = maxOf(playPauseButtonHeight, futureSlot2Height, futureSlot3Height),
                width = playPauseButtonWidth + (spacerPx) + futureSlot2Width +
                        (spacerPx) + futureSlot3Width
            ) {
                futureSlot3.fastForEach { it.place(0, 0) }
                futureSlot2.fastForEach { it.place(futureSlot3Width + spacerPx, 0) }
                playPauseButton.fastForEach { it.place(futureSlot3Width + spacerPx + futureSlot2Width + spacerPx, 0) }
            }
        }
    }
}

@Composable
private fun RootCompactPlaybackControlPlayPauseButton(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState
) {
    val progressionState by playbackProgressionStateAsSnapshotState(panelState = state)
    // IsPlaying callback from mediaController is somewhat not accurate
    val showPlay = !progressionState.playWhenReady
    val allowPlay = showPlay && progressionState.canPlay
    val icon =
        if (showPlay) {
            R.drawable.play_filled_round_corner_32
        } else {
            R.drawable.pause_filled_narrow_rounded_corner_32
        }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(35.dp)
            .clickable(
                enabled = !state.freeze && !showPlay || allowPlay,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            ) {
                if (state.freeze) {
                    return@clickable
                }
                if (showPlay) {
                    state.playbackController.play()
                } else {
                    state.playbackController.pause()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val pressed by interactionSource.collectIsPressedAsState()
        val size by animateDpAsState(targetValue =  if (pressed) 24.dp else 26.dp)
        Icon(
            modifier = Modifier
                .size(size)
            ,
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = remember(state.isSurfaceDark, showPlay, allowPlay) {
                if (!state.isSurfaceDark) {
                    Color(0xFF0F0F0F)
                } else {
                    Color(0xFFEBEBEB)
                }.let {
                    // TODO: define alpha spec
                    if (showPlay && !allowPlay) it.copy(alpha  = 0.38f) else it
                }
            }
        )
    }
}

@Composable
private fun playbackProgressionStateAsSnapshotState(
    panelState: RootCompactPlaybackControlPanelState
): State<PlaybackProgressionState> {
    val returns = remember(panelState) {
        mutableStateOf<PlaybackProgressionState>(PlaybackProgressionState.UNSET)
    }

    DisposableEffect(
        panelState,
        returns,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())

            coroutineScope.launch {

                var collector: Job? = null
                snapshotFlow { panelState.freeze }
                    .distinctUntilChanged()
                    .collect { freeze ->
                        collector?.cancel()
                        if (freeze) {
                            return@collect
                        }
                        collector = launch {
                            panelState.playbackController.playbackProgressionStateAsFlow()
                                .collect { progression ->
                                    returns.value = progression
                                }
                        }
                    }
            }

            onDispose { coroutineScope.cancel() }
        }
    )


    return returns
}

@Composable
private fun RootCompactPlaybackControlSurface(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState
) {
    val colorState = animatedCompositeSurfacePaletteColorAsState(
        dark = LocalIsThemeDark.current,
        compositeFactor = 0.78f,
        compositeOver = MD3Theme.surfaceVariantColorAsState().value,
        state = state
    )
    val clickableModifier = state.onSurfaceClicked
        ?.let { onSurfaceClicked ->
            Modifier.clickable(enabled = !state.freeze) { if (!state.freeze) onSurfaceClicked() }
        }
        ?: Modifier
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorState.value)
            .then(clickableModifier)
    )
    SideEffect {
        state.isSurfaceDark = colorState.value.luminance() < (155f / 255f)
    }
}

@Composable
private fun animatedCompositeSurfacePaletteColorAsState(
    dark: Boolean,
    compositeFactor: Float,
    compositeOver: Color,
    state: RootCompactPlaybackControlPanelState
): State<Color> {
    val paletteColor = surfacePaletteColor(dark = dark, state)
    return animateColorAsState(
        targetValue = remember(paletteColor, compositeFactor, compositeOver) {
            paletteColor.takeIf { it != Color.Unspecified }
                ?.copy(compositeFactor)
                ?.compositeOver(compositeOver)
                ?: compositeOver
        },
        animationSpec = tween(150)
    )
}

@Composable
private fun surfacePaletteColor(
    dark: Boolean,
    state: RootCompactPlaybackControlPanelState
): Color {
    val paletteColorState = remember(state) {
        mutableStateOf(Color.Unspecified)
    }
    val upDark = rememberUpdatedState(newValue = dark)
    DisposableEffect(
        state,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())

            coroutineScope.launch {
                var mediaIdCollector: Job? = null

                snapshotFlow { state.freeze }
                    .distinctUntilChanged()
                    .collect { freeze ->
                        mediaIdCollector?.cancel()
                        if (freeze) return@collect
                        mediaIdCollector = launch {
                            var artCollector: Job? = null
                            state.playbackController.currentlyPlayingMediaIdAsFlow()
                                .distinctUntilChanged()
                                .collect { id ->
                                    artCollector?.cancel()
                                    if (id == null) return@collect
                                    artCollector = launch {
                                        var paletteWorker: Job? = null
                                        state.mediaMetadataProvider.artworkAsFlow(id)
                                            .collect { art ->
                                                paletteWorker?.cancel()
                                                val rawArt = art?.image?.value
                                                if (rawArt !is Bitmap) {
                                                    // TODO: transform
                                                    paletteColorState.value = Color.Unspecified
                                                    return@collect
                                                }
                                                paletteWorker = launch {
                                                    val gen = suspendCancellableCoroutine<Palette?> { cont ->
                                                        Palette.from(rawArt).maximumColorCount(16).generate() { palette ->
                                                            cont.resume(palette)
                                                        }
                                                    }
                                                    if (gen == null) {
                                                        paletteColorState.value = Color.Unspecified
                                                        return@launch
                                                    }
                                                    snapshotFlow { upDark.value }
                                                        .collect { dark ->
                                                            val int = gen.run {
                                                                if (dark) {
                                                                    getDarkVibrantColor(getMutedColor(getVibrantColor(getLightVibrantColor(getDominantColor(-1)))))
                                                                } else {
                                                                    getLightVibrantColor(getVibrantColor(getDarkVibrantColor(getDominantColor(-1))))
                                                                }
                                                            }
                                                            paletteColorState.value = if (int == -1) Color.Unspecified else Color(int)
                                                        }
                                                }
                                            }
                                    }
                                }
                        }
                    }
            }

            onDispose { coroutineScope.cancel() }
        }
    )

    return paletteColorState.value
}


@Composable
private fun DescriptionPager(
    modifier: Modifier,
    state: RootCompactPlaybackControlPanelState
) {
    FoundationDescriptionPager(
        modifier = modifier,
        state = remember(state) {
            FoundationDescriptionPagerState(state)
        }
    )
}
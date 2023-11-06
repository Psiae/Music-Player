package dev.dexsr.klio.player.presentation.root

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlCompact
import com.flammky.musicplayer.player.presentation.root.rememberRootPlaybackControlCompactState
import dev.dexsr.klio.base.compose.Stack
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.blackOrWhite
import dev.dexsr.klio.base.theme.md3.compose.surfaceVariantColorAsState
import dev.dexsr.klio.player.presentation.LocalMediaArtwork
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import kotlin.time.Duration

@Composable
fun MD3RootCompactPlaybackControlPanel(
    modifier: Modifier = Modifier,
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
) {
    // TODO: Impl
    if (true) {
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
        height = 56.dp,
        bottomSpacing = bottomSpacing
    ).value
    Stack(modifier.offset { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }) {
        SubcomposeLayout() { constraints ->
            val layoutHeight = 56.dp
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Green)
                )
            }.fastMap { it.measure(contentConstraints) }

            var progressBarHeight = 0
            val progressBar = subcompose("ProgressBar") {
                Box(
                    modifier = Modifier
                        .sizeIn(maxHeight = 3.dp)
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(Color.Magenta)
                )
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
                    state= state
                )
            }.fastMap { measurable ->
                measurable
                    .measure(contentConstraints)
                    .also { if (it.width > artworkWidth) artworkWidth = it.width }
            }

            var controlWidth = 0
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
                // temp
                Box(
                    modifier = Modifier
                        .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
                        .fillMaxHeight()
                        .width(((24 * 3) + (7 * (3 - 1))).dp)
                        .background(Color.Blue)
                )
            }.fastMap { measurable ->
                measurable
                    .measure(contentConstraints)
                    .also { if (it.width > controlWidth) controlWidth = it.width }
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
                Box(
                    modifier = Modifier
                        .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(Color.Red)
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
                    val topOffset = pcPadding.calculateTopPadding().roundToPx()
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
        Timber.d("DEBUG: position=$position, duration=$duration, targetValue=$targetValue")
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
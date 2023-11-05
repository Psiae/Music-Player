package dev.dexsr.klio.player.presentation.root

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlCompact
import com.flammky.musicplayer.player.presentation.root.rememberRootPlaybackControlCompactState
import dev.dexsr.klio.base.compose.Stack
import kotlinx.coroutines.delay

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
    val offset = transitioningRootCompactPlaybackControlOffset(
        state = state,
        bottomSpacing
    )
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
                    modifier = Modifier.size(size = size)
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
private fun RootCompactPlaybackControlArtworkDisplay(
    modifier: Modifier
) {
    // temp
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Yellow)
    )
}

@Composable
private fun transitioningRootCompactPlaybackControlOffset(
    state: RootCompactPlaybackControlPanelState,
    bottomSpacing: Dp
): DpOffset {
    // TOO
    val dpOffset = remember {
        Animatable(
            initialValue = 56.dp,
            typeConverter = Dp.VectorConverter,
        )
    }.apply {
        LaunchedEffect(
            key1 = this,
            block = {
                delay(2000)
                animateTo(-bottomSpacing, tween(500))
            }
        )
    }
    return remember(dpOffset.value) { DpOffset(0.dp, dpOffset.value) }
}
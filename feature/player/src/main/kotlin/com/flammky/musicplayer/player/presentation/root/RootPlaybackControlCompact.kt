package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import com.flammky.musicplayer.player.presentation.root.CompactControlBackgroundState.Applier.Companion.PrepareCompositionInline
import com.flammky.musicplayer.player.presentation.root.CompactControlTransitionState.Applier.Companion.PrepareComposition
import com.flammky.musicplayer.player.presentation.root.CompactControlTransitionState.Companion.getLayoutHeight
import com.flammky.musicplayer.player.presentation.root.CompactControlTransitionState.Companion.getLayoutOffset
import com.flammky.musicplayer.player.presentation.root.CompactControlTransitionState.Companion.getLayoutWidth
import com.flammky.musicplayer.player.presentation.root.ControlCompactCoordinator.Companion.PrepareCompositionInline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Composable
fun rememberRootPlaybackControlCompactState(
    user: User,
    onBaseClicked: (() -> Unit)?,
    onArtworkClicked: (() -> Unit)?
): RootPlaybackControlCompactState {
    val coroutineScope = rememberCoroutineScope()
    val viewModel = hiltViewModel<PlaybackControlViewModel>()
    val state = remember(user, viewModel) {
        RootPlaybackControlCompactState(
            playbackController = viewModel.createUserPlaybackController(user),
            onObserveArtwork = viewModel::observeMediaArtwork,
            onObserveMetadata = viewModel::observeMediaMetadata,
            coroutineScope = coroutineScope,
            coroutineDispatchScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        )
    }.apply {
        artworkClickedHandle = onArtworkClicked
        baseClickedHandle = onBaseClicked
    }

    DisposableEffect(
        key1 = state,
        effect = {
            onDispose { state.dispose() }
        }
    )

    return state
}

@Composable
fun RootPlaybackControlCompact(
    state: RootPlaybackControlCompactState
) {
    val coordinator = state.coordinator
        .apply {
            PrepareCompositionInline()
        }
    TransitioningContentLayout(coordinator.layoutComposition)
}

@Composable
private fun TransitioningContentLayout(
    composition: ControlCompactComposition
) {
    val state = composition.transitionState
        .apply {
            applier.PrepareComposition()
        }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val localDensity = LocalDensity.current
        val offset = state.getLayoutOffset(constraints = constraints, density = localDensity)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(state.getLayoutHeight(constraints = constraints))
                .width(state.getLayoutWidth(constraints = constraints))
                .offset(offset.x, offset.y)
                .onGloballyPositioned { lc ->
                    with(localDensity) {
                        val posInParent = lc.positionInParent()
                        composition.topPosition =
                            posInParent.y.toDp()
                        composition.topPositionFromAnchor =
                            (lc.parentLayoutCoordinates!!.size.height - posInParent.y).toDp()
                    }
                }
        ) {
            ContentLayout(composition = composition)
        }
    }
}

@Composable
private fun ContentLayout(
    composition: ControlCompactComposition
) {
    Card(shape = RoundedCornerShape(10)) {
        Box(
            Modifier.fillMaxSize()
                .run {
                    with(composition.backgroundState) {
                        // TODO: change to ComposeLayout
                        applier.PrepareCompositionInline()
                        backgroundModifier().interactionModifier()
                    }
                }
        ) {
            // I have no Idea why clickable here is not working
            // when interaction is passed through the pager
            Column {
                Row(
                    modifier = Modifier
                        .padding(7.dp)
                        .weight(1f)
                ) {
                    ArtworkDisplay(state = composition.artworkDisplayState)
                    Spacer(modifier = Modifier.width(7.dp))
                    PagerControl(state = composition.pagerState)
                    Spacer(modifier = Modifier.width(7.dp))
                    ButtonControls(state = composition.controlsState)
                }
                TimeBar(state = composition.timeBarState)
            }
        }
    }
}

@Composable
private inline fun RowScope.PagerControl(
    state: CompactControlPagerState
) = Box(modifier = Modifier.weight(1f)) { CompactControlPager(state = state) }

@Composable
private inline fun ArtworkDisplay(
    state: CompactControlArtworkState
) = Box(modifier = Modifier) { CompactControlArtwork(state = state) }

@Composable
private inline fun ButtonControls(
    state: CompactButtonControlsState
) = Box(modifier = Modifier) { CompactControlButtons(state = state) }

@Composable
private inline fun ColumnScope.TimeBar(
    state: CompactControlTimeBarState
) = Box(modifier = Modifier) { CompactControlTimeBar(state = state) }
package dev.dexsr.klio.player.android.presentation.root.main

import androidx.collection.LruCache
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import dev.dexsr.klio.player.android.presentation.root.MediaMetadataProvider
import dev.dexsr.klio.player.android.presentation.root.main.pager.SnapLayoutInfoProvider
import dev.dexsr.klio.player.android.presentation.root.main.pager.dragDirectionDetector
import dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll.*

@Composable
fun PlaybackPagerDef(
    modifier: Modifier,
    state: LazyPlaybackPagerState,
    mediaMetadataProvider: MediaMetadataProvider,
    itemPadding: PaddingValues
) {
    val upItemPadding = rememberUpdatedState(newValue = itemPadding)
    val upState = rememberUpdatedState(newValue = state)
    val upMediaMetadataProvider = rememberUpdatedState(newValue = mediaMetadataProvider)
    val cache = remember {
        LruCache<String, Any>(1 + PlaybackPagerEagerRangePlaceholder * 2)
    }
    PlaybackPager(
        modifier = modifier.fillMaxSize(),
        state = state,
        isVertical = false,
        eagerRange = PlaybackPagerEagerRangePlaceholder,
        itemLayout = remember {
            {
                PlaybackPagerItemLayout(
                    modifier = Modifier,
                    mediaMetadataProvider = upMediaMetadataProvider.value,
                    contentPadding = upItemPadding.value,
                    scope = this,
                    cache = cache
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackPager(
    modifier: Modifier,
    state: LazyPlaybackPagerState,
    isVertical: Boolean,
    eagerRange: Int,
    itemLayout: @Composable PlaybackPagerItemScope.() -> Unit
) {
    check(eagerRange == PlaybackPagerEagerRangePlaceholder) {
        "eagerRange is temporarily statically set to PlaybackPagerEagerRangePlaceholder=$PlaybackPagerEagerRangePlaceholder, dynamic change is not yet implemented"
    }
    val latestContent = rememberUpdatedState(itemLayout)
    val controller = state.controller
    val itemLayoutProvider = remember(state) {
        PlaybackPagerItemLayoutProvider(
            state = state,
            itemContent = { latestContent.value },
            itemCount = { controller.renderData?.timeline?.windows?.size ?: 0 },
            getMediaID = { i -> controller.renderData!!.timeline.windows[i] }
        )
    }
    val measurePolicy = remember(
        state,
        isVertical,
        eagerRange,
        itemLayoutProvider
    ) {
        @Suppress("SuspiciousCallableReferenceInLambda")
        PlaybackPagerMeasurePolicy(
            isVertical = isVertical,
            eagerRange = eagerRange,
            itemProvider = itemLayoutProvider,
            controller = controller,
            horizontalAlignment = if (isVertical) Alignment.CenterHorizontally else null,
            verticalAlignment = if (isVertical) null else Alignment.CenterVertically
        )::subcomposeMeasurePolicy
    }
    val orientation = if (isVertical) {
        Orientation.Vertical
    } else {
        Orientation.Horizontal
    }
    val patchedOverscrollEffect = rememberPatchedOverscrollEffect()
    val playbackPagerOverscrollEffect = rememberPlaybackPagerOverscrollEffect()

    val density = LocalDensity.current
    val flingBehavior = remember(controller, density) {
        val highVelocityAnimationSpec = SplineBasedFloatDecayAnimationSpec(density)
            .generateDecayAnimationSpec<Float>()
        val lowVelocityAnimationSpec = TweenSpec<Float>(
            500,
            0,
            LinearEasing
        )
        val snapAnimationSpec = SpringSpec<Float>(
            Spring.DampingRatioNoBouncy,
            Spring.StiffnessMediumLow,
            null
        )
        SnapFlingBehavior(
            snapLayoutInfoProvider = SnapLayoutInfoProvider(
                pagerState = controller,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                decayAnimationSpec = highVelocityAnimationSpec,
                snapPositionalThreshold = 0.5f
            ),
            lowVelocityAnimationSpec = lowVelocityAnimationSpec,
            highVelocityAnimationSpec = highVelocityAnimationSpec,
            snapAnimationSpec = snapAnimationSpec,
            density = density,
            shortSnapVelocityThreshold = 400.dp
        )
    }

    SubcomposeLayout(
        modifier = modifier
            .then(controller.awaitLayoutModifier)
            .then(controller.remeasurementModifier)
            .playbackPagerScrollable(
                state = controller.gestureScrollableState,
                orientation = orientation,
                overscrollEffect = playbackPagerOverscrollEffect
            )
            .scrollable(
                orientation = orientation,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    orientation,
                    reverseScrolling = false
                ),
                interactionSource = controller.interactionSource,
                flingBehavior = flingBehavior,
                state = controller.gestureScrollableState1,
                overscrollEffect = remember(patchedOverscrollEffect) { patchedOverscrollEffect.asComposeFoundationOverscrollEffect() },
                enabled = true
            )
            .playbackPagerOverscroll(playbackPagerOverscrollEffect)
            .overscroll(patchedOverscrollEffect)
            .clipScrollableContainer(orientation)
            .dragDirectionDetector(controller),
        state = remember(eagerRange) {
            // TODO
            SubcomposeLayoutState(slotReusePolicy = SubcomposeSlotReusePolicy(0))
        },
        measurePolicy = measurePolicy
    )

    DisposableEffect(
        controller,
        effect =  {
            controller.init()
            onDispose { controller.dispose() }
        }
    )
}


class LazyPlaybackPagerState(
    val player: PlaybackPagerPlayer,
) {

    val controller = PlaybackPagerController(
        player
    )

    val interactionSource = MutableInteractionSource()
}


class PlaybackPagerRenderData(
    val timeline: PlaybackPagerTimeline,
    val head: PlaybackPagerTimeline? = null,
    val headCutoffIndex: Int? = null,
    val tail: PlaybackPagerTimeline? = null,
    val tailCutoffIndex: Int? = null,
)

class PlaybackPagerItemLayoutProvider(
    private val state: LazyPlaybackPagerState,
    private val itemContent: () -> @Composable PlaybackPagerItemScope.() -> Unit,
    private val itemCount: () -> Int,
    private val getMediaID: (Int) -> String,
) {

    private val contents = derivedStateOf {
        val count = itemCount()
        ArrayList<ItemLayoutData>(count)
            .apply { repeat(count) {
                add(ItemLayoutData(itemContent.invoke(), getMediaID(it), it))
            } }
    }

    fun getContent(index: Int): @Composable () -> Unit {
        val data = contents.value[index]
        return with(data) {
            @Composable {
                object : PlaybackPagerItemScope {
                    override val mediaID: String = data.mediaID
                    override val page: Int = data.page
                }.content()
            }
        }
    }

    private class ItemLayoutData(
        val content: @Composable PlaybackPagerItemScope.() -> Unit,
        val mediaID: String,
        val page: Int
    )
}





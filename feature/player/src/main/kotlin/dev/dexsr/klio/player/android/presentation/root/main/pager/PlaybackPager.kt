package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.collection.LruCache
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import dev.dexsr.klio.player.android.presentation.root.main.MediaMetadataProvider
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
    val playbackPagerOverscrollEffect = rememberPlaybackPagerOverscrollEffect()

    val density = LocalDensity.current

    SubcomposeLayout(
        modifier = modifier
            .then(controller.modifier)
            .playbackPagerScrollable(
                state = controller.gestureScrollableState,
                orientation = orientation,
                overscrollEffect = playbackPagerOverscrollEffect
            )
            .playbackPagerOverscroll(playbackPagerOverscrollEffect)
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





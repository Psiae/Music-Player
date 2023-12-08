package dev.dexsr.klio.player.android.presentation.root.main.pager.overscroll

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity

/**
 * @see [androidx.compose.foundation.OverscrollEffect]
 * */
interface PlaybackPagerOverscrollEffect {

    /**
     * Applies overscroll to [performScroll]. [performScroll] should represent a drag / scroll, and
     * returns the amount of delta consumed, so in simple cases the amount of overscroll to show
     * should be equal to `delta - performScroll(delta)`. The OverscrollEffect can optionally
     * consume some delta before calling [performScroll], such as to release any existing tension.
     * The implementation *must* call [performScroll] exactly once. This function should return the
     * sum of all the delta that was consumed during this operation - both by the overscroll and
     * [performScroll].
     *
     * For example, assume we want to apply overscroll to a custom component that isn't using
     * [androidx.compose.foundation.gestures.scrollable]. Here is a simple example of a component
     * using [androidx.compose.foundation.gestures.draggable] instead:
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_Before
     *
     * To apply overscroll, we need to decorate the existing logic with applyToScroll, and
     * return the amount of delta we have consumed when updating the drag position. Note that we
     * also need to call applyToFling - this is used as an end signal for overscroll so that effects
     * can correctly reset after any animations, when the gesture has stopped.
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_After
     *
     * @param delta total scroll delta available
     * @param source the source of the delta
     * @param performScroll the scroll action that the overscroll is applied to. The [Offset]
     * parameter represents how much delta is available, and the return value is how much delta was
     * consumed. Any delta that was not consumed should be used to show the overscroll effect.
     * @return the delta consumed from [delta] by the operation of this function - including that
     * consumed by [performScroll].
     */
    fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset

    /**
     * Applies overscroll to [performFling]. [performFling] should represent a fling (the release
     * of a drag or scroll), and returns the amount of [Velocity] consumed, so in simple cases the
     * amount of overscroll to show should be equal to `velocity - performFling(velocity)`. The
     * OverscrollEffect can optionally consume some [Velocity] before calling [performFling], such
     * as to release any existing tension. The implementation *must* call [performFling] exactly
     * once.
     *
     * For example, assume we want to apply overscroll to a custom component that isn't using
     * [androidx.compose.foundation.gestures.scrollable]. Here is a simple example of a component
     * using [androidx.compose.foundation.gestures.draggable] instead:
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_Before
     *
     * To apply overscroll, we decorate the existing logic with applyToScroll, and return the amount
     * of delta we have consumed when updating the drag position. We then call applyToFling using
     * the velocity provided by onDragStopped.
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_After
     *
     * @param velocity total [Velocity] available
     * @param performFling the [Velocity] consuming lambda that the overscroll is applied to. The
     * [Velocity] parameter represents how much [Velocity] is available, and the return value is how
     * much [Velocity] was consumed. Any [Velocity] that was not consumed should be used to show the
     * overscroll effect.
     */
    suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity)

    /**
     * Whether this OverscrollEffect is currently displaying overscroll.
     *
     * @return true if this OverscrollEffect is currently displaying overscroll
     */
    val isInProgress: Boolean

    /**
     * A [Modifier] that will draw this OverscrollEffect
     */
    val effectModifier: Modifier
}

@OptIn(ExperimentalFoundationApi::class)
fun PlaybackPagerOverscrollEffect(
    context: Context,
    config: OverscrollConfiguration
): PlaybackPagerOverscrollEffect {

    return PlaybackPagerOverscrollEffectImpl(context, config)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPlaybackPagerOverscrollEffect(

): PlaybackPagerOverscrollEffect {
    val ctx = LocalContext.current
    val config = LocalOverscrollConfiguration.current
    return remember(ctx, config) {
        if (config != null) {
            PlaybackPagerOverscrollEffect(ctx, config)
        } else {
            NoOpPlaybackPagerOverscrollEffect
        }
    }
}

/**
 * @see [androidx.compose.foundation.overscroll]
 * */
@Composable
fun Modifier.playbackPagerOverscroll(
    overscrollEffect: PlaybackPagerOverscrollEffect
) = this.then(overscrollEffect.effectModifier)


@OptIn(ExperimentalFoundationApi::class)
private class PlaybackPagerOverscrollEffectImpl(
    context: Context,
    config: OverscrollConfiguration
) : PlaybackPagerOverscrollEffect /* TODO: impl */ by NoOpPlaybackPagerOverscrollEffect {

    private val platformEdgeEffect = EdgeEffectCompat
}

private object NoOpPlaybackPagerOverscrollEffect : PlaybackPagerOverscrollEffect {

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset = performScroll(delta)

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) { performFling(velocity) }

    override val isInProgress: Boolean
        get() = false

    override val effectModifier: Modifier
        get() = Modifier
}
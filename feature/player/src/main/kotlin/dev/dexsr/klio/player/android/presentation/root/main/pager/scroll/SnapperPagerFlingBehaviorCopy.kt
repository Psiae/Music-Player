package dev.dexsr.klio.player.android.presentation.root.main.pager.scroll

import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.*
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerController
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerPageInfo
import dev.dexsr.klio.player.android.presentation.root.main.pager.SnapLayoutInfoProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign


@OptIn(ExperimentalFoundationApi::class)
class PlaybackPagerFlingBehaviorCopy(
    private val pagerState: PlaybackPagerController,
    private val density: Density,
    private val snapAnimationSpec: AnimationSpec<Float>,
    private val lowVelocityAnimationSpec: AnimationSpec<Float>,
    private val highVelocityAnimationSpec: DecayAnimationSpec<Float>,
) {

    // TODO: fix behavior

    private val snapLayoutInfoProvider = SnapLayoutInfoProvider(
        pagerState = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        decayAnimationSpec = highVelocityAnimationSpec,
        snapPositionalThreshold = 0.5f
    )

    private val pagerVisibleItemsInfo: List<PlaybackPagerPageInfo>
        get() = pagerState.pagerLayoutInfo.visiblePagesInfo

    private val pagerPageSize: Float
        get() = if (pagerVisibleItemsInfo.isNotEmpty()) {
            pagerVisibleItemsInfo.fastSumBy { it.size } / pagerVisibleItemsInfo.size.toFloat()
        } else {
            0f
        }

    private val motionScaleDuration = object : MotionDurationScale {
        override val scaleFactor: Float
            get() = 1f
    }

    private val velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }

    private val shortSnapVelocityThreshold = 0.5f

    suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return performFling(initialVelocity) {}
    }

    /**
     * Perform a snapping fling animation with given velocity and suspend until fling has
     * finished. This will behave the same way as [performFling] except it will report on
     * each remainingOffsetUpdate using the [onSettlingDistanceUpdated] lambda.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     *
     * @param onSettlingDistanceUpdated a lambda that will be called anytime the
     * distance to the settling offset is updated. The settling offset is the final offset where
     * this fling will stop and may change depending on the snapping animation progression.
     *
     * @return remaining velocity after fling operation has ended
     */
    suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onSettlingDistanceUpdated: (Float) -> Unit
    ): Float {
        val (remainingOffset, remainingState) = fling(initialVelocity, onSettlingDistanceUpdated)

        debugLog { "Post Settling Offset=$remainingOffset" }
        // No remaining offset means we've used everything, no need to propagate velocity. Otherwise
        // we couldn't use everything (probably because we have hit the min/max bounds of the
        // containing layout) we should propagate the offset.
        return if (remainingOffset == 0f) NoVelocity else remainingState.velocity
    }

    private suspend fun ScrollScope.fling(
        initialVelocity: Float,
        onRemainingScrollOffsetUpdate: (Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {
        // If snapping from scroll (short snap) or fling (long snap)
        val result = withContext(motionScaleDuration) {
            if (abs(initialVelocity) <= abs(velocityThreshold)) {
                shortSnap(initialVelocity, onRemainingScrollOffsetUpdate)
            } else {
                longSnap(initialVelocity, onRemainingScrollOffsetUpdate)
            }
        }

        onRemainingScrollOffsetUpdate(0f) // Animation finished or was cancelled
        return result
    }

    private suspend fun ScrollScope.shortSnap(
        velocity: Float,
        onRemainingScrollOffsetUpdate: (Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {
        debugLog { "Short Snapping" }
        val closestOffset = with(snapLayoutInfoProvider) {
            density.calculateSnappingOffset(0f)
        }

        Timber.d("PlaybackPagerFlingBehavior_DEBUG: shortSnap(vel=$velocity, closestOffset=$closestOffset)")

        var remainingScrollOffset = closestOffset

        val animationState = AnimationState(NoDistance, velocity)
        return animateSnap(
            closestOffset,
            closestOffset,
            animationState,
            snapAnimationSpec
        ) { delta ->
            remainingScrollOffset -= delta
            onRemainingScrollOffsetUpdate(remainingScrollOffset)
        }
    }

    private suspend fun ScrollScope.longSnap(
        initialVelocity: Float,
        onAnimationStep: (remainingScrollOffset: Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {
        debugLog { "Long Snapping" }
        val initialOffset =
            with(snapLayoutInfoProvider) { density.calculateApproachOffset(initialVelocity) }.let {
                abs(it) * sign(initialVelocity) // ensure offset sign is correct
            }
        var remainingScrollOffset = initialOffset

        onAnimationStep(remainingScrollOffset) // First Scroll Offset

        val (remainingOffset, animationState) = runApproach(
            initialOffset,
            initialVelocity
        ) { delta ->
            remainingScrollOffset -= delta
            onAnimationStep(remainingScrollOffset)
        }

        debugLog { "Settling Final Bound=$remainingOffset" }

        remainingScrollOffset = remainingOffset

        return animateSnap(
            remainingOffset,
            remainingOffset,
            animationState.copy(value = 0f),
            snapAnimationSpec
        ) { delta ->
            remainingScrollOffset -= delta
            onAnimationStep(remainingScrollOffset)
        }
    }

    private suspend fun ScrollScope.runApproach(
        initialTargetOffset: Float,
        initialVelocity: Float,
        onAnimationStep: (delta: Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {

        val animation =
            if (isDecayApproachPossible(offset = initialTargetOffset, velocity = initialVelocity)) {
                debugLog { "High Velocity Approach" }
                HighVelocityApproachAnimation(highVelocityAnimationSpec)
            } else {
                debugLog { "Low Velocity Approach" }
                LowVelocityApproachAnimation(
                    lowVelocityAnimationSpec,
                    snapLayoutInfoProvider,
                    density
                )
            }

        return approach(
            initialTargetOffset,
            initialVelocity,
            animation,
            snapLayoutInfoProvider,
            density,
            onAnimationStep
        )
    }

    /**
     * If we can approach the target and still have velocity left
     */
    private fun isDecayApproachPossible(
        offset: Float,
        velocity: Float
    ): Boolean {
        val decayOffset = highVelocityAnimationSpec.calculateTargetValue(NoDistance, velocity)
        val snapStepSize = with(snapLayoutInfoProvider) { density.calculateSnapStepSize() }
        return decayOffset.absoluteValue >= (offset.absoluteValue + snapStepSize)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is PlaybackPagerFlingBehaviorCopy) {
            other.snapAnimationSpec == this.snapAnimationSpec &&
                    other.highVelocityAnimationSpec == this.highVelocityAnimationSpec &&
                    other.lowVelocityAnimationSpec == this.lowVelocityAnimationSpec &&
                    other.snapLayoutInfoProvider == this.snapLayoutInfoProvider &&
                    other.density == this.density &&
                    other.shortSnapVelocityThreshold == this.shortSnapVelocityThreshold
        } else {
            false
        }
    }

    override fun hashCode(): Int = 0
        .let { 31 * it + snapAnimationSpec.hashCode() }
        .let { 31 * it + highVelocityAnimationSpec.hashCode() }
        .let { 31 * it + lowVelocityAnimationSpec.hashCode() }
        .let { 31 * it + snapLayoutInfoProvider.hashCode() }
        .let { 31 * it + density.hashCode() }
        .let { 31 * it + shortSnapVelocityThreshold.hashCode() }
}

/**
 * Creates and remember a [FlingBehavior] that performs snapping.
 * @param snapLayoutInfoProvider The information about the layout that will do snapping
 */
@ExperimentalFoundationApi
@Composable
fun rememberSnapFlingBehavior(
    snapLayoutInfoProvider: SnapLayoutInfoProvider
): SnapFlingBehavior {
    val density = LocalDensity.current
    val highVelocityApproachSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(
        snapLayoutInfoProvider,
        highVelocityApproachSpec,
        density
    ) {
        SnapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            lowVelocityAnimationSpec = tween(easing = LinearEasing),
            highVelocityAnimationSpec = highVelocityApproachSpec,
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            density = density
        )
    }
}

/**
 * To ensure we do not overshoot, the approach animation is divided into 2 parts.
 *
 * In the initial animation we animate up until targetOffset. At this point we will have fulfilled
 * the requirement of [SnapLayoutInfoProvider.calculateApproachOffset] and we should snap to the
 * next [SnapLayoutInfoProvider.calculateSnappingOffset].
 *
 * The second part of the approach is a UX improvement. If the target offset is too far (in here, we
 * define too far as over half a step offset away) we continue the approach animation a bit further
 * and leave the remainder to be snapped.
 */
@OptIn(ExperimentalFoundationApi::class)
private suspend fun ScrollScope.approach(
    initialTargetOffset: Float,
    initialVelocity: Float,
    animation: ApproachAnimation<Float, AnimationVector1D>,
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    density: Density,
    onAnimationStep: (delta: Float) -> Unit
): AnimationResult<Float, AnimationVector1D> {

    val (_, currentAnimationState) = animation.approachAnimation(
        this,
        initialTargetOffset,
        initialVelocity,
        onAnimationStep
    )

    val remainingOffset = with(snapLayoutInfoProvider) {
        density.calculateSnappingOffset(currentAnimationState.velocity)
    }

    // will snap the remainder
    return AnimationResult(remainingOffset, currentAnimationState)
}

private class AnimationResult<T, V : AnimationVector>(
    val remainingOffset: T,
    val currentAnimationState: AnimationState<T, V>
) {
    operator fun component1(): T = remainingOffset
    operator fun component2(): AnimationState<T, V> = currentAnimationState
}

private operator fun <T : Comparable<T>> ClosedFloatingPointRange<T>.component1(): T = this.start
private operator fun <T : Comparable<T>> ClosedFloatingPointRange<T>.component2(): T =
    this.endInclusive

/**
 * Run a [DecayAnimationSpec] animation up to before [targetOffset] using [animationState]
 *
 * @param targetOffset The destination of this animation. Since this is a decay animation, we can
 * use this value to prevent the animation to run until the end.
 * @param animationState The previous [AnimationState] for continuation purposes.
 * @param decayAnimationSpec The [DecayAnimationSpec] that will drive this animation
 * @param onAnimationStep Called for each new scroll delta emitted by the animation cycle.
 */
private suspend fun ScrollScope.animateDecay(
    targetOffset: Float,
    animationState: AnimationState<Float, AnimationVector1D>,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    onAnimationStep: (delta: Float) -> Unit
): AnimationResult<Float, AnimationVector1D> {
    var previousValue = 0f

    fun AnimationScope<Float, AnimationVector1D>.consumeDelta(delta: Float) {
        val consumed = scrollBy(delta)
        onAnimationStep(consumed)
        if (abs(delta - consumed) > 0.5f) cancelAnimation()
    }

    animationState.animateDecay(
        decayAnimationSpec,
        sequentialAnimation = animationState.velocity != 0f
    ) {
        Timber.d("PlaybackPagerFlingBehavior_DEBUG: animateDecay(targetOffset=$targetOffset)_onFrameDoScroll(value=$value, targetOffset=$targetOffset)")
        if (abs(value) >= abs(targetOffset)) {
            val finalValue = value.coerceToTarget(targetOffset)
            val finalDelta = finalValue - previousValue
            consumeDelta(finalDelta)
            cancelAnimation()
            previousValue = finalValue
        } else {
            val delta = value - previousValue
            consumeDelta(delta)
            previousValue = value
        }
    }

    debugLog {
        "Decay Animation: Proposed Offset=$targetOffset Achieved Offset=$previousValue"
    }
    return AnimationResult(
        targetOffset - previousValue,
        animationState
    )
}

/**
 * Runs a [AnimationSpec] to snap the list into [targetOffset]. Uses [cancelOffset] to stop this
 * animation before it reaches the target.
 *
 * @param targetOffset The final target of this animation
 * @param cancelOffset If we'd like to finish the animation earlier we use this value
 * @param animationState The current animation state for continuation purposes
 * @param snapAnimationSpec The [AnimationSpec] that will drive this animation
 * @param onAnimationStep Called for each new scroll delta emitted by the animation cycle.
 */
private suspend fun ScrollScope.animateSnap(
    targetOffset: Float,
    cancelOffset: Float,
    animationState: AnimationState<Float, AnimationVector1D>,
    snapAnimationSpec: AnimationSpec<Float>,
    onAnimationStep: (delta: Float) -> Unit
): AnimationResult<Float, AnimationVector1D> {
    var consumedUpToNow = 0f
    val initialVelocity = animationState.velocity
    animationState.animateTo(
        targetOffset,
        animationSpec = snapAnimationSpec,
        sequentialAnimation = (animationState.velocity != 0f)
    ) {
        val realValue = value.coerceToTarget(cancelOffset)
        val delta = realValue - consumedUpToNow
        val consumed = scrollBy(delta)
        Timber.d("PlaybackPagerFlingBehavior_DEBUG: animateSnap_onFrame(initialVel=$initialVelocity, vel=$velocity, value=$value, realValue=$realValue, delta=$delta, consumed=$consumed)")
        onAnimationStep(consumed)
        // stop when unconsumed or when we reach the desired value
        if (abs(delta - consumed) > 0.5f || realValue != value) {
            cancelAnimation()
        }
        consumedUpToNow += consumed
    }

    // Always course correct velocity so they don't become too large.
    val finalVelocity = animationState.velocity.coerceToTarget(initialVelocity)
    return AnimationResult(
        targetOffset - consumedUpToNow,
        animationState.copy(velocity = finalVelocity)
    )
}

private fun Float.coerceToTarget(target: Float): Float {
    if (target == 0f) return 0f
    return if (target > 0) coerceAtMost(target) else coerceAtLeast(target)
}

/**
 * The animations used to approach offset and approach half a step offset.
 */
private interface ApproachAnimation<T, V : AnimationVector> {
    suspend fun approachAnimation(
        scope: ScrollScope,
        offset: T,
        velocity: T,
        onAnimationStep: (delta: T) -> Unit
    ): AnimationResult<T, V>
}

@OptIn(ExperimentalFoundationApi::class)
private class LowVelocityApproachAnimation(
    private val lowVelocityAnimationSpec: AnimationSpec<Float>,
    private val layoutInfoProvider: SnapLayoutInfoProvider,
    private val density: Density
) : ApproachAnimation<Float, AnimationVector1D> {
    override suspend fun approachAnimation(
        scope: ScrollScope,
        offset: Float,
        velocity: Float,
        onAnimationStep: (delta: Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {
        val animationState = AnimationState(initialValue = 0f, initialVelocity = velocity)
        val targetOffset =
            (abs(offset) + with(layoutInfoProvider) { density.calculateSnapStepSize() }) * sign(
                velocity
            )
        return with(scope) {
            animateSnap(
                targetOffset = targetOffset,
                cancelOffset = offset,
                animationState = animationState,
                snapAnimationSpec = lowVelocityAnimationSpec,
                onAnimationStep = onAnimationStep
            )
        }
    }
}

private class HighVelocityApproachAnimation(
    private val decayAnimationSpec: DecayAnimationSpec<Float>
) : ApproachAnimation<Float, AnimationVector1D> {
    override suspend fun approachAnimation(
        scope: ScrollScope,
        offset: Float,
        velocity: Float,
        onAnimationStep: (delta: Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {
        val animationState = AnimationState(initialValue = 0f, initialVelocity = velocity)
        return with(scope) {
            animateDecay(offset, animationState, decayAnimationSpec, onAnimationStep)
        }
    }
}

internal val MinFlingVelocityDp = 400.dp
internal const val NoDistance = 0f
internal const val NoVelocity = 0f

internal fun calculateFinalOffset(velocity: Float, lowerBound: Float, upperBound: Float): Float {

    fun Float.isValidDistance(): Boolean {
        return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
    }

    debugLog { "Proposed Bounds: Lower=$lowerBound Upper=$upperBound" }

    val finalDistance = when (sign(velocity)) {
        0f -> {
            if (abs(upperBound) <= abs(lowerBound)) {
                upperBound
            } else {
                lowerBound
            }
        }

        1f -> upperBound
        -1f -> lowerBound
        else -> NoDistance
    }

    return if (finalDistance.isValidDistance()) {
        finalDistance
    } else {
        NoDistance
    }
}

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("SnapFlingBehavior: ${generateMsg()}")
    }
}
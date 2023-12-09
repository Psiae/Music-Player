package dev.dexsr.klio.player.android.presentation.root.main.pager.scroll

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.*
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerController
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerLayoutInfo
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackPagerPageInfo
import dev.dexsr.klio.player.android.presentation.root.main.pager.SnapLayoutInfoProvider
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign


@OptIn(ExperimentalFoundationApi::class)
class PlaybackPagerFlingBehavior(
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
            get() = DefaultScrollMotionDurationScaleFactor
    }

    private val velocityThreshold = with(density) { MinFlingVelocityDp.toPx() }

    suspend fun ScrollScope.performFling(
        initialVelocity: Float
    ): Float = performFling(
        initialVelocity = initialVelocity,
        onSettlingDistanceUpdated = {}
    )

    suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onSettlingDistanceUpdated: (Float) -> Unit
    ): Float {
        val (remainingOffset, remainingState) = fling(initialVelocity, onSettlingDistanceUpdated)

        // No remaining offset means we've used everything, no need to propagate velocity. Otherwise
        // we couldn't use everything (probably because we have hit the min/max bounds of the
        // containing layout) we should propagate the offset.
        return if (remainingOffset == 0f) 0f else remainingState.velocity
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

        return result
    }

    // snap within [MinFlingVelocityDp] in pixel
    private suspend fun ScrollScope.shortSnap(
        velocity: Float,
        onRemainingScrollOffsetUpdate: (Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {

        val closestOffset = with(snapLayoutInfoProvider) {
            density.calculateSnappingOffset(0f)
        }

        var remainingScrollOffset = closestOffset

        val animationState = AnimationState(NoDistance, velocity)
        return animateSnap(
            closestOffset,
            closestOffset,
            animationState,
            snapAnimationSpec,
            onAnimationStep = { delta ->
                remainingScrollOffset -= delta
                onRemainingScrollOffsetUpdate(remainingScrollOffset)
            }
        )
    }

    // snap over [MinFlingVelocityDp] in pixel
    private suspend fun ScrollScope.longSnap(
        initialVelocity: Float
    ): AnimationResult<Float, AnimationVector1D> {

        val offset =
            highVelocityAnimationSpec.calculateTargetValue(0f, initialVelocity).absoluteValue

        val finalDecayOffset = (offset - pagerPageSize).coerceAtLeast(0f)
        val initialOffset = if (finalDecayOffset == 0f) {
            finalDecayOffset
        } else {
            finalDecayOffset * initialVelocity.sign
        }

        val (remainingOffset, animationState) = runApproach(
            initialOffset,
            initialVelocity
        )

        return animateSnap(
            remainingOffset,
            remainingOffset,
            animationState.copy(value = 0f),
            snapAnimationSpec
        )
    }

    private suspend fun ScrollScope.longSnap(
        initialVelocity: Float,
        onAnimationStep: (remainingScrollOffset: Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {
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
        initialVelocity: Float
    ): AnimationResult<Float, AnimationVector1D> {
        val animation =
            if (isDecayApproachPossible(offset = initialTargetOffset, velocity = initialVelocity)) {
                HighVelocityApproachAnimation(highVelocityAnimationSpec)
            } else {
                LowVelocityApproachAnimation(
                    lowVelocityAnimationSpec,
                    snapLayoutInfoProvider,
                    density
                )
            }
        val animationState = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
        val (_, currentAnimationState) = with(this) {
            animateDecay(initialTargetOffset, animationState, highVelocityAnimationSpec)
        }
        val remainingOffset =
            findClosestOffset(currentAnimationState.velocity)
        // will snap the remainder
        return AnimationResult(remainingOffset, currentAnimationState)
    }

    private suspend fun ScrollScope.runApproach(
        initialTargetOffset: Float,
        initialVelocity: Float,
        onAnimationStep: (delta: Float) -> Unit
    ): AnimationResult<Float, AnimationVector1D> {

        val animation =
            if (isDecayApproachPossible(offset = initialTargetOffset, velocity = initialVelocity)) {
                HighVelocityApproachAnimation(
                    highVelocityAnimationSpec
                )
            } else {
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
        return if (other is PlaybackPagerFlingBehavior) {
            other.snapAnimationSpec == this.snapAnimationSpec &&
                    other.highVelocityAnimationSpec == this.highVelocityAnimationSpec &&
                    other.lowVelocityAnimationSpec == this.lowVelocityAnimationSpec &&
                    other.pagerState == this.pagerState &&
                    other.density == this.density
        } else {
            false
        }
    }

    override fun hashCode(): Int = 0
        .let { 31 * it + snapAnimationSpec.hashCode() }
        .let { 31 * it + highVelocityAnimationSpec.hashCode() }
        .let { 31 * it + lowVelocityAnimationSpec.hashCode() }
        .let { 31 * it + pagerState.hashCode() }
        .let { 31 * it + density.hashCode() }

    private fun findClosestOffset(
        velocity: Float,
    ): Float {

        fun Float.isValidDistance(): Boolean {
            return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
        }

        fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
            var lowerBoundOffset = Float.NEGATIVE_INFINITY
            var upperBoundOffset = Float.POSITIVE_INFINITY

            with(pagerState.pagerLayoutInfo) {
                visiblePagesInfo.fastForEach { item ->
                    val offset =
                        calculateDistanceToDesiredSnapPosition(this, item)

                    // Find item that is closest to the center
                    if (offset <= 0 && offset > lowerBoundOffset) {
                        lowerBoundOffset = offset
                    }

                    // Find item that is closest to center, but after it
                    if (offset >= 0 && offset < upperBoundOffset) {
                        upperBoundOffset = offset
                    }
                }
            }

            return lowerBoundOffset.rangeTo(upperBoundOffset)
        }

        val (lowerBound, upperBound) = calculateSnappingOffsetBounds()

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
            else -> 0f
        }

        return if (finalDistance.isValidDistance()) {
            finalDistance
        } else {
            0f
        }
    }

    /**
     * Run a [DecayAnimationSpec] animation up to before [targetOffset] using [animationState]
     *
     * @param targetOffset The destination of this animation. Since this is a decay animation, we can
     * use this value to prevent the animation to run until the end.
     * @param animationState The previous [AnimationState] for continuation purposes.
     * @param highVelocityAnimationSpec The [DecayAnimationSpec] that will drive this animation
     */
    private suspend fun ScrollScope.animateDecay(
        targetOffset: Float,
        animationState: AnimationState<Float, AnimationVector1D>,
        highVelocityAnimationSpec: DecayAnimationSpec<Float>
    ): AnimationResult<Float, AnimationVector1D> {
        var previousValue = 0f

        fun AnimationScope<Float, AnimationVector1D>.consumeDelta(delta: Float) {
            val consumed = scrollBy(delta)
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }

        animationState.animateDecay(
            highVelocityAnimationSpec,
            sequentialAnimation = animationState.velocity != 0f
        ) {
            if (abs(value) >= abs(targetOffset)) {
                val finalValue = value.animCoerceToTarget(targetOffset)
                val finalDelta = finalValue - previousValue
                consumeDelta(finalDelta)
                cancelAnimation()
            } else {
                val delta = value - previousValue
                consumeDelta(delta)
                previousValue = value
            }
        }
        return AnimationResult(
            targetOffset - previousValue,
            animationState
        )
    }

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
            if (abs(value) >= abs(targetOffset)) {
                val finalValue = value.animCoerceToTarget(targetOffset)
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
        return AnimationResult(
            targetOffset - previousValue,
            animationState
        )
    }

    private fun calculateDistanceToDesiredSnapPosition(
        layoutInfo: PlaybackPagerLayoutInfo,
        item: PlaybackPagerPageInfo
    ): Float {
        val containerSize =
            with(layoutInfo) { scrollAxisViewportSize - beforeContentPadding - afterContentPadding }

        val desiredDistance =
            containerSize.toFloat() / 2 - item.size.toFloat() / 2 // snap to center

        val itemCurrentPosition = item.offset
        return itemCurrentPosition - desiredDistance
    }

    /**
     * Runs a [AnimationSpec] to snap the list into [targetOffset]. Uses [cancelOffset] to stop this
     * animation before it reaches the target.
     *
     * @param targetOffset The final target of this animation
     * @param cancelOffset If we'd like to finish the animation earlier we use this value
     * @param animationState The current animation state for continuation purposes
     * @param snapAnimationSpec The [AnimationSpec] that will drive this animation
     */
    private suspend fun ScrollScope.animateSnap(
        targetOffset: Float,
        cancelOffset: Float,
        animationState: AnimationState<Float, AnimationVector1D>,
        snapAnimationSpec: AnimationSpec<Float>
    ): AnimationResult<Float, AnimationVector1D> {
        var consumedUpToNow = 0f
        val initialVelocity = animationState.velocity
        animationState.animateTo(
            targetOffset,
            animationSpec = snapAnimationSpec,
            sequentialAnimation = (animationState.velocity != 0f)
        ) {
            val realValue = value.animCoerceToTarget(cancelOffset)
            val delta = realValue - consumedUpToNow
            val consumed = scrollBy(delta)
            // stop when unconsumed or when we reach the desired value
            if (abs(delta - consumed) > 0.5f || realValue != value) {
                cancelAnimation()
            }
            consumedUpToNow += consumed
        }

        // Always course correct velocity so they don't become too large.
        val finalVelocity = animationState.velocity.animCoerceToTarget(initialVelocity)
        return AnimationResult(
            targetOffset - consumedUpToNow,
            animationState.copy(velocity = finalVelocity)
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
            val realValue = value.animCoerceToTarget(cancelOffset)
            val delta = realValue - consumedUpToNow
            val consumed = scrollBy(delta)
            onAnimationStep(consumed)
            // stop when unconsumed or when we reach the desired value
            if (abs(delta - consumed) > 0.5f || realValue != value) {
                cancelAnimation()
            }
            consumedUpToNow += consumed
        }

        // Always course correct velocity so they don't become too large.
        val finalVelocity = animationState.velocity.animCoerceToTarget(initialVelocity)
        return AnimationResult(
            targetOffset - consumedUpToNow,
            animationState.copy(velocity = finalVelocity)
        )
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
        return AnimationResult(
            remainingOffset,
            currentAnimationState
        )
    }

    private val PlaybackPagerLayoutInfo.scrollAxisViewportSize: Int
        get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

    private class AnimationResult<T, V : AnimationVector>(
        val remainingOffset: T,
        val currentAnimationState: AnimationState<T, V>
    ) {
        operator fun component1(): T = remainingOffset
        operator fun component2(): AnimationState<T, V> = currentAnimationState
    }

    private operator fun <T : Comparable<T>> ClosedFloatingPointRange<T>.component1(): T =
        this.start

    private operator fun <T : Comparable<T>> ClosedFloatingPointRange<T>.component2(): T =
        this.endInclusive

    private fun Float.animCoerceToTarget(target: Float): Float {
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
    private inner class LowVelocityApproachAnimation(
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

    private inner class HighVelocityApproachAnimation(
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
                animateDecay(
                    offset,
                    animationState,
                    decayAnimationSpec,
                    onAnimationStep
                )
            }
        }
    }

    companion object {
        private const val DefaultScrollMotionDurationScaleFactor = 1f
        private val MinFlingVelocityDp = 400.dp
        private const val NoDistance = 0f
        private const val NoVelocity = 0f
    }
}
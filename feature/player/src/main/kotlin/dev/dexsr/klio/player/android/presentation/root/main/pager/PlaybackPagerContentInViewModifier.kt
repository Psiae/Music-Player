package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import dev.dexsr.klio.player.android.presentation.root.main.gesture.UpdatableAnimationState
import dev.dexsr.klio.player.android.presentation.root.main.pager.gesture.PlaybackPagerBringIntoViewRequestPriorityQueue
import kotlinx.coroutines.*
import kotlin.math.abs

private const val DEBUG = false
private const val TAG = "PlaybackPagerContentInViewModifier"
private const val IMPL = false

// delegating node ?
@OptIn(ExperimentalFoundationApi::class)
internal class PlaybackPagerContentInViewModifier(
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val scrollState: PlaybackPagerScrollableState,
    private val reverseDirection: Boolean
) : BringIntoViewResponder,
    OnRemeasuredModifier,
    OnPlacedModifier {

    /**
     * Ongoing requests from [bringChildIntoView], with the invariant that it is always sorted by
     * overlapping order: each item's [Rect] completely overlaps the next item.
     *
     * May contain requests whose bounds are too big to fit in the current viewport. This is for
     * a few reasons:
     *  1. The viewport may shrink after a request was enqueued, causing a request that fit at the
     *     time it was enqueued to no longer fit.
     *  2. The size of the bounds of a request may change after it's added, causing it to grow
     *     larger than the viewport.
     *  3. Having complete information about too-big requests allows us to make the right decision
     *     about what part of the request to bring into view when smaller requests are also present.
     */
    private val bringIntoViewRequests = PlaybackPagerBringIntoViewRequestPriorityQueue()

    /** The [LayoutCoordinates] of this modifier (i.e. the scrollable container). */
    private var coordinates: LayoutCoordinates? = null
    private var focusedChild: LayoutCoordinates? = null

    /**
     * The previous bounds of the [focusedChild] used by [onRemeasured] to calculate when the
     * focused child is first clipped when scrolling is reversed.
     */
    private var focusedChildBoundsFromPreviousRemeasure: Rect? = null

    /**
     * Set to true when this class is actively animating the scroll to keep the focused child in
     * view.
     */
    private var trackingFocusedChild = false

    private var viewportSize = IntSize.Zero
    private var isAnimationRunning = false
    private val animationState = UpdatableAnimationState()

    val modifier: Modifier = this
        .onFocusedBoundsChanged {
            focusedChild = it
            if (DEBUG) println("[$TAG] new focused child: ${getFocusedChildBounds()}")
        }
        .bringIntoViewResponder(this)

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onRemeasured(size: IntSize) {
        val oldSize = viewportSize
        viewportSize = size

        // Don't care if the viewport grew.
        if (size >= oldSize) return

        if (DEBUG) println("[${TAG}] viewport shrunk: $oldSize -> $size")

        getFocusedChildBounds()?.let { focusedChild ->
            if (DEBUG) println("[${TAG}] focused child bounds: $focusedChild")
            val previousFocusedChildBounds = focusedChildBoundsFromPreviousRemeasure ?: focusedChild
            if (!isAnimationRunning && !trackingFocusedChild &&
                // Resize caused it to go from being fully visible to at least partially
                // clipped. Need to use the lastFocusedChildBounds to compare with the old size
                // only to handle the case where scrolling direction is reversed: in that case, when
                // the child first goes out-of-bounds, it will be out of bounds regardless of which
                // size we pass in, so the only way to detect the change is to use the previous
                // bounds.
                previousFocusedChildBounds.isMaxVisible(oldSize) && !focusedChild.isMaxVisible(size)
            ) {
                if (DEBUG) println(
                    "[${TAG}] focused child was clipped by viewport shrink: $focusedChild"
                )
                trackingFocusedChild = true
                launchAnimation()
            }

            this.focusedChildBoundsFromPreviousRemeasure = focusedChild
        }
    }

    @ExperimentalFoundationApi
    override suspend fun bringChildIntoView(localRect: () -> Rect?) {
        // Avoid creating no-op requests and no-op animations if the request does not require
        // scrolling or returns null.
        if (localRect()?.isMaxVisible() != false) return

        suspendCancellableCoroutine { continuation ->
            val request = Request(
                currentBounds = localRect,
                continuation = continuation
            )
            if (DEBUG) println("[${TAG}] Registering bringChildIntoView request: $request")
            // Once the request is enqueued, even if it returns false, the queue will take care of
            // handling continuation cancellation so we don't need to do that here.
            if (bringIntoViewRequests.enqueue(request) && !isAnimationRunning) {
                launchAnimation()
            }
        }
    }

    @ExperimentalFoundationApi
    override fun calculateRectForParent(localRect: Rect): Rect {
        check(viewportSize != IntSize.Zero) {
            "Expected BringIntoViewRequester to not be used before parents are placed."
        }
        // size will only be zero before the initial measurement.
        return computeDestination(localRect, viewportSize)
    }


    private fun getFocusedChildBounds(): Rect? {
        val coordinates = this.coordinates?.takeIf { it.isAttached } ?: return null
        val focusedChild = this.focusedChild?.takeIf { it.isAttached } ?: return null
        return coordinates.localBoundingBoxOf(focusedChild, clipBounds = false)
    }

    private fun launchAnimation() {
        check(!isAnimationRunning)

        if (DEBUG) println("[${TAG}] launchAnimation")

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var cancellationException: CancellationException? = null
            val animationJob = coroutineContext.job

            try {
                isAnimationRunning = true

                scrollState.bringChildFocusScroll {

                }

                // Complete any BIV requests if the animation didn't need to run, or if there were
                // requests that were too large to satisfy. Note that if the animation was
                // cancelled, this won't run, and the requests will be cancelled instead.
                if (DEBUG) println(
                    "[${TAG}] animation completed successfully, resuming" +
                            " ${bringIntoViewRequests.size} remaining BIV requests…"
                )
                bringIntoViewRequests.resumeAndRemoveAll()
            } catch (e: CancellationException) {
                cancellationException = e
                throw e
            } finally {
                if (DEBUG) {
                    println(
                        "[${TAG}] animation completed with ${bringIntoViewRequests.size} " +
                                "unsatisfied BIV requests"
                    )
                    cancellationException?.printStackTrace()
                }
                isAnimationRunning = false
                // Any BIV requests that were not completed should be considered cancelled.
                bringIntoViewRequests.cancelAndRemoveAll(cancellationException)
                trackingFocusedChild = false
            }
        }
    }

    private operator fun IntSize.compareTo(other: IntSize): Int = when (orientation) {
        Orientation.Horizontal -> width.compareTo(other.width)
        Orientation.Vertical -> height.compareTo(other.height)
    }

    private operator fun Size.compareTo(other: Size): Int = when (orientation) {
        Orientation.Horizontal -> width.compareTo(other.width)
        Orientation.Vertical -> height.compareTo(other.height)
    }

    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param childBounds The bounding box of the item that sent the request to be brought into view.
     * @return the destination rectangle.
     */
    private fun computeDestination(childBounds: Rect, containerSize: IntSize): Rect {
        return childBounds.translate(-relocationOffset(childBounds, containerSize))
    }

    /**
     * Returns true if this [Rect] is as visible as it can be given the [size] of the viewport.
     * This means either it's fully visible or too big to fit in the viewport all at once and
     * already filling the whole viewport.
     */
    private fun Rect.isMaxVisible(size: IntSize = viewportSize): Boolean {
        return relocationOffset(this, size) == Offset.Zero
    }

    private fun relocationOffset(childBounds: Rect, containerSize: IntSize): Offset {
        val size = containerSize.toSize()
        return when (orientation) {
            Orientation.Vertical -> Offset(
                x = 0f,
                y = relocationDistance(childBounds.top, childBounds.bottom, size.height)
            )

            Orientation.Horizontal -> Offset(
                x = relocationDistance(childBounds.left, childBounds.right, size.width),
                y = 0f
            )
        }
    }

    /**
     * Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
     * closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
     * The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
     * 'bottom').
     */
    private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, containerSize: Float) =
        when {
            // If the item is already visible, no need to scroll.
            leadingEdge >= 0 && trailingEdge <= containerSize -> 0f

            // If the item is visible but larger than the parent, we don't scroll.
            leadingEdge < 0 && trailingEdge > containerSize -> 0f

            // Find the minimum scroll needed to make one of the edges coincide with the parent's
            // edge.
            abs(leadingEdge) < abs(trailingEdge - containerSize) -> leadingEdge
            else -> trailingEdge - containerSize
        }

    /**
     * A request to bring some [Rect] in the scrollable viewport.
     *
     * @param currentBounds A function that returns the current bounds that the request wants to
     * make visible.
     * @param continuation The [CancellableContinuation] from the suspend function used to make the
     * request.
     */
    internal class Request(
        val currentBounds: () -> Rect?,
        val continuation: CancellableContinuation<Unit>,
    ) {
        override fun toString(): String {
            // Include the coroutine name in the string, if present, to help debugging.
            val name = continuation.context[CoroutineName]?.name
            return "Request@${hashCode().toString(radix = 16)}" +
                    (name?.let { "[$it](" } ?: "(") +
                    "currentBounds()=${currentBounds()}, " +
                    "continuation=$continuation)"
        }
    }
}
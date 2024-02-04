package dev.dexsr.klio.library.user.playlist

import androidx.annotation.UiThread
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.monotonicFrameClock
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import dev.dexsr.klio.base.composeui.SnapshotRead
import dev.dexsr.klio.base.composeui.annotations.ComposeUiClass
import dev.dexsr.klio.core.AndroidUiFoundation
import dev.dexsr.klio.core.MainDispatcher
import dev.dexsr.klio.core.isOnUiLooper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@ComposeUiClass
class PlaylistDetailScreenScrollState(
	private val flingBehavior: FlingBehavior
) : RememberObserver {

	private var _remeasurement: Remeasurement? = null

	private var _coroutineScope: CoroutineScope? = null

	private var layoutOffset: Offset = Offset.Zero

	private val coroutineScope
		get() = requireNotNull(_coroutineScope)

	// Compose Remember API

	override fun onAbandoned() {
	}

	override fun onForgotten() {
		coroutineScope.cancel()
	}

	override fun onRemembered() {
		_coroutineScope = CoroutineScope(SupervisorJob())
	}

	val rootFlingBehavior = flingBehavior

	val root = RootScrollableState(
		rootFlingBehavior,
		coroutineScope = ::coroutineScope
	)

	// TODO: consume overscroll effect
	@OptIn(ExperimentalComposeApi::class)
	val rootNestedScrollConnection = object : NestedScrollConnection {

		override suspend fun onPreFling(available: Velocity): Velocity {
			checkDescendantScrollDispatcher()
			val y = available.y
			val remain = doFlingBy(y)
			return Velocity(0f, y - remain)
		}

		override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
			checkDescendantScrollDispatcher()
			val y = available.y
			val remain = doFlingBy(y)
			return Velocity(0f, y - remain)
		}

		override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
			checkDescendantScrollDispatcher()
			val y = available.y
			val remain = doScrollBy(y)
			return Offset(0f, remain)
		}

		override fun onPostScroll(
			consumed: Offset,
			available: Offset,
			source: NestedScrollSource
		): Offset {
			checkDescendantScrollDispatcher()
			val y = available.y
			val remain = doScrollBy(y)
			return Offset(0f, remain)
		}

		private fun checkDescendantScrollDispatcher() {
			// expect that this callback is invoked on Modifier Node dispatcher,
			// unlike Recomposer, it should still be confined to the UI Thread
			check(AndroidUiFoundation.isOnUiLooper()) {
				"NestedScrollDispatcher did not dispatch event on Android Ui Looper"
			}
		}

		private fun doScrollBy(
			pixels: Float
		): Float {
			return root.dispatchRawDelta(pixels)
		}

		private suspend fun doFlingBy(
			velocity: Float
		): Float {
			var left = velocity
			val fling = coroutineScope.launch(AndroidUiFoundation.MainDispatcher.immediate) {
				root.scroll {
					val out = this
					val inner = object : ScrollScope {
						val iniSign = velocity.sign
						override fun scrollBy(pixels: Float): Float {
							if (pixels != 0f) {
								if (pixels.sign != iniSign) return 0f
							}
							return out.scrollBy(pixels)
						}
					}
					withContext(AndroidUiDispatcher.Main.monotonicFrameClock) {
						with(flingBehavior) {
							left = inner.performFling(velocity)
						}
					}
				}
				// if the scope is cancelled then child also should not render
			}
			try {
			    fling.join()
			} catch (ce: CancellationException) {
				left = 0f
			}
			return left
		}
	}

	// TODO: apply to overscroll0
	class RootScrollableState(
		private val flingBehavior: FlingBehavior,
		private val coroutineScope: () -> CoroutineScope
	) : ScrollableState {

		private var currentScrollPriority by mutableIntStateOf(-1)

		private var topPadding: ChildLayout? = null
		private var bottomPadding: ChildLayout? = null

		private var desc: ChildLayout? = null
		private var playlist: ScrollableChildLayout? = null

		private var layoutChilds = listOf<ChildLayout>()

		// scroll float register
		private var scrollFloatAcc by mutableFloatStateOf(0f)

		private var _viewportSize: IntSize? = null
			private set

		private var _maxLength: Int? = null
			private set

		private var currentFling: Job? = null
		private var currentScroll: Job? = null

		val viewportSize: IntSize
			get() = _viewportSize ?: IntSize.Zero

		val maxLength: Int
			get() = _maxLength ?: 0

		var value: Int by mutableIntStateOf(0)
			private set

		val scrollableValue: Float
			get() = value + scrollFloatAcc

		override val canScrollBackward: Boolean
			@SnapshotRead get() = value > 0
		override val canScrollForward: Boolean
			@SnapshotRead get() = value < maxLength
		override val isScrollInProgress: Boolean
			get() = currentScroll?.isActive == true || currentFling?.isActive == true

		val userLayoutDraggable = object : DraggableState {

			override fun dispatchRawDelta(delta: Float) {
				error("dispatchRawDelta on UserInput API")
			}

			override suspend fun drag(
				dragPriority: MutatePriority,
				block: suspend DragScope.() -> Unit
			) {
				check(AndroidUiFoundation.isOnUiLooper())
				if (dragPriority < MutatePriority.UserInput) {
					error("dragPriority=$dragPriority on UserInput API")
				}
				coroutineScope {
					scroll(dragPriority) {
						object : DragScope {
							override fun dragBy(pixels: Float) {
								scrollBy(pixels)
							}
						}.block()
					}
				}
			}
		}

		override fun dispatchRawDelta(delta: Float): Float = -doScrollTraversal(-delta)

		override suspend fun scroll(
			scrollPriority: MutatePriority,
			block: suspend ScrollScope.() -> Unit
		) {
			check(AndroidUiFoundation.isOnUiLooper())
			if (scrollPriority < MutatePriority.UserInput) {
				object : ScrollScope {
					override fun scrollBy(pixels: Float): Float = dispatchRawDelta(pixels)
				}.block()
				return
			}
			coroutineScope {
				currentScroll?.cancel()
				currentFling?.cancel()
				currentScroll = launch {
					object : ScrollScope {
						override fun scrollBy(pixels: Float): Float {
							return -doScrollTraversal(-pixels)
						}
					}.block()
				}
			}
		}

		fun onScrollMeasure(
			constraints: Constraints,
			height: Int,
			width: Int,
			viewport: Int
		) {
			check(AndroidUiFoundation.isOnUiLooper())
			_viewportSize = IntSize(width, viewport)
			_maxLength = height

			// coerce the scroll value
			if (value > maxLength) value = maxLength
		}

		fun onChildsMeasure(
			topPadding: ChildLayout,
			bottomPadding: ChildLayout,
			desc: ChildLayout,
			playlist: ChildLayout
		) {
			check(AndroidUiFoundation.isOnUiLooper())
			this.topPadding?.update(topPadding)
				?: run { this.topPadding = topPadding }
			this.bottomPadding?.update(bottomPadding)
				?: run { this.bottomPadding = bottomPadding }
			this.desc?.update(desc)
				?: run { this.desc = desc }
			this.playlist?.update(playlist)
				?: run { this.playlist = ScrollableChildLayout(playlist.height, playlist.spacing) }

			this.layoutChilds = listOfNotNull(this.topPadding, this.desc, this.playlist, this.bottomPadding)
		}

		fun updatePlaylistScrollable(state: ScrollableState) {
			this.playlist
				?.apply {
					scrollableScrollState = state
				}
				?: run { this.playlist = ScrollableChildLayout(0, 0) }
		}

		fun performFling(velocity: Velocity) {
			coroutineScope().launch(AndroidUiFoundation.MainDispatcher.immediate) {
				onUserScrollFling(velocity)
			}
		}

		// would it be nicer to let rootScroll be negative ?
		private fun doScrollTraversal(pixel: Float): Float {
			check(AndroidUiFoundation.isOnUiLooper())
			if (pixel == 0f) return 0f
			val scrollDelta = abs(pixel)
			var scrollDeltaVar = scrollDelta
			val scrollDeltaDirection = pixel.sign.toInt()
			val isScrollUp = scrollDeltaDirection < 0

			val layoutBottom = maxLength
			val viewportBottom = viewportSize.height

			var rootScroll = 0f
			val rootScrollLimit = layoutBottom - viewportBottom

			val initialScroll = value + scrollFloatAcc

			var h = 0

			fun rootLayoutConsumable(): Float {
				return if (isScrollUp) initialScroll - rootScroll
				else rootScrollLimit - (initialScroll + rootScroll)
			}

			fun consumeEndEffect() {
				this.applyConsumedScrollStrict(
					rootScroll,
					scrollDeltaDirection
				)
			}

			fun reachedDeltaConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)
				return scrollDeltaVar == 0f
			}

			fun reachedLayoutConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)
				return rootLayoutConsumable() == 0f
			}

			fun reachedAnyConstraint() = reachedDeltaConstraint() || reachedLayoutConstraint()

			fun consumed(): Float {
				check(scrollDeltaVar in 0f..scrollDelta)
				return (scrollDelta - scrollDeltaVar) * scrollDeltaDirection
			}

			fun rootConsumable(height: Int) {
				if (isScrollUp) {
					val top = layoutBottom - (h + height)
					if (top < initialScroll) {
						val consumable = minOf(initialScroll - top, scrollDeltaVar, rootLayoutConsumable())
						scrollDeltaVar -= consumable
						rootScroll += consumable
					}
				} else {
					if (h + height - initialScroll > 0) {
						val consume = minOf(height.toFloat(), scrollDeltaVar, rootLayoutConsumable())
						scrollDeltaVar -= consume
						rootScroll += consume
					}
				}
				h += height
			}

			fun rootTraversalConsumable(
				height: Int,
				consumeTraversal: (Float) -> Float
			) {
				if (isScrollUp) {
					rootConsumable(height)
				}
				if (scrollDeltaVar > 0) {
					val consumed = consumeTraversal(scrollDeltaVar * scrollDeltaDirection) * scrollDeltaDirection
					check(consumed in 0f..scrollDeltaVar)
					scrollDeltaVar -= consumed
				}
				if (!isScrollUp) {
					rootConsumable(height)
				}
			}

			(if (isScrollUp) layoutChilds.asReversed() else layoutChilds)
				.fastForEach { e ->
					when(e) {
						is ScrollableChildLayout -> rootTraversalConsumable(e.height + e.spacing, e::scrollTraversal)
						else -> rootConsumable(e.height + e.spacing)
					}
					if (reachedDeltaConstraint()) {
						consumeEndEffect()
						return consumed()
					}
				}

			check(reachedAnyConstraint()) {
				"constraint weren't reached, ini=$initialScroll, delta=$scrollDelta, deltaVar=$scrollDeltaVar, consumable=${rootLayoutConsumable()}"
			}
			consumeEndEffect()
			return consumed()
		}

		/*// [pixel] is scroll delta
		private fun doScrollTraversal(pixel: Float): Float {
			println("onUserScroll(pixel=$pixel)")
			if (pixel < 0f) return -doScrollTraversalUp(-pixel)
			if (pixel == 0f) return 0f
			val scrollDelta = pixel
			var scrollDeltaVar = scrollDelta
			var rootScroll = 0f
			val rootScrollLimit = maxLength - viewportSize.height
			val initialValue = value + scrollFloatAcc
			var h = 0

			fun layoutConsumable() = rootScrollLimit - (initialValue + rootScroll)

			fun consumeEndEffect() {
				val valueAcc = initialValue + rootScroll
				val valueAccInt = valueAcc.toInt()
				value = valueAccInt
				scrollFloatAcc = if (valueAccInt < rootScrollLimit) {
					valueAcc - valueAccInt
				} else {
					0f
				}
			}

			fun reachedAnyConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)

				// consumed all delta
				return scrollDeltaVar == 0f ||
					// reached end of layout
					layoutConsumable() == 0f
			}

			fun reachedLayoutConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)
				return layoutConsumable() == 0f
			}

			fun reachedDeltaConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)
				return scrollDeltaVar == 0f
			}

			fun consumed(): Float {
				check(scrollDeltaVar in 0f..scrollDelta)
				return (scrollDelta - scrollDeltaVar)
			}

			fun rootConsumable(height: Int) {
				if (h + height - initialValue > 0) {
					val consume = minOf(height.toFloat(), scrollDeltaVar, layoutConsumable())
					scrollDeltaVar -= consume
					rootScroll += consume
				}
				h += height
			}

			fun rootTraversalConsumable(
				height: Int,
				consumeTraversal: (Float) -> Float
			) {
				if (scrollDeltaVar > 0) {
					val consumed = consumeTraversal(scrollDeltaVar)
					check(consumed in 0f..scrollDeltaVar)
					scrollDeltaVar -= consumed
				}
				rootConsumable(height)
			}

			layoutChilds.fastForEach { e ->
				when(e) {
					is ScrollableChildLayout -> rootTraversalConsumable(e.height + e.spacing, e::scrollTraversal)
					else -> rootConsumable(e.height + e.spacing)
				}
				if (reachedDeltaConstraint()) {
					consumeEndEffect()
					return consumed()
				}
			}

			check(reachedAnyConstraint()) {
				"constraint weren't reached, delta=$scrollDelta, deltaVar=$scrollDeltaVar, layoutBottom=$rootScrollLimit, consumable=${layoutConsumable()}"
			}
			consumeEndEffect()
			return consumed()
		}

		// might be a bug in here
		private fun doScrollTraversalUp(
			pixel: Float
		): Float {
			if (pixel == 0f) return 0f
			// pixel should be absolute
			require(pixel > 0f)

			val scrollDelta = pixel
			var scrollDeltaVar = scrollDelta
			var rootScroll = 0f
			val initialValue = value + scrollFloatAcc
			val layoutBottom = maxLength
			val viewportHeight = viewportSize.height
			val viewportBottom = initialValue + viewportHeight
			val maxRootScroll = layoutBottom - viewportHeight

			var h = 0

			fun layoutConsumable() = initialValue - rootScroll

			fun consumed(): Float {
				check(scrollDeltaVar in 0f..scrollDelta)
				return (scrollDelta - scrollDeltaVar)
			}

			fun reachedDeltaConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)
				return scrollDeltaVar == 0f
			}

			fun reachedLayoutConstraint(): Boolean {
				check(scrollDeltaVar >= 0f)
				return layoutConsumable() == 0f
			}

			fun reachedAnyConstraint(): Boolean {
				return reachedDeltaConstraint() || reachedLayoutConstraint()
			}

			fun consumeEndEffect() {
				val accumulatedValue = layoutConsumable()
				check(accumulatedValue >= 0) {
					"accumulatedValue was not positive, accm=$accumulatedValue, initialValue=$initialValue delta=$scrollDelta, deltaVar=$scrollDeltaVar"
				}
				val accumulatedValueInt = accumulatedValue.roundToInt()
				value = accumulatedValueInt
				scrollFloatAcc = accumulatedValue - accumulatedValueInt
			}

			fun rootConsumable(
				height: Int
			) {
				val top = layoutBottom - (h + height)
				if (top < initialValue) {
					val consumable = minOf(initialValue - top, scrollDeltaVar, layoutConsumable())
					scrollDeltaVar -= consumable
					rootScroll += consumable
				}
				h += height
			}

			fun rootTraversalConsumable(
				height: Int,
				scrollTraversal: (Float) -> Float
			) {
				// scroll on root until fully visible then delegate to inner scroll
				rootConsumable(height)
				if (scrollDeltaVar > 0f) {
					val consumed = -scrollTraversal(-scrollDeltaVar)
					check(consumed in 0f..scrollDeltaVar)
					scrollDeltaVar -= consumed
				}
			}

			// delegated reverse (view)
			layoutChilds.asReversed().fastForEach { e ->
				when(e) {
					is ScrollableChildLayout -> rootTraversalConsumable(e.height + e.spacing, e::scrollTraversal)
					else -> rootConsumable(e.height + e.spacing)
				}
				if (reachedDeltaConstraint()) {
					consumeEndEffect()
					return consumed()
				}
			}
			check(reachedAnyConstraint()) {
				"constraint weren't reached, ini=$initialValue, delta=$scrollDelta, deltaVar=$scrollDeltaVar, consumable=${layoutConsumable()}"
			}
			consumeEndEffect()
			return consumed()
		}*/

		private fun applyConsumedScrollStrict(
			scroll: Float,
			direction: Int
		) {
			check(scroll >= 0) {
				"scroll cannot be negative"
			}
			if (scroll == 0f) return
			val layoutBottom = maxLength
			val viewportHeight = viewportSize.height
			val valueBound = 0..layoutBottom - viewportHeight
			val cv = value + scrollFloatAcc
			val new = cv + scroll * direction
			val newInt = new.roundToInt()
			check(newInt in valueBound) {
				"invalid scroll consume=$scroll, direction=$direction, value=$value"
			}
			value = newInt
			scrollFloatAcc = new - newInt
		}

		@OptIn(ExperimentalComposeApi::class)
		@UiThread
		private suspend fun onUserScrollFling(velocity: Velocity): Velocity {
			var avail = velocity

			coroutineScope {
				check(AndroidUiFoundation.isOnUiLooper())
				if (currentScroll?.isActive == true) return@coroutineScope
				currentFling?.cancel()
				val frameClock = coroutineContext[MonotonicFrameClock]
					?: AndroidUiDispatcher.Main.monotonicFrameClock
				currentFling = launch(frameClock) {
					val scope = object : ScrollScope {
						override fun scrollBy(pixels: Float): Float {
							return -doScrollTraversal(-pixels)
						}
					}
					with(scope) {
						with(flingBehavior) {
							avail = Velocity(avail.x, performFling(velocity.y))
						}
					}
				}
			}

			return avail
		}
	}
	open class ChildLayout(
		var height: Int,
		var spacing: Int
	) {

		fun update(height: Int, spacing: Int) {
			this.height = height
			this.spacing = spacing
		}

		fun update(layout: ChildLayout) {
			update(layout.height, layout.spacing)
		}
	}

	// do we really need this
	class ScrollableChildLayout(
		height: Int,
		spacing: Int,
	) : ChildLayout(height, spacing) {

		var scrollableScrollState: ScrollableState? = null

		fun scrollTraversal(pixels: Float): Float {
			println("NestedScrollableNode_scrollTraversal(pixels=$pixels)")
			return scrollableScrollState
				?.run {
					var acc = 0f
					// fixme
					acc += dispatchRawDelta(pixels)
					acc
				}
				?: 0f
		}
	}
}

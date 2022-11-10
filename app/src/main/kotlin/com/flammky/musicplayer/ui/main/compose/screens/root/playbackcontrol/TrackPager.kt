package com.flammky.musicplayer.ui.main.compose.screens.root.playbackcontrol

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxMetadata
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxViewModel
import com.google.accompanist.pager.*
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

@Composable
internal fun TrackDescriptionPager(
	modifier: Modifier,
	viewModel: PlaybackBoxViewModel,
	backgroundLuminance: State<Float>,
	onClick: () -> Unit
) {
	Box(modifier = modifier) {
		val streamFlowState = viewModel.playlistStreamStateFlow.collectAsState()
		TrackDescriptionHorizontalPager(
			index = streamFlowState.read().currentIndex,
			tracks = streamFlowState.read().list,
			viewModel = viewModel,
			backgroundLuminance = backgroundLuminance,
			onClick = onClick
		)
	}
}

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TrackDescriptionHorizontalPager(
	index: Int,
	tracks: List<String>,
	viewModel: PlaybackBoxViewModel,
	backgroundLuminance: State<Float>,
	onClick: () -> Unit
) {
	if (index < 0) return
	require(index < tracks.size) {
		"Playing Index($index) > size($tracks), must ensure playback info delivery is consistent on both"
	}

	val pagerState = rememberPagerState(index)

	val count = tracks.size

	val touched = remember { mutableStateOf(false) }
	val dragging by pagerState.interactionSource.collectIsDraggedAsState()

	LaunchedEffect(
		key1 = index
	) {
		if (!pagerState.isScrollInProgress) {
			touched.value = false
			pagerState.animateScrollToPage(page = index)
		}
	}

	LaunchedEffect(
		key1 = pagerState.currentPage,
		key2 = dragging
	) {
		if (!dragging) {
			if (touched.value &&
				pagerState.currentPage != index
			) {
				viewModel.seekIndex(pagerState.currentPage)
			}
		} else {
			touched.value = true
		}
	}

	// Fade Ends ?
	HorizontalPager(
		modifier = Modifier.fillMaxSize(),
		count = count,
		state = pagerState,
		flingBehavior = rememberFlingBehavior(pagerState)
	) { page: Int ->
		TrackDescriptionPagerItem(
			id = tracks[page],
			viewModel = viewModel, backgroundLuminance.read(),
			onClick = { onClick() }
		)
	}
}

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class)
@Composable
private fun rememberFlingBehavior(state: PagerState): FlingBehavior {
	val base = PagerDefaults.flingBehavior(state = state)
	return remember(state, base) {
		object : FlingBehavior {
			override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
				return with(base) { performFling(initialVelocity * 0.25f) }
			}
		}
	}
}

@Composable
private fun TrackDescriptionPagerItem(
	id: String,
	viewModel: PlaybackBoxViewModel,
	backgroundLuminance: Float,
	onClick: (String) -> Unit
) {
	require(backgroundLuminance in 0f..1f) {
		"BackgroundLuminance($backgroundLuminance) not in 0f..1f"
	}
	val metadataState = viewModel.observeBoxMetadata(id).collectAsState(initial = PlaybackBoxMetadata())

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(start = 10.dp, end = 10.dp)
			.clickable { onClick(id) }
		,
	) {
		val textColor = if (backgroundLuminance < 0.4f) Color.White else Color.Black
		val metadata = metadataState.read()

		BoxWithConstraints(
			modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
			contentAlignment = Alignment.CenterStart
		) {
			Text(
				text = metadata.title,
				fontSize = with(LocalDensity.current) { constraints.maxHeight.toSp() * 0.74f },
				fontWeight = FontWeight.Bold,
				color = textColor,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}

		BoxWithConstraints(
			modifier = Modifier.fillMaxWidth().fillMaxHeight(1f),
			contentAlignment = Alignment.CenterStart
		) {
			Text(
				text = metadata.subtitle,
				fontSize = with(LocalDensity.current) { constraints.maxHeight.toSp() * 0.7f },
				fontWeight = FontWeight.SemiBold,
				color = textColor,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}


private fun <T> State<T>.read(): T = value


private val NoPressGesture: suspend PressGestureScope.(Offset) -> Unit = { }

/**
 * Detects tap, double-tap, and long press gestures and calls [onTap], [onDoubleTap], and
 * [onLongPress], respectively, when detected. [onPress] is called when the press is detected
 * and the [PressGestureScope.tryAwaitRelease] and [PressGestureScope.awaitRelease] can be
 * used to detect when pointers have released or the gesture was canceled.
 * The first pointer down and final pointer up are consumed, and in the
 * case of long press, all changes after the long press is detected are consumed.
 *
 * When [onDoubleTap] is provided, the tap gesture is detected only after
 * the [ViewConfiguration.doubleTapMinTimeMillis] has passed and [onDoubleTap] is called if the
 * second tap is started before [ViewConfiguration.doubleTapTimeoutMillis]. If [onDoubleTap] is not
 * provided, then [onTap] is called when the pointer up has been received.
 *
 * If the first down event was consumed, the entire gesture will be skipped, including
 * [onPress]. If the first down event was not consumed, if any other gesture consumes the down or
 * up events, the pointer moves out of the input area, or the position change is consumed,
 * the gestures are considered canceled. [onDoubleTap], [onLongPress], and [onTap] will not be
 * called after a gesture has been canceled.
 */
suspend fun PointerInputScope.detectTapGestures(
	onDoubleTap: ((Offset) -> Unit)? = null,
	onLongPress: ((Offset) -> Unit)? = null,
	onPress: suspend PressGestureScope.(Offset) -> Unit = { NoPressGesture },
	onTap: ((Offset) -> Unit)? = null
) = coroutineScope {
	// special signal to indicate to the sending side that it shouldn't intercept and consume
	// cancel/up events as we're only require down events
	val pressScope = PressGestureScopeImpl(this@detectTapGestures)

	forEachGesture {
		awaitPointerEventScope {
			val down = awaitFirstDown()
			pressScope.reset()
			if (onPress !== NoPressGesture) launch {
				pressScope.onPress(down.position)
			}
			val longPressTimeout = onLongPress?.let {
				viewConfiguration.longPressTimeoutMillis
			} ?: (Long.MAX_VALUE / 2)
			var upOrCancel: PointerInputChange? = null
			try {
				// wait for first tap up or long press
				upOrCancel = withTimeout(longPressTimeout) {
					waitForUpOrCancellation()
				}
				if (upOrCancel == null) {
					pressScope.cancel() // tap-up was canceled
				} else {
					pressScope.release()
				}
			} catch (_: PointerEventTimeoutCancellationException) {
				onLongPress?.invoke(down.position)
				pressScope.release()
			}

			if (upOrCancel != null) {
				// tap was successful.
				if (onDoubleTap == null) {
					onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
				} else {
					// check for second tap
					val secondDown = awaitSecondDown(upOrCancel)

					if (secondDown == null) {
						onTap?.invoke(upOrCancel.position) // no valid second tap started
					} else {
						// Second tap down detected
						pressScope.reset()
						if (onPress !== NoPressGesture) {
							launch { pressScope.onPress(secondDown.position) }
						}

						try {
							// Might have a long second press as the second tap
							withTimeout(longPressTimeout) {
								val secondUp = waitForUpOrCancellation()
								if (secondUp != null) {
									pressScope.release()
									onDoubleTap(secondUp.position)
								} else {
									pressScope.cancel()
									onTap?.invoke(upOrCancel.position)
								}
							}
						} catch (e: PointerEventTimeoutCancellationException) {
							// The first tap was valid, but the second tap is a long press.
							// notify for the first tap
							onTap?.invoke(upOrCancel.position)

							// notify for the long press
							onLongPress?.invoke(secondDown.position)
							pressScope.release()
						}
					}
				}
			}
		}
	}
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
	do {
		val event = awaitPointerEvent()
		event.changes.forEach { it.consume() }
	} while (event.changes.any { it.pressed })
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a
 * second press event is received before the time out, it is returned or `null` is returned
 * if no second press is received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
	firstUp: PointerInputChange
): PointerInputChange? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
	val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
	var change: PointerInputChange
	// The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
	do {
		change = awaitFirstDown()
	} while (change.uptimeMillis < minUptime)
	change
}

/**
 * Shortcut for cases when we only need to get press/click logic, as for cases without long press
 * and double click we don't require channelling or any other complications.
 */
internal suspend fun PointerInputScope.detectTapAndPress(
	onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
	onTap: ((Offset) -> Unit)? = null
) {
	val pressScope = PressGestureScopeImpl(this)
	forEachGesture {
		coroutineScope {
			pressScope.reset()
			awaitPointerEventScope {

				val down = awaitFirstDown().also {  }

				if (onPress !== NoPressGesture) {
					launch { pressScope.onPress(down.position) }
				}

				val up = waitForUpOrCancellation()
				if (up == null) {
					pressScope.cancel() // tap-up was canceled
				} else {
					pressScope.release()
					onTap?.invoke(up.position)
				}
			}
		}
	}
}

/**
 * Reads events until the first down is received. If [requireUnconsumed] is `true` and the first
 * down is consumed in the [PointerEventPass.Main] pass, that gesture is ignored.
 */
suspend fun AwaitPointerEventScope.awaitFirstDown(
	requireUnconsumed: Boolean = true
): PointerInputChange =
	awaitFirstDownOnPass(pass = PointerEventPass.Main, requireUnconsumed = requireUnconsumed)

internal suspend fun AwaitPointerEventScope.awaitFirstDownOnPass(
	pass: PointerEventPass,
	requireUnconsumed: Boolean
): PointerInputChange {
	var event: PointerEvent
	do {
		event = awaitPointerEvent(pass)
	} while (
		!event.changes.all {
			if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
		}
	)
	return event.changes[0]
}

/**
 * Reads events until all pointers are up or the gesture was canceled. The gesture
 * is considered canceled when a pointer leaves the event region, a position change
 * has been consumed or a pointer down change event was consumed in the [PointerEventPass.Main]
 * pass. If the gesture was not canceled, the final up change is returned or `null` if the
 * event was canceled.
 */
suspend fun AwaitPointerEventScope.waitForUpOrCancellation(): PointerInputChange? {
	while (true) {
		val event = awaitPointerEvent(PointerEventPass.Main)
		if (event.changes.all { it.changedToUp() }) {
			// All pointers are up
			return event.changes[0]
		}

		if (event.changes.any {
				it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
			}
		) {
			return null // Canceled
		}

		// Check for cancel by position consumption. We can look on the Final pass of the
		// existing pointer event because it comes after the Main pass we checked above.
		val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
		if (consumeCheck.changes.any { it.isConsumed }) {
			return null
		}
	}
}

/**
 * [detectTapGestures]'s implementation of [PressGestureScope].
 */
private class PressGestureScopeImpl(
	density: Density
) : PressGestureScope, Density by density {
	private var isReleased = false
	private var isCanceled = false
	private val mutex = Mutex(locked = false)

	/**
	 * Called when a gesture has been canceled.
	 */
	fun cancel() {
		isCanceled = true
		mutex.unlock()
	}

	/**
	 * Called when all pointers are up.
	 */
	fun release() {
		isReleased = true
		mutex.unlock()
	}

	/**
	 * Called when a new gesture has started.
	 */
	fun reset() {
		mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
		isReleased = false
		isCanceled = false
	}

	override suspend fun awaitRelease() {
		if (!tryAwaitRelease()) {
			throw GestureCancellationException("The press gesture was canceled.")
		}
	}

	override suspend fun tryAwaitRelease(): Boolean {
		if (!isReleased && !isCanceled) {
			mutex.lock()
		}
		return isReleased
	}
}

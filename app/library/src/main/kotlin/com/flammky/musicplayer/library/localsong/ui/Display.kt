package com.flammky.musicplayer.library.localsong.ui

import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil.compose.AsyncImage
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

@Composable
internal fun LocalSongDisplay(
	modifier: Modifier = Modifier,
	viewModel: LocalSongViewModel,
	navigate: (String) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.then(modifier)
	) {
		DisplayHeader()
		DisplayContent(
			list = viewModel.listState.read(),
			navigate = navigate
		)
	}
}

@Composable
private fun DisplayHeader(
	textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
) {
	Text(text = "Device Songs")
}

@Composable
private fun DisplayContent(
	modifier: Modifier = Modifier,
	list: List<LocalSongModel>,
	navigate: (String) -> Unit
) {
	val density = LocalDensity.current
	val maxWidthState = remember { mutableStateOf<Dp?>(null) }
	val maxHeightState = remember { mutableStateOf<Dp?>(null) }
	Box(
		modifier = Modifier
			.fillMaxSize()
			.then(modifier)
			.onGloballyPositioned { bounds: LayoutCoordinates ->
				maxWidthState.overwrite(bounds.size.width.toDp(density))
				maxHeightState.overwrite(bounds.size.height.toDp(density))
			}
	)
	val maxWidth = maxWidthState.read()
	val maxHeight = maxHeightState.read()
	if (maxWidth != null && maxHeight != null) {
		DynamicDisplayContent(maxWidth = maxWidth, maxHeight = maxHeight, list = list, navigate)
	}
}

// Maybe call it LimitedFlowRow or something

@Composable
private fun DynamicDisplayContent(
	@FloatRange(25.0, Double.MAX_VALUE) maxWidth: Dp,
	@FloatRange(25.0, Double.MAX_VALUE)maxHeight: Dp,
	list: List<LocalSongModel>,
	navigate: (String) -> Unit
) {
	// 10 required by spacer, 15 required by minimum of 3 items
	if (maxWidth.value < 25 || maxHeight.value < 25) {
		error("Available Size Too Small")
	}
	val density = LocalDensity.current
	val maxWidthPx = maxWidth.toPx(density)
	val maxHeightPx = maxHeight.toPx(density)
	val rowContentStartSpacer = 5.dp
	val rowContentEndSpacer = 5.dp
	val rowContentTopSpacer = 5.dp
	val rowContentBottomSpacer = 5.dp

	// Ideally we want the child size to be 100 x 100
	// with at least 3 child on each row on vertical display orientation
	// now we should think about whether we increase spacer size or the item size on `too small`
	// case but not enough space for another Item

	val minChildAmount = 3
	val minRow = 1
	val maxRow = 2

	val childWidth = 100.dp
	val childHeight = 100.dp
	val childHorizontalSpacer = 10.dp

	val rowVerticalSpacer = rowContentTopSpacer + rowContentBottomSpacer
	val rowHorizontalSpacer = rowContentStartSpacer + rowContentEndSpacer
	val rowHeight = min(maxHeight, childHeight + rowVerticalSpacer)
	val rowWidth = maxWidth
	val rowContentHeight = rowHeight - rowHorizontalSpacer
	val rowContentWidth = rowWidth - rowHorizontalSpacer
	val rowSpacer = 15.dp

	var fixedChildWidth = childWidth
	var fixedChildHeight = childHeight

	fun isHorizontalChildAmountPossible(amount: Int): Boolean {
		return (rowContentWidth / (fixedChildWidth * amount + (childHorizontalSpacer * ceil(amount.toFloat() / 2)))).toInt() > 0
	}

	fun isVerticalChildAmountPossible(amount: Int): Boolean {
		return (rowContentHeight / (fixedChildHeight * amount)).toInt() > 0
	}

	fun possibleHorizontalChildAmount(): Int {
		var possible = 0
		while (isHorizontalChildAmountPossible(possible + 1)) {
			possible++
		}
		return possible
	}

	fun possibleVerticalChildAmount(): Int {
		var possible = 0
		while (isVerticalChildAmountPossible(possible + 1)) {
			possible++
		}
		return possible
	}

	while (possibleHorizontalChildAmount() < minChildAmount) {
		fixedChildWidth -= 1.dp
	}
	while (possibleVerticalChildAmount() < minRow) {
		fixedChildHeight -= 1.dp
	}

	val fixedChildSize = min(fixedChildWidth, fixedChildHeight)
	val fixedRowHeight = fixedChildSize + rowContentTopSpacer + rowContentBottomSpacer
	val fixedRowWidth = possibleHorizontalChildAmount()

	fun isRowAmountPossible(amount: Int): Boolean {
		val spacer = rowSpacer * ceil(amount.toFloat() / 2)
		return (maxHeight / (fixedRowHeight * amount + spacer)).toInt() > 0
	}

	fun possibleRowAmount(): Int {
		var possible = 0
		while (possible < maxRow && isRowAmountPossible(possible + 1)) {
			possible++
		}
		return possible
	}

	val possibleRowAmount = possibleRowAmount()

	repeat(possibleRowAmount) { index ->
		val lastRow = index == possibleRowAmount - 1
		val items =
			if (lastRow) {
				list.drop(min(list.size, 3))
			} else {
				list.take(min(list.size, 3))
			}
		DisplayContentRow(
			modifier = Modifier
				.width(rowWidth)
				.height(fixedRowHeight)
				.padding(
					start = rowContentStartSpacer,
					end = rowContentEndSpacer,
					top = rowContentTopSpacer,
					bottom = rowContentBottomSpacer
				),
			horizontalArrangement = Arrangement.spacedBy(childHorizontalSpacer),
			verticalAlignment = Alignment.CenterVertically,
			isLastRow = index == 1,
			items = items
		)
	}
}

@Composable
private fun DisplayContentRow(
	modifier: Modifier,
	horizontalArrangement: Arrangement.Horizontal,
	verticalAlignment: Alignment.Vertical,
	isLastRow: Boolean,
	items: List<LocalSongModel>
) {
	Row(
		modifier = remember(modifier) {
			Modifier
				.fillMaxWidth()
				.then(modifier)
		},
		horizontalArrangement = horizontalArrangement,
		verticalAlignment = verticalAlignment
	) {
		if (!isLastRow) {
			repeat(items.size) { i ->
				DisplayContentItem(items[i])
			}
		} else {
			repeat(items.size) { i ->
				if (i != items.lastIndex) {
					DisplayContentItem(items[i])
				} else {
					DisplayLastContentItem(items = items)
				}
			}
		}
	}
}

private object UNSET
private object LOADING

@Composable
private fun DisplayContentItem(
	item: LocalSongModel
) {
	val viewModel: LocalSongViewModel = activityViewModel()
	val model = remember { mutableStateOf<Any?>(null) }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color(0xFFC2C2C2))
	) {
		AsyncImage(
			modifier = Modifier.fillMaxSize(),
			model = model,
			contentDescription = "art"
		)
	}

	val scope = rememberCoroutineScope { SupervisorJob() }
	DisposableEffect(key1 = item) {
		val job = scope.launch {
			viewModel.collectArtwork(item).safeCollect { art -> model.overwrite(art) }
		}
		onDispose { job.cancel() }
	}
}

@Composable
private fun DisplayLastContentItem(
	items: List<LocalSongModel>
) {

}

@Px
private fun Dp.toPx(density: Density): Int {
	return with(density) { roundToPx() }
}

private fun Int.toDp(density: Density): Dp {
	return with(density) { toDp() }
}

// is explicit read like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> State<T>.read(): T {
	return value
}

// is explicit write like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> MutableState<T>.overwrite(value: T) {
	this.value = value
}

// is explicit write like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> MutableState<T>.rewrite(block: () -> T) {
	this.value = block()
}

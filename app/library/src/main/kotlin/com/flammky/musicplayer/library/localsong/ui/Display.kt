package com.flammky.musicplayer.library.localsong.ui

import android.graphics.Bitmap
import androidx.annotation.Px
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil.compose.AsyncImage
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.compose.rememberContextHelper
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
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
			.then(modifier),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		DisplayHeader()
		DisplayContent(
			list = viewModel.listState.read(),
			onItemClicked = { viewModel.play(it) },
			navigate = navigate
		)
	}
}

@Composable
private fun DisplayHeader(
	textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
) {
	Text(text = "Device Songs", style = textStyle)
}

@Composable
private fun DisplayContent(
	modifier: Modifier = Modifier,
	list: List<LocalSongModel>,
	onItemClicked: (item: LocalSongModel) -> Unit,
	navigate: (String) -> Unit
) {
	BoxWithConstraints(
		modifier = Modifier
			.fillMaxSize()
			.then(modifier)
	) {
		DynamicDisplayContent(
			maxWidth = this.maxWidth,
			maxHeight = this.maxHeight,
			horizontalSpacer = 10.dp,
			verticalSpacer= 10.dp,
			maximumRow = 2,
			list = list,
			onItemClicked = onItemClicked,
			navigate = navigate
		)
	}
}

// Maybe call it LimitedFlowRow or something

@Composable
private fun DynamicDisplayContent(
	list: List<LocalSongModel>,
	minWidth: Dp = 0.dp,
	maxWidth: Dp = Dp.Infinity,
	minHeight: Dp = 0.dp,
	maxHeight: Dp = Dp.Infinity,
	minChildWidth: Dp = 0.dp,
	maxChildWidth: Dp = maxWidth,
	minChildHeight: Dp = 0.dp,
	maxChildHeight: Dp = maxHeight,
	horizontalSpacer: Dp = 0.dp,
	verticalSpacer: Dp = 0.dp,
	maximumRow: Int,
	onItemClicked: (item: LocalSongModel) -> Unit,
	navigate: (String) -> Unit
) {
	require(minWidth.value >= 0) {
		"minWidth < 0"
	}
	require(minHeight.value >= 0) {
		"minHeight < 0"
	}
	require(minChildWidth.value >= 0) {
		"minChildWidth < 0"
	}
	require(minChildHeight.value >= 0) {
		"minChildHeight < 0"
	}
	require(minChildWidth >= minWidth) {
		"minChildWidth < minWidth"
	}
	require(minChildHeight >= minHeight) {
		"minChildHeight < minHeight"
	}
	require(maxChildWidth <= maxWidth) {
		"maxChildWidth > maxWidth"
	}
	require(maxChildHeight <= maxHeight) {
		"maxChildHeight > maxHeight"
	}
	when(list.size) {
		0 -> return // show no local song available
		1, 2, 3 -> Unit // show static layout
	}
	val contextHelper = rememberContextHelper()
	val constrainedMaxWidth = min(contextHelper.device.screenWidthDp.dp, maxWidth)
	val constrainedMaxHeight = min(contextHelper.device.screenHeightDp.dp, maxHeight)
	val constrainedMaxChildWidth = min(maxChildWidth, constrainedMaxWidth)
	val constrainedMaxChildHeight = min(maxChildHeight, constrainedMaxHeight)
	val landscape = contextHelper.configurations.isOrientationLandscape()

	fun contentSpacerAmount(childAmount: Int): Int {
		return if (childAmount <= 0) 0 else childAmount - 1
	}

	fun calculateContentSpacer(amount: Int): Dp {
		return (horizontalSpacer * contentSpacerAmount(amount))
			.also { Timber.d("DynamicDisplayContent calculateChildRowSpacer $amount, $it") }
	}

	fun calculateRowSpacer(amount: Int): Dp {
		return (horizontalSpacer * if (amount <= 0) 0 else amount - 1)
	}

	fun possibleRowAmount(height: Dp): Int = when {
		height > maxHeight -> 0
		height > maxHeight / 2 -> 1
		else -> {
			val raw = maxHeight / height
			val spacer = calculateRowSpacer(raw.toInt())
			((maxHeight - spacer) / height).toInt()
		}
	}.also {
		Timber.d("possibleRowAmount, $maxHeight, $height, $it")
	}.clamp(0, maximumRow)

	fun placeableHorizontalChild(width: Dp): Int {
		/*return (maxWidth / width).toInt()*/

		val raw = (maxWidth / width).toInt()
		val spacer = calculateContentSpacer(raw)
		return ((maxWidth - spacer) / width).toInt().also {
			Timber.d("DynamicDisplayContent possibleHorizontalChilds $it, $maxWidth, $width, $raw, $spacer")
		}
	}

	fun placeableVerticalChild(height: Dp): Int {
		return if (height > maxHeight) 0 else 1
	}

	fun isChildPlaceableHorizontally(amount: Int, width: Dp): Boolean {
		return placeableHorizontalChild(width) >= amount
	}

	fun horizontalContentLeftover(
		childWidth: Dp,
		childAmount: Int = placeableHorizontalChild(childWidth),
	): Dp {
		val spacer = calculateContentSpacer(childAmount)
		return (maxWidth - (childWidth * childAmount + spacer)).also {
			Timber.d("DynamicDisplayContent horizontalLeftover $maxWidth, $childAmount, $childWidth, $spacer, $it")
		}
	}

	fun verticalContentLeftover(childHeight: Dp): Dp {
		val possibleRow = possibleRowAmount(childHeight)
		return maxHeight - calculateRowSpacer(possibleRow) / possibleRow - childHeight
	}

	// the size we ideally want.
	val idealChildSize = min(
		maxHeight / 2 - verticalSpacer,
		maxWidth / 3 - calculateContentSpacer(3)
	)

	val vh =
		if (landscape) {
			maxHeight / 2 - calculateRowSpacer(2)
		} else {
			maxWidth / 3 - calculateContentSpacer(3)
		}.clamp(minChildHeight, maxChildHeight)

	val minimumChildSize = vh
	val maximumChildSize = minOf(maxWidth, maxHeight)

	var childSize: Dp = idealChildSize

	fun currentHorizontalContentLeftover(): Dp {
		return horizontalContentLeftover(childSize)
	}

	fun currentVerticalContentLeftover(): Dp {
		return verticalContentLeftover(childSize)
	}

	fun currentPlaceableHorizontalChild(): Int {
		return placeableHorizontalChild(childSize)
	}

	fun currentPlaceableVerticalChild(): Int {
		return placeableVerticalChild(childSize)
	}

	fun minimumSizeSatisfied(): Boolean = childSize > minimumChildSize

	fun expandConstrained() {
		val a = currentVerticalContentLeftover() / max(currentPlaceableVerticalChild(), 1)
		val b = currentHorizontalContentLeftover() / max(currentPlaceableHorizontalChild(), 1)

		childSize += minOf(
			currentVerticalContentLeftover() / max(currentPlaceableVerticalChild(), 1),
			currentHorizontalContentLeftover() / max(currentPlaceableHorizontalChild(), 1)
		).also {
			Timber.d("expand constrained to $a $b $it")
		}
	}

	// expand the content on horizontal axis
	fun expandHorizontally(constraintHeight: Boolean = true) {
		if (constraintHeight) {
			expandConstrained()
		} else {
			childSize += currentHorizontalContentLeftover() / max(currentPlaceableHorizontalChild(), 1)
		}
	}

	// expand the content on vertical axis
	fun expandVertically(constraintWidth: Boolean = true) {
		if (constraintWidth) {
			expandConstrained()
		} else {
			childSize += currentVerticalContentLeftover() / max(currentPlaceableVerticalChild(), 1)
		}
	}

	fun reduceHorizontalChild(amount: Int = 1) {
		val calc = maxWidth / max(placeableHorizontalChild(childSize) - amount, 1)
		if (calc <= maxHeight) {
			childSize = calc
			expandConstrained()
		} else {
			childSize = calc.clamp(0.dp, childSize + currentVerticalContentLeftover())
			expandConstrained()
		}
		Timber.d("reduceHorizontalChild to $calc, expand to $childSize, possible: ${placeableHorizontalChild(childSize)}")
	}

	fun possibleChildWidthForAmount(amount: Int): Dp {
		return ((maxWidth - calculateContentSpacer(amount)) / amount)
	}

	fun increaseHorizontalChild(amount: Int = 1) {
		val currentPlaceable = currentPlaceableHorizontalChild()
		childSize = possibleChildWidthForAmount(currentPlaceable + amount)
		expandConstrained()

		Timber.d("increaseHorizontalChild")
	}

	expandHorizontally()
	expandVertically()

	currentPlaceableHorizontalChild().let { amount ->
		if (amount < 3) increaseHorizontalChild(3 - amount)
	}

	Timber.d(
		"""
			leftover: ${currentHorizontalContentLeftover()}, ${currentVerticalContentLeftover()}
			childWidth: $childSize
			childHeight: $childSize
			maxWidth: $maxWidth
			maxHeight: $maxHeight
			"""
		)

	Column(
		modifier = Modifier.clip(RoundedCornerShape(5.dp)),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(verticalSpacer)
	) {
		val placeableRow = possibleRowAmount(childSize)
		val placeable = placeableHorizontalChild(childSize)
		var taken = 0
		repeat(placeableRow) { index ->
			val lastRow = index == placeableRow - 1
			val items =
				if (lastRow) {
					list.drop(taken)
				} else {
					list.drop(taken).take(min(list.size, placeable)).also { taken += it.size }
				}
			DisplayContentRow(
				modifier = Modifier
					.width(maxWidth)
					.height(childSize)
					/*.padding(
						start = contentStartPadding,
						end = contentEndPadding,
						top = contentTopPadding,
						bottom = contentBottomPadding
					)*/,
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
				isLastRow = lastRow,
				lastIndex = min(items.lastIndex, placeable - 1),
				items = items,
				onItemClicked = onItemClicked,
				navigateSongList = { navigate(LocalSongNavigator.localSongListRoute) }
			)
		}
	}
}

@Composable
private fun DisplayContentRow(
	modifier: Modifier,
	horizontalArrangement: Arrangement.Horizontal,
	verticalAlignment: Alignment.Vertical,
	isLastRow: Boolean,
	lastIndex: Int,
	items: List<LocalSongModel>,
	onItemClicked: (LocalSongModel) -> Unit,
	navigateSongList: () -> Unit
) {

	if (items.isEmpty()) return

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.then(modifier),
		horizontalArrangement = horizontalArrangement,
		verticalAlignment = verticalAlignment
	) {
		val repeat = lastIndex + 1
		if (!isLastRow) {
			repeat(repeat) { i ->
				DisplayContentItem(items[i], onItemClicked)
			}
		} else {
			repeat(repeat) { i ->
				if (i != lastIndex || items.lastIndex == lastIndex) {
					DisplayContentItem(items[i], onItemClicked)
				} else {
					DisplayLastContentItem(items = items.drop(i), navigateSongList)
				}
			}
		}
	}
}

private val UNSET = Any()
private val LOADING = Any()

@Composable
private fun DisplayContentItem(
	item: LocalSongModel,
	onItemClicked: (item: LocalSongModel) -> Unit
) {
	val viewModel: LocalSongViewModel = activityViewModel()
	val model = remember { mutableStateOf<Any?>(UNSET) }

	Box(
		modifier = Modifier
			.fillMaxHeight()
			.aspectRatio(1f, true)
			.background(Color(0xFFC2C2C2))
			.clip(RoundedCornerShape(5.dp))
			.clickable { onItemClicked(item) }
	) {
		AsyncImage(
			modifier = Modifier
				.fillMaxSize()
				.placeholder(
					visible = model.value == LOADING,
					color = Color(0xFFA0A0A0),
					highlight = PlaceholderHighlight.shimmer(Color.DarkGray)
				),
			model = model.read(),
			contentDescription = "art",
			contentScale = ContentScale.Crop
		)
		DisplayContentItemShadowedDescription(
			modifier = Modifier.align(Alignment.BottomCenter),
			text = item.displayName ?: ""
		)
	}

	val scope = rememberCoroutineScope()
	val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()
	DisposableEffect(key1 = item) {
		val job = scope.launch(coroutineContext) {
			model.overwrite(LOADING)
			viewModel.collectArtwork(item).safeCollect { art ->
				val value = model.value
				if (art is Bitmap && value is Bitmap && art.sameAs(value)) return@safeCollect
				model.overwrite(art)
			}
		}
		onDispose { job.cancel() }
	}
}

@Composable
private fun DisplayContentItemShadowedDescription(
	modifier: Modifier,
	text: String
) {
	val alpha = 0.75F
	val shadowColor = Color.Black
	val textColor = Color.White
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.background(
				brush = Brush.verticalGradient(
					colors = remember {
						listOf(
							shadowColor.copy(alpha = alpha - 0.50f),
							shadowColor.copy(alpha = alpha - 0.40f),
							shadowColor.copy(alpha = alpha - 0.30f),
							shadowColor.copy(alpha = alpha - 0.20f),
							shadowColor.copy(alpha = alpha - 0.10f),
							shadowColor.copy(alpha = alpha)
						)
					}
				)
			)
			.then(modifier),
		contentAlignment = Alignment.BottomCenter,
	) {
		Column {
			val typography = MaterialTheme.typography.labelMedium
			Spacer(modifier = Modifier.height(2.dp))
			Text(
				modifier = Modifier.fillMaxWidth(0.85f),
				text = text,
				color = textColor,
				style = typography,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Spacer(modifier = Modifier.height(2.dp))
		}
	}
}

@Composable
private fun DisplayLastContentItem(
	items: List<LocalSongModel>,
	navigate: () -> Unit
) {
	Box(
		modifier = Modifier
			.fillMaxHeight()
			.aspectRatio(1f, true)
			.background(Color(0xFFC2C2C2))
			.clip(RoundedCornerShape(5.dp))
			.clickable(onClick = navigate)
	) {
		Column {
			var i = 0
			repeat(min(2, items.size)) { rowIndex ->
				i += rowIndex
				val f = if (rowIndex == 0) 0.5f else 1f
				Row(modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight(f)
				) {
					repeat(min(2, items.drop(i).size)) { itemIndex ->
						i += itemIndex
						val model = items[i]
						DisplayLastContentItemChild(model)
					}
				}
			}
		}
		val shadowColor = Color.Black
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(shadowColor.copy(alpha = 0.7f))
				.padding(bottom = 2.dp)
		) {
			val textColor = Color.White
			Text(
				modifier = Modifier
					.align(Alignment.Center)
					.widthIn(max = 96.dp)
					.padding(horizontal = 2.dp),
				text = "${items.size}",
				color = textColor,
				style = MaterialTheme.typography.titleMedium,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
			val typography = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
			Text(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth(0.9f)
					.heightIn(20.dp),
				text = "See more",
				color = textColor,
				style = typography,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Composable
private fun DisplayLastContentItemChild(
	model: LocalSongModel
) {
	val viewModel: LocalSongViewModel = activityViewModel()
	val imageModel = remember { mutableStateOf<Any?>(UNSET) }

	AsyncImage(
		modifier = Modifier
			.fillMaxHeight()
			.aspectRatio(1f, true)
			.placeholder(
				visible = imageModel.value === LOADING,
				color = Color(0xFFA0A0A0)
			),
		model = imageModel.read(),
		contentDescription = "art",
		contentScale = ContentScale.Crop
	)

	val scope = rememberCoroutineScope()
	val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()
	DisposableEffect(key1 = model) {
		val job = scope.launch(coroutineContext) {
			imageModel.overwrite(LOADING)
			viewModel.collectArtwork(model).safeCollect { art ->
				val value = imageModel.value
				if (art is Bitmap && value is Bitmap && art.sameAs(value)) return@safeCollect
				imageModel.overwrite(art)
			}
		}
		onDispose { job.cancel() }
	}
}

@Px
private fun Dp.toPx(density: Density): Int {
	return with(density) { roundToPx() }
}

private fun Int.toDp(density: Density): Dp {
	return with(density) { toDp() }
}

// is explicit read like this better ?
private fun <T> State<T>.read(): T {
	return value
}

// is explicit write like this better ?
private fun <T> MutableState<T>.overwrite(value: T) {
	this.value = value
}

// is explicit write like this better ?
private fun <T> MutableState<T>.rewrite(block: (T) -> T) {
	this.value = block(this.value)
}

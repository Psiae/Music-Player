package com.flammky.musicplayer.library.localsong.ui

import android.graphics.Bitmap
import androidx.annotation.FloatRange
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
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.flammky.androidx.viewmodel.compose.activityViewModel
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
			.then(modifier)
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
			list = list,
			onItemClicked = onItemClicked,
			navigate = navigate
		)
	}
}

// Maybe call it LimitedFlowRow or something

@Composable
private fun DynamicDisplayContent(
	@FloatRange(15.0, Double.MAX_VALUE) maxWidth: Dp,
	@FloatRange(15.0, Double.MAX_VALUE) maxHeight: Dp,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	list: List<LocalSongModel>,
	onItemClicked: (item: LocalSongModel) -> Unit,
	navigate: (String) -> Unit
) {

	Timber.d("maxWidth: $maxWidth")

	if (list.isEmpty()) return // show no local song available

	val contextHelper = rememberContextHelper()

	val landscape = contextHelper.configurations.isOrientationLandscape()

	val rowSpacer = 5.dp
	val rowMax = 2
	val rowWidth = maxWidth
	val rowHeight = maxHeight / rowMax

	// ignore them for now
	val contentStartPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr)
	val contentEndPadding = contentPadding.calculateEndPadding(LayoutDirection.Ltr)
	val contentTopPadding = contentPadding.calculateTopPadding()
	val contentBottomPadding = contentPadding.calculateBottomPadding()
	val contentHorizontalPadding = contentStartPadding + contentEndPadding
	val contentVerticalPadding = contentTopPadding + contentBottomPadding

	val maxContentPadding = maxOf(
		contentStartPadding,
		contentEndPadding,
		contentTopPadding,
		contentBottomPadding
	)

	val childHorizontalSpacer = 10.dp

	fun contentSpacerAmount(childAmount: Int): Int {
		return if (childAmount <= 0) 0 else childAmount - 1
	}

	fun calculateContentSpacer(amount: Int): Dp {
		return (childHorizontalSpacer * contentSpacerAmount(amount))
			.also { Timber.d("DynamicDisplayContent calculateChildRowSpacer $amount, $it") }
	}

	fun calculateRowSpacer(amount: Int): Dp {
		return (rowSpacer * if (amount <= 0) 0 else amount - 1)
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
	}


	fun possibleHorizontalChilds(width: Dp): Int {
		/*return (maxWidth / width).toInt()*/

		val raw = (maxWidth / width).toInt()
		val spacer = calculateContentSpacer(raw)
		return ((maxWidth - spacer) / width).toInt().also {
			Timber.d("DynamicDisplayContent possibleHorizontalChilds $it, $maxWidth, $width, $raw, $spacer")
		}
	}

	fun possibleVerticalChilds(height: Dp): Int {
		return if (height > maxHeight) 0 else 1
	}

	fun horizontalContentLeftover(
		childWidth: Dp,
		childAmount: Int = possibleHorizontalChilds(childWidth),
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
		maxHeight / 2 - rowSpacer,
		maxWidth / 3 - calculateContentSpacer(3)
	)

	val vh =
		if (landscape) {
			maxHeight / 2 - calculateRowSpacer(2)
		} else {
			maxWidth / 3 - calculateContentSpacer(3)
		}

	val minimumChildSize = vh

	val idealRowAmount = when(list.size) {
		0 -> return
		in 1..possibleHorizontalChilds(idealChildSize) -> 1
		else -> if (idealChildSize + rowSpacer > maxHeight / 2) 1 else 2
	}

	require(idealChildSize.value > 0f) {
		"Size <= 0"
	}

	var childSize: Dp = idealChildSize

	fun currentHorizontalContentLeftover(): Dp {
		return horizontalContentLeftover(childSize)
	}

	fun currentVerticalContentLeftover(): Dp {
		return verticalContentLeftover(childSize)
	}

	fun currentPossibleHorizontalChild(): Int {
		return possibleHorizontalChilds(childSize)
	}

	fun currentPossibleVerticalChild(): Int {
		return possibleVerticalChilds(childSize)
	}

	fun minimumSizeSatisfied(): Boolean = childSize > minimumChildSize

	fun expandConstrained() {
		childSize += minOf(
			currentVerticalContentLeftover() / max(currentPossibleVerticalChild(), 1),
			currentHorizontalContentLeftover() / max(currentPossibleHorizontalChild(), 1)
		)
	}

	// expand the content on horizontal axis
	fun expandHorizontally(constraintHeight: Boolean = true) {
		if (constraintHeight) {
			expandConstrained()
		} else {
			childSize += currentHorizontalContentLeftover() / max(currentPossibleHorizontalChild(), 1)
		}
	}

	// expand the content on vertical axis
	fun expandVertically(constraintWidth: Boolean = true) {
		if (constraintWidth) {
			expandConstrained()
		} else {
			childSize += currentVerticalContentLeftover() / max(currentPossibleVerticalChild(), 1)
		}
	}

	fun shrinkHorizontally(amount: Int = 1) {
		childSize = maxWidth / (possibleHorizontalChilds(childSize) - amount)
		expandHorizontally()
		Timber.d("shrinkHorizontally to $childSize, ")
	}

	if (currentHorizontalContentLeftover() > 0.dp) expandHorizontally()
	if (currentVerticalContentLeftover() > 0.dp) expandVertically()
	while (!minimumSizeSatisfied()) shrinkHorizontally()

	Column(
		modifier = Modifier.clip(RoundedCornerShape(5.dp)),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(rowSpacer)
	) {
		val possibleRowAmount = possibleRowAmount(childSize)
		val possibleChildAmount = possibleHorizontalChilds(childSize)
		var taken = 0
		repeat(possibleRowAmount) { index ->
			val lastRow = index == possibleRowAmount - 1
			val items =
				if (lastRow) {
					list.drop(taken)
				} else {
					list.drop(taken).take(min(list.size, possibleChildAmount)).also { taken += it.size }
				}
			DisplayContentRow(
				modifier = Modifier
					.width(rowWidth)
					.height(childSize)
					/*.padding(
						start = contentStartPadding,
						end = contentEndPadding,
						top = contentTopPadding,
						bottom = contentBottomPadding
					)*/,
				horizontalArrangement = Arrangement.spacedBy(childHorizontalSpacer),
				verticalAlignment = Alignment.CenterVertically,
				isLastRow = lastRow,
				lastIndex = min(items.lastIndex, possibleChildAmount - 1),
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
				.background(shadowColor.copy(alpha = 0.8f))
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

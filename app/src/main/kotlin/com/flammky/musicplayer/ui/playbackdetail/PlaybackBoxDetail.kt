package com.flammky.musicplayer.ui.playbackdetail

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.android.medialib.player.Player
import com.flammky.androidx.content.context.findActivity
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.common.kotlin.comparable.clampPositive
import com.flammky.musicplayer.R
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.ui.Theme
import com.flammky.musicplayer.ui.util.compose.NoRipple
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
internal fun PlaybackBoxDetail(
	showSelf: State<Boolean>,
	viewModel: PlaybackDetailViewModel,
	dismiss: () -> Unit
) {

	// observe whether we should show ourselves
	val visibility = if (showSelf.read()) Visibility.VISIBLE else Visibility.GONE

	if (visibility.isVisible) {
		BackHandler(onBack = dismiss)
		// Consider Implementing landscape view
		LockScreenOrientation(landscape = false)
	}

	BoxWithConstraints(
		modifier = Modifier,
		contentAlignment = Alignment.BottomCenter,
	) {

		// Ensure that background contents are not clickable during transition
		NoRipple {
			Box(
				modifier = Modifier
					.fillMaxSize(fraction = if (visibility.isVisible) 1f else 0f)
					.clickable { }
			)
		}

		val animatedHeight = updateTransition(targetState = visibility, label = "")
			.animateDp(
				label = "",
				transitionSpec = {
					tween(
						durationMillis = 200,
						easing = if (visibility.isVisible) FastOutSlowInEasing else LinearOutSlowInEasing
					)
				}
			) { visibility ->
				if (visibility.isVisible) maxHeight else 0.dp
			}

		PlaybackBoxDetailTransition(
			target = maxHeight,
			heightState = animatedHeight,
			viewModel,
			dismiss
		)
	}
}

@Composable
private fun PlaybackBoxDetailTransition(
	target: Dp,
	heightState: State<Dp>,
	viewModel: PlaybackDetailViewModel,
	dismiss: () -> Unit
) {
	val transitioned = remember { mutableStateOf(false) }

	Box(
		modifier = Modifier
			.height(heightState.read())
			.fillMaxWidth()
			.background(
				Theme
					.dayNightAbsoluteColor()
					.copy(alpha = 0.9f)
			)
	) {
		if (heightState.read() == target) {
			transitioned.overwrite(true)
		} else if (heightState.read() == 0.dp && transitioned.read()) {
			transitioned.overwrite(false)
		}
		if (transitioned.read()) {
			PlaybackBoxDetails(viewModel, dismiss)
		}
	}
}

@Composable
private fun PlaybackBoxDetails(
	viewModel: PlaybackDetailViewModel,
	dismiss: () -> Unit
) {
	// Change to Guard until we implement horizontal orientation
	AssertVerticalOrientation()
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(Theme.backgroundColor())
			.statusBarsPadding()
	) {
		NoRipple {
			Spacer(modifier = Modifier.height(10.dp))
			DetailToolbar(dismiss)
			Spacer(modifier = Modifier.height(20.dp))
			DetailsContent(viewModel)
		}
	}
}

@Composable
private fun DetailToolbar(
	dismiss: () -> Unit
) {
	Column(modifier = Modifier
		.height(50.dp)
		.padding(vertical = 5.dp)) {
		Row(modifier = Modifier
			.height(40.dp)
			.fillMaxWidth()
			.padding(horizontal = 15.dp, vertical = 5.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			DismissAction(dismiss)
			Spacer(modifier = Modifier.weight(2f))
			Icon(
				modifier = Modifier.size(26.dp),
				painter = painterResource(id = R.drawable.more_vert_48px),
				contentDescription = "more",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}
	}
}

@Composable
private fun RowScope.DismissAction(
	dismiss: () -> Unit
) {
	val interactionSource = remember { MutableInteractionSource() }
	val size = if (interactionSource.collectIsPressedAsState().read()) 24.dp else 26.dp
	Box(
		modifier = Modifier
			.size(35.dp)
			.align(Alignment.CenterVertically)
			.clickable(interactionSource, null, onClick = dismiss)
	) {
		Icon(
			modifier = Modifier
				.align(Alignment.Center)
				.size(size),
			painter = painterResource(id = R.drawable.ios_glyph_expand_arrow_down_100),
			contentDescription = "close",
			tint = Theme.dayNightAbsoluteContentColor()
		)
	}
}



@Composable
private fun DetailsContent(
	viewModel: PlaybackDetailViewModel
) {
	BoxWithConstraints {
		val maxHeight = maxHeight
		Column(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.Top,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			val positionState = viewModel.positionStreamStateFlow.collectAsState()
			val trackState = viewModel.trackStreamStateFlow.collectAsState()
			TracksPagerDisplay(viewModel = viewModel)
			Spacer(modifier = Modifier.height(15.dp))
			CurrentTrackPlaybackDescriptions(viewModel = viewModel)
			Spacer(modifier = Modifier.height(15.dp))
			PlaybackControlProgressSeekbar(
				positionState = positionState,
				trackState = trackState,
				seek = { requestIndex, requestPosition -> viewModel.seek(requestIndex, requestPosition) }
			)
			Spacer(modifier = Modifier.height(15.dp))
			PlaybackControls(viewModel)
			Spacer(modifier = Modifier
				.fillMaxWidth()
				.height(2f / 10 * maxHeight))
		}
	}
}

@Composable
private fun TracksPagerDisplay(
	viewModel: PlaybackDetailViewModel
) {
	val trackState = viewModel.trackStreamStateFlow.collectAsState()
	BoxWithConstraints() {
		TracksPager(
			trackState = trackState,
			metadataStateForId = { id ->
				viewModel.observeMetadata(id).collectAsState()
			},
			seekIndex = { viewModel.seek(trackState.value.currentIndex, it) }
		)
	}
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TracksPager(
	trackState: State<PlaybackDetailTracksInfo>,
	metadataStateForId: @Composable (String) -> State<PlaybackDetailMetadata>,
	seekIndex: suspend (Int) -> Boolean,
) {
	val tracks by trackState.rememberDerive { it.tracks }
	val currentIndex by trackState.rememberDerive { it.currentIndex }

	val pagerState = rememberPagerState(currentIndex.clampPositive())

	Column {
		HorizontalPager(
			modifier = Modifier
				.fillMaxWidth()
				.height(300.dp),
			state = pagerState,
			count = tracks.size,
		) {
			TracksPagerItem(art = metadataStateForId(tracks[it]).read().artwork)
		}
	}

	val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
	val pressed by pagerState.interactionSource.collectIsPressedAsState()

	val touched = remember { mutableStateOf(false) }

	if (isDragged || pressed) {
		touched.overwrite(true)
	}

	LaunchedEffect(key1 = currentIndex) {
		if (currentIndex >= 0 && pagerState.currentPage != currentIndex && !isDragged) {
			touched.value = false
			if (pagerState.currentPage.inRangeSpread(currentIndex, 1)) {
				pagerState.animateScrollToPage(currentIndex)
			} else {
				pagerState.scrollToPage(currentIndex)
			}
		}
	}

	LaunchedEffect(
		key1 = isDragged,
		key2 = pagerState.currentPage,
		key3 = pagerState.isScrollInProgress
	) {
		val page = pagerState.currentPage
		if (page != currentIndex) {
			if (!isDragged && touched.value) seekIndex(page)
		}
	}
}

private fun Int.inRangeSpread(a: Int, amount: Int): Boolean {
	return (this in a..a + amount || this in a.. a - amount)
}

@Composable
private fun TracksPagerItem(
	art: Any?
) {
	Timber.d("TracksPagerItem, art: $art")
	Box(modifier = Modifier.fillMaxSize()) {
		val context = LocalContext.current
		val req = remember(art) {
			ImageRequest.Builder(context)
				.data(art)
				.crossfade(100)
				.build()
		}
		AsyncImage(
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.height(300.dp)
				.align(Alignment.Center),
			model = req,
			contentDescription = "art",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun CurrentTrackPlaybackDescriptions(
	viewModel: PlaybackDetailViewModel
) {
	PlaybackDescription(metadataState = viewModel.currentMetadataStateFlow.collectAsState(
		PlaybackDetailMetadata()
	))
}

@Composable
private fun PlaybackDescription(metadataState: State<PlaybackDetailMetadata>) {
	Box(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier
				.fillMaxWidth(0.7f)
				.align(Alignment.Center),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			val titleState = metadataState.rememberDerive { metadata -> metadata.title ?: "" }
			val subtitleState = metadataState.rememberDerive { metadata -> metadata.subtitle ?: "" }
			PlaybackDescriptionTitle(textState = titleState)
			PlaybackDescriptionSubtitle(textState = subtitleState)
		}
	}

}

@Composable
private fun PlaybackDescriptionTitle(textState: State<String>) {
	Text(
		text = textState.read(),
		color = Theme.dayNightAbsoluteContentColor(),
		style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
		overflow = TextOverflow.Ellipsis
	)
}

@Composable
private fun PlaybackDescriptionSubtitle(textState: State<String>) {
	Text(
		text = textState.read(),
		color = Theme.dayNightAbsoluteContentColor(),
		style = MaterialTheme.typography.titleMedium,
		overflow = TextOverflow.Ellipsis
	)
}

@Composable
private fun PlaybackControls(viewModel: PlaybackDetailViewModel) {
	PlaybackControlButtons(
		viewModel.playbackPropertiesStateFlow.collectAsState(),
		play = viewModel::playWhenReady,
		pause = viewModel::pause,
		next = viewModel::seekNext,
		previous = viewModel::seekPrevious,
		enableRepeat = viewModel::enableRepeatMode,
		enableRepeatAll = viewModel::enableRepeatAllMode,
		disableRepeat = viewModel::disableRepeatMode,
		enableShuffle = viewModel::enableShuffleMode,
		disableShuffle = viewModel::disableShuffleMode
	)
}

@Composable
private fun PlaybackControlButtons(
	propertiesInfoState: State<PlaybackDetailPropertiesInfo>,
	play: () -> Unit,
	pause: () -> Unit,
	next: () -> Unit,
	previous: () -> Unit,
	enableRepeat: () -> Unit,
	enableRepeatAll: () -> Unit,
	disableRepeat: () -> Unit,
	enableShuffle: () -> Unit,
	disableShuffle: () -> Unit,
) {
	val propertiesInfo = propertiesInfoState.read()
	Row(
		modifier = Modifier
			.fillMaxWidth(0.9f)
			.height(40.dp),
		horizontalArrangement = Arrangement.SpaceAround,
		verticalAlignment = Alignment.CenterVertically
	) {
		// Shuffle
		Box(
			modifier = Modifier.size(40.dp)
		) {
			Icon(
				modifier = Modifier
					.size(30.dp)
					.align(Alignment.TopCenter)
					.clickable { if (propertiesInfo.shuffleOn) disableShuffle() else enableShuffle() }
				,
				painter = painterResource(id = R.drawable.ios_glyph_shuffle_100),
				contentDescription = "shuffle",
				tint = if (propertiesInfo.shuffleOn)
					Theme.dayNightAbsoluteContentColor()
				else
					Color(0xFF787878)
			)
		}

		// Previous
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			Icon(
				modifier = Modifier
					.size(35.dp)
					.align(Alignment.TopCenter)
					.clickable { previous() }
				,
				painter = painterResource(id = R.drawable.ios_glyph_seek_previos_100),
				contentDescription = "previous",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}

		// Play / Pause
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			val rememberPlayPainter = painterResource(id = R.drawable.ios_glyph_play_100)
			val rememberPausePainter = painterResource(id = R.drawable.ios_glyph_pause_100)
			Icon(
				modifier = Modifier
					.size(40.dp)
					.align(Alignment.TopCenter)
					.clickable { if (propertiesInfo.playWhenReady) pause() else play() }
				,
				painter = if (propertiesInfo.playWhenReady)
					rememberPausePainter
				else
					rememberPlayPainter
				,
				contentDescription = "play",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}

		// Next
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			Icon(
				modifier = Modifier
					.size(35.dp)
					.align(Alignment.TopCenter)
					.clickable { if (propertiesInfo.hasNextMediaItem) next() }
				,
				painter = painterResource(id = R.drawable.ios_glyph_seek_next_100),
				contentDescription = "next",
				tint = if (propertiesInfo.hasNextMediaItem)
					Theme.dayNightAbsoluteContentColor()
				else
					Color(0xFF787878)
			)
		}

		// Repeat
		Box(modifier = Modifier
			.size(40.dp)
			.clickable {
				when (propertiesInfo.repeatMode) {
					Player.RepeatMode.OFF -> enableRepeat()
					Player.RepeatMode.ONE -> enableRepeatAll()
					Player.RepeatMode.ALL -> disableRepeat()
				}
			}
		) {
			val rememberRepeatPainter = painterResource(id = R.drawable.ios_glyph_repeat_100)
			val rememberRepeatOnePainter = painterResource(id = R.drawable.ios_glyph_repeat_one_100)
			Icon(
				modifier = Modifier
					.size(30.dp)
					.align(Alignment.TopCenter),
				painter = when (propertiesInfo.repeatMode) {
					Player.RepeatMode.OFF -> rememberRepeatPainter
					Player.RepeatMode.ONE -> rememberRepeatOnePainter
					Player.RepeatMode.ALL -> rememberRepeatPainter
				},
				contentDescription = "repeat",
				tint = if (propertiesInfo.repeatMode == Player.RepeatMode.OFF)
					Color(0xFF787878)
				else
					Theme.dayNightAbsoluteContentColor()
			)
		}
	}
}

@Composable
private fun PlaybackControlProgressSeekbar(
	positionState: State<PlaybackDetailPositionStream>,
	trackState: State<PlaybackDetailTracksInfo>,
	seek: suspend (Int, Long) -> Boolean
) {
	val position = positionState.read().position
	val duration = positionState.read().duration
	val positionChangeReason = positionState.read().positionChangeReason

	fun clampedPosition(): Float = (position / duration).toFloat().clamp(0f, 1f)

	val interactionSource = remember { MutableInteractionSource() }
	val draggedState = interactionSource.collectIsDraggedAsState()
	val pressedState = interactionSource.collectIsPressedAsState()
	val changedValue = remember { mutableStateOf(clampedPosition()) }
	val suppressSeekRequest = remember { mutableStateOf(false) }

	val suppressedUpdateValue = draggedState.read() || pressedState.read() || suppressSeekRequest.read()

	val rememberedIndex = remember { mutableStateOf(-1) }

	val coroutineScope = rememberCoroutineScope()

	Slider(
		modifier = Modifier
			.fillMaxWidth(0.9f)
			.height(14.dp),
		value = if (suppressedUpdateValue) {
			changedValue.read()
		} else {
			clampedPosition()
		},
		onValueChange = {
			rememberedIndex.overwrite(trackState.value.currentIndex)
			changedValue.overwrite(it)
		},
		onValueChangeFinished = {
			suppressSeekRequest.overwrite(true)
			val currentIndex = trackState.value.currentIndex
			if (currentIndex == rememberedIndex.value) {
				changedValue.value.takeIf { it > 0 }?.let {
					coroutineScope.launch {
						if (!seek(currentIndex, (duration.inWholeMilliseconds * it).toLong()))
							suppressSeekRequest.overwrite(false)
					}
				}
			}
		},
		interactionSource = interactionSource,
		trackHeight = 4.dp,
		thumbSize = 14.dp
	)

	LaunchedEffect(key1 = positionChangeReason) {
		if (positionChangeReason == PlaybackDetailPositionStream.PositionChangeReason.USER_SEEK) {
			suppressSeekRequest.overwrite(false)
		}
	}

	LaunchedEffect(key1 = trackState.read().currentIndex) {
		interactionSource.emit(DragInteraction.Cancel(DragInteraction.Start()))
	}
}

@Composable
private fun PlaybackControlProgressSlider(
	value: Float,
	onValueChange: (Float) -> Unit,
	onValueChangeFinished: () -> Unit,
	interactionSource: InteractionSource,
	sliderColor: Color,
) {

}



enum class Visibility {
	VISIBLE,
	GONE
}

private inline val Visibility.isVisible
	get() = this == Visibility.VISIBLE

@Composable
private fun LockScreenOrientation(landscape: Boolean) {
	val activity = LocalContext.current.findActivity() ?: return
	DisposableEffect(key1 = Unit) {
		val original = activity.requestedOrientation
		activity.requestedOrientation =
			if (landscape) {
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
			} else {
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			}
		onDispose { activity.requestedOrientation = original }
	}
}

@Composable
private fun verticalLayoutGuard(): State<Boolean> {
	val contextHelper = rememberLocalContextHelper()
	val verticalLayoutState = remember { mutableStateOf(false) }
	verticalLayoutState.overwrite(contextHelper.configurations.isOrientationPortrait())
	return verticalLayoutState
}

@Composable
private fun AssertVerticalOrientation(lazyMessage: (Int) -> Any = {}) {
	val contextHelper = rememberLocalContextHelper()
	check(contextHelper.configurations.isOrientationPortrait()) {
		val int = contextHelper.configurations.orientationInt
		"AssertVerticalLayout detected non-portrait orientation: ${int}\n" +
			"msg: ${lazyMessage(int)}"
	}
}

@Composable
private fun <T, R> State<T>.rememberDerive(calculation: (T) -> R): State<R> {
	return remember { derive(calculation) }
}

private fun <T, R> State<T>.derive(calculation: (T) -> R): State<R> {
	return derivedStateOf { calculation(read()) }
}

// is explicit read like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> State<T>.read(): T {
	return this.value
}

// is explicit write like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> MutableState<T>.overwrite(value: T) {
	this.value = value
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T> MutableState<T>.rewrite(value: (T) -> T) {
	this.value = value(this.value)
}

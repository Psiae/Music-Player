package com.flammky.musicplayer.ui.playbackcontrol

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.palette.graphics.Palette
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
import kotlinx.coroutines.*
import timber.log.Timber

@Composable
internal fun PlaybackBoxDetail(
	showSelf: State<Boolean>,
	attachBackHandler: State<Boolean>,
	viewModel: PlaybackControlViewModel,
	dismiss: () -> Unit
) {

	// observe whether we should show ourselves
	val visibility = if (showSelf.read()) Visibility.VISIBLE else Visibility.GONE

	if (visibility.isVisible) {
		if (attachBackHandler.read()) BackHandler(onBack = dismiss)
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

		// TODO: Something meaningful while transitioning
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
	viewModel: PlaybackControlViewModel,
	dismiss: () -> Unit
) {
	val savedTransitioned = rememberSaveable { mutableStateOf(false) }
	val transitioned = remember { mutableStateOf(false) }

	val yOffset =
		if (savedTransitioned.value) 0.dp else -(heightState.read() - target)

	Box(
		modifier = Modifier
			.offset(0.dp, yOffset)
			.height(target)
			.fillMaxWidth()
			.background(
				Theme
					.dayNightAbsoluteColor()
					.copy(alpha = 0.9f)
			)

	) {
		if (-yOffset == 0.dp) {
			transitioned.overwrite(true)
		} else if (heightState.read() == 0.dp) {
			transitioned.overwrite(false)
		}
		if (transitioned.read()) {
			PlaybackBoxDetails(viewModel) {
				savedTransitioned.overwrite(false)
				dismiss()
			}
		}
	}
}

@Composable
private fun PlaybackBoxDetails(
	viewModel: PlaybackControlViewModel,
	dismiss: () -> Unit
) {
	// Change to Guard until we implement horizontal orientation
	AssertVerticalOrientation()
	Box(modifier = Modifier
		.fillMaxSize()
		.background(Theme.backgroundColor())
	) {
		RadialPlaybackBackground(viewModel = viewModel)
		Column(
			modifier = Modifier
				.fillMaxSize()
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
	viewModel: PlaybackControlViewModel
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
				seek = { requestIndex, requestPosition ->
					Timber.d("Progressbar request seek pos $requestPosition")
					viewModel.seek(requestIndex, requestPosition)
				}
			)
			Spacer(modifier = Modifier.height(5.dp))
			PlaybackControls(viewModel)
			Spacer(modifier = Modifier
				.fillMaxWidth()
				.height(2f / 10 * maxHeight))
		}
	}
}

@Composable
private fun TracksPagerDisplay(
	viewModel: PlaybackControlViewModel
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

@Composable
private fun RadialPlaybackBackground(
	art: Any?
) {
	val absoluteBackgroundColor = Theme.dayNightAbsoluteColor()
	val backgroundColor = Theme.backgroundColor()
	val radialPaletteColor = remember { mutableStateOf(backgroundColor) }

	val compositeBase =
		if (isSystemInDarkTheme()) {
			backgroundColor
		} else {
			absoluteBackgroundColor
		}

	BoxWithConstraints {
		val color = animateColorAsState(
			targetValue = radialPaletteColor.read(),
			animationSpec = tween(500)
		).value
		val brush = remember(color) {
			Brush.radialGradient(
				colors = listOf(
					color.copy(alpha = 0.45f).compositeOver(compositeBase),
					color.copy(alpha = 0.35f).compositeOver(compositeBase),
					color.copy(alpha = 0.15f).compositeOver(compositeBase),
					color.copy(alpha = 0.1f).compositeOver(compositeBase),
					color.copy(alpha = 0.0f).compositeOver(compositeBase)
				),
				center = Offset(constraints.maxWidth.toFloat() / 2, constraints.maxHeight.toFloat() / 4),
				radius = constraints.maxWidth.toFloat()
			)
		}
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(brush)
		)
	}
	val darkTheme = isSystemInDarkTheme()
	val artwork = art
	LaunchedEffect(artwork) {
		val color = if (artwork is Bitmap) {
			withContext(Dispatchers.IO) {
				val palette = Palette.from(artwork).maximumColorCount(16).generate()
				ensureActive()
				val argb = palette.run {
					if (darkTheme) {
						getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
					} else {
						getVibrantColor(getLightMutedColor(getDominantColor(-1)))
					}
				}
				if (argb != -1) {
					Color(argb)
				} else {
					backgroundColor
				}
			}
		} else {
			backgroundColor
		}
		if (isActive) radialPaletteColor.overwrite(color)
	}
}

@Composable
private fun RadialPlaybackBackground(
	viewModel: PlaybackControlViewModel
) = RadialPlaybackBackground(viewModel.currentMetadataStateFlow.collectAsState().read().artwork)

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
			TracksPagerItem(metadataStateForId(tracks[it]))
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
	metadataState: State<PlaybackDetailMetadata>
) {
	val art = metadataState.value.artwork
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		val context = LocalContext.current
		val req = remember(art) {
			ImageRequest.Builder(context)
				.data(art)
				.crossfade(200)
				.build()
		}
		AsyncImage(
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.height(280.dp)
				.align(Alignment.Center),
			model = req,
			contentDescription = "art",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun CurrentTrackPlaybackDescriptions(
	viewModel: PlaybackControlViewModel
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
private fun PlaybackControls(viewModel: PlaybackControlViewModel) {
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
					.align(Alignment.Center)
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
					.align(Alignment.Center)
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
					.align(Alignment.Center)
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
					.align(Alignment.Center)
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
			Icon(
				modifier = Modifier
					.size(30.dp)
					.align(Alignment.Center),
				painter = when (propertiesInfo.repeatMode) {
					Player.RepeatMode.OFF -> painterResource(id = R.drawable.ios_glyph_repeat_100)
					Player.RepeatMode.ONE -> painterResource(id = R.drawable.ios_glyph_repeat_one_100)
					Player.RepeatMode.ALL -> painterResource(id = R.drawable.ios_glyph_repeat_100)
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

	fun clampedPositionProgress(): Float = (position / duration).toFloat().clamp(0f, 1f)
	fun clampedPosition(): Long = (duration.inWholeMilliseconds * clampedPositionProgress()).toLong()

	val interactionSource = remember { MutableInteractionSource() }
	val draggedState = interactionSource.collectIsDraggedAsState()
	val pressedState = interactionSource.collectIsPressedAsState()
	val changedValue = remember { mutableStateOf(clampedPositionProgress()) }
	val suppressSeekRequest = remember { mutableStateOf(false) }

	val suppressedUpdateValue = draggedState.read() || pressedState.read() || suppressSeekRequest.read()

	val rememberedIndex = remember { mutableStateOf(-1) }

	val coroutineScope = rememberCoroutineScope()

	Slider(
		modifier = Modifier
			.fillMaxWidth(0.85f)
			.height(14.dp),
		value = if (suppressedUpdateValue) {
			changedValue.read()
		} else {
			clampedPositionProgress()
		},
		onValueChange = {
			rememberedIndex.overwrite(trackState.value.currentIndex)
			changedValue.overwrite(it)
		},
		onValueChangeFinished = {
			changedValue.value.takeIf { it >= 0 }?.let {
				suppressSeekRequest.overwrite(true)
				coroutineScope.launch {
					if (!seek(trackState.value.currentIndex, (duration.inWholeMilliseconds * it).toLong()))
						suppressSeekRequest.overwrite(false)
				}
				return@Slider
			}
		},
		interactionSource = interactionSource,
		trackHeight = 4.dp,
		thumbSize = 14.dp,
		colors = SliderDefaults.colors(
			activeTrackColor = Theme.dayNightAbsoluteContentColor(),
			thumbColor = Theme.dayNightAbsoluteContentColor()
		)
	)

	Spacer(modifier = Modifier.height(3.dp))

	BoxWithConstraints {
		// Width of the Slider track, which is maxWidth minus Thumb Radius on both side
		val width = maxWidth * 0.85f - 14.dp
		Row(
			modifier = Modifier.width(width),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			val formattedDuration = remember(duration) {
				val seconds =
					if (duration.isNegative() || duration.isInfinite()) 0
					else duration.inWholeSeconds
				if (seconds > 3600) {
					String.format(
						"%02d:%02d:%02d",
						seconds / 3600,
						seconds % 3600 / 60,
						seconds % 60
					)
				} else {
					String.format(
						"%02d:%02d",
						seconds / 60,
						seconds % 60
					)
				}
			}

			val displayPositionProgress = if (suppressedUpdateValue) {
				changedValue.read()
			} else {
				clampedPositionProgress()
			}

			val formattedPosition = remember(displayPositionProgress) {
				val seconds =
					if (duration.isNegative()) 0
					else (duration.inWholeSeconds * displayPositionProgress).toLong()
				if (seconds > 3600) {
					String.format(
						"%02d:%02d:%02d",
						seconds / 3600,
						seconds % 3600 / 60,
						seconds % 60
					)
				} else {
					String.format(
						"%02d:%02d",
						seconds / 60,
						seconds % 60
					)
				}
			}

			Text(
				text = formattedPosition,
				style = MaterialTheme.typography.bodySmall
			)

			Text(
				text = formattedDuration,
				style = MaterialTheme.typography.bodySmall
			)
		}
	}

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

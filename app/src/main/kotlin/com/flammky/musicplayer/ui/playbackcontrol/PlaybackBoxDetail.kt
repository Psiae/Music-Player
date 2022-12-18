package com.flammky.musicplayer.ui.playbackcontrol

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.android.medialib.player.Player
import com.flammky.androidx.content.context.findActivity
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.common.kotlin.comparable.clampPositive
import com.flammky.musicplayer.R
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.ui.Theme
import com.flammky.musicplayer.ui.util.compose.NoRipple
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

/**
 * @param showSelfState whether to show this composable
 * @param attachBackHandlerState whether to attach the BackHandler, useful to ensure that the
 */
@Composable
internal fun DetailedPlaybackControl(
	showSelfState: State<Boolean>,
	attachBackHandlerState: State<Boolean>,
	viewModel: PlaybackControlViewModel,
	dismiss: () -> Unit
) {

	val visible = showSelfState.value
	val attachBackHandler = attachBackHandlerState.value

	val savedTransitioned = rememberSaveable { mutableStateOf(false) }
	// remember the first value we got from the saver
	val rememberSavedTransitioned = remember { mutableStateOf(savedTransitioned.value) }

	val localDismiss = remember(dismiss) {
		{
			savedTransitioned.value = false
			dismiss()
		}
	}

	if (visible) {
		LockScreenOrientation(landscape = false)
	}

	if (attachBackHandler) {
		BackHandler(onBack = localDismiss)
	}

	BoxWithConstraints(
		modifier = Modifier,
		contentAlignment = Alignment.BottomCenter,
	) {

		// Ensure that background contents are not clickable during transition
		NoRipple {
			Box(
				modifier = Modifier
					.fillMaxSize(fraction = if (visible) 1f else 0f)
					.clickable { }
			)
		}

		// TODO: Something meaningful while transitioning
		val animatedHeight = updateTransition(targetState = visible, label = "")
			.animateDp(
				label = "",
				transitionSpec = {
					remember(targetState) {
						tween(
							durationMillis = when {
								rememberSavedTransitioned.value -> {
									rememberSavedTransitioned.value = false
									0
								}
								targetState -> 250
								else -> 150
							},
							easing = if (targetState) FastOutSlowInEasing else LinearOutSlowInEasing
						)
					}
				}
			) { visible ->
				if (visible) maxHeight else 0.dp
			}.also { state ->
				if (state.value == maxHeight) { savedTransitioned.value = true }
			}

		PlaybackBoxDetailTransition(
			heightTargetDp = maxHeight,
			heightDpState = animatedHeight,
			viewModel = viewModel,
			dismiss = localDismiss
		)
	}
}

@Composable
private fun PlaybackBoxDetailTransition(
	heightTargetDp: Dp,
	heightDpState: State<Dp>,
	viewModel: PlaybackControlViewModel,
	dismiss: () -> Unit
) {
	val transitionedState = remember { mutableStateOf(false) }

	val yOffsetState = heightDpState.derive { heightDp ->
		heightTargetDp - heightDp
	}

	val yOffset = heightDpState.derive { heightDp ->
		heightTargetDp - heightDp
	}.value.also { y ->
		// switch the state
		when (y) {
			// no offset, fully visible, transitioned
			0.dp -> transitionedState.value = true
			// target offset, fully hidden, not transitioned
			heightTargetDp -> transitionedState.value = false
			// else keep whatever it was
		}
	}
	// on a xOffset a positive value is right, negative value is left
	// on a yOffset a positive value is down, negative value is up (0,0 == top-left)
	// offset == 0 means there is no offset applied which is the full visibility
	// offset > 0 && offset < target means the offset partially hide the layout
	// offset >= target means the offset fully hide the layout
	if (yOffset != heightTargetDp) {
		// load the composable when it's visible
		PlaybackBoxDetails(
			modifier = Modifier
				.offset(0.dp, yOffsetState.value)
				// set the height directly,
				// so composables will not recalculate dynamic size unnecessarily during transition (if any)
				.height(heightTargetDp)
				.fillMaxWidth()
				.background(
					Theme
						.dayNightAbsoluteColor()
						.copy(alpha = 0.94f)
				),
			viewModel = viewModel,
			transitionedState = transitionedState,
			dismiss = dismiss
		)
	}
}

@Composable
private fun PlaybackBoxDetails(
	modifier: Modifier,
	viewModel: PlaybackControlViewModel,
	transitionedState: State<Boolean>,
	dismiss: () -> Unit
) {
	val sessionIdState = remember {
		mutableStateOf<String?>(null)
	}

	val sessionControllerState = remember {
		mutableStateOf<PlaybackController?>(null)
	}

	val sessionId = sessionIdState.value
	val sessionController = sessionControllerState.value

	LaunchedEffect(key1 = null) {
		viewModel.observeCurrentSessionId().collect { id ->
			sessionIdState.value = id
			// set to null directly here so composables will be forgotten
			// and let the DisposableEffect cleanup
			sessionControllerState.value = null
		}
	}

	DisposableEffect(key1 = sessionId) {
		if (sessionId == null) {
			sessionControllerState.value = null
			return@DisposableEffect onDispose {  }
		}
		val controller = viewModel.createController(sessionId)
		sessionControllerState.value = controller
		onDispose { controller.dispose() }
	}

	BoxWithConstraints(modifier = modifier.fillMaxSize()) {

		// we should find alternatives for light mode
		val animatedAlpha = animateFloatAsState(
			targetValue = if (transitionedState.value) 1f else 0f,
			animationSpec = tween(if (transitionedState.value) 100 else 0)
		).value

		RadialPlaybackBackground(
			modifier = Modifier.alpha(animatedAlpha),
			controller = sessionController,
			viewModel = viewModel
		)
		Column(
			modifier = Modifier
				.fillMaxSize()
				.statusBarsPadding()
		) {
			NoRipple {
				DetailToolbar(
					modifier = Modifier.padding(top = 10.dp),
					dismiss = dismiss
				)
				if (sessionController == null) {
					return@NoRipple
				}
				DetailsContent(
					modifier = Modifier
						.padding(top = 20.dp)
						.alpha(animatedAlpha),
					viewModel,
					sessionController
				)
			}
		}
	}
}

@Composable
private fun DetailToolbar(
	modifier: Modifier,
	dismiss: () -> Unit
) {
	Box(modifier = modifier) {
		Column(modifier = Modifier
			.height(50.dp)
			.padding(vertical = 5.dp)
		) {
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
	modifier: Modifier,
	viewModel: PlaybackControlViewModel,
	playbackController: PlaybackController,
) {

	val playbackObserverState = remember {
		mutableStateOf<PlaybackObserver?>(null)
	}

	DisposableEffect(key1 = playbackController) {
		val observer = playbackController.createPlaybackObserver()
		playbackObserverState.value = observer
		onDispose(observer::dispose)
	}

	BoxWithConstraints(modifier = modifier) {

		val playbackObserver = playbackObserverState.value

		val id = playbackController.sessionID

		val maxHeight = maxHeight
		val maxWidth = maxWidth
		Column(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.Top,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			TracksPagerDisplay(
				viewModel = viewModel,
				controller = playbackController
			)
			CurrentTrackPlaybackDescriptions(
				modifier = Modifier.padding(top = 15.dp),
				viewModel = viewModel
			)
			// TODO: let it use controller
			if (playbackObserver != null) {
				PlaybackControlProgressSeekbar(
					modifier = Modifier.padding(top = 15.dp),
					id = id,
					maxWidth = maxWidth,
					playbackObserver = playbackObserver,
					seek = { seekId, pos ->
						coroutineContext.ensureActive()
						check(seekId == playbackController.sessionID)
						playbackController.requestSeekAsync(pos.milliseconds).await().let { result ->
							result.eventDispatch?.join()
							result.success
						}
					}
				)
				PlaybackControls(
					modifier = Modifier.padding(top = 5.dp),
					viewModel
				)
			}
		}
	}
}

@Composable
private fun TracksPagerDisplay(
	viewModel: PlaybackControlViewModel,
	controller: PlaybackController
) {
	val observerState = remember { mutableStateOf<PlaybackObserver?>(null) }
	val queueState = remember { mutableStateOf(PlaybackQueue.UNSET) }

	DisposableEffect(key1 = controller) {
		val observer = controller.createPlaybackObserver()
		observerState.value = observer
		queueState.value = PlaybackQueue.UNSET
		onDispose { observer.dispose() }
	}

	val observer = observerState.value
		?: return Box(modifier = Modifier
			.fillMaxWidth()
			.height(300.dp))

	val coroutineScope = rememberCoroutineScope()

	DisposableEffect(key1 = observer) {
		observer.createQueueCollector(coroutineScope.coroutineContext)
			.apply {
				coroutineScope.launch {
					startCollect().join()
					queueStateFlow.collect { queueState.value = it }
				}
			}
		onDispose { observer.dispose() }
	}

	TracksPager(
		queueState = queueState,
		metadataStateForId = { id ->
			viewModel.observeMetadata(id).collectAsState()
		},
		seekIndex = { index ->
			coroutineContext.ensureActive()
			val result = controller.requestSeekAsync(index, Duration.ZERO).await()
			result.eventDispatch?.join()
			result.success
		}
	)
}

@Composable
private fun RadialPlaybackBackground(
	modifier: Modifier = Modifier,
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

	BoxWithConstraints(
		modifier = modifier,
	) {
		val color = animateColorAsState(
			targetValue = radialPaletteColor.read(),
			animationSpec = tween(500)
		).value
		val brush = remember(color) {
			Brush.radialGradient(
				colors = listOf(
					color.copy(alpha = 0.55f).compositeOver(compositeBase),
					color.copy(alpha = 0.45f).compositeOver(compositeBase),
					color.copy(alpha = 0.35f).compositeOver(compositeBase),
					color.copy(alpha = 0.2f).compositeOver(compositeBase),
					color.copy(alpha = 0.15f).compositeOver(compositeBase),
					color.copy(alpha = 0.1f).compositeOver(compositeBase),
					color.copy(alpha = 0.05f).compositeOver(compositeBase),
					color.copy(alpha = 0.0f).compositeOver(compositeBase)
				),
				center = Offset(constraints.maxWidth.toFloat() / 2, constraints.maxHeight.toFloat() / 3.5f),
				radius = constraints.maxWidth.toFloat() * 0.9f
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
	modifier: Modifier = Modifier,
	controller: PlaybackController?,
	viewModel: PlaybackControlViewModel,
) {
	val coroutineScope = rememberCoroutineScope()

	val observerState = remember(controller) {
		mutableStateOf<PlaybackObserver?>(controller?.createPlaybackObserver())
	}

	val observer = observerState.value

	val idState = remember(observer) { mutableStateOf("") }
	val id = idState.value
	val metadataStateFlow = remember(id) {
		viewModel.observeMetadata(id)
	}
	RadialPlaybackBackground(
		modifier,
		metadataStateFlow.collectAsState().value.artwork
	)
	DisposableEffect(key1 = observer) {
		if (observer == null) {
			return@DisposableEffect onDispose {  }
		}
		val queueCollector = observer.createQueueCollector(coroutineScope.coroutineContext)
			.apply {
				coroutineScope.launch {
					startCollect().join()
					queueStateFlow.collect { queue ->
						idState.value = queue.list.getOrNull(queue.currentIndex) ?: ""
					}
				}
			}
		onDispose { queueCollector.dispose() }
	}
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TracksPager(
	queueState: State<PlaybackQueue>,
	metadataStateForId: @Composable (String) -> State<PlaybackControlTrackMetadata>,
	seekIndex: suspend (Int) -> Boolean,
) {
	BoxWithConstraints(
		modifier = Modifier
			.fillMaxWidth()
			.height(300.dp),
	) {
		val tracks = queueState.rememberDerive { it.list }.value
		val pagerState = rememberPagerState(queueState.value.currentIndex.clampPositive())

		if (tracks.isEmpty()) {
			return@BoxWithConstraints
		}

		HorizontalPager(
			modifier = Modifier.fillMaxSize(),
			state = pagerState,
			count = tracks.size
		) {
			TracksPagerItem(metadataStateForId(tracks[it]))
		}

		val touched = remember { mutableStateOf(false) }

		PagerListenMediaIndexChange(
			indexState = queueState.rememberDerive { it.currentIndex },
			pagerState = pagerState,
			onScroll = { touched.overwrite(false) }
		)

		PagerListenUserDrag(pagerState = pagerState) {
			touched.overwrite(true)
		}

		PagerListenPageChange(
			pagerState = pagerState,
			touchedState = touched,
			seekIndex = seekIndex
		)
	}
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerListenMediaIndexChange(
	indexState: State<Int>,
	pagerState: PagerState,
	onScroll: suspend () -> Unit
) {
	val currentIndex by indexState
	LaunchedEffect(
		key1 = currentIndex
	) {
		// TODO: Velocity check, if the user is dragging the pager but aren't moving
		// or if the user drag velocity will ends in another page
		if (currentIndex >= 0 &&
			pagerState.currentPage != currentIndex &&
			!pagerState.isScrollInProgress
		) {
			onScroll()
			if (currentIndex.inRangeSpread(pagerState.currentPage, 2)) {
				pagerState.animateScrollToPage(currentIndex)
			} else {
				pagerState.scrollToPage(currentIndex)
			}
		}
	}
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerListenUserDrag(
	pagerState: PagerState,
	onUserInteract: () -> Unit
) {
	val dragged = pagerState.interactionSource.collectIsDraggedAsState()
	LaunchedEffect(
		dragged.value
	) {
		if (dragged.value) onUserInteract()
	}
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PagerListenPageChange(
	pagerState: PagerState,
	touchedState: State<Boolean>,
	seekIndex: suspend (Int) -> Boolean
) {
	val dragging by pagerState.interactionSource.collectIsDraggedAsState()
	val touched by touchedState
	LaunchedEffect(
		pagerState.currentPage,
		dragging,
		touched
	) {
		if (touched && !dragging) {
			seekIndex(pagerState.currentPage)
		}
	}
}

private fun Int.inRangeSpread(a: Int, amount: Int): Boolean {
	return (this in a - amount.. a + amount)
}

@Composable
private fun TracksPagerItem(
	metadataState: State<PlaybackControlTrackMetadata>
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
	modifier: Modifier = Modifier,
	viewModel: PlaybackControlViewModel
) {
	PlaybackDescription(
		modifier = modifier,
		metadataState = viewModel.currentMetadataStateFlow.collectAsState()
	)
}

@Composable
private fun PlaybackDescription(
	modifier: Modifier,
	metadataState: State<PlaybackControlTrackMetadata>
) {
	val metadata = metadataState.value
	Timber.d("PlaybackDescription metadataState: $metadata")
	Box(modifier = modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.align(Alignment.Center),
			horizontalAlignment = Alignment.Start,
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
private fun PlaybackControls(
	modifier: Modifier = Modifier,
	viewModel: PlaybackControlViewModel
) {
	Box(modifier = modifier) {
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
			val interactionSource = remember { MutableInteractionSource() }
			val size by animateDpAsState(
				targetValue = if (interactionSource.collectIsPressedAsState().read()) 27.dp else 30.dp
			)
			Icon(
				modifier = Modifier
					.size(size)
					.align(Alignment.Center)
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = { if (propertiesInfo.shuffleOn) disableShuffle() else enableShuffle() }
					)
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
			val interactionSource = remember { MutableInteractionSource() }
			val size by animateDpAsState(
				targetValue = if (interactionSource.collectIsPressedAsState().read()) 32.dp else 35.dp
			)
			Icon(
				modifier = Modifier
					.size(size)
					.align(Alignment.Center)
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = { previous() }
					)
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
			val interactionSource = remember { MutableInteractionSource() }
			val size by animateDpAsState(
				targetValue = if (interactionSource.collectIsPressedAsState().read()) 37.dp else 40.dp
			)
			Icon(
				modifier = Modifier
					.size(size)
					.align(Alignment.Center)
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = { if (propertiesInfo.playWhenReady) pause() else play() }
					)
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

			val interactionSource = remember { MutableInteractionSource() }
			val size by animateDpAsState(
				targetValue = if (interactionSource.collectIsPressedAsState().read()) 32.dp else 35.dp
			)
			Icon(
				modifier = Modifier
					.size(size)
					.align(Alignment.Center)
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = { if (propertiesInfo.hasNextMediaItem) next() }
					)
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
		Box(modifier = Modifier.size(40.dp)) {
			val interactionSource = remember { MutableInteractionSource() }
			val size by animateDpAsState(
				targetValue = if (interactionSource.collectIsPressedAsState().read()) 27.dp else 30.dp
			)
			Icon(
				modifier = Modifier
					.size(size)
					.align(Alignment.Center)
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = {
							when (propertiesInfo.repeatMode) {
								Player.RepeatMode.OFF -> enableRepeat()
								Player.RepeatMode.ONE -> enableRepeatAll()
								Player.RepeatMode.ALL -> disableRepeat()
							}
						}
					),
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
	modifier: Modifier = Modifier,
	id: String,
	maxWidth: Dp,
	playbackObserver: PlaybackObserver,
	seek: suspend (String, Long) -> Boolean
) {
	val sliderPositionCollectorReady = remember {
		mutableStateOf(false)
	}
	val sliderTextPositionCollectorReady = remember {
		mutableStateOf(false)
	}
	val sliderDurationCollectorReady = remember {
		mutableStateOf(false)
	}
	val sliderReady = remember {
		mutableStateOf(false)
	}
	val sliderTextReady = remember {
		mutableStateOf(false)
	}

	val coroutineScope = rememberCoroutineScope()

	val sliderWidth = remember(maxWidth) {
		maxWidth * 0.85f
	}

	val sliderPositionCollector = remember {
		playbackObserver.createProgressionCollector(
			collectorContext = coroutineScope.coroutineContext,
			includeEvent = true,
		).apply {
			coroutineScope.launch {
				startCollectPosition().join()
				sliderPositionCollectorReady.value = true
			}
			setIntervalHandler { _, progress, duration, speed ->
				if (progress == ZERO || duration == ZERO || speed == 0f) {
					null
				} else {
					(duration.inWholeMilliseconds / sliderWidth.value / speed).toLong()
						.takeIf { it > 100 }?.milliseconds
						?: PlaybackConstants.DURATION_UNSET
				}.also {
					Timber.d("Playback_Slider_Debug: intervalHandler($it) param: $progress $duration $speed")
				}
			}
		}
	}

	val sliderTextPositionCollector = remember {
		playbackObserver.createProgressionCollector(
			collectorContext = coroutineScope.coroutineContext,
			includeEvent = true,
		).apply {
			coroutineScope.launch {
				startCollectPosition().join()
				sliderTextPositionCollectorReady.value = true
			}
		}
	}

	val durationCollector = remember {
		playbackObserver.createDurationCollector(
			collectorContext = coroutineScope.coroutineContext
		).apply {
			coroutineScope.launch {
				startCollect().join()
				sliderDurationCollectorReady.value = true
			}
		}
	}

	val durationState = durationCollector.durationStateFlow.collectAsState()

	val ready = remember {
		derivedStateOf { sliderReady.value && sliderTextReady.value }
	}

	val alpha = if (ready.value) 1f else 0f

	if (sliderPositionCollectorReady.value && sliderDurationCollectorReady.value) {
		sliderReady.value = true
	}

	if (sliderTextPositionCollectorReady.value && sliderDurationCollectorReady.value) {
		sliderTextReady.value = true
	}

	if (!sliderReady.value || !sliderTextReady.value) {
		return
	}

	val sliderTextPositionState = sliderTextPositionCollector.positionStateFlow.collectAsState()
	val sliderPositionState = sliderPositionCollector.positionStateFlow.collectAsState()

	val position by sliderPositionState
	val duration by durationState

	fun clampedPositionProgress(): Float = (position / duration).toFloat().clamp(0f, 1f)

	val interactionSource = remember { MutableInteractionSource() }
	val draggedState = interactionSource.collectIsDraggedAsState()
	val pressedState = interactionSource.collectIsPressedAsState()
	val changedValue = remember {
		// the Slider should constraint it to 0, consider change it to nullable Float
		mutableStateOf(-1f)
	}
	val seekRequests = remember {
		mutableStateOf(0)
	}

	// consume the `animate` composable, it will recreate a new state so it will not animate
	// should the first visible progress be animated ?
	val consumeAnimation = remember { mutableStateOf(1) }

	val suppressValue = draggedState.value or pressedState.value or (changedValue.value >= 0)

	require((seekRequests.value == 0) or (consumeAnimation.value > 0)) {
		"seekRequest should be followed by consumeAnimation"
	}

	val sliderValue = when {
		suppressValue -> {
			changedValue.value
		}
		consumeAnimation.value > 0 -> {
			consumeAnimation.value = 0
			clampedPositionProgress()
		}
		else -> {
			val clampedProgress = clampedPositionProgress()
			// should snap
			val tweenDuration = if (clampedProgress == 1f || clampedProgress == 0f) 0 else 100
			animateFloatAsState(
				targetValue = clampedProgress,
				animationSpec = tween(tweenDuration)
			).value
		}
	}

	Box(modifier = modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier
				.align(Alignment.Center)
				.alpha(alpha),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Slider(
				modifier = Modifier
					.width(sliderWidth)
					.height(16.dp),
				value = sliderValue,
				// unfortunately There's no guarantee these 2 will be called in order
				onValueChange = { value ->
					Timber.d("Slider_DEBUG: onValueChange: $value")
					changedValue.value = value
				},
				onValueChangeFinished = {
					Timber.d("Slider_DEBUG: onValueChangeFinished: ${changedValue.value}")
					changedValue.value.takeIf { it >= 0 }?.let {
						seekRequests.value++
						consumeAnimation.value++
						coroutineScope.launch {
							val seekTo = (duration.inWholeMilliseconds * it).toLong()
							// should this be async ?
							seek(id, seekTo)
							if (--seekRequests.value == 0) {
								changedValue.value = -1f
							}
						}
						return@Slider
					}
					if (seekRequests.value == 0) changedValue.value = -1f
				},
				interactionSource = interactionSource,
				trackHeight = 6.dp,
				thumbSize = 12.dp,
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
					PlaybackProgressSliderText(
						progressState = sliderTextPositionState.rememberDerive(
							calculation = { raw ->
								if (changedValue.value >= 0) {
									changedValue.value
								} else {
									raw.inWholeMilliseconds / durationState.value.inWholeMilliseconds.toFloat()
								}
							}
						),
						durationState = durationState
					)
				}
			}
		}
	}



	DisposableEffect(key1 = null) {
		onDispose {
			sliderPositionCollector.dispose()
			sliderTextPositionCollector.dispose()
			durationCollector.dispose()
		}
	}
}

@Composable
private fun RowScope.PlaybackProgressSliderText(
	progressState: State<Float>,
	durationState: State<Duration>
) {
	val progress = progressState.value
	val duration = durationState.value

	val formattedProgress: String = remember(progress) {
		val seconds =
			if (duration.isNegative()) 0
			else (duration.inWholeSeconds * progress).toLong()
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

	val formattedDuration: String = remember(duration) {
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

	Text(
		text = formattedProgress,
		style = MaterialTheme.typography.bodySmall
	)

	Text(
		text = formattedDuration,
		style = MaterialTheme.typography.bodySmall
	)
}


enum class Visibility {
	VISIBLE,
	GONE
}

private inline val Visibility.isVisible
	get() = this == Visibility.VISIBLE

@Composable
private fun LockScreenOrientation(landscape: Boolean) {
	val activity = LocalContext.current.findActivity()
		?: error("cannot Lock Screen Orientation, LocalContext is not an Activity")
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
	BoxWithConstraints {
		check(maxHeight >= maxWidth)
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

@SuppressLint("BanParcelableUsage", "ParcelCreator")
private class ValueWrapper <T> (
	@get:Synchronized
	@set:Synchronized
	var value: T
) : Parcelable {
	override fun describeContents(): Int = 0

	override fun writeToParcel(p0: Parcel, p1: Int) {
		p0.writeValue(value)
	}
}

private operator fun <T> ValueWrapper<T>.getValue(receiver: Any?, property: KProperty<*>): T = value
private operator fun <T> ValueWrapper<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) {
	this.value = value
}

private class SeekRequest(
	val position: Long,
)

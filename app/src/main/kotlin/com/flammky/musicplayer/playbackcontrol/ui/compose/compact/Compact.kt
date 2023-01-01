@file:OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalPagerApi::class,
	ExperimentalSnapperApi::class
)

package com.flammky.musicplayer.playbackcontrol.ui.compose.compact

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.musicplayer.R
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.playbackcontrol.ui.PlaybackControlTrackMetadata
import com.flammky.musicplayer.playbackcontrol.ui.PlaybackControlViewModel
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.ui.util.compose.NoRipple
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.placeholder.placeholder
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val CompactHeight = 55.dp

@Composable
internal fun TransitioningCompactPlaybackControl(
	bottomVisibilityVerticalPadding: Dp,
	onArtworkClicked: () -> Unit,
	onBaseClicked: () -> Unit,
	onVerticalVisibilityChanged: (Dp) -> Unit
) {
	val vm = viewModel<PlaybackControlViewModel>()
	val coroutineScope = rememberCoroutineScope()

	val sessionIDState = remember {
		mutableStateOf(vm.currentSessionID())
	}

	val controllerState = remember {
		mutableStateOf<PlaybackController?>(null)
	}

	val showSelfState = remember {
		mutableStateOf(false)
	}

	val animatedOffset by animateDpAsState(
		targetValue = if (showSelfState.value)
			bottomVisibilityVerticalPadding.unaryMinus()
		else
			CompactHeight
	)

	val sessionID = sessionIDState.value
	val sessionController = controllerState.value

	LaunchedEffect(
		key1 = null,
		block = {
			vm.observeCurrentSessionId().collect {
				sessionIDState.value = it
			}
		}
	)

	DisposableEffect(
		key1 = sessionID,
		effect = {
			if (sessionID == null) {
				return@DisposableEffect onDispose {  }
			}
			val supervisor = SupervisorJob()
			val newController = vm.createController(sessionID, coroutineScope.coroutineContext)
			controllerState.value = newController
			showSelfState.value = false
			newController.createPlaybackObserver(coroutineScope.coroutineContext)
				.apply {
					createQueueCollector()
						.apply {
							coroutineScope.launch(supervisor) {
								startCollect().join()
								queueStateFlow.collect { queue ->
									showSelfState.value = queue.currentIndex >= 0
								}
							}
						}
				}
			onDispose { supervisor.cancel() ; newController.dispose() }
		}
	)

	LaunchedEffect(
		key1 = animatedOffset,
		block = { onVerticalVisibilityChanged(animatedOffset - CompactHeight) }
	)

	BoxWithConstraints(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.BottomCenter
	) {
		val disposeCardState = remember {
			mutableStateOf(animatedOffset >= CompactHeight)
		}
		if (sessionController == null) {
			// hide & dispose
			return@BoxWithConstraints
		}
		if (!disposeCardState.value) {
			CardLayout(
				modifier = Modifier.offset(y = animatedOffset)
					.onGloballyPositioned { lc ->
						if (lc.positionInParent().y >= constraints.maxHeight) {
							disposeCardState.value = true
						}
					},
				viewModel = viewModel(),
				sessionController,
				onArtworkClicked = { onArtworkClicked() },
				onBaseClicked = { onBaseClicked() }
			)
		}
		if (animatedOffset < CompactHeight) {
			disposeCardState.value = false
		}
	}
}

// Consider a single observer
@Composable
private fun CardLayout(
	modifier: Modifier = Modifier,
	viewModel: PlaybackControlViewModel,
	controller: PlaybackController,
	onArtworkClicked: () -> Unit,
	onBaseClicked: () -> Unit
) {
	val surface = Theme.surfaceVariantColorAsState().value
	val absBackgroundColor = Theme.backgroundColorAsState().value
	val absBackgroundContentColor = Theme.backgroundContentColorAsState().value
	val animatedSurfaceColorState = animatedCompositeCardSurfacePaletteColorAsState(
		dark = surface.luminance() < 0.35f,
		compositeFactor = 0.7f,
		compositeOver = surface,
		controller = controller,
		viewModel = viewModel
	)
	val isBackgroundDarkState = remember {
		derivedStateOf { animatedSurfaceColorState.value.luminance() < 0.35f }
	}
	Card(
		modifier = modifier
			.height(CompactHeight)
			.fillMaxWidth()
			.padding(horizontal = 5.dp),
		shape = RoundedCornerShape(5.dp),
		colors = CardDefaults.cardColors(surface),
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(animatedSurfaceColorState.value),
			verticalArrangement = Arrangement.Bottom
		) {
			Row(
				modifier = Modifier
					.weight(1f, true)
					.clickable { onBaseClicked() }
					.padding(7.5.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				ArtworkCard(
					modifier = Modifier
						.fillMaxHeight()
						.aspectRatio(1f, true)
						.clip(RoundedCornerShape(5.dp))
						.clickable { onArtworkClicked() },
					viewModel = viewModel,
					controller = controller
				)
				DescriptionPager(
					modifier = Modifier
						.weight(2f, true)
						.padding(horizontal = 5.dp),
					controller = controller,
					isBackgroundDarkState = isBackgroundDarkState,
					viewModel = viewModel
				)
				PlaybackButtons(
					modifier = Modifier,
					dark = !isBackgroundDarkState.value,
					controller = controller,
					viewModel = viewModel
				)
			}
			AnimatedProgressBar(
				modifier = Modifier
					.height(2.5.dp)
					.fillMaxWidth()
					.background(
						Theme.surfaceVariantColorAsState().value
							.copy(alpha = 0.7f)
							.compositeOver(absBackgroundColor)
					),
				controller = controller
			)
		}
	}
	DisposableEffect(key1 = null, effect = {
		onDispose { Timber.d("CompactPlaybackControl disposed") }
	})
}

@Composable
private fun cardSurfacePaletteColor(
	dark: Boolean,
	controller: PlaybackController,
	viewModel: PlaybackControlViewModel,
): Color {
	val coroutineScope = rememberCoroutineScope()

	val paletteColorState = remember(controller) {
		mutableStateOf(Color.Unspecified)
	}

	val observer = remember(controller) {
		controller.createPlaybackObserver()
	}

	val queueState = remember(controller) {
		mutableStateOf(PlaybackConstants.QUEUE_UNSET)
	}

	val artworkState = remember(paletteColorState) {
		mutableStateOf<Any?>(null)
	}

	val queue = queueState.value
	val id = queue?.list?.getOrNull(queue.currentIndex)
	val artwork = artworkState.value

	DisposableEffect(key1 = observer, effect = {
		val supervisor = SupervisorJob()
		observer.createQueueCollector()
			.apply {
				coroutineScope.launch(supervisor) {
					startCollect().join()
					queueStateFlow.collect {
						queueState.value = it
					}
				}
			}
		onDispose { supervisor.cancel() ; observer.dispose() }
	})

	LaunchedEffect(key1 = id, block = {
		artworkState.value = null
		if (id == null) {
			return@LaunchedEffect
		}
		viewModel.observeMetadata(id).collect {
			artworkState.value = it.artwork
		}
	})

	LaunchedEffect(key1 = artwork, key2 = dark, block = {
		if (artwork !is Bitmap) {
			paletteColorState.value = Color.Unspecified
			return@LaunchedEffect
		}
		withContext(Dispatchers.IO) {
			// consider increasing the bitmap area
			val gen = suspendCancellableCoroutine<Color> { cont ->
				Palette.from(artwork).generate { palette ->
					val int = palette?.run {
						if (dark) {
							getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
						} else {
							getVibrantColor(getLightMutedColor(getDominantColor(-1)))
						}
					} ?: -1
					cont.resume(if (int != -1) Color(int) else Color.Unspecified)
				}
			}
			withContext(Dispatchers.Main) {
				ensureActive()
				paletteColorState.value = gen
			}
		}
	})

	return paletteColorState.value
}

@Composable
private fun animatedCompositeCardSurfacePaletteColorAsState(
	dark: Boolean,
	compositeFactor: Float,
	compositeOver: Color,
	controller: PlaybackController,
	viewModel: PlaybackControlViewModel
): State<Color> {
	val paletteColor = cardSurfacePaletteColor(dark, controller, viewModel)
		.takeIf { it != Color.Unspecified }
			?.copy(compositeFactor)
			?.compositeOver(compositeOver)
		?: compositeOver
	return animateColorAsState(paletteColor, tween(100))
}

@Composable
private fun ArtworkCard(
	modifier: Modifier,
	viewModel: PlaybackControlViewModel,
	controller: PlaybackController,
) {
	val coroutineScope = rememberCoroutineScope()
	val artState = remember(controller) {
		mutableStateOf<Any?>(null)
	}
	val observer = remember(controller) {
		controller.createPlaybackObserver(coroutineScope.coroutineContext)
	}
	val context = LocalContext.current
	val art = artState.value
	val req = remember(artState.value) {
		ImageRequest.Builder(context)
			.crossfade(true)
			.data(art)
			.build()
	}

	AsyncImage(
		modifier = modifier
			.fillMaxSize()
			.placeholder(
				art == null,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			),
		model = req,
		contentScale = ContentScale.Crop,
		contentDescription = null
	)

	DisposableEffect(
		key1 = observer,
		effect = {
			val supervisor = SupervisorJob()
			val channel = Channel<PlaybackQueue>(capacity = Channel.CONFLATED)
			val qCollector = observer.createQueueCollector(coroutineScope.coroutineContext)
				.apply {
					coroutineScope.launch(supervisor) {
						var lastJob: Job = Job()
						for (queue in channel) {
							lastJob.cancel()
							queue.list.getOrNull(queue.currentIndex)
								?.let {
									lastJob = launch {
										viewModel.observeMetadata(it).collect { metadata ->
											artState.value = metadata.artwork
										}
									}
								}
								?: run { artState.value = null }
						}
					}
					coroutineScope.launch(supervisor) {
						startCollect().join()
						queueStateFlow.collect { channel.send(it) }
					}
				}
			onDispose {
				supervisor.cancel() ; channel.close() ; observer.dispose()
				check(qCollector.disposed) {
					"QueueCollector=$qCollector was not disposed by parent Observer$observer"
				}
			}
		}
	)
}

@Composable
private fun DescriptionPager(
	modifier: Modifier = Modifier,
	isBackgroundDarkState: State<Boolean>,
	controller: PlaybackController,
	viewModel: PlaybackControlViewModel
) {
	val coroutineScope = rememberCoroutineScope()
	val queueState = remember {
		mutableStateOf(PlaybackConstants.QUEUE_UNSET)
	}
	val queueOverrideState = remember(controller) {
		mutableStateOf<PlaybackQueue?>(null)
	}
	val queueOverrideAmountState = remember(controller) {
		mutableStateOf(0)
	}
	val observer = remember(controller) {
		controller.createPlaybackObserver(coroutineScope.coroutineContext)
	}
	DisposableEffect(key1 = observer, effect = {
		val supervisor = SupervisorJob()
		observer.createQueueCollector(coroutineScope.coroutineContext)
			.apply {
				coroutineScope.launch(supervisor) {
					startCollect().join()
					queueStateFlow.collect {
						queueState.value = it
					}
				}
			}
		onDispose { supervisor.cancel() ; observer.dispose() }
	})

	BoxWithConstraints(modifier = modifier.fillMaxSize()) {
		val queue = queueState.value
		val queueOverride = queueOverrideState.value

		Timber.d("DescriptionPager: $queue")

		if (queue?.list?.getOrNull(queue.currentIndex) == null ||
			(queueOverride != null && queueOverride.list.getOrNull(queueOverride.currentIndex) == null)
		) {
			return@BoxWithConstraints
		}

		val pagerState = rememberPagerState(queue.currentIndex)
		val touchedState = remember { mutableStateOf(false) }
		val draggingState = pagerState.interactionSource.collectIsDraggedAsState()
		val baseFling = PagerDefaults.flingBehavior(state = pagerState)

		HorizontalPager(
			modifier = Modifier.fillMaxSize(),
			state = pagerState,
			count = queue.list.size,
			flingBehavior = remember(pagerState) {
				object : FlingBehavior {
					override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
						return with(baseFling) { performFling(initialVelocity * 0.05f) }
					}
				}
			}
		) {
			DescriptionPagerItem(
				dark = !isBackgroundDarkState.value,
				viewModel = viewModel,
				mediaID = queue.list[it]
			)
		}

		NoInline {
			LaunchedEffect(key1 = queue.currentIndex, queueOverride, block = {
				if (queueOverride != null) {
					return@LaunchedEffect
				}
				if (queue.currentIndex >= 0 &&
					pagerState.currentPage != queue.currentIndex &&
					!pagerState.isScrollInProgress
				) {
					touchedState.value = false
					if (pagerState.currentPage in queue.currentIndex - 2 .. queue.currentIndex + 2) {
						pagerState.animateScrollToPage(queue.currentIndex)
					} else {
						pagerState.scrollToPage(queue.currentIndex)
					}
				}
			})

			LaunchedEffect(key1 = draggingState.value, block = {
				if (draggingState.value) touchedState.value = true
			})

			val rememberedPageState = remember {
				mutableStateOf(pagerState.currentPage)
			}
			LaunchedEffect(
				key1 = pagerState.currentPage,
				key2 = draggingState.value,
				key3 = touchedState.value,
				block = {
					val currentPage = pagerState.currentPage
					if (touchedState.value && !draggingState.value &&
						(currentPage != rememberedPageState.value ||
							rememberedPageState.value != queueState.value.currentIndex) &&
						currentPage in queue.list.indices
					) {
						queueOverrideState.value = queue.copy(currentIndex = currentPage)
						rememberedPageState.value = currentPage
						queueOverrideAmountState.value++
						runCatching {
							controller.requestSeekAsync(
								currentPage,
								startPosition = Duration.ZERO,
								coroutineContext = /* defaulted by ViewModel */ EmptyCoroutineContext
							).await().eventDispatch?.join()
						}
						if (--queueOverrideAmountState.value == 0) queueOverrideState.value = null
					}
				}
			)
		}
	}
}

@Composable
private fun DescriptionPagerItem(
	modifier: Modifier = Modifier,
	dark: Boolean,
	viewModel: PlaybackControlViewModel,
	mediaID: String
) {
	val metadataState = remember {
		mutableStateOf<PlaybackControlTrackMetadata>(PlaybackControlTrackMetadata())
	}

	LaunchedEffect(key1 = mediaID, block = {
		viewModel.observeMetadata(mediaID).collect { metadataState.value = it }
	})

	Column(
		modifier = modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center
	) {
		val textColor = if (dark) Color.Black else Color.White
		val metadata = metadataState.value

		Text(
			text = metadata.title ?: "",
			fontSize = MaterialTheme.typography.bodyMedium.fontSize,
			fontWeight = FontWeight.Bold,
			color = textColor,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)

		Spacer(modifier = Modifier.height(1.dp))

		Text(
			text = metadata.subtitle ?: "",
			fontSize = MaterialTheme.typography.labelMedium.fontSize,
			fontWeight = FontWeight.SemiBold,
			color = textColor,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

@Composable
private fun PlaybackButtons(
	modifier: Modifier = Modifier,
	dark: Boolean,
	controller: PlaybackController,
	// TODO: remove
	viewModel: PlaybackControlViewModel,
) {
	NoRipple {
		Row(
			modifier = modifier.fillMaxHeight(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceEvenly
		) {
			Box(
				modifier = Modifier
					.fillMaxHeight()
					.width(30.dp),
				contentAlignment = Alignment.Center
			) {}
			Box(
				modifier = Modifier
					.fillMaxHeight()
					.width(30.dp),
				contentAlignment = Alignment.Center
			) {

			}
			Box(
				modifier = Modifier
					.fillMaxHeight()
					.width(30.dp),
				contentAlignment = Alignment.Center
			) {
				val propertiesInfoState = viewModel.playbackPropertiesStateFlow.collectAsState()
				// IsPlaying callback from mediaController is somewhat not accurate
				val showPlay = !propertiesInfoState.value.playWhenReady
				val icon =
					if (showPlay) {
						R.drawable.play_filled_round_corner_32
					} else {
						R.drawable.pause_filled_narrow_rounded_corner_32
					}

				val interactionSource = remember { MutableInteractionSource() }
				val pressed by interactionSource.collectIsPressedAsState()
				val size by animateDpAsState(targetValue =  if (pressed) 18.dp else 21.dp)
				Icon(
					modifier = Modifier
						.size(size)
						.clickable(
							interactionSource = interactionSource,
							indication = null
						) {
							if (showPlay) {
								viewModel.playWhenReady()
							} else {
								viewModel.pause()
							}
						},
					painter = painterResource(id = icon),
					contentDescription = null,
					tint = if (dark) Color.Black else Color.White,
				)
			}
		}
	}
}

@Composable
private fun AnimatedProgressBar(
	modifier: Modifier = Modifier,
	controller: PlaybackController
) {
	val coroutineScope = rememberCoroutineScope()
	BoxWithConstraints(modifier) {
		val width = maxWidth
		val observer = remember(controller) {
			controller.createPlaybackObserver(coroutineScope.coroutineContext)
		}
		val positionState = remember(observer) {
			mutableStateOf(Duration.ZERO)
		}
		val bufferedPositionState = remember(observer) {
			mutableStateOf(Duration.ZERO)
		}
		val durationState = remember(observer) {
			mutableStateOf(Duration.INFINITE)
		}
		val progressCollectorReadyState = remember(observer) {
			mutableStateOf(false)
		}
		val durationCollectorReadyState = remember(observer) {
			mutableStateOf(false)
		}

		DisposableEffect(key1 = observer, effect = {
			val supervisor = SupervisorJob()

			observer.createProgressionCollector(coroutineScope.coroutineContext)
				.apply {
					setCollectEvent(true)
					setIntervalHandler { isEvent, progress, duration, speed ->
						if (progress == Duration.ZERO || duration == Duration.ZERO || speed == 0f) {
							PlaybackConstants.DURATION_UNSET
						} else {
							(duration.inWholeMilliseconds / width.value / speed).toLong()
								.takeIf { it > 100 }?.milliseconds
								?: PlaybackConstants.DURATION_UNSET
						}
					}
					coroutineScope.launch(supervisor) {
						startCollectPosition().join()
						positionState.value = positionStateFlow.value
						bufferedPositionState.value = bufferedPositionStateFlow.value
						progressCollectorReadyState.value = true
						launch {
							positionStateFlow.collect {
								positionState.value = it
							}
						}
						launch {
							bufferedPositionStateFlow.collect {
								bufferedPositionState.value = it
							}
						}
					}
				}

			observer.createDurationCollector(coroutineScope.coroutineContext)
				.apply {
					coroutineScope.launch(supervisor) {
						startCollect().join()
						durationState.value = durationStateFlow.value
						durationCollectorReadyState.value = true
						launch {
							durationStateFlow.collect {
								durationState.value = it
							}
						}
					}
				}


			onDispose { supervisor.cancel() ; observer.dispose() }
		})


		NoInline {
			val bufferedPosition = bufferedPositionState.value
			val duration = durationState.value

			require(!bufferedPosition.isNegative() || bufferedPosition == PlaybackConstants.POSITION_UNSET) {
				"Negative Position ($bufferedPosition) must be equal to " +
					"PlaybackConstants.POSITION_UNSET (${PlaybackConstants.POSITION_UNSET})"
			}
			require(!bufferedPosition.isNegative() || duration == PlaybackConstants.DURATION_UNSET) {
				"Negative Duration ($duration) must be equal to " +
					"PlaybackConstants.DURATION_UNSET (${PlaybackConstants.DURATION_UNSET})"
			}
			val bufferedProgress by animateFloatAsState(
				targetValue = when {
					bufferedPosition == PlaybackConstants.POSITION_UNSET ||
						duration == PlaybackConstants.DURATION_UNSET -> 0f
					bufferedPosition > duration -> 1f
					else -> bufferedPosition.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds
				},
				animationSpec = tween(150)
			)
			LinearProgressIndicator(
				modifier = Modifier.fillMaxSize(),
				progress = bufferedProgress,
				color = Theme.surfaceVariantColorAsState().value.copy(alpha = 0.5f),
				trackColor = Color.Transparent
			)
		}
		NoInline {
			val position = positionState.value
			val duration = durationState.value

			require(!position.isNegative() || position == PlaybackConstants.POSITION_UNSET) {
				"Negative Position ($position) must be equal to " +
					"PlaybackConstants.POSITION_UNSET (${PlaybackConstants.POSITION_UNSET})"
			}
			require(!position.isNegative() || duration == PlaybackConstants.DURATION_UNSET) {
				"Negative Duration ($duration) must be equal to " +
					"PlaybackConstants.DURATION_UNSET (${PlaybackConstants.DURATION_UNSET})"
			}
			val progress by animateFloatAsState(
				targetValue = when {
					position == PlaybackConstants.POSITION_UNSET || duration == PlaybackConstants.DURATION_UNSET || position > duration -> 0f
					else -> position.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds
				},
				animationSpec = tween(150)
			)
			LinearProgressIndicator(
				modifier = Modifier.fillMaxSize(),
				progress = progress,
				color = Theme.backgroundContentColorAsState().value,
				trackColor = Color.Transparent
			)
		}
	}
}

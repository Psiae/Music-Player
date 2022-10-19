package com.flammky.musicplayer.ui.main.compose.screens.root

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.x.lifecycle.viewmodel.compose.activityViewModel
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.mediaplayer.domain.viewmodels.MainViewModel
import com.flammky.musicplayer.R
import com.flammky.musicplayer.domain.musiclib.entity.PlaybackState
import com.flammky.musicplayer.domain.musiclib.entity.PlaybackState.Companion.isEmpty
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.PlayerExtension.isStateBuffering
import com.flammky.musicplayer.ui.common.compose.LinearIndeterminateProgressIndicator
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxModel
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxPositions
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxViewModel
import com.flammky.musicplayer.ui.util.compose.NoRipple
import com.google.accompanist.placeholder.placeholder
import kotlinx.coroutines.*
import timber.log.Timber

@Composable
fun PlaybackControl(model: PlaybackControlModel, bottomOffset: Dp) {
	val mainVM: MainViewModel = viewModel()

	val targetState =
		if (model.showSelf.read()) {
			VISIBILITY.VISIBLE
		} else {
			VISIBILITY.GONE
		}

	val transition = updateTransition(targetState = targetState, label = "")

	val animatedOffset = transition.animateDp(
		label = "",
		transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) }
	) {
		if (it == VISIBILITY.VISIBLE) {
			bottomOffset
		} else {
			(-bottomOffset)
		}
	}

	if (animatedOffset.read() > 0.dp) {
		val density = LocalDensity.current.density
		Box(
			modifier = Modifier.onGloballyPositioned { bounds ->
				mainVM.playbackControlShownHeight.rewrite {
					(animatedOffset.read() - bottomOffset) + (bounds.size.height / density).dp
				}
			}
		) {
			PlaybackControlBox(model, animatedOffset.read())
		}
	} else {
		mainVM.playbackControlShownHeight.overwrite(0.dp)
	}
}

private val UNSET = Any()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackControlBox(
	model: PlaybackControlModel,
	bottomOffset: Dp
) {

	Timber.d("PlaybackControlBox recomposed")

	val boxVM: PlaybackBoxViewModel = activityViewModel()
	val coroutineScope = rememberCoroutineScope()
	val coroutineDispatcher = Dispatchers.Main.immediate + SupervisorJob()

	val context = LocalContext.current
	val darkTheme = isSystemInDarkTheme()
	val newModel = remember { boxVM.observeModel() }.collectAsState(initial = PlaybackBoxModel.UNSET)

	val cardBackgroundColor = cardPaletteColorState(paletteState = paletteState(boxVM = boxVM)).read()

	Card(
		modifier = Modifier
			.height(55.dp)
			.fillMaxWidth()
			.padding(start = 5.dp, end = 5.dp)
			.offset(x = 0.dp, y = -bottomOffset),
		shape = RoundedCornerShape(10),
		colors = CardDefaults.elevatedCardColors(cardBackgroundColor)
	) {

		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.BottomStart
		) {
			Row(
				modifier = Modifier
					.fillMaxSize()
					.padding(7.5.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Card(
					modifier = Modifier
						.fillMaxHeight()
						.aspectRatio(1f, true),
					shape = RoundedCornerShape(10),
				) {
					ArtworkDisplay(boxVM = boxVM)
				}
				val isCardDark = cardBackgroundColor.luminance() < 0.4f
				MetadataDescription(boxVM = boxVM, isBackgroundDark = isCardDark)

				// IsPlaying callback from mediaController is somewhat not accurate
				val showPlay = !newModel.read().playWhenReady
				val icon =
					if (showPlay) {
						R.drawable.play_filled_round_corner_32
					} else {
						R.drawable.pause_filled_narrow_rounded_corner_32
					}

				NoRipple {

					val interactionSource = remember { MutableInteractionSource() }
					val pressed by interactionSource.collectIsPressedAsState()
					val size by animateDpAsState(targetValue =  if (pressed) 18.dp else 21.dp)

					Icon(
						modifier = Modifier
							.weight(0.2f)
							.size(size)
							.clickable(
								interactionSource = interactionSource,
								indication = null
							) {
								if (showPlay) {
									boxVM.play()
								} else {
									boxVM.pause()
								}
							},
						painter = painterResource(id = icon),
						contentDescription = null,
						tint = if (isCardDark) Color.White else Color.Black,
					)
				}
			}

			ProgressIndicator(
				boxVM = boxVM,
				bufferingState = model.buffering
			)
		}
	}
}

@Composable
private fun paletteState(
	boxVM: PlaybackBoxViewModel
): State<Palette?> {
	val artState = remember { boxVM.observeCurrentArtwork() }.collectAsState(initial = null)
	val paletteState = remember { mutableStateOf<Palette?>(null) }

	val coroutineScope = rememberCoroutineScope()
	val coroutineDispatcher = Dispatchers.IO + SupervisorJob()

	val art = artState.read()
	DisposableEffect(key1 = art) {
		val paletteJob = coroutineScope.launch(coroutineDispatcher) {
			if (art is Bitmap) {
				Palette.from(art).maximumColorCount(16).generate().let {
					ensureActive()
					paletteState.overwrite(it)
				}
			} else {
				paletteState.overwrite(null)
			}
		}
		onDispose {
			paletteJob.cancel()
			paletteState.overwrite(null)
		}
	}

	return paletteState
}

@Composable
private fun cardPaletteColorState(
	paletteState: State<Palette?>
): State<Color> {
	val defaultCardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
	val colorState = remember {
		mutableStateOf(defaultCardBackgroundColor)
	}
	val palette = paletteState.read()
	val darkTheme = isSystemInDarkTheme()
	remember(palette.hashCode()) {
		val int = palette?.run {
			if (darkTheme) {
				getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
			} else {
				getVibrantColor(getLightMutedColor(getDominantColor(-1)))
			}
		} ?: -1
		val color = if (int != -1) Color(int) else defaultCardBackgroundColor
		color.copy(alpha = 0.5f)
			.compositeOver(defaultCardBackgroundColor)
			.also { colorState.overwrite(it) }
	}
	return colorState
}

@Composable
private fun ArtworkDisplay(
	boxVM: PlaybackBoxViewModel
) {
	val artState = remember { boxVM.observeCurrentArtwork() }.collectAsState(initial = null)
	val art = artState.read()
	val context = LocalContext.current

	val req = remember(art) {
		ImageRequest.Builder(context)
			.crossfade(true)
			.data(art)
			.memoryCachePolicy(CachePolicy.ENABLED)
			.build()
	}

	AsyncImage(
		modifier = Modifier
			.fillMaxSize()
			.placeholder(
				art == null,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			),
		model = req,
		contentScale = ContentScale.Crop,
		contentDescription = null
	)
}

@Composable
private fun RowScope.MetadataDescription(
	boxVM: PlaybackBoxViewModel,
	isBackgroundDark: Boolean
) {
	Column(
		modifier = Modifier
			.fillMaxHeight()
			.padding(start = 10.dp, end = 10.dp)
			.weight(1f, true),
		verticalArrangement = Arrangement.SpaceEvenly,
		horizontalAlignment = Alignment.Start,
	) {

		val textColor = if (isBackgroundDark) Color.White else Color.Black
		val metadataState = remember { boxVM.observeCurrentMetadata() }.collectAsState(initial = null)

		val metadata = metadataState.read()
		if (metadata is MediaMetadata) {
			val style = MaterialTheme.typography.bodyMedium
				.copy(
					color = textColor,
					fontWeight = FontWeight.SemiBold
				)
			Text(
				text = metadata.title ?: "",
				style = style,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
		if (metadata is AudioMetadata) {
			val style = MaterialTheme.typography.bodyMedium
				.copy(
					color = textColor,
					fontWeight = FontWeight.Medium
				)
			Text(
				text = metadata.albumArtistName ?: metadata.artistName ?: "",
				style = style,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}

@Composable
private fun ProgressIndicator(
	boxVM: PlaybackBoxViewModel,
	bufferingState: State<Boolean>,
) {

	val positionState = remember { boxVM.observePositions() }.collectAsState(
		initial = PlaybackBoxPositions()
	)

	Timber.d("ProgressIndicator recomposed")

	val darkTheme = isSystemInDarkTheme()
	val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
	val background = MaterialTheme.colorScheme.background

	val backgroundTrackColor = remember {
		surfaceVariant.copy(alpha = 0.5f).compositeOver(background)
	}

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.heightIn(2.dp),
		color = backgroundTrackColor
	) {

		val shouldTraverse = remember {
			mutableStateOf(false)
		}

		val loading = bufferingState.read() && positionState.read().bufferedPosition.inWholeMilliseconds <= 0

		if (loading || shouldTraverse.read()) {

			LinearIndeterminateProgressIndicator(
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(max = 2.dp),
				trackColor = Color.Transparent,
				color = if (darkTheme) Color.White else Color.Black,
				duration = 850 + 333 + 200 + 533,
				firstLineHeadDelay = 0,
				firstLineHeadDuration = 650,
				firstLineTailDuration = 650 + 150,
				secondLineHeadDelay = 850 + 333,
				secondLineTailDuration = 433,
				secondLineTailDelay = 850 + 333 + 300
			) {
				loading.also { shouldTraverse.overwrite(it) }
			}

		} else {
			AnimatedBufferedProgressIndicator(
				bufferedPosition = positionState.read().bufferedPosition.inWholeMilliseconds,
				duration = positionState.read().duration.inWholeMilliseconds
			)
			AnimatedPlaybackProgressIndicator(
				position = positionState.read().position.inWholeMilliseconds,
				duration = positionState.read().duration.inWholeMilliseconds
			)
		}
	}
}

@Composable
private fun AnimatedBufferedProgressIndicator(
	bufferedPosition: Long,
	duration: Long
) {

	Timber.d("AnimatedBuffered ProgressIndicator recomposed")

	val bufferedProgress by animateFloatAsState(
		targetValue = when {
			bufferedPosition == 0L || duration == 0L || bufferedPosition > duration -> 0f
			else -> bufferedPosition.toFloat() / duration
		},
		animationSpec = tween(150)
	)

	BufferedProgressIndicator(bufferedProgress = bufferedProgress)
}

@Composable
private fun BufferedProgressIndicator(
	bufferedProgress: Float,
) {
	val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

	val bufferProgressColor = remember {
		onSurfaceVariant.copy(alpha = 0.5f)
	}

	LinearIndeterminateProgressIndicator(
		modifier = Modifier
			.fillMaxWidth()
			.heightIn(max = 2.dp),
		trackColor = Color.Transparent,
		color = bufferProgressColor,
		progress = bufferedProgress.clamp(0f, 1f)
	)
}

@Composable
private fun AnimatedPlaybackProgressIndicator(
	position: Long,
	duration: Long
) {

	Timber.d("AnimatedPlayback ProgressIndicator recomposed")

	val progress by animateFloatAsState(
		targetValue = when {
			position == 0L || duration == 0L || position > duration -> 0f
			else -> position.toFloat() / duration
		},
		animationSpec = tween(150)
	)

	PlaybackProgressIndicator(progress = progress)
}

@Composable
private fun PlaybackProgressIndicator(progress: Float) {
	LinearIndeterminateProgressIndicator(
		modifier = Modifier
			.fillMaxWidth()
			.heightIn(max = 2.dp),
		trackColor = Color.Transparent,
		color = if (isSystemInDarkTheme()) Color.White else Color.Black,
		progress = progress.clamp(0f, 1f)
	)
}

@Preview
@Composable
fun PlaybackProgressIndicatorPreview() = PlaybackProgressIndicator(progress = 0.75f)

@Preview
@Composable
fun AnimatedPlaybackProgressIndicatorPreview() = AnimatedPlaybackProgressIndicator(
	position = 75,
	duration = 100
)

// Use ViewModel instead
class PlaybackControlModel() {

	private val mMediaItem = mutableStateOf<com.flammky.android.medialib.common.mediaitem.MediaItem>(com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET)

	private val mBuffering = mutableStateOf<Boolean>(value = false)
	private val mBufferedPosition = mutableStateOf<Long>(0)

	private var mShowSelf = mutableStateOf<Boolean>(false)
	private val mArt = mutableStateOf<Any?>(null)
	private val mPalette = mutableStateOf<Palette?>(null)

	private val mPlayAvailable = mutableStateOf<Boolean>(false)
	private val mShowPlay = mutableStateOf<Boolean>(false)
	private val mPlaybackPosition = mutableStateOf<Long>(0)
	private val mPlaybackDuration = mutableStateOf<Long>(0)

	private val mPlaybackTitle = mutableStateOf<String>("")
	private val mPlaybackArtist = mutableStateOf<String>("")

	val buffering: State<Boolean>
		get() = mBuffering

	val bufferedPosition: State<Long>
		get() = mBufferedPosition

	val playbackArtist: State<String>
		get() = mPlaybackArtist

	val playbackTitle: State<String>
		get() = mPlaybackTitle

	val showSelf: State<Boolean>
		get() = mShowSelf

	val playbackPosition: State<Long>
		get() = mPlaybackPosition

	val playbackDuration: State<Long>
		get() = mPlaybackDuration

	val palette: State<Palette?>
		get() = mPalette

	val showPlay: State<Boolean>
		get() = mShowPlay

	val playAvailable: State<Boolean>
		get() = mPlayAvailable

	val art: State<Any?>
		get() = mArt

	object NO_ART

	private var artJob = Job().job

	val mediaItem: State<com.flammky.android.medialib.common.mediaitem.MediaItem>
		get() = mMediaItem

	var actualMediaItem: com.flammky.android.medialib.common.mediaitem.MediaItem = com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET
		private set

	fun updateMediaItem(item: com.flammky.android.medialib.common.mediaitem.MediaItem) {
		mMediaItem.overwrite(item)
	}

	fun updateMetadata(metadata: MediaMetadata?) {
		when (metadata) {
			null -> {
				mPlaybackTitle.overwrite("")
				mPlaybackArtist.overwrite("")
			}
			is AudioMetadata -> {
				mPlaybackTitle.overwrite(metadata.title ?: "")
				mPlaybackArtist.overwrite(metadata.artistName ?: metadata.albumArtistName ?: "")
			}
			else -> {
				mPlaybackTitle.overwrite(metadata.title ?: "")
				mPlaybackArtist.overwrite("")
			}
		}
	}


	fun updateBy(state: PlaybackState) {
		mPlaybackDuration.overwrite(state.duration)
		mPlayAvailable.overwrite(!state.playWhenReady)
		mShowPlay.overwrite((!state.playing && (!state.playerState.isStateBuffering() && !state.playWhenReady )))
		mShowSelf.overwrite(!state.isEmpty && state.mediaItem !== MediaItem.EMPTY)
		mBuffering.overwrite( state.playerState.isStateBuffering())
	}

	private var qBufferingState = false
	private var qLoadingState = false

	private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
	private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	fun updatePosition(position: Long) {
		mPlaybackPosition.overwrite(position)
	}

	fun updateBufferedPosition(bufferedPosition: Long) {
		mBufferedPosition.overwrite(bufferedPosition)
	}

	fun updateArt(bitmap: Bitmap?) {
		Timber.d("updateArt $bitmap")
		mArt.overwrite(bitmap)
		artJob.cancel()

		if (bitmap != null) {
			artJob = ioScope.launch {
				Palette.from(bitmap).maximumColorCount(16).generate().let {
					ensureActive()
					mPalette.overwrite(it)
				}
			}
		} else {
			mPalette.overwrite(null)
		}
	}

	companion object {
		val placeHolder = PlaybackControlModel()

		inline val PlaybackControlModel.isPlaceholder
			get() = this === placeHolder
	}
}
enum class VISIBILITY {
	VISIBLE,
	GONE
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


private fun <T> State<T>.derive(): State<T> = derive { it }

private fun <T, R> State<T>.derive(
	calculation: (T) -> R
): State<R> {
	return derivedStateOf { calculation(value) }
}

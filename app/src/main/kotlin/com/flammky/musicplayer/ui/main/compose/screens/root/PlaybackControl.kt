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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.google.accompanist.placeholder.placeholder
import com.flammky.mediaplayer.domain.viewmodels.MainViewModel
import com.flammky.mediaplayer.domain.viewmodels.MediaViewModel
import com.flammky.musicplayer.R
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.musicplayer.domain.musiclib.entity.PlaybackState
import com.flammky.musicplayer.domain.musiclib.entity.PlaybackState.Companion.isEmpty
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.PlayerExtension.isStateBuffering
import com.flammky.musicplayer.ui.common.compose.LinearIndeterminateProgressIndicator
import com.flammky.musicplayer.ui.util.compose.NoRipple
import kotlinx.coroutines.*
import timber.log.Timber

@Composable
fun PlaybackControl(model: PlaybackControlModel, bottomOffset: Dp) {

	val mainVM: MainViewModel = viewModel()

	val targetState =
		if (model.showSelf.value) {
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

	if (animatedOffset.value > 0.dp) {
		val density = LocalDensity.current.density

		Box(
			modifier = Modifier.onGloballyPositioned {
				mainVM.playbackControlShownHeight.value =
					(animatedOffset.value - bottomOffset) + (it.size.height / density).dp
			}
		) {
			PlaybackControlBox(model, animatedOffset.value)
		}

	} else {
		mainVM.playbackControlShownHeight.value = 0.dp
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackControlBox(
	model: PlaybackControlModel,
	bottomOffset: Dp
) {

	Timber.d("PlaybackControlBox recomposed")

	val mediaVM: MediaViewModel = viewModel()

	val context = LocalContext.current
	val darkTheme = isSystemInDarkTheme()
	val defaultCardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant

	val palette = model.palette.value

	val cardBackgroundColor = remember(palette.hashCode()) {
		val int = palette?.run {
			if (darkTheme) {
				getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
			} else {
				getVibrantColor(getLightMutedColor(getDominantColor(-1)))
			}
		} ?: -1

		val color = if (int != -1) Color(int) else defaultCardBackgroundColor
		color.copy(alpha = 0.5f).compositeOver(defaultCardBackgroundColor)
	}

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

					val art = model.art.value

					val req = remember(art.hashCode()) {
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

				val isCardDark = cardBackgroundColor.luminance() < 0.4f

				Column(
					modifier = Modifier
						.fillMaxHeight()
						.padding(start = 10.dp, end = 10.dp)
						.weight(1f, true),
					verticalArrangement = Arrangement.SpaceEvenly,
					horizontalAlignment = Alignment.Start,
				) {

					val textColor = if (isCardDark) Color.White else Color.Black

					val style = MaterialTheme.typography.bodyMedium
						.copy(
							color = textColor,
							fontWeight = FontWeight.SemiBold
						)
					val style2 = MaterialTheme.typography.bodyMedium
						.copy(
							color = textColor,
							fontWeight = FontWeight.Medium
						)

					Text(
						text = model.playbackTitle.value,
						style = style,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
					Text(
						text = model.playbackArtist.value,
						style = style2,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
				}

				// IsPlaying callback from mediaController is somewhat not accurate
				val showPlay = model.showPlay.value
				val playAvailable = model.playAvailable.value

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
									mediaVM.play()
								} else {
									mediaVM.pause()
								}
							},
						painter = painterResource(id = icon),
						contentDescription = null,
						tint = if (isCardDark) Color.White else Color.Black,
					)
				}
			}

			ProgressIndicator(
				position = model.playbackPosition,
				bufferedPosition = model.bufferedPosition,
				duration = model.playbackDuration,
				loading = model.buffering.value && model.bufferedPosition.value == 0L
			)
		}
	}
}

@Composable
private fun ProgressIndicator(
	position: State<Long>,
	bufferedPosition: State<Long>,
	duration: State<Long>,
	loading: Boolean
) {

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

		if (loading || shouldTraverse.value) {

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
				shouldTraverse.value = loading
				loading
			}

		} else {
			AnimatedBufferedProgressIndicator(
				bufferedPosition = bufferedPosition.value,
				duration = duration.value
			)
			AnimatedPlaybackProgressIndicator(
				position = position.value,
				duration = duration.value
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

// Use ViewModel instead
class PlaybackControlModel() {

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

	var mediaItem: MediaItem = MediaItem.EMPTY
		private set

	var actualMediaItem: com.flammky.android.medialib.common.mediaitem.MediaItem = com.flammky.android.medialib.common.mediaitem.MediaItem.UNSET
		private set
	fun updateMediaItem(item: com.flammky.android.medialib.common.mediaitem.MediaItem) {
		actualMediaItem = item
		val metadata = item.metadata
		mPlaybackTitle.value = metadata.title ?: ""

		if (metadata is AudioMetadata) {
			mPlaybackArtist.value = metadata.artistName ?: metadata.albumArtistName ?: ""
		}
	}


	fun updateBy(state: PlaybackState) {
		mPlaybackDuration.value = state.duration
		mPlayAvailable.value = !state.playWhenReady
		mShowPlay.value = (!state.playing && (!state.playerState.isStateBuffering() && !state.playWhenReady ))
		mShowSelf.value = !state.isEmpty && state.mediaItem !== MediaItem.EMPTY
		mBuffering.value = state.playerState.isStateBuffering()
	}

	private var qBufferingState = false
	private var qLoadingState = false

	private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
	private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	fun updatePosition(position: Long) {
		mPlaybackPosition.value = position
	}

	fun updateBufferedPosition(bufferedPosition: Long) {
		mBufferedPosition.value = bufferedPosition
	}

	fun updateArt(bitmap: Bitmap?) {
		mArt.value = bitmap
		artJob.cancel()

		if (bitmap != null) {
			artJob = ioScope.launch {
				Palette.from(bitmap).maximumColorCount(16).generate().let {
					ensureActive()
					mPalette.value = it
				}
			}
		} else {
			mPalette.value = null
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

package com.kylentt.musicplayer.ui.main.compose.screens.root

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.placeholder
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.common.kotlin.comparable.clamp
import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState
import com.kylentt.musicplayer.domain.musiclib.entity.PlaybackState.Companion.isEmpty
import com.kylentt.musicplayer.ui.util.compose.NoRipple
import kotlinx.coroutines.*

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun PlaybackControlBox(
	model: PlaybackControlModel,
	bottomOffset: Dp
) {

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
		if (int != -1) Color(int) else defaultCardBackgroundColor
	}

	Card(
		modifier = Modifier
			.height(60.dp)
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
				modifier = Modifier.fillMaxSize(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Card(
					modifier = Modifier
						.fillMaxHeight()
						.padding(10.dp)
						.aspectRatio(1f, true),
					shape = RoundedCornerShape(5),
				) {

					val art = model.art.value

					val req = remember(art.hashCode()) {
						ImageRequest.Builder(context)
							.crossfade(true)
							.data(art)
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
					modifier = Modifier.weight(1f, true),
					verticalArrangement = Arrangement.SpaceEvenly,
					horizontalAlignment = Alignment.Start,
				) {

					val textColor = if (isCardDark) Color.White else Color.Black

					val style = MaterialTheme.typography.titleMedium.copy(color = textColor)
					val style2 = MaterialTheme.typography.titleSmall.copy(color = textColor)

					Text(
						text = model.playbackTitle.value,
						style = style,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
					Text(
						text = model.playbackArtist.value,
						style = style2,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
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
					val pressed = interactionSource.collectIsPressedAsState()
					val size by animateDpAsState(targetValue =  if (pressed.value) 18.dp else 22.dp)

					Icon(
						modifier = Modifier
							.weight(0.3f)
							.size(size)
							.clickable(
								interactionSource = interactionSource,
								indication = null
							) {
								if (showPlay) {
									if (playAvailable) mediaVM.play()
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
				position = model.playbackPosition.value.toFloat(),
				duration = model.playbackDuration.value.toFloat()
			)
		}
	}
}

@Composable
private fun ProgressIndicator(position: Float, duration: Float) {
	val progress =
		if (position == 0f || duration == 0f || position > duration) 0f else position / duration

	LinearProgressIndicator(
		modifier = Modifier
			.fillMaxWidth()
			.height((2.5).dp),
		progress = progress.clamp(0f, 1f),
		trackColor = MaterialTheme.colorScheme.surfaceVariant,
		color = if (isSystemInDarkTheme()) Color.White else Color.Black
	)
}

// Use ViewModel instead
class PlaybackControlModel() {

	private var mShowSelf = mutableStateOf<Boolean>(false)
	private val mArt = mutableStateOf<Any?>(null)
	private val mPalette = mutableStateOf<Palette?>(null)

	private val mPlayAvailable = mutableStateOf<Boolean>(false)
	private val mShowPlay = mutableStateOf<Boolean>(false)
	private val mPlaybackPosition = mutableStateOf<Long>(0)
	private val mPlaybackDuration = mutableStateOf<Long>(0)


	private val mPlaybackTitle = mutableStateOf<String>("")
	private val mPlaybackArtist = mutableStateOf<String>("")

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


	fun updateBy(state: PlaybackState) {
		val item = state.mediaItem
		val metadata = item.mediaMetadata
		mediaItem = item
		mPlaybackTitle.value = (metadata.title ?: metadata.albumTitle ?: "").toString()
		mPlaybackArtist.value = (metadata.artist ?: metadata.albumArtist ?: "").toString()
		mPlaybackDuration.value = state.duration
		mShowPlay.value = !state.playing || !state.playWhenReady
		mPlayAvailable.value = !state.playWhenReady
		mShowSelf.value = !state.isEmpty && state.mediaItem !== MediaItem.EMPTY
	}

	fun updatePosition(duration: Long) {
		mPlaybackPosition.value = duration
	}

	suspend fun updateArt(bitmap: Bitmap?) {
		mArt.value = bitmap
		artJob.cancel()

		if (bitmap != null) {
			artJob = withContext(Dispatchers.IO) {
				launch {
					Palette.from(bitmap).maximumColorCount(16).generate().let {
						ensureActive()
						mPalette.value = it
					}
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

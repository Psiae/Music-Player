package com.flammky.musicplayer.playbackcontrol.ui.compose.compact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.airbnb.lottie.compose.*
import com.flammky.musicplayer.R
import com.flammky.musicplayer.playbackcontrol.ui.PlaybackControlViewModel
import com.flammky.musicplayer.playbackcontrol.ui.model.PlayPauseCommand

@Composable
internal fun PropertiesControl(
	viewModel: PlaybackControlViewModel,
	dark: Boolean,
) {
	BoxWithConstraints {
		val baseIconSize = min(maxHeight, maxWidth / 3 - 10.dp)

		Row(
			modifier = Modifier.fillMaxSize(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceAround,
		) {
			PlayPauseButton(
				size = baseIconSize,
				contentSize = baseIconSize - 3.dp,
				dark = dark,
				commandState = viewModel.playPauseCommandStateFlow.collectAsState(),
				play = {  },
				pause = {  }
			)
			FavoriteButton(
				size = baseIconSize,
				contentSize = baseIconSize - 3.dp,
				dark = dark,
				trackId = "",
				changeFavorite = { info, favorite -> /* Impl */ }
			)
			QueueButton(
				size = baseIconSize,
				contentSize = baseIconSize - 3.dp,
				dark = dark,
				onClick = { /* Impl */ }
			)
		}
	}
}

@Composable
private fun PlayPauseButton(
	size: Dp,
	contentSize: Dp,
	dark: Boolean,
	commandState: State<PlayPauseCommand>,
	play: () -> Unit,
	pause: () -> Unit,
) {
	val command by commandState

	val resId =
		if (command is PlayPauseCommand.Pause) {
			R.drawable.ios_glyph_pause_100
		} else {
			R.drawable.ios_glyph_play_100
		}

	val interactionSource = remember { MutableInteractionSource() }

	val interactedSize =
		if (interactionSource.collectIsPressedAsState().value) contentSize - 3.dp else contentSize

	Box(
		modifier = Modifier
			.size(size)
			.clickable(
				enabled = command.enabled,
				interactionSource = interactionSource,
				indication = null,
				onClick = {
					when (command) {
						is PlayPauseCommand.Pause -> pause()
						is PlayPauseCommand.Play -> play()
					}
				}
			),
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.size(interactedSize),
			painter = painterResource(id = resId),
			contentDescription = when (command) {
				is PlayPauseCommand.Pause -> "Pause" + if (!command.enabled) "_NO" else ""
				is PlayPauseCommand.Play -> "Play" + if (!command.enabled) "_NO" else ""
			},
			tint = (if (dark) Color.Black else Color.White).let {
				if (command.enabled) it else Color.Gray.copy(alpha = 0.35f).compositeOver(it)
			}
		)
	}
}

@Composable
private fun FavoriteButton(
	size: Dp,
	contentSize: Dp,
	dark: Boolean,
	trackId: String,
	changeFavorite: (String, Boolean) -> Unit
) {
	var changed by remember {
		mutableStateOf(false)
	}
	var favorite by remember {
		mutableStateOf(false)
	}

	val compositionSpec = LottieCompositionSpec.RawRes(
		resId = if (dark) R.raw.lottie_favorite_dark else R.raw.lottie_favorite_light
	)

	val clipSpec = LottieClipSpec.Progress(
		min = if (favorite) 0.5f else 0f,
		max = if (favorite) 1f else 0.5f
	)

	val animatable = rememberLottieAnimatable()
	val composition = rememberLottieComposition(compositionSpec)

	Box(
		modifier = Modifier
			.size(size)
			.clickable { changeFavorite(trackId, !favorite) },
		contentAlignment = Alignment.Center,
	) {
		LottieAnimation(
			modifier = Modifier.size(contentSize),
			composition = composition.value,
			progress = { animatable.progress },
		)
	}

	LaunchedEffect(
		key1 = trackId
	) {
		changed = false
		favorite = /* User pref */ false
	}

	LaunchedEffect(
		key1 = favorite,
	) {
		animatable.animate(
			composition.value,
			1, 1, 1f,
			clipSpec,
			if (changed) clipSpec.min else clipSpec.max
		)
	}
}

@Composable
private fun QueueButton(
	size: Dp,
	contentSize: Dp,
	dark: Boolean,
	onClick: () -> Unit
) {
	val resId = 0

	val interactionSource = remember { MutableInteractionSource() }

	val interactedSize =
		if (interactionSource.collectIsPressedAsState().value) contentSize - 3.dp else contentSize

	Box(
		modifier = Modifier
			.size(size)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				onClick = onClick
			),
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.size(interactedSize),
			painter = painterResource(id = resId),
			contentDescription = "queue",
			tint = if (dark) Color.Black else Color.White
		)
	}
}

@Composable
private fun <T, R> State<T>.rememberDerive(derive: (T) -> R): State<R> {
	return remember { derivedStateOf { derive(this.value) } }
}

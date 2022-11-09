package com.flammky.musicplayer.playbackcontrol.ui.compact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.airbnb.lottie.compose.*
import com.flammky.musicplayer.R
import com.flammky.musicplayer.playbackcontrol.domain.model.TrackInfo
import com.flammky.musicplayer.playbackcontrol.ui.PlaybackControlViewModel

@Composable
internal fun PropertiesControl(
	viewModel: PlaybackControlViewModel,
	dark: Boolean,
) {
	val playbackInfoState = viewModel.playbackInfoStateFlow.collectAsState()
	val playbackPropertiesState = playbackInfoState.rememberDerive(
		derive = { info -> info.properties }
	)
	BoxWithConstraints {
		val baseIconSize = min(maxHeight, maxWidth / 3 - 10.dp)

		Row(
			modifier = Modifier.fillMaxSize(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceAround,
		) {
			PlayWhenReadyButton(
				size = baseIconSize,
				contentSize = baseIconSize - 3.dp,
				dark = dark,
				playWhenReadyState = playbackPropertiesState.rememberDerive(
					derive = { properties -> properties.playWhenReady }
				),
				onClick = { play -> /* Impl */ },
			)
			FavoriteButton(
				size = baseIconSize,
				contentSize = baseIconSize - 3.dp,
				dark = dark,
				trackInfoState = viewModel.currentTrackInfoStateFlow.collectAsState(),
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
private fun PlayWhenReadyButton(
	size: Dp,
	contentSize: Dp,
	dark: Boolean,
	playWhenReadyState: State<Boolean>,
	onClick: (Boolean) -> Unit
) {
	val playWhenReady by playWhenReadyState

	val resId =
		if (playWhenReady) R.drawable.ios_glyph_pause_100 else R.drawable.ios_glyph_play_100

	val interactionSource = remember { MutableInteractionSource() }

	val interactedSize =
		if (interactionSource.collectIsPressedAsState().value) contentSize - 3.dp else contentSize

	Box(
		modifier = Modifier
			.size(size)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				onClick = { onClick(playWhenReady) }
			),
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.size(interactedSize),
			painter = painterResource(id = resId),
			contentDescription = if (playWhenReady) "Pause" else "Play",
			tint = if (dark) Color.Black else Color.White
		)
	}
}

@Composable
private fun FavoriteButton(
	size: Dp,
	contentSize: Dp,
	dark: Boolean,
	trackInfoState: State<TrackInfo>,
	changeFavorite: (TrackInfo, Boolean) -> Unit
) {
	val trackInfo = trackInfoState.value

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
			.clickable { changeFavorite(trackInfo, !favorite) },
		contentAlignment = Alignment.Center,
	) {
		LottieAnimation(
			modifier = Modifier.size(contentSize),
			composition = composition.value,
			progress = { animatable.progress },
		)
	}

	LaunchedEffect(
		key1 = trackInfo
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

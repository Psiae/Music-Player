package com.flammky.musicplayer.ui.playbackdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.musicplayer.R
import com.flammky.musicplayer.ui.Theme
import com.flammky.musicplayer.ui.util.compose.NoRipple
import kotlinx.coroutines.launch

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
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(Theme.backgroundColor())
			.statusBarsPadding()
	) {
		NoRipple {
			DetailToolbar(dismiss)
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
				.size(size)
				.clickable(interactionSource, null, onClick = dismiss),
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
			verticalArrangement = Arrangement.Bottom,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			PlaybackControlProgressSeekbar(
				positionState = viewModel.positionStreamFlow.collectAsState(),
				trackState = viewModel.trackStreamFlow.collectAsState(),
				seek = { requestIndex, requestPosition -> viewModel.seek(requestIndex, requestPosition) }
			)
			PlaybackControls()
			Spacer(modifier = Modifier
				.fillMaxWidth()
				.height(2f / 10 * maxHeight))
		}
	}
}

@Composable
private fun PlaybackControls() {
	PlaybackControlButtons()
}

@Composable
private fun PlaybackControlButtons() {
	Row(
		modifier = Modifier
			.fillMaxWidth(0.9f)
			.height(40.dp),
		horizontalArrangement = Arrangement.SpaceAround,
		verticalAlignment = Alignment.CenterVertically
	) {
		// Shuffle
		Box(modifier = Modifier.size(40.dp)) {
			Icon(
				modifier = Modifier.size(30.dp),
				painter = painterResource(id = R.drawable.ios_glyph_shuffle_100),
				contentDescription = "shuffle",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}

		// Previous
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			Icon(
				modifier = Modifier.size(35.dp),
				painter = painterResource(id = R.drawable.ios_glyph_seek_previos_100),
				contentDescription = "previous",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}

		// Play / Pause
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			Icon(
				modifier = Modifier.size(40.dp),
				painter = painterResource(id = R.drawable.ios_glyph_play_100),
				contentDescription = "play",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}

		// Next
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			Icon(
				modifier = Modifier.size(35.dp),
				painter = painterResource(id = R.drawable.ios_glyph_seek_next_100),
				contentDescription = "next",
				tint = Theme.dayNightAbsoluteContentColor()
			)
		}

		// Repeat
		Box(modifier = Modifier.size(40.dp)) {
			// later check for command availability
			Icon(
				modifier = Modifier.size(30.dp),
				painter = painterResource(id = R.drawable.ios_glyph_repeat_100),
				contentDescription = "repeat",
				tint = Theme.dayNightAbsoluteContentColor()
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
		interactionSource = interactionSource
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



enum class Visibility {
	VISIBLE,
	GONE
}

private inline val Visibility.isVisible
	get() = this == Visibility.VISIBLE

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

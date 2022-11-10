@file:OptIn(ExperimentalPagerApi::class)

package com.flammky.musicplayer.playbackcontrol.ui.compact

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.flammky.musicplayer.playbackcontrol.domain.model.PlaybackInfo
import com.flammky.musicplayer.playbackcontrol.ui.model.PlaylistInfo
import com.flammky.musicplayer.playbackcontrol.ui.model.TrackDisplayInfo
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.coroutineContext

@Composable
internal fun PlaylistControlPager(
	playlistState: State<PlaybackInfo.Playlist>,
	dark: Boolean,
	seek: suspend (Int) -> Unit,
	observeTrackDisplayInfo: (String) -> Flow<TrackDisplayInfo>
) {
	BoxWithConstraints {


	}
}

@Composable
private fun PlaylistPager(
	modifier: Modifier,
	playlistInfoState: State<PlaylistInfo>,
	dark: Boolean,
	seek: suspend (Int) -> Boolean,
	observeTrackDisplayInfo: (String) -> Flow<TrackDisplayInfo>
) {
	val playlistInfo by playlistInfoState
	val pagerState = rememberPagerState(playlistInfo.index.clampPositive())

	HorizontalPager(
		modifier = modifier,
		count = playlistInfo.list.size,
	) {
		val flow = observeTrackDisplayInfo(playlistInfo.list[playlistInfo.index])
		TrackDescription(
			trackState = flow.collectAsState(initial = TrackDisplayInfo.UNSET),
			dark = dark
		)
	}

	PagerStateListeners(
		pagerState = pagerState,
		playlistInfoState = playlistInfoState,
		seek = seek
	)
}

@Composable
private fun PagerStateListeners(
	pagerState: PagerState,
	playlistInfoState: State<PlaylistInfo>,
	seek: suspend (Int) -> Boolean
) {
	val rememberUserTouch = remember { mutableStateOf(false) }

	ListenPlaybackIndexChange(
		pagerState = pagerState,
		indexState = playlistInfoState.rememberDerive(derive = { it.index }),
		onScroll = {
			coroutineContext.ensureActive()
			rememberUserTouch.value = false
		}
	)

	ListenUserDrag(
		pagerState = pagerState,
		onUserStartDrag = {
			rememberUserTouch.value = true
		},
		onUserStopDrag = {
			// Impl
		}
	)

	ListenPageSeek(
		pagerState = pagerState,
		touchedState = rememberUserTouch,
		playlistInfoState = playlistInfoState,
		seek = seek
	)
}

@Composable
private fun ListenPlaybackIndexChange(
	pagerState: PagerState,
	indexState: State<Int>,
	onScroll: suspend () -> Unit
) {
	val index by indexState
	LaunchedEffect(
		key1 = index,
	) {
		// should do Velocity check, if the user is dragging the pager but aren't moving
		// or if the user drag velocity will end in another page
		if (index >= 0 &&
			pagerState.currentPage != index &&
			!pagerState.isScrollInProgress
		) {
			onScroll()
			val snap = index !in pagerState.currentPage - 2 .. pagerState.currentPage + 2
			if (snap) pagerState.scrollToPage(index) else pagerState.animateScrollToPage(index)
		}
	}
}

@Composable
private fun ListenUserDrag(
	pagerState: PagerState,
	onUserStartDrag: () -> Unit,
	onUserStopDrag: () -> Unit
) {
	val rememberDragging = remember { mutableStateOf(false) }
	val dragging by pagerState.interactionSource.collectIsDraggedAsState()

	if (dragging) {
		rememberDragging.value = true
		onUserStartDrag()
	} else if (rememberDragging.value) {
		rememberDragging.value = false
		onUserStopDrag()
	}
}

@Composable
private fun ListenPageSeek(
	pagerState: PagerState,
	touchedState: State<Boolean>,
	playlistInfoState: State<PlaylistInfo>,
	seek: suspend (Int) -> Boolean
) {
	val dragged by pagerState.interactionSource.collectIsDraggedAsState()
	val touched by touchedState
	LaunchedEffect(
		key1 = dragged,
		key2 = pagerState.currentPage,
		key3 = pagerState.isScrollInProgress,
	) {
		if (!dragged && touched &&
			pagerState.currentPage != playlistInfoState.value.index
		) {
			@Suppress("ControlFlowWithEmptyBody")
			if (seek(pagerState.currentPage)) {
				// Impl
			} else {
				// Impl
			}
		}
	}
}

@Composable
private fun TrackDescription(
	trackState: State<TrackDisplayInfo>,
	dark: Boolean,
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.SpaceAround,
		horizontalAlignment = Alignment.Start,
	) {
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight(0.5f)
		) {
			Title(
				fontSize = with(LocalDensity.current) { constraints.maxHeight.toSp() * 0.74f },
				textState = trackState.rememberDerive(derive = { it.title }),
				dark = dark
			)
		}
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
		) {
			Subtitle(
				fontSize = with(LocalDensity.current) { constraints.maxHeight.toSp() * 0.7f },
				textState = trackState.rememberDerive(derive = { it.subtitle }),
				dark = dark
			)
		}
	}
}


@Composable
private fun Title(
	textState: State<String>,
	fontSize: TextUnit,
	dark: Boolean
) {
	Text(
		modifier = Modifier.fillMaxWidth(),
		text = textState.value,
		fontSize = fontSize,
		fontWeight = FontWeight.SemiBold,
		color = if (dark) Color.Black else Color.White
	)
}

@Composable
private fun Subtitle(
	textState: State<String>,
	fontSize: TextUnit,
	dark: Boolean
) {
	Text(
		modifier = Modifier.fillMaxWidth(),
		text = textState.value,
		fontSize = fontSize,
		fontWeight = FontWeight.Medium,
		color = if (dark) Color.Black else Color.White
	)
}

@Composable
private fun <T, R> State<T>.rememberDerive(derive: (T) -> R): State<R> {
	return remember { derivedStateOf { derive(this.value) } }
}

private fun Int.clampPositive() = if (this < 0) 0 else this

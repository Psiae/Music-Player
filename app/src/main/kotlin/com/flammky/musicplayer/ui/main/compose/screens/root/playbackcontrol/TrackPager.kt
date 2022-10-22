package com.flammky.musicplayer.ui.main.compose.screens.root.playbackcontrol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxMetadata
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@Composable
internal fun TrackDescriptionPager(
	modifier: Modifier,
	viewModel: PlaybackBoxViewModel,
	backgroundLuminance: State<Float>,
) {
	Box(modifier = modifier) {
		val streamFlowState = viewModel.playlistStreamFlow.collectAsState()
		TrackDescriptionHorizontalPager(
			index = streamFlowState.read().currentIndex,
			tracks = streamFlowState.read().list,
			viewModel = viewModel,
			backgroundLuminance = backgroundLuminance
		)
	}
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TrackDescriptionHorizontalPager(
	index: Int,
	tracks: List<String>,
	viewModel: PlaybackBoxViewModel,
	backgroundLuminance: State<Float>,
) {
	if (index < 0) return
	require(index < tracks.size) {
		"Playing Index($index) > size($tracks), must ensure playback info delivery is consistent on both"
	}

	val pagerState = rememberPagerState()

	LaunchedEffect(key1 = index) {
		pagerState.animateScrollToPage(page = index)
	}
	LaunchedEffect(key1 = pagerState.currentPage, key2 = pagerState.isScrollInProgress) {
		if (!pagerState.isScrollInProgress) {
			pagerState.animateScrollToPage(pagerState.currentPage)
			if (pagerState.currentPage != index) viewModel.seekIndex(pagerState.currentPage)
		}
	}

	HorizontalPager(
		modifier = Modifier.fillMaxSize(),
		count = tracks.size,
		state = pagerState
	) { page: Int ->
		TrackDescriptionPagerItem(id = tracks[page], viewModel = viewModel, backgroundLuminance.read())
	}
}

@Composable
private fun TrackDescriptionPagerItem(
	id: String,
	viewModel: PlaybackBoxViewModel,
	backgroundLuminance: Float,
) {
	require(backgroundLuminance in 0f..1f) {
		"BackgroundLuminance($backgroundLuminance) not in 0f..1f"
	}
	val metadataState = remember { mutableStateOf(PlaybackBoxMetadata()) }
	val coroutineScope = rememberCoroutineScope()

	DisposableEffect(key1 = id) {
		val collector = coroutineScope.launch {
			viewModel.observeBoxMetadata(id).safeCollect { metadataState.value = it }
		}
		onDispose { collector.cancel() }
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(start = 10.dp, end = 10.dp),
		verticalArrangement = Arrangement.SpaceEvenly,
		horizontalAlignment = Alignment.Start,
	) {
		val textColor = if (backgroundLuminance < 0.4f) Color.White else Color.Black
		val metadata = metadataState.read()
		val style = MaterialTheme.typography.bodyMedium
			.copy(
				color = textColor,
				fontWeight = FontWeight.SemiBold
			)
		Text(
			modifier = Modifier.align(Alignment.Start),
			text = metadata.title,
			style = style,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		val style2 = MaterialTheme.typography.bodyMedium
			.copy(
				color = textColor,
				fontWeight = FontWeight.Medium
			)
		Text(
			modifier = Modifier.align(Alignment.Start),
			text = metadata.subtitle,
			style = style2,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}


private fun <T> State<T>.read(): T = value

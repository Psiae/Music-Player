package dev.dexsr.klio.library.user.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import dev.dexsr.klio.base.isUNSET
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.LocalIsThemeDark
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Surface
import dev.dexsr.klio.base.theme.md3.compose.surfaceColorAsState
import dev.dexsr.klio.base.theme.md3.compose.surfaceColorAtElevation
import dev.dexsr.klio.base.theme.md3.compose.surfaceVariantColorAsState
import dev.dexsr.klio.library.media.PlaylistTrackArtwork
import dev.dexsr.klio.library.media.PlaylistTrackMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun PlaylistDetailScreen(
	modifier: Modifier = Modifier,
	playlistId: String
) { key(playlistId) { Box(
	modifier
		.fillMaxSize()
		.localMaterial3Surface()
) { Column {
	val state = rememberPlaylistDetailScreenState(playlistId = playlistId)

	run {
		val lazyLayoutState = rememberPlaylistDetailLazyLayoutState(
			playlistId = playlistId,
			orderedMeasure = true
		)
		when (val type = "list") {
			"list" -> PlaylistDetailScreenLazyColumn(
				Modifier,
				contentPadding = PaddingValues(
					top = LocalLayoutVisibility.Top.current,
					bottom = LocalLayoutVisibility.Bottom.current
				),
				state = lazyLayoutState
			)
			"grid" -> PlaylistDetailScreenLazyGrid(
				Modifier,
				contentPadding = PaddingValues(),
			)
		}
	}
} }}}

@Composable
private fun  PlaylistDetailScreenLazyColumn(
	modifier: Modifier = Modifier,
	state: PlaylistDetailLazyLayoutState,
	layoutState: LazyListState = rememberLazyListState(),
	contentPadding: PaddingValues,
) {
	val renderData = state.renderData
		?: return
	LazyColumn(
		modifier = modifier
			.fillMaxSize(),
		state = layoutState,
		contentPadding = contentPadding,
		// we can force remeasurement by updating this lambda
		content = {
			items(
				renderData.playlistTotalTrack,
				key = { i -> renderData.peekContent(i) ?: i },
				contentType = { i -> renderData.peekContent(i)?.let { "A" } ?: "B" }
			) { i ->
				val id = renderData.getContent(i)
				Box(modifier = Modifier
					.height(56.dp)
					.fillMaxWidth()) {
					PlaylistDetailScreenLazyListItem(
						modifier = Modifier,
						observeArtwork = id?.let {
							{ state.observeTrackArtwork(id) }
						} ?: { flowOf() },
						getCachedArtwork = id?.let {
							{ state.cachedTrackArtwork(id) }
						} ?: { null },
						observeMetadata = id?.let {
							{ state.observeTrackMetadata(id) }
						} ?: { flowOf() },
						getCachedMetadata = id?.let {
							{ state.cachedTrackMetadata(id) }
						} ?: { null },
					)
				}
			}
		}
	)
}

@Composable
private fun PlaylistDetailScreenLazyGrid(
	modifier: Modifier = Modifier,
	state: LazyGridState = rememberLazyGridState(),
	contentPadding: PaddingValues,
) {
	// TODO: impl
}


// TODO: share placeholder progress between items
// TODO: remove the deprecated accompanist-placeholder use and implement our own
@Composable
private fun PlaylistDetailScreenLazyListItem(
	modifier: Modifier = Modifier,
	observeArtwork: () -> Flow<PlaylistTrackArtwork>,
	getCachedArtwork: () -> PlaylistTrackArtwork?,
	observeMetadata: () -> Flow<PlaylistTrackMetadata>,
	getCachedMetadata: () -> PlaylistTrackMetadata?
) {
	val surfaceColor = localShimmerSurface()
	val highlightColor = localShimmerColor()
	val ctx = LocalContext.current
	Box(
		modifier
			.background(MD3Theme.surfaceColorAtElevation(elevation = 1.dp))
			.padding(4.dp)
			.fillMaxHeight()
			.fillMaxWidth()
	) {
		Row(modifier = Modifier.padding(horizontal = 4.dp)) {
			run {
				val artwork = run {
					val emitter = remember(observeArtwork, calculation = observeArtwork)
					val initial = remember(getCachedArtwork, calculation = getCachedArtwork)
					emitter.collectAsState(initial = initial).value
				}
				val imgRequest = remember(artwork) {
					if (artwork == null || artwork.isUNSET || artwork.isNone) return@remember null
					ImageRequest
						.Builder(ctx)
						.data(artwork.localImage.value)
						.build()
				}
				AsyncImage(
					modifier = Modifier
						.fillMaxHeight()
						.aspectRatio(1f, true)
						.placeholder(
							visible = artwork == null,
							highlight = PlaceholderHighlight.shimmer(highlightColor),
							color = surfaceColor
						),
					model = imgRequest,
					contentDescription = ""
				)
				Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(2)))
			}
			Column(
				modifier = Modifier
					.fillMaxHeight(),
				verticalArrangement = Arrangement.Center
			) {
				val metadata = run {
					val emitter = remember(observeMetadata, calculation = observeMetadata)
					val initial = remember(getCachedMetadata, calculation = getCachedMetadata)
					emitter.collectAsState(initial = initial).value
				}
				if (metadata == null) {
					Box(
						modifier = Modifier
							.width(200.dp)
							.height(12.dp)
							.placeholder(
								visible = true,
								highlight = PlaceholderHighlight.shimmer(highlightColor),
								color = surfaceColor
							)
					)
					Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(1)))
					Box(
						modifier = Modifier
							.width(200.dp)
							.height(12.dp)
							.placeholder(
								visible = true,
								highlight = PlaceholderHighlight.shimmer(highlightColor),
								color = surfaceColor
							)
					)
				} else {
					val textColor: Color = MD3Theme.backgroundContentColorAsState().value
					val style = with(MaterialTheme.typography.bodyMedium) {
						copy(
							color = textColor,
							fontWeight = FontWeight.Medium
						)
					}
					val style2 = with(MaterialTheme.typography.bodySmall) {
						copy(
							color = textColor,
							fontWeight = FontWeight.Normal
						)
					}
					Text(
						modifier = Modifier
							.fillMaxWidth()
							.wrapContentHeight(),
						text = metadata.title,
						style = style,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)

					Spacer(modifier = Modifier.height(3.dp))

					val formattedDuration = remember(metadata.duration) {
						val seconds = metadata.duration.inWholeSeconds
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

					val separator = remember {
						String("\u00b7".toByteArray(Charsets.UTF_8))
					}

					Text(
						modifier = Modifier
							.fillMaxWidth()
							.wrapContentHeight(),
						text = formattedDuration + " " +
							separator + " " +
							metadata.subtitle,
						style = style2,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
				}
			}
		}
	}
}


@Composable
private fun PlaylistDetailScreenLazyListItem(
	modifier: Modifier = Modifier,
) {
	val surfaceColor = localShimmerSurface()
	val highlightColor = localShimmerColor()
	Box(
		modifier
			.background(MD3Theme.surfaceColorAtElevation(elevation = 1.dp))
			.padding(4.dp)
			.fillMaxHeight()
			.fillMaxWidth()
	) {
		Row(modifier = Modifier.padding(horizontal = 4.dp)) {
			Box(
				modifier = Modifier
					.fillMaxHeight()
					.aspectRatio(1f, true)
					.placeholder(
						visible = true,
						highlight = PlaceholderHighlight.shimmer(highlightColor),
						color = surfaceColor
					)
			)
			Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(2)))
			Column(
				modifier = Modifier
					.fillMaxHeight(),
				verticalArrangement = Arrangement.Center
			) {
				Box(
					modifier = Modifier
						.width(200.dp)
						.height(12.dp)
						.placeholder(
							visible = true,
							highlight = PlaceholderHighlight.shimmer(highlightColor),
							color = surfaceColor
						)
				)
				Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(1)))
				Box(
					modifier = Modifier
						.width(200.dp)
						.height(12.dp)
						.placeholder(
							visible = true,
							highlight = PlaceholderHighlight.shimmer(highlightColor),
							color = surfaceColor
						)
				)
			}
		}
	}
}


@Composable
fun localShimmerSurface(): Color {
	val svar = MD3Theme.surfaceVariantColorAsState().value
	val s = MD3Theme.surfaceColorAsState().value
	return remember(svar, s) {
		s.copy(alpha = 0.35f).compositeOver(svar)
	}
}

@Composable
fun localShimmerColor(): Color {
	val sf = localShimmerSurface()
	val isDark = LocalIsThemeDark.current
	val content = if (isDark) {
		MD3Theme.backgroundContentColorAsState().value
	} else {
		Color(0xFFCCCCCC)
	}
	return remember(sf, content) {
		content.copy(alpha = 0.45f).compositeOver(sf)
	}
}

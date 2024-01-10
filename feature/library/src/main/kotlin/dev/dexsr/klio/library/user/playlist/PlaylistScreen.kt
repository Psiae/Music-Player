package dev.dexsr.klio.library.user.playlist

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.NoInlineBox
import dev.dexsr.klio.base.compose.nonScaledFontSize
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.DefaultMaterial3Theme
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.base.theme.md3.compose.blackOrWhite
import dev.dexsr.klio.base.theme.md3.compose.blackOrWhiteContent
import dev.dexsr.klio.base.theme.md3.compose.dpMarginIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Surface
import dev.dexsr.klio.library.compose.Playlist
import dev.dexsr.klio.library.compose.toStablePlaylist
import dev.dexsr.klio.library.shared.LocalMediaArtwork
import dev.dexsr.klio.media.playlist.LocalPlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.math.ceil

@Composable
fun YourPlaylistScreen(
	modifier: Modifier,

) {
	val repo = remember {
		LocalPlaylistRepository()
	}
	val detailState = remember {
		mutableStateOf<String?>(null)
	}
	YourPlaylistScreen(
		modifier = modifier
			.fillMaxSize(),
		observePlaylist = {
			flow {
				repo
					.observeChanges("device_songlist")
					.map { playlist -> listOf(playlist.toStablePlaylist())  }
					.collect(this)
			}
		},
		contentPaddingValues = run {
			// fixme: remove magic number
			val margin = MD3Theme.dpMarginIncrementsOf(1, 600.dp)
			PaddingValues(
				start = margin,
				end = margin,
				// fixme consume these
				top = LocalLayoutVisibility.Top.current + margin,
				bottom = LocalLayoutVisibility.Bottom.current + margin
			)
		},
		onClick = { detailState.value = it }
	)
	detailState.value?.let { detail ->
		PlaylistDetailScreen(playlistId = detail)
		// TODO
		BackHandler {
			detailState.value = null
		}
	}
}

@Composable
fun YourPlaylistScreen(
	modifier: Modifier,
	observePlaylist: () -> Flow<List<Playlist>>,
	contentPaddingValues: PaddingValues,
	onClick: ((String) -> Unit)?
) {
	Box(modifier.fillMaxSize()) {
		val gridState = rememberLazyGridState()
		val playlists = remember(observePlaylist) {
			observePlaylist.invoke()
		}.collectAsState(initial = emptyList()).value
		LazyVerticalGrid(
			modifier = Modifier
				.fillMaxSize(),
			columns = GridCells.Adaptive(90.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			state = gridState,
			contentPadding = contentPaddingValues
		) {
			items(
				playlists.size,
				key = { playlists[it].id },

			) { i ->
				val playlist = playlists[i]
				when (playlist.id) {
					// fixme: magic literal
					"device_songlist" -> DeviceFilesPlaylistCard(
						modifier = Modifier
							.fillMaxWidth()
							.defaultMinSize(90.dp),
						displayName = "Local Files",
						trackCount = playlist.contentCount,
						onClick = onClick?.let { { onClick(playlist.id) } }
					)
					else -> {
						YourPlaylistGridItem(
							modifier = Modifier
								.fillMaxWidth()
								.defaultMinSize(90.dp),
							displayName = playlist.displayName,
							trackCount = playlist.contentCount,
							getTrackId = { trackIndex -> "" },
							getTrackArt = { flowOf(LocalMediaArtwork.UNSET) },
							getCreatorName = { flowOf() },
							onClick = {}
						)
					}
				}
			}
		}
	}
}

@Composable
private fun YourPlaylistGridItem(
	modifier: Modifier,
	displayName: String,
	trackCount: Int,
	getTrackId: (Int) -> String,
	getTrackArt: (Int) -> Flow<LocalMediaArtwork>,
	getCreatorName: () -> Flow<String>,
	onClick: (() -> Unit)?,
) {
	val upOnClick = rememberUpdatedState(newValue = onClick)
	val ctx = LocalContext.current
	Box(
		modifier = modifier
	) {
		Column(
			modifier = Modifier
				.wrapContentSize()
				.clickable(
					enabled = upOnClick.value != null,
					onClick = { upOnClick.value?.invoke() }
				)
		) {
			NoInlineBox(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1f)
					.clip(RoundedCornerShape(4.dp))
					.background(Color(0xFFC2C2C2))
					.clickable(
						enabled = upOnClick.value != null,
						onClick = { upOnClick.value?.invoke() }
					)
			) {
				if (trackCount == 0) {
					return@NoInlineBox
				}
				if (trackCount == 1) {
					val artState = remember {
						// maybe: check if there's a non blocking cache
						mutableStateOf<LocalMediaArtwork?>(null, neverEqualPolicy())
					}
					LaunchedEffect(
						getTrackArt,
						block = {
							getTrackArt.invoke(0).collect { art ->
								artState.value = art
							}
						}
					)
					val art = artState.value
					val req = remember(art) {
						ImageRequest
							.Builder(ctx)
							.data(art?.image?.value)
							.build()
					}
					AsyncImage(
						modifier = Modifier.fillMaxSize(),
						model = req,
						contentDescription = null,
						contentScale = if (art?.allowTransform == true) {
							ContentScale.Crop
						} else {
							ContentScale.Fit
						}
					)
					return@NoInlineBox
				}
				Column {
					val rowCount = ceil(trackCount / 2f).toInt().coerceAtMost(2)
					val itemPerRow = 2
					repeat(rowCount) { rowIndex ->
						val fillHeightFraction = if (rowIndex == 0) 0.5f else 1f
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.fillMaxHeight(fillHeightFraction)
						) {
							val firstItemIndexInRow = itemPerRow * rowIndex
							repeat(
								itemPerRow
									.coerceAtMost(trackCount - firstItemIndexInRow)
							) { i ->
								val index = firstItemIndexInRow + i
								val id = getTrackId(index)
								// expect trackID to be unique among the playlist
								key(id) {
									val artState = remember {
										// maybe: check if there's a non blocking cache
										mutableStateOf<LocalMediaArtwork?>(null, neverEqualPolicy())
									}
									LaunchedEffect(
										getTrackArt,
										block = {
											getTrackArt.invoke(0).collect { art ->
												artState.value = art
											}
										}
									)
									val art = artState.value
									val req = remember(art) {
										ImageRequest
											.Builder(ctx)
											.data(art?.image?.value)
											.build()
									}
									AsyncImage(
										modifier = Modifier.fillMaxSize(),
										model = req,
										contentDescription = null,
										contentScale = if (art?.allowTransform == true) {
											ContentScale.Crop
										} else {
											ContentScale.Fit
										}
									)
								}
							}
						}
					}
				}
			}
			BasicText(
				modifier = Modifier.fillMaxWidth(),
				maxLines = 2,
				text = displayName,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme3.typography.labelMedium.let { style ->
					style.copy(
						fontSize = style.nonScaledFontSize(),
						color = MD3Theme.blackOrWhiteContent()
					)
				},
			)
			Row(
				horizontalArrangement = Arrangement.spacedBy(1.dp)
			) {
				BasicText(
					modifier = Modifier.weight(1f, fill = false),
					maxLines = 1,
					text = "${trackCount}",
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme3.typography.labelMedium.let { style ->
						style.copy(
							fontSize = style.nonScaledFontSize(),
							color = run {
								val tint = MD3Theme.blackOrWhiteContent().copy(alpha = 0.85f)
								val surface = MD3Theme.blackOrWhite()
								remember(tint, surface) { tint.compositeOver(surface) }
							},
						)
					},
				)
				BasicText(
					modifier = Modifier,
					maxLines = 1,
					text = if (trackCount > 1) "tracks" else "track",
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme3.typography.labelMedium.let { style ->
						style.copy(
							fontSize = style.nonScaledFontSize(),
							color = run {
								val tint = MD3Theme.blackOrWhiteContent().copy(alpha = 0.8f)
								val surface = MD3Theme.blackOrWhite()
								remember(tint, surface) { tint.compositeOver(surface) }
							},
						)
					},
				)
			}
		}
	}
}


@Composable
private fun DeviceFilesPlaylistCard(
	modifier: Modifier,
	displayName: String,
	trackCount: Int,
	onClick: (() -> Unit)?,
) {
	val upOnClick = rememberUpdatedState(newValue = onClick)
	val ctx = LocalContext.current
	Box(
		modifier = modifier
	) {
		Column(
			modifier = Modifier
				.wrapContentSize()
				.clickable(
					enabled = upOnClick.value != null,
					onClick = { upOnClick.value?.invoke() }
				)
		) {
			NoInlineBox(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1f)
					.clip(RoundedCornerShape(4.dp))
					.background(Color(0xFFC2C2C2))
					.clickable(
						enabled = upOnClick.value != null,
						onClick = { upOnClick.value?.invoke() }
					)
			) {
				// TODO: image
			}
			BasicText(
				modifier = Modifier.fillMaxWidth(),
				maxLines = 2,
				text = displayName,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme3.typography.labelMedium.let { style ->
					style.copy(
						fontSize = style.nonScaledFontSize(),
						color = MD3Theme.blackOrWhiteContent()
					)
				},
			)
			Row(
				horizontalArrangement = Arrangement.spacedBy(1.dp)
			) {
				BasicText(
					modifier = Modifier.weight(1f, fill = false),
					maxLines = 1,
					text = "${trackCount}",
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme3.typography.labelMedium.let { style ->
						style.copy(
							fontSize = style.nonScaledFontSize(),
							color = run {
								val tint = MD3Theme.blackOrWhiteContent().copy(alpha = 0.85f)
								val surface = MD3Theme.blackOrWhite()
								remember(tint, surface) { tint.compositeOver(surface) }
							},
						)
					},
				)
				BasicText(
					modifier = Modifier,
					maxLines = 1,
					text = if (trackCount > 1) "tracks" else "track",
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme3.typography.labelMedium.let { style ->
						style.copy(
							fontSize = style.nonScaledFontSize(),
							color = run {
								val tint = MD3Theme.blackOrWhiteContent().copy(alpha = 0.8f)
								val surface = MD3Theme.blackOrWhite()
								remember(tint, surface) { tint.compositeOver(surface) }
							},
						)
					},
				)
			}
		}
	}
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YourPlaylistItemPreview() {
	DefaultMaterial3Theme(
		dark = isSystemInDarkTheme()
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.localMaterial3Surface()
		) {
			YourPlaylistGridItem(
				modifier = Modifier.width(100.dp),
				displayName = "Local Songs",
				trackCount = 1,
				getTrackId = { "" },
				getTrackArt = { flowOf() },
				getCreatorName = { flowOf("Me") },
				onClick = {}
			)
		}
	}
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DevicePlaylistItemPreview() {
	DefaultMaterial3Theme(
		dark = isSystemInDarkTheme()
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.localMaterial3Surface()
				.padding(vertical = 16.dp, horizontal = 16.dp)
		) {
			DeviceFilesPlaylistCard(
				modifier = Modifier.width(100.dp),
				displayName = "Local Files",
				trackCount = Int.MAX_VALUE,
				onClick = {}
			)
		}
	}
}

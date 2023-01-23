package com.flammky.musicplayer.library.dump.localmedia.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.isDarkAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantContentColorAsState
import com.flammky.musicplayer.library.R
import com.flammky.musicplayer.library.dump.localmedia.data.LocalSongModel
import com.flammky.musicplayer.library.dump.ui.base.LibraryViewModel
import com.flammky.musicplayer.library.dump.ui.theme.Theme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
internal fun LocalSongListsLegacy() {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(
				color = com.flammky.musicplayer.base.theme.Theme.backgroundColorAsState().value
			),
	) {
		LocalSongListsColumn(
			vm = activityViewModel(),
			libVM = activityViewModel()
		)
	}
}

@Composable
private fun LocalSongListsColumn(
	vm: LocalSongViewModel,
	libVM: LibraryViewModel
) {
	SwipeRefresh(
		state = rememberSwipeRefreshState(isRefreshing = vm.refreshing.read()),
		onRefresh = { vm.refresh() },
		indicatorPadding = PaddingValues(top = 10.dp)
	) {
		val lazyColumnState = rememberLazyListState()
		LazyColumn() {
			val localSongs = vm.listState.read()
			itemsIndexed(
				items = localSongs,
				key = { index: Int, model: LocalSongModel -> model.id }
			) { index: Int, model: LocalSongModel ->
				Box(modifier = Modifier.padding(horizontal = 5.dp)) {
					LocalSongListsItem(
						vm,
						model
					) { vm.play(localSongs, index) }
					if (!com.flammky.musicplayer.base.theme.Theme.isDarkAsState().value) {
						Divider(
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 5.dp),
							color = com.flammky.musicplayer.base.theme.Theme.surfaceVariantColorAsState().value
						)
					}
				}
			}
			item() {
				Spacer(modifier = Modifier
					.fillMaxWidth()
					.height(LocalLayoutVisibility.LocalBottomBar.current))
			}
		}
	}
}

@Composable
private fun LocalSongListsItem(
	vm: LocalSongViewModel,
	model: LocalSongModel,
	play: () -> Unit
) {
	Box(
		modifier = Modifier
			.height(60.dp)
			.clickable { play() },
		contentAlignment = Alignment.Center
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
				.padding(5.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			ItemArtworkCard(model, vm)

			Spacer(modifier = Modifier.width(8.dp))

			ItemTextDescription(
				modifier = Modifier.weight(1f, true),
				model,
				vm,
			)

			Spacer(modifier = Modifier.width(8.dp))

			val id =
				if (isSystemInDarkTheme()) {
					R.drawable.more_vert_48px
				} else {
					R.drawable.more_vert_48px_dark
				}

			val context = LocalContext.current

			Box(
				modifier = Modifier
					.align(Alignment.CenterVertically)
					.fillMaxHeight()
					.aspectRatio(1f, true)
					.padding(5.dp)
					.clip(RoundedCornerShape(50))
					.clickable {
						Toast
							.makeText(context.applicationContext, "Coming Soon", Toast.LENGTH_SHORT)
							.show()
					}
			) {

				Icon(
					modifier = Modifier
						.align(Alignment.Center)
						.fillMaxSize(0.5f)
						.aspectRatio(1f),
					painter = painterResource(id = id),
					contentDescription = "More",
				)
			}
		}
	}
}

private val UNSET = Any()
private val LOADING = Any()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemArtworkCard(model: LocalSongModel, vm: LocalSongViewModel) {
	val context = LocalContext.current

	val art by vm.observeArtwork(model).collectAsState(Dispatchers.Main)

	Timber.d("ItemArtworkCard, model: $model, art: $art")

	val shape: Shape = remember {
		RoundedCornerShape(5)
	}

	Card(
		modifier = Modifier
			.fillMaxHeight()
			.aspectRatio(1f, true)
			.clip(shape),
		shape = shape,
		elevation = CardDefaults.elevatedCardElevation(2.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
	) {

		val imageModel = remember(art) {
			ImageRequest.Builder(context)
				.data(art)
				.memoryCachePolicy(CachePolicy.ENABLED)
				.build()
		}

		AsyncImage(
			modifier = Modifier
				.fillMaxSize()
				.placeholder(
					visible = imageModel.data === UNSET,
					color = Theme.localShimmerSurface(),
					shape = shape,
					highlight = PlaceholderHighlight.shimmer(highlightColor = Theme.localShimmerColor())
				)
				.clip(shape),
			model = imageModel,
			contentDescription = "Artwork",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun ItemTextDescription(
	modifier: Modifier,
	model: LocalSongModel,
	vm: LocalSongViewModel,
	textColor: Color = com.flammky.musicplayer.base.theme.Theme.surfaceVariantContentColorAsState().value
) {
	val coroutineScope = rememberCoroutineScope()
	val coroutineContext = Dispatchers.Main + SupervisorJob()

	val metadata = remember { mutableStateOf<AudioMetadata?>(AudioMetadata.UNSET) }

	DisposableEffect(key1 = model) {
		val job = coroutineScope.launch(coroutineContext) {
			vm.collectMetadata(model).collect {
				metadata.overwrite(it)
			}
		}
		onDispose { job.cancel() }
	}

	val formattedDuration = remember(metadata.read()?.duration) {
		val seconds = metadata.value?.duration?.inWholeSeconds ?: 0
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

	val separator = String("\u00b7".toByteArray(Charsets.UTF_8))


	Column(
		modifier = modifier.fillMaxHeight(),
		verticalArrangement = Arrangement.Center
	) {
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
				.wrapContentHeight()
				.placeholder(
					visible = metadata.value === AudioMetadata.UNSET,
					color = Theme.localShimmerSurface(),
					highlight = PlaceholderHighlight.shimmer(Theme.localShimmerColor())
				),
			text = metadata.read()?.title ?: model.displayName ?: "",
			style = style,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		Spacer(modifier = Modifier.height(3.dp))
		Text(
			modifier = Modifier
				.fillMaxWidth()
				.wrapContentHeight()
				.placeholder(
					visible = metadata.value === AudioMetadata.UNSET,
					color = Theme.localShimmerSurface(),
					highlight = PlaceholderHighlight.shimmer(Theme.localShimmerColor())
				),
			text = formattedDuration + " " +
				separator + " " +
				(metadata.read()?.albumArtistName ?: metadata.read()?.artistName ?: ""),
			style = style2,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}

// is explicit write like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> MutableState<T>.overwrite(value: T) {
	this.value = value
}

// is explicit read like this better ?
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> State<T>.read(): T {
	return this.value
}

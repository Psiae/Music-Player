package com.flammky.musicplayer.library.localsong.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
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
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.compose.VisibilityViewModel
import com.flammky.musicplayer.library.R
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import com.flammky.musicplayer.library.ui.base.LibraryViewModel
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
				color = with(MaterialTheme.colorScheme.background) {
					copy(alpha = 0.97f).compositeOver(Color.Black)
				}
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

	val rememberScope = rememberCoroutineScope()
	val supervisorDispatcher = Dispatchers.Main.immediate + SupervisorJob()

	SwipeRefresh(
		state = rememberSwipeRefreshState(isRefreshing = vm.refreshing.value),
		onRefresh = { vm.scheduleRefresh() },
		indicatorPadding = PaddingValues(top = 10.dp)
	) {
		LazyColumn(
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			val localSongs = vm.listState.read()
			items(localSongs.size) { i ->
				val model = localSongs[i]
				LocalSongListsItem(vm, model)
			}
			item() {
				val height = activityViewModel<VisibilityViewModel>().bottomVisibilityOffset.read()
				Spacer(modifier = Modifier.fillMaxWidth().height(height))
			}
		}
	}
}

@Composable
private fun LocalSongListsItem(
	vm: LocalSongViewModel,
	model: LocalSongModel
) {
	Box(
		modifier = Modifier
			.height(60.dp)
			.clickable { vm.play(model) }
			.background(MaterialTheme.colorScheme.surface),
		contentAlignment = Alignment.Center
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
				.padding(5.dp),
			horizontalArrangement = Arrangement.SpaceAround,
		) {
			ItemArtworkCard(model, vm)

			Spacer(modifier = Modifier.width(8.dp))

			val metadata = model.mediaItem.metadata as AudioMetadata

			val formattedDuration = remember(metadata.duration) {
				val seconds = metadata.duration?.inWholeSeconds ?: 0
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

			ItemTextDescription(
				modifier = Modifier.weight(1f, true),
				title = model.displayName ?: "",
				subtitle = "$formattedDuration " + "$separator ${metadata.artistName ?: "<unknown artist>"}",
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
					.weight(0.2f, true)
			) {

				Icon(
					modifier = Modifier
						.align(Alignment.Center)
						.fillMaxSize(0.5f),
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
	val coroutineScope = rememberCoroutineScope()
	val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()

	val art = remember { mutableStateOf<Any?>(UNSET) }

	DisposableEffect(key1 = model) {
		val job = coroutineScope.launch(coroutineContext) {
			vm.collectArtwork(model).safeCollect {
				Timber.d("ItemArtworkCard collected $it")
				art.overwrite(it)
			}
		}
		onDispose { job.cancel() }
	}

	val imageModel = remember(art.read()) {
		ImageRequest.Builder(context)
			.data(art.read())
			.crossfade(true)
			.memoryCachePolicy(CachePolicy.ENABLED)
			.build()
	}

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

		val shimmerBackground =
			if (isSystemInDarkTheme()) {
				Color.Black
			} else {
				Color.White
			}

		val shimmerColor =
			if (isSystemInDarkTheme()) {
				Color.DarkGray
			} else {
				Color.Gray
			}

		AsyncImage(
			modifier = Modifier
				.fillMaxSize()
				.clip(shape)
				.placeholder(
					visible = imageModel.data === UNSET,
					color = shimmerBackground,
					highlight = PlaceholderHighlight.shimmer(shimmerColor)
				),
			model = imageModel,
			contentDescription = "Artwork",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun ItemTextDescription(
	modifier: Modifier,
	title: String,
	subtitle: String,
	textColor: Color = if (isSystemInDarkTheme()) Color.White else Color.Black
) {
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
			text = title,
			style = style,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		Spacer(modifier = Modifier.height(3.dp))
		Text(
			text = subtitle,
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
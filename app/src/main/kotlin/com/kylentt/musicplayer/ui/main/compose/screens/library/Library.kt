package com.kylentt.musicplayer.ui.main.compose.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.musicplayer.ui.main.compose.local.MainProvider
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper
import jp.wasabeef.transformers.coil.CenterCropTransformation
import timber.log.Timber

@Composable
fun Library() {
	Column(
		modifier = Modifier.fillMaxSize()
	) {
		LibraryTopBar()
		LibraryContent()
	}
}

@Composable
private fun LibraryTopBar() {
	Box(
		modifier = Modifier
			.fillMaxWidth(),
		contentAlignment = Alignment.Center
	) {
		val typography = MaterialTheme.typography.headlineSmall
		Text(
			text = "Library",
			color = ColorHelper.textColor(),
			fontSize = typography.fontSize,
			fontWeight = typography.fontWeight,
			fontStyle = typography.fontStyle
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent() {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val storeOwner = context as ViewModelStoreOwner
	val vm: LibraryViewModel = hiltViewModel(storeOwner)
	val mediaVM: MediaViewModel = MainProvider.mediaViewModel
	val mainVM = MainProvider.mainViewModel

	val addPadding = mediaVM.playbackControlModel.showSelf
	val localSongs = vm.localSongs

	Timber.d("Library Content recomposed")

	SwipeRefresh(
		state = rememberSwipeRefreshState(isRefreshing = vm.refreshing.value),
		onRefresh = { vm.requestRefresh() },
		indicatorPadding = PaddingValues(top = 10.dp)
	) {

		Column {
			LazyVerticalGrid(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight()
					.padding(top = 10.dp, start = 10.dp, end = 10.dp),
				columns = GridCells.Fixed(2),
				verticalArrangement = Arrangement.spacedBy(10.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				items(localSongs.value.size) {

					val item = localSongs.value[it]
					val data = item.artState.value

					Card(
						modifier = Modifier
							.size(140.dp)
							.clickable { vm.playSong(item) }
						,
					) {
						Box(contentAlignment = Alignment.BottomCenter) {
							val req = remember(data.hashCode()) {
								ImageRequest.Builder(context)
									.data(data)
									.crossfade(true)
									.transformations(CenterCropTransformation())
									.build()
							}

							AsyncImage(
								modifier = Modifier
									.fillMaxSize()
									.placeholder(
										visible = !item.isArtLoaded,
										color = ColorHelper.tonePrimarySurface(elevation = 2.dp)
									)
								,
								model = req,
								contentDescription = null,
								contentScale = ContentScale.Crop
							)
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.background(
										Brush.verticalGradient(
											colors = listOf(
												Color.Transparent,
												Color.Black.copy(alpha = 0.8f),
											)
										)
									),
								contentAlignment = Alignment.BottomCenter,
							) {
								val typography = MaterialTheme.typography.labelLarge
								Text(
									modifier = Modifier.fillMaxWidth(0.9f),
									text = item.displayName,
									color = Color.White,
									style = typography,
									textAlign = TextAlign.Center,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis
								)
							}
						}
					}
				}
				items(2) {
					Spacer(
						modifier = Modifier
							.fillMaxWidth()
							.height(mainVM.scrollableExtraSpacerDp.value)
					)
				}
			}
		}

		LaunchedEffect(key1 = true) {
			lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				Timber.d("RefreshLocalSongOnResume")
				vm.validateLocalSongs()
			}
		}
	}
}

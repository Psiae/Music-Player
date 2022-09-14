package com.flammky.musicplayer.ui.main.compose.screens.library.old

import androidx.activity.ComponentActivity
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.flammky.mediaplayer.domain.viewmodels.MainViewModel
import com.flammky.mediaplayer.domain.viewmodels.MediaViewModel
import com.flammky.musicplayer.ui.main.compose.theme.color.ColorHelper
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
	val vm: LibraryViewModelOld = activityViewModel()
	val mediaVM: MediaViewModel = activityViewModel()
	val mainVM: MainViewModel = activityViewModel()

	Timber.d("LibraryContent recomposed")

	SwipeRefresh(
		state = rememberSwipeRefreshState(isRefreshing = vm.refreshing.value),
		onRefresh = { vm.requestRefresh() },
		indicatorPadding = PaddingValues(top = 10.dp)
	) {

		Timber.d("LibraryContent SwipeRefresh recomposed")

		Column {

			Timber.d("LibraryContent Column recomposed")

			LazyVerticalGrid(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight()
					.padding(top = 10.dp, start = 10.dp, end = 10.dp),
				columns = GridCells.Fixed(2),
				verticalArrangement = Arrangement.spacedBy(10.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {

				items(2) { Spacer(Modifier) }

				val stateList = vm.localSongs

				val localSongs = ArrayList(stateList)

				Timber.d("LibraryContent Column Grid recomposed, list: ${stateList.size}")

				items(localSongs.size) {

					Timber.d("LibraryContent Column Item $it recomposed")

					val item = localSongs[it]
					val data = item.artState.value

					Card(
						modifier = Modifier
							.size(140.dp)
							.clickable { vm.playSong(item) }
						,
					) {
						Box(contentAlignment = Alignment.BottomCenter) {

							val req = remember(data) {
								if (data == null) return@remember null

								ImageRequest.Builder(context)
									.data(data)
									.crossfade(true)
									.transformations(CenterCropTransformation())
									/*.listener(onError = { request: ImageRequest, result: ErrorResult ->
										val reqData = request.data
										if (reqData is File && !reqData.exists()) vm.requestRefresh()
									})*/
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
							.height(mainVM.bottomNavigatorHeight.value)
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

@Composable
private inline fun <reified T: ViewModel>activityViewModel(): T {
	val context = LocalContext.current
	require(context is ComponentActivity)
	return viewModel(context)
}

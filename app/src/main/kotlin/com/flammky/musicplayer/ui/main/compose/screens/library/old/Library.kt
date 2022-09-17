package com.flammky.musicplayer.ui.main.compose.screens.library.old

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.android.x.lifecycle.viewmodel.compose.activityViewModel
import com.flammky.common.kotlin.comparable.clamp
import com.flammky.mediaplayer.domain.viewmodels.MainViewModel
import com.flammky.musicplayer.R
import com.flammky.musicplayer.ui.main.compose.theme.color.ColorHelper
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import jp.wasabeef.transformers.coil.CenterCropTransformation
import timber.log.Timber

@Composable
fun Library() {
	val navController: NavHostController = rememberNavController()

	Column(
		modifier = Modifier.fillMaxSize()
	) {
		LibraryContent(navController)
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

@Composable
private fun LibraryContent(navController: NavHostController) {
	val vm: LibraryViewModelOld = activityViewModel()
	val lifecycleOwner = LocalLifecycleOwner.current

	NavHost(navController = navController, "main") {
		composable(route = "main") {
			LocalSongs(vm = vm, controller = navController)
		}
		composable(route = "localSongLists") {
			LocalSongLists()
		}
	}

	LaunchedEffect(key1 = true) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
			vm.validateLocalSongs()
		}
	}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalSongs(vm: LibraryViewModelOld, controller: NavController) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	Column(
		modifier = Modifier.padding(15.dp),
		verticalArrangement = Arrangement.spacedBy(15.dp)
	) {

		val headerStyle = MaterialTheme.typography.titleLarge
			.copy(fontWeight = FontWeight.Bold)

		Text(
			modifier = Modifier.fillMaxWidth(),
			text = "Your Device Song",
			style = headerStyle,
			color = ColorHelper.textColor(),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)

		FlowRow(
			modifier = Modifier.fillMaxWidth(),
			mainAxisSize = SizeMode.Expand,
			mainAxisAlignment = MainAxisAlignment.SpaceAround,
			mainAxisSpacing = 10.dp,
			crossAxisAlignment = FlowCrossAxisAlignment.Start,
			crossAxisSpacing = 15.dp
		) {

			val stateList = vm.localSongs
			val localSongs = stateList.toList()

			repeat(localSongs.size.clamp(0, 6)) { index ->

				Timber.d("LibraryContent Column Item $index recomposed")

				val item = localSongs[index]
				val data = item.artState.value

				Column(
					modifier = Modifier
						.width(100.dp)
						.height(100.dp)
						.clip(shape =  RoundedCornerShape(5))
						.background(
							if (isSystemInDarkTheme()) {
								Color(0xFF27292D)
							} else {
								Color(0xFFF0F4F5)
							}
						),
					verticalArrangement = Arrangement.Top,
					horizontalAlignment = Alignment.CenterHorizontally
				) {

					when {
						index < 5 -> {
							Card(
								modifier = Modifier
									.size(100.dp)
									.clip(
										shape = RoundedCornerShape(5)
									)
									.clickable { vm.playSong(item) }
									.background(MaterialTheme.colorScheme.surfaceVariant),
								elevation = CardDefaults.elevatedCardElevation(2.dp),
								shape = RoundedCornerShape(5),
							) {
								val req = remember(data) {
									if (data == null) return@remember null
									ImageRequest.Builder(context)
										.data(data)
										.crossfade(true)
										.transformations(CenterCropTransformation())
										.build()
								}
								Box {
									AsyncImage(
										modifier = Modifier
											.fillMaxSize()
											.placeholder(
												visible = !item.isArtLoaded,
												color = ColorHelper.tonePrimarySurface(elevation = 2.dp)
											),
										model = req,
										contentDescription = null,
										contentScale = ContentScale.Crop
									)
									val alpha = 0.50F
									val shadowColor = Color.Black
									val textColor = Color.White
									Box(
										modifier = Modifier
											.align(Alignment.BottomCenter)
											.fillMaxWidth()
											.background(
												Brush.verticalGradient(
													colors = listOf(
														shadowColor.copy(alpha = alpha - 0.50f),
														shadowColor.copy(alpha = alpha - 0.40f),
														shadowColor.copy(alpha = alpha - 0.30f),
														shadowColor.copy(alpha = alpha - 0.20f),
														shadowColor.copy(alpha = alpha - 0.10f),
														shadowColor.copy(alpha = alpha)
													)
												)
											),
										contentAlignment = Alignment.BottomCenter,
									) {
										Column {
											val typography = MaterialTheme.typography.labelMedium
											Spacer(modifier = Modifier.height(2.dp))
											Text(
												modifier = Modifier.fillMaxWidth(0.85f),
												text = item.displayName,
												color = textColor,
												style = typography,
												textAlign = TextAlign.Center,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis
											)
											Spacer(modifier = Modifier.height(2.dp))
										}
									}
								}
							}
						}
						index == 5 -> {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.clip(RoundedCornerShape(5))
									.clickable {
										controller.navigate("localSongLists")
									}
							) {
								Card(
									modifier = Modifier
										.size(100.dp)
										.clip(RoundedCornerShape(10)),
									elevation = CardDefaults.cardElevation(2.dp),
									shape = RoundedCornerShape(10)
								) {
									Box {
										Column {
											var i = 0
											repeat(2) { rowIndex ->
												i += rowIndex
												Row(
													modifier = Modifier
														.fillMaxWidth()
														.height(50.dp)
												) {
													repeat(2) { eIndex ->
														i += eIndex
														val currentIndex = 5 + i
														val maybeItem = if (localSongs.size >= currentIndex) {
															localSongs[currentIndex]
														} else {
															null
														}
														val maybeData = maybeItem?.artState?.value
														val req = remember(maybeData) {
															if (maybeItem?.noArt == true) return@remember R.drawable.blu_splash
															ImageRequest.Builder(context)
																.data(maybeData)
																.crossfade(true)
																.build()
														}
														Box(
															modifier = Modifier
																.height(50.dp)
																.width(50.dp)
														) {
															AsyncImage(
																modifier = Modifier
																	.fillMaxSize()
																	.placeholder(
																		visible = maybeData != null && !maybeItem.isArtLoaded,
																		color = ColorHelper.tonePrimarySurface(elevation = 2.dp)
																	),
																model = req,
																contentDescription = null,
																contentScale = ContentScale.Crop
															)
														}
													}
												}
											}
										}
										val shadowColor = Color.Black
										Box(
											modifier = Modifier
												.fillMaxSize()
												.background(shadowColor.copy(alpha = 0.8f))
												.padding(bottom = 2.dp)
										) {
											val textColor = Color.White
											Text(
												modifier = Modifier
													.align(Alignment.Center)
													.widthIn(max = 96.dp)
													.padding(horizontal = 2.dp),
												text = "${localSongs.size - 5}",
												color = textColor,
												style = MaterialTheme.typography.titleMedium,
												textAlign = TextAlign.Center,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis,
											)
											val typography = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
											Text(
												modifier = Modifier
													.align(Alignment.BottomCenter)
													.fillMaxWidth(0.9f)
													.heightIn(20.dp),
												text = "See more",
												color = textColor,
												style = typography,
												textAlign = TextAlign.Center,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis
											)
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalSongLists() {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val vm: LibraryViewModelOld = activityViewModel()
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
	}
}

package com.flammky.musicplayer.library.dump.ui.root

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flammky.android.content.context.ContextHelper
import com.flammky.androidx.content.context.findBase
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.NoInlineBox
import com.flammky.musicplayer.base.compose.rewrite
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasLevel
import com.flammky.musicplayer.library.R
import com.flammky.musicplayer.library.dump.localmedia.ui.LocalSongDisplay
import com.flammky.musicplayer.library.dump.localmedia.ui.LocalSongNavigator
import com.flammky.musicplayer.library.presentation.entry.PermGuard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf

@Composable
@Deprecated("Rewrite")
internal fun LibraryRoot() {
	ApplyBackground()
	LibraryRootNavigation()
}

@Composable
private fun LibraryRootNavigation(
	navController: NavHostController = rememberNavController()
) {
	NavHost(
		navController = navController,
		startDestination = "content"
	) {
		composable("content") {
			LibraryRootContent(navController)
		}
		with(LocalSongNavigator) { addLocalSongDestinations() }
	}
}

@Composable
private fun LibraryRootContent(
	navController: NavHostController
) {
	val tabsNavHostController = rememberNavController()

	Column(modifier = Modifier
		.fillMaxSize()
		.statusBarsPadding()) {
		NoInlineBox {
			val tabsNavBackstackEntry = tabsNavHostController.currentBackStackEntryAsState().value
			if (tabsNavBackstackEntry != null) {
				val selectedRowState = remember {
					mutableStateOf(-1)
				}.rewrite {
					when (tabsNavBackstackEntry.destination.route) {
						"local" -> 0
						"spotify" -> 1
						"youtube" -> 2
						else -> -1
					}
				}
				// TODO: Hardcoding is bad, thus we should rewrite these ourselves
				TabRow(
					selectedTabIndex = selectedRowState.value,
					indicator = { tabPositions ->
						val selectedTabIndex = selectedRowState.value
						if (selectedTabIndex !in tabPositions.indices) {
							return@TabRow
						}
						val currentTabPosition = tabPositions[selectedTabIndex]
						val currentTabWidth by animateDpAsState(
							targetValue = currentTabPosition.width,
							animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
						)
						val indicatorOffset by animateDpAsState(
							targetValue = currentTabPosition.left,
							animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
						)
						TabRowDefaults.Indicator(
							Modifier
								.wrapContentSize(Alignment.BottomStart)
								.offset(x = indicatorOffset + (currentTabPosition.width / 2 - 45.dp))
								.width(90.dp)
						)
					}
				) {
					LeadingIconTab(
						selected = selectedRowState.value == 0,
						onClick = {
							if (selectedRowState.value == 0) return@LeadingIconTab
							tabsNavHostController.navigate("local")
							selectedRowState.value = 0
						},
						icon = {
							Icon(
								modifier = Modifier.size(20.dp),
								painter = painterResource(id = R.drawable.touchscreen_96),
								contentDescription = "device"
							)
						},
						text = {
							Text(
								text = "Device",
								style = MaterialTheme.typography.titleSmall,
								color = Theme.backgroundContentColorAsState().value
							)
						}
					)
					LeadingIconTab(
						selected = selectedRowState.value == 0,
						onClick = {
							if (selectedRowState.value == 1) return@LeadingIconTab
							tabsNavHostController.navigate("spotify")
							selectedRowState.value = 1
						},
						icon = {
							Icon(
								modifier = Modifier.size(20.dp),
								painter = painterResource(id = R.drawable.spotify_logo_without_text),
								contentDescription = "device",
								tint = Color.Unspecified
							)
						},
						text = {
							Text(
								text = "Spotify",
								style = MaterialTheme.typography.titleSmall,
								color = Theme.surfaceContentColorAsState().value
							)
						}
					)
					LeadingIconTab(
						selected = selectedRowState.value == 0,
						onClick = {
							if (selectedRowState.value == 2) return@LeadingIconTab
							tabsNavHostController.navigate("youtube")
							selectedRowState.value = 2
						},
						icon = {
							Icon(
								modifier = Modifier.size(20.dp),
								painter = painterResource(id = R.drawable.ytm_240),
								contentDescription = "youtube",
								tint = Color.Unspecified
							)
						},
						text = {
							Text(
								text = "Youtube",
								style = MaterialTheme.typography.titleSmall,
								color = Theme.surfaceContentColorAsState().value
							)
						}
					)
				}
			}
		}
		Spacer(modifier = Modifier.height(10.dp))
		NavHost(navController = tabsNavHostController, "local") {
			composable("local") {
				LibraryRootDeviceContents(modifier = Modifier.padding(horizontal = 10.dp)) { navController.navigate(it) }
			}
			composable("spotify") {
				Box(modifier = Modifier.fillMaxSize()) {
					Text(
						modifier = Modifier.align(Alignment.Center),
						text = "Work In Progress",
						style = MaterialTheme.typography.headlineMedium,
						color = Theme.backgroundContentColorAsState().value
					)
				}
			}
			composable("youtube") {
				Box(modifier = Modifier.fillMaxSize()) {
					Text(
						modifier = Modifier.align(Alignment.Center),
						text = "Work In Progress",
						style = MaterialTheme.typography.headlineMedium,
						color = Theme.backgroundContentColorAsState().value
					)
				}
			}
		}
	}
}

@Composable
internal fun LibraryRootDeviceContents(
	modifier: Modifier = Modifier,
	navigate: (String) -> Unit
) {
	val permState = remember {
		mutableStateOf<Boolean?>(null)
	}
	PermGuard(onPermChanged = { nBool ->
		permState.value = nBool
	})

	when (permState.value) {
		true -> {
			Column(
				modifier = modifier,
			) {
				Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
				LocalSongDisplay(
					modifier = Modifier
						.heightIn(max = 300.dp),
					viewModel = activityViewModel(),
					navigate = navigate
				)
			}
		}
		false -> @OptIn(ExperimentalPermissionsApi::class) {
			val permission =
				if (com.flammky.musicplayer.core.sdk.AndroidAPI.hasLevel(33)) {
					android.Manifest.permission.READ_MEDIA_AUDIO
				} else {
					android.Manifest.permission.READ_EXTERNAL_STORAGE
				}
			val permissionState = rememberPermissionState(permission = permission) {}
			val ctx = LocalContext.current
			val rememberLauncher = rememberLauncherForActivityResult(
				contract = ActivityResultContracts.StartActivityForResult(),
				onResult = {}
			)
			Column(
				modifier = modifier
					.fillMaxSize(),
				verticalArrangement = Arrangement.Bottom
			) {
				BasicText(
					modifier = Modifier
						.align(Alignment.CenterHorizontally)
						.fillMaxWidth(),
					text = "READ AUDIO FILES PERMISSION NOT GRANTED",
					style = MaterialTheme.typography.labelLarge.copy(
						color = Theme.backgroundContentColorAsState().value,
						textAlign = TextAlign.Center
					)
				)
				Spacer(
					modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(5))
				)
				Button(
					modifier = Modifier
						.align(Alignment.CenterHorizontally),
					enabled = !permissionState.status.isGranted,
					onClick = {
						if (permissionState.status.isGranted) {
							return@Button
						}
						if (isPermissionRequestBlocked(ctx as Activity, permission)) {
							rememberLauncher.launch(
								Intent().apply {
									action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
									data = Uri.parse("package:${ctx.packageName}")
								}
							)
							return@Button
						}
						permissionState.launchPermissionRequest()
						savePermissionRequested(ctx as Activity, permission)
					},
					colors = ButtonDefaults.elevatedButtonColors(
						containerColor = MaterialTheme.colorScheme.primaryContainer
					)
				) {
					val text = if (permissionState.status.isGranted) "Permission Granted!" else "Grant Permission"
					val style = MaterialTheme.typography.titleMedium
					Text(
						text = text,
						color = Theme.backgroundContentColorAsState().value,
						fontSize = style.fontSize,
						fontStyle = style.fontStyle
					)
				}
				Spacer(
					modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(15))
				)
				Spacer(
					modifier = Modifier.height(LocalLayoutVisibility.Bottom.current)
				)
			}
		}
		null -> {

		}
	}
}

@Composable
private fun ApplyBackground() {
	Box(modifier = Modifier
		.fillMaxSize()
		.background(MaterialTheme.colorScheme.background)
	)
}

@Composable
private fun rememberContextHelper(): ContextHelper {
	val current = LocalContext.current
	return remember(current) {
		val context = if (current is ContextWrapper) current.findBase() else current
		ContextHelper(context)
	}
}

@Composable
private fun Int.toComposeDp(): Dp {
	return with(LocalDensity.current) { toDp() }
}

@Composable
private fun Float.toComposeDp(): Dp {
	return with(LocalDensity.current) { toDp() }
}

operator fun Float.times(other: Dp): Float {
	return this * other.value
}


private fun savePermissionRequested(activity: Activity, permissionString: String) {
	val sharedPref = activity.getSharedPreferences("perm", Context.MODE_PRIVATE)
	sharedPref.edit { putBoolean(permissionString, true) }
}

private fun isPermissionRequestBlocked(activity: Activity, permission: String): Boolean {
	val sharedPref = activity.getSharedPreferences("perm", Context.MODE_PRIVATE)
	return ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
		&& !activity.shouldShowRequestPermissionRationale(permission)
		&& sharedPref.getBoolean(permission, false)
}


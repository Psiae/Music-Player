package com.kylentt.musicplayer.ui.main.compose

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.ui.main.compose.theme.MainMaterial3Theme
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper
import com.kylentt.musicplayer.ui.util.compose.PermissionHelper

class EntryViewModel() : ViewModel() {
	var shouldPersistPager: Boolean? = null
}

private val entryViewModel
	@Composable get() = viewModel<EntryViewModel>()

private val readStoragePermissionString
	get() = PermissionHelper.Permission.READ_EXTERNAL_STORAGE.androidManifestString

private val writeStoragePermissionString
	get() = PermissionHelper.Permission.WRITE_EXTERNAL_STORAGE.androidManifestString

private val readStoragePermissionGranted
	@Composable get() = isPermissionGranted(readStoragePermissionString)

private val writeStoragePermissionGranted
	@Composable get() = isPermissionGranted(writeStoragePermissionString)

private val settingIntent
	@Composable get() = Intent().apply {
		action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
		data = "package:${LocalContext.current.packageName}".toUri()
	}

private val pageItems = listOf(
	PermissionPageItem(readStoragePermissionString, R.drawable.folder_search_base_256_blu_glass, false,
		"Read Storage Permission"
	),
	PermissionPageItem(writeStoragePermissionString, R.drawable.folder_write_base_256, true,
		"Write Storage Permission"
	),
)

@Composable
fun MainEntry(content: @Composable () -> Unit) {
	CheckEntryPermission(onGranted = content)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CheckEntryPermission(onGranted: @Composable () -> Unit) {
	val entryVM = entryViewModel
	val readGranted = readStoragePermissionGranted

	val entryAllowed = remember {
		mutableStateOf(entryVM.shouldPersistPager != true && readGranted)
	}

	if (!entryAllowed.value) {
		return EntryPermissionPager { entryAllowed.value = true }
	}

	onGranted()
}

@OptIn(ExperimentalPagerApi::class)
@ExperimentalPermissionsApi
@Composable
private fun EntryPermissionPager(onGranted: () -> Unit) {

	val granted = remember {
		mutableStateOf(false)
	}

	entryViewModel.shouldPersistPager = !granted.value

	if (granted.value) {
		return onGranted()
	}

	val allPermissionGranted = remember {
		mutableStateOf(false)
	}

	val pagerState = rememberPagerState()

	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Top,
		horizontalAlignment = Alignment.CenterHorizontally
	) {

		val pagerHeightFraction = when(LocalContext.current.resources.configuration.orientation) {
			Configuration.ORIENTATION_LANDSCAPE -> 0.75F
			else -> 0.85F
		}

		HorizontalPager(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight(pagerHeightFraction),
			count = pageItems.size,
			state = pagerState,
			verticalAlignment = Alignment.Top
		) { position ->
			val result = remember {
				mutableStateOf(false, neverEqualPolicy())
			}

			val (permString: String, resId: Int, optional: Boolean, title: String) = pageItems[position]

			val state = rememberPermissionState(permission = permString) {
				result.value = it
			}

			val rememberLauncher = rememberLauncherForActivityResult(
				contract = ActivityResultContracts.StartActivityForResult(),
				onResult = {}
			)

			val settingIntent = settingIntent

			PermissionPage(
				resId = resId,
				description = title + if (optional) " (Optional)" else " (Required)",
				isGranted = state.status.isGranted
			) {
				if (state.status.shouldShowRationale) {
					state.launchPermissionRequest()
				} else {
					rememberLauncher.launch(settingIntent)
				}
			}

			allPermissionGranted.value = pageItems
				.filter { !it.optional }
				.all { isPermissionGranted(it.permissionString) }
		}

		Column(
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.Top,
			horizontalAlignment = Alignment.CenterHorizontally
		) {

			Box(
				modifier = Modifier.fillMaxSize(0.3F),
				contentAlignment = Alignment.Center
			) {
				HorizontalPagerIndicator(pagerState = pagerState)
			}

			Row(
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically
			) {
				AnimatedVisibility (
					visible = allPermissionGranted.value,
				) {
					Button(
						modifier = Modifier.fillMaxWidth(0.7f),
						onClick = { granted.value = true },
						colors = ButtonDefaults
							.elevatedButtonColors(MaterialTheme.colorScheme.primaryContainer)
					) {
						val style = MaterialTheme.typography
						Text(
							text = "Let's Go",
							color = ColorHelper.textColor(),
							fontSize = style.titleMedium.fontSize
						)
					}
				}
			}
		}
	}
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun PermissionPage(
	resId: Int,
	description: String,
	isGranted: Boolean,
	noinline launchPermissionRequest: () -> Unit
) {
	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top
	) {

		val fraction = when(LocalContext.current.resources.configuration.orientation) {
			Configuration.ORIENTATION_LANDSCAPE -> 0.7f
			else -> 0.8f
		}

		Image(
			modifier = Modifier
				.fillMaxSize(fraction),
			alignment = Alignment.BottomCenter,
			painter = painterResource(id = resId),
			contentDescription = "Image"
		)

		Text(text = description, color = ColorHelper.textColor())

		Box(
			modifier = Modifier
				.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			Button(
				enabled = !isGranted,
				onClick = launchPermissionRequest,
				colors = ButtonDefaults.elevatedButtonColors(
					containerColor = MaterialTheme.colorScheme.primaryContainer
				)
			) {
				val text = if (isGranted) "Permission Granted!" else "Grant Permission"
				val style = MaterialTheme.typography.titleMedium
				Text(
					text = text,
					color = ColorHelper.textColor(),
					fontSize = style.fontSize,
					fontStyle = style.fontStyle
				)
			}
		}
	}
}

@Composable
private fun isPermissionGranted(
	permissionString: String,
	context: Context = LocalContext.current
): Boolean {
	return ContextCompat
		.checkSelfPermission(context, permissionString) == PackageManager.PERMISSION_GRANTED
}

private data class PermissionPageItem(
	val permissionString: String,
	@DrawableRes val resId: Int,
	val optional: Boolean,
	val title: String,
)

@Composable
@Preview()
private fun ReadStoragePermissionScreenPreview() {
	MainMaterial3Theme(darkTheme = false) {
		Surface {
			PermissionPage(resId = R.drawable.folder_search_base_256_blu_glass,
				description = "Read Storage Permission is Required",
				isGranted = false
			) {
			}
		}
	}
}

@Composable
@Preview
private fun ReadStoragePermissionScreenPreviewDark() {
	MainMaterial3Theme(darkTheme = true) {
		Surface {
			PermissionPage(resId = R.drawable.folder_search_base_256_blu_glass,
				description = "Read Storage Permission is Required",
				isGranted = false
			) {
			}
		}
	}
}

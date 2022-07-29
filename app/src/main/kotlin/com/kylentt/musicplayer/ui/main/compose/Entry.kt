package com.kylentt.musicplayer.ui.main.compose

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.*
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.ui.main.compose.theme.MainMaterial3Theme
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper
import com.kylentt.musicplayer.ui.util.compose.PermissionHelper
import com.kylentt.musicplayer.ui.util.compose.RecomposeOnEvent

class EntryViewModel() : ViewModel() {
	var savedEntryPermission: Boolean? = null
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
	PermissionPageItem(readStoragePermissionString, R.drawable.read_folder_base_256, false,
		"Read Storage Permission"
	),
	PermissionPageItem(writeStoragePermissionString, R.drawable.write_folder_base_256, true,
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
	val context = LocalContext.current
	val entryVM = entryViewModel
	val granted = readStoragePermissionGranted

	val allow = remember {
		// Use ViewModel as it only retain on config changes
		entryVM.savedEntryPermission = entryVM.savedEntryPermission ?: granted
		mutableStateOf(entryVM.savedEntryPermission!!, structuralEqualityPolicy())
	}

	if (!allow.value) {
		return EntryPermissionPager { allow.value = true }
	}

	onGranted()
	LocalLifecycleOwner.current.lifecycle.RecomposeOnEvent(onEvent = Lifecycle.Event.ON_RESUME) {
		require(it == Lifecycle.Event.ON_RESUME)
		allow.value = PermissionHelper.checkReadStoragePermission(context)
	}
}

@OptIn(ExperimentalPagerApi::class)
@ExperimentalPermissionsApi
@Composable
private fun EntryPermissionPager(onGranted: () -> Unit) {

	val context = LocalContext.current

	val granted = remember {
		mutableStateOf(false)
	}

	if (granted.value) {
		onGranted()
		return
	}

	val allPermissionGranted = remember {
		mutableStateOf(false)
	}

	val pagerState = rememberPagerState()

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.fillMaxHeight(),
		verticalArrangement = Arrangement.Top,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		HorizontalPager(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight(0.85f),
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

		HorizontalPagerIndicator(pagerState = pagerState)

		Row(
			modifier = Modifier.fillMaxSize(),
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
		Image(
			modifier = Modifier
				.fillMaxWidth(0.5f)
				.fillMaxHeight(0.8f),
			alignment = Alignment.BottomCenter,
			painter = painterResource(id = resId),
			contentDescription = "Image"
		)

		Text(text = description, color = ColorHelper.textColor())

		Box(
			modifier = Modifier.fillMaxSize(),
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
@Preview
private fun ReadStoragePermissionScreenPreview() {
	MainMaterial3Theme(darkTheme = false) {
		Surface {
			PermissionPage(resId = R.drawable.read_folder_base_256,
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
			PermissionPage(resId = R.drawable.read_folder_base_256,
				description = "Read Storage Permission is Required",
				isGranted = false
			) {
			}
		}
	}
}

package com.kylentt.musicplayer.ui.main.compose

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.theme.MaterialDesign3Theme
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper
import com.kylentt.musicplayer.ui.util.compose.PermissionHelper

@Composable
fun MainEntry(content: @Composable () -> Unit) {
	CheckEntryPermission() {
		content()
	}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CheckEntryPermission(onGranted: @Composable () -> Unit) {
	PermissionHelper.RequirePermissions(
		permissions = listOf(
			PermissionHelper.Permission.READ_EXTERNAL_STORAGE,
			PermissionHelper.Permission.WRITE_EXTERNAL_STORAGE
		),
		showRationale = { EntryPermissionRationale(state = it) },
		whenAllDenied = { EntryPermissionDenied(state = it) }
	) {
		onGranted()
	}
}

// TODO: Replace with slide able Pager and create Sealed class

@ExperimentalPermissionsApi
@Composable
private inline fun EntryPermissionRationale(state: MultiplePermissionsState) {
	EntryPermissionPage(state = state)
}

@ExperimentalPermissionsApi
@Composable
private inline fun EntryPermissionDenied(state: MultiplePermissionsState) {
	EntryPermissionPage(state = state)
}

@ExperimentalPermissionsApi
@Composable
private inline fun EntryPermissionPage(state: MultiplePermissionsState) {
	val rememberLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartActivityForResult(),
		onResult = {}
	)
	val settingIntent = Intent()
		.apply {
			action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
			data = "package:${LocalContext.current.packageName}".toUri()
		}

	state.permissions.forEach {
		when(it.permission) {
			PermissionHelper.Permission.READ_EXTERNAL_STORAGE.androidManifestString -> {
				if (!it.status.isGranted) {
					PermissionPage(
						R.drawable.read_folder_base_256,
						description = "Read Storage Permission is Required",
						isGranted = it.status.isGranted
					) {
						if (it.status.shouldShowRationale) {
							it.launchPermissionRequest()
						} else {
							rememberLauncher.launch(settingIntent)
						}
					}
					return
				}
			}
			PermissionHelper.Permission.WRITE_EXTERNAL_STORAGE.androidManifestString -> {
				PermissionPage(
					R.drawable.write_folder_base_256,
					description = "Write Storage Permission is Required",
					isGranted = it.status.isGranted
				) {
					if (it.status.shouldShowRationale) {
						it.launchPermissionRequest()
					} else {
						rememberLauncher.launch(settingIntent)
					}
				}
				return
			}
		}
	}
}

@Composable
private inline fun PermissionPage(
	resId: Int,
	description: String,
	isGranted: Boolean,
	noinline launchPermissionRequest: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.fillMaxHeight(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top
	) {
		Image(
			modifier = Modifier
				.fillMaxWidth(0.5f)
				.fillMaxHeight(0.7f),
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
				colors = ButtonDefaults.filledTonalButtonColors()
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
@Preview
private fun ReadStoragePermissionScreenPreview() {
	MaterialDesign3Theme {
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
	MaterialDesign3Theme(useDarkTheme = true) {
		Surface {
			PermissionPage(resId = R.drawable.read_folder_base_256,
				description = "Read Storage Permission is Required",
				isGranted = false
			) {
			}
		}
	}
}

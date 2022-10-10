package com.flammky.musicplayer.ui.main.compose.entry

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.android.app.permission.AndroidPermission
import com.flammky.android.content.context.ContextInfo
import com.flammky.android.content.context.rememberContextInfo
import com.flammky.musicplayer.R
import com.flammky.musicplayer.ui.main.compose.theme.MainMaterial3Theme
import com.flammky.musicplayer.ui.main.compose.theme.color.ColorHelper
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Deferred

class EntryViewModel() : ViewModel() {
	var shouldPersistPager: Boolean? = null
}

private val entryViewModel: EntryViewModel
	@Composable
	get() = viewModel()

private val readStoragePermission = AndroidPermission.Read_External_Storage
private val writeStoragePermission = AndroidPermission.Write_External_Storage

private val pageItems = listOf(
	PermissionPageItem(
		readStoragePermission, resId = R.drawable.folder_search_base_256_blu_glass,
		optional = false, title = "Read Storage Permission"
	),
	PermissionPageItem(
		writeStoragePermission, resId = R.drawable.folder_write_base_256,
		optional = true, title = "Write Storage Permission"
	),
)

@Composable
fun MainEntry(content: @Composable () -> Unit) {
	if (entryPermissionAsState().value) {
		content()
	}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun entryPermissionAsState(): State<Boolean> {

	val contextInfo = rememberContextInfo()

	val entryVM = entryViewModel

	val readStorageGranted = contextInfo.permissionInfo.readExternalStorageAllowed

	val entryAllowed = remember {
		mutableStateOf(entryVM.shouldPersistPager != true && readStorageGranted)
	}

	if (!entryAllowed.value) {
		EntryPermissionPager(contextInfo) { entryAllowed.value = true }
	}

	return entryAllowed
}

@OptIn(ExperimentalPagerApi::class)
@ExperimentalPermissionsApi
@Composable
private fun EntryPermissionPager(contextInfo: ContextInfo, onGranted: () -> Unit) {

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
		modifier = Modifier
			.navigationBarsPadding()
			.fillMaxSize(),
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

			val (perm: AndroidPermission, resId: Int, optional: Boolean, title: String) =
				pageItems[position]

			val state = rememberPermissionState(permission = perm.manifestPath) {
				result.value = it
			}

			val rememberLauncher = rememberLauncherForActivityResult(
				contract = ActivityResultContracts.StartActivityForResult(),
				onResult = {}
			)

			PermissionPage(
				resId = resId,
				description = title + if (optional) " (Optional)" else " (Required)",
				isGranted = state.status.isGranted
			) {
				if (state.status.shouldShowRationale) {
					state.launchPermissionRequest()
				} else {
					rememberLauncher.launch(contextInfo.commonIntent.appDetailSetting)
				}
			}

			allPermissionGranted.value = pageItems
				.filter { !it.optional }
				.all { contextInfo.isPermissionGranted(it.permission) }
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
    val permission: AndroidPermission,
    @DrawableRes val resId: Int,
    val optional: Boolean,
    val title: String,
)

@Composable
@Preview()
private fun ReadStoragePermissionScreenPreview() {
	MainMaterial3Theme(darkTheme = false) {
		val state = remember {
			mutableStateOf(false)
		}

		Surface {
			PermissionPage(resId = R.drawable.folder_search_base_256_blu_glass,
				description = "Read Storage Permission is Required",
				isGranted = state.value
			) {
				state.value = !state.value
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
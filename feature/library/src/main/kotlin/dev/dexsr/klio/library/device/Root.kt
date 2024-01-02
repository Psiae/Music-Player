package dev.dexsr.klio.library.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.base.compose.NoInlineColumn
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.core.sdk.AndroidAPI
import dev.dexsr.klio.core.sdk.AndroidBuildVersion.hasLevel
import dev.dexsr.klio.library.R
import com.flammky.musicplayer.library.dump.ui.root.LibraryRootDeviceContents
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.primaryColorAsState
import dev.dexsr.klio.base.theme.md3.margin
import dev.dexsr.klio.library.user.RootYourLibraryPanel

@Composable
internal fun DeviceRootContent(
	modifier: Modifier,
	navigate: (String) -> Unit
) {
	val hasDevicePermission = PlatformLibraryUI.hasQueryAudioFilesPermission()

	NoInlineColumn(
		modifier
			.fillMaxSize()
			.padding(horizontal = MD3Spec.margin.MARGIN_INCREMENTS_VALUE_COMPACT.dp)
			.verticalScroll(rememberScrollState())
	) {

		Spacer(modifier = Modifier.height(LocalLayoutVisibility.Top.current))
		Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))

		if (!hasDevicePermission) {
			CompositionLocalProvider(
				LocalLayoutVisibility.Bottom provides 0.dp
			) {
				Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
				RequestQueryAudioFilesPermissionPanel(modifier = Modifier.heightIn(max = 300.dp))
			}
			Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
			Spacer(modifier = Modifier.height(LocalLayoutVisibility.Bottom.current))
			return@NoInlineColumn
		}

		CompositionLocalProvider(
			LocalLayoutVisibility.Bottom provides 0.dp
		) {
			if (hasDevicePermission) {
				// TODO: redo
				LibraryRootDeviceContents(modifier = Modifier.heightIn(max = 300.dp), navigate = navigate)
				Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(5)))
			}
		}

		RootYourLibraryPanel(
			openPlaylist = { navigate("library.user.playlists") }
		)

		if (hasDevicePermission) {
			Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(5)))
			RootDeviceLibraryPanel()
		}

		if (!hasDevicePermission) {
			Column {
				Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(5)))
				Divider()
				Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
				Row(
					horizontalArrangement = Arrangement.spacedBy(MD3Theme.dpPaddingIncrementsOf(1)),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = "Device Library",
						style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
						color = Theme.backgroundContentColorAsState().value
					)
					Icon(
						modifier = Modifier.size(14.dp),
						painter = painterResource(id = R.drawable.touchscreen_96),
						contentDescription = null,
						tint = MD3Theme.primaryColorAsState().value
					)
				}
				RequestQueryAudioFilesPermissionPanel(modifier = Modifier.heightIn(max = 300.dp))
			}
		}

		Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
		Spacer(modifier = Modifier.height(LocalLayoutVisibility.Bottom.current))
	}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestQueryAudioFilesPermissionPanel(
	modifier: Modifier
) {
	val permission =
		if (AndroidAPI.hasLevel(33)) {
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
				.fillMaxWidth()
				.alpha(0.68f),
			text = "READ AUDIO FILES PERMISSION NOT GRANTED",
			style = MaterialTheme.typography.labelLarge.copy(
				color = Theme.backgroundContentColorAsState().value,
				textAlign = TextAlign.Center,
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

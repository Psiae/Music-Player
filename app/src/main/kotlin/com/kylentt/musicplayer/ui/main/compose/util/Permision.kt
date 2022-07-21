package com.kylentt.musicplayer.ui.main.compose.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import timber.log.Timber

object PermissionHelper {

	sealed class Permission {

		object READ_EXTERNAL_STORAGE : Permission() {
			override val androidManifestString: String
				get() = android.Manifest.permission.READ_EXTERNAL_STORAGE
		}

		object WRITE_EXTERNAL_STORAGE : Permission() {
			override val androidManifestString: String
				get() = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
		}

		abstract val androidManifestString: String
	}

	fun checkContextPermission(context: Context, permission: Permission): Boolean {
		return checkContextPermission(context, permission.androidManifestString)
	}

	fun checkContextPermission(context: Context, permission: String): Boolean {
		val status = ContextCompat.checkSelfPermission(context, permission)
		return status == PackageManager.PERMISSION_GRANTED
	}

	@ExperimentalPermissionsApi
	@Composable
	inline fun RequirePermission(
		permission: Permission,
		showRationale: @Composable (PermissionState) -> Unit,
		whenDenied: @Composable (PermissionState) -> Unit,
		whenGranted: @Composable (PermissionState) -> Unit
	) {
		val context = LocalContext.current

		val currentStatus = checkContextPermission(context, permission)

		val requestResult = remember {
			mutableStateOf(currentStatus, policy = neverEqualPolicy())
		}

		val permissionState = rememberPermissionState(
			permission = permission.androidManifestString
		) { result ->
			requestResult.value = result
		}

		Timber.d(
			"RequirePermission Composable," +
				"\nmanifest String: ${permission.androidManifestString}" +
				"\nhasPermission: $currentStatus" +
				"\nresult: ${requestResult.value}" +
				"\nstatus: ${permissionState.status}"
		)

		when {
			currentStatus -> {
				if (permissionState.status.isGranted) {
					whenGranted(permissionState)
				} else {
					Timber.e("Possible Inconsistency in @ExperimentalPermissionsApi")
				}
			}
			!requestResult.value -> {
				if (permissionState.status.shouldShowRationale) {
					showRationale(permissionState)
				} else {
					whenDenied(permissionState)
				}
			}
			else -> {
				throw IllegalStateException(
					"Should Never Reach Here," +
						"\npermissionResult = ${requestResult.value}," +
						"\nshouldShowRationale = ${permissionState.status.shouldShowRationale}"
				)
			}
		}
	}
}

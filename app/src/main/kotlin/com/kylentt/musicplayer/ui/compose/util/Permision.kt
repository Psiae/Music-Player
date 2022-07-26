package com.kylentt.musicplayer.ui.compose.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DoNotInline
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

	private fun checkContextPermission(context: Context, permission: String): Boolean {
		return ContextCompat.checkSelfPermission(
			context,
			permission
		) == PackageManager.PERMISSION_GRANTED
	}

	@ExperimentalPermissionsApi
	@DoNotInline
	@Composable
	fun RequirePermission(
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
				if (!permissionState.status.isGranted) {
					Timber.e(
						"Possible Inconsistency in @ExperimentalPermissionsApi " +
							"for SinglePermissionState"
					)
				}
				whenGranted(permissionState)
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
					"Should Never Reach Here,"
						+ "\npermissionResult = ${requestResult.value},"
						+ "\nshouldShowRationale = ${permissionState.status.shouldShowRationale}"
				)
			}
		}
	}

	@ExperimentalPermissionsApi
	@DoNotInline
	@Composable
	fun RequirePermissions(
		permissions: List<Permission>,
		showRationale: @Composable (MultiplePermissionsState) -> Unit,
		whenAllDenied: @Composable (MultiplePermissionsState) -> Unit,
		whenAllGranted: @Composable (MultiplePermissionsState) -> Unit
	) {
		val context = LocalContext.current

		val currentStatus = permissions.associate {
			it.androidManifestString to checkContextPermission(context, it)
		}

		val permissionResults = remember {
			mutableStateOf(currentStatus.toMap(), policy = neverEqualPolicy())
		}

		val permissionStates = rememberMultiplePermissionsState(
			permissions = permissions.map { it.androidManifestString }
		) {
			permissionResults.value = it
		}

		Timber.d(
			"RequirePermissions Composable," +
				"\nmanifest String: ${permissions.map { it.androidManifestString }}" +
				"\nhasPermission: $currentStatus" +
				"\nresult: ${permissionResults.value}" +
				"\nstatus: ${permissionStates.allPermissionsGranted}"
		)

		when {
			currentStatus.all { it.value } || permissions.isEmpty() -> {
				if (!permissionStates.allPermissionsGranted) {
					Timber.w(
						"Possible inconsistency in @ExperimentalPermissionsAPI "
							+ "for MultiplePermissionState"
					)
				}
				whenAllGranted(permissionStates)
			}
			permissionResults.value.any { !it.value } -> {
				if (permissionStates.shouldShowRationale) {
					showRationale(permissionStates)
				} else {
					whenAllDenied(permissionStates)
				}
			}
			else -> throw IllegalStateException()
		}
	}
}

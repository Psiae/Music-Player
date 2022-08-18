package com.kylentt.musicplayer.ui.util.compose

import androidx.annotation.DoNotInline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*
import com.kylentt.musicplayer.BuildConfig
import com.kylentt.musicplayer.common.android.context.rememberContextInfo
import com.kylentt.musicplayer.core.app.permission.AndroidPermission
import timber.log.Timber

@ExperimentalPermissionsApi
@DoNotInline
@Composable
fun RequirePermission(
	permission: AndroidPermission,
	showRationale: @Composable (PermissionState) -> Unit,
	whenDenied: @Composable (PermissionState) -> Unit,
	whenGranted: @Composable (PermissionState) -> Unit
) {

	val contextInfo = rememberContextInfo()

	val context = LocalContext.current

	val currentStatus = contextInfo.permissionInfo.isPermissionGranted(permission)

	val requestResult = remember {
		mutableStateOf(currentStatus, policy = neverEqualPolicy())
	}

	val permissionState = rememberPermissionState(
		permission = permission.manifestPath
	) { result ->
		requestResult.value = result
	}

	Timber.d(
		"RequirePermission Composable," +
			"\nmanifest String: ${permission.manifestPath}" +
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
	permissions: List<AndroidPermission>,
	showRationale: @Composable (MultiplePermissionsState) -> Unit,
	whenAllDenied: @Composable (MultiplePermissionsState) -> Unit,
	whenAllGranted: @Composable (MultiplePermissionsState) -> Unit
) {

	val contextInfo = rememberContextInfo()

	val currentStatus = permissions.associate {
		it.manifestPath to contextInfo.isPermissionGranted(it)
	}

	val permissionResults = remember {
		mutableStateOf(currentStatus.toMap(), policy = neverEqualPolicy())
	}

	val permissionStates = rememberMultiplePermissionsState(
		permissions = permissions.map { it.manifestPath }
	) {
		permissionResults.value = it
	}

	Timber.d(
		"RequirePermissions Composable," +
			"\nmanifest String: ${permissions.map { it.manifestPath }}" +
			"\nhasPermission: $currentStatus" +
			"\nresult: ${permissionResults.value}" +
			"\nstatus: ${permissionStates.allPermissionsGranted}"
	)

	when {
		currentStatus.all { it.value } -> {
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
		else -> {
			if (BuildConfig.DEBUG) {
				throw IllegalStateException()
			} else {
				whenAllDenied(permissionStates)
			}
		}
	}
}

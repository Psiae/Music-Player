package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.runtime.*
import com.google.accompanist.permissions.*
import com.kylentt.musicplayer.core.app.delegates.device.StoragePermissionHelper
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
inline fun RequireStorage(
    whenDenied: @Composable (permission: PermissionState) -> Unit,
    whenShowRationale: @Composable (permission: PermissionState) -> Unit,
    whenGranted: @Composable (permission: PermissionState) -> Unit,
) {
    val hasStoragePermission by StoragePermissionHelper

    val permissionResult = remember {
        mutableStateOf(hasStoragePermission, policy = neverEqualPolicy())
    }

	val permission = rememberPermissionState(
		permission = StoragePermissionHelper.Write_External_Storage
	) {
		permissionResult.value = it
	}

    Timber.d("RequireStorage Composable," +
        "\nhasPermission: $hasStoragePermission," +
        "\nresult: ${permissionResult.value}" +
        "\nstatus: ${permission.status},"
    )

    when {
		permission.status.isGranted
			&& hasStoragePermission -> whenGranted(permission)
        !permissionResult.value
            && permission.status.shouldShowRationale
            && !hasStoragePermission -> whenShowRationale(permission)
        !permissionResult.value
            && !permission.status.shouldShowRationale
            && !hasStoragePermission -> whenDenied(permission)
        else -> {
            throw IllegalStateException(
                "Should Never Reach Here," +
                    "\npermissionResult = ${permissionResult.value}," +
                    "\nshouldShowRationale = ${permission.status.shouldShowRationale}"
            )
        }
    }
}

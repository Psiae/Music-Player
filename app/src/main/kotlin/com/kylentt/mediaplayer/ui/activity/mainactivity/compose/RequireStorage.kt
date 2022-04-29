package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.runtime.*
import com.google.accompanist.permissions.*
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
inline fun RequireStorage(
    whenDenied: @Composable (state: PermissionState) -> Unit,
    whenShowRationale: @Composable (state: PermissionState) -> Unit,
    whenGranted: @Composable (state: PermissionState) -> Unit,
) {
    val hasStoragePermission by StoragePermissionDelegate

    val permissionResult = remember {
        mutableStateOf(hasStoragePermission, policy = neverEqualPolicy())
    }

    val permission = rememberPermissionState(
        permission = StoragePermissionDelegate.Write_External_Storage
    ) { result ->
        permissionResult.value = result
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
            && !hasStoragePermission -> {
            whenShowRationale(permission)
        }
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

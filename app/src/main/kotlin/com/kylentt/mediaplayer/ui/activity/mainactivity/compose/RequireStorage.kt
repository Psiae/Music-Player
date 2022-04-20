package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.*
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate

@OptIn(ExperimentalPermissionsApi::class)
@Composable
inline fun RequireStorage(
    whenDenied: @Composable (state: PermissionState) -> Unit,
    whenShowRationale: @Composable (state: PermissionState) -> Unit,
    whenGranted: @Composable (state: PermissionState) -> Unit,
) {
    val hasStoragePermission by StoragePermissionDelegate
    val permissionResult = remember { mutableStateOf(false, policy = neverEqualPolicy()) }
    val permission = rememberPermissionState(
        permission = StoragePermissionDelegate.Write_External_Storage
    ) { result ->
        permissionResult.value = result
    }

    when {
        permission.status.isGranted && hasStoragePermission -> {
            whenGranted(permission)
        }
        !permissionResult.value && permission.status.shouldShowRationale -> {
            whenShowRationale(permission)
        }
        !permissionResult.value && !permission.status.shouldShowRationale -> {
            whenDenied(permission)
        }
        else -> {
            throw IllegalStateException(
                "Should Never Reach Here," +
                    "\npermissionResult = ${permissionResult.value}," +
                    "\nshouldShowRationale = ${permission.status.shouldShowRationale}"
            )
        }
    }
}

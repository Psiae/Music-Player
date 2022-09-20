package com.flammky.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.*
import com.flammky.android.app.AppDelegate
import com.flammky.android.app.permission.AndroidPermission
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
inline fun RequireStorage(
    whenDenied: @Composable (permission: PermissionState) -> Unit,
    whenShowRationale: @Composable (permission: PermissionState) -> Unit,
    whenGranted: @Composable (permission: PermissionState) -> Unit,
) {
    val hasStoragePermission = AppDelegate.permissionManager.readExternalStorageAllowed

    val permissionResult = remember {
        mutableStateOf(hasStoragePermission, policy = neverEqualPolicy())
    }

	val permission = rememberPermissionState(
		permission = AndroidPermission.Read_External_Storage.manifestPath
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

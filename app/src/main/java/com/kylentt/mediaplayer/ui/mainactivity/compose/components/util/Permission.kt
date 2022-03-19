package com.kylentt.mediaplayer.ui.mainactivity.compose.components.util

import androidx.compose.runtime.*
import com.google.accompanist.permissions.*
import com.kylentt.mediaplayer.ui.mainactivity.compose.screen.permission.PermissionScreen
import timber.log.Timber

data class ComposePermission @OptIn(ExperimentalPermissionsApi::class) constructor(
    val permissionStr: String,
    val onPermissionScreenRequest: @Composable ( () -> Unit )? = null,
    val onDenied: @Composable () -> Unit,
    val onNotDenied: @Composable () -> Unit,
    val onGranted: @Composable () -> Unit
)


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequirePermission(
    perms: ComposePermission,
) {

    val trigger = remember { mutableStateOf(false) }
    val permScreen = remember { mutableStateOf(false) }

    val permissionState = rememberPermissionState(
        permission = perms.permissionStr
    ) {
        Timber.d("ComposeDebug permissionState Result $it")
        permScreen.value = !it
        trigger.value = !trigger.value
    }

    Timber.d("ComposeDebug RequirePermission ${permissionState.notDenied()} ${permScreen.value}")

    when {
        permissionState.granted() -> { Timber.d("ComposeDebug permissionState Granted")
            perms.onGranted()
        }
        permissionState.notDenied() && !permScreen.value -> { Timber.d("ComposeDebug permissionState NotDenied")
            perms.onNotDenied()
            SideEffect {
                permissionState.launchPermissionRequest()
            }
        }
        permissionState.denied() || permScreen.value -> {
            Timber.d("ComposeDebug permissionState Denied")
            perms.onDenied()
            perms.onPermissionScreenRequest?.let {
                PermissionScreen(perms = perms, state = permissionState)
            }
        }
        trigger.value -> { Unit }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.granted() = this.status.isGranted
@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.notDenied() = this.status.shouldShowRationale
@OptIn(ExperimentalPermissionsApi::class)
fun PermissionState.denied() = !this.status.shouldShowRationale
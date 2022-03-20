package com.kylentt.mediaplayer.ui.mainactivity.compose.components.util

import androidx.compose.runtime.*
import com.google.accompanist.permissions.*
import com.kylentt.mediaplayer.ui.mainactivity.compose.screen.permission.PermissionScreen
import timber.log.Timber

data class ComposePermission (
    val permissionStr: String,
    val onDenied: @Composable () -> Unit,
    val onPermissionScreenRequest: @Composable ( () -> Unit )? = null,
    val onGrantedAfterRequest: @Composable ( () -> Unit )? = null,
    val onNotDenied: @Composable () -> Unit,
    val onGranted: @Composable () -> Unit
)

@ExperimentalPermissionsApi
fun PermissionState.granted() = status.isGranted
@ExperimentalPermissionsApi
fun PermissionState.notDenied() = status.shouldShowRationale
@ExperimentalPermissionsApi
fun PermissionState.denied() = !status.shouldShowRationale

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
        permissionState.granted() && !permScreen.value -> { Timber.d("ComposeDebug permissionState Granted")
            perms.onGranted()
        }
        permissionState.notDenied() && !permScreen.value -> { Timber.d("ComposeDebug permissionState NotDenied")
            perms.onNotDenied()
            SideEffect {
                permissionState.launchPermissionRequest()
            }
        }
        permissionState.granted() && permScreen.value -> {Timber.d("ComposeDebug permissionState Granted after Denied")
            perms.onGrantedAfterRequest?.invoke()
        }
        permissionState.denied() || permScreen.value -> {
            Timber.d("ComposeDebug permissionState Denied")
            perms.onDenied()
            perms.onPermissionScreenRequest?.let {
                PermissionScreen(perms = perms, state = permissionState)
            }
        }
        trigger.value -> { Timber.e("ComposeDebug Trigger") }
    }
}
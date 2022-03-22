package com.kylentt.mediaplayer.ui.mainactivity.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.theme.md3.DefaultColor
import timber.log.Timber

sealed class ComposePermission {

    data class SinglePermission @OptIn(ExperimentalPermissionsApi::class) constructor(
        val permissionStr: String,
        val onNotDenied: @Composable () -> Unit,
        val onDenied: @Composable (state: PermissionState) -> Unit,
        val onPermissionScreenRequest: @Composable ( () -> Unit )? = null,
        val onGrantedAfterRequest: @Composable ( () -> Unit )? = null,
        val onGranted: @Composable () -> Unit
    ) : ComposePermission()

    enum class Status {
        NotDenied,
        Denied,
        Granted
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequirePermission(
    permission: ComposePermission.SinglePermission
) {
    val trigger = remember {
        mutableStateOf(false)
    }
    val permissionStatus = remember {
        mutableStateOf(false)
    }
    val permissionBeenDenied = remember {
        mutableStateOf(false)
    }
    val permissionState = rememberPermissionState(permission = permission.permissionStr) {
        permissionStatus.value = it
        trigger.value = !trigger.value
        Timber.d("onPermissionStateResult $it")
    }

    Timber.d("permissionStateCheck ${permissionState.status.shouldShowRationale} ${permissionStatus.value} ${trigger.value}")

    when {
        permissionState.status.isGranted && !permissionBeenDenied.value -> {
            permission.onGranted()
        }
        permissionState.status.isGranted && permissionBeenDenied.value -> {
            permission.onGrantedAfterRequest?.invoke() ?: permission.onGranted()
        }
        permissionState.status.shouldShowRationale && !permissionStatus.value -> {
            permission.onNotDenied()
            SideEffect {
                permissionState.launchPermissionRequest()
            }
        }
        !permissionState.status.shouldShowRationale && !permissionStatus.value-> {
            permissionBeenDenied.value = true
            permission.onDenied(permissionState)
        }
        trigger.value -> {
            Timber.e("Trigger true")
        }
        !trigger.value -> {
            Timber.e("Trigger false")
        }
    }
}

@Composable
fun PermissionScreen(
    grantButtonText: String,
    onGrantButton: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { onGrantButton() },
                colors = ButtonDefaults.buttonColors(containerColor = DefaultColor.getTonedSurface(10))
            ) {
                Text(text = grantButtonText, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
@file:OptIn(ExperimentalPermissionsApi::class)
package com.kylentt.musicplayer.ui.musicactivity.compose.environtment

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.google.accompanist.permissions.*
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.ColorHelper
import timber.log.Timber

object PermissionDefaults {

    sealed class DeniedOptions {
        data class PermissionScreen(val grantButtonText: String = "") : DeniedOptions()
    }

    sealed class NotDeniedOptions {
        data class RequestPermission(val backgroundComposable: @Composable () -> Unit) : NotDeniedOptions()
    }
}

@Composable
inline fun RequireStoragePermission(
    whenDenied: PermissionDefaults.DeniedOptions = PermissionDefaults.DeniedOptions.PermissionScreen("Grant Storage Permission"),
    whenShowRationale: PermissionDefaults.NotDeniedOptions,
    whenGranted: () -> Unit,
) {
    val context = LocalContext.current

    val permissionResult = remember { mutableStateOf(false, policy = neverEqualPolicy()) }

    val permission = rememberPermissionState(permission = Manifest.permission.WRITE_EXTERNAL_STORAGE) {
        permissionResult.value = it
    }

    when {
        permission.status.isGranted -> {
            whenGranted()
        }
        !permissionResult.value && permission.status.shouldShowRationale -> {
            when(whenShowRationale) {
                is PermissionDefaults.NotDeniedOptions.RequestPermission -> {
                    whenShowRationale.backgroundComposable()
                    SideEffect { permission.launchPermissionRequest() }
                }
            }
        }
        !permissionResult.value && !permission.status.shouldShowRationale -> {
            when(whenDenied) {
                is PermissionDefaults.DeniedOptions.PermissionScreen -> {
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = {}
                    )
                    PermissionScreen(
                        grantButtonText = whenDenied.grantButtonText.ifEmpty { "Grant Storage Permission" }
                    ) {
                        launcher.launch(Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()
                        ))
                    }
                }
            }
        }
        else -> {
            Timber.e("permissionState should never reach here! ${permissionResult.value} ${permission.status}")
        }
    }
}

@Composable
fun PermissionScreen(
    grantButtonText: String,
    onGrantButton: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onGrantButton() },
            colors = ButtonDefaults.buttonColors(containerColor = ColorHelper.getTonedSurface(10))
        ) {
            Text(text = grantButtonText, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
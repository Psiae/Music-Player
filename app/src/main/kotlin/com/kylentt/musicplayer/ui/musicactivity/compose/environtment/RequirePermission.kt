@file:OptIn(ExperimentalPermissionsApi::class)
package com.kylentt.musicplayer.ui.musicactivity.compose.environtment

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.ColorHelper
import timber.log.Timber

object ComposePermission {

    object SinglePermission {

        object PermissionDefaults {

            @Composable
            inline fun OnNotDenied(persistent: Boolean, crossinline onLaunchPermissionRequest: () -> Unit) {
                Timber.d("permissionResult OnNotDenied Composable")
                val beenRequested = remember {
                    mutableStateOf(false)
                }
                when {
                    !beenRequested.value -> {
                        beenRequested.value = true
                        SideEffect {
                            onLaunchPermissionRequest()
                        }
                    }
                    beenRequested.value -> {
                        if (persistent) {
                            SideEffect {
                                onLaunchPermissionRequest()
                            }
                        }
                    }
                }
            }

            @Composable
            fun OnDenied(grantButtonText: String) {
                val context = LocalContext.current
                val i = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {}
                PermissionScreen(grantButtonText = grantButtonText) {
                    i.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                }
            }
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
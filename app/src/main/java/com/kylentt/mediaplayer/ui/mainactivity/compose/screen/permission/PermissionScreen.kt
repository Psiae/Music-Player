package com.kylentt.mediaplayer.ui.mainactivity.compose.screen.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.ComposePermission
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.DefaultColor

@ExperimentalPermissionsApi
@Composable
fun PermissionScreen(
    perms: ComposePermission,
    state: PermissionState
) {

    val trigger = remember { mutableStateOf(false) }

    if (trigger.value) {
        perms.onPermissionScreenRequest?.invoke()
        trigger.value = false
    }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = DefaultColor.getTonedSurface(4)
    ) {
        Column(
            modifier = Modifier
                .background(Color.Gray)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (state.status.shouldShowRationale) {
                        state.launchPermissionRequest()
                    } else {
                        trigger.value = true
                    }
                }
            ) {
                Text(text = "Grant Permission")
            }
        }
    }
}
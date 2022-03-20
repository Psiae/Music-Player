package com.kylentt.mediaplayer.ui.mainactivity.compose.screen.permission

import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.ComposePermission
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.granted
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.DefaultColor

@ExperimentalPermissionsApi
@Composable
fun PermissionScreen(
    perms: ComposePermission,
    state: PermissionState
) {

    val context = LocalContext.current
    val setting = remember {
        mutableStateOf(false)
    }

    if (setting.value) {
        perms.onPermissionScreenRequest?.invoke()
        setting.value = false
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
                    when {
                        state.status.shouldShowRationale && ContextCompat.checkSelfPermission(context,
                            perms.permissionStr) != PackageManager.PERMISSION_GRANTED -> {
                            state.launchPermissionRequest()
                        }
                        !state.status.shouldShowRationale && ContextCompat.checkSelfPermission(context,
                            perms.permissionStr) != PackageManager.PERMISSION_GRANTED -> {
                            setting.value = true
                        }
                    }
                }
            ) {
                Text(text = "Grant Permission")
            }
        }
    }
}
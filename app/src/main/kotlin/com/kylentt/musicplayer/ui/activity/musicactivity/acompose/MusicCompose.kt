package com.kylentt.musicplayer.ui.activity.musicactivity.acompose

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.FakeRoot
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.RequireStorage
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.environtment.PermissionDefaults
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.environtment.PermissionScreen
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.environtment.RequireStoragePermission
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.musicroot.MusicRoot

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal inline fun MusicComposeDefault(
    whenGranted: () -> Unit
) {
    RequireStorage(
        whenDenied = { StorageDenied(state = it) },
        whenShowRationale = { ShowStorageRationale(state = it) }
    ) {
        whenGranted()
        MusicCompose(hasPermission = true)
    }
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private inline fun StorageDenied(
    state: PermissionState
) {
    require(!state.status.isGranted)
    require(!state.status.shouldShowRationale)
    check(!AppDelegate.hasStoragePermission)
    val localContext = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    val intent = Intent()
        .apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = "package:${localContext.packageName}".toUri()
        }

    Toast.makeText(localContext, "Permission Required", Toast.LENGTH_SHORT).show()
    PermissionScreen(grantButtonText = "Grant Storage Permission") {
        launcher.launch(intent)
    }
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private inline fun ShowStorageRationale(
    state: PermissionState
) {
    require(state.status.shouldShowRationale)
    check(!AppDelegate.hasStoragePermission)
    SideEffect {
        state.launchPermissionRequest()
    }
}

@Composable
internal fun MusicCompose(
    vm: MediaViewModel = viewModel(),
    hasPermission: Boolean
) {
    val setting = vm.appSettings.collectAsState()
    if (hasPermission && setting.value.isValid) {
        MusicRoot(true)
    } else {
        FakeRoot()
    }
}

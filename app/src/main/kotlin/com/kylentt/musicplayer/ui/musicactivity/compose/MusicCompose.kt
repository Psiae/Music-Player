package com.kylentt.musicplayer.ui.musicactivity.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.FakeRoot
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.core.helper.PermissionHelper.checkPermission
import com.kylentt.musicplayer.ui.musicactivity.compose.environtment.PermissionDefaults
import com.kylentt.musicplayer.ui.musicactivity.compose.environtment.RequireStoragePermission
import com.kylentt.musicplayer.ui.musicactivity.compose.musicroot.MusicRoot

@Composable
inline fun MusicComposeDefault(
    whenGranted: () -> Unit
) {
    RequireStoragePermission(
        whenDenied = PermissionDefaults.DeniedOptions.PermissionScreen(),
        whenShowRationale = PermissionDefaults.NotDeniedOptions.RequestPermission {
            MusicCompose(hasPermission = false)
        }
    ) {
        whenGranted()
        MusicCompose(hasPermission = true)
    }
}

@Composable
fun MusicCompose(
    hasPermission: Boolean
) {
    if (PermissionHelper.checkStoragePermission(LocalContext.current) && hasPermission) {
        MusicRoot(navWallpaper = true)
    } else {
        FakeRoot()
    }
}

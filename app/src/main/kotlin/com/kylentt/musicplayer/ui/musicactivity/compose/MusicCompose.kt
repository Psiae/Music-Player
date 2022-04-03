package com.kylentt.musicplayer.ui.musicactivity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.FakeRoot
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.musicplayer.ui.musicactivity.compose.environtment.PermissionDefaults
import com.kylentt.musicplayer.ui.musicactivity.compose.environtment.RequireStoragePermission
import com.kylentt.musicplayer.ui.musicactivity.compose.musicroot.MusicRoot
import com.kylentt.musicplayer.ui.preferences.AppSettings
import com.kylentt.musicplayer.ui.preferences.WallpaperSettings

@Composable
internal inline fun MusicComposeDefault(
    whenGranted: () -> Unit
) {
    RequireStoragePermission(
        whenShowRationale = PermissionDefaults.NotDeniedOptions.RequestPermission(
            backgroundComposable = { MusicCompose(hasPermission = false) }
        )
    ) {
        whenGranted()
        MusicCompose(hasPermission = true)
    }
}

@Composable
internal fun MusicCompose(
    vm: MediaViewModel = viewModel(),
    hasPermission: Boolean
) {
    val settings = vm.appSettings.collectAsState()
    val navWallpaper = settings.value.wallpaperSettings.mode == WallpaperSettings.Mode.NAVIGATION

    if (hasPermission) {
        MusicRoot(navWallpaper)
    } else {
        FakeRoot()
    }
}

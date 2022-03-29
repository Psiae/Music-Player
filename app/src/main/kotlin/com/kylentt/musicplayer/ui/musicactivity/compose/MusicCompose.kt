package com.kylentt.musicplayer.ui.musicactivity.compose

import androidx.compose.runtime.Composable
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.ui.musicactivity.compose.musicroot.MusicRoot

@Composable
fun MusicCompose() {
    if (PermissionHelper.checkStoragePermission()) {
        MusicRoot(navWallpaper = true)
    }
}

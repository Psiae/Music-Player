package com.kylentt.musicplayer.core.helper

import android.Manifest
import com.kylentt.musicplayer.app.AppProxy

object PermissionHelper {
    fun checkStoragePermission() = AppProxy.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) or AppProxy.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
}
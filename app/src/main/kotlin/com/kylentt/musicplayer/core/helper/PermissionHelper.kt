package com.kylentt.musicplayer.core.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.kylentt.musicplayer.app.AppProxy

object PermissionHelper {

  fun checkStoragePermission(context: Context? = null) =
    with(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
      context?.checkPermission(this) ?: AppProxy.checkSelfPermission(this)
    }

  fun ComponentActivity.checkStoragePermission() = checkStoragePermission(this)
  fun Context.checkPermission(perm: String): Boolean =
    ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
}

package com.kylentt.mediaplayer.app.delegates

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaperDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate

class AppDelegate private constructor(app: Application) {

  val base = app
  val deviceWallpaper: Drawable? by DeviceWallpaperDelegate
  val storagePermission: Boolean by StoragePermissionDelegate

  fun getVectorDrawable(@DrawableRes id: Int): ImageVector {
    return ImageVector.vectorResource(base.theme, base.resources, id)
  }

  companion object {
    private lateinit var delegate: AppDelegate

    val deviceWallpaper
      get() = delegate.deviceWallpaper
    val hasStoragePermission
      get() = delegate.storagePermission

    // bypass @RequiresPermission annotation
    fun checkStoragePermission() = hasStoragePermission
    fun getVectorImage(@DrawableRes id: Int) = delegate.getVectorDrawable(id)
    fun provides(app: Application) {
      check(!::delegate.isInitialized) { "check failed, AppDelegate has been initialized" }
      delegate = AppDelegate(app)
    }
  }
}

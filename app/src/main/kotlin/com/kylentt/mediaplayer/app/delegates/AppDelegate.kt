package com.kylentt.mediaplayer.app.delegates

import android.app.Application
import android.graphics.drawable.Drawable
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaperDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate

class AppDelegate(
  val base: Application
) {

  val storagePermission: Boolean by StoragePermissionDelegate
  val deviceWallpaper: Drawable? by DeviceWallpaperDelegate

  companion object {
    private lateinit var delegate: AppDelegate

    val hasStoragePermission
      get() = delegate.storagePermission
    val deviceWallpaper
      get() = delegate.deviceWallpaper

    // bypass @RequiresPermission
    fun checkStoragePermission() = hasStoragePermission

    fun provides(base: Application) {
      delegate = AppDelegate(base)
    }
  }

}

package com.kylentt.mediaplayer.app.delegates.device

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.Drawable
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import kotlin.reflect.KProperty

object DeviceWallpaperDelegate {

  private fun getDeviceWallpaper(accessor: Context): Drawable? {
    return if (AppDelegate.checkStoragePermission()) {
      WallpaperManager.getInstance(accessor).drawable
    } else {
      null
    }
  }

  operator fun getValue(appDelegate: AppDelegate, property: KProperty<*>): Drawable? {
    return getDeviceWallpaper(appDelegate.base)
  }

  operator fun getValue(any: Any?, property: KProperty<*>): Drawable? {
    return AppDelegate.deviceWallpaper
  }
}

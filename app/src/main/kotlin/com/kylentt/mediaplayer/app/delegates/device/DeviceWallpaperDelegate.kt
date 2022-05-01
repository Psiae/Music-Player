package com.kylentt.mediaplayer.app.delegates.device

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.Drawable
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import kotlin.reflect.KProperty

/**
 * Delegate to get DeviceWallpaper, gets the Context from [AppDelegate] Class
 * Can be Delegated in Receiver that has no Context Object Available, including Composable
 * @author Kylentt
 * @since 2022/04/30
 * @return Null if there's No Storage Permission, else Drawable from WallpaperManager
 */

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

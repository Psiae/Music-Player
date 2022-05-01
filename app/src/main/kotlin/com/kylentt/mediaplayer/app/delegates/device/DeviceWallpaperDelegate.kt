package com.kylentt.mediaplayer.app.delegates.device

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.Drawable
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import kotlin.reflect.KProperty

/**
 * Delegate to get [WallpaperManager] Wallpaper as [Drawable]?, gets the [Context] from [AppDelegate] Class
 * Can be delegated inside receiver that has no [Context] Object available, including [androidx.compose.runtime.Composable]
 * @return [Drawable]? from [WallpaperManager] if this App has StoragePermission and Valid Wallpaper is set else null
 * @see StoragePermissionDelegate
 * @author Kylentt
 * @since 2022/04/30
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

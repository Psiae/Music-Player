package com.kylentt.mediaplayer.app.delegates.device

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.delegates.LateLazy
import kotlin.reflect.KProperty

/**
 * Delegate to get [Drawable]? wallpaper from [WallpaperManager]
 *
 * [Context] is available from [AppDelegate] Class
 *
 * @return [Drawable] from [WallpaperManager] if this App has StoragePermission else null
 *
 * @author Kylentt
 * @since 2022/04/30
 * @see StoragePermission
 * @sample [com.kylentt.mediaplayer.ui.compose.rememberWallpaperDrawableAsState]
 */

object DeviceWallpaper {

  private val initializer = LateLazy<WallpaperManager>()
  private val manager by initializer

  private fun getDeviceWallpaper(accessor: Context): Drawable? {
    return elseNull(AppDelegate.checkStoragePermission()) {
      val instance =
        if (!initializer.isInitialized) {
          initializer.init { WallpaperManager.getInstance(accessor) }
        } else {
          manager
        }
      instance.drawable
    }
  }

  private inline fun <T> elseNull(condition: Boolean, block: () -> T): T? {
    return if (condition) block() else null
  }

  operator fun getValue(appDelegate: AppDelegate, property: KProperty<*>): Drawable? {
    return getDeviceWallpaper(appDelegate.base)
  }

  operator fun getValue(any: Any?, property: KProperty<*>): Drawable? {
    return AppDelegate.deviceWallpaper
  }
}

object DeviceWallpaperBitmap {

  operator fun getValue(any: Any?, property: KProperty<*>): Bitmap? {
    return AppDelegate.deviceWallpaper?.toBitmap()
  }
}

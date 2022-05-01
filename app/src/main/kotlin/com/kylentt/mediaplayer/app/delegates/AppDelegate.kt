package com.kylentt.mediaplayer.app.delegates

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaperDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate

/**
 * Singleton to use if there's need to get the [Application] Class instead of casting its Context
 * @author Kylentt
 * @since 2022/04/30
 */

class AppDelegate private constructor(app: Application) {

  /**
   * My CodeStyle is to convert Function to Getter Variable if it represent a STATE of thing
   * except when theres reasonable reason for not doing so
   * @see [checkStoragePermission]
   */

  @JvmField
  val base = app

  val deviceWallpaperDrawable: Drawable? by DeviceWallpaperDelegate
  val storagePermission: Boolean by StoragePermissionDelegate

  fun getImageVectorFromDrawable(@DrawableRes id: Int): ImageVector =
    ImageVector.vectorResource(base.theme, base.resources, id)

  companion object {
    private lateinit var delegate: AppDelegate

    val deviceWallpaper
      @JvmStatic get() = delegate.deviceWallpaperDrawable

    val hasStoragePermission
      @JvmStatic get() = delegate.storagePermission

    /**
     * Bypass [androidx.annotation.RequiresPermission]
     * of [android.Manifest.permission.READ_EXTERNAL_STORAGE] and [android.Manifest.permission.WRITE_EXTERNAL_STORAGE] annotation,
     * because the getter variable couldn't do so as it check the function name
     * @see [hasStoragePermission]
     * @return [Boolean] from [StoragePermissionDelegate]
     */
    @JvmStatic fun checkStoragePermission() = hasStoragePermission

    /**
     * In case there's need to get ImageVector of a Drawable when context isn't available, e.g: Sealed Class
     * @param [id] the Int id representation of [DrawableRes]
     * @return [ImageVector] from [ImageVector.Companion.vectorResource]
     */
    @JvmStatic fun getImageVector(@DrawableRes id: Int) = delegate.getImageVectorFromDrawable(id)

    /**
     * Provide the Instance, should be done through One-Time Initializer, e.g: Androidx.startup
     * @see [com.kylentt.mediaplayer.app.dependency.AppInitializer.create]
     * @param [app] the base [Application] class to access resource from
     */
    @JvmStatic fun provides(app: Application) {
      check(!::delegate.isInitialized) { "check failed, AppDelegate was Initialized" }
      delegate = AppDelegate(app)
    }
  }
}

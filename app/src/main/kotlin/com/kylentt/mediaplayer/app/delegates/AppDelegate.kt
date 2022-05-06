package com.kylentt.mediaplayer.app.delegates

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaper
import com.kylentt.mediaplayer.app.delegates.device.StoragePermission
import com.kylentt.mediaplayer.helper.Preconditions.checkState

/**
 * Singleton to use if there's need to get the [Application] Context
 * @author Kylentt
 * @since 2022/04/30
 */

class AppDelegate private constructor(app: Application) {

  @JvmField
  val base = app

  val deviceWallpaperDrawable: Drawable? by DeviceWallpaper
  val storagePermission: Boolean by StoragePermission

  fun getImageVectorFromDrawable(@DrawableRes id: Int): ImageVector =
    ImageVector.vectorResource(base.theme, base.resources, id)

  companion object {

    private val initializer = LateLazy<AppDelegate>()

    private val delegate: AppDelegate by initializer

    @JvmStatic
    val deviceWallpaper
       get() = delegate.deviceWallpaperDrawable

    @JvmStatic
    val hasStoragePermission
       get() = delegate.storagePermission

    /**
     * Bypass [androidx.annotation.RequiresPermission]
     * anyOf [android.Manifest.permission.READ_EXTERNAL_STORAGE] and [android.Manifest.permission.WRITE_EXTERNAL_STORAGE] annotation,
     * because the getter variable couldn't do so as it check the function name
     * @return [Boolean] from [StoragePermission]
     */

    @JvmStatic fun checkStoragePermission() = hasStoragePermission

    /**
     * In case there's need to get ImageVector of a Drawable when context isn't available, e.g: Sealed Class
     * @param [id] the Int id of [DrawableRes]
     * @return [ImageVector] from [ImageVector.Companion.vectorResource]
     */

    @JvmStatic fun getImageVector(@DrawableRes id: Int) = delegate.getImageVectorFromDrawable(id)

    /**
     * Provide the Instance, should be done through One-Time Initializer, e.g: Androidx.startup
     * @see [com.kylentt.mediaplayer.app.dependency.AppInitializer.create]
     * @param [app] the base [Application] class to access resource from
     */

    @JvmStatic
    fun provides(app: Application) {
      LateLazySample.runTestCase()
      checkState(!initializer.isInitialized)
      initializer.init { AppDelegate(app) }
    }
  }
}

package com.kylentt.mediaplayer.app.delegates

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaperDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate

/**
 * Singleton to use if there's need to get the Application Class instead of casting the Context
 * Delegation seems like a good Idea
 * */

class AppDelegate private constructor(app: Application) {

  val base = app
  val deviceWallpaper: Drawable? by DeviceWallpaperDelegate
  val storagePermission: Boolean by StoragePermissionDelegate

  fun getImageVector(@DrawableRes id: Int): ImageVector =
    ImageVector.vectorResource(base.theme, base.resources, id)

  companion object {
    private lateinit var delegate: AppDelegate

    val deviceWallpaper
      get() = delegate.deviceWallpaper
    val hasStoragePermission
      get() = delegate.storagePermission

    /** bypass @RequiresPermission annotation, because the getter variable couldn't do so as it check the function name  */
    fun checkStoragePermission() = hasStoragePermission

    fun getImageVector(@DrawableRes id: Int) = delegate.getImageVector(id)

    /** provide the Instance, should be done through One-Time Initializer, e.g: Androidx.startup */
    fun provides(app: Application) {
      check(!::delegate.isInitialized) { "check failed, AppDelegate was Initialized" }
      delegate = AppDelegate(app)
    }
  }
}

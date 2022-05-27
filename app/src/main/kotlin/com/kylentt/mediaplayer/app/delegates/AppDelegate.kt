package com.kylentt.mediaplayer.app.delegates

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.core.content.ContextCompat
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaper
import com.kylentt.mediaplayer.app.delegates.device.StoragePermission
import com.kylentt.mediaplayer.core.annotation.Singleton
import com.kylentt.mediaplayer.core.delegates.LateLazy
import com.kylentt.mediaplayer.core.delegates.LateLazySample
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkState


interface ApplicationDelegate {
	val application: Application
	fun hasPermission(permission: String): Boolean
	fun hasStoragePermission(): Boolean
	fun getDeviceWallpaper(): Drawable?
	fun getImageVector(@DrawableRes id: Int): ImageVector
}

/**
 * Singleton to use if there's need to get the [Application] Context
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
@javax.inject.Singleton
class AppDelegate private constructor(app: Application) : ApplicationDelegate {

	private constructor(context: Context) : this(context.applicationContext as Application)

	private val base = app
	private val storagePermission by StoragePermission
	private val wallpaperDrawable by DeviceWallpaper

	override val application: Application
		get() = base

	override fun hasPermission(permission: String): Boolean {
		checkArgument(isPermissionString(permission)) {
			"Invalid Permission String: $permission"
		}
		return checkSelfPermission(permission)
	}

	override fun hasStoragePermission(): Boolean = storagePermission

	override fun getDeviceWallpaper(): Drawable? = wallpaperDrawable
	override fun getImageVector(@DrawableRes id: Int): ImageVector {
		return ImageVector.vectorResource(application.theme, application.resources, id)
	}

	private fun checkSelfPermission(permission: String): Boolean {
		checkArgument(isPermissionString(permission)) {
			"Invalid Permission String: $permission"
		}
		val status = ContextCompat.checkSelfPermission(application, permission)
		return isPermissionStatusGranted(status)
	}

	private fun isPermissionString(string: String): Boolean =
		string.startsWith(ANDROID_PERMISSION_PREFIX)

	private fun isPermissionStatusGranted(status: Int): Boolean =
		status == PackageManager.PERMISSION_GRANTED

  companion object {
		const val ANDROID_PERMISSION_PREFIX = "android.permission"

    private val initializer = LateLazy<AppDelegate>()
    private val delegate by initializer

    @JvmStatic
    val deviceWallpaperDrawable
       get() = delegate.getDeviceWallpaper()

    @JvmStatic
    val hasStoragePermission
       get() = delegate.hasStoragePermission()

    /**
     * Bypass [androidx.annotation.RequiresPermission as it check the function name
     * @return [Boolean] from [StoragePermission]
     */

    @JvmStatic fun checkStoragePermission() = hasStoragePermission

    /**
     * In case there's need to get ImageVector of a Drawable when context isn't available, e.g: Sealed Class
     * @param [id] the Int id of [DrawableRes]
     * @return [ImageVector] from [ImageVector.Companion.vectorResource]
     */

    @JvmStatic
		fun getImageVectorFromDrawableId(@DrawableRes id: Int) = delegate.getImageVector(id)

    /**
     * Provide the Instance
     * @see [com.kylentt.mediaplayer.app.dependency.AppInitializer.create]
     * @param [context] the base [Application] class to access resource from
     */

    @JvmStatic
    fun provides(context: Context): AppDelegate {
      if (BuildConfig.DEBUG) {
				LateLazySample.runTestCase()
      }
      checkState(!initializer.isInitialized)
      return initializer.initializeValue { AppDelegate(context) }
    }
  }
}

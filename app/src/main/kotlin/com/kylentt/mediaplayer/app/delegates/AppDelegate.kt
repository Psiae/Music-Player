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
import com.kylentt.mediaplayer.core.delegates.LateInitializerDelegate
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
 * Singleton to use the [Application] Context when there's no [Context] available,
 *
 * UI related Object returned by this class will use the Application class Theme
 *
 * @sample com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainBottomNavItems[2]
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
class AppDelegate private constructor(private val app: Application) : ApplicationDelegate {

	private constructor(context: Context) : this(context.applicationContext as Application)

	private val storagePermission by StoragePermission
	private val wallpaperDrawable by DeviceWallpaper

	override val application: Application
		get() = app

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
		val status = ContextCompat.checkSelfPermission(application, permission)
		return isPermissionStatusGranted(status)
	}

	private fun isPermissionString(string: String): Boolean =
		string.startsWith(ANDROID_PERMISSION_PREFIX)

	private fun isPermissionStatusGranted(status: Int): Boolean =
		status == PackageManager.PERMISSION_GRANTED

  companion object {
		const val ANDROID_PERMISSION_PREFIX = "android.permission."

    private val initializer: LateInitializerDelegate<AppDelegate> = LateLazy()
    private val delegate by initializer

    @JvmStatic
    val deviceWallpaperDrawable
       get() = delegate.getDeviceWallpaper()

    @JvmStatic
    val hasStoragePermission
       get() = delegate.hasStoragePermission()

    @JvmStatic fun checkStoragePermission() = hasStoragePermission

    @JvmStatic fun getImageVectorFromDrawableId(@DrawableRes id: Int) = delegate.getImageVector(id)

    @JvmStatic
		infix fun provides(context: Context): AppDelegate {
      checkState(!initializer.isInitialized)
      return initializer.initializeValue { AppDelegate(context) }
    }
  }
}

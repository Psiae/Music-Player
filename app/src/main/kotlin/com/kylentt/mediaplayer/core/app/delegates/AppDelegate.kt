package com.kylentt.mediaplayer.core.app.delegates

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.core.content.ContextCompat
import com.kylentt.mediaplayer.core.app.delegates.AppDelegate.Constants.ANDROID_PERMISSION_PREFIX
import com.kylentt.mediaplayer.core.app.delegates.device.DeviceWallpaper
import com.kylentt.mediaplayer.core.app.delegates.device.StoragePermissionHelper
import com.kylentt.mediaplayer.core.delegates.LateInitializerDelegate
import com.kylentt.mediaplayer.core.delegates.LateLazy
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import kotlin.reflect.KClass


interface ApplicationDelegate {
	fun hasPermission(permission: String): Boolean
	fun hasStoragePermission(): Boolean
	fun getDeviceWallpaper(): Drawable?
	fun getImageVector(@DrawableRes id: Int): ImageVector
	fun getLauncherIntent(): Intent
	fun getComponentName(kls: KClass<*>): ComponentName
}

class AppDelegate private constructor(app: Application) : ApplicationDelegate, ContextWrapper(app) {

	private constructor(context: Context) : this(context.applicationContext as Application)

	private val storagePermission by StoragePermissionHelper
	private val wallpaperDrawable by DeviceWallpaper

	private fun checkPermission(permission: String): Boolean {
		val status = ContextCompat.checkSelfPermission(this, permission)
		return isPermissionStatusGranted(status)
	}

	private fun isPermissionString(string: String): Boolean =
		string.startsWith(ANDROID_PERMISSION_PREFIX)

	private fun isPermissionStatusGranted(status: Int): Boolean =
		status == PackageManager.PERMISSION_GRANTED

	override fun hasPermission(permission: String): Boolean {
		checkArgument(isPermissionString(permission)) {
			"Invalid Permission String: $permission"
		}
		return checkPermission(permission)
	}

	override fun hasStoragePermission(): Boolean = storagePermission

	override fun getDeviceWallpaper(): Drawable? = wallpaperDrawable

	override fun getImageVector(@DrawableRes id: Int): ImageVector {
		return ImageVector.vectorResource(theme, resources, id)
	}

	override fun getComponentName(kls: KClass<*>): ComponentName {
		return ComponentName(this, kls.java)
	}

	override fun getLauncherIntent(): Intent {
		return packageManager.getLaunchIntentForPackage(this.packageName)!!
	}

	object Constants {
		const val ANDROID_PERMISSION_PREFIX = "android.permission."
	}

  companion object {

    private val initializer: LateInitializerDelegate<ApplicationDelegate> = LateLazy()
    private val delegate by initializer

    @JvmStatic
    val deviceWallpaperDrawable
       get() = delegate.getDeviceWallpaper()

    @JvmStatic
    val hasStoragePermission
       get() = delegate.hasStoragePermission()

    @JvmStatic fun checkStoragePermission() = hasStoragePermission

    @JvmStatic fun imageVectorFromDrawableId(@DrawableRes id: Int) = delegate.getImageVector(id)

		@JvmStatic fun componentName(kls: KClass<*>): ComponentName = delegate.getComponentName(kls)

		@JvmStatic fun launcherIntent(): Intent = delegate.getLauncherIntent()

    @JvmStatic
		infix fun provides(context: Context): ApplicationDelegate {
      checkState(!initializer.isInitialized)
      return initializer.initializeValue { AppDelegate(context) }
    }
  }
}

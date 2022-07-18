package com.kylentt.musicplayer.core.app.delegates

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
import com.kylentt.musicplayer.core.app.delegates.AppDelegate.Constants.ANDROID_PERMISSION_PREFIX
import com.kylentt.musicplayer.core.app.delegates.device.DeviceWallpaper
import com.kylentt.musicplayer.core.app.delegates.device.StoragePermissionHelper
import com.kylentt.musicplayer.common.late.LateLazy
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

	private fun checkPermission(permission: String): Boolean {
		val status = ContextCompat.checkSelfPermission(this, permission)
		return isPermissionStatusGranted(status)
	}

	private fun isPermissionString(string: String): Boolean {
		return string.startsWith(ANDROID_PERMISSION_PREFIX)
	}

	private fun isPermissionStatusGranted(status: Int): Boolean {
		return status == PackageManager.PERMISSION_GRANTED
	}

	object Constants {
		const val ANDROID_PERMISSION_PREFIX = "android.permission."
	}

  companion object {
    private val initializer = LateLazy<AppDelegate>()
    private val delegate by initializer

    val deviceWallpaperDrawable
       get() = delegate.getDeviceWallpaper()

    val storagePermissionStatus
       get() = delegate.hasStoragePermission()

		fun checkStoragePermission() = storagePermissionStatus
		fun imageVectorFromDrawableId(@DrawableRes id: Int) = delegate.getImageVector(id)
		fun componentName(kls: KClass<*>): ComponentName = delegate.getComponentName(kls)
		fun launcherIntent(): Intent = delegate.getLauncherIntent()

		infix fun provides(context: Context): ApplicationDelegate {
      checkState(!initializer.isInitialized)
      return initializer.initializeValue { AppDelegate(context) }
    }
  }
}

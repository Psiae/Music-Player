package com.flammky.android.content.context

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.flammky.android.manifest.permission.AndroidPermission

class ContextHelper(private val context: Context) {

	val configurations = object : Configurations {
		override val orientationInt: Int get() = context.orientationInt

		override fun isOrientationPortrait(): Boolean {
			return !isOrientationLandscape()
		}

		override fun isOrientationLandscape(): Boolean {
			return orientationInt == android.content.res.Configuration.ORIENTATION_LANDSCAPE
		}
	}

	val permissions = object : Permissions {

		override val common = object : Permissions.Common {

			override val hasReadExternalStorage: Boolean
				get() = hasPermission(AndroidPermission.Read_External_Storage)

			override val hasWriteExternalStorage: Boolean
				get() = hasPermission(AndroidPermission.Write_External_Stoage)
		}

		override fun hasPermission(permission: AndroidPermission) = hasPermission(permission.manifestPath)

		override fun hasPermission(path: String): Boolean {
			return context.checkPermission(path, android.os.Process.myPid(), android.os.Process.myUid()) ==
				PackageManager.PERMISSION_GRANTED
		}
	}

	val device = object : Device {
		override val screenWidthPx: Int
			get() = context.resources.displayMetrics.widthPixels

		override val screenHeightPx: Int
			get() = context.resources.displayMetrics.heightPixels

		override val screenWidthDp: Float
			get() = screenWidthPx / context.resources.displayMetrics.density

		override val screenHeightDp: Float
			get() = screenHeightPx / context.resources.displayMetrics.density

		override val memInfo: MemoryInfo
			get() {
				val memOut = MemoryInfo()
				(context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memOut)
				return memOut
			}
	}

	val intents = object : Intents {

		override val common: Intents.Common = object : Intents.Common {

			override fun appDetailSettings(): Intent {
				return Intent().apply {
					action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
					data = Uri.parse("package:${context.packageName}")
				}
			}
		}
	}

	interface Permissions {

		val common: Common

		fun hasPermission(permission: AndroidPermission): Boolean
		fun hasPermission(path: String): Boolean

		interface Common {
			val hasReadExternalStorage: Boolean
			val hasWriteExternalStorage: Boolean
		}
	}

	interface Configurations {
		val orientationInt: Int

		fun isOrientationPortrait(): Boolean
		fun isOrientationLandscape(): Boolean
	}

	interface Device {
		val screenWidthPx: Int
		val screenHeightPx: Int
		val screenWidthDp: Float
		val screenHeightDp: Float
		val memInfo: MemoryInfo
	}

	interface Intents {

		val common: Common

		interface Common {
			fun appDetailSettings(): Intent
		}
	}

	companion object {
		inline val Context.orientationInt: Int
			get() = resources.configuration.orientation
	}
}


private fun Context.findBase(): Context {
	return if (this is ContextWrapper) this.findBase() else this
}

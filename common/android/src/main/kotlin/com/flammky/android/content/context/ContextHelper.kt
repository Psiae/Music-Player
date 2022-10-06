package com.flammky.android.content.context

import android.content.Context
import android.content.pm.PackageManager
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

	companion object {
		inline val Context.orientationInt: Int
			get() = resources.configuration.orientation
	}
}

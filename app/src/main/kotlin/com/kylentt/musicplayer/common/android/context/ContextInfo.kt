package com.kylentt.musicplayer.common.android.context

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class ContextInfo(private val context: Context) {

	val readStorageAllowed: Boolean
		get() = PermissionHelper.checkReadStoragePermission(context)

	val writeStorageAllowed: Boolean
		get() = PermissionHelper.checkWriteStoragePermission(context)

	val settingIntent = Intent().apply {
		action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
		data = "package:${context.packageName}".toUri()
	}

	fun isPermissionGranted(permission: PermissionHelper.Permission): Boolean {
		return PermissionHelper.checkContextPermission(context, permission)
	}
}

@Composable
fun rememberContextInfo(): ContextInfo {
	val context = requireNotNull(LocalContext.current)
	return remember(context) { ContextInfo(context) }
}

object PermissionHelper {

	sealed class Permission {

		object READ_EXTERNAL_STORAGE : Permission() {
			override val androidManifestString: String
				get() = android.Manifest.permission.READ_EXTERNAL_STORAGE
		}

		object WRITE_EXTERNAL_STORAGE : Permission() {
			override val androidManifestString: String
				get() = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
		}

		abstract val androidManifestString: String
	}

	fun checkContextPermission(context: Context, permission: Permission): Boolean {
		return checkContextPermission(context, permission.androidManifestString)
	}

	fun checkReadStoragePermission(context: Context): Boolean {
		return checkContextPermission(context, Permission.READ_EXTERNAL_STORAGE)
	}

	fun checkWriteStoragePermission(context: Context): Boolean {
		return checkContextPermission(context, Permission.WRITE_EXTERNAL_STORAGE)
	}

	private fun checkContextPermission(context: Context, permission: String): Boolean {
		return ContextCompat
			.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
	}
}

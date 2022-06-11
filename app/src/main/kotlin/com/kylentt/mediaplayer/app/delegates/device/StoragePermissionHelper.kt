package com.kylentt.mediaplayer.app.delegates.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate to check Storage Permission Status
 * gets the Context from [AppDelegate] Class
 * @return [Boolean] true if both [checkReadStoragePermission] and [checkWriteStoragePermission] is true
 * @see [DeviceWallpaper]
 * @author Kylentt
 * @since 2022/04/30
 */

object StoragePermissionHelper : ReadOnlyProperty<Any?, Boolean> {

  const val Read_External_Storage = Manifest.permission.READ_EXTERNAL_STORAGE
  const val Write_External_Storage = Manifest.permission.WRITE_EXTERNAL_STORAGE

	override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
		return when (thisRef) {
			is Context -> checkReadWriteStoragePermission(thisRef)
			else -> AppDelegate.checkStoragePermission() // extend ContextWrapper
		}
	}

	@JvmStatic
	fun checkReadStoragePermission(context: Context): Boolean {
		return ContextCompat
			.checkSelfPermission(context, Read_External_Storage) == PackageManager.PERMISSION_GRANTED
	}

	@JvmStatic
	fun checkWriteStoragePermission(context: Context): Boolean {
		return ContextCompat
			.checkSelfPermission(context, Write_External_Storage) == PackageManager.PERMISSION_GRANTED
	}

	@JvmStatic
	fun checkReadWriteStoragePermission(context: Context): Boolean {
		return checkReadStoragePermission(context) and checkWriteStoragePermission(context)
	}
}

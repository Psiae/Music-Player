package com.flammky.android.medialib.temp.common.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

sealed class AndroidPermission {

	abstract val manifestPath: String

	object Write_External_Storage : AndroidPermission() {
		override val manifestPath: String = Manifest.permission.WRITE_EXTERNAL_STORAGE
	}

	object Read_External_Storage : AndroidPermission() {
		override val manifestPath: String = Manifest.permission.READ_EXTERNAL_STORAGE
	}
}

class AndroidPermissionInfo(private val context: Context) {

	val writeExternalStorageAllowed
		get() = isPermissionGranted(AndroidPermission.Write_External_Storage)

	val readExternalStorageAllowed
		get() = isPermissionGranted(AndroidPermission.Read_External_Storage)

	fun isPermissionGranted(permission: AndroidPermission): Boolean {
		return ContextCompat.checkSelfPermission(context, permission.manifestPath) == STATUS_GRANTED
	}

	companion object {
		private val STATUS_GRANTED
			get() = PackageManager.PERMISSION_GRANTED
	}
}

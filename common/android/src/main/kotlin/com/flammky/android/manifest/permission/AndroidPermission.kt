package com.flammky.android.manifest.permission

import android.Manifest

sealed class AndroidPermission(
	val manifestPath: String
) {

	object Read_External_Storage : AndroidPermission(
		Manifest.permission.READ_EXTERNAL_STORAGE
	)

	object Write_External_Stoage : AndroidPermission(
		Manifest.permission.WRITE_EXTERNAL_STORAGE
	)

	class Other(val path: String) : AndroidPermission(path)
}

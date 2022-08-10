package com.kylentt.musicplayer.core.app.permission

import android.app.Application
import com.kylentt.musicplayer.common.late.LateLazy

class PermissionManager private constructor(private val application: Application) {
	private val mPermissionInfo = AndroidPermissionInfo(application)

	val writeExternalStorageAllowed
		get() = mPermissionInfo.writeExternalStorageAllowed

	val readExternalStorageAllowed
		get() = mPermissionInfo.readExternalStorageAllowed

	companion object {
		private val instance = LateLazy<PermissionManager>()

		fun get(application: Application) = instance.construct { PermissionManager(application) }
	}
}

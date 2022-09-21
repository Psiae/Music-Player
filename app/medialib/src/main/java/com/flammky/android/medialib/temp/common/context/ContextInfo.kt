package com.flammky.android.medialib.temp.common.context

import android.content.Context
import com.flammky.android.medialib.temp.common.permission.AndroidPermission
import com.flammky.android.medialib.temp.common.permission.AndroidPermissionInfo
import com.flammky.android.medialib.temp.common.intent.AndroidCommonIntent


class ContextInfo  constructor (private val context: Context) {

	val commonIntent = AndroidCommonIntent(context)
	val permissionInfo = AndroidPermissionInfo(context)
	fun isPermissionGranted(permission: AndroidPermission): Boolean {
		return permissionInfo.isPermissionGranted(permission)
	}
}
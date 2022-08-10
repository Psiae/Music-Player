package com.kylentt.musicplayer.core.app

import android.app.Application
import android.content.Context
import com.kylentt.musicplayer.common.android.context.ContextInfo
import com.kylentt.musicplayer.common.late.LateLazy
import com.kylentt.musicplayer.core.app.cache.CacheManager
import com.kylentt.musicplayer.core.app.device.DeviceManager
import com.kylentt.musicplayer.core.app.permission.PermissionManager


interface ApplicationDelegate {
	val cacheManager: CacheManager
	val contextInfo: ContextInfo
	val deviceManager: DeviceManager
	val permissionManager: PermissionManager
}

class AppDelegate private constructor (context: Context) : ApplicationDelegate {
	private val mBase: Application = context.applicationContext as Application

	override val cacheManager = CacheManager.get(mBase)
	override val contextInfo = ContextInfo(mBase)
	override val deviceManager = DeviceManager.get(mBase)
	override val permissionManager = PermissionManager.get(mBase)

	companion object {
		private val delegate = LateLazy<AppDelegate>()

		val cacheManager
			get() = delegate.value.cacheManager

		val deviceManager
			get() = delegate.value.deviceManager

		val permissionManager
			get() = delegate.value.permissionManager

		infix fun provides(context: Context) = delegate.construct { AppDelegate(context) }
	}
}

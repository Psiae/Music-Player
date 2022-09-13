package com.flammky.android.medialib.temp.internal

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.core.content.getSystemService
import com.flammky.android.medialib.temp.common.context.ContextInfo
import com.flammky.android.medialib.temp.common.permission.AndroidPermissionInfo
import com.flammky.android.medialib.temp.common.intent.AndroidCommonIntent

internal class AndroidContext (context: Context) : ContextWrapper(context) {
	val application = applicationContext as Application

	val info: Info = RealInfo()
	val systemService: SystemService = RealSystemService()

	interface Info {
		val permissionInfo: AndroidPermissionInfo
		val commonIntent: AndroidCommonIntent
	}

	private inner class RealInfo : Info {
		private val delegate = ContextInfo(application)

		override val permissionInfo: AndroidPermissionInfo
			get() = delegate.permissionInfo

		override val commonIntent: AndroidCommonIntent
			get() = delegate.commonIntent
	}

	interface SystemService {
		val audioManager: AudioManager
		val notificationManager: NotificationManager
	}

	private inner class RealSystemService : SystemService {
		override val audioManager: AudioManager = application.getSystemService()!!
		override val notificationManager: NotificationManager = application.getSystemService()!!
	}
}

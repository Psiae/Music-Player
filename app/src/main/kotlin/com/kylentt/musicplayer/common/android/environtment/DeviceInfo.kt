package com.kylentt.musicplayer.common.android.environtment

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DimenRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceInfo @Inject constructor(@ApplicationContext context: Context) {
	private val localContext = context.applicationContext
	private val activityManager = localContext.getSystemService(ActivityManager::class.java)

	val memoryInfo: MemoryInfo
		get() {
			val memInfo = MemoryInfo()
			activityManager.getMemoryInfo(memInfo)
			return memInfo
		}

	val screenWidthPixel: Int
		get() = localContext.screenWidthPixel

	val screenHeightPixel: Int
		get() = localContext.screenHeightPixel

	val isOrientationPortrait
		get() = when (localContext.screenOrientation) {
			Configuration.ORIENTATION_LANDSCAPE -> false
			else -> true
		}

	fun dimensionPixelSize(@DimenRes id: Int): Int {
		return localContext.resources.getDimensionPixelSize(id)
	}

	val isOrientationLandscape
		get() = localContext.screenOrientation == Configuration.ORIENTATION_LANDSCAPE

	private inner class NotificationInfo

	companion object {


		val Context.screenWidthPixel: Int
			get() = resources.displayMetrics.widthPixels

		val Context.screenHeightPixel: Int
			get() = resources.displayMetrics.heightPixels

		val Context.screenHeightDp
			get() = screenHeightPixel.toFloat() * resources.displayMetrics.density

		val Context.screenWidthDp
			get() = screenWidthPixel.toFloat() * resources.displayMetrics.density

		val Context.screenOrientation
			get() = resources.configuration.orientation
	}
}

package com.flammky.android.environment

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DimenRes
import androidx.core.content.getSystemService

class DeviceInfo constructor(context: Context) {
	private val context = context.applicationContext

	private val activityManager: ActivityManager = this.context.getSystemService()!!

	val memoryInfo: MemoryInfo
		get() {
			val memInfo = MemoryInfo()
			activityManager.getMemoryInfo(memInfo)
			return memInfo
		}

	val screenWidthPixel: Int
		get() = context.screenWidthPixel

	val screenHeightPixel: Int
		get() = context.screenHeightPixel

	val isOrientationPortrait
		get() = !isOrientationLandscape

	val isOrientationLandscape
		get() = context.screenOrientation == Configuration.ORIENTATION_LANDSCAPE

	fun dimensionPixelSize(@DimenRes id: Int): Int {
		return context.resources.getDimensionPixelSize(id)
	}

	private inner class NotificationInfo

	companion object {

		val Context.screenWidthPixel: Int
			get() = resources.displayMetrics.widthPixels

		val Context.screenHeightPixel: Int
			get() = resources.displayMetrics.heightPixels

		val Context.screenHeightDp
			get() = screenHeightPixel / resources.displayMetrics.density

		val Context.screenWidthDp
			get() = screenWidthPixel / resources.displayMetrics.density

		val Context.screenOrientation
			get() = resources.configuration.orientation
	}
}

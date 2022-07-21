package com.kylentt.musicplayer.common.context

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceInfo @Inject constructor(@ApplicationContext context: Context) {
	private val localContext = context.applicationContext

	val screenWidthPixel: Int
		get() = localContext.screenWidthPixel

	val screenHeightPixel: Int
		get() = localContext.screenHeightPixel

	val isOrientationPortrait
		get() = when(localContext.screenOrientation) {
			Configuration.ORIENTATION_LANDSCAPE -> false
			else -> true
		}

	val isOrientationLandscape
		get() = localContext.screenOrientation == Configuration.ORIENTATION_LANDSCAPE

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

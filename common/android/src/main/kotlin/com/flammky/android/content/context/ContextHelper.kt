package com.flammky.android.content.context

import android.content.Context
import android.content.res.Configuration

class ContextHelper(private val context: Context) {


	fun isOrientationPortrait(): Boolean = !isOrientationLandscape()

	fun isOrientationLandscape(): Boolean {
		return context.orientationInt == Configuration.ORIENTATION_LANDSCAPE
	}

	companion object {
		inline val Context.orientationInt: Int
			get() = resources.configuration.orientation
	}
}

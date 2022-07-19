package com.kylentt.musicplayer.common.activity

import android.app.Activity
import androidx.core.view.WindowCompat

@Throws(IllegalArgumentException::class)
fun Activity.disableWindowFitSystemDecor() {
	requireNotNull(window) {
		"$this window was null" +
			"call this function when or after Activity.onCreate() is called"
	}
	WindowCompat.setDecorFitsSystemWindows(window, false)
}

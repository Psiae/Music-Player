package com.flammky.musicplayer.common.android.activity

import android.app.Activity
import android.view.Window
import com.flammky.musicplayer.common.android.activity.window.disableDecorFitSystem

/**
 * make the Content Views of the attached [Window] of the given [Activity]
 * to not consider fitting for System Insets such as Status bar and Navigation bar
 *
 * @throws IllegalArgumentException if the given [Activity] window is null
 */

@kotlin.jvm.Throws(IllegalArgumentException::class)
fun Activity.disableWindowDecorFitSystemInsets(): Activity {
	requireNotNull(window) {
		"Activity.window was null, " +
			"try calling this function when of after onCreate(Bundle?) is called"
	}
	window.disableDecorFitSystem()
	return this
}

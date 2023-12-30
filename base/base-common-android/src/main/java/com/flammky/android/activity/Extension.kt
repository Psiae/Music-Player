package com.flammky.android.activity

import android.app.Activity
import android.view.Window
import com.flammky.android.view.window.disableSystemWindowInsets

/**
 * make the Content Views of the attached [Window] of the given [Activity]
 * to not consider fitting for System Insets such as Status bar and Navigation bar
 *
 * @throws IllegalArgumentException if the given [Activity] window is null
 */

@kotlin.jvm.Throws(IllegalArgumentException::class)
fun Activity.disableSystemWindowInsets(): Activity = apply {
	requireNotNull(window) {
		"Activity.window was null, " +
			"try calling this function when of after onCreate(Bundle?) is called"
	}.apply {
		disableSystemWindowInsets()
	}
}

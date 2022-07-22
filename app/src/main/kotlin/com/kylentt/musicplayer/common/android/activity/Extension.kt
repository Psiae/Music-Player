package com.kylentt.musicplayer.common.android.activity

import android.app.Activity
import com.kylentt.musicplayer.common.android.activity.window.disableWindowFitSystemDecor

@kotlin.jvm.Throws(IllegalArgumentException::class)
fun Activity.disableWindowFitSystemDecor(): Unit {
	requireNotNull(window) {
		"Activity.window was null, " +
			"try calling this function when of after onCreate(Bundle?) is called"
	}
	window.disableWindowFitSystemDecor()
}

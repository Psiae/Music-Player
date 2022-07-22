package com.kylentt.musicplayer.common.android.activity.window

import android.view.Window
import androidx.core.view.WindowCompat

fun Window.disableWindowFitSystemDecor(): Unit {
	WindowCompat.setDecorFitsSystemWindows(this, false)
}

package com.kylentt.musicplayer.common.android.activity.window

import android.view.Window
import androidx.core.view.WindowCompat

/**
 * make the Content Views of the given [Window]
 * to not consider fitting for System Insets such as Status bar and Navigation bar
 */

fun Window.disableDecorFitSystem(): Window {
	WindowCompat.setDecorFitsSystemWindows(this, false)
	return this
}

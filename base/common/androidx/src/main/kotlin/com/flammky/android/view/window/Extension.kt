package com.flammky.android.view.window

import android.view.Window
import androidx.core.view.WindowCompat

/**
 * make the Content Views of the given [Window]
 * to not consider fitting for System Insets such as Status bar and Navigation bar
 */

fun Window.disableFitSystemInsets(): Window = apply {
	WindowCompat.setDecorFitsSystemWindows(this, false)
}

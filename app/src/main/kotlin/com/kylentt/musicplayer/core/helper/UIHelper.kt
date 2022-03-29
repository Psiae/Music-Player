package com.kylentt.musicplayer.core.helper

import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat

object UIHelper {

    fun ComponentActivity.disableFitWindow() = WindowCompat.setDecorFitsSystemWindows(window, false)
}
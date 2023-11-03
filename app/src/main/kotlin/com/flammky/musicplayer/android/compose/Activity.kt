package com.flammky.musicplayer.android.compose

import androidx.compose.runtime.staticCompositionLocalOf
import com.flammky.musicplayer.android.main.IntentManager

val LocalIntentManager = staticCompositionLocalOf<IntentManager> {
	error("IntentManager was not provided")
}

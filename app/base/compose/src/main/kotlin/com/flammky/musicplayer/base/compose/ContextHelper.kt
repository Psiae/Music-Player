package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.flammky.android.content.context.ContextHelper

@Composable
fun rememberLocalContextHelper(): ContextHelper {
	val current = LocalContext.current
	return remember(current) {
		ContextHelper(current)
	}
}

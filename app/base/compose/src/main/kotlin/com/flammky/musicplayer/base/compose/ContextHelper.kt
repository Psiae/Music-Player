package com.flammky.musicplayer.base.compose

import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.flammky.android.content.context.ContextHelper
import com.flammky.androidx.content.context.findBase

@Composable
fun rememberContextHelper(): ContextHelper {
	val current = LocalContext.current
	return remember(current) {
		val context = if (current is ContextWrapper) current.findBase() else current
		ContextHelper(context)
	}
}

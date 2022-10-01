package com.flammky.musicplayer.library.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.flammky.musicplayer.library.ui.root.LibraryRoot

/** Library Composable internal entry point */
@Composable
internal fun LibraryEntry() {
	val allow = remember { mutableStateOf(true) }
	if (allow.value) {
		LibraryRoot()
	} else {
		TODO()
	}
}



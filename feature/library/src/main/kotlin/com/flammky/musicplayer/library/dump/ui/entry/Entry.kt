package com.flammky.musicplayer.library.dump.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.flammky.musicplayer.library.dump.ui.root.LibraryRoot

/** Library Composable internal entry point */
@Composable
internal fun LibraryEntry() {
	if (libraryGuard().value) {
		LibraryRoot()
	}
}

// If we want to restrict something
@Composable
private fun libraryGuard(): State<Boolean> {
	val allow = remember { mutableStateOf(true) }
	// TODO()
	return allow
}



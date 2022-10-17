package com.flammky.musicplayer.library.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.flammky.musicplayer.library.ui.root.LibraryRoot
import com.flammky.musicplayer.library.util.read

/** Library Composable internal entry point */
@Composable
internal fun LibraryEntry() {
	if (libraryGuard().read()) {
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



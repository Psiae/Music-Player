package com.flammky.musicplayer.library.presentation

import androidx.compose.runtime.Composable
import com.flammky.musicplayer.library.dump.ui.root.LibraryRoot
import com.flammky.musicplayer.library.presentation.entry.EntryGuard

@Composable
internal fun Library() {
	EntryGuard {
		LibraryRoot()
	}
}

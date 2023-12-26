package com.flammky.musicplayer.library.presentation

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.flammky.musicplayer.library.dump.ui.root.LibraryRoot
import dev.dexsr.klio.library.ui.root.LibraryRootNavigator

@Composable
internal fun Library() {
	/*LibraryRoot()*/
	val navigator = remember { LibraryRootNavigator() }
	dev.dexsr.klio.library.ui.root.LibraryRoot(navigator = navigator)
}

package dev.dexsr.klio.library.ui.root

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dexsr.klio.library.ui.nav.LibraryUiNavigator

@Stable
class LibraryRootNavigator : LibraryUiNavigator("root") {

	val startDestination
		get() = "main"

	var currentDestination by mutableStateOf<String?>(startDestination)
}

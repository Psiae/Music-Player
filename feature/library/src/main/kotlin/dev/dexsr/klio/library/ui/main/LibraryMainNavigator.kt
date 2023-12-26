package dev.dexsr.klio.library.ui.main

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.library.ui.nav.LibraryUiNavigator

@Stable
class LibraryMainNavigator : LibraryUiNavigator("main") {

	val startDestination: String
		get() = "device"

	var currentDestination: String by mutableStateOf(startDestination)
		private set

	var currentSubDestination: String? by mutableStateOf<String?>(null)
		private set

	fun navigate(destination: String) {
		currentDestination = destination
	}

	fun navigateSub(destination: String) {
		currentSubDestination = destination
	}

	fun pop(): Boolean {
		currentSubDestination?.let {
			currentSubDestination = null
			return true
		}
		return false
	}

	@SnapshotRead
	fun canPop(): Boolean {
		return currentSubDestination != null
	}

	companion object {

		fun AndroidSaver(): Saver<LibraryMainNavigator, android.os.Bundle> {
			return Saver(
				save = { o -> Bundle().apply {
					putString("currentDestination", o.currentDestination)
					putString("currentSubDestination", o.currentSubDestination)
				} },
				restore = { s -> LibraryMainNavigator().apply {
					currentDestination = s.getString("currentDestination") ?: currentDestination
					currentSubDestination = s.getString("currentSubDestination")
				} }
			)
		}
	}
}

@Composable
fun rememberMainNavigator(): LibraryMainNavigator {

	return rememberSaveable(
		saver = LibraryMainNavigator.AndroidSaver()
	) {
		LibraryMainNavigator()
	}
}

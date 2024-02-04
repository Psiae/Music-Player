package dev.dexsr.klio.library.ui.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dexsr.klio.base.composeui.SimpleStack
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Background
import dev.dexsr.klio.library.ui.main.LibraryUiMain

@Composable
fun LibraryRoot(
	navigator: LibraryRootNavigator
) {
	LibraryRootNavHost(navigator = navigator)
}

@Composable
private fun LibraryRootNavHost(
	navigator: LibraryRootNavigator
) {
	SimpleStack(
		modifier = Modifier
			.fillMaxSize()
			.localMaterial3Background()
	) {
		val content: (@Composable () -> Unit)? = when (navigator.currentDestination) {
			"main" -> {
				@Composable {
					LibraryUiMain(rootNavigator = navigator)
				}
			}
			 else -> null
		}
		content?.invoke()
	}
}

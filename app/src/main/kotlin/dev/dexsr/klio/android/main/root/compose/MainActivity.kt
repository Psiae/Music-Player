package dev.dexsr.klio.android.main.root.compose

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.flammky.musicplayer.android.compose.LocalIntentManager
import com.flammky.musicplayer.base.compose.LocalFitSystemWindows
import dev.dexsr.klio.android.main.MainActivity
import dev.dexsr.klio.android.main.root.compose.md3.MD3RootScreen
import dev.dexsr.klio.base.composeui.ComposableLambda

fun MainActivity.setComposeRootContent(
	fitSystemWindow: Boolean
) {
	// TODO
	setContent {
		ComposeRootScreen(fitSystemWindow)
	}
}

@Composable
private fun MainActivity.ComposeRootScreen(
	fitSystemWindow: Boolean
) {
	ProvideComposeRootCompositionLocals(fitSystemWindow) {
		MD3RootScreen()
	}
}

@Composable
private fun MainActivity.ProvideComposeRootCompositionLocals(
	fitSystemWindow: Boolean,
	content: ComposableLambda
) {
	CompositionLocalProvider(
		LocalIntentManager provides intentManager,
		LocalFitSystemWindows provides fitSystemWindow,
		content = content
	)
}

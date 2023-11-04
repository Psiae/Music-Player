package dev.dexsr.klio.android.main.compose

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.flammky.musicplayer.android.compose.LocalIntentManager
import dev.dexsr.klio.android.main.MainActivity
import dev.dexsr.klio.android.main.compose.md3.MD3RootScreen
import dev.dexsr.klio.base.compose.ComposableFun

fun MainActivity.setComposeRootContent() {
	// TODO
	setContent {
		ComposeRootScreen()
	}
}

@Composable
private fun MainActivity.ComposeRootScreen() {
	ProvideComposeRootCompositionLocals {
		MD3RootScreen()
	}
}

@Composable
private fun MainActivity.ProvideComposeRootCompositionLocals(
	content: ComposableFun
) {
	CompositionLocalProvider(LocalIntentManager provides intentManager, content = content)
}

package dev.dexsr.klio.android.main.compose.md3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasSnowCone
import dev.dexsr.klio.base.compose.ComposableFun
import dev.dexsr.klio.base.compose.SimpleLayoutMeasurePolicy
import dev.dexsr.klio.base.theme.md3.compose.DefaultMaterial3Theme
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Background

@Composable
internal fun MD3RootScreen() {
	DefaultMaterial3Theme(
		dynamic = AndroidAPI.hasSnowCone(),
		dark = isSystemInDarkTheme()
	) {
		MD3RootSurface {
			MD3RootContent(modifier = Modifier)
		}
	}
}

@Composable
private inline fun MD3RootSurface(
	modifier: Modifier = Modifier,
	content: ComposableFun
) {
	Layout(
		modifier = Modifier
			.fillMaxSize()
			.localMaterial3Background(),
		content = content,
		measurePolicy = SimpleLayoutMeasurePolicy
	)
}

package dev.dexsr.klio.android.main.root.compose.md3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import dev.dexsr.klio.core.sdk.AndroidAPI
import dev.dexsr.klio.core.sdk.AndroidBuildVersion.hasSnowCone
import dev.dexsr.klio.base.composeui.ComposableLambda
import dev.dexsr.klio.base.composeui.SimpleStackLayoutMeasurePolicy
import dev.dexsr.klio.base.theme.md3.compose.DefaultMaterial3Theme
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Background

@Composable
internal fun MD3RootScreen() {
	DefaultMaterial3Theme(
		dynamic = AndroidAPI.hasSnowCone(),
		dark = isSystemInDarkTheme()
	) {
		MD3RootSurface {
			MD3RootContent(
				modifier = Modifier
			)
		}
	}
}

@Composable
private inline fun MD3RootSurface(
	modifier: Modifier = Modifier,
	content: ComposableLambda
) {
	Layout(
		modifier = modifier
			.fillMaxSize()
			.localMaterial3Background(),
		content = content,
		measurePolicy = SimpleStackLayoutMeasurePolicy
	)
}

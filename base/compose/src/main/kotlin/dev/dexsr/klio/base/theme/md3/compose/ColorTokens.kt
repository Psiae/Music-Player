package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.dexsr.klio.base.theme.md3.MD3Theme

@Composable
// TODO: complete
fun MD3Theme.colorFromToken(tokenStr: String): Color {
	return when (tokenStr) {
		"SurfaceVariant" -> MD3Theme.surfaceVariantColorAsState().value
		"OnSurfaceVariant" -> MD3Theme.surfaceVariantContentColorAsState().value
		else -> Color.Unspecified
	}
}

@Composable
// TODO: complete
fun MD3Theme.colorFromContentToken(tokenStr: String): Color {
	return when (tokenStr) {
		"OnBackground" -> MD3Theme.backgroundContentColorAsState().value
		"OnSurface" -> MD3Theme.surfaceContentColorAsState().value
		"OnSurfaceVariant" -> MD3Theme.surfaceVariantContentColorAsState().value
		"OnSecondaryContainer" -> MD3Theme.secondaryContainerContentColorAsState().value
		"OnPrimaryContainer" -> MD3Theme.primaryContainerContentColorAsState().value
		else -> Color.Unspecified
	}
}

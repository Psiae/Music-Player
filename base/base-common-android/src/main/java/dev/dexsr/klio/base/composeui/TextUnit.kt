package dev.dexsr.klio.base.composeui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified

@Composable
fun TextStyle.nonFontScaled(): TextStyle {
	if (fontSize.isUnspecified) return this
	return copy(fontSize = fontSize.div(LocalDensity.current.fontScale))
}

@Composable
fun TextStyle.nonScaledFontSize(): TextUnit {
	if (fontSize.isUnspecified) fontSize
	return fontSize / LocalDensity.current.fontScale
}

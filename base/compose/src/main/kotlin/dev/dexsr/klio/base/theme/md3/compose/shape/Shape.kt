package dev.dexsr.klio.base.theme.md3.compose.shape

import androidx.compose.foundation.shape.RoundedCornerShape as ComposeRoundedCornerShape
import dev.dexsr.klio.base.theme.shape.RoundedCornerShape as ThemeRoundedCornerShape

fun ThemeRoundedCornerShape.toComposeShape(): ComposeRoundedCornerShape {
	return ComposeRoundedCornerShape(
		topStart = topLeft,
		topEnd = topRight,
		bottomStart = bottomLeft,
		bottomEnd = bottomRight
	)
}

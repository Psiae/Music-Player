package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.theme.md3.Padding as ThemePadding

fun ThemePadding.toComposePadding(): PaddingValues {
	return PaddingValues(start = left.dp, top = top.dp, end = right.dp, bottom = bottom.dp)
}

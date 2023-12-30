package dev.dexsr.klio.base.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Float.checkedDpStatic(): Dp {
	if (this == Dp.Hairline.value) return Dp.Hairline
	if (this == Dp.Infinity.value) return Dp.Infinity
	if (this == Dp.Unspecified.value) return Dp.Unspecified
	return dp
}

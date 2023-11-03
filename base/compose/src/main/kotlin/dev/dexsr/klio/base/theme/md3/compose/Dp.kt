package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.PADDING_INCREMENT_DP

fun MD3Theme.dpPaddingIncrementsOf(
	n: Int
): Dp = (PADDING_INCREMENT_DP * n).dp

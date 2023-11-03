package dev.dexsr.klio.theme.md3.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.theme.md3.MD3Theme

val PADDING_INCREMENT_DP = 4

fun MD3Theme.dpPaddingIncrementsOf(
	n: Int
): Dp = (PADDING_INCREMENT_DP * n).dp

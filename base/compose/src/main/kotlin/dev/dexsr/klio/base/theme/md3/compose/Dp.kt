package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.PADDING_INCREMENT_DP

// maybe: spec ?
fun MD3Theme.dpPaddingIncrementsOf(
	n: Int
): Dp = (MD3Spec.PADDING_INCREMENT_DP * n).dp

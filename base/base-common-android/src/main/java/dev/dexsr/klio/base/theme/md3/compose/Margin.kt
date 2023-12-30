package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.theme.md3.MARGIN_INCREMENTS_VALUE_COMPACT
import dev.dexsr.klio.base.theme.md3.MARGIN_INCREMENTS_VALUE_EXPANDED
import dev.dexsr.klio.base.theme.md3.MARGIN_INCREMENTS_VALUE_MEDIUM
import dev.dexsr.klio.base.theme.md3.MD3Theme


fun MD3Theme.marginIncrements(
    other: Dp,
    n: Int,
    widthConstraints: Dp
): Dp = other + dpMarginIncrementsOf(n, widthConstraints)

fun MD3Theme.dpMarginIncrementsOf(
    n: Int,
    widthConstraints: Dp
): Dp = when {
    widthConstraints.value <= 0 -> 0.dp
    widthConstraints.value <= WindowSize.COMPACT.maxWidthDp -> MARGIN_INCREMENTS_VALUE_COMPACT.dp
    widthConstraints.value <= WindowSize.MEDIUM.maxWidthDp -> MARGIN_INCREMENTS_VALUE_MEDIUM.dp
    else -> MARGIN_INCREMENTS_VALUE_EXPANDED.dp
} * n

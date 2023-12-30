package dev.dexsr.klio.base.theme.md3.compose

internal sealed class WindowSize {

	object COMPACT

	object MEDIUM

	object EXPANDED
}

private const val WINDOWSIZE_COMPACT_MAXWIDTH_DP = 600

private const val WINDOWSIZE_MEDIUM_MAXWIDTH_DP = 840

private const val WINDOWSIZE_EXPANDED_MAXWIDTH_DP = Int.MAX_VALUE

internal val WindowSize.COMPACT.maxWidthDp
	get() = WINDOWSIZE_COMPACT_MAXWIDTH_DP

internal val WindowSize.MEDIUM.maxWidthDp
	get() = WINDOWSIZE_EXPANDED_MAXWIDTH_DP

internal val WindowSize.EXPANDED.maxWidthDp
	get() = WINDOWSIZE_EXPANDED_MAXWIDTH_DP

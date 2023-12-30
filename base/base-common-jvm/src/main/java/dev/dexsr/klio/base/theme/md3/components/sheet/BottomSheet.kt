package dev.dexsr.klio.base.theme.md3.components.sheet

import dev.dexsr.klio.base.theme.md3.MD3Spec

object BottomSheet {

	const val DragHandleAlpha = 0.4f
	const val DragHandleWidthDp = 32f
	const val DragHandleHeightDp = 4f
	const val DragHandleVerticalPadding = 22
	const val DragHandleAlignmentHorizontalBias = 0f
	const val MaxWidthDp = 640
	const val DragHandleColorToken = "OnSurfaceVariant"
	const val PeekHeightDp = 56f

	// TODO shape

	fun topMarginDp(windowWidthDp: Float): Float {
		if (windowWidthDp > 640f) {
			return 56f
		} else if (windowWidthDp > 0f) {
			return 72f
		}
		return 0f
	}

	fun horizontalMarginDp(windowWidthDp: Float): Float {
		if (windowWidthDp > 640f) {
			return 56f
		}
		return 0f
	}
}

val MD3Spec.bottomSheet
	get() = BottomSheet

val MD3Spec.BottomSheetDragHandleAlpha
	get() = bottomSheet.DragHandleAlpha

val MD3Spec.BottomSheetDragHandleWidthDp
	get() = bottomSheet.DragHandleWidthDp

val MD3Spec.BottomSheetDragHandleHeightDp
	get() = bottomSheet.DragHandleHeightDp

val MD3Spec.BottomSheetDragHandleColorToken
	get() = bottomSheet.DragHandleColorToken

val MD3Spec.BottomSheetPeekHeightDp
	get() = bottomSheet.PeekHeightDp

val MD3Spec.BottomSheetMaxWidthDp
	get() = bottomSheet.MaxWidthDp

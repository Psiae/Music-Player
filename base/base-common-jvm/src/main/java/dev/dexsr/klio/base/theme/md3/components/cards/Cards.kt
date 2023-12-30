package dev.dexsr.klio.base.theme.md3.components.cards

import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.Padding
import dev.dexsr.klio.base.theme.shape.RoundedCornerShape

object Cards {
	val shapeDp = RoundedCornerShape(12f, 12f, 12f, 12f)
	val paddingDp = Padding(left = 16f, right = 16f)

	const val DISABLED_ALPHA = 0.38f
	const val ENABLED_ALPHA = 1f
	const val DISABLED_CONTENT_ALPHA = 0.68f
	const val DISABLED_CONTENT_TEXT_ALPHA = 0.38f
	const val ICON_SIZE_DP = 24f
	const val SPACING_DP = 8F
}

val MD3Spec.cards: Cards
	get() = Cards

val MD3Spec.cardsShapeDp: RoundedCornerShape
	get() = Cards.shapeDp

fun Cards.alpha(enabled: Boolean) = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
fun Cards.contentAlpha(enabled: Boolean, isText: Boolean): Float {
	if (enabled) return 1f
	if (isText) DISABLED_CONTENT_TEXT_ALPHA
	return DISABLED_CONTENT_ALPHA
}

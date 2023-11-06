package com.flammky.musicplayer.base.theme

/**
 * Abstract Theme
 */
abstract class Theme protected constructor() {

	// Companion for extension
	@Deprecated(
		"use MD3Theme instead",
		ReplaceWith("MD3Theme", "dev.dexsr.klio.base.theme.md3.MD3Theme")
	)
	companion object
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

/**
 * Thrown if when trying to read box id the length doesn't make any sense
 */
class InvalidBoxHeaderException(message: String?) : RuntimeException(message) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -8797541836152099722L
	}
}

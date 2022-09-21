package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

/**
 * Thrown if when trying to read box id just finds nulls
 * Normally an error, but if occurs at end of file we allow it
 */
class NullBoxIdException(message: String?) : RuntimeException(message) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 7160724085873888124L
	}
}

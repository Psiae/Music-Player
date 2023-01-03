package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

import java.io.IOException

/**
 * Should be thrown when unable to create a file when it is expected it should be creatable. For example because
 * you dont have permission to write to the folder that it is in.
 */
class UnableToCreateFileException(message: String?) : IOException(message) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 3390375837765957908L
	}
}

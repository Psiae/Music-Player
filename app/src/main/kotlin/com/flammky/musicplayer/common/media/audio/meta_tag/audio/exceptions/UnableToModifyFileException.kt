package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

import java.io.IOException

/**
 * Should be thrown when unable to modify a file when it is expected it should be modifiable. For example because
 * you dont have permission to modify files in the folder that it is in.
 */
class UnableToModifyFileException(message: String?) : IOException(message) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 5015053427539691565L
	}
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

import java.io.IOException

/**
 * Should be thrown when unable to rename a file when it is expected it should rename. For example could occur on Vista
 * because you do not have Special Permission 'Delete' set to Denied.
 */
class UnableToRenameFileException(message: String?) : IOException(message) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -3942088615944301367L
	}
}

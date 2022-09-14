package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

/**
 * This exception is used when a string was about to be interpreted as a GUID,
 * but did not match the format.<br></br>
 *
 * @author Christian Laireiter
 */
class GUIDFormatException
/**
 * Creates an instance.
 *
 * @param detail detail message.
 */
	(detail: String?) : IllegalArgumentException(detail) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 6035645678612384953L
	}
}

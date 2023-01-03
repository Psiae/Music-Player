package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

/**
 * This exception should be thrown idf it appears the file is a video file, jaudiotagger only supports audio
 * files.
 */
class CannotReadVideoException : CannotReadException {
	/**
	 * Creates an instance.
	 */
	constructor() : super()
	constructor(ex: Throwable?) : super(ex)

	/**
	 * Creates an instance.
	 *
	 * @param message The message.
	 */
	constructor(message: String?) : super(message)

	/**
	 * Creates an instance.
	 *
	 * @param message The error message.
	 * @param cause   The throwable causing this exception.
	 */
	constructor(message: String?, cause: Throwable?) : super(message, cause)

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -7185020848474992115L
	}
}

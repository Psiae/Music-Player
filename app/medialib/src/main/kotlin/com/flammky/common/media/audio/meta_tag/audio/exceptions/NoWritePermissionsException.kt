package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

/**
 * Use this exception instead of the more general CannotWriteException if unable to write file because of a permissions
 * problem
 */
class NoWritePermissionsException : CannotWriteException {
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
		private const val serialVersionUID = -156467854598317547L
	}
}

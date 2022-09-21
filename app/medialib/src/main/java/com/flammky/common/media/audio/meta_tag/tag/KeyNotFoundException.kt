package com.flammky.musicplayer.common.media.audio.meta_tag.tag

/**
 * Thrown if the key cannot be found
 *
 *
 * Should not happen with well written code, hence RuntimeException.
 */
class KeyNotFoundException : RuntimeException {
	/**
	 * Creates a new KeyNotFoundException datatype.
	 */
	constructor()

	/**
	 * Creates a new KeyNotFoundException datatype.
	 *
	 * @param ex the cause.
	 */
	constructor(ex: Throwable?) : super(ex)

	/**
	 * Creates a new KeyNotFoundException datatype.
	 *
	 * @param msg the detail message.
	 */
	constructor(msg: String?) : super(msg)

	/**
	 * Creates a new KeyNotFoundException datatype.
	 *
	 * @param msg the detail message.
	 * @param ex  the cause.
	 */
	constructor(msg: String?, ex: Throwable?) : super(msg, ex)

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -4532369719091873024L
	}
}

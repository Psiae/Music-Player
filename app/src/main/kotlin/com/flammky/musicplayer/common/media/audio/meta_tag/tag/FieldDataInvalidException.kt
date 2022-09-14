package com.flammky.musicplayer.common.media.audio.meta_tag.tag

/**
 * Thrown if the try and create a field with invalid data
 *
 *
 * For example if try and create an Mp4Field with type Byte using data that cannot be parsed as a number
 * then this exception will be thrown
 */
class FieldDataInvalidException : TagException {
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
		private const val serialVersionUID = 3073420523534394699L
	}
}

package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom

/**
 * List of valid values for the Rating (rtng) atom
 *
 * These are held as a byte field
 *
 * TODO:Is this only used in video
 */
enum class Mp4RatingValue
/**
 * @param description of value
 * @param id          used internally
 */(
	/**
	 * This is the value of the fieldname that is actually used to write mp4
	 *
	 * @return
	 */
	val description: String,
	/**
	 * Return id used in the file
	 *
	 * @return id
	 */
	val id: Int
) {
	CLEAN("Clean", 2), EXPLICIT("Explicit", 4);

}

package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom

/**
 * List of valid values for the Content Type (Stik) atom
 *
 *
 * These are held as a byte field, normally only used for purcahed items, audio files use a stik of one
 */
enum class Mp4ContentTypeValue
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
	MOVIE("Movie", 0), NORMAL("Normal", 1), AUDIO_BOOK("AudioBook", 2), BOOKMARK(
		"Whacked Bookmark",
		5
	),
	MUSIC_VIDEO("Music Video", 6), SHORT_FILM("Short Film", 9), TV_SHOW(
		"TV Show",
		10
	),
	BOOKLET("Booklet", 11);

	/**
	 * @return the id as a string (convenience method for use with mp4.createtagField()
	 */
	val idAsString: String
		get() = id.toString()
}

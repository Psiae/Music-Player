package com.flammky.musicplayer.common.media.audio.meta_tag.audio

/**
 * Files formats currently supported by Library.
 * Each enum value is associated with a file suffix (extension).
 */
enum class SupportedFileFormat
/** Constructor for internal use by this enum.
 */(
	/**
	 * File Suffix
	 */
	val filesuffix: String,
	/**
	 * User Friendly Name
	 */
	val displayName: String
) {
	OGG("ogg", "Ogg"), OPUS("opus", "Opus"), OGA("oga", "Oga"), MP3("mp3", "Mp3"), FLAC("flac", "Flac"), MP4(
		"mp4",
		"Mp4"
	),
	M4A("m4a", "Mp4"), M4P("m4p", "M4p"), WMA("wma", "Wma"), WAV("wav", "Wav"), RA(
		"ra",
		"Ra"
	),
	RM("rm", "Rm"), M4B("m4b", "Mp4"), AIF("aif", "Aif"), AIFF("aiff", "Aif"), AIFC(
		"aifc",
		"Aif Compressed"
	),
	DSF("dsf", "Dsf"), DFF("dff", "Dff");
	/**
	 * Returns the file suffix (lower case without initial .) associated with the format.
	 */

}

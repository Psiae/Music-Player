package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

/**
 * AIFF types, refers to BigEndian or LittleEndian
 */
enum class AiffType(  //Originally Compressed AIFF but also used for Uncompressed in LE rather than BE order
	var code: String
) {
	AIFF("AIFF"),  //Original non-compressed format on Mac pre-intel hardware
	AIFC("AIFC");

}

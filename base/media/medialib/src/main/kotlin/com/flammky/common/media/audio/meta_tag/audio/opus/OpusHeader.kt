package com.flammky.common.media.audio.meta_tag.audio.opus


import java.nio.charset.Charset


/**
 * Defines variables common to all vorbis headers
 */
interface OpusHeader {
	companion object {
		const val HEAD_CAPTURE_PATTERN = "OpusHead"
		val HEAD_CAPTURE_PATTERN_AS_BYTES =
			HEAD_CAPTURE_PATTERN.toByteArray(Charset.forName("ISO_8859_1"))
		const val HEAD_CAPTURE_PATTERN_POS = 0
		val HEAD_CAPTURE_PATTERN_LENGTH = HEAD_CAPTURE_PATTERN_AS_BYTES.size

		//Capture pattern at start of header
		const val TAGS_CAPTURE_PATTERN = "OpusTags"
		val TAGS_CAPTURE_PATTERN_AS_BYTES =
			TAGS_CAPTURE_PATTERN.toByteArray(Charset.forName("ISO_8859_1"))
		const val TAGS_CAPTURE_PATTERN_POS = 0
		val TAGS_CAPTURE_PATTERN_LENGTH = TAGS_CAPTURE_PATTERN_AS_BYTES.size
	}
}

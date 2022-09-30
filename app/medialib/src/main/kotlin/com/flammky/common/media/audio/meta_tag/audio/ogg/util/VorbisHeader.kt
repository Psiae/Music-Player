package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util

/**
 * Defines variables common to all vorbis headers
 */
interface VorbisHeader {
	companion object {
		//Capture pattern at start of header
		const val CAPTURE_PATTERN = "vorbis"

		val CAPTURE_PATTERN_AS_BYTES = byteArrayOf(
			'v'.code.toByte(),
			'o'.code.toByte(),
			'r'.code.toByte(),
			'b'.code.toByte(),
			'i'.code.toByte(),
			's'.code.toByte()
		)
		const val FIELD_PACKET_TYPE_POS = 0
		const val FIELD_CAPTURE_PATTERN_POS = 1
		const val FIELD_PACKET_TYPE_LENGTH = 1
		const val FIELD_CAPTURE_PATTERN_LENGTH = 6

		//Vorbis uses UTF-8 for all text
		const val CHARSET_UTF_8 = "UTF-8"
	}
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util

import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * Vorbis Setup header
 *
 * We dont need to decode a vorbis setup header for metatagging, but we should be able to identify
 * it.
 *
 * @author Paul Taylor
 * @version 12th August 2007
 */
class VorbisSetupHeader(vorbisData: ByteArray) : VorbisHeader {
	var isValid = false
		private set

	init {
		decodeHeader(vorbisData)
	}

	fun decodeHeader(b: ByteArray) {
		val packetType = b[VorbisHeader.Companion.FIELD_PACKET_TYPE_POS].toInt()
		logger.fine(
			"packetType$packetType"
		)
		val vorbis = String(
			b,
			VorbisHeader.Companion.FIELD_CAPTURE_PATTERN_POS,
			VorbisHeader.Companion.FIELD_CAPTURE_PATTERN_LENGTH,
			StandardCharsets.ISO_8859_1
		)
		if (packetType == VorbisPacketType.SETUP_HEADER.type && vorbis == VorbisHeader.Companion.CAPTURE_PATTERN) {
			isValid = true
		}
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom")
	}
}

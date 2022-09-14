/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.VorbisVersion
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * Vorbis Identification header
 *
 * From http://xiph.org/vorbis/doc/Vorbis_I_spec.html#id326710
 *
 * The identification header is a short header of only a few fields used to declare the stream definitively as Vorbis,
 * and provide a few externally relevant pieces of information about the audio stream. The identification header is
 * coded as follows:
 *
 * 1) [vorbis_version] = read 32 bits as unsigned integer
 * 2) [audio_channels] = read 8 bit integer as unsigned
 * 3) [audio_sample_rate] = read 32 bits as unsigned integer
 * 4) [bitrate_maximum] = read 32 bits as signed integer
 * 5) [bitrate_nominal] = read 32 bits as signed integer
 * 6) [bitrate_minimum] = read 32 bits as signed integer
 * 7) [blocksize_0] = 2 exponent (read 4 bits as unsigned integer)
 * 8) [blocksize_1] = 2 exponent (read 4 bits as unsigned integer)
 * 9) [framing_flag] = read one bit
 *
 * $Id$
 *
 * @author Raphael Slinckx (KiKiDonK)
 * @version 16 d�cembre 2003
 */
class VorbisIdentificationHeader(vorbisData: ByteArray) : VorbisHeader {
	var channelNumber = 0
		private set
	var isValid = false
		private set
	private var vorbisVersion = 0
	var samplingRate = 0
		private set
	var minBitrate = 0
		private set
	var nominalBitrate = 0
		private set
	var maxBitrate = 0
		private set

	init {
		decodeHeader(vorbisData)
	}

	val encodingType: String
		get() = VorbisVersion.values()[vorbisVersion].toString()

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
		if (packetType == VorbisPacketType.IDENTIFICATION_HEADER.type && vorbis == VorbisHeader.Companion.CAPTURE_PATTERN) {
			vorbisVersion =
				b[7] + (b[8].toInt() shl 8) + (b[9].toInt() shl 16) + (b[10].toInt() shl 24)
			logger.fine(
				"vorbisVersion$vorbisVersion"
			)
			channelNumber = u(b[FIELD_AUDIO_CHANNELS_POS].toInt())
			logger.fine("audioChannels" + channelNumber)
			samplingRate =
				u(b[12].toInt()) + (u(b[13].toInt()) shl 8) + (u(b[14].toInt()) shl 16) + (u(
					b[15].toInt()
				) shl 24)
			logger.fine("audioSampleRate" + samplingRate)
			logger.fine("audioSampleRate" + b[12] + " " + b[13] + " " + b[14])

			//TODO is this right spec says signed
			minBitrate =
				u(b[16].toInt()) + (u(b[17].toInt()) shl 8) + (u(b[18].toInt()) shl 16) + (u(
					b[19].toInt()
				) shl 24)
			nominalBitrate =
				u(b[20].toInt()) + (u(b[21].toInt()) shl 8) + (u(b[22].toInt()) shl 16) + (u(
					b[23].toInt()
				) shl 24)
			maxBitrate =
				u(b[24].toInt()) + (u(b[25].toInt()) shl 8) + (u(b[26].toInt()) shl 16) + (u(
					b[27].toInt()
				) shl 24)
			//byte blockSize0 = (byte) ( b[28] & 240 );
			//byte blockSize1 = (byte) ( b[28] & 15 );
			val framingFlag = b[FIELD_FRAMING_FLAG_POS].toInt()
			logger.fine(
				"framingFlag$framingFlag"
			)
			if (framingFlag != 0) {
				isValid = true
			}
		}
	}

	private fun u(i: Int): Int {
		return i and 0xFF
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom")
		const val FIELD_VORBIS_VERSION_POS = 7
		const val FIELD_AUDIO_CHANNELS_POS = 11
		const val FIELD_AUDIO_SAMPLE_RATE_POS = 12
		const val FIELD_BITRATE_MAX_POS = 16
		const val FIELD_BITRATE_NOMAIML_POS = 20
		const val FIELD_BITRATE_MIN_POS = 24
		const val FIELD_BLOCKSIZE_POS = 28
		const val FIELD_FRAMING_FLAG_POS = 29
		const val FIELD_VORBIS_VERSION_LENGTH = 4
		const val FIELD_AUDIO_CHANNELS_LENGTH = 1
		const val FIELD_AUDIO_SAMPLE_RATE_LENGTH = 4
		const val FIELD_BITRATE_MAX_LENGTH = 4
		const val FIELD_BITRATE_NOMAIML_LENGTH = 4
		const val FIELD_BITRATE_MIN_LENGTH = 4
		const val FIELD_BLOCKSIZE_LENGTH = 1
		const val FIELD_FRAMING_FLAG_LENGTH = 1
	}
}

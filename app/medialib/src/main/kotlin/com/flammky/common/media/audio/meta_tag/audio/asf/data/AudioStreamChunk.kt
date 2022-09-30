/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.math.BigInteger

/**
 * This class represents the stream chunk describing an audio stream. <br></br>
 *
 * @author Christian Laireiter
 */
class AudioStreamChunk
/**
 * Creates an instance.
 *
 * @param chunkLen Length of the entire chunk (including guid and size)
 */
	(chunkLen: BigInteger?) : StreamChunk(GUID.GUID_AUDIOSTREAM, chunkLen) {
	/**
	 * @return Returns the averageBytesPerSec.
	 */
	/**
	 * @param avgeBytesPerSec The averageBytesPerSec to set.
	 */
	/**
	 * Stores the average amount of bytes used by audio stream. <br></br>
	 * This value is a field within type specific data of audio stream. Maybe it
	 * could be used to calculate the KBPs.
	 */
	var averageBytesPerSec: Long = 0
	/**
	 * @return Returns the bitsPerSample.
	 */
	/**
	 * Sets the bitsPerSample
	 *
	 * @param bps
	 */
	/**
	 * Amount of bits used per sample. <br></br>
	 */
	var bitsPerSample = 0
	/**
	 * @return Returns the blockAlignment.
	 */
	/**
	 * Sets the blockAlignment.
	 *
	 * @param align
	 */
	/**
	 * The block alignment of the audio data.
	 */
	var blockAlignment: Long = 0
	/**
	 * @return Returns the channelCount.
	 */
	/**
	 * @param channels The channelCount to set.
	 */
	/**
	 * Number of channels.
	 */
	var channelCount: Long = 0
	/**
	 * @return Returns the codecData.
	 */
	/**
	 * Sets the codecData
	 *
	 * @param codecSpecificData
	 */
	/**
	 * Some data which needs to be interpreted if the codec is handled.
	 */
	var codecData = ByteArray(0)
		get() = field.clone()
		set(codecSpecificData) {
			requireNotNull(codecSpecificData)
			field = codecSpecificData.clone()
		}
	/**
	 * @return Returns the compressionFormat.
	 */
	/**
	 * @param cFormatCode The compressionFormat to set.
	 */
	/**
	 * The audio compression format code.
	 */
	var compressionFormat: Long = 0
	/**
	 * @return Returns the errorConcealment.
	 */
	/**
	 * This method sets the error concealment type which is given by two GUIDs. <br></br>
	 *
	 * @param errConc the type of error concealment the audio stream is stored as.
	 */
	/**
	 * this field stores the error concealment type.
	 */
	var errorConcealment: GUID? = null
	/**
	 * @return Returns the samplingRate.
	 */
	/**
	 * @param sampRate The samplingRate to set.
	 */
	/**
	 * Sampling rate of audio stream.
	 */
	var samplingRate: Long = 0

	/**
	 * This method will take a look at [.compressionFormat]and returns a
	 * String with its hex value and if known a textual note on what coded it
	 * represents. <br></br>
	 *
	 * @return A description for the used codec.
	 */
	val codecDescription: String
		get() {
			val result = StringBuilder(
				java.lang.Long.toHexString(
					compressionFormat
				)
			)
			var furtherDesc = " (Unknown)"
			for (aCODEC_DESCRIPTIONS in CODEC_DESCRIPTIONS) {
				if (aCODEC_DESCRIPTIONS[0].equals(result.toString(), ignoreCase = true)) {
					furtherDesc = aCODEC_DESCRIPTIONS[1]
					break
				}
			}
			if (result.length % 2 == 0) {
				result.insert(0, "0x")
			} else {
				result.insert(0, "0x0")
			}
			result.append(furtherDesc)
			return result.toString()
		}

	/**
	 * This method takes the value of [.getAverageBytesPerSec]and
	 * calculates the kbps out of it, by simply multiplying by 8 and dividing by
	 * 1000. <br></br>
	 *
	 * @return amount of bits per second in kilo bits.
	 */
	val kbps: Int
		get() = averageBytesPerSec.toInt() * 8 / 1000

	/**
	 * This mehtod returns whether the audio stream data is error concealed. <br></br>
	 * For now only interleaved concealment is known. <br></br>
	 *
	 * @return `true` if error concealment is used.
	 */
	val isErrorConcealed: Boolean
		get() = errorConcealment == GUID.GUID_AUDIO_ERROR_CONCEALEMENT_INTERLEAVED

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.append(prefix).append("  |-> Audio info:").append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |  : Bitrate : ").append(kbps).append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |  : Channels : ").append(channelCount).append(" at ")
			.append(
				samplingRate
			).append(" Hz").append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |  : Bits per Sample: ").append(bitsPerSample)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |  : Formatcode: ").append(codecDescription)
			.append(Utils.LINE_SEPARATOR)
		return result.toString()
	}

	companion object {
		/**
		 * Stores the hex values of codec identifiers to their descriptions. <br></br>
		 */
		val CODEC_DESCRIPTIONS = arrayOf(
			arrayOf("161", " (Windows Media Audio (ver 7,8,9))"),
			arrayOf("162", " (Windows Media Audio 9 series (Professional))"),
			arrayOf("163", "(Windows Media Audio 9 series (Lossless))"),
			arrayOf("7A21", " (GSM-AMR (CBR))"),
			arrayOf("7A22", " (GSM-AMR (VBR))")
		)

		/**
		 * Stores the audio codec number for WMA
		 */
		const val WMA: Long = 0x161

		/**
		 * Stores the audio codec number for WMA (CBR)
		 */
		const val WMA_CBR: Long = 0x7A21

		/**
		 * Stores the audio codec number for WMA_LOSSLESS
		 */
		const val WMA_LOSSLESS: Long = 0x163

		/**
		 * Stores the audio codec number for WMA_PRO
		 */
		const val WMA_PRO: Long = 0x162

		/**
		 * Stores the audio codec number for WMA (VBR)
		 */
		const val WMA_VBR: Long = 0x7A22
	}
}

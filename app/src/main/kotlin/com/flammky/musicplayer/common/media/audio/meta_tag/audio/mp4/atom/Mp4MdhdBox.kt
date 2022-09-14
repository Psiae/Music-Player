package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.u
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MdhdBox ( media (stream) header), holds the Sampling Rate used.
 */
class Mp4MdhdBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	var sampleRate = 0
	var timeLength: Long = 0

	/**
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		dataBuffer.order(ByteOrder.BIG_ENDIAN)
		val version = dataBuffer[VERSION_FLAG_POS]
		if (version.toInt() == LONG_FORMAT) {
			sampleRate = dataBuffer.getInt(TIMESCALE_LONG_POS)
			timeLength = dataBuffer.getLong(DURATION_LONG_POS)
		} else {
			sampleRate = dataBuffer.getInt(TIMESCALE_SHORT_POS)
			timeLength = u(dataBuffer.getInt(DURATION_SHORT_POS))
		}
	}

	companion object {
		const val VERSION_FLAG_POS = 0
		const val OTHER_FLAG_POS = 1
		const val CREATED_DATE_SHORT_POS = 4
		const val MODIFIED_DATE_SHORT_POS = 8
		const val TIMESCALE_SHORT_POS = 12
		const val DURATION_SHORT_POS = 16
		const val CREATED_DATE_LONG_POS = 4
		const val MODIFIED_DATE_LONG_POS = 12
		const val TIMESCALE_LONG_POS = 20
		const val DURATION_LONG_POS = 24
		const val VERSION_FLAG_LENGTH = 1
		const val OTHER_FLAG_LENGTH = 3
		const val CREATED_DATE_SHORT_LENGTH = 4
		const val MODIFIED_DATE_SHORT_LENGTH = 4
		const val CREATED_DATE_LONG_LENGTH = 8
		const val MODIFIED_DATE_LONG_LENGTH = 8
		const val TIMESCALE_LENGTH = 4
		const val DURATION_SHORT_LENGTH = 4
		const val DURATION_LONG_LENGTH = 8
		private const val LONG_FORMAT = 1
	}
}

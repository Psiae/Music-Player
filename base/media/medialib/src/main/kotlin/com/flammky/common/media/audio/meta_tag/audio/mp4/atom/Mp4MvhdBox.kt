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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.u
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MvhdBox (movie (presentation) header box)
 *
 *
 * This MP4Box contains important audio information we need. It can be used to calculate track length,
 * depending on the version field this can be in either short or long format
 */
class Mp4MvhdBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	private var timeScale = 0
	private var timeLength: Long = 0

	/**
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		dataBuffer.order(ByteOrder.BIG_ENDIAN)
		val version = dataBuffer[VERSION_FLAG_POS]
		if (version.toInt() == LONG_FORMAT) {
			timeScale = dataBuffer.getInt(TIMESCALE_LONG_POS)
			timeLength = dataBuffer.getLong(DURATION_LONG_POS)
		} else {
			timeScale = dataBuffer.getInt(TIMESCALE_SHORT_POS)
			timeLength = u(dataBuffer.getInt(DURATION_SHORT_POS))
		}
	}

	val length: Int
		get() = (timeLength / timeScale).toInt()
	val preciseLength: Double
		get() = timeLength / timeScale.toDouble()

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

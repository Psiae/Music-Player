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
 * This class represents the "Stream Bitrate Properties" chunk of an ASF media
 * file. <br></br>
 * It is optional, but contains useful information about the streams bitrate.<br></br>
 *
 * @author Christian Laireiter
 */
class StreamBitratePropertiesChunk(chunkLen: BigInteger?) :
	Chunk(GUID.GUID_STREAM_BITRATE_PROPERTIES, chunkLen) {
	/**
	 * For each call of [.addBitrateRecord] an [Long]
	 * object is appended, which represents the average bitrate.
	 */
	private val bitRates: MutableList<Long>

	/**
	 * For each call of [.addBitrateRecord] an [Integer]
	 * object is appended, which represents the stream-number.
	 */
	private val streamNumbers: MutableList<Int>

	/**
	 * Creates an instance.
	 *
	 * @param chunkLen Length of current chunk.
	 */
	init {
		bitRates = ArrayList()
		streamNumbers = ArrayList()
	}

	/**
	 * Adds the public values of a stream-record.
	 *
	 * @param streamNum      The number of the referred stream.
	 * @param averageBitrate Its average bitrate.
	 */
	fun addBitrateRecord(streamNum: Int, averageBitrate: Long) {
		streamNumbers.add(streamNum)
		bitRates.add(averageBitrate)
	}

	/**
	 * Returns the average bitrate of the given stream.<br></br>
	 *
	 * @param streamNumber Number of the stream whose bitrate to determine.
	 * @return The average bitrate of the numbered stream. `-1` if no
	 * information was given.
	 */
	fun getAvgBitrate(streamNumber: Int): Long {
		val index = streamNumbers.indexOf(streamNumber)
		val result: Long
		result = if (index == -1) {
			-1
		} else {
			bitRates[index]
		}
		return result
	}

	/**
	 * (overridden)
	 *
	 * @see Chunk.prettyPrint
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		for (i in bitRates.indices) {
			result.append(prefix).append("  |-> Stream no. \"").append(streamNumbers[i])
				.append("\" has an average bitrate of \"").append(
					bitRates[i]
				).append('"').append(Utils.LINE_SEPARATOR)
		}
		return result.toString()
	}
}

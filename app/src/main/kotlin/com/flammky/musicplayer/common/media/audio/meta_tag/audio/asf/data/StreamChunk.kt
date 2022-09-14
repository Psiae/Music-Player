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
 * This class is the base for all handled stream contents. <br></br>
 * A Stream chunk delivers information about a audio or video stream. Because of
 * this the stream chunk identifies in one field what type of stream it is
 * describing and so other data is provided. However some information is common
 * to all stream chunks which are stored in this hierarchy of the class tree.
 *
 * @author Christian Laireiter
 */
abstract class StreamChunk(streamType: GUID?, chunkLen: BigInteger?) :
	Chunk(GUID.GUID_STREAM, chunkLen) {
	/**
	 * @return Returns the contentEncrypted.
	 */
	/**
	 * @param cntEnc The contentEncrypted to set.
	 */
	/**
	 * If `true`, the stream data is encrypted.
	 */
	var isContentEncrypted = false
	/**
	 * @return Returns the streamNumber.
	 */
	/**
	 * @param streamNum The streamNumber to set.
	 */
	/**
	 * This field stores the number of the current stream. <br></br>
	 */
	var streamNumber = 0
	/**
	 * @return Returns the streamSpecificDataSize.
	 */
	/**
	 * @param strSpecDataSize The streamSpecificDataSize to set.
	 */
	/**
	 * @see .typeSpecificDataSize
	 */
	var streamSpecificDataSize: Long = 0
	/**
	 * @return Returns the timeOffset.
	 */
	/**
	 * @param timeOffs sets the time offset
	 */
	/**
	 * Something technical. <br></br>
	 * Format time in 100-ns steps.
	 */
	var timeOffset: Long = 0
	/**
	 * Returns the stream type of the stream chunk.<br></br>
	 *
	 * @return [GUID.GUID_AUDIOSTREAM] or [GUID.GUID_VIDEOSTREAM].
	 */
	/**
	 * Stores the stream type.<br></br>
	 *
	 * @see GUID.GUID_AUDIOSTREAM
	 *
	 * @see GUID.GUID_VIDEOSTREAM
	 */
	val streamType: GUID?
	/**
	 * @return Returns the typeSpecificDataSize.
	 */
	/**
	 * @param typeSpecDataSize The typeSpecificDataSize to set.
	 */
	/**
	 * Stores the size of type specific data structure within chunk.
	 */
	var typeSpecificDataSize: Long = 0

	/**
	 * Creates an instance
	 *
	 * @param streamType The GUID which tells the stream type represented (
	 * [GUID.GUID_AUDIOSTREAM] or [GUID.GUID_VIDEOSTREAM]
	 * ):
	 * @param chunkLen   length of chunk
	 */
	init {
		assert(GUID.GUID_AUDIOSTREAM.equals(streamType) || GUID.GUID_VIDEOSTREAM.equals(streamType))
		this.streamType = streamType
	}

	/**
	 * (overridden)
	 *
	 * @see Chunk.prettyPrint
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.append(prefix).append("  |-> Stream number: ").append(streamNumber)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |-> Type specific data size  : ").append(
			typeSpecificDataSize
		).append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |-> Stream specific data size: ").append(
			streamSpecificDataSize
		).append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |-> Time Offset              : ").append(timeOffset).append(
			Utils.LINE_SEPARATOR
		)
		result.append(prefix).append("  |-> Content Encryption       : ").append(isContentEncrypted)
			.append(
				Utils.LINE_SEPARATOR
			)
		return result.toString()
	}
}

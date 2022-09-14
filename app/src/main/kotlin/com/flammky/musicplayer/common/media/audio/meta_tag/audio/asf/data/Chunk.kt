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
 * This class represents a chunk within ASF streams. <br></br>
 * Each chunk starts with a 16byte [GUID] identifying the type.
 * After that a number (represented by 8 bytes) follows which shows the size in
 * bytes of the chunk. Finally there is the data of the chunk.
 *
 * @author Christian Laireiter
 */
open class Chunk {
	/**
	 * @return Returns the chunkLength.
	 */
	/**
	 * The length of current chunk. <br></br>
	 */
	val chunkLength: BigInteger
	/**
	 * @return Returns the guid.
	 */
	/**
	 * The GUID of represented chunk header.
	 */
	val guid: GUID
	/**
	 * @return Returns the position.
	 */
	/**
	 * Sets the position.
	 *
	 * @param pos position to set.
	 */
	/**
	 * The position of current header object within file or stream.
	 */
	var position: Long = 0

	/**
	 * Creates an instance
	 *
	 * @param headerGuid The GUID of header object.
	 * @param chunkLen   Length of current chunk.
	 */
	constructor(headerGuid: GUID?, chunkLen: BigInteger?) {
		requireNotNull(headerGuid) { "GUID must not be null." }
		require(!(chunkLen == null || chunkLen.compareTo(BigInteger.ZERO) < 0)) { "chunkLen must not be null nor negative." }
		guid = headerGuid
		chunkLength = chunkLen
	}

	/**
	 * Creates an instance
	 *
	 * @param headerGuid The GUID of header object.
	 * @param pos        Position of header object within stream or file.
	 * @param chunkLen   Length of current chunk.
	 */
	constructor(headerGuid: GUID?, pos: Long, chunkLen: BigInteger?) {
		requireNotNull(headerGuid) { "GUID must not be null" }
		require(pos >= 0) { "Position of header can't be negative." }
		require(!(chunkLen == null || chunkLen.compareTo(BigInteger.ZERO) < 0)) { "chunkLen must not be null nor negative." }
		guid = headerGuid
		position = pos
		chunkLength = chunkLen
	}

	/**
	 * This method returns the End of the current chunk introduced by current
	 * header object.
	 *
	 * @return Position after current chunk.
	 */
	@get:Deprecated("typo, use {@link #getChunkEnd()} instead.")
	val chunckEnd: Long
		get() = position + chunkLength.toLong()

	/**
	 * This method returns the End of the current chunk introduced by current
	 * header object.
	 *
	 * @return Position after current chunk.
	 */
	val chunkEnd: Long
		get() = position + chunkLength.toLong()

	/**
	 * This method creates a String containing useful information prepared to be
	 * printed on STD-OUT. <br></br>
	 * This method is intended to be overwritten by inheriting classes.
	 *
	 * @param prefix each line gets this string prepended.
	 * @return Information of current Chunk Object.
	 */
	open fun prettyPrint(prefix: String): String {
		val result = StringBuilder()
		result.append(prefix).append("-> GUID: ").append(
			GUID.getGuidDescription(
				guid
			)
		).append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  | : Starts at position: ").append(position)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  | : Last byte at: ").append(chunkEnd - 1)
			.append(Utils.LINE_SEPARATOR)
		return result.toString()
	}

	/**
	 * (overridden)
	 *
	 * @see Object.toString
	 */
	override fun toString(): String {
		return prettyPrint("")
	}
}

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
 * This class was intended to store the data of a chunk which contained the
 * encoding parameters in textual form. <br></br>
 * Since the needed parameters were found in other chunks the implementation of
 * this class was paused. <br></br>
 * TODO complete analysis.
 *
 * @author Christian Laireiter
 */
class EncodingChunk(chunkLen: BigInteger?) : Chunk(GUID.GUID_ENCODING, chunkLen) {
	/**
	 * The read strings.
	 */
	private val strings: MutableList<String>

	/**
	 * Creates an instance.
	 *
	 * @param chunkLen Length of current chunk.
	 */
	init {
		strings = ArrayList()
	}

	/**
	 * This method appends a String.
	 *
	 * @param toAdd String to add.
	 */
	fun addString(toAdd: String) {
		strings.add(toAdd)
	}

	/**
	 * This method returns a collection of all [Strings][String] which
	 * were added due [.addString].
	 *
	 * @return Inserted Strings.
	 */
	fun getStrings(): Collection<String> {
		return ArrayList(strings)
	}

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		strings.iterator()
		for (string in strings) {
			result.append(prefix).append("  | : ").append(string).append(Utils.LINE_SEPARATOR)
		}
		return result.toString()
	}
}

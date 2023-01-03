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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.ContentDescription
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * Reads and interprets the data of a ASF chunk containing title, author... <br></br>
 *
 * @author Christian Laireiter
 * @see ContentDescription
 */
class ContentDescriptionReader
/**
 * Should not be used for now.
 */
protected constructor() : ChunkReader {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return false
	}

	/**
	 * {@inheritDoc}
	 */
	override val applyingIds: Array<GUID>
		get() = APPLYING.clone()

	/**
	 * Returns the next 5 UINT16 values as an array.<br></br>
	 *
	 * @param stream stream to read from
	 * @return 5 int values read from stream.
	 * @throws IOException on I/O Errors.
	 */
	@Throws(IOException::class)
	private fun getStringSizes(stream: InputStream): IntArray {
		val result = IntArray(5)
		for (i in result.indices) {
			result[i] = Utils.readUINT16(stream)
		}
		return result
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(guid: GUID?, stream: InputStream, chunkStart: Long): Chunk? {
		val chunkSize = Utils.readBig64(stream)
		/*
		 * Now comes 16-Bit values representing the length of the Strings which
		 * follows.
		 */
		val stringSizes = getStringSizes(stream)

		/*
		 * Now we know the String length of each occuring String.
		 */
		val strings = arrayOfNulls<String>(stringSizes.size)
		for (i in strings.indices) {
			if (stringSizes[i] > 0) {
				strings[i] = Utils.readFixedSizeUTF16Str(stream, stringSizes[i])
			}
		}
		/*
		 * Now create the result
		 */
		val result = ContentDescription(chunkStart, chunkSize)
		if (stringSizes[0] > 0) {
			result.title = strings[0]
		}
		if (stringSizes[1] > 0) {
			result.author = strings[1]
		}
		if (stringSizes[2] > 0) {
			result.setCopyright(strings[2])
		}
		if (stringSizes[3] > 0) {
			result.comment = strings[3]
		}
		if (stringSizes[4] > 0) {
			result.rating = strings[4]
		}
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_CONTENTDESCRIPTION)
	}
}

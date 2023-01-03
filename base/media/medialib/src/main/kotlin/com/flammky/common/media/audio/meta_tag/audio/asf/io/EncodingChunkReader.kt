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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.EncodingChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * This class reads the chunk containing encoding data <br></br>
 * **Warning:**<br></br>
 * Implementation is not completed. More analysis of this chunk is needed.
 *
 * @author Christian Laireiter
 **** */
internal class EncodingChunkReader
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
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(guid: GUID?, stream: InputStream, chunkStart: Long): Chunk? {
		val chunkLen = Utils.readBig64(stream)
		val result = EncodingChunk(chunkLen)
		var readBytes = 24
		// Can't be interpreted
		/*
		 * What do I think of this data, well it seems to be another GUID. Then
		 * followed by a UINT16 indicating a length of data following (by half).
		 * My test files just had the length of one and a two bytes zero.
		 */stream.skip(20)
		readBytes += 20

		/*
		 * Read the number of strings which will follow
		 */
		val stringCount = Utils.readUINT16(stream)
		readBytes += 2

		/*
		 * Now reading the specified amount of strings.
		 */for (i in 0 until stringCount) {
			val curr = Utils.readCharacterSizedString(stream)
			result.addString(curr)
			readBytes += 4 + 2 * curr.length
		}
		stream.skip(chunkLen.toLong() - readBytes)
		result.position = chunkStart
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_ENCODING)
	}
}

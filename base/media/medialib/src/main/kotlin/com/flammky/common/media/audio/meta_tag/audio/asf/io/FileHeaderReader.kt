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
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.FileHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * Reads and interprets the data of the file header. <br></br>
 *
 * @author Christian Laireiter
 */
class FileHeaderReader
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
		// Skip client GUID.
		stream.skip(16)
		val fileSize = Utils.readBig64(stream)
		// fileTime in 100 ns since midnight of 1st january 1601 GMT
		val fileTime = Utils.readBig64(stream)
		val packageCount = Utils.readBig64(stream)
		val timeEndPos = Utils.readBig64(stream)
		val duration = Utils.readBig64(stream)
		val timeStartPos = Utils.readBig64(stream)
		val flags = Utils.readUINT32(stream)
		val minPkgSize = Utils.readUINT32(stream)
		val maxPkgSize = Utils.readUINT32(stream)
		val uncompressedFrameSize = Utils.readUINT32(stream)
		val result = FileHeader(
			chunkLen,
			fileSize,
			fileTime,
			packageCount,
			duration,
			timeStartPos,
			timeEndPos,
			flags,
			minPkgSize,
			maxPkgSize,
			uncompressedFrameSize
		)
		result.position = chunkStart
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_FILE)
	}
}

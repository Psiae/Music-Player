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

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * Reads and interprets the data of the audio or video stream information chunk. <br></br>
 *
 * @author Christian Laireiter
 */
class StreamChunkReader
/**
 * Shouldn't be used for now.
 */
protected constructor() : ChunkReader {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return true
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
		var result: StreamChunk? = null
		val chunkLength = Utils.readBig64(stream)
		// Now comes GUID indicating whether stream content type is audio or
		// video
		val streamTypeGUID = Utils.readGUID(stream)
		if (GUID.GUID_AUDIOSTREAM.equals(streamTypeGUID) || GUID.GUID_VIDEOSTREAM.equals(
				streamTypeGUID
			)
		) {

			// A GUID is indicating whether the stream is error
			// concealed
			val errorConcealment = Utils.readGUID(stream)
			/*
			 * Read the Time Offset
			 */
			val timeOffset = Utils.readUINT64(stream)
			val typeSpecificDataSize = Utils.readUINT32(stream)
			val streamSpecificDataSize = Utils.readUINT32(stream)

			/*
			 * Read a bit field. (Contains stream number, and whether the stream
			 * content is encrypted.)
			 */
			val mask = Utils.readUINT16(stream)
			val streamNumber = mask and 127
			val contentEncrypted = mask and 0x8000 != 0

			/*
			 * Skip a reserved field
			 */stream.skip(4)

			/*
			 * very important to set for every stream type. The size of bytes
			 * read by the specific stream type, in order to skip the remaining
			 * unread bytes of the stream chunk.
			 */
			val streamSpecificBytes: Long
			if (GUID.GUID_AUDIOSTREAM.equals(streamTypeGUID)) {
				/*
				 * Reading audio specific information
				 */
				val audioStreamChunk = AudioStreamChunk(chunkLength)
				result = audioStreamChunk

				/*
				 * read WAVEFORMATEX and format extension.
				 */
				val compressionFormat = Utils.readUINT16(stream).toLong()
				val channelCount = Utils.readUINT16(stream).toLong()
				val samplingRate = Utils.readUINT32(stream)
				val avgBytesPerSec = Utils.readUINT32(stream)
				val blockAlignment = Utils.readUINT16(stream).toLong()
				val bitsPerSample = Utils.readUINT16(stream)
				val codecSpecificDataSize = Utils.readUINT16(stream)
				val codecSpecificData = ByteArray(codecSpecificDataSize)
				stream.read(codecSpecificData)
				audioStreamChunk.compressionFormat = compressionFormat
				audioStreamChunk.channelCount = channelCount
				audioStreamChunk.samplingRate = samplingRate
				audioStreamChunk.averageBytesPerSec = avgBytesPerSec
				audioStreamChunk.errorConcealment = errorConcealment
				audioStreamChunk.blockAlignment = blockAlignment
				audioStreamChunk.bitsPerSample = bitsPerSample
				audioStreamChunk.codecData = codecSpecificData
				streamSpecificBytes = (18 + codecSpecificData.size).toLong()
			} else {
				/*
				 * Reading video specific information
				 */
				val videoStreamChunk = VideoStreamChunk(chunkLength)
				result = videoStreamChunk
				val pictureWidth = Utils.readUINT32(stream)
				val pictureHeight = Utils.readUINT32(stream)

				// Skip unknown field
				stream.skip(1)

				/*
				 * Now read the format specific data
				 */
				// Size of the data section (formatDataSize)
				stream.skip(2)
				stream.skip(16)
				val fourCC = ByteArray(4)
				stream.read(fourCC)
				videoStreamChunk.pictureWidth = pictureWidth
				videoStreamChunk.pictureHeight = pictureHeight
				videoStreamChunk.setCodecId(fourCC)
				streamSpecificBytes = 31
			}

			/*
			 * Setting common values for audio and video
			 */result.streamNumber = streamNumber
			result.streamSpecificDataSize = streamSpecificDataSize
			result.typeSpecificDataSize = typeSpecificDataSize
			result.timeOffset = timeOffset
			result.isContentEncrypted = contentEncrypted
			result.position = chunkStart
			/*
			 * Now skip remainder of chunks bytes. chunk-length - 24 (size of
			 * GUID and chunklen) - streamSpecificBytes(stream type specific
			 * data) - 54 (common data)
			 */stream.skip(chunkLength.toLong() - 24 - streamSpecificBytes - 54)
		}
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_STREAM)
	}
}

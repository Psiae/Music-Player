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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NoWritePermissionsException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileWriter2
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Write/delete tag info for Dsf file
 */
class DsfFileWriter : AudioFileWriter2() {

	/** [org.jaudiotagger.audio.dsf.DsfFileWriter] */


	@Throws(CannotWriteException::class, UnsupportedOperationException::class)
	override fun writeTag(tag: Tag, file: Path) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException("Require API >= 26")

		try {
			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				val dsd: DsdChunk? =
					DsdChunk.readChunk(Utils.readFileDataIntoBufferLE(fc, DsdChunk.DSD_HEADER_LENGTH))
				if (dsd != null) {
					if (dsd.metadataOffset > 0) {
						fc.position(dsd.metadataOffset)
						//If room for an ID3 tag
						if (fc.size() - fc.position() >= DsfChunkType.ID3.code.length) {
							val id3Chunk: ID3Chunk? =
								ID3Chunk.readChunk(
									Utils.readFileDataIntoBufferLE(
										fc,
										(fc.size() - fc.position()).toInt()
									)
								)
							if (id3Chunk != null) {
								//Remove Existing tag
								fc.position(dsd.metadataOffset)
								fc.truncate(fc.position())
								val bb = convert(tag as ID3v2TagBase)
								fc.write(bb)
								dsd.fileLength = fc.size()
								fc.position(0)
								fc.write(dsd.write())
							} else {
								throw CannotWriteException(file.toString() + "Could not find existing ID3v2 Tag (1)")
							}
						} else {
							fc.position(dsd.metadataOffset)
							fc.truncate(fc.position())
							val bb = convert(tag as ID3v2TagBase)
							fc.write(bb)
							dsd.fileLength = fc.size()
							fc.position(0)
							fc.write(dsd.write())
						}
					} else {
						//Write new tag and new offset and size
						fc.position(fc.size())
						dsd.metadataOffset = fc.size()
						val bb = convert(tag as ID3v2TagBase)
						fc.write(bb)
						dsd.fileLength = fc.size()
						fc.position(0)
						fc.write(dsd.write())
					}
				}
			}
		} catch (ade: AccessDeniedException) {
			throw NoWritePermissionsException(file.toString() + ":" + ade.message)
		} catch (ioe: IOException) {
			throw CannotWriteException(ioe.message)
		}
	}

	/**
	 * Convert ID3 tag into a ByteBuffer, also ensures always even to avoid problems
	 *
	 * @param tag
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	fun convert(tag: ID3v2TagBase): ByteBuffer {
		return try {
			var baos = ByteArrayOutputStream()
			var existingTagSize = tag.size.toLong()

			//If existingTag is uneven size lets make it even
			if (existingTagSize > 0 && Utils.isOddLength(existingTagSize)) {
				existingTagSize++
			}

			//Write Tag to buffer and use (amended) existingTagSize to set the ID3 header if originally odd sized
			tag.write(baos, existingTagSize.toInt())

			//If making the adjusstment caused written buffer to be odd then add byte and try again
			if (Utils.isOddLength(baos.toByteArray().size.toLong())) {
				val newSize = baos.toByteArray().size + 1
				baos = ByteArrayOutputStream()
				tag.write(baos, newSize)
			}
			val buf = ByteBuffer.wrap(baos.toByteArray())
			buf.rewind()
			buf
		} catch (ioe: IOException) {
			//Should never happen as not writing to file at this point
			throw RuntimeException(ioe)
		}
	}

	/**
	 * Delete Metadata tag
	 *
	 * @param tag
	 * @param file
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, UnsupportedOperationException::class)
	override fun deleteTag(tag: Tag, file: Path) {
		if (!VersionHelper.hasOreo()) throw UnsupportedOperationException("Require API <= 26")

		try {
			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				val dsd: DsdChunk? =
					DsdChunk.readChunk(Utils.readFileDataIntoBufferLE(fc, DsdChunk.DSD_HEADER_LENGTH))
				if (dsd != null) {
					if (dsd.metadataOffset > 0) {
						fc.position(dsd.metadataOffset)
						val id3Chunk: ID3Chunk? =
							ID3Chunk.readChunk(
								Utils.readFileDataIntoBufferLE(
									fc,
									(fc.size() - fc.position()).toInt()
								)
							)
						if (id3Chunk != null) {
							fc.truncate(dsd.metadataOffset)
							//set correct value for fileLength and zero offset
							dsd.metadataOffset = 0
							dsd.fileLength = fc.size()
							fc.position(0)
							fc.write(dsd.write())
						}
					} else {
						//Do Nothing;
					}
				}
			}
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
	}
}

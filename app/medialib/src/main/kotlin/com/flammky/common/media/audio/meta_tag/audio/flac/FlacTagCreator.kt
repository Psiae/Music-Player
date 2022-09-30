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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.BlockType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataPicture
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AbstractTagCreator
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.flac.FlacTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentCreator
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.util.logging.Logger

/**
 * Create the tag data ready for writing to flac file
 */
class FlacTagCreator : AbstractTagCreator() {
	/**
	 * Convert Metadata
	 *
	 * @param tag
	 * @param isLastBlock
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	override fun convertMetadata(tag: Tag?, isLastBlock: Boolean): ByteBuffer {
		val flacTag = tag as FlacTag
		var tagLength = 0
		var vorbiscomment: ByteBuffer? = null
		if (flacTag.vorbisCommentTag != null) {
			vorbiscomment = creator.convertMetadata(flacTag.vorbisCommentTag)
			tagLength = vorbiscomment.capacity() + MetadataBlockHeader.HEADER_LENGTH
		}
		for (image in flacTag.getImages()) {
			tagLength += image.bytes.limit() + MetadataBlockHeader.HEADER_LENGTH
		}
		logger.config(
			"Convert flac tag:taglength:$tagLength"
		)
		val buf = ByteBuffer.allocate(tagLength)
		val vorbisHeader: MetadataBlockHeader
		//If there are other metadata blocks
		if (flacTag.vorbisCommentTag != null) {
			vorbisHeader = if (isLastBlock || flacTag.getImages().isNotEmpty()) {
				MetadataBlockHeader(false, BlockType.VORBIS_COMMENT, vorbiscomment!!.capacity())
			} else {
				MetadataBlockHeader(true, BlockType.VORBIS_COMMENT, vorbiscomment!!.capacity())
			}
			buf.put(vorbisHeader.bytes)
			buf.put(vorbiscomment)
		}

		//Images
		val li: ListIterator<MetadataBlockDataPicture> = flacTag.getImages().listIterator()
		while (li.hasNext()) {
			val imageField = li.next()
			val imageHeader: MetadataBlockHeader =
				if (isLastBlock || li.hasNext()) {
					MetadataBlockHeader(false, BlockType.PICTURE, imageField.length)
				} else {
					MetadataBlockHeader(true, BlockType.PICTURE, imageField.length)
				}
			buf.put(imageHeader.bytes)
			buf.put(imageField.bytes)
		}
		buf.rewind()
		return buf
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac")

		//TODO make an option
		const val DEFAULT_PADDING = 4000
		private val creator = VorbisCommentCreator()
	}
}

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
package org.jaudiotagger.audio.ogg

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.VorbisHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.VorbisPacketType
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.VorbisCommentCreator
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.util.logging.Logger

/**
 * Creates a Vorbis Comment Tag from a VorbisComment for use within an OggVorbis Container.
 *
 *
 * When a Vorbis Comment is used within OggVorbis it additionally has a vorbis header and a framing
 * bit.
 *
 *
 * When a Vorbis Comment is used within OpusVorbis it additionally has a OpusTags header.
 */
class OggVorbisCommentTagCreator {
	private val creator = VorbisCommentCreator()
	private val prefix: ByteArray
	private val capturePattern: ByteArray
	private val shouldWriteFramingBit: Boolean

	constructor(prefix: ByteArray, pattern: ByteArray, shouldWriteFramingBit: Boolean) {
		this.prefix = prefix
		capturePattern = pattern
		this.shouldWriteFramingBit = shouldWriteFramingBit
	}

	constructor() {
		prefix = byteArrayOf(VorbisPacketType.COMMENT_HEADER.type.toByte())
		capturePattern = VorbisHeader.CAPTURE_PATTERN_AS_BYTES
		shouldWriteFramingBit = true
	}

	// Creates the ByteBuffer for the ogg tag
	@Throws(UnsupportedEncodingException::class)
	fun convert(tag: Tag?): ByteBuffer {
		val ogg: ByteBuffer = creator.convertMetadata(tag)
		var tagLength = ogg.capacity() + prefix.size + capturePattern.size
		if (shouldWriteFramingBit) {
			tagLength += FIELD_FRAMING_BIT_LENGTH
		}
		val buf = ByteBuffer.allocate(tagLength)

		//[packet type=comment0x03]['vorbis']
		buf.put(prefix)
		buf.put(capturePattern)

		//The actual tag
		buf.put(ogg)

		//Framing bit = 1
		if (shouldWriteFramingBit) {
			buf.put(FRAMING_BIT_VALID_VALUE)
		}
		buf.rewind()
		return buf
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg")
		const val FIELD_FRAMING_BIT_LENGTH = 1
		const val FRAMING_BIT_VALID_VALUE = 0x01.toByte()
	}
}

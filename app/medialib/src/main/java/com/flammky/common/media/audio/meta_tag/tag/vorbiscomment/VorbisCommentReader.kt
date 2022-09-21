/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util.VorbisHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Create the VorbisCommentTag by reading from the raw packet data
 *
 *
 * This is in the same format whether encoded with Ogg or Flac
 * except the framing bit is only present when used within Ogg Vorbis
 *
 * <pre>
 * From the http://xiph.org/vorbis/doc/Vorbis_I_spec.html#vorbis-spec-comment
 * Read decodes the packet data using the following algorithm:
 * [vendor_length] = read an unsigned integer of 32 bits
 * [vendor_string] = read a UTF-8 vector as [vendor_length] octets
 * [user_comment_list_length] = read an unsigned integer of 32 bits
 * iterate [user_comment_list_length] times {
 * 5) [length] = read an unsigned integer of 32 bits
 * 6) this iteration's user comment = read a UTF-8 vector as [length] octets
 * }
 * [framing_bit] = read a single bit as boolean
 * if ( [framing_bit] unset or end-of-packet ) then ERROR
 * done.
</pre> *
 */
class VorbisCommentReader {
	/**
	 * @param rawdata
	 * @param isFramingBit
	 * @param path
	 * @return logical representation of VorbisCommentTag
	 * @throws IOException
	 * @throws CannotReadException
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun read(rawdata: ByteArray, isFramingBit: Boolean, path: Path?): VorbisCommentTag {
		val tag = VorbisCommentTag()
		var b = ByteArray(FIELD_VENDOR_LENGTH_LENGTH)
		System.arraycopy(
			rawdata,
			FIELD_VENDOR_LENGTH_POS,
			b,
			FIELD_VENDOR_LENGTH_POS,
			FIELD_VENDOR_LENGTH_LENGTH
		)
		var pos = FIELD_VENDOR_LENGTH_LENGTH
		val vendorStringLength = Utils.getIntLE(b)
		b = ByteArray(vendorStringLength)
		System.arraycopy(rawdata, pos, b, 0, vendorStringLength)
		pos += vendorStringLength
		tag.vendor = String(
			b,
			Charset.forName(VorbisHeader.CHARSET_UTF_8)
		)
		logger.config("Vendor is:" + tag.vendor)
		b = ByteArray(FIELD_USER_COMMENT_LIST_LENGTH)
		System.arraycopy(rawdata, pos, b, 0, FIELD_USER_COMMENT_LIST_LENGTH)
		pos += FIELD_USER_COMMENT_LIST_LENGTH
		val userComments = Utils.getIntLE(b)
		logger.config(
			"Number of user comments:$userComments"
		)
		for (i in 0 until userComments) {
			b = ByteArray(FIELD_COMMENT_LENGTH_LENGTH)
			System.arraycopy(rawdata, pos, b, 0, FIELD_COMMENT_LENGTH_LENGTH)
			pos += FIELD_COMMENT_LENGTH_LENGTH
			val commentLength = Utils.getIntLE(b)
			logger.config(
				"Next Comment Length:$commentLength"
			)
			if (commentLength > JAUDIOTAGGER_MAX_COMMENT_LENGTH) {
				if (path != null) {
					logger.warning(
						path.toString() + ":" + ErrorMessage.VORBIS_COMMENT_LENGTH_TOO_LARGE.getMsg(
							commentLength
						)
					)
				} else {
					logger.warning(ErrorMessage.VORBIS_COMMENT_LENGTH_TOO_LARGE.getMsg(commentLength))
				}
				break
			} else if (commentLength > rawdata.size - pos) {
				if (path != null) {
					logger.warning(
						path.toString() + ":" + ErrorMessage.VORBIS_COMMENT_LENGTH_LARGE_THAN_HEADER.getMsg(
							commentLength,
							rawdata.size - pos
						)
					)
				} else {
					logger.warning(
						ErrorMessage.VORBIS_COMMENT_LENGTH_LARGE_THAN_HEADER.getMsg(
							commentLength,
							rawdata.size
						)
					)
				}
				break
			} else {
				b = ByteArray(commentLength)
				System.arraycopy(rawdata, pos, b, 0, commentLength)
				pos += commentLength
				val fieldComment = VorbisCommentTagField(b)
				logger.config("Adding:" + fieldComment.id)
				tag.addField(fieldComment)
			}
		}

		//Check framing bit, only exists when vorbisComment used within OggVorbis
		if (isFramingBit) {
			if (rawdata[pos].toInt() and 0x01 != 1) {
				throw CannotReadException(
					ErrorMessage.OGG_VORBIS_NO_FRAMING_BIT.getMsg(
						rawdata[pos].toInt() and 0x01
					)
				)
			}
		}
		return tag
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.tag.vorbiscomment.VorbisCommentReader")
		const val FIELD_VENDOR_LENGTH_POS = 0
		const val FIELD_VENDOR_STRING_POS = 4
		const val FIELD_VENDOR_LENGTH_LENGTH = 4
		const val FIELD_USER_COMMENT_LIST_LENGTH = 4
		const val FIELD_COMMENT_LENGTH_LENGTH = 4

		/**
		 * max comment length that jaudiotagger can handle, this isnt the maximum column length allowed but we dont
		 * dont allow comments larger than this because of problem with allocating memory  (10MB shoudl be fine for all apps)
		 */
		private const val JAUDIOTAGGER_MAX_COMMENT_LENGTH = 10000000
	}
}

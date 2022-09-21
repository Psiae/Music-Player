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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagTextField
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * This class represents the name and content of a tag entry in ogg-files.
 * <br></br>
 *
 * @author Raphael Slinckx (KiKiDonK)
 * @author Christian Laireiter (liree)
 */
class VorbisCommentTagField : TagTextField {
	/**
	 * If `true`, the id of the current encapsulated tag field is
	 * specified as a common field. <br></br>
	 * Example is "ARTIST" which should be interpreted by any application as the
	 * artist of the media content. <br></br>
	 * Will be set during construction with [.checkCommon].
	 */
	override var isCommon = false
		private set

	/**
	 * Stores the content of the tag field. <br></br>
	 */
	override var content: String? = null

	/**
	 * Stores the id (name) of the tag field. <br></br>
	 */
	override var id: String? = null
		private set

	/**
	 * Creates an instance.
	 *
	 * @param raw Raw byte data of the tagfield.
	 * @throws UnsupportedEncodingException If the data doesn't conform "UTF-8" specification.
	 */
	constructor(raw: ByteArray?) {
		val field = String(raw!!, Charset.forName("UTF-8"))
		val i = field.indexOf("=")
		if (i == -1) {
			//Beware that ogg ID, must be capitalized and contain no space..
			id = ERRONEOUS_ID
			content = field
		} else {
			id = field.substring(0, i).uppercase(Locale.getDefault())
			if (field.length > i) {
				content = field.substring(i + 1)
			} else {
				//We have "XXXXXX=" with nothing after the "="
				content = ""
			}
		}
		checkCommon()
	}

	/**
	 * Creates an instance.
	 *
	 * @param fieldId      ID (name) of the field.
	 * @param fieldContent Content of the field.
	 */
	constructor(fieldId: String, fieldContent: String?) {
		id = fieldId.uppercase(Locale.getDefault())
		content = fieldContent
		checkCommon()
	}

	/**
	 * This method examines the ID of the current field and modifies
	 * [.common]in order to reflect if the tag id is a commonly used one.
	 * <br></br>
	 */
	private fun checkCommon() {
		isCommon = id == VorbisCommentFieldKey.TITLE.fieldName
			|| id == VorbisCommentFieldKey.ALBUM.fieldName
			|| id == VorbisCommentFieldKey.ARTIST.fieldName
			|| id == VorbisCommentFieldKey.GENRE.fieldName
			|| id == VorbisCommentFieldKey.TRACKNUMBER.fieldName
			|| id == VorbisCommentFieldKey.DATE.fieldName
			|| id == VorbisCommentFieldKey.DESCRIPTION.fieldName
			|| id == VorbisCommentFieldKey.COMMENT.fieldName
	}

	/**
	 * This method will copy all bytes of `src` to `dst`
	 * at the specified location.
	 *
	 * @param src       bytes to copy.
	 * @param dst       where to copy to.
	 * @param dstOffset at which position of `dst` the data should be
	 * copied.
	 */
	protected fun copy(src: ByteArray, dst: ByteArray?, dstOffset: Int) {
		//        for (int i = 0; i < src.length; i++)
		//            dst[i + dstOffset] = src[i];
		/*
		 * Heared that this method is optimized and does its job very near of
		 * the system.
		 */
		System.arraycopy(src, 0, dst, dstOffset, src.size)
	}

	override fun copyContent(field: TagField?) {
		if (field is TagTextField) {
			content = field.content
		}
	}

	override var encoding: Charset?
		get() = StandardCharsets.UTF_8
		set(s) {
			if (StandardCharsets.UTF_8 != s) {
				throw UnsupportedOperationException("The encoding of OggTagFields cannot be " + "changed.(specified to be UTF-8)")
			}
		}

	// "="
	@get:Throws(UnsupportedEncodingException::class)
	override val rawContent: ByteArray
		get() {
			val size = ByteArray(VorbisCommentReader.FIELD_COMMENT_LENGTH_LENGTH)
			val idBytes = id!!.toByteArray(StandardCharsets.ISO_8859_1)
			val contentBytes = content!!.toByteArray(StandardCharsets.UTF_8)
			val b = ByteArray(4 + idBytes.size + 1 + contentBytes.size)
			val length = idBytes.size + 1 + contentBytes.size
			size[3] = (length and -0x1000000 shr 24).toByte()
			size[2] = (length and 0x00FF0000 shr 16).toByte()
			size[1] = (length and 0x0000FF00 shr 8).toByte()
			size[0] = (length and 0x000000FF).toByte()
			var offset = 0
			copy(size, b, offset)
			offset += 4
			copy(idBytes, b, offset)
			offset += idBytes.size
			b[offset] = 0x3D.toByte()
			offset++ // "="
			copy(contentBytes, b, offset)
			return b
		}
	override val isBinary: Boolean
		get() = false

	override fun isBinary(b: Boolean) {
		if (b) {
			// Only throw if binary = true requested.
			throw UnsupportedOperationException(
				"""
    OggTagFields cannot be changed to binary.
    binary data should be stored elsewhere according to Vorbis_I_spec.
    """.trimIndent()
			)
		}
	}

	override val isEmpty: Boolean
		get() = content == ""

	override fun toDescriptiveString(): String {
		return content!!
	}

	companion object {
		/**
		 * If id is invalid
		 */
		private const val ERRONEOUS_ID = "ERRONEOUS"
	}
}

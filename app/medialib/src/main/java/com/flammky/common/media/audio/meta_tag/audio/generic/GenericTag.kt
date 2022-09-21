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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * This is a complete example implementation of  [AbstractTag]
 *
 * @author Raphaël Slinckx
 */
abstract class GenericTag : AbstractTag() {
	/**
	 * Implementations of [TagTextField] for use with
	 * &quot;ISO-8859-1&quot; strings.
	 *
	 * @author Raphaël Slinckx
	 */
	protected inner class GenericTagTextField(
		override val id: String,
		override var content: String?
	) : TagTextField {

		override val isEmpty: Boolean
			get() = content == ""

		override fun copyContent(field: TagField?) {
			if (field is TagTextField) {
				content = field.content!!
			}
		}

		/* Not allowed */
		override var encoding: Charset?
			get() = StandardCharsets.ISO_8859_1
			set(s) {
				/* Not allowed */
			}
		override val rawContent: ByteArray
			get() = if (content == null) EMPTY_BYTE_ARRAY else content!!.toByteArray(encoding!!)
		override val isBinary: Boolean
			get() = false

		override fun isBinary(b: Boolean) {
			/* not supported */
		}

		override val isCommon: Boolean
			get() = true


		override fun toDescriptiveString(): String {
			return content.toString()
		}
	}

	override fun isAllowedEncoding(enc: Charset?): Boolean {
		return true
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		return if (supportedKeys.contains(genericKey)) {
			require(values[0] != null) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			GenericTagTextField(genericKey!!.name, values[0])
		} else {
			throw UnsupportedOperationException(
				ErrorMessage.OPERATION_NOT_SUPPORTED_FOR_FIELD.getMsg(
					genericKey
				)
			)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirst(genericKey: FieldKey?): String? {
		return getValue(genericKey, 0)
	}

	@Throws(KeyNotFoundException::class)
	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		return if (supportedKeys.contains(
				genericKey
			)
		) {
			getItem(genericKey!!.name, n)
		} else {
			throw UnsupportedOperationException(
				ErrorMessage.OPERATION_NOT_SUPPORTED_FOR_FIELD.getMsg(
					genericKey
				)
			)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getFields(genericKey: FieldKey?): List<TagField?> {
		return fieldMap[genericKey?.name] ?: return ArrayList()
	}

	@Throws(KeyNotFoundException::class)
	override fun getAll(genericKey: FieldKey?): List<String?> {
		return super.getAll(genericKey?.name)
	}

	@Throws(KeyNotFoundException::class)
	override fun deleteField(genericKey: FieldKey?) {
		if (supportedKeys.contains(genericKey)) {
			deleteField(genericKey!!.name)
		} else {
			throw UnsupportedOperationException(
				ErrorMessage.OPERATION_NOT_SUPPORTED_FOR_FIELD.getMsg(
					genericKey
				)
			)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(id: FieldKey?): TagField? {
		return if (supportedKeys.contains(
				id
			)
		) {
			getFirstField(id!!.name)
		} else {
			throw UnsupportedOperationException(
				ErrorMessage.OPERATION_NOT_SUPPORTED_FOR_FIELD.getMsg(
					id
				)
			)
		}
	}

	override val artworkList: List<Artwork>
		get() = emptyList()

	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		throw UnsupportedOperationException(ErrorMessage.GENERIC_NOT_SUPPORTED.msg)
	}

	companion object {
		private val EMPTY_BYTE_ARRAY = byteArrayOf()
		val supportedKeys: EnumSet<FieldKey> = EnumSet.of(
			FieldKey.ALBUM,
			FieldKey.ARTIST,
			FieldKey.TITLE,
			FieldKey.TRACK,
			FieldKey.GENRE,
			FieldKey.COMMENT,
			FieldKey.YEAR
		)

	}
}

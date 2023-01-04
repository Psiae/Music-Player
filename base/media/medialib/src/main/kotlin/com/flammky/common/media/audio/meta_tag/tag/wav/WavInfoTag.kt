/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericTag
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import java.util.*

/**
 * Represent wav metadata found in the LISTINFO Chunk
 *
 * An LIST INFO chunk was the original way to store metadata but similarly to ID3v1 it suffers from a limited
 * set of fields, although non-standard extra field cannot be added, notably there is no support for images.
 *
 * Many Wavc editors now instead/additionally add data with an ID3 tag
 */
class WavInfoTag : GenericTag() {
	//We dont use these fields but we need to read them so they can be written back if user modifies
	private val unrecognisedFields: MutableList<TagTextField> = ArrayList()
	var startLocationInFile: Long? = null
		private set

	//End location of this chunk
	var endLocationInFile: Long? = null
		private set

	override fun toString(): String {
		val output = StringBuilder("Wav Info Tag:\n")
		if (startLocationInFile != null) {
			output.append(
				"\tstartLocation:" + Hex.asDecAndHex(
					startLocationInFile!!
				) + "\n"
			)
		}
		if (endLocationInFile != null) {
			output.append(
				"\tendLocation:" + Hex.asDecAndHex(
					endLocationInFile!!
				) + "\n"
			)
		}
		output.append(super.toString().replace("\u0000", ""))
		if (unrecognisedFields.size > 0) {
			output.append("\nUnrecognized Tags:\n")
			for (next in unrecognisedFields) {
				output.append(
					"""	${next.id}:${next.content!!.replace("\u0000", "")}
"""
				)
			}
		}
		return output.toString()
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}

	fun setStartLocationInFile(startLocationInFile: Long) {
		this.startLocationInFile = startLocationInFile
	}

	fun setEndLocationInFile(endLocationInFile: Long) {
		this.endLocationInFile = endLocationInFile
	}

	val sizeOfTag: Long
		get() = if (endLocationInFile == null || startLocationInFile == null) {
			0
		} else endLocationInFile!! - startLocationInFile!! - ChunkHeader.CHUNK_HEADER_SIZE

	fun addUnRecognizedField(code: String, contents: String?) {
		unrecognisedFields.add(GenericTagTextField(code, contents))
	}

	fun getUnrecognisedFields(): List<TagTextField> {
		return unrecognisedFields
	}

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

	companion object {
		val supportedKeys: EnumSet<FieldKey> = EnumSet.of(
			FieldKey.ALBUM,
			FieldKey.ARTIST,
			FieldKey.ALBUM_ARTIST,
			FieldKey.TITLE,
			FieldKey.TRACK,
			FieldKey.GENRE,
			FieldKey.COMMENT,
			FieldKey.YEAR,
			FieldKey.RECORD_LABEL,
			FieldKey.ISRC,
			FieldKey.COMPOSER,
			FieldKey.LYRICIST,
			FieldKey.ENCODER,
			FieldKey.CONDUCTOR,
			FieldKey.RATING,
			FieldKey.COPYRIGHT
		)

	}
}

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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt16
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Represents simple text field that contains an array of number,
 *
 *
 * But reads the data content as an array of 16 bit unsigned numbers
 */
open class Mp4TagTextNumberField : Mp4TagTextField {
	/**
	 * @return the individual numbers making up this field
	 */
	//Holds the numbers decoded
	var numbers: MutableList<Short>? = null
		protected set

	/**
	 * Create a new number, already parsed in subclasses
	 *
	 * @param id
	 * @param numberArray
	 */
	constructor(id: String?, numberArray: String?) : super(id, numberArray)
	constructor(id: String?, data: ByteBuffer) : super(id, data)

	override val dataBytes: ByteArray
		get() {
			val baos = ByteArrayOutputStream()
			for (number in numbers!!) {
				try {
					baos.write(
						getSizeBEInt16(
							number
						)
					)
				} catch (e: IOException) {
					//This should never happen because we are not writing to file at this point.
					throw RuntimeException(e)
				}
			}
			return baos.toByteArray()
		}


	override fun copyContent(field: TagField?) {
		if (field is Mp4TagTextNumberField) {
			this.content = field.content
			numbers = field.numbers
		}
	}

	override val fieldType: Mp4FieldType
		get() = Mp4FieldType.IMPLICIT

	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Data actually contains a 'Data' Box so process data using this
		val header = Mp4BoxHeader(data)
		val databox = Mp4DataBox(header, data)
		dataSize = header.dataLength
		content = databox.content
		numbers = databox.getNumbers().toMutableList()
	}

	companion object {
		const val NUMBER_LENGTH = 2
	}
}

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

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

/**
 * Represents binary data
 *
 *
 * Subclassed by cover art field,
 * TODO unaware of any other binary fields at the moment
 */
open class Mp4TagBinaryField : Mp4TagField {

	var dataSize = 0
		protected set

	var data: ByteArray = byteArrayOf()
		private set

	override var isBinary = false
		protected set

	/**
	 * Construct an empty Binary Field
	 *
	 * @param id
	 */
	constructor(id: String?) : super(id)

	/**
	 * Construct new binary field with binarydata provided
	 *
	 * @param id
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	constructor(id: String?, data: ByteArray) : super(id) {
		this.data = data
	}

	/**
	 * Construct binary field from rawdata of audio file
	 *
	 * @param id
	 * @param raw
	 * @throws UnsupportedEncodingException
	 */
	constructor(id: String?, raw: ByteBuffer) : super(id, raw)


	override val fieldType: Mp4FieldType
		get() = Mp4FieldType.IMPLICIT

	override var dataBytes: ByteArray
		get() {
			return data
		}
		set(value) {
			data = value
		}

	override fun build(raw: ByteBuffer) {
		val header = Mp4BoxHeader(raw)
		dataSize = header.dataLength

		//Skip the version and length fields
		raw.position(raw.position() + Mp4DataBox.PRE_DATA_LENGTH)

		//Read the raw data into byte array
		data = ByteArray(dataSize - Mp4DataBox.PRE_DATA_LENGTH)
		for (i in data.indices) {
			data[i] = raw.get()
		}

		//After returning buffers position will be after the end of this atom
	}

	override val isEmpty: Boolean
		get() = data.size == 0

	override fun copyContent(field: TagField?) {
		if (field is Mp4TagBinaryField) {
			data = field.data
			isBinary = field.isBinary
		}
	}

	override fun toDescriptiveString(): String = "Mp4TagBinaryField@${hashCode()}"
}

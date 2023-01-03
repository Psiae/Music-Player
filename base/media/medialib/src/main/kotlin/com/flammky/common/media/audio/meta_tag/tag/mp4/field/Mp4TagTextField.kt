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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagTextField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Represents a single text field
 *
 *
 * Mp4 metadata normally held as follows:
 * <pre>
 * MP4Box Parent contains
 * :length (includes length of data child)  (4 bytes)
 * :name         (4 bytes)
 * :child with
 * :length          (4 bytes)
 * :name 'Data'     (4 bytes)
 * :atom version    (1 byte)
 * :atom type flags (3 bytes)
 * :null field      (4 bytes)
 * :data
</pre> *
 *
 *
 * Note:This class is initialized with the child data atom only, the parent data has already been processed, this may
 * change as it seems that code should probably be enscapulated into this. Whereas the raw content returned by the
 * getRawContent() contains the byte data for parent and child.
 */
open class Mp4TagTextField : Mp4TagField, TagTextField {
	protected var dataSize = 0

	override var content: String? = null

	/**
	 * Construct from File
	 *
	 * @param id   parent id
	 * @param data atom data
	 * @throws UnsupportedEncodingException
	 */
	constructor(id: String?, data: ByteBuffer) : super(id, data)

	/**
	 * Construct new Field
	 *
	 * @param id      parent id
	 * @param content data atom data
	 */
	constructor(id: String?, content: String?) : super(id) {
		this.content = content
	}

	@Throws(UnsupportedEncodingException::class)
	override fun build(data: ByteBuffer) {
		//Data actually contains a 'Data' Box so process data using this
		val header = Mp4BoxHeader(data)
		val databox = Mp4DataBox(header, data)
		dataSize = header.dataLength
		content = databox.content
	}

	override fun copyContent(field: TagField?) {
		if (field is Mp4TagTextField) {
			content = field.content
		}
	}


	override val dataBytes: ByteArray
		@Throws(UnsupportedEncodingException::class)
		get() = content
			?.toByteArray(encoding!!)
			?: byteArrayOf()

	override val fieldType: Mp4FieldType
		get() = Mp4FieldType.TEXT

	/* Not allowed */
	override var encoding: Charset?
		get() = StandardCharsets.UTF_8
		set(s) {
			/* Not allowed */
		}
	override val isBinary: Boolean
		get() = false
	override val isEmpty: Boolean
		get() = content!!.trim { it <= ' ' } == ""

	override fun toDescriptiveString(): String {
		return content!!
	}
}

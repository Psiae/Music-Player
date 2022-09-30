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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.Mp4FieldType
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * This abstract class represents a link between piece of data, and how it is stored as an mp4 atom
 *
 * Note there isnt a one to one correspondance between a tag field and a box because some fields are represented
 * by multiple boxes, for example many of the MusicBrainz fields use the '----' box, which in turn uses one of mean,
 * name and data box. So an instance of a tag field maps to one item of data such as 'Title', but it may have to read
 * multiple boxes to do this.
 *
 * There are various subclasses that represent different types of fields
 */
abstract class Mp4TagField : TagField {
	/**
	 * @return field identifier
	 */
	override var id: String? = null
		protected set

	//Just used by reverese dns class, so it knows the size of its aprent so it can detect end correctly
	protected var parentHeader: Mp4BoxHeader? = null

	protected constructor(id: String?) {
		this.id = id
	}

	/**
	 * Used by subclasses that canot identify their id until after they have been built such as ReverseDnsField
	 *
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	protected constructor(data: ByteBuffer) {
		build(data)
	}

	/**
	 * Used by reverese dns when reading from file, so can identify when there is a data atom
	 *
	 * @param parentHeader
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	protected constructor(parentHeader: Mp4BoxHeader?, data: ByteBuffer) {
		this.parentHeader = parentHeader
		build(data)
	}

	protected constructor(id: String?, data: ByteBuffer) : this(id) {
		build(data)
	}

	override fun isBinary(b: Boolean) {
		/* One cannot choose if an arbitrary block can be binary or not */
	}

	override val isCommon: Boolean
		get() = id == Mp4FieldKey.ARTIST.fieldName || id == Mp4FieldKey.ALBUM.fieldName || id == Mp4FieldKey.TITLE.fieldName || id == Mp4FieldKey.TRACK.fieldName || id == Mp4FieldKey.DAY.fieldName || id == Mp4FieldKey.COMMENT.fieldName || id == Mp4FieldKey.GENRE.fieldName

	/**
	 * @return field identifier as it will be held within the file
	 */
	protected val idBytes: ByteArray
		protected get() = id!!.toByteArray(StandardCharsets.ISO_8859_1)

	/**
	 * @return the data as it is held on file
	 * @throws UnsupportedEncodingException
	 */
	@get:Throws(UnsupportedEncodingException::class)
	protected abstract val dataBytes: ByteArray

	/**
	 * @return the field type of this field
	 */
	abstract val fieldType: Mp4FieldType

	/**
	 * Processes the data and sets the position of the data buffer to just after the end of this fields data
	 * ready for processing next field.
	 *
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	protected abstract fun build(data: ByteBuffer)//This should never happen as were not actually writing to/from a file//Create Data Box

	//Wrap in Parent box
	/**
	 * Convert back to raw content, includes parent and data atom as views as one thing externally
	 *
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@get:Throws(UnsupportedEncodingException::class)
	override val rawContent: ByteArray
		get() {
			logger.fine("Getting Raw data for:" + id)
			return try {
				//Create Data Box
				val databox = rawContentDataOnly

				//Wrap in Parent box
				val outerbaos = ByteArrayOutputStream()
				outerbaos.write(getSizeBEInt32(Mp4BoxHeader.HEADER_LENGTH + databox.size))
				outerbaos.write(id!!.toByteArray(StandardCharsets.ISO_8859_1))
				outerbaos.write(databox)
				outerbaos.toByteArray()
			} catch (ioe: IOException) {
				//This should never happen as were not actually writing to/from a file
				throw RuntimeException(ioe)
			}
		}//This should never happen as were not actually writing to/from a file//Create Data Box

	/**
	 * Get raw content for the data component only
	 *
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@get:Throws(UnsupportedEncodingException::class)
	open val rawContentDataOnly: ByteArray
		get() {
			logger.fine("Getting Raw data for:" + id)
			return try {
				//Create Data Box
				val baos = ByteArrayOutputStream()
				val data = dataBytes
				baos.write(getSizeBEInt32(Mp4DataBox.DATA_HEADER_LENGTH + data.size))
				baos.write(Mp4DataBox.IDENTIFIER.toByteArray(StandardCharsets.ISO_8859_1))
				baos.write(byteArrayOf(0))
				baos.write(byteArrayOf(0, 0, fieldType.fileClassId.toByte()))
				baos.write(byteArrayOf(0, 0, 0, 0))
				baos.write(data)
				baos.toByteArray()
			} catch (ioe: IOException) {
				//This should never happen as were not actually writing to/from a file
				throw RuntimeException(ioe)
			}
		}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.tag.mp4")
	}
}

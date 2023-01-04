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
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.binaryDataIsBmpFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.binaryDataIsGifFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.binaryDataIsJpgFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.binaryDataIsPngFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4NameBox
import java.nio.ByteBuffer

/**
 * Represents Cover Art
 *
 *
 * Note:Within this library we have a seperate TagCoverField for every image stored, however this does not map
 * very directly to how they are physically stored within a file, because all are stored under a single covr atom, so
 * a more complex conversion has to be done then for other fields when writing multiple images back to file.
 */
class Mp4TagCoverField : Mp4TagBinaryField {
	//Type
	private var imageType: Mp4FieldType? = null

	/**
	 * @return data and header size
	 */
	//Contains the size of each atom including header, required because may only have data atom or
	//may have data and name atom
	var dataAndHeaderSize = 0
		private set

	/**
	 * Empty CoverArt Field
	 */
	constructor() : super(Mp4FieldKey.ARTWORK.fieldName)

	/**
	 * Construct CoverField by reading data from audio file
	 *
	 * @param raw
	 * @param imageType
	 * @throws UnsupportedEncodingException
	 */
	constructor(raw: ByteBuffer, imageType: Mp4FieldType) : super(
		Mp4FieldKey.ARTWORK.fieldName,
	) {
		build(raw)
		this.imageType = imageType
		if (!Mp4FieldType.Companion.isCoverArtType(imageType)) {
			logger.warning(ErrorMessage.MP4_IMAGE_FORMAT_IS_NOT_TO_EXPECTED_TYPE.getMsg(imageType))
		}
	}

	/**
	 * Construct new cover art with binarydata provided
	 *
	 *
	 * Identifies the imageType by looking at the data
	 *
	 * @param data
	 * @throws UnsupportedEncodingException
	 */
	constructor(data: ByteArray) : super(Mp4FieldKey.ARTWORK.fieldName, data) {

		//Read signature
		imageType =
			if (binaryDataIsPngFormat(
					data
				)
			) {
				Mp4FieldType.COVERART_PNG
			} else if (binaryDataIsJpgFormat(
					data
				)
			) {
				Mp4FieldType.COVERART_JPEG
			} else if (binaryDataIsGifFormat(
					data
				)
			) {
				Mp4FieldType.COVERART_GIF
			} else if (binaryDataIsBmpFormat(
					data
				)
			) {
				Mp4FieldType.COVERART_BMP
			} else {
				logger.warning(
					ErrorMessage.GENERAL_UNIDENITIFED_IMAGE_FORMAT.msg
				)
				Mp4FieldType.COVERART_PNG
			}
	}

	override val fieldType: Mp4FieldType
		get() = imageType!!


	override var isBinary: Boolean
		get() = true
		set(isBinary) {
			super.isBinary = isBinary
		}

	override fun toDescriptiveString(): String {
		return imageType.toString() + ":" + dataBytes.size + "bytes"
	}

	override fun build(raw: ByteBuffer) {
		val header = Mp4BoxHeader(raw)
		dataSize = header.dataLength
		dataAndHeaderSize = header.length

		//Skip the version and length fields
		raw.position(raw.position() + Mp4DataBox.PRE_DATA_LENGTH)

		//Read the raw data into byte array
		dataBytes = ByteArray(dataSize - Mp4DataBox.PRE_DATA_LENGTH)
		raw[dataBytes, 0, dataBytes.size]

		//Is there room for another atom (remember actually passed all the data so unless Covr is last atom
		//there will be room even though more likely to be for the text top level atom)
		val positionAfterDataAtom = raw.position()
		if (raw.position() + Mp4BoxHeader.HEADER_LENGTH <= raw.limit()) {
			//Is there a following name field (not the norm)
			val nameHeader = Mp4BoxHeader(raw)
			if (nameHeader.id == Mp4NameBox.IDENTIFIER) {
				dataSize += nameHeader.dataLength
				dataAndHeaderSize += nameHeader.length
			} else {
				raw.position(positionAfterDataAtom)
			}
		}

		println("$this built, data=${data.size}")
		//After returning buffers position will be after the end of this atom
	}

	companion object {
		/**
		 *
		 * @param imageType
		 * @return the corresponding mimetype
		 */
		@JvmStatic
		fun getMimeTypeForImageType(imageType: Mp4FieldType): String? {
			return if (imageType == Mp4FieldType.COVERART_PNG) {
				ImageFormats.MIME_TYPE_PNG
			} else if (imageType == Mp4FieldType.COVERART_JPEG) {
				ImageFormats.MIME_TYPE_JPEG
			} else if (imageType == Mp4FieldType.COVERART_GIF) {
				ImageFormats.MIME_TYPE_GIF
			} else if (imageType == Mp4FieldType.COVERART_BMP) {
				ImageFormats.MIME_TYPE_BMP
			} else {
				null
			}
		}
	}
}

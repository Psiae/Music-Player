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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getIntBE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getString
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader.Companion.seekWithinLevel
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4MetaBox
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4NonStandardFieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.atom.Mp4DataBox
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.field.*
import java.io.IOException
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Reads metadata from mp4,
 *
 *
 * The metadata tags are usually held under the ilst atom as shown below
 *
 * Valid Exceptions to the rule:
 *
 * Can be no udta atom with meta rooted immediately under moov instead
 *
 * Can be no udta/meta atom at all
 *
 * <pre>
 * |--- ftyp
 * |--- moov
 * |......|
 * |......|----- mvdh
 * |......|----- trak
 * |......|----- udta
 * |..............|
 * |..............|-- meta
 * |....................|
 * |....................|-- hdlr
 * |....................|-- ilst
 * |.........................|
 * |.........................|---- @nam (Optional for each metadatafield)
 * |.........................|.......|-- data
 * |.........................|....... ecetera
 * |.........................|---- ---- (Optional for reverse dns field)
 * |.................................|-- mean
 * |.................................|-- name
 * |.................................|-- data
 * |.................................... ecetere
 * |
 * |--- mdat
</pre> *
 */
class Mp4TagReader {
	/*
	 * The metadata is stored in the box under the hierachy moov.udta.meta.ilst
	 *
	 * There are gaps between these boxes

	 */
	@Throws(CannotReadException::class, IOException::class)
	fun read(file: Path): Mp4Tag {
		return if (VersionHelper.hasOreo()) {
			Files.newByteChannel(file).use { fc ->
				read(fc)
			}
		} else {
			RandomAccessFile(file.toFile(), "r").use { read(it.channel) }
		}
	}

	fun read(sbc: SeekableByteChannel): Mp4Tag {
		val tag = Mp4Tag()
		//Get to the facts everything we are interested in is within the moov box, so just load data from file
		//once so no more file I/O needed
		val moovHeader = seekWithinLevel(
			sbc,
			Mp4AtomIdentifier.MOOV.fieldName
		)
			?: throw CannotReadException(
				ErrorMessage.MP4_FILE_NOT_CONTAINER.msg
			)
		val moovBuffer =
			ByteBuffer.allocate(moovHeader.length - Mp4BoxHeader.HEADER_LENGTH)
		sbc.read(moovBuffer)
		moovBuffer.rewind()

		//Level 2-Searching for "udta" within "moov"
		var boxHeader = seekWithinLevel(moovBuffer, Mp4AtomIdentifier.UDTA.fieldName)
		if (boxHeader != null) {
			//Level 3-Searching for "meta" within udta
			boxHeader = seekWithinLevel(moovBuffer, Mp4AtomIdentifier.META.fieldName)
			if (boxHeader == null) {
				logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.msg)
				return tag
			}
			val meta = Mp4MetaBox(boxHeader, moovBuffer)
			meta.processData()

			//Level 4- Search for "ilst" within meta
			boxHeader = seekWithinLevel(moovBuffer, Mp4AtomIdentifier.ILST.fieldName)
			//This file does not actually contain a tag
			if (boxHeader == null) {
				logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.msg)
				return tag
			}
		} else {
			//Level 2-Searching for "meta" not within udta
			boxHeader = seekWithinLevel(moovBuffer, Mp4AtomIdentifier.META.fieldName)
			if (boxHeader == null) {
				logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.msg)
				return tag
			}
			val meta = Mp4MetaBox(boxHeader, moovBuffer)
			meta.processData()


			//Level 3- Search for "ilst" within meta
			boxHeader = seekWithinLevel(moovBuffer, Mp4AtomIdentifier.ILST.fieldName)
			//This file does not actually contain a tag
			if (boxHeader == null) {
				logger.warning(ErrorMessage.MP4_FILE_HAS_NO_METADATA.msg)
				return tag
			}
		}

		//Size of metadata (exclude the size of the ilst parentHeader), take a slice starting at
		//metadata children to make things safer
		val length = boxHeader.length - Mp4BoxHeader.HEADER_LENGTH
		val metadataBuffer = moovBuffer.slice()
		//Datalength is longer are there boxes after ilst at this level?
		logger.config("headerlengthsays:" + length + "datalength:" + metadataBuffer.limit())
		var read = 0
		logger.config("Started to read metadata fields at position is in metadata buffer:" + metadataBuffer.position())
		while (read < length) {
			//Read the boxHeader
			boxHeader.update(metadataBuffer)

			//Create the corresponding datafield from the id, and slice the buffer so position of main buffer
			//wont get affected
			logger.config("Next position is at:" + metadataBuffer.position())
			createMp4Field(tag, boxHeader, metadataBuffer.slice())

			//Move position in buffer to the start of the next parentHeader
			metadataBuffer.position(metadataBuffer.position() + boxHeader.dataLength)
			read += boxHeader.length
		}
		return tag
	}

	/**
	 * Process the field and add to the tag
	 *
	 * Note:In the case of coverart MP4 holds all the coverart within individual dataitems all within
	 * a single covr atom, we will add separate mp4field for each image.
	 *
	 * @param tag
	 * @param header
	 * @param raw
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	private fun createMp4Field(tag: Mp4Tag, header: Mp4BoxHeader, raw: ByteBuffer) {
		//Header with no data #JAUDIOTAGGER-463
		if (header.dataLength == 0) {
			//Just Ignore
		} else if (header.id == Mp4TagReverseDnsField.IDENTIFIER) {
			//
			try {
				val field: TagField = Mp4TagReverseDnsField(header, raw)
				tag.addField(field)
			} catch (e: Exception) {
				logger.warning(ErrorMessage.MP4_UNABLE_READ_REVERSE_DNS_FIELD.getMsg(e.message))
				val field: TagField = Mp4TagRawBinaryField(header, raw)
				tag.addField(field)
			}
		} else {
			val currentPos = raw.position()
			val isDataIdentifier = getString(
				raw,
				Mp4BoxHeader.IDENTIFIER_POS,
				Mp4BoxHeader.IDENTIFIER_LENGTH,
				StandardCharsets.ISO_8859_1
			) == Mp4DataBox.IDENTIFIER
			raw.position(currentPos)
			if (isDataIdentifier) {
				//Need this to decide what type of Field to create
				var type = getIntBE(
					raw,
					Mp4DataBox.TYPE_POS_INCLUDING_HEADER,
					Mp4DataBox.TYPE_POS_INCLUDING_HEADER + Mp4DataBox.TYPE_LENGTH - 1
				)
				var fieldType = Mp4FieldType.getFieldType(type)
				logger.config("Box Type id:" + header.id + ":type:" + fieldType)

				//Special handling for some specific identifiers otherwise just base on class id
				if (header.id == Mp4FieldKey.TRACK.fieldName) {
					val field: TagField = Mp4TrackField(header.id, raw)
					tag.addField(field)
				} else if (header.id == Mp4FieldKey.DISCNUMBER.fieldName) {
					val field: TagField = Mp4DiscNoField(header.id, raw)
					tag.addField(field)
				} else if (header.id == Mp4FieldKey.GENRE.fieldName) {
					val field: TagField = Mp4GenreField(header.id, raw)
					tag.addField(field)
				} else if (header.id == Mp4FieldKey.ARTWORK.fieldName || Mp4FieldType.isCoverArtType(fieldType!!)) {
					var processedDataSize = 0
					var imageCount = 0
					//The loop should run for each image (each data atom)
					while (processedDataSize < header.dataLength) {
						//There maybe a mixture of PNG and JPEG images so have to check type
						//for each subimage (if there are more than one image)
						if (imageCount > 0) {
							type = getIntBE(
								raw, processedDataSize + Mp4DataBox.TYPE_POS_INCLUDING_HEADER,
								processedDataSize + Mp4DataBox.TYPE_POS_INCLUDING_HEADER + Mp4DataBox.TYPE_LENGTH - 1
							)
							fieldType = Mp4FieldType.getFieldType(type)
						}
						val field = Mp4TagCoverField(raw, fieldType!!)
						tag.addField(field)
						processedDataSize += field.dataAndHeaderSize
						imageCount++
					}
				} else if (fieldType == Mp4FieldType.TEXT) {
					val field: TagField = Mp4TagTextField(header.id, raw)
					tag.addField(field)
				} else if (fieldType == Mp4FieldType.IMPLICIT) {
					val field: TagField = Mp4TagTextNumberField(header.id, raw)
					tag.addField(field)
				} else if (fieldType == Mp4FieldType.INTEGER) {
					val field: TagField = Mp4TagByteField(header.id, raw)
					tag.addField(field)
				} else {
					var existingId = false
					for (key in Mp4FieldKey.values()) {
						if (key.fieldName == header.id) {
							//The parentHeader is a known id but its field type is not one of the expected types so
							//this field is invalid. i.e I received a file with the TMPO set to 15 (Oxf) when it should
							//be 21 (ox15) so looks like somebody got their decimal and hex numbering confused
							//So in this case best to ignore this field and just write a warning
							existingId = true
							logger.warning("Known Field:" + header.id + " with invalid field type of:" + type + " is ignored")
							break
						}
					}

					//Unknown field id with unknown type so just create as binary
					if (!existingId) {
						logger.warning("UnKnown Field:" + header.id + " with invalid field type of:" + type + " created as binary")
						val field: TagField = Mp4TagBinaryField(header.id, raw)
						tag.addField(field)
					}
				}
			} else {
				//MediaMonkey 3 CoverArt Attributes field, does not have data items so just
				//copy parent and child as is without modification
				if (header.id == Mp4NonStandardFieldKey.AAPR.fieldName) {
					val field: TagField = Mp4TagRawBinaryField(header, raw)
					tag.addField(field)
				} else {
					val field: TagField = Mp4TagRawBinaryField(header, raw)
					tag.addField(field)
				}
			}
		}
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.tag.mp4")
	}
}

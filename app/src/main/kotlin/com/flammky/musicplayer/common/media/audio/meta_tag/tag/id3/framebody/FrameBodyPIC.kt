/**
 * @author : Paul Taylor
 * @author : Eric Farng
 *
 * Version @version:$Id$
 *
 * MusicTag Copyright (C)2003,2004
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.getFormatForMimeType
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * ID3v22 Attached Picture
 *
 *
 *  This frame contains a picture directly related to the audio file.
 * Image format is preferably "PNG" [PNG] or "JPG" [JFIF]. Description
 * is a short description of the picture, represented as a terminated
 * textstring. The description has a maximum length of 64 characters,
 * but may be empty. There may be several pictures attached to one file,
 * each in their individual "PIC" frame, but only one with the same
 * ontent descriptor. There may only be one picture with the picture
 * type declared as picture type $01 and $02 respectively. There is a
 * possibility to put only a link to the image file by using the image
 * format' "--" and having a complete URL [URL] instead of picture data.
 * The use of linked files should however be used restrictively since
 * there is the risk of separation of files.
 *
 * Attached picture   "PIC"
 * Frame size         $xx xx xx
 * Text encoding      $xx
 * Image format       $xx xx xx
 * Picture type       $xx
 * Description        textstring $00 (00)
 * Picture data       binary data>
 *
 *
 * Picture type:  $00  Other
 * $01  32x32 pixels 'file icon' (PNG only)
 * $02  Other file icon
 * $03  Cover (front)
 * $04  Cover (back)
 * $05  Leaflet page
 * $06  Media (e.g. lable side of CD)
 * $07  Lead artist/lead performer/soloist
 * $08  Artist/performer
 * $09  Conductor
 * $0A  Band/Orchestra
 * $0B  Composer
 * $0C  Lyricist/text writer
 * $0D  Recording Location
 * $0E  During recording
 * $0F  During performance
 * $10  Movie/video screen capture
 * $11  A bright coloured fish
 * $12  Illustration
 * $13  Band/artist logotype
 * $14  Publisher/Studio logotype
 */
class FrameBodyPIC : AbstractID3v2FrameBody, ID3v22FrameBody {

	override val identifier: String?
		get() = ID3v22Frames.FRAME_ID_V2_ATTACHED_PICTURE

	/**
	 * Creates a new FrameBodyPIC datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
	}

	constructor(body: FrameBodyPIC) : super(body)

	/**
	 * Creates a new FrameBodyPIC datatype.
	 *
	 * @param textEncoding
	 * @param imageFormat
	 * @param pictureType
	 * @param description
	 * @param data
	 */
	constructor(
		textEncoding: Byte,
		imageFormat: String?,
		pictureType: Byte,
		description: String?,
		data: ByteArray?
	) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, imageFormat)
		setPictureType(pictureType)
		this.description = description
		imageData = data
	}

	/**
	 * Conversion from v2 PIC to v3/v4 APIC
	 * @param body
	 */
	constructor(body: FrameBodyAPIC) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, body.textEncoding)
		setObjectValue(
			DataTypes.OBJ_IMAGE_FORMAT, getFormatForMimeType(
				(body.getObjectValue(DataTypes.OBJ_MIME_TYPE) as String)
			)
		)
		setObjectValue(DataTypes.OBJ_PICTURE_DATA, body.getObjectValue(DataTypes.OBJ_PICTURE_DATA))
		description = body.description
		imageData = body.imageData
	}

	/**
	 * Creates a new FrameBodyPIC datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * Get a description of the image
	 *
	 * @return a description of the image
	 */
	/**
	 * Set a description of the image
	 *
	 * @param description of the image
	 */
	var description: String?
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as String
		set(description) {
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}
	/**
	 * Get Image data
	 *
	 * @return
	 */
	/**
	 * Set imageData
	 *
	 * @param imageData
	 */
	var imageData: ByteArray?
		get() = getObjectValue(DataTypes.OBJ_PICTURE_DATA) as ByteArray
		set(imageData) {
			setObjectValue(DataTypes.OBJ_PICTURE_DATA, imageData)
		}

	/**
	 * Set Picture Type
	 *
	 * @param pictureType
	 */
	fun setPictureType(pictureType: Byte) {
		setObjectValue(DataTypes.OBJ_PICTURE_TYPE, pictureType)
	}

	/**
	 * @return picturetype
	 */
	val pictureType: Int
		get() = (getObjectValue(DataTypes.OBJ_PICTURE_TYPE) as? Long)?.toInt() ?: -1

	/**
	 * If the description cannot be encoded using current encoder, change the encoder
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = TextEncoding.UTF_16
		}
		super.write(tagBuffer)
	}

	/**
	 * Get a description of the image
	 *
	 * @return a description of the image
	 */
	val formatType: String?
		get() = getObjectValue(DataTypes.OBJ_IMAGE_FORMAT) as String
	val isImageUrl: Boolean
		get() = formatType != null && formatType == IMAGE_IS_URL

	/**
	 * Get mimetype
	 *
	 * @return a description of the image
	 */
	val mimeType: String
		get() = getObjectValue(DataTypes.OBJ_MIME_TYPE) as String

	/**
	 * @return the image url if there is otherwise return an empty String
	 */
	fun getImageUrl(): String {
		return if (isImageUrl) {
			String(
				(getObjectValue(DataTypes.OBJ_PICTURE_DATA) as ByteArray),
				0,
				(getObjectValue(DataTypes.OBJ_PICTURE_DATA) as ByteArray).size,
				StandardCharsets.ISO_8859_1
			)
		} else {
			""
		}
	}

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(StringFixedLength(DataTypes.OBJ_IMAGE_FORMAT, this, 3))
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_PICTURE_TYPE,
				this,
				PictureTypes.PICTURE_TYPE_FIELD_SIZE
			)
		)
		objectList.add(StringNullTerminated(DataTypes.OBJ_DESCRIPTION, this))
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_PICTURE_DATA, this))
	}

	companion object {
		const val IMAGE_IS_URL = "-->"
	}
}

/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.getMimeTypeForFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Attached picture frame.
 *
 *
 * This frame contains a picture directly related to the audio file.
 * Image format is the MIME type and subtype for the image. In
 * the event that the MIME media type name is omitted, "image/" will be
 * implied. The "image/png" or "image/jpeg" picture format
 * should be used when interoperability is wanted. Description is a
 * short description of the picture, represented as a terminated
 * textstring. The description has a maximum length of 64 characters,
 * but may be empty. There may be several pictures attached to one file,
 * each in their individual "APIC" frame, but only one with the same
 * content descriptor. There may only be one picture with the picture
 * type declared as picture type $01 and $02 respectively. There is the
 * possibility to put only a link to the image file by using the 'MIME
 * type' "-->" and having a complete URL instead of picture data.
 * The use of linked files should however be used sparingly since there
 * is the risk of separation of files.
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2> &lt;Header for 'Attached picture', ID: "APIC"&gt;</td></tr>
 * <tr><td>Text encoding  </td><td>$xx                            </td></tr>
 * <tr><td>MIME type      </td><td>&lt;text string&gt; $00        </td></tr>
 * <tr><td>Picture type   </td><td>$xx                            </td></tr>
 * <tr><td>Description    </td><td>&lt;text string according to encoding&gt; $00 (00)</td></tr>
 * <tr><td>Picture data   </td><td>&lt;binary data&gt;            </td></tr>
</table> *
 *
 * <table border=0 width="70%">
 * <tr><td rowspan=21 valign=top>Picture type:</td>
 * <td>$00 </td><td>Other                                </td></tr>
 * <tr><td>$01 </td><td>32x32 pixels 'file icon' (PNG only)  </td></tr>
 * <tr><td>$02 </td><td>Other file icon                      </td></tr>
 * <tr><td>$03 </td><td>Cover (front)                        </td></tr>
 * <tr><td>$04 </td><td>Cover (back)                         </td></tr>
 * <tr><td>$05 </td><td>Leaflet page                         </td></tr>
 * <tr><td>$06 </td><td>Media (e.g. lable side of CD)        </td></tr>
 * <tr><td>$07 </td><td>Lead artist/lead performer/soloist   </td></tr>
 * <tr><td>$08 </td><td>Artist/performer                     </td></tr>
 * <tr><td>$09 </td><td>Conductor                            </td></tr>
 * <tr><td>$0A </td><td>Band/Orchestra                       </td></tr>
 * <tr><td>$0B </td><td>Composer                             </td></tr>
 * <tr><td>$0C </td><td>Lyricist/text writer                 </td></tr>
 * <tr><td>$0D </td><td>Recording Location                   </td></tr>
 * <tr><td>$0E </td><td>During recording                     </td></tr>
 * <tr><td>$0F </td><td>During performance                   </td></tr>
 * <tr><td>$10 </td><td>Movie/video screen capture           </td></tr>
 * <tr><td>$11 </td><td>A bright coloured fish               </td></tr>
 * <tr><td>$12 </td><td>Illustration                         </td></tr>
 * <tr><td>$13 </td><td>Band/artist logotype                 </td></tr>
 * <tr><td>$14 </td><td>Publisher/Studio logotype            </td></tr>
</table> *
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */


/** [org.jaudiotagger.tag.id3.framebody.FrameBodyCTOC] */
class FrameBodyAPIC : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_ATTACHED_PICTURE

	/**
	 * Creates a new FrameBodyAPIC datatype.
	 */
	constructor() {
		//Initilise default text encoding
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
	}

	constructor(body: FrameBodyAPIC) : super(body)

	/**
	 * Conversion from v2 PIC to v3/v4 APIC
	 * @param body
	 */
	constructor(body: FrameBodyPIC) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, body.textEncoding)
		setObjectValue(
			DataTypes.OBJ_MIME_TYPE, getMimeTypeForFormat(
				(body.getObjectValue(DataTypes.OBJ_IMAGE_FORMAT) as String)
			)
		)
		setObjectValue(DataTypes.OBJ_PICTURE_TYPE, body.getObjectValue(DataTypes.OBJ_PICTURE_TYPE))
		setObjectValue(DataTypes.OBJ_DESCRIPTION, body.description)
		setObjectValue(DataTypes.OBJ_PICTURE_DATA, body.getObjectValue(DataTypes.OBJ_PICTURE_DATA))
	}

	/**
	 * Creates a new FrameBodyAPIC datatype.
	 *
	 * @param textEncoding
	 * @param mimeType
	 * @param pictureType
	 * @param description
	 * @param data
	 */
	constructor(
		textEncoding: Byte,
		mimeType: String,
		pictureType: Byte,
		description: String,
		data: ByteArray?
	) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		this.mimeType = mimeType
		setPictureType(pictureType)
		this.description = description
		imageData = data
	}

	/**
	 * Creates a new FrameBodyAPIC datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	override val userFriendlyValue: String
		get() = if (imageData != null) {
			mimeType + ":" + description + ":" + imageData!!.size
		} else {
			mimeType + ":" + description + ":0"
		}
	/**
	 * Get a description of the image
	 *
	 * @return a description of the image
	 */
	/**
	 * Set a description of the image
	 *
	 * @param description
	 */
	var description: String
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as? String ?: ""
		set(description) {
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}

	/** The mime Type [null] may be null */
	var mimeType: String
		get() = getObjectValue(DataTypes.OBJ_MIME_TYPE) as? String ?: ""
		set(mimeType) {
			setObjectValue(DataTypes.OBJ_MIME_TYPE, mimeType)
		}

	/** The imageData as [ByteArray], may be null */
	var imageData: ByteArray?
		get() = getObjectValue(DataTypes.OBJ_PICTURE_DATA) as? ByteArray
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
	 * $-1	-1	 Invalid / Not Specified
	 *
	 * $00  0    Other
	 *
	 * $01  1    32x32 pixels 'file icon' (PNG only)
	 *
	 * $02  2    Other file icon
	 *
	 * $03  3    Cover (front)
	 *
	 * $04  4    Cover (back)
	 *
	 * $05  5    Leaflet page
	 *
	 * $06  6    Media (e.g. lable side of CD)
	 *
	 * $07  7    Lead artist/lead performer/soloist
	 *
	 * $08  8    Artist/performer
	 *
	 * $09  9    Conductor
	 *
	 * $0A  10   Band/Orchestra
	 *
	 * $0B  11   Composer
	 *
	 * $0C  12   Lyricist/text writer
	 *
	 * $0D  13   Recording Location
	 *
	 * $0E  14   During recording
	 *
	 * $0F  15   During performance
	 *
	 * $10  16   Movie/video screen capture
	 *
	 * $11  17   A bright coloured fish
	 *
	 * $12  18   Illustration
	 *
	 * $13  19   Band/artist logotype
	 *
	 * $14  20   Publisher/Studio logotype
	 *
	 */

	val pictureType: Int
		get() = (getObjectValue(DataTypes.OBJ_PICTURE_TYPE) as? Long)?.toInt() ?: -1

	/**
	 * If the description cannot be encoded using current encoder, change the encoder
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		if (TagOptionSingleton.instance.isAPICDescriptionITunesCompatible) {
			textEncoding = TextEncoding.ISO_8859_1
			if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
				description = ""
			}
		} else {
			if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
				textEncoding = TextEncoding.UTF_16
			}
		}
		super.write(tagBuffer)
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
		objectList.add(StringNullTerminated(DataTypes.OBJ_MIME_TYPE, this))
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_PICTURE_TYPE,
				this,
				PictureTypes.PICTURE_TYPE_FIELD_SIZE
			)
		)
		objectList.add(TextEncodedStringNullTerminated(DataTypes.OBJ_DESCRIPTION, this))
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_PICTURE_DATA, this))
	}

	/**
	 * Whether the Image Data  is held as a url rather than actually being Image Data
	 */
	val isImageUrl: Boolean
		get() = mimeType == IMAGE_IS_URL


	/**
	 * The Image Url if there is any, otherwise empty String ```""```
	 */
	val imageUrl: String
		get() {
			if (!isImageUrl) return ""
			val byteArray = getObjectValue(DataTypes.OBJ_PICTURE_DATA) as? ByteArray ?: return ""
			return String(byteArray, 0, byteArray.size, StandardCharsets.ISO_8859_1)
		}

	companion object {
		const val IMAGE_IS_URL = "-->"
	}
}

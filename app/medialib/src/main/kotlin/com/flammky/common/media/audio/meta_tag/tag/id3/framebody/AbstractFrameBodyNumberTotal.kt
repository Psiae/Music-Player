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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberHashMap
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.PartOfSet
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer

/**
 * Track number/position in set Text Information frame.
 *
 *
 * The 'Track number/Position in set' frame is a numeric string containing the order number of the audio-file on its original recording.
 *
 * This may be extended with a "/" character and a numeric string containing the total number of tracks/elements on the original recording.
 * e.g. "4/9".
 *
 * Some applications like to prepend the track number with a zero to aid sorting, (i.e 02 comes before 10)
 *
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
abstract class AbstractFrameBodyNumberTotal : AbstractID3v2FrameBody {
	/**
	 * Creates a new FrameBodyTRCK datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, PartOfSet.PartOfSetValue())
	}

	constructor(body: AbstractFrameBodyNumberTotal) : super(body)

	/**
	 * Creates a new FrameBodyTRCK datatype, the value is parsed literally
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_TEXT, PartOfSet.PartOfSetValue(text))
	}

	constructor(textEncoding: Byte, trackNo: Int?, trackTotal: Int?) : super() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_TEXT, PartOfSet.PartOfSetValue(trackNo, trackTotal))
	}

	override val userFriendlyValue: String
		get() {
			val value = getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue
			return value.count.toString()
		}

	/**
	 * Creates a new FrameBodyTRCK datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws java.io.IOException
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 * The ID3v2 frame identifier
	 *
	 * @return the ID3v2 frame identifier  for this frame type
	 */
	abstract override val identifier: String?
	open var text: String?
		get() = getObjectValue(DataTypes.OBJ_TEXT).toString()
		set(text) {
			setObjectValue(DataTypes.OBJ_TEXT, PartOfSet.PartOfSetValue(text))
		}
	var number: Int?
		get() {
			val value = getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue
			return value.count
		}
		set(trackNo) {
			(getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).count = trackNo
		}

	val trackNoAsText: String?
		get() = (getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).countAsText

	fun setNumber(trackNo: String) {
		(getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).setCount(trackNo)
	}

	var total: Int?
		get() = (getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).total
		set(trackTotal) {
			(getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).total =
				trackTotal
		}

	val trackTotalAsText: String?
		get() = (getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).totalAsText


	fun setTotal(trackTotal: String) {
		(getObjectValue(DataTypes.OBJ_TEXT) as PartOfSet.PartOfSetValue).setTotal(trackTotal)
	}

	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(PartOfSet(DataTypes.OBJ_TEXT, this))
	}
}

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
 * People List
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberHashMap
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.PairedTextEncodedStringNullTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * Used by frames that take a pair of values such as TIPL, IPLS and TMCL
 *
 */
abstract class AbstractFrameBodyPairs : AbstractID3v2FrameBody, ID3v24FrameBody {
	/**
	 * Creates a new AbstractFrameBodyPairs datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
	}

	/**
	 * Creates a new AbstractFrameBodyPairs data type.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		this.text = text
	}

	/**
	 * Creates a new AbstractFrameBodyPairs data type.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 * The ID3v2 frame identifier
	 *
	 * @return the ID3v2 frame identifier  for this frame type
	 */
	abstract override val identifier: String?

	/**
	 * Parse text as a null separated pairing of function and name
	 *
	 * @param text
	 */
	fun addPair(text: String?) {
		val stz = StringTokenizer(text, "\u0000")
		if (stz.countTokens() == 2) {
			addPair(stz.nextToken(), stz.nextToken())
		} else {
			addPair("", text)
		}
	}

	/**
	 * Add pair
	 *
	 * @param function
	 * @param name
	 */
	fun addPair(function: String?, name: String?) {
		val value =
			(getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?)?.getValue()
		value?.add(function, name)
	}

	/**
	 * Remove all Pairs
	 */
	fun resetPairs() {
		val value =
			(getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?)?.getValue()
		value?.getMapping()?.clear()
	}

	/**
	 * Because have a text encoding we need to check the data values do not contain characters that cannot be encoded in
	 * current encoding before we write data. If they do change the encoding.
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		if (!(getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?)!!.canBeEncoded()) {
			textEncoding = TextEncoding.UTF_16
		}
		super.write(tagBuffer)
	}

	/**
	 * Consists of a text encoding , and then a series of null terminated Strings, there should be an even number
	 * of Strings as they are paired as involvement/involvee
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(PairedTextEncodedStringNullTerminated(DataTypes.OBJ_TEXT, this))
	}

	val pairing: PairedTextEncodedStringNullTerminated.ValuePairs
		get() = getObject(DataTypes.OBJ_TEXT)!!.value as PairedTextEncodedStringNullTerminated.ValuePairs

	/**
	 * Get key at index
	 *
	 * @param index
	 * @return value at index
	 */
	fun getKeyAtIndex(index: Int): String? {
		val text = getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?
		return text?.getValue()?.getMapping()?.get(index)?.key
	}

	/**
	 * Get value at index
	 *
	 * @param index
	 * @return value at index
	 */
	fun getValueAtIndex(index: Int): String? {
		val text = getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?
		return text?.getValue()?.getMapping()?.get(index)?.value
	}

	/**
	 * @return number of text pairs
	 */
	val numberOfPairs: Int
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?
			return text?.getValue()?.getNumberOfPairs() ?: 0
		}

	/**
	 * Set the text, decoded as pairs of involvee - involvement
	 *
	 * @param text
	 */
	var text: String?
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as PairedTextEncodedStringNullTerminated?
			val sb = StringBuilder()
			var count = 1
			for (entry in text?.getValue()?.getMapping() ?: return null) {
				sb.append(entry.key + '\u0000' + entry.value)
				if (count != numberOfPairs) {
					sb.append('\u0000')
				}
				count++
			}
			return sb.toString()
		}
		set(text) {
			val value = PairedTextEncodedStringNullTerminated.ValuePairs()
			val stz = StringTokenizer(text, "\u0000")
			while (stz.hasMoreTokens()) {
				val key = stz.nextToken()
				if (stz.hasMoreTokens()) {
					value.add(key, stz.nextToken())
				}
			}
			setObjectValue(DataTypes.OBJ_TEXT, value)
		}
	override val userFriendlyValue: String
		get() = text!!
}

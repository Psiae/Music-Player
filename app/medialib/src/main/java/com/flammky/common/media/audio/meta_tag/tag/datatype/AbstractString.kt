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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.StandardCharsets

/**
 * A partial implementation for String based ID3 fields
 */
abstract class AbstractString : AbstractDataType {

	override var value: Any? = super.value

	override var size: Int = 0

	override fun readByteArray(arr: ByteArray?, offset: Int) {
		TODO("Not yet implemented")
	}

	override fun writeByteArray(): ByteArray? {
		TODO("Not yet implemented")
	}

	/**
	 * Creates a new  datatype
	 *
	 * @param identifier
	 * @param frameBody
	 */
	protected constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	/**
	 * Creates a new  datatype, with value
	 *
	 * @param identifier
	 * @param frameBody
	 * @param value
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?, value: String?) : super(
		identifier,
		frameBody,
		value
	)

	/**
	 * Copy constructor
	 *
	 * @param object
	 */
	protected constructor(`object`: AbstractString?) : super(`object`)

	/**
	 * Return String representation of data type
	 *
	 * @return a string representation of the value
	 */
	override fun toString(): String {
		return value as String
	}

	/**
	 * Check the value can be encoded with the specified encoding
	 * @return
	 */
	fun canBeEncoded(): Boolean {
		//Try and write to buffer using the CharSet defined by the textEncoding field (note if using UTF16 we dont
		//need to worry about LE,BE at this point it makes no difference)
		val textEncoding = this.body?.textEncoding ?: return false
		val encoding = TextEncoding.instanceOf
		val charset = encoding.getCharsetForId(textEncoding.toInt()) ?: return false
		val encoder = charset.newEncoder()
		return if (encoder.canEncode(value as String)) {
			true
		} else {
			logger.finest(
				"Failed Trying to decode" + value + "with" + encoder.toString()
			)
			false
		}
	}

	/**
	 * If they have specified UTF-16 then decoder works out by looking at BOM
	 * but if missing we have to make an educated guess otherwise just use
	 * specified decoder
	 *
	 * @param inBuffer
	 * @return
	 */
	protected fun getCorrectDecoder(inBuffer: ByteBuffer): CharsetDecoder? {
		var decoder: CharsetDecoder? = null
		if (inBuffer.remaining() <= 2) {
			decoder = textEncodingCharSet!!.newDecoder()
			decoder.reset()
			return decoder
		}
		if (textEncodingCharSet === StandardCharsets.UTF_16) {
			if (inBuffer.getChar(0).code == 0xfffe || inBuffer.getChar(0).code == 0xfeff) {
				//Get the Specified Decoder
				decoder = textEncodingCharSet!!.newDecoder()
				decoder.reset()
			} else {
				if (inBuffer[0].toInt() == 0) {
					decoder = StandardCharsets.UTF_16BE.newDecoder()
					decoder.reset()
				} else {
					decoder = StandardCharsets.UTF_16LE.newDecoder()
					decoder.reset()
				}
			}
		} else {
			decoder = textEncodingCharSet!!.newDecoder()
			decoder?.reset()
		}
		return decoder
	}

	/**
	 * Get the text encoding being used.
	 *
	 * The text encoding is defined by the frame body that the text field belongs to.
	 *
	 * @return the text encoding charset
	 */
	protected open val textEncodingCharSet: Charset?
		get() {
			val textEncoding = this.body?.textEncoding ?: return null
			return TextEncoding.instanceOf.getCharsetForId(textEncoding.toInt())
		}
}

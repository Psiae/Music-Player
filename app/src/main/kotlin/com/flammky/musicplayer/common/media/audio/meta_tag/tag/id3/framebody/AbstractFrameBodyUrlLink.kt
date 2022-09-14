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
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.StringSizeTerminated
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Abstract super class of all URL Frames
 */
abstract class AbstractFrameBodyUrlLink : AbstractID3v2FrameBody {
	/**
	 * Creates a new FrameBodyUrlLink datatype.
	 */
	protected constructor() : super()

	/**
	 * Copy Constructor
	 * @param body
	 */
	protected constructor(body: AbstractFrameBodyUrlLink) : super(body)

	/**
	 * Creates a new FrameBodyUrlLink datatype., set up with data.
	 *
	 * @param urlLink
	 */
	constructor(urlLink: String?) {
		setObjectValue(DataTypes.OBJ_URLLINK, urlLink)
	}

	/**
	 * Creates a new FrameBodyUrlLink datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	protected constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	override val userFriendlyValue: String
		get() = urlLink!!
	/**
	 * Get URL Link
	 *
	 * @return the urllink
	 */
	/**
	 * Set URL Link
	 *
	 * @param urlLink
	 */
	var urlLink: String?
		get() = getObjectValue(DataTypes.OBJ_URLLINK) as String
		set(urlLink) {
			requireNotNull(urlLink) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			setObjectValue(DataTypes.OBJ_URLLINK, urlLink)
		}

	/**
	 * If the description cannot be encoded using the current encoding change the encoder
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		val encoder = StandardCharsets.ISO_8859_1.newEncoder()
		val origUrl = urlLink!!
		if (!encoder.canEncode(origUrl)) {
			//ALL W Frames only support ISO-8859-1 for the url itself, if unable to encode let us assume
			//the link just needs url encoding
			urlLink = encodeURL(origUrl)

			//We still cant convert so just set log error and set to blank to allow save to continue
			if (!encoder.canEncode(urlLink)) {
				logger.warning(ErrorMessage.MP3_UNABLE_TO_ENCODE_URL.getMsg(origUrl))
				urlLink = ""
			} else {
				logger.warning(ErrorMessage.MP3_URL_SAVED_ENCODED.getMsg(origUrl, urlLink))
			}
		}
		super.write(tagBuffer)
	}

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(StringSizeTerminated(DataTypes.OBJ_URLLINK, this))
	}

	/**
	 * Encode url because may receive url already encoded or not, but we can only store as ISO8859-1
	 *
	 * @param url
	 * @return
	 */
	private fun encodeURL(url: String): String {
		return try {
			val splitURL = url.split("(?<!/)/(?!/)".toRegex()).toTypedArray()
			val sb = StringBuffer(splitURL[0])
			for (i in 1 until splitURL.size) {
				sb.append("/").append(URLEncoder.encode(splitURL[i], "utf-8"))
			}
			sb.toString()
		} catch (uee: UnsupportedEncodingException) {
			//Should never happen as utf-8 is always availablebut in case it does we just return the utl
			//unmodified
			logger.warning("Uable to url encode because utf-8 charset not available:" + uee.message)
			url
		}
	}
}

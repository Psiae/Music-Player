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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer

/**
 * Original release year Text information frame.
 *
 * The 'Original release year' frame is intended for the year when the original recording, if for example the music
 * in the file should be a cover of a previously released song, was released. The field is formatted as in the "TYER"
 * frame.
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
class FrameBodyTORY : AbstractFrameBodyTextInfo, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v23Frames.FRAME_ID_V3_TORY

	/**
	 * Creates a new FrameBodyTORY datatype.
	 */
	constructor()
	constructor(body: FrameBodyTORY) : super(body)

	/**
	 * Creates a new FrameBodyTORY datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * When converting v4 TDOR to v3 TORY frame
	 * @param body
	 */
	constructor(body: FrameBodyTDOR) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		var year = body.text
		if (body.text!!.length > NUMBER_OF_DIGITS_IN_YEAR) {
			year = body.text!!.substring(0, NUMBER_OF_DIGITS_IN_YEAR)
		}
		setObjectValue(DataTypes.OBJ_TEXT, year)
	}

	/**
	 * Creates a new FrameBodyTORY datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	companion object {
		private const val NUMBER_OF_DIGITS_IN_YEAR = 4
	}
}

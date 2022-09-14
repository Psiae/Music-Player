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
 * Year Text information frame.
 *
 * The 'Year' frame is a numeric string with a year of the recording. This frames is always four characters long (until the year 10000).
 *
 * Deprecated in v2.4.0
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
class FrameBodyTYER : AbstractFrameBodyTextInfo, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v23Frames.FRAME_ID_V3_TYER

	/**
	 * Creates a new FrameBodyTYER datatype.
	 */
	constructor()
	constructor(body: FrameBodyTYER) : super(body)

	/**
	 * When converting v4 TDRC frame to v3 TYER
	 * @param body
	 */
	constructor(body: FrameBodyTDRC) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, body.text)
	}

	/**
	 * Creates a new FrameBodyTYER datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTYER datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
}

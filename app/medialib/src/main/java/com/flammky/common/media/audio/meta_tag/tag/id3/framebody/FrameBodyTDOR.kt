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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer

/**
 *
 * The 'Original release time' frame contains a timestamp describing
 * when the original recording of the audio was released. Timestamp
 * format is described in the ID3v2 structure document.
 */
class FrameBodyTDOR : AbstractFrameBodyTextInfo, ID3v24FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_ORIGINAL_RELEASE_TIME

	/**
	 * Creates a new FrameBodyTDOR datatype.
	 */
	constructor()
	constructor(body: FrameBodyTDOR) : super(body)

	/**
	 * When converting v3 TDAT to v4 TDRC frame
	 * @param body
	 */
	constructor(body: FrameBodyTORY) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, body.text)
	}

	/**
	 * Creates a new FrameBodyTDOR datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTDOR datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
}

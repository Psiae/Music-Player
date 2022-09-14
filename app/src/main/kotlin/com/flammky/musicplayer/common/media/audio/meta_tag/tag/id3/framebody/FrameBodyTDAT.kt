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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames
import java.nio.ByteBuffer

/**
 * Date Text information frame.
 *
 * The 'Date' frame is a numeric string in the DDMM format containing the date for the recording. This field is always four characters long.
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
class FrameBodyTDAT : AbstractFrameBodyTextInfo, ID3v23FrameBody {
	var isMonthOnly = false

	override val identifier: String?
		get() = ID3v23Frames.FRAME_ID_V3_TDAT

	/**
	 * Creates a new FrameBodyTDAT datatype.
	 */
	constructor()
	constructor(body: FrameBodyTDAT) : super(body)

	/**
	 * Creates a new FrameBodyTDAT datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTDAT datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	companion object {
		const val DATA_SIZE = 4
		const val DAY_START = 0
		const val DAY_END = 2
		const val MONTH_START = 2
		const val MONTH_END = 4
	}
}

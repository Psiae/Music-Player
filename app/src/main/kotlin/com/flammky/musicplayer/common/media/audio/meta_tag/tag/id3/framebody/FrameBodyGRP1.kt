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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * iTunes grouping field introduced in 12.5.4.42, before that iTunes used TIT1 as is the norm, but it now uses that
 * for Classical Work. Jaudiotagger maps WORK key to TXXX:WORK for work because TIT1 is in use more for GROUPING.
 * Unfortunately TIT1 is defined in ID3 spec to be used for either which is problematic
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class FrameBodyGRP1 : AbstractFrameBodyTextInfo, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_ITUNES_GROUPING

	/**
	 * Creates a new FrameBodyTBPM datatype.
	 */
	constructor()
	constructor(body: FrameBodyGRP1) : super(body)

	/**
	 * Creates a new FrameBodyTBPM datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTBPM datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
}

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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.PartOfSet
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
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
class FrameBodyTRCK : AbstractFrameBodyNumberTotal, ID3v23FrameBody, ID3v24FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_TRACK

	/**
	 * Creates a new FrameBodyTRCK datatype.
	 */
	constructor() : super()
	constructor(body: FrameBodyTRCK) : super(body)

	/**
	 * Creates a new FrameBodyTRCK datatype, the value is parsed literally
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String) : super(textEncoding, text)
	constructor(textEncoding: Byte, trackNo: Int?, trackTotal: Int?) : super(
		textEncoding,
		trackNo,
		trackTotal
	)

	/**
	 * Creates a new FrameBodyTRCK datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws java.io.IOException
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	var trackNo: Int
		get() = number!!
		set(trackNo) {
			number = trackNo
		}

	fun setTrackNo(trackNo: String) {
		setNumber(trackNo)
	}

	var trackTotal: Int
		get() = total!!
		set(trackTotal) {
			total = trackTotal
		}

	fun setTrackTotal(trackTotal: String) {
		setTotal(trackTotal)
	}

	override var text: String?
		get() = super.text
		set(text) {
			setObjectValue(DataTypes.OBJ_TEXT, PartOfSet.PartOfSetValue(text))
		}
}

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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.ByteArraySizeTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberFixedLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

class FrameBodySIGN : AbstractID3v2FrameBody, ID3v24FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_SIGNATURE

	/**
	 * Creates a new FrameBodySIGN datatype.
	 */
	constructor() {
		//        this.setObject("Group Symbol", new Byte((byte) 0));
		//        this.setObject("Signature", new byte[0]);
	}

	constructor(body: FrameBodySIGN) : super(body)

	/**
	 * Creates a new FrameBodySIGN datatype.
	 *
	 * @param groupSymbol
	 * @param signature
	 */
	constructor(groupSymbol: Byte, signature: ByteArray?) {
		setObjectValue(DataTypes.OBJ_GROUP_SYMBOL, groupSymbol)
		setObjectValue(DataTypes.OBJ_SIGNATURE, signature)
	}

	/**
	 * Creates a new FrameBodySIGN datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return
	 */
	/**
	 * @param groupSymbol
	 */
	var groupSymbol: Byte
		get() = if (getObjectValue(DataTypes.OBJ_GROUP_SYMBOL) != null) {
			getObjectValue(DataTypes.OBJ_GROUP_SYMBOL) as Byte
		} else {
			0.toByte()
		}
		set(groupSymbol) {
			setObjectValue(DataTypes.OBJ_GROUP_SYMBOL, groupSymbol)
		}
	/**
	 * @return
	 */
	/**
	 * @param signature
	 */
	var signature: ByteArray?
		get() = getObjectValue(DataTypes.OBJ_SIGNATURE) as ByteArray
		set(signature) {
			setObjectValue(DataTypes.OBJ_SIGNATURE, signature)
		}

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(NumberFixedLength(DataTypes.OBJ_GROUP_SYMBOL, this, 1))
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_SIGNATURE, this))
	}
}

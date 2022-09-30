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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.BooleanByte
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberFixedLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * Body of Recommended buffer size frame, generally used for streaming audio
 */
class FrameBodyRBUF : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_RECOMMENDED_BUFFER_SIZE

	/**
	 * Creates a new FrameBodyRBUF datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_BUFFER_SIZE, 0.toByte())
		setObjectValue(DataTypes.OBJ_EMBED_FLAG, java.lang.Boolean.FALSE)
		setObjectValue(DataTypes.OBJ_OFFSET, 0.toByte())
	}

	constructor(body: FrameBodyRBUF) : super(body)

	/**
	 * Creates a new FrameBodyRBUF datatype.
	 *
	 * @param bufferSize
	 * @param embeddedInfoFlag
	 * @param offsetToNextTag
	 */
	constructor(bufferSize: Byte, embeddedInfoFlag: Boolean, offsetToNextTag: Byte) {
		setObjectValue(DataTypes.OBJ_BUFFER_SIZE, bufferSize)
		setObjectValue(DataTypes.OBJ_EMBED_FLAG, embeddedInfoFlag)
		setObjectValue(DataTypes.OBJ_OFFSET, offsetToNextTag)
	}

	/**
	 * Creates a new FrameBodyRBUF datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(NumberFixedLength(DataTypes.OBJ_BUFFER_SIZE, this, BUFFER_FIELD_SIZE))
		objectList.add(
			BooleanByte(
				DataTypes.OBJ_EMBED_FLAG,
				this,
				EMBED_FLAG_BIT_POSITION.toByte().toInt()
			)
		)
		objectList.add(NumberFixedLength(DataTypes.OBJ_OFFSET, this, OFFSET_FIELD_SIZE))
	}

	companion object {
		private const val BUFFER_FIELD_SIZE = 3
		private const val EMBED_FLAG_BIT_POSITION = 1
		private const val OFFSET_FIELD_SIZE = 4
	}
}

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
 * Frame that is not currently suported by this application
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.ByteArraySizeTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import java.nio.ByteBuffer

/**
 * Represents a framebody for a frame identifier jaudiotagger has not implemented a framebody for.
 *
 * This is likley to be because the FrameBody is not specified in the Specification but it may just be because the code
 * has yet to be written, the library uses this framebody when it cant find an alternative. This is different to the
 * ID3v2ExtensionFrameBody Interface which should be implemented by frame bodies that are non standard such as
 * iTunes compilation frame (TCMP) but are commonly used.
 */
class FrameBodyUnsupported : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody,
	ID3v22FrameBody {
	/**
	 * Return the frame identifier
	 *
	 * @return the identifier
	 */
	/**
	 * Because used by any unknown frame identifier varies
	 */
	override var identifier = ""
		private set

	@Deprecated("because no identifier set")
	constructor()

	/**
	 * Creates a new FrameBodyUnsupported
	 * @param identifier
	 */
	constructor(identifier: String) {
		this.identifier = identifier
	}

	/**
	 * Create a new FrameBodyUnsupported
	 *
	 * @param identifier
	 * @param value
	 */
	constructor(identifier: String, value: ByteArray?) {
		this.identifier = identifier
		setObjectValue(DataTypes.OBJ_DATA, value)
	}

	/**
	 * Creates a new FrameBodyUnsupported datatype.
	 *
	 * @param value
	 */
	@Deprecated("because no identifier set")
	constructor(value: ByteArray?) {
		setObjectValue(DataTypes.OBJ_DATA, value)
	}

	/**
	 * Copy constructor
	 *
	 * @param copyObject a copy is made of this
	 */
	constructor(copyObject: FrameBodyUnsupported) : super(copyObject) {
		identifier = copyObject.identifier
	}

	/**
	 * Creates a new FrameBodyUnsupported datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidFrameException if unable to create framebody from buffer
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 * @param obj
	 * @return whether obj is equivalent to this object
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is FrameBodyUnsupported) {
			return false
		}
		return identifier == obj.identifier && super.equals(obj)
	}

	/**
	 * Because the contents of this frame are an array of bytes and could be large we just
	 * return the identifier.
	 *
	 * @return a string representation of this frame
	 */
	override fun toString(): String {
		return identifier
	}

	/**
	 * Setup the Object List. A byte Array which will be read upto frame size
	 * bytes.
	 */
	override fun setupObjectList() {
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_DATA, this))
	}
}

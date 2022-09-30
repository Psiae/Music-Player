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
 * Represents a Unique File ID for the file which relates
 * to an external database.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.ByteArraySizeTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.StringNullTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * A UFID Framebody consists of an owner that identifies the server hosting the
 * unique identifier database, and the unique identifier itself which can be up to 64
 * bytes in length.
 */
class FrameBodyUFID : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_UNIQUE_FILE_ID

	/**
	 * Creates a new FrameBodyUFID datatype.
	 */
	constructor() {
		owner = ""
		uniqueIdentifier = ByteArray(0)
	}

	constructor(body: FrameBodyUFID) : super(body)

	/**
	 * Creates a new FrameBodyUFID datatype.
	 *
	 * @param owner            url of the database
	 * @param uniqueIdentifier unique identifier
	 */
	constructor(owner: String?, uniqueIdentifier: ByteArray?) {
		this.owner = owner
		this.uniqueIdentifier = uniqueIdentifier
	}

	/**
	 * Creates FrameBodyUFID datatype from buffer
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return the url of the the database that this ufid is stored in
	 */
	/**
	 * Set the owner of url of the the database that this ufid is stored in
	 *
	 * @param owner should be a valid url
	 */
	var owner: String?
		get() = getObjectValue(DataTypes.OBJ_OWNER) as String
		set(owner) {
			setObjectValue(DataTypes.OBJ_OWNER, owner)
		}
	/**
	 * @return the unique identifier (within the owners domain)
	 */
	/**
	 * Set the unique identifier (within the owners domain)
	 *
	 * @param uniqueIdentifier
	 */
	var uniqueIdentifier: ByteArray?
		get() = getObjectValue(DataTypes.OBJ_DATA) as ByteArray
		set(uniqueIdentifier) {
			setObjectValue(DataTypes.OBJ_DATA, uniqueIdentifier)
		}

	override fun setupObjectList() {
		objectList.add(StringNullTerminated(DataTypes.OBJ_OWNER, this))
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_DATA, this))
	}

	companion object {
		const val UFID_MUSICBRAINZ = "http://musicbrainz.org"
		const val UFID_ID3TEST = "http://www.id3.org/dummy/ufid.html"
	}
}

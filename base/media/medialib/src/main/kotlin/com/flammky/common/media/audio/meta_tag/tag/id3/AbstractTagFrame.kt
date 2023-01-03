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
 * This class represents 'parts of tags'. It contains methods that they all use
 * use. ID3v2 tags have frames. Lyrics3 tags have fields. ID3v1 tags do not
 * have parts. It also contains their header while the body contains the
 * actual fragments.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.copyObject
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual

/**
 * A frame contains meta-information of a particular type. A frame contains a header and a body
 */
abstract class AbstractTagFrame : AbstractTagItem {
	/**
	 * Actual data this fragment holds
	 */
	@JvmField
	protected var frameBody: AbstractTagFrameBody? = null

	constructor()

	/**
	 * This constructs the bodies copy constructor this in turn invokes
	 * * bodies objectlist.
	 * @param copyObject
	 */
	constructor(copyObject: AbstractTagFrame) {
		frameBody = copyObject(copyObject.frameBody) as AbstractTagFrameBody?
		frameBody!!.header = this
	}
	/**
	 * Returns the body datatype for this fragment. The body datatype contains the
	 * actual information for the fragment.
	 *
	 * @return the body datatype
	 */
	/**
	 * Sets the body datatype for this fragment. The body datatype contains the
	 * actual information for the fragment.
	 *
	 * @param frameBody the body datatype
	 */
	var body: AbstractTagFrameBody?
		get() = this.frameBody
		set(value) {
			value?.header = this
			frameBody = value
		}

	/**
	 * Returns true if this datatype and it's body is a subset of the argument.
	 * This datatype is a subset if the argument is the same class.
	 *
	 * @param obj datatype to determine if subset of
	 * @return true if this datatype and it's body is a subset of the argument.
	 */
	override fun isSubsetOf(obj: Any?): Boolean {
		if (obj !is AbstractTagFrame) {
			return false
		}
		if (frameBody == null && obj.frameBody == null) {
			return true
		}
		return if (frameBody == null || obj.frameBody == null) {
			false
		} else frameBody!!.isSubsetOf(obj.frameBody) && super.isSubsetOf(
			obj
		)
	}

	/**
	 * Returns true if this datatype and its body equals the argument and its
	 * body. this datatype is equal if and only if they are the same class and
	 * have the same `getSubId` id string.
	 *
	 * @param obj datatype to determine equality of
	 * @return true if this datatype and its body equals the argument and its
	 * body.
	 */
	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		if (obj !is AbstractTagFrame) {
			return false
		}
		val that = obj
		return areEqual(
			identifier, that.identifier
		) &&
			areEqual(frameBody, that.frameBody) &&
			super.equals(that)
	}

	override fun toString(): String {
		return body.toString()
	}
}

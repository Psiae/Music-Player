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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody

/**
 * Represents a timestamp field
 */
class StringDateTime : StringSizeTerminated {


	/**
	 * Creates a new ObjectStringDateTime datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	constructor(`object`: StringDateTime?) : super(`object`)

	override var value: Any? = super.value
		get() = field?.toString()?.replace(' ', 'T')
		set(value) {
			if (value != null) {
				field = value.toString().replace(' ', 'T')
			}
		}


	override fun equals(other: Any?): Boolean {
		return other is StringDateTime && super.equals(other)
	}
}

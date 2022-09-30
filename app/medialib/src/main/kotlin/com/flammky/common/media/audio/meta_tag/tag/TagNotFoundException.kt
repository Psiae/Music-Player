/*
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id$
 *
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
 *
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag

/**
 * Thrown if the tag o isn't found. This is different from
 * the `InvalidTagException`. Each tag  has an
 * ID string or some way saying that it simply exists. If this string is
 * missing, `TagNotFoundException` is thrown. If the ID string
 * exists, then any other error while reading throws an
 * `InvalidTagException`.
 *
 * @author Eric Farng
 * @version $Revision$
 */
class TagNotFoundException : TagException {
	/**
	 * Creates a new TagNotFoundException datatype.
	 */
	constructor()

	/**
	 * Creates a new TagNotFoundException datatype.
	 *
	 * @param ex the cause.
	 */
	constructor(ex: Throwable?) : super(ex)

	/**
	 * Creates a new TagNotFoundException datatype.
	 *
	 * @param msg the detail message.
	 */
	constructor(msg: String?) : super(msg)

	/**
	 * Creates a new TagNotFoundException datatype.
	 *
	 * @param msg the detail message.
	 * @param ex  the cause.
	 */
	constructor(msg: String?, ex: Throwable?) : super(msg, ex)

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -7952067424639848036L
	}
}

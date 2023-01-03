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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3

class Lyrics3v1Iterator(lyrics3v1Tag: Lyrics3v1) : MutableIterator<String> {
	/**
	 *
	 */
	private val tag: Lyrics3v1

	/**
	 *
	 */
	private var lastIndex = 0

	/**
	 *
	 */
	private var removeIndex = 0

	/**
	 * Creates a new Lyrics3v1Iterator datatype.
	 *
	 * @param lyrics3v1Tag
	 */
	init {
		tag = lyrics3v1Tag
	}

	/**
	 * @return
	 */
	override fun hasNext(): Boolean {
		return (tag.getLyric().indexOf('\n', lastIndex) < 0
			&& lastIndex <= tag.getLyric().length
			)
	}

	/**
	 * @return
	 * @throws NoSuchElementException
	 */
	override fun next(): String {
		val nextIndex = tag.getLyric().indexOf('\n', lastIndex)
		removeIndex = lastIndex
		val line: String
		if (lastIndex >= 0) {
			line = if (nextIndex >= 0) {
				tag.getLyric().substring(lastIndex, nextIndex)
			} else {
				tag.getLyric().substring(lastIndex)
			}
			lastIndex = nextIndex
		} else {
			throw NoSuchElementException("Iteration has no more elements.")
		}
		return line
	}

	/**
	 *
	 */
	override fun remove() {
		val lyric = tag.getLyric().substring(0, removeIndex) + tag.getLyric().substring(lastIndex)
		tag.setLyric(lyric)
	}
}

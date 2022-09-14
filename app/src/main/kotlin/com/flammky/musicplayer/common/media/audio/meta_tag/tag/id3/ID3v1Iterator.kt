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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

class ID3v1Iterator
/**
 * Creates a new ID3v1Iterator datatype.
 *
 * @param id3v1tag
 */(
	/**
	 *
	 */
	private val id3v1tag: ID3v1Tag
) : MutableIterator<Any?> {
	/**
	 *
	 */
	private val lastIndex = 0

	/**
	 * @return
	 */
	override fun hasNext(): Boolean {
		return hasNext(lastIndex)
	}

	/**
	 * @return
	 */
	override fun next(): Any? {
		return next(lastIndex)
	}

	/**
	 *
	 */
	override fun remove() {
		when (lastIndex) {
			TITLE -> {
				id3v1tag.firstTitle = ""
				id3v1tag.firstArtist = ""
				id3v1tag.firstAlbum = ""
				id3v1tag.firstComment = ""
				id3v1tag.firstYear = ""
				id3v1tag.genre = ((-1).toByte())
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.setTrack(((-1).toByte()).toString())
				}
			}
			ARTIST -> {
				id3v1tag.firstArtist = ""
				id3v1tag.firstAlbum = ""
				id3v1tag.firstComment = ""
				id3v1tag.firstYear = ""
				id3v1tag.genre = ((-1).toByte())
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.setTrack((-1).toByte().toString())
				}
			}
			ALBUM -> {
				id3v1tag.firstAlbum = ""
				id3v1tag.firstComment = ""
				id3v1tag.firstYear = ""
				id3v1tag.genre = (-1.toByte()).toByte()
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.setTrack((-1).toByte().toString())
				}
			}
			COMMENT -> {
				id3v1tag.firstComment = ""
				id3v1tag.firstYear = ""
				id3v1tag.genre = (-1.toByte()).toByte()
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.setTrack((-1).toByte().toString())
				}
			}
			YEAR -> {
				id3v1tag.firstYear = ""
				id3v1tag.genre = (-1.toByte()).toByte()
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.setTrack((-1).toByte().toString())
				}
			}
			GENRE -> {
				id3v1tag.genre = (-1.toByte()).toByte()
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.setTrack((-1).toByte().toString())
				}
			}
			TRACK -> if (id3v1tag is ID3v11Tag) {
				id3v1tag.setTrack((-1).toByte().toString())
			}
		}
	}

	/**
	 * @param index
	 * @return
	 */
	private fun hasNext(index: Int): Boolean {
		return when (index) {
			TITLE -> id3v1tag.firstTitle?.isNotEmpty() == true || hasNext(index + 1)
			ARTIST -> (id3v1tag.firstArtist?.length ?: 0) > 0 || hasNext(index + 1)
			ALBUM -> (id3v1tag.firstAlbum?.length ?: 0) > 0 || hasNext(index + 1)
			COMMENT -> (id3v1tag.firstComment?.length ?: 0) > 0 || hasNext(index + 1)
			YEAR -> (id3v1tag.firstYear?.length ?: 0) > 0 || hasNext(index + 1)
			GENRE -> id3v1tag.genre >= 0.toByte() || hasNext(index + 1)
			TRACK -> {
				if (id3v1tag is ID3v11Tag) {
					id3v1tag.track >= 0.toByte() || hasNext(index + 1)
				} else false
			}
			else -> false
		}
	}

	/**
	 * @param index
	 * @return
	 * @throws NoSuchElementException
	 */
	private fun next(index: Int): Any? {
		return when (lastIndex) {
			0 -> if (id3v1tag.firstTitle?.length ?: 0 > 0) id3v1tag.firstTitle else next(index + 1)
			TITLE -> if (id3v1tag.firstArtist?.length ?: 0 > 0) id3v1tag.firstArtist else next(index + 1)
			ARTIST -> if (id3v1tag.firstAlbum?.length ?: 0 > 0) id3v1tag.firstAlbum else next(index + 1)
			ALBUM -> if (id3v1tag.firstComment?.length ?: 0 > 0) id3v1tag.firstComment else next(index + 1)
			COMMENT -> if (id3v1tag.firstYear?.length ?: 0 > 0) id3v1tag.firstYear else next(index + 1)
			YEAR -> if (id3v1tag.genre >= 0.toByte()) id3v1tag.genre else next(index + 1)
			GENRE -> if (id3v1tag is ID3v11Tag && id3v1tag.track >= 0.toByte()) id3v1tag.track else null
			else -> throw NoSuchElementException("Iteration has no more elements.")
		}
	}

	companion object {
		/**
		 *
		 */
		private const val TITLE = 1

		/**
		 *
		 */
		private const val ARTIST = 2

		/**
		 *
		 */
		private const val ALBUM = 3

		/**
		 *
		 */
		private const val COMMENT = 4

		/**
		 *
		 */
		private const val YEAR = 5

		/**
		 *
		 */
		private const val GENRE = 6

		/**
		 *
		 */
		private const val TRACK = 7
	}
}

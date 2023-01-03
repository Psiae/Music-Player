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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagNotFoundException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.truncate
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class Lyrics3v1 : AbstractLyrics3 {
	/**
	 *
	 */
	private var lyric: String = ""

	/**
	 * Creates a new Lyrics3v1 datatype.
	 */
	constructor()
	constructor(copyObject: Lyrics3v1) : super(copyObject) {
		lyric = copyObject.lyric
	}

	constructor(mp3Tag: AbstractTag?) {
		if (mp3Tag != null) {
			val lyricTag: Lyrics3v2
			lyricTag =
				if (mp3Tag is Lyrics3v1) {
					throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
				} else if (mp3Tag is Lyrics3v2) {
					mp3Tag
				} else {
					Lyrics3v2(mp3Tag)
				}
			val lyricField = lyricTag.getField("LYR")?.body as? FieldFrameBodyLYR
			lyric = lyricField?.lyric ?: ""
		}
	}

	/**
	 * Creates a new Lyrics3v1 datatype.
	 *
	 * @throws TagNotFoundException
	 * @throws IOException
	 * @param byteBuffer
	 */
	constructor(byteBuffer: ByteBuffer) {
		try {
			read(byteBuffer)
		} catch (e: TagException) {
			e.printStackTrace()
		}
	}

	/**
	 * @return
	 */
	override val identifier: String
		get() = "Lyrics3v1.00"

	/**
	 * @param lyric
	 */
	fun setLyric(lyric: String) {
		this.lyric = truncate(lyric, 5100) ?: ""
	}

	/**
	 * @return
	 */
	fun getLyric(): String {
		return lyric
	}

	/**
	 * @return
	 */
	override val size: Int
		get() = "LYRICSBEGIN".length + lyric.length + "LYRICSEND".length

	/**
	 * @param obj
	 * @return
	 */
	override fun isSubsetOf(obj: Any?): Boolean {
		return obj is Lyrics3v1 && obj.lyric.contains(
			lyric
		)
	}

	/**
	 * @param obj
	 * @return
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is Lyrics3v1) {
			return false
		}
		return lyric == obj.lyric && super.equals(obj)
	}

	/**
	 * @return
	 * @throws UnsupportedOperationException
	 */
	override fun iterator(): Iterator<Any?>? {
		/**
		 * @todo Implement this org.jaudiotagger.tag.AbstractMP3Tag abstract method
		 */
		throw UnsupportedOperationException("Method iterator() not yet implemented.")
	}

	/**
	 * TODO implement
	 *
	 * @param byteBuffer
	 * @return
	 * @throws IOException
	 */
	override fun seek(byteBuffer: ByteBuffer?): Boolean {
		return false
	}

	/**
	 * @param byteBuffer
	 * @throws TagNotFoundException
	 * @throws IOException
	 */
	@Throws(TagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val buffer = ByteArray(5100 + 9 + 11)
		val lyricBuffer: String
		if (!seek(byteBuffer)) {
			throw TagNotFoundException("ID3v1 tag not found")
		}
		byteBuffer[buffer]
		lyricBuffer = String(buffer)
		lyric = lyricBuffer.substring(0, lyricBuffer.indexOf("LYRICSEND"))
	}

	/**
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun seek(file: RandomAccessFile): Boolean {
		val buffer = ByteArray(5100 + 9 + 11)
		var lyricsEnd: String
		val lyricsStart: String
		var offset: Long

		// check right before the ID3 1.0 tag for the lyrics tag
		file.seek(file.length() - 128 - 9)
		file.read(buffer, 0, 9)
		lyricsEnd = String(buffer, 0, 9)
		if (lyricsEnd == "LYRICSEND") {
			offset = file.filePointer
		} else {
			// check the end of the file for a lyrics tag incase an ID3
			// tag wasn't placed after it.
			file.seek(file.length() - 9)
			file.read(buffer, 0, 9)
			lyricsEnd = String(buffer, 0, 9)
			offset = if (lyricsEnd == "LYRICSEND") {
				file.filePointer
			} else {
				return false
			}
		}

		// the tag can at most only be 5100 bytes
		offset -= (5100 + 9 + 11).toLong()
		file.seek(offset)
		file.read(buffer)
		lyricsStart = String(buffer)

		// search for the tag
		val i = lyricsStart.indexOf("LYRICSBEGIN")
		if (i == -1) {
			return false
		}
		file.seek(offset + i + 11)
		return true
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		val str = """${identifier} ${size}
"""
		return str + lyric
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile?) {
		var str: String
		var offset: Int
		val buffer: ByteArray
		delete(file)
		file!!.seek(file.length())
		buffer = ByteArray(lyric.length + 11 + 9)
		str = "LYRICSBEGIN"
		for (i in 0 until str.length) {
			buffer[i] = str[i].code.toByte()
		}
		offset = str.length
		str = truncate(lyric, 5100)!!
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		offset += str.length
		str = "LYRICSEND"
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		offset += str.length
		file.write(buffer, 0, offset)
	}
}

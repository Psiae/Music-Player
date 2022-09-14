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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidTagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.ID3v2LyricLine
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.Lyrics3Line
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.Lyrics3TimeStamp
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodySYLT
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyUSLT
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class FieldFrameBodyLYR : AbstractLyrics3v2FieldFrameBody {
	/**
	 *
	 */
	private var lines = ArrayList<Lyrics3Line>()

	/**
	 * Creates a new FieldBodyLYR datatype.
	 */
	constructor()
	constructor(copyObject: FieldFrameBodyLYR) : super(copyObject) {
		var old: Lyrics3Line
		for (i in copyObject.lines.indices) {
			old = copyObject.lines[i]
			lines.add(Lyrics3Line(old))
		}
	}

	/**
	 * Creates a new FieldBodyLYR datatype.
	 *
	 * @param line
	 */
	constructor(line: String) {
		readString(line)
	}

	/**
	 * Creates a new FieldBodyLYR datatype.
	 *
	 * @param sync
	 */
	constructor(sync: FrameBodySYLT?) {
		addLyric(sync)
	}

	/**
	 * Creates a new FieldBodyLYR datatype.
	 *
	 * @param unsync
	 */
	constructor(unsync: FrameBodyUSLT?) {
		addLyric(unsync)
	}

	/**
	 * Creates a new FieldBodyLYR datatype.
	 * @param byteBuffer
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer) {
		read(byteBuffer)
	}

	/**
	 * @return
	 */
	override val identifier: String
		get() = "LYR"
	/**
	 * @return
	 */
	/**
	 * @param str
	 */
	var lyric: String
		get() = writeString()
		set(str) {
			readString(str)
		}//return size - 2; // cut off the last crlf pair

	/**
	 * @return
	 */
	override val size: Int
		get() {
			var size = 0
			var line: Lyrics3Line
			for (line1 in lines) {
				line = line1
				size += line.size + 2
			}
			return size

			//return size - 2; // cut off the last crlf pair
		}

	/**
	 * @param obj
	 * @return
	 */
	override fun isSubsetOf(obj: Any?): Boolean {
		if (obj !is FieldFrameBodyLYR) {
			return false
		}
		val superset = obj.lines
		for (line in lines) {
			if (!superset.contains(line)) {
				return false
			}
		}
		return super.isSubsetOf(obj)
	}

	/**
	 * @param sync
	 */
	fun addLyric(sync: FrameBodySYLT?) {
		// SYLT frames are made of individual lines
		val iterator = sync!!.iterator()
		val lineMap = HashMap<String, Lyrics3Line>()
		while (iterator!!.hasNext()) {
			val element = iterator.next()
			if (element is ID3v2LyricLine) {
				var currentLine = element

				// createField copy to use in new tag
				currentLine = ID3v2LyricLine(currentLine)
				val timeStamp = Lyrics3TimeStamp("Time Stamp", this)
				timeStamp.setTimeStamp(currentLine.timeStamp, sync.timeStampFormat.toByte())
				var newLine: Lyrics3Line?
				if (lineMap.containsKey(currentLine.text)) {
					newLine = lineMap[currentLine.text]
					newLine!!.addTimeStamp(timeStamp)
				} else {
					newLine = Lyrics3Line("Lyric Line", this)
					newLine.setLyric(currentLine)
					newLine.setTimeStamp(timeStamp)
					lineMap[currentLine.text] = newLine
					lines.add(newLine)
				}
			}
		}
	}

	/**
	 * @param unsync
	 */
	fun addLyric(unsync: FrameBodyUSLT?) {
		// USLT frames are just long text string;
		val line = Lyrics3Line("Lyric Line", this)
		line.setLyric(unsync!!.lyric)
		lines.add(line)
	}

	/**
	 * @param obj
	 * @return
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is FieldFrameBodyLYR) {
			return false
		}
		return lines == obj.lines && super.equals(obj)
	}

	/**
	 * @return
	 */
	fun hasTimeStamp(): Boolean {
		var present = false
		for (line in lines) {
			if (line.hasTimeStamp()) {
				present = true
			}
		}
		return present
	}

	/**
	 * @return
	 */
	override fun iterator(): Iterator<Lyrics3Line?>? {
		return lines.iterator()
	}

	/**
	 *
	 *
	 *
	 */
	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val lineString: String
		var buffer = ByteArray(5)

		// read the 5 character size
		byteBuffer[buffer, 0, 5]
		val size = String(buffer, 0, 5).toInt()
		if (size == 0 && !TagOptionSingleton.instance.isLyrics3KeepEmptyFieldIfRead) {
			throw InvalidTagException("Lyircs3v2 Field has size of zero.")
		}
		buffer = ByteArray(size)

		// read the SIZE length description
		byteBuffer[buffer]
		lineString = String(buffer)
		readString(lineString)
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		var str = identifier + " : "
		for (line in lines) {
			str += line.toString()
		}
		return str
	}

	/**
	 * @param file
	 * @throws java.io.IOException
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile) {
		val size: Int
		var offset = 0
		var buffer = ByteArray(5)
		var str: String
		size = this.size
		str = Integer.toString(size)
		for (i in 0 until 5 - str.length) {
			buffer[i] = '0'.code.toByte()
		}
		offset += 5 - str.length
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		offset += str.length
		file.write(buffer, 0, 5)
		if (size > 0) {
			str = writeString()
			buffer = ByteArray(str.length)
			for (i in 0 until str.length) {
				buffer[i] = str[i].code.toByte()
			}
			file.write(buffer)
		}
	}

	/**
	 * @param lineString
	 */
	private fun readString(lineString: String) {
		// now readString each line and put in the vector;
		var token: String?
		var offset = 0
		var delim: Int = lineString.indexOf(Lyrics3v2Fields.Companion.CRLF)
		lines = ArrayList()
		var line: Lyrics3Line
		while (delim >= 0) {
			token = lineString.substring(offset, delim)
			line = Lyrics3Line("Lyric Line", this)
			line.setLyric(token)
			lines.add(line)
			offset = delim + Lyrics3v2Fields.Companion.CRLF.length
			delim = lineString.indexOf(Lyrics3v2Fields.Companion.CRLF, offset)
		}
		if (offset < lineString.length) {
			token = lineString.substring(offset)
			line = Lyrics3Line("Lyric Line", this)
			line.setLyric(token)
			lines.add(line)
		}
	}

	/**
	 * @return
	 */
	private fun writeString(): String {
		var line: Lyrics3Line
		var str = ""
		for (line1 in lines) {
			line = line1
			str += line.writeString() + Lyrics3v2Fields.Companion.CRLF
		}
		return str

		//return str.substring(0,str.length()-2); // cut off the last CRLF pair
	}

	/**
	 * TODO
	 */
	override fun setupObjectList() {}
}

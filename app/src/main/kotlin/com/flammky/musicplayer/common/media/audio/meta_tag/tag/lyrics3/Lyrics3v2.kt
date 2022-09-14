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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractID3v2Frame
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v1Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class Lyrics3v2 : AbstractLyrics3 {


	/**
	 *
	 */
	private var fieldMap = HashMap<String?, Lyrics3v2Field>()

	/**
	 * Creates a new Lyrics3v2 datatype.
	 */
	constructor()
	constructor(copyObject: Lyrics3v2) : super(copyObject) {
		val iterator: Iterator<String?> = copyObject.fieldMap.keys.iterator()
		var oldIdentifier: String?
		var newIdentifier: String?
		var newObject: Lyrics3v2Field
		while (iterator.hasNext()) {
			oldIdentifier = iterator.next()
			newIdentifier = oldIdentifier
			newObject = Lyrics3v2Field(
				copyObject.fieldMap[newIdentifier]
			)
			fieldMap[newIdentifier] = newObject
		}
	}

	/**
	 * Creates a new Lyrics3v2 datatype.
	 *
	 * @param mp3tag
	 * @throws UnsupportedOperationException
	 */
	constructor(mp3tag: AbstractTag?) {
		if (mp3tag != null) {
			// upgrade the tag to lyrics3v2
			if (mp3tag is Lyrics3v2) {
				throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
			} else if (mp3tag is Lyrics3v1) {
				val newField: Lyrics3v2Field
				newField = Lyrics3v2Field(
					FieldFrameBodyLYR(mp3tag.getLyric())
				)
				fieldMap[newField.identifier] = newField
			} else {
				var newField: Lyrics3v2Field?
				val iterator: Iterator<List<TagField?>?>?
				iterator = ID3v24Tag(mp3tag).iterator()
				while (iterator!!.hasNext()) {
					try {
						val fields = iterator.next()
						for (element in fields!!) {
							if (element is AbstractID3v2Frame) {
								val frame = element
								if (Lyrics3v2Field.Companion.isLyrics3v2Field(frame)) {
									newField = Lyrics3v2Field(frame)
									if (newField != null) {
										fieldMap[newField.identifier] = newField
									}
								}
							}
						}
					} catch (ex: TagException) {
						//invalid frame to createField lyrics3 field. ignore and keep going
					}
				}
			}
		}
	}

	/**
	 * Creates a new Lyrics3v2 datatype.
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
	 * @param field
	 */
	fun setField(field: Lyrics3v2Field) {
		fieldMap[field.identifier] = field
	}

	/**
	 * Gets the value of the frame identified by identifier
	 *
	 * @param identifier The three letter code
	 * @return The value associated with the identifier
	 */
	fun getField(identifier: String?): Lyrics3v2Field? {
		return fieldMap[identifier]
	}

	/**
	 * @return
	 */
	val fieldCount: Int
		get() = fieldMap.size

	/**
	 * @return
	 */
	override val identifier: String
		get() = "Lyrics3v2.00"// include LYRICSBEGIN, but not 6 char size or LYRICSEND

	/**
	 * @return
	 */
	override val size: Int
		get() {
			var size = 0
			val iterator: Iterator<Lyrics3v2Field> = fieldMap.values.iterator()
			var field: Lyrics3v2Field
			while (iterator.hasNext()) {
				field = iterator.next()
				size += field.size
			}

			// include LYRICSBEGIN, but not 6 char size or LYRICSEND
			return 11 + size
		}

	/**
	 * @param obj
	 * @return
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is Lyrics3v2) {
			return false
		}
		return fieldMap == obj.fieldMap && super.equals(obj)
	}

	/**
	 * @param identifier
	 * @return
	 */
	fun hasField(identifier: String?): Boolean {
		return fieldMap.containsKey(identifier)
	}

	/**
	 * @return
	 */
	override fun iterator(): Iterator<Lyrics3v2Field?>? {
		return fieldMap.values.iterator()
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

	@Throws(TagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val lyricSize: Int
		lyricSize = if (seek(byteBuffer)) {
			seekSize(byteBuffer)
		} else {
			throw TagNotFoundException("Lyrics3v2.00 Tag Not Found")
		}

		// reset file pointer to the beginning of the tag;
		seek(byteBuffer)
		byteBuffer.position()
		fieldMap = HashMap()
		var lyric: Lyrics3v2Field

		// read each of the fields
		while (byteBuffer.position() < lyricSize - 11) {
			try {
				lyric = Lyrics3v2Field(byteBuffer)
				setField(lyric)
			} catch (ex: InvalidTagException) {
				// keep reading until we're done
			}
		}
	}

	/**
	 * @param identifier
	 */
	fun removeField(identifier: String?) {
		fieldMap.remove(identifier)
	}

	/**
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun seek(file: RandomAccessFile): Boolean {
		val buffer = ByteArray(11)
		var lyricEnd: String
		val lyricStart: String
		var filePointer: Long
		val lyricSize: Long

		// check right before the ID3 1.0 tag for the lyrics tag
		file.seek(file.length() - 128 - 9)
		file.read(buffer, 0, 9)
		lyricEnd = String(buffer, 0, 9)
		if (lyricEnd == "LYRICS200") {
			filePointer = file.filePointer
		} else {
			// check the end of the file for a lyrics tag incase an ID3
			// tag wasn't placed after it.
			file.seek(file.length() - 9)
			file.read(buffer, 0, 9)
			lyricEnd = String(buffer, 0, 9)
			filePointer = if (lyricEnd == "LYRICS200") {
				file.filePointer
			} else {
				return false
			}
		}

		// read the 6 bytes for the length of the tag
		filePointer -= (9 + 6).toLong()
		file.seek(filePointer)
		file.read(buffer, 0, 6)
		lyricSize = String(buffer, 0, 6).toInt().toLong()

		// read the lyrics begin tag if it exists.
		file.seek(filePointer - lyricSize)
		file.read(buffer, 0, 11)
		lyricStart = String(buffer, 0, 11)
		return lyricStart == "LYRICSBEGIN"
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		val iterator: Iterator<Lyrics3v2Field> = fieldMap.values.iterator()
		var field: Lyrics3v2Field
		var str = """${identifier} ${size}
"""
		while (iterator.hasNext()) {
			field = iterator.next()
			str += field.toString() + "\n"
		}
		return str
	}

	/**
	 * @param identifier
	 */
	fun updateField(identifier: String) {
		var lyrField: Lyrics3v2Field?
		if (identifier == "IND") {
			val lyricsPresent = fieldMap.containsKey("LYR")
			var timeStampPresent = false
			if (lyricsPresent) {
				lyrField = fieldMap["LYR"]
				val lyrBody = lyrField!!.body as FieldFrameBodyLYR?
				timeStampPresent = lyrBody!!.hasTimeStamp()
			}
			lyrField = Lyrics3v2Field(FieldFrameBodyIND(lyricsPresent, timeStampPresent))
			setField(lyrField)
		}
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile?) {
		var offset = 0
		val size: Long
		val filePointer: Long
		val buffer = ByteArray(6 + 9)
		var str: String
		var field: Lyrics3v2Field?
		val iterator: Iterator<Lyrics3v2Field>
		ID3v1Tag()
		delete(file)
		file!!.seek(file.length())
		filePointer = file.filePointer
		str = "LYRICSBEGIN"
		for (i in 0 until str.length) {
			buffer[i] = str[i].code.toByte()
		}
		file.write(buffer, 0, str.length)

		// IND needs to go first. lets createField/update it and write it first.
		updateField("IND")
		field = fieldMap["IND"]
		field!!.write(file)
		iterator = fieldMap.values.iterator()
		while (iterator.hasNext()) {
			field = iterator.next()
			val id: String = field.identifier
			val save = TagOptionSingleton.instance.getLyrics3SaveField(id)
			if (id != "IND" && save) {
				field.write(file)
			}
		}
		size = file.filePointer - filePointer
		if (this.size.toLong() != size) {
			//logger.config("Lyrics3v2 size didn't match up while writing.");
			//logger.config("this.getsize()     = " + this.getSize());
			//logger.config("size (filePointer) = " + size);
		}
		str = java.lang.Long.toString(size)
		for (i in 0 until 6 - str.length) {
			buffer[i] = '0'.code.toByte()
		}
		offset += 6 - str.length
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		offset += str.length
		str = "LYRICS200"
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		offset += str.length
		file.write(buffer, 0, offset)
	}

	/**
	 * TODO
	 * @param byteBuffer
	 * @return
	 */
	private fun seekSize(byteBuffer: ByteBuffer?): Int {
		/*
		byte[] buffer = new byte[11];
		String lyricEnd = "";
		long filePointer = 0;

		// check right before the ID3 1.0 tag for the lyrics tag
		file.seek(file.length() - 128 - 9);
		file.read(buffer, 0, 9);
		lyricEnd = new String(buffer, 0, 9);

		if (lyricEnd.equals("LYRICS200"))
		{
				filePointer = file.getFilePointer();
		}
		else
		{
				// check the end of the file for a lyrics tag incase an ID3
				// tag wasn't placed after it.
				file.seek(file.length() - 9);
				file.read(buffer, 0, 9);
				lyricEnd = new String(buffer, 0, 9);

				if (lyricEnd.equals("LYRICS200"))
				{
						filePointer = file.getFilePointer();
				}
				else
				{
						return -1;
				}
		}

		// read the 6 bytes for the length of the tag
		filePointer -= (9 + 6);
		file.seek(filePointer);
		file.read(buffer, 0, 6);

		return Integer.parseInt(new String(buffer, 0, 6));
		*/
		return -1
	}
}

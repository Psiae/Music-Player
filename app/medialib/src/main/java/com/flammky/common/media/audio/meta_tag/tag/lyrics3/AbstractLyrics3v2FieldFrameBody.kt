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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractDataType
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

abstract class AbstractLyrics3v2FieldFrameBody : AbstractTagFrameBody {
	constructor()
	constructor(copyObject: AbstractLyrics3v2FieldFrameBody) : super(copyObject)

	/**
	 * This is called by superclass when attempt to read data from file.
	 *
	 * @param file
	 * @return
	 * @throws InvalidTagException
	 * @throws IOException
	 */
	@Throws(InvalidTagException::class, IOException::class)
	protected fun readHeader(file: RandomAccessFile): Int {
		val size: Int
		val buffer = ByteArray(5)

		// read the 5 character size
		file.read(buffer, 0, 5)
		size = String(buffer, 0, 5).toInt()
		if (size == 0 && !TagOptionSingleton.instance.isLyrics3KeepEmptyFieldIfRead) {
			throw InvalidTagException("Lyircs3v2 Field has size of zero.")
		}
		return size
	}

	/**
	 * This is called by superclass when attempt to write data from file.
	 *
	 * @param file
	 * @param size
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected fun writeHeader(file: RandomAccessFile, size: Int) {
		val str: String
		var offset = 0
		val buffer = ByteArray(5)
		/**
		 * @todo change this to use pad String
		 */
		str = Integer.toString(size)
		for (i in 0 until 5 - str.length) {
			buffer[i] = '0'.code.toByte()
		}
		offset += 5 - str.length
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		file.write(buffer)
	}

	/**
	 * This reads a frame body from its file into the appropriate FrameBody class
	 * Read the data from the given file into this datatype. The file needs to
	 * have its file pointer in the correct location. The size as indicated in the
	 * header is passed to the frame constructor when reading from file.
	 *
	 * @param byteBuffer file to read
	 * @throws IOException         on any I/O error
	 * @throws InvalidTagException if there is any error in the data format.
	 */
	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val size = size
		//Allocate a buffer to the size of the Frame Body and read from file
		val buffer = ByteArray(size)
		byteBuffer[buffer]
		//Offset into buffer, incremented by length of previous MP3Object
		var offset = 0

		//Go through the ObjectList of the Frame reading the data into the
		//correct datatype.
		var `object`: AbstractDataType?
		val iterator: Iterator<AbstractDataType?> = objectList.listIterator()
		while (iterator.hasNext()) {
			//The read has extended further than the defined frame size
			if (offset > size - 1) {
				throw InvalidTagException("Invalid size for Frame Body")
			}

			//Get next Object and load it with data from the Buffer
			`object` = iterator.next()
			`object`?.readByteArray(buffer, offset)
			//Increment Offset to start of next datatype.
			offset += `object`?.size ?: 0
		}
	}

	/**
	 * Write the contents of this datatype to the file at the position it is
	 * currently at.
	 *
	 * @param file destination file
	 * @throws IOException on any I/O error
	 */
	@Throws(IOException::class)
	open fun write(file: RandomAccessFile) {
		//Write the various fields to file in order
		var buffer: ByteArray?
		var `object`: AbstractDataType?
		val iterator: Iterator<AbstractDataType?> = objectList.listIterator()
		while (iterator.hasNext()) {
			`object` = iterator.next()
			buffer = `object`?.writeByteArray()
			file.write(buffer)
		}
	}
}

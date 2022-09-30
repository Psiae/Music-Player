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
 * Abstract Superclass of all Frame Bodys
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidTagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Contains the content for an ID3v2 frame, (the header is held directly within the frame
 */
abstract class AbstractID3v2FrameBody : AbstractTagFrameBody {
	/**
	 * Return size of frame body,if frameBody already exist will take this value from the frame header
	 * but it is always recalculated before writing any changes back to disk.
	 *
	 * @return size in bytes of this frame body
	 */
	/**
	 * Set size based on size passed as parameter from frame header,
	 * done before read
	 * @param size
	 */
	/**
	 * Frame Body Size, originally this is size as indicated in frame header
	 * when we come to writing data we recalculate it.
	 */
	override var size = 0

	/**
	 * Create Empty Body. Super Constructor sets up Object list
	 */
	protected constructor()

	/**
	 * Create Body based on another body
	 * @param copyObject
	 */
	protected constructor(copyObject: AbstractID3v2FrameBody) : super(copyObject)

	/**
	 * Creates a new FrameBody dataType from file. The super
	 * Constructor sets up the Object list for the frame.
	 *
	 * @param byteBuffer from where to read the frame body from
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	protected constructor(byteBuffer: ByteBuffer, frameSize: Int) : super() {
		size = frameSize
		read(byteBuffer)
	}

	/**
	 * Return the ID3v2 Frame Identifier, must be implemented by concrete subclasses
	 *
	 * @return the frame identifier
	 */
	abstract override val identifier: String?

	/**
	 * Set size based on size of the DataTypes making up the body,done after write
	 */
	fun setSize() {
		size = 0
		for (`object` in objectList) {
			size += `object`?.size ?: 0
		}
	}

	/**
	 * Are two bodies equal
	 *
	 * @param obj
	 */
	override fun equals(obj: Any?): Boolean {
		return obj is AbstractID3v2FrameBody && super.equals(obj)
	}

	/**
	 * This reads a frame body from a ByteBuffer into the appropriate FrameBody class and update the position of the
	 * buffer to be just after the end of this frameBody
	 *
	 * The ByteBuffer represents the tag and its position should be at the start of this frameBody. The size as
	 * indicated in the header is passed to the frame constructor when reading from file.
	 *
	 * @param byteBuffer file to read
	 * @throws InvalidFrameException if unable to construct a frameBody from the ByteBuffer
	 */
	//TODO why don't we just slice byteBuffer, set limit to size and convert readByteArray to take a ByteBuffer
	//then we wouldn't have to temporary allocate space for the buffer, using lots of needless memory
	//and providing extra work for the garbage collector.
	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val size = size
		logger.config("Reading body for" + identifier + ":" + size)

		//Allocate a buffer to the size of the Frame Body and read from file
		val buffer = ByteArray(size)
		byteBuffer[buffer]

		//Offset into buffer, incremented by length of previous dataType
		//this offset is only used internally to decide where to look for the next
		//dataType within a frameBody, it does not decide where to look for the next frame body
		var offset = 0

		//Go through the ObjectList of the Frame reading the data into the
		for (`object` in objectList)  //correct dataType.
		{
			logger.finest(
				"offset:$offset"
			)

			//The read has extended further than the defined frame size (ok to extend upto
			//size because the next datatype may be of length 0.)
			if (offset > size) {
				logger.warning("Invalid Size for FrameBody")
				throw InvalidFrameException("Invalid size for Frame Body")
			}

			//Try and load it with data from the Buffer
			//if it fails frame is invalid
			try {
				`object`?.readByteArray(buffer, offset)
			} catch (e: InvalidDataTypeException) {
				logger.warning("Problem reading datatype within Frame Body:" + e.message)
				throw e
			}
			//Increment Offset to start of next datatype.
			offset += `object`?.size ?: 0
		}
	}

	/**
	 * Write the contents of this datatype to the byte array
	 *
	 * @param tagBuffer
	 */
	open fun write(tagBuffer: ByteArrayOutputStream) {
		logger.config("Writing frame body for" + identifier + ":Est Size:" + size)
		//Write the various fields to file in order
		for (`object` in objectList) {
			val objectData = `object`?.writeByteArray()
			if (objectData != null) {
				try {
					tagBuffer.write(objectData)
				} catch (ioe: IOException) {
					//This could never happen coz not writing to file, so convert to RuntimeException
					throw RuntimeException(ioe)
				}
			}
		}
		setSize()
		logger.config("Written frame body for" + identifier + ":Real Size:" + size)
	}

	/**
	 * Return String Representation of Datatype     *
	 */
	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_BODY, "")
		for (nextObject in objectList) {
			nextObject?.createStructure()
		}
		MP3File.structureFormatter?.closeHeadingElement(TYPE_BODY)
	}

	companion object {
		protected const val TYPE_BODY = "body"
	}
}

/*
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
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.EmptyFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameIdentifierException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID22To23
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID22To24
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID23To22
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.copyObject
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.forceFrameID22To23
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.forceFrameID23To22
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.isID3v22FrameIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.isID3v23FrameIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.AbstractID3v2FrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyDeprecated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyUnsupported
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.regex.Pattern

/**
 * Represents an ID3v2.2 frame.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */

/** [org.jaudiotagger.tag.id3.ID3v22Frame] */
class ID3v22Frame : AbstractID3v2Frame {


	override val frameIdSize: Int
		get() = Companion.FRAME_ID_SIZE

	override val frameSizeSize: Int
		get() = Companion.FRAME_ID_SIZE

	override val frameHeaderSize: Int
		get() = Companion.FRAME_HEADER_SIZE

	constructor()

	/**
	 * Creates a new ID3v22 Frame with given body
	 *
	 * @param body New body and frame is based on this
	 */
	constructor(body: AbstractID3v2FrameBody?) : super(body)

	/**
	 * Compare for equality
	 * To be deemed equal obj must be a IDv23Frame with the same identifier
	 * and the same flags.
	 * containing the same body,datatype list ectera.
	 * equals() method is made up from all the various components
	 *
	 * @param obj
	 * @return if true if this object is equivalent to obj
	 */
	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		if (obj !is ID3v22Frame) {
			return false
		}
		val that = obj
		return areEqual(
			statusFlags, that.statusFlags
		) &&
			areEqual(
				encodingFlags, that.encodingFlags
			) &&
			super.equals(that)
	}

	/**
	 * Creates a new ID3v22 Frame of type identifier.
	 *
	 * An empty body of the correct type will be automatically created. This constructor should be used when wish to
	 * create a new frame from scratch using user values
	 * @param identifier
	 */
	constructor(identifier: String) {
		logger.config(
			"Creating empty frame of type$identifier"
		)
		var bodyIdentifier: String? = identifier
		this.identifier = identifier

		//If dealing with v22 identifier (Note this constructor is used by all three tag versions)
		if (isID3v22FrameIdentifier(
				bodyIdentifier!!
			)
		) {
			//Does it have its own framebody (PIC,CRM) or are we using v23/v24 body (the normal case)
			if (forceFrameID22To23(bodyIdentifier) != null) {
				//Do not convert
			} else if (bodyIdentifier == "CRM") {
				//Do not convert.
				//TODO we don't have a way of converting this to v23 which is why its not in the ForceMap
			} else if (bodyIdentifier == ID3v22Frames.FRAME_ID_V2_TYER || bodyIdentifier == ID3v22Frames.FRAME_ID_V2_TIME) {
				bodyIdentifier = ID3v24Frames.FRAME_ID_YEAR
			} else if (isID3v22FrameIdentifier(
					bodyIdentifier
				)
			) {
				bodyIdentifier = convertFrameID22To23(
					bodyIdentifier
				)
			}
		}

		// Use reflection to map id to frame body, which makes things much easier
		// to keep things up to date.
		frameBody = try {
			val c =
				Class.forName("org.jaudiotagger.tag.id3.framebody.FrameBody$bodyIdentifier") as Class<AbstractID3v2FrameBody>
			c.newInstance()
		} catch (cnfe: ClassNotFoundException) {
			logger.log(Level.SEVERE, cnfe.message, cnfe)
			FrameBodyUnsupported(identifier)
		} //Instantiate Interface/Abstract should not happen
		catch (ie: InstantiationException) {
			logger.log(Level.SEVERE, ie.message, ie)
			throw RuntimeException(ie)
		} //Private Constructor shouild not happen
		catch (iae: IllegalAccessException) {
			logger.log(Level.SEVERE, iae.message, iae)
			throw RuntimeException(iae)
		}
		frameBody!!.header = this
		logger.config("Created empty frame of type" + this.identifier + "with frame body of" + bodyIdentifier)
	}

	/**
	 * Copy Constructor
	 *
	 * Creates a new v22 frame based on another v22 frame
	 * @param frame
	 */
	constructor(frame: ID3v22Frame?) : super(frame) {
		logger.config("Creating frame from a frame of same version")
	}

	@Throws(InvalidFrameException::class)
	private fun createV22FrameFromV23Frame(frame: ID3v23Frame) {
		identifier = convertFrameID23To22(frame.identifier)!!
		if (identifier != null) {
			logger.config("V2:Orig id is:" + frame.identifier + ":New id is:" + identifier)
			frameBody = copyObject(frame.body) as AbstractID3v2FrameBody?
		} else if (isID3v23FrameIdentifier(frame.identifier)) {
			identifier = forceFrameID23To22(frame.identifier)!!
			if (identifier != null) {
				logger.config("V2:Force:Orig id is:" + frame.identifier + ":New id is:" + identifier)
				frameBody = this.readBody(identifier, (frame.body as AbstractID3v2FrameBody?)!!)
			} else {
				throw InvalidFrameException("Unable to convert v23 frame:" + frame.identifier + " to a v22 frame")
			}
		} else if (frame.body is FrameBodyDeprecated) {
			//Was it valid for this tag version, if so try and reconstruct
			if (isID3v22FrameIdentifier(frame.identifier)) {
				frameBody = frame.body
				identifier = frame.identifier
				logger.config("DEPRECATED:Orig id is:" + frame.identifier + ":New id is:" + identifier)
			} else {
				frameBody = FrameBodyDeprecated(frame.body as FrameBodyDeprecated?)
				identifier = frame.identifier
				logger.config("DEPRECATED:Orig id is:" + frame.identifier + ":New id is:" + identifier)
			}
		} else {
			frameBody = FrameBodyUnsupported(frame.body as FrameBodyUnsupported)
			identifier = frame.identifier
			logger.config("v2:UNKNOWN:Orig id is:" + frame.identifier + ":New id is:" + identifier)
		}
	}

	/**
	 * Creates a new ID3v22 Frame from another frame of a different tag version
	 *
	 * @param frame to construct the new frame from
	 * @throws InvalidFrameException
	 */
	constructor(frame: AbstractID3v2Frame?) {
		logger.config("Creating frame from a frame of a different version")
		if (frame is ID3v22Frame) {
			throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
		}

		// If it is a v24 frame is it possible to convert it into a v23 frame, and then convert from that
		if (frame is ID3v24Frame) {
			val v23Frame = ID3v23Frame(frame)
			createV22FrameFromV23Frame(v23Frame)
		} else if (frame is ID3v23Frame) {
			createV22FrameFromV23Frame(frame)
		}
		frameBody!!.header = this
		logger.config("Created frame from a frame of a different version")
	}

	/**
	 * Creates a new ID3v22Frame datatype by reading from byteBuffer.
	 *
	 * @param byteBuffer to read from
	 * @param loggingFilename
	 * @throws InvalidFrameException
	 */
	constructor(byteBuffer: ByteBuffer, loggingFilename: String) {
		this.loggingFilename = loggingFilename
		read(byteBuffer)
	}

	/**
	 * Creates a new ID3v23Frame datatype by reading from byteBuffer.
	 *
	 * @param byteBuffer to read from
	 * @throws InvalidFrameException
	 */
	@Deprecated(
		"""use {@link #ID3v22Frame(ByteBuffer,String)} instead
      """
	)
	constructor(byteBuffer: ByteBuffer) : this(byteBuffer, "")

	/**
	 * Return size of frame
	 *
	 * @return int size of frame
	 */
	override val size: Int
		get() = frameBody!!.size + this.frameHeaderSize

	override fun isPadding(buffer: ByteArray): Boolean {
		return buffer[0] == '\u0000'.code.toByte() && buffer[1] == '\u0000'.code.toByte() && buffer[2] == '\u0000'.code.toByte()
	}

	/**
	 * Read frame from file.
	 * Read the frame header then delegate reading of data to frame body.
	 *
	 * @param byteBuffer
	 */
	@Throws(InvalidFrameException::class, InvalidDataTypeException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val identifier = readIdentifier(byteBuffer)
		val buffer = ByteArray(this.frameSizeSize)

		// Is this a valid identifier?
		if (!isValidID3v2FrameIdentifier(identifier)) {
			logger.config(
				"Invalid identifier:$identifier"
			)
			byteBuffer.position(byteBuffer.position() - (this.frameIdSize - 1))
			throw InvalidFrameIdentifierException(
				"$loggingFilename:$identifier:is not a valid ID3v2.20 frame"
			)
		}
		//Read Frame Size (same size as Frame Id so reuse buffer)
		byteBuffer[buffer, 0, this.frameSizeSize]
		frameSize = decodeSize(buffer)
		if (frameSize < 0) {
			throw InvalidFrameException("$identifier has invalid size of:$frameSize")
		} else if (frameSize == 0) {
			//We dont process this frame or add to framemap becuase contains no useful information
			logger.warning(
				"Empty Frame:$identifier"
			)
			throw EmptyFrameException("$identifier is empty frame")
		} else if (frameSize > byteBuffer.remaining()) {
			logger.warning(
				"Invalid Frame size larger than size before mp3 audio:$identifier"
			)
			throw InvalidFrameException("$identifier is invalid frame")
		} else {
			logger.fine(
				"Frame Size Is:$frameSize"
			)
			//Convert v2.2 to v2.4 id just for reading the data
			var id = convertFrameID22To24(identifier)
			if (id == null) {
				//OK,it may be convertable to a v.3 id even though not valid v.4
				id = convertFrameID22To23(identifier)
				if (id == null) {
					// Is it a valid v22 identifier so should be able to find a
					// frame body for it.
					id =
						if (isID3v22FrameIdentifier(
								identifier
							)
						) {
							identifier
						} else {
							UNSUPPORTED_ID
						}
				}
			}
			logger.fine(
				"Identifier was:$identifier reading using:$id"
			)

			//Create Buffer that only contains the body of this frame rather than the remainder of tag
			val frameBodyBuffer = byteBuffer.slice()
			frameBodyBuffer.limit(frameSize)
			frameBody = try {
				readBody(id, frameBodyBuffer, frameSize)
			} finally {
				//Update position of main buffer, so no attempt is made to reread these bytes
				byteBuffer.position(byteBuffer.position() + frameSize)
			}
		}
	}

	/**
	 * Read Frame Size, which has to be decoded
	 * @param buffer
	 * @return
	 */
	private fun decodeSize(buffer: ByteArray): Int {
		val bi = BigInteger(buffer)
		val tmpSize = bi.toInt()
		if (tmpSize < 0) {
			logger.warning(
				"Invalid Frame Size of:" + tmpSize + "Decoded from bin:" + Integer.toBinaryString(
					tmpSize
				) + "Decoded from hex:" + Integer.toHexString(tmpSize)
			)
		}
		return tmpSize
	}

	/**
	 * Write Frame raw data
	 *
	 */
	override fun write(tagBuffer: ByteArrayOutputStream?) {
		logger.config(
			"Write Frame to Buffer$identifier"
		)
		//This is where we will write header, move position to where we can
		//write body
		val headerBuffer = ByteBuffer.allocate(this.frameHeaderSize)

		//Write Frame Body Data
		val bodyOutputStream = ByteArrayOutputStream()
		(frameBody as AbstractID3v2FrameBody).write(bodyOutputStream)

		//Write Frame Header
		//Write Frame ID must adjust can only be 3 bytes long
		headerBuffer.put(identifier.toByteArray(StandardCharsets.ISO_8859_1), 0, this.frameIdSize)
		encodeSize(headerBuffer, frameBody!!.size)

		//Add header to the Byte Array Output Stream
		try {
			tagBuffer!!.write(headerBuffer.array())

			//Add body to the Byte Array Output Stream
			tagBuffer.write(bodyOutputStream.toByteArray())
		} catch (ioe: IOException) {
			//This could never happen coz not writing to file, so convert to RuntimeException
			throw RuntimeException(ioe)
		}
	}

	/**
	 * Write Frame Size (can now be accurately calculated, have to convert 4 byte int
	 * to 3 byte format.
	 * @param headerBuffer
	 * @param size
	 */
	private fun encodeSize(headerBuffer: ByteBuffer, size: Int) {
		headerBuffer.put((size and 0x00FF0000 shr 16).toByte())
		headerBuffer.put((size and 0x0000FF00 shr 8).toByte())
		headerBuffer.put((size and 0x000000FF).toByte())
		logger.fine(
			"Frame Size Is Actual:" + size + ":Encoded bin:" + Integer.toBinaryString(size) + ":Encoded Hex" + Integer.toHexString(
				size
			)
		)
	}

	/**
	 * Does the frame identifier meet the syntax for a idv3v2 frame identifier.
	 * must start with a capital letter and only contain capital letters and numbers
	 *
	 * @param identifier
	 * @return
	 */
	fun isValidID3v2FrameIdentifier(identifier: String?): Boolean {
		val m = validFrameIdentifier.matcher(identifier)
		return m.matches()
	}

	/**
	 * Return String Representation of body
	 */
	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_FRAME, identifier)
		MP3File.structureFormatter?.addElement(TYPE_FRAME_SIZE, frameSize)
		frameBody!!.createStructure()
		MP3File.structureFormatter?.closeHeadingElement(TYPE_FRAME)
	}

	/**
	 * @return true if considered a common frame
	 */
	override val isCommon: Boolean
		get() = ID3v22Frames.instanceOf.isCommon(id)

	/**
	 * @return true if considered a common frame
	 */
	override val isBinary: Boolean
		get() = ID3v22Frames.instanceOf.isBinary(id)

	/**
	 * Sets the charset encoding used by the field.
	 *
	 * @param encoding charset.
	 */
	override var encoding: Charset?
		get() = super.encoding
		set(encoding) {
			val encodingId = TextEncoding.instanceOf.getIdForCharset(encoding)
			if (encodingId != null) {
				if (encodingId < 2) {
					body!!.textEncoding = encodingId.toByte()
				}
			}
		}

	companion object {
		private val validFrameIdentifier = Pattern.compile("[A-Z][0-9A-Z]{2}")
		private val FRAME_ID_SIZE = 3
		private val FRAME_SIZE_SIZE = 3
		val FRAME_HEADER_SIZE = FRAME_ID_SIZE + FRAME_SIZE_SIZE
	}
}

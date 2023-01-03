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

import com.flammky.musicplayer.common.media.audio.meta_tag.FileConstants
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.EmptyFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidFrameIdentifierException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Compression.uncompress
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID22To23
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID23To24
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID24To23
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.copyObject
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.forceFrameID22To23
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.forceFrameID24To23
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.isID3v22FrameIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.isID3v23FrameIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.isID3v24FrameIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.AbstractID3v2FrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyDeprecated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyUnsupported
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.ID3v23FrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import kotlin.experimental.inv

/**
 * Represents an ID3v2.3 frame.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class ID3v23Frame : AbstractID3v2Frame {


	override val frameIdSize: Int
		get() = FRAME_ID_SIZE
	override val frameSizeSize: Int
		get() = FRAME_SIZE_SIZE
	override val frameHeaderSize: Int
		get() = FRAME_HEADER_SIZE

	/**
	 * If the frame is encrypted then the encryption method is stored in this byte
	 */
	var encryptionMethod = 0
		private set

	/**
	 * If the frame belongs in a group with other frames then the group identifier byte is stored
	 */
	var groupIdentifier = 0
		private set

	/**
	 * Creates a new ID3v23 Frame
	 */
	constructor()

	/**
	 * Creates a new ID3v23 Frame of type identifier.
	 *
	 *
	 * An empty body of the correct type will be automatically created.
	 * This constructor should be used when wish to create a new
	 * frame from scratch using user data.
	 * @param identifier
	 */
	constructor(identifier: String?) : super(identifier!!) {
		statusFlags = StatusFlags()
		encodingFlags = EncodingFlags()
	}

	/**
	 * Copy Constructor
	 *
	 * Creates a new v23 frame  based on another v23 frame
	 * @param frame
	 */
	constructor(frame: ID3v23Frame) : super(frame) {
		statusFlags = StatusFlags(frame.statusFlags?.originalFlags ?: 0)
		encodingFlags = EncodingFlags(frame.encodingFlags?.flags ?: 0)
	}

	/**
	 * Partially construct ID3v24 Frame form an IS3v23Frame
	 *
	 * Used for Special Cases
	 *
	 * @param frame
	 * @param identifier
	 * @throws InvalidFrameException
	 */
	constructor(frame: ID3v24Frame, identifier: String?) {
		this.identifier = identifier!!
		statusFlags = StatusFlags(frame.statusFlags as ID3v24Frame.StatusFlags)
		encodingFlags = EncodingFlags(frame.encodingFlags!!.flags)
	}

	/**
	 * Creates a new ID3v23Frame  based on another frame of a different version.
	 *
	 * @param frame
	 * @throws InvalidFrameException
	 */
	constructor(frame: AbstractID3v2Frame) {
		logger.finer("Creating frame from a frame of a different version")
		if (frame is ID3v23Frame) {
			throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
		} else if (frame is ID3v22Frame) {
			statusFlags = StatusFlags()
			encodingFlags = EncodingFlags()
		} else if (frame is ID3v24Frame) {
			statusFlags = StatusFlags(frame.statusFlags as ID3v24Frame.StatusFlags)
			encodingFlags = EncodingFlags(frame.encodingFlags!!.flags)
		}
		if (frame is ID3v24Frame) {
			//Unknown Frame e.g NCON, also protects when known id but has unsupported frame body
			if (frame.body is FrameBodyUnsupported) {
				frameBody = FrameBodyUnsupported(frame.body as FrameBodyUnsupported)
				(frameBody as FrameBodyUnsupported).header = this
				identifier = frame.identifier
				logger.config("UNKNOWN:Orig id is:" + frame.identifier + ":New id is:" + identifier)
				return
			} else if (frame.body is FrameBodyDeprecated) {
				//Was it valid for this tag version, if so try and reconstruct
				if (isID3v23FrameIdentifier(frame.identifier)) {
					frameBody = (frame.body as FrameBodyDeprecated?)!!.originalFrameBody
					frameBody!!.header = this
					frameBody!!.textEncoding = getTextEncoding(this, frameBody!!.textEncoding)
					identifier = frame.identifier
					logger.config("DEPRECATED:Orig id is:" + frame.identifier + ":New id is:" + identifier)
				} else {
					frameBody = FrameBodyDeprecated(frame.body as FrameBodyDeprecated?)
					frameBody!!.header = this
					frameBody!!.textEncoding = getTextEncoding(this, frameBody!!.textEncoding)
					identifier = frame.identifier
					logger.config("DEPRECATED:Orig id is:" + frame.identifier + ":New id is:" + identifier)
					return
				}
			} else if (isID3v24FrameIdentifier(frame.identifier)) {
				logger.finer("isID3v24FrameIdentifier")
				//Version between v4 and v3
				identifier = convertFrameID24To23(frame.identifier).toString()
				if (identifier != null) {
					logger.finer("V4:Orig id is:" + frame.identifier + ":New id is:" + identifier)
					frameBody = copyObject(frame.body) as AbstractTagFrameBody?
					frameBody!!.header = this
					frameBody!!.textEncoding = getTextEncoding(this, frameBody!!.textEncoding)
					return
				} else {
					//Is it a known v4 frame which needs forcing to v3 frame e.g. TDRC - TYER,TDAT
					identifier = forceFrameID24To23(frame.identifier).toString()
					if (identifier != null) {
						logger.finer("V4:Orig id is:" + frame.identifier + ":New id is:" + identifier)
						frameBody =
							this.readBody(identifier, (frame.body as AbstractID3v2FrameBody?)!!)
						frameBody!!.header = this
						frameBody!!.textEncoding = getTextEncoding(this, frameBody!!.textEncoding)
						return
					} else {
						val baos = ByteArrayOutputStream()
						(frame.body as AbstractID3v2FrameBody?)!!.write(baos)
						identifier = frame.identifier
						frameBody = FrameBodyUnsupported(identifier, baos.toByteArray())
						frameBody!!.header = this
						logger.finer("V4:Orig id is:" + frame.identifier + ":New Id Unsupported is:" + identifier)
						return
					}
				}
			} else {
				logger.severe("Orig id is:" + frame.identifier + ":Unable to create Frame Body")
				throw InvalidFrameException("Orig id is:" + frame.identifier + "Unable to create Frame Body")
			}
		} else if (frame is ID3v22Frame) {
			if (isID3v22FrameIdentifier(frame.identifier)) {
				identifier = convertFrameID22To23(frame.identifier).toString()
				if (identifier != null) {
					logger.config("V3:Orig id is:" + frame.identifier + ":New id is:" + identifier)
					frameBody = copyObject(frame.body) as AbstractTagFrameBody?
					frameBody!!.header = this
					return
				} else if (isID3v22FrameIdentifier(frame.identifier)) {
					//Force v2 to v3
					identifier = forceFrameID22To23(frame.identifier).toString()
					if (identifier != null) {
						logger.config("V22Orig id is:" + frame.identifier + "New id is:" + identifier)
						frameBody =
							this.readBody(identifier, (frame.body as AbstractID3v2FrameBody?)!!)
						frameBody!!.header = this
						return
					} else {
						frameBody = FrameBodyDeprecated(frame.body as AbstractID3v2FrameBody?)
						frameBody!!.header = this
						identifier = frame.identifier
						logger.config("Deprecated:V22:orig id id is:" + frame.identifier + ":New id is:" + identifier)
						return
					}
				}
			} else {
				frameBody = FrameBodyUnsupported(frame.body as FrameBodyUnsupported)
				frameBody!!.header = this
				identifier = frame.identifier
				logger.config("UNKNOWN:Orig id is:" + frame.identifier + ":New id is:" + identifier)
				return
			}
		}
		logger.warning("Frame is unknown version:" + frame.javaClass)
	}

	/**
	 * Creates a new ID3v23Frame dataType by reading from byteBuffer.
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
	 * Creates a new ID3v23Frame dataType by reading from byteBuffer.
	 *
	 * @param byteBuffer to read from
	 * @throws InvalidFrameException
	 */
	@Deprecated(
		"""use {@link #ID3v23Frame(ByteBuffer,String)} instead
      """
	)
	constructor(byteBuffer: ByteBuffer) : this(byteBuffer, "")

	/**
	 * Return size of frame
	 *
	 * @return int frame size
	 */
	override val size: Int
		get() = frameBody!!.size + Companion.FRAME_HEADER_SIZE

	/**
	 * Compare for equality
	 * To be deemed equal obj must be a IDv23Frame with the same identifier
	 * and the same flags.
	 * containing the same body,dataType list ectera.
	 * equals() method is made up from all the various components
	 *
	 * @param obj
	 * @return if true if this object is equivalent to obj
	 */
	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		if (obj !is ID3v23Frame) {
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
	 * Read the frame from a byteBuffer
	 *
	 * @param byteBuffer buffer to read from
	 */
	@Throws(InvalidFrameException::class, InvalidDataTypeException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val identifier = readIdentifier(byteBuffer)
		if (!isValidID3v2FrameIdentifier(identifier)) {
			logger.config(
				"$loggingFilename:Invalid identifier:$identifier"
			)
			byteBuffer.position(byteBuffer.position() - (this.frameIdSize - 1))
			throw InvalidFrameIdentifierException(
				"$loggingFilename:$identifier:is not a valid ID3v2.30 frame"
			)
		}
		//Read the size field (as Big Endian Int - byte buffers always initialised to Big Endian order)
		frameSize = byteBuffer.int
		if (frameSize < 0) {
			logger.warning(
				"$loggingFilename:Invalid Frame Size:$frameSize:$identifier"
			)
			throw InvalidFrameException("$identifier is invalid frame:$frameSize")
		} else if (frameSize == 0) {
			logger.warning(
				"$loggingFilename:Empty Frame Size:$identifier"
			)
			//We don't process this frame or add to frameMap because contains no useful information
			//Skip the two flag bytes so in correct position for subsequent frames
			byteBuffer.get()
			byteBuffer.get()
			throw EmptyFrameException("$identifier is empty frame")
		} else if (frameSize + 2 > byteBuffer.remaining()) {
			logger.warning(loggingFilename + ":Invalid Frame size of " + frameSize + " larger than size of" + byteBuffer.remaining() + " before mp3 audio:" + identifier)
			throw InvalidFrameException(identifier + " is invalid frame:" + frameSize + " larger than size of" + byteBuffer.remaining() + " before mp3 audio:" + identifier)
		}

		//Read the flag bytes
		statusFlags = StatusFlags(
			byteBuffer.get()
		)
		encodingFlags = EncodingFlags(
			byteBuffer.get()
		)
		var id: String?

		//If this identifier is a valid v24 identifier or easily converted to v24
		id = convertFrameID23To24(identifier)
		// Cant easily be converted to v24 but is it a valid v23 identifier
		if (id == null) {
			// If it is a valid v23 identifier so should be able to find a
			//  frame body for it.
			id =
				if (isID3v23FrameIdentifier(
						identifier
					)
				) {
					identifier
				} else {
					UNSUPPORTED_ID
				}
		}

		//Read extra bits appended to frame header for various encodings
		//These are not included in header size but are included in frame size but won't be read when we actually
		//try to read the frame body data
		var extraHeaderBytesCount = 0
		var decompressedFrameSize = -1
		if ((encodingFlags as EncodingFlags).isCompression) {
			//Read the Decompressed Size
			decompressedFrameSize = byteBuffer.int
			extraHeaderBytesCount = FRAME_COMPRESSION_UNCOMPRESSED_SIZE
			logger.fine(
				"$loggingFilename:Decompressed frame size is:$decompressedFrameSize"
			)
		}
		if ((encodingFlags as EncodingFlags).isEncryption) {
			//Consume the encryption byte
			extraHeaderBytesCount += FRAME_ENCRYPTION_INDICATOR_SIZE
			encryptionMethod = byteBuffer.get().toInt()
		}
		if ((encodingFlags as EncodingFlags).isGrouping) {
			//Read the Grouping byte, but do nothing with it
			extraHeaderBytesCount += FRAME_GROUPING_INDICATOR_SIZE
			groupIdentifier = byteBuffer.get().toInt()
		}
		if ((encodingFlags as EncodingFlags).isNonStandardFlags) {
			//Probably corrupt so treat as a standard frame
			logger.severe(loggingFilename + ":InvalidEncodingFlags:" + Hex.asHex((encodingFlags as EncodingFlags).flags))
		}
		if ((encodingFlags as EncodingFlags).isCompression) {
			if (decompressedFrameSize > 100 * frameSize) {
				throw InvalidFrameException(
					"$identifier is invalid frame, frame size $frameSize cannot be:$decompressedFrameSize when uncompressed"
				)
			}
		}

		//Work out the real size of the frameBody data
		val realFrameSize = frameSize - extraHeaderBytesCount
		if (realFrameSize <= 0) {
			throw InvalidFrameException("$identifier is invalid frame, realframeSize is:$realFrameSize")
		}
		val frameBodyBuffer: ByteBuffer
		//Read the body data
		try {
			if ((encodingFlags as EncodingFlags).isCompression) {
				frameBodyBuffer = uncompress(
					identifier,
					loggingFilename,
					byteBuffer,
					decompressedFrameSize,
					realFrameSize
				)
				frameBody =
					if ((encodingFlags as EncodingFlags).isEncryption) {
						readEncryptedBody(id, frameBodyBuffer, decompressedFrameSize)
					} else {
						readBody(id, frameBodyBuffer, decompressedFrameSize)
					}
			} else if ((encodingFlags as EncodingFlags).isEncryption) {
				if (byteBuffer.remaining() >= frameSize) {
					frameBodyBuffer = byteBuffer.slice()
					frameBodyBuffer.limit(frameSize)
					frameBody = readEncryptedBody(identifier, frameBodyBuffer, frameSize)
				} else {
					logger.warning(loggingFilename + ":Invalid Frame " + frameSize + " encodingFlagSetButNotEnoughBytes:" + byteBuffer.remaining() + " before mp3 audio:" + identifier)
					throw InvalidFrameException(identifier + " invalid frame:" + frameSize + "  encodingFlagSetButNotEnoughBytes:" + byteBuffer.remaining() + " before mp3 audio:" + identifier)
				}
			} else {
				//Create Buffer that only contains the body of this frame rather than the remainder of tag
				frameBodyBuffer = byteBuffer.slice()
				frameBodyBuffer.limit(realFrameSize)
				frameBody = readBody(id, frameBodyBuffer, realFrameSize)
			}
			//TODO code seems to assume that if the frame created is not a v23FrameBody
			//it should be deprecated, but what about if somehow a V24Frame has been put into a V23 Tag, shouldn't
			//it then be created as FrameBodyUnsupported
			if (frameBody !is ID3v23FrameBody) {
				logger.config(
					"$loggingFilename:Converted frameBody with:$identifier to deprecated frameBody"
				)
				frameBody = FrameBodyDeprecated(frameBody as AbstractID3v2FrameBody?)
			}
		} finally {
			//Update position of main buffer, so no attempt is made to re-read these bytes
			byteBuffer.position(byteBuffer.position() + realFrameSize)
		}
	}

	/**
	 * Write the frame to bufferOutputStream
	 *
	 */
	override fun write(tagBuffer: ByteArrayOutputStream?) {
		logger.config(
			"Writing frame to buffer:$identifier"
		)
		//This is where we will write header, move position to where we can
		//write body
		val headerBuffer = ByteBuffer.allocate(Companion.FRAME_HEADER_SIZE)

		//Write Frame Body Data
		val bodyOutputStream = ByteArrayOutputStream()
		(frameBody as AbstractID3v2FrameBody).write(bodyOutputStream)
		//Write Frame Header write Frame ID
		if (identifier.length == 3) {
			identifier = "$identifier "
		}
		headerBuffer.put(
			identifier.toByteArray(StandardCharsets.ISO_8859_1),
			0,
			Companion.FRAME_ID_SIZE
		)
		//Write Frame Size
		val size = frameBody!!.size
		logger.fine("Frame Size Is:$size")
		headerBuffer.putInt(frameBody!!.size)

		//Write the Flags
		//Status Flags:leave as they were when we read
		headerBuffer.put(statusFlags!!.writeFlags)

		//Remove any non standard flags
		(encodingFlags as EncodingFlags).unsetNonStandardFlags()

		//Unset Compression flag if previously set because we uncompress previously compressed frames on write.
		(encodingFlags as EncodingFlags).unsetCompression()
		headerBuffer.put(encodingFlags!!.flags)
		try {
			//Add header to the Byte Array Output Stream
			tagBuffer!!.write(headerBuffer.array())
			if ((encodingFlags as EncodingFlags).isEncryption) {
				tagBuffer.write(encryptionMethod)
			}
			if ((encodingFlags as EncodingFlags).isGrouping) {
				tagBuffer.write(groupIdentifier)
			}

			//Add body to the Byte Array Output Stream
			tagBuffer.write(bodyOutputStream.toByteArray())
		} catch (ioe: IOException) {
			//This could never happen coz not writing to file, so convert to RuntimeException
			throw RuntimeException(ioe)
		}
	}

	override var statusFlags: AbstractID3v2Frame.StatusFlags? = super.statusFlags
	override var encodingFlags: AbstractID3v2Frame.EncodingFlags? = super.encodingFlags

	/**
	 * This represents a frame headers Status Flags
	 * Make adjustments if necessary based on frame type and specification.
	 */
	inner class StatusFlags : AbstractID3v2Frame.StatusFlags {
		constructor() {
			originalFlags = 0.toByte()
			writeFlags = 0.toByte()
		}

		constructor(flags: Byte) {
			originalFlags = flags
			writeFlags = flags
			modifyFlags()
		}

		/**
		 * Use this constructor when convert a v24 frame
		 * @param statusFlags
		 */
		constructor(statusFlags: ID3v24Frame.StatusFlags) {
			originalFlags = convertV4ToV3Flags(statusFlags.originalFlags)
			writeFlags = originalFlags
			modifyFlags()
		}

		private fun convertV4ToV3Flags(v4Flag: Byte): Byte {
			var v3Flag = 0.toByte()
			if (v4Flag.toInt() and ID3v24Frame.MASK_TAG_ALTER_PRESERVATION != 0) {
				v3Flag = (v3Flag.toInt() or MASK_FILE_ALTER_PRESERVATION.toByte()
					.toInt()).toByte()
			}
			if (v4Flag.toInt() and ID3v24Frame.MASK_TAG_ALTER_PRESERVATION != 0) {
				v3Flag = (v3Flag.toInt() or MASK_TAG_ALTER_PRESERVATION.toByte()
					.toInt()).toByte()
			}
			return v3Flag
		}


		/** [org.jaudiotagger.tag.id3.ID3v23Frame] */
		protected fun modifyFlags() {
			val str = identifier
			if (ID3v23Frames.instanceOf.isDiscardIfFileAltered(str)) {
				writeFlags = (writeFlags.toInt() or MASK_FILE_ALTER_PRESERVATION.toByte().toInt()).toByte()
				writeFlags =
					(writeFlags.toInt() and MASK_TAG_ALTER_PRESERVATION.toByte().inv().toInt()).toByte()
			} else {
				writeFlags = (writeFlags.toInt() and MASK_FILE_ALTER_PRESERVATION.toByte()
					.inv().toInt()).toByte()
				writeFlags = (writeFlags.toInt() and MASK_TAG_ALTER_PRESERVATION.toByte()
					.inv().toInt()).toByte()
			}
		}

		override fun createStructure() {
			MP3File.structureFormatter?.openHeadingElement(TYPE_FLAGS, "")
			MP3File.structureFormatter?.addElement(
				TYPE_TAGALTERPRESERVATION,
				originalFlags.toInt() and MASK_TAG_ALTER_PRESERVATION
			)
			MP3File.structureFormatter?.addElement(
				TYPE_FILEALTERPRESERVATION,
				originalFlags.toInt() and MASK_FILE_ALTER_PRESERVATION
			)
			MP3File.structureFormatter?.addElement(
				TYPE_READONLY,
				originalFlags.toInt() and MASK_READ_ONLY
			)
			MP3File.structureFormatter?.closeHeadingElement(TYPE_FLAGS)
		}
	}

	/**
	 * This represents a frame headers Encoding Flags
	 */
	inner class EncodingFlags : AbstractID3v2Frame.EncodingFlags {
		constructor() : super()
		constructor(flags: Byte) : super(flags) {
			logEnabledFlags()
		}

		fun setCompression() {
			flags = (flags.toInt() or MASK_COMPRESSION).toByte()
		}

		fun setEncryption() {
			flags = (flags.toInt() or MASK_ENCRYPTION).toByte()
		}

		fun setGrouping() {
			flags = (flags.toInt() or MASK_GROUPING_IDENTITY).toByte()
		}

		fun unsetCompression() {
			flags = (flags.toInt() and MASK_COMPRESSION.toByte().inv().toInt()).toByte()
		}

		fun unsetEncryption() {
			flags = (flags.toInt() and MASK_ENCRYPTION.toByte().inv().toInt()).toByte()
		}

		fun unsetGrouping() {
			flags = (flags.toInt() and MASK_GROUPING_IDENTITY.toByte().inv().toInt()).toByte()
		}

		val isNonStandardFlags: Boolean
			get() = flags.toInt() and FileConstants.BIT4 > 0
				|| flags.toInt() and FileConstants.BIT3 > 0
				|| flags.toInt() and FileConstants.BIT2 > 0
				|| flags.toInt() and FileConstants.BIT1 > 0
				|| flags.toInt() and FileConstants.BIT0 > 0

		fun unsetNonStandardFlags() {
			if (isNonStandardFlags) {
				logger.warning(
					loggingFilename + ":" + identifier + ":Unsetting Unknown Encoding Flags:" + Hex.asHex(
						flags
					)
				)
				flags = (flags.toInt() and FileConstants.BIT4.toByte()
					.inv().toInt()).toByte()
				flags = (flags.toInt() and FileConstants.BIT3.toByte()
					.inv().toInt()).toByte()
				flags = (flags.toInt() and FileConstants.BIT2.toByte()
					.inv().toInt()).toByte()
				flags = (flags.toInt() and FileConstants.BIT1.toByte()
					.inv().toInt()).toByte()
				flags = (flags.toInt() and FileConstants.BIT0.toByte()
					.inv().toInt()).toByte()
			}
		}

		fun logEnabledFlags() {
			if (isNonStandardFlags) {
				logger.warning(
					loggingFilename + ":" + identifier + ":Unknown Encoding Flags:" + Hex.asHex(
						flags
					)
				)
			}
			if (isCompression) {
				logger.warning(
					"$loggingFilename:$identifier is compressed"
				)
			}
			if (isEncryption) {
				logger.warning(
					"$loggingFilename:$identifier is encrypted"
				)
			}
			if (isGrouping) {
				logger.warning(
					"$loggingFilename:$identifier is grouped"
				)
			}
		}

		val isCompression: Boolean
			get() = flags.toInt() and MASK_COMPRESSION > 0
		val isEncryption: Boolean
			get() = flags.toInt() and MASK_ENCRYPTION > 0
		val isGrouping: Boolean
			get() = flags.toInt() and MASK_GROUPING_IDENTITY > 0

		override fun createStructure() {
			MP3File.structureFormatter?.openHeadingElement(TYPE_FLAGS, "")
			MP3File.structureFormatter?.addElement(TYPE_COMPRESSION, flags.toInt() and MASK_COMPRESSION)
			MP3File.structureFormatter?.addElement(TYPE_ENCRYPTION, flags.toInt() and MASK_ENCRYPTION)
			MP3File.structureFormatter?.addElement(
				TYPE_GROUPIDENTITY,
				flags.toInt() and MASK_GROUPING_IDENTITY
			)
			MP3File.structureFormatter?.closeHeadingElement(TYPE_FLAGS)
		}
	}

	/**
	 * Does the frame identifier meet the syntax for a idv3v2 frame identifier.
	 * must start with a capital letter and only contain capital letters and numbers
	 *
	 * @param identifier to be checked
	 * @return whether the identifier is valid
	 */
	fun isValidID3v2FrameIdentifier(identifier: String): Boolean {
		val m = validFrameIdentifier.matcher(identifier)
		return m.matches()
	}

	/**
	 * Return String Representation of body
	 */
	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_FRAME, identifier)
		MP3File.structureFormatter?.addElement(TYPE_FRAME_SIZE, frameSize)
		statusFlags?.createStructure()
		encodingFlags?.createStructure()
		frameBody?.createStructure()
		MP3File.structureFormatter?.closeHeadingElement(TYPE_FRAME)
	}

	/**
	 * @return true if considered a common frame
	 */
	override val isCommon: Boolean
		get() = ID3v23Frames.instanceOf.isCommon(id)

	/**
	 * @return true if considered a common frame
	 */
	override val isBinary: Boolean
		get() = ID3v23Frames.instanceOf.isBinary(id)

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
		private val validFrameIdentifier = Pattern.compile("[A-Z][0-9A-Z]{3}")

		@JvmStatic
		protected val FRAME_ID_SIZE = 4

		@JvmStatic
		protected val FRAME_FLAGS_SIZE = 2

		@JvmStatic
		protected val FRAME_SIZE_SIZE = 4

		@JvmStatic
		protected val FRAME_COMPRESSION_UNCOMPRESSED_SIZE = 4

		@JvmStatic
		protected val FRAME_ENCRYPTION_INDICATOR_SIZE = 1

		@JvmStatic
		protected val FRAME_GROUPING_INDICATOR_SIZE = 1

		val FRAME_HEADER_SIZE = FRAME_ID_SIZE + FRAME_SIZE_SIZE + FRAME_FLAGS_SIZE

		const val TYPE_TAGALTERPRESERVATION = "typeTagAlterPreservation"
		const val TYPE_FILEALTERPRESERVATION = "typeFileAlterPreservation"
		const val TYPE_READONLY = "typeReadOnly"

		/**
		 * Discard frame if tag altered
		 */
		const val MASK_TAG_ALTER_PRESERVATION = FileConstants.BIT7

		/**
		 * Discard frame if audio file part altered
		 */
		const val MASK_FILE_ALTER_PRESERVATION = FileConstants.BIT6

		/**
		 * Frame tagged as read only
		 */
		const val MASK_READ_ONLY = FileConstants.BIT5

		const val TYPE_COMPRESSION = "compression"
		const val TYPE_ENCRYPTION = "encryption"
		const val TYPE_GROUPIDENTITY = "groupidentity"

		/**
		 * Frame is compressed
		 */
		const val MASK_COMPRESSION = FileConstants.BIT7

		/**
		 * Frame is encrypted
		 */
		const val MASK_ENCRYPTION = FileConstants.BIT6

		/**
		 * Frame is part of a group
		 */
		const val MASK_GROUPING_IDENTITY = FileConstants.BIT5
	}
}

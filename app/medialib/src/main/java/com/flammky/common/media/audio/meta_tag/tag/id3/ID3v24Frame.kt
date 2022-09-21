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
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.Lyrics3Line
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Compression.uncompress
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.bufferToValue
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.isBufferEmpty
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.isBufferNotSyncSafe
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.valueToBuffer
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.convertFrameID23To24
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.copyObject
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.forceFrameID23To24
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.isID3v23FrameIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Unsynchronization.requiresUnsynchronization
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Unsynchronization.synchronize
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Unsynchronization.unsynchronize
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3.*
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import kotlin.experimental.inv

/**
 * Represents an ID3v2.4 frame.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class ID3v24Frame : AbstractID3v2Frame {

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

	constructor()

	/**
	 * Creates a new ID3v2_4Frame of type identifier. An empty
	 * body of the correct type will be automatically created.
	 * This constructor should be used when wish to create a new
	 * frame from scratch using user input
	 *
	 * @param identifier defines the type of body to be created
	 */
	constructor(identifier: String?) : super(identifier!!) {
		//Super Constructor creates a frame with empty body of type specified
		statusFlags = StatusFlags()
		encodingFlags = EncodingFlags()
	}

	/**
	 * Copy Constructor:Creates a new ID3v24 frame datatype based on another frame.
	 *
	 * @param frame
	 */
	constructor(frame: ID3v24Frame) : super(frame) {
		statusFlags = StatusFlags(frame.statusFlags!!.originalFlags)
		encodingFlags = EncodingFlags(frame.encodingFlags!!.flags)
	}

	@Throws(InvalidFrameException::class)
	private fun createV24FrameFromV23Frame(frame: ID3v23Frame) {
		// Is it a straight conversion e.g TALB - TALB
		identifier = convertFrameID23To24(frame.identifier).toString()
		logger.finer("Creating V24frame from v23:" + frame.identifier + ":" + identifier)


		//We cant convert unsupported bodies properly
		if (frame.body is FrameBodyUnsupported) {
			frameBody = FrameBodyUnsupported(frame.body as FrameBodyUnsupported)
			frameBody!!.header = this
			identifier = frame.identifier
			logger.finer("V3:UnsupportedBody:Orig id is:" + frame.identifier + ":New id is:" + identifier)
		} //Simple Copy
		else if (identifier != null) {
			//Special Case
			if (frame.identifier == ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO && (frame.body as FrameBodyTXXX?)!!.description == FrameBodyTXXX.MOOD) {
				frameBody = FrameBodyTMOO(frame.body as FrameBodyTXXX)
				frameBody!!.header = this
				identifier = frameBody!!.identifier!!
			} else {
				logger.finer("V3:Orig id is:" + frame.identifier + ":New id is:" + identifier)
				frameBody = copyObject(frame.body) as AbstractTagFrameBody?
				frameBody!!.header = this
			}
		} else if (isID3v23FrameIdentifier(frame.identifier)) {
			identifier = forceFrameID23To24(frame.identifier)!!
			if (identifier != null) {
				logger.config("V3:Orig id is:" + frame.identifier + ":New id is:" + identifier)
				frameBody = this.readBody(identifier, (frame.body as AbstractID3v2FrameBody?)!!)
				frameBody!!.header = this
			} else {
				frameBody = FrameBodyDeprecated(frame.body as AbstractID3v2FrameBody?)
				frameBody!!.header = this
				identifier = frame.identifier
				logger.finer("V3:Deprecated:Orig id is:" + frame.identifier + ":New id is:" + identifier)
			}
		} else {
			if (frame.body is FrameBodyUnsupported) {
				frameBody = FrameBodyUnsupported(frame.body as FrameBodyUnsupported)
				frameBody!!.header = this
				identifier = frame.identifier
				logger.finer("V3:Unknown:Orig id is:" + frame.identifier + ":New id is:" + identifier)
			} else if (frame.body is FrameBodyDeprecated) {
				frameBody = FrameBodyDeprecated(frame.body as FrameBodyDeprecated?)
				frameBody!!.header = this
				identifier = frame.identifier
				logger.finer("V3:Deprecated:Orig id is:" + frame.identifier + ":New id is:" + identifier)
			}
		}
	}

	/**
	 * Partially construct ID3v24 Frame form an IS3v23Frame
	 *
	 *
	 * Used for Special Cases
	 *
	 * @param frame
	 * @param identifier
	 * @throws InvalidFrameException
	 */
	constructor(frame: ID3v23Frame, identifier: String?) {
		this.identifier = identifier!!
		statusFlags = StatusFlags(frame.statusFlags as ID3v23Frame.StatusFlags)
		encodingFlags = EncodingFlags(frame.encodingFlags!!.flags)
	}

	/**
	 * Creates a new ID3v24 frame datatype based on another frame of different version
	 * Converts the framebody to the equivalent v24 framebody or to UnsupportedFrameBody if identifier
	 * is unknown.
	 *
	 * @param frame to construct a new frame from
	 * @throws InvalidFrameException
	 */
	constructor(frame: AbstractID3v2Frame) {
		//Should not be called
		if (frame is ID3v24Frame) {
			throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
		} else if (frame is ID3v23Frame) {
			statusFlags = StatusFlags(frame.statusFlags as ID3v23Frame.StatusFlags)
			encodingFlags = EncodingFlags(frame.encodingFlags!!.flags)
		} else if (frame is ID3v22Frame) {
			statusFlags = StatusFlags()
			encodingFlags = EncodingFlags()
		}

		// Convert Identifier. If the id was a known id for the original
		// version we should be able to convert it to an v24 frame, although it may mean minor
		// modification to the data. If it was not recognised originally it should remain
		// unknown.
		if (frame is ID3v23Frame) {
			createV24FrameFromV23Frame(frame)
		} else if (frame is ID3v22Frame) {
			val v23Frame = ID3v23Frame(frame)
			createV24FrameFromV23Frame(v23Frame)
		}
		frameBody!!.header = this
	}

	/**
	 * Creates a new ID3v2_4Frame datatype based on Lyrics3.
	 *
	 * @param field
	 * @throws InvalidTagException
	 */
	constructor(field: Lyrics3v2Field) {
		val id: String = field.identifier
		val value: String?
		if (id == "IND") {
			throw InvalidTagException("Cannot create ID3v2.40 frame from Lyrics3 indications field.")
		} else if (id == "LYR") {
			val lyric = field.body as FieldFrameBodyLYR?
			var line: Lyrics3Line
			val iterator = lyric!!.iterator()
			val sync: FrameBodySYLT
			val unsync: FrameBodyUSLT
			val hasTimeStamp = lyric.hasTimeStamp()
			// we'll create only one frame here.
			// if there is any timestamp at all, we will create a sync'ed frame.
			sync = FrameBodySYLT(
				0.toByte().toInt(),
				"ENG",
				2.toByte().toInt(),
				1.toByte().toInt(),
				"",
				ByteArray(0)
			)
			unsync = FrameBodyUSLT(0.toByte(), "ENG", "", "")
			while (iterator!!.hasNext()) {
				line = iterator.next()!!
				if (hasTimeStamp) {
					// sync.addLyric(line);
				} else {
					unsync.addLyric(line)
				}
			}
			if (hasTimeStamp) {
				frameBody = sync
				frameBody!!.header = this
			} else {
				frameBody = unsync
				frameBody!!.header = this
			}
		} else if (id == "INF") {
			value = (field.body as FieldFrameBodyINF).additionalInformation
			frameBody = FrameBodyCOMM(0.toByte(), "ENG", "", value)
			frameBody!!.header = this
		} else if (id == "AUT") {
			value = (field.body as FieldFrameBodyAUT).author
			frameBody = FrameBodyTCOM(0.toByte(), value)
			frameBody!!.header = this
		} else if (id == "EAL") {
			value = (field.body as FieldFrameBodyEAL).album
			frameBody = FrameBodyTALB(0.toByte(), value)
			frameBody!!.header = this
		} else if (id == "EAR") {
			value = (field.body as FieldFrameBodyEAR).artist
			frameBody = FrameBodyTPE1(0.toByte(), value)
			frameBody!!.header = this
		} else if (id == "ETT") {
			value = (field.body as FieldFrameBodyETT).title
			frameBody = FrameBodyTIT2(0.toByte(), value)
			frameBody!!.header = this
		} else if (id == "IMG") {
			throw InvalidTagException("Cannot create ID3v2.40 frame from Lyrics3 image field.")
		} else {
			throw InvalidTagException("Cannot caret ID3v2.40 frame from $id Lyrics3 field")
		}
	}

	/**
	 * Creates a new ID3v24Frame datatype by reading from byteBuffer.
	 *
	 * @param byteBuffer      to read from
	 * @param loggingFilename
	 * @throws InvalidFrameException
	 */
	constructor(byteBuffer: ByteBuffer, loggingFilename: String) {
		this.loggingFilename = loggingFilename
		read(byteBuffer)
	}

	/**
	 * Creates a new ID3v24Frame datatype by reading from byteBuffer.
	 *
	 * @param byteBuffer to read from
	 * @throws InvalidFrameException
	 */
	@Deprecated("use {@link #ID3v24Frame(ByteBuffer, String)} instead")
	constructor(byteBuffer: ByteBuffer) : this(byteBuffer, "")

	/**
	 * @param obj
	 * @return if obj is equivalent to this frame
	 */
	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		if (obj !is ID3v24Frame) {
			return false
		}
		val that = obj
		return areEqual(
			statusFlags, that.statusFlags
		) && areEqual(
			encodingFlags, that.encodingFlags
		) && super.equals(that)
	}

	/**
	 * Return size of frame
	 *
	 * @return int frame size
	 */
	override val size: Int
		get() = frameBody!!.size + Companion.FRAME_HEADER_SIZE

	/**
	 * If frame is greater than certain size it will be decoded differently if unsynchronized to if synchronized
	 * Frames with certain byte sequences should be unsynchronized but sometimes editors do not
	 * unsynchronize them so this method checks both cases and goes with the option that fits best with the data
	 *
	 * @param byteBuffer
	 * @throws InvalidFrameException
	 */
	@Throws(InvalidFrameException::class)
	private fun checkIfFrameSizeThatIsNotSyncSafe(byteBuffer: ByteBuffer) {
		if (frameSize > ID3SyncSafeInteger.MAX_SAFE_SIZE) {
			//Set Just after size field this is where we want to be when we leave this if statement
			val currentPosition = byteBuffer.position()

			//Read as nonsync safe integer
			byteBuffer.position(currentPosition - this.frameIdSize)
			val nonSyncSafeFrameSize = byteBuffer.int

			//Is the frame size syncsafe, should always be BUT some encoders such as Itunes do not do it properly
			//so do an easy check now.
			byteBuffer.position(currentPosition - this.frameIdSize)
			val isNotSyncSafe = isBufferNotSyncSafe(
				byteBuffer
			)

			//not relative so need to move position
			byteBuffer.position(currentPosition)
			if (isNotSyncSafe) {
				logger.warning(
					"$loggingFilename:Frame size is NOT stored as a sync safe integer:$identifier"
				)

				//This will return a larger frame size so need to check against buffer size if too large then we are
				//buggered , give up
				frameSize = if (nonSyncSafeFrameSize > byteBuffer.remaining() - -FRAME_FLAG_SIZE) {
					logger.warning(
						"$loggingFilename:Invalid Frame size larger than size before mp3 audio:$identifier"
					)
					throw InvalidFrameException("$identifier is invalid frame")
				} else {
					nonSyncSafeFrameSize
				}
			} else {
				//appears to be sync safe but lets look at the bytes just after the reported end of this
				//frame to see if find a valid frame header

				//Read the Frame Identifier
				var readAheadbuffer = ByteArray(this.frameIdSize)
				byteBuffer.position(currentPosition + frameSize + FRAME_FLAG_SIZE)
				if (byteBuffer.remaining() < this.frameIdSize) {
					//There is no padding or framedata we are at end so assume syncsafe
					//reset position to just after framesize
					byteBuffer.position(currentPosition)
				} else {
					byteBuffer[readAheadbuffer, 0, this.frameIdSize]

					//reset position to just after framesize
					byteBuffer.position(currentPosition)
					var readAheadIdentifier = String(readAheadbuffer)
					if (isValidID3v2FrameIdentifier(readAheadIdentifier)) {
						//Everything ok, so continue
					} else if (isBufferEmpty(
							readAheadbuffer
						)
					) {
						//no data found so assume entered padding in which case assume it is last
						//frame and we are ok
					} else {
						//Ok lets try using a non-syncsafe integer

						//size returned will be larger so is it valid
						if (nonSyncSafeFrameSize > byteBuffer.remaining() - FRAME_FLAG_SIZE) {
							//invalid so assume syncsafe
							byteBuffer.position(currentPosition)
						} else {
							readAheadbuffer = ByteArray(this.frameIdSize)
							byteBuffer.position(currentPosition + nonSyncSafeFrameSize + FRAME_FLAG_SIZE)
							if (byteBuffer.remaining() >= this.frameIdSize) {
								byteBuffer[readAheadbuffer, 0, this.frameIdSize]
								readAheadIdentifier = String(readAheadbuffer)

								//reset position to just after framesize
								byteBuffer.position(currentPosition)

								//ok found a valid identifier using non-syncsafe so assume non-syncsafe size
								//and continue
								if (isValidID3v2FrameIdentifier(readAheadIdentifier)) {
									frameSize = nonSyncSafeFrameSize
									logger.warning(
										"$loggingFilename:Assuming frame size is NOT stored as a sync safe integer:$identifier"
									)
								} else if (isBufferEmpty(readAheadbuffer)) {
									frameSize = nonSyncSafeFrameSize
									logger.warning(
										"$loggingFilename:Assuming frame size is NOT stored as a sync safe integer:$identifier"
									)
								} else {
								}
							} else {
								//reset position to just after framesize
								byteBuffer.position(currentPosition)

								//If the unsync framesize matches exactly the remaining bytes then assume it has the
								//correct size for the last frame
								if (byteBuffer.remaining() == 0) {
									frameSize = nonSyncSafeFrameSize
								} else {
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Read the frame size form the header, check okay , if not try to fix
	 * or just throw exception
	 *
	 * @param byteBuffer
	 * @throws InvalidFrameException
	 */
	@Throws(InvalidFrameException::class)
	private fun getFrameSize(byteBuffer: ByteBuffer?) {
		//Read frame size as syncsafe integer
		frameSize = bufferToValue(
			byteBuffer!!
		)
		if (frameSize < 0) {
			logger.warning(
				"$loggingFilename:Invalid Frame size:$identifier"
			)
			throw InvalidFrameException("$identifier is invalid frame")
		} else if (frameSize == 0) {
			logger.warning(
				"$loggingFilename:Empty Frame:$identifier"
			)
			//We dont process this frame or add to framemap becuase contains no useful information
			//Skip the two flag bytes so in correct position for subsequent frames
			byteBuffer.get()
			byteBuffer.get()
			throw EmptyFrameException("$identifier is empty frame")
		} else if (frameSize > byteBuffer.remaining() - FRAME_FLAG_SIZE) {
			logger.warning(
				"$loggingFilename:Invalid Frame size larger than size before mp3 audio:$identifier"
			)
			throw InvalidFrameException("$identifier is invalid frame")
		}
		checkIfFrameSizeThatIsNotSyncSafe(byteBuffer)
	}

	/**
	 * Read the frame from the specified file.
	 * Read the frame header then delegate reading of data to frame body.
	 *
	 * @param byteBuffer to read the frame from
	 */
	@Throws(InvalidFrameException::class, InvalidDataTypeException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val identifier = readIdentifier(byteBuffer)

		//Is this a valid identifier?
		if (!isValidID3v2FrameIdentifier(identifier)) {
			//If not valid move file pointer back to one byte after
			//the original check so can try again.
			logger.config(
				"$loggingFilename:Invalid identifier:$identifier"
			)
			byteBuffer.position(byteBuffer.position() - (this.frameIdSize - 1))
			throw InvalidFrameIdentifierException(
				"$loggingFilename:$identifier:is not a valid ID3v2.30 frame"
			)
		}

		//Get the frame size, adjusted as necessary
		getFrameSize(byteBuffer)

		//Read the flag bytes
		statusFlags = StatusFlags(
			byteBuffer.get()
		)
		encodingFlags = EncodingFlags(
			byteBuffer.get()
		)

		//Read extra bits appended to frame header for various encodings
		//These are not included in header size but are included in frame size but wont be read when we actually
		//try to read the frame body data
		var extraHeaderBytesCount = 0
		var dataLengthSize = -1
		if ((encodingFlags as EncodingFlags).isGrouping) {
			extraHeaderBytesCount = FRAME_GROUPING_INDICATOR_SIZE
			groupIdentifier = byteBuffer.get().toInt()
		}
		if ((encodingFlags as EncodingFlags).isEncryption) {
			//Read the Encryption byte, but do nothing with it
			extraHeaderBytesCount += FRAME_ENCRYPTION_INDICATOR_SIZE
			encryptionMethod = byteBuffer.get().toInt()
		}
		if ((encodingFlags as EncodingFlags).isDataLengthIndicator) {
			//Read the sync safe size field
			dataLengthSize = bufferToValue(
				byteBuffer
			)
			extraHeaderBytesCount += FRAME_DATA_LENGTH_SIZE
			logger.config(
				"$loggingFilename:Frame Size Is:$frameSize Data Length Size:$dataLengthSize"
			)
		}

		//Work out the real size of the frameBody data
		val realFrameSize = frameSize - extraHeaderBytesCount

		//Create Buffer that only contains the body of this frame rather than the remainder of tag
		var frameBodyBuffer = byteBuffer.slice()
		frameBodyBuffer.limit(realFrameSize)

		//Do we need to synchronize the frame body
		var syncSize = realFrameSize
		if ((encodingFlags as EncodingFlags).isUnsynchronised) {
			//We only want to synchronize the buffer up to the end of this frame (remember this
			//buffer contains the remainder of this tag not just this frame), and we cannot just
			//create a new buffer because when this method returns the position of the buffer is used
			//to look for the next frame, so we need to modify the buffer. The action of synchronizing causes
			//bytes to be dropped so the existing buffer is large enough to hold the modifications
			frameBodyBuffer = synchronize(frameBodyBuffer)
			syncSize = frameBodyBuffer.limit()
			logger.config(
				"$loggingFilename:Frame Size After Syncing is:$syncSize"
			)
		}

		//Read the body data
		try {
			if ((encodingFlags as EncodingFlags).isCompression) {
				frameBodyBuffer = uncompress(
					identifier,
					loggingFilename,
					byteBuffer,
					dataLengthSize,
					realFrameSize
				)
				frameBody = if ((encodingFlags as EncodingFlags).isEncryption) {
					readEncryptedBody(identifier, frameBodyBuffer, dataLengthSize)
				} else {
					readBody(identifier, frameBodyBuffer, dataLengthSize)
				}
			} else if ((encodingFlags as EncodingFlags).isEncryption) {
				frameBodyBuffer = byteBuffer.slice()
				frameBodyBuffer.limit(realFrameSize)
				frameBody = readEncryptedBody(identifier, byteBuffer, frameSize)
			} else {
				frameBody = readBody(identifier, frameBodyBuffer, syncSize)
			}
			if (frameBody !is ID3v24FrameBody) {
				logger.config(
					"$loggingFilename:Converted frame body with:$identifier to deprecated framebody"
				)
				frameBody = FrameBodyDeprecated(frameBody as AbstractID3v2FrameBody?)
			}
		} finally {
			//Update position of main buffer, so no attempt is made to reread these bytes
			byteBuffer.position(byteBuffer.position() + realFrameSize)
		}
	}

	/**
	 * Write the frame. Writes the frame header but writing the data is delegated to the
	 * frame body.
	 */
	override fun write(tagBuffer: ByteArrayOutputStream?) {
		val unsynchronization: Boolean
		logger.config(
			"Writing frame to file:$identifier"
		)

		//This is where we will write header, move position to where we can
		//write bodybuffer
		val headerBuffer = ByteBuffer.allocate(Companion.FRAME_HEADER_SIZE)

		//Write Frame Body Data to a new stream
		val bodyOutputStream = ByteArrayOutputStream()
		(frameBody as AbstractID3v2FrameBody).write(bodyOutputStream)

		//Does it need unsynchronizing, and are we allowing unsychronizing
		var bodyBuffer = bodyOutputStream.toByteArray()
		unsynchronization =
			TagOptionSingleton.instance.isUnsyncTags && requiresUnsynchronization(bodyBuffer)
		if (unsynchronization) {
			bodyBuffer = unsynchronize(bodyBuffer)
			logger.config("bodybytebuffer:sizeafterunsynchronisation:" + bodyBuffer.size)
		}

		//Write Frame Header
		//Write Frame ID, the identifier must be 4 bytes bytes long it may not be
		//because converted an unknown v2.2 id (only 3 bytes long)
		if (identifier.length == 3) {
			identifier = "$identifier "
		}
		headerBuffer.put(
			identifier.toByteArray(StandardCharsets.ISO_8859_1),
			0,
			Companion.FRAME_ID_SIZE
		)

		//Write Frame Size based on size of body buffer (if it has been unsynced then it size
		//will have increased accordingly
		val size = bodyBuffer.size
		logger.fine(
			"Frame Size Is:$size"
		)
		headerBuffer.put(valueToBuffer(size))

		//Write the Flags
		//Status Flags:leave as they were when we read
		headerBuffer.put(statusFlags!!.writeFlags)

		//Remove any non standard flags
		(encodingFlags as EncodingFlags).unsetNonStandardFlags()

		//Encoding we only support unsynchronization
		if (unsynchronization) {
			(encodingFlags as EncodingFlags).setUnsynchronised()
		} else {
			(encodingFlags as EncodingFlags).unsetUnsynchronised()
		}
		//These are not currently supported on write
		(encodingFlags as EncodingFlags).unsetCompression()
		(encodingFlags as EncodingFlags).unsetDataLengthIndicator()
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

			//Add bodybuffer to the Byte Array Output Stream
			tagBuffer.write(bodyBuffer)
		} catch (ioe: IOException) {
			//This could never happen coz not writing to file, so convert to RuntimeException
			throw RuntimeException(ioe)
		}
	}

	/**
	 * Member Class This represents a frame headers Status Flags
	 * Make adjustments if necessary based on frame type and specification.
	 */
	inner class StatusFlags : AbstractID3v2Frame.StatusFlags {
		/**
		 * Use this when creating a frame from scratch
		 */
		internal constructor() : super()

		/**
		 * Use this constructor when reading from file or from another v4 frame
		 *
		 * @param flags
		 */
		internal constructor(flags: Byte) {
			originalFlags = flags
			writeFlags = flags
			modifyFlags()
		}

		/**
		 * Use this constructor when convert a v23 frame
		 *
		 * @param statusFlags
		 */
		internal constructor(statusFlags: ID3v23Frame.StatusFlags) {
			originalFlags = convertV3ToV4Flags(statusFlags.originalFlags)
			writeFlags = originalFlags
			modifyFlags()
		}

		/**
		 * Convert V3 Flags to equivalent V4 Flags
		 *
		 * @param v3Flag
		 * @return
		 */
		private fun convertV3ToV4Flags(v3Flag: Byte): Byte {
			var v4Flag = 0.toByte()
			if (v3Flag.toInt() and ID3v23Frame.MASK_FILE_ALTER_PRESERVATION != 0) {
				v4Flag = (v4Flag.toInt() or MASK_FILE_ALTER_PRESERVATION.toByte().toInt()).toByte()
			}
			if (v3Flag.toInt() and ID3v23Frame.MASK_TAG_ALTER_PRESERVATION != 0) {
				v4Flag = (v4Flag.toInt() or MASK_TAG_ALTER_PRESERVATION.toByte().toInt()).toByte()
			}
			return v4Flag
		}

		/**
		 * Makes modifications to flags based on specification and frameid
		 */
		protected fun modifyFlags() {
			val str = identifier
			if (ID3v24Frames.instanceOf.isDiscardIfFileAltered(str)) {
				writeFlags = (writeFlags.toInt() or MASK_FILE_ALTER_PRESERVATION.toByte().toInt()).toByte()
				writeFlags =
					(writeFlags.toInt() and MASK_TAG_ALTER_PRESERVATION.toByte().inv().toInt()).toByte()
			} else {
				writeFlags =
					(writeFlags.toInt() and MASK_FILE_ALTER_PRESERVATION.toByte().inv().toInt()).toByte()
				writeFlags =
					(writeFlags.toInt() and MASK_TAG_ALTER_PRESERVATION.toByte().inv().toInt()).toByte()
			}
		}

		override fun createStructure() {
			MP3File.structureFormatter?.openHeadingElement(
				TYPE_FLAGS,
				""
			)
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
	internal inner class EncodingFlags : AbstractID3v2Frame.EncodingFlags {
		/**
		 * Use this when creating a frame from scratch
		 */
		constructor() : super()

		/**
		 * Use this when creating a frame from existing flags in another v4 frame
		 *
		 * @param flags
		 */
		constructor(flags: Byte) : super(flags) {
			logEnabledFlags()
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
					ErrorMessage.MP3_FRAME_IS_COMPRESSED.getMsg(
						loggingFilename,
						identifier
					)
				)
			}
			if (isEncryption) {
				logger.warning(
					ErrorMessage.MP3_FRAME_IS_ENCRYPTED.getMsg(
						loggingFilename,
						identifier
					)
				)
			}
			if (isGrouping) {
				logger.config(ErrorMessage.MP3_FRAME_IS_GROUPED.getMsg(loggingFilename, identifier))
			}
			if (isUnsynchronised) {
				logger.config(
					ErrorMessage.MP3_FRAME_IS_UNSYNCHRONISED.getMsg(
						loggingFilename,
						identifier
					)
				)
			}
			if (isDataLengthIndicator) {
				logger.config(
					ErrorMessage.MP3_FRAME_IS_DATA_LENGTH_INDICATOR.getMsg(
						loggingFilename,
						identifier
					)
				)
			}
		}

		val isCompression: Boolean
			get() = flags.toInt() and MASK_COMPRESSION > 0
		val isEncryption: Boolean
			get() = flags.toInt() and MASK_ENCRYPTION > 0
		val isGrouping: Boolean
			get() = flags.toInt() and MASK_GROUPING_IDENTITY > 0
		val isUnsynchronised: Boolean
			get() = flags.toInt() and MASK_FRAME_UNSYNCHRONIZATION > 0
		val isDataLengthIndicator: Boolean
			get() = flags.toInt() and MASK_DATA_LENGTH_INDICATOR > 0

		fun setCompression() {
			flags = (flags.toInt() or MASK_COMPRESSION).toByte()
		}

		fun setEncryption() {
			flags = (flags.toInt() or MASK_ENCRYPTION).toByte()
		}

		fun setGrouping() {
			flags = (flags.toInt() or MASK_GROUPING_IDENTITY).toByte()
		}

		fun setUnsynchronised() {
			flags = (flags.toInt() or MASK_FRAME_UNSYNCHRONIZATION).toByte()
		}

		fun setDataLengthIndicator() {
			flags = (flags.toInt() or MASK_DATA_LENGTH_INDICATOR).toByte()
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

		fun unsetUnsynchronised() {
			flags = (flags.toInt() and MASK_FRAME_UNSYNCHRONIZATION.toByte().inv().toInt()).toByte()
		}

		fun unsetDataLengthIndicator() {
			flags = (flags.toInt() and MASK_DATA_LENGTH_INDICATOR.toByte().inv().toInt()).toByte()
		}

		val isNonStandardFlags: Boolean
			get() = flags.toInt() and FileConstants.BIT7 > 0
				|| flags.toInt() and FileConstants.BIT5 > 0
				|| flags.toInt() and FileConstants.BIT4 > 0

		fun unsetNonStandardFlags() {
			if (isNonStandardFlags) {
				logger.warning(
					loggingFilename + ":" + identifier + ":Unsetting Unknown Encoding Flags:" + Hex.asHex(
						flags
					)
				)
				flags = (flags.toInt() and FileConstants.BIT7.toByte().inv().toInt()).toByte()
				flags = (flags.toInt() and FileConstants.BIT5.toByte().inv().toInt()).toByte()
				flags = (flags.toInt() and FileConstants.BIT4.toByte().inv().toInt()).toByte()
			}
		}

		override fun createStructure() {
			MP3File.structureFormatter?.openHeadingElement(TYPE_FLAGS, "")
			MP3File.structureFormatter?.addElement(TYPE_COMPRESSION, flags.toInt() and MASK_COMPRESSION)
			MP3File.structureFormatter?.addElement(TYPE_ENCRYPTION, flags.toInt() and MASK_ENCRYPTION)
			MP3File.structureFormatter?.addElement(
				TYPE_GROUPIDENTITY,
				flags.toInt() and MASK_GROUPING_IDENTITY
			)
			MP3File.structureFormatter?.addElement(
				TYPE_FRAMEUNSYNCHRONIZATION,
				flags.toInt() and MASK_FRAME_UNSYNCHRONIZATION
			)
			MP3File.structureFormatter?.addElement(
				TYPE_DATALENGTHINDICATOR,
				flags.toInt() and MASK_DATA_LENGTH_INDICATOR
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
		statusFlags?.createStructure()
		encodingFlags?.createStructure()
		frameBody?.createStructure()
		MP3File.structureFormatter?.closeHeadingElement(TYPE_FRAME)
	}

	/**
	 * @return true if considered a common frame
	 */
	override val isCommon: Boolean
		get() = ID3v24Frames.instanceOf.isCommon(id)

	/**
	 * @return true if considered a common frame
	 */
	override val isBinary: Boolean
		get() = ID3v24Frames.instanceOf.isBinary(id)

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
				if (encodingId < 4) {
					body!!.textEncoding = encodingId.toByte()
				}
			}
		}

	companion object {
		private val validFrameIdentifier = Pattern.compile("[A-Z][0-9A-Z]{3}")

		@JvmStatic
		protected val FRAME_DATA_LENGTH_SIZE = 4

		@JvmStatic
		protected val FRAME_ID_SIZE = 4

		@JvmStatic
		protected val FRAME_FLAG_SIZE = 2

		@JvmStatic
		protected val FRAME_SIZE_SIZE = 4

		@JvmStatic
		protected val FRAME_ENCRYPTION_INDICATOR_SIZE = 1

		@JvmStatic
		protected val FRAME_GROUPING_INDICATOR_SIZE = 1

		@JvmStatic
		protected val FRAME_HEADER_SIZE = FRAME_ID_SIZE + FRAME_SIZE_SIZE + FRAME_FLAG_SIZE

		const val TYPE_COMPRESSION = "compression"
		const val TYPE_ENCRYPTION = "encryption"
		const val TYPE_GROUPIDENTITY = "groupidentity"
		const val TYPE_FRAMEUNSYNCHRONIZATION = "frameUnsynchronisation"
		const val TYPE_DATALENGTHINDICATOR = "dataLengthIndicator"

		/**
		 * Frame is part of a group
		 */
		const val MASK_GROUPING_IDENTITY = FileConstants.BIT6

		/**
		 * Frame is compressed
		 */
		const val MASK_COMPRESSION = FileConstants.BIT3

		/**
		 * Frame is encrypted
		 */
		const val MASK_ENCRYPTION = FileConstants.BIT2

		/**
		 * Unsynchronisation
		 */
		const val MASK_FRAME_UNSYNCHRONIZATION = FileConstants.BIT1

		/**
		 * Length
		 */
		const val MASK_DATA_LENGTH_INDICATOR = FileConstants.BIT0

		const val TYPE_TAGALTERPRESERVATION = "typeTagAlterPreservation"
		const val TYPE_FILEALTERPRESERVATION = "typeFileAlterPreservation"
		const val TYPE_READONLY = "typeReadOnly"

		/**
		 * Discard frame if tag altered
		 */
		const val MASK_TAG_ALTER_PRESERVATION = FileConstants.BIT6

		/**
		 * Discard frame if audio part of file altered
		 */
		const val MASK_FILE_ALTER_PRESERVATION = FileConstants.BIT5

		/**
		 * Frame tagged as read only
		 */
		const val MASK_READ_ONLY = FileConstants.BIT4
	}
}

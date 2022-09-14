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
 *  you can getFields a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.FileConstants
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.bufferToValue
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.valueToBuffer
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Unsynchronization.requiresUnsynchronization
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Unsynchronization.synchronize
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Unsynchronization.unsynchronize
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames.Companion.instanceOf
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23PreferredFrameOrderComparator.Companion.instanceof
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.AndroidArtwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.util.logging.Level

/**
 * Represents an ID3v2.3 tag.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class ID3v23Tag : ID3v2TagBase {

	override val release: Byte
		get() = RELEASE
	override val majorVersion: Byte
		get() = MAJOR_VERSION
	override val revision: Byte
		get() = REVISION

	/**
	 * CRC Checksum calculated
	 */
	protected var crcDataFlag = false

	/**
	 * Experiemntal tag
	 */
	protected var experimental = false

	/**
	 * Contains extended header
	 */
	protected var extended = false
	/**
	 * @return  Cyclic Redundancy Check 32 Value
	 */
	/**
	 * Crcdata Checksum in extended header
	 */
	var crc32 = 0
		private set

	/**
	 * Tag padding
	 */
	var paddingSize = 0
		private set
	/**
	 * @return is tag unsynchronized
	 */
	/**
	 * All frames in the tag uses unsynchronisation
	 */
	var isUnsynchronization = false
		protected set

	/**
	 * The tag is compressed
	 */
	var compression = false
		protected set

	/**
	 * Creates a new empty ID3v2_3 datatype.
	 */
	constructor() {
		frameMap = LinkedHashMap<String, MutableList<TagField?>>()
		encryptedFrameMap = LinkedHashMap<String, MutableList<TagField?>>()
	}

	/**
	 * Copy primitives applicable to v2.3
	 */
	override fun copyPrimitives(copyObj: ID3v2TagBase) {
		logger.config("Copying primitives")
		super.copyPrimitives(copyObj)
		if (copyObj is ID3v23Tag) {
			val copyObject = copyObj
			crcDataFlag = copyObject.crcDataFlag
			experimental = copyObject.experimental
			extended = copyObject.extended
			crc32 = copyObject.crc32
			paddingSize = copyObject.paddingSize
		}
	}

	/**
	 * Override to merge TIPL/TMCL into single IPLS frame
	 *
	 * @param newFrame
	 * @param existingFrame
	 */
	override fun combineFrames(newFrame: AbstractID3v2Frame?, existing: MutableList<TagField?>) {
		var existingFrame: AbstractID3v2Frame? = null
		for (field in existing) {
			if (field is AbstractID3v2Frame) {
				val frame = field
				if (frame.identifier == newFrame!!.identifier) {
					existingFrame = frame
				}
			}
		}

		//We dont add this new frame we just add the contents to existing frame
		if (newFrame!!.identifier == ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE && existingFrame != null) {
			val oldVps = (existingFrame.body as FrameBodyIPLS?)!!.pairing
			val newVps = (newFrame.body as FrameBodyIPLS?)!!.pairing
			for (next in newVps.getMapping()) {
				oldVps.add(next)
			}
		} else {
			existing.add(newFrame)
		}
	}

	public override fun addFrame(frame: AbstractID3v2Frame?) {
		try {
			if (frame is ID3v23Frame) {
				copyFrameIntoMap(frame.identifier, frame)
			} else {
				val frames = convertFrame(frame)
				for (next in frames!!) {
					copyFrameIntoMap(next!!.identifier, next)
				}
			}
		} catch (ife: InvalidFrameException) {
			logger.log(Level.SEVERE, "Unable to convert frame:" + frame!!.identifier)
		}
	}

	@Throws(InvalidFrameException::class)
	override fun convertFrame(frame: AbstractID3v2Frame?): List<AbstractID3v2Frame?>? {
		val frames: MutableList<AbstractID3v2Frame?> = ArrayList()
		if (frame!!.identifier == ID3v24Frames.FRAME_ID_YEAR && frame.body is FrameBodyTDRC) {
			//TODO will overwrite any existing TYER or TIME frame, do we ever want multiples of these
			val tmpBody = frame.body as FrameBodyTDRC?
			tmpBody!!.findMatchingMaskAndExtractV3Values()
			var newFrame: ID3v23Frame
			if (tmpBody.year != "") {
				newFrame = ID3v23Frame(ID3v23Frames.FRAME_ID_V3_TYER)
				(newFrame.body as FrameBodyTYER?)!!.text = tmpBody.year
				frames.add(newFrame)
			}
			if (tmpBody.date != "") {
				newFrame = ID3v23Frame(ID3v23Frames.FRAME_ID_V3_TDAT)
				(newFrame.body as FrameBodyTDAT?)!!.text = tmpBody.date
				(newFrame.body as FrameBodyTDAT?)!!.isMonthOnly = tmpBody.isMonthOnly
				frames.add(newFrame)
			}
			if (tmpBody.time != "") {
				newFrame = ID3v23Frame(ID3v23Frames.FRAME_ID_V3_TIME)
				(newFrame.body as FrameBodyTIME?)!!.text = tmpBody.time
				(newFrame.body as FrameBodyTIME?)!!.isHoursOnly = tmpBody.isHoursOnly
				frames.add(newFrame)
			}
		} else if (frame.identifier == ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE && frame.body is FrameBodyTIPL) {
			val pairs = (frame.body as FrameBodyTIPL?)!!.pairing.getMapping()
			val ipls: AbstractID3v2Frame = ID3v23Frame(
				(frame as ID3v24Frame?)!!, ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE
			)
			val iplsBody = FrameBodyIPLS(frame.body!!.textEncoding, pairs)
			ipls.body = iplsBody
			frames.add(ipls)
		} else if (frame.identifier == ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS && frame.body is FrameBodyTMCL) {
			val pairs = (frame.body as FrameBodyTMCL?)!!.pairing.getMapping()
			val ipls: AbstractID3v2Frame = ID3v23Frame(
				(frame as ID3v24Frame?)!!, ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE
			)
			val iplsBody = FrameBodyIPLS(frame.body!!.textEncoding, pairs)
			ipls.body = iplsBody
			frames.add(ipls)
		} else {
			frames.add(
				ID3v23Frame(
					frame
				)
			)
		}
		return frames
	}

	/**
	 * Copy Constructor, creates a new ID3v2_3 Tag based on another ID3v2_3 Tag
	 * @param copyObject
	 */
	constructor(copyObject: ID3v23Tag) : super(copyObject) {
		//This doesn't do anything.
		logger.config("Creating tag from another tag of same type")
		copyPrimitives(copyObject)
		copyFrames(copyObject)
	}

	/**
	 * Constructs a new tag based upon another tag of different version/type
	 * @param mp3tag
	 */
	constructor(mp3tag: AbstractTag?) {
		logger.config("Creating tag from a tag of a different version")
		frameMap = LinkedHashMap<String, MutableList<TagField?>>()
		encryptedFrameMap = LinkedHashMap<String, MutableList<TagField?>>()
		if (mp3tag != null) {
			val convertedTag: ID3v24Tag
			//Should use simpler copy constructor
			if (mp3tag is ID3v23Tag) {
				throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
			}
			convertedTag =
				if (mp3tag is ID3v24Tag) {
					mp3tag
				} else {
					ID3v24Tag(mp3tag)
				}
			loggingFilename = convertedTag.loggingFilename
			//Copy Primitives
			copyPrimitives(convertedTag)
			//Copy Frames
			copyFrames(convertedTag)
			logger.config("Created tag from a tag of a different version")
		}
	}

	/**
	 * Creates a new ID3v2_3 datatype.
	 *
	 * @param buffer
	 * @param loggingFilename
	 * @throws TagException
	 */
	constructor(buffer: ByteBuffer, loggingFilename: String) {
		this.loggingFilename = loggingFilename
		read(buffer)
	}

	/**
	 * Creates a new ID3v2_3 datatype.
	 *
	 * @param buffer
	 * @throws TagException
	 */
	@Deprecated("use {@link #ID3v23Tag(ByteBuffer,String)} instead")
	constructor(buffer: ByteBuffer) : this(buffer, "")

	override val identifier: String?
		get() = "ID3v2.30"

	/**
	 * Return frame size based upon the sizes of the tags rather than the physical
	 * no of bytes between start of ID3Tag and start of Audio Data.
	 *
	 * TODO this is incorrect, because of subclasses
	 *
	 * @return size of tag
	 */
	override val size: Int
		get() {
			var size = TAG_HEADER_LENGTH
			if (extended) {
				size += TAG_EXT_HEADER_LENGTH
				if (crcDataFlag) {
					size += TAG_EXT_HEADER_CRC_LENGTH
				}
			}
			size += super.size
			return size
		}

	/**
	 * Is Tag Equivalent to another tag
	 *
	 * @param obj
	 * @return true if tag is equivalent to another
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is ID3v23Tag) {
			return false
		}
		val `object` = obj
		if (crc32 != `object`.crc32) {
			return false
		}
		if (crcDataFlag != `object`.crcDataFlag) {
			return false
		}
		if (experimental != `object`.experimental) {
			return false
		}
		return if (extended != `object`.extended) {
			false
		} else paddingSize == `object`.paddingSize && super.equals(obj)
	}

	/**
	 * Read header flags
	 *
	 *
	 * Log info messages for flags that have been set and log warnings when bits have been set for unknown flags
	 * @param buffer
	 * @throws TagException
	 */
	@Throws(TagException::class)
	private fun readHeaderFlags(buffer: ByteBuffer?) {
		//Allowable Flags
		val flags = buffer!!.get()
		isUnsynchronization = flags.toInt() and MASK_V23_UNSYNCHRONIZATION != 0
		extended = flags.toInt() and MASK_V23_EXTENDED_HEADER != 0
		experimental = flags.toInt() and MASK_V23_EXPERIMENTAL != 0

		//Not allowable/Unknown Flags
		if (flags.toInt() and FileConstants.BIT4 != 0) {
			logger.warning(
				ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(
					loggingFilename,
					FileConstants.BIT4
				)
			)
		}
		if (flags.toInt() and FileConstants.BIT3 != 0) {
			logger.warning(
				ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(
					loggingFilename,
					FileConstants.BIT3
				)
			)
		}
		if (flags.toInt() and FileConstants.BIT2 != 0) {
			logger.warning(
				ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(
					loggingFilename,
					FileConstants.BIT2
				)
			)
		}
		if (flags.toInt() and FileConstants.BIT1 != 0) {
			logger.warning(
				ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(
					loggingFilename,
					FileConstants.BIT1
				)
			)
		}
		if (flags.toInt() and FileConstants.BIT0 != 0) {
			logger.warning(
				ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(
					loggingFilename,
					FileConstants.BIT0
				)
			)
		}
		if (isUnsynchronization) {
			logger.config(ErrorMessage.ID3_TAG_UNSYNCHRONIZED.getMsg(loggingFilename))
		}
		if (extended) {
			logger.config(ErrorMessage.ID3_TAG_EXTENDED.getMsg(loggingFilename))
		}
		if (experimental) {
			logger.config(ErrorMessage.ID3_TAG_EXPERIMENTAL.getMsg(loggingFilename))
		}
	}

	/**
	 * Read the optional extended header
	 *
	 * @param buffer
	 * @param size
	 */
	private fun readExtendedHeader(buffer: ByteBuffer?, size: Int) {
		// Int is 4 bytes.
		var size = size
		val extendedHeaderSize = buffer!!.int
		// Extended header without CRC Data
		if (extendedHeaderSize == TAG_EXT_HEADER_DATA_LENGTH) {
			//Flag should not be setField , if is log a warning
			val extFlag = buffer.get()
			crcDataFlag = extFlag.toInt() and MASK_V23_CRC_DATA_PRESENT != 0
			if (crcDataFlag) {
				logger.warning(ErrorMessage.ID3_TAG_CRC_FLAG_SET_INCORRECTLY.getMsg(loggingFilename))
			}
			//2nd Flag Byte (not used)
			buffer.get()

			//Take padding and ext header size off the size to be read
			paddingSize = buffer.int
			if (paddingSize > 0) {
				logger.config(
					ErrorMessage.ID3_TAG_PADDING_SIZE.getMsg(
						loggingFilename,
						paddingSize
					)
				)
			}
			size = size - (paddingSize + TAG_EXT_HEADER_LENGTH)
		} else if (extendedHeaderSize == TAG_EXT_HEADER_DATA_LENGTH + TAG_EXT_HEADER_CRC_LENGTH) {
			logger.config(ErrorMessage.ID3_TAG_CRC.getMsg(loggingFilename))

			//Flag should be setField, if nor just act as if it is
			val extFlag = buffer.get()
			crcDataFlag = extFlag.toInt() and MASK_V23_CRC_DATA_PRESENT != 0
			if (!crcDataFlag) {
				logger.warning(ErrorMessage.ID3_TAG_CRC_FLAG_SET_INCORRECTLY.getMsg(loggingFilename))
			}
			//2nd Flag Byte (not used)
			buffer.get()
			//Take padding size of size to be read
			paddingSize = buffer.int
			if (paddingSize > 0) {
				logger.config(
					ErrorMessage.ID3_TAG_PADDING_SIZE.getMsg(
						loggingFilename,
						paddingSize
					)
				)
			}
			size = size - (paddingSize + TAG_EXT_HEADER_LENGTH + TAG_EXT_HEADER_CRC_LENGTH)
			//CRC Data
			crc32 = buffer.int
			logger.config(ErrorMessage.ID3_TAG_CRC_SIZE.getMsg(loggingFilename, crc32))
		} else {
			logger.warning(
				ErrorMessage.ID3_EXTENDED_HEADER_SIZE_INVALID.getMsg(
					loggingFilename,
					extendedHeaderSize
				)
			)
			buffer.position(buffer.position() - FIELD_TAG_EXT_SIZE_LENGTH)
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(TagException::class)
	override fun read(buffer: ByteBuffer) {
		val size: Int
		if (!seek(buffer)) {
			throw TagNotFoundException("$identifier tag not found")
		}
		logger.config(
			"$loggingFilename:Reading ID3v23 tag"
		)
		readHeaderFlags(buffer)

		// Read the size, this is size of tag not including the tag header
		size = bufferToValue(
			buffer
		)
		logger.config(ErrorMessage.ID_TAG_SIZE.getMsg(loggingFilename, size))

		//Extended Header
		if (extended) {
			readExtendedHeader(buffer, size)
		}

		//Slice Buffer, so position markers tally with size (i.e do not include tagHeader)
		var bufferWithoutHeader = buffer.slice()
		//We need to synchronize the buffer
		if (isUnsynchronization) {
			bufferWithoutHeader = synchronize(bufferWithoutHeader)
		}
		readFrames(bufferWithoutHeader, size)
		logger.config(loggingFilename + ":Loaded Frames,there are:" + frameMap!!.keys.size)
	}

	/**
	 * Read the frames
	 *
	 * Read from byteBuffer upto size
	 *
	 * @param byteBuffer
	 * @param size
	 */
	protected fun readFrames(byteBuffer: ByteBuffer, size: Int) {
		//Now start looking for frames
		var next: ID3v23Frame
		frameMap = LinkedHashMap<String, MutableList<TagField?>>()
		encryptedFrameMap = LinkedHashMap<String, MutableList<TagField?>>()


		//Read the size from the Tag Header
		fileReadBytes = size
		logger.finest(loggingFilename + ":Start of frame body at:" + byteBuffer.position() + ",frames data size is:" + size)

		// Read the frames until got to up to the size as specified in header or until
		// we hit an invalid frame identifier or padding
		while (byteBuffer.position() < size) {
			var id: String
			try {
				//Read Frame
				val posBeforeRead = byteBuffer.position()
				logger.config(
					"$loggingFilename:Looking for next frame at:$posBeforeRead"
				)
				next = ID3v23Frame(byteBuffer, loggingFilename)
				id = next.identifier
				logger.config(
					"$loggingFilename:Found $id at frame at:$posBeforeRead"
				)
				loadFrameIntoMap(id, next)
			} //Found Padding, no more frames
			catch (ex: PaddingException) {
				logger.info(loggingFilename + ":Found padding starting at:" + byteBuffer.position())
				break
			} //Found Empty Frame, log it - empty frames should not exist
			catch (ex: EmptyFrameException) {
				logger.warning(loggingFilename + ":Empty Frame:" + ex.message)
				emptyFrameBytes += ID3v23Frame.FRAME_HEADER_SIZE
			} catch (ifie: InvalidFrameIdentifierException) {
				logger.warning(loggingFilename + ":Invalid Frame Identifier:" + ifie.message)
				invalidFrames++
				//Don't try and find any more frames
				break
			} //Problem trying to find frame, often just occurs because frameHeader includes padding
			//and we have reached padding
			catch (ife: InvalidFrameException) {
				logger.warning(loggingFilename + ":Invalid Frame:" + ife.message)
				invalidFrames++
				//Don't try and find any more frames
				break
			} //Failed reading frame but may just have invalid data but correct length so lets carry on
			//in case we can read the next frame
			catch (idete: InvalidDataTypeException) {
				logger.warning(loggingFilename + ":Corrupt Frame:" + idete.message)
				invalidFrames++
				continue
			}
		}
	}

	/**
	 * Write the ID3 header to the ByteBuffer.
	 *
	 * TODO Calculate the CYC Data Check
	 * TODO Reintroduce Extended Header
	 *
	 * @param padding is the size of the padding portion of the tag
	 * @param size    is the size of the body data
	 * @return ByteBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeHeaderToBuffer(padding: Int, size: Int): ByteBuffer {
		// Flags,currently we never calculate the CRC
		// and if we dont calculate them cant keep orig values. Tags are not
		// experimental and we never createField extended header to keep things simple.
		extended = false
		experimental = false
		crcDataFlag = false

		// Create Header Buffer,allocate maximum possible size for the header
		val headerBuffer =
			ByteBuffer.allocate(TAG_HEADER_LENGTH + TAG_EXT_HEADER_LENGTH + TAG_EXT_HEADER_CRC_LENGTH)

		//TAGID
		headerBuffer.put(TAG_ID)

		//Major Version
		headerBuffer.put(this.majorVersion)

		//Minor Version
		headerBuffer.put(this.revision)

		//Flags
		var flagsByte: Byte = 0
		if (isUnsynchronization) {
			flagsByte = (flagsByte.toInt() or MASK_V23_UNSYNCHRONIZATION).toByte()
		}
		if (extended) {
			flagsByte = (flagsByte.toInt() or MASK_V23_EXTENDED_HEADER).toByte()
		}
		if (experimental) {
			flagsByte = (flagsByte.toInt() or MASK_V23_EXPERIMENTAL).toByte()
		}
		headerBuffer.put(flagsByte)

		//Additional Header Size,(for completeness we never actually write the extended header)
		var additionalHeaderSize = 0
		if (extended) {
			additionalHeaderSize += TAG_EXT_HEADER_LENGTH
			if (crcDataFlag) {
				additionalHeaderSize += TAG_EXT_HEADER_CRC_LENGTH
			}
		}

		//Size As Recorded in Header, don't include the main header length
		headerBuffer.put(valueToBuffer(padding + size + additionalHeaderSize))

		//Write Extended Header
		if (extended) {
			var extFlagsByte1: Byte = 0
			val extFlagsByte2: Byte = 0

			//Contains CRCData
			if (crcDataFlag) {
				headerBuffer.putInt(TAG_EXT_HEADER_DATA_LENGTH + TAG_EXT_HEADER_CRC_LENGTH)
				extFlagsByte1 = (extFlagsByte1.toInt() or MASK_V23_CRC_DATA_PRESENT).toByte()
				headerBuffer.put(extFlagsByte1)
				headerBuffer.put(extFlagsByte2)
				headerBuffer.putInt(paddingSize)
				headerBuffer.putInt(crc32)
			} else {
				headerBuffer.putInt(TAG_EXT_HEADER_DATA_LENGTH)
				headerBuffer.put(extFlagsByte1)
				headerBuffer.put(extFlagsByte2)
				//Newly Calculated Padding As Recorded in Extended Header
				headerBuffer.putInt(padding)
			}
		}
		headerBuffer.flip()
		return headerBuffer
	}

	/**
	 * Write tag to file
	 *
	 * TODO:we currently never write the Extended header , but if we did the size calculation in this
	 * method would be slightly incorrect
	 *
	 * @param file The file to write to
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun write(file: File?, audioStartLocation: Long): Long {
		loggingFilename = file!!.name
		logger.config(
			"Writing tag to file:$loggingFilename"
		)

		//Write Body Buffer
		var bodyByteBuffer = writeFramesToBuffer().toByteArray()
		logger.config(loggingFilename + ":bodybytebuffer:sizebeforeunsynchronisation:" + bodyByteBuffer.size)

		// Unsynchronize if option enabled and unsync required
		isUnsynchronization =
			TagOptionSingleton.instance.isUnsyncTags && requiresUnsynchronization(bodyByteBuffer)
		if (isUnsynchronization) {
			bodyByteBuffer = unsynchronize(bodyByteBuffer)
			logger.config(loggingFilename + ":bodybytebuffer:sizeafterunsynchronisation:" + bodyByteBuffer.size)
		}
		val sizeIncPadding =
			calculateTagSize(bodyByteBuffer.size + TAG_HEADER_LENGTH, audioStartLocation.toInt())
		val padding = sizeIncPadding - (bodyByteBuffer.size + TAG_HEADER_LENGTH)
		logger.config(
			"$loggingFilename:Current audiostart:$audioStartLocation"
		)
		logger.config(
			"$loggingFilename:Size including padding:$sizeIncPadding"
		)
		logger.config(
			"$loggingFilename:Padding:$padding"
		)
		val headerBuffer = writeHeaderToBuffer(padding, bodyByteBuffer.size)
		writeBufferToFile(
			file,
			headerBuffer,
			bodyByteBuffer,
			padding,
			sizeIncPadding,
			audioStartLocation
		)
		return sizeIncPadding.toLong()
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(channel: WritableByteChannel?, currentTagSize: Int) {
		logger.config(
			"$loggingFilename:Writing tag to channel"
		)
		var bodyByteBuffer = writeFramesToBuffer().toByteArray()
		logger.config(loggingFilename + ":bodybytebuffer:sizebeforeunsynchronisation:" + bodyByteBuffer.size)

		// Unsynchronize if option enabled and unsync required
		isUnsynchronization =
			TagOptionSingleton.instance.isUnsyncTags && requiresUnsynchronization(bodyByteBuffer)
		if (isUnsynchronization) {
			bodyByteBuffer = unsynchronize(bodyByteBuffer)
			logger.config(loggingFilename + ":bodybytebuffer:sizeafterunsynchronisation:" + bodyByteBuffer.size)
		}
		var padding = 0
		if (currentTagSize > 0) {
			val sizeIncPadding =
				calculateTagSize(bodyByteBuffer.size + TAG_HEADER_LENGTH, currentTagSize)
			padding = sizeIncPadding - (bodyByteBuffer.size + TAG_HEADER_LENGTH)
			logger.config(
				"$loggingFilename:Padding:$padding"
			)
		}
		val headerBuffer = writeHeaderToBuffer(padding, bodyByteBuffer.size)
		channel!!.write(headerBuffer)
		channel.write(ByteBuffer.wrap(bodyByteBuffer))
		writePadding(channel, padding)
	}

	/**
	 * For representing the MP3File in an XML Format
	 */
	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_TAG, identifier.toString())
		super.createStructureHeader()

		//Header
		MP3File.structureFormatter?.openHeadingElement(TYPE_HEADER, "")
		MP3File.structureFormatter?.addElement(TYPE_UNSYNCHRONISATION, isUnsynchronization)
		MP3File.structureFormatter?.addElement(TYPE_EXTENDED, extended)
		MP3File.structureFormatter?.addElement(TYPE_EXPERIMENTAL, experimental)
		MP3File.structureFormatter?.addElement(TYPE_CRCDATA, crc32)
		MP3File.structureFormatter?.addElement(TYPE_PADDINGSIZE, paddingSize)
		MP3File.structureFormatter?.closeHeadingElement(TYPE_HEADER)
		//Body
		super.createStructureBody()
		MP3File.structureFormatter?.closeHeadingElement(TYPE_TAG)
	}

	override fun createFrame(id: String?): ID3v23Frame {
		return ID3v23Frame(id)
	}

	/**
	 * Create Frame for Id3 Key
	 *
	 * Only textual data supported at the moment, should only be used with frames that
	 * support a simple string argument.
	 *
	 * @param id3Key
	 * @param value
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun createField(id3Key: ID3v23FieldKey?, value: String?): TagField {
		if (id3Key == null) {
			throw KeyNotFoundException()
		}
		return super.doCreateTagField(FrameAndSubId(null, id3Key.frameId, id3Key.subId), value!!)
	}

	/**
	 * Retrieve the first value that exists for this id3v23key
	 *
	 * @param id3v23FieldKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(id3v23FieldKey: ID3v23FieldKey?): String? {
		if (id3v23FieldKey == null) {
			throw KeyNotFoundException()
		}
		val genericKey = instanceOf.getGenericKeyFromId3(id3v23FieldKey)
		return if (genericKey != null) {
			super.getFirst(genericKey)
		} else {
			val frameAndSubId: FrameAndSubId =
				FrameAndSubId(
					null,
					id3v23FieldKey.frameId,
					id3v23FieldKey.subId
				)
			super.doGetValueAtIndex(frameAndSubId, 0)
		}
	}

	/**
	 * Delete fields with this id3v23FieldKey
	 *
	 * @param id3v23FieldKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(id3v23FieldKey: ID3v23FieldKey?) {
		if (id3v23FieldKey == null) {
			throw KeyNotFoundException()
		}
		super.doDeleteTagField(FrameAndSubId(null, id3v23FieldKey.frameId, id3v23FieldKey.subId))
	}

	/**
	 * Delete fields with this (frame) id
	 * @param id
	 */
	override fun deleteField(id: String?) {
		super.doDeleteTagField(FrameAndSubId(null, id, null))
	}

	override fun getFrameAndSubIdFromGenericKey(genericKey: FieldKey?): FrameAndSubId {
		requireNotNull(genericKey) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val id3v23FieldKey = instanceOf.getId3KeyFromGenericKey(
			genericKey
		)
			?: throw KeyNotFoundException(genericKey.name)
		return FrameAndSubId(genericKey, id3v23FieldKey.frameId, id3v23FieldKey.subId)
	}

	override val iD3Frames: ID3Frames
		get() = instanceOf

	/**
	 * @return comparator used to order frames in preferred order for writing to file
	 * so that most important frames are written first.
	 */
	override val preferredFrameOrderComparator: Comparator<String>?
		get() = instanceof

	/**
	 * The list of `Artwork` if any, otherwise an empty List
	 */
	override val artworkList: List<Artwork>
		get() {
			val coverArtFields = getFields(FieldKey.COVER_ART) ?: return emptyList()
			val artworks = ArrayList<Artwork>(coverArtFields.size)

			// Collection<E>.forEach { element: E -> } is preferred for iteration however
			// for-Loop should be used instead for `continue&break` clarity
			for (artField in coverArtFields) {
				if (artField !is AbstractID3v2Frame) continue

				val body = artField.body
				if (body !is FrameBodyAPIC) continue

				AndroidArtwork().run {
					mimeType = body.mimeType
					pictureType = body.pictureType
					description = body.description

					if (body.isImageUrl) {
						isLinked = true
						imageUrl = body.imageUrl
					} else {
						binaryData = body.imageData
					}

					artworks.add(this)
				}
			}

			return artworks
		}

	/**
	 * {@inheritDoc}
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		val frame: AbstractID3v2Frame =
			createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).frameId)
		val body = frame.body as? FrameBodyAPIC
			?: return null
		return if (!artwork!!.isLinked) {
			body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, artwork.binaryData)
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, artwork.pictureType)
			body.setObjectValue(DataTypes.OBJ_MIME_TYPE, artwork.mimeType)
			body.setObjectValue(DataTypes.OBJ_DESCRIPTION, artwork.description)
			frame
		} else {
			try {
				body.setObjectValue(
					DataTypes.OBJ_PICTURE_DATA,
					artwork.imageUrl.toByteArray(charset("ISO-8859-1"))
				)
			} catch (uoe: UnsupportedEncodingException) {
				throw RuntimeException(uoe.message)
			}
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, artwork.pictureType)
			body.setObjectValue(DataTypes.OBJ_MIME_TYPE, FrameBodyAPIC.IMAGE_IS_URL)
			body.setObjectValue(DataTypes.OBJ_DESCRIPTION, artwork.description)
			frame
		}
	}

	/**
	 * Create Artwork
	 *
	 * @param data
	 * @param mimeType of the image
	 * @see PictureTypes
	 *
	 * @return
	 */
	fun createArtworkField(data: ByteArray?, mimeType: String?): TagField {
		val frame: AbstractID3v2Frame =
			createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).frameId)
		val body = frame.body as? FrameBodyAPIC ?: return frame
		body.setObjectValue(DataTypes.OBJ_PICTURE_DATA, data)
		body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID)
		body.setObjectValue(DataTypes.OBJ_MIME_TYPE, mimeType)
		body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		return frame
	}

	/**
	 * Overridden to allow special handling for mapping YEAR to TYER and TDAT Frames
	 *
	 * @param genericKey is the generic key
	 * @param values      to store
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		require(values[0] != null) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val value = values[0]
		return if (genericKey === FieldKey.GENRE) {
			requireNotNull(value) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			val formatKey = getFrameAndSubIdFromGenericKey(genericKey)
			val frame: AbstractID3v2Frame = createFrame(formatKey.frameId)
			val framebody = frame.body as FrameBodyTCON?
			framebody!!.setV23Format()
			if (TagOptionSingleton.instance.isWriteMp3GenresAsText) {
				framebody.text = value
			} else {
				framebody.text = FrameBodyTCON.convertGenericToID3v23Genre(value)
			}
			frame
		} else if (genericKey === FieldKey.YEAR) {
			if (value?.length == 1) {
				val tyer: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TYER)
				(tyer.body as AbstractFrameBodyTextInfo?)!!.text = "000$value"
				tyer
			} else if (value?.length == 2) {
				val tyer: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TYER)
				(tyer.body as AbstractFrameBodyTextInfo?)!!.text =
					"00$value"
				tyer
			} else if (value?.length == 3) {
				val tyer: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TYER)
				(tyer.body as AbstractFrameBodyTextInfo?)!!.text =
					"0$value"
				tyer
			} else if (value?.length == 4) {
				val tyer: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TYER)
				(tyer.body as AbstractFrameBodyTextInfo?)!!.text = value
				tyer
			} else if ((value?.length ?: 0) > 4) {
				val tyer: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TYER)
				(tyer.body as AbstractFrameBodyTextInfo?)!!.text = value?.substring(0, 4)
				if (value?.length ?: 0 >= 10) {
					//Have a full yyyy-mm-dd value that needs storing in two frames in ID3
					val month = value?.substring(5, 7)
					val day = value?.substring(8, 10)
					val tdat: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TDAT)
					(tdat.body as AbstractFrameBodyTextInfo?)!!.text = day + month
					val ag = TyerTdatAggregatedFrame()
					ag.addFrame(tyer)
					ag.addFrame(tdat)
					ag
				} else if (value?.length ?: 0 >= 7) {
					//TDAT frame requires both month and day so if we only have the month we just have to make
					//the day up
					val month = value?.substring(5, 7)
					val day = "01"
					val tdat: AbstractID3v2Frame = createFrame(ID3v23Frames.FRAME_ID_V3_TDAT)
					(tdat.body as AbstractFrameBodyTextInfo?)!!.text = day + month
					val ag = TyerTdatAggregatedFrame()
					ag.addFrame(tyer)
					ag.addFrame(tdat)
					ag
				} else {
					//We only have year data
					tyer
				}
			} else {
				null
			}
		} else {
			super.createField(genericKey, *values)
		}
	}

	/**
	 *
	 * @param genericKey
	 * @param n
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		return if (genericKey === FieldKey.YEAR) {
			val fields = getFrame(TyerTdatAggregatedFrame.ID_TYER_TDAT)
			val af =
				if (fields != null && !fields.isEmpty()) fields[0] as AggregatedFrame? else null
			if (af != null) {
				af.content
			} else {
				super.getValue(genericKey, n)
			}
		} else if (genericKey === FieldKey.GENRE) {
			val fields = getFields(genericKey)
			if (fields != null && fields.size > 0) {
				val frame = fields[0] as AbstractID3v2Frame?
				val body = frame!!.body as FrameBodyTCON?
				return FrameBodyTCON.convertID3v23GenreToGeneric(
					body!!.values[n]
				)
			}
			""
		} else {
			super.getValue(genericKey, n)
		}
	}

	override fun loadFrameIntoMap(frameId: String, next: AbstractID3v2Frame) {
		if (next.body is FrameBodyTCON) {
			(next.body as FrameBodyTCON?)!!.setV23Format()
		}
		super.loadFrameIntoMap(frameId, next)
	}

	override fun loadFrameIntoSpecifiedMap(
		map: MutableMap<String, MutableList<TagField?>>,
		frameId: String,
		frame: AbstractID3v2Frame?
	) {
		if (frameId != ID3v23Frames.FRAME_ID_V3_TYER && frameId != ID3v23Frames.FRAME_ID_V3_TDAT) {
			super.loadFrameIntoSpecifiedMap(map, frameId, frame)
			return
		}
		if (frameId == ID3v23Frames.FRAME_ID_V3_TDAT) {
			if (frame!!.content!!.length == 0) {
				//Discard not useful to complicate by trying to map it
				logger.warning(
					"$loggingFilename:TDAT is empty so just ignoring"
				)
				return
			}
		}
		if (map.containsKey(frameId) || map.containsKey(TyerTdatAggregatedFrame.ID_TYER_TDAT)) {
			//If we have multiple duplicate frames in a tag separate them with semicolons
			if (duplicateFrameId.length > 0) {
				duplicateFrameId += ";"
			}
			duplicateFrameId += frameId
			duplicateBytes += frame!!.size
		} else if (frameId == ID3v23Frames.FRAME_ID_V3_TYER) {
			if (map.containsKey(ID3v23Frames.FRAME_ID_V3_TDAT)) {
				val ag = TyerTdatAggregatedFrame()
				ag.addFrame(frame!!)
				for (field in map[ID3v23Frames.FRAME_ID_V3_TDAT]!!) {
					if (field is AbstractID3v2Frame) {
						ag.addFrame((field as AbstractID3v2Frame?)!!)
					}
				}
				map.remove(ID3v23Frames.FRAME_ID_V3_TDAT)
				putAsList(map, TyerTdatAggregatedFrame.ID_TYER_TDAT, ag)
			} else {
				putAsList(map, ID3v23Frames.FRAME_ID_V3_TYER, frame)
			}
		} else if (frameId == ID3v23Frames.FRAME_ID_V3_TDAT) {
			if (map.containsKey(ID3v23Frames.FRAME_ID_V3_TYER)) {
				val ag = TyerTdatAggregatedFrame()
				for (field in map[ID3v23Frames.FRAME_ID_V3_TYER]!!) {
					if (field is AbstractID3v2Frame) {
						ag.addFrame((field as AbstractID3v2Frame?)!!)
					}
				}
				ag.addFrame(frame!!)
				map.remove(ID3v23Frames.FRAME_ID_V3_TYER)
				putAsList(map, TyerTdatAggregatedFrame.ID_TYER_TDAT, ag)
			} else {
				putAsList(map, ID3v23Frames.FRAME_ID_V3_TDAT, frame)
			}
		}
	}

	private fun putAsList(
		map: MutableMap<String, MutableList<TagField?>>,
		id: String,
		tf: TagField?
	) {
		val fields: MutableList<TagField?> = ArrayList()
		fields.add(tf)
		map[id] = fields
	}

	/**
	 * Overridden because GENRE can need converting of data to ID3v23 format and
	 * YEAR key is specially processed by getFields() for ID3
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getAll(genericKey: FieldKey?): List<String?>? {
		return if (genericKey === FieldKey.GENRE) {
			val fields = getFields(genericKey)
			val convertedGenres: MutableList<String?> = ArrayList()
			if (fields != null && fields.size > 0) {
				val frame = fields[0] as AbstractID3v2Frame?
				val body = frame!!.body as FrameBodyTCON?
				for (next in body!!.values) {
					convertedGenres.add(FrameBodyTCON.convertID3v23GenreToGeneric(next))
				}
			}
			convertedGenres
		} else if (genericKey === FieldKey.YEAR) {
			val fields = getFields(genericKey)
			val results: MutableList<String?> = ArrayList()
			if (fields != null && fields.size > 0) {
				for (next in fields) {
					if (next is TagTextField) {
						results.add(next.content)
					}
				}
			}
			results
		} else {
			super.getAll(genericKey)
		}
	}

	/**
	 * Overridden because YEAR key can be served by TDAT, TYER or special aggreagted frame
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFields(genericKey: FieldKey?): List<TagField?>? {
		requireNotNull(genericKey) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		return if (genericKey === FieldKey.YEAR) {
			val fields =
				getFrame(TyerTdatAggregatedFrame.ID_TYER_TDAT)
			val af =
				if (fields != null && !fields.isEmpty()) fields[0] as AggregatedFrame? else null
			if (af != null) {
				val list: MutableList<TagField?> =
					ArrayList()
				list.add(af)
				list
			} else {
				super.getFields(genericKey)
			}
		} else {
			super.getFields(genericKey)
		}
	}

	/**
	 * Remove frame(s) with this identifier from tag
	 *
	 * @param identifier frameId to look for
	 */
	override fun removeFrame(identifier: String) {
		logger.config(
			"Removing frame with identifier:$identifier"
		)
		frameMap!!.remove(identifier)
		if (identifier == ID3v23Frames.FRAME_ID_V3_TYER) {
			frameMap!!.remove(ID3v23Frames.FRAME_ID_V3_TYER)
			frameMap!!.remove(TyerTdatAggregatedFrame.ID_TYER_TDAT)
		} else {
			frameMap!!.remove(identifier)
		}
	}

	companion object {
		protected const val TYPE_CRCDATA = "crcdata"
		protected const val TYPE_EXPERIMENTAL = "experimental"
		protected const val TYPE_EXTENDED = "extended"
		protected const val TYPE_PADDINGSIZE = "paddingsize"
		protected const val TYPE_UNSYNCHRONISATION = "unsyncronisation"
		protected var TAG_EXT_HEADER_LENGTH = 10
		protected var TAG_EXT_HEADER_CRC_LENGTH = 4
		protected var FIELD_TAG_EXT_SIZE_LENGTH = 4
		protected var TAG_EXT_HEADER_DATA_LENGTH = TAG_EXT_HEADER_LENGTH - FIELD_TAG_EXT_SIZE_LENGTH

		/**
		 * ID3v2.3 Header bit mask
		 */
		const val MASK_V23_UNSYNCHRONIZATION = FileConstants.BIT7

		/**
		 * ID3v2.3 Header bit mask
		 */
		const val MASK_V23_EXTENDED_HEADER = FileConstants.BIT6

		/**
		 * ID3v2.3 Header bit mask
		 */
		const val MASK_V23_EXPERIMENTAL = FileConstants.BIT5

		/**
		 * ID3v2.3 Extended Header bit mask
		 */
		const val MASK_V23_CRC_DATA_PRESENT = FileConstants.BIT7

		/**
		 * ID3v2.3 RBUF frame bit mask
		 */
		const val MASK_V23_EMBEDDED_INFO_FLAG = FileConstants.BIT1

		/**
		 * Retrieve the Release
		 */
		val RELEASE: Byte = 2

		/**
		 * Retrieve the Major Version
		 */
		val MAJOR_VERSION: Byte = 3

		/**
		 * Retrieve the Revision
		 */
		val REVISION: Byte = 0
	}
}

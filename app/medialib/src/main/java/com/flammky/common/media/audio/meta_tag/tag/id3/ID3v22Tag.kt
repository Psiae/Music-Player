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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Frames.Companion.instanceOf
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22PreferredFrameOrderComparator.Companion.instanceof
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats
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
 * Represents an ID3v2.2 tag.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
open class ID3v22Tag : ID3v2TagBase {
	override val release: Byte
		get() = Companion.RELEASE
	override val majorVersion: Byte
		get() = Companion.MAJOR_VERSION
	override val revision: Byte
		get() = Companion.REVISION

	/**
	 * @return is tag compressed
	 */
	/**
	 * The tag is compressed, although no compression scheme is defined in ID3v22
	 */
	var isCompression = false
		protected set
	/**
	 * @return is tag unsynchronized
	 */
	/**
	 * If set all frames in the tag uses unsynchronisation
	 */
	var isUnsynchronization = false
		protected set

	/**
	 * Creates a new empty ID3v2_2 tag.
	 */
	constructor() {
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()
	}

	/**
	 * Copy primitives applicable to v2.2
	 */
	override fun copyPrimitives(copyObj: ID3v2TagBase) {
		logger.config("Copying primitives")
		super.copyPrimitives(copyObj)

		//Set the primitive types specific to v2_2.
		if (copyObj is ID3v22Tag) {
			val copyObject = copyObj
			isCompression = copyObject.isCompression
			isUnsynchronization = copyObject.isUnsynchronization
		} else if (copyObj is ID3v23Tag) {
			val copyObject = copyObj
			isCompression = copyObject.compression
			isUnsynchronization = copyObject.isUnsynchronization
		} else if (copyObj is ID3v24Tag) {
			isCompression = false
			isUnsynchronization = copyObj.isUnsynchronization
		}
	}

	/**
	 * Copy Constructor, creates a new ID3v2_2 Tag based on another ID3v2_2 Tag
	 * @param copyObject
	 */
	constructor(copyObject: ID3v22Tag) : super(copyObject) {
		//This doesnt do anything.
		logger.config("Creating tag from another tag of same type")
		copyPrimitives(copyObject)
		copyFrames(copyObject)
	}

	/**
	 * Constructs a new tag based upon another tag of different version/type
	 * @param mp3tag
	 */
	constructor(mp3tag: AbstractTag?) {
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()
		logger.config("Creating tag from a tag of a different version")
		//Default Superclass constructor does nothing
		if (mp3tag != null) {
			val convertedTag: ID3v24Tag
			//Should use the copy constructor instead
			convertedTag =
				if (mp3tag !is ID3v23Tag && mp3tag is ID3v22Tag) {
					throw UnsupportedOperationException("Copy Constructor not called. Please type cast the argument")
				} else if (mp3tag is ID3v24Tag) {
					mp3tag
				} else {
					ID3v24Tag(mp3tag)
				}
			loggingFilename = convertedTag.loggingFilename
			//Set the primitive types specific to v2_2.
			copyPrimitives(convertedTag)
			//Set v2.2 Frames
			copyFrames(convertedTag)
			logger.config("Created tag from a tag of a different version")
		}
	}

	/**
	 * Creates a new ID3v2_2 datatype.
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
	 * Creates a new ID3v2_2 datatype.
	 *
	 * @param buffer
	 * @throws TagException
	 */
	@Deprecated("use {@link #ID3v22Tag(ByteBuffer,String)} instead")
	constructor(buffer: ByteBuffer) : this(buffer, "")

	override val identifier: String?
		get() = "ID3v2_2.20"

	/**
	 * Return frame size based upon the sizes of the frames rather than the size
	 * including padding recorded in the tag header
	 *
	 * @return size
	 */
	override val size: Int
		get() {
			var size = TAG_HEADER_LENGTH
			size += super.size
			return size
		}

	/**
	 * @param obj
	 * @return equality
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is ID3v22Tag) {
			return false
		}
		val `object` = obj
		return if (isCompression != `object`.isCompression) {
			false
		} else isUnsynchronization == `object`.isUnsynchronization && super.equals(
			obj
		)
	}

	@Throws(InvalidFrameException::class)
	override fun convertFrame(frame: AbstractID3v2Frame?): List<AbstractID3v2Frame?>? {
		val frames: MutableList<AbstractID3v2Frame?> = ArrayList()
		if (frame!!.identifier == ID3v24Frames.FRAME_ID_YEAR && frame.body is FrameBodyTDRC) {
			val tmpBody = frame.body as FrameBodyTDRC?
			var newFrame: ID3v22Frame
			if (tmpBody!!.year?.length != 0) {
				//Create Year frame (v2.2 id,but uses v2.3 body)
				newFrame = ID3v22Frame(ID3v22Frames.FRAME_ID_V2_TYER)
				(newFrame.body as AbstractFrameBodyTextInfo?)!!.text = tmpBody.year
				frames.add(newFrame)
			}
			if (tmpBody.time?.length != 0) {
				//Create Time frame (v2.2 id,but uses v2.3 body)
				newFrame = ID3v22Frame(ID3v22Frames.FRAME_ID_V2_TIME)
				(newFrame.body as AbstractFrameBodyTextInfo?)!!.text = tmpBody.time
				frames.add(newFrame)
			}
		} else {
			frames.add(ID3v22Frame(frame))
		}
		return frames
	}

	public override fun addFrame(frame: AbstractID3v2Frame?) {
		try {
			if (frame is ID3v22Frame) {
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

	/**
	 * Read tag Header Flags
	 *
	 * @param byteBuffer
	 * @throws TagException
	 */
	@Throws(TagException::class)
	private fun readHeaderFlags(byteBuffer: ByteBuffer?) {
		//Flags
		val flags = byteBuffer!!.get()
		isUnsynchronization = flags.toInt() and MASK_V22_UNSYNCHRONIZATION != 0
		isCompression = flags.toInt() and MASK_V22_COMPRESSION != 0
		if (isUnsynchronization) {
			logger.config(ErrorMessage.ID3_TAG_UNSYNCHRONIZED.getMsg(loggingFilename))
		}
		if (isCompression) {
			logger.config(ErrorMessage.ID3_TAG_COMPRESSED.getMsg(loggingFilename))
		}

		//Not allowable/Unknown Flags
		if (flags.toInt() and FileConstants.BIT5 != 0) {
			logger.warning(
				ErrorMessage.ID3_INVALID_OR_UNKNOWN_FLAG_SET.getMsg(
					loggingFilename,
					FileConstants.BIT5
				)
			)
		}
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
					FileConstants.BIT3
				)
			)
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(TagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val size: Int
		if (!seek(byteBuffer)) {
			throw TagNotFoundException("ID3v2.20 tag not found")
		}
		logger.config(
			"$loggingFilename:Reading tag from file"
		)

		//Read the flags
		readHeaderFlags(byteBuffer)

		// Read the size
		size = bufferToValue(
			byteBuffer
		)

		//Slice Buffer, so position markers tally with size (i.e do not include tagheader)
		var bufferWithoutHeader = byteBuffer.slice()

		//We need to synchronize the buffer
		if (isUnsynchronization) {
			bufferWithoutHeader = synchronize(bufferWithoutHeader)
		}
		readFrames(bufferWithoutHeader, size)
		logger.config(loggingFilename + ":" + "Loaded Frames,there are:" + frameMap!!.keys.size)
	}

	/**
	 * Read frames from tag
	 * @param byteBuffer
	 * @param size
	 */
	protected fun readFrames(byteBuffer: ByteBuffer, size: Int) {
		//Now start looking for frames
		var next: ID3v22Frame
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()

		//Read the size from the Tag Header
		fileReadBytes = size
		logger.finest(loggingFilename + ":" + "Start of frame body at:" + byteBuffer.position() + ",frames sizes and padding is:" + size)
		/* todo not done yet. Read the first Frame, there seems to be quite a
		 ** common case of extra data being between the tag header and the first
		 ** frame so should we allow for this when reading first frame, but not subsequent frames
		 */
		// Read the frames until got to upto the size as specified in header
		while (byteBuffer.position() < size) {
			try {
				//Read Frame
				logger.config(loggingFilename + ":" + "looking for next frame at:" + byteBuffer.position())
				next = ID3v22Frame(byteBuffer, loggingFilename)
				val id = next.identifier
				loadFrameIntoMap(id, next)
			} //Found Padding, no more frames
			catch (ex: PaddingException) {
				logger.config(loggingFilename + ":Found padding starting at:" + byteBuffer.position())
				break
			} //Found Empty Frame
			catch (ex: EmptyFrameException) {
				logger.warning(loggingFilename + ":" + "Empty Frame:" + ex.message)
				emptyFrameBytes += ID3v22Frame.FRAME_HEADER_SIZE
			} catch (ifie: InvalidFrameIdentifierException) {
				logger.config(loggingFilename + ":" + "Invalid Frame Identifier:" + ifie.message)
				invalidFrames++
				//Dont try and find any more frames
				break
			} //Problem trying to find frame
			catch (ife: InvalidFrameException) {
				logger.warning(loggingFilename + ":" + "Invalid Frame:" + ife.message)
				invalidFrames++
				//Dont try and find any more frames
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
	 * This is used when we need to translate a single frame into multiple frames,
	 * currently required for TDRC frames.
	 * @param frame
	 */
	//TODO will overwrite any existing TYER or TIME frame, do we ever want multiples of these
	protected fun translateFrame(frame: AbstractID3v2Frame) {
		val tmpBody = frame.body as FrameBodyTDRC?
		var newFrame: ID3v22Frame
		if (tmpBody?.year?.length != 0) {
			//Create Year frame (v2.2 id,but uses v2.3 body)
			newFrame = ID3v22Frame(ID3v22Frames.FRAME_ID_V2_TYER)
			(newFrame.body as AbstractFrameBodyTextInfo?)?.text = tmpBody?.year
			setFrame(newFrame)
		}
		if (tmpBody?.time?.length != 0) {
			//Create Time frame (v2.2 id,but uses v2.3 body)
			newFrame = ID3v22Frame(ID3v22Frames.FRAME_ID_V2_TIME)
			(newFrame.body as AbstractFrameBodyTextInfo?)?.text = tmpBody?.time
			setFrame(newFrame)
		}
	}

	/**
	 * Write the ID3 header to the ByteBuffer.
	 *
	 *
	 * @param padding
	 * @param size
	 * @return ByteBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeHeaderToBuffer(padding: Int, size: Int): ByteBuffer {
		isCompression = false

		//Create Header Buffer
		val headerBuffer = ByteBuffer.allocate(TAG_HEADER_LENGTH)

		//TAGID
		headerBuffer.put(TAG_ID)
		//Major Version
		headerBuffer.put(this.majorVersion)
		//Minor Version
		headerBuffer.put(this.revision)

		//Flags
		var flags = 0.toByte()
		if (isUnsynchronization) {
			flags = (flags.toInt() or MASK_V22_UNSYNCHRONIZATION.toByte()
				.toInt()).toByte()
		}
		if (isCompression) {
			flags = (flags.toInt() or MASK_V22_COMPRESSION.toByte()
				.toInt()).toByte()
		}
		headerBuffer.put(flags)

		//Size As Recorded in Header, don't include the main header length
		headerBuffer.put(valueToBuffer(padding + size))
		headerBuffer.flip()
		return headerBuffer
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun write(file: File?, audioStartLocation: Long): Long {
		loggingFilename = file!!.name
		logger.config(
			"Writing tag to file:$loggingFilename"
		)

		// Write Body Buffer
		var bodyByteBuffer = writeFramesToBuffer().toByteArray()

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
		logger.config("$loggingFilename:Current audiostart:$audioStartLocation")
		logger.config("$loggingFilename:Size including padding:$sizeIncPadding")
		logger.config("$loggingFilename:Padding:$padding")
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

		//Unsynchronize if option enabled and unsync required
		isUnsynchronization =
			TagOptionSingleton.instance.isUnsyncTags && requiresUnsynchronization(
				bodyByteBuffer
			)
		if (isUnsynchronization) {
			bodyByteBuffer = unsynchronize(bodyByteBuffer)
			logger.config(loggingFilename + ":bodybytebuffer:sizeafterunsynchronisation:" + bodyByteBuffer.size)
		}
		var padding = 0
		if (currentTagSize > 0) {
			val sizeIncPadding =
				calculateTagSize(bodyByteBuffer.size + TAG_HEADER_LENGTH, currentTagSize)
			padding = sizeIncPadding - (bodyByteBuffer.size + TAG_HEADER_LENGTH)
		}
		val headerBuffer = writeHeaderToBuffer(padding, bodyByteBuffer.size)
		channel!!.write(headerBuffer)
		channel.write(ByteBuffer.wrap(bodyByteBuffer))
		writePadding(channel, padding)
	}

	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_TAG, identifier.toString())
		super.createStructureHeader()

		//Header
		MP3File.structureFormatter?.openHeadingElement(TYPE_HEADER, "")
		MP3File.structureFormatter?.addElement(TYPE_COMPRESSION, isCompression)
		MP3File.structureFormatter?.addElement(TYPE_UNSYNCHRONISATION, isUnsynchronization)
		MP3File.structureFormatter?.closeHeadingElement(TYPE_HEADER)
		//Body
		super.createStructureBody()
		MP3File.structureFormatter?.closeHeadingElement(TYPE_TAG)
	}

	/**
	 * Create Frame
	 *
	 * @param id frameid
	 * @return
	 */
	override fun createFrame(id: String?): ID3v22Frame {
		return ID3v22Frame(id!!)
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
	fun createField(id3Key: ID3v22FieldKey?, value: String?): TagField {
		if (id3Key == null) {
			throw KeyNotFoundException()
		}
		return doCreateTagField(FrameAndSubId(null, id3Key.frameId, id3Key.subId), value!!)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}

		val values = values.filterNotNull().toTypedArray()

		requireNotNull(values) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val value = values[0]
		return if (genericKey === FieldKey.GENRE) {
			requireNotNull(value) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			val formatKey = getFrameAndSubIdFromGenericKey(genericKey)
			val frame: AbstractID3v2Frame =
				createFrame(formatKey.frameId)
			val framebody =
				frame.body as FrameBodyTCON?
			framebody!!.setV23Format()
			framebody.text =
				FrameBodyTCON.convertGenericToID3v22Genre(
					value
				)
			frame
		} else {
			super.createField(genericKey, *values)
		}
	}

	/**
	 * Retrieve the first value that exists for this id3v22key
	 *
	 * @param id3v22FieldKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(id3v22FieldKey: ID3v22FieldKey?): String? {
		if (id3v22FieldKey == null) {
			throw KeyNotFoundException()
		}
		val genericKey = instanceOf.getGenericKeyFromId3(id3v22FieldKey)
		return if (genericKey != null) {
			super.getFirst(genericKey)
		} else {
			val frameAndSubId: FrameAndSubId =
				FrameAndSubId(
					null,
					id3v22FieldKey.frameId,
					id3v22FieldKey.subId
				)
			super.doGetValueAtIndex(frameAndSubId, 0)
		}
	}

	/**
	 * Delete fields with this id3v22FieldKey
	 *
	 * @param id3v22FieldKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(id3v22FieldKey: ID3v22FieldKey?) {
		if (id3v22FieldKey == null) {
			throw KeyNotFoundException()
		}
		super.doDeleteTagField(FrameAndSubId(null, id3v22FieldKey.frameId, id3v22FieldKey.subId))
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
		val id3v22FieldKey = instanceOf.getId3KeyFromGenericKey(
			genericKey
		)
			?: throw KeyNotFoundException(genericKey.name)
		return FrameAndSubId(genericKey, id3v22FieldKey.frameId, id3v22FieldKey.subId)
	}

	override val iD3Frames: ID3Frames
		get() = instanceOf

	/**
	 *
	 * @return comparator used to order frames in preffrred order for writing to file
	 * so that most important frames are written first.
	 */
	override val preferredFrameOrderComparator: Comparator<String>
		get() = instanceof

	/**
	 * {@inheritDoc}
	 */
	override val artworkList: List<Artwork>
		get() {
			val coverartList = getFields(FieldKey.COVER_ART) ?: return emptyList()
			val artworkList: MutableList<Artwork> = ArrayList(coverartList.size)

			for (next in coverartList) {
				val coverArt = (next as? AbstractID3v2Frame)?.body as? FrameBodyPIC ?: continue
				val artwork = AndroidArtwork()
				artwork.mimeType = ImageFormats.getMimeTypeForFormat(coverArt.formatType) ?: ""
				artwork.pictureType = coverArt.pictureType
				artwork.description = coverArt.description ?: ""
				if (coverArt.isImageUrl) {
					artwork.isLinked = true
					artwork.imageUrl = coverArt.getImageUrl()
				} else {
					artwork.binaryData = coverArt.imageData
				}
				artworkList.add(artwork)
			}
			return artworkList
		}

	/**
	 * {@inheritDoc}
	 */
	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		val frame: AbstractID3v2Frame =
			createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).frameId)
		val body = frame.body as FrameBodyPIC?
		return if (!artwork!!.isLinked) {
			body!!.setObjectValue(DataTypes.OBJ_PICTURE_DATA, artwork.binaryData)
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, artwork.pictureType)
			body.setObjectValue(
				DataTypes.OBJ_IMAGE_FORMAT,
				ImageFormats.getFormatForMimeType(artwork.mimeType)
			)
			body.setObjectValue(DataTypes.OBJ_DESCRIPTION, artwork.description)
			frame
		} else {
			try {
				body!!.setObjectValue(
					DataTypes.OBJ_PICTURE_DATA,
					artwork.imageUrl.toByteArray(charset("ISO-8859-1"))
				)
			} catch (uoe: UnsupportedEncodingException) {
				throw RuntimeException(uoe.message)
			}
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, artwork.pictureType)
			body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, FrameBodyAPIC.IMAGE_IS_URL)
			body.setObjectValue(DataTypes.OBJ_DESCRIPTION, artwork.description)
			frame
		}
	}

	fun createArtworkField(data: ByteArray?, mimeType: String?): TagField {
		val frame: AbstractID3v2Frame =
			createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).frameId)
		val body = frame.body as FrameBodyPIC?
		body!!.setObjectValue(DataTypes.OBJ_PICTURE_DATA, data)
		body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID)
		body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, ImageFormats.getFormatForMimeType(mimeType))
		body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		return frame
	}

	/**
	 * Maps the generic key to the id3 key and return the list of values for this field as strings
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
					convertedGenres.add(FrameBodyTCON.convertID3v22GenreToGeneric(next))
				}
			}
			convertedGenres
		} else {
			super.getAll(genericKey)
		}
	}

	@Throws(KeyNotFoundException::class)
	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		return if (genericKey === FieldKey.GENRE) {
			val fields = getFields(genericKey)
			if (fields != null && fields.size > 0) {
				val frame = fields[0] as AbstractID3v2Frame?
				val body = frame!!.body as FrameBodyTCON?
				return FrameBodyTCON.convertID3v22GenreToGeneric(
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

	companion object {
		protected const val TYPE_COMPRESSION = "compression"
		protected const val TYPE_UNSYNCHRONISATION = "unsyncronisation"

		/**
		 * Bit mask to indicate tag is Unsychronization
		 */
		const val MASK_V22_UNSYNCHRONIZATION = FileConstants.BIT7

		/**
		 * Bit mask to indicate tag is compressed, although compression is not
		 * actually defined in v22 so just ignored
		 */
		const val MASK_V22_COMPRESSION = FileConstants.BIT6

		/**
		 * Retrieve the Release
		 */
		val RELEASE: Byte = 2

		/**
		 * Retrieve the Major Version
		 */
		val MAJOR_VERSION: Byte = 2

		/**
		 * Retrieve the Revision
		 */
		val REVISION: Byte = 0
	}
}

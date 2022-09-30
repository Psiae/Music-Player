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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.Pair
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.bufferToValue
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3SyncSafeInteger.valueToBuffer
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames.Companion.instanceOf
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24PreferredFrameOrderComparator.Companion.instanceof
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.AndroidArtwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3.AbstractLyrics3
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3.Lyrics3v2
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3.Lyrics3v2Field
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.util.logging.Level

/**
 * Represents an ID3v2.4 tag.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class ID3v24Tag : ID3v2TagBase {

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
	 * Are all frames within this tag unsynchronized
	 *
	 *
	 * Because synchronization occurs at the frame level it is not normally desirable to unsynchronize all frames
	 * and hence this flag is not normally set.
	 *
	 * @return are all frames within the tag unsynchronized
	 */
	/**
	 * All frames in the tag uses unsynchronisation
	 */
	var isUnsynchronization = false
		protected set

	/**
	 * CRC Checksum
	 */
	protected var crcData = 0

	/**
	 * Contains a footer
	 */
	protected var footer = false

	/**
	 * Tag is an update
	 */
	protected var updateTag = false

	/**
	 * Tag has restrictions
	 */
	protected var tagRestriction = false

	/**
	 * If Set Image encoding restrictions
	 *
	 * 0   No restrictions
	 * 1   Images are encoded only with PNG [PNG] or JPEG [JFIF].
	 */
	protected var imageEncodingRestriction: Byte = 0

	/**
	 * If set Image size restrictions
	 *
	 * 00  No restrictions
	 * 01  All images are 256x256 pixels or smaller.
	 * 10  All images are 64x64 pixels or smaller.
	 * 11  All images are exactly 64x64 pixels, unless required
	 * otherwise.
	 */
	protected var imageSizeRestriction: Byte = 0

	/**
	 * If set then Tag Size Restrictions
	 *
	 * 00   No more than 128 frames and 1 MB total tag size.
	 * 01   No more than 64 frames and 128 KB total tag size.
	 * 10   No more than 32 frames and 40 KB total tag size.
	 * 11   No more than 32 frames and 4 KB total tag size.
	 */
	protected var tagSizeRestriction: Byte = 0

	/**
	 * If set Text encoding restrictions
	 *
	 * 0    No restrictions
	 * 1    Strings are only encoded with ISO-8859-1 [ISO-8859-1] or
	 * UTF-8 [UTF-8].
	 */
	protected var textEncodingRestriction: Byte = 0

	/**
	 * Tag padding
	 */
	protected var paddingSize = 0

	/**
	 * If set Text fields size restrictions
	 *
	 * 00   No restrictions
	 * 01   No string is longer than 1024 characters.
	 * 10   No string is longer than 128 characters.
	 * 11   No string is longer than 30 characters.
	 *
	 * Note that nothing is said about how many bytes is used to
	 * represent those characters, since it is encoding dependent. If a
	 * text frame consists of more than one string, the sum of the
	 * strungs is restricted as stated.
	 */
	protected var textFieldSizeRestriction: Byte = 0

	/**
	 * Creates a new empty ID3v2_4 datatype.
	 */
	constructor() {
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()
	}

	/**
	 * Copy primitives applicable to v2.4, this is used when cloning a v2.4 datatype
	 * and other objects such as v2.3 so need to check instanceof
	 */
	override fun copyPrimitives(copyObj: ID3v2TagBase) {
		logger.config(
			"$loggingFilename:Copying primitives"
		)
		super.copyPrimitives(copyObj)
		if (copyObj is ID3v24Tag) {
			val copyObject = copyObj
			footer = copyObject.footer
			tagRestriction = copyObject.tagRestriction
			updateTag = copyObject.updateTag
			imageEncodingRestriction = copyObject.imageEncodingRestriction
			imageSizeRestriction = copyObject.imageSizeRestriction
			tagSizeRestriction = copyObject.tagSizeRestriction
			textEncodingRestriction = copyObject.textEncodingRestriction
			textFieldSizeRestriction = copyObject.textFieldSizeRestriction
		}
	}

	/**
	 * Copy the frame
	 *
	 * If the frame is already an ID3v24 frame we can add as is, if not we need to convert
	 * to id3v24 frame(s)
	 *
	 * @param frame
	 */
	public override fun addFrame(frame: AbstractID3v2Frame?) {
		try {
			if (frame is ID3v24Frame) {
				copyFrameIntoMap(frame.identifier, frame)
			} else {
				val frames = convertFrame(frame)
				for (next in frames!!) {
					copyFrameIntoMap(next!!.identifier, next)
				}
			}
		} catch (ife: InvalidFrameException) {
			logger.log(
				Level.SEVERE,
				loggingFilename + ":Unable to convert frame:" + frame!!.identifier
			)
		}
	}

	/**
	 * Convert frame into ID3v24 frame(s)
	 *
	 * @param frame
	 * @return
	 * @throws InvalidFrameException
	 */
	@Throws(InvalidFrameException::class)
	override fun convertFrame(frame: AbstractID3v2Frame?): List<AbstractID3v2Frame?>? {
		var frame = frame
		val frames: MutableList<AbstractID3v2Frame?> = ArrayList()
		if (frame is ID3v22Frame && frame.identifier == ID3v22Frames.FRAME_ID_V2_IPLS) {
			frame = ID3v23Frame(frame)
			frames.add(frame)
		} else if (frame is ID3v23Frame && frame.identifier == ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE) {
			val pairs = (frame.body as FrameBodyIPLS?)!!.pairing.getMapping()
			val pairsTipl: MutableList<Pair> = ArrayList()
			for (next in pairs) {
				pairsTipl.add(next)
			}
			if (pairsTipl.size > 0) {
				val tipl: AbstractID3v2Frame = ID3v24Frame(
					(frame as ID3v23Frame?)!!, ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE
				)
				val tiplBody = FrameBodyTIPL(
					frame.body!!.textEncoding, pairsTipl
				)
				tipl.body = tiplBody
				frames.add(tipl)
			}
		} else {
			frames.add(
				ID3v24Frame(
					frame!!
				)
			)
		}
		return frames
	}

	/**
	 * Two different frames both converted to TDRCFrames, now if this is the case one of them
	 * may have actually have been created as a FrameUnsupportedBody because TDRC is only
	 * supported in ID3v24, but is often created in v23 tags as well together with the valid TYER
	 * frame OR it might be that we have two v23 frames that map to TDRC such as TYER,TIME or TDAT
	 *
	 * @param newFrame
	 * @param existingFrame
	 */
	override fun combineFrames(newFrame: AbstractID3v2Frame?, existing: MutableList<TagField?>) {
		//We dont add this new frame we just add the contents to existing frame
		//
		if (newFrame!!.body is FrameBodyTDRC) {
			var existingFrame: AbstractID3v2Frame? = null
			val tfi = existing.iterator()
			while (tfi.hasNext()) {
				val tf = tfi.next()
				/*if (tf is FrameBodyUnsupported) {
					tfi.remove()
				}*/
				if (tf is AbstractID3v2Frame) {
					existingFrame = tf
					break
				}
			}
			if (existing.isEmpty()) {
				existing.add(newFrame)
				return
			}
			if (existingFrame!!.body is FrameBodyTDRC) {
				val body = existingFrame.body as FrameBodyTDRC?
				val newBody = newFrame.body as FrameBodyTDRC?

				//#304:Check for NullPointer, just ignore this frame
				if (newBody!!.originalID == null) {
					return
				}
				//Just add the data to the frame
				if (newBody.originalID == ID3v23Frames.FRAME_ID_V3_TYER) {
					body?.setYear(newBody.year)
				} else if (newBody.originalID == ID3v23Frames.FRAME_ID_V3_TDAT) {
					body?.setDate(newBody.date)
					body?.isMonthOnly = newBody.isMonthOnly
				} else if (newBody.originalID == ID3v23Frames.FRAME_ID_V3_TIME) {
					body?.setTime(newBody.time)
					body?.isHoursOnly = newBody.isHoursOnly
				}
				body!!.setObjectValue(DataTypes.OBJ_TEXT, body.formattedText)
			} else {
				//we just lose this frame, we have already got one with the correct id.
				logger.warning(loggingFilename + ":Found duplicate TDRC frame in invalid situation,discarding:" + newFrame.identifier)
			}
		} else {
			existing.add(newFrame)
		}
	}

	/**
	 * Copy Constructor, creates a new ID3v2_4 Tag based on another ID3v2_4 Tag
	 * @param copyObject
	 */
	constructor(copyObject: ID3v24Tag) {
		logger.config(
			"$loggingFilename:Creating tag from another tag of same type"
		)
		copyPrimitives(copyObject)
		copyFrames(copyObject)
	}

	/**
	 * Creates a new ID3v2_4 datatype based on another (non 2.4) tag
	 *
	 * @param mp3tag
	 */
	constructor(mp3tag: AbstractTag?) {
		logger.config(
			"$loggingFilename:Creating tag from a tag of a different version"
		)
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()
		if (mp3tag != null) {
			//Should use simpler copy constructor
			if (mp3tag is ID3v24Tag) {
				throw UnsupportedOperationException("$loggingFilename:Copy Constructor not called. Please type cast the argument")
			} else if (mp3tag is ID3v2TagBase) {
				loggingFilename = mp3tag.loggingFilename
				copyPrimitives(mp3tag)
				copyFrames((mp3tag as ID3v2TagBase?)!!)
			} else if (mp3tag is ID3v1Tag) {
				// convert id3v1 tags.
				val id3tag = mp3tag
				var newFrame: ID3v24Frame
				var newBody: AbstractID3v2FrameBody
				if (!id3tag.firstTitle.isNullOrEmpty()) {
					newBody = FrameBodyTIT2(0.toByte(), id3tag.firstTitle)
					newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_TITLE)
					newFrame.body = newBody
					setFrame(newFrame)
				}
				if (!id3tag.firstArtist.isNullOrEmpty()) {
					newBody = FrameBodyTPE1(0.toByte(), id3tag.firstArtist)
					newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_ARTIST)
					newFrame.body = newBody
					setFrame(newFrame)
				}
				if (!id3tag.firstAlbum.isNullOrEmpty()) {
					newBody = FrameBodyTALB(0.toByte(), id3tag.firstAlbum)
					newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_ALBUM)
					newFrame.body = newBody
					setFrame(newFrame)
				}
				if (!id3tag.firstYear.isNullOrEmpty()) {
					newBody = FrameBodyTDRC(0.toByte(), id3tag.firstYear)
					newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_YEAR)
					newFrame.body = newBody
					setFrame(newFrame)
				}
				if (!id3tag.firstComment.isNullOrEmpty()) {
					newBody = FrameBodyCOMM(0.toByte(), "ENG", "", id3tag.firstComment)
					newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_COMMENT)
					newFrame.body = newBody
					setFrame(newFrame)
				}
				if (id3tag.genre.toInt() and ID3v1Tag.BYTE_TO_UNSIGNED >= 0 && id3tag.genre.toInt() and ID3v1Tag.BYTE_TO_UNSIGNED != ID3v1Tag.BYTE_TO_UNSIGNED) {
					val genreId = id3tag.genre.toInt() and ID3v1Tag.BYTE_TO_UNSIGNED
					val genre =
						"(" + genreId + ") " + GenreTypes.instanceOf.getValueForId(genreId)
					newBody = FrameBodyTCON(0.toByte(), genre)
					newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_GENRE)
					newFrame.body = newBody
					setFrame(newFrame)
				}
				if (mp3tag is ID3v11Tag) {
					val id3tag2 = mp3tag
					if (id3tag2.track > 0) {
						newBody = FrameBodyTRCK(0.toByte(), id3tag2.track.toString())
						newFrame = ID3v24Frame(ID3v24Frames.FRAME_ID_TRACK)
						newFrame.body = newBody
						setFrame(newFrame)
					}
				}
			} else if (mp3tag is AbstractLyrics3) {
				//Put the conversion stuff in the individual frame code.
				val lyric: Lyrics3v2
				lyric = if (mp3tag is Lyrics3v2) {
					Lyrics3v2(mp3tag as Lyrics3v2?)
				} else {
					Lyrics3v2(mp3tag)
				}
				val iterator = lyric.iterator()
				var field: Lyrics3v2Field?
				var newFrame: ID3v24Frame
				while (iterator!!.hasNext()) {
					try {
						field = iterator.next()
						newFrame = ID3v24Frame(field!!)
						setFrame(newFrame)
					} catch (ex: InvalidTagException) {
						logger.warning(
							"$loggingFilename:Unable to convert Lyrics3 to v24 Frame:Frame Identifier"
						)
					}
				}
			}
		}
	}

	/**
	 * Creates a new ID3v2_4 datatype.
	 *
	 * @param buffer
	 * @param loggingFilename
	 * @throws TagException
	 */
	constructor(buffer: ByteBuffer, loggingFilename: String) {
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()
		this.loggingFilename = loggingFilename
		read(buffer)
	}

	/**
	 * Creates a new ID3v2_4 datatype.
	 *
	 * @param buffer
	 * @throws TagException
	 */
	@Deprecated("use {@link #ID3v24Tag(ByteBuffer,String)} instead")
	constructor(buffer: ByteBuffer) : this(buffer, "")

	override val identifier: String
		get() = "ID3v2.40"

	/**
	 * Return tag size based upon the sizes of the frames rather than the physical
	 * no of bytes between start of ID3Tag and start of Audio Data.
	 *
	 * @return size
	 */
	override val size: Int
		get() {
			var size = TAG_HEADER_LENGTH
			if (extended) {
				size += TAG_EXT_HEADER_LENGTH
				if (updateTag) {
					size += TAG_EXT_HEADER_UPDATE_LENGTH
				}
				if (crcDataFlag) {
					size += TAG_EXT_HEADER_CRC_LENGTH
				}
				if (tagRestriction) {
					size += TAG_EXT_HEADER_RESTRICTION_LENGTH
				}
			}
			size += super.size
			logger.finer(
				"$loggingFilename:Tag Size is$size"
			)
			return size
		}

	/**
	 * @param obj
	 * @return equality
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is ID3v24Tag) {
			return false
		}
		val `object` = obj
		if (footer != `object`.footer) {
			return false
		}
		if (imageEncodingRestriction != `object`.imageEncodingRestriction) {
			return false
		}
		if (imageSizeRestriction != `object`.imageSizeRestriction) {
			return false
		}
		if (tagRestriction != `object`.tagRestriction) {
			return false
		}
		if (tagSizeRestriction != `object`.tagSizeRestriction) {
			return false
		}
		if (textEncodingRestriction != `object`.textEncodingRestriction) {
			return false
		}
		return if (textFieldSizeRestriction != `object`.textFieldSizeRestriction) {
			false
		} else updateTag == `object`.updateTag && super.equals(obj)
	}

	/**
	 * Read header flags
	 *
	 *
	 * Log info messages for falgs that have been set and log warnings when bits have been set for unknown flags
	 *
	 * @param byteBuffer
	 * @throws TagException
	 */
	@Throws(TagException::class)
	private fun readHeaderFlags(byteBuffer: ByteBuffer?) {
		//Flags
		val flags = byteBuffer!!.get()
		isUnsynchronization = flags.toInt() and MASK_V24_UNSYNCHRONIZATION != 0
		extended = flags.toInt() and MASK_V24_EXTENDED_HEADER != 0
		experimental = flags.toInt() and MASK_V24_EXPERIMENTAL != 0
		footer = flags.toInt() and MASK_V24_FOOTER_PRESENT != 0

		//Not allowable/Unknown Flags
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
		if (footer) {
			logger.warning(ErrorMessage.ID3_TAG_FOOTER.getMsg(loggingFilename))
		}
	}

	/**
	 * Read the optional extended header
	 *
	 * @param byteBuffer
	 * @param size
	 * @throws InvalidTagException
	 */
	@Throws(InvalidTagException::class)
	private fun readExtendedHeader(byteBuffer: ByteBuffer?, size: Int) {
		var buffer: ByteArray

		// int is 4 bytes.
		val extendedHeaderSize = byteBuffer!!.int

		// the extended header must be at least 6 bytes
		if (extendedHeaderSize <= TAG_EXT_HEADER_LENGTH) {
			throw InvalidTagException(
				ErrorMessage.ID3_EXTENDED_HEADER_SIZE_TOO_SMALL.getMsg(
					loggingFilename,
					extendedHeaderSize
				)
			)
		}

		//Number of bytes
		byteBuffer.get()

		// Read the extended flag bytes
		val extFlag = byteBuffer.get()
		updateTag = extFlag.toInt() and MASK_V24_TAG_UPDATE != 0
		crcDataFlag = extFlag.toInt() and MASK_V24_CRC_DATA_PRESENT != 0
		tagRestriction = extFlag.toInt() and MASK_V24_TAG_RESTRICTIONS != 0

		// read the length byte if the flag is set
		// this tag should always be zero but just in case
		// read this information.
		if (updateTag) {
			byteBuffer.get()
		}

		//CRC-32
		if (crcDataFlag) {
			// the CRC has a variable length
			byteBuffer.get()
			buffer = ByteArray(TAG_EXT_HEADER_CRC_DATA_LENGTH)
			byteBuffer[buffer, 0, TAG_EXT_HEADER_CRC_DATA_LENGTH]
			crcData = 0
			for (i in 0 until TAG_EXT_HEADER_CRC_DATA_LENGTH) {
				crcData = crcData shl 8
				crcData += buffer[i].toInt()
			}
		}

		//Tag Restriction
		if (tagRestriction) {
			byteBuffer.get()
			buffer = ByteArray(1)
			byteBuffer[buffer, 0, 1]
			tagSizeRestriction =
				(buffer[0].toInt() and MASK_V24_TAG_SIZE_RESTRICTIONS shr 6).toByte()
			textEncodingRestriction =
				(buffer[0].toInt() and MASK_V24_TEXT_ENCODING_RESTRICTIONS shr 5).toByte()
			textFieldSizeRestriction =
				(buffer[0].toInt() and MASK_V24_TEXT_FIELD_SIZE_RESTRICTIONS shr 3).toByte()
			imageEncodingRestriction =
				(buffer[0].toInt() and MASK_V24_IMAGE_ENCODING shr 2).toByte()
			imageSizeRestriction = (buffer[0].toInt() and MASK_V24_IMAGE_SIZE_RESTRICTIONS).toByte()
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(TagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val size: Int
		if (!seek(byteBuffer)) {
			throw TagNotFoundException(loggingFilename + ":" + identifier + " tag not found")
		}
		readHeaderFlags(byteBuffer)

		// Read the size, this is size of tag apart from tag header
		size = bufferToValue(
			byteBuffer
		)
		logger.config(
			"$loggingFilename:Reading tag from file size set in header is:$size"
		)
		if (extended) {
			readExtendedHeader(byteBuffer, size)
		}

		//Note if there was an extended header the size value has padding taken
		//off so we dont search it.
		readFrames(byteBuffer, size)
	}

	/**
	 * Read frames from tag
	 * @param byteBuffer
	 * @param size
	 */
	protected fun readFrames(byteBuffer: ByteBuffer?, size: Int) {
		logger.finest(loggingFilename + ":" + "Start of frame body at" + byteBuffer!!.position())
		//Now start looking for frames
		var next: ID3v24Frame
		frameMap = linkedMapOf()
		encryptedFrameMap = linkedMapOf()

		//Read the size from the Tag Header
		fileReadBytes = size
		// Read the frames until got to upto the size as specified in header
		logger.finest(loggingFilename + ":" + "Start of frame body at:" + byteBuffer.position() + ",frames data size is:" + size)
		while (byteBuffer.position() <= size) {
			var id: String
			try {
				//Read Frame
				logger.config(loggingFilename + ":" + "looking for next frame at:" + byteBuffer.position())
				next = ID3v24Frame(byteBuffer, loggingFilename)
				id = next.identifier
				loadFrameIntoMap(id, next)
			} //Found Padding, no more frames
			catch (ex: PaddingException) {
				logger.config(loggingFilename + ":Found padding starting at:" + byteBuffer.position())
				break
			} //Found Empty Frame
			catch (ex: EmptyFrameException) {
				logger.warning(loggingFilename + ":" + "Empty Frame:" + ex.message)
				emptyFrameBytes += TAG_HEADER_LENGTH
			} catch (ifie: InvalidFrameIdentifierException) {
				logger.config(loggingFilename + ":" + "Invalid Frame Identifier:" + ifie.message)
				invalidFrames++
				//Don't try and find any more frames
				break
			} //Problem trying to find frame
			catch (ife: InvalidFrameException) {
				logger.warning(loggingFilename + ":" + "Invalid Frame:" + ife.message)
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
	 * @param padding is the size of the padding
	 * @param size    is the size of the body data
	 * @return ByteBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeHeaderToBuffer(padding: Int, size: Int): ByteBuffer {
		//This would only be set if every frame in tag has been unsynchronized, I only unsychronize frames
		//that need it, in any case I have been advised not to set it even then.
		isUnsynchronization = false

		// Flags,currently we never calculate the CRC
		// and if we dont calculate them cant keep orig values. Tags are not
		// experimental and we never create extended header to keep things simple.
		extended = false
		experimental = false
		footer = false

		// Create Header Buffer,allocate maximum possible size for the header
		val headerBuffer = ByteBuffer.allocate(TAG_HEADER_LENGTH)
		//TAGID
		headerBuffer.put(TAG_ID)

		//Major Version
		headerBuffer.put(this.majorVersion)

		//Minor Version
		headerBuffer.put(this.revision)

		//Flags
		var flagsByte: Byte = 0
		if (isUnsynchronization) {
			flagsByte = (flagsByte.toInt() or MASK_V24_UNSYNCHRONIZATION).toByte()
		}
		if (extended) {
			flagsByte = (flagsByte.toInt() or MASK_V24_EXTENDED_HEADER).toByte()
		}
		if (experimental) {
			flagsByte = (flagsByte.toInt() or MASK_V24_EXPERIMENTAL).toByte()
		}
		if (footer) {
			flagsByte = (flagsByte.toInt() or MASK_V24_FOOTER_PRESENT).toByte()
		}
		headerBuffer.put(flagsByte)

		//Size As Recorded in Header, don't include the main header length
		//Additional Header Size,(for completeness we never actually write the extended header, or footer)
		var additionalHeaderSize = 0
		if (extended) {
			additionalHeaderSize += TAG_EXT_HEADER_LENGTH
			if (updateTag) {
				additionalHeaderSize += TAG_EXT_HEADER_UPDATE_LENGTH
			}
			if (crcDataFlag) {
				additionalHeaderSize += TAG_EXT_HEADER_CRC_LENGTH
			}
			if (tagRestriction) {
				additionalHeaderSize += TAG_EXT_HEADER_RESTRICTION_LENGTH
			}
		}

		//Size As Recorded in Header, don't include the main header length
		headerBuffer.put(valueToBuffer(padding + size + additionalHeaderSize))

		//Write Extended Header
		var extHeaderBuffer: ByteBuffer? = null
		if (extended) {
			//Write Extended Header Size
			var extendedSize = TAG_EXT_HEADER_LENGTH
			if (updateTag) {
				extendedSize += TAG_EXT_HEADER_UPDATE_LENGTH
			}
			if (crcDataFlag) {
				extendedSize += TAG_EXT_HEADER_CRC_LENGTH
			}
			if (tagRestriction) {
				extendedSize += TAG_EXT_HEADER_RESTRICTION_LENGTH
			}
			extHeaderBuffer = ByteBuffer.allocate(extendedSize)
			extHeaderBuffer.putInt(extendedSize)
			//Write Number of flags Byte
			extHeaderBuffer.put(TAG_EXT_NUMBER_BYTES_DATA_LENGTH.toByte())
			//Write Extended Flags
			var extFlag: Byte = 0
			if (updateTag) {
				extFlag = (extFlag.toInt() or MASK_V24_TAG_UPDATE).toByte()
			}
			if (crcDataFlag) {
				extFlag = (extFlag.toInt() or MASK_V24_CRC_DATA_PRESENT).toByte()
			}
			if (tagRestriction) {
				extFlag = (extFlag.toInt() or MASK_V24_TAG_RESTRICTIONS).toByte()
			}
			extHeaderBuffer.put(extFlag)
			//Write Update Data
			if (updateTag) {
				extHeaderBuffer.put(0.toByte())
			}
			//Write CRC Data
			if (crcDataFlag) {
				extHeaderBuffer.put(TAG_EXT_HEADER_CRC_DATA_LENGTH.toByte())
				extHeaderBuffer.put(0.toByte())
				extHeaderBuffer.putInt(crcData)
			}
			//Write Tag Restriction
			if (tagRestriction) {
				extHeaderBuffer.put(TAG_EXT_HEADER_RESTRICTION_DATA_LENGTH.toByte())
				//todo not currently setting restrictions
				extHeaderBuffer.put(0.toByte())
			}
		}
		if (extHeaderBuffer != null) {
			extHeaderBuffer.flip()
			headerBuffer.put(extHeaderBuffer)
		}
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
			"$loggingFilename:Writing tag to file:"
		)

		//Write Body Buffer
		val bodyByteBuffer = writeFramesToBuffer().toByteArray()

		//Calculate Tag Size including Padding
		val sizeIncPadding =
			calculateTagSize(bodyByteBuffer.size + TAG_HEADER_LENGTH, audioStartLocation.toInt())

		//Calculate padding bytes required
		val padding = sizeIncPadding - (bodyByteBuffer.size + TAG_HEADER_LENGTH)
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
		val bodyByteBuffer = writeFramesToBuffer().toByteArray()
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

	/**
	 * Display the tag in an XMLFormat
	 */
	override fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_TAG, identifier)
		super.createStructureHeader()

		//Header
		MP3File.structureFormatter?.openHeadingElement(TYPE_HEADER, "")
		MP3File.structureFormatter?.addElement(TYPE_UNSYNCHRONISATION, isUnsynchronization)
		MP3File.structureFormatter?.addElement(TYPE_CRCDATA, crcData)
		MP3File.structureFormatter?.addElement(TYPE_EXPERIMENTAL, experimental)
		MP3File.structureFormatter?.addElement(TYPE_EXTENDED, extended)
		MP3File.structureFormatter?.addElement(TYPE_PADDINGSIZE, paddingSize)
		MP3File.structureFormatter?.addElement(TYPE_FOOTER, footer)
		MP3File.structureFormatter?.addElement(TYPE_IMAGEENCODINGRESTRICTION, paddingSize)
		MP3File.structureFormatter?.addElement(TYPE_IMAGESIZERESTRICTION, imageSizeRestriction.toInt())
		MP3File.structureFormatter?.addElement(TYPE_TAGRESTRICTION, tagRestriction)
		MP3File.structureFormatter?.addElement(TYPE_TAGSIZERESTRICTION, tagSizeRestriction.toInt())
		MP3File.structureFormatter?.addElement(
			TYPE_TEXTFIELDSIZERESTRICTION,
			textFieldSizeRestriction.toInt()
		)
		MP3File.structureFormatter?.addElement(
			TYPE_TEXTENCODINGRESTRICTION,
			textEncodingRestriction.toInt()
		)
		MP3File.structureFormatter?.addElement(TYPE_UPDATETAG, updateTag)
		MP3File.structureFormatter?.closeHeadingElement(TYPE_HEADER)

		//Body
		super.createStructureBody()
		MP3File.structureFormatter?.closeHeadingElement(TYPE_TAG)
	}

	/**
	 * Create a new frame with the specified frameid
	 *
	 * @param id
	 * @return
	 */
	override fun createFrame(id: String?): ID3v24Frame {
		return ID3v24Frame(id)
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
	fun createField(id3Key: ID3v24FieldKey?, value: String?): TagField {
		if (id3Key == null) {
			throw KeyNotFoundException()
		}
		return super.doCreateTagField(FrameAndSubId(null, id3Key.frameId, id3Key.subId), value!!)
	}

	/**
	 * Retrieve the first value that exists for this id3v24key
	 *
	 * @param id3v24FieldKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(id3v24FieldKey: ID3v24FieldKey?): String? {
		if (id3v24FieldKey == null) {
			throw KeyNotFoundException()
		}
		val genericKey = instanceOf.getGenericKeyFromId3(id3v24FieldKey)
		return if (genericKey != null) {
			super.getFirst(genericKey)
		} else {
			val frameAndSubId: FrameAndSubId =
				FrameAndSubId(
					null,
					id3v24FieldKey.frameId,
					id3v24FieldKey.subId
				)
			super.doGetValueAtIndex(frameAndSubId, 0)
		}
	}

	/**
	 * Delete fields with this id3v24FieldKey
	 *
	 * @param id3v24FieldKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(id3v24FieldKey: ID3v24FieldKey?) {
		if (id3v24FieldKey == null) {
			throw KeyNotFoundException()
		}
		super.doDeleteTagField(FrameAndSubId(null, id3v24FieldKey.frameId, id3v24FieldKey.subId))
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
		val id3v24FieldKey = instanceOf.getId3KeyFromGenericKey(
			genericKey
		)
			?: throw KeyNotFoundException(genericKey.name)
		return FrameAndSubId(genericKey, id3v24FieldKey.frameId, id3v24FieldKey.subId)
	}

	override val iD3Frames: ID3Frames
		get() = instanceOf

	/**
	 * @return comparator used to order frames in preferred order for writing to file
	 * so that most important frames are written first.
	 */
	override val preferredFrameOrderComparator: Comparator<String>?
		get() = instanceof
	override val artworkList: List<Artwork>
		get() {
			val coverartList = getFields(FieldKey.COVER_ART)
			val artworkList: MutableList<Artwork> = ArrayList(
				coverartList!!.size
			)
			for (next in coverartList) {
				val coverArt = (next as AbstractID3v2Frame?)?.body as? FrameBodyAPIC ?: continue
				val artwork = AndroidArtwork()
				artwork.mimeType = coverArt.mimeType
				artwork.pictureType = coverArt.pictureType
				artwork.description = coverArt.description
				if (coverArt.isImageUrl) {
					artwork.isLinked = true
					artwork.imageUrl = coverArt.imageUrl
				} else {
					artwork.binaryData = coverArt.imageData
				}
				artworkList.add(artwork)
			}
			return artworkList
		}

	@Throws(FieldDataInvalidException::class)
	override fun createField(artwork: Artwork?): TagField? {
		val frame: AbstractID3v2Frame =
			createFrame(getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).frameId)
		val body = frame.body as FrameBodyAPIC?
		return if (!artwork!!.isLinked) {
			body!!.setObjectValue(DataTypes.OBJ_PICTURE_DATA, artwork.binaryData)
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, artwork.pictureType)
			body.setObjectValue(DataTypes.OBJ_MIME_TYPE, artwork.mimeType)
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
		val body = frame.body as FrameBodyAPIC?
		body!!.setObjectValue(DataTypes.OBJ_PICTURE_DATA, data)
		body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID)
		body.setObjectValue(DataTypes.OBJ_MIME_TYPE, mimeType)
		body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		return frame
	}

	/**
	 * Overridden for special Genre support
	 *
	 * @param genericKey is the generic key
	 * @param values
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		return if (genericKey === FieldKey.GENRE) {
			requireNotNull(values) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			requireNotNull(values[0]) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			val value = values[0]
			val formatKey = getFrameAndSubIdFromGenericKey(genericKey)
			val frame: AbstractID3v2Frame =
				createFrame(formatKey.frameId)
			val framebody =
				frame.body as FrameBodyTCON?
			if (TagOptionSingleton.instance.isWriteMp3GenresAsText) {
				framebody!!.text = value
			} else {
				framebody!!.text = FrameBodyTCON.convertGenericToID3v24Genre(value!!)
			}
			frame
		} else {
			super.createField(genericKey, *values)
		}
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
					convertedGenres.add(FrameBodyTCON.convertID3v24GenreToGeneric(next))
				}
			}
			convertedGenres
		} else {
			super.getAll(genericKey)
		}
	}

	/**
	 * Retrieve the value that exists for this generic key and this index
	 *
	 * Have to do some special mapping for certain generic keys because they share frame
	 * with another generic key.
	 *
	 * @param genericKey
	 * @return
	 */
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
				return FrameBodyTCON.convertID3v24GenreToGeneric(
					body!!.values[n]
				)
			}
			""
		} else {
			super.getValue(genericKey, n)
		}
	}

	companion object {
		protected const val TYPE_FOOTER = "footer"
		protected const val TYPE_IMAGEENCODINGRESTRICTION = "imageEncodingRestriction"
		protected const val TYPE_IMAGESIZERESTRICTION = "imageSizeRestriction"
		protected const val TYPE_TAGRESTRICTION = "tagRestriction"
		protected const val TYPE_TAGSIZERESTRICTION = "tagSizeRestriction"
		protected const val TYPE_TEXTENCODINGRESTRICTION = "textEncodingRestriction"
		protected const val TYPE_TEXTFIELDSIZERESTRICTION = "textFieldSizeRestriction"
		protected const val TYPE_UPDATETAG = "updateTag"
		protected const val TYPE_CRCDATA = "crcdata"
		protected const val TYPE_EXPERIMENTAL = "experimental"
		protected const val TYPE_EXTENDED = "extended"
		protected const val TYPE_PADDINGSIZE = "paddingsize"
		protected const val TYPE_UNSYNCHRONISATION = "unsyncronisation"
		protected var TAG_EXT_HEADER_LENGTH = 6
		protected var TAG_EXT_HEADER_UPDATE_LENGTH = 1
		protected var TAG_EXT_HEADER_CRC_LENGTH = 6
		protected var TAG_EXT_HEADER_RESTRICTION_LENGTH = 2
		protected var TAG_EXT_HEADER_CRC_DATA_LENGTH = 5
		protected var TAG_EXT_HEADER_RESTRICTION_DATA_LENGTH = 1
		protected var TAG_EXT_NUMBER_BYTES_DATA_LENGTH = 1

		/**
		 * ID3v2.4 Header bit mask
		 */
		const val MASK_V24_UNSYNCHRONIZATION = FileConstants.BIT7

		/**
		 * ID3v2.4 Header bit mask
		 */
		const val MASK_V24_EXTENDED_HEADER = FileConstants.BIT6

		/**
		 * ID3v2.4 Header bit mask
		 */
		const val MASK_V24_EXPERIMENTAL = FileConstants.BIT5

		/**
		 * ID3v2.4 Header bit mask
		 */
		const val MASK_V24_FOOTER_PRESENT = FileConstants.BIT4

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_TAG_UPDATE = FileConstants.BIT6

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_CRC_DATA_PRESENT = FileConstants.BIT5

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_TAG_RESTRICTIONS = FileConstants.BIT4

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_TAG_SIZE_RESTRICTIONS =
			FileConstants.BIT7.toByte().toInt() or FileConstants.BIT6

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_TEXT_ENCODING_RESTRICTIONS = FileConstants.BIT5

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_TEXT_FIELD_SIZE_RESTRICTIONS = FileConstants.BIT4 or FileConstants.BIT3

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_IMAGE_ENCODING = FileConstants.BIT2

		/**
		 * ID3v2.4 Extended header bit mask
		 */
		const val MASK_V24_IMAGE_SIZE_RESTRICTIONS = FileConstants.BIT2 or FileConstants.BIT1
		/**
		 * ID3v2.4 Header Footer are the same as the header flags. WHY?!?! move the
		 * flags from thier position in 2.3??????????
		 */
		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_TAG_ALTER_PRESERVATION = FileConstants.BIT6

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_FILE_ALTER_PRESERVATION = FileConstants.BIT5

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_READ_ONLY = FileConstants.BIT4

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_GROUPING_IDENTITY = FileConstants.BIT6

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_COMPRESSION = FileConstants.BIT4

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_ENCRYPTION = FileConstants.BIT3

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_FRAME_UNSYNCHRONIZATION = FileConstants.BIT2

		/**
		 * ID3v2.4 Header Footer bit mask
		 */
		const val MASK_V24_DATA_LENGTH_INDICATOR = FileConstants.BIT1

		/**
		 * Retrieve the Release
		 */
		val RELEASE: Byte = 2

		/**
		 * Retrieve the Major Version
		 */
		val MAJOR_VERSION: Byte = 4

		/**
		 * Retrieve the Revision
		 */
		val REVISION: Byte = 0
	}
}

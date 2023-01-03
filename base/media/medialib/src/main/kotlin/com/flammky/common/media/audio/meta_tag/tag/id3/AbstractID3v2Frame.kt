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

import com.flammky.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyTOAL
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.logging.Level

/**
 * This abstract class is each frame header inside a ID3v2 tag.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
abstract class AbstractID3v2Frame : AbstractTagFrame, TagTextField {
	/**
	 * Return the frame identifier
	 *
	 * @return the frame identifier
	 */
	//Frame identifier
	override var identifier = ""
		protected set

	//Frame Size
	@JvmField
	protected var frameSize = 0
	/**
	 * Retrieve the logging filename to be used in debugging
	 *
	 * @return logging filename to be used in debugging
	 */
	/**
	 * Set logging filename when construct tag for read from file
	 *
	 * @param loggingFilename
	 */
	//The purpose of this is to provide the filename that should be used when writing debug messages
	//when problems occur reading or writing to file, otherwise it is difficult to track down the error
	//when processing many files
	var loggingFilename = ""

	/**
	 *
	 * @return size in bytes of the frameid field
	 */
	protected abstract val frameIdSize: Int

	/**
	 *
	 * @return the size in bytes of the frame size field
	 */
	protected abstract val frameSizeSize: Int

	/**
	 *
	 * @return the size in bytes of the frame header
	 */
	protected abstract val frameHeaderSize: Int

	/**
	 * Create an empty frame
	 */
	protected constructor()

	/**
	 * This holds the Status flags (not supported in v2.20
	 */
	open var statusFlags: StatusFlags? = null

	/**
	 * This holds the Encoding flags (not supported in v2.20)
	 */
	open var encodingFlags: EncodingFlags? = null

	/**
	 * Create a frame based on another frame
	 * @param frame
	 */
	constructor(frame: AbstractID3v2Frame?) : super(
		frame!!
	)

	/**
	 * Create a frame based on a body
	 * @param body
	 */
	constructor(body: AbstractID3v2FrameBody?) {
		frameBody = body
		frameBody!!.header = this
	}

	/**
	 * Create a new frame with empty body based on identifier
	 * @param identifier
	 */
	//TODO the identifier checks should be done in the relevent subclasses
	constructor(identifier: String) {
		logger.config(
			"Creating empty frame of type$identifier"
		)
		this.identifier = identifier

		// Use reflection to map id to frame body, which makes things much easier
		// to keep things up to date.
		frameBody = try {
			val prefix = "com.kylentt.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBody"

			val c = Class.forName("$prefix$identifier") as Class<AbstractID3v2FrameBody>
			c.newInstance()
		} catch (cnfe: ClassNotFoundException) {
			logger.severe(cnfe.message)
			FrameBodyUnsupported(identifier)
		} //Instantiate Interface/Abstract should not happen
		catch (ie: InstantiationException) {
			logger.log(Level.SEVERE, "InstantiationException:$identifier", ie)
			throw RuntimeException(ie)
		} //Private Constructor shouild not happen
		catch (iae: IllegalAccessException) {
			logger.log(Level.SEVERE, "IllegalAccessException:$identifier", iae)
			throw RuntimeException(iae)
		}
		frameBody!!.header = this
		if (this is ID3v24Frame) {
			frameBody!!.textEncoding =
				TagOptionSingleton.instance.getId3v24DefaultTextEncoding()
		} else if (this is ID3v23Frame) {
			frameBody!!.textEncoding =
				TagOptionSingleton.instance.getId3v23DefaultTextEncoding()
		}
		logger.config(
			"Created empty frame of type$identifier"
		)
	}

	/**
	 * Return the frame identifier, this only identifies the frame it does not provide a unique
	 * key, when using frames such as TXXX which are used by many fields     *
	 *
	 * @return the frame identifier (Tag Field Interface)
	 */
	//TODO, this is confusing only returns the frameId, which does not neccessarily uniquely
	//identify the frame
	override val id: String
		get() = identifier

	//TODO:needs implementing but not sure if this method is required at all
	override fun copyContent(field: TagField?) {}

	/**
	 * Read the frameBody when frame marked as encrypted
	 *
	 * @param identifier
	 * @param byteBuffer
	 * @param frameSize
	 * @return
	 * @throws InvalidFrameException
	 * @throws InvalidDataTypeException
	 * @throws InvalidTagException
	 */
	@Throws(InvalidFrameException::class, InvalidDataTypeException::class)
	protected fun readEncryptedBody(
		identifier: String?,
		byteBuffer: ByteBuffer,
		frameSize: Int
	): AbstractID3v2FrameBody {
		return try {
			val frameBody: AbstractID3v2FrameBody = FrameBodyEncrypted(identifier, byteBuffer, frameSize)
			frameBody.header = this
			frameBody
		} catch (ite: InvalidTagException) {
			throw InvalidDataTypeException(ite)
		}
	}

	protected open fun isPadding(buffer: ByteArray): Boolean {
		return buffer[0] == '\u0000'.code.toByte() && buffer[1] == '\u0000'.code.toByte() && buffer[2] == '\u0000'.code.toByte() && buffer[3] == '\u0000'.code.toByte()
	}

	/**
	 * Read the frame body from the specified file via the buffer
	 *
	 * @param identifier the frame identifier
	 * @param byteBuffer to read the frame body from
	 * @param frameSize
	 * @return a newly created FrameBody
	 * @throws InvalidFrameException unable to construct a framebody from the data
	 */
	@Throws(InvalidFrameException::class, InvalidDataTypeException::class)
	protected fun readBody(
		identifier: String,
		byteBuffer: ByteBuffer,
		frameSize: Int
	): AbstractID3v2FrameBody {
		//Map to FrameBody (keep old reflection code in case we miss any)
		//In future maybe able to do https://stackoverflow.com/questions/61662231/better-alternative-to-reflection-than-large-switch-statement-using-java-8
		logger.finest("Creating framebody:start")
		val frameBody: AbstractID3v2FrameBody
		frameBody = try {
			when (identifier) {
				ID3v24Frames.FRAME_ID_AUDIO_ENCRYPTION -> FrameBodyAENC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ATTACHED_PICTURE -> FrameBodyAPIC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_AUDIO_SEEK_POINT_INDEX -> FrameBodyASPI(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_CHAPTER -> FrameBodyCHAP(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_COMMENT -> FrameBodyCOMM(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_COMMERCIAL_FRAME -> FrameBodyCOMR(byteBuffer, frameSize)
				ID3v22Frames.FRAME_ID_V2_ENCRYPTED_FRAME -> FrameBodyCRM(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_CHAPTER_TOC -> FrameBodyCTOC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ENCRYPTION -> FrameBodyENCR(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_EQUALISATION2 -> FrameBodyEQU2(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_EVENT_TIMING_CODES -> FrameBodyETCO(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_GENERAL_ENCAPS_OBJECT -> FrameBodyGEOB(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ITUNES_GROUPING -> FrameBodyGRP1(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_GROUP_ID_REG -> FrameBodyGRID(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_INVOLVED_PEOPLE -> FrameBodyIPLS(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_LINKED_INFO -> FrameBodyLINK(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_MUSIC_CD_ID -> FrameBodyMCDI(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_MOVEMENT_NO -> FrameBodyMVIN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_MOVEMENT -> FrameBodyMVNM(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_OWNERSHIP -> FrameBodyOWNE(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_PLAY_COUNTER -> FrameBodyPCNT(byteBuffer, frameSize)
				ID3v22Frames.FRAME_ID_V2_ATTACHED_PICTURE -> FrameBodyPIC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_POPULARIMETER -> FrameBodyPOPM(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_POSITION_SYNC -> FrameBodyPOSS(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_PRIVATE -> FrameBodyPRIV(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_RECOMMENDED_BUFFER_SIZE -> FrameBodyRBUF(
					byteBuffer,
					frameSize
				)
				ID3v24Frames.FRAME_ID_RELATIVE_VOLUME_ADJUSTMENT2 -> FrameBodyRVA2(
					byteBuffer,
					frameSize
				)
				ID3v23Frames.FRAME_ID_V3_RELATIVE_VOLUME_ADJUSTMENT -> FrameBodyRVAD(
					byteBuffer,
					frameSize
				)
				ID3v24Frames.FRAME_ID_REVERB -> FrameBodyRVRB(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_SEEK -> FrameBodySEEK(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_SIGNATURE -> FrameBodySIGN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_SYNC_LYRIC -> FrameBodySYLT(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_SYNC_TEMPO -> FrameBodySYTC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ALBUM -> FrameBodyTALB(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_BPM -> FrameBodyTBPM(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_IS_COMPILATION -> FrameBodyTCMP(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_GENRE -> FrameBodyTCON(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_COPYRIGHTINFO -> FrameBodyTCOP(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_TDAT -> FrameBodyTDAT(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ENCODING_TIME -> FrameBodyTDEN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_PLAYLIST_DELAY -> FrameBodyTDLY(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ORIGINAL_RELEASE_TIME -> FrameBodyTDOR(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_YEAR -> FrameBodyTDRC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_RELEASE_TIME -> FrameBodyTDRL(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_TAGGING_TIME -> FrameBodyTDTG(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ENCODEDBY -> FrameBodyTENC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_LYRICIST -> FrameBodyTEXT(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_FILE_TYPE -> FrameBodyTFLT(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_TIME -> FrameBodyTIME(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_INVOLVED_PEOPLE -> FrameBodyTIPL(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_CONTENT_GROUP_DESC -> FrameBodyTIT1(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_TITLE -> FrameBodyTIT2(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_TITLE_REFINEMENT -> FrameBodyTIT3(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_INITIAL_KEY -> FrameBodyTKEY(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_LANGUAGE -> FrameBodyTLAN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_LENGTH -> FrameBodyTLEN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_MUSICIAN_CREDITS -> FrameBodyTMCL(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_MEDIA_TYPE -> FrameBodyTMED(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_MOOD -> FrameBodyTMOO(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ORIG_TITLE -> FrameBodyTOAL(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ORIG_FILENAME -> FrameBodyTOFN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ORIG_LYRICIST -> FrameBodyTOLY(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ORIGARTIST -> FrameBodyTOPE(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_TORY -> FrameBodyTORY(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_FILE_OWNER -> FrameBodyTOWN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ARTIST -> FrameBodyTPE1(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ACCOMPANIMENT -> FrameBodyTPE2(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_CONDUCTOR -> FrameBodyTPE3(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_REMIXED -> FrameBodyTPE4(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_SET -> FrameBodyTPOS(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_PRODUCED_NOTICE -> FrameBodyTPRO(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_PUBLISHER -> FrameBodyTPUB(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_TRACK -> FrameBodyTRCK(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_TRDA -> FrameBodyTRDA(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_RADIO_NAME -> FrameBodyTRSN(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_RADIO_OWNER -> FrameBodyTRSO(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_TSIZ -> FrameBodyTSIZ(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ALBUM_ARTIST_SORT_ORDER_ITUNES -> FrameBodyTSO2(
					byteBuffer,
					frameSize
				)
				ID3v24Frames.FRAME_ID_ALBUM_SORT_ORDER -> FrameBodyTSOA(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_COMPOSER_SORT_ORDER_ITUNES -> FrameBodyTSOC(
					byteBuffer,
					frameSize
				)
				ID3v24Frames.FRAME_ID_ARTIST_SORT_ORDER -> FrameBodyTSOP(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_TITLE_SORT_ORDER -> FrameBodyTSOT(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_ISRC -> FrameBodyTSRC(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_HW_SW_SETTINGS -> FrameBodyTSSE(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_SET_SUBTITLE -> FrameBodyTSST(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_USER_DEFINED_INFO -> FrameBodyTXXX(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_TYER -> FrameBodyTYER(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_UNIQUE_FILE_ID -> FrameBodyUFID(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_TERMS_OF_USE -> FrameBodyUSER(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_UNSYNC_LYRICS -> FrameBodyUSLT(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_COMMERCIAL -> FrameBodyWCOM(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_COPYRIGHT -> FrameBodyWCOP(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_FILE_WEB -> FrameBodyWOAF(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_ARTIST_WEB -> FrameBodyWOAR(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_SOURCE_WEB -> FrameBodyWOAS(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_OFFICIAL_RADIO -> FrameBodyWORS(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_PAYMENT -> FrameBodyWPAY(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_URL_PUBLISHERS -> FrameBodyWPUB(byteBuffer, frameSize)
				ID3v24Frames.FRAME_ID_USER_DEFINED_URL -> FrameBodyWXXX(byteBuffer, frameSize)
				ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ -> FrameBodyXSOA(
					byteBuffer,
					frameSize
				)
				ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_MUSICBRAINZ -> FrameBodyXSOP(
					byteBuffer,
					frameSize
				)
				ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ -> FrameBodyXSOT(
					byteBuffer,
					frameSize
				)
				else -> {
					val prefix = "com.kylentt.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBody"
					val c = Class.forName("$prefix$identifier") as Class<AbstractID3v2FrameBody>
					val constructorParameterTypes =
						arrayOf(Class.forName("java.nio.ByteBuffer"), Integer.TYPE)
					val constructorParameterValues = arrayOf<Any>(byteBuffer, frameSize)
					val construct = c.getConstructor(*constructorParameterTypes)
					construct.newInstance(*constructorParameterValues)
				}
			}
		} catch (e: InvalidTagException) {
			throw InvalidFrameException(e.message)
		} //No class defined for this frame type,use FrameUnsupported
		catch (cex: ClassNotFoundException) {
			logger.config(
				loggingFilename + ":" + "Identifier not recognised:" + identifier + " using FrameBodyUnsupported"
			)
			try {
				FrameBodyUnsupported(byteBuffer, frameSize)
			} //Should only throw InvalidFrameException but unfortunately legacy hierachy forces
			//read method to declare it can throw InvalidtagException
			catch (ife: InvalidFrameException) {
				throw ife
			} catch (te: InvalidTagException) {
				throw InvalidFrameException(te.message)
			}
		} //An error has occurred during frame instantiation, if underlying cause is an unchecked exception or error
		//propagate it up otherwise mark this frame as invalid
		catch (ite: InvocationTargetException) {
			logger.severe(
				loggingFilename + ":" + "An error occurred within abstractID3v2FrameBody for identifier:" + identifier + ":" + ite.cause!!.message
			)
			if (ite.cause is Error) {
				throw (ite.cause as Error?)!!
			} else if (ite.cause is RuntimeException) {
				throw (ite.cause as RuntimeException?)!!
			} else if (ite.cause is InvalidFrameException) {
				throw (ite.cause as InvalidFrameException?)!!
			} else if (ite.cause is InvalidDataTypeException) {
				throw (ite.cause as InvalidDataTypeException?)!!
			} else {
				throw InvalidFrameException(
					ite.cause!!.message
				)
			}
		} //No Such Method should not happen
		catch (sme: NoSuchMethodException) {
			logger.log(Level.SEVERE, loggingFilename + ":" + "No such method:" + sme.message, sme)
			throw RuntimeException(sme.message)
		} //Instantiate Interface/Abstract should not happen
		catch (ie: InstantiationException) {
			logger.log(
				Level.SEVERE,
				loggingFilename + ":" + "Instantiation exception:" + ie.message,
				ie
			)
			throw RuntimeException(ie.message)
		} //Private Constructor shouild not happen
		catch (iae: IllegalAccessException) {
			logger.log(
				Level.SEVERE,
				loggingFilename + ":" + "Illegal access exception :" + iae.message,
				iae
			)
			throw RuntimeException(iae.message)
		}
		logger.finest(
			loggingFilename + ":" + "Created framebody:end" + frameBody.identifier
		)
		frameBody.header = this
		return frameBody
	}

	/**
	 * Get the next frame id, throwing an exception if unable to do this and check against just having padded data
	 *
	 * @param byteBuffer
	 * @return
	 * @throws PaddingException
	 * @throws InvalidFrameException
	 */
	@Throws(PaddingException::class, InvalidFrameException::class)
	protected fun readIdentifier(byteBuffer: ByteBuffer): String {
		val buffer = ByteArray(frameIdSize)

		//Read the Frame Identifier
		if (frameIdSize <= byteBuffer.remaining()) {
			byteBuffer[buffer, 0, frameIdSize]
		}
		if (isPadding(buffer)) {
			throw PaddingException(loggingFilename + ":only padding found")
		}
		if (frameHeaderSize - frameIdSize > byteBuffer.remaining()) {
			logger.warning(
				loggingFilename + ":" + "No space to find another frame:"
			)
			throw InvalidFrameException(
				loggingFilename + ":" + "No space to find another frame"
			)
		}
		identifier = String(buffer)
		logger.fine(
			loggingFilename + ":" + "Identifier is" + identifier
		)
		return identifier
	}

	/**
	 * This creates a new body based of type identifier but populated by the data
	 * in the body. This is a different type to the body being created which is why
	 * TagUtility.copyObject() can't be used. This is used when converting between
	 * different versions of a tag for frames that have a non-trivial mapping such
	 * as TYER in v3 to TDRC in v4. This will only work where appropriate constructors
	 * exist in the frame body to be created, for example a FrameBodyTYER requires a constructor
	 * consisting of a FrameBodyTDRC.
	 *
	 * If this method is called and a suitable constructor does not exist then an InvalidFrameException
	 * will be thrown
	 *
	 * @param identifier to determine type of the frame
	 * @param body
	 * @return newly created framebody for this type
	 * @throws InvalidFrameException if unable to construct a framebody for the identifier and body provided.
	 */
	@Throws(InvalidFrameException::class)  //TODO using reflection is rather slow perhaps we should change this
	protected fun readBody(
		identifier: String,
		body: AbstractID3v2FrameBody
	): AbstractID3v2FrameBody {
		/* Use reflection to map id to frame body, which makes things much easier
		 * to keep things up to date, although slight performance hit.
		 */
		val frameBody: AbstractID3v2FrameBody
		frameBody = try {
			val c =
				Class.forName("org.jaudiotagger.tag.id3.framebody.FrameBody$identifier") as Class<AbstractID3v2FrameBody>
			val constructorParameterTypes = arrayOf<Class<*>>(body.javaClass)
			val constructorParameterValues = arrayOf<Any>(body)
			val construct = c.getConstructor(*constructorParameterTypes)
			construct.newInstance(*constructorParameterValues)
		} catch (cex: ClassNotFoundException) {
			logger.config(
				"Identifier not recognised:$identifier unable to create framebody"
			)
			throw InvalidFrameException("FrameBody$identifier does not exist")
		} //If suitable constructor does not exist
		catch (sme: NoSuchMethodException) {
			logger.log(Level.SEVERE, "No such method:" + sme.message, sme)
			throw InvalidFrameException("FrameBody" + identifier + " does not have a constructor that takes:" + body.javaClass.name)
		} catch (ite: InvocationTargetException) {
			logger.severe("An error occurred within abstractID3v2FrameBody")
			logger.log(
				Level.SEVERE,
				"Invocation target exception:" + ite.cause!!.message,
				ite.cause
			)
			if (ite.cause is Error) {
				throw (ite.cause as Error?)!!
			} else if (ite.cause is RuntimeException) {
				throw (ite.cause as RuntimeException?)!!
			} else {
				throw InvalidFrameException(
					ite.cause!!.message
				)
			}
		} //Instantiate Interface/Abstract should not happen
		catch (ie: InstantiationException) {
			logger.log(Level.SEVERE, "Instantiation exception:" + ie.message, ie)
			throw RuntimeException(ie.message)
		} //Private Constructor shouild not happen
		catch (iae: IllegalAccessException) {
			logger.log(Level.SEVERE, "Illegal access exception :" + iae.message, iae)
			throw RuntimeException(iae.message)
		}
		logger.finer("frame Body created" + frameBody.identifier)
		frameBody.header = this
		return frameBody
	}

	override val rawContent: ByteArray
		get() {
			val baos = ByteArrayOutputStream()
			write(baos)
			return baos.toByteArray()
		}

	abstract fun write(tagBuffer: ByteArrayOutputStream?)

	/**
	 * @param b
	 */
	override fun isBinary(b: Boolean) {
		//do nothing because whether or not a field is binary is defined by its id and is immutable
	}

	//TODO depends on the body
	override val isEmpty: Boolean
		get() {
			val body = body ?: return true
			//TODO depends on the body
			return false
		}

	open class StatusFlags protected constructor() {
		/**
		 * This returns the flags as they were originally read or created
		 * @return
		 */
		var originalFlags: Byte = 0
			protected set

		/**
		 * This returns the flags amended to meet specification
		 * @return
		 */
		var writeFlags: Byte = 0
			protected set

		open fun createStructure() {}
		override fun equals(obj: Any?): Boolean {
			if (this === obj) return true
			if (obj !is StatusFlags) {
				return false
			}
			val that = obj
			return areEqual(
				originalFlags.toLong(), that.originalFlags.toLong()
			) &&
				areEqual(
					writeFlags.toLong(), that.writeFlags.toLong()
				)
		}

		companion object {
			@JvmStatic
			protected val TYPE_FLAGS = "statusFlags"
		}
	}

	open class EncodingFlags {
		open var flags: Byte = 0

		protected constructor() {
			resetFlags()
		}

		protected constructor(flags: Byte) {
			this.flags = flags
		}

		fun resetFlags() {
			flags = 0.toByte()
		}

		open fun createStructure() {}
		override fun equals(obj: Any?): Boolean {
			if (this === obj) return true
			if (obj !is EncodingFlags) {
				return false
			}
			return areEqual(
				flags.toLong(), obj.flags.toLong()
			)
		}

		companion object {
			@JvmStatic
			protected val TYPE_FLAGS = "encodingFlags"
		}
	}

	/**
	 * Return String Representation of frame
	 */
	open fun createStructure() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_FRAME, identifier)
		MP3File.structureFormatter?.closeHeadingElement(TYPE_FRAME)
	}

	override fun equals(obj: Any?): Boolean {
		if (this === obj) return true
		if (obj !is AbstractID3v2Frame) {
			return false
		}
		return super.equals(obj)
	}
	/**
	 * Returns the content of the field.
	 *
	 * For frames consisting of different fields, this will return the value deemed to be most
	 * likely to be required
	 *
	 * @return Content
	 */
	/**
	 * Sets the content of the field.
	 *
	 * @param content fields content.
	 */
	override var content: String?
		get() = body!!.userFriendlyValue
		set(content) {
			throw UnsupportedOperationException("Not implemented please use the generic tag methods for setting content")
		}

	/**
	 * Returns the current used charset encoding.
	 *
	 * @return Charset encoding.
	 */
	override var encoding: Charset? = null
		get() {
			val textEncoding = body?.textEncoding ?: return null
			return TextEncoding.instanceOf.getCharsetForId(textEncoding.toInt())
		}

	override fun toDescriptiveString(): String = body.toString()

	companion object {
		@JvmStatic
		protected val TYPE_FRAME = "frame"

		@JvmStatic
		protected val TYPE_FRAME_SIZE = "frameSize"

		@JvmStatic
		protected val UNSUPPORTED_ID = "Unsupported"
	}
}

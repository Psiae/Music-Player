/**
 * @author : Paul Taylor
 *
 *
 * Version @version:$Id$
 *
 *
 * MusicTag Copyright (C)2003,2004
 *
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.roundToInt

/**
 * Represents the audio header of an MP3 File
 *
 *
 * The audio header consists of a number of
 * audio frames. Because we are not trying to play the audio but only extract some information
 * regarding the audio we only need to read the first  audio frames to ensure that we have correctly
 * identified them as audio frames and extracted the metadata we reuire.
 *
 *
 * Start of Audio id 0xFF (11111111) and then second byte anded with 0xE0(11100000).
 * For example 2nd byte doesnt have to be 0xE0 is just has to have the top 3 signicant
 * bits set. For example 0xFB (11111011) is a common occurence of the second match. The 2nd byte
 * defines flags to indicate various mp3 values.
 *
 *
 * Having found these two values we then read the header which comprises these two bytes plus a further
 * two to ensure this really is a MP3Header, sometimes the first frame is actually a dummy frame with summary information
 * held within about the whole file, typically using a Xing Header or LAme Header. This is most useful when the file
 * is variable bit rate, if the file is variable bit rate but does not use a summary header it will not be correctly
 * identified as a VBR frame and the track length will be incorrectly calculated. Strictly speaking MP3 means
 * Layer III file but MP2 Layer II), MP1 Layer I) and MPEG-2 files are sometimes used and named with
 * the .mp3 suffix so this library attempts to supports all these formats.
 */
open class MP3AudioHeader : AudioHeader {
	private var mAudioDataLength: Long? = null
	private var mAudioDataStartPosition: Long? = null
	private var mAudioDataEndPosition: Long? = null
	private var mBitRate: Long? = 0
	private var mByteRate: Int? = null
	private var mBitsPerSample: Int? = null
	private var mEncodingType: String? = null
	private var mFormat: String? = null
	private var mIsVbr: Boolean? = null
	private var mIsLossless: Boolean? = null
	private var mNoOfChannels: Int? = null
	private var mNoOfSamples: Long? = null
	private var mSamplingRate: Int? = null
	private var mTrackLength: Double = 0.0

	override val trackLength: Int?
		get() = mTrackLength.roundToInt()

	override var audioDataLength: Long
		get() = 0
		set(value) {
			mAudioDataLength = value
		}

	override var audioDataStartPosition: Long?
		get() = mAudioDataStartPosition
		set(value) {
			mAudioDataStartPosition = value
		}

	override var audioDataEndPosition: Long?
		get() = mAudioDataEndPosition
		set(value) {
			mAudioDataEndPosition = value
		}

	override val encodingType: String?
		get() {
			val get = mp3FrameHeader ?: return null
			return get.versionAsString + " " + get.layerAsString
		}

	override var byteRate: Int?
		get() = null
		set(value) {
			mByteRate = value
		}

	override val bitRate: String?
		get() {
			return if (mp3XingFrame != null && mp3XingFrame!!.isVbr) {
				isVbrIdentifier.toString() + mBitRate?.toString()
			} else if (mp3VbriFrame != null) {
				isVbrIdentifier.toString() + mBitRate?.toString()
			} else {
				mBitRate?.toString()
			}
		}

	override val bitRateAsNumber: Long?
		get() = mBitRate?.toLong()

	override val sampleRate: String?
		get() = mp3FrameHeader?.samplingRate?.toString()

	override val sampleRateAsNumber: Int?
		get() = mp3FrameHeader?.samplingRate

	override val format: String?
		get() = SupportedFileFormat.MP3.displayName

	override val channels: String?
		get() = mp3FrameHeader?.channelModeAsString

	override val isVariableBitRate: Boolean
		get() {
			return if (mp3XingFrame != null) {
				mp3XingFrame!!.isVbr
			} else if (mp3VbriFrame != null) {
				mp3VbriFrame!!.isVbr()
			} else {
				mp3FrameHeader!!.isVariableBitRate
			}
		}

	override var preciseTrackLength: Double
		get() = mTrackLength
		set(value) {
			mTrackLength = value
		}

	override val bitsPerSample: Int
		get() = 16

	override val isLossless: Boolean?
		get() = false

	override var noOfSamples: Long?
		get() = mNoOfSamples
		set(value) {
			mNoOfSamples = value
		}


	var mp3FrameHeader: MPEGFrameHeader? = null
	protected var mp3XingFrame: XingFrame? = null
	protected var mp3VbriFrame: VbriFrame? = null
	private var fileSize: Long = 0
	/**
	 * Returns the byte position of the first MP3 Frame that the
	 * `file` arguement refers to. This is the first byte of music
	 * data and not the ID3 Tag Frame.
	 *
	 * @return the byte position of the first MP3 Frame
	 */
	/**
	 * Set the location of where the Audio file begins in the file
	 *
	 * @param startByte
	 */
	var mp3StartByte: Long = 0

	/**
	 * @return the the time each frame contributes to the audio in fractions of seconds
	 */
	private var timePerFrame = 0.0

	/**
	 * @return The number of frames within the Audio File, calculated as accurately as possible
	 */
	var numberOfFrames: Long = 0
		private set

	/**
	 * @return The number of frames within the Audio File, calculated by dividing the filesize by
	 * the number of frames, this may not be the most accurate method available.
	 */
	var numberOfFramesEstimate: Long = 0
		private set

	/**
	 * Encoder retrieved from frame/Xing header
	 */
	var encoder: String? = ""
		private set

	constructor()

	/**
	 * Search for the first MP3Header in the file
	 *
	 * The search starts from the start of the file, it is usually safer to use the alternative constructor that
	 * allows you to provide the length of the tag header as a parameter so the tag can be skipped over.
	 *
	 * @param seekFile
	 * @throws IOException
	 * @throws InvalidAudioFrameException
	 */
	constructor(seekFile: File) {
		if (!seek(seekFile, 0)) {
			throw InvalidAudioFrameException("No audio header found within" + seekFile.name)
		}
	}

	/**
	 * Search for the first MP3Header in the file
	 *
	 * Starts searching from location startByte, this is because there is likely to be an ID3TagHeader
	 * before the start of the audio. If this tagHeader contains unsynchronized information there is a
	 * possibility that it might be inaccurately identified as the start of the Audio data. Various checks
	 * are done in this code to prevent this happening but it cannot be guaranteed.
	 *
	 * Of course if the startByte provided overstates the length of the tag header, this could mean the
	 * start of the MP3AudioHeader is missed, further checks are done within the MP3 class to recognize
	 * if this has occurred and take appropriate action.
	 *
	 * @param seekFile
	 * @param startByte
	 * @throws IOException
	 * @throws InvalidAudioFrameException
	 */
	constructor(seekFile: File, startByte: Long) {
		if (!seek(seekFile, startByte)) {
			throw InvalidAudioFrameException(ErrorMessage.NO_AUDIO_HEADER_FOUND.getMsg(seekFile.name))
		}
	}

	constructor(seekFile: FileDescriptor, startByte: Long) {
		if (!seek(seekFile, startByte)) {
			throw InvalidAudioFrameException(null)
		}
	}

	@Throws(IOException::class)
	fun seek(seekFile: FileDescriptor, startByte: Long): Boolean {
		//References to Xing/VRbi Header
		var header: ByteBuffer?

		//This is substantially faster than updating the file-channels position
		val fis = FileInputStream(seekFile)
		val fc = fis.channel

		val length = fc.size()

		//Read into Byte Buffer in Chunks
		val bb = ByteBuffer.allocateDirect(FILE_BUFFER_SIZE)

		//Move FileChannel to the starting position (skipping over tag if any)
		fc.position(startByte)

		//Update filePointerCount
		var filePointerCount: Long = startByte

		//Read from here into the byte buffer , doesn't move location of filepointer
		fc.read(bb, startByte)
		bb.flip()
		var syncFound = false
		try {
			do {
				//TODO remaining() is quite an expensive operation, isn't there a way we can work this out without
				//interrogating the bytebuffer. Also this is rarely going to be true, and could be made less true
				//by increasing FILE_BUFFER_SIZE
				if (bb.remaining() <= MIN_BUFFER_REMAINING_REQUIRED) {
					bb.clear()
					fc.position(filePointerCount)
					fc.read(bb, fc.position())
					bb.flip()

					val bbLimit = bb.limit()
					if (bbLimit <= MIN_BUFFER_REMAINING_REQUIRED) {
						//No mp3 exists
						Timber.e("byteBuffer limit $bbLimit is less than or equal $MIN_BUFFER_REMAINING_REQUIRED")
						return false
					}
				}
				//MP3File.logger.finest("fc:"+fc.position() + "bb"+bb.position());
				if (MPEGFrameHeader.isMPEGFrame(bb)) {
					try {
						mp3FrameHeader = MPEGFrameHeader.Companion.parseMPEGHeader(bb)
						syncFound = true
						//if(2==1) use this line when you want to test getting the next frame without using xing
						if (
							XingFrame.isXingFrame(bb, mp3FrameHeader).also { header = it } != null
						) {
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest("Found Possible XingHeader")
							}
							try {
								//Parses Xing frame without modifying position of main buffer
								mp3XingFrame = XingFrame.parseXingFrame(header!!)
							} catch (ex: InvalidAudioFrameException) {
								// We Ignore because even if Xing Header is corrupted
								//doesn't mean file is corrupted
							}
							break
						} else if (VbriFrame.isVbriFrame(bb, mp3FrameHeader).also { header = it } != null
						) {
							try {
								//Parses Vbri frame without modifying position of main buffer
								mp3VbriFrame = VbriFrame.Companion.parseVBRIFrame(header!!)
							} catch (ex: InvalidAudioFrameException) {
								// We Ignore because even if Vbri Header is corrupted
								//doesn't mean file is corrupted
							}
							break
						} else {
							syncFound = isNextFrameValid(filePointerCount, bb, fc)
							if (syncFound) {
								break
							}
						}
					} catch (ex: InvalidAudioFrameException) {
						// We Ignore because likely to be incorrect sync bits ,
						// will just continue in loop
					}
				}

				//TODO position() is quite an expensive operation, isn't there a way we can work this out without
				//interrogating the bytebuffer
				bb.position(bb.position() + 1)
				filePointerCount++
			} while (!syncFound)
		} catch (ex: EOFException) {
			logger.log(Level.WARNING, "Reached end of file without finding sync match", ex)
			syncFound = false
		} catch (iox: IOException) {
			logger.log(Level.SEVERE, "IOException occurred whilst trying to find sync", iox)
			syncFound = false
			throw iox
		} finally {
			fc?.close()
			fis.close()
		}

		setFileSize(length)
		mp3StartByte = filePointerCount
		setTimePerFrame()
		setNumberOfFrames()
		setTrackLength()
		setBitRate()
		setEncoder()
		return syncFound
	}

	/**
	 * Returns true if the first MP3 frame can be found for the MP3 file
	 *
	 * This is the first byte of  music data and not the ID3 Tag Frame.     *
	 *
	 * @param seekFile  MP3 file to seek
	 * @param startByte if there is an ID3v2tag we dont want to start reading from the start of the tag
	 * @return true if the first MP3 frame can be found
	 * @throws IOException on any I/O error
	 */
	@Throws(IOException::class)
	fun seek(seekFile: File, startByte: Long): Boolean {
		//References to Xing/VRbi Header
		var header: ByteBuffer?

		//This is substantially faster than updating the filechannels position
		var filePointerCount: Long
		val fis = FileInputStream(seekFile)
		val fc = fis.channel

		//Read into Byte Buffer in Chunks
		val bb = ByteBuffer.allocateDirect(FILE_BUFFER_SIZE)

		//Move FileChannel to the starting position (skipping over tag if any)
		fc!!.position(startByte)

		//Update filePointerCount
		filePointerCount = startByte

		//Read from here into the byte buffer , doesn't move location of filepointer
		fc.read(bb, startByte)
		bb.flip()
		var syncFound = false
		try {
			do {
				//TODO remaining() is quite an expensive operation, isn't there a way we can work this out without
				//interrogating the bytebuffer. Also this is rarely going to be true, and could be made less true
				//by increasing FILE_BUFFER_SIZE
				if (bb.remaining() <= MIN_BUFFER_REMAINING_REQUIRED) {
					bb.clear()
					fc.position(filePointerCount)
					fc.read(bb, fc.position())
					bb.flip()

					val bbLimit = bb.limit()
					if (bbLimit <= MIN_BUFFER_REMAINING_REQUIRED) {
						//No mp3 exists
						Timber.e("byteBuffer limit $bbLimit is less than or equal $MIN_BUFFER_REMAINING_REQUIRED")
						return false
					}
				}
				//MP3File.logger.finest("fc:"+fc.position() + "bb"+bb.position());
				if (MPEGFrameHeader.Companion.isMPEGFrame(bb)) {
					try {
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest(
								"Found Possible header at:$filePointerCount"
							)
						}
						mp3FrameHeader = MPEGFrameHeader.Companion.parseMPEGHeader(bb)
						syncFound = true
						//if(2==1) use this line when you want to test getting the next frame without using xing
						if (
							XingFrame.isXingFrame(bb, mp3FrameHeader).also { header = it } != null
						) {
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest("Found Possible XingHeader")
							}
							try {
								//Parses Xing frame without modifying position of main buffer
								mp3XingFrame = XingFrame.parseXingFrame(header!!)
							} catch (ex: InvalidAudioFrameException) {
								// We Ignore because even if Xing Header is corrupted
								//doesn't mean file is corrupted
							}
							break
						} else if (VbriFrame.isVbriFrame(bb, mp3FrameHeader).also { header = it } != null
						) {
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest("Found Possible VbriHeader")
							}
							try {
								//Parses Vbri frame without modifying position of main buffer
								mp3VbriFrame = VbriFrame.Companion.parseVBRIFrame(header!!)
							} catch (ex: InvalidAudioFrameException) {
								// We Ignore because even if Vbri Header is corrupted
								//doesn't mean file is corrupted
							}
							break
						} else {
							syncFound = isNextFrameValid(seekFile, filePointerCount, bb, fc)
							if (syncFound) {
								break
							}
						}
					} catch (ex: InvalidAudioFrameException) {
						// We Ignore because likely to be incorrect sync bits ,
						// will just continue in loop
					}
				}

				//TODO position() is quite an expensive operation, isn't there a way we can work this out without
				//interrogating the bytebuffer
				bb.position(bb.position() + 1)
				filePointerCount++
			} while (!syncFound)
		} catch (ex: EOFException) {
			logger.log(Level.WARNING, "Reached end of file without finding sync match", ex)
			syncFound = false
		} catch (iox: IOException) {
			logger.log(Level.SEVERE, "IOException occurred whilst trying to find sync", iox)
			syncFound = false
			throw iox
		} finally {
			if (fc != null) {
				fc.close()
			}
			if (fis != null) {
				fis.close()
			}
		}

		//Return to start of audio header
		if (logger.isLoggable(Level.FINEST)) {
			logger.finer(
				"Return found matching mp3 header starting at$filePointerCount"
			)
		}
		setFileSize(seekFile.length())
		mp3StartByte = filePointerCount
		setTimePerFrame()
		setNumberOfFrames()
		setTrackLength()
		setBitRate()
		setEncoder()
		/*if((filePointerCount - startByte )>0)
		{
				logger.severe(seekFile.getName()+"length:"+startByte+"Difference:"+(filePointerCount - startByte));
		}
		*/return syncFound
	}

	@Throws(IOException::class)
	private fun isNextFrameValid(
		filePointerCount: Long,
		bb: ByteBuffer,
		fc: FileChannel
	): Boolean {
		var result = false
		var currentPosition = bb.position()

		//Our buffer is not large enough to fit in the whole of this frame, something must
		//have gone wrong because frames are not this large, so just return false
		//bad frame header
		if (mp3FrameHeader!!.frameLength > FILE_BUFFER_SIZE - MIN_BUFFER_REMAINING_REQUIRED) {
			logger.finer("Frame size is too large to be a frame:" + mp3FrameHeader!!.frameLength)
			return false
		}

		//Check for end of buffer if not enough room get some more
		if (bb.remaining() <= MIN_BUFFER_REMAINING_REQUIRED + mp3FrameHeader!!.frameLength) {
			logger.finer("Buffer too small, need to reload, buffer size:" + bb.remaining())
			bb.clear()
			fc.position(filePointerCount)
			fc.read(bb, fc.position())
			bb.flip()
			//So now original buffer has been replaced, so set current position to start of buffer
			currentPosition = 0
			//Not enough left
			if (bb.limit() <= MIN_BUFFER_REMAINING_REQUIRED) {
				//No mp3 exists
				logger.finer("Nearly at end of file, no header found:")
				return false
			}

			//Still Not enough left for next alleged frame size so giving up
			if (bb.limit() <= MIN_BUFFER_REMAINING_REQUIRED + mp3FrameHeader!!.frameLength) {
				//No mp3 exists
				logger.finer("Nearly at end of file, no room for next frame, no header found:")
				return false
			}
		}

		//Position bb to the start of the alleged next frame
		bb.position(bb.position() + mp3FrameHeader!!.frameLength)
		if (MPEGFrameHeader.Companion.isMPEGFrame(bb)) {
			result = try {
				MPEGFrameHeader.Companion.parseMPEGHeader(bb)
				logger.finer("Check next frame confirms is an audio header ")
				true
			} catch (ex: InvalidAudioFrameException) {
				logger.finer("Check next frame has identified this is not an audio header")
				false
			}
		} else {
			logger.finer("isMPEGFrame has identified this is not an audio header")
		}
		//Set back to the start of the previous frame
		bb.position(currentPosition)
		return result
	}

	/**
	 * Called in some circumstances to check the next frame to ensure we have the correct audio header
	 *
	 * @param seekFile
	 * @param filePointerCount
	 * @param bb
	 * @param fc
	 * @return true if frame is valid
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun isNextFrameValid(
		seekFile: File,
		filePointerCount: Long,
		bb: ByteBuffer,
		fc: FileChannel?
	): Boolean {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finer("Checking next frame" + seekFile.name + ":fpc:" + filePointerCount + "skipping to:" + (filePointerCount + mp3FrameHeader!!.frameLength))
		}
		var result = false
		var currentPosition = bb.position()

		//Our buffer is not large enough to fit in the whole of this frame, something must
		//have gone wrong because frames are not this large, so just return false
		//bad frame header
		if (mp3FrameHeader!!.frameLength > FILE_BUFFER_SIZE - MIN_BUFFER_REMAINING_REQUIRED) {
			logger.finer("Frame size is too large to be a frame:" + mp3FrameHeader!!.frameLength)
			return false
		}

		//Check for end of buffer if not enough room get some more
		if (bb.remaining() <= MIN_BUFFER_REMAINING_REQUIRED + mp3FrameHeader!!.frameLength) {
			logger.finer("Buffer too small, need to reload, buffer size:" + bb.remaining())
			bb.clear()
			fc!!.position(filePointerCount)
			fc.read(bb, fc.position())
			bb.flip()
			//So now original buffer has been replaced, so set current position to start of buffer
			currentPosition = 0
			//Not enough left
			if (bb.limit() <= MIN_BUFFER_REMAINING_REQUIRED) {
				//No mp3 exists
				logger.finer("Nearly at end of file, no header found:")
				return false
			}

			//Still Not enough left for next alleged frame size so giving up
			if (bb.limit() <= MIN_BUFFER_REMAINING_REQUIRED + mp3FrameHeader!!.frameLength) {
				//No mp3 exists
				logger.finer("Nearly at end of file, no room for next frame, no header found:")
				return false
			}
		}

		//Position bb to the start of the alleged next frame
		bb.position(bb.position() + mp3FrameHeader!!.frameLength)
		if (MPEGFrameHeader.Companion.isMPEGFrame(bb)) {
			result = try {
				MPEGFrameHeader.Companion.parseMPEGHeader(bb)
				logger.finer("Check next frame confirms is an audio header ")
				true
			} catch (ex: InvalidAudioFrameException) {
				logger.finer("Check next frame has identified this is not an audio header")
				false
			}
		} else {
			logger.finer("isMPEGFrame has identified this is not an audio header")
		}
		//Set back to the start of the previous frame
		bb.position(currentPosition)
		return result
	}

	/**
	 * Set number of frames in this file, use Xing if exists otherwise ((File Size - Non Audio Part)/Frame Size)
	 */
	protected fun setNumberOfFrames() {
		numberOfFramesEstimate = (fileSize - mp3StartByte) / mp3FrameHeader!!.frameLength
		numberOfFrames = if (mp3XingFrame != null && mp3XingFrame!!.isFrameCountEnabled) {
			mp3XingFrame!!.frameCount.toLong()
		} else if (mp3VbriFrame != null) {
			mp3VbriFrame!!.frameCount.toLong()
		} else {
			numberOfFramesEstimate
		}
	}

	/**
	 * Set the time each frame contributes to the audio in fractions of seconds, the higher
	 * the sampling rate the shorter the audio segment provided by the frame,
	 * the number of samples is fixed by the MPEG Version and Layer
	 */
	protected fun setTimePerFrame() {
		timePerFrame = mp3FrameHeader!!.noOfSamples / mp3FrameHeader!!.samplingRate!!.toDouble()

		//Because when calculating framelength we may have altered the calculation slightly for MPEGVersion2
		//to account for mono/stereo we seem to have to make a corresponding modification to get the correct time
		if (mp3FrameHeader!!.version == MPEGFrameHeader.Companion.VERSION_2 || mp3FrameHeader!!.version == MPEGFrameHeader.VERSION_2_5) {
			if (mp3FrameHeader!!.layer == MPEGFrameHeader.Companion.LAYER_II || mp3FrameHeader!!.layer == MPEGFrameHeader.Companion.LAYER_III) {
				if (mp3FrameHeader!!.numberOfChannels == 1) {
					timePerFrame /= 2
				}
			}
		}
	}

	/**
	 * Estimate the length of the audio track in seconds
	 * Calculation is Number of frames multiplied by the Time Per Frame using the first frame as a prototype
	 * Time Per Frame is the number of samples in the frame (which is defined by the MPEGVersion/Layer combination)
	 * divided by the sampling rate, i.e the higher the sampling rate the shorter the audio represented by the frame is going
	 * to be.
	 */
	protected fun setTrackLength() {
		mTrackLength = numberOfFrames * timePerFrame
	}

	/**
	 * Return the length in user friendly format
	 * @return
	 */
	val trackLengthAsString: String
		get() {
			val timeIn: Date
			return try {
				val lengthInSecs = trackLength!!.toLong()
				synchronized(timeInFormat) { timeIn = timeInFormat.parse(lengthInSecs.toString()) }
				if (lengthInSecs < NO_SECONDS_IN_HOUR) {
					synchronized(timeOutFormat) { return timeOutFormat.format(timeIn) }
				} else {
					synchronized(timeOutOverAnHourFormat) {
						return timeOutOverAnHourFormat.format(
							timeIn
						)
					}
				}
			} catch (pe: ParseException) {
				logger.warning("Unable to parse:" + preciseTrackLength + " failed with ParseException:" + pe.message)
				""
			}
		}

	/**
	 * Set bitrate in kbps, if Vbr use Xingheader if possible
	 */
	protected fun setBitRate() {
		mBitRate =
			if (mp3XingFrame != null && mp3XingFrame!!.isVbr) {
				if (mp3XingFrame!!.isAudioSizeEnabled && mp3XingFrame!!.audioSize > 0) {
					(mp3XingFrame!!.audioSize * Utils.BITS_IN_BYTE_MULTIPLIER / (timePerFrame * numberOfFrames * Utils.KILOBYTE_MULTIPLIER)).toLong()
				} else {
					((fileSize - mp3StartByte) * Utils.BITS_IN_BYTE_MULTIPLIER / (timePerFrame * numberOfFrames * Utils.KILOBYTE_MULTIPLIER)).toLong()
				}
			} else if (mp3VbriFrame != null) {
				if (mp3VbriFrame!!.audioSize > 0) {
					(mp3VbriFrame!!.audioSize * Utils.BITS_IN_BYTE_MULTIPLIER / (timePerFrame * numberOfFrames * Utils.KILOBYTE_MULTIPLIER)).toLong()
				} else {
					((fileSize - mp3StartByte) * Utils.BITS_IN_BYTE_MULTIPLIER / (timePerFrame * numberOfFrames * Utils.KILOBYTE_MULTIPLIER)).toLong()
				}
			} else {
				mp3FrameHeader!!.bitRate!!.toLong()
			}
	}

	protected fun setEncoder() {
		val mp3XingFrame = this.mp3XingFrame
		val mp3VbriFrame = this.mp3VbriFrame
		if (mp3XingFrame != null) {
			val lameFrame = mp3XingFrame.lameFrame
			if (lameFrame != null) encoder = lameFrame.encoder
		} else if (mp3VbriFrame != null) {
			encoder = mp3VbriFrame.encoder
		}
	}

	/**
	 * @return MPEG Version (1-3)
	 */
	val mpegVersion: String?
		get() = mp3FrameHeader?.versionAsString

	/**
	 * @return MPEG Layer (1-3)
	 */
	val mpegLayer: String?
		get() = mp3FrameHeader?.layerAsString

	/**
	 * @return Emphasis
	 */
	val emphasis: String?
		get() = mp3FrameHeader?.emphasisAsString

	val isProtected: Boolean
		get() = mp3FrameHeader!!.isProtected

	val isPrivate: Boolean
		get() = mp3FrameHeader!!.isPrivate

	val isCopyrighted: Boolean
		get() = mp3FrameHeader!!.isCopyrighted

	val isOriginal: Boolean
		get() = mp3FrameHeader!!.isOriginal

	val isPadding: Boolean
		get() = mp3FrameHeader!!.isPadding

	/**
	 * Set the size of the file, required in some calculations
	 *
	 * @param fileSize
	 */
	protected fun setFileSize(fileSize: Long) {
		this.fileSize = fileSize
	}

	/**
	 * @return a string representation
	 */
	override fun toString(): String {
		val out = StringBuilder()
		out.append("Audio Header content:\n")
		out.append(
			"""	fileSize:$fileSize
	encoder:$encoder
	encoderType:$encodingType
	format:$format
	startByte:""" + Hex.asHex(
				mp3StartByte
			) + "\n"
				+ "\tnumberOfFrames:" + numberOfFrames + "\n"
				+ "\tnumberOfFramesEst:" + numberOfFramesEstimate + "\n"
				+ "\ttimePerFrame:" + timePerFrame + "\n"
				+ "\tbitrate:" + bitRate + "\n"
				+ "\ttrackLength:" + trackLengthAsString + "\n"
		)
		if (mp3FrameHeader != null) {
			out.append(mp3FrameHeader.toString())
		} else {
			out.append("MPEG Frame Header:false")
		}
		if (mp3XingFrame != null) {
			out.append(mp3XingFrame.toString())
		} else {
			out.append("Xing Frame:false")
		}
		if (mp3VbriFrame != null) {
			out.append(mp3VbriFrame.toString())
		} else {
			out.append("VBRI Frame:false")
		}
		return out.toString()
	}


	companion object {
		private val timeInFormat = SimpleDateFormat("ss", Locale.UK)
		private val timeOutFormat = SimpleDateFormat("mm:ss", Locale.UK)
		private val timeOutOverAnHourFormat = SimpleDateFormat("kk:mm:ss", Locale.UK)
		private const val isVbrIdentifier = '~'

		//Logger
		var logger = Logger.getLogger("org.jaudiotagger.audio.mp3")

		/**
		 * After testing the average location of the first MP3Header bit was at 5000 bytes so this is
		 * why chosen as a default.
		 */
		private const val FILE_BUFFER_SIZE = 5000
		private val MIN_BUFFER_REMAINING_REQUIRED: Int =
			MPEGFrameHeader.Companion.HEADER_SIZE + XingFrame.Companion.MAX_BUFFER_SIZE_NEEDED_TO_READ_XING
		private const val NO_SECONDS_IN_HOUR = 3600
	}
}

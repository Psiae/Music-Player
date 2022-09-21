package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import java.nio.ByteBuffer
import java.util.*

/**
 * Xing Frame
 *
 *
 * In some MP3s which variable bit rate the first frame in the file contains a special frame called a Xing Frame,
 * instead of audio data. This is used to store additional information about the file. The most important aspect for
 * this library is details allowing us to determine the bitrate of a Variable Bit Rate VBR file without having
 * to process the whole file.
 *
 * Xing VBR Tag data format is 120 bytes long
 * 4 bytes for Header Tag
 * 4 bytes for Header Flags
 * 4 bytes for FRAME SIZE
 * 4 bytes for AUDIO_SIZE
 * 100 bytes for entry (NUMTOCENTRIES)
 * 4 bytes for VBR SCALE. a VBR quality indicator: 0=best 100=worst
 *
 * It my then contain a Lame Frame ( a Lame frame is in essence an extended Xing Frame
 */
class XingFrame private constructor(private val header: ByteBuffer) {
	/**
	 * Is this XingFrame detailing a variable bit rate MPEG
	 *
	 * @return
	 */
	var isVbr = false
		private set

	/**
	 * @return true if frameCount has been specified in header
	 */
	var isFrameCountEnabled = false
		private set

	/**
	 * @return count of frames
	 */
	var frameCount = -1
		private set

	/**
	 * @return true if audioSize has been specified in header
	 */
	var isAudioSizeEnabled = false
		private set

	/**
	 * @return size of audio data in bytes
	 */
	var audioSize = -1
		private set
	var lameFrame: LameFrame? = null

	/**
	 * Read the Xing Properties from the buffer
	 */
	init {

		//Go to start of Buffer
		header.rewind()

		//Set Vbr
		setVbr()

		//Read Flags, only the fourth byte of interest to us
		val flagBuffer = ByteArray(XING_FLAG_BUFFER_SIZE)
		header[flagBuffer]

		//Read FrameCount if flag set
		if (flagBuffer[BYTE_4].toInt() and 1.toByte().toInt() != 0) {
			setFrameCount()
		}

		//Read Size if flag set
		if (flagBuffer[BYTE_4].toInt() and (1 shl 1).toByte().toInt() != 0) {
			setAudioSize()
		}

		//TODO TOC
		//TODO VBR Quality

		//Look for LAME Header as long as we have enough bytes to do it properly
		if (header.limit() >= XING_HEADER_BUFFER_SIZE + LameFrame.Companion.LAME_HEADER_BUFFER_SIZE) {
			header.position(XING_HEADER_BUFFER_SIZE)
			lameFrame = LameFrame.Companion.parseLameFrame(
				header
			)
		}
	}

	/**
	 * Set whether or not VBR, (Xing can also be used for CBR though this is less useful)
	 */
	private fun setVbr() {
		//Is it VBR or CBR
		val identifier = ByteArray(XING_IDENTIFIER_BUFFER_SIZE)
		header[identifier]
		if (Arrays.equals(identifier, XING_VBR_ID)) {
			AudioFile.logger.finest("Is Vbr")
			isVbr = true
		}
	}

	/**
	 * Set count of frames
	 */
	private fun setFrameCount() {
		val frameCountBuffer = ByteArray(XING_FRAMECOUNT_BUFFER_SIZE)
		header[frameCountBuffer]
		isFrameCountEnabled = true
		frameCount =
			frameCountBuffer[BYTE_1].toInt() shl 24 and -0x1000000 or (frameCountBuffer[BYTE_2].toInt() shl 16 and 0x00FF0000) or (frameCountBuffer[BYTE_3].toInt() shl 8 and 0x0000FF00) or (frameCountBuffer[BYTE_4].toInt() and 0x000000FF)
	}

	/**
	 * Set size of AudioData
	 */
	private fun setAudioSize() {
		val frameSizeBuffer = ByteArray(XING_AUDIOSIZE_BUFFER_SIZE)
		header[frameSizeBuffer]
		isAudioSizeEnabled = true
		audioSize =
			frameSizeBuffer[BYTE_1].toInt() shl 24 and -0x1000000 or (frameSizeBuffer[BYTE_2].toInt() shl 16 and 0x00FF0000) or (frameSizeBuffer[BYTE_3].toInt() shl 8 and 0x0000FF00) or (frameSizeBuffer[BYTE_4].toInt() and 0x000000FF)
	}

	/**
	 * @return a string representation
	 */
	override fun toString(): String {
		return """Xing Header+
	vbr:${isVbr}
	frameCountEnabled:$isFrameCountEnabled
	frameCount:$frameCount
	audioSizeEnabled:$isAudioSizeEnabled
	audioFileSize:$audioSize
"""
	}

	companion object {
		//The offset into first frame varies based on the MPEG frame properties
		private const val MPEG_VERSION_1_MODE_MONO_OFFSET = 21
		private const val MPEG_VERSION_1_MODE_STEREO_OFFSET = 36
		private const val MPEG_VERSION_2_MODE_MONO_OFFSET = 13
		private const val MPEG_VERSION_2_MODE_STEREO_OFFSET = 21
		private const val XING_HEADER_BUFFER_SIZE = 120
		private const val XING_IDENTIFIER_BUFFER_SIZE = 4
		private const val XING_FLAG_BUFFER_SIZE = 4
		private const val XING_FRAMECOUNT_BUFFER_SIZE = 4
		private const val XING_AUDIOSIZE_BUFFER_SIZE = 4
		val MAX_BUFFER_SIZE_NEEDED_TO_READ_XING: Int =
			MPEG_VERSION_1_MODE_STEREO_OFFSET + XING_HEADER_BUFFER_SIZE + LameFrame.Companion.LAME_HEADER_BUFFER_SIZE
		private const val BYTE_1 = 0
		private const val BYTE_2 = 1
		private const val BYTE_3 = 2
		private const val BYTE_4 = 3

		/**
		 * Use when it is a VBR (Variable Bitrate) file
		 */
		private val XING_VBR_ID =
			byteArrayOf('X'.code.toByte(), 'i'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte())

		/**
		 * Use when it is a CBR (Constant Bitrate) file
		 */
		private val XING_CBR_ID =
			byteArrayOf('I'.code.toByte(), 'n'.code.toByte(), 'f'.code.toByte(), 'o'.code.toByte())

		/**
		 * Parse the XingFrame of an MP3File, cannot be called until we have validated that
		 * this is a XingFrame
		 *
		 * @return
		 * @throws InvalidAudioFrameException
		 */
		@Throws(InvalidAudioFrameException::class)
		fun parseXingFrame(header: ByteBuffer): XingFrame {
			return XingFrame(header)
		}

		/**
		 * IS this a Xing frame
		 *
		 * @param bb
		 * @param mpegFrameHeader
		 * @return true if this is a Xing frame
		 */
		fun isXingFrame(bb: ByteBuffer, mpegFrameHeader: MPEGFrameHeader?): ByteBuffer? {
			//We store this so can return here after scanning through buffer
			val startPosition = bb.position()

			//Get to Start of where Xing Frame Should be ( we dont know if it is one at this point)
			if (mpegFrameHeader?.version == MPEGFrameHeader.Companion.VERSION_1) {
				if (mpegFrameHeader.version == MPEGFrameHeader.Companion.MODE_MONO) {
					bb.position(startPosition + MPEG_VERSION_1_MODE_MONO_OFFSET)
				} else {
					bb.position(startPosition + MPEG_VERSION_1_MODE_STEREO_OFFSET)
				}
			} else {
				if (mpegFrameHeader?.channelMode == MPEGFrameHeader.Companion.MODE_MONO) {
					bb.position(startPosition + MPEG_VERSION_2_MODE_MONO_OFFSET)
				} else {
					bb.position(startPosition + MPEG_VERSION_2_MODE_STEREO_OFFSET)
				}
			}

			//Create header from here
			val header = bb.slice()

			// Return Buffer to start Point
			bb.position(startPosition)

			//Check Identifier
			val identifier = ByteArray(XING_IDENTIFIER_BUFFER_SIZE)
			header[identifier]
			if (!Arrays.equals(identifier, XING_VBR_ID) && !Arrays.equals(
					identifier,
					XING_CBR_ID
				)
			) {
				return null
			}
			AudioFile.logger.finest("Found Xing Frame")
			return header
		}
	}
}

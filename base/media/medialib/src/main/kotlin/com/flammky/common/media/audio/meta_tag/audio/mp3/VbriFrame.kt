package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import java.nio.ByteBuffer
import java.util.*

/**
 * Vrbi Frame
 *
 *
 * In MP3s encoded using the franhofer encoder which variable bit rate the first frame in the file contains a
 * special frame called a Vrbi Frame, instead of audio data (Other Vbr encoders use the more common Xing Frame).
 * This is used to store additional information about the file. The most important aspect for
 * this library is details allowing us to determine the bitrate of a Variable Bit Rate VBR file without having
 * to process the whole file.
 *
 * From http://www.codeproject.com/KB/audio-video/mpegaudioinfo.aspx#SideInfo
 *
 * This header is only used by MPEG audio files encoded with the Fraunhofer Encoder as far as I know. It is different from the XING header. You find it exactly
 * 32 bytes after the end of the first MPEG audio header in the file. (Note that the position is zero-based; position, length and example are each in byte-format.)
 * Position 	Length 	Meaning 	Example
 * 0    		    4 	VBR header ID in 4 ASCII chars, always 'VBRI', not NULL-terminated 	'VBRI'
 * 4	  			2 	Version ID as Big-Endian WORD 	1
 * 6 				2 	Delay as Big-Endian float 	7344
 * 8 				2 	Quality indicator 	75
 * 10 				4 	Number of Bytes of Audio as Big-Endian DWORD 	45000
 * 14 				4 	Number of Frames as Big-Endian DWORD 	7344
 * 18 				2 	Number of entries within TOC table as Big-Endian WORD 	100
 * 20 				2 	Scale factor of TOC table entries as Big-Endian DWORD 	1
 * 22 				2 	Size per table entry in bytes (max 4) as Big-Endian WORD 	2
 * 24 				2 	Frames per table entry as Big-Endian WORD 	845
 * 26 						TOC entries for seeking as Big-Endian integral. From size per table entry and number of entries, you can calculate the length of this field.
 *
 */
class VbriFrame private constructor(private val header: ByteBuffer) {
	private val vbr = false

	/**
	 * @return count of frames
	 */
	var frameCount = -1
		private set

	/**
	 * @return size of audio data in bytes
	 */
	var audioSize = -1
		private set

	/**
	 * Read the VBRI Properties from the buffer
	 */
	init {
		//Go to start of Buffer
		header.rewind()
		header.position(10)
		setAudioSize()
		setFrameCount()
	}

	/**
	 * Set size of AudioData
	 */
	private fun setAudioSize() {
		val frameSizeBuffer = ByteArray(VBRI_AUDIOSIZE_BUFFER_SIZE)
		header[frameSizeBuffer]
		audioSize =
			frameSizeBuffer[BYTE_1].toInt() shl 24 and -0x1000000 or (frameSizeBuffer[BYTE_2].toInt() shl 16 and 0x00FF0000) or (frameSizeBuffer[BYTE_3].toInt() shl 8 and 0x0000FF00) or (frameSizeBuffer[BYTE_4].toInt() and 0x000000FF)
	}

	/**
	 * Set count of frames
	 */
	private fun setFrameCount() {
		val frameCountBuffer = ByteArray(VBRI_FRAMECOUNT_BUFFER_SIZE)
		header[frameCountBuffer]
		frameCount =
			frameCountBuffer[BYTE_1].toInt() shl 24 and -0x1000000 or (frameCountBuffer[BYTE_2].toInt() shl 16 and 0x00FF0000) or (frameCountBuffer[BYTE_3].toInt() shl 8 and 0x0000FF00) or (frameCountBuffer[BYTE_4].toInt() and 0x000000FF)
	}

	/**
	 * Is this VBRIFrame detailing a varaible bit rate MPEG
	 *
	 * @return
	 */
	fun isVbr(): Boolean {
		return true
	}

	val encoder: String
		get() = "Fraunhofer"

	/**
	 * @return a string represntation
	 */
	override fun toString(): String {
		return """VBRIheader
	vbr:$vbr
	frameCount:$frameCount
	audioFileSize:$audioSize
	encoder:${encoder}
"""
	}

	companion object {
		//The offset into frame
		private val VBRI_OFFSET: Int = MPEGFrameHeader.Companion.HEADER_SIZE + 32
		private const val VBRI_HEADER_BUFFER_SIZE = 120 //TODO this is just a guess, not right
		private const val VBRI_IDENTIFIER_BUFFER_SIZE = 4
		private const val VBRI_AUDIOSIZE_BUFFER_SIZE = 4
		private const val VBRI_FRAMECOUNT_BUFFER_SIZE = 4
		val MAX_BUFFER_SIZE_NEEDED_TO_READ_VBRI = VBRI_OFFSET + VBRI_HEADER_BUFFER_SIZE
		private const val BYTE_1 = 0
		private const val BYTE_2 = 1
		private const val BYTE_3 = 2
		private const val BYTE_4 = 3

		/**
		 * Identifier
		 */
		private val VBRI_VBR_ID =
			byteArrayOf('V'.code.toByte(), 'B'.code.toByte(), 'R'.code.toByte(), 'I'.code.toByte())

		/**
		 * Parse the VBRIFrame of an MP3File, cannot be called until we have validated that
		 * this is a VBRIFrame
		 *
		 * @return
		 * @throws InvalidAudioFrameException
		 */
		@Throws(InvalidAudioFrameException::class)
		fun parseVBRIFrame(header: ByteBuffer): VbriFrame {
			return VbriFrame(header)
		}

		/**
		 * IS this a VBRI frame
		 *
		 * @param bb
		 * @param mpegFrameHeader
		 * @return raw header if this is a VBRI frame
		 */
		fun isVbriFrame(bb: ByteBuffer, mpegFrameHeader: MPEGFrameHeader?): ByteBuffer? {

			//We store this so can return here after scanning through buffer
			val startPosition = bb.position()
			AudioFile.logger.finest("Checking VBRI Frame at$startPosition")
			bb.position(startPosition + VBRI_OFFSET)

			//Create header from here
			val header = bb.slice()

			// Return Buffer to start Point
			bb.position(startPosition)

			//Check Identifier
			val identifier = ByteArray(VBRI_IDENTIFIER_BUFFER_SIZE)
			header[identifier]
			if (!Arrays.equals(identifier, VBRI_VBR_ID)) {
				return null
			}
			AudioFile.logger.finest("Found VBRI Frame")
			return header
		}
	}
}

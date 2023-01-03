package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 *
 *
 * Aiff File Header always consists of
 *
 *
 *  * ckID - always FORM
 *  * chSize - size in 4 bytes
 *  * formType - currently either AIFF or AIFC, see [AiffType]
 *  * chunks[] - an array of chunks
 *
 */
class AiffFileHeader(private val loggingName: String) {
	/**
	 * Reads the file header and registers the data (file type) with the given header.
	 *
	 * @param fc random access file
	 * @param aiffAudioHeader the [AudioHeader] we set the read data to
	 * @return the number of bytes in the FORM chunk, i.e. the size of the payload (not including the 8bit header)
	 * @throws IOException
	 * @throws CannotReadException if the file is not a valid AIFF file
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun readHeader(fc: FileChannel, aiffAudioHeader: AiffAudioHeader): Long {
		val headerData = ByteBuffer.allocateDirect(IffHeaderChunk.FORM_HEADER_LENGTH)
		headerData.order(ByteOrder.BIG_ENDIAN)
		val bytesRead = fc.read(headerData)
		headerData.position(0)
		if (bytesRead < IffHeaderChunk.FORM_HEADER_LENGTH) {
			throw IOException(loggingName + ":AIFF:Unable to read required number of databytes read:" + bytesRead + ":required:" + IffHeaderChunk.FORM_HEADER_LENGTH)
		}
		val signature = Utils.readFourBytesAsChars(headerData)
		return if (FORM == signature) {
			// read chunk size
			val chunkSize = headerData.int.toLong()
			logger.config(
				loggingName + ":Reading AIFF header size:" + Hex.asDecAndHex(chunkSize)
					+ ":File Size Should End At:" + Hex.asDecAndHex(chunkSize + ChunkHeader.CHUNK_HEADER_SIZE)
			)
			readFileType(headerData, aiffAudioHeader)
			chunkSize
		} else {
			throw CannotReadException(
				"$loggingName:Not an AIFF file: incorrect signature $signature"
			)
		}
	}

	/**
	 * Reads the file type ([AiffType]).
	 *
	 * @throws CannotReadException if the file type is not supported
	 */
	@Throws(IOException::class, CannotReadException::class)
	private fun readFileType(bytes: ByteBuffer, aiffAudioHeader: AiffAudioHeader) {
		val type = Utils.readFourBytesAsChars(bytes)
		if (AiffType.AIFF.code == type) {
			aiffAudioHeader.fileType = AiffType.AIFF
		} else if (AiffType.AIFC.code == type) {
			aiffAudioHeader.fileType = AiffType.AIFC
		} else {
			throw CannotReadException(
				"$loggingName:Invalid AIFF file: Incorrect file type info $type"
			)
		}
	}

	companion object {
		private const val FORM = "FORM"
		private val logger = Logger.getLogger("org.jaudiotagger.audio.aiff.AudioFileHeader")
	}
}

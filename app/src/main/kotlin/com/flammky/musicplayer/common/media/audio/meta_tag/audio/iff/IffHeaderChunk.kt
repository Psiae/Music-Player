package com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.isOddLength
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 * Common to all IFF formats such as Wav and Aiff, is the the top level chunk, 8 byte header plus 4 byte type field
 */
object IffHeaderChunk {
	var logger = Logger.getLogger("org.jaudiotagger.audio.iff")

	@JvmField
	var SIGNATURE_LENGTH = 4

	@JvmField
	var SIZE_LENGTH = 4

	@JvmField
	var TYPE_LENGTH = 4
	var FORM_HEADER_LENGTH = SIGNATURE_LENGTH + SIZE_LENGTH + TYPE_LENGTH

	/**
	 * If Size is not even then we skip a byte, because chunks have to be aligned
	 *
	 * @param raf
	 * @param chunkHeader
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun ensureOnEqualBoundary(raf: RandomAccessFile, chunkHeader: ChunkHeader) {
		if (isOddLength(chunkHeader.size)) {
			// Must come out to an even byte boundary unless at end of file
			if (raf.filePointer < raf.length()) {
				logger.config("Skipping Byte because on odd boundary")
				raf.skipBytes(1)
			}
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun ensureOnEqualBoundary(fc: FileChannel, chunkHeader: ChunkHeader) {
		if (isOddLength(chunkHeader.size)) {
			// Must come out to an even byte boundary unless at end of file
			if (fc.position() < fc.size()) {
				logger.config("Skipping Byte because on odd boundary")
				fc.position(fc.position() + 1)
			}
		}
	}
}

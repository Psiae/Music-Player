package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.IffHeaderChunk
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * DSD Chunk
 */
class DsdChunk private constructor(dataBuffer: ByteBuffer) {
	var chunkSizeLength: Long
	var fileLength: Long
	var metadataOffset: Long

	init {
		chunkSizeLength = dataBuffer.long
		fileLength = dataBuffer.long
		metadataOffset = dataBuffer.long
	}

	override fun toString(): String {
		return ("ChunkSize:" + chunkSizeLength
			+ ":fileLength:" + fileLength
			+ ":metadata:" + metadataOffset)
	}

	/**
	 * Write new DSDchunk to buffer
	 *
	 * @return
	 */
	fun write(): ByteBuffer {
		val buffer = ByteBuffer.allocateDirect(DSD_HEADER_LENGTH)
		buffer.order(ByteOrder.LITTLE_ENDIAN)
		buffer.put(DsfChunkType.DSD.code.toByteArray(StandardCharsets.US_ASCII))
		buffer.putLong(chunkSizeLength)
		buffer.putLong(fileLength)
		buffer.putLong(metadataOffset)
		buffer.flip()
		return buffer
	}

	companion object {
		const val CHUNKSIZE_LENGTH = 8
		const val FILESIZE_LENGTH = 8
		const val METADATA_OFFSET_LENGTH = 8
		const val FMT_CHUNK_MIN_DATA_SIZE_ = 40
		val DSD_HEADER_LENGTH =
			IffHeaderChunk.SIGNATURE_LENGTH + CHUNKSIZE_LENGTH + FILESIZE_LENGTH + METADATA_OFFSET_LENGTH

		fun readChunk(dataBuffer: ByteBuffer): DsdChunk? {
			val type = Utils.readFourBytesAsChars(dataBuffer)
			return if (DsfChunkType.DSD.code == type) {
				DsdChunk(dataBuffer)
			} else null
		}
	}
}

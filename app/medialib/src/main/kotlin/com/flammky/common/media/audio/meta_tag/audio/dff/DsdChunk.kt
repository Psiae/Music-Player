package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf.DsfChunkType
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.nio.ByteBuffer

/**
 * DSD Chunk
 */
class DsdChunk private constructor(dataBuffer: ByteBuffer) {
	override fun toString(): String {
		return DffChunkType.DSD.code
	}

	companion object {
		const val CHUNKSIZE_LENGTH = 8
		const val SIGNATURE_LENGTH = 4
		const val DSD_HEADER_LENGTH = CHUNKSIZE_LENGTH
		fun readChunk(dataBuffer: ByteBuffer): DsdChunk? {
			val type = Utils.readFourBytesAsChars(dataBuffer)
			return if (DsfChunkType.DSD.code == type) {
				DsdChunk(dataBuffer)
			} else null
		}
	}
}

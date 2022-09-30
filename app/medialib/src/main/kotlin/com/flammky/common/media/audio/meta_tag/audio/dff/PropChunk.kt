package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.nio.ByteBuffer

/**
 * PROP Chunk.
 */
class PropChunk private constructor(dataBuffer: ByteBuffer) {
	override fun toString(): String {
		return DffChunkType.PROP.code
	}

	companion object {
		const val CHUNKSIZE_LENGTH = 8
		const val SIGNATURE_LENGTH = 4
		const val PROP_HEADER_LENGTH = SIGNATURE_LENGTH + CHUNKSIZE_LENGTH
		fun readChunk(dataBuffer: ByteBuffer): PropChunk? {
			val type = Utils.readFourBytesAsChars(dataBuffer)
			return if (DffChunkType.PROP.code == type) {
				PropChunk(dataBuffer)
			} else null
		}
	}
}

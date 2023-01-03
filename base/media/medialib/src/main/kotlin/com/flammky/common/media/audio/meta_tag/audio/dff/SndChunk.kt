package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.nio.ByteBuffer

/**
 * DSD Chunk
 */
class SndChunk private constructor(dataBuffer: ByteBuffer) {
	override fun toString(): String {
		return DffChunkType.SND.code
	}

	companion object {
		const val CHUNKSIZE_LENGTH = 0
		const val SIGNATURE_LENGTH = 4
		const val SND_HEADER_LENGTH = SIGNATURE_LENGTH + CHUNKSIZE_LENGTH
		fun readChunk(dataBuffer: ByteBuffer): SndChunk? {
			val type = Utils.readFourBytesAsChars(dataBuffer)
			return if (DffChunkType.SND.code == type) {
				SndChunk(dataBuffer)
			} else null
		}
	}
}

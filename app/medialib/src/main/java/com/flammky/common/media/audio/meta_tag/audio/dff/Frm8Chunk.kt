package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.nio.ByteBuffer

/**
 * DSD Chunk
 */
class Frm8Chunk private constructor(dataBuffer: ByteBuffer) {
	override fun toString(): String {
		return DffChunkType.FRM8.code
	}

	companion object {
		const val SIGNATURE_LENGTH = 4
		const val CHUNKSIZE_LENGTH = 8
		const val FRM8_HEADER_LENGTH = SIGNATURE_LENGTH + CHUNKSIZE_LENGTH
		fun readChunk(dataBuffer: ByteBuffer): Frm8Chunk? {
			val frm8Signature = Utils.readFourBytesAsChars(dataBuffer)
			return if (DffChunkType.FRM8.code != frm8Signature) {
				null
			} else Frm8Chunk(
				dataBuffer
			)
		}
	}
}

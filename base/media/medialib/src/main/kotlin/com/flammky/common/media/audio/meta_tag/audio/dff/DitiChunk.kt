package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * DITI Chunk. Carry the Title.
 */
class DitiChunk(dataBuffer: ByteBuffer?) : BaseChunk(dataBuffer) {
	@Throws(IOException::class)
	public override fun readDataChunch(fc: FileChannel) {
		super.readDataChunch(fc)

		//skipToChunkEnd(fc);
	}

	override fun toString(): String {
		return DffChunkType.DITI.code
	}
}

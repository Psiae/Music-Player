package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * DSD Chunk
 */
class EndChunk(dataBuffer: ByteBuffer?) : BaseChunk(dataBuffer) {
	/**
	 * @return the dataEnd (should be the end of file)
	 */
	var dataEnd: Long? = null
		private set

	@Throws(IOException::class)
	public override fun readDataChunch(fc: FileChannel) {
		super.readDataChunch(fc)
		dataEnd = this.chunkEnd

		//skipToChunkEnd(fc);
	}

	/**
	 * @return the point where data starts
	 */
	val dataStart: Long?
		get() = chunkStart

	override fun toString(): String {
		return DffChunkType.END.code + " (END)"
	}
}

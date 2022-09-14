package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * FS Chunk. Retrive samplerate.
 */
class FsChunk(dataBuffer: ByteBuffer?) : BaseChunk(dataBuffer) {
	/**
	 * @return the sampleRate
	 */
	var sampleRate = 0
		private set

	@Throws(IOException::class)
	public override fun readDataChunch(fc: FileChannel) {
		super.readDataChunch(fc)
		val audioData = Utils.readFileDataIntoBufferLE(fc, 4)
		sampleRate = Integer.reverseBytes(audioData.int)
		skipToChunkEnd(fc)
	}

	override fun toString(): String {
		return DffChunkType.FS.code
	}
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * FS Chunk. Retrive samplerate.
 */
class FrteChunk(dataBuffer: ByteBuffer?) : BaseChunk(dataBuffer) {
	/**
	 * @return the numFrames
	 */
	var numFrames = 0
		private set

	/**
	 * @return the rate
	 */
	var rate: Short? = null
		private set

	@Throws(IOException::class)
	public override fun readDataChunch(fc: FileChannel) {
		super.readDataChunch(fc)
		var audioData = Utils.readFileDataIntoBufferLE(fc, 4)
		numFrames = Integer.reverseBytes(audioData.int)
		audioData = Utils.readFileDataIntoBufferLE(fc, 2)
		rate = java.lang.Short.reverseBytes(audioData.short)
		skipToChunkEnd(fc)
	}

	override fun toString(): String {
		return DffChunkType.FRTE.code
	}
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

/**
 * CMPR Chunk. Retrieve compression.
 */
class CmprChunk(dataBuffer: ByteBuffer) : BaseChunk(dataBuffer) {
	/**
	 * @return the compression
	 */
	var compression: String? = null
		private set

	/**
	 * @return the description
	 */
	var description: String? = null
		private set

	@Throws(IOException::class)
	public override fun readDataChunch(fc: FileChannel) {
		super.readDataChunch(fc)
		var audioData = Utils.readFileDataIntoBufferLE(fc, 4)
		compression = Utils.readFourBytesAsChars(audioData)
		audioData = Utils.readFileDataIntoBufferLE(fc, 1)
		val b = audioData.get()
		val blen = b.toInt() and 255
		audioData = Utils.readFileDataIntoBufferLE(fc, blen)
		val buff = ByteArray(blen)
		audioData[buff]
		description = String(buff, StandardCharsets.ISO_8859_1)

		//System.out.println(" new postion: "+fc.position());
		skipToChunkEnd(fc)
	}

	override fun toString(): String {
		return DffChunkType.CMPR.code
	}
}

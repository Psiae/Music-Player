package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * CHNL Chunk. Retrive channels info.
 */
class ChnlChunk(dataBuffer: ByteBuffer) : BaseChunk(dataBuffer) {
	/**
	 * @return the sampleRate
	 */
	var numChannels: Short = 0
		private set

	var IDs: Array<String?>? = null

	@Throws(IOException::class)
	public override fun readDataChunch(fc: FileChannel) {
		super.readDataChunch(fc)
		var audioData = Utils.readFileDataIntoBufferLE(fc, 2)
		numChannels = java.lang.Short.reverseBytes(audioData.short)
		//System.out.println(" numChannels: "+numChannels);

		//System.out.println(" new postion: "+fc.position());
		IDs = arrayOfNulls(numChannels.toInt())
		for (i in 0 until numChannels) {
			audioData = Utils.readFileDataIntoBufferLE(fc, 4)
			IDs!![i] = Utils.readFourBytesAsChars(audioData)
		}
		//System.out.printf("Channels %s%n\n", Arrays.toString(IDs));
		//System.out.println(" new postion: "+fc.position());
		skipToChunkEnd(fc)
	}

	override fun toString(): String {
		return DffChunkType.CHNL.code
	}
}

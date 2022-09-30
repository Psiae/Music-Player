package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidChunkException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Base Chunk for all chuncks in the dff FRM8 Chunk.
 */
open class BaseChunk protected constructor(dataBuffer: ByteBuffer?) {
	/**
	 * @return the chunkSize
	 */
	var chunkSize: Long? = null
		private set

	/**
	 * @return the chunk Start position
	 */
	var chunkStart: Long? = null
		private set

	@Throws(IOException::class)
	protected open fun readDataChunch(fc: FileChannel) {
		val audioData = Utils.readFileDataIntoBufferLE(fc, 8)
		chunkSize = java.lang.Long.reverseBytes(audioData.long)
		chunkStart = fc.position()

		//System.out.println("chunck: "+this+" size: "+this.getChunkSize()+" starts at: "+this.getChunkStart());
	}

	@Throws(IOException::class)
	protected fun skipToChunkEnd(fc: FileChannel) {
		val skip = chunkEnd - fc.position()
		if (skip > 0) {
			// Read audio data
			Utils.readFileDataIntoBufferLE(fc, skip.toInt())
		}
	}

	/**
	 * @return the chunk End position.
	 */
	val chunkEnd: Long
		get() = chunkStart!! + chunkSize!!

	companion object {
		const val ID_LENGHT = 4

		@Throws(InvalidChunkException::class)
		fun readIdChunk(dataBuffer: ByteBuffer): BaseChunk {
			val type = Utils.readFourBytesAsChars(dataBuffer)
			//System.out.println("BaseChunk.type: "+type);
			return if (DffChunkType.FS.code == type) {
				FsChunk(dataBuffer)
			} else if (DffChunkType.CHNL.code == type) {
				ChnlChunk(dataBuffer)
			} else if (DffChunkType.CMPR.code == type) {
				CmprChunk(dataBuffer)
			} else if (DffChunkType.END.code == type || DffChunkType.DSD.code == type) {
				EndChunk(dataBuffer)
			} else if (DffChunkType.DST.code == type) {
				DstChunk(dataBuffer)
			} else if (DffChunkType.FRTE.code == type) {
				FrteChunk(dataBuffer)
			} else if (DffChunkType.ID3.code == type) {
				Id3Chunk(dataBuffer)
			} else {
				throw InvalidChunkException("$type is not recognized as a valid DFF chunk")
			}
		}
	}
}

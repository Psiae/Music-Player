package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Abstract class For reading Aiff Chunks used by both Audio and Tag Reader
 */
abstract class AiffChunkReader {
	/**
	 * Read the next chunk into ByteBuffer as specified by ChunkHeader and moves raf file pointer
	 * to start of next chunk/end of file.
	 *
	 * @param fc
	 * @param chunkHeader
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected fun readChunkDataIntoBuffer(fc: FileChannel, chunkHeader: ChunkHeader): ByteBuffer {
		val chunkData = ByteBuffer.allocateDirect(chunkHeader.size.toInt())
		chunkData.order(ByteOrder.BIG_ENDIAN)
		fc.read(chunkData)
		chunkData.position(0)
		return chunkData
	}
}

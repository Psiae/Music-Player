package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Sound chunk.
 * Doesn't actually read the content, but skips it.
 */
class SoundChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer
) : Chunk(chunkData, chunkHeader) {
	/**
	 * Reads a chunk and extracts information.
	 *
	 * @return `false` if the chunk is structurally
	 * invalid, otherwise `true`
	 */
	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		return true
	}
}

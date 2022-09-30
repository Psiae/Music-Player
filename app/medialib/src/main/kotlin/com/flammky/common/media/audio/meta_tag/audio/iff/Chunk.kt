package com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff

import java.io.IOException
import java.nio.ByteBuffer

/**
 * Abstract superclass for IFF/AIFF chunks.
 *
 * @author Gary McGath
 */
abstract class Chunk
/**
 * Constructor used by Wav
 *
 * @param chunkData
 * @param chunkHeader
 */(protected var chunkData: ByteBuffer, protected var chunkHeader: ChunkHeader) {
	/**
	 * Reads a chunk and puts appropriate information into
	 * the RepInfo object.
	 *
	 * @return `false` if the chunk is structurally
	 * invalid, otherwise `true`
	 */
	@Throws(IOException::class)
	abstract fun readChunk(): Boolean
}

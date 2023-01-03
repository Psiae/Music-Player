package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Contains the name of the sampled sound. The Name Chunk is optional.
 * No more than one Name Chunk may exist within a FORM AIFF.
 */
class NameChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	aiffAudioHeader: AiffAudioHeader
) : TextChunk(chunkHeader, chunkData, aiffAudioHeader) {
	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		aiffAudioHeader.name = readChunkText()
		return true
	}
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Contains one or more author names. An author in this case is the creator of a sampled sound.
 * The Author Chunk is optional. No more than one Author Chunk may exist within a FORM AIFF.
 */
class AuthorChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	aiffAudioHeader: AiffAudioHeader
) : TextChunk(chunkHeader, chunkData, aiffAudioHeader) {

	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		aiffAudioHeader.author = readChunkText()
		return true
	}
}

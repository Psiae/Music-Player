package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Contains a comment. Use of this chunk is discouraged within FORM AIFF. The more powerful [CommentsChunk]
 * should be used instead. The Annotation Chunk is optional. Many Annotation Chunks may exist within a FORM AIFF.
 *
 * @see CommentsChunk
 */
class AnnotationChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	aiffAudioHeader: AiffAudioHeader
) : TextChunk(chunkHeader, chunkData, aiffAudioHeader) {

	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		aiffAudioHeader.addAnnotation(readChunkText())
		return true
	}
}

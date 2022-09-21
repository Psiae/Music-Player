package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Provides common functionality for textual chunks like [NameChunk], [AuthorChunk],
 * [CopyrightChunk] and [AnnotationChunk].
 */
abstract class TextChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	protected val aiffAudioHeader: AiffAudioHeader
) : Chunk(chunkData, chunkHeader) {

	/**
	 * Reads the chunk and transforms it to a [String].
	 *
	 * @return text string
	 * @throws IOException if the read fails
	 */
	@Throws(IOException::class)
	protected fun readChunkText(): String {
		// the spec actually only defines ASCII, not ISO_8859_1, but it probably does not hurt to be lenient
		return Utils.getString(chunkData, 0, chunkData.remaining(), StandardCharsets.ISO_8859_1)
	}
}

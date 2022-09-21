package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 *
 *
 * The Copyright Chunk contains a copyright notice for the sound. text contains a date followed
 * by the copyright owner. The chunk ID '(c) ' serves as the copyright characters '©'. For example,
 * a Copyright Chunk containing the text "1988 Apple Computer, Inc." means "© 1988 Apple Computer, Inc."
 *
 *
 *
 * The Copyright Chunk is optional. No more than one Copyright Chunk may exist within a FORM AIFF.
 *
 */
class CopyrightChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	aiffAudioHeader: AiffAudioHeader
) : TextChunk(chunkHeader, chunkData, aiffAudioHeader) {

	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		aiffAudioHeader.copyright = readChunkText()
		return true
	}
}

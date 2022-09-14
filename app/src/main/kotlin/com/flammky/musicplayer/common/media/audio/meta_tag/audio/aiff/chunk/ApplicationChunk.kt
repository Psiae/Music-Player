package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 * The Application Chunk can be used for any purposes whatsoever by developers and application authors. For
 * example, an application that edits sounds might want to use this chunk to store editor state parameters such as
 * magnification levels, last cursor position, etc.
 */
class ApplicationChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	private val aiffHeader: AiffAudioHeader
) : Chunk(chunkData, chunkHeader) {
	/**
	 * Reads a chunk and puts an Application property into
	 * the RepInfo object.
	 *
	 * @return `false` if the chunk is structurally
	 * invalid, otherwise `true`
	 */
	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		val applicationSignature = Utils.readFourBytesAsChars(chunkData)
		var applicationName: String? = null

		/* If the application signature is 'pdos' or 'stoc',
		 * then the beginning of the data area is a Pascal
		 * string naming the application.  Otherwise, we
		 * ignore the data.  ('pdos' is for Apple II
		 * applications, 'stoc' for the entire non-Apple world.)
		 */

		if (SIGNATURE_STOC == applicationSignature || SIGNATURE_PDOS == applicationSignature) {
			applicationName = Utils.readPascalString(chunkData)
		}

		aiffHeader.addApplicationIdentifier("$applicationSignature: $applicationName")
		return true
	}

	companion object {
		private const val SIGNATURE_PDOS = "pdos"
		private const val SIGNATURE_STOC = "stoc"
	}
}

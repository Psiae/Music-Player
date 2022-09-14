package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * A chunk modifier which works with information provided by
 * [WriteableChunk] objects.<br></br>
 *
 * @author Christian Laireiter
 */
class WriteableChunkModifer
/**
 * Creates an instance.<br></br>
 *
 * @param chunk chunk to write
 */(
	/**
	 * The chunk to write.
	 */
	private val writableChunk: WriteableChunk
) : ChunkModifier {
	/**
	 * {@inheritDoc}
	 */
	override fun isApplicable(guid: GUID): Boolean {
		return guid.equals(writableChunk.guid)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun modify(
		guid: GUID?,
		chunk: InputStream?,
		destination: OutputStream
	): ModificationResult { // NOPMD by Christian Laireiter on 5/9/09 5:03 PM
		var destination = destination
		var chunkDiff = 0
		var newSize: Long = 0
		var oldSize: Long = 0
		assert(CountingOutputstream(destination).also { destination = it } != null)
		if (!writableChunk.isEmpty) {
			newSize = writableChunk.writeInto(destination)
			assert(newSize == writableChunk.currentAsfChunkSize)
			assert((destination as CountingOutputstream).count == newSize)
			if (guid == null) {
				chunkDiff++
			}
		}
		if (guid != null) {
			assert(isApplicable(guid))
			if (writableChunk.isEmpty) {
				chunkDiff--
			}
			oldSize = Utils.readUINT64(chunk!!)
			chunk.skip(oldSize - 24)
		}
		return ModificationResult(chunkDiff, newSize - oldSize, guid)
	}
}

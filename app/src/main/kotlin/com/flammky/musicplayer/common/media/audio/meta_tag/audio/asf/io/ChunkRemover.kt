package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * This [ChunkModifier] implementation is meant to remove selected chunks.<br></br>
 *
 * @author Christian Laireiter
 */
class ChunkRemover(vararg guids: GUID) : ChunkModifier {
	/**
	 * Stores the GUIDs, which are about to be removed by this modifier.<br></br>
	 */
	private val toRemove: MutableSet<GUID>

	/**
	 * Creates an instance, for removing selected chunks.<br></br>
	 *
	 * @param guids the GUIDs which are about to be removed by this modifier.
	 */
	init {
		toRemove = HashSet()
		for (current in guids) {
			toRemove.add(current)
		}
	}

	/**
	 * {@inheritDoc}
	 */
	override fun isApplicable(guid: GUID): Boolean {
		return toRemove.contains(guid)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun modify(
		guid: GUID?,
		source: InputStream?,
		destination: OutputStream
	): ModificationResult {
		val result: ModificationResult =
			if (guid == null) {
				// Now a chunk should be added, however, this implementation is for
				// removal.
				ModificationResult(0, 0)
			} else {
				assert(isApplicable(guid))
				// skip the chunk length minus 24 bytes for the already read length
				// and the guid.
				val chunkLen = Utils.readUINT64(source!!)
				source.skip(chunkLen - 24)
				ModificationResult(-1, -1 * chunkLen, guid)
			}
		return result
	}
}

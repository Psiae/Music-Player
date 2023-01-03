package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Reads an ASF chunk and writes a modified copy.<br></br>
 *
 * @author Christian Laireiter
 */
interface ChunkModifier {
	/**
	 * Determines, whether the modifier handles chunks identified by given
	 * `guid`.
	 *
	 * @param guid GUID to test.
	 * @return `true`, if this modifier can be used to modify the
	 * chunk.
	 */
	fun isApplicable(guid: GUID): Boolean

	/**
	 * Writes a modified copy of the chunk into the `destination.`.<br></br>
	 *
	 * @param guid        GUID of the chunk to modify.
	 * @param source      a stream providing the chunk, starting at the chunks length
	 * field.
	 * @param destination destination for the modified chunk.
	 * @return the differences between source and destination.
	 * @throws IOException on I/O errors.
	 */
	@Throws(IOException::class)
	fun modify(guid: GUID?, source: InputStream?, destination: OutputStream): ModificationResult
}

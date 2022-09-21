package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import java.io.IOException
import java.io.OutputStream

/**
 * Implementors can write themselves directly to an output stream, and have the
 * ability to tell the size they would need, as well as determine if they are
 * empty.<br></br>
 *
 * @author Christian Laireiter
 */
interface WriteableChunk {
	/**
	 * This method calculates the total amount of bytes, the chunk would consume
	 * in an ASF file.<br></br>
	 *
	 * @return amount of bytes the chunk would currently need in an ASF file.
	 */
	val currentAsfChunkSize: Long

	/**
	 * Returns the GUID of the chunk.
	 *
	 * @return GUID of the chunk.
	 */
	val guid: GUID?

	/**
	 * `true` if it is not necessary to write the chunk into an ASF
	 * file, since it contains no information.
	 *
	 * @return `true` if no useful data will be preserved.
	 */
	val isEmpty: Boolean

	/**
	 * Writes the chunk into the specified output stream, as ASF stream chunk.<br></br>
	 *
	 * @param out stream to write into.
	 * @return amount of bytes written.
	 * @throws IOException on I/O errors
	 */
	@Throws(IOException::class)
	fun writeInto(out: OutputStream): Long
}

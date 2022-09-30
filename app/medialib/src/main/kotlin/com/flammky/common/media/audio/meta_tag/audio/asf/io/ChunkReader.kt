package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import java.io.IOException
import java.io.InputStream

/**
 * A ChunkReader provides methods for reading an ASF chunk.<br></br>
 *
 * @author Christian Laireiter
 */
interface ChunkReader {
	/**
	 * Tells whether the reader can fail to return a valid chunk.<br></br>
	 * The current Use would be a modified version of [StreamChunkReader],
	 * which is configured to only manage audio streams. However, the primary
	 * GUID for audio and video streams is the same. So if a stream shows itself
	 * to be a video stream, the reader would return `null`.<br></br>
	 *
	 * @return `true`, if further analysis of the chunk can show,
	 * that the reader is not applicable, despite the header GUID
	 * [identification][.getApplyingIds] told it can handle
	 * the chunk.
	 */
	fun canFail(): Boolean

	/**
	 * Returns the GUIDs identifying the types of chunk, this reader will parse.<br></br>
	 *
	 * @return the GUIDs identifying the types of chunk, this reader will parse.<br></br>
	 */
	val applyingIds: Array<GUID>

	/**
	 * Parses the chunk.
	 *
	 * @param guid           the GUID of the chunks header, which is about to be read.
	 * @param stream         source to read chunk from.<br></br>
	 * No [GUID] is expected at the currents stream position.
	 * The length of the chunk is about to follow.
	 * @param streamPosition the position in stream, the chunk starts.<br></br>
	 * @return the read chunk. (Mostly a subclass of [Chunk]).<br></br>
	 * @throws IOException On I/O Errors.
	 */
	@Throws(IOException::class)
	fun read(guid: GUID?, stream: InputStream, streamPosition: Long): Chunk?
}

package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffUtil
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 *
 *
 * A comment consists of a time stamp, marker id, and a text count followed by text.
 *
 * <pre>
 * typedef struct {
 * unsigned long   timeStamp;
 * MarkerID        marker;
 * unsigned short  count;
 * char            text[];
 * } Comment;
</pre> *
 *
 *
 * `timeStamp` indicates when the comment was created. Units are the number of seconds
 * since January 1, 1904. (This time convention is the one used by the Macintosh. For procedures
 * that manipulate the time stamp, see The Operating System Utilities chapter in Inside Macintosh,
 * vol II). For a routine that will convert this to an Apple II GS/OS format time, please see
 * Apple II File Type Note for filetype 0xD8, aux type 0x0000.
 *
 *
 *
 * A comment can be linked to a marker. This allows applications to store long descriptions of
 * markers as a comment. If the comment is referring to a marker, then marker is the ID of that
 * marker. Otherwise, marker is zero, indicating that this comment is not linked to a marker.
 *
 *
 *
 * `count` is the length of the text that makes up the comment. This is a 16 bit quantity,
 * allowing much longer comments than would be available with a pstring.
 *
 *
 *
 * `text` contains the comment itself. This text must be padded with a byte at the end to
 * insure that it is an even number of bytes in length. This pad byte, if present, is not
 * included in count.
 *
 *
 * @see AnnotationChunk
 */
class CommentsChunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	private val aiffHeader: AiffAudioHeader
) : Chunk(chunkData, chunkHeader) {
	/**
	 * Reads a chunk and extracts information.
	 *
	 * @return `false` if the chunk is structurally
	 * invalid, otherwise `true`
	 */
	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		val numComments = Utils.u(chunkData.short)

		// For each comment
		for (i in 0 until numComments) {
			val timestamp = Utils.u(chunkData.int)
			val jTimestamp = AiffUtil.timestampToDate(timestamp)
			val marker = Utils.u(chunkData.short)
			val count = Utils.u(chunkData.short)
			// Append a timestamp to the comment
			val text = Utils.getString(
				chunkData,
				0,
				count,
				StandardCharsets.ISO_8859_1
			) + " " + AiffUtil.formatDate(jTimestamp)
			if (count % 2 != 0) {
				//#300
				//if count is odd, text is padded with an extra byte that we need to consume
				//but possibly the extra byte is missing so allow for that
				if (chunkData.position() < chunkData.limit()) {
					chunkData.get()
				}
			}
			aiffHeader.addComment(text)
		}
		return true
	}
}

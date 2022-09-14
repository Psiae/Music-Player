package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.AiffUtil
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import java.io.IOException
import java.nio.ByteBuffer

/**
 *
 *
 * The Format Version Chunk contains a date field to indicate the format rules for an
 * AIFF-C specification. This will enable smoother future upgrades to this specification.
 *
 *
 *
 * ckID is always 'FVER'.
 *
 *
 *
 * `ckDataSize` is the size of the data portion of the chunk, in bytes. It does not
 * include the 8 bytes used by ckID and ckDataSize. For this Chunk, ckDataSize has a value of 4.
 *
 *
 *
 * `timeStamp` indicates when the format version for the AIFF-C file was created.
 * Units are the number of seconds since January 1, 1904. (This time convention is the one
 * used by the Macintosh. For procedures that manipulate the time stamp, see The Operating
 * System Utilities chapter in Inside Macintosh, vol II ). For a routine that will convert
 * this to an Apple II GS/OS format time, please see Apple II File Type Note for filetype
 * 0xD8, aux type 0x0000.
 *
 *
 *
 * The Format Version Chunk is required. One and only one Format Version Chunk must appear in a FORM AIFC.
 *
 */
class FormatVersionChunk(
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
		val rawTimestamp = chunkData.int.toLong()
		// The timestamp is in seconds since January 1, 1904.
		// We must convert to Java time.
		val timestamp = AiffUtil.timestampToDate(rawTimestamp)
		aiffHeader.timestamp = timestamp
		return true
	}
}

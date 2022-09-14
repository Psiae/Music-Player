package com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex

/**
 * Created by Paul on 22/01/2016.
 */
open class ChunkSummary(var chunkId: String, var fileStartLocation: Long, var chunkSize: Long) {

	override fun toString(): String {
		val endLocation: Long =
			fileStartLocation + chunkSize + ChunkHeader.Companion.CHUNK_HEADER_SIZE
		return ("$chunkId:StartLocation:"
			+ Hex.asDecAndHex(
			fileStartLocation
		)
			+ ":SizeIncHeader:"
			+ (chunkSize + ChunkHeader.Companion.CHUNK_HEADER_SIZE)
			+ ":EndLocation:"
			+ Hex.asDecAndHex(endLocation))
	}

	val endLocation: Long
		get() = fileStartLocation + chunkSize + ChunkHeader.Companion.CHUNK_HEADER_SIZE
}

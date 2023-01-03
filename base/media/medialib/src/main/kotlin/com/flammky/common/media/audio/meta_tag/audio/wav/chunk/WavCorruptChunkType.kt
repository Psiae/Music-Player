package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk

/**
 * Chunk types incorrectly aligned, we can work round these, the 4th char either leading or ending is not known
 *
 * @see Chunk
 */
enum class WavCorruptChunkType(val code: String) {
	CORRUPT_ID3_EARLY("id3"), CORRUPT_ID3_LATE("d3 "), CORRUPT_LIST_EARLY("LIS"), CORRUPT_LIST_LATE(
		"IST"
	);

}

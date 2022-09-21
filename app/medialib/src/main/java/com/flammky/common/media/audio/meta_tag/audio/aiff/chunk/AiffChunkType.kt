package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.*

/**
 * Chunk types mark each [ChunkHeader]. They are *always* 4 ASCII chars long.
 *
 * @see [Chunk]
 */
enum class AiffChunkType(val code: String) {

	FORMAT_VERSION("FVER"),
	APPLICATION("APPL"),
	SOUND("SSND"),
	COMMON("COMM"),
	COMMENTS("COMT"),
	NAME("NAME"),
	AUTHOR("AUTH"),
	COPYRIGHT("(c) "),
	ANNOTATION("ANNO"),
	TAG("ID3 "),
	CORRUPT_TAG_LATE("D3 \u0000"),
	CORRUPT_TAG_EARLY("\u0000ID3");

	companion object {
		private val CODE_TYPE_MAP: MutableMap<String, AiffChunkType> = HashMap()

		init {
			values().forEach { type -> CODE_TYPE_MAP[type.code] = type }
		}

		/**
		 * Get [AiffChunkType] for code (e.g. "SSND").
		 *
		 * @param code chunk id
		 * @return chunk type or `null` if not registered
		 */
		@JvmStatic
		operator fun get(code: String): AiffChunkType? = CODE_TYPE_MAP[code]
	}
}

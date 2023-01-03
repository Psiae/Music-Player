package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf

/**
 * Chunk types mark each [ChunkHeader]. They are *always* 4 ASCII chars long.
 *
 * @see Chunk
 */
enum class DsfChunkType
/**
 * @param code 4 char string
 */(
	/**
	 * 4 char type code.
	 *
	 * @return 4 char type code, e.g. "SSND" for the sound chunk.
	 */
	val code: String
) {
	DSD("DSD "), FORMAT("fmt "), DATA("data"), ID3("ID3");

	companion object {
		private val CODE_TYPE_MAP: MutableMap<String, DsfChunkType> = HashMap()

		/**
		 * Get [DsfChunkType] for code (e.g. "SSND").
		 *
		 * @param code chunk id
		 * @return chunk type or `null` if not registered
		 */
		@Synchronized
		operator fun get(code: String): DsfChunkType? {
			if (CODE_TYPE_MAP.isEmpty()) {
				for (type in values()) {
					CODE_TYPE_MAP[type.code] = type
				}
			}
			return CODE_TYPE_MAP[code]
		}
	}
}

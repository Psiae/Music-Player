package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav

/**
 * Chunk types mark each [ChunkHeader]. They are *always* 4 ASCII chars long.
 *
 * @see Chunk
 */
enum class WavChunkType
/**
 * @param code 4 char string
 */(
	/**
	 * 4 char type code.
	 *
	 * @return 4 char type code, e.g. "SSND" for the sound chunk.
	 */
	val code: String, description: String
) {
	FORMAT("fmt ", "Basic Audio Information"), FACT(
		"fact",
		"Only strictly required for Non-PCM or compressed data"
	),
	DATA("data", "Stores the actual audio data"), LIST(
		"LIST",
		"List chunk, wraps round other chunks"
	),
	INFO("INFO", "Original metadata implementation"), ID3(
		"id3 ",
		"Stores metadata in ID3 chunk"
	),
	JUNK("JUNK", "Junk Data"), PAD("PAD ", "Official Padding Data"), IXML(
		"iXML",
		"Location Sound Metadata"
	),
	BRDK("BRDK", "BRDK"), ID3_UPPERCASE(
		"ID3 ",
		"Stores metadata in ID3 chunk, should be lowercase id"
	);

	companion object {
		private val CODE_TYPE_MAP: MutableMap<String?, WavChunkType> = HashMap()

		/**
		 * Get [WavChunkType] for code (e.g. "SSND").
		 *
		 * @param code chunk id
		 * @return chunk type or `null` if not registered
		 */
		@Synchronized
		operator fun get(code: String?): WavChunkType? {
			if (CODE_TYPE_MAP.isEmpty()) {
				for (type in values()) {
					CODE_TYPE_MAP[type.code] = type
				}
			}
			return CODE_TYPE_MAP[code]
		}
	}
}

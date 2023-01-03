package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav

/**
 * Wav sub format stored as two byte le integer
 */
enum class WavSubFormat(val code: Int, val description: String) {
	FORMAT_PCM(0x1, "WAV PCM"), FORMAT_FLOAT(0x3, "WAV IEEE_FLOAT"), FORMAT_ALAW(
		0x6,
		"WAV A-LAW"
	),
	FORMAT_MULAW(0x7, "WAV µ-LAW"), FORMAT_EXTENSIBLE(0xFFFE, "EXTENSIBLE"), FORMAT_GSM_COMPRESSED(
		0x31,
		"GSM_COMPRESSED"
	);

	companion object {
		// Reverse-lookup map for getting a compression type from code
		private val lookup: MutableMap<Int, WavSubFormat> = HashMap()

		init {
			for (next in values()) {
				lookup[next.code] = next
			}
		}

		fun getByCode(code: Int): WavSubFormat? {
			return lookup[code]
		}
	}
}

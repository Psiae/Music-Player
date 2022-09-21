package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

import java.util.*

/**
 * List of keys used by IPLS, TIPL and TMCL names that we map to individual keys, these are
 * handled differently to the remainder such as musicians and their instruments which is
 * essentially an infinite list.
 */
enum class StandardIPLSKey(val key: String) {
	ENGINEER("engineer"), MIXER("mix"), DJMIXER("DJ-mix"), PRODUCER("producer"), ARRANGER("arranger");

	companion object {
		private val lookup: MutableMap<String, StandardIPLSKey> = HashMap()

		init {
			for (s in EnumSet.allOf(StandardIPLSKey::class.java)) {
				lookup[s.key] = s
			}
		}

		operator fun get(key: String): StandardIPLSKey? {
			return lookup[key]
		}

		fun isKey(key: String): Boolean {
			return Companion[key] != null
		}
	}
}

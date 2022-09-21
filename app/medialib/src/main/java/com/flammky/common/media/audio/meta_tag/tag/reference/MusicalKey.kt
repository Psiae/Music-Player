package com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference

import java.util.*

/**
 * Musical key used by the Key tagFieldKey
 *
 * It is not enforced but can be used to verify the Musical key according to the ID3 specification of the TKEY field
 */
enum class MusicalKey(val value: String) {
	NOTE_A("A"), NOTE_B("B"), NOTE_C("C"), NOTE_D("D"), NOTE_E("E"), NOTE_F("F"), NOTE_G("G"), FLAT(
		"b"
	),
	SHARP("#"), MINOR("m"), OFF_KEY("o");

	companion object {
		private const val MAX_KEY_LENGTH = 3
		private val groundKeyMap: HashMap<String, MusicalKey> = HashMap(values().size)
		private val halfKeyMap: HashMap<String, MusicalKey> = HashMap(values().size)

		init {
			val groundKey = EnumSet.of(NOTE_A, NOTE_B, NOTE_C, NOTE_D, NOTE_E, NOTE_F, NOTE_G)
			for (curr in groundKey) {
				groundKeyMap[curr.value] = curr
			}
			val halfKey = EnumSet.of(FLAT, SHARP, MINOR)
			for (curr in halfKey) {
				halfKeyMap[curr.value] = curr
			}
		}

		fun isValid(musicalKey: String?): Boolean {
			if (musicalKey == null || musicalKey.length > MAX_KEY_LENGTH || musicalKey.length == 0) {
				return false
			}
			if (musicalKey.length == 1) {
				if (musicalKey == OFF_KEY.value) {
					return true
				}
			}
			if (!groundKeyMap.containsKey(musicalKey.substring(0, 1))) {
				return false
			}
			if (musicalKey.length == 2 || musicalKey.length == 3) {
				if (!halfKeyMap.containsKey(musicalKey.substring(1, 2))) {
					return false
				}
			}
			if (musicalKey.length == 3) {
				if (musicalKey.substring(2, 3) != MINOR.value) {
					return false
				}
			}
			return true
		}
	}
}

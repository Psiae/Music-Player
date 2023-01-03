package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes

/**
 * ID3V2 Genre list
 *
 *
 * Merging of Id3v2 genres and the extended ID3v2 genres
 */
class V2GenreTypes private constructor() {//Sort
	/**
	 * @return list of all valid v2 genres in alphabetical order
	 */
	val alphabeticalValueList: List<String>
		get() {
			val genres = GenreTypes.instanceOf.getAlphabeticalValueList()
			genres.add(ID3V2ExtendedGenreTypes.CR.description)
			genres.add(ID3V2ExtendedGenreTypes.RX.description)

			//Sort
			genres.sort()
			return genres
		}

	companion object {
		private var v2GenresTypes: V2GenreTypes? = null
		val instanceOf: V2GenreTypes?
			get() {
				if (v2GenresTypes == null) {
					v2GenresTypes = V2GenreTypes()
				}
				return v2GenresTypes
			}
	}
}

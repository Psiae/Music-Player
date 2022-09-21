package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.reference

/**
 * Defines the how ratings are stored In Windows Media Player for ID3
 *
 * Rating=0 → POPM=0
 * Rating=1 → POPM=1
 * Rating=2 → POPM=64
 * Rating=3 → POPM=128
 * Rating=4 → POPM=196
 * Rating=5 → POPM=255
 */
class MediaPlayerRating private constructor() : ID3Rating() {
	override fun convertRatingFromFiveStarScale(value: Int): Int {
		require(!(value < 0 || value > 5)) { "convert Ratings from Five Star Scale accepts values from 0 to 5 not:$value" }
		var newValue = 0
		when (value) {
			0 -> {}
			1 -> newValue = 1
			2 -> newValue = 64
			3 -> newValue = 128
			4 -> newValue = 196
			5 -> newValue = 255
		}
		return newValue
	}

	override fun convertRatingToFiveStarScale(value: Int): Int {
		var newValue = 0
		newValue = if (value <= 0) {
			0
		} else if (value <= 1) {
			1
		} else if (value <= 64) {
			2
		} else if (value <= 128) {
			3
		} else if (value <= 196) {
			4
		} else {
			5
		}
		return newValue
	}

	companion object {
		private var rating: ID3Rating? = null
		val instance: ID3Rating
			get() {
				if (rating == null) {
					rating = MediaPlayerRating()
				}
				return rating!!
			}
	}
}

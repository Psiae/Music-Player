package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.reference

/**
 * Defines the how ratings are stored in iTunes (but iTunes doesn't actually store in the field)
 *
 * Rating=0 → POPM=0
 * Rating=1 → POPM=20
 * Rating=2 → POPM=40
 * Rating=3 → POPM=60
 * Rating=4 → POPM=80
 * Rating=5 → POPM=100
 */
class ITunesRating private constructor() : ID3Rating() {
	override fun convertRatingFromFiveStarScale(value: Int): Int {
		require(!(value < 0 || value > 5)) { "convert Ratings from Five Star Scale accepts values from 0 to 5 not:$value" }
		var newValue = 0
		when (value) {
			0 -> {}
			1 -> newValue = 20
			2 -> newValue = 40
			3 -> newValue = 60
			4 -> newValue = 80
			5 -> newValue = 100
		}
		return newValue
	}

	override fun convertRatingToFiveStarScale(value: Int): Int {
		var newValue = 0
		newValue = if (value <= 0) {
			0
		} else if (value <= 20) {
			1
		} else if (value <= 40) {
			2
		} else if (value <= 60) {
			3
		} else if (value <= 80) {
			4
		} else if (value <= 100) {
			5
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
					rating = ITunesRating()
				}
				return rating!!
			}
	}
}

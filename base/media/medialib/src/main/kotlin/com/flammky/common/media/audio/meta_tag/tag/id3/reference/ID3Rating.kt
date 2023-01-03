package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.reference

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Tagger

/** Factory class that can be used to convert ratings to suit your preferred tagger/player
 *
 */
//TODO Only the main ones done yet
abstract class ID3Rating {
	abstract fun convertRatingFromFiveStarScale(value: Int): Int
	abstract fun convertRatingToFiveStarScale(value: Int): Int

	companion object {
		fun getInstance(tagger: Tagger?): ID3Rating {
			return when (tagger) {
				Tagger.ITUNES -> ITunesRating.instance
				Tagger.MEDIA_MONKEY -> MediaMonkeyPlayerRating.instance
				Tagger.MEDIAPLAYER -> MediaPlayerRating.instance
				else -> MediaPlayerRating.instance
			}
		}
	}
}
